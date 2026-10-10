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
    /** v39: what the loading changed (a close-up front picture swapped for the full-length one), for the shot list. */
    public final java.util.List<String> notes = new java.util.ArrayList<String>();

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
        /** v35: the picture at half and a quarter of its size (null when too small): drawn far smaller than its pixels, a picture shimmers. */
        public Object imgHalf, imgQuarter;
        public int w, h;
        public float mouthX, mouthY, mouthHW;
        public float eyeLX, eyeLY, eyeRX, eyeRY, eyeR;
        public float turbanY;       // bottom of turban (0 = none / unknown)
        public int skin = 0xFFD9A074, lip = 0xFF9C4A3E, lid = 0xFFC88A66;
        public boolean faceKnown;
        /** v38: the picture's own mouth is open (teeth or the dark inside show between the lips): it is never painted open further. */
        public boolean mouthOpen;
        public transient Cutout.Result pixelsForSampling;
        /** Bones and face for moving the picture (null = moved as one piece). */
        public Rig rig;
        /** The same picture soaked by rain: darker, deeper colours (made only for rainy stories). */
        public Object wetImg;
        /** v34: the picture's silhouette, small and soft (black with the outline's alpha): its shadow cast by the sun on the ground. */
        public Object shadowImg;
        /** Rim light along the outline, lit from the left / from the right (null = none). */
        public Object rimL, rimR;
        /** The picture with its turban / cap taken off, and that headwear on its own (null = none found). */
        public Object bareImg, hatImg;
        /** Where the headwear sits, in eye-distances from the point between the eyes (left, top, right, bottom). */
        public float hatX0, hatY0, hatX1, hatY1;
        public int hatW, hatH;
        /**
         * The same character seen from other sides (Figure3D.VIEW_ANGLES: three-quarter, side in mid-stride,
         * back), each a sprite of its own with its own rig, or null where there is none. The front picture
         * stays the identity; the views are used for walking, for two-shots and for over-the-shoulder reverses.
         */
        public Sprite[] views;
        /** The view at an index of Figure3D.VIEW_ANGLES, or null. */
        public Sprite view(int i) { return views == null || i < 0 || i >= views.length ? null : views[i]; }
        /** v27: the user's own pictures of this character from a sheet — angles, poses, expressions (null = none). */
        public java.util.List<PoseSprite> poses;
        /** v33: this front picture as a still pose picture (a beast drawn as it is), made on first use. */
        PoseSprite mainStill;
    }

    /**
     * One of the user's pictures of a character (v27): what it shows (read by PoseSense or tagged by the user) and
     * its sprite, read from the file only when a shot uses it (a hundred pictures are not all needed).
     */
    public static final class PoseSprite {
        public String file;
        public float angle = Angles.FRONT;
        public int pose = PoseSense.STAND, emotion = PoseSense.NEUTRAL;
        /** Its height against a standing picture of the character (1 = as tall; a sitting picture is lower). */
        public float hRatio = 1f;
        /** The face points given with it (mouth x, y, half-width, eyes), or null to find them in the picture. */
        public float[] facePoints;
        public boolean beast;
        /** v39: a picture of the face and shoulders only (a close-up of the sheet): used in face shots, never in a wide one. */
        public boolean closeUp;
        /** v39: the main picture's eye distance in its own heights (the measure every picture of the character is sized by), 0 = unknown. */
        float mainEye;
        Sprite sprite, main;
        Loader loader;
        public boolean faceKnown() { return facePoints != null && facePoints[0] > 0 || (sprite != null && sprite.faceKnown); }
        /** The sprite, read and prepared on first use. */
        public synchronized Sprite sprite() {
            if (sprite != null || loader == null) return sprite;
            Sprite v = makeSprite(loader, file, spriteSide, beast);
            if (v == null) return null;
            if (facePoints != null && facePoints[0] > 0) {
                v.mouthX = facePoints[0]; v.mouthY = facePoints[1]; v.mouthHW = facePoints[2];
                v.eyeLX = facePoints[3]; v.eyeLY = facePoints[4]; v.eyeRX = facePoints[5]; v.eyeRY = facePoints[6]; v.eyeR = facePoints[7];
                v.faceKnown = true;
            }
            if (main != null) { v.skin = main.skin; v.lip = main.lip; v.lid = main.lid; }
            // v39: sized by the face — the eyes are as far apart in every picture of the same person, so the picture's
            // height against the standing one is the main picture's eye distance over this one's (a close-up drawn by a
            // sheet as large as a whole figure is no giant)
            float eyeHere = v.faceKnown && Math.abs(angle) < 46 && v.h > 0 ? Math.abs(v.eyeRX - v.eyeLX) * v.w / (float) v.h : 0;
            float faceGeo = v.faceKnown && Math.abs(v.eyeRX - v.eyeLX) > 0.001f ? (v.mouthY - (v.eyeLY + v.eyeRY) / 2) * v.h / (Math.abs(v.eyeRX - v.eyeLX) * v.w) : 0;
            float sized = sizeBy(mainEye, eyeHere, faceGeo, pose, v.w, v.h, (v.eyeLY + v.eyeRY) / 2);
            if (sized > 0) { hRatio = sized; closeUp = sized < 0.5f; }
            // the rig only where the face must speak (a front or three-quarter with a face); a picture of a pose is drawn as it is
            boolean frontish = Math.abs(angle) < 46;
            mouthFromPicture(v.pixelsForSampling, v);
            if (frontish && v.faceKnown && pose == PoseSense.STAND) {
                try { v.rig = Rig.build(v.pixelsForSampling, v, main == null ? new Look() : mainLook, loader); } catch (RuntimeException e) { v.rig = null; }
            }
            // v39: every other picture with a face (sitting, drinking, waving, bending, a half-length picture) keeps its
            // body as it is but its face lives: the lips speak, the eyes blink and look, the feeling shows
            if (v.rig == null && frontish && v.faceKnown && !beast) {
                try { v.rig = Rig.buildStill(v.pixelsForSampling, v, main == null ? new Look() : mainLook, loader); } catch (RuntimeException e) { v.rig = null; }
            }
            if (v.pixelsForSampling != null) {
                try {
                    Cutout.Result cr = v.pixelsForSampling;
                    int[][] rl = RimLight.make(cr.px, cr.w, cr.h);
                    v.rimL = loader.create(rl[0], rl[2][0], rl[2][1]);
                    v.rimR = loader.create(rl[1], rl[2][0], rl[2][1]);
                } catch (Throwable ignored) { v.rimL = v.rimR = null; }
                v.shadowImg = castShadow(v.pixelsForSampling, loader);
            }
            v.pixelsForSampling = null;
            sprite = v;
            return v;
        }
        Look mainLook;
    }

    public static final class Backdrop {
        public Object img;
        public int w, h;
        public float x0 = 0, y0 = 0, x1 = 1, y1 = 1;  // crop window (fractions)
        public float ground = 0.9f;                    // where feet stand (fraction of frame height)
        /** A picture of one object on a transparent background (an insert), not a place. */
        public boolean object;
        /** Sky, water, waterfall and plants found in the picture (null = not read). */
        public Nature.Scan scan;
        /** The average colour of the picture's lower part (the ground): the colour of the bounce light (0 = unknown). */
        public int avgLow;
        /** v32: a picture (the user's own, or one made for the place) rather than a painted set: it carries its own light and mood. */
        public boolean picture;
    }

    public static final class Shot {
        public String scene;          // scene number ("8") or "" for any
        public String[] keys;
        public Backdrop pic;
        /** A picture of an object on a transparent background (an insert of the thing itself, shown once, over the darkened scene). */
        public boolean object;
    }

    public final Map<String, Sprite> sprites = new HashMap<String, Sprite>();   // by character id
    /** v34: the pictures of a character's changes of clothes, by "id#n" (the user's picture, else their own recoloured). */
    public final Map<String, Sprite> costumes = new HashMap<String, Sprite>();

    public Sprite costumeSprite(String id, int n) { return costumes.get(id + "#" + n); }
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

    /**
     * v38: the picture's own mouth, read from its pixels before the face mesh is built. Teeth (light, and much less
     * coloured than the skin around them) found just below or around the mouth point move the point onto the real
     * opening (a point left on the skin under the nose made the mesh open a second mouth there: the "new lips"), and
     * a mouth showing teeth or its dark inside is marked open — it is never painted open further.
     */
    public static void mouthFromPicture(Cutout.Result r, Sprite s) {
        s.mouthOpen = false;
        if (r == null || r.px == null || s == null || !s.faceKnown || s.mouthHW <= 0) return;
        int cx = Math.round(s.mouthX * r.w), cy = Math.round(s.mouthY * r.h), hw = Math.max(2, Math.round(s.mouthHW * r.w));
        // the skin's colourfulness around the mouth (the cheeks either side)
        int skinSat = 0, skinN = 0;
        for (int y = cy - hw / 2; y <= cy + hw / 2; y += 2) for (int x : new int[]{cx - hw * 3 / 2, cx + hw * 3 / 2}) {
            if (x < 0 || x >= r.w || y < 0 || y >= r.h) continue;
            int c = r.px[y * r.w + x];
            if ((c >>> 24) < 128) continue;
            int R = (c >> 16) & 255, G = (c >> 8) & 255, B = c & 255;
            skinSat += Math.max(R, Math.max(G, B)) - Math.min(R, Math.min(G, B)); skinN++;
        }
        int satMax = skinN > 0 ? Math.min(80, Math.max(30, skinSat / skinN - 25)) : 60;
        int teeth = 0, n = 0;
        long ty = 0;
        for (int y = cy - Math.round(hw * 0.6f); y <= cy + Math.round(hw * 1.2f); y++) {
            if (y < 0 || y >= r.h) continue;
            for (int x = cx - Math.round(hw * 0.6f); x <= cx + Math.round(hw * 0.6f); x++) {
                if (x < 0 || x >= r.w) continue;
                int c = r.px[y * r.w + x];
                if ((c >>> 24) < 128) continue;
                int R = (c >> 16) & 255, G = (c >> 8) & 255, B = c & 255;
                int lum = (R * 77 + G * 150 + B * 29) >> 8, sat = Math.max(R, Math.max(G, B)) - Math.min(R, Math.min(G, B));
                n++;
                if (lum > 140 && sat < satMax) { teeth++; ty += y; }
            }
        }
        if (n > 20 && teeth > n * 0.03f) {
            // the opening is just below the teeth's middle (the upper teeth show first)
            float my = ty / (float) teeth + 0.05f * hw;
            s.mouthY = Math.max(0, Math.min(1, my / r.h));
            s.mouthOpen = true;
            cy = Math.round(my);
        }
        // the dark inside of an open mouth, just at and below the (refined) point — a moustache above it never counts
        int dark = 0, m = 0;
        for (int y = cy - Math.round(hw * 0.1f); y <= cy + Math.round(hw * 0.5f); y++) {
            if (y < 0 || y >= r.h) continue;
            for (int x = cx - Math.round(hw * 0.5f); x <= cx + Math.round(hw * 0.5f); x++) {
                if (x < 0 || x >= r.w) continue;
                int c = r.px[y * r.w + x];
                if ((c >>> 24) < 128) continue;
                int lum = (((c >> 16) & 255) * 77 + ((c >> 8) & 255) * 150 + (c & 255) * 29) >> 8;
                m++;
                if (lum < 55) dark++;
            }
        }
        if (m > 20 && dark > m * 0.2f) s.mouthOpen = true;
    }

    /** The user's reverse angle of a scene's place (manifest line scene|<number>r|file), or null. */
    public Backdrop reverseBackdrop(int number) { return scenes.get(number + "r"); }

    /** The user's picture of the journey into a scene's place (manifest line scene|<number>j|file, ScenePlan), or null. */
    public Backdrop journeyBackdrop(int number) { return scenes.get(number + "j"); }

    public Shot shotFor(int scene, String text) {
        for (Shot s : shots) {
            if (s.scene.length() > 0 && !s.scene.equals(String.valueOf(scene))) continue;
            for (String k : s.keys) {
                k = k.trim();
                if (k.length() == 0) continue;
                // an English key is a whole word ("key" is not in "monkey"); a Hindi key is a stem
                if (k.charAt(0) < 0x0900 ? Txt.hasWord(text, k) : Txt.has(text, k)) return s;
            }
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
        r.pxRead = null;                                         // v34: the readings are done; the film draws the picture as it is
        Sprite s = new Sprite();
        s.w = r.w; s.h = r.h;
        s.img = L.create(r.px, r.w, r.h);
        Object[] mm = mips(L, r.px, r.w, r.h);
        s.imgHalf = mm[0]; s.imgQuarter = mm[1];
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
    public static void takeOffHeadwear(Sprite s, Loader L) { takeOffHeadwearImpl(s, L); }

    /** v34: a change of clothes made from a character's own picture (its pixels recoloured): the same face points, its own rig. */
    static Sprite costumeVariant(Sprite base, Cutout.Result cr, int[] px, Look look, Loader L) {
        Sprite v = new Sprite();
        v.w = base.w; v.h = base.h;
        v.img = L.create(px, cr.w, cr.h);
        Object[] mm = mips(L, px, cr.w, cr.h);
        v.imgHalf = mm[0]; v.imgQuarter = mm[1];
        v.mouthX = base.mouthX; v.mouthY = base.mouthY; v.mouthHW = base.mouthHW;
        v.eyeLX = base.eyeLX; v.eyeRX = base.eyeRX; v.eyeLY = base.eyeLY; v.eyeRY = base.eyeRY; v.eyeR = base.eyeR;
        v.faceKnown = base.faceKnown; v.skin = base.skin; v.lip = base.lip; v.lid = base.lid; v.turbanY = base.turbanY;
        int[] keep = cr.px;
        cr.px = px;
        try { v.rig = Rig.build(cr, v, look, L); } catch (RuntimeException e) { v.rig = null; } finally { cr.px = keep; }
        v.mouthOpen = base.mouthOpen;
        v.rimL = base.rimL; v.rimR = base.rimR; v.shadowImg = base.shadowImg;
        return v;
    }

    /**
     * v35: a picture's smaller copies — {half, quarter} — each made from the one above by averaging 2 x 2 pixels (the
     * colour weighted by its alpha, so the outline gets no dark fringe). A picture drawn much smaller than its own
     * pixels shimmers as it moves (every screen pixel picks one of many); the copy nearest its size on the screen does
     * not. null where the copy would be under 48 pixels.
     */
    public static Object[] mips(Loader L, int[] px, int w, int h) {
        Object[] out = new Object[2];
        if (L == null || px == null) return out;
        int[] cur = px;
        int cw = w, ch = h;
        try {
            for (int lv = 0; lv < 2; lv++) {
                int nw = (cw + 1) / 2, nh = (ch + 1) / 2;
                if (nw < 48 || nh < 48) break;
                int[] n = new int[nw * nh];
                for (int y = 0; y < nh; y++) {
                    int y0 = 2 * y, y1 = Math.min(ch - 1, y0 + 1);
                    for (int x = 0; x < nw; x++) {
                        int x0 = 2 * x, x1 = Math.min(cw - 1, x0 + 1);
                        int c0 = cur[y0 * cw + x0], c1 = cur[y0 * cw + x1], c2 = cur[y1 * cw + x0], c3 = cur[y1 * cw + x1];
                        int a0 = c0 >>> 24, a1 = c1 >>> 24, a2 = c2 >>> 24, a3 = c3 >>> 24, as = a0 + a1 + a2 + a3;
                        if (as == 0) continue;
                        int r = (((c0 >> 16) & 255) * a0 + ((c1 >> 16) & 255) * a1 + ((c2 >> 16) & 255) * a2 + ((c3 >> 16) & 255) * a3 + as / 2) / as;
                        int g = (((c0 >> 8) & 255) * a0 + ((c1 >> 8) & 255) * a1 + ((c2 >> 8) & 255) * a2 + ((c3 >> 8) & 255) * a3 + as / 2) / as;
                        int b = ((c0 & 255) * a0 + (c1 & 255) * a1 + (c2 & 255) * a2 + (c3 & 255) * a3 + as / 2) / as;
                        n[y * nw + x] = (((as + 2) / 4) << 24) | (r << 16) | (g << 8) | b;
                    }
                }
                out[lv] = L.create(n, nw, nh);
                cur = n; cw = nw; ch = nh;
            }
        } catch (Throwable e) {
            // out of memory: the picture is drawn from its full size, as before
        }
        return out;
    }

    /**
     * v35: of a picture and its smaller copies, the one to draw at this size on the screen (shrink = its pixels per
     * screen pixel): the full picture down to two and a half times smaller (sampled between four pixels, a picture up
     * to that small stays smooth and is the sharpest), then the half, then the quarter.
     */
    public static Object mip(Object full, Object half, Object quarter, float shrink) {
        if (shrink >= 5f && quarter != null) return quarter;
        if (shrink >= 2.5f && half != null) return half;
        return full;
    }

    /** v34: a soft, small silhouette of a cut-out (at most 64 px on its long side, blurred once) for the sun's cast shadow. */
    static Object castShadow(Cutout.Result cr, Loader L) {
        if (cr == null || cr.px == null || cr.w < 4 || cr.h < 4) return null;
        int big = Math.max(cr.w, cr.h);
        int sw = Math.max(4, Math.round(cr.w * 64f / big)), sh = Math.max(4, Math.round(cr.h * 64f / big));
        float[] a = new float[sw * sh];
        for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) {
            int x0 = x * cr.w / sw, x1 = Math.max(x0 + 1, (x + 1) * cr.w / sw), y0 = y * cr.h / sh, y1 = Math.max(y0 + 1, (y + 1) * cr.h / sh);
            long sum = 0; int n = 0;
            for (int yy = y0; yy < y1; yy += Math.max(1, (y1 - y0) / 4)) for (int xx = x0; xx < x1; xx += Math.max(1, (x1 - x0) / 4)) { sum += cr.px[yy * cr.w + xx] >>> 24; n++; }
            a[y * sw + x] = n == 0 ? 0 : sum / (float) n;
        }
        int[] out = new int[sw * sh];
        for (int y = 0; y < sh; y++) for (int x = 0; x < sw; x++) {
            float sum = 0; int n = 0;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                int xx = x + dx, yy = y + dy;
                if (xx < 0 || yy < 0 || xx >= sw || yy >= sh) { n++; continue; }
                sum += a[yy * sw + xx]; n++;
            }
            int al = Math.min(255, Math.round(sum / n));
            out[y * sw + x] = al << 24;
        }
        try { return L.create(out, sw, sh); } catch (RuntimeException e) { return null; }
    }

    private static void takeOffHeadwearImpl(Sprite s, Loader L) {
        Cutout.Result r = s.pixelsForSampling;
        if (r == null || !s.faceKnown) return;
        try {
            Headwear hw = Headwear.strip(r.px, r.w, r.h, s.eyeLX * r.w, s.eyeLY * r.h, s.eyeRX * r.w, s.eyeRY * r.h, s.skin);
            int[] bare;
            int[] hatColors;
            if (hw != null) {
                bare = hw.bare;
                hatColors = palette(hw.hat, hw.hat.length);
                s.hatImg = L.create(hw.hat, hw.hx1 - hw.hx0 + 1, hw.hy1 - hw.hy0 + 1);
                s.hatW = hw.hx1 - hw.hx0 + 1; s.hatH = hw.hy1 - hw.hy0 + 1;
                s.hatX0 = hw.relX0; s.hatY0 = hw.relY0; s.hatX1 = hw.relX1; s.hatY1 = hw.relY1;
            } else if (s.turbanY > 0) {
                // the headwear could not be cut out as a piece: the picture's top is taken off along the cut line and a
                // bald head painted, so the bare head is drawn through the mesh like everyone else (v22)
                bare = simpleBare(r, s);
                hatColors = palette(r.px, Math.min(r.px.length, Math.round(s.turbanY * r.h) * r.w));
            } else return;
            // what the headwear leaves beside the bare head (a turban's tail, a feather, a band's end): pixels of the
            // headwear's own colour above the shoulders and outside the face go too (v22)
            clearHeadwearLeftovers(bare, r, s, hatColors);
            s.bareImg = L.create(bare, r.w, r.h);
            if (s.rig != null) s.rig.faceBareImg = s.rig.faceFrom(bare, r.w, r.h, L);
        } catch (RuntimeException e) {
            s.bareImg = null; s.hatImg = null;
        }
    }

    static int averageOpaque(int[] px, int fallback) {
        long sr = 0, sg = 0, sb = 0, n = 0;
        if (px != null) for (int c : px) { if ((c >>> 24) < 128) continue; sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; n++; }
        if (n == 0) return fallback;
        return 0xFF000000 | ((int) (sr / n) << 16) | ((int) (sg / n) << 8) | (int) (sb / n);
    }

    static int averageNonSkin(int[] px, int w, int y0, int y1, int h) {
        long sr = 0, sg = 0, sb = 0, n = 0;
        for (int y = Math.max(0, y0); y < Math.min(y1, h); y++) for (int x = 0; x < w; x++) {
            int c = px[y * w + x];
            if ((c >>> 24) < 128 || Cutout.isSkin(c)) continue;
            sr += (c >> 16) & 255; sg += (c >> 8) & 255; sb += c & 255; n++;
        }
        if (n == 0) return 0;
        return 0xFF000000 | ((int) (sr / n) << 16) | ((int) (sg / n) << 8) | (int) (sb / n);
    }

    /** The picture without its top (above the cut line), a bald head painted in its place. */
    static int[] simpleBare(Cutout.Result r, Sprite s) {
        int[] out = r.px.clone();
        int cutRow = Math.round(s.turbanY * r.h);
        float faceCx = (s.eyeLX + s.eyeRX) / 2f * r.w, faceW = Math.max(0.1f, s.eyeRX - s.eyeLX) * r.w * 1.3f;
        for (int y = 0; y < Math.min(cutRow, r.h); y++) for (int x = 0; x < r.w; x++) if (!Cutout.isSkin(out[y * r.w + x])) out[y * r.w + x] = 0;
        int skin = s.skin, shade = Puppet.shade(s.skin, 0.9f);
        float cy = cutRow + faceW * 0.12f, rx = faceW * 0.5f, ry = faceW * 0.36f;
        for (int y = Math.max(0, Math.round(cy - ry)); y < Math.min(r.h, cutRow + 1); y++) for (int x = Math.max(0, Math.round(faceCx - rx)); x < Math.min(r.w, Math.round(faceCx + rx)); x++) {
            float dx = (x - faceCx) / rx, dy = (y - cy) / ry;
            float d = dx * dx + dy * dy;
            if (d > 1) continue;
            if ((out[y * r.w + x] >>> 24) > 200 && Cutout.isSkin(out[y * r.w + x])) continue;
            out[y * r.w + x] = d > 0.8f ? shade : skin;
        }
        return out;
    }

    /**
     * v38: the main colours of a piece of headwear (up to eight, each at least 3% of its pixels): a turban is blue
     * and red, not their purple average.
     */
    static int[] palette(int[] px, int n) {
        int[] count = new int[512];
        long[] sr = new long[512], sg = new long[512], sb = new long[512];
        int total = 0;
        for (int i = 0; i < n && i < px.length; i++) {
            int c = px[i];
            if ((c >>> 24) < 128 || Cutout.isSkin(c)) continue;
            int R = (c >> 16) & 255, G = (c >> 8) & 255, B = c & 255;
            int b = (R >> 5) << 6 | (G >> 5) << 3 | (B >> 5);
            count[b]++; sr[b] += R; sg[b] += G; sb[b] += B; total++;
        }
        java.util.List<Integer> out = new java.util.ArrayList<Integer>();
        for (int k = 0; k < 8 && total > 0; k++) {
            int best = -1;
            for (int b = 0; b < 512; b++) if (count[b] > 0 && (best < 0 || count[b] > count[best])) best = b;
            if (best < 0 || count[best] < total * 0.03f) break;
            out.add(0xFF000000 | (int) (sr[best] / count[best]) << 16 | (int) (sg[best] / count[best]) << 8 | (int) (sb[best] / count[best]));
            count[best] = 0;
        }
        int[] a = new int[out.size()];
        for (int i = 0; i < a.length; i++) a[i] = out.get(i);
        return a;
    }

    /**
     * What the headwear leaves beside the bare head (a turban's tail, a feather, a band's end): pixels of the
     * headwear's own colours, beside the face and above the eyes, go. v38: only those — the eyebrows, a
     * moustache's curl, the cheek and a hand raised to the face beside it stay (clearing everything that was not skin
     * there cut holes through the guard's face, the wall showing through them).
     */
    static void clearHeadwearLeftovers(int[] bare, Cutout.Result r, Sprite s, int[] hatColors) {
        if (hatColors == null || hatColors.length == 0) return;
        int faceCx = Math.round((s.eyeLX + s.eyeRX) / 2f * r.w), faceHalf = Math.round(Math.max(0.1f, s.eyeRX - s.eyeLX) * r.w * 0.8f);
        float eyeY = (s.eyeLY + s.eyeRY) / 2f;
        // (only above the eyes: below them, beside the face, are the ears, the cheeks, a moustache's curl and a raised
        // hand — an ear's shaded skin came close enough to a red band to be cut away)
        // Below the eyes (down to the neck) only the headwear's cool colours (blue, green, purple — never a skin's) or its
        // vivid dyed ones go:
        // a turban's flap hanging beside the ear, but not the ear
        int eyes = Math.round(Math.max(0, eyeY - Math.max(0.1f, s.eyeRX - s.eyeLX) * 0.15f) * r.h);
        int neck = Math.round(Math.min(r.h - 1, (s.mouthY + Math.max(0.02f, s.mouthY - eyeY) * 1.0f) * r.h));
        for (int y = 0; y < neck; y++) {
            for (int x = 0; x < r.w; x++) {
                int c = bare[y * r.w + x];
                if ((c >>> 24) < 20 || Cutout.isSkin(c)) continue;
                if (Math.abs(x - faceCx) <= faceHalf) continue;
                boolean hat = false;
                int cr = (c >> 16) & 255, cg = (c >> 8) & 255, cb = c & 255;
                boolean vivid = Math.max(cr, Math.max(cg, cb)) - Math.min(cr, Math.min(cg, cb)) >= 130;     // a dyed band, not shaded skin
                for (int h : hatColors) {
                    boolean cool = (h & 255) > ((h >> 16) & 255) + 20;
                    if (y < eyes ? Cutout.dist(c, h) < 75 : (cool || vivid) && Cutout.dist(c, h) < 60) { hat = true; break; }
                }
                if (hat) bare[y * r.w + x] = 0;
            }
        }
        // what is left floating beside the head (a band's end the colours missed) is joined to nothing: everything
        // above the neck that cannot be reached from the face or from the body below the neck goes
        boolean[] seen = new boolean[r.w * (neck + 1)];
        int[] stack = new int[r.w * (neck + 1)];
        int sp = 0;
        for (int y = 0; y <= neck && y < r.h; y++) for (int x = Math.max(0, faceCx - faceHalf); x <= Math.min(r.w - 1, faceCx + faceHalf); x++) {
            int i = y * r.w + x;
            if ((bare[i] >>> 24) >= 20 && !seen[i]) { seen[i] = true; stack[sp++] = i; }
        }
        for (int x = 0; x < r.w && neck < r.h; x++) {
            int i = neck * r.w + x;
            if ((bare[i] >>> 24) >= 20 && !seen[i]) { seen[i] = true; stack[sp++] = i; }
        }
        while (sp > 0) {
            int i = stack[--sp], x = i % r.w, y = i / r.w;
            for (int k = 0; k < 4; k++) {
                int nx = x + (k == 0 ? 1 : k == 1 ? -1 : 0), ny = y + (k == 2 ? 1 : k == 3 ? -1 : 0);
                if (nx < 0 || nx >= r.w || ny < 0 || ny > neck) continue;
                int j = ny * r.w + nx;
                if (seen[j] || (bare[j] >>> 24) < 20) continue;
                seen[j] = true; stack[sp++] = j;
            }
        }
        for (int y = 0; y < neck; y++) for (int x = 0; x < r.w; x++) {
            int i = y * r.w + x;
            if (!seen[i] && (bare[i] >>> 24) >= 20) bare[i] = 0;
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
        b.picture = true;
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
                b.avgLow = averageLow(px, d[0], d[1]);
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

    /** The average colour of the bottom quarter of a picture (the ground that bounces the key light back up). */
    public static int averageLow(int[] px, int w, int h) {
        long r = 0, g = 0, b = 0, n = 0;
        for (int y = h * 3 / 4; y < h; y += 2) for (int x = 0; x < w; x += 2) {
            int c = px[y * w + x];
            r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255; n++;
        }
        if (n == 0) return 0;
        return 0xFF000000 | ((int) (r / n) << 16) | ((int) (g / n) << 8) | (int) (b / n);
    }

    // ------------------------------------------------------------------ manifest

    /**
     * Reads a cast manifest (see assets/sample/cast.txt) and loads all pictures.
     * Character names are matched against the parsed story.
     */
    /** v26: the size pictures are read at follows the film's output (set by the job before loading): a 1080p film reads sharper pictures than a 480p one. */
    public static int spriteSide = 1100, backdropSide = 1600;

    /**
     * v35: the sizes the pictures of a film are read at, for an output this tall — {character side, place side}. A
     * character at twice the output's height (a close-up enlarges a face; read larger, it stays sharp), a place at
     * 1.6 times the output's width (the camera comes in on it too); never more than the phone's free memory allows
     * for this many pictures (their pixels live outside the app's heap since Android 8: all of them together take at
     * most two fifths of the memory free now), never less than the sizes of v26-v34 (1.25 times the height, 1.15
     * times the width). heapMax (what Android gives the app) caps the size one picture is read at.
     */
    public static int[] sizesFor(int outHeight, long heapMax, long freeMemory, int figures, int places) {
        boolean big = heapMax >= (512L << 20);
        int capS = big ? 2600 : 2000, capB = big ? 3200 : 2600;
        int floorS = Math.max(900, Math.min(capS, Math.round(outHeight * 1.25f)));
        int floorB = Math.max(1280, Math.min(capB, Math.round(outHeight * 16f / 9f * 1.15f)));
        int s = Math.max(floorS, Math.min(capS, outHeight * 2));
        int b = Math.max(floorB, Math.min(capB, Math.round(outHeight * 16f / 9f * 1.6f)));
        // a cut-out figure is about half as wide as it is tall; with its smaller copies, rim lights and face layer about
        // 1.9 pictures of it; its meshes take some 6 MB whatever its size. A place picture is 16:9.
        double fixed = figures * 6e6;
        double need = fixed + figures * (s * (double) s * 0.5 * 4 * 1.9) + places * (b * (double) b * 9 / 16 * 4);
        double budget = freeMemory * 0.4;
        if (need > budget) {
            double k = Math.sqrt(Math.max(0.05, (budget - fixed) / Math.max(1, need - fixed)));
            s = Math.max(floorS, (int) (s * k));
            b = Math.max(floorB, (int) (b * k));
        }
        return new int[]{s, b};
    }

    /** v35: how many character pictures (characters, views, poses, changes of clothes) and place pictures a cast manifest loads. */
    public static int[] pictureCounts(String manifest) {
        int figures = 0, places = 0;
        if (manifest != null) for (String raw : manifest.split("\n")) {
            String l = raw.trim();
            if (l.startsWith("char|") || l.startsWith("view|") || l.startsWith("pose|") || l.startsWith("costume|")) figures++;
            else if (l.startsWith("scene|") || l.startsWith("shot|") || l.startsWith("title|") || l.startsWith("end|")) places++;
        }
        return new int[]{figures, places};
    }

    public static Art fromManifest(String text, Story story, Loader L) {
        Art art = new Art();
        boolean rainy = mentions(story, "बारिश", "वर्षा", "बरसात", "बूँदाबाँदी", "तूफ़ान", "तूफान", "rain", "storm", "drizzl", "monsoon");
        java.util.List<String[]> views = new java.util.ArrayList<String[]>();
        java.util.List<String[]> poses = new java.util.ArrayList<String[]>();
        java.util.List<String[]> costumeLines = new java.util.ArrayList<String[]>();
        // v39: every character's own pictures first: a front picture that is only a face and shoulders (a sheet's
        // close-up chosen as the front) is swapped for the user's full-length standing front picture, and kept as a
        // close-up — everything is sized against the front picture, so a close-up front made giants of the others
        java.util.Map<String, java.util.List<String[]>> poseLines = new java.util.HashMap<String, java.util.List<String[]>>();
        for (String raw : text.split("\n")) {
            String[] f = raw.trim().split("\\|");
            if (f[0].equals("pose") && f.length >= 15) {
                java.util.List<String[]> l = poseLines.get(f[1]);
                if (l == null) { l = new java.util.ArrayList<String[]>(); poseLines.put(f[1], l); }
                l.add(f);
            }
        }
        java.util.List<String> extra = new java.util.ArrayList<String>();
        for (String raw : text.split("\n")) {
            String line = raw.trim();
            if (line.length() == 0 || line.startsWith("#")) continue;
            String[] f = line.split("\\|");
            if (f[0].equals("char") && f.length >= 3 && poseLines.containsKey(f[1])) {
                String[] better = fullLengthFront(L, f, poseLines.get(f[1]));
                if (better != null) {
                    extra.add("pose|" + f[1] + "|" + f[2] + "|0|0|0|1");      // the close-up stays, for the face shots
                    f = better;
                    art.notes.add("front picture of " + f[1] + ": the full-length one (" + f[2] + ") instead of a close-up");
                }
            }
            try {
                if (f[0].equals("pose") && f.length >= 6) {
                    poses.add(f);            // v27: after the characters, below
                } else if (f[0].equals("view") && f.length >= 4) {
                    views.add(f);            // after the characters, below
                } else if (f[0].equals("costume") && f.length >= 3) {
                    costumeLines.add(f);     // v34: after the characters, below
                } else if (f[0].equals("char") && f.length >= 3) {
                    Story.CharacterDef c = ScriptParser.resolve(story, f[1]);
                    if (c == null) continue;
                    boolean beast = c.look != null && (c.look.kind == Look.ANIMAL || c.look.kind == Look.BIRD);
                    Sprite s = makeSprite(L, f[2], spriteSide, beast);
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
                    mouthFromPicture(s.pixelsForSampling, s);
                    try { s.rig = Rig.build(s.pixelsForSampling, s, c.look, L); } catch (RuntimeException e) { s.rig = null; }
                    if (c.look != null && c.look.headwear == Look.HW_TURBAN && !beast) takeOffHeadwear(s, L);
                    if (s.rig != null && s.rig.face && !beast) {
                        // the face again from the full picture, for sharp close-ups
                        try {
                            // (v35: up to 1.6 times the body's size, at least 2400 px as before, at most 3600)
                            int[] hi = L.decode(f[2], Math.max(2400, Math.min(3600, Math.round(spriteSide * 1.6f))));
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
                    s.shadowImg = castShadow(s.pixelsForSampling, L);
                    if (rainy && s.pixelsForSampling != null) {
                        Cutout.Result cr = s.pixelsForSampling;
                        s.wetImg = L.create(wetPixels(cr.px), cr.w, cr.h);
                    }
                    // v34: every change of clothes of the story, from this very picture recoloured (a picture of the
                    // new clothes, when the user gives one, replaces it below)
                    if (s.pixelsForSampling != null && !beast) for (int ci = 0; ci < c.costumes.size(); ci++) {
                        try {
                            Look cl = c.costumes.get(ci).look;
                            Cutout.Result cr = s.pixelsForSampling;
                            Sprite v = costumeVariant(s, cr, Costumes.recolour(cr.px, cr.w, cr.h, s.faceKnown ? Math.max(cr.chinY, s.mouthY + 0.02f) : 0.3f, cl.primary, cl.secondary), cl, L);
                            if (v != null) art.costumes.put(c.id + "#" + (ci + 1), v);
                        } catch (Throwable ignored) { /* the character's own picture stays */ }
                    }
                    s.pixelsForSampling = null;
                    art.sprites.put(c.id, s);
                } else if (f[0].equals("scene") && f.length >= 3) {
                    Backdrop b = makeBackdrop(L, f[2], backdropSide);
                    if (b == null) continue;
                    if (f.length >= 8) {
                        b.x0 = Float.parseFloat(f[3]); b.y0 = Float.parseFloat(f[4]);
                        b.x1 = Float.parseFloat(f[5]); b.y1 = Float.parseFloat(f[6]);
                        b.ground = Float.parseFloat(f[7]);
                    }
                    art.scenes.put(f[1], b);
                } else if (f[0].equals("shot") && f.length >= 4) {
                    Backdrop b = makeBackdrop(L, f[3], backdropSide);
                    if (b == null) continue;
                    Shot s = new Shot();
                    s.scene = f[1];
                    s.keys = f[2].split(",");
                    s.pic = b;
                    s.object = f.length >= 5 && f[4].trim().equals("object");
                    b.object = s.object;
                    art.shots.add(s);
                } else if (f[0].equals("title") || f[0].equals("end")) {
                    Backdrop b = makeBackdrop(L, f[1], backdropSide);
                    if (b == null) continue;
                    boolean txt = f.length < 3 || !f[2].equals("0");
                    if (f[0].equals("title")) { art.title = b; art.titleText = txt; }
                    else { art.end = b; art.endText = txt; }
                }
            } catch (RuntimeException e) {
                // a bad line must never stop the film; skip it
            }
        }
        // the views of the characters (view|name|angle|file|mouthX|mouthY|mouthHW|eyeLX|eyeLY|eyeRX|eyeRY|eyeR)
        // v34: the user's own pictures of a change of clothes ("costume|key#n|file") replace the recoloured ones
        for (String[] f : costumeLines) {
            try {
                int hash = f[1].lastIndexOf('#');
                if (hash <= 0) continue;
                Story.CharacterDef c = ScriptParser.resolve(story, f[1].substring(0, hash));
                int n = Integer.parseInt(f[1].substring(hash + 1).trim());
                if (c == null || n < 1 || n > c.costumes.size()) continue;
                Sprite v = makeSprite(L, f[2], spriteSide, false);
                if (v == null) continue;
                if (f.length >= 11) {
                    // v34: a doll of the new look made by the 3D maker carries its own face points (like a char line)
                    float[] fp = new float[8];
                    for (int i = 0; i < 8; i++) fp[i] = Float.parseFloat(f[3 + i].trim());
                    if (fp[0] > 0) {
                        v.mouthX = fp[0]; v.mouthY = fp[1]; v.mouthHW = fp[2];
                        v.eyeLX = fp[3]; v.eyeLY = fp[4]; v.eyeRX = fp[5]; v.eyeRY = fp[6]; v.eyeR = fp[7];
                        v.faceKnown = true;
                    }
                    if (f.length >= 12) v.turbanY = Float.parseFloat(f[11].trim());
                    resample(v);
                }
                mouthFromPicture(v.pixelsForSampling, v);
                try { v.rig = Rig.build(v.pixelsForSampling, v, c.costumes.get(n - 1).look, L); } catch (RuntimeException e) { v.rig = null; }
                v.shadowImg = castShadow(v.pixelsForSampling, L);
                v.pixelsForSampling = null;
                art.costumes.put(c.id + "#" + n, v);
            } catch (RuntimeException ignored) { /* a broken line never stops the film */ }
        }
        for (String[] f : views) {
            try {
                Story.CharacterDef c = ScriptParser.resolve(story, f[1]);
                if (c == null) continue;
                Sprite main = art.sprites.get(c.id);
                int idx = Figure3D.viewIndex(Float.parseFloat(f[2].trim()));
                if (main == null || idx < 0) continue;
                Sprite v = makeSprite(L, f[3], spriteSide, false);
                if (v == null) continue;
                v.faceKnown = false;
                if (f.length >= 12) {
                    float[] p = new float[8];
                    for (int i = 0; i < 8; i++) p[i] = Float.parseFloat(f[4 + i].trim());
                    if (p[0] > 0 && p[1] > 0) {
                        v.mouthX = p[0]; v.mouthY = p[1]; v.mouthHW = p[2];
                        v.eyeLX = p[3]; v.eyeLY = p[4]; v.eyeRX = p[5]; v.eyeRY = p[6]; v.eyeR = p[7];
                        v.faceKnown = true;
                    }
                }
                v.skin = main.skin; v.lip = main.lip; v.lid = main.lid;
                mouthFromPicture(v.pixelsForSampling, v);
                try { v.rig = Rig.build(v.pixelsForSampling, v, c.look, L); } catch (RuntimeException e) { v.rig = null; }
                if (v.rig != null && v.pixelsForSampling != null) {
                    try {
                        Cutout.Result cr = v.pixelsForSampling;
                        int[][] rl = RimLight.make(cr.px, cr.w, cr.h);
                        v.rimL = L.create(rl[0], rl[2][0], rl[2][1]);
                        v.rimR = L.create(rl[1], rl[2][0], rl[2][1]);
                    } catch (Throwable ignored) { v.rimL = v.rimR = null; }
                    v.shadowImg = castShadow(v.pixelsForSampling, L);
                    if (rainy) v.wetImg = L.create(wetPixels(v.pixelsForSampling.px), v.pixelsForSampling.w, v.pixelsForSampling.h);
                }
                v.pixelsForSampling = null;
                if (main.views == null) main.views = new Sprite[Figure3D.VIEW_ANGLES.length];
                main.views[idx] = v;
            } catch (RuntimeException e) {
                // a bad line must never stop the film; skip it
            }
        }
        for (String e : extra) poses.add(e.split("\\|"));
        // v27: the user's own pictures of each character (pose|name|file|angle|pose|emotion|hRatio|mouthX|mouthY|mouthHW|eyeLX|eyeLY|eyeRX|eyeRY|eyeR)
        for (String[] f : poses) {
            try {
                Story.CharacterDef c = ScriptParser.resolve(story, f[1]);
                if (c == null) continue;
                Sprite main = art.sprites.get(c.id);
                if (main == null) continue;
                PoseSprite p = new PoseSprite();
                p.file = f[2];
                p.angle = Float.parseFloat(f[3].trim());
                p.pose = Integer.parseInt(f[4].trim());
                p.emotion = Integer.parseInt(f[5].trim());
                if (f.length >= 7) { try { p.hRatio = Math.max(0.2f, Math.min(1.5f, Float.parseFloat(f[6].trim()))); } catch (NumberFormatException ignored) {} }
                if (f.length >= 15) {
                    float[] pts = new float[8];
                    for (int i = 0; i < 8; i++) pts[i] = Float.parseFloat(f[7 + i].trim());
                    if (pts[0] > 0 && pts[1] > 0) p.facePoints = pts;
                }
                p.beast = c.look != null && (c.look.kind == Look.ANIMAL || c.look.kind == Look.BIRD);
                p.main = main; p.mainLook = c.look != null ? c.look : new Look(); p.loader = L;
                // v39: sized by the face (see sprite()): estimated now from the picture's proportions, exact when it is read
                if (main.faceKnown && main.h > 0) p.mainEye = Math.abs(main.eyeRX - main.eyeLX) * main.w / (float) main.h;
                if (p.mainEye > 0 && p.facePoints != null && Math.abs(p.angle) < 46 && !p.beast) {
                    try {
                        int[] d = L.decode(p.file, 64);
                        if (d != null && d[1] > 0) {
                            float[] fp = p.facePoints;
                            float ed = Math.abs(fp[5] - fp[3]);
                            float eyeHere = ed * d[0] / (float) d[1];
                            float faceGeo = ed > 0.001f ? (fp[1] - (fp[4] + fp[6]) / 2) * d[1] / (ed * d[0]) : 0;
                            float sized = sizeBy(p.mainEye, eyeHere, faceGeo, p.pose, d[0], d[1], (fp[4] + fp[6]) / 2);
                            if (sized > 0) { p.hRatio = sized; p.closeUp = sized < 0.5f; }
                        }
                    } catch (RuntimeException ignored) { }
                }
                if (main.poses == null) main.poses = new java.util.ArrayList<PoseSprite>();
                main.poses.add(p);
            } catch (RuntimeException e) {
                // a bad view never stops the film
            }
        }
        return art;
    }

    /**
     * v39: a picture's height against the standing front picture, from its face: the main picture's eye distance over
     * this one's (both in their own pictures' heights), when the face found is a plausible face (the mouth below the
     * eyes about as far as the eyes are apart) and the size fits the pose; otherwise the pose's usual size — a face and
     * shoulders a third, a seated figure three fifths, a standing one the whole. 0 = leave it as it is.
     */
    static float sizeBy(float mainEye, float eyeHere, float faceGeo, int pose, int w, int h, float eyeY) {
        if (w <= 0 || h <= 0) return 0;
        float aspect = h / (float) w;
        boolean bust = aspect < 1.3f && eyeY > 0.28f;            // the face fills the top half: a face-and-shoulders picture
        boolean seated = pose == PoseSense.SIT || pose == PoseSense.CROUCH;
        float lo = bust ? 0.18f : seated ? 0.42f : pose == PoseSense.LIE ? 0.25f : 0.75f, hi = bust ? 0.55f : seated ? 0.82f : pose == PoseSense.LIE ? 0.6f : 1.12f;
        float typical = bust ? 0.34f : seated ? 0.62f : pose == PoseSense.LIE ? 0.4f : 1f;
        if (mainEye > 0 && eyeHere > 0.005f && faceGeo > 0.45f && faceGeo < 1.7f) {
            float r = mainEye / eyeHere;
            if (r >= lo && r <= hi) return r;
        }
        return typical;
    }

    /**
     * v39: when the front picture is no full-length figure (about as wide as it is tall: a face and shoulders, a
     * sitting picture), the best full-length standing front picture among the character's own (eyes near the top, a
     * tall figure, a calm or happy face) as a char line, or null when the front is fine or there is none.
     */
    static String[] fullLengthFront(Loader L, String[] charLine, java.util.List<String[]> poses) {
        try {
            int[] d = L.decode(charLine[2], 64);
            if (d == null || d[1] >= d[0] * 1.35f) return null;           // tall enough: a whole figure
            float mainAspect = d[1] / (float) Math.max(1, d[0]);
            String[] best = null;
            float bestScore = -1;
            for (String[] p : poses) {
                float angle = Float.parseFloat(p[3].trim());
                int pose = Integer.parseInt(p[4].trim()), emo = Integer.parseInt(p[5].trim());
                if (Math.abs(angle) > 1 || (pose != PoseSense.STAND && pose != PoseSense.WAVE) || (emo != PoseSense.NEUTRAL && emo != PoseSense.HAPPY)) continue;
                float eyeY = (Float.parseFloat(p[11].trim()) + Float.parseFloat(p[13].trim())) / 2, mouthX = Float.parseFloat(p[7].trim());
                // a child's head is a bigger part of the figure: the eyes up to a third of the way down
                if (mouthX <= 0 || eyeY <= 0 || eyeY > 0.35f) continue;
                int[] pd = L.decode(p[2], 64);
                if (pd == null) continue;
                float aspect = pd[1] / (float) Math.max(1, pd[0]);
                if (aspect < Math.max(1.3f, mainAspect + 0.3f)) continue;       // clearly more of the figure than the front
                float sc = aspect - 2 * eyeY + (emo == PoseSense.NEUTRAL ? 0.3f : 0) + (pose == PoseSense.STAND ? 0.3f : 0);
                if (sc > bestScore) { bestScore = sc; best = p; }
            }
            if (best == null) return null;
            // char|name|file|mouthX|mouthY|mouthHW|eyeLX|eyeLY|eyeRX|eyeRY|eyeR|turbanY
            return new String[]{"char", charLine[1], best[2], best[7], best[8], best[9], best[10], best[11], best[12], best[13], best[14], "0"};
        } catch (RuntimeException e) {
            return null;
        }
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
