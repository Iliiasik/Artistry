package iliiasik.artistry.block;

import com.mojang.serialization.MapCodec;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

public class PosterBlock extends AbstractCanvasBlock {

    public static final MapCodec<PosterBlock> CODEC = MapCodec.unit(
            () -> new PosterBlock(BlockBehaviour.Properties.of())
    );

    public PosterBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PosterBlockEntity(pos, state);
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos,
                            BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            dropCanvas(world, pos);
        }
        super.onRemove(state, world, pos, newState, moved);
    }
}
