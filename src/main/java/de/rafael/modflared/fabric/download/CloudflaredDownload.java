package de.rafael.modflared.fabric.download;

//------------------------------
//
// This class was developed by Rafael K.
// On 10/31/2022 at 11:17 PM
// In the project cloudflared
//
//------------------------------

import de.rafael.modflared.fabric.Modflared;
import de.rafael.modflared.fabric.program.CloudflaredProgram;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.Platform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URL;
import java.util.Arrays;
import java.util.Optional;
import java.util.function.Consumer;

public enum CloudflaredDownload {

    WINDOW_32("windows", "x86", ".exe", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-386.exe"),
    WINDOW_64("windows", "amd64", ".exe", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"),
    LINUX_32("linux", "x86", "", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-386"),
    LINUX_64("linux", "amd64", "", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64");

    public static void findAndDownload(Consumer<CloudflaredProgram> consumer) {
        new Thread(() -> {
            String name = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();
            Optional<CloudflaredDownload> downloadOptional = Arrays.stream(CloudflaredDownload.values()).filter(item -> name.contains(item.getName()) && arch.contains(item.getArch())).findFirst();
            if(downloadOptional.isPresent()) {
                CloudflaredDownload download = downloadOptional.get();
                if(!download.isInstalled()) {
                    try {
                        download.download();
                        consumer.accept(download.program());
                    } catch(Exception exception) {
                        Modflared.LOGGER.error("Error: " + exception.getMessage());
                        exception.printStackTrace();
                    }
                } else {
                    consumer.accept(download.program());
                }
            }
        }, "Modflared Init Thread").start();
    }

    private final String name;
    private final String arch;
    private final String fileName;
    private final String download;

    CloudflaredDownload(String name, String arch, String fileNamePrefix, String download) {
        this.name = name;
        this.arch = arch;
        this.fileName = name + "-" + arch + fileNamePrefix;
        this.download = download;
    }

    @Contract(" -> new")
    public @NotNull CloudflaredProgram program() {
        return new CloudflaredProgram(new File(Modflared.DATA_FOLDER, fileName));
    }

    public void download() throws IOException {
        File output = new File(Modflared.DATA_FOLDER, fileName);
        if(!output.getParentFile().exists()) output.getParentFile().mkdirs();
        if(!output.exists()) output.createNewFile();
        Modflared.LOGGER.info("Starting download of cloudflared from[" + getDownload() + "]!");
        try (BufferedInputStream in = new BufferedInputStream(new URL(getDownload()).openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(output)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
        if(Platform.get() == Platform.LINUX) {
            new ProcessBuilder("chmod", "+x", output.getName()).directory(output.getParentFile()).start();
        }
        Modflared.LOGGER.info("Download finished of cloudflared!");
    }

    public boolean isInstalled() {
        return new File(Modflared.DATA_FOLDER, fileName).exists();
    }

    public String getName() {
        return name;
    }

    public String getArch() {
        return arch;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDownload() {
        return download;
    }

}
