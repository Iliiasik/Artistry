package iliiasik.artistry.client.renderer;

import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;

public final class CanvasCellPainter {

    private CanvasCellPainter() {}

    public static void paintCell(NativeImage image, int cx, int cy, int cell,
                                 short idx, int col, int bgColor) {
        if (idx == CanvasData.COLOR_PIXEL) {
            fillCell(image, cx, cy, cell, col);
            return;
        }
        if (idx <= 0) {
            fillCell(image, cx, cy, cell, bgColor);
            return;
        }
        Sprite sp = BlockPalette.getSprite(idx);
        if (sp == null) {
            fillCell(image, cx, cy, cell, bgColor);
            return;
        }
        blitSprite(image, sp, cx, cy, cell, bgColor);
    }

    public static void fillCell(NativeImage image, int cx, int cy, int cell, int argb) {
        for (int py = 0; py < cell; py++)
            for (int px = 0; px < cell; px++)
                image.setColorArgb(cx + px, cy + py, argb);
    }

    private static void blitSprite(NativeImage image, Sprite sp, int cellX, int cellY,
                                   int cell, int bgColor) {
        NativeImage img;
        try {
            java.lang.reflect.Field f =
                    sp.getContents().getClass().getDeclaredField("mipmapLevelsImages");
            f.setAccessible(true);
            img = ((NativeImage[]) f.get(sp.getContents()))[0];
        } catch (Exception e) {
            fillCell(image, cellX, cellY, cell, bgColor);
            return;
        }
        int sprW = sp.getContents().getWidth();
        int sprH = sp.getContents().getHeight();
        for (int py = 0; py < cell; py++)
            for (int px = 0; px < cell; px++)
                image.setColorArgb(
                        cellX + px, cellY + py,
                        img.getColorArgb(
                                Math.min(px * sprW / cell, sprW - 1),
                                Math.min(py * sprH / cell, sprH - 1)
                        )
                );
    }
}