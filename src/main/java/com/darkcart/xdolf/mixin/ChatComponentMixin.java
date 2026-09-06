package com.darkcart.xdolf.mixin;

import com.darkcart.xdolf.XdolfFont;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ChatComponent;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.util.FormattedCharSequence;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.Redirect;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import java.util.List;

/** Uses the Xdolf font while retaining Minecraft's chat history, scrolling and fade behaviour. */
@Mixin(ChatComponent.class)
public abstract class ChatComponentMixin {
    @Inject(method="render",at=@At("HEAD"))
    private void xdolf$beginTtfChat(GuiGraphics graphics,int tickCount,int mouseX,int mouseY,boolean focused,CallbackInfo ci) {
        XdolfFont.beginChat();
    }

    @Inject(method="render",at=@At("RETURN"))
    private void xdolf$endTtfChat(GuiGraphics graphics,int tickCount,int mouseX,int mouseY,boolean focused,CallbackInfo ci) {
        XdolfFont.endChat();
    }

    @Redirect(method="addMessageToDisplayQueue",at=@At(value="INVOKE",
        target="Lnet/minecraft/client/gui/components/ComponentRenderUtils;wrapComponents(Lnet/minecraft/network/chat/FormattedText;ILnet/minecraft/client/gui/Font;)Ljava/util/List;"))
    private List<FormattedCharSequence> xdolf$wrapWithTtfMetrics(FormattedText text,int width,Font font) {
        return XdolfFont.wrapChat(text,width,font);
    }
}
