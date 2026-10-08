package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

/**
 * How a voice should sound beyond pitch and speed, read from the character's description ("खरखरी आवाज़",
 * "a booming voice", "kaanpti awaaz") and from each line's acting direction ("चिल्लाते हुए", "whispering",
 * "रोते हुए"). Applied as real sound processing after the voice is made, so it works with every voice engine
 * and with the user's own voice samples.
 */
public final class VoiceStyle {
    // qualities of the character's voice, 0..1
    public float raspy, breathy, tremble, boom, nasal, squeak, robot, magic, ghost, growl;
    // the way one line is said
    public float shout, cry, laugh, sing, far, soft;
    /** Changes to speed (%) and pitch (Hz) asked for by the acting direction. */
    public int ratePct, pitchHz;
    /** What was understood, in English (shown in the studio and the production file). */
    public final List<String> words = new ArrayList<String>();

    static final String[] RASPY = {"खरखरी", "खरखराती", "कर्कश", "फटी आवाज़", "फटी हुई आवाज़", "भर्राई", "खुरदरी", "रूखी आवाज़", "raspy", "hoarse",
            "croaky", "gravelly", "husky", "harsh voice", "rough voice", "kharkhari", "karkash"};
    static final String[] BREATHY = {"साँस भरी", "सांस भरी", "फुसफुसाती", "हवा जैसी", "breathy", "airy", "whispery", "phusphusati"};
    static final String[] TREMBLE = {"काँपती", "कांपती", "कंपकंपाती", "लरज़ती", "लरजती", "थरथराती", "shaky", "trembling", "quivering", "wobbly", "quavering",
            "kaanpti", "kampti"};
    static final String[] BOOM = {"गूँजती", "गूंजती", "गरजती", "गरजदार", "बुलंद", "गड़गड़ाती", "भारी-भरकम", "booming", "thundering", "echoing", "resonant",
            "thunderous", "goonjti", "garajdar"};
    static final String[] NASAL = {"नाक से", "नकियाती", "निनियाती", "मिमियाती", "nasal", "whiny", "through the nose"};
    static final String[] SQUEAK = {"चूँ-चूँ", "चूं-चूं", "चीं-चीं", "कीं-कीं", "चिचियाती", "squeaky", "mousy", "chipmunk", "tiny voice", "squeak"};
    static final String[] ROBOT = {"मशीनी", "यांत्रिक", "रोबोट", "robotic", "robot", "metallic", "mechanical", "machine voice"};
    static final String[] MAGIC = {"जादुई आवाज़", "दैवीय", "दिव्य", "आकाशवाणी", "परी जैसी", "ethereal", "angelic", "divine", "magical voice", "heavenly",
            "fairy voice", "voice from the sky"};
    static final String[] GHOST = {"भूत", "प्रेत", "आत्मा", "पिशाच", "ghost", "spooky", "eerie", "phantom", "spirit voice", "bhoot"};
    static final String[] GROWL = {"गुर्राती", "गुर्राहट", "गुर्राता", "growl", "snarl", "gurrati"};

    // the way a line is said (from the direction in brackets)
    static final String[] SHOUT = {"चिल्ला", "चीख", "चीखते", "ज़ोर से", "जोर से", "गरजते", "दहाड़", "ललकार", "shout", "yell", "scream", "roar", "bellow",
            "chillate", "chilla", "zor se", "jor se", "cheekh"};
    static final String[] CRY = {"रोते", "रोती", "सिसक", "रुआँस", "रुआंस", "आँसू", "cry", "crying", "sob", "tearful", "in tears", "rote", "sisak"};
    static final String[] LAUGH = {"हँसते", "हंसते", "हँसती", "हंसती", "खिलखिला", "ठहाका", "laugh", "giggl", "chuckl", "haste hue", "hanste", "khilkhila"};
    static final String[] PANT = {"हाँफ", "हांफ", "थके हुए", "थकी हुई", "पसीने", "panting", "out of breath", "breathless", "gasping", "haanf"};
    static final String[] SLEEPY = {"नींद", "उबास", "जम्हाई", "sleepy", "yawn", "drowsy", "neend"};
    static final String[] SING = {"गाते हुए", "गाती हुई", "गुनगुना", "गीत गाते", "singing", "sings", "humming", "hums", "gaate hue", "gungunate"};
    static final String[] SLOWLY = {"धीरे-धीरे", "धीरे धीरे", "धीमे-धीमे", "धीमी आवाज़ में", "रुक-रुक", "ठहर-ठहर", "slowly", "haltingly", "dheere dheere"};
    static final String[] QUICKLY = {"जल्दी से", "जल्दी-जल्दी", "फटाफट", "तेज़ी से", "हड़बड़ा", "quickly", "hurriedly", "in a rush", "rapidly", "jaldi se", "jaldi jaldi"};
    static final String[] SOFTLY = {"धीरे से", "प्यार से", "नरमी से", "कोमलता से", "softly", "gently", "tenderly", "dheere se", "pyaar se"};
    static final String[] FAR = {"दूर से", "दूर से चिल्ला", "from far", "from a distance", "far away", "door se"};

