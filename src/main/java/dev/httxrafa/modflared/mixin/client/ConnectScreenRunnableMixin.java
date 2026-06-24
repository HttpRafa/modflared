package dev.httxrafa.modflared.mixin.client;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.interfaces.mixin.IConnectScreen;
import dev.httxrafa.modflared.tunnel.TunnelStatus;
import io.netty.channel.ChannelFuture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.network.Connection;
import net.minecraft.server.network.EventLoopGroupHolder;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;

@Mixin(targets = "net.minecraft.client.gui.screens.ConnectScreen$1")
public class ConnectScreenRunnableMixin {

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/Connection;connect(Ljava/net/InetSocketAddress;Lnet/minecraft/server/network/EventLoopGroupHolder;Lnet/minecraft/network/Connection;)Lio/netty/channel/ChannelFuture;"))
    private @NotNull ChannelFuture connect(@NotNull InetSocketAddress address, EventLoopGroupHolder holder, Connection connection) {
        var status = Modflared.TUNNEL_MANAGER.handleConnect(address);
        Modflared.TUNNEL_MANAGER.prepareConnection(status, connection);

        var currentScreen = Minecraft.getInstance().gui.screen();
        if (currentScreen instanceof ConnectScreen connectScreen) {
            ((IConnectScreen) connectScreen).setStatus(status);
        }

        return Connection.connect(status.state() == TunnelStatus.State.USE ? status.runningTunnel().access().tunnelAddress() : address, holder, connection);
    }

}
