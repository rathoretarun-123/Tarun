package com.tarun.kahani.core;

/**
 * What a cut-out figure shows (v27): the pose (standing, walking, running, sitting, lying, crouching, arms up,
 * waving, pointing) read from its silhouette, and the emotion (happy, laughing, sad, angry, surprised, thinking,
 * asleep) read from its face — so the director can pick, for every shot, the user's own picture of that pose
 * and feeling instead of bending the front picture into it. Every measure is a fraction of the figure, so the
 * reading does not depend on the picture's size.
 */
public final class PoseSense {
    private PoseSense() {}

    public static final int STAND = 0, WALK = 1, RUN = 2, SIT = 3, LIE = 4, CROUCH = 5, ARMS_UP = 6, WAVE = 7, POINT = 8, FIGHT = 9;
    public static final int NEUTRAL = 0, HAPPY = 1, LAUGH = 2, SAD = 3, ANGRY = 4, SURPRISED = 5, THINK = 6, ASLEEP = 7;

    /** What was read of one figure. */
    public static final class Tag {
        public float angle = Angles.FRONT;
        public int pose = STAND, emotion = NEUTRAL;
        public M m;
        public String toString() { return Angles.name(angle) + " " + poseName(pose) + " " + emotionName(emotion); }
    }

    /** The measures the reading is made from (fractions of the figure's height, width or face). */
    public static final class M {
        public float aspect, hRatio, feet, spanLow, lean, armsL, armsR, reachL, reachR, lowMass, topMass;
        public boolean face;
        public float eyeDark, skin, tears, mouthOpen, mouthWide, mouthDark, browSlant, handAtFace, faceH, hairAbove;
        public String toString() {
            return String.format(java.util.Locale.US, "asp=%.2f hR=%.2f feet=%.2f span=%.2f lean=%.2f arms=%.2f/%.2f reach=%.2f/%.2f low=%.2f | skin=%.2f hairAb=%.2f eyeDark=%.2f tears=%.3f mouth=%.2fx%.2f dark=%.3f hand=%.3f",
                    aspect, hRatio, feet, spanLow, lean, armsL, armsR, reachL, reachR, lowMass, skin, hairAbove, eyeDark, tears, mouthOpen, mouthWide, mouthDark, handAtFace);
        }
    }

    public static String poseName(int p) {
        switch (p) { case WALK: return "walking"; case RUN: return "running"; case SIT: return "sitting"; case LIE: return "lying"; case CROUCH: return "crouching"; case ARMS_UP: return "arms up"; case WAVE: return "waving"; case POINT: return "pointing"; case FIGHT: return "fighting"; default: return "standing"; }
    }

    public static String emotionName(int e) {
        switch (e) { case HAPPY: return "happy"; case LAUGH: return "laughing"; case SAD: return "sad"; case ANGRY: return "angry"; case SURPRISED: return "surprised"; case THINK: return "thinking"; case ASLEEP: return "asleep"; default: return "neutral"; }
    }

    /** The film's emotion (Pose.*) this reading stands for. */
    public static int filmEmotion(int e) {
        switch (e) { case HAPPY: return Pose.HAPPY; case LAUGH: return Pose.LAUGH; case SAD: return Pose.SAD; case ANGRY: return Pose.ANGRY; case SURPRISED: return Pose.SURPRISED; case THINK: return Pose.CURIOUS; case ASLEEP: return Pose.DIZZY; default: return Pose.NEUTRAL; }
    }

    /** The reading's emotion group of a film emotion (Pose.*). */
    public static int groupOf(int filmEmotion) {
        switch (filmEmotion) {
            case Pose.HAPPY: case Pose.PROUD: case Pose.RELIEVED: return HAPPY;
            case Pose.LAUGH: return LAUGH;
            case Pose.SAD: case Pose.PAIN: return SAD;
            case Pose.ANGRY: case Pose.EVIL: case Pose.DETERMINED: return ANGRY;
            case Pose.SCARED: case Pose.SURPRISED: return SURPRISED;
            case Pose.CURIOUS: case Pose.SUSPICIOUS: case Pose.WHISPER: return THINK;
            case Pose.DIZZY: return ASLEEP;
            default: return NEUTRAL;
        }
    }

    static float lum(int c) { return 0.299f * ((c >> 16) & 255) + 0.587f * ((c >> 8) & 255) + 0.114f * (c & 255); }
    static boolean on(int c) { return (c >>> 24) > 100; }