    /** True when the phrase starts a word (so "जगाते" does not count as "गाते"). */
    static boolean hasWord(String text, String... phrases) {
        String h = Txt.norm(text);
        for (String p : phrases) {
            String n = Txt.norm(p);
            for (int at = h.indexOf(n); at >= 0; at = h.indexOf(n, at + 1)) {
                if (at == 0 || !Character.isLetter(h.charAt(at - 1))) return true;
            }
        }
        return false;
    }

    /** The character's own voice qualities: from the description and from what the character is. */
    public static VoiceStyle forCharacter(Story.CharacterDef c) {
        VoiceStyle s = new VoiceStyle();
        if (c == null) return s;
        String v = VoiceMatch.voiceText(c.description);
        String all = c.displayName + " " + (c.fullName == null ? "" : c.fullName) + " " + c.description;
        if (hasWord(v, RASPY)) s.raspy = 0.65f;
        if (hasWord(v, BREATHY)) s.breathy = 0.45f;
        if (hasWord(v, TREMBLE)) s.tremble = 0.7f;
        if (hasWord(v, BOOM)) s.boom = 0.7f;
        if (hasWord(v, NASAL)) s.nasal = 0.7f;
        if (hasWord(v, SQUEAK)) s.squeak = 0.7f;
        if (hasWord(v, ROBOT) || hasWord(all, "रोबोट", "robot")) s.robot = 0.7f;
        // a voice from a machine (an AI, a computer, a phone, an app): a touch of the machine in it
        if (c.voiceOnly && (all.matches("(?s).*\\bAI\\b.*") || hasWord(all, "कंप्यूटर", "computer", "मशीन", "machine", "app", "ऐप", "phone", "फ़ोन", "robot", "रोबोट"))) s.robot = Math.max(s.robot, 0.45f);
        if (hasWord(v, MAGIC)) s.magic = 0.7f;
        if (hasWord(all, GHOST)) s.ghost = 0.75f;
        if (hasWord(v, GROWL)) s.growl = 0.6f;
        Look l = c.look;
        if (l != null) {
            // what the character is gives a gentle default; the description can only make it stronger
            if (l.kind == Look.MONSTER) { s.boom = Math.max(s.boom, 0.5f); s.growl = Math.max(s.growl, 0.3f); }
            if (l.kind == Look.WITCH) { s.raspy = Math.max(s.raspy, 0.35f); s.tremble = Math.max(s.tremble, 0.15f); }
            if (l.kind == Look.OLD_MAN || c.age >= 65) { s.tremble = Math.max(s.tremble, 0.25f); s.raspy = Math.max(s.raspy, 0.15f); }
            if (l.kind == Look.MONKEY) s.squeak = Math.max(s.squeak, 0.2f);
            if (l.kind == Look.ANIMAL || l.kind == Look.BIRD) animal(s, l.species);
        }
        // animals without a drawn kind, found by name
        if (hasWord(all, "मेंढक", "मेढक", "frog", "toad")) { s.raspy = Math.max(s.raspy, 0.35f); s.growl = Math.max(s.growl, 0.15f); }
        if (hasWord(all, "साँप", "सांप", "नाग", "snake", "cobra", "serpent")) s.breathy = Math.max(s.breathy, 0.4f);
        s.describe();
        return s;
    }

