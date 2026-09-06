package com.darkcart.xdolf.mixin;

import com.darkcart.xdolf.LegacyGuiFont;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Applies the same TTF renderer to the input field, suggestions and chat messages while chat is open. */
@Mixin(ChatScreen.class)
public abstract class ChatScreenMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void xdolf$beginTtfChatScreen(GuiGraphics graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci) {
        LegacyGuiFont.beginChat();
    }

    @Inject(method="render",at=@At("RETURN"))
    private void xdolf$endTtfChatScreen(GuiGraphics graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci) {
        LegacyGuiFont.endChat();
    }
}
