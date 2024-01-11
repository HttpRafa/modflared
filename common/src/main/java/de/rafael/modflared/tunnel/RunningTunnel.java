package de.rafael.modflared.tunnel;

//------------------------------
//
// This class was developed by Rafael K.
// On 10/31/2022 at 11:29 PM
// In the project cloudflared
//
//------------------------------

import de.rafael.modflared.Modflared;
import de.rafael.modflared.download.CloudflaredBinary;
import de.rafael.modflared.tunnel.manager.TunnelManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.Platform;

import java.io.*;
import java.net.InetSocketAddress;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

public record RunningTunnel(Access access, Process process) {

    public static @NotNull CompletableFuture<RunningTunnel> createTunnel(@NotNull CloudflaredBinary binary, @NotNull Access access) {
        var future = new CompletableFuture<RunningTunnel>();
        Modflared.EXECUTOR.execute(() -> {
            try {
                var command = access.command(binary.getFile());
                Modflared.LOGGER.info(Arrays.toString(command).replace(",",""));
                if (Platform.get() == Platform.WINDOWS) {
                    command[0] = "\"" + TunnelManager.DATA_FOLDER.getAbsolutePath() + "\\" + command[0] + "\"";
                }
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                // Since LINUX, MACOSX, and WINDOWS are the only options, this will work to only set the directory for Linux and MacOS
                if (Platform.get() != Platform.WINDOWS) {
                    processBuilder.directory(TunnelManager.DATA_FOLDER);
                }
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    TunnelManager.CLOUDFLARE_LOGGER.info(line);
                    if (line.contains("Start Websocket listener")) {
                        // Wait for the websocket to start (this is a hacky solution, but I don't really see a better way)
                        Thread.sleep(250);
                        future.complete(new RunningTunnel(access, process)); // Tunnel was started. Return running tunnel to minecraft client
                    }
                }
            } catch (IOException | InterruptedException exception) {
                Modflared.LOGGER.error("Failed to start cloudflared: " + exception.getMessage(), exception);
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    public void closeTunnel() {
        process.destroy();
    }

    public record Access(String protocol, String hostname, String bindHost, int bindPort) {
        @Contract("_ -> new")
        public static @NotNull Access localWithRandomPort(String host) {
            return new Access("tcp", host, "127.0.0.1", (int) (Math.random() * 10000 + 25565));
        }

        @Contract("_ -> new")
        public String @NotNull [] command(@NotNull File executable) {
            return new String[] {(Platform.get() != Platform.WINDOWS ? "./" : "") + executable.getName(), "access", protocol, "--hostname", hostname, "--url", bindHost + ":" + bindPort};
        }

        @Contract(value = " -> new", pure = true)
        public @NotNull InetSocketAddress tunnelAddress() {
            return new InetSocketAddress(bindHost, bindPort);
        }
    }

}
