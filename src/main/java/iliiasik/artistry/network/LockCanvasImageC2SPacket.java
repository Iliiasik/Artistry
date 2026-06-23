package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record LockCanvasImageC2SPacket(PosterTarget target, UUID imageUuid, boolean lock) {
    public static void encode(LockCanvasImageC2SPacket p, FriendlyByteBuf buf) {
        PosterTarget.write(buf, p.target);
        buf.writeLong(p.imageUuid.getMostSignificantBits());
        buf.writeLong(p.imageUuid.getLeastSignificantBits());
        buf.writeBoolean(p.lock);
    }
    public static LockCanvasImageC2SPacket decode(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new LockCanvasImageC2SPacket(target, uuid, buf.readBoolean());
    }
}