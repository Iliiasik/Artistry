package iliiasik.artistry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record CanvasEnterRequestC2SPacket(BlockPos pos) {
    public static void encode(CanvasEnterRequestC2SPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
    }
    public static CanvasEnterRequestC2SPacket decode(FriendlyByteBuf buf) {
        return new CanvasEnterRequestC2SPacket(buf.readBlockPos());
    }
}