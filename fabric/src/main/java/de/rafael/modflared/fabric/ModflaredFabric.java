package de.rafael.modflared.fabric;

import de.rafael.modflared.Modflared;
import net.fabricmc.api.ModInitializer;

public class ModflaredFabric implements ModInitializer {

    @Override
    public void onInitialize() {
        Modflared.init();
    }

}
