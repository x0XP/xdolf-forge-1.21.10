package com.x0xp.xdolf.mixin;

import com.x0xp.xdolf.XRayModule;
import net.minecraft.client.renderer.chunk.SectionCompiler;
import net.minecraft.world.level.block.state.BlockState;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Mixin(SectionCompiler.class)
public abstract class SectionCompilerMixin {
    @Redirect(method = "compile", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/state/BlockState;isSolidRender()Z"))
    private boolean xdolf$visibility(BlockState state) { return !XRayModule.rendering && state.isSolidRender(); }
}
