package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;

/**
 * A character in three dimensions built from the user's own picture, so the director can show that character
 * from any side — three-quarter, side, back — on the same line as the picture that was uploaded or chosen:
 * the same colours, the same costume, the same height and build.
 *
 * The model: every row of the cut-out picture is split into its opaque runs (the head, the body, an arm beside
 * the body, each leg). Each run becomes a ring of an elliptical tube — as wide as the run, as deep as its part
 * allows (a head is round, a body flatter, arms and legs round, a skirt in between) — and the rings of
 * neighbouring rows are joined, so the picture's own outline becomes a closed figure. The figure is coloured by
 * the picture itself: the front half shows the picture, the back half shows the picture mirrored (the costume
 * goes round), and the back of the head shows the top of the head (hair, a turban, a bald crown), never the
 * face. The picture keeps most of its own light and shade.
 *
 * This is a geometric and photometric model, computed on the phone from one picture; it is not a learned
 * network and cannot invent what the picture does not show (a pattern only on the back, a tail hidden behind).
 */
public final class Figure3D {
    private Figure3D() {}

    /** Which part of the figure a run belongs to. */
    static final int P_BODY = 0, P_ARM_L = 1, P_ARM_R = 2, P_LEG_L = 3, P_LEG_R = 4;

    /** The figure built from a picture: the picture, its runs and the rows that matter. */
    public static final class Model {
        public int[] px;
        public int w, h;
        /** The figure's middle (fraction of the width) and the rows of its parts (pixels). */
        public float cx;
        public int topRow, botRow, hairRow, eyeRow, chinRow, hipRow, armEndRow;
        /** The band of rows above the hairline that the back of the head is painted with (its height, pixels). */
        public int bandH;
        public boolean faceKnown, legs, animal;
        public float eyeLX, eyeLY, eyeRX, eyeRY, eyeR, mouthX, mouthY, mouthHW, turbanY;
        /** Half the face width (pixels). */
        public float headHalf;
        /** Per row: the opaque runs as {a0, b0, a1, b1, …} (pixels, inclusive), or null. */
        int[][] runs;
        /** Per row and run: the part and the depth factor (how deep the tube is for its width). */
        byte[][] part;
        float[][] depth;
        /**
         * Per row and run: the round core {ca, cb} of the run (pixels). What the run has beyond its core — hair
         * beside the head, braids beside the body, a ribbon, a tail — is thin, like paper, not a tube.
         */
        int[][] core;
        public int runs() { int n = 0; for (int[] r : runs) if (r != null) n += r.length / 2; return n; }
    }

    // ------------------------------------------------------------------ building the model

