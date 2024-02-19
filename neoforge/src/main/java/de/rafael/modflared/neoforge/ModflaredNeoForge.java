package de.rafael.modflared.neoforge;

import de.rafael.modflared.Modflared;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.event.lifecycle.FMLClientSetupEvent;
import org.jetbrains.annotations.NotNull;

@Mod(Modflared.MOD_ID)
public class ModflaredNeoForge {

    public ModflaredNeoForge(@NotNull IEventBus eventBus) {
        eventBus.register(this);
    }

    @SubscribeEvent
    public void onClientSetup(FMLClientSetupEvent event) {
        Modflared.init();
    }

}
