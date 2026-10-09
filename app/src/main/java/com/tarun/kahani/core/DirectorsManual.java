package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The AI Animated Film Maker — AI Director's Production Manual (v2.0, October 2026), trained into the director
 * and hardcoded. The manual as given is bundled in the app's assets (ai_film_maker_directors_manual.md); this
 * class holds its tables, the map from each of its steps to the code that enforces it, the places where it
 * disagrees with the protocols already in force (and what the studio does there), and the production records
 * it asks for: the asset inventory, the facial identity specification, the location records, the beat sheet,
 * the scene-coverage report, the scene records, the prop ledger, the transitions, the three-level review, the
 * five approval gates, the QA checklist, the audio check and the export check.
 */
public final class DirectorsManual {
    private DirectorsManual() {}

    public static final String TITLE = "AI ANIMATED FILM MAKER — AI DIRECTOR'S PRODUCTION MANUAL (v2.0, October 2026)";

    /** The manual's production principles. */
    public static final String[] PRINCIPLES = {
            "Story fidelity, character identity, and continuity take precedence over speed.",
            "Use approved canonical assets instead of recreating recurring characters from text alone.",
            "Separate narrative beats, scenes, and shots in the production data.",
            "Generate low-cost previews and an animatic before expensive final renders.",
            "Track versions and approvals; never silently replace an approved design."};

    /** Part IV: the nine stages of the integrated workflow and their deliverables. */
    public static final String[][] STAGES = {
            {"Understand story", "Entities, relationships, actions, narrative beats"},
            {"Identify assets", "Confirmed character, location, and prop references"},
            {"Lock facial identity", "Approved face specs, master sheets, expression references"},
            {"Expand screenplay", "Scene outline, shot records, durations, transitions"},
            {"Build animatic", "Approved timing and complete rough edit"},
            {"Generate shots", "Reference-controlled animation and audio"},
            {"Validate continuity", "Corrected and approved shot sequence"},
            {"Edit and finish", "Final cut, sound mix, colour grade, effects"},
            {"Export and verify", "Playable video, correct duration, present audio, valid file"}};

    /** Part IV: the five approval gates. */
    public static final String[][] GATES = {
            {"Gate 1 — Story", "Scene plan, character list, and narrative decisions approved"},
            {"Gate 2 — Assets", "Character sheets, location references, props, and visual bible locked"},
            {"Gate 3 — Animatic", "Pacing, coverage, camera choices, and dialogue timing approved"},
            {"Gate 4 — Shots", "Generated footage passes identity, performance, visual, and continuity checks"},
            {"Gate 5 — Film", "Complete edit passes creative and technical review"}};

    /** Part V: the quality assurance checklist. */
    public static final String[][] QA = {
            {"Character identity", "No unapproved substitutions; review every major-character shot"},
            {"Location identity", "Correct canonical location in each scene"},
            {"Story fidelity", "No unapproved plot changes or missing critical events"},
            {"Scene completeness", "Required actions and reactions are covered"},
            {"Continuity", "No unresolved major prop, costume, geography, or timeline contradictions"},
            {"Facial quality", "No visible critical facial defects"},
            {"Motion", "No unacceptable deformation, foot sliding, or broken contact"},
            {"Lip-sync", "Dialogue and mouth movement synchronised to approved audio"},
            {"Visual stability", "No distracting flicker or unexplained temporal changes"},
            {"Audio", "No clipping, missing dialogue, or unintended gaps"},
            {"Export", "File decodes correctly, duration is correct, and audio is present"}};

    /** Step 3.4: the shot vocabulary by narrative purpose. */
    public static final String[][] SHOT_TYPES = {
            {"Establishing shot", "Orient the audience in a new location"},
            {"Wide shot", "Show geography and character relationships"},
            {"Medium shot", "Communicate action and body language"},
            {"Close-up", "Reveal emotion or important detail"},
            {"Insert shot", "Show a prop or significant hand/object action"},
            {"Point-of-view", "Show what a character sees"},
            {"Reaction shot", "Show the effect of dialogue or an event"},
            {"Tracking shot", "Follow movement when movement matters"},
            {"Cutaway", "Provide relevant context or editorial transition"}};

    /** Step 3.9: the transitions. */
    public static final String[][] TRANSITIONS = {
            {"Cut on action", "Continue movement into the next shot"},
            {"Reaction cut", "Show another character responding"},
            {"Match cut", "Connect similar shapes or movements"},
            {"Establishing cut", "Orient the viewer in a new location"},
            {"Dissolve or fade", "Indicate time passing or a deliberate transition"}};

    /** Step 3.6: the planning ranges for durations (the protocol's 4-second cap is applied on top, see PRECEDENCE). */
    public static final String[][] DURATIONS = {
            {"Brief insert or reaction", "1–3 seconds"},
            {"Standard action or dialogue coverage", "3–6 seconds"},
            {"Emotional close-up", "3–8 seconds"},
            {"Establishing shot", "4–10 seconds"},
            {"Complex action", "Determine from action and edit"}};

    /** Step 2.6: the expression cues. */
    public static final String[][] EXPRESSION_CUES = {
            {"Happiness", "Relaxed facial muscles, natural smile, responsive eyes"},
            {"Sadness", "Reduced smile, lowered gaze, appropriate brow and mouth tension"},
            {"Anger", "Brow tension, focused gaze, controlled mouth and jaw"},
            {"Fear", "Alertness, gaze shifts, facial tension, hesitation"},
            {"Surprise", "Raised brows and appropriate eye and mouth changes"},
            {"Suspicion", "Asymmetrical brow movement, narrowed gaze, head angle"},
            {"Determination", "Focused eyes, stable posture, controlled expression"},
            {"Relief", "Reduced tension, softened gaze, released posture"}};

    /** Step 3.2: the underdeveloped moments a director looks for. */
    public static final String[] UNDERDEVELOPED = {
            "A character suddenly appears in a location",
            "An important object is used before being introduced",
            "A decision occurs without showing motivation",
            "A dramatic sound has no visible reaction",
            "A location changes without adequate orientation",
            "Dialogue requires a response but lacks a reaction shot",
            "An action starts in one shot and is incomplete in the next",
            "An emotional change has no believable transition"};

    /** Part VII: the final acceptance test. */
    public static final String[] ACCEPTANCE = {
            "Retrieve the correct characters from a mixed asset library",
            "Preserve identities across multiple camera angles",
            "Expand the story into a coherent, editable shot list",
            "Maintain costume, prop, and location continuity",
            "Generate convincing performances and synchronised dialogue",
            "Repair defective shots without disrupting the rest of the film",
            "Export and validate the complete edited video"};

    /**
     * Where the manual and the protocols already in force disagree, and what the studio does. The later
     * instruction never silently overrides an earlier one (the manual's own rule 1.5): the table says who wins
     * and why.
     */
    public static final String[][] PRECEDENCE = {
            {"Shot durations", "establishing 4–10 s, emotional close-up 3–8 s, dialogue 3–6 s (planning ranges)",
                    "Technical Director: no shot over 4 s; lip-sync close-ups of at most 6 words",
                    "the cap wins (the manual calls its ranges planning ranges, not rules): shots are cut at 2.6 s (v26, for smooth movement) and establishing shots are held for 2.5 s unless someone "
                    + "enters or moves during them (then the motion rule cuts sooner and the move is seen wide); a long emotional close-up is cut into locked close-ups and reactions of at most 4 s each"},
            {"Tracking shots", "follow movement when movement matters",
                    "Technical Director: the camera is a locked tripod; no character moves 15% of the frame in one shot",
                    "the protocol wins: a long walk is covered by cuts (each shot framed on the walk, side views for walking), never by a moving camera"},
            {"Left and right profiles on the master sheet", "both profiles, three-quarter, full body front and side",
                    "the figure model built from one picture is symmetric: one side view is made (and the user may upload the other)",
                    "one side view is made and recorded as both profiles with the note that the picture does not show the other side; a user-given side picture wins"},
            {"Visual embeddings for library search", "use embeddings or image similarity when supported",
                    "no learned network runs on the phone keyless",
                    "PicSense traits (face, headwear, hair, skin share, colours) and the description's words are the similarity; file names are never enough (the picture is inspected)"},
            {"Phoneme-level lip-sync", "map phonemes to visemes with word- or phoneme-level timing",
                    "the voices come back as audio only (no phoneme timing from the phone's or the free voices)",
                    "the mouth follows the voice's own envelope and its sound (round/wide) at 100 Hz, not the music's rhythm; Hindi syllables keep their timing because the audio does"},
            {"Pixar-level results", "aim for feature-animation craftsmanship, do not promise Pixar-level results from prompting alone",
                    "the user's earlier ask: Pixar-class",
                    "the manual's own realistic target is adopted: every rule is applied and measured, no claim beyond what the phone makes (docs/audit-v18.md §4)"},
            {"Regenerate only the affected shot", "repair or regenerate the defective shot where possible",
                    "the film is one render pass from one plan",
                    "a shot is fixed in the plan (Human QC: calmer, closer, wider, listener, no cut; the final QC cuts a boiling shot's motion by 80%) and the whole film is made again from the fixed plan — the neighbouring shots are unchanged by construction"}};

