package com.x0xp.xdolf.module.render;

import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by entity render hooks. */
public final class ChamsModule extends ClientModule {
    public ChamsModule() {
        super("Chams", "Render textured living-entity models through walls.", "Render");
    }

    @Override
    public void tick(Minecraft mc) {}
}
