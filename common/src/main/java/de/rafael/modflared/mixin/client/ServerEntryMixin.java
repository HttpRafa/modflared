package de.rafael.modflared.mixin.client;

import de.rafael.modflared.Modflared;
import de.rafael.modflared.interfaces.mixin.IServerInfo;
import de.rafael.modflared.tunnel.TunnelStatus;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gui.DrawableHelper;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerScreen;
import net.minecraft.client.gui.screen.multiplayer.MultiplayerServerListWidget;
import net.minecraft.client.network.ServerInfo;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.Collections;

@Mixin(MultiplayerServerListWidget.ServerEntry.class)
public abstract class ServerEntryMixin {

    @Shadow @Final private ServerInfo server;
    @Shadow @Final private MultiplayerScreen screen;

    @Shadow @Final private MinecraftClient client;

    @Unique
    private static final Identifier MODFLARED_INDICATOR_TEXTURE = new Identifier(Modflared.MOD_ID, "textures/gui/indicator.png");

    @Inject(method = "render", at = @At("TAIL"))
    public void render(MatrixStack matrices, int index, int y, int x, int entryWidth, int entryHeight, int mouseX, int mouseY, boolean hovered, float tickDelta, CallbackInfo ci) {
        var tunnelStatus = ((IServerInfo) server).getTunnelStatus();
        if(tunnelStatus != null && tunnelStatus.state() == TunnelStatus.State.USE) {
            int xOffset = entryWidth - 15;
            int yOffset = 10 + 1;
            if(Modflared.PLATFORM.isForge()) {
                xOffset = entryWidth - (15 * 2) + 2;
            }

            this.client.getTextureManager().bindTexture(MODFLARED_INDICATOR_TEXTURE);
            DrawableHelper.drawTexture(matrices, x + xOffset, y + yOffset, 0, 0, 10, 10, 10, 10);

            // Tooltip
            int l = mouseX - x;
            int m = mouseY - y;
            if (l >= (Modflared.PLATFORM.isForge() ? entryWidth - (15 * 2) + 2 : entryWidth - 15) && l <= (Modflared.PLATFORM.isForge() ? entryWidth - 15 - 5 : entryWidth - 5) && m >= 9 && m <= 9 + 10) {
                this.screen.setTooltip(Collections.singletonList(new TranslatableText("gui.multiplayer.tunnel.status.0").formatted(Formatting.AQUA)));
            }
        }
    }

}
