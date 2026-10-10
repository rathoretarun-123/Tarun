package com.tarun.kahani.core;

/**
 * Removes the plain (usually white) background around a character picture and finds the face,
 * so uploaded character art can be animated: mouth for lip-sync, eyes for blinking.
 * Works on ARGB int arrays so it runs identically on Android and desktop.
 */
public final class Cutout {
    private Cutout() {}

    /** Result: cropped pixels plus face landmarks in normalised (0..1) coordinates of the crop. */
    public static final class Result {
        public int[] px;
        public int w, h;
        public float mouthX = 0.5f, mouthY = 0.2f, mouthW = 0.08f;
        public float eyeLX = 0.44f, eyeRX = 0.56f, eyeY = 0.15f, eyeR = 0.03f;
        public float headTop = 0f, faceTop = 0.05f, chinY = 0.25f;
        public int skin = 0xFFD9A074, lip = 0xFF9C4A3E;
        public boolean faceFound;
        /** v34: heads side by side in the picture (Ravana): the face found is the central one's; 1 for everyone else. */
        public int heads = 1;
        /**
         * v34 (the emotion / activity / angle guide §7.3): the pixels the readings are made from — for a figure so
         * dark that no face can be read in it as it is, a brightened copy (gamma and a contrast stretch, the
         * original kept for drawing); null when the figure reads as it is. read() gives the right one.
         */
        public int[] pxRead;
        /** v34: the figure's mean luminance (0..255) and its light level (BRIGHT … SILHOUETTE), tagged on every picture (§6 step 6). */
        public float light = 128;
        public int lightLevel = NORMAL;
        public int[] read() { return pxRead != null ? pxRead : px; }
        /** Where this cut-out sits in the picture it was cut from (pixels), and that picture's width. */
        public int cropX, cropY, cropW, srcW;
    }

    /** v34: light levels of a figure, from its mean luminance (the guide's bright / normal / low / very_low / silhouette). */
    public static final int BRIGHT = 0, NORMAL = 1, LOW = 2, VERY_LOW = 3, SILHOUETTE = 4;

    public static String lightName(int level) {
        switch (level) { case BRIGHT: return "bright"; case LOW: return "low light"; case VERY_LOW: return "very dark"; case SILHOUETTE: return "silhouette"; default: return "normal light"; }
    }

    public static int lightLevel(float meanLum) { return meanLum >= 165 ? BRIGHT : meanLum >= 70 ? NORMAL : meanLum >= 45 ? LOW : meanLum >= 25 ? VERY_LOW : SILHOUETTE; }

