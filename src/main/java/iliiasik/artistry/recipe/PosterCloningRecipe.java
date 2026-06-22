package iliiasik.artistry.recipe;

import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;

public class PosterCloningRecipe extends CustomRecipe {
    public PosterCloningRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        int emptyCount = 0;
        int filledCount = 0;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.POSTER.get())) {
                    if (CanvasData.isSizeChosenForStack(stack)) {
                        filledCount++;
                    } else {
                        emptyCount++;
                    }
                } else {
                    return false;
                }
            }
        }

        return filledCount == 1 && emptyCount == 1;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        int emptyCount = 0;
        int filledCount = 0;
        ItemStack filledPoster = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.POSTER.get())) {
                    if (CanvasData.isSizeChosenForStack(stack)) {
                        filledPoster = stack;
                        filledCount++;
                    } else {
                        emptyCount++;
                    }
                } else {
                    return ItemStack.EMPTY;
                }
            }
        }

        if (filledCount == 1 && emptyCount == 1) {
            ItemStack result = filledPoster.copy();
            result.setCount(1);
            return result;
        }

        return ItemStack.EMPTY;
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> list = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (!stack.isEmpty()) {
                if (stack.is(ModItems.POSTER.get()) && CanvasData.isSizeChosenForStack(stack)) {
                    ItemStack remainder = stack.copy();
                    remainder.setCount(1);
                    list.set(i, remainder);
                } else {
                    ItemStack remainder = stack.getCraftingRemainingItem();
                    if (!remainder.isEmpty()) {
                        list.set(i, remainder);
                    }
                }
            }
        }

        return list;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.POSTER_CLONING.get();
    }
}