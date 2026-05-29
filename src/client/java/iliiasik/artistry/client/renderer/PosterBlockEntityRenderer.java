package iliiasik.artistry.client.renderer;

import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.image.ClientImageCache;
import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasImage;
import iliiasik.artistry.network.RequestImageC2SPacket;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.RenderPhase;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.VertexFormat;
import net.minecraft.client.render.VertexFormats;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PosterBlockEntityRenderer implements BlockEntityRenderer<PosterBlockEntity> {

    private static final float Z_CANVAS = 15f / 16f - 0.001f;
    private static final float Z_IMAGES = 15f / 16f - 0.001f;
    private static final Map<Long, PosterTexture> CACHE = new HashMap<>();
    private static final Set<UUID> pendingRequests = new HashSet<>();
    private static final Map<Identifier, RenderLayer> IMAGE_RENDER_LAYERS = new HashMap<>();

    public PosterBlockEntityRenderer() {}

    private static RenderLayer getImageLayer(Identifier texture) {
        return IMAGE_RENDER_LAYERS.computeIfAbsent(texture, tex ->
                RenderLayer.of(
                        "artistry:poster_image/" + tex,
                        VertexFormats.POSITION_COLOR_TEXTURE_OVERLAY_LIGHT_NORMAL,
                        VertexFormat.DrawMode.QUADS,
                        256,
                        false,
                        false,
                        RenderLayer.MultiPhaseParameters.builder()
                                .program(RenderPhase.ENTITY_CUTOUT_NONULL_PROGRAM)
                                .texture(new RenderPhase.Texture(tex, false, false))
                                .transparency(RenderPhase.NO_TRANSPARENCY)
                                .cull(RenderPhase.DISABLE_CULLING)
                                .lightmap(RenderPhase.ENABLE_LIGHTMAP)
                                .overlay(RenderPhase.ENABLE_OVERLAY_COLOR)
                                .layering(RenderPhase.POLYGON_OFFSET_LAYERING)
                                .build(false)
                )
        );
    }

    @Override
    public void render(PosterBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockPalette.ensureLoaded();

        PosterTexture tex = CACHE.computeIfAbsent(entity.getPos().asLong(), k -> new PosterTexture());
        tex.update(entity.canvasData);

        Direction facing = entity.getCachedState().get(PosterBlock.FACING);

        matrices.push();
        applyFacingRotation(matrices, facing);
        PosterRenderHelper.renderQuad(matrices, vertexConsumers, tex.getTextureId(), Z_CANVAS, light);

        if (entity.canvasData.isSizeChosen() && !entity.imageLayer.getImages().isEmpty()) {
            if (vertexConsumers instanceof VertexConsumerProvider.Immediate immediate) {
                immediate.draw(RenderLayer.getEntityCutout(tex.getTextureId()));
            }

            int canvasSize = entity.canvasData.canvasSize;
            for (CanvasImage img : entity.imageLayer.getImages()) {
                if (!ClientImageCache.has(img.uuid)) {
                    if (!pendingRequests.contains(img.uuid)) {
                        pendingRequests.add(img.uuid);
                        ClientPlayNetworking.send(new RequestImageC2SPacket(img.uuid));
                    }
                    continue;
                }

                Identifier imgTex = img.pixelized
                        ? ClientImageCache.getOrBuildPixelizedTexture(img.uuid, img.gridW, img.gridH)
                        : ClientImageCache.getTexture(img.uuid);
                if (imgTex == null) continue;

                float x0 = (float) img.gridX / canvasSize;
                float x1 = (float)(img.gridX + img.gridW) / canvasSize;
                float y0 = (float) img.gridY / canvasSize;
                float y1 = (float)(img.gridY + img.gridH) / canvasSize;

                renderImageQuad(matrices, vertexConsumers, imgTex, x0, y0, x1, y1, light);
            }
        }

        matrices.pop();
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

    private void renderImageQuad(MatrixStack matrices, VertexConsumerProvider vertexConsumers,
                                 Identifier tex, float x0, float y0, float x1, float y1,
                                 int light) {
        VertexConsumer vc = vertexConsumers.getBuffer(getImageLayer(tex));
        Matrix4f mat = matrices.peek().getPositionMatrix();
        int ov = OverlayTexture.DEFAULT_UV;

        float mx0 = 1f - x1;
        float mx1 = 1f - x0;

        vc.vertex(mat, mx0, 1f - y0, Z_IMAGES).texture(1f, 0f).color(255,255,255,255).overlay(ov).light(light).normal(matrices.peek(), 0,0,1);
        vc.vertex(mat, mx1, 1f - y0, Z_IMAGES).texture(0f, 0f).color(255,255,255,255).overlay(ov).light(light).normal(matrices.peek(), 0,0,1);
        vc.vertex(mat, mx1, 1f - y1, Z_IMAGES).texture(0f, 1f).color(255,255,255,255).overlay(ov).light(light).normal(matrices.peek(), 0,0,1);
        vc.vertex(mat, mx0, 1f - y1, Z_IMAGES).texture(1f, 1f).color(255,255,255,255).overlay(ov).light(light).normal(matrices.peek(), 0,0,1);
    }

    private static void applyFacingRotation(MatrixStack matrices, Direction facing) {
        matrices.translate(0.5, 0.5, 0.5);
        switch (facing) {
            case NORTH -> matrices.multiply(new Quaternionf().rotationY(0f));
            case EAST  -> matrices.multiply(new Quaternionf().rotationY((float) Math.toRadians(-90f)));
            case SOUTH -> matrices.multiply(new Quaternionf().rotationY((float) Math.toRadians(180f)));
            case WEST  -> matrices.multiply(new Quaternionf().rotationY((float) Math.toRadians(90f)));
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
            Identifier id = Identifier.of("artistry", "poster_" + UUID.randomUUID().toString().replace("-", ""));
            holder = new CanvasTextureHolder(id, TEX_SIZE);
        }

        public Identifier getTextureId() { return holder.getTextureId(); }

        public void update(CanvasData data) {
            BlockPalette.ensureLoaded();
            holder.updatePixels(data, TEX_SIZE);
        }

        public void close() { holder.close(); }
    }
}