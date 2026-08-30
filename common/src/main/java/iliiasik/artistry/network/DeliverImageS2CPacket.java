package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record DeliverImageS2CPacket(UUID uuid, byte[] bytes) implements CustomPacketPayload {

    public static final Type<DeliverImageS2CPacket> TYPE = new Type<>(Artistry.id("deliver_image"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeliverImageS2CPacket> CODEC =
            StreamCodec.ofMember(DeliverImageS2CPacket::write, DeliverImageS2CPacket::read);

    private static void write(DeliverImageS2CPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeInt(p.bytes.length);
        buf.writeBytes(p.bytes);
    }

    private static DeliverImageS2CPacket read(RegistryFriendlyByteBuf buf) {
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new DeliverImageS2CPacket(uuid, bytes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}