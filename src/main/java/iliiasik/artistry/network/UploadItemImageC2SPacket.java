package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.Hand;

public record UploadItemImageC2SPacket(Hand hand, byte[] bytes) implements CustomPayload {

    public static final Id<UploadItemImageC2SPacket> ID = new Id<>(Artistry.id("upload_item_image"));
    public static final PacketCodec<PacketByteBuf, UploadItemImageC2SPacket> CODEC =
            PacketCodec.of(UploadItemImageC2SPacket::write, UploadItemImageC2SPacket::read);

    private static void write(UploadItemImageC2SPacket p, PacketByteBuf buf) {
        buf.writeBoolean(p.hand == Hand.MAIN_HAND);
        buf.writeInt(p.bytes.length);
        buf.writeBytes(p.bytes);
    }

    private static UploadItemImageC2SPacket read(PacketByteBuf buf) {
        Hand hand = buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND;
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new UploadItemImageC2SPacket(hand, bytes);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}