package iliiasik.artistry.client.ui.renderer;

import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.Sprite;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class CanvasRenderer {

    private static final int MAX_SIZE = CanvasData.MAX_SIZE;
    private static final int TEX_SIZE = 512;
    private static final int BG_COLOR = 0xFFFDF7E8;

    private final NativeImage image;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;
    private final short[][] lastSnapshot = new short[MAX_SIZE][MAX_SIZE];
    private final int[][] colorSnapshot  = new int[MAX_SIZE][MAX_SIZE];
    private boolean dirty = true;

    public CanvasRenderer() {
        textureId = Identifier.of("artistry", "canvas_" + UUID.randomUUID().toString().replace("-", ""));
        texture = new NativeImageBackedTexture(textureId.toString(), TEX_SIZE, TEX_SIZE, false);
        image = texture.getImage();
        clearImage();
        texture.upload();
        MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
    }

    private void clearImage() {
        for (int y = 0; y < TEX_SIZE; y++)
            for (int x = 0; x < TEX_SIZE; x++)
                image.setColorArgb(x, y, BG_COLOR);
    }

    public void update(CanvasData data) {
        if (!data.isSizeChosen()) return;
        BlockPalette.ensureLoaded();
        int size = data.canvasSize;
        int cell = TEX_SIZE / size;
        boolean changed = false;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                short idx = data.pixels[y][x];
                int col = data.colors[y][x];
                if (!dirty && idx == lastSnapshot[y][x] && col == colorSnapshot[y][x]) continue;
                lastSnapshot[y][x] = idx;
                colorSnapshot[y][x] = col;
                changed = true;

                int cellX = x * cell;
                int cellY = y * cell;

                if (idx == CanvasData.COLOR_PIXEL) {
                    fillCellWithColor(cellX, cellY, col, cell);
                    continue;
                }
                if (idx <= 0) {
                    fillCell(cellX, cellY, cell);
                    continue;
                }
                Sprite sp = BlockPalette.getSprite(idx);
                if (sp == null) {
                    fillCell(cellX, cellY, cell);
                    continue;
                }
                blitSprite(sp, cellX, cellY, cell);
            }
        }

        if (changed) {
            texture.upload();
            dirty = false;
        }
    }

    public void render(DrawContext ctx, int x, int y, int size) {
        ctx.drawTexture(
                RenderPipelines.GUI_TEXTURED,
                textureId,
                x, y,
                0.0F, 0.0F,
                size, size,
                TEX_SIZE, TEX_SIZE,
                TEX_SIZE, TEX_SIZE,
                0xFFFFFFFF
        );
    }

    public void close() {
        texture.close();
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
            java.lang.reflect.Field f = sp.getContents().getClass().getDeclaredField("mipmapLevelsImages");
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