    /** The measures of a cut-out (its face found by Cutout.process). standH: the height of a standing figure of the same character (0 = unknown). */
    public static M measure(Cutout.Result r, int standH) {
        M m = new M();
        int w = r.w, h = r.h;
        int[] px = r.px;
        if (w < 4 || h < 4 || px == null) return m;
        m.aspect = w / (float) h;
        m.hRatio = standH > 0 ? h / (float) standH : 0;
        // the silhouette's rows: span and centroid
        int[] x0 = new int[h], x1 = new int[h], cnt = new int[h];
        long total = 0;
        for (int y = 0; y < h; y++) {
            int a = -1, b = -1, c = 0;
            for (int x = 0; x < w; x++) if (on(px[y * w + x])) { if (a < 0) a = x; b = x; c++; }
            x0[y] = a; x1[y] = b; cnt[y] = c; total += c;
        }
        if (total == 0) return m;
        // mass in the lowest / highest 30% of the rows
        long low = 0, top = 0;
        for (int y = 0; y < h; y++) { if (y >= h * 0.7f) low += cnt[y]; if (y < h * 0.3f) top += cnt[y]; }
        m.lowMass = low / (float) total; m.topMass = top / (float) total;
        // the widest span in the lowest 30% (legs apart, a stride)
        int best = 0;
        for (int y = (int) (h * 0.7f); y < h; y++) if (x0[y] >= 0) best = Math.max(best, x1[y] - x0[y] + 1);
        m.spanLow = best / (float) h;
        // the ground contacts: in the lowest 8% of the rows, the runs of opaque pixels; the outermost two centres
        float feet = 0;
        for (int y = (int) (h * 0.92f); y < h; y++) {
            int runs = 0, firstC = -1, lastC = -1, start = -1;
            for (int x = 0; x <= w; x++) {
                boolean o = x < w && on(px[y * w + x]);
                if (o && start < 0) start = x;
                if (!o && start >= 0) { int c = (start + x - 1) / 2; if (x - start >= Math.max(2, h * 0.015f)) { runs++; if (firstC < 0) firstC = c; lastC = c; } start = -1; }
            }
            if (runs >= 2) feet = Math.max(feet, (lastC - firstC) / (float) h);
        }
        m.feet = feet;
        // the lean: the centroid of the upper third against the lower third
        double cxTop = 0, cxLow = 0; long nTop = 0, nLow = 0;
        for (int y = 0; y < h; y++) {
            if (cnt[y] == 0) continue;
            double cx = (x0[y] + x1[y]) / 2.0;
            if (y < h / 3) { cxTop += cx * cnt[y]; nTop += cnt[y]; }
            else if (y >= h * 2 / 3) { cxLow += cx * cnt[y]; nLow += cnt[y]; }
        }
        if (nTop > 0 && nLow > 0) m.lean = (float) ((cxTop / nTop - cxLow / nLow) / h);
        // the face and what hangs on it
        m.face = r.faceFound;
        float fw = Math.max(0.05f, (r.eyeRX - r.eyeLX) / 0.4f) * w;         // the face's width in pixels
        float cx = r.mouthX * w, eyeY = r.eyeY * h, chin = r.chinY * h, faceTop = r.faceTop * h;
        float fh = Math.max(4, chin - faceTop);
        m.faceH = fh / h;
        if (r.faceFound) {
            m.eyeDark = Angles.eyeDarkness(r);
            m.hairAbove = (r.faceTop - r.headTop) / Math.max(0.01f, r.chinY - r.faceTop);
            float inter = Math.max(0.02f, r.eyeRX - r.eyeLX);
            m.skin = Angles.skinShare(r, r.eyeLX - inter * 0.5f, r.eyeY - inter * 0.3f, r.eyeRX + inter * 0.5f, r.mouthY + inter * 0.25f);
        }
        // arms raised: pixels above the eyes well beyond the head (a wave, a cheer, a hand on the head), in face areas
        float headHalf = fw * 1.5f;
        long aL = 0, aR = 0;
        int eyeRow = r.faceFound ? (int) eyeY : (int) (h * 0.12f);
        for (int y = 0; y < eyeRow; y++) for (int x = 0; x < w; x++) {
            if (!on(px[y * w + x])) continue;
            if (x < cx - headHalf) aL++; else if (x > cx + headHalf) aR++;
        }
        float faceArea = Math.max(1, fw * fh);
        m.armsL = aL / faceArea; m.armsR = aR / faceArea;
        // a reach sideways at the torso's height (an arm pointing, a sword out): the widest row against the body's usual width there
        int t0 = (int) (h * 0.3f), t1 = (int) (h * 0.62f);
        java.util.List<Integer> spans = new java.util.ArrayList<Integer>();
        for (int y = t0; y < t1; y++) if (x0[y] >= 0) spans.add(x1[y] - x0[y] + 1);
        if (spans.size() >= 4) {
            java.util.Collections.sort(spans);
            float body = spans.get(spans.size() / 2);
            float rl = 0, rr = 0;
            for (int y = t0; y < t1; y++) {
                if (x0[y] < 0) continue;
                float left = cx - x0[y], right = x1[y] - cx;
                rl = Math.max(rl, left - body * 0.5f); rr = Math.max(rr, right - body * 0.5f);
            }
            m.reachL = rl / w; m.reachR = rr / w;
        }
        if (!r.faceFound) return m;
        // tears: light blue or white drops on the cheeks (inside the face, below the eyes, above the chin)
        long tear = 0, cheek = 0;
        for (int y = (int) (eyeY + fh * 0.1f); y < (int) (eyeY + fh * 0.55f) && y < h; y++) for (int x = Math.max(0, (int) (cx - fw * 0.55f)); x < Math.min(w, (int) (cx + fw * 0.55f)); x++) {
            int c = px[y * w + x];
            if (!on(c)) continue;
            float dx = Math.abs(x - cx);
            if (dx < fw * 0.1f) continue;
            cheek++;
            int rr = (c >> 16) & 255, gg = (c >> 8) & 255, bb = c & 255;
            if (bb >= 165 && bb >= rr + 25 && bb >= gg - 5 && lum(c) >= 140) tear++;
        }
        m.tears = cheek == 0 ? 0 : tear / (float) cheek;
        // the mouth: the dark inside (an open mouth), the red of lips and tongue, teeth — just above the chin
        int my0 = Math.max(0, (int) (chin - fh * 0.4f)), my1 = Math.min(h, (int) (chin + fh * 0.06f));
        int mx0 = Math.max(0, (int) (cx - fw * 0.34f)), mx1 = Math.min(w, (int) (cx + fw * 0.34f));
        int bx0 = w, bx1 = -1, by0 = h, by1 = -1; long dark = 0;
        for (int y = my0; y < my1; y++) {
            // the mouth lies inside the skin of the face: from the face's middle outwards, through skin and mouth
            // colours only — the first hair, cloth or background pixel ends the face on that side
            int cxi = Math.max(mx0, Math.min(mx1 - 1, (int) cx));
            int l = cxi, rgt = cxi;
            while (l > mx0 && faceOrMouth(px[y * w + l - 1])) l--;
            while (rgt < mx1 - 1 && faceOrMouth(px[y * w + rgt + 1])) rgt++;
            for (int x = l; x <= rgt; x++) {
                int c = px[y * w + x];
                if (!mouthPixel(c)) continue;
                dark++;
                if (x < bx0) bx0 = x; if (x > bx1) bx1 = x; if (y < by0) by0 = y; if (y > by1) by1 = y;
            }
        }
        if (bx1 >= bx0) { m.mouthWide = (bx1 - bx0 + 1) / fw; m.mouthOpen = (by1 - by0 + 1) / fh; }
        m.mouthDark = dark / (fw * fh);
        // the brows: the dark strokes above each eye — their inner end lower than the outer (anger) or higher (sorrow)
        float slant = 0; int nb = 0;
        for (int e = 0; e < 2; e++) {
            float ex = (e == 0 ? r.eyeLX : r.eyeRX) * w;
            int by0b = Math.max(0, (int) (eyeY - fh * 0.24f)), by1b = (int) (eyeY - fh * 0.05f);
            double inY = 0, outY = 0; long nIn = 0, nOut = 0;
            for (int y = by0b; y < by1b; y++) for (int x = Math.max(0, (int) (ex - fw * 0.2f)); x < Math.min(w, (int) (ex + fw * 0.2f)); x++) {
                int c = px[y * w + x];
                if (!on(c) || lum(c) >= 75) continue;
                boolean inner = e == 0 ? x > ex : x < ex;
                if (inner) { inY += y; nIn++; } else { outY += y; nOut++; }
            }
            if (nIn > 3 && nOut > 3) { slant += (float) ((inY / nIn - outY / nOut) / fh); nb++; }
        }
        m.browSlant = nb == 0 ? 0 : slant / nb;
        // a hand at the face: skin beside the lower face, outside it
        long hand = 0;
        for (int y = (int) (eyeY + fh * 0.2f); y < Math.min(h, (int) (chin + fh * 0.35f)); y++) for (int x = 0; x < w; x++) {
            float dx = Math.abs(x - cx);
            if (dx < fw * 0.6f || dx > fw * 1.4f) continue;
            if (Cutout.isSkin(px[y * w + x])) hand++;
        }
        m.handAtFace = hand / (fw * fh);
        return m;
    }

