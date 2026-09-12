package iliiasik.artistry.client.palette;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

class PaintSwatchesTest {

    private static List<PaintSwatch> snapshot(PaintSwatches swatches) {
        List<PaintSwatch> state = new ArrayList<>();
        state.add(swatches.primary());
        state.add(swatches.secondary());
        for (int i = 0; i < PaintSwatches.HISTORY_SIZE; i++) {
            state.add(swatches.history(i));
        }
        return state;
    }

    private static void assertInvariants(PaintSwatches swatches, String step) {
        assertNotEquals(swatches.primary(), swatches.secondary(), step + ": the two slots became equal");
        for (int i = 0; i < PaintSwatches.HISTORY_SIZE; i++) {
            assertNotEquals(swatches.primary(), swatches.history(i), step + ": primary leaked into the history");
            assertNotEquals(swatches.secondary(), swatches.history(i), step + ": secondary leaked into the history");
            for (int j = i + 1; j < PaintSwatches.HISTORY_SIZE; j++) {
                assertNotEquals(swatches.history(i), swatches.history(j), step + ": duplicate history entries");
            }
        }
    }

    private static void pickHistory(PaintSwatches swatches, int index, boolean secondary) {
        swatches.select(swatches.history(index), secondary);
    }

    @Test
    @DisplayName("The history starts with three distinct colors")
    void defaultHistory() {
        PaintSwatches swatches = new PaintSwatches();
        for (int i = 0; i < PaintSwatches.HISTORY_SIZE; i++) {
            for (int j = i + 1; j < PaintSwatches.HISTORY_SIZE; j++) {
                assertNotEquals(swatches.history(i), swatches.history(j));
            }
        }
    }

    @Test
    @DisplayName("A fresh pick pushes the replaced swatch to the front of the history")
    void pickPushesPrevious() {
        PaintSwatches swatches = new PaintSwatches();
        PaintSwatch previousPrimary = swatches.primary();
        PaintSwatch dropped = swatches.history(PaintSwatches.HISTORY_SIZE - 1);
        PaintSwatch picked = PaintSwatch.ofColor(0xFF123456);

        swatches.select(picked, false);

        assertEquals(picked, swatches.primary());
        assertEquals(previousPrimary, swatches.history(0));
        for (int i = 0; i < PaintSwatches.HISTORY_SIZE; i++) {
            assertNotEquals(dropped, swatches.history(i));
        }
    }

    @Test
    @DisplayName("Picking a swatch from the history swaps it with the slot")
    void pickFromHistorySwaps() {
        PaintSwatches swatches = new PaintSwatches();
        PaintSwatch previousPrimary = swatches.primary();
        PaintSwatch fromHistory = swatches.history(1);

        swatches.select(fromHistory, false);

        assertEquals(fromHistory, swatches.primary());
        assertEquals(previousPrimary, swatches.history(1));
    }

    @Test
    @DisplayName("The secondary slot swaps independently of the primary")
    void secondarySlotIsIndependent() {
        PaintSwatches swatches = new PaintSwatches();
        PaintSwatch primaryBefore = swatches.primary();
        PaintSwatch previousSecondary = swatches.secondary();
        PaintSwatch fromHistory = swatches.history(0);

        swatches.select(fromHistory, true);

        assertEquals(primaryBefore, swatches.primary());
        assertEquals(fromHistory, swatches.secondary());
        assertEquals(previousSecondary, swatches.history(0));
    }

    @Test
    @DisplayName("Swapping the slots leaves the history alone")
    void swapSlots() {
        PaintSwatches swatches = new PaintSwatches();
        PaintSwatch primaryBefore = swatches.primary();
        PaintSwatch secondaryBefore = swatches.secondary();
        PaintSwatch historyBefore = swatches.history(0);

        swatches.swapSlots();

        assertEquals(secondaryBefore, swatches.primary());
        assertEquals(primaryBefore, swatches.secondary());
        assertEquals(historyBefore, swatches.history(0));
    }

    @Test
    @DisplayName("Re-picking the current swatch changes nothing")
    void repickIsIgnored() {
        PaintSwatches swatches = new PaintSwatches();
        PaintSwatch first = swatches.history(0);

        swatches.select(swatches.primary(), false);

        assertEquals(first, swatches.history(0));
    }

