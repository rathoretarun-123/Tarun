package com.tarun.kahani.core;

import java.util.List;

/**
 * The director's training: a Pixar-style directing guide turned into decisions for every line of a scene.
 * (The numbers refer to the sections of docs/director-training.md.)
 *
 * The order of thinking is the guide's core model (§1, §49): story purpose → the character's emotion →
 * performance → staging → composition → camera → light → sound → cut. The camera is never the starting point.
 *
 *  - Emotion decides the shot size: closer as feelings grow, back out when they are released (§3, §4, §41).
 *  - Every scene is a sequence with an arc: establish → introduce → develop → escalate → peak → release (§2, §44).
 *  - Close-ups are kept for moments when something changes inside a character (§42); at most about a third
 *    of the lines get one, and only the peak may get an extreme close-up (§3, §43).
 *  - Relationships are shown with both characters in the frame; conversations use over-the-shoulder framing,
 *    closer as the conversation becomes more important (§7, §8).
 *  - Camera height follows feeling: eye level normally, low for power and threat, high for vulnerability (§5).
 *  - The more important the performance, the simpler the camera: strong moments are static, a realisation gets
 *    a slow push-in, the release pulls back, calm talk breathes gently (§13, §14, §43).
 *  - Reactions are shown and allowed to breathe: event → reaction → silence → response (§9, §31).
 *  - Calm conversations hold a shot over several lines instead of cutting on every line; action scenes cut
 *    faster (§32, §46.3).
 *  - Light follows mood: soft and warm for warmth and safety, harder and directional for conflict and fear (§19).
 */
public final class ShotPlanner {
    private ShotPlanner() {}

    public static final int XWIDE = 0, WIDE = 1, MWIDE = 2, MEDIUM = 3, MCU = 4, CU = 5, XCU = 6;
    /** Zoom on the 1280 x 720 stage for each shot size. */
    public static final float[] ZOOM = {1.0f, 1.08f, 1.25f, 1.45f, 1.7f, 1.95f, 2.3f};
    public static final String[] SIZE_NAME = {"Extreme wide", "Wide", "Medium wide", "Medium", "Medium close-up", "Close-up", "Extreme close-up"};

    public static final int ESTABLISH = 0, INTRODUCE = 1, DEVELOP = 2, ESCALATE = 3, PEAK = 4, RELEASE = 5;
    public static final String[] STAGE_NAME = {"Establish", "Introduce", "Develop", "Escalate", "Peak", "Release"};

    public static final int SINGLE = 0, TWO_SHOT = 1, OTS = 2;
    public static final String[] TYPE_NAME = {"Single", "Two-shot", "Over the shoulder"};

    public static final int STATIC = 0, PUSH_IN = 1, PULL_BACK = 2, DRIFT = 3;
    public static final String[] MOVE_NAME = {"Static", "Slow push-in", "Pull-back", "Gentle drift"};

    /** The plan for one line of dialogue. */
    public static final class Plan {
        public float intensity;      // 0..1, how strongly the character feels in this line
        public int stage = DEVELOP, size = MEDIUM, type = OTS, move = DRIFT;
        public int height;           // -1 high angle (vulnerable), 0 eye level, +1 low angle (power)
        public boolean reaction;     // show the listener's reaction after the line and let it breathe
        public float breathe;        // seconds of silence after the line
        public boolean hold;         // keep the framing of the previous line (no cut)
        public boolean realise;      // a realisation, decision, confession
        public boolean relation;     // the line is about the relationship between the two
        public boolean isolated;     // a character alone with a heavy feeling
        public float light = 0.4f;   // 0 soft and warm .. 1 hard and directional
        public String purpose = "";
    }

    static final String[] SHOUT = {"चिल्ला", "ज़ोर से", "जोर से", "गरज", "चीख", "दहाड़", "shout", "scream", "yell", "roar", "cry out"};
    static final String[] REALISE = {"समझ गई", "समझ गया", "समझ गए", "अब मैं समझ", "अब समझ", "पता चल गया", "मैं करूँगी", "मैं करूँगा", "फ़ैसला", "फैसला",
            "वादा", "माफ़", "माफ", "क्षमा", "अलविदा", "कभी नहीं", "सच में", "मुझे पता है", "realise", "realize", "i understand", "now i see", "i promise",
            "promise", "decided", "i will", "sorry", "forgive", "goodbye", "never", "i love", "it was you", "the truth"};
    static final String[] RELATION = {"दोस्त", "सहेली", "साथ", "हम दोनों", "माँ", "पिताजी", "बेटी", "बेटा", "बहन", "दीदी", "भैया", "भाई", "प्यार", "धन्यवाद",
            "शुक्रिया", "माफ़", "माफ", "वादा", "friend", "together", "sister", "brother", "mother", "father", "thank", "love", "sorry", "promise", "family"};
    static final String[] ACTION = {"दौड़", "भाग", "लड़", "युद्ध", "हमला", "पीछा", "कूद", "तलवार", "बचाओ", "जल्दी", "run", "chase", "fight", "attack",
            "battle", "escape", "hurry", "jump", "sword", "help!"};

