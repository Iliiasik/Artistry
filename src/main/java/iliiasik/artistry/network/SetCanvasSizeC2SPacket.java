package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

public record SetCanvasSizeC2SPacket(PosterTarget target, int size) implements CustomPayload {

    public static final Id<SetCanvasSizeC2SPacket> ID = new Id<>(Artistry.id("set_canvas_size"));
    public static final PacketCodec<PacketByteBuf, SetCanvasSizeC2SPacket> CODEC =
            PacketCodec.of(SetCanvasSizeC2SPacket::write, SetCanvasSizeC2SPacket::read);

    private static void write(SetCanvasSizeC2SPacket p, PacketByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
        buf.writeByte(p.size);
    }

    private static SetCanvasSizeC2SPacket read(PacketByteBuf buf) {
        PosterTarget target = PosterTarget.CODEC.decode(buf);
        return new SetCanvasSizeC2SPacket(target, buf.readByte() & 0xFF);
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}