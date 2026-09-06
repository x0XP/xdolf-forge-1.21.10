package com.x0xp.xdolf.mixin;

import com.x0xp.xdolf.Hooks;
import net.minecraft.client.player.LocalPlayer;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LocalPlayer.class)
public abstract class LocalPlayerMixin {
    @Shadow private float jumpRidingScale;
    @Inject(method = "sendRidingJump", at = @At("HEAD"))
    private void xdolf$horseJump(CallbackInfo ci) { if (Hooks.active("HorseJump")) jumpRidingScale = 1; }
}
