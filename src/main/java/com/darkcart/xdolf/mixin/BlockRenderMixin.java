package com.darkcart.xdolf.mixin;

import com.darkcart.xdolf.XRayModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderMixin {
    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
    private void xdolf$filter(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack pose, VertexConsumer consumer,
                              boolean sides, List<BlockModelPart> parts, CallbackInfo ci) {
        if (XRayModule.rendering && !XRayModule.visible(state)) ci.cancel();
    }
    @Inject(method = "renderLiquid", at = @At("HEAD"), cancellable = true)
    private void xdolf$fluid(CallbackInfo ci) { if (XRayModule.rendering) ci.cancel(); }
}
