package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.ClientModule;


import net.minecraft.client.Minecraft;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

public final class NoFallModule extends ClientModule {
    private int delay;

    public NoFallModule() {
        super("NoFall", "Send grounded status while falling; server-dependent.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {
        if (delay > 0) delay--;
        if (delay == 0 && mc.player.fallDistance > 2 && !mc.player.isFallFlying() && !mc.player.isPassenger()) {
            mc.player.connection.send(new ServerboundMovePlayerPacket.StatusOnly(true, mc.player.horizontalCollision));
            delay = 5;
        }
    }

    @Override
    public void reset(Minecraft mc) {
        delay = 0;
    }
}
