package com.tarun.kahani.core;

/**
 * Takes the turban, cap or crown off a character picture. The headwear is found from its own colours (the cloth
 * above the brows that is neither skin nor hair, and everything of the same colours joined to it down the sides
 * of the head); the picture without it gets a bare head of the face's own skin, shaded round, and the headwear
 * itself is kept as a separate cut-out, so the very same turban can be put on whoever snatched it.
 */
public final class Headwear {
    /** The picture without its headwear (same size as the original). */
    public int[] bare;
    /** The headwear alone, cropped to hx0..hx1, hy0..hy1 of the original picture (pixels). */
    public int[] hat;
    public int hx0, hy0, hx1, hy1;
    /** Where the headwear sits relative to the eyes: offsets in eye-distances from the point between the eyes. */
    public float relX0, relY0, relX1, relY1;

    private Headwear() {}

    /**
     * eyes in pixels: (lx, ly) and (rx, ry). Returns null when there is no clear headwear (bare heads, plain hair,
     * a scarf the same colour as the skin).
     */
    public static Headwear strip(int[] px, int w, int h, float lx, float ly, float rx, float ry, int skin) {
        float d = Math.abs(rx - lx);
        if (d < 6) return null;
        float ex = (lx + rx) / 2, ey = (ly + ry) / 2;
        // the head's half width at the eyes, from the outline (the face is usually 2.2..2.8 eye distances wide)
        int row = Math.max(0, Math.min(h - 1, (int) ey));
        int a = (int) ex, b = (int) ex;
        while (a > 0 && (px[row * w + a - 1] >>> 24) > 128 && ex - a < 2.2f * d) a--;
        while (b < w - 1 && (px[row * w + b + 1] >>> 24) > 128 && b - ex < 2.2f * d) b++;
        float half = Math.max(0.95f * d, Math.min(1.45f * d, (b - a) / 2f));
        int x0 = Math.max(0, (int) (ex - 2.2f * d)), x1 = Math.min(w - 1, (int) (ex + 2.2f * d));
        int yTop = 0, yLow = Math.min(h - 1, (int) (ey + 0.6f * d));
        float brow = ey - 0.6f * d;       // the brows; above them is forehead or headwear
        // 1. the headwear's colours: what is clearly above the face and is neither skin nor hair
        int[] hist = new int[4096];
        int n = 0;
        for (int y = yTop; y < (int) (ey - 0.85f * d) && y < h; y++) for (int x = x0; x <= x1; x++) {
            int c = px[y * w + x];
            if ((c >>> 24) < 200 || skinLike(c, skin) || hairLike(c)) continue;
            hist[bin(c)]++;
            n++;
        }
        if (n < d * d * 0.6f) return null;           // nothing (or almost nothing) on the head
        boolean[] pal = new boolean[4096];
        for (int i = 0; i < 4096; i++) if (hist[i] > n * 0.004f) pal[i] = true;
        // 2. headwear pixels: above the brows anything that is not skin (jewels and folds too), lower down at the
        // sides of the head only the headwear's own colours; then only what is joined to the top part
        boolean[] m = new boolean[w * h];
        for (int y = yTop; y <= yLow; y++) for (int x = x0; x <= x1; x++) {
            int c = px[y * w + x];
            if ((c >>> 24) < 128) continue;
            boolean side = Math.abs(x - ex) > 0.62f * d;
            if (y < ey - 0.8f * d) m[y * w + x] = !skinLike(c, skin) && (!hairLike(c) || y < ey - 1.1f * d || pal[bin(c)]);
            else if (y < brow && !hairLike(c) && !skinLike(c, skin) && Math.abs(x - ex) > 0.3f * d) m[y * w + x] = pal[bin(c)];
            else if (side && y < ey + 0.45f * d) m[y * w + x] = pal[bin(c)] || near(pal, c);
        }
        int[] q = new int[w * h];
        boolean[] keep = new boolean[w * h];
        int qh = 0, qt = 0;
        for (int y = yTop; y < (int) (ey - 1.0f * d) && y <= yLow; y++) for (int x = x0; x <= x1; x++) {
            int i = y * w + x;
            if (m[i] && !keep[i]) { keep[i] = true; q[qt++] = i; }
        }
        int count = 0, mnx = w, mxx = -1, mny = h, mxy = -1;
        while (qh < qt) {
            int i = q[qh++], x = i % w, y = i / w;
            count++;
            if (x < mnx) mnx = x; if (x > mxx) mxx = x; if (y < mny) mny = y; if (y > mxy) mxy = y;
            for (int k = 0; k < 4; k++) {
                int nx = x + (k == 0 ? -1 : k == 1 ? 1 : 0), ny = y + (k == 2 ? -1 : k == 3 ? 1 : 0);
                if (nx < x0 || nx > x1 || ny < yTop || ny > yLow) continue;
                int j = ny * w + nx;
                if (m[j] && !keep[j]) { keep[j] = true; q[qt++] = j; }
            }
        }
        if (count < d * d * 0.8f || mxx < 0) return null;
        // small gaps inside the headwear (a jewel, a skin-coloured fold) belong to it: fill each row's holes
        for (int y = mny; y <= mxy; y++) {
            int first = -1, last = -1;
            for (int x = mnx; x <= mxx; x++) if (keep[y * w + x]) { if (first < 0) first = x; last = x; }
            if (first < 0) continue;
            boolean above = y < ey - 0.95f * d;
            for (int x = first; x <= last; x++) {
                int i = y * w + x;
                if (keep[i] || (px[i] >>> 24) < 128) continue;
                if (above) keep[i] = true;
            }
        }
        // a little wider, so no coloured fringe of the headwear stays on the bare head
        boolean[] grown = keep.clone();
        int gr = Math.max(1, (int) (d * 0.04f));
        for (int y = mny; y <= mxy; y++) for (int x = mnx; x <= mxx; x++) {
            if (!keep[y * w + x]) continue;
            for (int dy = -gr; dy <= gr; dy++) for (int dx = -gr; dx <= gr; dx++) {
                int xx = x + dx, yy = y + dy;
                if (xx < 0 || yy < 0 || xx >= w || yy >= h || yy > yLow) continue;
                int j = yy * w + xx;
                if ((px[j] >>> 24) > 0 && !skinLike(px[j], skin)) grown[j] = true;
            }
        }
        keep = grown;
        mnx = Math.max(0, mnx - gr); mxx = Math.min(w - 1, mxx + gr); mny = Math.max(0, mny - gr); mxy = Math.min(h - 1, mxy + gr);

        Headwear hw = new Headwear();
        hw.hx0 = mnx; hw.hy0 = mny; hw.hx1 = mxx; hw.hy1 = mxy;
        int cw = mxx - mnx + 1, ch = mxy - mny + 1;
        hw.hat = new int[cw * ch];
        for (int y = 0; y < ch; y++) for (int x = 0; x < cw; x++) {
            int i = (y + mny) * w + x + mnx;
            if (keep[i]) hw.hat[y * cw + x] = px[i];
        }
        hw.relX0 = (mnx - ex) / d; hw.relX1 = (mxx + 1 - ex) / d;
        hw.relY0 = (mny - ey) / d; hw.relY1 = (mxy + 1 - ey) / d;

        // 3. the bare head: the skull is an oval over the face; where the headwear was, inside the oval becomes
        // skin (lit from above, darker towards the edges), outside it becomes see-through
        hw.bare = px.clone();
        float cx = ex, rxk = half * 1.02f, ryk = half * 1.12f, cy = ey - 0.12f * d;
        // the skin of the forehead just under the headwear (the closest match for the bare scalp)
        long fr = 0, fg = 0, fb = 0, fn = 0;
        for (int y = (int) (ey - 0.78f * d); y < (int) (ey - 0.55f * d); y++) for (int x = (int) (ex - 0.5f * d); x <= (int) (ex + 0.5f * d); x++) {
            if (x < 0 || y < 0 || x >= w || y >= h || keep[y * w + x]) continue;
            int c = px[y * w + x];
            if ((c >>> 24) < 200 || !skinLike(c, skin)) continue;
            fr += (c >> 16) & 255; fg += (c >> 8) & 255; fb += c & 255; fn++;
        }
        if (fn > 20) skin = 0xFF000000 | ((int) (fr / fn) << 16) | ((int) (fg / fn) << 8) | (int) (fb / fn);
        int sr = (skin >> 16) & 255, sg = (skin >> 8) & 255, sb = skin & 255;
        for (int y = mny; y <= mxy; y++) for (int x = mnx; x <= mxx; x++) {
            int i = y * w + x;
            if (!keep[i]) continue;
            float u = (x - cx) / rxk, v = (y - cy) / ryk;
            float e = (float) Math.sqrt(u * u + v * v);
            if (e > 1.03f || y > ey + 0.2f * d) { hw.bare[i] = 0; continue; }
            float shade = 1.06f - 0.28f * e * e + 0.1f * Math.max(0, -v - u * 0.3f) * (1 - e);
            float spec = Math.max(0, 1 - ((u + 0.25f) * (u + 0.25f) + (v + 0.62f) * (v + 0.62f)) * 14) * 0.18f;
            int r2 = clamp(sr * shade + 255 * spec), g2 = clamp(sg * shade + 255 * spec), b2 = clamp(sb * shade + 255 * spec);
            int al = e > 0.97f ? (int) (255 * (1.03f - e) / 0.06f) : 255;
            hw.bare[i] = (Math.max(0, Math.min(255, al)) << 24) | (r2 << 16) | (g2 << 8) | b2;
        }
        // high above the eyes, nothing outside the bare skull is left (stray bits of the headwear's edge)
        for (int y = mny; y < Math.min(mxy + 1, (int) (ey - 0.9f * d)); y++) for (int x = mnx; x <= mxx; x++) {
            float u = (x - cx) / rxk, v = (y - cy) / ryk;
            if (u * u + v * v > 1.06f * 1.06f) hw.bare[y * w + x] = 0;
        }
        // no seam where the new scalp meets the real forehead: blend into the pixels just below
        int blend = Math.max(2, (int) (d * 0.14f));
        for (int x = mnx; x <= mxx; x++) {
            int below = -1, dist = 0;
            for (int y = Math.min(h - 1, mxy + blend); y >= mny; y--) {
                int i = y * w + x;
                if (!keep[i]) {
                    if ((px[i] >>> 24) > 200 && skinLike(px[i], skin)) { below = px[i]; dist = 0; } else below = -1;
                    continue;
                }
                dist++;
                if (below == -1 || dist > blend || (hw.bare[i] >>> 24) == 0) continue;
                float k = 1 - dist / (float) blend;
                hw.bare[i] = mixRgb(hw.bare[i], below, k * k);
            }
        }
        return hw;
    }

