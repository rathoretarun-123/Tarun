package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Up to ten pictures of one thing from different angles (the phone guide §5.2): which angle each picture shows,
 * and the splitting of one picture that holds several angles side by side into separate pictures.
 */
public final class Angles {
    private Angles() {}

    /** The view angles the studio keeps: 0 front, -45 three-quarter, -90 side, 180 back (Figure3D.VIEW_ANGLES for the last three). */
    public static final float FRONT = 0, THREE_QUARTER = -45, SIDE = -90, BACK = 180;

    /** One figure cut from a sheet: its pixels (with transparency) and where it was. */
    public static final class Piece {
        public int[] px;
        public int w, h, x0, y0;
    }

    /**
     * Splits a picture that holds several figures side by side (a sheet of angles) into its figures: the
     * background is removed, the opaque parts are grouped (parts closer than 2% of the width belong together),
     * groups under 3% of the area are noise, and the pieces come back left to right. One figure gives one piece.
     */
    public static List<Piece> split(int[] src, int w, int h) {
        int[] px = src.clone();
        if (!Cutout.hasAlpha(px)) Cutout.removeBackground(px, w, h);
        // a coarse grid of opaque cells (joins parts across small gaps)
        int cell = Math.max(2, w / 160);
        int gw = (w + cell - 1) / cell, gh = (h + cell - 1) / cell;
        boolean[] on = new boolean[gw * gh];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if ((px[y * w + x] >>> 24) > 100) on[(y / cell) * gw + x / cell] = true;
        int reach = Math.max(1, Math.round(0.02f * w / cell));
        int[] label = new int[gw * gh];
        int n = 0;
        int[] stack = new int[gw * gh];
        for (int i = 0; i < on.length; i++) {
            if (!on[i] || label[i] != 0) continue;
            n++;
            int sp = 0;
            stack[sp++] = i;
            label[i] = n;
            while (sp > 0) {
                int c = stack[--sp];
                int cx = c % gw, cy = c / gw;
                for (int dy = -reach; dy <= reach; dy++) for (int dx = -reach; dx <= reach; dx++) {
                    int nx = cx + dx, ny = cy + dy;
                    if (nx < 0 || ny < 0 || nx >= gw || ny >= gh) continue;
                    int j = ny * gw + nx;
                    if (on[j] && label[j] == 0) { label[j] = n; stack[sp++] = j; }
                }
            }
        }
        List<Piece> out = new ArrayList<Piece>();
        int minArea = Math.round(0.03f * gw * gh);
        for (int k = 1; k <= n; k++) {
            int x0 = gw, y0 = gh, x1 = -1, y1 = -1, area = 0;
            for (int i = 0; i < label.length; i++) if (label[i] == k) { int x = i % gw, y = i / gw; area++; x0 = Math.min(x0, x); y0 = Math.min(y0, y); x1 = Math.max(x1, x); y1 = Math.max(y1, y); }
            if (area < minArea) continue;
            Piece p = new Piece();
            p.x0 = Math.max(0, x0 * cell - cell); p.y0 = Math.max(0, y0 * cell - cell);
            int px1 = Math.min(w, (x1 + 2) * cell), py1 = Math.min(h, (y1 + 2) * cell);
            p.w = px1 - p.x0; p.h = py1 - p.y0;
            p.px = new int[p.w * p.h];
            for (int y = 0; y < p.h; y++) for (int x = 0; x < p.w; x++) {
                int i = (y + p.y0) * w + x + p.x0;
                // only this group's pixels (a neighbour's arm reaching into the box stays transparent)
                int gx = (x + p.x0) / cell, gy = (y + p.y0) / cell;
                p.px[y * p.w + x] = label[gy * gw + gx] == k ? px[i] : 0;
            }
            out.add(p);
        }
        // left to right (a sheet reads that way)
        java.util.Collections.sort(out, new java.util.Comparator<Piece>() {
            public int compare(Piece a, Piece b) { return Integer.compare(a.x0, b.x0); }
        });
        return out;
    }

    /**
     * The angle a figure's picture shows, from where its face is: both eyes found and centred = front; the
     * eyes off the figure's centre = three-quarter; one narrow figure with a face at its edge = side; no face
     * on a figure = back. Returns FRONT, THREE_QUARTER, SIDE or BACK.
     */
    public static float guess(Cutout.Result r) {
        if (r == null) return FRONT;
        if (!r.faceFound) {
            // no face: a back (or a side so narrow that the face finder lost it)
            return r.h > 0 && r.w / (float) r.h < 0.3f ? SIDE : BACK;
        }
        // the face finder also "finds" a face on the back of a head (the neck, the arms): real eyes are dark spots
        // on skin, a back has none (the spots are as bright as the skin), or its "face" is as wide as the body
        float inter = Math.max(0.02f, r.eyeRX - r.eyeLX);
        float dark = eyeDarkness(r);
        float faceW = inter / 0.4f;
        float hairAbove = (r.faceTop - r.headTop) / Math.max(0.01f, r.chinY - r.faceTop);
        float skin = skinShare(r, r.eyeLX - inter * 0.5f, r.eyeY - inter * 0.3f, r.eyeRX + inter * 0.5f, r.mouthY + inter * 0.25f);
        if (dark > 0.42f || faceW > 1.0f || (hairAbove > 2.2f && skin < 0.3f)) return BACK;
        float eyeMid = (r.eyeLX + r.eyeRX) / 2f;
        float off = Math.abs(eyeMid - 0.5f);
        if (inter < 0.05f || off > 0.16f) return SIDE;
        if (off > 0.06f || inter < 0.085f) return THREE_QUARTER;
        return FRONT;
    }