    @Test
    @DisplayName("Picking the other slot's swatch swaps the slots instead of duplicating it")
    void pickingTheOtherSlotSwaps() {
        PaintSwatches swatches = new PaintSwatches();
        PaintSwatch primaryBefore = swatches.primary();
        PaintSwatch secondaryBefore = swatches.secondary();
        PaintSwatch historyBefore = swatches.history(0);

        swatches.select(secondaryBefore, false);

        assertEquals(secondaryBefore, swatches.primary());
        assertEquals(primaryBefore, swatches.secondary());
        assertEquals(historyBefore, swatches.history(0));
        assertInvariants(swatches, "swap by selection");
    }

    @Test
    @DisplayName("Three fresh picks push the whole default history out in order")
    void freshPicksBehaveLikeAStack() {
        PaintSwatches swatches = new PaintSwatches();
        PaintSwatch start = swatches.primary();
        PaintSwatch first = PaintSwatch.ofColor(0xFF111111);
        PaintSwatch second = PaintSwatch.ofColor(0xFF222222);
        PaintSwatch third = PaintSwatch.ofColor(0xFF333333);

        swatches.select(first, false);
        swatches.select(second, false);
        swatches.select(third, false);

        assertEquals(third, swatches.primary());
        assertEquals(second, swatches.history(0));
        assertEquals(first, swatches.history(1));
        assertEquals(start, swatches.history(2));
    }

    @Test
    @DisplayName("Picking the same history cell twice restores the previous state")
    void historyPickIsAnInvolution() {
        for (int index = 0; index < PaintSwatches.HISTORY_SIZE; index++) {
            for (boolean secondary : new boolean[]{false, true}) {
                PaintSwatches swatches = new PaintSwatches();
                swatches.select(PaintSwatch.ofColor(0xFF445566), false);
                swatches.select(PaintSwatch.ofBlock(9), true);
                List<PaintSwatch> before = snapshot(swatches);

                pickHistory(swatches, index, secondary);
                pickHistory(swatches, index, secondary);

                assertEquals(before, snapshot(swatches), "index " + index + " secondary " + secondary);
            }
        }
    }

    @Test
    @DisplayName("Two laps of a closed loop bring the state back every time")
    void twoLapsReturnToStart() {
        PaintSwatches swatches = new PaintSwatches();
        swatches.select(PaintSwatch.ofColor(0xFF102030), false);
        swatches.select(PaintSwatch.ofBlock(12), true);
        List<PaintSwatch> start = snapshot(swatches);

        for (int lap = 1; lap <= 2; lap++) {
            pickHistory(swatches, 0, false);
            pickHistory(swatches, 1, true);
            pickHistory(swatches, 1, true);
            pickHistory(swatches, 0, false);
            assertInvariants(swatches, "lap " + lap);
            assertEquals(start, snapshot(swatches), "lap " + lap);
        }

        swatches.swapSlots();
        swatches.swapSlots();
        assertEquals(start, snapshot(swatches));
    }

    @Test
    @DisplayName("A long mixed run never breaks the rules of the strip")
    void longRunKeepsInvariants() {
        PaintSwatches swatches = new PaintSwatches();
        Random random = new Random(20260901L);

        for (int step = 0; step < 500; step++) {
            switch (random.nextInt(5)) {
                case 0 -> swatches.select(PaintSwatch.ofColor(0xFF000000 | random.nextInt(0x1000)), false);
                case 1 -> swatches.select(PaintSwatch.ofColor(0xFF000000 | random.nextInt(0x1000)), true);
                case 2 -> swatches.select(PaintSwatch.ofBlock(1 + random.nextInt(24)), random.nextBoolean());
                case 3 -> pickHistory(swatches, random.nextInt(PaintSwatches.HISTORY_SIZE), random.nextBoolean());
                default -> swatches.swapSlots();
            }
            assertInvariants(swatches, "step " + step);
        }
    }

    @Test
    @DisplayName("The history never holds the swatch of either slot")
    void historyExcludesSlots() {
        PaintSwatches swatches = new PaintSwatches();
        swatches.select(PaintSwatch.ofColor(0xFF111111), false);
        swatches.select(PaintSwatch.ofColor(0xFF222222), true);
        swatches.select(swatches.history(2), false);
        swatches.select(PaintSwatch.ofBlock(17), true);

        for (int i = 0; i < PaintSwatches.HISTORY_SIZE; i++) {
            assertNotEquals(swatches.primary(), swatches.history(i));
            assertNotEquals(swatches.secondary(), swatches.history(i));
        }
    }
}
