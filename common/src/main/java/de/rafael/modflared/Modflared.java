package de.rafael.modflared;

//------------------------------
//
// This class was developed by Rafael K.
// On 10/31/2022 at 11:17 PM
// In the project cloudflared
//
//------------------------------

import de.rafael.modflared.download.CloudflaredDownload;
import dev.architectury.platform.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Modflared {

    public static File BASE_FOLDER;
    public static File DATA_FOLDER;
    public static File ACCESS_FILE;

    public static final String MOD_ID = "modflared";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        BASE_FOLDER = Platform.getGameFolder().resolve("modflared/").toFile();
        DATA_FOLDER = new File(BASE_FOLDER, "bin/");
        ACCESS_FILE = new File(BASE_FOLDER, "access.json");

        CloudflaredDownload.findAndDownload(program -> {
            program.loadAccess();
            program.startAccess();
        });
    }

}
