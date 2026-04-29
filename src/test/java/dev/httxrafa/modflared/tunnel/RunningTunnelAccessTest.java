package dev.httxrafa.modflared.tunnel;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class RunningTunnelAccessTest {

    @Test
    public void computePortIsDeterministicForSameHost() {
        int first = RunningTunnel.Access.computePort("example.com");
        int second = RunningTunnel.Access.computePort("example.com");

        assertEquals(first, second);
    }

    @Test
    public void computePortStaysInMinecraftCompatibleRange() {
        int port = RunningTunnel.Access.computePort("play.example.com");

        assertTrue(port >= 25565);
        assertTrue(port <= 65530);
    }

    @Test
    public void localAccessUsesLoopbackAndComputedPort() {
        RunningTunnel.Access access = RunningTunnel.Access.localWithRandomPort("play.example.com");

        assertEquals("tcp", access.getProtocol());
        assertEquals("play.example.com", access.getHostname());
        assertEquals("127.0.0.1", access.getTunnelAddress().getHostString());
        assertEquals(RunningTunnel.Access.computePort("play.example.com"), access.getTunnelAddress().getPort());
    }
}
