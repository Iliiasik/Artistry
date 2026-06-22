package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

public record SetCanvasSizeC2SPacket(PosterTarget target, int size) implements CustomPacketPayload {

    public static final Type<SetCanvasSizeC2SPacket> TYPE = new Type<>(Artistry.id("set_canvas_size"));
    public static final StreamCodec<RegistryFriendlyByteBuf, SetCanvasSizeC2SPacket> CODEC =
            StreamCodec.ofMember(SetCanvasSizeC2SPacket::write, SetCanvasSizeC2SPacket::read);

    private static void write(SetCanvasSizeC2SPacket p, RegistryFriendlyByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
        buf.writeByte(p.size);
    }

    private static SetCanvasSizeC2SPacket read(RegistryFriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.CODEC.decode(buf);
        return new SetCanvasSizeC2SPacket(target, buf.readByte() & 0xFF);
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}