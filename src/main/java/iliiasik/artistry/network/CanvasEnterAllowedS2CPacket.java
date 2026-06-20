package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record CanvasEnterAllowedS2CPacket(BlockPos pos) implements CustomPayload {

    public static final Id<CanvasEnterAllowedS2CPacket> ID = new Id<>(Artistry.id("canvas_enter_allowed"));
    public static final PacketCodec<PacketByteBuf, CanvasEnterAllowedS2CPacket> CODEC =
            PacketCodec.of(CanvasEnterAllowedS2CPacket::write, CanvasEnterAllowedS2CPacket::read);

    private static void write(CanvasEnterAllowedS2CPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
    }

    private static CanvasEnterAllowedS2CPacket read(PacketByteBuf buf) {
        return new CanvasEnterAllowedS2CPacket(buf.readBlockPos());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}