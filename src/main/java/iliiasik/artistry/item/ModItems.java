package iliiasik.artistry.item;

import iliiasik.artistry.Artistry;
import net.minecraft.item.Item;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKey;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.Registries;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;
import net.minecraft.item.ItemGroups;

public class ModItems {
    public static final RegistryKey<Item> POSTER_KEY = RegistryKey.of(RegistryKeys.ITEM, Artistry.id("poster"));

    public static final PosterItem POSTER = new PosterItem(
            new Item.Settings().maxCount(1).registryKey(POSTER_KEY));

    public static void register() {
        Registry.register(Registries.ITEM, POSTER_KEY, POSTER);

        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> {
            entries.add(POSTER);
        });
    }
}