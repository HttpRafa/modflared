package de.rafael.modflared.mixin.client;

import de.rafael.modflared.interfaces.mixin.IServerInfo;
import de.rafael.modflared.tunnel.TunnelStatus;
import net.minecraft.client.network.ServerInfo;
import org.spongepowered.asm.mixin.*;

@Implements(@Interface(iface = IServerInfo.class, prefix = "serverInfo$"))
@Mixin(ServerInfo.class)
public abstract class ServerInfoMixin implements IServerInfo {

    @Unique
    private TunnelStatus modflared$tunnelStatus;

    @Intrinsic
    public void serverInfo$setTunnelStatus(TunnelStatus status) {
        this.modflared$tunnelStatus = status;
    }

    @Intrinsic
    public TunnelStatus serverInfo$getTunnelStatus() {
        return modflared$tunnelStatus;
    }

}
