package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the outbound attack packet hook. */
final class CriticalsModule extends ClientModule {
    CriticalsModule() {
        super("Criticals", "Send a short airborne packet sequence before melee attacks.", "Combat");
    }

    @Override
    public void tick(Minecraft mc) {}
}