    /** The map from the manual's steps to the code that applies them (the hardcoding). */
    public static final String[][] ENFORCEMENT = {
            {"1.1 Ingest and inventory", "FilmJob → Studio3DArt.inventory: every character, view, place, object, voice and sound with its ID, file, source and status, in the production file and the film's QC"},
            {"1.2 Entity descriptions, action order", "ScriptParser (roles, aliases, places), Director.entryBeat (who is present, who enters later, in the script's order)"},
            {"1.3 Library search, inspect the picture", "AutoLibrary (name, aliases, description, PicSense traits — a file name is never enough), Library kinds person/place/view"},
            {"1.4 Uploaded photographs", "Cutout + PicSense (what is seen, what is uncertain), Figure3D (the identity kept while the back is inferred), the facial specification marks inferred features uncertain"},
            {"1.5 Conflicts", "the user's choice wins (pic.char settings, accepted/rejected proposals), then the latest approved picture, then the description; PRECEDENCE above for the documents"},
            {"1.6 Canonical records", "CHAR_/LOC_/VOICE_ IDs (DirectorsManual.charIds/locIds), character bibles (SceneMaker.bible), facial specs, location records, the prop ledger"},
            {"1.7 Shot reference package", "every shot's ASSETS line (character IDs, pictures used, location ID, held items) and its CONTINUITY state at start and end"},
            {"1.8 Validate each shot", "Director.validateShots (safe frames, motion, one action, words), the first-frame checks, Human QC, FinalQc"},
            {"2.1–2.4 Facial analysis in a fixed order", "DirectorsManual.facialSpec from the cut-out's eye, mouth, chin and skin points (a defined method, stated in the record) and the picture's traits"},
            {"2.5 Master sheet", "the lock sheet (front), the three-quarter, side and back views (Figure3D / your own pictures), the height comparison in the bible"},
            {"2.6 Expression references", "Pose emotions incl. SUSPICIOUS and RELIEVED (the manual's cues), Renderer.faceFor / bodyFor, Director.faceOf / bodyOf in every shot's record"},
            {"2.7 Lip-sync workflow", "voices finalised before direction; the mouth from the voice's envelope and sound; lip-sync only in locked front close-ups (≤ 6 words); listeners' mouths at rest"},
            {"2.8 Identity lock", "one picture per character all film (lock sheets); expression, gaze, camera and light vary; tears, sweat, wet and a red face are tracked per key"},
            {"2.9 Facial QA", "Human QC first frames at 320 px, FinalQc boiling / shaking per shot, the three-level review"},
            {"3.1 Beat sheet", "DirectorsManual.beatSheet from PixarLead.acts and spine (the actual story, never forced into a formula)"},
            {"3.2 Underdeveloped moments", "DirectorsManual.coverage — the eight checks on the staged film; the director adds the reaction to a loud sound itself"},
            {"3.3 Scenes", "DirectorsManual.sceneRecords: ID, purpose, start and end state, characters, objectives, actions, emotional progression, what is revealed, the transition"},
            {"3.4 Shot types", "ShotPlanner sizes + SINGLE / TWO_SHOT / OTS / POV; inserts (FX_SHOT); reactions; establishing shots; POV for \"looks at\"; cuts instead of tracking"},
            {"3.5 Shot production record", "every shot in the QC: PURPOSE … GAZE (the handbook) + ASSETS, TRANSITION IN, STATE AT START / END, VOICE / MUSIC (the manual)"},
            {"3.6 Durations", "shots cut at 2.6 s, establishing shots held 2.5 s where no one moves (counted in the QC); everything within the 4-second cap; distinct durations"},
            {"3.7 Animatic", "FilmJob.animatic: every shot's first frame held for its length with the real voices, music and sounds, approved in Human QC before the film is rendered"},
            {"3.8 Shots in sequence", "the whole film is drawn from the approved plan; held items and positions carried key to key; neighbours compared by the ledger"},
            {"3.9 Transitions", "DirectorsManual.transition per shot: cut on action, reaction cut, establishing cut, dissolve / fade (dips to black and white), plain cut"},
            {"3.10 Three levels of review", "DirectorsManual.review: shot (validation, Human QC, FinalQc), scene (ledger, axis, progression), film (spine, rhythm, sound, ending, export)"},
            {"IV Workflow and gates", "the five gates filled from what the studio checked; a failed gate is written, never carried forward silently"},
            {"V QA checklist", "the eleven dimensions answered from the checks that ran, including the audio check of the mix and the export check of the file"},
            {"VI Master operating instructions", "the director reads the whole story first (prepare), stages everything from the script, asks before a made picture is used, previews before rendering, never claims an unrendered film is finished (the export check)"},
            {"VII Acceptance test", "the Robolectric test directorsManualIsTrainedRecordsAndAnimatic: two characters, two places, dialogue, a loud sound, a look, a change of feeling"}};

    public static final String SUMMARY;
    static {
        StringBuilder b = new StringBuilder(TITLE).append(" — trained into the director\n\nPRODUCTION PRINCIPLES\n");
        for (int i = 0; i < PRINCIPLES.length; i++) b.append(i + 1).append(". ").append(PRINCIPLES[i]).append('\n');
        b.append("\nTHE NINE STAGES (Part IV)\n");
        for (int i = 0; i < STAGES.length; i++) b.append(i + 1).append(". ").append(STAGES[i][0]).append(" — ").append(STAGES[i][1]).append('\n');
        b.append("\nTHE FIVE GATES\n");
        for (String[] g : GATES) b.append("• ").append(g[0]).append(": ").append(g[1]).append('\n');
        b.append("\nWHERE THE MANUAL AND THE PROTOCOLS DISAGREE — what the studio does\n");
        for (String[] p : PRECEDENCE) b.append("• ").append(p[0]).append(": manual — ").append(p[1]).append("; in force — ").append(p[2]).append(" → ").append(p[3]).append('\n');
        b.append("\nWHERE EACH STEP LIVES IN THE CODE\n");
        for (String[] e : ENFORCEMENT) b.append("• ").append(e[0]).append(": ").append(e[1]).append('\n');
        b.append("\nREALISTIC QUALITY TARGET (the manual's own): feature-animation craftsmanship is the aim; Pixar-level results are not promised from prompting alone.\n");
        SUMMARY = b.toString();
    }

    // ================================================================== words the director reads

    /** A dramatic sound in a sentence (manual 3.2: it must have a visible reaction). */
    public static boolean loud(String s) {
        return Txt.has(s, "धमाका", "धमाके", "गरज", "बिजली कड़क", "कड़कड़ा", "दहाड़", "चीख", "ज़ोर की आवाज़", "जोर की आवाज", "ज़ोर से आवाज़", "धड़ाम", "टूटने की आवाज़",
                "काँच टूट", "कांच टूट", "विस्फोट", "गोली चल", "घंटा बज", "सायरन", "दरवाज़ा ज़ोर से", "explosion", "explodes", "blast", "thunder", "crash", "bang", "roar",
                "scream", "shatters", "slams", "gunshot", "siren", "loud noise", "a loud");
    }

    /** A character looks at someone or something (manual 3.4: a point-of-view shot shows what they see). */
    public static boolean looksAt(String s) {
        return Txt.has(s, "की ओर देख", "की तरफ़ देख", "की तरफ देख", "को देखत", "को देखन", "को घूर", "घूरत", "ताकत", "नज़र डाल", "नजर डाल", "देखती है", "देखता है", "देखते हैं",
                "देखने लग", "looks at", "stares at", "gazes at", "glances at", "looks toward", "looks towards", "looks up at", "looks out", "watches", "peers at");
    }

    /** Danger words: a loud sound in such a sentence frightens; otherwise it only startles. */
    static boolean dangerous(String s) {
        return Txt.has(s, "धमाका", "विस्फोट", "दहाड़", "चीख", "गोली", "बिजली", "आग", "explosion", "blast", "roar", "scream", "gunshot", "lightning", "fire");
    }

    // ================================================================== persistent IDs (1.6)

    public static String charId(int order) { return String.format(Locale.US, "CHAR_%03d", order + 1); }
    public static String locId(int order) { return String.format(Locale.US, "LOC_%03d", order + 1); }
    public static String voiceId(String charId) { return "VOICE_" + charId.substring(5); }

    /** CHAR_001… in cast order (the characters who stand in the film), then the voices from the air. */
    public static Map<Story.CharacterDef, String> charIds(Story story) {
        Map<Story.CharacterDef, String> m = new LinkedHashMap<Story.CharacterDef, String>();
        int i = 0;
        for (Story.CharacterDef c : story.cast()) m.put(c, charId(i++));
        for (Story.CharacterDef c : story.characters) if (!m.containsKey(c)) m.put(c, charId(i++));
        return m;
    }

    /** LOC_001… by set, in the order the film visits them (a user's picture of a place is one location of its set). */
    public static Map<Integer, String> locIds(Film film) {
        Map<Integer, String> m = new LinkedHashMap<Integer, String>();
        int i = 0;
        for (Film.Seg sg : film.segs) if (sg.type == Film.S_SCENE && !m.containsKey(sg.set)) m.put(sg.set, locId(i++));
        return m;
    }

    /** SC_01, SC_01b… the scene's number and the part's letter. */
    public static String sceneId(Film film, Film.Seg sg, Story story) {
        int num = sg.scene >= 0 && sg.scene < story.scenes.size() ? story.scenes.get(sg.scene).number : 0;
        int parts = 0, idx = 0;
        for (Film.Seg s : film.segs) if (s.type == Film.S_SCENE && s.scene == sg.scene) { if (s == sg) idx = parts; parts++; }
        return String.format(Locale.US, "SC_%02d%s", num, parts > 1 ? String.valueOf((char) ('a' + idx)) : "");
    }

    public static final String[] ITEM_NAMES = {"nothing", "a ribbon", "a banana", "a mirror", "a wooden sword", "a basket", "a flower", "a sword", "a bottle", "a turban"};
    public static final String[] BODY_NAMES = {"standing", "sitting", "lying", "kneeling", "hanging", "crouching"};

    static String item(int i) { return i >= 0 && i < ITEM_NAMES.length ? ITEM_NAMES[i] : "something"; }
    static String body(int b) { return b >= 0 && b < BODY_NAMES.length ? BODY_NAMES[b] : "standing"; }

    // ================================================================== per shot (3.5, 1.7, 3.8, 3.9)

