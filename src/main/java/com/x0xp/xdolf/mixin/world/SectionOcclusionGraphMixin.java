package com.x0xp.xdolf.mixin.world;

import com.x0xp.xdolf.core.Hooks;
import net.minecraft.client.renderer.SectionOcclusionGraph;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/**
 * Smart section occlusion assumes the camera is travelling through ordinary visible air spaces.
 * A noclip camera inside solid terrain violates that assumption and can cull large underground
 * regions. Freecam keeps normal frustum/render-distance culling, but disables this portal-style
 * section-to-section occlusion while active.
 */
@Mixin(SectionOcclusionGraph.class)
public abstract class SectionOcclusionGraphMixin {
    @ModifyVariable(
        method = "update(ZLnet/minecraft/client/Camera;Lnet/minecraft/client/renderer/culling/Frustum;Ljava/util/List;Lit/unimi/dsi/fastutil/longs/LongOpenHashSet;)V",
        at = @At("HEAD"),
        argsOnly = true,
        ordinal = 0
    )
    private boolean xdolf$freecamSmartCull(boolean smartCull) {
        return Hooks.enabled("Freecam") ? false : smartCull;
    }
}
