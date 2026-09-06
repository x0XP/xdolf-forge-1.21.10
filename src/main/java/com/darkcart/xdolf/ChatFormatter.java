package com.darkcart.xdolf;

import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.network.chat.Style;

/** Formats Xdolf's chat prefix and Minecraft section-code colours. */
final class ChatFormatter {
    private ChatFormatter() {}

    static Component prefixed(String text) {
        return Component.literal("[")
            .append(Component.literal("Xdolf").withStyle(ChatFormatting.RED))
            .append(Component.literal("] "))
            .append(parse(text));
    }

    static Component parse(String text) {
        MutableComponent result = Component.empty();
        Style style = Style.EMPTY;
        int start = 0;
        for (int i = 0; i + 1 < text.length(); i++) {
            if (text.charAt(i) != '\u00a7') continue;
            if (i > start) result.append(Component.literal(text.substring(start, i)).setStyle(style));
            ChatFormatting formatting = ChatFormatting.getByCode(text.charAt(i + 1));
            if (formatting != null) style = formatting == ChatFormatting.RESET ? Style.EMPTY : style.applyFormat(formatting);
            i++;
            start = i + 1;
        }
        if (start < text.length()) result.append(Component.literal(text.substring(start)).setStyle(style));
        return result;
    }

    static String colourArguments(String syntax) {
        return syntax.replace("<", "<\u00a7a").replace(">", "\u00a7f>");
    }
}
