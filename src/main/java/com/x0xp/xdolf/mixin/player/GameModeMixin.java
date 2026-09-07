package com.x0xp.xdolf.mixin;

import com.x0xp.xdolf.Hooks;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.MultiPlayerGameMode;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.network.protocol.game.ServerboundMovePlayerPacket;

@Mixin(MultiPlayerGameMode.class)
public abstract class GameModeMixin {
    @Shadow private int destroyDelay;
    @Shadow private float destroyProgress;
    @Shadow private boolean isDestroying;
    @Shadow private BlockPos destroyBlockPos;
    @Unique private String xdolf$destroyedBlockName;

    @Inject(method = "destroyBlock", at = @At("HEAD"))
    private void xdolf$captureDestroyedBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        var mc = Minecraft.getInstance();
        xdolf$destroyedBlockName = mc.level == null ? "block"
            : mc.level.getBlockState(pos).getBlock().getName().getString();
    }

    @Inject(method = "destroyBlock", at = @At("RETURN"))
    private void xdolf$announceDestroyedBlock(BlockPos pos, CallbackInfoReturnable<Boolean> cir) {
        if (cir.getReturnValue()) Hooks.announcerBlockBroken(xdolf$destroyedBlockName);
        xdolf$destroyedBlockName = null;
    }
    @Inject(method = "continueDestroyBlock", at = @At("HEAD"))
    private void xdolf$speedmine(BlockPos pos, Direction direction, CallbackInfoReturnable<Boolean> cir) {
        if (!Hooks.active("Speedmine")) return;
        destroyDelay = 0;
        var mc = Minecraft.getInstance();
        if (isDestroying && pos.equals(destroyBlockPos)) {
            destroyProgress = Math.max(destroyProgress, (float)Hooks.setting("Speedmine","progress",0.4));
            float step = mc.level.getBlockState(pos).getDestroyProgress(mc.player, mc.level, pos);
            destroyProgress += step * (float) (Hooks.setting("Speedmine", "multiplier", 2) - 1);
        }
    }
    @Inject(method = "attack", at = @At("HEAD"))
    private void xdolf$critical(Player player, Entity target, CallbackInfo ci) {
        Hooks.announcerEntityAttacked();
        if (!Hooks.active("Criticals") || Hooks.active("NoFall") || !(target instanceof LivingEntity)
            || !player.onGround() || player.isInWater() || player.isInLava() || player.isPassenger()) return;
        var connection = Minecraft.getInstance().player.connection;
        for (double offset : new double[] {0.0625, 0, 0.00001, 0})
            connection.send(new ServerboundMovePlayerPacket.Pos(player.getX(), player.getY() + offset, player.getZ(), false, player.horizontalCollision));
    }
}
