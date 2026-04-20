package iliiasik.artistry.block;

import com.mojang.serialization.MapCodec;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import net.minecraft.block.*;
import net.minecraft.block.entity.BlockEntity;
import net.minecraft.item.ItemPlacementContext;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.EnumProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.WorldView;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

public class PosterBlock extends BlockWithEntity {

    public static final MapCodec<PosterBlock> CODEC = MapCodec.unit(
            () -> new PosterBlock(AbstractBlock.Settings.create())
    );

    public static final EnumProperty<Direction> FACING = Properties.FACING;

    private static final VoxelShape SHAPE_NORTH  = Block.createCuboidShape(0, 0, 15.9, 16, 16, 16);
    private static final VoxelShape SHAPE_SOUTH  = Block.createCuboidShape(0, 0, 0, 16, 16, 0.1);
    private static final VoxelShape SHAPE_WEST   = Block.createCuboidShape(15.9, 0, 0, 16, 16, 16);
    private static final VoxelShape SHAPE_EAST   = Block.createCuboidShape(0, 0, 0, 0.1, 16, 16);
    private static final VoxelShape SHAPE_UP     = Block.createCuboidShape(0, 0, 0, 16, 0.1, 16);
    private static final VoxelShape SHAPE_DOWN   = Block.createCuboidShape(0, 15.9, 0, 16, 16, 16);

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
        return getDefaultState().with(FACING, ctx.getSide());
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext ctx) {
        return switch (state.get(FACING)) {
            case SOUTH -> SHAPE_SOUTH;
            case WEST  -> SHAPE_WEST;
            case EAST  -> SHAPE_EAST;
            case UP    -> SHAPE_UP;
            case DOWN  -> SHAPE_DOWN;
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
    public BlockState getStateForNeighborUpdate(
            BlockState state,
            WorldView world,
            net.minecraft.world.tick.ScheduledTickView tickView,
            BlockPos pos,
            Direction direction,
            BlockPos neighborPos,
            BlockState neighborState,
            net.minecraft.util.math.random.Random random) {
        if (direction == state.get(FACING).getOpposite() && !state.canPlaceAt(world, pos)) {
            return Blocks.AIR.getDefaultState();
        }
        return super.getStateForNeighborUpdate(state, world, tickView, pos, direction, neighborPos, neighborState, random);
    }

    @Override
    public BlockState onBreak(World world, BlockPos pos, BlockState state, PlayerEntity player) {
        if (!world.isClient()) {
            BlockEntity be = world.getBlockEntity(pos);
            if (be instanceof PosterBlockEntity poster) {
                ItemStack stack = new ItemStack(iliiasik.artistry.item.ModItems.POSTER);
                NbtCompound tag = new NbtCompound();
                tag.put("canvas", poster.canvasData.toNbt());
                stack.set(net.minecraft.component.DataComponentTypes.CUSTOM_DATA,
                        net.minecraft.component.type.NbtComponent.of(tag));
                dropStack(world, pos, stack);
            }
        }
        super.onBreak(world, pos, state, player);
        return state;
    }
}