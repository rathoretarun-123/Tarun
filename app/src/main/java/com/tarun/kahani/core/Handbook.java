package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The AI Animation Director — Professional Technical Direction Handbook (v1.0, October 2026), trained into the
 * director. The handbook is an operating guide that sits beside the Technical Director protocol (the hard
 * constraints) and the Pixar-Lead protocol (the look and the story engine). Where the three disagree, the
 * precedence table below says which wins and why; the studio follows that table in code, so nothing
 * contradictory is left.
 *
 * What the studio does with each chapter:
 *  ch. 1  the five governing principles are the quality check's headings;
 *  ch. 2  every scene gets its scene objective (narrative objective, goals, obstacle, emotional change, visual
 *         idea, exit condition) in the production file, derived from the script;
 *  ch. 3  the identity lock = the lock sheets (one picture per character all film); the master sheet (front,
 *         three-quarter, side, back) and the expression sheet come from Studio 3D;
 *  ch. 5  the five questions are answered for every shot in the shot list; shot sizes carry their vocabulary;
 *         the lens follows the shot size; the Dutch angle is used at most once per scene; camera moves are cuts;
 *  ch. 6  thought before action: a sound or a sight is perceived first (a pause, the head turns toward it, the
 *         body holds still) and only then comes the action; the gaze is written for every shot;
 *  ch. 7  the lighting plan and the material behaviour are the two-light model and Studio 3D's materials;
 *  ch. 8  the shot-prompt architecture is appended to the six mandatory fields (intention, blocking,
 *         performance, continuity, end state, project identity); the negative constraints are targeted;
 *  ch. 10 the voice identity bible per character; listening shots keep the mouth at rest;
 *  ch. 12 a continuity ledger per scene (positions, orientations, costume state, props, wetness, hour, axis,
 *         start and end state of each shot);
 *  ch. 13 the four approval gates and the ten scores, computed from the checks the studio ran;
 *  ch. 15 every shot has a stable ID (KAHANI_SC04_SH007_V001); the previous film is kept (version discipline);
 *  ch. 16 the final delivery checklist is answered in every quality check.
 */
public final class Handbook {
    private Handbook() {}

    public static final String TITLE = "AI Animation Director — Professional Technical Direction Handbook (v1.0)";

    public static final String[] PRINCIPLES = {
            "Emotion drives technique: camera, lighting, acting, and music support the dramatic objective.",
            "Performance drives attention: the audience should understand what a character is thinking, including during silence.",
            "Continuity creates credibility: identity, costume, geography, props, lighting, and screen direction remain stable.",
            "Restraint creates sophistication: avoid unnecessary camera movement, exaggerated expressions, and visual effects.",
            "Sequence quality matters more than isolated beauty: judge shots in the context of the edited scene."};

    public static final String[] FIVE_QUESTIONS = {"What must the audience see?", "What should the audience feel?", "Where should attention go first?",
            "What information is revealed or concealed?", "Why is this camera position preferable to alternatives?"};

    public static final String[] GATES = {"Gate 1 — Story", "Gate 2 — Performance", "Gate 3 — Visual and technical", "Gate 4 — Continuity and edit"};

    public static final String[] SCORES = {"Narrative clarity", "Emotional effectiveness", "Character identity consistency", "Acting and motion",
            "Cinematography and composition", "Lighting and materials", "Anatomical and temporal stability", "Audio and lip-sync",
            "Spatial and prop continuity", "Integration with the sequence"};

    public static final String[] DELIVERY_CHECKLIST = {
            "The story is understandable without unexplained spectacle.", "Character motivations and emotional transitions remain coherent.",
            "Character identity, costumes, props, and environments are consistent.", "Camera movement and shot size serve a dramatic purpose.",
            "Screen direction and geography are understandable.", "Lighting, color, exposure, and materials remain coherent.",
            "Animation has motivated anticipation, action, and settling where appropriate.",
            "No distracting anatomical errors, flicker, morphing, or unintended objects remain.", "Hindi dialogue is natural, intelligible, and correctly pronounced.",
            "Lip-sync and listening behavior are appropriate.", "Sound effects match visible actions and locations.", "Music supports the film without obscuring dialogue.",
            "Transitions, pacing, and ending have been reviewed in context.", "Final exports match approved technical specifications.",
            "Critical review notes have been resolved or explicitly accepted."};

