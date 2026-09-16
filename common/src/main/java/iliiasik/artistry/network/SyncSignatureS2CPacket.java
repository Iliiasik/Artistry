package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasSignature;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record SyncSignatureS2CPacket(@Nullable BlockPos pos,
                                     @Nullable String playerName,
                                     @Nullable UUID playerUuid,
                                     long signedAt) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("sync_signature");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(pos != null);
        if (pos != null) buf.writeBlockPos(pos);
        buf.writeBoolean(playerName != null);
        if (playerName != null) buf.writeUtf(playerName, CanvasSignature.MAX_NAME_LENGTH);
        buf.writeBoolean(playerUuid != null);
        if (playerUuid != null) {
            buf.writeLong(playerUuid.getMostSignificantBits());
            buf.writeLong(playerUuid.getLeastSignificantBits());
        }
        buf.writeLong(signedAt);
    }

    public static SyncSignatureS2CPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        String name = buf.readBoolean() ? buf.readUtf(CanvasSignature.MAX_NAME_LENGTH) : null;
        UUID uuid = buf.readBoolean() ? new UUID(buf.readLong(), buf.readLong()) : null;
        return new SyncSignatureS2CPacket(pos, name, uuid, buf.readLong());
    }
}
