package com.tarun.kahani.core;

/**
 * v34: what a description puts on a face, painted onto a picture where its eyes are known — spectacles, dark
 * glasses, goggles, a cloth blindfold, an eye patch, a bandage round the forehead. Used when the 3D maker takes the
 * user's own uploaded figure as the base of a character it has no picture of (the figure keeps the user's style; the
 * description's details are added to it). Soft edges: every shape is drawn with a one-pixel falloff.
 */
public final class FaceProps {
    private FaceProps() {}

    /** True when the look has something this class can paint. */
    public static boolean any(Look l) {
        return l != null && (l.glasses > 0 || (l.injury & Look.INJ_HEAD) != 0);
    }

    /**
     * Paints in place. Eye points are fractions of the picture (x of its width, y of its height); eyeR is a fraction
     * of its width. The picture's left eye is the one with the smaller x.
     */
    public static void paint(int[] px, int w, int h, float eyeLX, float eyeLY, float eyeRX, float eyeRY, float eyeR, Look l) {
        if (!any(l) || px == null) return;
        float lx = eyeLX * w, ly = eyeLY * h, rx = eyeRX * w, ry = eyeRY * h;
        if (lx > rx) { float t = lx; lx = rx; rx = t; t = ly; ly = ry; ry = t; }
        float d = Math.max(4f, rx - lx);                         // the distance between the eyes: the face's scale
        float er = Math.max(2f, Math.min(eyeR * w, d * 0.45f));
        float cy = (ly + ry) / 2;
        if ((l.injury & Look.INJ_HEAD) != 0) {
            // a white bandage wound round the forehead between the brows and the hairline: it curves round the head
            // (lower at the sides, narrower there), lighter on top, a fold line, a gauze pad over one temple
            float cx = (lx + rx) / 2, by = cy - d * 0.6f;
            arcBand(px, w, h, cx, by, d * 1.08f, d * 0.12f, d * 0.24f, 0xFFFBFAF6, 0xFFD9D4C8);
            arcBand(px, w, h, cx, by + d * 0.02f, d * 1.02f, d * 0.12f, Math.max(1f, d * 0.018f), 0xFFC9C3B6, 0xFFC9C3B6);
            disc(px, w, h, cx + d * 0.52f, by + d * 0.02f, d * 0.16f, d * 0.12f, 0xFFFFFFFF, 1f);
            ring(px, w, h, cx + d * 0.52f, by + d * 0.02f, d * 0.15f, Math.max(1f, d * 0.015f), 0xFFE2DDD2);
        }
        switch (l.glasses) {
            case 1:      // round spectacles: thin dark rims, a bridge, a glint on each lens
                ring(px, w, h, lx, ly, er * 1.55f, Math.max(1.2f, er * 0.16f), 0xFF2E3338);
                ring(px, w, h, rx, ry, er * 1.55f, Math.max(1.2f, er * 0.16f), 0xFF2E3338);
                band(px, w, h, lx + er * 1.5f, ly - er * 0.2f, rx - er * 1.5f, ry - er * 0.2f, Math.max(1f, er * 0.13f), 0xFF2E3338);
                disc(px, w, h, lx - er * 0.5f, ly - er * 0.55f, er * 0.32f, er * 0.18f, 0x88FFFFFF, 1f);
                disc(px, w, h, rx - er * 0.5f, ry - er * 0.55f, er * 0.32f, er * 0.18f, 0x88FFFFFF, 1f);
                break;
            case 2:      // dark glasses: two dark lenses joined by a bridge
                disc(px, w, h, lx, ly, er * 1.6f, er * 1.3f, 0xFF15181C, 1f);
                disc(px, w, h, rx, ry, er * 1.6f, er * 1.3f, 0xFF15181C, 1f);
                band(px, w, h, lx + er * 1.4f, ly - er * 0.3f, rx - er * 1.4f, ry - er * 0.3f, Math.max(1f, er * 0.2f), 0xFF15181C);
                disc(px, w, h, lx - er * 0.6f, ly - er * 0.5f, er * 0.4f, er * 0.2f, 0x55FFFFFF, 1f);
                break;
            case 3:      // goggles: thick brown rims, a pale blue tint, a strap round the head
                band(px, w, h, lx - d * 0.95f, ly, rx + d * 0.95f, ry, Math.max(1.5f, er * 0.35f), 0xFF3E2723);
                ring(px, w, h, lx, ly, er * 1.7f, Math.max(1.5f, er * 0.4f), 0xFF5D4037);
                ring(px, w, h, rx, ry, er * 1.7f, Math.max(1.5f, er * 0.4f), 0xFF5D4037);
                disc(px, w, h, lx, ly, er * 1.5f, er * 1.5f, 0x5080DEEA, 1f);
                disc(px, w, h, rx, ry, er * 1.5f, er * 1.5f, 0x5080DEEA, 1f);
                break;
            case 4: {    // a cloth blindfold over both eyes, wrapping round the head (lower and narrower at the sides), with two folds
                float cx = (lx + rx) / 2;
                arcBand(px, w, h, cx, cy, d * 1.08f, er * 0.55f, er * 2.6f, 0xFF464B63, 0xFF1C1F2C);
                arcBand(px, w, h, cx, cy - er * 0.35f, d * 1.0f, er * 0.55f, Math.max(1f, er * 0.1f), 0xFF2A2E40, 0xFF2A2E40);
                arcBand(px, w, h, cx, cy + er * 0.55f, d * 1.0f, er * 0.55f, Math.max(1f, er * 0.1f), 0xFF151722, 0xFF151722);
                break;
            }
            case 5:      // a black patch over the left eye on a thin strap
                band(px, w, h, lx - d * 0.6f, ly - er * 1.1f, rx + d * 0.55f, ry - d * 0.75f, Math.max(1f, er * 0.18f), 0xFF15161A);
                disc(px, w, h, lx, ly, er * 1.55f, er * 1.4f, 0xFF15161A, 1f);
                break;
            default:
        }
    }

