package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record MoveCanvasImageC2SPacket(PosterTarget target, UUID uuid,
                                       int gridX, int gridY, int gridW, int gridH) {
    public static void encode(MoveCanvasImageC2SPacket p, FriendlyByteBuf buf) {
        PosterTarget.write(buf, p.target);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeInt(p.gridX);
        buf.writeInt(p.gridY);
        buf.writeInt(p.gridW);
        buf.writeInt(p.gridH);
    }
    public static MoveCanvasImageC2SPacket decode(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new MoveCanvasImageC2SPacket(target, uuid,
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }
}