package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * v38: the animated director's craft — the studio-style (Pixar / Disney tradition) directing guidance the user
 * gave as an additional guide, trained into the director for its family films. Only the film craft of that guide
 * is here: appealing, readable performances, cinematic light that follows the beat, a storytelling camera, the
 * best of the user's own pictures for every beat (and a request for the ones that are missing instead of inventing
 * them), continuity, pacing with breathing room, and a delivery with an edit list with scene markers. The guide's sexual and nude content is not trained, not recognised and never drawn (LEFT_OUT). Each
 * rule names the code that applies it; the film's shot list reports what was done for its own story.
 */
public final class FilmCraft {
    private FilmCraft() {}

    public static final String TITLE = "THE ANIMATED DIRECTOR'S CRAFT — studio-style directing for family films (v38)";

    /** {the rule, where the studio applies it}. */
    public static final String[][] RULES = {
            {"Treat every script as a production brief", "Director: characters, places, the emotional arc of each scene (ShotPlanner: establish → introduce → develop → escalate → peak → release), the camera, the light, the action beats and the sound cues are read from the script before a single frame is drawn; the shot list records each of them"},
            {"Appealing, readable characters", "Puppet, Rig: soft rounded forms, clear silhouettes, large expressive eyes with catch-lights; the user's own picture keeps its identity in every shot (Casting) — one face, one body, one skin tone from the user's pictures alone"},
            {"Light that tells the feeling — key, fill and rim", "Renderer.grade, Art rim lights: a warm key from the sun's or the flame's side, a cooler, softer fill in the shadows, a rim light along the outline (the third light of the three-point setup; PixarLead's 'two lights' are key and fill, the rim is the outline's edge) — hard and directional for conflict, soft and warm for tender moments"},
            {"Colour follows the beat", "FilmCraft.beatLook (FilmLook.at): at the peak of a scene the colour is a little richer and the contrast a touch deeper; a tender, quiet shot is a little warmer; blended in over the first half second of the shot, never a jump"},
            {"Exaggerated but believable performance", "Renderer.pose, FilmCraft.flush: the face carries the feeling of the line with Pixar-style clarity — the cheeks flush with anger, pride, laughter, embarrassment and effort, more strongly at the peak of a scene, softly in calm talk (on drawn faces the cheeks, on the user's pictures a soft glow over the cheeks); never a grotesque face"},
            {"A storytelling camera", "ShotPlanner, Director: an establishing wide shot for every new place, medium shots for conversation, closer at the peak, a slow push-in while the tension rises, a pull-back for the release, an over-the-shoulder reverse for dialogue; cuts on the line, not in the middle of a word"},
            {"Pick the best of the user's pictures for every beat", "Casting.cast: for every shot and character the picture with the right angle (front, three-quarter, side, back), the right pose (standing, sitting, walking, pointing, waving) and the feeling of the moment, ranked in that order of the guide — the feeling first, then the body and angle; decided once per shot, never per frame"},
            {"Never invent a picture the user can give — ask for it", "FilmCraft.pictureWants: the shots that wanted an angle or a feeling the user's pictures do not have are counted per character and listed in the shot list (\"PICTURES THAT WOULD HELP\") with the angle and feeling to upload; the drawn stand-in is used only until the user uploads one, and the 3D maker only after the user's pictures (SituationsGuide)"},
            {"Animated pictures only", "Library, SheetSaver: only animated, drawn or 3D-rendered pictures are taken for characters (v37); the director never imports a stranger's picture for a character"},
            {"Continuity in every shot", "FilmCraft.check: the same character keeps the same picture identity and costume (changes only when the story changes them, and staged), the speaker of every line is on the stage and facing us for most of it, sound and picture land together"},
            {"Touch only as the script writes it", "Director.activityFrom, actionFrom: a hug, a held hand or a peck on the cheek only where the script says so, between the characters it names; the director never adds one of its own (FilmCraft.check counts them)"},
            {"Pacing: build, sustain, then breathe", "ShotPlanner.breathe, PixarLead ma: after the peak of a scene a pause before the next line and a reaction shot; tension builds over the escalating lines, calm talk is cut gently; FilmCraft.check measures the breathing room after every peak line"},
            {"Sound in sync", "Director, Mixer: footsteps on the frames the feet land, effects at the moment of the action, the voice on the lips (Mixer.envelope); natural, high-fidelity sound — never comic sound effects on a serious moment"},
            {"Deliver with markers", "FilmCraft.editList, FilmJob: the film comes with an edit list with scene markers (edit_list.txt: every scene and shot with its timecode, size, camera move, beat and line) so a precise change can be asked for by shot; it can be downloaded beside the film. Subtitles are added after the film by an instruction (\"add subtitles\"), burnt into the picture"},
            {"Polish", "FilmLook: a filmic tone curve, a soft bloom, split toning and output sharpening; Renderer: clean edges, depth (Set3D depth of field outdoors), soft shadows from the sun, the moon and the flames"},
    };

