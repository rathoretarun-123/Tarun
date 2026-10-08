package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Pixar-Lead protocol (v4.0), hardcoded: PIXAR_LEAD = StorySpine + Braintrust + Deakins2Light + 12Principles
 * + SafeZoneResizing[format_spec] + AntiStretchValidation. Pixar is the boss: the story comes first.
 *
 * Everything here is used by the director, the renderer and the descriptions file:
 *  - the story engine: the six-beat spine of the script and its acts (R4), the rules it checks (R1, R5, R7, R13, R19);
 *  - the Braintrust loop: every 5 shots the four questions, as suggestions (the director decides);
 *  - Deakins lighting: at most two lights (key + bounce), a colour script per act and place, the background
 *    dictates the colour;
 *  - Disney's 12 principles, as motion rules and as tags in every video prompt;
 *  - the enhancers: Nolan (real sounds, cross-cutting), Miyazaki (ma: a quiet shot after two fast ones),
 *    Narsimha (real garment names), Russo (one comic extra action per scene, the same colours per hero),
 *    Gunn (one joke in a scary scene, one scary shadow in a funny one), Spider-Verse (animation on twos:
 *    experts 24, learners 12, rebels 8 frames per second);
 *  - the photo resizing module: the format specs, their safe zones and character scale, and the rules
 *    (never stretch, the ratio is a parameter, no blurry upscale, letterbox / pillarbox, first-frame checks,
 *    thumbnail and poster made separately).
 */
public final class PixarLead {
    private PixarLead() {}

    // ================================================================== formats and safe zones

    /** One delivery format: its shape, native size and safe zones (fractions of the frame). */
    public static final class Format {
        public final String id, ar, label;
        public final float ratio;
        public final int w, h;
        /** Empty margins: top (headroom / app UI), bottom (captions), left and right. */
        public final float top, bottom, side;
        /** Where the eyes sit in a close-up (fraction of the frame height from the top). */
        public final float eyeLine;
        /** An adult character's height as a fraction of the frame height (RULE_RESIZE_6). */
        public final float charScale;
        /** The safe area for faces, as a fraction of the frame (centre). */
        public final float safe;
        public final String focal, note;
        Format(String id, String ar, String label, float ratio, int w, int h, float top, float bottom, float side, float eyeLine, float charScale, float safe, String focal, String note) {
            this.id = id; this.ar = ar; this.label = label; this.ratio = ratio; this.w = w; this.h = h; this.top = top; this.bottom = bottom; this.side = side;
            this.eyeLine = eyeLine; this.charScale = charScale; this.safe = safe; this.focal = focal; this.note = note;
        }
    }

    public static final Format[] FORMATS = {
            new Format("YOUTUBE_MAIN_16_9", "16:9", "YouTube / TV — landscape 16:9", 16 / 9f, 1920, 1080, 0.20f, 0.05f, 0.10f, 1 / 3f, 0.60f, 0.80f, "85mm",
                    "face in the centre 80%, eyes on the top-third line, 20% headroom, 10% empty left and right"),
            new Format("REELS_TIKTOK_9_16", "9:16", "Reels / Shorts / TikTok — vertical 9:16", 9 / 16f, 1080, 1920, 0.15f, 0.15f, 0.15f, 0.5f, 0.50f, 0.60f, "35mm",
                    "face in the middle 60%, 15% empty at the top for the app's buttons, 15% at the bottom for captions, 15% at the sides"),
            new Format("INSTA_FEED_1_1", "1:1", "Instagram post — square 1:1", 1f, 1080, 1080, 0.15f, 0.10f, 0.15f, 0.45f, 0.65f, 0.70f, "50mm",
                    "face in the centre 70% circle"),
            new Format("INSTA_PORTRAIT_4_5", "4:5", "Instagram portrait 4:5", 0.8f, 1080, 1350, 0.15f, 0.12f, 0.15f, 0.45f, 0.60f, 0.70f, "50mm",
                    "face in the centre 70%"),
            new Format("CINEMA_2_39_1", "2.39:1", "Cinema — 2.39:1 widescreen", 2.39f, 1920, 804, 0.15f, 0.05f, 0.10f, 0.38f, 0.60f, 0.80f, "85mm",
                    "letterbox, never a crop of heads"),
    };

