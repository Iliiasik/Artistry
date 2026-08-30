package iliiasik.artistry.item;

import iliiasik.artistry.block.ModBlocks;
import iliiasik.artistry.block.PosterBlock;
import iliiasik.artistry.block.entity.PosterBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.InteractionResultHolder;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;

public class PosterItem extends Item {

    public PosterItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null) return InteractionResult.PASS;
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;

        Direction face = ctx.getClickedFace();
        if (face == Direction.UP || face == Direction.DOWN) return InteractionResult.PASS;

        Level world = ctx.getLevel();
        BlockPos pos = ctx.getClickedPos();
        BlockPos placePos = pos.relative(face);

        if (!world.getBlockState(placePos).isAir()) return InteractionResult.FAIL;

        BlockState state = ModBlocks.POSTER.get()
                .defaultBlockState()
                .setValue(PosterBlock.FACING, face);

        if (!state.canSurvive(world, placePos)) return InteractionResult.FAIL;

        if (!world.isClientSide()) {
            world.setBlockAndUpdate(placePos, state);
            if (world.getBlockEntity(placePos) instanceof PosterBlockEntity poster) {
                poster.loadFromItemStack(ctx.getItemInHand());
                poster.markDirtyAndSync();
            }
            if (!player.getAbilities().instabuild) {
                ctx.getItemInHand().shrink(1);
            }
        }
        return InteractionResult.SUCCESS;
    }
}