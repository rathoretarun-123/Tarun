package com.tarun.kahani.core;

/**
 * The look of the pictures the user uploaded or chose, read from the pictures themselves, so that everything the
 * studio makes for the same story (a doll for a character without a picture, a place in 3D, a figure's views)
 * sits on the same line: lit from the same side, as warm, as saturated and as contrasty as the references
 * (the scene maker guide, ch. 2 and 5: a project style, stored apart from any character's identity).
 */
public final class StyleCue {
    /** Which side the key light comes from in the references (-1 left, +1 right), and how sure (0..1). */
    public float lightSide = -1, lightSure;
    /** Warmth (red minus blue, -1..1), saturation (0..1), contrast (0..1) of the references, and their count. */
    public float warmth, saturation = 0.45f, contrast = 0.22f;
    public int pictures;
    /** The skin tones seen in the cast's pictures (ARGB), for a doll of a character without a picture. */
    public int[] skins = new int[0];
    /** The reference's measured values before averaging (for the production file). */
    private float sumWarm, sumSat, sumCon, sumSide;

    /** The studio's defaults when the user gave no picture at all. */
    public static StyleCue none() { return new StyleCue(); }

    /** Adds a reference picture (a character cut-out with transparency, or a place). */
    public void add(int[] px, int w, int h, boolean character) {
        long n = 0;
        double warm = 0, sat = 0, lum = 0, lum2 = 0, left = 0, right = 0;
        long nl = 0, nr = 0;
        long sr = 0, sg = 0, sb = 0, ns = 0;
        int step = Math.max(1, (int) Math.sqrt(w * (long) h / 60000));
        for (int y = 0; y < h; y += step) for (int x = 0; x < w; x += step) {
            int c = px[y * w + x];
            if ((c >>> 24) < 200) continue;
            int r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
            int mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b));
            float l = (0.299f * r + 0.587f * g + 0.114f * b) / 255f;
            n++;
            warm += (r - b) / 255f;
            sat += mx == 0 ? 0 : (mx - mn) / (float) mx;
            lum += l; lum2 += l * l;
            if (character ? Cutout.isSkin(c) : true) {
                if (x < w / 2) { left += l; nl++; } else { right += l; nr++; }
            }
            if (character && Cutout.isSkin(c)) { sr += r; sg += g; sb += b; ns++; }
        }
        if (n < 100) return;
        pictures++;
        sumWarm += warm / n; sumSat += sat / n;
        double mean = lum / n, var = lum2 / n - mean * mean;
        sumCon += (float) Math.sqrt(Math.max(0, var));
        if (nl > 50 && nr > 50) {
            double d = right / nr - left / nl;
            sumSide += d;
        }
        warmth = sumWarm / pictures; saturation = sumSat / pictures; contrast = sumCon / pictures;
        float side = sumSide / pictures;
        lightSide = side >= 0 ? 1 : -1;
        lightSure = Math.min(1, Math.abs(side) * 20);
        if (ns > 200) {
            int[] more = new int[skins.length + 1];
            System.arraycopy(skins, 0, more, 0, skins.length);
            more[skins.length] = 0xFF000000 | (int) (sr / ns) << 16 | (int) (sg / ns) << 8 | (int) (sb / ns);
            skins = more;
        }
    }

    /** A skin tone from the cast's pictures (the n-th), or the given default. */
    public int skin(int n, int dflt) { return skins.length == 0 ? dflt : skins[Math.abs(n) % skins.length]; }

    /**
     * Brings a picture the studio made onto the line of the references: its saturation and contrast toward
     * theirs (halfway — the references are the master, but a doll is still a doll), a little of their warmth.
     * Transparent pixels stay transparent.
     */
    public void grade(int[] px, int w, int h) {
        if (pictures == 0) return;
        StyleCue own = new StyleCue();
        own.add(px, w, h, false);
        if (own.pictures == 0) return;
        float satK = 1 + 0.5f * (saturation - own.saturation) / Math.max(0.08f, own.saturation);
        satK = Math.max(0.6f, Math.min(1.6f, satK));
        float conK = 1 + 0.5f * (contrast - own.contrast) / Math.max(0.05f, own.contrast);
        conK = Math.max(0.75f, Math.min(1.35f, conK));
        float warmK = (warmth - own.warmth) * 0.4f * 255;
        for (int i = 0; i < px.length; i++) {
            int c = px[i], a = c >>> 24;
            if (a == 0) continue;
            float r = c >> 16 & 255, g = c >> 8 & 255, b = c & 255;
            float l = 0.299f * r + 0.587f * g + 0.114f * b;
            r = l + (r - l) * satK; g = l + (g - l) * satK; b = l + (b - l) * satK;
            r = 128 + (r - 128) * conK; g = 128 + (g - 128) * conK; b = 128 + (b - 128) * conK;
            r += warmK * 0.5f; b -= warmK * 0.5f;
            px[i] = (a << 24) | clamp(r) << 16 | clamp(g) << 8 | clamp(b);
        }
    }

    private static int clamp(float v) { return v < 0 ? 0 : v > 255 ? 255 : Math.round(v); }

    /** One line for the production file. */
    public String describe() {
        if (pictures == 0) return "Style cue: no reference pictures; the studio's own look";
        return String.format(java.util.Locale.US, "Style cue from %d reference picture(s): key light from the %s (%.0f%% sure), warmth %+.2f, saturation %.2f, contrast %.2f, %d skin tone(s)",
                pictures, lightSide < 0 ? "left" : "right", lightSure * 100, warmth, saturation, contrast, skins.length);
    }
}
