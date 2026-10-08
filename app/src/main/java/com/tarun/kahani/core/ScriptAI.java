package com.tarun.kahani.core;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * The "script reader" with a language model: turns any natural-language story (prose, Hindi or English) into the
 * screenplay layout the director understands, keeping every detail of characters, dresses, places, emotions and
 * sounds. The answer is checked with the normal parser; if it is not usable the original text is kept.
 */
public final class ScriptAI {
    private ScriptAI() {}

    static final String FORMAT_HI =
            "पात्र और रूप-रंग का विवरण:\n"
            + "1. <पूरा नाम> (<उम्र> वर्ष):\n"
            + " * चेहरा: <चेहरा, बाल, आँखें, त्वचा का रंग>\n"
            + " * पहनावा: <कपड़े, रंग, गहने, हथियार/सामान>\n"
            + "2. ...\n"
            + "\n"
            + "स्थानों का विवरण:\n"
            + "1. <जगह का नाम>:\n"
            + "<जगह का विवरण>\n"
            + "\n"
            + "<फ़िल्म का शीर्षक>\n"
            + "दृश्य 1: <दृश्य का शीर्षक>\n"
            + "(स्थान: <जगह, समय (सुबह/दोपहर/शाम/रात), मौसम, आसपास की आवाज़ें, कौन-कौन मौजूद है>)\n"
            + "<पात्र का नाम> (<भाव और हरकत, जैसे: डरते हुए, धीरे से>): \"<संवाद>\"\n"
            + "(<मंच निर्देश: कौन क्या करता है, कौन सी आवाज़ आती है>)\n"
            + "दृश्य 2: ...\n";

    static final String FORMAT_EN =
            "Characters & Appearance:\n"
            + "1. <Full name> (<age> years):\n"
            + " * Face: <face, hair, eyes, skin colour>\n"
            + " * Dress: <clothes, colours, jewellery, items>\n"
            + "\n"
            + "Places:\n"
            + "1. <Place name>:\n"
            + "<description>\n"
            + "\n"
            + "<Film title>\n"
            + "Scene 1: <scene title>\n"
            + "(Place: <where, time of day, weather, sounds around, who is there>)\n"
            + "<Character name> (<emotion and action, e.g. scared, whispering>): \"<dialogue>\"\n"
            + "(<stage direction: who does what, what sound is heard>)\n"
            + "Scene 2: ...\n";

    public static String system(boolean hindi) {
        return "You are an expert screenwriter and film director for children's animated films (ages 6-15), "
                + "working like a careful script supervisor. You read a story written in natural language and rewrite it "
                + "as a screenplay WITHOUT losing any detail: every character (with age, face, hair, dress colours, "
                + "jewellery, items they carry), every place (with time of day, weather, light and natural sounds), every "
                + "action, emotion, nuance and sound effect. Keep the author's own words for dialogue wherever possible. "
                + "If the story is told in prose, turn the reported speech into dialogue lines of the right characters and "
                + "put actions into stage directions in brackets. Use a narrator (" + (hindi ? "कथावाचक" : "Narrator")
                + ") ONLY if the story itself asks for a narrator. Description paragraphs must never become dialogue. "
                + "Keep it child-friendly and fully clothed. Write in " + (hindi ? "Hindi (Devanagari)" : "English")
                + ". Output ONLY the screenplay text in exactly this layout, nothing else:\n\n" + (hindi ? FORMAT_HI : FORMAT_EN);
    }

    /** True when the local parser could not find a proper screenplay (prose story). */
    public static boolean needsRewrite(Story s) {
        if (s.dialogueCount() < 3) return true;
        int described = 0;
        for (Story.CharacterDef c : s.characters) if (c.fromScript && c.description.length() > 20) described++;
        return described == 0;
    }

    public static final class Result {
        public String script;     // the screenplay to use
        public boolean rewritten; // true when the language model's version is used
        public String note = "";
    }

