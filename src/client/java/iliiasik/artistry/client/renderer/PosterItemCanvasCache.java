package iliiasik.artistry.client.renderer;

import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.util.Identifier;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class PosterItemCanvasCache {

    private static final int MAX_ENTRIES = 16;
    private static final int TEX_SIZE = 512;

    private static final LinkedHashMap<Integer, CacheEntry> CACHE =
            new LinkedHashMap<>(MAX_ENTRIES, 0.75f, true) {
                @Override
                protected boolean removeEldestEntry(Map.Entry<Integer, CacheEntry> eldest) {
                    if (size() > MAX_ENTRIES) {
                        eldest.getValue().close();
                        return true;
                    }
                    return false;
                }
            };

    private PosterItemCanvasCache() {}

    public static Identifier getTexture(ItemStack stack) {
        int hash = nbtHash(stack);
        CacheEntry entry = CACHE.get(hash);
        if (entry == null) {
            entry = new CacheEntry();
            CACHE.put(hash, entry);
        }
        CanvasData data = readCanvasData(stack);
        entry.update(data);
        return entry.getTextureId();
    }

    private static int nbtHash(ItemStack stack) {
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return 0;
        return comp.copyNbt().hashCode();
    }

    private static CanvasData readCanvasData(ItemStack stack) {
        CanvasData data = new CanvasData();
        NbtComponent comp = stack.get(DataComponentTypes.CUSTOM_DATA);
        if (comp == null) return data;
        NbtCompound nbt = comp.copyNbt();
        if (nbt.contains("canvas")) {
            data.fromNbt(nbt.getCompound("canvas"));
        }
        return data;
    }

    private static class CacheEntry {
        private final CanvasTextureHolder holder;

        CacheEntry() {
            Identifier id = Identifier.of("artistry",
                    "poster_item_" + UUID.randomUUID().toString().replace("-", ""));
            holder = new CanvasTextureHolder(id, TEX_SIZE);
        }

        void update(CanvasData data) {
            BlockPalette.ensureLoaded();
            holder.updatePixels(data, TEX_SIZE);
        }

        Identifier getTextureId() { return holder.getTextureId(); }
        void close() { holder.close(); }
    }
}