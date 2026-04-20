package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;

import java.util.ArrayList;
import java.util.List;

public record SaveItemCanvasC2SPacket(Hand hand, List<CanvasData.PixelChange> changes)
        implements CustomPayload {

    public static final Id<SaveItemCanvasC2SPacket> ID = new Id<>(Artistry.id("save_item_canvas"));

    public static final PacketCodec<PacketByteBuf, SaveItemCanvasC2SPacket> CODEC =
            PacketCodec.of(SaveItemCanvasC2SPacket::write, SaveItemCanvasC2SPacket::read);

    private static void write(SaveItemCanvasC2SPacket packet, PacketByteBuf buf) {
        buf.writeBoolean(packet.hand == Hand.MAIN_HAND);
        buf.writeShort(packet.changes.size());
        for (CanvasData.PixelChange c : packet.changes) {
            buf.writeByte(c.x());
            buf.writeByte(c.y());
            buf.writeShort(c.blockIndex());
        }
    }

    private static SaveItemCanvasC2SPacket read(PacketByteBuf buf) {
        Hand hand = buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        int count = buf.readShort() & 0xFFFF;
        List<CanvasData.PixelChange> changes = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            byte x = buf.readByte();
            byte y = buf.readByte();
            short idx = buf.readShort();
            changes.add(new CanvasData.PixelChange(x, y, idx));
        }
        return new SaveItemCanvasC2SPacket(hand, changes);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}