package com.tarun.kahani.core;

/**
 * The cinematic finish of every frame, like the final grade of an animated feature:
 *  - a filmic tone curve (gently lifted blacks, a soft S for depth, highlights that roll off instead of clipping),
 *  - a soft glow around the bright parts (bloom: sunlight, lamps, sparkles, white clothes),
 *  - richer colour where it is dull (vibrance) without over-saturating what is already colourful,
 *  - split toning: shadows a touch cool, highlights a touch warm,
 *  - and a colour script: warmth, saturation and contrast follow the mood of each part of the story (warm and
 *    bright for happy moments, cooler and quieter for sad ones, harder for danger), blended across cuts,
 *  - v35: output sharpening (an unsharp mask on the light only): the fine detail that enlarged pictures and the
 *    video encoder soften is lifted back; noise and smooth skin are left alone and hard edges get no halo.
 * Works on finished ARGB pixels; one instance per drawing thread (it keeps its own buffers).
 */
public final class FilmLook {

    /** The look of one moment of the film. */
    public static final class Params {
        public float warmth;          // -1 cool .. +1 warm
        public float saturation = 1;  // 1 = as drawn
        public float contrast = 1;    // 1 = the base filmic curve
        public float bloom = 0.14f;   // strength of the glow
        public float green;           // -1 magenta .. +1 green tint (the colour script: a cave is green, a night is blue)
        public Params set(Params o) { warmth = o.warmth; saturation = o.saturation; contrast = o.contrast; bloom = o.bloom; green = o.green; return this; }
    }

    private final int w, h, sw, sh;
    private final int[] small;
    /** v35: the light of five rows of the frame, as it was, and the same rows blurred across (a ring, for the unsharp mask). */
    private final int[] lumRing, blurRing;
    /** v35: how strongly the detail is lifted (x256): 0.6 at 1080p, less at smaller sizes, where pictures are not enlarged. */
    private final int sharpen;
    private final int[] lutR = new int[256], lutG = new int[256], lutB = new int[256];
    private final Params last = new Params();
    private boolean built;

    public FilmLook(int w, int h) {
        this.w = w; this.h = h;
        this.sw = Math.max(4, w / 4); this.sh = Math.max(4, h / 4);
        this.small = new int[sw * sh];
        this.lumRing = new int[w * 5];
        this.blurRing = new int[w * 5];
        this.sharpen = Math.round(256 * Math.max(0.25f, Math.min(0.6f, 0.25f + h / 1080f * 0.35f)));
    }

    /** Row y of the frame as it is now (before it is finished): its light, and its light blurred across (1 4 6 4 1). */
    private void readRow(int[] px, int y) {
        int o = (y % 5) * w, row = y * w;
        for (int x = 0; x < w; x++) {
            int c = px[row + x];
            lumRing[o + x] = (((c >> 16) & 255) * 77 + ((c >> 8) & 255) * 150 + (c & 255) * 29) >> 8;
        }
        int last = w - 1;
        for (int x = 0; x < w; x++) {
            int a = lumRing[o + Math.max(0, x - 2)], b = lumRing[o + Math.max(0, x - 1)], c = lumRing[o + x],
                    d = lumRing[o + Math.min(last, x + 1)], e = lumRing[o + Math.min(last, x + 2)];
            blurRing[o + x] = a + 4 * b + 6 * c + 4 * d + e;            // x16
        }
    }

    /** The look for a moment of the film: the colour script, from the place, the time of day, the mood and the act of the part. */
    public static Params forSeg(Film.Seg s, Params out) {
        out.warmth = 0.08f; out.saturation = 1.03f; out.contrast = 1f; out.bloom = 0.14f; out.green = 0;
        if (s == null) return out;
        // the colour script (Deakins): the place and the time of day dictate the colour, one palette per act
        PixarLead.colorScript(s.set, s.tod, s.mood, s.act, out);
        if (s.festive) { out.saturation += 0.04f; out.bloom += 0.05f; }
        out.warmth = Math.max(-0.5f, Math.min(0.45f, out.warmth));
        return out;
    }

    /**
     * The look at time t, blended over a second around each change of part (no sudden colour jumps); v38: with the
     * colour of the beat over it (FilmCraft.beatLook: richer at the peak of a scene, warmer in a tender shot).
     */
    public static Params at(Film film, float t, Params out) {
        if (film == null) return forSeg(null, out);
        return FilmCraft.beatLook(film, t, scriptAt(film, t, out));
    }

