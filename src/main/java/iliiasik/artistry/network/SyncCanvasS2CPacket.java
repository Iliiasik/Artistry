package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.ArrayList;
import java.util.List;

public record SyncCanvasS2CPacket(BlockPos pos, List<CanvasData.PixelChange> changes)
        implements CustomPayload {

    public static final Id<SyncCanvasS2CPacket> ID = new Id<>(Artistry.id("sync_canvas"));

    public static final PacketCodec<PacketByteBuf, SyncCanvasS2CPacket> CODEC =
            PacketCodec.of(SyncCanvasS2CPacket::write, SyncCanvasS2CPacket::read);

    private static void write(SyncCanvasS2CPacket packet, PacketByteBuf buf) {
        buf.writeBlockPos(packet.pos);
        buf.writeShort(packet.changes.size());
        for (CanvasData.PixelChange c : packet.changes) {
            buf.writeByte(c.x());
            buf.writeByte(c.y());
            buf.writeShort(c.blockIndex());
        }
    }

    private static SyncCanvasS2CPacket read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int count = buf.readShort() & 0xFFFF;
        List<CanvasData.PixelChange> changes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            byte x = buf.readByte();
            byte y = buf.readByte();
            short idx = buf.readShort();
            changes.add(new CanvasData.PixelChange(x, y, idx));
        }
        return new SyncCanvasS2CPacket(pos, changes);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}