package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The "trained director": turns the parsed script into a timed shooting plan.
 * Character/place descriptions are used only for looks — they are never narrated.
 * The film always opens with the title page, then "दृश्य 1", and ends with "समाप्त" / "The End".
 */
public final class Director {

    public static final class Options {
        public boolean narrator = false;     // speak stage directions aloud
        public boolean narrateTitle = false; // speak the title on the title page
        public boolean subtitles = false;    // can be switched on later from the command box
        public boolean sceneCards = false;   // "दृश्य N" cards between scenes (off: scenes flow like a film)
        public float pace = 1f;              // >1 = tighter pauses
        /**
         * The Technical Director protocol (docs/technical-director.md): locked static cameras, every change of view
         * a clean cut, shots of about 3 seconds, long lines in front-facing close-ups split into short shots, no
         * camera shake unless the story asks for one (an earthquake, thunder).
         */
        public boolean technical = true;
        /** The film's shape, decided once: "16:9", "9:16" or "1:1". Narrow frames stage characters closer together. */
        public String aspect = "16:9";
        /** The user's own sounds: effects are played where an action or direction mentions them. */
        public SoundLib sounds;
    }

    private final Map<String, Float> userSoundAt = new HashMap<String, Float>();

    /** Plays the user's own effect sound that fits this text (not the same one twice within 6 seconds). */
    private void userEffect(String text, float t) {
        if (opt.sounds == null || text == null || text.length() == 0) return;
        SoundLib.Entry e = opt.sounds.bestUser(text, "sfx");
        if (e == null) return;
        Float last = userSoundAt.get(e.path);
        if (last != null && t - last < 6) return;
        if (!userSoundAt.containsKey(e.path) && userSoundAt.size() < 12) {
            String said = text.length() > 50 ? text.substring(0, 50) + "…" : text;
            film.notes.add("🔊 Your sound \"" + e.title + "\" plays at: " + said);
        }
        userSoundAt.put(e.path, t);
        Film.Sfx x = new Film.Sfx(Film.SFX_USER, t + 0.15f, Math.max(0.8f, Math.min(8f, e.seconds > 0 ? e.seconds : 3f)), 0.75f);
        x.file = e.path;
        film.sfx.add(x);
    }

    private final Story story;
    private final Options opt;
    private Film film;
    private Art art;
    /** line index per [scene][beat], -1 if none */
    private int[][] beatLine;
    private int titleLine = -1;

    // flags that persist from scene to scene (a stolen turban stays stolen...)
    private final Map<Story.CharacterDef, Boolean> pNoHead = new HashMap<Story.CharacterDef, Boolean>();
    private final Map<Story.CharacterDef, Boolean> pTurban = new HashMap<Story.CharacterDef, Boolean>();
    private final Map<Story.CharacterDef, Boolean> pDisguise = new HashMap<Story.CharacterDef, Boolean>();
    private final Map<Story.CharacterDef, Integer> pTurbanColor = new HashMap<Story.CharacterDef, Integer>();

    public Director(Story story, Options opt) {
        this.story = story;
        this.opt = opt == null ? new Options() : opt;
        java.util.Arrays.fill(wOpen, -1);
    }

    // ================================================================== phase 1: what must be spoken

    public Film prepare() {
        film = new Film();
        film.story = story;
        film.subtitles = opt.subtitles;
        if (opt.narrateTitle && story.title.length() > 0) {
            titleLine = addLine(null, story.title, Pose.HAPPY, false, false, story.title);
        }
        beatLine = new int[story.scenes.size()][];
        for (int si = 0; si < story.scenes.size(); si++) {
            Story.Scene sc = story.scenes.get(si);
            beatLine[si] = new int[sc.beats.size()];
            boolean cave = Sets.detect(sc.setting) == Sets.CAVE_IN;
            for (int bi = 0; bi < sc.beats.size(); bi++) {
                Story.Beat b = sc.beats.get(bi);
                beatLine[si][bi] = -1;
                if (b.type == Story.Beat.DIALOGUE) {
                    int emo = emotionOf(b.manner, b.text, b.speaker);
                    boolean whisper = Txt.has(b.manner, "फुसफुसा", "whisper", "धीरे से");
                    boolean echo = cave || Txt.has(b.manner, "गूँजती", "गूंजती", "echo");
                    beatLine[si][bi] = addLine(b.speaker, Txt.forSpeech(b.text), emo, whisper, echo, b.text);
                    film.lines.get(beatLine[si][bi]).manner = b.manner;
                } else if (opt.narrator) {
                    String t = b.text.replaceFirst("^(स्थान|Location|Place)\\s*[:：]\\s*", "");
                    t = t.replaceFirst("^दृश्य बदलता है\\s*[:：]\\s*", "");
                    beatLine[si][bi] = addLine(null, Txt.forSpeech(Txt.withoutParens(t)), Pose.NEUTRAL, false, false, t);
                }
            }
        }
        return film;
    }

    private int addLine(Story.CharacterDef who, String text, int emo, boolean whisper, boolean echo, String shown) {
        Film.Line l = new Film.Line();
        l.index = film.lines.size();
        l.who = who;
        l.text = text;
        l.shown = story.shown(shown);
        l.hindi = text == null || text.trim().length() == 0 ? story.hindi : Txt.mostlyHindi(text);
        l.emotion = emo;
        l.whisper = whisper;
        l.echo = echo;
        l.dur = estimate(text);
        film.lines.add(l);
        return l.index;
    }

    /** Rough speaking time, used if the speech engine fails. */
    public static float estimate(String text) {
        return 0.6f + text.length() * 0.075f;
    }

    public static int emotionOf(String manner, String text, Story.CharacterDef who) {
        String m = manner == null ? "" : manner;
        boolean villain = who != null && who.look != null && !who.look.hero;
        if (Txt.has(m, "क्रूर", "कुटिल", "दुष्ट", "evil", "wicked")) return Pose.EVIL;
        if (Txt.has(m, "चीख", "चिल्ला", "दर्द", "scream")) return Pose.PAIN;
        if (Txt.has(m, "गुस्स", "दहाड़", "क्रोध", "angry", "roar")) return Pose.ANGRY;
        if (Txt.has(m, "रोते", "रोकर", "रो रहे", "रोती", "फूट-फूट", "cry", "sob")) return Pose.SAD;
        if (Txt.has(m, "हँस", "हंस", "खिलखिला", "laugh") || Txt.has(text, "हा हा", "ही ही", "ही-ही")) return villain ? Pose.EVIL : Pose.LAUGH;
        if (Txt.has(m, "डर", "घबरा", "सहम", "काँप", "कांप", "झिझक", "scared", "afraid")) return Pose.SCARED;
        if (Txt.has(m, "खाँस", "खांस", "चक्कर", "dizzy")) return Pose.DIZZY;
        if (Txt.has(m, "फुसफुसा", "whisper")) return Pose.WHISPER;
        if (Txt.has(m, "गर्व", "सीना", "proud")) return Pose.PROUD;
        if (Txt.has(m, "दृढ़", "गंभीर", "आत्मविश्वास", "विश्वास", "तेज़ आवाज़", "determined", "firm")) return Pose.DETERMINED;
        if (Txt.has(m, "मासूम", "उत्सुक", "आँखें बड़ी", "curious", "innocent")) return Pose.CURIOUS;
        if (Txt.has(m, "मुस्कुरा", "मुस्कान", "प्यार", "मीठी", "खुशी", "smil", "happy")) return villain ? Pose.EVIL : Pose.HAPPY;
        if (Txt.has(m, "भारी आवाज़", "गूँजती") && villain) return Pose.ANGRY;
        return Pose.NEUTRAL;
    }

    // ================================================================== phase 2: timing & staging

    /** Call after the voices are synthesised (line.dur and line.env filled). */
    public Film direct(Art art) {
        this.art = art == null ? new Art() : art;
        if (film.subtitles) { /* keep */ }
        float t = 0;

        // ---------------- title page
        Film.Seg title = new Film.Seg();
        title.type = Film.S_TITLE;
        title.t0 = 0;
        title.text1 = story.title;
        title.text2 = story.subtitle;
        title.backdrop = this.art.title;
        float tdur = 7.5f;
        if (titleLine >= 0) {
            Film.Line l = film.lines.get(titleLine);
            l.start = 2.2f;
            tdur = Math.max(tdur, l.start + l.dur + 2.2f);
        }
        title.t1 = tdur;
        title.fadeIn = 0.8f;
        title.fadeOut = 0.7f;
        film.segs.add(title);
        film.music.add(new Film.Music(Film.M_TITLE, 0, tdur));
        film.sfx.add(new Film.Sfx(Film.SFX_FANFARE, 0.3f, 4.5f, 0.9f));
        film.sfx.add(new Film.Sfx(Film.SFX_MAGIC, 1.0f, 2f, 0.5f));
        film.notes.add("Title page: \"" + story.title + "\"" + (this.art.title != null ? " (your picture)" : " (made by the studio)") + " + music");
        t = tdur;

        // ---------------- scenes
        for (int si = 0; si < story.scenes.size(); si++) {
            Story.Scene sc = story.scenes.get(si);
            if (!opt.sceneCards) {
                // no title cards: a soft "whoosh" transition into the next scene
                if (si > 0) film.sfx.add(new Film.Sfx(Film.SFX_WHOOSH_CARD, t - 0.3f, 1.0f, 0.35f));
            } else {
            Film.Seg card = new Film.Seg();
            card.type = Film.S_CARD;
            card.scene = si;
            card.t0 = t;
            card.t1 = t + 3.0f;
            card.text1 = sc.heading;
            card.text2 = sc.title;
            card.set = Sets.detect(firstSentence(sc.setting));
            card.tod = Sets.detectTime(sc.setting, Sets.DAY);
            card.backdrop = this.art.sceneBackdrop(sc.number, 0);
            card.fadeIn = 0.4f; card.fadeOut = 0.4f;
            film.segs.add(card);
            film.sfx.add(new Film.Sfx(Film.SFX_WHOOSH_CARD, t + 0.2f, 1.2f, 0.7f));
            film.sfx.add(new Film.Sfx(Film.SFX_CHIME, t + 0.5f, 1.6f, 0.5f));
            t = card.t1;
            }

            // split into parts at "दृश्य बदलता है"
            List<int[]> parts = new ArrayList<int[]>();
            int start = 0;
            for (int bi = 0; bi < sc.beats.size(); bi++) {
                Story.Beat b = sc.beats.get(bi);
                if (bi > 0 && b.type == Story.Beat.DIRECTION && Txt.has(b.text, "दृश्य बदल", "स्थान बदल", "scene changes", "cut to")) {
                    parts.add(new int[]{start, bi});
                    start = bi;
                }
            }
            parts.add(new int[]{start, sc.beats.size()});
            int prevTod = Sets.DAY;
            for (int pi = 0; pi < parts.size(); pi++) {
                int[] pr = parts.get(pi);
                String where = pi == 0 ? (sc.setting.length() > 0 ? sc.setting : sc.title) : sc.beats.get(pr[0]).text;
                if (pi == 0 && parts.size() > 1) {
                    // "गुफा और फिर रत्नगढ़ का महल" -> first part is the cave mouth
                    String fs = firstSentence(where);
                    int fir = Txt.firstIndex(fs, "और फिर", "then");
                    if (fir > 0) where = fs.substring(0, fir);
                }
                t = stagePart(si, sc, pi, parts.size(), pr[0], pr[1], where, prevTod, t);
                prevTod = film.segs.get(film.segs.size() - 1).tod;
            }
        }

        // ---------------- ending
        Film.Seg end = new Film.Seg();
        end.type = Film.S_END;
        end.t0 = t;
        end.t1 = t + 8f;
        end.text1 = story.hindi ? "समाप्त" : "The End";
        end.text2 = story.title;
        end.backdrop = this.art.end;
        end.fadeIn = 0.8f; end.fadeOut = 1.2f;
        film.segs.add(end);
        film.music.add(new Film.Music(Film.M_END, t, end.t1));
        film.sfx.add(new Film.Sfx(Film.SFX_END_CHORD, t + 0.4f, 5f, 0.9f));
        film.sfx.add(new Film.Sfx(Film.SFX_MAGIC, t + 0.6f, 2f, 0.5f));
        film.notes.add("End page: \"" + end.text1 + "\"" + (this.art.end != null ? " (your picture)" : " (made by the studio)") + " + music");
        film.duration = end.t1;
        scoreMusic();
        if (opt.technical) {
            // the Technical Director protocol, in its own order: lock the camera, then no shot may move a character
            // more than 15% of the frame, hold two actions, or last longer than 4 seconds
            calmMoves();
            lockCameras();
            enforceMotion();
            oneActionPerShot();
            limitShotLength(TechnicalDirector.MAX_SHOT_SECONDS);
        }
        film.shotList = qualityCheck();
        return film;
    }

