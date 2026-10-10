package com.tarun.kahani.core;

/**
 * Nature in the film: what a background picture contains (sky, water, a waterfall, plants), and the weather and
 * physics drawn over it — rain with splashes, snow, wind-blown leaves and petals, dust, fog, fire, fireflies,
 * lightning, a stone's arc into a pond with its splash and rings of waves.
 *
 * Every particle follows simple physics (gravity, wind drift, bounce) computed directly from the time, so any
 * frame can be drawn on its own (several frames are drawn at once on the phone) and the motion stays smooth.
 */
public final class Nature {
    private Nature() {}

    // ================================================================== reading a background picture

    /** Where the sky, water, waterfall and plants are in a picture (coarse grid, values 0..1). */
    public static final class Scan {
        public static final int GW = 64, GH = 36;
        public final float[] water = new float[GW * GH], fall = new float[GW * GH], plants = new float[GW * GH];
        /**
         * v35: how far up its plant each point of foliage is, as the wind moves it (0.3 near the root .. 1 at the top
         * of a tree; on a field of grass the near grass, larger on the screen, more than the far).
         */
        public final float[] reach = new float[GW * GH];
        public float skyBottom;              // fraction of the picture height
        public float waterTop = -1, waterBottom = -1;   // fractions of the height where the water lies
        public boolean anyWater, anyFall, anyPlants;
        public float at(float[] m, float u, float v) {
            float x = u * GW - 0.5f, y = v * GH - 0.5f;
            int x0 = (int) Math.floor(x), y0 = (int) Math.floor(y);
            float fx = x - x0, fy = y - y0;
            return lerp(lerp(cell(m, x0, y0), cell(m, x0 + 1, y0), fx), lerp(cell(m, x0, y0 + 1), cell(m, x0 + 1, y0 + 1), fx), fy);
        }
        static float cell(float[] m, int x, int y) {
            if (x < 0) x = 0; if (y < 0) y = 0; if (x >= GW) x = GW - 1; if (y >= GH) y = GH - 1;
            return m[y * GW + x];
        }
    }

    static float lerp(float a, float b, float f) { return a + (b - a) * f; }

    /** Reads a background picture (ARGB pixels). */
    public static Scan scan(int[] px, int w, int h) {
        Scan s = new Scan();
        float[] hsv = new float[3];
        // sky: rows from the top while most of the row is sky-coloured
        int skyRow = 0;
        for (int y = 0; y < h; y += 2) {
            int skyN = 0, n = 0;
            for (int x = 0; x < w; x += 3) {
                hsv(px[y * w + x], hsv);
                n++;
                if ((hsv[0] >= 180 && hsv[0] <= 240 && hsv[2] > 0.5f && hsv[1] < 0.75f) || (hsv[1] < 0.18f && hsv[2] > 0.72f)
                        || (y < h * 0.4f && hsv[2] > 0.6f && (hsv[0] < 50 || hsv[0] > 330) && hsv[1] < 0.6f)) skyN++;
            }
            if (skyN < n * 0.35f) break;
            skyRow = y + 2;
        }
        s.skyBottom = skyRow / (float) h;
        int GW = Scan.GW, GH = Scan.GH, N = GW * GH;
        float[] cnt = new float[N], wat = new float[N], foam = new float[N], grn = new float[N], gx = new float[N], gy = new float[N], blue = new float[N], gtex = new float[N];
        float minWaterY = Math.max(s.skyBottom + 0.03f, 0.3f) * h;
        for (int y = 0; y + 2 < h; y += 2) {
            int by = Math.min(GH - 1, y * GH / h);
            for (int x = 0; x + 2 < w; x += 2) {
                int bx = Math.min(GW - 1, x * GW / w);
                int g = by * GW + bx;
                cnt[g]++;
                int c = px[y * w + x];
                float l = lum(c), dxl = Math.abs(lum(px[y * w + x + 2]) - l), dyl = Math.abs(lum(px[(y + 2) * w + x]) - l);
                hsv(c, hsv);
                float hh = hsv[0], ss = hsv[1], vv = hsv[2];
                if (hh >= 175 && hh <= 240 && ss > 0.2f) blue[g]++;
                // v35: the crowns of trees stand against the sky: foliage is read in the sky's rows too
                boolean leafy = hh >= 60 && hh <= 168 && ss > 0.18f && vv > 0.12f;
                if (y <= skyRow + h / 40) { if (leafy) { grn[g]++; gtex[g] += dxl + dyl; } continue; }
                if (hh >= 175 && hh <= 235 && ss > 0.12f && ss < 0.7f && vv > 0.25f && vv < 0.96f && y > minWaterY && dxl + dyl < 0.12f) wat[g]++;
                else if (ss < 0.2f && vv > 0.8f) { foam[g]++; gx[g] += dxl; gy[g] += dyl; }
                else if (leafy) { grn[g]++; gtex[g] += dxl + dyl; }
            }
        }
        float blueAll = 0, cntAll = 0;
        for (int i = 0; i < N; i++) {
            blueAll += blue[i]; cntAll += cnt[i];
            if (cnt[i] == 0) continue;
            s.water[i] = wat[i] / cnt[i] > 0.45f ? Math.min(1, wat[i] / cnt[i] * 1.4f) : 0;
            s.plants[i] = Math.min(1, grn[i] / cnt[i] * 1.5f);
            // v35: leaves and grass have texture; a flat green wall, door or sofa does not, and stays still
            float tex = grn[i] > 0 ? gtex[i] / grn[i] : 0;
            s.plants[i] *= Math.max(0, Math.min(1, (tex - 0.012f) / 0.025f));
            // falling water: bright white with vertical streaks (changes more across than down)
            boolean streaks = foam[i] / cnt[i] > 0.4f && gx[i] > gy[i] * 1.35f;
            float yb = (i / GW + 0.5f) / GH;
            s.fall[i] = streaks && yb > s.skyBottom + 0.02f ? Math.min(1, foam[i] / cnt[i] * 1.3f) : 0;
        }
        keepBig(s.water, 10);
        keepBig(s.fall, 4);
        reach(s);
        // a picture that is blue all over is blue light (night, magic), not water
        if (blueAll > cntAll * 0.45f) java.util.Arrays.fill(s.water, 0);
        blur(s.water); blur(s.plants); blur(s.fall);
        float sw = 0, sf = 0, sp = 0;
        for (int i = 0; i < N; i++) { sw += s.water[i]; sf += s.fall[i]; sp += s.plants[i]; }
        s.anyWater = sw > 6; s.anyFall = sf > 2.5f; s.anyPlants = sp > 12;
        for (int i = 0; i < N; i++) {
            if (s.water[i] < 0.5f) continue;
            float v = (i / GW + 0.5f) / GH;
            if (s.waterTop < 0 || v < s.waterTop) s.waterTop = v;
            if (v > s.waterBottom) s.waterBottom = v;
        }
        return s;
    }

