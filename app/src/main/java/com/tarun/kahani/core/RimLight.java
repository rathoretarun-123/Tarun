package com.tarun.kahani.core;

/**
 * Rim light for a cut-out character, as in an animated feature: a soft, warm edge of light along the outline on
 * the side the key light comes from (and a little along the top: hair, shoulders), so the character sits in the
 * place's light instead of looking pasted on. Made once per picture, at half size (it is soft), for light from
 * the left and from the right; drawn through the character's own mesh, so it bends with every move.
 */
public final class RimLight {
    private RimLight() {}

    /** {left-lit pixels, right-lit pixels} at (w / 2) x (h / 2), from the cut-out's outline (ARGB). */
    public static int[][] make(int[] px, int w, int h) {
        int hw = Math.max(1, w / 2), hh = Math.max(1, h / 2);
        float[] a = new float[hw * hh];
        for (int y = 0; y < hh; y++) for (int x = 0; x < hw; x++) {
            int s = 0;
            for (int dy = 0; dy < 2; dy++) for (int dx = 0; dx < 2; dx++) {
                int xx = Math.min(w - 1, x * 2 + dx), yy = Math.min(h - 1, y * 2 + dy);
                s += px[yy * w + xx] >>> 24;
            }
            a[y * hw + x] = s / (4 * 255f);
        }
        // the soft inside of the outline: alpha blurred over about 1 % of the height
        int r = Math.max(2, hh / 110);
        float[] b = blur(a, hw, hh, r);
        int[] left = new int[hw * hh], right = new int[hw * hh];
        for (int y = 1; y < hh - 1; y++) for (int x = 1; x < hw - 1; x++) {
            int i = y * hw + x;
            float al = a[i];
            if (al < 0.05f) continue;
            float edge = Math.max(0, Math.min(1, (1 - b[i]) * 2.6f)) * al;   // 1 near the outline, 0 deep inside
            if (edge < 0.02f) continue;
            // the outline's outward direction (towards where the picture ends)
            float nx = b[i - 1] - b[i + 1], ny = b[i - hw] - b[i + hw];
            float len = (float) Math.sqrt(nx * nx + ny * ny);
            if (len < 1e-4f) continue;
            nx /= len; ny /= len;
            float up = Math.max(0, -ny) * 0.45f;
            float fl = Math.max(0, -nx), fr = Math.max(0, nx);
            int aL = Math.round(255 * edge * Math.min(1, (float) Math.pow(fl, 0.8) + up * fl * 0.5f + up * 0.35f));
            int aR = Math.round(255 * edge * Math.min(1, (float) Math.pow(fr, 0.8) + up * fr * 0.5f + up * 0.35f));
            // warm white light
            if (aL > 0) left[i] = (Math.min(255, aL) << 24) | 0xFFF2DE;
            if (aR > 0) right[i] = (Math.min(255, aR) << 24) | 0xFFF2DE;
        }
        return new int[][]{left, right, {hw, hh}};
    }

    private static float[] blur(float[] a, int w, int h, int r) {
        float[] t = new float[a.length], o = new float[a.length];
        for (int y = 0; y < h; y++) {
            float s = 0;
            for (int x = -r; x <= r; x++) s += a[y * w + Math.max(0, Math.min(w - 1, x))];
            for (int x = 0; x < w; x++) {
                t[y * w + x] = s / (2 * r + 1);
                s += a[y * w + Math.min(w - 1, x + r + 1)] - a[y * w + Math.max(0, x - r)];
            }
        }
        for (int x = 0; x < w; x++) {
            float s = 0;
            for (int y = -r; y <= r; y++) s += t[Math.max(0, Math.min(h - 1, y)) * w + x];
            for (int y = 0; y < h; y++) {
                o[y * w + x] = s / (2 * r + 1);
                s += t[Math.min(h - 1, y + r + 1) * w + x] - t[Math.max(0, y - r) * w + x];
            }
        }
        return o;
    }
}