    /**
     * A talking animal keeps a clear voice for its words, with a touch of its kind: a lion or tiger rumbles,
     * a mouse or sparrow squeaks, a crow is hoarse, an elephant booms, a goat or cow bleats.
     */
    static void animal(VoiceStyle s, int sp) {
        switch (sp) {
            case Look.SP_LION: case Look.SP_TIGER: s.growl = Math.max(s.growl, 0.35f); s.boom = Math.max(s.boom, 0.35f); break;
            case Look.SP_BEAR: s.growl = Math.max(s.growl, 0.3f); s.boom = Math.max(s.boom, 0.4f); break;
            case Look.SP_WOLF: s.growl = Math.max(s.growl, 0.3f); s.raspy = Math.max(s.raspy, 0.15f); break;
            case Look.SP_FOX: s.nasal = Math.max(s.nasal, 0.25f); break;
            case Look.SP_DOG: s.raspy = Math.max(s.raspy, 0.25f); s.growl = Math.max(s.growl, 0.1f); break;
            case Look.SP_CAT: s.nasal = Math.max(s.nasal, 0.3f); s.squeak = Math.max(s.squeak, 0.15f); break;
            case Look.SP_MOUSE: case Look.SP_RABBIT: case Look.SP_SPARROW: s.squeak = Math.max(s.squeak, 0.45f); break;
            case Look.SP_PARROT: s.nasal = Math.max(s.nasal, 0.4f); s.squeak = Math.max(s.squeak, 0.25f); break;
            case Look.SP_CROW: s.raspy = Math.max(s.raspy, 0.5f); s.nasal = Math.max(s.nasal, 0.2f); break;
            case Look.SP_ELEPHANT: s.boom = Math.max(s.boom, 0.55f); break;
            case Look.SP_COW: case Look.SP_GOAT: s.tremble = Math.max(s.tremble, 0.25f); s.nasal = Math.max(s.nasal, 0.2f); break;
            case Look.SP_HORSE: s.breathy = Math.max(s.breathy, 0.2f); s.boom = Math.max(s.boom, 0.2f); break;
            case Look.SP_TORTOISE: s.tremble = Math.max(s.tremble, 0.2f); s.raspy = Math.max(s.raspy, 0.2f); break;
            case Look.SP_OWL: s.boom = Math.max(s.boom, 0.3f); s.breathy = Math.max(s.breathy, 0.15f); break;
            case Look.SP_HEN: case Look.SP_DUCK: s.nasal = Math.max(s.nasal, 0.45f); break;
            case Look.SP_EAGLE: case Look.SP_PEACOCK: s.raspy = Math.max(s.raspy, 0.2f); break;
            case Look.SP_DEER: s.breathy = Math.max(s.breathy, 0.2f); break;
            default:
        }
    }

    /** Adds or removes qualities asked for in the edit box ("raspy,booming,-trembling"). */
    public static VoiceStyle withEdits(VoiceStyle base, String edits) {
        VoiceStyle s = base == null ? new VoiceStyle() : base.copy();
        if (edits == null || edits.length() == 0) return s;
        for (String e : edits.split(",")) {
            boolean off = e.startsWith("-");
            String k = off ? e.substring(1) : e;
            float v = off ? 0 : 0.7f;
            if (k.equals("raspy")) s.raspy = v;
            else if (k.equals("trembling")) s.tremble = v;
            else if (k.equals("booming")) s.boom = v;
            else if (k.equals("robotic")) s.robot = v;
            else if (k.equals("whisper")) s.breathy = off ? 0 : 0.85f;
            else if (k.equals("nasal")) s.nasal = v;
            else if (k.equals("ghostly")) s.ghost = v;
            else if (k.equals("magical")) s.magic = v;
            else if (k.equals("growl")) s.growl = off ? 0 : 0.6f;
        }
        s.describe();
        return s;
    }

