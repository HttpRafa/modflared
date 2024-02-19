package de.rafael.modflared.mixin.client;

import de.rafael.modflared.Modflared;
import de.rafael.modflared.interfaces.mixin.IServerInfo;
import de.rafael.modflared.tunnel.TunnelStatus;
import net.minecraft.client.network.MultiplayerServerListPinger;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.network.ClientConnection;
import net.minecraft.util.profiler.MultiValueDebugSampleLogImpl;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;

@Mixin(MultiplayerServerListPinger.class)
public abstract class MultiplayerServerListPingerMixin {

    @Redirect(method = "add", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/util/profiler/MultiValueDebugSampleLogImpl;)Lnet/minecraft/network/ClientConnection;"))
    public ClientConnection connect(InetSocketAddress address, boolean useEpoll, MultiValueDebugSampleLogImpl packetSizeLog, ServerInfo entry) {
        var result = Modflared.TUNNEL_MANAGER.handleConnect(address);
        if(result.state() == TunnelStatus.State.USE) {
            var connection = ClientConnection.connect(result.runningTunnel().access().tunnelAddress(), useEpoll, packetSizeLog);
            Modflared.TUNNEL_MANAGER.prepareConnection(result, connection);
            ((IServerInfo) entry).setTunnelStatus(result);
            return connection;
        }
        return ClientConnection.connect(address, useEpoll, packetSizeLog);
    }

}
