package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record ImageUploadedS2CPacket(@Nullable BlockPos pos, UUID uuid,
                                     int gridX, int gridY, int gridW, int gridH) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("image_uploaded");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBoolean(pos != null);
        if (pos != null) buf.writeBlockPos(pos);
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        buf.writeInt(gridX);
        buf.writeInt(gridY);
        buf.writeInt(gridW);
        buf.writeInt(gridH);
    }

    public static ImageUploadedS2CPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new ImageUploadedS2CPacket(pos, uuid,
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }
}
