package com.x0xp.xdolf.mixin;

import com.x0xp.xdolf.XdolfFont;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the Xdolf TTF renderer to chat input, suggestions and messages. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void xdolf$beginTtfChatScreen(GuiGraphics graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci) {
        XdolfFont.beginChat();
    }

    @Inject(method="render",at=@At("RETURN"))
    private void xdolf$endTtfChatScreen(GuiGraphics graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci) {
        XdolfFont.endChat();
    }
}
