package com.darkcart.xdolf.mixin;

import com.darkcart.xdolf.LegacyGuiFont;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Uses the original TTF glyph size and makes chat line backgrounds wide enough to contain it. */
@Mixin(GuiGraphics.class)
public abstract class GuiGraphicsMixin {
    @Unique private boolean xdolf$chatFillReentry;

    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfString(Font font,String text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!LegacyGuiFont.renderingChat())return;
        LegacyGuiFont.draw((GuiGraphics)(Object)this,text,x,y,color,shadow);
        ci.cancel();
    }

    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfSequence(Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!LegacyGuiFont.renderingChat())return;
        LegacyGuiFont.draw((GuiGraphics)(Object)this,text,x,y,color,shadow);
        ci.cancel();
    }

    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfComponent(Font font,Component text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!LegacyGuiFont.renderingChat())return;
        LegacyGuiFont.draw((GuiGraphics)(Object)this,text.getVisualOrderText(),x,y,color,shadow);
        ci.cancel();
    }

    @Inject(method="fill(IIIII)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$fitChatBackground(int x1,int y1,int x2,int y2,int color,CallbackInfo ci) {
        if(xdolf$chatFillReentry||!LegacyGuiFont.renderingChat()||x1>-3||x2<=0)return;
        int required=LegacyGuiFont.chatBackgroundWidth();
        if(required<=0)return;
        GuiGraphics graphics=(GuiGraphics)(Object)this;
        int desired=Math.min(graphics.guiWidth()+4,required+4);
        if(desired<=x2)return;
        xdolf$chatFillReentry=true;
        try {
            graphics.fill(x1,y1,desired,y2,color);
        } finally {
            xdolf$chatFillReentry=false;
        }
        ci.cancel();
    }
}
