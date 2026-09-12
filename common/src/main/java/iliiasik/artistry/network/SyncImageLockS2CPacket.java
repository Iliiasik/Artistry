package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record SyncImageLockS2CPacket(BlockPos pos, UUID imageUuid,
                                     @Nullable UUID playerUuid) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("sync_image_lock");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeLong(imageUuid.getMostSignificantBits());
        buf.writeLong(imageUuid.getLeastSignificantBits());
        boolean hasPlayer = playerUuid != null;
        buf.writeBoolean(hasPlayer);
        if (hasPlayer) {
            buf.writeLong(playerUuid.getMostSignificantBits());
            buf.writeLong(playerUuid.getLeastSignificantBits());
        }
    }

    public static SyncImageLockS2CPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID imageUuid = new UUID(buf.readLong(), buf.readLong());
        UUID playerUuid = buf.readBoolean() ? new UUID(buf.readLong(), buf.readLong()) : null;
        return new SyncImageLockS2CPacket(pos, imageUuid, playerUuid);
    }
}
