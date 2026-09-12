package iliiasik.artistry.block;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.network.ArtistryNetwork;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.GameRules;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

@SuppressWarnings("deprecation")
public abstract class AbstractCanvasBlock extends BaseEntityBlock {

    public static final EnumProperty<Direction> FACING = BlockStateProperties.HORIZONTAL_FACING;

    protected AbstractCanvasBlock(Properties settings) {
        super(settings);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext ctx) {
        Direction side = ctx.getClickedFace();
        if (side == Direction.UP || side == Direction.DOWN) return null;
        return defaultBlockState().setValue(FACING, side);
    }

    @Override
    public VoxelShape getShape(BlockState state, BlockGetter world, BlockPos pos, CollisionContext ctx) {
        return PosterShapes.forFacing(state.getValue(FACING));
    }

    @Override
    public RenderShape getRenderShape(BlockState state) {
        return RenderShape.MODEL;
    }

    @Override
    public boolean canSurvive(BlockState state, LevelReader world, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos support = pos.relative(facing.getOpposite());
        return world.getBlockState(support).isFaceSturdy(world, support, facing);
    }

    @Override
    public BlockState updateShape(BlockState state, Direction direction,
                                  BlockState neighborState, LevelAccessor world,
                                  BlockPos pos, BlockPos neighborPos) {
        if (direction == state.getValue(FACING).getOpposite() && !state.canSurvive(world, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, direction, neighborState, world, pos, neighborPos);
    }

    protected static void dropCanvas(Level world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof PosterBlockEntity poster)) return;
        if (!(world instanceof ServerLevel serverWorld)) return;
        ArtistryNetwork.broadcastPosterRemoved(serverWorld, pos);
        if (world.getGameRules().getBoolean(GameRules.RULE_DOBLOCKDROPS)) {
            poster.dropWithCanvas(pos);
        }
    }
}
