package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;

/**
 * v43 probe of everyday activities and natural phenomena. Each row of a TSV file is one stage direction, staged in a
 * tiny two-character story exactly as VocabProbe does (Mother calls the main character, the direction, the main
 * character answers), and the film is checked for what a correct director should have put on the screen for it.
 *
 * <pre>
 * File (UTF-8), one row per test sentence, TAB-separated:   lang &lt;TAB&gt; sentence &lt;TAB&gt; expect
 *   lang     hi or en
 *   expect   code groups separated by '|': at least one group must hold; the codes of a group joined by '&amp;' must all hold;
 *            a code written "!CODE" must not hold (a wrong side effect, e.g. "ACT:WAVE&amp;!W:SEA")
 * Lines starting with '#' are comments. A line starting with "#@" sets the context of the rows after it:
 *   #@ cat=&lt;category&gt; place=&lt;out|room|bedroom|kitchen|bathroom|street|park|terrace|garden|shore&gt;
 *      start=&lt;stand|chair|sofa|floor|bed|rain|fog|wind&gt; tod=&lt;day|night|evening|morning&gt; age=&lt;kid|adult&gt;
 *   (anything left out takes its default: place=out (the courtyard), start=stand, tod=day, age=kid)
 * The main character is राहुल / Rahul (a boy of 10, a man of 32 with age=adult), or प्रिया / Priya (a girl of 12, a
 * woman of 28) when the sentence names her and not him.
 *
 * Codes (constants are looked up in Film and Pose by name; an unknown name stops the probe):
 *   W:&lt;NAME&gt;      a Film.Weather of type Film.W_&lt;NAME&gt; is on the screen while the sentence plays (it began with it, or was
 *                 there already: such rows are listed as WEAK when the neutral sentence passes too)
 *   WEND:&lt;NAME&gt;   a Film.Weather of type Film.W_&lt;NAME&gt; ends with the sentence (the rain stops, the fog lifts, the wind drops)
 *   FX:&lt;NAME&gt;     an effect Film.FX_&lt;NAME&gt; begins with the sentence (or the scene has more of them than with a neutral sentence)
 *   SFX:&lt;NAME&gt;    a sound Film.SFX_&lt;NAME&gt; plays with the sentence
 *   ACT:&lt;NAME&gt;    the main character has an act Film.G_&lt;NAME&gt;
 *   TASK:&lt;NAME&gt;   ... a G_TASK act with item Film.T_&lt;NAME&gt;
 *   RIDE:&lt;NAME&gt;   ... a G_RIDE act with |item| % Film.V_PASSENGER == Film.V_&lt;NAME&gt;
 *   PG:&lt;NAME&gt;     ... a G_PLAYGROUND act with |item| == Film.PG_&lt;NAME&gt;
 *   EX:&lt;NAME&gt;     ... a G_EXERCISE act with item Film.EX_&lt;NAME&gt;
 *   MK:&lt;NAME&gt;     ... a G_MAKEUP act with item Film.MK_&lt;NAME&gt;
 *   DRINK:&lt;NAME&gt;  ... a G_DRINK act with item Pose.I_&lt;NAME&gt;
 *   BODY:&lt;NAME&gt;   ... a key with body Pose.&lt;NAME&gt; (SIT, LIE, STAND, KNEEL, CROUCH, HANG) which they were not in before
 *   SEAT:&lt;NAME&gt;   ... a key with seat Film.SEAT_&lt;NAME&gt;
 *   EMO:&lt;NAME&gt;    ... a key with emotion Pose.&lt;NAME&gt; which they did not show before
 *   HOLD:&lt;NAME&gt;   ... a key with holdR or holdL == Pose.I_&lt;NAME&gt;
 *   MOVE          ... a key moving them more than 40 px (or a walking track)
 *   RUN           ... a key with run
 *   EYES          ... a key with the eyes shut (open before)
 *   AWAKE         ... a key with the eyes open (shut before: waking up)
 *   TEARS         ... a key with tears
 *   SWEAT         ... a key with sweat (a hot sun, hard work)
 *   COSTUME       ... a key with other clothes (Film.Key.costume: a change of clothes the story describes)
 *   HIDE          ... a key hidden (anchor A_HIDDEN or A_ROCK_HIDE, or not visible)
 *   UMBRELLA      ... takes up an umbrella with the sentence (Film.Actor.umbrellaAt: not before the window, at its end)
 *   ANY           anything at all staged beyond the two neutral sentences (VocabProbe's "staged")
 * "With the sentence" is the window from the end of Mother's call to the start of the main character's answer; what the
 * film with the neutral sentence has at the same moment (the face the call itself draws, the same key, act, effect or
 * sound at the same time) is not counted — the story before the sentence is the same in both.
 *
 * Run:  java -Dstdout.encoding=UTF-8 -cp ... com.tarun.kahani.jvm.PhenomenaProbe probe.tsv
 * Env:  SHOW=1 also prints the rows that pass;  ONLY=text runs only the rows whose sentence or category contains it, or
 *       ONLY=n / ONLY=n-m the rows on those lines of the file (Devanagari in ONLY needs a UTF-8 locale: LC_ALL=C.UTF-8);
 *       DUMP=1 prints the main character's keys and acts around the window of every row run.
 * Prints "FAIL lang sentence expect -> got: …" for each failing row, a summary per category, rows whose
 * expectation also holds for the neutral sentence (a weak test), and last "PROBE passed of total (hi a/b, en c/d)".
 * </pre>
 */
public class PhenomenaProbe {

    // ================================================================== the context of a row

    static final class Ctx {
        String cat = "misc", place = "out", start = "stand", tod = "day", age = "kid";
        /** v44: fam=1 — Father (पापा) is in the scene too (a family in the car, two on a bike). */
        boolean fam;
        Ctx copy() { Ctx c = new Ctx(); c.cat = cat; c.place = place; c.start = start; c.tod = tod; c.age = age; c.fam = fam; return c; }
        String key() { return place + "/" + start + "/" + tod + "/" + age + (fam ? "/fam" : ""); }
    }

