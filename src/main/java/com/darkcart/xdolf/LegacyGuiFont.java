package com.darkcart.xdolf;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/** Original XFont's AWT/TTF font, glyph padding, quarter scale, advance and shadow. */
public final class LegacyGuiFont {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("xdolf", "legacy_gui_font");
    private static final int SIZE = 2048;
    private static final int[] WIDTH = new int[2048], X = new int[2048], Y = new int[2048];
    private static final char[] LEGACY_CODES = "0123456789abcdef".toCharArray();
    private static final int[] LEGACY_RGB = {
        0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,
        0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF
    };
    private static boolean ready;
    private static int glyphHeight;
    private static int chatDepth;

    private LegacyGuiFont() {}

    private static void init() {
        if (ready) return;
        try {
            var atlas = new BufferedImage(SIZE, SIZE, BufferedImage.TYPE_INT_ARGB);
            var graphics = atlas.createGraphics();
            graphics.setFont(new Font("Roboto", Font.PLAIN, 36));
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setColor(Color.WHITE);
            var metrics = graphics.getFontMetrics();
            glyphHeight = metrics.getHeight() + 3;
            int x = 0, y = 0;
            for (int i = 0; i < WIDTH.length; i++) {
                WIDTH[i] = Math.max(7, metrics.charWidth((char)i) + 8);
                if (x + WIDTH[i] >= SIZE) { x = 0; y += glyphHeight; }
                X[i] = x; Y[i] = y;
                graphics.drawString(String.valueOf((char)i), x + 3, y + 1 + metrics.getAscent());
                x += WIDTH[i];
            }
            graphics.dispose();
            var bytes = new ByteArrayOutputStream();
            ImageIO.write(atlas, "png", bytes);
            var image = NativeImage.read(new ByteArrayInputStream(bytes.toByteArray()));
            Minecraft.getInstance().getTextureManager().register(TEXTURE, new DynamicTexture(() -> "Xdolf legacy TTF font", image));
            ready = true;
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot create Xdolf TTF font", error); }
    }

    /** ChatComponent/ChatScreen bracket their render with these so every chat glyph uses this font. */
    public static void beginChat() { chatDepth++; }
    public static void endChat() { if (chatDepth > 0) chatDepth--; }
    public static boolean renderingChat() { return chatDepth > 0; }

    private static final net.minecraft.client.renderer.RenderType WORLD_TEXT=net.minecraft.client.renderer.RenderType.text(TEXTURE);
    public static void drawWorld(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,org.joml.Matrix4f transform,String text,float x,float y,int color) {
        init();
        color=opaqueIfNeeded(color);
        // World nametag matrices use an inverted GUI Y axis, so -1 is visually down for the shadow.
        worldLine(buffers,transform,text,x+1,y-1,(color&0xFF000000)|0x000D0D0D,true);
        worldLine(buffers,transform,text,x,y,color,false);
    }
    private static void worldLine(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,org.joml.Matrix4f transform,String text,float x,float y,int color,boolean shadow) {
        var matrix=new org.joml.Matrix4f(transform).translate(x-1.5f,y,shadow?0.001f:0).scale(0.25f,0.25f,1);
        var consumer=buffers.getBuffer(WORLD_TEXT);int offset=0,current=color;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='\u00a7'&&i+1<text.length()) {
                char formatting=Character.toLowerCase(text.charAt(++i));
                int code="0123456789abcdef".indexOf(formatting);
                if(!shadow&&code>=0)current=(color&0xFF000000)|LEGACY_RGB[code];
                else if(!shadow&&formatting=='r')current=color;
                continue;
            }
            if(c>=WIDTH.length)continue;
            float u=X[c]/(float)SIZE,v=Y[c]/(float)SIZE,right=(X[c]+WIDTH[c])/(float)SIZE,bottom=(Y[c]+glyphHeight)/(float)SIZE;
            consumer.addVertex(matrix,offset,glyphHeight,0).setColor(current).setUv(u,bottom).setUv2(240,240);
            consumer.addVertex(matrix,offset+WIDTH[c],glyphHeight,0).setColor(current).setUv(right,bottom).setUv2(240,240);
            consumer.addVertex(matrix,offset+WIDTH[c],0,0).setColor(current).setUv(right,v).setUv2(240,240);
            consumer.addVertex(matrix,offset,0,0).setColor(current).setUv(u,v).setUv2(240,240);
            offset+=WIDTH[c]-8;
        }
    }
    public static int width(String text) {
        init(); int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) { i++; continue; }
            if (c < WIDTH.length) width += WIDTH[c] - 8;
        }
        return width / 4;
    }
    public static String trim(String text, int max) {
        int end = text.length();
        while (end > 0 && width(text.substring(0, end)) > max) end--;
        return text.substring(0, end);
    }

    public static void draw(GuiGraphics g,String text,float x,float y,int color) { draw(g,text,x,y,color,true); }
    public static void draw(GuiGraphics g,String text,float x,float y,int color,boolean shadow) {
        init();
        color=opaqueIfNeeded(color);
        if(shadow)drawLine(g,text,x+1,y+1,(color&0xFF000000)|0x000D0D0D,true);
        drawLine(g,text,x,y,color,false);
    }
    public static void draw(GuiGraphics g,FormattedCharSequence sequence,float x,float y,int color) { draw(g,toLegacy(sequence),x,y,color,true); }
    public static void draw(GuiGraphics g,FormattedCharSequence sequence,float x,float y,int color,boolean shadow) { draw(g,toLegacy(sequence),x,y,color,shadow); }

    /**
     * Chat still wraps, scrolls and positions its cursor with Minecraft's Font metrics. Fit each
     * TTF draw to the exact width Minecraft allocated so the custom glyphs never drift through the
     * background, cursor, wrapped line boundary or neighbouring chat element.
     */
    public static void drawFitted(GuiGraphics g,String text,float x,float y,int color,boolean shadow,int targetWidth) {
        int natural=Math.max(1,width(text));
        float fit=targetWidth>0?targetWidth/(float)natural:1f;
        g.pose().pushMatrix();
        g.pose().translate(x,y);
        g.pose().scale(fit,1f);
        draw(g,text,0,0,color,shadow);
        g.pose().popMatrix();
    }
    public static void drawFitted(GuiGraphics g,FormattedCharSequence sequence,float x,float y,int color,boolean shadow,int targetWidth) {
        drawFitted(g,toLegacy(sequence),x,y,color,shadow,targetWidth);
    }

    private static void drawLine(GuiGraphics g, String text, float x, float y, int color, boolean shadow) {
        g.pose().pushMatrix(); g.pose().translate(x - 1.5f, y); g.pose().scale(0.25f, 0.25f);
        int offset = 0, current = color;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                char formatting=Character.toLowerCase(text.charAt(++i));
                int index = "0123456789abcdef".indexOf(formatting);
                if (!shadow && index >= 0) current=(color&0xFF000000)|LEGACY_RGB[index];
                else if(!shadow&&formatting=='r')current=color;
                continue;
            }
            if (c >= WIDTH.length) continue;
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, offset, 0, X[c], Y[c], WIDTH[c], glyphHeight, SIZE, SIZE, current);
            offset += WIDTH[c] - 8;
        }
        g.pose().popMatrix();
    }
    private static String toLegacy(FormattedCharSequence sequence) {
        var out=new StringBuilder();
        final int[] last={-2};
        sequence.accept((index,style,codePoint)->{
            int next=style.getColor()==null?-1:nearestLegacy(style.getColor().getValue());
            if(next!=last[0]) {
                out.append('\u00a7').append(next<0?'f':LEGACY_CODES[next]);
                last[0]=next;
            }
            if(Character.isBmpCodePoint(codePoint))out.append((char)codePoint);
            else out.appendCodePoint(codePoint);
            return true;
        });
        return out.toString();
    }
    private static int nearestLegacy(int rgb) {
        rgb&=0xFFFFFF;int r=rgb>>16&255,g=rgb>>8&255,b=rgb&255,best=0,bestDistance=Integer.MAX_VALUE;
        for(int i=0;i<LEGACY_RGB.length;i++) {
            int c=LEGACY_RGB[i],dr=r-(c>>16&255),dg=g-(c>>8&255),db=b-(c&255),d=dr*dr+dg*dg+db*db;
            if(d<bestDistance){bestDistance=d;best=i;}
        }
        return best;
    }
    private static int opaqueIfNeeded(int color) { return (color>>>24)==0 ? color|0xFF000000 : color; }
}
