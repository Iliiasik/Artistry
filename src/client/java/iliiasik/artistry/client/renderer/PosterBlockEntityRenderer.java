package iliiasik.artistry.client.renderer;

import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.render.OverlayTexture;
import net.minecraft.client.render.RenderLayers;
import net.minecraft.client.render.block.entity.BlockEntityRenderer;
import net.minecraft.client.render.block.entity.state.BlockEntityRenderState;
import net.minecraft.client.render.command.OrderedRenderCommandQueue;
import net.minecraft.client.render.state.CameraRenderState;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.math.Vec3d;
import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.jspecify.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 This class is responsible for drawing and rendering
 the poster itself in the game world, not in the UI
 **/

public class PosterBlockEntityRenderer
        implements BlockEntityRenderer<PosterBlockEntity, PosterBlockEntityRenderer.PosterRenderState> {

    private static final int MAX_SIZE = CanvasData.MAX_SIZE;
    private static final int TEX_SIZE = 512;
    private static final int BG_COLOR = 0xFFFDF7E8;

    private static final float Z_CANVAS = 15f / 16f - 0.001f;

    private static final Map<Long, PosterTexture> CACHE = new HashMap<>();

    public PosterBlockEntityRenderer() {}

    public static class PosterRenderState extends BlockEntityRenderState {
        public Direction  facing     = Direction.SOUTH;
        public long       posKey     = 0L;
        public CanvasData canvasData = null;
    }

    @Override
    public PosterRenderState createRenderState() {
        return new PosterRenderState();
    }

    @Override
    public void updateRenderState(PosterBlockEntity entity, PosterRenderState state,
                                  float tickProgress, Vec3d cameraPos,
                                  net.minecraft.client.render.command.ModelCommandRenderer
                                          .@Nullable CrumblingOverlayCommand crumblingOverlay) {
        BlockEntityRenderState.updateBlockEntityRenderState(entity, state, crumblingOverlay);
        state.facing     = entity.getCachedState().get(PosterBlock.FACING);
        state.posKey     = entity.getPos().asLong();
        state.canvasData = entity.canvasData;
    }

    @Override
    public void render(PosterRenderState state, MatrixStack matrices,
                       OrderedRenderCommandQueue queue, CameraRenderState cameraState) {
        BlockPalette.ensureLoaded();

        PosterTexture tex = CACHE.computeIfAbsent(state.posKey, k -> new PosterTexture());
        tex.update(state.canvasData);

        int packedLight = state.lightmapCoordinates;

        matrices.push();
        applyFacingRotation(matrices, state.facing);

        queue.submitCustom(matrices, RenderLayers.entityCutout(tex.getTextureId()),
                (entry, vc) -> {
                    Matrix4f mat = entry.getPositionMatrix();
                    int overlay = OverlayTexture.DEFAULT_UV;

                    vc.vertex(mat, 0f, 1f, Z_CANVAS).texture(1f, 0f)
                            .color(255, 255, 255, 255).overlay(overlay)
                            .light(packedLight).normal(entry, 0, 0, 1);
                    vc.vertex(mat, 1f, 1f, Z_CANVAS).texture(0f, 0f)
                            .color(255, 255, 255, 255).overlay(overlay)
                            .light(packedLight).normal(entry, 0, 0, 1);
                    vc.vertex(mat, 1f, 0f, Z_CANVAS).texture(0f, 1f)
                            .color(255, 255, 255, 255).overlay(overlay)
                            .light(packedLight).normal(entry, 0, 0, 1);
                    vc.vertex(mat, 0f, 0f, Z_CANVAS).texture(1f, 1f)
                            .color(255, 255, 255, 255).overlay(overlay)
                            .light(packedLight).normal(entry, 0, 0, 1);
                });

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
        private final NativeImage              image;
        private final NativeImageBackedTexture texture;
        private final Identifier               textureId;
        private final short[][]                snapshot      = new short[MAX_SIZE][MAX_SIZE];
        private final int[][]                  colorSnapshot = new int[MAX_SIZE][MAX_SIZE];
        private       boolean                  dirty         = true;

        public PosterTexture() {
            String uuid = UUID.randomUUID().toString().replace("-", "");
            textureId = Identifier.of("paint", "poster_" + uuid);
            texture   = new NativeImageBackedTexture(textureId.toString(), TEX_SIZE, TEX_SIZE, false);
            image     = texture.getImage();
            fillImage();
            texture.upload();
            MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
        }

        public Identifier getTextureId() {
            return textureId;
        }

        public void update(CanvasData data) {
            if (data == null || !data.isSizeChosen()) return;
            int size = data.canvasSize;
            int cell = TEX_SIZE / size;
            boolean changed = false;
            for (int y = 0; y < size; y++) {
                for (int x = 0; x < size; x++) {
                    short idx = data.pixels[y][x];
                    int col   = data.colors[y][x];
                    if (!dirty && idx == snapshot[y][x] && col == colorSnapshot[y][x]) continue;
                    snapshot[y][x]      = idx;
                    colorSnapshot[y][x] = col;
                    changed = true;
                    int cx = x * cell, cy = y * cell;
                    if (idx == CanvasData.COLOR_PIXEL) {
                        fillCellWithColor(cx, cy, col, cell);
                        continue;
                    }
                    if (idx <= 0) { fillCell(cx, cy, cell); continue; }
                    Sprite sp = BlockPalette.getSprite(idx);
                    if (sp == null) { fillCell(cx, cy, cell); continue; }
                    blitSprite(sp, cx, cy, cell);
                }
            }
            if (changed) {
                texture.upload();
                dirty = false;
            }
        }

        public void close() {
            MinecraftClient mc = MinecraftClient.getInstance();
            if (mc != null) mc.getTextureManager().destroyTexture(textureId);
            texture.close();
        }

        private void fillImage() {
            for (int y = 0; y < TEX_SIZE; y++)
                for (int x = 0; x < TEX_SIZE; x++)
                    image.setColorArgb(x, y, BG_COLOR);
        }

        private void fillCell(int cx, int cy, int cell) {
            for (int py = 0; py < cell; py++)
                for (int px = 0; px < cell; px++)
                    image.setColorArgb(cx + px, cy + py, BG_COLOR);
        }

        private void fillCellWithColor(int cx, int cy, int argb, int cell) {
            for (int py = 0; py < cell; py++)
                for (int px = 0; px < cell; px++)
                    image.setColorArgb(cx + px, cy + py, argb);
        }

        private void blitSprite(Sprite sp, int cellX, int cellY, int cell) {
            NativeImage img;
            try {
                java.lang.reflect.Field f =
                        sp.getContents().getClass().getDeclaredField("mipmapLevelsImages");
                f.setAccessible(true);
                img = ((NativeImage[]) f.get(sp.getContents()))[0];
            } catch (Exception e) {
                fillCell(cellX, cellY, cell);
                return;
            }
            int sprW = sp.getContents().getWidth();
            int sprH = sp.getContents().getHeight();
            for (int py = 0; py < cell; py++) {
                for (int px = 0; px < cell; px++) {
                    int sx = Math.min(px * sprW / cell, sprW - 1);
                    int sy = Math.min(py * sprH / cell, sprH - 1);
                    image.setColorArgb(cellX + px, cellY + py, img.getColorArgb(sx, sy));
                }
            }
        }
    }
}