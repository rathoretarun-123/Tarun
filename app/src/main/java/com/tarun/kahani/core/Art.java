package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Pictures supplied by the user: character art (animated as cut-out sprites with lip-sync),
 * scene backgrounds, full-screen cinematic shots, and the title / ending pictures.
 */
public final class Art {

    /** Platform image loading: returns ARGB pixels (for cut-out) and creates drawable handles. */
    public interface Loader {
        /** Decodes a picture, scaled so its longest side is at most maxSide. Returns {w, h, pixels...} or null. */
        int[] decode(String name, int maxSide);
        /** Creates a drawable image from ARGB pixels. */
        Object create(int[] argb, int w, int h);
        /** Decodes directly into a drawable (no pixel access needed). */
        Object load(String name, int maxSide);
        int width(Object img);
        int height(Object img);
    }

    public static final class Sprite {
        public Object img;
        public int w, h;
        public float mouthX, mouthY, mouthHW;
        public float eyeLX, eyeLY, eyeRX, eyeRY, eyeR;
        public float turbanY;       // bottom of turban (0 = none / unknown)
        public int skin = 0xFFD9A074, lip = 0xFF9C4A3E, lid = 0xFFC88A66;
        public boolean faceKnown;
        public transient Cutout.Result pixelsForSampling;
        /** Bones and face for moving the picture (null = moved as one piece). */
        public Rig rig;
        /** The same picture soaked by rain: darker, deeper colours (made only for rainy stories). */
        public Object wetImg;
        /** Rim light along the outline, lit from the left / from the right (null = none). */
        public Object rimL, rimR;
        /** The picture with its turban / cap taken off, and that headwear on its own (null = none found). */
        public Object bareImg, hatImg;
        /** Where the headwear sits, in eye-distances from the point between the eyes (left, top, right, bottom). */
        public float hatX0, hatY0, hatX1, hatY1;
        public int hatW, hatH;
    }

    public static final class Backdrop {
        public Object img;
        public int w, h;
        public float x0 = 0, y0 = 0, x1 = 1, y1 = 1;  // crop window (fractions)
        public float ground = 0.9f;                    // where feet stand (fraction of frame height)
        /** Sky, water, waterfall and plants found in the picture (null = not read). */
        public Nature.Scan scan;
    }

    public static final class Shot {
        public String scene;          // scene number ("8") or "" for any
        public String[] keys;
        public Backdrop pic;
    }

    public final Map<String, Sprite> sprites = new HashMap<String, Sprite>();   // by character id
    public final Map<String, Backdrop> scenes = new HashMap<String, Backdrop>(); // "1", "10a", "10b"
    public final List<Shot> shots = new ArrayList<Shot>();
    public Backdrop title, end;
    public boolean titleText = true, endText = true;

    public Backdrop sceneBackdrop(int number, int part) {
        Backdrop b = scenes.get(number + (part == 0 ? "a" : part == 1 ? "b" : "c"));
        if (b == null && part == 0) b = scenes.get(String.valueOf(number));
        if (b == null && part > 0 && !scenes.containsKey(number + "a")) b = null;
        return b;
    }

    public Shot shotFor(int scene, String text) {
        for (Shot s : shots) {
            if (s.scene.length() > 0 && !s.scene.equals(String.valueOf(scene))) continue;
            for (String k : s.keys) if (k.length() > 0 && Txt.has(text, k)) return s;
        }
        return null;
    }

    // ------------------------------------------------------------------ building sprites

    /** Cuts out a character picture and prepares it for animation. Landmarks may be overridden afterwards. */
    public static Sprite makeSprite(Loader L, String file, int maxSide) {
        return makeSprite(L, file, maxSide, false);
    }

    /** animal: the background showing between the legs is cut out too. */
    public static Sprite makeSprite(Loader L, String file, int maxSide, boolean animal) {
        int[] d = L.decode(file, maxSide);
        if (d == null) return null;
        int w = d[0], h = d[1];
        int[] px = new int[w * h];
        System.arraycopy(d, 2, px, 0, w * h);
        Cutout.Result r = Cutout.process(px, w, h, animal);
        Sprite s = new Sprite();
        s.w = r.w; s.h = r.h;
        s.img = L.create(r.px, r.w, r.h);
        s.mouthX = r.mouthX; s.mouthY = r.mouthY; s.mouthHW = r.mouthW;
        s.eyeLX = r.eyeLX; s.eyeRX = r.eyeRX; s.eyeLY = r.eyeY; s.eyeRY = r.eyeY; s.eyeR = r.eyeR;
        s.faceKnown = r.faceFound;
        s.skin = r.skin; s.lip = r.lip;
        s.lid = r.skin;
        s.pixelsForSampling = r;
        return s;
    }

