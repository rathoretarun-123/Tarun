package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * The Technical Director package (docs/technical-director.md): everything needed to make the film's pictures,
 * clips, sounds and voices in any other app, written after the director has read and planned the story.
 *
 *  1. Character Lock Sheets — costume text copied verbatim from the script, height in feet, voice lock
 *  2. Location Lock Plates — foreground / midground / background with depth, ground and light, no characters
 *  3. Objects — every object the story names, where it appears, a prompt for each
 *  4. Sounds and voices — the background of every part, the effects, the music moods, every voice
 *  5. Shot table — every shot of the film with the six mandatory fields (CHARACTERS, PLACEMENT, ACTION,
 *     GROUNDING, LIGHTING, CAMERA), lip-sync split into close-ups of at most six words, a first-frame image
 *     prompt and a video prompt from that frame, and the validation checklist
 *  6. Pipeline order, error correction and the final law
 */
public final class ShotBook {
    private ShotBook() {}

    static final String STABLE = TechnicalDirector.STABLE;
    static final String CAMERA = TechnicalDirector.CAMERA;

    public static final String[] OBJECTS = {"तलवार", "sword", "भाला", "spear", "ढाल", "shield", "छड़ी", "wand", "stick", "शीशा", "दर्पण", "mirror",
            "पोटली", "potli", "satchel", "bag", "थैला", "घंटा", "घंटी", "bell", "दीया", "दीपक", "diya", "lamp", "लालटेन", "lantern", "मशाल", "torch",
            "रस्सी", "rope", "जाल", "net", "पत्थर", "stone", "rock", "चाबी", "key", "किताब", "book", "पत्र", "चिट्ठी", "letter", "नक्शा", "map", "ताज", "मुकुट",
            "crown", "हार", "necklace", "अंगूठी", "ring", "गेंद", "ball", "फल", "आम", "mango", "fruit", "फूल", "flower", "कली", "bud", "टोकरी", "basket",
            "घड़ा", "pot", "नाव", "boat", "पतंग", "kite", "ढोल", "drum", "बांसुरी", "बाँसुरी", "flute", "सिंहासन", "throne", "संदूक", "बक्सा", "chest", "box",
            "जड़ी-बूटी", "herbs", "बोतल", "bottle", "कटार", "dagger", "गदा", "mace", "कुल्हाड़ी", "axe", "परशु", "तितली", "butterfly"};

    /** Height in feet from the studio's relative height (a grown man = 1). */
    static String feet(Look l) {
        float ft = (l == null ? 1 : l.height) * 5.8f;
        return String.format(Locale.US, "%.1f ft", ft);
    }

    static String costume(Story.CharacterDef c) {
        String s = LookDesigner.section(c.description, "पहनावा", "पोशाक", "कपड़े", "outfit", "dress", "clothes", "costume", "wears");
        if (s.trim().length() == 0) s = c.description;
        return Bible.oneLine(s).replaceFirst("^[*•\\s]+", "");
    }

    static String face(Story.CharacterDef c) {
        String s = LookDesigner.section(c.description, "चेहरा", "face", "शरीर", "body", "रूप", "looks");
        return Bible.oneLine(s).replaceFirst("^[*•\\s]+", "");
    }

    static String clean(String s) { return TechnicalDirector.clean(s); }

    static String ground(int set) {
        switch (set) {
            case Sets.GARDEN: return "soft grass and a stone path";
            case Sets.FOREST: return "muddy forest floor with fallen leaves";
            case Sets.COURTYARD: return "sun-warmed marble floor";
            case Sets.GATE: return "stone paving before the gate";
            case Sets.CAVE_IN: case Sets.CAVE_MOUTH: return "rocky, damp cave ground";
            case Sets.HALL: return "polished marble floor";
            case Sets.VILLAGE: return "dusty packed earth";
            case Sets.CELEBRATION: return "stone floor strewn with marigold petals";
            case Sets.ROOFTOP: return "concrete terrace tiles";
            case Sets.BASEMENT: return "cracked concrete floor with puddles";
            case Sets.ROOM: return "wooden floor with a rug";
            case Sets.STREET: return "asphalt with a painted lane line";
            default: return "firm natural ground";
        }
    }

