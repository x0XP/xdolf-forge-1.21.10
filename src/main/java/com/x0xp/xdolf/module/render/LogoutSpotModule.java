package com.x0xp.xdolf.module.render;

import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.phys.Vec3;

import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Tracks the last world position of players that genuinely leave the tab list.
 *
 * <p>Players merely leaving entity render distance remain in the connection's player
 * list, so they are not incorrectly recorded as logout spots. The final client-side
 * Player object is retained while a spot exists; this preserves the skin, equipment,
 * pose and rotations needed to draw the logout model exactly where it disappeared.</p>
 */
public final class LogoutSpotModule extends ClientModule {
    public record Spot(UUID id, String name, String dimension, Vec3 position, Player ghost) {}

    private static final Map<UUID, Spot> LOGOUT_SPOTS = new LinkedHashMap<>();
    private final Map<UUID, Spot> lastSeen = new HashMap<>();
    private String dimension;

    public LogoutSpotModule() {
        super("LogoutSpot", "Render logout positions, retained player models and optional tracers.", "Render");
        toggle("tracers", true);
    }

    public static java.util.Collection<Spot> spots() {
        return java.util.List.copyOf(LOGOUT_SPOTS.values());
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) return;

        String currentDimension = mc.level.dimension().location().toString();
        if (!currentDimension.equals(dimension)) {
            lastSeen.clear();
            LOGOUT_SPOTS.clear();
            dimension = currentDimension;
        }

        // A player that is back in the network player list is no longer logged out,
        // even if they have not yet re-entered our entity render distance.
        LOGOUT_SPOTS.entrySet().removeIf(entry -> mc.getConnection().getPlayerInfo(entry.getKey()) != null);

        var visible = new java.util.HashSet<UUID>();
        for (Player player : mc.level.players()) {
            if (player == mc.player) continue;
            UUID id = player.getUUID();
            visible.add(id);
            lastSeen.put(id, new Spot(id, player.getName().getString(), currentDimension, player.position(), player));
        }

        var iterator = lastSeen.entrySet().iterator();
        while (iterator.hasNext()) {
            var entry = iterator.next();
            if (visible.contains(entry.getKey())) continue;

            // If the player is still in PlayerInfo, the entity simply unloaded or moved
            // outside render distance. Only a missing PlayerInfo is treated as logout.
            if (mc.getConnection().getPlayerInfo(entry.getKey()) == null) {
                LOGOUT_SPOTS.put(entry.getKey(), entry.getValue());
                iterator.remove();
            }
        }
    }

    @Override
    public void reset(Minecraft mc) {
        lastSeen.clear();
        LOGOUT_SPOTS.clear();
        dimension = null;
    }
}