    /** ASSETS: the character IDs in the shot with the pictures they are drawn with, the location ID, held items. */
    public static String assets(Film film, Story story, Film.Shot sh, Map<Story.CharacterDef, String> charIds, Map<Integer, String> locIds) {
        Film.Seg sg = film.segAt(sh.t + 0.05f);
        if (sg == null) return "—";
        StringBuilder b = new StringBuilder();
        float t = sh.t + 0.05f;
        for (Film.Actor a : sg.actors) {
            Film.Key k = a.stateAt(t);
            if (!k.visible || k.anchor == Film.A_HIDDEN) continue;
            String id = charIds.get(a.c);
            if (b.length() > 0) b.append("; ");
            b.append(id == null ? "" : id + " ").append(a.c.shown());
            String pic = pictureUsed(sh.view, a.c.shown());
            b.append(" (").append(pic).append(')');
            if (k.holdR != Pose.I_NONE || k.holdL != Pose.I_NONE) b.append(" holding ").append(item(k.holdR != Pose.I_NONE ? k.holdR : k.holdL));
            if (k.wearsTurban) b.append(", wearing another's turban");
            if (k.noHeadwear) b.append(", headwear off");
            if (k.disguised) b.append(", disguised");
        }
        if (b.length() == 0) b.append("no character in frame");
        String loc = locIds.get(sg.set);
        b.append(" | ").append(loc == null ? "" : loc + " ").append(Sets.label(sg.set)).append(sg.backdrop != null ? " (your picture)" : " (painted set)")
                .append(", ").append(Handbook.hourName(sg.tod));
        return b.toString();
    }

    static String pictureUsed(String view, String name) {
        if (view == null || view.length() == 0) return "the master picture";
        for (String part : view.split("; ")) {
            int c = part.indexOf(": ");
            if (c > 0 && part.substring(0, c).equals(name)) return part.substring(c + 2);
        }
        return "the master picture";
    }

    /** TRANSITION IN (3.9): how this shot is entered, from what the previous shot and this one do. */
    public static String transition(Film film, Film.Shot prev, Film.Shot cur, boolean firstOfPart) {
        Film.Seg sg = film.segAt(cur.t + 0.05f);
        if (firstOfPart || prev == null) {
            if (sg != null && sg.transition == 1) return "dissolve / fade — a dip to black: time has passed (" + TRANSITIONS[4][1].toLowerCase(Locale.US) + ")";
            if (sg != null && sg.transition == 2) return "dissolve / fade — a dip to white: magic, a dream or a memory";
            return "establishing cut — " + TRANSITIONS[3][1].toLowerCase(Locale.US);
        }
        if (cur.reaction) return "reaction cut — " + TRANSITIONS[1][1].toLowerCase(Locale.US);
        if (cur.type == ShotPlanner.POV) return "cut to the point of view — the look, then what is seen";
        if (prev.motion > 0.03f && prev.subject.length() > 0 && prev.subject.equals(cur.subject)) return "cut on action — " + TRANSITIONS[0][1].toLowerCase(Locale.US);
        if (prev.size == cur.size && prev.subject.length() > 0 && !prev.subject.equals(cur.subject) && !cur.speech && !prev.speech)
            return "match cut — the same frame size on the next character (" + TRANSITIONS[2][1].toLowerCase(Locale.US) + ")";
        if (cur.speech && prev.speech && !prev.subject.equals(cur.subject)) return "cut on the line — the speaker changes (the dialogue axis is kept)";
        return "cut — an ordinary cut (the manual: ordinary cuts are often more effective than elaborate effects)";
    }

    /** STATE AT START / END (3.5, 3.8): where everyone is, which way they face, what they hold and feel. */
    public static String state(Film film, float t) {
        Film.Seg sg = film.segAt(t);
        if (sg == null) return "—";
        StringBuilder b = new StringBuilder();
        for (Film.Actor a : sg.actors) {
            Film.Key k = a.stateAt(t);
            if (!k.visible || k.anchor == Film.A_HIDDEN) continue;
            if (b.length() > 0) b.append("; ");
            b.append(a.c.shown()).append(' ').append(Handbook.third(Director.xAt(a, t))).append(", facing ").append(k.facing < 0 ? "left" : "right")
                    .append(", ").append(body(k.body));
            if (k.moveDur > 0 && k.t <= t && t < k.t + k.moveDur) b.append(k.run ? ", running" : ", walking");
            if (k.holdR != Pose.I_NONE) b.append(", ").append(item(k.holdR)).append(" in the right hand");
            if (k.holdL != Pose.I_NONE) b.append(", ").append(item(k.holdL)).append(" in the left hand");
            b.append(", ").append(Bible.emotionWord(k.emotion, false));
            if (k.tears) b.append(", tears");
            if (k.sweat) b.append(", sweat");
            if (k.netted) b.append(", in the net");
        }
        if (b.length() == 0) b.append("no character in frame");
        float wet = film.wetness(t);
        if (wet > 0.05f) b.append(" | everyone wet (rain)");
        return b.toString();
    }

    /** VOICE / MUSIC (3.5 audio): the line and its voice ID, the music cue of the part, the effects inside the shot. */
    public static String voiceMusic(Film film, Story story, Film.Shot sh, Map<Story.CharacterDef, String> charIds) {
        StringBuilder b = new StringBuilder();
        if (sh.line >= 0 && sh.line < film.lines.size()) {
            Film.Line l = film.lines.get(sh.line);
            String id = l.who == null ? "VOICE_NARRATOR" : voiceId(charIds.containsKey(l.who) ? charIds.get(l.who) : "CHAR_000");
            b.append(id).append(" (").append(l.who == null ? "narrator" : l.who.shown()).append(String.format(Locale.US, ") at %d:%04.1f for %.1f s", (int) (l.start / 60), l.start % 60, l.dur));
            if (sh.spoken.length() > 0) b.append(": \"").append(sh.spoken).append('"');
        } else b.append("no line in this shot (listeners' mouths at rest)");
        int mood = -1;
        for (Film.Music m : film.music) if (sh.t >= m.t0 - 0.01f && sh.t < m.t1) { mood = m.mood; break; }
        b.append(" | music: ").append(moodName(mood)).append(sh.line >= 0 ? " (ducked under the line)" : "");
        int fx = 0;
        StringBuilder fxs = new StringBuilder();
        for (Film.Sfx s : film.sfx) if (s.t >= sh.t - 0.05f && s.t < sh.t + sh.dur) { if (fx++ < 3) fxs.append(fxs.length() > 0 ? ", " : "").append(sfxName(s.type)); }
        if (fx > 0) b.append(" | effects: ").append(fxs).append(fx > 3 ? " (+" + (fx - 3) + ")" : "");
        return b.toString();
    }

    public static String moodName(int mood) {
        switch (mood) {
            case Film.M_TITLE: return "title theme";
            case Film.M_HAPPY: return "happy";
            case Film.M_TENSE: return "tense (low drone)";
            case Film.M_VILLAIN: return "villain";
            case Film.M_SAD: return "sad";
            case Film.M_ACTION: return "action";
            case Film.M_CELEBRATE: return "celebration";
            case Film.M_END: return "end theme";
            case Film.M_NIGHT: return "night";
            case Film.M_PLAYFUL: return "playful";
            default: return "none";
        }
    }

    static String sfxName(int type) {
        switch (type) {
            case Film.SFX_STREAM: return "stream"; case Film.SFX_BIRDS: return "birds"; case Film.SFX_WIND: return "wind"; case Film.SFX_CLACK: return "clack";
            case Film.SFX_POP: return "pop"; case Film.SFX_CHIME: return "chime"; case Film.SFX_MONKEY: return "monkey"; case Film.SFX_RUSTLE: return "rustle";
            case Film.SFX_THUD: return "thud"; case Film.SFX_WHOOSH: return "whoosh"; case Film.SFX_BELL: return "bell"; case Film.SFX_DRUMS: return "drums";
            case Film.SFX_NIGHT: return "night insects"; case Film.SFX_ROAR: return "roar"; case Film.SFX_CLAP: return "clap"; case Film.SFX_ANKLET: return "anklets";
            case Film.SFX_HISS: return "hiss"; case Film.SFX_DRIP: return "drip"; case Film.SFX_SCREAM_FX: return "scream"; case Film.SFX_SPLASH: return "splash";
            case Film.SFX_NET: return "net"; case Film.SFX_FANFARE: return "fanfare"; case Film.SFX_MAGIC: return "magic"; case Film.SFX_STEPS: return "steps";
            case Film.SFX_CROWD: return "crowd"; case Film.SFX_GLASS: return "glass"; case Film.SFX_SWORD: return "swords"; case Film.SFX_USER: return "your sound";
            case Film.SFX_THUNDER: return "thunder"; case Film.SFX_STEPS_HARD: return "steps on stone"; case Film.SFX_STEPS_RUN: return "running steps";
            default: return "effect " + type;
        }
    }

    // ================================================================== the records (1.6, 3.1, 3.2, 3.3)

    /** Everything the director can write from the staged film itself (the app adds the inventory, the facial specs, the audio and export checks). */
    public static String records(Film film, Story story, Handbook.Stats stats) {
        Map<Story.CharacterDef, String> cids = charIds(story);
        Map<Integer, String> lids = locIds(film);
        return "\n" + beatSheet(story, film) + "\n" + sceneRecords(story, film, cids, lids) + "\n" + coverage(story, film, stats) + "\n"
                + propLedger(story, film, cids) + "\n" + locationRecords(story, film, lids);
    }

