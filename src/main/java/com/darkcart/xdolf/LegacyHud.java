package com.darkcart.xdolf;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import java.util.Comparator;
import java.util.Locale;

/** XDolfOverlay layout: no replacement watermark; right-aligned names and potion rows. */
final class LegacyHud {
    static boolean showModules=true,showPotions=true;
    static void render(GuiGraphics graphics) {
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.options.hideGui||mc.screen instanceof ChatScreen||mc.getDebugOverlay().showDebugScreen())return;
        int width=graphics.guiWidth(),height=graphics.guiHeight();
        if(showModules) {
            var enabled=ClientRuntime.MODULES.stream().filter(m->m.enabled()&&!m.name.equals("Fullbright"))
                .sorted(Comparator.comparingInt((ClientModule m)->LegacyGuiFont.width(ClientScreen.label(m))).reversed()).toList();
            for(int i=0;i<enabled.size();i++) {
                String text=ClientScreen.label(enabled.get(i));LegacyGuiFont.draw(graphics,text,width-LegacyGuiFont.width(text)-2,i*10,0xFFFFFFFF);
            }
        }
        if(showPotions) {
            int count=0;
            for(var effect:mc.player.getActiveEffects().stream().sorted().toList()) {
                String name=net.minecraft.client.resources.language.I18n.get(effect.getEffect().value().getDescriptionId());
                if(effect.getAmplifier()>=1&&effect.getAmplifier()<=3)name+=" "+new String[]{"II","III","IV"}[effect.getAmplifier()-1];
                int seconds=effect.getDuration()/20;
                String duration=effect.isInfiniteDuration()?"**:**":String.format(Locale.ROOT,"%d:%02d",seconds/60,seconds%60);
                potion(graphics,name,duration,BuiltInRegistries.MOB_EFFECT.getKey(effect.getEffect().value()),++count,width,height);
            }
            if(Hooks.enabled("Fullbright")&&!mc.player.getActiveEffects().stream().anyMatch(e->e.getEffect().equals(MobEffects.NIGHT_VISION)))
                potion(graphics,net.minecraft.client.resources.language.I18n.get(MobEffects.NIGHT_VISION.value().getDescriptionId()),"**:**",ResourceLocation.withDefaultNamespace("night_vision"),++count,width,height);
        }
        ClientScreen.renderPinned(graphics);
    }
    private static void potion(GuiGraphics g,String name,String duration,ResourceLocation effect,int row,int width,int height) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED,ResourceLocation.fromNamespaceAndPath(effect.getNamespace(),"mob_effect/"+effect.getPath()),width-20,height-row*20,18,18);
        LegacyGuiFont.draw(g,name,width-LegacyGuiFont.width(name)-22,height-row*20,0xFFFFFFFF);
        LegacyGuiFont.draw(g,duration,width-LegacyGuiFont.width(duration)/2-32,height+10-row*20,0xFF7F7F7F);
    }
}
