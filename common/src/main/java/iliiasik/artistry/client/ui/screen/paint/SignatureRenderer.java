package iliiasik.artistry.client.ui.screen.paint;

import iliiasik.artistry.client.ui.layout.PaintDimensions;
import iliiasik.artistry.client.util.ModTextures;
import iliiasik.artistry.client.util.TextUtil;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;

public final class SignatureRenderer {

    private static final int BADGE_TEXTURE_WIDTH = PaintDimensions.BADGE_TEXTURE_WIDTH;
    private static final int BADGE_TEXTURE_HEIGHT = PaintDimensions.BADGE_TEXTURE_HEIGHT;
    private static final int SEAL_TEXTURE_SIZE = PaintDimensions.SEAL_TEXTURE_SIZE;

    private static final int TEXT_COLOR = 0xFFFDF7E8;
    private static final int TEXT_PADDING = 4;
    private static final int FONT_HEIGHT = 8;
    private static final float TEXT_HEIGHT_RATIO = 0.5f;
    private static final float MIN_TEXT_SCALE = 1.0f;

    private SignatureRenderer() {}

    public static void render(GuiGraphics ctx, Font font, String playerName, PaintDimensions dims) {
        ctx.blit(ModTextures.BADGE,
                dims.badgeX, dims.badgeY,
                dims.badgeW, dims.badgeH,
                0f, 0f,
                BADGE_TEXTURE_WIDTH, BADGE_TEXTURE_HEIGHT,
                BADGE_TEXTURE_WIDTH, BADGE_TEXTURE_HEIGHT);

        ctx.blit(ModTextures.WAX_SEAL,
                dims.sealX, dims.sealY,
                dims.sealW, dims.sealH,
                0f, 0f,
                SEAL_TEXTURE_SIZE, SEAL_TEXTURE_SIZE,
                SEAL_TEXTURE_SIZE, SEAL_TEXTURE_SIZE);

        float badgeScale = (float) dims.badgeW / BADGE_TEXTURE_WIDTH;
        float textScale = Math.max(MIN_TEXT_SCALE, dims.badgeH * TEXT_HEIGHT_RATIO / FONT_HEIGHT);
        int padding = Math.round(TEXT_PADDING * badgeScale);
        int maxWidth = Math.round((dims.badgeW - padding * 2) / textScale);
        String name = TextUtil.truncate(font, playerName, maxWidth);
        if (name.isEmpty()) return;

        float textWidth = font.width(name) * textScale;
        float textHeight = FONT_HEIGHT * textScale;

        ctx.pose().pushPose();
        ctx.pose().translate(
                dims.badgeX + (dims.badgeW - textWidth) / 2f,
                dims.badgeY + (dims.badgeH - textHeight) / 2f,
                0);
        ctx.pose().scale(textScale, textScale, 1f);
        ctx.drawString(font, name, 0, 0, TEXT_COLOR, false);
        ctx.pose().popPose();
    }
}
