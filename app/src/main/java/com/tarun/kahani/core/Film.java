package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;

/** The director's complete shooting plan: what is on screen and heard at every moment. */
public final class Film {
    public Story story;
    public float duration;
    public final List<Seg> segs = new ArrayList<Seg>();
    public final List<Line> lines = new ArrayList<Line>();
    public final List<Sfx> sfx = new ArrayList<Sfx>();
    public final List<Music> music = new ArrayList<Music>();
    public final List<String> notes = new ArrayList<String>();   // director's notes for the analysis screen
    public Object titleImage, endImage;                           // user pictures (platform images) or null
    public boolean subtitles = true;

    // -------------------------------------------------------------- segments
    public static final int S_TITLE = 0, S_CARD = 1, S_SCENE = 2, S_END = 3;

    public static final class Seg {
        public int type;
        public float t0, t1;
        public int set, tod;
        public int scene = -1;
        public String text1 = "", text2 = "";
        public final List<Actor> actors = new ArrayList<Actor>();
        public final List<Fx> fx = new ArrayList<Fx>();
        public final List<Cam> cams = new ArrayList<Cam>();
        public final List<Sub> subs = new ArrayList<Sub>();
        public float fadeIn = 0.5f, fadeOut = 0.5f;
        public Art.Backdrop backdrop;     // user picture for this set, or null for the painted set
        public float ground = Sets.GROUND; // y of the floor line in stage coordinates
        public boolean festive;           // celebration lights
        public int mood = -1;
        public Actor find(Story.CharacterDef c) {
            for (Actor a : actors) if (a.c == c) return a;
            return null;
        }
    }

    // -------------------------------------------------------------- actors
    public static final int A_GROUND = 0, A_BRANCH = 1, A_SHOULDER = 2, A_CARRIED = 3, A_HIDDEN = 4, A_ROCK_HIDE = 5,
            A_ON_FACE = 6, A_CAGE = 7;

    public static final class Key {
        public float t, moveDur;
        public float x, depth;
        public int body = Pose.STAND;
        public float facing = 1;
        public int emotion = Pose.NEUTRAL;
        public int anchor = A_GROUND;
        public Actor anchorActor;
        public boolean visible = true, run, disguised, noHeadwear, wearsTurban, redFace, tears, netted, sweat, eyesShut;
        public int holdR = Pose.I_NONE, holdL = Pose.I_NONE;
        public Key copy() {
            Key k = new Key();
            k.t = t; k.moveDur = 0; k.x = x; k.depth = depth; k.body = body; k.facing = facing; k.emotion = emotion; k.anchor = anchor;
            k.anchorActor = anchorActor; k.visible = visible; k.run = false; k.disguised = disguised; k.noHeadwear = noHeadwear;
            k.wearsTurban = wearsTurban; k.redFace = redFace; k.tears = tears; k.netted = netted; k.sweat = sweat; k.eyesShut = eyesShut;
            k.holdR = holdR; k.holdL = holdL;
            return k;
        }
    }

    public static final int G_TALK = 0, G_CLAP = 1, G_LAUGH = 2, G_SWORD = 3, G_WAND = 4, G_POINT = 5, G_SALUTE = 6, G_HUG = 7,
            G_CRY = 8, G_TREMBLE = 9, G_CRUSH = 10, G_DANCE = 11, G_PAT = 12, G_GIVE = 13, G_RUB_EYES = 14, G_FLAIL = 15,
            G_PUSH = 16, G_PULL_ROPE = 17, G_TWIRL = 18, G_HOLD_HEAD = 19, G_REACH = 20, G_FIST = 21, G_SCRATCH = 22,
            G_JUMP = 23, G_SHAKE_HEAD = 24, G_CLUTCH = 25, G_WALK_PLACE = 26, G_BOUNCE = 27, G_MIRROR = 28, G_THROW = 29,
            G_SPLASH = 30, G_WIGGLE = 31, G_COUGH = 32, G_PLAY = 33, G_LOOK_UP = 34, G_WHISPER = 35, G_PROUD = 36,
            G_ROAR = 37, G_BLOCK = 38, G_STEP_BACK = 39, G_OFFER = 40, G_HOLD_HAND = 41;

    public static final class Act {
        public float t0, t1;
        public int type;
        public Actor target;
        public Act(float t0, float t1, int type) { this.t0 = t0; this.t1 = t1; this.type = type; }
    }

    public static final class Speak {
        public float t0, t1;
        public int line;
        public int emotion;
    }