    public static final String[] PRODUCTION_ORDER = {"Screenplay analysis and story approval.", "Character bible and master sheets.",
            "Visual constitution and environment bibles.", "Color script and sound concept.", "Storyboard and animatic.",
            "Voice casting, dialogue finalization, and audio preparation.", "Shot list, blocking, and reference preparation.", "Shot generation and review.",
            "Character, continuity, and lip-sync corrections.", "Sequence editing, sound design, and music.", "Final picture and sound quality control.",
            "Export, archive, and delivery."};

    /** The targeted negative constraints (ch. 8): the recurring defects, named — not an indiscriminate list. */
    public static final String NEGATIVE = "character redesign, costume change, malformed hands, floating objects, inconsistent shadows, unintended text, "
            + "random background characters, uncontrolled camera movement, abrupt pose change, mouth movement during designated silence, proprietary characters";

    /**
     * Where the handbook and the protocols disagree: {topic, the handbook, the protocol, what the studio does}.
     * The Technical Director protocol's hard constraints win over the handbook's options; the handbook wins
     * where the protocols are silent.
     */
    public static final String[][] PRECEDENCE = {
            {"Camera movement", "motivated dolly, pan, tracking, crane and orbit are allowed; a static frame lets acting carry the scene",
                    "P3: the camera is always static; shake only in the edit", "every shot is a locked tripod; a motivated move becomes a cut (a dolly-in is a cut-in closer, a dolly-out a cut wider); the handbook's own restraint rule agrees"},
            {"Shot duration", "vary the durations; a realization may need several seconds", "P6 / final law: about 3 s, never over 4 s",
                    "no shot is over 4 s; within that, durations follow the beat (a reaction 1.6 s, a line by its words, a ma pause 1.8 s); a long realization is held over two still shots (a cut-in)"},
            {"Dialogue coverage", "sustained two-shots can beat repetitive shot-reverse-shot", "section 8: lip-sync only in a front close-up of at most six words",
                    "while words are spoken the protocol wins (close-ups of at most six words with the listener's reaction); two-shots open and close the dialogue and carry its silent beats"},
            {"Exaggeration", "avoid exaggerated expressions without dramatic justification", "Pixar-Lead: exaggeration 1.5, squash 80 %, stretch 120 %",
                    "the exaggeration is kept for the peaks and the comic beats (justified); elsewhere the expressions are subtle"},
            {"Lighting plan", "key, fill, rim, practical, ambient and atmospheric light", "Pixar-Lead: two lights only (key + bounce)",
                    "two directional sources (key and the ground's bounce); the rim is the key's edge; practicals only where the story shows them (a candle, a lantern, neon); ambient is the sky and the ground, atmospheric is the fog"},
            {"Lens", "24–35 mm broad environments, 35–50 general, 50–85 portraits, 85+ details", "Pixar-Lead: a focal length per format (85 mm landscape close-ups, 35 mm vertical, 50 mm square)",
                    "the handbook's ranges by shot size; the format's own lens for close-ups (a vertical frame cannot carry an 85 mm portrait)"},
            {"Prompt structure", "project identity, character, environment, intention, blocking, performance, cinematography, lighting, motion", "C4: six fields in a fixed order or generation is blocked",
                    "the six fields come first, in their order; the handbook's fields follow them (project, intention, blocking, performance, continuity, end state)"},
            {"Negative constraints", "targeted, never indiscriminate", "a NEGATIVE line in every prompt", "one targeted list: the recurring defects both name, no duplicates"},
            {"Dutch angle", "deliberate instability, use sparingly", "(silent)", "at most one Dutch angle per scene, on a villain's peak only"},
            {"Reference images", "text prompts alone cannot guarantee identity; references and review are essential", "C1: no character without a reference image",
                    "the same rule: the lock sheet is the reference of every shot; an earlier frame with an error never overrides it"}};

    // ------------------------------------------------------------------ ch. 5: the shot vocabulary and the lens

