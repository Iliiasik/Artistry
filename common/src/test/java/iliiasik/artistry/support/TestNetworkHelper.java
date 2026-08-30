package iliiasik.artistry.support;

import iliiasik.artistry.platform.services.INetworkHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestNetworkHelper implements INetworkHelper {

    public static final List<CustomPacketPayload> SENT_TO_PLAYER = Collections.synchronizedList(new ArrayList<>());
    public static final List<CustomPacketPayload> SENT_TO_SERVER = Collections.synchronizedList(new ArrayList<>());

    public static void reset() {
        SENT_TO_PLAYER.clear();
        SENT_TO_SERVER.clear();
    }

    @Override
    public void sendToPlayer(ServerPlayer player, CustomPacketPayload payload) {
        SENT_TO_PLAYER.add(payload);
    }

    @Override
    public void sendToServer(CustomPacketPayload payload) {
        SENT_TO_SERVER.add(payload);
    }
}
