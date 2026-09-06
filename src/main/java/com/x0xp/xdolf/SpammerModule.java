package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import java.util.UUID;

final class SpammerModule extends ClientModule {
    final TextSetting message = textSetting("message", "Message",
        "The chat message repeated while Spammer is enabled.", "test", 256);
    final ChoiceSetting mode = choiceSetting("mode", "Mode",
        "Anti-spam adds a changing suffix when servers reject duplicates.", "normal", "normal", "antispam");
    final NumberSetting delay = numberSetting("delay", "Delay (ms)",
        "Minimum time between messages.", 1800, 1, 120000, 100);
    private long lastMessage;

    SpammerModule() {
        super("Spammer", "Repeat a configurable message at a controlled interval.", "Player");
    }

    public void tick(Minecraft mc) {
        if (message.get().isBlank()) {
            setEnabled(false);
            ClientRuntime.message("Set a message first in Spammer options or with .spam <message>.");
            return;
        }
        long now = System.nanoTime();
        if (lastMessage == 0) lastMessage = now;
        if (now - lastMessage < Math.round(delay.get()) * 1_000_000L) return;
        lastMessage = now;
        String suffix = mode.get().equals("antispam")
            ? " [" + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "]" : "";
        String value = message.get();
        mc.player.connection.sendChat(value.substring(0, Math.min(value.length(), 256 - suffix.length())) + suffix);
    }

    public void reset(Minecraft mc) { lastMessage = 0; }
}
