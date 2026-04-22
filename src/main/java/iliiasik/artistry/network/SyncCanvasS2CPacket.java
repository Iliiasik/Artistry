package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public record SyncCanvasS2CPacket(BlockPos pos, List<CanvasData.PixelChange> changes)
        implements CustomPayload {

    public static final Id<SyncCanvasS2CPacket> ID = new Id<>(Artistry.id("sync_canvas"));
    public static final PacketCodec<PacketByteBuf, SyncCanvasS2CPacket> CODEC =
            PacketCodec.of(SyncCanvasS2CPacket::write, SyncCanvasS2CPacket::read);

    private static void write(SyncCanvasS2CPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
        CanvasData.PixelChange.writeList(buf, p.changes);
    }

    private static SyncCanvasS2CPacket read(PacketByteBuf buf) {
        return new SyncCanvasS2CPacket(buf.readBlockPos(), CanvasData.PixelChange.readList(buf));
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}