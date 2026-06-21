package iliiasik.artistry.recipe;

import iliiasik.artistry.item.ModItems;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialCraftingRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.util.collection.DefaultedList;
import net.minecraft.world.World;
import net.minecraft.recipe.input.CraftingRecipeInput;

public class PosterCloningRecipe extends SpecialCraftingRecipe {
    public PosterCloningRecipe(CraftingRecipeCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingRecipeInput input, World world) {
        int emptyCount = 0;
        int filledCount = 0;

        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (stack.isOf(ModItems.POSTER)) {
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
    public ItemStack craft(CraftingRecipeInput input, RegistryWrapper.WrapperLookup lookup) {
        int emptyCount = 0;
        int filledCount = 0;
        ItemStack filledPoster = ItemStack.EMPTY;

        for (int i = 0; i < input.getSize(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (stack.isOf(ModItems.POSTER)) {
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
    public DefaultedList<ItemStack> getRemainder(CraftingRecipeInput input) {
        DefaultedList<ItemStack> list = DefaultedList.ofSize(input.getSize(), ItemStack.EMPTY);

        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = input.getStackInSlot(i);
            if (!stack.isEmpty()) {
                if (stack.isOf(ModItems.POSTER) && CanvasData.isSizeChosenForStack(stack)) {
                    ItemStack remainder = stack.copy();
                    remainder.setCount(1);
                    list.set(i, remainder);
                } else {
                    Item remainderItem = stack.getItem().getRecipeRemainder();
                    if (remainderItem != null) {
                        list.set(i, new ItemStack(remainderItem));
                    }
                }
            }
        }

        return list;
    }

    @Override
    public boolean fits(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.POSTER_CLONING;
    }
}