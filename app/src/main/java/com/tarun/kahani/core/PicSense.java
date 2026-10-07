package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The director's eyes for uploaded pictures, without any internet:
 *  - is it a real photo or artwork (cartoon / 3D render)?
 *  - is it one figure (character) or a whole place (background)?
 *  - which colours does the figure wear, what does the place look like (green, dark, sky, marble, warm light)?
 * and how well does it fit each character / place described in the script.
 * Works on small ARGB arrays (about 256 px), identically on Android and desktop.
 */
public final class PicSense {
    private PicSense() {}

    public static final class Info {
        public int w, h;
        public float photo;              // 0..1 how much it looks like a real photo
        public boolean figure;           // a single figure on a plain background (or a portrait photo)
        public float skin;               // share of skin-coloured pixels in the figure/centre
        public final float[] hue = new float[15];   // 12 hue bins + white, grey, black (clothing/figure colours)
        public float green, dark, sky, white, warm, bright;   // place features
        public boolean isPhoto() { return photo > 0.5f; }
        public String toMeta() {
            StringBuilder b = new StringBuilder();
            b.append("photo=").append(r2(photo)).append(";figure=").append(figure ? 1 : 0).append(";skin=").append(r2(skin));
            b.append(";hue=");
            for (int i = 0; i < hue.length; i++) { if (i > 0) b.append(','); b.append(r2(hue[i])); }
            b.append(";place=").append(r2(green)).append(',').append(r2(dark)).append(',').append(r2(sky)).append(',')
                    .append(r2(white)).append(',').append(r2(warm)).append(',').append(r2(bright));
            return b.toString();
        }
        public static Info fromMeta(String meta) {
            if (meta == null || !meta.contains("hue=")) return null;
            Info in = new Info();
            for (String kv : meta.split(";")) {
                int e = kv.indexOf('=');
                if (e < 0) continue;
                String k = kv.substring(0, e), v = kv.substring(e + 1);
                try {
                    if (k.equals("photo")) in.photo = Float.parseFloat(v);
                    else if (k.equals("figure")) in.figure = v.equals("1");
                    else if (k.equals("skin")) in.skin = Float.parseFloat(v);
                    else if (k.equals("hue")) { String[] f = v.split(","); for (int i = 0; i < Math.min(f.length, 15); i++) in.hue[i] = Float.parseFloat(f[i]); }
                    else if (k.equals("place")) {
                        String[] f = v.split(",");
                        in.green = Float.parseFloat(f[0]); in.dark = Float.parseFloat(f[1]); in.sky = Float.parseFloat(f[2]);
                        in.white = Float.parseFloat(f[3]); in.warm = Float.parseFloat(f[4]); in.bright = Float.parseFloat(f[5]);
                    }
                } catch (RuntimeException ignored) {}
            }
            return in;
        }
    }

    public static boolean DEBUG = false;

    static float r2(float v) { return Math.round(v * 100) / 100f; }

    static int lum(int c) { return (((c >> 16) & 255) * 77 + ((c >> 8) & 255) * 150 + (c & 255) * 29) >> 8; }

    // ------------------------------------------------------------------ analysis

