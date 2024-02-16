package de.rafael.modflared.interfaces.mixin;

import de.rafael.modflared.tunnel.manager.TunnelManager.HandleConnectResult;

public interface ConnectScreenInterface {

    void modflared$setStatus(HandleConnectResult status);

}