    /** What of the guide is not trained, and why (said in the shot list and the protocols screen, never hidden). */
    public static final String[][] LEFT_OUT = {
            {"Sexual acts, nudity, sexual body parts, sexual vocabulary and dirty talk (the guide's §1.1, §2 in part, §3, §4 nudity tags, §5 fluids, §6 adult audio)", "Not trained: the studio makes family films; it does not recognise, draw, voice or score any of it"},
            {"Choosing or compositing pictures by nudity", "Not trained: pictures are chosen by angle, pose and feeling only"},
            {"Downloading sounds or voices from outside", "Not done without the user: the studio uses its own synthesised effects and the sounds the user adds to the library"},
            {"\"Pixar / Disney quality\"", "Not claimed: the studio follows their craft (appeal, readable faces, light, camera, pacing); its pictures are drawn and animated on a phone and are not feature-film renders"},
    };

    public static final String SUMMARY;
    static {
        StringBuilder b = new StringBuilder(TITLE).append('\n');
        for (String[] r : RULES) b.append("• ").append(r[0]).append(" — ").append(r[1]).append('\n');
        b.append("Not trained from the guide:\n");
        for (String[] r : LEFT_OUT) b.append("• ").append(r[0]).append(" — ").append(r[1]).append('\n');
        SUMMARY = b.toString();
    }

    // ------------------------------------------------------------------ the look of the beat

    /** The shot playing at t (the last one that started), or null. */
    public static Film.Shot shotAt(Film film, float t) {
        if (film == null) return null;
        Film.Shot cur = null;
        for (Film.Shot sh : film.shots) { if (sh.t <= t + 1e-4f) cur = sh; else break; }
        return cur != null && t < cur.t + cur.dur + 0.5f ? cur : null;
    }

    /** True for a tender shot: soft light and a quiet, warm feeling. */
    static boolean tender(Film.Shot sh) { return sh.light < 0.3f && sh.stage != ShotPlanner.PEAK && sh.stage != ShotPlanner.ESCALATE; }

    /**
     * The colour of the beat over the colour script: richer and a touch deeper at the peak of a scene, a little
     * warmer in a tender shot — blended in over the first half second of the shot.
     */
    public static FilmLook.Params beatLook(Film film, float t, FilmLook.Params out) {
        Film.Shot sh = shotAt(film, t);
        if (sh == null) return out;
        float k = Math.max(0, Math.min(1, (t - sh.t) / 0.5f));
        if (sh.stage == ShotPlanner.PEAK) { out.saturation += 0.06f * k; out.contrast += 0.03f * k; }
        else if (tender(sh)) out.warmth = Math.min(0.45f, out.warmth + 0.06f * k);
        return out;
    }

    // ------------------------------------------------------------------ the face

    /**
     * How strongly the cheeks flush (0..1) with a feeling at an intensity (0..1): anger, pride, laughter,
     * embarrassment, pain and effort colour the cheeks; calm talk barely; sadness and fear drain them.
     */
    public static float flush(int emotion, float intensity) {
        float base;
        switch (emotion) {
            case Pose.ANGRY: base = 0.75f; break;
            case Pose.LAUGH: base = 0.6f; break;
            case Pose.PROUD: case Pose.DETERMINED: base = 0.45f; break;
            case Pose.PAIN: base = 0.5f; break;
            case Pose.HAPPY: base = 0.3f; break;
            case Pose.WHISPER: base = 0.15f; break;
            default: return 0;
        }
        return Math.max(0, Math.min(1, base * (0.45f + 0.75f * Math.max(0, Math.min(1, intensity)))));
    }

