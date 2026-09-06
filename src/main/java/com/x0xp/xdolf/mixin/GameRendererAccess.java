package com.x0xp.xdolf.mixin;

import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Camera;
import net.minecraft.client.renderer.GameRenderer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Invoker;

@Mixin(GameRenderer.class)
public interface GameRendererAccess {
    @Invoker("getFov") float xdolf$fov(Camera camera, float partialTick, boolean changingFov);
    @Invoker("bobView") void xdolf$bobView(PoseStack poseStack, float partialTick);
}
