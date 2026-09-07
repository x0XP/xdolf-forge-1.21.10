package com.x0xp.xdolf.module.world;

import com.x0xp.xdolf.*;
import com.x0xp.xdolf.module.support.*;

import net.minecraft.client.Minecraft;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.Tags;

/** Immutable block selection and a volatile flag are safe to read on chunk workers. */
public final class XRayModule extends ClientModule {
    private record Selection(java.util.Set<String> added,java.util.Set<String> removed) {}
    private static volatile Selection selection=new Selection(java.util.Set.of(),java.util.Set.of());
    public static volatile boolean rendering;
    public XRayModule() { super("XRay", "Render ores and selected storage blocks through terrain.", "World"); }
    public static boolean visible(BlockState state) {
        String id=net.minecraft.core.registries.BuiltInRegistries.BLOCK.getKey(state.getBlock()).toString();
        var snapshot=selection;
        if(snapshot.removed.contains(id))return false;
        if(snapshot.added.contains(id))return true;
        return state.is(Tags.Blocks.ORES) || state.is(Blocks.ANCIENT_DEBRIS) || state.is(Blocks.CHEST)
            || state.is(Blocks.TRAPPED_CHEST) || state.is(Blocks.ENDER_CHEST) || state.is(Blocks.SPAWNER);
    }
    public static void edit(String id,boolean add) {
        var a=new java.util.HashSet<>(selection.added);var r=new java.util.HashSet<>(selection.removed);
        if(add){r.remove(id);a.add(id);}else{a.remove(id);r.add(id);}
        selection=new Selection(java.util.Set.copyOf(a),java.util.Set.copyOf(r));
        var mc=Minecraft.getInstance();if(rendering&&mc.level!=null)mc.levelRenderer.allChanged();
    }
    public static void loadSelection(java.util.Properties p) {
        selection=new Selection(ids(p.getProperty("xray.added","")),ids(p.getProperty("xray.removed","")));
    }
    private static java.util.Set<String> ids(String value) {
        var set=new java.util.HashSet<String>();for(String id:value.split(","))if(net.minecraft.resources.ResourceLocation.tryParse(id)!=null)set.add(id);return java.util.Set.copyOf(set);
    }
    public static void saveSelection(java.util.Properties p) {p.setProperty("xray.added",String.join(",",selection.added));p.setProperty("xray.removed",String.join(",",selection.removed));}
    private void update(Minecraft mc, boolean value) {
        if (rendering == value) return;
        rendering = value;
        if (mc.level != null) mc.levelRenderer.allChanged();
    }
    @Override public void activate(Minecraft mc) { update(mc, true); }
    @Override public void tick(Minecraft mc) { update(mc, true); }
    @Override public void reset(Minecraft mc) { update(mc, false); }
}
