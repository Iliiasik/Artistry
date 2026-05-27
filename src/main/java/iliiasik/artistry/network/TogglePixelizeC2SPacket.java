package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

import java.util.UUID;

public record TogglePixelizeC2SPacket(BlockPos pos, UUID uuid) implements CustomPayload {

    public static final Id<TogglePixelizeC2SPacket> ID = new Id<>(Artistry.id("toggle_pixelize"));
    public static final PacketCodec<PacketByteBuf, TogglePixelizeC2SPacket> CODEC =
            PacketCodec.of(TogglePixelizeC2SPacket::write, TogglePixelizeC2SPacket::read);

    private static void write(TogglePixelizeC2SPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
        buf.writeLong(p.uuid.getMostSignificantBits());
        buf.writeLong(p.uuid.getLeastSignificantBits());
    }

    private static TogglePixelizeC2SPacket read(PacketByteBuf buf) {
        BlockPos pos = buf.readBlockPos();
        return new TogglePixelizeC2SPacket(pos, new UUID(buf.readLong(), buf.readLong()));
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}