    /**
     * Builds the figure from a cut-out picture (ARGB, transparent outside the character). face: the manifest's
     * face points {mouthX, mouthY, mouthHW, eyeLX, eyeLY, eyeRX, eyeRY, eyeR, turbanY} as fractions, or null
     * when the face is unknown. look: the character's look (an animal's side-on picture is handled as one).
     */
    public static Model build(int[] px, int w, int h, float[] face, Look look) {
        Model m = new Model();
        m.px = defringe(px, w, h); m.w = w; m.h = h;
        px = m.px;
        m.animal = look != null && (look.kind == Look.ANIMAL || look.kind == Look.BIRD) && h < w * 1.25f;
        int minGap = Math.max(2, w / 120);
        m.runs = new int[h][];
        m.topRow = -1; m.botRow = -1;
        for (int y = 0; y < h; y++) {
            int[] r = runsOf(px, w, y, minGap);
            m.runs[y] = r;
            if (r != null) { if (m.topRow < 0) m.topRow = y; m.botRow = y; }
        }
        if (m.topRow < 0 || m.botRow - m.topRow < 20) throw new IllegalArgumentException("The picture has no figure in it");
        smoothRuns(m);
        // ---- the rows of the parts, as the rig finds them
        float body;
        if (face != null && face[0] > 0 && face[1] > (face[4] + face[6]) / 2) {
            m.faceKnown = true;
            m.mouthX = face[0]; m.mouthY = face[1]; m.mouthHW = face[2];
            m.eyeLX = face[3]; m.eyeLY = face[4]; m.eyeRX = face[5]; m.eyeRY = face[6]; m.eyeR = face[7];
            m.turbanY = face.length > 8 ? face[8] : 0;
            float eyeY = (m.eyeLY + m.eyeRY) / 2;
            m.cx = (m.eyeLX + m.eyeRX + 2 * m.mouthX) / 4;
            m.eyeRow = Math.round(eyeY * h);
            float eyeDist = Math.max(0.02f, Math.abs(m.eyeRX - m.eyeLX)) * w;
            m.headHalf = 1.3f * eyeDist;
            // the face itself: the span of skin at the eye rows (ribbons and hair beside it are not the skull)
            int[] face0 = null;
            for (int dy = -2; dy <= 2 && face0 == null; dy++) {
                int yy = Math.max(0, Math.min(h - 1, m.eyeRow + dy * Math.max(1, (int) (eyeDist * 0.1f))));
                int[] er = m.runs[yy];
                if (er != null) face0 = skinSpan(px, w, yy, er[0], er[er.length - 1]);
                if (face0 != null && face0[1] - face0[0] < eyeDist * 1.2f) face0 = null;
            }
            m.headHalf = face0 != null ? (face0[1] - face0[0]) / 2f : eyeDist * 1.0f;
            m.hairRow = Math.max(m.topRow, Math.round(m.eyeRow - 1.15f * eyeDist));
            m.chinRow = Math.min(m.botRow, Math.round(Math.min(0.6f, m.mouthY + (m.mouthY - eyeY) * 0.95f) * h));
        } else {
            m.faceKnown = false;
            m.cx = 0.5f;
            m.eyeRow = Math.round(m.topRow + 0.1f * (m.botRow - m.topRow));
            m.hairRow = m.topRow;
            m.chinRow = Math.round(m.topRow + 0.22f * (m.botRow - m.topRow));
            int[] r = m.runs[Math.min(m.botRow, m.eyeRow)];
            m.headHalf = r != null ? (r[1] - r[0]) * 0.5f : w * 0.12f;
        }
        body = m.botRow - m.chinRow;
        // hips: about 42 % down the body, moved to the narrowest middle part nearby
        int hip = Math.round(m.chinRow + 0.42f * body), best = -1;
        float bestW = 1e9f;
        int cxPx = Math.round(m.cx * w);
        for (int y = Math.round(hip - 0.08f * body); y <= Math.round(hip + 0.06f * body); y++) {
            if (y < m.topRow || y > m.botRow || m.runs[y] == null) continue;
            int[] run = runAt(m.runs[y], cxPx);
            if (run == null) continue;
            float rw = run[1] - run[0];
            if (rw < bestW) { bestW = rw; best = y; }
        }
        m.hipRow = best > 0 ? best : hip;
        m.armEndRow = Math.round(m.chinRow + 0.62f * body);
        // legs: two separate runs below the hips for a good part of the way down
        int two = 0, rows = 0;
        for (int y = Math.round(m.hipRow + 0.12f * body); y < m.botRow - 0.03f * h; y += 2) {
            if (y < 0 || y >= h || m.runs[y] == null) continue;
            rows++;
            if (twoLegs(m.runs[y], minGap) != null) two++;
        }
        m.legs = rows > 0 && two > rows * 0.35f;
        if (m.animal) { m.legs = false; m.hairRow = m.topRow; m.chinRow = m.topRow; m.hipRow = m.topRow + (m.botRow - m.topRow) / 2; }
        // the band the back of the head is painted with: the hair above the hairline, extended below it while the
        // rows there hold no skin (a furry forehead, a turban's rim), so it is never a few rows stretched into stripes
        m.bandH = Math.max(3, Math.round(0.45f * (m.hairRow - m.topRow)));
        int want = Math.round(0.22f * Math.max(1, m.chinRow - m.topRow));
        for (int y = m.hairRow; y < m.chinRow && m.bandH < want; y++) {
            int[] r = m.runs[y];
            if (r == null) break;
            int[] core = runAt(r, cxPx);
            int skin = 0, n = 0;
            for (int x = core[0]; x <= core[1]; x += 2) { n++; if (Cutout.isSkin(px[y * w + x])) skin++; }
            if (n == 0 || skin > n * 0.3f) break;
            m.hairRow = y + 1;
            m.bandH++;
        }
        // ---- every run gets its part, its depth and its round core
        m.part = new byte[h][];
        m.depth = new float[h][];
        m.core = new int[h][];
        for (int y = 0; y < h; y++) {
            int[] r = m.runs[y];
            if (r == null) continue;
            int n = r.length / 2;
            m.part[y] = new byte[n];
            m.depth[y] = new float[n];
            m.core[y] = new int[n * 2];
            int main = mainRun(r, cxPx);
            int[] legs = m.legs && y > m.hipRow + 0.12f * body ? twoLegs(r, minGap) : null;
            for (int i = 0; i < n; i++) {
                int a = r[i * 2], b = r[i * 2 + 1];
                byte p = P_BODY;
                float f;
                if (legs != null && (a == legs[0] || a == legs[2])) p = a == legs[0] ? (byte) P_LEG_L : (byte) P_LEG_R;
                else if (i != main) p = (a + b) / 2 < cxPx ? (byte) P_ARM_L : (byte) P_ARM_R;
                if (m.animal) f = p == P_BODY ? 0.6f : 0.85f;
                else if (p == P_LEG_L || p == P_LEG_R) f = 0.9f;
                else if (p == P_ARM_L || p == P_ARM_R) f = 0.9f;
                else if (y < m.chinRow) f = 0.92f;                                   // the head is round
                else if (y < m.chinRow + 0.06f * body) f = 0.9f;                     // the neck
                else if (y < m.hipRow) f = 0.55f;                                    // the body is flatter
                else if (m.legs) f = 0.75f;                                          // the legs together
                else f = 0.62f;                                                      // a skirt, a robe, a dress
                m.part[y][i] = p;
                m.depth[y][i] = f;
                int ca = a, cb = b;
                if (p == P_BODY && !m.animal && i == main) {
                    if (y < m.chinRow && m.faceKnown) {
                        // the skull: a sphere a quarter wider than the face, centred a little above the eyes, narrowing to
                        // the chin; hair, ribbons and ears beyond it are flat, tall headwear keeps most of its width round
                        float R = m.headHalf * 1.25f, half;
                        float runHalf = (b - a) / 2f;
                        if (y < m.eyeRow) {
                            float dy = (m.eyeRow - 0.25f * R) - y;
                            float sphere = dy <= 0 ? R : (float) Math.sqrt(Math.max(0, R * R - dy * dy));
                            half = Math.min(runHalf, Math.max(sphere, 0.75f * runHalf));
                        } else {
                            half = Math.min(runHalf, R * (1 - 0.3f * (y - m.eyeRow) / Math.max(1f, m.chinRow - m.eyeRow)));
                        }
                        int mid = Math.max(a, Math.min(b, Math.round(m.cx * w)));
                        ca = Math.max(a, Math.round(mid - half)); cb = Math.min(b, Math.round(mid + half));
                    } else if (y < m.hipRow + 0.1f * body) {
                        // hair hanging beside the body: the dark margins of the run are flat, the body inside is round
                        // (and the body is symmetric about the figure's middle)
                        int[] inner = brightCore(px, w, y, a, b);
                        int mid = Math.max(a, Math.min(b, Math.round(m.cx * w)));
                        if (inner != null) {
                            int half = Math.min(mid - inner[0], inner[1] - mid);
                            if (half * 2 >= 0.55f * (b - a)) { ca = Math.max(a, mid - half); cb = Math.min(b, mid + half); }
                        }
                    }
                }
                m.core[y][i * 2] = ca; m.core[y][i * 2 + 1] = cb;
            }
        }
        smoothCores(m);
        return m;
    }

