package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

final class FreecamModule extends ClientModule {
    private static Vec3 previousPosition;
    private static Vec3 position;
    final ModuleSetting speed = setting("speed", 0.5, 0.05, 3, 0.05);

    FreecamModule() { super("Freecam", "Move the camera independently; your player remains in the world.", "World"); }

    static Vec3 cameraPosition(float partialTick) {
        if (position == null) return null;
        if (previousPosition == null) return position;
        double t = Math.max(0.0, Math.min(1.0, partialTick));
        return previousPosition.lerp(position, t);
    }

    public void tick(Minecraft mc) {
        if (position == null) {
            position = mc.player.getEyePosition();
            previousPosition = position;
        }
        previousPosition = position;
        position = position.add(MovementModules.direction(mc, speed.get()))
            .add(0, MovementModules.vertical(mc, speed.get()), 0);
    }

    public void reset(Minecraft mc) {
        position = null;
        previousPosition = null;
    }
}
