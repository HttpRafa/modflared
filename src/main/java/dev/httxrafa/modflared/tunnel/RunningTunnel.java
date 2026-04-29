package dev.httxrafa.modflared.tunnel;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.binary.Cloudflared;
import dev.httxrafa.modflared.tunnel.manager.TunnelManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.zip.CRC32;

public class RunningTunnel {

    private final Access access;
    private final Process process;

    public RunningTunnel(Access access, Process process) {
        this.access = access;
        this.process = process;
    }

    public Access getAccess() {
        return access;
    }

    public Process getProcess() {
        return process;
    }

    public static CompletableFuture<RunningTunnel> createTunnel(final Cloudflared binary, final Access access) {
        final CompletableFuture<RunningTunnel> future = new CompletableFuture<RunningTunnel>();
        Modflared.EXECUTOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    ProcessBuilder processBuilder = new ProcessBuilder(binary.buildCommand(access));
                    if (!isWindows()) {
                        processBuilder.directory(TunnelManager.DATA_FOLDER);
                    }
                    processBuilder.redirectErrorStream(true);
                    Process process = processBuilder.start();
                    BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8));

                    String line;
                    while ((line = reader.readLine()) != null) {
                        TunnelManager.CLOUDFLARE_LOGGER.info(line);
                        if (line.contains("Start Websocket listener")) {
                            Thread.sleep(250L);
                            future.complete(new RunningTunnel(access, process));
                        }
                    }

                    if (!future.isDone()) {
                        future.completeExceptionally(new IOException("cloudflared exited before opening websocket listener"));
                    }
                } catch (IOException e) {
                    Modflared.LOGGER.error("Failed to start cloudflared", e);
                    future.completeExceptionally(e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    Modflared.LOGGER.error("Interrupted while starting cloudflared", e);
                    future.completeExceptionally(e);
                }
            }
        });
        return future;
    }

    public void closeTunnel() {
        process.destroy();
    }

    public static boolean isWindows() {
        return System.getProperty("os.name").toLowerCase().contains("windows");
    }

    public static class Access {
        private final String protocol;
        private final String hostname;
        private final InetSocketAddress tunnelAddress;

        public Access(String protocol, String hostname, InetSocketAddress tunnelAddress) {
            this.protocol = protocol;
            this.hostname = hostname;
            this.tunnelAddress = tunnelAddress;
        }

        public static Access localWithRandomPort(String host) {
            return new Access("tcp", host, new InetSocketAddress("127.0.0.1", computePort(host)));
        }

        public String[] command(String fileName, boolean prefix) {
            String executable = prefix && !isWindows() ? "./" + fileName : fileName;
            String host = hostname;
            int destinationPort = -1;
            int colonIndex = hostname.lastIndexOf(':');
            if (colonIndex > 0) {
                host = hostname.substring(0, colonIndex);
                try {
                    destinationPort = Integer.parseInt(hostname.substring(colonIndex + 1));
                } catch (NumberFormatException ignored) {
                    // Not a valid port, keep full string as hostname
                    host = hostname;
                    destinationPort = -1;
                }
            }
            if (destinationPort > 0) {
                return new String[] {
                        executable,
                        "access",
                        protocol,
                        "--hostname",
                        host,
                        "--destination",
                        host + ":" + destinationPort,
                        "--url",
                        tunnelAddress.getHostString() + ":" + tunnelAddress.getPort()
                };
            }
            return new String[] {
                    executable,
                    "access",
                    protocol,
                    "--hostname",
                    host,
                    "--url",
                    tunnelAddress.getHostString() + ":" + tunnelAddress.getPort()
            };
        }

        public static int computePort(String host) {
            final int minPort = 25565;
            final int maxPort = 65530;
            final int range = maxPort - minPort + 1;

            CRC32 crc32 = new CRC32();
            crc32.update(host.getBytes(StandardCharsets.UTF_8));
            long hash = crc32.getValue();

            return (int) ((hash % range) + minPort);
        }

        public String getProtocol() {
            return protocol;
        }

        public String getHostname() {
            return hostname;
        }

        public InetSocketAddress getTunnelAddress() {
            return tunnelAddress;
        }
    }
}
