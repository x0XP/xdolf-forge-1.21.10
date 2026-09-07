package com.x0xp.xdolf.module.render;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

public final class WaypointsModule extends ClientModule {
    public WaypointsModule() {
        super("Waypoints", "Toggle rendering for saved .waypoint locations.", "Render");
    }

    @Override
    public void tick(Minecraft mc) {}
}
