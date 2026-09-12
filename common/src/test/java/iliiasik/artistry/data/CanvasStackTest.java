package iliiasik.artistry.data;

import iliiasik.artistry.support.McFixture;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CanvasStackTest {

    @BeforeAll
    static void boot() {
        McFixture.bootstrap();
    }

    private static ItemStack withTag(CompoundTag tag) {
        ItemStack stack = new ItemStack(Items.PAPER);
        stack.setTag(tag);
        return stack;
    }

    @Test
    @DisplayName("A stack without custom data has no canvas")
    void plainStackHasNoCanvas() {
        assertFalse(CanvasData.isSizeChosenForStack(new ItemStack(Items.PAPER)));
    }

    @Test
    @DisplayName("A stack with foreign custom data has no canvas")
    void foreignDataHasNoCanvas() {
        CompoundTag tag = new CompoundTag();
        tag.putString("something", "else");
        assertFalse(CanvasData.isSizeChosenForStack(withTag(tag)));
    }

    @Test
    @DisplayName("A canvas of size zero does not count as chosen")
    void zeroSizeIsNotChosen() {
        CanvasData data = new CanvasData();
        CompoundTag tag = new CompoundTag();
        tag.put("canvas", data.toNbt());
        assertFalse(CanvasData.isSizeChosenForStack(withTag(tag)));
    }

    @Test
    @DisplayName("A canvas with a size is detected on the stack")
    void chosenSizeIsDetected() {
        CanvasData data = new CanvasData();
        data.canvasSize = 16;
        CompoundTag tag = new CompoundTag();
        tag.put("canvas", data.toNbt());
        assertTrue(CanvasData.isSizeChosenForStack(withTag(tag)));
    }

    @Test
    @DisplayName("A canvas entry of the wrong type is treated as absent")
    void deformedCanvasEntryIsIgnored() {
        CompoundTag tag = new CompoundTag();
        tag.putString("canvas", "not a compound");
        assertFalse(CanvasData.isSizeChosenForStack(withTag(tag)));
    }

    @Test
    @DisplayName("Painted item data survives a copy of the stack")
    void stackCopyKeepsCanvas() {
        CanvasData data = new CanvasData();
        data.canvasSize = 32;
        data.setColor(0, 0, 0xFF112233);
        CompoundTag tag = new CompoundTag();
        tag.put("canvas", data.toNbt());

        ItemStack copy = withTag(tag).copy();

        assertTrue(CanvasData.isSizeChosenForStack(copy));

        CompoundTag copied = copy.getTag();
        assertNotNull(copied);
        CanvasData restored = new CanvasData();
        restored.fromNbt(copied.getCompound("canvas"));

        assertTrue(restored.diff(data).isEmpty());
    }
}
