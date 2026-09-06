package com.darkcart.xdolf;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;
import java.awt.Color;
import java.awt.Font;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import javax.imageio.ImageIO;

/** Original XFont's AWT font, glyph padding, quarter scale, advance and shadow. */
final class LegacyGuiFont {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("xdolf", "legacy_gui_font");
    private static final int SIZE = 2048;
    private static final int[] WIDTH = new int[2048], X = new int[2048], Y = new int[2048];
    private static boolean ready;
    private static int glyphHeight;
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
            Minecraft.getInstance().getTextureManager().register(TEXTURE, new DynamicTexture(() -> "Xdolf legacy GUI font", image));
            ready = true;
        } catch (java.io.IOException error) { throw new IllegalStateException("Cannot create Xdolf GUI font", error); }
    }
    private static final net.minecraft.client.renderer.RenderType WORLD_TEXT=net.minecraft.client.renderer.RenderType.text(TEXTURE);
    static void drawWorld(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,org.joml.Matrix4f transform,String text,float x,float y,int color) {
        init();worldLine(buffers,transform,text,x+1,y+1,0xFF0D0D0D,true);worldLine(buffers,transform,text,x,y,color,false);
    }
    private static void worldLine(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,org.joml.Matrix4f transform,String text,float x,float y,int color,boolean shadow) {
        var matrix=new org.joml.Matrix4f(transform).translate(x-1.5f,y,0).scale(0.25f,0.25f,1);
        var consumer=buffers.getBuffer(WORLD_TEXT);int offset=0,current=color;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='\u00a7'&&i+1<text.length()) {
                int code="0123456789abcdef".indexOf(text.charAt(++i));
                if(!shadow&&code>=0) {int v=(code>>3&1)*85;current=0xFF000000|((code>>2&1)*170+v+(code==6?85:0))<<16|((code>>1&1)*170+v)<<8|(code&1)*170+v;}
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
    static int width(String text) {
        init(); int width = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) { i++; continue; }
            if (c < WIDTH.length) width += WIDTH[c] - 8;
        }
        return width / 4;
    }
    static String trim(String text, int max) {
        int end = text.length();
        while (end > 0 && width(text.substring(0, end)) > max) end--;
        return text.substring(0, end);
    }
    static void draw(GuiGraphics g, String text, float x, float y, int color) {
        init(); drawLine(g, text, x + 1, y + 1, 0xFF0D0D0D, true);
        drawLine(g, text, x, y, color, false);
    }
    private static void drawLine(GuiGraphics g, String text, float x, float y, int color, boolean shadow) {
        g.pose().pushMatrix(); g.pose().translate(x - 1.5f, y); g.pose().scale(0.25f, 0.25f);
        int offset = 0, current = color;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '\u00a7' && i + 1 < text.length()) {
                int index = "0123456789abcdef".indexOf(text.charAt(++i));
                if (!shadow && index >= 0) {
                    int v = (index >> 3 & 1) * 85;
                    int r = (index >> 2 & 1) * 170 + v + (index == 6 ? 85 : 0);
                    current = 0xFF000000 | r << 16 | ((index >> 1 & 1) * 170 + v) << 8 | (index & 1) * 170 + v;
                }
                continue;
            }
            if (c >= WIDTH.length) continue;
            g.blit(RenderPipelines.GUI_TEXTURED, TEXTURE, offset, 0, X[c], Y[c], WIDTH[c], glyphHeight, SIZE, SIZE, current);
            offset += WIDTH[c] - 8;
        }
        g.pose().popMatrix();
    }
}
