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

    public void calculate(int screenWidth, int screenHeight) {
        float scaleX = screenWidth / BASE_W;
        float scaleY = screenHeight / BASE_H;
        uiScale = Math.min(scaleX, scaleY);

        canvasSize = s(512);
        canvasX = (screenWidth - canvasSize) / 2;
        canvasY = (screenHeight - canvasSize) / 2;

        int border = (canvasSize - Math.round(480 * uiScale)) / 2;
        drawingAreaSize = canvasSize - border * 2;
        drawingAreaX = canvasX + border;
        drawingAreaY = canvasY + border;

        toolSwitchW = s(64);
        toolSwitchH = s(128);
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
    }

    public int s(int virtualValue) {
        return Math.round(virtualValue * uiScale);
    }
}