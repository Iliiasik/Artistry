package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record UploadImageC2SPacket(PosterTarget target, byte[] bytes) implements CustomPacketPayload {

    public static final Type<UploadImageC2SPacket> TYPE = new Type<>(Artistry.id("upload_image"));
    public static final StreamCodec<RegistryFriendlyByteBuf, UploadImageC2SPacket> CODEC =
            StreamCodec.ofMember(UploadImageC2SPacket::write, UploadImageC2SPacket::read);

    private static void write(UploadImageC2SPacket p, RegistryFriendlyByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
        buf.writeInt(p.bytes.length);
        buf.writeBytes(p.bytes);
    }

    private static UploadImageC2SPacket read(RegistryFriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.CODEC.decode(buf);
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new UploadImageC2SPacket(target, bytes);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}