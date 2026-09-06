package com.darkcart.xdolf;

import net.minecraft.client.Minecraft;
import java.util.List;

final class NetworkModules {
    static String spamMessage = "test";

    private static ClientModule hook(String name, String description, String category) {
        return new ClientModule(name, description, category) { public void tick(Minecraft mc) {} };
    }

    static void addTo(List<ClientModule> modules) {
        modules.add(hook("AntiVelocity", "Ignore player velocity and explosion knockback packets.", "Combat"));
        modules.add(hook("AntiHunger", "Suppress sprint notifications and grounded movement flags; server-dependent.", "Player"));
        modules.add(hook("Criticals", "Send a short airborne packet sequence before melee attacks.", "Combat"));
        modules.add(new ClientModule("Timer", "Scale client tick speed; servers may correct this.", "World") {
            { setting("speed", 1.2, 0.1, 5, 0.1); }
            public void tick(Minecraft mc) {}
        });
        modules.add(new ClientModule("Spammer", "Repeat the message set with .spam while enabled.", "Player") {
            long lastMessage;
            public void tick(Minecraft mc) {
                if (spamMessage.isBlank()) {
                    setEnabled(false);
                    ClientRuntime.message("Set a message first with .spam <message>.");
                    return;
                }
                long now = System.nanoTime();
                if (lastMessage == 0) lastMessage = now;
                if (now - lastMessage >= Commands.spamDelay * 1_000_000L) {
                    lastMessage = now;
                    String suffix = Commands.spamMode.equals("antispam")
                        ? " [" + java.util.UUID.randomUUID().toString().replace("-", "").substring(0, 16) + "]"
                        : "";
                    String message = spamMessage.substring(0, Math.min(spamMessage.length(), 256 - suffix.length())) + suffix;
                    mc.player.connection.sendChat(message);
                }
            }
            public void reset(Minecraft mc) { lastMessage = 0; }
        });
        modules.add(new ClientModule("Jesus", "Walk on fluid surfaces; sneak to descend.", "Player") {
            public void tick(Minecraft mc) {
                if ((mc.player.isInWater() || mc.player.isInLava()) && !mc.options.keyShift.isDown() && !mc.player.isPassenger()) {
                    var motion = mc.player.getDeltaMovement();
                    mc.player.setDeltaMovement(motion.x, 0.1, motion.z);
                }
            }
        });
    }
}
