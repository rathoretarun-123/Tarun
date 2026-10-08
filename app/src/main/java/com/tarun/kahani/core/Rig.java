package com.tarun.kahani.core;

/**
 * Bones and a face for a picture character, so a flat picture can turn and nod its head, swing and lift its
 * arms, step with its legs, lean, breathe and change its expression (smile, frown, raised or angry brows,
 * wide or squinting eyes).
 *
 * The picture is drawn as a mesh: every mesh point follows the bones near it (head, arms, legs, upper body),
 * blended smoothly so the picture bends instead of breaking. The face is drawn a second time as a finer mesh
 * on top, where small shifts around the eyes, brows, cheeks and mouth corners make the expressions.
 *
 * All landmarks are fractions of the cut-out picture (0..1), found from its outline and the face points.
 */
public final class Rig {
    // body landmarks (fractions of the picture)
    public float cx, top, bottom, chinY, neckY, shoulderY, shoulderHalf, hipY, gapX, legHalf;
    public boolean legs;          // two separate legs (trousers, bare legs) — otherwise a skirt / robe
    public float armEnd;          // where the hands end (fraction of the picture height)
    public float armInner;        // distance from the middle where the arms start (fraction of shoulderHalf)
    // face landmarks
    public boolean face;
    public float eLX, eLY, eRX, eRY, eR, mX, mY, mHW;
    // the face drawn a second time: a crop of the picture with soft edges
    public Object faceImg, faceWetImg;
    public float fu0, fv0, fu1, fv1;
    /** How much each body mesh point is loose hair (0..1), from the picture's dark hair colours. */
    public float[] hairW;

    public static final int BW = 14, BH = 28, FW = 16, FH = 16;

    /** What the rig does in one frame. Angles in degrees (positive = clockwise on screen). */
    public static final class State {
        public float headRot, nod;           // nod: +1 looks down, -1 looks up
        public float lean;                   // upper body
        public float armL, armR;             // outward swing of the arm on the picture's left / right side
        public float legLAng, legRAng, legLLift, legRLift;
        public float legScale = 1;           // < 1 when sitting / kneeling
        public float breathe;                // -1..1
        // face, 0..1
        public float smile, frown, innerUp, browUp, browUpR, anger, wide, squint;
        public float wind;                   // + blows towards the picture's right
        public float time;
        public void reset() {
            headRot = nod = lean = armL = armR = legLAng = legRAng = legLLift = legRLift = breathe = wind = time = 0;
            legScale = 1;
            smile = frown = innerUp = browUp = browUpR = anger = wide = squint = 0;
        }
        /** Moves the face part of this state towards target (k = 0..1 per frame). */
        public void easeFace(State target, float k) {
            smile += (target.smile - smile) * k; frown += (target.frown - frown) * k; innerUp += (target.innerUp - innerUp) * k;
            browUp += (target.browUp - browUp) * k; browUpR += (target.browUpR - browUpR) * k; anger += (target.anger - anger) * k;
            wide += (target.wide - wide) * k; squint += (target.squint - squint) * k;
        }
    }

    // ------------------------------------------------------------------ building

