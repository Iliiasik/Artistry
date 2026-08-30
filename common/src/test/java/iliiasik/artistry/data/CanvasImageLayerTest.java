package iliiasik.artistry.data;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanvasImageLayerTest {

    private static CanvasImage image(int x, int y, int w, int h) {
        return new CanvasImage(UUID.randomUUID(), x, y, w, h);
    }

    @Test
    @DisplayName("Image dimensions are clamped to the minimum grid size")
    void dimensionsAreClamped() {
        CanvasImage img = new CanvasImage(UUID.randomUUID(), 0, 0, 1, 0);
        assertEquals(CanvasImage.MIN_GRID, img.gridW);
        assertEquals(CanvasImage.MIN_GRID, img.gridH);
    }

    @Test
    @DisplayName("Image NBT round trip preserves every field")
    void imageNbtRoundTrip() {
        CanvasImage img = image(3, 4, 8, 9);
        img.pixelized = true;
        img.addedSeq = 42;

        CanvasImage restored = CanvasImage.fromNbt(img.toNbt());

        assertEquals(img.uuid, restored.uuid);
        assertEquals(img.gridX, restored.gridX);
        assertEquals(img.gridY, restored.gridY);
        assertEquals(img.gridW, restored.gridW);
        assertEquals(img.gridH, restored.gridH);
        assertTrue(restored.pixelized);
        assertEquals(42, restored.addedSeq);
    }

    @Test
    @DisplayName("copy is independent from the original")
    void copyIsIndependent() {
        CanvasImage img = image(1, 1, 5, 5);
        CanvasImage copy = img.copy();
        img.gridX = 20;
        img.pixelized = true;

        assertNotSame(img, copy);
        assertEquals(1, copy.gridX);
        assertFalse(copy.pixelized);
    }

    @Test
    @DisplayName("Lock state distinguishes the owner from other players")
    void lockOwnership() {
        CanvasImage img = image(0, 0, 4, 4);
        UUID owner = UUID.randomUUID();
        UUID other = UUID.randomUUID();

        assertFalse(img.isLocked());
        assertFalse(img.isLockedByOther(owner));

        img.lockedByPlayer = owner;
        assertTrue(img.isLocked());
        assertFalse(img.isLockedByOther(owner));
        assertTrue(img.isLockedByOther(other));
    }

    @Test
    @DisplayName("Added images receive increasing sequence numbers")
    void addAssignsIncreasingSequence() {
        CanvasImageLayer layer = new CanvasImageLayer();
        CanvasImage first = image(0, 0, 4, 4);
        CanvasImage second = image(1, 1, 4, 4);
        layer.addImage(first);
        layer.addImage(second);

        assertTrue(second.addedSeq > first.addedSeq);
    }

    @Test
    @DisplayName("Adding past the limit evicts the oldest images")
    void limitEvictsOldest() {
        CanvasImageLayer layer = new CanvasImageLayer();
        CanvasImage[] added = new CanvasImage[CanvasImageLayer.MAX_IMAGES + 2];
        for (int i = 0; i < added.length; i++) {
            added[i] = image(i, i, 4, 4);
        }

        for (int i = 0; i < CanvasImageLayer.MAX_IMAGES; i++) {
            assertTrue(layer.addImage(added[i]).isEmpty());
        }

        List<UUID> evictedFirst = layer.addImage(added[CanvasImageLayer.MAX_IMAGES]);
        assertEquals(List.of(added[0].uuid), evictedFirst);

        List<UUID> evictedSecond = layer.addImage(added[CanvasImageLayer.MAX_IMAGES + 1]);
        assertEquals(List.of(added[1].uuid), evictedSecond);

        assertEquals(CanvasImageLayer.MAX_IMAGES, layer.getImages().size());
        assertNull(layer.findByUuid(added[0].uuid));
        assertNull(layer.findByUuid(added[1].uuid));
    }

    @Test
    @DisplayName("findByUuid and removeImage work on the exact instance")
    void findAndRemove() {
        CanvasImageLayer layer = new CanvasImageLayer();
        CanvasImage img = image(2, 2, 6, 6);
        layer.addImage(img);

        assertEquals(img, layer.findByUuid(img.uuid));
        assertNull(layer.findByUuid(UUID.randomUUID()));

        layer.removeImage(img.uuid);
        assertTrue(layer.getImages().isEmpty());
        layer.removeImage(img.uuid);
        assertTrue(layer.getImages().isEmpty());
    }

    @Test
    @DisplayName("moveToTop puts the image last in the draw order")
    void moveToTopReorders() {
        CanvasImageLayer layer = new CanvasImageLayer();
        CanvasImage bottom = image(0, 0, 4, 4);
        CanvasImage middle = image(1, 1, 4, 4);
        CanvasImage top = image(2, 2, 4, 4);
        layer.addImage(bottom);
        layer.addImage(middle);
        layer.addImage(top);

        layer.moveToTop(bottom.uuid);

        List<CanvasImage> order = layer.getImages();
        assertEquals(middle, order.get(0));
        assertEquals(top, order.get(1));
        assertEquals(bottom, order.get(2));

        layer.moveToTop(UUID.randomUUID());
        assertEquals(3, layer.getImages().size());
    }

    @Test
    @DisplayName("Layer NBT round trip preserves order and sequence numbers")
    void layerNbtRoundTrip() {
        CanvasImageLayer layer = new CanvasImageLayer();
        CanvasImage a = image(0, 0, 4, 4);
        CanvasImage b = image(5, 5, 7, 7);
        b.pixelized = true;
        layer.addImage(a);
        layer.addImage(b);

        CanvasImageLayer restored = new CanvasImageLayer();
        restored.fromNbt(layer.toNbt());

        assertEquals(2, restored.getImages().size());
        assertEquals(a.uuid, restored.getImages().get(0).uuid);
        assertEquals(b.uuid, restored.getImages().get(1).uuid);
        assertTrue(restored.getImages().get(1).pixelized);

        CanvasImage next = image(9, 9, 4, 4);
        restored.addImage(next);
        assertTrue(next.addedSeq > b.addedSeq);
    }

    @Test
    @DisplayName("An empty or foreign NBT list yields an empty layer")
    void emptyNbtYieldsEmptyLayer() {
        CanvasImageLayer layer = new CanvasImageLayer();
        layer.addImage(image(0, 0, 4, 4));

        layer.fromNbt(new ListTag());
        assertTrue(layer.getImages().isEmpty());

        ListTag garbage = new ListTag();
        garbage.add(new CompoundTag());
        layer.fromNbt(garbage);
        assertEquals(1, layer.getImages().size());
        assertEquals(CanvasImage.MIN_GRID, layer.getImages().get(0).gridW);
    }

    @Test
    @DisplayName("copyFrom clones the images instead of sharing them")
    void copyFromClonesImages() {
        CanvasImageLayer source = new CanvasImageLayer();
        CanvasImage img = image(1, 2, 5, 6);
        source.addImage(img);

        CanvasImageLayer target = new CanvasImageLayer();
        target.copyFrom(source);

        img.gridX = 30;
        assertEquals(1, target.getImages().get(0).gridX);
        assertNotSame(img, target.getImages().get(0));
    }

    @Test
    @DisplayName("clampToCanvas pulls oversized images back inside the canvas")
    void clampPullsImagesInside() {
        CanvasImageLayer layer = new CanvasImageLayer();
        CanvasImage oversized = image(30, 30, 20, 20);
        CanvasImage negative = image(-5, -5, 4, 4);
        layer.addImage(oversized);
        layer.addImage(negative);

        layer.clampToCanvas(32);

        for (CanvasImage img : layer.getImages()) {
            assertTrue(img.gridX >= 0 && img.gridY >= 0);
            assertTrue(img.gridX + img.gridW <= 32);
            assertTrue(img.gridY + img.gridH <= 32);
            assertTrue(img.gridW >= CanvasImage.MIN_GRID);
            assertTrue(img.gridH >= CanvasImage.MIN_GRID);
        }
    }

    @Test
    @DisplayName("clampToCanvas leaves valid images and the revision untouched")
    void clampKeepsValidImages() {
        CanvasImageLayer layer = new CanvasImageLayer();
        layer.addImage(image(4, 4, 8, 8));
        int revision = layer.revision();

        layer.clampToCanvas(32);

        CanvasImage img = layer.getImages().get(0);
        assertEquals(4, img.gridX);
        assertEquals(8, img.gridW);
        assertEquals(revision, layer.revision());
    }

    @Test
    @DisplayName("clampToCanvas bumps the revision when it changes something")
    void clampBumpsRevision() {
        CanvasImageLayer layer = new CanvasImageLayer();
        layer.addImage(image(30, 0, 20, 4));
        int revision = layer.revision();

        layer.clampToCanvas(32);

        assertNotEquals(revision, layer.revision());
    }

    @Test
    @DisplayName("clampToCanvas ignores a canvas without a chosen size")
    void clampIgnoresUnsizedCanvas() {
        CanvasImageLayer layer = new CanvasImageLayer();
        layer.addImage(image(30, 30, 20, 20));

        layer.clampToCanvas(0);

        assertEquals(30, layer.getImages().get(0).gridX);
    }
}
