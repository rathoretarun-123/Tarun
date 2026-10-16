package com.tarun.kahani.core;

/**
 * A smooth blur for the out-of-focus background of close shots: three box passes in each direction (very close
 * to a Gaussian), so the far picture melts softly instead of breaking into blocks when it is enlarged.
 */
public final class Blur {
    private Blur() {}

    /** Blurs ARGB pixels in place; r = box radius in pixels. */
    public static void gauss(int[] px, int w, int h, int r) {
        if (r < 1 || w < 2 || h < 2) return;
        int[] tmp = new int[Math.max(w, h)];
        int[] out = new int[Math.max(w, h)];
        for (int pass = 0; pass < 3; pass++) {
            for (int y = 0; y < h; y++) { System.arraycopy(px, y * w, tmp, 0, w); line(tmp, out, w, r); System.arraycopy(out, 0, px, y * w, w); }
            for (int x = 0; x < w; x++) {
                for (int y = 0; y < h; y++) tmp[y] = px[y * w + x];
                line(tmp, out, h, r);
                for (int y = 0; y < h; y++) px[y * w + x] = out[y];
            }
        }
    }

    /** One running-sum box pass over n values (edges repeat the end pixel). */
    private static void line(int[] in, int[] out, int n, int r) {
        int sa = 0, sr = 0, sg = 0, sb = 0, div = 2 * r + 1;
        for (int i = -r; i <= r; i++) {
            int c = in[Math.max(0, Math.min(n - 1, i))];
            sa += c >>> 24; sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255;
        }
        for (int i = 0; i < n; i++) {
            out[i] = ((sa / div) << 24) | ((sr / div) << 16) | ((sg / div) << 8) | (sb / div);
            int add = in[Math.min(n - 1, i + r + 1)], rem = in[Math.max(0, i - r)];
            sa += (add >>> 24) - (rem >>> 24);
            sr += ((add >> 16) & 255) - ((rem >> 16) & 255);
            sg += ((add >> 8) & 255) - ((rem >> 8) & 255);
            sb += (add & 255) - (rem & 255);
        }
    }
}
