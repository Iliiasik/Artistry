package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;

import java.util.UUID;

public record MoveCanvasImageC2SPacket(PosterTarget target, UUID uuid,
                                       int gridX, int gridY,
                                       int gridW, int gridH) implements CustomPayload {

    public static final Id<MoveCanvasImageC2SPacket> ID = new Id<>(Artistry.id("move_canvas_image"));
    public static final PacketCodec<PacketByteBuf, MoveCanvasImageC2SPacket> CODEC =
            PacketCodec.of(MoveCanvasImageC2SPacket::write, MoveCanvasImageC2SPacket::read);

    private static void write(MoveCanvasImageC2SPacket p, PacketByteBuf buf) {
        PosterTarget.CODEC.encode(buf, p.target);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
        buf.writeInt(p.gridX);
        buf.writeInt(p.gridY);
        buf.writeInt(p.gridW);
        buf.writeInt(p.gridH);
    }

    private static MoveCanvasImageC2SPacket read(PacketByteBuf buf) {
        PosterTarget target = PosterTarget.CODEC.decode(buf);
        UUID uuid = new UUID(buf.readLong(), buf.readLong());
        return new MoveCanvasImageC2SPacket(target, uuid,
                buf.readInt(), buf.readInt(), buf.readInt(), buf.readInt());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}