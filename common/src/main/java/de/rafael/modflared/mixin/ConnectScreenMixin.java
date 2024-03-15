package de.rafael.modflared.mixin;

import de.rafael.modflared.Modflared;
import de.rafael.modflared.interfaces.mixin.IConnectScreen;
import de.rafael.modflared.tunnel.TunnelStatus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.screen.ConnectScreen;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.network.ClientConnection;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.net.InetAddress;
import java.net.InetSocketAddress;

@Mixin(targets = "net.minecraft.client.gui.screen.ConnectScreen$1")
abstract class ConnectScreen1Mixin implements Runnable {

    @Redirect(method = "run", at = @At(value = "INVOKE", target = "Lnet/minecraft/network/ClientConnection;connect(Ljava/net/InetAddress;IZ)Lnet/minecraft/network/ClientConnection;"))
    private @NotNull ClientConnection connect(InetAddress inetAddress, int port, boolean shouldUseNativeTransport) {
        InetSocketAddress address = new InetSocketAddress(inetAddress, port);
        TunnelStatus status = Modflared.TUNNEL_MANAGER.handleConnect(address);

        Screen currentScreen =  MinecraftClient.getInstance().currentScreen;
        if (currentScreen instanceof ConnectScreen) {
            ((IConnectScreen) currentScreen).setStatus(status);
        }

        InetSocketAddress targetAddress = status.state() == TunnelStatus.State.USE ? status.runningTunnel().access().tunnelAddress() : address;
        ClientConnection connection = ClientConnection.connect(targetAddress.getAddress(), targetAddress.getPort(), shouldUseNativeTransport);
        Modflared.TUNNEL_MANAGER.prepareConnection(status, connection);

        return connection;
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
    public void render(MatrixStack matrices, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // This screen starts drawing before the connection is established, so we need to check if the status is null
        // We're also checking if the status is the default "Connecting..." status, because we know we've connected to the server
        // when the status changes
        if (this.modflared$status == null || !status.equals(new TranslatableText("connect.connecting"))) return;

        int y = this.height / 2 - 50;
        // Connecting Text is drawn at y = this.height / 2 - 50
        y += 10;

        for (Text status : this.modflared$status.generateFeedback()) {
            y += 10;
            drawCenteredText(matrices, this.textRenderer, status, this.width / 2, y, 16777215);
        }
    }

}
