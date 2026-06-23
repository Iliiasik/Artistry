package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

public record UploadImageC2SPacket(PosterTarget target, byte[] bytes) {
    public static void encode(UploadImageC2SPacket p, FriendlyByteBuf buf) {
        PosterTarget.write(buf, p.target);
        buf.writeInt(p.bytes.length);
        buf.writeBytes(p.bytes);
    }
    public static UploadImageC2SPacket decode(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        int len = buf.readInt();
        byte[] bytes = new byte[len];
        buf.readBytes(bytes);
        return new UploadImageC2SPacket(target, bytes);
    }
}