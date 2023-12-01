package de.rafael.modflared.forge.mixins;

import de.rafael.modflared.forge.Modflared;
import net.minecraft.client.Minecraft;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.concurrent.ExecutionException;

@Mixin(Minecraft.class)
public class MinecraftClientMixin {
    @Inject(method = "clearLevel(Lnet/minecraft/client/gui/screens/Screen;)V", at = @At("TAIL"))
    public void disconnect(CallbackInfo ci) {
        Modflared.LOGGER.info("Disconnecting from server, stopping cloudflared...");
        if (Modflared.PROGRAM.isCompletedExceptionally() || Modflared.PROGRAM.isCancelled() || !Modflared.PROGRAM.isDone()) {
            Modflared.LOGGER.info("Cloudflared is not running, skipping...");
            return;
        }

        try {
            Modflared.PROGRAM.get().closeTunnel();
            Modflared.LOGGER.info("Cloudflared stopped successfully");
        } catch (InterruptedException | ExecutionException e) {
            Modflared.LOGGER.error("Error while stopping cloudflared: {}", e.getMessage(), e);
        }
    }
}
