package iliiasik.artistry.network;

import iliiasik.artistry.data.CanvasData;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

import java.util.List;

public record SyncCanvasS2CPacket(BlockPos pos, List<CanvasData.PixelChange> changes) {
    public static void encode(SyncCanvasS2CPacket p, FriendlyByteBuf buf) {
        buf.writeBlockPos(p.pos);
        CanvasData.PixelChange.writeList(buf, p.changes);
    }
    public static SyncCanvasS2CPacket decode(FriendlyByteBuf buf) {
        return new SyncCanvasS2CPacket(buf.readBlockPos(), CanvasData.PixelChange.readList(buf));
    }
}