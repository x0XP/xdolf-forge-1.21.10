package com.x0xp.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.world.item.Items;

final class AutoTotemModule extends ClientModule {
    private int delay;

    AutoTotemModule() {
        super("AutoTotem", "Automatically replace the offhand item with a Totem of Undying.", "Combat");
    }

    @Override
    public void tick(Minecraft mc) {
        if (delay > 0) { delay--; return; }
        if (!InventoryActions.available(mc) || mc.player.isUsingItem()) return;
        if (mc.player.getOffhandItem().is(Items.TOTEM_OF_UNDYING)) return;

        for (int i = 0; i < 36; i++) {
            if (!mc.player.getInventory().getItem(i).is(Items.TOTEM_OF_UNDYING)) continue;
            int source = InventoryActions.menuSlot(i);
            if (!mc.player.inventoryMenu.getSlot(source).mayPickup(mc.player)) continue;
            InventoryActions.click(mc, source);
            InventoryActions.click(mc, 45);
            InventoryActions.click(mc, source);
            delay = 3;
            return;
        }
    }

    @Override
    public void reset(Minecraft mc) {
        delay = 0;
    }
}
