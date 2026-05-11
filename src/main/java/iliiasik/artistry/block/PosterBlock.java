package iliiasik.artistry.block;

import com.mojang.serialization.MapCodec;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.network.ModNetwork;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldAccess;
import net.minecraft.world.WorldView;
import org.jetbrains.annotations.Nullable;

public class PosterBlock extends BlockWithEntity {

    public static final MapCodec<PosterBlock> CODEC = MapCodec.unit(
            () -> new PosterBlock(AbstractBlock.Settings.create())
    );

    public static final EnumProperty<Direction> FACING = Properties.HORIZONTAL_FACING;

    private static final VoxelShape SHAPE_NORTH = Block.createCuboidShape(0, 0, 15.9, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH = Block.createCuboidShape(0, 0, 0, 16, 16, 0.1);
    private static final VoxelShape SHAPE_WEST  = Block.createCuboidShape(15.9, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_EAST  = Block.createCuboidShape(0, 0, 0, 0.1, 16, 16);

    public PosterBlock(Settings settings) {
        super(settings);
        setDefaultState(getStateManager().getDefaultState().with(FACING, Direction.NORTH));
    }

    @Override
    protected MapCodec<? extends BlockWithEntity> getCodec() {
        return CODEC;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACING);
    }

    @Override
    public @Nullable BlockState getPlacementState(ItemPlacementContext ctx) {
        Direction side = ctx.getSide();
        if (side == Direction.UP || side == Direction.DOWN) return null;
        return getDefaultState().with(FACING, side);
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return switch (state.get(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
            default    -> SHAPE_NORTH;
        };
    }

    @Override
    public BlockRenderType getRenderType(BlockState state) {
        return BlockRenderType.MODEL;
    }

    @Override
    public BlockEntity createBlockEntity(BlockPos pos, BlockState state) {
        return new PosterBlockEntity(pos, state);
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        BlockPos support = pos.offset(state.get(FACING).getOpposite());
        return world.getBlockState(support).isSolidBlock(world, support);
    }

    @Override
    public BlockState getStateForNeighborUpdate(BlockState state, Direction direction,
                                                BlockState neighborState, WorldAccess world,
                                                BlockPos pos, BlockPos neighborPos) {
        if (direction == state.get(FACING).getOpposite() && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, direction, neighborState, world, pos, neighborPos);
    }

    @Override
    public void onStateReplaced(BlockState state, World world, BlockPos pos,
                                BlockState newState, boolean moved) {
        if (!state.isOf(newState.getBlock())) {
            if (world.getBlockEntity(pos) instanceof PosterBlockEntity poster) {
                if (world instanceof ServerWorld serverWorld) {
                    ModNetwork.broadcastPosterRemoved(serverWorld, pos);
                }
                poster.dropWithCanvas(pos);
            }
        }
        super.onStateReplaced(state, world, pos, newState, moved);
    }
}