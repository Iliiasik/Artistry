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
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.network.ModNetwork;
import iliiasik.artistry.network.RequestImageC2SPacket;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderStateShard;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.ResourceLocation;
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

    private static final Map<Long, PosterTexture> CACHE = new HashMap<>();
    private static final Set<UUID> pendingRequests = new HashSet<>();
    private static final Map<ResourceLocation, RenderType> IMAGE_RENDER_LAYERS = new HashMap<>();

    public PosterBlockEntityRenderer() {}

    private static RenderType getImageLayer(ResourceLocation texture) {
        return IMAGE_RENDER_LAYERS.computeIfAbsent(texture, Layers::image);
    }

    private static final class Layers extends RenderStateShard {
        private Layers() { super("", () -> {}, () -> {}); }

        static RenderType image(ResourceLocation tex) {
            return RenderType.create(
                    "artistry:poster_image/" + tex,
                    DefaultVertexFormat.NEW_ENTITY,
                    VertexFormat.Mode.QUADS,
                    256,
                    false,
                    false,
                    RenderType.CompositeState.builder()
                            .setShaderState(RENDERTYPE_ENTITY_CUTOUT_NO_CULL_SHADER)
                            .setTextureState(new TextureStateShard(tex, false, false))
                            .setTransparencyState(NO_TRANSPARENCY)
                            .setCullState(NO_CULL)
                            .setLightmapState(LIGHTMAP)
                            .setOverlayState(OVERLAY)
                            .setLayeringState(POLYGON_OFFSET_LAYERING)
                            .createCompositeState(false)
            );
        }
    }

    @Override
    public void render(PosterBlockEntity entity, float tickDelta, PoseStack matrices,
                       @NotNull MultiBufferSource vertexConsumers, int light, int overlay) {
        BlockPalette.ensureLoaded();

        PosterTexture tex = CACHE.computeIfAbsent(entity.getBlockPos().asLong(), k -> new PosterTexture());
        tex.update(entity.canvasData);

        Direction facing = entity.getBlockState().getValue(PosterBlock.FACING);

        matrices.pushPose();
        applyFacingRotation(matrices, facing);
        PosterRenderHelper.renderQuad(matrices, vertexConsumers, tex.getTextureId(), Z_CANVAS, light);

        if (entity.canvasData.isSizeChosen() && !entity.imageLayer.getImages().isEmpty()) {

            int canvasSize = entity.canvasData.canvasSize;
            List<CanvasImage> drawList = entity.imageLayer.getImages();
            requestMissingImages(drawList);

            List<VisibleFragment> fragments = ImageOcclusionClipper.computeVisibleFragments(drawList);
            for (VisibleFragment frag : fragments) {
                CanvasImage img = frag.image();
                if (!ClientImageCache.has(img.uuid)) continue;

                ResourceLocation imgTex = img.pixelized
                        ? ClientImageCache.getOrBuildPixelizedTexture(img.uuid, img.gridW, img.gridH)
                        : ClientImageCache.getTexture(img.uuid);
                if (imgTex == null) continue;

                float x0 = (float) frag.destRect().x0() / canvasSize;
                float x1 = (float) frag.destRect().x1() / canvasSize;
                float y0 = (float) frag.destRect().y0() / canvasSize;
                float y1 = (float) frag.destRect().y1() / canvasSize;

                renderImageQuad(matrices, vertexConsumers, imgTex,
                        x0, y0, x1, y1,
                        frag.u0(), frag.v0(), frag.u1(), frag.v1(),
                        light);
            }
        }

        matrices.popPose();
    }

    private void requestMissingImages(List<CanvasImage> images) {
        for (CanvasImage img : images) {
            if (!ClientImageCache.has(img.uuid)) {
                if (!pendingRequests.contains(img.uuid)) {
                    pendingRequests.add(img.uuid);
                    ModNetwork.sendToServer(new RequestImageC2SPacket(img.uuid));
                }
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

    private void renderImageQuad(PoseStack matrices, MultiBufferSource vertexConsumers,
                                 ResourceLocation tex, float x0, float y0, float x1, float y1,
                                 float texU0, float texV0, float texU1, float texV1,
                                 int light) {
        VertexConsumer vc = vertexConsumers.getBuffer(getImageLayer(tex));
        Matrix4f mat = matrices.last().pose();
        int ov = OverlayTexture.NO_OVERLAY;

        float mx0 = 1f - x1;
        float mx1 = 1f - x0;

        emitImageVertex(vc, mat, matrices, mx0, 1f - y0, texU1, texV0, ov, light);
        emitImageVertex(vc, mat, matrices, mx1, 1f - y0, texU0, texV0, ov, light);
        emitImageVertex(vc, mat, matrices, mx1, 1f - y1, texU0, texV1, ov, light);
        emitImageVertex(vc, mat, matrices, mx0, 1f - y1, texU1, texV1, ov, light);
    }

    private void emitImageVertex(VertexConsumer vc, Matrix4f mat, PoseStack matrices,
                                 float x, float y, float u, float v, int overlay, int light) {
        vc.vertex(mat, x, y, Z_IMAGES)
                .color(255, 255, 255, 255)
                .uv(u, v)
                .overlayCoords(overlay)
                .uv2(light)
                .normal(matrices.last().normal(), 0, 0, 1)
                .endVertex();
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
        PosterTexture tex = CACHE.remove(pos.asLong());
        if (tex != null) tex.close();
    }

    public static class PosterTexture {
        private static final int TEX_SIZE = 512;
        private final CanvasTextureHolder holder;

        public PosterTexture() {
            ResourceLocation id = new ResourceLocation(
                    "artistry", "poster_" + UUID.randomUUID().toString().replace("-", ""));
            holder = new CanvasTextureHolder(id, TEX_SIZE);
        }

        public ResourceLocation getTextureId() { return holder.getTextureId(); }

        public void update(CanvasData data) {
            BlockPalette.ensureLoaded();
            holder.updatePixels(data, TEX_SIZE);
        }

        public void close() { holder.close(); }
    }
}