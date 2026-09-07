package com.x0xp.xdolf.module.combat;

import com.x0xp.xdolf.module.ClientModule;


import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the outbound attack packet hook. */
public final class CriticalsModule extends ClientModule {
    public CriticalsModule() {
        super("Criticals", "Send a short airborne packet sequence before melee attacks.", "Combat");
    }

    @Override
    public void tick(Minecraft mc) {}
}
