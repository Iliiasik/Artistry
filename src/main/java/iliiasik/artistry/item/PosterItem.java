package iliiasik.artistry.item;

import iliiasik.artistry.block.ModBlocks;
import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import net.minecraft.block.BlockState;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

public class PosterItem extends Item {

    public PosterItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        return ActionResult.SUCCESS;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext ctx) {
        PlayerEntity player = ctx.getPlayer();
        if (player == null) return ActionResult.PASS;
        if (!player.isSneaking()) return ActionResult.PASS;

        Direction face = ctx.getSide();
        if (face == Direction.UP || face == Direction.DOWN) return ActionResult.PASS;

        World world = ctx.getWorld();
        BlockPos pos = ctx.getBlockPos();
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
            if (!player.isCreative()) {
                ctx.getStack().decrement(1);
            }
        }
        return ActionResult.SUCCESS;
    }
}