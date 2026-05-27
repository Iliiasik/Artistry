package iliiasik.artistry.client.image;

import iliiasik.artistry.data.CanvasImage;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.util.Identifier;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

public final class CanvasImageRenderer {

    private static final int PIXELIZE_SUBDIVISIONS = 8;

    private CanvasImageRenderer() {}

    public static void renderAll(DrawContext ctx, List<CanvasImage> images,
                                 int drawX, int drawY, int drawSize, int canvasSize,
                                 UUID selectedUuid) {
        double pixelSize = (double) drawSize / canvasSize;

        for (CanvasImage img : images) {
            int sx = drawX + (int)(img.gridX * pixelSize);
            int sy = drawY + (int)(img.gridY * pixelSize);
            int sw = (int)(img.gridW * pixelSize);
            int sh = (int)(img.gridH * pixelSize);

            Identifier tex = ClientImageCache.getTexture(img.uuid);
            if (tex == null) continue;

            if (img.pixelized) {
                renderPixelized(ctx, img.uuid, sx, sy, sw, sh, img.gridW, img.gridH);
            } else {
                ctx.drawTexture(tex, sx, sy, sw, sh, 0f, 0f,
                        ClientImageCache.getWidth(img.uuid),
                        ClientImageCache.getHeight(img.uuid),
                        ClientImageCache.getWidth(img.uuid),
                        ClientImageCache.getHeight(img.uuid));
            }

            if (img.uuid.equals(selectedUuid)) {
                ctx.drawBorder(sx, sy, sw, sh, 0xFFFFFFFF);
                renderCornerHandles(ctx, sx, sy, sw, sh);
            }
        }
    }

    private static void renderPixelized(DrawContext ctx, UUID uuid,
                                        int sx, int sy, int sw, int sh,
                                        int gridW, int gridH) {
        byte[] bytes = getRawBytes(uuid);
        if (bytes == null) return;

        try {
            NativeImage src = NativeImage.read(new ByteArrayInputStream(bytes));
            int srcW = src.getWidth();
            int srcH = src.getHeight();

            int blockW = gridW * PIXELIZE_SUBDIVISIONS;
            int blockH = gridH * PIXELIZE_SUBDIVISIONS;

            for (int bx = 0; bx < blockW; bx++) {
                for (int by = 0; by < blockH; by++) {
                    int px = (int)((float) bx / blockW * srcW);
                    int py = (int)((float) by / blockH * srcH);
                    px = Math.min(px, srcW - 1);
                    py = Math.min(py, srcH - 1);

                    int abgr = src.getColor(px, py);
                    int argb = abgrToArgb(abgr);

                    int x0 = sx + bx * sw / blockW;
                    int y0 = sy + by * sh / blockH;
                    int x1 = sx + (bx + 1) * sw / blockW;
                    int y1 = sy + (by + 1) * sh / blockH;

                    ctx.fill(x0, y0, x1, y1, argb);
                }
            }

            src.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static int abgrToArgb(int abgr) {
        int a = (abgr >> 24) & 0xFF;
        int b = (abgr >> 16) & 0xFF;
        int g = (abgr >> 8)  & 0xFF;
        int r =  abgr        & 0xFF;
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private static byte[] getRawBytes(UUID uuid) {
        return ClientImageCache.getRawBytes(uuid);
    }

    private static void renderCornerHandles(DrawContext ctx, int sx, int sy, int sw, int sh) {
        int hs = 4;
        ctx.fill(sx,          sy,          sx + hs,      sy + hs,      0xFFFFFFFF);
        ctx.fill(sx + sw - hs, sy,          sx + sw,      sy + hs,      0xFFFFFFFF);
        ctx.fill(sx,          sy + sh - hs, sx + hs,      sy + sh,      0xFFFFFFFF);
        ctx.fill(sx + sw - hs, sy + sh - hs, sx + sw,     sy + sh,      0xFFFFFFFF);
    }
}