    /**
     * Takes the halo of the old background out of the cut-out's soft edge: a half-transparent edge pixel is a mix
     * of the character and the background it was cut from, and the figure's sides would show that mix as a pale
     * streak. The background colour is read from the faintest edge pixels and un-mixed.
     */
    static int[] defringe(int[] px, int w, int h) {
        long br = 0, bg = 0, bb = 0, n = 0;
        for (int c : px) {
            int a = c >>> 24;
            if (a > 8 && a < 90) { br += c >> 16 & 255; bg += c >> 8 & 255; bb += c & 255; n++; }
        }
        if (n < 20) return px;
        float Br = br / (float) n, Bg = bg / (float) n, Bb = bb / (float) n;
        int[] out = px.clone();
        for (int i = 0; i < out.length; i++) {
            int c = out[i], a = c >>> 24;
            if (a < 90 || a >= 250) continue;
            float k = a / 255f;
            int r = Math.max(0, Math.min(255, Math.round(((c >> 16 & 255) - (1 - k) * Br) / k)));
            int g = Math.max(0, Math.min(255, Math.round(((c >> 8 & 255) - (1 - k) * Bg) / k)));
            int b = Math.max(0, Math.min(255, Math.round(((c & 255) - (1 - k) * Bb) / k)));
            out[i] = (a << 24) | (r << 16) | (g << 8) | b;
        }
        return out;
    }

