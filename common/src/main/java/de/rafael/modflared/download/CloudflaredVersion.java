package de.rafael.modflared.download;

//------------------------------
//
// This class was developed by Rafael K.
// On 10/31/2022 at 11:17 PM
// In the project cloudflared
//
//------------------------------

import de.rafael.modflared.Modflared;
import de.rafael.modflared.github.GithubAPI;
import de.rafael.modflared.tunnel.RunningTunnel;
import de.rafael.modflared.tunnel.manager.TunnelManager;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.lwjgl.system.Platform;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.Collections;
import java.util.concurrent.CompletableFuture;

public class CloudflaredVersion {

    private final CloudflaredDownload download;
    private volatile String version;

    private static final File VERSION_FILE = new File(TunnelManager.DATA_FOLDER, "version.json");

    private static final String GITHUB_DOWNLOAD_ENDPOINT = "https://github.com/cloudflare/cloudflared/releases/download/";

    public CloudflaredVersion(CloudflaredDownload download, String version) {
        this.version = version;
        this.download = download;
    }

    public static CompletableFuture<CloudflaredVersion> create() {
        if(VERSION_FILE.exists()) {
            try {
                CloudflaredVersion version = Modflared.GSON.fromJson(new InputStreamReader(new FileInputStream(VERSION_FILE)), CloudflaredVersion.class);
                if(version != null) return CompletableFuture.completedFuture(version);
            } catch (Throwable throwable) {
                Modflared.LOGGER.error("Failed to load existing version file creating new one...", throwable);
            }
        }
        return GithubAPI.requestLatestVersion().thenApply(latestVersion -> new CloudflaredVersion(CloudflaredDownload.find(), latestVersion));
    }

    public @Nullable RunningTunnel createTunnel(RunningTunnel.Access access) {
        try {
            return RunningTunnel.createTunnel(this, access).get();
        } catch (Exception exception) {
            Modflared.LOGGER.error("Failed to create tunnel", exception);
            return null;
        }
    }

    public CompletableFuture<Void> prepare() {
        if(isInstalled()) {
            CompletableFuture<Void> completableFuture = new CompletableFuture<>();
            isUptoDate().whenComplete((pair, throwable) -> {
                if (throwable != null) {
                    Modflared.LOGGER.error("Failed to check for updates", throwable);
                    TunnelManager.displayErrorToast();
                    completableFuture.complete(null);
                } else {
                    if(!pair.getLeft()) {
                        Modflared.LOGGER.info("Update detected updating...");
                        version = pair.getRight();
                        downloadAndSaveInfo().whenComplete((unused, throwable1) -> {
                            if (throwable1 != null) Modflared.LOGGER.error("Failed to download update", throwable1);
                            TunnelManager.displayErrorToast();
                            completableFuture.complete(null);
                        });
                    } else {
                        Modflared.LOGGER.info("Cloudflared has no updates :)");
                        completableFuture.complete(null);
                    }
                }
            });
            return completableFuture;
        } else {
            return downloadAndSaveInfo();
        }
    }

    private CompletableFuture<Void> downloadAndSaveInfo() {
        return downloadFile().thenAccept(unused -> {
            try {
                save();
            } catch (Throwable throwable) {
                Modflared.LOGGER.error("Failed to save current installed version", throwable);
                TunnelManager.displayErrorToast();
            }
        });
    }

    public boolean isInstalled() {
        return createBinaryRef().exists() && VERSION_FILE.exists();
    }

    @Contract(" -> new")
    public @NotNull File createBinaryRef() {
        return new File(TunnelManager.DATA_FOLDER, download.fileName());
    }

    public CompletableFuture<Pair<Boolean, String>> isUptoDate() {
        return GithubAPI.requestLatestVersion().thenApply(latestVersion -> new Pair<>(latestVersion.equals(version), latestVersion));
    }

    public @NotNull CompletableFuture<Void> downloadFile() {
        return GithubAPI.requestFileHash(download.downloadFile()).thenAcceptAsync(fileHash -> {
            try {
                for (int i = 0; i < 5; i++) {
                    Modflared.LOGGER.info("Downloading cloudflared version {} from github. Attempt: {}", version, i + 1);
                    File file = syncDownloadFile();

                    // Check if file is corrupt
                    if(fileHash.compareTo(file)) {
                        Modflared.LOGGER.info("Download finished of cloudflared version {}!", version);
                        return;
                    } else {
                        file.delete();
                    }
                }
            } catch (InterruptedException exception) {
                throw new IllegalStateException("Error while unpacking MacOS cloudflared download", exception);
            } catch (Exception exception) {
                throw new IllegalStateException("Failed to download cloudflared binary", exception);
            }
            throw new IllegalStateException("Modflared failed 5 times to download cloudflared from github. Please check your internet connection");
        }, Modflared.EXECUTOR);
    }

    private @NotNull File syncDownloadFile() throws IOException, InterruptedException {
        File output = new File(TunnelManager.DATA_FOLDER, download.fileName());
        if(!output.getParentFile().exists()) output.getParentFile().mkdirs();
        if(!output.exists()) output.createNewFile();
        try (BufferedInputStream in = new BufferedInputStream(URI.create(GITHUB_DOWNLOAD_ENDPOINT + version + "/" + download.downloadFile()).toURL().openStream()); BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(output))) {
            byte[] dataBuffer = new byte[1024];
            int bytesRead;
            while ((bytesRead = in.read(dataBuffer, 0, 1024)) != -1) {
                fileOutputStream.write(dataBuffer, 0, bytesRead);
            }
            fileOutputStream.flush();
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
        return output;
    }

    private void save() throws IOException {
        Files.write(VERSION_FILE.toPath(), Collections.singleton(Modflared.GSON.toJson(this)), StandardCharsets.UTF_8);
    }

}
