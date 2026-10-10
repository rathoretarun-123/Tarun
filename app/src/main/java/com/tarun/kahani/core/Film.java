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
    /** v34: who looks at whom, and when — an entrance, an exit, a return, a reveal, someone hurt (the eyes go there). */
    public final List<Watch> watches = new ArrayList<Watch>();
    public final List<Music> music = new ArrayList<Music>();
    public final List<Amb> ambience = new ArrayList<Amb>();
    public final List<Weather> weather = new ArrayList<Weather>();
    public final List<String> notes = new ArrayList<String>();   // director's notes for the analysis screen
    /** The story spine (six beats, PixarLead R4) and the Braintrust review of the shots (PixarLead), for the descriptions and the QC. */
    public String[] spine;
    public PixarLead.Review braintrust;
    public String hero = "";
    /** Every shot as the director planned it (for the shot list and its quality check). */
    public final List<Shot> shots = new ArrayList<Shot>();
    /** The director's shot list and quality check, in the training guide's format. */
    public String shotList = "";
    /** Shots the user asked to calm down after checking them (Human QC): {t0, t1}; motion there is cut by 80%. */
    public final List<float[]> calm = new ArrayList<float[]>();

    /** 1 normally; 0.2 inside a shot the user asked to be calmer (protocol: reduce motion by 80%). */
    public float calmAt(float t) {
        for (float[] c : calm) if (t >= c[0] && t < c[1]) return TechnicalDirector.MOTION_FIX;
        return 1f;
    }

    /** One planned shot. */
    public static final class Shot {
        public float t, dur;
        public int part = -1;
        public int size, type, height, move, stage = -1;
        public String subject = "", other = "", purpose = "", action = "", face = "", body = "", sound = "", cutWhen = "", emotionalPurpose = "";
        public float light = 0.4f;
        public boolean reaction;
        public int line = -1;            // the dialogue line spoken in this shot (index into lines), or -1
        /** Technical Director checks: the largest movement of a character (fraction of the frame width), the
         *  words spoken in the shot, how many actions start in it. */
        public float motion;
        public int words, actions;
        public boolean speech;
        /** The words spoken in this shot (lip-sync shots). */
        public String spoken = "";
        /** What the user changed after checking the shot (Human QC), or "". */
        public String fixed = "";
        /** The handbook's record of the shot (ch. 5, 6, 15): its stable ID, the lens, where the eyes go, where attention goes first. */
        public String id = "", lens = "", gaze = "", attention = "";
        /** An over-the-shoulder reverse: the character whose shoulder and back are in the foreground, or "". */
        public String ots = "";
        /** The picture of each character this shot is drawn with (the front picture, or a view made from it). */
        public String view = "";
        /** v27: the user's own picture each character is drawn with in this shot (character id → index into Art.Sprite.poses; -1 = the front picture). */
        public final java.util.Map<String, Integer> pictures = new java.util.HashMap<String, Integer>();
        /** v28: while a character walks or runs in this shot, the user's pictures of its steps (indices into Sprite.poses), shown one per step. */
        public final java.util.Map<String, int[]> cycles = new java.util.HashMap<String, int[]>();
    }
    /** What the director counted while planning (the handbook's scorecard, ch. 13). */
    public Handbook.Stats stats;
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
        /** The user's reverse angle of the place (the phone guide §5.2), drawn behind reverse shots; null = the same picture. */
        public Art.Backdrop backdropReverse;
        public float ground = Sets.GROUND; // y of the floor line in stage coordinates
        public boolean festive;           // celebration lights
        public int mood = -1;
        /** v33: the user's instruction for this part's light (-1 much darker .. 1 much brighter; 0 = as planned). */
        public float bright = 0f;
        /** How this part begins: 0 a soft dissolve, 1 a dip to black (time passes), 2 a dip to white (magic, dreams, memories). */
        public int transition;
        /** The act of the story this part belongs to (1 setup, 2 the turn, 3 escalation, 4 climax, 5 resolution): the colour script follows it. */
        public int act = 1;
        /** The character scale lock of the delivery format (PixarLead RULE_RESIZE_6): a standing character's height as a share of the frame height. */
        public float charScale = 0.6f;
        /** A solid colour behind everything (thumbnail / poster pages), or 0 for the painted or photographed set. */
        public int solid;
        /** The characters of this part are in a boat on the water (they stand on its deck and move with it). */
        public boolean inBoat;
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
        /** v34: the back is to the camera (turns away, walks away, leaves): the back picture is drawn. */
        public boolean backTurned;
        public int holdR = Pose.I_NONE, holdL = Pose.I_NONE;
        public int seat = -1;       // when sitting: SEAT_* (-1 = none drawn)
        /** v34: the clothes worn now — 0 the character's own, n the n-th change of the story (CharacterDef.costumes). */
        public int costume;
        public Key copy() {
            Key k = new Key();
            k.t = t; k.moveDur = 0; k.x = x; k.depth = depth; k.body = body; k.facing = facing; k.emotion = emotion; k.anchor = anchor;
            k.anchorActor = anchorActor; k.visible = visible; k.run = false; k.disguised = disguised; k.noHeadwear = noHeadwear;
            k.wearsTurban = wearsTurban; k.redFace = redFace; k.tears = tears; k.netted = netted; k.sweat = sweat; k.eyesShut = eyesShut;
            k.holdR = holdR; k.holdL = holdL; k.seat = seat; k.backTurned = backTurned; k.costume = costume;
            return k;
        }
    }

    public static final int G_TALK = 0, G_CLAP = 1, G_LAUGH = 2, G_SWORD = 3, G_WAND = 4, G_POINT = 5, G_SALUTE = 6, G_HUG = 7,
            G_CRY = 8, G_TREMBLE = 9, G_CRUSH = 10, G_DANCE = 11, G_PAT = 12, G_GIVE = 13, G_RUB_EYES = 14, G_FLAIL = 15,
            G_PUSH = 16, G_PULL_ROPE = 17, G_TWIRL = 18, G_HOLD_HEAD = 19, G_REACH = 20, G_FIST = 21, G_SCRATCH = 22,
            G_JUMP = 23, G_SHAKE_HEAD = 24, G_CLUTCH = 25, G_WALK_PLACE = 26, G_BOUNCE = 27, G_MIRROR = 28, G_THROW = 29,
            G_SPLASH = 30, G_WIGGLE = 31, G_COUGH = 32, G_PLAY = 33, G_LOOK_UP = 34, G_WHISPER = 35, G_PROUD = 36,
            G_ROAR = 37, G_BLOCK = 38, G_STEP_BACK = 39, G_OFFER = 40, G_HOLD_HAND = 41, G_BOW = 42, G_WAVE = 43, G_NOD = 44,
            G_TURN = 45, G_LOOK_AWAY = 46,
            G_HEAD_SCRATCH = 47,  // the comic beat (Russo / Gunn): a puzzled scratch of the head, a shrug
            G_WEIGHT_SHIFT = 48,  // secondary action while idle: the weight moves from one foot to the other, a glance aside
            G_SHIELD_EYES = 49,   // an arm up against a blinding light, the head turned away
            G_COVER_HEAD = 52,    // v34: caught in the rain without an umbrella — shoulders up, head down, hands over the head
            G_SHIVER = 53,        // v34: cold (snow): a small fast shiver, the arms held in to the body
            G_SIGN = 54,          // v35: sign language — both hands shape the words in front of the chest, the face goes with them
            G_EAT = 55,           // v35: eating — the hand goes from the plate to the mouth, then chewing
            G_DRINK = 56,         // v35: drinking from a cup, a glass or a bottle (Act.item) — raised to the lips, a sip or gulps, lowered
            G_STRETCH = 57,       // v35: waking — the arms stretch up, a yawn
            G_TASK = 58,          // v36: an everyday task (Act.item = Film.T_*): cooking, sweeping, washing, reading, writing…
            G_PULL = 50,          // a hard pull (a plug, a wire, a rope): lean back and yank
            G_LISTEN = 51;        // thought before action (handbook ch. 6): a pause, the head turns toward the sound, the body holds still

    /** Seats under a sitting character. */
    public static final int SEAT_FLOOR = 0, SEAT_STOOL = 1, SEAT_THRONE = 2, SEAT_ROCK = 3, SEAT_WHEELCHAIR = 4,   // v34: a wheelchair goes where its sitter goes
            SEAT_CHAIR = 5, SEAT_SOFA = 6, SEAT_BED = 7;     // v35: a chair with a back, a cushioned sofa, a bed (sat on, lain and slept in)

    /** v36: the everyday tasks a G_TASK act shows. */
    public static final int T_COOK = 1, T_SWEEP = 2, T_WASH = 3, T_READ = 4, T_WRITE = 5, T_PHONE = 6, T_BRUSH = 7, T_COMB = 8, T_WATER = 9, T_PAPER = 10;

    public static final class Act {
        public float t0, t1;
        public int type;
        public Actor target;
        /** v35: what the hands hold for it (Pose.I_*: the cup, the glass, the bottle, the plate). */
        public int item;
        public Act(float t0, float t1, int type) { this.t0 = t0; this.t1 = t1; this.type = type; }
    }

    public static final class Speak {
        public float t0, t1;
        public int line;
        public int emotion;
        /** v34: spoken by the animal the actor rides (Durga's lion): the animal's jaw moves, not the rider's lips. */
        public boolean mount;
    }

    public static final class Actor {
        public Story.CharacterDef c;
        public Look look;
        public final List<Key> keys = new ArrayList<Key>();
        public final List<Act> acts = new ArrayList<Act>();
        public final List<Speak> speaks = new ArrayList<Speak>();
        public int order;
        /** Spider-Verse: the frame rate this character's poses step on (24 experts, 12 learners, 8 rebels; 0 = smooth, every frame). */
        public int stepFps;
        public Key last() { return keys.get(keys.size() - 1); }
        /** Starts a new key at time t copying the current state. */
        public Key at(float t) {
            Key l = last();
            if (Math.abs(l.t - t) < 0.001f && l.moveDur == 0) return l;
            Key k = l.copy();
            k.t = t;
            // a character still on the way keeps going (from where they are) instead of arriving all at once
            if (l.moveDur > 0 && t > l.t && t < l.t + l.moveDur) { k.moveDur = l.t + l.moveDur - t; k.run = l.run; }
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
            FX_MAGIC_FLOWER = 14, FX_HIDE_ROCK = 15, FX_LADDOO_GLOW = 16, FX_FIREWORKS = 17, FX_DUST = 18, FX_BOULDER = 19, FX_SHOT = 20, FX_TITLE_SPARKS = 21,
            FX_LIGHTNING = 22,    // a flash and a bolt (x = where in the sky)
            FX_STONE = 23,        // a: the thrower; x, y: where it lands; t2: when it lands
            FX_WATER_HIT = 24,    // something falls into water at x, y (color = size 1..3)
            FX_THROW = 25,        // a: thrower, b: target (or x, y); kind: what flies
            FX_FALL = 26,         // something falls from above at x (kind) and bounces
            FX_SEAT = 27,         // a throne / stool / rock at x (kind = SEAT_*), sized for actor a
            FX_SHADOW_PASS = 28,  // Gunn: one scary shadow sweeping over the ground in a funny scene (x = where it starts)
            // the cues of any script (Cues): light, machines, little things that fly or spark
            FX_GLOW_AREA = 29,    // lights come on across the stage (a garden of lamps or flowers lighting up); color = their colour
            FX_TWINKLE = 30,      // small lights twinkling over an area around x, y
            FX_FLICKER = 31,      // the light of the whole frame flickers
            FX_LIGHTS_OFF = 32,   // the lights go out: the frame darkens and stays dark until t1
            FX_GLITCH = 33,       // a digital glitch: bands of the picture slip, static
            FX_DRONE = 34,        // a small drone flies about near actor a (or x, y)
            FX_HEARTS = 35,       // little hearts rise above actor a
            FX_NOTIFY = 36,       // a glowing notification card pops up above actor a
            FX_DATA = 37,         // a stream of light particles flows from x, y away (data leaving)
            FX_SPARKS = 38,       // electric sparks at x, y (near actor a's hands)
            FX_STEAM = 39;        // steam or smoke rising at x, y

    public static final class Fx {
        public float t0, t1;
        public int type;
        public float x, y;
        public Actor a, b;
        public int color;
        public float t2;                  // secondary moment (e.g. bud turns into flower)
        public Art.Backdrop pic;          // full-screen cinematic picture (FX_SHOT)
        public int kind;                  // FX_THROW / FX_FALL: 0 stone, 1 ball, 2 fruit, 3 flower
        public Fx(int type, float t0, float t1) { this.type = type; this.t0 = t0; this.t1 = t1; }
    }

    public static final class Cam {
        public float t, cx, cy, zoom, ease;   // ease 0 = cut
        public float roll;                    // degrees: a tilted "Dutch angle" for menace or unease
        public boolean still;                 // a strong performance: the camera holds perfectly still
        public int angle;                     // -1 high angle (vulnerable), 0 eye level, +1 low angle (power)
        public float light = -1;              // 0 soft and warm .. 1 hard and directional (-1 = from the scene's mood)
        public boolean keep;                  // a cut the protocol needs (a new group of at most six words): never dropped
        /** A reverse shot (the listener's face, over the speaker's shoulder): the place's reverse angle is behind it when the user gave one. */
        public boolean reverse;
        public Cam(float t, float cx, float cy, float zoom, float ease) { this.t = t; this.cx = cx; this.cy = cy; this.zoom = zoom; this.ease = ease; }
    }

    public static final class Sub {
        public float t0, t1;
        public String who, text;
        /** v35: a line said in sign language: shown even when subtitles are off. */
        public boolean signed;
    }

    // -------------------------------------------------------------- audio
    public static final class Line {
        public int index;
        public Story.CharacterDef who;   // null = narrator
        public String text;              // what is spoken
        public String shown;             // what is shown as subtitle
        public boolean hindi = true;     // language of this line (a story may mix Hindi and English dialogue)
        public String manner = "";       // acting direction from the script, e.g. "डरते हुए, धीरे से"
        public int emotion;
        public boolean whisper, echo;
        public float dur;                // seconds, filled after synthesis
        public float[] env;              // mouth openness envelope, 100 Hz
        public float[] shape;            // mouth shape, 100 Hz: 0 round ("oo") .. 1 wide ("ee", "s")
        public float start;              // placed on timeline
        public float gain = 1f;
        /** v35: said in sign language (the speaker cannot speak, or signs): the hands sign, the lips stay still, the words are subtitled and voiced over. */
        public boolean signed;
    }

    public static final int SFX_STREAM = 0, SFX_BIRDS = 1, SFX_WIND = 2, SFX_CLACK = 3, SFX_POP = 4, SFX_CHIME = 5, SFX_MONKEY = 6,
            SFX_RUSTLE = 7, SFX_THUD = 8, SFX_WHOOSH = 9, SFX_BELL = 10, SFX_DRUMS = 11, SFX_NIGHT = 12, SFX_ROAR = 13, SFX_CLAP = 14,
            SFX_ANKLET = 15, SFX_HISS = 16, SFX_DRIP = 17, SFX_SCREAM_FX = 18, SFX_SPLASH = 19, SFX_NET = 20, SFX_WHOOSH_CARD = 21,
            SFX_FANFARE = 22, SFX_END_CHORD = 23, SFX_MAGIC = 24, SFX_STEPS = 25, SFX_CROWD = 26, SFX_GLASS = 27, SFX_SWORD = 28,
            SFX_USER = 29, SFX_THUNDER = 30,
            SFX_STEPS_HARD = 31,  // Nolan: real sounds — steps on stone, marble, a cave floor
            SFX_STEPS_RUN = 32,   // running steps
            // the sounds of a city and of machines (synthesized), and the generic cue sounds of any script
            SFX_HUM = 33,         // an electric hum (a tubelight, a server room)
            SFX_TRAFFIC = 34,     // distant traffic
            SFX_DRONE = 35,       // a drone's buzz passing
            SFX_BEEP = 36,        // beeps (a robot, a device)
            SFX_CLICK = 37,       // a click, a clack, a switch
            SFX_TYPING = 38,      // keyboard typing
            SFX_BUZZ = 39,        // a buzz / hum of an insect or a machine
            SFX_GLITCH = 40,      // digital glitch, static
            SFX_POWER_DOWN = 41,  // lights and machines going off
            SFX_SPARK = 42,       // an electric spark, a wire pulled
            SFX_HEARTBEAT = 43,   // a heartbeat (Gunn: scary = silence + heartbeat)
            SFX_TWINKLE = 44,     // small lights twinkling
            SFX_BLIP = 45,        // a notification pop
            SFX_CRACKLE = 46,     // crackling (an old machine, fire, static)
            SFX_HISS_SHORT = 47,  // a short hiss ("फूँ", "फुस्स")
            SFX_TAP = 48,         // light taps ("टप-टप")
            SFX_BOOM = 49,        // a boom, an explosion
            // v34: the sounds of walking aids and of rain on an umbrella
            SFX_STICK = 50,       // slow steps with a walking stick's wooden tap (or a walking frame's)
            SFX_CRUTCH = 51,      // crutches: the two rubber tips, then the step
            SFX_WHEELCHAIR = 52,  // a wheelchair rolling: tyres on the floor, the hand-rims ticking
            SFX_UMBRELLA_RAIN = 53, // rain drumming on an umbrella over the head
            SFX_DOOR = 54,          // v34: a door — the latch, the hinge, the door closing (an entrance or an exit indoors)
            // v35: a limp's uneven steps, and the sounds of everyday actions
            SFX_STEPS_LIMP = 55,    // a firm step, then a lighter one that drags
            SFX_CHAIR = 56,         // a chair: its legs scrape, the wood creaks as someone sits or gets up
            SFX_SOFA = 57,          // a sofa: the cushion gives with a soft thump, a spring sighs
            SFX_BED = 58,           // a bed: the frame creaks, the covers rustle
            SFX_EAT = 59,           // eating: a spoon on the plate, then quiet chewing
            SFX_SIP = 60,           // a sip from a cup or a glass, a swallow, the cup set down
            SFX_GULP = 61,          // drinking from a bottle: gulps
            SFX_CUP = 62,           // a cup or a glass set down on the table
            SFX_SLEEP = 63,         // a sleeper's slow, soft breathing
            SFX_SIZZLE = 64,        // v36: cooking — oil sizzling in the pan, the ladle scraping and tapping
            SFX_SWEEP = 65,         // v36: a broom's swishes on the floor
            SFX_SCRUB = 66,         // v36: washing — water sloshing, scrubbing, a wring
            SFX_PAGE = 67,          // v36: a page turned
            SFX_SCRIBBLE = 68,      // v36: a pen or pencil on paper
            SFX_BRUSH = 69,         // v36: brushing teeth — the brush's quick strokes
            SFX_POUR = 70;          // v36: water poured (a watering can over the plants)

    // -------------------------------------------------------------- weather and nature
    public static final int W_RAIN = 0, W_STORM = 1, W_WIND = 2, W_SNOW = 3, W_FOG = 4, W_FIRE = 5, W_FIREFLIES = 6, W_LEAVES = 7,
            W_PETALS = 8, W_DUST = 9, W_STARS = 10, W_RAINBOW = 11, W_SEA = 12, W_BOAT = 13, W_CANDLES = 14, W_CLOUDS = 15,
            W_BIRDS = 16, W_QUAKE = 17, W_KINDS = 18;

    /** Weather from t0 to t1 (it builds up and dies away over a couple of seconds). */
    public static final class Weather {
        public float t0, t1, strength;
        public int type;
        public float x;          // fire: where it burns (stage x)
        public int kind;         // candles: 0 candles, 1 diyas, 2 torches; clouds: 1 = dark
        public Weather(int type, float t0, float t1, float strength) { this.type = type; this.t0 = t0; this.t1 = t1; this.strength = strength; }
    }

    /** How strong a kind of weather is at time t (0 = none). */
    public float weather(int type, float t) {
        float best = 0;
        for (Weather w : weather) {
            if (w.type != type || t < w.t0 - 0.01f || t > w.t1 + 2.5f) continue;
            float ramp = type == W_RAIN || type == W_STORM || type == W_SNOW ? 2.5f : 1.5f;
            float k = Math.min(1, (t - w.t0) / ramp) * Math.min(1, Math.max(0, (w.t1 + ramp - t) / ramp));
            best = Math.max(best, w.strength * Math.max(0, k));
        }
        return best;
    }

    /** Wind at time t: steady part from wind and storms, with gusts (+ blows left to right). */
    public float wind(float t) {
        float w = Math.max(weather(W_WIND, t), Math.max(weather(W_STORM, t) * 1.1f, weather(W_DUST, t)));
        w = Math.max(w, weather(W_RAIN, t) * 0.25f);
        if (w <= 0) return 0;
        return w * (0.75f + 0.25f * (float) Math.sin(t * 1.3) + 0.12f * (float) Math.sin(t * 3.7));
    }

    /** How wet the characters are: soaking up during rain, drying slowly afterwards. */
    public float wetness(float t) {
        float wet = 0;
        for (Weather w : weather) {
            if ((w.type != W_RAIN && w.type != W_STORM) || t < w.t0) continue;
            float soak = Math.min(1, (Math.min(t, w.t1) - w.t0) / 6f) * w.strength;
            float dry = t > w.t1 ? Math.max(0, 1 - (t - w.t1) / 60f) : 1;
            wet = Math.max(wet, Math.min(1, soak) * dry);
        }
        return wet;
    }

    public static final class Sfx {
        public float t, dur, gain;
        public int type;
        public String file;      // SFX_USER: the user's own sound (library path)
        /** v34: where it is heard (-0.6 left .. +0.6 right) at its start and its end — steps move with the walker — and how loud at
         *  each end (a character walking off fades away, one walking in fades up). pan1 NaN: it stays where it starts. */
        public float pan, pan1 = Float.NaN, gain0 = 1f, gain1 = 1f;
        public Sfx(int type, float t, float dur, float gain) { this.type = type; this.t = t; this.dur = dur; this.gain = gain; }
    }

    /** v34: an actor's eyes held on another actor from t0 to t1 (null who: everyone else on the stage). */
    public static final class Watch {
        public Actor who, at;
        public float t0, t1;
        public Watch(Actor who, Actor at, float t0, float t1) { this.who = who; this.at = at; this.t0 = t0; this.t1 = t1; }
    }

    public static final int M_TITLE = 0, M_HAPPY = 1, M_TENSE = 2, M_VILLAIN = 3, M_SAD = 4, M_ACTION = 5, M_CELEBRATE = 6,
            M_END = 7, M_NIGHT = 8, M_PLAYFUL = 9;

    /** A bed of real recorded ambience chosen by matching these words (location, time, weather). */
    public static final class Amb {
        public float t0, t1;
        public String words;
        public Amb(float t0, float t1, String words) { this.t0 = t0; this.t1 = t1; this.words = words; }
    }

    public static final class Music {
        public float t0, t1;
        public int mood;
        /** Loudness over time (seconds -> 0..1.3): swells at dramatic moments, dips under whispers. */
        public float[] envT, envV;
        /** Soft, muffled sound (sad or night-time moods). */
        public boolean soft;
        public float level(float t) {
            if (envT == null || envT.length == 0) return 1f;
            if (t <= envT[0]) return envV[0];
            for (int i = 1; i < envT.length; i++) {
                if (t < envT[i]) {
                    float f = (t - envT[i - 1]) / Math.max(1e-3f, envT[i] - envT[i - 1]);
                    return envV[i - 1] + (envV[i] - envV[i - 1]) * f;
                }
            }
            return envV[envV.length - 1];
        }
        public Music(int mood, float t0, float t1) { this.mood = mood; this.t0 = t0; this.t1 = t1; }
    }

    public Seg segAt(float t) {
        for (Seg s : segs) if (t >= s.t0 && t < s.t1) return s;
        return segs.isEmpty() ? null : segs.get(segs.size() - 1);
    }
}
