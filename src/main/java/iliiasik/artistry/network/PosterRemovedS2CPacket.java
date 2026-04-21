package iliiasik.artistry.network;

import iliiasik.artistry.Artistry;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.util.math.BlockPos;

public record PosterRemovedS2CPacket(BlockPos pos) implements CustomPayload {

    public static final Id<PosterRemovedS2CPacket> ID = new Id<>(Artistry.id("poster_removed"));

    public static final PacketCodec<PacketByteBuf, PosterRemovedS2CPacket> CODEC =
            PacketCodec.of(PosterRemovedS2CPacket::write, PosterRemovedS2CPacket::read);

    private static void write(PosterRemovedS2CPacket p, PacketByteBuf buf) {
        buf.writeBlockPos(p.pos);
    }

    private static PosterRemovedS2CPacket read(PacketByteBuf buf) {
        return new PosterRemovedS2CPacket(buf.readBlockPos());
    }

    @Override
    public Id<? extends CustomPayload> getId() { return ID; }
}