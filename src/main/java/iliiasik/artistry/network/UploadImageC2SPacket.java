package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record UploadImageC2SPacket(BlockPos pos, byte[] bytes) implements CustomPayload {

    public static final Id<UploadImageC2SPacket> ID = new Id<>(Artistry.id("upload_image"));
    public static final PacketCodec<PacketByteBuf, UploadImageC2SPacket> CODEC =
            PacketCodec.of(UploadImageC2SPacket::write, UploadImageC2SPacket::read);

    private static void write(UploadImageC2SPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeInt(p.bytes.length);
        buf.writeBytes(p.bytes);
    }

    private static UploadImageC2SPacket read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new UploadImageC2SPacket(pos, bytes);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}