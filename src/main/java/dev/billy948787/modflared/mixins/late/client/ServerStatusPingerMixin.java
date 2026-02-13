package dev.billy948787.modflared.mixins.late.client;

import dev.billy948787.modflared.Modflared;
import dev.billy948787.modflared.interfaces.mixin.IServerData;
import dev.billy948787.modflared.tunnel.TunnelStatus;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.network.OldServerPinger;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Mixin(OldServerPinger.class)
public abstract class ServerStatusPingerMixin {

    @Redirect(method = "func_147224_a", at = @At(
        value = "INVOKE",
        target = "Lnet/minecraft/network/NetworkManager;provideLanClient(Ljava/net/InetAddress;I)Lnet/minecraft/network/NetworkManager;"
    ))
    public NetworkManager pingServer(InetAddress address, int port, ServerData serverData) {
        InetSocketAddress original = new InetSocketAddress(address, port);

        TunnelStatus status = Modflared.TUNNEL_MANAGER.handleConnect(original);

        ((IServerData) serverData).setTunnelStatus(status);

        InetSocketAddress target = (status.state() == TunnelStatus.State.USE)
            ? status.runningTunnel().access().tunnelAddress()
            : original;

        NetworkManager networkManager = NetworkManager.provideLanClient(target.getAddress(), target.getPort());

        Modflared.TUNNEL_MANAGER.prepareConnection(status, networkManager);

        return networkManager;
    }
}
