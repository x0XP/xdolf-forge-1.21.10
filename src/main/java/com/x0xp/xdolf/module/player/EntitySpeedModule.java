package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.support.MovementMath;
import com.x0xp.xdolf.settings.NumberSetting;

import com.x0xp.xdolf.module.ClientModule;


import net.minecraft.client.Minecraft;

public final class EntitySpeedModule extends ClientModule {
    private final NumberSetting speed = setting("speed", 3, 0.1, 3.86, 0.01);

    public EntitySpeedModule() {
        super("EntitySpeed", "Change the speed of the vehicle you control.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {
        var vehicle = mc.player.getVehicle();
        if (vehicle != null && vehicle.getControllingPassenger() == mc.player) {
            var move = MovementMath.direction(mc, speed.get());
            vehicle.setDeltaMovement(move.x, vehicle.getDeltaMovement().y, move.z);
        }
    }
}