    /** How strongly a line is felt: the feeling, how it is said, and what is said. */
    public static float intensity(int emotion, String manner, String text) {
        float i;
        switch (emotion) {
            case Pose.HAPPY: i = 0.3f; break;
            case Pose.LAUGH: i = 0.38f; break;
            case Pose.CURIOUS: case Pose.PROUD: i = 0.35f; break;
            case Pose.DETERMINED: i = 0.5f; break;
            case Pose.SURPRISED: i = 0.6f; break;
            case Pose.EVIL: i = 0.6f; break;
            case Pose.SAD: i = 0.66f; break;
            case Pose.SCARED: i = 0.72f; break;
            case Pose.ANGRY: i = 0.75f; break;
            case Pose.PAIN: i = 0.8f; break;
            case Pose.DIZZY: i = 0.3f; break;
            default: i = 0.15f;
        }
        String all = (manner == null ? "" : manner) + " " + (text == null ? "" : text);
        if (Txt.has(all, SHOUT)) i += 0.15f;
        if (realisation(all)) i += 0.2f;
        int bangs = 0;
        for (int k = 0; k < (text == null ? 0 : text.length()); k++) if (text.charAt(k) == '!') bangs++;
        i += Math.min(0.1f, bangs * 0.05f);
        return Math.max(0, Math.min(1, i));
    }

    public static boolean realisation(String s) { return Txt.has(s, REALISE); }
    public static boolean relationship(String s) { return Txt.has(s, RELATION); }
    public static boolean action(String s) {
        int n = 0;
        for (String w : ACTION) if (Txt.has(s, w)) n++;
        return n >= 2;
    }

