package com.darkcart.xdolf.mixin;

import com.darkcart.xdolf.LegacyGuiFont;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keep vanilla chat layout/scroll/fade, replacing only its glyph renderer. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void xdolf$beginTtfChat(GuiGraphics graphics,int tickCount,int mouseX,int mouseY,boolean focused,CallbackInfo ci) {
        LegacyGuiFont.beginChat();
    }

    @Inject(method="render",at=@At("RETURN"))
    private void xdolf$endTtfChat(GuiGraphics graphics,int tickCount,int mouseX,int mouseY,boolean focused,CallbackInfo ci) {
        LegacyGuiFont.endChat();
    }
}