    /**
     * Finds the bones from the cut-out picture's outline. kind = Look kind; animals on four legs get no rig.
     * Returns null when the picture is not a standing figure.
     */
    public static Rig build(Cutout.Result r, Art.Sprite s, Look look, Art.Loader L) {
        if (r == null || r.px == null || r.w < 40 || r.h < 80) return null;
        if (look != null && (look.kind == Look.ANIMAL || look.kind == Look.BIRD)) return null;
        int w = r.w, h = r.h;
        float aspect = h / (float) w;
        if (aspect < 1.25f) return null;               // not an upright figure
        int[] left = new int[h], right = new int[h];
        int topRow = -1, botRow = -1;
        for (int y = 0; y < h; y++) {
            left[y] = -1; right[y] = -1;
            for (int x = 0; x < w; x++) if ((r.px[y * w + x] >>> 24) > 128) { if (left[y] < 0) left[y] = x; right[y] = x; }
            if (left[y] >= 0) { if (topRow < 0) topRow = y; botRow = y; }
        }
        if (topRow < 0 || botRow - topRow < h / 2) return null;
        Rig g = new Rig();
        g.top = topRow / (float) h;
        g.bottom = botRow / (float) h;
        g.face = s.faceKnown;
        g.eLX = s.eyeLX; g.eLY = s.eyeLY; g.eRX = s.eyeRX; g.eRY = s.eyeRY; g.eR = s.eyeR;
        g.mX = s.mouthX; g.mY = s.mouthY; g.mHW = s.mouthHW;
        float eyeY = (s.eyeLY + s.eyeRY) / 2;
        if (s.faceKnown && s.mouthY > eyeY) {
            g.cx = (s.eyeLX + s.eyeRX + 2 * s.mouthX) / 4;
            g.chinY = Math.min(0.6f, s.mouthY + (s.mouthY - eyeY) * 0.95f);
        } else {
            g.cx = 0.5f;
            g.chinY = g.top + (g.bottom - g.top) * 0.22f;
        }
        g.neckY = g.chinY + 0.015f;
        float body = g.bottom - g.chinY;
        // shoulders: first row under the chin that is clearly wider than the head
        int headRow = (int) ((s.faceKnown ? eyeY : g.top + 0.1f) * h);
        float headW = Math.max(4, right[Math.max(topRow, Math.min(botRow, headRow))] - left[Math.max(topRow, Math.min(botRow, headRow))]);
        g.shoulderY = g.chinY + 0.05f * body;
        for (int y = (int) (g.chinY * h); y < (int) ((g.chinY + 0.25f * body) * h) && y <= botRow; y++) {
            if (left[y] >= 0 && right[y] - left[y] > headW * 1.25f) { g.shoulderY = y / (float) h + 0.01f; break; }
        }
        g.shoulderY = Math.max(g.shoulderY, g.chinY + 0.035f);   // the arms never reach into the face
        int sr = Math.min(botRow, (int) ((g.shoulderY + 0.04f * body) * h));
        g.shoulderHalf = left[sr] >= 0 ? (right[sr] - left[sr]) / 2f / w : 0.3f;
        // hips: about 42 % down the body, moved to the narrowest middle part nearby
        float hip = g.chinY + 0.42f * body;
        int best = -1;
        float bestW = 1e9f;
        int cxPx = (int) (g.cx * w);
        for (int y = (int) ((hip - 0.08f * body) * h); y <= (int) ((hip + 0.06f * body) * h); y++) {
            if (y < topRow || y > botRow) continue;
            int[] run = runAround(r.px, w, y, cxPx);
            if (run == null) continue;
            float rw = run[1] - run[0];
            if (rw < bestW) { bestW = rw; best = y; }
        }
        g.hipY = best > 0 ? best / (float) h : hip;
        // legs: two separate runs below the hips for a good part of the way down
        int two = 0, rows = 0;
        float gapSum = 0, halfSum = 0;
        for (int y = (int) ((g.hipY + 0.12f * body) * h); y < (int) ((g.bottom - 0.03f) * h); y += 2) {
            if (y < 0 || y >= h) continue;
            rows++;
            int[] runs = twoRuns(r.px, w, y, Math.max(2, w / 80));
            if (runs != null) { two++; gapSum += (runs[1] + runs[2]) / 2f; halfSum += ((runs[1] - runs[0]) + (runs[3] - runs[2])) / 4f; }
        }
        g.legs = rows > 0 && two > rows * 0.35f;
        // arms: rows where the outline splits into arm | body | arm show how far down the hands come
        // and where the arms begin; otherwise a typical length is used
        int last = -1, found = 0;
        float innerSum = 0;
        int y0 = (int) ((g.shoulderY + 0.04f * body) * h), y1 = (int) ((g.chinY + 0.62f * body) * h);
        for (int y = Math.max(topRow, y0); y <= Math.min(botRow, y1); y++) {
            int[] arm = threeRuns(r.px, w, y, Math.max(2, w / 120), cxPx);
            if (arm == null) continue;
            found++;
            last = y;
            innerSum += Math.min(cxPx - arm[0], arm[1] - cxPx) / (g.shoulderHalf * w);
        }
        if (found > 0.04f * body * h) {
            g.armEnd = last / (float) h + 0.015f;
            g.armInner = Math.max(0.35f, Math.min(0.85f, innerSum / found - 0.08f));
        } else {
            g.armEnd = g.chinY + 0.36f * body;
            g.armInner = 0.55f;
        }
        g.gapX = g.legs ? gapSum / two / w : g.cx;
        g.legHalf = g.legs ? halfSum / two / w : 0.1f;
        g.hairW = new float[(BW + 1) * (BH + 1)];
        for (int j = 0; j <= BH; j++) {
            for (int i = 0; i <= BW; i++) {
                int cxp = Math.min(w - 1, i * (w - 1) / BW), cyp = Math.min(h - 1, j * (h - 1) / BH);
                int dark = 0, n = 0;
                for (int dy = -4; dy <= 4; dy += 2) for (int dx = -4; dx <= 4; dx += 2) {
                    int xx = cxp + dx * w / 200, yy = cyp + dy * h / 400;
                    if (xx < 0 || yy < 0 || xx >= w || yy >= h) continue;
                    int c = r.px[yy * w + xx];
                    if ((c >>> 24) < 128) continue;
                    n++;
                    int rr = (c >> 16) & 255, gg = (c >> 8) & 255, bb = c & 255;
                    if (rr + gg + bb < 200 && !Cutout.isSkin(c)) dark++;
                }
                float v = j / (float) BH;
                g.hairW[j * (BW + 1) + i] = n == 0 || v > g.hipY + 0.25f * (g.bottom - g.hipY) ? 0 : dark / (float) n;
            }
        }
        if (g.face && L != null) g.makeFace(r, L);
        return g;
    }

