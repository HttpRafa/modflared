package dev.billy948787.modflared;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.SidedProxy;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import dev.billy948787.modflared.tunnel.manager.TunnelManager;

@Mod(
    modid = Modflared.MOD_ID,
    version = Tags.VERSION,
    name = "Modflared",
    acceptedMinecraftVersions = "[1.7.10]",
    acceptableRemoteVersions = "*")
public class Modflared {

    public static final ExecutorService EXECUTOR = Executors.newCachedThreadPool();
    public static final Gson GSON = new GsonBuilder().disableHtmlEscaping()
        .setPrettyPrinting()
        .serializeNulls()
        .create();

    public static final String MOD_ID = "modflared";
    public static final Logger LOGGER = LogManager.getLogger(MOD_ID);

    public static final TunnelManager TUNNEL_MANAGER = new TunnelManager();

    @SidedProxy(
        clientSide = "dev.billy948787.modflared.ClientProxy",
        serverSide = "dev.billy948787.modflared.CommonProxy")
    public static CommonProxy proxy;

    @Mod.EventHandler
    // preInit "Run before anything else. Read your config, create blocks, items, etc, and register them with the
    // GameRegistry." (Remove if not needed)
    public void preInit(FMLPreInitializationEvent event) {
        proxy.preInit(event);
    }
}
