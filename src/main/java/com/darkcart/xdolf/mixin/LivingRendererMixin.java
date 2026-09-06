package com.darkcart.xdolf.mixin;

import com.darkcart.xdolf.Hooks;
import com.darkcart.xdolf.ChamsRenderTypes;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.LivingEntityRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.ResourceLocation;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntityRenderer.class)
public abstract class LivingRendererMixin {
    @Shadow public abstract ResourceLocation getTextureLocation(LivingEntityRenderState state);
    @Inject(method = "getRenderType", at = @At("HEAD"), cancellable = true)
    private void xdolf$chams(LivingEntityRenderState state, boolean visible, boolean translucent, boolean glowing, CallbackInfoReturnable<RenderType> cir) {
        if (Hooks.enabled("Chams") && state.entityType == net.minecraft.world.entity.EntityType.PLAYER && (visible || translucent)) cir.setReturnValue(ChamsRenderTypes.forTexture(getTextureLocation(state)));
    }
}
