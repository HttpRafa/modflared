package de.rafael.modflared;

import de.rafael.modflared.platform.LoaderPlatform;
import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public class ModflaredPlatform {

    @ExpectPlatform
    public static Path getGameDir() {
        throw new AssertionError();
    }

    @ExpectPlatform
    public static LoaderPlatform getPlatform() {
        throw new AssertionError();
    }

}
