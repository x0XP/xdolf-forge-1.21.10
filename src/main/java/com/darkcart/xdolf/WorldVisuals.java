package com.darkcart.xdolf;

import com.darkcart.xdolf.mixin.GameRendererAccess;
import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.monster.Monster;
import net.minecraft.world.entity.vehicle.AbstractMinecart;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.*;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.ChestType;
import net.minecraft.world.level.chunk.LevelChunk;
import net.minecraft.world.level.chunk.status.ChunkStatus;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.FramePassManager;
import net.minecraftforge.client.event.AddFramePassEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.OptionalDouble;

/** World-space renderer for Xdolf tracers, ESP, storage, trajectories and visual smoke fixtures. */
final class WorldVisuals implements FramePassManager.PassDefinition {
    private record Segment(Vec3 a,Vec3 b,int color,double width,boolean stableStart) {
        Segment(Vec3 a,Vec3 b,int color,double width){this(a,b,color,width,false);}
    }
    private record Box(AABB bounds,int fill,Vec3 pivot,float yaw) {}
    private record Tag(Vec3 position,String text,float scale,int offset) {}
    private record Scene(Vec3 camera,Quaternionf rotation,List<Segment> lines,List<Box> boxes,List<Tag> tags) {
        static Scene empty(){return new Scene(Vec3.ZERO,new Quaternionf(),List.of(),List.of(),List.of());}
    }

    private Scene scene=Scene.empty();
    static boolean smokeFixture;
    private static int smokeLines,smokeBoxes,smokeTags;

    static void assertSmokeRendered() {
        if(smokeLines==0||smokeBoxes==0||smokeTags==0)
            throw new IllegalStateException("World frame pass did not render all visual fixture types");
    }