    public static final class Actor {
        public Story.CharacterDef c;
        public Look look;
        public final List<Key> keys = new ArrayList<Key>();
        public final List<Act> acts = new ArrayList<Act>();
        public final List<Speak> speaks = new ArrayList<Speak>();
        public int order;
        public Key last() { return keys.get(keys.size() - 1); }
        /** Starts a new key at time t copying the current state. */
        public Key at(float t) {
            Key l = last();
            if (Math.abs(l.t - t) < 0.001f && l.moveDur == 0) return l;
            Key k = l.copy();
            k.t = t;
            keys.add(k);
            return k;
        }
        public Key stateAt(float t) {
            Key r = keys.get(0);
            for (Key k : keys) { if (k.t <= t) r = k; else break; }
            return r;
        }
    }

    // -------------------------------------------------------------- effects
    public static final int FX_BLOOM = 0, FX_SMOKE = 1, FX_BEAM = 2, FX_BELL = 3, FX_NET = 4, FX_SPARKLE = 5, FX_BUTTERFLY = 6,
            FX_FLASH = 7, FX_SPLASH = 8, FX_RIBBON = 9, FX_CAGE = 10, FX_LEAVES = 11, FX_GREEN_GLOW = 12, FX_SHAKE = 13,
            FX_MAGIC_FLOWER = 14, FX_HIDE_ROCK = 15, FX_LADDOO_GLOW = 16, FX_FIREWORKS = 17, FX_DUST = 18, FX_BOULDER = 19, FX_SHOT = 20, FX_TITLE_SPARKS = 21;

    public static final class Fx {
        public float t0, t1;
        public int type;
        public float x, y;
        public Actor a, b;
        public int color;
        public float t2;                  // secondary moment (e.g. bud turns into flower)
        public Art.Backdrop pic;          // full-screen cinematic picture (FX_SHOT)
        public Fx(int type, float t0, float t1) { this.type = type; this.t0 = t0; this.t1 = t1; }
    }

    public static final class Cam {
        public float t, cx, cy, zoom, ease;   // ease 0 = cut
        public Cam(float t, float cx, float cy, float zoom, float ease) { this.t = t; this.cx = cx; this.cy = cy; this.zoom = zoom; this.ease = ease; }
    }

    public static final class Sub {
        public float t0, t1;
        public String who, text;
    }

    // -------------------------------------------------------------- audio
    public static final class Line {
        public int index;
        public Story.CharacterDef who;   // null = narrator
        public String text;              // what is spoken
        public String shown;             // what is shown as subtitle
        public int emotion;
        public boolean whisper, echo;
        public float dur;                // seconds, filled after synthesis
        public float[] env;              // mouth openness envelope, 100 Hz
        public float start;              // placed on timeline
        public float gain = 1f;
    }

    public static final int SFX_STREAM = 0, SFX_BIRDS = 1, SFX_WIND = 2, SFX_CLACK = 3, SFX_POP = 4, SFX_CHIME = 5, SFX_MONKEY = 6,
            SFX_RUSTLE = 7, SFX_THUD = 8, SFX_WHOOSH = 9, SFX_BELL = 10, SFX_DRUMS = 11, SFX_NIGHT = 12, SFX_ROAR = 13, SFX_CLAP = 14,
            SFX_ANKLET = 15, SFX_HISS = 16, SFX_DRIP = 17, SFX_SCREAM_FX = 18, SFX_SPLASH = 19, SFX_NET = 20, SFX_WHOOSH_CARD = 21,
            SFX_FANFARE = 22, SFX_END_CHORD = 23, SFX_MAGIC = 24, SFX_STEPS = 25, SFX_CROWD = 26, SFX_GLASS = 27, SFX_SWORD = 28;

    public static final class Sfx {
        public float t, dur, gain;
        public int type;
        public Sfx(int type, float t, float dur, float gain) { this.type = type; this.t = t; this.dur = dur; this.gain = gain; }
    }

    public static final int M_TITLE = 0, M_HAPPY = 1, M_TENSE = 2, M_VILLAIN = 3, M_SAD = 4, M_ACTION = 5, M_CELEBRATE = 6,
            M_END = 7, M_NIGHT = 8, M_PLAYFUL = 9;

    public static final class Music {
        public float t0, t1;
        public int mood;
        public Music(int mood, float t0, float t1) { this.mood = mood; this.t0 = t0; this.t1 = t1; }
    }

    public Seg segAt(float t) {
        for (Seg s : segs) if (t >= s.t0 && t < s.t1) return s;
        return segs.isEmpty() ? null : segs.get(segs.size() - 1);
    }
}