    /**
     * Prepares the bare-headed version of a character who wears a turban or cap (for when it is snatched or
     * taken off) and the headwear itself (for whoever puts it on).
     */
    public static void takeOffHeadwear(Sprite s, Loader L) {
        Cutout.Result r = s.pixelsForSampling;
        if (r == null || !s.faceKnown) return;
        try {
            Headwear hw = Headwear.strip(r.px, r.w, r.h, s.eyeLX * r.w, s.eyeLY * r.h, s.eyeRX * r.w, s.eyeRY * r.h, s.skin);
            if (hw == null) return;
            s.bareImg = L.create(hw.bare, r.w, r.h);
            s.hatImg = L.create(hw.hat, hw.hx1 - hw.hx0 + 1, hw.hy1 - hw.hy0 + 1);
            s.hatW = hw.hx1 - hw.hx0 + 1; s.hatH = hw.hy1 - hw.hy0 + 1;
            s.hatX0 = hw.relX0; s.hatY0 = hw.relY0; s.hatX1 = hw.relX1; s.hatY1 = hw.relY1;
            if (s.rig != null) s.rig.faceBareImg = s.rig.faceFrom(hw.bare, r.w, r.h, L);
        } catch (RuntimeException e) {
            s.bareImg = null; s.hatImg = null;
        }
    }

    /** Re-samples skin and lip colours after landmarks are changed. */
    public static void resample(Sprite s) {
        Cutout.Result r = s.pixelsForSampling;
        if (r == null) return;
        s.skin = skinIn(r, s.eyeLX - s.eyeR, Math.min(s.eyeLY, s.eyeRY) - s.eyeR * 3f, s.eyeRX + s.eyeR, s.mouthY, s.skin);
        s.lid = s.skin;
        s.lip = Puppet.shade(Puppet.mix(s.skin, 0xFFB03A3A, 0.5f), 0.8f);
    }

    /** Average of skin-coloured pixels inside a box (ignores hair, mustache, eyes). */
    static int skinIn(Cutout.Result r, float x0, float y0, float x1, float y1, int fallback) {
        int ax = Math.max(0, (int) (x0 * r.w)), bx = Math.min(r.w - 1, (int) (x1 * r.w));
        int ay = Math.max(0, (int) (y0 * r.h)), by = Math.min(r.h - 1, (int) (y1 * r.h));
        long sr = 0, sg = 0, sb = 0, n = 0;
        int step = Math.max(1, (bx - ax) / 60);
        for (int y = ay; y <= by; y += step) {
            for (int x = ax; x <= bx; x += step) {
                int c = r.px[y * r.w + x];
                if (!Cutout.isSkin(c)) continue;
                sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; n++;
            }
        }
        if (n < 10) return fallback;
        return 0xFF000000 | ((int) (sr / n) << 16) | ((int) (sg / n) << 8) | (int) (sb / n);
    }

    static int sample(Cutout.Result r, float fx, float fy, int fallback) {
        int cx = (int) (fx * r.w), cy = (int) (fy * r.h);
        long sr = 0, sg = 0, sb = 0, n = 0;
        int rad = Math.max(2, r.w / 150);
        for (int y = cy - rad; y <= cy + rad; y++) {
            for (int x = cx - rad; x <= cx + rad; x++) {
                if (x < 0 || y < 0 || x >= r.w || y >= r.h) continue;
                int c = r.px[y * r.w + x];
                if ((c >>> 24) < 200) continue;
                sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; n++;
            }
        }
        if (n == 0) return fallback;
        return 0xFF000000 | ((int) (sr / n) << 16) | ((int) (sg / n) << 8) | (int) (sb / n);
    }

    public static Backdrop makeBackdrop(Loader L, String file, int maxSide) {
        Object img = L.load(file, maxSide);
        if (img == null) return null;
        Backdrop b = new Backdrop();
        b.img = img;
        b.w = L.width(img);
        b.h = L.height(img);
        fitCrop(b);
        // where the sky, water and plants are, so the water can flow and the plants sway
        try {
            int[] d = L.decode(file, 480);
            if (d != null) {
                int[] px = new int[d[0] * d[1]];
                System.arraycopy(d, 2, px, 0, px.length);
                b.scan = Nature.scan(px, d[0], d[1]);
                b.ground = findGround(px, d[0], d[1], b);
            }
        } catch (RuntimeException ignored) {
        }
        return b;
    }