    static float lum(int c) { return (((c >> 16) & 255) * 0.3f + ((c >> 8) & 255) * 0.59f + (c & 255) * 0.11f) / 255f; }

    /**
     * v35: how much the wind moves each point of foliage. Each column's runs of foliage are read from the bottom up: a
     * run standing on the bottom of the picture is ground cover (grass, a hedge) — its near part, larger on the screen,
     * moves more than its far part; a run above the ground (a tree's crown, a bush) moves more the higher up it is.
     */
    static void reach(Scan s) {
        int GW = Scan.GW, GH = Scan.GH;
        for (int x = 0; x < GW; x++) {
            int y = GH - 1;
            while (y >= 0) {
                if (s.plants[y * GW + x] < 0.25f) { y--; continue; }
                int bottom = y;
                while (y >= 0 && s.plants[y * GW + x] >= 0.25f) y--;
                int top = y + 1, len = bottom - top + 1;
                boolean ground = bottom >= GH - 2 - GH / 10;
                for (int k = top; k <= bottom; k++) {
                    float up = len <= 1 ? 1f : (bottom - k) / (float) (len - 1);       // 0 at the run's foot, 1 at its top
                    s.reach[k * GW + x] = ground ? 0.3f + 0.5f * (1 - up) : 0.2f + 0.8f * up;
                }
            }
        }
        blur(s.reach);
    }

    /** Removes patches of a mask smaller than min cells (a blue flower is not a pond). */
    static void keepBig(float[] m, int min) {
        int GW = Scan.GW, GH = Scan.GH;
        int[] lab = new int[m.length];
        int[] stack = new int[m.length];
        int next = 1;
        for (int i = 0; i < m.length; i++) {
            if (m[i] <= 0 || lab[i] != 0) continue;
            int sp = 0, size = 0;
            stack[sp++] = i;
            lab[i] = next;
            java.util.List<Integer> cells = new java.util.ArrayList<Integer>();
            while (sp > 0) {
                int c = stack[--sp];
                cells.add(c);
                size++;
                int x = c % GW, y = c / GW;
                int[] nb = {x > 0 ? c - 1 : -1, x < GW - 1 ? c + 1 : -1, y > 0 ? c - GW : -1, y < GH - 1 ? c + GW : -1};
                for (int q : nb) if (q >= 0 && m[q] > 0 && lab[q] == 0) { lab[q] = next; stack[sp++] = q; }
            }
            if (size < min) for (int c : cells) m[c] = 0;
            next++;
        }
    }

    static void blur(float[] m) {
        float[] o = new float[m.length];
        for (int y = 0; y < Scan.GH; y++) for (int x = 0; x < Scan.GW; x++) {
            float a = 0; int c = 0;
            for (int dy = -1; dy <= 1; dy++) for (int dx = -1; dx <= 1; dx++) {
                int xx = x + dx, yy = y + dy;
                if (xx < 0 || yy < 0 || xx >= Scan.GW || yy >= Scan.GH) continue;
                a += m[yy * Scan.GW + xx]; c++;
            }
            o[y * Scan.GW + x] = a / c;
        }
        System.arraycopy(o, 0, m, 0, m.length);
    }

    static void hsv(int c, float[] o) {
        float r = ((c >> 16) & 255) / 255f, g = ((c >> 8) & 255) / 255f, b = (c & 255) / 255f;
        float mx = Math.max(r, Math.max(g, b)), mn = Math.min(r, Math.min(g, b)), d = mx - mn;
        float hh = 0;
        if (d > 1e-5f) {
            if (mx == r) hh = 60 * (((g - b) / d) % 6);
            else if (mx == g) hh = 60 * ((b - r) / d + 2);
            else hh = 60 * ((r - g) / d + 4);
        }
        if (hh < 0) hh += 360;
        o[0] = hh; o[1] = mx <= 0 ? 0 : d / mx; o[2] = mx;
    }

    // ================================================================== the living background

    public static final int MW = 192, MH = 108;    // the smallest mesh of a living place (plants, water, falls)
    /** The most cells a place picture is bent through (pixel level: one cell per screen pixel, up to a 1080p frame). */
    public static final int MAX_COLS = 1920, MAX_ROWS = 1080, MAX_CELLS = 2073600;
    /** Screen pixels per mesh cell of a place picture (1 = pixel level). */
    public static float CELL_PX = 1f;

    /**
     * How finely to bend a place picture this frame: {cols, rows}. A picture with plants, water or a waterfall
     * gets about one cell per CELL_PX screen pixels (never more cells than it has pixels); a picture where
     * nothing moves is mapped exactly to the pixel by a plain grid (every point of it lands where it belongs).
     * screenW = the whole picture's width on screen in pixels.
     */
    public static int[] meshSize(Scan s, float screenW, int srcW, int srcH) {
        boolean alive = s != null && (s.anyPlants || s.anyWater || s.anyFall);
        if (!alive) return new int[]{16, Math.max(9, Math.round(16f * srcH / Math.max(1, srcW)))};
        int cols = Math.round(screenW / CELL_PX);
        cols = Math.max(MW, Math.min(Math.min(MAX_COLS, srcW), cols));
        int rows = Math.max(MH, Math.min(Math.min(MAX_ROWS, srcH), Math.round(cols * srcH / (float) Math.max(1, srcW))));
        while ((long) cols * rows > MAX_CELLS) { cols = cols * 9 / 10; rows = rows * 9 / 10; }
        return new int[]{cols, rows};
    }

    /**
     * v35: the gusts of the wind over a place (-1..1) at a point across it (u, 0..1) and a moment: three waves of
     * different length running across the place in the wind's direction (dir), so a gust is seen to travel — a wave
     * running over a field of grass, one tree after the next bending — and never repeats in an obvious beat.
     */
    public static float gustField(float u, float t, float dir) {
        double a = t * 0.23 * dir, b = t * 0.31 * dir, c = t * 0.47 * dir;
        return (float) (0.55 * Math.sin(2 * Math.PI * (u * 0.7 - a) + 0.4)
                + 0.30 * Math.sin(2 * Math.PI * (u * 1.6 - b) + 2.1)
                + 0.15 * Math.sin(2 * Math.PI * (u * 3.1 - c) + 4.7));
    }

    /**
     * v35: how far (in units of the plant's reach) the wind pushes foliage at a point and moment: + towards the right.
     * In real wind (|wind| above the breeze) plants lean with it and the travelling gusts push them further — always
     * with the wind, never against it; in still air (the breeze that always moves outdoors) they rock gently back and
     * forth; and every plant also rocks at its own slow pace (a tree's crown about every two seconds).
     */
    public static float windPush(float u, float v, float t, float wind, float breeze) {
        float w = Math.abs(wind), dir = wind >= 0 ? 1 : -1;
        float strength = breeze + w;
        float g = gustField(u, t, dir);
        float push = dir * w * (0.8f + 0.6f * (0.5f + 0.5f * g));
        push += 0.35f * strength * g * (1 - Math.min(1f, w));
        push += 0.18f * strength * (float) Math.sin(t * 2 * Math.PI * 0.47 + u * 7 + v * 3);
        return push;
    }

