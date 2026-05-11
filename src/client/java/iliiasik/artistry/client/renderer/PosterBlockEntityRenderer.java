package iliiasik.artistry.client.renderer;

import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayer;
import net.minecraft.client.render.VertexConsumer;
import net.minecraft.client.render.VertexConsumerProvider;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class PosterBlockEntityRenderer implements BlockEntityRenderer<PosterBlockEntity> {

    private static final float Z_CANVAS = 15f / 16f - 0.001f;

    private static final Map<Long, PosterTexture> CACHE = new HashMap<>();

    public PosterBlockEntityRenderer() {}

    @Override
    public void render(PosterBlockEntity entity, float tickDelta, MatrixStack matrices,
                       VertexConsumerProvider vertexConsumers, int light, int overlay) {
        BlockPalette.ensureLoaded();

        PosterTexture tex = CACHE.computeIfAbsent(entity.getPos().asLong(), k -> new PosterTexture());
        tex.update(entity.canvasData);

        Direction facing = entity.getCachedState().get(PosterBlock.FACING);

        matrices.push();
        applyFacingRotation(matrices, facing);

        VertexConsumer vc = vertexConsumers.getBuffer(RenderLayer.getEntityCutout(tex.getTextureId()));
        Matrix4f mat = matrices.peek().getPositionMatrix();
        int ov = OverlayTexture.DEFAULT_UV;

        vc.vertex(mat, 0f, 1f, Z_CANVAS).texture(1f, 0f).color(255, 255, 255, 255).overlay(ov).light(light).normal(matrices.peek(), 0, 0, 1);
        vc.vertex(mat, 1f, 1f, Z_CANVAS).texture(0f, 0f).color(255, 255, 255, 255).overlay(ov).light(light).normal(matrices.peek(), 0, 0, 1);
        vc.vertex(mat, 1f, 0f, Z_CANVAS).texture(0f, 1f).color(255, 255, 255, 255).overlay(ov).light(light).normal(matrices.peek(), 0, 0, 1);
        vc.vertex(mat, 0f, 0f, Z_CANVAS).texture(1f, 1f).color(255, 255, 255, 255).overlay(ov).light(light).normal(matrices.peek(), 0, 0, 1);

        matrices.pop();
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