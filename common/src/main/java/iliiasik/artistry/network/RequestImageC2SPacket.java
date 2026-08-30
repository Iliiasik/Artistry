package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record RequestImageC2SPacket(UUID uuid) implements CustomPacketPayload {

    public static final Type<RequestImageC2SPacket> TYPE = new Type<>(Artistry.id("request_image"));
    public static final StreamCodec<RegistryFriendlyByteBuf, RequestImageC2SPacket> CODEC =
            StreamCodec.ofMember(RequestImageC2SPacket::write, RequestImageC2SPacket::read);

    private static void write(RequestImageC2SPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }

    private static RequestImageC2SPacket read(RegistryFriendlyByteBuf buf) {
        return new RequestImageC2SPacket(new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}