    /** key, Hindi place, Hindi "where", English place, English scene heading, English "where". */
    static final String[][] PLACES = {
        {"out", "घर का आँगन", "आँगन में", "The courtyard of the house", "The courtyard", "in the courtyard"},
        {"room", "घर का कमरा", "कमरे में", "The living room of the house", "The living room", "in the living room"},
        {"bedroom", "घर का बेडरूम", "बेडरूम में", "The bedroom of the house", "The bedroom", "in the bedroom"},
        {"kitchen", "घर की रसोई", "रसोई में", "The kitchen of the house", "The kitchen", "in the kitchen"},
        {"bathroom", "घर का बाथरूम", "बाथरूम में", "The bathroom of the house", "The bathroom", "in the bathroom"},
        {"street", "घर के बाहर की सड़क", "सड़क के किनारे", "The road outside the house", "The road", "by the road"},
        {"park", "मोहल्ले का पार्क", "पार्क में", "The park near the house", "The park", "in the park"},
        {"terrace", "घर की छत", "छत पर", "The terrace of the house", "The terrace", "on the terrace"},
        {"garden", "घर का बगीचा", "बगीचे में", "The garden of the house", "The garden", "in the garden"},
        {"shore", "मछुआरों का गाँव", "गाँव में", "The fishermen's village", "The fishermen's village", "in the village"},
        // v44: royal stories, the hunt, snow, the highway
        {"palace", "राजमहल का दरबार", "दरबार में", "The royal court of the palace", "The royal court", "in the court"},
        {"arena", "महल का अखाड़ा", "अखाड़े में", "The training ground of the palace", "The training ground", "on the training ground"},
        {"forest", "घना जंगल", "जंगल में", "The dense forest", "The forest", "in the forest"},
        {"snow", "बर्फ़ से ढकी पहाड़ी", "बर्फ़ में", "The snowy hillside", "The snowy hillside", "in the snow"},
        {"road", "शहर की बड़ी सड़क", "सड़क पर", "The main road of the city", "The main road", "on the road"},
    };

    static String[] place(String key) {
        for (String[] p : PLACES) if (p[0].equals(key)) return p;
        throw new IllegalArgumentException("unknown place " + key);
    }

    static String hiTod(String t) {
        switch (t) { case "night": return "रात"; case "evening": return "शाम"; case "morning": return "सुबह"; case "day": return "दिन"; default: throw new IllegalArgumentException("unknown tod " + t); }
    }

    /** The tiny story: the main character and Mother, Mother's call, the sentence, the answer. */
    static String story(boolean en, Ctx c, String sentence, boolean girl) {
        String[] p = place(c.place);
        boolean adult = "adult".equals(c.age);
        if (!en) {
            String n = girl ? "प्रिया" : "राहुल";
            String who = girl ? (adult ? "28 साल, बेटी" : "12 साल, बेटी") : (adult ? "32 साल, बेटा" : "10 साल, बेटा");
            String mom = adult ? "60 साल, माँ" : "38 साल, माँ";
            String stand = girl ? "खड़ी" : "खड़ा", sit = girl ? "बैठी" : "बैठा", sleep = girl ? "सो रही" : "सो रहा";
            String here = n + " " + p[2] + " " + stand + " है।";
            String open;
            switch (c.start) {
                case "stand": open = here; break;
                case "chair": open = n + " " + p[2] + " कुर्सी पर " + sit + " है।"; break;
                case "sofa": open = n + " " + p[2] + " सोफ़े पर " + sit + " है।"; break;
                case "floor": open = n + " " + p[2] + " ज़मीन पर " + sit + " है।"; break;
                case "bed": open = n + " " + p[2] + " बिस्तर पर " + sleep + " है।"; break;
                case "throne": open = n + " " + p[2] + " सिंहासन पर " + sit + " है।"; break;
                case "rain": open = "तेज़ बारिश हो रही है। " + here; break;
                case "fog": open = "चारों ओर घना कोहरा छाया हुआ है। " + here; break;
                case "wind": open = "तेज़ हवा चल रही है। " + here; break;
                default: throw new IllegalArgumentException("unknown start " + c.start);
            }
            String dad = c.fam ? "\n3. पापा - " + (adult ? "62 साल, पिता" : "40 साल, पिता") : "";
            return "शीर्षक: जाँच\n\nपात्र परिचय\n1. " + n + " - " + who + "\n2. माँ - " + mom + dad + "\n\nजगह का परिचय\n1. " + p[1]
                    + "\n\nस्क्रिप्ट शुरू\n\nदृश्य 1: " + p[1] + " - " + hiTod(c.tod) + "\n\n[" + open + " माँ पास में खड़ी है।" + (c.fam ? " पापा भी वहीं हैं।" : "") + "]\n\nमाँ: \"" + n + "!\"\n\n["
                    + sentence + "]\n\n" + n + ": \"हाँ माँ।\"\n";
        }
        String n = girl ? "Priya" : "Rahul", him = girl ? "her" : "him", his = girl ? "her" : "his";
        String who = girl ? (adult ? "28, a young woman" : "12, a girl") : (adult ? "32, a man" : "10, a boy");
        String mom = (adult ? "60" : "38") + ", " + his + " mother";
        String here = n + " stands " + p[5] + ".";
        String open;
        switch (c.start) {
            case "stand": open = here; break;
            case "chair": open = n + " is sitting on a chair " + p[5] + "."; break;
            case "sofa": open = n + " is sitting on the sofa " + p[5] + "."; break;
            case "floor": open = n + " is sitting on the floor " + p[5] + "."; break;
            case "bed": open = n + " is asleep in bed " + p[5] + "."; break;
            case "throne": open = n + " is sitting on the throne " + p[5] + "."; break;
            case "rain": open = "It is raining heavily. " + here; break;
            case "fog": open = "Thick fog lies all around. " + here; break;
            case "wind": open = "A strong wind is blowing. " + here; break;
            default: throw new IllegalArgumentException("unknown start " + c.start);
        }
        String dad = c.fam ? "\n3. Father - " + (adult ? "62" : "40") + ", " + his + " father" : "";
        return "Title: Check\n\nCharacters\n1. " + n + " - " + who + "\n2. Mother - " + mom + dad + "\n\nPlaces\n1. " + p[3]
                + "\n\nScript begins\n\nScene 1: " + p[4] + " - " + c.tod + "\n\n[" + open + " Mother stands near " + him + "." + (c.fam ? " Father is there too." : "") + "]\n\nMother: \"" + n + "!\"\n\n["
                + sentence + "]\n\n" + n + ": \"Yes, mother.\"\n";
    }

