package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.List;

public record SaveCanvasC2SPacket(BlockPos pos, List<CanvasData.PixelChange> changes)
        implements CustomPayload {

    public static final Id<SaveCanvasC2SPacket> ID = new Id<>(Artistry.id("save_canvas"));
    public static final PacketCodec<PacketByteBuf, SaveCanvasC2SPacket> CODEC =
            PacketCodec.of(SaveCanvasC2SPacket::write, SaveCanvasC2SPacket::read);

    private static void write(SaveCanvasC2SPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
        CanvasData.PixelChange.writeList(buf, p.changes);
    }

    private static SaveCanvasC2SPacket read(PacketByteBuf buf) {
        return new SaveCanvasC2SPacket(buf.readBlockPos(), CanvasData.PixelChange.readList(buf));
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}