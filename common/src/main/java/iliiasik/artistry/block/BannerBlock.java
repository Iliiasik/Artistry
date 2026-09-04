package iliiasik.artistry.block;

import com.mojang.serialization.MapCodec;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import org.jetbrains.annotations.Nullable;

public class BannerBlock extends AbstractCanvasBlock {

    public static final MapCodec<BannerBlock> CODEC = MapCodec.unit(
            () -> new BannerBlock(BlockBehaviour.Properties.of())
    );

    public static final EnumProperty<BannerPart> PART = EnumProperty.create("part", BannerPart.class);

    public static final float WORLD_SIZE = 2f;

    public BannerBlock(Properties settings) {
        super(settings);
        registerDefaultState(getStateDefinition().any()
                .setValue(FACING, Direction.NORTH)
                .setValue(PART, BannerPart.ORIGIN));
    }

    public static BlockPos partPos(BlockPos origin, Direction facing, BannerPart part) {
        return origin.relative(facing.getClockWise(), part.sideSteps()).above(part.upSteps());
    }

    public static BlockPos originOf(BlockPos partPos, Direction facing, BannerPart part) {
        return partPos.relative(facing.getClockWise().getOpposite(), part.sideSteps()).below(part.upSteps());
    }

    public static BlockPos originOf(BlockState state, BlockPos pos) {
        return originOf(pos, state.getValue(FACING), state.getValue(PART));
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(FACING, PART);
    }

    @Override
    protected RenderShape getRenderShape(BlockState state) {
        return state.getValue(PART) == BannerPart.ORIGIN ? RenderShape.MODEL : RenderShape.INVISIBLE;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return state.getValue(PART) == BannerPart.ORIGIN ? new PosterBlockEntity(pos, state) : null;
    }

    @Override
    protected void onRemove(BlockState state, Level world, BlockPos pos,
                            BlockState newState, boolean moved) {
        if (!state.is(newState.getBlock())) {
            if (state.getValue(PART) == BannerPart.ORIGIN) {
                dropCanvas(world, pos);
            }
            removeOtherParts(state, world, pos);
        }
        super.onRemove(state, world, pos, newState, moved);
    }

    private void removeOtherParts(BlockState state, Level world, BlockPos pos) {
        Direction facing = state.getValue(FACING);
        BlockPos origin = originOf(state, pos);

        for (BannerPart other : BannerPart.values()) {
            BlockPos otherPos = partPos(origin, facing, other);
            if (otherPos.equals(pos)) continue;
            BlockState otherState = world.getBlockState(otherPos);
            if (!otherState.is(this) || otherState.getValue(FACING) != facing) continue;
            world.setBlock(otherPos, Blocks.AIR.defaultBlockState(),
                    Block.UPDATE_CLIENTS | Block.UPDATE_SUPPRESS_DROPS);
        }
    }
}
