package iliiasik.artistry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.UUID;

public record SyncImageLockS2CPacket(BlockPos pos, UUID imageUuid, @Nullable UUID playerUuid) {
    public static void encode(SyncImageLockS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeLong(p.imageUuid.getMostSignificantBits());
        buf.writeLong(p.imageUuid.getLeastSignificantBits());
        boolean hasPlayer = p.playerUuid != null;
        buf.writeBoolean(hasPlayer);
        if (hasPlayer) {
            buf.writeLong(p.playerUuid.getMostSignificantBits());
            buf.writeLong(p.playerUuid.getLeastSignificantBits());
        }
    }
    public static SyncImageLockS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID imageUuid = new UUID(buf.readLong(), buf.readLong());
        UUID playerUuid = buf.readBoolean() ? new UUID(buf.readLong(), buf.readLong()) : null;
        return new SyncImageLockS2CPacket(pos, imageUuid, playerUuid);
    }
}