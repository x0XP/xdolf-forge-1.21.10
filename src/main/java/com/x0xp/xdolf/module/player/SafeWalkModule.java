package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the SafeWalk movement mixin. */
public final class SafeWalkModule extends ClientModule {
    public SafeWalkModule() {
        super("SafeWalk", "Apply edge protection while walking on the ground.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {}
}
