package com.x0xp.xdolf.mixin;

import net.minecraft.client.renderer.LevelRenderer;
import net.minecraft.client.renderer.SubmitNodeStorage;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.gen.Accessor;

/** Gives Xdolf's retained logout player model access to the normal world entity submit queue. */
@Mixin(LevelRenderer.class)
public interface LevelRendererAccess {
    @Accessor("submitNodeStorage")
    SubmitNodeStorage xdolf$getSubmitNodeStorage();
}
