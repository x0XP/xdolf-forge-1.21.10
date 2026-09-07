package com.x0xp.xdolf.module.combat;

import com.x0xp.xdolf.module.ClientModule;


import net.minecraft.client.Minecraft;

/** Behaviour is supplied by inbound velocity/explosion packet hooks. */
public final class AntiVelocityModule extends ClientModule {
    public AntiVelocityModule() {
        super("AntiVelocity", "Ignore player velocity and explosion knockback packets.", "Combat");
    }

    @Override
    public void tick(Minecraft mc) {}
}
