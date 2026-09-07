package com.x0xp.xdolf.mixin.render;

import com.x0xp.xdolf.ui.clickgui.ClientScreen;

import com.x0xp.xdolf.core.Hooks;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(GameRenderer.class)
public abstract class GameRendererMixin {
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