    public static final Format THUMBNAIL = new Format("THUMBNAIL_16_9", "16:9", "Thumbnail 16:9", 16 / 9f, 1280, 720, 0.08f, 0.15f, 0.10f, 0.42f, 0.60f, 0.60f, "85mm",
            "face 60% of the frame, nothing under the bottom 15% (text goes there)");

    /** The format for an aspect string ("16:9", "9:16", "1:1", "4:5", "2.39:1"); 16:9 when unknown. */
    public static Format spec(String ar) {
        for (Format f : FORMATS) if (f.ar.equals(ar)) return f;
        return FORMATS[0];
    }

    /** The format whose shape is closest to a frame's width / height. */
    public static Format specFor(float ratio) {
        Format best = FORMATS[0];
        for (Format f : FORMATS) if (Math.abs(Math.log(f.ratio / ratio)) < Math.abs(Math.log(best.ratio / ratio))) best = f;
        return best;
    }

    /** Words that must never be in a generation prompt (RULE_RESIZE_1): resizing is done natively, never by stretching. */
    public static final String[] FORBIDDEN_RESIZE = {"stretch", "resize to", "crop to", "convert aspect ratio", "outpaint"};

    // ================================================================== the story engine

    public static final String[] STORY_RULES = {
            "R1: You admire a character for trying more than for their successes.",
            "R4: Once upon a time there was ___. Every day, ___. One day ___. Because of that, ___. Because of that, ___. Until finally ___.",
            "R5: Simplify. Focus. Combine characters. Hop over detours. At most 4 characters per 60 seconds.",
            "R7: Come up with your ending before you figure out your middle.",
            "R13: Give your characters opinions. Passive / malleable might seem likable to you as you write, but it's poison.",
            "R19: Coincidences to get characters into trouble are great; coincidences to get them out of it are cheating."};

    static final String[] TROUBLE = {"नफरत", "कैद", "अपहरण", "गायब", "खतरा", "डर", "चोरी", "चुरा", "हमला", "शाप", "जाल", "धोखा", "बीमार", "खो ", "खो गया", "खो गई",
            "भाग गया", "तूफ़ान", "तूफान", "बाढ़", "आग लग", "राक्षस", "चुड़ैल", "दुश्मन", "शत्रु", "hate", "kidnap", "danger", "fear", "steal", "stole", "attack",
            "curse", "trap", "trick", "sick", "lost", "storm", "flood", "fire", "monster", "witch", "enemy", "villain"};
    static final String[] CLIMAX = {"मुकाबला", "लड़ाई", "युद्ध", "बचा", "छुड़ा", "भागते", "पीछा", "आज़ाद", "टकरा", "हराया", "हरा दिया", "fight", "battle", "rescue",
            "escape", "chase", "free", "defeat", "showdown", "final"};
    static final String[] RESOLVE = {"उत्सव", "जश्न", "खुशी", "मुस्कुरा", "वापस", "सीख", "सबक", "इनाम", "गले लगा", "celebrat", "festival", "happy", "return", "lesson",
            "learn", "reward", "hug", "the end", "समाप्त"};

    /** The hero: the character with the most lines (the first named one when nobody speaks). */
    public static Story.CharacterDef hero(Story st) {
        Story.CharacterDef best = null;
        int bestN = -1;
        for (Story.CharacterDef c : st.characters) {
            int n = 0;
            for (Story.Scene sc : st.scenes) for (Story.Beat b : sc.beats) if (b.type == Story.Beat.DIALOGUE && b.speaker == c) n++;
            if (n > bestN) { bestN = n; best = c; }
        }
        return best;
    }

    /** The scene (index) where the trouble begins — "One day" — and where the climax is. */
    public static int[] turningPoints(Story st) {
        int n = st.scenes.size();
        int oneDay = -1, climax = -1, bestC = 0;
        for (int i = 0; i < n; i++) {
            String all = sceneText(st.scenes.get(i));
            if (oneDay < 0 && i > 0 && Txt.has(all, TROUBLE)) oneDay = i;
            int c = 0;
            for (String w : CLIMAX) if (Txt.has(all, w)) c++;
            if (i >= Math.max(1, n / 2) && c >= bestC && c > 0) { bestC = c; climax = i; }
        }
        if (oneDay < 0) oneDay = Math.min(n - 1, Math.max(1, n / 4));
        if (climax < 0) climax = Math.max(oneDay, n - 2);
        return new int[]{oneDay, climax};
    }

