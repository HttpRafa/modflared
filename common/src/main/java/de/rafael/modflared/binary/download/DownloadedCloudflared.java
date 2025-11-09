package de.rafael.modflared.binary.download;

import de.rafael.modflared.Modflared;
import de.rafael.modflared.binary.Cloudflared;
import de.rafael.modflared.github.GithubAPI;
import de.rafael.modflared.tunnel.RunningTunnel;
import de.rafael.modflared.tunnel.manager.TunnelManager;
import net.minecraft.util.Pair;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.lwjgl.system.Platform;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

public class DownloadedCloudflared extends Cloudflared {

    private final CloudflaredDownload download;

    private static final File VERSION_FILE = new File(TunnelManager.DATA_FOLDER, "version.json");

    private static final String GITHUB_DOWNLOAD_ENDPOINT = "https://github.com/cloudflare/cloudflared/releases/download/";

    public DownloadedCloudflared(CloudflaredDownload download, String version) {
        super(version);
        this.download = download;
    }

    public static CompletableFuture<Cloudflared> tryCreate() {
        if(VERSION_FILE.exists()) {
            try {
                var version = Modflared.GSON.fromJson(new InputStreamReader(new FileInputStream(VERSION_FILE)), DownloadedCloudflared.class);
                if(version != null) return CompletableFuture.completedFuture(version);
            } catch (Throwable throwable) {
                Modflared.LOGGER.error("Failed to load existing version file creating new one...", throwable);
            }
        }
        return GithubAPI.requestLatestVersion().thenApply(latestVersion -> new DownloadedCloudflared(CloudflaredDownload.find(), latestVersion));
    }

    @Override
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

    @Override
    public String[] buildCommand(RunningTunnel.@NotNull Access access) {
        var command = access.command(createBinaryRef().getName(), true);
        Modflared.LOGGER.info(Arrays.toString(command).replace(",",""));
        if (Platform.get() == Platform.WINDOWS) {
            command[0] = "\"" + TunnelManager.DATA_FOLDER.getAbsolutePath() + "\\" + command[0] + "\"";
        }
        return command;
    }

    private @NotNull CompletableFuture<Void> downloadAndSaveInfo() {
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
        return GithubAPI.requestFileHash(download.downloadFile()).thenAcceptAsync(expected -> {
            try {
                for (int i = 0; i < 4; i++) {
                    Modflared.LOGGER.info("Downloading cloudflared version {} from github. Attempt: {}", version, i + 1);
                    var downloadedFile = syncDownloadFile();
                    Modflared.LOGGER.info("Downloaded file preparing cloudflared binary...");
                    var file = new File(TunnelManager.DATA_FOLDER, download.fileName());
                    prepareFile(downloadedFile, file);

                    // Check if file is corrupt
                    Modflared.LOGGER.info("Checking file integrity");
                    var provided = GithubAPI.FileHash.computeHash(file);
                    if(expected.compareTo(provided)) {
                        Modflared.LOGGER.info("Download finished of cloudflared version {}!", version);
                        return;
                    } else {
                        Modflared.LOGGER.warn("This downloaded file does not match with the file hash provided on GitHub.");
                        Modflared.LOGGER.warn("Expected {}, Provided: {}", expected.hash(), provided.hash());

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
        File output = new File(TunnelManager.DATA_FOLDER, UUID.randomUUID().toString());
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

        return output;
    }

    private void prepareFile(@NotNull File downloadedFile, File targetFile) throws IOException, InterruptedException {
        var platform = Platform.get();

        if(platform == Platform.MACOSX) {
            var workingDirectory = downloadedFile.getParentFile().toPath();
            runCommand(workingDirectory, "tar", "-xzf", downloadedFile.getName());
            Files.move(workingDirectory.resolve("cloudflared"), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } else {
            Files.move(downloadedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
        }

        if(platform == Platform.LINUX || platform == Platform.MACOSX) {
            makeExecutable(targetFile.toPath());
        }

        downloadedFile.delete();
    }

    private void runCommand(@NotNull Path workingDirectory, String... command) throws IOException, InterruptedException {
        ProcessBuilder processBuilder = new ProcessBuilder(command)
                .directory(workingDirectory.toFile())
                .redirectErrorStream(true);
        Process process = processBuilder.start();
        boolean finished = process.waitFor(60, TimeUnit.SECONDS);
        if (!finished) {
            process.destroyForcibly();
            throw new IOException("Command timed out: " + String.join(" ", command));
        }
        int code = process.exitValue();
        if (code != 0) {
            throw new IOException("Command failed (exit " + code + "): " + String.join(" ", command));
        }
    }

    private void makeExecutable(@NotNull Path path) throws IOException {
        try {
            var permissions = Files.getPosixFilePermissions(path);
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException exception) {
            // Fallback (non-POSIX)
            var file = path.toFile();
            if(!file.setExecutable(true, false)) {
                throw new IOException("Failed to set executable bit on " + file.getName());
            }
        }
    }

    private void save() throws IOException {
        Files.writeString(VERSION_FILE.toPath(), Modflared.GSON.toJson(this), StandardCharsets.UTF_8);
    }

}
