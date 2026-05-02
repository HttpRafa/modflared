package dev.httxrafa.modflared.mixin.client;

import dev.httxrafa.modflared.interfaces.mixin.IConnectScreen;
import dev.httxrafa.modflared.tunnel.TunnelStatus;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.util.text.ITextComponent;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Implements(@Interface(iface = IConnectScreen.class, prefix = "connectScreen$"))
@Mixin(GuiConnecting.class)
public abstract class GuiConnectingMixin extends GuiScreen implements IConnectScreen {

    @Unique
    private TunnelStatus modflared$status;

    @Inject(method = "drawScreen", at = @At("TAIL"))
    private void modflared$drawTunnelStatus(int mouseX, int mouseY, float partialTicks, CallbackInfo callbackInfo) {
        if (modflared$status == null) {
            return;
        }

        int y = this.height / 2 - 50;
        for (ITextComponent status : modflared$status.generateFeedback()) {
            y += 10;
            this.drawCenteredString(this.fontRenderer, status.getFormattedText(), this.width / 2, y, 16777215);
        }
    }

    @Intrinsic
    public void connectScreen$setStatus(TunnelStatus status) {
        this.modflared$status = status;
    }
}
