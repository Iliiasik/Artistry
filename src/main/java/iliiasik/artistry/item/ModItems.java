package iliiasik.artistry.item;

import iliiasik.artistry.Artistry;
import net.minecraft.item.Item;
import net.minecraft.item.ItemGroups;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.fabricmc.fabric.api.itemgroup.v1.ItemGroupEvents;

public class ModItems {
    public static final PosterItem POSTER = new PosterItem(
            new Item.Settings().maxCount(64));

    public static void register() {
        Registry.register(Registries.ITEM, Artistry.id("poster"), POSTER);
        ItemGroupEvents.modifyEntriesEvent(ItemGroups.TOOLS).register(entries -> entries.add(POSTER));
    }
}