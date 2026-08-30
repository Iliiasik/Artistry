package iliiasik.artistry.client.renderer;

import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.blaze3d.vertex.VertexFormat;
import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.client.renderer.ImageOcclusionClipper.VisibleFragment;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.data.CanvasImageLayer;
import iliiasik.artistry.debug.ArtistryDebug;
import iliiasik.artistry.network.ArtistryNetwork;
import iliiasik.artistry.network.RequestImageC2SPacket;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.NotNull;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PosterBlockEntityRenderer implements BlockEntityRenderer<PosterBlockEntity> {

    private static final float Z_CANVAS = 15f / 16f - 0.001f;
    private static final float Z_IMAGES = 15f / 16f - 0.001f;

    private static final Map<Long, PosterState> STATES = new HashMap<>();
    private static final Set<UUID> pendingRequests = new HashSet<>();
    private static final Map<ResourceLocation, RenderType> IMAGE_RENDER_LAYERS = new HashMap<>();

    private static long lastSweep = 0;

    public PosterBlockEntityRenderer() {}

    private static RenderType getImageLayer(ResourceLocation texture) {
        return IMAGE_RENDER_LAYERS.computeIfAbsent(texture, tex ->
                RenderType.create(
                        "artistry:poster_image/" + tex,
                        DefaultVertexFormat.NEW_ENTITY,
                        VertexFormat.Mode.QUADS,
                        256,
                        false,
                        false,
                        RenderType.CompositeState.builder()
                                .setShaderState(RenderStateShard.RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                                .setTextureState(new RenderStateShard.TextureStateShard(tex, false, false))
                                .setTransparencyState(RenderStateShard.NO_TRANSPARENCY)
                                .setCullState(RenderStateShard.NO_CULL)
                                .setLightmapState(RenderStateShard.LIGHTMAP)
                                .setOverlayState(RenderStateShard.OVERLAY)
                                .setLayeringState(RenderStateShard.POLYGON_OFFSET_LAYERING)
                                .createCompositeState(false)
                )
        );
    }

    @Override
    public void render(PosterBlockEntity entity, float tickDelta, PoseStack matrices,
                       @NotNull MultiBufferSource vertexConsumers, int light, int overlay) {
        long profilerStart = ArtistryDebug.hooks().isRecording() ? System.nanoTime() : 0L;
        BlockPalette.ensureLoaded();

        long now = System.currentTimeMillis();
        List<CanvasImage> images = entity.imageLayer.getImages();
        if (!images.isEmpty()) requestMissingImages(images);

        BlockPos pos = entity.getBlockPos();
        PosterState state = STATES.computeIfAbsent(pos.asLong(), key -> new PosterState());
        state.lastSeen = now;

        float worldSize = entity.canvasWorldSize();
        int desired = PosterLod.select(state.level, cameraDistance(pos, worldSize), worldSize);
        state.update(entity, desired, now);

        if (state.hasContent) {
            matrices.pushPose();
            applyFacingRotation(matrices, entity.getBlockState().getValue(PosterBlock.FACING));
            PosterRenderHelper.renderQuad(matrices, vertexConsumers, state.slot, Z_CANVAS, light);
            if (!PosterLod.bakesImages(state.level) && entity.canvasData.isSizeChosen() && !images.isEmpty()) {
                renderImageQuads(matrices, vertexConsumers, entity.canvasData.canvasSize,
                        state.fragments(entity.imageLayer), light);
            }
            matrices.popPose();
        }

        if (profilerStart != 0L) ArtistryDebug.hooks().poster(System.nanoTime() - profilerStart);
    }

    private static double cameraDistance(BlockPos pos, float worldSize) {
        Vec3 camera = Minecraft.getInstance().gameRenderer.getMainCamera().getPosition();
        double half = worldSize * 0.5;
        double dx = pos.getX() + half - camera.x;
        double dy = pos.getY() + half - camera.y;
        double dz = pos.getZ() + half - camera.z;
        return Math.sqrt(dx * dx + dy * dy + dz * dz);
    }

    public static void tick() {
        long now = System.currentTimeMillis();
        PosterLod.refresh(now);
        sweep(now);
    }

    private static void sweep(long now) {
        if (now - lastSweep < RenderTuning.POSTER_SWEEP_INTERVAL_MILLIS) return;
        lastSweep = now;
        STATES.entrySet().removeIf(entry -> {
            if (now - entry.getValue().lastSeen <= RenderTuning.POSTER_IDLE_MILLIS) return false;
            entry.getValue().close();
            return true;
        });
        PosterLod.sweep(now);
    }

    private void requestMissingImages(List<CanvasImage> images) {
        for (CanvasImage img : images) {
            if (!ClientImageCache.has(img.uuid) && pendingRequests.add(img.uuid)) {
                ArtistryNetwork.sendToServer(new RequestImageC2SPacket(img.uuid));
            }
        }
    }

    public static void onImageReceived(UUID uuid) {
        pendingRequests.remove(uuid);
    }

    public static void clearPendingRequests(Collection<UUID> uuids) {
        pendingRequests.removeAll(uuids);
    }

    public static void clearAllPendingRequests() {
        pendingRequests.clear();
    }

    private void renderImageQuads(PoseStack matrices, MultiBufferSource vertexConsumers,
                                  int canvasSize, List<VisibleFragment> fragments, int light) {
        for (VisibleFragment frag : fragments) {
            CanvasImage img = frag.image();
            if (!ClientImageCache.has(img.uuid)) continue;

            ResourceLocation imgTex = img.pixelized
                    ? ClientImageCache.getOrBuildPixelizedTexture(img.uuid, img.gridW, img.gridH)
                    : ClientImageCache.getTexture(img.uuid);
            if (imgTex == null) continue;

            float x0 = 1f - (float) frag.destRect().x1() / canvasSize;
            float x1 = 1f - (float) frag.destRect().x0() / canvasSize;
            float y0 = 1f - (float) frag.destRect().y0() / canvasSize;
            float y1 = 1f - (float) frag.destRect().y1() / canvasSize;

            VertexConsumer vc = vertexConsumers.getBuffer(getImageLayer(imgTex));
            Matrix4f mat = matrices.last().pose();
            int ov = OverlayTexture.NO_OVERLAY;
            PosterRenderHelper.vertex(vc, mat, matrices, x0, y0, Z_IMAGES, frag.u1(), frag.v0(), ov, light);
            PosterRenderHelper.vertex(vc, mat, matrices, x1, y0, Z_IMAGES, frag.u0(), frag.v0(), ov, light);
            PosterRenderHelper.vertex(vc, mat, matrices, x1, y1, Z_IMAGES, frag.u0(), frag.v1(), ov, light);
            PosterRenderHelper.vertex(vc, mat, matrices, x0, y1, Z_IMAGES, frag.u1(), frag.v1(), ov, light);
        }
    }

    private static void applyFacingRotation(PoseStack matrices, Direction facing) {
        matrices.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case NORTH -> matrices.mulPose(new Quaternionf().rotationY(0f));
            case EAST  -> matrices.mulPose(new Quaternionf().rotationY((float) Math.toRadians(-90f)));
            case SOUTH -> matrices.mulPose(new Quaternionf().rotationY((float) Math.toRadians(180f)));
            case WEST  -> matrices.mulPose(new Quaternionf().rotationY((float) Math.toRadians(90f)));
        }
        matrices.translate(-0.5, -0.5, -0.5);
    }

    public static void invalidate(BlockPos pos) {
        PosterState state = STATES.remove(pos.asLong());
        if (state != null) state.close();
    }

    public static void clearAll() {
        for (PosterState state : STATES.values()) {
            state.close();
        }
        STATES.clear();
        PosterLod.clear();
        IMAGE_RENDER_LAYERS.clear();
        lastSweep = 0;
        RenderBudget.reset();
    }

    private record Acquired(int level, CanvasAtlas.Slot slot) {}

    private static Acquired acquire(int desired) {
        if (!RenderBudget.claim()) return null;
        long start = System.nanoTime();
        try {
            for (int level = desired; level < PosterLod.LEVELS; level++) {
                CanvasAtlas.Slot slot = PosterLod.atlas(level).allocate();
                if (slot != null) return new Acquired(level, slot);
            }
            for (int level = desired - 1; level >= 0; level--) {
                CanvasAtlas.Slot slot = PosterLod.atlas(level).allocate();
                if (slot != null) return new Acquired(level, slot);
            }
            return null;
        } finally {
            RenderBudget.charge(System.nanoTime() - start);
        }
    }

    private static final class PosterState {

        private long lastSeen = System.currentTimeMillis();
        private long lastSwitch = 0;

        private int level = -1;
        private CanvasAtlas.Slot slot;
        private boolean hasContent = false;

        private int pendingLevel = -1;
        private CanvasAtlas.Slot pendingSlot;

        private int builtCanvasRevision = -1;
        private int builtLayerRevision = -1;
        private int builtPaletteGeneration = -1;
        private long lastBuild = 0;
        private boolean builtComplete = true;

        private int fragmentsRevision = -1;
        private List<VisibleFragment> fragments = List.of();

        private void update(PosterBlockEntity entity, int desired, long now) {
            if (slot == null) {
                Acquired acquired = acquire(PosterLod.initialLevel(desired));
                if (acquired == null) return;
                level = acquired.level();
                slot = acquired.slot();
                hasContent = false;
            }

            if (!hasContent || needsRebuild(entity, now)) {
                if (build(entity, level, slot, now)) {
                    hasContent = true;
                    markBuilt(entity);
                }
                return;
            }

            if (desired == level || now - lastSwitch < RenderTuning.POSTER_MIN_SWITCH_MILLIS) {
                releasePending();
                return;
            }

            if (pendingSlot == null || pendingLevel != desired) {
                releasePending();
                Acquired acquired = acquire(desired);
                if (acquired == null || acquired.level() == level) {
                    if (acquired != null) PosterLod.atlas(acquired.level()).release(acquired.slot());
                    return;
                }
                pendingLevel = acquired.level();
                pendingSlot = acquired.slot();
            }

            if (!build(entity, pendingLevel, pendingSlot, now)) return;

            PosterLod.atlas(level).release(slot);
            level = pendingLevel;
            slot = pendingSlot;
            pendingLevel = -1;
            pendingSlot = null;
            lastSwitch = now;
            markBuilt(entity);
        }

        private boolean needsRebuild(PosterBlockEntity entity, long now) {
            if (builtPaletteGeneration != CanvasCellPainter.generation()) return true;
            if (builtCanvasRevision != entity.canvasData.revision()) return true;
            if (!PosterLod.bakesImages(level)) return false;
            if (builtLayerRevision != entity.imageLayer.revision()) return true;
            return !builtComplete && now - lastBuild >= RenderTuning.POSTER_INCOMPLETE_RETRY_MILLIS;
        }

        private boolean build(PosterBlockEntity entity, int target, CanvasAtlas.Slot targetSlot, long now) {
            if (!RenderBudget.claim()) return false;
            long start = System.nanoTime();
            int span = PosterLod.slotSize(target);

            CanvasComposer.composeCells(targetSlot.image(), targetSlot.originX(), targetSlot.originY(),
                    span, entity.canvasData);
            if (!PosterLod.bakesImages(target) || !entity.canvasData.isSizeChosen()) {
                builtComplete = true;
            } else if (!hasContent) {
                builtComplete = false;
            } else {
                builtComplete = CanvasComposer.composeFragments(targetSlot.image(),
                        targetSlot.originX(), targetSlot.originY(), span,
                        entity.canvasData.canvasSize, fragments(entity.imageLayer));
            }
            targetSlot.upload();
            RenderBudget.charge(System.nanoTime() - start);
            lastBuild = now;
            return true;
        }

        private void markBuilt(PosterBlockEntity entity) {
            builtCanvasRevision = entity.canvasData.revision();
            builtLayerRevision = entity.imageLayer.revision();
            builtPaletteGeneration = CanvasCellPainter.generation();
        }

        private List<VisibleFragment> fragments(CanvasImageLayer layer) {
            if (fragmentsRevision != layer.revision()) {
                fragmentsRevision = layer.revision();
                fragments = ImageOcclusionClipper.computeVisibleFragments(layer.getImages());
            }
            return fragments;
        }

        private void releasePending() {
            if (pendingSlot == null) return;
            PosterLod.atlas(pendingLevel).release(pendingSlot);
            pendingSlot = null;
            pendingLevel = -1;
        }

        private void close() {
            if (slot != null) {
                PosterLod.atlas(level).release(slot);
                slot = null;
                level = -1;
                hasContent = false;
            }
            releasePending();
        }
    }
}
