package com.x0xp.xdolf.module.world;

import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the client interaction-delay hook. */
public final class FastPlaceModule extends ClientModule {
    public FastPlaceModule() {
        super("FastPlace", "Remove the client-side right-click delay.", "World");
    }

    @Override
    public void tick(Minecraft mc) {}
}
