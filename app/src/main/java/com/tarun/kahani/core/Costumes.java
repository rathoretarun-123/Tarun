package com.tarun.kahani.core;

/**
 * v34: a change of clothes for a character drawn from the user's own picture, when no picture of the new clothes
 * was given: the user's picture itself with its clothes recoloured to the new outfit's colours (the face, the skin,
 * the hair and every fold and shadow of the clothes stay as they are). The 3D maker is not needed for it.
 */
public final class Costumes {
    private Costumes() {}

    /**
     * The clothes of a cut-out (below the chin; neither skin nor hair-dark) recoloured: the most worn colour to
     * primary, the second one to secondary; white or grey cloth is tinted, so a white kurta can become a red one.
     */
    public static int[] recolour(int[] px, int w, int h, float chinY, int primary, int secondary) {
        int[] out = px.clone();
        int y0 = Math.max(0, Math.min(h - 1, Math.round(chinY * h)));
        // the two most worn colours of the clothes: 12 hue bins, a 13th for white and grey cloth
        float[] bins = new float[13];
        float[] v = new float[3];
        for (int y = y0; y < h; y++) for (int x = 0; x < w; x++) {
            int c = px[y * w + x];
            if ((c >>> 24) < 128 || Cutout.isSkin(c)) continue;
            hsv(c, v);
            if (v[2] < 0.18f) continue;                                   // hair, shoes, outlines stay
            bins[v[1] < 0.18f ? 12 : ((int) (v[0] / 30f + 0.5f)) % 12] += 1;
        }
        int b1 = 0;
        for (int i = 1; i < 13; i++) if (bins[i] > bins[b1]) b1 = i;
        if (bins[b1] == 0) return out;
        int b2 = -1;
        for (int i = 0; i < 13; i++) if (i != b1 && (b2 < 0 || bins[i] > bins[b2])) b2 = i;
        if (b2 >= 0 && bins[b2] < bins[b1] * 0.15f) b2 = -1;
        float[] t1 = new float[3], t2 = new float[3];
        hsv(primary, t1); hsv(secondary, t2);
        for (int y = y0; y < h; y++) for (int x = 0; x < w; x++) {
            int i = y * w + x, c = px[i];
            if ((c >>> 24) < 20 || Cutout.isSkin(c)) continue;
            hsv(c, v);
            if (v[2] < 0.18f) continue;
            int bin = v[1] < 0.18f ? 12 : ((int) (v[0] / 30f + 0.5f)) % 12;
            float[] to;
            if (near(bin, b1)) to = t1; else if (b2 >= 0 && near(bin, b2)) to = t2; else continue;
            // the new hue and saturation, the old light (folds and shadows stay); white cloth darkens a little to
            // carry the colour, dark cloth keeps its depth
            float sat = Math.max(to[1] * 0.9f, v[1]);
            float val = v[1] < 0.18f ? v[2] * (0.55f + 0.45f * to[2]) : Math.min(1f, v[2] * (0.6f + 0.4f * to[2] / Math.max(0.2f, v[2])));
            out[i] = (c & 0xFF000000) | rgb(to[0], Math.min(1, sat), Math.max(0, Math.min(1, val)));
        }
        return out;
    }

    static boolean near(int bin, int ref) {
        if (bin == 12 || ref == 12) return bin == ref;
        int d = Math.abs(bin - ref);
        return Math.min(d, 12 - d) <= 1;
    }

    static void hsv(int c, float[] out) {
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float hh;
        if (d == 0) hh = 0;
        else if (max == r) hh = 60 * (((g - b) / d) % 6);
        else if (max == g) hh = 60 * ((b - r) / d + 2);
        else hh = 60 * ((r - g) / d + 4);
        if (hh < 0) hh += 360;
        out[0] = hh; out[1] = max == 0 ? 0 : d / max; out[2] = max;
    }

    static int rgb(float h, float s, float v) {
        float c = v * s, x = c * (1 - Math.abs((h / 60f) % 2 - 1)), m = v - c;
        float r, g, b;
        if (h < 60) { r = c; g = x; b = 0; } else if (h < 120) { r = x; g = c; b = 0; } else if (h < 180) { r = 0; g = c; b = x; }
        else if (h < 240) { r = 0; g = x; b = c; } else if (h < 300) { r = x; g = 0; b = c; } else { r = c; g = 0; b = x; }
        return (Math.round((r + m) * 255) << 16) | (Math.round((g + m) * 255) << 8) | Math.round((b + m) * 255);
    }
}
