package iliiasik.artistry.item;

import iliiasik.artistry.block.ModBlocks;
import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class PosterItem extends Item {

    public PosterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
        Direction face = ctx.getSide();

        if (face == Direction.UP || face == Direction.DOWN) return ActionResult.FAIL;
        BlockPos placePos = pos.offset(face);
        if (!world.getBlockState(placePos).isAir()) return ActionResult.FAIL;

        BlockState state = ModBlocks.POSTER
                .getDefaultState()
                .with(PosterBlock.FACING, face);

        if (!state.canPlaceAt(world, placePos)) return ActionResult.FAIL;

        if (!world.isClient()) {
            world.setBlockState(placePos, state);
            if (world.getBlockEntity(placePos) instanceof PosterBlockEntity poster) {
                poster.loadFromItemStack(ctx.getStack());
                poster.markDirtyAndSync();
            }
            PlayerEntity player = ctx.getPlayer();
            if (player != null && !player.isCreative()) {
                ctx.getStack().decrement(1);
            }
        }
        return ActionResult.SUCCESS;
    }
}