    static Film direct(String text) {
        Story st = ScriptParser.parse(text);
        Director d = new Director(st, new Director.Options());
        d.prepare();
        return d.direct(new Art());
    }

    // ================================================================== one staged row

    static final class Run {
        Film f, base;
        Film.Seg seg, baseSeg;
        Film.Actor main, baseMain;
        float tA, tB;
        String sig, sig0, sig1;
        String error;
    }

    static Film.Seg scene(Film f) {
        for (Film.Seg s : f.segs) if (s.type == Film.S_SCENE) return s;
        return null;
    }

    static Film.Actor mainActor(Film.Seg s, String name) {
        if (s == null) return null;
        for (Film.Actor a : s.actors) if (a.c != null && (name.equals(a.c.displayName) || name.equals(a.c.fullName) || (a.c.fullName != null && a.c.fullName.startsWith(name)))) return a;
        return null;
    }

    /** VocabProbe's signature of a film (what is staged for whom, without the times). */
    static String sig(Film f) {
        StringBuilder b = new StringBuilder();
        for (Film.Seg sg : f.segs) {
            if (sg.type != Film.S_SCENE) continue;
            for (Film.Actor ac : sg.actors) {
                if (ac.c == null) continue;
                b.append(ac.c.fullName).append(':');
                for (Film.Act a : ac.acts) b.append("A").append(a.type).append(',');
                Film.Key prev = null;
                for (Film.Key k : ac.keys) {
                    if (prev == null || prev.body != k.body) b.append("B").append(k.body).append(',');
                    if (prev == null || Math.abs(prev.x - k.x) > 20) b.append("X,");
                    if (prev == null || prev.emotion != k.emotion) b.append("E").append(k.emotion).append(',');
                    if (prev == null || prev.holdR != k.holdR || prev.holdL != k.holdL) b.append("H").append(k.holdR).append('/').append(k.holdL).append(',');
                    if (prev == null || prev.visible != k.visible) b.append("V").append(k.visible).append(',');
                    if (prev == null || prev.eyesShut != k.eyesShut || prev.tears != k.tears || prev.sweat != k.sweat) b.append("F,");
                    prev = k;
                }
                b.append(';');
            }
            b.append("fx").append(sg.fx.size()).append(';');
        }
        b.append("sfx").append(f.sfx.size());
        return b.toString();
    }

    /** The neutral films of a context (cached): [0] looks at Mother, [1] stays where they are. */
    static final Map<String, Film[]> BASE = new HashMap<String, Film[]>();

    static Film[] baselines(boolean en, Ctx c, boolean girl) {
        String key = (en ? "en/" : "hi/") + c.key() + (girl ? "/girl" : "/boy");
        Film[] b = BASE.get(key);
        if (b != null) return b;
        String n = en ? (girl ? "Priya" : "Rahul") : (girl ? "प्रिया" : "राहुल");
        String s0 = en ? n + " looks at Mother." : n + (girl ? " माँ की ओर देखती है।" : " माँ की ओर देखता है।");
        String s1 = en ? n + " keeps standing." : n + (girl ? " खड़ी रहती है।" : " खड़ा रहता है।");
        b = new Film[]{direct(story(en, c, s0, girl)), direct(story(en, c, s1, girl))};
        BASE.put(key, b);
        return b;
    }

    /** Stages a row, or (neutral) judges the neutral film [0] against the other neutral film [1] for the weak check. */
    static Run stage(boolean en, Ctx c, String sentence, boolean girl, boolean neutral) {
        Run r = new Run();
        try {
            Film[] b = baselines(en, c, girl);
            r.f = neutral ? b[0] : direct(story(en, c, sentence, girl));
            r.base = neutral ? b[1] : b[0];
            r.seg = scene(r.f);
            r.baseSeg = scene(r.base);
            String name = en ? (girl ? "Priya" : "Rahul") : (girl ? "प्रिया" : "राहुल");
            r.main = mainActor(r.seg, name);
            r.baseMain = mainActor(r.baseSeg, name);
            if (r.seg == null || r.main == null || r.baseMain == null) { r.error = "no scene or no " + name + " in the film"; return r; }
            if (r.f.lines.size() < 2) { r.error = "the two lines of dialogue were not found"; return r; }
            Film.Line l0 = r.f.lines.get(0), l1 = r.f.lines.get(r.f.lines.size() - 1);
            r.tA = l0.start + l0.dur - 0.05f;
            r.tB = l1.start;
            r.sig = sig(r.f);
            r.sig0 = sig(b[0]);
            r.sig1 = sig(b[1]);
        } catch (Throwable e) {
            r.error = "exception " + e;
        }
        return r;
    }

    // ================================================================== constants by name