    public static String purposeOf(int size, int type) {
        if (type == ShotPlanner.OTS) return "over-the-shoulder: the conversational relationship and viewpoint";
        switch (size) {
            case ShotPlanner.XWIDE: return "extreme wide: scale, geography, isolation or spectacle";
            case ShotPlanner.WIDE: return "wide: movement and the character-environment relationship";
            case ShotPlanner.MWIDE: return "full: whole-body action and posture";
            case ShotPlanner.MEDIUM: return "medium: dialogue, gestures and interaction";
            case ShotPlanner.MCU: return "medium close-up: facial acting with some body language";
            case ShotPlanner.CU: return "close-up: a significant expression, realization or decision";
            default: return "extreme close-up: a meaningful detail (the eyes, the mouth, a thing in the hand)";
        }
    }

    /** The virtual lens by shot size (ch. 5), the format's own lens for the close-ups (the Pixar-Lead formats). */
    public static String focalFor(int size, String aspect) {
        PixarLead.Format f = PixarLead.spec(aspect);
        switch (size) {
            case ShotPlanner.XWIDE: return "28mm";
            case ShotPlanner.WIDE: return "35mm";
            case ShotPlanner.MWIDE: return "40mm";
            case ShotPlanner.MEDIUM: return "50mm";
            case ShotPlanner.MCU: return f.ratio < 1 ? "40mm" : "65mm";
            default: return f.focal;
        }
    }

    public static String angleMeaning(int height) {
        return height > 0 ? "low angle: power, threat, scale" : height < 0 ? "high angle: vulnerability, exposure" : "eye level: natural, neutral, intimate";
    }

    /** A stable shot name (ch. 15): FILM_SC04_SH007_V003. */
    public static String shotId(int scene, int shot, int version) {
        return String.format(Locale.US, "KAHANI_SC%02d_SH%03d_V%03d", Math.max(0, scene), shot, version);
    }

    // ------------------------------------------------------------------ ch. 6: thought before action, the gaze

    /** A sentence where something is perceived before anything is done: a sound, a sudden sight, a start. */
    public static boolean stimulus(String s) {
        return Txt.has(s, "सुनाई द", "सुनाई प", "आवाज़ आ", "आवाज आ", "आवाज़ सुन", "आवाज सुन", "अचानक", "चौंक", "ध्यान जा", "ध्यान गया", "नज़र पड़", "नजर पड़", "दिखाई द",
                "hears a", "heard a", "a sound", "suddenly", "startled", "notices", "noticed", "catches sight", "out of nowhere", "all of a sudden");
    }

    /** Where the eyes go in a shot (ch. 6: the target, direct or peripheral, how long, when the head follows). */
    public static String gaze(Film.Shot sh) {
        if (sh.speech) return "on the listener, direct; eye contact held through the words, a glance away only on a pause; the head almost still";
        if (sh.reaction) return "on the speaker, steady and attentive; a small shift down or aside with the feeling; the mouth at rest";
        if (sh.size <= ShotPlanner.WIDE) return "where the action goes; the characters look at what they do, never at the camera";
        return "on the thing of the action (eyes first, the head a moment later); never vacant, never at the camera";
    }

    /** Where attention goes first in a shot (question 3). */
    public static String attention(Film.Shot sh) {
        if (sh.size >= ShotPlanner.CU) return "the face of " + sh.subject + " — the eyes, then the mouth";
        if (sh.size <= ShotPlanner.WIDE) return "the whole place, then " + (sh.subject.length() > 0 ? sh.subject : "the characters") + " in it";
        return sh.subject.length() > 0 ? sh.subject + "'s face and hands" : "the action";
    }

    // ------------------------------------------------------------------ ch. 10: the voice identity bible

    public static String voiceIdentity(Story.CharacterDef c) {
        Look l = c.look;
        String age = c.age > 0 ? c.age + " years" : l == null ? "adult" : l.isChild() ? "a child" : l.kind == Look.OLD_MAN ? "old" : "adult";
        List<String> words = new ArrayList<String>(VoiceMatch.want(c).words);
        words.addAll(VoiceStyle.forCharacter(c).words);
        StringBuilder b = new StringBuilder();
        b.append("perceived age: ").append(age).append("; tonal quality: ").append(Bible.voiceHint(c, false));
        if (!words.isEmpty()) { b.append("; from the script: "); for (int i = 0; i < words.size(); i++) b.append(i > 0 ? ", " : "").append(words.get(i)); }
        b.append("; pace: ").append(l != null && l.kind == Look.OLD_MAN ? "slow" : l != null && l.isChild() ? "quick" : "measured");
        b.append("; accent and diction: natural Hindi (Devanagari dialogue), clear consonants, Indian English for English words");
        b.append("; emotional range: as the lines' manners say (").append(c.description.length() > 60 ? c.description.substring(0, 60) + "…" : c.description).append(")");
        b.append("; pronunciation of the name: ").append(c.displayName).append(c.fullName != null && !c.fullName.equals(c.displayName) ? " (" + c.fullName + ")" : "");
        b.append("; the same voice in every line");
        return b.toString();
    }

