package com.x0xp.xdolf.mixin;

import com.x0xp.xdolf.Hooks;
import net.minecraft.client.Camera;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.FogType;
import net.minecraft.world.phys.Vec3;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Camera.class)
public abstract class CameraMixin {
    @Shadow private boolean detached;
    @Shadow protected abstract void setPosition(Vec3 position);

    @Inject(method = "setup", at = @At("TAIL"))
    private void xdolf$freecam(BlockGetter level, Entity entity, boolean detachedView, boolean reverseView,
                               float partialTick, CallbackInfo ci) {
        var position = Hooks.freecamPosition(partialTick);
        if (Hooks.enabled("Freecam") && position != null) {
            setPosition(position);
            detached = true;
        }
    }

    /** Freecam is allowed to sit inside solid blocks without applying the camera's in-block view state. */
    @Inject(method = "getBlockAtCamera", at = @At("HEAD"), cancellable = true)
    private void xdolf$freecamBlockView(CallbackInfoReturnable<BlockState> cir) {
        if (Hooks.enabled("Freecam")) cir.setReturnValue(Blocks.AIR.defaultBlockState());
    }

    /** Likewise, clipping through water/lava must not replace the normal freecam scene with fluid fog. */
    @Inject(method = "getFluidInCamera", at = @At("HEAD"), cancellable = true)
    private void xdolf$freecamFluidView(CallbackInfoReturnable<FogType> cir) {
        if (Hooks.enabled("Freecam")) cir.setReturnValue(FogType.NONE);
    }
}
