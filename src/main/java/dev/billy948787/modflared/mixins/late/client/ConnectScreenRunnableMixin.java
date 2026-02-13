package dev.billy948787.modflared.mixins.late.client;

import java.net.InetAddress;
import java.net.InetSocketAddress;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.GuiConnecting;
import net.minecraft.network.NetworkManager;

import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

import dev.billy948787.modflared.Modflared;
import dev.billy948787.modflared.interfaces.mixin.IConnectScreen;
import dev.billy948787.modflared.tunnel.TunnelStatus;

@Mixin(targets = "net.minecraft.client.multiplayer.GuiConnecting$1")
public class ConnectScreenRunnableMixin {

    @Redirect(
        method = "run",
        at = @At(
            value = "INVOKE",
            target = "Lnet/minecraft/network/NetworkManager;provideLanClient(Ljava/net/InetAddress;I)Lnet/minecraft/network/NetworkManager;"))
    private @NotNull NetworkManager connect(@NotNull InetAddress address, int port) {
        var originalInetSocketAddress = new InetSocketAddress(address, port);

        var status = Modflared.TUNNEL_MANAGER.handleConnect(originalInetSocketAddress);

        var targetInetSocketAddress = (status.state() == TunnelStatus.State.USE) ? status.runningTunnel()
            .access()
            .tunnelAddress() : originalInetSocketAddress;

        var networkManager = NetworkManager
            .provideLanClient(targetInetSocketAddress.getAddress(), targetInetSocketAddress.getPort());
        Modflared.TUNNEL_MANAGER.prepareConnection(status, networkManager);

        var currentScreen = Minecraft.getMinecraft().currentScreen;

        if (currentScreen instanceof GuiConnecting) {
            ((IConnectScreen) currentScreen).setStatus(status);
        }

        return networkManager;
    }
}
