package iliiasik.artistry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record CanvasCursorC2SPacket(BlockPos pos, short gx, short gy) {
    public static void encode(CanvasCursorC2SPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeShort(p.gx);
        buf.writeShort(p.gy);
    }
    public static CanvasCursorC2SPacket decode(FriendlyByteBuf buf) {
        return new CanvasCursorC2SPacket(buf.readBlockPos(), buf.readShort(), buf.readShort());
    }
}