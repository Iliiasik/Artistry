package iliiasik.artistry.data;

import net.minecraft.nbt.ByteArrayTag;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.IntArrayTag;
import net.minecraft.network.FriendlyByteBuf;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CanvasData {
    public static final int MAX_SIZE = 32;
    public static final short COLOR_PIXEL = Short.MIN_VALUE;

    public int canvasSize = 0;

    public final short[][] pixels = new short[MAX_SIZE][MAX_SIZE];
    public final int[][] colors   = new int[MAX_SIZE][MAX_SIZE];

    public boolean isSizeChosen() {
        return canvasSize > 0;
    }

    public static boolean isSizeChosenForStack(net.minecraft.world.item.ItemStack stack) {
        CompoundTag nbt = stack.getTag();
        if (nbt == null || !nbt.contains("canvas")) return false;
        return nbt.getCompound("canvas").getInt("size") > 0;
    }

    public void setColor(int x, int y, int argb) {
        pixels[y][x] = COLOR_PIXEL;
        colors[y][x] = argb;
    }

    public CompoundTag toNbt() {
        CompoundTag nbt = new CompoundTag();
        nbt.putInt("size", canvasSize);
        byte[] bytes = new byte[MAX_SIZE * MAX_SIZE * 2];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        for (int y = 0; y < MAX_SIZE; y++)
            for (int x = 0; x < MAX_SIZE; x++)
                buf.putShort(pixels[y][x]);
        nbt.put("p", new ByteArrayTag(bytes));
        int[] flat = new int[MAX_SIZE * MAX_SIZE];
        for (int y = 0; y < MAX_SIZE; y++)
            System.arraycopy(colors[y], 0, flat, y * MAX_SIZE, MAX_SIZE);
        nbt.put("c", new IntArrayTag(flat));
        return nbt;
    }

    public void fromNbt(CompoundTag nbt) {
        canvasSize = nbt.getInt("size");
        if (nbt.contains("p")) {
            byte[] bytes = nbt.getByteArray("p");
            if (bytes.length == MAX_SIZE * MAX_SIZE * 2) {
                ByteBuffer buf = ByteBuffer.wrap(bytes);
                for (int y = 0; y < MAX_SIZE; y++)
                    for (int x = 0; x < MAX_SIZE; x++)
                        pixels[y][x] = buf.getShort();
            }
        }
        if (nbt.contains("c")) {
            int[] flat = nbt.getIntArray("c");
            if (flat.length == MAX_SIZE * MAX_SIZE) {
                for (int y = 0; y < MAX_SIZE; y++)
                    System.arraycopy(flat, y * MAX_SIZE, colors[y], 0, MAX_SIZE);
            }
        }
    }

    public void copyFrom(CanvasData other) {
        this.canvasSize = other.canvasSize;
        for (int y = 0; y < MAX_SIZE; y++) {
            System.arraycopy(other.pixels[y], 0, pixels[y], 0, MAX_SIZE);
            System.arraycopy(other.colors[y], 0, colors[y], 0, MAX_SIZE);
        }
    }

    public List<PixelChange> diff(CanvasData other) {
        List<PixelChange> changes = new ArrayList<>();
        for (int y = 0; y < MAX_SIZE; y++)
            for (int x = 0; x < MAX_SIZE; x++)
                if (pixels[y][x] != other.pixels[y][x] || colors[y][x] != other.colors[y][x])
                    changes.add(new PixelChange((byte) x, (byte) y, pixels[y][x], colors[y][x]));
        return changes;
    }

    public record PixelChange(byte x, byte y, short blockIndex, int color) {
        public static void writeList(FriendlyByteBuf buf, List<PixelChange> changes) {
            buf.writeShort(changes.size());
            for (PixelChange c : changes) {
                buf.writeByte(c.x);
                buf.writeByte(c.y);
                buf.writeShort(c.blockIndex);
                buf.writeInt(c.color);
            }
        }

        public static List<PixelChange> readList(FriendlyByteBuf buf) {
            int count = buf.readShort() & 0xFFFF;
            List<PixelChange> changes = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
                changes.add(new PixelChange(buf.readByte(), buf.readByte(), buf.readShort(), buf.readInt()));
            return changes;
        }
    }
}