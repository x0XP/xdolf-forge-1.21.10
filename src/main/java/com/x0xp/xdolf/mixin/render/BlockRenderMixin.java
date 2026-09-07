package com.x0xp.xdolf.mixin.render;

import com.x0xp.xdolf.module.world.XRayModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.block.BlockRenderDispatcher;
import net.minecraft.client.renderer.block.model.BakedQuad;
import net.minecraft.client.renderer.block.model.BlockModelPart;
import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockAndTintGetter;
import net.minecraft.world.level.block.state.BlockState;
import java.util.Arrays;
import java.util.List;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(BlockRenderDispatcher.class)
public abstract class BlockRenderMixin {
    @Inject(method = "renderBatched", at = @At("HEAD"), cancellable = true)
    private void xdolf$filter(BlockState state, BlockPos pos, BlockAndTintGetter level, PoseStack pose, VertexConsumer consumer,
                              boolean sides, List<BlockModelPart> parts, CallbackInfo ci) {
        if (XRayModule.rendering && !XRayModule.visible(state)) ci.cancel();
    }

    /**
     * XRay-selected blocks should remain readable regardless of shader lighting direction.
     * Wrapping the consumer here keeps the compatibility hook at BlockRenderDispatcher's stable
     * public render entry point instead of injecting into OptiFine-rewritten model internals.
     */
    @ModifyVariable(method = "renderBatched", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private VertexConsumer xdolf$xrayFullbright(VertexConsumer consumer) {
        return XRayModule.rendering ? new XRayFullbrightConsumer(consumer) : consumer;
    }

    @Inject(method = "renderLiquid", at = @At("HEAD"), cancellable = true)
    private void xdolf$fluid(CallbackInfo ci) { if (XRayModule.rendering) ci.cancel(); }

    private static final class XRayFullbrightConsumer implements VertexConsumer {
        private final VertexConsumer delegate;

        private XRayFullbrightConsumer(VertexConsumer delegate) {
            this.delegate = delegate;
        }

        @Override
        public VertexConsumer addVertex(float x, float y, float z) {
            delegate.addVertex(x, y, z);
            return this;
        }

        @Override
        public VertexConsumer setColor(int red, int green, int blue, int alpha) {
            delegate.setColor(red, green, blue, alpha);
            return this;
        }

        @Override
        public VertexConsumer setUv(float u, float v) {
            delegate.setUv(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv1(int u, int v) {
            delegate.setUv1(u, v);
            return this;
        }

        @Override
        public VertexConsumer setUv2(int u, int v) {
            delegate.setLight(LightTexture.FULL_BRIGHT);
            return this;
        }

        @Override
        public VertexConsumer setNormal(float normalX, float normalY, float normalZ) {
            delegate.setNormal(normalX, normalY, normalZ);
            return this;
        }

        @Override
        public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float red, float green, float blue,
                                float alpha, int packedLight, int packedOverlay) {
            delegate.putBulkData(pose, quad, red, green, blue, alpha, LightTexture.FULL_BRIGHT, packedOverlay);
        }

        @Override
        public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness,
                                float red, float green, float blue, float alpha,
                                int[] lightmap, int packedOverlay, boolean readExistingColor) {
            float[] fullBrightness = new float[brightness.length];
            Arrays.fill(fullBrightness, 1.0f);
            int[] fullLight = new int[lightmap.length];
            Arrays.fill(fullLight, LightTexture.FULL_BRIGHT);
            delegate.putBulkData(pose, quad, fullBrightness, red, green, blue, alpha,
                fullLight, packedOverlay, readExistingColor);
        }
    }
}
