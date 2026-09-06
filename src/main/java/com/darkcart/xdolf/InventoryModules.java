package com.darkcart.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.inventory.ClickType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemAttributeModifiers;
import java.util.List;

final class InventoryModules {
    private static boolean available(Minecraft mc) {
        return mc.gameMode != null && mc.player.containerMenu == mc.player.inventoryMenu
            && mc.player.containerMenu.getCarried().isEmpty();
    }

    private static int menuSlot(int inventorySlot) { return inventorySlot < 9 ? inventorySlot + 36 : inventorySlot; }

    private static void click(Minecraft mc, int slot) {
        mc.gameMode.handleInventoryMouseClick(mc.player.inventoryMenu.containerId, slot, 0, ClickType.PICKUP, mc.player);
    }

    private static double armorValue(ItemStack stack, EquipmentSlot slot) {
        double[] total = {0};
        stack.getOrDefault(DataComponents.ATTRIBUTE_MODIFIERS, ItemAttributeModifiers.EMPTY).forEach(slot, (attribute, modifier) -> {
            if (attribute.equals(Attributes.ARMOR)) total[0] += modifier.amount();
        });
        return total[0];
    }

    static void addTo(List<ClientModule> modules) {
        modules.add(new ClientModule("AutoArmor", "Equip higher base-armor pieces using inventory transactions.", "Combat") {
            int delay;
            public void tick(Minecraft mc) {
                if (delay > 0) { delay--; return; }
                if (!available(mc) || mc.player.isUsingItem()) return;
                for (int i = 0; i < 36; i++) {
                    var candidate = mc.player.getInventory().getItem(i);
                    var equippable = candidate.get(DataComponents.EQUIPPABLE);
                    if (equippable == null) continue;
                    var slot = equippable.slot();
                    int target = switch (slot) { case HEAD -> 5; case CHEST -> 6; case LEGS -> 7; case FEET -> 8; default -> -1; };
                    if (target < 0) continue;
                    var current = mc.player.getItemBySlot(slot);
                    // Do not replace an equipped elytra automatically.
                    if (current.is(Items.ELYTRA) || armorValue(candidate, slot) <= armorValue(current, slot)) continue;
                    if (!mc.player.inventoryMenu.getSlot(target).mayPickup(mc.player)) continue;
                    click(mc, menuSlot(i)); click(mc, target); click(mc, menuSlot(i));
                    delay = 5;
                    return;
                }
            }
            public void reset(Minecraft mc) { delay = 0; }
        });
        modules.add(new ClientModule("AutoEat", "Eat ordinary food; preserve golden apples and avoid harmful food.", "Player") {
            final ModuleSetting hunger = setting("hunger", 7, 0, 19, 1);
            int original = -1, selected = -1;
            LocalPlayer owner;
            boolean holdingUse;
            public void tick(Minecraft mc) {
                if (!available(mc)) { reset(mc); return; }
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
                        && !stack.is(Items.CHICKEN)) { found = i; break; }
                }
                if (found == -1) { reset(mc); return; }
                owner = mc.player;
                if (original == -1) original = mc.player.getInventory().getSelectedSlot();
                if (found >= 9) {
                    // Swap a food stack into the currently selected hotbar slot without using the cursor.
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
            public void reset(Minecraft mc) {
                if (holdingUse) {
                    mc.options.keyUse.setDown(false);
                    if (owner == mc.player && mc.gameMode != null && owner.isUsingItem()) mc.gameMode.releaseUsingItem(owner);
                }
                if (owner != null && original >= 0 && owner.getInventory().getSelectedSlot() == selected)
                    owner.getInventory().setSelectedSlot(original);
                owner = null; original = selected = -1; holdingUse = false;
            }
        });
    }
}
