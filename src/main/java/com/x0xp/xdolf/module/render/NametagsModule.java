package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

final class NametagsModule extends ClientModule {
    NametagsModule() {
        super("Nametags", "Original player names and health percentages.", "Render");
    }

    @Override
    public void tick(Minecraft mc) {}
}