    /** The colour script alone at time t. */
    static Params scriptAt(Film film, float t, Params out) {
        Film.Seg s = film.segAt(t);
        forSeg(s, out);
        if (s == null) return out;
        int i = film.segs.indexOf(s);
        float into = t - s.t0;
        if (into < 1f && i > 0) {
            Params p = forSeg(film.segs.get(i - 1), new Params());
            float k = 0.5f + 0.5f * into;      // half way at the cut, fully the new look a second later
            mix(p, out, k, out);
        }
        return out;
    }

    static void mix(Params a, Params b, float k, Params out) {
        out.warmth = a.warmth + (b.warmth - a.warmth) * k;
        out.saturation = a.saturation + (b.saturation - a.saturation) * k;
        out.contrast = a.contrast + (b.contrast - a.contrast) * k;
        out.bloom = a.bloom + (b.bloom - a.bloom) * k;
        out.green = a.green + (b.green - a.green) * k;
    }

    private void build(Params p) {
        for (int i = 0; i < 256; i++) {
            float x = i / 255f;
            // filmic curve: lifted toe, soft S around the middle, a shoulder that rolls the highlights off
            float y = 0.012f + x * 0.988f;
            float s = y * y * (3 - 2 * y);
            y = y + (s - y) * 0.22f * p.contrast + (p.contrast - 1) * (y - 0.5f) * 0.5f;
            if (y > 0.82f) y = 0.82f + (1 - (float) Math.exp(-(y - 0.82f) / 0.18f)) * 0.17f;
            y = Math.max(0, Math.min(1, y));
            // split toning: highlights warmer, shadows a touch cooler (more so in a warm or cool moment)
            float hi = Math.max(0, y - 0.5f) * 2, lo = Math.max(0, 0.5f - y) * 2;
            float wm = p.warmth;
            float gn = p.green;      // green tint: the mid tones lean green (a cave), or magenta when negative
            lutR[i] = clamp((y + 0.025f * hi * (0.6f + wm) - 0.01f * lo * (0.5f - wm * 0.3f) + 0.02f * wm * y - 0.012f * gn * y) * 255);
            lutG[i] = clamp((y + 0.008f * hi * (0.6f + wm) + 0.03f * gn * y * (1 - y) * 2) * 255);
            lutB[i] = clamp((y - 0.022f * hi * (0.6f + wm) + 0.018f * lo * (0.6f - wm * 0.5f) - 0.025f * wm * y - 0.012f * gn * y) * 255);
        }
        last.set(p);
        built = true;
    }

    static int clamp(float v) { int x = Math.round(v); return x < 0 ? 0 : x > 255 ? 255 : x; }