    /** v35: the quick trembling of leaves (-1..1, three to four times a second, clump by clump: a few dozen across the picture). */
    public static float flutter(float u, float v, float t) {
        return (float) (0.6 * Math.sin(t * 2 * Math.PI * 2.7 + u * 120 + v * 70) + 0.4 * Math.sin(t * 2 * Math.PI * 4.1 + u * 190 - v * 110));
    }

    /**
     * Mesh points for drawing a background picture with its plants swaying in the wind (a gentle breeze always
     * outdoors) and its water rippling along the flow (a waterfall streams down). The picture's crop window (x0..x1,
     * y0..y1 of the picture) fills the stage (0..W, 0..H).
     */
    public static void backdropMesh(Scan s, float x0, float y0, float x1, float y1, float W, float H, float t, float wind, float sea, float[] out) {
        backdropMesh(s, x0, y0, x1, y1, W, H, t, wind, sea, out, MW, MH, true);
    }

    public static void backdropMesh(Scan s, float x0, float y0, float x1, float y1, float W, float H, float t, float wind, float sea, float[] out, int MW, int MH) {
        backdropMesh(s, x0, y0, x1, y1, W, H, t, wind, sea, out, MW, MH, true);
    }

    /**
     * outdoors: the plants move in the wind (v35: the realistic wind — Nature.windPush, a lean with real wind,
     * travelling gusts, the plant's own rocking, the tops of trees moving more than the crown's foot, near grass more
     * than far, leaves trembling); indoors a plant stays still.
     */
    public static void backdropMesh(Scan s, float x0, float y0, float x1, float y1, float W, float H, float t, float wind, float sea, float[] out, int MW, int MH, boolean outdoors) {
        int k = 0;
        float w = Math.abs(wind);
        boolean plantsMove = s != null && s.anyPlants && outdoors;
        float breeze = 0.3f;
        // the reach of the sway at the top of a plant (stage units), and of the leaves' trembling
        float amp = 14f * W / 1280f, ampF = (0.6f + 1.4f * Math.min(1.5f, w)) * W / 1280f;
        for (int j = 0; j <= MH; j++) {
            float v = j / (float) MH;
            for (int i = 0; i <= MW; i++) {
                float u = i / (float) MW;
                float X = (u - x0) / (x1 - x0) * W, Y = (v - y0) / (y1 - y0) * H;
                float edge = Math.min(Math.min(i, MW - i), Math.min(j, MH - j)) >= 1 ? 1 : 0;
                float dx = 0, dy = 0;
                if (plantsMove) {
                    float p = s.at(s.plants, u, v);
                    if (p > 0.02f) {
                        float reach = s.at(s.reach, u, v);
                        float push = windPush(u, v, t, wind, breeze) * amp * reach * p;
                        float fl = flutter(u, v, t) * ampF * p * (0.5f + 0.5f * reach);
                        dx += push + fl;
                        // a bending stem lowers its tip a little; leaves tremble up and down too
                        dy += Math.abs(push) * 0.12f + flutter(v, u, t + 0.37f) * ampF * 0.6f * p;
                    }
                }
                if (s != null && s.anyWater) {
                    float wv = s.at(s.water, u, v);
                    if (wv > 0.02f) {
                        // waves travelling along the river
                        float big = 1 + 2.2f * sea + w;
                        dx += wv * 2.0f * big * (float) Math.sin(u * 70 - t * 2.4 + v * 20);
                        dy += wv * 1.4f * big * (float) Math.sin(u * 45 + v * 60 - t * 3.3) + wv * sea * 4 * (float) Math.sin(v * 25 - t * 1.6);
                    }
                }
                if (s != null && s.anyFall) {
                    float fv = s.at(s.fall, u, v);
                    if (fv > 0.02f) dy += fv * 3.2f * (float) Math.sin(v * 90 - t * 10);
                }
                out[k++] = X + dx * edge;
                out[k++] = Y + dy * edge;
            }
        }
    }

    /** Sparkles running along the water, and white streaks falling down a waterfall (stage coordinates). */
    public static void waterLife(Gfx g, Scan s, float x0, float y0, float x1, float y1, float W, float H, float t) {
        if (s.anyWater) {
            for (int i = 0; i < 70; i++) {
                float u = rnd(i, 1), v = s.skyBottom + (1 - s.skyBottom) * rnd(i, 2);
                if (s.at(s.water, u, v) < 0.55f) continue;
                float life = 1.6f + rnd(i, 3) * 1.4f;
                float age = (t + rnd(i, 4) * life) % life;
                float X = (u - x0) / (x1 - x0) * W + age * 22, Y = (v - y0) / (y1 - y0) * H;
                float a = (float) Math.sin(Math.PI * age / life) * 0.55f;
                g.color(alpha(0xFFFFFFFF, a));
                g.oval(X, Y, 7 + rnd(i, 5) * 9, 1.4f);
            }
        }
        if (s.anyFall) {
            for (int i = 0; i < 90; i++) {
                float u = rnd(i, 11), v0 = rnd(i, 12);
                if (s.at(s.fall, u, v0) < 0.4f) continue;
                float speed = 0.25f + rnd(i, 13) * 0.2f;
                float vv = (v0 + t * speed) % 1f;
                if (s.at(s.fall, u, vv) < 0.3f) continue;
                float X = (u - x0) / (x1 - x0) * W, Y = (vv - y0) / (y1 - y0) * H;
                g.color(alpha(0xFFFFFFFF, 0.45f));
                g.line(X, Y, X, Y + 16 + rnd(i, 14) * 18, 1.6f);
            }
        }
    }

    /** The water point nearest to stage point (sx, sy), or null when the picture shows no water. */
    public static float[] waterPoint(Scan s, float x0, float y0, float x1, float y1, float W, float H, float sx, float sy) {
        if (s == null || !s.anyWater) return null;
        float best = 1e18f;
        float[] r = null;
        for (int gy = 0; gy < Scan.GH; gy++) for (int gx = 0; gx < Scan.GW; gx++) {
            if (s.water[gy * Scan.GW + gx] < 0.6f) continue;
            float u = (gx + 0.5f) / Scan.GW, v = (gy + 0.5f) / Scan.GH;
            float X = (u - x0) / (x1 - x0) * W, Y = (v - y0) / (y1 - y0) * H;
            if (X < 40 || X > W - 40 || Y < 0 || Y > H) continue;
            float d = (X - sx) * (X - sx) + (Y - sy) * (Y - sy) * 0.5f;
            if (d < best) { best = d; r = new float[]{X, Y}; }
        }
        return r;
    }

    // ================================================================== weather (screen space)

    /** Deterministic "random" number 0..1 for particle i, property k. */
    public static float rnd(int i, int k) {
        double x = Math.sin(i * 12.9898 + k * 78.233 + 0.12345) * 43758.5453;
        return (float) (x - Math.floor(x));
    }

