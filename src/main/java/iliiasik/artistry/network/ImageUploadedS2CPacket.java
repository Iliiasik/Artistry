package iliiasik.artistry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import javax.annotation.Nullable;
import java.util.UUID;

public record ImageUploadedS2CPacket(@Nullable BlockPos pos, UUID uuid, int gridX, int gridY, int gridW, int gridH) {
    public static void encode(ImageUploadedS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBoolean(p.pos != null);
        if (p.pos != null) buf.writeBlockPos(p.pos);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeInt(p.gridX);
        buf.writeInt(p.gridY);
        buf.writeInt(p.gridW);
        buf.writeInt(p.gridH);
    }
    public static ImageUploadedS2CPacket decode(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new ImageUploadedS2CPacket(pos, uuid, buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }
}