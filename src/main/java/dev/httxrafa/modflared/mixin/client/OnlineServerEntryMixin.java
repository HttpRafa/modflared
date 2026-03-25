package dev.httxrafa.modflared.mixin.client;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.interfaces.mixin.IServerData;
import dev.httxrafa.modflared.tunnel.TunnelStatus;
import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.multiplayer.ServerSelectionList;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerSelectionList.OnlineServerEntry.class)
public abstract class OnlineServerEntryMixin extends ServerSelectionList.Entry {

    @Shadow @Final private ServerData serverData;

    @Unique
    private static final Identifier MODFLARED_INDICATOR_TEXTURE = Identifier.fromNamespaceAndPath(Modflared.MOD_ID, "icon/indicator");

    @Inject(method = "extractContent", at = @At("TAIL"))
    public void extractContent(GuiGraphicsExtractor graphics, int mouseX, int mouseY, boolean hovered, float deltaTicks, CallbackInfo callbackInfo) {
        var tunnelStatus = ((IServerData) serverData).getTunnelStatus();
        if(tunnelStatus != null && tunnelStatus.state() == TunnelStatus.State.USE) {
            int xOffset = this.getContentWidth() - 15;
            int yOffset = 10 + 1;
            int x = this.getContentX();
            int y = this.getContentY();
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, MODFLARED_INDICATOR_TEXTURE, x + xOffset, y + yOffset, 10, 10);

            // Tooltip
            int l = mouseX - x;
            int m = mouseY - y;
            if (l >= this.getContentWidth() - 15 && l <= this.getContentWidth() - 5 && m >= 9 && m <= 9 + 10) {
                graphics.setTooltipForNextFrame(Component.translatable("gui.multiplayer.tunnel.status.0").withStyle(ChatFormatting.AQUA), mouseX, mouseY);
            }
        }
    }

}
