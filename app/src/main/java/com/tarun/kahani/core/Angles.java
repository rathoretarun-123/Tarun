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
     * Splits a sheet that holds several figures (angles, poses, expressions — in a row, or rows and columns)
     * into them. v26: the sheet's background colour is read from its border; the figures are found by the empty
     * columns and rows between them (projection), rows first then columns within each row, or the other way
     * round — whichever finds more figures; thin grid lines and small labels never count; every piece is then
     * cut out on its own (its real background removed, its holes kept). When no empty column or row separates
     * anything, the opaque groups of the cut-out are used (the v21 way). One figure gives one piece.
     */
    public static List<Piece> split(int[] src, int w, int h) { return split(src, w, h, true); }

    /**
     * The same for a sheet of place pictures (v26): the panels of a background sheet (rows and columns of views
     * of one place) come back as whole crops — nothing is cut out of them. cutOut = false.
     */
    public static List<Piece> split(int[] src, int w, int h, boolean cutOut) {
        boolean alpha = Cutout.hasAlpha(src);
        boolean[] on = new boolean[w * h];
        int bg = borderColour(src, w, h);
        if (alpha) { for (int i = 0; i < on.length; i++) on[i] = (src[i] >>> 24) > 100; }
        else {
            // the background is what the border's colour reaches from the border (white clothes on a white sheet
            // stay part of the figure: a plain colour distance would cut a white kurta into gaps)
            int[] tmp = src.clone();
            try { Cutout.removeBackground(tmp, w, h, false, bg); } catch (RuntimeException e) { for (int i = 0; i < tmp.length; i++) tmp[i] = Cutout.dist(src[i], bg) > 60 ? src[i] | 0xFF000000 : 0; }
            for (int i = 0; i < on.length; i++) on[i] = (tmp[i] >>> 24) > 100;
        }
        clearGridLines(on, w, h);
        int[] labelsA = new int[w * h], labelsB = new int[w * h];
        List<int[]> cellsA = cells(on, w, h, true, labelsA), cellsB = cells(on, w, h, false, labelsB);
        List<Piece> a = pieces(src, w, h, cellsA, alpha || !cutOut ? 0 : bg, labelsA), b = pieces(src, w, h, cellsB, alpha || !cutOut ? 0 : bg, labelsB);
        // the order that finds more figures wins — unless it did so by cutting a figure into a big and a small part
        // (a piece under 30% of the median area), which the other order did not
        List<Piece> fa = figures(a, w, h), fb = figures(b, w, h);
        boolean ta = tinyPiece(fa), tb = tinyPiece(fb);
        List<Piece> best = ta != tb ? (ta ? b : a) : fa.size() >= fb.size() ? a : b;
        if (figures(best, w, h).size() >= 2) return order(best, h);
        return splitByGroups(src, w, h);
    }

    /** The sheet's background colour: the mean of its border pixels. */
    static int borderColour(int[] px, int w, int h) {
        long sr = 0, sg = 0, sb = 0; int n = 0;
        for (int x = 0; x < w; x += Math.max(1, w / 64)) for (int y : new int[]{0, 1, h - 2, h - 1}) { int c = px[y * w + x]; sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; n++; }
        for (int y = 0; y < h; y += Math.max(1, h / 64)) for (int x : new int[]{0, 1, w - 2, w - 1}) { int c = px[y * w + x]; sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; n++; }
        return 0xFF000000 | ((int) (sr / n) << 16) | ((int) (sg / n) << 8) | (int) (sb / n);
    }

    /** The thin lines of a sheet's grid or table (long, under 0.6% of the sheet thick) cleared, so they never join the figures. */
    static void clearGridLines(boolean[] on, int w, int h) {
        int[] cols = new int[w], rows = new int[h];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (on[y * w + x]) { cols[x]++; rows[y]++; }
        int thinW = Math.max(4, Math.round(w * 0.006f)), thinH = Math.max(4, Math.round(h * 0.006f));
        // only the line itself goes: a pixel of the band whose column (or row) is occupied just outside the band on
        // both sides belongs to a figure the line runs through, and stays
        for (int x = 0; x < w; ) {
            if (cols[x] < h * 0.5f) { x++; continue; }
            int x1 = x;
            while (x1 < w && cols[x1] >= h * 0.5f) x1++;
            if (x1 - x <= thinW) {
                int l = Math.max(0, x - 2), rr = Math.min(w - 1, x1 + 1);
                for (int y = 0; y < h; y++) { boolean through = on[y * w + l] && on[y * w + rr]; if (!through) for (int xx = x; xx < x1; xx++) on[y * w + xx] = false; }
            }
            x = x1;
        }
        for (int y = 0; y < h; ) {
            if (rows[y] < w * 0.5f) { y++; continue; }
            int y1 = y;
            while (y1 < h && rows[y1] >= w * 0.5f) y1++;
            if (y1 - y <= thinH) {
                int t = Math.max(0, y - 2), b = Math.min(h - 1, y1 + 1);
                for (int x = 0; x < w; x++) { boolean through = on[t * w + x] && on[b * w + x]; if (!through) for (int yy = y; yy < y1; yy++) on[yy * w + x] = false; }
            }
            y = y1;
        }
    }

    /** The occupied runs of a profile: runs thinner than 'noise' are dropped (grid lines), gaps thinner than 'join' are closed. Returns {start, end} pairs (end exclusive). */
    static List<int[]> runs(int[] count, int threshold, int noise, int join) {
        boolean[] occ = new boolean[count.length];
        for (int i = 0; i < count.length; i++) occ[i] = count[i] > threshold;
        List<int[]> out = new ArrayList<int[]>();
        int i = 0;
        while (i < occ.length) {
            if (!occ[i]) { i++; continue; }
            int j = i;
            while (j < occ.length && occ[j]) j++;
            if (j - i >= noise) out.add(new int[]{i, j});
            i = j;
        }
        // close the small gaps — and the seams of a sheet's grid running through a figure (an empty band under 0.8% of
        // the length with the figure going on at over half its peak on both sides)
        int max = 0;
        for (int v : count) max = Math.max(max, v);
        int reach = Math.max(3, Math.round(count.length * 0.02f));
        List<int[]> merged = new ArrayList<int[]>();
        for (int[] r : out) {
            if (!merged.isEmpty()) {
                int[] prev = merged.get(merged.size() - 1);
                int gap = r[0] - prev[1];
                boolean seam = gap < Math.max(3, count.length * 0.008f) && count[Math.max(0, prev[1] - reach)] > 0.25f * max && count[Math.min(count.length - 1, r[0] + reach)] > 0.25f * max;
                if (gap < join || seam) { prev[1] = r[1]; continue; }
            }
            merged.add(r);
        }
        return merged;
    }

    /**
     * The cells {x0, y0, x1, y1} of the sheet: a recursive cut along empty rows and columns (rows first or columns
     * first), and where nothing empty separates two figures that touch (a sword tip, a braid, a horn, a tail) a
     * cut at the thinnest place between them — only when the part is clearly too long for one figure and both
     * halves are figure-tall, so an outstretched arm is never cut off.
     */
    static List<int[]> cells(boolean[] on, int w, int h, boolean rowsFirst) { return cells(on, w, h, rowsFirst, null); }

    /** With 'labels' (w*h, zero = none): a cell {x0, y0, x1, y1, label} whose figure was told apart from a touching neighbour by erosion keeps only its own pixels. */
    static List<int[]> cells(boolean[] on, int w, int h, boolean rowsFirst, int[] labels) {
        List<int[]> out = new ArrayList<int[]>();
        xy(on, w, h, new int[]{0, 0, w, h}, rowsFirst, false, 0, 0, out, labels);
        return out;
    }

    /** The tight box {x0, y0, x1, y1} of the occupied pixels inside a region, or null when it is empty. */
    static int[] tight(boolean[] on, int w, int[] r) {
        int x0 = r[2], y0 = r[3], x1 = r[0] - 1, y1 = r[1] - 1;
        for (int y = r[1]; y < r[3]; y++) for (int x = r[0]; x < r[2]; x++) if (on[y * w + x]) { if (x < x0) x0 = x; if (x > x1) x1 = x; if (y < y0) y0 = y; if (y > y1) y1 = y; }
        return x1 < x0 ? null : new int[]{x0, y0, x1 + 1, y1 + 1};
    }

    static int[] profile(boolean[] on, int w, int[] r, boolean rows) {
        int[] p = new int[rows ? r[3] - r[1] : r[2] - r[0]];
        for (int y = r[1]; y < r[3]; y++) for (int x = r[0]; x < r[2]; x++) if (on[y * w + x]) p[rows ? y - r[1] : x - r[0]]++;
        return p;
    }

    private static void xy(boolean[] on, int w, int h, int[] region, boolean rows, boolean triedOther, int depth, int siblingMedian, List<int[]> out, int[] labels) {
        int[] r = tight(on, w, region);
        if (r == null) return;
        int rw = r[2] - r[0], rh = r[3] - r[1];
        if (rw < 2 || rh < 2) return;
        if (depth > 14) { out.add(r); return; }
        int[] p = profile(on, w, r, rows);
        int along = rows ? rh : rw, across = rows ? rw : rh;
        int dim = rows ? h : w;
        List<int[]> runs = runs(p, Math.max(2, across / 400), Math.max(3, dim / 100), Math.max(2, dim / 160));
        if (runs.size() >= 2) {
            List<Integer> lens = new ArrayList<Integer>();
            for (int[] run : runs) lens.add(run[1] - run[0]);
            java.util.Collections.sort(lens);
            int med = lens.get(lens.size() / 2);
            for (int[] run : runs) {
                int[] sub = rows ? new int[]{r[0], r[1] + run[0], r[2], r[1] + run[1]} : new int[]{r[0] + run[0], r[1], r[0] + run[1], r[3]};
                xy(on, w, h, sub, !rows, false, depth + 1, runs.size() >= 3 ? med : 0, out, labels);
            }
            return;
        }
        // one run. A soft gap first: a nearly empty line (a braid tip or a sword point crossing it) between two
        // figure-sized parts is a gap all the same.
        int cutAt = valley(on, w, r, rows, p, siblingMedian, 0.03f);
        // else two figures touching: cut at the thinnest place when the part is too long for one figure (a
        // figure is taller than wide (a side view up to 3 times): stacked ones over 3.4 times as tall as wide, side-by-side ones over 1.3 times as wide as tall)
        boolean tooLong = along > (rows ? 3.4f : 1.3f) * across || (siblingMedian > 0 && along > 1.6f * siblingMedian);
        // (a stacked pair is cut only at a nearly empty line — a raised hand or a sword crossing it — never at a neck or a waist)
        if (cutAt < 0 && tooLong) cutAt = valley(on, w, r, rows, p, siblingMedian, rows ? 0.08f : 0.3f);
        if (cutAt > 0) {
            int[] a = rows ? new int[]{r[0], r[1], r[2], r[1] + cutAt} : new int[]{r[0], r[1], r[0] + cutAt, r[3]};
            int[] b = rows ? new int[]{r[0], r[1] + cutAt, r[2], r[3]} : new int[]{r[0] + cutAt, r[1], r[2], r[3]};
            xy(on, w, h, a, rows, false, depth + 1, siblingMedian, out, labels);
            xy(on, w, h, b, rows, false, depth + 1, siblingMedian, out, labels);
            return;
        }
        if (!triedOther) { xy(on, w, h, r, !rows, true, depth + 1, siblingMedian, out, labels); return; }
        // two figures touching more than lightly (a foot on a tail, a fist at a shoulder): when the part is too long
        // for one figure in either direction, the mask thinned a little falls apart into them
        // the thinning check is its own proof (two figure-sized groups beside each other), so every part at least
        // as wide as it is tall (figures are taller than wide; a pair side by side is square or wider) is tried
        boolean tooLongEither = rw > 0.9f * rh || rh > 3.0f * rw || (siblingMedian > 0 && along > 1.5f * siblingMedian);
        if (tooLongEither && labels != null) {
            List<int[]> parts = erodeSplit(on, w, r, labels);
            if (parts != null) { out.addAll(parts); return; }
        }
        out.add(r);
    }

    private static int nextLabel = 1;

    /**
     * The figures of a part that touch: the mask eroded by 2% of the part's smaller side is labelled into its
     * groups; the groups at least 40% as tall or as wide as the part are figures; every pixel of the part is then
     * given to the nearest figure (a flood from all of them at once). Returns the figures' cells {x0, y0, x1, y1,
     * label} or null when the part does not fall apart.
     */
    static List<int[]> erodeSplit(boolean[] on, int w, int[] r, int[] labels) {
        // thinned a little first, then more (a foot resting on a leg is thicker than a tail tip), never past 6%
        for (float f : new float[]{0.02f, 0.035f, 0.05f, 0.06f}) {
            List<int[]> parts = erodeSplit(on, w, r, labels, f);
            if (parts != null) return parts;
        }
        return null;
    }

    static List<int[]> erodeSplit(boolean[] on, int w, int[] r, int[] labels, float fraction) {
        int x0 = r[0], y0 = r[1], rw = r[2] - r[0], rh = r[3] - r[1];
        if (rw < 8 || rh < 8) return null;
        int rad = Math.max(2, Math.round(Math.min(rw, rh) * fraction));
        boolean[] t = new boolean[rw * rh], e = new boolean[rw * rh];
        for (int y = 0; y < rh; y++) for (int x = 0; x < rw; x++) {
            boolean all = true;
            for (int d = -rad; d <= rad && all; d++) { int xx = x + d; if (xx < 0 || xx >= rw || !on[(y + y0) * w + xx + x0]) all = false; }
            t[y * rw + x] = all;
        }
        for (int y = 0; y < rh; y++) for (int x = 0; x < rw; x++) {
            boolean all = true;
            for (int d = -rad; d <= rad && all; d++) { int yy = y + d; if (yy < 0 || yy >= rh || !t[yy * rw + x]) all = false; }
            e[y * rw + x] = all;
        }
        int[] lab = new int[rw * rh];
        int[] stack = new int[rw * rh];
        int n = 0;
        List<int[]> boxes = new ArrayList<int[]>();      // {x0, y0, x1, y1, area}
        for (int i = 0; i < lab.length; i++) {
            if (!e[i] || lab[i] != 0) continue;
            n++;
            int sp = 0; stack[sp++] = i; lab[i] = n;
            int bx0 = rw, by0 = rh, bx1 = -1, by1 = -1, area = 0;
            while (sp > 0) {
                int c = stack[--sp];
                int cx = c % rw, cy = c / rw;
                area++;
                if (cx < bx0) bx0 = cx;
                if (cx > bx1) bx1 = cx;
                if (cy < by0) by0 = cy;
                if (cy > by1) by1 = cy;
                int[] nb = {cx > 0 ? c - 1 : -1, cx < rw - 1 ? c + 1 : -1, cy > 0 ? c - rw : -1, cy < rh - 1 ? c + rw : -1};
                for (int j : nb) if (j >= 0 && e[j] && lab[j] == 0) { lab[j] = n; stack[sp++] = j; }
            }
            boxes.add(new int[]{bx0, by0, bx1 + 1, by1 + 1, area});
        }
        // the figures: in a wide part the figures stand side by side, so each is nearly as tall as the part and
        // they lie beside each other (a head cut from its body by the thinning is neither); in a tall part the other way round
        boolean wide = rw >= rh;
        List<Integer> big = new ArrayList<Integer>();
        for (int k = 0; k < boxes.size(); k++) {
            int[] b = boxes.get(k);
            // a figure lying or sitting beside a standing one is well under its height, but never under 35% of it
            boolean tall = b[3] - b[1] >= 0.35f * rh && b[2] - b[0] >= 0.15f * rw, broad = b[2] - b[0] >= 0.35f * rw && b[3] - b[1] >= 0.15f * rh;
            if ((wide ? tall : broad) && b[4] >= 0.02f * rw * rh) big.add(k + 1);
        }
        if (big.size() < 2) return null;
        // the figures of a pair are alike in bulk: a fist, a horn or a sword tip beside a body is far smaller (under 40% of it)
        int largest = 0;
        for (int k : big) largest = Math.max(largest, boxes.get(k - 1)[4]);
        for (int k : big) if (boxes.get(k - 1)[4] < 0.4f * largest) return null;
        for (int i = 0; i < big.size(); i++) for (int j = i + 1; j < big.size(); j++) {
            int[] a = boxes.get(big.get(i) - 1), b = boxes.get(big.get(j) - 1);
            int o = wide ? Math.min(a[2], b[2]) - Math.max(a[0], b[0]) : Math.min(a[3], b[3]) - Math.max(a[1], b[1]);
            int small = wide ? Math.min(a[2] - a[0], b[2] - b[0]) : Math.min(a[3] - a[1], b[3] - b[1]);
            if (o > 0.5f * small) return null;      // two groups over each other: one figure in pieces, not two figures
        }
        int[] owner = new int[rw * rh];
        int[] queue = new int[rw * rh];
        int qh = 0, qt = 0;
        for (int i = 0; i < lab.length; i++) if (lab[i] != 0 && big.contains(lab[i])) { owner[i] = lab[i]; queue[qt++] = i; }
        while (qh < qt) {
            int c = queue[qh++];
            int cx = c % rw, cy = c / rw;
            int[] nb = {cx > 0 ? c - 1 : -1, cx < rw - 1 ? c + 1 : -1, cy > 0 ? c - rw : -1, cy < rh - 1 ? c + rw : -1};
            for (int j : nb) if (j >= 0 && owner[j] == 0 && on[(j / rw + y0) * w + j % rw + x0]) { owner[j] = owner[c]; queue[qt++] = j; }
        }
        List<int[]> out = new ArrayList<int[]>();
        for (int k : big) {
            int bx0 = rw, by0 = rh, bx1 = -1, by1 = -1;
            int id;
            synchronized (Angles.class) { id = nextLabel++; }
            for (int i = 0; i < owner.length; i++) {
                if (owner[i] != k) continue;
                int x = i % rw, y = i / rw;
                if (x < bx0) bx0 = x;
                if (x > bx1) bx1 = x;
                if (y < by0) by0 = y;
                if (y > by1) by1 = y;
                labels[(y + y0) * w + x + x0] = id;
            }
            if (bx1 < bx0) continue;
            out.add(new int[]{bx0 + x0, by0 + y0, bx1 + 1 + x0, by1 + 1 + y0, id});
        }
        return out.size() >= 2 ? out : null;
    }

    /**
     * The place to cut a part that holds two touching figures: the thinnest place of the profile (under 30% of
     * its peak), away from both ends, where both halves are still at least 60% as tall (or wide) as the whole —
     * so an arm or a sword sticking out is never cut off as a "figure". Returns the offset, or -1.
     */
    private static int valley(boolean[] on, int w, int[] r, boolean rows, int[] p, int siblingMedian, float maxFrac) {
        int along = p.length, across = rows ? r[2] - r[0] : r[3] - r[1];
        int minPart = Math.max(8, Math.max(siblingMedian > 0 ? Math.round(siblingMedian * 0.35f) : 0, Math.round(across * 0.28f)));
        if (along < 2 * minPart) return -1;
        int k = Math.max(2, along / 60);
        int max = 0;
        for (int v : p) max = Math.max(max, v);
        List<int[]> cands = new ArrayList<int[]>();      // {offset, smoothed value}
        for (int x = minPart; x < along - minPart; x++) {
            int v = 0, n = 0;
            for (int d = -k; d <= k; d++) { int xx = x + d; if (xx >= 0 && xx < along) { v += p[xx]; n++; } }
            v = Math.round(v / (float) Math.max(1, n));
            if (v <= maxFrac * max) cands.add(new int[]{x, v});
        }
        java.util.Collections.sort(cands, new java.util.Comparator<int[]>() {
            public int compare(int[] a, int[] b) { return Integer.compare(a[1], b[1]); }
        });
        int tried = 0;
        for (int[] c : cands) {
            if (++tried > 40) break;
            int cutAt = c[0];
            // a seam of the sheet's grid running through a figure: an empty band thinner than 0.8% of the length with the
            // figure going on at full width on both sides of it — never a place to cut
            int b0 = cutAt, b1 = cutAt;
            while (b0 > 0 && p[b0 - 1] <= 0.1f * max) b0--;
            while (b1 < along - 1 && p[b1 + 1] <= 0.1f * max) b1++;
            int reach = Math.max(3, Math.round(along * 0.02f));
            int left = Math.max(0, b0 - reach), right = Math.min(along - 1, b1 + reach);
            if (b1 - b0 + 1 < Math.max(3, along * 0.008f) && p[left] > 0.5f * max && p[right] > 0.5f * max) continue;
            int[] a = rows ? new int[]{r[0], r[1], r[2], r[1] + cutAt} : new int[]{r[0], r[1], r[0] + cutAt, r[3]};
            int[] b = rows ? new int[]{r[0], r[1] + cutAt, r[2], r[3]} : new int[]{r[0] + cutAt, r[1], r[2], r[3]};
            int[] ta = tight(on, w, a), tb = tight(on, w, b);
            if (ta == null || tb == null) continue;
            int ea = rows ? ta[2] - ta[0] : ta[3] - ta[1], eb = rows ? tb[2] - tb[0] : tb[3] - tb[1];
            // a vertical cut needs both halves nearly as wide as the whole (a head on a neck is narrower: no cut there)
            float need = rows ? 0.8f : 0.6f;
            if (ea >= need * across && eb >= need * across) return cutAt;
        }
        return -1;
    }

    /** Each cell cut out on its own: a margin of background around it, the real background removed, the holes kept. */
    static List<Piece> pieces(int[] src, int w, int h, List<int[]> cells, int bg) { return pieces(src, w, h, cells, bg, null); }

    static List<Piece> pieces(int[] src, int w, int h, List<int[]> cells, int bg, int[] labels) {
        boolean alpha = bg == 0;
        List<Piece> out = new ArrayList<Piece>();
        for (int[] c : cells) {
            int m = Math.max(2, Math.min(w, h) / 200);
            int x0 = Math.max(0, c[0] - m), y0 = Math.max(0, c[1] - m), x1 = Math.min(w, c[2] + m), y1 = Math.min(h, c[3] + m);
            Piece p = new Piece();
            p.x0 = x0; p.y0 = y0; p.w = x1 - x0; p.h = y1 - y0;
            if (p.w < 2 || p.h < 2) continue;
            p.px = new int[p.w * p.h];
            for (int y = 0; y < p.h; y++) System.arraycopy(src, (y + y0) * w + x0, p.px, y * p.w, p.w);
            if (c.length >= 5 && labels != null) {
                // a figure told apart from a touching neighbour: the neighbour's pixels in this box become background
                int own = c[4];
                for (int y = 0; y < p.h; y++) for (int x = 0; x < p.w; x++) { int l = labels[(y + y0) * w + x + x0]; if (l != 0 && l != own) p.px[y * p.w + x] = alpha ? 0 : bg; }
            }
            if (!alpha) {
                // a cell's own background: its border is background (the gap around the figure), the figure is not
                try { Cutout.removeBackground(p.px, p.w, p.h, true, bg); } catch (RuntimeException e) { /* kept as it is */ }
            }
            out.add(p);
        }
        return out;
    }

    /**
     * The figures among the pieces (v26): at least 30% as tall as the tallest, 12% of the sheet's height and 4% of
     * its width (labels, arrows, crumbs and grid bits dropped), the twelve largest (a sheet of ten, with room for an eleventh). Fewer than two: not a sheet.
     */
    public static List<Piece> figures(List<Piece> parts, int w, int h) {
        List<Piece> out = new ArrayList<Piece>();
        int tallest = 0;
        for (Piece pc : parts) tallest = Math.max(tallest, pc.h);
        for (Piece pc : parts) if (pc.h >= tallest * 0.3f && pc.h >= h * 0.12f && pc.w >= w * 0.04f && opaqueShare(pc) > 0.04f) out.add(pc);
        // a prop that came loose from a figure (a sword, a hat, a bone) is far smaller than the figures: under 30% of their median area
        if (out.size() >= 3) {
            List<Long> areas = new ArrayList<Long>();
            for (Piece pc : out) areas.add((long) pc.w * pc.h);
            java.util.Collections.sort(areas);
            long median = areas.get(areas.size() / 2);
            List<Piece> kept = new ArrayList<Piece>();
            for (Piece pc : out) if ((long) pc.w * pc.h >= 0.3f * median) kept.add(pc);
            out = kept;
        }
        if (out.size() < 2) return new ArrayList<Piece>();
        if (out.size() > 12) {
            java.util.Collections.sort(out, new java.util.Comparator<Piece>() {
                public int compare(Piece a, Piece b) { return Long.compare((long) b.w * b.h, (long) a.w * a.h); }
            });
            out = new ArrayList<Piece>(out.subList(0, 12));
        }
        return order(out, h);
    }

    /** True when a piece is under 30% of the median piece area (a part of a figure, not a figure). */
    static boolean tinyPiece(List<Piece> figs) {
        if (figs.size() < 2) return false;
        List<Long> areas = new ArrayList<Long>();
        for (Piece p : figs) areas.add((long) p.w * p.h);
        java.util.Collections.sort(areas);
        long median = areas.get(areas.size() / 2);
        return areas.get(0) < 0.3f * median;
    }

    static float opaqueShare(Piece p) {
        int n = 0;
        for (int i = 0; i < p.px.length; i += 7) if ((p.px[i] >>> 24) > 100) n++;
        return n * 7f / Math.max(1, p.px.length);
    }

    /** Reading order: row by row (pieces whose tops lie within a quarter of the height of each other), left to right. */
    static List<Piece> order(List<Piece> in, final int h) {
        List<Piece> out = new ArrayList<Piece>(in);
        final int band = Math.max(1, h / 4);
        java.util.Collections.sort(out, new java.util.Comparator<Piece>() {
            public int compare(Piece a, Piece b) {
                int ra = a.y0 / band, rb = b.y0 / band;
                return ra != rb ? Integer.compare(ra, rb) : Integer.compare(a.x0, b.x0);
            }
        });
        return out;
    }

    /** The v21 split: the opaque groups of the cut-out (parts closer than 2% of the width belong together), groups under 3% of the area are noise. */
    public static List<Piece> splitByGroups(int[] src, int w, int h) {
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
        return order(out, h);
    }

    /**
     * The angle a figure's picture shows, from where its face is: both eyes found and centred = front; the
     * eyes off the figure's centre = three-quarter; one narrow figure with a face at its edge = side; no face
     * on a figure = back. Returns FRONT, THREE_QUARTER, SIDE or BACK.
     */
    public static float guess(Cutout.Result r) { return guess(r, false); }

    /**
     * v27: beast = an animal (its fur reads as skin, so the skin rule is not used). For a person the first question
     * is skin: a back shows hair where the face would be (skin under 42% of the face box), whatever the eyes do —
     * so a face with its eyes shut (laughing, crying, asleep) is still a front, never a back.
     */
    public static float guess(Cutout.Result r, boolean beast) {
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
        if (beast) { if (dark > 0.42f || faceW > 1.0f || (hairAbove > 2.2f && skin < 0.3f)) return BACK; }
        else if (skin < 0.42f || (faceW > 1.0f && skin < 0.6f) || hairAbove > 1.6f || (dark > 0.42f && skin < 0.55f)) return BACK;   // a "face" found far below the head's top is a hand, not a face; dark "eyes" on little skin are hair
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
