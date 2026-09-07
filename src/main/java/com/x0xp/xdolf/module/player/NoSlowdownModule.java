package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the client movement/block hooks. */
public final class NoSlowdownModule extends ClientModule {
    public NoSlowdownModule() {
        super("NoSlowdown", "Reduce ice slipperiness on the client, matching the original module.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {}
}
