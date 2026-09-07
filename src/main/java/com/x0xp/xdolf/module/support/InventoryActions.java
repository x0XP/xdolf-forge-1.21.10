package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.ItemAttributeModifiers;

/** Shared, synced inventory operations used by automatic inventory modules. */
final class InventoryActions {
    private InventoryActions() {}

    static boolean available(Minecraft mc) {
        return mc.gameMode != null && mc.player.containerMenu == mc.player.inventoryMenu
            && mc.player.containerMenu.getCarried().isEmpty();
    }

    static int menuSlot(int inventorySlot) {
        return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot;
    }

    static void click(Minecraft mc, int slot) {
        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, mc.player);
    }

    static double armorValue(ItemStack stack, EquipmentSlot slot) {
        double[] total = {0};
        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).forEach(slot, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ARMOR)) total[0] += modifier.amount();
        });
        return total[0];
    }
}
