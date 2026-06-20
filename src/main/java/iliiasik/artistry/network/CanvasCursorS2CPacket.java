package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record CanvasCursorS2CPacket(BlockPos pos, UUID uuid, short gx, short gy) implements CustomPayload {

    public static final Id<CanvasCursorS2CPacket> ID = new Id<>(Artistry.id("canvas_cursor_sync"));
    public static final PacketCodec<PacketByteBuf, CanvasCursorS2CPacket> CODEC =
            PacketCodec.of(CanvasCursorS2CPacket::write, CanvasCursorS2CPacket::read);

    private static void write(CanvasCursorS2CPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeShort(p.gx);
        buf.writeShort(p.gy);
    }

    private static CanvasCursorS2CPacket read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new CanvasCursorS2CPacket(pos, uuid, buf.readShort(), buf.readShort());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}