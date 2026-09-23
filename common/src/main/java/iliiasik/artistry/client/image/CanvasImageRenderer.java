package iliiasik.artistry.client.image;

import com.mojang.blaze3d.systems.RenderSystem;
import iliiasik.artistry.client.presence.CanvasPresence;
import iliiasik.artistry.data.CanvasImage;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

import java.util.List;
import java.util.UUID;

public final class CanvasImageRenderer {

    private static final float LOCKED_ALPHA = 0.5f;
    private static final float BACKDROP_ALPHA = 0.7f;
    private static final int SELECTED_OUTLINE = 0xFFFFFFFF;
    private static final int OPAQUE = 0xFF000000;

    private CanvasImageRenderer() {}

    public static void renderAll(GuiGraphics ctx, List<CanvasImage> images,
                                 int drawX, int drawY, int drawSize, int canvasSize,
                                 UUID selectedUuid, UUID localPlayerUuid,
                                 CanvasPresence presence) {
        double pixelSize = (double) drawSize / canvasSize;
        boolean editing = selectedUuid != null && othersPresent(images, presence, localPlayerUuid);

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

            float alpha = alphaFor(selected, editing, lockedByOther);
            boolean faded = alpha < 1f;

            if (faded) {
                RenderSystem.enableBlend();
                RenderSystem.defaultBlendFunc();
                ctx.setColor(1f, 1f, 1f, alpha);
            }

            ctx.blit(tex, sx, sy, sw, sh, 0f, 0f, srcW, srcH, srcW, srcH);

            if (faded) {
                ctx.setColor(1f, 1f, 1f, 1f);
                RenderSystem.disableBlend();
            }

            if (lockedByOther && presence != null) {
                ctx.renderOutline(sx, sy, sw, sh, OPAQUE | presence.colorFor(img.lockedByPlayer));
            }

            if (selected) {
                ctx.renderOutline(sx, sy, sw, sh, SELECTED_OUTLINE);
                renderCornerHandles(ctx, sx, sy, sw, sh);
            }
        }
    }

    private static boolean othersPresent(List<CanvasImage> images, CanvasPresence presence, UUID localPlayerUuid) {
        if (presence != null) {
            for (CanvasPresence.RemoteCursor cursor : presence.cursors()) {
                if (!cursor.uuid.equals(localPlayerUuid)) return true;
            }
        }
        if (localPlayerUuid == null) return false;
        for (CanvasImage img : images) {
            if (img.isLockedByOther(localPlayerUuid)) return true;
        }
        return false;
    }

    private static float alphaFor(boolean selected, boolean editing, boolean lockedByOther) {
        if (selected) return 1f;
        if (editing) return BACKDROP_ALPHA;
        if (lockedByOther) return LOCKED_ALPHA;
        return 1f;
    }

    private static void renderCornerHandles(GuiGraphics ctx, int sx, int sy, int sw, int sh) {
        int hs = 4;
        ctx.fill(sx,           sy,           sx + hs,      sy + hs,      0xFFFFFFFF);
        ctx.fill(sx + sw - hs, sy,           sx + sw,      sy + hs,      0xFFFFFFFF);
        ctx.fill(sx,           sy + sh - hs, sx + hs,      sy + sh,      0xFFFFFFFF);
        ctx.fill(sx + sw - hs, sy + sh - hs, sx + sw,      sy + sh,      0xFFFFFFFF);
    }
}
