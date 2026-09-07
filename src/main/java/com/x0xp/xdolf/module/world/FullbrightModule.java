package com.x0xp.xdolf.module.world;

import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the light-map/client render hooks. */
public final class FullbrightModule extends ClientModule {
    public FullbrightModule() {
        super("Fullbright", "Client-side night vision without changing real potion effects.", "World");
    }

    @Override
    public void tick(Minecraft mc) {}
}
