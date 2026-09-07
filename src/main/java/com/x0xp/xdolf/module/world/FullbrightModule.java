package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the light-map/client render hooks. */
final class FullbrightModule extends ClientModule {
    FullbrightModule() {
        super("Fullbright", "Client-side night vision without changing real potion effects.", "World");
    }

    @Override
    public void tick(Minecraft mc) {}
}
