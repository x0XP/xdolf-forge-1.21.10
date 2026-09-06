package com.darkcart.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.boss.enderdragon.EndCrystal;
import java.util.Comparator;
import java.util.List;

final class CombatModules {
    static void addTo(List<ClientModule> modules) {
        modules.add(new ClientModule("KillAura", "Attack the nearest visible selected target at full cooldown.", "Combat") {
            final ModuleSetting range = setting("range", 3.75, 3, 10, 0.25);
            final ModuleSetting players = setting("players", 1, 0, 1, 1);
            final ModuleSetting monsters = setting("monsters", 1, 0, 1, 1);
            final ModuleSetting mobs = setting("mobs", 1, 0, 1, 1);
            final ModuleSetting walls = setting("walls", 1, 0, 1, 1);
            final ModuleSetting seen = setting("seen", 0, 0, 1, 1);
            public void tick(Minecraft mc) {
                if (mc.gameMode == null || mc.player.isUsingItem() || mc.player.getAttackStrengthScale(0) < 1) return;
                var target = mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(range.get()), entity ->
                    entity != mc.player && entity.isAlive() && !entity.isSpectator()
                    && entity.distanceToSqr(mc.player) <= range.get() * range.get()
                    && (walls.on() || mc.player.hasLineOfSight(entity)) && !mc.player.isAlliedTo(entity)
                    && (!seen.on() || Math.abs(net.minecraft.util.Mth.wrapDegrees((float)Math.toDegrees(Math.atan2(entity.getZ()-mc.player.getZ(), entity.getX()-mc.player.getX()))-90-mc.player.getYRot())) <= 60)
                    && !SocialState.isFriend(entity.getName().getString())
                    && ((players.on() && entity instanceof Player) || (mobs.on() && !(entity instanceof Player) && (!(entity instanceof Monster) || monsters.on()))))
                    .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(mc.player))).orElse(null);
                if (target != null) {
                    mc.gameMode.attack(mc.player, target);
                    mc.player.swing(InteractionHand.MAIN_HAND);
                }
            }
        });
        modules.add(new ClientModule("CrystalAura", "Break the nearest visible end crystal in range.", "Combat") {
            final ModuleSetting range = setting("range", 3.75, 3, 10, 0.25);
            final ModuleSetting rate = setting("speed", 8, 1, 20, 1);
            long lastAttack;
            public void tick(Minecraft mc) {
                long now = System.nanoTime();
                if (now-lastAttack < 1_000_000_000L/rate.get()) return;
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
            public void reset(Minecraft mc) { lastAttack = 0; }
        });
    }
}
