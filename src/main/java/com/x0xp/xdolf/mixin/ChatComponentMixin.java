package com.x0xp.xdolf.mixin;

import com.x0xp.xdolf.XdolfFont;
import net.minecraft.client.GuiMessage;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

/** Uses the Xdolf font while retaining Minecraft's chat history, scrolling and fade behaviour. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow public abstract int getLinesPerPage();
    @Unique private int xdolf$visibleTextWidth;

    @Inject(method="render",at=@At("HEAD"))
    private void xdolf$beginTtfChat(GuiGraphics graphics,int tickCount,int mouseX,int mouseY,boolean focused,CallbackInfo ci) {
        XdolfFont.beginChat();
        int first=Math.max(0,chatScrollbarPos);
        int last=Math.min(trimmedMessages.size(),first+Math.max(0,getLinesPerPage()));
        int widest=0;
        for(int i=first;i<last;i++)widest=Math.max(widest,XdolfFont.width(trimmedMessages.get(i).content()));
        xdolf$visibleTextWidth=widest;
    }

    @Inject(method="render",at=@At("RETURN"))
    private void xdolf$endTtfChat(GuiGraphics graphics,int tickCount,int mouseX,int mouseY,boolean focused,CallbackInfo ci) {
        XdolfFont.endChat();
        xdolf$visibleTextWidth=0;
    }

    /**
     * Vanilla fills each message row to the configured maximum chat width. With Xdolf's narrower
     * TTF advances that leaves a large empty rectangle. Only the actual message-row fill starts at
     * x=-4 and extends right of zero; tag indicators, queue UI and scrollbar use different bounds.
     */
    @Redirect(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void xdolf$fitMessageBackground(GuiGraphics graphics,int left,int top,int right,int bottom,int color) {
        if(left==-4&&right>0&&xdolf$visibleTextWidth>0)right=Math.min(right,xdolf$visibleTextWidth+8);
        graphics.fill(left,top,right,bottom,color);
    }

    @Redirect(method="addMessageToDisplayQueue",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;"))
    private List<FormattedCharSequence> xdolf$wrapWithTtfMetrics(FormattedText text,int width,Font font) {
        return XdolfFont.wrapChat(text,width,font);
    }
}
