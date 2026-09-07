package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by inbound velocity/explosion packet hooks. */
final class AntiVelocityModule extends ClientModule {
    AntiVelocityModule() {
        super("AntiVelocity", "Ignore player velocity and explosion knockback packets.", "Combat");
    }

    @Override
    public void tick(Minecraft mc) {}
}
