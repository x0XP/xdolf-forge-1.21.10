package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import java.util.List;

/** Module declarations only. Rendering is performed in the world frame passes. */
final class RenderOverlays {
    static void addTo(List<ClientModule> modules) {
        modules.add(new ClientModule("Tracers","Original distance-coloured player and chest tracers.","Render") {
            {toggle("players",true);toggle("chests",false);}
            public void tick(Minecraft mc) {}
        });
        modules.add(new ClientModule("Nametags","Original player names and health percentages.","Render") {public void tick(Minecraft mc) {}});
        modules.add(new ClientModule("StorageESP","Original filled storage boxes and type colours.","Render") {public void tick(Minecraft mc) {}});
        modules.add(new ClientModule("Trajectories","Original projectile line and landing box.","Render") {public void tick(Minecraft mc) {}});
        modules.add(new ClientModule("Waypoints","Toggle rendering for saved .waypoint locations.","Render") {public void tick(Minecraft mc) {}});
        modules.add(new LogoutSpotModule());
    }
}
