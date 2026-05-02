package dev.httxrafa.modflared.github;

import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import dev.httxrafa.modflared.Modflared;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

public class GithubAPI {

    private static final String GITHUB_USER = "cloudflare";
    private static final String GITHUB_REPOSITORY = "cloudflared";

    private static URL GITHUB_API_ENDPOINT = null;

    static {
        try {
            GITHUB_API_ENDPOINT = URI.create("https://api.github.com/repos/" + GITHUB_USER + "/" + GITHUB_REPOSITORY + "/releases/latest").toURL();
        } catch (MalformedURLException exception) {
            Modflared.LOGGER.error("Failed to create url object of github endpoint.", exception);
        }
    }

    public static CompletableFuture<String> requestLatestVersion() {
        return CompletableFuture.supplyAsync(new java.util.function.Supplier<String>() {
            @Override
            public String get() {
                try {
                    return getJsonFromEndpoint(GITHUB_API_ENDPOINT).get("tag_name").getAsString();
                } catch (Throwable throwable) {
                    throw new IllegalStateException("Failed to get latest cloudflared version from github", throwable);
                }
            }
        }, Modflared.EXECUTOR);
    }

    public static CompletableFuture<FileHash> requestFileHash(String filename) {
        return CompletableFuture.supplyAsync(new java.util.function.Supplier<FileHash>() {
            @Override
            public FileHash get() {
                try {
                    List<FileHash> hashes = extractHashes(getJsonFromEndpoint(GITHUB_API_ENDPOINT));
                    for (FileHash item : hashes) {
                        if (item.file().equals(filename)) {
                            return item;
                        }
                    }
                    throw new IllegalStateException("File hash not found for " + filename);
                } catch (Throwable throwable) {
                    throw new IllegalStateException("Failed to get file hash from github", throwable);
                }
            }
        }, Modflared.EXECUTOR);
    }

    private static List<FileHash> extractHashes(JsonObject data) {
        List<FileHash> hashes = new ArrayList<FileHash>();
        String[] lines = data.get("body").getAsString().split("\n");
        for (String line : lines) {
            if (line.startsWith("cloudflared-") && line.contains(":")) {
                String[] fileData = line.split(":");
                hashes.add(new FileHash(fileData[0].trim(), fileData[1].trim()));
            }
        }
        return Collections.unmodifiableList(hashes);
    }

    private static JsonObject getJsonFromEndpoint(URL url) throws IOException {
        URLConnection connection = url.openConnection();
        InputStream inputStream = connection.getInputStream();
        return new JsonParser().parse(new InputStreamReader(inputStream)).getAsJsonObject();
    }

    public static class FileHash {
        private final String file;
        private final String hash;

        public FileHash(String file, String hash) {
            this.file = file;
            this.hash = hash;
        }

        public String file() {
            return file;
        }

        public String hash() {
            return hash;
        }

        public boolean compareTo(File file) throws IOException {
            return compareTo(computeHash(file));
        }

        public boolean compareTo(FileHash hash) {
            return Objects.equals(this.hash, hash.hash());
        }

        public static FileHash computeHash(File file) throws IOException {
            ByteSource byteSource = Files.asByteSource(file);
            HashCode hashCode = byteSource.hash(Hashing.sha256());
            return new FileHash(file.getName(), hashCode.toString());
        }
    }

}