    static String sceneText(Story.Scene sc) {
        StringBuilder b = new StringBuilder(sc.title).append(' ').append(sc.setting).append(' ');
        for (Story.Beat bt : sc.beats) b.append(bt.text).append(' ').append(bt.manner == null ? "" : bt.manner).append(' ');
        return b.toString();
    }

    /** The act of every scene: 1 setup, 2 the turn, 3 escalation, 4 climax, 5 resolution. */
    public static int[] acts(Story st) {
        int n = st.scenes.size();
        int[] tp = turningPoints(st);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = i < tp[0] ? 1 : i == tp[0] ? 2 : i < tp[1] ? 3 : i == tp[1] ? 4 : 5;
        return a;
    }

    /** The story spine (R4) read from the script: six beats, each a short line. */
    public static String[] spine(Story st) {
        String[] s = new String[6];
        Story.CharacterDef h = hero(st);
        int n = st.scenes.size();
        if (n == 0) return new String[]{"", "", "", "", "", ""};
        int[] tp = turningPoints(st);
        s[0] = "Once upon a time there was " + (h == null ? "someone" : h.shown() + (h.age > 0 ? " (" + h.age + ")" : "")
                + (h.description != null && h.description.trim().length() > 0 ? ", " + Bible.oneLine(Bible.firstClauseOf(h.description)) : ""));
        s[1] = "Every day, " + firstAction(st.scenes.get(0));
        s[2] = "One day, " + headline(st.scenes.get(tp[0]));
        int b1 = Math.min(n - 1, tp[0] + 1), b2 = Math.min(n - 1, Math.max(b1 + 1, (tp[0] + tp[1]) / 2));
        s[3] = "Because of that, " + headline(st.scenes.get(b1));
        s[4] = "Because of that, " + headline(st.scenes.get(b2 == b1 ? Math.min(n - 1, b1 + 1) : b2));
        s[5] = "Until finally, " + headline(st.scenes.get(n - 1)) + (n - 1 > tp[1] ? " (after " + headline(st.scenes.get(tp[1])) + ")" : "");
        return s;
    }

    static String headline(Story.Scene sc) {
        String t = sc.title.trim().length() > 0 ? sc.title : Bible.firstClauseOf(sc.setting);
        return Bible.oneLine(t.length() > 70 ? t.substring(0, 70) + "…" : t);
    }

    static String firstAction(Story.Scene sc) {
        for (Story.Beat b : sc.beats) {
            if (b.type != Story.Beat.DIRECTION) continue;
            String t = Txt.withoutParens(b.text).replaceFirst("^(स्थान|Location|Place|Setting)\\s*[:：]\\s*", "");
            List<String> sents = Director.sentences(t);
            for (String x : sents) if (!Txt.has(x, "स्थान", "समय", "Location") && x.trim().length() > 8) return Bible.oneLine(x.length() > 80 ? x.substring(0, 80) + "…" : x);
        }
        return headline(sc);
    }

    // ================================================================== the Braintrust loop

    public static final String[] BRAINTRUST_QUESTIONS = {"Is the story clear without dialogue?", "Does the character have a strong opinion?",
            "Is there a ma pause (a quiet moment)?", "Is the costume culturally accurate?"};
    public static final String AUTHORITY = "AUTHORITY: the Braintrust suggests only; the director decides.";

    /** Words of will and opinion in a line (R13). */
    static final String[] OPINION = {"!", "नहीं", "मत ", "चाहिए", "होगा", "ज़रूर", "कभी नहीं", "वचन", "मैं", "हम ", "must", "never", "won't", "will", "promise", "no!", "i will"};
    /** Real garments and ornaments (Narsimha: a legacy, not a generic princess dress). */
    public static final String[] GARMENTS = {"लहंगा", "लहँगा", "घाघरा", "घेरेदार", "चोली", "साड़ी", "बनारसी", "सिल्क", "अचकन", "शेरवानी", "पगड़ी", "साफ़ा", "साफा", "दुपट्टा", "पल्लू",
            "सलवार", "कुर्ता", "कुर्ती", "धोती", "जूती", "जूतियाँ", "मोजड़ी", "गोटा", "ज़री", "जरी", "ज़रदोज़ी", "कढ़ाई", "बंडी", "अंगरखा", "लबादा", "कवच", "वर्दी", "कमरबंद",
            "ओढ़नी", "घूँघट", "चूड़ियाँ", "पायल", "बाजूबंद", "माला", "मुकुट", "कलगी", "रुमाल", "फ्रॉक", "lehenga", "ghagra", "choli", "saree", "sari", "banarasi", "achkan",
            "sherwani", "turban", "pagdi", "dupatta", "salwar", "kurta", "dhoti", "juti", "jutti", "mojari", "gota", "zari", "zardozi", "bandi", "cloak", "armour", "armor",
            "uniform", "bandana", "jacket", "frock", "belt", "tunic", "robe", "sash"};

