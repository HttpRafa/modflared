package de.rafael.modflared.fabric.mixins;

import de.rafael.modflared.fabric.Modflared;
import io.netty.channel.ChannelFuture;
import net.minecraft.network.ClientConnection;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetSocketAddress;

@Mixin(targets = "net.minecraft.client.gui.screen.multiplayer.ConnectScreen$1")
public abstract class ConnectScreenMixin implements Runnable {

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;"))
    private ChannelFuture connect(@NotNull InetSocketAddress address, boolean useEpoll, ClientConnection connection) {
        return ClientConnection.connect(Modflared.TUNNEL_MANAGER.handleConnect(address, connection), useEpoll, connection);
    }

}