    /** The opaque run of a row that contains x (or the nearest one). */
    static int[] runAround(int[] px, int w, int y, int x) {
        int row = y * w;
        if (x < 0 || x >= w) return null;
        if ((px[row + x] >>> 24) <= 128) return null;
        int a = x, b = x;
        while (a > 0 && (px[row + a - 1] >>> 24) > 128) a--;
        while (b < w - 1 && (px[row + b + 1] >>> 24) > 128) b++;
        return new int[]{a, b};
    }

    /** Two biggest opaque runs of a row, left to right, if the row splits in two (legs). */
    static int[] twoRuns(int[] px, int w, int y, int minGap) {
        int row = y * w;
        int[] best = null;
        int b1 = 0, b2 = 0;
        int x = 0;
        int[][] runs = new int[8][];
        int n = 0;
        while (x < w && n < 8) {
            while (x < w && (px[row + x] >>> 24) <= 128) x++;
            if (x >= w) break;
            int a = x;
            while (x < w && (px[row + x] >>> 24) > 128) x++;
            runs[n++] = new int[]{a, x - 1};
        }
        if (n < 2) return null;
        // the two widest runs, kept in left-to-right order
        int i1 = 0, i2 = 1;
        for (int i = 0; i < n; i++) {
            int len = runs[i][1] - runs[i][0];
            if (len > b1) { b2 = b1; i2 = i1; b1 = len; i1 = i; } else if (len > b2) { b2 = len; i2 = i; }
        }
        int l = Math.min(i1, i2), r = Math.max(i1, i2);
        if (runs[r][0] - runs[l][1] < minGap || b2 < b1 * 0.35f) return null;
        best = new int[]{runs[l][0], runs[l][1], runs[r][0], runs[r][1]};
        return best;
    }

    /**
     * For a row that splits into three or more parts (arm, body, arm): {inner edge of the left arm, inner edge
     * of the right arm}, or null.
     */
    static int[] threeRuns(int[] px, int w, int y, int minGap, int cx) {
        int row = y * w;
        int x = 0, n = 0;
        int[] starts = new int[12], ends = new int[12];
        while (x < w && n < 12) {
            while (x < w && (px[row + x] >>> 24) <= 128) x++;
            if (x >= w) break;
            int a = x;
            while (x < w && (px[row + x] >>> 24) > 128) x++;
            if (n > 0 && a - ends[n - 1] <= minGap) { ends[n - 1] = x; continue; }   // a tiny gap is no gap
            starts[n] = a; ends[n] = x; n++;
        }
        if (n < 3) return null;
        // the run holding the middle is the body; the runs either side are arms
        int mid = -1;
        for (int i = 0; i < n; i++) if (starts[i] <= cx && ends[i] >= cx) mid = i;
        if (mid <= 0 || mid >= n - 1) return null;
        return new int[]{starts[mid], ends[mid]};
    }