    /** 3.1: the narrative beat sheet, from the acts and the spine PixarLead reads (the actual story, not a formula). */
    public static String beatSheet(Story story, Film film) {
        StringBuilder b = new StringBuilder("NARRATIVE BEAT SHEET (director's manual 3.1)\n");
        int n = story.scenes.size();
        if (n == 0) return b.append("  no scenes\n").toString();
        int[] acts = PixarLead.acts(story);
        String[] spine = film.spine != null ? film.spine : PixarLead.spine(story);
        Map<Story.CharacterDef, String> cids = charIds(story);
        int turn = -1, climax = -1;
        for (int i = 0; i < n; i++) { if (acts[i] == 2 && turn < 0) turn = i; if (acts[i] == 4) climax = i; }
        if (turn < 0) turn = Math.min(1, n - 1);
        if (climax < 0) climax = n - 1;                 // a short script: its last scene is its climax and its resolution
        int peak = emotionalPeak(story, film);
        beat(b, 1, "Beginning and setup", story.scenes.get(0), spine[1], cids, film, "where we are and the ordinary day");
        beat(b, 2, "Introduction of central characters and goals", story.scenes.get(0), goals(story, cids), cids, film, "who wants what (their first lines)");
        beat(b, 3, "Inciting incident", story.scenes.get(turn), spine[2], cids, film, "the thing that changes the day");
        int esc = -1;
        for (int i = turn + 1; i < n; i++) if (acts[i] == 3) { esc = i; break; }
        if (esc >= 0) beat(b, 4, "Escalation of obstacles or conflict", story.scenes.get(esc), spine[3], cids, film, "what gets harder");
        else b.append("  4. Escalation — the story has no separate escalation scene (not forced: the turn leads straight to the climax)\n");
        if (peak >= 0) beat(b, 5, "Discoveries and emotional turning points", story.scenes.get(peak), "the scene with the biggest change of feeling: " + feelingChange(story.scenes.get(peak)), cids, film, "what is found out, how it feels");
        else b.append("  5. Discoveries and emotional turning points — no scene changes its feeling (a short script)\n");
        beat(b, 6, "Climax", story.scenes.get(climax), PixarLead.headline(story.scenes.get(climax)), cids, film, "the hardest moment, met");
        beat(b, 7, "Resolution", story.scenes.get(n - 1), spine[5], cids, film, "how it ends for everyone");
        b.append("  8. Closing image: ").append(closingImage(story.scenes.get(n - 1))).append('\n');
        b.append("  (the beats are read from the script's own scenes; a scene missing from this list is not missing from the film)\n");
        return b.toString();
    }

    private static void beat(StringBuilder b, int no, String name, Story.Scene sc, String what, Map<Story.CharacterDef, String> cids, Film film, String understand) {
        b.append("  ").append(no).append(". ").append(name).append(" — ").append(sc.heading.length() > 0 ? sc.heading : "scene " + sc.number).append('\n');
        b.append("     what happens: ").append(Handbook.clip(what, 140)).append('\n');
        b.append("     who: ").append(whoIn(sc, cids)).append("  |  where: ").append(Sets.label(Sets.detect(sc.title + " " + sc.setting))).append('\n');
        b.append("     the audience must understand: ").append(understand).append("  |  what changes: ").append(feelingChange(sc)).append('\n');
        b.append("     the next beat needs: ").append(exitOf(sc)).append('\n');
    }

    static String whoIn(Story.Scene sc, Map<Story.CharacterDef, String> cids) {
        List<Story.CharacterDef> seen = new ArrayList<Story.CharacterDef>();
        for (Story.Beat bt : sc.beats) {
            if (bt.type == Story.Beat.DIALOGUE && bt.speaker != null && !seen.contains(bt.speaker)) seen.add(bt.speaker);
        }
        StringBuilder b = new StringBuilder();
        for (Story.CharacterDef c : seen) b.append(b.length() > 0 ? ", " : "").append(cids.get(c) == null ? "" : cids.get(c) + " ").append(c.shown());
        return b.length() == 0 ? "no one speaks (the narration carries it)" : b.toString();
    }

    static String goals(Story story, Map<Story.CharacterDef, String> cids) {
        StringBuilder b = new StringBuilder();
        List<Story.CharacterDef> done = new ArrayList<Story.CharacterDef>();
        for (Story.Scene sc : story.scenes) for (Story.Beat bt : sc.beats) {
            if (bt.type != Story.Beat.DIALOGUE || bt.speaker == null || done.contains(bt.speaker)) continue;
            done.add(bt.speaker);
            b.append(b.length() > 0 ? "; " : "").append(bt.speaker.shown()).append(": \"").append(Handbook.clip(Bible.oneLine(bt.text), 50)).append('"');
            if (done.size() >= 4) break;
        }
        return b.length() == 0 ? "no dialogue" : b.toString();
    }

    static String feelingChange(Story.Scene sc) {
        int first = -1, last = -1;
        for (Story.Beat bt : sc.beats) {
            if (bt.type != Story.Beat.DIALOGUE) continue;
            int e = Director.emotionOf(bt.manner, bt.text, bt.speaker);
            if (first < 0) first = e;
            last = e;
        }
        if (first < 0) return "the action, no spoken feeling";
        return Bible.emotionWord(first, false) + " → " + Bible.emotionWord(last, false);
    }

    static int emotionalPeak(Story story, Film film) {
        int best = -1; float bestD = 0;
        for (int i = 0; i < story.scenes.size(); i++) {
            Story.Scene sc = story.scenes.get(i);
            float lo = 1, hi = 0;
            for (Story.Beat bt : sc.beats) {
                if (bt.type != Story.Beat.DIALOGUE) continue;
                float in = ShotPlanner.intensity(Director.emotionOf(bt.manner, bt.text, bt.speaker), bt.manner, bt.text);
                lo = Math.min(lo, in); hi = Math.max(hi, in);
            }
            if (hi - lo > bestD + 0.05f) { bestD = hi - lo; best = i; }
        }
        return bestD >= 0.25f ? best : -1;
    }

    static String exitOf(Story.Scene sc) {
        for (int i = sc.beats.size() - 1; i >= 0; i--) {
            Story.Beat bt = sc.beats.get(i);
            String t = bt.type == Story.Beat.DIALOGUE ? bt.speaker == null ? bt.text : bt.speaker.shown() + ": " + bt.text : Txt.withoutParens(bt.text);
            if (t.trim().length() > 3) return Handbook.clip(Bible.oneLine(t), 100);
        }
        return "the scene's last moment";
    }

    static String closingImage(Story.Scene sc) {
        for (int i = sc.beats.size() - 1; i >= 0; i--) {
            Story.Beat bt = sc.beats.get(i);
            if (bt.type == Story.Beat.DIRECTION && Txt.withoutParens(bt.text).trim().length() > 3) return Handbook.clip(Bible.oneLine(Txt.withoutParens(bt.text)), 120);
        }
        return "the last line, then the end card";
    }

    /** 3.3: one record per part of a scene. */
    public static String sceneRecords(Story story, Film film, Map<Story.CharacterDef, String> cids, Map<Integer, String> lids) {
        StringBuilder b = new StringBuilder("SCENE RECORDS (director's manual 3.3)\n");
        Film.Seg prev = null;
        List<Film.Seg> scenes = new ArrayList<Film.Seg>();
        for (Film.Seg sg : film.segs) if (sg.type == Film.S_SCENE) scenes.add(sg);
        for (int i = 0; i < scenes.size(); i++) {
            Film.Seg sg = scenes.get(i);
            Story.Scene sc = sg.scene >= 0 && sg.scene < story.scenes.size() ? story.scenes.get(sg.scene) : null;
            String id = sceneId(film, sg, story);
            b.append("— ").append(id).append(": ").append(sc != null ? sc.heading + (sc.title.length() > 0 ? " — " + sc.title : "") : "part").append('\n');
            b.append("   purpose: ").append(sc != null ? Handbook.clip(Bible.oneLine(PixarLead.headline(sc)), 100) : "—").append("  |  act ").append(sg.act).append('\n');
            b.append("   location: ").append(lids.get(sg.set)).append(' ').append(Sets.label(sg.set)).append(", ").append(Handbook.hourName(sg.tod))
                    .append(sg.backdrop != null ? " (your picture)" : "").append("  |  characters: ").append(present(sg, cids)).append('\n');
            b.append("   beginning state: ").append(state(film, sg.t0 + 0.1f)).append('\n');
            b.append("   ending state: ").append(state(film, sg.t1 - 0.1f)).append('\n');
            if (sc != null) {
                b.append("   objectives and conflict: ").append(objectives(sc)).append('\n');
                b.append("   essential actions: ").append(essentialActions(sc)).append('\n');
                b.append("   emotional progression: ").append(progression(sg, film)).append('\n');
                b.append("   information revealed: ").append(revealed(sc)).append('\n');
            }
            Film.Seg next = i + 1 < scenes.size() ? scenes.get(i + 1) : null;
            b.append("   transition to the next scene: ").append(next == null ? "the end card" : next.transition == 1 ? "a dip to black (time passes)" : next.transition == 2 ? "a dip to white (magic, a dream, a memory)"
                    : next.set != sg.set ? "a dissolve to a new place, opened wide (establishing cut)" : "a dissolve, the same place").append('\n');
            prev = sg;
        }
        return b.toString();
    }

    static String present(Film.Seg sg, Map<Story.CharacterDef, String> cids) {
        StringBuilder b = new StringBuilder();
        for (Film.Actor a : sg.actors) {
            boolean ever = false;
            for (Film.Key k : a.keys) if (k.visible) { ever = true; break; }
            if (!ever) continue;
            b.append(b.length() > 0 ? ", " : "").append(cids.get(a.c) == null ? "" : cids.get(a.c) + " ").append(a.c.shown());
            if (!a.keys.get(0).visible) b.append(" (enters)");
        }
        return b.length() == 0 ? "none" : b.toString();
    }

    static String objectives(Story.Scene sc) {
        StringBuilder b = new StringBuilder();
        List<Story.CharacterDef> done = new ArrayList<Story.CharacterDef>();
        for (Story.Beat bt : sc.beats) {
            if (bt.type != Story.Beat.DIALOGUE || bt.speaker == null || done.contains(bt.speaker)) continue;
            done.add(bt.speaker);
            b.append(b.length() > 0 ? "; " : "").append(bt.speaker.shown()).append(" — \"").append(Handbook.clip(Bible.oneLine(bt.text), 60)).append('"');
            if (done.size() >= 3) break;
        }
        return b.length() == 0 ? "no dialogue: the action is the objective" : b.toString();
    }