    /** The span of skin in a run: {first, last} skin-coloured pixel, or null. */
    static int[] skinSpan(int[] px, int w, int y, int a, int b) {
        int row = y * w, x0 = -1, x1 = -1;
        for (int x = a; x <= b; x++) if (Cutout.isSkin(px[row + x])) { if (x0 < 0) x0 = x; x1 = x; }
        return x0 < 0 ? null : new int[]{x0, x1};
    }

    /** The part of a run between its dark (hair-like) margins: {first, last} non-dark pixel, or null. */
    static int[] brightCore(int[] px, int w, int y, int a, int b) {
        int row = y * w, x0 = -1, x1 = -1;
        for (int x = a; x <= b; x++) {
            int c = px[row + x];
            if ((c >>> 24) < 128) continue;
            boolean dark = ((c >> 16 & 255) + (c >> 8 & 255) + (c & 255)) < 200 && !Cutout.isSkin(c);
            if (!dark) { if (x0 < 0) x0 = x; x1 = x; }
        }
        return x0 < 0 ? null : new int[]{x0, x1};
    }

    /** The cores change smoothly from row to row (no steps where a curl of hair starts): averaged over five rows up and down. */
    private static void smoothCores(Model m) {
        int[][] out = new int[m.h][];
        for (int y = 0; y < m.h; y++) {
            int[] r = m.runs[y], c = m.core[y];
            if (r == null) continue;
            out[y] = c.clone();
            for (int i = 0; i < r.length / 2; i++) {
                int a = r[i * 2], b = r[i * 2 + 1];
                float sa = c[i * 2], sb = c[i * 2 + 1];
                int n = 1;
                for (int dy = -8; dy <= 8; dy++) {
                    if (dy == 0 || y + dy < 0 || y + dy >= m.h || m.runs[y + dy] == null) continue;
                    int[] o = m.runs[y + dy], oc = m.core[y + dy];
                    for (int j = 0; j < o.length / 2; j++) {
                        int overlap = Math.min(b, o[j * 2 + 1]) - Math.max(a, o[j * 2]);
                        if (overlap > 0 && m.part[y + dy][j] == m.part[y][i]) { sa += oc[j * 2]; sb += oc[j * 2 + 1]; n++; break; }
                    }
                }
                out[y][i * 2] = Math.max(a, Math.round(sa / n));
                out[y][i * 2 + 1] = Math.min(b, Math.round(sb / n));
            }
        }
        m.core = out;
    }

    /** The opaque runs of a row (alpha above half), tiny gaps closed, slivers dropped: {a, b, a, b, …} or null. */
    static int[] runsOf(int[] px, int w, int y, int minGap) {
        int row = y * w, x = 0;
        List<int[]> runs = new ArrayList<int[]>();
        while (x < w) {
            while (x < w && (px[row + x] >>> 24) <= 128) x++;
            if (x >= w) break;
            int a = x;
            while (x < w && (px[row + x] >>> 24) > 128) x++;
            int b = x - 1;
            if (!runs.isEmpty() && a - runs.get(runs.size() - 1)[1] <= minGap) { runs.get(runs.size() - 1)[1] = b; continue; }
            runs.add(new int[]{a, b});
        }
        int n = 0;
        for (int[] r : runs) if (r[1] - r[0] >= 2) n++;
        if (n == 0) return null;
        int[] out = new int[n * 2];
        int k = 0;
        for (int[] r : runs) if (r[1] - r[0] >= 2) { out[k++] = r[0]; out[k++] = r[1]; }
        return out;
    }

    /** The run of a row that holds x, else the nearest one. */
    static int[] runAt(int[] runs, int x) {
        if (runs == null) return null;
        int best = -1, bestD = Integer.MAX_VALUE;
        for (int i = 0; i < runs.length / 2; i++) {
            int a = runs[i * 2], b = runs[i * 2 + 1];
            int d = x < a ? a - x : x > b ? x - b : 0;
            if (d < bestD) { bestD = d; best = i; }
        }
        return best < 0 ? null : new int[]{runs[best * 2], runs[best * 2 + 1]};
    }

