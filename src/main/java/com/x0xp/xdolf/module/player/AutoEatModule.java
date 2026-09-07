package com.x0xp.xdolf.module.player;

import com.x0xp.xdolf.module.ClientModule;
import com.x0xp.xdolf.settings.*;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.Items;

public final class AutoEatModule extends ClientModule {
    private final NumberSetting hunger = setting("hunger", 7, 0, 19, 1);
    private int original = -1;
    private int selected = -1;
    private LocalPlayer owner;
    private boolean holdingUse;

    public AutoEatModule() {
        super("AutoEat", "Eat ordinary food; preserve golden apples and avoid harmful food.", "Player");
    }

    @Override
    public void tick(Minecraft mc) {
        if (!InventoryActions.available(mc)) { reset(mc); return; }
        if (owner != null && owner != mc.player) reset(mc);
        if (holdingUse && mc.player.getInventory().getSelectedSlot() != selected) { reset(mc); return; }
        if (mc.player.getFoodData().getFoodLevel() > hunger.get() || !mc.player.canEat(false)) { reset(mc); return; }
        if (mc.player.isUsingItem()) return;

        int found = -1;
        for (int i = 0; i < 36; i++) {
            var stack = mc.player.getInventory().getItem(i);
            if (stack.has(DataComponents.FOOD) && !stack.is(Items.GOLDEN_APPLE) && !stack.is(Items.ENCHANTED_GOLDEN_APPLE)
                && !stack.is(Items.ROTTEN_FLESH) && !stack.is(Items.SPIDER_EYE) && !stack.is(Items.PUFFERFISH)
                && !stack.is(Items.POISONOUS_POTATO) && !stack.is(Items.SUSPICIOUS_STEW) && !stack.is(Items.CHORUS_FRUIT)
                && !stack.is(Items.CHICKEN)) {
                found = i;
                break;
            }
        }
        if (found == -1) { reset(mc); return; }

        owner = mc.player;
        if (original == -1) original = mc.player.getInventory().getSelectedSlot();
        if (found >= 9) {
            mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, found, original, ClickType.SWAP, mc.player);
            found = original;
        }
        selected = found;
        mc.player.getInventory().setSelectedSlot(selected);
        mc.gameMode.useItem(mc.player, InteractionHand.MAIN_HAND);
        if (mc.player.isUsingItem()) {
            mc.options.keyUse.setDown(true);
            holdingUse = true;
        }
    }

    @Override
    public void reset(Minecraft mc) {
        if (holdingUse) {
            mc.options.keyUse.setDown(false);
            if (owner == mc.player && mc.gameMode != null && owner.isUsingItem()) mc.gameMode.releaseUsingItem(owner);
        }
        if (owner != null && original >= 0 && owner.getInventory().getSelectedSlot() == selected)
            owner.getInventory().setSelectedSlot(original);
        owner = null;
        original = selected = -1;
        holdingUse = false;
    }
}
