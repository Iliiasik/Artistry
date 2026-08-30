package iliiasik.artistry.client.renderer;

import iliiasik.artistry.client.renderer.ImageOcclusionClipper.VisibleFragment;
import iliiasik.artistry.data.CanvasImage;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ImageOcclusionClipperTest {

    private static CanvasImage image(int x, int y, int w, int h) {
        return new CanvasImage(UUID.randomUUID(), x, y, w, h);
    }

    private static int visibleArea(List<VisibleFragment> fragments, CanvasImage image) {
        int area = 0;
        for (VisibleFragment fragment : fragments) {
            if (fragment.image() != image) continue;
            area += (fragment.destRect().x1() - fragment.destRect().x0())
                    * (fragment.destRect().y1() - fragment.destRect().y0());
        }
        return area;
    }

    private static void assertUvInsideUnitSquare(List<VisibleFragment> fragments) {
        for (VisibleFragment fragment : fragments) {
            assertTrue(fragment.u0() >= 0f && fragment.u0() <= 1f, "u0 " + fragment.u0());
            assertTrue(fragment.v0() >= 0f && fragment.v0() <= 1f, "v0 " + fragment.v0());
            assertTrue(fragment.u1() >= 0f && fragment.u1() <= 1f, "u1 " + fragment.u1());
            assertTrue(fragment.v1() >= 0f && fragment.v1() <= 1f, "v1 " + fragment.v1());
            assertTrue(fragment.u1() > fragment.u0(), "empty u span");
            assertTrue(fragment.v1() > fragment.v0(), "empty v span");
        }
    }

    @Test
    @DisplayName("An empty layer produces no fragments")
    void emptyLayer() {
        assertTrue(ImageOcclusionClipper.computeVisibleFragments(List.of()).isEmpty());
    }

    @Test
    @DisplayName("A single image is drawn as one full fragment")
    void singleImageIsWhole() {
        CanvasImage image = image(2, 3, 8, 6);
        List<VisibleFragment> fragments = ImageOcclusionClipper.computeVisibleFragments(List.of(image));

        assertEquals(1, fragments.size());
        VisibleFragment fragment = fragments.get(0);
        assertEquals(2, fragment.destRect().x0());
        assertEquals(3, fragment.destRect().y0());
        assertEquals(10, fragment.destRect().x1());
        assertEquals(9, fragment.destRect().y1());
        assertEquals(0f, fragment.u0());
        assertEquals(0f, fragment.v0());
        assertEquals(1f, fragment.u1());
        assertEquals(1f, fragment.v1());
    }

    @Test
    @DisplayName("Images that do not touch each other stay whole")
    void disjointImagesStayWhole() {
        CanvasImage left = image(0, 0, 4, 4);
        CanvasImage right = image(10, 10, 4, 4);

        List<VisibleFragment> fragments =
                ImageOcclusionClipper.computeVisibleFragments(List.of(left, right));

        assertEquals(2, fragments.size());
        assertEquals(16, visibleArea(fragments, left));
        assertEquals(16, visibleArea(fragments, right));
    }

    @Test
    @DisplayName("An image covered completely is not drawn at all")
    void coveredImageIsDropped() {
        CanvasImage bottom = image(4, 4, 4, 4);
        CanvasImage top = image(0, 0, 16, 16);

        List<VisibleFragment> fragments =
                ImageOcclusionClipper.computeVisibleFragments(List.of(bottom, top));

        assertEquals(0, visibleArea(fragments, bottom));
        assertEquals(256, visibleArea(fragments, top));
    }

    @Test
    @DisplayName("Two identical rectangles hide the lower one")
    void identicalRectanglesHideTheLowerOne() {
        CanvasImage bottom = image(1, 1, 5, 5);
        CanvasImage top = image(1, 1, 5, 5);

        List<VisibleFragment> fragments =
                ImageOcclusionClipper.computeVisibleFragments(List.of(bottom, top));

        assertEquals(0, visibleArea(fragments, bottom));
        assertEquals(25, visibleArea(fragments, top));
    }

    @Test
    @DisplayName("A partial overlap keeps exactly the uncovered area")
    void partialOverlapKeepsUncoveredArea() {
        CanvasImage bottom = image(0, 0, 8, 8);
        CanvasImage top = image(4, 0, 8, 8);

        List<VisibleFragment> fragments =
                ImageOcclusionClipper.computeVisibleFragments(List.of(bottom, top));

        assertEquals(32, visibleArea(fragments, bottom));
        assertEquals(64, visibleArea(fragments, top));
        assertUvInsideUnitSquare(fragments);
    }

    @Test
    @DisplayName("A hole in the middle splits the image into four pieces")
    void holeInTheMiddleSplitsIntoFourPieces() {
        CanvasImage bottom = image(0, 0, 9, 9);
        CanvasImage top = image(3, 3, 3, 3);

        List<VisibleFragment> fragments =
                ImageOcclusionClipper.computeVisibleFragments(List.of(bottom, top));

        long bottomFragments = fragments.stream().filter(f -> f.image() == bottom).count();
        assertEquals(4, bottomFragments);
        assertEquals(81 - 9, visibleArea(fragments, bottom));
        assertUvInsideUnitSquare(fragments);
    }

    @Test
    @DisplayName("The uv rectangle matches the visible part of the image")
    void uvMatchesVisiblePart() {
        CanvasImage bottom = image(0, 0, 10, 10);
        CanvasImage top = image(5, 0, 10, 10);

        List<VisibleFragment> fragments =
                ImageOcclusionClipper.computeVisibleFragments(List.of(bottom, top));

        VisibleFragment visible = fragments.stream()
                .filter(f -> f.image() == bottom)
                .findFirst()
                .orElseThrow();

        assertEquals(0f, visible.u0());
        assertEquals(0.5f, visible.u1());
        assertEquals(0f, visible.v0());
        assertEquals(1f, visible.v1());
    }

    @Test
    @DisplayName("Draw order decides which image wins")
    void drawOrderDecidesTheWinner() {
        CanvasImage first = image(0, 0, 6, 6);
        CanvasImage second = image(0, 0, 6, 6);

        List<VisibleFragment> forward =
                ImageOcclusionClipper.computeVisibleFragments(List.of(first, second));
        List<VisibleFragment> reversed =
                ImageOcclusionClipper.computeVisibleFragments(List.of(second, first));

        assertEquals(0, visibleArea(forward, first));
        assertEquals(36, visibleArea(forward, second));
        assertEquals(36, visibleArea(reversed, first));
        assertEquals(0, visibleArea(reversed, second));
    }

    @Test
    @DisplayName("A stack of overlapping images keeps only the visible remainder")
    void stackOfImagesKeepsRemainder() {
        CanvasImage a = image(0, 0, 12, 12);
        CanvasImage b = image(2, 2, 12, 12);
        CanvasImage c = image(4, 4, 12, 12);

        List<VisibleFragment> fragments =
                ImageOcclusionClipper.computeVisibleFragments(List.of(a, b, c));

        assertEquals(144 - 100, visibleArea(fragments, a));
        assertEquals(144 - 100, visibleArea(fragments, b));
        assertEquals(144, visibleArea(fragments, c));
        assertUvInsideUnitSquare(fragments);
    }

    @Test
    @DisplayName("Fragments never overlap each other")
    void fragmentsDoNotOverlap() {
        List<CanvasImage> images = List.of(
                image(0, 0, 10, 10),
                image(3, 3, 10, 10),
                image(6, 1, 5, 12),
                image(1, 8, 12, 4));

        List<VisibleFragment> fragments = ImageOcclusionClipper.computeVisibleFragments(images);

        boolean[][] covered = new boolean[32][32];
        for (VisibleFragment fragment : fragments) {
            for (int y = fragment.destRect().y0(); y < fragment.destRect().y1(); y++) {
                for (int x = fragment.destRect().x0(); x < fragment.destRect().x1(); x++) {
                    assertTrue(!covered[y][x], "cell " + x + "," + y + " is drawn twice");
                    covered[y][x] = true;
                }
            }
        }
    }
}
