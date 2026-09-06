package com.darkcart.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Shared client-thread checks for the client-only mixins. */
public final class Hooks {
    private Hooks() {}
    public static net.minecraft.world.phys.Vec3 freecamPosition() { return FreecamModule.position; }
    public static boolean enabled(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !mc.isSameThread() || mc.player == null || mc.level == null) return false;
        var module = ClientRuntime.find(name);
        return module != null && module.enabled();
    }
    public static boolean active(String name) {
        Minecraft mc = Minecraft.getInstance();
        return enabled(name) && (name.equals("Freecam") || !enabled("Freecam")) && mc.screen == null && !mc.isPaused();
    }
    public static double setting(String module, String name, double fallback) {
        var value = ClientRuntime.find(module);
        if (value == null || value.setting(name) == null) return fallback;
        return value.setting(name).get();
    }
    public static boolean espTarget(Entity entity) {
        var mc = Minecraft.getInstance();
        if (entity == mc.player) return false;
        boolean boss=entity.getType()==net.minecraft.world.entity.EntityType.WITHER||entity.getType()==net.minecraft.world.entity.EntityType.ENDER_DRAGON;
        String option = entity instanceof net.minecraft.world.entity.player.Player ? "players"
            : entity instanceof net.minecraft.world.entity.monster.Monster || boss ? "monsters"
            : entity instanceof net.minecraft.world.entity.Mob ? "passive"
            : !(entity instanceof net.minecraft.world.entity.projectile.Projectile) && entity.getType().getCategory()==net.minecraft.world.entity.MobCategory.MISC ? "items" : null;
        return option != null && setting("EntityESP",option,1) != 0;
    }
    public static boolean highlight(Entity entity) {
        return entity instanceof LivingEntity && entity != Minecraft.getInstance().player
            && entity.isAlive() && !entity.isInvisible() && entity.distanceToSqr(Minecraft.getInstance().player) <= 128 * 128;
    }
}