    /** One line: the character's qualities plus how this line is said. */
    public static VoiceStyle forLine(VoiceStyle base, String manner, boolean whisper, boolean echo) {
        VoiceStyle s = base == null ? new VoiceStyle() : base.copy();
        String m = manner == null ? "" : manner;
        if (hasWord(m, SHOUT)) { s.shout = 0.7f; s.ratePct += 6; s.pitchHz += 10; }
        if (hasWord(m, CRY)) { s.cry = 0.7f; s.ratePct -= 8; s.pitchHz += 6; s.breathy = Math.max(s.breathy, 0.25f); }
        if (hasWord(m, LAUGH)) { s.laugh = 0.6f; s.ratePct += 6; s.pitchHz += 6; }
        if (hasWord(m, PANT)) { s.breathy = Math.max(s.breathy, 0.45f); s.ratePct += 8; s.pitchHz += 4; }
        if (hasWord(m, SLEEPY)) { s.breathy = Math.max(s.breathy, 0.2f); s.ratePct -= 15; s.pitchHz -= 6; }
        if (hasWord(m, SING)) { s.sing = 0.7f; s.ratePct -= 10; }
        if (hasWord(m, SLOWLY)) s.ratePct -= 15;
        if (hasWord(m, QUICKLY)) s.ratePct += 15;
        if (hasWord(m, SOFTLY) && s.shout == 0) { s.soft = 0.5f; s.ratePct -= 5; s.pitchHz -= 3; }
        if (hasWord(m, FAR)) s.far = 0.7f;
        if (whisper) { s.breathy = Math.max(s.breathy, 0.85f); s.soft = Math.max(s.soft, 0.6f); }
        if (echo) s.boom = Math.max(s.boom, 0.25f);
        s.ratePct = Math.max(-35, Math.min(35, s.ratePct));
        s.pitchHz = Math.max(-20, Math.min(25, s.pitchHz));
        s.describe();
        return s;
    }

    VoiceStyle copy() {
        VoiceStyle s = new VoiceStyle();
        s.raspy = raspy; s.breathy = breathy; s.tremble = tremble; s.boom = boom; s.nasal = nasal; s.squeak = squeak; s.robot = robot;
        s.magic = magic; s.ghost = ghost; s.growl = growl;
        return s;
    }

    void describe() {
        words.clear();
        if (raspy > 0.3f) words.add("raspy");
        if (breathy > 0.6f) words.add("whispered"); else if (breathy > 0.3f) words.add("breathy");
        if (tremble > 0.3f) words.add("trembling");
        if (boom > 0.3f) words.add("booming");
        if (nasal > 0) words.add("nasal");
        if (squeak > 0) words.add("squeaky");
        if (robot > 0) words.add("robotic");
        if (magic > 0) words.add("magical echo");
        if (ghost > 0) words.add("ghostly");
        if (growl > 0.3f) words.add("growling");
        if (shout > 0) words.add("shouting");
        if (cry > 0) words.add("crying");
        if (laugh > 0) words.add("laughing");
        if (sing > 0) words.add("sing-song");
        if (far > 0) words.add("from far away");
    }

    public boolean any() {
        return raspy + breathy + tremble + boom + nasal + squeak + robot + magic + ghost + growl + shout + cry + laugh + sing + far + soft > 0
                || ratePct != 0 || pitchHz != 0;
    }

    /** Changes cached voices when the style changes. */
    public String signature() {
        return String.format(java.util.Locale.US, "%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%.2f,%d,%d",
                raspy, breathy, tremble, boom, nasal, squeak, robot, magic, ghost, growl, shout, cry, laugh, sing, far, soft, ratePct, pitchHz);
    }

    public String label() {
        StringBuilder b = new StringBuilder();
        for (String w : words) { if (b.length() > 0) b.append(", "); b.append(w); }
        return b.toString();
    }

    // ================================================================== sound processing

