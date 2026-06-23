package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record RequestImageC2SPacket(UUID uuid) {
    public static void encode(RequestImageC2SPacket p, FriendlyByteBuf buf) {
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }
    public static RequestImageC2SPacket decode(FriendlyByteBuf buf) {
        return new RequestImageC2SPacket(new UUID(buf.readLong(), buf.readLong()));
    }
}