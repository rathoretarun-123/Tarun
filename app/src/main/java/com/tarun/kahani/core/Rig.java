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
    public final boolean[] armUp = new boolean[2];   // an arm raised beside the face (e.g. a hand on the moustache)
    public float headHalf = 0.12f;                   // half the face width (fraction of the picture width)
    public float eyeV;
    // face landmarks
    public boolean face;
    public float eLX, eLY, eRX, eRY, eR, mX, mY, mHW;
    // the face drawn a second time: a crop of the picture with soft edges
    public Object faceImg, faceWetImg;
    public float fu0, fv0, fu1, fv1;
    /** How much each body mesh point is loose hair (0..1), from the picture's dark hair colours. */
    public float[] hairW;

    /**
     * The finest meshes: up to 48 x 96 cells over a person (96 x 48 over an animal) and 48 x 48 over the face.
     * Each frame uses as many as the picture's size on screen needs (about one cell per 6 pixels), so a
     * character in a close-up bends as smoothly as a drawing, and small far-away figures cost little.
     */
    public static final int BW = 48, BH = 96, FW = 48, FH = 48;
    /** The most mesh columns and rows for this picture: tall for people, wide (the same number of points) for animals. */
    public int mw = BW, mh = BH;
    /** Screen pixels per mesh cell the fine mesh aims for. */
    public static float CELL_PX = 6f;
    public static boolean DEBUG;   // fine meshes: smooth bends, lips and brows

    /** What the rig does in one frame. Angles in degrees (positive = clockwise on screen). */
    public static final class State {
        public float headRot, nod;           // nod: +1 looks down, -1 looks up
        public float lean;                   // upper body
        public float armL, armR;             // outward swing of the arm on the picture's left / right side
        public float legLAng, legRAng, legLLift, legRLift;
        public float legScale = 1;           // < 1 when kneeling / crouching
        public float sit;                    // 0 standing .. 1 seated: thighs fold towards the viewer, shins stay
        public boolean twirl;                // the raised hand fidgets (twirling a moustache)
        public float breathe;                // -1..1
        // face, 0..1
        public float smile, frown, innerUp, browUp, browUpR, anger, wide, squint;
        public float wind;                   // + blows towards the picture's right
        public float time;
        // animals: tail swing (degrees), jaw open 0..1, ear twitch -1..1, ears folded back 0..1, walking
        public float tail, jaw, ear, earBack, walkPhase, walkAmt;
        // lips (people): how wide (ee, s) or round (oo, o) the mouth is while it opens with the voice (jaw)
        public float lipWide, lipRound;
        public void reset() {
            headRot = nod = lean = armL = armR = legLAng = legRAng = legLLift = legRLift = breathe = wind = time = 0;
            tail = jaw = ear = earBack = walkPhase = walkAmt = 0;
            lipWide = lipRound = 0;
            legScale = 1; sit = 0; twirl = false;
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
     * Finds the bones from the cut-out picture's outline. Standing people get head, arms, legs and a face;
     * animals (and any long, lying shape) get a head with a jaw and ears, a tail and legs.
     */
    public static Rig build(Cutout.Result r, Art.Sprite s, Look look, Art.Loader L) {
        if (r == null || r.px == null || r.w < 40 || r.h < 40) return null;
        int w = r.w, h = r.h;
        float aspect = h / (float) w;
        boolean beast = look != null && (look.kind == Look.ANIMAL || look.kind == Look.BIRD);
        if (beast || aspect < 1.25f) {
            // an upright animal picture (a monkey, a bear standing) moves like a person
            if (beast && aspect >= 1.5f && r.h >= 80) beast = false;
            else {
                int[] ct = new int[w], cb = new int[w];
                int x0 = -1, x1 = -1, y0 = h, y1 = -1;
                for (int x = 0; x < w; x++) {
                    ct[x] = -1; cb[x] = -1;
                    for (int y = 0; y < h; y++) if ((r.px[y * w + x] >>> 24) > 128) { if (ct[x] < 0) ct[x] = y; cb[x] = y; }
                    if (ct[x] >= 0) { if (x0 < 0) x0 = x; x1 = x; y0 = Math.min(y0, ct[x]); y1 = Math.max(y1, cb[x]); }
                }
                if (x0 < 0 || x1 - x0 < w / 3 || y1 - y0 < 20) return null;
                return buildAnimal(r, s, ct, cb, x0, x1, y0, y1);
            }
        }
        if (r.h < 80) return null;
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
        // an arm raised to the face: skin beside the head, well outside the face, between the eyes and shoulders
        g.eyeV = s.faceKnown ? eyeY : g.top + 0.1f;
        g.headHalf = headW / 2f / w;
        int eyeRow = (int) (g.eyeV * h), shRow = (int) (g.chinY * h);     // beside the face: from the eyes to the chin
        int[] armRows = new int[2];
        int rowsN = 0;
        // the face width from the eyes (the outline at eye level may already include a raised arm)
        if (s.faceKnown && Math.abs(s.eyeRX - s.eyeLX) > 0.02f) headW = Math.abs(s.eyeRX - s.eyeLX) * w * 2.6f;
        g.headHalf = headW / 2f / w;
        for (int y = Math.max(topRow, eyeRow); y < Math.min(botRow, shRow); y++) {
            rowsN++;
            for (int side = 0; side < 2; side++) {
                // what sticks out beside the face on this side: bright (a sleeve, a hand), not dark hair
                int from = side == 0 ? Math.max(0, left[y]) : (int) (cxPx + headW * 0.7f);
                int to = side == 0 ? (int) (cxPx - headW * 0.7f) : Math.min(w - 1, right[y]);
                if (left[y] < 0 || to - from < headW * 0.3f) continue;
                float lum = 0;
                int n = 0, skin = 0;
                for (int x = from; x <= to; x += 2) {
                    int c = r.px[y * w + x];
                    if ((c >>> 24) < 128) continue;
                    n++;
                    lum += (((c >> 16) & 255) + ((c >> 8) & 255) + (c & 255)) / 765f;
                    if (Cutout.isSkin(c)) skin++;
                }
                if (n > headW * 0.08f && (lum / n > 0.42f || skin > n * 0.3f)) armRows[side]++;
            }
        }
        if (DEBUG) System.out.println("armRows " + armRows[0] + "/" + armRows[1] + " of " + rowsN);
        for (int side = 0; side < 2; side++) g.armUp[side] = rowsN > 4 && armRows[side] > rowsN * 0.3f;
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
        final float[] tmp = new float[2], legTmp = new float[2], o = new float[2], bumps = new float[5 * 40];
        public final float[] body = new float[(BW + 1) * (BH + 1) * 2];
        /** The mesh size used in this frame (pass these to Gfx.imageMesh). */
        public int cols = BW, rows = BH;
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
            float ang = side == 0 ? s.armL : -s.armR;
            if (armUp[side]) {
                // a hand held at the face does not swing out; it fidgets (twirls the moustache), more when acting
                float fid = (float) (Math.sin(s.time * 2.2 + side) * 2.5 + (s.twirl ? Math.sin(s.time * 7) * 5 : 0));
                ang = (side == 0 ? 1 : -1) * fid;
            }
            double a = Math.toRadians(ang);   // outward: the left arm turns clockwise
            f.armC[side] = (float) Math.cos(a); f.armS[side] = (float) Math.sin(a);
        }
        float[] la = {s.legLAng, s.legRAng, (s.legLAng - s.legRAng) * 0.15f};
        for (int i = 0; i < 3; i++) { double a = Math.toRadians(la[i]); f.legC[i] = (float) Math.cos(a); f.legS[i] = (float) Math.sin(a); }
        float headH = (neckY - this.top) * h;
        f.nodS = 1 - 0.035f * Math.abs(s.nod);
        f.nodDy = s.nod * headH * 0.04f;
        if (animal) {
            // the eyes and mouth drawn on an animal follow its head about the neck
            f.nX = left + neckX * w; f.nY = top + neckY2 * h;
            double a = Math.toRadians(animalHeadAngle(s));
            f.hCos = (float) Math.cos(a); f.hSin = (float) Math.sin(a);
            f.lCos = 1; f.lSin = 0; f.nodS = 1; f.nodDy = 0;
        }
    }

    /** The head angle of an animal: positive nod looks down (towards the ground in front of it). */
    float animalHeadAngle(State s) {
        return s.headRot + s.nod * 10 * headSide;
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
        if (animal) { moveAnimal(f, s, x, y, o); return; }
        moveBody(f, s, x, y, o);
        float u = (x - f.L0) / f.W0, v = (y - f.T0) / f.H0;
        float hair = 0;
        if (hairW != null) {
            int i = Math.max(0, Math.min(BW, Math.round(u * BW))), j = Math.max(0, Math.min(BH, Math.round(v * BH)));
            hair = hairW[j * (BW + 1) + i] * smooth(top + 0.04f, top + 0.3f, v);
        }
        float cloth = smooth(hipY, bottom, v) * (legs ? 0.3f : 1f);
        float t = s.time;
        // always alive (follow-through): loose hair and a skirt's hem keep swaying a little after every move,
        // more while walking; breathing lifts the shoulders and head
        float alive = 1f + 2.2f * s.walkAmt;
        o[0] += f.W0 * alive * (0.0045f * hair * (float) Math.sin(t * 1.7 + v * 5 + u * 2)
                + 0.0035f * cloth * cloth * (float) Math.sin(t * 2.3 + v * 9 - u * 3));
        o[1] -= f.H0 * 0.0022f * s.breathe * (1 - smooth(hipY - 0.12f, hipY, v)) * smooth(top, shoulderY, v + 0.05f);
        if (s.wind == 0) return;
        // wind: loose hair streams out (more the further it hangs), skirts bend and flutter at the hem
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
        // a raised arm beside the face belongs to the arm, not the head
        for (int side = 0; side < 2; side++) {
            if (!armUp[side]) continue;
            float out = (side == 0 ? cx - u : u - cx);
            float wr = smooth(headHalf * 1.0f, headHalf * 1.3f, out) * smooth(eyeV - 0.05f, eyeV - 0.01f, v) * (1 - smooth(shoulderY, shoulderY + 0.03f, v));
            if (wr <= 0) continue;
            wHead *= 1 - wr;
            if (side == 0) wArmL = Math.max(wArmL, wr); else wArmR = Math.max(wArmR, wr);
        }
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
        // sitting: the thighs come towards the viewer (they shorten), the shins stay and the knees part a little
        float sx2 = x, sy2 = y;
        if (s.sit > 0) {
            float knee = hipY + (bottom - hipY) * 0.5f;
            if (legs) {
                if (v < knee) sy2 = hy + (y - hy) * (1 - 0.8f * s.sit);
                else sy2 = y - (knee - hipY) * f.H0 * 0.8f * s.sit;
                sx2 = x + (u < gapX ? -1 : 1) * 0.035f * f.W0 * s.sit * smooth(hipY, knee, v);
            } else {
                // a skirt or robe settles and spreads over the seat
                sy2 = hy + (y - hy) * (1 - 0.38f * s.sit);
                sx2 = f.L0 + (cx + (u - cx) * (1 + 0.12f * s.sit * smooth(hipY, bottom, v))) * f.W0;
            }
        }
        if (legs) {
            float side = smooth(gapX - 0.02f, gapX + 0.02f, u);     // 0 = left leg, 1 = right leg
            float jx = f.L0 + gapX * f.W0;
            legPoint(f, sx2, sy2, jx - legHalf * f.W0, hy, 0, s.legLLift, s.legScale);
            float rlx = f.legTmp[0], rly = f.legTmp[1];
            legPoint(f, sx2, sy2, jx + legHalf * f.W0, hy, 1, s.legRLift, s.legScale);
            lx = rlx + (f.legTmp[0] - rlx) * side;
            ly = rly + (f.legTmp[1] - rly) * side;
        } else {
            legPoint(f, sx2, sy2, f.L0 + cx * f.W0, hy, 2, 0, Math.max(0.7f, s.legScale));
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

    /** Mesh points of the whole picture for this frame into f.body, at the finest detail. */
    public void bodyMesh(Frame f, State s, float left, float top, float w, float h) {
        bodyMesh(f, s, left, top, w, h, 1e9f);
    }

    /**
     * Mesh points of the whole picture for this frame into f.body: (f.cols+1)*(f.rows+1) pairs. screenPx is the
     * picture's height on screen in pixels: the mesh gets about one cell per CELL_PX pixels, up to the finest.
     */
    public void bodyMesh(Frame f, State s, float left, float top, float w, float h, float screenPx) {
        frame(f, s, left, top, w, h);
        int rows = Math.max(Math.min(mh, 12), Math.min(mh, Math.round(screenPx / CELL_PX)));
        int cols = Math.max(4, Math.min(mw, Math.round(rows * mw / (float) mh)));
        f.rows = rows; f.cols = cols;
        int k = 0;
        for (int j = 0; j <= rows; j++) {
            float y = top + h * j / rows;
            for (int i = 0; i <= cols; i++) {
                float x = left + w * i / cols;
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
        // lips: the corners go out for "ee" / "s", in and forward for "oo" / "o"; the upper lip lifts a little
        if (s.jaw > 0.02f) {
            float cxm = (0.17f * s.lipWide - 0.24f * s.lipRound) * mh;
            n = add(d, n, mx - mh, my + 0.08f * mh, 0.5f * mh, -cxm, 0.04f * mh * s.jaw);
            n = add(d, n, mx + mh, my + 0.08f * mh, 0.5f * mh, cxm, 0.04f * mh * s.jaw);
            n = add(d, n, mx, my - 0.42f * mh, 0.5f * mh, 0, -0.06f * mh * s.jaw);
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
                // the jaw: everything below the lips (lower lip, chin) drops together; the lips part at the mouth line
                if (s.jaw > 0.02f) {
                    float hx = (x - mx) / (1.7f * mh);
                    dy += jawDrop(mh) * s.jaw * smooth(my - 0.05f * mh, my + 0.3f * mh, y) * (float) Math.exp(-hx * hx);
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

    /** How far the lower lip comes down when the mouth is fully open (pixels, from the mouth's half width). */
    public static float jawDrop(float mouthHalfW) { return 0.62f * mouthHalfW; }

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
        if (animal) {   // an animal carries things in its mouth
            float ax = f.L0 + (headSide < 0 ? headX0 + 0.05f : headX1 - 0.05f) * f.W0;
            move(f, s, ax, f.T0 + jawY * f.H0, o);
            return;
        }
        float hx = f.L0 + (cx + (side == 0 ? -1 : 1) * shoulderHalf * 0.85f) * f.W0;
        float hy = f.T0 + (hipY + 0.02f) * f.H0;
        move(f, s, hx, hy, o);
    }

    /** How far the feet come up (sitting, kneeling), so the picture can be lowered to keep them on the ground. */
    public float feetRise(State s, float h) {
        if (animal) return 0.8f * s.sit * (bottom - legTop) * h;
        float rise = (1 - s.legScale) * (bottom - hipY) * h;
        if (s.sit > 0) rise += (legs ? 0.8f * 0.5f : 0.38f) * s.sit * (bottom - hipY) * h;
        return rise;
    }


    // ================================================================== animals

    /** Four-legged animals and birds (and any lying-down shape): head, jaw, tail, ears, legs. */
    public boolean animal;
    public int headSide;                      // -1 head on the picture's left, +1 on the right
    public float bodyTop, belly, headX0, headX1, headTop, headBottom, tailX, tailY, neckX, neckY2;
    public float frontLegX, backLegX, legTop, jawX, jawY, jawTipX;
    public float frontLegHalf = 0.08f, backLegHalf = 0.08f;   // half the width of each pair of legs
    public boolean hasTail;
    /** The face finder's eyes / mouth really lie on the animal's head (otherwise they are not drawn over). */
    public boolean eyesOnHead, mouthOnHead;

    static Rig buildAnimal(Cutout.Result r, Art.Sprite s, int[] top, int[] bot, int x0, int x1, int y0, int y1) {
        int w = r.w, h = r.h;
        Rig g = new Rig();
        g.animal = true;
        g.top = y0 / (float) h;
        g.bottom = y1 / (float) h;
        int width = x1 - x0 + 1;
        // the head: where the face was found, or the end of the body whose outline rises higher
        if (s.faceKnown) g.headSide = (s.eyeLX + s.eyeRX) / 2 < (x0 + x1) / 2f / w ? -1 : 1;
        else {
            float l = 0, rr = 0;
            int band = Math.max(1, width * 30 / 100);
            for (int x = x0; x < x0 + band; x++) l += top[x] < 0 ? h : top[x];
            for (int x = x1 - band + 1; x <= x1; x++) rr += top[x] < 0 ? h : top[x];
            g.headSide = l < rr ? -1 : 1;
        }
        // body thickness along the length; the tail is a thin part at the far end
        float maxThick = 0;
        for (int x = x0; x <= x1; x++) if (top[x] >= 0) maxThick = Math.max(maxThick, bot[x] - top[x]);
        int tailEnd = g.headSide < 0 ? x1 : x0, step = g.headSide < 0 ? -1 : 1;
        int tailBase = tailEnd;
        for (int x = tailEnd, n = 0; n < width * 0.35f; x += step, n++) {
            if (x < x0 || x > x1 || top[x] < 0) continue;
            if (bot[x] - top[x] > maxThick * 0.45f) { tailBase = x; break; }
        }
        g.tailX = tailBase / (float) w;
        int tb = Math.max(x0, Math.min(x1, tailBase));
        g.tailY = (top[tb] >= 0 ? top[tb] + (bot[tb] - top[tb]) * 0.25f : (y0 + y1) / 2f) / h;
        // where the tail joins: the tail's own pixels just outside the body
        int out = tb - step * 3;
        if (out >= x0 && out <= x1 && top[out] >= 0) {
            int sum = 0, n = 0;
            for (int y = top[out]; y <= bot[out]; y++) if ((r.px[y * w + out] >>> 24) > 128) { sum += y; n++; if (n > Math.max(4, h / 25)) break; }
            if (n > 0) g.tailY = sum / (float) n / h;
        }
        // the head block: the outer 30 % of the length on the head side
        float hx0 = g.headSide < 0 ? x0 : x1 - width * 0.32f, hx1 = g.headSide < 0 ? x0 + width * 0.32f : x1;
        g.headX0 = hx0 / w; g.headX1 = hx1 / w;
        float ht = h, hb = 0;
        for (int x = (int) hx0; x <= (int) hx1 && x <= x1; x++) if (x >= x0 && top[x] >= 0) { ht = Math.min(ht, top[x]); hb = Math.max(hb, bot[x]); }
        g.headTop = ht / h;
        // the legs start where the outline splits into separate legs (rows with gaps near the bottom)
        // (the tail is left out, it hangs beside the legs; a few joined rows at the feet or shadow do not count)
        int lx0 = g.headSide < 0 ? x0 : Math.min(x1, tailBase + 3), lx1 = g.headSide < 0 ? Math.max(x0, tailBase - 3) : x1;
        int legRow = -1, split = 0, solid = 0, need = Math.max(3, (int) ((y1 - y0) * 0.04f));
        for (int y = y1; y > y0 + (y1 - y0) * 0.3f; y--) {
            int runs = 0;
            boolean in = false;
            for (int x = lx0; x <= lx1; x++) {
                boolean o = (r.px[y * w + x] >>> 24) > 128;
                if (o && !in) runs++;
                in = o;
            }
            if (runs >= 2) { split++; solid = 0; if (split >= need) legRow = y; }
            else if (split >= need && ++solid >= need) break;
        }
        g.legTop = (legRow > 0 ? legRow : y0 + (y1 - y0) * 0.62f) / (float) h;
        // a real tail: a thin part joined to the body, attached above the legs
        int filled = 0, cols = Math.abs(tailBase - tailEnd);
        for (int x = Math.min(tailBase, tailEnd); x <= Math.max(tailBase, tailEnd); x++) if (x >= x0 && x <= x1 && top[x] >= 0) filled++;
        g.hasTail = cols >= width * 0.03f && filled >= cols * 0.6f && g.tailY < g.legTop;
        // the legs themselves: opaque runs a little below where they part, grouped into front and back pairs
        int ly = Math.min(y1, (int) ((g.legTop * h + y1) / 2));
        float mid = (x0 + x1) / 2f;
        int fa = -1, fb = -1, ba = -1, bb = -1;
        for (int x = lx0, a0 = -1; x <= lx1 + 1; x++) {
            boolean o = x <= lx1 && (r.px[ly * w + x] >>> 24) > 128;
            if (o && a0 < 0) a0 = x;
            if (!o && a0 >= 0) {
                int b0 = x - 1;
                boolean front = g.headSide < 0 ? (a0 + b0) / 2f < mid : (a0 + b0) / 2f > mid;
                if (front) { if (fa < 0) fa = a0; fb = b0; } else { if (ba < 0) ba = a0; bb = b0; }
                a0 = -1;
            }
        }
        g.belly = g.legTop;
        // the head ends a little below the middle between its top and the legs (the chest and front legs are not head)
        g.headBottom = Math.min(Math.min(g.legTop, hb / h), g.headTop + (g.legTop - g.headTop) * 0.72f);
        float bodyMid = (x0 + x1) / 2f / w;
        g.neckX = g.headSide < 0 ? g.headX1 : g.headX0;
        g.neckY2 = g.headTop + (g.headBottom - g.headTop) * 0.65f;
        g.frontLegX = g.headSide < 0 ? bodyMid - (bodyMid - x0 / (float) w) * 0.55f : bodyMid + (x1 / (float) w - bodyMid) * 0.55f;
        g.backLegX = g.headSide < 0 ? bodyMid + (x1 / (float) w - bodyMid) * 0.5f : bodyMid - (bodyMid - x0 / (float) w) * 0.5f;
        if (fa >= 0) { g.frontLegX = (fa + fb) / 2f / w; g.frontLegHalf = Math.max(0.03f, (fb - fa) / 2f / w); }
        if (ba >= 0) { g.backLegX = (ba + bb) / 2f / w; g.backLegHalf = Math.max(0.03f, (bb - ba) / 2f / w); }
        g.cx = bodyMid;
        g.face = false;      // animals: no separate face crop; the head, jaw and ears carry the feeling
        g.mX = s.mouthX; g.mY = s.mouthY; g.mHW = s.mouthHW;
        float headH = g.headBottom - g.headTop;
        boolean mouthOnHead = s.faceKnown && s.mouthY > g.headTop && s.mouthY < g.headBottom + 0.05f
                && (g.headSide < 0 ? s.mouthX < g.neckX : s.mouthX > g.neckX);
        g.mouthOnHead = mouthOnHead;
        float eyeX = (s.eyeLX + s.eyeRX) / 2, eyeY = (s.eyeLY + s.eyeRY) / 2;
        g.eyesOnHead = s.faceKnown && eyeY > g.headTop - 0.02f && eyeY < g.headBottom
                && eyeX > Math.min(g.headX0, g.headX1) - 0.02f && eyeX < Math.max(g.headX0, g.headX1) + 0.02f;
        // the jaw opens below the mouth line, from a hinge just behind the mouth
        g.jawY = mouthOnHead ? s.mouthY : g.headTop + headH * 0.62f;
        int jr = Math.max(0, Math.min(h - 1, (int) (g.jawY * h)));
        g.jawTipX = g.headSide < 0 ? g.headX0 : g.headX1;
        if (g.headSide < 0) { for (int x = x0; x <= x1; x++) if ((r.px[jr * w + x] >>> 24) > 128) { g.jawTipX = x / (float) w; break; } }
        else for (int x = x1; x >= x0; x--) if ((r.px[jr * w + x] >>> 24) > 128) { g.jawTipX = x / (float) w; break; }
        float headLen = Math.abs(g.headX1 - g.headX0);
        float hinge = g.jawTipX - g.headSide * headLen * 0.4f;     // the jaw is the front of the snout
        g.jawX = Math.max(Math.min(g.headX0, g.headX1), Math.min(Math.max(g.headX0, g.headX1), hinge));
        g.mw = BH; g.mh = BW;
        if (DEBUG) System.out.printf("animal head=%d tail=%.2f,%.2f legTop=%.2f headTop=%.2f neck=%.2f%n", g.headSide, g.tailX, g.tailY, g.legTop, g.headTop, g.neckX);
        return g;
    }

    /** Where one point of an animal picture goes in this frame. */
    private void moveAnimal(Frame f, State s, float x, float y, float[] o) {
        float u = (x - f.L0) / f.W0, v = (y - f.T0) / f.H0;
        float px = x, py = y;
        // breathing: the body swells a little
        float bodyW = (1 - smooth(legTop - 0.05f, legTop, v));
        py = f.T0 + (legTop + (v - legTop) * (1 + 0.015f * s.breathe * bodyW)) * f.H0;
        // head (with jaw and ears): rotates about the neck
        float hd = headSide < 0 ? smooth(neckX + 0.03f, neckX - 0.06f, u) : smooth(neckX - 0.03f, neckX + 0.06f, u);
        hd *= 1 - smooth(headBottom - 0.04f, headBottom + 0.06f, v);
        float qx = px, qy = py;
        if (hd > 0) {
            float nx = f.L0 + neckX * f.W0, ny = f.T0 + neckY2 * f.H0;
            // jaw: the lower front of the head drops open with the voice
            float jx = x, jy = py;
            float headH = (headBottom - headTop);
            float jawZone = smooth(jawY - headH * 0.05f, jawY + headH * 0.08f, v) * (headSide < 0 ? smooth(jawX + 0.02f, jawX - 0.04f, u) : smooth(jawX - 0.02f, jawX + 0.04f, u));
            if (jawZone > 0 && s.jaw > 0) {
                float hingeX = f.L0 + jawX * f.W0, hingeY = f.T0 + jawY * f.H0;
                double a = Math.toRadians(headSide * 16 * s.jaw * jawZone);
                float c = (float) Math.cos(a), sn = (float) Math.sin(a), dx = jx - hingeX, dy = jy - hingeY;
                jx = hingeX + c * dx - sn * dy; jy = hingeY + sn * dx + c * dy;
            }
            // ears: the top of the head twitches (and folds back when angry or afraid)
            float earZone = 1 - smooth(headTop + headH * 0.12f, headTop + headH * 0.3f, v);
            if (earZone > 0) {
                float ex = f.L0 + (headSide < 0 ? (headX0 + headX1) / 2 : (headX0 + headX1) / 2) * f.W0, ey = f.T0 + (headTop + headH * 0.3f) * f.H0;
                double a = Math.toRadians((s.ear * 9 + s.earBack * 20 * -headSide) * earZone);
                float c = (float) Math.cos(a), sn = (float) Math.sin(a), dx = jx - ex, dy = jy - ey;
                jx = ex + c * dx - sn * dy; jy = ey + sn * dx + c * dy;
            }
            double a = Math.toRadians(animalHeadAngle(s));
            float c = (float) Math.cos(a), sn = (float) Math.sin(a), dx = jx - nx, dy = jy - ny;
            float hx = nx + c * dx - sn * dy, hy = ny + sn * dx + c * dy;
            qx += hd * (hx - qx); qy += hd * (hy - qy);
        }
        // tail: wags about its base
        float tl = headSide < 0 ? smooth(tailX - 0.02f, tailX + 0.03f, u) : smooth(tailX + 0.02f, tailX - 0.03f, u);
        if (tl > 0 && hasTail) {
            float tx = f.L0 + tailX * f.W0, ty = f.T0 + tailY * f.H0;
            float reach = Math.min(1, (float) Math.hypot((u - tailX) * f.W0, (v - tailY) * f.H0) / (0.12f * f.W0));
            double a = Math.toRadians(-headSide * s.tail * reach);   // + tail = down
            float c = (float) Math.cos(a), sn = (float) Math.sin(a), dx = qx - tx, dy = qy - ty;
            qx += tl * (tx + c * dx - sn * dy - qx); qy += tl * (ty + sn * dx + c * dy - qy);
        }
        // lying down or sitting: the legs fold under the body
        if (s.sit > 0 && v > legTop) {
            float ly = f.T0 + legTop * f.H0;
            qy = ly + (qy - ly) * (1 - 0.8f * s.sit);
        }
        // legs: front and back pairs swing in turn when walking
        float lg = smooth(legTop - 0.02f, legTop + 0.04f, v);
        if (lg > 0 && s.walkAmt > 0) {
            boolean front = Math.abs(u - frontLegX) < Math.abs(u - backLegX);
            float lh = front ? frontLegHalf : backLegHalf;
            lg *= 1 - smooth(lh + 0.01f, lh + 0.05f, Math.abs(u - (front ? frontLegX : backLegX)));
            float lx = f.L0 + (front ? frontLegX : backLegX) * f.W0, ly = f.T0 + legTop * f.H0;
            float ang = (float) Math.sin(s.walkPhase + (front ? 0 : Math.PI)) * 14 * s.walkAmt;
            double a = Math.toRadians(ang);
            float c = (float) Math.cos(a), sn = (float) Math.sin(a), dx = qx - lx, dy = qy - ly;
            qx += lg * (lx + c * dx - sn * dy - qx); qy += lg * (ly + sn * dx + c * dy - qy);
        }
        o[0] = qx;
        o[1] = qy;
    }
}
