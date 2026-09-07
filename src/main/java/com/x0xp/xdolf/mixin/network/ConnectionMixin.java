package com.x0xp.xdolf.mixin.network;

import com.x0xp.xdolf.core.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;
import net.minecraft.network.protocol.game.ServerboundPlayerCommandPacket;
import io.netty.channel.ChannelFutureListener;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Connection.class)
public abstract class ConnectionMixin {
    @Inject(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), cancellable = true)
    private void xdolf$sprint(Packet<?> packet, ChannelFutureListener listener, boolean flush, CallbackInfo ci) {
        if (((Connection) (Object) this).getSending() != PacketFlow.SERVERBOUND || !Hooks.active("AntiHunger")) return;
        if (packet instanceof ServerboundPlayerCommandPacket command &&
            (command.getAction() == ServerboundPlayerCommandPacket.Action.START_SPRINTING || command.getAction() == ServerboundPlayerCommandPacket.Action.STOP_SPRINTING)) ci.cancel();
    }
    @ModifyVariable(method = "send(Lnet/minecraft/network/protocol/Packet;Lio/netty/channel/ChannelFutureListener;Z)V", at = @At("HEAD"), argsOnly = true)
    private Packet<?> xdolf$groundFlag(Packet<?> packet) {
        if (((Connection) (Object) this).getSending() != PacketFlow.SERVERBOUND || !Hooks.active("AntiHunger")
            || Minecraft.getInstance().player.fallDistance > 0 || !(packet instanceof ServerboundMovePlayerPacket move)) return packet;
        if (move.hasPosition() && move.hasRotation()) return new ServerboundMovePlayerPacket.PosRot(move.getX(0), move.getY(0), move.getZ(0), move.getYRot(0), move.getXRot(0), false, move.horizontalCollision());
        if (move.hasPosition()) return new ServerboundMovePlayerPacket.Pos(move.getX(0), move.getY(0), move.getZ(0), false, move.horizontalCollision());
        if (move.hasRotation()) return new ServerboundMovePlayerPacket.Rot(move.getYRot(0), move.getXRot(0), false, move.horizontalCollision());
        return new ServerboundMovePlayerPacket.StatusOnly(false, move.horizontalCollision());
    }
}
