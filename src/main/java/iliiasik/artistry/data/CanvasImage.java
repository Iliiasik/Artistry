package iliiasik.artistry.data;

import net.minecraft.nbt.NbtCompound;

import java.util.UUID;

public class CanvasImage {

    public UUID uuid;
    public int gridX;
    public int gridY;
    public int gridW;
    public int gridH;
    public boolean pixelized;
    public long addedSeq;

    public static final int MIN_GRID = 3;

    public java.util.UUID lockedByPlayer = null;

    public boolean isLocked() { return lockedByPlayer != null; }
    public boolean isLockedByOther(java.util.UUID localPlayer) {
        return lockedByPlayer != null && !lockedByPlayer.equals(localPlayer);
    }

    public CanvasImage(UUID uuid, int gridX, int gridY, int gridW, int gridH) {
        this.uuid = uuid;
        this.gridX = gridX;
        this.gridY = gridY;
        this.gridW = Math.max(MIN_GRID, gridW);
        this.gridH = Math.max(MIN_GRID, gridH);
        this.pixelized = false;
        this.addedSeq = 0;
    }

    public CanvasImage copy() {
        CanvasImage c = new CanvasImage(uuid, gridX, gridY, gridW, gridH);
        c.pixelized = pixelized;
        c.addedSeq = addedSeq;
        return c;
    }

    public NbtCompound toNbt() {
        NbtCompound tag = new NbtCompound();
        tag.putLong("uuid_most", uuid.getMostSignificantBits());
        tag.putLong("uuid_least", uuid.getLeastSignificantBits());
        tag.putInt("x", gridX);
        tag.putInt("y", gridY);
        tag.putInt("w", gridW);
        tag.putInt("h", gridH);
        tag.putBoolean("pixelized", pixelized);
        tag.putLong("added_seq", addedSeq);
        return tag;
    }

    public static CanvasImage fromNbt(NbtCompound tag) {
        UUID uuid = new UUID(tag.getLong("uuid_most"), tag.getLong("uuid_least"));
        CanvasImage img = new CanvasImage(uuid,
                tag.getInt("x"), tag.getInt("y"),
                tag.getInt("w"), tag.getInt("h"));
        img.pixelized = tag.getBoolean("pixelized");
        img.addedSeq = tag.getLong("added_seq");
        return img;
    }
}