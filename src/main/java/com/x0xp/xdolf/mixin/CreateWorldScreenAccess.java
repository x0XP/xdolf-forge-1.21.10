package com.x0xp.xdolf.mixin;
import net.minecraft.client.gui.screens.worldselection.CreateWorldScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;
@Mixin(CreateWorldScreen.class)
public interface CreateWorldScreenAccess {
    @Invoker("onCreate") void xdolf$create();
}