    /** How dark the two eye spots are against the face's skin (1 = as bright as skin, 0 = black): real eyes are dark. */
    public static float eyeDarkness(Cutout.Result r) {
        if (r.px == null || r.w <= 0) return 1;
        float skinL = lum(r.skin);
        float sum = 0; int n = 0;
        float rad = Math.max(1.5f, r.eyeR * r.w * 0.55f);
        float[][] eyes = {{r.eyeLX * r.w, r.eyeY * r.h}, {r.eyeRX * r.w, r.eyeY * r.h}};
        for (float[] e : eyes) {
            for (int y = Math.round(e[1] - rad); y <= e[1] + rad; y++) for (int x = Math.round(e[0] - rad); x <= e[0] + rad; x++) {
                if (x < 0 || y < 0 || x >= r.w || y >= r.h) continue;
                int c = r.px[y * r.w + x];
                if ((c >>> 24) < 100) continue;
                sum += lum(c); n++;
            }
        }
        return n == 0 ? 1 : (sum / n) / Math.max(1, skinL);
    }

    static float lum(int c) { return 0.299f * ((c >> 16) & 255) + 0.587f * ((c >> 8) & 255) + 0.114f * (c & 255); }

    /** The measures the guess is made from, for checking. */
    public static String debug(Cutout.Result r) {
        float inter = Math.max(0.02f, r.eyeRX - r.eyeLX);
        return String.format(java.util.Locale.US, "face=%b hairAbove=%.2f faceW=%.2f eyeDark=%.2f skin=%.2f mouthW=%.2f", r.faceFound,
                (r.faceTop - r.headTop) / Math.max(0.01f, r.chinY - r.faceTop), inter / 0.4f, eyeDarkness(r),
                skinShare(r, r.eyeLX - inter * 0.5f, r.eyeY - inter * 0.3f, r.eyeRX + inter * 0.5f, r.mouthY + inter * 0.25f), r.mouthW);
    }

    /** The share of skin-coloured pixels in a box of the cut-out (fractions of its width and height). */
    static float skinShare(Cutout.Result r, float fx0, float fy0, float fx1, float fy1) {
        if (r.px == null || r.w <= 0 || r.h <= 0) return 1;
        int x0 = Math.max(0, Math.round(fx0 * r.w)), x1 = Math.min(r.w, Math.round(fx1 * r.w));
        int y0 = Math.max(0, Math.round(fy0 * r.h)), y1 = Math.min(r.h, Math.round(fy1 * r.h));
        int n = 0, skin = 0;
        for (int y = y0; y < y1; y++) for (int x = x0; x < x1; x++) {
            int c = r.px[y * r.w + x];
            if ((c >>> 24) < 100) continue;
            n++;
            if (Cutout.isSkin(c)) skin++;
        }
        return n == 0 ? 1 : skin / (float) n;
    }

    public static String name(float angle) {
        if (Math.abs(angle - BACK) < 1) return "back";
        if (Math.abs(angle - SIDE) < 1 || Math.abs(angle + SIDE) < 1) return "side";
        if (Math.abs(angle - THREE_QUARTER) < 1 || Math.abs(angle + THREE_QUARTER) < 1) return "three-quarter";
        return "front";
    }

    /** Among several pictures that all guess the same angle, the later ones step on to the next missing angle (ten pictures never all become "front"). */
    public static float[] assign(float[] guessed) {
        float[] out = guessed.clone();
        boolean[] taken = new boolean[4];
        float[] order = {FRONT, THREE_QUARTER, SIDE, BACK};
        for (int i = 0; i < out.length; i++) {
            int idx = index(out[i]);
            if (!taken[idx]) { taken[idx] = true; continue; }
            int free = -1;
            for (int k = 0; k < 4; k++) if (!taken[k]) { free = k; break; }
            if (free < 0) { out[i] = Float.NaN; continue; }   // a fifth angle: kept in the library as another picture of the same thing
            out[i] = order[free];
            taken[free] = true;
        }
        return out;
    }

    static int index(float a) {
        if (Math.abs(a - BACK) < 1) return 3;
        if (Math.abs(Math.abs(a) - 90) < 1) return 2;
        if (Math.abs(Math.abs(a) - 45) < 1) return 1;
        return 0;
    }
}
