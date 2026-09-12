package iliiasik.artistry.client.palette;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ColorPaletteTest {

    @Test
    @DisplayName("The primary hues map to the primary colours")
    void primaryHues() {
        assertEquals(0xFFFF0000, ColorPalette.hsvToArgb(0f, 1f, 1f));
        assertEquals(0xFF00FF00, ColorPalette.hsvToArgb(1f / 3f, 1f, 1f));
        assertEquals(0xFF0000FF, ColorPalette.hsvToArgb(2f / 3f, 1f, 1f));
    }

    @Test
    @DisplayName("Zero saturation produces grey and zero value produces black")
    void greyAndBlack() {
        assertEquals(0xFFFFFFFF, ColorPalette.hsvToArgb(0.5f, 0f, 1f));
        assertEquals(0xFF808080, ColorPalette.hsvToArgb(0.5f, 0f, 128f / 255f));
        assertEquals(0xFF000000, ColorPalette.hsvToArgb(0.5f, 1f, 0f));
    }

    @Test
    @DisplayName("The alpha channel is always opaque")
    void alphaIsOpaque() {
        for (int i = 0; i <= 10; i++) {
            int argb = ColorPalette.hsvToArgb(i / 10f, 1f, 1f);
            assertEquals(0xFF, (argb >>> 24) & 0xFF);
        }
    }

    @Test
    @DisplayName("Converting to hsv and back keeps the colour")
    void hsvRoundTrip() {
        int[] colors = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFFFFFFFF, 0xFF000000, 0xFF336699};
        for (int color : colors) {
            float[] hsv = ColorPalette.argbToHsv(color);
            int restored = ColorPalette.hsvToArgb(hsv[0], hsv[1], hsv[2]);
            assertEquals(color, restored, () -> String.format("%08X", color));
        }
    }

    @Test
    @DisplayName("Hue stays inside the unit range")
    void hueIsNormalised() {
        int[] colors = {0xFFFF0000, 0xFF00FF00, 0xFF0000FF, 0xFF00FFFF, 0xFFFF00FF, 0xFFFFFF00};
        for (int color : colors) {
            float hue = ColorPalette.argbToHsv(color)[0];
            assertTrue(hue >= 0f && hue <= 1f, "hue out of range: " + hue);
        }
    }

    @Test
    @DisplayName("Hex formatting drops the alpha channel")
    void hexFormatting() {
        assertEquals("#3366CC", ColorPalette.argbToHex(0xFF3366CC));
        assertEquals("#000000", ColorPalette.argbToHex(0xFF000000));
        assertEquals("#FFFFFF", ColorPalette.argbToHex(0x00FFFFFF));
    }

    @Test
    @DisplayName("Hex parsing accepts both notations and forces full alpha")
    void hexParsing() {
        assertEquals(0xFF3366CC, ColorPalette.hexToArgb("#3366CC"));
        assertEquals(0xFF3366CC, ColorPalette.hexToArgb("3366CC"));
        assertEquals(0xFFABCDEF, ColorPalette.hexToArgb("abcdef"));
    }

    @Test
    @DisplayName("Hex and colour conversions are inverse to each other")
    void hexRoundTrip() {
        int color = 0xFF12AB34;
        assertEquals(color, ColorPalette.hexToArgb(ColorPalette.argbToHex(color)));
    }

    @Test
    @DisplayName("Malformed hex input is rejected")
    void malformedHexIsRejected() {
        assertThrows(NumberFormatException.class, () -> ColorPalette.hexToArgb("12345"));
        assertThrows(NumberFormatException.class, () -> ColorPalette.hexToArgb("1234567"));
        assertThrows(NumberFormatException.class, () -> ColorPalette.hexToArgb(""));
        assertThrows(NumberFormatException.class, () -> ColorPalette.hexToArgb("#"));
        assertThrows(NumberFormatException.class, () -> ColorPalette.hexToArgb("ZZZZZZ"));
    }

    @Test
    @DisplayName("The native image conversion swaps red and blue")
    void argbToAbgrSwapsChannels() {
        assertEquals(0xAADDCCBB, ColorPalette.argbToAbgr(0xAABBCCDD));
        assertEquals(0xFF0000FF, ColorPalette.argbToAbgr(0xFFFF0000));
        assertEquals(0xFFFF0000, ColorPalette.argbToAbgr(0xFF0000FF));
        assertEquals(0xFF00FF00, ColorPalette.argbToAbgr(0xFF00FF00));
    }

    @Test
    @DisplayName("The native image conversion is its own inverse")
    void argbToAbgrIsInvolution() {
        int color = 0x12345678;
        assertEquals(color, ColorPalette.argbToAbgr(ColorPalette.argbToAbgr(color)));
    }
}
