package iliiasik.artistry.client.renderer;

import iliiasik.artistry.client.palette.BlockPalette;
import iliiasik.artistry.data.CanvasData;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;

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

    public static ResourceLocation getTexture(ItemStack stack) {
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
        CompoundTag tag = stack.getTag();
        return tag == null ? 0 : tag.hashCode();
    }

    private static CanvasData readCanvasData(ItemStack stack) {
        CanvasData data = new CanvasData();
        CompoundTag nbt = stack.getTag();
        if (nbt != null && nbt.contains("canvas")) {
            data.fromNbt(nbt.getCompound("canvas"));
        }
        return data;
    }

    private static class CacheEntry {
        private final CanvasTextureHolder holder;

        CacheEntry() {
            ResourceLocation id = new ResourceLocation("artistry",
                    "poster_item_" + UUID.randomUUID().toString().replace("-", ""));
            holder = new CanvasTextureHolder(id, TEX_SIZE);
        }

        void update(CanvasData data) {
            BlockPalette.ensureLoaded();
            holder.updatePixels(data, TEX_SIZE);
        }

        ResourceLocation getTextureId() { return holder.getTextureId(); }
        void close() { holder.close(); }
    }
}