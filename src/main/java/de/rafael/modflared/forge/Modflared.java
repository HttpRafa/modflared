package de.rafael.modflared.forge;

import de.rafael.modflared.forge.download.CloudflaredDownload;
import net.minecraft.client.Minecraft;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import org.apache.logging.log4j.Logger;

import java.io.File;

@Mod(modid = Modflared.MOD_ID, name = Modflared.NAME, version = Modflared.VERSION)
public class Modflared {

    public static Logger LOGGER;

    public static final String MOD_ID = "modflared";
    public static final String NAME = "Modflared";
    public static final String VERSION = "0.1.3";

    public static File BASE_FOLDER;
    public static File DATA_FOLDER;
    public static File ACCESS_FILE;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent event) {
        LOGGER = event.getModLog();
    }

    @Mod.EventHandler
    public void init(FMLInitializationEvent event) {
        BASE_FOLDER = Minecraft.getMinecraft().mcDataDir.toPath().resolve("modflared/").toFile();
        DATA_FOLDER = new File(BASE_FOLDER, "bin/");
        ACCESS_FILE = new File(BASE_FOLDER, "access.json");

        CloudflaredDownload.findAndDownload(program -> {
            program.loadAccess();
            program.startAccess();
        });
    }

}
