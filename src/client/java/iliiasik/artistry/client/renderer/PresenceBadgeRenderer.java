package iliiasik.artistry.client.renderer;

import iliiasik.artistry.client.presence.CanvasPresence;
import iliiasik.artistry.data.CanvasImage;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.PlayerListEntry;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

public class PresenceBadgeRenderer {

    public static void renderAll(DrawContext context, CanvasPresence presence,
                                 List<CanvasImage> images,
                                 int areaX, int areaY, int areaSize, int canvasSize,
                                 UUID localPlayerUuid) {
        MinecraftClient mc = MinecraftClient.getInstance();
        if (mc.getNetworkHandler() == null) return;
        double pixelSize = (double) areaSize / canvasSize;

        Set<UUID> shownViaLock = new HashSet<>();
        for (CanvasImage img : images) {
            UUID owner = img.lockedByPlayer;
            if (owner == null || owner.equals(localPlayerUuid)) continue;
            shownViaLock.add(owner);
            float cx = (float) (areaX + (img.gridX + img.gridW / 2.0) * pixelSize);
            float cy = (float) (areaY + (img.gridY + img.gridH / 2.0) * pixelSize);
            drawBadge(context, mc, owner, cx, cy, true);
        }

        for (CanvasPresence.RemoteCursor c : presence.cursors()) {
            if (c.uuid.equals(localPlayerUuid)) continue;
            if (shownViaLock.contains(c.uuid)) continue;
            float px = (float) (areaX + c.curGx * pixelSize);
            float py = (float) (areaY + c.curGy * pixelSize);
            drawBadge(context, mc, c.uuid, px, py, false);
        }
    }

    private static void drawBadge(DrawContext context, MinecraftClient mc, UUID uuid,
                                  float x, float y, boolean centered) {
        String name = resolveName(mc, uuid);
        if (name == null) return;
        TextRenderer tr = mc.textRenderer;
        int textW = tr.getWidth(name);
        int padX = 5;
        int padY = 3;
        int h = tr.fontHeight + padY * 2;
        int w = textW + padX * 2;
        int r = h / 2;

        int rgb = colorFor(uuid);
        int fill = (0xCC << 24) | rgb;
        int border = (0xE6 << 24) | lighten(rgb, 0.45f);
        int dot = (0xE6 << 24) | rgb;

        int bx;
        int by;
        if (centered) {
            bx = Math.round(x - w / 2.0f);
            by = Math.round(y - h / 2.0f);
        } else {
            int dotX = Math.round(x);
            int dotY = Math.round(y);
            int half = 2;
            context.fill(dotX - half, dotY - half, dotX + half, dotY + half, dot);
            bx = dotX + 5;
            by = dotY + 5;
        }

        fillRoundedRect(context, bx, by, w, h, r, fill);
        strokeRoundedRect(context, bx, by, w, h, r, border);
        context.drawText(tr, name, bx + padX, by + padY, 0xFFFFFFFF, true);
    }

    private static void fillRoundedRect(DrawContext context, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int inset = rowInset(row, h, r);
            context.fill(x + inset, y + row, x + w - inset, y + row + 1, color);
        }
    }

    private static void strokeRoundedRect(DrawContext context, int x, int y, int w, int h, int r, int color) {
        r = Math.max(0, Math.min(r, Math.min(w, h) / 2));
        for (int row = 0; row < h; row++) {
            int inset = rowInset(row, h, r);
            int outerL = x + inset;
            int outerR = x + w - inset;
            if (row >= 1 && row <= h - 2) {
                int innerInset = rowInset(row - 1, h - 2, Math.max(0, r - 1));
                int innerL = x + 1 + innerInset;
                int innerR = x + w - 1 - innerInset;
                context.fill(outerL, y + row, innerL, y + row + 1, color);
                context.fill(innerR, y + row, outerR, y + row + 1, color);
            } else {
                context.fill(outerL, y + row, outerR, y + row + 1, color);
            }
        }
    }

    private static int rowInset(int row, int h, int r) {
        if (row < r) {
            int dy = r - 1 - row;
            return r - (int) Math.floor(Math.sqrt((double) r * r - (double) dy * dy));
        } else if (row >= h - r) {
            int dy = row - (h - r);
            return r - (int) Math.floor(Math.sqrt((double) r * r - (double) dy * dy));
        }
        return 0;
    }

    private static String resolveName(MinecraftClient mc, UUID uuid) {
        PlayerListEntry entry = mc.getNetworkHandler().getPlayerListEntry(uuid);
        if (entry == null) return null;
        return entry.getProfile().getName();
    }

    private static int colorFor(UUID uuid) {
        float hue = (uuid.hashCode() & 0xFFFF) / 65535.0f;
        return hsvToRgb(hue, 0.6f, 0.9f);
    }

    private static int lighten(int rgb, float amount) {
        int r = (rgb >> 16) & 0xFF;
        int g = (rgb >> 8) & 0xFF;
        int b = rgb & 0xFF;
        r = (int) (r + (255 - r) * amount);
        g = (int) (g + (255 - g) * amount);
        b = (int) (b + (255 - b) * amount);
        return (r << 16) | (g << 8) | b;
    }

    private static int hsvToRgb(float h, float s, float v) {
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }
}