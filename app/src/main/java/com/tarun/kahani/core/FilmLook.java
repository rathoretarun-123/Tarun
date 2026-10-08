package com.tarun.kahani.core;

/**
 * The cinematic finish of every frame, like the final grade of an animated feature:
 *  - a filmic tone curve (gently lifted blacks, a soft S for depth, highlights that roll off instead of clipping),
 *  - a soft glow around the bright parts (bloom: sunlight, lamps, sparkles, white clothes),
 *  - richer colour where it is dull (vibrance) without over-saturating what is already colourful,
 *  - split toning: shadows a touch cool, highlights a touch warm,
 *  - and a colour script: warmth, saturation and contrast follow the mood of each part of the story (warm and
 *    bright for happy moments, cooler and quieter for sad ones, harder for danger), blended across cuts.
 * Works on finished ARGB pixels; one instance per drawing thread (it keeps its own buffers).
 */
public final class FilmLook {

    /** The look of one moment of the film. */
    public static final class Params {
        public float warmth;          // -1 cool .. +1 warm
        public float saturation = 1;  // 1 = as drawn
        public float contrast = 1;    // 1 = the base filmic curve
        public float bloom = 0.14f;   // strength of the glow
        public Params set(Params o) { warmth = o.warmth; saturation = o.saturation; contrast = o.contrast; bloom = o.bloom; return this; }
    }

    private final int w, h, sw, sh;
    private final int[] small;
    private final int[] lutR = new int[256], lutG = new int[256], lutB = new int[256];
    private final Params last = new Params();
    private boolean built;

    public FilmLook(int w, int h) {
        this.w = w; this.h = h;
        this.sw = Math.max(4, w / 4); this.sh = Math.max(4, h / 4);
        this.small = new int[sw * sh];
    }

    /** The look for a moment of the film: the colour script, from the part's mood and time of day. */
    public static Params forSeg(Film.Seg s, Params out) {
        out.warmth = 0.08f; out.saturation = 1.03f; out.contrast = 1f; out.bloom = 0.14f;
        if (s == null) return out;
        switch (s.mood) {
            case Film.M_HAPPY: case Film.M_CELEBRATE: case Film.M_PLAYFUL:
                out.warmth = 0.25f; out.saturation = 1.06f; out.bloom = 0.2f; break;
            case Film.M_SAD:
                out.warmth = -0.3f; out.saturation = 0.86f; out.contrast = 0.96f; out.bloom = 0.1f; break;
            case Film.M_TENSE: case Film.M_VILLAIN:
                out.warmth = -0.25f; out.saturation = 0.92f; out.contrast = 1.1f; out.bloom = 0.07f; break;
            case Film.M_NIGHT:
                out.warmth = -0.4f; out.saturation = 0.9f; out.contrast = 1.04f; out.bloom = 0.16f; break;
            case Film.M_ACTION:
                out.warmth = 0.15f; out.saturation = 1.06f; out.contrast = 1.1f; out.bloom = 0.1f; break;
            default:
        }
        if (s.tod == Sets.EVENING) { out.warmth += 0.25f; out.bloom += 0.04f; }
        else if (s.tod == Sets.MORNING) out.warmth += 0.08f;
        else if (s.tod == Sets.NIGHT) out.warmth -= 0.15f;
        if (s.festive) { out.saturation += 0.04f; out.bloom += 0.05f; }
        out.warmth = Math.max(-0.5f, Math.min(0.4f, out.warmth));
        return out;
    }

    /** The look at time t, blended over a second around each change of part (no sudden colour jumps). */
    public static Params at(Film film, float t, Params out) {
        if (film == null) return forSeg(null, out);
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
            lutR[i] = clamp((y + 0.025f * hi * (0.6f + wm) - 0.01f * lo * (0.5f - wm * 0.3f) + 0.02f * wm * y) * 255);
            lutG[i] = clamp((y + 0.008f * hi * (0.6f + wm)) * 255);
            lutB[i] = clamp((y - 0.022f * hi * (0.6f + wm) + 0.018f * lo * (0.6f - wm * 0.5f) - 0.025f * wm * y) * 255);
        }
        last.set(p);
        built = true;
    }

    static int clamp(float v) { int x = Math.round(v); return x < 0 ? 0 : x > 255 ? 255 : x; }

    /** Applies the finish to a frame (ARGB, w x h). */
    public void apply(int[] px, Params p) {
        if (!built || Math.abs(p.warmth - last.warmth) > 0.01f || Math.abs(p.contrast - last.contrast) > 0.01f) build(p);
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
                int k = l <= 170 ? 0 : (l - 170) * 3;             // only what is bright glows
                if (k > 255) k = 255;
                small[y * sw + x] = 0xFF000000 | ((r * k >> 8) << 16) | ((g * k >> 8) << 8) | (b * k >> 8);
            }
            Blur.gauss(small, sw, sh, Math.max(2, sw / 70));
        }
        int bl = Math.round(p.bloom * 256);
        float vib = 0.16f * p.saturation, satK = p.saturation;
        for (int y = 0; y < h; y++) {
            float fy = (y + 0.5f) / 4 - 0.5f;
            int sy0 = Math.max(0, Math.min(sh - 1, (int) Math.floor(fy))), sy1 = Math.min(sh - 1, sy0 + 1);
            float wy = Math.max(0, Math.min(1, fy - sy0));
            for (int x = 0; x < w; x++) {
                int i = y * w + x, c = px[i];
                int R = lutR[(c >> 16) & 255], G = lutG[(c >> 8) & 255], B = lutB[c & 255];
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
