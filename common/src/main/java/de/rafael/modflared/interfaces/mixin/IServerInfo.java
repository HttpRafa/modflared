package de.rafael.modflared.interfaces.mixin;

import de.rafael.modflared.tunnel.TunnelStatus;

public interface IServerInfo {

    void setTunnelStatus(TunnelStatus status);
    TunnelStatus getTunnelStatus();

}
