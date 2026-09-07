package com.x0xp.xdolf.module.render;

import com.x0xp.xdolf.module.ClientModule;


import net.minecraft.client.Minecraft;

public final class TracersModule extends ClientModule {
    public TracersModule() {
        super("Tracers", "Original distance-coloured player and chest tracers.", "Render");
        toggle("players", true);
        toggle("chests", false);
    }

    @Override
    public void tick(Minecraft mc) {}
}
