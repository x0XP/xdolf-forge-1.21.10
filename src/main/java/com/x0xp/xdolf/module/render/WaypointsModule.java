package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

final class WaypointsModule extends ClientModule {
    WaypointsModule() {
        super("Waypoints", "Toggle rendering for saved .waypoint locations.", "Render");
    }

    @Override
    public void tick(Minecraft mc) {}
}