    static final String[] BODIES = {"STAND", "SIT", "LIE", "KNEEL", "HANG", "CROUCH"};
    static final String[] EMOTIONS = {"NEUTRAL", "HAPPY", "LAUGH", "ANGRY", "SAD", "SCARED", "SURPRISED", "EVIL", "DETERMINED", "DIZZY", "WHISPER",
            "PROUD", "CURIOUS", "PAIN", "SUSPICIOUS", "RELIEVED"};

    static int constant(Class<?> c, String name) {
        try {
            Field f = c.getField(name);
            if (!Modifier.isStatic(f.getModifiers()) || f.getType() != int.class) throw new NoSuchFieldException(name);
            return f.getInt(null);
        } catch (Exception e) {
            throw new IllegalArgumentException("no constant " + c.getSimpleName() + "." + name);
        }
    }

    static int film(String prefix, String name) { return constant(Film.class, prefix + name); }

    static int pose(String name, String[] allowed) {
        if (allowed != null && !Arrays.asList(allowed).contains(name)) throw new IllegalArgumentException("not a Pose body/emotion: " + name);
        return constant(Pose.class, name);
    }

    static final Map<String, Map<Integer, String>> NAMES = new HashMap<String, Map<Integer, String>>();

    /** The name of a constant of Film (by prefix) or Pose ("I_", "BODY", "EMO"). */
    static String nameOf(String prefix, int v) {
        Map<Integer, String> m = NAMES.get(prefix);
        if (m == null) {
            m = new HashMap<Integer, String>();
            if (prefix.equals("BODY") || prefix.equals("EMO")) {
                for (String n : prefix.equals("BODY") ? BODIES : EMOTIONS) m.put(constant(Pose.class, n), n);
            } else {
                Class<?> c = prefix.equals("I_") ? Pose.class : Film.class;
                for (Field f : c.getFields()) {
                    if (!Modifier.isStatic(f.getModifiers()) || f.getType() != int.class || !f.getName().startsWith(prefix)) continue;
                    if (f.getName().equals("V_PASSENGER") || f.getName().equals("W_KINDS")) continue;
                    // (names are matched from their start: the prefix "FX_" does not take the SFX_ sounds)
                    try { if (!m.containsKey(f.getInt(null))) m.put(f.getInt(null), f.getName().substring(prefix.length())); } catch (IllegalAccessException e) { /* public */ }
                }
            }
            NAMES.put(prefix, m);
        }
        String n = m.get(v);
        return n != null ? n : String.valueOf(v);
    }

    // ================================================================== checks

    static boolean in(Run r, float t) { return t >= r.tA - 0.05f && t < r.tB - 0.001f; }

    static boolean sameKey(Film.Key a, Film.Key b) {
        return Math.abs(a.t - b.t) < 0.002f && a.body == b.body && a.seat == b.seat && a.emotion == b.emotion && Math.abs(a.x - b.x) < 0.5f
                && a.holdR == b.holdR && a.holdL == b.holdL && a.visible == b.visible && a.eyesShut == b.eyesShut && a.tears == b.tears
                && a.run == b.run && a.anchor == b.anchor && a.sweat == b.sweat && a.costume == b.costume;
    }

    /** The main character's keys in the window that the neutral film does not have (the sentence's own). */
    static List<Film.Key> windowKeys(Run r) {
        List<Film.Key> out = new ArrayList<Film.Key>();
        for (Film.Key k : r.main.keys) {
            if (!in(r, k.t)) continue;
            boolean old = false;
            if (r.baseMain != null && r.baseMain != r.main) for (Film.Key b : r.baseMain.keys) if (sameKey(k, b)) { old = true; break; }
            if (!old) out.add(k);
        }
        return out;
    }

    /** The main character's acts beginning in the window that the neutral film does not have. */
    static List<Film.Act> windowActs(Run r) {
        List<Film.Act> out = new ArrayList<Film.Act>();
        for (Film.Act a : r.main.acts) {
            if (!in(r, a.t0)) continue;
            boolean old = false;
            if (r.baseMain != null && r.baseMain != r.main) for (Film.Act b : r.baseMain.acts) if (b.type == a.type && b.item == a.item && Math.abs(b.t0 - a.t0) < 0.002f) { old = true; break; }
            if (!old) out.add(a);
        }
        return out;
    }

    /** The sounds of the window that the neutral film does not have. */
    static List<Film.Sfx> windowSfx(Run r) {
        List<Film.Sfx> out = new ArrayList<Film.Sfx>();
        for (Film.Sfx s : r.f.sfx) {
            if (!in(r, s.t)) continue;
            boolean old = false;
            if (r.base != r.f) for (Film.Sfx b : r.base.sfx) if (b.type == s.type && Math.abs(b.t - s.t) < 0.002f) { old = true; break; }
            if (!old) out.add(s);
        }
        return out;
    }

    /** The effects of the scene beginning in the window that the neutral film does not have. */
    static List<Film.Fx> windowFx(Run r) {
        List<Film.Fx> out = new ArrayList<Film.Fx>();
        for (Film.Fx x : r.seg.fx) {
            if (!in(r, x.t0)) continue;
            boolean old = false;
            if (r.baseSeg != null && r.baseSeg != r.seg) for (Film.Fx b : r.baseSeg.fx) if (b.type == x.type && Math.abs(b.t0 - x.t0) < 0.002f) { old = true; break; }
            if (!old) out.add(x);
        }
        return out;
    }

    static Film.Key before(Run r) { return r.main.stateAt(r.tA - 0.06f); }

    /** mode 0: item equal; 1: |item| % V_PASSENGER equal; 2: |item| equal; any: item ignored. */
    static boolean act(Run r, int type, int item, int mode, boolean any) {
        for (Film.Act a : windowActs(r)) {
            if (a.type != type) continue;
            if (any) return true;
            int it = mode == 1 ? Math.abs(a.item) % Film.V_PASSENGER : mode == 2 ? Math.abs(a.item) : a.item;
            if (it == item) return true;
        }
        return false;
    }

