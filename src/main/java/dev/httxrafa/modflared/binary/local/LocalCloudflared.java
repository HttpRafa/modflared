package dev.httxrafa.modflared.binary.local;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.binary.Cloudflared;
import dev.httxrafa.modflared.tunnel.RunningTunnel;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.util.concurrent.CompletableFuture;

public class LocalCloudflared extends Cloudflared {

    public LocalCloudflared(String version) {
        super(version);
    }

    @Override
    public CompletableFuture<Void> prepare() {
        return CompletableFuture.completedFuture(null);
    }

    @Override
    public String[] buildCommand(RunningTunnel.Access access) {
        return access.command("cloudflared", false);
    }

    public static Cloudflared tryCreate() {
        // Check if cloudflared is already installed on the system
        try {
            ProcessBuilder builder = new ProcessBuilder("cloudflared", "--version");
            Process process = builder.start();
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String versionString = reader.readLine();
            String version = versionString.split(" ")[2];
            Modflared.LOGGER.info("Cloudflared output: " + versionString);
            Modflared.LOGGER.info("Cloudflared version " + version + " is already installed on the system");
            return new LocalCloudflared(version);
        } catch (Throwable ignored) {
            Modflared.LOGGER.info("Cloudflared is not installed on the system. Downloading it if necessary...");
        }
        return null;
    }

}