    static int alpha(int c, float a) {
        int A = (int) (Math.max(0, Math.min(1, a)) * ((c >>> 24) & 255));
        return (A << 24) | (c & 0xFFFFFF);
    }

    /** Rain streaks falling with the wind; far drops are thinner and fainter. */
    public static void rain(Gfx g, float w, float h, float t, float amount, float wind) {
        int n = (int) (900 * Math.min(1.6f, amount));     // dense, fine rain
        float slant = wind * 0.35f;
        float sc = h / 720f;
        // a cool, grey light and veils of rain in the distance
        g.color(alpha(0xFF3A4860, 0.22f * Math.min(1, amount)));
        g.rect(0, 0, w, h);
        for (int k = 0; k < 3; k++) {
            float x = ((t * (90 + k * 40) * (wind >= 0 ? 1 : -1) + k * w / 3) % (w * 1.4f) + w * 1.4f) % (w * 1.4f) - w * 0.2f;
            g.radial(x, h * 0.45f, w * 0.35f, alpha(0xFFC8D2DC, 0.12f * Math.min(1, amount)), 0x00C8D2DC);
            g.rect(0, 0, w, h);
        }
        for (int i = 0; i < n; i++) {
            float depth = rnd(i, 1);                       // 0 far .. 1 near
            float speed = h * (1.5f + depth * 1.2f);
            float len = (20 + depth * 42) * sc;
            float period = (h + len) / speed;
            float age = (t + rnd(i, 2) * period) % period;
            float y = -len + age * speed;
            float x = rnd(i, 3) * (w + h * Math.abs(slant)) - (slant > 0 ? h * slant : 0) + y * slant;
            g.color(alpha(0xFFE4ECF4, 0.28f + depth * 0.37f));
            g.line(x, y, x - len * slant, y - len, (0.7f + depth * 1.4f) * sc);
        }
    }

    /** Drops hitting the ground: little crowns and rings (stage coordinates, along the floor). */
    public static void rainSplashes(Gfx g, float ground, float t, float amount) {
        int n = (int) (130 * Math.min(1.6f, amount));
        for (int i = 0; i < n; i++) {
            float life = 0.32f;
            float slot = (t + rnd(i, 7) * life) / life;
            int gen = (int) Math.floor(slot);
            float age = (slot - gen) * life;
            int id = i * 131 + gen;
            float x = rnd(id, 8) * 1280, y = ground - 6 + rnd(id, 9) * 70;
            float r = 3 + age * 40;
            g.color(alpha(0xFFE8F0F8, 0.5f * (1 - age / life)));
            g.strokeOval(x, y, r, r * 0.3f, 1.2f);
            // two droplets thrown up and falling back (gravity)
            float vy = -90, gr = 900;
            float dy = vy * age + 0.5f * gr * age * age;
            if (dy < 0) {
                g.oval(x - 4 - age * 30, y + dy, 1.3f, 1.3f);
                g.oval(x + 4 + age * 30, y + dy, 1.3f, 1.3f);
            }
        }
    }

    /** Snow flakes drifting down and swaying. */
    public static void snow(Gfx g, float w, float h, float t, float amount, float wind) {
        int n = (int) (380 * Math.min(1.5f, amount));
        for (int i = 0; i < n; i++) {
            float depth = rnd(i, 1);
            float speed = 35 + depth * 70;
            float period = (h + 20) / speed;
            float age = (t + rnd(i, 2) * period) % period;
            float y = -10 + age * speed;
            float x = (rnd(i, 3) * (w + 200) - 100 + wind * 60 * age + (float) Math.sin(t * (0.8 + rnd(i, 4)) + i) * 14 * (1 + depth)) % (w + 200);
            if (x < -100) x += w + 200;
            g.color(alpha(0xFFFFFFFF, 0.55f + depth * 0.4f));
            g.oval(x, y, (1.6f + depth * 3.2f) * h / 720f, (1.6f + depth * 3.2f) * h / 720f);
        }
        g.color(alpha(0xFFE8F0FF, 0.1f * amount));
        g.rect(0, 0, w, h);
    }

    /** Leaves (or petals) blown by the wind: they tumble, fall slowly and travel with the gusts. */
    public static void leaves(Gfx g, float w, float h, float t, float amount, float wind, boolean petals) {
        int n = (int) ((petals ? 120 : 75) * Math.min(1.5f, amount));
        int[] cols = petals ? new int[]{0xFFF48FB1, 0xFFE91E63, 0xFFFFD54F, 0xFFFFFFFF, 0xFFFF7043}
                : new int[]{0xFF7CB342, 0xFF558B2F, 0xFFC0A030, 0xFFA0522D, 0xFF9CCC65};
        float drift = petals ? 30 + wind * 140 : 60 + wind * 380;
        for (int i = 0; i < n; i++) {
            float fall = (petals ? 45 : 55) + rnd(i, 1) * 50;
            float period = (h + 40) / fall;
            float age = (t + rnd(i, 2) * period) % period;
            float y = -20 + age * fall + (float) Math.sin(age * 3 + i) * 10;
            float x = rnd(i, 3) * (w + 400) - 200 + drift * age;
            x = ((x + 200) % (w + 400) + (w + 400)) % (w + 400) - 200;
            float s = ((petals ? 6 : 9) + rnd(i, 4) * 7) * h / 720f;
            g.save();
            g.translate(x, y);
            g.rotate((t * (90 + rnd(i, 5) * 200) + i * 37) % 360);
            g.scale(1, 0.45f + 0.55f * Math.abs((float) Math.sin(t * 3 + i)));   // tumbling
            g.color(cols[i % cols.length]);
            g.oval(0, 0, s, s * 0.5f);
            g.restore();
        }
    }

    /** A dust storm: sandy haze and grit racing along with the wind. */
    public static void dust(Gfx g, float w, float h, float t, float amount, float wind) {
        g.color(alpha(0xFFC8A060, 0.28f * Math.min(1, amount)));
        g.rect(0, 0, w, h);
        int n = (int) (160 * amount);
        float dir = wind >= 0 ? 1 : -1;
        for (int i = 0; i < n; i++) {
            float speed = 500 + rnd(i, 1) * 600;
            float period = (w + 100) / speed;
            float age = (t + rnd(i, 2) * period) % period;
            float x = dir > 0 ? -50 + age * speed : w + 50 - age * speed;
            float y = rnd(i, 3) * h + (float) Math.sin(t * 2 + i) * 8;
            g.color(alpha(0xFFB08850, 0.35f));
            g.line(x, y, x - dir * (10 + rnd(i, 4) * 30), y, 1.2f);
        }
    }