    static int mixRgb(int a, int b, float k) {
        int r = (int) (((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k), g = (int) (((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = (int) ((a & 255) * (1 - k) + (b & 255) * k);
        return (a & 0xFF000000) | (r << 16) | (g << 8) | bl;
    }

    static int clamp(float v) { return v < 0 ? 0 : v > 255 ? 255 : (int) v; }

    static int bin(int c) { return (((c >> 20) & 15) << 8) | (((c >> 12) & 15) << 4) | ((c >> 4) & 15); }

    /** A colour one step away from the palette (shading of the same cloth). */
    static boolean near(boolean[] pal, int c) {
        int r = (c >> 20) & 15, g = (c >> 12) & 15, b = (c >> 4) & 15;
        for (int dr = -1; dr <= 1; dr++) for (int dg = -1; dg <= 1; dg++) for (int db = -1; db <= 1; db++) {
            int rr = r + dr, gg = g + dg, bb = b + db;
            if (rr < 0 || gg < 0 || bb < 0 || rr > 15 || gg > 15 || bb > 15) continue;
            if (pal[(rr << 8) | (gg << 4) | bb]) return true;
        }
        return false;
    }

    static boolean skinLike(int c, int skin) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        int sr = (skin >> 16) & 255, sg = (skin >> 8) & 255, sb = skin & 255;
        // the same hue as the face's skin, lighter or darker (shadows and highlights on the forehead)
        float l = (r + g + b) / 3f + 1, sl = (sr + sg + sb) / 3f + 1;
        float dr = r / l - sr / sl, dg = g / l - sg / sl, db = b / l - sb / sl;
        return Math.sqrt(dr * dr + dg * dg + db * db) < 0.16 && l > sl * 0.45f && l < sl * 1.5f;
    }

    static boolean hairLike(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        return r + g + b < 170 && Math.max(r, Math.max(g, b)) - Math.min(r, Math.min(g, b)) < 45;
    }
}
