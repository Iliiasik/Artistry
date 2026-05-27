package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record ImageUploadedS2CPacket(BlockPos pos, UUID uuid, int gridX, int gridY, int gridW, int gridH) implements CustomPayload {

    public static final Id<ImageUploadedS2CPacket> ID = new Id<>(Artistry.id("image_uploaded"));
    public static final PacketCodec<PacketByteBuf, ImageUploadedS2CPacket> CODEC =
            PacketCodec.of(ImageUploadedS2CPacket::write, ImageUploadedS2CPacket::read);

    private static void write(ImageUploadedS2CPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeInt(p.gridX);
        buf.writeInt(p.gridY);
        buf.writeInt(p.gridW);
        buf.writeInt(p.gridH);
    }

    private static ImageUploadedS2CPacket read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new ImageUploadedS2CPacket(pos, uuid, buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}