    /** Fog: soft bands of mist drifting slowly. */
    public static void fog(Gfx g, float w, float h, float t, float amount) {
        for (int i = 0; i < 4; i++) {
            float y = h * (0.35f + i * 0.17f);
            float x = ((t * (12 + i * 7) + i * 300) % (w + 800)) - 400;
            g.radial(x, y, w * 0.55f, alpha(0xFFE8ECF0, 0.32f * amount), 0x00E8ECF0);
            g.rect(0, 0, w, h);
            g.radial(x + w * 0.6f, y + 30, w * 0.5f, alpha(0xFFE8ECF0, 0.26f * amount), 0x00E8ECF0);
            g.rect(0, 0, w, h);
        }
        g.color(alpha(0xFFDCE2E8, 0.22f * amount));
        g.rect(0, 0, w, h);
    }

    /** A flash of lightning: the bolt in the sky and the whole picture lit for a moment. age in seconds. */
    public static void lightningBolt(Gfx g, float x, float top, float bottom, float age, int seed) {
        if (age < 0 || age > 0.45f) return;
        float a = age < 0.08f ? 1 : age < 0.14f ? 0.25f : age < 0.22f ? 0.9f : Math.max(0, 1 - (age - 0.22f) / 0.23f);
        float px = x, py = top;
        g.begin();
        g.moveTo(px, py);
        int steps = 9;
        float[] xs = new float[steps + 1], ys = new float[steps + 1];
        xs[0] = px; ys[0] = py;
        for (int i = 1; i <= steps; i++) {
            px += (rnd(seed * 31 + i, 1) - 0.5f) * 70;
            py = top + (bottom - top) * i / steps;
            xs[i] = px; ys[i] = py;
            g.lineTo(px, py);
        }
        g.color(alpha(0xFFB0C8FF, 0.35f * a));
        g.strokePath(14);
        g.begin();
        g.moveTo(xs[0], ys[0]);
        for (int i = 1; i <= steps; i++) g.lineTo(xs[i], ys[i]);
        g.color(alpha(0xFFFFFFFF, a));
        g.strokePath(3.2f);
        // a branch
        g.begin();
        g.moveTo(xs[3], ys[3]);
        g.lineTo(xs[3] + 40 + rnd(seed, 9) * 40, ys[3] + (bottom - top) * 0.18f);
        g.lineTo(xs[3] + 70 + rnd(seed, 10) * 40, ys[3] + (bottom - top) * 0.3f);
        g.color(alpha(0xFFFFFFFF, 0.7f * a));
        g.strokePath(1.8f);
    }

    /** The bright flash over everything. */
    public static void flash(Gfx g, float w, float h, float age) {
        if (age < 0 || age > 0.5f) return;
        float a = (float) (0.7 * Math.exp(-age * 12) + 0.45 * Math.exp(-(age - 0.2) * (age - 0.2) * 300));
        g.color(alpha(0xFFF0F4FF, Math.min(0.85f, a)));
        g.rect(0, 0, w, h);
    }

    /** A rainbow arc in the sky. */
    public static void rainbow(Gfx g, float cx, float cy, float r, float amount) {
        int[] c = {0xFFFF4040, 0xFFFF9A30, 0xFFFFE840, 0xFF50D050, 0xFF40A0FF, 0xFF5050E0, 0xFF9050D0};
        for (int i = 0; i < c.length; i++) {
            g.begin();
            float rr = r - i * 9;
            g.moveTo(cx - rr, cy);
            g.cubicTo(cx - rr, cy - rr * 1.33f, cx + rr, cy - rr * 1.33f, cx + rr, cy);
            g.color(alpha(c[i], 0.32f * amount));
            g.strokePath(9);
        }
    }

    /** Twinkling stars over the sky. */
    public static void stars(Gfx g, float w, float skyBottom, float t, float amount) {
        for (int i = 0; i < 90; i++) {
            float x = rnd(i, 1) * w, y = rnd(i, 2) * skyBottom;
            float tw = 0.5f + 0.5f * (float) Math.sin(t * (1 + rnd(i, 3) * 3) + i);
            g.color(alpha(0xFFFFFFF0, (0.25f + 0.6f * tw) * amount));
            float s = 0.8f + rnd(i, 4) * 1.6f;
            g.oval(x, y, s, s);
        }
    }

    // ================================================================== on the stage (near the characters)

    /** Fireflies drifting and blinking near the ground. */
    public static void fireflies(Gfx g, float ground, float t, float amount) {
        int n = (int) (42 * amount);
        for (int i = 0; i < n; i++) {
            float x = 640 + 600 * (float) Math.sin(t * (0.11 + rnd(i, 1) * 0.12) + i * 2.1);
            float y = ground - 60 - 260 * rnd(i, 2) + 40 * (float) Math.sin(t * (0.3 + rnd(i, 3) * 0.4) + i);
            float blink = Math.max(0, (float) Math.sin(t * (1.5 + rnd(i, 4) * 2) + i * 1.3));
            g.radial(x, y, 14, alpha(0xFFE8FF70, 0.55f * blink), 0x00E8FF70);
            g.rect(x - 14, y - 14, 28, 28);
            g.color(alpha(0xFFFFFFC0, 0.9f * blink));
            g.oval(x, y, 2, 2);
        }
    }

    /** A campfire: logs, flickering flames, sparks rising and smoke. */
    public static void fire(Gfx g, float x, float ground, float t, float size) { fire(g, x, ground, t, size, 0); }

    /** v36: wind bends the flames, the sparks and the smoke away from where it blows from. */
    public static void fire(Gfx g, float x, float ground, float t, float size, float wind) {
        float s = size, lean = Math.max(-0.7f, Math.min(0.7f, wind * 0.6f));
        g.color(0xFF4E342E);
        g.save(); g.translate(x, ground - 6 * s); g.rotate(14); g.roundRect(-38 * s, -6 * s, 76 * s, 12 * s, 5 * s); g.restore();
        g.save(); g.translate(x, ground - 6 * s); g.rotate(-14); g.roundRect(-38 * s, -6 * s, 76 * s, 12 * s, 5 * s); g.restore();
        // glow on the ground
        g.radial(x, ground - 20 * s, 160 * s, 0x60FF9A30, 0x00FF9A30);
        g.rect(x - 160 * s, ground - 180 * s, 320 * s, 340 * s);
        // v36: flames that look like flames — tongues that rise along a travelling wave, narrow to a tip, lick off
        // the top and fade; deep red at the edge, orange, yellow, a white-hot core at the base
        for (int k = -3; k <= 3; k++) {
            float side = Math.abs(k) / 3f;
            float hgt = (95 - 38 * side) * s * (0.85f + 0.3f * rnd(k + 9, 5));
            tongue(g, x + k * 11 * s, ground - 6 * s, (15 - 4 * side) * s, hgt, t, k * 7 + 3, lean, 1f);
        }
        // the hot core low in the middle
        tongue(g, x, ground - 6 * s, 20 * s, 55 * s, t * 1.1f, 101, lean, 0.6f);
        // sparks: thrown up, slowed by the air, fading
        for (int i = 0; i < 14; i++) {
            float life = 1.2f + rnd(i, 1);
            float age = (t + rnd(i, 2) * life) % life;
            float sx = x + (rnd(i, 3) - 0.5f) * 40 * s + (float) Math.sin(age * 4 + i) * 10 * s + lean * age * age * 60 * s;
            float sy = ground - 40 * s - age * (90 + rnd(i, 4) * 60) * s;
            g.color(alpha(0xFFFFD54F, 1 - age / life));
            g.oval(sx, sy, 2.2f * s, 2.2f * s);
        }
        // smoke
        for (int i = 0; i < 6; i++) {
            float life = 4f;
            float age = (t + i * life / 6) % life;
            float sy = ground - 110 * s - age * 45 * s, sx = x + (float) Math.sin(age + i) * 20 * s + age * (12 + 50 * lean) * s;
            g.color(alpha(0xFF707070, 0.18f * (1 - age / life)));
            g.oval(sx, sy, (18 + age * 16) * s, (14 + age * 12) * s);
        }
    }

