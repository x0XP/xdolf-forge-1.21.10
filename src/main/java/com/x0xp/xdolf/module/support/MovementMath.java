package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Shared movement vector calculations used by movement-oriented modules. */
final class MovementMath {
    private MovementMath() {}

    static Vec3 direction(Minecraft mc, double speed) {
        double forward = (mc.options.keyUp.isDown() ? 1 : 0) - (mc.options.keyDown.isDown() ? 1 : 0);
        double strafe = (mc.options.keyLeft.isDown() ? 1 : 0) - (mc.options.keyRight.isDown() ? 1 : 0);
        double length = Math.hypot(forward, strafe);
        if (length == 0) return Vec3.ZERO;
        double yaw = Math.toRadians(mc.player.getYRot());
        return new Vec3((strafe * Math.cos(yaw) - forward * Math.sin(yaw)) * speed / length,
            0, (forward * Math.cos(yaw) + strafe * Math.sin(yaw)) * speed / length);
    }

    static double vertical(Minecraft mc, double speed) {
        return ((mc.options.keyJump.isDown() ? 1 : 0) - (mc.options.keyShift.isDown() ? 1 : 0)) * speed;
    }
}