    /** Crops the face with soft edges so it can be drawn over the body with its own fine mesh. */
    void makeFace(Cutout.Result r, Art.Loader L) {
        float d = Math.abs(eRX - eLX);
        if (d < 0.02f) { face = false; return; }
        float fcx = (eLX + eRX) / 2;
        float eyeY = (eLY + eRY) / 2;
        float dv = d * r.w / (float) r.h;     // the eye distance in height units
        fu0 = Math.max(0, fcx - 1.3f * d);
        fu1 = Math.min(1, fcx + 1.3f * d);
        fv0 = Math.max(0, eyeY - 1.15f * dv);
        fv1 = Math.min(chinY, 1);
        int x0 = (int) (fu0 * r.w), x1 = (int) Math.ceil(fu1 * r.w), y0 = (int) (fv0 * r.h), y1 = (int) Math.ceil(fv1 * r.h);
        int cw = Math.min(r.w, x1) - x0, ch = Math.min(r.h, y1) - y0;
        if (cw < 12 || ch < 12) { face = false; return; }
        int[] px = new int[cw * ch];
        float feather = Math.min(cw, ch) * 0.16f;
        for (int y = 0; y < ch; y++) {
            for (int x = 0; x < cw; x++) {
                int c = r.px[(y + y0) * r.w + x + x0];
                float e = Math.min(Math.min(x, cw - 1 - x), Math.min(y, ch - 1 - y));
                float k = Math.min(1, e / feather);
                int a = (int) ((c >>> 24) * k * k * (3 - 2 * k));
                px[y * cw + x] = (a << 24) | (c & 0xFFFFFF);
            }
        }
        // exact crop edges as fractions
        fu0 = x0 / (float) r.w; fu1 = (x0 + cw) / (float) r.w;
        fv0 = y0 / (float) r.h; fv1 = (y0 + ch) / (float) r.h;
        faceImg = L.create(px, cw, ch);
        faceWetImg = L.create(Art.wetPixels(px), cw, ch);
    }

    // ------------------------------------------------------------------ moving

    static float smooth(float a, float b, float x) {
        float t = Math.max(0, Math.min(1, (x - a) / (b - a)));
        return t * t * (3 - 2 * t);
    }

    /**
     * Working values for one frame. Each renderer (thread) keeps its own, so several frames can be drawn at
     * the same time from the same pictures.
     */
    public static final class Frame {
        float L0, T0, W0, H0;
        float nX, nY, hipCX, hipCY;          // neck pivot, hip centre
        float hCos = 1, hSin, lCos = 1, lSin, nodS = 1, nodDy;
        final float[] armC = new float[2], armS = new float[2], legC = new float[3], legS = new float[3];
        final float[] tmp = new float[2], legTmp = new float[2], o = new float[2], bumps = new float[5 * 24];
        public final float[] body = new float[(BW + 1) * (BH + 1) * 2];
        public final float[] face = new float[(FW + 1) * (FH + 1) * 2];
    }

    private void frame(Frame f, State s, float left, float top, float w, float h) {
        f.L0 = left; f.T0 = top; f.W0 = w; f.H0 = h;
        f.nX = left + cx * w; f.nY = top + neckY * h;
        f.hipCX = left + cx * w; f.hipCY = top + hipY * h;
        double hr = Math.toRadians(s.headRot), lr = Math.toRadians(s.lean);
        f.hCos = (float) Math.cos(hr); f.hSin = (float) Math.sin(hr);
        f.lCos = (float) Math.cos(lr); f.lSin = (float) Math.sin(lr);
        for (int side = 0; side < 2; side++) {
            double a = Math.toRadians(side == 0 ? s.armL : -s.armR);   // outward: the left arm turns clockwise
            f.armC[side] = (float) Math.cos(a); f.armS[side] = (float) Math.sin(a);
        }
        float[] la = {s.legLAng, s.legRAng, (s.legLAng - s.legRAng) * 0.15f};
        for (int i = 0; i < 3; i++) { double a = Math.toRadians(la[i]); f.legC[i] = (float) Math.cos(a); f.legS[i] = (float) Math.sin(a); }
        float headH = (neckY - this.top) * h;
        f.nodS = 1 - 0.035f * Math.abs(s.nod);
        f.nodDy = s.nod * headH * 0.04f;
    }

    /** Head bone then upper-body lean: the exact transform applyHead() gives the canvas. */
    private static void head(Frame f, float x, float y, float[] o) {
        headLocal(f, x, y, o);
        lean(f, o[0], o[1], o);
    }

    private static void headLocal(Frame f, float x, float y, float[] o) {
        y += f.nodDy;
        float dx = x - f.nX, dy = (y - f.nY) * f.nodS;
        o[0] = f.nX + f.hCos * dx - f.hSin * dy;
        o[1] = f.nY + f.hSin * dx + f.hCos * dy;
    }

