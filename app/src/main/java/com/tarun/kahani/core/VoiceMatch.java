package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Matches voices to characters using the voice the script describes ("भारी आवाज़", "मीठी, सुरीली आवाज़",
 * "squeaky", "raspy and slow"…) together with the character's age, gender and kind.
 * Voice samples are measured (pitch, how lively the pitch is, speaking speed, brightness, roughness) and
 * compared with what the description asks for.
 */
public final class VoiceMatch {
    private VoiceMatch() {}

    /** What a character's voice should be like. */
    public static final class Want {
        public float pitchLo = 85, pitchHi = 300;
        public boolean deep, high, soft, rough, slow, fast, loud, lively;
        public final List<String> words = new ArrayList<String>();   // e.g. "deep", "sweet" (shown to the user)
    }

    static final String[] DEEP = {"भारी", "गहरी", "गहरा", "मोटी आवाज़", "मोटा", "गंभीर", "गरजती", "गरजदार", "रौबदार", "booming", "deep", "low voice",
            "heavy voice", "gruff", "baritone", "bass", "bhari", "gehri"};
    static final String[] HIGH = {"पतली", "ऊँची आवाज़", "ऊंची आवाज़", "तीखी", "चहकती", "बारीक", "squeaky", "high-pitched", "high pitched", "shrill",
            "thin voice", "chirpy", "patli"};
    static final String[] SOFT = {"मीठी", "कोमल", "मधुर", "प्यारी आवाज़", "नरम", "सुरीली", "सम्मोहक", "शांत", "sweet", "soft", "gentle", "melodious",
            "warm voice", "calm", "soothing", "meethi", "komal"};
    static final String[] ROUGH = {"खरखरी", "कर्कश", "फटी", "भर्राई", "खुरदरी", "raspy", "hoarse", "rough", "croaky", "crackly", "harsh voice", "gravelly"};
    static final String[] SLOW = {"धीमी", "धीरे", "ठहर", "slow", "drawl", "dheemi"};
    static final String[] FAST = {"तेज़ आवाज़", "चंचल", "फुर्तीली", "जल्दी-जल्दी", "fast", "quick", "chatty", "lively", "bubbly", "chanchal"};
    static final String[] LOUD = {"ज़ोरदार", "जोरदार", "गूँजती", "गूंजती", "कड़क", "loud", "booming", "thundering", "echoing", "commanding"};

    /** The parts of a description that talk about the voice (or the whole text if it never says "voice"). */
    static String voiceText(String d) {
        if (d == null) return "";
        StringBuilder b = new StringBuilder();
        for (String part : d.split("[।.;\n]")) if (Txt.has(part, "आवाज़", "आवाज", "स्वर", "voice", "awaaz", "बोल", "speaks", "talks")) b.append(part).append(' ');
        return b.length() > 0 ? b.toString() : d;
    }

