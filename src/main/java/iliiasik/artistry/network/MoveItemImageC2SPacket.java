package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;

import java.util.UUID;

public record MoveItemImageC2SPacket(Hand hand, UUID uuid,
                                     int gridX, int gridY,
                                     int gridW, int gridH) implements CustomPayload {

    public static final Id<MoveItemImageC2SPacket> ID = new Id<>(Artistry.id("move_item_image"));
    public static final PacketCodec<PacketByteBuf, MoveItemImageC2SPacket> CODEC =
            PacketCodec.of(MoveItemImageC2SPacket::write, MoveItemImageC2SPacket::read);

    private static void write(MoveItemImageC2SPacket p, PacketByteBuf buf) {
        buf.writeBoolean(p.hand == Hand.MAIN_HAND);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeInt(p.gridX);
        buf.writeInt(p.gridY);
        buf.writeInt(p.gridW);
        buf.writeInt(p.gridH);
    }

    private static MoveItemImageC2SPacket read(PacketByteBuf buf) {
        Hand hand = buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new MoveItemImageC2SPacket(hand, uuid,
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}