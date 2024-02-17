package de.rafael.modflared.mixin;

import de.rafael.modflared.Modflared;
import de.rafael.modflared.tunnel.manager.TunnelManager;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.network.ClientConnection;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Mixin(MultiplayerServerListPinger.class)
public class MultiplayerServerListPingerMixin {

    @Redirect(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetAddress;IZ)Lnet/minecraft/network/ClientConnection;"))
    public ClientConnection connect(InetAddress inetAddress, int port, boolean shouldUseNativeTransport) {
        var handleConnectResult = Modflared.TUNNEL_MANAGER.handleConnect(new InetSocketAddress(inetAddress, port));

        var address = handleConnectResult.address();
        var connection = ClientConnection.connect(address.getAddress(), address.getPort(), shouldUseNativeTransport);

        var tunnelConnection = (TunnelManager.Connection) connection;
        tunnelConnection.setRunningTunnel(handleConnectResult.runningTunnel());
        return connection;
    }

}
