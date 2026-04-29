package dev.httxrafa.modflared.mixin.client;

import dev.httxrafa.modflared.Modflared;
import dev.httxrafa.modflared.interfaces.mixin.IConnectScreen;
import dev.httxrafa.modflared.tunnel.TunnelStatus;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.network.NetworkManager;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;

@Mixin(targets = "net.minecraft.client.multiplayer.GuiConnecting$1")
public class GuiConnectingThreadMixin {

    @Redirect(
            method = "run",
            at = @At(
                    value = "INVOKE",
                    target = "Lnet/minecraft/network/NetworkManager;createNetworkManagerAndConnect(Ljava/net/InetAddress;IZ)Lnet/minecraft/network/NetworkManager;"
            )
    )
    private NetworkManager modflared$routeDirectConnect(InetAddress address, int port, boolean useNativeTransport) {
        InetSocketAddress original = new InetSocketAddress(address, port);
        TunnelStatus status = Modflared.TUNNEL_MANAGER.handleConnect(original);

        Minecraft minecraft = Minecraft.getMinecraft();
        if (minecraft.currentScreen instanceof GuiConnecting) {
            ((IConnectScreen) minecraft.currentScreen).setStatus(status);
        }

        InetSocketAddress target;
        if (status.getState() == TunnelStatus.State.USE && status.getRunningTunnel() != null) {
            target = status.getRunningTunnel().getAccess().getTunnelAddress();
        } else {
            target = original;
        }

        try {
            NetworkManager manager = NetworkManager.createNetworkManagerAndConnect(
                    InetAddress.getByName(target.getHostString()),
                    target.getPort(),
                    useNativeTransport
            );
            Modflared.TUNNEL_MANAGER.prepareConnection(status, manager);
            return manager;
        } catch (UnknownHostException exception) {
            throw new RuntimeException("Failed to resolve tunnel target " + target, exception);
        }
    }
}
