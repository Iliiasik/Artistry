package iliiasik.artistry.fabric;

import iliiasik.artistry.network.CanvasCursorC2SPacket;
import iliiasik.artistry.network.CanvasEnterRequestC2SPacket;
import iliiasik.artistry.network.CanvasViewC2SPacket;
import iliiasik.artistry.network.DeleteCanvasImageC2SPacket;
import iliiasik.artistry.network.LockCanvasImageC2SPacket;
import iliiasik.artistry.network.MoveCanvasImageC2SPacket;
import iliiasik.artistry.network.RequestImageC2SPacket;
import iliiasik.artistry.network.SaveCanvasC2SPacket;
import iliiasik.artistry.network.ServerPacketHandlers;
import iliiasik.artistry.network.SetCanvasSizeC2SPacket;
import iliiasik.artistry.network.SignPosterC2SPacket;
import iliiasik.artistry.network.TogglePixelizeC2SPacket;
import iliiasik.artistry.network.UploadImageC2SPacket;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;

import java.util.function.BiConsumer;
import java.util.function.Function;

public final class FabricModNetwork {

    private FabricModNetwork() {}

    public static void register() {
        receive(SetCanvasSizeC2SPacket.ID, SetCanvasSizeC2SPacket::read, ServerPacketHandlers::onSetCanvasSize);
        receive(SaveCanvasC2SPacket.ID, SaveCanvasC2SPacket::read, ServerPacketHandlers::onSaveCanvas);
        receive(UploadImageC2SPacket.ID, UploadImageC2SPacket::read, ServerPacketHandlers::onUploadImage);
        receive(MoveCanvasImageC2SPacket.ID, MoveCanvasImageC2SPacket::read, ServerPacketHandlers::onMoveImage);
        receive(DeleteCanvasImageC2SPacket.ID, DeleteCanvasImageC2SPacket::read, ServerPacketHandlers::onDeleteImage);
        receive(TogglePixelizeC2SPacket.ID, TogglePixelizeC2SPacket::read, ServerPacketHandlers::onTogglePixelize);
        receive(LockCanvasImageC2SPacket.ID, LockCanvasImageC2SPacket::read, ServerPacketHandlers::onLockImage);
        receive(RequestImageC2SPacket.ID, RequestImageC2SPacket::read, ServerPacketHandlers::onRequestImage);
        receive(CanvasViewC2SPacket.ID, CanvasViewC2SPacket::read, ServerPacketHandlers::onCanvasView);
        receive(CanvasCursorC2SPacket.ID, CanvasCursorC2SPacket::read, ServerPacketHandlers::onCanvasCursor);
        receive(CanvasEnterRequestC2SPacket.ID, CanvasEnterRequestC2SPacket::read, ServerPacketHandlers::onCanvasEnterRequest);
        receive(SignPosterC2SPacket.ID, SignPosterC2SPacket::read, ServerPacketHandlers::onSignPoster);
    }

    private static <T> void receive(ResourceLocation channel,
                                    Function<FriendlyByteBuf, T> decoder,
                                    BiConsumer<T, ServerPlayer> handler) {
        ServerPlayNetworking.registerGlobalReceiver(channel, (server, player, listener, buf, responseSender) -> {
            T packet = decoder.apply(buf);
            server.execute(() -> handler.accept(packet, player));
        });
    }
}
