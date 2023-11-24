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
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Consumer;

public enum CloudflaredDownload {

    WINDOW_32("windows", "x86", ".exe", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-386.exe"),
    WINDOW_64("windows", "amd64", ".exe", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"),
    LINUX_32("linux", "x86", "", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-386"),
    LINUX_64("linux", "amd64", "", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64"),
    // MacOS version is behind
    MAC_OS_X_64("mac os x", "x86_64", "", "https://github.com/cloudflare/cloudflared/releases/download/2023.8.2/cloudflared-darwin-amd64.tgz");

    public static CompletableFuture<CloudflaredProgram> findAndDownload() {
        var completableFuture = new CompletableFuture<CloudflaredProgram>();
        new Thread(() -> {
            String name = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();
            Optional<CloudflaredDownload> downloadOptional = Arrays.stream(CloudflaredDownload.values()).filter(item -> name.contains(item.getName()) && arch.contains(item.getArch())).findFirst();
            if(downloadOptional.isPresent()) {
                CloudflaredDownload download = downloadOptional.get();
                if(!download.isInstalled()) {
                    try {
                        download.download();
                        completableFuture.complete(download.program());
                    } catch(InterruptedException e) {
                        Thread.currentThread().interrupt();
                        Modflared.LOGGER.error("Error while untarring MacOS cloudflared download: {}", e.getMessage());
                        e.printStackTrace();
                        completableFuture.completeExceptionally(e);
                    } catch(Exception exception) {
                        Modflared.LOGGER.error("Error: {}", exception.getMessage());
                        exception.printStackTrace();
                        completableFuture.completeExceptionally(exception);
                    }
                } else {
                    completableFuture.complete(download.program());
                }
            }
        }, "Modflared Init Thread").start();

        return completableFuture;
    }

    private final String name;
    private final String arch;
    private final String fileName;
    private final String download;

    CloudflaredDownload(String name, String arch, String fileNameSuffix, String download) {
        this.name = name;
        this.arch = arch;
        this.fileName = name + "-" + arch + fileNameSuffix;
        this.download = download;
    }

    @Contract(" -> new")
    public @NotNull CloudflaredProgram program() {
        return new CloudflaredProgram(new File(Modflared.DATA_FOLDER, fileName));
    }

    public void download() throws InterruptedException, IOException {
        File output = new File(Modflared.DATA_FOLDER, fileName);
        if(!output.getParentFile().exists()) output.getParentFile().mkdirs();
        if(!output.exists()) output.createNewFile();
        Modflared.LOGGER.info("Starting download of cloudflared from[" + getDownload() + "]!");
        try (BufferedInputStream in = new BufferedInputStream(URI.create(getDownload()).toURL().openStream());
            FileOutputStream fileOutputStream = new FileOutputStream(output)) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
        }
        
        switch (Platform.get()) {
            case MACOSX:
                new ProcessBuilder("tar", "-xzf", output.getName()).directory(output.getParentFile()).start().waitFor();
                new ProcessBuilder("mv", "cloudflared", output.getName()).directory(output.getParentFile()).start().waitFor();
                //Fallthrough
            case LINUX:
                new ProcessBuilder("chmod", "+x", output.getName()).directory(output.getParentFile()).start();
                break;
            default:
                break;
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