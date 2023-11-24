package de.rafael.modflared.fabric;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import de.rafael.modflared.fabric.download.CloudflaredDownload;
import de.rafael.modflared.fabric.program.CloudflaredProgram;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.network.ServerAddress;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.Attribute;
import javax.naming.directory.Attributes;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Properties;
import java.util.concurrent.CompletableFuture;

public class Modflared implements ClientModInitializer {

    public static File BASE_FOLDER;
    public static File DATA_FOLDER;
    public static File FORCED_TUNNELS;

    public static final String MOD_ID = "modflared";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
    public static final Logger CF_LOGGER = LoggerFactory.getLogger("Cloudflared");


    public static CompletableFuture<CloudflaredProgram> PROGRAM;

    public static final ArrayList<ServerAddress> FORCE_USE_TUNNEL_SERVERS = new ArrayList<>();


    @Override
    public void onInitializeClient() {
        BASE_FOLDER = FabricLoader.getInstance().getGameDir().resolve("modflared/").toFile();
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

                if (!ServerAddress.isValid(serverString)) {
                    LOGGER.error("Invalid server address: {}", serverString);
                    continue;
                }
                Modflared.FORCE_USE_TUNNEL_SERVERS.add(ServerAddress.parse(serverString));
            }
        } catch (Exception exception) {
            LOGGER.error("Failed to load forced tunnels: " + exception.getMessage(), exception);
        }

        LOGGER.info("Loaded {} forced tunnels", Modflared.FORCE_USE_TUNNEL_SERVERS.size());
        for (ServerAddress serverAddress : Modflared.FORCE_USE_TUNNEL_SERVERS) {
            LOGGER.info(" - {}", serverAddress.getAddress());
        }
    }
}