    static boolean holds(String code, Run r) {
        int c = code.indexOf(':');
        String kind = c < 0 ? code : code.substring(0, c), arg = c < 0 ? "" : code.substring(c + 1);
        switch (kind) {
            case "W": {
                int ty = film("W_", arg);
                for (Film.Weather w : r.f.weather) if (w.type == ty && w.t0 <= r.tB && w.t1 >= r.tA - 0.05f) return true;
                return false;
            }
            case "WEND": {
                int ty = film("W_", arg);
                for (Film.Weather w : r.f.weather) if (w.type == ty && in(r, w.t1)) return true;
                return false;
            }
            case "FX": {
                int ty = film("FX_", arg), n = 0, n0 = 0;
                for (Film.Fx x : windowFx(r)) if (x.type == ty) return true;
                for (Film.Fx x : r.seg.fx) if (x.type == ty) n++;
                if (r.baseSeg != null && r.baseSeg != r.seg) for (Film.Fx x : r.baseSeg.fx) if (x.type == ty) n0++;
                return n > n0;
            }
            case "SFX": {
                int ty = film("SFX_", arg);
                for (Film.Sfx s : windowSfx(r)) if (s.type == ty) return true;
                return false;
            }
            case "ACT": return act(r, film("G_", arg), 0, 0, true);
            case "TASK": return act(r, Film.G_TASK, film("T_", arg), 0, false);
            case "RIDE": return act(r, Film.G_RIDE, film("V_", arg), 1, false);
            case "PG": return act(r, Film.G_PLAYGROUND, film("PG_", arg), 2, false);
            case "EX": return act(r, Film.G_EXERCISE, film("EX_", arg), 0, false);
            case "MK": return act(r, Film.G_MAKEUP, film("MK_", arg), 0, false);
            case "DRINK": return act(r, Film.G_DRINK, constant(Pose.class, "I_" + arg), 0, false);
            case "BODY": {
                int b = pose(arg, BODIES);
                if (before(r).body == b) return false;
                for (Film.Key k : windowKeys(r)) if (k.body == b) return true;
                return false;
            }
            case "SEAT": {
                int s = film("SEAT_", arg);
                for (Film.Key k : windowKeys(r)) if (k.seat == s && (k.body == Pose.SIT || k.body == Pose.LIE)) return true;
                return false;
            }
            case "EMO": {
                int e = pose(arg, EMOTIONS);
                if (before(r).emotion == e) return false;
                for (Film.Key k : windowKeys(r)) if (k.emotion == e) return true;
                return false;
            }
            case "HOLD": {
                int i = constant(Pose.class, "I_" + arg);
                for (Film.Key k : windowKeys(r)) if (k.holdR == i || k.holdL == i) return true;
                return false;
            }
            case "MOVE": return moved(r) > 40;
            case "RUN": { for (Film.Key k : windowKeys(r)) if (k.run) return true; return false; }
            case "EYES": {
                if (before(r).eyesShut) return false;
                for (Film.Key k : windowKeys(r)) if (k.eyesShut) return true;
                return false;
            }
            case "AWAKE": {
                if (!before(r).eyesShut) return false;
                for (Film.Key k : windowKeys(r)) if (!k.eyesShut) return true;
                return false;
            }
            case "TEARS": { for (Film.Key k : windowKeys(r)) if (k.tears) return true; return false; }
            case "SWEAT": { for (Film.Key k : windowKeys(r)) if (k.sweat) return true; return false; }
            case "COSTUME": { int c0 = before(r).costume; for (Film.Key k : windowKeys(r)) if (k.costume != c0) return true; return false; }
            case "HIDE": { for (Film.Key k : windowKeys(r)) if (hidden(k)) return true; return false; }
            case "UMBRELLA": return !r.main.umbrellaAt(r.main.look, r.tA - 0.06f) && r.main.umbrellaAt(r.main.look, r.tB - 0.05f);
            case "ANY": return !r.sig.equals(r.sig0) && !r.sig.equals(r.sig1);
            // v44: a ride's mishap (on anyone's ride in the window: the main character may be the passenger), a
            // passenger's seat, a weapon from the saddle, the bow's mark, play in the snow, a close insert
            case "MISHAP": {
                int m = film("MISHAP_", arg);
                for (Film.Actor a : r.seg.actors) for (Film.Act x : a.acts)
                    if (x.type == Film.G_RIDE && x.mishap == m && x.mishapAt >= 0 && in(r, x.t0 + x.mishapAt)) return true;
                return false;
            }
            case "SEATNO": {
                int sn = film("CAR_", arg);
                for (Film.Act x : r.main.acts) if (x.type == Film.G_RIDE && x.seat == sn && x.t1 > r.tA && x.t0 < r.tB) return true;
                return false;
            }
            case "MOUNTED": {
                int g = film("G_", arg);
                for (Film.Act x : r.main.acts) if (x.type == Film.G_RIDE && x.mounted == g && x.t0 + x.mountedAt < r.tB && x.t0 + x.mountedTo > r.tA) return true;
                return false;
            }
            case "ARCH": return act(r, Film.G_ARCHERY, film("ARCH_", arg), 0, false);
            case "SNOW": return act(r, Film.G_SNOW_PLAY, film("SNOW_", arg), 0, false);
            case "INSERT": {
                int k = film("INS_", arg);
                for (Film.Fx x : windowFx(r)) if (x.type == Film.FX_INSERT && x.kind == k) return true;
                return false;
            }
            default: throw new IllegalArgumentException("unknown code " + code);
        }
    }

    static boolean hidden(Film.Key k) { return !k.visible || k.anchor == Film.A_HIDDEN || k.anchor == Film.A_ROCK_HIDE; }

