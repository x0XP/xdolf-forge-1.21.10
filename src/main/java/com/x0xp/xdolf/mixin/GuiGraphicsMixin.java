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
    /**
     * Vanilla positions its nine-pixel font lower inside a chat row than Xdolf's taller TTF wants.
     * Keep the TTF at its native size and move only chat-rendered glyphs up two GUI pixels so the
     * descenders and one-pixel shadow remain inside the translucent row/input background.
     */
    private static final int XDOLF_CHAT_Y_OFFSET=-2;

    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Ljava/lang/String;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfString(Font font,String text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!XdolfFont.renderingChat())return;
        XdolfFont.draw((GuiGraphics)(Object)this,text,x,y+XDOLF_CHAT_Y_OFFSET,color,shadow);
        ci.cancel();
    }

    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/util/FormattedCharSequence;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfSequence(Font font,FormattedCharSequence text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!XdolfFont.renderingChat())return;
        XdolfFont.draw((GuiGraphics)(Object)this,text,x,y+XDOLF_CHAT_Y_OFFSET,color,shadow);
        ci.cancel();
    }

    @Inject(method="drawString(Lnet/minecraft/client/gui/Font;Lnet/minecraft/network/chat/Component;IIIZ)V",at=@At("HEAD"),cancellable=true)
    private void xdolf$ttfComponent(Font font,Component text,int x,int y,int color,boolean shadow,CallbackInfo ci) {
        if(!XdolfFont.renderingChat())return;
        XdolfFont.draw((GuiGraphics)(Object)this,text.getVisualOrderText(),x,y+XDOLF_CHAT_Y_OFFSET,color,shadow);
        ci.cancel();
    }
}
