package iliiasik.artistry.recipe;

import iliiasik.artistry.data.CanvasSignature;
import iliiasik.artistry.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class BannerCraftingRecipe extends CustomRecipe {

    private static final int GRID = 3;

    public BannerCraftingRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        return source(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        ItemStack poster = source(input);
        if (poster == null) return ItemStack.EMPTY;

        ItemStack result = new ItemStack(ModItems.BANNER.get());
        CustomData data = poster.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            result.set(DataComponents.CUSTOM_DATA, data);
        }
        return result;
    }

    @Nullable
    private static ItemStack source(CraftingInput input) {
        if (input.width() != GRID || input.height() != GRID) return null;

        for (int row = 0; row < GRID; row++) {
            for (int column = 0; column < GRID; column++) {
                ItemStack stack = input.getItem(row * GRID + column);
                boolean corner = (row == 0 || row == GRID - 1) && (column == 0 || column == GRID - 1);
                boolean center = row == 1 && column == 1;

                if (center) continue;
                if (corner) {
                    if (!stack.is(Items.STICK)) return null;
                } else if (!stack.is(Items.PAPER)) {
                    return null;
                }
            }
        }

        ItemStack poster = input.getItem(GRID + 1);
        if (!poster.is(ModItems.POSTER.get())) return null;
        if (CanvasSignature.isSignedStack(poster)) return null;
        return poster;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width >= GRID && height >= GRID;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BANNER_CRAFTING.get();
    }
}
