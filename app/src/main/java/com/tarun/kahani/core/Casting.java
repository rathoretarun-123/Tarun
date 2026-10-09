package com.tarun.kahani.core;

import java.util.List;

/**
 * The director's choice of picture for every character in every shot (v27): among the user's own pictures of the
 * character (the sheet's angles, poses and expressions, read by PoseSense or tagged by the user), the one that
 * shows the angle the camera needs, the pose the body is in and the feeling of the moment — decided once per
 * shot, never per frame (a picture that changed between frames would read as a shake), recorded in the shot list
 * ("PICTURES USED") and drawn as it is. The front picture with its rig is the fallback: it is the only one that
 * can bend; a real picture is never bent.
 */
public final class Casting {
    private Casting() {}

    /** Index meaning "the front picture (rigged)". */
    public static final int MAIN = -1;

    /** What a shot asks of a character's picture. */
    public static final class Want {
        public float angle = Angles.FRONT;
        public int pose = PoseSense.STAND, emotion = PoseSense.NEUTRAL;
        public boolean speaking, ots;
        public String toString() { return Angles.name(angle) + " " + PoseSense.poseName(pose) + " " + PoseSense.emotionName(emotion) + (speaking ? " (speaking)" : ""); }
    }

    /** Decides the pictures of every shot of the film. */
    public static void cast(Art art, Film film, Story story) {
        if (art == null || film == null) return;
        for (Film.Shot sh : film.shots) {
            sh.pictures.clear();
            sh.cycles.clear();
            Film.Seg s = film.segAt(sh.t + 0.01f);
            if (s == null) continue;
            float mid = sh.t + Math.max(0.05f, Math.min(sh.dur * 0.5f, 1.2f));
            for (Film.Actor a : s.actors) {
                Film.Key k = a.stateAt(mid);
                if (!k.visible) continue;
                Art.Sprite sp = art.sprites.get(a.c.id);
                if (sp == null || sp.poses == null || sp.poses.isEmpty()) continue;
                Want w = want(film, s, sh, a, mid);
                int best = choose(sp, w);
                // v33: a beast (fur, four legs, a monster) is never bent through the rig: its front picture is drawn as it is
                boolean beast = a.c.look != null && (a.c.look.kind == Look.ANIMAL || a.c.look.kind == Look.BIRD || a.c.look.kind == Look.MONSTER || a.c.look.kind == Look.MONKEY);
                if (best == MAIN && beast) {
                    for (int i = 0; i < sp.poses.size(); i++) {
                        Art.PoseSprite p = sp.poses.get(i);
                        if (isFrontish(p.angle) && p.pose == PoseSense.STAND && (p.emotion == PoseSense.NEUTRAL || p.emotion == w.emotion)) { best = i; if (sameAngle(p.angle, Angles.FRONT)) break; }
                    }
                }
                sh.pictures.put(a.c.id, best);
                if (best >= 0 && (w.pose == PoseSense.WALK || w.pose == PoseSense.RUN)) {
                    int[] cyc = stepCycle(sp, best, w);
                    if (cyc != null) sh.cycles.put(a.c.id, cyc);
                }
            }
        }
    }

