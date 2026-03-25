package dev.httxrafa.modflared.mixin.client;

import dev.httxrafa.modflared.interfaces.mixin.IConnectScreen;
import dev.httxrafa.modflared.tunnel.TunnelStatus;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.ConnectScreen;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Implements(@Interface(iface = IConnectScreen.class, prefix = "connectScreen$"))
@Mixin(ConnectScreen.class)
public abstract class ConnectScreenMixin extends Screen implements IConnectScreen {

    protected ConnectScreenMixin(Component title) {
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
    private Component status;

    @Inject(method = "extractRenderState", at = @At("TAIL"))
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta, CallbackInfo ci) {
        // This screen starts drawing before the connection is established, so we need to check if the status is null
        // We're also checking if the status is the default "Connecting..." status, because we know we've connected to the server
        // when the status changes
        if (this.modflared$status == null || !status.equals(Component.translatable("connect.connecting"))) return;

        int y = this.height / 2 - 50;
        // Connecting Text is drawn at y = this.height / 2 - 50
        y += 10;

        for (Component status : this.modflared$status.generateFeedback()) {
            y += 10;
            graphics.centeredText(this.font, status, this.width / 2, y, 16777215);
        }
    }

}
