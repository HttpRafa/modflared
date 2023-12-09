package de.rafael.modflared.fabric;

import de.rafael.modflared.fabric.tunnel.manager.TunnelManager;
import net.fabricmc.api.ClientModInitializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Modflared implements ClientModInitializer {

    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public static final String MOD_ID = "modflared";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static final TunnelManager TUNNEL_MANAGER = new TunnelManager();

    @Override
    public void onInitializeClient() {
        TUNNEL_MANAGER.prepareBinary();
        TUNNEL_MANAGER.loadForcedTunnels();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            TUNNEL_MANAGER.closeTunnels();
            EXECUTOR.shutdownNow();
        }));
    }

}
