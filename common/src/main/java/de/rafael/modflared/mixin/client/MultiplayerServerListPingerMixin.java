package de.rafael.modflared.mixin.client;

import de.rafael.modflared.Modflared;
import de.rafael.modflared.interfaces.mixin.IServerInfo;
import de.rafael.modflared.tunnel.TunnelStatus;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Mixin(MultiplayerServerListPinger.class)
public abstract class MultiplayerServerListPingerMixin {

    @Redirect(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetAddress;IZ)Lnet/minecraft/network/ClientConnection;"))
    public ClientConnection connect(InetAddress address, int port, boolean shouldUseNativeTransport, ServerInfo entry) {
        TunnelStatus result = Modflared.TUNNEL_MANAGER.handleConnect(new InetSocketAddress(address, port));
        if(result.state() == TunnelStatus.State.USE) {
            ClientConnection connection = ClientConnection.connect(result.runningTunnel().access().tunnelAddress().getAddress(), result.runningTunnel().access().tunnelAddress().getPort(), shouldUseNativeTransport);
            Modflared.TUNNEL_MANAGER.prepareConnection(result, connection);
            ((IServerInfo) entry).setTunnelStatus(result);
            return connection;
        }
        return ClientConnection.connect(address, port, shouldUseNativeTransport);
    }

}