    /** The intensity of the beat at t (the shot's stage), for the face: 0.3 calm .. 1 at the peak. */
    public static float beatIntensity(Film film, float t) {
        Film.Shot sh = shotAt(film, t);
        if (sh == null) return 0.4f;
        switch (sh.stage) {
            case ShotPlanner.PEAK: return 1f;
            case ShotPlanner.ESCALATE: return 0.75f;
            case ShotPlanner.ESTABLISH: case ShotPlanner.RELEASE: return 0.3f;
            default: return 0.5f;
        }
    }

    // ------------------------------------------------------------------ delivery

    static String srtTime(float t) {
        long ms = Math.max(0, Math.round(t * 1000.0));
        return String.format(Locale.US, "%02d:%02d:%02d,%03d", ms / 3600000, ms / 60000 % 60, ms / 1000 % 60, ms % 1000);
    }

    static String tc(float t, int fps) {
        int f = Math.max(0, Math.round(t * fps));
        return String.format(Locale.US, "%02d:%02d:%02d:%02d", f / (3600 * fps), f / (60 * fps) % 60, f / fps % 60, f % fps);
    }

    /** The edit list with scene markers: every scene, then each of its shots with timecodes, size, move, beat and line. */
    public static String editList(Film film, int fps, String title) {
        StringBuilder b = new StringBuilder();
        b.append("TITLE: ").append(title == null || title.length() == 0 ? "Kahani film" : title).append('\n');
        b.append("FCM: NON-DROP FRAME  •  ").append(fps).append(" fps  •  ").append(tc(film.duration, fps)).append(" long\n\n");
        int shotNo = 0;
        Film.Seg cur = null;
        for (Film.Shot sh : film.shots) {
            Film.Seg s = film.segAt(sh.t + 0.01f);
            if (s != cur) {
                cur = s;
                if (s != null) {
                    String name = s.text1 != null && s.text1.length() > 0 ? s.text1 : s.type == Film.S_TITLE ? "Title" : s.type == Film.S_END ? "The end" : "Part";
                    if (s.type == Film.S_SCENE && film.story != null && s.scene >= 0 && s.scene < film.story.scenes.size()) {
                        Story.Scene sc = film.story.scenes.get(s.scene);
                        String place = sc.title.length() > 0 ? sc.title : sc.setting;
                        if (place.length() > 0) name = place;
                    }
                    b.append("* MARKER ").append(tc(s.t0, fps)).append("  ")
                            .append(s.scene >= 0 ? "SCENE " + (s.scene + 1) + " — " : "").append(name).append('\n');
                }
            }
            shotNo++;
            String size = sh.size >= 0 && sh.size < ShotPlanner.SIZE_NAME.length ? ShotPlanner.SIZE_NAME[sh.size] : "";
            String move = sh.move >= 0 && sh.move < ShotPlanner.MOVE_NAME.length ? ShotPlanner.MOVE_NAME[sh.move] : "";
            String stage = sh.stage >= 0 && sh.stage < ShotPlanner.STAGE_NAME.length ? ShotPlanner.STAGE_NAME[sh.stage] : "";
            b.append(String.format(Locale.US, "%03d  %s %s  %s", shotNo, tc(sh.t, fps), tc(sh.t + sh.dur, fps), sh.id.length() > 0 ? sh.id : "V"));
            b.append("  ").append(size);
            if (move.length() > 0) b.append(", ").append(move);
            if (stage.length() > 0) b.append(", ").append(stage);
            if (sh.subject.length() > 0) b.append("  — ").append(sh.subject);
            b.append('\n');
            if (sh.line >= 0 && sh.line < film.lines.size()) {
                Film.Line ln = film.lines.get(sh.line);
                String said = ln.shown != null && ln.shown.length() > 0 ? ln.shown : ln.text;
                if (said != null && said.length() > 0) b.append("     * LINE: ").append(ln.who != null ? ln.who.shown() + ": " : "").append(Txt.withoutParens(said)).append('\n');
            } else if (sh.action.length() > 0) b.append("     * ACTION: ").append(sh.action).append('\n');
        }
        return b.toString();
    }

