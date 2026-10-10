package com.tarun.kahani.core;

import java.util.List;
import java.util.Locale;

/**
 * v30: the "AI Animated Film Director — Reference-Based Training & Production Guide" (character identity, 3D
 * construction, cinematic rendering, scene continuity, video workflows, quality assurance; Disney's twelve
 * principles and Pixar's philosophy integrated), hardcoded: what the studio does for every section, the twelve
 * principles mapped to the code that applies them, the scene brief (section 7) written into every shot list, the
 * weighted quality score of the whole film (section 12) with the critical-defect override, and the parts of the
 * guide a phone cannot do (model training, adapters, video models), said plainly.
 */
public final class DirectorTraining {
    private DirectorTraining() {}

    public static final String TITLE = "AI ANIMATED FILM DIRECTOR — Reference-Based Training & Production Guide (October 2026)";

    public static final String GOVERNING = "The character must remain the same; the camera, expression, action and environment may change when instructed. "
            + "Every approved still or shot must belong to the same coherent animated production.";

    /** The twelve principles (2.1) and where the studio applies each one. */
    public static final String[][] TWELVE_MAP = {
            {"Squash and stretch", "Rig: impact poses squash and stretch the mesh with the volume kept (jumps, landings, the belly on a laugh); never on a real picture of a pose (v27)"},
            {"Anticipation", "Director: a wind-up before every move and gesture (the anticipation key, Rig.smooth 0.2 s); thought before action (a pause and a look before a reaction); v34: a turned back reads for a moment before someone walks off, and the eyes go first, a quick turn after, when someone else comes in (Director.watch)"},
            {"Staging", "ShotPlanner: one idea per shot, the subject in the centre 60%, look room, the silhouette readable; the Technical Director's one action per shot; v34: everyone on the stage looks at an entrance, an exit, a reveal or someone hurt, which points the audience there too (Director.watch, Renderer.gaze); a still frame a character walks out of, a wide frame a run crosses (Director.frameAround); v35: a photo eating or drinking is staged in a close-up so the cup reaching the lips reads (Renderer.drawToLips); v36: lighting a lamp is framed low and wide so the flames catching read; a task is shown with its tool (Director.taskFrom)"},
            {"Straight ahead and pose to pose", "Film.Key: the key poses first (stateAt interpolates between them); the user's own pose pictures are the extremes (Casting, v27)"},
            {"Follow through and overlapping action", "Renderer: hair, cloth, ears and tails lag behind the body (secondary motion per picture part); the stop after a walk settles over a fifth of a second; v35: grass, leaves and branches follow the gusts, the tips after the roots (Nature.gustField); v36: a pallu, a dupatta, hair, fur, a mane and a tail follow the same gusts outdoors (Puppet.clothTail)"},
            {"Slow in and slow out", "Rig.smooth / ease curves on every move, gesture and camera move; no linear timing anywhere; v35: sitting down, getting up, lying back and sitting up pass through every in-between (Pose.sit, Pose.lie), slower from a low seat"},
            {"Arcs", "Limb and head paths through the rig follow arcs (rotation about joints, never a straight slide); camera moves ease along a curve"},
            {"Secondary action", "Breathing (mo.sy), blinks, idle weight shift (halved in v27), hand gestures with the manner of the line; v34: eyes that follow whoever speaks, looking up when rain begins and covering the head, a shiver in snow, a hand out in front for a blindfolded walk (Renderer.gaze, Director.skyReaction, travelFrom)"},
            {"Timing", "Director: heavy characters (monsters) get fewer, slower frames; light ones quick; cuts at 2.6 s; a ma pause after two fast beats; v34: a walk is timed by its distance and the walker (a run, a slow walk, a stick, a blindfold; never over 4.5 s), the story waits for it, and those left behind look after someone who has gone for a moment"},
            {"Exaggeration", "Pose emotions pushed (eyes, brows, mouth) but never past the identity: the face points stay the picture's own"},
            {"Solid drawing", "Figure3D views from the picture keep its proportions; the feet on the floor line with a contact shadow; one height per character all film; v35: a seated character's hips on the seat's own height and the feet on the floor (Puppet.seatHeight)"},
            {"Appeal", "The user's own pictures first (v26–v29), readable silhouettes, expressive eyes and mouth through the mesh, design harmony from the style cue"},
    };

