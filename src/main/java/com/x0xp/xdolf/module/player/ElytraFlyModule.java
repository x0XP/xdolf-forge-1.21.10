package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

final class ElytraFlyModule extends ClientModule {
    private final NumberSetting speed = setting("speed", 1.41, 0.1, 1.45, 0.01);

    ElytraFlyModule() {
        super("ElytraFly", "Control horizontal and vertical speed while gliding.", "Player");
        conflictsWith("Flight", "ElytraPlus");
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc.player.isFallFlying())
            mc.player.setDeltaMovement(MovementMath.direction(mc, speed.get()).add(0, MovementMath.vertical(mc, speed.get()), 0));
    }
}
