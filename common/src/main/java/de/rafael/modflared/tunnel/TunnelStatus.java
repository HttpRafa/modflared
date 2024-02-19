package de.rafael.modflared.tunnel;

import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

public record TunnelStatus(RunningTunnel runningTunnel, State state) {

    public @Unmodifiable List<Text> generateFeedback() {
        return switch (state) {
            case USE -> List.of(
                    new TranslatableText("gui.tunnel.status.use").formatted(Formatting.AQUA)
            );
            case DONT_USE -> List.of();
            case FAILED_TO_DETERMINE -> List.of(
                    new TranslatableText("gui.tunnel.status.failed.0").formatted(Formatting.RED),
                    new TranslatableText("gui.tunnel.status.failed.1").formatted(Formatting.RED),
                    new TranslatableText("gui.tunnel.status.failed.2").formatted(Formatting.RED)
            );
        };
    }

    public enum State {
        USE,
        DONT_USE,
        FAILED_TO_DETERMINE
    }

}
