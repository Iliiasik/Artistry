package iliiasik.artistry.neoforge;

import iliiasik.artistry.Artistry;
import iliiasik.artistry.platform.services.IRegistryHelper;
import net.minecraft.core.Registry;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.level.ItemLike;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.neoforge.event.BuildCreativeModeTabContentsEvent;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

public class NeoForgeRegistryHelper implements IRegistryHelper {

    private static final Map<ResourceKey<? extends Registry<?>>, DeferredRegister<?>> REGISTERS = new LinkedHashMap<>();
    private static final Map<ResourceKey<CreativeModeTab>, List<Supplier<? extends ItemLike>>> TAB_ENTRIES = new LinkedHashMap<>();

    private static IEventBus modEventBus;

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> Supplier<T> register(ResourceKey<? extends Registry<? super T>> registry, String path, Supplier<T> factory) {
        DeferredRegister deferred = REGISTERS.computeIfAbsent(registry, key -> {
            DeferredRegister created = DeferredRegister.create((ResourceKey) key, Artistry.MOD_ID);
            if (modEventBus != null) created.register(modEventBus);
            return created;
        });
        return (Supplier<T>) deferred.register(path, factory);
    }

    @Override
    public void addToCreativeTab(ResourceKey<CreativeModeTab> tab, Supplier<? extends ItemLike> item) {
        TAB_ENTRIES.computeIfAbsent(tab, key -> new ArrayList<>()).add(item);
    }

    public static void bind(IEventBus bus) {
        modEventBus = bus;
        for (DeferredRegister<?> register : REGISTERS.values()) {
            register.register(bus);
        }
        bus.addListener(NeoForgeRegistryHelper::onBuildCreativeTabContents);
    }

    private static void onBuildCreativeTabContents(BuildCreativeModeTabContentsEvent event) {
        List<Supplier<? extends ItemLike>> entries = TAB_ENTRIES.get(event.getTabKey());
        if (entries == null) return;
        for (Supplier<? extends ItemLike> entry : entries) {
            event.accept(entry.get());
        }
    }
}
