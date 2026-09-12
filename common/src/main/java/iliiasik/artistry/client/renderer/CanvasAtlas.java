package iliiasik.artistry.client.renderer;

import com.mojang.blaze3d.platform.NativeImage;
import iliiasik.artistry.Artistry;
import iliiasik.artistry.debug.ArtistryDebug;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.ResourceLocation;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public final class CanvasAtlas {

    private final String name;
    private final int pageSize;
    private final int slotSize;
    private final int slotsPerRow;
    private final int maxPages;
    private final List<Page> pages = new ArrayList<>();

    private int nextPageId = 0;

    public CanvasAtlas(String name, int pageSize, int slotSize, int maxPages) {
        this.name = name;
        this.pageSize = pageSize;
        this.slotSize = slotSize;
        this.slotsPerRow = pageSize / slotSize;
        this.maxPages = maxPages;
    }

    public Slot allocate() {
        for (Page page : pages) {
            Slot slot = page.take();
            if (slot != null) return slot;
        }
        if (pages.size() >= maxPages) return null;
        Page page = new Page(nextPageId++);
        pages.add(page);
        return page.take();
    }

    public void release(Slot slot) {
        slot.page.give(slot);
    }

    public void sweep(long now) {
        pages.removeIf(page -> {
            if (!page.expired(now)) return false;
            page.close();
            return true;
        });
    }

    public void clear() {
        for (Page page : pages) {
            page.close();
        }
        pages.clear();
        nextPageId = 0;
    }

    public final class Slot {

        private final Page page;
        private final int originX;
        private final int originY;
        private final float u0;
        private final float v0;
        private final float u1;
        private final float v1;

        private Slot(Page page, int originX, int originY) {
            this.page = page;
            this.originX = originX;
            this.originY = originY;
            this.u0 = (float) originX / pageSize;
            this.v0 = (float) originY / pageSize;
            this.u1 = (float) (originX + slotSize) / pageSize;
            this.v1 = (float) (originY + slotSize) / pageSize;
        }

        public ResourceLocation texture() {
            return page.id;
        }

        public NativeImage image() {
            return page.image;
        }

        public int originX() {
            return originX;
        }

        public int originY() {
            return originY;
        }

        public float u0() {
            return u0;
        }

        public float v0() {
            return v0;
        }

        public float u1() {
            return u1;
        }

        public float v1() {
            return v1;
        }

        public void upload() {
            page.upload(originX, originY, slotSize);
        }
    }

    private final class Page {

        private final NativeImage image;
        private final DynamicTexture texture;
        private final ResourceLocation id;
        private final Deque<Slot> free = new ArrayDeque<>();
        private final int capacity;

        private long emptySince;

        private Page(int pageId) {
            image = new NativeImage(pageSize, pageSize, false);
            texture = new DynamicTexture(image);
            id = Artistry.id("canvas_atlas/" + name + "_" + pageId);
            Minecraft.getInstance().getTextureManager().register(id, texture);
            ArtistryDebug.hooks().textureCreated(pageSize);
            ArtistryDebug.hooks().textureUploaded(pageSize);
            capacity = slotsPerRow * slotsPerRow;
            for (int i = capacity - 1; i >= 0; i--) {
                free.push(new Slot(this, (i % slotsPerRow) * slotSize, (i / slotsPerRow) * slotSize));
            }
            emptySince = System.currentTimeMillis();
        }

        private Slot take() {
            Slot slot = free.poll();
            if (slot != null) emptySince = 0;
            return slot;
        }

        private void give(Slot slot) {
            free.push(slot);
            if (free.size() == capacity) emptySince = System.currentTimeMillis();
        }

        private boolean expired(long now) {
            return emptySince != 0 && now - emptySince > RenderTuning.ATLAS_EMPTY_PAGE_GRACE_MILLIS;
        }

        private void upload(int x, int y, int size) {
            texture.bind();
            image.upload(0, x, y, x, y, size, size, false, false, false, false);
            ArtistryDebug.hooks().textureUploaded(size);
        }

        private void close() {
            Minecraft.getInstance().getTextureManager().release(id);
            texture.close();
            ArtistryDebug.hooks().textureClosed(pageSize);
        }
    }
}
