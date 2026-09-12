package iliiasik.artistry.network;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ChunkAssembler {

    private static final int MAX_TOTAL = 64 * 1024 * 1024;

    private final Map<UUID, Entry> active = new HashMap<>();

    private static final class Entry {
        final byte[] data;
        int received;

        Entry(int total) {
            this.data = new byte[total];
        }
    }

    public byte @Nullable [] accept(UUID id, int total, int offset, byte[] chunk) {
        Entry entry;
        if (offset == 0) {
            if (total < 0 || total > MAX_TOTAL) {
                active.remove(id);
                return null;
            }
            entry = new Entry(total);
            active.put(id, entry);
        } else {
            entry = active.get(id);
            if (entry == null || entry.data.length != total) {
                active.remove(id);
                return null;
            }
        }

        if (offset < 0 || offset + chunk.length > entry.data.length) {
            active.remove(id);
            return null;
        }

        System.arraycopy(chunk, 0, entry.data, offset, chunk.length);
        entry.received += chunk.length;
        if (entry.received >= entry.data.length) {
            active.remove(id);
            return entry.data;
        }
        return null;
    }

    public void forget(UUID id) {
        active.remove(id);
    }

    public void clear() {
        active.clear();
    }
}
