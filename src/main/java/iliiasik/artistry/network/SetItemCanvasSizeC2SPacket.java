package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;

public record SetItemCanvasSizeC2SPacket(Hand hand, int size) implements CustomPayload {

    public static final Id<SetItemCanvasSizeC2SPacket> ID = new Id<>(Artistry.id("set_item_canvas_size"));
    public static final PacketCodec<PacketByteBuf, SetItemCanvasSizeC2SPacket> CODEC =
            PacketCodec.of(SetItemCanvasSizeC2SPacket::write, SetItemCanvasSizeC2SPacket::read);

    private static void write(SetItemCanvasSizeC2SPacket p, PacketByteBuf buf) {
        buf.writeBoolean(p.hand == Hand.MAIN_HAND);
        buf.writeByte(p.size);
    }

    private static SetItemCanvasSizeC2SPacket read(PacketByteBuf buf) {
        Hand hand = buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        return new SetItemCanvasSizeC2SPacket(hand, buf.readByte() & 0xFF);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}