package iliiasik.artistry.client.util;

import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientPacketListener;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public final class PlayerHeads {

    private static final int SKIN_TEXTURE_SIZE = 64;
    private static final int FACE_U = 8;
    private static final int FACE_V = 8;
    private static final int HAT_U = 40;
    private static final int HAT_V = 8;
    private static final int FACE_SIZE = 8;

    private PlayerHeads() {}

    public static ResourceLocation skinOf(@Nullable UUID uuid) {
        if (uuid == null) return DefaultPlayerSkin.getDefaultSkin();

        ClientPacketListener connection = Minecraft.getInstance().getConnection();
        if (connection != null) {
            PlayerInfo info = connection.getPlayerInfo(uuid);
            if (info != null) return info.getSkinLocation();
        }
        return DefaultPlayerSkin.getDefaultSkin(uuid);
    }

    public static void draw(net.minecraft.client.gui.GuiGraphics ctx,
                            ResourceLocation skin, int x, int y, int size) {
        ctx.blit(skin, x, y, size, size,
                FACE_U, FACE_V, FACE_SIZE, FACE_SIZE, SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE);
        ctx.blit(skin, x, y, size, size,
                HAT_U, HAT_V, FACE_SIZE, FACE_SIZE, SKIN_TEXTURE_SIZE, SKIN_TEXTURE_SIZE);
    }
}
