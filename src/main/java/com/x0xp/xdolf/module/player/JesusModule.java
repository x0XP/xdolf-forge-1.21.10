package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

final class JesusModule extends ClientModule {
    JesusModule() {
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
