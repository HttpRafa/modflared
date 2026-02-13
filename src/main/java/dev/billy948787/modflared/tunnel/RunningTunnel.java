package dev.billy948787.modflared.tunnel;

import com.github.bsideup.jabel.Desugar;
import dev.billy948787.modflared.Modflared;
import dev.billy948787.modflared.binary.Cloudflared;
import dev.billy948787.modflared.tunnel.manager.TunnelManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.LWJGLUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.concurrent.CompletableFuture;
import java.util.zip.CRC32;

public @Desugar record RunningTunnel(Access access, Process process) {

    public static @NotNull CompletableFuture<RunningTunnel> createTunnel(@NotNull Cloudflared binary, @NotNull Access access) {
        var future = new CompletableFuture<RunningTunnel>();
        Modflared.EXECUTOR.execute(() -> {
            try {
                ProcessBuilder processBuilder = new ProcessBuilder(binary.buildCommand(access));
                // Since LINUX, MACOSX, and WINDOWS are the only options, this will work to only set the directory for Linux and MacOS
                if (LWJGLUtil.getPlatform() != LWJGLUtil.PLATFORM_WINDOWS) {
                    processBuilder.directory(TunnelManager.DATA_FOLDER);
                }
                processBuilder.redirectErrorStream(true);
                var process = processBuilder.start();

                var reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

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
                Modflared.LOGGER.error("Failed to start cloudflared", exception);
                future.completeExceptionally(exception);
            }
        });
        return future;
    }

    public void closeTunnel() {
        process.destroy();
    }

    public @Desugar record Access(String protocol, String hostname, InetSocketAddress tunnelAddress) {
        @Contract("_ -> new")
        public static @NotNull Access localWithRandomPort(String host) {
            return new Access("tcp", host, new InetSocketAddress("127.0.0.1", computePort(host)));
        }

        public String @NotNull [] command(@NotNull String fileName, boolean prefix) {
            return new String[]{(prefix && LWJGLUtil.getPlatform() != LWJGLUtil.PLATFORM_WINDOWS ? "./" : "") + fileName, "access", protocol, "--hostname", hostname, "--url", tunnelAddress.getHostString() + ":" + tunnelAddress.getPort()};
        }

        public static int computePort(@NotNull String host) {
            final int MIN_PORT = 25565;
            final int MAX_PORT = 65530;
            final int RANGE = MAX_PORT - MIN_PORT + 1;

            CRC32 crc32 = new CRC32();
            crc32.update(host.getBytes());
            long hash = crc32.getValue();

            return (int) ((hash % RANGE) + MIN_PORT);
        }
    }

}