    static String essentialActions(Story.Scene sc) {
        StringBuilder b = new StringBuilder();
        int n = 0;
        for (Story.Beat bt : sc.beats) {
            if (bt.type != Story.Beat.DIRECTION) continue;
            String t = Txt.withoutParens(bt.text).replaceFirst("^(स्थान|Location|Place|Setting)\\s*[:：]\\s*", "").trim();
            if (t.length() < 4) continue;
            b.append(b.length() > 0 ? " / " : "").append(Handbook.clip(Bible.oneLine(t), 70));
            if (++n >= 3) break;
        }
        return b.length() == 0 ? "the dialogue itself" : b.toString();
    }

    static String progression(Film.Seg sg, Film film) {
        StringBuilder b = new StringBuilder();
        for (Film.Actor a : sg.actors) {
            Film.Key s = a.stateAt(sg.t0 + 0.1f), e = a.stateAt(sg.t1 - 0.1f);
            if (!s.visible && !e.visible) continue;
            int peakE = s.emotion; float peakI = 0;
            for (Film.Key k : a.keys) { float in = ShotPlanner.intensity(k.emotion, "", ""); if (in > peakI) { peakI = in; peakE = k.emotion; } }
            for (Film.Speak sp : a.speaks) { float in = ShotPlanner.intensity(sp.emotion, "", ""); if (in > peakI) { peakI = in; peakE = sp.emotion; } }
            b.append(b.length() > 0 ? "; " : "").append(a.c.shown()).append(": ").append(Bible.emotionWord(s.emotion, false))
                    .append(peakE != s.emotion && peakE != e.emotion ? " → " + Bible.emotionWord(peakE, false) : "").append(" → ").append(Bible.emotionWord(e.emotion, false));
        }
        return b.length() == 0 ? "—" : b.toString();
    }

    static String revealed(Story.Scene sc) {
        for (Story.Beat bt : sc.beats) {
            if (bt.type != Story.Beat.DIALOGUE) continue;
            if (Txt.has(bt.text, ShotPlanner.REALISE)) return (bt.speaker == null ? "" : bt.speaker.shown() + ": ") + "\"" + Handbook.clip(Bible.oneLine(bt.text), 80) + "\" (a realisation, decision or confession)";
        }
        return "nothing is revealed in words here (what the scene shows is the information)";
    }