    /** The index of the run that holds the figure's middle (else the widest). */
    static int mainRun(int[] runs, int cx) {
        int best = 0, bestW = -1;
        for (int i = 0; i < runs.length / 2; i++) {
            int a = runs[i * 2], b = runs[i * 2 + 1];
            if (a <= cx && cx >= a && cx <= b) return i;
            if (b - a > bestW) { bestW = b - a; best = i; }
        }
        return best;
    }

    /** Two legs: the two widest runs, left to right, when the second is at least a third of the first. */
    static int[] twoLegs(int[] runs, int minGap) {
        int n = runs.length / 2;
        if (n < 2) return null;
        int i1 = -1, i2 = -1, b1 = -1, b2 = -1;
        for (int i = 0; i < n; i++) {
            int len = runs[i * 2 + 1] - runs[i * 2];
            if (len > b1) { b2 = b1; i2 = i1; b1 = len; i1 = i; } else if (len > b2) { b2 = len; i2 = i; }
        }
        int l = Math.min(i1, i2), r = Math.max(i1, i2);
        if (runs[r * 2] - runs[l * 2 + 1] < minGap || b2 < b1 * 0.35f) return null;
        return new int[]{runs[l * 2], runs[l * 2 + 1], runs[r * 2], runs[r * 2 + 1]};
    }

    /** Takes the jitter of the cut-out's edge out of the runs: each edge is averaged with the same edge two rows up and down. */
    private static void smoothRuns(Model m) {
        int[][] out = new int[m.h][];
        for (int y = 0; y < m.h; y++) {
            int[] r = m.runs[y];
            if (r == null) continue;
            out[y] = r.clone();
            for (int i = 0; i < r.length / 2; i++) {
                int a = r[i * 2], b = r[i * 2 + 1], sa = a, sb = b, n = 1;
                for (int dy = -4; dy <= 4; dy++) {
                    if (dy == 0 || y + dy < 0 || y + dy >= m.h || m.runs[y + dy] == null) continue;
                    int[] o = m.runs[y + dy];
                    for (int j = 0; j < o.length / 2; j++) {
                        int oa = o[j * 2], ob = o[j * 2 + 1];
                        int overlap = Math.min(b, ob) - Math.max(a, oa);
                        float wd = (b - a + 1), wo = (ob - oa + 1);
                        if (overlap > 0 && wo > wd * 0.7f && wo < wd * 1.4f) { sa += oa; sb += ob; n++; break; }
                    }
                }
                out[y][i * 2] = Math.round(sa / (float) n);
                out[y][i * 2 + 1] = Math.round(sb / (float) n);
            }
        }
        m.runs = out;
    }

    // ------------------------------------------------------------------ the figure as a mesh

    /** Vertices round every ring: half of them on the front, half on the back; four of each half per flap. */
    private static final int HALF = 22, SEG = HALF * 2, FLAP = 4;

    /** The depth offset of a run for the walking stride (legs apart, arms opposite), in picture pixels. */
    private static float strideOf(Model m, int y, int part, float stride) {
        if (stride <= 0) return 0;
        switch (part) {
            case P_LEG_L: return stride * Math.min(1, (y - m.hipRow) / Math.max(1f, m.botRow - m.hipRow));
            case P_LEG_R: return -stride * Math.min(1, (y - m.hipRow) / Math.max(1f, m.botRow - m.hipRow));
            case P_ARM_L: return -stride * 0.6f * Math.max(0, Math.min(1, (y - m.chinRow) / Math.max(1f, m.armEndRow - m.chinRow)));
            case P_ARM_R: return stride * 0.6f * Math.max(0, Math.min(1, (y - m.chinRow) / Math.max(1f, m.armEndRow - m.chinRow)));
            default: return 0;
        }
    }

