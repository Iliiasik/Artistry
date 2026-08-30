package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public record ImageUploadedS2CPacket(@Nullable BlockPos pos, UUID uuid, int gridX, int gridY, int gridW, int gridH) implements CustomPacketPayload {

    public static final Type<ImageUploadedS2CPacket> TYPE = new Type<>(Artistry.id("image_uploaded"));
    public static final StreamCodec<RegistryFriendlyByteBuf, ImageUploadedS2CPacket> CODEC =
            StreamCodec.ofMember(ImageUploadedS2CPacket::write, ImageUploadedS2CPacket::read);

    private static void write(ImageUploadedS2CPacket p, RegistryFriendlyByteBuf buf) {
        buf.writeBoolean(p.pos != null);
        if (p.pos != null) buf.writeBlockPos(p.pos);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeInt(p.gridX);
        buf.writeInt(p.gridY);
        buf.writeInt(p.gridW);
        buf.writeInt(p.gridH);
    }

    private static ImageUploadedS2CPacket read(RegistryFriendlyByteBuf buf) {
        BlockPos pos = buf.readBoolean() ? buf.readBlockPos() : null;
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new ImageUploadedS2CPacket(pos, uuid, buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}