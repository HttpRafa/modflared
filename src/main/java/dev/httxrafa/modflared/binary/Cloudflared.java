package dev.httxrafa.modflared.binary;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.binary.download.DownloadedCloudflared;
import dev.httxrafa.modflared.binary.local.LocalCloudflared;
import dev.httxrafa.modflared.tunnel.RunningTunnel;

import java.util.concurrent.CompletableFuture;

public abstract class Cloudflared {

    protected volatile String version;

    public Cloudflared(String version) {
        this.version = version;
    }

    public abstract CompletableFuture<Void> prepare();

    public abstract String[] buildCommand(RunningTunnel.Access access);

    public static CompletableFuture<Cloudflared> create() {
        Cloudflared local = LocalCloudflared.tryCreate();
        if(local != null) {
            return CompletableFuture.completedFuture(local);
        }
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
