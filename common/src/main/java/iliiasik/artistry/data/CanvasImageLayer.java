package iliiasik.artistry.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.Tag;
import net.minecraft.util.Mth;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CanvasImageLayer {

    public static final int MAX_IMAGES = 4;

    private final List<CanvasImage> images = new ArrayList<>();
    private long nextSeq = 1;
    private int revision = 0;

    public List<CanvasImage> getImages() {
        return images;
    }

    public int revision() {
        return revision;
    }

    public void markChanged() {
        revision++;
    }

    public List<UUID> addImage(CanvasImage image) {
        image.addedSeq = nextSeq++;
        images.add(image);
        revision++;
        return enforceLimit();
    }

    private List<UUID> enforceLimit() {
        List<UUID> evicted = new ArrayList<>();
        while (images.size() > MAX_IMAGES) {
            CanvasImage oldest = null;
            for (CanvasImage img : images) {
                if (oldest == null || img.addedSeq < oldest.addedSeq) {
                    oldest = img;
                }
            }
            if (oldest != null) {
                images.remove(oldest);
                evicted.add(oldest.uuid);
            } else {
                break;
            }
        }
        return evicted;
    }

    public void clampToCanvas(int canvasSize) {
        if (canvasSize <= 0) return;
        int maxSpan = Math.max(CanvasImage.MIN_GRID, canvasSize);
        boolean changed = false;
        for (CanvasImage img : images) {
            int w = Mth.clamp(img.gridW, CanvasImage.MIN_GRID, maxSpan);
            int h = Mth.clamp(img.gridH, CanvasImage.MIN_GRID, maxSpan);
            int x = Mth.clamp(img.gridX, 0, Math.max(0, canvasSize - w));
            int y = Mth.clamp(img.gridY, 0, Math.max(0, canvasSize - h));
            if (w == img.gridW && h == img.gridH && x == img.gridX && y == img.gridY) continue;
            img.gridW = w;
            img.gridH = h;
            img.gridX = x;
            img.gridY = y;
            changed = true;
        }
        if (changed) revision++;
    }

    public void removeImage(UUID uuid) {
        if (images.removeIf(img -> img.uuid.equals(uuid))) revision++;
    }

    public CanvasImage findByUuid(UUID uuid) {
        for (CanvasImage img : images) {
            if (img.uuid.equals(uuid)) return img;
        }
        return null;
    }

    public void moveToTop(UUID uuid) {
        CanvasImage img = findByUuid(uuid);
        if (img != null) {
            images.remove(img);
            images.add(img);
            revision++;
        }
    }

    public ListTag toNbt() {
        ListTag list = new ListTag();
        for (CanvasImage img : images) {
            list.add(img.toNbt());
        }
        return list;
    }

    public void fromNbt(ListTag list) {
        revision++;
        images.clear();
        long maxSeq = 0;
        for (Tag el : list) {
            if (el instanceof CompoundTag tag) {
                CanvasImage img = CanvasImage.fromNbt(tag);
                images.add(img);
                if (img.addedSeq > maxSeq) maxSeq = img.addedSeq;
            }
        }
        nextSeq = maxSeq + 1;
    }

    public void copyFrom(CanvasImageLayer other) {
        revision++;
        images.clear();
        for (CanvasImage img : other.images) {
            images.add(img.copy());
        }
        nextSeq = other.nextSeq;
    }
}
