package dev.httxrafa.modflared.tunnel.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.binary.Cloudflared;
import dev.httxrafa.modflared.interfaces.mixin.IConnection;
import dev.httxrafa.modflared.tunnel.RunningTunnel;
import dev.httxrafa.modflared.tunnel.TunnelStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ServerAddress;
import net.minecraft.network.NetworkManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.io.*;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class TunnelManager {

    public static final File BASE_FOLDER = new File(Minecraft.getMinecraft().gameDir, "modflared");
    public static final File DATA_FOLDER = new File(BASE_FOLDER, "bin");
    public static final File FORCED_TUNNELS_FILE = new File(BASE_FOLDER, "forced_tunnels.json");

    public static final Logger CLOUDFLARE_LOGGER = LogManager.getLogger("Cloudflared");

    private final AtomicReference<Cloudflared> cloudflared = new AtomicReference<>();
    private final List<ServerAddress> forcedTunnels = new ArrayList<>();

    private final List<RunningTunnel> runningTunnels = new ArrayList<>();

    public void initDirectories() {
        DATA_FOLDER.mkdirs();
    }

    public RunningTunnel createTunnel(String host) {
        Cloudflared binary = this.cloudflared.get();
        if (binary != null) {
            Modflared.LOGGER.info("Starting tunnel to {}", host);
            RunningTunnel process = binary.createTunnel(RunningTunnel.Access.localWithRandomPort(host));
            if (process == null) return null;
            this.runningTunnels.add(process);
            return process;
        } else {
            return null;
        }
    }

    public void closeTunnel(RunningTunnel runningTunnel) {
        Modflared.LOGGER.info("Stopping tunnel to {}", runningTunnel.getAccess().getTunnelAddress());
        this.runningTunnels.remove(runningTunnel);
        runningTunnel.closeTunnel();
    }

    public void closeTunnels() {
        for (RunningTunnel runningTunnel : this.runningTunnels) {
            runningTunnel.closeTunnel();
        }
    }

    /**
     * Check the TXT records for the server to see if it should use a tunnel We check for a TXT record on the same subdomain as
     * the server we are connecting to.
     * <p>
     * This is done by checking if the server has a TXT record with the value "cloudflared-use-tunnel" or
     * "cloudflared-route=<route>"
     * <li>
     * If the server has the TXT record "cloudflared-route=<route>", it will use the route as the route for the tunnel.
     * </li>
     * <li>
     * If the server has the TXT record "cloudflared-use-tunnel", it will use the server address as the route for the tunnel
     * </li>
     * <li>
     * If the server has neither of the TXT records, it will not use a tunnel (unless it is in the forced tunnels list)
     * </li>
     *
     * @return The route to use for the tunnel, or null if the tunnel should not be used
     * @throws IOException If an error occurs while resolving the DNS TXT records
     */
    public String shouldUseTunnel(String host) throws IOException {
        for (ServerAddress serverAddress : forcedTunnels) {
            if (serverAddress.getIP().equalsIgnoreCase(host)) {
                return host;
            }
        }

        // Check if the host is an IP address
        if (!isHost(host)) {
            return null;
        }
        try {
            Properties properties = new Properties();
            properties.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            InitialDirContext dirContext = new InitialDirContext(properties);
            Attributes attributes = dirContext.getAttributes(host, new String[]{"TXT"});
            Attribute txtRecords = attributes.get("TXT");

            if (txtRecords != null) {
                javax.naming.NamingEnumeration<?> iterator = txtRecords.getAll();
                while (iterator.hasMore()) {
                    String record = ((String) iterator.next()).replaceAll("\"", "");
                    if (record.startsWith("cloudflared-route=")) {
                        return record.replace("cloudflared-route=", "");
                    } else if (record.equals("cloudflared-use-tunnel")) {
                        return host;
                    }
                }
            }
        } catch (NamingException | UncheckedIOException exception) {
            Modflared.LOGGER.error("Failed to resolve DNS TXT entries: " + exception.getMessage(), exception);

            Modflared.LOGGER.error(
                    "Modflared was unable to determine if a tunnel should be used for {} and is defaulting to not " +
                            "using a tunnel", host);
            Modflared.LOGGER.error(
                    "If you believe you need a tunnel for this server, please add it to the forced tunnels list in " +
                            "the modflared folder in your game directory.");
            Modflared.LOGGER.error("For more information, please see the modflared documentation at " +
                    "https://github.com/HttpRafa/modflared?tab=readme-ov-file#versions-100-and-onward");

            throw new IOException(exception);
        }

        return null;
    }

    /**
     * @param ip The IP address or hostname
     * @return Whether the string is a hostname
     * @see <a href="https://stackoverflow.com/a/57612280">How do you tell whether a string is an IP or a hostname</a>
     */
    private boolean isHost(String ip) {
        if (ip.trim().equalsIgnoreCase("localhost")) {
            return false;
        }
        try {
            InetAddress[] ips = InetAddress.getAllByName(ip);
            // #getAllByName will return an InetAddress that is the same as the input if it is an IP address,
            // so we can check if the input is an IP address by comparing the InetAddress to the input
            return !(ips.length == 1 && ips[0].getHostAddress().equals(ip));
        } catch (UnknownHostException e) {
            return true;
        }
    }

    public void prepareConnection(TunnelStatus status, NetworkManager connection) {
        IConnection tunnelConnection = (IConnection) connection;
        if (status.getRunningTunnel() != null) {
            tunnelConnection.setRunningTunnel(status.getRunningTunnel());
        }
    }

    public TunnelStatus handleConnect(InetSocketAddress address) {
        if(this.cloudflared.get() == null) {
            Modflared.LOGGER.warn("Modflared is not ready yet, ignoring all connections.");
            return new TunnelStatus(null, TunnelStatus.State.DONT_USE);
        }

        RunningTunnel runningTunnel = null;
        TunnelStatus.State state = TunnelStatus.State.DONT_USE;

        String route = null;
        try {
            route = Modflared.TUNNEL_MANAGER.shouldUseTunnel(address.getHostName());
        } catch (IOException ignored) {
            state = TunnelStatus.State.FAILED_TO_DETERMINE;
        }

        if (route != null) {
            runningTunnel = Modflared.TUNNEL_MANAGER.createTunnel(route);
            state = TunnelStatus.State.USE;
        }
        return new TunnelStatus(runningTunnel, state);
    }

    public void prepareBinary() {
        Cloudflared.create().whenComplete(new java.util.function.BiConsumer<Cloudflared, Throwable>() {
            @Override
            public void accept(Cloudflared version, Throwable throwable) {
                if (throwable != null) {
                    Modflared.LOGGER.error(throwable.getMessage(), throwable);
                } else {
                    version.prepare().whenComplete(new java.util.function.BiConsumer<Void, Throwable>() {
                        @Override
                        public void accept(Void unused, Throwable throwable1) {
                            if (throwable1 != null) {
                                Modflared.LOGGER.error(throwable1.getMessage(), throwable1);
                                displayErrorToast();
                            } else {
                                TunnelManager.this.cloudflared.set(version);
                            }
                        }
                    });
                }
            }
        });
    }

    public void loadForcedTunnels() {
        this.forcedTunnels.clear();
        if (!FORCED_TUNNELS_FILE.exists()) {
            return;
        }

        try {
            JsonArray entriesArray = new JsonParser().parse(
                    new InputStreamReader(new FileInputStream(FORCED_TUNNELS_FILE))).getAsJsonArray();
            for (JsonElement jsonElement : entriesArray) {
                String serverString = jsonElement.getAsString();

                ServerAddress serverAddress = parseServerAddress(serverString);
                if (serverAddress == null) {
                    continue;
                }
                forcedTunnels.add(serverAddress);
            }
        } catch (Exception exception) {
            Modflared.LOGGER.error("Failed to load forced tunnels", exception);
        }

        Modflared.LOGGER.info("Loaded {} forced tunnels", forcedTunnels.size());
        for (ServerAddress serverAddress : forcedTunnels) {
            Modflared.LOGGER.info(" - {}", serverAddress.getIP());
        }
    }

    private ServerAddress parseServerAddress(String serverString) {
        try {
            return ServerAddress.fromString(serverString);
        } catch (RuntimeException exception) {
            Modflared.LOGGER.error("Invalid server address: " + serverString, exception);
            return null;
        }
    }

    public static void displayErrorToast() {
        Modflared.LOGGER.error("Modflared setup failed. Check the log for cloudflared setup details.");
    }

}
