package iliiasik.artistry.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import iliiasik.artistry.data.CanvasSignature;
import iliiasik.artistry.item.ModItems;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.Level;
import org.jetbrains.annotations.Nullable;

public class BannerCraftingRecipe extends ShapedRecipe {

    private final ShapedRecipePattern pattern;
    private final ItemStack result;

    public BannerCraftingRecipe(String group, CraftingBookCategory category,
                                ShapedRecipePattern pattern, ItemStack result, boolean showNotification) {
        super(group, category, pattern, result, showNotification);
        this.pattern = pattern;
        this.result = result;
    }

    @Override
    public boolean matches(CraftingInput input, Level world) {
        if (!super.matches(input, world)) return false;
        return source(input) != null;
    }

    @Override
    public ItemStack assemble(CraftingInput input, HolderLookup.Provider registries) {
        ItemStack banner = super.assemble(input, registries);
        ItemStack poster = source(input);
        if (poster == null) return banner;

        CustomData data = poster.get(DataComponents.CUSTOM_DATA);
        if (data != null) {
            banner.set(DataComponents.CUSTOM_DATA, data);
        }
        return banner;
    }

    @Nullable
    private static ItemStack source(CraftingInput input) {
        for (int slot = 0; slot < input.size(); slot++) {
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

        private static final MapCodec<BannerCraftingRecipe> CODEC = RecordCodecBuilder.mapCodec(instance ->
                instance.group(
                        Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                        CraftingBookCategory.CODEC.fieldOf("category")
                                .orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
                        ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> recipe.pattern),
                        ItemStack.STRICT_CODEC.fieldOf("result").forGetter(recipe -> recipe.result),
                        Codec.BOOL.optionalFieldOf("show_notification", Boolean.TRUE)
                                .forGetter(ShapedRecipe::showNotification)
                ).apply(instance, BannerCraftingRecipe::new));

        private static final StreamCodec<RegistryFriendlyByteBuf, BannerCraftingRecipe> STREAM_CODEC =
                StreamCodec.of(Serializer::toNetwork, Serializer::fromNetwork);

        @Override
        public MapCodec<BannerCraftingRecipe> codec() {
            return CODEC;
        }

        @Override
        public StreamCodec<RegistryFriendlyByteBuf, BannerCraftingRecipe> streamCodec() {
            return STREAM_CODEC;
        }

        private static void toNetwork(RegistryFriendlyByteBuf buffer, BannerCraftingRecipe recipe) {
            buffer.writeUtf(recipe.getGroup());
            buffer.writeEnum(recipe.category());
            ShapedRecipePattern.STREAM_CODEC.encode(buffer, recipe.pattern);
            ItemStack.STREAM_CODEC.encode(buffer, recipe.result);
            buffer.writeBoolean(recipe.showNotification());
        }

        private static BannerCraftingRecipe fromNetwork(RegistryFriendlyByteBuf buffer) {
            String group = buffer.readUtf();
            CraftingBookCategory category = buffer.readEnum(CraftingBookCategory.class);
            ShapedRecipePattern pattern = ShapedRecipePattern.STREAM_CODEC.decode(buffer);
            ItemStack result = ItemStack.STREAM_CODEC.decode(buffer);
            boolean showNotification = buffer.readBoolean();
            return new BannerCraftingRecipe(group, category, pattern, result, showNotification);
        }
    }
}
