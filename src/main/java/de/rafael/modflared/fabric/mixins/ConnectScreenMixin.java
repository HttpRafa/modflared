package de.rafael.modflared.fabric.mixins;

import de.rafael.modflared.fabric.Modflared;
import de.rafael.modflared.fabric.program.CloudflaredProgram.Access;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.LocalCapture;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.ExecutionException;

@Mixin(targets = "net.minecraft.client.gui.screen.ConnectScreen$1")
public abstract class ConnectScreenMixin implements Runnable {
    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Ljava/util/Optional;get()Ljava/lang/Object;"))
    Object connect(Optional<InetSocketAddress> optional) throws ExecutionException, InterruptedException {

        InetSocketAddress address = optional.get();
        Modflared.LOGGER.info("Connecting to " + address);

        List<String> txtRecords = new ArrayList<>();

        try {
            // Check the TXT records for the server to see if it should use a tunnel
            // We check for a TXT record on the same subdomain as the server we are connecting to
            // This is done by checking if the server has a TXT record with the value "cloudflared-use-tunnel" or "cloudflared-route=<route>"
            // If the server has the TXT record "cloudflared-route=<route>", it will use the route as the route for the tunnel
            // If the server has the TXT record "cloudflared-use-tunnel", it will use the server address as the route for the tunnel
            // If the server has neither of the TXT records, it will not use a tunnel (unless it is in the forced tunnels list)

            var env = new Properties();
            env.put("java.naming.factory.initial",
                    "com.sun.jndi.dns.DnsContextFactory");
            DirContext dirContext = new InitialDirContext(env);
            Attributes attrs = dirContext.getAttributes(address.getHostName(), new String[] { "TXT" });
            Attribute txt = attrs.get("TXT");

            if (txt != null) {
                NamingEnumeration<?> e = txt.getAll();

                while (e.hasMore()) {
                    txtRecords.add((String) e.next());
                }
            }
        } catch (NamingException e) {
            Modflared.LOGGER.info("Failed to get TXT Records, for the inputed server", e);
        }


        // Check if the server is in the forced tunnels list
        boolean useTunnel =
                Modflared.FORCE_USE_TUNNEL_SERVERS.stream()
                        .anyMatch(serverAddress -> serverAddress.getAddress().equalsIgnoreCase(address.getHostName()));
        String route = "";

        for (String txtRecord : txtRecords) {
            if (txtRecord.startsWith("cloudflared-route=")) {
                route = txtRecord.replace("cloudflared-route=", "");
                useTunnel = true;
            } else if (txtRecord.equals("cloudflared-use-tunnel")) {
                useTunnel = true;
            }
        }

        if (useTunnel) {
            if (route.isEmpty()) {
                Modflared.LOGGER.info("Using tunnel for " + address.getHostName());
                route = address.getHostName();
            } else {
                Modflared.LOGGER.info("Using tunnel for " + address.getHostName() + " with route " + route);
            }

            // Wait for cloudflared to download
            // Will also throw an exception if cloudflared failed to download
            var cloudflaredProgram = Modflared.PROGRAM.get();


            var port = (int) (Math.random() * 10000 + 25565);
            cloudflaredProgram.startAccess(new Access("tcp", route, "localhost", port)).get();

            return new InetSocketAddress("localhost", port); // Return the new address to connect to
        } else {
            Modflared.LOGGER.info("Not using tunnel for " + address.getHostName());
        }
        return optional.get(); // Return the original address
    }
}
