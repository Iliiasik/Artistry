package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;

import java.util.List;

public record SaveItemCanvasC2SPacket(Hand hand, List<CanvasData.PixelChange> changes)
        implements CustomPayload {

    public static final Id<SaveItemCanvasC2SPacket> ID = new Id<>(Artistry.id("save_item_canvas"));
    public static final PacketCodec<PacketByteBuf, SaveItemCanvasC2SPacket> CODEC =
            PacketCodec.of(SaveItemCanvasC2SPacket::write, SaveItemCanvasC2SPacket::read);

    private static void write(SaveItemCanvasC2SPacket p, PacketByteBuf buf) {
        buf.writeBoolean(p.hand == Hand.MAIN_HAND);
        CanvasData.PixelChange.writeList(buf, p.changes);
    }

    private static SaveItemCanvasC2SPacket read(PacketByteBuf buf) {
        Hand hand = buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        return new SaveItemCanvasC2SPacket(hand, CanvasData.PixelChange.readList(buf));
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}