package iliiasik.artistry.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public class CanvasTextureHolder {

    private static final int MAX_SIZE = CanvasData.MAX_SIZE;
    private static final int BG_COLOR = 0xFFFDF7E8;

    private final NativeImage image;
    private final DynamicTexture texture;
    private final ResourceLocation textureId;
    private final short[][] snapshot      = new short[MAX_SIZE][MAX_SIZE];
    private final int[][]   colorSnapshot = new int[MAX_SIZE][MAX_SIZE];
    private boolean dirty = true;

    public CanvasTextureHolder(ResourceLocation textureId, int texSize) {
        this.textureId = textureId;
        image   = new NativeImage(texSize, texSize, false);
        texture = new DynamicTexture(image);
        CanvasCellPainter.fillCell(image, 0, 0, texSize, BG_COLOR);
        texture.upload();
        Minecraft.getInstance().getTextureManager().register(textureId, texture);
    }

    public void updatePixels(CanvasData data, int texSize) {
        if (data == null || !data.isSizeChosen()) return;
        int size = data.canvasSize;
        int cell = texSize / size;
        boolean changed = false;
        for (int y = 0; y < size; y++) {
            for (int x = 0; x < size; x++) {
                short idx = data.pixels[y][x];
                int col   = data.colors[y][x];
                if (!dirty && idx == snapshot[y][x] && col == colorSnapshot[y][x]) continue;
                snapshot[y][x]      = idx;
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

    public ResourceLocation getTextureId() { return textureId; }

    public void close() {
        Minecraft.getInstance().getTextureManager().release(textureId);
        texture.close();
    }
}