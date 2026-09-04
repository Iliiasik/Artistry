package iliiasik.artistry.client.ui.layout;

import iliiasik.artistry.client.ClientServerSettings;
import iliiasik.artistry.client.palette.PaintSwatches;

public class PaintDimensions {

    public static final int HISTORY_CELLS = PaintSwatches.HISTORY_SIZE;
    public static final float BASE_W = 900.0f;
    public static final float BASE_H = 600.0f;

    public static final int SIGN_TEXTURE_WIDTH = 32;
    public static final int SIGN_TEXTURE_HEIGHT = 16;
    public static final int BADGE_TEXTURE_WIDTH = 80;
    public static final int BADGE_TEXTURE_HEIGHT = 16;
    public static final int SEAL_TEXTURE_SIZE = 32;
    public static final int SECONDARY_TEXTURE_SIZE = 24;
    public static final int HISTORY_TEXTURE_SIZE = 16;
    public static final int SWATCH_FRAME_BORDER = 2;

    private static final int PANEL_SCALE = 2;
    private static final int SWATCH_GAP = 2;

    private static final int PANEL_GAP = 6;

    public float uiScale;

    public int canvasSize;
    public int canvasX;
    public int canvasY;

    public int drawingAreaSize;
    public int drawingAreaX;
    public int drawingAreaY;

    public int toolSwitchX;
    public int toolSwitchY;
    public int toolSwitchW;
    public int toolSwitchH;

    public int sizeSwitchX;
    public int sizeSwitchY;
    public int sizeSwitchW;
    public int sizeSwitchH;

    public int paletteX;
    public int paletteY;
    public int paletteW;
    public int paletteH;

    public int paletteSwitcherX;
    public int paletteSwitcherY;
    public int paletteSwitcherW;
    public int paletteSwitcherH;

    public int hexInputX;
    public int hexInputY;
    public int hexInputW;
    public int hexInputH;

    public int signX;
    public int signY;
    public int signW;
    public int signH;

    public int badgeX;
    public int badgeY;
    public int badgeW;
    public int badgeH;

    public int sealX;
    public int sealY;
    public int sealW;
    public int sealH;

    public int secondaryX;
    public int secondaryY;
    public int secondarySize;

    public int historyX;
    public int historyY;
    public int historySize;
    public int historyGap;

    public int swatchStripX;
    public int swatchStripY;
    public int swatchStripW;
    public int swatchStripH;

    public void calculate(int screenWidth, int screenHeight) {
        float scaleX = screenWidth / BASE_W;
        float scaleY = screenHeight / BASE_H;
        uiScale = Math.min(scaleX, scaleY);

        int rawDrawing = Math.round(480 * uiScale);
        drawingAreaSize = (rawDrawing / 32) * 32;
        int border = Math.round(drawingAreaSize * 16.0f / 480.0f);
        canvasSize = drawingAreaSize + border * 2;
        canvasX = (screenWidth - canvasSize) / 2;
        canvasY = (screenHeight - canvasSize) / 2;
        drawingAreaX = canvasX + border;
        drawingAreaY = canvasY + border;

        int toolButtons = ClientServerSettings.imagesDisabled() ? 3 : 4;
        toolSwitchW = s(64);
        toolSwitchH = s(64) * toolButtons + Math.round(s(64) * (6.0f / 64.0f)) * (toolButtons - 1);
        toolSwitchX = canvasX + canvasSize + s(12);
        toolSwitchY = canvasY;

        sizeSwitchW = s(64);
        sizeSwitchH = s(64);
        sizeSwitchX = toolSwitchX;
        sizeSwitchY = toolSwitchY + toolSwitchH + s(PANEL_GAP);

        float rawScale = uiScale * 2.5f;
        float perfectScale = Math.max(0.5f, Math.round(rawScale * 2.0f) / 2.0f);

        paletteW = Math.round(32 * perfectScale);
        paletteH = Math.round(162 * perfectScale);
        paletteX = canvasX - s(12) - paletteW;
        paletteY = canvasY;

        paletteSwitcherW = Math.round(32 * perfectScale);
        paletteSwitcherH = Math.round(32 * perfectScale);
        paletteSwitcherX = paletteX;
        paletteSwitcherY = paletteY + paletteH + s(PANEL_GAP);

        secondarySize = Math.round(SECONDARY_TEXTURE_SIZE * perfectScale);
        historySize = Math.round(HISTORY_TEXTURE_SIZE * perfectScale);
        historyGap = Math.round(SWATCH_GAP * perfectScale);

        secondaryX = paletteX - historyGap - secondarySize;
        secondaryY = paletteY + paletteW / 2 - secondarySize / 2;
        historyX = secondaryX + (secondarySize - historySize) / 2;
        historyY = secondaryY + secondarySize + historyGap;

        swatchStripX = Math.min(secondaryX, historyX);
        swatchStripY = secondaryY;
        swatchStripW = Math.max(secondarySize, historySize);
        swatchStripH = historyY + HISTORY_CELLS * (historySize + historyGap) - historyGap - secondaryY;

        hexInputX = paletteX + Math.round(2 * perfectScale);
        hexInputY = paletteY + Math.round((2 + 28 + 2) * perfectScale);
        hexInputW = Math.round(28 * perfectScale);
        hexInputH = Math.round(7 * perfectScale);

        signW = s(SIGN_TEXTURE_WIDTH * PANEL_SCALE);
        signH = s(SIGN_TEXTURE_HEIGHT * PANEL_SCALE);
        signX = sizeSwitchX;
        signY = sizeSwitchY + sizeSwitchH + s(PANEL_GAP);

        badgeW = s(BADGE_TEXTURE_WIDTH * PANEL_SCALE);
        badgeH = s(BADGE_TEXTURE_HEIGHT * PANEL_SCALE);
        sealW = s(SEAL_TEXTURE_SIZE);
        sealH = s(SEAL_TEXTURE_SIZE);

        int signatureGap = s(PANEL_GAP);
        int signatureWidth = badgeW + signatureGap + sealW;
        int signatureHeight = Math.max(badgeH, sealH);
        int signatureTop = canvasY - signatureHeight - signatureGap;
        badgeX = canvasX + (canvasSize - signatureWidth) / 2;
        badgeY = signatureTop + (signatureHeight - badgeH) / 2;
        sealX = badgeX + badgeW + signatureGap;
        sealY = signatureTop + (signatureHeight - sealH) / 2;
    }

    public int s(int virtualValue) {
        return Math.round(virtualValue * uiScale);
    }
}