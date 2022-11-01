package de.rafael.modflared;

//------------------------------
//
// This class was developed by Rafael K.
// On 10/31/2022 at 11:17 PM
// In the project cloudflared
//
//------------------------------

import de.rafael.modflared.download.CloudflaredDownload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;

public class Modflared {

    public static File BASE_FOLDER = new File("modflared/");
    public static File DATA_FOLDER = new File(BASE_FOLDER, "bin/");
    public static File ACCESS_FILE = new File(BASE_FOLDER, "access.json");

    public static final String MOD_ID = "modflared";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static void init() {
        CloudflaredDownload.findAndDownload(program -> {
            program.loadAccess();
            program.startAccess();
        });
    }

}
