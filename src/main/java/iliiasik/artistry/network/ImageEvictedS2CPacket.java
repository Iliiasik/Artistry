package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ImageEvictedS2CPacket(List<UUID> uuids) implements CustomPayload {

    public static final Id<ImageEvictedS2CPacket> ID = new Id<>(Artistry.id("image_evicted"));
    public static final PacketCodec<PacketByteBuf, ImageEvictedS2CPacket> CODEC =
            PacketCodec.of(ImageEvictedS2CPacket::write, ImageEvictedS2CPacket::read);

    private static void write(ImageEvictedS2CPacket p, PacketByteBuf buf) {
        buf.writeInt(p.uuids.size());
        for (UUID uuid : p.uuids) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    }

    private static ImageEvictedS2CPacket read(PacketByteBuf buf) {
        int count = buf.readInt();
        List<UUID> uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(new UUID(buf.readLong(), buf.readLong()));
        }
        return new ImageEvictedS2CPacket(uuids);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}