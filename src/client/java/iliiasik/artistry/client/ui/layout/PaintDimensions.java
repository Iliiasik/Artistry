package iliiasik.artistry.client.ui.layout;

public class PaintDimensions {
    public static final float BASE_W = 900.0f;
    public static final float BASE_H = 600.0f;

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

        toolSwitchW = s(64);
        toolSwitchH = s(64) * 4 + Math.round(s(64) * (6.0f / 64.0f)) * 3;
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

        hexInputX = paletteX + Math.round(2 * perfectScale);
        hexInputY = paletteY + Math.round((2 + 28 + 2) * perfectScale);
        hexInputW = Math.round(28 * perfectScale);
        hexInputH = Math.round(7 * perfectScale);
    }

    public int s(int virtualValue) {
        return Math.round(virtualValue * uiScale);
    }
}