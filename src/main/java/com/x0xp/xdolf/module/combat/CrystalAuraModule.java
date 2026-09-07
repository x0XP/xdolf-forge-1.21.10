package com.x0xp.xdolf.module.combat;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import java.util.Comparator;

public final class CrystalAuraModule extends ClientModule {
    private final NumberSetting range = setting("range", 3.75, 3, 10, 0.25);
    private final NumberSetting rate = setting("speed", 8, 1, 20, 1);
    private long lastAttack;

    public CrystalAuraModule() {
        super("CrystalAura", "Break the nearest visible end crystal in range.", "Combat");
    }

    @Override
    public void tick(Minecraft mc) {
        long now = System.nanoTime();
        if (now - lastAttack < 1_000_000_000L / rate.get()) return;
        if (mc.gameMode == null || mc.player.isUsingItem()) return;
        var target = mc.level.getEntitiesOfClass(EndCrystal.class, mc.player.getBoundingBox().inflate(range.get()), entity ->
            entity.distanceToSqr(mc.player) <= range.get() * range.get() && mc.player.hasLineOfSight(entity))
            .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(mc.player))).orElse(null);
        if (target != null) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
            lastAttack = now;
        }
    }

    @Override
    public void reset(Minecraft mc) {
        lastAttack = 0;
    }
}