    /** Pixar's principles (2.2) and the studio's answer. */
    public static final String[][] PIXAR_MAP = {
            {"Story is king", "every shot is planned from a line or a stage direction; no decorative shot exists (ShotPlanner from the script only)"},
            {"Emotional resonance", "the feeling of every line read from its manner; the face and body carry it before any surface detail (Pose emotions, Casting's feeling pictures)"},
            {"Clarity of idea", "one action per shot (Technical Director), the five questions answered for every shot (Handbook)"},
            {"Believable worlds", "the place's hour, weather and floor line; the two-light model with the bounce from the ground; the user's own place pictures"},
            {"Appeal and design harmony", "the style cue from the user's pictures rules every made picture; the colour script per act"},
            {"Cinematic language", "shot sizes, angles, lenses and moves chosen by purpose (ShotPlanner, Handbook.focalFor, Handbook.angleMeaning)"},
            {"Iteration and honesty", "Human QC of the first frames, the Final QC at quarter speed, the weighted score below with the critical-defect override"},
            {"Team memory", "cast.txt (the manifest), the library's readings (posetag), the asset inventory, the facial identity specification, the continuity ledger"},
    };

    /** Every section of the guide and what the studio does for it. */
    public static final String[][] SECTIONS = {
            {"1 System architecture", "input layer = the story screen and the library; reference analyser = PicSense + Cutout + PoseSense + Angles; canonical asset memory = cast.txt + the library's metadata; still-image engine = Studio3DArt / Figure3D / Doll3D; scene-sequence engine = Director + ShotPlanner; quality control = FinalQc + Handbook.score + this score"},
            {"2 Disney and Pixar principles", "the two maps above"},
            {"3 Reference-image understanding", "PicSense reads identity, proportions, colours, clothing and the place; Cutout the face; Angles the angle; PoseSense the pose and feeling; confidence high / medium / low in the character bible (SceneMaker.bible)"},
            {"4 Character identity specification", "CHAR_001… IDs that never change (DirectorsManual.charIds); the master picture, its views and pose pictures linked to the ID; the facial identity specification; the change history from the user's corrections (learnChoice, the pose review)"},
            {"5 Building a convincing 3D character", "Figure3D from the silhouette and the picture's texture; the face points kept under rotation; the rig only where a face can speak; a real picture of a pose is never bent (v27)"},
            {"6 Visual style specification", "StyleCue (light side, saturation, contrast) from the references; the colour script per act; the format's resolution and aspect ratio"},
            {"7 Prompt interpretation and scene construction", "the SCENE BRIEF written for every scene of the shot list: subjects (IDs), action, environment, lighting and mood, camera, constraints"},
            {"8 Camera, composition and cinematic language", "the nine shot types of the table are all planned (ShotPlanner sizes, OTS, POV, low and high angles, lead room for movement); screen direction kept across cuts"},
            {"9 Lighting and rendering", "key + bounce (fill) + rim; contact shadows; no bloom past the filmic grade; one light direction per shot"},
            {"10 Consistency across video scenes", "the continuity record of every shot (ASSETS, STATE AT START / END, TRANSITION IN); character, spatial and temporal continuity checked by the ledger"},
            {"11 Master system prompt", "this class and the protocols screen: the studio's behaviour is code, not a prompt"},
            {"12 Quality-control scoring engine", "the weighted score of the film (QC SCORE) with the categories, the thresholds 85 / 70 and the critical-defect override; the automatic rejection conditions the studio can see"},
            {"13 Training-data preparation and model improvement", "not possible on a phone: no fine-tuning, no adapters, no image or video model runs inside the app; the user's pictures and corrections are the only learning (said plainly)"},
            {"14 Implementation checklist", "reference processing ✔, character consistency ✔, image generation ✔ (style, pose, camera, resolution and aspect ratio), video production ✔ (script → scenes → shots → keyframes → motion → review), quality assurance ✔ (weighted score, critical defects, the repair loop through Human QC, asset versioning in the library)"},
            {"15 Recommended development order", "followed in this order since v18: reference stills, memory, multi-shot scenes, motion, automated QC; model training left out"},
    };

