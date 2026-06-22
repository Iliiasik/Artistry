package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record DeleteCanvasImageC2SPacket(PosterTarget target, UUID uuid) implements CustomPacketPayload {

    public static final Type<DeleteCanvasImageC2SPacket> TYPE = new Type<>(Artistry.id("delete_canvas_image"));
    public static final StreamCodec<RegistryFriendlyByteBuf, DeleteCanvasImageC2SPacket> CODEC =
            StreamCodec.ofMember(DeleteCanvasImageC2SPacket::write, DeleteCanvasImageC2SPacket::read);

    private static void write(DeleteCanvasImageC2SPacket p, RegistryFriendlyByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }

    private static DeleteCanvasImageC2SPacket read(RegistryFriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.CODEC.decode(buf);
        return new DeleteCanvasImageC2SPacket(target, new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}