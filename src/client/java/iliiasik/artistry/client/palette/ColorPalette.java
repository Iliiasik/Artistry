package iliiasik.artistry.client.palette;

public final class ColorPalette {

    private ColorPalette() {}

    public static int hsvToArgb(float h, float s, float v) {
        float[] rgb = hsvToRgb(h, s, v);
        int r = Math.round(rgb[0] * 255);
        int g = Math.round(rgb[1] * 255);
        int b = Math.round(rgb[2] * 255);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    public static float[] hsvToRgb(float h, float s, float v) {
        if (s == 0f) return new float[]{v, v, v};
        int i = (int)(h * 6);
        float f = h * 6 - i;
        float p = v * (1 - s);
        float q = v * (1 - f * s);
        float t = v * (1 - (1 - f) * s);
        return switch (i % 6) {
            case 0 -> new float[]{v, t, p};
            case 1 -> new float[]{q, v, p};
            case 2 -> new float[]{p, v, t};
            case 3 -> new float[]{p, q, v};
            case 4 -> new float[]{t, p, v};
            default -> new float[]{v, p, q};
        };
    }

    public static float[] rgbToHsv(float r, float g, float b) {
        float max = Math.max(r, Math.max(g, b));
        float min = Math.min(r, Math.min(g, b));
        float delta = max - min;
        float h = 0f, s = 0f, v = max;
        if (delta > 0.0001f) {
            s = delta / max;
            if (max == r)      h = (g - b) / delta / 6f;
            else if (max == g) h = (2f + (b - r) / delta) / 6f;
            else               h = (4f + (r - g) / delta) / 6f;
            if (h < 0) h += 1f;
        }
        return new float[]{h, s, v};
    }

    public static float[] argbToHsv(int argb) {
        float r = ((argb >> 16) & 0xFF) / 255f;
        float g = ((argb >> 8) & 0xFF) / 255f;
        float b = (argb & 0xFF) / 255f;
        return rgbToHsv(r, g, b);
    }

    public static String argbToHex(int argb) {
        return String.format("#%02X%02X%02X",
                (argb >> 16) & 0xFF,
                (argb >> 8) & 0xFF,
                argb & 0xFF);
    }

    public static int hexToArgb(String hex) {
        String clean = hex.startsWith("#") ? hex.substring(1) : hex;
        if (clean.length() != 6) throw new NumberFormatException("Invalid hex length");
        return 0xFF000000 | Integer.parseInt(clean, 16);
    }
}