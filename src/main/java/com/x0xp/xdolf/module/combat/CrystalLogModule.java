package com.x0xp.xdolf.module.combat;

import com.x0xp.xdolf.settings.NumberSetting;

import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.settings.*;

import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;
import net.minecraft.network.chat.Component;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;

public final class CrystalLogModule extends ClientModule {
    private final NumberSetting range = setting("range", 2, 1, 10, 1);

    public CrystalLogModule() {
        super("CrystalLog", "Disconnect when an end crystal is within the configured range.", "Combat");
    }

    @Override
    public void tick(Minecraft mc) {
        if (!mc.level.getEntitiesOfClass(EndCrystal.class, mc.player.getBoundingBox().inflate(range.get()),
            crystal -> crystal.distanceToSqr(mc.player) <= range.get() * range.get()).isEmpty()) {
            setEnabled(false);
            mc.getConnection().getConnection().disconnect(Component.literal("Xdolf CrystalLog: nearby crystal"));
        }
    }
}
