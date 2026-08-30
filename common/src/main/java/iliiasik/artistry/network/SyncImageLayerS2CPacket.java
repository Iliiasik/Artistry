package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasImage;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SyncImageLayerS2CPacket(BlockPos pos, List<CanvasImage> images) implements CustomPacketPayload {

    public static final Type<SyncImageLayerS2CPacket> TYPE = new Type<>(Artistry.id("sync_image_layer"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncImageLayerS2CPacket> CODEC =
            StreamCodec.ofMember(SyncImageLayerS2CPacket::write, SyncImageLayerS2CPacket::read);

    private static void write(SyncImageLayerS2CPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeInt(p.images.size());
        for (CanvasImage img : p.images) {
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

    private static SyncImageLayerS2CPacket read(RegistryFriendlyByteBuf buf) {
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

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}