package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record DeliverImageS2CPacket(UUID uuid, byte[] bytes) {
    public static void encode(DeliverImageS2CPacket p, FriendlyByteBuf buf) {
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeInt(p.bytes.length);
        buf.writeBytes(p.bytes);
    }
    public static DeliverImageS2CPacket decode(FriendlyByteBuf buf) {
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new DeliverImageS2CPacket(uuid, bytes);
    }
}