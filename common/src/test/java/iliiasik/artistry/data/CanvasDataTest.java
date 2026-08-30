package iliiasik.artistry.data;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanvasDataTest {

    private static CanvasData filled(int size) {
        CanvasData data = new CanvasData();
        data.canvasSize = size;
        for (int y = 0; y < CanvasData.MAX_SIZE; y++) {
            for (int x = 0; x < CanvasData.MAX_SIZE; x++) {
                data.pixels[y][x] = (short) ((x * 7 + y * 13) % 200);
                data.colors[y][x] = 0xFF000000 | (x << 16) | (y << 8) | 0x33;
            }
        }
        return data;
    }

    @Test
    @DisplayName("Fresh canvas has no chosen size")
    void freshCanvasHasNoSize() {
        CanvasData data = new CanvasData();
        assertEquals(0, data.canvasSize);
        assertFalse(data.isSizeChosen());
    }

    @Test
    @DisplayName("setColor marks the pixel as a raw color")
    void setColorMarksColorPixel() {
        CanvasData data = new CanvasData();
        data.setColor(4, 9, 0xFF00FF00);
        assertEquals(CanvasData.COLOR_PIXEL, data.pixels[9][4]);
        assertEquals(0xFF00FF00, data.colors[9][4]);
    }

    @Test
    @DisplayName("NBT round trip preserves every pixel and colour")
    void nbtRoundTrip() {
        CanvasData source = filled(16);
        CanvasData target = new CanvasData();
        target.fromNbt(source.toNbt());

        assertEquals(source.canvasSize, target.canvasSize);
        for (int y = 0; y < CanvasData.MAX_SIZE; y++) {
            for (int x = 0; x < CanvasData.MAX_SIZE; x++) {
                assertEquals(source.pixels[y][x], target.pixels[y][x], "pixel " + x + "," + y);
                assertEquals(source.colors[y][x], target.colors[y][x], "color " + x + "," + y);
            }
        }
    }

    @Test
    @DisplayName("Negative block indices survive the NBT round trip")
    void nbtKeepsColorPixelMarker() {
        CanvasData source = new CanvasData();
        source.canvasSize = 8;
        source.setColor(0, 0, 0xFFAABBCC);
        CanvasData target = new CanvasData();
        target.fromNbt(source.toNbt());
        assertEquals(CanvasData.COLOR_PIXEL, target.pixels[0][0]);
        assertEquals(0xFFAABBCC, target.colors[0][0]);
    }

    @Test
    @DisplayName("Empty NBT leaves the canvas untouched instead of throwing")
    void emptyNbtIsTolerated() {
        CanvasData data = filled(8);
        short before = data.pixels[3][3];
        data.fromNbt(new CompoundTag());
        assertEquals(0, data.canvasSize);
        assertEquals(before, data.pixels[3][3]);
    }

    @Test
    @DisplayName("Truncated pixel and colour arrays are rejected without corrupting the canvas")
    void deformedNbtIsRejected() {
        CanvasData data = filled(8);
        short beforePixel = data.pixels[5][5];
        int beforeColor = data.colors[5][5];

        CompoundTag broken = new CompoundTag();
        broken.putInt("size", 12);
        broken.put("p", new ByteArrayTag(new byte[7]));
        broken.put("c", new IntArrayTag(new int[3]));

        data.fromNbt(broken);

        assertEquals(12, data.canvasSize);
        assertEquals(beforePixel, data.pixels[5][5]);
        assertEquals(beforeColor, data.colors[5][5]);
    }

    @Test
    @DisplayName("copyFrom produces an independent snapshot")
    void copyFromIsDeep() {
        CanvasData source = filled(16);
        CanvasData copy = new CanvasData();
        copy.copyFrom(source);

        source.pixels[2][2] = 999;
        source.colors[2][2] = 0x12345678;
        source.canvasSize = 32;

        assertNotEquals(source.pixels[2][2], copy.pixels[2][2]);
        assertNotEquals(source.colors[2][2], copy.colors[2][2]);
        assertEquals(16, copy.canvasSize);
    }

    @Test
    @DisplayName("diff of identical canvases is empty")
    void diffOfEqualCanvasesIsEmpty() {
        CanvasData a = filled(16);
        CanvasData b = new CanvasData();
        b.copyFrom(a);
        assertTrue(a.diff(b).isEmpty());
    }

    @Test
    @DisplayName("diff reports exactly the changed pixels")
    void diffReportsChangedPixels() {
        CanvasData base = filled(16);
        CanvasData changed = new CanvasData();
        changed.copyFrom(base);
        changed.pixels[1][2] = 77;
        changed.setColor(30, 31, 0xFF010203);

        List<CanvasData.PixelChange> changes = changed.diff(base);
        assertEquals(2, changes.size());

        CanvasData.PixelChange first = changes.stream()
                .filter(c -> c.x() == 2 && c.y() == 1)
                .findFirst()
                .orElseThrow();
        assertEquals((short) 77, first.blockIndex());

        CanvasData.PixelChange second = changes.stream()
                .filter(c -> c.x() == 30 && c.y() == 31)
                .findFirst()
                .orElseThrow();
        assertEquals(CanvasData.COLOR_PIXEL, second.blockIndex());
        assertEquals(0xFF010203, second.color());
    }

    @Test
    @DisplayName("diff is symmetric in the number of reported changes")
    void diffIsSymmetric() {
        CanvasData a = filled(16);
        CanvasData b = new CanvasData();
        b.copyFrom(a);
        b.pixels[7][7] = (short) (a.pixels[7][7] + 1);
        assertEquals(a.diff(b).size(), b.diff(a).size());
    }

    @Test
    @DisplayName("only the offered canvas sizes are valid")
    void sizeValidation() {
        for (int size : CanvasData.SIZES) {
            assertTrue(CanvasData.isValidSize(size));
        }
        assertFalse(CanvasData.isValidSize(0));
        assertFalse(CanvasData.isValidSize(-8));
        assertFalse(CanvasData.isValidSize(33));
        assertFalse(CanvasData.isValidSize(1000));
    }

    @Test
    @DisplayName("bounds check rejects coordinates outside the canvas")
    void boundsCheck() {
        CanvasData data = filled(16);
        assertTrue(data.inBounds(0, 0));
        assertTrue(data.inBounds(15, 15));
        assertFalse(data.inBounds(16, 0));
        assertFalse(data.inBounds(0, 16));
        assertFalse(data.inBounds(-1, 0));
        assertFalse(data.inBounds(200, 200));
    }

    @Test
    @DisplayName("a corrupt size in nbt cannot exceed the pixel arrays")
    void corruptSizeIsClamped() {
        CompoundTag tag = new CompoundTag();
        tag.putInt("size", 1000);
        CanvasData data = new CanvasData();
        data.fromNbt(tag);
        assertEquals(CanvasData.MAX_SIZE, data.canvasSize);

        tag.putInt("size", -4);
        data.fromNbt(tag);
        assertEquals(0, data.canvasSize);
    }
}
