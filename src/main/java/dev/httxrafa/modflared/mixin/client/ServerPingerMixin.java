package dev.httxrafa.modflared.mixin.client;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.interfaces.mixin.IServerData;
import dev.httxrafa.modflared.tunnel.TunnelStatus;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.ServerPinger;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Mixin(ServerPinger.class)
public abstract class ServerPingerMixin {

    @Redirect(
            method = "ping",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkManager;createNetworkManagerAndConnect(Ljava/net/InetAddress;IZ)Lnet/minecraft/network/NetworkManager;"
            )
    )
    private NetworkManager modflared$routeServerPing(InetAddress address, int port, boolean useNativeTransport, ServerData data) {
        InetSocketAddress original = new InetSocketAddress(address, port);
        TunnelStatus status = Modflared.TUNNEL_MANAGER.handleConnect(original);
        ((IServerData) data).setTunnelStatus(status);

        InetSocketAddress target;
        if (status.getState() == TunnelStatus.State.USE && status.getRunningTunnel() != null) {
            target = status.getRunningTunnel().getAccess().getTunnelAddress();
        } else {
            target = original;
        }

        try {
            NetworkManager manager = NetworkManager.createNetworkManagerAndConnect(
                    InetAddress.getByName(target.getHostString()),
                    target.getPort(),
                    useNativeTransport
            );
            Modflared.TUNNEL_MANAGER.prepareConnection(status, manager);
            return manager;
        } catch (UnknownHostException exception) {
            throw new RuntimeException("Failed to resolve ping target " + target, exception);
        }
    }
}
