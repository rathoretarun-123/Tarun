package com.tarun.kahani.core;

/**
 * v34: the approval checklist of the "Pixar-Level 3D Still Picture Maker" training manual (§2 the five criteria,
 * §15 the QA checklist), measured on every picture the 3D maker makes, so a proposal comes with what passed and
 * what did not, and a doll that fails on light is rendered again before it is offered:
 * <ol>
 *   <li>skin glows, not plastic — few clipped highlights on the skin, and the skin's shadow side warmer than its
 *       lit side (light bleeding through skin turns red in the shade);</li>
 *   <li>expressive eyes with a catch-light — a bright highlight inside each iris;</li>
 *   <li>soft light with ambient occlusion, not flat and not harsh — the figure's tones spread (not one flat value),
 *       few crushed blacks and few blown whites;</li>
 *   <li>the background in depth of field — for a place: the far part softer than the stage (a character is drawn
 *       on its own; the film gives the depth);</li>
 *   <li>like its reference — the colours of the picture it was made from (hue similarity of at least 0.75);</li>
 *   <li>no deformed hands — a doll's hands are built from geometry (always whole); a made picture is checked by eye;</li>
 *   <li>resolution — at least 2048 px on the long side.</li>
 * </ol>
 * Every measure is a share or a ratio, so it does not depend on the picture's size.
 */
public final class StillQa {
    private StillQa() {}

    public static final class Result {
        public int score;
        public boolean flat, harsh, dark, catchLights, warmSkin;
        public final java.util.List<String> lines = new java.util.ArrayList<String>();
        /** One line for the proposal (no "|": the proposal's fields are split on it). */
        public String summary() {
            StringBuilder b = new StringBuilder("Still QA ").append(score).append("/100:");
            for (String l : lines) b.append(' ').append(l).append(';');
            return b.toString().replace('|', '/');
        }
    }

    static float lum(int c) { return 0.299f * ((c >> 16) & 255) + 0.587f * ((c >> 8) & 255) + 0.114f * (c & 255); }

    /**
     * eyes: {eyeLX, eyeLY, eyeRX, eyeRY, eyeR} as fractions of the picture (eyeR of its width), or null when the face
     * is unknown; refHue: the reference picture's hue histogram (PicSense.Info.hue), or null; place: a background
     * plate (opaque) rather than a cut-out figure; hands: HANDS_GEOMETRY (a doll), HANDS_SOURCE (the user's own
     * picture, recoloured) or HANDS_LOOK (a picture from a service: checked by eye before approval).
     */
    public static final int HANDS_GEOMETRY = 0, HANDS_SOURCE = 1, HANDS_LOOK = 2;

