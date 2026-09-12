package iliiasik.artistry.client.image;

import iliiasik.artistry.data.CanvasImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public final class CanvasImageRenderer {

    private CanvasImageRenderer() {}

    public static void renderAll(GuiGraphics ctx, List<CanvasImage> images,
                                 int drawX, int drawY, int drawSize, int canvasSize,
                                 UUID selectedUuid, UUID localPlayerUuid) {
        double pixelSize = (double) drawSize / canvasSize;

        for (CanvasImage img : images) {
            int sx = drawX + (int)(img.gridX * pixelSize);
            int sy = drawY + (int)(img.gridY * pixelSize);
            int sw = (int)(img.gridW * pixelSize);
            int sh = (int)(img.gridH * pixelSize);

            boolean lockedByOther = localPlayerUuid != null && img.isLockedByOther(localPlayerUuid);
            boolean selected = img.uuid.equals(selectedUuid);

            ResourceLocation tex;
            if (img.pixelized) {
                tex = ClientImageCache.getOrBuildPixelizedTexture(img.uuid, img.gridW, img.gridH);
            } else {
                tex = ClientImageCache.getTexture(img.uuid);
            }
            if (tex == null) continue;

            int srcW = ClientImageCache.getWidth(img.uuid);
            int srcH = ClientImageCache.getHeight(img.uuid);

            if (lockedByOther) {
                ctx.setColor(1f, 1f, 1f, 0.5f);
            }

            ctx.blit(tex, sx, sy, sw, sh, 0f, 0f, srcW, srcH, srcW, srcH);

            if (lockedByOther) {
                ctx.setColor(1f, 1f, 1f, 1f);
                ctx.renderOutline(sx, sy, sw, sh, 0xFFFF4444);
            }

            if (selected) {
                ctx.renderOutline(sx, sy, sw, sh, 0xFFFFFFFF);
                renderCornerHandles(ctx, sx, sy, sw, sh);
            }
        }
    }

    private static void renderCornerHandles(GuiGraphics ctx, int sx, int sy, int sw, int sh) {
        int hs = 4;
        ctx.fill(sx,           sy,           sx + hs,      sy + hs,      0xFFFFFFFF);
        ctx.fill(sx + sw - hs, sy,           sx + sw,      sy + hs,      0xFFFFFFFF);
        ctx.fill(sx,           sy + sh - hs, sx + hs,      sy + sh,      0xFFFFFFFF);
        ctx.fill(sx + sw - hs, sy + sh - hs, sx + sw,      sy + sh,      0xFFFFFFFF);
    }
}
