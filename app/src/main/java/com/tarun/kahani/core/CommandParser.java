package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Understands after-preview commands in everyday Hindi / English / Hinglish, e.g.
 * "background music थोड़ा कम करो और वृंदा की आवाज़ तेज़ करो", "brightness बढ़ाओ", "make file smaller",
 * "khan ki awaaz moti karo", "subtitles लगाओ", "instagram के लिए बनाओ".
 * Returns edit commands for {@link Edits#apply}. Anything it cannot understand is returned in {@link Result#unknown}
 * so the app can ask an online AI (LLM) to translate it into the same commands.
 */
public final class CommandParser {

    public static final class Result {
        public final List<Map<String, Object>> commands = new ArrayList<Map<String, Object>>();
        public final List<String> unknown = new ArrayList<String>();
    }

    private static final String[] UP = {"बढ़ा", "बढा", "ज़्यादा", "ज्यादा", "अधिक", "तेज़", "तेज", "ऊँच", "ऊंच", "ऊपर", "increase", "more", "louder",
            "higher", "raise", "boost", "up", "brighter", "badha", "badhao", "jyada", "zyada", "tez", "bigger", "large"};
    private static final String[] DOWN = {"कम", "घटा", "धीम", "हल्क", "नीच", "decrease", "less", "lower", "reduce", "down", "softer", "quieter",
            "dim", "darker", "kam", "ghata", "smaller", "small", "compress", "dheema", "halka"};
    private static final String[] MUCH = {"बहुत", "काफ़ी", "काफी", "ज़्यादा", "a lot", "much", "very", "bahut", "double", "दोगुन"};
    private static final String[] LITTLE = {"थोड़ा", "थोडा", "थोड़ी", "हल्का सा", "slightly", "little", "bit", "thoda", "थोड़े"};
    private static final String[] OFF = {"हटा", "बंद", "निकाल", "मत", "off", "remove", "no ", "without", "hata", "band", "disable", "mute", "म्यूट"};
    private static final String[] ON = {"लगा", "जोड़", "जोड", "चालू", "दिखा", "add", "show", "on", "enable", "laga", "jod"};

    public static Result parse(String text, List<String> characterNames) {
        if (text != null) text = Hinglish.knownWords(text);   // "music kam karo" -> "music कम करो"
        Result r = new Result();
        if (text == null) return r;
        // split into separate requests
        String[] parts = text.split("(?i)\\s+और\\s+|\\s+तथा\\s+|\\s+फिर\\s+|\\s+and\\s+|\\s+then\\s+|,|।|\\.|;|\\n");
        String prev = null;
        Map<String, Object> prevCmd = null;
        for (String p : parts) {
            String q = p.trim();
            if (q.length() < 2) continue;
            Map<String, Object> c = one(q, characterNames);
            if (c == null && prev != null) {
                // "खान की आवाज़ और मोटी करो": the split was wrong, read both parts together
                Map<String, Object> merged = one(prev + " " + q, characterNames);
                if (merged != null && prevCmd != null && !merged.get("op").equals(prevCmd.get("op"))) {
                    r.commands.set(r.commands.indexOf(prevCmd), merged);
                    prevCmd = merged;
                    prev = prev + " " + q;
                    continue;
                }
            }
            if (c != null) { r.commands.add(c); prevCmd = c; }
            else { r.unknown.add(q); prevCmd = null; }
            prev = q;
        }
        return r;
    }

    /** Whole-word check (so "बंद" does not match inside "बंदर"). */
    static boolean word(String t, String... ws) {
        String n = " " + Txt.norm(t).replaceAll("[^\\p{L}\\p{M}0-9%]+", " ") + " ";
        for (String w : ws) {
            String x = Txt.norm(w).trim();
            if (n.contains(" " + x + " ")) return true;
            // allow Hindi verb endings: "बंद करो", "हटाओ"
            int i = n.indexOf(" " + x);
            while (i >= 0) {
                int e = i + 1 + x.length();
                if (e < n.length()) {
                    char ch = n.charAt(e);
                    if (ch == ' ' || (ch >= '\u093E' && ch <= '\u094D') || ch == 'ो' || ch == 'ा') {
                        // "बंदर": next char र is a consonant -> not a match; vowel signs are fine for verbs (हटाओ, बढ़ाओ)
                        if (!(x.equals(Txt.norm("बंद")) && ch != ' ')) return true;
                    }
                }
                i = n.indexOf(" " + x, i + 1);
            }
        }
        return false;
    }

    /** Consonant skeleton of a name in either script, so "Raju" matches "राजू" and "Vrinda" matches "वृंदा". */
    static String skeleton(String s) {
        StringBuilder b = new StringBuilder();
        String n = Txt.norm(s);
        for (int i = 0; i < n.length(); i++) {
            char c = n.charAt(i);
            String m = DEV.get(c);
            if (m != null) b.append(m);
            else if (c >= 'a' && c <= 'z' && "aeiouyh".indexOf(c) < 0) b.append(c == 'w' ? 'v' : c == 'z' ? 'j' : c == 'q' ? 'k' : c == 'c' ? 'k' : c);
        }
        // collapse doubles
        StringBuilder o = new StringBuilder();
        for (int i = 0; i < b.length(); i++) if (i == 0 || b.charAt(i) != b.charAt(i - 1)) o.append(b.charAt(i));
        return o.toString();
    }

    private static final java.util.Map<Character, String> DEV = new java.util.HashMap<Character, String>();
    static {
        String[][] m = {{"क", "k"}, {"ख", "k"}, {"ग", "g"}, {"घ", "g"}, {"च", "k"}, {"छ", "k"}, {"ज", "j"}, {"झ", "j"}, {"ट", "t"}, {"ठ", "t"},
                {"ड", "d"}, {"ढ", "d"}, {"ण", "n"}, {"त", "t"}, {"थ", "t"}, {"द", "d"}, {"ध", "d"}, {"न", "n"}, {"प", "p"}, {"फ", "f"},
                {"ब", "b"}, {"भ", "b"}, {"म", "m"}, {"य", ""}, {"र", "r"}, {"ल", "l"}, {"व", "v"}, {"श", "s"}, {"ष", "s"}, {"स", "s"},
                {"ह", ""}, {"ं", "n"}, {"ञ", "n"}, {"ङ", "n"}, {"ृ", "r"}};
        for (String[] e : m) DEV.put(e[0].charAt(0), e[1]);
    }

    static boolean nameIn(String text, String name) {
        for (String part : name.split("\\s+")) {
            if (part.length() < 2 || ScriptParser.isTitleWord(part)) continue;
            if (Txt.has(text, part)) return true;
            String sk = skeleton(part);
            if (sk.length() < 2) continue;
            for (String w : text.split("[^\\p{L}\\p{M}]+")) {
                if (w.length() >= 2 && skeleton(w).equals(sk)) return true;
            }
        }
        return false;
    }

    private static boolean has(String t, String... w) { return Txt.has(t, w); }

    private static float amount(String t, boolean up) {
        Matcher m = Pattern.compile("([0-9]+)\\s*(%|प्रतिशत|percent)").matcher(Txt.digitsToAscii(t));
        if (m.find()) {
            float p = Integer.parseInt(m.group(1)) / 100f;
            return up ? 1 + p : Math.max(0.05f, 1 - p);
        }
        boolean much = has(t, MUCH), little = has(t, LITTLE);
        if (up) return much ? 1.7f : little ? 1.15f : 1.35f;
        return much ? 0.45f : little ? 0.85f : 0.7f;
    }

    private static Map<String, Object> cmd(String op) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("op", op);
        return m;
    }

    private static Map<String, Object> one(String t, List<String> names) {
        boolean up = has(t, UP) && !(has(t, "कम") && Txt.firstIndex(t, "कम") > Txt.firstIndex(t, UP));
        boolean down = has(t, DOWN) && !up;
        // ---------- reset
        if (has(t, "reset", "पहले जैसा", "पहले जैसी", "undo all", "सब हटा", "original")) return cmd("reset");
        // ---------- platform / aspect
        if (has(t, "instagram", "इंस्टा", "reel", "रील", "shorts", "vertical", "खड़ा", "9:16")) { Map<String, Object> c = cmd("aspect"); c.put("value", "9:16"); return c; }
        if (has(t, "youtube", "यूट्यूब", "horizontal", "आड़ा", "16:9", "landscape")) { Map<String, Object> c = cmd("aspect"); c.put("value", "16:9"); return c; }
        if (has(t, "square", "वर्गाकार", "1:1")) { Map<String, Object> c = cmd("aspect"); c.put("value", "1:1"); return c; }
        if (has(t, "facebook", "फेसबुक")) { Map<String, Object> c = cmd("aspect"); c.put("value", "16:9"); return c; }
        // ---------- resolution
        Matcher res = Pattern.compile("(2160|1440|1080|720|480|360)\\s*p?").matcher(t);
        if (res.find() && !has(t, "%")) { Map<String, Object> c = cmd("resolution"); c.put("value", (double) Math.min(1080, Integer.parseInt(res.group(1)))); return c; }
        if (has(t, "full hd", "fullhd", "फुल एचडी")) { Map<String, Object> c = cmd("resolution"); c.put("value", 1080.0); return c; }
        // ---------- subtitles
        if (has(t, "subtitle", "उपशीर्षक", "caption", "सबटाइटल", "कैप्शन", "likha", "लिखे")) {
            Map<String, Object> c = cmd("subtitles");
            c.put("on", !word(t, "हटा", "हटाओ", "बंद", "off", "remove", "नहीं", "मत", "without", "no"));
            return c;
        }
        // ---------- file size
        if (has(t, "file size", "size", "साइज़", "साइज", "आकार", "mb", "एमबी", "compress", "फ़ाइल", "file")) {
            Map<String, Object> c = cmd("file_size");
            c.put("factor", (double) (has(t, "कम", "छोटा", "छोटी", "small", "reduce", "compress", "decrease", "less", "kam") ? 0.5f : 1.5f));
            return c;
        }
        // ---------- picture
        if (has(t, "brightness", "चमक", "उजाला", "रोशनी", "bright", "रौशनी", "andhera", "अंधेरा", "dark")) {
            Map<String, Object> c = cmd("brightness");
            boolean darkWord = has(t, "अंधेरा", "andhera", "dark");
            boolean darker;
            if (darkWord) {
                // "too dark" / "अंधेरा बहुत है" is a complaint -> brighter; "make it darker" / "अंधेरा करो" -> darker
                boolean complaint = has(t, "बहुत", "too", "ज़्यादा", "ज्यादा", "zyada", "है", "hai", "लग", "lag", "कम", "less", "reduce");
                darker = has(t, "darker", "more dark", "अंधेरा करो", "अंधेरा बढ़", "dark करो", "make it dark") || (!complaint && up);
            } else {
                darker = down && !up;
            }
            float a = amount(t, true) - 1;
            c.put("factor", (double) (1 + (darker ? -a * 0.6f : a * 0.6f)));
            return c;
        }
        if (has(t, "contrast", "कंट्रास्ट")) { Map<String, Object> c = cmd("contrast"); c.put("factor", (double) amount(t, !down)); return c; }
        if (has(t, "saturation", "रंग", "colour", "color", "कलर", "rang")) {
            Map<String, Object> c = cmd(has(t, "गर्म", "warm", "सुनहर", "golden") || has(t, "ठंड", "cool", "नीला", "blue") ? "warmth" : "saturation");
            if (c.get("op").equals("warmth")) c.put("factor", (double) (has(t, "ठंड", "cool", "नीला", "blue") ? 0.7f : 1.3f));
            else c.put("factor", (double) amount(t, !down));
            return c;
        }
        if (has(t, "गर्म", "warm", "golden")) { Map<String, Object> c = cmd("warmth"); c.put("factor", 1.3); return c; }
        if (has(t, "ठंड", "cool tone", "cooler")) { Map<String, Object> c = cmd("warmth"); c.put("factor", 0.7); return c; }
        // ---------- a character's voice
        String who = null;
        if (names != null) for (String n : names) if (nameIn(t, n)) { who = n; break; }
        boolean voiceWord = has(t, "आवाज़", "आवाज", "voice", "awaaz", "awaz", "volume", "बोल", "sound", "level");
        if (who != null) {
            if (has(t, "pitch", "पिच", "सुर", "मोटी", "मोटा", "भारी", "पतली", "पतला", "deeper", "deep", "higher pitch", "moti", "patli", "bhari")) {
                Map<String, Object> c = cmd("voice_pitch");
                c.put("who", who);
                boolean lower = has(t, "मोटी", "मोटा", "भारी", "deeper", "deep", "moti", "bhari", "lower", "नीच") || (down && !has(t, "पतली", "पतला"));
                c.put("factor", (double) (lower ? 0.85f : 1.15f));
                return c;
            }
            if (has(t, "slow", "धीरे", "धीमा", "धीमी", "fast", "जल्दी", "speed", "रफ़्तार", "गति", "dheere", "jaldi")) {
                Map<String, Object> c = cmd("voice_rate");
                c.put("who", who);
                c.put("factor", (double) (has(t, "slow", "धीरे", "धीमा", "धीमी", "dheere") ? 0.85f : 1.15f));
                return c;
            }
            Map<String, Object> c = cmd("voice_gain");
            c.put("who", who);
            if (word(t, "mute", "म्यूट", "बंद", "chup", "चुप")) c.put("value", 0.0);
            else c.put("factor", (double) amount(t, !down));
            return c;
        }
        // ---------- mix
        if (has(t, "music", "संगीत", "म्यूज़िक", "म्यूजिक", "गाना", "धुन", "song", "bgm", "gaana")) {
            Map<String, Object> c = cmd("music");
            if (word(t, "mute", "म्यूट", "बंद", "हटा", "हटाओ", "off", "remove")) c.put("value", 0.0); else c.put("factor", (double) amount(t, !down));
            return c;
        }
        if (has(t, "narrator", "कथावाचक", "सूत्रधार")) { Map<String, Object> c = cmd("narrator"); c.put("factor", (double) amount(t, !down)); return c; }
        if (has(t, "background sound", "background noise", "पृष्ठभूमि", "बैकग्राउंड", "ambience", "माहौल", "आसपास", "nature", "प्राकृतिक")) {
            Map<String, Object> c = cmd("ambience");
            if (word(t, "mute", "म्यूट", "बंद", "हटा", "हटाओ", "off")) c.put("value", 0.0); else c.put("factor", (double) amount(t, !down));
            return c;
        }
        if (has(t, "effect", "इफ़ेक्ट", "इफेक्ट", "ध्वनि", "धमाके", "sfx")) {
            Map<String, Object> c = cmd("sfx");
            if (word(t, "mute", "म्यूट", "बंद", "हटा", "हटाओ", "off")) c.put("value", 0.0); else c.put("factor", (double) amount(t, !down));
            return c;
        }
        if (voiceWord && has(t, "सब", "all", "dialogue", "संवाद", "सभी", "voices")) {
            Map<String, Object> c = cmd("voices"); c.put("factor", (double) amount(t, !down)); return c;
        }
        if (has(t, "slow", "धीमा", "धीमी", "fast", "तेज़ चल", "speed", "रफ़्तार", "pace")) {
            Map<String, Object> c = cmd("speed");
            c.put("factor", (double) (has(t, "slow", "धीमा", "धीमी") ? 0.85f : 1.15f));
            return c;
        }
        if (voiceWord) { Map<String, Object> c = cmd("voices"); c.put("factor", (double) amount(t, !down)); return c; }
        return null;
    }
}
