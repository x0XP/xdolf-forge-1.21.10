package com.x0xp.xdolf.module.combat;

import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.settings.*;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.player.Player;
import java.util.Comparator;

public final class KillAuraModule extends ClientModule {
    private final NumberSetting range = setting("range", 3.75, 3, 10, 0.25);
    private final BooleanSetting players = toggle("players", true);
    private final BooleanSetting monsters = toggle("monsters", true);
    private final BooleanSetting mobs = toggle("mobs", true);
    private final BooleanSetting walls = booleanSetting("walls", "Hit Through Walls", "", true);
    private final BooleanSetting seen = booleanSetting("seen", "Can Be Seen", "", false);

    public KillAuraModule() {
        super("KillAura", "Attack the nearest visible selected target at full cooldown.", "Combat");
    }

    @Override
    public void tick(Minecraft mc) {
        if (mc.gameMode == null || mc.player.isUsingItem() || mc.player.getAttackStrengthScale(0) < 1) return;
        var target = mc.level.getEntitiesOfClass(LivingEntity.class, mc.player.getBoundingBox().inflate(range.get()), entity ->
            entity != mc.player && entity.isAlive() && !entity.isSpectator()
                && entity.distanceToSqr(mc.player) <= range.get() * range.get()
                && (walls.on() || mc.player.hasLineOfSight(entity)) && !mc.player.isAlliedTo(entity)
                && (!seen.on() || Math.abs(net.minecraft.util.Mth.wrapDegrees((float)Math.toDegrees(
                    Math.atan2(entity.getZ()-mc.player.getZ(), entity.getX()-mc.player.getX()))-90-mc.player.getYRot())) <= 60)
                && !SocialState.isFriend(entity.getName().getString())
                && ((players.on() && entity instanceof Player)
                    || (mobs.on() && !(entity instanceof Player) && (!(entity instanceof Monster) || monsters.on()))))
            .stream().min(Comparator.comparingDouble(entity -> entity.distanceToSqr(mc.player))).orElse(null);
        if (target != null) {
            mc.gameMode.attack(mc.player, target);
            mc.player.swing(InteractionHand.MAIN_HAND);
        }
    }
}
