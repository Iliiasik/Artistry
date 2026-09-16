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
    @DisplayName("The signature column stacks seal, badge and date to the right of the frame")
    void signatureColumnStacksToTheRight() {
        int[][] screens = {{854, 480}, {1280, 720}, {1600, 900}, {1920, 1080}, {3840, 2160}};

        for (int[] screen : screens) {
            dimensions.calculate(screen[0], screen[1], true);
            String at = " at " + screen[0] + "x" + screen[1];

            assertTrue(dimensions.signatureX >= dimensions.canvasX + dimensions.canvasSize,
                    "the column overlaps the frame" + at);

            assertTrue(dimensions.headY >= dimensions.sealY + dimensions.sealH,
                    "the head overlaps the seal" + at);
            assertTrue(dimensions.badgeY >= dimensions.headY + dimensions.headSize,
                    "the badge overlaps the head" + at);
            assertTrue(dimensions.dateY >= dimensions.badgeY + dimensions.badgeH,
                    "the date overlaps the badge" + at);
            assertEquals(centreOf(dimensions.dateX, dimensions.dateW),
                    centreOf(dimensions.headX, dimensions.headSize),
                    "the head is not on the column axis" + at);

            assertEquals(centreOf(dimensions.sealX, dimensions.sealW),
                    centreOf(dimensions.badgeX, dimensions.badgeW),
                    "the seal and the badge are not on one axis" + at);
            assertEquals(centreOf(dimensions.badgeX, dimensions.badgeW),
                    centreOf(dimensions.dateX, dimensions.dateW),
                    "the badge and the date are not on one axis" + at);

            assertTrue(dimensions.signatureX + dimensions.signatureW <= screen[0],
                    "the column ran off the right edge" + at);
            assertTrue(dimensions.signatureY >= 0
                            && dimensions.signatureY + dimensions.signatureH <= screen[1],
                    "the column ran off the top or bottom" + at);
        }
    }

    @Test
    @DisplayName("The frame and the signature column are centred together")
    void signedLayoutCentresTheWholeGroup() {
        int[][] screens = {{1280, 720}, {1920, 1080}, {3840, 2160}};

        for (int[] screen : screens) {
            dimensions.calculate(screen[0], screen[1], true);
            String at = " at " + screen[0] + "x" + screen[1];

            int groupLeft = dimensions.canvasX;
            int groupRight = dimensions.signatureX + dimensions.signatureW;
            assertTrue(Math.abs((groupLeft + groupRight) / 2 - screen[0] / 2) <= 1,
                    "the frame and the column are not centred as one group" + at);

            dimensions.calculate(screen[0], screen[1], false);
            assertEquals((screen[0] - dimensions.canvasSize) / 2, dimensions.canvasX,
                    "an unsigned canvas must stay centred on its own" + at);
        }
    }

    private static int centreOf(int start, int size) {
        return start + size / 2;
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
    @DisplayName("The whole screen is driven by one snapped scale")
    void everyWidgetSharesThePaletteScale() {
        int[][] screens = {{854, 480}, {900, 600}, {1280, 720}, {1366, 768}, {1440, 900},
                {1600, 900}, {1680, 1050}, {1920, 1080}, {2560, 1440}, {3440, 1440}, {3840, 2160}};

        for (int[] screen : screens) {
            dimensions.calculate(screen[0], screen[1]);
            String at = " at " + screen[0] + "x" + screen[1];

            assertEquals(dimensions.paletteSwitcherW, dimensions.panelButton,
                    "the tool button must match the palette switcher" + at);
            assertEquals(dimensions.panelButton, dimensions.toolSwitchW, "tool switch" + at);
            assertEquals(dimensions.panelButton, dimensions.sizeSwitchW, "size switch" + at);
            assertEquals(dimensions.panelButton, dimensions.sizeSwitchH, "size switch is square" + at);
            assertEquals(dimensions.panelButton, dimensions.signW, "sign width" + at);
            assertEquals(dimensions.signW, dimensions.signH * 2, "the sign is 32x16" + at);
            assertEquals(dimensions.panelButton, dimensions.sealW, "the seal matches a button" + at);
            assertEquals(dimensions.sealW, dimensions.sealH, "the seal is square" + at);
            assertEquals(dimensions.panelButton / 2, dimensions.badgeH, "badge height" + at);
            assertEquals(dimensions.badgeH, dimensions.dateH, "the date plate matches the badge" + at);
            assertEquals(dimensions.badgeW, dimensions.dateW, "the date plate matches the badge" + at);
            assertEquals(dimensions.badgeW, dimensions.headSize, "the head matches the badge width" + at);
            assertEquals(0, dimensions.headSize % PaintDimensions.HEAD_FACE_TEXELS,
                    "the head must scale by whole texels" + at);
        }
    }

    @Test
    @DisplayName("The tool panel height matches the buttons the widget actually stacks")
    void toolPanelHeightMatchesTheStackedButtons() {
        int[][] screens = {{854, 480}, {900, 600}, {1280, 720}, {1600, 900},
                {1920, 1080}, {2560, 1440}, {3840, 2160}};

        for (int[] screen : screens) {
            dimensions.calculate(screen[0], screen[1]);
            String at = " at " + screen[0] + "x" + screen[1];

            int button = dimensions.panelButton;
            int gap = PaintDimensions.buttonGap(button);
            int lastButtonTop = button * 3 + gap * 3;

            assertEquals(button * 4 + gap * 3, dimensions.toolSwitchH,
                    "the layout and the widget disagree on the stack height" + at);
            assertEquals(dimensions.toolSwitchH, lastButtonTop + button,
                    "the fourth button does not end where the panel ends" + at);
        }
    }

    @Test
    @DisplayName("The right column fits above and below on every common screen")
    void panelFitsVertically() {
        int[][] screens = {{854, 480}, {1280, 720}, {1600, 900}, {1920, 1080}, {2560, 1440}, {3840, 2160}};

        for (int[] screen : screens) {
            dimensions.calculate(screen[0], screen[1]);
            String at = " at " + screen[0] + "x" + screen[1];

            assertTrue(dimensions.signY + dimensions.signH <= screen[1],
                    "the sign button ran off the bottom" + at);
            assertTrue(dimensions.toolSwitchY >= 0,
                    "the tool panel ran off the top" + at);
        }
    }

    @Test
    @DisplayName("The panel still fits beside the canvas on every common screen")
    void panelFitsBesideTheCanvas() {
        int[][] screens = {{854, 480}, {1280, 720}, {1600, 900}, {1920, 1080}, {2560, 1440}, {3840, 2160}};

        for (int[] screen : screens) {
            dimensions.calculate(screen[0], screen[1]);
            assertTrue(dimensions.toolSwitchX + dimensions.toolSwitchW <= screen[0],
                    "the tool panel ran off the screen at " + screen[0] + "x" + screen[1]);
            assertTrue(dimensions.swatchStripX >= 0,
                    "the palette ran off the screen at " + screen[0] + "x" + screen[1]);
        }
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
