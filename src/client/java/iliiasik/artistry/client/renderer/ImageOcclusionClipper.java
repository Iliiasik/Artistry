package iliiasik.artistry.client.renderer;

import iliiasik.artistry.data.CanvasImage;

import java.util.ArrayList;
import java.util.List;

public final class ImageOcclusionClipper {

    private ImageOcclusionClipper() {}

    public record Rect(int x0, int y0, int x1, int y1) {
        public int width() { return x1 - x0; }
        public int height() { return y1 - y0; }
        public boolean isEmpty() { return x1 <= x0 || y1 <= y0; }
    }


    public record VisibleFragment(CanvasImage image, Rect destRect, float u0, float v0, float u1, float v1) {}


    public static List<VisibleFragment> computeVisibleFragments(List<CanvasImage> drawOrder) {
        List<VisibleFragment> result = new ArrayList<>();

        for (int i = 0; i < drawOrder.size(); i++) {
            CanvasImage img = drawOrder.get(i);
            Rect full = new Rect(img.gridX, img.gridY, img.gridX + img.gridW, img.gridY + img.gridH);

            List<Rect> visible = new ArrayList<>();
            visible.add(full);

            for (int j = i + 1; j < drawOrder.size(); j++) {
                CanvasImage occluder = drawOrder.get(j);
                Rect occluderRect = new Rect(occluder.gridX, occluder.gridY,
                        occluder.gridX + occluder.gridW, occluder.gridY + occluder.gridH);

                List<Rect> next = new ArrayList<>();
                for (Rect r : visible) {
                    next.addAll(subtract(r, occluderRect));
                }
                visible = next;
                if (visible.isEmpty()) break;
            }

            for (Rect r : visible) {
                if (r.isEmpty()) continue;
                float u0 = (float) (r.x0 - img.gridX) / img.gridW;
                float v0 = (float) (r.y0 - img.gridY) / img.gridH;
                float u1 = (float) (r.x1 - img.gridX) / img.gridW;
                float v1 = (float) (r.y1 - img.gridY) / img.gridH;
                result.add(new VisibleFragment(img, r, u0, v0, u1, v1));
            }
        }

        return result;
    }


    private static List<Rect> subtract(Rect base, Rect cut) {
        int ix0 = Math.max(base.x0(), cut.x0());
        int iy0 = Math.max(base.y0(), cut.y0());
        int ix1 = Math.min(base.x1(), cut.x1());
        int iy1 = Math.min(base.y1(), cut.y1());

        if (ix1 <= ix0 || iy1 <= iy0) {
            List<Rect> result = new ArrayList<>(1);
            result.add(base);
            return result;
        }

        List<Rect> result = new ArrayList<>(4);

        if (iy0 > base.y0()) {
            result.add(new Rect(base.x0(), base.y0(), base.x1(), iy0));
        }
        if (iy1 < base.y1()) {
            result.add(new Rect(base.x0(), iy1, base.x1(), base.y1()));
        }
        if (ix0 > base.x0()) {
            result.add(new Rect(base.x0(), iy0, ix0, iy1));
        }
        if (ix1 < base.x1()) {
            result.add(new Rect(ix1, iy0, base.x1(), iy1));
        }

        result.removeIf(Rect::isEmpty);
        return result;
    }
}