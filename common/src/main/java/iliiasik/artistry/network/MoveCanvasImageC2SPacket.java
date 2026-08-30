package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;

import java.util.UUID;

public record MoveCanvasImageC2SPacket(PosterTarget target, UUID uuid,
                                       int gridX, int gridY,
                                       int gridW, int gridH) implements CustomPacketPayload {

    public static final Type<MoveCanvasImageC2SPacket> TYPE = new Type<>(Artistry.id("move_canvas_image"));
    public static final StreamCodec<RegistryFriendlyByteBuf, MoveCanvasImageC2SPacket> CODEC =
            StreamCodec.ofMember(MoveCanvasImageC2SPacket::write, MoveCanvasImageC2SPacket::read);

    private static void write(MoveCanvasImageC2SPacket p, RegistryFriendlyByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeInt(p.gridX);
        buf.writeInt(p.gridY);
        buf.writeInt(p.gridW);
        buf.writeInt(p.gridH);
    }

    private static MoveCanvasImageC2SPacket read(RegistryFriendlyByteBuf buf) {
        PosterTarget target = PosterTarget.CODEC.decode(buf);
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new MoveCanvasImageC2SPacket(target, uuid,
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public Type<? extends CustomPacketPayload> type() { return TYPE; }
}