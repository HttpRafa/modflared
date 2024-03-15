package de.rafael.modflared.tunnel;

import com.google.common.collect.Lists;
import net.minecraft.text.Text;
import net.minecraft.text.TranslatableText;
import net.minecraft.util.Formatting;
import org.jetbrains.annotations.Unmodifiable;

import java.util.ArrayList;
import java.util.List;

public class TunnelStatus {

    private final RunningTunnel runningTunnel;
    private final State state;

    public TunnelStatus(RunningTunnel runningTunnel, State state) {
        this.runningTunnel = runningTunnel;
        this.state = state;
    }

    public @Unmodifiable List<Text> generateFeedback() {
        if(state == State.USE) {
            return Lists.newArrayList(
                    new TranslatableText("gui.tunnel.status.use").formatted(Formatting.AQUA)
            );
        } else if(state == State.DONT_USE) {
            return Lists.newArrayList();
        } else if(state == State.FAILED_TO_DETERMINE) {
            return Lists.newArrayList(
                    new TranslatableText("gui.tunnel.status.failed.0").formatted(Formatting.RED),
                    new TranslatableText("gui.tunnel.status.failed.1").formatted(Formatting.RED),
                    new TranslatableText("gui.tunnel.status.failed.2").formatted(Formatting.RED)
            );
        }
        return Lists.newArrayList();
    }

    public RunningTunnel runningTunnel() {
        return runningTunnel;
    }

    public State state() {
        return state;
    }

    public enum State {
        USE,
        DONT_USE,
        FAILED_TO_DETERMINE
    }

}