    public static boolean costumeCultural(String costume) {
        if (costume == null) return false;
        return Txt.has(costume, GARMENTS);
    }

    /** The Braintrust's review of a planned film: the four questions, every five shots, and the story rules. */
    public static final class Review {
        public int windows, clear, opinion, ma, costumeOk, costumeTotal;
        public final List<String> notes = new ArrayList<String>();
        public final List<String> crowded = new ArrayList<String>();
        public String text = "";
    }

    public static Review braintrust(Film film, Story st) {
        Review r = new Review();
        List<Film.Shot> shots = film.shots;
        // costume (Q4), once per character
        List<String> plain = new ArrayList<String>();
        for (Story.CharacterDef c : st.characters) {
            r.costumeTotal++;
            if (costumeCultural(c.description)) r.costumeOk++; else plain.add(c.shown());
        }
        for (int w = 0; w < shots.size(); w += 5) {
            int end = Math.min(shots.size(), w + 5);
            r.windows++;
            boolean clear = false, opinion = false, ma = false;
            for (int i = w; i < end; i++) {
                Film.Shot sh = shots.get(i);
                if (!sh.speech && sh.action.length() > 0 && !sh.action.contains("\"")) clear = true;
                if (sh.motion > 0.03f || sh.actions > 1) clear = true;
                if (!sh.speech && sh.dur >= 1.5f) ma = true;
                if (sh.line >= 0 && sh.line < film.lines.size()) {
                    Film.Line l = film.lines.get(sh.line);
                    if (l.emotion != Pose.NEUTRAL || Txt.has(l.text, OPINION)) opinion = true;
                } else if (!sh.speech) opinion = true;      // nobody speaks: the question does not arise
            }
            if (clear) r.clear++;
            if (opinion) r.opinion++;
            if (ma) r.ma++;
            if (r.notes.size() < 12) {
                String where = String.format(Locale.US, "shots %d-%d", w + 1, end);
                if (!clear) r.notes.add(where + ": five shots of talk — the story should read without the words (an action, a look, an object)");
                if (!opinion) r.notes.add(where + ": nobody wants anything here — give the speaker an opinion (R13)");
                if (!ma) r.notes.add(where + ": no quiet moment — the director holds a still shot after the next action (ma)");
            }
        }
        // R5: at most four characters per 60 seconds
        for (float t0 = 0; t0 < film.duration; t0 += 60) {
            java.util.Set<String> who = new java.util.HashSet<String>();
            for (Film.Line l : film.lines) if (l.who != null && l.start >= t0 && l.start < t0 + 60) who.add(l.who.displayName);
            if (who.size() > 4) r.crowded.add(String.format(Locale.US, "%d:%02d-%d:%02d: %d speaking characters (%s)", (int) t0 / 60, (int) t0 % 60,
                    (int) (t0 + 60) / 60, (int) (t0 + 60) % 60, who.size(), join(who)));
        }
        StringBuilder b = new StringBuilder();
        b.append("BRAINTRUST (every 5 shots, ").append(r.windows).append(" rounds) — ").append(AUTHORITY).append('\n');
        b.append(String.format(Locale.US, "• Q1 story clear without dialogue: %d of %d rounds%n", r.clear, r.windows));
        b.append(String.format(Locale.US, "• Q2 the character has a strong opinion: %d of %d rounds%n", r.opinion, r.windows));
        b.append(String.format(Locale.US, "• Q3 a ma pause (quiet moment): %d of %d rounds%n", r.ma, r.windows));
        b.append(String.format(Locale.US, "• Q4 costume culturally accurate (real garment names): %d of %d characters%s%n", r.costumeOk, r.costumeTotal,
                plain.isEmpty() ? "" : " — generic: " + join(plain) + " (name the real garment: ghagra-choli, Banarasi saree, achkan…)"));
        b.append("• R5 at most four characters per 60 s: ").append(r.crowded.isEmpty() ? "yes" : "no — " + join(r.crowded) + " (the director keeps every shot on one or two of them; combining characters is the writer's call)").append('\n');
        for (String n : r.notes) b.append("• suggestion: ").append(n).append('\n');
        r.text = b.toString();
        return r;
    }