    /** The warm, flickering light of a fire over the picture. */
    public static void fireLight(Gfx g, float w, float h, float t, float amount) {
        float f = 0.8f + 0.2f * (float) (Math.sin(t * 13) * 0.5 + Math.sin(t * 7.3) * 0.5);
        g.color(alpha(0xFFFF8A30, 0.12f * amount * f));
        g.rect(0, 0, w, h);
    }

    /**
     * A thrown stone: a real arc under gravity from the hand to where it lands. Returns true while flying.
     * age = seconds since it left the hand; T = flight time.
     */
    public static boolean stone(Gfx g, float x0, float y0, float x1, float y1, float age, float T) {
        if (age < 0 || age > T) return false;
        float gr = 1500;
        float vy = (y1 - y0 - 0.5f * gr * T * T) / T;
        float x = x0 + (x1 - x0) * age / T, y = y0 + vy * age + 0.5f * gr * age * age;
        g.color(0xFF6D6D6D);
        g.oval(x, y, 6, 5);
        g.color(0x55000000);
        g.oval(x + 1, y + 2, 5, 3);
        return true;
    }

    /**
     * Something falls into water at (x, y): a crown of drops thrown up and falling back under gravity, then
     * rings of waves spreading out and fading. size 1 = a stone, 3 = a person jumping in.
     */
    public static void splashRipples(Gfx g, float x, float y, float age, float size) {
        if (age < 0 || age > 4f) return;
        // drops
        if (age < 0.9f) {
            for (int i = 0; i < 26 + (int) (size * 10); i++) {
                float ang = (float) (Math.PI * (0.15 + 0.7 * rnd(i, 1)));
                float sp = (180 + rnd(i, 2) * 220) * (float) Math.sqrt(size);
                float vx = (float) Math.cos(ang) * sp * (rnd(i, 3) > 0.5f ? 1 : -1), vy = -(float) Math.sin(ang) * sp;
                // gravity with a little air drag: x slows, the drop falls faster than it rose
                float drag = (float) Math.exp(-1.2f * age);
                float dx = vx * age * drag, dy = vy * age + 0.5f * 1400 * age * age;
                if (dy > 4) continue;
                float big = rnd(i, 4) < 0.3f ? 1.6f : 1f;
                g.color(alpha(0xFFE3F2FD, 0.85f * (1 - age / 0.9f)));
                g.oval(x + dx, y + dy, (1.4f + size) * big, (2.0f + size) * big);
            }
            // the column of water
            float col = (float) Math.max(0, Math.sin(Math.PI * Math.min(1, age / 0.5f))) * 26 * size;
            g.color(alpha(0xFFE3F2FD, 0.6f));
            g.oval(x, y - col * 0.5f, 4 * size, col * 0.5f + 1);
        }
        // waves: three rings, each spreading at the same speed and dying away
        for (int k = 0; k < 6; k++) {
            float a = age - k * 0.16f;
            if (a <= 0) continue;
            float r = (10 + a * 70) * (0.7f + 0.3f * size);
            float alpha = (float) Math.exp(-a / 1.1f) * 0.7f;
            g.color(alpha(0xFFFFFFFF, alpha));
            g.strokeOval(x, y, r, r * 0.28f, 2.2f);
            g.color(alpha(0xFF1A3A50, alpha * 0.5f));
            g.strokeOval(x, y + 2, r * 0.96f, r * 0.27f, 1.4f);
        }
    }

    /** Drops of water dripping off a wet character. */
    public static void drips(Gfx g, float x, float top, float ground, float w, float t, float wet, int seed) {
        int n = (int) (6 * wet);
        for (int i = 0; i < n; i++) {
            int id = seed * 17 + i;
            float life = 0.6f + rnd(id, 1) * 0.6f;
            float age = (t + rnd(id, 2) * 3) % 3f;
            if (age > life) continue;
            float sx = x + (rnd(id, 3) - 0.5f) * w * 0.8f, sy = top + (ground - top) * (0.35f + rnd(id, 4) * 0.4f);
            float y = sy + 0.5f * 1200 * age * age;
            if (y > ground) continue;
            g.color(alpha(0xFFBBDEFB, 0.8f));
            g.oval(sx, y, 1.6f, 2.4f);
        }
    }
    // ================================================================== sea, boats, flames, sky life, objects

    /**
     * Waves of the sea rolling onto the shore: crests travel towards the viewer across the water area and break
     * into white foam that spreads and fades (stage coordinates of the water band top..bottom).
     */
    public static void shoreWaves(Gfx g, Scan s, float x0, float y0, float x1, float y1, float W, float H,
                                  float top, float bottom, float t, float strength) {
        int waves = 4;
        for (int k = 0; k < waves; k++) {
            float period = 6.5f - Math.min(2.5f, strength * 1.5f);
            float p = ((t / period + k / (float) waves) % 1f);
            float y = top + (bottom - top) * p * p;            // waves speed up as they reach the shore
            float a = (float) Math.sin(Math.PI * p) * (0.35f + 0.3f * strength);
            g.begin();
            boolean started = false;
            for (int i = 0; i <= 64; i++) {
                float X = W * i / 64f;
                float u = x0 + (x1 - x0) * X / W, v = y0 + (y1 - y0) * y / H;
                boolean water = s == null || s.at(s.water, u, v) > 0.35f;
                float yy = y + (float) Math.sin(X * 0.02f + t * 1.3f + k) * 6 * (0.5f + p);
                if (!water) { started = false; continue; }
                if (!started) { g.moveTo(X, yy); started = true; } else g.lineTo(X, yy);
            }
            g.color(alpha(0xFFFFFFFF, a));
            g.strokePath(2.5f + 5 * p);
            // foam left behind as the wave breaks
            if (p > 0.75f) {
                for (int i = 0; i < 40; i++) {
                    float X = W * rnd(i + k * 50, 1);
                    float u = x0 + (x1 - x0) * X / W, v = y0 + (y1 - y0) * y / H;
                    if (s != null && s.at(s.water, u, v) <= 0.35f) continue;
                    g.color(alpha(0xFFF4FAFF, a * 0.8f));
                    g.oval(X, y + rnd(i, 2) * 14, 6 + rnd(i, 3) * 14, 2 + rnd(i, 4) * 2);
                }
            }
        }
    }

