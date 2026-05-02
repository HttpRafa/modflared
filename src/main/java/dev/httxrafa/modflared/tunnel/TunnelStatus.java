package dev.httxrafa.modflared.tunnel;

import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.util.text.TextFormatting;
import net.minecraft.util.text.ITextComponent;

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
            feedback.add(new TextComponentTranslation("gui.tunnel.status.use").setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.AQUA)));
        } else if (state == State.FAILED_TO_DETERMINE) {
            feedback.add(new TextComponentTranslation("gui.tunnel.status.failed.0").setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.RED)));
            feedback.add(new TextComponentTranslation("gui.tunnel.status.failed.1").setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.RED)));
            feedback.add(new TextComponentTranslation("gui.tunnel.status.failed.2").setStyle(new net.minecraft.util.text.Style().setColor(TextFormatting.RED)));
        }
        return Collections.unmodifiableList(feedback);
    }

    public enum State {
        USE,
        DONT_USE,
        FAILED_TO_DETERMINE
    }
}
