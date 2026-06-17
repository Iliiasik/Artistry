package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;

import java.util.UUID;

public record ToggleItemPixelizeC2SPacket(Hand hand, UUID uuid) implements CustomPayload {

    public static final Id<ToggleItemPixelizeC2SPacket> ID = new Id<>(Artistry.id("toggle_item_pixelize"));
    public static final PacketCodec<PacketByteBuf, ToggleItemPixelizeC2SPacket> CODEC =
            PacketCodec.of(ToggleItemPixelizeC2SPacket::write, ToggleItemPixelizeC2SPacket::read);

    private static void write(ToggleItemPixelizeC2SPacket p, PacketByteBuf buf) {
        buf.writeBoolean(p.hand == Hand.MAIN_HAND);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }

    private static ToggleItemPixelizeC2SPacket read(PacketByteBuf buf) {
        Hand hand = buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        return new ToggleItemPixelizeC2SPacket(hand, new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}