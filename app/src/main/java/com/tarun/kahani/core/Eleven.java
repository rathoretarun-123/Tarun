package com.tarun.kahani.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * ElevenLabs voices (optional, with the user's own key, typed into the app and kept only on the phone):
 * the most lifelike voices available, speaking Hindi and English with the multilingual model. Each character
 * gets its own voice from the account's voice list, chosen by gender, age and the voice the script describes,
 * and keeps it for the whole story; feelings change how steady or expressive the voice is.
 */
public final class Eleven {
    public String key = "";
    public String base = "https://api.elevenlabs.io/v1/";
    public String model = "eleven_multilingual_v2";
    final Cloud http;

    public Eleven(Cloud http, String key) { this.http = http; this.key = key == null ? "" : key.trim(); }

    public boolean ready() { return key.length() > 10; }

    public static final class Voice {
        public String id = "", name = "", gender = "", age = "", accent = "", description = "";
        public String toString() { return name + " (" + gender + ", " + age + (accent.length() > 0 ? ", " + accent : "") + ")"; }
    }

    /** The voices this account can use (the ready-made ones and any the user made or added). */
    public List<Voice> voices() throws IOException {
        Object r = Json.parseLoose(new String(http.request("GET", base + "voices", null, null, new String[]{"xi-api-key", key}), "UTF-8"));
        List<Voice> out = new ArrayList<Voice>();
        List<Object> vs = Json.arr(r, "voices");
        if (vs != null) for (Object x : vs) {
            Voice v = new Voice();
            v.id = Json.str(x, "voice_id", "");
            v.name = Json.str(x, "name", "");
            Object l = Json.obj(x, "labels");
            v.gender = Json.str(l, "gender", "").toLowerCase(Locale.ROOT);
            v.age = Json.str(l, "age", "").toLowerCase(Locale.ROOT).replace('_', ' ');
            v.accent = Json.str(l, "accent", "").toLowerCase(Locale.ROOT);
            v.description = (Json.str(l, "description", "") + " " + Json.str(l, "descriptive", "") + " " + Json.str(x, "description", "")).toLowerCase(Locale.ROOT).trim();
            if (v.id.length() > 0) out.add(v);
        }
        return out;
    }

    /** One line as MP3. Feelings: lower stability = more expressive; style = how much acting. */
    public byte[] speak(String text, String voiceId, int emotion, boolean whisper) throws IOException {
        float stability = 0.5f, style = 0.2f;
        switch (emotion) {
            case Pose.ANGRY: case Pose.SCARED: case Pose.SURPRISED: case Pose.LAUGH: case Pose.EVIL: stability = 0.3f; style = 0.5f; break;
            case Pose.HAPPY: case Pose.PROUD: case Pose.DETERMINED: stability = 0.38f; style = 0.38f; break;
            case Pose.SAD: case Pose.PAIN: stability = 0.42f; style = 0.35f; break;
            default:
        }
        if (whisper) { stability = 0.6f; style = 0.1f; }
        Map<String, Object> vs = new LinkedHashMap<String, Object>();
        vs.put("stability", (double) stability);
        vs.put("similarity_boost", 0.8);
        vs.put("style", (double) style);
        vs.put("use_speaker_boost", Boolean.TRUE);
        Map<String, Object> body = new LinkedHashMap<String, Object>();
        body.put("text", text);
        body.put("model_id", model);
        body.put("voice_settings", vs);
        byte[] mp3 = http.request("POST", base + "text-to-speech/" + voiceId + "?output_format=mp3_44100_128", "application/json; charset=utf-8",
                Json.write(body).getBytes("UTF-8"), new String[]{"xi-api-key", key, "Accept", "audio/mpeg"});
        if (mp3.length < 600) throw new IOException("ElevenLabs returned no sound");
        return mp3;
    }

    /** How well a voice fits a character (higher is better). */
    public static float score(Voice v, Look look, int age, String voiceWords, boolean hindi) {
        boolean female = look != null && look.female;
        boolean child = (look != null && look.isChild()) || (age > 0 && age < 15);
        boolean old = (look != null && look.kind == Look.OLD_MAN) || age >= 65;
        String words = voiceWords == null ? "" : voiceWords.toLowerCase(Locale.ROOT);
        // the script's Hindi / Hinglish voice words in the words ElevenLabs uses to describe its voices
        String[][] hi = {{"भारी", "deep"}, {"गहरी", "deep"}, {"bhaari", "deep"}, {"gehri", "deep"}, {"मीठी", "sweet"}, {"meethi", "sweet"},
                {"कोमल", "soft"}, {"धीमी", "calm"}, {"शांत", "calm"}, {"खरखरी", "raspy"}, {"कर्कश", "raspy"}, {"चंचल", "playful"},
                {"chanchal", "playful"}, {"रोबदार", "authoritative"}, {"बुलंद", "strong"}, {"गरजती", "strong"}, {"सुरीली", "sweet"}};
        for (String[] m : hi) if (words.contains(m[0])) words += " " + m[1];
        float s = 0;
        if (v.gender.length() > 0) s += v.gender.startsWith(female ? "f" : "m") ? 10 : -10;
        boolean young = v.age.contains("young"), aged = v.age.contains("old") || v.age.contains("elder");
        if (child) s += young ? 4 : aged ? -4 : 0;
        else if (old) s += aged ? 4 : young ? -2 : 1;
        else s += v.age.contains("middle") ? 2 : young ? 1 : aged ? -1 : 0;
        if (hindi && (v.accent.contains("indian") || v.accent.contains("hindi"))) s += 3;
        for (String w : new String[]{"deep", "warm", "soft", "calm", "raspy", "gravelly", "strong", "gentle", "sweet", "playful", "authoritative", "husky", "bright", "energetic"}) {
            if (words.contains(w) && v.description.contains(w)) s += 3;
        }
        return s;
    }

    /** The best voice for one character; voices already given to others only when none is left. */
    public static Voice pick(List<Voice> voices, Look look, int age, String voiceWords, boolean hindi, Set<String> used) {
        if (voices == null || voices.isEmpty()) return null;
        Voice best = null;
        float bestScore = -1e9f;
        for (Voice v : voices) {
            float s = score(v, look, age, voiceWords, hindi) - (used != null && used.contains(v.id) ? 6 : 0);
            if (s > bestScore) { bestScore = s; best = v; }
        }
        return best;
    }

    /**
     * Voices for a whole cast at once: the closest fits are settled first (a deep-voiced king and a raspy old
     * grandfather each get the voice that suits them best), every character a different voice while there are
     * enough. looks[i], ages[i], words[i] describe character i; returns a voice index per character.
     */
    public static int[] assign(List<Voice> voices, Look[] looks, int[] ages, String[] words, boolean hindi) {
        int n = looks.length, m = voices == null ? 0 : voices.size();
        int[] out = new int[n];
        java.util.Arrays.fill(out, -1);
        if (m == 0) return out;
        float[][] sc = new float[n][m];
        for (int i = 0; i < n; i++) for (int j = 0; j < m; j++) sc[i][j] = score(voices.get(j), looks[i], ages[i], words[i], hindi);
        boolean[] usedV = new boolean[m];
        int left = n;
        while (left > 0) {
            boolean allUsed = true;
            for (boolean u : usedV) if (!u) { allUsed = false; break; }
            if (allUsed) java.util.Arrays.fill(usedV, false);     // more characters than voices: start sharing
            float best = -1e9f;
            int bi = -1, bj = -1;
            for (int i = 0; i < n; i++) {
                if (out[i] >= 0) continue;
                for (int j = 0; j < m; j++) if (!usedV[j] && sc[i][j] > best) { best = sc[i][j]; bi = i; bj = j; }
            }
            if (bi < 0) break;
            out[bi] = bj;
            usedV[bj] = true;
            left--;
        }
        return out;
    }
}
