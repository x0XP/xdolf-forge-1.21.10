package com.darkcart.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

final class FreecamModule extends ClientModule {
    static Vec3 position;
    final ModuleSetting speed = setting("speed", 0.5, 0.05, 3, 0.05);
    FreecamModule() { super("Freecam", "Move the camera independently; your player remains in the world.", "World"); }
    public void tick(Minecraft mc) {
        if (position == null) position = mc.player.getEyePosition();
        position = position.add(MovementModules.direction(mc, speed.get())).add(0, MovementModules.vertical(mc, speed.get()), 0);
    }
    public void reset(Minecraft mc) { position = null; }
}
