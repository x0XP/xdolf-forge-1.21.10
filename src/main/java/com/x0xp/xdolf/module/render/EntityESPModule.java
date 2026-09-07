package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

final class EntityESPModule extends ClientModule {
    EntityESPModule() {
        super("EntityESP", "Show selected entities with outlines or boxes.", "Render");
        toggle("players", true);
        toggle("monsters", true);
        toggle("passive", true);
        toggle("items", true);
        toggle("outline", true);
    }

    @Override
    public void tick(Minecraft mc) {}
}
