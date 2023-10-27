package de.rafael.modflared.forge.program;

//------------------------------
//
// This class was developed by Rafael K.
// On 10/31/2022 at 11:29 PM
// In the project cloudflared
//
//------------------------------

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import de.rafael.modflared.forge.Modflared;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class CloudflaredProgram {

    public static int ACCESS_COUNT = 0;

    private final File executableFile;

    private final List<Access> accesses = new ArrayList<>();
    private final List<Process> processes = new ArrayList<>();

    public CloudflaredProgram(File executableFile) {
        this.executableFile = executableFile;

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            for (Process process : processes) {
                process.destroy();
            }
        }));
    }

    public void loadAccess() {
        if(!Modflared.ACCESS_FILE.exists()) {
            Modflared.LOGGER.error("No accessData to load found!");
            return;
        }
        accesses.clear();

        try(FileInputStream inputStream = new FileInputStream(Modflared.ACCESS_FILE)) {
            JsonArray entriesArray = new JsonParser().parse(new InputStreamReader(inputStream)).getAsJsonArray();
            for (JsonElement jsonElement : entriesArray) {
                JsonObject entryObject = jsonElement.getAsJsonObject();
                JsonObject bindObject = entryObject.getAsJsonObject("bind");

                accesses.add(new Access(this,
                        entryObject.get("use").getAsBoolean(),
                        entryObject.get("protocol").getAsString(),
                        entryObject.get("hostname").getAsString(),
                        bindObject.get("host").getAsString(),
                        bindObject.get("port").getAsInt()));
            }
        } catch (Exception exception) {
            Modflared.LOGGER.error("Failed to load accessData: " + exception.getMessage());
            exception.printStackTrace();
        }
    }

    public void startAccess() {
        for (Access access : accesses) {
            if(!access.isUse()) continue;
            new Thread(() -> {
                try {
                    String[] command = access.command();
                    Modflared.LOGGER.info(Arrays.toString(command).replaceAll(",",""));
                    String osName = System.getProperty("os.name").toLowerCase();
                    if(osName.contains("win")) {
                        command[0] = "\"" + Modflared.DATA_FOLDER.getAbsolutePath() + "\\" + command[0] + "\"";
                    }
                    ProcessBuilder processBuilder = new ProcessBuilder(command);
                    if(osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                        processBuilder.directory(Modflared.DATA_FOLDER);
                    }
                    Process process = processBuilder.start();
                    processes.add(process);
                    InputStream inputStream = process.getInputStream();
                    InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                    BufferedReader bufferedReader = new BufferedReader(inputStreamReader);

                    String line;
                    while ((line = bufferedReader.readLine()) != null) {
                        Modflared.LOGGER.info(line);
                    }
                } catch (IOException exception) {
                    Modflared.LOGGER.error("Failed to start cloudflared: " + exception.getMessage());
                    exception.printStackTrace();
                }
            }, "Access#" + ACCESS_COUNT).start();
            ACCESS_COUNT++;
        }
    }

    public File getExecutableFile() {
        return executableFile;
    }

    public List<Process> getProcesses() {
        return processes;
    }

    public static class Access {

        private final CloudflaredProgram program;

        private final boolean use;
        private final String protocol;
        private final String hostname;
        private final String bind_host;
        private final int bind_port;

        public Access(CloudflaredProgram program, boolean use, String protocol, String hostname, String bind_host, int bind_port) {
            this.program = program;
            this.use = use;
            this.protocol = protocol;
            this.hostname = hostname;
            this.bind_host = bind_host;
            this.bind_port = bind_port;
        }

        public String[] command() {
            String osName = System.getProperty("os.name").toLowerCase();
            String executePrefix = "";
            if(osName.contains("nix") || osName.contains("nux") || osName.contains("aix")) {
                executePrefix = "./";
            }
            return new String[] {executePrefix + program.getExecutableFile().getName(), "access", protocol, "--hostname", hostname, "--url", bind_host + ":" + bind_port};
        }

        public boolean isUse() {
            return use;
        }

        public String getProtocol() {
            return protocol;
        }

        public String getHostname() {
            return hostname;
        }

        public String getBind_Host() {
            return bind_host;
        }

        public int getBind_Port() {
            return bind_port;
        }

    }

}
