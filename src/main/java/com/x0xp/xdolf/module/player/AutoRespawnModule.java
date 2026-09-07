package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.ClientModule;


import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class AutoRespawnModule extends ClientModule {
    private LocalPlayer lastDeath;

    public AutoRespawnModule() {
        super("AutoRespawn", "Request respawn once after each death.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc.player.isDeadOrDying()) {
            if (lastDeath != mc.player) {
                lastDeath = mc.player;
                mc.player.respawn();
            }
        } else lastDeath = null;
    }

    @Override
    public void reset(Minecraft mc) {
        lastDeath = null;
    }
}
