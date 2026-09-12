package iliiasik.artistry.item;

import iliiasik.artistry.block.BannerBlock;
import iliiasik.artistry.block.BannerPart;
import iliiasik.artistry.block.ModBlocks;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class BannerItem extends PosterItem {

    public BannerItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Direction facing = placementFacing(ctx);
        if (facing == null) return InteractionResult.PASS;

        Level world = ctx.getLevel();
        BlockPos clicked = ctx.getClickedPos().relative(facing);
        BlockPos origin = clicked.relative(facing.getClockWise().getOpposite());

        BlockState base = ModBlocks.BANNER.get().defaultBlockState().setValue(BannerBlock.FACING, facing);

        for (BannerPart part : BannerPart.values()) {
            BlockPos partPos = BannerBlock.partPos(origin, facing, part);
            if (!world.getBlockState(partPos).isAir()) return InteractionResult.FAIL;
            if (!base.setValue(BannerBlock.PART, part).canSurvive(world, partPos)) return InteractionResult.FAIL;
        }

        if (!world.isClientSide()) {
            for (BannerPart part : BannerPart.values()) {
                world.setBlockAndUpdate(BannerBlock.partPos(origin, facing, part),
                        base.setValue(BannerBlock.PART, part));
            }
            if (world.getBlockEntity(origin) instanceof PosterBlockEntity poster) {
                poster.loadFromItemStack(ctx.getItemInHand());
                poster.markDirtyAndSync();
            }
            consumePlaced(ctx);
        }
        return InteractionResult.SUCCESS;
    }
}
