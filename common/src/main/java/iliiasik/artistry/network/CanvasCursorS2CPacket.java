package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record CanvasCursorS2CPacket(BlockPos pos, UUID uuid, short gx, short gy) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("canvas_cursor_s2c");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
        buf.writeShort(gx);
        buf.writeShort(gy);
    }

    public static CanvasCursorS2CPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new CanvasCursorS2CPacket(pos, uuid, buf.readShort(), buf.readShort());
    }
}
