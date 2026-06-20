package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record LockCanvasImageC2SPacket(PosterTarget target, UUID imageUuid, boolean lock) implements CustomPayload {

    public static final Id<LockCanvasImageC2SPacket> ID = new Id<>(Artistry.id("lock_canvas_image"));
    public static final PacketCodec<PacketByteBuf, LockCanvasImageC2SPacket> CODEC =
            PacketCodec.of(LockCanvasImageC2SPacket::write, LockCanvasImageC2SPacket::read);

    private static void write(LockCanvasImageC2SPacket p, PacketByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
        buf.writeLong(p.imageUuid.getMostSignificantBits());
        buf.writeLong(p.imageUuid.getLeastSignificantBits());
        buf.writeBoolean(p.lock);
    }

    private static LockCanvasImageC2SPacket read(PacketByteBuf buf) {
        PosterTarget target = PosterTarget.CODEC.decode(buf);
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new LockCanvasImageC2SPacket(target, uuid, buf.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}