    /** The largest step of the main character's place in the window (px); a walking track counts as a move. */
    static float moved(Run r) {
        float most = 0;
        List<Film.Key> ks = r.main.keys, own = windowKeys(r);
        for (int i = 1; i < ks.size(); i++) if (own.contains(ks.get(i))) most = Math.max(most, Math.abs(ks.get(i).x - ks.get(i - 1).x));
        for (Film.Track tr : r.main.track) if (tr.walking && tr.t0 < r.tB && tr.t1 > r.tA) most = Math.max(most, 999);
        return most;
    }

    /** Checks the whole expectation: groups split by '|', codes in a group by '&'. */
    static boolean expect(String exp, Run r) {
        for (String grp : exp.split("\\|")) {
            boolean all = true;
            for (String code : grp.split("&")) {
                code = code.trim();
                boolean not = code.startsWith("!");
                if (holds(not ? code.substring(1) : code, r) == not) { all = false; break; }
            }
            if (all) return true;
        }
        return false;
    }

    /** Validates every code of an expectation (unknown kinds and constants stop the probe before anything runs). */
    static void validate(String exp) {
        for (String grp : exp.split("\\|")) for (String code : grp.split("&")) {
            code = code.trim();
            if (code.startsWith("!")) code = code.substring(1);
            int c = code.indexOf(':');
            String kind = c < 0 ? code : code.substring(0, c), arg = c < 0 ? "" : code.substring(c + 1);
            switch (kind) {
                case "W": case "WEND": film("W_", arg); break;
                case "FX": film("FX_", arg); break;
                case "SFX": film("SFX_", arg); break;
                case "ACT": film("G_", arg); break;
                case "TASK": film("T_", arg); break;
                case "RIDE": film("V_", arg); break;
                case "PG": film("PG_", arg); break;
                case "EX": film("EX_", arg); break;
                case "MK": film("MK_", arg); break;
                case "SEAT": film("SEAT_", arg); break;
                case "SEATNO": film("CAR_", arg); break;
                case "MISHAP": film("MISHAP_", arg); break;
                case "MOUNTED": film("G_", arg); break;
                case "ARCH": film("ARCH_", arg); break;
                case "SNOW": film("SNOW_", arg); break;
                case "INSERT": film("INS_", arg); break;
                case "DRINK": case "HOLD": constant(Pose.class, "I_" + arg); break;
                case "BODY": pose(arg, BODIES); break;
                case "EMO": pose(arg, EMOTIONS); break;
                case "MOVE": case "RUN": case "EYES": case "AWAKE": case "TEARS": case "SWEAT": case "COSTUME": case "HIDE": case "UMBRELLA": case "ANY":
                    if (arg.length() > 0) throw new IllegalArgumentException("code takes no argument: " + code);
                    break;
                default: throw new IllegalArgumentException("unknown code " + code);
            }
        }
    }

    // ================================================================== what the film had

    static String actName(Film.Act a) {
        String n = nameOf("G_", a.type);
        switch (a.type) {
            case Film.G_TASK: return n + "/" + nameOf("T_", a.item);
            case Film.G_RIDE: return n + "/" + nameOf("V_", Math.abs(a.item) % Film.V_PASSENGER) + (a.item >= Film.V_PASSENGER ? "(passenger)" : a.item < 0 ? "(behind)" : "")
                    + (a.seat >= 0 ? "[seat " + nameOf("CAR_", a.seat) + "]" : "") + (a.mishap != 0 ? "[" + nameOf("MISHAP_", a.mishap) + "]" : "") + (a.mounted != 0 ? "[" + nameOf("G_", a.mounted) + "]" : "");
            case Film.G_ARCHERY: return n + "/" + nameOf("ARCH_", a.item);
            case Film.G_SNOW_PLAY: return n + "/" + nameOf("SNOW_", a.item);
            case Film.G_PLAYGROUND: return n + "/" + nameOf("PG_", Math.abs(a.item));
            case Film.G_EXERCISE: return n + "/" + nameOf("EX_", a.item);
            case Film.G_MAKEUP: return n + "/" + nameOf("MK_", a.item);
            case Film.G_DRINK: case Film.G_EAT: return a.item != 0 ? n + "/" + nameOf("I_", a.item) : n;
            default: return n;
        }
    }

    static void addOnce(List<String> l, String s) { if (!l.contains(s)) l.add(s); }

