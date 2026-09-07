package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

/** Movement input is supplied by ClientRuntime's input hook. */
public final class AutoWalkModule extends ClientModule {
    public AutoWalkModule() {
        super("AutoWalk", "Hold forward until disabled or a screen opens.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {}
}
