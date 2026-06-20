package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record CanvasEnterRequestC2SPacket(BlockPos pos) implements CustomPayload {

    public static final Id<CanvasEnterRequestC2SPacket> ID = new Id<>(Artistry.id("canvas_enter_request"));
    public static final PacketCodec<PacketByteBuf, CanvasEnterRequestC2SPacket> CODEC =
            PacketCodec.of(CanvasEnterRequestC2SPacket::write, CanvasEnterRequestC2SPacket::read);

    private static void write(CanvasEnterRequestC2SPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
    }

    private static CanvasEnterRequestC2SPacket read(PacketByteBuf buf) {
        return new CanvasEnterRequestC2SPacket(buf.readBlockPos());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}