package dev.httxrafa.modflared;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import dev.httxrafa.modflared.tunnel.manager.TunnelManager;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

@Mod(
        modid = Modflared.MOD_ID,
        name = Modflared.MOD_NAME,
        version = Modflared.VERSION,
        clientSideOnly = true
)
public class Modflared {

    public static final String MOD_ID = "modflared";
    public static final String MOD_NAME = "Modflared";
    public static final String VERSION = "1.12.2-legacy.1";

    public static final Logger LOGGER = LogManager.getLogger(MOD_NAME);
    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    public static final Gson GSON = new GsonBuilder()
            .disableHtmlEscaping()
            .setPrettyPrinting()
            .serializeNulls()
            .create();

    public static final TunnelManager TUNNEL_MANAGER = new TunnelManager();

    @EventHandler
    public void init(FMLInitializationEvent event) {
        TUNNEL_MANAGER.initDirectories();
        TUNNEL_MANAGER.prepareBinary();
        TUNNEL_MANAGER.loadForcedTunnels();

        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            @Override
            public void run() {
                TUNNEL_MANAGER.closeTunnels();
                EXECUTOR.shutdownNow();
            }
        }, "Modflared Shutdown"));

        LOGGER.info("Modflared client setup complete");
    }
}
