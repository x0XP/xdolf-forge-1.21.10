package com.x0xp.xdolf;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
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
    private static final int ATLAS_WIDTH = 2048;
    private static final int BASE_FONT_SIZE = 9;
    private static final int MAX_RASTER_SCALE = 4;
    private static final int GLYPH_COUNT = 2048;
    private static final FontAtlas[] ATLASES = new FontAtlas[MAX_RASTER_SCALE + 1];
    private static final char[] FORMAT_CODES = "0123456789abcdef".toCharArray();
    private static final int[] CHAT_RGB = {
        0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,
        0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF
    };
    private static FontAtlas activeAtlas;
    private static int chatDepth;
    private static int smokeRasterScale;

    private XdolfFont() {}

    private static final class FontAtlas {
        final int scale;
        final int textureHeight;
        final int glyphHeight;
        final int cellPadding;
        final int visibleTop;
        final int visibleBottom;
        final int[] width;
        final int[] x;
        final int[] y;
        final ResourceLocation texture;
        final RenderType worldText;

        FontAtlas(int scale,int textureHeight,int glyphHeight,int cellPadding,int visibleTop,int visibleBottom,
                  int[] width,int[] x,int[] y,ResourceLocation texture) {
            this.scale=scale;
            this.textureHeight=textureHeight;
            this.glyphHeight=glyphHeight;
            this.cellPadding=cellPadding;
            this.visibleTop=visibleTop;
            this.visibleBottom=visibleBottom;
            this.width=width;
            this.x=x;
            this.y=y;
            this.texture=texture;
            this.worldText=RenderType.text(texture);
        }
    }

    /**
     * Select an atlas rasterised for the current GUI scale. Each scale keeps its own texture, so
     * switching GUI scale never replaces a texture that queued render commands may still reference.
     */
    private static FontAtlas atlas() {
        int desiredScale=currentRasterScale();
        FontAtlas current=activeAtlas;
        if(current!=null&&current.scale==desiredScale)return current;
        FontAtlas cached=ATLASES[desiredScale];
        if(cached==null) {
            cached=buildAtlas(desiredScale);
            ATLASES[desiredScale]=cached;
        }
        activeAtlas=cached;
        return cached;
    }

    private static int currentRasterScale() {
        if(Boolean.getBoolean("xdolf.smokeTest")&&smokeRasterScale>0)return smokeRasterScale;
        Minecraft minecraft=Minecraft.getInstance();
        if(minecraft==null||minecraft.getWindow()==null)return 1;
        int scale=(int)Math.round(minecraft.getWindow().getGuiScale());
        return Math.max(1,Math.min(MAX_RASTER_SCALE,scale));
    }

    /** CI-only override that exercises another raster scale without mutating Minecraft's live GUI. */
    static void smokeRasterScale(int scale) {
        if(!Boolean.getBoolean("xdolf.smokeTest"))return;
        smokeRasterScale=scale>=1&&scale<=MAX_RASTER_SCALE?scale:0;
        activeAtlas=null;
    }

    private static FontAtlas buildAtlas(int scale) {
        long started=System.nanoTime();
        try {
            int fontSize=BASE_FONT_SIZE*scale;
            int cellPadding=Math.max(2,2*scale);
            int leftPadding=Math.max(1,Math.round(0.75f*scale));
            int topPadding=Math.max(0,Math.round(0.25f*scale));
            int extraHeight=Math.max(1,Math.round(0.75f*scale));
            int[] width=new int[GLYPH_COUNT],xPos=new int[GLYPH_COUNT],yPos=new int[GLYPH_COUNT];

            // Measure/layout first so each raster scale only allocates the texture height it uses.
            var measuringImage=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
            var measuringGraphics=measuringImage.createGraphics();
            measuringGraphics.setFont(new java.awt.Font("Roboto",java.awt.Font.PLAIN,fontSize));
            measuringGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            measuringGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            var metrics=measuringGraphics.getFontMetrics();
            int glyphHeight=metrics.getHeight()+extraHeight;
            int x=0,y=0;
            for(int i=0;i<GLYPH_COUNT;i++) {
                width[i]=Math.max(scale+1,metrics.charWidth((char)i)+cellPadding);
                if(x+width[i]>=ATLAS_WIDTH) { x=0; y+=glyphHeight; }
                xPos[i]=x;
                yPos[i]=y;
                x+=width[i];
            }
            measuringGraphics.dispose();

            int usedHeight=y+glyphHeight;
            int textureHeight=nextPowerOfTwo(Math.max(32,usedHeight));
            if(textureHeight>2048)throw new IllegalStateException("Xdolf TTF atlas overflow at GUI scale "+scale);

            var imageBuffer=new BufferedImage(ATLAS_WIDTH,textureHeight,BufferedImage.TYPE_INT_ARGB);
            var graphics=imageBuffer.createGraphics();
            graphics.setFont(new java.awt.Font("Roboto",java.awt.Font.PLAIN,fontSize));
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            graphics.setColor(Color.WHITE);
            for(int i=0;i<GLYPH_COUNT;i++)
                graphics.drawString(String.valueOf((char)i),xPos[i]+leftPadding,yPos[i]+topPadding+metrics.getAscent());
            graphics.dispose();

            int[] visible=measureVisibleAsciiBounds(imageBuffer,width,xPos,yPos,glyphHeight);
            var bytes=new ByteArrayOutputStream();
            ImageIO.write(imageBuffer,"png",bytes);
            var nativeImage=NativeImage.read(new ByteArrayInputStream(bytes.toByteArray()));
            var dynamicTexture=new DynamicTexture(() -> "Xdolf TTF font scale "+scale,nativeImage);
            dynamicTexture.setFilter(false,false);
            ResourceLocation texture=ResourceLocation.fromNamespaceAndPath("xdolf","client_font_"+scale);
            Minecraft.getInstance().getTextureManager().register(texture,dynamicTexture);

            if(Boolean.getBoolean("xdolf.smokeTest")) {
                double elapsed=(System.nanoTime()-started)/1_000_000_000.0;
                LogUtils.getLogger().info("XDOLF_FONT_ATLAS_OK: scale {} {}x{} built in {} ms",scale,ATLAS_WIDTH,textureHeight,Math.round(elapsed*1000.0));
            }
            return new FontAtlas(scale,textureHeight,glyphHeight,cellPadding,visible[0],visible[1],width,xPos,yPos,texture);
        } catch(java.io.IOException error) {
            throw new IllegalStateException("Cannot create Xdolf TTF font",error);
        }
    }

    /** Measure actual non-transparent glyph pixels rather than centring the padded atlas cell. */
    private static int[] measureVisibleAsciiBounds(BufferedImage atlas,int[] width,int[] x,int[] y,int glyphHeight) {
        int top=glyphHeight;
        int bottom=0;
        for(int c=33;c<=126;c++) {
            int cellX=x[c],cellY=y[c],cellWidth=width[c];
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
        if(bottom<=top)return new int[]{0,glyphHeight};
        return new int[]{top,bottom};
    }

    public static void beginChat() { chatDepth++; }
    public static void endChat() { if(chatDepth>0)chatDepth--; }
    public static boolean renderingChat() { return chatDepth>0; }

    /** Native GUI-space height of one Xdolf TTF atlas row. */
    public static int lineHeight() {
        FontAtlas atlas=atlas();
        return Math.max(10,ceilDiv(atlas.glyphHeight,atlas.scale));
    }

    /**
     * Returns the Y correction required to visually centre Xdolf's actual glyph ink (plus its
     * one-pixel drop shadow) inside a container whose vanilla text origin is already known.
     */
    public static int centeredYOffset(int containerTop,int containerHeight,int vanillaTextY) {
        FontAtlas atlas=atlas();
        float scale=1.0f/atlas.scale;
        float visibleTopGui=atlas.visibleTop*scale;
        float visibleBottomGui=atlas.visibleBottom*scale+1.0f;
        float visibleHeightGui=visibleBottomGui-visibleTopGui;
        float desiredVisibleTop=containerTop+(containerHeight-visibleHeightGui)*0.5f;
        float desiredCellTop=desiredVisibleTop-visibleTopGui;
        return Math.round(desiredCellTop-vanillaTextY);
    }

    public static void drawWorld(net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,
                                 org.joml.Matrix4f transform,String text,float x,float y,int color) {
        FontAtlas atlas=atlas();
        color=opaqueIfNeeded(color);
        worldLine(atlas,buffers,transform,text,x+1,y-1,(color&0xFF000000)|0x000D0D0D,true);
        worldLine(atlas,buffers,transform,text,x,y,color,false);
    }

    private static void worldLine(FontAtlas atlas,net.minecraft.client.renderer.MultiBufferSource.BufferSource buffers,
                                  org.joml.Matrix4f transform,String text,float x,float y,int color,boolean shadow) {
        float scale=1.0f/atlas.scale;
        var matrix=new org.joml.Matrix4f(transform).translate(x-1.5f,y,shadow?0.001f:0).scale(scale,scale,1);
        var consumer=buffers.getBuffer(atlas.worldText);
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
            if(c>=atlas.width.length)continue;
            float u=atlas.x[c]/(float)ATLAS_WIDTH,v=atlas.y[c]/(float)atlas.textureHeight;
            float right=(atlas.x[c]+atlas.width[c])/(float)ATLAS_WIDTH;
            float bottom=(atlas.y[c]+atlas.glyphHeight)/(float)atlas.textureHeight;
            consumer.addVertex(matrix,offset,atlas.glyphHeight,0).setColor(current).setUv(u,bottom).setUv2(240,240);
            consumer.addVertex(matrix,offset+atlas.width[c],atlas.glyphHeight,0).setColor(current).setUv(right,bottom).setUv2(240,240);
            consumer.addVertex(matrix,offset+atlas.width[c],0,0).setColor(current).setUv(right,v).setUv2(240,240);
            consumer.addVertex(matrix,offset,0,0).setColor(current).setUv(u,v).setUv2(240,240);
            offset+=atlas.width[c]-atlas.cellPadding;
        }
    }

    public static int width(String text) {
        FontAtlas atlas=atlas();
        int width=0;
        for(int i=0;i<text.length();i++) {
            char c=text.charAt(i);
            if(c=='\u00a7'&&i+1<text.length()){i++;continue;}
            if(c<atlas.width.length)width+=atlas.width[c]-atlas.cellPadding;
        }
        return Math.round(width/(float)atlas.scale);
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
        FontAtlas atlas=atlas();
        color=opaqueIfNeeded(color);
        if(shadow)drawLine(atlas,g,text,x+1,y+1,(color&0xFF000000)|0x000D0D0D,true);
        drawLine(atlas,g,text,x,y,color,false);
    }
    public static void draw(GuiGraphics g,FormattedCharSequence sequence,float x,float y,int color) { draw(g,toFormattedString(sequence),x,y,color,true); }
    public static void draw(GuiGraphics g,FormattedCharSequence sequence,float x,float y,int color,boolean shadow) { draw(g,toFormattedString(sequence),x,y,color,shadow); }

    private static void drawLine(FontAtlas atlas,GuiGraphics g,String text,float x,float y,int color,boolean shadow) {
        float scale=1.0f/atlas.scale;
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
            if(c>=atlas.width.length)continue;
            g.blit(RenderPipelines.GUI_TEXTURED,atlas.texture,offset,0,atlas.x[c],atlas.y[c],atlas.width[c],atlas.glyphHeight,
                ATLAS_WIDTH,atlas.textureHeight,current);
            offset+=atlas.width[c]-atlas.cellPadding;
        }
        g.pose().popMatrix();
    }

    /** Snap GUI-space origins to the physical framebuffer grid before raster submission. */
    private static float snapToFramebuffer(float value) {
        double guiScale=Minecraft.getInstance().getWindow().getGuiScale();
        if(guiScale<=0.0)return value;
        return (float)(Math.round(value*guiScale)/guiScale);
    }

    private static int nextPowerOfTwo(int value) {
        int highest=Integer.highestOneBit(value);
        return highest==value?value:highest<<1;
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