    /**
     * Where a vertex of the figure takes its colour from in the picture: the front half from the picture, the
     * back half from the picture mirrored (the costume goes round), the back of the head from the top of the head.
     */
    private static void texPoint(Model m, int y, float a, float b, float c, float cr, float x, boolean back, float[] out) {
        float u = x, v = y + 0.5f;
        boolean headRows = !m.animal && m.hairRow > m.topRow + 4 && y < m.chinRow;
        boolean faceRows = headRows && y >= m.hairRow;
        float side = Math.abs(x - c) / Math.max(1f, cr);
        // the back of the head, and the sides of the head beyond the temples, look like the top of the head — hair,
        // a turban, a crown, a bald head, never the face stretched round: the band just above the hairline, as
        // wide as the head, repeated down and round without a seam
        if ((back && faceRows && side < 1.02f) || (headRows && side > 0.8f && side < 1.02f)) {
            float bandH = m.bandH;
            float k = (y - (headRows && !faceRows ? m.topRow : m.hairRow)) / bandH;
            if (!faceRows) k = (m.hairRow - y) / bandH;                  // the crown's sides: the band read upward
            float tri = k - (float) Math.floor(k);
            if (((int) Math.floor(k) & 1) == 1) tri = 1 - tri;
            float vv = m.hairRow - 1 - tri * bandH;
            int[] tr = runAt(m.runs[Math.min(m.h - 1, Math.max(0, Math.round(vv)))], Math.round(c));
            if (tr != null) {
                float tc = (tr[0] + tr[1]) / 2f, trr = Math.max(1, (tr[1] - tr[0]) / 2f);
                float stretch = Math.min(1.15f, trr / Math.max(1, cr));
                u = tc + (x - c) * stretch;
                v = vv;
                a = tr[0]; b = tr[1];
            }
        }
        // never the soft edge of the cut-out: three pixels inside
        if (u < a + 3) u = a + 3;
        if (u > b - 3) u = b - 3;
        out[0] = u; out[1] = v;
    }

    /**
     * The front half of a ring, left to right: a flat flap, the round core, a flat flap. Fills x[], z[] (z of the
     * front; the back is its mirror), nx[], nz[] (the outward normal, before the slope) for HALF vertices.
     */
    private static void halfRing(float a, float b, float ca, float cb, float f, float thin, float[] x, float[] z, float[] nx, float[] nz) {
        float c = (ca + cb) / 2f, cr = Math.max(0.5f, (cb - ca) / 2f + 0.5f);
        float zc = f * cr;
        float phi0 = (float) Math.acos(Math.min(1, thin / Math.max(1e-3f, zc)));
        int nc = HALF - 2 * FLAP;
        for (int k = 0; k < FLAP; k++) {
            float t = k / (float) (FLAP - 1);
            x[k] = a + (ca - a) * 0.75f * t; z[k] = thin * t; nx[k] = -0.35f * (1 - t); nz[k] = 1;
        }
        for (int k = 0; k < nc; k++) {
            float phi = -phi0 + 2 * phi0 * k / (nc - 1);
            float sn = (float) Math.sin(phi), cs = (float) Math.cos(phi);
            x[FLAP + k] = c + cr * sn; z[FLAP + k] = zc * cs; nx[FLAP + k] = sn; nz[FLAP + k] = cs / Math.max(0.2f, f);
        }
        for (int k = 0; k < FLAP; k++) {
            float t = k / (float) (FLAP - 1);
            int i = HALF - FLAP + k;
            x[i] = cb + (b - cb) * (0.25f + 0.75f * t); z[i] = thin * (1 - t); nx[i] = 0.35f * t; nz[i] = 1;
        }
    }

