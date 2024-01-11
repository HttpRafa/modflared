package de.rafael.modflared.fabric;

import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class ModflaredPlatformImpl {

    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

}
