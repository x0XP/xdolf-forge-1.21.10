package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

final class TracersModule extends ClientModule {
    TracersModule() {
        super("Tracers", "Original distance-coloured player and chest tracers.", "Render");
        toggle("players", true);
        toggle("chests", false);
    }

    @Override
    public void tick(Minecraft mc) {}
}
