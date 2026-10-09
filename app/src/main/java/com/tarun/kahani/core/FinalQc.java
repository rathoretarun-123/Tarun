package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The last steps of the Technical Director pipeline (section 11), done by the studio itself:
 *
 *  6. "If video fails QC (shaky / morph), reduce motion 80% and regenerate from the same first frame":
 *     before the film is made, every shot is played frame by frame at check size — the frames a viewer would see
 *     one by one at 0.25x speed — and every two frames in a row are compared. A locked camera and one small
 *     action change little between two frames; a shot whose picture changes more (boiling, morphing) or whose
 *     whole frame moves (a shake) has its motion cut by 80% ({@link TechnicalDirector#MOTION_FIX}, the same
 *     "calmer" fix as Human QC) and is played again from the same first frame. The result goes in the shot list.
 *  8. "Final QC: play at 0.25x speed, check for morphing, floating, finger count": the finished film is metered
 *     frame by frame while it is written ({@link Meter}); floating is read from the renderer's own record of
 *     where every standing character's feet were drawn against the ground line; the finger count is a rule of
 *     the pictures (drawn hands never change; AI hands are asked for with five fingers or hidden).
 *
 * The camera shakes the story asks for (an earthquake, thunder, a blast) and the whole-frame effects of the story
 * (a flash, lightning, a blackout, a glitch, flickering light) are not shakes or morphing: they are counted apart.
 */
public final class FinalQc {
    private FinalQc() {}

    /** What the platform gives the check: a surface to draw a frame on and read its pixels back. */
    public interface Surface {
        Gfx gfx();
        /** The pixels of the last frame drawn (ARGB, row by row). */
        int[] pixels();
        int width();
        int height();
    }

    public interface Progress {
        void at(int done, int total);
        /** True stops the check early (the result holds what was checked so far). */
        boolean cancelled();
    }

    public static final int FPS = 24;
    /** Frames in a row per window, and windows per shot (its start, its middle and its end). */
    public static final int FRAMES = 4, WINDOWS = 3;
    /** Frames are compared on a grid of this many cells across (a cell is a few pixels: boiling shows, grain does not). */
    public static final int GRID_W = 96;
    /**
     * Two frames in a row of a steady shot differ by less than this per cell (0-255 grey, averaged over the frame).
     * Measured over four test films (532 shots, drawn and photographed characters, rain, storms, lip-sync close-ups):
     * the largest honest value was 9.3 (a photographed face speaking in an extreme close-up); the limit is 1.5x that.
     */
    public static final float BOIL = 14f;
    /** ... and fewer than this share of the cells change at all (more means the whole picture moved: a shake). The largest honest value measured was 23%. */
    public static final float SHAKE = 0.5f;
    /** A cell has changed from this difference on (0-255). */
    public static final int CHANGED = 20;
    /** A standing character floats when its feet are drawn higher than this share of its height above the ground. */
    public static final float FLOAT_TOLERANCE = 0.025f;

    /** What step 6 found. */
    public static final class Result {
        public int shots, checked, skipped, windows, pairs, boiling, shaking, calmed, stillLively, storyShakes, effects, feetChecked, floating;
        public float worstBoil, worstMoved;
        public String worstAt = "";
        public final List<String> notes = new ArrayList<String>();

        public String text() {
            StringBuilder b = new StringBuilder();
            b.append("FINAL QC — pipeline steps 6 and 8, done by the studio (the frames a viewer sees one by one at 0.25x speed)\n");
            b.append(String.format(Locale.US, "• Step 6 — every shot played frame by frame at check size before the film was made: %d of %d shots "
                    + "(%d windows of %d frames in a row, %d pairs of frames compared; %d shots too short or inside a dissolve to compare)%n",
                    checked, shots, windows, FRAMES, pairs, skipped));
            b.append(String.format(Locale.US, "• Morphing / boiling (two frames in a row differing by more than %.0f per cell on average): %d shots; "
                    + "camera shake (more than %.0f%% of the frame changing between two frames): %d shots → motion cut by 80%% and played again "
                    + "from the same first frame: %d calmed, %d still lively%n", BOIL, boiling, SHAKE * 100, shaking, calmed, stillLively));
            b.append(String.format(Locale.US, "• Shakes the story asks for (an earthquake, thunder, a blast): %d shots; whole-frame story effects "
                    + "(a flash, lightning, a blackout, a glitch, flickering light) left out of the comparison: %d moments%n", storyShakes, effects));
            b.append(String.format(Locale.US, "• Floating: %d of %d feet positions drawn above the ground line (every standing character is drawn on "
                    + "the floor line with its contact shadow)%n", floating, feetChecked));
            b.append("• Finger count: drawn characters have fixed drawn hands; picture characters keep the hands of their own picture (AI pictures "
                    + "are asked for five fingers or hidden hands, and every picture was shown in Human QC)\n");
            b.append(String.format(Locale.US, "• Largest change between two frames in a row: %.1f per cell%s (limit %.0f); largest share of the frame "
                    + "changed: %.0f%% (limit %.0f%%)%n", worstBoil, worstAt.isEmpty() ? "" : " at " + worstAt, BOIL, worstMoved * 100, SHAKE * 100));
            for (String n : notes) b.append("  - ").append(n).append('\n');
            return b.toString();
        }
    }

    /** Step 6 for the whole film: every shot played and, when lively, calmed and played again. */
    public static Result check(Film film, Art art, Surface s, Progress p) {
        Result r = new Result();
        r.shots = film.shots.size();
        Renderer ren = new Renderer(film, art);
        ren.feetLog = new ArrayList<float[]>();
        int gw = Math.min(GRID_W, s.width()), gh = Math.max(1, Math.round(gw * s.height() / (float) s.width()));
        float[] prev = new float[gw * gh], cur = new float[gw * gh];
        for (int i = 0; i < film.shots.size(); i++) {
            if (p != null) { if (p.cancelled()) break; p.at(i, film.shots.size()); }
            Film.Shot sh = film.shots.get(i);
            Verdict v = play(film, ren, s, sh, prev, cur, gw, gh, r);
            if (v.windows == 0) { r.skipped++; continue; }
            r.checked++;
            r.windows += v.windows;
            r.pairs += v.pairs;
            if (v.storyShake) r.storyShakes++;
            r.effects += v.effects;
            if (v.boil > r.worstBoil) { r.worstBoil = v.boil; r.worstAt = stamp(v.worstT); }
            r.worstMoved = Math.max(r.worstMoved, v.moved);
            if (!v.lively()) continue;
            if (v.boils()) r.boiling++;
            if (v.shakes()) r.shaking++;
            // the protocol's fix: the motion of the shot cut by 80%, played again from the same first frame
            film.calm.add(new float[]{sh.t, sh.t + sh.dur});
            Verdict again = play(film, ren, s, sh, prev, cur, gw, gh, null);
            if (again.lively()) {
                r.stillLively++;
                if (r.notes.size() < 12) r.notes.add(String.format(Locale.US, "shot %03d at %s still lively after the 80%% cut (%s): an effect of the story moves there",
                        i + 1, stamp(sh.t), again.why()));
            } else {
                r.calmed++;
                if (r.notes.size() < 12) r.notes.add(String.format(Locale.US, "shot %03d at %s calmed: %s → motion cut by 80%%", i + 1, stamp(sh.t), v.why()));
            }
        }
        return r;
    }

    /** What playing one shot showed. */
    static final class Verdict {
        int windows, pairs, effects;
        float boilSum, boil, moved, worstT;
        boolean storyShake;
        float mean() { return pairs == 0 ? 0 : boilSum / pairs; }
        boolean boils() { return pairs > 0 && mean() > BOIL; }
        boolean shakes() { return pairs > 0 && moved > SHAKE; }
        boolean lively() { return boils() || shakes(); }
        String why() {
            return boils() ? String.format(Locale.US, "boiling %.1f per cell", mean()) : String.format(Locale.US, "%.0f%% of the frame moved", moved * 100);
        }
    }

    /** Plays the windows of a shot frame by frame and compares every two frames in a row. */
    static Verdict play(Film film, Renderer ren, Surface s, Film.Shot sh, float[] prev, float[] cur, int gw, int gh, Result feet) {
        Verdict v = new Verdict();
        Film.Seg sg = film.segAt(sh.t + 0.01f);
        for (float t0 : windows(film, sh)) {
            v.windows++;
            boolean have = false;
            for (int i = 0; i < FRAMES; i++) {
                float t = t0 + i / (float) FPS;
                ren.render(s.gfx(), t);
                if (feet != null) feet(ren, feet, t);
                grid(s.pixels(), s.width(), s.height(), cur, gw, gh);
                if (have) {
                    int sp = special(film, sg, t);
                    if (sp == 2) v.effects++;
                    else if (sp == 1) v.storyShake = true;
                    else {
                        float[] d = compare(prev, cur);
                        v.pairs++;
                        v.boilSum += d[0];
                        if (d[0] > v.boil) { v.boil = d[0]; v.worstT = t; }
                        v.moved = Math.max(v.moved, d[1]);
                    }
                }
                float[] tmp = prev; prev = cur; cur = tmp;
                have = true;
            }
        }
        return v;
    }

    /** The floating check: the renderer's record of every standing character's feet in the frame just drawn. */
    static void feet(Renderer ren, Result r, float t) {
        if (ren.feetLog == null) return;
        for (float[] e : ren.feetLog) {
            r.feetChecked++;
            if (e[3] == 0 && e[1] - e[0] > FLOAT_TOLERANCE * e[2]) {
                r.floating++;
                if (r.notes.size() < 12) r.notes.add(String.format(Locale.US, "floating at %s: feet drawn %.0f units above the ground line", stamp(t), e[1] - e[0]));
            }
        }
    }

    /** The steady part of a scene: after its fade-in or dissolve, before its fade-out. {from, to} */
    static float[] steady(Film film, Film.Seg sg) {
        int si = film.segs.indexOf(sg);
        Film.Seg prev = si > 0 ? film.segs.get(si - 1) : null, next = si + 1 < film.segs.size() ? film.segs.get(si + 1) : null;
        boolean dIn = prev != null && prev.type == Film.S_SCENE && sg.type == Film.S_SCENE;
        boolean dOut = next != null && next.type == Film.S_SCENE && sg.type == Film.S_SCENE;
        return new float[]{sg.t0 + (dIn ? Renderer.DISSOLVE : Math.max(0, sg.fadeIn)) + 0.05f, sg.t1 - (dOut ? 0 : Math.max(0, sg.fadeOut)) - 0.05f};
    }

    /** The windows of a shot (the start of each; FRAMES frames in a row from there), clear of the part's fades and dissolves. */
    static List<Float> windows(Film film, Film.Shot sh) {
        List<Float> out = new ArrayList<Float>();
        float span = (FRAMES - 1) / (float) FPS;
        float a = sh.t + 0.1f, b = sh.t + sh.dur - 0.1f - span;
        Film.Seg sg = film.segAt(sh.t + 0.01f);
        if (sg != null) {
            float[] st = steady(film, sg);
            a = Math.max(a, st[0]);
            b = Math.min(b, st[1] - span);
        }
        if (b < a) return out;
        for (int i = 0; i < WINDOWS; i++) {
            float t = WINDOWS == 1 ? (a + b) / 2 : a + (b - a) * i / (WINDOWS - 1);
            if (out.isEmpty() || t - out.get(out.size() - 1) > span + 0.5f / FPS) out.add(t);
        }
        return out;
    }

    /**
     * 0 = a plain moment; 1 = the story shakes the camera here (an earthquake, thunder, a blast); 2 = a
     * whole-frame effect of the story (a flash, lightning, a blackout, a glitch, flickering light, fireworks).
     */
    static int special(Film film, Film.Seg sg, float t) {
        int out = 0;
        if (sg != null) for (Film.Fx f : sg.fx) {
            if (t < f.t0 - 0.05f || t > f.t1 + 0.6f) continue;
            switch (f.type) {
                case Film.FX_SHAKE: out = Math.max(out, 1); break;
                case Film.FX_FLASH: case Film.FX_LIGHTNING: case Film.FX_GLITCH: case Film.FX_FLICKER: case Film.FX_FIREWORKS: return 2;
                case Film.FX_LIGHTS_OFF: if (t < f.t0 + 0.8f || t > f.t1 - 0.3f) return 2; break;
                default:
            }
        }
        if (film.weather(Film.W_QUAKE, t) > 0.01f) out = Math.max(out, 1);
        return out;
    }

    /** The frame on the comparison grid: the grey of a small block in the middle of every cell. */
    static void grid(int[] px, int w, int h, float[] out, int gw, int gh) {
        float cw = w / (float) gw, ch = h / (float) gh;
        int bs = Math.max(1, Math.min(4, (int) Math.min(cw, ch)));
        for (int gy = 0; gy < gh; gy++) {
            int y0 = Math.max(0, Math.min(h - bs, (int) (gy * ch + (ch - bs) / 2)));
            for (int gx = 0; gx < gw; gx++) {
                int x0 = Math.max(0, Math.min(w - bs, (int) (gx * cw + (cw - bs) / 2)));
                int sum = 0;
                for (int y = y0; y < y0 + bs; y++) {
                    int row = y * w;
                    for (int x = x0; x < x0 + bs; x++) {
                        int c = px[row + x];
                        sum += (c >> 16 & 255) * 2 + (c >> 8 & 255) * 3 + (c & 255);
                    }
                }
                out[gy * gw + gx] = sum / (6f * bs * bs);
            }
        }
    }

    /** {the mean difference per cell, the share of cells that changed}. */
    static float[] compare(float[] a, float[] b) {
        float sum = 0;
        int changed = 0;
        for (int i = 0; i < a.length; i++) {
            float d = Math.abs(a[i] - b[i]);
            sum += d;
            if (d > CHANGED) changed++;
        }
        return new float[]{sum / Math.max(1, a.length), changed / (float) Math.max(1, a.length)};
    }

    static String stamp(float t) { return String.format(Locale.US, "%d:%04.1f", (int) (t / 60), t % 60); }

    /**
     * Step 8: the finished film, metered frame by frame as it is written. Feed every frame in order; the report
     * says what the whole film showed.
     */
    public static final class Meter {
        private final Film film;
        private final int w, h, gw, gh, fps;
        private float[] prev, cur;
        private int prevFrame = -9, prevShot = -2, cursor;
        private final float[] boilSum, boilMax, movedMax;
        private final int[] pairsOf;
        private final boolean[] shook;
        public int frames, pairs, effects;
        public float worstBoil, worstMoved;
        public String worstAt = "";

        public Meter(Film film, int w, int h, int fps) {
            this.film = film; this.w = w; this.h = h; this.fps = fps;
            gw = Math.min(GRID_W, w);
            gh = Math.max(1, Math.round(gw * h / (float) w));
            prev = new float[gw * gh];
            cur = new float[gw * gh];
            int n = film.shots.size();
            boilSum = new float[n]; boilMax = new float[n]; movedMax = new float[n]; pairsOf = new int[n]; shook = new boolean[n];
        }

        /** Frame f of the film, its pixels as written (after the look and the grade). */
        public void frame(int f, int[] px) {
            frames++;
            float t = f / (float) fps;
            int si = shotAt(t);
            float[] tmp = prev; prev = cur; cur = tmp;
            grid(px, w, h, cur, gw, gh);
            if (si >= 0 && si == prevShot && f == prevFrame + 1) {
                Film.Seg sg = film.segAt(t);
                float[] st = sg == null ? null : steady(film, sg);
                if (st != null && t - 1f / fps >= st[0] && t <= st[1]) {
                    int sp = special(film, sg, t);
                    if (sp == 2) effects++;
                    else if (sp == 1) shook[si] = true;
                    else {
                        float[] d = compare(prev, cur);
                        pairs++;
                        pairsOf[si]++;
                        boilSum[si] += d[0];
                        boilMax[si] = Math.max(boilMax[si], d[0]);
                        movedMax[si] = Math.max(movedMax[si], d[1]);
                        if (d[0] > worstBoil) { worstBoil = d[0]; worstAt = stamp(t); }
                        worstMoved = Math.max(worstMoved, d[1]);
                    }
                }
            }
            prevFrame = f;
            prevShot = si;
        }

        private int shotAt(float t) {
            List<Film.Shot> sh = film.shots;
            if (sh.isEmpty()) return -1;
            if (cursor >= sh.size() || sh.get(cursor).t > t) cursor = 0;
            while (cursor + 1 < sh.size() && sh.get(cursor + 1).t <= t) cursor++;
            Film.Shot s = sh.get(cursor);
            return s.t <= t && t < s.t + s.dur ? cursor : -1;
        }

        public int checkedShots() { int n = 0; for (int p : pairsOf) if (p > 0) n++; return n; }
        public int boilingShots() { int n = 0; for (int i = 0; i < pairsOf.length; i++) if (pairsOf[i] > 0 && boilSum[i] / pairsOf[i] > BOIL) n++; return n; }
        public int shakingShots() { int n = 0; for (int i = 0; i < pairsOf.length; i++) if (pairsOf[i] > 0 && movedMax[i] > SHAKE) n++; return n; }
        public int storyShakes() { int n = 0; for (boolean b : shook) if (b) n++; return n; }

        public String report() {
            return String.format(Locale.US, "• Step 8 — the finished film metered frame by frame as it was written (%d frames; %d pairs of frames in a row "
                    + "compared in %d shots): morphing / boiling %d shots, camera shake %d shots, shakes the story asks for %d, whole-frame story effects "
                    + "left out %d moments; the largest change between two frames %.1f per cell%s (limit %.0f), the largest share of the frame changed %.0f%% (limit %.0f%%)%n",
                    frames, pairs, checkedShots(), boilingShots(), shakingShots(), storyShakes(), effects, worstBoil,
                    worstAt.isEmpty() ? "" : " at " + worstAt, BOIL, worstMoved * 100, SHAKE * 100);
        }
    }
}
