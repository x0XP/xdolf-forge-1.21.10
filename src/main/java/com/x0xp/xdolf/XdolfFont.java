package com.x0xp.xdolf;

import com.mojang.blaze3d.platform.NativeImage;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.List;
import javax.imageio.ImageIO;

/** Xdolf's Roboto/AWT font renderer used by the GUI, HUD, chat and world labels. */
public final class XdolfFont {
    private static final ResourceLocation TEXTURE = ResourceLocation.fromNamespaceAndPath("xdolf", "client_font");
    private static final int SIZE = 2048;
    private static final int BASE_FONT_SIZE = 9;
    private static final int MAX_RASTER_SCALE = 4;
    private static final int[] WIDTH = new int[2048], X = new int[2048], Y = new int[2048];
    private static final char[] FORMAT_CODES = "0123456789abcdef".toCharArray();
    private static final int[] CHAT_RGB = {
        0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,
        0x555555,0x5555FF,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF
    };
    private static boolean ready;
    private static int rasterScale;
    private static int glyphHeight;
    private static int cellPadding;
    private static int visibleTop;
    private static int visibleBottom;
    private static int chatDepth;

    private XdolfFont() {}

    /**
     * Build the atlas at roughly one source texel per framebuffer pixel. The old renderer always
     * rasterised Roboto at 36px and then scaled the atlas by 0.25, which only maps cleanly when the
     * Minecraft GUI scale happens to be 4. At GUI scale 1/2 that made several antialiased source
     * texels collapse into the same physical pixel and produced the broken/thin text seen at low
     * resolutions. Rebuilding when GUI scale changes keeps sampling stable without changing the
     * font's apparent GUI-space size.
     */
    private static void init() {
        int desiredScale=currentRasterScale();
        if(ready&&rasterScale==desiredScale)return;
        rebuild(desiredScale);
    }