    public static Info analyse(int[] px, int w, int h) {
        Info in = new Info();
        in.w = w; in.h = h;
        in.photo = photoScore(px, w, h);
        boolean alpha = Cutout.hasAlpha(px);
        int[] fig = px;
        if (alpha) in.figure = true;
        else if (plainBorder(px, w, h)) {
            fig = px.clone();
            Cutout.removeBackground(fig, w, h);
            int opaque = 0;
            for (int c : fig) if ((c >>> 24) > 128) opaque++;
            float f = opaque / (float) (w * h);
            in.figure = f > 0.05f && f < 0.8f;
            if (!in.figure) fig = px;
        } else if (in.photo > 0.5f && h >= w * 0.9f) {
            // portrait photo with a busy background: assume the person stands in the middle
            int[] seg = segmentCentre(px, w, h);
            int opaque = 0;
            for (int c : seg) if ((c >>> 24) > 128) opaque++;
            float sk = skinShare(seg);
            if (opaque > w * h * 0.08f && sk > 0.04f) { in.figure = true; fig = seg; }
        }
        // colours of the figure (or the centre of the picture)
        int figTop = h, figBottom = 0;
        if (in.figure) for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) if ((fig[y * w + x] >>> 24) >= 128) { figTop = Math.min(figTop, y); figBottom = Math.max(figBottom, y); }
        float total = 0;
        int skinN = 0, n = 0;
        for (int y = 0; y < h; y++) {
            for (int x = 0; x < w; x++) {
                int c = fig[y * w + x];
                if (in.figure ? (c >>> 24) < 128 : (x < w / 6 || x > w * 5 / 6)) continue;
                if (in.figure && y < figTop + (figBottom - figTop) * 0.18f) { if (isSkin(c)) { skinN++; n++; } continue; }   // head: hair, face
                n++;
                if (isSkin(c)) { skinN++; if (in.figure) continue; }
                float[] hsv = hsv(c);
                int bin;
                if (hsv[2] < 0.18f) bin = 14;
                else if (hsv[1] < 0.15f) bin = hsv[2] > 0.82f ? 12 : 13;
                else bin = ((int) (hsv[0] / 30f + 0.5f)) % 12;
                float wgt = bin >= 12 ? 0.6f : 0.4f + hsv[1];
                in.hue[bin] += wgt;
                total += wgt;
            }
        }
        if (total > 0) for (int i = 0; i < in.hue.length; i++) in.hue[i] /= total;
        in.skin = n == 0 ? 0 : skinN / (float) n;
        // whole-picture look (for places)
        int cnt = 0;
        for (int y = 0; y < h; y += 2) {
            for (int x = 0; x < w; x += 2) {
                int c = px[y * w + x];
                float[] hsv = hsv(c);
                cnt++;
                if (hsv[2] < 0.25f) in.dark++;
                if (hsv[1] > 0.25f && hsv[0] > 70 && hsv[0] < 170 && hsv[2] > 0.2f) in.green++;
                if (y < h / 3 && hsv[0] > 185 && hsv[0] < 250 && hsv[1] > 0.15f && hsv[2] > 0.45f) in.sky++;
                if (hsv[1] < 0.15f && hsv[2] > 0.78f) in.white++;
                if ((hsv[0] < 50 || hsv[0] > 340) && hsv[1] > 0.4f && hsv[2] > 0.45f) in.warm++;
                in.bright += hsv[2];
            }
        }
        if (cnt > 0) { in.dark /= cnt; in.green /= cnt; in.sky /= cnt / 3f; in.white /= cnt; in.warm /= cnt; in.bright /= cnt; }
        in.sky = Math.min(1, in.sky);
        return in;
    }

    /**
     * Real photos keep fine texture and sensor noise in smooth areas; cartoons and 3D renders are clean.
     * Also counts distinct colours: illustrations use fewer.
     */
    public static float photoScore(int[] px, int w, int h) {
        double res = 0;
        int n = 0;
        Set<Integer> colours = new HashSet<Integer>();
        for (int y = 1; y < h - 1; y++) {
            for (int x = 1; x < w - 1; x++) {
                int i = y * w + x;
                int c = px[i];
                if ((c >>> 24) < 200) continue;
                int l = lum(c);
                int gx = lum(px[i + 1]) - lum(px[i - 1]), gy = lum(px[i + w]) - lum(px[i - w]);
                if (Math.abs(gx) + Math.abs(gy) > 40) continue;          // skip edges: only smooth areas
                int lap = 4 * l - lum(px[i - 1]) - lum(px[i + 1]) - lum(px[i - w]) - lum(px[i + w]);
                res += Math.abs(lap);
                n++;
                if ((x + y) % 3 == 0) colours.add(((c >> 19) & 31) << 10 | ((c >> 11) & 31) << 5 | ((c >> 3) & 31));
            }
        }
        if (n < 50) return 0;
        float noise = (float) (res / n);                 // ~1-3 renders/cartoons, 4-10 photos
        float variety = colours.size() / (float) Math.max(1, n / 3);
        if (DEBUG) System.out.println("  noise=" + noise + " variety=" + variety);
        float s = (noise - 2.6f) / 3f + (variety - 0.25f);
        return Math.max(0, Math.min(1, 0.5f + s));
    }

    public static boolean plainBorder(int[] px, int w, int h) {
        long sr = 0, sg = 0, sb = 0;
        int n = 0;
        List<Integer> samples = new ArrayList<Integer>();
        for (int x = 0; x < w; x += Math.max(1, w / 40)) { samples.add(px[x]); samples.add(px[(h - 1) * w + x]); }
        for (int y = 0; y < h; y += Math.max(1, h / 40)) { samples.add(px[y * w]); samples.add(px[y * w + w - 1]); }
        for (int c : samples) { sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; n++; }
        int mr = (int) (sr / n), mg = (int) (sg / n), mb = (int) (sb / n);
        int close = 0;
        for (int c : samples) {
            int d = Math.abs(((c >> 16) & 255) - mr) + Math.abs(((c >> 8) & 255) - mg) + Math.abs((c & 255) - mb);
            if (d < 50) close++;
        }
        return close > samples.size() * 0.85f;
    }

    static boolean isSkin(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        if (!(r > g && g > b)) return false;
        float[] h = hsv(c);
        return h[0] >= 5 && h[0] <= 32 && h[1] >= 0.18f && h[1] <= 0.6f && h[2] > 0.35f;
    }

    static float skinShare(int[] px) {
        int n = 0, s = 0;
        for (int c : px) { if ((c >>> 24) < 128) continue; n++; if (isSkin(c)) s++; }
        return n == 0 ? 0 : s / (float) n;
    }

    public static float[] hsv(int c) {
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        float max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b)), d = max - min;
        float hh = 0;
        if (d > 1e-4f) {
            if (max == r) hh = 60 * (((g - b) / d) % 6);
            else if (max == g) hh = 60 * ((b - r) / d + 2);
            else hh = 60 * ((r - g) / d + 4);
        }
        if (hh < 0) hh += 360;
        return new float[]{hh, max == 0 ? 0 : d / max, max};
    }

    /**
     * Separates a person standing in the middle of a busy photo: colours common in the centre but rare near the
     * edges are "person"; the result keeps the biggest connected region and fades its edge.
     */
    public static int[] segmentCentre(int[] px, int w, int h) {
        int n = w * h;
        // body-shape guide: a person standing in the middle, narrow at the head, wider at the shoulders
        float[] prior = new float[n];
        for (int y = 0; y < h; y++) {
            float fy = y / (float) h;
            float half = fy < 0.08f ? 0.06f : fy < 0.38f ? 0.12f + (fy - 0.08f) * 0.35f : 0.24f + (fy - 0.38f) * 0.25f;
            for (int x = 0; x < w; x++) {
                float d = Math.abs(x - w / 2f) / w;
                prior[y * w + x] = d < half ? 0.85f : d < half + 0.08f ? 0.5f : 0.12f;
            }
        }
        byte[] fg = new byte[n];
        for (int i = 0; i < n; i++) fg[i] = (byte) (prior[i] > 0.6f ? 1 : 0);
        float[] fh = new float[4096], bh = new float[4096];
        int[] key = new int[n];
        for (int i = 0; i < n; i++) { int c = px[i]; key[i] = ((c >> 20) & 15) << 8 | ((c >> 12) & 15) << 4 | ((c >> 4) & 15); }
        for (int iter = 0; iter < 3; iter++) {
            java.util.Arrays.fill(fh, 1f);
            java.util.Arrays.fill(bh, 1f);
            float fs = 4096, bs = 4096;
            for (int i = 0; i < n; i++) {
                if (fg[i] == 1) { fh[key[i]] += 1; fs += 1; }
                else { bh[key[i]] += 1; bs += 1; }
            }
            for (int i = 0; i < n; i++) {
                float pf = fh[key[i]] / fs, pb = bh[key[i]] / bs;
                float post = pf / (pf + pb);
                float p = post * 0.65f + prior[i] * 0.35f;
                fg[i] = (byte) (p > 0.5f ? 1 : 0);
            }
            fg = majority(fg, w, h);
            fg = majority(fg, w, h);
            fg = largestCentral(fg, w, h);
            fillHoles(fg, w, h);
        }
        // soft edge
        int[] out = new int[n];
        for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
            int i = y * w + x;
            if (fg[i] == 0) { out[i] = px[i] & 0x00FFFFFF; continue; }
            boolean edge = (x > 0 && fg[i - 1] == 0) || (x < w - 1 && fg[i + 1] == 0) || (y > 0 && fg[i - w] == 0) || (y < h - 1 && fg[i + w] == 0);
            out[i] = edge ? (px[i] & 0x00FFFFFF) | 0x99000000 : px[i] | 0xFF000000;
        }
        return out;
    }

    static byte[] majority(byte[] fg, int w, int h) {
        byte[] o = new byte[w * h];
        for (int y = 1; y < h - 1; y++) for (int x = 1; x < w - 1; x++) {
            int s = 0;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) s += fg[(y + dy) * w + x + dx];
            o[y * w + x] = (byte) (s >= 5 ? 1 : 0);
        }
        return o;
    }

    /** Keeps the biggest region that touches the middle column. */
    static byte[] largestCentral(byte[] fg, int w, int h) {
        int n = w * h;
        int[] label = new int[n], queue = new int[n];
        int bestLabel = 0, bestSize = 0, cur = 0;
        for (int i = 0; i < n; i++) {
            if (fg[i] == 0 || label[i] != 0) continue;
            cur++;
            int qh = 0, qt = 0, size = 0;
            queue[qt++] = i;
            label[i] = cur;
            boolean centre = false;
            while (qh < qt) {
                int j = queue[qh++];
                size++;
                int x = j % w;
                if (Math.abs(x - w / 2) < w / 8) centre = true;
                if (x > 0 && fg[j - 1] == 1 && label[j - 1] == 0) { label[j - 1] = cur; queue[qt++] = j - 1; }
                if (x < w - 1 && fg[j + 1] == 1 && label[j + 1] == 0) { label[j + 1] = cur; queue[qt++] = j + 1; }
                if (j >= w && fg[j - w] == 1 && label[j - w] == 0) { label[j - w] = cur; queue[qt++] = j - w; }
                if (j + w < n && fg[j + w] == 1 && label[j + w] == 0) { label[j + w] = cur; queue[qt++] = j + w; }
            }
            if (centre && size > bestSize) { bestSize = size; bestLabel = cur; }
        }
        byte[] o = new byte[n];
        for (int i = 0; i < n; i++) o[i] = (byte) (label[i] == bestLabel && bestLabel != 0 ? 1 : 0);
        return o;
    }

    /** Background pockets that do not reach the picture's border are part of the person (face, hair). */
    static void fillHoles(byte[] fg, int w, int h) {
        int n = w * h;
        byte[] outside = new byte[n];
        int[] queue = new int[n];
        int qt = 0, qh = 0;
        for (int x = 0; x < w; x++) { if (fg[x] == 0) { outside[x] = 1; queue[qt++] = x; } int b = (h - 1) * w + x; if (fg[b] == 0 && outside[b] == 0) { outside[b] = 1; queue[qt++] = b; } }
        for (int y = 0; y < h; y++) { int a = y * w; if (fg[a] == 0 && outside[a] == 0) { outside[a] = 1; queue[qt++] = a; } int b = a + w - 1; if (fg[b] == 0 && outside[b] == 0) { outside[b] = 1; queue[qt++] = b; } }
        while (qh < qt) {
            int j = queue[qh++];
            int x = j % w;
            int[] nb = {x > 0 ? j - 1 : -1, x < w - 1 ? j + 1 : -1, j - w, j + w};
            for (int k : nb) if (k >= 0 && k < n && fg[k] == 0 && outside[k] == 0) { outside[k] = 1; queue[qt++] = k; }
        }
        for (int i = 0; i < n; i++) if (fg[i] == 0 && outside[i] == 0) fg[i] = 1;
    }

    // ------------------------------------------------------------------ matching to the script

    static int hueBin(int c) {
        float[] hsv = hsv(c);
        if (hsv[2] < 0.18f) return 14;
        if (hsv[1] < 0.15f) return hsv[2] > 0.82f ? 12 : 13;
        return ((int) (hsv[0] / 30f + 0.5f)) % 12;
    }

    /** How well a picture fits a character's description: worn colours, figure/animal cues. 0..1 */
    public static float matchCharacter(Info in, Story.CharacterDef c) {
        if (!in.figure) return in.skin > 0.08f && in.isPhoto() ? 0.15f : 0.02f;
        List<Integer> cols = dressColours(c.description);
        float s;
        if (!cols.isEmpty()) {
            // description colour distribution (dress colours weigh more), smoothed over neighbouring hues
            float[] want = new float[15];
            float tw = 0;
            for (int i = 0; i < Math.min(5, cols.size()); i++) {
                int b = hueBin(cols.get(i));
                float wgt = i == 0 ? 1.4f : i == 1 ? 1f : 0.6f;
                want[b] += wgt;
                if (b < 12) { want[(b + 1) % 12] += wgt * 0.35f; want[(b + 11) % 12] += wgt * 0.35f; }
                tw += wgt * (b < 12 ? 1.7f : 1f);
            }
            float[] have = new float[15];
            for (int b = 0; b < 15; b++) {
                have[b] = in.hue[b];
                if (b < 12) have[b] += 0.35f * (in.hue[(b + 1) % 12] + in.hue[(b + 11) % 12]);
            }
            float inter = 0, hs = 0;
            for (int b = 0; b < 15; b++) hs += have[b];
            for (int b = 0; b < 15; b++) inter += Math.min(want[b] / tw, have[b] / hs);
            s = inter;   // 0..1: shared colour mass
        } else s = 0.25f;
        Look l = c.look;
        if (l != null) {
            boolean animal = l.kind == Look.MONKEY || l.kind == Look.ANIMAL || l.kind == Look.BIRD || l.kind == Look.MONSTER;
            float brownish = in.hue[1] + in.hue[0] * 0.5f + in.hue[14] * 0.5f;
            if (animal && in.skin < 0.05f && brownish > 0.3f) s += 0.15f;
            if (!animal && in.skin > 0.04f) s += 0.05f;
            if (animal && in.skin > 0.12f && l.kind != Look.MONSTER) s -= 0.1f;
        }
        return Math.max(0, Math.min(1, s));
    }

    static final String[] DRESS = {"पहनावा", "पहने", "पहनी", "पहनता", "पहनती", "वर्दी", "कपड़े", "कपड़ा", "लहंगा", "लहंगे", "घाघरा", "घाघरे", "साड़ी",
            "कुर्ता", "कुर्ते", "चोली", "सलवार", "बंडी", "कवच", "धोती", "शेरवानी", "अचकन", "फ्रॉक", "चोगा", "लबादा", "पगड़ी", "dress", "wearing",
            "wears", "outfit", "frock", "uniform", "clothes", "robe", "cloak", "armour", "armor", "shirt", "saree", "sari", "lehenga", "kurta",
            "jacket", "coat", "suit", "skirt", "fur", "बाल वाला", "फर"};
    static final String[] NOT_DRESS = {"बिंदी", "आँख", "आंख", "बाल", "तिलक", "सिंदूर", "होंठ", "नाखून", "दाँत", "bindi", "eyes", "eye", "hair",
            "lips", "nails", "teeth", "tilak"};

    /** Colours of the clothes first (from the dress part of the description), then other body colours. */
    static List<Integer> dressColours(String description) {
        List<Integer> dress = new ArrayList<Integer>(), other = new ArrayList<Integer>();
        for (String part : description.split("[।.;,*\\n]|\\s-\\s")) {
            List<Integer> cs = LookDesigner.colorsIn(part);
            if (cs.isEmpty()) continue;
            boolean isDress = Txt.has(part, DRESS), notDress = Txt.has(part, NOT_DRESS);
            for (int c : cs) {
                if (isDress && !notDress) { if (!dress.contains(c)) dress.add(c); }
                else if (!notDress && !other.contains(c)) other.add(c);
            }
        }
        for (int c : other) if (!dress.contains(c)) dress.add(c);
        return dress;
    }

    /** How well a picture fits a place description (forest = green, cave/night = dark, palace = white…). 0..1 */
    public static float matchPlace(Info in, String text) {
        if (in.figure) return 0.02f;
        int set = Sets.detect(text);
        boolean night = Txt.has(text, "रात", "night", "अँधेर", "अंधेर", "dark", "शाम", "evening");
        float[] want = new float[6]; // green, dark, sky, white, warm, bright
        switch (set) {
            case Sets.FOREST: want[0] = 1; want[1] = 0.3f; break;
            case Sets.GARDEN: want[0] = 0.8f; want[2] = 0.5f; want[5] = 0.5f; break;
            case Sets.CAVE_IN: case Sets.CAVE_MOUTH: want[1] = 1; want[0] = 0.2f; break;
            case Sets.CELEBRATION: want[4] = 1; want[1] = 0.3f; break;
            case Sets.HALL: case Sets.GATE: case Sets.COURTYARD: want[3] = 0.8f; want[4] = 0.4f; want[5] = 0.5f; break;
            case Sets.VILLAGE: want[0] = 0.4f; want[4] = 0.5f; want[2] = 0.4f; break;
            default: want[2] = 0.5f; want[0] = 0.5f; want[5] = 0.5f;
        }
        if (night) { want[1] += 0.8f; want[5] = 0; want[2] = 0; }
        float[] have = {in.green, in.dark, in.sky, in.white, in.warm, in.bright};
        float dot = 0, nw = 0;
        for (int i = 0; i < 6; i++) { dot += want[i] * Math.min(1, have[i] * 2.2f); nw += want[i]; }
        return nw == 0 ? 0.3f : Math.max(0, Math.min(1, dot / nw));
    }

    /** Words shared between what we know about a picture (name, tags, AI caption) and a description. 0..1 */
    public static float textMatch(String about, String name, String description) {
        if (about == null || about.trim().length() == 0) return 0;
        String a = " " + Txt.norm(about).replaceAll("[^\\p{L}\\p{M}0-9]+", " ") + " ";
        float s = 0;
        for (String w : Txt.norm(name).split("\\s+")) {
            if (w.length() >= 2 && !ScriptParser.isTitleWord(w) && a.contains(" " + w + " ")) s += 0.6f;
        }
        int hits = 0, total = 0;
        for (String w : Txt.norm(description).replaceAll("[^\\p{L}\\p{M}0-9]+", " ").split("\\s+")) {
            if (w.length() < 4) continue;
            total++;
            if (a.contains(" " + w + " ")) hits++;
        }
        if (total > 0) s += Math.min(0.5f, hits / (float) Math.min(total, 8));
        return Math.min(1, s);
    }

    /** Best one-to-one assignment, greedily by score; returns target index per item (-1 = none). */
    public static int[] assign(float[][] score, float min) {
        int items = score.length, targets = items == 0 ? 0 : score[0].length;
        int[] out = new int[items];
        java.util.Arrays.fill(out, -1);
        boolean[] usedI = new boolean[items], usedT = new boolean[targets];
        while (true) {
            float best = min;
            int bi = -1, bt = -1;
            for (int i = 0; i < items; i++) {
                if (usedI[i]) continue;
                for (int t = 0; t < targets; t++) {
                    if (usedT[t]) continue;
                    if (score[i][t] > best) { best = score[i][t]; bi = i; bt = t; }
                }
            }
            if (bi < 0) break;
            out[bi] = bt;
            usedI[bi] = true;
            usedT[bt] = true;
        }
        return out;
    }
}
