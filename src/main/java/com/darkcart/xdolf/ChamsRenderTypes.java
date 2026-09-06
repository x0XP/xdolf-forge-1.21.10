package com.darkcart.xdolf;

import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.resources.ResourceLocation;
import java.util.HashMap;
import java.util.Map;

public final class ChamsRenderTypes {
    private static final Map<ResourceLocation, RenderType> CACHE = new HashMap<>();
    private static final RenderPipeline PIPELINE = RenderPipeline.builder(RenderPipelines.ENTITY_SNIPPET)
        .withLocation(ResourceLocation.fromNamespaceAndPath(Xdolf.ID, "pipeline/chams"))
        .withShaderDefine("ALPHA_CUTOUT", 0.1f)
        .withCull(false).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false).withBlend(BlendFunction.TRANSLUCENT).build();

    public static RenderType forTexture(ResourceLocation texture) {
        if (CACHE.size() > 128) CACHE.clear();
        return CACHE.computeIfAbsent(texture, value -> RenderType.create("xdolf_chams", 1536, true, true, PIPELINE,
            RenderType.CompositeState.builder()
                .setTextureState(new RenderStateShard.TextureStateShard(value, false))
                .setLightmapState(RenderStateShard.LIGHTMAP).setOverlayState(RenderStateShard.OVERLAY)
                .createCompositeState(false)));
    }
}