    /** A painted sea for painted sets: deep water with rolling waves between the horizon and the shore. */
    public static void paintedSea(Gfx g, float W, float horizon, float shore, float t, float strength) {
        g.linear(0, horizon, 0, shore, 0xFF1E5F8C, 0xFF3FA6C9);
        g.rect(0, horizon, W, shore - horizon);
        for (int r = 0; r < 7; r++) {
            float y = horizon + (shore - horizon) * (r + 1) / 8f;
            g.begin();
            for (int i = 0; i <= 40; i++) {
                float x = W * i / 40f;
                float yy = y + (float) Math.sin(x * 0.015f + t * (0.8 + r * 0.15) + r) * (3 + r * 1.5f) * (0.6f + strength);
                if (i == 0) g.moveTo(x, yy); else g.lineTo(x, yy);
            }
            g.color(alpha(0xFFE8F6FF, 0.25f + r * 0.05f));
            g.strokePath(1.5f + r * 0.4f);
        }
        g.color(0xFFE8D4A8);
        g.rect(0, shore, W, 6);
    }

    /**
     * A wooden boat floating: it rides up and down on the waves and rocks from side to side; in strong wind or
     * a storm it pitches harder and shudders.
     */
    public static void boat(Gfx g, float x, float waterY, float t, float size, float rough) {
        float bob = (float) Math.sin(t * 1.4) * 5 * size * (1 + 2 * rough);
        float roll = (float) (Math.sin(t * 1.1 + 0.7) * (3 + 10 * rough) + (rough > 0.5f ? Math.sin(t * 23) * 1.5 * rough : 0));
        float drift = (float) Math.sin(t * 0.2) * 20 * size;
        g.save();
        g.translate(x + drift, waterY + bob);
        g.rotate(roll);
        float L = 170 * size, D = 36 * size;
        g.begin();
        g.moveTo(-L * 0.55f, -D * 0.5f);
        g.quadTo(-L * 0.45f, D * 0.7f, 0, D * 0.75f);
        g.quadTo(L * 0.45f, D * 0.7f, L * 0.58f, -D * 0.6f);
        g.close();
        g.color(0xFF6D4325);
        g.fillPath();
        g.color(0xFF8B5A33);
        g.line(-L * 0.52f, -D * 0.35f, L * 0.55f, -D * 0.42f, 4 * size);
        g.color(0xFF4E2E18);
        for (int i = -2; i <= 2; i++) g.line(i * L * 0.18f, -D * 0.35f, i * L * 0.16f, D * 0.6f, 1.5f * size);
        // mast and sail (the sail fills with the wind)
        g.color(0xFF5D3A1F);
        g.line(0, -D * 0.4f, 0, -D * 3.2f, 4 * size);
        float fill = 0.3f + 0.7f * Math.min(1, rough + 0.3f);
        g.begin();
        g.moveTo(2, -D * 3.1f);
        g.quadTo(L * 0.25f * fill + 10, -D * 2.0f, 2, -D * 0.6f);
        g.close();
        g.color(0xFFF2E8D5);
        g.fillPath();
        g.restore();
        g.color(alpha(0xFFFFFFFF, 0.5f));
        g.strokeOval(x + drift, waterY + bob + D * 0.5f, L * 0.62f, 6 * size, 2);
    }

    /**
     * A small flame (candle, oil lamp, torch): it flickers, leans away from the wind and lights the air around
     * it. kind 0 candle, 1 diya, 2 torch.
     */
    public static void flame(Gfx g, float x, float y, float t, float size, float wind, int kind, int seed) {
        float fl = (float) (Math.sin(t * (11 + seed % 5) + seed) * 0.12 + Math.sin(t * 23 + seed * 3) * 0.06);
        float lean = Math.max(-0.6f, Math.min(0.6f, wind * 0.5f)) + (float) Math.sin(t * 2.7 + seed) * 0.05f;
        float h = (kind == 2 ? 46 : 22) * size * (1 + fl);
        float w = (kind == 2 ? 16 : 7) * size;
        g.radial(x, y - h * 0.5f, h * (kind == 2 ? 4f : 3.2f), alpha(0xFFFFB347, 0.35f + fl), 0x00FFB347);
        g.rect(x - h * 4, y - h * 4.5f, h * 8, h * 8);
        if (kind == 0) { g.color(0xFFF5EEDC); g.roundRect(x - 5 * size, y, 10 * size, 34 * size, 2 * size); }
        else if (kind == 1) { g.color(0xFFB5652B); g.oval(x, y + 5 * size, 13 * size, 5 * size); g.color(0xFF8C4A1E); g.oval(x, y + 7 * size, 11 * size, 3 * size); }
        else { g.color(0xFF5D3A1F); g.line(x, y, x - lean * 4, y + 70 * size, 6 * size); }
        // v36: the flame itself, as a real one (a candle's is one calm tongue, a torch's three restless ones)
        if (kind == 2) {
            for (int k = -1; k <= 1; k++) tongue(g, x + k * w * 0.6f, y, w * (k == 0 ? 0.9f : 0.65f), h * (k == 0 ? 1f : 0.75f), t, seed * 5 + k + 1, lean, 1f);
        } else tongue(g, x, y, w, h, t * (kind == 0 ? 0.7f : 0.85f), seed * 5, lean, 1f);
        // the blue at the root of a flame
        g.color(0x993070FF);
        g.oval(x, y - 1, w * 0.4f, w * 0.35f);
    }

    /**
     * v36: one tongue of flame. Its centre line rises along a travelling wave (the turbulence of hot air), so the
     * sway grows with height and runs upwards; it is widest a fifth of the way up and narrows to a tip that flickers
     * in height; now and then a small lick breaks off the top and rises, shrinking and fading; four colour layers
     * from the deep red edge through orange and yellow to the white-hot core. lean: the wind's bend (+ to the right);
     * core below 1 draws only the inner layers (a hot centre).
     */
    static void tongue(Gfx g, float bx, float base, float w, float h, float t, int seed, float lean, float core) {
        float ph = rnd(seed, 1) * 6.283f, sp = 2.2f + rnd(seed, 2) * 1.2f;
        float flick = 1 + 0.16f * (float) Math.sin(t * (10 + rnd(seed, 3) * 6) + ph) + 0.08f * (float) Math.sin(t * 23 + ph * 2);
        float H = h * flick;
        final int N = 10;
        int[] cols = {0xD9C62A0A, 0xE6FF6A13, 0xF0FFB02E, 0xFFFFF1B8};
        float[] widths = {1f, 0.78f, 0.55f, 0.3f}, heights = {1f, 0.86f, 0.66f, 0.42f};
        for (int layer = core < 1 ? 1 : 0; layer < 4; layer++) {
            float lw = w * widths[layer] * core, lh = H * heights[layer];
            g.begin();
            for (int pass = 0; pass < 2; pass++) {
                for (int ii = 0; ii <= N; ii++) {
                    int i = pass == 0 ? ii : N - ii;
                    float f = i / (float) N;
                    float cx = bx + flameWave(f, t, sp, ph) * w * 0.9f + lean * lh * f * f;
                    float half = lw * flameProfile(f);
                    float px = cx + (pass == 0 ? -half : half), py = base - lh * f;
                    if (pass == 0 && ii == 0) g.moveTo(px, py); else g.lineTo(px, py);
                }
            }
            g.close();
            g.color(cols[layer]);
            g.fillPath();
        }
        // a lick breaking off the top and rising
        float life = 0.45f + rnd(seed, 4) * 0.3f, age = (t + rnd(seed, 5) * life) % life, u = age / life;
        float ly = base - H * (0.95f + 0.6f * u), lx = bx + flameWave(1, t, sp, ph) * w * 0.9f + lean * H * (1 + u * 0.5f);
        float lr = w * 0.35f * (1 - u);
        if (lr > 0.5f) {
            g.begin(); g.moveTo(lx, ly - lr * 2.2f); g.quadTo(lx + lr, ly - lr * 0.2f, lx, ly + lr * 0.6f); g.quadTo(lx - lr, ly - lr * 0.2f, lx, ly - lr * 2.2f); g.close();
            g.color(alpha(0xFFFF9A2E, 0.85f * (1 - u))); g.fillPath();
        }
    }

