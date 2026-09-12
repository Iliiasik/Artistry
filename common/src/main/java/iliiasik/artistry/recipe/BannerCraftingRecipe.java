package iliiasik.artistry.recipe;

import com.google.gson.JsonObject;
import iliiasik.artistry.data.CanvasSignature;
import iliiasik.artistry.item.ModItems;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class BannerCraftingRecipe extends ShapedRecipe {

    public BannerCraftingRecipe(ResourceLocation id, String group, CraftingBookCategory category,
                                int width, int height, NonNullList<Ingredient> ingredients,
                                ItemStack result, boolean showNotification) {
        super(id, group, category, width, height, ingredients, result, showNotification);
    }

    @Override
    public boolean matches(CraftingContainer input, Level world) {
        if (!super.matches(input, world)) return false;
        return source(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingContainer input, RegistryAccess registryAccess) {
        ItemStack banner = super.assemble(input, registryAccess);
        ItemStack poster = source(input);
        if (poster == null) return banner;

        CompoundTag tag = poster.getTag();
        if (tag != null) {
            banner.setTag(tag.copy());
        }
        return banner;
    }

    @Nullable
    private static ItemStack source(CraftingContainer input) {
        for (int slot = 0; slot < input.getContainerSize(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (!stack.is(ModItems.POSTER.get())) continue;
            return CanvasSignature.isSignedStack(stack) ? null : stack;
        }
        return null;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.BANNER_CRAFTING.get();
    }

    public static final class Serializer implements RecipeSerializer<BannerCraftingRecipe> {

        private static final ShapedRecipe.Serializer VANILLA = new ShapedRecipe.Serializer();

        @Override
        public BannerCraftingRecipe fromJson(ResourceLocation id, JsonObject json) {
            return rewrap(VANILLA.fromJson(id, json));
        }

        @Override
        public BannerCraftingRecipe fromNetwork(ResourceLocation id, FriendlyByteBuf buffer) {
            return rewrap(VANILLA.fromNetwork(id, buffer));
        }

        @Override
        public void toNetwork(FriendlyByteBuf buffer, BannerCraftingRecipe recipe) {
            VANILLA.toNetwork(buffer, recipe);
        }

        private static BannerCraftingRecipe rewrap(ShapedRecipe base) {
            return new BannerCraftingRecipe(base.getId(), base.getGroup(), base.category(),
                    base.getWidth(), base.getHeight(), base.getIngredients(),
                    base.getResultItem(RegistryAccess.EMPTY), base.showNotification());
        }
    }
}
