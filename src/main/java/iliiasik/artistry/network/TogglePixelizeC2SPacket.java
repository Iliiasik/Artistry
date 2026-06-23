package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record TogglePixelizeC2SPacket(PosterTarget target, UUID uuid) {
    public static void encode(TogglePixelizeC2SPacket p, FriendlyByteBuf buf) {
        PosterTarget.write(buf, p.target);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }
    public static TogglePixelizeC2SPacket decode(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        return new TogglePixelizeC2SPacket(target, new UUID(buf.readLong(), buf.readLong()));
    }
}