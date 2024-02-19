package de.rafael.modflared;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import de.rafael.modflared.platform.LoaderPlatform;
import de.rafael.modflared.tunnel.manager.TunnelManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Modflared {

    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping().setPrettyPrinting().serializeNulls().create();

    public static final String MOD_ID = "modflared";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final LoaderPlatform PLATFORM = ModflaredPlatform.getPlatform();

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
