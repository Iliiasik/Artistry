package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CanvasCursorC2SPacket(BlockPos pos, short gx, short gy) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("canvas_cursor_c2s");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeShort(gx);
        buf.writeShort(gy);
    }

    public static CanvasCursorC2SPacket read(FriendlyByteBuf buf) {
        return new CanvasCursorC2SPacket(buf.readBlockPos(), buf.readShort(), buf.readShort());
    }
}