    public static Want want(Story.CharacterDef c) {
        Want w = new Want();
        Look l = c == null ? null : c.look;
        if (l != null) {
            switch (l.kind) {
                case Look.GIRL: case Look.BOY: w.pitchLo = c.age > 0 && c.age <= 8 ? 250 : 215; w.pitchHi = 360; w.lively = true; break;
                case Look.WOMAN: w.pitchLo = 160; w.pitchHi = 265; break;
                case Look.WITCH: w.pitchLo = 150; w.pitchHi = 280; break;
                case Look.OLD_MAN: w.pitchLo = 75; w.pitchHi = 140; w.slow = true; break;
                case Look.MONSTER: w.pitchLo = 55; w.pitchHi = 120; w.deep = true; w.loud = true; break;
                case Look.MONKEY: w.pitchLo = 220; w.pitchHi = 420; w.fast = true; break;
                case Look.ANIMAL: case Look.BIRD:
                    if (l.height < 0.4f) { w.pitchLo = 220; w.pitchHi = 420; } else if (l.height > 0.9f) { w.pitchLo = 60; w.pitchHi = 140; }
                    break;
                default:
                    if (l.female) { w.pitchLo = 160; w.pitchHi = 265; } else { w.pitchLo = 85; w.pitchHi = 165; }
            }
            if (c.age >= 60 && l.kind != Look.OLD_MAN) w.slow = true;
        }
        String v = voiceText(c == null ? "" : c.description);
        if (Txt.has(v, DEEP)) { w.deep = true; w.words.add("deep"); }
        if (Txt.has(v, HIGH)) { w.high = true; w.words.add("high"); }
        if (Txt.has(v, SOFT)) { w.soft = true; w.words.add("sweet / soft"); }
        if (Txt.has(v, ROUGH)) { w.rough = true; w.words.add("raspy"); }
        if (Txt.has(v, SLOW)) { w.slow = true; w.words.add("slow"); }
        if (Txt.has(v, FAST)) { w.fast = true; w.words.add("lively / fast"); }
        if (Txt.has(v, LOUD)) { w.loud = true; w.words.add("loud / booming"); }
        if (w.deep && !w.high) { w.pitchLo *= 0.85f; w.pitchHi *= 0.9f; }
        if (w.high && !w.deep) { w.pitchLo *= 1.1f; w.pitchHi *= 1.15f; }
        return w;
    }

    /**
     * How well a measured voice sample fits (0..1). vf = {median pitch Hz, pitch liveliness (semitones),
     * syllables per second, brightness (spectral centre Hz), roughness 0..1}.
     */
    public static float score(float[] vf, Want w) {
        if (vf == null || vf.length < 5 || vf[0] <= 0) return 0;
        float p = vf[0];
        float s = 0, tw = 0;
        // pitch range (most important)
        float semis = 0;
        if (p < w.pitchLo) semis = (float) (12 * Math.log(w.pitchLo / p) / Math.log(2));
        else if (p > w.pitchHi) semis = (float) (12 * Math.log(p / w.pitchHi) / Math.log(2));
        s += 3f * Math.max(0, 1 - semis / 7f);
        tw += 3f;
        float mid = (float) Math.sqrt(w.pitchLo * w.pitchHi);
        if (w.deep) { tw += 1; s += p < mid ? 1 : 0.2f; tw += 0.5f; s += vf[3] < 1600 ? 0.5f : 0.1f; }
        if (w.high) { tw += 1; s += p > mid ? 1 : 0.2f; }
        if (w.soft) { tw += 1; s += vf[4] < 0.35f ? 1 : 0.3f; tw += 0.5f; s += vf[3] < 2200 ? 0.5f : 0.2f; }
        if (w.rough) { tw += 1; s += vf[4] > 0.45f ? 1 : 0.2f; }
        // speaking speed (measured syllables per second of speech; ordinary speech is about 6-7.5)
        if (w.slow) { tw += 1; s += clamp((7.2f - vf[2]) / 2.4f); }
        if (w.fast) { tw += 1; s += clamp((vf[2] - 6.0f) / 2.4f); }
        if (w.lively) { tw += 0.5f; s += vf[1] > 2.5f ? 0.5f : 0.1f; }
        return s / tw;
    }

    static float clamp(float v) { return Math.max(0.15f, Math.min(1, v)); }

    /** The studio's natural voice adjusted to the described voice (deeper, higher, slower, faster…). */
    public static EdgeVoice.Cast adjust(EdgeVoice.Cast c, Want w) {
        if (c == null || w == null) return c;
        int p = c.pitchHz, r = c.ratePct;
        if (w.deep && !w.high) p -= 12;
        if (w.high && !w.deep) p += 14;
        if (w.slow && !w.fast) r -= 9;
        if (w.fast && !w.slow) r += 9;
        if (w.soft) r -= 3;
        if (w.loud && w.deep) p -= 6;
        // pushed further the neural voice stops sounding natural
        p = Math.max(-26, Math.min(44, p));
        r = Math.max(-22, Math.min(20, r));
        return new EdgeVoice.Cast(c.voice, p, r);
    }

