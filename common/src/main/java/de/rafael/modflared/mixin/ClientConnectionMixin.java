package de.rafael.modflared.mixin;

import de.rafael.modflared.Modflared;
import de.rafael.modflared.interfaces.mixin.IClientConnection;
import de.rafael.modflared.tunnel.RunningTunnel;
import de.rafael.modflared.tunnel.manager.TunnelManager;
import io.netty.channel.ChannelFuture;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Implements(@Interface(iface = IClientConnection.class, prefix = "connection$"))
@Mixin(ClientConnection.class)
public abstract class ClientConnectionMixin implements IClientConnection {

    @Unique
    private RunningTunnel modflared$runningTunnel = null;

    /* Replaced by MultiplayerServerListPingerMixin
    @Redirect(method = "connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/util/profiler/PerformanceLog;)Lnet/minecraft/network/ClientConnection;", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;"))
    private static ChannelFuture connect(@NotNull InetSocketAddress address, boolean useEpoll, ClientConnection connection) {
        return ClientConnection.connect(Modflared.TUNNEL_MANAGER.handleConnect(address, connection).address(), useEpoll, connection);
    }*/

    @Inject(method = "disconnect", at = @At("TAIL"))
    public void disconnect(Text disconnectReason, CallbackInfo callbackInfo) {
        synchronized(this) {
            if(this.modflared$runningTunnel != null) {
                Modflared.TUNNEL_MANAGER.closeTunnel(this.modflared$runningTunnel);
                this.modflared$runningTunnel = null;
            }
        }
    }

    @Intrinsic
    public void connection$setRunningTunnel(RunningTunnel runningTunnel) {
        this.modflared$runningTunnel = runningTunnel;
    }
    
}
