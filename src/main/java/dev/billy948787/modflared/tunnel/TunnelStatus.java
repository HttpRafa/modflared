package dev.billy948787.modflared.tunnel;

import com.github.bsideup.jabel.Desugar;
import com.google.common.collect.ImmutableList;
import net.minecraft.util.ChatComponentTranslation;
import net.minecraft.util.ChatStyle;
import net.minecraft.util.EnumChatFormatting;
import net.minecraft.util.IChatComponent;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public @Desugar record TunnelStatus(RunningTunnel runningTunnel, State state) {

    public @Unmodifiable List<IChatComponent> generateFeedback() {
        return switch (state) {
            case USE -> ImmutableList.of(
                new ChatComponentTranslation("gui.tunnel.status.use").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.AQUA))
            );
            case DONT_USE -> ImmutableList.of();
            case FAILED_TO_DETERMINE -> ImmutableList.of(
                new ChatComponentTranslation("gui.tunnel.status.failed.0").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)),
                new ChatComponentTranslation("gui.tunnel.status.failed.1").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED)),
                new ChatComponentTranslation("gui.tunnel.status.failed.2").setChatStyle(new ChatStyle().setColor(EnumChatFormatting.RED))
            );
        };
    }

    public enum State {
        USE,
        DONT_USE,
        FAILED_TO_DETERMINE
    }

}