    /** What the guide asks for that a phone app cannot do, and what the studio does instead. */
    public static final String[][] LIMITS = {
            {"Fine-tune a model or train a character adapter (13, 15)", "no model training on the phone; identity comes from the user's own pictures (master, views, pose pictures) drawn as they are"},
            {"An image-to-video model for motion (10.3)", "motion is the rig and the user's pose pictures (the step cycle); no video model"},
            {"Ask for clarification on a contradiction (7.1)", "the studio applies its documented priority: the master picture over the description, the explicit instruction over both (SceneMaker)"},
            {"Tracking-style composition (8)", "the Technical Director's locked tripod wins: lead room is left in the frame, the camera does not track"},
    };

    // ------------------------------------------------------------------ 7: the scene brief

    /** The structured scene brief of a scene (section 7), written at the head of its shots in the shot list. */
    public static String brief(Film film, Story story, Film.Seg sg, Film.Shot first, java.util.Map<Story.CharacterDef, String> cids) {
        if (sg == null || sg.scene < 0 || sg.scene >= story.scenes.size()) return "";
        Story.Scene sc = story.scenes.get(sg.scene);
        StringBuilder b = new StringBuilder("SCENE BRIEF (training guide §7)\n");
        StringBuilder subj = new StringBuilder();
        for (Film.Actor a : sg.actors) {
            String id = cids == null ? null : cids.get(a.c);
            subj.append(subj.length() > 0 ? ", " : "").append(a.c.shown()).append(id != null ? " (" + id + ")" : "");
        }
        b.append("  Subjects: ").append(subj.length() > 0 ? subj : "no character (the place alone)").append('\n');
        String action = Director.firstSentence(sc.setting);
        if (action.length() > 140) action = action.substring(0, 140) + "…";
        b.append("  Action: ").append(action.length() > 0 ? action : sc.title.length() > 0 ? sc.title : "as the lines say").append('\n');
        b.append("  Environment: ").append(Sets.label(sg.set)).append(" — floor line at ").append(Math.round(sg.ground)).append(", the place's own picture when given\n");
        b.append("  Lighting and mood: ").append(first != null && first.light >= 0.7f ? "harder, directional light" : first != null && first.light <= 0.25f ? "soft, warm light" : "the natural light of the place")
                .append("; mood ").append(DirectorsManual.moodName(sg.mood)).append('\n');
        b.append("  Camera: ").append(first != null ? ShotPlanner.SIZE_NAME[Math.max(0, Math.min(ShotPlanner.SIZE_NAME.length - 1, first.size))] + " first, " + ShotPlanner.MOVE_NAME[Math.max(0, Math.min(ShotPlanner.MOVE_NAME.length - 1, first.move))].toLowerCase(Locale.US) : "as planned").append("; locked tripod\n");
        b.append("  Constraints: identity locked to the master pictures; no outfit change unless the script says so; every named character and prop present; screen direction kept\n");
        return b.toString();
    }

    // ------------------------------------------------------------------ 12: the weighted score of the film

