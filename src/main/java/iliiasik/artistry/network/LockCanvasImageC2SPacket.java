package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record LockCanvasImageC2SPacket(BlockPos pos, UUID imageUuid, boolean lock) implements CustomPayload {

    public static final Id<LockCanvasImageC2SPacket> ID = new Id<>(Artistry.id("lock_canvas_image"));
    public static final PacketCodec<PacketByteBuf, LockCanvasImageC2SPacket> CODEC =
            PacketCodec.of(LockCanvasImageC2SPacket::write, LockCanvasImageC2SPacket::read);

    private static void write(LockCanvasImageC2SPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeLong(p.imageUuid.getMostSignificantBits());
        buf.writeLong(p.imageUuid.getLeastSignificantBits());
        buf.writeBoolean(p.lock);
    }

    private static LockCanvasImageC2SPacket read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new LockCanvasImageC2SPacket(pos, uuid, buf.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}