    /** What the shot asks of this character at its middle. */
    public static Want want(Film film, Film.Seg s, Film.Shot sh, Film.Actor a, float t) {
        Want w = new Want();
        Film.Key k = a.stateAt(t);
        Film.Key mv = null;
        for (Film.Key kk : a.keys) if (kk.moveDur > 0 && t >= kk.t && t < kk.t + kk.moveDur) mv = kk;
        // the angle: the back for the shoulder in the foreground, the side while walking, the three-quarter in a
        // two-shot turned to the other, the front otherwise
        if (sh.ots.length() > 0 && sh.ots.equals(a.c.shown())) { w.angle = Angles.BACK; w.ots = true; }
        else if (mv != null && k.body == Pose.STAND && k.anchor == Film.A_GROUND) w.angle = Angles.SIDE;
        else if (sh.type == ShotPlanner.TWO_SHOT && k.anchor == Film.A_GROUND) {
            for (Film.Actor o : s.actors) {
                if (o == a || !o.stateAt(t).visible || o.stateAt(t).anchor != Film.A_GROUND) continue;
                float dx = Director.xAt(o, t) - Director.xAt(a, t);
                if (Math.abs(dx) >= 40 && Math.abs(dx) <= 900 && Math.signum(dx) == Math.signum(k.facing)) { w.angle = Angles.THREE_QUARTER; break; }
            }
        }
        // the pose: the body's state, the gesture of the moment, the walk
        switch (k.body) {
            case Pose.SIT: case Pose.KNEEL: w.pose = PoseSense.SIT; break;
            case Pose.LIE: w.pose = PoseSense.LIE; break;
            case Pose.CROUCH: w.pose = PoseSense.CROUCH; break;
            default: w.pose = mv != null ? (mv.run ? PoseSense.RUN : PoseSense.WALK) : PoseSense.STAND;
        }
        if (w.pose == PoseSense.STAND) for (Film.Act ac : a.acts) {
            if (ac.t1 <= sh.t || ac.t0 >= sh.t + sh.dur) continue;
            switch (ac.type) {
                case Film.G_SWORD: case Film.G_BLOCK: w.pose = PoseSense.FIGHT; break;
                case Film.G_POINT: w.pose = PoseSense.POINT; break;
                case Film.G_WAVE: case Film.G_SALUTE: w.pose = PoseSense.WAVE; break;
                case Film.G_CLAP: case Film.G_DANCE: case Film.G_JUMP: case Film.G_FLAIL: case Film.G_BOUNCE: w.pose = PoseSense.ARMS_UP; break;
                default:
            }
        }
        // the feeling: the line being spoken, else the key's emotion
        int emo = k.emotion;
        for (Film.Speak spk : a.speaks) if (t >= spk.t0 && t < spk.t1) { w.speaking = true; if (spk.emotion != Pose.NEUTRAL) emo = spk.emotion; }
        if (!w.speaking) for (Film.Speak spk : a.speaks) if (spk.t0 < sh.t + sh.dur && spk.t1 > sh.t) { w.speaking = true; if (spk.emotion != Pose.NEUTRAL) emo = spk.emotion; }
        w.emotion = PoseSense.groupOf(emo);
        if (k.eyesShut && w.pose == PoseSense.LIE) w.emotion = PoseSense.ASLEEP;
        return w;
    }

    /**
     * v28: the pictures of the character's steps at this angle — the chosen one first, then its other walking
     * and running pictures, then a standing one of the same angle (up to four) — shown one per step, so a walk
     * is a walk and not a picture gliding; null when there is only the one.
     */
    public static int[] stepCycle(Art.Sprite sp, int chosen, Want w) {
        List<Art.PoseSprite> ps = sp.poses;
        if (ps == null || chosen < 0 || chosen >= ps.size()) return null;
        java.util.List<Integer> out = new java.util.ArrayList<Integer>();
        out.add(chosen);
        float angle = ps.get(chosen).angle;
        for (int pass = 0; pass < 2 && out.size() < 4; pass++) {
            for (int i = 0; i < ps.size() && out.size() < 4; i++) {
                if (out.contains(i)) continue;
                Art.PoseSprite p = ps.get(i);
                if (!sameAngle(p.angle, angle)) continue;
                boolean step = p.pose == PoseSense.WALK || p.pose == PoseSense.RUN;
                if (pass == 0 ? !step : p.pose != PoseSense.STAND) continue;
                if (p.emotion != PoseSense.NEUTRAL && p.emotion != w.emotion && p.emotion != ps.get(chosen).emotion) continue;
                out.add(i);
            }
        }
        if (out.size() < 2) return null;
        int[] cyc = new int[out.size()];
        for (int i = 0; i < cyc.length; i++) cyc[i] = out.get(i);
        return cyc;
    }

