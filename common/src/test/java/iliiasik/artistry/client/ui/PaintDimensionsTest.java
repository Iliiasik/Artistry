package iliiasik.artistry.client.ui;

import iliiasik.artistry.client.ClientServerSettings;
import iliiasik.artistry.client.ui.layout.PaintDimensions;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaintDimensionsTest {

    private PaintDimensions dimensions;

    @BeforeEach
    void setUp() {
        ClientServerSettings.reset();
        dimensions = new PaintDimensions();
    }

    @AfterEach
    void tearDown() {
        ClientServerSettings.reset();
    }

    @Test
    @DisplayName("The ui scale follows the smaller screen ratio")
    void uiScaleUsesSmallerRatio() {
        dimensions.calculate(1800, 600);
        assertEquals(1.0f, dimensions.uiScale, 0.0001f);

        dimensions.calculate(900, 1200);
        assertEquals(1.0f, dimensions.uiScale, 0.0001f);

        dimensions.calculate(1800, 1200);
        assertEquals(2.0f, dimensions.uiScale, 0.0001f);
    }

    @Test
    @DisplayName("The drawing area is always a multiple of the maximum grid")
    void drawingAreaIsGridAligned() {
        int[][] screens = {{854, 480}, {900, 600}, {1280, 720}, {1920, 1080}, {2560, 1440}, {3840, 2160}};
        for (int[] screen : screens) {
            dimensions.calculate(screen[0], screen[1]);
            assertEquals(0, dimensions.drawingAreaSize % 32,
                    "drawing area " + dimensions.drawingAreaSize + " at " + screen[0] + "x" + screen[1]);
            assertTrue(dimensions.drawingAreaSize > 0);
        }
    }

    @Test
    @DisplayName("The canvas is centred on the screen")
    void canvasIsCentred() {
        dimensions.calculate(1920, 1080);

        assertEquals((1920 - dimensions.canvasSize) / 2, dimensions.canvasX);
        assertEquals((1080 - dimensions.canvasSize) / 2, dimensions.canvasY);
    }

    @Test
    @DisplayName("The drawing area sits inside the canvas frame")
    void drawingAreaSitsInsideCanvas() {
        dimensions.calculate(1920, 1080);

        assertTrue(dimensions.drawingAreaX >= dimensions.canvasX);
        assertTrue(dimensions.drawingAreaY >= dimensions.canvasY);
        assertTrue(dimensions.drawingAreaX + dimensions.drawingAreaSize
                <= dimensions.canvasX + dimensions.canvasSize);
        assertTrue(dimensions.drawingAreaY + dimensions.drawingAreaSize
                <= dimensions.canvasY + dimensions.canvasSize);
    }

    @Test
    @DisplayName("The tool panel sits to the right and the palette to the left")
    void panelsAreOnOppositeSides() {
        dimensions.calculate(1920, 1080);

        assertTrue(dimensions.toolSwitchX >= dimensions.canvasX + dimensions.canvasSize,
                "the tool panel must not overlap the canvas");
        assertTrue(dimensions.paletteX + dimensions.paletteW <= dimensions.canvasX,
                "the palette must not overlap the canvas");
    }

    @Test
    @DisplayName("The size switch sits below the tool panel without overlapping it")
    void sizeSwitchIsBelowToolPanel() {
        dimensions.calculate(1920, 1080);

        assertEquals(dimensions.toolSwitchX, dimensions.sizeSwitchX);
        assertTrue(dimensions.sizeSwitchY >= dimensions.toolSwitchY + dimensions.toolSwitchH);
    }

    @Test
    @DisplayName("The palette switcher sits below the palette without overlapping it")
    void paletteSwitcherIsBelowPalette() {
        dimensions.calculate(1920, 1080);

        assertEquals(dimensions.paletteX, dimensions.paletteSwitcherX);
        assertTrue(dimensions.paletteSwitcherY >= dimensions.paletteY + dimensions.paletteH);
    }

    @Test
    @DisplayName("The hex field stays inside the palette")
    void hexFieldStaysInsidePalette() {
        dimensions.calculate(1920, 1080);

        assertTrue(dimensions.hexInputX >= dimensions.paletteX);
        assertTrue(dimensions.hexInputX + dimensions.hexInputW <= dimensions.paletteX + dimensions.paletteW);
        assertTrue(dimensions.hexInputY >= dimensions.paletteY);
        assertTrue(dimensions.hexInputY + dimensions.hexInputH <= dimensions.paletteY + dimensions.paletteH);
    }

    @Test
    @DisplayName("The sign button sits under the size switch and shares its width")
    void signButtonFollowsThePanel() {
        dimensions.calculate(1600, 900);
        assertEquals(dimensions.sizeSwitchX, dimensions.signX);
        assertEquals(dimensions.sizeSwitchW, dimensions.signW);
        assertTrue(dimensions.signY >= dimensions.sizeSwitchY + dimensions.sizeSwitchH,
                "the sign button overlaps the size switch");
    }

    @Test
    @DisplayName("The swatch strip sits left of the palette without overlapping it")
    void swatchStripSitsLeftOfPalette() {
        dimensions.calculate(1600, 900);
        assertTrue(dimensions.secondaryX + dimensions.secondarySize <= dimensions.paletteX,
                "the secondary swatch overlaps the palette");
        assertTrue(dimensions.historyX + dimensions.historySize <= dimensions.paletteX,
                "the history overlaps the palette");
        assertTrue(dimensions.historyY >= dimensions.secondaryY + dimensions.secondarySize,
                "the history overlaps the secondary swatch");
        assertTrue(dimensions.swatchStripX <= dimensions.secondaryX
                        && dimensions.swatchStripX <= dimensions.historyX,
                "the strip bounds miss a cell on the left");
        assertTrue(dimensions.swatchStripY + dimensions.swatchStripH
                        >= dimensions.historyY + PaintDimensions.HISTORY_CELLS * dimensions.historySize,
                "the strip bounds are shorter than the cells");
    }

    @Test
    @DisplayName("The signature sits above the frame, badge and seal centred together")
    void signatureSitsAboveTheFrame() {
        dimensions.calculate(1600, 900);
        assertTrue(dimensions.badgeY + dimensions.badgeH <= dimensions.canvasY,
                "the badge overlaps the frame");
        assertTrue(dimensions.sealY + dimensions.sealH <= dimensions.canvasY,
                "the seal overlaps the frame");
        assertTrue(dimensions.sealX >= dimensions.badgeX + dimensions.badgeW,
                "the seal overlaps the badge");
        assertEquals(dimensions.badgeY + dimensions.badgeH / 2,
                dimensions.sealY + dimensions.sealH / 2,
                "the badge and the seal are not on one line");

        int groupLeft = dimensions.badgeX;
        int groupRight = dimensions.sealX + dimensions.sealW;
        int canvasCentre = dimensions.canvasX + dimensions.canvasSize / 2;
        assertTrue(Math.abs((groupLeft + groupRight) / 2 - canvasCentre) <= 1,
                "the signature group is not centred on the frame");
    }

    @Test
    @DisplayName("Disabling images removes one tool button from the panel")
    void disablingImagesShrinksToolPanel() {
        dimensions.calculate(1920, 1080);
        int withImages = dimensions.toolSwitchH;

        ClientServerSettings.apply(true, 50, 100);
        dimensions.calculate(1920, 1080);
        int withoutImages = dimensions.toolSwitchH;

        assertTrue(withoutImages < withImages,
                "three buttons must be shorter than four");
    }

    @Test
    @DisplayName("A larger screen produces a larger layout")
    void largerScreenProducesLargerLayout() {
        dimensions.calculate(900, 600);
        int small = dimensions.canvasSize;

        dimensions.calculate(1800, 1200);
        assertTrue(dimensions.canvasSize > small);
    }

    @Test
    @DisplayName("The scale helper rounds virtual units")
    void scaleHelperRounds() {
        dimensions.calculate(1800, 1200);
        assertEquals(2.0f, dimensions.uiScale, 0.0001f);
        assertEquals(128, dimensions.s(64));
        assertEquals(0, dimensions.s(0));
    }

    @Test
    @DisplayName("A tiny screen still produces a usable layout")
    void tinyScreenIsStillUsable() {
        dimensions.calculate(320, 240);

        assertTrue(dimensions.uiScale > 0f);
        assertTrue(dimensions.drawingAreaSize >= 0);
        assertEquals(0, dimensions.drawingAreaSize % 32);
    }
}
