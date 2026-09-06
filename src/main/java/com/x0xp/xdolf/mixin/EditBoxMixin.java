package com.x0xp.xdolf.mixin;

import com.x0xp.xdolf.XdolfFont;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

/** Keeps chat input caret/selection geometry on the same metrics as Xdolf's TTF glyphs. */
@Mixin(EditBox.class)
public abstract class EditBoxMixin {
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