    /**
     * Applies the style to speech (mono PCM at sr). The level of the result matches the input (softer for
     * soft or far lines). aiVoice: the AI voice already acts the line, so only the character's qualities and
     * the place are added.
     */
    public float[] apply(float[] x, int sr, boolean aiVoice) {
        if (x == null || x.length < sr / 20) return x;
        float peakIn = peak(x);
        if (peakIn < 1e-4f) return x;
        Random rnd = new Random(x.length * 31L + 7);
        float[] y = x;
        float shout = aiVoice ? 0 : this.shout, cry = aiVoice ? 0 : this.cry, laugh = aiVoice ? 0 : this.laugh, sing = aiVoice ? 0 : this.sing;
        float breathy = aiVoice ? Math.min(this.breathy, 0.3f) : this.breathy;
        if (squeak > 0) y = VoiceFx.pitch(y, 1f + 0.4f * squeak);
        // pitch wobble: trembling, crying, singing, laughing
        float vibDepth = 0.05f * tremble + 0.05f * cry + 0.035f * sing + 0.02f * laugh + 0.015f * ghost;
        if (vibDepth > 0) {
            float rate = sing > 0 ? 5.5f : cry > 0 ? 7.5f : 6.2f;
            y = vibrato(y, sr, rate, vibDepth, tremble + cry > 0, rnd);
        }
        if (raspy > 0) y = rasp(y, sr, raspy, rnd);
        if (growl > 0) y = growl(y, sr, growl, rnd);
        if (nasal > 0) {
            y = biquad(y, sr, 2, 350, 0.7f, 0);
            y = biquad(y, sr, 0, 1250, 1.6f, 11 * nasal);
        }
        if (robot > 0) y = robot(y, sr, robot);
        if (breathy > 0) y = breath(y, sr, breathy, rnd);
        if (shout > 0) y = drive(y, shout);
        if (boom > 0) y = biquad(y, sr, 1, 180, 0.7f, 7 * boom);
        if (far > 0) y = biquad(y, sr, 3, 2400, 0.7f, 0);
        // loudness wobble
        float trem = 0.22f * tremble + 0.3f * cry + 0.35f * laugh + 0.2f * ghost;
        if (trem > 0) y = tremolo(y, sr, laugh > 0 ? 6.5f : cry > 0 ? 5f : 5.8f, Math.min(0.6f, trem), rnd);
        if (cry > 0) y = sobs(y, sr, cry, rnd);
        if (magic > 0) y = shimmer(y, sr, magic);
        float rev = Math.max(Math.max(boom * 0.2f, magic * 0.28f), Math.max(ghost * 0.4f, far * 0.3f));
        if (rev > 0.02f) {
            float t60 = ghost > 0 || magic > 0 ? 2.2f : boom > 0.4f ? 1.6f : 1.1f;
            y = reverb(y, sr, t60, rev);
        }
        float level = (1 - 0.35f * soft) * (1 - 0.4f * far) * (1 + 0.1f * shout);
        float pk = peak(y);
        if (pk > 1e-6f) {
            float g = peakIn * level / pk;
            for (int i = 0; i < y.length; i++) y[i] *= g;
        }
        return y;
    }

    static float peak(float[] x) {
        float m = 0;
        for (float v : x) m = Math.max(m, Math.abs(v));
        return m;
    }

    /** Follows the loudness of speech (fast attack, slower release). */
    static float[] envelope(float[] x, int sr) {
        float[] e = new float[x.length];
        float a = 1 - (float) Math.exp(-1.0 / (0.002 * sr)), r = 1 - (float) Math.exp(-1.0 / (0.03 * sr));
        float v = 0;
        for (int i = 0; i < x.length; i++) {
            float ax = Math.abs(x[i]);
            v += (ax > v ? a : r) * (ax - v);
            e[i] = v;
        }
        return e;
    }

    /** Pitch wobble by a gently moving delay; "shaky" makes the wobble uneven, like an old or crying voice. */
    static float[] vibrato(float[] x, int sr, float rate, float depth, boolean shaky, Random rnd) {
        float[] y = new float[x.length];
        double phase = 0;
        float maxD = (float) (depth * sr / (2 * Math.PI * rate));
        float base = maxD + 2;
        float wob = 1, target = 1;
        for (int i = 0; i < x.length; i++) {
            if (shaky && i % (sr / 8) == 0) target = 0.5f + rnd.nextFloat();
            wob += 0.0005f * (target - wob);
            phase += 2 * Math.PI * rate * (shaky ? 0.85f + 0.3f * wob : 1f) / sr;
            float d = base + maxD * wob * (float) Math.sin(phase);
            float p = i - d;
            int k = (int) Math.floor(p);
            float f = p - k;
            float a = k >= 0 && k < x.length ? x[k] : 0, b = k + 1 >= 0 && k + 1 < x.length ? x[k + 1] : 0;
            y[i] = a + (b - a) * f;
        }
        return y;
    }

    /** Uneven loudness from cycle to cycle plus a little hiss: a hoarse, raspy voice. */
    static float[] rasp(float[] x, int sr, float amt, Random rnd) {
        float[] e = envelope(x, sr);
        float[] y = new float[x.length];
        float j = 0, jt = 0, n1 = 0, lp = 0;
        int hold = sr / 140;
        for (int i = 0; i < x.length; i++) {
            if (i % hold == 0) jt = (rnd.nextFloat() * 2 - 1);
            j += 0.25f * (jt - j);
            float noise = rnd.nextFloat() * 2 - 1;
            float hp = noise - n1;
            n1 = noise;
            lp += 0.3f * (hp - lp);     // hiss kept in the voice's range, not a high whistle
            y[i] = x[i] * (1 + 0.7f * amt * j) + lp * e[i] * 0.35f * amt;
        }
        // a touch of soft saturation
        float pk = Math.max(1e-6f, peak(y));
        float k = 1 + 1.5f * amt;
        float tk = (float) Math.tanh(k);
        for (int i = 0; i < y.length; i++) y[i] = (float) Math.tanh(k * y[i] / pk) / tk * pk;
        return y;
    }

