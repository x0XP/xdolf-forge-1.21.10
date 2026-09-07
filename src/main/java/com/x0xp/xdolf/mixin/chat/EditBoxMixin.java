package com.x0xp.xdolf.mixin.chat;

import com.x0xp.xdolf.chat.ChatTextContext;
import com.x0xp.xdolf.ui.XdolfFont;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

/** Keeps chat input text, caret and selection geometry on the same metrics as Xdolf's TTF. */
@Mixin(EditBox.class)
public abstract class EditBoxMixin {
    @Unique private boolean xdolf$pushedChatOffset;

    @Inject(method="renderWidget",at=@At("HEAD"))
    private void xdolf$centerChatInput(GuiGraphics graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci) {
        xdolf$pushedChatOffset=false;
        if(!XdolfFont.renderingChat())return;

        EditBox box=(EditBox)(Object)this;
        int vanillaTextY=box.isBordered()?box.getY()+(box.getHeight()-8)/2:box.getY();
        ChatTextContext.push(XdolfFont.centeredYOffset(box.getY(),box.getHeight(),vanillaTextY));
        xdolf$pushedChatOffset=true;
    }

    @Inject(method="renderWidget",at=@At("RETURN"))
    private void xdolf$finishChatInput(GuiGraphics graphics,int mouseX,int mouseY,float partialTick,CallbackInfo ci) {
        if(xdolf$pushedChatOffset) {
            ChatTextContext.pop();
            xdolf$pushedChatOffset=false;
        }
    }

    @Redirect(
        method="renderWidget",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;width(Lnet/minecraft/util/FormattedCharSequence;)I")
    )
    private int xdolf$ttfCursorWidth(Font font,FormattedCharSequence text) {
        if(!XdolfFont.renderingChat())return font.width(text);
        // EditBox adds one vanilla pixel after this width before drawing an end-of-line underscore.
        // Subtract it here so the blinking caret begins directly after the final TTF glyph.
        return Math.max(0,XdolfFont.width(text)-1);
    }

    @Redirect(
        method="renderWidget",
        at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/Font;width(Ljava/lang/String;)I")
    )
    private int xdolf$ttfSelectionWidth(Font font,String text) {
        return XdolfFont.renderingChat()?XdolfFont.width(text):font.width(text);
    }
}