    /** The film's six ratings (0–5), from what the director and the final QC saw. */
    public static int[] ratings(Film film, Art art, Story story, FinalQc.Result qc) {
        int chars = 0, withPicture = 0, withRealAngles = 0, dolls = 0;
        java.util.Set<String> seen = new java.util.HashSet<String>();
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors) {
            if (a.c == null || !seen.add(a.c.id)) continue;
            chars++;
            Art.Sprite sp = art == null ? null : art.sprites.get(a.c.id);
            if (sp != null) { withPicture++; if (sp.poses != null && !sp.poses.isEmpty() || sp.views != null) withRealAngles++; } else dolls++;
        }
        // identity: the user's own picture of every character is the canonical reference; a drawn puppet is medium
        int identity = chars == 0 ? 5 : Math.round(2 + 3f * withPicture / chars);
        // geometry: feet on the floor line, faces whole (first-frame checks), real pictures never bent
        int geometry = 4;
        if (qc != null && qc.feetChecked > 0) geometry = qc.floating == 0 ? 5 : qc.floating * 10 < qc.feetChecked ? 4 : 3;
        else if (film.stats != null && film.stats.stillWrong == 0) geometry = 5;
        // style: one style cue, one line of pictures; a mix of pictures and dolls is medium
        int style = dolls == 0 ? 5 : withPicture == 0 ? 3 : 4;
        // lighting and composition: no boiling, no shake left; the frames framed by the protocol
        int light = 5;
        if (qc != null) light = Math.max(2, 5 - Math.min(3, qc.boilingLeft + qc.shakingLeft));
        if (film.stats != null && film.stats.stillWrong > 0) light = Math.min(light, 4);
        // prompt compliance: every character of every scene drawn in at least one shot of it
        int comply = 5;
        int missing = missingCharacters(film);
        if (missing > 0) comply = Math.max(1, 5 - missing);
        // continuity: the ledger holds the states; jump cuts removed; one picture per shot
        int cont = film.stats != null ? (film.stats.jumpRemoved <= 2 ? 5 : 4) : 4;
        return new int[]{Math.min(5, identity), geometry, style, light, comply, cont};
    }

    /** Characters present in a scene who are drawn in no shot of that scene (12.1: a required character missing). */
    public static int missingCharacters(Film film) {
        int missing = 0;
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            for (Film.Actor a : sg.actors) {
                boolean visibleOnce = false;
                for (Film.Key k : a.keys) if (k.visible) { visibleOnce = true; break; }
                if (!visibleOnce) continue;
                boolean drawn = false;
                for (Film.Shot sh : film.shots) {
                    if (sh.t + sh.dur <= sg.t0 || sh.t >= sg.t1) continue;
                    if (a.stateAt(sh.t + Math.min(0.5f, sh.dur * 0.5f)).visible) { drawn = true; break; }
                }
                if (!drawn) missing++;
            }
        }
        return missing;
    }

    /** The automatic rejection conditions (12.1) the studio can see, or "" when none applies. */
    public static String critical(Film film, Art art, FinalQc.Result qc) {
        int missing = missingCharacters(film);
        if (missing > 0) return missing + " character(s) of a scene drawn in none of its shots";
        if (qc != null && qc.feetChecked > 0 && qc.floating * 2 > qc.feetChecked) return "most feet positions float above the floor (broken geometry)";
        return "";
    }

    /** The QC SCORE card of a film for the shot list (section 12), with the ratings and the override. */
    public static String scoreCard(Film film, Art art, Story story, FinalQc.Result qc) {
        int[] r = ratings(film, art, story, qc);
        int sc = SceneMaker.score(r);
        String crit = critical(film, art, qc);
        StringBuilder b = new StringBuilder();
        b.append("QC SCORE (training guide §12 — weighted rubric, 0–5 per category): ").append(sc).append("/100 — ").append(SceneMaker.status(sc, crit)).append('\n');
        for (int i = 0; i < SceneMaker.CATEGORIES.length; i++)
            b.append(String.format(Locale.US, "  %-26s %d/5  (weight %d%%)%n", SceneMaker.CATEGORIES[i], r[i], SceneMaker.WEIGHTS[i]));
        b.append("  Critical-defect override: ").append(crit.length() > 0 ? "YES — " + crit : "none (no character missing from its scene, feet on the floor)").append('\n');
        b.append("  Automatic rejection conditions watched: a face differing from the canonical picture (impossible: the pictures are the user's own, drawn as they are); "
                + "a required character or prop missing (counted above); extra limbs or broken geometry (the feet check); an outfit change without instruction (impossible: one picture set per character); "
                + "the action or composition violated (the validation layer); unexplained changes between frames (the Final QC's boiling and shake check); artefacts (the Final QC).\n");
        return b.toString();
    }

    /** The guide in short, for the protocols screen. */
    public static final String SUMMARY;
    static {
        StringBuilder b = new StringBuilder(TITLE).append('\n');
        b.append("Governing principle: ").append(GOVERNING).append('\n');
        b.append("The twelve principles, and where the studio applies each:\n");
        for (String[] p : TWELVE_MAP) b.append("  • ").append(p[0]).append(" — ").append(p[1]).append('\n');
        b.append("Pixar's philosophy:\n");
        for (String[] p : PIXAR_MAP) b.append("  • ").append(p[0]).append(" — ").append(p[1]).append('\n');
        b.append("Sections 1–15, what the studio does:\n");
        for (String[] p : SECTIONS) b.append("  • ").append(p[0]).append(": ").append(p[1]).append('\n');
        b.append("What a phone cannot do, said plainly:\n");
        for (String[] p : LIMITS) b.append("  • ").append(p[0]).append(" → ").append(p[1]).append('\n');
        SUMMARY = b.toString();
    }
}
