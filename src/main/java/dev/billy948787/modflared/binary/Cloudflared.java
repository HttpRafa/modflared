package dev.billy948787.modflared.binary;

import dev.billy948787.modflared.Modflared;
import dev.billy948787.modflared.binary.download.DownloadedCloudflared;
import dev.billy948787.modflared.binary.local.LocalCloudflared;
import dev.billy948787.modflared.tunnel.RunningTunnel;

import java.util.concurrent.CompletableFuture;

public abstract class Cloudflared {

    protected volatile String version;

    public Cloudflared(String version) {
        this.version = version;
    }

    public abstract CompletableFuture<Void> prepare();

    public abstract String[] buildCommand(RunningTunnel.Access access);

    public static CompletableFuture<Cloudflared> create() {
        var local = LocalCloudflared.tryCreate();
        if (local != null) return CompletableFuture.completedFuture(local);

        return DownloadedCloudflared.tryCreate();
    }

    public RunningTunnel createTunnel(RunningTunnel.Access access) {
        try {
            return RunningTunnel.createTunnel(this, access).get();
        } catch (Exception exception) {
            Modflared.LOGGER.error("Failed to create tunnel", exception);
            return null;
        }
    }

}
