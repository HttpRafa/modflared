package de.rafael.modflared;

import de.rafael.modflared.tunnel.manager.TunnelManager;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Modflared {

    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();

    public static final String MOD_ID = "modflared";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final TunnelManager TUNNEL_MANAGER = new TunnelManager();

    public static void init() {
        TUNNEL_MANAGER.prepareBinary();
        TUNNEL_MANAGER.loadForcedTunnels();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            TUNNEL_MANAGER.closeTunnels();
            EXECUTOR.shutdownNow();
        }));
    }

}