    public static Result check(int[] px, int w, int h, float[] eyes, float[] refHue, boolean place, int hands) {
        Result r = new Result();
        int applicable = 0, passed = 0;
        // ---- the figure's tones
        long n = 0; double sum = 0, sum2 = 0; long black = 0, white = 0;
        int[] hist = new int[256];
        for (int i = 0; i < px.length; i += 2) {
            int c = px[i];
            if ((c >>> 24) < 200) continue;
            float l = lum(c);
            hist[Math.min(255, (int) l)]++;
            n++; sum += l; sum2 += l * l;
            if (l < 14) black++;
            if (l > 248) white++;
        }
        float mean = n == 0 ? 0 : (float) (sum / n), sd = n == 0 ? 0 : (float) Math.sqrt(Math.max(0, sum2 / n - mean * mean));
        float blackS = n == 0 ? 0 : black / (float) n, whiteS = n == 0 ? 0 : white / (float) n;
        int p90 = 0;
        for (long acc = 0; p90 < 255; p90++) { acc += hist[p90]; if (acc >= n * 0.9f) break; }
        // ---- 1. the skin: clipped share and warm shadows
        long skinN = 0, clipped = 0;
        double lr = 0, lg = 0, dr = 0, dg = 0; long ln = 0, dn = 0;
        java.util.List<Float> skinL = new java.util.ArrayList<Float>();
        for (int i = 0; i < px.length; i += 3) {
            int c = px[i];
            if ((c >>> 24) < 200 || !Cutout.isSkin(c)) continue;
            skinL.add(lum(c));
        }
        float med = 0;
        if (!skinL.isEmpty()) { java.util.Collections.sort(skinL); med = skinL.get(skinL.size() / 2); }
        for (int i = 0; i < px.length; i += 3) {
            int c = px[i];
            if ((c >>> 24) < 200 || !Cutout.isSkin(c)) continue;
            skinN++;
            int rr = (c >> 16) & 255, gg = (c >> 8) & 255, bb = c & 255;
            if (rr > 250 && gg > 250 && bb > 245) clipped++;
            if (lum(c) >= med) { lr += rr; lg += gg; ln++; } else { dr += rr; dg += gg; dn++; }
        }
        if (skinN > 30 && ln > 0 && dn > 0) {
            applicable++;
            float warmLit = (float) (lr / Math.max(1, lg)), warmShade = (float) (dr / Math.max(1, dg));
            r.warmSkin = clipped / (float) skinN < 0.02f && warmShade >= warmLit * 0.98f;
            if (r.warmSkin) { passed++; r.lines.add("✔ skin glows (warm shadows, no plastic shine)"); }
            else r.lines.add(String.format(java.util.Locale.US, "✖ skin reads plastic (shine %.1f%%, shade warmth %.2f vs %.2f)", clipped * 100f / skinN, warmShade, warmLit));
        }
        // ---- 2. the eyes: a catch-light inside each iris
        if (eyes != null && eyes[4] > 0) {
            applicable++;
            int found = 0;
            for (int e = 0; e < 2; e++) {
                float ex = eyes[e * 2] * w, ey = eyes[e * 2 + 1] * h, rad = Math.max(2f, eyes[4] * w * 0.55f);
                float darkest = 255, brightest = 0;
                for (int y = Math.round(ey - rad); y <= ey + rad; y++) for (int x = Math.round(ex - rad); x <= ex + rad; x++) {
                    if (x < 0 || y < 0 || x >= w || y >= h || (x - ex) * (x - ex) + (y - ey) * (y - ey) > rad * rad) continue;
                    int c = px[y * w + x];
                    if ((c >>> 24) < 200) continue;
                    float l = lum(c);
                    darkest = Math.min(darkest, l); brightest = Math.max(brightest, l);
                }
                if (darkest < 90 && brightest > 200) found++;               // a dark iris with a bright highlight in it
            }
            r.catchLights = found == 2;
            if (r.catchLights) { passed++; r.lines.add("✔ eyes with catch-lights"); }
            else r.lines.add("✖ eyes without a catch-light (" + found + " of 2)");
        }
        // ---- 3. the light: soft, not flat, not harsh
        applicable++;
        r.flat = sd < 10 || sd < mean * 0.12f;                       // the spread against the figure's own level: a dark fur coat is not flat
        r.harsh = blackS > 0.10f || whiteS > 0.08f;
        r.dark = mean < 70 && p90 < 105;                               // dark colours are fine when the light still reaches them (their brightest tenth lit)
        if (!r.flat && !r.harsh && !r.dark) { passed++; r.lines.add(String.format(java.util.Locale.US, "✔ soft light with depth (tone spread %.0f)", sd)); }
        else r.lines.add(r.flat ? String.format(java.util.Locale.US, "✖ flat light (tone spread %.0f)", sd) : r.dark ? String.format(java.util.Locale.US, "✖ too dark (mean %.0f, brightest tenth %d)", mean, p90)
                : String.format(java.util.Locale.US, "✖ harsh light (%.0f%% black, %.0f%% white)", blackS * 100, whiteS * 100));
        // ---- 4. depth of field: a place's far part softer than its stage
        if (place && w > 32 && h > 32) {
            applicable++;
            float far = sharpness(px, w, h, 0.1f, 0.4f), stage = sharpness(px, w, h, 0.55f, 0.85f);
            if (far <= stage * 1.05f) { passed++; r.lines.add("✔ depth: the far part is softer than the stage"); }
            else r.lines.add("✖ no depth of field (the far part is as sharp as the stage)");
        } else if (!place) r.lines.add("• depth of field: given by the film around the character");
        // ---- 5. like its reference
        if (refHue != null) {
            applicable++;
            float sim;
            synchronized (StillQa.class) { int[] sp = small(px, w, h, 256); sim = hueSimilarity(PicSense.analyse(sp, sw, sh).hue, refHue); }
            if (sim >= 0.75f) { passed++; r.lines.add(String.format(java.util.Locale.US, "✔ like its reference (%.2f)", sim)); }
            else r.lines.add(String.format(java.util.Locale.US, "✖ unlike its reference (%.2f, needs 0.75)", sim));
        }
        // ---- 6. hands
        if (hands == HANDS_LOOK) r.lines.add("• hands: look at them before you approve");
        else if (!place) { applicable++; passed++; r.lines.add(hands == HANDS_GEOMETRY ? "✔ hands built whole (geometry)" : "✔ hands as in your picture"); }
        // ---- 7. resolution
        applicable++;
        if (Math.max(w, h) >= 2048) { passed++; r.lines.add(Math.max(w, h) + " px ✔"); }
        else r.lines.add("✖ " + Math.max(w, h) + " px (needs 2048; the phone's memory set this size)");
        r.score = Math.round(100f * passed / Math.max(1, applicable));
        return r;
    }

