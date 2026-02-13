package dev.billy948787.modflared;

import cpw.mods.fml.common.event.FMLPreInitializationEvent;

public class ClientProxy extends CommonProxy {

    @Override
    public void preInit(FMLPreInitializationEvent event) {
        var configDir = event.getModConfigurationDirectory();

        Modflared.TUNNEL_MANAGER.initDirectories();
        Modflared.TUNNEL_MANAGER.prepareBinary();
        Modflared.TUNNEL_MANAGER.loadForcedTunnels();

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            Modflared.TUNNEL_MANAGER.closeTunnels();
            Modflared.EXECUTOR.shutdownNow();
        }));

        super.preInit(event);
    }
}
