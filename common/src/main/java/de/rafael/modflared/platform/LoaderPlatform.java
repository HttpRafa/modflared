package de.rafael.modflared.platform;

public enum LoaderPlatform {

    FABRIC,
    NEOFORGE;

    public boolean isNeoForge() {
        return this == NEOFORGE;
    }

}
