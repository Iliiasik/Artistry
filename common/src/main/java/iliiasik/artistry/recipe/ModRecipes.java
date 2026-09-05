package iliiasik.artistry.recipe;

import iliiasik.artistry.platform.Services;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;

import java.util.function.Supplier;

public final class ModRecipes {

    public static final Supplier<RecipeSerializer<PosterCloningRecipe>> POSTER_CLONING =
            Services.REGISTRY.register(Registries.RECIPE_SERIALIZER, "poster_cloning",
                    () -> new SimpleCraftingRecipeSerializer<>(PosterCloningRecipe::new));

    public static final Supplier<RecipeSerializer<BannerCraftingRecipe>> BANNER_CRAFTING =
            Services.REGISTRY.register(Registries.RECIPE_SERIALIZER, "banner_crafting",
                    BannerCraftingRecipe.Serializer::new);

    private ModRecipes() {}

    public static void init() {}
}
