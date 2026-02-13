package dev.billy948787.modflared.mixins.late.client;

import dev.billy948787.modflared.Modflared;
import dev.billy948787.modflared.interfaces.mixin.IServerData;
import dev.billy948787.modflared.tunnel.TunnelStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Gui;
import net.minecraft.client.gui.GuiMultiplayer;
import net.minecraft.client.gui.ServerListEntryNormal;
import net.minecraft.client.multiplayer.ServerData;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ServerListEntryNormal.class)
public abstract class OnlineServerEntryMixin {

    @Shadow
    @Final
    private ServerData field_148301_e;

    @Shadow
    @Final
    private GuiMultiplayer field_148303_c;

    @Unique
    private static final ResourceLocation MODFLARED_INDICATOR_TEXTURE = new ResourceLocation(Modflared.MOD_ID, "textures/gui/indicator.png");

    @Inject(method = "drawEntry", at = @At("TAIL"))
    public void drawEntry(int p_148279_1_, int p_148279_2_, int p_148279_3_, int p_148279_4_, int p_148279_5_, Tessellator p_148279_6_, int p_148279_7_, int p_148279_8_, boolean p_148279_9_, CallbackInfo ci) {
        TunnelStatus tunnelStatus = ((IServerData) this.field_148301_e).getTunnelStatus();

        if (tunnelStatus != null && tunnelStatus.state() == TunnelStatus.State.USE) {
            Minecraft mc = Minecraft.getMinecraft();

            int iconX = p_148279_2_ + p_148279_4_ - 28;
            int iconY = p_148279_3_;
            int size = 10;


            mc.getTextureManager().bindTexture(MODFLARED_INDICATOR_TEXTURE);

            Gui.func_146110_a(iconX, iconY, 0, 0, size, size, size, size);

            int relativeMouseX = p_148279_7_ - p_148279_2_;
            int relativeMouseY = p_148279_8_ - p_148279_3_;


            if (p_148279_7_ >= iconX && p_148279_7_ <= iconX + size && p_148279_8_ >= iconY && p_148279_8_ <= iconY + size) {
                String tooltipText = EnumChatFormatting.AQUA + StatCollector.translateToLocal("gui.multiplayer.tunnel.status.0");

                this.field_148303_c.func_146793_a(tooltipText);
            }
        }
    }
}
