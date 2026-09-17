package iliiasik.artistry.client.ui.screen.paint;

import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.util.ModTextures;
import iliiasik.artistry.client.util.PlayerHeads;
import iliiasik.artistry.client.util.TextUtil;
import iliiasik.artistry.data.CanvasSignature;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.UUID;

public final class SignatureRenderer {

    private static final int BADGE_TEXTURE_WIDTH = PaintDimensions.BADGE_TEXTURE_WIDTH;
    private static final int BADGE_TEXTURE_HEIGHT = PaintDimensions.BADGE_TEXTURE_HEIGHT;
    private static final int DATE_TEXTURE_WIDTH = PaintDimensions.DATE_TEXTURE_WIDTH;
    private static final int DATE_TEXTURE_HEIGHT = PaintDimensions.DATE_TEXTURE_HEIGHT;
    private static final int SEAL_TEXTURE_SIZE = PaintDimensions.SEAL_TEXTURE_SIZE;

    private static final DateTimeFormatter DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd.MM.yyyy", Locale.ROOT);
    private static final String UNKNOWN_DATE = "—";

    private static final int TEXT_COLOR = 0xFFFDF7E8;
    private static final int TEXT_PADDING = 4;
    private static final int FONT_HEIGHT = 8;
    private static final int DIGIT_INK_HEIGHT = 7;
    private static final float TEXT_HEIGHT_RATIO = 0.5f;
    private static final float MIN_TEXT_SCALE = 1.0f;

    private SignatureRenderer() {}

    public static void render(GuiGraphics ctx, Font font, String playerName,
                              @Nullable UUID playerUuid, long signedAt, PaintDimensions dims) {
        ctx.blit(ModTextures.WAX_SEAL,
                dims.sealX, dims.sealY,
                dims.sealW, dims.sealH,
                0f, 0f,
                SEAL_TEXTURE_SIZE, SEAL_TEXTURE_SIZE,
                SEAL_TEXTURE_SIZE, SEAL_TEXTURE_SIZE);

        ctx.blit(ModTextures.BADGE,
                dims.badgeX, dims.badgeY,
                dims.badgeW, dims.badgeH,
                0f, 0f,
                BADGE_TEXTURE_WIDTH, BADGE_TEXTURE_HEIGHT,
                BADGE_TEXTURE_WIDTH, BADGE_TEXTURE_HEIGHT);

        ctx.blit(ModTextures.DATE,
                dims.dateX, dims.dateY,
                dims.dateW, dims.dateH,
                0f, 0f,
                DATE_TEXTURE_WIDTH, DATE_TEXTURE_HEIGHT,
                DATE_TEXTURE_WIDTH, DATE_TEXTURE_HEIGHT);

        PlayerHeads.draw(ctx, PlayerHeads.skinOf(playerUuid),
                dims.headX, dims.headY, dims.headSize);

        drawCentred(ctx, font, playerName,
                dims.badgeX, dims.badgeY, dims.badgeW, dims.badgeH,
                BADGE_TEXTURE_WIDTH, FONT_HEIGHT);
        drawCentred(ctx, font, formatDate(signedAt),
                dims.dateX, dims.dateY, dims.dateW, dims.dateH,
                DATE_TEXTURE_WIDTH, DIGIT_INK_HEIGHT);
    }

    public static String formatDate(long signedAt) {
        if (signedAt == CanvasSignature.UNKNOWN_DATE) return UNKNOWN_DATE;
        return DATE_FORMAT.format(Instant.ofEpochMilli(signedAt).atZone(ZoneId.systemDefault()));
    }

    private static void drawCentred(GuiGraphics ctx, Font font, String text,
                                    int x, int y, int width, int height,
                                    int textureWidth, int inkHeight) {
        float plateScale = (float) width / textureWidth;
        float textScale = Math.max(MIN_TEXT_SCALE, height * TEXT_HEIGHT_RATIO / FONT_HEIGHT);
        int padding = Math.round(TEXT_PADDING * plateScale);
        int maxWidth = Math.round((width - padding * 2) / textScale);

        String fitted = TextUtil.truncate(font, text, maxWidth);
        if (fitted.isEmpty()) return;

        float textWidth = font.width(fitted) * textScale;
        float textHeight = inkHeight * textScale;

        ctx.pose().pushPose();
        ctx.pose().translate(
                x + (width - textWidth) / 2f,
                y + (height - textHeight) / 2f,
                0);
        ctx.pose().scale(textScale, textScale, 1f);
        ctx.drawString(font, fitted, 0, 0, TEXT_COLOR, false);
        ctx.pose().popPose();
    }
}
