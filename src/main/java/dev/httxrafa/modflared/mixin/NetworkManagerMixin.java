package dev.httxrafa.modflared.mixin;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.interfaces.mixin.IConnection;
import dev.httxrafa.modflared.tunnel.RunningTunnel;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Implements(@Interface(iface = IConnection.class, prefix = "connection$"))
@Mixin(NetworkManager.class)
public abstract class NetworkManagerMixin implements IConnection {

    @Unique
    private RunningTunnel modflared$runningTunnel;

    @Inject(method = "closeChannel", at = @At("TAIL"))
    private void modflared$closeTunnelOnDisconnect(CallbackInfo callbackInfo) {
        synchronized (this) {
            if (modflared$runningTunnel != null) {
                Modflared.TUNNEL_MANAGER.closeTunnel(modflared$runningTunnel);
                modflared$runningTunnel = null;
            }
        }
    }

    @Intrinsic
    public void connection$setRunningTunnel(RunningTunnel runningTunnel) {
        this.modflared$runningTunnel = runningTunnel;
    }
}
