package iliiasik.artistry.data;

import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class CanvasImageLayer {

    private final List<CanvasImage> images = new ArrayList<>();

    public List<CanvasImage> getImages() {
        return images;
    }

    public void addImage(CanvasImage image) {
        images.add(image);
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
        for (NbtElement el : list) {
            if (el instanceof NbtCompound tag) {
                images.add(CanvasImage.fromNbt(tag));
            }
        }
    }

    public void copyFrom(CanvasImageLayer other) {
        images.clear();
        for (CanvasImage img : other.images) {
            images.add(img.copy());
        }
    }
}