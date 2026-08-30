package iliiasik.artistry.fabric;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.platform.services.IRegistryHelper;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

import java.util.function.Supplier;

public class FabricRegistryHelper implements IRegistryHelper {

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Supplier<T> register(ResourceKey<? extends Registry<? super T>> registry, String path, Supplier<T> factory) {
        Registry target = BuiltInRegistries.REGISTRY.get(registry.location());
        if (target == null) {
            throw new IllegalStateException("Unknown registry " + registry.location());
        }
        T value = factory.get();
        Registry.register(target, Artistry.id(path), value);
        return () -> value;
    }

    @Override
    public void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
        ItemGroupEvents.modifyEntriesEvent(tab).register(entries -> entries.accept(item.get()));
    }
}
