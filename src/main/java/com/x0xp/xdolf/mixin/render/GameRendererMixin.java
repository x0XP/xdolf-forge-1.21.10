package com.x0xp.xdolf.mixin.render;

import com.x0xp.xdolf.ui.clickgui.ClientScreen;

import com.x0xp.xdolf.core.Hooks;
import com.x0xp.xdolf.render.MarkerVisuals;
import com.x0xp.xdolf.render.WorldVisuals;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
    @org.spongepowered.asm.mixin.Unique
    private com.mojang.blaze3d.buffers.GpuBufferSlice xdolf$worldProjection;

    @Inject(method = "renderLevel", at = @At("HEAD"))
    private void xdolf$resetWorldProjection(CallbackInfo ci) {
        xdolf$worldProjection = null;
    }

    // Capture the world projection before hand rendering replaces it. It includes the
    // world FOV and camera effects needed by both ESP alignment and tracer unbobbing.
    @Inject(method = "renderLevel", at = @At(value = "INVOKE",
        target = "Lnet/minecraft/client/renderer/LevelRenderer;renderLevel(Lcom/mojang/blaze3d/resource/GraphicsResourceAllocator;Lnet/minecraft/client/DeltaTracker;ZLnet/minecraft/client/Camera;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lorg/joml/Matrix4f;Lcom/mojang/blaze3d/buffers/GpuBufferSlice;Lorg/joml/Vector4f;Z)V", shift = At.Shift.AFTER))
    private void xdolf$captureWorldProjection(CallbackInfo ci) {
        xdolf$worldProjection = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrixBuffer();
    }

    // OptiFine performs its final composite after LevelRenderer.renderLevel returns.
    // Wait for GameRenderer's entire world stage, including that composite, to finish.
    @Inject(method = "renderLevel", at = @At("RETURN"))
    private void xdolf$renderShaderWorldVisuals(CallbackInfo ci) {
        if (xdolf$worldProjection == null) return;
        var previous = com.mojang.blaze3d.systems.RenderSystem.getProjectionMatrixBuffer();
        var previousType = com.mojang.blaze3d.systems.RenderSystem.getProjectionType();
        com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(xdolf$worldProjection,
            com.mojang.blaze3d.ProjectionType.PERSPECTIVE);
        try {
            WorldVisuals.renderAfterLevel();
            MarkerVisuals.renderAfterLevel();
        } finally {
            com.mojang.blaze3d.systems.RenderSystem.setProjectionMatrix(previous, previousType);
            xdolf$worldProjection = null;
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

