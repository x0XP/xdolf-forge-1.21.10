package com.x0xp.xdolf.mixin;

import com.x0xp.xdolf.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.core.Holder;
import net.minecraft.world.effect.MobEffect;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin {
    @Inject(method = "hasEffect", at = @At("HEAD"), cancellable = true)
    private void xdolf$hasNightVision(Holder<MobEffect> effect, CallbackInfoReturnable<Boolean> cir) {
        if ((Object) this == Minecraft.getInstance().player && effect.equals(MobEffects.NIGHT_VISION) && (Hooks.enabled("Fullbright") || Hooks.enabled("XRay"))) cir.setReturnValue(true);
    }
    @Inject(method = "getEffect", at = @At("HEAD"), cancellable = true)
    private void xdolf$getNightVision(Holder<MobEffect> effect, CallbackInfoReturnable<MobEffectInstance> cir) {
        if ((Object) this == Minecraft.getInstance().player && effect.equals(MobEffects.NIGHT_VISION) && (Hooks.enabled("Fullbright") || Hooks.enabled("XRay")))
            cir.setReturnValue(new MobEffectInstance(MobEffects.NIGHT_VISION, 1000, 0, false, false, false));
    }
}
