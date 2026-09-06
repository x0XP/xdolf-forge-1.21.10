package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import java.util.List;

final class NetworkModules {
    private static ClientModule hook(String name, String description, String category) {
        return new ClientModule(name, description, category) { public void tick(Minecraft mc) {} };
    }

    static SpammerModule spammer() {
        return (SpammerModule) ClientRuntime.find("Spammer");
    }

    static void addTo(List<ClientModule> modules) {
        modules.add(hook("AntiVelocity", "Ignore player velocity and explosion knockback packets.", "Combat"));
        modules.add(hook("AntiHunger", "Suppress sprint notifications and grounded movement flags; server-dependent.", "Player"));
        modules.add(hook("Criticals", "Send a short airborne packet sequence before melee attacks.", "Combat"));
        modules.add(new ClientModule("Timer", "Scale client tick speed; servers may correct this.", "World") {
            { setting("speed", 1.2, 0.1, 5, 0.1); }
            public void tick(Minecraft mc) {}
        });
        modules.add(new SpammerModule());
        modules.add(new AnnouncerModule());
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
