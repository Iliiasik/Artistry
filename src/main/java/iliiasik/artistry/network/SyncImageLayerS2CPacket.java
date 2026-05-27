package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasImage;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record SyncImageLayerS2CPacket(BlockPos pos, List<CanvasImage> images) implements CustomPayload {

    public static final Id<SyncImageLayerS2CPacket> ID = new Id<>(Artistry.id("sync_image_layer"));
    public static final PacketCodec<PacketByteBuf, SyncImageLayerS2CPacket> CODEC =
            PacketCodec.of(SyncImageLayerS2CPacket::write, SyncImageLayerS2CPacket::read);

    private static void write(SyncImageLayerS2CPacket p, PacketByteBuf buf) {
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
        }
    }

    private static SyncImageLayerS2CPacket read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readInt();
        List<CanvasImage> images = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            UUID uuid = new UUID(buf.readLong(), buf.readLong());
            CanvasImage img = new CanvasImage(uuid, buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
            img.pixelized = buf.readBoolean();
            images.add(img);
        }
        return new SyncImageLayerS2CPacket(pos, images);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}