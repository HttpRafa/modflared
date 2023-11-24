package de.rafael.modflared.fabric.program;

//------------------------------
//
// This class was developed by Rafael K.
// On 10/31/2022 at 11:29 PM
// In the project cloudflared
//
//------------------------------

import de.rafael.modflared.fabric.Modflared;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.Platform;

import java.io.*;
import java.lang.ProcessBuilder.Redirect;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class CloudflaredProgram {

    public static AtomicInteger ACCESS_COUNT = new AtomicInteger(0);

    private final File executableFile;

    private @Nullable Process process = null;

    public CloudflaredProgram(File executableFile) {
        this.executableFile = executableFile;

        Runtime.getRuntime().addShutdownHook(new Thread(this::closeTunnel));
    }

    public void closeTunnel() {
        if (process == null) {
            return;
        }
        process.destroy();
    }

    public CompletableFuture<Void> startAccess(Access access) {
        var future = new CompletableFuture<Void>();
        new Thread(() -> {
            try {
                String[] command = access.command(this);
                Modflared.LOGGER.info(Arrays.toString(command).replace(",",""));
                if (Platform.get() == Platform.WINDOWS) {
                    command[0] = "\"" + Modflared.DATA_FOLDER.getAbsolutePath() + "\\" + command[0] + "\"";
                }
                ProcessBuilder processBuilder = new ProcessBuilder(command);
                // Since LINUX, MACOSX, and WINDOWS are the only options, this will work to only set the directory for Linux and MacOS
                if (Platform.get() != Platform.WINDOWS) {
                    processBuilder.directory(Modflared.DATA_FOLDER);
                }
                processBuilder.redirectErrorStream(true);
                Process process = processBuilder.start();
                this.process = process;

                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));

                String line;
                while ((line = reader.readLine()) != null) {
                    Modflared.CF_LOGGER.info(line);
                    if (line.contains("Start Websocket listener")) {
                        // Wait for the websocket to start (this is a hacky solution, but I don't really see a better way)
                        Thread.sleep(200);
                        future.complete(null);
                    }
                }

            } catch (IOException | InterruptedException exception) {
                Modflared.LOGGER.error("Failed to start cloudflared: " + exception.getMessage(), exception);
                future.completeExceptionally(exception);
            }
        }, "Access#" + ACCESS_COUNT.getAndIncrement()).start();
        return future;
    }

    public File getExecutableFile() {
        return executableFile;
    }

    public static class Access {
        private final String protocol;
        private final String hostname;
        private final String bind_host;
        private final int bind_port;

        public Access(String protocol, String hostname, String bind_host, int bind_port) {
            this.protocol = protocol;
            this.hostname = hostname;
            this.bind_host = bind_host;
            this.bind_port = bind_port;
        }

        public String[] command(CloudflaredProgram program) {
            return new String[] {(Platform.get() != Platform.WINDOWS ? "./" : "") + program.getExecutableFile().getName(), "access", protocol, "--hostname", hostname, "--url", bind_host + ":" + bind_port};
        }
        public String getProtocol() {
            return protocol;
        }

        public String getHostname() {
            return hostname;
        }

        public String getBind_Host() {
            return bind_host;
        }

        public int getBind_Port() {
            return bind_port;
        }

    }

}