    /** A low, buzzing growl under the voice (giants, monsters, angry animals). */
    static float[] growl(float[] x, int sr, float amt, Random rnd) {
        float[] y = new float[x.length];
        double ph = 0;
        float f = 46, ft = 46;
        for (int i = 0; i < x.length; i++) {
            if (i % (sr / 20) == 0) ft = 38 + rnd.nextFloat() * 18;
            f += 0.002f * (ft - f);
            ph += 2 * Math.PI * f / sr;
            float m = (float) Math.sin(ph);
            y[i] = x[i] * (1 - 0.45f * amt + 0.45f * amt * m * Math.abs(m) + 0.25f * amt);
        }
        return y;
    }

    /** Metallic robot voice: ring modulation and a short resonant echo. */
    static float[] robot(float[] x, int sr, float amt) {
        float[] y = new float[x.length];
        int d = (int) (0.006f * sr);
        for (int i = 0; i < x.length; i++) {
            float ring = x[i] * (float) Math.sin(2 * Math.PI * 32 * i / sr);
            float v = x[i] * (1 - 0.6f * amt) + ring * 0.9f * amt;
            if (i >= d) v += y[i - d] * 0.55f * amt;
            y[i] = v;
        }
        return y;
    }

    /** Breath noise that follows the words; strong values become a whisper. */
    static float[] breath(float[] x, int sr, float amt, Random rnd) {
        float[] e = envelope(x, sr);
        float[] y = new float[x.length];
        float n1 = 0, lp = 0;
        float dry = 1 - 0.75f * Math.max(0, amt - 0.4f) / 0.6f;
        for (int i = 0; i < x.length; i++) {
            float noise = rnd.nextFloat() * 2 - 1;
            float hp = noise - n1;
            n1 = noise;
            lp += 0.3f * (hp - lp);    // breathy band, roughly 1-5 kHz
            y[i] = x[i] * dry + lp * e[i] * 2.2f * amt * amt;
        }
        return y;
    }

    /** Louder, edgier voice for shouting. */
    static float[] drive(float[] x, float amt) {
        float pk = Math.max(1e-6f, peak(x));
        float k = 1 + 2.2f * amt;
        float tk = (float) Math.tanh(k);
        float[] y = new float[x.length];
        float prev = 0;
        for (int i = 0; i < x.length; i++) {
            float v = (float) Math.tanh(k * x[i] / pk) / tk * pk;
            y[i] = v + 0.35f * amt * (v - prev);
            prev = v;
        }
        return y;
    }

    static float[] tremolo(float[] x, int sr, float rate, float depth, Random rnd) {
        float[] y = new float[x.length];
        double ph = rnd.nextFloat() * Math.PI;
        for (int i = 0; i < x.length; i++) {
            ph += 2 * Math.PI * rate / sr;
            y[i] = x[i] * (1 - depth * 0.5f * (1 + (float) Math.sin(ph)));
        }
        return y;
    }

    /** Short catches in the voice, like sobbing. */
    static float[] sobs(float[] x, int sr, float amt, Random rnd) {
        float[] y = x.clone();
        int at = (int) (sr * (0.5f + rnd.nextFloat()));
        while (at < y.length) {
            int len = (int) (sr * 0.09f);
            for (int i = 0; i < len && at + i < y.length; i++) {
                float w = (float) Math.sin(Math.PI * i / len);
                y[at + i] *= 1 - 0.65f * amt * w;
            }
            at += (int) (sr * (0.9f + rnd.nextFloat() * 0.9f));
        }
        return y;
    }

