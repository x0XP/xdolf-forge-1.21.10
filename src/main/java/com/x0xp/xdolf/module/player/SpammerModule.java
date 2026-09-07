package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.settings.*;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;
import java.util.UUID;

public final class SpammerModule extends ClientModule {
    public final TextSetting message = textSetting("message", "Message",
        "The chat message repeated while Spammer is enabled.", "test", 256);
    public final ChoiceSetting mode = choiceSetting("mode", "Mode",
        "Anti-spam adds a changing suffix when servers reject duplicates.", "normal", "normal", "antispam");
    public final NumberSetting delay = numberSetting("delay", "Delay (ms)",
        "Minimum time between messages.", 1800, 1, 120000, 100);
    private long lastMessage;

    public SpammerModule() {
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
        if (ChatQueue.pending(this)) return;
        lastMessage = now;
        String suffix = mode.get().equals("antispam")
            ? " [" + UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "]" : "";
        String value = message.get();
        ChatQueue.offer(this, value.substring(0, Math.min(value.length(), 256 - suffix.length())) + suffix, Math.round(delay.get()));
    }

    public void reset(Minecraft mc) { lastMessage = 0; ChatQueue.cancel(this); }
}
