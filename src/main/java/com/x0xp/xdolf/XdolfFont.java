package com.x0xp.xdolf;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.logging.LogUtils;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.FormattedCharSequence;
import java.awt.Color;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;


import java.util.List;


/** Xdolf's Roboto/AWT font renderer used by the GUI, HUD, chat and world labels. */
public final class XdolfFont {
    private static final int ATLAS_WIDTH = 2048;
    private static final int BASE_FONT_SIZE = 9;
    private static final int COMPACT_FONT_SIZE = 7;
    private static final int MAX_RASTER_SCALE = 4;
    private static final int GLYPH_COUNT = 2048;
    private static final java.util.Map<String, FontAtlas> EXTRA_ATLASES = new java.util.HashMap<>();
    // Hard cap avoids evicting textures still referenced by queued render commands.
    // Each extra page is at most 2048 x 2048 RGBA: at most 64 MiB total.
    private static final int MAX_EXTRA_ATLASES = 4;
    private static final java.util.Map<ResourceLocation, FontAtlas> REPLACEMENT_ATLASES = new java.util.HashMap<>();
    private static java.awt.Font bundledFont;
    private static final FontAtlas[] ATLASES = new FontAtlas[MAX_RASTER_SCALE + 1];
    private static final FontAtlas[] COMPACT_ATLASES = new FontAtlas[MAX_RASTER_SCALE + 1];
    private static final int[] CHAT_RGB = {
        0x000000,0x0000AA,0x00AA00,0x00AAAA,0xAA0000,0xAA00AA,0xFFAA00,0xAAAAAA,
        0x555555,0x5555FF,0x55FF55,0x55FFFF,0xFF5555,0xFF55FF,0xFFFF55,0xFFFFFF
    };
    private static FontAtlas activeAtlas;
    private static FontAtlas activeCompactAtlas;
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
            cached=buildAtlas(desiredScale,BASE_FONT_SIZE,"client_font_","main");
            ATLASES[desiredScale]=cached;
        }
        activeAtlas=cached;
        return cached;
    }

    /** Smaller, separately rasterised text for subordinate click-GUI controls. */
    private static FontAtlas compactAtlas() {
        int desiredScale=currentRasterScale();
        FontAtlas current=activeCompactAtlas;
        if(current!=null&&current.scale==desiredScale)return current;
        FontAtlas cached=COMPACT_ATLASES[desiredScale];
        if(cached==null) {
            cached=buildAtlas(desiredScale,COMPACT_FONT_SIZE,"client_font_compact_","compact");
            COMPACT_ATLASES[desiredScale]=cached;
        }
        activeCompactAtlas=cached;
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
        activeCompactAtlas=null;
    }

    private static java.awt.Font font(int size) throws java.io.IOException {
        if (bundledFont == null) {
            try (var stream = XdolfFont.class.getResourceAsStream("/assets/xdolf/font/Roboto-Regular.ttf")) {
                if (stream == null) throw new java.io.IOException("Bundled Roboto font missing");
                bundledFont = java.awt.Font.createFont(java.awt.Font.TRUETYPE_FONT, stream);
            } catch (java.awt.FontFormatException error) {
                throw new java.io.IOException("Invalid bundled Roboto font", error);
            }
        }
        return bundledFont.deriveFont((float) size);
    }

    private static FontAtlas glyphAtlas(FontAtlas base, int codePoint) {
        int page = codePoint / GLYPH_COUNT;
        if (page == 0) return base.texture.getPath().contains("compact") ? compactAtlas() : atlas();
        boolean compact = base.texture.getPath().contains("compact");
        String key = (compact ? "compact_" : "main_") + base.scale + "_" + page;
        FontAtlas cached = EXTRA_ATLASES.get(key);
        if (cached != null) return cached;
        if (EXTRA_ATLASES.size() >= MAX_EXTRA_ATLASES) {
            FontAtlas primary = compact ? compactAtlas() : atlas();
            return REPLACEMENT_ATLASES.computeIfAbsent(primary.texture, ignored -> {
                int[] widths = new int[GLYPH_COUNT], xs = new int[GLYPH_COUNT], ys = new int[GLYPH_COUNT];
                java.util.Arrays.fill(widths, primary.width['?']);
                java.util.Arrays.fill(xs, primary.x['?']);
                java.util.Arrays.fill(ys, primary.y['?']);
                return new FontAtlas(primary.scale, primary.textureHeight, primary.glyphHeight,
                    primary.cellPadding, primary.visibleTop, primary.visibleBottom, widths, xs, ys, primary.texture);
            });
        }
        return EXTRA_ATLASES.computeIfAbsent(key, ignored -> buildAtlas(base.scale,
            compact ? COMPACT_FONT_SIZE : BASE_FONT_SIZE, "font_page_" + key + "_", "unicode", page));
    }

    private static FontAtlas buildAtlas(int scale,int baseFontSize,String texturePrefix,String role) {
        return buildAtlas(scale,baseFontSize,texturePrefix,role,0);
    }

    private static FontAtlas buildAtlas(int scale,int baseFontSize,String texturePrefix,String role,int page) {
        long started=System.nanoTime();
        try {
            int fontSize=baseFontSize*scale;
            var primaryFont=font(fontSize);
            var fallbackFont=new java.awt.Font("Dialog",java.awt.Font.PLAIN,fontSize);
            int cellPadding=Math.max(2,2*scale);
            int leftPadding=Math.max(1,Math.round(0.75f*scale));
            int topPadding=Math.max(0,Math.round(0.25f*scale));
            int extraHeight=Math.max(1,Math.round(0.75f*scale));
            int[] width=new int[GLYPH_COUNT],xPos=new int[GLYPH_COUNT],yPos=new int[GLYPH_COUNT];

            // Measure/layout first so each raster scale only allocates the texture height it uses.
            var measuringImage=new BufferedImage(1,1,BufferedImage.TYPE_INT_ARGB);
            var measuringGraphics=measuringImage.createGraphics();
            measuringGraphics.setFont(primaryFont);
            measuringGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            measuringGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            var metrics=measuringGraphics.getFontMetrics();
            int glyphHeight=metrics.getHeight()+extraHeight;
            int x=0,y=0;
            for(int i=0;i<GLYPH_COUNT;i++) {
                int codePoint=page*GLYPH_COUNT+i;
                var selected=primaryFont.canDisplay(codePoint)?primaryFont:fallbackFont;
                width[i]=Math.max(cellPadding,measuringGraphics.getFontMetrics(selected).charWidth(codePoint)+cellPadding);
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
            graphics.setFont(primaryFont);
            graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
            graphics.setColor(Color.WHITE);
            for(int i=0;i<GLYPH_COUNT;i++) {
                int codePoint=page*GLYPH_COUNT+i;
                graphics.setFont(primaryFont.canDisplay(codePoint)?primaryFont:fallbackFont);
                graphics.drawString(new String(Character.toChars(codePoint)),xPos[i]+leftPadding,yPos[i]+topPadding+metrics.getAscent());
            }
            graphics.dispose();

            int[] visible=measureVisibleAsciiBounds(imageBuffer,width,xPos,yPos,glyphHeight);
            var nativeImage=new NativeImage(ATLAS_WIDTH,textureHeight,false);
            for(int py=0;py<textureHeight;py++)
                for(int px=0;px<ATLAS_WIDTH;px++)
                    nativeImage.setPixel(px,py,imageBuffer.getRGB(px,py));
            var dynamicTexture=new DynamicTexture(() -> "Xdolf "+role+" TTF font scale "+scale,nativeImage);
            dynamicTexture.setFilter(false,false);
            ResourceLocation texture=ResourceLocation.fromNamespaceAndPath("xdolf",texturePrefix+scale);
            Minecraft.getInstance().getTextureManager().register(texture,dynamicTexture);

            if(Boolean.getBoolean("xdolf.smokeTest")) {
                double elapsed=(System.nanoTime()-started)/1_000_000_000.0;
                LogUtils.getLogger().info("XDOLF_FONT_ATLAS_OK: {} scale {} {}x{} built in {} ms",role,scale,ATLAS_WIDTH,textureHeight,Math.round(elapsed*1000.0));
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
        return centeredYOffset(atlas(),containerTop,containerHeight,vanillaTextY);
    }

    public static int compactCenteredYOffset(int containerTop,int containerHeight,int vanillaTextY) {
        return centeredYOffset(compactAtlas(),containerTop,containerHeight,vanillaTextY);
    }

    private static int centeredYOffset(FontAtlas atlas,int containerTop,int containerHeight,int vanillaTextY) {
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
        for(int i=0;i<text.length();) {
            int c=text.codePointAt(i);
            i+=Character.charCount(c);
            if(c=='\u00a7'&&i<text.length()) {
                char formatting=Character.toLowerCase(text.charAt(i++));
                int code="0123456789abcdef".indexOf(formatting);
                if(!shadow&&code>=0)current=(color&0xFF000000)|CHAT_RGB[code];
                else if(!shadow&&formatting=='r')current=color;
                continue;
            }
            atlas=glyphAtlas(atlas,c);
            c%=GLYPH_COUNT;
            consumer=buffers.getBuffer(atlas.worldText);
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
        return width(atlas(),text);
    }

    public static int compactWidth(String text) {
        return width(compactAtlas(),text);
    }

    private static int width(FontAtlas atlas,String text) {
        int width=0;
        for(int i=0;i<text.length();) {
            int c=text.codePointAt(i);
            i+=Character.charCount(c);
            if(c=='\u00a7'&&i<text.length()){i++;continue;}
            var glyph=glyphAtlas(atlas,c);
            width+=glyph.width[c%GLYPH_COUNT]-glyph.cellPadding;
        }
        return Math.round(width/(float)atlas.scale);
    }

    public static int width(FormattedCharSequence sequence) {
        final float[] advance={0};
        sequence.accept((index,style,cp)->{
            var glyph=glyphAtlas(atlas(),cp);
            advance[0]+=(glyph.width[cp%GLYPH_COUNT]-glyph.cellPadding)/(float)glyph.scale+(style.isBold()?1:0);
            return true;
        });
        return Math.round(advance[0]);
    }

    public static String trim(String text,int max) {
        return trim(atlas(),text,max);
    }

    public static String compactTrim(String text,int max) {
        return trim(compactAtlas(),text,max);
    }

    private static String trim(FontAtlas atlas,String text,int max) {
        int end=0;
        int advance=0;
        while(end<text.length()) {
            int cp=text.codePointAt(end);
            int next=end+Character.charCount(cp);
            if(cp=='\u00a7'&&next<text.length()) { end=next+1; continue; }
            var glyph=glyphAtlas(atlas,cp);
            int candidate=advance+glyph.width[cp%GLYPH_COUNT]-glyph.cellPadding;
            if(Math.round(candidate/(float)atlas.scale)>max)break;
            advance=candidate;
            end=next;
        }
        return text.substring(0,end);
    }

    /**
     * Minecraft owns chat scrolling/fading and line placement; this chooses the vanilla wrap width
     * whose resulting lines fit the Xdolf font's real advances inside the configured chat width.
     */
    public static List<FormattedCharSequence> wrapChat(FormattedText text,int visualWidth,Font vanillaFont) {
        var splitter=new net.minecraft.client.StringSplitter((cp,style)->{
            var glyph=glyphAtlas(atlas(),cp);
            return (glyph.width[cp%GLYPH_COUNT]-glyph.cellPadding)/(float)glyph.scale+(style.isBold()?1:0);
        });
        return splitter.splitLines(text,Math.max(1,visualWidth),net.minecraft.network.chat.Style.EMPTY).stream()
            .map(line->net.minecraft.locale.Language.getInstance().getVisualOrder(line)).toList();
    }

    public static void draw(GuiGraphics g,String text,float x,float y,int color) { draw(g,text,x,y,color,true); }
    public static void draw(GuiGraphics g,String text,float x,float y,int color,boolean shadow) {
        FontAtlas atlas=atlas();
        color=opaqueIfNeeded(color);
        if(shadow)drawLine(atlas,g,text,x+1,y+1,(color&0xFF000000)|0x000D0D0D,true);
        drawLine(atlas,g,text,x,y,color,false);
    }
    public static void drawCompact(GuiGraphics g,String text,float x,float y,int color) { drawCompact(g,text,x,y,color,true); }
    public static void drawCompact(GuiGraphics g,String text,float x,float y,int color,boolean shadow) {
        FontAtlas atlas=compactAtlas();
        color=opaqueIfNeeded(color);
        if(shadow)drawLine(atlas,g,text,x+1,y+1,(color&0xFF000000)|0x000D0D0D,true);
        drawLine(atlas,g,text,x,y,color,false);
    }
    public static void draw(GuiGraphics g,FormattedCharSequence sequence,float x,float y,int color) { draw(g,sequence,x,y,color,true); }
    public static void draw(GuiGraphics g,FormattedCharSequence sequence,float x,float y,int color,boolean shadow) {
        final float[] offset={0};
        final int baseColor=opaqueIfNeeded(color);
        sequence.accept((index,style,cp)->{
            var glyph=glyphAtlas(atlas(),cp);
            float advance=(glyph.width[cp%GLYPH_COUNT]-glyph.cellPadding)/(float)glyph.scale+(style.isBold()?1:0);
            int tint=style.getColor()==null?baseColor:(baseColor&0xFF000000)|style.getColor().getValue();
            String value=new String(Character.toChars(cp));
            if(style.isItalic()) {
                g.pose().pushMatrix();
                g.pose().translate(x+offset[0],y);
                g.pose().mul(new org.joml.Matrix3x2f(1,0,-0.2f,1,0,0));
                draw(g,value,0,0,tint,shadow);
                if(style.isBold())draw(g,value,1,0,tint,false);
                g.pose().popMatrix();
            } else {
                draw(g,value,x+offset[0],y,tint,shadow);
            if(style.isBold())draw(g,value,x+offset[0]+1,y,tint,false);
            }
            if(style.isUnderlined())UiDraw.rect(g,x+offset[0],y+10,x+offset[0]+advance,y+11,tint);
            if(style.isStrikethrough())UiDraw.rect(g,x+offset[0],y+6,x+offset[0]+advance,y+7,tint);
            offset[0]+=advance;
            return true;
        });
    }

    private static void drawLine(FontAtlas atlas,GuiGraphics g,String text,float x,float y,int color,boolean shadow) {
        float scale=1.0f/atlas.scale;
        float snappedX=snapToFramebuffer(x-1.5f);
        float snappedY=snapToFramebuffer(y);
        g.pose().pushMatrix();
        g.pose().translate(snappedX,snappedY);
        g.pose().scale(scale,scale);
        int offset=0,current=color;
        for(int i=0;i<text.length();) {
            int c=text.codePointAt(i);
            i+=Character.charCount(c);
            if(c=='\u00a7'&&i<text.length()) {
                char formatting=Character.toLowerCase(text.charAt(i++));
                int index="0123456789abcdef".indexOf(formatting);
                if(!shadow&&index>=0)current=(color&0xFF000000)|CHAT_RGB[index];
                else if(!shadow&&formatting=='r')current=color;
                continue;
            }
            atlas=glyphAtlas(atlas,c);
            c%=GLYPH_COUNT;
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

    private static int opaqueIfNeeded(int color) {
        int opaque = (color >>> 24) == 0 ? color | 0xFF000000 : color;
        return UiDraw.fade(opaque, UiDraw.globalAlpha());
    }
}
