package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

final class TimerModule extends ClientModule {
    TimerModule() {
        super("Timer", "Scale client tick speed; servers may correct this.", "World");
        setting("speed", 1.2, 0.1, 5, 0.1);
    }

    @Override
    public void tick(Minecraft mc) {}
}
