package com.tarun.kahani.core;

/** Colour edits from the command box (brightness, contrast, saturation, warmth) applied to finished frames. */
public final class Grade {
    private final byte[] r = new byte[256], g = new byte[256], b = new byte[256];
    private final int sat;            // saturation * 256
    public final boolean identity;

    public Grade(Edits e) {
        float br = e == null ? 0 : e.brightness, ct = e == null ? 1 : e.contrast, wm = e == null ? 0 : e.warmth;
        sat = Math.round((e == null ? 1 : e.saturation) * 256);
        identity = Math.abs(br) < 0.01f && Math.abs(ct - 1) < 0.01f && Math.abs(wm) < 0.01f && sat == 256;
        for (int i = 0; i < 256; i++) {
            float v = i / 255f;
            v = (v - 0.5f) * ct + 0.5f;
            v += br * 0.35f;
            r[i] = clamp(v + wm * 0.06f);
            g[i] = clamp(v + wm * 0.015f);
            b[i] = clamp(v - wm * 0.07f);
        }
    }

    static byte clamp(float v) { int x = Math.round(v * 255); return (byte) (x < 0 ? 0 : x > 255 ? 255 : x); }

    public void apply(int[] px, int n) {
        if (identity) return;
        for (int i = 0; i < n; i++) {
            int c = px[i];
            int R = r[(c >> 16) & 255] & 255, G = g[(c >> 8) & 255] & 255, B = b[c & 255] & 255;
            if (sat != 256) {
                int y = (R * 77 + G * 150 + B * 29) >> 8;
                R = y + (((R - y) * sat) >> 8); G = y + (((G - y) * sat) >> 8); B = y + (((B - y) * sat) >> 8);
                R = R < 0 ? 0 : R > 255 ? 255 : R; G = G < 0 ? 0 : G > 255 ? 255 : G; B = B < 0 ? 0 : B > 255 ? 255 : B;
            }
            px[i] = (c & 0xFF000000) | (R << 16) | (G << 8) | B;
        }
    }
}