    /** Reads the story; returns the AI screenplay when it is good, else the original. Never throws. */
    public static Result read(Cloud cloud, String text) {
        Result r = new Result();
        r.script = text;
        boolean hinglish = Hinglish.isHinglish(text);
        boolean hindi = Txt.mostlyHindi(text) || hinglish;
        String clipped = text.length() > 60000 ? text.substring(0, 60000) : text;
        try {
            String sys = system(hindi) + (hinglish ? "\n\nThe story is written in Hinglish (Hindi in English letters). Write the screenplay in "
                    + "Hindi using Devanagari script so the voices pronounce it correctly; keep English words the characters "
                    + "actually say (like 'sorry', 'thank you') in English letters." : "");
            String answer = cloud.ask(sys, (hindi ? "कहानी:\n\n" : "Story:\n\n") + clipped, false);
            answer = stripFences(answer);
            Story before = ScriptParser.parse(text);
            Story after = ScriptParser.parse(answer);
            boolean better = after.dialogueCount() >= Math.max(1, before.dialogueCount() * 8 / 10)
                    && after.characters.size() >= 1 && after.scenes.size() >= 1;
            if (better) {
                r.script = answer;
                r.rewritten = true;
                r.note = "AI read the story: " + after.characters.size() + " characters, " + after.scenes.size() + " scenes, "
                        + after.dialogueCount() + " lines.";
            } else {
                r.note = "The AI answer was not usable — your original story will be used.";
            }
        } catch (IOException e) {
            r.note = "Could not reach the AI (check internet / key) — the built-in reader will be used. "
                    + shortErr(e);
        } catch (RuntimeException e) {
            r.note = "Could not read the AI answer — your original story will be used.";
        }
        return r;
    }

    static String shortErr(Exception e) {
        String m = e.getMessage() == null ? e.getClass().getSimpleName() : e.getMessage();
        return m.length() > 120 ? m.substring(0, 120) : m;
    }

    static String stripFences(String s) {
        String t = s.trim();
        if (t.startsWith("```")) {
            int nl = t.indexOf('\n');
            t = nl > 0 ? t.substring(nl + 1) : t;
            if (t.endsWith("```")) t = t.substring(0, t.length() - 3);
        }
        return t.trim();
    }

    // ------------------------------------------------------------------ post-preview commands

    /** Turns a free-form editing request into Edits commands with the language model. */
    public static List<Map<String, Object>> commands(Cloud cloud, String request, List<String> names) throws IOException {
        String sys = "You convert a user's request about editing a finished cartoon video into JSON commands. "
                + "Reply ONLY with {\"commands\":[...]} . Each command is an object with \"op\" and optional \"who\", \"factor\", \"value\", \"on\". "
                + "Allowed ops: brightness (factor 1.2 = brighter, value -0.8..0.8), contrast (factor), saturation (factor), "
                + "warmth (factor 1.2 = warmer), music (factor = volume multiplier), sfx (factor), ambience (factor), voices (factor), "
                + "narrator (factor), voice_gain (who, factor), voice_pitch (who, factor 1.1 = higher), voice_rate (who, factor 1.1 = faster), voice_style (who, style one of raspy|trembling|booming|robotic|whisper|nasal|ghostly|magical|growl, on true/false), "
                + "speed (factor), subtitles (on true/false), file_size (factor <1 smaller, >1 larger), resolution (value 480/720/1080), "
                + "aspect (value \"16:9\"|\"9:16\"|\"1:1\"), reset. Character names: " + names + ". "
                + "Use factor 1.3 for 'increase', 0.7 for 'decrease', 1.6/0.4 for 'a lot'. If nothing matches reply {\"commands\":[]}.";
        Object o = Cloud.jsonIn(cloud.ask(sys, request, true));
        List<Map<String, Object>> out = new ArrayList<Map<String, Object>>();
        List<Object> cs = o instanceof List ? castList(o) : Json.arr(o, "commands");
        if (cs != null) for (Object c : cs) if (c instanceof Map) out.add(castMap(c));
        return out;
    }

    @SuppressWarnings("unchecked")
    static List<Object> castList(Object o) { return (List<Object>) o; }

    @SuppressWarnings("unchecked")
    static Map<String, Object> castMap(Object o) { return (Map<String, Object>) o; }

    // ------------------------------------------------------------------ picture recognition

    /**
     * Which character or place does this uploaded picture show? Uses the file name first (works offline),
     * then Gemini vision when a key is set. Returns one of the candidates, or null.
     */
    public static String identify(Cloud cloud, String fileName, byte[] jpeg, List<String> candidates) {
        String byName = matchName(fileName, candidates);
        if (byName != null) return byName;
        if (cloud == null || !cloud.hasGemini() || jpeg == null) return null;
        try {
            List<byte[]> ims = new ArrayList<byte[]>();
            ims.add(jpeg);
            String a = cloud.gemini("You match pictures to the cast and places of a children's film.",
                    "Which ONE of these does the picture show? Answer with the exact name only, or NONE.\n" + candidates, false, ims, null);
            return matchName(a, candidates);
        } catch (IOException e) {
            return null;
        }
    }

    /** What the AI saw in a picture. */
    public static final class Seen {
        public String match;          // candidate name, or null
        public String type = "";      // character | place | title | end | other
        public boolean realPhoto;
        public String caption = "";
    }