    /** The best of the character's pictures for the want, or MAIN. */
    public static int choose(Art.Sprite sp, Want w) {
        List<Art.PoseSprite> ps = sp.poses;
        if (ps == null || ps.isEmpty()) return MAIN;
        // the front picture's own score: the front angle, standing, neutral, and a little for the rig that can bend
        // and gesture (a picture of the same angle, pose and feeling never beats it; one of the moment's feeling does)
        // v33: the rig's small edge only while it must speak (the mouth moves on the front picture); otherwise the
        // user's own picture of the moment wins whenever it fits as well
        float mainScore = score(Angles.FRONT, PoseSense.STAND, PoseSense.NEUTRAL, sp.faceKnown, w) + (w.speaking && sp.faceKnown ? 0.3f : 0f);
        int best = MAIN; float bestScore = mainScore;
        for (int i = 0; i < ps.size(); i++) {
            Art.PoseSprite p = ps.get(i);
            float sc = score(p.angle, p.pose, p.emotion, p.faceKnown(), w);
            if (sc > bestScore) { bestScore = sc; best = i; }
        }
        return best;
    }

    static float score(float angle, int pose, int emotion, boolean face, Want w) {
        float s = 0;
        // angle
        if (sameAngle(angle, w.angle)) s += 3;
        else if (isFrontish(angle) && isFrontish(w.angle)) s += 2.2f;                          // a three-quarter for a front: nearly as good
        else if (w.ots) return -100;
        else s -= 5;
        // pose
        if (pose == w.pose) s += 2;
        else if (lowPose(pose) != lowPose(w.pose) || pose == PoseSense.LIE || w.pose == PoseSense.LIE) s -= 4;     // sitting for standing, or lying for anything else: never
        else if (pose == PoseSense.STAND && (w.pose == PoseSense.WALK || w.pose == PoseSense.RUN)) s += 0.4f;      // a standing side picture can glide
        else if (w.pose == PoseSense.STAND && (pose == PoseSense.POINT || pose == PoseSense.WAVE)) s += 0.3f;
        else if ((pose == PoseSense.WALK && w.pose == PoseSense.RUN) || (pose == PoseSense.RUN && w.pose == PoseSense.WALK)) s += 1.2f;
        else s -= 1;
        // feeling
        if (emotion == w.emotion) s += 3;                                                       // the feeling of the moment: the reason to cut to a real picture
        else if (emotion == PoseSense.NEUTRAL) s += 0.7f;
        else if (w.emotion == PoseSense.NEUTRAL && emotion == PoseSense.HAPPY) s += 0.4f;
        else if ((emotion == PoseSense.HAPPY && w.emotion == PoseSense.LAUGH) || (emotion == PoseSense.LAUGH && w.emotion == PoseSense.HAPPY)) s += 1.3f;
        else s -= 2;                                                     // a crying picture for a laugh: never
        // speaking: the mouth must be there to move
        if (w.speaking && !(face && isFrontish(angle))) s -= 10;
        return s;
    }

    static boolean sameAngle(float a, float b) { return Math.abs(Math.abs(a) - Math.abs(b)) < 1; }
    static boolean isFrontish(float a) { return sameAngle(a, Angles.FRONT) || sameAngle(a, Angles.THREE_QUARTER); }
    static boolean lowPose(int p) { return p == PoseSense.SIT || p == PoseSense.CROUCH; }

    /** The shot list's line: who is drawn with which picture. */
    public static String describe(Art art, Film film, Film.Shot sh) {
        if (art == null || film == null || sh.pictures.isEmpty()) return "";
        Film.Seg s = film.segAt(sh.t + 0.01f);
        if (s == null) return "";
        StringBuilder b = new StringBuilder();
        for (Film.Actor a : s.actors) {
            Integer idx = sh.pictures.get(a.c.id);
            if (idx == null) continue;
            Art.Sprite sp = art.sprites.get(a.c.id);
            String what;
            if (idx < 0 || sp == null || sp.poses == null || idx >= sp.poses.size()) what = "the front picture (rigged)";
            else {
                Art.PoseSprite p = sp.poses.get(idx);
                what = "your picture " + (idx + 1) + " (" + Angles.name(p.angle) + ", " + PoseSense.poseName(p.pose) + ", " + PoseSense.emotionName(p.emotion) + ")";
                int[] cyc = sh.cycles.get(a.c.id);
                if (cyc != null) { StringBuilder c = new StringBuilder(); for (int k : cyc) c.append(c.length() > 0 ? ", " : "").append(k + 1); what += " stepping through pictures " + c; }
            }
            b.append(b.length() > 0 ? "; " : "").append(a.c.shown()).append(" ← ").append(what);
        }
        return b.toString();
    }
}
