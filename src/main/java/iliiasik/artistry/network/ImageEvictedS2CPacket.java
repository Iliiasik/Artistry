package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ImageEvictedS2CPacket(List<UUID> uuids) {
    public static void encode(ImageEvictedS2CPacket p, FriendlyByteBuf buf) {
        buf.writeInt(p.uuids.size());
        for (UUID uuid : p.uuids) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    }
    public static ImageEvictedS2CPacket decode(FriendlyByteBuf buf) {
        int count = buf.readInt();
        List<UUID> uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(new UUID(buf.readLong(), buf.readLong()));
        }
        return new ImageEvictedS2CPacket(uuids);
    }
}