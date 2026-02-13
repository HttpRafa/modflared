package dev.billy948787.modflared.github;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Unmodifiable;

import com.github.bsideup.jabel.Desugar;
import com.google.common.hash.HashCode;
import com.google.common.hash.Hashing;
import com.google.common.io.ByteSource;
import com.google.common.io.Files;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import dev.billy948787.modflared.Modflared;

public class GithubAPI {

    private static final String GITHUB_USER = "cloudflare";
    private static final String GITHUB_REPOSITORY = "cloudflared";
    // The 2.2.4 gson' JsonParser need init a instance to call parse method instead of call a static class method
    private static final JsonParser JSON_PARSER = new JsonParser();

    private static URL GITHUB_API_ENDPOINT = null;

    static {
        try {
            GITHUB_API_ENDPOINT = URI
                .create("https://api.github.com/repos/" + GITHUB_USER + "/" + GITHUB_REPOSITORY + "/releases/latest")
                .toURL();
        } catch (MalformedURLException exception) {
            Modflared.LOGGER.error("Failed to create url object of github endpoint.", exception);
        }
    }

    @Contract(" -> new")
    public static @NotNull CompletableFuture<String> requestLatestVersion() {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return getJsonFromEndpoint(GITHUB_API_ENDPOINT).get("tag_name")
                    .getAsString();
            } catch (Throwable throwable) {
                throw new IllegalStateException("Failed to get latest cloudflared version from github", throwable);
            }
        }, Modflared.EXECUTOR);
    }

    @Contract("_ -> new")
    public static @NotNull CompletableFuture<FileHash> requestFileHash(String filename) {
        return CompletableFuture.supplyAsync(() -> {
            try {
                return extractHashes(getJsonFromEndpoint(GITHUB_API_ENDPOINT)).stream()
                    .filter(item -> item.file.equals(filename))
                    .findFirst()
                    .get();
            } catch (Throwable throwable) {
                throw new IllegalStateException("Failed to get file hash from github", throwable);
            }
        }, Modflared.EXECUTOR);
    }

    private static @NotNull @Unmodifiable List<FileHash> extractHashes(@NotNull JsonObject data) {
        return Arrays.stream(
            data.get("body")
                .getAsString()
                .split("\n"))
            .filter(item -> item.startsWith("cloudflared-") && item.contains(":"))
            .map(item -> {
                var fileData = item.split(":");
                return new FileHash(fileData[0].trim(), fileData[1].trim());
            })
            .collect(Collectors.toList());
    }

    private static JsonObject getJsonFromEndpoint(@NotNull URL url) throws IOException {
        URLConnection connection = url.openConnection();
        InputStream inputStream = connection.getInputStream();

        return JSON_PARSER.parse(new InputStreamReader(inputStream))
            .getAsJsonObject();
    }

    public @Desugar record FileHash(String file, String hash) {

        public boolean compareTo(File file) throws IOException {
            return compareTo(computeHash(file));
        }

        public boolean compareTo(@NotNull FileHash hash) {
            return Objects.equals(this.hash, hash.hash());
        }

        public static @NotNull FileHash computeHash(File file) throws IOException {
            ByteSource byteSource = Files.asByteSource(file);
            HashCode hashCode = byteSource.hash(Hashing.sha256());
            return new FileHash(file.getName(), hashCode.toString());
        }

    }

}
