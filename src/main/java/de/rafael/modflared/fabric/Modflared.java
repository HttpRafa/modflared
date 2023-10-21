package de.rafael.modflared.fabric;

import de.rafael.modflared.fabric.download.CloudflaredDownload;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Modflared implements ClientModInitializer {

    public static File BASE_FOLDER;
    public static File DATA_FOLDER;
    public static File ACCESS_FILE;

    public static final String MOD_ID = "modflared";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    @Override
    public void onInitializeClient() {
        BASE_FOLDER = FabricLoader.getInstance().getGameDir().resolve("modflared/").toFile();
        DATA_FOLDER = new File(BASE_FOLDER, "bin/");
        ACCESS_FILE = new File(BASE_FOLDER, "access.json");

        CloudflaredDownload.findAndDownload(program -> {
            program.loadAccess();
            program.startAccess();
        });
    }
}