    static String summary(Run r) {
        if (r.error != null) return r.error;
        List<String> parts = new ArrayList<String>();
        List<String> acts = new ArrayList<String>();
        for (Film.Act a : windowActs(r)) addOnce(acts, actName(a));
        if (!acts.isEmpty()) parts.add("acts" + acts);
        Film.Key b0 = before(r);
        List<String> body = new ArrayList<String>(), emo = new ArrayList<String>(), hold = new ArrayList<String>(), flags = new ArrayList<String>();
        for (Film.Key k : windowKeys(r)) {
            if (k.body != b0.body || k.seat != b0.seat) addOnce(body, nameOf("BODY", k.body) + (k.seat >= 0 ? "/" + nameOf("SEAT_", k.seat) : ""));
            if (k.emotion != b0.emotion) addOnce(emo, nameOf("EMO", k.emotion));
            if (k.holdR != Pose.I_NONE) addOnce(hold, nameOf("I_", k.holdR));
            if (k.holdL != Pose.I_NONE) addOnce(hold, nameOf("I_", k.holdL));
            if (k.run) addOnce(flags, "run");
            if (k.eyesShut && !b0.eyesShut) addOnce(flags, "eyes-shut");
            if (!k.eyesShut && b0.eyesShut) addOnce(flags, "eyes-open");
            if (k.tears) addOnce(flags, "tears");
            if (k.sweat) addOnce(flags, "sweat");
            if (k.costume != b0.costume) addOnce(flags, "costume" + k.costume);
            if (hidden(k)) addOnce(flags, k.visible ? "hidden" : "invisible");
            if (k.backTurned) addOnce(flags, "back-turned");
            if (k.facing != b0.facing) addOnce(flags, "turns");
        }
        if (!body.isEmpty()) parts.add("body" + body);
        if (!emo.isEmpty()) parts.add("emo" + emo);
        if (!hold.isEmpty()) parts.add("hold" + hold);
        float mv = moved(r);
        if (mv > 40) flags.add(0, mv >= 999 ? "walks" : "moves " + Math.round(mv) + "px");
        if (r.main.umbrellaAt(r.main.look, r.tB - 0.05f)) flags.add(r.main.umbrellaAt(r.main.look, r.tA - 0.06f) ? "umbrella(already before the sentence)" : "umbrella");
        if (!flags.isEmpty()) parts.add("flags" + flags);
        List<String> w = new ArrayList<String>();
        for (Film.Weather x : r.f.weather) {
            if (in(r, x.t0)) addOnce(w, "+" + nameOf("W_", x.type));
            else if (x.t0 <= r.tB && x.t1 >= r.tA) addOnce(w, nameOf("W_", x.type));
            if (in(r, x.t1)) addOnce(w, "-" + nameOf("W_", x.type));
        }
        if (!w.isEmpty()) parts.add("weather" + w);
        List<String> fx = new ArrayList<String>();
        for (Film.Fx x : windowFx(r)) addOnce(fx, nameOf("FX_", x.type));
        if (r.baseSeg != null && r.baseSeg != r.seg) {
            Map<Integer, Integer> n = new HashMap<Integer, Integer>();
            for (Film.Fx x : r.seg.fx) n.put(x.type, (n.containsKey(x.type) ? n.get(x.type) : 0) + 1);
            for (Film.Fx x : r.baseSeg.fx) n.put(x.type, (n.containsKey(x.type) ? n.get(x.type) : 0) - 1);
            for (Map.Entry<Integer, Integer> e : n.entrySet()) if (e.getValue() > 0) addOnce(fx, nameOf("FX_", e.getKey()));
        }
        if (!fx.isEmpty()) parts.add("fx" + fx);
        List<String> sfx = new ArrayList<String>();
        for (Film.Sfx s : windowSfx(r)) addOnce(sfx, nameOf("SFX_", s.type));
        if (!sfx.isEmpty()) parts.add("sfx" + sfx);
        // what the others on the stage were given with the sentence (an action the director gave to the wrong one)
        for (Film.Actor o : r.seg.actors) {
            if (o == r.main || o.c == null) continue;
            Film.Actor ob = r.baseSeg == null ? null : r.baseSeg.find(o.c) != null ? r.baseSeg.find(o.c) : findByName(r.baseSeg, o.c.fullName);
            List<String> oa = new ArrayList<String>();
            for (Film.Act a : o.acts) {
                if (!in(r, a.t0) || a.type == Film.G_TALK) continue;
                boolean old = false;
                if (ob != null && ob != o) for (Film.Act b : ob.acts) if (b.type == a.type && b.item == a.item && Math.abs(b.t0 - a.t0) < 0.002f) { old = true; break; }
                if (!old) addOnce(oa, actName(a));
            }
            if (!oa.isEmpty()) parts.add(o.c.fullName + oa);
        }
        if (parts.isEmpty()) return "nothing" + (r.sig.equals(r.sig0) || r.sig.equals(r.sig1) ? " (same as the neutral sentence)" : " in the window");
        StringBuilder b = new StringBuilder();
        for (String p : parts) { if (b.length() > 0) b.append(' '); b.append(p); }
        return b.toString().replace(", ", ",");
    }

    static Film.Actor findByName(Film.Seg s, String fullName) {
        if (s == null || fullName == null) return null;
        for (Film.Actor a : s.actors) if (a.c != null && fullName.equals(a.c.fullName)) return a;
        return null;
    }

    static void dump(Run r) {
        if (r.error != null) { System.out.println("    " + r.error); return; }
        System.out.printf(Locale.US, "    set %s (%s), hour %d   window %.2f..%.2f  lines:", Sets.label(r.seg.set), Sets.outdoorSet(r.seg.set) ? "outdoors" : "indoors", r.seg.tod, r.tA, r.tB);
        for (Film.Line l : r.f.lines) System.out.printf(Locale.US, " [%.2f+%.2f %s]", l.start, l.dur, l.text);
        System.out.println();
        for (Film.Key k : r.main.keys) System.out.printf(Locale.US, "    key %6.2f x=%4.0f md=%.2f body=%s seat=%d emo=%s hold=%d/%d vis=%b run=%b eyes=%b%n", k.t, k.x, k.moveDur,
                nameOf("BODY", k.body), k.seat, nameOf("EMO", k.emotion), k.holdR, k.holdL, k.visible, k.run, k.eyesShut);
        for (Film.Act a : r.main.acts) System.out.printf(Locale.US, "    act %6.2f..%6.2f %s%n", a.t0, a.t1, actName(a));
        for (Film.Weather w : r.f.weather) System.out.printf(Locale.US, "    weather %6.2f..%6.2f %s%n", w.t0, w.t1, nameOf("W_", w.type));
        for (Film.Fx x : r.seg.fx) System.out.printf(Locale.US, "    fx %6.2f..%6.2f %s%n", x.t0, x.t1, nameOf("FX_", x.type));
        for (Film.Sfx s : r.f.sfx) if (s.t > r.tA - 3 && s.t < r.tB + 3) System.out.printf(Locale.US, "    sfx %6.2f %s%n", s.t, nameOf("SFX_", s.type));
    }

    // ================================================================== main

    static final class Row { String lang, sentence, expect; Ctx ctx; int line; }

