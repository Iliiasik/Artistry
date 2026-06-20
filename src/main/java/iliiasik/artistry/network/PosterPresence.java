package iliiasik.artistry.network;

import iliiasik.artistry.block.entity.PosterBlockEntity;
import iliiasik.artistry.data.CanvasImage;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.math.BlockPos;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

public class PosterPresence {

    private record Loc(ServerWorld world, BlockPos pos) {}

    private static final Map<Loc, Map<UUID, ServerPlayerEntity>> viewers = new HashMap<>();
    private static final Map<UUID, Set<Loc>> byPlayer = new HashMap<>();

    public static void open(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        Loc loc = new Loc(world, pos.toImmutable());
        viewers.computeIfAbsent(loc, k -> new HashMap<>()).put(player.getUuid(), player);
        byPlayer.computeIfAbsent(player.getUuid(), k -> new HashSet<>()).add(loc);
    }

    public static void close(ServerPlayerEntity player, ServerWorld world, BlockPos pos) {
        Loc loc = new Loc(world, pos.toImmutable());
        removeViewer(loc, player.getUuid());
        Set<Loc> set = byPlayer.get(player.getUuid());
        if (set != null) {
            set.remove(loc);
            if (set.isEmpty()) byPlayer.remove(player.getUuid());
        }
        releaseLocks(player.getUuid(), world, pos);
        broadcastLeave(loc, player.getUuid());
    }

    public static void disconnect(ServerPlayerEntity player) {
        Set<Loc> set = byPlayer.remove(player.getUuid());
        if (set == null) return;
        for (Loc loc : set) {
            removeViewer(loc, player.getUuid());
            releaseLocks(player.getUuid(), loc.world(), loc.pos());
            broadcastLeave(loc, player.getUuid());
        }
    }

    public static boolean tryOpen(ServerPlayerEntity player, ServerWorld world, BlockPos pos, int maxEditors) {
        Loc loc = new Loc(world, pos.toImmutable());
        Map<UUID, ServerPlayerEntity> v = viewers.get(loc);
        boolean alreadyViewer = v != null && v.containsKey(player.getUuid());
        int count = v == null ? 0 : v.size();
        if (maxEditors > 0 && !alreadyViewer && count >= maxEditors) {
            return false;
        }
        open(player, world, pos);
        return true;
    }

    public static void updateCursor(ServerPlayerEntity player, ServerWorld world, BlockPos pos, short gx, short gy) {
        Loc loc = new Loc(world, pos.toImmutable());
        Map<UUID, ServerPlayerEntity> v = viewers.get(loc);
        if (v == null || !v.containsKey(player.getUuid())) return;
        CanvasCursorS2CPacket packet = new CanvasCursorS2CPacket(pos, player.getUuid(), gx, gy);
        for (ServerPlayerEntity other : v.values()) {
            if (other.getUuid().equals(player.getUuid())) continue;
            ServerPlayNetworking.send(other, packet);
        }
    }

    private static void removeViewer(Loc loc, UUID playerUuid) {
        Map<UUID, ServerPlayerEntity> v = viewers.get(loc);
        if (v != null) {
            v.remove(playerUuid);
            if (v.isEmpty()) viewers.remove(loc);
        }
    }

    private static void broadcastLeave(Loc loc, UUID playerUuid) {
        Map<UUID, ServerPlayerEntity> v = viewers.get(loc);
        if (v == null) return;
        CanvasPresenceLeaveS2CPacket packet = new CanvasPresenceLeaveS2CPacket(loc.pos(), playerUuid);
        for (ServerPlayerEntity other : v.values()) {
            ServerPlayNetworking.send(other, packet);
        }
    }

    private static void releaseLocks(UUID playerUuid, ServerWorld world, BlockPos pos) {
        if (!(world.getBlockEntity(pos) instanceof PosterBlockEntity poster)) return;
        boolean changed = false;
        for (CanvasImage img : poster.imageLayer.getImages()) {
            if (playerUuid.equals(img.lockedByPlayer)) {
                img.lockedByPlayer = null;
                changed = true;
                PosterAccess.sendNear(world, pos, null, new SyncImageLockS2CPacket(pos, img.uuid, null));
            }
        }
        if (changed) {
            poster.markDirtyAndSync();
            PosterAccess.sendNear(world, pos, null, new SyncImageLayerS2CPacket(pos, poster.imageLayer.getImages()));
        }
    }
}