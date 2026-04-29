package dev.httxrafa.modflared.binary.download;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.binary.Cloudflared;
import dev.httxrafa.modflared.github.GithubAPI;
import dev.httxrafa.modflared.tunnel.RunningTunnel;
import dev.httxrafa.modflared.tunnel.manager.TunnelManager;

import java.io.*;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
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
                DownloadedCloudflared version = Modflared.GSON.fromJson(new InputStreamReader(new FileInputStream(VERSION_FILE)), DownloadedCloudflared.class);
                if(version != null) {
                    return CompletableFuture.completedFuture(version);
                }
            } catch (Throwable throwable) {
                Modflared.LOGGER.error("Failed to load existing version file creating new one...", throwable);
            }
        }
        return GithubAPI.requestLatestVersion().thenApply(new java.util.function.Function<String, Cloudflared>() {
            @Override
            public Cloudflared apply(String latestVersion) {
                return new DownloadedCloudflared(CloudflaredDownload.find(), latestVersion);
            }
        });
    }

    @Override
    public CompletableFuture<Void> prepare() {
        if(isInstalled()) {
            final CompletableFuture<Void> completableFuture = new CompletableFuture<Void>();
            isUptoDate().whenComplete(new java.util.function.BiConsumer<Boolean, Throwable>() {
                @Override
                public void accept(Boolean upToDate, Throwable throwable) {
                    if (throwable != null) {
                        Modflared.LOGGER.error("Failed to check for updates", throwable);
                        TunnelManager.displayErrorToast();
                        completableFuture.complete(null);
                    } else {
                        if(!upToDate) {
                            Modflared.LOGGER.info("Update detected updating...");
                            DownloadedCloudflared.this.version = version; // This line looks wrong in original too
                            downloadAndSaveInfo().whenComplete(new java.util.function.BiConsumer<Void, Throwable>() {
                                @Override
                                public void accept(Void unused, Throwable throwable1) {
                                    if (throwable1 != null) {
                                        Modflared.LOGGER.error("Failed to download update", throwable1);
                                    }
                                    TunnelManager.displayErrorToast();
                                    completableFuture.complete(null);
                                }
                            });
                        } else {
                            Modflared.LOGGER.info("Cloudflared has no updates :)");
                            completableFuture.complete(null);
                        }
                    }
                }
            });
            return completableFuture;
        } else {
            return downloadAndSaveInfo();
        }
    }

    @Override
    public String[] buildCommand(RunningTunnel.Access access) {
        String[] command = access.command(createBinaryRef().getName(), true);
        Modflared.LOGGER.info(Arrays.toString(command).replace(",",""));
        if (RunningTunnel.isWindows()) {
            command[0] = "\"" + TunnelManager.DATA_FOLDER.getAbsolutePath() + "\\" + command[0] + "\"";
        }
        return command;
    }

    private CompletableFuture<Void> downloadAndSaveInfo() {
        return downloadFile().thenAccept(new java.util.function.Consumer<Void>() {
            @Override
            public void accept(Void unused) {
                try {
                    save();
                } catch (Throwable throwable) {
                    Modflared.LOGGER.error("Failed to save current installed version", throwable);
                    TunnelManager.displayErrorToast();
                }
            }
        });
    }

    public boolean isInstalled() {
        return createBinaryRef().exists() && VERSION_FILE.exists();
    }

    public File createBinaryRef() {
        return new File(TunnelManager.DATA_FOLDER, download.fileName());
    }

    public CompletableFuture<Boolean> isUptoDate() {
        return GithubAPI.requestLatestVersion().thenApply(new java.util.function.Function<String, Boolean>() {
            @Override
            public Boolean apply(String latestVersion) {
                return latestVersion.equals(DownloadedCloudflared.this.version);
            }
        });
    }

    public CompletableFuture<Void> downloadFile() {
        return GithubAPI.requestFileHash(download.downloadFile()).thenAcceptAsync(new java.util.function.Consumer<GithubAPI.FileHash>() {
            @Override
            public void accept(GithubAPI.FileHash expected) {
                try {
                    for (int i = 0; i < 4; i++) {
                        Modflared.LOGGER.info("Downloading cloudflared version " + version + " from github. Attempt: " + (i + 1));
                        File downloadedFile = syncDownloadFile();
                        Modflared.LOGGER.info("Downloaded file preparing cloudflared binary...");
                        File file = new File(TunnelManager.DATA_FOLDER, download.fileName());
                        prepareFile(downloadedFile, file);

                        // Check if file is corrupt
                        Modflared.LOGGER.info("Checking file integrity");
                        GithubAPI.FileHash provided = GithubAPI.FileHash.computeHash(file);
                        if(expected.compareTo(provided)) {
                            Modflared.LOGGER.info("Download finished of cloudflared version " + version + "!");
                            return;
                        } else {
                            Modflared.LOGGER.warn("This downloaded file does not match with the file hash provided on GitHub.");
                            Modflared.LOGGER.warn("Expected " + expected.hash() + ", Provided: " + provided.hash());
                            file.delete();
                        }
                    }
                } catch (InterruptedException exception) {
                    throw new IllegalStateException("Error while unpacking MacOS cloudflared download", exception);
                } catch (Exception exception) {
                    throw new IllegalStateException("Failed to download cloudflared binary", exception);
                }
                throw new IllegalStateException("Modflared failed 5 times to download cloudflared from github. Please check your internet connection");
            }
        }, Modflared.EXECUTOR);
    }

    private File syncDownloadFile() throws IOException, InterruptedException {
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

    private void prepareFile(File downloadedFile, File targetFile) throws IOException, InterruptedException {
        if(download.osName().contains("mac os x")) {
            throw new IOException("macOS cloudflared download is not supported in the first 1.12.2 legacy release");
        }

        Files.move(downloadedFile.toPath(), targetFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

        if(!RunningTunnel.isWindows()) {
            makeExecutable(targetFile.toPath());
        }

        downloadedFile.delete();
    }

    private void runCommand(Path workingDirectory, String... command) throws IOException, InterruptedException {
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

    private void makeExecutable(Path path) throws IOException {
        try {
            Set<PosixFilePermission> permissions = new HashSet<PosixFilePermission>(Files.getPosixFilePermissions(path));
            permissions.add(PosixFilePermission.OWNER_EXECUTE);
            permissions.add(PosixFilePermission.GROUP_EXECUTE);
            permissions.add(PosixFilePermission.OTHERS_EXECUTE);
            Files.setPosixFilePermissions(path, permissions);
        } catch (UnsupportedOperationException exception) {
            // Fallback (non-POSIX)
            File file = path.toFile();
            if(!file.setExecutable(true, false)) {
                throw new IOException("Failed to set executable bit on " + file.getName());
            }
        }
    }

    private void save() throws IOException {
        Files.write(VERSION_FILE.toPath(), Modflared.GSON.toJson(this).getBytes(StandardCharsets.UTF_8));
    }

}