    private static int sw, sh;

    /** A copy at most 'side' pixels on its long side (nearest pixel), the size the library reads pictures at. */
    static synchronized int[] small(int[] px, int w, int h, int side) {
        float k = Math.min(1f, side / (float) Math.max(w, h));
        sw = Math.max(1, Math.round(w * k)); sh = Math.max(1, Math.round(h * k));
        int[] out = new int[sw * sh];
        for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) out[y * sw + x] = px[Math.min(h - 1, (int) (y / k)) * w + Math.min(w - 1, (int) (x / k))];
        return out;
    }

    /** The mean gradient of a band of rows (fractions of the height): how sharp that part of the picture is. */
    static float sharpness(int[] px, int w, int h, float y0f, float y1f) {
        int y0 = Math.max(1, (int) (h * y0f)), y1 = Math.min(h - 1, (int) (h * y1f));
        double s = 0; long n = 0;
        for (int y = y0; y < y1; y += 2) for (int x = 1; x < w - 1; x += 2) {
            float gx = lum(px[y * w + x + 1]) - lum(px[y * w + x - 1]), gy = lum(px[(y + 1) * w + x]) - lum(px[(y - 1) * w + x]);
            s += Math.abs(gx) + Math.abs(gy); n++;
        }
        return n == 0 ? 0 : (float) (s / n);
    }

    /** A 12-bin hue histogram of the opaque, coloured pixels (the same bins as PicSense's first twelve). */
    public static float[] hues(int[] px, int w, int h) {
        float[] hb = new float[12];
        float[] hsv = new float[3];
        long n = 0;
        for (int i = 0; i < px.length; i += 3) {
            int c = px[i];
            if ((c >>> 24) < 200) continue;
            int rr = (c >> 16) & 255, gg = (c >> 8) & 255, bb = c & 255;
            int mx = Math.max(rr, Math.max(gg, bb)), mn = Math.min(rr, Math.min(gg, bb));
            if (mx < 40 || mx - mn < 30) continue;
            float hue;
            if (mx == rr) hue = 60f * (((gg - bb) / (float) (mx - mn) + 6) % 6);
            else if (mx == gg) hue = 60f * ((bb - rr) / (float) (mx - mn) + 2);
            else hue = 60f * ((rr - gg) / (float) (mx - mn) + 4);
            hb[Math.min(11, (int) (hue / 30f))]++;
            n++;
        }
        if (n > 0) for (int i = 0; i < 12; i++) hb[i] /= n;
        return hb;
    }

    /** Cosine similarity of two colour histograms (PicSense's twelve hues and white, grey, black). */
    public static float hueSimilarity(float[] a, float[] b) {
        double ab = 0, aa = 0, bb = 0;
        for (int i = 0; i < a.length && i < b.length; i++) { ab += a[i] * b[i]; aa += a[i] * a[i]; bb += b[i] * b[i]; }
        return aa == 0 || bb == 0 ? 0 : (float) (ab / Math.sqrt(aa * bb));
    }
}
