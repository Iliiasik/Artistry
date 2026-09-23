package iliiasik.artistry.client.presence;

import net.minecraft.util.Mth;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CanvasPresence {

    private static final float BADGE_SATURATION = 0.6f;
    private static final float BADGE_VALUE = 0.9f;
    private static final int COLOR_SLOTS = 16;
    private static final int SLOT_BITS = 4;

    public static class RemoteCursor {
        public final UUID uuid;
        public float targetGx;
        public float targetGy;
        public float curGx;
        public float curGy;
        public boolean initialized;

        public RemoteCursor(UUID uuid) { this.uuid = uuid; }
    }

    private final Map<UUID, RemoteCursor> cursors = new HashMap<>();
    private final Map<UUID, Integer> colorSlots = new HashMap<>();
    private final boolean[] slotTaken = new boolean[COLOR_SLOTS];

    public void updateCursor(UUID uuid, float gx, float gy) {
        RemoteCursor c = cursors.computeIfAbsent(uuid, RemoteCursor::new);
        c.targetGx = gx;
        c.targetGy = gy;
        if (!c.initialized) {
            c.curGx = gx;
            c.curGy = gy;
            c.initialized = true;
        }
    }

    public void remove(UUID uuid) {
        cursors.remove(uuid);
        Integer slot = colorSlots.remove(uuid);
        if (slot != null) slotTaken[slot] = false;
    }

    public Collection<RemoteCursor> cursors() {
        return cursors.values();
    }

    public int colorFor(UUID uuid) {
        Integer slot = colorSlots.get(uuid);
        if (slot == null) {
            int free = takeSlot();
            if (free < 0) return hueToColor((uuid.hashCode() & 0xFFFF) / 65535.0f);
            colorSlots.put(uuid, free);
            slot = free;
        }
        return hueToColor(hueForSlot(slot));
    }

    private int takeSlot() {
        for (int i = 0; i < COLOR_SLOTS; i++) {
            if (!slotTaken[i]) {
                slotTaken[i] = true;
                return i;
            }
        }
        return -1;
    }

    private static float hueForSlot(int slot) {
        int spread = 0;
        for (int bit = 0; bit < SLOT_BITS; bit++) {
            spread = (spread << 1) | ((slot >> bit) & 1);
        }
        return spread / (float) COLOR_SLOTS;
    }

    private static int hueToColor(float h) {
        float s = BADGE_SATURATION;
        float v = BADGE_VALUE;
        int i = (int) (h * 6) % 6;
        float f = h * 6 - (int) (h * 6);
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        float r, g, b;
        switch (i) {
            case 0 -> { r = v; g = t; b = p; }
            case 1 -> { r = q; g = v; b = p; }
            case 2 -> { r = p; g = v; b = t; }
            case 3 -> { r = p; g = q; b = v; }
            case 4 -> { r = t; g = p; b = v; }
            default -> { r = v; g = p; b = q; }
        }
        return ((int) (r * 255) << 16) | ((int) (g * 255) << 8) | (int) (b * 255);
    }

    public void interpolate(float factor) {
        for (RemoteCursor c : cursors.values()) {
            c.curGx = Mth.lerp(factor, c.curGx, c.targetGx);
            c.curGy = Mth.lerp(factor, c.curGy, c.targetGy);
        }
    }
}
