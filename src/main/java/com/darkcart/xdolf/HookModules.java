package com.darkcart.xdolf;

import net.minecraft.client.Minecraft;
import java.util.List;

final class HookModules {
    private static ClientModule hook(String name, String description, String category) {
        // Behavior lives in the registered mixins, not in per-tick mutation.
        return new ClientModule(name, description, category) { public void tick(Minecraft mc) {} };
    }
    static void addTo(List<ClientModule> modules) {
        modules.add(hook("Fullbright", "Client-side night vision without changing real potion effects.", "World"));
        modules.add(hook("NoHurtCam", "Disable the camera shake caused by damage.", "Render"));
        modules.add(new ClientModule("EntityESP", "Show selected entities with outlines or boxes.", "Render") {
            { setting("players",1,0,1,1); setting("monsters",1,0,1,1); setting("passive",1,0,1,1); setting("items",1,0,1,1); setting("outline",1,0,1,1); }
            public void tick(Minecraft mc) {}
        });
        modules.add(hook("Chams", "Render textured living-entity models through walls.", "Render"));
        modules.add(hook("HorseJump", "Send maximum charge when releasing a mount jump.", "Player"));
        modules.add(hook("SafeWalk", "Apply edge protection while walking on the ground.", "Player"));
        modules.add(hook("NoSlowdown", "Reduce ice slipperiness on the client, matching the original module.", "Player"));
        modules.add(hook("FastPlace", "Remove the client-side right-click delay.", "World"));
        modules.add(new ClientModule("Speedmine", "Multiply client-side mining progress; server-dependent.", "World") {
            { setting("multiplier", 1, 1, 5, 0.25); setting("progress",0.4,0.1,1,0.01); }
            public void tick(Minecraft mc) {}
        });
    }
}
