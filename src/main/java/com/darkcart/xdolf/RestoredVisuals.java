package com.darkcart.xdolf;

import com.mojang.blaze3d.framegraph.FramePass;
import com.mojang.blaze3d.pipeline.BlendFunction;
import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.platform.DepthTestFunction;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.DeltaTracker;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.LevelTargetBundle;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.state.LevelRenderState;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.client.FramePassManager;
import net.minecraftforge.client.event.AddFramePassEvent;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.OptionalDouble;

/** World-space rendering for modules that existed in the old source but were not active in the initial port. */
final class RestoredVisuals implements FramePassManager.PassDefinition {
    private record Segment(Vec3 a, Vec3 b, int color, double width) {}
    private record Box(AABB bounds, int fill, int edge) {}
    private record Scene(Vec3 camera, Quaternionf rotation, List<Segment> lines, List<Box> boxes) {
        static Scene empty() { return new Scene(Vec3.ZERO, new Quaternionf(), List.of(), List.of()); }
    }

    private Scene scene = Scene.empty();

    private static final RenderPipeline LINE_PIPELINE = RenderPipeline.builder(RenderPipelines.LINES_SNIPPET)
        .withLocation(id("pipeline/restored_lines")).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false).withCull(false).withBlend(BlendFunction.TRANSLUCENT).build();
    private static final RenderPipeline BOX_PIPELINE = RenderPipeline.builder(RenderPipelines.DEBUG_FILLED_SNIPPET)
        .withVertexFormat(com.mojang.blaze3d.vertex.DefaultVertexFormat.POSITION_COLOR,
            com.mojang.blaze3d.vertex.VertexFormat.Mode.QUADS)
        .withLocation(id("pipeline/restored_boxes")).withDepthTestFunction(DepthTestFunction.NO_DEPTH_TEST)
        .withDepthWrite(false).withCull(false).withBlend(BlendFunction.TRANSLUCENT).build();
    private static final RenderType BOX = RenderType.create("xdolf_restored_box", 1536, false, false, BOX_PIPELINE,
        RenderType.CompositeState.builder().createCompositeState(false));
    private static final Map<Double, RenderType> LINES = new HashMap<>();

    private static ResourceLocation id(String path) {
        return ResourceLocation.fromNamespaceAndPath(Xdolf.ID, path);
    }

    static void register() {
        AddFramePassEvent.BUS.addListener(event -> event.addPass(id("restored_visuals"), new RestoredVisuals()));
    }

    @Override
    public void targets(LevelTargetBundle targets, FramePass pass) {
        targets.main = pass.readsAndWrites(targets.main);
        var mc = Minecraft.getInstance();
        scene = extract(mc, mc.gameRenderer.getMainCamera().getPartialTickTime());
    }

    public void extracts(LevelTargetBundle targets, FramePass pass, DeltaTracker delta) {
        targets.main = pass.readsAndWrites(targets.main);
        scene = extract(Minecraft.getInstance(), delta.getGameTimeDeltaPartialTick(false));
    }

    @Override
    public void executes(LevelRenderState state) {
        if (scene.lines.isEmpty() && scene.boxes.isEmpty()) return;

        var pose = new PoseStack();
        pose.mulPose(new Quaternionf(scene.rotation).conjugate());
        var modelView = RenderSystem.getModelViewStack();
        modelView.pushMatrix();
        modelView.identity();
        try (var storage = new ByteBufferBuilder(32768)) {
            var buffers = MultiBufferSource.immediate(storage);
            for (var box : scene.boxes) drawBox(buffers, pose.last().pose(), box, scene.camera);
            for (var segment : scene.lines)
                line(buffers, pose.last().pose(), segment.a.subtract(scene.camera), segment.b.subtract(scene.camera),
                    segment.color, segment.width);
            buffers.endBatch();
        } finally {
            modelView.popMatrix();
        }
    }

    private static Scene extract(Minecraft mc, float partial) {
        if (mc.player == null || mc.level == null) return Scene.empty();

        var camera = mc.gameRenderer.getMainCamera();
        var origin = camera.getPosition();
        var rotation = new Quaternionf(camera.rotation());
        var forward = camera.getLookVector();
        var start = origin.add(forward.x, forward.y, forward.z);
        var lines = new ArrayList<Segment>();
        var boxes = new ArrayList<Box>();
        String dimension = mc.level.dimension().location().toString();

        if (Hooks.enabled("Waypoints")) {
            for (var waypoint : LegacyCommands.waypoints) {
                if (!dimension.equals(waypoint.dimension())) continue;
                int color = waypointColor(waypoint.name());
                var bounds = new AABB(waypoint.x(), waypoint.y(), waypoint.z(),
                    waypoint.x() + 1.0, waypoint.y() + 1.0, waypoint.z() + 1.0);
                boxes.add(new Box(bounds, 0x2F000000 | color, 0x80000000));
                lines.add(new Segment(start,
                    new Vec3(waypoint.x() + 0.5, waypoint.y() + 0.5, waypoint.z() + 0.5),
                    0xFF000000 | color, 1.5));
            }
        }

        if (Hooks.enabled("LogoutSpot")) {
            for (var spot : LogoutSpotModule.spots()) {
                if (!dimension.equals(spot.dimension())) continue;
                var p = spot.position();
                // The original helper rendered a one-block-wide, two-block-tall logout marker.
                boxes.add(new Box(new AABB(p.x, p.y, p.z, p.x + 1.0, p.y + 2.0, p.z + 1.0),
                    0x2FFF0000, 0xFFFF0000));
                lines.add(new Segment(start, p.add(0.5, 1.0, 0.5), 0xFFFF0000, 1.5));
            }
        }

        return new Scene(origin, rotation, List.copyOf(lines), List.copyOf(boxes));
    }

    /** Original waypoints picked a random RGB colour when created; this keeps a stable bright colour per name. */
    private static int waypointColor(String name) {
        int hash = name.hashCode();
        int r = 80 + Math.floorMod(hash, 176);
        int g = 80 + Math.floorMod(Integer.rotateLeft(hash, 11), 176);
        int b = 80 + Math.floorMod(Integer.rotateLeft(hash, 22), 176);
        return (r << 16) | (g << 8) | b;
    }

    private static final class LineStates extends RenderStateShard {
        private LineStates() { super("xdolf_restored_lines", () -> {}, () -> {}); }
        static RenderType.CompositeState state(double width) {
            return RenderType.CompositeState.builder()
                .setLineState(new LineStateShard(OptionalDouble.of(width))).createCompositeState(false);
        }
    }

    private static RenderType lineType(double width) {
        return LINES.computeIfAbsent(width, w -> RenderType.create("xdolf_restored_line_" + w, 1536, false, false,
            LINE_PIPELINE, LineStates.state(w)));
    }

    private static void line(MultiBufferSource.BufferSource buffers, Matrix4f matrix, Vec3 a, Vec3 b, int color, double width) {
        var direction = b.subtract(a).normalize();
        if (direction.lengthSqr() < 1e-12) return;
        var consumer = buffers.getBuffer(lineType(width));
        var normal = new org.joml.Vector3f((float) direction.x, (float) direction.y, (float) direction.z);
        matrix.transformDirection(normal);
        consumer.addVertex(matrix, (float) a.x, (float) a.y, (float) a.z).setColor(color).setNormal(normal.x, normal.y, normal.z);
        consumer.addVertex(matrix, (float) b.x, (float) b.y, (float) b.z).setColor(color).setNormal(normal.x, normal.y, normal.z);
    }

    private static void drawBox(MultiBufferSource.BufferSource buffers, Matrix4f matrix, Box box, Vec3 camera) {
        var b = box.bounds;
        Vec3[] p = new Vec3[] {
            new Vec3(b.minX, b.minY, b.minZ), new Vec3(b.maxX, b.minY, b.minZ),
            new Vec3(b.minX, b.maxY, b.minZ), new Vec3(b.maxX, b.maxY, b.minZ),
            new Vec3(b.minX, b.minY, b.maxZ), new Vec3(b.maxX, b.minY, b.maxZ),
            new Vec3(b.minX, b.maxY, b.maxZ), new Vec3(b.maxX, b.maxY, b.maxZ)
        };
        for (int i = 0; i < p.length; i++) p[i] = p[i].subtract(camera);

        var fill = buffers.getBuffer(BOX);
        for (int[] face : new int[][] {{0,1,3,2},{4,6,7,5},{0,4,5,1},{2,3,7,6},{0,2,6,4},{1,5,7,3}})
            for (int i : face)
                fill.addVertex(matrix, (float) p[i].x, (float) p[i].y, (float) p[i].z).setColor(box.fill);

        for (int i = 0; i < 8; i++) {
            for (int mask : new int[] {1,2,4}) {
                if (i < (i ^ mask)) line(buffers, matrix, p[i], p[i ^ mask], box.edge, 1.0);
            }
        }
    }
}
