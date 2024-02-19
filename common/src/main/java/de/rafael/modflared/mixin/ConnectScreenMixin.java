package de.rafael.modflared.mixin;

import de.rafael.modflared.Modflared;
import de.rafael.modflared.interfaces.mixin.IConnectScreen;
import de.rafael.modflared.tunnel.TunnelStatus;
import io.netty.channel.ChannelFuture;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.gui.screen.multiplayer.ConnectScreen;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetSocketAddress;

@Mixin(targets = "net.minecraft.client.gui.screen.multiplayer.ConnectScreen$1")
abstract class ConnectScreen1Mixin implements Runnable {

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect" +
            "(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;"))
    private ChannelFuture connect(@NotNull InetSocketAddress address, boolean useEpoll, ClientConnection connection) {
        var status = Modflared.TUNNEL_MANAGER.handleConnect(address);
        Modflared.TUNNEL_MANAGER.prepareConnection(status, connection);

        var currentScreen =  MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof ConnectScreen connectScreen) {
            ((IConnectScreen) connectScreen).setStatus(status);
        }

        return ClientConnection.connect(status.runningTunnel().access().tunnelAddress(), useEpoll, connection);
    }

}

@Implements(@Interface(iface = IConnectScreen.class, prefix = "connectScreen$"))
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen implements IConnectScreen {

    protected ConnectScreenMixin(Text title) {
        super(title);
    }

    @Unique
    @Nullable
    public TunnelStatus modflared$status;

    @Intrinsic
    public void connectScreen$setStatus(TunnelStatus status) {
        this.modflared$status = status;
    }

    @Shadow
    private Text status;

    @Inject(method = "render", at = @At("TAIL"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // This screen starts drawing before the connection is established, so we need to check if the status is null
        // We're also checking if the status is the default "Connecting..." status, because we know we've connected to the server
        // when the status changes
        if (this.modflared$status == null || !status.equals(Text.translatable("connect.connecting"))) return;

        int y = this.height / 2 - 50;
        // Connecting Text is drawn at y = this.height / 2 - 50
        y += 10;

        for (Text status : this.modflared$status.generateFeedback()) {
            y += 10;
            context.drawCenteredTextWithShadow(this.textRenderer, status, this.width / 2, y, 16777215);
        }
    }

}
