package com.x0xp.xdolf.module.render;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

public final class NametagsModule extends ClientModule {
    public NametagsModule() {
        super("Nametags", "Original player names and health percentages.", "Render");
    }

    @Override
    public void tick(Minecraft mc) {}
}
