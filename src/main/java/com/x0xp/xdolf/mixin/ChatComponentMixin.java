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
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;
import java.util.ArrayList;
import java.util.List;

/** Uses the Xdolf font while retaining Minecraft's chat history, scrolling and fade behaviour. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Shadow @Final private List<GuiMessage.Line> trimmedMessages;
    @Shadow private int chatScrollbarPos;
    @Shadow public abstract int getLinesPerPage();

    @Unique private List<Integer> xdolf$visibleLineWidths=List.of();
    @Unique private int xdolf$backgroundLine;

    @Inject(method="render",at=@At("HEAD"))
    private void xdolf$beginTtfChat(GuiGraphics graphics,int tickCount,int mouseX,int mouseY,boolean focused,CallbackInfo ci) {
        XdolfFont.beginChat();
        int first=Math.max(0,chatScrollbarPos);
        int last=Math.min(trimmedMessages.size(),first+Math.max(0,getLinesPerPage()));
        var widths=new ArrayList<Integer>(Math.max(0,last-first));
        for(int i=first;i<last;i++) {
            GuiMessage.Line line=trimmedMessages.get(i);
            if(focused||tickCount-line.addedTime()<200)widths.add(XdolfFont.width(line.content()));
        }
        xdolf$visibleLineWidths=List.copyOf(widths);
        xdolf$backgroundLine=0;
    }

    @Inject(method="render",at=@At("RETURN"))
    private void xdolf$endTtfChat(GuiGraphics graphics,int tickCount,int mouseX,int mouseY,boolean focused,CallbackInfo ci) {
        XdolfFont.endChat();
        xdolf$visibleLineWidths=List.of();
        xdolf$backgroundLine=0;
    }

    /** Xdolf's TTF plus its shadow needs slightly more room than the vanilla nine-pixel row. */
    @Inject(method="getLineHeight",at=@At("RETURN"),cancellable=true)
    private void xdolf$ttfLineHeight(CallbackInfoReturnable<Integer> cir) {
        cir.setReturnValue(Math.max(cir.getReturnValue(),14));
    }

    /**
     * Fit each message background to its own TTF width. Current Minecraft does not guarantee the
     * old -4 left coordinate for every chat configuration, so identify message-row fills by their
     * short height and left-edge placement instead of one exact vanilla coordinate.
     */
    @Redirect(method="render",at=@At(value="INVOKE",target="Lnet/minecraft/client/gui/GuiGraphics;fill(IIIII)V"))
    private void xdolf$fitMessageBackground(GuiGraphics graphics,int left,int top,int right,int bottom,int color) {
        int rowHeight=bottom-top;
        if(left<=0&&right>left&&rowHeight>0&&rowHeight<=18&&xdolf$backgroundLine<xdolf$visibleLineWidths.size()) {
            int textWidth=xdolf$visibleLineWidths.get(xdolf$backgroundLine++);
            right=Math.min(right,left+textWidth+9);
        }
        graphics.fill(left,top,right,bottom,color);
    }

    @Redirect(method="addMessageToDisplayQueue",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;"))
    private List<FormattedCharSequence> xdolf$wrapWithTtfMetrics(FormattedText text,int width,Font font) {
        return XdolfFont.wrapChat(text,width,font);
    }
}
