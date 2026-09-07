package com.x0xp.xdolf.mixin.accessor;

import net.minecraft.client.player.ClientInput;
import net.minecraft.world.phys.Vec2;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

@Mixin(ClientInput.class)
public interface ClientInputAccess {
    @Accessor("moveVector") void xdolf$setMoveVector(Vec2 value);
}
