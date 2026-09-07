package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;

final class TrajectoriesModule extends ClientModule {
    TrajectoriesModule() {
        super("Trajectories", "Original projectile line and landing box.", "Render");
    }

    @Override
    public void tick(Minecraft mc) {}
}
