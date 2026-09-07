package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by outbound movement/action packet hooks. */
final class AntiHungerModule extends ClientModule {
    AntiHungerModule() {
        super("AntiHunger", "Suppress sprint notifications and grounded movement flags; server-dependent.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {}
}
