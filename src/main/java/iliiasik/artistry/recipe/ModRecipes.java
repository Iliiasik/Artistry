package iliiasik.artistry.recipe;

import iliiasik.artistry.Artistry;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {
    public static final DeferredRegister<RecipeSerializer<?>> RECIPE_SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Artistry.MOD_ID);

    public static final RegistryObject<RecipeSerializer<PosterCloningRecipe>> POSTER_CLONING =
            RECIPE_SERIALIZERS.register("poster_cloning",
                    () -> new SimpleCraftingRecipeSerializer<>(PosterCloningRecipe::new));

    public static void register(IEventBus bus) {
        RECIPE_SERIALIZERS.register(bus);
    }
}