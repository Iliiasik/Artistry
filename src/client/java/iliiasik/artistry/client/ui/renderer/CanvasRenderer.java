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

    private static final int SIZE = CanvasData.SIZE;
    private static final int TEX_SIZE = 512;
    private static final int CELL = TEX_SIZE / SIZE;
    private static final int BG_COLOR = 0xFFFDF7E8;

    private final NativeImage image;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;
    private final short[][] lastSnapshot = new short[SIZE][SIZE];
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
        for (int y = 0; y < TEX_SIZE; y++) {
            for (int x = 0; x < TEX_SIZE; x++) {
                image.setColorArgb(x, y, BG_COLOR);
            }
        }
    }

    private void blitSprite(Sprite sp, int cellX, int cellY) {
        NativeImage img = null;
        try {
            java.lang.reflect.Field f = sp.getContents().getClass().getDeclaredField("mipmapLevelsImages");
            f.setAccessible(true);
            img = ((NativeImage[]) f.get(sp.getContents()))[0];
        } catch (Exception e) {
            for (int py = 0; py < CELL; py++) {
                for (int px = 0; px < CELL; px++) {
                    image.setColorArgb(cellX + px, cellY + py, BG_COLOR);
                }
            }
            return;
        }

        int sprW = sp.getContents().getWidth();
        int sprH = sp.getContents().getHeight();

        for (int py = 0; py < CELL; py++) {
            for (int px = 0; px < CELL; px++) {
                int sx = Math.min(px * sprW / CELL, sprW - 1);
                int sy = Math.min(py * sprH / CELL, sprH - 1);
                int color = img.getColorArgb(sx, sy);
                image.setColorArgb(cellX + px, cellY + py, color);
            }
        }
    }

    public void update(CanvasData data) {
        BlockPalette.ensureLoaded();
        boolean changed = false;

        for (int y = 0; y < SIZE; y++) {
            for (int x = 0; x < SIZE; x++) {
                short idx = data.pixels[y][x];
                if (idx == lastSnapshot[y][x]) continue;
                lastSnapshot[y][x] = idx;
                changed = true;

                int cellX = x * CELL;
                int cellY = y * CELL;

                if (idx <= 0) {
                    for (int py = 0; py < CELL; py++) {
                        for (int px = 0; px < CELL; px++) {
                            image.setColorArgb(cellX + px, cellY + py, BG_COLOR);
                        }
                    }
                    continue;
                }

                Sprite sp = BlockPalette.getSprite(idx);
                if (sp == null) {
                    for (int py = 0; py < CELL; py++) {
                        for (int px = 0; px < CELL; px++) {
                            image.setColorArgb(cellX + px, cellY + py, BG_COLOR);
                        }
                    }
                    continue;
                }

                blitSprite(sp, cellX, cellY);
            }
        }

        if (changed || dirty) {
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
}