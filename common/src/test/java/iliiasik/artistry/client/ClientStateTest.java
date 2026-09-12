package iliiasik.artistry.client;

import iliiasik.artistry.client.ui.widget.HoverFadeHelper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClientStateTest {

    @AfterEach
    void resetSettings() {
        ClientServerSettings.reset();
    }

    @Test
    @DisplayName("Client settings start at the documented defaults")
    void defaults() {
        ClientServerSettings.reset();
        assertFalse(ClientServerSettings.imagesDisabled());
        assertEquals(50, ClientServerSettings.batchIntervalMs());
        assertEquals(100, ClientServerSettings.cursorIntervalMs());
    }

    @Test
    @DisplayName("Server settings replace the local defaults")
    void serverSettingsAreApplied() {
        ClientServerSettings.apply(true, 250, 750);

        assertTrue(ClientServerSettings.imagesDisabled());
        assertEquals(250, ClientServerSettings.batchIntervalMs());
        assertEquals(750, ClientServerSettings.cursorIntervalMs());
    }

    @Test
    @DisplayName("Leaving a server restores the defaults")
    void resetRestoresDefaults() {
        ClientServerSettings.apply(true, 999, 999);
        ClientServerSettings.reset();

        assertFalse(ClientServerSettings.imagesDisabled());
        assertEquals(50, ClientServerSettings.batchIntervalMs());
        assertEquals(100, ClientServerSettings.cursorIntervalMs());
    }

    @Test
    @DisplayName("An idle widget renders at the resting brightness")
    void idleWidgetIsDim() {
        HoverFadeHelper helper = new HoverFadeHelper();
        int color = helper.computeColor();

        assertEquals(0xFF, (color >>> 24) & 0xFF);
        assertEquals(200, (color >> 16) & 0xFF);
        assertEquals(200, (color >> 8) & 0xFF);
        assertEquals(200, color & 0xFF);
    }

    @Test
    @DisplayName("Hovering brightens the widget without ever leaving the valid range")
    void hoverBrightensWidget() throws InterruptedException {
        HoverFadeHelper helper = new HoverFadeHelper();
        int previous = helper.computeColor() & 0xFF;

        for (int i = 0; i < 20; i++) {
            Thread.sleep(10);
            helper.update(true);
            int current = helper.computeColor() & 0xFF;
            assertTrue(current >= previous, "brightness must not drop while hovered");
            assertTrue(current >= 200 && current <= 255, "brightness out of range: " + current);
            previous = current;
        }
        assertTrue(previous > 200, "hovering long enough must brighten the widget");
    }

    @Test
    @DisplayName("Losing hover darkens the widget back towards the resting brightness")
    void losingHoverDarkensWidget() throws InterruptedException {
        HoverFadeHelper helper = new HoverFadeHelper();
        for (int i = 0; i < 20; i++) {
            Thread.sleep(10);
            helper.update(true);
        }
        int hovered = helper.computeColor() & 0xFF;

        for (int i = 0; i < 20; i++) {
            Thread.sleep(10);
            helper.update(false);
        }
        int released = helper.computeColor() & 0xFF;

        assertTrue(released < hovered, "the widget must fade back out");
        assertTrue(released >= 200, "brightness must not fall below the resting value");
    }

    @Test
    @DisplayName("The widget colour is always fully opaque grey")
    void widgetColourIsOpaqueGrey() throws InterruptedException {
        HoverFadeHelper helper = new HoverFadeHelper();
        for (int i = 0; i < 5; i++) {
            Thread.sleep(5);
            helper.update(i % 2 == 0);
            int color = helper.computeColor();
            int r = (color >> 16) & 0xFF;
            int g = (color >> 8) & 0xFF;
            int b = color & 0xFF;
            assertEquals(0xFF, (color >>> 24) & 0xFF);
            assertEquals(r, g);
            assertEquals(g, b);
        }
    }
}
