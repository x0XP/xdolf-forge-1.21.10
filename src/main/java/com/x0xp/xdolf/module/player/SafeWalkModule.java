package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the SafeWalk movement mixin. */
final class SafeWalkModule extends ClientModule {
    SafeWalkModule() {
        super("SafeWalk", "Apply edge protection while walking on the ground.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {}
}
