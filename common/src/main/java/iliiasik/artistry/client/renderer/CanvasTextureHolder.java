package iliiasik.artistry.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.debug.ArtistryDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

public class CanvasTextureHolder {

    private static final int MAX_SIZE = CanvasData.MAX_SIZE;

    private final NativeImage image;
    private final DynamicTexture texture;
    private final ResourceLocation textureId;
    private final int texSize;
    private final short[][] snapshot      = new short[MAX_SIZE][MAX_SIZE];
    private final int[][]   colorSnapshot = new int[MAX_SIZE][MAX_SIZE];
    private boolean dirty = true;
    private boolean needsRepaint = true;

    public CanvasTextureHolder(ResourceLocation textureId, int texSize) {
        this.textureId = textureId;
        this.texSize = texSize;
        image   = new NativeImage(texSize, texSize, false);
        texture = new DynamicTexture(image);
        CanvasComposer.fillBackground(image, 0, 0, texSize);
        texture.upload();
        Minecraft.getInstance().getTextureManager().register(textureId, texture);
        ArtistryDebug.hooks().textureCreated(texSize);
        ArtistryDebug.hooks().textureUploaded(texSize);
    }

    public void updatePixels(CanvasData data) {
        if (data == null || !data.isSizeChosen()) return;
        if (!needsRepaint) return;
        needsRepaint = false;

        int cellCount = data.canvasSize;
        boolean changed = false;
        for (int cellY = 0; cellY < cellCount; cellY++) {
            int top = CanvasCellPainter.edge(cellY, cellCount, texSize);
            int bottom = CanvasCellPainter.edge(cellY + 1, cellCount, texSize);
            for (int cellX = 0; cellX < cellCount; cellX++) {
                short blockIndex = data.pixels[cellY][cellX];
                int color = data.colors[cellY][cellX];
                if (!dirty && blockIndex == snapshot[cellY][cellX]
                        && color == colorSnapshot[cellY][cellX]) continue;
                snapshot[cellY][cellX] = blockIndex;
                colorSnapshot[cellY][cellX] = color;
                changed = true;
                CanvasCellPainter.paintCell(image,
                        CanvasCellPainter.edge(cellX, cellCount, texSize), top,
                        CanvasCellPainter.edge(cellX + 1, cellCount, texSize), bottom,
                        blockIndex, color, CanvasComposer.BG_COLOR);
            }
        }
        if (changed) {
            texture.upload();
            ArtistryDebug.hooks().textureUploaded(texSize);
            dirty = false;
        }
    }

    public void markDirty() {
        needsRepaint = true;
    }

    public void markPaletteChanged() {
        dirty = true;
        needsRepaint = true;
    }

    public ResourceLocation getTextureId() { return textureId; }

    public void close() {
        Minecraft.getInstance().getTextureManager().release(textureId);
        texture.close();
        ArtistryDebug.hooks().textureClosed(texSize);
    }
}