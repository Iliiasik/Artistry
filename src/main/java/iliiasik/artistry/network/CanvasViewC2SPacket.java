package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record CanvasViewC2SPacket(BlockPos pos, boolean open) implements CustomPayload {

    public static final Id<CanvasViewC2SPacket> ID = new Id<>(Artistry.id("canvas_view"));
    public static final PacketCodec<PacketByteBuf, CanvasViewC2SPacket> CODEC =
            PacketCodec.of(CanvasViewC2SPacket::write, CanvasViewC2SPacket::read);

    private static void write(CanvasViewC2SPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeBoolean(p.open);
    }

    private static CanvasViewC2SPacket read(PacketByteBuf buf) {
        return new CanvasViewC2SPacket(buf.readBlockPos(), buf.readBoolean());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}