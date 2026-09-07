package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by outbound movement/action packet hooks. */
public final class AntiHungerModule extends ClientModule {
    public AntiHungerModule() {
        super("AntiHunger", "Suppress sprint notifications and grounded movement flags; server-dependent.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {}
}
