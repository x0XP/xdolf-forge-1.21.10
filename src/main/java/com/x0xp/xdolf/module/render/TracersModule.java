package com.x0xp.xdolf.module.render;

import com.x0xp.xdolf.module.ClientModule;
import net.minecraft.client.Minecraft;

public final class TracersModule extends ClientModule {
    public TracersModule() {
        super("Tracers", "Distance-coloured player and chest tracers aligned to visible targets.", "Render");
        toggle("players", true);
        toggle("chests", false);
    }

    @Override
    public void tick(Minecraft mc) {}
}