    private static void lean(Frame f, float x, float y, float[] o) {
        float dx = x - f.hipCX, dy = y - f.hipCY;
        o[0] = f.hipCX + f.lCos * dx - f.lSin * dy;
        o[1] = f.hipCY + f.lSin * dx + f.lCos * dy;
    }

    /** Where one point of the picture (local coordinates) goes in this frame. */
    private void move(Frame f, State s, float x, float y, float[] o) {
        moveBody(f, s, x, y, o);
        if (s.wind == 0) return;
        // wind: loose hair streams out (more the further it hangs), skirts bend and flutter at the hem
        float u = (x - f.L0) / f.W0, v = (y - f.T0) / f.H0;
        float hair = 0;
        if (hairW != null) {
            int i = Math.max(0, Math.min(BW, Math.round(u * BW))), j = Math.max(0, Math.min(BH, Math.round(v * BH)));
            hair = hairW[j * (BW + 1) + i] * smooth(top + 0.04f, top + 0.3f, v);
        }
        float cloth = smooth(hipY, bottom, v) * (legs ? 0.3f : 1f);
        float t = s.time;
        float flap = 0.75f + 0.35f * (float) Math.sin(t * 6.3 + v * 9 + u * 3);
        float dx = s.wind * f.W0 * (0.07f * hair * flap + 0.05f * cloth * cloth * (0.8f + 0.4f * (float) Math.sin(t * 7.1 + v * 12)));
        float dy = -Math.abs(s.wind) * f.H0 * 0.01f * hair * (float) Math.sin(t * 5 + u * 6);
        o[0] += dx;
        o[1] += dy;
    }

    private void moveBody(Frame f, State s, float x, float y, float[] o) {
        float u = (x - f.L0) / f.W0, v = (y - f.T0) / f.H0;
        // ---- upper body: head, arms, breathing, then the lean
        float wHead = 1 - smooth(chinY + 0.005f, chinY + 0.05f, v);
        float armTop = shoulderY - 0.02f, armBot = armEnd;
        float wArmV = smooth(armTop, armTop + 0.04f, v) * (1 - smooth(armBot - 0.03f, armBot + 0.02f, v));
        float off = (u - cx) / Math.max(0.05f, shoulderHalf);
        float wArmL = wArmV * smooth(-armInner + 0.1f, -armInner - 0.15f, off), wArmR = wArmV * smooth(armInner - 0.1f, armInner + 0.15f, off);
        // breathing: the chest widens a little
        float chest = smooth(shoulderY, shoulderY + 0.05f, v) * (1 - smooth(hipY - 0.08f, hipY, v));
        float px = f.L0 + (cx + (u - cx) * (1 + 0.012f * s.breathe * chest)) * f.W0, py = y;
        float qx = px, qy = py;
        if (wHead > 0) {
            headLocal(f, px, py, f.tmp);
            qx += wHead * (f.tmp[0] - px); qy += wHead * (f.tmp[1] - py);
        }
        if (wArmL > 0 || wArmR > 0) {
            float sy = f.T0 + (shoulderY + 0.015f) * f.H0;
            for (int side = 0; side < 2; side++) {
                float wa = side == 0 ? wArmL : wArmR;
                if (wa <= 0) continue;
                float sx = f.L0 + (cx + (side == 0 ? -1 : 1) * shoulderHalf * 0.62f) * f.W0;
                float c = f.armC[side], sn = f.armS[side];
                float dx = px - sx, dy = py - sy;
                qx += wa * (sx + c * dx - sn * dy - px);
                qy += wa * (sy + sn * dx + c * dy - py);
            }
        }
        lean(f, qx, qy, f.tmp);
        float ux = f.tmp[0], uy = f.tmp[1];
        // ---- lower body: legs (or a skirt) from the hips
        float wLow = smooth(hipY - 0.03f, hipY + 0.05f, v);
        if (wLow <= 0) { o[0] = ux; o[1] = uy; return; }
        float lx, ly;
        float hy = f.T0 + hipY * f.H0;
        if (legs) {
            float side = smooth(gapX - 0.02f, gapX + 0.02f, u);     // 0 = left leg, 1 = right leg
            float jx = f.L0 + gapX * f.W0;
            legPoint(f, x, y, jx - legHalf * f.W0, hy, 0, s.legLLift, s.legScale);
            float rlx = f.legTmp[0], rly = f.legTmp[1];
            legPoint(f, x, y, jx + legHalf * f.W0, hy, 1, s.legRLift, s.legScale);
            lx = rlx + (f.legTmp[0] - rlx) * side;
            ly = rly + (f.legTmp[1] - rly) * side;
        } else {
            legPoint(f, x, y, f.L0 + cx * f.W0, hy, 2, 0, Math.max(0.7f, s.legScale));
            lx = f.legTmp[0]; ly = f.legTmp[1];
        }
        o[0] = ux + (lx - ux) * wLow;
        o[1] = uy + (ly - uy) * wLow;
    }

