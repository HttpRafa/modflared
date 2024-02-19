package de.rafael.modflared.fabric;

import de.rafael.modflared.platform.LoaderPlatform;
import net.fabricmc.loader.api.FabricLoader;

import java.nio.file.Path;

public class ModflaredPlatformImpl {

    public static Path getGameDir() {
        return FabricLoader.getInstance().getGameDir();
    }

    public static LoaderPlatform getPlatform() {
        return LoaderPlatform.FABRIC;
    }

}
