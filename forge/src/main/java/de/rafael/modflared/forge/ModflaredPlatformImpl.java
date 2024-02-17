package de.rafael.modflared.forge;

import net.minecraftforge.fml.loading.FMLPaths;

import java.nio.file.Path;

public class ModflaredPlatformImpl {

    public static Path getGameDir() {
        return FMLPaths.GAMEDIR.get();
    }

}
