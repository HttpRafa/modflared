package dev.billy948787.modflared.mixins.early;

import net.minecraft.client.multiplayer.ServerData;

import org.spongepowered.asm.mixin.Implements;
import org.spongepowered.asm.mixin.Interface;
import org.spongepowered.asm.mixin.Intrinsic;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;

import dev.billy948787.modflared.interfaces.mixin.IServerData;
import dev.billy948787.modflared.tunnel.TunnelStatus;

@Implements(@Interface(iface = IServerData.class, prefix = "serverData$"))
@Mixin(ServerData.class)
public abstract class ServerDataMixin implements IServerData {

    @Unique
    private TunnelStatus modflared$tunnelStatus;

    @Intrinsic
    public void serverData$setTunnelStatus(TunnelStatus status) {
        this.modflared$tunnelStatus = status;
    }

    @Intrinsic
    public TunnelStatus serverData$getTunnelStatus() {
        return modflared$tunnelStatus;
    }

}
