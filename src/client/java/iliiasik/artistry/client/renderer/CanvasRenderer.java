package iliiasik.artistry.client.renderer;

import iliiasik.artistry.data.CanvasData;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.gl.RenderPipelines;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;

import java.util.UUID;

public class CanvasRenderer {

    private static final int MAX_SIZE = CanvasData.MAX_SIZE;
    private static final int TEX_SIZE = 512;
    private static final int BG_COLOR = 0xFFFDF7E8;

    private final NativeImage image;
    private final NativeImageBackedTexture texture;
    private final Identifier textureId;
    private final short[][] lastSnapshot  = new short[MAX_SIZE][MAX_SIZE];
    private final int[][]   colorSnapshot = new int[MAX_SIZE][MAX_SIZE];
    private boolean dirty = true;

    public CanvasRenderer() {
        textureId = Identifier.of("artistry", "canvas_" + UUID.randomUUID().toString().replace("-", ""));
        texture = new NativeImageBackedTexture(textureId.toString(), TEX_SIZE, TEX_SIZE, false);
        image = texture.getImage();
        CanvasCellPainter.fillCell(image, 0, 0, TEX_SIZE, BG_COLOR);
        texture.upload();
        MinecraftClient.getInstance().getTextureManager().registerTexture(textureId, texture);
    }

    public void update(CanvasData data) {
        if (!data.isSizeChosen()) return;
        iliiasik.artistry.client.palette.BlockPalette.ensureLoaded();
        int size = data.canvasSize;
        int cell = TEX_SIZE / size;
        boolean changed = false;

        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                short idx = data.pixels[y][x];
                int col = data.colors[y][x];
                if (!dirty && idx == lastSnapshot[y][x] && col == colorSnapshot[y][x]) continue;
                lastSnapshot[y][x]  = idx;
                colorSnapshot[y][x] = col;
                changed = true;
                CanvasCellPainter.paintCell(image, x * cell, y * cell, cell, idx, col, BG_COLOR);
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
}