package iliiasik.artistry.block;

import net.minecraft.core.BlockPos;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

public final class PosterBlocks {

    private PosterBlocks() {}

    @Nullable
    public static BlockPos originOf(BlockGetter world, BlockPos pos) {
        BlockState state = world.getBlockState(pos);
        if (state.getBlock() instanceof BannerBlock) return BannerBlock.originOf(state, pos);
        if (state.getBlock() instanceof PosterBlock) return pos;
        return null;
    }
}