    /**
     * v34 (§7.3, §7.4): the reading copy of a dark figure — gamma 1.8 then a stretch that puts its brightest 2% near
     * white, so the eyes, the mouth and the skin can be found in a picture taken in a dark room. Alpha is kept; the
     * original pixels are what the film draws.
     */
    static int[] enhance(int[] px, int w, int h) {
        int[] out = new int[px.length];
        int[] lut = new int[256];
        for (int i = 0; i < 256; i++) lut[i] = Math.min(255, (int) Math.round(255 * Math.pow(i / 255.0, 1 / 1.8)));
        int[] hist = new int[256];
        int n = 0;
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            if ((c >>> 24) < 100) { out[i] = c; continue; }
            int r = lut[(c >> 16) & 255], g = lut[(c >> 8) & 255], b = lut[c & 255];
            out[i] = (c & 0xFF000000) | (r << 16) | (g << 8) | b;
            hist[(r * 299 + g * 587 + b * 114) / 1000]++;
            n++;
        }
        if (n == 0) return out;
        int acc = 0, p98 = 255;
        for (int i = 0; i < 256; i++) { acc += hist[i]; if (acc >= n * 0.98f) { p98 = i; break; } }
        if (p98 < 225 && p98 > 0) {
            float k = 235f / p98;
            for (int i = 0; i < out.length; i++) {
                int c = out[i];
                if ((c >>> 24) < 100) continue;
                int r = Math.min(255, (int) (((c >> 16) & 255) * k)), g = Math.min(255, (int) (((c >> 8) & 255) * k)), b = Math.min(255, (int) ((c & 255) * k));
                out[i] = (c & 0xFF000000) | (r << 16) | (g << 8) | b;
            }
        }
        return out;
    }

    static int dist(int a, int b) {
        int dr = ((a >> 16) & 255) - ((b >> 16) & 255);
        int dg = ((a >> 8) & 255) - ((b >> 8) & 255);
        int db = (a & 255) - (b & 255);
        return Math.abs(dr) + Math.abs(dg) + Math.abs(db);
    }

    /** True if the picture already has transparency (e.g. a PNG cut-out). */
    public static boolean hasAlpha(int[] px) {
        int n = 0;
        for (int i = 0; i < px.length; i += 97) if ((px[i] >>> 24) < 250) n++;
        return n > px.length / 97 / 50;
    }

    public static Result process(int[] src, int w, int h) {
        return process(src, w, h, false);
    }

    /** holes: also clear background seen through closed gaps (between an animal's legs, under its belly). */
    public static Result process(int[] src, int w, int h, boolean holes) {
        boolean given = hasAlpha(src);
        int[] px = given ? src.clone() : cutFull(src, w, h, holes);
        if (!given) defringe(px, src, w, h);
        // crop to opaque bounds
        int minX = w, minY = h, maxX = -1, maxY = -1;
        for (int y = 0; y < h; y++) {
            int row = y * w;
            for (int x = 0; x < w; x++) {
                if ((px[row + x] >>> 24) > 40) {
                    if (x < minX) minX = x;
                    if (x > maxX) maxX = x;
                    if (y < minY) minY = y;
                    if (y > maxY) maxY = y;
                }
            }
        }
        Result r = new Result();
        if (maxX < 0) { r.px = px; r.w = w; r.h = h; return r; }
        int cw = maxX - minX + 1, ch = maxY - minY + 1;
        int[] out = new int[cw * ch];
        for (int y = 0; y < ch; y++) System.arraycopy(px, (y + minY) * w + minX, out, y * cw, cw);
        r.px = out; r.w = cw; r.h = ch;
        r.cropX = minX; r.cropY = minY; r.cropW = cw; r.srcW = w;
        // v34: the light level of the figure; a dark one is read from a brightened copy (the original is drawn)
        long lsum = 0; int ln = 0;
        for (int i = 0; i < r.px.length; i += 3) { int c = r.px[i]; if ((c >>> 24) >= 100) { lsum += (((c >> 16) & 255) * 299 + ((c >> 8) & 255) * 587 + (c & 255) * 114) / 1000; ln++; } }
        r.light = ln == 0 ? 0 : lsum / (float) ln;
        r.lightLevel = lightLevel(r.light);
        // the picture as it is first: a face read in its own light is the reading (dark clothes or fur do not make
        // a dark room, and brightening fur turns it skin-coloured — a monkey's back would read as a face)
        findFace(r);
        refineFace(r, 0, r.w);
        countHeads(r);
        if (!r.faceFound && r.lightLevel >= LOW && ln > 0) {
            // the dual path (the guide §7.3): no face could be read in the dark — the brightened copy is read instead
            int[] orig = r.px;
            r.px = enhance(orig, r.w, r.h);
            findFace(r);
            refineFace(r, 0, r.w);
            countHeads(r);
            if (r.faceFound) {
                r.pxRead = r.px;
                // the skin colour for drawing is the picture's own: the reading copy's, darkened back
                long esum = 0; int en = 0;
                for (int i = 0; i < r.pxRead.length; i += 3) { int c = r.pxRead[i]; if ((c >>> 24) >= 100) { esum += (((c >> 16) & 255) * 299 + ((c >> 8) & 255) * 587 + (c & 255) * 114) / 1000; en++; } }
                float k = en == 0 ? 1 : Math.max(0.1f, Math.min(1f, r.light / Math.max(1f, esum / (float) en)));
                r.skin = 0xFF000000 | ((int) (((r.skin >> 16) & 255) * k) << 16) | ((int) (((r.skin >> 8) & 255) * k) << 8) | (int) ((r.skin & 255) * k);
                r.lip = Puppet.shade(Puppet.mix(r.skin, 0xFFB03A3A, 0.45f), 0.8f);
            }
            r.px = orig;
        }
        return r;
    }

    /**
     * v34: a many-headed character (Ravana's ten heads in a row): the face-shaped runs of skin across the head band,
     * side by side, each about as wide as the face found. Three or more make a many-headed figure (two could be a
     * hand beside the face); the face is then taken from the central run, since the central head is the one that
     * speaks and looks at the camera.
     */
    static void countHeads(Result r) {
        if (!r.faceFound || r.w < 40 || r.h < 40) return;
        int w = r.w, h = r.h;
        int y0 = Math.max(0, (int) (r.faceTop * h)), y1 = Math.min(h - 1, (int) (r.chinY * h));
        if (y1 - y0 < 6) return;
        float faceWpx = Math.max(8f, (r.eyeRX - r.eyeLX) * w * 2.5f);
        float[] share = new float[w];
        for (int x = 0; x < w; x++) {
            int n = 0;
            for (int y = y0; y <= y1; y++) if (isSkin(r.px[y * w + x])) n++;
            share[x] = n / (float) (y1 - y0 + 1);
        }
        java.util.List<int[]> runs = new java.util.ArrayList<int[]>();
        int start = -1, gap = 0;
        for (int x = 0; x <= w; x++) {
            boolean on = x < w && share[x] > 0.45f;
            if (on) { if (start < 0) start = x; gap = 0; }
            else if (start >= 0 && ++gap > faceWpx * 0.12f) { runs.add(new int[]{start, x - gap}); start = -1; gap = 0; }
        }
        if (start >= 0) runs.add(new int[]{start, w - 1});
        java.util.List<int[]> heads = new java.util.ArrayList<int[]>();
        for (int[] run : runs) {
            int rw = run[1] - run[0] + 1;
            if (rw < faceWpx * 0.45f || rw > faceWpx * 1.8f) continue;
            float mean = 0;
            for (int x = run[0]; x <= run[1]; x++) mean += share[x];
            if (mean / rw < 0.62f) continue;                       // a hand in the band fills less of it than a face does
            heads.add(run);
        }
        if (heads.size() < 3) return;
        r.heads = heads.size();
        int[] central = heads.get(0);
        for (int[] run : heads) if (Math.abs((run[0] + run[1]) / 2f - w / 2f) < Math.abs((central[0] + central[1]) / 2f - w / 2f)) central = run;
        float mx = r.mouthX * w;
        if (mx < central[0] - faceWpx * 0.1f || mx > central[1] + faceWpx * 0.1f) {
            // the eyes found belong to a side head: the central head's own, if a convincing pair is there
            refineFace(r, Math.max(0, central[0] - (int) (faceWpx * 0.2f)), Math.min(w, central[1] + (int) (faceWpx * 0.2f)));
        }
    }

    /**
     * Finds the real eyes and mouth (instead of guessing them from face proportions): eyes are a matching pair of
     * dark irises inside brighter whites, side by side in the upper part of the figure; the mouth is the darkest,
     * reddest short line below them, at a distance that fits the eyes' spacing. Keeps the old guess when no
     * convincing pair is found.
     */
    static void refineFace(Result r) { refineFace(r, 0, r.w); }

    /** rx0..rx1: the columns the eyes may lie in (v34: one head of a many-headed figure); the whole width otherwise. */
    static void refineFace(Result r, int rx0, int rx1) {
        int w = r.w, h = r.h;
        if (w < 40 || h < 40) return;
        final float rc = (rx0 + rx1) / 2f, rw = Math.max(1, rx1 - rx0);
        int W1 = w + 1;
        long[] iy = new long[W1 * (h + 1)], ia = new long[W1 * (h + 1)], is = new long[W1 * (h + 1)], ib = new long[W1 * (h + 1)], ic = new long[W1 * (h + 1)];
        int[] lum = new int[w * h];
        for (int y = 0; y < h; y++) {
            long rowY = 0, rowA = 0, rowS = 0, rowB = 0, rowC = 0;
            for (int x = 0; x < w; x++) {
                int c = r.px[y * w + x];
                boolean op = (c >>> 24) > 200;
                int l = op ? (((c >> 16) & 255) * 299 + ((c >> 8) & 255) * 587 + (c & 255) * 114) / 1000 : 0;
                lum[y * w + x] = l;
                rowY += l; rowA += op ? 1 : 0;
                rowS += op && faceSkin(c) ? 1 : 0;
                rowB += l > 175 ? 1 : 0;                 // whites of the eyes, highlights
                rowC += l > 125 ? 1 : 0;                 // the same in a dark or shaded face
                iy[(y + 1) * W1 + x + 1] = iy[y * W1 + x + 1] + rowY;
                ia[(y + 1) * W1 + x + 1] = ia[y * W1 + x + 1] + rowA;
                is[(y + 1) * W1 + x + 1] = is[y * W1 + x + 1] + rowS;
                ib[(y + 1) * W1 + x + 1] = ib[y * W1 + x + 1] + rowB;
                ic[(y + 1) * W1 + x + 1] = ic[y * W1 + x + 1] + rowC;
            }
        }
        // where to look: the upper part of the figure (people: top half; the face is usually higher)
        int top = (int) (r.headTop * h);
        // the head: a full figure's face is in its top third; a portrait's in its upper two thirds
        boolean full = h > w * 1.6f;
        int yMax = Math.min(h - 1, top + (int) ((h - top) * (full ? 0.3f : 0.62f)));
        int maxR = Math.max(3, (int) (w * 0.09f)), minR = Math.max(2, (int) (w * 0.018f));
        java.util.List<float[]> cands = new java.util.ArrayList<float[]>();     // x, y, r, score
        for (int rad = minR; rad <= maxR; rad = Math.max(rad + 1, (int) (rad * 1.18f))) {
            int in = Math.max(1, (int) (rad * 0.6f)), out = (int) (rad * 1.55f);
            int step = Math.max(1, rad / 3);
            for (int y = top + out; y < yMax - out; y += step) {
                for (int x = Math.max(out, rx0); x < Math.min(w - out, rx1); x += step) {
                    long aIn = box(ia, W1, x - in, y - in, x + in, y + in), aOut = box(ia, W1, x - out, y - out, x + out, y + out);
                    int nIn = (2 * in + 1) * (2 * in + 1), nOut = (2 * out + 1) * (2 * out + 1);
                    float mIn = box(iy, W1, x - in, y - in, x + in, y + in) / (float) nIn;
                    float mRing = (box(iy, W1, x - out, y - out, x + out, y + out) - box(iy, W1, x - in, y - in, x + in, y + in)) / (float) (nOut - nIn);
                    float sc = mRing - mIn;
                    if (aOut < nOut * 0.97f) continue;                 // the whole eye area must be inside the figure
                    // darker than its surroundings (relative, so dark skin and shaded eyes count too)
                    if (sc < Math.max(22, mRing * 0.33f) || mRing < 50) continue;
                    // an eye has some white (the eyeball, a catch-light) in or around it
                    if (box(mRing < 105 ? ic : ib, W1, x - out, y - out, x + out, y + out) < nOut * 0.04f) continue;
                    cands.add(new float[]{x, y, rad, sc * (float) Math.sqrt(rad / (double) minR)});
                }
            }
        }
        if (cands.size() < 2) return;
        java.util.Collections.sort(cands, new java.util.Comparator<float[]>() {
            public int compare(float[] a, float[] b) { return Float.compare(b[3], a[3]); }
        });
        // keep the strongest, at least an eye apart from each other
        java.util.List<float[]> peaks = new java.util.ArrayList<float[]>();
        for (float[] c : cands) {
            boolean near = false;
            // (each size on its own: a small iris inside a large dark socket is still a candidate of its own size)
            for (float[] p : peaks) if (Math.max(c[2], p[2]) < Math.min(c[2], p[2]) * 1.3f && Math.hypot(c[0] - p[0], c[1] - p[1]) < Math.max(c[2], p[2]) * 1.6f) { near = true; break; }
            if (!near) peaks.add(c);
            if (peaks.size() >= 400) break;
        }
        float bestS = -1e9f;
        float[] bl = null, br = null;
        java.util.List<float[]> good = new java.util.ArrayList<float[]>();     // Lx, Ly, Rx, Ry, score, iL, iR
        for (int i = 0; i < peaks.size(); i++) for (int j = i + 1; j < peaks.size(); j++) {
            float[] a = peaks.get(i), b = peaks.get(j);
            float[] L = a[0] < b[0] ? a : b, R = a[0] < b[0] ? b : a;
            float dx = R[0] - L[0], dy = Math.abs(R[1] - L[1]);
            float rr = Math.max(L[2], R[2]);
            if (dx < rr * 2.6f || dx > w * 0.6f || dy > dx * 0.18f) continue;
            float ratio = L[2] / R[2];
            if (ratio < 0.6f || ratio > 1.65f) continue;
            float mid = (L[0] + R[0]) / 2;
            if (Math.abs(mid - rc) > rw * 0.3f) continue;
            // between the eyes: the bridge of the nose is brighter than the irises
            float bridge = box(iy, W1, (int) (mid - rr * 0.5f), (int) ((L[1] + R[1]) / 2 - rr * 0.5f), (int) (mid + rr * 0.5f), (int) ((L[1] + R[1]) / 2 + rr * 0.5f))
                    / (float) ((2 * (int) (rr * 0.5f) + 1) * (2 * (int) (rr * 0.5f) + 1));
            // a face: skin (or fur) on both cheeks below the eyes and between them
            float ey0 = (L[1] + R[1]) / 2;
            int ch = Math.max(2, (int) (dx * 0.18f));
            float cheekL = box(is, W1, (int) L[0] - ch, (int) (ey0 + dx * 0.35f), (int) L[0] + ch, (int) (ey0 + dx * 0.6f)) / (float) ((2 * ch + 1) * ((int) (dx * 0.25f) + 1));
            float cheekR = box(is, W1, (int) R[0] - ch, (int) (ey0 + dx * 0.35f), (int) R[0] + ch, (int) (ey0 + dx * 0.6f)) / (float) ((2 * ch + 1) * ((int) (dx * 0.25f) + 1));
            float between = box(is, W1, (int) (mid - ch), (int) (ey0 - ch), (int) (mid + ch), (int) (ey0 + ch)) / (float) ((2 * ch + 1) * (2 * ch + 1));
            if (Math.min(cheekL, cheekR) < 0.25f || between < 0.2f) continue;
            float sc = L[3] + R[3] - 0.6f * Math.abs(L[3] - R[3]) - dy * 0.8f + (bridge > 110 ? 12 : -25)
                    + 40 * Math.min(cheekL, cheekR) + 20 * between
                    - ((L[1] + R[1]) / 2 - top) / (float) h * 60;      // higher is more likely the face
            good.add(new float[]{L[0], L[1], R[0], R[1], sc, a == L ? i : j, a == L ? j : i});
            if (sc > bestS) { bestS = sc; bl = L; br = R; }
        }
        if (bl == null || bestS < 70) return;
        // eyebrows make a pair too: if a nearly as good pair sits just below this one, those are the eyes
        for (int pass = 0; pass < 2; pass++) {
            float bdx = br[0] - bl[0], bey = (bl[1] + br[1]) / 2, bmx = (bl[0] + br[0]) / 2;
            float[] lower = null;
            for (float[] g : good) {
                float gdx = g[2] - g[0], gey = (g[1] + g[3]) / 2, gmx = (g[0] + g[2]) / 2;
                if (g[4] < bestS * 0.6f || gey - bey < bdx * 0.15f || gey - bey > bdx * 0.55f) continue;
                if (Math.abs(gmx - bmx) > bdx * 0.25f || gdx < bdx * 0.7f || gdx > bdx * 1.4f) continue;
                if (lower == null || g[4] > lower[4]) lower = g;
            }
            if (lower == null) break;
            bl = peaks.get((int) lower[5]); br = peaks.get((int) lower[6]); bestS = lower[4];
        }
        float d = br[0] - bl[0], ey = (bl[1] + br[1]) / 2, mx = (bl[0] + br[0]) / 2;
        // the mouth: the strongest dark / red line in a band below the eyes
        int half = (int) (d * 0.38f);
        float bestM = -1e9f;
        int my = -1;
        // a moustache: a thick band of dark hair under the nose; the lips are just below it
        int mBot = -1, run = 0;
        for (int y = (int) (ey + d * 0.35f); y < Math.min(h - 1, (int) (ey + d * 1.3f)); y++) {
            int hairN = 0, n = 0;
            for (int x = (int) mx - half; x <= (int) mx + half; x++) {
                if (x < 0 || x >= w) continue;
                int c = r.px[y * w + x];
                n++;
                if ((c >>> 24) >= 200 && lum[y * w + x] < 70 && ((c >> 16) & 255) - ((c >> 8) & 255) < 30) hairN++;
            }
            if (n > 0 && hairN > n * 0.45f) { run++; if (run >= Math.max(2, (int) (d * 0.07f))) mBot = y; }
            else if (mBot > 0) break;
            else run = 0;
        }
        float lo = 0.5f, hi = 1.3f, expect = 0.82f;
        if (mBot > 0) { lo = (mBot - ey) / d - 0.05f; hi = lo + 0.4f; expect = lo + 0.13f; }
        for (int y = (int) (ey + d * lo); y < Math.min(h - 2, (int) (ey + d * hi)); y++) {
            float line = 0, around = 0;
            int nl = 0, na = 0;
            for (int x = (int) mx - half; x <= (int) mx + half; x++) {
                if (x < 0 || x >= w) continue;
                int c = r.px[y * w + x];
                if ((c >>> 24) < 200) continue;
                int red = ((c >> 16) & 255) - ((c >> 8) & 255);
                // a moustache or beard is dark but not red: it is hair, not the lips
                boolean hair = lum[y * w + x] < 70 && red < 25;
                line += (hair ? 60 : 255 - lum[y * w + x]) + Math.max(0, red) * 1.6f;
                nl++;
                int ya = Math.max(0, y - (int) (d * 0.14f)), yb = Math.min(h - 1, y + (int) (d * 0.14f));
                around += (255 - lum[ya * w + x]) + (255 - lum[yb * w + x]);
                na += 2;
            }
            if (nl < half) continue;
            float sc = line / nl - around / Math.max(1, na);
            // a mouth is usually about one eye-distance below the eyes
            sc -= Math.abs((y - ey) / d - expect) * 40;
            if (sc > bestM) { bestM = sc; my = y; }
        }
        r.faceFound = true;
        r.eyeLX = bl[0] / w; r.eyeRX = br[0] / w; r.eyeY = ey / h;
        r.eyeR = Math.max(bl[2], br[2]) * 0.9f / w;
        r.mouthX = mx / w;
        r.mouthY = (my > 0 && bestM > 8 ? my : ey + d * expect) / h;
        r.mouthW = d * 0.42f / w;
        // the face's skin from the cheeks (below and outside the eyes)
        long sr = 0, sg = 0, sb = 0, sn = 0;
        for (int y = (int) (ey + d * 0.3f); y < Math.min(h, (int) (ey + d * 0.55f)); y++)
            for (int x : new int[]{(int) (bl[0]), (int) (br[0])}) {
                if (x < 0 || x >= w) continue;
                int c = r.px[y * w + x];
                if ((c >>> 24) < 200) continue;
                sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; sn++;
            }
        if (sn > 3) {
            r.skin = 0xFF000000 | ((int) (sr / sn) << 16) | ((int) (sg / sn) << 8) | (int) (sb / sn);
            r.lip = Puppet.shade(Puppet.mix(r.skin, 0xFFB03A3A, 0.45f), 0.8f);
        }
    }

    /** Skin of a face in pictures and cartoons (light to dark brown skin, and the monkey's fur-free face). */
    static boolean faceSkin(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        // not red cloth (too little green) or gold (too little blue)
        return r > 80 && r > g && g >= b - 10 && r - b > 20 && r - b < 150 && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 18
                && g > r * 0.42f && b > r * 0.3f;
    }

    static long box(long[] ii, int W1, int x0, int y0, int x1, int y1) {
        x0 = Math.max(0, x0); y0 = Math.max(0, y0);
        x1 = Math.min(W1 - 2, x1);
        int hh = ii.length / W1 - 2;
        y1 = Math.min(hh, y1);
        if (x1 < x0 || y1 < y0) return 0;
        return ii[(y1 + 1) * W1 + x1 + 1] - ii[y0 * W1 + x1 + 1] - ii[(y1 + 1) * W1 + x0] + ii[y0 * W1 + x0];
    }

    public static int[] cutFull(int[] src, int w, int h) { return cutFull(src, w, h, false); }

    /**
     * The character cut out on the full canvas. A plain studio background is flood-filled away; a photo or a
     * picture with a busy scene behind the character (a palace, a garden) is separated by colour models of
     * "what is at the edges" against "what is in the middle", refined a few times — so the background never
     * travels with the character.
     */
    public static int[] cutFull(int[] src, int w, int h, boolean holes) {
        int[] px = src.clone();
        if (PicSense.plainBorder(px, w, h)) {
            removeBackground(px, w, h, holes);
            // if almost nothing was removed the "plain" border lied: separate it the busy way
            int opaque = 0;
            for (int c : px) if ((c >>> 24) > 128) opaque++;
            if (opaque < w * h * 0.8f) return px;
            px = src.clone();
        }
        return segmentBusy(px, w, h);
    }

    /**
     * v35: the outline cleaned of the old background. A cut-out's edge pixels are a mix of the character and what was
     * behind it — a white wall leaves a light halo round the hair (and pockets of it between fine strands), which a
     * film enlarges and sharpens. Every pixel of the outline (part-transparent, or opaque within three pixels of the
     * cut) is unmixed: the background's colour there (the removed pixels nearby, as they were) and the character's own
     * colour nearby (its opaque pixels further in, the least background-like counting most) tell how much of the pixel
     * is the character. The first ring takes the character's colour with that share as its alpha (a soft, clean edge);
     * further in, a pixel that is almost the background's own colour (a pocket between strands of hair) fades out the
     * same way, and any other loses the background's share of its colour. Where the character and the background are
     * alike in colour nothing is changed.
     */
    static void defringe(int[] px, int[] src, int w, int h) {
        int n = w * h;
        if (w < 8 || h < 8) return;
        byte[] gone = new byte[n];
        for (int i = 0; i < n; i++) gone[i] = (byte) ((px[i] >>> 24) < 128 ? 1 : 0);
        byte[] d1 = dilate3(gone, w, h), d2 = dilate3(d1, w, h), d3 = dilate3(d2, w, h);
        int[] out = px.clone();
        final int R = 5;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int i = y * w + x, c = px[i], al = c >>> 24;
                if (al == 0 || gone[i] == 1) continue;
                int ring = d1[i] == 1 ? 1 : d2[i] == 1 ? 2 : d3[i] == 1 ? 3 : 0;
                if (ring == 0 && al >= 250) continue;
                // the background here, as it was
                long br = 0, bg = 0, bb = 0; int bn = 0;
                int y0 = Math.max(0, y - R), y1 = Math.min(h - 1, y + R), x0 = Math.max(0, x - R), x1 = Math.min(w - 1, x + R);
                for (int yy = y0; yy <= y1; yy++) for (int xx = x0; xx <= x1; xx++) {
                    int j = yy * w + xx;
                    if (gone[j] == 0) continue;
                    int o = src[j];
                    br += (o >> 16) & 255; bg += (o >> 8) & 255; bb += o & 255; bn++;
                }
                if (bn == 0) continue;
                float Br = br / (float) bn, Bg = bg / (float) bn, Bb = bb / (float) bn;
                // the character's own colour nearby: opaque pixels past the first ring, weighted by how unlike the background they are
                float fr = 0, fg = 0, fb = 0, fw = 0;
                for (int pass = 0; pass < 2 && fw == 0; pass++) {
                    for (int yy = y0; yy <= y1; yy++) for (int xx = x0; xx <= x1; xx++) {
                        int j = yy * w + xx, o = px[j];
                        if ((o >>> 24) < 250 || (pass == 0 && d1[j] == 1)) continue;
                        float r = (o >> 16) & 255, g = (o >> 8) & 255, b = o & 255;
                        float wgt = (r - Br) * (r - Br) + (g - Bg) * (g - Bg) + (b - Bb) * (b - Bb) + 1;
                        fr += r * wgt; fg += g * wgt; fb += b * wgt; fw += wgt;
                    }
                }
                if (fw == 0) continue;
                fr /= fw; fg /= fw; fb /= fw;
                float dr = fr - Br, dg = fg - Bg, db = fb - Bb, l2 = dr * dr + dg * dg + db * db;
                if (l2 < 600) continue;                     // the character and the background are alike here
                float cr = (c >> 16) & 255, cg = (c >> 8) & 255, cb = c & 255;
                float a = ((cr - Br) * dr + (cg - Bg) * dg + (cb - Bb) * db) / l2;
                a = Math.max(0, Math.min(1, a));
                float nearB = (cr - Br) * (cr - Br) + (cg - Bg) * (cg - Bg) + (cb - Bb) * (cb - Bb);
                int nr, ng, nb, na = al;
                if (ring == 1 || al < 250) {
                    nr = Math.round(fr); ng = Math.round(fg); nb = Math.round(fb);
                    na = Math.min(al, Math.round(255 * Math.min(1f, a * 1.15f)));
                } else if (a < 0.4f && nearB < 40 * 40) {
                    // a pocket of the old background between strands: it fades out
                    nr = Math.round(fr); ng = Math.round(fg); nb = Math.round(fb);
                    na = Math.min(al, Math.round(255 * a / 0.4f));
                } else {
                    float k = (1 - a) * (ring == 2 ? 1f : 0.6f);
                    nr = Math.round(cr + (fr - cr) * k); ng = Math.round(cg + (fg - cg) * k); nb = Math.round(cb + (fb - cb) * k);
                }
                out[i] = (Math.max(0, Math.min(255, na)) << 24) | (Math.max(0, Math.min(255, nr)) << 16) | (Math.max(0, Math.min(255, ng)) << 8) | Math.max(0, Math.min(255, nb));
            }
        }
        pockets(out, src, w, h, d3);
        System.arraycopy(out, 0, px, 0, n);
    }

    /**
     * v35: on a plain background, the small pockets of it caught inside the outline — inside the loop of a curl, between
     * strands of hair, under an arm — are let through: an opaque patch of the background's own colour that reaches
     * within four pixels of the cut and is small (at most 0.6% of the figure) fades out with its likeness to the
     * background. A patch far from the outline (the white of an eye, teeth, a white shirt) is never touched.
     */
    static void pockets(int[] px, int[] src, int w, int h, byte[] d3) {
        int n = w * h;
        // the background's colour: the picture's border, when it is plain
        long br = 0, bg = 0, bb = 0; int bn = 0;
        for (int x = 0; x < w; x++) for (int y : new int[]{0, 1, h - 2, h - 1}) { int c = src[y * w + x]; br += (c >> 16) & 255; bg += (c >> 8) & 255; bb += c & 255; bn++; }
        for (int y = 0; y < h; y++) for (int x : new int[]{0, 1, w - 2, w - 1}) { int c = src[y * w + x]; br += (c >> 16) & 255; bg += (c >> 8) & 255; bb += c & 255; bn++; }
        float Br = br / (float) bn, Bg = bg / (float) bn, Bb = bb / (float) bn;
        float spread = 0;
        for (int x = 0; x < w; x += 3) { int c = src[x]; spread += Math.abs(((c >> 16) & 255) - Br) + Math.abs(((c >> 8) & 255) - Bg) + Math.abs((c & 255) - Bb); }
        if (spread / Math.max(1, (w + 2) / 3) > 24) return;          // not a plain background
        byte[] d4 = dilate3(d3, w, h);
        int opaque = 0;
        byte[] like = new byte[n];
        for (int i = 0; i < n; i++) {
            int c = px[i];
            if ((c >>> 24) < 128) continue;
            opaque++;
            float dr = ((c >> 16) & 255) - Br, dg = ((c >> 8) & 255) - Bg, db = (c & 255) - Bb;
            if (dr * dr + dg * dg + db * db < 34 * 34) like[i] = 1;
        }
        int maxArea = Math.max(400, Math.round(opaque * 0.006f));
        int[] stack = new int[n];
        int[] comp = new int[maxArea + 1];
        byte[] seen = new byte[n];
        for (int i0 = 0; i0 < n; i0++) {
            if (like[i0] == 0 || seen[i0] == 1) continue;
            int sp = 0, area = 0;
            boolean nearCut = false, tooBig = false;
            stack[sp++] = i0; seen[i0] = 1;
            while (sp > 0) {
                int i = stack[--sp];
                if (area < comp.length) comp[area] = i;
                area++;
                if (area > maxArea) tooBig = true;
                if (d4[i] == 1) nearCut = true;
                int x = i % w, y = i / w;
                if (x > 0 && like[i - 1] == 1 && seen[i - 1] == 0) { seen[i - 1] = 1; stack[sp++] = i - 1; }
                if (x < w - 1 && like[i + 1] == 1 && seen[i + 1] == 0) { seen[i + 1] = 1; stack[sp++] = i + 1; }
                if (y > 0 && like[i - w] == 1 && seen[i - w] == 0) { seen[i - w] = 1; stack[sp++] = i - w; }
                if (y < h - 1 && like[i + w] == 1 && seen[i + w] == 0) { seen[i + w] = 1; stack[sp++] = i + w; }
            }
            if (tooBig || !nearCut) continue;
            for (int k = 0; k < area; k++) {
                int i = comp[k], c = px[i];
                float dr = ((c >> 16) & 255) - Br, dg = ((c >> 8) & 255) - Bg, db = (c & 255) - Bb;
                float d = (float) Math.sqrt(dr * dr + dg * dg + db * db);
                float keep = Math.max(0, Math.min(1, (d - 16) / 18f));
                px[i] = (Math.round((c >>> 24) * keep) << 24) | (c & 0xFFFFFF);
            }
        }
    }

    /** A mask grown by one pixel in every direction (3 x 3). */
    static byte[] dilate3(byte[] m, int w, int h) {
        byte[] t = new byte[m.length], o = new byte[m.length];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int i = y * w + x;
            t[i] = (byte) (m[i] == 1 || (x > 0 && m[i - 1] == 1) || (x < w - 1 && m[i + 1] == 1) ? 1 : 0);
        }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int i = y * w + x;
            o[i] = (byte) (t[i] == 1 || (y > 0 && t[i - w] == 1) || (y < h - 1 && t[i + w] == 1) ? 1 : 0);
        }
        return o;
    }

    /** Colour key with 5 bits per channel. */
    static int key5(int c) { return ((c >> 19) & 31) << 10 | ((c >> 11) & 31) << 5 | ((c >> 3) & 31); }

    /**
     * Separates a character from a busy background. The outer edge of the picture is background, the middle
     * (shaped like a standing figure) is probably the character; colour histograms of both are learnt, every
     * pixel is judged, the judgement is smoothed, and the models are learnt again from the new split (5 rounds).
     * Background seen through gaps (between arm and body) is removed when it is clearly background-coloured.
     */
    public static int[] segmentBusy(int[] px, int w, int h) {
        int n = w * h;
        float[] prior = new float[n];
        int band = Math.max(2, Math.min(w, h) / 30);
        for (int y = 0; y < h; y++) {
            float fy = y / (float) h;
            float half = fy < 0.06f ? 0.1f : fy < 0.35f ? 0.16f + (fy - 0.06f) * 0.35f : 0.26f + (fy - 0.35f) * 0.18f;
            for (int x = 0; x < w; x++) {
                float d = Math.abs(x - w / 2f) / w;
                boolean edge = x < band || y < band || x >= w - band || y >= h - band;
                prior[y * w + x] = edge ? 0.01f : d < half ? 0.8f : d < half + 0.1f ? 0.45f : 0.15f;
            }
        }
        int[] key = new int[n];
        for (int i = 0; i < n; i++) key[i] = key5(px[i]);
        byte[] fg = new byte[n];
        for (int i = 0; i < n; i++) fg[i] = (byte) (prior[i] > 0.6f ? 1 : 0);
        float[] fh = new float[32768], bh = new float[32768];
        for (int iter = 0; iter < 5; iter++) {
            java.util.Arrays.fill(fh, 0.05f);
            java.util.Arrays.fill(bh, 0.05f);
            float fs = 1638, bs = 1638;
            for (int i = 0; i < n; i++) {
                if (iter == 0) {
                    // the first models come only from sure places: the middle of the body, and the picture's
                    // edges and outer sides (the floor below the feet is not taken for the character)
                    int x = i % w, y = i / w;
                    float fx = Math.abs(x - w / 2f) / w, fy = y / (float) h;
                    if (fx < 0.08f && fy > 0.15f && fy < 0.7f) { fh[key[i]] += 1; fs += 1; }
                    else if (prior[i] < 0.05f || fx > 0.42f) { bh[key[i]] += 1; bs += 1; }
                    continue;
                }
                if (fg[i] == 1) { fh[key[i]] += 1; fs += 1; } else { bh[key[i]] += 1; bs += 1; }
            }
            // blur the histograms a little (neighbouring colours behave alike)
            fh = blurHist(fh); bh = blurHist(bh);
            float pw = iter == 0 ? 0.6f : 0.35f;      // the shape guide counts less as the colours are learnt
            for (int i = 0; i < n; i++) {
                float pf = fh[key[i]] / fs, pb = bh[key[i]] / bs;
                double lo = Math.log(pf / pb) + pw * 2.2 * Math.log(prior[i] / (1 - prior[i]));
                fg[i] = (byte) (lo > 0 ? 1 : 0);
            }
            fg = PicSense.majority(fg, w, h);
            fg = PicSense.majority(fg, w, h);
            fg = PicSense.largestCentral(fg, w, h);
            PicSense.fillHoles(fg, w, h);
        }
        // ground and scenery touching the edge of the picture: everything reachable from the edge through
        // clearly background-coloured pixels is background (the floor around the feet, a wall behind a shoulder)
        {
            byte[] bgReach = new byte[n];
            int[] q = new int[n];
            int qh = 0, qt = 0;
            for (int x = 0; x < w; x++) { q[qt++] = x; q[qt++] = (h - 1) * w + x; }
            for (int y = 1; y < h - 1; y++) { q[qt++] = y * w; q[qt++] = y * w + w - 1; }
            for (int i = 0; i < qt; i++) bgReach[q[i]] = 1;
            while (qh < qt) {
                int i = q[qh++];
                int x = i % w, y = i / w;
                int[] nb = {x > 0 ? i - 1 : -1, x < w - 1 ? i + 1 : -1, y > 0 ? i - w : -1, y < h - 1 ? i + w : -1};
                for (int j : nb) {
                    if (j < 0 || bgReach[j] != 0) continue;
                    if (fg[j] == 0 || bh[key[j]] > fh[key[j]] * 2.5f) { bgReach[j] = 1; q[qt++] = j; }
                }
            }
            for (int i = 0; i < n; i++) if (bgReach[i] == 1) fg[i] = 0;
            fg = PicSense.majority(fg, w, h);
            fg = PicSense.largestCentral(fg, w, h);
        }
        // cut thin bridges to patches of scenery: shrink, keep the main body, grow back (morphological opening)
        {
            int r = Math.max(2, Math.min(w, h) / 90);
            byte[] er = erode(fg, w, h, r);
            er = PicSense.largestCentral(er, w, h);
            byte[] di = dilate(er, w, h, r + 1);
            for (int i = 0; i < n; i++) if (di[i] == 0) fg[i] = 0;
            fg = PicSense.largestCentral(fg, w, h);
        }
        // gaps inside the outline that are clearly background colour (between the arm and the body)
        for (int i = 0; i < n; i++) {
            if (fg[i] == 0) continue;
            float pf = fh[key[i]], pb = bh[key[i]];
            if (pb > pf * 6 && prior[i] < 0.7f) fg[i] = 2;      // candidate
        }
        boolean[] seen = new boolean[n];
        int[] stack = new int[n];
        for (int s0 = 0; s0 < n; s0++) {
            if (fg[s0] != 2 || seen[s0]) continue;
            int sp = 0, cnt = 0;
            stack[sp++] = s0; seen[s0] = true;
            java.util.List<Integer> comp = new java.util.ArrayList<Integer>();
            while (sp > 0) {
                int i = stack[--sp]; comp.add(i); cnt++;
                int x = i % w, y = i / w;
                int[] nb = {x > 0 ? i - 1 : -1, x < w - 1 ? i + 1 : -1, y > 0 ? i - w : -1, y < h - 1 ? i + w : -1};
                for (int j : nb) if (j >= 0 && !seen[j] && fg[j] == 2) { seen[j] = true; stack[sp++] = j; }
            }
            byte to = cnt > n / 600 ? (byte) 0 : (byte) 1;
            for (int i : comp) fg[i] = to;
        }
        // soft edge: a 2-pixel feather where the character meets the removed background
        int[] out = new int[n];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int i = y * w + x;
            if (fg[i] == 0) { out[i] = px[i] & 0x00FFFFFF; continue; }
            int near = 0;
            for (int dy = -2; dy <= 2; dy++) for (int dx = -2; dx <= 2; dx++) {
                int xx = x + dx, yy = y + dy;
                if (xx < 0 || yy < 0 || xx >= w || yy >= h || fg[yy * w + xx] == 0) near++;
            }
            int a = near == 0 ? 255 : Math.max(0, 255 - near * 14);
            out[i] = (a << 24) | (px[i] & 0xFFFFFF);
        }
        return out;
    }

    /** Square erosion / dilation with radius r (separable, so fast on big pictures). */
    static byte[] erode(byte[] m, int w, int h, int r) { return morph(m, w, h, r, true); }
    static byte[] dilate(byte[] m, int w, int h, int r) { return morph(m, w, h, r, false); }

    static byte[] morph(byte[] m, int w, int h, int r, boolean erode) {
        byte[] t = new byte[m.length], o = new byte[m.length];
        for (int y = 0; y < h; y++) {
            int run = 0;   // sliding count of set pixels in the window
            for (int x = -r; x < w; x++) {
                int add = x + r, rem = x - r - 1;
                if (add < w && m[y * w + add] != 0) run++;
                if (rem >= 0 && m[y * w + rem] != 0) run--;
                if (x < 0) continue;
                int span = Math.min(w - 1, x + r) - Math.max(0, x - r) + 1;
                t[y * w + x] = (byte) (erode ? (run == span ? 1 : 0) : (run > 0 ? 1 : 0));
            }
        }
        for (int x = 0; x < w; x++) {
            int run = 0;
            for (int y = -r; y < h; y++) {
                int add = y + r, rem = y - r - 1;
                if (add < h && t[add * w + x] != 0) run++;
                if (rem >= 0 && t[rem * w + x] != 0) run--;
                if (y < 0) continue;
                int span = Math.min(h - 1, y + r) - Math.max(0, y - r) + 1;
                o[y * w + x] = (byte) (erode ? (run == span ? 1 : 0) : (run > 0 ? 1 : 0));
            }
        }
        return o;
    }

    static float[] blurHist(float[] hst) {
        float[] o = new float[hst.length];
        for (int r = 0; r < 32; r++) for (int g = 0; g < 32; g++) for (int b = 0; b < 32; b++) {
            int i = r << 10 | g << 5 | b;
            float s = hst[i] * 2;
            int c = 2;
            if (r > 0) { s += hst[i - 1024]; c++; } if (r < 31) { s += hst[i + 1024]; c++; }
            if (g > 0) { s += hst[i - 32]; c++; } if (g < 31) { s += hst[i + 32]; c++; }
            if (b > 0) { s += hst[i - 1]; c++; } if (b < 31) { s += hst[i + 1]; c++; }
            o[i] = s / c * 2.2f;
        }
        return o;
    }

    /** Flood-fills the background colour from the borders and makes it transparent with soft edges. */
    public static void removeBackground(int[] px, int w, int h) {
        removeBackground(px, w, h, false);
    }

    public static void removeBackground(int[] px, int w, int h, boolean holes) {
        // background colour = median-ish of border samples
        long sr = 0, sg = 0, sb = 0; int n = 0;
        for (int x = 0; x < w; x += Math.max(1, w / 64)) {
            int[] ys = {0, 1, h - 2, h - 1};
            for (int y : ys) { int c = px[y * w + x]; sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; n++; }
        }
        for (int y = 0; y < h; y += Math.max(1, h / 64)) {
            int[] xs = {0, 1, w - 2, w - 1};
            for (int x : xs) { int c = px[y * w + x]; sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; n++; }
        }
        int bg = 0xFF000000 | ((int) (sr / n) << 16) | ((int) (sg / n) << 8) | (int) (sb / n);
        removeBackground(px, w, h, holes, bg);
    }

    /** The same with a known background colour (v26: a piece cut from a sheet whose border may touch a neighbour). */
    public static void removeBackground(int[] px, int w, int h, boolean holes, int bg) {
        final int TOL = 60, SOFT = 120;
        byte[] state = new byte[w * h]; // 0 unknown, 1 background, 2 queued
        int[] queue = new int[w * h];
        int qh = 0, qt = 0;
        for (int x = 0; x < w; x++) { queue[qt++] = x; queue[qt++] = (h - 1) * w + x; }
        for (int y = 1; y < h - 1; y++) { queue[qt++] = y * w; queue[qt++] = y * w + w - 1; }
        for (int i = 0; i < qt; i++) state[queue[i]] = 2;
        int shadowRow = (int) (h * 0.9f);
        while (qh < qt) {
            int i = queue[qh++];
            if (dist(px[i], bg) > TOL && !(i / w >= shadowRow && isShadow(px[i]))) { state[i] = 0; continue; }
            state[i] = 1;
            int x = i % w, y = i / w;
            if (x > 0 && state[i - 1] == 0) { state[i - 1] = 2; queue[qt++] = i - 1; }
            if (x < w - 1 && state[i + 1] == 0) { state[i + 1] = 2; queue[qt++] = i + 1; }
            if (y > 0 && state[i - w] == 0) { state[i - w] = 2; queue[qt++] = i - w; }
            if (y < h - 1 && state[i + w] == 0) { state[i + w] = 2; queue[qt++] = i + w; }
            if (qt >= queue.length - 4) qt = queue.length - 4; // safety (cannot overflow in practice)
        }
        // closed gaps of plain background colour (the page seen between legs that stand on their shadow)
        if (holes) {
            int min = Math.max(30, w * h / 900);
            int[] comp = new int[w * h];
            for (int s0 = 0; s0 < px.length; s0++) {
                if (state[s0] != 0 || dist(px[s0], bg) > TOL * 0.6f) continue;
                int n0 = 0;
                comp[n0++] = s0;
                state[s0] = 3;
                for (int k = 0; k < n0; k++) {
                    int i = comp[k], x = i % w, y = i / w;
                    int[] nb = {x > 0 ? i - 1 : -1, x < w - 1 ? i + 1 : -1, y > 0 ? i - w : -1, y < h - 1 ? i + w : -1};
                    for (int j : nb) if (j >= 0 && state[j] == 0 && dist(px[j], bg) <= TOL * 0.6f) { state[j] = 3; comp[n0++] = j; }
                }
                byte to = n0 >= min ? (byte) 1 : (byte) 4;
                for (int k = 0; k < n0; k++) state[comp[k]] = to;
            }
            for (int i = 0; i < state.length; i++) if (state[i] == 4) state[i] = 0;
        }
        // soft shadows near the floor and anti-aliased edges
        for (int i = 0; i < px.length; i++) {
            if (state[i] == 1) { px[i] = 0; continue; }
            int x = i % w, y = i / w;
            boolean edge = (x > 0 && state[i - 1] == 1) || (x < w - 1 && state[i + 1] == 1)
                    || (y > 0 && state[i - w] == 1) || (y < h - 1 && state[i + w] == 1);
            if (edge) {
                int d = dist(px[i], bg);
                if (d < SOFT) {
                    int a = (int) (255f * (d - TOL * 0.5f) / (SOFT - TOL * 0.5f));
                    a = Math.max(0, Math.min(255, a));
                    px[i] = (a << 24) | (px[i] & 0xFFFFFF);
                }
            }
        }
    }

    /** Soft grey floor shadow under a character on a white background. */
    static boolean isShadow(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
        return mx - mn < 22 && mx > 120;
    }

    public static boolean isSkin(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        if ((c >>> 24) < 200) return false;
        return r > 95 && g > 50 && b > 25 && r > g && g >= b - 5 && (r - b) > 30 && (r - g) < 95 && (r - g) > 8 && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) > 25;
    }

    /** Heuristic face finder for front-facing cartoon characters (the user can correct it in the app). */
    static void findFace(Result r) {
        int w = r.w, h = r.h;
        int top = 0;
        outer:
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if ((r.px[y * w + x] >>> 24) > 128) { top = y; break outer; }
        r.headTop = top / (float) h;
        int scanH = (int) (h * 0.42f);
        // per-row skin counts in the upper part, restricted to central 70%
        int x0 = (int) (w * 0.15f), x1 = (int) (w * 0.85f);
        int[] rowCount = new int[scanH];
        long sx = 0, cnt = 0;
        for (int y = top; y < Math.min(h, top + scanH); y++) {
            int c = 0;
            for (int x = x0; x < x1; x++) if (isSkin(r.px[y * w + x])) { c++; sx += x; }
            rowCount[y - top] = c;
            cnt += c;
        }
        if (cnt < 50) return;
        // face = the biggest face-shaped patch of skin in the upper body (hands are small, chest/arms are lower)
        int sh = Math.min(h - top, (int) (h * 0.5f));
        int[] lab = new int[w * sh];
        int[] queue = new int[w * sh];
        int best = -1, bestA = 0, bx0 = 0, bx1 = 0, by0 = 0, by1 = 0, cur = 0;
        for (int y = 0; y < sh; y++) for (int x = x0; x < x1; x++) {
            int i = y * w + x;
            if (lab[i] != 0 || !isSkin(r.px[(y + top) * w + x])) continue;
            cur++;
            int qh = 0, qt = 0, area = 0, mnx = x, mxx = x, mny = y, mxy = y;
            queue[qt++] = i;
            lab[i] = cur;
            while (qh < qt) {
                int j = queue[qh++];
                area++;
                int jx = j % w, jy = j / w;
                if (jx < mnx) mnx = jx; if (jx > mxx) mxx = jx; if (jy < mny) mny = jy; if (jy > mxy) mxy = jy;
                int[] nb = {jx > x0 ? j - 1 : -1, jx < x1 - 1 ? j + 1 : -1, jy > 0 ? j - w : -1, jy < sh - 1 ? j + w : -1};
                for (int k : nb) {
                    if (k < 0 || lab[k] != 0 || !isSkin(r.px[(k / w + top) * w + k % w])) continue;
                    lab[k] = cur;
                    queue[qt++] = k;
                }
            }
            int bw = mxx - mnx + 1, bh = mxy - mny + 1;
            // face-shaped: not a thin strip, not much wider than tall; higher up is more likely the face
            float shape = bh > bw * 0.55f && bw > w * 0.08f ? 1f : 0.25f;
            float high = 1.2f - mny / (float) sh * 0.6f;
            int score = (int) (area * shape * high);
            if (score > bestA) { bestA = score; best = cur; bx0 = mnx; bx1 = mxx; by0 = mny; by1 = mxy; }
        }
        if (best < 0 || bestA < 30) return;
        // the patch may include the neck: a face is about as tall as 1.25 x its width
        int faceWpx = bx1 - bx0 + 1;
        int fTop = by0, fBot = Math.min(by1, by0 + (int) (faceWpx * 0.95f));
        for (int i = 0; i < scanH && i < rowCount.length; i++) rowCount[i] = 0;
        long fx = 0, fc = 0; int minX = w, maxX = 0;
        long sr = 0, sg = 0, sb = 0;
        for (int y = top + fTop; y <= top + fBot; y++) {
            for (int x = x0; x < x1; x++) {
                if (lab[(y - top) * w + x] != best) continue;
                int c = r.px[y * w + x];
                fx += x; fc++;
                if (x < minX) minX = x;
                if (x > maxX) maxX = x;
                sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255;
            }
        }
        if (fc < 20) return;
        float cx = fx / (float) fc;
        float faceW = Math.min(maxX - minX, w * 0.6f);
        float faceTopPx = top + fTop, chinPx = top + fBot;
        float faceH = chinPx - faceTopPx;
        if (faceH < h * 0.04f) return;
        r.faceFound = true;
        r.skin = 0xFF000000 | ((int) (sr / fc) << 16) | ((int) (sg / fc) << 8) | (int) (sb / fc);
        r.lip = Puppet.shade(Puppet.mix(r.skin, 0xFFB03A3A, 0.45f), 0.8f);
        r.faceTop = faceTopPx / h;
        r.chinY = chinPx / h;
        r.mouthX = cx / w;
        r.mouthY = (faceTopPx + faceH * 0.78f) / h;
        r.mouthW = faceW * 0.26f / w;
        r.eyeY = (faceTopPx + faceH * 0.47f) / h;
        r.eyeLX = (cx - faceW * 0.2f) / w;
        r.eyeRX = (cx + faceW * 0.2f) / w;
        r.eyeR = faceW * 0.1f / w;
    }
}
