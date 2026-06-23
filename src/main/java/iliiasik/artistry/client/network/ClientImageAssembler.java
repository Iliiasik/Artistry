package iliiasik.artistry.client.network;

import iliiasik.artistry.network.DeliverImageS2CPacket;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public final class ClientImageAssembler {
    private static final int MAX_TOTAL = 64 * 1024 * 1024;
    private static final Map<UUID, Entry> ACTIVE = new HashMap<>();

    private ClientImageAssembler() {
    }

    private static final class Entry {
        final byte[] data;
        int received;
        Entry(int total) {
            this.data = new byte[total];
        }
    }

    public static byte[] accept(DeliverImageS2CPacket packet) {
        UUID id = packet.uuid();
        Entry entry;
        if (packet.offset() == 0) {
            if (packet.total() < 0 || packet.total() > MAX_TOTAL) {
                ACTIVE.remove(id);
                return null;
            }
            entry = new Entry(packet.total());
            ACTIVE.put(id, entry);
        } else {
            entry = ACTIVE.get(id);
            if (entry == null || entry.data.length != packet.total()) {
                ACTIVE.remove(id);
                return null;
            }
        }
        byte[] chunk = packet.chunk();
        if (packet.offset() < 0 || packet.offset() + chunk.length > entry.data.length) {
            ACTIVE.remove(id);
            return null;
        }
        System.arraycopy(chunk, 0, entry.data, packet.offset(), chunk.length);
        entry.received += chunk.length;
        if (entry.received >= entry.data.length) {
            ACTIVE.remove(id);
            return entry.data;
        }
        return null;
    }
}