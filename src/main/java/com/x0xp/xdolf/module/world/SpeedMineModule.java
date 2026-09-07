package com.x0xp.xdolf.module.world;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

public final class SpeedMineModule extends ClientModule {
    public SpeedMineModule() {
        super("Speedmine", "Multiply client-side mining progress; server-dependent.", "World");
        setting("multiplier", 1, 1, 5, 0.25);
        setting("progress", 0.4, 0.1, 1, 0.01);
    }

    @Override
    public void tick(Minecraft mc) {}
}
