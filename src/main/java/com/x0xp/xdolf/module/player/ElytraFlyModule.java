package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.support.MovementMath;
import com.x0xp.xdolf.settings.NumberSetting;

import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.settings.*;

import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

public final class ElytraFlyModule extends ClientModule {
    private final NumberSetting speed = setting("speed", 1.41, 0.1, 1.45, 0.01);

    public ElytraFlyModule() {
        super("ElytraFly", "Control horizontal and vertical speed while gliding.", "Player");
        conflictsWith("Flight", "ElytraPlus");
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc.player.isFallFlying())
            mc.player.setDeltaMovement(MovementMath.direction(mc, speed.get()).add(0, MovementMath.vertical(mc, speed.get()), 0));
    }
}
