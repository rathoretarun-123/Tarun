package com.tarun.kahani.core;

/** Instantaneous body state of a puppet for one frame. */
public final class Pose {
    // body poses
    public static final int STAND = 0, SIT = 1, LIE = 2, KNEEL = 3, HANG = 4, CROUCH = 5;
    // emotions
    public static final int NEUTRAL = 0, HAPPY = 1, LAUGH = 2, ANGRY = 3, SAD = 4, SCARED = 5, SURPRISED = 6,
            EVIL = 7, DETERMINED = 8, DIZZY = 9, WHISPER = 10, PROUD = 11, CURIOUS = 12, PAIN = 13;
    // held items
    public static final int I_NONE = 0, I_RIBBON = 1, I_BANANA = 2, I_MIRROR = 3, I_WOOD_SWORD = 4, I_BASKET = 5,
            I_FLOWER = 6, I_SWORD = 7, I_BOTTLE = 8, I_TURBAN = 9;

    public int body = STAND;
    public int emotion = NEUTRAL;
    public float facing = 1f;          // +1 right, -1 left
    public float mouth;                // 0..1 open
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
    public long seed;

    public void reset() {
        body = STAND; emotion = NEUTRAL; facing = 1; mouth = 0; blink = 0; armL = 8; armR = 8; elbowL = 10; elbowR = 10;
        walk = 0; walkAmt = 0; tilt = 0; headTilt = 0; bob = 0; squash = 1; holdR = I_NONE; holdL = I_NONE;
        disguised = false; noHeadwear = false; wearsTurban = false; redFace = false; tears = false; sweat = false;
        eyesClosed = false; fist = false; glowWand = false; carrying = false; turbanBand = 0; wind = 0; wet = 0;
    }
}
