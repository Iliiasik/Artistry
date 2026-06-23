package iliiasik.artistry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.UUID;

public record CanvasPresenceLeaveS2CPacket(BlockPos pos, UUID uuid) {
    public static void encode(CanvasPresenceLeaveS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }
    public static CanvasPresenceLeaveS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new CanvasPresenceLeaveS2CPacket(pos, new UUID(buf.readLong(), buf.readLong()));
    }
}