    static String join(java.util.Collection<String> c) {
        StringBuilder b = new StringBuilder();
        for (String s : c) b.append(b.length() > 0 ? ", " : "").append(s);
        return b.toString();
    }

    // ================================================================== Deakins: two lights, a colour script

    public static final int MAX_LIGHTS = 2;

    /** The key light of a place and time: {direction: -1 from the left, +1 from the right, 0 above; colour; strength; name}. */
    public static final class Key {
        public float dir;
        public int color;
        public float strength;
        public String name, bounce;
    }

    public static Key keyLight(int set, int tod) {
        Key k = new Key();
        if (set == Sets.CAVE_IN || set == Sets.CAVE_MOUTH) { k.dir = -0.6f; k.color = 0xFF9BE89B; k.strength = 0.55f; k.name = "Key: green-tinted shaft of light from the cave mouth, top left"; k.bounce = "Bounce: very low, from the wet cave floor"; }
        else if (tod == Sets.MORNING) { k.dir = -1; k.color = 0xFFFFE2A8; k.strength = 0.7f; k.name = "Key: warm low sun from the left, 30 deg"; k.bounce = "Bounce: soft sky light from the right"; }
        else if (tod == Sets.EVENING) { k.dir = 1; k.color = 0xFFFFB070; k.strength = 0.8f; k.name = "Key: orange sunset from the right, 15 deg"; k.bounce = "Bounce: purple sky from the left"; }
        else if (tod == Sets.NIGHT) { k.dir = 0.6f; k.color = 0xFF9FB4E8; k.strength = 0.35f; k.name = "Key: cool moonlight, top right"; k.bounce = "Bounce: dim blue from the ground"; }
        else if (set == Sets.HALL) { k.dir = -0.7f; k.color = 0xFFFFE8C8; k.strength = 0.6f; k.name = "Key: warm window light, top left 45 deg"; k.bounce = "Bounce: soft, from the marble floor"; }
        else { k.dir = -0.8f; k.color = 0xFFFFF0D0; k.strength = 0.6f; k.name = "Key: warm sun, top left 45 deg"; k.bounce = "Bounce: soft, from the ground"; }
        return k;
    }

    /** The colour script: the palette of a place, a time of day and an act (background dictates colour). */
    public static void colorScript(int set, int tod, int mood, int act, FilmLook.Params out) {
        out.warmth = 0.08f; out.saturation = 1.03f; out.contrast = 1f; out.bloom = 0.14f; out.green = 0;
        boolean cave = set == Sets.CAVE_IN || set == Sets.CAVE_MOUTH;
        if (cave) { out.warmth = -0.05f; out.green = 0.22f; out.saturation = 0.95f; out.contrast = 1.08f; out.bloom = 0.12f; }      // cave: green with orange accents
        else if (tod == Sets.NIGHT) { out.warmth = -0.42f; out.green = -0.04f; out.saturation = 0.9f; out.contrast = 1.04f; out.bloom = 0.16f; }   // night: blue
        else if (set == Sets.GARDEN || set == Sets.COURTYARD || set == Sets.GATE || set == Sets.VILLAGE || set == Sets.GENERIC_OUT) {
            out.warmth = 0.22f; out.saturation = 1.06f; out.bloom = 0.18f;                                                          // garden: warm yellow
        } else if (set == Sets.CELEBRATION) { out.warmth = 0.3f; out.saturation = 1.08f; out.bloom = 0.22f; }
        else if (set == Sets.HALL) { out.warmth = 0.15f; out.saturation = 1.02f; }
        else if (set == Sets.FOREST) { out.warmth = -0.05f; out.green = 0.1f; out.saturation = 1.0f; }
        if (tod == Sets.EVENING) { out.warmth += 0.25f; out.bloom += 0.04f; }
        else if (tod == Sets.MORNING) out.warmth += 0.06f;
        switch (mood) {
            case Film.M_SAD: out.warmth -= 0.2f; out.saturation -= 0.1f; out.contrast -= 0.04f; break;
            case Film.M_TENSE: case Film.M_VILLAIN: out.saturation -= 0.06f; out.contrast += 0.08f; out.bloom -= 0.05f; break;
            case Film.M_HAPPY: case Film.M_CELEBRATE: case Film.M_PLAYFUL: out.saturation += 0.03f; break;
            default:
        }
        // the acts: a lighter setup, more contrast as it escalates, the strongest at the climax, warmth returning at the end
        switch (act) {
            case 1: out.contrast -= 0.02f; break;
            case 3: out.contrast += 0.03f; break;
            case 4: out.contrast += 0.07f; out.saturation += 0.02f; break;
            case 5: out.warmth += 0.08f; out.bloom += 0.03f; break;
            default:
        }
        out.warmth = Math.max(-0.5f, Math.min(0.45f, out.warmth));
        out.bloom = Math.max(0.05f, Math.min(0.3f, out.bloom));
    }

