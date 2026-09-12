package iliiasik.artistry.support;

import iliiasik.artistry.platform.services.IRegistryHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class TestRegistryHelper implements IRegistryHelper {

    public static final Map<String, ResourceKey<? extends Registry<?>>> REGISTERED = new LinkedHashMap<>();
    public static final List<ResourceKey<CreativeModeTab>> CREATIVE_TABS = new ArrayList<>();

    @Override
    public <T> Supplier<T> register(ResourceKey<? extends Registry<? super T>> registry, String path, Supplier<T> factory) {
        REGISTERED.put(registry.location() + "/" + path, registry);
        return new Supplier<>() {
            private T value;

            @Override
            public T get() {
                if (value == null) value = factory.get();
                return value;
            }
        };
    }

    @Override
    public void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
        CREATIVE_TABS.add(tab);
    }
}
