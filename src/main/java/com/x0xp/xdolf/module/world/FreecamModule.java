package com.x0xp.xdolf.module.world;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;
import net.minecraft.world.phys.Vec3;

/** Camera-only noclip movement. The server-side player remains at its real position. */
public final class FreecamModule extends ClientModule {
    private static Vec3 position;
    private static long lastFrameNanos;
    final NumberSetting speed = setting("speed", 0.5, 0.05, 3, 0.05);

    public FreecamModule() { super("Freecam", "Move the camera independently; your player remains in the world.", "World"); }

    /**
     * Freecam motion is integrated from render-frame time instead of advancing only at 20 client
     * ticks per second. This keeps camera travel continuous at high refresh rates and avoids the
     * characteristic tick-to-tick teleport/stutter of a tick-driven camera.
     */
    public static Vec3 cameraPosition(float partialTick) {
        if (position == null) return null;

        Minecraft mc = Minecraft.getInstance();
        long now = System.nanoTime();
        if (lastFrameNanos == 0L) {
            lastFrameNanos = now;
            return position;
        }

        double elapsed = Math.max(0.0, (now - lastFrameNanos) / 1_000_000_000.0);
        lastFrameNanos = now;

        // Do not compensate for a long frozen/stalled frame with one giant camera jump.
        double seconds = Math.min(elapsed, 0.05);
        if (seconds <= 0.0 || mc.player == null || mc.isPaused()) return position;

        // Existing speed values were blocks per client tick. Preserve that feel at 20 ticks/sec.
        double distance = Hooks.setting("Freecam", "speed", 0.5) * 20.0 * seconds;
        position = position.add(MovementMath.direction(mc, distance))
            .add(0, MovementMath.vertical(mc, distance), 0);
        return position;
    }

    private static void start(Minecraft mc) {
        if (position != null || mc.player == null) return;
        position = mc.player.getEyePosition();
        lastFrameNanos = System.nanoTime();
    }

    @Override
    public void activate(Minecraft mc) { start(mc); }

    @Override
    public void tick(Minecraft mc) { start(mc); }

    @Override
    public void reset(Minecraft mc) {
        position = null;
        lastFrameNanos = 0L;
    }
}
