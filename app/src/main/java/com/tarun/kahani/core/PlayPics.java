package com.tarun.kahani.core;

import java.util.List;

/**
 * v41: the playground things of the user's own pictures — a slide seen from the side and a swing's seat seen from the
 * side — read from their cut-out shapes, so the film's slide and swing are the ones the user gave (the drawn ones when
 * none of their pictures shows the thing from the side).
 */
public final class PlayPics {
    private PlayPics() { }

    static final String[] SLIDE_KEYS = {"फिसलपट्टी", "फिसल पट्टी", "फिसलन पट्टी", "स्लाइड", "slide"};
    static final String[] SWING_KEYS = {"झूला", "झूले", "swing"};
    static final String[] ROUND_KEYS = {"गोल झूला", "चकरी", "merry", "roundabout"};

    /** The slide of the film: the user's side view of their slide when there is one, else the drawn slide. */
    public static Props.SlideGeo slide(Art art) {
        Props.SlideGeo g = art == null ? null : art.slideGeo;
        return g != null ? g : Props.SlideGeo.drawn();
    }

    /** The user's side view of the swing's seat (its chains meeting above it), or null. */
    public static Art.Thing seat(Art art) {
        return art == null ? null : art.swingSeat;
    }

    /** Reads the pictures of the story's things ({key, file}) for a slide and a swing seat seen from the side. */
    static void read(Art art, Art.Loader L, List<String[]> things) {
        float bestSlide = 0, bestSeat = 0;
        Props.SlideGeo slideGeo = null;
        int[] slidePx = null;
        int slideW = 0, slideH = 0, seatColor = 0;
        String seatFile = null, slideFile = null;
        float[] seatBox = null;
        for (String[] t : things) {
            String key = t[0], file = t[1];
            boolean slide = Txt.has(key, SLIDE_KEYS), swing = !slide && Txt.has(key, SWING_KEYS) && !Txt.has(key, ROUND_KEYS);
            if (!slide && !swing) continue;
            int[] d = L.decode(file, slide ? 900 : 700);
            if (d == null) continue;
            int w = d[0], h = d[1];
            int[] px = new int[w * h];
            System.arraycopy(d, 2, px, 0, px.length);
            if (slide) floorShadow(px, w, h);
            boolean[] on = new boolean[w * h];
            int n = 0;
            for (int i = 0; i < on.length; i++) if (on[i] = (px[i] >>> 24) > 100) n++;
            if (n == 0 || n > on.length * 0.92f) continue;           // not cut out: a picture of a scene, not a thing
            if (slide) {
                Object[] r = slideSide(on, w, h);
                if (r != null && (Float) r[0] > bestSlide) { bestSlide = (Float) r[0]; slideGeo = (Props.SlideGeo) r[1]; slidePx = px; slideW = w; slideH = h; slideFile = file; }
            } else {
                float[] r = seatSide(on, w, h);
                if (r != null && r[0] > bestSeat) { bestSeat = r[0]; seatBox = r; seatFile = file; seatColor = mainColor(px); }
            }
        }
        if (slideGeo != null) {
            slideGeo.pic = L.create(slidePx, slideW, slideH);
            if (slideGeo.pic != null) { art.slideGeo = slideGeo; art.notes.add("the slide: your side view of it (" + slideFile + ")"); }
        }
        if (seatBox != null) {
            Art.Thing th = new Art.Thing();
            th.img = L.load(seatFile, 700);
            if (th.img != null) {
                th.w = L.width(th.img); th.h = L.height(th.img);
                th.bx0 = seatBox[1]; th.by0 = seatBox[2]; th.bx1 = seatBox[3]; th.by1 = seatBox[4];
                th.color = seatColor;
                art.swingSeat = th;
                art.notes.add("the swing's seat: your side view of it (" + seatFile + ")");
            }
        }
    }

