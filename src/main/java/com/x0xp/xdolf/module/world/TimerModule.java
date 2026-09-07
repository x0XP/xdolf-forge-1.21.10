package com.x0xp.xdolf.module.world;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

public final class TimerModule extends ClientModule {
    public TimerModule() {
        super("Timer", "Scale client tick speed; servers may correct this.", "World");
        setting("speed", 1.2, 0.1, 5, 0.1);
    }

    @Override
    public void tick(Minecraft mc) {}
}
