package iliiasik.artistry.network;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasImage;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PosterPresence {

    private record Loc(ServerLevel level, BlockPos pos) {}

    private static final Map<Loc, Map<UUID, ServerPlayer>> viewers = new HashMap<>();
    private static final Map<UUID, Set<Loc>> byPlayer = new HashMap<>();

    public static void open(ServerPlayer player, ServerLevel level, BlockPos pos) {
        Loc loc = new Loc(level, pos.immutable());
        viewers.computeIfAbsent(loc, k -> new HashMap<>()).put(player.getUUID(), player);
        byPlayer.computeIfAbsent(player.getUUID(), k -> new HashSet<>()).add(loc);
    }

    public static void close(ServerPlayer player, ServerLevel level, BlockPos pos) {
        Loc loc = new Loc(level, pos.immutable());
        removeViewer(loc, player.getUUID());
        Set<Loc> set = byPlayer.get(player.getUUID());
        if (set != null) {
            set.remove(loc);
            if (set.isEmpty()) byPlayer.remove(player.getUUID());
        }
        releaseLocks(player.getUUID(), level, pos);
        broadcastLeave(loc, player.getUUID());
    }

    public static void disconnect(ServerPlayer player) {
        Set<Loc> set = byPlayer.remove(player.getUUID());
        if (set == null) return;
        for (Loc loc : set) {
            removeViewer(loc, player.getUUID());
            releaseLocks(player.getUUID(), loc.level(), loc.pos());
            broadcastLeave(loc, player.getUUID());
        }
    }

    public static boolean tryOpen(ServerPlayer player, ServerLevel level, BlockPos pos, int maxEditors) {
        Loc loc = new Loc(level, pos.immutable());
        Map<UUID, ServerPlayer> v = viewers.get(loc);
        boolean alreadyViewer = v != null && v.containsKey(player.getUUID());
        int count = v == null ? 0 : v.size();
        if (maxEditors > 0 && !alreadyViewer && count >= maxEditors) {
            return false;
        }
        open(player, level, pos);
        return true;
    }

    public static void updateCursor(ServerPlayer player, ServerLevel level, BlockPos pos, short gx, short gy) {
        Loc loc = new Loc(level, pos.immutable());
        Map<UUID, ServerPlayer> v = viewers.get(loc);
        if (v == null || !v.containsKey(player.getUUID())) return;
        CanvasCursorS2CPacket packet = new CanvasCursorS2CPacket(pos, player.getUUID(), gx, gy);
        for (ServerPlayer other : v.values()) {
            if (other.getUUID().equals(player.getUUID())) continue;
            PacketDistributor.sendToPlayer(other, packet);
        }
    }

    private static void removeViewer(Loc loc, UUID playerUuid) {
        Map<UUID, ServerPlayer> v = viewers.get(loc);
        if (v != null) {
            v.remove(playerUuid);
            if (v.isEmpty()) viewers.remove(loc);
        }
    }

    private static void broadcastLeave(Loc loc, UUID playerUuid) {
        Map<UUID, ServerPlayer> v = viewers.get(loc);
        if (v == null) return;
        CanvasPresenceLeaveS2CPacket packet = new CanvasPresenceLeaveS2CPacket(loc.pos(), playerUuid);
        for (ServerPlayer other : v.values()) {
            PacketDistributor.sendToPlayer(other, packet);
        }
    }

    private static void releaseLocks(UUID playerUuid, ServerLevel level, BlockPos pos) {
        if (!(level.getBlockEntity(pos) instanceof PosterBlockEntity poster)) return;
        boolean changed = false;
        for (CanvasImage img : poster.imageLayer.getImages()) {
            if (playerUuid.equals(img.lockedByPlayer)) {
                img.lockedByPlayer = null;
                changed = true;
                PosterAccess.sendNear(level, pos, null, new SyncImageLockS2CPacket(pos, img.uuid, null));
            }
        }
        if (changed) {
            poster.markDirtyAndSync();
            PosterAccess.sendNear(level, pos, null, new SyncImageLayerS2CPacket(pos, poster.imageLayer.getImages()));
        }
    }
}