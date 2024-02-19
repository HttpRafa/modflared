package de.rafael.modflared.platform;

public enum LoaderPlatform {

    FABRIC,
    FORGE;

    public boolean isForge() {
        return this == FORGE;
    }

}
