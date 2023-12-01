package de.rafael.modflared.forge;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.rafael.modflared.forge.download.CloudflaredDownload;
import de.rafael.modflared.forge.program.CloudflaredProgram;
import net.minecraft.client.multiplayer.resolver.ServerAddress;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import net.minecraftforge.fml.loading.FMLPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Mod(Modflared.MOD_ID)
public class Modflared {

    public static File BASE_FOLDER;
    public static File DATA_FOLDER;
    public static File FORCED_TUNNELS;

    public static final String MOD_ID = "modflared";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Logger CF_LOGGER = LoggerFactory.getLogger("Cloudflared");


    public static CompletableFuture<CloudflaredProgram> PROGRAM;

    public static final ArrayList<ServerAddress> FORCE_USE_TUNNEL_SERVERS = new ArrayList<>();

    public Modflared() {
        FMLJavaModLoadingContext.get().getModEventBus().addListener(this::setup);
    }

    private void setup(final FMLCommonSetupEvent event) {
        BASE_FOLDER = FMLPaths.GAMEDIR.get().resolve("modflared/").toFile();
        DATA_FOLDER = new File(BASE_FOLDER, "bin/");
        FORCED_TUNNELS = new File(BASE_FOLDER, "forced_tunnels.json");

        PROGRAM = CloudflaredDownload.findAndDownload();

        loadAccess();
    }

    public void loadAccess() {
        if (!Modflared.FORCED_TUNNELS.exists()) {
            Modflared.LOGGER.error("No accessData to load found!");
            return;
        }

        try {
            JsonArray entriesArray = JsonParser.parseReader(
                    new InputStreamReader(new FileInputStream(Modflared.FORCED_TUNNELS))).getAsJsonArray();
            for (JsonElement jsonElement : entriesArray) {
                var serverString = jsonElement.getAsString();

                if (!ServerAddress.isValidAddress(serverString)) {
                    LOGGER.error("Invalid server address: {}", serverString);
                    continue;
                }
                Modflared.FORCE_USE_TUNNEL_SERVERS.add(ServerAddress.parseString(serverString));
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to load forced tunnels: " + exception.getMessage(), exception);
        }

        LOGGER.info("Loaded {} forced tunnels", Modflared.FORCE_USE_TUNNEL_SERVERS.size());
        for (ServerAddress serverAddress : Modflared.FORCE_USE_TUNNEL_SERVERS) {
            LOGGER.info(" - {}", serverAddress.getHost());
        }
    }
}
