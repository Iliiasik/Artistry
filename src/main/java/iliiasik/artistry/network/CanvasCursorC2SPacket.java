package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record CanvasCursorC2SPacket(BlockPos pos, short gx, short gy) implements CustomPayload {

    public static final Id<CanvasCursorC2SPacket> ID = new Id<>(Artistry.id("canvas_cursor"));
    public static final PacketCodec<PacketByteBuf, CanvasCursorC2SPacket> CODEC =
            PacketCodec.of(CanvasCursorC2SPacket::write, CanvasCursorC2SPacket::read);

    private static void write(CanvasCursorC2SPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeShort(p.gx);
        buf.writeShort(p.gy);
    }

    private static CanvasCursorC2SPacket read(PacketByteBuf buf) {
        return new CanvasCursorC2SPacket(buf.readBlockPos(), buf.readShort(), buf.readShort());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}