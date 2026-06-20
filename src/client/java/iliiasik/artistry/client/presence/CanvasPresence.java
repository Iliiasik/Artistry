package iliiasik.artistry.client.presence;

import net.minecraft.util.math.MathHelper;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class CanvasPresence {

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
    }

    public void clear() {
        cursors.clear();
    }

    public Collection<RemoteCursor> cursors() {
        return cursors.values();
    }

    public void interpolate(float factor) {
        for (RemoteCursor c : cursors.values()) {
            c.curGx = MathHelper.lerp(factor, c.curGx, c.targetGx);
            c.curGy = MathHelper.lerp(factor, c.curGy, c.targetGy);
        }
    }
}