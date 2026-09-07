package com.x0xp.xdolf.ui.hud;

import com.x0xp.xdolf.core.ClientRuntime;
import com.x0xp.xdolf.core.Hooks;
import com.x0xp.xdolf.render.VisualStyle;
import com.x0xp.xdolf.social.SocialState;
import com.x0xp.xdolf.ui.UiDraw;
import com.x0xp.xdolf.ui.XdolfFont;
import com.x0xp.xdolf.ui.clickgui.ClientScreen;

import com.x0xp.xdolf.module.ClientModule;

import com.x0xp.xdolf.module.render.LogoutSpotModule;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.ChatScreen;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Xdolf HUD, module list, potion list and screen-space player/logout labels. */
public final class Hud {
    public static boolean showModules=true,showPotions=true;
    private static final long MODULE_ENTER_NS = 180_000_000L;
    private static final long MODULE_MOVE_NS = 160_000_000L;
    private static final long MODULE_EXIT_NS = 160_000_000L;
    private static final Map<ClientModule, ModuleHudEntry> MODULE_HUD = new IdentityHashMap<>();

    public static void render(GuiGraphics graphics) {
        var mc=Minecraft.getInstance();
        if(mc.player==null||mc.level==null||mc.options.hideGui)return;

        if(Hooks.enabled("Nametags"))renderNametags(graphics,mc);
        if(Hooks.enabled("LogoutSpot"))renderLogoutSpotTags(graphics,mc);
        // ClientScreen redraws notifications after its dim layer/panels so cards stay in the foreground.
        if(!(mc.screen instanceof ClientScreen))NotificationCards.render(graphics);

        if(mc.screen instanceof ChatScreen||mc.getDebugOverlay().showDebugScreen())return;
        int width=graphics.guiWidth(),height=graphics.guiHeight();
        if(showModules) renderModules(graphics,width);
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

    private static void renderModules(GuiGraphics graphics,int width) {
        long now=System.nanoTime();
        var enabled=ClientRuntime.MODULES.stream().filter(m->m.enabled()&&!m.name.equals("Fullbright"))
            .sorted(Comparator.comparingInt((ClientModule m)->XdolfFont.width(ClientScreen.label(m))).reversed()).toList();

        for(int i=0;i<enabled.size();i++) {
            ClientModule module=enabled.get(i);
            String text=ClientScreen.label(module);
            float targetX=width-XdolfFont.width(text)-2;
            float targetY=i*10.0f;
            ModuleHudEntry entry=MODULE_HUD.get(module);
            if(entry==null) {
                entry=new ModuleHudEntry(width+6.0f,targetY,now);
                MODULE_HUD.put(module,entry);
            } else if(entry.exiting) {
                entry.revive(now);
            }
            entry.moveTo(targetX,targetY,now);
        }

        var iterator=MODULE_HUD.entrySet().iterator();
        while(iterator.hasNext()) {
            var item=iterator.next();
            ModuleHudEntry entry=item.getValue();
            if(!enabled.contains(item.getKey())) entry.startExit(width+6.0f,now);
            if(entry.finished(now)) iterator.remove();
        }

        for(var item:MODULE_HUD.entrySet()) {
            ClientModule module=item.getKey();
            ModuleHudEntry entry=item.getValue();
            float alpha=entry.alpha(now);
            if(alpha<=0.01f)continue;
            XdolfFont.draw(graphics,ClientScreen.label(module),entry.x(now),entry.y(now),UiDraw.fade(0xFFFFFFFF,alpha));
        }
    }

    private static final class ModuleHudEntry {
        final long created;
        float fromX;
        float fromY;
        float targetX;
        float targetY;
        long moveStarted;
        boolean exiting;
        long exitStarted;

        ModuleHudEntry(float x,float y,long now) {
            created=now;
            fromX=targetX=x;
            fromY=targetY=y;
            moveStarted=now;
        }

        float x(long now) {
            float t=UiDraw.easeOutCubic(UiDraw.clamp01((now-moveStarted)/(float)MODULE_MOVE_NS));
            return fromX+(targetX-fromX)*t;
        }

        float y(long now) {
            float t=UiDraw.easeOutCubic(UiDraw.clamp01((now-moveStarted)/(float)MODULE_MOVE_NS));
            return fromY+(targetY-fromY)*t;
        }

        float alpha(long now) {
            float enter=UiDraw.easeOutCubic(UiDraw.clamp01((now-created)/(float)MODULE_ENTER_NS));
            if(!exiting)return enter;
          float exit=UiDraw.easeInCubic(UiDraw.clamp01((now-exitStarted)/(float)MODULE_EXIT_NS));
            return enter*(1.0f-exit);
      }

        void moveTo(float x,float y,long now) {
            if(targetX==x&&targetY==y)return;
            fromX=x(now);
            fromY=y(now);
            targetX=x;
            targetY=y;
            moveStarted=now;
        }

        void startExit(float hiddenX,long now) {
            if(exiting)return;
            exiting=true;
            exitStarted=now;
            moveTo(hiddenX,y(now),now);
        }

        void revive(long now) {
            exiting=false;
            fromX=x(now);
            fromY=y(now);
            moveStarted=now;
      }

        boolean finished(long now) {
            return exiting&&now-exitStarted>=MODULE_EXIT_NS;
        }
    }

    private static void renderNametags(GuiGraphics g,Minecraft mc) {
        var camera=mc.gameRenderer.getMainCamera();
        float partial=camera.getPartialTickTime();
        Vec3 cameraPos=camera.getPosition();
        var look=camera.getLookVector();
        Vec3 forward=new Vec3(look.x,look.y,look.z);
        int screenWidth=g.guiWidth(),screenHeight=g.guiHeight();

        for(Player player:mc.level.players()) {
            if(player==mc.player||player.deathTime>0)continue;
            Vec3 playerPos=player.getPosition(partial);
            Vec3 world=playerPos.add(0,player.getBbHeight()+0.55,0);
            ScreenPoint point=project(mc,world,cameraPos,forward,screenWidth,screenHeight);
            if(point==null)continue;

            float distance=(float)cameraPos.distanceTo(playerPos);
            String text=VisualStyle.tag(player.getName().getString(),player.getHealth(),player.getArmorValue(),SocialState.isFriend(player.getName().getString()));
            drawNametag(g,mc,player,text,point.x,point.y,VisualStyle.screenTagScale(distance));
        }
    }

    private static void renderLogoutSpotTags(GuiGraphics g,Minecraft mc) {
        var camera=mc.gameRenderer.getMainCamera();
        Vec3 cameraPos=camera.getPosition();
        var look=camera.getLookVector();
        Vec3 forward=new Vec3(look.x,look.y,look.z);
        int screenWidth=g.guiWidth(),screenHeight=g.guiHeight();
        String dimension=mc.level.dimension().location().toString();

        for(var spot:LogoutSpotModule.spots()) {
            if(!dimension.equals(spot.dimension()))continue;
            Vec3 center=spot.position().add(0.5,1.0,0.5);
            Vec3 world=spot.position().add(0.5,2.45,0.5);
            ScreenPoint point=project(mc,world,cameraPos,forward,screenWidth,screenHeight);
            if(point==null)continue;
            float distance=(float)cameraPos.distanceTo(center);
            String text=spot.name()+" \u00a7c[LogoutSpot]";
            drawLabel(g,text,point.x,point.y,VisualStyle.screenTagScale(distance));
        }
    }

    private record ScreenPoint(float x,float y) {}

    private static ScreenPoint project(Minecraft mc,Vec3 world,Vec3 cameraPos,Vec3 forward,int screenWidth,int screenHeight) {
        Vec3 relative=world.subtract(cameraPos);
        if(relative.dot(forward)<=0.01)return null;
        Vec3 projected=mc.gameRenderer.projectPointToScreen(world);
        if(projected.z<-1.0||projected.z>1.0)return null;
        float x=(float)((projected.x+1.0)*0.5*screenWidth);
        float y=(float)((1.0-projected.y)*0.5*screenHeight);
        if(x<-128||x>screenWidth+128||y<-64||y>screenHeight+64)return null;
        return new ScreenPoint(x,y);
    }

    private static void drawNametag(GuiGraphics g,Minecraft mc,Player player,String text,float screenX,float screenY,float scale) {
        drawLabel(g,text,screenX,screenY,scale);

        List<ItemStack> items=tagItems(player);
        if(items.isEmpty())return;
        int rowWidth=items.size()*18-2;
        int start=-rowWidth/2;
        int itemY=-20;
        g.pose().pushMatrix();
        g.pose().translate(screenX,screenY);
        g.pose().scale(scale,scale);
        for(int i=0;i<items.size();i++) {
            ItemStack stack=items.get(i);
            int itemX=start+i*18;
            g.renderItem(stack,itemX,itemY);
            g.renderItemDecorations(mc.font,stack,itemX,itemY);
        }
        g.pose().popMatrix();
    }

    private static void drawLabel(GuiGraphics g,String text,float screenX,float screenY,float scale) {
        int textWidth=XdolfFont.width(text);
        int left=-textWidth/2-3,right=textWidth/2+3;
        g.pose().pushMatrix();
        g.pose().translate(screenX,screenY);
        g.pose().scale(scale,scale);

        g.fill(left,-1,right,11,0x88000000);
        g.fill(left,-1,right,0,0xFF000000);
        g.fill(left,10,right,11,0xFF000000);
        g.fill(left,-1,left+1,11,0xFF000000);
        g.fill(right-1,-1,right,11,0xFF000000);
        XdolfFont.draw(g,text,-textWidth/2f,0,0xFFFFFFFF);
        g.pose().popMatrix();
    }

    private static List<ItemStack> tagItems(Player player) {
        var items=new ArrayList<ItemStack>(6);
        add(items,player.getMainHandItem());
        add(items,player.getItemBySlot(EquipmentSlot.HEAD));
        add(items,player.getItemBySlot(EquipmentSlot.CHEST));
        add(items,player.getItemBySlot(EquipmentSlot.LEGS));
        add(items,player.getItemBySlot(EquipmentSlot.FEET));
        add(items,player.getOffhandItem());
        return items;
    }

    private static void add(List<ItemStack> items,ItemStack stack) {
        if(stack!=null&&!stack.isEmpty())items.add(stack);
    }

    private static void potion(GuiGraphics g,String name,String duration,ResourceLocation effect,int row,int width,int height) {
        g.blitSprite(RenderPipelines.GUI_TEXTURED,ResourceLocation.fromNamespaceAndPath(effect.getNamespace(),"mob_effect/"+effect.getPath()),width-20,height-row*20,18,18);
        XdolfFont.draw(g,name,width-XdolfFont.width(name)-22,height-row*20,0xFFFFFFFF);
        XdolfFont.draw(g,duration,width-XdolfFont.width(duration)/2-32,height+10-row*20,0xFF7F7F7F);
    }
}