    /** v36: the sideways sway of a flame at height f (0 base .. 1 tip): it grows with height and travels upwards. */
    static float flameWave(float f, float t, float sp, float ph) {
        return f * f * (0.55f * (float) Math.sin(6.283f * (f * 1.3f - t * sp * 0.5f) + ph) + 0.25f * (float) Math.sin(6.283f * (f * 2.7f - t * sp) + ph * 1.7f));
    }

    /** v36: a flame's half-width at height f: round at the base, widest a fifth of the way up, a sharp tip. */
    static float flameProfile(float f) {
        return (float) (Math.sqrt(Math.min(1, f * 5)) * Math.pow(1 - f, 0.9));
    }

    /** Soft clouds drifting across the sky (dark and low in a storm). */
    public static void clouds(Gfx g, float W, float skyBottom, float t, float amount, boolean dark) {
        for (int i = 0; i < 7; i++) {
            float speed = 6 + rnd(i, 1) * 10;
            float x = ((rnd(i, 2) * (W + 600) + t * speed) % (W + 600)) - 300;
            float y = skyBottom * (0.15f + rnd(i, 3) * 0.6f);
            float r = 70 + rnd(i, 4) * 90;
            int c = dark ? 0xFF4A5060 : 0xFFF4F6FA;
            for (int k = 0; k < 4; k++) {
                g.color(alpha(c, (dark ? 0.55f : 0.35f) * amount));
                g.oval(x + (k - 1.5f) * r * 0.55f, y + (k % 2) * r * 0.12f, r * (0.55f + 0.2f * (k % 3)), r * 0.38f);
            }
        }
    }

    /** A few birds flying across the sky, flapping. */
    public static void birds(Gfx g, float W, float skyBottom, float t, float amount) {
        int n = (int) (7 * amount);
        for (int i = 0; i < n; i++) {
            float speed = 60 + rnd(i, 1) * 50;
            float period = (W + 300) / speed;
            float age = (t + rnd(i, 2) * period) % period;
            float x = -150 + age * speed, y = skyBottom * (0.2f + rnd(i, 3) * 0.6f) + (float) Math.sin(age * 1.3 + i) * 12;
            float flap = (float) Math.sin(t * 9 + i * 2) * 7;
            float s = 7 + rnd(i, 4) * 5;
            g.color(0xCC2A2A2A);
            g.begin();
            g.moveTo(x - s, y - flap);
            g.quadTo(x - s * 0.4f, y - 2, x, y);
            g.quadTo(x + s * 0.4f, y - 2, x + s, y - flap);
            g.strokePath(2);
        }
    }

    /**
     * Something in flight or falling, under gravity, bouncing where it lands (fruit from a tree, a thrown ball).
     * kind: 0 stone, 1 ball, 2 fruit, 3 flower. age 0 = when it is let go.
     */
    public static void flyingObject(Gfx g, float x0, float y0, float vx, float vy, float groundY, float age, int kind, float size) {
        float gr = 1500, x = x0, y = y0, vX = vx, vY = vy, a = age;
        for (int b = 0; b < 4 && a > 0; b++) {
            float tHit = solveHit(y, vY, gr, groundY);
            if (a < tHit) { x += vX * a; y += vY * a + 0.5f * gr * a * a; a = 0; break; }
            x += vX * tHit; y = groundY;
            a -= tHit;
            vY = -(vY + gr * tHit) * 0.4f; vX *= 0.6f;          // each bounce keeps 40 % of the speed
            if (Math.abs(vY) < 60) { x += vX * Math.min(a, 0.3f); a = 0; break; }
        }
        float r = (kind == 0 ? 6 : kind == 1 ? 10 : kind == 2 ? 11 : 8) * size;
        g.color(0x40000000);
        g.oval(x, groundY + 3, r * 1.1f, r * 0.3f);
        int c = kind == 0 ? 0xFF757575 : kind == 1 ? 0xFFE53935 : kind == 2 ? 0xFFFFA726 : 0xFFF06292;
        g.color(c);
        g.oval(x, y - r, r, r);
        if (kind == 2) { g.color(0xFF43A047); g.oval(x + r * 0.4f, y - r * 1.9f, r * 0.45f, r * 0.22f); }
        if (kind == 1) { g.color(0x66FFFFFF); g.oval(x - r * 0.3f, y - r * 1.3f, r * 0.3f, r * 0.25f); }
    }

    static float solveHit(float y, float vy, float gr, float ground) {
        float a = 0.5f * gr, b = vy, c = y - ground;
        float d = b * b - 4 * a * c;
        if (d < 0) return 1e9f;
        float s = (float) Math.sqrt(d);
        float t1 = (-b - s) / (2 * a), t2 = (-b + s) / (2 * a);
        return t1 > 1e-4f ? t1 : t2 > 1e-4f ? t2 : 1e9f;
    }

    /** Dust and bits falling while the ground shakes. */
    public static void quakeDust(Gfx g, float w, float h, float t, float amount) {
        int n = (int) (240 * amount);
        float sc = h / 720f;
        for (int i = 0; i < n; i++) {
            float period = 1.2f + rnd(i, 1);
            float age = (t + rnd(i, 2) * period) % period;
            float x = rnd(i, 3) * w + (float) Math.sin(t * 40 + i) * 3, y = 0.5f * 900 * age * age * sc;
            g.color(alpha(i % 3 == 0 ? 0xFF6D5A48 : 0xFFA89276, 0.75f));
            float r = (1.5f + rnd(i, 4) * 3) * sc;
            g.oval(x, y, r, r);
        }
        g.color(alpha(0xFFB09A80, 0.2f * amount));
        g.rect(0, 0, w, h);
    }
}
