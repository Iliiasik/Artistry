package iliiasik.artistry.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CanvasImageLayer {

    public static final int MAX_IMAGES = 4;

    private final List<CanvasImage> images = new ArrayList<>();
    private long nextSeq = 1;

    public List<CanvasImage> getImages() {
        return images;
    }

    public List<UUID> addImage(CanvasImage image) {
        image.addedSeq = nextSeq++;
        images.add(image);
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

    public boolean removeImage(UUID uuid) {
        return images.removeIf(img -> img.uuid.equals(uuid));
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
        }
    }

    public NbtList toNbt() {
        NbtList list = new NbtList();
        for (CanvasImage img : images) {
            list.add(img.toNbt());
        }
        return list;
    }

    public void fromNbt(NbtList list) {
        images.clear();
        long maxSeq = 0;
        for (NbtElement el : list) {
            if (el instanceof NbtCompound tag) {
                CanvasImage img = CanvasImage.fromNbt(tag);
                images.add(img);
                if (img.addedSeq > maxSeq) maxSeq = img.addedSeq;
            }
        }
        nextSeq = maxSeq + 1;
    }

    public void copyFrom(CanvasImageLayer other) {
        images.clear();
        for (CanvasImage img : other.images) {
            images.add(img.copy());
        }
        nextSeq = other.nextSeq;
    }
}