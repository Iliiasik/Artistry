package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CanvasViewC2SPacket(BlockPos pos, boolean open) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("canvas_view");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(open);
    }

    public static CanvasViewC2SPacket read(FriendlyByteBuf buf) {
        return new CanvasViewC2SPacket(buf.readBlockPos(), buf.readBoolean());
    }
}
