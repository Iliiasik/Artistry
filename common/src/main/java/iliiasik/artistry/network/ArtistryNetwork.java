package iliiasik.artistry.network;

import iliiasik.artistry.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.ChunkPos;
import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public final class ArtistryNetwork {

    public static final int UPLOAD_CHUNK_SIZE = 30000;
    public static final int DELIVER_CHUNK_SIZE = 900000;

    private ArtistryNetwork() {}

    public static void sendToPlayer(ServerPlayer player, ArtistryPacket packet) {
        Services.NETWORK.sendToPlayer(player, packet);
    }

    public static void sendToServer(ArtistryPacket packet) {
        Services.NETWORK.sendToServer(packet);
    }

    public static void sendNear(ServerLevel level, BlockPos pos,
                                @Nullable ServerPlayer exclude, ArtistryPacket packet) {
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)) {
            if (player == exclude) continue;
            sendToPlayer(player, packet);
        }
    }

    public static void sendNearExcept(ServerLevel level, BlockPos pos,
                                      Set<UUID> exclude, ArtistryPacket packet) {
        for (ServerPlayer player : level.getChunkSource().chunkMap.getPlayers(new ChunkPos(pos), false)) {
            if (exclude.contains(player.getUUID())) continue;
            sendToPlayer(player, packet);
        }
    }

    public static void broadcastPosterRemoved(ServerLevel level, BlockPos pos) {
        CanvasRoom.forget(level, pos);
        sendNear(level, pos, null, new PosterRemovedS2CPacket(pos));
    }

    public static void uploadImage(PosterTarget target, byte[] bytes) {
        int total = bytes.length;
        if (total == 0) {
            sendToServer(new UploadImageC2SPacket(target, 0, 0, new byte[0]));
            return;
        }
        for (int offset = 0; offset < total; offset += UPLOAD_CHUNK_SIZE) {
            int len = Math.min(UPLOAD_CHUNK_SIZE, total - offset);
            byte[] chunk = new byte[len];
            System.arraycopy(bytes, offset, chunk, 0, len);
            sendToServer(new UploadImageC2SPacket(target, total, offset, chunk));
        }
    }

    public static void deliverImage(ServerPlayer player, UUID uuid, byte[] bytes) {
        int total = bytes.length;
        if (total == 0) {
            sendToPlayer(player, new DeliverImageS2CPacket(uuid, 0, 0, new byte[0]));
            return;
        }
        for (int offset = 0; offset < total; offset += DELIVER_CHUNK_SIZE) {
            int len = Math.min(DELIVER_CHUNK_SIZE, total - offset);
            byte[] chunk = new byte[len];
            System.arraycopy(bytes, offset, chunk, 0, len);
            sendToPlayer(player, new DeliverImageS2CPacket(uuid, total, offset, chunk));
        }
    }
}
