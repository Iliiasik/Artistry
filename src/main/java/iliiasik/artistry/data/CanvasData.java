package iliiasik.artistry.data;

import net.minecraft.nbt.NbtByteArray;
import net.minecraft.nbt.NbtCompound;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

public class CanvasData {
    public static final int SIZE = 32;
    public final short[][] pixels = new short[SIZE][SIZE];

    public NbtCompound toNbt() {
        NbtCompound nbt = new NbtCompound();
        byte[] bytes = new byte[SIZE * SIZE * 2];
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                buf.putShort(pixels[y][x]);
        nbt.put("p", new NbtByteArray(bytes));
        return nbt;
    }

    public void fromNbt(NbtCompound nbt) {
        if (!nbt.contains("p")) return;
        byte[] bytes = nbt.getByteArray("p").orElse(new byte[0]);
        if (bytes.length != SIZE * SIZE * 2) return;
        ByteBuffer buf = ByteBuffer.wrap(bytes);
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                pixels[y][x] = buf.getShort();
    }

    public void copyFrom(CanvasData other) {
        for (int y = 0; y < SIZE; y++)
            System.arraycopy(other.pixels[y], 0, pixels[y], 0, SIZE);
    }

    public List<PixelChange> diff(CanvasData oldData) {
        List<PixelChange> changes = new ArrayList<>();
        for (int y = 0; y < SIZE; y++)
            for (int x = 0; x < SIZE; x++)
                if (pixels[y][x] != oldData.pixels[y][x])
                    changes.add(new PixelChange((byte) x, (byte) y, pixels[y][x]));
        return changes;
    }

    public record PixelChange(byte x, byte y, short blockIndex) {}
}