    /**
     * A band round the head seen from the front: its middle line sags by "sag" at the sides (it wraps round), it is
     * th thick in the middle and a quarter thinner at the sides (foreshortened), shaded from top to bottom, and
     * fades a little where it turns away.
     */
    static void arcBand(int[] px, int w, int h, float cx, float cy, float half, float sag, float th, int top, int bottom) {
        int x0 = Math.max(0, (int) (cx - half - 2)), x1 = Math.min(w - 1, (int) (cx + half + 2));
        int y0 = Math.max(0, (int) (cy - th - 2)), y1 = Math.min(h - 1, (int) (cy + sag + th + 2));
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            float u = (x + 0.5f - cx) / half;
            if (u < -1 || u > 1) continue;
            float mid = cy + sag * u * u, t = th * (1 - 0.25f * u * u);
            float dy = y + 0.5f - mid;
            float a = Math.max(0, Math.min(1, t / 2 - Math.abs(dy) + 0.5f));
            if (a <= 0) continue;
            a *= 1 - 0.35f * Math.max(0, (Math.abs(u) - 0.8f) / 0.2f);
            float k = Math.max(0, Math.min(1, dy / t + 0.5f));
            int c = mix(top, bottom, k);
            blend(px, y * w + x, c, a);
        }
    }

    private static int mix(int a, int b, float k) {
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k);
        int g = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    /** A filled ellipse, blended over the picture with a soft edge (only where the picture is not transparent). */
    static void disc(int[] px, int w, int h, float cx, float cy, float rx, float ry, int argb, float k) {
        int x0 = Math.max(0, (int) (cx - rx - 2)), x1 = Math.min(w - 1, (int) (cx + rx + 2));
        int y0 = Math.max(0, (int) (cy - ry - 2)), y1 = Math.min(h - 1, (int) (cy + ry + 2));
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            float dx = (x + 0.5f - cx) / rx, dy = (y + 0.5f - cy) / ry;
            float r = (float) Math.sqrt(dx * dx + dy * dy);
            float edge = (1 - r) * Math.min(rx, ry);                  // pixels inside the edge
            float a = Math.max(0, Math.min(1, edge + 0.5f)) * k;
            if (a > 0) blend(px, y * w + x, argb, a);
        }
    }

    /** A ring (an ellipse's outline) of the given thickness. */
    static void ring(int[] px, int w, int h, float cx, float cy, float r, float th, int argb) {
        int x0 = Math.max(0, (int) (cx - r - th - 2)), x1 = Math.min(w - 1, (int) (cx + r + th + 2));
        int y0 = Math.max(0, (int) (cy - r - th - 2)), y1 = Math.min(h - 1, (int) (cy + r + th + 2));
        for (int y = y0; y <= y1; y++) for (int x = x0; x <= x1; x++) {
            float dd = Math.abs((float) Math.hypot(x + 0.5f - cx, (y + 0.5f - cy) * 1.12f) - r);
            float a = Math.max(0, Math.min(1, th / 2 - dd + 0.5f));
            if (a > 0) blend(px, y * w + x, argb, a);
        }
    }

    /** A straight band from (x0,y0) to (x1,y1), th thick. */
    static void band(int[] px, int w, int h, float x0, float y0, float x1, float y1, float th, int argb) {
        float len = (float) Math.hypot(x1 - x0, y1 - y0);
        if (len < 1e-3f) return;
        float ux = (x1 - x0) / len, uy = (y1 - y0) / len;
        int bx0 = Math.max(0, (int) (Math.min(x0, x1) - th - 2)), bx1 = Math.min(w - 1, (int) (Math.max(x0, x1) + th + 2));
        int by0 = Math.max(0, (int) (Math.min(y0, y1) - th - 2)), by1 = Math.min(h - 1, (int) (Math.max(y0, y1) + th + 2));
        for (int y = by0; y <= by1; y++) for (int x = bx0; x <= bx1; x++) {
            float vx = x + 0.5f - x0, vy = y + 0.5f - y0;
            float along = vx * ux + vy * uy;
            if (along < 0 || along > len) continue;
            float across = Math.abs(-vx * uy + vy * ux);
            float a = Math.max(0, Math.min(1, th / 2 - across + 0.5f));
            if (a > 0) blend(px, y * w + x, argb, a);
        }
    }

    private static void blend(int[] px, int i, int argb, float a) {
        int c = px[i];
        if ((c >>> 24) < 16) return;                                 // never on the transparent background
        float ca = ((argb >>> 24) / 255f) * a;
        int r = Math.round(((c >> 16) & 255) * (1 - ca) + ((argb >> 16) & 255) * ca);
        int g = Math.round(((c >> 8) & 255) * (1 - ca) + ((argb >> 8) & 255) * ca);
        int b = Math.round((c & 255) * (1 - ca) + (argb & 255) * ca);
        px[i] = (c & 0xFF000000) | (r << 16) | (g << 8) | b;
    }
}
