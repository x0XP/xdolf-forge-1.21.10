package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.core.Hooks;

import com.x0xp.xdolf.module.ClientModule;


import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;

public final class SprintModule extends ClientModule {
    private LocalPlayer owner;
    private boolean applied;

    public SprintModule() {
        super("Sprint", "Sprint while moving forward and able to sprint.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {
        var player = mc.player;
        boolean eligible = (mc.options.keyUp.isDown() || Hooks.active("AutoWalk")) && !player.isShiftKeyDown()
            && !player.horizontalCollision && !player.isUsingItem()
            && (player.getFoodData().getFoodLevel() > 6 || player.getAbilities().mayfly);
        if (eligible && !player.isSprinting()) {
            owner = player;
            applied = true;
            player.setSprinting(true);
        } else if (!eligible) reset(mc);
    }

    @Override
    public void reset(Minecraft mc) {
        if (applied && owner != null) owner.setSprinting(false);
        applied = false;
        owner = null;
    }
}
