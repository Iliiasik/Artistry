package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record CanvasCursorC2SPacket(BlockPos pos, short gx, short gy) implements CustomPacketPayload {

    public static final Type<CanvasCursorC2SPacket> TYPE = new Type<>(Artistry.id("canvas_cursor"));
    public static final StreamCodec<RegistryFriendlyByteBuf, CanvasCursorC2SPacket> CODEC =
            StreamCodec.ofMember(CanvasCursorC2SPacket::write, CanvasCursorC2SPacket::read);

    private static void write(CanvasCursorC2SPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeShort(p.gx);
        buf.writeShort(p.gy);
    }

    private static CanvasCursorC2SPacket read(RegistryFriendlyByteBuf buf) {
        return new CanvasCursorC2SPacket(buf.readBlockPos(), buf.readShort(), buf.readShort());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}