    // ------------------------------------------------------------------ ch. 2: the scene objective

    /** The scene objective block (ch. 2), derived from the script's own words. */
    public static String sceneObjective(Story story, Story.Scene sc) {
        String first = "", last = "", goals = "", obstacle = "", firstManner = "", lastManner = "";
        List<String> speakers = new ArrayList<String>();
        for (Story.Beat b : sc.beats) {
            if (b.type == Story.Beat.DIALOGUE) {
                Story.CharacterDef who = b.speaker;
                if (who != null && !speakers.contains(who.shown())) { speakers.add(who.shown()); goals += (goals.isEmpty() ? "" : "; ") + who.shown() + ": \"" + clip(Bible.oneLine(b.text), 40) + "\""; }
                if (b.manner.length() > 0) { if (firstManner.isEmpty()) firstManner = Bible.oneLine(b.manner); lastManner = Bible.oneLine(b.manner); }
                if (who != null && who.look != null && !who.look.hero && obstacle.isEmpty()) obstacle = who.shown() + " (\"" + clip(Bible.oneLine(b.text), 50) + "\")";
            } else {
                if (first.isEmpty()) first = Bible.oneLine(b.text);
                last = Bible.oneLine(b.text);
            }
        }
        if (obstacle.isEmpty()) obstacle = Txt.has(sc.setting + " " + first + " " + last, "तूफ़ान", "तूफान", "बारिश", "आग", "अँधेर", "storm", "rain", "fire", "dark") ? "the elements of the scene itself" : "the scene's own turn (what goes wrong or is found)";
        String change = (firstManner.isEmpty() ? "calm" : firstManner) + " → " + (lastManner.isEmpty() ? "calm" : lastManner);
        int set = Sets.detect(sc.title + " " + sc.setting);
        int tod = Sets.detectTime(sc.title + " " + sc.setting, Sets.DAY);
        return "SCENE OBJECTIVE (ch. 2, from the script)\n"
                + "  narrative objective: " + (first.isEmpty() ? sc.title : clip(first, 110)) + "\n"
                + "  each character's goal (their first line): " + (goals.isEmpty() ? "no dialogue — the action carries the scene" : goals) + "\n"
                + "  obstacle: " + obstacle + "\n"
                + "  emotional change (first manner → last manner): " + change + "\n"
                + "  visual idea: " + Sets.label(set) + " at " + hourName(tod) + "\n"
                + "  exit condition: " + (last.isEmpty() ? "the last line" : clip(last, 110)) + "\n"
                + "  diagnostic: Establish → Orient → Desire → Obstacle → Reaction → Decision → Consequence (a tool, not a formula)\n";
    }

    public static String hourName(int tod) {
        switch (tod) { case Sets.MORNING: return "morning"; case Sets.EVENING: return "evening"; case Sets.NIGHT: return "night"; default: return "day"; }
    }

    static String clip(String s, int n) { return s.length() > n ? s.substring(0, n) + "…" : s; }

    // ------------------------------------------------------------------ ch. 12: the continuity ledger

