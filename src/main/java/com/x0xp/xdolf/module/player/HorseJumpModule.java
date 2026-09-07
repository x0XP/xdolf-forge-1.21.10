package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.ClientModule;


import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the mount-jump input hook. */
public final class HorseJumpModule extends ClientModule {
    public HorseJumpModule() {
        super("HorseJump", "Send maximum charge when releasing a mount jump.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {}
}
