package com.x0xp.xdolf.chat;


import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.module.ModuleManager;

import net.minecraft.client.Minecraft;
import java.util.LinkedHashMap;

/** One pending message per producer; automated messages share a cooldown. */
public final class ChatQueue {
    private record Pending(String message, long gap) {}
    private static final LinkedHashMap<ClientModule, Pending> PENDING = new LinkedHashMap<>();
    private static long lastSent;
    private static long lastGap;

    public static boolean pending(ClientModule source) { return PENDING.containsKey(source); }
    public static void cancel(ClientModule source) { PENDING.remove(source); }
    public static void clear() { PENDING.clear(); lastSent = lastGap = 0; }

    public static void offer(ClientModule source, String message, long delayMs) {
        PENDING.putIfAbsent(source, new Pending(message, Math.max(1, delayMs) * 1_000_000L));
    }

    public static void tick(Minecraft mc) {
        if (mc.player == null || mc.level == null || mc.getConnection() == null) { clear(); return; }
        PENDING.keySet().removeIf(source -> !source.enabled());
        if (PENDING.isEmpty()) return;
        var entry = PENDING.entrySet().iterator().next();
        if (ModuleManager.status(entry.getKey(), mc).activity() != ModuleManager.Activity.ACTIVE) return;
        long now = System.nanoTime();
        if (lastSent != 0 && now - lastSent < Math.max(lastGap, entry.getValue().gap())) return;
        String message = entry.getValue().message();
        mc.player.connection.sendChat(message);
        lastSent = now;
        lastGap = entry.getValue().gap();
        PENDING.remove(entry.getKey());
    }
}
