package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the client movement/block hooks. */
final class NoSlowdownModule extends ClientModule {
    NoSlowdownModule() {
        super("NoSlowdown", "Reduce ice slipperiness on the client, matching the original module.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {}
}
