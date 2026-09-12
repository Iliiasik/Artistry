package iliiasik.artistry.client.util;

import net.minecraft.client.gui.Font;

public final class TextUtil {

    private static final String ELLIPSIS = "...";

    private TextUtil() {}

    public static String truncate(Font font, String text, int maxWidth) {
        if (maxWidth <= 0) return "";
        if (font.width(text) <= maxWidth) return text;

        int ellipsisWidth = font.width(ELLIPSIS);
        if (ellipsisWidth > maxWidth) return "";

        String head = font.plainSubstrByWidth(text, maxWidth - ellipsisWidth);
        return head + ELLIPSIS;
    }
}
