package dev.billy948787.modflared.mixins.late;

import java.util.List;
import java.util.Set;

import javax.annotation.Nonnull;

import dev.billy948787.modflared.mixins.Mixins;
import org.jetbrains.annotations.NotNull;

import com.gtnewhorizon.gtnhmixins.ILateMixinLoader;
import com.gtnewhorizon.gtnhmixins.LateMixin;
import com.gtnewhorizon.gtnhmixins.builders.IMixins;

@LateMixin
public class LateMixinsLoader implements ILateMixinLoader {

    @Override
    public String getMixinConfig() {
        return "mixins.modflared.late.json";
    }

    @Nonnull
    @Override
    public @NotNull List<String> getMixins(Set<String> loadedMods) {
        return IMixins.getLateMixins(Mixins.class, loadedMods);
    }
}
