package iliiasik.artistry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record CanvasCursorS2CPacket(BlockPos pos, UUID uuid, short gx, short gy) {
    public static void encode(CanvasCursorS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeShort(p.gx);
        buf.writeShort(p.gy);
    }
    public static CanvasCursorS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new CanvasCursorS2CPacket(pos, uuid, buf.readShort(), buf.readShort());
    }
}