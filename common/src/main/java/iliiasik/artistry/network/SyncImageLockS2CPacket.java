package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import javax.annotation.Nullable;
import java.util.UUID;

public record SyncImageLockS2CPacket(BlockPos pos, UUID imageUuid, @Nullable UUID playerUuid) implements CustomPacketPayload {
    public static final Type<SyncImageLockS2CPacket> TYPE = new Type<>(Artistry.id("sync_image_lock"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SyncImageLockS2CPacket> CODEC =
            StreamCodec.ofMember(SyncImageLockS2CPacket::write, SyncImageLockS2CPacket::read);

    private static void write(SyncImageLockS2CPacket p, RegistryFriendlyByteBuf buf) {
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

    private static SyncImageLockS2CPacket read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID imageUuid = new UUID(buf.readLong(), buf.readLong());
        UUID playerUuid = buf.readBoolean() ? new UUID(buf.readLong(), buf.readLong()) : null;
        return new SyncImageLockS2CPacket(pos, imageUuid, playerUuid);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}