package iliiasik.artistry.network;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.InteractionHand;

public sealed interface PosterTarget permits PosterTarget.World, PosterTarget.Held {

    StreamCodec<RegistryFriendlyByteBuf, PosterTarget> CODEC =
            StreamCodec.ofMember(PosterTarget::write, PosterTarget::read);

    record World(BlockPos pos) implements PosterTarget {}

    record Held(InteractionHand hand) implements PosterTarget {}

    private static void write(PosterTarget target, RegistryFriendlyByteBuf buf) {
        if (target instanceof World world) {
            buf.writeByte(0);
            buf.writeBlockPos(world.pos());
        } else if (target instanceof Held held) {
            buf.writeByte(1);
            buf.writeBoolean(held.hand() == InteractionHand.MAIN_HAND);
        }
    }

    private static PosterTarget read(RegistryFriendlyByteBuf buf) {
        byte type = buf.readByte();
        if (type == 0) {
            return new World(buf.readBlockPos());
        }
        return new Held(buf.readBoolean() ? InteractionHand.MAIN_HAND : InteractionHand.OFF_HAND);
    }
}