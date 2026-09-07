package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Batches ordinary player activity into one rate-limited public chat message. */
final class AnnouncerModule extends ClientModule {
    final NumberSetting delay = numberSetting("delay", "Delay (ms)",
        "Minimum time between announcement messages.", 1800, 250, 120000, 100);
    final BooleanSetting walking = booleanSetting("walking", "Distance walked",
        "Announce horizontal distance travelled on foot.", true);
    final NumberSetting minimumDistance = numberSetting("minimum_distance", "Min. distance",
        "Keep accumulating until at least this many metres have been travelled.", 5, 1, 100, 1);
    final BooleanSetting breaking = booleanSetting("breaking", "Blocks broken",
        "Count mined blocks by type and announce them as one batch.", true);
    final BooleanSetting eating = booleanSetting("eating", "Food eaten",
        "Count completed food uses by item and announce them as one batch.", true);
    final BooleanSetting jumping = booleanSetting("jumping", "Jumps",
        "Count jumps and announce the total rather than every jump.", false);
    final BooleanSetting attacking = booleanSetting("attacking", "Attacks",
        "Count attacks made against entities.", false);

    private final Map<String, Integer> broken = new LinkedHashMap<>();
    private final Map<String, Integer> eaten = new LinkedHashMap<>();
    private LocalPlayer owner;
    private Vec3 previousPosition;
    private boolean previousGrounded;
    private double distance;
    private int jumps;
    private int attacks;
    private long intervalStarted;

    AnnouncerModule() {
        super("Announcer", "Share batched walking, mining, eating, jumping and combat activity in chat.", "Player");
    }

    public void tick(Minecraft mc) {
        LocalPlayer player = mc.player;
        if (owner != player) initialise(player);

        Vec3 current = player.position();
        if (previousPosition != null && !player.isPassenger() && !player.isFallFlying()) {
            double moved = Math.hypot(current.x - previousPosition.x, current.z - previousPosition.z);
            if (moved <= 8.0 && walking.on()) distance += moved;
        }
        if (jumping.on() && previousGrounded && !player.onGround() && current.y > previousPosition.y + 0.05) jumps++;
        previousPosition = current;
        previousGrounded = player.onGround();

        long now = System.nanoTime();
        if (intervalStarted == 0) intervalStarted = now;
        if (now - intervalStarted < Math.round(delay.get()) * 1_000_000L) return;
        if (ChatQueue.pending(this)) return;
        intervalStarted = now;

        List<String> parts = new ArrayList<>();
        long metres = (long) Math.floor(distance);
        if (walking.on() && metres >= Math.round(minimumDistance.get())) {
            parts.add("walked " + metres + (metres == 1 ? " metre" : " metres"));
            distance -= metres;
        }
        if (breaking.on() && !broken.isEmpty()) {
            parts.add("mined " + summarise(broken, "block", "blocks"));
            broken.clear();
        }
        if (eating.on() && !eaten.isEmpty()) {
            parts.add("ate " + summarise(eaten, "item", "items"));
            eaten.clear();
        }
        if (jumping.on() && jumps > 0) {
            parts.add("jumped " + jumps + (jumps == 1 ? " time" : " times"));
            jumps = 0;
        }
        if (attacking.on() && attacks > 0) {
            parts.add("attacked " + attacks + (attacks == 1 ? " entity" : " entities"));
            attacks = 0;
        }
        if (parts.isEmpty()) return;

        String message = "I just " + join(parts) + ".";
        ChatQueue.offer(this, message.substring(0, Math.min(message.length(), 256)), Math.round(delay.get()));
    }

    void blockBroken(String name) {
        if (enabled() && breaking.on()) broken.merge(cleanName(name), 1, Integer::sum);
    }

    void foodEaten(String name) {
        if (enabled() && eating.on()) eaten.merge(cleanName(name), 1, Integer::sum);
    }

    void entityAttacked() {
        if (enabled() && attacking.on()) attacks++;
    }

    private void initialise(LocalPlayer player) {
        owner = player;
        previousPosition = player.position();
        previousGrounded = player.onGround();
        intervalStarted = System.nanoTime();
    }

    public void reset(Minecraft mc) {
        ChatQueue.cancel(this);
        owner = null;
        previousPosition = null;
        previousGrounded = false;
        distance = 0;
        jumps = attacks = 0;
        broken.clear();
        eaten.clear();
        intervalStarted = 0;
    }

    private static String cleanName(String name) {
        return name == null || name.isBlank() ? "item" : name.toLowerCase(Locale.ROOT);
    }

    private static String summarise(Map<String, Integer> values, String singular, String plural) {
        int total = values.values().stream().mapToInt(Integer::intValue).sum();
        if (values.size() == 1) {
            var entry = values.entrySet().iterator().next();
            return entry.getValue() + " " + entry.getKey();
        }
        var details = values.entrySet().stream().limit(3)
            .map(entry -> entry.getValue() + " " + entry.getKey()).toList();
        int shown = values.entrySet().stream().limit(3).mapToInt(Map.Entry::getValue).sum();
        String result = String.join(", ", details);
        if (shown < total) result += ", and " + (total - shown) + " other " + ((total - shown) == 1 ? singular : plural);
        return result;
    }

    private static String join(List<String> parts) {
        if (parts.size() == 1) return parts.get(0);
        if (parts.size() == 2) return parts.get(0) + " and " + parts.get(1);
        return String.join(", ", parts.subList(0, parts.size() - 1)) + ", and " + parts.get(parts.size() - 1);
    }
}