    private static int currentRasterScale() {
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft==null||minecraft.getWindow()==null)return 1;
        int scale=(int)Math.round(minecraft.getWindow().getGuiScale());
        return Math.max(1,Math.min(MAX_RASTER_SCALE,scale));
    }

    private static void rebuild(int scale) {
        try {
            rasterScale=scale;
            int fontSize=BASE_FONT_SIZE*rasterScale;
            cellPadding=Math.max(2,2*rasterScale);
            int leftPadding=Math.max(1,Math.round(0.75f*rasterScale));
            int topPadding=Math.max(0,Math.round(0.25f*rasterScale));
            int extraHeight=Math.max(1,Math.round(0.75f*rasterScale));

            var atlas=new BufferedImage(SIZE,SIZE,BufferedImage.TYPE_INT_ARGB);
            var graphics=atlas.createGraphics();
            graphics.setFont(new java.awt.Font("Roboto",java.awt.Font.PLAIN,fontSize));
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            graphics.setColor(Color.WHITE);
            var metrics=graphics.getFontMetrics();
            glyphHeight=metrics.getHeight()+extraHeight;
            int x=0,y=0;
            for(int i=0;i<WIDTH.length;i++) {
                WIDTH[i]=Math.max(rasterScale+1,metrics.charWidth((char)i)+cellPadding);
                if(x+WIDTH[i]>=SIZE) { x=0; y+=glyphHeight; }
                if(y+glyphHeight>=SIZE)throw new IllegalStateException("Xdolf TTF atlas overflow at GUI scale "+rasterScale);
                X[i]=x;
                Y[i]=y;
                graphics.drawString(String.valueOf((char)i),x+leftPadding,y+topPadding+metrics.getAscent());
                x+=WIDTH[i];
            }
            graphics.dispose();
            measureVisibleAsciiBounds(atlas);

            var bytes=new ByteArrayOutputStream();
            ImageIO.write(atlas,"png",bytes);
            var image=NativeImage.read(new ByteArrayInputStream(bytes.toByteArray()));
            var texture=new DynamicTexture(() -> "Xdolf TTF font",image);
            texture.setFilter(false,false);
            Minecraft.getInstance().getTextureManager().register(TEXTURE,texture);
            ready=true;
        } catch(java.io.IOException error) {
            throw new IllegalStateException("Cannot create Xdolf TTF font",error);
        }
    }

    /** Measure actual non-transparent glyph pixels rather than centring the padded atlas cell. */
    private static void measureVisibleAsciiBounds(BufferedImage atlas) {
        int top=glyphHeight;
        int bottom=0;
        for(int c=33;c<=126;c++) {
            int cellX=X[c],cellY=Y[c],cellWidth=WIDTH[c];
            for(int yy=0;yy<glyphHeight;yy++) {
                for(int xx=0;xx<cellWidth;xx++) {
                    if((atlas.getRGB(cellX+xx,cellY+yy)>>>24)!=0) {
                        top=Math.min(top,yy);
                        bottom=Math.max(bottom,yy+1);
                        break;
                    }
                }
            }
        }
        if(bottom<=top) {
            visibleTop=0;
            visibleBottom=glyphHeight;
        } else {
            visibleTop=top;
            visibleBottom=bottom;
        }
    }

    public static void beginChat() { chatDepth++; }
    public static void endChat() { if(chatDepth>0)chatDepth--; }
    public static boolean renderingChat() { return chatDepth>0; }

    /** Native GUI-space height of one Xdolf TTF atlas row. */
    public static int lineHeight() {
        init();
        return Math.max(10,ceilDiv(glyphHeight,rasterScale));
    }

    /**
     * Returns the Y correction required to visually centre Xdolf's actual glyph ink (plus its
     * one-pixel drop shadow) inside a container whose vanilla text origin is already known.
     */
    public static int centeredYOffset(int containerTop,int containerHeight,int vanillaTextY) {
        init();
        float scale=1.0f/rasterScale;
        float visibleTopGui=visibleTop*scale;
        float visibleBottomGui=visibleBottom*scale+1.0f;
        float visibleHeightGui=visibleBottomGui-visibleTopGui;
        float desiredVisibleTop=containerTop+(containerHeight-visibleHeightGui)*0.5f;
        float desiredCellTop=desiredVisibleTop-visibleTopGui;
        return Math.round(desiredCellTop-vanillaTextY);
    }

    private static final net.minecraft.client.renderer.RenderType WORLD_TEXT=net.minecraft.client.renderer.RenderType.text(TEXTURE);

    public static void drawWorld(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,
                                 org.joml.Matrix4f transform,String text,float x,float y,int color) {
        init();
        color=opaqueIfNeeded(color);
        worldLine(buffers,transform,text,x+1,y-1,(color&0xFF000000)|0x000D0D0D,true);
        worldLine(buffers,transform,text,x,y,color,false);
    }

    private static void worldLine(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,
                                  org.joml.Matrix4f transform,String text,float x,float y,int color,boolean shadow) {
        float scale=1.0f/rasterScale;
        var matrix=new org.joml.Matrix4f(transform).translate(x-1.5f,y,shadow?0.001f:0).scale(scale,scale,1);
        var consumer=buffers.getBuffer(WORLD_TEXT);
        int offset=0,current=color;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='\u00a7'&&i+1<text.length()) {
                char formatting=Character.toLowerCase(text.charAt(++i));
                int code="0123456789abcdef".indexOf(formatting);
                if(!shadow&&code>=0)current=(color&0xFF000000)|CHAT_RGB[code];
                else if(!shadow&&formatting=='r')current=color;
                continue;
            }
            if(c>=WIDTH.length)continue;
            float u=X[c]/(float)SIZE,v=Y[c]/(float)SIZE,right=(X[c]+WIDTH[c])/(float)SIZE,bottom=(Y[c]+glyphHeight)/(float)SIZE;
            consumer.addVertex(matrix,offset,glyphHeight,0).setColor(current).setUv(u,bottom).setUv2(240,240);
            consumer.addVertex(matrix,offset+WIDTH[c],glyphHeight,0).setColor(current).setUv(right,bottom).setUv2(240,240);
            consumer.addVertex(matrix,offset+WIDTH[c],0,0).setColor(current).setUv(right,v).setUv2(240,240);
            consumer.addVertex(matrix,offset,0,0).setColor(current).setUv(u,v).setUv2(240,240);
            offset+=WIDTH[c]-cellPadding;
        }
    }

    public static int width(String text) {
        init();
        int width=0;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='\u00a7'&&i+1<text.length()){i++;continue;}
            if(c<WIDTH.length)width+=WIDTH[c]-cellPadding;
        }
        return Math.round(width/(float)rasterScale);
    }

    public static int width(FormattedCharSequence sequence) { return width(toFormattedString(sequence)); }

    public static String trim(String text,int max) {
        int end=text.length();
        while(end>0&&width(text.substring(0,end))>max)end--;
        return text.substring(0,end);
    }

    /**
     * Minecraft owns chat scrolling/fading and line placement; this chooses the vanilla wrap width
     * whose resulting lines fit the Xdolf font's real advances inside the configured chat width.
     */
    public static List<FormattedCharSequence> wrapChat(FormattedText text,int visualWidth,Font vanillaFont) {
        if(visualWidth<=1)return ComponentRenderUtils.wrapComponents(text,Math.max(1,visualWidth),vanillaFont);
        int low=1,high=visualWidth,best=1;
        List<FormattedCharSequence> bestLines=ComponentRenderUtils.wrapComponents(text,best,vanillaFont);
        while(low<=high) {
            int candidate=(low+high)>>>1;
            List<FormattedCharSequence> lines=ComponentRenderUtils.wrapComponents(text,candidate,vanillaFont);
            boolean fits=true;
            for(var line:lines)if(width(line)>visualWidth){fits=false;break;}
            if(fits){best=candidate;bestLines=lines;low=candidate+1;}
            else high=candidate-1;
        }
        return bestLines;
    }

    public static void draw(GuiGraphics g,String text,float x,float y,int color) { draw(g,text,x,y,color,true); }
    public static void draw(GuiGraphics g,String text,float x,float y,int color,boolean shadow) {
        init();
        color=opaqueIfNeeded(color);
        if(shadow)drawLine(g,text,x+1,y+1,(color&0xFF000000)|0x000D0D0D,true);
        drawLine(g,text,x,y,color,false);
    }
    public static void draw(GuiGraphics g,FormattedCharSequence sequence,float x,float y,int color) { draw(g,toFormattedString(sequence),x,y,color,true); }
    public static void draw(GuiGraphics g,FormattedCharSequence sequence,float x,float y,int color,boolean shadow) { draw(g,toFormattedString(sequence),x,y,color,shadow); }

    private static void drawLine(GuiGraphics g,String text,float x,float y,int color,boolean shadow) {
        float scale=1.0f/rasterScale;
        float snappedX=snapToFramebuffer(x-1.5f);
        float snappedY=snapToFramebuffer(y);
        g.pose().pushMatrix();
        g.pose().translate(snappedX,snappedY);
        g.pose().scale(scale,scale);
        int offset=0,current=color;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='\u00a7'&&i+1<text.length()) {
                char formatting=Character.toLowerCase(text.charAt(++i));
                int index="0123456789abcdef".indexOf(formatting);
                if(!shadow&&index>=0)current=(color&0xFF000000)|CHAT_RGB[index];
                else if(!shadow&&formatting=='r')current=color;
                continue;
            }
            if(c>=WIDTH.length)continue;
            g.blit(RenderPipelines.GUI_TEXTURED,TEXTURE,offset,0,X[c],Y[c],WIDTH[c],glyphHeight,SIZE,SIZE,current);
            offset+=WIDTH[c]-cellPadding;
        }
        g.pose().popMatrix();
    }

    /** Snap GUI-space origins to the physical framebuffer grid before raster submission. */
    private static float snapToFramebuffer(float value) {
        double guiScale=Minecraft.getInstance().getWindow().getGuiScale();
        if(guiScale<=0.0)return value;
        return (float)(Math.round(value*guiScale)/guiScale);
    }

    private static int ceilDiv(int value,int divisor) {
        return (value+divisor-1)/divisor;
    }

    private static String toFormattedString(FormattedCharSequence sequence) {
        var out=new StringBuilder();
        final int[] last={-2};
        sequence.accept((index,style,codePoint)->{
            int next=style.getColor()==null?-1:nearestChatColor(style.getColor().getValue());
            if(next!=last[0]) {
                out.append('\u00a7').append(next<0?'f':FORMAT_CODES[next]);
                last[0]=next;
            }
            if(Character.isBmpCodePoint(codePoint))out.append((char)codePoint);
            else out.appendCodePoint(codePoint);
            return true;
        });
        return out.toString();
    }

    private static int nearestChatColor(int rgb) {
        rgb&=0xFFFFFF;
        int r=rgb>>16&255,g=rgb>>8&255,b=rgb&255,best=0,bestDistance=Integer.MAX_VALUE;
        for(int i=0;i<CHAT_RGB.length;i++) {
            int c=CHAT_RGB[i],dr=r-(c>>16&255),dg=g-(c>>8&255),db=b-(c&255),d=dr*dr+dg*dg+db*db;
            if(d<bestDistance){bestDistance=d;best=i;}
        }
        return best;
    }

    private static int opaqueIfNeeded(int color) { return (color>>>24)==0?color|0xFF000000:color; }
}