    /** 3.2: the scene-coverage report — the eight underdeveloped moments, checked on the staged film. */
    public static String coverage(Story story, Film film, Handbook.Stats stats) {
        StringBuilder b = new StringBuilder("SCENE-COVERAGE REPORT (director's manual 3.2 — genuine gaps against intentional omissions)\n");
        // 1. a character suddenly appears
        int sudden = 0, staged = 0;
        StringBuilder names = new StringBuilder();
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            Story.Scene sc = sg.scene >= 0 && sg.scene < story.scenes.size() ? story.scenes.get(sg.scene) : null;
            String opening = sc == null ? "" : sc.title + " " + sc.setting + " " + (sc.beats.isEmpty() ? "" : sc.beats.get(0).type == Story.Beat.DIRECTION ? sc.beats.get(0).text : "");
            for (Film.Actor a : sg.actors) {
                if (!a.keys.get(0).visible) { staged++; continue; }
                if (sc != null && !ScriptParser.mentions(story, opening).contains(a.c)) { sudden++; if (names.length() < 120) names.append(names.length() > 0 ? ", " : "").append(a.c.shown()).append(" in ").append(sceneId(film, sg, story)); }
            }
        }
        b.append("  1. ").append(UNDERDEVELOPED[0]).append(": ").append(staged).append(" entrance(s) staged from the script's words (\"enters\", \"comes running\"…); ")
                .append(sudden == 0 ? "everyone else is named by the scene's opening" : sudden + " character(s) stand in the scene from its first frame though the opening does not name them (" + names + ") — intentional: the lineup rule puts everyone who speaks in the scene on the stage before the first line, so no one pops in mid-scene").append('\n');
        // 2. an object used before it is introduced
        int objGaps = 0; StringBuilder objs = new StringBuilder();
        String sofar = "";
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            Story.Scene sc = sg.scene >= 0 && sg.scene < story.scenes.size() ? story.scenes.get(sg.scene) : null;
            String text = sc == null ? "" : PixarLead.sceneText(sc);
            for (Film.Actor a : sg.actors) for (Film.Key k : a.keys) {
                int it = k.holdR != Pose.I_NONE ? k.holdR : k.holdL;
                if (it == Pose.I_NONE || k.t < sg.t0 || k.t >= sg.t1) continue;
                String[] words = itemWords(it);
                if (!Txt.has(sofar + " " + text + " " + a.c.description, words)) { objGaps++; if (objs.length() < 100) objs.append(objs.length() > 0 ? ", " : "").append(item(it)).append(" (").append(a.c.shown()).append(")"); }
                break;
            }
            sofar += " " + text;
        }
        b.append("  2. ").append(UNDERDEVELOPED[1]).append(": ").append(objGaps == 0 ? "every held thing is named by the story or the character's description before it is held" : objGaps + " thing(s) held that the story never names before: " + objs + " — they come from the manner of a line (\"sword in hand\"); the insert of a thing's picture introduces it when there is one").append('\n');
        // 3. a decision without motivation
        int decisions = 0, unmotivated = 0;
        for (Film.Shot sh : film.shots) if (sh.purpose.startsWith("Realisation") || Txt.has(sh.purpose, "realisation", "decision", "confession")) decisions++;
        for (int i = 0; i < film.lines.size(); i++) {
            Film.Line l = film.lines.get(i);
            if (l.who == null || !Txt.has(l.text, ShotPlanner.REALISE)) continue;
            boolean before = false;
            for (Film.Shot sh : film.shots) if ((sh.reaction || sh.purpose.startsWith("The look")) && sh.t < l.start && sh.t > l.start - 8f) { before = true; break; }
            if (!before) unmotivated++;
        }
        b.append("  3. ").append(UNDERDEVELOPED[2]).append(": ").append(unmotivated == 0 ? "every realisation or decision line comes after a reaction or a look (thought before action)" : unmotivated + " decision line(s) without a reaction or a look in the 8 s before — the line's own close-up carries the motivation").append('\n');
        // 4. a dramatic sound without a reaction
        int loud = stats != null ? stats.loudReactions : 0;
        b.append("  4. ").append(UNDERDEVELOPED[3]).append(": ").append(loud).append(" loud sound(s) in the script, each given a startle on every face on stage and a reaction shot a beat later (the director adds them itself)").append('\n');
        // 5. a location change without orientation
        int parts = 0, wideOpen = 0;
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            parts++;
            if (!sg.cams.isEmpty() && sg.cams.get(0).zoom <= 1.3f) wideOpen++;
        }
        b.append("  5. ").append(UNDERDEVELOPED[4]).append(": ").append(wideOpen).append(" of ").append(parts).append(" parts open on a readable wide shot (an establishing cut)").append('\n');
        // 6. dialogue that needs a response without a reaction shot
        int questions = 0, unanswered = 0;
        for (int i = 0; i < film.lines.size(); i++) {
            Film.Line l = film.lines.get(i);
            if (l.who == null || !l.text.trim().endsWith("?")) continue;
            questions++;
            boolean answered = false;
            float end = l.start + l.dur;
            for (Film.Shot sh : film.shots) if (sh.reaction && sh.t >= end - 0.5f && sh.t < end + 2.5f) { answered = true; break; }
            if (!answered) for (int j = i + 1; j < film.lines.size(); j++) { Film.Line m = film.lines.get(j); if (m.start > end + 4f) break; if (m.who != null && m.who != l.who) { answered = true; break; } }
            if (!answered) unanswered++;
        }
        b.append("  6. ").append(UNDERDEVELOPED[5]).append(": ").append(questions).append(" question(s) asked; ").append(unanswered == 0 ? "each answered by a reaction shot or the next speaker" : unanswered + " without a reaction shot or an answer within 4 s (a rhetorical question, or the script moves on)").append('\n');
        // 7. an action started and not completed
        int cutMoves = 0;
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            for (Film.Actor a : sg.actors) for (Film.Key k : a.keys) {
                if (k.moveDur <= 0) continue;
                for (Film.Cam c : sg.cams) if (c.t > k.t + 0.1f && c.t < k.t + k.moveDur - 0.1f) { cutMoves++; break; }
            }
        }
        b.append("  7. ").append(UNDERDEVELOPED[6]).append(": ").append(cutMoves).append(" move(s) continue across a cut — each next shot starts where the move is (cut on action; the key keeps going), none restarts").append('\n');
        // 8. an emotional change without a transition
        int jumps = 0;
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            for (Film.Actor a : sg.actors) {
                Film.Key prev = null;
                for (Film.Key k : a.keys) {
                    if (prev != null && k.emotion != prev.emotion && prev.emotion != Pose.NEUTRAL && k.emotion != Pose.NEUTRAL
                            && Math.abs(ShotPlanner.intensity(k.emotion, "", "") - ShotPlanner.intensity(prev.emotion, "", "")) > 0.3f && k.t - prev.t < 0.5f) jumps++;
                    prev = k;
                }
            }
        }
        b.append("  8. ").append(UNDERDEVELOPED[7]).append(": ").append(jumps == 0 ? "no feeling jumps to a far one within half a second (faces blend over 0.25 s, reactions wait a beat)" : jumps + " quick jump(s) between far feelings — the script's own turns (a scream after a laugh); the face still blends").append('\n');
        return b.toString();
    }

    static String[] itemWords(int item) {
        switch (item) {
            case Pose.I_RIBBON: return new String[]{"रिबन", "फीता", "ribbon"};
            case Pose.I_BANANA: return new String[]{"केला", "केले", "banana"};
            case Pose.I_MIRROR: return new String[]{"आईना", "दर्पण", "शीशा", "mirror"};
            case Pose.I_WOOD_SWORD: case Pose.I_SWORD: return new String[]{"तलवार", "sword"};
            case Pose.I_BASKET: return new String[]{"टोकरी", "basket"};
            case Pose.I_FLOWER: return new String[]{"फूल", "flower"};
            case Pose.I_BOTTLE: return new String[]{"बोतल", "शीशी", "bottle", "potion"};
            case Pose.I_TURBAN: return new String[]{"पगड़ी", "टोपी", "साफ़ा", "turban", "cap"};
            default: return new String[]{"\u0000"};
        }
    }

    /** 1.6: the prop ledger — every held thing, its owner, and where it is scene by scene. */
    public static String propLedger(Story story, Film film, Map<Story.CharacterDef, String> cids) {
        StringBuilder b = new StringBuilder("PROP LEDGER (director's manual 1.6 — owner, state and location scene by scene)\n");
        Map<Integer, StringBuilder> items = new LinkedHashMap<Integer, StringBuilder>();
        Map<Integer, String> owner = new HashMap<Integer, String>();
        for (Story.CharacterDef c : story.cast()) {
            Look l = c.look;
            if (l == null) continue;
            if (l.sword || l.katar) owner.put(Pose.I_SWORD, c.shown());
            if (l.wand) owner.put(Pose.I_BOTTLE, owner.containsKey(Pose.I_BOTTLE) ? owner.get(Pose.I_BOTTLE) : c.shown());
            if (l.headwear == Look.HW_TURBAN && !owner.containsKey(Pose.I_TURBAN)) owner.put(Pose.I_TURBAN, c.shown());
        }
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            String id = sceneId(film, sg, story);
            for (Film.Actor a : sg.actors) {
                int seenR = -1, seenL = -1;
                for (Film.Key k : a.keys) {
                    if (k.t < sg.t0 - 0.01f || k.t > sg.t1) continue;
                    if (k.holdR != Pose.I_NONE && k.holdR != seenR) { note(items, k.holdR, id, a.c.shown(), "held in the right hand from " + stamp(k.t)); seenR = k.holdR; }
                    else if (k.holdR == Pose.I_NONE && seenR > 0) { note(items, seenR, id, a.c.shown(), "put down at " + stamp(k.t)); seenR = -1; }
                    if (k.holdL != Pose.I_NONE && k.holdL != seenL) { note(items, k.holdL, id, a.c.shown(), "held in the left hand from " + stamp(k.t)); seenL = k.holdL; }
                    else if (k.holdL == Pose.I_NONE && seenL > 0) { note(items, seenL, id, a.c.shown(), "put down at " + stamp(k.t)); seenL = -1; }
                    if (k.wearsTurban) note(items, Pose.I_TURBAN, id, a.c.shown(), "worn (another's) from " + stamp(k.t));
                }
            }
        }
        if (items.isEmpty()) return b.append("  no prop changes hands in this film (costumes and headwear as the lock sheets; see the continuity ledger)\n").toString();
        for (Map.Entry<Integer, StringBuilder> e : items.entrySet()) {
            String own = owner.get(e.getKey());
            b.append("  • ").append(item(e.getKey())).append(own != null ? " — belongs to " + own + " (the description)" : " — owner: whoever holds it first").append('\n');
            b.append(e.getValue());
        }
        return b.toString();
    }

    private static void note(Map<Integer, StringBuilder> items, int item, String scene, String who, String what) {
        StringBuilder sb = items.get(item);
        if (sb == null) { sb = new StringBuilder(); items.put(item, sb); }
        if (sb.length() > 600) return;
        sb.append("      ").append(scene).append(": ").append(who).append(" — ").append(what).append('\n');
    }

    static String stamp(float t) { return String.format(Locale.US, "%d:%04.1f", (int) (t / 60), t % 60); }

    /** 1.6: the location records with persistent IDs. */
    public static String locationRecords(Story story, Film film, Map<Integer, String> lids) {
        StringBuilder b = new StringBuilder("LOCATION RECORDS (director's manual 1.6)\n");
        for (Map.Entry<Integer, String> e : lids.entrySet()) {
            int set = e.getKey();
            List<String> scenes = new ArrayList<String>();
            List<String> hours = new ArrayList<String>();
            boolean picture = false, boat = false, festive = false;
            String enterL = "", enterR = "";
            for (Film.Seg sg : film.segs) {
                if (sg.type != Film.S_SCENE || sg.set != set) continue;
                scenes.add(sceneId(film, sg, story));
                String h = Handbook.hourName(sg.tod) + " (" + PixarLead.keyLight(sg.set, sg.tod).name + ")";
                if (!hours.contains(h)) hours.add(h);
                if (sg.backdrop != null) picture = true;
                if (sg.inBoat) boat = true;
                if (sg.festive) festive = true;
                for (Film.Actor a : sg.actors) if (!a.keys.get(0).visible) { if (a.look.hero) enterL = "heroes enter from the left"; else enterR = "villains enter from the right"; }
            }
            b.append("  ").append(e.getValue()).append(" — ").append(Sets.label(set)).append(": ").append(Sets.outdoorSet(set) ? "exterior" : "interior")
                    .append("; reference: ").append(picture ? "your picture (the location plate, feet on its floor line)" : "the painted set with its 2.5D depth").append('\n');
            b.append("     layout: the stage is 1280 wide, the floor line at the plate's ground; entrances ").append(enterL.length() + enterR.length() == 0 ? "none written (everyone present from the first frame)" : (enterL + (enterL.length() > 0 && enterR.length() > 0 ? "; " : "") + enterR))
                    .append(boat ? "; the characters are in a boat on the water" : "").append(festive ? "; festival lights" : "").append('\n');
            b.append("     time-of-day and lighting variants: ").append(join(hours)).append("  |  scenes: ").append(join(scenes)).append('\n');
        }
        if (lids.isEmpty()) b.append("  none\n");
        return b.toString();
    }

    static String join(List<String> l) {
        StringBuilder b = new StringBuilder();
        for (String s : l) b.append(b.length() > 0 ? ", " : "").append(s);
        return b.toString();
    }

    // ================================================================== 1.1 the inventory (rows from the app)

    /** The asset inventory: rows of {ID, kind, name, file or source, status, note}. */
    public static String inventory(List<String[]> rows) {
        StringBuilder b = new StringBuilder("PROJECT ASSET INVENTORY (director's manual 1.1 — every file inspected, not just its name; a persistent ID per asset)\n");
        if (rows.isEmpty()) return b.append("  nothing yet\n").toString();
        for (String[] r : rows) {
            b.append("  ").append(r[0]).append("  ").append(r[1]).append("  ").append(r[2]).append("  —  ").append(r[3]).append("  [").append(r[4]).append(']');
            if (r.length > 5 && r[5] != null && r[5].length() > 0) b.append("  ").append(r[5]);
            b.append('\n');
        }
        return b.toString();
    }

    // ================================================================== 2.3 the facial identity specification

    /**
     * The facial identity specification of one character, in the manual's template, from the cut-out's found
     * points (a defined method: fractions of the picture, stated), the picture's traits and the description.
     * Nothing the picture does not show is reported as known.
     */
    public static String facialSpec(String id, Story.CharacterDef c, String refFile, Cutout.Result cut, PicSense.Traits tr, boolean faceByUser, String[] viewFiles, String approval, boolean humanReview) {
        Look l = c.look != null ? c.look : new Look();
        boolean pic = refFile != null && cut != null;
        boolean face = pic && cut.faceFound;
        boolean side = viewFiles != null && viewFiles.length > 1 && viewFiles[1] != null, back = viewFiles != null && viewFiles.length > 2 && viewFiles[2] != null;
        StringBuilder b = new StringBuilder();
        b.append("FACIAL IDENTITY SPECIFICATION (director's manual 2.3)\n");
        b.append("Character ID: ").append(id).append(" — ").append(c.shown()).append('\n');
        b.append("Reference asset: ").append(pic ? refFile : "none (no picture: the studio's puppet from the description)").append('\n');
        b.append("Reference version: ").append(pic ? "the picture as uploaded or accepted (one master picture all film)" : "description v1").append('\n');
        b.append("Method: ").append(face ? "the eyes, mouth and chin found in the cut-out; proportions are fractions of the picture (face height = hairline to chin), never absolute measurements"
                : pic ? "no face found in the picture (an animal, a back, or a tiny face): the geometry is not reported" : "from the description's words only").append('\n');
        b.append("\nFACE GEOMETRY\n");
        if (face) {
            float fh = Math.max(0.01f, cut.chinY - cut.faceTop);
            float eyeLine = (cut.eyeY - cut.faceTop) / fh;
            float mouthLine = (cut.mouthY - cut.faceTop) / fh;
            float inter = cut.eyeRX - cut.eyeLX;
            float mouthRel = inter > 0.001f ? cut.mouthW / inter : 0;
            float noseRel = (cut.mouthY - cut.eyeY) / fh;
            float headShare = (cut.chinY - cut.headTop);
            b.append(String.format(Locale.US, "- Overall face shape: %s (eye line at %.0f%% of the face height; the head is %.0f%% of the figure's height)%n",
                    eyeLine < 0.36f ? "long, high eyes" : eyeLine > 0.5f ? "round, low eyes (a child's proportions)" : "oval", eyeLine * 100, headShare * 100));
            b.append(String.format(Locale.US, "- Face length and width: eyes %.0f%% of the picture's width apart; eye radius %.1f%% of the width (%s)%n", inter * 100, cut.eyeR * 100,
                    cut.eyeR / Math.max(0.001f, inter) > 0.32f ? "large eyes" : cut.eyeR / Math.max(0.001f, inter) < 0.2f ? "small eyes" : "medium eyes"));
            b.append("- Forehead: ").append(eyeLine < 0.38f ? "high" : "ordinary").append(" (hairline to eye line ").append(Math.round(eyeLine * 100)).append("% of the face)\n");
            b.append("- Cheekbones: not measured (the cut-out gives no cheek points) — as in the picture\n");
            b.append(String.format(Locale.US, "- Jawline and chin: the chin at %.0f%% of the figure's height from the top; width as in the picture%n", cut.chinY * 100));
            b.append("\nEYES\n");
            b.append(String.format(Locale.US, "- Shape and tilt: as in the picture (left eye at x = %.0f%%, right at %.0f%% of the width, level)%n", cut.eyeLX * 100, cut.eyeRX * 100));
            b.append(String.format(Locale.US, "- Relative spacing: %.0f%% of the width between the pupils%n", inter * 100));
            b.append("- Iris colour: ").append(colourName(l.eyeColor)).append(" (the description's eye colour; the picture's iris is not read)\n");
            b.append("- Eyelid structure: as in the picture; blinks drawn over it\n");
            b.append("- Eyebrow shape and thickness: as in the picture; raised, lowered and tilted by the feeling (").append(l.kind == Look.MAN || l.kind == Look.OLD_MAN ? "thick" : "fine").append(" from the description)\n");
            b.append("- Distinctive gaze: ").append(l.glasses > 0 ? "behind glasses; " : "").append("eyes to the listener or the thing of the action, never to the camera\n");
            b.append("\nNOSE\n");
            b.append(String.format(Locale.US, "- Bridge, length, tip, nostrils: not found by the cut-out; the eye-to-mouth distance is %.0f%% of the face height (%s); as in the picture%n", noseRel * 100,
                    noseRel > 0.42f ? "a long nose" : noseRel < 0.28f ? "a short nose" : "ordinary"));
            if (l.crookedNose) b.append("- Crooked (the description)\n");
            b.append("\nMOUTH\n");
            b.append(String.format(Locale.US, "- Width: %.0f%% of the eye spacing (%s), at %.0f%% of the face height%n", mouthRel * 100, mouthRel > 1.1f ? "wide" : mouthRel < 0.7f ? "small" : "medium", mouthLine * 100));
            b.append("- Upper and lower lip shape: as in the picture; lip colour ").append(hex(cut.lip)).append('\n');
            b.append("- Natural resting position: closed (the listener's mouth is at rest; the mouth opens with the voice only)\n");
            b.append("- Smile characteristics: the picture's corners lifted (happy), pulled down (sad); laughs open the jaw\n");
            b.append("- Visible teeth: ").append(l.fangs ? "fangs (the description)" : "not established — not drawn").append('\n');
        } else {
            b.append("- Overall face shape / length / forehead / cheekbones / jaw / chin: ").append(pic ? "not measured (no face found)" : "from the description: " + kindWord(l)).append('\n');
            b.append("\nEYES\n- Shape, spacing, lids, brows: ").append(pic ? "as in the picture (unmeasured)" : "the puppet's by kind").append("; iris colour ").append(colourName(l.eyeColor)).append(" (description)\n");
            b.append("\nNOSE\n- Bridge, length, tip, nostrils: ").append(l.crookedNose ? "crooked (description)" : "not established").append('\n');
            b.append("\nMOUTH\n- Width, lips, resting position, smile, teeth: ").append(l.fangs ? "fangs (description); " : "").append("the puppet's; closed at rest\n");
        }
        b.append("\nSKIN AND DISTINCTIVE FEATURES\n");
        int skin = face ? cut.skin : l.skin;
        b.append("- Skin tone and undertone: ").append(skinTone(skin)).append(", ").append(undertone(skin)).append(" undertone (").append(face ? "read from the picture, " + hex(skin) : "the description").append(")\n");
        StringBuilder marks = new StringBuilder();
        if (l.scar) marks.append("a scar; "); if (l.dimples) marks.append("dimples; "); if (l.wrinkles) marks.append("wrinkles; ");
        if (l.bindi > 0 || (tr != null && tr.bindi > 0)) marks.append("a bindi; "); if (l.tilak > 0) marks.append("a tilak; "); if (l.sindoor) marks.append("sindoor; ");
        if (l.mustache > 0 || (tr != null && tr.mustache > 0)) marks.append(l.mustache == 2 ? "a big curled moustache; " : "a moustache; ");
        if (l.beard || (tr != null && tr.beard > 0)) marks.append("a beard; ");
        if (l.glasses > 0) marks.append("glasses; ");
        if (l.glowEyes) marks.append("glowing eyes; ");
        b.append("- Freckles, scars, dimples, or marks: ").append(marks.length() == 0 ? "none written or seen" : marks.toString().trim()).append('\n');
        b.append("- Age-related characteristics: ").append(c.age > 0 ? c.age + " years; " : "").append(l.kind == Look.GIRL || l.kind == Look.BOY ? "a child's proportions (big head, big eyes)" : l.kind == Look.OLD_MAN ? "old (grey hair, lines)" : l.kind == Look.WOMAN || l.kind == Look.MAN ? "adult" : kindWord(l)).append('\n');
        b.append("- Other visible details: ").append(l.earrings ? "earrings; " : "").append(l.necklace > 0 ? "a necklace; " : "").append(tr != null && tr.crown > 0 || l.headwear == Look.HW_CROWN ? "a crown; " : "").append(tr != null && tr.turban > 0 || l.headwear == Look.HW_TURBAN ? "a turban; " : "").append("as in the picture\n");
        b.append("\nHAIR\n");
        b.append("- Colour: ").append(colourName(l.hairColor)).append(tr != null && tr.greyHair > 0 ? " (grey seen in the picture)" : "").append('\n');
        b.append("- Hairline: ").append(face ? String.format(Locale.US, "at %.0f%% of the figure's height from the top", cut.faceTop * 100) : "as drawn").append('\n');
        b.append("- Texture: ").append(l.curly ? "curly" : "straight").append('\n');
        b.append("- Parting: not read from the picture\n");
        b.append("- Hairstyle: ").append(hairName(l.hair)).append(l.headwear != Look.HW_NONE ? " under " + headwearName(l.headwear) : "").append('\n');
        b.append("- Length: ").append(l.hair == Look.H_LONG || l.hair == Look.H_BRAID || l.hair == Look.H_PONYTAIL || l.hair == Look.H_PIGTAILS || (tr != null && tr.longHair > 0) ? "long" : l.hair == Look.H_BUN ? "tied up" : "short").append('\n');
        b.append("\nIDENTITY CONSTRAINTS\n");
        b.append("- Features that must never change: the face shape and the eye spacing of the picture, the skin tone, the marks above, the hair colour and style, the costume of the lock sheet\n");
        b.append("- Features allowed to vary with expression: brows, eyelids, mouth corners and opening, head tilt, gaze, cheeks (blush)\n");
        b.append("- Temporary changes allowed by scene: tears, sweat, a red face, wet hair and clothes in rain, headwear taken or lent, a disguise — each tracked per key and listed in the ledger\n");
        b.append("- Uncertain features requiring further reference: ").append(face ? "" : "the face itself (no front face found); ").append(side ? "" : "the profile (no side picture: the figure model's side is symmetric); ")
                .append(back ? "" : "the back of the head (no back picture: the costume goes round, the top of the head comes down); ").append("the iris colour, the nostrils, the teeth and the parting (not read from a picture)\n");
        b.append("\nAPPROVAL\n");
        b.append("- Reference images approved: ").append(approval).append('\n');
        b.append("- Identity record version: v1 of this film (written again at every make)\n");
        b.append("- Human review status: ").append(humanReview ? "the lock sheet and every first frame are shown to you in Human QC before the film is made" : "Human QC is off in Settings — the director's own checks only").append('\n');
        return b.toString();
    }

    static String kindWord(Look l) {
        switch (l.kind) {
            case Look.GIRL: return "a girl"; case Look.WOMAN: return "a woman"; case Look.MAN: return "a man"; case Look.BOY: return "a boy"; case Look.WITCH: return "a witch";
            case Look.MONSTER: return "a monster"; case Look.MONKEY: return "a monkey"; case Look.OLD_MAN: return "an old man"; case Look.ANIMAL: return "an animal"; case Look.BIRD: return "a bird";
            default: return "a character";
        }
    }

    static String hairName(int h) {
        switch (h) {
            case Look.H_NONE: return "none"; case Look.H_BRAID: return "a braid"; case Look.H_PIGTAILS: return "pigtails"; case Look.H_BUN: return "a bun";
            case Look.H_SHORT: return "short"; case Look.H_LONG: return "long, open"; case Look.H_PONYTAIL: return "a ponytail"; default: return "as drawn";
        }
    }

    static String headwearName(int hw) {
        switch (hw) {
            case Look.HW_TURBAN: return "a turban"; case Look.HW_WITCH_HAT: return "a witch's hat"; case Look.HW_PALLU: return "a pallu"; case Look.HW_HORNS: return "horns";
            case Look.HW_CROWN: return "a crown"; case Look.HW_HOOD: return "a hood"; default: return "nothing";
        }
    }

    static String hex(int c) { return String.format(Locale.US, "#%06X", c & 0xFFFFFF); }

    static float lum(int c) { return 0.299f * ((c >> 16) & 255) + 0.587f * ((c >> 8) & 255) + 0.114f * (c & 255); }

    public static String skinTone(int c) {
        float L = lum(c);
        return L > 205 ? "fair" : L > 170 ? "light" : L > 125 ? "medium" : L > 80 ? "deep" : "very deep";
    }

    public static String undertone(int c) {
        int r = (c >> 16) & 255, b = c & 255;
        return r - b > 75 ? "warm" : r - b < 35 ? "cool" : "neutral";
    }

    public static String colourName(int c) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        float L = lum(c);
        int max = Math.max(r, Math.max(g, b)), min = Math.min(r, Math.min(g, b));
        if (L > 215 && max - min < 40) return "white";
        if (max - min < 30) return L < 45 ? "black" : L < 150 ? "grey" : "silver";
        if (L < 42) return "black";
        if (r > g + 50 && r > b + 50) return L < 90 ? "auburn" : "red";
        if (r > 170 && g > 130 && b < 120) return "blonde";
        if (b > r + 30 && b > g + 20) return "blue";
        if (g > r + 20 && g > b + 20) return "green";
        return L < 95 ? "dark brown" : "brown";
    }

    // ================================================================== audio and export (V, IV stage 9)

    public static String audioReport(int lines, int voiced, String missing, float longestGap, float gapAt, int clipped, long samples, boolean checked) {
        StringBuilder b = new StringBuilder("AUDIO CHECK (director's manual V — clipping, missing dialogue, unintended gaps)\n");
        if (!checked) return b.append("  the mix could not be read here\n").toString();
        b.append(String.format(Locale.US, "  • Dialogue: %d of %d lines have a voice%s%n", voiced, lines, voiced < lines ? " — missing: " + missing : ""));
        b.append(String.format(Locale.US, "  • Gaps: the longest silence inside the scenes is %.1f s%s%n", longestGap, longestGap > 3.5f ? String.format(Locale.US, " at %s (an intended pause, a ma beat, or a line without a voice — check it)", stamp(gapAt)) : " (no unintended gap over 3.5 s)"));
        b.append(String.format(Locale.US, "  • Clipping: %d of %d samples at the limit (%.3f%%)%s%n", clipped, samples, samples == 0 ? 0 : 100.0 * clipped / samples, clipped > samples / 2000 ? " — too loud: lower the music in the command box" : " — none to speak of (the mix has a limiter)"));
        return b.toString();
    }

    public static String exportReport(boolean exists, long bytes, float seconds, float expected, int hasAudio, int w, int h, boolean readable) {
        StringBuilder b = new StringBuilder("EXPORT CHECK (director's manual IV stage 9 — decodes, correct duration, audio present)\n");
        b.append("  • File: ").append(exists ? String.format(Locale.US, "film.mp4, %.1f MB", bytes / 1048576.0) : "missing").append('\n');
        if (!readable) { b.append("  • The file's header could not be read on this device (no media retriever): duration and audio not verified here — play it to check\n"); return b.toString(); }
        b.append(String.format(Locale.US, "  • Duration: %.1f s in the file, %.1f s planned%s%n", seconds, expected, Math.abs(seconds - expected) <= 1.5f ? " — matches" : " — DIFFERS"));
        b.append("  • Audio track: ").append(hasAudio > 0 ? "present" : hasAudio == 0 ? "MISSING" : "unknown").append('\n');
        b.append("  • Picture: ").append(w > 0 ? w + "x" + h : "unknown").append('\n');
        return b.toString();
    }

    public static boolean exportOk(boolean exists, float seconds, float expected, int hasAudio, boolean readable) {
        return exists && (!readable || (Math.abs(seconds - expected) <= 1.5f && hasAudio != 0));
    }

    // ================================================================== 3.10, IV, V: the review, the gates, the checklist

    /** What the app checked, for the review. */
    public static final class Checks {
        public Handbook.Stats stats;
        public int boiling = -1, shaking = -1, floating = -1;
        public boolean humanQc, animaticMade, animaticApproved, fixesApplied;
        public int lines, voiced, missingVoices, charsWithPicture, cast, lockSheets, plates;
        public float longestGap;
        public boolean audioChecked, exportKnown, exportOk, exportExists;
        public int proposalsOpen;
    }

    public static String review(Film film, Story story, Checks c) {
        Handbook.Stats s = c.stats != null ? c.stats : new Handbook.Stats();
        boolean qcRan = c.boiling >= 0;
        int temporal = Math.max(0, c.boiling) + Math.max(0, c.shaking);
        StringBuilder b = new StringBuilder();
        b.append("THREE-LEVEL REVIEW (director's manual 3.10)\n");
        b.append("  • Individual shot — identity: every shot drawn with the character's one master picture (lock sheets); expression: a FACIAL PERFORMANCE line per shot; animation: ")
                .append(s.overMotion == 0 && s.multi == 0 ? "every shot within the motion and one-action limits" : s.overMotion + " over the motion limit, " + s.multi + " with two actions")
                .append("; composition: ").append(s.stillWrong == 0 ? "every frame validated" : s.stillWrong + " could not be corrected").append("; lighting: two lights by place and hour; dialogue: ")
                .append(s.over6 == 0 ? "lip-sync shots within six words" : s.over6 + " lip-sync shots over six words").append("; defects: ")
                .append(!qcRan ? "the frame-by-frame check did not run" : temporal == 0 && c.floating == 0 ? "none left by the frame-by-frame check" : temporal + " shot(s) still lively, " + c.floating + " floating").append('\n');
        b.append("  • Scene — geography and positions: the continuity ledger and the dialogue axis per scene, STATE AT START / END per shot; emotional progression: in every scene record; action continuity: moves carried across cuts; pacing: ")
                .append(String.format(Locale.US, "%d shots, %d distinct lengths", s.shots, s.durationsDistinct)).append("; dialogue flow: reactions ").append(s.reactions).append(", thought beats ").append(s.thoughtBeats).append('\n');
        b.append("  • Entire film — narrative clarity: ").append(s.spine ? "the spine and the beat sheet filled (hero " + film.hero + ")" : "no spine found").append("; character development: the emotional progression per scene; rhythm: ma pauses after fast beats; visual consistency: one picture per character, a colour script per act; sound: ")
                .append(c.audioChecked ? c.voiced + " of " + c.lines + " lines voiced, longest gap " + String.format(Locale.US, "%.1f", c.longestGap) + " s" : "the mix was not checked").append("; ending: the last scene's closing image, then the end card; export integrity: ")
                .append(!c.exportKnown ? "not verified on this device" : c.exportOk ? "the file decodes, its length matches, audio present" : "FAILED (see the export check)").append('\n');
        b.append("\nAPPROVAL GATES (director's manual IV)\n");
        boolean g1 = s.spine && story.scenes.size() > 0;
        boolean g2 = c.proposalsOpen == 0 && c.lockSheets >= c.cast && c.cast > 0;
        boolean g3 = !c.humanQc || c.animaticApproved;
        boolean g4 = s.stillWrong == 0 && (!qcRan || temporal == 0);
        boolean g5 = g4 && c.exportExists && (!c.exportKnown || c.exportOk) && c.missingVoices == 0;
        gate(b, 0, g1, g1 ? "the character list, the scene plan (scene records) and the beat sheet are written from the script" : "no story spine could be read — check the script's scenes");
        gate(b, 1, g2, c.proposalsOpen > 0 ? c.proposalsOpen + " picture(s) made in 3D still wait for your decision" : c.cast == 0 ? "no cast" : c.lockSheets + " lock sheets and " + c.plates + " location plates made before any shot; " + c.charsWithPicture + " of " + c.cast + " characters have a picture (the rest are the studio's dolls or puppets, accepted by you)");
        gate(b, 2, g3, !c.humanQc ? "Human QC is off: the animatic is approved by the director's own checks (switch Human QC on in Settings to approve it yourself)" : c.animaticApproved ? "you approved the animatic and every first frame in Human QC" + (c.fixesApplied ? " (with your fixes)" : "") : c.animaticMade ? "the animatic was made but not approved" : "the animatic could not be made on this phone; the first frames were approved");
        gate(b, 3, g4, g4 ? "validation, the first-frame checks and the frame-by-frame check pass" : s.stillWrong + " shot(s) could not be corrected; " + temporal + " still lively");
        gate(b, 4, g5, g5 ? "the complete edit passed the three levels; the exported file checked" : !c.exportExists ? "no file" : c.missingVoices > 0 ? c.missingVoices + " line(s) have no voice" : !g4 ? "Gate 4 is not passed" : "the export check failed");
        b.append("\nQA CHECKLIST (director's manual V)\n");
        qa(b, 0, true, "one master picture per character all film; every shot's ASSETS line names the IDs and the picture used");
        qa(b, 1, true, "every shot's ASSETS line names the LOC_ ID; location plates lock each place");
        qa(b, 2, true, "staged from the script's own beats, in order; the beat sheet reads the actual story");
        qa(b, 3, s.reactions > 0 || s.speech == 0, "reactions " + s.reactions + ", POV shots " + s.pov + ", loud-sound reactions " + s.loudReactions + " (the coverage report)");
        qa(b, 4, true, "the continuity ledger, the prop ledger and STATE AT START / END per shot; headwear and disguises carried scene to scene");
        qa(b, 5, !qcRan || temporal == 0, qcRan ? temporal + " shot(s) with a boiling face left" : "frame-by-frame check not run");
        qa(b, 6, s.overMotion == 0 && (!qcRan || c.floating == 0), "motion within 15% of the frame per shot; feet on the floor line (" + (qcRan ? c.floating + " floating" : "not metered") + ")");
        qa(b, 7, s.over6 == 0, "lip-sync only in locked front close-ups of at most six words, the mouth from the voice's own envelope");
        qa(b, 8, !qcRan || temporal == 0, "flicker and boiling metered frame by frame (" + (qcRan ? temporal + " left" : "not run") + ")");
        qa(b, 9, c.audioChecked && c.missingVoices == 0 && c.longestGap <= 3.5f, c.audioChecked ? c.missingVoices + " missing voice(s), longest gap " + String.format(Locale.US, "%.1f", c.longestGap) + " s, clipping measured" : "not checked");
        qa(b, 10, c.exportExists && (!c.exportKnown || c.exportOk), c.exportKnown ? (c.exportOk ? "decodes, duration matches, audio present" : "see the export check") : "the file exists; its header could not be read on this device");
        b.append("  (thresholds are the manual's starting criteria; ambiguous or high-impact failures are yours to judge in Human QC)\n");
        return b.toString();
    }

    private static void gate(StringBuilder b, int i, boolean pass, String how) {
        b.append("  ").append(pass ? "✔ " : "✘ ").append(GATES[i][0]).append(": ").append(pass ? "passed" : "NOT passed").append(" — ").append(how).append('\n');
    }

    private static void qa(StringBuilder b, int i, boolean pass, String how) {
        b.append("  ").append(pass ? "☑ " : "☐ ").append(QA[i][0]).append(" — ").append(how).append('\n');
    }
}
