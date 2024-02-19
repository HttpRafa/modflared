package de.rafael.modflared.neoforge;

import de.rafael.modflared.platform.LoaderPlatform;
import net.neoforged.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ModflaredPlatformImpl {

    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

    public static LoaderPlatform getPlatform() {
        return LoaderPlatform.NEOFORGE;
    }

}
