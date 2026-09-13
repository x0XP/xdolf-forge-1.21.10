package com.x0xp.xdolf.mixin.render;

import com.x0xp.xdolf.core.Hooks;
import com.x0xp.xdolf.render.MarkerVisuals;
import com.x0xp.xdolf.render.WorldVisuals;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.PerspectiveProjectionMatrixBuffer;
import org.joml.Matrix4f;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyArg;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @Shadow @Final
    private PerspectiveProjectionMatrixBuffer levelProjectionMatrixBuffer;

    /**
     * Keep a CPU-side copy rather than retaining RenderSystem's GpuBufferSlice. GameRenderer
     * reuses the level projection uniform later in the frame, so a stored slice does not preserve
     * its matrix contents. That made tracer-origin bob cancellation use a later projection while
     * ESP could still appear aligned.
     */
    @Unique
    private Matrix4f xdolf$worldProjectionMatrix;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void xdolf$resetWorldProjection(CallbackInfo ci) {
        xdolf$worldProjectionMatrix = null;
    }

    // This is the exact projection LevelRenderer sees after hurt camera, view bobbing and
    // portal/nausea transforms. Copy it before the hand and OptiFine stages can replace it.
    @ModifyArg(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V"), index = 5)
    private Matrix4f xdolf$captureWorldProjection(Matrix4f projection) {
        xdolf$worldProjectionMatrix = new Matrix4f(projection);
        return projection;
    }

    // OptiFine performs its final composite after LevelRenderer.renderLevel returns. Draw Xdolf
    // after the complete world stage, but first re-upload the captured world matrix so ESP and
    // tracer bob compensation operate against the same projection used for the terrain.
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void xdolf$renderShaderWorldVisuals(CallbackInfo ci) {
        Matrix4f worldProjection = xdolf$worldProjectionMatrix;
        if (worldProjection == null) return;

        var previous = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrixBuffer();
        var previousType = com.mojang.blaze3d.systems.RenderSystem.getProjectionType();
        var restoredWorldProjection = levelProjectionMatrixBuffer.getBuffer(worldProjection);
        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(restoredWorldProjection,
            com.mojang.blaze3d.ProjectionType.PERSPECTIVE);
        try {
            WorldVisuals.renderAfterLevel();
            MarkerVisuals.renderAfterLevel();
        } finally {
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(previous, previousType);
            xdolf$worldProjectionMatrix = null;
        }
    }

    @Inject(method = "processBlurEffect", at = @At("HEAD"), cancellable = true)
    private void xdolf$legacyGuiBackground(CallbackInfo ci) {
        if (net.minecraft.client.Minecraft.getInstance().screen instanceof com.x0xp.xdolf.ui.clickgui.ClientScreen) ci.cancel();
    }

    @Inject(method = "bobHurt", at = @At("HEAD"), cancellable = true)
    private void xdolf$noHurt(CallbackInfo ci) {
        if (Hooks.enabled("NoHurtCam")) ci.cancel();
    }

    @Inject(method = "getNightVisionScale", at = @At("HEAD"), cancellable = true)
    private static void xdolf$nightVision(CallbackInfoReturnable<Float> cir) {
        if ((Hooks.enabled("Fullbright") || Hooks.enabled("XRay"))) cir.setReturnValue(1f);
    }
}
