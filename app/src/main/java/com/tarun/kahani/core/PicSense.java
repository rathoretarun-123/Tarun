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
        public float water, beige, flowers, lights;
        public boolean isPhoto() { return photo > 0.5f; }
        public String toMeta() {
            StringBuilder b = new StringBuilder();
            b.append("photo=").append(r2(photo)).append(";figure=").append(figure ? 1 : 0).append(";skin=").append(r2(skin));
            b.append(";hue=");
            for (int i = 0; i < hue.length; i++) { if (i > 0) b.append(','); b.append(r2(hue[i])); }
            b.append(";place=").append(r2(green)).append(',').append(r2(dark)).append(',').append(r2(sky)).append(',')
                    .append(r2(white)).append(',').append(r2(warm)).append(',').append(r2(bright)).append(',')
                    .append(r2(water)).append(',').append(r2(beige)).append(',').append(r2(flowers)).append(',').append(r2(lights));
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
                        if (f.length >= 10) { in.water = Float.parseFloat(f[6]); in.beige = Float.parseFloat(f[7]); in.flowers = Float.parseFloat(f[8]); in.lights = Float.parseFloat(f[9]); }
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
                if (hsv[1] > 0.2f && hsv[0] > 55 && hsv[0] < 170 && hsv[2] > 0.18f) in.green++;
                if (y < h / 3 && ((hsv[0] > 185 && hsv[0] < 250 && hsv[1] > 0.15f && hsv[2] > 0.45f)
                        || (hsv[0] > 15 && hsv[0] < 60 && hsv[1] < 0.6f && hsv[2] > 0.78f))) in.sky++;   // blue sky or a golden sunrise sky
                if (hsv[1] < 0.15f && hsv[2] > 0.78f) in.white++;
                if ((hsv[0] < 50 || hsv[0] > 340) && hsv[1] > 0.4f && hsv[2] > 0.45f) in.warm++;
                in.bright += hsv[2];
                if (y > h / 4 && ((hsv[0] > 170 && hsv[0] < 225 && hsv[1] > 0.12f && hsv[2] > 0.45f) || (hsv[1] < 0.12f && hsv[2] > 0.88f && y > h / 3))) in.water++;
                if (hsv[0] >= 22 && hsv[0] <= 52 && hsv[1] >= 0.08f && hsv[1] <= 0.4f && hsv[2] > 0.62f) in.beige++;
                if ((hsv[0] >= 290 || hsv[0] <= 8) && hsv[1] > 0.45f && hsv[2] > 0.5f) in.flowers++;
                if (hsv[2] > 0.85f && hsv[1] > 0.35f && hsv[0] >= 25 && hsv[0] <= 60) in.lights++;
            }
        }
        if (cnt > 0) {
            in.dark /= cnt; in.green /= cnt; in.sky /= cnt / 3f; in.white /= cnt; in.warm /= cnt; in.bright /= cnt;
            in.water /= cnt; in.beige /= cnt; in.flowers /= cnt; in.lights /= cnt;
        }
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
        // character art sits on a light, plain background; a dark uniform border is a night or cave scene
        float bright = (mr * 0.3f + mg * 0.59f + mb * 0.11f) / 255f;
        return close > samples.size() * 0.85f && bright > 0.55f;
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

    // ------------------------------------------------------------------ what the figure looks like

    /** Visible features of a figure, each -1 (no), 0 (can't tell) or 1 (yes); headRatio = head height / body height. */
    public static final class Traits {
        public int face, mustache, beard, bindi, turban, crown, hat, longHair, skirt, trousers, spear, greyHair, animal;
        public float headRatio;
        public String toMeta() {
            return "tr=" + face + "," + mustache + "," + beard + "," + bindi + "," + turban + "," + crown + "," + hat + "," + longHair + ","
                    + skirt + "," + trousers + "," + spear + "," + greyHair + "," + animal + "," + r2(headRatio);
        }
        public static Traits fromMeta(String meta) {
            if (meta == null) return null;
            int i = meta.indexOf("tr=");
            if (i < 0) return null;
            String v = meta.substring(i + 3);
            int e = v.indexOf(';');
            if (e >= 0) v = v.substring(0, e);
            String[] f = v.split(",");
            if (f.length < 14) return null;
            Traits t = new Traits();
            try {
                t.face = Integer.parseInt(f[0]); t.mustache = Integer.parseInt(f[1]); t.beard = Integer.parseInt(f[2]);
                t.bindi = Integer.parseInt(f[3]); t.turban = Integer.parseInt(f[4]); t.crown = Integer.parseInt(f[5]);
                t.hat = Integer.parseInt(f[6]); t.longHair = Integer.parseInt(f[7]); t.skirt = Integer.parseInt(f[8]);
                t.trousers = Integer.parseInt(f[9]); t.spear = Integer.parseInt(f[10]); t.greyHair = Integer.parseInt(f[11]);
                t.animal = Integer.parseInt(f[12]); t.headRatio = Float.parseFloat(f[13]);
            } catch (NumberFormatException ex) { return null; }
            return t;
        }
    }

    static float lumF(int c) { return lum(c) / 255f; }

    /** Average colour of the opaque pixels in a box (fractions of the crop). */
    static int avg(Cutout.Result r, float x0, float y0, float x1, float y1) {
        int ax = Math.max(0, (int) (x0 * r.w)), bx = Math.min(r.w - 1, (int) (x1 * r.w));
        int ay = Math.max(0, (int) (y0 * r.h)), by = Math.min(r.h - 1, (int) (y1 * r.h));
        long sr = 0, sg = 0, sb = 0, n = 0;
        for (int y = ay; y <= by; y++) for (int x = ax; x <= bx; x++) {
            int c = r.px[y * r.w + x];
            if ((c >>> 24) < 160) continue;
            sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; n++;
        }
        if (n == 0) return 0;
        return 0xFF000000 | ((int) (sr / n) << 16) | ((int) (sg / n) << 8) | (int) (sb / n);
    }

    /** Width of the figure (opaque span) on a row, as a fraction of the crop width. */
    static float rowWidth(Cutout.Result r, float fy) {
        int y = Math.max(0, Math.min(r.h - 1, (int) (fy * r.h)));
        int a = -1, b = -1;
        for (int x = 0; x < r.w; x++) if ((r.px[y * r.w + x] >>> 24) > 128) { if (a < 0) a = x; b = x; }
        return a < 0 ? 0 : (b - a + 1) / (float) r.w;
    }

    /** Number of separate opaque runs on a row (two legs = 2). */
    static int runs(Cutout.Result r, float fy, float minFrac) {
        int y = Math.max(0, Math.min(r.h - 1, (int) (fy * r.h)));
        int n = 0, len = 0;
        for (int x = 0; x <= r.w; x++) {
            boolean on = x < r.w && (r.px[y * r.w + x] >>> 24) > 128;
            if (on) len++;
            else { if (len > r.w * minFrac) n++; len = 0; }
        }
        return n;
    }

    /** Reads dress style, moustache, beard, bindi, headwear, hair, weapons and body proportions from a figure. */
    public static Traits traits(Cutout.Result r) {
        Traits t = new Traits();
        if (r == null || r.w < 20 || r.h < 40) return t;
        t.face = r.faceFound ? 1 : -1;
        float skinShare = skinShare(r.px);
        if (!r.faceFound) {
            t.animal = skinShare < 0.04f ? 1 : 0;
            return t;
        }
        // fur: an animal's "skin colour" covers most of the body, a person's only face, hands and feet
        int op = 0, fur = 0;
        for (int c : r.px) {
            if ((c >>> 24) < 128) continue;
            op++;
            float[] hv = hsv(c);
            if (hv[0] >= 5 && hv[0] <= 40 && hv[1] > 0.3f && hv[2] > 0.15f && hv[2] < 0.8f) fur++;
        }
        float furShare = op == 0 ? 0 : fur / (float) op;
        if (DEBUG) System.out.println("  fur=" + furShare);
        t.animal = furShare > 0.5f ? 1 : -1;
        float faceTop = r.faceTop, chin = r.chinY, fh = chin - faceTop;
        float cx = r.mouthX, fw = Math.max(0.05f, (r.eyeRX - r.eyeLX) * 2.6f);
        t.headRatio = (chin - r.headTop) / 1f;
        float skinL = lumF(r.skin);
        // moustache: the band between nose and mouth is much darker than the skin
        int mu = avg(r, cx - fw * 0.22f, faceTop + fh * 0.64f, cx + fw * 0.22f, faceTop + fh * 0.74f);
        if (mu != 0) {
            float l = lumF(mu);
            float[] h = hsv(mu);
            boolean lipRed = (h[0] < 20 || h[0] > 330) && h[1] > 0.45f;
            t.mustache = l < skinL * 0.62f && !lipRed ? 1 : l > skinL * 0.82f ? -1 : 0;
        }
        // beard: the chin below the mouth is dark
        int be = avg(r, cx - fw * 0.25f, faceTop + fh * 0.88f, cx + fw * 0.25f, Math.min(1, chin + fh * 0.05f));
        if (be != 0) t.beard = lumF(be) < skinL * 0.45f ? 1 : lumF(be) > skinL * 0.8f ? -1 : 0;
        // bindi / tilak: a small spot between the eyebrows that is clearly not skin-coloured
        float ex = (r.eyeLX + r.eyeRX) / 2, ey = r.eyeY - (r.eyeRX - r.eyeLX) * 0.55f;
        float sp = Math.max(0.008f, (r.eyeRX - r.eyeLX) * 0.12f);
        int bi = avg(r, ex - sp, ey - sp, ex + sp, ey + sp);
        int around = avg(r, ex - sp * 4, ey - sp * 3, ex - sp * 2, ey + sp * 3);
        if (bi != 0 && around != 0) {
            float[] hb = hsv(bi), ha = hsv(around);
            float dh = Math.abs(hb[0] - ha[0]);
            if (dh > 180) dh = 360 - dh;
            boolean distinct = (hb[1] > 0.55f && dh > 12) || lumF(bi) < lumF(around) * 0.55f || (hb[0] > 40 && hb[0] < 70 && hb[1] > 0.5f && dh > 15);
            t.bindi = distinct ? 1 : -1;
        }
        // headwear: what covers the top of the head
        if (faceTop - r.headTop > 0.02f) {
            int hw = avg(r, cx - fw * 0.3f, r.headTop + (faceTop - r.headTop) * 0.25f, cx + fw * 0.3f, r.headTop + (faceTop - r.headTop) * 0.75f);
            float[] h = hsv(hw);
            boolean hairLike = h[2] < 0.35f || (h[1] < 0.5f && h[0] >= 10 && h[0] <= 45 && h[2] < 0.55f);
            boolean gold = h[0] >= 38 && h[0] <= 60 && h[1] > 0.45f && h[2] > 0.55f;
            float tall = (faceTop - r.headTop) / Math.max(0.01f, fh);
            t.crown = gold ? 1 : -1;
            t.turban = !hairLike && !gold && h[1] > 0.3f && tall > 0.45f ? 1 : hairLike ? -1 : 0;
            t.hat = !hairLike && tall > 1.1f && h[2] < 0.4f ? 1 : -1;
            t.greyHair = hairLike ? -1 : (h[1] < 0.15f && h[2] > 0.6f ? 1 : -1);
        }
        // long hair / braid: dark hair continues beside or below the face
        int side = avg(r, cx - fw * 0.75f, chin, cx - fw * 0.5f, chin + fh * 0.6f);
        int side2 = avg(r, cx + fw * 0.5f, chin, cx + fw * 0.75f, chin + fh * 0.6f);
        float dl = Math.min(side == 0 ? 1 : lumF(side), side2 == 0 ? 1 : lumF(side2));
        t.longHair = dl < 0.3f ? 1 : 0;
        // dress: a skirt flares out at the bottom, trousers show two legs
        float waist = rowWidth(r, 0.55f), hem = rowWidth(r, 0.86f);
        int legsLow = runs(r, 0.93f, 0.04f), legsMid = runs(r, 0.82f, 0.04f);
        if (hem > waist * 1.22f) { t.skirt = 1; t.trousers = -1; }                       // lehenga / saree / frock flares out
        else if (legsLow >= 2 && legsMid >= 2 && hem <= waist * 1.15f) { t.trousers = 1; t.skirt = -1; }
        // spear / staff: something long and thin rises well above the head
        // the very top of the figure is narrow for a while (a spear tip / staff above the head)
        int narrow = 0;
        for (float fy = 0.01f; fy < 0.12f; fy += 0.01f) if (rowWidth(r, fy) < 0.1f && rowWidth(r, fy) > 0) narrow++;
        t.spear = narrow >= 5 ? 1 : -1;
        return t;
    }

    /** Colours plus visible traits (dress style, moustache, bindi, headwear, spear, fur…) against a character. */
    public static float matchCharacter(Info in, Traits t, Story.CharacterDef c) {
        float col = matchCharacter(in, c);
        if (t == null || !in.figure) return col;
        float[] tm = traitMatch(t, c);
        float wt = Math.min(0.75f, tm[1] / 12f);
        return col * (1 - wt) + tm[0] * wt;
    }

    /** Agreement between what a picture shows and what the description says (0..1), plus how much was compared. */
    public static float[] traitMatch(Traits t, Story.CharacterDef c) {
        if (t == null || c == null || c.look == null) return new float[]{0.5f, 0};
        Look l = c.look;
        String d = c.description;
        float s = 0, w = 0;
        boolean animalDesc = l.kind == Look.MONKEY || l.kind == Look.ANIMAL || l.kind == Look.BIRD || l.kind == Look.MONSTER;
        // {picture trait, description says yes, weight}
        int descMust = l.mustache > 0 || Txt.has(d, "मूँछ", "मूंछ", "moustache", "mustache") ? 1 : -1;
        int descBeard = l.beard || Txt.has(d, "दाढ़ी", "दाढी", "beard") ? 1 : -1;
        int descBindi = l.bindi != 0 || l.tilak != 0 || Txt.has(d, "बिंदी", "तिलक", "bindi", "tilak") ? 1 : -1;
        int descTurban = l.headwear == Look.HW_TURBAN || Txt.has(d, "पगड़ी", "साफ़ा", "साफा", "turban", "pagdi") ? 1 : -1;
        int descCrown = l.headwear == Look.HW_CROWN || Txt.has(d, "मुकुट", "ताज", "crown") ? 1 : -1;
        int descSkirt = l.outfit == Look.O_LEHENGA || l.outfit == Look.O_SAREE || l.outfit == Look.O_FROCK
                || Txt.has(d, "लहंगा", "लहँगा", "घाघरा", "साड़ी", "फ्रॉक", "lehenga", "saree", "sari", "frock", "skirt", "gown") ? 1 : 0;
        int descTrousers = l.outfit == Look.O_UNIFORM || l.outfit == Look.O_ACHKAN || l.outfit == Look.O_ARMOR || l.outfit == Look.O_JACKET
                || Txt.has(d, "पायजामा", "पजामा", "धोती", "सलवार", "वर्दी", "trousers", "pants", "pyjama", "uniform") ? 1 : 0;
        int descHat = l.headwear == Look.HW_WITCH_HAT || Txt.has(d, "टोपी", "हैट", "hat", "cap") ? 1 : -1;
        int descSpear = l.spear || Txt.has(d, "भाला", "भाले", "spear", "लाठी", "staff") ? 1 : -1;
        int descLongHair = l.hair == Look.H_BRAID || l.hair == Look.H_LONG || l.hair == Look.H_PIGTAILS
                || Txt.has(d, "चोटी", "चोटियों", "लंबे बाल", "braid", "long hair", "pigtail") ? 1 : 0;
        int descGrey = l.kind == Look.OLD_MAN || c.age >= 60 || Txt.has(d, "सफ़ेद बाल", "सफेद बाल", "grey hair", "white hair", "बुज़ुर्ग", "बूढ़") ? 1 : 0;
        float[][] pairs = {
                {t.mustache, descMust, 2.2f}, {t.beard, descBeard, 1.4f}, {t.bindi, descBindi, 1.2f},
                {t.turban, descTurban, 2.0f}, {t.crown, descCrown, 1.6f}, {t.hat, descHat, 1.6f},
                {t.skirt, descSkirt == 1 ? 1 : descTrousers == 1 ? -1 : 0, 1.8f}, {t.trousers, descTrousers == 1 ? 1 : descSkirt == 1 ? -1 : 0, 1.4f},
                {t.spear, descSpear, 1.2f}, {t.longHair, descLongHair, 0.8f}, {t.greyHair, descGrey, 1.0f},
                {t.animal, animalDesc ? 1 : -1, 2.5f}};
        for (float[] p : pairs) {
            if (p[0] == 0 || p[1] == 0) continue;
            w += p[2];
            if ((p[0] > 0) == (p[1] > 0)) s += p[2];
        }
        // (head size is not used for age: in cartoon / 3D animation styles even adults have big heads)
        return new float[]{w == 0 ? 0.5f : s / w, w};
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

    /** {feature index, weight, description words…}: what a described detail looks like in a picture. */
    static final Object[][] PLACE_CUES = {
            {6, 1.6f, "झरन", "नदी", "तालाब", "झील", "पानी", "जल", "समुद्र", "सरोवर", "waterfall", "river", "lake", "pond", "sea", "water", "fountain", "फव्वार"},
            {7, 1.3f, "महल", "द्वार", "गेट", "मंदिर", "दरबार", "प्रांगण", "किला", "हवेली", "संगमरमर", "palace", "gate", "temple", "fort", "castle", "marble", "courtyard", "building"},
            {0, 1.3f, "बगीच", "बाग", "जंगल", "पेड़", "घास", "हरे-भरे", "हरा", "वन", "garden", "forest", "jungle", "tree", "grass", "green", "park"},
            {8, 1.0f, "फूल", "फूलों", "गुलाब", "कमल", "flower", "roses", "lotus", "blossom"},
            {2, 1.0f, "आसमान", "आकाश", "धूप", "सुबह", "सूर्योदय", "दिन", "sky", "sunny", "morning", "sunrise", "daylight"},
            {1, 1.6f, "रात", "अँधेर", "अंधेर", "अंधकार", "गुफा", "night", "dark", "cave", "shadow"},
            {9, 1.2f, "रोशनी", "दीये", "दीप", "उत्सव", "जश्न", "लालटेन", "मशाल", "lights", "lamps", "festival", "diya", "torch", "lantern", "celebration"},
            {4, 0.8f, "सूर्यास्त", "शाम", "सुनहर", "sunset", "evening", "golden"},
            {3, 0.8f, "सफ़ेद", "सफेद", "बर्फ", "white", "snow"}};

    static float feature(Info in, int k) {
        switch (k) {
            case 0: return in.green; case 1: return in.dark; case 2: return in.sky; case 3: return in.white; case 4: return in.warm;
            case 5: return in.bright; case 6: return in.water; case 7: return in.beige + in.white * 0.5f; case 8: return in.flowers;
            default: return in.lights;
        }
    }

    /** Typical share of a feature in a picture where that detail is clearly visible. */
    static final float[] FULL = {0.35f, 0.45f, 0.35f, 0.3f, 0.25f, 0.6f, 0.08f, 0.25f, 0.04f, 0.03f};

    /**
     * How well a picture fits a place description. Every detail mentioned (water, palace, garden, flowers, sky,
     * night/cave, lights, sunset…) is looked for in the picture; details that are not there lower the score. 0..1
     */
    public static float matchPlace(Info in, String text) {
        if (in.figure) return 0.02f;
        float s = 0, w = 0;
        for (Object[] cue : PLACE_CUES) {
            boolean said = false;
            for (int i = 2; i < cue.length; i++) if (Txt.has(text, (String) cue[i])) { said = true; break; }
            if (!said) continue;
            int k = (Integer) cue[0];
            float wt = (Float) cue[1];
            w += wt;
            s += wt * Math.min(1, feature(in, k) / FULL[k]);
        }
        // a dark picture for a sunny place (or a bright one for night) is a strong "no"
        boolean night = Txt.has(text, "रात", "night", "अँधेर", "अंधेर", "गुफा", "cave");
        if (!night && in.dark > 0.45f) { s -= 0.6f; w += 0.6f; }
        if (night && in.bright > 0.6f) { s -= 0.6f; w += 0.6f; }
        float detail = w == 0 ? 0.3f : Math.max(0, s / w);
        return 0.65f * detail + 0.35f * matchPlaceKind(in, text);
    }

    static float matchPlaceKind(Info in, String text) {
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
        // improve the whole arrangement: swap or move pictures while the total fit gets better
        boolean better = true;
        for (int round = 0; better && round < 50; round++) {
            better = false;
            for (int i = 0; i < items; i++) {
                for (int j = i + 1; j < items; j++) {
                    int ti = out[i], tj = out[j];
                    if (ti < 0 && tj < 0) continue;
                    float now = (ti >= 0 ? score[i][ti] : 0) + (tj >= 0 ? score[j][tj] : 0);
                    float swap = (tj >= 0 ? score[i][tj] : 0) + (ti >= 0 ? score[j][ti] : 0);
                    boolean okI = tj < 0 || score[i][tj] > min, okJ = ti < 0 || score[j][ti] > min;
                    if (swap > now + 1e-4f && okI && okJ) { out[i] = tj; out[j] = ti; better = true; }
                }
                // or take a free target that fits better
                for (int t = 0; t < targets; t++) {
                    boolean taken = false;
                    for (int k = 0; k < items; k++) if (out[k] == t) { taken = true; break; }
                    if (taken) continue;
                    float cur = out[i] >= 0 ? score[i][out[i]] : min;
                    if (score[i][t] > cur + 1e-4f) { out[i] = t; better = true; }
                }
            }
        }
        return out;
    }
}
