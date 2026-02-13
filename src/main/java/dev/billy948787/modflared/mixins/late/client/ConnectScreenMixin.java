package dev.billy948787.modflared.mixins.late.client;

import dev.billy948787.modflared.interfaces.mixin.IConnectScreen;
import dev.billy948787.modflared.tunnel.TunnelStatus;
import net.minecraft.client.gui.GuiScreen;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.network.NetworkManager;
import net.minecraft.util.IChatComponent;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Implements(@Interface(iface = IConnectScreen.class, prefix = "connectScreen$"))
@Mixin(GuiConnecting.class)
public abstract class ConnectScreenMixin extends GuiScreen implements IConnectScreen {

    @Unique
    @Nullable
    public TunnelStatus modflared$status;

    @Intrinsic
    public void connectScreen$setStatus(TunnelStatus status) {
        this.modflared$status = status;
    }

    @Shadow
    private NetworkManager field_146371_g;

    @Inject(method = "drawScreen", at = @At("Tail"))
    public void drawScreen(int mouseX, int mouseY, float partialTicks, CallbackInfo ci) {
        if (this.modflared$status == null || this.field_146371_g != null) return;

        int y = this.height / 2 - 50;

        y += 10;

        for (IChatComponent feedback : this.modflared$status.generateFeedback()) {
            y += 10;
            this.drawCenteredString(this.fontRendererObj, feedback.getFormattedText(), this.width / 2, y, 16777215);
        }
    }

}
