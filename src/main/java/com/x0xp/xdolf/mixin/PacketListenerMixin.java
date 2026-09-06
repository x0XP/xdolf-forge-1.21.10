package com.x0xp.xdolf.mixin;

import com.x0xp.xdolf.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.network.protocol.game.ClientboundExplodePacket;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import java.util.Optional;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(ClientPacketListener.class)
public abstract class PacketListenerMixin {
    @Inject(method = "handleSetEntityMotion", at = @At("HEAD"), cancellable = true)
    private void xdolf$velocity(ClientboundSetEntityMotionPacket packet, CallbackInfo ci) {
        if (Hooks.active("AntiVelocity") && packet.getId() == Minecraft.getInstance().player.getId()) ci.cancel();
    }
    @ModifyVariable(method = "handleExplosion", at = @At("HEAD"), argsOnly = true)
    private ClientboundExplodePacket xdolf$explosion(ClientboundExplodePacket packet) {
        if (!Hooks.active("AntiVelocity")) return packet;
        // Keep explosion effects and sound; remove only the optional player impulse.
        return new ClientboundExplodePacket(packet.center(), packet.radius(), packet.blockCount(), Optional.empty(),
            packet.explosionParticle(), packet.explosionSound(), packet.blockParticles());
    }
}
