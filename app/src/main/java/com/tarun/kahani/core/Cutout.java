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
    }

    private static int dist(int a, int b) {
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
        int[] px = src.clone();
        if (!hasAlpha(px)) removeBackground(px, w, h, holes);
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
        findFace(r);
        return r;
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

    static boolean isSkin(int c) {
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
