package com.x0xp.xdolf.module.combat;

import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Items;

public final class AutoArmorModule extends ClientModule {
    private int delay;

    public AutoArmorModule() {
        super("AutoArmor", "Equip higher base-armor pieces using inventory transactions.", "Combat");
    }

    @Override
    public void tick(Minecraft mc) {
        if (delay > 0) { delay--; return; }
        if (!InventoryActions.available(mc) || mc.player.isUsingItem()) return;
        for (int i = 0; i < 36; i++) {
            var candidate = mc.player.getInventory().getItem(i);
            var equippable = candidate.get(DataComponents.EQUIPPABLE);
            if (equippable == null) continue;
            var slot = equippable.slot();
            int target = switch (slot) { case HEAD -> 5; case CHEST -> 6; case LEGS -> 7; case FEET -> 8; default -> -1; };
            if (target < 0) continue;
            var current = mc.player.getItemBySlot(slot);
            if (current.is(Items.ELYTRA) || InventoryActions.armorValue(candidate, slot) <= InventoryActions.armorValue(current, slot)) continue;
            if (!mc.player.inventoryMenu.getSlot(target).mayPickup(mc.player)) continue;
            int source = InventoryActions.menuSlot(i);
            InventoryActions.click(mc, source);
            InventoryActions.click(mc, target);
            InventoryActions.click(mc, source);
            delay = 5;
            return;
        }
    }

    @Override
    public void reset(Minecraft mc) {
        delay = 0;
    }
}
