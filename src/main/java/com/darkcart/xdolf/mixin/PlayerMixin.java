package com.darkcart.xdolf.mixin;

import com.darkcart.xdolf.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.world.entity.player.Player;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(Player.class)
public abstract class PlayerMixin {
    @Inject(method = "isStayingOnGroundSurface", at = @At("HEAD"), cancellable = true)
    private void xdolf$safeWalk(CallbackInfoReturnable<Boolean> cir) {
        var player = Minecraft.getInstance().player;
        if ((Object) this == player && Hooks.active("SafeWalk") && player.onGround() && !player.getAbilities().flying) cir.setReturnValue(true);
    }
}