    /**
     * Builds the figure into a scene: the picture's height is 1 unit, its middle is x = 0, its feet are at y = 0.
     * stride: the walking stride in units (legs apart in depth, arms opposite) for a side view, or 0.
     * Returns the material index.
     */
    static int mesh(Studio3D.Scene s, Model m, float stride) {
        Studio3D.Mesh mesh = s.mesh;
        int mat = mesh.mat(Studio3D.Material.textured(m.px, m.w, m.h, 0.68f));
        float H = m.botRow - m.topRow + 1, unit = 1f / H;
        float cxPx = m.cx * m.w, thin = 0.012f * H;
        int rs = Math.max(1, Math.round(H / 640f));
        float strPx = stride * H;
        // the rings of the previous row: {a, b, base vertex index, core radius} per run
        List<float[]> prev = new ArrayList<float[]>();
        float[] tp = new float[2], hx = new float[HALF], hz = new float[HALF], hnx = new float[HALF], hnz = new float[HALF];
        for (int y = m.topRow; y <= m.botRow; y += rs) {
            int[] r = m.runs[y];
            List<float[]> cur = new ArrayList<float[]>();
            if (r != null) {
                for (int i = 0; i < r.length / 2; i++) {
                    float a = r[i * 2], b = r[i * 2 + 1], ca = m.core[y][i * 2], cb = m.core[y][i * 2 + 1];
                    float c = (ca + cb) / 2f, cr = (cb - ca) / 2f + 0.5f, f = m.depth[y][i];
                    float dz = strideOf(m, y, m.part[y][i], strPx);
                    // the ring above this run (the one it grows out of), for joining and for the slope of the surface
                    float[] parent = null;
                    float bestOv = 0;
                    for (float[] p : prev) {
                        float ov = Math.min(b, p[1]) - Math.max(a, p[0]);
                        if (ov > bestOv) { bestOv = ov; parent = p; }
                    }
                    float slope = parent != null ? (cr - parent[3]) / rs : 0;
                    if (slope > 3) slope = 3; else if (slope < -3) slope = -3;
                    halfRing(a, b, ca, cb, f, thin, hx, hz, hnx, hnz);
                    float wy = (m.botRow + 0.5f - (y + 0.5f)) * unit;
                    int base = mesh.nv;
                    for (int k = 0; k < SEG; k++) {
                        boolean back = k >= HALF;
                        int j = back ? SEG - 1 - k : k;            // the back goes right to left, mirrored in depth
                        float x = hx[j], z = back ? -hz[j] : hz[j];
                        texPoint(m, y, a, b, c, cr, x, back, tp);
                        float nx = hnx[j], nz = back ? -hnz[j] : hnz[j];
                        float nl = (float) Math.sqrt(nx * nx + nz * nz);
                        mesh.vertex((x - cxPx) * unit, wy, (z + dz) * unit, nx / nl, slope, nz / nl, tp[0], tp[1]);
                    }
                    if (parent != null) {
                        int pb = (int) parent[2];
                        for (int k = 0; k < SEG; k++) {
                            int k1 = (k + 1) % SEG;
                            mesh.triangle(pb + k, base + k, base + k1, mat);
                            mesh.triangle(pb + k, base + k1, pb + k1, mat);
                        }
                    } else {
                        // the first ring of a part (the crown of the head, a hand that starts on its own): closed with a fan
                        int centre = mesh.vertex((c - cxPx) * unit, wy + 0.4f * unit, dz * unit, 0, 1, 0, c, y + 0.5f);
                        for (int k = 0; k < SEG; k++) mesh.triangle(centre, base + k, base + (k + 1) % SEG, mat);
                    }
                    cur.add(new float[]{a, b, base, cr});
                }
            }
            prev = cur;
        }
        return mat;
    }

    // ------------------------------------------------------------------ rendering views

    /**
     * The figure seen from an angle (0 the front, 45 three-quarter, 90 the side, 180 the back), about 'size'
     * pixels tall, on a transparent background, with the face points carried over (valid up to the three-quarter
     * view). stride: the walking stride for a side view (0.1 is a step), or 0.
     */
    public static Doll3D.Result render(Model m, int size, float angleDeg, float stride) {
        Studio3D.Scene s = new Studio3D.Scene();
        mesh(s, m, stride);
        float wide = m.w / (float) Math.max(1, m.botRow - m.topRow + 1);
        // the face points on the front surface, carried through the view
        float[][] pts = {{m.eyeLX, m.eyeLY}, {m.eyeRX, m.eyeRY}, {m.mouthX, m.mouthY}, {m.mouthX + m.mouthHW, m.mouthY}, {m.eyeLX + m.eyeR, m.eyeLY}};
        for (float[] p : pts) {
            float u = p[0] * m.w, v = p[1] * m.h;
            int y = Math.max(0, Math.min(m.h - 1, Math.round(v)));
            int[] run = m.faceKnown ? runAt(m.runs[y], Math.round(u)) : null;
            float z = 0;
            if (run != null) {
                float c = (run[0] + run[1]) / 2f, rad = (run[1] - run[0]) / 2f + 0.5f;
                float sin = Math.max(-1, Math.min(1, (u - c) / rad));
                float f = m.depth[y] != null ? m.depth[y][Math.max(0, mainRun(m.runs[y], Math.round(u)))] : 0.9f;
                z = f * rad * (float) Math.sqrt(Math.max(0, 1 - sin * sin));
            }
            float unit = 1f / Math.max(1, m.botRow - m.topRow + 1);
            s.mark((u - m.cx * m.w) * unit, (m.botRow + 0.5f - v) * unit, z * unit);
        }
        return renderFigure(s, wide, size, angleDeg, m.faceKnown && Math.abs(angleDeg) <= 50);
    }

