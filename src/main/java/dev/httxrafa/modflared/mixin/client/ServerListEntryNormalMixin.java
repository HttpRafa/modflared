package dev.httxrafa.modflared.mixin.client;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.interfaces.mixin.IServerData;
import dev.httxrafa.modflared.tunnel.TunnelStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.util.ResourceLocation;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerListEntryNormal.class)
public abstract class ServerListEntryNormalMixin {

    @Shadow
    @Final
    private GuiMultiplayer owner;

    @Shadow
    @Final
    private ServerData server;

    @Unique
    private static final ResourceLocation MODFLARED_INDICATOR_TEXTURE = new ResourceLocation(
            Modflared.MOD_ID,
            "textures/gui/sprites/icon/indicator.png"
    );

    @Unique
    private static final int MODFLARED_INDICATOR_SIZE = 10;

    @Unique
    private static final int MODFLARED_INDICATOR_RIGHT_OFFSET = 28;

    @Unique
    private static final String MODFLARED_INDICATOR_TOOLTIP = "Modflared in use";

    @Inject(method = "drawEntry", at = @At("TAIL"))
    private void modflared$drawTunnelIndicator(int slotIndex, int x, int y, int listWidth, int slotHeight, int mouseX, int mouseY, boolean isSelected, float partialTicks, CallbackInfo callbackInfo) {
        TunnelStatus tunnelStatus = ((IServerData) this.server).getTunnelStatus();
        if (tunnelStatus == null || tunnelStatus.getState() != TunnelStatus.State.USE) {
            return;
        }

        int indicatorX = x + listWidth - MODFLARED_INDICATOR_RIGHT_OFFSET;
        int indicatorY = y + 11;

        Minecraft.getMinecraft().getTextureManager().bindTexture(MODFLARED_INDICATOR_TEXTURE);
        Gui.drawModalRectWithCustomSizedTexture(
                indicatorX,
                indicatorY,
                0.0F,
                0.0F,
                MODFLARED_INDICATOR_SIZE,
                MODFLARED_INDICATOR_SIZE,
                MODFLARED_INDICATOR_SIZE,
                MODFLARED_INDICATOR_SIZE
        );

        if (mouseX >= indicatorX && mouseX <= indicatorX + MODFLARED_INDICATOR_SIZE && mouseY >= indicatorY && mouseY <= indicatorY + MODFLARED_INDICATOR_SIZE) {
            this.owner.setHoveringText(MODFLARED_INDICATOR_TOOLTIP);
        }
    }
}
