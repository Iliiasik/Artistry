package iliiasik.artistry.recipe;

import iliiasik.artistry.Artistry;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(Registries.RECIPE_SERIALIZER, Artistry.MOD_ID);

    public static final Supplier<RecipeSerializer<PosterCloningRecipe>> POSTER_CLONING =
            RECIPE_SERIALIZERS.register("poster_cloning",
                    () -> new SimpleCraftingRecipeSerializer<>(PosterCloningRecipe::new));

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
}