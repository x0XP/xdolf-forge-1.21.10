package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

public final class FlightModule extends ClientModule {
    private final NumberSetting speed = setting("speed", 1, 0.1, 10, 0.05);

    public FlightModule() {
        super("Flight", "Controlled flight; servers may reject this movement.", "Player");
        conflictsWith("ElytraFly", "ElytraPlus");
    }

    @Override
    public void tick(Minecraft mc) {
        if (!mc.player.isPassenger())
            mc.player.setDeltaMovement(MovementMath.direction(mc, speed.get()).add(0, MovementMath.vertical(mc, speed.get()), 0));
    }
}
