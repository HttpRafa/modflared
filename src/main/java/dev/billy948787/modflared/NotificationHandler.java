package dev.billy948787.modflared;

import cpw.mods.fml.common.eventhandler.SubscribeEvent;
import cpw.mods.fml.common.gameevent.PlayerEvent;
import dev.billy948787.modflared.tunnel.manager.TunnelManager;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.EnumChatFormatting;

public class NotificationHandler {
    @SubscribeEvent
    public void onPlayerLoggedIn(PlayerEvent.PlayerLoggedInEvent event) {
        if (TunnelManager.HAS_ERROR) {
            var msg = new ChatComponentTranslation("gui.toast.body.error");

            msg.getChatStyle().setColor(EnumChatFormatting.RED);

            event.player.addChatMessage(msg);

            TunnelManager.HAS_ERROR = false;
        }
    }
}