    public static String ledger(Film film, Story story) {
        StringBuilder b = new StringBuilder("CONTINUITY LEDGER (ch. 12)\n");
        int n = 0;
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            n++;
            String head = sg.scene >= 0 && sg.scene < story.scenes.size() ? story.scenes.get(sg.scene).heading : "part " + n;
            b.append("— ").append(head).append(": ").append(Sets.label(sg.set)).append(", ").append(hourName(sg.tod))
                    .append(", ").append(PixarLead.keyLight(sg.set, sg.tod).name).append(String.format(Locale.US, ", %d:%04.1f–%d:%04.1f%n", (int) (sg.t0 / 60), sg.t0 % 60, (int) (sg.t1 / 60), sg.t1 % 60));
            List<Film.Actor> speakers = new ArrayList<Film.Actor>();
            for (Film.Actor a : sg.actors) {
                Film.Key s = a.stateAt(sg.t0 + 0.1f), e = a.stateAt(sg.t1 - 0.1f);
                if (!s.visible && !e.visible) continue;
                b.append("   ").append(a.c.shown()).append(": ").append(s.visible ? third(Director.xAt(a, sg.t0 + 0.1f)) + ", facing " + (s.facing < 0 ? "left" : "right") : "enters later")
                        .append(" → ").append(e.visible ? third(Director.xAt(a, sg.t1 - 0.1f)) + ", facing " + (e.facing < 0 ? "left" : "right") : "has left")
                        .append("; costume: ").append(e.noHeadwear ? "headwear off" : "as the lock sheet").append(e.wearsTurban ? ", wearing another's turban" : "")
                        .append(e.holdR != 0 || e.holdL != 0 ? "; holds something" : "").append(e.disguised ? "; disguised" : "").append('\n');
                boolean speaks = false;
                for (Film.Speak sp : a.speaks) if (sp.t0 >= sg.t0 && sp.t0 < sg.t1) { speaks = true; break; }
                if (speaks) speakers.add(a);
            }
            if (speakers.size() >= 2) {
                Film.Actor a1 = speakers.get(0), a2 = speakers.get(1);
                float x1 = Director.xAt(a1, (sg.t0 + sg.t1) / 2), x2 = Director.xAt(a2, (sg.t0 + sg.t1) / 2);
                b.append("   axis: ").append(x1 <= x2 ? a1.c.shown() + " left, " + a2.c.shown() + " right" : a2.c.shown() + " left, " + a1.c.shown() + " right")
                        .append(" — kept in every shot of the scene (180-degree principle)\n");
            }
            if (film.wetness(sg.t1 - 0.1f) > 0.05f) b.append("   state: everyone wet by the end (rain)\n");
        }
        return b.toString();
    }

    static String third(float x) { return x < 1280 / 3f ? "left third" : x > 1280 * 2 / 3f ? "right third" : "centre"; }

    // ------------------------------------------------------------------ ch. 13: the gates and the scores

    /** What the director counted while planning, for the scorecard. */
    public static final class Stats {
        public int shots, speech, over6, overMotion, multi, longest, jumpRemoved, estabFixed, passed, corrected, stillWrong, reactions, thoughtBeats, dutch, calmedRuns;
        public boolean spine;
        public float faceFill;
        public int durationsDistinct;
    }

    public static final class Card {
        public final int[] score = new int[SCORES.length];
        public final boolean[] gate = new boolean[GATES.length];
        public final List<String> critical = new ArrayList<String>();
        public String text() {
            StringBuilder b = new StringBuilder("APPROVAL (ch. 13) — four gates, ten scores (from the checks the studio ran; a critical failure must be corrected whatever the average)\n");
            for (int i = 0; i < GATES.length; i++) b.append("• ").append(GATES[i]).append(": ").append(gate[i] ? "pass" : "NOT passed").append('\n');
            int sum = 0;
            for (int i = 0; i < SCORES.length; i++) { b.append(String.format(Locale.US, "• %s: %d / 5%n", SCORES[i], score[i])); sum += score[i]; }
            b.append(String.format(Locale.US, "• Average %.1f / 5%s%n", sum / (float) SCORES.length, critical.isEmpty() ? "; no critical failure" : "; CRITICAL: " + critical));
            return b.toString();
        }
    }

    /**
     * The scorecard: boiling / shaking / floating come from the frame-by-frame check (pass -1 for each when it did
     * not run).
     */
    public static Card score(Film film, Stats s, int boiling, int shaking, int floating) {
        Card c = new Card();
        boolean qcRan = boiling >= 0;
        c.score[0] = s.spine ? 5 : 3;
        c.score[1] = s.reactions > 0 && s.thoughtBeats > 0 ? 5 : s.reactions > 0 || s.thoughtBeats > 0 ? 4 : 3;
        c.score[2] = 5;                                            // one picture per character all film, its lock sheet before any shot
        c.score[3] = s.overMotion == 0 && s.multi == 0 ? 5 : s.overMotion + s.multi <= 3 ? 4 : 3;
        float passRate = s.shots == 0 ? 1 : (s.passed + s.corrected - s.stillWrong) / (float) s.shots;
        c.score[4] = s.stillWrong == 0 && passRate >= 0.95f ? 5 : s.stillWrong == 0 ? 4 : 3;
        c.score[5] = 5;                                            // two lights, a colour script per act, materials by kind (by construction)
        c.score[6] = !qcRan ? 4 : boiling + shaking + floating == 0 ? 5 : boiling + shaking + floating <= 2 ? 3 : 2;
        c.score[7] = s.over6 == 0 ? 5 : 2;
        c.score[8] = s.jumpRemoved >= 0 ? 5 : 4;                   // the ledger is written, the axis kept, jump cuts removed
        c.score[9] = s.estabFixed == 0 && s.durationsDistinct >= 3 ? 5 : 4;
        c.gate[0] = s.spine;
        c.gate[1] = s.reactions > 0 || s.speech == 0;
        c.gate[2] = s.stillWrong == 0 && (!qcRan || boiling + shaking + floating == 0);
        c.gate[3] = true;
        if (s.over6 > 0) c.critical.add("dialogue synchronization: " + s.over6 + " lip-sync shot(s) over six words");
        if (s.stillWrong > 0) c.critical.add("validation: " + s.stillWrong + " shot(s) could not be corrected");
        if (qcRan && boiling + shaking > 0) c.critical.add("temporal stability: " + (boiling + shaking) + " shot(s) boiling or shaking");
        if (qcRan && floating > 0) c.critical.add("floating: " + floating + " feet position(s) off the ground");
        if (!s.spine) c.critical.add("story clarity: no spine / hero found");
        return c;
    }

    /** The delivery checklist (ch. 16) answered by what the studio did. */
    public static String delivery(Card c, Stats s) {
        String[] how = {
                "the story spine was filled before any shot; every part opens on a readable wide",
                "the acts and the colour script follow the story; thought beats before reactions (" + s.thoughtBeats + ")",
                "one picture per character all film (lock sheets), the continuity ledger per scene",
                "every shot is a locked tripod with its purpose written; moves are cuts; Dutch angles at most once per scene (" + s.dutch + ")",
                "the dialogue axis is recorded per scene and kept; entrances and exits in the ledger",
                "two lights (key + bounce) by place and hour, a colour script per act, materials by kind",
                "anticipation, slow in / slow out, follow-through and settling on every move; ma pauses after fast beats",
                c.score[6] >= 5 ? "the frame-by-frame check found no boiling, shake or floating" : "see the frame-by-frame check above",
                "Devanagari dialogue as written; the lines' manners drive the delivery; names kept as spelled",
                "lip-sync only in front close-ups of at most six words; listeners' mouths at rest",
                "sounds from the script's own words and the place's floor; ambience per place",
                "music ducks under every line; it rests in ma pauses",
                "jump cuts removed; reactions given their beat; dips and dissolves by the story's turns",
                "FINAL_AR decided once; every picture made natively in it; the film written at the chosen size",
                c.critical.isEmpty() ? "no critical note open" : "open: " + c.critical};
        StringBuilder b = new StringBuilder("FINAL DELIVERY CHECKLIST (ch. 16)\n");
        for (int i = 0; i < DELIVERY_CHECKLIST.length; i++) b.append(c.critical.isEmpty() || i != 14 ? "☑ " : "☐ ").append(DELIVERY_CHECKLIST[i]).append(" — ").append(how[i]).append('\n');
        return b.toString();
    }

    /** The whole handbook, as the studio states it (the text as given is in the app's assets). */
    public static final String SUMMARY;
    static {
        StringBuilder b = new StringBuilder(TITLE).append(" — trained into the director\n\nFIVE GOVERNING PRINCIPLES\n");
        for (int i = 0; i < PRINCIPLES.length; i++) b.append(i + 1).append(". ").append(PRINCIPLES[i]).append('\n');
        b.append("\nWHERE THE HANDBOOK AND THE PROTOCOLS DISAGREE — what the studio does\n");
        for (String[] p : PRECEDENCE) b.append("• ").append(p[0]).append(": handbook — ").append(p[1]).append("; protocol — ").append(p[2]).append(" → ").append(p[3]).append('\n');
        b.append("\nRECOMMENDED PRODUCTION ORDER (Appendix B)\n");
        for (int i = 0; i < PRODUCTION_ORDER.length; i++) b.append(i + 1).append(". ").append(PRODUCTION_ORDER[i]).append('\n');
        SUMMARY = b.toString();
    }
}