    /**
     * Shows the picture to the vision model together with the story's characters and places (name + description)
     * and asks which one it shows, whether it is a real photo, and a short caption for the library.
     */
    public static Seen look(Cloud cloud, byte[] jpeg, List<String[]> candidates) throws IOException {
        StringBuilder b = new StringBuilder();
        b.append("This picture was uploaded for a children's animated film. Here are the film's characters and places:\n");
        for (String[] c : candidates) {
            String d = c[1] == null ? "" : Bible.oneLine(c[1]);
            if (d.length() > 260) d = d.substring(0, 260);
            b.append("- ").append(c[0]).append(": ").append(d).append("\n");
        }
        b.append("\nWhich ONE of these does the picture show? Use the descriptions (age, clothes, colours, animal, place features). ")
                .append("Reply only with JSON: {\"match\":\"<exact name from the list or NONE>\",\"type\":\"character|place|title|end|other\",")
                .append("\"real_photo\":<true if it is a real camera photograph of a real person/place, false for drawings, cartoons, 3D renders or AI art>,")
                .append("\"caption\":\"<short English description: who/what, clothes, colours, setting>\"}");
        Object o = Cloud.jsonIn(cloud.vision(b.toString(), jpeg));
        Seen s = new Seen();
        if (o == null) return s;
        String m = Json.str(o, "match", "NONE");
        for (String[] c : candidates) if (c[0].equals(m)) s.match = c[0];
        if (s.match == null && !m.equalsIgnoreCase("NONE")) {
            List<String> names = new ArrayList<String>();
            for (String[] c : candidates) names.add(c[0]);
            s.match = matchName(m, names);
        }
        s.type = Json.str(o, "type", "");
        s.realPhoto = Json.bool(o, "real_photo", false);
        s.caption = Json.str(o, "caption", "");
        return s;
    }

    /** What the AI heard in a sound: kind (amb, sfx, music, voices) and words in English and Hindi. */
    public static final class Heard {
        public String kind = "";
        public String words = "";
    }

    /** Lets the AI listen to (at most 20 seconds of) a sound and say what it is. */
    public static Heard listen(Cloud cloud, float[] pcm, int sr) throws IOException {
        int n = Math.min(pcm.length, sr * 20);
        float[] x = Mixer.resampleTo(java.util.Arrays.copyOf(pcm, n), sr, 16000);
        String prompt = "Listen to this sound recording for a children's animated film. Reply only with JSON: "
                + "{\"kind\":\"background|effect|music|voices\",\"words\":[\"up to 6 short English words for what is heard\", \"then the same words in Hindi (Devanagari)\"]}. "
                + "background = continuous sound that can loop under a scene (rain, river, birds, wind, traffic); effect = a single event (door, thunder, horse, bell); "
                + "voices = people talking or a crowd in the background; music = music or singing.";
        Object o = Cloud.jsonIn(cloud.listen(prompt, Wav.encode16(x, 16000)));
        Heard h = new Heard();
        if (o == null) return h;
        String k = Json.str(o, "kind", "");
        h.kind = k.startsWith("effect") ? "sfx" : k.startsWith("music") ? "music" : k.startsWith("voice") ? "voices" : k.startsWith("back") ? "amb" : "";
        List<Object> ws = Json.arr(o, "words");
        StringBuilder b = new StringBuilder();
        if (ws != null) for (Object w : ws) if (w instanceof String && ((String) w).trim().length() > 0) { if (b.length() > 0) b.append(", "); b.append(((String) w).trim()); }
        h.words = b.toString();
        return h;
    }

    /** The nature and physics effects the film engine can show, in the words the director understands. */
    public static final String[] CUES = {"rain", "heavy rain", "light rain", "rain stopped", "storm", "dust storm", "strong wind", "breeze",
            "wind stopped", "thunder and lightning", "snow", "fog", "fog cleared", "campfire", "candles", "diyas", "torches", "fireflies",
            "stars", "rainbow", "falling leaves", "flower petals falling", "clouds", "dark clouds", "birds flying", "sea waves", "high waves",
            "boat", "earthquake", "fruit fell from the tree", "ball fell", "throws a ball", "threw a stone into the water",
            "jumped into the water"};