    private static final RenderPipeline LINE_PIPELINE=RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation(id("pipeline/world_lines")).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false).withCull(false).withBlend(BlendFunction.TRANSLUCENT).build();
    private static final RenderPipeline BOX_PIPELINE=RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR,com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
        .withLocation(id("pipeline/world_boxes")).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false).withCull(false).withBlend(BlendFunction.TRANSLUCENT).build();
    private static final RenderType BOX=RenderType.create("xdolf_world_box",1536,false,false,BOX_PIPELINE,
        RenderType.CompositeState.builder().createCompositeState(false));
    private static final RenderPipeline TAG_PIPELINE=RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR,com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
        .withLocation(id("pipeline/world_tag_background")).withDepthWrite(false).withCull(false)
        .withBlend(BlendFunction.TRANSLUCENT).build();
    private static final RenderType TAG_BACKGROUND=RenderType.create("xdolf_world_tag_background",1536,false,false,TAG_PIPELINE,
        RenderType.CompositeState.builder().createCompositeState(false));
    private static final java.util.Map<Double,RenderType> LINES=new java.util.HashMap<>();

    private static ResourceLocation id(String path){return ResourceLocation.fromNamespaceAndPath(Xdolf.ID,path);}

    static void register() {
        AddFramePassEvent.BUS.addListener(event->event.addPass(id("world_visuals"),new WorldVisuals()));
    }

    @Override
    public void targets(LevelTargetBundle targets,FramePass pass) {
        targets.main=pass.readsAndWrites(targets.main);
        var mc=Minecraft.getInstance();
        scene=extract(mc,mc.gameRenderer.getMainCamera().getPartialTickTime());
    }

    public void extracts(LevelTargetBundle targets,FramePass pass,DeltaTracker delta) {
        targets.main=pass.readsAndWrites(targets.main);
        scene=extract(Minecraft.getInstance(),delta.getGameTimeDeltaPartialTick(false));
    }

    @Override
    public void executes(LevelRenderState state) {
        if(scene.lines.isEmpty()&&scene.boxes.isEmpty()&&scene.tags.isEmpty())return;
        if(Boolean.getBoolean("xdolf.smokeTest")&&smokeFixture) {
            smokeLines=scene.lines.size();smokeBoxes=scene.boxes.size();smokeTags=scene.tags.size();
        }

        var pose=new PoseStack();
        pose.mulPose(new Quaternionf(scene.rotation).conjugate());
        Matrix4f inverseBob=null,inverseView=null;
        var mc=Minecraft.getInstance();
        if(mc.options.bobView().get()) {
            var bob=new PoseStack();
            ((GameRendererAccess)mc.gameRenderer).xdolf$bobView(bob,mc.gameRenderer.getMainCamera().getPartialTickTime());
            inverseBob=new Matrix4f(bob.last().pose()).invert();
            inverseView=new Matrix4f(pose.last().pose()).invert();
        }

        var modelView=RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        try(var storage=new ByteBufferBuilder(65536)) {
            var buffers=MultiBufferSource.immediate(storage);
            for(var box:scene.boxes)drawBox(buffers,pose.last().pose(),box,scene.camera);
            for(var segment:scene.lines) {
                var a=segment.a.subtract(scene.camera);
                var b=segment.b.subtract(scene.camera);
                if(segment.stableStart&&inverseBob!=null)a=unbobbedStart(a,pose.last().pose(),inverseBob,inverseView);
                line(buffers,pose.last().pose(),a,b,segment.color,segment.width);
            }
            buffers.endBatch();

            for(var tag:scene.tags) {
                var matrix=new Matrix4f(pose.last().pose())
                    .translate((float)(tag.position.x-scene.camera.x),(float)(tag.position.y-scene.camera.y),(float)(tag.position.z-scene.camera.z))
                    .rotate(scene.rotation).scale(tag.scale,-tag.scale,tag.scale);
                int half=XdolfFont.width(tag.text)/2;
                quad(buffers.getBuffer(TAG_BACKGROUND),matrix,-half-2,tag.offset,half+2,tag.offset+11,0x80000000);
                float left=-half-2,right=half+2,top=tag.offset,bottom=tag.offset+11;
                quad(buffers.getBuffer(TAG_BACKGROUND),matrix,left,top,right,top+0.5f,0xFF000000);
                quad(buffers.getBuffer(TAG_BACKGROUND),matrix,left,bottom-0.5f,right,bottom,0xFF000000);
                quad(buffers.getBuffer(TAG_BACKGROUND),matrix,left,top,left+0.5f,bottom,0xFF000000);
                quad(buffers.getBuffer(TAG_BACKGROUND),matrix,right-0.5f,top,right,bottom,0xFF000000);
                buffers.endBatch(TAG_BACKGROUND);
                XdolfFont.drawWorld(buffers,matrix,tag.text,-half,tag.offset,0xFFFFFFFF);
                buffers.endBatch();
            }
        } finally {
            modelView.popMatrix();
        }
    }

    private static Vec3 unbobbedStart(Vec3 relative,Matrix4f view,Matrix4f inverseBob,Matrix4f inverseView) {
        var point=new org.joml.Vector3f((float)relative.x,(float)relative.y,(float)relative.z);
        view.transformPosition(point);
        inverseBob.transformPosition(point);
        inverseView.transformPosition(point);
        return new Vec3(point.x,point.y,point.z);
    }

    private static final class LineStates extends RenderStateShard {
        private LineStates(){super("xdolf_world_lines",()->{},()->{});}
        static RenderType.CompositeState state(double width) {
            return RenderType.CompositeState.builder().setLineState(new LineStateShard(OptionalDouble.of(width))).createCompositeState(false);
        }
    }

    private static RenderType lineType(double width) {
        return LINES.computeIfAbsent(width,w->RenderType.create("xdolf_world_line_"+w,1536,false,false,LINE_PIPELINE,LineStates.state(w)));
    }

    private static void line(MultiBufferSource.BufferSource buffers,Matrix4f matrix,Vec3 a,Vec3 b,int color,double width) {
        var direction=b.subtract(a).normalize();
        if(direction.lengthSqr()<1e-12)return;
        var consumer=buffers.getBuffer(lineType(width));
        var normal=new org.joml.Vector3f((float)direction.x,(float)direction.y,(float)direction.z);
        matrix.transformDirection(normal);
        consumer.addVertex(matrix,(float)a.x,(float)a.y,(float)a.z).setColor(color).setNormal(normal.x,normal.y,normal.z);
        consumer.addVertex(matrix,(float)b.x,(float)b.y,(float)b.z).setColor(color).setNormal(normal.x,normal.y,normal.z);
    }

    private static Vec3 corner(Box box,int bits) {
        var b=box.bounds;
        var v=new Vec3((bits&1)==0?b.minX:b.maxX,(bits&2)==0?b.minY:b.maxY,(bits&4)==0?b.minZ:b.maxZ);
        return box.pivot==null?v:v.subtract(box.pivot).yRot(box.yaw).add(box.pivot);
    }

    private static void drawBox(MultiBufferSource.BufferSource buffers,Matrix4f matrix,Box box,Vec3 camera) {
        Vec3[] p=new Vec3[8];
        for(int i=0;i<8;i++)p[i]=corner(box,i).subtract(camera);
        var fill=buffers.getBuffer(BOX);
        for(int[] face:new int[][]{{0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}})
            for(int i:face)fill.addVertex(matrix,(float)p[i].x,(float)p[i].y,(float)p[i].z).setColor(box.fill);
        for(int i=0;i<8;i++)for(int mask:new int[]{1,2,4})if(i<(i^mask))
            line(buffers,matrix,p[i],p[i^mask],VisualStyle.BOX_EDGE,VisualStyle.BOX_WIDTH);
    }

    private static void quad(VertexConsumer c,Matrix4f matrix,float x,float y,float right,float bottom,int color) {
        c.addVertex(matrix,x,y,0).setColor(color);c.addVertex(matrix,right,y,0).setColor(color);
        c.addVertex(matrix,right,bottom,0).setColor(color);c.addVertex(matrix,x,bottom,0).setColor(color);
    }

    private static Scene extract(Minecraft mc,float partial) {
        if(mc.player==null||mc.level==null)return Scene.empty();
        var camera=mc.gameRenderer.getMainCamera();
        var origin=camera.getPosition();
        if(Boolean.getBoolean("xdolf.smokeTest")&&smokeFixture)return fixture(origin,new Quaternionf(camera.rotation()));

        var segments=new ArrayList<Segment>();
        var boxes=new ArrayList<Box>();
        var tags=new ArrayList<Tag>();
        boolean tracers=Hooks.enabled("Tracers"),storage=Hooks.enabled("StorageESP");
        boolean esp=Hooks.enabled("EntityESP")&&Hooks.setting("EntityESP","outline",1)==0;
        boolean names=Hooks.enabled("Nametags");
        var forward=camera.getLookVector();
        var start=origin.add(forward.x,forward.y,forward.z);

        for(var entity:mc.level.entitiesForRendering()) {
            if(entity instanceof Player player&&player!=mc.player) {
                if(tracers&&Hooks.setting("Tracers","players",1)!=0)
                    segments.add(new Segment(start,entity.position(),VisualStyle.tracerColor(mc.player.distanceTo(entity),SocialState.isFriend(entity.getName().getString())),VisualStyle.TRACER_WIDTH,true));
                if(names&&player.deathTime<=0) {
                    String text=VisualStyle.tag(player.getName().getString(),player.getHealth(),player.getArmorValue(),SocialState.isFriend(player.getName().getString()));
                    float distance=mc.player.distanceTo(player);
                    int offset=VisualStyle.tagOffset(distance,player.isShiftKeyDown());
                    tags.add(new Tag(player.position().add(0,player.getBbHeight()+0.5,0),text,VisualStyle.tagScale(distance),offset));
                }
            }

            if(esp&&Hooks.espTarget(entity)) {
                int color=entity instanceof Player?SocialState.isFriend(entity.getName().getString())?0x0000FF:0xFF0000
                    :entity instanceof Monster||entity.getType()==net.minecraft.world.entity.EntityType.ENDER_DRAGON||entity.getType()==net.minecraft.world.entity.EntityType.WITHER?0xFF0000:0x00FF00;
                var b=entity.getBoundingBox();
                boxes.add(new Box(new AABB(b.minX-0.05,b.minY,b.minZ-0.05,b.maxX+0.05,b.maxY+0.1,b.maxZ+0.05),0x26000000|color,null,0));
            }

            if(storage&&entity instanceof AbstractMinecart) {
                var type=entity.getType();
                int color=type==net.minecraft.world.entity.EntityType.CHEST_MINECART?0x00FF00:0xFFFFFF;
                if(type==net.minecraft.world.entity.EntityType.CHEST_MINECART||type==net.minecraft.world.entity.EntityType.FURNACE_MINECART||type==net.minecraft.world.entity.EntityType.HOPPER_MINECART)
                    boxes.add(new Box(new AABB(entity.blockPosition()),0x40000000|color,null,0));
            }
        }

        boolean chestTracers=tracers&&Hooks.setting("Tracers","chests",0)!=0;
        if(storage||chestTracers) {
            int radius=mc.options.renderDistance().get()+2;
            int cx=mc.player.blockPosition().getX()>>4,cz=mc.player.blockPosition().getZ()>>4;
            var merged=new HashSet<BlockPos>();
            for(int x=cx-radius;x<=cx+radius;x++)for(int z=cz-radius;z<=cz+radius;z++) {
                var chunk=mc.level.getChunkSource().getChunk(x,z,ChunkStatus.FULL,false);
                if(!(chunk instanceof LevelChunk loaded))continue;
                for(var be:loaded.getBlockEntities().values()) {
                    var pos=be.getBlockPos();
                    var blockState=be.getBlockState();
                    if(chestTracers&&be instanceof ChestBlockEntity)
                        segments.add(new Segment(start,Vec3.atLowerCornerOf(pos),VisualStyle.CHEST_TRACER,VisualStyle.TRACER_WIDTH,true));
                    if(!storage||merged.contains(pos))continue;

                    int color;
                    if(be instanceof ChestBlockEntity)color=blockState.is(Blocks.TRAPPED_CHEST)?0xFF0000:0x00FF00;
                    else if(be instanceof EnderChestBlockEntity)color=0xFF00FF;
                    else if(be instanceof ShulkerBoxBlockEntity)color=0xFFFF00;
                    else if(be instanceof AbstractFurnaceBlockEntity||be instanceof DispenserBlockEntity||be instanceof HopperBlockEntity)color=0xFFFFFF;
                    else continue;

                    var bounds=new AABB(pos);
                    if(be instanceof ChestBlockEntity&&blockState.hasProperty(BlockStateProperties.CHEST_TYPE)&&blockState.getValue(BlockStateProperties.CHEST_TYPE)!=ChestType.SINGLE) {
                        var other=pos.relative(ChestBlock.getConnectedDirection(blockState));
                        if(mc.level.getBlockEntity(other) instanceof ChestBlockEntity) {
                            bounds=bounds.minmax(new AABB(other));
                            merged.add(other.immutable());
                        }
                    }
                    merged.add(pos.immutable());
                    boxes.add(new Box(bounds,0x40000000|color,null,0));
                }
            }
        }

        if(Hooks.enabled("Trajectories"))trajectory(mc,partial,segments,boxes,origin);
        return new Scene(origin,new Quaternionf(camera.rotation()),List.copyOf(segments),List.copyOf(boxes),List.copyOf(tags));
    }

    /** Fixed visual objects for CI screenshots in the opt-in development world. */
    private static Scene fixture(Vec3 camera,Quaternionf rotation) {
        var forward=new org.joml.Vector3f(0,0,-1).rotate(rotation);
        var right=new org.joml.Vector3f(1,0,0).rotate(rotation);
        var lines=new ArrayList<Segment>();
        var boxes=new ArrayList<Box>();
        var tags=new ArrayList<Tag>();
        var start=camera.add(forward.x,forward.y,forward.z);
        for(int i=0;i<4;i++) {
            double distance=new double[]{5,50,100,20}[i],side=(i-1.5)*distance*0.25;
            var end=camera.add(forward.x*distance+right.x*side,forward.y*distance-1.6,forward.z*distance+right.z*side);
            lines.add(new Segment(start,end,VisualStyle.tracerColor(distance,i==3),VisualStyle.TRACER_WIDTH,true));
            int color=i==0?0xFF0000:i==1?0x00FF00:i==2?0x0000FF:0xFF00FF;
            boxes.add(new Box(new AABB(end.x-0.35,end.y,end.z-0.35,end.x+0.35,end.y+1.9,end.z+0.35),0x26000000|color,null,0));
            tags.add(new Tag(end.add(0,2.3,0),VisualStyle.tag("VisualTest"+i,20-i*3,20-i*4,i==3),VisualStyle.tagScale((float)distance),-14));
        }
        return new Scene(camera,rotation,List.copyOf(lines),List.copyOf(boxes),List.copyOf(tags));
    }

    private static void trajectory(Minecraft mc,float partial,List<Segment> lines,List<Box> boxes,Vec3 camera) {
        var stack=mc.player.getMainHandItem();
        boolean bow=stack.is(Items.BOW),rod=stack.is(Items.FISHING_ROD);
        if(!bow&&!rod&&!stack.is(Items.SNOWBALL)&&!stack.is(Items.EGG)&&!stack.is(Items.ENDER_PEARL))return;
        double yaw=Math.toRadians(mc.player.getYRot());
        var position=mc.player.getPosition(partial).add(-Math.cos(yaw)*0.16,mc.player.getEyeHeight()-0.100149011612,-Math.sin(yaw)*0.16);
        float charge=mc.player.isUsingItem()?mc.player.getTicksUsingItem()/20f:1;
        charge=Math.min(1,(charge*charge+charge*2)/3);
        if(charge<=0.1)charge=1;
        var motion=mc.player.getLookAngle().normalize().scale(bow?charge*3:1.5);
        double gravity=bow?0.05:rod?0.15:0.03;
        var points=new ArrayList<Vec3>();
        var eye=mc.player.getEyePosition();
        for(int i=0;i<1000;i++) {
            points.add(position);
            position=position.add(motion);
            motion=motion.scale(0.99).add(0,-gravity,0);
            if(mc.level.clip(new ClipContext(eye,position,ClipContext.Block.COLLIDER,ClipContext.Fluid.NONE,mc.player)).getType()!=HitResult.Type.MISS)break;
        }
        int color=0xFF00FF00|Math.min(255,(int)(mc.player.position().distanceTo(position)/100*255))<<16;
        for(int i=1;i<points.size();i++)lines.add(new Segment(points.get(i-1),points.get(i),color,VisualStyle.TRAJECTORY_WIDTH));
        float angle=(float)Math.toRadians(mc.player.getYRot()*Math.signum(position.y-camera.y));
        boxes.add(new Box(new AABB(position.x-0.35,position.y-0.5,position.z-0.5,position.x+0.65,position.y+0.5,position.z+0.5),(color&0xFFFFFF)|0x2F000000,position,angle));
    }
}
