package de.rafael.modflared.fabric;

import de.rafael.modflared.Modflared;
import net.fabricmc.api.ClientModInitializer;

public class ModflaredFabricClient implements ClientModInitializer {

    @Override
    public void onInitializeClient() {
        Modflared.init();
    }

}