    // ------------------------------------------------------------------ pictures that would help

    /**
     * The angles and feelings the shots wanted of each character that the user's pictures do not have: per
     * character, "side view (4 shots)", "a sad face (2 shots)"; a character with no picture at all is asked for
     * one. Empty when the pictures cover the film.
     */
    public static List<String> pictureWants(Art art, Film film) {
        List<String> out = new ArrayList<String>();
        if (film == null) return out;
        Map<String, Map<String, Integer>> wants = new LinkedHashMap<String, Map<String, Integer>>();
        Map<String, String> names = new LinkedHashMap<String, String>();
        for (Film.Shot sh : film.shots) {
            Film.Seg s = film.segAt(sh.t + 0.01f);
            if (s == null || s.type != Film.S_SCENE) continue;
            float mid = sh.t + Math.max(0.05f, Math.min(sh.dur * 0.5f, 1.2f));
            for (Film.Actor a : s.actors) {
                if (a.c == null || a.c.voiceOnly || !a.stateAt(mid).visible) continue;
                names.put(a.c.id, a.c.shown());
                Map<String, Integer> m = wants.get(a.c.id);
                if (m == null) { m = new LinkedHashMap<String, Integer>(); wants.put(a.c.id, m); }
                Art.Sprite sp = art == null ? null : art.sprites.get(a.c.id);
                if (sp == null) { add(m, "a picture (drawn by the studio until you add one)"); continue; }
                Casting.Want w = Casting.want(film, s, sh, a, mid);
                // (a three-quarter shot is served nearly as well by the front picture: only a side or a back is asked for)
                if ((Casting.sameAngle(w.angle, Angles.SIDE) || Casting.sameAngle(w.angle, Angles.BACK)) && !hasAngle(sp, w.angle)) add(m, Angles.name(w.angle) + " view");
                if (sp.poses != null && !sp.poses.isEmpty() && w.emotion != PoseSense.NEUTRAL && w.emotion != PoseSense.NO_FACE && w.emotion != PoseSense.ASLEEP) {
                    boolean has = false;
                    for (Art.PoseSprite p : sp.poses) if (p.emotion == w.emotion) { has = true; break; }
                    if (!has) add(m, "a " + PoseSense.emotionName(w.emotion).toLowerCase(Locale.US) + " face");
                }
            }
        }
        for (Map.Entry<String, Map<String, Integer>> e : wants.entrySet()) {
            if (e.getValue().isEmpty()) continue;
            StringBuilder b = new StringBuilder(names.get(e.getKey())).append(": ");
            int i = 0;
            for (Map.Entry<String, Integer> w : e.getValue().entrySet()) {
                if (i++ > 0) b.append("; ");
                b.append(w.getKey()).append(" (").append(w.getValue()).append(w.getValue() == 1 ? " shot)" : " shots)");
            }
            out.add(b.toString());
        }
        return out;
    }

    static String joined(List<String> l) {
        StringBuilder b = new StringBuilder();
        for (String x : l) { if (b.length() > 0) b.append(", "); b.append(x); }
        return b.toString();
    }

    static void add(Map<String, Integer> m, String k) { Integer v = m.get(k); m.put(k, v == null ? 1 : v + 1); }

    static boolean hasAngle(Art.Sprite sp, float angle) {
        if (sp.poses != null) for (Art.PoseSprite p : sp.poses) if (Casting.sameAngle(p.angle, angle)) return true;
        int vi = Figure3D.viewIndex(angle);
        return vi >= 0 && sp.view(vi) != null;
    }

    // ------------------------------------------------------------------ the checklist

    /** What the checklist measured. */
    public static final class Check {
        public int lines, readable, peaks, breathed, touches, peakShots, tenderShots;
        public final List<String> notReadable = new ArrayList<String>();
    }

