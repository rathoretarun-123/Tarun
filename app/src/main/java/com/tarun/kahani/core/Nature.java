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
        public float skyBottom;              // fraction of the picture height
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
        float[] cnt = new float[N], wat = new float[N], foam = new float[N], grn = new float[N], gx = new float[N], gy = new float[N], blue = new float[N];
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
                if (y <= skyRow + h / 40) continue;
                if (hh >= 175 && hh <= 235 && ss > 0.12f && ss < 0.7f && vv > 0.25f && vv < 0.96f && y > minWaterY && dxl + dyl < 0.12f) wat[g]++;
                else if (ss < 0.2f && vv > 0.8f) { foam[g]++; gx[g] += dxl; gy[g] += dyl; }
                else if (hh >= 65 && hh <= 165 && ss > 0.18f && vv > 0.12f) grn[g]++;
            }
        }
        float blueAll = 0, cntAll = 0;
        for (int i = 0; i < N; i++) {
            blueAll += blue[i]; cntAll += cnt[i];
            if (cnt[i] == 0) continue;
            s.water[i] = wat[i] / cnt[i] > 0.45f ? Math.min(1, wat[i] / cnt[i] * 1.4f) : 0;
            s.plants[i] = Math.min(1, grn[i] / cnt[i] * 1.5f);
            // falling water: bright white with vertical streaks (changes more across than down)
            boolean streaks = foam[i] / cnt[i] > 0.4f && gx[i] > gy[i] * 1.35f;
            float yb = (i / GW + 0.5f) / GH;
            s.fall[i] = streaks && yb > s.skyBottom + 0.02f ? Math.min(1, foam[i] / cnt[i] * 1.3f) : 0;
        }
        keepBig(s.water, 10);
        keepBig(s.fall, 4);
        // a picture that is blue all over is blue light (night, magic), not water
        if (blueAll > cntAll * 0.45f) java.util.Arrays.fill(s.water, 0);
        blur(s.water); blur(s.plants); blur(s.fall);
        float sw = 0, sf = 0, sp = 0;
        for (int i = 0; i < N; i++) { sw += s.water[i]; sf += s.fall[i]; sp += s.plants[i]; }
        s.anyWater = sw > 6; s.anyFall = sf > 2.5f; s.anyPlants = sp > 12;
        return s;
    }

    static float lum(int c) { return (((c >> 16) & 255) * 0.3f + ((c >> 8) & 255) * 0.59f + (c & 255) * 0.11f) / 255f; }

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

    public static final int MW = 48, MH = 27;

    /**
     * Mesh points for drawing a background picture with its plants swaying in the wind (a gentle breeze always)
     * and its water rippling along the flow (a waterfall streams down). The picture's crop window (x0..x1,
     * y0..y1 of the picture) fills the stage (0..W, 0..H).
     */
    public static void backdropMesh(Scan s, float x0, float y0, float x1, float y1, float W, float H, float t, float wind, float[] out) {
        int k = 0;
        float sway = 0.25f + Math.abs(wind);
        float dir = wind >= 0 ? 1 : -1;
        for (int j = 0; j <= MH; j++) {
            float v = j / (float) MH;
            for (int i = 0; i <= MW; i++) {
                float u = i / (float) MW;
                float X = (u - x0) / (x1 - x0) * W, Y = (v - y0) / (y1 - y0) * H;
                float edge = Math.min(Math.min(i, MW - i), Math.min(j, MH - j)) >= 1 ? 1 : 0;
                float dx = 0, dy = 0;
                if (s.anyPlants) {
                    float p = s.at(s.plants, u, v);
                    if (p > 0.02f) {
                        // tops of plants move more than their roots; gusts travel across the picture
                        float gust = (float) (Math.sin(t * 1.7 + u * 9 + v * 3) * 0.6 + Math.sin(t * 3.1 + u * 17) * 0.25);
                        float lean = Math.abs(wind) * 0.8f;
                        dx += p * (2.2f + 9f * sway) * (gust * 0.6f + lean * dir) * (0.4f + 0.6f * (1 - v));
                        dy += p * 1.2f * sway * (float) Math.sin(t * 2.3 + u * 13);
                    }
                }
                if (s.anyWater) {
                    float wv = s.at(s.water, u, v);
                    if (wv > 0.02f) {
                        // waves travelling along the river
                        dx += wv * 2.0f * (float) Math.sin(u * 70 - t * 2.4 + v * 20);
                        dy += wv * 1.4f * (float) Math.sin(u * 45 + v * 60 - t * 3.3);
                    }
                }
                if (s.anyFall) {
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
        int n = (int) (380 * Math.min(1.6f, amount));
        float slant = wind * 0.35f;
        for (int i = 0; i < n; i++) {
            float depth = rnd(i, 1);                       // 0 far .. 1 near
            float speed = h * (1.4f + depth * 1.1f);
            float len = 14 + depth * 30;
            float period = (h + len) / speed;
            float age = (t + rnd(i, 2) * period) % period;
            float y = -len + age * speed;
            float x = rnd(i, 3) * (w + h * Math.abs(slant)) - (slant > 0 ? h * slant : 0) + y * slant;
            g.color(alpha(0xFFDDE6F0, 0.18f + depth * 0.3f));
            g.line(x, y, x - len * slant, y - len, 0.8f + depth * 1.4f);
        }
        // a cool, grey light
        g.color(alpha(0xFF40506A, 0.16f * Math.min(1, amount)));
        g.rect(0, 0, w, h);
    }

    /** Drops hitting the ground: little crowns and rings (stage coordinates, along the floor). */
    public static void rainSplashes(Gfx g, float ground, float t, float amount) {
        int n = (int) (60 * Math.min(1.6f, amount));
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
        int n = (int) (220 * Math.min(1.5f, amount));
        for (int i = 0; i < n; i++) {
            float depth = rnd(i, 1);
            float speed = 35 + depth * 70;
            float period = (h + 20) / speed;
            float age = (t + rnd(i, 2) * period) % period;
            float y = -10 + age * speed;
            float x = (rnd(i, 3) * (w + 200) - 100 + wind * 60 * age + (float) Math.sin(t * (0.8 + rnd(i, 4)) + i) * 14 * (1 + depth)) % (w + 200);
            if (x < -100) x += w + 200;
            g.color(alpha(0xFFFFFFFF, 0.55f + depth * 0.4f));
            g.oval(x, y, 1.4f + depth * 2.8f, 1.4f + depth * 2.8f);
        }
        g.color(alpha(0xFFE8F0FF, 0.1f * amount));
        g.rect(0, 0, w, h);
    }

    /** Leaves (or petals) blown by the wind: they tumble, fall slowly and travel with the gusts. */
    public static void leaves(Gfx g, float w, float h, float t, float amount, float wind, boolean petals) {
        int n = (int) ((petals ? 70 : 40) * Math.min(1.5f, amount));
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
            float s = (petals ? 4 : 6) + rnd(i, 4) * 5;
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
        int n = (int) (26 * amount);
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
    public static void fire(Gfx g, float x, float ground, float t, float size) {
        float s = size;
        g.color(0xFF4E342E);
        g.save(); g.translate(x, ground - 6 * s); g.rotate(14); g.roundRect(-38 * s, -6 * s, 76 * s, 12 * s, 5 * s); g.restore();
        g.save(); g.translate(x, ground - 6 * s); g.rotate(-14); g.roundRect(-38 * s, -6 * s, 76 * s, 12 * s, 5 * s); g.restore();
        // glow on the ground
        g.radial(x, ground - 20 * s, 160 * s, 0x60FF9A30, 0x00FF9A30);
        g.rect(x - 160 * s, ground - 180 * s, 320 * s, 340 * s);
        // flames: tongues that flicker
        int[] cols = {0xFFFF5722, 0xFFFF9800, 0xFFFFC107, 0xFFFFF59D};
        for (int layer = 0; layer < 4; layer++) {
            float sc = 1 - layer * 0.22f;
            for (int k = -2; k <= 2; k++) {
                float fl = (float) (Math.sin(t * (9 + k) + k * 1.7 + layer) * 0.18 + Math.sin(t * 17 + k) * 0.08);
                float hgt = (70 + 20 * (2 - Math.abs(k))) * s * sc * (1 + fl);
                float bx = x + k * 13 * s * sc;
                g.begin();
                g.moveTo(bx - 14 * s * sc, ground - 8 * s);
                g.quadTo(bx - 16 * s * sc, ground - hgt * 0.5f, bx + fl * 20 * s, ground - hgt);
                g.quadTo(bx + 16 * s * sc, ground - hgt * 0.5f, bx + 14 * s * sc, ground - 8 * s);
                g.close();
                g.color(alpha(cols[layer], 0.85f));
                g.fillPath();
            }
        }
        // sparks: thrown up, slowed by the air, fading
        for (int i = 0; i < 14; i++) {
            float life = 1.2f + rnd(i, 1);
            float age = (t + rnd(i, 2) * life) % life;
            float sx = x + (rnd(i, 3) - 0.5f) * 40 * s + (float) Math.sin(age * 4 + i) * 10 * s;
            float sy = ground - 40 * s - age * (90 + rnd(i, 4) * 60) * s;
            g.color(alpha(0xFFFFD54F, 1 - age / life));
            g.oval(sx, sy, 2.2f * s, 2.2f * s);
        }
        // smoke
        for (int i = 0; i < 6; i++) {
            float life = 4f;
            float age = (t + i * life / 6) % life;
            float sy = ground - 110 * s - age * 45 * s, sx = x + (float) Math.sin(age + i) * 20 * s + age * 12 * s;
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
            for (int i = 0; i < 12 + (int) (size * 6); i++) {
                float ang = (float) (Math.PI * (0.15 + 0.7 * rnd(i, 1)));
                float sp = (180 + rnd(i, 2) * 220) * (float) Math.sqrt(size);
                float vx = (float) Math.cos(ang) * sp * (rnd(i, 3) > 0.5f ? 1 : -1), vy = -(float) Math.sin(ang) * sp;
                float dx = vx * age, dy = vy * age + 0.5f * 1400 * age * age;
                if (dy > 4) continue;
                g.color(alpha(0xFFE3F2FD, 0.85f * (1 - age / 0.9f)));
                g.oval(x + dx, y + dy, 2 + size, 2.6f + size);
            }
            // the column of water
            float col = (float) Math.max(0, Math.sin(Math.PI * Math.min(1, age / 0.5f))) * 26 * size;
            g.color(alpha(0xFFE3F2FD, 0.6f));
            g.oval(x, y - col * 0.5f, 4 * size, col * 0.5f + 1);
        }
        // waves: three rings, each spreading at the same speed and dying away
        for (int k = 0; k < 3; k++) {
            float a = age - k * 0.28f;
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
}
