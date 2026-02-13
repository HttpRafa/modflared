package dev.billy948787.modflared.interfaces.mixin;

import dev.billy948787.modflared.tunnel.TunnelStatus;

public interface IServerData {

    void setTunnelStatus(TunnelStatus status);

    TunnelStatus getTunnelStatus();

}
