package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;

import java.util.List;

public record SyncCanvasS2CPacket(BlockPos pos,
                                  List<CanvasData.PixelChange> changes) implements ArtistryPacket {

    public static final ResourceLocation ID = Artistry.id("sync_canvas");

    @Override
    public ResourceLocation id() {
        return ID;
    }

    @Override
    public void write(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        CanvasData.PixelChange.writeList(buf, changes);
    }

    public static SyncCanvasS2CPacket read(FriendlyByteBuf buf) {
        return new SyncCanvasS2CPacket(buf.readBlockPos(), CanvasData.PixelChange.readList(buf));
    }
}
