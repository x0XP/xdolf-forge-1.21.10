package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

/** Behaviour is supplied by the mount-jump input hook. */
final class HorseJumpModule extends ClientModule {
    HorseJumpModule() {
        super("HorseJump", "Send maximum charge when releasing a mount jump.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {}
}