    // ================================================================== Disney's 12 principles

    public static final String PRINCIPLES = "anticipation, follow-through, slow in slow out, arcs, squash and stretch, secondary action";
    public static final String[] TWELVE = {"squash and stretch", "anticipation", "staging", "straight ahead and pose to pose", "follow through and overlapping action",
            "slow in and slow out", "arcs", "secondary action", "timing", "exaggeration", "solid drawing", "appeal"};

    // ================================================================== Spider-Verse: animation on twos

    public static final int FPS_EXPERT = 24, FPS_LEARNER = 12, FPS_REBEL = 8;
    static final String[] REBEL = {"नटखट", "शरारती", "शैतान", "बागी", "विद्रोही", "mischie", "naughty", "rebel", "trickster", "cheeky"};
    static final String[] LEARNER = {"सीख", "नई", "नया", "नन्ह", "learner", "learning", "beginner", "clumsy", "अनाड़ी"};
    static final String[] EXPERT = {"तलवारबाज़", "तलवार", "योद्धा", "बहादुर", "निडर", "माहिर", "expert", "warrior", "brave", "skilled", "master", "सैनिक", "guard", "राजा", "king", "queen", "रानी"};

    /** The frame rate a character is animated on: experts move on ones (24), learners on twos (12), rebels on threes (8). */
    public static int stepFps(Story.CharacterDef c) {
        Look l = c.look;
        String d = c.description == null ? "" : c.description;
        if (l != null && l.kind == Look.MONKEY) return FPS_REBEL;
        if (Txt.has(d, REBEL)) return FPS_REBEL;
        if (Txt.has(d, EXPERT)) return FPS_EXPERT;
        if (l != null && l.isChild() && c.age > 0 && c.age < 10) return FPS_LEARNER;
        if (Txt.has(d, LEARNER)) return FPS_LEARNER;
        return FPS_EXPERT;
    }

    public static String stepName(int fps) { return fps >= 24 ? "on ones (24 fps, an expert)" : fps >= 12 ? "on twos (12 fps, a learner)" : "on threes (8 fps, a rebel)"; }

    // ================================================================== the enhancers and the comic beat

    static final String[] COMIC = {"नटखट", "शरारती", "मज़ाकिया", "मजाकिया", "हँसोड़", "घबराहट", "अति-उत्साही", "गोल-मटोल", "funny", "comic", "mischie", "naughty", "clumsy", "nervous", "silly"};

    /** A character who can carry a joke (Russo: one comic extra action per scene; Gunn: one joke in a scary scene). */
    public static boolean comic(Story.CharacterDef c) {
        if (c.look != null && c.look.kind == Look.MONKEY) return true;
        return c.description != null && Txt.has(c.description, COMIC);
    }

    /** Fast action in a beat (Miyazaki: after two fast beats, a quiet one). */
    static final String[] FAST = {"दौड़", "भाग", "छलाँग", "कूद", "झपट", "टकरा", "लड़", "वार", "मार", "उछल", "फेंक", "गिर", "चीख", "भागत", "run", "jump", "leap", "chase",
            "fight", "strike", "throw", "fall", "scream", "dash", "rush"};

