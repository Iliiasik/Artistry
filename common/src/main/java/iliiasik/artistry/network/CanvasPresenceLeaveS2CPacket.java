package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.UUID;

public record CanvasPresenceLeaveS2CPacket(BlockPos pos, UUID uuid) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("canvas_presence_leave");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeLong(uuid.getMostSignificantBits());
        buf.writeLong(uuid.getLeastSignificantBits());
    }

    public static CanvasPresenceLeaveS2CPacket read(FriendlyByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new CanvasPresenceLeaveS2CPacket(pos, new UUID(buf.readLong(), buf.readLong()));
    }
}
