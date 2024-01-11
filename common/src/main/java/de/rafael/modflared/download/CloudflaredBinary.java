package de.rafael.modflared.download;

//------------------------------
//
// This class was developed by Rafael K.
// On 10/31/2022 at 11:17 PM
// In the project cloudflared
//
//------------------------------

import de.rafael.modflared.Modflared;
import de.rafael.modflared.tunnel.RunningTunnel;
import de.rafael.modflared.tunnel.manager.TunnelManager;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.Platform;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.util.Arrays;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

public enum CloudflaredBinary {

    WINDOW_32("windows", "x86", ".exe", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-386.exe"),
    WINDOW_64("windows", "amd64", ".exe", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-windows-amd64.exe"),
    LINUX_32("linux", "x86", "", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-386"),
    LINUX_64("linux", "amd64", "", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-linux-amd64"),
    MAC_OS_X_64("mac os x", "x86_64", "", "https://github.com/cloudflare/cloudflared/releases/latest/download/cloudflared-darwin-amd64.tgz");

    @Contract(" -> new")
    public static @NotNull CompletableFuture<CloudflaredBinary> findAndDownload() {
        return CompletableFuture.supplyAsync(() -> {
            String name = System.getProperty("os.name").toLowerCase();
            String arch = System.getProperty("os.arch").toLowerCase();
            Optional<CloudflaredBinary> download = Arrays.stream(CloudflaredBinary.values()).filter(item -> name.contains(item.name) && arch.contains(item.arch)).findFirst();
            if(download.isPresent()) {
                var binary = download.get();
                if(!binary.isInstalled()) {
                    try {
                        binary.download();
                        return binary;
                    } catch (InterruptedException exception) {
                        throw new IllegalStateException("Error while unpacking MacOS cloudflared download", exception);
                    } catch (Exception exception) {
                        throw new IllegalStateException("Failed to download cloudflared binary", exception);
                    }
                } else {
                    return binary;
                }
            } else {
                throw new IllegalStateException("Cloudflared could not be downloaded because no binary file was found for the current operating system");
            }
        }, Modflared.EXECUTOR);
    }

    private final String name;
    private final String arch;
    private final String fileName;
    private final String download;

    CloudflaredBinary(String name, String arch, String fileNameSuffix, String download) {
        this.name = name;
        this.arch = arch;
        this.fileName = name + "-" + arch + fileNameSuffix;
        this.download = download;
    }

    public @Nullable RunningTunnel createTunnel(RunningTunnel.Access access) {
        try {
            return RunningTunnel.createTunnel(this, access).get();
        } catch (Exception exception) {
            Modflared.LOGGER.error("Failed to create tunnel", exception);
            return null;
        }
    }

    public void download() throws InterruptedException, IOException {
        File output = new File(TunnelManager.DATA_FOLDER, fileName);
        if(!output.getParentFile().exists()) output.getParentFile().mkdirs();
        if(!output.exists()) output.createNewFile();
        Modflared.LOGGER.info("Starting download of cloudflared from[" + download + "]!");
        try (BufferedInputStream in = new BufferedInputStream(URI.create(download).toURL().openStream());
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

    @Contract(value = " -> new", pure = true)
    public @NotNull File getFile() {
        return new File(TunnelManager.DATA_FOLDER, fileName);
    }

    public boolean isInstalled() {
        return getFile().exists();
    }

}