    static Ctx directive(String line, int no) {
        Ctx c = new Ctx();
        for (String kv : line.substring(2).trim().split("\\s+")) {
            if (kv.isEmpty()) continue;
            int e = kv.indexOf('=');
            if (e < 0) throw new IllegalArgumentException("line " + no + ": bad directive " + kv);
            String k = kv.substring(0, e), v = kv.substring(e + 1);
            switch (k) {
                case "cat": c.cat = v; break;
                case "place": place(v); c.place = v; break;
                case "start": c.start = v; break;
                case "tod": hiTod(v); c.tod = v; break;
                case "age": if (!v.equals("kid") && !v.equals("adult")) throw new IllegalArgumentException("line " + no + ": age " + v); c.age = v; break;
                case "fam": c.fam = v.equals("1"); break;
                default: throw new IllegalArgumentException("line " + no + ": unknown directive " + k);
            }
        }
        if (!Arrays.asList("stand", "chair", "sofa", "floor", "bed", "throne", "rain", "fog", "wind").contains(c.start)) throw new IllegalArgumentException("line " + no + ": start " + c.start);
        return c;
    }

    /** ONLY: a line number, a range of line numbers, or text in the sentence (or the category's name). */
    static boolean selected(String only, Row row) {
        if (only.matches("\\d+")) return row.line == Integer.parseInt(only);
        if (only.matches("\\d+-\\d+")) { String[] p = only.split("-"); return row.line >= Integer.parseInt(p[0]) && row.line <= Integer.parseInt(p[1]); }
        return row.sentence.contains(only) || row.ctx.cat.equals(only);
    }

    public static void main(String[] args) throws Exception {
        if (args.length < 1) { System.err.println("usage: PhenomenaProbe probe.tsv"); System.exit(2); }
        List<Row> rows = new ArrayList<Row>();
        Ctx ctx = new Ctx();
        int no = 0;
        for (String line : Files.readAllLines(Paths.get(args[0]), StandardCharsets.UTF_8)) {
            no++;
            String t = line.trim();
            if (t.startsWith("#@")) { ctx = directive(t, no); continue; }
            if (t.isEmpty() || t.startsWith("#")) continue;
            String[] f = line.split("\t");
            if (f.length < 3) throw new IllegalArgumentException("line " + no + ": expected lang<TAB>sentence<TAB>expect: " + line);
            Row r = new Row();
            r.lang = f[0].trim(); r.sentence = f[1].trim(); r.expect = f[2].trim(); r.ctx = ctx.copy(); r.line = no;
            if (!r.lang.equals("hi") && !r.lang.equals("en")) throw new IllegalArgumentException("line " + no + ": lang " + r.lang);
            try { validate(r.expect); } catch (IllegalArgumentException e) { throw new IllegalArgumentException("line " + no + ": " + e.getMessage()); }
            rows.add(r);
        }
        String only = System.getenv("ONLY");
        boolean show = System.getenv("SHOW") != null, dumpAll = System.getenv("DUMP") != null;
        int pass = 0, n = 0, hiP = 0, hiN = 0, enP = 0, enN = 0;
        Map<String, int[]> cats = new LinkedHashMap<String, int[]>();      // hi pass, hi total, en pass, en total
        List<String> weak = new ArrayList<String>();
        for (Row row : rows) {
            if (only != null && !selected(only, row)) continue;
            boolean en = row.lang.equals("en");
            boolean girl = en ? row.sentence.contains("Priya") && !row.sentence.contains("Rahul") : row.sentence.contains("प्रिया") && !row.sentence.contains("राहुल");
            Run r = stage(en, row.ctx, row.sentence, girl, false);
            boolean ok;
            try { ok = r.error == null && expect(row.expect, r); }
            catch (Throwable e) { ok = false; r.error = "exception " + e; }
            n++;
            if (ok) pass++;
            if (en) { enN++; if (ok) enP++; } else { hiN++; if (ok) hiP++; }
            int[] c = cats.get(row.ctx.cat);
            if (c == null) cats.put(row.ctx.cat, c = new int[4]);
            c[en ? 2 : 0] += ok ? 1 : 0;
            c[en ? 3 : 1]++;
            if (!ok) System.out.println("FAIL " + row.lang + " " + row.sentence + " " + row.expect + " -> got: " + summary(r));
            else if (show) System.out.println("pass " + row.lang + " " + row.sentence + " " + row.expect + " -> " + summary(r));
            if (dumpAll) { System.out.println("    (line " + row.line + ", " + row.ctx.key() + (girl ? ", Priya" : "") + ")"); dump(r); }
            // a weak test: the neutral sentence passes the same expectation
            if (ok && r.error == null) {
                Run b = stage(en, row.ctx, null, girl, true);
                try { if (b.error == null && expect(row.expect, b)) weak.add(row.lang + " " + row.sentence + " " + row.expect); } catch (Throwable e) { /* ignore */ }
            }
        }
        System.out.println();
        System.out.println(String.format(Locale.US, "%-14s %9s %9s %9s", "category", "hi", "en", "all"));
        for (Map.Entry<String, int[]> e : cats.entrySet()) {
            int[] c = e.getValue();
            System.out.println(String.format(Locale.US, "%-14s %9s %9s %9s", e.getKey(), c[1] > 0 ? c[0] + "/" + c[1] : "-", c[3] > 0 ? c[2] + "/" + c[3] : "-", (c[0] + c[2]) + "/" + (c[1] + c[3])));
        }
        if (!weak.isEmpty()) {
            System.out.println();
            System.out.println("WEAK (the neutral sentence passes the same expectation):");
            for (String w : weak) System.out.println("  " + w);
        }
        System.out.println();
        System.out.println("PROBE " + pass + " of " + n + " (hi " + hiP + "/" + hiN + ", en " + enP + "/" + enN + ")");
    }
}
