package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record DeliverImageS2CPacket(UUID uuid, byte[] bytes) implements CustomPayload {

    public static final Id<DeliverImageS2CPacket> ID = new Id<>(Artistry.id("deliver_image"));
    public static final PacketCodec<PacketByteBuf, DeliverImageS2CPacket> CODEC =
            PacketCodec.of(DeliverImageS2CPacket::write, DeliverImageS2CPacket::read);

    private static void write(DeliverImageS2CPacket p, PacketByteBuf buf) {
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeInt(p.bytes.length);
        buf.writeBytes(p.bytes);
    }

    private static DeliverImageS2CPacket read(PacketByteBuf buf) {
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new DeliverImageS2CPacket(uuid, bytes);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}