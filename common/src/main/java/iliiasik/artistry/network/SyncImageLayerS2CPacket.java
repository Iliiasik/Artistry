package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasImage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SyncImageLayerS2CPacket(BlockPos pos, List<CanvasImage> images) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("sync_image_layer");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeInt(images.size());
        for (CanvasImage img : images) {
            buf.writeLong(img.uuid.getMostSignificantBits());
            buf.writeLong(img.uuid.getLeastSignificantBits());
            buf.writeInt(img.gridX);
            buf.writeInt(img.gridY);
            buf.writeInt(img.gridW);
            buf.writeInt(img.gridH);
            buf.writeBoolean(img.pixelized);
            boolean locked = img.lockedByPlayer != null;
            buf.writeBoolean(locked);
            if (locked) {
                buf.writeLong(img.lockedByPlayer.getMostSignificantBits());
                buf.writeLong(img.lockedByPlayer.getLeastSignificantBits());
            }
        }
    }

    public static SyncImageLayerS2CPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readInt();
        List<CanvasImage> images = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID uuid = new UUID(buf.readLong(), buf.readLong());
            CanvasImage img = new CanvasImage(uuid, buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
            img.pixelized = buf.readBoolean();
            if (buf.readBoolean()) {
                img.lockedByPlayer = new UUID(buf.readLong(), buf.readLong());
            }
            images.add(img);
        }
        return new SyncImageLayerS2CPacket(pos, images);
    }
}
