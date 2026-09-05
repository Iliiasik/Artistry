package iliiasik.artistry.network;

import iliiasik.artistry.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;

import org.jetbrains.annotations.Nullable;

import java.util.Set;
import java.util.UUID;

public final class ArtistryNetwork {

    private ArtistryNetwork() {}

    public static void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        Services.NETWORK.sendToPlayer(player, payload);
    }

    public static void sendToServer(CustomPacketPayload payload) {
        Services.NETWORK.sendToServer(payload);
    }

    public static void sendNear(ServerLevel level, BlockPos pos,
                                @Nullable ServerPlayer exclude, CustomPacketPayload payload) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        for (ServerPlayer player : level.players()) {
            if (player == exclude) continue;
            if (player.getChunkTrackingView().isInViewDistance(chunkX, chunkZ)) {
                sendToPlayer(player, payload);
            }
        }
    }

    public static void sendNearExcept(ServerLevel level, BlockPos pos,
                                      Set<UUID> exclude, CustomPacketPayload payload) {
        int chunkX = SectionPos.blockToSectionCoord(pos.getX());
        int chunkZ = SectionPos.blockToSectionCoord(pos.getZ());
        for (ServerPlayer player : level.players()) {
            if (exclude.contains(player.getUUID())) continue;
            if (player.getChunkTrackingView().isInViewDistance(chunkX, chunkZ)) {
                sendToPlayer(player, payload);
            }
        }
    }

    public static void broadcastPosterRemoved(ServerLevel level, BlockPos pos) {
        CanvasRoom.forget(level, pos);
        sendNear(level, pos, null, new PosterRemovedS2CPacket(pos));
    }
}
