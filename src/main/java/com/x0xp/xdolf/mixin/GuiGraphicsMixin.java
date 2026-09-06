package com.x0xp.xdolf.mixin;

import com.x0xp.xdolf.XdolfFont;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Replaces chat glyph submission while leaving Minecraft's chat layout lifecycle intact. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfString(Font font,String text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!XdolfFont.renderingChat())return;
        XdolfFont.draw((GuiGraphics)(Object)this,text,x,y+ChatTextContext.yOffset(),color,shadow);
        ci.cancel();
    }

    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfSequence(Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!XdolfFont.renderingChat())return;
        XdolfFont.draw((GuiGraphics)(Object)this,text,x,y+ChatTextContext.yOffset(),color,shadow);
        ci.cancel();
    }

    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfComponent(Font font,Component text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!XdolfFont.renderingChat())return;
        XdolfFont.draw((GuiGraphics)(Object)this,text.getVisualOrderText(),x,y+ChatTextContext.yOffset(),color,shadow);
        ci.cancel();
    }
}
