package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

/** Movement input is supplied by ClientRuntime's input hook. */
final class AutoWalkModule extends ClientModule {
    AutoWalkModule() {
        super("AutoWalk", "Hold forward until disabled or a screen opens.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {}
}
