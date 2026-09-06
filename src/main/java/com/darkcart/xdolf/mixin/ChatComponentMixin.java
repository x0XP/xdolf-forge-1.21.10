package com.darkcart.xdolf.mixin;

import com.darkcart.xdolf.LegacyGuiFont;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

import java.util.List;

/** Keep vanilla chat scroll/fade behaviour while sizing its background for the Xdolf TTF glyphs. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow public abstract int getLinesPerPage();

    @Inject(method="render",at=@At("HEAD"))
    private void xdolf$beginTtfChat(GuiGraphics graphics,int tickCount,int mouseX,int mouseY,boolean focused,CallbackInfo ci) {
        int requiredWidth=0;
        int start=Math.max(0,chatScrollbarPos);
        int end=Math.min(trimmedMessages.size(),start+Math.max(1,getLinesPerPage()));
        for(int i=start;i<end;i++) requiredWidth=Math.max(requiredWidth,LegacyGuiFont.width(trimmedMessages.get(i).content()));
        LegacyGuiFont.beginChat(requiredWidth);
    }

    @Inject(method="render",at=@At("RETURN"))
    private void xdolf$endTtfChat(GuiGraphics graphics,int tickCount,int mouseX,int mouseY,boolean focused,CallbackInfo ci) {
        LegacyGuiFont.endChat();
    }
}
