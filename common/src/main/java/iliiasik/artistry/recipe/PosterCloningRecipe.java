package iliiasik.artistry.recipe;

import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasSignature;
import iliiasik.artistry.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.NonNullList;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class PosterCloningRecipe extends CustomRecipe {

    private record Pair(ItemStack filled, ItemStack empty) {}

    public PosterCloningRecipe(CraftingBookCategory category) {
        super(category);
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        return resolve(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider lookup) {
        Pair pair = resolve(input);
        if (pair == null) return ItemStack.EMPTY;

        ItemStack result = new ItemStack(pair.empty().getItem());
        CustomData data = pair.filled().get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            result.set(DataComponents.CUSTOM_DATA, data);
        }
        return result;
    }

    @Nullable
    private static Pair resolve(CraftingInput input) {
        ItemStack filled = ItemStack.EMPTY;
        ItemStack empty = ItemStack.EMPTY;

        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;
            if (!ModItems.isCanvas(stack)) return null;
            if (CanvasSignature.isSignedStack(stack)) return null;

            if (CanvasData.isSizeChosenForStack(stack)) {
                if (!filled.isEmpty()) return null;
                filled = stack;
            } else {
                if (!empty.isEmpty()) return null;
                empty = stack;
            }
        }

        if (filled.isEmpty() || empty.isEmpty()) return null;
        return new Pair(filled, empty);
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingInput input) {
        NonNullList<ItemStack> list = NonNullList.withSize(input.size(), ItemStack.EMPTY);

        for (int i = 0; i < list.size(); i++) {
            ItemStack stack = input.getItem(i);
            if (stack.isEmpty()) continue;

            if (ModItems.isCanvas(stack) && CanvasData.isSizeChosenForStack(stack)) {
                ItemStack remainder = stack.copy();
                remainder.setCount(1);
                list.set(i, remainder);
            } else {
                Item remainder = stack.getItem().getCraftingRemainingItem();
                if (remainder != null) {
                    list.set(i, new ItemStack(remainder));
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
