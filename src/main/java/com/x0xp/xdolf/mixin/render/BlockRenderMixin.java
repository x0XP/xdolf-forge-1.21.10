package com.x0xp.xdolf.mixin.render;

import com.x0xp.xdolf.module.world.XRayModule;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
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
     * Lift XRay-selected blocks out of deep shader darkness without making them emissive.
     * The consumer preserves normal per-face contrast, but applies a modest brightness/light floor
     * so side faces remain readable through terrain with Complementary enabled.
     */
    @ModifyVariable(method = "renderBatched", at = @At("HEAD"), argsOnly = true, ordinal = 0)
    private VertexConsumer xdolf$xrayReadableLight(VertexConsumer consumer) {
        return XRayModule.rendering ? new XRayReadableLightConsumer(consumer) : consumer;
    }

    @Inject(method = "renderLiquid", at = @At("HEAD"), cancellable = true)
    private void xdolf$fluid(CallbackInfo ci) { if (XRayModule.rendering) ci.cancel(); }

    private static final class XRayReadableLightConsumer implements VertexConsumer {
        private static final float BRIGHTNESS_FLOOR = 0.72f;
        // Packed block/sky light use four-bit values shifted into bits 4 and 20 respectively.
        // 11/15 is deliberately below full-bright while still keeping shader-darkened faces legible.
        private static final int LIGHT_FLOOR = 0xB000B0;
        private static final int LIGHT_MASK = 0xF000F0;

        private final VertexConsumer delegate;

        private XRayReadableLightConsumer(VertexConsumer delegate) {
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
            delegate.setLight(liftLight((v << 16) | (u & 0xFFFF)));
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
            delegate.putBulkData(pose, quad, red, green, blue, alpha, liftLight(packedLight), packedOverlay);
        }

        @Override
        public void putBulkData(PoseStack.Pose pose, BakedQuad quad, float[] brightness,
                                float red, float green, float blue, float alpha,
                                int[] lightmap, int packedOverlay, boolean readExistingColor) {
            float[] liftedBrightness = Arrays.copyOf(brightness, brightness.length);
            for (int i = 0; i < liftedBrightness.length; i++)
                liftedBrightness[i] = Math.max(liftedBrightness[i], BRIGHTNESS_FLOOR);

            int[] liftedLight = Arrays.copyOf(lightmap, lightmap.length);
            for (int i = 0; i < liftedLight.length; i++)
                liftedLight[i] = liftLight(liftedLight[i]);

            delegate.putBulkData(pose, quad, liftedBrightness, red, green, blue, alpha,
                liftedLight, packedOverlay, readExistingColor);
        }

        private static int liftLight(int packedLight) {
            int block = Math.max(packedLight & 0xF0, LIGHT_FLOOR & 0xF0);
            int sky = Math.max(packedLight & 0xF00000, LIGHT_FLOOR & 0xF00000);
            return (packedLight & ~LIGHT_MASK) | block | sky;
        }
    }
}
