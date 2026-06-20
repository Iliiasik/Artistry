package iliiasik.artistry.network;

import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;

public sealed interface PosterTarget permits PosterTarget.World, PosterTarget.Held {

    PacketCodec<PacketByteBuf, PosterTarget> CODEC = PacketCodec.of(PosterTarget::write, PosterTarget::read);

    record World(BlockPos pos) implements PosterTarget {}

    record Held(Hand hand) implements PosterTarget {}

    private static void write(PosterTarget target, PacketByteBuf buf) {
        if (target instanceof World world) {
            buf.writeByte(0);
            buf.writeBlockPos(world.pos());
        } else if (target instanceof Held held) {
            buf.writeByte(1);
            buf.writeBoolean(held.hand() == Hand.MAIN_HAND);
        }
    }

    private static PosterTarget read(PacketByteBuf buf) {
        byte type = buf.readByte();
        if (type == 0) {
            return new World(buf.readBlockPos());
        }
        return new Held(buf.readBoolean() ? Hand.MAIN_HAND : Hand.OFF_HAND);
    }
}