package iliiasik.artistry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record CanvasViewC2SPacket(BlockPos pos, boolean open) {
    public static void encode(CanvasViewC2SPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeBoolean(p.open);
    }
    public static CanvasViewC2SPacket decode(FriendlyByteBuf buf) {
        return new CanvasViewC2SPacket(buf.readBlockPos(), buf.readBoolean());
    }
}