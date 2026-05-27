package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record RequestImageC2SPacket(UUID uuid) implements CustomPayload {

    public static final Id<RequestImageC2SPacket> ID = new Id<>(Artistry.id("request_image"));
    public static final PacketCodec<PacketByteBuf, RequestImageC2SPacket> CODEC =
            PacketCodec.of(RequestImageC2SPacket::write, RequestImageC2SPacket::read);

    private static void write(RequestImageC2SPacket p, PacketByteBuf buf) {
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }

    private static RequestImageC2SPacket read(PacketByteBuf buf) {
        return new RequestImageC2SPacket(new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}