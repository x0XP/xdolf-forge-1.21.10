package com.x0xp.xdolf.module.render;

import com.x0xp.xdolf.module.ClientModule;


import net.minecraft.client.Minecraft;

public final class EntityESPModule extends ClientModule {
    public EntityESPModule() {
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
