package com.tarun.kahani.core;

/**
 * Turns a real photo (person, animal, place) into a flat cartoon picture: smooths skin and surfaces, reduces colours
 * to bold bands, brightens them and draws dark outlines. Used for "make an avatar from my photo".
 */
public final class Toon {
    private Toon() {}

    public static int[] apply(int[] src, int w, int h) {
        int[] a = smooth(src, w, h, 2);
        a = smooth(a, w, h, 2);
        int[] out = new int[w * h];
        // edges from luminance gradient of the smoothed picture
        float[] lum = new float[w * h];
        for (int i = 0; i < w * h; i++) {
            int c = a[i];
            lum[i] = ((c >> 16) & 255) * 0.3f + ((c >> 8) & 255) * 0.59f + (c & 255) * 0.11f;
        }
        int step = 28;  // colour band width
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x;
                int c = a[i];
                int R = (c >> 16) & 255, G = (c >> 8) & 255, B = c & 255;
                // boost saturation a little, then posterise
                int Y = (R * 77 + G * 150 + B * 29) >> 8;
                R = clamp(Y + (R - Y) * 13 / 10); G = clamp(Y + (G - Y) * 13 / 10); B = clamp(Y + (B - Y) * 13 / 10);
                R = clamp((R / step) * step + step / 2); G = clamp((G / step) * step + step / 2); B = clamp((B / step) * step + step / 2);
                float e = 0;
                if (x > 0 && y > 0 && x < w - 1 && y < h - 1) {
                    float gx = lum[i + 1] - lum[i - 1], gy = lum[i + w] - lum[i - w];
                    e = (float) Math.sqrt(gx * gx + gy * gy);
                }
                if (e > 38) { float k = Math.max(0.15f, 1 - (e - 38) / 60f); R = (int) (R * k); G = (int) (G * k); B = (int) (B * k); }
                out[i] = (c & 0xFF000000) | (R << 16) | (G << 8) | B;
            }
        }
        return out;
    }

    static int clamp(int v) { return v < 0 ? 0 : v > 255 ? 255 : v; }

    /** Edge-preserving smoothing (small bilateral-like filter on a grid). */
    static int[] smooth(int[] s, int w, int h, int r) {
        int[] o = new int[w * h];
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = s[y * w + x];
                int cr = (c >> 16) & 255, cg = (c >> 8) & 255, cb = c & 255;
                float sr = 0, sg = 0, sb = 0, sw = 0;
                for (int dy = -r; dy <= r; dy++) {
                    int yy = y + dy;
                    if (yy < 0 || yy >= h) continue;
                    for (int dx = -r; dx <= r; dx++) {
                        int xx = x + dx;
                        if (xx < 0 || xx >= w) continue;
                        int d = s[yy * w + xx];
                        int dr = (d >> 16) & 255, dg = (d >> 8) & 255, db = d & 255;
                        int diff = Math.abs(dr - cr) + Math.abs(dg - cg) + Math.abs(db - cb);
                        float wt = diff > 90 ? 0.02f : 1f - diff / 100f;
                        sr += dr * wt; sg += dg * wt; sb += db * wt; sw += wt;
                    }
                }
                o[y * w + x] = (c & 0xFF000000) | (Math.round(sr / sw) << 16) | (Math.round(sg / sw) << 8) | Math.round(sb / sw);
            }
        }
        return o;
    }
}
