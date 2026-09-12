package iliiasik.artistry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.InteractionHand;

public sealed interface PosterTarget permits PosterTarget.World, PosterTarget.Held {

    record World(BlockPos pos) implements PosterTarget {}

    record Held(InteractionHand hand) implements PosterTarget {}

    static void write(FriendlyByteBuf buf, PosterTarget target) {
        if (target instanceof World world) {
            buf.writeByte(0);
            buf.writeBlockPos(world.pos());
        } else if (target instanceof Held held) {
            buf.writeByte(1);
            buf.writeBoolean(held.hand() == InteractionHand.MAIN_HAND);
        }
    }

    static PosterTarget read(FriendlyByteBuf buf) {
        byte type = buf.readByte();
        if (type == 0) {
            return new World(buf.readBlockPos());
        }
        return new Held(buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }
}