    /** which: 0 left leg, 1 right leg, 2 skirt. */
    private static void legPoint(Frame f, float x, float y, float jx, float jy, int which, float lift, float scale) {
        float dy = (y - jy) * scale * (1 - lift);
        float dx = x - jx;
        float c = f.legC[which], s = f.legS[which];
        f.legTmp[0] = jx + c * dx - s * dy;
        f.legTmp[1] = jy + s * dx + c * dy;
    }

    /** Mesh points of the whole picture for this frame into f.body: (BW+1)*(BH+1) pairs. */
    public void bodyMesh(Frame f, State s, float left, float top, float w, float h) {
        frame(f, s, left, top, w, h);
        int k = 0;
        for (int j = 0; j <= BH; j++) {
            float y = top + h * j / BH;
            for (int i = 0; i <= BW; i++) {
                float x = left + w * i / BW;
                move(f, s, x, y, f.o);
                f.body[k++] = f.o[0];
                f.body[k++] = f.o[1];
            }
        }
    }

    /** Mesh points of the face crop with the expression into f.face (call after bodyMesh). */
    public void faceMesh(Frame f, State s) {
        float left = f.L0, top = f.T0, w = f.W0, h = f.H0;
        float x0 = left + fu0 * w, y0 = top + fv0 * h, cw = (fu1 - fu0) * w, ch = (fv1 - fv0) * h;
        float ex1 = left + eLX * w, ey1 = top + eLY * h, ex2 = left + eRX * w, ey2 = top + eRY * h;
        float er = Math.max(2, eR * w), mx = left + mX * w, my = top + mY * h, mh = Math.max(3, mHW * w);
        if (ex1 > ex2) { float t = ex1; ex1 = ex2; ex2 = t; t = ey1; ey1 = ey2; ey2 = t; }
        // the shifts that make the expression: centre x, y, size, move x, move y
        float[] d = f.bumps;
        int n = 0;
        if (s.smile > 0) {           // mouth corners up and out, cheeks up
            n = add(d, n, mx - mh, my, 0.65f * mh, -0.18f * mh * s.smile, -0.45f * mh * s.smile);
            n = add(d, n, mx + mh, my, 0.65f * mh, 0.18f * mh * s.smile, -0.45f * mh * s.smile);
            n = add(d, n, ex1, ey1 + 2.1f * er, 1.4f * er, 0, -0.35f * er * s.smile);
            n = add(d, n, ex2, ey2 + 2.1f * er, 1.4f * er, 0, -0.35f * er * s.smile);
        }
        if (s.frown > 0) {           // corners down
            n = add(d, n, mx - mh, my, 0.65f * mh, 0.05f * mh * s.frown, 0.4f * mh * s.frown);
            n = add(d, n, mx + mh, my, 0.65f * mh, -0.05f * mh * s.frown, 0.4f * mh * s.frown);
        }
        // eyelids: squint (laughing, pain, anger) or wide open (surprise, fear)
        float lid = 0.3f * s.squint + 0.3f * s.anger - 0.4f * s.wide;
        float low = -0.45f * s.squint + 0.2f * s.wide;
        if (lid != 0) { n = add(d, n, ex1, ey1 - 0.95f * er, 0.8f * er, 0, lid * er); n = add(d, n, ex2, ey2 - 0.95f * er, 0.8f * er, 0, lid * er); }
        if (low != 0) { n = add(d, n, ex1, ey1 + 0.95f * er, 0.8f * er, 0, low * er); n = add(d, n, ex2, ey2 + 0.95f * er, 0.8f * er, 0, low * er); }
        // brows: up (surprise), inner ends up (sad, worried), down and together (anger)
        float by1 = ey1 - 1.75f * er, by2 = ey2 - 1.75f * er;
        float up = s.browUp + 0.3f * s.wide;
        if (up > 0) { n = add(d, n, ex1, by1, 1.3f * er, 0, -0.8f * er * up); n = add(d, n, ex2, by2, 1.3f * er, 0, -0.8f * er * up); }
        if (s.browUpR > 0) n = add(d, n, ex2, by2, 1.3f * er, 0, -0.9f * er * s.browUpR);
        if (s.innerUp > 0) {
            n = add(d, n, ex1 + 0.6f * er, by1 + 0.2f * er, 0.95f * er, 0, -0.7f * er * s.innerUp);
            n = add(d, n, ex2 - 0.6f * er, by2 + 0.2f * er, 0.95f * er, 0, -0.7f * er * s.innerUp);
            n = add(d, n, ex1 - 1.0f * er, by1, 0.9f * er, 0, 0.25f * er * s.innerUp);
            n = add(d, n, ex2 + 1.0f * er, by2, 0.9f * er, 0, 0.25f * er * s.innerUp);
        }
        if (s.anger > 0) {
            n = add(d, n, ex1 + 0.6f * er, by1 + 0.2f * er, 0.95f * er, 0.3f * er * s.anger, 0.65f * er * s.anger);
            n = add(d, n, ex2 - 0.6f * er, by2 + 0.2f * er, 0.95f * er, -0.3f * er * s.anger, 0.65f * er * s.anger);
            n = add(d, n, mx - mh, my, 0.6f * mh, 0, 0.15f * mh * s.anger);
            n = add(d, n, mx + mh, my, 0.6f * mh, 0, 0.15f * mh * s.anger);
        }
        int k = 0;
        for (int j = 0; j <= FH; j++) {
            for (int i = 0; i <= FW; i++) {
                float x = x0 + cw * i / FW, y = y0 + ch * j / FH;
                float dx = 0, dy = 0;
                for (int b = 0; b < n; b += 5) {
                    float ddx = x - d[b], ddy = y - d[b + 1], sg = d[b + 2];
                    float g = (float) Math.exp(-(ddx * ddx + ddy * ddy) / (2 * sg * sg));
                    dx += g * d[b + 3];
                    dy += g * d[b + 4];
                }
                // nothing moves at the edges of the crop, so it melts into the picture below
                float edge = Math.min(Math.min(i, FW - i) / (float) FW, Math.min(j, FH - j) / (float) FH);
                float keep = smooth(0, 0.18f, edge);
                head(f, x + dx * keep, y + dy * keep, f.o);
                f.face[k++] = f.o[0];
                f.face[k++] = f.o[1];
            }
        }
    }

