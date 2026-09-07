package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by entity render hooks. */
final class ChamsModule extends ClientModule {
    ChamsModule() {
        super("Chams", "Render textured living-entity models through walls.", "Render");
    }

    @Override
    public void tick(Minecraft mc) {}
}
