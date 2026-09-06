package com.darkcart.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.Items;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.event.sound.PlaySoundEvent;

final class AutoFishModule extends ClientModule {
    private record Splash(Vec3 position, long time) {}
    private volatile Splash splash;
    private int delay = 20;
    private final ModuleSetting autoCast = setting("autocast", 0, 0, 1, 1);
    private final ModuleSetting recastDelay = setting("recast", 40, 10, 120, 1);
    private final ModuleSetting castDelay = setting("castdelay", 5, 1, 60, 1);
    private final ModuleSetting recaster = setting("recaster", 1, 0, 1, 1);
    private long lastUse;
    private boolean castAfterCatch;

    AutoFishModule() {
        super("AutoFish", "Reel on nearby bobber splashes and optionally recast.", "Player");
        PlaySoundEvent.BUS.addListener(event -> {
            var sound = event.getOriginalSound();
            if (sound.getLocation().getPath().equals("entity.fishing_bobber.splash"))
                splash = new Splash(new Vec3(sound.getX(), sound.getY(), sound.getZ()), System.nanoTime());
        });
    }

    @Override public void tick(Minecraft mc) {
        if (delay > 0) { delay--; return; }
        if (mc.gameMode == null || mc.player.isUsingItem()) return;
        InteractionHand hand = mc.player.getMainHandItem().is(Items.FISHING_ROD) ? InteractionHand.MAIN_HAND
            : mc.player.getOffhandItem().is(Items.FISHING_ROD) ? InteractionHand.OFF_HAND : null;
        if (hand == null) { splash = null; return; }
        var hook = mc.player.fishing;
        Splash recent = splash;
        long now = System.nanoTime();
        if (lastUse == 0) lastUse = now;
        if (hook != null && ((recent != null && now - recent.time() < 1_000_000_000L
            && recent.position().distanceToSqr(hook.position()) < 4) || (recaster.on() && now-lastUse >= recastDelay.get()*1_000_000_000L))) {
            splash = null;
            mc.gameMode.useItem(mc.player, hand);
            delay = 20; lastUse = now; castAfterCatch = true;
        } else if (hook == null && (castAfterCatch || (autoCast.on() && now-lastUse >= castDelay.get()*1_000_000_000L))) {
            splash = null;
            mc.gameMode.useItem(mc.player, hand);
            delay = 20; lastUse = now; castAfterCatch = false;
        }
    }

    @Override public void reset(Minecraft mc) { splash = null; delay = 20; lastUse = 0; castAfterCatch = false; }
}
