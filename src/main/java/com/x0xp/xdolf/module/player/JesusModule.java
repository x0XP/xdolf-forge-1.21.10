package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.ClientModule;


import net.minecraft.client.Minecraft;

public final class JesusModule extends ClientModule {
    public JesusModule() {
        super("Jesus", "Walk on fluid surfaces; sneak to descend.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {
        if ((mc.player.isInWater() || mc.player.isInLava()) && !mc.options.keyShift.isDown() && !mc.player.isPassenger()) {
            var motion = mc.player.getDeltaMovement();
            mc.player.setDeltaMovement(motion.x, 0.1, motion.z);
        }
    }
}
