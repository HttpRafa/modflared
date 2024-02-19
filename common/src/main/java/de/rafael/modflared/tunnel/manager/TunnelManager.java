package de.rafael.modflared.tunnel.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.rafael.modflared.Modflared;
import de.rafael.modflared.ModflaredPlatform;
import de.rafael.modflared.download.CloudflaredVersion;
import de.rafael.modflared.interfaces.mixin.IClientConnection;
import de.rafael.modflared.tunnel.RunningTunnel;
import de.rafael.modflared.tunnel.TunnelStatus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.client.toast.SystemToast;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.InitialDirContext;
import java.io.*;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class TunnelManager {

    public static final File BASE_FOLDER = ModflaredPlatform.getGameDir().resolve("modflared/").toFile();
    public static final File DATA_FOLDER = new File(BASE_FOLDER, "bin/");
    public static final File FORCED_TUNNELS_FILE = new File(BASE_FOLDER, "forced_tunnels.json");

    public static final Logger CLOUDFLARE_LOGGER = LoggerFactory.getLogger("Cloudflared");

    private final AtomicReference<CloudflaredVersion> cloudflared = new AtomicReference<>();
    private final List<ServerAddress> forcedTunnels = new ArrayList<>();

    private final List<RunningTunnel> runningTunnels = new ArrayList<>();

    public RunningTunnel createTunnel(String host) {
        var binary = this.cloudflared.get();
        if(binary != null) {
            Modflared.LOGGER.info("Starting tunnel to {}", host);
            var process = binary.createTunnel(RunningTunnel.Access.localWithRandomPort(host));
            if(process == null) return null;
            this.runningTunnels.add(process);
            return process;
        } else {
            return null;
        }
    }

    public void closeTunnel(@NotNull RunningTunnel runningTunnel) {
        Modflared.LOGGER.info("Stopping tunnel to {}", runningTunnel.access().tunnelAddress());
        this.runningTunnels.remove(runningTunnel);
        runningTunnel.closeTunnel();
    }

    public void closeTunnels() {
        for (RunningTunnel runningTunnel : this.runningTunnels) {
            runningTunnel.closeTunnel();
        }
    }

    /*
     Check the TXT records for the server to see if it should use a tunnel
     We check for a TXT record on the same subdomain as the server we are connecting to
     This is done by checking if the server has a TXT record with the value "cloudflared-use-tunnel" or "cloudflared-route=<route>"
     If the server has the TXT record "cloudflared-route=<route>", it will use the route as the route for the tunnel
     If the server has the TXT record "cloudflared-use-tunnel", it will use the server address as the route for the tunnel
     If the server has neither of the TXT records, it will not use a tunnel (unless it is in the forced tunnels list)
     */
    public String shouldUseTunnel(String host) throws IOException {
        if (forcedTunnels.stream().anyMatch(serverAddress -> serverAddress.getAddress().equalsIgnoreCase(host))) {
            return host;
        }

        try {
            var properties = new Properties();
            properties.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            InitialDirContext dirContext = new InitialDirContext(properties);
            Attributes attributes = dirContext.getAttributes(host, new String[]{"TXT"});
            Attribute txtRecords = attributes.get("TXT");

            if (txtRecords != null) {
                var iterator = txtRecords.getAll();
                while (iterator.hasMore()) {
                    var record = (String) iterator.next();
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

    public void prepareConnection(@NotNull TunnelStatus status, ClientConnection connection) {
        var tunnelConnection = (IClientConnection) connection;
        if(status.runningTunnel() != null) {
            tunnelConnection.setRunningTunnel(status.runningTunnel());
        }
    }

    public TunnelStatus handleConnect(@NotNull InetSocketAddress address) {
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
        CloudflaredVersion.create().whenComplete((version, throwable) -> {
            if(throwable != null) {
                Modflared.LOGGER.error(throwable.getMessage(), throwable);
            } else {
                version.prepare().whenComplete((unused, throwable1) -> {
                    if(throwable1 != null) {
                        Modflared.LOGGER.error(throwable1.getMessage(), throwable1);
                        displayErrorToast();
                    } else {
                        this.cloudflared.set(version);
                    }
                });
            }
        });
    }

    public void loadForcedTunnels() {
        this.forcedTunnels.clear();
        if (!FORCED_TUNNELS_FILE.exists()) {
            return;
        }

        try {
            JsonArray entriesArray = JsonParser.parseReader(
                    new InputStreamReader(new FileInputStream(FORCED_TUNNELS_FILE))).getAsJsonArray();
            for (JsonElement jsonElement : entriesArray) {
                var serverString = jsonElement.getAsString();

                if (!ServerAddress.isValid(serverString)) {
                    Modflared.LOGGER.error("Invalid server address: {}", serverString);
                    continue;
                }
                forcedTunnels.add(ServerAddress.parse(serverString));
            }
        } catch (Exception exception) {
            Modflared.LOGGER.error("Failed to load forced tunnels", exception);
        }

        Modflared.LOGGER.info("Loaded {} forced tunnels", forcedTunnels.size());
        for (ServerAddress serverAddress : forcedTunnels) {
            Modflared.LOGGER.info(" - {}", serverAddress.getAddress());
        }
    }

    public static void displayErrorToast() {
        MinecraftClient.getInstance().getToastManager().add(new SystemToast(SystemToast.Type.PERIODIC_NOTIFICATION, Text.translatable("gui.toast.title.error"), Text.translatable("gui.toast.body.error")));
    }

}
