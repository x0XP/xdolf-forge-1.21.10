package com.darkcart.xdolf.mixin;

import com.darkcart.xdolf.LegacyGuiFont;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces chat glyph submission while preserving the widths Minecraft used for layout/wrapping. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfString(Font font,String text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!LegacyGuiFont.renderingChat())return;
        LegacyGuiFont.drawFitted((GuiGraphics)(Object)this,text,x,y,color,shadow,font.width(text));
        ci.cancel();
    }

    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfSequence(Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!LegacyGuiFont.renderingChat())return;
        LegacyGuiFont.drawFitted((GuiGraphics)(Object)this,text,x,y,color,shadow,font.width(text));
        ci.cancel();
    }

    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfComponent(Font font,Component text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!LegacyGuiFont.renderingChat())return;
        var visual=text.getVisualOrderText();
        LegacyGuiFont.drawFitted((GuiGraphics)(Object)this,visual,x,y,color,shadow,font.width(visual));
        ci.cancel();
    }
}
