package dev.httxrafa.modflared.tunnel;

import net.minecraft.util.text.ITextComponent;
import net.minecraft.util.text.TextComponentString;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TunnelStatus {

    private final RunningTunnel runningTunnel;
    private final State state;

    public TunnelStatus(RunningTunnel runningTunnel, State state) {
        this.runningTunnel = runningTunnel;
        this.state = state;
    }

    public RunningTunnel getRunningTunnel() {
        return runningTunnel;
    }

    public State getState() {
        return state;
    }

    public List<ITextComponent> generateFeedback() {
        List<ITextComponent> feedback = new ArrayList<ITextComponent>();
        if (state == State.USE) {
            feedback.add(translate("gui.tunnel.status.use", "Using Cloudflare tunnel", TextFormatting.AQUA));
        } else if (state == State.FAILED_TO_DETERMINE) {
            feedback.add(translate("gui.tunnel.status.failed.0", "Modflared could not determine if a tunnel is required.", TextFormatting.RED));
            feedback.add(translate("gui.tunnel.status.failed.1", "The connection will continue without a tunnel.", TextFormatting.RED));
            feedback.add(translate("gui.tunnel.status.failed.2", "Add this server to forced_tunnels.json if it must use a tunnel.", TextFormatting.RED));
        }
        return Collections.unmodifiableList(feedback);
    }

    private static ITextComponent translate(String key, String fallback, TextFormatting formatting) {
        ITextComponent component = new TextComponentTranslation(key);
        if (component.getUnformattedText().equals(key)) {
            component = new TextComponentString(fallback);
        }
        return component.setStyle(new net.minecraft.util.text.Style().setColor(formatting));
    }

    public enum State {
        USE,
        DONT_USE,
        FAILED_TO_DETERMINE
    }
}
