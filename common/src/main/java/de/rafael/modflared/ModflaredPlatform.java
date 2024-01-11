package de.rafael.modflared;

import dev.architectury.injectables.annotations.ExpectPlatform;

import java.nio.file.Path;

public class ModflaredPlatform {

    @ExpectPlatform
    public static Path getGameDir() {
        throw new AssertionError();
    }

}
