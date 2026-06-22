package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record TogglePixelizeC2SPacket(PosterTarget target, UUID uuid) implements CustomPacketPayload {

    public static final Type<TogglePixelizeC2SPacket> TYPE = new Type<>(Artistry.id("toggle_pixelize"));
    public static final StreamCodec<RegistryFriendlyByteBuf, TogglePixelizeC2SPacket> CODEC =
            StreamCodec.ofMember(TogglePixelizeC2SPacket::write, TogglePixelizeC2SPacket::read);

    private static void write(TogglePixelizeC2SPacket p, RegistryFriendlyByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }

    private static TogglePixelizeC2SPacket read(RegistryFriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.CODEC.decode(buf);
        return new TogglePixelizeC2SPacket(target, new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}