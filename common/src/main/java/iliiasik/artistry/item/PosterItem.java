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
import org.jetbrains.annotations.Nullable;

public class PosterItem extends Item {

    public PosterItem(Properties settings) {
        super(settings);
    }

    @Override
    public InteractionResultHolder<ItemStack> use(Level world, Player player, InteractionHand hand) {
        return InteractionResultHolder.success(player.getItemInHand(hand));
    }

    @Nullable
    protected static Direction placementFacing(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player == null || !player.isShiftKeyDown()) return null;
        Direction face = ctx.getClickedFace();
        if (face == Direction.UP || face == Direction.DOWN) return null;
        return face;
    }

    protected static void consumePlaced(UseOnContext ctx) {
        Player player = ctx.getPlayer();
        if (player != null && !player.getAbilities().instabuild) {
            ctx.getItemInHand().shrink(1);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext ctx) {
        Direction face = placementFacing(ctx);
        if (face == null) return InteractionResult.PASS;

        Level world = ctx.getLevel();
        BlockPos placePos = ctx.getClickedPos().relative(face);

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
            consumePlaced(ctx);
        }
        return InteractionResult.SUCCESS;
    }
}
