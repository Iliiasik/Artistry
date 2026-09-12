package iliiasik.artistry.network;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.config.ArtistryConfig;
import iliiasik.artistry.data.CanvasData;
import iliiasik.artistry.data.CanvasDirtySet;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("resource")
public final class CanvasRoom {

    private record Loc(ServerLevel level, BlockPos pos) {}

    private static final Map<Loc, CanvasDirtySet> pending = new HashMap<>();

    private static long lastFlush = 0;

    private CanvasRoom() {}

    public static void publish(ServerLevel level, BlockPos pos, ServerPlayer author,
                               List<CanvasData.PixelChange> changes) {
        if (changes.isEmpty()) return;
        BlockPos key = pos.immutable();

        SyncCanvasS2CPacket packet = new SyncCanvasS2CPacket(key, changes);
        for (ServerPlayer viewer : PosterPresence.viewersOf(level, key)) {
            if (viewer.getUUID().equals(author.getUUID())) continue;
            ArtistryNetwork.sendToPlayer(viewer, packet);
        }

        pending.computeIfAbsent(new Loc(level, key), k -> new CanvasDirtySet()).add(changes);
    }

    public static void tick() {
        if (pending.isEmpty()) return;
        long now = System.currentTimeMillis();
        if (now - lastFlush < ArtistryConfig.get().network.worldSyncIntervalMs) return;
        lastFlush = now;

        Iterator<Map.Entry<Loc, CanvasDirtySet>> it = pending.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<Loc, CanvasDirtySet> entry = it.next();
            it.remove();
            deliver(entry.getKey(), entry.getValue());
        }
    }

    public static void flush(ServerLevel level, BlockPos pos) {
        Loc loc = new Loc(level, pos.immutable());
        CanvasDirtySet dirty = pending.remove(loc);
        if (dirty != null) deliver(loc, dirty);
    }

    public static void forget(ServerLevel level, BlockPos pos) {
        pending.remove(new Loc(level, pos.immutable()));
    }

    public static void clear() {
        pending.clear();
        lastFlush = 0;
    }

    private static void deliver(Loc loc, CanvasDirtySet dirty) {
        if (dirty.isEmpty()) return;
        if (!(loc.level().getBlockEntity(loc.pos()) instanceof PosterBlockEntity poster)) return;

        Set<UUID> room = PosterPresence.viewerIdsOf(loc.level(), loc.pos());
        ArtistryNetwork.sendNearExcept(loc.level(), loc.pos(), room,
                new SyncCanvasS2CPacket(loc.pos(), dirty.collect(poster.canvasData)));
    }
}
