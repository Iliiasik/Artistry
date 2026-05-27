package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record SyncImageLockS2CPacket(BlockPos pos, UUID imageUuid, UUID playerUuid) implements CustomPayload {

    public static final Id<SyncImageLockS2CPacket> ID = new Id<>(Artistry.id("sync_image_lock"));
    public static final PacketCodec<PacketByteBuf, SyncImageLockS2CPacket> CODEC =
            PacketCodec.of(SyncImageLockS2CPacket::write, SyncImageLockS2CPacket::read);

    private static void write(SyncImageLockS2CPacket p, PacketByteBuf buf) {
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

    private static SyncImageLockS2CPacket read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID imageUuid = new UUID(buf.readLong(), buf.readLong());
        UUID playerUuid = buf.readBoolean() ? new UUID(buf.readLong(), buf.readLong()) : null;
        return new SyncImageLockS2CPacket(pos, imageUuid, playerUuid);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}