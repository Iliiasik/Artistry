package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public record ImageEvictedS2CPacket(List<UUID> uuids) implements CustomPacketPayload {

    public static final Type<ImageEvictedS2CPacket> TYPE = new Type<>(Artistry.id("image_evicted"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageEvictedS2CPacket> CODEC =
            StreamCodec.ofMember(ImageEvictedS2CPacket::write, ImageEvictedS2CPacket::read);

    private static void write(ImageEvictedS2CPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeInt(p.uuids.size());
        for (UUID uuid : p.uuids) {
            buf.writeLong(uuid.getMostSignificantBits());
            buf.writeLong(uuid.getLeastSignificantBits());
        }
    }

    private static ImageEvictedS2CPacket read(RegistryFriendlyByteBuf buf) {
        int count = buf.readInt();
        List<UUID> uuids = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            uuids.add(new UUID(buf.readLong(), buf.readLong()));
        }
        return new ImageEvictedS2CPacket(uuids);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}