    /**
     * Renders a figure built into a scene (height 1, feet at y = 0, middle at x = 0, five face marks or none)
     * from an angle, about 'size' pixels tall, on a transparent background: the long lens far away of the
     * doll's master sheet, the key and rim lights walking round with the camera. wide: the figure's width in
     * units of its height (the canvas width).
     */
    static Doll3D.Result renderFigure(Studio3D.Scene s, float wide, int size, float angleDeg, boolean faceKnown) {
        float H = 1f;
        float extentH = 1.06f * H, extentW = Math.max(wide, 0.5f) * H;
        float dist = 12f * H;
        double a = Math.toRadians(angleDeg);
        float sn = (float) Math.sin(a), cs = (float) Math.cos(a);
        s.camX = dist * sn; s.camY = extentH * 0.47f; s.camZ = dist * cs;
        s.lookX = 0; s.lookY = extentH * 0.47f; s.lookZ = 0;
        int h = size, w = Math.round(size * Math.max(0.6f, Math.min(1.6f, wide)));
        float half = Math.max(extentH / 2f, extentW / 2f * h / (float) w) * 1.04f;
        s.fovDeg = (float) Math.toDegrees(2 * Math.atan(half / dist));
        float kx = -0.45f, ky = 0.8f, kz = 0.85f, rx = 0.75f, ry = 0.4f, rz = -0.5f;
        s.keyX = kx * cs + kz * sn; s.keyY = ky; s.keyZ = -kx * sn + kz * cs;
        s.rimX = rx * cs + rz * sn; s.rimY = ry; s.rimZ = -rx * sn + rz * cs;
        s.keyStrength = 1.0f; s.fillStrength = 0.5f; s.rimStrength = 0.35f; s.ambient = 0.42f;
        s.skyTop = 0;
        s.ao = true; s.aoRadius = 0.03f * H;
        s.shadows = true;
        while (s.marks.size() < 5) s.mark(0, 0.8f, 0);
        Studio3D.Picture p = Studio3D.crop(Studio3D.render(s, w, h, 2), 3);
        Doll3D.Result r = new Doll3D.Result();
        r.px = p.px; r.w = p.w; r.h = p.h;
        r.sideView = false;
        r.faceKnown = faceKnown;
        r.eyeLX = p.marks[0][0]; r.eyeLY = p.marks[0][1];
        r.eyeRX = p.marks[1][0]; r.eyeRY = p.marks[1][1];
        r.mouthX = p.marks[2][0]; r.mouthY = p.marks[2][1];
        r.mouthHW = Math.abs(p.marks[3][0] - p.marks[2][0]);
        r.eyeR = Math.abs(p.marks[4][0] - p.marks[0][0]);
        r.turbanY = 0;
        return r;
    }

    /**
     * The angles of the views the director uses besides the picture itself: three-quarter, side (mid-stride),
     * back. The camera stands to the character's left (negative angles), so the three-quarter and side views
     * face to the right like a front picture does (facing +1); the renderer mirrors them for the other way.
     */
    public static final float[] VIEW_ANGLES = {-45, -90, 180};
    public static final String[] VIEW_NAMES = {"three-quarter", "side", "back"};

    /** The index of a view angle in VIEW_ANGLES, or -1. */
    public static int viewIndex(float angle) {
        for (int i = 0; i < VIEW_ANGLES.length; i++) if (Math.abs(VIEW_ANGLES[i] - angle) < 1 || Math.abs(VIEW_ANGLES[i] + angle) < 1 && Math.abs(angle) != 180) return i;
        return -1;
    }

    /** The three views of a figure, 'size' pixels tall: three-quarter, side in mid-stride, back. */
    public static Doll3D.Result[] views(Model m, int size) {
        Doll3D.Result[] out = new Doll3D.Result[VIEW_ANGLES.length];
        for (int i = 0; i < VIEW_ANGLES.length; i++) out[i] = render(m, size, VIEW_ANGLES[i], Math.abs(VIEW_ANGLES[i]) == 90 ? 0.1f : 0);
        return out;
    }
}
