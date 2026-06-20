package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record TogglePixelizeC2SPacket(PosterTarget target, UUID uuid) implements CustomPayload {

    public static final Id<TogglePixelizeC2SPacket> ID = new Id<>(Artistry.id("toggle_pixelize"));
    public static final PacketCodec<PacketByteBuf, TogglePixelizeC2SPacket> CODEC =
            PacketCodec.of(TogglePixelizeC2SPacket::write, TogglePixelizeC2SPacket::read);

    private static void write(TogglePixelizeC2SPacket p, PacketByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }

    private static TogglePixelizeC2SPacket read(PacketByteBuf buf) {
        PosterTarget target = PosterTarget.CODEC.decode(buf);
        return new TogglePixelizeC2SPacket(target, new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}