    /**
     * Plans every line of one part of a scene. emotions/manners/texts: one entry per line, in order.
     * action: a fast scene (chases, fights), where shots are not held.
     */
    public static Plan[] plan(List<Integer> emotions, List<String> manners, List<String> texts, boolean action) {
        int n = emotions.size();
        Plan[] p = new Plan[n];
        if (n == 0) return p;
        float[] raw = new float[n], s = new float[n];
        for (int i = 0; i < n; i++) {
            p[i] = new Plan();
            raw[i] = intensity(emotions.get(i), manners.get(i), texts.get(i));
            String all = manners.get(i) + " " + texts.get(i);
            p[i].realise = realisation(all);
            p[i].relation = relationship(all);
            // feelings carry over: a tense scene stays tense for a line or two (emotional momentum)
            s[i] = i == 0 ? raw[i] : Math.max(raw[i], s[i - 1] * 0.72f);
            p[i].intensity = s[i];
        }
        int peak = 0;
        for (int i = 1; i < n; i++) if (raw[i] >= raw[peak]) peak = i;
        boolean hasPeak = raw[peak] >= 0.5f;
        boolean xcuUsed = false;
        for (int i = 0; i < n; i++) {
            Plan q = p[i];
            int emo = emotions.get(i);
            // where this line is in the scene's arc
            if (i == 0) q.stage = INTRODUCE;
            else if (hasPeak && i == peak) q.stage = PEAK;
            else if (hasPeak && i == peak + 1) q.stage = RELEASE;
            else if (s[i] >= 0.45f || (hasPeak && i < peak && s[i] > s[i - 1] + 0.05f)) q.stage = ESCALATE;
            else q.stage = DEVELOP;
            // shot size from the feeling
            float v = s[i];
            q.size = v < 0.22f ? MWIDE : v < 0.42f ? MEDIUM : v < 0.6f ? MCU : CU;
            if (q.stage == PEAK && raw[i] >= 0.8f && !xcuUsed) { q.size = XCU; xcuUsed = true; }
            if (q.stage == INTRODUCE) q.size = Math.min(q.size, MEDIUM);         // establish before going close (§2)
            if (q.stage == RELEASE) q.size = Math.min(q.size, MWIDE);            // let the moment settle, show the larger situation
            boolean shock = emo == Pose.SURPRISED || emo == Pose.SCARED;
            if (i > 0 && !shock) q.size = Math.min(q.size, p[i - 1].size + 2);   // closer step by step, unless something shocks
            // light: soft and warm for warmth, harder for conflict and fear
            q.light = emo == Pose.ANGRY || emo == Pose.EVIL || emo == Pose.SCARED || emo == Pose.PAIN ? 0.8f
                    : emo == Pose.HAPPY || emo == Pose.LAUGH || q.relation ? 0.15f : emo == Pose.SAD ? 0.55f : 0.4f;
            // camera height
            q.height = (emo == Pose.SCARED || emo == Pose.SAD) && raw[i] >= 0.55f ? -1
                    : q.stage == PEAK && (emo == Pose.DETERMINED || emo == Pose.PROUD) ? 1 : 0;
            // movement: the more important the performance, the simpler the camera
            if (q.stage == RELEASE) q.move = PULL_BACK;
            else if (q.realise && q.stage != PEAK) q.move = PUSH_IN;
            else if (raw[i] >= 0.55f || q.stage == PEAK) q.move = STATIC;
            else q.move = DRIFT;
            // the reaction is often more important than the line itself
            q.reaction = raw[i] >= 0.6f || q.realise;
            q.breathe = q.reaction ? 0.5f + 0.6f * raw[i] + (q.stage == PEAK ? 0.4f : 0) : 0;
        }
        // close-ups are precious: about a third of the lines at most; the weakest give theirs up first
        int budget = Math.max(1, Math.round(n * 0.34f)), cus = 0;
        for (Plan q : p) if (q.size >= CU) cus++;
        while (cus > budget) {
            int weakest = -1;
            for (int i = 0; i < n; i++) if (p[i].size >= CU && p[i].stage != PEAK && (weakest < 0 || p[i].intensity < p[weakest].intensity)) weakest = i;
            if (weakest < 0) break;
            p[weakest].size = MCU;
            cus--;
        }
        // framing type and whether to cut at all
        for (int i = 0; i < n; i++) {
            Plan q = p[i];
            q.type = q.size >= CU ? SINGLE : (q.relation || q.intensity < 0.35f) ? TWO_SHOT : OTS;
            if (q.stage == RELEASE) q.type = TWO_SHOT;
            q.hold = !action && i > 0 && q.type == TWO_SHOT && p[i - 1].type == TWO_SHOT && p[i - 1].size == q.size
                    && Math.abs(q.intensity - p[i - 1].intensity) < 0.15f && q.move != PULL_BACK;
            q.purpose = purpose(q);
        }
        return p;
    }

    static String purpose(Plan q) {
        switch (q.stage) {
            case INTRODUCE: return q.type == TWO_SHOT ? "Introduce who matters and how they stand with each other" : "Introduce the speaker before going closer";
            case PEAK: return "The emotional peak: something changes inside the character";
            case RELEASE: return "Release: let the moment settle and show the larger situation";
            case ESCALATE: return q.realise ? "A realisation: the camera moves in with the thought" : "The tension rises: closer to the feeling";
            default:
                if (q.relation) return "Show the relationship: both characters in the frame";
                return q.type == OTS ? "Follow the conversation from one to the other" : "Develop what is happening";
        }
    }

    /** How much of a character's height each shot size shows (close-up: face and shoulders). */
    static final float[] SHOWS = {0, 0, 1.6f, 1.1f, 0.72f, 0.5f, 0.36f};
    /** The most the camera can come in before a picture gets soft. */
    public static float MAX_ZOOM = 2.9f;

    /** The zoom that frames a character of height h (stage units) at this shot size. */
    public static float zoomFor(int size, float h) {
        if (size <= WIDE || h <= 0) return ZOOM[size];
        return Math.max(ZOOM[size] * 0.9f, Math.min(MAX_ZOOM, 720f / (SHOWS[size] * h)));
    }

    /** The shot size whose zoom is closest to z (for the shot list of shots planned elsewhere). */
    public static int sizeOf(float z) {
        int best = 0;
        for (int i = 1; i < ZOOM.length; i++) if (Math.abs(ZOOM[i] - z) < Math.abs(ZOOM[best] - z)) best = i;
        return best;
    }
}