    /** A soft, sparkling double of the voice (fairies, gods, a voice from the sky). */
    static float[] shimmer(float[] x, int sr, float amt) {
        float[] up = VoiceFx.pitch(x, 2f);
        float[] y = new float[x.length];
        int d0 = (int) (0.012f * sr);
        for (int i = 0; i < x.length; i++) {
            float m1 = (float) (d0 + 0.003f * sr * Math.sin(2 * Math.PI * 0.27 * i / sr));
            float m2 = (float) (d0 + 0.004f * sr * Math.sin(2 * Math.PI * 0.41 * i / sr + 1));
            float c = tap(x, i - m1) * 0.4f + tap(x, i - m2) * 0.4f;
            y[i] = x[i] + amt * (c + (i < up.length ? up[i] * 0.12f : 0));
        }
        return y;
    }

    static float tap(float[] x, float p) {
        int k = (int) Math.floor(p);
        if (k < 0 || k + 1 >= x.length) return 0;
        float f = p - k;
        return x[k] + (x[k + 1] - x[k]) * f;
    }

    /** Room / hall / sky reverb (Schroeder: four combs and two all-passes). The tail is added at the end. */
    static float[] reverb(float[] x, int sr, float t60, float mix) {
        int tail = (int) (Math.min(t60, 2f) * 0.6f * sr);
        int n = x.length + tail;
        float[] wet = new float[n];
        float[] combMs = {29.7f, 37.1f, 41.1f, 43.7f};
        float size = 0.8f + 0.4f * Math.min(1, t60 / 2f);
        for (float ms : combMs) {
            int d = (int) (ms * size * sr / 1000);
            float g = (float) Math.pow(10, -3.0 * d / (t60 * sr));
            float[] buf = new float[d];
            int p = 0;
            float lp = 0;
            for (int i = 0; i < n; i++) {
                float in = i < x.length ? x[i] : 0;
                float out = buf[p];
                lp += 0.4f * (out - lp);          // high frequencies die away faster
                buf[p] = in + lp * g;
                wet[i] += out * 0.25f;
                p = (p + 1) % d;
            }
        }
        for (float ms : new float[]{5.0f, 1.7f}) {
            int d = Math.max(1, (int) (ms * sr / 1000));
            float[] buf = new float[d];
            int p = 0;
            for (int i = 0; i < n; i++) {
                float b = buf[p];
                float v = wet[i] + b * 0.7f;
                buf[p] = v;
                wet[i] = b - v * 0.7f;
                p = (p + 1) % d;
            }
        }
        float[] y = new float[n];
        for (int i = 0; i < n; i++) y[i] = (i < x.length ? x[i] * (1 - mix * 0.5f) : 0) + wet[i] * mix * 1.6f;
        return y;
    }

    /**
     * Second-order filters (RBJ cookbook). type 0 = peak, 1 = low shelf, 2 = high-pass, 3 = low-pass; gain in dB.
     */
    static float[] biquad(float[] x, int sr, int type, float f, float q, float gainDb) {
        double A = Math.pow(10, gainDb / 40), w = 2 * Math.PI * f / sr, cs = Math.cos(w), sn = Math.sin(w), al = sn / (2 * q);
        double b0, b1, b2, a0, a1, a2;
        switch (type) {
            case 0:
                b0 = 1 + al * A; b1 = -2 * cs; b2 = 1 - al * A; a0 = 1 + al / A; a1 = -2 * cs; a2 = 1 - al / A;
                break;
            case 1: {
                double sq = 2 * Math.sqrt(A) * al;
                b0 = A * ((A + 1) - (A - 1) * cs + sq); b1 = 2 * A * ((A - 1) - (A + 1) * cs); b2 = A * ((A + 1) - (A - 1) * cs - sq);
                a0 = (A + 1) + (A - 1) * cs + sq; a1 = -2 * ((A - 1) + (A + 1) * cs); a2 = (A + 1) + (A - 1) * cs - sq;
                break;
            }
            case 2:
                b0 = (1 + cs) / 2; b1 = -(1 + cs); b2 = (1 + cs) / 2; a0 = 1 + al; a1 = -2 * cs; a2 = 1 - al;
                break;
            default:
                b0 = (1 - cs) / 2; b1 = 1 - cs; b2 = (1 - cs) / 2; a0 = 1 + al; a1 = -2 * cs; a2 = 1 - al;
        }
        float[] y = new float[x.length];
        double x1 = 0, x2 = 0, y1 = 0, y2 = 0;
        for (int i = 0; i < x.length; i++) {
            double v = (b0 * x[i] + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2) / a0;
            x2 = x1; x1 = x[i]; y2 = y1; y1 = v;
            y[i] = (float) v;
        }
        return y;
    }
}
