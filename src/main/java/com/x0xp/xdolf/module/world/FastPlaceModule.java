package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the client interaction-delay hook. */
final class FastPlaceModule extends ClientModule {
    FastPlaceModule() {
        super("FastPlace", "Remove the client-side right-click delay.", "World");
    }

    @Override
    public void tick(Minecraft mc) {}
}