    /** The dark inside of an open mouth (not a warm dark: that is skin or fur), or the red of lips and a tongue. */
    static boolean mouthPixel(int c) {
        if (!on(c)) return false;
        int rr = (c >> 16) & 255, gg = (c >> 8) & 255, bb = c & 255;
        boolean inside = lum(c) < 62 && !(rr > 90 && rr > gg + 20);
        boolean red = rr > 150 && gg < 115 && bb < 115 && rr > gg + 60;
        boolean teeth = rr > 225 && gg > 225 && bb > 215;
        return inside || red || teeth;
    }

    static boolean faceOrMouth(int c) { return on(c) && (Cutout.isSkin(c) || mouthPixel(c)); }

    /** The reading of a cut-out figure. standH: the height of a standing figure of the same character (0 = unknown). */
    public static Tag tag(Cutout.Result r, int standH, boolean beast) {
        Tag t = new Tag();
        t.angle = Angles.guess(r, beast);
        M m = measure(r, standH);
        t.m = m;
        // ---- the pose, from the silhouette
        boolean sideways = t.angle == Angles.SIDE || t.angle == Angles.THREE_QUARTER;
        boolean low = m.hRatio > 0 ? m.hRatio < 0.86f && m.lowMass >= 0.32f : m.lowMass > 0.5f;   // a low, bottom-heavy figure sits or crouches (crossed legs spread the "feet": still a seat)
        float reach = Math.max(m.reachL, m.reachR);
        if (m.aspect > 1.25f && (m.hRatio == 0 || m.hRatio < 0.8f)) t.pose = LIE;
        else if (low) t.pose = m.lean > 0.1f || m.aspect < 0.62f ? CROUCH : SIT;
        else if (reach >= 0.35f && m.feet >= 0.4f) t.pose = FIGHT;
        else if (m.feet >= 0.3f || (m.spanLow >= 0.55f && Math.abs(m.lean) >= 0.05f)) t.pose = RUN;
        else if (sideways && (m.feet >= 0.18f || m.spanLow >= 0.44f)) t.pose = WALK;
        else if (m.armsL >= 0.25f && m.armsR >= 0.25f) t.pose = ARMS_UP;
        else if (m.armsL >= 0.4f || m.armsR >= 0.4f) t.pose = WAVE;
        else if (reach >= 0.22f && Math.min(m.reachL, m.reachR) < 0.1f && !sideways) t.pose = POINT;
        // ---- the emotion, from the face (front or three-quarter only)
        if (!m.face || t.angle == Angles.BACK) { if (t.pose == LIE) t.emotion = ASLEEP; return t; }
        boolean eyesShut = m.eyeDark > 0.42f && m.skin >= 0.42f;
        // a mouth box that fills its scan limits is hair or clothing, not a mouth: no evidence of an open mouth then
        boolean open = m.mouthOpen >= 0.16f && m.mouthOpen < 0.42f && m.mouthWide >= 0.25f && m.mouthWide < 0.56f && m.mouthDark >= 0.015f;
        if (t.pose == LIE && eyesShut) t.emotion = ASLEEP;
        else if (m.tears >= 0.012f) t.emotion = SAD;
        else if (open && eyesShut) t.emotion = LAUGH;
        else if (open && m.mouthOpen >= m.mouthWide * 0.85f && m.mouthWide < 0.42f) t.emotion = SURPRISED;
        else if (open && m.mouthWide >= 0.42f) t.emotion = LAUGH;
        else if (m.handAtFace >= 0.08f && !beast && !sideways) t.emotion = THINK;
        else if (eyesShut && !sideways) t.emotion = HAPPY;
        else t.emotion = NEUTRAL;
        return t;
    }
}