    /**
     * Technical Director protocol: the camera is a locked tripod. A move planned right after a cut becomes that
     * shot's framing; any later move becomes a clean cut; every shot holds perfectly still.
     */
    private void lockCameras() {
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            java.util.Collections.sort(sg.cams, new java.util.Comparator<Film.Cam>() {
                public int compare(Film.Cam a, Film.Cam b) { return Float.compare(a.t, b.t); }
            });
            List<Film.Cam> out = new ArrayList<Film.Cam>();
            for (Film.Cam c : sg.cams) {
                Film.Cam last = out.isEmpty() ? null : out.get(out.size() - 1);
                if (c.ease > 0 && last != null && c.t - last.t < 0.7f) {
                    // the move's goal is the shot: frame it so from the cut on
                    last.cx = c.cx; last.cy = c.cy; last.zoom = c.zoom; last.roll = c.roll; last.angle = c.angle;
                    if (c.light >= 0) last.light = c.light;
                    continue;
                }
                c.ease = 0;
                c.still = true;
                out.add(c);
            }
            for (Film.Cam c : out) c.still = true;
            sg.cams.clear();
            sg.cams.addAll(out);
        }
        for (Film.Shot sh : film.shots) sh.move = ShotPlanner.STATIC;
    }

    /**
     * No shot runs longer than about four seconds: a long hold is cut in two or three, alternating the framing
     * with a tighter one on the same subject (a cut-in) and back, each still.
     */
    private void limitShotLength(float max) {
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            List<Film.Cam> add = new ArrayList<Film.Cam>();
            for (int i = 0; i < sg.cams.size(); i++) {
                Film.Cam c = sg.cams.get(i);
                float next = i + 1 < sg.cams.size() ? sg.cams.get(i + 1).t : sg.t1;
                if (next - c.t <= max) continue;
                int n = (int) Math.ceil((next - c.t) / TechnicalDirector.SHOT_SECONDS);
                float step = (next - c.t) / n;
                for (int k = 1; k < n; k++) {
                    boolean tight = k % 2 == 1;
                    // the second framing of a long hold: closer for a wider shot; for a close-up a touch wider (a
                    // close-up never gets tighter here, so a head, hair or a turban is never cut)
                    boolean close = c.zoom >= ShotPlanner.ZOOM[ShotPlanner.CU] * 0.95f;
                    float z2 = close ? Math.max(1f, c.zoom / 1.18f) : Math.min(Math.max(ShotPlanner.MAX_ZOOM, c.zoom), c.zoom * 1.2f);
                    float y2 = close ? c.cy : c.cy - 14 / c.zoom;
                    Film.Cam d = new Film.Cam(c.t + k * step, c.cx, tight ? y2 : c.cy, tight ? z2 : c.zoom, 0);
                    d.still = true; d.roll = c.roll; d.angle = c.angle; d.light = c.light;
                    add.add(d);
                }
            }
            sg.cams.addAll(add);
            java.util.Collections.sort(sg.cams, new java.util.Comparator<Film.Cam>() {
                public int compare(Film.Cam a, Film.Cam b) { return Float.compare(a.t, b.t); }
            });
        }
    }

    /** Who is in the middle of a framing at time t (for shots the director did not plan line by line). */
    private static Film.Actor nearestTo(Film.Seg sg, float cx, float t) {
        Film.Actor best = null;
        float bd = 1e9f;
        for (Film.Actor a : sg.actors) {
            if (!a.stateAt(t).visible) continue;
            float d = Math.abs(xAt(a, t) - cx);
            if (d < bd) { bd = d; best = a; }
        }
        return best;
    }

    /**
     * The shot list follows the film exactly: every cut is a shot. Shots the planner decided keep their reasons;
     * the others (staging of the action, cut-ins that keep shots short) are described from what is on screen.
     */
    private void shotsFromCuts() {
        List<Film.Shot> planned = new ArrayList<Film.Shot>(film.shots);
        film.shots.clear();
        int part = -1;
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            part++;
            for (int i = 0; i < sg.cams.size(); i++) {
                Film.Cam c = sg.cams.get(i);
                if (c.ease > 0) continue;
                float next = sg.t1;
                for (int j = i + 1; j < sg.cams.size(); j++) if (sg.cams.get(j).ease == 0) { next = sg.cams.get(j).t; break; }
                Film.Shot match = null;
                for (Film.Shot p : planned) if (Math.abs(p.t - c.t) < 0.25f) { match = p; break; }
                Film.Shot sh = match;
                if (sh == null) {
                    sh = new Film.Shot();
                    sh.t = c.t;
                    sh.size = ShotPlanner.sizeOf(c.zoom);
                    sh.height = c.angle;
                    Film.Actor who = nearestTo(sg, c.cx, c.t + 0.1f);
                    sh.subject = who == null ? Sets.label(sg.set) : who.c.shown();
                    Film.Sub talk = null;
                    for (Film.Sub sb : sg.subs) if (c.t + 0.1f >= sb.t0 && c.t + 0.1f < sb.t1) talk = sb;
                    sh.type = sh.size <= ShotPlanner.MWIDE ? ShotPlanner.TWO_SHOT : ShotPlanner.SINGLE;
                    if (talk != null) for (Film.Line fl : film.lines) if (fl.start <= c.t + 0.1f && fl.start + fl.dur > c.t + 0.1f) {
                        sh.line = fl.index;
                        // a cut inside a line spoken in close-up is still a lip-sync shot: its share of the words
                        if (c.zoom >= ShotPlanner.ZOOM[ShotPlanner.CU] * 0.95f) {
                            sh.speech = true;
                            sh.spoken = wordsIn(fl, c.t, next);
                            sh.words = sh.spoken.isEmpty() ? 0 : sh.spoken.split("\\s+").length;
                        }
                        break;
                    }
                    sh.purpose = talk != null ? "A cut-in / cut-out on the speaker so no shot is longer than about 3 seconds"
                            : sh.size <= ShotPlanner.MWIDE ? "Follow the action: everyone who moves stays in the frame" : "Show what " + sh.subject + " does";
                    sh.action = talk != null ? (talk.who.length() > 0 ? talk.who + ": \"" + clip(talk.text, 40) + "\"" : clip(talk.text, 50))
                            : (who == null ? "the place" : who.c.shown() + (who.stateAt(c.t + 0.1f).moveDur > 0 ? " moves" : " in the scene"));
                    sh.face = who == null ? "—" : faceOf(who.stateAt(c.t + 0.1f).emotion);
                    sh.cutWhen = "the action moves on";
                    sh.emotionalPurpose = "Keep the story readable";
                    sh.sound = "Ambience of " + Sets.label(sg.set);
                    sh.light = c.light < 0 ? 0.4f : c.light;
                }
                sh.part = part;
                sh.t = c.t;
                sh.dur = Math.max(0.3f, next - c.t);
                sh.move = c.ease > 0 ? ShotPlanner.DRIFT : ShotPlanner.STATIC;
                film.shots.add(sh);
            }
        }
    }

    // ------------------------------------------------------------------ the director checks the film (§48)

    /**
     * Looks over every planned shot like the guide's quality-control loop, fixes what it can and writes the
     * shot list: jump cuts become smooth reframes, every part opens on a wide shot, close-ups stay rare, strong
     * performances keep a still camera, reactions get time, and the pacing of each part is measured.
     */
    private String qualityCheck() {
        int jump = 0, estab = 0, still = 0;
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            java.util.Collections.sort(sg.cams, new java.util.Comparator<Film.Cam>() {
                public int compare(Film.Cam a, Film.Cam b) { return Float.compare(a.t, b.t); }
            });
            // every part starts readable and wide (§2)
            if (sg.cams.isEmpty() || sg.cams.get(0).t > sg.t0 + 1.0f || sg.cams.get(0).zoom > 1.3f) {
                Film.Cam w = new Film.Cam(sg.t0, 640, 372, 1.08f, 0);
                sg.cams.add(0, w);
                estab++;
            }
            Film.Cam prevCut = null;
            List<Film.Cam> drop = new ArrayList<Film.Cam>();
            for (Film.Cam c : sg.cams) {
                if (c.ease > 0) continue;
                // a cut to almost the same framing jars (a jump cut): keep the shot running instead
                // (with a locked camera), or glide there
                if (!c.keep && prevCut != null && Math.abs(c.zoom - prevCut.zoom) < 0.1f && Math.abs(c.cx - prevCut.cx) < 55 && Math.abs(c.cy - prevCut.cy) < 40
                        && c.t - prevCut.t > 0.3f) { if (opt.technical) drop.add(c); else c.ease = 0.8f; jump++; }
                else prevCut = c;
            }
            sg.cams.removeAll(drop);
        }
        if (opt.technical) {
            // the protocol's passes again until nothing changes (a reframed shot has a different frame width)
            for (int pass = 0; pass < 3; pass++) {
                int before = motionCuts + actionCuts;
                limitShotLength(TechnicalDirector.MAX_SHOT_SECONDS);
                safeFrames();
                enforceMotion();
                oneActionPerShot();
                if (motionCuts + actionCuts == before) break;
            }
            limitShotLength(TechnicalDirector.MAX_SHOT_SECONDS);
            safeFrames();
            // the last word goes to the motion and action limits (their new shots are framed on the action)
            enforceMotion();
            oneActionPerShot();
            limitShotLength(TechnicalDirector.MAX_SHOT_SECONDS);
        }
        shotsFromCuts();
        if (opt.technical) validateShots();
        int longest = 0;
        for (Film.Shot sh : film.shots) {
            if (!opt.technical && !sh.reaction && sh.stage == ShotPlanner.PEAK && sh.move != ShotPlanner.STATIC && sh.move != ShotPlanner.PUSH_IN) { sh.move = ShotPlanner.STATIC; still++; }
            if (sh.dur > TechnicalDirector.MAX_SHOT_SECONDS + 0.05f) longest++;
        }
        StringBuilder b = new StringBuilder();
        b.append("DIRECTOR'S SHOT LIST — ").append(story.title).append('\n');
        b.append("Planned with the Pixar-style directing guide: emotion → performance → composition → camera → light → sound → cut.\n\n");
        int n = 0, cus = 0, reactions = 0, statics = 0, twos = 0;
        int part = -2;
        for (Film.Shot sh : film.shots) {
            if (sh.part != part) {
                part = sh.part;
                Film.Seg sg = film.segAt(sh.t + 0.01f);
                if (sg != null && sg.scene >= 0) b.append("────────── ").append(story.scenes.get(sg.scene).heading).append(": ").append(Sets.label(sg.set))
                        .append(sg.transition == 1 ? "  (opens with a dip to black: time has passed)" : sg.transition == 2 ? "  (opens with a dip to white)" : "").append('\n');
            }
            n++;
            if (sh.size >= ShotPlanner.CU) cus++;
            if (sh.reaction) reactions++;
            if (sh.move == ShotPlanner.STATIC) statics++;
            if (sh.type == ShotPlanner.TWO_SHOT) twos++;
            b.append(String.format(java.util.Locale.US, "SHOT %03d   at %d:%04.1f\n", n, (int) (sh.t / 60), sh.t % 60));
            b.append("PURPOSE: ").append(sh.purpose).append('\n');
            b.append("SHOT SIZE: ").append(ShotPlanner.SIZE_NAME[sh.size]).append(" (").append(ShotPlanner.TYPE_NAME[sh.type]).append(")\n");
            b.append("CAMERA HEIGHT: ").append(sh.height > 0 ? "Low angle (power)" : sh.height < 0 ? "High angle (vulnerable)" : "Eye level").append('\n');
            b.append("CAMERA MOVEMENT: ").append(ShotPlanner.MOVE_NAME[sh.move]).append('\n');
            if (sh.other.length() > 0) b.append("COMPOSITION: ").append(sh.other).append('\n');
            b.append("CHARACTER ACTION: ").append(sh.action).append('\n');
            b.append("FACIAL PERFORMANCE: ").append(sh.face).append('\n');
            if (sh.body.length() > 0) b.append("BODY LANGUAGE: ").append(sh.body).append('\n');
            b.append("LIGHTING: ").append(sh.light >= 0.7f ? "Harder, directional light (conflict, fear)" : sh.light <= 0.25f ? "Soft, warm light (warmth, safety)" : "Natural light of the place").append('\n');
            b.append("FOCUS: ").append(sh.subject.length() > 0 ? sh.subject : "the place").append('\n');
            b.append("SOUND: ").append(sh.sound.length() > 0 ? sh.sound : "music and ambience").append('\n');
            b.append(String.format(java.util.Locale.US, "DURATION: %.1f s%n", sh.dur));
            b.append("CUT WHEN: ").append(sh.cutWhen).append('\n');
            b.append("EMOTIONAL PURPOSE: ").append(sh.emotionalPurpose).append("\n\n");
        }
        b.append("QUALITY CHECK\n");
        b.append("• Every part opens on a readable wide shot: ").append(estab == 0 ? "yes" : "fixed " + estab).append('\n');
        b.append("• Jump cuts (a cut to almost the same framing): ").append(jump == 0 ? "none" : jump + (opt.technical ? " removed (the shot simply continues)" : " turned into smooth reframes")).append('\n');
        if (opt.technical) {
            int speech = 0, over6 = 0, overMotion = 0, multi = 0;
            for (Film.Shot sh : film.shots) {
                if (sh.speech) { speech++; if (sh.words > TechnicalDirector.LIP_SYNC_WORDS) over6++; }
                if (sh.motion >= TechnicalDirector.MAX_MOTION) overMotion++;
                if (sh.actions > 1) multi++;
            }
            b.append("• Technical Director protocol: locked tripod, every shot static, no camera shake unless the story asks for it\n");
            b.append(String.format(java.util.Locale.US, "• Shots longer than 4 s: %d%n", longest));
            b.append(String.format(java.util.Locale.US, "• Lip-sync shots (front close-ups, face filling the frame, at most 6 words): %d; with more than 6 words: %d%n", speech, over6));
            b.append(String.format(java.util.Locale.US, "• Runs slowed to walking pace: %d; characters who stop walking to speak: %d%n", calmed, stillToSpeak));
            b.append(String.format(java.util.Locale.US, "• Cuts made so no character moves 15%% of the frame in one shot: %d; shots still over the limit (very fast moves the story's timing leaves no room to slow): %d%n", motionCuts, overMotion));
            b.append(String.format(java.util.Locale.US, "• Cuts made so each shot holds one action: %d; shots with more than one: %d%n", actionCuts, multi));
            b.append(String.format(java.util.Locale.US, "• Shots reframed for the %s frame (whole characters, 15%% side margins, headroom): %d%n", opt.aspect, framed));
            b.append(String.format(java.util.Locale.US, "• Validation layer: %d of %d shots passed at once, %d corrected%s%n", passed, n, corrected,
                    stillWrong.isEmpty() ? "" : "; could not fully correct: " + stillWrong));
        } else {
            b.append(String.format(java.util.Locale.US, "• Close-ups kept for turning points: %d of %d shots (%.0f%%)%n", cus, n, n == 0 ? 0 : 100f * cus / n));
        }
        b.append("• Static shots: ").append(statics).append(" of ").append(n).append(still > 0 ? " (" + still + " made still)" : "").append('\n');
        b.append("• Reactions shown and allowed to breathe: ").append(reactions).append('\n');
        b.append("• Relationships shown with both characters in the frame: ").append(twos).append(" two-shots\n");
        float spoken = 0;
        for (Film.Seg sg : film.segs) if (sg.type == Film.S_SCENE) spoken += sg.t1 - sg.t0;
        b.append(String.format(java.util.Locale.US, "• Pacing: %d shots in %.0f s of scenes, an average shot of %.1f s%n", n, spoken, n == 0 ? 0 : spoken / n));
        return b.toString();
    }

    // ------------------------------------------------------------------ the film score

    /** Mood a single line asks for, given the scene's mood. */
    private static int lineMood(Film.Line l, int base) {
        boolean villain = l.who != null && l.who.look != null && !l.who.look.hero;
        switch (l.emotion) {
            case Pose.EVIL: return Film.M_VILLAIN;
            case Pose.ANGRY: return villain ? Film.M_VILLAIN : base == Film.M_ACTION ? Film.M_ACTION : Film.M_TENSE;
            case Pose.SCARED: case Pose.PAIN: return Film.M_TENSE;
            case Pose.SAD: return Film.M_SAD;
            case Pose.LAUGH: case Pose.HAPPY: return base == Film.M_CELEBRATE ? base : base == Film.M_HAPPY ? Film.M_HAPPY : Film.M_PLAYFUL;
            case Pose.DETERMINED: case Pose.PROUD: return base == Film.M_TENSE || base == Film.M_VILLAIN ? Film.M_ACTION : base;
            default: return villain && base != Film.M_CELEBRATE ? Film.M_TENSE : base;
        }
    }

    static float moodLevel(int m) {
        switch (m) {
            case Film.M_ACTION: case Film.M_CELEBRATE: case Film.M_TITLE: return 1f;
            case Film.M_TENSE: case Film.M_VILLAIN: return 0.9f;
            case Film.M_PLAYFUL: return 0.85f;
            case Film.M_SAD: case Film.M_END: return 0.8f;
            case Film.M_NIGHT: return 0.7f;
            default: return 0.72f;
        }
    }

    /**
     * Turns one music cue per scene into a real film score: the music follows what happens inside the scene
     * (a villain speaks -> menacing, someone is scared -> tense, laughter -> playful, sorrow -> soft sad piano),
     * swells before shocks, dips under whispers, and short drum hits mark surprises.
     */
    private void scoreMusic() {
        List<Film.Music> out = new ArrayList<Film.Music>();
        for (Film.Music m : film.music) {
            if (m.mood == Film.M_TITLE || m.mood == Film.M_END) { shape(m, null); out.add(m); continue; }
            // lines inside this cue, in time order
            List<Film.Line> ls = new ArrayList<Film.Line>();
            for (Film.Line l : film.lines) if (l.start >= m.t0 && l.start < m.t1 && l.dur > 0) ls.add(l);
            java.util.Collections.sort(ls, new java.util.Comparator<Film.Line>() {
                public int compare(Film.Line a, Film.Line b) { return Float.compare(a.start, b.start); }
            });
            // mood regions
            List<float[]> regions = new ArrayList<float[]>();   // {t0, t1, mood}
            float cur = m.t0;
            int mood = m.mood;
            for (Film.Line l : ls) {
                int lm = lineMood(l, m.mood);
                if (lm != mood) {
                    regions.add(new float[]{cur, Math.max(cur, l.start - 0.6f), mood});
                    cur = Math.max(cur, l.start - 0.6f);
                    mood = lm;
                }
            }
            regions.add(new float[]{cur, m.t1, mood});
            // short regions are merged into the one before (music should not jump every line)
            List<float[]> merged = new ArrayList<float[]>();
            for (float[] r : regions) {
                if (r[1] - r[0] < 0.05f) continue;
                if (!merged.isEmpty()) {
                    float[] last = merged.get(merged.size() - 1);
                    if (last[2] == r[2] || r[1] - r[0] < 7f) { last[1] = r[1]; continue; }
                    if (last[1] - last[0] < 7f) { last[1] = r[1]; last[2] = r[2]; continue; }
                }
                merged.add(new float[]{r[0], r[1], r[2]});
            }
            for (int i = 0; i < merged.size(); i++) {
                float[] r = merged.get(i);
                // overlap neighbours by 1.5 s so one mood crossfades into the next
                Film.Music c = new Film.Music((int) r[2], i == 0 ? r[0] : r[0] - 0.75f, i == merged.size() - 1 ? r[1] : r[1] + 0.75f);
                shape(c, ls);
                out.add(c);
            }
            // drum hit before surprises and a villain's first menacing line
            boolean villainHeard = false;
            for (Film.Line l : ls) {
                boolean villain = l.who != null && l.who.look != null && !l.who.look.hero;
                if (l.emotion == Pose.SURPRISED || (villain && !villainHeard && (l.emotion == Pose.EVIL || l.emotion == Pose.ANGRY))) {
                    film.sfx.add(new Film.Sfx(Film.SFX_DRUMS, Math.max(m.t0, l.start - 0.45f), 0.9f, 0.4f));
                }
                if (villain) villainHeard = true;
            }
        }
        film.music.clear();
        film.music.addAll(out);
    }

    /** Loudness curve of one cue: in-scene swells and dips around the lines. */
    private void shape(Film.Music m, List<Film.Line> ls) {
        float base = moodLevel(m.mood);
        m.soft = m.mood == Film.M_SAD || m.mood == Film.M_NIGHT;
        List<float[]> pts = new ArrayList<float[]>();
        pts.add(new float[]{m.t0, base});
        if (ls != null) for (Film.Line l : ls) {
            if (l.start < m.t0 || l.start >= m.t1) continue;
            float lv = base;
            if (l.emotion == Pose.SURPRISED || l.emotion == Pose.SCARED || l.emotion == Pose.ANGRY || l.emotion == Pose.EVIL) lv = Math.min(1.3f, base + 0.25f);
            else if (l.whisper || l.emotion == Pose.WHISPER) lv = base * 0.6f;
            else if (l.emotion == Pose.SAD) lv = base * 0.85f;
            if (lv == base) continue;
            pts.add(new float[]{Math.max(m.t0, l.start - 1.2f), base});
            pts.add(new float[]{l.start, lv});
            pts.add(new float[]{l.start + l.dur, lv});
            pts.add(new float[]{Math.min(m.t1, l.start + l.dur + 1.5f), base});
        }
        pts.add(new float[]{m.t1, base});
        java.util.Collections.sort(pts, new java.util.Comparator<float[]>() {
            public int compare(float[] a, float[] b) { return Float.compare(a[0], b[0]); }
        });
        m.envT = new float[pts.size()];
        m.envV = new float[pts.size()];
        for (int i = 0; i < pts.size(); i++) { m.envT[i] = pts.get(i)[0]; m.envV[i] = pts.get(i)[1]; }
    }

    static String firstSentence(String s) {
        int i = s.indexOf('।');
        if (i < 0) i = s.indexOf(". ");
        return i > 0 ? s.substring(0, i) : s;
    }

    // ------------------------------------------------------------------ staging one part of a scene

    private Film.Seg seg;
    private float ground;
    private boolean estab;

    /** Time for "state" facts (sitting, caged, hiding): the start of the part when it is the opening description. */
    private float ts(float t) { return estab ? seg.t0 : t; }
    private Story.CharacterDef lastSubject;
    private List<Story.CharacterDef> lastGroup = new ArrayList<Story.CharacterDef>();
    /** The shot planner's plan for each dialogue beat of the part being staged. */
    private final Map<Integer, ShotPlanner.Plan> partPlan = new HashMap<Integer, ShotPlanner.Plan>();
    private int partNo = -1;
    private boolean partFast;
    /** The last dialogue shot (to hold a framing over several calm lines instead of cutting). */
    private Film.Shot lastDlgShot;
    private Film.Actor lastDlgA, lastDlgB;
    private int dlgCount;
    private Film.Actor lastSpeaker;

    private boolean leading;

    private float stagePart(int si, Story.Scene sc, int pi, int nParts, int b0, int b1, String where, int prevTod, float t) {
        leading = true;
        seg = new Film.Seg();
        seg.type = Film.S_SCENE;
        seg.scene = si;
        seg.t0 = t;
        seg.set = Sets.detect(firstSentence(where));
        if (seg.set == Sets.GENERIC_OUT) seg.set = Sets.detect(where);
        seg.tod = Sets.detectTime(where, pi == 0 && si == 0 ? Sets.MORNING : prevTod == Sets.NIGHT && seg.set != Sets.CAVE_IN ? Sets.NIGHT : Sets.DAY);
        if (seg.set == Sets.CAVE_IN) seg.tod = Sets.NIGHT;
        seg.festive = Txt.has(where, "सजा", "रोशनियों", "ढोल", "उत्सव", "जश्न", "celebrat", "festiv");
        if (seg.festive && seg.set != Sets.CAVE_IN) seg.set = Sets.CELEBRATION;
        seg.backdrop = nParts > 1 ? art.sceneBackdrop(sc.number, pi) : art.sceneBackdrop(sc.number, 0);
        if (nParts == 1 && seg.backdrop == null) seg.backdrop = art.scenes.get(String.valueOf(sc.number));
        seg.ground = seg.backdrop != null ? seg.backdrop.ground * 720f : Sets.GROUND;
        ground = seg.ground;
        seg.fadeIn = 0.45f; seg.fadeOut = 0.45f;
        film.segs.add(seg);
        lastSubject = null;
        lastGroup.clear();
        dlgCount = 0;
        lastSpeaker = null;
        lastDlgShot = null;

        // ---------- who is in this part, and who arrives later
        List<Story.CharacterDef> order = new ArrayList<Story.CharacterDef>();
        Map<Story.CharacterDef, Integer> entryBeat = new HashMap<Story.CharacterDef, Integer>();
        String voiceFromOutside = Txt.has(where, "बाहर से", "आवाज़ आती", "from outside") ? "y" : "";
        for (int bi = b0; bi < b1; bi++) {
            Story.Beat b = sc.beats.get(bi);
            if (b.type == Story.Beat.DIALOGUE) {
                Story.CharacterDef c = b.speaker;
                if (c != null && !order.contains(c)) {
                    order.add(c);
                    boolean far = Txt.has(b.manner, "दूर से");
                    if ((voiceFromOutside.length() > 0 && bi > b0 && !mentionsAny(sc, b0, bi, c)) || far) entryBeat.put(c, bi);
                }
                // characters named in the manner (e.g. "वृंदा की तलवार को रोकते हुए") are on stage too
                for (Story.CharacterDef m : ScriptParser.mentions(story, b.manner)) if (!order.contains(m)) order.add(m);
                // ...and so is anyone addressed by name ("और कृपा, तुम्हारी चतुराई...")
                for (Story.CharacterDef m : vocatives(b.text)) if (!order.contains(m)) order.add(m);
            } else {
                String txt = b.text;
                for (String sent : sentences(txt)) {
                    List<Story.CharacterDef> ms = presentWithGroups(sent);
                    for (int k = 0; k < ms.size(); k++) {
                        Story.CharacterDef m = ms.get(k);
                        if (order.contains(m)) continue;
                        order.add(m);
                        if (bi > b0 && isEntry(sent)) entryBeat.put(m, bi);
                    }
                }
            }
        }

        // ---------- layout: heroes from the left, villains on the right
        List<Story.CharacterDef> heroes = new ArrayList<Story.CharacterDef>(), villains = new ArrayList<Story.CharacterDef>();
        for (Story.CharacterDef c : order) (c.look.hero ? heroes : villains).add(c);
        java.util.Collections.sort(villains, new java.util.Comparator<Story.CharacterDef>() {
            public int compare(Story.CharacterDef a, Story.CharacterDef b) { return Float.compare(a.look.height, b.look.height); }
        });
        List<Story.CharacterDef> lineup = new ArrayList<Story.CharacterDef>(heroes);
        lineup.addAll(villains);
        int n = lineup.size();
        float left = n <= 2 ? 0.32f : n <= 4 ? 0.2f : 0.1f, right = n <= 2 ? 0.68f : n <= 4 ? 0.8f : 0.9f;
        // a narrow frame (Reels, Shorts, square posts): the group stands closer so nobody is cut at the sides
        float narrow = "9:16".equals(opt.aspect) ? 0.42f : "1:1".equals(opt.aspect) ? 0.7f : 1f;
        left = 0.5f - (0.5f - left) * narrow;
        right = 0.5f + (right - 0.5f) * narrow;
        for (int i = 0; i < n; i++) {
            Story.CharacterDef c = lineup.get(i);
            Film.Actor a = new Film.Actor();
            a.c = c;
            a.look = c.look;
            a.order = i;
            Film.Key k = new Film.Key();
            k.t = seg.t0;
            k.x = n == 1 ? 640 : 1280 * (left + (right - left) * i / (float) (n - 1));
            k.depth = (n > 5 && i % 2 == 1) ? 1 : 0;
            k.facing = k.x < 640 ? 1 : -1;
            k.noHeadwear = bool(pNoHead.get(c));
            k.wearsTurban = bool(pTurban.get(c));
            k.disguised = bool(pDisguise.get(c));
            Integer ent = entryBeat.get(c);
            if (ent != null) k.visible = false;
            a.keys.add(k);
            seg.actors.add(a);
        }

        // ---------- ambience & music
        ambience(where, t);
        weatherFrom(where + " । " + sc.title + (pi == 0 && sc.cues.length() > 0 ? " । " + sc.cues : ""), seg.t0, true);
        if (Sets.outdoorSet(seg.set) && (seg.tod == Sets.NIGHT || seg.tod == Sets.EVENING)) {
            // natural night: stars in the sky, fireflies in the woods
            if (wOpen[Film.W_STARS] < 0) open(Film.W_STARS, seg.t0, 0.9f);
            if ((seg.set == Sets.FOREST || seg.set == Sets.GARDEN) && wOpen[Film.W_FIREFLIES] < 0) open(Film.W_FIREFLIES, seg.t0, 0.6f);
        }
        int mood = moodOf(sc, b0, b1, where, villains.size() > 0);
        seg.mood = mood;

        // ---------- the director's plan for this part (the emotional arc decides the shots)
        partNo++;
        partPlan.clear();
        List<Integer> pe = new ArrayList<Integer>(), pb = new ArrayList<Integer>();
        List<String> pm = new ArrayList<String>(), ptx = new ArrayList<String>();
        StringBuilder partText = new StringBuilder(where);
        for (int bi = b0; bi < b1; bi++) {
            Story.Beat b = sc.beats.get(bi);
            partText.append(" । ").append(b.text).append(' ').append(b.manner == null ? "" : b.manner);
            if (b.type == Story.Beat.DIALOGUE && beatLine[si][bi] >= 0) {
                pe.add(film.lines.get(beatLine[si][bi]).emotion);
                pm.add(b.manner == null ? "" : b.manner);
                ptx.add(b.text);
                pb.add(bi);
            }
        }
        partFast = ShotPlanner.action(partText.toString());
        ShotPlanner.Plan[] plans = ShotPlanner.plan(pe, pm, ptx, partFast);
        for (int i = 0; i < plans.length; i++) partPlan.put(pb.get(i), plans[i]);
        // how this part begins: time passes (dip to black), magic or memory (dip to white), else a dissolve
        String head = where + " " + (pi == 0 ? sc.title : "");
        if (Txt.has(head, "अगले दिन", "अगली सुबह", "कुछ दिन", "कुछ दिनों", "कई दिन", "साल बाद", "महीने बाद", "बाद में", "next day", "next morning",
                "days later", "weeks later", "years later", "later that", "some time later", "the following")) seg.transition = 1;
        else if (Txt.has(head, "सपना", "सपने", "याद", "बीते", "जादू", "dream", "flashback", "memory", "remember", "magic")) seg.transition = 2;

        // ---------- beats
        int noteIdx = film.notes.size();
        film.notes.add("");
        float tc = t + 0.2f;
        for (int bi = b0; bi < b1; bi++) {
            Story.Beat b = sc.beats.get(bi);
            // entrances that happen at this beat
            for (Film.Actor a : seg.actors) {
                Integer ent = entryBeat.get(a.c);
                if (ent != null && ent == bi) {
                    boolean far = b.type == Story.Beat.DIALOGUE && Txt.has(b.manner, "दूर से");
                    String sent = b.type == Story.Beat.DIRECTION ? b.text : "";
                    tc = enter(a, tc, sent, far);
                }
            }
            if (b.type == Story.Beat.DIALOGUE) { leading = false; tc = dialogue(si, bi, b, tc); }
            else tc = direction(si, bi, b, tc, bi == b0 && pi == 0, where);
        }
        seg.t1 = tc + 0.8f;
        if (pi == nParts - 1) closeAllWeather(seg.t1);
        else { closeWeather(Film.W_STARS, seg.t1); closeWeather(Film.W_FIREFLIES, seg.t1); }
        film.music.add(new Film.Music(mood, seg.t0, seg.t1));
        film.ambience.add(new Film.Amb(seg.t0, seg.t1, where + " " + Sets.name(seg.set) + " " + ambWords(seg)));
        film.notes.set(noteIdx, "Part " + sc.number + (nParts > 1 ? " (" + (char) ('a' + pi) + ")" : "") + ": " + Sets.label(seg.set)
                + (seg.backdrop != null ? " [your picture]" : "") + ", characters: " + names(lineup));
        // remember persistent flags
        for (Film.Actor a : seg.actors) {
            Film.Key k = a.last();
            pNoHead.put(a.c, k.noHeadwear);
            pTurban.put(a.c, k.wearsTurban);
            pDisguise.put(a.c, k.disguised);
        }
        return seg.t1;
    }

    private static boolean bool(Boolean b) { return b != null && b; }

    private String names(List<Story.CharacterDef> l) {
        StringBuilder sb = new StringBuilder();
        for (Story.CharacterDef c : l) { if (sb.length() > 0) sb.append(", "); sb.append(c.shown()); }
        return sb.toString();
    }

    private boolean mentionsAny(Story.Scene sc, int from, int to, Story.CharacterDef c) {
        for (int i = from; i < to; i++) {
            Story.Beat b = sc.beats.get(i);
            String txt = b.type == Story.Beat.DIRECTION ? b.text : b.manner;
            if (ScriptParser.mentions(story, txt).contains(c)) return true;
            if (b.type == Story.Beat.DIALOGUE && b.speaker == c) return true;
        }
        return false;
    }

    /** Names used to address someone: followed by a comma or exclamation mark. */
    private List<Story.CharacterDef> vocatives(String text) {
        List<Story.CharacterDef> out = new ArrayList<Story.CharacterDef>();
        for (Story.CharacterDef c : story.characters) {
            for (String a : c.aliases) {
                if (a.length() < 2) continue;
                if (!Txt.has(text, a + ",", a + " ,")) continue;
                // "मेरी वानुषा, ..." is calling out for someone absent, not talking to them
                if (Txt.has(text, "मेरी " + a, "मेरा " + a, "मेरे " + a, "my " + a)) continue;
                out.add(c);
                break;
            }
        }
        return out;
    }

    static boolean isEntry(String s) {
        return Txt.has(s, "बाहर निकल", "आता है", "आती है", "आते हैं", "आ जाता", "आ जाती", "आ बैठ", "प्रवेश", "घुसते", "घुसता",
                "छलाँग", "उल्टा लटक", "दौड़ते हुए आते", "आगे आती", "आगे आता", "enters", "arrives", "comes in", "पहुँचते");
    }

    static List<String> sentences(String text) {
        List<String> out = new ArrayList<String>();
        for (String s : text.split("[।|]|(?<=[.!?])\\s+")) {
            String q = s.trim();
            if (q.length() > 1) out.add(q);
        }
        return out;
    }

    private List<Story.CharacterDef> presentWithGroups(String s) { return withGroups(s, ScriptParser.present(story, s)); }

    /** Named characters plus group words like "सिपाही", "चारों गार्ड", "तीनों लड़कियाँ". */
    private List<Story.CharacterDef> mentionsWithGroups(String s, List<Story.CharacterDef> context) {
        return withGroups(s, ScriptParser.mentions(story, s));
    }

    private List<Story.CharacterDef> withGroups(String s, List<Story.CharacterDef> named) {
        List<Story.CharacterDef> out = new ArrayList<Story.CharacterDef>(named);
        if (Txt.has(s, "सिपाही", "गार्ड", "पहरेदार", "सैनिक", "guards", "soldiers")) {
            for (Story.CharacterDef c : story.characters)
                if ((c.look.outfit == Look.O_UNIFORM || c.look.spear) && !out.contains(c)) out.add(c);
        }
        if (Txt.has(s, "लड़कियाँ", "लड़कियां", "girls", "राजकुमारियाँ", "princesses")) {
            for (Story.CharacterDef c : story.characters)
                if (c.look.kind == Look.GIRL && !out.contains(c)) out.add(c);
        }
        if (Txt.has(s, "तीनों", "दोनों", "सब ", "सभी") && out.isEmpty()) out.addAll(lastGroup);
        return out;
    }

    private Film.Actor actor(Story.CharacterDef c) { return c == null ? null : seg.find(c); }

    // ------------------------------------------------------------------ entrances

    private float enter(Film.Actor a, float tc, String sentence, boolean far) {
        Film.Key k0 = a.keys.get(0);
        float dest = k0.x;
        boolean fromRight = !a.look.hero || dest > 640;
        float startX = fromRight ? 1400 : -120;
        Film.Key off = a.at(tc);
        off.visible = true;
        off.x = startX;
        off.facing = fromRight ? -1 : 1;
        if (far) { dest = fromRight ? 1180 : 100; off.depth = 1; }
        boolean run = Txt.has(sentence, "दौड़", "छलाँग", "run", "jump");
        if (Txt.has(sentence, "उल्टा लटक", "डाल", "पेड़ से") && a.look.kind == Look.MONKEY) {
            Film.Key k = a.at(tc);
            k.x = 250; k.anchor = Film.A_BRANCH; k.body = Pose.HANG; k.visible = true;
            film.sfx.add(new Film.Sfx(Film.SFX_RUSTLE, tc, 1.2f, 0.7f));
            return tc;
        }
        if (Txt.has(sentence, "हरी", "रोशनी", "जादुई", "धुएँ") && !a.look.hero) {
            Film.Key k = a.at(tc);
            k.x = dest;
            Film.Fx fx = new Film.Fx(Film.FX_GREEN_GLOW, tc, tc + 1.4f);
            fx.x = dest; fx.y = ground - 150; fx.a = a;
            seg.fx.add(fx);
            film.sfx.add(new Film.Sfx(Film.SFX_MAGIC, tc, 1.5f, 0.6f));
            return tc;
        }
        Film.Key k = a.at(tc + 0.01f);
        k.x = dest;
        k.moveDur = run ? 0.9f : 1.3f;
        k.run = run;
        k.facing = dest > startX ? 1 : -1;
        if (a.look.kind == Look.MONSTER || a.look.height > 1.25f) {
            // reveal (§15): first only the ground shaking under the steps, then the camera pulls back and up
            float h = heightOf(a);
            Film.Cam c1 = new Film.Cam(tc + 0.3f, dest, ground - h * 0.12f, 2.1f, 0);
            c1.still = true;
            seg.cams.add(c1);
            Film.Cam c2 = new Film.Cam(tc + 1.3f, dest, ground - h * 0.55f, 1.12f, 2.2f);
            c2.angle = 1; c2.light = 0.85f;
            seg.cams.add(c2);
            Film.Shot sh = shot(tc + 0.3f, ShotPlanner.CU, ShotPlanner.SINGLE, 1, ShotPlanner.PULL_BACK, a, null, ShotPlanner.INTRODUCE);
            sh.purpose = "Reveal: the audience first wonders, then sees how big " + a.c.shown() + " is";
            sh.action = a.c.shown() + " arrives";
            sh.other = "Starts on the feet, pulls back and tilts up to the whole figure";
            sh.face = faceOf(Pose.EVIL);
            sh.body = "Heavy steps";
            sh.cutWhen = "the whole figure is seen";
            sh.emotionalPurpose = "Surprise and awe";
            sh.light = 0.85f;
            k.moveDur = Math.max(k.moveDur, 1.8f);
            lastDlgShot = null;
            tc += 1.6f;
        }
        if (a.look.kind == Look.MONSTER) film.sfx.add(new Film.Sfx(Film.SFX_THUD, tc, 1.4f, 0.8f));
        else film.sfx.add(new Film.Sfx(Film.SFX_STEPS, tc, k.moveDur, 0.35f));
        if (a.look.anklets) film.sfx.add(new Film.Sfx(Film.SFX_ANKLET, tc, k.moveDur, 0.4f));
        return tc + (far ? 0.2f : 0.4f);
    }

    // ------------------------------------------------------------------ dialogue

    private float dialogue(int si, int bi, Story.Beat b, float tc) {
        Film.Line line = film.lines.get(beatLine[si][bi]);
        Film.Actor sp = actor(b.speaker);
        if (sp == null) {
            // should not happen (speakers are always staged) — keep audio anyway
            line.start = tc;
            sub(tc, tc + line.dur, b.speaker == null ? "" : b.speaker.shown(), line.shown);
            return tc + line.dur + 0.35f;
        }
        // things the speaker points at ("देखो वह नाव!", "look, a rainbow") appear; AI cues for this line too
        sceneryFrom(b.text, tc);
        String cue = newCues(b.cue, b.text + " " + b.manner);
        if (cue.length() > 0) natureFrom(cue, tc, sp);
        // speaker must be visible
        Film.Key cur = sp.stateAt(tc);
        if (!cur.visible) { Film.Key k = sp.at(tc); k.visible = true; }
        if (cur.anchor == Film.A_HIDDEN) { Film.Key k = sp.at(tc); k.anchor = Film.A_GROUND; }

        // who is being addressed: a name in the line or the manner, else the previous speaker
        Film.Actor to = null;
        for (Story.CharacterDef m : ScriptParser.mentions(story, b.manner + " । " + b.text)) {
            Film.Actor a = actor(m);
            if (a != null && a != sp && a.stateAt(tc).visible) { to = a; break; }
        }
        if (to == null && lastSpeaker != null && lastSpeaker != sp && lastSpeaker.stateAt(tc).visible) to = lastSpeaker;
        if (to == null) {
            float best = 1e9f;
            for (Film.Actor a : seg.actors) {
                if (a == sp || !a.stateAt(tc).visible) continue;
                float d = Math.abs(xAt(a, tc) - xAt(sp, tc));
                if (d < best) { best = d; to = a; }
            }
        }
        float start = tc + 0.12f;
        float end = start + line.dur;
        line.start = start;
        // face each other
        if (to != null) {
            float sx = xAt(sp, start), tx = xAt(to, start);
            Film.Key ks = sp.at(start - 0.05f);
            if (sp.stateAt(start).anchor == Film.A_GROUND) ks.facing = tx >= sx ? 1 : -1;
            Film.Key kt = to.at(start - 0.05f);
            if (to.stateAt(start).anchor == Film.A_GROUND && to.stateAt(start).body != Pose.LIE) kt.facing = sx >= tx ? 1 : -1;
        }
        Film.Speak s = new Film.Speak();
        s.t0 = start; s.t1 = end; s.line = line.index; s.emotion = line.emotion;
        sp.speaks.add(s);
        sp.acts.add(new Film.Act(start, end, Film.G_TALK));
        // gestures from the manner, e.g. (तलवार घुमाते हुए) (घुटनों के बल गिरकर रोते हुए)
        mannerActions(sp, to, b.manner, start, end, line.emotion);
        headwearFromWords(sp, to, b.text, start);
        if (line.emotion == Pose.SAD) { Film.Key k = sp.at(start); k.tears = true; }
        if (line.emotion == Pose.LAUGH && sp.look.hero && Txt.has(b.text, "हा हा", "हँस")) {
            // the little princess' laugh makes flowers bloom (story magic) – only if the script says so later
        }
        sub(start, end, b.speaker.shown(), line.shown);
        ShotPlanner.Plan plan = partPlan.get(bi);
        if (opt.technical && plan != null) {
            // no long dialogue in a far shot: more than six words go to a front-facing close-up (lip-sync protocol)
            int words = line.text.trim().split("\\s+").length;
            if (words > 6 && plan.size < ShotPlanner.CU) { plan.size = ShotPlanner.CU; plan.type = ShotPlanner.SINGLE; plan.hold = false; }
            else if (plan.size < ShotPlanner.MCU && !(plan.type == ShotPlanner.TWO_SHOT && words <= 6)) plan.size = ShotPlanner.MCU;
        }
        if (opt.technical) { standStillToSpeak(sp, start, end); lipSyncShots(sp, to, start, end, line, plan); }
        else camDialogue(sp, to, start, line.emotion, plan, line);
        float after = 0.35f;
        if (to != null && plan != null && to.stateAt(start).visible) {
            listenerPerformance(sp, to, start, end, line, plan);
            // the reaction is often more important than the line: show it and let it breathe (§9, §31)
            if (plan.reaction && to.stateAt(end).visible && reactionShot(to, sp, end, line, plan)) after += plan.breathe;
        }
        lastSpeaker = sp;
        lastSubject = b.speaker;
        dlgCount++;
        return end + after;
    }

    // ================================================================== Human QC (pipeline step 4)

    public static final int FIX_NONE = 0, FIX_CALM = 1, FIX_CLOSER = 2, FIX_WIDER = 3, FIX_LISTENER = 4, FIX_REMOVE = 5;
    public static final String[] FIX_NAMES = {"Looks good", "Calmer — less movement (motion cut by 80%, subtle breathing only)",
            "Closer", "Wider", "Show the listener instead (the speaker heard off-screen)", "No cut here — the shot before simply goes on"};

    /**
     * The user checked the first frame of a shot and asked for a fix (the protocol's error correction applied to
     * this film). Returns what was done.
     */
    public String fixShot(int index, int fix) {
        if (film == null || index < 0 || index >= film.shots.size() || fix == FIX_NONE) return "";
        Film.Shot sh = film.shots.get(index);
        Film.Seg sg = film.segAt(sh.t + 0.01f);
        if (sg == null || sg.type != Film.S_SCENE) return "";
        Film.Cam cam = null;
        for (Film.Cam c : sg.cams) if (c.t <= sh.t + 0.01f && c.ease == 0) cam = c;
        if (cam == null) return "";
        seg = sg;
        ground = sg.ground;
        float t = sh.t + 0.05f;
        // who the shot is about: the character nearest the middle of the frame
        Film.Actor main = null;
        float bd = 1e9f;
        for (Film.Actor a : sg.actors) {
            if (!a.stateAt(t).visible) continue;
            float d = Math.abs(xAt(a, t) - cam.cx);
            if (d < bd) { bd = d; main = a; }
        }
        String done;
        switch (fix) {
            case FIX_CALM:
                film.calm.add(new float[]{sh.t, sh.t + sh.dur});
                done = "calmer (motion cut by 80%)";
                break;
            case FIX_CLOSER: case FIX_WIDER: {
                float z = fix == FIX_CLOSER ? Math.min(8f, cam.zoom * 1.25f) : Math.max(1f, cam.zoom / 1.25f);
                cam.zoom = z;
                if (main != null) {
                    // keep the whole head in the frame with headroom
                    float[] fb = faceBox(main, t);
                    float fh = 720f / z;
                    cam.cx = fix == FIX_CLOSER ? fb[0] : cam.cx;
                    if (fb[4] < cam.cy - fh / 2 + 0.06f * fh) cam.cy = fb[4] - 0.06f * fh + fh / 2;
                }
                done = fix == FIX_CLOSER ? "closer" : "wider";
                break;
            }
            case FIX_LISTENER: {
                Film.Actor other = null;
                float od = 1e9f;
                for (Film.Actor a : sg.actors) {
                    if (a == main || !a.stateAt(t).visible || a.stateAt(t).anchor != Film.A_GROUND) continue;
                    float d = main == null ? 0 : Math.abs(xAt(a, t) - xAt(main, t));
                    if (d < od) { od = d; other = a; }
                }
                if (other == null) {
                    cam.zoom = Math.max(1f, cam.zoom / 1.6f);
                    done = "wider (no listener in the scene)";
                } else {
                    Film.Cam c = faceCam(other, t, cam.light, 0.75f);
                    cam.cx = c.cx; cam.cy = c.cy; cam.zoom = c.zoom;
                    sh.speech = false; sh.reaction = true; sh.subject = other.c.shown();
                    done = "shows " + other.c.shown() + " listening";
                }
                break;
            }
            case FIX_REMOVE: {
                int i = sg.cams.indexOf(cam);
                if (i > 0) { sg.cams.remove(i); done = "no cut (the shot before goes on)"; }
                else done = "kept (the first shot of a part)";
                break;
            }
            default: return "";
        }
        sh.fixed = done;
        return done;
    }

    // ================================================================== Technical Director protocol (enforced)

    /**
     * Lip-sync protocol (section 8): a line is spoken only in front-facing close-ups whose face fills most of the
     * frame, at most six words per shot. A longer line is split into close-ups of balanced word groups (e.g. 4 + 5
     * words), alternating the close-up with a tighter one and, every third part, the listener's silent reaction.
     */
    private void lipSyncShots(Film.Actor sp, Film.Actor to, float start, float end, Film.Line line, ShotPlanner.Plan plan) {
        String text = line.shown == null || line.shown.trim().isEmpty() ? line.text : line.shown;
        String[] w = text.trim().split("\\s+");
        int n = Math.max(1, (int) Math.ceil(w.length / (float) TechnicalDirector.LIP_SYNC_WORDS));
        float total = 0;
        for (String x : w) total += x.length() + 1;
        float light = plan == null ? -1 : plan.light;
        int stage = plan == null ? ShotPlanner.DEVELOP : plan.stage;
        boolean listener = to != null && to.stateAt(start).visible && to.stateAt(start).anchor == Film.A_GROUND && to.stateAt(start).body != Pose.LIE;
        float before = 0;
        int done = 0;
        for (int k = 0; k < n; k++) {
            int from = Math.round(k * w.length / (float) n), upto = Math.round((k + 1) * w.length / (float) n);
            float tk = start + (end - start) * before / Math.max(1, total);
            StringBuilder words = new StringBuilder();
            for (int i = from; i < upto; i++) { words.append(i > from ? " " : "").append(w[i]); before += w[i].length() + 1; }
            boolean reaction = listener && k % 3 == 2;
            Film.Shot sh;
            if (reaction) {
                // Shot C = reaction: the listener's face, silent; the line goes on off-screen
                Film.Cam c = faceCam(to, tk, light, 0.75f);
                c.keep = true;
                seg.cams.add(c);
                sh = shot(tk, ShotPlanner.MCU, ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, to, sp, stage);
                sh.reaction = true;
                sh.purpose = "Reaction (part " + (k + 1) + " of " + n + "): " + to.c.shown() + " listens; the line goes on off-screen";
                sh.action = to.c.shown() + " listens, subtle breathing, one small change of expression";
                sh.face = faceOf(empathy(line.emotion, sp, to));
                sh.speech = false;
            } else {
                Film.Cam c = faceCam(sp, tk, light, k % 2 == 1 ? 1.18f : 1f);
                c.keep = true;
                seg.cams.add(c);
                sh = shot(tk, k % 2 == 1 ? ShotPlanner.XCU : ShotPlanner.CU, ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, sp, to, stage);
                sh.line = line.index;
                sh.speech = true;
                sh.words = upto - from;
                sh.spoken = words.toString();
                sh.purpose = "Lip-sync close-up" + (n > 1 ? " (part " + (k + 1) + " of " + n + ")" : "") + ": front-facing, head still, only the mouth and jaw move";
                sh.action = sp.c.shown() + ": \"" + words + "\"";
                sh.face = faceOf(line.emotion);
                done++;
            }
            sh.light = light;
            sh.cutWhen = k + 1 < n ? "after these " + (upto - from) + " words" : "the line ends";
            sh.emotionalPurpose = "Lip-sync only in a locked close-up (contains_speech = " + (!reaction) + ")";
            sh.other = "Face in the centre of the frame, " + Math.round(TechnicalDirector.HEADROOM * 100) + "% headroom";
        }
        lastDlgShot = null; lastDlgA = sp; lastDlgB = to;
    }

    /**
     * The words of a line heard between t0 and t1 (each word is timed by its share of the letters, the same
     * timing the lip-sync shots are cut on).
     */
    static String wordsIn(Film.Line fl, float t0, float t1) {
        String text = fl.shown == null || fl.shown.trim().isEmpty() ? fl.text : fl.shown;
        String[] w = text.trim().split("\\s+");
        float total = 0;
        for (String x : w) total += x.length() + 1;
        StringBuilder b = new StringBuilder();
        float before = 0;
        for (String x : w) {
            float mid = fl.start + fl.dur * (before + (x.length() + 1) / 2f) / Math.max(1, total);
            before += x.length() + 1;
            if (mid >= t0 && mid < t1) b.append(b.length() > 0 ? " " : "").append(x);
        }
        return b.toString();
    }

    /**
     * Lip-sync needs the head almost still (8.6): a character walking when their line begins stops where they
     * are, says the line, and then walks on to where they were going.
     */
    private void standStillToSpeak(Film.Actor sp, float start, float end) {
        Film.Key last = sp.keys.get(sp.keys.size() - 1);
        if (last.t > start + 1e-3f) return;                       // later plans exist already: leave them
        Film.Key cur = sp.stateAt(start);
        if (cur.moveDur <= 0 || start >= cur.t + cur.moveDur - 0.15f || cur.anchor != Film.A_GROUND) return;
        float x = xAt(sp, start), dest = cur.x, remaining = cur.t + cur.moveDur - start;
        boolean run = cur.run;
        Film.Key stop = sp.at(start);
        stop.x = x; stop.moveDur = 0; stop.run = false;
        Film.Key go = sp.at(end + 0.15f);
        go.x = dest; go.moveDur = Math.max(0.6f, remaining); go.run = run;
        stillToSpeak++;
    }

    private int stillToSpeak, calmed;

    /**
     * C5: no running in a shot. A move faster than a brisk walk is slowed to walking pace where the story's
     * timing leaves room for it (the next instruction for that character comes later).
     */
    private void calmMoves() {
        final float maxSpeed = 260f;          // stage units per second (about a third of a 16:9 frame)
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            for (Film.Actor a : sg.actors) {
                for (int i = 1; i < a.keys.size(); i++) {
                    Film.Key k = a.keys.get(i);
                    if (k.moveDur <= 0) continue;
                    float dist = Math.abs(k.x - prevX(a, k));
                    if (dist / k.moveDur <= maxSpeed) continue;
                    float want = dist / maxSpeed, room = sg.t1 - k.t;
                    if (i + 1 < a.keys.size()) room = Math.min(room, a.keys.get(i + 1).t - k.t);
                    float d = Math.min(want, room - 0.05f);
                    if (d > k.moveDur + 0.05f) { k.moveDur = d; k.run = false; calmed++; }
                }
            }
        }
    }

    /**
     * A close-up framed on the face itself (section 8.3): the face fills 65-75% of the frame where the picture is
     * sharp enough for it (never enlarged more than 1.25x beyond its own pixels), centred, with headroom.
     * In a narrow frame (9:16) the face's width keeps to the centre 60%.
     */
    private Film.Cam faceCam(Film.Actor a, float t, float light, float tight) {
        float[] fb = faceBox(a, t);
        float ar = TechnicalDirector.ratio(opt.aspect);
        float fill = Math.min(TechnicalDirector.FACE_MIN, TechnicalDirector.SAFE_ZONE * ar / 0.85f) * (tight > 1 ? 1.1f : tight < 1 ? 0.75f : 1f);
        float zoom = fill * 720f / fb[2];
        // the whole head stays in the frame: hair, a turban or a cap (from its top to the chin) fills at most
        // 85% of the frame height, with headroom above it
        // (with room for the small moves of the shot: a bounce, a nod, a gesture lifts the head a little)
        float chin = fb[1] + fb[2] * 0.5f, headTop = fb[4] - 18;
        zoom = Math.min(zoom, 0.85f * 720f / Math.max(1, chin - headTop));
        zoom = Math.max(ShotPlanner.ZOOM[ShotPlanner.MCU], Math.min(fb[3], zoom));
        if (tight > 1) {
            // the next group of words gets a visibly different framing (never a jump cut): closer if the picture
            // allows it, otherwise a little wider
            float base = Math.max(ShotPlanner.ZOOM[ShotPlanner.MCU], Math.min(fb[3], Math.min(fill / 1.1f * 720f / fb[2], 0.85f * 720f / Math.max(1, fb[1] + fb[2] * 0.5f - fb[4]))));
            if (zoom < base * 1.12f) zoom = Math.max(ShotPlanner.ZOOM[ShotPlanner.MCU], base / 1.2f);
        }
        float fh = 720f / zoom;
        float cy = fb[1] + 0.04f * fh;
        if (headTop < cy - fh / 2 + 0.06f * fh) cy = headTop - 0.06f * fh + fh / 2;
        Film.Cam c = new Film.Cam(t, fb[0], cy, zoom, 0);
        c.still = true;
        c.light = light;
        return c;
    }

    /**
     * Where a character's face is on the stage: {centre x, centre y, face height, the closest zoom that keeps it
     * sharp}. From the picture's eye and mouth points when it has them.
     */
    private float[] faceBox(Film.Actor a, float t) {
        float h = heightOf(a), x = finalX(a, t);
        Film.Key k = a.stateAt(t);
        float top = ground - h;
        if (k.body == Pose.SIT) top += h * 0.17f;
        else if (k.body == Pose.KNEEL || k.body == Pose.CROUCH) top += h * 0.25f;
        if (k.anchor == Film.A_BRANCH) top = ground - 450 - h * 0.1f;
        else if (k.anchor == Film.A_SHOULDER || k.anchor == Film.A_ON_FACE) top = ground - 320 - h * 0.1f;
        Art.Sprite sp = art == null ? null : art.sprites.get(a.c.id);
        if (sp != null && sp.faceKnown && sp.mouthY > (sp.eyeLY + sp.eyeRY) / 2) {
            float ey = (sp.eyeLY + sp.eyeRY) / 2, d = sp.mouthY - ey;
            float fh = Math.max(0.05f, 3.2f * d) * h;
            float fy = top + (ey + 0.3f * d) * h;
            float w = h * sp.w / (float) Math.max(1, sp.h);
            float fx = x + ((sp.eyeLX + sp.eyeRX) / 2 - 0.5f) * w * (k.facing < 0 ? -1 : 1);
            float scale = sp.rig != null ? sp.rig.faceScale : 1f;
            float srcPx = 3.2f * d * sp.h * scale;
            float zmax = Math.max(ShotPlanner.MAX_ZOOM, srcPx * 1.25f / fh);
            // the top of the head: the top of the picture (hair, turban), higher when wearing someone's turban
            float headTop = top;
            if (k.wearsTurban) {
                // the turban they wear is someone else's real one: its top, measured from its owner's picture
                float d2 = Math.abs(sp.eyeRX - sp.eyeLX) * w * 1.08f, above = 1.6f;
                if (seg != null) for (Film.Actor o : seg.actors) {
                    Art.Sprite os = art.sprites.get(o.c.id);
                    if (o != a && os != null && os.hatImg != null && o.stateAt(t).noHeadwear) above = Math.max(above, -os.hatY0);
                }
                headTop = Math.min(headTop, fy - 0.3f * d * h - above * d2);
            }
            return new float[]{fx, fy, fh, Math.min(8f, zmax), headTop};
        }
        return new float[]{x, top + 0.11f * h, 0.17f * h, ShotPlanner.MAX_ZOOM, top - (k.wearsTurban ? 0.12f * h : 0)};
    }

    /** The frame's width on the stage for a camera (FINAL_AR decides it). */
    private float frameW(Film.Cam c) { return 720f / c.zoom * TechnicalDirector.ratio(opt.aspect); }

    /** The cut that ends the shot starting with camera i (or the end of the part). */
    private static float shotEnd(Film.Seg sg, int i) {
        for (int j = i + 1; j < sg.cams.size(); j++) if (sg.cams.get(j).ease == 0) return sg.cams.get(j).t;
        return sg.t1;
    }

    private static void sortCams(Film.Seg sg) {
        java.util.Collections.sort(sg.cams, new java.util.Comparator<Film.Cam>() {
            public int compare(Film.Cam a, Film.Cam b) { return Float.compare(a.t, b.t); }
        });
    }

    /**
     * The largest distance a character in the frame travels within [t0, t1), as a fraction of the frame width
     * (from where they first appear in the frame; characters off-screen do not count).
     */
    private float motionIn(Film.Seg sg, Film.Cam c, float t0, float t1) {
        float fw = frameW(c), most = 0;
        for (Film.Actor a : sg.actors) {
            float ref = Float.NaN;
            for (float t = t0 + 0.01f; t < t1; t += 0.05f) {
                if (!a.stateAt(t).visible) { ref = Float.NaN; continue; }
                float x = xAt(a, t);
                boolean in = Math.abs(x - c.cx) < fw * 0.55f;
                if (Float.isNaN(ref)) { if (in) ref = x; continue; }
                if (in || Math.abs(ref - c.cx) < fw * 0.55f) {
                    most = Math.max(most, Math.abs(x - ref) / fw);
                }
            }
        }
        return most;
    }

    /**
     * C5 motion limit: within one shot no character travels 15% of the frame width or more. A long walk or a run
     * becomes several static shots: at the moment the limit is reached the film cuts to a new, still framing that
     * has the moving character ahead of them again (fast action = more cuts, never a moving camera).
     */
    private void enforceMotion() {
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            sortCams(sg);
            for (int i = 0; i < sg.cams.size() && i < 4000; i++) {
                Film.Cam c = sg.cams.get(i);
                if (c.ease > 0) continue;
                float t0 = c.t, t1 = shotEnd(sg, i), fw = frameW(c);
                Film.Cam cut = null;
                for (Film.Actor a : sg.actors) {
                    float ref = Float.NaN;
                    for (float t = t0 + 0.01f; t < t1 - 0.3f; t += 0.05f) {
                        if (!a.stateAt(t).visible) { ref = Float.NaN; continue; }
                        float x = xAt(a, t);
                        boolean in = Math.abs(x - c.cx) < fw * 0.55f;
                        if (Float.isNaN(ref)) { if (in) ref = x; continue; }
                        float dx = x - ref;
                        if (Math.abs(dx) >= fw * (TechnicalDirector.MAX_MOTION - 0.005f) && (in || Math.abs(ref - c.cx) < fw * 0.55f)) {
                            if (cut == null || t < cut.t) {
                                // the new framing: the mover a little behind the centre, room ahead to move into
                                float nx = c.zoom >= ShotPlanner.ZOOM[ShotPlanner.MCU] ? x : c.cx + dx + Math.signum(dx) * fw * 0.1f;
                                cut = new Film.Cam(Math.max(t, t0 + 0.4f), nx, c.cy, c.zoom, 0);
                                cut.still = true; cut.roll = c.roll; cut.angle = c.angle; cut.light = c.light;
                            }
                            break;
                        }
                    }
                }
                if (cut != null && cut.t - t0 >= 0.39f) { sg.cams.add(i + 1, cut); motionCuts++; }
            }
        }
    }

    private int motionCuts, actionCuts, framed;

    private static boolean isAction(int type) {
        return type != Film.G_TALK && type != Film.G_NOD && type != Film.G_LOOK_AWAY && type != Film.G_WHISPER;
    }

    /**
     * P1: one action per shot. When a second action begins inside a shot, the film cuts to it: a closer, still
     * framing on whoever does it.
     */
    private void oneActionPerShot() {
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            sortCams(sg);
            for (int i = 0; i < sg.cams.size() && i < 4000; i++) {
                Film.Cam c = sg.cams.get(i);
                if (c.ease > 0) continue;
                float t0 = c.t, t1 = shotEnd(sg, i);
                List<Float> starts = new ArrayList<Float>();
                for (Film.Actor a : sg.actors) for (Film.Act ac : a.acts) if (isAction(ac.type) && ac.t0 >= t0 - 0.05f && ac.t0 < t1 && a.stateAt(ac.t0 + 0.01f).visible) starts.add(ac.t0);
                if (starts.size() < 2) continue;
                java.util.Collections.sort(starts);
                float first = starts.get(0), second = -1;
                for (float x : starts) if (x - first > 0.3f) { second = x; break; }
                if (second < 0 || second - t0 < 0.5f || t1 - second < 0.5f) continue;
                Film.Actor who = null;
                for (Film.Actor a : sg.actors) for (Film.Act ac : a.acts) if (isAction(ac.type) && Math.abs(ac.t0 - second) < 0.001f) who = a;
                if (who == null) continue;
                float z = c.zoom * 1.25f;
                if (z > ShotPlanner.MAX_ZOOM && c.zoom >= ShotPlanner.MAX_ZOOM) z = c.zoom / 1.25f;
                z = Math.min(Math.max(ShotPlanner.MAX_ZOOM, c.zoom), z);
                float h = Renderer.actorHeight(who.look, who.c, art, sg);
                Film.Cam d = new Film.Cam(second, xAt(who, second), sg.ground - h * 0.55f, z, 0);
                d.still = true; d.angle = c.angle; d.light = c.light;
                sg.cams.add(i + 1, d);
                actionCuts++;
            }
        }
    }

    /**
     * Section 7, resizing: in the film's own shape (FINAL_AR), everyone a shot is about stays whole inside the
     * frame with 15% empty at left and right, faces in the centre 60%, and room above the heads. A group too
     * wide for a narrow frame is taken in by a wider shot; when even that cannot hold them, the shot frames its
     * main character completely rather than cutting anyone in half.
     */
    private void safeFrames() {
        float ar = TechnicalDirector.ratio(opt.aspect);
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            sortCams(sg);
            for (int i = 0; i < sg.cams.size(); i++) {
                Film.Cam c = sg.cams.get(i);
                if (c.ease > 0) continue;
                float t = c.t + 0.05f;
                boolean faceShot = false;
                for (Film.Shot sh : film.shots) if (Math.abs(sh.t - c.t) < 0.06f && (sh.speech || sh.reaction)) faceShot = true;
                // who the shot is about: the characters inside the framing the director chose (planned in 16:9)
                float intent = 1280f / c.zoom;
                List<Film.Actor> subj = new ArrayList<Film.Actor>();
                Film.Actor main = null;
                float bd = 1e9f;
                for (Film.Actor a : sg.actors) {
                    if (!a.stateAt(t).visible) continue;
                    float x = xAt(a, t);
                    if (Math.abs(x - c.cx) < intent / 2) subj.add(a);
                    if (Math.abs(x - c.cx) < bd) { bd = Math.abs(x - c.cx); main = a; }
                }
                if (main == null) continue;
                if (faceShot) {
                    // a face shot is framed on its face already; keep the face inside the centre 60%, and nobody
                    // else half in the frame: a neighbour cut by the edge is moved out of it (or fully in)
                    float fw = frameW(c);
                    float[] fb = faceBox(main, t);
                    float lim = fw * TechnicalDirector.SAFE_ZONE / 2;
                    for (Film.Actor o : sg.actors) {
                        if (o == main || !o.stateAt(t).visible) continue;
                        float[] ob = faceBox(o, t);
                        float half = ob[2] * 0.95f, left = c.cx - fw / 2, right = c.cx + fw / 2;     // the whole head with its hair
                        boolean cut = (ob[0] - half < left && ob[0] + half > left) || (ob[0] - half < right && ob[0] + half > right);
                        if (!cut) continue;
                        // push the frame away from them, as far as the main face can stay in the safe zone
                        float away = ob[0] > c.cx ? -1 : 1;
                        float need = away < 0 ? (c.cx + fw / 2) - (ob[0] - half) : (ob[0] + half) - (c.cx - fw / 2);
                        float room = lim - Math.abs(fb[0] - (c.cx + away * need));
                        if (room >= 0) { c.cx += away * need; framed++; }
                    }
                    if (Math.abs(fb[0] - c.cx) > lim) { c.cx = fb[0] + Math.signum(c.cx - fb[0]) * lim * 0.8f; framed++; }
                    continue;
                }
                if (subj.isEmpty()) subj.add(main);
                float minL = 1e9f, maxR = -1e9f, headTop = 1e9f, feet = -1e9f;
                for (Film.Actor a : subj) {
                    float h = Renderer.actorHeight(a.look, a.c, art, sg), x = xAt(a, t);
                    float half = h * bodyAspect(a) / 2;
                    minL = Math.min(minL, x - half); maxR = Math.max(maxR, x + half);
                    headTop = Math.min(headTop, sg.ground - h);
                    feet = Math.max(feet, sg.ground);
                }
                float span = maxR - minL, usable = 1 - 2 * TechnicalDirector.SIDE_MARGIN;
                float oldX = c.cx, oldZ = c.zoom;
                if (span > frameW(c) * usable) {
                    float z = 720f * ar * usable / span;
                    if (z >= 1f) c.zoom = Math.min(c.zoom, z);
                    else {
                        // not everyone fits even in the widest shot: frame the main character whole and centred
                        float h = Renderer.actorHeight(main.look, main.c, art, sg), x = xAt(main, t), half = h * bodyAspect(main) / 2;
                        minL = x - half; maxR = x + half; headTop = sg.ground - h;
                        c.zoom = Math.min(c.zoom, Math.max(1f, 720f * ar * usable / (2 * half)));
                    }
                }
                c.cx = (minL + maxR) / 2;
                if (subj.size() == 1) {
                    // one character: on a third of the frame, with room in front of their gaze (still whole and
                    // inside the 15% side margins)
                    Film.Actor one = subj.get(0);
                    float fw = frameW(c), half = (maxR - minL) / 2;
                    float off = Math.min(fw / 6, fw * (0.5f - TechnicalDirector.SIDE_MARGIN) - half);
                    if (off > 0) c.cx += one.stateAt(t).facing * off;
                }
                // headroom above the highest head; a full shot keeps the feet in the frame too
                float fh = 720f / c.zoom;
                float topEdge = c.cy - fh / 2;
                float room = TechnicalDirector.HEADROOM * (c.zoom <= ShotPlanner.ZOOM[ShotPlanner.MWIDE] + 0.01f ? 0.5f : 0.6f);
                if (headTop < topEdge + room * fh) c.cy = headTop - room * fh + fh / 2;
                if (c.zoom <= ShotPlanner.ZOOM[ShotPlanner.WIDE] + 0.01f && feet > c.cy + fh / 2 - 0.03f * fh) {
                    float need = feet - headTop + (room + 0.03f) * fh;
                    if (need > fh && 720f / need >= 1f) { c.zoom = Math.min(c.zoom, 720f / need); fh = 720f / c.zoom; }
                    c.cy = headTop - room * fh + fh / 2;
                }
                if (Math.abs(c.cx - oldX) > 2 || Math.abs(c.zoom - oldZ) > 0.01f) framed++;
            }
        }
    }

    /** Width / height of a character's picture (for keeping it whole inside the frame). */
    private float bodyAspect(Film.Actor a) {
        Art.Sprite sp = art == null ? null : art.sprites.get(a.c.id);
        if (sp != null && sp.h > 0) return Math.min(1.6f, sp.w / (float) sp.h);
        return a.look.kind == Look.ANIMAL ? 1.4f : 0.45f;
    }

    private int passed, corrected;
    private final List<String> stillWrong = new ArrayList<String>();

    /**
     * Section 10, the validation layer, on the finished shot list: every shot is checked (static camera, at most
     * 4 s, motion under 15% of the frame, one action, lip-sync only in a front close-up of at most six words,
     * feet on the ground); what fails is corrected by the error-correction rules (section 12).
     */
    private void validateShots() {
        passed = 0; corrected = 0; stillWrong.clear();
        for (Film.Shot sh : film.shots) {
            Film.Seg sg = film.segAt(sh.t + 0.01f);
            if (sg == null) continue;
            Film.Cam cam = null;
            int ci = -1;
            for (int i = 0; i < sg.cams.size(); i++) if (sg.cams.get(i).t <= sh.t + 0.01f) { cam = sg.cams.get(i); ci = i; }
            if (cam == null) continue;
            sh.motion = motionIn(sg, cam, sh.t, sh.t + sh.dur);
            // actions = distinct moments something starts (0.3 s apart), by characters who are there
            List<Float> starts = new ArrayList<Float>();
            for (Film.Actor a : sg.actors) for (Film.Act ac : a.acts)
                if (isAction(ac.type) && ac.t0 >= sh.t - 0.05f && ac.t0 < sh.t + sh.dur && a.stateAt(ac.t0 + 0.01f).visible) starts.add(ac.t0);
            java.util.Collections.sort(starts);
            int acts = 0;
            float lastStart = -9;
            for (float x : starts) if (x - lastStart > 0.3f) { acts++; lastStart = x; }
            // one that begins in the last half second belongs to the next shot (it is cut on the action)
            if (acts > 1 && lastStart > sh.t + sh.dur - 0.5f) acts--;
            sh.actions = Math.max(1, acts);
            if (sh.speech && sh.line >= 0 && sh.line < film.lines.size()) {
                sh.spoken = wordsIn(film.lines.get(sh.line), sh.t, sh.t + sh.dur);
                sh.words = sh.spoken.isEmpty() ? 0 : sh.spoken.split("\\s+").length;
            }
            TechnicalDirector.Shot v = new TechnicalDirector.Shot();
            v.placement = "Foreground 0-1 m | Midground left third / right third 2-4 m | Background 10-100 m";
            v.grounding = TechnicalDirector.GROUND;
            v.prompt = "CHARACTERS: PLACEMENT: ACTION: GROUNDING: LIGHTING: CAMERA: " + TechnicalDirector.STABLE;
            v.staticCamera = cam.still && cam.ease == 0;
            v.motion = sh.motion; v.actions = sh.actions; v.seconds = sh.dur;
            v.speech = sh.speech; v.closeUp = sh.size >= ShotPlanner.CU; v.words = sh.words;
            if (TechnicalDirector.validate(v).isEmpty()) { passed++; continue; }
            List<String> left = TechnicalDirector.correct(v);
            corrected++;
            if (!v.staticCamera || cam.ease > 0) { cam.ease = 0; cam.still = true; }
            if (!v.speech && sh.speech) sh.speech = false;
            if (!left.isEmpty() && stillWrong.size() < 12) stillWrong.add(String.format(java.util.Locale.US, "%d:%04.1f %s", (int) (sh.t / 60), sh.t % 60, left));
        }
    }

    /**
     * A long line is never one long shot: every ~3 seconds the film cuts to the listener's face for a moment and
     * back to the speaker's close-up (the protocol's "100 perfect 3-second shots").
     */
    private void splitLongLine(Film.Actor sp, Film.Actor to, float start, float end, Film.Line line) {
        Film.Cam first = null;
        for (int i = seg.cams.size() - 1; i >= 0; i--) if (seg.cams.get(i).t <= start + 0.01f && seg.cams.get(i).ease == 0) { first = seg.cams.get(i); break; }
        if (first == null) return;
        float th = heightOf(to), tx = finalX(to, start);
        Film.Key tk = to.stateAt(start);
        if (tk.anchor != Film.A_GROUND || tk.body == Pose.LIE) return;
        int part = 1;
        for (float t = start + 3.0f; t < end - 1.6f; t += 4.6f) {
            float zoom = ShotPlanner.zoomFor(ShotPlanner.MCU, th);
            Film.Cam c = new Film.Cam(t, tx + tk.facing * (1280f / zoom) * 0.12f, ground - th * 0.8f + 0.1f * (720f / zoom), zoom, 0);
            c.still = true; c.light = first.light;
            seg.cams.add(c);
            Film.Shot sh = shot(t, ShotPlanner.MCU, ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, to, sp, ShotPlanner.DEVELOP);
            sh.reaction = true;
            sh.purpose = "Cutaway to the listener, so no shot runs longer than about 3 seconds";
            sh.action = to.c.shown() + " listens (" + sp.c.shown() + " goes on speaking off-screen)";
            sh.face = faceOf(tk.emotion);
            sh.cutWhen = "1.6 s later, back to the speaker";
            sh.emotionalPurpose = "Keep the listener present in the conversation";
            Film.Cam back = new Film.Cam(t + 1.6f, first.cx, first.cy, first.zoom, 0);
            back.still = true; back.roll = first.roll; back.angle = first.angle; back.light = first.light;
            seg.cams.add(back);
            Film.Shot sb = shot(t + 1.6f, ShotPlanner.sizeOf(first.zoom), ShotPlanner.SINGLE, first.angle, ShotPlanner.STATIC, sp, to, ShotPlanner.DEVELOP);
            sb.line = line.index;
            sb.purpose = "The speaker again (part " + (++part) + " of the line)";
            sb.action = sp.c.shown() + " goes on speaking";
            sb.face = faceOf(line.emotion);
            sb.cutWhen = "the next part of the line or its end";
            sb.emotionalPurpose = "Lip-sync in a locked close-up";
        }
    }

    /**
     * The listener acts too, without words (§10, §11, §28): the face follows what is heard a moment later,
     * a calm friendly line gets a nod, a sad or scolded listener looks away and back.
     */
    private void listenerPerformance(Film.Actor sp, Film.Actor to, float start, float end, Film.Line line, ShotPlanner.Plan plan) {
        Film.Key cur = to.stateAt(start);
        if (cur.anchor != Film.A_GROUND || cur.body == Pose.LIE) return;
        int e = empathy(line.emotion, sp, to);
        if (plan.intensity >= 0.5f && cur.emotion == Pose.NEUTRAL && e != Pose.NEUTRAL && end - start > 1.2f) {
            Film.Key k = to.at(start + 0.6f);        // a beat later: they take it in, then react
            k.emotion = e;
            if (!plan.reaction) to.at(end + 0.5f).emotion = Pose.NEUTRAL;
        }
        if (plan.intensity < 0.45f && (plan.relation || line.emotion == Pose.HAPPY || line.emotion == Pose.NEUTRAL) && end - start > 1.8f && dlgCount % 2 == 1) {
            float m = (start + end) / 2;
            to.acts.add(new Film.Act(m, m + 0.8f, Film.G_NOD));
        }
        if ((line.emotion == Pose.ANGRY && to.look.hero && sp.look.hero) || cur.emotion == Pose.SAD) {
            if (end - start > 1.6f) to.acts.add(new Film.Act(start + 0.7f, Math.max(start + 1.4f, end - 0.3f), Film.G_LOOK_AWAY));
        }
    }

    /** What the listener feels on hearing a line. */
    private static int empathy(int emo, Film.Actor sp, Film.Actor to) {
        switch (emo) {
            case Pose.SAD: case Pose.PAIN: return Pose.SAD;
            case Pose.SCARED: return Pose.SCARED;
            case Pose.SURPRISED: return Pose.SURPRISED;
            case Pose.HAPPY: case Pose.LAUGH: return Pose.HAPPY;
            case Pose.EVIL: return to.look.hero ? Pose.SCARED : Pose.EVIL;
            case Pose.ANGRY: return !sp.look.hero && to.look.hero ? Pose.SCARED : to.look.hero ? Pose.SAD : Pose.ANGRY;
            case Pose.DETERMINED: return to.look.hero ? Pose.DETERMINED : Pose.NEUTRAL;
            default: return Pose.NEUTRAL;
        }
    }

    /** After the line: the listener's face, a beat for the feeling to land, then the scene goes on. */
    private boolean reactionShot(Film.Actor to, Film.Actor sp, float end, Film.Line line, ShotPlanner.Plan plan) {
        Film.Key tk = to.stateAt(end);
        if (tk.anchor != Film.A_GROUND || tk.body == Pose.LIE) return false;
        float th = heightOf(to), tx = finalX(to, end);
        int size = Math.max(ShotPlanner.MCU, Math.min(ShotPlanner.CU, plan.size));
        float zoom = ShotPlanner.zoomFor(size, th);
        float cy = (tk.body == Pose.SIT ? ground - th * 0.4f : ground - th * 0.8f) + 0.1f * (720f / zoom);
        Film.Cam c = new Film.Cam(end + 0.05f, tx + tk.facing * (1280f / zoom) * 0.12f, cy, zoom, 0);
        c.still = true;
        c.light = plan.light;
        seg.cams.add(c);
        boolean changed = false;
        Film.Key r = to.at(end + 0.3f);     // anticipation: a tiny pause, then the face changes (§23)
        if (r.emotion == Pose.NEUTRAL) {
            int e = empathy(line.emotion, sp, to);
            r.emotion = e == Pose.NEUTRAL ? Pose.SURPRISED : e;
            changed = true;
        }
        if (changed) to.at(end + 0.35f + plan.breathe + 0.5f).emotion = Pose.NEUTRAL;
        Film.Shot sh = shot(end + 0.05f, size, ShotPlanner.SINGLE, plan.height, ShotPlanner.STATIC, to, sp, plan.stage);
        sh.reaction = true;
        sh.purpose = "Reaction: the audience feels the line through " + to.c.shown() + "'s face, and it is allowed to breathe";
        sh.action = to.c.shown() + " listens and takes it in, without words";
        sh.face = faceOf(r.emotion);
        sh.cutWhen = "the feeling has landed (" + Math.round(plan.breathe * 10) / 10f + " s of silence)";
        sh.emotionalPurpose = "Let the reaction breathe before the scene goes on";
        sh.light = plan.light;
        lastDlgShot = null;
        return true;
    }

    /**
     * "Give my cap back!": whoever asks for their cap or turban back is not wearing it, and the one they ask
     * (or the monkey in the scene) is - even when the script never showed it being taken.
     */
    private void headwearFromWords(Film.Actor sp, Film.Actor to, String text, float t) {
        if (text == null || sp.look.headwear != Look.HW_TURBAN || sp.stateAt(t).noHeadwear) return;
        if (!Txt.has(text, "टोपी", "पगड़ी", "साफ़ा", "cap", "turban", "topi", "pagdi")) return;
        if (!Txt.has(text, "वापस", "लौटा", "दे दे", "दे दो", "लाओ", "return", "give it back", "wapas", "lauta")) return;
        if (!Txt.has(text, "मेरी", "मेरा", "my", "meri")) return;
        Film.Actor taker = to != null && to != sp ? to : null;
        if (taker == null) for (Film.Actor a : seg.actors) if (a != sp && a.look.kind == Look.MONKEY) { taker = a; break; }
        Film.Key k = sp.at(t); k.noHeadwear = true;
        if (taker != null && !taker.stateAt(t).wearsTurban) {
            Film.Key k2 = taker.at(t); k2.wearsTurban = true;
            pTurbanColor.put(taker.c, sp.look.headColor);
        }
    }

    private void mannerActions(Film.Actor a, Film.Actor to, String m, float t0, float t1, int emo) {
        if (m == null || m.length() == 0) return;
        List<Film.Actor> me = new ArrayList<Film.Actor>();
        me.add(a);
        postureFrom(m, t0, a, me);
        userEffect(m, t0);
        natureFrom(m, t0, a);
        if (Txt.has(m, "तलवार")) {
            Film.Key k = a.at(t0); k.holdR = Pose.I_WOOD_SWORD;
            a.acts.add(new Film.Act(t0, t1, Txt.has(m, "रोक") ? Film.G_BLOCK : Film.G_SWORD));
            film.sfx.add(new Film.Sfx(Film.SFX_CLACK, t0 + 0.3f, Math.min(2.5f, t1 - t0), 0.45f));
        }
        if (Txt.has(m, "हाथ फैला")) a.acts.add(new Film.Act(t0, t1, Film.G_REACH));
        if (Txt.has(m, "ताली")) { a.acts.add(new Film.Act(t0, t0 + 1.5f, Film.G_CLAP)); film.sfx.add(new Film.Sfx(Film.SFX_CLAP, t0, 1.4f, 0.6f)); }
        if (Txt.has(m, "मूँछ", "मूंछ")) a.acts.add(new Film.Act(t0, t1, Film.G_TWIRL));
        if (Txt.has(m, "सिर") && Txt.has(m, "पकड़")) a.acts.add(new Film.Act(t0, t1, Film.G_HOLD_HEAD));
        if (Txt.has(m, "पेट पकड़", "हँसते", "हंसते", "खिलखिला")) a.acts.add(new Film.Act(t0, t1, Film.G_LAUGH));
        if (Txt.has(m, "सींग हिला", "सिर हिला")) a.acts.add(new Film.Act(t0, t1, Film.G_SHAKE_HEAD));
        if (Txt.has(m, "दहाड़")) { a.acts.add(new Film.Act(t0, t0 + 1.4f, Film.G_ROAR)); film.sfx.add(new Film.Sfx(Film.SFX_ROAR, t0, 1.6f, 0.55f)); shake(t0, t0 + 0.8f); }
        if (Txt.has(m, "टोकरी")) { Film.Key k = a.at(t0); k.holdR = Pose.I_BASKET; a.acts.add(new Film.Act(t0, t1, Film.G_OFFER)); }
        if (Txt.has(m, "भेष")) { Film.Key k = a.at(t0); k.disguised = true; if (k.holdR == Pose.I_NONE) k.holdR = Pose.I_BASKET; }
        if (Txt.has(m, "कदम पीछे", "पीछे खींच", "step back")) {
            a.acts.add(new Film.Act(t0, t0 + 0.8f, Film.G_STEP_BACK));
            Film.Key k = a.at(t0); k.x = xAt(a, t0) - a.stateAt(t0).facing * 70; k.moveDur = 0.7f;
        }
        if (Txt.has(m, "धुआँ", "धुंआ")) {
            Film.Fx fx = new Film.Fx(Film.FX_SMOKE, t0 + 0.5f, t0 + 3f); fx.a = a; fx.color = 0xFF76FF03; fx.x = xAt(a, t0); fx.y = ground - 150;
            seg.fx.add(fx);
            film.sfx.add(new Film.Sfx(Film.SFX_MAGIC, t0 + 0.5f, 1.5f, 0.45f));
        }
        if (Txt.has(m, "खाँस", "खांस")) { a.acts.add(new Film.Act(t0, t1, Film.G_COUGH)); }
        if (Txt.has(m, "चक्कर")) a.acts.add(new Film.Act(t0, t1, Film.G_WIGGLE));
        if (Txt.has(m, "घुटनों")) { Film.Key k = a.at(t0); k.body = Pose.KNEEL; }
        if (Txt.has(m, "काँप", "कांप")) a.acts.add(new Film.Act(t0, t1, Film.G_TREMBLE));
        if (Txt.has(m, "रिबन") && Txt.has(m, "छाती", "लगा")) { Film.Key k = a.at(t0); k.holdR = Pose.I_RIBBON; a.acts.add(new Film.Act(t0, t1, Film.G_CLUTCH)); }
        if (Txt.has(m, "पोटली")) a.acts.add(new Film.Act(t0, t1, Film.G_CLUTCH));
        if (Txt.has(m, "हाथ") && Txt.has(m, "पकड़") && to != null) {
            moveNear(a, to, t0, 0.6f);
            a.acts.add(new Film.Act(t0, t1, Film.G_HOLD_HAND)); a.acts.get(a.acts.size() - 1).target = to;
        }
        if (Txt.has(m, "आगे बढ़", "बिना रुके")) a.acts.add(new Film.Act(t0, t1, Film.G_WALK_PLACE));
        if (Txt.has(m, "डरते", "चारों तरफ देख")) a.acts.add(new Film.Act(t0, t1, Film.G_TREMBLE));
        if (Txt.has(m, "फुसफुसा", "कान में")) {
            a.acts.add(new Film.Act(t0, t1, Film.G_WHISPER));
            if (to != null && Txt.has(m, "कान में")) moveNear(a, to, t0 - 0.3f, 0.4f);
        }
        if (Txt.has(m, "सिर पर हाथ")) { a.acts.add(new Film.Act(t0, t1, Film.G_PAT)); if (to != null) moveNear(a, to, t0, 0.5f); }
        if (Txt.has(m, "आँखें मल", "आँखें मलमल")) a.acts.add(new Film.Act(t0, t1, Film.G_RUB_EYES));
        if (Txt.has(m, "छड़ी गिरा")) {
            Film.Fx fx = new Film.Fx(Film.FX_DUST, t0 + 0.4f, t0 + 1.4f); fx.x = xAt(a, t0) + 40; fx.y = ground; seg.fx.add(fx);
        }
        if (Txt.has(m, "हाथ-पैर मार", "हवा में हाथ")) a.acts.add(new Film.Act(t0, t1, Film.G_FLAIL));
        if (Txt.has(m, "आँखें खोल")) { Film.Key k = a.at(t0); k.body = Pose.SIT; k.eyesShut = false; }
        if (Txt.has(m, "गले लगा") && to != null) {
            moveNear(a, to, t0 - 0.2f, 0.5f);
            Film.Key kt = to.at(t0); if (kt.body == Pose.LIE || kt.body == Pose.SIT) kt.body = Pose.STAND;
            a.acts.add(new Film.Act(t0, t1, Film.G_HUG)); a.acts.get(a.acts.size() - 1).target = to;
            to.acts.add(new Film.Act(t0, t1, Film.G_HUG)); to.acts.get(to.acts.size() - 1).target = a;
        }
        if (Txt.has(m, "केले", "केला")) { Film.Key k = a.at(t0); k.holdR = Pose.I_BANANA; a.acts.add(new Film.Act(t0, t1, Film.G_GIVE)); }
        if (Txt.has(m, "दाँत दिखा", "ज़ोर से") && emo == Pose.NEUTRAL) a.acts.add(new Film.Act(t0, t1, Film.G_LAUGH));
        if (Txt.has(m, "गर्व", "सीना")) a.acts.add(new Film.Act(t0, t1, Film.G_PROUD));
        if (Txt.has(m, "घबरा")) { Film.Key k = a.at(t0); k.sweat = true; }
    }

    // ------------------------------------------------------------------ stage directions

    private float direction(int si, int bi, Story.Beat b, float tc, boolean establishing, String where) {
        float t0 = tc;
        int li = beatLine[si][bi];
        String text = b.text.replaceFirst("^(स्थान|Location|Place|Setting)\\s*[:：]\\s*", "");
        List<String> sents = sentences(text);
        if (establishing && sents.size() > 0) {
            // establishing shot (§2, §16): the camera cranes down from the sky onto the location and settles wide,
            // so where we are, the time of day and who is here read clearly before anything comes close
            boolean grand = Txt.has(where + " " + text, "विशाल", "बड़ा", "विस्तृत", "महल", "जंगल", "पहाड़", "समुद्र", "सागर", "नगर", "huge", "vast", "giant",
                    "palace", "forest", "mountain", "sea", "ocean", "city", "kingdom");
            cam(t0, 640, grand ? 270 : 300, grand ? 1.26f : 1.18f, 0);
            cam(t0 + 0.1f, 640, 372, grand ? 1.0f : 1.06f, grand ? 6.0f : 5.0f);
            Film.Shot sh = shot(t0, grand ? ShotPlanner.XWIDE : ShotPlanner.WIDE, ShotPlanner.TWO_SHOT, 0, ShotPlanner.PULL_BACK, null, null, ShotPlanner.ESTABLISH);
            sh.subject = Sets.label(seg.set);
            sh.purpose = "Establish the place, its scale and the time of day before going closer";
            sh.action = clip(Txt.withoutParens(text), 60);
            sh.other = grand ? "A huge place and small figures (scale contrast)" : "The whole place, the characters where they stand";
            sh.face = "—";
            sh.body = "Everyone in their places";
            sh.cutWhen = "the place is clear and the first action begins";
            sh.emotionalPurpose = "Orientation: the audience knows where it is";
            sh.light = 0.3f;
        }
        Art.Shot shot = art.shotFor(story.scenes.get(si).number, text);
        estab = establishing || leading;      // descriptions before anyone speaks set how things are from the start
        String cue = newCues(b.cue, text);
        if (cue.length() > 0) {
            Film.Actor who = null;
            for (Story.CharacterDef c : ScriptParser.mentions(story, text)) { who = actor(c); if (who != null) break; }
            natureFrom(cue, tc, who);
        }
        for (String s : sents) {
            float d = sentence(s, tc, establishing);
            tc += d;
        }
        estab = false;
        float minDur = establishing ? 3.2f : 2.2f;
        if (tc - t0 < minDur) tc = t0 + minDur;
        if (li >= 0) { // narrator speaks the direction
            Film.Line l = film.lines.get(li);
            l.start = t0 + 0.2f;
            sub(l.start, l.start + l.dur, "", l.shown);
            if (tc < l.start + l.dur + 0.3f) tc = l.start + l.dur + 0.3f;
        }
        if (shot != null) {
            Film.Fx fx = new Film.Fx(Film.FX_SHOT, t0 + 0.2f, t0 + 4.4f);
            fx.pic = shot.pic;
            seg.fx.add(fx);
            // the action continues after the cinematic picture
            float shift = fx.t1 - t0 - 0.3f;
            shiftAfter(t0 + 0.3f, shift);
            tc += shift;
            film.notes.add("  ↳ cinematic shot: " + Txt.withoutParens(text).substring(0, Math.min(40, Txt.withoutParens(text).length())) + "…");
        }
        return tc;
    }

    /** Stages one sentence of a stage direction; returns its duration. */
    private float sentence(String s, float t, boolean establishing) {
        List<Story.CharacterDef> ms = mentionsWithGroups(s, null);
        boolean pronoun = Txt.has(s.length() > 6 ? s.substring(0, Math.min(s.length(), 8)) : s, "वह ", "वो ", "उसने", "उसकी", "उसके", "उसे ", "तीनों", "दोनों");
        Story.CharacterDef subjC = (!ms.isEmpty() && !(pronoun && lastSubject != null && !Txt.has(s, "तीनों", "दोनों"))) ? ms.get(0) : lastSubject;
        if (Txt.has(s, "तीनों", "दोनों") && ms.isEmpty() && !lastGroup.isEmpty()) subjC = lastGroup.get(0);
        Film.Actor subj = actor(subjC);
        List<Film.Actor> others = new ArrayList<Film.Actor>();
        for (Story.CharacterDef c : ms) { Film.Actor a = actor(c); if (a != null && a != subj) others.add(a); }
        Film.Actor target = others.isEmpty() ? null : others.get(0);
        List<Film.Actor> group = new ArrayList<Film.Actor>();
        if (subj != null) group.add(subj);
        group.addAll(others);
        if (Txt.has(s, "तीनों", "दोनों", "मिलकर") && group.size() <= 1) {
            group.clear();
            for (Story.CharacterDef c : lastGroup) { Film.Actor a = actor(c); if (a != null) group.add(a); }
        }
        float d = 1.4f;
        boolean focusSet = false;

        // ---- weather and nature that the action calls for (rain starts, thunder, a stone into the pond…)
        d = Math.max(d, natureFrom(s, t, subj));
        // ---- sounds named in the text (onomatopoeia), and the user's own effects for what happens
        userEffect(s, t);
        if (Txt.has(s, "कल-कल", "झरन")) film.sfx.add(new Film.Sfx(Film.SFX_STREAM, t, 4, 0.35f));
        if (Txt.has(s, "चह-चह", "चिड़िय")) film.sfx.add(new Film.Sfx(Film.SFX_BIRDS, t, 4, 0.4f));
        if (Txt.has(s, "सर-सर") && !Txt.has(s, "रेंग")) film.sfx.add(new Film.Sfx(Film.SFX_WIND, t, 4, 0.35f));
        if (Txt.has(s, "रेंग")) film.sfx.add(new Film.Sfx(Film.SFX_HISS, t, 2, 0.5f));
        if (Txt.has(s, "खट-खट", "टकराने")) film.sfx.add(new Film.Sfx(Film.SFX_CLACK, t, 3, 0.5f));
        if (Txt.has(s, "सरसराहट", "पत्तों")) { film.sfx.add(new Film.Sfx(Film.SFX_RUSTLE, t, 1.5f, 0.6f)); fx(Film.FX_LEAVES, t, t + 2, 250, 260, null, null); }
        if (Txt.has(s, "चीं-चीं", "खिर-खिर")) film.sfx.add(new Film.Sfx(Film.SFX_MONKEY, t + 0.2f, 1.6f, 0.6f));
        if (Txt.has(s, "खर-खर", "जंगली जानवर")) film.sfx.add(new Film.Sfx(Film.SFX_NIGHT, t, 6, 0.45f));
        if (Txt.has(s, "ढोल", "नगाड़")) film.sfx.add(new Film.Sfx(Film.SFX_DRUMS, t, 6, 0.5f));

        // ---- establishing description of the location (no actions) – just hold
        if (establishing && group.isEmpty()) return 2.4f;

        if (subj != null) { lastSubject = subj.c; }
        if (group.size() > 1) {
            lastGroup.clear();
            for (Film.Actor a : group) lastGroup.add(a.c);
        }

        // ---- actions
        if (Txt.has(s, "तितली") && subj != null) {
            if (Txt.has(s, "पीछे दौड़", "पकड़")) {
                Film.Fx fx = fx(Film.FX_BUTTERFLY, t, t + 60, xAt(subj, t), ground - 260, subj, null);
                fx.t2 = 1e9f;
                runBackForth(subj, t, 3.4f);
                d = Math.max(d, 3.6f);
            } else if (Txt.has(s, "बैठ")) {
                // butterfly settles on a dry bud
                for (Film.Fx f : seg.fx) if (f.type == Film.FX_BUTTERFLY && f.t1 > t) { f.t2 = t; f.x = 900; f.y = ground - 70; }
                Film.Fx bud = fx(Film.FX_MAGIC_FLOWER, t, t + 600, 900, ground - 10, null, null);
                bud.t2 = 1e9f;
                Film.Key k = subj.at(t); k.facing = 900 > xAt(subj, t) ? 1 : -1;
                d = Math.max(d, 2.2f);
            }
        }
        if (Txt.has(s, "ताली") && subj != null) { subj.acts.add(new Film.Act(t, t + 1.6f, Film.G_CLAP)); film.sfx.add(new Film.Sfx(Film.SFX_CLAP, t, 1.5f, 0.6f)); d = Math.max(d, 1.8f); }
        if (Txt.has(s, "हँस", "खिलखिला") && subj != null && !Txt.has(s, "हँसी गूँज")) {
            Film.Key k = subj.at(t); k.emotion = Pose.LAUGH;
            subj.acts.add(new Film.Act(t, t + 1.8f, Film.G_LAUGH));
            Film.Key k2 = subj.at(t + 1.9f); k2.emotion = Pose.HAPPY;
            d = Math.max(d, 2f);
        }
        if (Txt.has(s, "फूल में बदल", "फूल खिल", "खिल उठ", "पप-पप")) {
            for (Film.Fx f : seg.fx) if (f.type == Film.FX_MAGIC_FLOWER && f.t2 > t) f.t2 = t + 0.4f;
            Film.Fx bloom = fx(Film.FX_BLOOM, t + 0.3f, t + 600, 640, ground, subj, null);
            bloom.color = Txt.has(s, "हज़ारों", "पूरे बगीचे") ? 1 : 0;
            for (int i = 0; i < 6; i++) film.sfx.add(new Film.Sfx(Film.SFX_POP, t + 0.3f + i * 0.22f, 0.3f, 0.5f));
            film.sfx.add(new Film.Sfx(Film.SFX_MAGIC, t + 0.3f, 2.5f, 0.55f));
            if (Txt.has(s, "रोशनी", "दिव्य")) fx(Film.FX_FLASH, t + 0.4f, t + 1.6f, 640, 360, null, null).color = 0xFFFFF59D;
            cam(t, 700, ground - 150, 1.15f, 0.8f);
            focusSet = true;
            d = Math.max(d, 3.4f);
        }
        if (subj != null && subj.look.kind == Look.BIRD && Txt.has(s, "पेड़ पर", "डाल पर", "शाखा", "on a tree", "on the branch")) {
            Film.Key k = subj.at(ts(t)); k.anchor = Film.A_BRANCH; k.x = 260; k.visible = true;
        }
        if (Txt.has(s, "उल्टा लटक") && subj != null) {
            Film.Key k = subj.at(t); k.visible = true; k.anchor = Film.A_BRANCH; k.body = Pose.HANG; k.x = 260;
        }
        if (Txt.has(s, "पगड़ी", "टोपी", "साफ़ा", "turban", "cap", "pagdi", "topi") && Txt.has(s, "छीन", "उड़ा", "झपट", "ले भाग", "उतार", "snatch", "grab", "took", "chheen") && subj != null) {
            Film.Actor victim = target;
            if (victim == null) for (Film.Actor a : seg.actors) if (a.look.headwear == Look.HW_TURBAN && a != subj) { victim = a; break; }
            if (victim != null && victim.stateAt(t).noHeadwear) victim = null;      // already taken
            if (victim != null) {
                float vx = xAt(victim, t);
                Film.Key k1 = subj.at(t + 0.4f); k1.anchor = Film.A_GROUND; k1.body = Pose.STAND; k1.x = vx - 60; k1.moveDur = 0.5f; k1.run = true;
                subj.acts.add(new Film.Act(t + 0.4f, t + 1.0f, Film.G_JUMP));
                Film.Key kv = victim.at(t + 0.9f); kv.noHeadwear = true;
                Film.Key k2 = subj.at(t + 0.9f); k2.wearsTurban = true;
                pTurbanColor.put(subj.c, victim.look.headColor);
                victim.acts.add(new Film.Act(t + 0.9f, t + 3f, Film.G_HOLD_HEAD));
                film.sfx.add(new Film.Sfx(Film.SFX_WHOOSH, t + 0.5f, 0.6f, 0.6f));
                cam(t + 0.3f, vx - 30, ground - 250, 1.35f, 0.4f);
                focusSet = true;
                d = Math.max(d, 2.6f);
            }
        }
        if (Txt.has(s, "पगड़ी पहन", "टोपी पहन", "पगड़ी अपने सिर", "टोपी अपने सिर") && subj != null) { Film.Key k = subj.at(t); k.wearsTurban = true; }
        if (Txt.has(s, "डाल पर चढ़", "ऊँची डाल") && subj != null) {
            Film.Key k = subj.at(t + 0.2f); k.anchor = Film.A_BRANCH; k.body = Pose.STAND; k.x = 230; k.moveDur = 0.6f;
            subj.acts.add(new Film.Act(t, t + 0.7f, Film.G_JUMP));
        }
        if (Txt.has(s, "मटक") && subj != null) { subj.acts.add(new Film.Act(t + 0.8f, t + 3.2f, Film.G_WIGGLE)); d = Math.max(d, 3.2f); }
        if (Txt.has(s, "मूँछ", "मूंछ") && Txt.has(s, "मरोड़", "तान") && subj != null) subj.acts.add(new Film.Act(t, t + 2.5f, Film.G_TWIRL));
        if (Txt.has(s, "सीना फुला") && subj != null) subj.acts.add(new Film.Act(t, t + 2.5f, Film.G_PROUD));
        if (Txt.has(s, "शर्म", "गुस्से से लाल", "चेहरा गुस्से") && subj != null) {
            Film.Actor who = subj;
            for (Film.Actor a : group) if (Txt.has(s, a.c.displayName + " बेबसी", a.c.displayName + " ")) who = a;
            if (Txt.has(s, "बेबसी") && target != null) who = target;
            Film.Key k = who.at(t + 0.3f); k.redFace = true; k.emotion = Pose.ANGRY;
            who.acts.add(new Film.Act(t + 0.3f, t + 2.5f, Film.G_LOOK_UP));
            Film.Key k2 = who.at(t + 4f); k2.redFace = false;
            camOn(who, t + 0.3f, 1.6f);
            focusSet = true;
            d = Math.max(d, 2.5f);
        }
        d = Math.max(d, postureFrom(s, t, subj, group));
        if (Txt.has(s, "मसल") && subj != null) { Film.Key k = subj.at(t); k.holdR = Pose.I_FLOWER; subj.acts.add(new Film.Act(t + 0.5f, t + 2f, Film.G_CRUSH)); Film.Key k2 = subj.at(t + 2f); k2.holdR = Pose.I_NONE; d = Math.max(d, 2.2f); }
        if (Txt.has(s, "आँखें") && Txt.has(s, "धधक", "दहक", "चमक") && subj != null && !subj.look.hero) {
            camOn(subj, t, 1.9f); focusSet = true; film.sfx.add(new Film.Sfx(Film.SFX_ROAR, t, 1.2f, 0.25f));
        }
        if (Txt.has(s, "छड़ी") && Txt.has(s, "घुमा", "इशारे") && subj != null) {
            Film.Actor w = subj;
            for (Film.Actor a : seg.actors) if (a.look.wand) w = a;
            w.acts.add(new Film.Act(t, t + 2f, Film.G_WAND));
            fx(Film.FX_SPARKLE, t, t + 2f, xAt(w, t), ground - 300, w, null).color = 0xFF76FF03;
            film.sfx.add(new Film.Sfx(Film.SFX_MAGIC, t, 1.6f, 0.5f));
        }
        if (Txt.has(s, "खेल रही", "खेल रहा", "खेलना") && subj != null && !Txt.has(s, "छोड़")) subj.acts.add(new Film.Act(t, t + 3f, Film.G_PLAY));
        if (Txt.has(s, "खेलना छोड़") && subj != null) { Film.Key k = subj.at(t); k.emotion = Pose.CURIOUS; }
        if (Txt.has(s, "आँखें") && Txt.has(s, "उत्सुकता", "चमक उठ") && subj != null && subj.look.hero) { camOn(subj, t, 1.8f); focusSet = true; }
        if (Txt.has(s, "तरफ बढ़", "ओर बढ़", "की तरफ चल") && subj != null) {
            Film.Actor dest = target;
            if (dest == null) for (Film.Actor a : seg.actors) if (!a.look.hero && a.stateAt(t).visible) dest = a;
            float x = dest != null ? xAt(dest, t) - Math.signum(xAt(dest, t) - xAt(subj, t)) * 150 : Math.min(1100, xAt(subj, t) + 260);
            Film.Key k = subj.at(t); k.x = x; k.moveDur = 2.2f; k.facing = x > xAt(subj, t) ? 1 : -1;
            d = Math.max(d, 2.4f);
        }
        if (Txt.has(s, "टिमटिमा")) fx(Film.FX_LADDOO_GLOW, t, t + 3, 0, 0, null, null);
        if (Txt.has(s, "छुआ", "छूते")) {
            Film.Actor v = subj;
            if (v != null) v.acts.add(new Film.Act(t, t + 0.8f, Film.G_REACH));
        }
        if (Txt.has(s, "धुएँ", "धुआँ", "फूँ") && Txt.has(s, "फट", "गुबार", "फूँ")) {
            Film.Actor at = subj;
            if (Txt.has(s, "गायब") || at == null) at = null;
            if (at != null && !Txt.has(s, "गायब")) {
                fx(Film.FX_SMOKE, t + 0.4f, t + 3.4f, xAt(at, t) + 40, ground - 200, at, null).color = 0xFF2E7D32;
                film.sfx.add(new Film.Sfx(Film.SFX_WHOOSH, t + 0.3f, 1.2f, 0.8f));
                d = Math.max(d, 2.4f);
            }
        }
        if (Txt.has(s, "बेहोश") && Txt.has(s, "गिर") && subj != null) {
            Film.Key k = subj.at(t + 0.5f); k.body = Pose.LIE; k.eyesShut = true; k.emotion = Pose.DIZZY;
            film.sfx.add(new Film.Sfx(Film.SFX_THUD, t + 0.6f, 0.4f, 0.4f));
            camOn(subj, t, 1.3f); focusSet = true;
            d = Math.max(d, 2f);
        }
        if (Txt.has(s, "पिंजरे में") && subj != null) {
            Film.Key k = subj.at(ts(t)); k.body = Pose.LIE; k.anchor = Film.A_CAGE; k.x = 760; k.visible = true; k.eyesShut = true;
            Film.Fx cage = fx(Film.FX_CAGE, ts(t), seg.t0 + 1e6f, 760, ground, subj, null);
            cage.t2 = 1e9f;
        }
        if (Txt.has(s, "भेष") && Txt.has(s, "उतर", "हट")) {
            Film.Actor w = null;
            for (Film.Actor a : group) if (a.stateAt(t).disguised) w = a;
            if (w == null) for (Film.Actor a : seg.actors) if (a.stateAt(t).disguised) w = a;
            if (w != null) {
                Film.Key k = w.at(t + 0.4f); k.disguised = false; k.holdR = Pose.I_NONE; k.emotion = Pose.EVIL;
                fx(Film.FX_FLASH, t + 0.3f, t + 1.1f, xAt(w, t), ground - 200, w, null).color = 0xFF76FF03;
                film.sfx.add(new Film.Sfx(Film.SFX_MAGIC, t + 0.3f, 1.5f, 0.7f));
                camOn(w, t + 0.3f, 1.7f); focusSet = true;
                d = Math.max(d, 2.4f);
            }
        }
        if (Txt.has(s, "धम-धम")) { film.sfx.add(new Film.Sfx(Film.SFX_THUD, t, 1.6f, 0.9f)); shake(t, t + 1.4f); }
        if (Txt.has(s, "कंधे पर उठा") && subj != null) {
            Film.Actor victim = null;
            for (Film.Actor a : others) if (a.stateAt(t).body == Pose.LIE) victim = a;
            if (victim == null && target != null) victim = target;
            if (victim != null) {
                Film.Key k = subj.at(t + 0.3f); k.x = xAt(victim, t) + 90; k.moveDur = 0.8f;
                Film.Key kc = subj.at(t + 1.3f); kc.holdL = Pose.I_NONE;
                subj.acts.add(new Film.Act(t + 1.3f, t + 6f, Film.G_PULL_ROPE)); // arms up carrying
                Film.Key kv = victim.at(t + 1.3f); kv.anchor = Film.A_CARRIED; kv.anchorActor = subj;
                d = Math.max(d, 2.2f);
            }
        }
        if (Txt.has(s, "गायब") && subj != null) {
            float tv = t + (Txt.has(s, "कंधे") ? 2.4f : 0.6f);
            fx(Film.FX_SMOKE, tv - 0.3f, tv + 2.2f, xAt(subj, tv), ground - 220, subj, null).color = 0xFF2E7D32;
            film.sfx.add(new Film.Sfx(Film.SFX_WHOOSH, tv - 0.3f, 1.4f, 0.9f));
            Film.Key k = subj.at(tv); k.visible = false;
            for (Film.Actor a : seg.actors) {
                Film.Key st = a.stateAt(tv);
                if (st.anchor == Film.A_CARRIED && st.anchorActor == subj) { Film.Key kk = a.at(tv); kk.visible = false; }
            }
            d = Math.max(d, tv - t + 1.2f);
        }
        if (Txt.has(s, "रिबन") && Txt.has(s, "गिर")) {
            fx(Film.FX_RIBBON, t + d - 0.6f, seg.t0 + 1e6f, 700, ground - 4, null, null).color = 0xFFC62828;
            cam(t + d - 0.6f, 700, ground - 60, 2.2f, 0.6f);
            focusSet = true;
            d += 1.8f;
        }
        if (Txt.has(s, "रिबन") && Txt.has(s, "उठा") && subj != null) {
            for (Film.Fx f : seg.fx) if (f.type == Film.FX_RIBBON && f.t1 > t) f.t1 = t + 0.5f;
            Film.Key k = subj.at(t + 0.5f); k.holdR = Pose.I_RIBBON;
            subj.acts.add(new Film.Act(t + 0.8f, t + 2.2f, Film.G_FIST));
        }
        if (Txt.has(s, "भीड़ को चीरते", "आगे आती", "आगे आता") && subj != null) {
            Film.Key k = subj.at(t); k.x = 640; k.moveDur = 1.2f; k.depth = 0;
            Film.Key k2 = subj.at(t + 1.3f); k2.emotion = Pose.ANGRY;
            camOn(subj, t + 1.2f, 1.5f); focusSet = true;
            d = Math.max(d, 2.0f);
        }
        if (Txt.has(s, "मुट्ठी") && subj != null) subj.acts.add(new Film.Act(t + 0.6f, t + 2.2f, Film.G_FIST));
        if (Txt.has(s, "कंधे पर आ बैठ", "कंधे पर बैठ") && subj != null && target != null) {
            Film.Key k = subj.at(t + 0.2f); k.anchor = Film.A_SHOULDER; k.anchorActor = target; k.visible = true; k.body = Pose.STAND;
            subj.acts.add(new Film.Act(t, t + 0.6f, Film.G_JUMP));
            film.sfx.add(new Film.Sfx(Film.SFX_MONKEY, t, 1.2f, 0.6f));
        }
        if (Txt.has(s, "रिबन है", "हाथ में") && subj != null && subj.look.kind == Look.MONKEY) { Film.Key k = subj.at(t); k.holdR = Pose.I_RIBBON; }
        if (Txt.has(s, "उँगली दिखा", "इशारा") && subj != null) subj.acts.add(new Film.Act(t, t + 2.5f, Film.G_POINT));
        if (Txt.has(s, "आगे बढ़ रहे", "तेज़ी से आगे", "चल रहे")) {
            for (Film.Actor a : group) {
                Film.Key k = a.at(t); k.x = Math.min(1180, xAt(a, t) + 140); k.moveDur = 3.5f; k.facing = 1;
            }
            d = Math.max(d, 3.5f);
        }
        if (Txt.has(s, "चट्टान पर उछल") && subj != null) {
            Film.Key k = subj.at(t); k.x = 1080; k.moveDur = 0.7f; k.anchor = Film.A_GROUND;
            subj.acts.add(new Film.Act(t, t + 0.8f, Film.G_JUMP));
        }
        if (Txt.has(s, "पत्थर से बंद", "दरवाज़े को") && subj != null) { subj.acts.add(new Film.Act(t, t + 2.5f, Film.G_PUSH)); film.sfx.add(new Film.Sfx(Film.SFX_THUD, t + 0.8f, 1.2f, 0.6f)); }
        if (Txt.has(s, "नाच") && subj != null) subj.acts.add(new Film.Act(t, t + 3.5f, Film.G_DANCE));
        if (Txt.has(s, "छिपे", "छिपी", "छुपे") && !Txt.has(s, "झाड़ियों")) {
            int i = 0;
            for (Film.Actor a : group) {
                if (!a.look.hero) continue;
                Film.Key k = a.at(ts(t)); k.anchor = Film.A_ROCK_HIDE; k.x = 150 + i * 120; k.depth = 1; k.facing = 1;
                i++;
            }
            if (i > 0) fx(Film.FX_HIDE_ROCK, ts(t), seg.t0 + 1e6f, 260, ground, null, null).t2 = 1e9f;
        }
        if (Txt.has(s, "झाड़ियों में छिपा") && subj != null) {
            // comes out of the bushes
            Film.Key k = subj.at(t); k.visible = false; k.x = 1380;
            Film.Key k2 = subj.at(t + 0.05f); k2.visible = true; k2.x = 950; k2.moveDur = 1.4f; k2.facing = -1;
            d = Math.max(d, 1.6f);
        }
        if (Txt.has(s, "शीशा", "रोशनी") && Txt.has(s, "आँखों पर", "आंखों पर") && subj != null) {
            Film.Actor victim = target;
            Film.Key k = subj.at(t); k.holdR = Pose.I_MIRROR; k.anchor = Film.A_GROUND;
            subj.acts.add(new Film.Act(t, t + 3.5f, Film.G_MIRROR));
            Film.Fx beam = fx(Film.FX_BEAM, t + 0.8f, t + 5.5f, 0, 0, subj, victim);
            beam.color = 0xFFFFF8E1;
            film.sfx.add(new Film.Sfx(Film.SFX_GLASS, t + 0.8f, 1.5f, 0.6f));
            if (victim != null) { Film.Key kv = victim.at(t + 1f); kv.emotion = Pose.PAIN; victim.acts.add(new Film.Act(t + 1f, t + 4f, Film.G_RUB_EYES)); }
            d = Math.max(d, 3.2f);
        }
        if (Txt.has(s, "नाक पर", "चेहरे पर", "सिर पर बैठ", "पीठ पर")) {
            // "X की नाक पर जा बैठता" -> X is the target, the other character jumps
            Film.Actor owner = possessorBefore(s, new String[]{"नाक पर", "चेहरे पर", "सिर पर", "पीठ पर"});
            if (owner != null) {
                Film.Actor jumper = null;
                for (Film.Actor a : group) if (a != owner) { jumper = a; break; }
                if (jumper != null) { subj = jumper; target = owner; }
            }
        }
        if (Txt.has(s, "नाक पर") && subj != null && target != null) {
            Film.Key k = subj.at(t + 0.3f); k.anchor = Film.A_ON_FACE; k.anchorActor = target;
            subj.acts.add(new Film.Act(t, t + 0.5f, Film.G_JUMP));
            subj.acts.add(new Film.Act(t + 0.5f, t + 6f, Film.G_SCRATCH));
            target.acts.add(new Film.Act(t + 0.5f, t + 6f, Film.G_FLAIL));
            Film.Key kt = target.at(t + 0.5f); kt.emotion = Pose.PAIN;
            film.sfx.add(new Film.Sfx(Film.SFX_MONKEY, t + 0.2f, 1.5f, 0.7f));
            camOn(target, t + 0.4f, 1.4f); focusSet = true;
            d = Math.max(d, 2.4f);
        }
        if (Txt.has(s, "पिंजरे के पास", "पिंजरे तक") && subj != null) {
            Film.Key k = subj.at(t); k.anchor = Film.A_GROUND; k.x = 660; k.moveDur = 1.0f; k.run = true; k.facing = 1; k.depth = 0;
            for (Film.Actor a : seg.actors) {
                if (a.stateAt(t).anchor == Film.A_ROCK_HIDE && a != subj) { Film.Key ka = a.at(t + 0.2f); ka.anchor = Film.A_GROUND; ka.x = 880; ka.moveDur = 1.1f; ka.run = true; ka.depth = 0; }
            }
            for (Film.Fx f : seg.fx) if (f.type == Film.FX_HIDE_ROCK && f.t1 > t) f.t1 = t + 0.3f;
            d = Math.max(d, 1.4f);
        }
        if (Txt.has(s, "पानी") && Txt.has(s, "छींट", "छीट") && subj != null) {
            Film.Actor v = null;
            for (Film.Actor a : others) if (a.stateAt(t).body == Pose.LIE) v = a;
            fx(Film.FX_SPLASH, t + 1.0f, t + 2.2f, v != null ? xAt(v, t) : xAt(subj, t) + 100, ground - 40, v, null);
            Film.Key k = subj.at(t + 0.4f); k.holdR = Pose.I_BOTTLE;
            Film.Key k2 = subj.at(t + 2.4f); k2.holdR = Pose.I_NONE;
            film.sfx.add(new Film.Sfx(Film.SFX_SPLASH, t + 1.0f, 0.8f, 0.6f));
            d = Math.max(d, 2.2f);
        }
        if (Txt.has(s, "कुंडी खोल", "पिंजरा खोल")) {
            for (Film.Fx f : seg.fx) if (f.type == Film.FX_CAGE && f.t2 > t) f.t2 = t + 0.6f;
            film.sfx.add(new Film.Sfx(Film.SFX_CLACK, t + 0.5f, 0.4f, 0.6f));
            for (Film.Actor a : seg.actors) if (a.stateAt(t).anchor == Film.A_CAGE) { Film.Key k = a.at(t + 0.6f); k.anchor = Film.A_GROUND; }
        }
        if (Txt.has(s, "दौड़कर") && Txt.has(s, "मुहाने", "पहुँच") && !group.isEmpty()) {
            int i = 0;
            for (Film.Actor a : group) {
                Film.Key k0 = a.at(t); k0.x = -150 - i * 90; k0.visible = true; k0.anchor = Film.A_GROUND; k0.body = Pose.STAND;
                Film.Key k = a.at(t + 0.05f); k.x = 330 + i * 120; k.moveDur = 1.6f; k.run = true; k.facing = 1;
                i++;
            }
            d = Math.max(d, 2.2f);
        }
        if (Txt.has(s, "घंट") && Txt.has(s, "रस्सी", "खींच")) {
            for (Film.Actor a : group) a.acts.add(new Film.Act(t, t + 3f, Film.G_PULL_ROPE));
            d = Math.max(d, 1.5f);
        }
        if (Txt.has(s, "टन", "घंटे की")) {
            Film.Fx bell = fx(Film.FX_BELL, t, t + 4.5f, 640, 330, null, null);
            for (int i = 0; i < 3; i++) film.sfx.add(new Film.Sfx(Film.SFX_BELL, t + 0.2f + i * 1.0f, 4f, 0.8f));
            shake(t + 0.2f, t + 2.6f);
            d = Math.max(d, 3.2f);
        }
        if (Txt.has(s, "घुसते", "घुसता") && !others.isEmpty() || Txt.has(s, "जाल फेंक")) {
            // guards rush in and throw a net over the villains
            List<Film.Actor> catchers = new ArrayList<Film.Actor>(), caught = new ArrayList<Film.Actor>();
            for (Film.Actor a : group) (a.look.hero ? catchers : caught).add(a);
            int i = 0;
            for (Film.Actor a : catchers) {
                Film.Key k0 = a.stateAt(t);
                if (!k0.visible) { Film.Key kk = a.at(t); kk.x = -150 - i * 80; kk.visible = true; }
                Film.Key k = a.at(t + 0.1f); k.x = 520 + i * 95; k.moveDur = 1.3f; k.run = true; k.facing = 1;
                i++;
            }
            film.sfx.add(new Film.Sfx(Film.SFX_STEPS, t, 1.4f, 0.6f));
            for (Film.Actor a : caught) {
                Film.Key k = a.at(t + 1.6f); k.netted = true; k.emotion = Pose.ANGRY;
                Film.Key k0 = a.at(t); if (!k0.visible) { k0.visible = true; }
                fx(Film.FX_NET, t + 1.4f, seg.t0 + 1e6f, xAt(a, t), ground, a, null);
                a.acts.add(new Film.Act(t + 1.6f, t + 4f, Film.G_FLAIL));
            }
            if (!caught.isEmpty()) {
                film.sfx.add(new Film.Sfx(Film.SFX_NET, t + 1.4f, 1f, 0.7f));
                List<Film.Actor> both = new ArrayList<Film.Actor>(caught);
                if (!catchers.isEmpty()) both.add(catchers.get(catchers.size() - 1));
                camGroup(both, t + 1.2f, 2.4f);
                focusSet = true;
            }
            d = Math.max(d, 3.6f);
        }
        if (Txt.has(s, "सिर पर हाथ") && subj != null && target != null) {
            moveNear(subj, target, t, 0.7f);
            subj.acts.add(new Film.Act(t + 0.7f, t + 3f, Film.G_PAT));
            Film.Key kt = target.at(t + 0.7f); kt.emotion = Pose.HAPPY;
            d = Math.max(d, 2f);
        }
        if (Txt.has(s, "सैल्यूट", "salute") && subj != null) {
            if (target != null) moveNear(subj, target, t, 0.6f);
            subj.acts.add(new Film.Act(t + 0.6f, t + 2.6f, Film.G_SALUTE));
            d = Math.max(d, 2.4f);
        }
        if (Txt.has(s, "हाथ थाम") && subj != null && target != null) {
            moveNear(subj, target, t, 0.6f);
            subj.acts.add(new Film.Act(t + 0.6f, t + 6, Film.G_HOLD_HAND));
            d = Math.max(d, 1.6f);
        }
        if (Txt.has(s, "पसीना")) for (Film.Actor a : group) { Film.Key k = a.at(t); k.sweat = true; }
        if (Txt.has(s, "तलवार") && Txt.has(s, "अभ्यास", "टकरा") ) {
            int i = 0;
            for (Film.Actor a : group) {
                Film.Key k = a.at(t); k.holdR = Pose.I_WOOD_SWORD; k.facing = i % 2 == 0 ? 1 : -1;
                a.acts.add(new Film.Act(t, t + 3.5f, i % 2 == 0 ? Film.G_SWORD : Film.G_BLOCK));
                i++;
            }
            if (group.size() >= 2) { moveNear(group.get(0), group.get(1), t, 0.1f); }
            d = Math.max(d, 3.4f);
        }
        if (Txt.has(s, "दौड़ते हुए आते", "दौड़ते हुए आती")) d = Math.max(d, 1.8f);
        if (Txt.has(s, "पहरा")) d = Math.max(d, 2.2f);

        if (!focusSet && !establishing) {
            List<Film.Actor> vis = new ArrayList<Film.Actor>();
            for (Film.Actor a : group) if (a.stateAt(t + d * 0.5f).visible) vis.add(a);
            if (vis.size() == 1) camOn(vis.get(0), t, 1.35f);
            else if (vis.size() >= 2) camGroup(vis, t + 0.1f, d);
            else cam(t, 640, 360, 1.0f, 0.6f);
        }
        return d;
    }

    // ------------------------------------------------------------------ helpers

    /** The character named just before "की/के/का <...> keyword" in a sentence (the owner of a body part). */
    private Film.Actor possessorBefore(String s, String[] keys) {
        int k = Txt.firstIndex(s, keys);
        if (k < 0) return null;
        String before = Txt.norm(s).substring(0, k);
        Film.Actor best = null;
        int bestPos = -1;
        for (Film.Actor a : seg.actors) {
            for (String al : a.c.aliases) {
                String na = Txt.norm(al);
                if (na.length() < 2) continue;
                int i = before.lastIndexOf(na);
                if (i < 0) continue;
                String rest = before.substring(i + na.length()).trim();
                if ((rest.startsWith("की") || rest.startsWith("के") || rest.startsWith("का")) && i > bestPos) { bestPos = i; best = a; }
            }
        }
        return best;
    }

    /** Pushes everything staged at or after time t later by dt (used to make room for a cinematic picture). */
    private void shiftAfter(float t, float dt) {
        for (Film.Actor a : seg.actors) {
            for (Film.Key k : a.keys) if (k.t >= t) k.t += dt;
            for (Film.Act x : a.acts) if (x.t0 >= t) { x.t0 += dt; x.t1 += dt; }
        }
        for (Film.Fx f : seg.fx) {
            if (f.type == Film.FX_SHOT) continue;
            if (f.t0 >= t) { f.t0 += dt; if (f.t1 < 1e5f) f.t1 += dt; if (f.t2 > 0 && f.t2 < 1e5f) f.t2 += dt; }
        }
        for (Film.Cam c : seg.cams) if (c.t >= t) c.t += dt;
        for (Film.Sfx x : film.sfx) if (x.t >= t && x.t < seg.t0 + 1e5f) x.t += dt;
    }

    // ================================================================== weather and nature

    private final float[] wOpen = new float[Film.W_KINDS];   // start of weather in progress (-1 = none)
    private final float[] wStr = new float[Film.W_KINDS];
    private float fireX = 640;
    private int candleKind;
    private boolean cloudsDark;

    /** A spot on the floor where no character stands at time t (for a campfire). */
    private float freeSpot(float t) {
        float best = 640, bestD = -1;
        for (float x = 200; x <= 1080; x += 40) {
            float d = 1e9f;
            if (seg != null) for (Film.Actor a : seg.actors) if (a.stateAt(t).visible) d = Math.min(d, Math.abs(xAt(a, t) - x));
            if (d > bestD) { bestD = d; best = x; }
        }
        return best;
    }

    static final String[] RAIN = {"बारिश", "वर्षा", "बरसात", "बूँदाबाँदी", "बूंदाबांदी", "बरसने", "बरस रह", "rain", "drizzl", "monsoon", "baarish", "barish"};
    static final String[] RAIN_STOP = {"बारिश रुक", "बारिश थम", "बारिश बंद", "वर्षा रुक", "वर्षा थम", "rain stopped", "rain stops", "stopped raining",
            "rain ended", "धूप निकल", "sun came out", "बादल छँट", "बादल छंट"};
    static final String[] STORM = {"तूफ़ान", "तूफान", "storm", "cyclone", "toofan"};
    static final String[] DUST = {"आँधी", "आंधी", "dust storm", "sandstorm", "aandhi"};
    static final String[] WIND = {"हवा चल", "हवा बह", "तेज़ हवा", "तेज हवा", "ठंडी हवा", "हवा के झोंक", "हवा का झोंका", "हवा में उड़", "हवा में लहरा",
            "सरसराती हवा", "wind", "breeze", "gust", "windy", "hawa chal"};
    static final String[] WIND_STRONG = {"तेज़ हवा", "तेज हवा", "ज़ोर की हवा", "जोर की हवा", "झोंक", "strong wind", "gust", "howling", "fierce wind"};
    static final String[] WIND_SOFT = {"हल्की हवा", "ठंडी हवा", "मंद हवा", "breeze", "gentle wind", "soft wind"};
    static final String[] WIND_STOP = {"हवा रुक", "हवा थम", "wind stopped", "wind died", "wind dropped"};
    static final String[] THUNDER = {"बिजली कड़क", "बिजली चमक", "बिजली गिर", "बादल गरज", "गड़गड़ाहट", "कड़कड़ाहट", "thunder", "lightning", "bijli"};
    static final String[] SNOW = {"बर्फ़ गिर", "बर्फ गिर", "बर्फबारी", "बर्फ़बारी", "हिमपात", "snow"};
    static final String[] FOG = {"कोहरा", "कोहरे", "धुंध", "fog", "mist", "kohra"};
    static final String[] FOG_STOP = {"कोहरा छँट", "कोहरा छंट", "धुंध छँट", "fog lifted", "fog cleared", "mist cleared"};
    static final String[] FIRE = {"अलाव", "आग जल", "आग के पास", "आग के चारों", "आग जला", "लकड़ियाँ जल", "हवन", "campfire", "bonfire", "fire burning",
            "around the fire", "by the fire", "lit a fire"};
    static final String[] FIREFLY = {"जुगनू", "firefl"};
    static final String[] PETALS = {"फूल बरस", "फूलों की वर्षा", "फूलों की बारिश", "पुष्प वर्षा", "पंखुड़ियाँ", "पंखुड़ियां", "petals", "shower of flowers", "flowers rained"};
    static final String[] LEAVES = {"पत्ते उड़", "पत्ते गिर", "पतझड़", "सूखे पत्ते", "falling leaves", "leaves fly", "leaves flew", "autumn"};
    static final String[] STARS = {"तारे", "सितारे", "तारों", "stars", "starry"};
    static final String[] RAINBOW = {"इंद्रधनुष", "इन्द्रधनुष", "rainbow"};
    static final String[] SEA = {"समुद्र", "सागर", "समंदर", "ocean", "sea ", "seashore", "beach", "लहरें", "लहरों", "waves", "shore", "तट पर"};
    static final String[] BOAT = {"नाव", "नौका", "किश्ती", "कश्ती", "boat", "ship", "जहाज़", "जहाज", "बेड़ा", "raft"};
    static final String[] CANDLE = {"मोमबत्ती", "मोमबत्तियाँ", "candle"};
    static final String[] DIYA = {"दीया", "दीये", "दीयों", "दीपक", "दीप जल", "दीपावली", "दिवाली", "diya", "diwali", "oil lamp", "lamps"};
    static final String[] TORCH = {"मशाल", "मशालें", "लालटेन", "torch", "lantern"};
    static final String[] CLOUDS = {"बादल", "घटा", "घटाएँ", "clouds", "cloudy", "overcast"};
    static final String[] DARK_CLOUDS = {"काले बादल", "घने बादल", "dark clouds", "black clouds", "storm clouds"};
    static final String[] BIRDS_FLY = {"पक्षी उड़", "चिड़िया उड़", "चिड़ियाँ उड़", "पंछी उड़", "पक्षियों का झुंड", "birds fly", "birds flew", "flock of birds", "birds flying"};
    static final String[] QUAKE = {"भूकंप", "भूचाल", "धरती काँप", "धरती कांप", "धरती हिल", "ज़मीन हिल", "जमीन हिल", "ज़मीन काँप", "जमीन कांप", "earthquake",
            "ground shook", "ground shakes", "ground trembl", "earth shook"};
    static final String[] FELL = {"गिरा", "गिरे", "गिरी", "टपका", "टपके", "fell", "falls", "dropped", "drops from"};
    static final String[] FALLERS = {"आम", "सेब", "फल", "नारियल", "अमरूद", "गेंद", "फूल", "apple", "mango", "fruit", "coconut", "ball", "flower"};
    static final String[] BALL = {"गेंद", "ball"};
    static final String[] FRUIT = {"आम", "सेब", "फल", "नारियल", "अमरूद", "apple", "mango", "fruit", "coconut", "guava"};
    static final String[] FLOWER = {"फूल", "flower"};
    static final String[] STONE = {"पत्थर", "कंकड़", "कंकड", "ढेला", "stone", "pebble", "rock"};
    static final String[] THROW = {"फेंक", "उछाल", "throw", "threw", "toss", "hurl", "flung"};
    static final String[] WATERWORDS = {"तालाब", "नदी", "पानी", "झील", "सरोवर", "कुआँ", "कुएँ", "pond", "river", "lake", "water", "well", "stream"};
    static final String[] INTO_WATER = {"पानी में कूद", "तालाब में कूद", "नदी में कूद", "पानी में गिर", "तालाब में गिर", "नदी में गिर", "पानी में छलांग",
            "jumped into the water", "jumps into the water", "dived into", "dives into", "fell into the water", "fell into the pond", "fell into the river",
            "jumped into the pond", "jumped into the river"};

    private void open(int type, float t, float strength) {
        if (wOpen[type] >= 0) { wStr[type] = Math.max(wStr[type], strength); return; }
        wOpen[type] = t;
        wStr[type] = strength;
    }

    private void closeWeather(int type, float t) {
        if (wOpen[type] < 0) return;
        float t0 = wOpen[type];
        wOpen[type] = -1;
        if (t - t0 < 0.5f) return;
        Film.Weather w = new Film.Weather(type, t0, t, wStr[type]);
        if (type == Film.W_FIRE) w.x = fireX;
        if (type == Film.W_CANDLES) w.kind = candleKind;
        if (type == Film.W_CLOUDS) { w.kind = cloudsDark ? 1 : 0; cloudsDark = false; }
        film.weather.add(w);
        // the sound of it
        String amb = type == Film.W_RAIN ? (wStr[type] > 1.1f ? "heavy rain" : "rain") : type == Film.W_STORM ? "heavy rain storm"
                : type == Film.W_WIND ? "wind" : type == Film.W_DUST ? "storm wind आँधी" : type == Film.W_FIRE ? "fire" : null;
        if (amb != null) film.ambience.add(new Film.Amb(t0, t, amb));
        if (type == Film.W_STORM) {
            // a storm brings lightning every few seconds
            float lt = t0 + 3f;
            int i = 0;
            while (lt < t - 1) {
                lightning(lt, i++);
                lt += 6 + Nature.rnd(i, 7) * 7;
            }
        }
    }

    private void closeAllWeather(float t) {
        for (int k = 0; k < Film.W_KINDS; k++) closeWeather(k, t);
    }

    private void lightning(float t, int i) {
        Film.Fx f = new Film.Fx(Film.FX_LIGHTNING, t, t + 0.6f);
        f.x = 200 + Nature.rnd(i + (int) (t * 10), 3) * 880;
        f.color = i;
        seg.fx.add(f);
        film.sfx.add(new Film.Sfx(Film.SFX_THUNDER, t + 0.3f + Nature.rnd(i, 5) * 0.6f, 5f, 0.85f));
        shake(t + 0.35f, t + 1.1f);
    }

    // ================================================================== postures and gestures

    static final String[] SIT_DOWN = {"बैठ गया", "बैठ गई", "बैठ गए", "बैठ गयी", "बैठ जाता", "बैठ जाती", "बैठ जाते", "बैठते हैं", "बैठा है", "बैठी है",
            "बैठे हैं", "बैठा हुआ", "बैठी हुई", "बैठे हुए", "पर बैठा", "पर बैठी", "पर बैठे", "बैठकर", "बैठ कर", "sat down", "sits down", "sat on", "sits on",
            "is sitting", "are sitting", "was sitting", "were sitting", "seated", "took a seat", "baith gaya", "baith gayi"};
    static final String[] STAND_UP = {"उठ खड़", "उठकर खड़", "खड़ा हो गया", "खड़ी हो गई", "खड़े हो गए", "खड़ी हो गयी", "उठ गया", "उठ गई", "उठ गए",
            "stood up", "stands up", "got up", "gets up", "rose to", "uth khada"};
    static final String[] LIE_DOWN = {"लेट गया", "लेट गई", "लेट गए", "सो गया", "सो गई", "सो गए", "lay down", "lies down", "fell asleep"};
    static final String[] THRONE = {"सिंहासन", "राजगद्दी", "गद्दी पर", "throne"};
    static final String[] STOOL = {"कुर्सी", "मूढ़ा", "मूढ़े", "चौकी", "बेंच", "चारपाई", "खाट", "chair", "stool", "bench", "cot"};
    static final String[] ROCK = {"चट्टान", "पत्थर पर", "rock", "boulder"};
    static final String[] FLOOR = {"ज़मीन पर", "जमीन पर", "धरती पर", "फर्श पर", "दरी", "चटाई", "floor", "ground", "on the grass", "घास पर"};
    static final String[] BOW = {"प्रणाम", "नमस्ते", "नमस्कार", "झुककर", "सिर झुका", "bow", "bowed", "namaste", "pranam"};
    static final String[] WAVE = {"हाथ हिला", "टाटा", "wave", "waved", "waving"};
    static final String[] NOD = {"हाँ में सिर", "सिर हिलाकर हाँ", "nodded", "nods"};
    static final String[] TURN = {"पीछे मुड़", "मुड़कर", "मुड़ गया", "मुड़ गई", "turned around", "turns around", "turned back"};

    /** Sitting down, getting up, lying down, bowing, waving, nodding, turning — for the subject (or the whole group). */
    private float postureFrom(String s, float t, Film.Actor subj, List<Film.Actor> group) {
        if (subj == null) return 0;
        float d = 0;
        List<Film.Actor> who = group != null && group.size() > 1 && Txt.has(s, "सब", "सभी", "दोनों", "तीनों", "all", "both", "everyone") ? group : null;
        if (who == null) { who = new ArrayList<Film.Actor>(); who.add(subj); }
        boolean birdLike = subj.look.kind == Look.MONKEY || subj.look.kind == Look.BIRD;
        if (Txt.has(s, STAND_UP)) {
            for (Film.Actor a : who) { Film.Key k = a.at(t + 0.1f); if (k.body == Pose.SIT || k.body == Pose.KNEEL || k.body == Pose.LIE) { k.body = Pose.STAND; } }
            d = Math.max(d, 1.0f);
        } else if (Txt.has(s, SIT_DOWN) && !birdLike && !Txt.has(s, "कंधे पर", "नाक पर", "सिर पर", "पीठ पर", "डाल पर", "shoulder")) {
            int seat = Txt.has(s, THRONE) ? Film.SEAT_THRONE : Txt.has(s, STOOL) ? Film.SEAT_STOOL : Txt.has(s, ROCK) ? Film.SEAT_ROCK
                    : Txt.has(s, FLOOR) ? Film.SEAT_FLOOR : -2;
            for (Film.Actor a : who) {
                Film.Key k = a.at(ts(t));
                k.body = Pose.SIT;
                int st = seat;
                if (st == -2) {
                    // a king or queen in their hall sits on the throne; others on a stool, or on the ground outdoors
                    boolean royal = Txt.has(a.c.displayName + " " + a.c.fullName + " " + a.c.description, "राजा", "रानी", "महाराज", "king", "queen");
                    st = royal && (seg.set == Sets.HALL || seg.set == Sets.COURTYARD) ? Film.SEAT_THRONE : Sets.outdoorSet(seg.set) ? Film.SEAT_FLOOR : Film.SEAT_STOOL;
                }
                k.seat = st;
                // the seat is furniture: it stays in the place after they get up
                if (st >= Film.SEAT_STOOL) {
                    boolean have = false;
                    for (Film.Fx f : seg.fx) if (f.type == Film.FX_SEAT && f.a == a) have = true;
                    if (!have) { Film.Fx f = fx(Film.FX_SEAT, seg.t0, 1e6f, xAt(a, ts(t)), ground, a, null); f.kind = st; }
                }
            }
            d = Math.max(d, estab ? 0 : 1.0f);
        } else if (Txt.has(s, LIE_DOWN)) {
            for (Film.Actor a : who) { Film.Key k = a.at(t + 0.2f); k.body = Pose.LIE; }
            d = Math.max(d, 1.2f);
        }
        if (Txt.has(s, BOW)) { for (Film.Actor a : who) a.acts.add(new Film.Act(t, t + 1.7f, Film.G_BOW)); d = Math.max(d, 1.7f); }
        if (Txt.has(s, WAVE)) { for (Film.Actor a : who) a.acts.add(new Film.Act(t, t + 1.8f, Film.G_WAVE)); d = Math.max(d, 1.6f); }
        if (Txt.has(s, NOD)) { for (Film.Actor a : who) a.acts.add(new Film.Act(t, t + 1.2f, Film.G_NOD)); d = Math.max(d, 1.2f); }
        if (Txt.has(s, TURN)) {
            for (Film.Actor a : who) {
                a.acts.add(new Film.Act(t, t + 0.36f, Film.G_TURN));
                Film.Key k = a.at(t + 0.18f);
                k.facing = -a.stateAt(t).facing;
            }
            d = Math.max(d, 0.8f);
        }
        return d;
    }

    /** The AI's cues for a line, without the events the text itself already sets off (no double lightning or stones). */
    private static String newCues(String cue, String text) {
        if (cue == null || cue.length() == 0) return "";
        StringBuilder b = new StringBuilder();
        for (String c : cue.split(" । ")) {
            boolean dup = (Txt.has(c, THUNDER) && Txt.has(text, THUNDER)) || (Txt.has(c, QUAKE) && Txt.has(text, QUAKE))
                    || (Txt.has(c, STONE) && Txt.has(text, STONE) && Txt.has(text, THROW))
                    || (Txt.has(c, FELL) && Txt.has(text, FELL) && Txt.has(text, FALLERS))
                    || (Txt.has(c, THROW) && Txt.has(text, THROW)) || (Txt.has(c, INTO_WATER) && Txt.has(text, INTO_WATER));
            if (!dup) b.append(c).append(" । ");
        }
        return b.toString();
    }

    /** Scenery named in a line of dialogue: it is there to be seen (no weather starts from talk). */
    private void sceneryFrom(String s, float t) {
        if (s == null) return;
        if (Txt.has(s, BOAT)) { open(Film.W_BOAT, t, 1f); if (wOpen[Film.W_SEA] < 0 && !Txt.has(s, "नदी", "river", "तालाब", "lake", "झील")) open(Film.W_SEA, t, 0.6f); }
        if (Txt.has(s, SEA) && !Txt.has(s, "जाएँगे", "जाएंगे", "चलेंगे", "will go", "let's go")) open(Film.W_SEA, t, 0.8f);
        if (Txt.has(s, RAINBOW)) open(Film.W_RAINBOW, t, 1f);
        if (Txt.has(s, BIRDS_FLY)) open(Film.W_BIRDS, t, 1f);
        if (Txt.has(s, FIREFLY)) open(Film.W_FIREFLIES, t, 1f);
        if (Txt.has(s, STARS) && (seg.tod == Sets.NIGHT || seg.tod == Sets.EVENING)) open(Film.W_STARS, t, 1f);
    }

    private static boolean negated(String s) {
        return Txt.has(s, "नहीं", "बिना", "बंद हो", "not ", "no rain", "without");
    }

    /** Weather named in a place description or an action line. Returns time the action needs. */
    private float weatherFrom(String s, float t, boolean place) {
        float d = 0;
        if (Txt.has(s, RAIN_STOP)) { closeWeather(Film.W_RAIN, t); closeWeather(Film.W_STORM, t); }
        else if (Txt.has(s, RAIN) && !negated(s)) {
            float st = Txt.has(s, "तेज़ बारिश", "तेज बारिश", "मूसलाधार", "heavy rain", "pouring", "downpour") ? 1.35f
                    : Txt.has(s, "हल्की", "बूँदाबाँदी", "बूंदाबांदी", "drizzl", "light rain") ? 0.5f : 1f;
            open(Film.W_RAIN, t, st);
            d = 2.2f;
        }
        if (Txt.has(s, DUST) && !negated(s)) { open(Film.W_DUST, t, 1f); open(Film.W_WIND, t, 1f); d = 2.2f; }
        else if (Txt.has(s, STORM) && !negated(s)) { open(Film.W_STORM, t, 1f); open(Film.W_RAIN, t, 1.3f); d = 2.2f; }
        if (Txt.has(s, WIND_STOP)) closeWeather(Film.W_WIND, t);
        else if (Txt.has(s, WIND) && !negated(s)) {
            open(Film.W_WIND, t, Txt.has(s, WIND_STRONG) ? 1f : Txt.has(s, WIND_SOFT) ? 0.35f : 0.6f);
            d = Math.max(d, 1.6f);
        }
        if (Txt.has(s, SNOW) && !negated(s)) { open(Film.W_SNOW, t, 1f); d = Math.max(d, 2f); }
        if (Txt.has(s, FOG_STOP)) closeWeather(Film.W_FOG, t);
        else if (Txt.has(s, FOG) && !negated(s)) open(Film.W_FOG, t, Txt.has(s, "घना", "घने", "thick", "dense") ? 1f : 0.7f);
        if (Txt.has(s, FIRE) && !Txt.has(s, "आग बबूला", "आग-बबूला")) {
            if (wOpen[Film.W_FIRE] < 0) fireX = freeSpot(t + 0.5f);
            open(Film.W_FIRE, t, 1f);
        }
        if (Txt.has(s, FIREFLY)) open(Film.W_FIREFLIES, t, 1f);
        if (Txt.has(s, STARS)) open(Film.W_STARS, t, 1f);
        if (Txt.has(s, RAINBOW)) open(Film.W_RAINBOW, t, 1f);
        if (Txt.has(s, LEAVES)) open(Film.W_LEAVES, t, 1f);
        if (Txt.has(s, PETALS)) {
            if (place) open(Film.W_PETALS, t, 1f);
            else film.weather.add(new Film.Weather(Film.W_PETALS, t, t + 7, 1f));
            d = Math.max(d, 2.5f);
        }
        if (!place && Txt.has(s, THUNDER) && !negated(s)) { lightning(t + 0.2f, film.weather.size() + (int) t); d = Math.max(d, 1.8f); }
        if (Txt.has(s, SEA)) open(Film.W_SEA, t, Txt.has(s, "ऊँची लहर", "ऊंची लहर", "big waves", "high waves", "rough sea") ? 1.3f : 0.8f);
        if (Txt.has(s, BOAT)) { open(Film.W_BOAT, t, 1f); if (wOpen[Film.W_SEA] < 0 && !Txt.has(s, "नदी", "river", "तालाब", "lake", "झील")) open(Film.W_SEA, t, 0.6f); }
        if (Txt.has(s, CANDLE) && !negated(s)) { open(Film.W_CANDLES, t, 1f); candleKind = 0; }
        else if (Txt.has(s, DIYA) && !negated(s)) { open(Film.W_CANDLES, t, 1f); candleKind = 1; }
        else if (Txt.has(s, TORCH) && !negated(s)) { open(Film.W_CANDLES, t, 1f); candleKind = 2; }
        if (Txt.has(s, DARK_CLOUDS)) { open(Film.W_CLOUDS, t, 1f); cloudsDark = true; }
        else if (Txt.has(s, CLOUDS) && !Txt.has(s, "बादल गरज", "बादल छँट", "बादल छंट")) open(Film.W_CLOUDS, t, 0.8f);
        if (Txt.has(s, BIRDS_FLY)) open(Film.W_BIRDS, t, 1f);
        if (Txt.has(s, QUAKE) && !negated(s)) {
            float q = place ? t + 0.5f : t;
            film.weather.add(new Film.Weather(Film.W_QUAKE, q, q + 3.5f, 1f));
            shake(q, q + 3.5f);
            film.sfx.add(new Film.Sfx(Film.SFX_THUNDER, q, 4f, 0.7f));
            d = Math.max(d, 3.5f);
        }
        return d;
    }

    /** Nature in an action line: weather, a stone thrown into water, someone jumping into a pond. */
    private float natureFrom(String s, float t, Film.Actor subj) {
        float d = weatherFrom(s, t, false);
        if (Txt.has(s, STONE) && Txt.has(s, THROW)) {
            boolean water = Txt.has(s, WATERWORDS) || (seg.backdrop != null && seg.backdrop.scan != null && seg.backdrop.scan.anyWater);
            float sx = subj != null ? xAt(subj, t) : 640, face = subj != null ? subj.stateAt(t).facing : 1;
            float[] p = null;
            if (water && seg.backdrop != null && seg.backdrop.scan != null) {
                Art.Backdrop b = seg.backdrop;
                p = Nature.waterPoint(b.scan, b.x0, b.y0, b.x1, b.y1, 1280, 720, sx + face * 300, ground);
            }
            if (p == null) p = new float[]{Math.max(80, Math.min(1200, sx + face * 300)), ground + 40};
            float throwAt = t + 0.5f, flight = 0.9f;
            if (subj != null) subj.acts.add(new Film.Act(t, t + 1.0f, Film.G_THROW));
            Film.Fx f = fx(Film.FX_STONE, throwAt, throwAt + flight + 4.2f, p[0], p[1], subj, null);
            f.t2 = throwAt + flight;
            f.color = water ? 1 : 0;
            film.sfx.add(new Film.Sfx(Film.SFX_WHOOSH, throwAt, 0.6f, 0.35f));
            film.sfx.add(new Film.Sfx(water ? Film.SFX_SPLASH : Film.SFX_THUD, throwAt + flight, 1.2f, 0.6f));
            d = Math.max(d, 3f);
        }
        // things falling (fruit from a tree, a ball) and things thrown (a ball, a fruit, a flower)
        if (Txt.has(s, FALLERS) && Txt.has(s, FELL) && !Txt.has(s, INTO_WATER)) {
            int kind = Txt.has(s, BALL) ? 1 : Txt.has(s, FRUIT) ? 2 : Txt.has(s, FLOWER) ? 3 : 0;
            float x = subj != null ? xAt(subj, t) + subj.stateAt(t).facing * 70 : freeSpot(t);
            Film.Fx f = fx(Film.FX_FALL, t + 0.3f, t + 3.3f, x, ground, subj, null);
            f.kind = kind;
            film.sfx.add(new Film.Sfx(Film.SFX_THUD, t + 0.3f + 0.53f, 0.6f, 0.45f));
            d = Math.max(d, 2.2f);
        } else if ((Txt.has(s, BALL) || Txt.has(s, "फल फेंक", "आम फेंक", "फूल फेंक", "fruit", "flower")) && Txt.has(s, THROW) && !Txt.has(s, STONE)) {
            int kind = Txt.has(s, BALL) ? 1 : Txt.has(s, FLOWER) ? 3 : 2;
            Film.Actor to = null;
            for (Story.CharacterDef c : mentionsWithGroups(s, null)) { Film.Actor a = actor(c); if (a != null && a != subj) { to = a; break; } }
            float sx = subj != null ? xAt(subj, t) : 400, face = subj != null ? subj.stateAt(t).facing : 1;
            float tx = to != null ? xAt(to, t) : Math.max(80, Math.min(1200, sx + face * 320));
            if (subj != null) subj.acts.add(new Film.Act(t, t + 1.0f, Film.G_THROW));
            Film.Fx f = fx(Film.FX_THROW, t + 0.5f, t + 3.5f, tx, ground - (to != null ? 230 : 0), subj, to);
            f.kind = kind;
            if (to != null) to.acts.add(new Film.Act(t + 1.2f, t + 2.2f, Film.G_REACH));
            film.sfx.add(new Film.Sfx(Film.SFX_WHOOSH, t + 0.5f, 0.6f, 0.35f));
            d = Math.max(d, 2.6f);
        }
        if (Txt.has(s, INTO_WATER) && subj != null) {
            float x = xAt(subj, t) + subj.stateAt(t).facing * 120;
            Film.Fx f = fx(Film.FX_WATER_HIT, t + 0.6f, t + 4.8f, x, ground + 20, subj, null);
            f.color = 3;
            film.sfx.add(new Film.Sfx(Film.SFX_SPLASH, t + 0.6f, 1.2f, 0.8f));
            d = Math.max(d, 2.6f);
        }
        return d;
    }

    private Film.Fx fx(int type, float t0, float t1, float x, float y, Film.Actor a, Film.Actor b) {
        Film.Fx f = new Film.Fx(type, t0, t1);
        f.x = x; f.y = y; f.a = a; f.b = b;
        seg.fx.add(f);
        return f;
    }

    private void shake(float t0, float t1) { seg.fx.add(new Film.Fx(Film.FX_SHAKE, t0, t1)); }

    private void sub(float t0, float t1, String who, String text) {
        Film.Sub s = new Film.Sub();
        s.t0 = t0; s.t1 = t1; s.who = who; s.text = text;
        seg.subs.add(s);
    }

    /** x position of an actor at time t (interpolating walks). */
    public static float xAt(Film.Actor a, float t) {
        Film.Key prev = null, cur = a.keys.get(0);
        for (Film.Key k : a.keys) { if (k.t <= t) { prev = cur; cur = k; } else break; }
        if (cur.moveDur > 0 && t < cur.t + cur.moveDur && prev != null) {
            float u = (t - cur.t) / cur.moveDur;
            u = u * u * (3 - 2 * u);
            float px = prevX(a, cur);
            return px + (cur.x - px) * u;
        }
        return cur.x;
    }

    static float prevX(Film.Actor a, Film.Key k) {
        int i = a.keys.indexOf(k);
        if (i <= 0) return k.x;
        Film.Key p = a.keys.get(i - 1);
        // the previous key may itself be mid-move at k.t: start from where the character really is then, so a
        // new instruction never makes anyone jump
        if (p.moveDur > 0 && k.t < p.t + p.moveDur && k.t > p.t) return xAt(a, k.t - 1e-4f);
        return p.x;
    }

    private void moveNear(Film.Actor a, Film.Actor to, float t, float dur) {
        float ax = xAt(a, t), tx = xAt(to, t);
        float gap = 95 + (a.look.kind == Look.MONSTER || to.look.kind == Look.MONSTER ? 80 : 0);
        float dest = ax < tx ? tx - gap : tx + gap;
        if (Math.abs(dest - ax) < 10) return;
        Film.Key k = a.at(t);
        k.x = dest; k.moveDur = Math.max(0.3f, dur); k.facing = tx > dest ? 1 : -1;
        if (k.anchor != Film.A_GROUND && k.anchor != Film.A_CAGE) k.anchor = Film.A_GROUND;
    }

    private void runBackForth(Film.Actor a, float t, float dur) {
        float x0 = xAt(a, t);
        Film.Key k1 = a.at(t); k1.x = x0 + 220; k1.moveDur = dur * 0.45f; k1.run = true; k1.facing = 1;
        Film.Key k2 = a.at(t + dur * 0.5f); k2.x = x0 + 40; k2.moveDur = dur * 0.45f; k2.run = true; k2.facing = -1;
        Film.Key k3 = a.at(t + dur); k3.facing = 1;
        film.sfx.add(new Film.Sfx(Film.SFX_ANKLET, t, dur, a.look.anklets ? 0.45f : 0f));
        a.acts.add(new Film.Act(t, t + dur, Film.G_REACH));
    }

    // ------------------------------------------------------------------ camera

    private void cam(float t, float cx, float cy, float zoom, float ease) { seg.cams.add(new Film.Cam(t, cx, cy, zoom, ease)); }
    private void cam(float t, float cx, float cy, float zoom, float ease, float roll) { Film.Cam c = new Film.Cam(t, cx, cy, zoom, ease); c.roll = roll; seg.cams.add(c); }

    private float heightOf(Film.Actor a) { return Renderer.actorHeight(a.look, a.c, art, seg); }

    private void camOn(Film.Actor a, float t, float zoom) {
        float h = heightOf(a);
        Film.Key k = a.stateAt(t + 0.5f);
        float x = k.moveDur > 0 ? k.x : xAt(a, t);
        float y = ground - h * (zoom > 1.6f ? 0.8f : 0.62f);
        if (k.anchor == Film.A_BRANCH) y = ground - 450;
        if (k.body == Pose.LIE) y = ground - 60;
        cam(t, x, y, zoom, 0);
    }

    private void camGroup(List<Film.Actor> as, float t, float d) {
        float minX = 1e9f, maxX = -1e9f, maxH = 0;
        for (Film.Actor a : as) {
            float x = a.stateAt(t + 0.5f).x;
            minX = Math.min(minX, x); maxX = Math.max(maxX, x); maxH = Math.max(maxH, heightOf(a));
        }
        float span = maxX - minX + 420;
        float zoom = Math.max(1.0f, Math.min(1.4f, 1280f / span));
        float y = ground - maxH * 0.55f;
        cam(t, (minX + maxX) / 2, y, zoom, 0.5f);
    }

    /**
     * The dialogue shot, as the shot planner decided: size from the feeling, two-shot / over-the-shoulder /
     * single, camera height, composition with look room, isolation, and movement (static for strong performances).
     */
    private void camDialogue(Film.Actor sp, Film.Actor to, float t, int emo, ShotPlanner.Plan plan, Film.Line line) {
        if (plan == null) { plan = new ShotPlanner.Plan(); plan.intensity = ShotPlanner.intensity(emo, "", line.text); }
        float h = heightOf(sp);
        float sx = finalX(sp, t);
        Film.Key st = sp.stateAt(t);
        float faceY = ground - h * 0.8f;
        if (st.anchor == Film.A_SHOULDER || st.anchor == Film.A_BRANCH || st.anchor == Film.A_ON_FACE) faceY = ground - (st.anchor == Film.A_BRANCH ? 450 : 320);
        if (st.body == Pose.LIE || st.body == Pose.SIT) faceY = ground - h * 0.35f;
        int visible = 0;
        float nearest = 1e9f;
        for (Film.Actor a : seg.actors) {
            if (!a.stateAt(t).visible) continue;
            visible++;
            if (a != sp) nearest = Math.min(nearest, Math.abs(xAt(a, t) - sx));
        }
        boolean menace = !sp.look.hero && (emo == Pose.EVIL || emo == Pose.ANGRY);
        int size = plan.size, type = plan.type, height = menace ? 1 : plan.height, move = plan.move;
        if (to == null || visible <= 1) { if (type != ShotPlanner.SINGLE) type = ShotPlanner.SINGLE; }
        // a heavy feeling, alone: off-centre in a wider frame with empty space around (§6)
        boolean isolated = (emo == Pose.SAD || emo == Pose.SCARED) && (visible <= 1 || nearest > 520) && plan.stage != ShotPlanner.PEAK;
        if (isolated) { size = ShotPlanner.MWIDE; type = ShotPlanner.SINGLE; move = ShotPlanner.PULL_BACK; }
        // calm talk between the same two: keep the framing, no cut (use the simplest shot, §46)
        if (plan.hold && lastDlgShot != null && to != null && ((lastDlgA == sp && lastDlgB == to) || (lastDlgA == to && lastDlgB == sp))) {
            lastDlgShot.action += " • " + sp.c.shown() + ": \"" + clip(line.shown, 36) + "\"";
            return;
        }
        float zoom = ShotPlanner.zoomFor(size, h);
        float roll = menace ? (dlgCount % 2 == 0 ? 1 : -1) * 2.4f : 0;
        float cx, cy;
        String comp;
        float view = 1280f / zoom;
        if (type == ShotPlanner.TWO_SHOT && to != null) {
            float tx = finalX(to, t);
            float span = Math.abs(sx - tx) + 460;
            zoom = Math.max(1.0f, Math.min(zoom, 1280f / span));
            cx = (sx + tx) / 2; cy = ground - h * 0.55f;
            comp = "Both characters in the frame" + (Math.abs(sx - tx) < 260 ? ", close together (connection)" : Math.abs(sx - tx) > 600 ? ", far apart (distance between them)" : "");
        } else if (type == ShotPlanner.OTS && to != null) {
            float tx = finalX(to, t);
            cx = sx * 0.72f + tx * 0.28f; cy = ground - h * 0.62f;
            comp = "Over " + to.c.shown() + "'s shoulder onto " + sp.c.shown();
        } else if (isolated) {
            float away = 0;
            for (Film.Actor a : seg.actors) if (a != sp && a.stateAt(t).visible) away += Math.signum(sx - xAt(a, t));
            float side = away == 0 ? -st.facing : Math.signum(away);
            cx = sx - side * view * 0.27f; cy = ground - h * 0.55f;
            comp = sp.c.shown() + " small at the edge of the frame, empty space around (isolation)";
        } else {
            // a single: the speaker on a third, with room to look into (§17, §18)
            cx = sx + st.facing * view * (size >= ShotPlanner.CU ? 0.12f : 0.16f);
            // the face in the safe zone: about 40% from the top (20% headroom)
            cy = size >= ShotPlanner.MCU ? faceY + 0.1f * (720f / zoom) : ground - h * 0.6f;
            comp = sp.c.shown() + " on the " + (st.facing > 0 ? "left" : "right") + " third, looking " + (st.facing > 0 ? "right" : "left") + " with room in front";
        }
        // camera height: a low camera makes them strong, a high one makes them small (§5)
        if (height > 0) cy += h * 0.1f;
        else if (height < 0) cy -= h * 0.12f;
        if (emo == Pose.SCARED && sp.look.isChild() && height == 0) { height = -1; cy -= h * 0.1f; roll = -1.5f; }
        Film.Cam c = new Film.Cam(t, cx, cy, zoom, 0);
        c.roll = roll; c.angle = height; c.light = plan.light; c.still = move == ShotPlanner.STATIC;
        if (move == ShotPlanner.PULL_BACK) { c.zoom = Math.min(2.4f, zoom * 1.18f); c.still = false; }
        seg.cams.add(c);
        switch (move) {
            case ShotPlanner.PUSH_IN: {   // a realisation: the camera moves in with the thought
                Film.Cam pc = new Film.Cam(t + 0.15f, cx, cy - h * 0.03f, Math.min(2.45f, zoom * 1.14f), Math.max(1.5f, line.dur - 0.2f));
                pc.roll = roll; pc.angle = height; pc.light = plan.light;
                seg.cams.add(pc);
                break;
            }
            case ShotPlanner.PULL_BACK: { // release / isolation: back out to the larger situation
                Film.Cam pc = new Film.Cam(t + 0.1f, cx, cy, zoom, Math.max(2.5f, line.dur));
                pc.roll = roll; pc.angle = height; pc.light = plan.light;
                seg.cams.add(pc);
                break;
            }
            case ShotPlanner.DRIFT: {     // calm talk: the frame breathes a little
                Film.Cam pc = new Film.Cam(t + 0.05f, cx, cy - 2, zoom * 1.03f, 3f);
                pc.roll = roll; pc.angle = height; pc.light = plan.light;
                seg.cams.add(pc);
                break;
            }
            default:
        }
        Film.Shot sh = shot(t, size, type, height, move, sp, to, plan.stage);
        sh.line = line.index;
        sh.purpose = isolated ? "Show isolation: the character alone with the feeling" : plan.purpose;
        sh.action = sp.c.shown() + ": \"" + clip(line.shown, 48) + "\"";
        sh.face = faceOf(emo);
        sh.body = bodyOf(emo, sp.look);
        sh.light = plan.light;
        sh.cutWhen = plan.reaction ? "the line lands — cut to the listener's reaction" : "the next line begins";
        sh.emotionalPurpose = ShotPlanner.STAGE_NAME[plan.stage] + ": " + (plan.intensity >= 0.6f ? "a strong moment — the camera stays simple" : plan.intensity >= 0.4f ? "the feeling grows" : "keep the conversation easy to follow");
        sh.other = comp;
        lastDlgShot = sh; lastDlgA = sp; lastDlgB = to;
    }

    static String clip(String s, int n) { s = s == null ? "" : s.replace('\n', ' '); return s.length() > n ? s.substring(0, n) + "…" : s; }

    /** Records a planned shot for the shot list. */
    private Film.Shot shot(float t, int size, int type, int height, int move, Film.Actor subject, Film.Actor other, int stage) {
        Film.Shot sh = new Film.Shot();
        sh.t = t; sh.size = size; sh.type = type; sh.height = height; sh.move = move; sh.stage = stage; sh.part = partNo;
        sh.subject = subject == null ? "" : subject.c.shown();
        sh.sound = "Dialogue in the room of " + Sets.label(seg.set) + ", " + Sets.name(seg.set) + " ambience";
        film.shots.add(sh);
        return sh;
    }

    static String faceOf(int emo) {
        switch (emo) {
            case Pose.HAPPY: return "A warm smile, cheeks lifted";
            case Pose.LAUGH: return "Laughing, eyes squeezed";
            case Pose.SAD: return "Eyes lowered, inner brows up, mouth corners down";
            case Pose.ANGRY: return "Brows down and together, narrowed eyes";
            case Pose.SCARED: return "Wide eyes, raised inner brows";
            case Pose.SURPRISED: return "Brows up, eyes wide, mouth open";
            case Pose.EVIL: return "A crooked smile under lowered brows";
            case Pose.DETERMINED: return "Set jaw, steady eyes";
            case Pose.PROUD: return "Chin up, a small smile";
            case Pose.CURIOUS: return "One brow raised";
            case Pose.PAIN: return "Squeezed eyes, a frown";
            default: return "Calm, listening";
        }
    }

    static String bodyOf(int emo, Look l) {
        String e = l.energy > 1.1f ? "quick, big gestures" : l.energy < 0.9f ? "small, slow movements" : "natural gestures";
        String p = l.poise > 0.5f ? ", upright and proud" : l.poise < -0.5f ? ", shoulders in, head a little down" : "";
        switch (emo) {
            case Pose.SAD: return "Shoulders drop, head down" + p;
            case Pose.SCARED: return "Leans back, arms in" + p;
            case Pose.ANGRY: return "Leans in, arms out, " + e + p;
            case Pose.HAPPY: case Pose.LAUGH: return "Open posture, " + e + p;
            default: return e.substring(0, 1).toUpperCase() + e.substring(1) + p;
        }
    }

    private float finalX(Film.Actor a, float t) {
        Film.Key k = a.stateAt(t);
        if ((k.anchor == Film.A_SHOULDER || k.anchor == Film.A_CARRIED || k.anchor == Film.A_ON_FACE) && k.anchorActor != null) return finalX(k.anchorActor, t);
        return k.x;
    }

    // ------------------------------------------------------------------ sound design

    static String ambWords(Film.Seg s) {
        String w;
        switch (s.set) {
            case Sets.CAVE_IN: case Sets.CAVE_MOUTH: w = "cave गुफा"; break;
            case Sets.FOREST: w = s.tod == Sets.NIGHT || s.tod == Sets.EVENING ? "jungle night जंगल रात" : "forest जंगल birds"; break;
            case Sets.CELEBRATION: w = "festival crowd mela उत्सव"; break;
            case Sets.HALL: w = "palace hall महल"; break;
            case Sets.VILLAGE: w = "village गाँव"; break;
            case Sets.GATE: case Sets.COURTYARD: w = "palace महल " + (s.tod == Sets.NIGHT ? "night" : "birds"); break;
            default: w = s.tod == Sets.NIGHT ? "night रात crickets" : "garden बगीचा birds morning";
        }
        return w;
    }

    private void ambience(String where, float t) {
        if (Txt.has(where, "झरन", "कल-कल")) film.sfx.add(new Film.Sfx(Film.SFX_STREAM, t, 8, 0.25f));
        if (Txt.has(where, "चिड़िय", "चह-चह", "सुबह", "सूर्योदय", "बगीच")) film.sfx.add(new Film.Sfx(Film.SFX_BIRDS, t, 8, 0.3f));
        if (Txt.has(where, "हवा")) film.sfx.add(new Film.Sfx(Film.SFX_WIND, t, 8, 0.25f));
        if (seg.set == Sets.CAVE_IN) film.sfx.add(new Film.Sfx(Film.SFX_DRIP, t, 30, 0.35f));
        if (seg.set == Sets.FOREST && (seg.tod == Sets.NIGHT || seg.tod == Sets.EVENING)) film.sfx.add(new Film.Sfx(Film.SFX_NIGHT, t, 30, 0.35f));
        if (seg.festive) { film.sfx.add(new Film.Sfx(Film.SFX_DRUMS, t, 12, 0.45f)); film.sfx.add(new Film.Sfx(Film.SFX_CROWD, t, 6, 0.25f)); }
    }

    private int moodOf(Story.Scene sc, int b0, int b1, String where, boolean villains) {
        StringBuilder all = new StringBuilder(where);
        for (int i = b0; i < b1; i++) all.append(' ').append(sc.beats.get(i).text).append(' ').append(sc.beats.get(i).manner);
        String a = all.toString();
        if (seg.festive || Txt.has(a, "ढोल", "उत्सव", "जश्न")) return Film.M_CELEBRATE;
        if (Txt.has(a, "मुकाबला", "जाल फेंक", "चिल्लाते", "नाक पर", "लड़ाई", "fight")) return Film.M_ACTION;
        if (Txt.has(a, "गायब हो गई", "रोते", "फूट-फूट", "अनर्थ")) return Film.M_SAD;
        if (villains && seg.set == Sets.CAVE_IN) return Film.M_VILLAIN;
        if (Txt.has(a, "अपहरण", "बेहोश", "फट जाता")) return Film.M_TENSE;
        if (villains) return Film.M_TENSE;
        if (seg.tod == Sets.NIGHT || seg.set == Sets.FOREST) return Film.M_NIGHT;
        if (Txt.has(a, "बंदर", "शैतानी", "नटखट", "पगड़ी")) return Film.M_PLAYFUL;
        return Film.M_HAPPY;
    }
}
