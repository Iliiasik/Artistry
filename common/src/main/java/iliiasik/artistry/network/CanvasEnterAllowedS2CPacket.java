package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

public record CanvasEnterAllowedS2CPacket(BlockPos pos, boolean needsSize) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("canvas_enter_allowed");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeBoolean(needsSize);
    }

    public static CanvasEnterAllowedS2CPacket read(FriendlyByteBuf buf) {
        return new CanvasEnterAllowedS2CPacket(buf.readBlockPos(), buf.readBoolean());
    }
}