    /**
     * Where the characters' feet go in a place picture: on its floor (the ground, a courtyard, a hall's floor),
     * found as the band at the bottom of the picture whose colours carry on from the very bottom rows, and
     * the feet a little more than half way down that band (the near part of the floor, never in the sky or up
     * a wall). As a fraction of the crop window's height.
     */
    public static float findGround(int[] px, int w, int h, Backdrop b) {
        int cx0 = (int) (b.x0 * w), cx1 = Math.max(cx0 + 4, (int) (b.x1 * w)), cy0 = (int) (b.y0 * h), cy1 = Math.max(cy0 + 8, (int) (b.y1 * h));
        cx1 = Math.min(w, cx1); cy1 = Math.min(h, cy1);
        int ch = cy1 - cy0;
        int x0 = cx0 + (cx1 - cx0) / 10, x1 = cx1 - (cx1 - cx0) / 10;
        // the floor's colours (bottom 10 % of the picture) against the colours of the rest (its top half)
        int band = Math.max(2, ch / 10);
        float[] fl = new float[4096], up = new float[4096];
        float nf = 0, nu = 0;
        for (int y = cy1 - band; y < cy1; y++) for (int x = x0; x < x1; x++) { fl[bin(px[y * w + x])]++; nf++; }
        for (int y = cy0; y < cy0 + ch / 2; y++) for (int x = x0; x < x1; x += 2) { up[bin(px[y * w + x])]++; nu++; }
        if (nf == 0 || nu == 0) return 0.9f;
        boolean[] floorish = new boolean[4096];
        for (int k = 0; k < 4096; k++) floorish[k] = fl[k] / nf > 0.0005f && fl[k] / nf > 1.5f * up[k] / nu;
        // going up: the first rows where most of the picture no longer looks like the floor
        int top = cy1 - band, miss = 0;
        for (int y = cy1 - band; y > cy0 + ch / 3; y--) {
            int like = 0, cnt = 0;
            for (int x = x0; x < x1; x += 2) { if (floorish[bin(px[y * w + x])]) like++; cnt++; }
            if (like < cnt * 0.5f) { if (++miss >= Math.max(2, ch / 60)) break; }
            else { miss = 0; top = y; }
        }
        float floorTop = (top - cy0) / (float) ch;
        float g = floorTop + 0.6f * (1 - floorTop);
        return Math.max(0.8f, Math.min(0.93f, g));
    }

    static int bin(int c) { return (((c >> 20) & 15) << 8) | (((c >> 12) & 15) << 4) | ((c >> 4) & 15); }

    // ------------------------------------------------------------------ manifest

