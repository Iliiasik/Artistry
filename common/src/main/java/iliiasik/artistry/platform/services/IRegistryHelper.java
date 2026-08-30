package iliiasik.artistry.platform.services;

import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

public interface IRegistryHelper {

    <T> Supplier<T> register(ResourceKey<? extends Registry<? super T>> registry, String path, Supplier<T> factory);

    void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item);
}
