package de.rafael.modflared.mixin;

import de.rafael.modflared.Modflared;
import de.rafael.modflared.interfaces.mixin.ConnectScreenInterface;
import de.rafael.modflared.tunnel.manager.TunnelManager;
import de.rafael.modflared.tunnel.manager.TunnelManager.HandleConnectResult;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Mixin(targets = "net.minecraft.client.gui.screen.ConnectScreen$1")
abstract class ConnectScreen1Mixin implements Runnable {

    /*@Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect" +
            "(Ljava/net/InetSocketAddress;ZLnet/minecraft/network/ClientConnection;)Lio/netty/channel/ChannelFuture;"))
    private ChannelFuture connect(@NotNull InetSocketAddress address, boolean useEpoll, ClientConnection connection) {
        var handleConnectResult = Modflared.TUNNEL_MANAGER.handleConnect(address, connection);

        var currentScreen =  MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof ConnectScreen connectScreen) {
            ((ConnectScreenInterface) connectScreen).modflared$setStatus(handleConnectResult);
        }

        return ClientConnection.connect(handleConnectResult.address(), useEpoll, connection);
    }*/
    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetAddress;IZ)Lnet/minecraft/network/ClientConnection;"))
    private ClientConnection connect(InetAddress inetAddress, int port, boolean shouldUseNativeTransport) {
        var handleConnectResult = Modflared.TUNNEL_MANAGER.handleConnect(new InetSocketAddress(inetAddress, port));

        var currentScreen =  MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof ConnectScreen connectScreen) {
            ((ConnectScreenInterface) connectScreen).modflared$setStatus(handleConnectResult);
        }

        var address = handleConnectResult.address();
        var connection = ClientConnection.connect(address.getAddress(), address.getPort(), shouldUseNativeTransport);

        var tunnelConnection = (TunnelManager.Connection) connection;
        tunnelConnection.setRunningTunnel(handleConnectResult.runningTunnel());
        return connection;
    }

}

@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen implements ConnectScreenInterface {

    protected ConnectScreenMixin(Text title) {
        super(title);
    }

    @Unique
    @Nullable
    public TunnelManager.HandleConnectResult modflared$status;

    @Override
    public void modflared$setStatus(HandleConnectResult status) {
        this.modflared$status = status;
    }

    @Shadow
    private Text status;

    /*@Inject(method = "render", at = @At("TAIL"))
    public void render(DrawContext context, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // This screen starts drawing before the connection is established, so we need to check if the status is null
        // We're also checking if the status is the default "Connecting..." status, because we know we've connected to the server
        // when the status changes
        if (this.modflared$status == null || !status.equals(Text.translatable("connect.connecting"))) return;

        int y = this.height / 2 - 50;
        // Connecting Text is drawn at y = this.height / 2 - 50
        y += 10;

        for (Text status : this.modflared$status.getStatusFeedback()) {
            y += 10;
            context.drawCenteredTextWithShadow(this.textRenderer, status, this.width / 2, y, 16777215);

        }
    }*/
    @Inject(method = "render", at = @At("TAIL"))
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // This screen starts drawing before the connection is established, so we need to check if the status is null
        // We're also checking if the status is the default "Connecting..." status, because we know we've connected to the server
        // when the status changes
        if (this.modflared$status == null || !status.equals(new TranslatableText("connect.connecting"))) return;

        int y = this.height / 2 - 50;
        // Connecting Text is drawn at y = this.height / 2 - 50
        y += 10;

        for (Text status : this.modflared$status.getStatusFeedback()) {
            y += 10;
            drawCenteredText(matrices, this.textRenderer, status, this.width / 2, y, 16777215);
        }
    }
}