    private static int add(float[] d, int n, float x, float y, float sigma, float dx, float dy) {
        if (n + 5 > d.length) return n;
        d[n] = x; d[n + 1] = y; d[n + 2] = Math.max(1, sigma); d[n + 3] = dx; d[n + 4] = dy;
        return n + 5;
    }

    /** Gives the canvas the head's transform (for the mouth, blinking eyes and tears drawn on the face). */
    public static void applyHead(Frame f, Gfx g) {
        g.translate(f.hipCX, f.hipCY);
        rotate(g, f.lSin, f.lCos);
        g.translate(-f.hipCX, -f.hipCY);
        g.translate(f.nX, f.nY);
        rotate(g, f.hSin, f.hCos);
        g.scale(1, f.nodS);
        g.translate(-f.nX, -f.nY);
        g.translate(0, f.nodDy);
    }

    private static void rotate(Gfx g, float sin, float cos) {
        float deg = (float) Math.toDegrees(Math.atan2(sin, cos));
        if (deg != 0) g.rotate(deg);
    }

    /** Where the hand of one arm is in this frame (side 0 = the picture's left, 1 = right). Call after bodyMesh. */
    public void handAt(Frame f, State s, int side, float[] o) {
        float hx = f.L0 + (cx + (side == 0 ? -1 : 1) * shoulderHalf * 0.85f) * f.W0;
        float hy = f.T0 + (hipY + 0.02f) * f.H0;
        move(f, s, hx, hy, o);
    }

    /** How far the feet come up (sitting, kneeling), so the picture can be lowered to keep them on the ground. */
    public float feetRise(State s, float h) {
        return (1 - s.legScale) * (bottom - hipY) * h;
    }
}