    /**
     * The soft grey floor a studio picture of a thing keeps under it (light, colourless, along the bottom) becomes a
     * see-through dark shadow: on the place's grass it is a shadow, not a pale patch of studio floor.
     */
    static void floorShadow(int[] px, int w, int h) {
        int y1 = -1, y0 = h;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if ((px[y * w + x] >>> 24) > 100) { y0 = Math.min(y0, y); y1 = Math.max(y1, y); break; }
        if (y1 < 0) return;
        int band = Math.max(4, Math.round((y1 - y0 + 1) * 0.12f));
        for (int y = Math.max(0, y1 - band); y <= y1; y++) for (int x = 0; x < w; x++) {
            int c = px[y * w + x];
            if ((c >>> 24) <= 20) continue;
            int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
            int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b)), lum = (r * 299 + g * 587 + b * 114) / 1000;
            if (mx - mn > 18 || lum < 150 || lum >= 232) continue;
            int a = Math.max(0, Math.min(105, Math.round((245 - lum) * 1.4f)));
            px[y * w + x] = a << 24;
        }
    }

    /** The colour a thing is mostly painted in (its saturated pixels' mean), or 0. */
    static int mainColor(int[] px) {
        long r = 0, g = 0, b = 0, n = 0;
        for (int c : px) {
            if ((c >>> 24) <= 100) continue;
            int cr = (c >> 16) & 255, cg = (c >> 8) & 255, cb = c & 255;
            if (Math.max(cr, Math.max(cg, cb)) - Math.min(cr, Math.min(cg, cb)) < 70) continue;
            r += cr; g += cg; b += cb; n++;
        }
        if (n < 50) return 0;
        return 0xFF000000 | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
    }

    /**
     * A slide seen from the side: its top edge falls in one long, even slope from the platform to the run-out (the
     * chute), at least two fifths of its width and a third of its height. Returns {score, geometry} or null.
     * The geometry is in shares of the child's height (the platform is SLIDE_TOP of it high), its anchor the top of
     * the chute, x along the chute's direction.
     */
    static Object[] slideSide(boolean[] on, int w, int h) {
        int x0 = w, x1 = -1, y0 = h, y1 = -1;
        int[] top = new int[w];
        for (int x = 0; x < w; x++) {
            top[x] = -1;
            for (int y = 0; y < h; y++) if (on[y * w + x]) { top[x] = y; break; }
            if (top[x] >= 0) { x0 = Math.min(x0, x); x1 = Math.max(x1, x); }
        }
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (on[y * w + x]) { y0 = Math.min(y0, y); y1 = Math.max(y1, y); break; }
        int bw = x1 - x0 + 1, bh = y1 - y0 + 1;
        if (bw < 40 || bh < 40 || bw < bh * 0.9f) return null;
        // the top edge smoothed (a median of 5 columns)
        float[] sm = new float[w];
        for (int x = x0; x <= x1; x++) {
            int[] v = new int[5];
            int k = 0;
            for (int dx = -2; dx <= 2; dx++) { int xx = Math.max(x0, Math.min(x1, x + dx)); v[k++] = top[xx] < 0 ? y1 : top[xx]; }
            java.util.Arrays.sort(v);
            sm[x] = v[2];
        }
        // the run-out is the lower end; walk from it toward the other end while the edge keeps rising gently
        float left = 0, right = 0;
        int band = Math.max(3, bw / 6);
        for (int i = 0; i < band; i++) { left += sm[x0 + i]; right += sm[x1 - i]; }
        int dir = left > right ? 1 : -1;                       // +1: the run-out is at the left (the chute runs down to the left)
        int start = dir > 0 ? x0 : x1, step = dir > 0 ? 1 : -1;
        // a soft floor shadow left round the feet is no part of the chute: start where something stands up
        while (start >= x0 && start <= x1 && sm[start] > y1 - bh * 0.12f) start += step;
        if (start < x0 || start > x1) return null;
        int run = 0, xTop = start;
        float prev = sm[start], climb = 0;
        int flat = 0;
        for (int x = start + step; x >= x0 && x <= x1; x += step) {
            float dy = prev - sm[x];                           // positive: the edge rises toward the platform
            if (dy < -bh * 0.02f) break;                       // it falls again: past the top
            if (dy > bh * 0.06f) break;                        // a sudden jump up (the hood, a rail): the chute ends here
            if (dy <= 0.2f) flat++; else flat = 0;
            if (flat > bw * 0.12f && run > bw * 0.2f) break;   // a long level stretch after the slope: the platform
            climb += Math.max(0, dy);
            prev = sm[x];
            run++;
            xTop = x;
        }
        if (run < bw * 0.4f || climb < bh * 0.3f) return null;
        // where the chute starts its run-out (its lowest third), and the ground (the lowest point of the picture)
        int xEnd = start;
        float groundY = y1, topY = sm[xTop], endY = sm[start];
        float deck = groundY - topY;
        if (deck < bh * 0.25f) return null;
        float u = Props.SLIDE_TOP / deck;                       // picture pixels → shares of the child's height
        Props.SlideGeo g = new Props.SlideGeo();
        // chute coordinates: 0 at the top of the chute, positive toward its end
        int sgn = dir > 0 ? -1 : 1;                             // image x grows toward the run-out when sgn = +1
        float back = sgn > 0 ? (xTop - x0) : (x1 - xTop);        // the platform and what is behind the chute's top
        g.sitX = 0.04f;
        g.deckX = -Math.min(0.2f, back * u * 0.5f);
        g.deckY = Props.SLIDE_TOP;
        g.rungX = -back * u - 0.02f;
        g.footX = g.rungX - 0.2f;
        g.endX = Math.abs(xEnd - xTop) * u;
        g.endY = Math.max(0.05f, (groundY - endY) * u);
        g.landX = g.endX + 0.32f;
        for (int i = 0; i <= 32; i++) {
            float xc = g.sitX + (g.endX - g.sitX) * i / 32f;
            int ix = Math.round(xTop + sgn * xc / u);
            ix = Math.max(x0, Math.min(x1, ix));
            g.surf[i] = Math.max(0, (groundY - sm[ix]) * u - 0.02f);
        }
        g.surf[0] = Math.min(g.surf[0], g.deckY);
        g.picFlip = sgn < 0;
        g.picW = w * u;
        g.picH = h * u;
        g.picTop = groundY * u;
        g.picX = sgn > 0 ? -xTop * u : (xTop - w) * u;
        float score = run / (float) bw + climb / bh;
        return new Object[]{score, g};
    }

    /**
     * A swing's seat seen from the side: its chains come together above it (the top of the picture a narrow band
     * of chain, the seat below wider). Returns {score, box x0, y0, x1, y1 (shares of the picture)} or null.
     */
    static float[] seatSide(boolean[] on, int w, int h) {
        int x0 = w, x1 = -1, y0 = h, y1 = -1;
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if (on[y * w + x]) { x0 = Math.min(x0, x); x1 = Math.max(x1, x); y0 = Math.min(y0, y); y1 = Math.max(y1, y); }
        int bw = x1 - x0 + 1, bh = y1 - y0 + 1;
        if (bw < 10 || bh < 40 || bh < bw * 1.2f) return null;           // a hanging seat is taller than wide
        int tLo = w, tHi = -1, bLo = w, bHi = -1;
        for (int y = y0; y < y0 + Math.max(2, bh * 0.08f); y++) for (int x = x0; x <= x1; x++) if (on[y * w + x]) { tLo = Math.min(tLo, x); tHi = Math.max(tHi, x); }
        for (int y = y1 - Math.max(2, (int) (bh * 0.1f)); y <= y1; y++) for (int x = x0; x <= x1; x++) if (on[y * w + x]) { bLo = Math.min(bLo, x); bHi = Math.max(bHi, x); }
        if (tHi < 0 || bHi < 0) return null;
        float topSpan = (tHi - tLo + 1) / (float) bw, seatSpan = (bHi - bLo + 1) / (float) bw;
        if (topSpan > 0.4f || seatSpan < 0.6f) return null;               // two chains far apart: seen from the front
        float score = 1 - topSpan;
        return new float[]{score, x0 / (float) w, y0 / (float) h, (x1 + 1) / (float) w, (y1 + 1) / (float) h};
    }
}