    /**
     * Reads a cast manifest (see assets/sample/cast.txt) and loads all pictures.
     * Character names are matched against the parsed story.
     */
    public static Art fromManifest(String text, Story story, Loader L) {
        Art art = new Art();
        boolean rainy = mentions(story, "बारिश", "वर्षा", "बरसात", "बूँदाबाँदी", "तूफ़ान", "तूफान", "rain", "storm", "drizzl", "monsoon");
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.length() == 0 || line.startsWith("#")) continue;
            String[] f = line.split("\\|");
            try {
                if (f[0].equals("char") && f.length >= 3) {
                    Story.CharacterDef c = ScriptParser.resolve(story, f[1]);
                    if (c == null) continue;
                    boolean beast = c.look != null && (c.look.kind == Look.ANIMAL || c.look.kind == Look.BIRD);
                    Sprite s = makeSprite(L, f[2], 1100, beast);
                    if (s == null) continue;
                    if (f.length >= 11) {
                        float[] v = new float[8];
                        for (int i = 0; i < 8; i++) v[i] = Float.parseFloat(f[3 + i].trim());
                        if (v[0] > 0) {
                            s.mouthX = v[0]; s.mouthY = v[1]; s.mouthHW = v[2];
                            s.eyeLX = v[3]; s.eyeLY = v[4]; s.eyeRX = v[5]; s.eyeRY = v[6]; s.eyeR = v[7];
                            s.faceKnown = true;
                        }
                        if (f.length >= 12) s.turbanY = Float.parseFloat(f[11].trim());
                        resample(s);
                    }
                    // head, arms, legs and face for animating the picture (needs the final face points)
                    try { s.rig = Rig.build(s.pixelsForSampling, s, c.look, L); } catch (RuntimeException e) { s.rig = null; }
                    if (c.look != null && c.look.headwear == Look.HW_TURBAN && !beast) takeOffHeadwear(s, L);
                    if (s.rig != null && s.rig.face && !beast) {
                        // the face again from the full picture, for sharp close-ups
                        try {
                            int[] hi = L.decode(f[2], 2400);
                            if (hi != null) {
                                int[] px = new int[hi[0] * hi[1]];
                                System.arraycopy(hi, 2, px, 0, px.length);
                                Cutout.Result cr = s.pixelsForSampling;
                                // the cut-out is a crop of the picture: the same crop in the large one
                                if (cr != null && cr.cropW > 0) {
                                    float k = hi[0] / (float) cr.srcW;
                                    int x0 = Math.round(cr.cropX * k), y0 = Math.round(cr.cropY * k), w = Math.round(cr.w * k), h = Math.round(cr.h * k);
                                    w = Math.min(w, hi[0] - x0); h = Math.min(h, hi[1] - y0);
                                    int[] crop = new int[w * h];
                                    for (int y = 0; y < h; y++) System.arraycopy(px, (y + y0) * hi[0] + x0, crop, y * w, w);
                                    s.rig.sharpFace(crop, w, h, cr, L);
                                }
                            }
                        } catch (Throwable ignored) {
                            // out of memory or an unusual file: the face layer stays at the picture's size
                        }
                    }
                    if (s.rig != null && s.pixelsForSampling != null) {
                        // the rim light of the outline (soft, half size), for the key light from either side
                        try {
                            Cutout.Result cr = s.pixelsForSampling;
                            int[][] rl = RimLight.make(cr.px, cr.w, cr.h);
                            s.rimL = L.create(rl[0], rl[2][0], rl[2][1]);
                            s.rimR = L.create(rl[1], rl[2][0], rl[2][1]);
                        } catch (Throwable ignored) {
                            s.rimL = s.rimR = null;
                        }
                    }
                    if (rainy && s.pixelsForSampling != null) {
                        Cutout.Result cr = s.pixelsForSampling;
                        s.wetImg = L.create(wetPixels(cr.px), cr.w, cr.h);
                    }
                    s.pixelsForSampling = null;
                    art.sprites.put(c.id, s);
                } else if (f[0].equals("scene") && f.length >= 3) {
                    Backdrop b = makeBackdrop(L, f[2], 1600);
                    if (b == null) continue;
                    if (f.length >= 8) {
                        b.x0 = Float.parseFloat(f[3]); b.y0 = Float.parseFloat(f[4]);
                        b.x1 = Float.parseFloat(f[5]); b.y1 = Float.parseFloat(f[6]);
                        b.ground = Float.parseFloat(f[7]);
                    }
                    art.scenes.put(f[1], b);
                } else if (f[0].equals("shot") && f.length >= 4) {
                    Backdrop b = makeBackdrop(L, f[3], 1600);
                    if (b == null) continue;
                    Shot s = new Shot();
                    s.scene = f[1];
                    s.keys = f[2].split(",");
                    s.pic = b;
                    art.shots.add(s);
                } else if (f[0].equals("title") || f[0].equals("end")) {
                    Backdrop b = makeBackdrop(L, f[1], 1600);
                    if (b == null) continue;
                    boolean txt = f.length < 3 || !f[2].equals("0");
                    if (f[0].equals("title")) { art.title = b; art.titleText = txt; }
                    else { art.end = b; art.endText = txt; }
                }
            } catch (RuntimeException e) {
                // a bad line must never stop the film; skip it
            }
        }
        return art;
    }

    static boolean mentions(Story st, String... words) {
        if (st == null) return false;
        for (Story.Scene sc : st.scenes) {
            if (Txt.has(sc.setting + " " + sc.title, words)) return true;
            for (Story.Beat b : sc.beats) if (Txt.has(b.text + " " + b.manner, words)) return true;
        }
        return false;
    }

    /** A picture's colours when soaked: darker and a little deeper, with a cool tint (alpha kept). */
    public static int[] wetPixels(int[] px) {
        int[] o = new int[px.length];
        for (int i = 0; i < px.length; i++) {
            int c = px[i];
            int a = c >>> 24;
            float r = ((c >> 16) & 255), g = ((c >> 8) & 255), b = c & 255;
            float l = (r + g + b) / 3;
            r = (l + (r - l) * 1.15f) * 0.7f; g = (l + (g - l) * 1.15f) * 0.72f; b = (l + (b - l) * 1.15f) * 0.78f + 6;
            o[i] = (a << 24) | (clamp255(r) << 16) | (clamp255(g) << 8) | clamp255(b);
        }
        return o;
    }

    static int clamp255(float v) { return v < 0 ? 0 : v > 255 ? 255 : (int) v; }

    /** Sets a centred 16:9 crop window for a picture of any shape. */
    public static void fitCrop(Backdrop b) {
        float ar = b.w / (float) Math.max(1, b.h);
        float target = 16f / 9f;
        if (ar > target) {
            float wFrac = target / ar;
            b.x0 = (1 - wFrac) / 2; b.x1 = b.x0 + wFrac; b.y0 = 0; b.y1 = 1;
        } else {
            float hFrac = ar / target;
            b.y0 = (1 - hFrac) / 2; b.y1 = b.y0 + hFrac; b.x0 = 0; b.x1 = 1;
        }
        b.ground = 0.9f;
    }
}
