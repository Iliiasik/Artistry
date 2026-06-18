package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;

import java.util.UUID;

public record MoveItemImageToTopC2SPacket(Hand hand, UUID uuid) implements CustomPayload {

    public static final Id<MoveItemImageToTopC2SPacket> ID = new Id<>(Artistry.id("move_item_image_to_top"));
    public static final PacketCodec<PacketByteBuf, MoveItemImageToTopC2SPacket> CODEC =
            PacketCodec.of(MoveItemImageToTopC2SPacket::write, MoveItemImageToTopC2SPacket::read);

    private static void write(MoveItemImageToTopC2SPacket p, PacketByteBuf buf) {
        buf.writeBoolean(p.hand == Hand.MAIN_HAND);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }

    private static MoveItemImageToTopC2SPacket read(PacketByteBuf buf) {
        Hand hand = buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        return new MoveItemImageToTopC2SPacket(hand, new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}