    static String foreground(int set) {
        switch (set) {
            case Sets.GARDEN: return "out-of-focus flowers and leaves at the bottom corners";
            case Sets.FOREST: return "mossy rock edge and out-of-focus ferns";
            case Sets.COURTYARD: return "edge of a carved pillar, out of focus";
            case Sets.GATE: return "edge of a potted plant, out of focus";
            case Sets.CAVE_IN: case Sets.CAVE_MOUTH: return "dark wet rock edge, out of focus";
            case Sets.HALL: return "edge of a silk curtain, out of focus";
            case Sets.VILLAGE: return "a wooden cart wheel, out of focus";
            case Sets.CELEBRATION: return "an out-of-focus marigold garland";
            case Sets.ROOFTOP: return "the edge of a planter with glowing solar flowers, out of focus";
            case Sets.BASEMENT: return "the corner of a broken arcade machine, out of focus";
            case Sets.ROOM: return "the arm of a sofa, out of focus";
            case Sets.STREET: return "a parked scooter, out of focus";
            default: return "out-of-focus leaves";
        }
    }

    /** The LIGHTING field: two lights only (Deakins) — the key and its bounce; the rim is the key's own edge light. */
    static String light(int set, int tod, float hard) {
        PixarLead.Key k = PixarLead.keyLight(set, tod);
        String ratio = hard >= 0.7f ? "harder key, bounce ratio 4:1, deeper shadows" : hard <= 0.25f ? "soft key, bounce ratio 1.5:1" : "bounce ratio 2:1";
        return k.name + " | " + k.bounce + " | Rim: the key's own edge light on hair and shoulders (no third lamp) — two lights only (" + ratio + ")";
    }

    static String framing(int size) {
        switch (size) {
            case ShotPlanner.XWIDE: return "the whole place with small full figures";
            case ShotPlanner.WIDE: return "full-body framing with the place around";
            case ShotPlanner.MWIDE: return "knees-up framing";
            case ShotPlanner.MEDIUM: return "waist-up framing";
            case ShotPlanner.MCU: return "chest-up framing";
            case ShotPlanner.CU: return "face and shoulders, face 65-75% of the frame";
            default: return "the face fills the frame";
        }
    }

    static Story.CharacterDef byShown(Story st, String shown) {
        for (Story.CharacterDef c : st.characters) if (c.shown().equals(shown)) return c;
        return null;
    }

    static List<String> chunks(String text, int max) {
        List<String> out = new ArrayList<String>();
        String[] w = text.trim().split("\\s+");
        for (int i = 0; i < w.length; i += max) {
            StringBuilder b = new StringBuilder();
            for (int j = i; j < Math.min(w.length, i + max); j++) b.append(j > i ? " " : "").append(w[j]);
            out.add(b.toString());
        }
        return out;
    }

