package dev.billy948787.modflared.mixins;

import javax.annotation.Nonnull;

import com.gtnewhorizon.gtnhmixins.builders.IMixins;
import com.gtnewhorizon.gtnhmixins.builders.MixinBuilder;

public enum Mixins implements IMixins {

    // Read the java doc of IMixins and MixinBuilder for further information

    // You should declare all of your mixins early and late in this same enum

    MIXIN_EARLY(new MixinBuilder().setPhase(Phase.EARLY)
        .addClientMixins("ServerDataMixin", "ConnectionMixin")),

    MIXIN_LATE(new MixinBuilder().setPhase(Phase.LATE)
        .addClientMixins(
            "ConnectScreenMixin",
            "ConnectScreenRunnableMixin",
            "OnlineServerEntryMixin",
            "ServerStatusPingerMixin"));

    private final MixinBuilder builder;

    Mixins(MixinBuilder builder) {
        this.builder = builder;
    }

    @Nonnull
    @Override
    public MixinBuilder getBuilder() {
        return builder;
    }
}
