package de.rafael.modflared.api.tunnel;

@SuppressWarnings("unused")
public interface IModflaredApiTunnel {
    /**
     * Returns true if this is a modflared tunnel.
     * */
    default boolean isModflaredTunnel(String host, int port) {
        return false;
    }

    /**
     * Add a tunnel dependency.
     * */
    void addTunnelDependency(String host, int port, String id);

    /**
     * Remove a tunnel dependency.
     * */
    void removeTunnelDependency(String host, int port, String id);
}
