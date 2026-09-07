package com.x0xp.xdolf.mixin.render;

import com.x0xp.xdolf.core.Hooks;
import com.x0xp.xdolf.module.world.XRayModule;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Block.class)
public abstract class BlockMixin {
    @Inject(method = "shouldRenderFace(Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"), cancellable = true)
    private static void xdolf$faces(BlockState state, BlockState adjacent, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (XRayModule.rendering && XRayModule.visible(state)) cir.setReturnValue(true);
    }
    @Inject(method = "shouldRenderFace(Lnet/minecraft/world/level/BlockGetter;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/world/level/block/state/BlockState;Lnet/minecraft/core/Direction;)Z", at = @At("HEAD"), cancellable = true)
    private static void xdolf$forgeFaces(BlockGetter level, BlockPos pos, BlockState state, BlockState adjacent, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (XRayModule.rendering && XRayModule.visible(state)) cir.setReturnValue(true);
    }
    @Inject(method = "getFriction", at = @At("HEAD"), cancellable = true)
    private void xdolf$iceFriction(CallbackInfoReturnable<Float> cir) {
        if (((Object) this == Blocks.ICE || (Object) this == Blocks.PACKED_ICE || (Object) this == Blocks.BLUE_ICE)
            && Hooks.active("NoSlowdown")) cir.setReturnValue(0.39f);
    }
}
