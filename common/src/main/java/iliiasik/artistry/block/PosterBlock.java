package iliiasik.artistry.block;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;

@SuppressWarnings("deprecation")
public class PosterBlock extends AbstractCanvasBlock {

    public PosterBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any().setValue(FACING, Direction.NORTH));
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
    public void onRemove(BlockState state, Level world, BlockPos pos,
                         BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            dropCanvas(world, pos);
        }
        super.onRemove(state, world, pos, newState, moved);
    }
}
