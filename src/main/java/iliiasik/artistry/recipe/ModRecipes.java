package iliiasik.artistry.recipe;

import iliiasik.artistry.Artistry;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.SpecialRecipeSerializer;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;

public class ModRecipes {
    public static final RecipeSerializer<PosterCloningRecipe> POSTER_CLONING = Registry.register(
            Registries.RECIPE_SERIALIZER,
            Artistry.id("poster_cloning"),
            new SpecialRecipeSerializer<>(PosterCloningRecipe::new)
    );

    public static void register() {
    }
}
