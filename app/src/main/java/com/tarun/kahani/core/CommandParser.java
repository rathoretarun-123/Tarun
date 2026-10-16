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
            "higher", "raise", "boost", "up", "brighter", "badha", "badhao", "jyada", "zyada", "tez", "bigger", "large", "larger", "stronger",
            "amplify", "enhance", "turn up", "pump up", "clearer", "vivid", "punchier"};
    private static final String[] DOWN = {"कम", "घटा", "धीम", "हल्क", "नीच", "decrease", "less", "lower", "reduce", "down", "softer", "quieter",
            "dim", "darker", "kam", "ghata", "smaller", "small", "compress", "dheema", "halka", "turn down", "tone down", "quiet", "calmer",
            "weaker", "subtle", "subtler", "gentler", "lighter", "drop", "cut", "shrink"};
    private static final String[] MUCH = {"बहुत", "काफ़ी", "काफी", "ज़्यादा", "a lot", "much", "very", "bahut", "double", "दोगुन", "way", "really",
            "significantly", "a great deal"};
    private static final String[] LITTLE = {"थोड़ा", "थोडा", "थोड़ी", "हल्का सा", "slightly", "little", "bit", "thoda", "थोड़े", "touch", "tad",
            "somewhat", "a little"};
    private static final String[] OFF = {"हटा", "बंद", "निकाल", "मत", "off", "remove", "no ", "without", "hata", "band", "disable", "mute", "म्यूट"};
    private static final String[] ON = {"लगा", "जोड़", "जोड", "चालू", "दिखा", "add", "show", "on", "enable", "laga", "jod"};

    public static Result parse(String text, List<String> characterNames) {
        Result r = new Result();
        if (text == null) return r;
        text = text.replaceAll("(?i)black\\s*(and|&)\\s*white", "black-white");
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
            // "make Khan deeper and a bit slower": the second part is about Khan too
            if (prevCmd != null && prevCmd.containsKey("who") && !mentionsSubject(q, characterNames)) {
                String who = (String) prevCmd.get("who");
                boolean sUp = has(q, UP), sDown = has(q, DOWN);
                if (c != null && "speed".equals(c.get("op"))) { c.put("op", "voice_rate"); c.put("who", who); }
                else if (c == null && (sUp || sDown || has(q, "louder", "softer", "quieter"))) {
                    c = cmd(has(q, "slow", "fast", "quick") ? "voice_rate" : has(q, "deep", "high", "squeak") ? "voice_pitch" : "voice_gain");
                    c.put("who", who);
                    c.put("factor", (double) (c.get("op").equals("voice_gain") ? amount(q, !sDown) : (has(q, "slow", "deep") ? 0.85f : 1.15f)));
                }
            }
            // "the film drags, speed it up" is one request, not two
            if (c != null && prevCmd != null && c.get("op").equals(prevCmd.get("op"))
                    && String.valueOf(c.get("who")).equals(String.valueOf(prevCmd.get("who")))) { prev = q; continue; }
            if (c != null) { r.commands.add(c); prevCmd = c; }
            else { r.unknown.add(q); prevCmd = null; }
            prev = q;
        }
        return r;
    }

    /** Does this part name its own subject (music, picture, a character…)? */
    static boolean mentionsSubject(String q, List<String> names) {
        if (has(q, "music", "song", "film", "video", "movie", "background", "subtitle", "caption", "bright", "colour", "color", "effect",
                "narrator", "file", "संगीत", "फ़िल्म", "वीडियो")) return true;
        if (names != null) for (String n : names) if (nameIn(q, n)) return true;
        return false;
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

    /** English role words and the Hindi words a character's name may carry ("the king" finds "राजा तरुण"). */
    static final String[][] ROLES = {
            {"king", "राजा"}, {"queen", "रानी"}, {"princess", "राजकुमारी"}, {"prince", "राजकुमार"}, {"witch", "चुड़ैल", "डायन"},
            {"monster", "राक्षस", "दानव"}, {"demon", "राक्षस", "दानव"}, {"giant", "राक्षस", "दैत्य"}, {"monkey", "बंदर"},
            {"guard", "गार्ड", "सिपाही", "पहरेदार"}, {"soldier", "सैनिक", "सिपाही"}, {"mother", "माँ", "माता", "अम्मा"}, {"mom", "माँ", "माता"},
            {"father", "पिता", "बापू"}, {"dad", "पिता", "बापू"}, {"grandmother", "दादी", "नानी"}, {"granny", "दादी", "नानी"},
            {"grandfather", "दादा", "नाना"}, {"grandpa", "दादा", "नाना"}, {"teacher", "गुरु", "अध्यापक", "मास्टर"}, {"ghost", "भूत"},
            {"fairy", "परी"}, {"lion", "शेर", "सिंह"}, {"tiger", "बाघ"}, {"elephant", "हाथी"}, {"crow", "कौवा", "कौआ"}, {"fox", "लोमड़ी"},
            {"rabbit", "खरगोश"}, {"tortoise", "कछुआ"}, {"sage", "ऋषि", "साधु", "बाबा"}, {"minister", "मंत्री"}, {"aunt", "आंटी", "चाची", "मौसी"},
            {"uncle", "चाचा", "मामा", "अंकल"}, {"farmer", "किसान"}, {"merchant", "सेठ", "व्यापारी"}, {"thief", "चोर"}, {"dog", "कुत्ता"},
            {"cat", "बिल्ली"}, {"parrot", "तोता"}, {"snake", "साँप", "नाग"}, {"owl", "उल्लू"}, {"bear", "भालू"}, {"wolf", "भेड़िया"},
    };

    static boolean nameIn(String text, String name) {
        String low = " " + text.toLowerCase(java.util.Locale.ROOT).replaceAll("[^a-z]+", " ") + " ";
        for (String[] r : ROLES) {
            if (!low.contains(" " + r[0] + " ") && !low.contains(" " + r[0] + "s ") && !low.contains(" " + r[0] + "'s ")) continue;
            for (int i = 1; i < r.length; i++) if (Txt.has(name, r[i])) return true;
        }
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

    /** The character the request names (its name, or a word like "monkey" / "बंदर" matching a name), or null. */
    static String subject(String t, List<String> names) {
        if (names == null) return null;
        String best = null;
        for (String n : names) {
            if (n == null || n.length() < 2) continue;
            if (Txt.norm(t).contains(Txt.norm(n)) && (best == null || n.length() > best.length())) best = n;
            else {
                String first = n.split("\\s+")[0];
                if (first.length() >= 2 && Txt.norm(t).contains(Txt.norm(first)) && best == null) best = n;
            }
        }
        if (best == null) {
            String[][] kinds = {{"monkey", "बंदर", "राजू"}, {"monster", "राक्षस", "beast", "दानव"}, {"bull", "बैल"}, {"witch", "चुड़ैल"}, {"king", "राजा"}, {"queen", "रानी"}};
            for (String[] k : kinds) if (has(t, k)) for (String n : names) for (String w : k) if (Txt.norm(n).contains(Txt.norm(w))) return n;
        }
        return best;
    }

    private static Map<String, Object> cmd(String op) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("op", op);
        return m;
    }

    private static Map<String, Object> one(String t, List<String> names) {
        boolean up = has(t, UP) && !(has(t, "कम") && Txt.firstIndex(t, "कम") > Txt.firstIndex(t, UP));
        boolean down = has(t, DOWN) && !up;
        // complaints: "the music is too loud" -> lower it; "I can't hear Vrinda" -> louder
        String lt = t.toLowerCase();
        if (lt.matches(".*\\btoo (loud|high|bright|fast|big|large|strong|much|heavy|noisy|saturated|colou?rful|warm|sharp)\\b.*")
                || lt.matches(".*\\b(overpowering|drowns|drowning|deafening|harsh)\\b.*")) { up = false; down = true; }
        else if (lt.matches(".*\\btoo (quiet|soft|low|dark|slow|small|weak|faint|dull|cold|pale|little)\\b.*")
                || lt.matches(".*\\b(can'?t|cannot|can not|hard to|barely) (hear|see)\\b.*") || lt.matches(".*\\bnot (loud|bright|clear) enough\\b.*")) { up = true; down = false; }
        // ---------- reset
        if (has(t, "reset", "पहले जैसा", "पहले जैसी", "undo all", "सब हटा", "original")) return cmd("reset");
        // ---------- v33: the size of a character ("make the monkey bigger", "Khan is too small")
        if (has(t, "bigger", "larger", "taller", "huge", "giant", "smaller", "tiny", "shorter", "too big", "too large", "too small", "बड़ा", "बड़ी", "छोटा", "छोटी", "लंबा", "लंबी")
                && !has(t, "font", "subtitle", "text", "file", "resolution", "music", "voice", "sound")) {
            String who = subject(t, names);
            if (who != null) {
                Map<String, Object> c = cmd("size");
                c.put("who", who);
                boolean smaller = has(t, "smaller", "tiny", "shorter", "too big", "too large", "छोटा", "छोटी") && !has(t, "too small");
                c.put("factor", (double) (smaller ? (has(t, "much", "बहुत") ? 0.7f : 0.85f) : (has(t, "much", "बहुत", "huge", "giant") ? 1.4f : 1.2f)));
                return c;
            }
        }
        // ---------- v33: the light of one part ("scene 2 brighter", "the cave is too dark", "part 1 darker")
        if (has(t, "bright", "lighter", "dark", "dim", "light", "रोशनी", "उजाला", "अँधेरा", "अंधेरा", "काला") && !has(t, "subtitle", "music", "voice")) {
            java.util.regex.Matcher sm = java.util.regex.Pattern.compile("(?i)\\b(scene|part|दृश्य|भाग)\\s*(\\d+)").matcher(t);
            String which = sm.find() ? sm.group(2) : null;
            if (which == null) {
                for (String place : new String[]{"cave", "गुफा", "garden", "बगीचा", "forest", "जंगल", "palace", "महल", "room", "कमरा", "village", "गाँव", "night", "रात"})
                    if (has(t, place)) { which = place; break; }
            }
            if (which != null) {
                Map<String, Object> c = cmd("scene_brightness");
                c.put("scene", which);
                boolean darker = (has(t, "dark", "dim", "अँधेरा", "अंधेरा", "काला") && !has(t, "too dark", "less dark", "not so dark", "बहुत अँधेरा", "बहुत अंधेरा")) || has(t, "too bright", "less bright");
                float a = has(t, "much", "बहुत", "very", "lot") ? 0.45f : 0.25f;
                c.put("factor", (double) (1 + (darker ? -a : a)));
                return c;
            }
        }
        // ---------- v33: the length of shots ("shorter shots", "more cuts", "longer shots", "fewer cuts", "cuts too fast")
        if (has(t, "shots", "shot", "cuts", "cut ", "शॉट", "कट")) {
            boolean shorter = has(t, "shorter", "short", "more cuts", "faster cuts", "quicker", "छोटे", "ज़्यादा कट", "ज्यादा कट") && !has(t, "too many", "too fast", "too quick", "too short");
            boolean longer = has(t, "longer", "long", "fewer", "less cuts", "too many", "too fast", "too quick", "too short", "slower cuts", "लंबे", "कम कट");
            if (shorter || longer) { Map<String, Object> c = cmd("shot_length"); c.put("factor", (double) (shorter ? 0.75f : 1.3f)); return c; }
        }
        // ---------- platform / aspect
        if (has(t, "4:5", "4 by 5", "insta portrait", "instagram portrait", "feed portrait", "पोर्ट्रेट")) { Map<String, Object> c = cmd("aspect"); c.put("value", "4:5"); return c; }
        if (has(t, "2.39", "cinemascope", "cinema scope", "anamorphic", "widescreen", "सिनेमास्कोप", "theatre", "theater", "cinema")) { Map<String, Object> c = cmd("aspect"); c.put("value", "2.39:1"); return c; }
        if (has(t, "instagram", "इंस्टा", "reel", "रील", "shorts", "vertical", "खड़ा", "9:16")) { Map<String, Object> c = cmd("aspect"); c.put("value", "9:16"); return c; }
        if (has(t, "youtube", "यूट्यूब", "horizontal", "आड़ा", "16:9", "landscape")) { Map<String, Object> c = cmd("aspect"); c.put("value", "16:9"); return c; }
        if (has(t, "square", "वर्गाकार", "1:1")) { Map<String, Object> c = cmd("aspect"); c.put("value", "1:1"); return c; }
        if (has(t, "facebook", "फेसबुक")) { Map<String, Object> c = cmd("aspect"); c.put("value", "16:9"); return c; }
        // ---------- resolution
        Matcher res = Pattern.compile("(2160|1440|1080|720|480|360)\\s*p?").matcher(t);
        if (res.find() && !has(t, "%")) { Map<String, Object> c = cmd("resolution"); c.put("value", (double) Math.min(1080, Integer.parseInt(res.group(1)))); return c; }
        if (has(t, "full hd", "fullhd", "फुल एचडी")) { Map<String, Object> c = cmd("resolution"); c.put("value", 1080.0); return c; }
        // ---------- subtitles
        if (has(t, "subtitle", "उपशीर्षक", "caption", "सबटाइटल", "कैप्शन", "likha", "लिखे", "text on screen", "on-screen text")) {
            Map<String, Object> c = cmd("subtitles");
            c.put("on", !word(t, "हटा", "हटाओ", "बंद", "off", "remove", "नहीं", "मत", "without", "no"));
            return c;
        }
        // ---------- file size
        if (has(t, "file size", "size", "साइज़", "साइज", "आकार", "एमबी", "compress", "फ़ाइल", "file", "upload", "whatsapp") || word(t, "mb")) {
            Map<String, Object> c = cmd("file_size");
            c.put("factor", (double) (has(t, "कम", "छोटा", "छोटी", "small", "reduce", "compress", "decrease", "less", "kam", "too big", "too large",
                    "upload", "whatsapp", "lighter") || down ? 0.5f : 1.5f));
            return c;
        }
        // ---------- picture
        if (has(t, "brightness", "चमक", "उजाला", "रोशनी", "bright", "रौशनी", "andhera", "अंधेरा", "dark", "dim", "gloomy", "lighting", "exposure")) {
            Map<String, Object> c = cmd("brightness");
            boolean darkWord = has(t, "अंधेरा", "andhera", "dark");
            boolean darker;
            if (darkWord) {
                // "too dark" / "अंधेरा बहुत है" is a complaint -> brighter; "make it darker" / "अंधेरा करो" -> darker
                boolean complaint = has(t, "बहुत", "too", "ज़्यादा", "ज्यादा", "zyada", "है", "hai", "लग", "lag", "कम", "less", "reduce");
                darker = has(t, "darker", "more dark", "अंधेरा करो", "अंधेरा बढ़", "dark करो", "make it dark", "moodier") || (!complaint && up);
                if (has(t, "too dark", "too dim", "too gloomy")) darker = false;
            } else {
                darker = down && !up;
            }
            float a = amount(t, true) - 1;
            c.put("factor", (double) (1 + (darker ? -a * 0.6f : a * 0.6f)));
            return c;
        }
        if (has(t, "contrast", "कंट्रास्ट")) { Map<String, Object> c = cmd("contrast"); c.put("factor", (double) amount(t, !down)); return c; }
        if (has(t, "black-white", "black and white", "black & white", "grayscale", "greyscale")) { Map<String, Object> c = cmd("saturation"); c.put("value", 0.0); return c; }
        if (has(t, "saturation", "रंग", "colour", "color", "कलर", "rang", "vivid", "vibrant", "pale", "washed out")) {
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
            // voice effects: "make the king's voice raspy", "give the ghost an echo", "no more robot voice for Tinku"
            String style = has(t, "raspy", "hoarse", "gravelly", "husky", "rough voice", "croaky") ? "raspy"
                    : has(t, "trembl", "shaky", "quiver", "wobbl") ? "trembling"
                    : has(t, "echo", "boom", "resonant", "thunderous") ? "booming"
                    : has(t, "robot", "metallic", "mechanical") ? "robotic"
                    : has(t, "whisper", "breathy", "airy") ? "whisper"
                    : has(t, "nasal", "whiny") ? "nasal"
                    : has(t, "ghost", "spooky", "eerie") ? "ghostly"
                    : has(t, "magical", "divine", "angelic", "ethereal", "heavenly") ? "magical"
                    : has(t, "growl", "snarl") ? "growl" : null;
            if (style != null) {
                Map<String, Object> c = cmd("voice_style");
                c.put("who", who);
                c.put("style", style);
                c.put("on", !(word(t, "no", "not", "without", "remove", "less", "stop", "off", "normal") || has(t, "too much", "too ")));
                return c;
            }
            if (has(t, "pitch", "पिच", "सुर", "मोटी", "मोटा", "भारी", "पतली", "पतला", "deeper", "deep", "higher pitch", "moti", "patli", "bhari",
                    "squeaky", "squeakier", "younger", "older", "childlike", "thinner", "heavier", "manlier", "lighter voice")) {
                Map<String, Object> c = cmd("voice_pitch");
                c.put("who", who);
                boolean lower = has(t, "मोटी", "मोटा", "भारी", "deeper", "deep", "moti", "bhari", "lower", "नीच", "older", "heavier", "manlier")
                        || (down && !has(t, "पतली", "पतला", "younger", "squeak", "thinner", "childlike"));
                c.put("factor", (double) (lower ? 0.85f : 1.15f));
                return c;
            }
            if (has(t, "slow", "धीरे", "धीमा", "धीमी", "fast", "जल्दी", "speed", "रफ़्तार", "गति", "dheere", "jaldi", "quicker", "rushed", "hurried")) {
                Map<String, Object> c = cmd("voice_rate");
                c.put("who", who);
                boolean slower = has(t, "slow", "धीरे", "धीमा", "धीमी", "dheere", "rushed", "hurried", "too fast", "too quick");
                if (has(t, "too slow")) slower = false;
                c.put("factor", (double) (slower ? 0.85f : 1.15f));
                return c;
            }
            Map<String, Object> c = cmd("voice_gain");
            c.put("who", who);
            if (word(t, "mute", "म्यूट", "बंद", "chup", "चुप")) c.put("value", 0.0);
            else c.put("factor", (double) amount(t, !down));
            return c;
        }
        // ---------- mix
        if (has(t, "music", "संगीत", "म्यूज़िक", "म्यूजिक", "गाना", "धुन", "song", "bgm", "gaana", "soundtrack", "score", "melody", "tune")) {
            Map<String, Object> c = cmd("music");
            if (word(t, "mute", "म्यूट", "बंद", "हटा", "हटाओ", "off", "remove")) c.put("value", 0.0); else c.put("factor", (double) amount(t, !down));
            return c;
        }
        if (has(t, "narrator", "कथावाचक", "सूत्रधार")) { Map<String, Object> c = cmd("narrator"); c.put("factor", (double) amount(t, !down)); return c; }
        if (has(t, "background sound", "background noise", "पृष्ठभूमि", "बैकग्राउंड", "ambience", "ambient", "माहौल", "आसपास", "nature", "प्राकृतिक",
                "birds", "noise", "surroundings")) {
            Map<String, Object> c = cmd("ambience");
            if (word(t, "mute", "म्यूट", "बंद", "हटा", "हटाओ", "off")) c.put("value", 0.0); else c.put("factor", (double) amount(t, !down));
            return c;
        }
        if (has(t, "effect", "इफ़ेक्ट", "इफेक्ट", "ध्वनि", "धमाके", "sfx", "foley")) {
            Map<String, Object> c = cmd("sfx");
            if (word(t, "mute", "म्यूट", "बंद", "हटा", "हटाओ", "off")) c.put("value", 0.0); else c.put("factor", (double) amount(t, !down));
            return c;
        }
        if ((voiceWord && has(t, "सब", "all", "dialogue", "संवाद", "सभी", "voices")) || has(t, "dialogue", "dialogues", "speech", "voices")) {
            Map<String, Object> c = cmd("voices"); c.put("factor", (double) amount(t, !down)); return c;
        }
        if (has(t, "slow", "धीमा", "धीमी", "fast", "तेज़ चल", "speed", "रफ़्तार", "pace", "pacing", "quicker", "drags", "dragging", "rushed")) {
            Map<String, Object> c = cmd("speed");
            boolean slower = has(t, "slow", "धीमा", "धीमी", "rushed", "too fast", "too quick");
            if (has(t, "too slow", "drags", "dragging", "speed up", "faster", "quicker")) slower = false;
            c.put("factor", (double) (slower ? 0.85f : 1.15f));
            return c;
        }
        if (voiceWord) { Map<String, Object> c = cmd("voices"); c.put("factor", (double) amount(t, !down)); return c; }
        return null;
    }
}
