package de.rafael.modflared.fabric.tunnel.manager;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.rafael.modflared.fabric.Modflared;
import de.rafael.modflared.fabric.download.CloudflaredBinary;
import de.rafael.modflared.fabric.tunnel.RunningTunnel;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ServerAddress;
import net.minecraft.network.ClientConnection;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.concurrent.atomic.AtomicReference;

public class TunnelManager {

    public static final File BASE_FOLDER = FabricLoader.getInstance().getGameDir().resolve("modflared/").toFile();
    public static final File DATA_FOLDER = new File(BASE_FOLDER, "bin/");
    public static final File FORCED_TUNNELS_FILE = new File(BASE_FOLDER, "forced_tunnels.json");

    public static final Logger CLOUDFLARE_LOGGER = LoggerFactory.getLogger("Cloudflared");

    private final AtomicReference<CloudflaredBinary> binary = new AtomicReference<>();
    private final List<ServerAddress> forcedTunnels = new ArrayList<>();

    private final List<RunningTunnel> runningTunnels = new ArrayList<>();

    public RunningTunnel createTunnel(String host) {
        var binary = this.binary.get();
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
    public String shouldUseTunnel(String host) {
        if(forcedTunnels.stream().anyMatch(serverAddress -> serverAddress.getAddress().equalsIgnoreCase(host))) {
            return host;
        }

        try {
            var properties = new Properties();
            properties.put("java.naming.factory.initial", "com.sun.jndi.dns.DnsContextFactory");
            DirContext dirContext = new InitialDirContext(properties);
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
        } catch (NamingException exception) {
            Modflared.LOGGER.error("Failed to resolve DNS TXT entries: " + exception.getMessage(), exception);
        }

        return null;
    }

    public InetSocketAddress handleConnect(@NotNull InetSocketAddress address, ClientConnection connection) {
        var tunnelConnection = (TunnelManager.Connection) connection;

        var route = Modflared.TUNNEL_MANAGER.shouldUseTunnel(address.getHostName());
        if(route != null) {
            tunnelConnection.setRunningTunnel(Modflared.TUNNEL_MANAGER.createTunnel(route));
            return tunnelConnection.getRunningTunnel().access().tunnelAddress();
        }
        return address;
    }

    public void prepareBinary() {
        CloudflaredBinary.findAndDownload().whenComplete((cloudflaredBinary, throwable) -> {
            if(throwable != null) {
                Modflared.LOGGER.error(throwable.getMessage(), throwable);
            } else {
                this.binary.set(cloudflaredBinary);
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
            Modflared.LOGGER.error("Failed to load forced tunnels: " + exception.getMessage(), exception);
        }

        Modflared.LOGGER.info("Loaded {} forced tunnels", forcedTunnels.size());
        for (ServerAddress serverAddress : forcedTunnels) {
            Modflared.LOGGER.info(" - {}", serverAddress.getAddress());
        }
    }
    
    public interface Connection {
        RunningTunnel getRunningTunnel();
        void setRunningTunnel(RunningTunnel runningTunnel);
    }

}
