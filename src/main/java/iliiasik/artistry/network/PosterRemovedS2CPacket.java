package iliiasik.artistry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public record PosterRemovedS2CPacket(BlockPos pos) {
    public static void encode(PosterRemovedS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
    }
    public static PosterRemovedS2CPacket decode(FriendlyByteBuf buf) {
        return new PosterRemovedS2CPacket(buf.readBlockPos());
    }
}