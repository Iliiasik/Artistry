package iliiasik.artistry.network;

import net.minecraft.network.FriendlyByteBuf;

public record SetCanvasSizeC2SPacket(PosterTarget target, int size) {
    public static void encode(SetCanvasSizeC2SPacket p, FriendlyByteBuf buf) {
        PosterTarget.write(buf, p.target);
        buf.writeByte(p.size);
    }
    public static SetCanvasSizeC2SPacket decode(FriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.read(buf);
        return new SetCanvasSizeC2SPacket(target, buf.readByte() & 0xFF);
    }
}