    public static boolean fast(String text) { return text != null && Txt.has(text, FAST); }

    // ================================================================== first-frame checks (RULE_RESIZE_7)

    /** Head not cut: the top of the head inside the frame with room above it. */
    public static final float HEAD_MIN = 0.02f;
    /** Feet of a full-body shot: between 85 % and 98 % of the frame height, with the shadow visible. */
    public static final float FEET_MIN = 0.85f, FEET_MAX = 0.98f;
    /** A stretched face: the eye distance changed by more than 5 % — impossible here (pictures are scaled uniformly). */
    public static final float STRETCH_TOLERANCE = 0.05f;
    /** A morph: a character's height changing by more than 10 % within a shot. */
    public static final float HEIGHT_VARIATION = 0.10f;

    /** The thumbnail and poster prompts (RULE_RESIZE_8): made separately, never resized from a frame. */
    public static String thumbnailPrompt(String hero, String costume, String palette) {
        return "Thumbnail (made separately, natively 1280x720): extreme close-up front of " + hero + ", face 60% of the frame, mouth closed, eyes with a catch-light, "
                + "solid background colour " + palette + " (the hero's palette), no text, high contrast. Costume (verbatim): " + costume;
    }

    /** The hero's palette (Russo: the same colours for a hero in every shot): named from the look's main colour. */
    public static String paletteName(Story.CharacterDef hero) {
        if (hero == null || hero.look == null) return "deep teal";
        int c = hero.look.primary;
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        if (r > g + 40 && r > b + 40) return "warm crimson";
        if (g > r + 20 && g > b + 20) return "forest green";
        if (b > r + 20 && b > g + 20) return "royal blue";
        if (r > 180 && g > 150 && b < 120) return "marigold yellow";
        if (r > 150 && b > 150 && g < 120) return "plum purple";
        return "deep teal";
    }

    /** The hero's palette as a colour for a solid background (the thumbnail and the poster): deep, so the face reads. */
    public static int paletteColor(Story.CharacterDef hero) {
        String n = paletteName(hero);
        if (n.startsWith("warm crimson")) return 0xFF7A1F2B;
        if (n.startsWith("forest")) return 0xFF1F4D2E;
        if (n.startsWith("royal")) return 0xFF1E3A78;
        if (n.startsWith("marigold")) return 0xFFB8741A;
        if (n.startsWith("plum")) return 0xFF4A2160;
        return 0xFF145A5A;
    }

    /** The protocol in one line each, for the top of the descriptions and the QC screen. */
    public static final String SUMMARY = "PIXAR-LEAD PROTOCOL v4.0 — PIXAR_LEAD = StorySpine + Braintrust + Deakins2Light + 12Principles + SafeZoneResizing[format] + AntiStretchValidation\n"
            + "Story engine: R1 trying over success, R4 the six-beat spine, R5 at most 4 characters per 60 s, R7 the ending first, R13 opinions, R19 no coincidence out of trouble.\n"
            + "Braintrust: every 5 shots — story clear without dialogue? strong opinion? a ma pause? costume culturally accurate? — suggestions only, the director decides.\n"
            + "Deakins: two lights only (key + bounce); one colour script per act — garden warm yellow, cave green and orange, night blue; the background dictates the colour.\n"
            + "Disney: " + PRINCIPLES + " in every clip. Enhancers: Nolan real sounds and cross-cutting; Miyazaki ma after two fast shots; "
            + "Narsimha real garment names; Russo one comic beat per scene and the hero's own colours; Gunn one joke in a scary scene, one shadow in a funny one; "
            + "Spider-Verse animation on twos (experts 24, learners 12, rebels 8 fps).\n"
            + "Resizing: never stretch; the ratio is a parameter; safe zones per format; no blurry upscale; letterbox / pillarbox, never a cut head or side; "
            + "character scale lock 16:9 0.6, 9:16 0.5, 1:1 0.65, 4:5 0.6; first-frame checks (face stretch under 5%, head inside, feet in the bottom 85-98% with a shadow); "
            + "thumbnail and poster made separately.";

    public static String posterPrompt(String hero, String costume) {
        return "Poster (made separately, natively 1080x1920): " + hero + " centred, full body, feet on the ground with a shadow, 40% empty space at the top for the title, "
                + "20% at the bottom for credits, no text. Costume (verbatim): " + costume;
    }
}
