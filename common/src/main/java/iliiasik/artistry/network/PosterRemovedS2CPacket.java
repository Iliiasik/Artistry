package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record PosterRemovedS2CPacket(BlockPos pos) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("poster_removed");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static PosterRemovedS2CPacket read(FriendlyByteBuf buf) {
        return new PosterRemovedS2CPacket(buf.readBlockPos());
    }
}
