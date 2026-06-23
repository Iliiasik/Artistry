package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

public record UploadImageC2SPacket(PosterTarget target, int total, int offset, byte[] chunk) {
    public static void encode(UploadImageC2SPacket p, FriendlyByteBuf buf) {
        PosterTarget.write(buf, p.target);
        buf.writeInt(p.total);
        buf.writeInt(p.offset);
        buf.writeInt(p.chunk.length);
        buf.writeBytes(p.chunk);
    }
    public static UploadImageC2SPacket decode(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        int total = buf.readInt();
        int offset = buf.readInt();
        int len = buf.readInt();
        byte[] chunk = new byte[len];
        buf.readBytes(chunk);
        return new UploadImageC2SPacket(target, total, offset, chunk);
    }
}