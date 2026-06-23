package iliiasik.artistry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record CanvasEnterAllowedS2CPacket(BlockPos pos, boolean needsSize) {
    public static void encode(CanvasEnterAllowedS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeBoolean(p.needsSize);
    }
    public static CanvasEnterAllowedS2CPacket decode(FriendlyByteBuf buf) {
        return new CanvasEnterAllowedS2CPacket(buf.readBlockPos(), buf.readBoolean());
    }
}