    /** Applies the finish to a frame (ARGB, w x h). */
    public void apply(int[] px, Params p) {
        if (!built || Math.abs(p.warmth - last.warmth) > 0.01f || Math.abs(p.contrast - last.contrast) > 0.01f || Math.abs(p.green - last.green) > 0.01f) build(p);
        // ---- bloom: the bright parts, at a quarter of the size, blurred softly
        boolean glow = p.bloom > 0.01f;
        if (glow) {
            for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) {
                int r = 0, g = 0, b = 0;
                int y0 = y * 4, x0 = x * 4;
                for (int dy = 0; dy < 4; dy++) {
                    int row = Math.min(h - 1, y0 + dy) * w;
                    for (int dx = 0; dx < 4; dx++) {
                        int c = px[row + Math.min(w - 1, x0 + dx)];
                        r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255;
                    }
                }
                r >>= 4; g >>= 4; b >>= 4;
                int l = (r * 77 + g * 150 + b * 29) >> 8;
                // only what is really bright glows (v35: from 200, not 170 — a sky or a white wall made a haze
                // over the whole frame that read as blur)
                int k = l <= 200 ? 0 : (l - 200) * 5;
                if (k > 255) k = 255;
                small[y * sw + x] = 0xFF000000 | ((r * k >> 8) << 16) | ((g * k >> 8) << 8) | (b * k >> 8);
            }
            Blur.gauss(small, sw, sh, Math.max(2, sw / 70));
        }
        int bl = Math.round(p.bloom * 0.6f * 256);       // v35: a gentler glow (the same colour script, 60% of its strength)
        float vib = 0.16f * p.saturation, satK = p.saturation;
        // v35: the rows the unsharp mask needs, read before they are finished (row y + 2 is read when row y is done)
        readRow(px, 0);
        if (h > 1) readRow(px, 1);
        for (int y = 0; y < h; y++) {
            if (y + 2 < h) readRow(px, y + 2);
            int r0 = (Math.max(0, y - 2) % 5) * w, r1 = (Math.max(0, y - 1) % 5) * w, r2 = (y % 5) * w,
                    r3 = (Math.min(h - 1, y + 1) % 5) * w, r4 = (Math.min(h - 1, y + 2) % 5) * w;
            float fy = (y + 0.5f) / 4 - 0.5f;
            int sy0 = Math.max(0, Math.min(sh - 1, (int) Math.floor(fy))), sy1 = Math.min(sh - 1, sy0 + 1);
            float wy = Math.max(0, Math.min(1, fy - sy0));
            for (int x = 0; x < w; x++) {
                int i = y * w + x, c = px[i];
                int cr = (c >> 16) & 255, cg = (c >> 8) & 255, cb = c & 255;
                // the unsharp mask: the light minus its 5x5 blur is the fine detail; above two levels (grain, noise and
                // smooth skin stay as they are) it is lifted, never by more than 20 levels (no halo at a hard edge)
                int blur = blurRing[r0 + x] + 4 * blurRing[r1 + x] + 6 * blurRing[r2 + x] + 4 * blurRing[r3 + x] + blurRing[r4 + x];   // x256
                int detail = (lumRing[r2 + x] << 8) - blur, mag = Math.abs(detail) - 512;
                if (mag > 0) {
                    int lift = (Math.min(mag, 20 << 8) * sharpen) >> 16;
                    if (detail < 0) lift = -lift;
                    cr = cr + lift < 0 ? 0 : cr + lift > 255 ? 255 : cr + lift;
                    cg = cg + lift < 0 ? 0 : cg + lift > 255 ? 255 : cg + lift;
                    cb = cb + lift < 0 ? 0 : cb + lift > 255 ? 255 : cb + lift;
                }
                int R = lutR[cr], G = lutG[cg], B = lutB[cb];
                // vibrance: dull colours get richer, already rich ones much less
                int mx = Math.max(R, Math.max(G, B)), mn = Math.min(R, Math.min(G, B));
                float chroma = (mx - mn) / 255f;
                float f = satK + vib * (1 - chroma) * (satK >= 0.95f ? 1f : 0.3f);
                if (f != 1f) {
                    int yy = (R * 77 + G * 150 + B * 29) >> 8;
                    R = clamp(yy + (R - yy) * f); G = clamp(yy + (G - yy) * f); B = clamp(yy + (B - yy) * f);
                }
                if (glow) {
                    float fx = (x + 0.5f) / 4 - 0.5f;
                    int sx0 = Math.max(0, Math.min(sw - 1, (int) Math.floor(fx))), sx1 = Math.min(sw - 1, sx0 + 1);
                    float wx = Math.max(0, Math.min(1, fx - sx0));
                    int a = small[sy0 * sw + sx0], b2 = small[sy0 * sw + sx1], cc = small[sy1 * sw + sx0], d = small[sy1 * sw + sx1];
                    float gr = lerp2((a >> 16) & 255, (b2 >> 16) & 255, (cc >> 16) & 255, (d >> 16) & 255, wx, wy);
                    float gg = lerp2((a >> 8) & 255, (b2 >> 8) & 255, (cc >> 8) & 255, (d >> 8) & 255, wx, wy);
                    float gb = lerp2(a & 255, b2 & 255, cc & 255, d & 255, wx, wy);
                    // screen blend: light adds, never past white
                    int ar = (int) (gr * bl) >> 8, ag = (int) (gg * bl) >> 8, ab = (int) (gb * bl) >> 8;
                    R = 255 - (255 - R) * (255 - ar) / 255;
                    G = 255 - (255 - G) * (255 - ag) / 255;
                    B = 255 - (255 - B) * (255 - ab) / 255;
                }
                px[i] = (c & 0xFF000000) | (R << 16) | (G << 8) | B;
            }
        }
    }

    private static float lerp2(int a, int b, int c, int d, float wx, float wy) {
        return (a + (b - a) * wx) * (1 - wy) + (c + (d - c) * wx) * wy;
    }
}
