package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record DeleteCanvasImageC2SPacket(BlockPos pos, UUID uuid) implements CustomPayload {

    public static final Id<DeleteCanvasImageC2SPacket> ID = new Id<>(Artistry.id("delete_canvas_image"));
    public static final PacketCodec<PacketByteBuf, DeleteCanvasImageC2SPacket> CODEC =
            PacketCodec.of(DeleteCanvasImageC2SPacket::write, DeleteCanvasImageC2SPacket::read);

    private static void write(DeleteCanvasImageC2SPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }

    private static DeleteCanvasImageC2SPacket read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new DeleteCanvasImageC2SPacket(pos, new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}