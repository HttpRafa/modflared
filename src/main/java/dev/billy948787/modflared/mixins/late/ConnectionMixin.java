package dev.billy948787.modflared.mixins.late;

import net.minecraft.network.NetworkManager;
import net.minecraft.util.IChatComponent;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import dev.billy948787.modflared.Modflared;
import dev.billy948787.modflared.interfaces.mixin.IConnection;
import dev.billy948787.modflared.tunnel.RunningTunnel;

@Implements(@Interface(iface = IConnection.class, prefix = "connection$"))
@Mixin(NetworkManager.class)
public abstract class ConnectionMixin implements IConnection {

    @Unique
    private RunningTunnel modflared$runningTunnel = null;

    /*
     * Replaced by MultiplayerServerListPingerMixin
     * @Redirect(method =
     * "connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/util/profiler/PerformanceLog;)Lnet/minecraft/network/ClientConnection;",
     * at = @At(value = "INVOKE", target =
     * "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;"
     * ))
     * private static ChannelFuture connect(@NotNull InetSocketAddress address, boolean useEpoll, ClientConnection
     * connection) {
     * return ClientConnection.connect(Modflared.TUNNEL_MANAGER.handleConnect(address, connection).address(), useEpoll,
     * connection);
     * }
     */

    @Inject(method = "closeChannel", at = @At("TAIL"))
    public void disconnect(IChatComponent message, CallbackInfo ci) {
        synchronized (this) {
            if (this.modflared$runningTunnel != null) {
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
