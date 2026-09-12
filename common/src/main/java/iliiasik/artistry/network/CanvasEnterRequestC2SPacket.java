package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CanvasEnterRequestC2SPacket(BlockPos pos) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("canvas_enter_request");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    public static CanvasEnterRequestC2SPacket read(FriendlyByteBuf buf) {
        return new CanvasEnterRequestC2SPacket(buf.readBlockPos());
    }
}
