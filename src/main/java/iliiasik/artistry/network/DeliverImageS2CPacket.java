package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record DeliverImageS2CPacket(UUID uuid, int total, int offset, byte[] chunk) {
    public static void encode(DeliverImageS2CPacket p, FriendlyByteBuf buf) {
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeInt(p.total);
        buf.writeInt(p.offset);
        buf.writeInt(p.chunk.length);
        buf.writeBytes(p.chunk);
    }
    public static DeliverImageS2CPacket decode(FriendlyByteBuf buf) {
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        int total = buf.readInt();
        int offset = buf.readInt();
        int len = buf.readInt();
        byte[] chunk = new byte[len];
        buf.readBytes(chunk);
        return new DeliverImageS2CPacket(uuid, total, offset, chunk);
    }
}