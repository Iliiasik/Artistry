package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record DeleteCanvasImageC2SPacket(PosterTarget target, UUID uuid) {
    public static void encode(DeleteCanvasImageC2SPacket p, FriendlyByteBuf buf) {
        PosterTarget.write(buf, p.target);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }
    public static DeleteCanvasImageC2SPacket decode(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        return new DeleteCanvasImageC2SPacket(target, new UUID(buf.readLong(), buf.readLong()));
    }
}