    /** What the sample has that the character needs, e.g. "female, sweet, slow" (empty if nothing fits). */
    public static String fits(float[] vf, Want w) {
        if (vf == null || vf.length < 5 || vf[0] <= 0) return "";
        List<String> o = new ArrayList<String>();
        float p = vf[0];
        float mid = (float) Math.sqrt(w.pitchLo * w.pitchHi);
        if (p >= w.pitchLo * 0.94f && p <= w.pitchHi * 1.06f) o.add(p < 120 ? "deep male" : p < 175 ? "male" : p < 245 ? "female" : "child-like");
        if (w.deep && p < mid) o.add("deep");
        if (w.high && p > mid) o.add("high");
        if (w.soft && vf[4] < 0.35f) o.add("sweet / smooth");
        if (w.rough && vf[4] > 0.45f) o.add("raspy");
        if (w.slow && vf[2] < 6.2f) o.add("slow");
        if (w.fast && vf[2] > 7f) o.add("fast");
        StringBuilder b = new StringBuilder();
        for (String s : o) { if (b.length() > 0) b.append(", "); b.append(s); }
        return b.toString();
    }

    /**
     * Gives each character at most one sample and each sample to at most one character, taking the best
     * fits first. Returns, for every character, the index of its sample or -1.
     */
    public static int[] assign(List<float[]> samples, List<Want> wants, float min) {
        int nc = wants.size(), ns = samples.size();
        int[] out = new int[nc];
        java.util.Arrays.fill(out, -1);
        boolean[] used = new boolean[ns];
        float[][] sc = new float[nc][ns];
        for (int c = 0; c < nc; c++) for (int s = 0; s < ns; s++) sc[c][s] = score(samples.get(s), wants.get(c));
        while (true) {
            int bc = -1, bs = -1;
            float best = min;
            for (int c = 0; c < nc; c++) {
                if (out[c] >= 0) continue;
                for (int s = 0; s < ns; s++) if (!used[s] && sc[c][s] >= best) { best = sc[c][s]; bc = c; bs = s; }
            }
            if (bc < 0) break;
            out[bc] = bs;
            used[bs] = true;
        }
        return out;
    }

    /** Reads the measurements stored with a library voice ("pitch,liveliness,rate,brightness,roughness"). */
    public static float[] parse(String s) {
        if (s == null || s.length() == 0) return null;
        String[] p = s.split(",");
        if (p.length < 5) return null;
        float[] v = new float[5];
        try {
            for (int i = 0; i < 5; i++) v[i] = Float.parseFloat(p[i]);
        } catch (NumberFormatException e) {
            return null;
        }
        return v;
    }

    public static String format(float[] v) {
        return String.format(java.util.Locale.US, "%.0f,%.2f,%.2f,%.0f,%.3f", v[0], v[1], v[2], v[3], v[4]);
    }

    /** Short words describing a measured sample, e.g. "female, lively, bright". */
    public static String describe(float[] vf) {
        if (vf == null || vf.length < 5 || vf[0] <= 0) return "";
        List<String> o = new ArrayList<String>();
        float p = vf[0];
        o.add(p < 120 ? "deep male" : p < 175 ? "male" : p < 245 ? "female" : "child-like");
        if (vf[2] > 8.2f) o.add("fast"); else if (vf[2] < 5f) o.add("slow");
        if (vf[4] > 0.45f) o.add("raspy"); else if (vf[4] < 0.25f) o.add("smooth");
        if (vf[1] > 3f) o.add("lively");
        StringBuilder b = new StringBuilder();
        for (String s : o) { if (b.length() > 0) b.append(", "); b.append(s); }
        return b.toString();
    }
}
