package com.darkcart.xdolf.mixin;

import com.darkcart.xdolf.Hooks;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.world.entity.Entity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Prevent vanilla labels being drawn over the original custom player tags. */
@Mixin(EntityRenderer.class)
public abstract class EntityNameTagMixin {
    @Inject(method="extractRenderState",at=@At("TAIL"))
    private void xdolf$customTag(Entity entity,EntityRenderState state,float partialTick,CallbackInfo ci) {
        if(Hooks.enabled("Nametags"))state.nameTag=null;
    }
}