    /**
     * The quality and continuity checklist: the speaker of every line on the stage and facing us for most of it,
     * breathing room after every peak line, the touches (hugs, held hands, pecks) the script wrote, the shots
     * whose colour follows the beat.
     */
    public static Check check(Film film) {
        Check c = new Check();
        if (film == null) return c;
        for (int i = 0; i < film.lines.size(); i++) {
            Film.Line ln = film.lines.get(i);
            if (ln.who == null || ln.dur <= 0) continue;
            Film.Seg s = film.segAt(ln.start + 0.01f);
            if (s == null || s.type != Film.S_SCENE) continue;
            Film.Actor sp = null;
            for (Film.Actor a : s.actors) if (a.c == ln.who) sp = a;
            if (sp == null) continue;                                    // a voice from off the stage
            c.lines++;
            int seen = 0, n = 8;
            for (int k = 0; k < n; k++) {
                Film.Key key = sp.stateAt(ln.start + ln.dur * (k + 0.5f) / n);
                if (key.visible && !key.backTurned) seen++;
            }
            if (seen * 10 >= n * 6) c.readable++;
            else if (c.notReadable.size() < 5) c.notReadable.add(ln.who.shown() + " at " + srtTime(ln.start).substring(3, 8));
        }
        for (Film.Shot sh : film.shots) {
            if (sh.stage == ShotPlanner.PEAK) c.peakShots++;
            else if (tender(sh)) c.tenderShots++;
            if (sh.stage != ShotPlanner.PEAK || sh.line < 0 || sh.line >= film.lines.size()) continue;
            Film.Line ln = film.lines.get(sh.line);
            c.peaks++;
            float end = ln.start + ln.dur, next = Float.MAX_VALUE;
            for (Film.Line o : film.lines) if (o != ln && o.start >= end - 0.05f && o.start < next) next = o.start;
            Film.Seg s = film.segAt(ln.start + 0.01f);
            if (next == Float.MAX_VALUE || next - end >= 0.35f || (s != null && next >= s.t1)) c.breathed++;
        }
        for (Film.Seg s : film.segs) for (Film.Actor a : s.actors) for (Film.Act ac : a.acts)
            if (ac.type == Film.G_HUG || ac.type == Film.G_KISS || ac.type == Film.G_HOLD_HAND) c.touches++;
        return c;
    }

    /** The shot list's section: what the craft guide did for this film and what its checklist found. */
    public static String report(Art art, Film film) {
        Check c = check(film);
        StringBuilder b = new StringBuilder("\nTHE ANIMATED DIRECTOR'S CRAFT (v38 — studio-style directing for family films)\n");
        b.append(String.format(Locale.US, "Readable performances: the speaker on the stage and facing us for %d of %d line(s)%s%n", c.readable, c.lines,
                c.notReadable.isEmpty() ? "" : " — check " + joined(c.notReadable)));
        b.append(String.format(Locale.US, "Pacing: %d of %d peak line(s) followed by breathing room (a pause of 0.35 s or more, or the end of the scene)%n", c.breathed, c.peaks));
        b.append(String.format(Locale.US, "Colour by beat: %d peak shot(s) a little richer, %d tender shot(s) a little warmer; cheeks flush with anger, pride, laughter and effort%n", c.peakShots, c.tenderShots));
        b.append(String.format(Locale.US, "Touch only as written: %d hug(s), held hand(s) or peck(s), each from the script's own words%n", c.touches));
        List<String> wants = pictureWants(art, film);
        if (wants.isEmpty()) b.append("PICTURES THAT WOULD HELP: none — your pictures cover every shot\n");
        else {
            b.append("PICTURES THAT WOULD HELP (upload them and make the film again; until then the studio uses what it has):\n");
            for (String w : wants) b.append("  • ").append(w).append('\n');
        }
        b.append("Delivery: edit_list.txt (scene markers and every shot's timecode) is saved with the film; subtitles: type \"add subtitles\" after the film\n");
        b.append("Not trained from the guide: its sexual and nude content (the studio makes family films)\n");
        return b.toString();
    }
}
