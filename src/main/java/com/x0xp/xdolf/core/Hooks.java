package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;

/** Shared client-thread checks for the client-only mixins. */
public final class Hooks {
    private Hooks() {}

    public static net.minecraft.world.phys.Vec3 freecamPosition(float partialTick) {
        return FreecamModule.cameraPosition(partialTick);
    }

    public static boolean enabled(String name) {
        Minecraft mc = Minecraft.getInstance();
        if (mc == null || !mc.isSameThread() || mc.player == null || mc.level == null) return false;
        var module = ClientRuntime.find(name);
        return module != null && module.enabled();
    }

    /**
     * A module remains active while inventory, chat and other client screens are open. Freecam still
     * owns player movement while selected, so other movement hooks are suspended until it is disabled.
     * Tick-driven gameplay modules independently avoid advancing during a true paused game.
     */
    public static boolean active(String name) {
        var module = ClientRuntime.find(name);
        return module != null && ModuleManager.active(module, Minecraft.getInstance());
    }

    public static double setting(String module, String name, double fallback) {
        var value = ClientRuntime.find(module);
        if (value == null) return fallback;
        var setting = value.setting(name);
        if (setting instanceof NumberSetting number) return number.get();
        if (setting instanceof BooleanSetting toggle) return toggle.on() ? 1 : 0;
        return fallback;
    }

    public static void announcerBlockBroken(String name) {
        var module = ClientRuntime.find("Announcer");
        if (active("Announcer") && module instanceof AnnouncerModule announcer) announcer.blockBroken(name);
    }

    public static void announcerFoodEaten(String name) {
        var module = ClientRuntime.find("Announcer");
        if (active("Announcer") && module instanceof AnnouncerModule announcer) announcer.foodEaten(name);
    }

    public static void announcerEntityAttacked() {
        var module = ClientRuntime.find("Announcer");
        if (active("Announcer") && module instanceof AnnouncerModule announcer) announcer.entityAttacked();
    }

    public static boolean espTarget(Entity entity) {
        var mc = Minecraft.getInstance();
        if (entity == mc.player) return false;
        boolean boss = entity.getType() == net.minecraft.world.entity.EntityType.WITHER
            || entity.getType() == net.minecraft.world.entity.EntityType.ENDER_DRAGON;
        String option = entity instanceof net.minecraft.world.entity.player.Player ? "players"
            : entity instanceof net.minecraft.world.entity.monster.Monster || boss ? "monsters"
            : entity instanceof net.minecraft.world.entity.Mob ? "passive"
            : !(entity instanceof net.minecraft.world.entity.projectile.Projectile)
                && entity.getType().getCategory() == net.minecraft.world.entity.MobCategory.MISC ? "items" : null;
        return option != null && setting("EntityESP", option, 1) != 0;
    }

    public static boolean highlight(Entity entity) {
        var mc = Minecraft.getInstance();
        if (!(entity instanceof LivingEntity) || entity == mc.player || !entity.isAlive() || entity.isInvisible()) return false;
        var camera = mc.gameRenderer.getMainCamera();
        return camera != null && entity.position().distanceToSqr(camera.getPosition()) <= 128 * 128;
    }
}