    /** The whole package. aspect = the one final aspect ratio ("16:9", "9:16", "1:1"). lib may be null. */
    public static String write(Story st, SoundLib lib, String aspect) {
        Director.Options opt = new Director.Options();
        opt.technical = true;
        opt.sounds = lib;
        opt.aspect = aspect == null ? "16:9" : aspect;
        Director d = new Director(st, opt);
        d.prepare();
        Film film = d.direct(null);
        StringBuilder b = new StringBuilder();
        String ar = aspect == null ? "16:9" : aspect;
        b.append(TechnicalDirector.PROTOCOL).append("\n\n");
        b.append(PixarLead.SUMMARY).append("\n\n");
        b.append(Handbook.SUMMARY).append("\n\n");
        PixarLead.Format fmt = PixarLead.spec(ar);
        b.append("TECHNICAL DIRECTOR PACKAGE — ").append(st.title).append("\n");
        b.append("============================================================\n");
        b.append("FINAL_AR = ").append(ar).append("   (decided once; set it as the generator's shape.aspect_ratio parameter, never write it in a prompt)\n");
        b.append("FORMAT: ").append(TechnicalDirector.formatLine(ar)).append("\n");
        b.append("Safe zone (").append(fmt.id).append("): ").append(fmt.note).append(". Character scale lock: a standing adult is ")
                .append(Math.round(fmt.charScale * 100)).append("% of the frame height. Lens: ").append(fmt.focal).append(".\n");
        b.append("Resizing: never stretch; the ratio is a parameter; a plate is made natively at ").append(fmt.w).append("x").append(fmt.h)
                .append(" (no blurry upscale); letterbox or pillarbox instead of cutting heads or sides; the thumbnail and the poster are made separately.\n");
        b.append("Every shot: about 3 seconds, one action, locked tripod. Pixar quality comes from 100 perfect 3-second shots, not 1 bad 60-second shot.\n\n");
        b.append("PIPELINE ORDER\n");
        b.append("1. Make the Character Lock Sheets (images) below. (The studio makes its own for every character before any shot: lock_char_N.jpg with the film.)\n"
                + "2. Make the Location Lock Plates (images, no characters). (The studio makes its own for every place: lock_place_N.jpg.)\n"
                + "3. Use the Shot table below.\n"
                + "4. For each shot: make the first-frame image -> check it yourself -> fix and make again if needed. (In the app: Human QC shows the first frame of every shot.)\n"
                + "5. For each approved first frame: make a 3-second video with that image as input.\n"
                + "6. If a clip shakes or morphs: reduce the motion by 80% and make it again from the same first frame. (The studio plays every shot frame by frame "
                + "itself before the film is made and calms a lively one by 80%.)\n"
                + "7. Join the clips in an editor; add sound effects, film grain, and camera shake only there if needed.\n"
                + "8. Final check at 0.25x speed: morphing, floating feet, finger count. (The studio meters the finished film frame by frame as it is written; "
                + "the result is in the film's quality check, in this file after a film is made.)\n"
                + "Or upload your finished pictures, clips' stills, voices and sounds into this app: the studio places them by their names.\n\n");

        // ---- 0. the story engine: hero, spine, acts, Braintrust
        String[] spine = film.spine != null ? film.spine : PixarLead.spine(st);
        Story.CharacterDef hero = PixarLead.hero(st);
        int[] acts = PixarLead.acts(st);
        b.append("0. STORY SPINE (R4) — hero: ").append(hero == null ? "—" : hero.shown()).append("\n------------------------------------------------------------\n");
        for (String sp : spine) b.append("• ").append(sp).append("\n");
        b.append("Acts: ");
        for (int i = 0; i < st.scenes.size(); i++) b.append(i > 0 ? ", " : "").append("scene ").append(st.scenes.get(i).number).append(" = act ").append(acts[i]);
        b.append("\n");
        for (String r : PixarLead.STORY_RULES) b.append(r).append("\n");
        b.append("\n").append(film.braintrust != null ? film.braintrust.text : PixarLead.braintrust(film, st).text).append("\n");

        // ---- 1. characters
        b.append("1. CHARACTER LOCK SHEETS (").append(st.cast().size()).append(")\n------------------------------------------------------------\n");
        for (Story.CharacterDef c : st.cast()) {
            b.append("LOCK NAME: ").append(c.shown()).append("\n");
            if (c.fullName != null && !c.fullName.equals(c.shown())) b.append("Full name: ").append(c.fullName).append("\n");
            b.append("Kind: ").append(Bible.kindWord(c.look, false)).append(c.age > 0 ? ", " + c.age + " years" : "").append(", height ").append(feet(c.look)).append("\n");
            String f = face(c);
            if (f.length() > 0) b.append("FACE (verbatim): ").append(f).append("\n");
            b.append("COSTUME LOCK (verbatim — copy-paste this text into every prompt, never paraphrase): ").append(costume(c)).append("\n");
            if (!PixarLead.costumeCultural(c.description)) b.append("   (Braintrust: the costume has no real garment name — a ghagra-choli, a Banarasi saree, an achkan, a pagdi… — the writer decides.)\n");
            b.append("ANIMATION: ").append(PixarLead.stepName(PixarLead.stepFps(c))).append("; principles in every clip: ").append(PixarLead.PRINCIPLES).append("\n");
            List<String> vw = new ArrayList<String>(VoiceMatch.want(c).words);
            vw.addAll(VoiceStyle.forCharacter(c).words);
            b.append("VOICE IDENTITY (handbook ch. 10): ").append(Handbook.voiceIdentity(c)).append("\n");
            b.append("VOICE LOCK: ").append(Bible.voiceHint(c, false));
            if (!vw.isEmpty()) { b.append(" — "); for (int i = 0; i < vw.size(); i++) b.append(i > 0 ? ", " : "").append(vw.get(i)); }
            b.append(". Keep the same voice in every line; record or generate 10-20 s as a sample.\n");
            b.append("LOCK SHEET IMAGE PROMPT: Character reference sheet of ").append(c.shown()).append(", premium 3D animated feature style, ")
                    .append("front view, side view and back view standing on a plain pure-white background, plus four head expressions (neutral, happy, sad, angry). ")
                    .append("Height ").append(feet(c.look)).append(". ").append(f.length() > 0 ? "Face: " + f + " " : "")
                    .append("Costume: ").append(costume(c)).append(" Feet firmly on the ground with a soft contact shadow. Hands with 5 fingers, anatomically correct. No text.\n\n");
        }

        // ---- 2. places
        List<String[]> places = Bible.places(st);
        b.append("2. LOCATION LOCK PLATES (").append(places.size()).append(")\n------------------------------------------------------------\n");
        for (String[] p : places) {
            int set = Sets.detect(p[0] + " " + p[1]);
            int tod = Sets.detectTime(p[1], Sets.DAY);
            b.append("PLATE: ").append(p[0]).append("\n");
            if (p[1].length() > 0) b.append("Description (verbatim): ").append(Bible.oneLine(p[1])).append("\n");
            b.append("Foreground (0-1 m from the camera, blurry): ").append(foreground(set)).append("\n");
            b.append("Midground (2-4 m): empty stage for the characters, left third and right third clear\n");
            b.append("Background (10-100 m): ").append(Bible.oneLine(Bible.firstClauseOf(p[1].length() > 0 ? p[1] : p[0]))).append("\n");
            b.append("Ground rule: ").append(ground(set)).append(", flat where the characters stand\n");
            b.append("Lighting: ").append(light(set, tod, 0.4f)).append("\n");
            b.append("PLATE IMAGE PROMPT: ").append(clean(Bible.placePrompt(p[0], p[1], null).replace("wide establishing shot", "establishing plate")
                    .replace(" 16:9", ""))).append(" No characters, no people. Foreground: ").append(foreground(set)).append(". Ground: ").append(ground(set))
                    .append(". ").append(STABLE).append(".\n\n");
        }

        // ---- 3. objects
        Map<String, List<Integer>> objects = new LinkedHashMap<String, List<Integer>>();
        for (Story.Scene sc : st.scenes) {
            StringBuilder all = new StringBuilder(sc.setting).append(' ');
            for (Story.Beat bt : sc.beats) all.append(bt.text).append(' ').append(bt.manner == null ? "" : bt.manner).append(' ');
            for (String o : OBJECTS) {
                if (!Txt.has(all.toString(), o)) continue;
                List<Integer> l = objects.get(o);
                if (l == null) { l = new ArrayList<Integer>(); objects.put(o, l); }
                if (!l.contains(sc.number)) l.add(sc.number);
            }
        }
        b.append("3. OBJECTS (").append(objects.size()).append(")\n------------------------------------------------------------\n");
        for (Map.Entry<String, List<Integer>> e : objects.entrySet()) {
            b.append("OBJECT: ").append(e.getKey()).append("   (scenes ").append(e.getValue()).append(")\n");
            b.append("OBJECT IMAGE PROMPT: a single ").append(e.getKey()).append(" for a premium 3D animated Indian children's film, consistent design in every shot, ")
                    .append("plain pure-white background, soft studio light, a soft contact shadow under it, no hands, no text.\n");
        }
        b.append("\n");

        // ---- 4. sounds and voices
        b.append("4. SOUNDS AND VOICES\n------------------------------------------------------------\n");
        for (Film.Amb a : film.ambience) {
            Film.Seg sg = film.segAt(a.t0 + 0.05f);
            String where = sg != null && sg.scene >= 0 ? st.scenes.get(sg.scene).heading + " (" + Sets.label(sg.set) + ")" : "";
            b.append("Background sound ").append(where).append(": ").append(Bible.oneLine(Director.ambWords(sg))).append(", loops under the whole part\n");
        }
        Map<String, Integer> fx = new LinkedHashMap<String, Integer>();
        for (Film.Sfx s : film.sfx) {
            String n = Mixer.sfxFile(s.type);
            n = n == null ? (s.file != null ? new java.io.File(s.file).getName() : "effect") : n.replace(".ogg", "").replace('_', ' ');
            Integer k = fx.get(n);
            fx.put(n, k == null ? 1 : k + 1);
        }
        b.append("Sound effects (name × times used): ");
        int q = 0;
        for (Map.Entry<String, Integer> e : fx.entrySet()) b.append(q++ > 0 ? ", " : "").append(e.getKey()).append(" ×").append(e.getValue());
        b.append("\nMusic: follows each scene's mood (title fanfare, calm, playful, tense, sad, celebration, end chord).\n");
        for (Story.CharacterDef c : st.characters) {
            int n = 0;
            for (Film.Line l : film.lines) if (l.who == c) n++;
            if (n > 0) b.append("Voice of ").append(c.shown()).append(": ").append(n).append(" lines — ").append(Bible.voiceHint(c, false)).append("\n");
        }
        b.append("\n");

        // ---- 5. shots
        b.append("5. SHOT TABLE (").append(film.shots.size()).append(" shots)\n------------------------------------------------------------\n");
        Map<Integer, List<Film.Shot>> byLine = new LinkedHashMap<Integer, List<Film.Shot>>();
        for (Film.Shot sh : film.shots) if (sh.line >= 0 && !sh.reaction) {
            List<Film.Shot> l = byLine.get(sh.line);
            if (l == null) { l = new ArrayList<Film.Shot>(); byLine.put(sh.line, l); }
            l.add(sh);
        }
        int n = 0;
        java.util.Set<Integer> objectiveDone = new java.util.HashSet<Integer>();
        for (Film.Shot sh : film.shots) {
            n++;
            Film.Seg sg = film.segAt(sh.t + 0.01f);
            if (sg == null) continue;
            String id = String.format(Locale.US, "%03d", n);
            int sceneNo = sg.scene >= 0 && sg.scene < st.scenes.size() ? st.scenes.get(sg.scene).number : 0;
            String shotId = sh.id.length() > 0 ? sh.id : Handbook.shotId(sceneNo, n, 1);
            // who is in the frame, left to right
            Film.Cam cam = null;
            for (Film.Cam c : sg.cams) if (c.t <= sh.t + 0.01f) cam = c;
            float cx = cam == null ? 640 : cam.cx, view = 1280f / (cam == null ? 1 : cam.zoom);
            List<Film.Actor> inFrame = new ArrayList<Film.Actor>();
            for (Film.Actor a : sg.actors) {
                if (!a.stateAt(sh.t + 0.05f).visible) continue;
                float u = (Director.xAt(a, sh.t + 0.05f) - cx) / view + 0.5f;
                if (u > -0.05f && u < 1.05f) inFrame.add(a);
            }
            java.util.Collections.sort(inFrame, new java.util.Comparator<Film.Actor>() {
                public int compare(Film.Actor x, Film.Actor y) { return Float.compare(x.keys.get(0).x, y.keys.get(0).x); }
            });
            StringBuilder chars = new StringBuilder(), place = new StringBuilder(), grounding = new StringBuilder();
            for (Film.Actor a : inFrame) {
                chars.append(chars.length() > 0 ? " | " : "").append(a.c.shown()).append(" — costume: ").append(costume(a.c));
                float u = (Director.xAt(a, sh.t + 0.05f) - cx) / view + 0.5f;
                String third = u < 0.4f ? "Midground Left Third" : u > 0.6f ? "Midground Right Third" : "Midground Center";
                place.append(third).append(": ").append(a.c.shown()).append(" | ");
                grounding.append(a.c.shown()).append(" is ").append(feet(a.look)).append(", ");
            }
            if (sg.scene >= 0 && sg.scene < st.scenes.size() && !objectiveDone.contains(sg.scene)) {
                objectiveDone.add(sg.scene);
                b.append(Handbook.sceneObjective(st, st.scenes.get(sg.scene))).append("\n");
            }
            String bgText = sg.scene >= 0 ? Bible.oneLine(Bible.firstClauseOf(st.scenes.get(sg.scene).setting.length() > 0 ? st.scenes.get(sg.scene).setting : st.scenes.get(sg.scene).title)) : Sets.label(sg.set);
            String placement = "Foreground (0-1 m): " + foreground(sg.set) + " | " + place + "Background (10-100 m): " + bgText
                    + " | Depth: characters at 3 m, background 30 m | Occlusion: characters in front of the background | Ground: " + ground(sg.set);
            Film.Line line = sh.line >= 0 && sh.line < film.lines.size() ? film.lines.get(sh.line) : null;
            String action = sh.reaction ? sh.action + ", subtle breathing, one small change of expression"
                    : line != null ? sh.subject + " speaks, head almost still, only mouth and jaw move" : clean(sh.action);
            String groundingTxt = "feet firmly on the ground, shadow under feet touching the ground, scale reference: " + grounding + "gravity and weight";
            String lighting = light(sg.set, sg.tod, sh.light);
            b.append("SHOT ").append(id).append("  ").append(shotId).append(String.format(Locale.US, "   [%s, at %d:%04.1f, %.1f s → make as %d clip(s) of 3 s]%n",
                    sg.scene >= 0 ? st.scenes.get(sg.scene).heading : "", (int) (sh.t / 60), sh.t % 60, sh.dur, Math.max(1, Math.round(sh.dur / 3f))));
            b.append("PURPOSE: ").append(sh.purpose).append("   FRAMING: ").append(framing(sh.size)).append("\n");
            b.append("1. CHARACTERS: ").append(chars.length() > 0 ? chars : "none (location plate only)").append("\n");
            b.append("2. PLACEMENT: ").append(placement).append("\n");
            b.append("3. ACTION: ").append(action).append(" (one action only, less than 10% of the frame)\n");
            b.append("4. GROUNDING: ").append(groundingTxt).append("\n");
            b.append("5. LIGHTING: ").append(lighting).append("\n");
            b.append("6. CAMERA: ").append(CAMERA).append("\n");
            // the handbook's shot-prompt architecture (ch. 8), after the six mandatory fields
            String lens = Handbook.focalFor(sh.size, ar);
            String project = "shot " + shotId + " of \"" + st.title + "\", " + String.format(Locale.US, "%.1f", Math.min(sh.dur, TechnicalDirector.MAX_SHOT_SECONDS)) + " s, " + ar + ", 24 fps";
            String intention = sh.emotionalPurpose + (sh.purpose.length() > 0 ? " — " + sh.purpose : "");
            String blocking = "start: " + (place.length() > 0 ? place.toString() : "the place") + "; one action; end: the same positions, the action's end pose";
            String performance = "face: " + sh.face + (sh.body.length() > 0 ? "; body: " + sh.body : "") + "; gaze: " + Handbook.gaze(sh);
            String continuity = "the lock sheets' costumes; no identity drift; the ledger of the scene; " + Handbook.angleMeaning(sh.height) + "; lens " + lens;
            String endState = sh.cutWhen;
            b.append("7. PROJECT: ").append(project).append("\n8. INTENTION: ").append(intention).append("\n9. BLOCKING: ").append(blocking)
                    .append("\n10. PERFORMANCE: ").append(performance).append("\n11. CONTINUITY: ").append(continuity).append("\n12. END STATE: ").append(endState).append("\n");
            // lip-sync: only in a front-facing close-up, at most six words per shot
            if (line != null && !sh.reaction) {
                // exactly the words heard in this shot (at most six, timed like the film's own lip-sync shots)
                String w = sh.spoken.length() > 0 ? sh.spoken : Director.wordsIn(line, sh.t, sh.t + sh.dur);
                if (w.length() > 0) b.append("   LIP-SYNC ").append(id).append(": \"").append(w).append("\" — ").append(TechnicalDirector.LIP_SYNC)
                        .append(line.manner.length() > 0 ? ", said: " + Bible.oneLine(line.manner) : "").append("\n");
                if (sh.size < ShotPlanner.CU) b.append("   (This framing is not a close-up: make it silent and add the lip-sync in post, e.g. Wav2Lip.)\n");
            }
            String hands = sh.size >= ShotPlanner.CU ? "hands out of the frame" : TechnicalDirector.HANDS;
            boolean speech = line != null && !sh.reaction;
            String refs = inFrame.isEmpty() ? "none" : "";
            for (Film.Actor a : inFrame) refs += (refs.length() > 0 ? ", " : "") + "Lock Sheet of " + a.c.shown();
            // the ratio is a parameter: kept out of the text while it is checked, filled in last (C3)
            int fps = 24;
            for (Film.Actor a : inFrame) if (a.c.shown().equals(sh.subject)) fps = a.stepFps > 0 ? a.stepFps : PixarLead.stepFps(a.c);
            String formatLine = fmt.id + " — safe zone: " + fmt.note;
            String image = TechnicalDirector.fill(TechnicalDirector.IMAGE_TEMPLATE, "FINAL_AR", "@AR@", "SIZE", fmt.w + "x" + fmt.h, "REFERENCES", refs,
                    "FORMAT", formatLine, "FRAMING", framing(sh.size), "FOCAL", lens,
                    "CHARACTERS", chars.length() > 0 ? chars.toString() : "none", "PLACEMENT", placement, "ACTION", action, "GROUNDING", groundingTxt,
                    "LIGHTING", lighting, "HANDS", hands, "INTENTION", intention, "PERFORMANCE", performance, "CONTINUITY", continuity);
            boolean speaks = speech && sh.size >= ShotPlanner.CU;
            String video = TechnicalDirector.fill(TechnicalDirector.VIDEO_TEMPLATE, "SHOT", id, "SNAPSHOT", "first_frame_" + id, "FINAL_AR", "@AR@",
                    "DURATION", String.format(Locale.US, "%.1f", Math.min(sh.dur, TechnicalDirector.MAX_SHOT_SECONDS)), "SPEECH_FLAG", speaks ? "true" : "false",
                    "FPS", fps + (fps < 24 ? " (animated " + PixarLead.stepName(fps) + ", rendered at 24)" : ""), "FORMAT", formatLine,
                    "CHARACTERS", chars.length() > 0 ? chars.toString() : "none", "PLACEMENT", placement, "ACTION", action, "GROUNDING", groundingTxt,
                    "LIGHTING", lighting, "SPEECH", speaks ? "contains_speech = true" : "contains_speech = false",
                    "PROJECT", project, "INTENTION", intention, "BLOCKING", blocking, "PERFORMANCE", performance, "CONTINUITY", continuity, "END", endState);
            // the validation layer: every prompt is checked before it is written; what fails is corrected first
            TechnicalDirector.Shot chk = new TechnicalDirector.Shot();
            chk.id = id; chk.characters = chars.toString(); chk.placement = placement; chk.action = action; chk.grounding = groundingTxt;
            chk.lighting = lighting; chk.prompt = image + "\n" + video; chk.seconds = Math.min(sh.dur, TechnicalDirector.MAX_SHOT_SECONDS);
            chk.motion = sh.motion; chk.speech = speech; chk.closeUp = sh.size >= ShotPlanner.CU; chk.words = sh.words; chk.video = true;
            chk.fullBody = sh.size <= ShotPlanner.WIDE; chk.headTop = sh.size <= ShotPlanner.WIDE ? fmt.top : 0.1f; chk.feet = 0.92f;
            List<String> left = TechnicalDirector.correct(chk);
            if (!chk.fixes.isEmpty()) { image = clean(image); video = clean(video); }
            image = image.replace("@AR@", ar); video = video.replace("@AR@", ar);
            b.append("IMAGE PROMPT (first frame):\n").append(image).append("\n");
            b.append("VIDEO PROMPT (from the approved first frame, 3 s):\n").append(video).append("\n");
            b.append("VALIDATION: ").append(left.isEmpty() ? "passed — reference image per character, costume verbatim, layers + thirds + depth, ground contact + shadow, "
                    + "motion under 15% of the frame, static camera, aspect ratio as a parameter, " + (speech ? (chk.speech ? "lip-sync in a front close-up of at most 6 words, " : "silent (lip-sync in post), ") : "")
                    + "no morphing / static background / smooth motion, principle tags, head and feet inside the frame" : "NOT passed: " + left);
            if (!chk.fixes.isEmpty()) b.append("  (corrected: ").append(chk.fixes).append(")");
            b.append("\n\n");
        }

        // ---- 6. error correction
        b.append("6. ERROR CORRECTION\n------------------------------------------------------------\n");
        b.append("Face morphs → the motion is too big: reduce it to \"subtle breathing only\".\n");
        b.append("Background moves → add \"static background, background locked, no parallax\".\n");
        b.append("Character floats → add \"feet firmly planted, gravity, weight, shadow under feet touching ground\".\n");
        b.append("Costume changes → the costume was paraphrased: paste the exact COSTUME LOCK text.\n");
        b.append("Hands deformed → hide them: \"hands behind the back, not visible\" or a face-only close-up.\n");
        b.append("Lip-sync bad → make a silent close-up and add the lip-sync in post (Wav2Lip).\n\n");
        b.append("FINAL LAW: Pixar quality comes from 100 perfect 3-second shots, not 1 bad 60-second shot. Always think in 3-second static shots.\n\n");

        // ---- the final checklist before export, answered by what the app does
        b.append("FINAL CHECKLIST BEFORE EXPORT\n------------------------------------------------------------\n");
        String[] answers = {"yes — every plate is asked for natively at " + fmt.w + "x" + fmt.h + " and its real size is verified (within 5%)",
                "yes — one picture per character for the whole film; pictures are scaled uniformly, never stretched (face stretch check: 0%)",
                "yes — the first-frame check reframes any shot whose subject's head would touch the top (headroom " + Math.round(fmt.top * 100) + "%)",
                "yes — full shots keep the feet in the bottom 85-98% with a soft contact shadow",
                "yes — " + fmt.note,
                "yes — locked tripod in every shot, the place drawn once and held still (only its water and leaves live)",
                "yes — shape.aspect_ratio = " + ar + " is a parameter; the words are cleaned from every prompt",
                "yes — native size for each format; a different format means the director frames every shot again, never a resized copy"};
        for (int i = 0; i < PixarLead.CHECKLIST.length; i++) b.append("[x] ").append(PixarLead.CHECKLIST[i]).append("  → ").append(answers[i]).append('\n');
        b.append("\n");

        // ---- 7. thumbnail and poster (RULE_RESIZE_8: made separately, never resized from a frame)
        b.append("7. THUMBNAIL AND POSTER (made separately)\n------------------------------------------------------------\n");
        String heroName = hero == null ? "the hero" : hero.shown(), heroCostume = hero == null ? "" : costume(hero);
        b.append(PixarLead.thumbnailPrompt(heroName, heroCostume, PixarLead.paletteName(hero))).append("\n");
        b.append(PixarLead.posterPrompt(heroName, heroCostume)).append("\n");
        b.append("The app makes both itself (thumbnail.jpg 1280x720, poster.jpg 1080x1920) next to the film.\n\n");
        b.append(film.shotList);
        return b.toString();
    }
}
