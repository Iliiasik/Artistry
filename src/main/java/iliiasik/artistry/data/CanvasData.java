package iliiasik.artistry.data;

import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtIntArray;
import net.minecraft.network.PacketByteBuf;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CanvasData {
    public static final int SIZE = 32;
    public static final short COLOR_PIXEL = Short.MIN_VALUE;

    public final short[][] pixels = new short[SIZE][SIZE];
    public final int[][] colors   = new int[SIZE][SIZE];

    public void setColor(int x, int y, int argb) {
        pixels[y][x] = COLOR_PIXEL;
        colors[y][x] = argb;
    }

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        byte[] bytes = new byte[SIZE * SIZE * 2];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                buf.putShort(pixels[y][x]);
        nbt.put("p", new NbtByteArray(bytes));

        int[] flat = new int[SIZE * SIZE];
        for (int y = 0; y < SIZE; y++)
            System.arraycopy(colors[y], 0, flat, y * 32, SIZE);
        nbt.put("c", new NbtIntArray(flat));
        return nbt;
    }

    public void fromNbt(NbtCompound nbt) {
        if (nbt.contains("p")) {
            byte[] bytes = nbt.getByteArray("p").orElse(new byte[0]);
            if (bytes.length == SIZE * SIZE * 2) {
                ByteBuffer buf = ByteBuffer.wrap(bytes);
                for (int y = 0; y < SIZE; y++)
                    for (int x = 0; x < SIZE; x++)
                        pixels[y][x] = buf.getShort();
            }
        }
        if (nbt.contains("c")) {
            int[] flat = nbt.getIntArray("c").orElse(new int[0]);
            if (flat.length == SIZE * SIZE) {
                for (int y = 0; y < SIZE; y++)
                    System.arraycopy(flat, y * 32, colors[y], 0, SIZE);
            }
        }
    }

    public void copyFrom(CanvasData other) {
        for (int y = 0; y < SIZE; y++) {
            System.arraycopy(other.pixels[y], 0, pixels[y], 0, SIZE);
            System.arraycopy(other.colors[y], 0, colors[y], 0, SIZE);
        }
    }

    public List<PixelChange> diff(CanvasData other) {
        List<PixelChange> changes = new ArrayList<>();
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                if (pixels[y][x] != other.pixels[y][x] || colors[y][x] != other.colors[y][x])
                    changes.add(new PixelChange((byte) x, (byte) y, pixels[y][x], colors[y][x]));
        return changes;
    }

    public record PixelChange(byte x, byte y, short blockIndex, int color) {
        public static void writeList(PacketByteBuf buf, List<PixelChange> changes) {
            buf.writeShort(changes.size());
            for (PixelChange c : changes) {
                buf.writeByte(c.x);
                buf.writeByte(c.y);
                buf.writeShort(c.blockIndex);
                buf.writeInt(c.color);
            }
        }

        public static List<PixelChange> readList(PacketByteBuf buf) {
            int count = buf.readShort() & 0xFFFF;
            List<PixelChange> changes = new ArrayList<>(count);
            for (int i = 0; i < count; i++)
                changes.add(new PixelChange(buf.readByte(), buf.readByte(), buf.readShort(), buf.readInt()));
            return changes;
        }
    }
}