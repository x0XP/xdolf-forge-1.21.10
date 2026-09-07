package com.x0xp.xdolf.mixin.world;

import com.x0xp.xdolf.module.world.XRayModule;
import net.minecraft.world.level.block.state.BlockBehaviour;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/**
 * Makes terrain non-solid for XRay face generation without depending on the
 * implementation of SectionCompiler.compile(). OptiFine replaces that method,
 * so call-site redirects there are not stable across vanilla and OptiFine.
 */
@Mixin(BlockBehaviour.BlockStateBase.class)
public abstract class BlockStateRenderMixin {
    @Inject(method = "isSolidRender", at = @At("HEAD"), cancellable = true)
    private void xdolf$xrayNonSolid(CallbackInfoReturnable<Boolean> cir) {
        if (XRayModule.rendering) cir.setReturnValue(false);
    }
}
