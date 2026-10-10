package com.tarun.kahani.core;

/** Instantaneous body state of a puppet for one frame. */
public final class Pose {
    // body poses
    public static final int STAND = 0, SIT = 1, LIE = 2, KNEEL = 3, HANG = 4, CROUCH = 5;
    // emotions
    public static final int NEUTRAL = 0, HAPPY = 1, LAUGH = 2, ANGRY = 3, SAD = 4, SCARED = 5, SURPRISED = 6,
            EVIL = 7, DETERMINED = 8, DIZZY = 9, WHISPER = 10, PROUD = 11, CURIOUS = 12, PAIN = 13,
            SUSPICIOUS = 14, RELIEVED = 15;   // the director's manual (2.6): suspicion and relief
    // held items
    public static final int I_NONE = 0, I_RIBBON = 1, I_BANANA = 2, I_MIRROR = 3, I_WOOD_SWORD = 4, I_BASKET = 5,
            I_FLOWER = 6, I_SWORD = 7, I_BOTTLE = 8, I_TURBAN = 9;

    public int body = STAND;
    public int emotion = NEUTRAL;
    public float facing = 1f;          // +1 right, -1 left
    public float mouth;                // 0..1 open
    public float mouthWide = 0.5f;     // 0 round ("oo") .. 1 wide ("ee", "s"), from the sound of the voice
    public float mountMouth;           // v34: 0..1 open — the jaw of the animal a rider sits on (Durga's lion speaking)
    public float blink;                // 0..1 closed
    public float armL = 8, armR = 8;   // degrees, 0 = down, 90 = sideways out, 170 = up
    public float elbowL = 10, elbowR = 10;
    public float walk;                 // walk cycle phase (radians), used when moving
    public float walkAmt;              // 0 still, 1 full stride
    public float tilt;                 // body lean in degrees
    public float headTilt;
    public float bob;                  // vertical offset px
    public float squash = 1f;
    public int holdR = I_NONE, holdL = I_NONE;
    public int holdColor = 0xFFC62828;
    public boolean disguised, noHeadwear, wearsTurban, redFace, tears, sweat, eyesClosed, fist, glowWand;
    public int turbanColor = 0xFF2F5DB5, turbanBand = 0;
    public boolean carrying;           // arms up holding something on shoulder
    public float time;                 // seconds, for idle motion (hair, cloak)
    public float wind;                 // wind on hair and clothes (+ blows to the right)
    public float wet;                  // 0 dry .. 1 soaked
    public float sit;                  // 0 standing .. 1 seated (sitting down and getting up move through it)
    public float nod;                  // + head down, - head up
    public float wave;                 // > 0 while waving: the arm swings out and back
    public boolean twirl;              // twirling a moustache / fidgeting with the raised hand
    public boolean swing;
    public boolean umbrellaOpen;       // v34: an umbrella held open over the head (rain outdoors)
    /** v34: where the eyes look, on screen (-1 left .. +1 right, -1 up .. +1 down), and how much they rest on someone (0..1). */
    public float gazeX, gazeY, gazeHeld;
    public String turbanOwner;         // whose turban / cap is being worn (its real picture is used when there is one)              // a sword is being swung right now (otherwise a held sword rests calmly)
    public long seed;

    /** v34: a copy of this pose with another body state (a rider seated on the mount). */
    public Pose copyFor(int bodyState) {
        Pose q = new Pose();
        q.body = bodyState; q.emotion = emotion; q.facing = facing; q.mouth = mouth; q.mouthWide = mouthWide; q.blink = blink;
        q.armL = armL; q.armR = armR; q.elbowL = elbowL; q.elbowR = elbowR; q.walk = 0; q.walkAmt = 0; q.tilt = tilt; q.headTilt = headTilt;
        q.bob = 0; q.squash = 1f; q.mountMouth = mountMouth; q.holdR = holdR; q.holdL = holdL; q.holdColor = holdColor;
        q.disguised = disguised; q.noHeadwear = noHeadwear; q.wearsTurban = wearsTurban; q.redFace = redFace; q.tears = tears; q.sweat = sweat;
        q.eyesClosed = eyesClosed; q.fist = fist; q.glowWand = glowWand; q.turbanColor = turbanColor; q.turbanBand = turbanBand; q.carrying = carrying;
        q.time = time; q.wind = wind; q.wet = wet; q.sit = bodyState == SIT ? 1f : sit; q.nod = nod; q.wave = wave; q.twirl = twirl; q.swing = swing;
        q.turbanOwner = turbanOwner; q.seed = seed; q.umbrellaOpen = umbrellaOpen;
        q.gazeX = gazeX; q.gazeY = gazeY; q.gazeHeld = gazeHeld;
        return q;
    }

    public void reset() {
        body = STAND; emotion = NEUTRAL; facing = 1; mouth = 0; mouthWide = 0.5f; swing = false; turbanOwner = null; blink = 0; armL = 8; armR = 8; elbowL = 10; elbowR = 10;
        walk = 0; walkAmt = 0; tilt = 0; headTilt = 0; bob = 0; squash = 1; holdR = I_NONE; holdL = I_NONE;
        disguised = false; noHeadwear = false; wearsTurban = false; redFace = false; tears = false; sweat = false;
        eyesClosed = false; fist = false; glowWand = false; carrying = false; turbanBand = 0; wind = 0; wet = 0;
        sit = 0; nod = 0; wave = 0; twirl = false; umbrellaOpen = false;
        gazeX = 0; gazeY = 0; gazeHeld = 0;
    }
}