    /**
     * Lets the AI read the story for nature and physics — also when it is only implied ("the sky opened up",
     * "monsoon evening", "the old boat creaked on the waves") — and mark each scene and line with effects from
     * CUES. Returns JSON {"scenes":[{"scene":i,"place":[..],"lines":[{"line":j,"cues":[..]}]}]} (i, j from 0).
     */
    public static String natureCues(Cloud cloud, Story st) throws IOException {
        StringBuilder all = new StringBuilder("{\"scenes\":[");
        boolean first = true;
        int si = 0;
        while (si < st.scenes.size()) {
            // a few scenes per request, so long stories fit
            StringBuilder b = new StringBuilder();
            int from = si;
            while (si < st.scenes.size() && (b.length() < 4500 || si == from)) {
                Story.Scene sc = st.scenes.get(si);
                b.append("SCENE ").append(si).append(" — place: ").append(Bible.oneLine(sc.setting + " " + sc.title)).append('\n');
                for (int bi = 0; bi < sc.beats.size(); bi++) {
                    Story.Beat bt = sc.beats.get(bi);
                    String t = Bible.oneLine((bt.manner.length() > 0 ? "(" + bt.manner + ") " : "") + bt.text);
                    if (t.length() > 180) t = t.substring(0, 180);
                    b.append("  line ").append(bi).append(bt.type == Story.Beat.DIALOGUE ? " [speech]: " : " [action]: ").append(t).append('\n');
                }
                si++;
            }
            StringBuilder vocab = new StringBuilder();
            for (String c : CUES) { if (vocab.length() > 0) vocab.append(", "); vocab.append('"').append(c).append('"'); }
            String system = "You are the director of a children's animated film. You read a script (Hindi, English or Hinglish) and decide "
                    + "which weather, nature and physics effects each scene and moment needs, also when they are only implied "
                    + "(e.g. 'the sky opened up' = rain, 'monsoon evening' = clouds and light rain, 'the sea was angry' = high waves and strong wind, "
                    + "'they sat around the fire telling stories' = campfire). Use ONLY these effect names: " + vocab
                    + ". Place effects last the whole scene; line effects start at that line. Speech only shows effects that are really happening. "
                    + "Reply ONLY with JSON: {\"scenes\":[{\"scene\":<number>,\"place\":[...],\"lines\":[{\"line\":<number>,\"cues\":[...]}]}]}";
            String ans = cloud.ask(system, b.toString(), true);
            Object o = Cloud.jsonIn(ans);
            List<Object> scenes = o == null ? null : Json.arr(o, "scenes");
            if (scenes != null) for (Object sc : scenes) {
                int n = (int) Json.num(sc, "scene", -1);
                if (n < from || n >= si) continue;
                if (!first) all.append(',');
                first = false;
                all.append(Json.write(sc));
            }
        }
        return all.append("]}").toString();
    }

    /** Puts the cues of natureCues() onto the story's scenes and lines (unknown effect names are ignored). */
    public static void applyCues(Story st, String json) {
        Object o = Json.parseLoose(json);
        List<Object> scenes = o == null ? null : Json.arr(o, "scenes");
        if (scenes == null) return;
        java.util.Set<String> ok = new java.util.HashSet<String>(java.util.Arrays.asList(CUES));
        for (Object sc : scenes) {
            int n = (int) Json.num(sc, "scene", -1);
            if (n < 0 || n >= st.scenes.size()) continue;
            Story.Scene scene = st.scenes.get(n);
            scene.cues = joinCues(Json.arr(sc, "place"), ok);
            List<Object> lines = Json.arr(sc, "lines");
            if (lines != null) for (Object l : lines) {
                int bi = (int) Json.num(l, "line", -1);
                if (bi < 0 || bi >= scene.beats.size()) continue;
                scene.beats.get(bi).cue = joinCues(Json.arr(l, "cues"), ok);
            }
        }
    }

    static String joinCues(List<Object> l, java.util.Set<String> ok) {
        StringBuilder b = new StringBuilder();
        if (l != null) for (Object x : l) {
            if (!(x instanceof String)) continue;
            String c = ((String) x).trim().toLowerCase(java.util.Locale.ROOT);
            if (!ok.contains(c)) continue;
            if (b.length() > 0) b.append(" । ");
            b.append(c).append(' ');
        }
        return b.toString();
    }

    /** Finds a candidate whose name appears in s (Devanagari or Latin spelling). */
    public static String matchName(String s, List<String> candidates) {
        if (s == null) return null;
        String n = s.replace('_', ' ').replace('-', ' ').replace('.', ' ');
        String nn = Txt.norm(n).trim();
        String best = null;
        int bestScore = 0;
        for (String c : candidates) {
            if (!CommandParser.nameIn(n, c)) continue;
            // the same name beats shared words, shared words beat names that only sound alike ("राजा" / "राजू")
            int score = c.length();
            if (Txt.norm(c).trim().equals(nn)) score += 100000;
            for (String part : c.split("\\s+")) if (part.length() >= 2 && !ScriptParser.isTitleWord(part) && Txt.has(n, part)) score += 1000;
            if (score > bestScore) { best = c; bestScore = score; }
        }
        return best;
    }
}
