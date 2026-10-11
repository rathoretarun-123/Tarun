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
        /** Spider-Verse: characters animated on twos / threes by skill (experts 24, learners 12, rebels 8 fps); off = every frame. */
        public boolean onTwos;
        /**
         * v35: the height of the film in pixels (1080 for a 1080p film): a close-up never enlarges a picture more than
         * 1.5 times its own pixels at this size. 720 = the stage itself (the rule as it was before v35).
         */
        public int outHeight = 720;
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
    /** v35: who was asleep (lying, eyes shut) at the end of the last part. */
    private final Map<Story.CharacterDef, Boolean> pAsleep = new HashMap<Story.CharacterDef, Boolean>();

    /** v35: does this part wake the character (or take them out of bed) before they say anything? */
    private boolean wakesIn(Story.Scene sc, int b0, int b1, Story.CharacterDef c) {
        for (int bi = b0; bi < b1 && bi < sc.beats.size(); bi++) {
            Story.Beat b = sc.beats.get(bi);
            if (b.type == Story.Beat.DIALOGUE) { if (b.speaker == c) return false; continue; }
            for (String sent : sentences(b.text)) {
                if (!ScriptParser.mentions(story, sent).contains(c)) continue;
                if (Txt.has(sent, WAKE) || (Txt.has(sent, STAND_UP) && Txt.has(sent, BED))) return true;
                return false;
            }
        }
        return false;
    }

    /** v35: a picture the story would be better with — a character seen sitting or lying down. */
    public static final class PoseNeed {
        public Story.CharacterDef c;
        /** PoseSense.SIT or PoseSense.LIE. */
        public int pose;
        public int scene;
    }

    /**
     * v35: who sits or lies down in the story (the first named in the stage direction), once per pose. A user's
     * standing photo can only be lowered onto a seat (the thighs shortened as if coming towards the camera) or turned
     * to lie down, so the director asks for a picture of them sitting or lying — and uses it, as it uses every
     * picture of the user's, whenever the character sits or lies (Casting).
     */
    public static List<PoseNeed> poseNeeds(Story story) {
        List<PoseNeed> out = new ArrayList<PoseNeed>();
        for (Story.Scene sc : story.scenes) for (Story.Beat b : sc.beats) {
            if (b.type != Story.Beat.DIRECTION) continue;
            for (String sent : sentences(b.text)) {
                int pose = Txt.has(sent, LIE_DOWN) || Txt.has(sent, WAKE) ? PoseSense.LIE
                        : Txt.has(sent, SIT_DOWN) && !Txt.has(sent, "कंधे पर", "नाक पर", "सिर पर", "पीठ पर", "डाल पर", "shoulder") && !Txt.has(sent, KNEEL_WORDS) && vehicleIn(sent, false) == 0 ? PoseSense.SIT : -1;
                if (pose < 0) continue;
                List<Story.CharacterDef> m = ScriptParser.mentions(story, sent);
                if (m.isEmpty()) continue;
                Story.CharacterDef c = m.get(0);
                if (pose == PoseSense.SIT && creatureSits(sent, c)) continue;
                if (c.look == null || !c.look.isHumanoid() || c.look.aid == Look.AID_WHEELCHAIR) continue;
                boolean dup = false;
                for (PoseNeed n : out) if (n.c == c && n.pose == pose) dup = true;
                if (dup) continue;
                PoseNeed n = new PoseNeed();
                n.c = c; n.pose = pose; n.scene = sc.number;
                out.add(n);
            }
        }
        return out;
    }

    /** v35: does the character speak in this part before any action names them? */
    private boolean speaksFirst(Story.Scene sc, int b0, int b1, Story.CharacterDef c) {
        for (int bi = b0; bi < b1 && bi < sc.beats.size(); bi++) {
            Story.Beat b = sc.beats.get(bi);
            if (b.type == Story.Beat.DIALOGUE) { if (b.speaker == c) return true; continue; }
            if (ScriptParser.mentions(story, b.text).contains(c)) return false;
        }
        return false;
    }
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
        // the director's manual (2.6): suspicion and relief are directing cues of their own
        if (Txt.has(m, "शक", "संदेह", "शंका", "suspicious", "suspicion", "doubtful", "warily", "wary")) return Pose.SUSPICIOUS;
        if (Txt.has(m, "राहत", "चैन की साँस", "चैन की सांस", "सुकून", "relieved", "relief", "sigh of relief")) return Pose.RELIEVED;
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

        // ---------------- the story engine (PixarLead): the hero, the six-beat spine and the act of every scene
        sceneActs = PixarLead.acts(story);
        film.spine = PixarLead.spine(story);
        Story.CharacterDef heroDef = PixarLead.hero(story);
        film.hero = heroDef == null ? "" : heroDef.shown();
        maPauses = 0; comicBeats = 0; shadowPasses = 0; framedHead = 0; framedFeet = 0; framedFace = 0; faceFillSum = 0; faceFillN = 0;
        thoughtBeats = 0; dutchCount = 0; dutchUsed = 0; usedInserts.clear(); povShots = 0; loudReactions = 0; bridges = 0;

        // ---------------- scenes
        for (int si = 0; si < story.scenes.size(); si++) {
            Story.Scene sc = story.scenes.get(si);
            if (!opt.sceneCards) {
                // no title cards: a soft "whoosh" transition into the next scene
                if (si > 0) film.sfx.add(new Film.Sfx(Film.SFX_WHOOSH_CARD, t - 0.3f, 1.0f, 0.35f));
                // the phone guide (8.2) and the manual (3.2): a new place is established before anyone speaks in it — a
                // bridge of the place alone (its picture, a slow push) when the place changes between two scenes
                int newSet = Sets.forScene(sc);
                Film.Seg last = film.segs.isEmpty() ? null : film.segs.get(film.segs.size() - 1);
                // v26: the user's own picture of the journey into this place (ScenePlan) is the bridge when given
                Art.Backdrop journey = this.art.journeyBackdrop(sc.number);
                Art.Backdrop plate = journey != null ? journey : this.art.sceneBackdrop(sc.number, 0);
                if (si > 0 && last != null && last.type == Film.S_SCENE && (last.set != newSet || (journey != null && ScenePlan.bridgeBefore(story, si))) && plate != null) {
                    Film.Seg bridge = new Film.Seg();
                    bridge.type = Film.S_CARD;
                    bridge.scene = si;
                    bridge.t0 = t;
                    bridge.t1 = t + (journey != null ? 2.6f : 2.4f);
                    bridge.text1 = "";
                    bridge.text2 = "";
                    bridge.set = newSet;
                    bridge.tod = Sets.detectTime(sc.setting, Sets.DAY);
                    bridge.backdrop = plate;
                    bridge.fadeIn = 0.3f; bridge.fadeOut = 0.3f;
                    film.segs.add(bridge);
                    film.notes.add("Bridge before " + sc.heading + ": the new place established on its own (" + Sets.label(newSet) + ") before anyone speaks there" + (journey != null ? " — your picture of the way there" : ""));
                    bridges++;
                    t = bridge.t1;
                }
            } else {
            Film.Seg card = new Film.Seg();
            card.type = Film.S_CARD;
            card.scene = si;
            card.t0 = t;
            card.t1 = t + 3.0f;
            card.text1 = sc.heading;
            card.text2 = sc.title;
            card.set = Sets.forScene(sc);
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
        // v40: with no picture made for the title or the end, they are drawn on the story's own place — the user's
        // picture of it, at its hour — never the studio's daylight palace garden after a night by a fire
        if (this.art.title == null || this.art.end == null) {
            Film.Seg first = null, last = null;
            for (Film.Seg sg : film.segs) if (sg.type == Film.S_SCENE && sg.backdrop != null && sg.backdrop.picture && !sg.backdrop.object) { if (first == null) first = sg; last = sg; }
            if (this.art.title == null && first != null) { title.backdrop = first.backdrop; title.set = first.set; title.tod = first.tod; title.ground = first.ground; }
            if (this.art.end == null && last != null) { end.backdrop = last.backdrop; end.set = last.set; end.tod = last.tod; end.ground = last.ground; }
        }
        end.fadeIn = 0.8f; end.fadeOut = 1.2f;
        film.segs.add(end);
        film.music.add(new Film.Music(Film.M_END, t, end.t1));
        film.sfx.add(new Film.Sfx(Film.SFX_END_CHORD, t + 0.4f, 5f, 0.9f));
        film.sfx.add(new Film.Sfx(Film.SFX_MAGIC, t + 0.6f, 2f, 0.5f));
        film.notes.add("End page: \"" + end.text1 + "\"" + (this.art.end != null ? " (your picture)" : " (made by the studio)") + " + music");
        film.duration = end.t1;
        mobility();
        umbrellaRain();
        walkSounds();
        settleStays();
        steadyRiders();
        vehicleDoors();
        placeSteps();
        scoreMusic();
        if (opt.technical) {
            // the Technical Director protocol, in its own order: lock the camera, then no shot may move a character
            // more than 15% of the frame, hold two actions, or last longer than 4 seconds
            calmMoves();
            lockCameras();
            enforceMotion();
            oneActionPerShot();
            limitShotLength(TechnicalDirector.CUT_SECONDS);
        }
        film.shotList = qualityCheck();
        // the Braintrust (PixarLead): the four questions every five shots, suggestions only — appended to the shot list
        film.braintrust = PixarLead.braintrust(film, story);
        film.shotList += "\n" + film.braintrust.text;
        // the handbook's continuity ledger (ch. 12)
        film.shotList += "\n" + Handbook.ledger(film, story);
        // the director's manual: the beat sheet (3.1), the scene records (3.3), the coverage report (3.2), the prop ledger and the location records (1.6)
        film.shotList += DirectorsManual.records(film, story, film.stats);
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
                // v41: the place's opening wide is held whole up to the cap (the manual's establishing shot), not cut
                // in two at the editing length
                boolean open = openingWide(sg, i);
                if (open && next - c.t <= TechnicalDirector.MAX_SHOT_SECONDS - 0.05f) continue;
                // ... and a longer one keeps its first stretch whole (the hold), the rest is cut at the editing length
                float base = c.t;
                if (open && next - c.t - (ESTAB_HOLD + 0.4f) >= 0.8f) base = c.t + ESTAB_HOLD + 0.4f;
                int n = (int) Math.ceil((next - base) / max);      // v34: every piece under the cut (2.2 s), not under the 3-s nominal shot
                float step = (next - base) / n;
                List<Float> times = new ArrayList<Float>();
                if (base > c.t) times.add(base);
                for (int k = 1; k < n; k++) times.add(base + k * step);
                for (int k = 1; k <= times.size(); k++) {
                    boolean tight = k % 2 == 1;
                    // v41: never a cut-in in the middle of a jump (a puddle's splash at the feet): after it lands, or none
                    float tk = times.get(k - 1);
                    if (tight) {
                        // v42: ... or before it takes off, when waiting for the landing would hold the shot past the
                        // cap (an opening wide still keeps its whole hold)
                        float prev = k >= 2 ? times.get(k - 2) : c.t, least = k == 1 && open ? ESTAB_HOLD : 0.8f;
                        for (Film.Actor a : sg.actors) for (Film.Act x : a.acts)
                            if (x.type == Film.G_JUMP && tk > x.t0 - 0.2f && tk < x.t1 + 0.6f && a.stateAt(tk).visible) {
                                float before = x.t0 - 0.25f;
                                tk = x.t1 + 0.6f - prev > TechnicalDirector.MAX_SHOT_SECONDS - 0.05f && before - prev >= least ? before : x.t1 + 0.6f;
                            }
                        if (tk > next - 0.6f) continue;
                    }
                    // the second framing of a long hold: closer for a wider shot; for a close-up a touch wider (a
                    // close-up never gets tighter here, so a head, hair or a turban is never cut)
                    boolean close = c.zoom >= ShotPlanner.ZOOM[ShotPlanner.CU] * 0.95f;
                    float z2 = close ? Math.max(1f, c.zoom / 1.18f) : Math.min(Math.max(ShotPlanner.MAX_ZOOM, c.zoom), c.zoom * 1.2f);
                    float y2 = close ? c.cy : c.cy - 14 / c.zoom;
                    Film.Cam d = new Film.Cam(tk, c.cx, tight ? y2 : c.cy, tight ? z2 : c.zoom, 0);
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
                for (Film.Shot p : planned) if (Math.abs(p.t - c.t) < 0.25f && (match == null || Math.abs(p.t - c.t) < Math.abs(match.t - c.t))) match = p;
                if (match != null) planned.remove(match);            // a planned shot describes one cut, never two
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
                    // a cut inside a cinematic picture (a long still split so no shot is longer than 4 s): the picture stays
                    for (Film.Fx f : sg.fx) if (f.type == Film.FX_SHOT && c.t > f.t0 + 0.05f && c.t < f.t1 - 0.05f) {
                        sh.purpose = "The cinematic picture of the moment, continued (a long still cut so no shot is longer than 4 s)";
                        sh.subject = "the moment, as a picture";
                        sh.type = ShotPlanner.SINGLE;
                        sh.size = ShotPlanner.WIDE;
                        who = null;
                        break;
                    }
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
    /**
     * Two cuts within a few frames of each other would flash one frame of the first (a planned cut and the lip-sync
     * cut at the line's start): the later one is the shot, the earlier is dropped (the opening cut of a part stays;
     * then the later one goes). A cut to almost the same framing jars (a jump cut): the shot runs on instead (with
     * a locked camera), or glides there. Returns the jump cuts taken out.
     */
    private int cleanCuts(Film.Seg sg) { return cleanCuts(sg, true); }

    private int cleanCuts(Film.Seg sg, boolean jumps) {
        int jump = 0;
        List<Film.Cam> flash = new ArrayList<Film.Cam>();
        Film.Cam prevC = null;
        for (Film.Cam c : sg.cams) {
            if (c.ease > 0) continue;
            if (prevC != null && c.t - prevC.t < 0.3f) {
                // (the opening cut of a part stays; v41: so does a story cut kept on purpose — the later one goes)
                if (prevC.t <= sg.t0 + 0.01f || (prevC.keep && !c.keep)) { flash.add(c); continue; }
                flash.add(prevC);
            }
            prevC = c;
        }
        sg.cams.removeAll(flash);
        if (!jumps) return 0;
        Film.Cam prevCut = null;
        List<Film.Cam> drop = new ArrayList<Film.Cam>();
        for (Film.Cam c : sg.cams) {
            if (c.ease > 0) continue;
            if (!c.keep && prevCut != null && Math.abs(c.zoom - prevCut.zoom) < 0.1f && Math.abs(c.cx - prevCut.cx) < 55 && Math.abs(c.cy - prevCut.cy) < 40
                    && c.t - prevCut.t > 0.3f) { if (opt.technical) drop.add(c); else c.ease = 0.8f; jump++; }
            else prevCut = c;
        }
        sg.cams.removeAll(drop);
        return jump;
    }

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
            jump += cleanCuts(sg);
        }
        if (opt.technical) {
            // the protocol's passes again until nothing changes (a reframed shot has a different frame width)
            for (int pass = 0; pass < 3; pass++) {
                int before = motionCuts + actionCuts;
                limitShotLength(TechnicalDirector.CUT_SECONDS);
                safeFrames();
                enforceMotion();
                oneActionPerShot();
                if (motionCuts + actionCuts == before) break;
            }
            limitShotLength(TechnicalDirector.CUT_SECONDS);
            safeFrames();
            // the last word goes to the motion and action limits (their new shots are framed on the action)
            enforceMotion();
            oneActionPerShot();
            limitShotLength(TechnicalDirector.CUT_SECONDS);
        }
        playFrames();
        // v41: the passes above add cuts of their own (a shot cut in two, a play framed): no flash cut may come back
        // with them — and the shots stay under the cap with one action each after the clean-up
        for (Film.Seg sg : film.segs) if (sg.type == Film.S_SCENE) { sortCams(sg); jump += cleanCuts(sg); }
        if (opt.technical) {
            oneActionPerShot();
            limitShotLength(TechnicalDirector.CUT_SECONDS);
            for (Film.Seg sg : film.segs) if (sg.type == Film.S_SCENE) { sortCams(sg); cleanCuts(sg, false); }
        }
        shotsFromCuts();
        if (opt.technical) validateShots();
        int longest = 0;
        for (Film.Shot sh : film.shots) {
            if (!opt.technical && !sh.reaction && sh.stage == ShotPlanner.PEAK && sh.move != ShotPlanner.STATIC && sh.move != ShotPlanner.PUSH_IN) { sh.move = ShotPlanner.STATIC; still++; }
            if (sh.dur > TechnicalDirector.MAX_SHOT_SECONDS + 0.05f) longest++;
        }
        // v27: the user's own pictures of every character, chosen once per shot (angle, pose, feeling), before the list is written
        Casting.cast(this.art, film, story);
        StringBuilder b = new StringBuilder();
        b.append("DIRECTOR'S SHOT LIST — ").append(story.title).append('\n');
        b.append("Planned with the Pixar-style directing guide: emotion → performance → composition → camera → light → sound → cut.\n\n");
        int n = 0, cus = 0, reactions = 0, statics = 0, twos = 0;
        int part = -2;
        Film.Shot prevShot = null;
        Map<Story.CharacterDef, String> cids = DirectorsManual.charIds(story);
        Map<Integer, String> lids = DirectorsManual.locIds(film);
        for (Film.Shot sh : film.shots) {
            boolean newPart = sh.part != part;
            if (sh.part != part) {
                part = sh.part;
                Film.Seg sg = film.segAt(sh.t + 0.01f);
                if (sg != null && sg.scene >= 0) b.append("────────── ").append(story.scenes.get(sg.scene).heading).append(": ").append(Sets.label(sg.set))
                        .append(sg.transition == 1 ? "  (opens with a dip to black: time has passed)" : sg.transition == 2 ? "  (opens with a dip to white)" : "").append('\n');
                // v30: the structured scene brief of the training guide (§7)
                if (sg != null && sg.scene >= 0) b.append(DirectorTraining.brief(film, story, sg, sh, cids));
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
            b.append("EMOTIONAL PURPOSE: ").append(sh.emotionalPurpose).append('\n');
            // the handbook's record (ch. 5, 6, 15)
            Film.Seg sgh = film.segAt(sh.t + 0.01f);
            int sceneNo = sgh != null && sgh.scene >= 0 && sgh.scene < story.scenes.size() ? story.scenes.get(sgh.scene).number : 0;
            sh.id = Handbook.shotId(sceneNo, n, 1);
            sh.lens = Handbook.focalFor(sh.size, opt.aspect);
            sh.gaze = Handbook.gaze(sh);
            sh.attention = Handbook.attention(sh);
            b.append("SHOT ID: ").append(sh.id).append("   LENS: ").append(sh.lens).append("   ANGLE: ").append(Handbook.angleMeaning(sh.height)).append('\n');
            if (sh.ots.length() > 0) b.append("OVER THE SHOULDER: ").append(sh.ots).append("'s shoulder and back in the foreground, soft (the back view made from the picture)\n");
            sh.view = sh.pictures.isEmpty() ? SceneMaker.viewsUsed(this.art, film, sh) : Casting.describe(this.art, film, sh);
            if (sh.view.length() > 0) b.append("PICTURES USED: ").append(sh.view).append('\n');
            b.append("FIVE QUESTIONS: see — ").append(sh.action).append(" | feel — ").append(sh.emotionalPurpose).append(" | attention first — ").append(sh.attention)
                    .append(" | reveals — ").append(sh.purpose).append(" | why this camera — ").append(Handbook.purposeOf(sh.size, sh.type)).append('\n');
            b.append("GAZE: ").append(sh.gaze).append('\n');
            // the director's manual (3.5, 1.7, 3.8, 3.9): the reference package, the transition, the state at both ends, the audio
            b.append("ASSETS: ").append(DirectorsManual.assets(film, story, sh, cids, lids)).append('\n');
            b.append("TRANSITION IN: ").append(DirectorsManual.transition(film, prevShot, sh, newPart)).append('\n');
            b.append("STATE AT START: ").append(DirectorsManual.state(film, sh.t + 0.05f)).append('\n');
            b.append("STATE AT END: ").append(DirectorsManual.state(film, sh.t + Math.max(0.1f, sh.dur - 0.05f))).append('\n');
            b.append("VOICE / MUSIC: ").append(DirectorsManual.voiceMusic(film, story, sh, cids)).append("\n\n");
            prevShot = sh;
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
            b.append(String.format(java.util.Locale.US, "• Moves read from the story's directions (v34): %d leaving, %d coming back, %d across or around, %d up to someone; "
                    + "%d lines waited until their speaker had walked onto the stage (v35)%n", exits, returns, crossings, approaches, waitedToEnter));
            b.append(String.format(java.util.Locale.US, "• Realism (v34): %d looks held on an entrance, an exit, a return, a reveal or someone hurt (%d turned round to watch); "
                    + "%d reactions to rain or snow beginning; %d hurried out of the rain; %d felt their way blindfolded; %d doors heard; %d footsteps moved across the stereo%n",
                    watchCount, watchTurns, skyReactions, hurried, feltWay, doorsHeard, stepsPlaced));
            b.append(bodiesReport());
            if (sittings + risings + sleepers + wakings + meals + drinks > 0)
                b.append(String.format(java.util.Locale.US, "• Everyday actions (v35): %d sat down (a chair, a sofa, a bed, the floor — each its own furniture and sound), %d got up "
                        + "(leaning forward, slower from a low seat; from lying, sitting up first), %d lay down to sleep (sat, then lay back; eyes shut, slow breath, a blanket in bed), "
                        + "%d woke (sat up, stretched and yawned), %d ate (hand to mouth, chewing, heard), %d drank (raised to the lips, a sip or gulps, the cup set down); "
                        + "%d shots of a seated speaker from a standing photo kept from the waist up, %d close-ups of a photo eating or drinking (the cup or morsel comes up to the lips)%n",
                        sittings, risings, sleepers, wakings, meals, drinks, waistUp, mealCloseUps));
            if (rides + playground + exercises + makeups + baths + screens + pecks + hugs > 0)
                b.append(String.format(java.util.Locale.US, "• Activities (v37): %d rides (a bicycle, a motorbike, a scooter, a car, a bus, an auto — the wheels turn with the distance, the engine or the bell heard), "
                        + "%d on the playground (a swing, a slide, a see-saw, a merry-go-round), %d exercises (dumbbells, a skipping rope, squats, yoga, push-ups), "
                        + "%d make-up (kajal, lipstick, a bindi, mehndi, powder, face paint — it stays on), %d baths (behind a curtain from the shoulders down, water from a mug, then a towel), "
                        + "%d changes of clothes behind a folding screen, %d pecks on the cheek or the forehead, %d hugs%n",
                        rides, playground, exercises, makeups, baths, screens, pecks, hugs));
            if (tasks + lightingShots > 0)
                b.append(String.format(java.util.Locale.US, "• Everyday tasks (v36): %d tasks shown with their tool and sound (cooking at the stove, sweeping, washing, reading, writing, "
                        + "a phone call, brushing teeth, combing hair, watering plants); %d shots framed on a lamp, candle, torch or fire being lit%n", tasks, lightingShots));
            b.append(String.format(java.util.Locale.US, "• Cuts made so no character moves 15%% of the frame in one shot: %d; shots still over the limit (very fast moves the story's timing leaves no room to slow): %d%n", motionCuts, overMotion));
            b.append(String.format(java.util.Locale.US, "• Cuts made so each shot holds one action: %d; shots with more than one: %d%n", actionCuts, multi));
            b.append(String.format(java.util.Locale.US, "• Shots reframed for the %s frame (whole characters, 15%% side margins, headroom): %d%n", opt.aspect, framed));
            b.append(String.format(java.util.Locale.US, "• Validation layer: %d of %d shots passed at once, %d corrected%s%n", passed, n, corrected,
                    stillWrong.isEmpty() ? "" : "; could not fully correct: " + stillWrong));
            PixarLead.Format fmt = PixarLead.spec(opt.aspect);
            b.append("• Pixar-Lead protocol v4.0: format ").append(fmt.id).append(" (").append(fmt.note).append("), character scale lock ")
                    .append(Math.round(fmt.charScale * 100)).append("% of the frame height, two lights only (key + bounce), colour script per act\n");
            b.append(String.format(java.util.Locale.US, "• First-frame checks: %d shots reframed for a cut head, %d for feet out of the frame (feet in the bottom 85-98%% with their shadow)%n", framedHead, framedFeet));
            b.append(String.format(java.util.Locale.US, "• Lip-sync framing (8.3): the face fills %.0f%% of the frame on average in %d lip-sync shots (65-75%% where the picture and the "
                    + "frame allow it; the whole head always inside; a narrow frame keeps the face in its centre 60%%); reframed on the face: %d%n",
                    faceFillN == 0 ? 0 : 100 * faceFillSum / faceFillN, faceFillN, framedFace));
            b.append(String.format(java.util.Locale.US, "• Objective (small films of 30-90 s): this film is %d:%02d — the script decides the length; it is made as %d shots of about 3 s, "
                    + "each one checked on its own (the Pixar Test is passed shot by shot, not by the minute)%n", (int) (film.duration / 60), Math.round(film.duration) % 60, n));
            b.append(String.format(java.util.Locale.US, "• Miyazaki ma pauses after two fast beats: %d; Russo / Gunn comic beats: %d; Gunn shadow passes in funny scenes: %d%n", maPauses, comicBeats, shadowPasses));
            b.append("• Story spine filled before any shot was planned (R4, hardcoded): yes — hero ").append(film.hero.length() > 0 ? film.hero : "—").append("; the ending was read first (R3)\n");
            b.append("• Spider-Verse animation on twos: ").append(opt.onTwos ? "on (experts 24, learners 12, rebels 8 fps)" : "off (every character moves every frame; switch it on in Settings)").append('\n');
            b.append("• Nolan: real sounds — steps by the floor of the place (stone, marble, cave, earth), running steps for runs; cross-cutting between speaker and listener in long lines\n");
            java.util.Set<Integer> durs = new java.util.HashSet<Integer>();
            for (Film.Shot sh : film.shots) durs.add(Math.round(sh.dur * 4));
            b.append(String.format(java.util.Locale.US, "• Handbook (AI Animation Director): the five questions answered for every shot, a stable shot ID and a lens by shot size; "
                    + "thought-before-action beats (a pause and a look before the reaction): %d; Dutch angles: %d (at most one per scene); shot durations: %d distinct lengths (never the same for every shot); "
                    + "the continuity ledger per scene and the four approval gates follow below%n", thoughtBeats, dutchCount, durs.size()));
            Handbook.Stats hs = new Handbook.Stats();
            hs.shots = n; hs.speech = speech; hs.over6 = over6; hs.overMotion = overMotion; hs.multi = multi; hs.longest = longest; hs.jumpRemoved = jump;
            hs.estabFixed = estab; hs.passed = passed; hs.corrected = corrected; hs.stillWrong = stillWrong.size(); hs.reactions = reactions;
            hs.thoughtBeats = thoughtBeats; hs.dutch = dutchCount; hs.calmedRuns = calmed; hs.spine = film.hero.length() > 0; hs.faceFill = faceFillN == 0 ? 0 : faceFillSum / faceFillN;
            hs.durationsDistinct = durs.size();
            hs.pov = povShots; hs.loudReactions = loudReactions;
            for (Film.Shot sh : film.shots) if (sh.stage == ShotPlanner.ESTABLISH && sh.purpose.startsWith("Establish")) { hs.estabTotal++; if (sh.dur >= 2.4f) hs.estabHeld++; }
            b.append(String.format(java.util.Locale.US, "• Director's manual (v2.0): establishing shots held for the cut length (2.5 s; shots are cut at 2.6 s since v26, the 4-s cap stays): %d of %d (the rest are cut sooner by the "
                    + "protocol's motion rule — someone enters or moves during them, and the move is seen wide); point-of-view shots for looks: %d; "
                    + "reactions staged for loud sounds: %d; suspicion and relief read from the manners; every shot carries its ASSETS, TRANSITION IN, STATE AT START / END and "
                    + "VOICE / MUSIC lines; the beat sheet, scene records, coverage report, prop ledger and location records follow the ledger%n", hs.estabHeld, hs.estabTotal, povShots, loudReactions));
            b.append(String.format(java.util.Locale.US, "• Phone-local guide (v1.0): establishing bridges added where the place changes between scenes: %d; reverse shots use the place's reverse angle when you gave one; "
                    + "real uploaded angles replace made views; no painted mouth where no mouth was found%n", bridges));
            film.stats = hs;
        } else {
            b.append(String.format(java.util.Locale.US, "• Close-ups kept for turning points: %d of %d shots (%.0f%%)%n", cus, n, n == 0 ? 0 : 100f * cus / n));
        }
        // v30: the training guide's weighted score of the film (§12), before the final QC refines it
        b.append(DirectorTraining.scoreCard(film, this.art, story, null));
        b.append(SituationsGuide.report(story, film, eyeLevelShots, blindNoPov, reveals, concernBeats, reliefBeats));
        b.append(FilmCraft.report(this.art, film));          // v38: the animated director's craft guide
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
            case Pose.SUSPICIOUS: return base == Film.M_CELEBRATE ? base : Film.M_TENSE;
            case Pose.RELIEVED: return base == Film.M_TENSE || base == Film.M_VILLAIN || base == Film.M_ACTION ? Film.M_HAPPY : base;
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
    /**
     * v41 (blocking): two people never stand inside each other. After a beat, where two are left standing on the
     * same spot once they have arrived (out of a car, after a walk to the same place) — not in a hug, hand in hand,
     * riding or playing together — the one who came last steps aside to free ground (sitting and lying people are
     * left where they are).
     */
    private void unstack(float tc) {
        for (int p = 0; p < seg.actors.size(); p++) for (int q = p + 1; q < seg.actors.size(); q++) {
            Film.Actor A = seg.actors.get(p), B = seg.actors.get(q);
            if (A.look == null || B.look == null || !A.look.isHumanoid() || !B.look.isHumanoid() || A.c.voiceOnly || B.c.voiceOnly) continue;
            float t = Math.max(tc, Math.max(arrivalAfter(A, tc), arrivalAfter(B, tc)));
            Film.Key ka = A.stateAt(t), kb = B.stateAt(t);
            if (!ka.visible || !kb.visible || ka.anchor != Film.A_GROUND || kb.anchor != Film.A_GROUND) continue;
            float xa = xAt(A, t), xb = xAt(B, t);
            if (Math.abs(xa - xb) >= 60 || xa < -40 || xa > 1320) continue;
            if (closeOnPurpose(A, t) || closeOnPurpose(B, t)) continue;
            // who steps aside: one standing (the later of the two in the cast unless only the other stands)
            Film.Actor m = kb.body == Pose.STAND ? B : ka.body == Pose.STAND ? A : null;
            if (m == null) continue;
            Film.Actor o = m == B ? A : B;
            float mx = xAt(m, t), ox = xAt(o, t);
            float dir = mx > ox ? 1 : mx < ox ? -1 : (mx < 640 ? 1 : -1);
            float h = heightOf(m), step = Math.max(110, h * 0.42f);
            float to = mx + dir * step;
            if (to < 140 || to > 1140) { dir = -dir; to = mx + dir * step; }
            // the new spot must be free too (of where the others stand or are going)
            for (int tries = 0; tries < 6; tries++) {
                boolean taken = false;
                for (Film.Actor x : seg.actors) if (x != m && x.stateAt(t).visible && (Math.abs(xAt(x, t) - to) < 70 || Math.abs(x.stateAt(t + 3f).x - to) < 70)) taken = true;
                if (!taken) break;
                to += dir * 80;
                if (to < 120 || to > 1160) { dir = -dir; to = mx + dir * step; }
            }
            to = Math.max(120, Math.min(1160, to));
            Film.Key k = m.at(t + 0.15f);
            k.x = to; k.moveDur = Math.max(0.5f, Math.min(1.1f, Math.abs(to - mx) / 220f)); k.facing = dir; k.run = false;
            Film.Key back = m.at(t + 0.15f + k.moveDur + 0.05f);
            back.facing = ox > to ? 1 : -1;
            unstacked++;
        }
        for (Film.Actor a : seg.actors) orderKeys(a);
    }
    int unstacked;
    private int backAtEdge;

    /** v41: the x nearest to want (170..1110) with at least gap clear of everyone visible at t but those in except. */
    private float freeNear(float want, float t, List<Film.Actor> except, float gap0) {
        // a crowded stage: a narrower gap next
        for (float gap : new float[]{gap0, gap0 * 0.6f})
        for (int k = 0; k <= 16; k++) {
            float cand = want + (k % 2 == 1 ? 1 : -1) * ((k + 1) / 2) * 60f;
            if (cand < 170 || cand > 1110) continue;
            boolean ok = true;
            for (Film.Actor o : seg.actors) {
                if (except.contains(o) || !o.stateAt(t).visible || o.look == null || !o.look.isHumanoid()) continue;
                if (Math.abs(xAt(o, t) - cand) < gap || Math.abs(o.stateAt(t + 3f).x - cand) < gap) { ok = false; break; }
            }
            if (ok) return cand;
        }
        return want;
    }

    /** v41: nobody (but those excepted) stands within gap of x at t or three seconds on. */
    private boolean freeAt(float x, float t, List<Film.Actor> except, float gap) {
        for (Film.Actor o : seg.actors) {
            if (except.contains(o) || !o.stateAt(t).visible || o.look == null || !o.look.isHumanoid()) continue;
            if (Math.abs(xAt(o, t) - x) < gap || Math.abs(o.stateAt(t + 3f).x - x) < gap) return false;
        }
        return true;
    }

    /** v41: close to someone on purpose at t: a hug, a held hand, a ride, a gift, a kiss, the swing or the slide. */
    private static boolean closeOnPurpose(Film.Actor a, float t) {
        for (Film.Act x : a.acts)
            if ((x.type == Film.G_HUG || x.type == Film.G_HOLD_HAND || x.type == Film.G_RIDE || x.type == Film.G_KISS || x.type == Film.G_PLAYGROUND
                    || x.type == Film.G_GIVE) && t >= x.t0 - 0.5f && t <= x.t1 + 0.5f) return true;
        return false;
    }

    /** v41: when a walk under way at t (or set off within half a second of it) ends; t when none is. */
    static float arrivalAfter(Film.Actor a, float t) {
        float end = t;
        for (Film.Key k : a.keys) if (k.moveDur > 0 && k.visible && k.t <= t + 0.5f && k.t + k.moveDur > end) end = k.t + k.moveDur;
        return end;
    }

    /** v41: as ts, for one character: the part's start only while nothing has been staged for them after it. */
    private float tsFor(Film.Actor a, float t) { return estab && a.last().t <= seg.t0 + 0.3f ? seg.t0 : t; }
    private Story.CharacterDef lastSubject;
    private List<Story.CharacterDef> lastGroup = new ArrayList<Story.CharacterDef>();
    /** v39: the last group named together anywhere in the story (kept from scene to scene). */
    private final List<Story.CharacterDef> storyGroup = new ArrayList<Story.CharacterDef>();
    /** The shot planner's plan for each dialogue beat of the part being staged. */
    private final Map<Integer, ShotPlanner.Plan> partPlan = new HashMap<Integer, ShotPlanner.Plan>();
    private int partNo = -1;
    private boolean partFast;
    /** The last dialogue shot (to hold a framing over several calm lines instead of cutting). */
    private Film.Shot lastDlgShot;
    private Film.Actor lastDlgA, lastDlgB;
    private int dlgCount;
    /** v41: lines and reaction shots in the part being staged (one reaction at least where someone listens). */
    private int dlgInPart, partReactions, forcedReactions, partEnd;
    private boolean momentNoted;
    private Film.Actor lastSpeaker;

    private boolean leading;

    private float stagePart(int si, Story.Scene sc, int pi, int nParts, int b0, int b1, String where, int prevTod, float t) {
        leading = true;
        dlgInPart = 0; partReactions = 0; partEnd = b1;
        seg = new Film.Seg();
        seg.type = Film.S_SCENE;
        seg.scene = si;
        seg.t0 = t;
        if (pi == 0 && where.equals(sc.setting.length() > 0 ? sc.setting : sc.title)) seg.set = Sets.forScene(sc);   // v34: the scene's one place
        else {
            seg.set = Sets.detect(firstSentence(where));
            if (seg.set == Sets.GENERIC_OUT) seg.set = Sets.detect(where);
        }
        // the hour: the scene's own words first (its title, the first line of its action), then the place's first
        // sentence (a place description that says the flowers glow "at night" does not make the morning night)
        String firstDir = "";
        for (int bi = b0; bi < b1; bi++) if (sc.beats.get(bi).type == Story.Beat.DIRECTION) { firstDir = firstSentence(sc.beats.get(bi).text); break; }
        int fallback = pi == 0 && si == 0 ? Sets.MORNING : prevTod == Sets.NIGHT && seg.set != Sets.CAVE_IN ? Sets.NIGHT : Sets.DAY;
        // v36: a setting that states its hour in a sentence of its own ("गाँव का मैदान। रात का समय।") is that hour —
        // only a short sentence or one about the time (not a description of the place that mentions the night)
        int fromSetting = Sets.detectTime(firstSentence(where), -1);
        if (fromSetting < 0) for (String sen : sentences(where)) {
            if (!Txt.has(sen, "समय", "बेला", "वक़्त", "वक्त", "time") && sen.trim().split("\\s+").length > 4) continue;
            int tt = Sets.detectTime(sen, -1);
            if (tt >= 0) { fromSetting = tt; break; }
        }
        seg.tod = Sets.detectTime((pi == 0 ? sc.title + " । " : "") + firstDir, fromSetting >= 0 ? fromSetting : fallback);
        if (seg.set == Sets.CAVE_IN) seg.tod = Sets.NIGHT;
        if (seg.set == Sets.BASEMENT) seg.tod = Sets.NIGHT;      // a closed basement knows no daylight
        seg.festive = Txt.has(where, "सजा", "रोशनियों", "ढोल", "उत्सव", "जश्न", "celebrat", "festiv");
        // the characters are in a boat: the place is the water itself, or the boat is named as where they are
        String partWords = where + " " + (b0 < b1 ? sc.beats.get(b0).text : "");
        seg.inBoat = (Txt.has(where, "नाव में", "नाव पर", "नौका में", "in the boat", "on the boat", "in a boat", "on a boat", "in the ship", "on the ship", "on the raft")
                || (Txt.has(where, "समुद्र में", "सागर में", "नदी में", "झील में", "बीच समुद्र", "खुला समुद्र", "खुले समुद्र", "at sea", "open sea", "middle of the sea", "middle of the lake", "on the river")
                && Txt.has(partWords, BOAT))) && !Txt.has(where, "किनारे", "shore", "beach", "तट");
        if (seg.festive && seg.set != Sets.CAVE_IN) seg.set = Sets.CELEBRATION;
        seg.backdrop = nParts > 1 ? art.sceneBackdrop(sc.number, pi) : art.sceneBackdrop(sc.number, 0);
        seg.backdropReverse = art.reverseBackdrop(sc.number);      // the user's reverse angle of the place, for the reverse shots
        seg.backdropViews = art.placeViews(sc.number);              // v39: the other views of the place, behind the close singles
        if (nParts == 1 && seg.backdrop == null) seg.backdrop = art.scenes.get(String.valueOf(sc.number));
        seg.ground = seg.backdrop != null ? seg.backdrop.ground * 720f : Sets.GROUND;
        ground = seg.ground;
        seg.fadeIn = 0.45f; seg.fadeOut = 0.45f;
        lightsOff = false;
        // PixarLead: the act this part belongs to (its colour script) and the format's character scale lock
        seg.act = sceneActs != null && si < sceneActs.length ? sceneActs[si] : 1;
        seg.charScale = PixarLead.spec(opt.aspect).charScale;
        film.segs.add(seg);
        lastSubject = null;
        lastGroup.clear();
        dlgCount = 0;
        dutchUsed = 0;
        lastSpeaker = null;
        lastDlgShot = null;

        // ---------- who is in this part, and who arrives later
        List<Story.CharacterDef> order = new ArrayList<Story.CharacterDef>();
        Map<Story.CharacterDef, Integer> entryBeat = new HashMap<Story.CharacterDef, Integer>();
        String voiceFromOutside = Txt.has(where, "बाहर से", "आवाज़ आती", "from outside") ? "y" : "";
        // v34: only addressed or named in a manner so far (not seen speaking or acting): a later "… walks in" is their entrance
        java.util.Set<Story.CharacterDef> soft = new java.util.HashSet<Story.CharacterDef>();
        for (int bi = b0; bi < b1; bi++) {
            Story.Beat b = sc.beats.get(bi);
            if (b.type == Story.Beat.DIALOGUE) {
                Story.CharacterDef c = b.speaker;
                // a voice from the air, a device or off-screen is heard, never stood on the stage
                if (c != null && (c.voiceOnly || b.offScreen)) c = null;
                if (c != null && !order.contains(c)) {
                    order.add(c);
                    boolean far = Txt.has(b.manner, "दूर से");
                    if ((voiceFromOutside.length() > 0 && bi > b0 && !mentionsAny(sc, b0, bi, c)) || far) entryBeat.put(c, bi);
                }
                // v38: the others saying the line together ("तीनों:", "मीना और राजू:") are on the stage too
                if (c != null) for (Story.CharacterDef m : b.chorus) if (!m.voiceOnly && !order.contains(m)) order.add(m);
                // characters named in the manner (e.g. "वृंदा की तलवार को रोकते हुए") are on stage too
                for (Story.CharacterDef m : ScriptParser.mentions(story, b.manner)) if (!order.contains(m)) { order.add(m); soft.add(m); }
                // ...and so is anyone addressed by name ("और कृपा, तुम्हारी चतुराई...")
                for (Story.CharacterDef m : vocatives(b.text)) if (!order.contains(m)) { order.add(m); soft.add(m); }
                if (c != null) soft.remove(c);
            } else {
                String txt = b.text;
                for (String sent : sentences(txt)) {
                    List<Story.CharacterDef> ms = presentWithGroups(sent);
                    // v39: "चारों पार्क में चलते हुए आते हैं" with no name: the scene's own people (those who speak or are named in it)
                    if (ms.isEmpty() && Txt.has(sent, "चारों", "तीनों", "दोनों", "पाँचों", "पांचों", "सब ", "सभी") && isEntry(sent)) ms = sceneCast(sc, b0, b1, sent);
                    for (int k = 0; k < ms.size(); k++) {
                        Story.CharacterDef m = ms.get(k);
                        if (order.contains(m)) {
                            // "रोहन, बारिश में भीग जाओगे!" … "(रोहन बैसाखी के सहारे चलकर आता है)": called first, then he comes
                            if (soft.remove(m) && bi > b0 && isEntry(sent) && !entryBeat.containsKey(m)) entryBeat.put(m, bi);
                            continue;
                        }
                        order.add(m);
                        // v39: also in the opening description, when it is a walk or a run in ("वनुशा दौड़ती हुई कमरे में आती
                        // है", "चारों पार्क में चलते हुए आते हैं") — they come in, they are not found standing there
                        if ((bi > b0 || walkingIn(sent)) && isEntry(sent)) entryBeat.put(m, bi);
                    }
                }
            }
        }

        // v34: an animal ridden by a character of this part (Durga's lion) is part of the rider's picture: it is not
        // staged on its own (its lines move its jaw in the rider's picture); a rider is staged whenever the animal is
        for (int i = 0; i < order.size(); i++) {
            Story.CharacterDef c = order.get(i);
            if (c.rider == null) continue;
            if (!order.contains(c.rider)) order.set(i, c.rider);
            else { order.remove(i); i--; }
        }
        java.util.LinkedHashSet<Story.CharacterDef> once = new java.util.LinkedHashSet<Story.CharacterDef>(order);
        order.clear(); order.addAll(once);

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
            Integer cn = costumeNow.get(c);
            k.costume = cn == null ? 0 : cn;                     // v34: the clothes they last changed into
            k.disguised = bool(pDisguise.get(c));
            Integer ent = entryBeat.get(c);
            if (ent != null) k.visible = false;
            // v34: a drawn character in a wheelchair is seated in it from the start of the part, so every shot of the
            // part is planned for a seated figure (eye-level framing, the face where it is); mobility() keeps it so
            Look lk0 = k.costume > 0 && k.costume <= c.costumes.size() ? c.costumes.get(k.costume - 1).look : c.look;
            if (lk0 != null && lk0.aid == Look.AID_WHEELCHAIR && (art == null || !art.sprites.containsKey(c.id))) { k.body = Pose.SIT; k.seat = Film.SEAT_WHEELCHAIR; }
            // v35: one who wakes (or gets out of bed) in this part begins it asleep in bed — and one asleep at the end of
            // the last part is still asleep when the next part in a room begins
            boolean asleepNow = c.look != null && c.look.isHumanoid() && k.body == Pose.STAND
                    && (wakesIn(sc, b0, b1, c) || (bool(pAsleep.get(c)) && !Sets.outdoorSet(seg.set) && !speaksFirst(sc, b0, b1, c)));
            if (asleepNow) { k.body = Pose.LIE; k.seat = Film.SEAT_BED; k.eyesShut = true; k.visible = true; }
            a.keys.add(k);
            if (asleepNow) { Film.Fx bf = fx(Film.FX_SEAT, seg.t0, 1e6f, k.x, 0, a, null); bf.kind = Film.SEAT_BED; }
            // Spider-Verse: the frame rate this character's poses step on (only when the user asks for it)
            a.stepFps = opt.onTwos ? PixarLead.stepFps(c) : 0;
            seg.actors.add(a);
        }
        // v34: the tallest of the part (a giant, a rider on her lion) keeps its head inside the picture — the camera
        // never looks above the place's picture, so the whole cast is drawn smaller together (their sizes keep to
        // each other) when the tallest would rise above its top
        float tallest = 0;
        for (Film.Actor a : seg.actors) tallest = Math.max(tallest, heightOf(a));
        float room = ground - 0.06f * 720f;
        if (tallest > room && room > 0) {
            seg.charScale *= Math.max(0.5f, room / tallest);
            film.notes.add("  ↳ everyone drawn at " + Math.round(seg.charScale * 100) + "% so the tallest keeps its head in the picture");
        }

        // ---------- ambience & music
        if (seg.inBoat) { if (wOpen[Film.W_SEA] < 0) open(Film.W_SEA, seg.t0, 0.8f); film.notes.add("  ↳ in a boat on the water"); }
        ambience(where, t);
        weatherFrom(where + " । " + sc.title + (pi == 0 && sc.cues.length() > 0 ? " । " + sc.cues : ""), seg.t0, true);
        campfire(sc);
        playgroundProps(sc, b0, b1, where);
        seg.sofaColor = LookDesigner.colourNear(where + " । " + partWords, "सोफे", "सोफा", "सोफ़े", "सोफ़ा", "sofa", "couch");
        // v41: "गीली सड़क, पानी के छोटे-छोटे गड्ढे" — the place's puddles lie on the ground (the sky in them), out of doors
        if (Sets.outdoorSet(seg.set) && (Txt.has(where, "पोखर", "puddle") || (Txt.has(where, "गड्ढ") && Txt.has(where, "पानी", "बारिश", "गीली", "गीला", "कीचड़")))) {
            float[][] spots = {{330, 30, 200}, {840, 54, 240}, {1080, 14, 150}};
            for (float[] sp : spots) { Film.Fx pf = fx(Film.FX_PUDDLE, seg.t0, 1e6f, sp[0], sp[1], null, null); pf.color = (int) sp[2]; pf.t2 = -1; }
            film.notes.add("  ↳ puddles on the ground (the place's description)");
        }
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
        int fastRun = 0;
        for (int bi = b0; bi < b1; bi++) {
            Story.Beat b = sc.beats.get(bi);
            // v34: a change of clothes at this beat — from here on the character wears it (in this part and the next)
            List<Object[]> changed = new ArrayList<Object[]>();
            for (Story.CharacterDef c : story.characters) for (int ci = 0; ci < c.costumes.size(); ci++) {
                Story.Costume co = c.costumes.get(ci);
                if (co.scene != sc.number || co.beat != bi) continue;
                Integer was = costumeNow.get(c);
                Look before = was == null || was == 0 ? c.look : c.costumes.get(was - 1).look;
                costumeNow.put(c, ci + 1);
                Film.Actor ca = actor(c);
                if (ca != null) {
                    // v37: changing clothes on the stage is done behind a folding screen — the new clothes are on
                    // as they step out from behind it (a family film never shows the change itself)
                    boolean onStage = ca.stateAt(tc).visible && ca.look != null && ca.look.isHumanoid() && b.type == Story.Beat.DIRECTION
                            && Txt.has(b.text, "कपड़े बदल", "ड्रेस बदल", "पोशाक बदल", "वस्त्र बदल", "वेशभूषा बदल", "changes clothes", "changes her clothes", "changes his clothes", "changes into", "change into", "gets dressed", "कपड़े पहन", "तैयार हो");
                    if (onStage && co.look != null && (co.look.outfit != before.outfit || co.look.primary != before.primary || co.look.secondary != before.secondary)) {
                        Film.Act scr = new Film.Act(tc, tc + 2.6f, Film.G_SCREEN);
                        scr.item = before.primary;
                        ca.acts.add(scr);
                        film.sfx.add(new Film.Sfx(Film.SFX_RUSTLE, tc + 0.5f, 1.4f, 0.3f));
                        tc += 1.3f;
                        screens++;
                    }
                    Film.Key ck = ca.at(tc + 0.05f);
                    ck.costume = ci + 1;
                    film.sfx.add(new Film.Sfx(Film.SFX_WHOOSH, tc, 0.5f, 0.3f));
                    changed.add(new Object[]{ca, before, co});
                }
                film.notes.add("    ↳ " + c.shown() + " — a new look: " + co.text);
            }
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
            for (Film.Actor a : seg.actors) orderKeys(a);
            unstack(tc);
            for (Object[] ch : changed) {
                // v37: from behind the screen: the reveal waits until they have stepped out
                for (Film.Act sa : ((Film.Actor) ch[0]).acts) if (sa.type == Film.G_SCREEN && sa.t1 > tc) tc = sa.t1 + 0.05f;
                tc = newLook((Film.Actor) ch[0], (Look) ch[1], (Story.Costume) ch[2], tc);
            }
            // Miyazaki's ma: after two fast beats, one quiet one — a still shot that lets the moment breathe
            boolean fastBeat = PixarLead.fast(b.text + " " + (b.manner == null ? "" : b.manner)) || (b.type == Story.Beat.DIALOGUE && partFast && beatLine[si][bi] >= 0
                    && (film.lines.get(beatLine[si][bi]).emotion == Pose.ANGRY || film.lines.get(beatLine[si][bi]).emotion == Pose.SCARED));
            if (fastBeat) fastRun++; else fastRun = 0;
            if (fastRun >= 2 && opt.technical && bi + 1 < b1) { tc = maPause(tc); fastRun = 0; }
        }
        if (opt.technical) {
            // Russo: one comic extra action per scene (and Gunn: one joke in a scary scene); Gunn: one scary
            // shadow in a funny scene when the story has a villain
            comicBeat(b0 < b1 ? tc : t, mood);
            if ((mood == Film.M_HAPPY || mood == Film.M_PLAYFUL || mood == Film.M_CELEBRATE) && storyHasVillain() && tc - t > 6) shadowPass(t + (tc - t) * 0.55f);
        }
        paceTracks(tc + 0.8f);
        playUntil(tc + 0.8f);
        seg.t1 = tc + 0.8f;
        for (Film.Fx f : seg.fx) if (f.type == Film.FX_LIGHTS_OFF && f.t1 > seg.t1) f.t1 = seg.t1;
        if (pi == nParts - 1) closeAllWeather(seg.t1);
        else { closeWeather(Film.W_STARS, seg.t1); closeWeather(Film.W_FIREFLIES, seg.t1); }
        film.music.add(new Film.Music(mood, seg.t0, seg.t1));
        // Gunn: music is a character — scary = silence and a heartbeat (the tense score is already a low drone)
        if ((mood == Film.M_TENSE || mood == Film.M_VILLAIN) && seg.t1 - seg.t0 > 4) film.sfx.add(new Film.Sfx(Film.SFX_HEARTBEAT, seg.t0 + 1f, Math.min(14f, seg.t1 - seg.t0 - 1.5f), 0.2f));
        film.ambience.add(new Film.Amb(seg.t0, seg.t1, where + " " + Sets.name(seg.set) + " " + ambWords(seg)));
        film.notes.set(noteIdx, "Part " + sc.number + (nParts > 1 ? " (" + (char) ('a' + pi) + ")" : "") + ": " + Sets.label(seg.set)
                + (seg.backdrop != null ? " [your picture]" : "") + ", characters: " + names(lineup));
        // remember persistent flags
        for (Film.Actor a : seg.actors) {
            Film.Key k = a.last();
            pNoHead.put(a.c, k.noHeadwear);
            pTurban.put(a.c, k.wearsTurban);
            pDisguise.put(a.c, k.disguised);
            pAsleep.put(a.c, k.body == Pose.LIE && k.eyesShut);          // v35: still asleep for the next part
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

    /** v39: the middle of a group on the stage at t (where it leaves from together). */
    private static float groupSide(List<Film.Actor> who, float t) {
        float sum = 0; int n = 0;
        for (Film.Actor a : who) { if (!a.stateAt(t).visible) continue; sum += xAt(a, t); n++; }
        return n == 0 ? 640 : sum / n;
    }

    /** v39: a walk or a run into the place ("दौड़ती हुई … आती है", "चलते हुए आते हैं", "walks in"). */
    static boolean walkingIn(String s) {
        return Txt.has(s, "चलते हुए आ", "चलती हुई आ", "चलता हुआ आ", "चलकर आ", "चल कर आ", "दौड़ते हुए आ", "दौड़ती हुई", "दौड़ता हुआ", "भागते हुए आ",
                "भागती हुई आ", "भागता हुआ आ", "दौड़कर आ", "भागकर आ", "walks in", "runs in", "comes in", "enters", "rushes in", "hurries in");
    }

    /** v39: the people of a part (who speak or are named in it), at most as many as the group word says. */
    private List<Story.CharacterDef> sceneCast(Story.Scene sc, int b0, int b1, String sent) {
        List<Story.CharacterDef> out = new ArrayList<Story.CharacterDef>();
        for (int bi = b0; bi < b1; bi++) {
            Story.Beat b = sc.beats.get(bi);
            if (b.type == Story.Beat.DIALOGUE) {
                if (b.speaker != null && !b.speaker.voiceOnly && !out.contains(b.speaker)) out.add(b.speaker);
                for (Story.CharacterDef m : b.chorus) if (!m.voiceOnly && !out.contains(m)) out.add(m);
            } else for (Story.CharacterDef m : ScriptParser.mentions(story, b.text)) if (!out.contains(m)) out.add(m);
        }
        int n = Txt.has(sent, "दोनों") ? 2 : Txt.has(sent, "तीनों") ? 3 : Txt.has(sent, "चारों") ? 4 : Txt.has(sent, "पाँचों", "पांचों") ? 5 : out.size();
        return n < out.size() ? new ArrayList<Story.CharacterDef>(out.subList(0, n)) : out;
    }

    static boolean isEntry(String s) {
        return Txt.has(s, "बाहर निकल", "आता है", "आती है", "आते हैं", "आ जाता", "आ जाती", "आ बैठ", "प्रवेश", "घुसते", "घुसता",
                "छलाँग", "उल्टा लटक", "दौड़ते हुए आते", "आगे आती", "आगे आता", "enters", "arrives", "comes in", "पहुँचते", "पहुँचती", "पहुँचता",
                "walks in", "walk in", "rolls in", "wheels in", "comes over", "limps in", "hobbles in");
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
        // v43: indoors, through the room's door when it has been seen (not in through the wall on the other side)
        Film.Fx roomDoor = seenDoor();
        if (roomDoor != null && indoors(seg.set)) fromRight = roomDoor.x > 0;
        float startX = fromRight ? 1400 : -120;
        Film.Key off = a.at(tc);
        off.visible = true;
        off.x = startX;
        off.facing = fromRight ? -1 : 1;
        if (far) { dest = fromRight ? 1180 : 100; off.depth = 1; }
        // v42: to free ground — the place they began the scene on may be taken by now (समीर came in onto अर्जुन)
        else {
            java.util.List<Film.Actor> self = java.util.Collections.singletonList(a);
            if (!freeAt(dest, tc, self, 84)) dest = freeNear(dest, tc, self, 140);
            // v43: in through the room's door, they stop before the first one standing in their way (the one who
            // opened the door to them) instead of walking through them to the middle of the room
            if (roomDoor != null && indoors(seg.set)) {
                float dir = fromRight ? -1 : 1, near = Float.NaN;
                for (Film.Actor o : seg.actors) {
                    if (o == a || !o.stateAt(tc).visible || o.look == null || !o.look.isHumanoid()) continue;
                    float ox = o.stateAt(tc + 1.5f).x;
                    if ((ox - startX) * dir > 0 && (dest - ox) * dir > 0 && (Float.isNaN(near) || Math.abs(ox - startX) < Math.abs(near - startX))) near = ox;
                }
                if (!Float.isNaN(near)) {
                    float stop = near - dir * 140;
                    if (stop >= 170 && stop <= 1110 && (stop - (fromRight ? 1130 : 150)) * dir > 40) dest = stop;
                }
            }
        }
        boolean run = Txt.has(sentence, "दौड़", "छलाँग", "run", "jump");
        if (Txt.has(sentence, "उल्टा लटक", "डाल", "पेड़ से") && a.look.kind == Look.MONKEY) {
            Film.Key k = a.at(tc);
            k.x = 250; k.anchor = Film.A_BRANCH; k.body = Pose.HANG; k.visible = true;
            film.sfx.add(new Film.Sfx(Film.SFX_RUSTLE, tc, 1.2f, 0.7f));
            // v34: "पेड़ से कूदकर नीचे आते हैं" — he comes down: a moment on the branch, then a jump to the ground
            // beside the others (before, he hung upside down through the whole scene, his face out of every shot)
            if (!Txt.has(sentence, "उल्टा लटक") && Txt.has(sentence, "कूद", "उतर", "नीचे आ", "jump down", "jumps down", "jumped down",
                    "climbs down", "climb down", "drops down", "comes down", "came down")) {
                k.body = Pose.STAND;
                Film.Key down = a.at(tc + 0.9f);
                down.anchor = Film.A_GROUND; down.body = Pose.STAND; down.x = dest; down.moveDur = 0.6f; down.run = true;
                down.facing = dest > 250 ? 1 : -1;
                a.acts.add(new Film.Act(tc + 0.8f, tc + 1.5f, Film.G_JUMP));
                film.sfx.add(new Film.Sfx(Film.SFX_WHOOSH, tc + 0.85f, 0.5f, 0.5f));
                film.sfx.add(new Film.Sfx(Film.SFX_THUD, tc + 1.5f, 0.4f, 0.45f));
                return tc + 1.7f;
            }
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
        // v34 (realism): coming through the rain without an umbrella, one hurries in; a blindfolded one feels the way in
        boolean aided = a.look.aid != Look.AID_NONE || (a.look.injury & Look.INJ_LEG) != 0;
        if (!run && !aided && !a.umbrellaAt(a.look, tc) && a.look.isHumanoid() && !a.look.cannotSee() && Sets.outdoorSet(seg.set)
                && (wOpen[Film.W_RAIN] >= 0 || wOpen[Film.W_STORM] >= 0)) { run = true; hurried++; }
        boolean blindIn = a.look.cannotSee();         // v35: blind as well as blindfolded
        if (blindIn) run = false;
        Film.Key k = a.at(tc + 0.01f);
        k.x = dest;
        k.moveDur = run ? 0.9f : blindIn ? 2.0f : 1.3f;
        k.run = run;
        k.facing = dest > startX ? 1 : -1;
        // (v35: a hand out in front without a cane; with one, the cane finds the way and taps)
        if (blindIn) { if (a.look.aid != Look.AID_STICK) a.acts.add(new Film.Act(tc + 0.01f, tc + 2.0f, Film.G_REACH)); feltWay++; }
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
        else if (a.look.aid != Look.AID_NONE || (a.look.injury & Look.INJ_LEG) != 0) {
            // v34: the sound of how they come: a wheelchair rolls, a stick or a walking frame taps, crutches knock and
            // swing; all slower, as mobility() makes their moves a third longer
            int aidSound = a.look.aid == Look.AID_WHEELCHAIR ? Film.SFX_WHEELCHAIR : a.look.aid == Look.AID_CRUTCHES ? Film.SFX_CRUTCH
                    : a.look.aid == Look.AID_STICK || a.look.aid == Look.AID_WALKER ? Film.SFX_STICK : stepsSound(false);
            film.sfx.add(new Film.Sfx(aidSound, tc, k.moveDur * 1.35f, a.look.aid == Look.AID_WHEELCHAIR ? 0.45f : 0.38f));
        }
        else if (careful(a.look)) film.sfx.add(new Film.Sfx(Film.SFX_STEPS_LIMP, tc, k.moveDur * 1.2f, 0.36f));     // v35: a limp is heard, uneven
        else film.sfx.add(new Film.Sfx(stepsSound(run), tc, k.moveDur, 0.35f));
        if (a.look.anklets) film.sfx.add(new Film.Sfx(Film.SFX_ANKLET, tc, k.moveDur, 0.4f));
        // v34 (realism): coming into a room, a door is heard on their side; everyone already there looks up and
        // watches them come in (one with their back to them turns round)
        if (!far && indoors(seg.set)) door(tc - 0.15f, fromRight ? 0.6f : -0.6f);
        watch(a, tc, tc + k.moveDur + 0.5f, true);
        return tc + (far ? 0.2f : 0.4f);
    }

    // ------------------------------------------------------------------ dialogue

    private float dialogue(int si, int bi, Story.Beat b, float tc) {
        Film.Line line = film.lines.get(beatLine[si][bi]);
        Film.Actor sp = actor(b.speaker);
        // v34: the animal a rider sits on speaks from the rider's picture (its jaw, not her lips)
        boolean mountLine = false;
        if (sp == null && b.speaker != null && b.speaker.rider != null) { sp = actor(b.speaker.rider); mountLine = sp != null; }
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
        // speaker must be visible (and facing us again to speak: a turned back turns round)
        if (!mountLine && sp.stateAt(tc).backTurned) { Film.Key kb = sp.at(tc); kb.backTurned = false; }
        Film.Key cur = sp.stateAt(tc);
        // v41: one who went off (rode away, walked out) and now speaks comes back in first, from the side they left by,
        // to a free spot — never a voice from beyond the edge of the picture, nor a figure popping up out there
        float sx0 = xAt(sp, tc);
        if (!mountLine && cur.anchor == Film.A_GROUND && (sx0 < 40 || sx0 > 1240)) {
            float from = sx0 < 640 ? -120 : 1400, to = from < 0 ? 260 : 1020;
            for (int tries = 0; tries < 6; tries++) {
                boolean free = true;
                for (Film.Actor o : seg.actors) if (o != sp && o.stateAt(tc).visible && Math.abs(xAt(o, tc) - to) < 130) free = false;
                if (free) break;
                to += from < 0 ? 150 : -150;
            }
            Film.Key in = sp.at(tc);
            in.visible = true; in.x = from; in.moveDur = 0; in.facing = from < 0 ? 1 : -1; in.backTurned = false;
            if (in.body == Pose.SIT || in.body == Pose.LIE) { in.body = Pose.STAND; in.seat = -1; }
            Film.Key go = sp.at(tc + 0.05f);
            go.x = to; go.moveDur = Math.max(1.0f, Math.min(2.2f, Math.abs(to - from) / 330f)); go.run = false;
            if (indoors(seg.set)) door(tc - 0.15f, from < 0 ? -0.6f : 0.6f);
            leftFrom.remove(sp);
            cameBackToSpeak++;
            film.notes.add(String.format(java.util.Locale.US, "  ↳ %s comes back in to speak (%.1f s)", sp.c.shown(), tc));
            cur = sp.stateAt(tc);
        }
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
        // v35: a speaker still walking in from beyond the edge of the picture is let in first — the line begins once
        // they are on the stage (nobody speaks from outside the frame, nobody jumps in to speak)
        Film.Key ek = sp.stateAt(start);
        if (ek.moveDur > 0 && ek.anchor == Film.A_GROUND && start < ek.t + ek.moveDur) {
            float in = start;
            while (in < ek.t + ek.moveDur) { float xs = xAt(sp, in); if (xs >= 80 && xs <= 1200) break; in += 0.05f; }
            if (in > start + 0.01f) { start = Math.min(in, ek.t + ek.moveDur); waitedToEnter++; }
        }
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
        s.t0 = start; s.t1 = end; s.line = line.index; s.emotion = line.emotion; s.mount = mountLine;
        sp.speaks.add(s);
        // v38: a line said together ("तीनों:", "दोनों (एक साथ):"): everyone of the group on the stage says it too —
        // they come into view, face us and their lips move with the same words
        List<Film.Actor> together = new ArrayList<Film.Actor>();
        if (!mountLine) for (Story.CharacterDef m : b.chorus) {
            Film.Actor ca = actor(m);
            if (ca == null || ca == sp || together.contains(ca)) continue;
            Film.Key ck = ca.stateAt(tc);
            if (!ck.visible) { Film.Key k = ca.at(tc); k.visible = true; }
            if (ck.anchor == Film.A_HIDDEN) { Film.Key k = ca.at(tc); k.anchor = Film.A_GROUND; }
            if (ck.backTurned) { Film.Key k = ca.at(tc); k.backTurned = false; }
            Film.Speak cs = new Film.Speak();
            cs.t0 = start; cs.t1 = end; cs.line = line.index; cs.emotion = line.emotion;
            ca.speaks.add(cs);
            boolean cSigns = ca.look != null && ca.look.signs();
            ca.acts.add(new Film.Act(start, end, cSigns ? Film.G_SIGN : Film.G_TALK));
            together.add(ca);
            chorusLines++;
        }
        // v36: a phone call goes on while its caller speaks (the phone stays at the ear through the line)
        Film.Act call = null;
        for (Film.Act ac : sp.acts) if (ac.type == Film.G_TASK && ac.item == Film.T_PHONE && ac.t1 > start - 2f && ac.t0 < start) call = ac;
        // v35: a character who cannot speak (or signs) says the line in sign language: the hands sign, the lips stay still
        boolean signs = !mountLine && sp.look != null && sp.look.signs();
        line.signed = signs;
        if (signs) signedLines++;
        if (!mountLine) {
            sp.acts.add(new Film.Act(start, end, signs ? Film.G_SIGN : Film.G_TALK));
            // gestures from the manner, e.g. (तलवार घुमाते हुए) (घुटनों के बल गिरकर रोते हुए)
            mannerActions(sp, to, b.manner, start, end, line.emotion);
        }
        if (call != null) { call.t1 = Math.max(call.t1, end + 0.4f); sp.acts.remove(call); sp.acts.add(call); }
        cuesFrom(b.manner, start, sp, to, null);
        if (!mountLine) headwearFromWords(sp, to, b.text, start);
        if (line.emotion == Pose.SAD && !mountLine) { Film.Key k = sp.at(start); k.tears = true; }
        if (line.emotion == Pose.LAUGH && sp.look.hero && Txt.has(b.text, "हा हा", "हँस")) {
            // the little princess' laugh makes flowers bloom (story magic) – only if the script says so later
        }
        String whoShown = b.speaker.shown();
        for (Film.Actor ca : together) whoShown += ", " + ca.c.shown();
        sub(start, end, whoShown, line.shown);
        if (signs) seg.subs.get(seg.subs.size() - 1).signed = true;
        ShotPlanner.Plan plan = partPlan.get(bi);
        if (opt.technical && plan != null) {
            // no long dialogue in a far shot: more than six words go to a front-facing close-up (lip-sync protocol)
            int words = line.text.trim().split("\\s+").length;
            if (words > 6 && plan.size < ShotPlanner.CU) { plan.size = ShotPlanner.CU; plan.type = ShotPlanner.SINGLE; plan.hold = false; }
            else if (plan.size < ShotPlanner.MCU && !(plan.type == ShotPlanner.TWO_SHOT && words <= 6)) plan.size = ShotPlanner.MCU;
        }
        if (mountLine) {
            // the animal speaks: the whole picture (the rider and the animal's head) in a medium-wide frame, never
            // the rider's face alone
            ShotPlanner.Plan mp = new ShotPlanner.Plan();
            if (plan != null) { mp.intensity = plan.intensity; mp.stage = plan.stage; mp.light = plan.light; mp.height = plan.height; }
            mp.size = ShotPlanner.MWIDE; mp.type = ShotPlanner.SINGLE; mp.move = ShotPlanner.STATIC;
            standStillToSpeak(sp, start, end);
            camDialogue(sp, null, start, line.emotion, mp, line);
            lastSpeaker = sp;
            lastSubject = b.speaker;
            dlgCount++;
            return end + 0.35f;
        }
        if (!together.isEmpty()) {
            // everyone saying it is seen: one steady medium-wide frame on the group, no single close-up
            ShotPlanner.Plan gp = new ShotPlanner.Plan();
            if (plan != null) { gp.intensity = plan.intensity; gp.stage = plan.stage; gp.light = plan.light; gp.height = plan.height; }
            gp.size = ShotPlanner.MWIDE; gp.type = ShotPlanner.TWO_SHOT; gp.move = ShotPlanner.STATIC;
            standStillToSpeak(sp, start, end);
            for (Film.Actor ca : together) standStillToSpeak(ca, start, end);
            // the frame spans the speaker and the one of the group farthest from them: the others stand between
            Film.Actor far = together.get(0);
            for (Film.Actor ca : together) if (Math.abs(xAt(ca, start) - xAt(sp, start)) > Math.abs(xAt(far, start) - xAt(sp, start))) far = ca;
            camDialogue(sp, far, start, line.emotion, gp, line);
            lastSpeaker = sp;
            lastSubject = b.speaker;
            dlgCount++;
            return end + 0.35f;
        }
        if (opt.technical) { standStillToSpeak(sp, start, end); lipSyncShots(sp, to, start, end, line, plan); }
        else camDialogue(sp, to, start, line.emotion, plan, line);
        float after = 0.35f;
        if (to != null && plan != null && to.stateAt(start).visible) {
            listenerPerformance(sp, to, start, end, line, plan);
            // the reaction is often more important than the line: show it and let it breathe (§9, §31)
            // v41: and every part with someone listening shows at least one (the handbook's performance gate): after
            // the first line said with feeling, or a question or an exclamation, if the plan chose none so far
            boolean lastLine = true;
            Story.Scene scn = story.scenes.get(si);
            for (int k = bi + 1; k < Math.min(partEnd, scn.beats.size()); k++) if (scn.beats.get(k).type == Story.Beat.DIALOGUE) { lastLine = false; break; }
            boolean force = !plan.reaction && partReactions == 0
                    && (line.emotion != Pose.NEUTRAL || line.text.contains("!") || line.text.contains("?") || lastLine);
            if ((plan.reaction || force) && to.stateAt(end).visible && reactionShot(to, sp, end, line, plan)) {
                after += plan.reaction ? plan.breathe : 0.9f;
                partReactions++;
                if (force) forcedReactions++;
            }
        }
        dlgInPart++;
        lastSpeaker = sp;
        lastSubject = b.speaker;
        dlgCount++;
        return end + after;
    }

    // ================================================================== the thumbnail and the poster (RULE_RESIZE_8)

    /**
     * The thumbnail and the poster are made separately, natively in their own formats, never resized from a frame:
     * the thumbnail (16:9) is the hero's face, 60 % of the frame, on a solid colour of the hero's palette with the
     * bottom 15 % empty for text; the poster (9:16) is the hero full body, centred, feet on the ground with a
     * shadow, 40 % empty at the top for the title and 20 % at the bottom for credits. The two parts are not in the
     * film's timeline: they are drawn with {@link Renderer#renderSeg}. Call after {@link #direct}.
     */
    public Film.Seg[] stills() {
        Story.CharacterDef hero = PixarLead.hero(story);
        if (hero == null || hero.look == null) return new Film.Seg[0];
        Film.Seg keepSeg = seg;
        float keepGround = ground;
        Film.Seg[] out = new Film.Seg[2];
        for (int i = 0; i < 2; i++) {
            PixarLead.Format f = i == 0 ? PixarLead.THUMBNAIL : PixarLead.spec("9:16");
            Film.Seg s = new Film.Seg();
            s.type = Film.S_SCENE;
            s.t0 = 0; s.t1 = 2;
            s.set = Sets.GARDEN; s.tod = Sets.DAY;
            s.mood = Film.M_HAPPY; s.act = 5;
            s.charScale = f.charScale;
            s.ground = Sets.GROUND;
            s.solid = PixarLead.paletteColor(hero);
            s.fadeIn = 0; s.fadeOut = 0;
            Film.Actor a = new Film.Actor();
            a.c = hero; a.look = hero.look; a.order = 0;
            Film.Key k = new Film.Key();
            k.t = 0; k.x = 640; k.facing = 1; k.emotion = Pose.HAPPY; k.visible = true;
            a.keys.add(k);
            s.actors.add(a);
            seg = s; ground = s.ground;
            float h = heightOf(a);
            Film.Cam c;
            if (i == 0) {
                // the face 60% of the frame, the whole head inside, a little above the middle (text goes at the bottom)
                float[] fb = faceBox(a, 0.5f);
                float zoom = Math.min(fb[3], 0.6f * 720f / fb[2]);
                float chin = fb[1] + fb[2] * 0.5f, headTop = fb[4] - 18;
                zoom = Math.min(zoom, 0.8f * 720f / Math.max(1, chin - headTop));
                zoom = Math.max(1.2f, zoom);
                float fh = 720f / zoom;
                float cy = fb[1] + 0.02f * fh;
                if (headTop < cy - fh / 2 + 0.06f * fh) cy = headTop - 0.06f * fh + fh / 2;
                c = new Film.Cam(0, fb[0], cy, zoom, 0);
            } else {
                // full body, 45% of the frame height: the head's top at 40% from the top, the feet at 85%
                float fh = h / 0.45f;
                c = new Film.Cam(0, 640, ground - h + 0.1f * fh, 720f / fh, 0);
            }
            c.still = true; c.light = 0.2f;
            s.cams.add(c);
            out[i] = s;
        }
        seg = keepSeg; ground = keepGround;
        return out;
    }

    // ================================================================== Lock sheets and location plates (pipeline steps 1-2)

    /**
     * C1 / pipeline step 1: the Character Lock Sheet of a character — standing, front view, neutral, on a plain
     * plate, exactly as the film draws them (their picture rigged, or their drawn puppet): the reference every
     * shot is made from. Rendered with {@link Renderer#renderSeg}.
     */
    public Film.Seg lockSheet(Story.CharacterDef c, float frameAspect) {
        Film.Seg keepSeg = seg;
        float keepGround = ground;
        Film.Seg s = new Film.Seg();
        s.type = Film.S_SCENE;
        s.t0 = 0; s.t1 = 2;
        s.set = Sets.GARDEN; s.tod = Sets.DAY;
        s.mood = Film.M_HAPPY; s.act = 1;
        s.charScale = PixarLead.spec(opt.aspect).charScale;
        s.ground = Sets.GROUND;
        s.solid = 0xFFE4EAF0;
        s.fadeIn = 0; s.fadeOut = 0;
        Film.Actor a = new Film.Actor();
        a.c = c; a.look = c.look; a.order = 0;
        Film.Key k = new Film.Key();
        k.t = 0; k.x = 640; k.facing = 1; k.emotion = Pose.NEUTRAL; k.visible = true;
        a.keys.add(k);
        s.actors.add(a);
        seg = s; ground = s.ground;
        float h = heightOf(a);
        // what is drawn: a picture is as wide as its picture; a drawn animal is wider than tall (tail and head) and
        // its head rises above its body; a bird or a monkey too
        float[] ext = drawnExtent(c, h);
        float drawnW = ext[0], drawnH = ext[1];
        // the whole character inside, with room above and beside: at most 82% of the frame's height, 84% of its width
        float fh = Math.max(drawnH / 0.82f, drawnW / (frameAspect * 0.84f));
        float top = ground - drawnH, frameTop = top - (fh - drawnH) / 2;
        Film.Cam cam = new Film.Cam(0, 640, frameTop + fh / 2, 720f / fh, 0);
        cam.still = true; cam.light = 0.2f;
        s.cams.add(cam);
        seg = keepSeg; ground = keepGround;
        return s;
    }

    /** The width and height a character takes when drawn standing, for a height h. */
    private float[] drawnExtent(Story.CharacterDef c, float h) {
        Art.Sprite sp = art == null ? null : art.sprites.get(c.id);
        if (sp != null && sp.h > 0) return new float[]{h * sp.w / (float) sp.h, h};
        int kind = c.look == null ? Look.MAN : c.look.kind;
        switch (kind) {
            case Look.ANIMAL: return new float[]{2.1f * h, 1.35f * h};
            case Look.BIRD: return new float[]{1.6f * h, 1.2f * h};
            case Look.MONKEY: return new float[]{1.2f * h, 1.1f * h};
            case Look.MONSTER: return new float[]{1.1f * h, 1.1f * h};
            default: return new float[]{0.7f * h, 1.05f * h};
        }
    }

    /** The lock sheet's picture size: portrait for people, landscape for animals, birds and pictures wider than tall. {w, h} */
    public int[] lockSheetSize(Story.CharacterDef c) {
        float[] ext = drawnExtent(c, 100f);
        return ext[0] > ext[1] ? new int[]{640, 480} : new int[]{480, 640};
    }

    /**
     * Pipeline step 2: one Location Lock Plate per place of the film — the place exactly as the film shows it
     * (the user's or AI picture, or the painted set, at its time of day), with no characters. {label, part}
     */
    public List<Object[]> locationPlates() {
        List<Object[]> out = new ArrayList<Object[]>();
        java.util.Set<String> seen = new java.util.HashSet<String>();
        if (film == null) return out;
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            String key = sg.set + "|" + sg.tod + "|" + (sg.backdrop == null ? "painted" : String.valueOf(System.identityHashCode(sg.backdrop)));
            if (!seen.add(key)) continue;
            Film.Seg s = new Film.Seg();
            s.type = Film.S_SCENE;
            s.t0 = 0; s.t1 = 2;
            s.set = sg.set; s.tod = sg.tod; s.scene = sg.scene;
            s.backdrop = sg.backdrop; s.ground = sg.ground;
            s.mood = sg.mood; s.act = sg.act; s.charScale = sg.charScale; s.festive = sg.festive; s.midground = sg.midground;
            s.fadeIn = 0; s.fadeOut = 0;
            Film.Cam cam = new Film.Cam(0, 640, 360, 1f, 0);
            cam.still = true; cam.light = 0.3f;
            s.cams.add(cam);
            String label = sg.scene >= 0 && sg.scene < story.scenes.size() ? story.scenes.get(sg.scene).heading + " — " + Sets.label(sg.set) : Sets.label(sg.set);
            out.add(new Object[]{label, s});
        }
        return out;
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
                // Shot C = reaction: the listener's face, silent; the line goes on off-screen — over the speaker's
                // shoulder when the speaker's back view exists (the scene maker guide, ch. 7: the listener's perspective)
                Film.Cam c = faceCam(to, tk, light, 0.75f);
                c.keep = true;
                c.reverse = true;
                seg.cams.add(c);
                boolean ots = overShoulder(sp, to, tk);
                sh = shot(tk, ShotPlanner.MCU, ots ? ShotPlanner.OTS : ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, to, sp, stage);
                sh.reaction = true;
                if (ots) sh.ots = sp.c.shown();
                sh.purpose = "Reaction (part " + (k + 1) + " of " + n + "): " + to.c.shown() + " listens" + (ots ? ", seen over " + sp.c.shown() + "'s shoulder" : "") + "; the line goes on off-screen";
                sh.action = to.c.shown() + " listens, subtle breathing, one small change of expression";
                sh.face = faceOf(empathy(line.emotion, sp, to));
                sh.speech = false;
            } else if (line.signed) {
                // v35: a line in sign language: the hands and the face in one frame (a medium close-up of the upper
                // body), never a close-up that would cut the hands off; no lip-sync — the words are in the subtitle
                camOn(sp, tk, ShotPlanner.zoomFor(ShotPlanner.MCU, heightOf(sp)) * 0.85f);
                seg.cams.get(seg.cams.size() - 1).keep = true;
                sh = shot(tk, ShotPlanner.MCU, ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, sp, to, stage);
                sh.line = line.index;
                sh.speech = false;
                sh.purpose = "Signed" + (n > 1 ? " (part " + (k + 1) + " of " + n + ")" : "") + ": " + sp.c.shown() + " signs — hands and face in the frame, the words in the subtitle";
                sh.action = sp.c.shown() + " signs: \"" + words + "\"";
                sh.face = faceOf(line.emotion);
                done++;
            } else {
                // v22: never an extreme close-up for lip-sync (it cuts the hair); the second group of words is framed a
                // little wider instead, so the two shots still differ
                Film.Cam c = faceCam(sp, tk, light, k % 2 == 1 ? 0.75f : 1f);
                c.keep = true;
                seg.cams.add(c);
                sh = shot(tk, k % 2 == 1 ? ShotPlanner.MCU : ShotPlanner.CU, ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, sp, to, stage);
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
    // ------------------------------------------------------------------ PixarLead: ma, the comic beat, the shadow, weight

    /** Who the quiet shot is on: the hero when present, else the last subject, else anyone visible. */
    private Film.Actor quietSubject(float t) {
        Film.Actor best = null;
        for (Film.Actor a : seg.actors) {
            if (!a.stateAt(t).visible || a.stateAt(t).anchor == Film.A_HIDDEN) continue;
            if (a.c.shown().equals(film.hero)) return a;
            if (best == null || (lastSubject != null && a.c == lastSubject)) best = a;
        }
        return best;
    }

    /**
     * Miyazaki's ma: after two fast beats, one quiet shot — 1.8 seconds of a still medium-wide frame on the hero,
     * nobody moving, only the wind. The protocol keeps it (Cam.keep): it is never dropped as a jump cut.
     */
    private float maPause(float tc) {
        Film.Actor a = quietSubject(tc);
        if (a == null) return tc;
        float t0 = tc + 0.1f, dur = 1.8f;
        float h = heightOf(a), x = finalX(a, t0);
        Film.Cam c = new Film.Cam(t0, x, ground - h * 0.55f, ShotPlanner.ZOOM[ShotPlanner.MWIDE], 0);
        c.still = true; c.keep = true; c.light = 0.2f;
        seg.cams.add(c);
        Film.Shot sh = shot(t0, ShotPlanner.MWIDE, ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, a, null, ShotPlanner.ESTABLISH);
        sh.purpose = "Ma (Miyazaki): a quiet breath after two fast beats";
        sh.action = a.c.shown() + " stands still, breathing; nothing else moves";
        sh.face = "The feeling settles on the face";
        sh.body = "Weight on both feet, shoulders down";
        sh.cutWhen = "the breath is over (1.8 s)";
        sh.emotionalPurpose = "The audience catches up with the moment";
        sh.light = 0.2f;
        film.sfx.add(new Film.Sfx(Film.SFX_WIND, t0, dur, 0.18f));
        shiftAfter(t0, dur);
        maPauses++;
        return tc + dur;
    }

    /** Russo / Gunn: one small comic action per part by a character who can carry it (never in a sad part). */
    private void comicBeat(float tc, int mood) {
        if (mood == Film.M_SAD) return;
        Film.Actor who = null;
        for (Film.Actor a : seg.actors) if (PixarLead.comic(a.c) && a.stateAt(tc - 0.5f).visible && a.stateAt(tc - 0.5f).anchor == Film.A_GROUND
                && a.look.aid == Look.AID_NONE && a.look.injury == 0 && a.look.condition == 0) { who = a; break; }      // v34/v35: a walking aid, an injury or a body's difference is never a joke
        if (who == null) return;
        // at a quiet moment near the end of the part: nobody speaking, this character not acting
        float t0 = Math.max(seg.t0 + 1f, tc - 1.6f);
        for (Film.Actor a : seg.actors) for (Film.Speak sp : a.speaks) if (t0 < sp.t1 && t0 + 1.2f > sp.t0) return;
        for (Film.Act ac : who.acts) if (t0 < ac.t1 && t0 + 1.2f > ac.t0) return;
        who.acts.add(new Film.Act(t0, t0 + 1.2f, Film.G_HEAD_SCRATCH));
        comicBeats++;
    }

    /** Gunn: a scary shadow sweeps over the ground of a funny scene, from the villain's side. */
    private void shadowPass(float t) {
        Film.Fx fx = new Film.Fx(Film.FX_SHADOW_PASS, t, t + 1.6f);
        fx.x = 1500; fx.y = ground;
        seg.fx.add(fx);
        film.sfx.add(new Film.Sfx(Film.SFX_WHOOSH, t + 0.2f, 1.2f, 0.25f));
        shadowPasses++;
    }

    private boolean storyHasVillain() {
        for (Story.CharacterDef c : story.characters) if (c.look != null && !c.look.hero) return true;
        return false;
    }

    /** Nolan: real sounds — the steps of the place's floor (stone and marble ring, earth and grass thud), running steps when running. */
    private int stepsSound(boolean run) {
        if (run) return Film.SFX_STEPS_RUN;
        switch (seg.set) {
            case Sets.COURTYARD: case Sets.GATE: case Sets.HALL: case Sets.CAVE_IN: case Sets.CAVE_MOUTH: case Sets.CELEBRATION:
            case Sets.ROOFTOP: case Sets.BASEMENT: case Sets.STREET: case Sets.ROOM: return Film.SFX_STEPS_HARD;
            default: return Film.SFX_STEPS;
        }
    }

    /** Timing by weight: how much faster (>1) or slower (<1) than a grown person this character may move. */
    static float weightSpeed(Film.Actor a) {
        Look l = a.look;
        if (l == null) return 1f;
        if (l.kind == Look.MONKEY) return 1.35f;
        if (l.kind == Look.MONSTER) return 0.75f;
        float k = 1f;
        if (l.height > 1.2f) k *= 0.85f;
        if (l.girth > 1.15f) k *= 0.85f;
        if (l.isChild()) k *= 1.15f;
        if (l.kind == Look.ANIMAL) k *= 1.2f;
        return k;
    }

    private void standStillToSpeak(Film.Actor sp, float start, float end) {
        Film.Key last = sp.keys.get(sp.keys.size() - 1);
        if (last.t > start + 1e-3f) return;                       // later plans exist already: leave them
        Film.Key cur = sp.stateAt(start);
        if (cur.moveDur <= 0 || start >= cur.t + cur.moveDur - 0.15f || cur.anchor != Film.A_GROUND) return;
        float x = xAt(sp, start), dest = cur.x, remaining = cur.t + cur.moveDur - start;
        boolean run = cur.run;
        if (x < 60 || x > 1220) {
            // still outside the stage (an entrance from afar): they are there when the line begins, nobody
            // speaks from beyond the edge of the picture
            Film.Key here = sp.at(start);
            here.x = dest; here.moveDur = 0; here.run = false;
            stillToSpeak++;
            return;
        }
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
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            for (Film.Actor a : sg.actors) {
                // timing by weight (the 12 principles): a light, quick character may move faster than a heavy, tall one
                final float maxSpeed = 260f * weightSpeed(a);          // stage units per second (about a third of a 16:9 frame)
                for (int i = 1; i < a.keys.size(); i++) {
                    Film.Key k = a.keys.get(i);
                    if (k.moveDur <= 0) continue;
                    // (v43: not a vehicle's move — a bike is not held to walking pace, and its ride ends when it stops:
                    // stretched, the rider slid on after getting off)
                    if (rideKeys.contains(k)) continue;
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
        // (v22: the whole head with its hair and headwear takes at most 74% of the frame height, and the top of the
        // picture gets a margin of its own — a nod, a bounce or a lifted brow never pushes the hair out of the frame)
        float chin = fb[1] + fb[2] * 0.5f, headTop = fb[4] - 0.05f * heightOf(a) - 18;
        zoom = Math.min(zoom, 0.74f * 720f / Math.max(1, chin - headTop));
        zoom = Math.max(ShotPlanner.ZOOM[ShotPlanner.MCU], Math.min(fb[3], zoom));
        // v41: on a swing going high, or sliding down, a close shot would lose them: a medium shot on the swing's arc
        float[] pl = playAt(a, t);
        boolean moving = pl != null && pl[1] > 6;
        if (moving) zoom = Math.min(zoom, 1.1f);
        if (tight > 1 && !moving) {
            // the next group of words gets a visibly different framing (never a jump cut): closer if the picture
            // allows it, otherwise a little wider
            float base = Math.max(ShotPlanner.ZOOM[ShotPlanner.MCU], Math.min(fb[3], Math.min(fill / 1.1f * 720f / fb[2], 0.74f * 720f / Math.max(1, chin - headTop))));
            if (zoom < base * 1.12f) zoom = Math.max(ShotPlanner.ZOOM[ShotPlanner.MCU], base / 1.2f);
        }
        float fh = 720f / zoom;
        // the eyes on the format's eye line (16:9 the top third, 9:16 the middle); the head never cut: headroom wins
        PixarLead.Format fmt = PixarLead.spec(opt.aspect);
        float eyeY = fb[1] - fb[2] * 0.09f;
        float cy = eyeY - fmt.eyeLine * fh + fh / 2;
        cy = Math.max(cy, fb[1] + 0.04f * fh - 0.1f * fh);
        if (headTop < cy - fh / 2 + 0.06f * fh) cy = headTop - 0.06f * fh + fh / 2;
        float hw = 360f / zoom * TechnicalDirector.ratio(opt.aspect);
        Film.Cam c = new Film.Cam(t, Math.max(hw, Math.min(1280 - hw, fb[0])), cy, zoom, 0);
        c.subject = a;
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
        if (k.body == Pose.SIT) top = ground - h * Math.min(1f, seatedFace(a, t) + 0.115f);     // v35: by the seat's height
        float[] pl = playAt(a, t);
        if (pl != null) top -= pl[0];                                                             // v41: up on the slide
        else if (k.body == Pose.LIE && k.anchor == Film.A_GROUND && a.look != null && a.look.isHumanoid()) {
            // v41: lying on the grass or a bed: low down, the whole body in a medium shot (a close-up of a face turned on
            // its side is no lip-sync shot, and a frame aimed at a standing head finds nothing but the trees)
            float bed = k.seat == Film.SEAT_BED ? Renderer.BED_TOP * h : k.seat == Film.SEAT_SOFA ? Renderer.seatTopOf(Film.SEAT_SOFA) * h : 0;
            float fy = ground - bed - 0.12f * h;
            return new float[]{x, fy, 0.17f * h, Math.min(1.5f, 720f / (0.9f * h)), fy - 0.25f * h};
        }
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
            // v35: sharp at the film's own size — the face on the screen (zoom x face height x pixels per stage unit)
            // is at most 1.5 times the face's pixels in the picture; a small picture gets a medium close-up (62% of
            // the character's height in the frame) rather than an enlarged, soft one
            float px = Math.max(1f, opt.outHeight / 720f);
            float zmax = Math.max(Math.min(ShotPlanner.MAX_ZOOM, 720f / (0.62f * h)), srcPx * 1.5f / (fh * px));
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
                        Film.Key st = a.stateAt(t);
                        if (!st.visible) { ref = Float.NaN; continue; }
                        // v34 (realism): a character walking off the stage is let go — the shot holds and they walk out
                        // of it (the camera never chases anyone off the stage); the rule still counts every move that
                        // stays in the picture
                        if (st.moveDur > 0 && t < st.t + st.moveDur && (st.x < 0 || st.x > 1280)) { ref = Float.NaN; continue; }
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
                // v41: the place's opening wide is held (the manual's establishing shot): the movement in it is seen wide
                if (cut != null && openingWide(sg, i) && cut.t < t0 + ESTAB_HOLD) {
                    if (shotEnd(sg, i) - (t0 + ESTAB_HOLD) >= 0.5f) cut.t = t0 + ESTAB_HOLD; else cut = null;
                }
                if (cut != null && cut.t - t0 >= 0.39f) { sg.cams.add(i + 1, cut); motionCuts++; }
            }
        }
    }

    private int motionCuts, actionCuts, framed;
    /** v41: how long the opening wide of a place is held before the protocol's passes may cut into it (the manual 3.6). */
    static final float ESTAB_HOLD = 2.5f;
    private static boolean openingWide(Film.Seg sg, int i) {
        Film.Cam c = sg.cams.get(i);
        return i == 0 && c.t <= sg.t0 + 0.6f && c.zoom <= 1.3f;
    }
    /** PixarLead counters: ma pauses (Miyazaki), comic beats (Russo / Gunn), shadow passes (Gunn), first-frame reframes (head, feet). */
    private int maPauses, comicBeats, shadowPasses, framedHead, framedFeet, framedFace;
    /** The handbook's beats: thought before action (ch. 6), Dutch angles used (ch. 5: sparingly, at most one per scene). */
    private int thoughtBeats, dutchCount, dutchUsed;
    /** The director's manual: point-of-view shots made for looks (3.4), reactions staged for loud sounds (3.2). */
    private int povShots, loudReactions;
    /** Establishing bridges added between scenes whose place changes (the phone guide 8.2). */
    private int bridges;
    /** The face fill of the lip-sync shots (section 8.3), summed, and how many were measured. */
    private float faceFillSum;
    private int faceFillN;
    private int[] sceneActs;

    private static boolean isAction(int type) {
        return type != Film.G_TALK && type != Film.G_NOD && type != Film.G_LOOK_AWAY && type != Film.G_WHISPER
                && type != Film.G_HEAD_SCRATCH && type != Film.G_WEIGHT_SHIFT;
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
                // v41: not inside the held opening wide of a place (the action is seen wide there)
                if (openingWide(sg, i) && second < t0 + ESTAB_HOLD) continue;
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
        PixarLead.Format fmt = PixarLead.spec(opt.aspect);
        final float side = Math.max(TechnicalDirector.SIDE_MARGIN, fmt.side);
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            sortCams(sg);
            for (int i = 0; i < sg.cams.size(); i++) {
                Film.Cam c = sg.cams.get(i);
                if (c.ease > 0 || c.place) continue;
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
                // v41: a face shot stays on the one it was made for (nudged once, the nearest to its centre may be a
                // neighbour, and the next pass would frame them instead)
                if (c.subject != null && c.subject.stateAt(t).visible && sg.actors.contains(c.subject)) main = c.subject;
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
                        // a neighbour standing almost on the subject (occlusion rule): the frame moves to the
                        // subject's free side as far as the safe zone allows; the renderer draws the subject in front
                        float oh = Renderer.actorHeight(o.look, o.c, art, sg) * bodyAspect(o) / 2;
                        boolean onTop = Math.abs(ob[0] - fb[0]) < oh + fb[2] * 0.6f;
                        if (!cut && !onTop) continue;
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
                float span = maxR - minL, usable = 1 - 2 * side;
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
                    float off = Math.min(fw / 6, fw * (0.5f - side) - half);
                    if (off > 0) c.cx += one.stateAt(t).facing * off;
                }
                // headroom above the highest head; a full shot keeps the feet in the frame too
                float fh = 720f / c.zoom;
                float topEdge = c.cy - fh / 2;
                float room = Math.max(fmt.top * 0.75f, TechnicalDirector.HEADROOM * (c.zoom <= ShotPlanner.ZOOM[ShotPlanner.MWIDE] + 0.01f ? 0.5f : 0.6f));
                if (headTop < topEdge + room * fh) c.cy = headTop - room * fh + fh / 2;
                // a full shot keeps the feet (and their shadow) inside the bottom 85-98% of the frame, above the caption zone
                float feetMax = Math.min(PixarLead.FEET_MAX - 0.04f, 1 - fmt.bottom + 0.02f);
                if (c.zoom <= ShotPlanner.ZOOM[ShotPlanner.WIDE] + 0.01f && feet > c.cy + fh / 2 - (1 - feetMax) * fh) {
                    float need = feet - headTop + (room + 1 - feetMax) * fh;
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
    /** Object inserts already shown (each once). */
    private final Set<Art.Shot> usedInserts = new java.util.HashSet<Art.Shot>();

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
            v.prompt = "CHARACTERS: PLACEMENT: ACTION: GROUNDING: LIGHTING: CAMERA: PRINCIPLES: " + TechnicalDirector.STABLE;
            v.staticCamera = cam.still && cam.ease == 0;
            v.motion = sh.motion; v.actions = sh.actions; v.seconds = sh.dur;
            v.speech = sh.speech; v.closeUp = sh.size >= ShotPlanner.CU; v.words = sh.words;
            // the first-frame checks (RULE_RESIZE_7): the subject's head inside the frame (an extreme close-up fills the
            // frame with the face by design), the feet of a full shot in the bottom 85-98%
            Film.Actor main = null;
            for (Film.Actor a : sg.actors) if (a.c.shown().equals(sh.subject) && a.stateAt(sh.t + 0.05f).visible) { main = a; break; }
            if (main == null) main = nearestTo(sg, cam.cx, sh.t + 0.05f);
            float fh = 720f / cam.zoom, topEdge = cam.cy - fh / 2;
            Film.Key mk = main == null ? null : main.stateAt(sh.t + 0.05f);
            Film.Seg keepS = seg; float keepG = ground;
            seg = sg; ground = sg.ground;
            // v41: one lying down, or up on the slide or the swing, is not checked against a standing head and feet
            boolean offFeet = mk != null && (mk.body == Pose.LIE || playAt(main, sh.t + 0.05f) != null);
            seg = keepS; ground = keepG;
            if (main != null && mk.anchor == Film.A_GROUND && sh.size < ShotPlanner.XCU && !offFeet) {
                float h = heightOf(main);
                v.headTop = (sg.ground - h - topEdge) / fh;
                v.feet = (sg.ground - topEdge) / fh;
                v.fullBody = v.feet < 1.02f && sh.size <= ShotPlanner.WIDE;
                if (sh.size <= ShotPlanner.WIDE && v.feet >= 1.02f) v.fullBody = true;      // a full shot whose feet fell out of the frame
            }
            // section 8.3: in a lip-sync shot the face fills 65-75% of the frame — measured, and what this
            // picture and this frame allow (the whole head inside, the centre 60% of a narrow frame, sharpness)
            if (sh.speech && main != null && sh.size >= ShotPlanner.CU) {
                Film.Seg keepSeg = seg; float keepGround = ground;
                seg = sg; ground = sg.ground;
                float[] fb = faceBox(main, sh.t + 0.05f);
                seg = keepSeg; ground = keepGround;
                float ar = TechnicalDirector.ratio(opt.aspect);
                float chin = fb[1] + fb[2] * 0.5f, headTop = fb[4] - 18;
                v.face = fb[2] * cam.zoom / 720f;
                v.faceWant = Math.min(Math.min(TechnicalDirector.FACE_MAX, TechnicalDirector.SAFE_ZONE * ar / 0.85f),
                        Math.min(0.85f * fb[2] / Math.max(1, chin - headTop), fb[3] * fb[2] / 720f));
                faceFillSum += v.face;
                faceFillN++;
            }
            if (TechnicalDirector.validate(v).isEmpty()) { passed++; continue; }
            List<String> left = TechnicalDirector.correct(v);
            corrected++;
            if (!v.staticCamera || cam.ease > 0) { cam.ease = 0; cam.still = true; }
            if (!v.speech && sh.speech) sh.speech = false;
            for (String f : v.fixes) {
                // the frame moves so the head (or the feet) comes back inside it; a full shot goes a little wider
                if (f.startsWith("framed wider: headroom") && main != null) { float h = heightOf(main); cam.cy = sg.ground - h - 0.08f * fh + fh / 2; framedHead++; }
                else if (f.startsWith("framed wider: feet") && main != null) {
                    float h = heightOf(main), need = h / 0.84f;
                    if (need > fh && 720f / need >= 1f) { cam.zoom = 720f / need; fh = 720f / cam.zoom; }
                    cam.cy = sg.ground - 0.92f * fh + fh / 2; framedFeet++;
                } else if (f.startsWith("framed on the face") && main != null) {
                    // the close-up framed on the face again (65-75%), visibly different from the shot before it
                    Film.Seg keepSeg = seg; float keepGround = ground;
                    seg = sg; ground = sg.ground;
                    Film.Cam prevCam = ci > 0 ? sg.cams.get(ci - 1) : null;
                    Film.Cam fc = faceCam(main, sh.t + 0.05f, cam.light, 1f);
                    if (prevCam != null && Math.abs(fc.zoom - prevCam.zoom) < prevCam.zoom * 0.1f) fc = faceCam(main, sh.t + 0.05f, cam.light, 1.18f);
                    float[] fb = faceBox(main, sh.t + 0.05f);
                    seg = keepSeg; ground = keepGround;
                    cam.cx = fc.cx; cam.cy = fc.cy; cam.zoom = fc.zoom;
                    faceFillSum += fb[2] * cam.zoom / 720f - v.face;       // the average counts the new framing
                    framedFace++;
                }
            }
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
        int e = takes(line, sp, to);
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
    /**
     * v34: the listener's own next words say how they take the news — a goddess who answers a frightened king with
     * "डरो मत, मैं आ गई हूँ" hears his fear with resolve, not fear (her next line within the next four).
     */
    /**
     * v34: what helps a character walk (or a leg in plaster) decides how they move — a wheelchair rolls (never stands, kneels, runs or
     * jumps; a drawn character sits in it, a picture is drawn as it is, since it shows the chair); a walking stick
     * or crutches walk a third slower and never run or jump.
     */
    /** v35: a body that walks with care: a walking aid, a leg in plaster, a limp, an artificial or a missing leg. */
    static boolean careful(Look lk) {
        return lk != null && (lk.aid != Look.AID_NONE || (lk.injury & Look.INJ_LEG) != 0 || (lk.condition & (Look.C_LIMP | Look.C_ARTIFICIAL_LEG | Look.C_NO_LEG)) != 0);
    }

    /** v35: the shot list's line on the bodies of the story: what the director did for each. */
    private String bodiesReport() {
        int limp = 0, arm = 0, leg = 0, fingers = 0, eye = 0, blind = 0, deaf = 0, signs = 0;
        for (Story.CharacterDef c : story.characters) {
            Look l = c.look;
            if (l == null) continue;
            if ((l.condition & (Look.C_LIMP | Look.C_ARTIFICIAL_LEG)) != 0) limp++;
            if (l.missingArm() != 0) arm++;
            if (l.missingLeg() != 0) leg++;
            if ((l.condition & Look.C_FINGERS) != 0) fingers++;
            if ((l.condition & Look.C_ONE_EYE) != 0 || l.glasses == 5) eye++;
            if ((l.condition & Look.C_BLIND) != 0) blind++;
            if (l.cannotHear()) deaf++;
            if (l.signs()) signs++;
        }
        if (limp + arm + leg + fingers + eye + blind + deaf + signs == 0) return "";
        return String.format(java.util.Locale.US, "• Bodies (v35): %d limping (uneven steps, never running), %d with one arm (an empty sleeve; one hand does the work), %d with one leg "
                + "(crutches, or an artificial leg and a limp), %d with fingers missing, %d with one eye, %d blind (feels the way or follows the cane, listens, no point-of-view shot), "
                + "%d deaf (%d sounds or calls not heard — no startle, no turn), %d who sign (%d lines in sign language: hands and face framed together, the words in the subtitle)%n",
                limp, arm, leg, fingers, eye, blind, deaf, deafUnheard, signs, signs, signedLines);
    }

    private void mobility() {
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors) {
            if (a.look == null) continue;
            boolean any = careful(a.look);
            for (Story.Costume co : a.c.costumes) any |= careful(co.look);
            if (!any) continue;
            boolean picture = art != null && art.sprites.containsKey(a.c.id);
            for (int i = 0; i < a.keys.size(); i++) {
                Film.Key k = a.keys.get(i);
                // v34: the look at this moment (a plaster cut off mid-story lets the character run again)
                Look lk = k.costume > 0 && k.costume <= a.c.costumes.size() ? a.c.costumes.get(k.costume - 1).look : a.look;
                int aid = lk.aid;
                if (!careful(lk)) continue;
                k.run = false;
                if (aid == Look.AID_WHEELCHAIR) {
                    if (k.body != Pose.LIE) { k.body = picture ? Pose.STAND : Pose.SIT; k.seat = picture ? -1 : Film.SEAT_WHEELCHAIR; }
                } else if (k.moveDur > 0) {
                    // a walking aid or a plaster: a third slower; a limp on its own: a fifth
                    float slow = aid != Look.AID_NONE || (lk.injury & Look.INJ_LEG) != 0 ? 1.35f : 1.2f;
                    float room = i + 1 < a.keys.size() ? a.keys.get(i + 1).t - k.t : k.moveDur * slow;
                    k.moveDur = Math.max(k.moveDur, Math.min(k.moveDur * slow, room));
                }
            }
            for (java.util.Iterator<Film.Act> it = a.acts.iterator(); it.hasNext(); ) {
                Film.Act act = it.next();
                int ty = act.type;
                Film.Key at = a.keys.isEmpty() ? null : a.stateAt(act.t0);     // v35: read the state (at() would add a key out of time order)
                Look lk = at != null && at.costume > 0 && at.costume <= a.c.costumes.size() ? a.c.costumes.get(at.costume - 1).look : a.look;
                boolean hurt = careful(lk);
                if (!hurt) continue;
                if (ty == Film.G_JUMP || ty == Film.G_BOUNCE || (lk.aid == Look.AID_WHEELCHAIR && (ty == Film.G_DANCE || ty == Film.G_WALK_PLACE))) it.remove();
            }
        }
    }

    /**
     * v34 (realism): every walk is heard, not only an entrance — a move across the stage or out of it gets the steps
     * of its floor (or the stick, crutches or wheelchair of the one moving) unless a step sound already covers it.
     */
    private void walkSounds() {
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors) {
            if (a.look == null || a.look.kind == Look.BIRD || a.look.mount >= 0) continue;
            for (Film.Key k : a.keys) {
                if (k.moveDur < 0.5f || k.anchor != Film.A_GROUND || !k.visible || k.t < sg.t0 || k.t >= sg.t1) continue;
                float t0 = k.t, t1 = k.t + k.moveDur;
                boolean heard = false;
                for (Film.Sfx x : film.sfx) {
                    boolean step = x.type == Film.SFX_STEPS || x.type == Film.SFX_STEPS_HARD || x.type == Film.SFX_STEPS_RUN || x.type == Film.SFX_THUD
                            || x.type == Film.SFX_STICK || x.type == Film.SFX_CRUTCH || x.type == Film.SFX_WHEELCHAIR || x.type == Film.SFX_STEPS_LIMP;
                    if (step && x.t < t1 && x.t + x.dur > t0) { heard = true; break; }
                }
                if (heard) continue;
                Look lk = k.costume > 0 && k.costume <= a.c.costumes.size() ? a.c.costumes.get(k.costume - 1).look : a.look;
                int type = lk.kind == Look.MONSTER ? Film.SFX_THUD : lk.aid == Look.AID_WHEELCHAIR ? Film.SFX_WHEELCHAIR : lk.aid == Look.AID_CRUTCHES ? Film.SFX_CRUTCH
                        : lk.aid == Look.AID_STICK || lk.aid == Look.AID_WALKER ? Film.SFX_STICK
                        : careful(lk) ? Film.SFX_STEPS_LIMP : floorSteps(sg.set, k.run);       // v35: a limp is heard, uneven
                film.sfx.add(new Film.Sfx(type, t0, k.moveDur, lk.kind == Look.MONSTER ? 0.6f : 0.3f));
            }
        }
    }

    /** The steps of a place's floor (as stepsSound, for any part). */
    private static int floorSteps(int set, boolean run) {
        if (run) return Film.SFX_STEPS_RUN;
        switch (set) {
            case Sets.COURTYARD: case Sets.GATE: case Sets.HALL: case Sets.CAVE_IN: case Sets.CAVE_MOUTH: case Sets.CELEBRATION:
            case Sets.ROOFTOP: case Sets.BASEMENT: case Sets.STREET: case Sets.ROOM: return Film.SFX_STEPS_HARD;
            default: return Film.SFX_STEPS;
        }
    }

    /** v34: rain drums on the umbrella of whoever holds one open in the rain outdoors (the renderer opens it there). */
    private void umbrellaRain() {
        for (Film.Seg sg : film.segs) {
            if (!Sets.outdoorSet(sg.set)) continue;
            for (Film.Actor a : sg.actors) {
                if (a.look == null || !(a.look.umbrella || a.umbrellaFrom >= 0) || !a.look.isHumanoid()) continue;
                float start = -1;
                for (int i = 0; ; i++) {
                    float t = sg.t0 + i * 0.25f;
                    boolean end = t >= sg.t1;
                    boolean on = !end && Math.max(film.weather(Film.W_RAIN, t), film.weather(Film.W_STORM, t)) > 0.05f && a.stateAt(t).visible && a.umbrellaAt(a.look, t);
                    if (on && start < 0) start = t;
                    if (!on && start >= 0) {
                        float e = Math.min(t, sg.t1);
                        if (e - start >= 0.5f) film.sfx.add(new Film.Sfx(Film.SFX_UMBRELLA_RAIN, start, e - start, 0.3f));
                        start = -1;
                    }
                    if (end) break;
                }
            }
        }
    }

    /** v34: what a character looks like at a moment (the change of clothes, bandage or plaster worn then). */
    private Look lookAt(Film.Actor a, float t) {
        if (a == null) return null;
        Film.Key k = a.stateAt(t);
        return k.costume > 0 && k.costume <= a.c.costumes.size() ? a.c.costumes.get(k.costume - 1).look : a.look;
    }

    /** v34: a feeling held for a moment, then the one before it again. */
    private void feel(Film.Actor a, float t0, float dur, int emotion) {
        int before = a.stateAt(t0).emotion;
        a.at(t0).emotion = emotion;
        a.at(t0 + dur).emotion = before;
    }

    /** v34 (the situations guide): counts of what the director did for the situations of the story (in the shot list). */
    int eyeLevelShots, blindNoPov, reveals, concernBeats, reliefBeats;

    /**
     * v34 (the situations guide): a change of look is staged, not just swapped. New clothes: a reveal — the whole
     * figure framed medium-wide with a slow push-in, a sparkle, the character proud, the others surprised and then
     * delighted. A new bandage or plaster: the hurt one winces, the others look on with concern. A bandage taken off
     * or a plaster cut: relief all round. Glasses on or off: no fuss. Returns the time after it.
     */
    private float newLook(Film.Actor a, Look before, Story.Costume co, float tc) {
        Look now = co.look;
        if (a == null || now == null || !a.stateAt(tc).visible) return tc;
        boolean hurt = (now.injury & ~before.injury) != 0, healed = (before.injury & ~now.injury) != 0;
        boolean clothes = now.outfit != before.outfit || now.primary != before.primary || now.secondary != before.secondary;
        List<Film.Actor> others = new ArrayList<Film.Actor>();
        for (Film.Actor o : seg.actors) if (o != a && o.stateAt(tc).visible && o.look != null && o.look.isHumanoid()) others.add(o);
        if (hurt) {
            feel(a, tc, 1.4f, Pose.PAIN);
            for (Film.Actor o : others) feel(o, tc + 0.2f, 1.6f, Pose.SAD);
            watch(a, tc, tc + 1.8f, false);
            camOn(a, tc, 1.5f);
            Film.Shot sh = shot(tc, ShotPlanner.MEDIUM, ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, a, others.isEmpty() ? null : others.get(0), ShotPlanner.ESCALATE);
            sh.purpose = "Hurt: " + a.c.shown() + " — " + clip(co.text, 50);
            sh.action = co.text;
            sh.face = "A wince, then brave";
            sh.body = "The hurt arm, leg or head held still";
            sh.cutWhen = "the wince has passed";
            sh.emotionalPurpose = "Concern: the others look on, worried";
            concernBeats++;
            return tc + 1.85f;
        }
        if (healed) {
            feel(a, tc, 1.6f, Pose.RELIEVED);
            for (Film.Actor o : others) feel(o, tc + 0.3f, 1.4f, Pose.HAPPY);
            watch(a, tc, tc + 1.6f, false);
            reliefBeats++;
            return tc + 1.75f;
        }
        if (clothes) {
            // the reveal: the whole new look, held a moment, a slow push-in, a sparkle; the others react
            Film.Key k = a.stateAt(tc);
            float h = heightOf(a), x = xAt(a, tc);
            cam(tc, x, ground - h * 0.52f, 1.15f, 0);
            Film.Shot sh = shot(tc, ShotPlanner.MWIDE, ShotPlanner.SINGLE, 0, ShotPlanner.PUSH_IN, a, null, ShotPlanner.PEAK);
            sh.purpose = "Reveal: " + a.c.shown() + "'s new look — " + clip(co.text, 50);
            sh.action = a.c.shown() + " shows the new look";
            sh.face = faceOf(Pose.PROUD);
            sh.body = "Standing tall, a little turn to show it";
            sh.cutWhen = "the new look has been seen whole";
            sh.emotionalPurpose = "Delight: the change is an event";
            feel(a, tc, 1.8f, Pose.PROUD);
            for (Film.Actor o : others) { feel(o, tc + 0.4f, 0.6f, Pose.SURPRISED); feel(o, tc + 1.0f, 1.0f, Pose.HAPPY); }
            watch(a, tc, tc + 2.0f, false);
            Film.Fx fx = new Film.Fx(Film.FX_SPARKLE, tc + 0.2f, tc + 1.4f);
            fx.x = x; fx.y = ground - h * 0.6f;
            seg.fx.add(fx);
            film.sfx.add(new Film.Sfx(Film.SFX_TWINKLE, tc + 0.2f, 1.0f, 0.3f));
            if (k.body != Pose.SIT && k.body != Pose.LIE && now.aid == Look.AID_NONE) a.acts.add(new Film.Act(tc + 0.3f, tc + 1.5f, Film.G_PROUD));
            reveals++;
            return tc + 2.05f;
        }
        return tc;
    }

    /** v34: the clothes each character last changed into (CharacterDef.costumes, 1-based; absent = their own). */
    private final Map<Story.CharacterDef, Integer> costumeNow = new HashMap<Story.CharacterDef, Integer>();

    private Film.Line nextOwnLine(Film.Actor to, Film.Line line) {
        int i0 = film.lines.indexOf(line);
        if (i0 < 0) return null;
        for (int i = i0 + 1; i < Math.min(film.lines.size(), i0 + 5); i++) if (film.lines.get(i).who == to.c) return film.lines.get(i);
        return null;
    }

    private boolean reassures(Film.Actor to, Film.Line line) {
        Film.Line l = nextOwnLine(to, line);
        return l != null && Txt.has(l.text + " " + l.manner, "डरो मत", "डरो नहीं", "घबराओ मत", "घबराओ नहीं", "चिंता मत", "चिंता न", "फ़िक्र मत", "फिक्र मत",
                "मैं आ गई", "मैं आ गया", "मैं हूँ न", "हम हैं न", "मैं बचाऊँग", "मैं बचाउंग", "शांत", "don't worry", "do not worry", "don't be afraid",
                "do not be afraid", "fear not", "i am here", "i'm here", "calm", "relax", "it's okay", "it is okay");
    }

    /**
     * v34: how this listener takes the line — the empathy of the line, unless the listener's own next words say
     * otherwise: a goddess who answers fear with "डरो मत" hears it with resolve, one who answers a demon's threat
     * in anger hears it in anger; a frightened answer keeps the fear.
     */
    private int takes(Film.Line line, Film.Actor sp, Film.Actor to) {
        int e = empathy(line.emotion, sp, to);
        if (e != Pose.SCARED && e != Pose.SAD) return e;
        if (reassures(to, line)) return e == Pose.SCARED ? Pose.DETERMINED : Pose.SAD;
        Film.Line next = nextOwnLine(to, line);
        if (e == Pose.SCARED && next != null && (next.emotion == Pose.ANGRY || next.emotion == Pose.DETERMINED)) return next.emotion;
        return e;
    }

    private static int empathy(int emo, Film.Actor sp, Film.Actor to) {
        switch (emo) {
            case Pose.SAD: case Pose.PAIN: return Pose.SAD;
            case Pose.SCARED: return Pose.SCARED;
            case Pose.SURPRISED: return Pose.SURPRISED;
            case Pose.HAPPY: case Pose.LAUGH: return Pose.HAPPY;
            case Pose.EVIL: return to.look.hero ? Pose.SCARED : Pose.EVIL;
            case Pose.ANGRY: return !sp.look.hero && to.look.hero ? Pose.SCARED : to.look.hero ? Pose.SAD : Pose.ANGRY;
            case Pose.DETERMINED: return to.look.hero ? Pose.DETERMINED : Pose.NEUTRAL;
            case Pose.SUSPICIOUS: return to.look.hero ? Pose.CURIOUS : Pose.NEUTRAL;
            case Pose.RELIEVED: return Pose.HAPPY;
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
        Art.Sprite tsp = art == null ? null : art.sprites.get(to.c.id);
        if (tsp != null && tsp.faceKnown) {
            // v34: a picture whose face is known is framed by its face, as the speaking close-ups are — the whole head
            // with its crown in the frame (a rider's face is near the top of a picture that holds her animal too)
            Film.Cam fc = faceCam(to, end + 0.05f, plan.light, size >= ShotPlanner.CU ? 1f : 0.75f);
            float hw = 360f / fc.zoom * TechnicalDirector.ratio(opt.aspect);
            c = new Film.Cam(end + 0.05f, Math.max(hw, Math.min(1280 - hw, fc.cx + tk.facing * (1280f / fc.zoom) * 0.08f)), fc.cy, fc.zoom, 0);
        }
        c.still = true;
        c.light = plan.light;
        c.reverse = true;                       // the listener's face: the place's reverse angle behind it when the user gave one
        seg.cams.add(c);
        boolean changed = false;
        Film.Key r = to.at(end + 0.3f);     // anticipation: a tiny pause, then the face changes (§23)
        if (r.emotion == Pose.NEUTRAL) {
            int e = takes(line, sp, to);
            r.emotion = e == Pose.NEUTRAL ? Pose.SURPRISED : e;
            changed = true;
        }
        if (changed) to.at(end + 0.35f + plan.breathe + 0.5f).emotion = Pose.NEUTRAL;
        boolean ots = overShoulder(sp, to, end + 0.05f);
        Film.Shot sh = shot(end + 0.05f, size, ots ? ShotPlanner.OTS : ShotPlanner.SINGLE, plan.height, ShotPlanner.STATIC, to, sp, plan.stage);
        sh.reaction = true;
        if (ots) sh.ots = sp.c.shown();
        sh.purpose = "Reaction: the audience feels the line through " + to.c.shown() + "'s face" + (ots ? ", over " + sp.c.shown() + "'s shoulder" : "") + ", and it is allowed to breathe";
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
        // v40: one who walks round the fire: what the line's words say about the walk (from behind to the front, the
        // other way round, a short sit on the empty log) shapes the path, and is taken out of the rest of the manner
        if (pacers.containsKey(a)) m = paceEvents(a, m, t0, t1);
        if (m.length() == 0) return;
        List<Film.Actor> me = new ArrayList<Film.Actor>();
        me.add(a);
        postureFrom(m, t0, a, me);
        mealCU = null;                                // a manner ("sipping tea") stays in the dialogue's own framing
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
        if (Txt.has(m, "कदम पीछे", "पीछे कदम", "पीछे खींच", "step back")) {
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
        // (v40: "कुल्हड़ को दोनों हाथों से पकड़ कर" holds a cup, nobody's hand)
        if (Txt.has(m, "हाथ पकड़", "का हाथ", "के हाथ", "की हाथ", "हाथ थाम") && !Txt.has(m, "हाथों से", "हाथ से") && to != null) {
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
        dirScene = story.scenes.get(si);
        dirBeat = bi;
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
        // v41: the swing or the slide is in the place itself (played on, swaying in the wind): no picture of it over the scene
        if (shot != null && shot.object && shot.keys != null) for (String key : shot.keys)
            if ((swingProp != null && Txt.has(key, PlayPics.SWING_KEYS)) || (slideProp != null && Txt.has(key, PlayPics.SLIDE_KEYS))) { shot = null; break; }
        // v41: an uploaded picture of a moment is a reference, never shown in place of the moment: the rig acts it (the
        // picture stays in the library for the user to look at)
        if (shot != null && !shot.object) {
            if (!momentNoted) film.notes.add("  ↳ pictures of moments are kept as reference: the characters act those moments");
            momentNoted = true;
            shot = null;
        }
        // an object's picture is an insert of the thing itself, once — when the story first brings it in
        if (shot != null && shot.object && !usedInserts.add(shot)) shot = null;
        // a picture of the moment headwear changes hands shows the old state (the owner still wearing it): the
        // staged action is the truth here, the insert is left out (v22, the continuity rule)
        if (shot != null && !shot.object && Txt.has(text, "पगड़ी", "टोपी", "साफ़ा", "turban", "cap", "pagdi") && Txt.has(text, "छीन", "झपट", "उड़ा", "उतार", "snatch", "grab", "takes")) {
            film.notes.add("Insert left out: the picture of the moment the headwear is taken would show it still worn");
            shot = null;
        }
        estab = establishing || leading;      // descriptions before anyone speaks set how things are from the start
        String cue = newCues(b.cue, text);
        if (cue.length() > 0) {
            Film.Actor who = null;
            for (Story.CharacterDef c : ScriptParser.mentions(story, text)) { who = actor(c); if (who != null) break; }
            natureFrom(cue, tc, who);
        }
        for (String s : sents) {
            float d = staged(s, tc, establishing);
            tc += d;
        }
        estab = false;
        // the director's manual (3.6): an establishing shot wants 4-10 s; the protocol caps every shot at 4 s — so it is
        // held for the top of the cap (3.9 s) before anything comes closer
        float minDur = establishing ? 2.5f : 2.0f;
        if (tc - t0 < minDur) tc = t0 + minDur;
        if (establishing) {
            // nothing cuts closer before the establishing shot has been held: a cut planned inside it (an entrance, a
            // first action) waits until the hold is over — the action itself is seen wide, as the manual's table wants
            for (Film.Cam c : seg.cams) if (c.ease == 0 && c.t > t0 + 0.15f && c.t < t0 + 2.5f) c.t = t0 + 2.5f;
        }
        if (li >= 0) { // narrator speaks the direction
            Film.Line l = film.lines.get(li);
            l.start = t0 + 0.2f;
            sub(l.start, l.start + l.dur, "", l.shown);
            if (tc < l.start + l.dur + 0.3f) tc = l.start + l.dur + 0.3f;
        }
        if (shot != null) {
            Film.Fx fx = new Film.Fx(Film.FX_SHOT, t0 + 0.2f, t0 + (shot.object ? 2.8f : 4.4f));
            fx.pic = shot.pic;
            seg.fx.add(fx);
            // the action continues after the cinematic picture
            float shift = fx.t1 - t0 - 0.3f;
            shiftAfter(t0 + 0.3f, shift);
            tc += shift;
            // the picture is a shot of its own: a cut into it and a cut back to the same framing, so the shot list
            // and the frame-by-frame check see the cuts the viewer sees
            Film.Cam at = null;
            for (Film.Cam c : seg.cams) if (c.t <= fx.t0 + 1e-3f && (at == null || c.t >= at.t)) at = c;
            Film.Cam in = at == null ? new Film.Cam(fx.t0, 640, 372, 1.08f, 0) : new Film.Cam(fx.t0, at.cx, at.cy, at.zoom, 0);
            Film.Cam out = at == null ? new Film.Cam(fx.t1, 640, 372, 1.08f, 0) : new Film.Cam(fx.t1, at.cx, at.cy, at.zoom, 0);
            if (at != null) { in.light = at.light; out.light = at.light; in.angle = at.angle; out.angle = at.angle; in.roll = at.roll; out.roll = at.roll; }
            in.keep = true; in.still = true; out.keep = true; out.still = true;
            seg.cams.add(in);
            seg.cams.add(out);
            String what = Txt.withoutParens(text);
            if (what.length() > 60) what = what.substring(0, 60) + "…";
            Film.Shot ins = shot(fx.t0, shot.object ? ShotPlanner.CU : ShotPlanner.WIDE, ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, null, null, ShotPlanner.DEVELOP);
            ins.subject = shot.object ? "the thing itself (insert)" : "the moment, as a picture";
            ins.purpose = shot.object ? "Insert shot: the thing itself in close-up, from its own picture — once, when the story first brings it in"
                    : "A cinematic picture of this moment from the library, shown as a still while the narration goes on";
            ins.action = what;
            ins.face = "—";
            ins.body = "—";
            ins.cutWhen = "the picture has been seen (" + String.format(java.util.Locale.US, "%.1f", fx.t1 - fx.t0) + " s)";
            ins.emotionalPurpose = shot.object ? "The audience sees exactly what the story is about" : "The stage direction made visible";
            ins.sound = "The scene's ambience under the picture";
            film.notes.add("  ↳ cinematic shot: " + Txt.withoutParens(text).substring(0, Math.min(40, Txt.withoutParens(text).length())) + "…");
        }
        return tc;
    }

    // ---------------------------------------------------------------- v41: everyday words
    /**
     * v41: stages one sentence; when nothing in it was staged (words the director has no rule of its own for), the
     * everyday words give its character at least a face and a gesture — no sentence of the script goes by with nothing
     * on the screen.
     */
    private float staged(String s, float t, boolean establishing) {
        // v43: two clauses, each with its own subject named ("माँ फिसल जाती हैं और राहुल उनका हाथ पकड़कर उन्हें उठाता है"):
        // each is staged in its turn (one sentence gave both actions to the first one named). Not for rides, which
        // read the whole sentence ("स्कूटर स्टार्ट करता है और प्रिया पीछे बैठती है").
        if (!establishing && seg != null && !Txt.has(s, RIDE_VERBS) && vehicleIn(s + " से आ", false) == 0) {
            List<String> cl = clauses(s);
            if (cl.size() == 2) {
                List<Story.CharacterDef> m0 = ScriptParser.mentions(story, cl.get(0)), m1 = ScriptParser.mentions(story, cl.get(1));
                if (!m0.isEmpty() && !m1.isEmpty()) {
                    Story.CharacterDef s0 = m0.get(subjectIndex(cl.get(0), m0)), s1 = m1.get(subjectIndex(cl.get(1), m1));
                    if (s0 != s1 && !m0.contains(s1) && actor(s0) != null && actor(s1) != null) {
                        float d0 = staged(cl.get(0), t, false);
                        lastSubject = s0;
                        float d1 = staged(cl.get(1), t + d0, false);
                        splitSentences++;
                        return d0 + d1;
                    }
                }
            }
        }
        int before = stagedCount(), sfx0 = film.sfx.size();
        // v43: the one the sentence is about — when all they got was a turn of the head to a sound in it ("धड़ाम से
        // ज़मीन पर गिरता है" startled everyone, and the fall itself was lost), the everyday words still stage them
        Film.Actor who = null;
        if (seg != null) {
            List<Story.CharacterDef> ms = ScriptParser.mentions(story, s);
            if (!ms.isEmpty()) who = actor(ms.get(subjectIndex(s, ms)));
        }
        int acts0 = who != null ? who.acts.size() : 0;
        String sig0 = who != null ? stateSig(who) : null;
        float d = sentence(s, t, establishing);
        boolean nothing = stagedCount() == before && film.sfx.size() == sfx0;
        boolean incidental = !nothing && who != null && who.stateAt(t).visible && stateSig(who).equals(sig0);
        if (incidental)
            for (int i = acts0; i < who.acts.size(); i++) {
                int ty = who.acts.get(i).type;
                if (ty != Film.G_LISTEN && ty != Film.G_TURN && ty != Film.G_LOOK_AWAY && ty != Film.G_STEP_BACK) { incidental = false; break; }
            }
        if (nothing || incidental) d = Math.max(d, everyday(s, t));
        return d;
    }

    /** v43: an actor's states in order (where, posture, seat, face, seen, eyes, things in hand, sweat) — the facing left out. */
    private static String stateSig(Film.Actor a) {
        StringBuilder b = new StringBuilder();
        String last = null;
        for (Film.Key k : a.keys) {
            String one = Math.round(k.x) + "/" + k.body + "/" + k.seat + "/" + k.emotion + "/" + k.visible + "/" + k.eyesShut + "/" + k.holdR + "/" + k.holdL + "/" + k.sweat;
            if (!one.equals(last)) { b.append(one).append(';'); last = one; }
        }
        return b.toString();
    }

    private int stagedCount() {
        int n = seg.fx.size();
        for (Film.Actor a : seg.actors) n += a.acts.size() + a.keys.size() * 1000;
        return n;
    }

    private static final int EV_NONE = 0, EV_NEAR = 1, EV_FALL = 2, EV_HIDE = 3, EV_KNEEL = 4, EV_FLOOR = 5, EV_STAND = 6,
            EV_EYES = 7, EV_WAKE = 8, EV_UMBRELLA = 9, EV_WALK = 10, EV_READ = 12, EV_SWEEP = 13, EV_WASH = 14,
            EV_COOK = 15, EV_LOOK_ROUND = 16, EV_THUD = 17, EV_DOOR = 18, EV_GIVE = 19, EV_RUN = 20, EV_KNOCK = 21,
            EV_SWEAT = 22, EV_KICK = 23;
    /**
     * v41: the everyday words, each row: its words (Hindi stems, spellings, English), the face (-1: the face the feeling
     * words give), the gesture (-1: none), how long, and what else it does. The first row that fits is the action.
     */
    private static final Object[][] EVERYDAY = {
        {new String[]{"पैर छू", "पाँव छू", "पांव छू", "चरण छू", "चरण स्पर्श", "touches the feet", "touches her feet", "touches his feet"}, Pose.HAPPY, Film.G_BOW, 2.2f, EV_NEAR},
        {new String[]{"धक्का", "धकेल", "pushes", "shoves"}, Pose.ANGRY, Film.G_PUSH, 1.4f, EV_NEAR},
        // v43: clinging, a hand held out to help up
        {new String[]{"लिपट", "गले लग", "गले से लग", "clings to", "hugs", "embraces"}, Pose.HAPPY, Film.G_HUG, 2.0f, EV_NEAR},
        {new String[]{"हाथ पकड़", "हाथ थाम", "सहारा दे", "सहारा देकर", "उठने में मदद", "helps", "takes her hand", "takes his hand", "holds her hand", "holds his hand", "helping"}, Pose.HAPPY, Film.G_HOLD_HAND, 2.0f, EV_NEAR},
        {new String[]{"चिढ़ा", "छेड़", "teases", "mocks", "makes fun"}, Pose.LAUGH, Film.G_POINT, 1.8f, EV_NONE},
        {new String[]{"माफ़ी", "माफी", "क्षमा", "sorry", "apologi"}, Pose.SAD, Film.G_BOW, 2.0f, EV_NONE},
        {new String[]{"हाथ जोड़", "नमस्कार", "प्रणाम", "folds his hands", "folds her hands", "joins his hands", "joins her hands"}, Pose.HAPPY, Film.G_BOW, 1.8f, EV_NONE},
        {new String[]{"पूजा", "प्रार्थना", "आरती", "दुआ", "prays", "praying", "worship"}, Pose.RELIEVED, Film.G_BOW, 2.6f, EV_EYES},
        {new String[]{"छाता", "छतरी", "umbrella"}, -1, Film.G_REACH, 1.2f, EV_UMBRELLA},
        {new String[]{"छिप", "छुप", "hides", "hiding", "ducks down", "ducks behind"}, Pose.SCARED, -1, 2.2f, EV_HIDE},
        // v43: peeping out from behind something (a crouch), or a curious look out
        {new String[]{"की ओट से", "के पीछे से झाँक", "के पीछे से झांक", "from behind"}, Pose.CURIOUS, -1, 2.0f, EV_HIDE},
        {new String[]{"झाँक", "झांक", "peeks", "peeps", "peeking", "peers"}, Pose.CURIOUS, Film.G_TURN, 1.6f, EV_NONE},
        {new String[]{"घुटनों के बल", "घुटने के बल", "घुटने टेक", "kneels", "on his knees", "on her knees"}, -1, -1, 1.6f, EV_KNEEL},
        {new String[]{"पालथी", "cross-legged", "cross legged"}, -1, -1, 1.4f, EV_FLOOR},
        // v44: unsteady after drinking (a sway, a stumble caught) — before the fall's words
        {new String[]{"नशे में", "डगमगा", "staggers", "staggering", "drunkenly", "totters", "tipsy", "sways on"}, Pose.DIZZY, Film.G_STAGGER, 3.2f, EV_NONE},
        {new String[]{"गिर", "फिसल", "ठोकर", "लड़खड़ा", "falls", "trips", "slips", "stumbles", "tumbles"}, Pose.PAIN, Film.G_FLAIL, 2.8f, EV_FALL},
        {new String[]{"उठ खड़", "खड़ा हो", "खड़ी हो", "खड़े हो", "उठ जा", "उठ बैठ", "उठता", "उठती", "उठते", "stands up", "gets up", "rises"}, -1, -1, 1.0f, EV_STAND},
        {new String[]{"जाग", "नींद खुल", "आँख खुल", "आंख खुल", "wakes up", "awakes"}, -1, Film.G_RUB_EYES, 1.8f, EV_WAKE},
        {new String[]{"अंगड़ाई", "उबासी", "जम्हाई", "हाथ-पैर फैला", "हाथ पैर फैला", "बदन सीधा", "शरीर सीधा", "stretches", "yawn"}, -1, Film.G_STRETCH, 1.8f, EV_NONE},
        {new String[]{"आँखें मल", "आंखें मल", "आँख मल", "rubs his eyes", "rubs her eyes"}, -1, Film.G_RUB_EYES, 1.6f, EV_NONE},
        {new String[]{"छींक", "खाँस", "खांस", "sneez", "cough"}, -1, Film.G_COUGH, 1.4f, EV_NONE},
        {new String[]{"सिर खुजा", "scratches his head", "scratches her head"}, Pose.CURIOUS, Film.G_HEAD_SCRATCH, 1.8f, EV_NONE},
        {new String[]{"ना में सिर", "इनकार में सिर", "न में सिर", "मना करता", "मना करती", "shakes his head", "shakes her head"}, -1, Film.G_SHAKE_HEAD, 1.4f, EV_NONE},
        {new String[]{"हाँ में सिर", "सिर हिला", "सहमति", "nods"}, -1, Film.G_NOD, 1.2f, EV_NONE},
        {new String[]{"आँख मार", "आंख मार", "winks"}, Pose.HAPPY, Film.G_NOD, 1.0f, EV_NONE},
        {new String[]{"जीभ निकाल", "मुँह बना", "मुंह बना", "makes a face", "sticks out his tongue", "sticks out her tongue", "pulls a face"}, Pose.LAUGH, Film.G_WIGGLE, 1.6f, EV_NONE},
        {new String[]{"आँखें बंद", "आंखें बंद", "आँख बंद", "closes his eyes", "closes her eyes", "shuts his eyes", "shuts her eyes"}, Pose.RELIEVED, -1, 2.2f, EV_EYES},
        {new String[]{"काँप", "कांप", "ठिठुर", "कंपकंपा", "shiver", "trembles"}, Pose.SCARED, Film.G_SHIVER, 2.2f, EV_NONE},
        {new String[]{"फुसफुसा", " कान में", "whisper"}, Pose.WHISPER, Film.G_WHISPER, 1.8f, EV_NEAR},
        {new String[]{"चिल्ला", "चीख", "shouts", "yells", "screams"}, Pose.ANGRY, Film.G_FIST, 1.6f, EV_NONE},
        {new String[]{"सीटी", "whistles"}, Pose.HAPPY, Film.G_WEIGHT_SHIFT, 1.6f, EV_NONE},
        // v43: shaving at the mirror, ironing a shirt, hanging the washing out
        {new String[]{"दाढ़ी बना", "शेव", "रेज़र", "रेजर", "shave", "razor"}, -1, Film.G_MIRROR, 2.4f, EV_NONE},
        {new String[]{"इस्त्री", "प्रेस करता", "प्रेस करती", "irons", "ironing"}, -1, Film.G_PAT, 2.4f, EV_NONE},
        {new String[]{"कपड़े टाँग", "कपड़े टांग", "रस्सी पर", "hangs the", "hangs up", "hangs out"}, -1, Film.G_REACH, 1.6f, EV_NONE},
        // v43: in the blazing sun — a hand over the eyes; sweat
        {new String[]{"आँखों पर हाथ", "आंखों पर हाथ", "shields his eyes", "shields her eyes", "shades his eyes", "shades her eyes"}, -1, Film.G_SHIELD_EYES, 1.8f, EV_NONE},
        {new String[]{"पसीन", "sweating", "sweats", "drenched in sweat"}, Pose.SAD, Film.G_WEIGHT_SHIFT, 1.8f, EV_SWEAT},
        // v43: dances by name
        {new String[]{"भांगड़ा", "भाँगड़ा", "गरबा", "डांडिया", "ठुमके", "नाचता", "नाचती", "नाचते", "नाच रह", "नृत्य", "dances", "dancing", "bhangra", "garba"}, Pose.HAPPY, Film.G_DANCE, 3.0f, EV_NONE},
        {new String[]{"गाना गा", "गीत गा", "गुनगुना", "गाता", "गाती", "गाते", "sings", "hums"}, Pose.HAPPY, Film.G_BOUNCE, 2.4f, EV_NONE},
        // v43: pointing (before the sky: "points at the sky")
        {new String[]{"उँगली", "उंगली", "इशारा कर", "इशारे से", "points at", "points to", "pointing"}, Pose.CURIOUS, Film.G_POINT, 1.6f, EV_NONE},
        {new String[]{"ऊपर देख", "आसमान", "आकाश", "तारे देख", "looks up", "the sky", "the stars"}, Pose.CURIOUS, Film.G_LOOK_UP, 1.8f, EV_NONE},
        {new String[]{"ढूँढ", "ढूंढ", "खोज", "तलाश", "searches", "looks for", "looking for", "looks around"}, Pose.CURIOUS, -1, 2.4f, EV_LOOK_ROUND},
        {new String[]{"के पीछे भाग", "के पीछे दौड़", "पीछे-पीछे भाग", "पीछे पीछे भाग", "पीछे-पीछे दौड़", "पीछे पीछे दौड़", "पीछा कर", "runs after", "chases",
                "जॉगिंग", "जोगिंग", "दौड़ लगा", "jogging", "jogs", "goes for a run"}, Pose.HAPPY, -1, 1.6f, EV_RUN},
        {new String[]{"चुपके", "दबे पाँव", "दबे पांव", "पंजों के बल", "sneaks", "tiptoe"}, Pose.CURIOUS, -1, 2.4f, EV_WALK},
        {new String[]{"सीढ़ियाँ", "सीढ़ियां", "सीढ़ी", "उतर", "the stairs", "the steps", "comes down", "goes down"}, -1, -1, 2.2f, EV_WALK},
        {new String[]{"खिड़की से", "खिड़की के बाहर", "बाहर देख", "out of the window", "looks out"}, Pose.CURIOUS, Film.G_TURN, 1.6f, EV_NONE},
        {new String[]{"कपड़े बदल", "ड्रेस बदल", "changes clothes", "changes his clothes", "changes her clothes"}, -1, Film.G_TURN, 1.4f, EV_NONE},
        {new String[]{"धीरे-धीरे चल", "धीरे धीरे चल", "टहल", "चहलकदमी", "लंगड़ा", "walks slowly", "strolls", "paces", "limps"}, -1, -1, 2.6f, EV_WALK},
        {new String[]{"सहला", "पुचकार", "थपथपा", " pats", " pets", "strokes"}, Pose.HAPPY, Film.G_PAT, 1.8f, EV_NEAR},
        {new String[]{"दाना डाल", "दाना खिला", "scatters", "feeds the birds"}, Pose.HAPPY, Film.G_THROW, 1.6f, EV_NONE},
        // v43: food served, a thing shown, a thing taken from someone
        {new String[]{"परोस", "serves", "serving"}, Pose.HAPPY, Film.G_OFFER, 1.8f, EV_GIVE},
        {new String[]{"दिखाता", "दिखाती", "दिखाते", "shows his", "shows her", "shows the", "shows a"}, Pose.PROUD, Film.G_OFFER, 1.8f, EV_GIVE},
        {new String[]{"ले लेता", "ले लेती", "ले लेते", "ले लिया", "ले ली", "छीन", "takes the", "takes a", "takes his", "takes her", "snatches"}, -1, Film.G_REACH, 1.3f, EV_NEAR},
        {new String[]{"तोहफ़ा", "तोहफा", "उपहार", "गिफ्ट", "थमा", "सौंप", "पकड़ा देता", "पकड़ा देती", "देता है", "देती है", "देते हैं", "gives", "hands him", "hands her", "hands over", "offers"}, Pose.HAPPY, Film.G_GIVE, 1.8f, EV_GIVE},
        {new String[]{"फेंक", "उछाल", "throws", "tosses"}, -1, Film.G_THROW, 1.2f, EV_NONE},
        {new String[]{"किक", "लात मार", "शॉट मार", "kicks", "shoots the ball"}, Pose.DETERMINED, Film.G_PLAY, 1.2f, EV_KICK},
        {new String[]{"पतंग", "kite"}, Pose.HAPPY, Film.G_PULL_ROPE, 2.6f, EV_NONE},
        {new String[]{"पकड़", "लपक", "catches", "grabs"}, -1, Film.G_REACH, 1.2f, EV_NONE},
        {new String[]{"बिस्तर पर कूद", "उछल-कूद", "उछलकूद", "bounces"}, Pose.LAUGH, Film.G_BOUNCE, 2.0f, EV_NONE},
        {new String[]{"कूद", "उछल", "छलांग", "छलाँग", "फुदक", "jumps", " hops", "leaps"}, Pose.HAPPY, Film.G_JUMP, 1.0f, EV_NONE},
        {new String[]{"तैर", "swims", "swimming", "छप-छप", "छपछप"}, Pose.HAPPY, Film.G_SPLASH, 2.4f, EV_NONE},
        {new String[]{"पोंछा", "mops", "wipes the floor"}, -1, -1, 3.2f, EV_SWEEP},
        {new String[]{"कपड़े सुखा", "कपड़े धो", "धुलाई", "बर्तन मांज", "बर्तन माँज", "hangs the clothes", "washes the clothes", "does the laundry",
                "साबुन लगा", "रगड़-रगड़", "scrubs"}, -1, -1, 3.2f, EV_WASH},
        {new String[]{"सब्ज़ी काट", "सब्जी काट", "प्याज़ काट", "प्याज काट", "टमाटर काट", "आलू काट", "आटा गूँध", "आटा गूंध", "रोटी बेल", "chops", "chopping", "cuts vegetables", "cuts the vegetables", "kneads"}, -1, -1, 3.2f, EV_COOK},
        {new String[]{"चिट्ठी", "पत्र पढ़", "ख़त पढ़", "नोट पढ़", "letter", "reads the note", "reads a note"}, -1, -1, 3.0f, EV_READ},
        {new String[]{"खटखटा", "दस्तक", "knocks", "knocking", "knock on", "knocked"}, -1, Film.G_REACH, 1.4f, EV_KNOCK},
        {new String[]{"दरवाज़ा", "दरवाजा", "दरवाज़े", "दरवाजे", "किवाड़", "गेट खोल", "गेट बंद", "the door", "the gate"}, -1, Film.G_REACH, 1.4f, EV_DOOR},
        {new String[]{"जलाता", "जलाती", "जलाते", "बुझा", "बत्ती", "स्विच", "पंखा", "lights the", "lights a", "switches", "turns on", "turns off", "blows out"}, -1, Film.G_REACH, 1.4f, EV_NONE},
        {new String[]{"ताला", "चाबी", "locks", "unlocks"}, -1, Film.G_REACH, 1.4f, EV_NONE},
        {new String[]{"कार्टून", "cartoon", "on tv", "on the tv", "टीवी देख", "watches tv", "watching tv"}, Pose.HAPPY, -1, 2.4f, EV_NONE},
        {new String[]{"वीडियो गेम", "video game", "मोबाइल", "mobile", "टीवी", "television", "watches the tv"}, Pose.CURIOUS, Film.G_REACH, 2.4f, EV_NONE},
        {new String[]{"फोटो", "फ़ोटो", "तस्वीर खींच", "सेल्फ़ी", "सेल्फी", "photo", "selfie"}, Pose.HAPPY, Film.G_REACH, 1.8f, EV_NONE},
        {new String[]{"घड़ी", "समय देख", "the time", "his watch", "her watch"}, Pose.CURIOUS, Film.G_LOOK_AWAY, 1.4f, EV_NONE},
        {new String[]{"क्रिकेट", "फुटबॉल", "फ़ुटबॉल", "बैडमिंटन", "कबड्डी", "cricket", "football", "badminton", "खेलता", "खेलती", "खेलते", "plays"}, Pose.HAPPY, Film.G_PLAY, 2.6f, EV_NONE},
        {new String[]{"ओढ़", "लपेट", "wraps", "pulls the blanket"}, -1, Film.G_CLUTCH, 1.8f, EV_NONE},
        {new String[]{"यूनिफ़ॉर्म", "यूनिफॉर्म", "वर्दी", "uniform"}, -1, Film.G_SCREEN, 1.8f, EV_NONE},
        {new String[]{"बस्ता", "स्कूल बैग", "थैला", "school bag", "schoolbag", "backpack"}, -1, Film.G_REACH, 1.4f, EV_NONE},
        {new String[]{"पहन", "बाँध", "बांध", "puts on", "wears", "ties his", "ties her"}, -1, Film.G_BOW, 1.6f, EV_NONE},
        {new String[]{"झुक", "bends", " bows"}, -1, Film.G_BOW, 1.4f, EV_NONE},
        {new String[]{"चढ़", "climbs", " boards"}, -1, Film.G_REACH, 1.6f, EV_NONE},
        {new String[]{"गिनत", "गिनती", "गिनने", " counts"}, Pose.CURIOUS, Film.G_REACH, 1.8f, EV_NONE},
        {new String[]{"काट", "फुला", " cuts", "blows up", "inflates"}, Pose.HAPPY, Film.G_REACH, 1.6f, EV_NONE},
        {new String[]{"उठा", "रख", "तोड़", "खोल", "बंद कर", "picks up", "puts down", "plucks", "opens", "closes", "lifts", "places"}, -1, Film.G_REACH, 1.3f, EV_NONE},
    };
    /** v41: the feeling words of a direction: the face, a gesture that shows it (-1: none), how long. */
    private static final Object[][] FEELINGS = {
        {new String[]{"रोता", "रोती", "रोते", "रोने", "रो पड़", "रो रहा", "रो रही", "रो रहे", "रोकर", "आँसू", "आंसू", "सिसक", "फफक", "cries", "crying", "sobs", "weeps", "in tears"}, Pose.SAD, Film.G_CRY, 2.6f},
        {new String[]{"खिलखिला", "हँस", "हंस", "laugh", "giggl", "chuckl"}, Pose.LAUGH, Film.G_LAUGH, 1.8f},
        {new String[]{"मुस्कुरा", "मुस्करा", "मुस्कान", "smiles", "grins"}, Pose.HAPPY, -1, 1.8f},
        {new String[]{"गुस्सा", "गुस्से", "नाराज़", "नाराज", "क्रोध", "तमतमा", "पैर पटक", "angry", "furious", "annoyed", "anger", "rage", "stamps his foot", "stamps her foot"}, Pose.ANGRY, Film.G_FIST, 1.8f},
        {new String[]{"डर", "घबरा", "सहम", "भयभीत", "scared", "afraid", "frightened", "terrified", "panics"}, Pose.SCARED, Film.G_TREMBLE, 2.0f},
        {new String[]{"उदास", "दुखी", "दुःखी", "निराश", "मायूस", "बोर हो", "ऊब", "sad", "upset", "disappointed", "gloomy", "unhappy", "bored"}, Pose.SAD, Film.G_LOOK_AWAY, 2.2f},
        {new String[]{"शरमा", "शर्मा", "झेंप", "लजा", "blushes", "shy"}, Pose.HAPPY, Film.G_LOOK_AWAY, 1.8f},
        {new String[]{"हैरान", "चकित", "अचंभ", "आश्चर्य", "दंग", "हक्का-बक्का", "चौंक", "surprised", "amazed", "astonished", "stunned", "shocked", "startled"}, Pose.SURPRISED, Film.G_STEP_BACK, 1.6f},
        {new String[]{"उलझन", "असमंजस", "confused", "puzzled"}, Pose.CURIOUS, Film.G_HEAD_SCRATCH, 2.0f},
        {new String[]{"सोच", "विचार", "ponders", "thinks", "wonders", "thoughtful"}, Pose.CURIOUS, Film.G_HEAD_SCRATCH, 2.0f},
        {new String[]{"गर्व", "proud"}, Pose.PROUD, Film.G_PROUD, 1.8f},
        {new String[]{"राहत", "चैन की", "सुकून", "relieved", "relief"}, Pose.RELIEVED, -1, 1.6f},
        {new String[]{"दर्द", "चोट", "कराह", "hurts", "in pain", "winces"}, Pose.PAIN, Film.G_CLUTCH, 1.8f},
        {new String[]{"थक", "हाँफ", "हांफ", "tired", "exhausted", "panting"}, Pose.SAD, Film.G_WEIGHT_SHIFT, 1.8f},
        {new String[]{"शक कर", "शक से", "शक हो", "संदेह", "जलन हो", "जलन से", "ईर्ष्या", "suspicious", "jealous"}, Pose.SUSPICIOUS, -1, 1.6f},
        {new String[]{"खुश", "ख़ुश", "प्रसन्न", "excited", "happy", "delighted", "overjoyed", "joyful"}, Pose.HAPPY, Film.G_BOUNCE, 1.6f},
    };
    int everydayStaged;

    /**
     * v41: the everyday words of a sentence nothing else staged; returns the time they take (0: none fitted). v43: two
     * actions joined in one sentence ("माँ फिसल जाती हैं और राहुल उनका हाथ पकड़कर उन्हें उठाता है") are each staged in
     * turn, each clause with its own subject (one naming nobody goes on with the one before's).
     */
    private float everyday(String s, float t) {
        if (seg == null || negated(s)) return 0;
        List<String> cl = clauses(s);
        if (cl.size() < 2) return everydayOne(s, t, null, null);
        float used = 0;
        Film.Actor prev = null;
        for (String c : cl) {
            List<Story.CharacterDef> cm = mentionsWithGroups(c, null);
            Film.Actor own = cm.isEmpty() ? null : actor(cm.get(subjectIndex(c, cm)));
            float d = everydayOne(c, t + used, own == null ? prev : null, own != null && own != prev ? prev : null);
            used += d;
            if (own != null) prev = own;
        }
        return used;
    }

    /**
     * v43: the clauses of a sentence that joins two actions with "और" / "फिर" / "and" / "then" — split only where the
     * part before ends on a verb ("माँ फिसल जाती हैं और …"), never between two names ("राहुल और प्रिया नाचते हैं").
     */
    static List<String> clauses(String s) {
        List<String> out = new ArrayList<String>();
        String rest = s.trim();
        String[] joins = {" और फिर ", ", और ", " और ", ", फिर ", " फिर ", " and then ", ", then ", " then ", ", and ", " and ", "; "};
        boolean split = true;
        while (split) {
            split = false;
            int best = -1; String bj = null;
            for (String j : joins) {
                int i = rest.indexOf(j);
                while (i > 0) {
                    if (verbEnd(rest.substring(0, i)) && rest.length() - i - j.length() > 3) { if (best < 0 || i < best) { best = i; bj = j; } break; }
                    i = rest.indexOf(j, i + 1);
                }
            }
            if (best > 0) {
                out.add(rest.substring(0, best).trim());
                rest = rest.substring(best + bj.length()).trim();
                split = true;
            }
        }
        out.add(rest);
        return out;
    }

    /** v43: the text ends on a verb (Hindi: ...ता / ती / ते / है / हैं / था / गया …; English: a word in -s or -ed after a subject). */
    static boolean verbEnd(String left) {
        String l = left.trim().replaceAll("[,।.!?;]+$", "").trim();
        if (l.isEmpty()) return false;
        String[] w = l.split("\\s+");
        String last = w[w.length - 1];
        if (Txt.isDevanagari(last.charAt(0)))
            return last.endsWith("ता") || last.endsWith("ती") || last.endsWith("ते") || last.endsWith("है") || last.endsWith("हैं")
                    || last.endsWith("था") || last.endsWith("थी") || last.endsWith("थे") || last.endsWith("गया") || last.endsWith("गई")
                    || last.endsWith("गए") || last.endsWith("गयी") || last.endsWith("लगा") || last.endsWith("लगी") || last.endsWith("लगे");
        String lw = last.toLowerCase(java.util.Locale.ROOT);
        return w.length >= 2 && lw.length() > 3 && (lw.endsWith("s") || lw.endsWith("ed")) && !lw.endsWith("ss");
    }

    /**
     * v43: which of the characters named in a sentence is its subject: the one in the main clause after a participle
     * ("माँ की आवाज़ सुनकर राहुल की आँख खुल जाती है" — राहुल, not माँ; "Hearing Mother call, Rahul …"), and never one
     * marked as the object ("माँ को", "माँ से", "to Mother", "from Mother") when another is not. Else the first named.
     */
    static int subjectIndex(String s, List<Story.CharacterDef> ms) {
        if (ms == null || ms.size() < 2) return 0;
        String n = Txt.norm(s);
        int[] pos = new int[ms.size()], end = new int[ms.size()];
        for (int i = 0; i < ms.size(); i++) {
            pos[i] = -1;
            Story.CharacterDef c = ms.get(i);
            List<String> names = new ArrayList<String>(c.aliases);
            if (c.displayName != null) names.add(c.displayName);
            for (String nm : names) {
                String x = Txt.norm(nm);
                if (x.length() < 2) continue;
                int k = n.indexOf(x);
                if (k >= 0 && (pos[i] < 0 || k < pos[i])) { pos[i] = k; end[i] = k + x.length(); }
            }
        }
        int part = -1;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("[\\u0900-\\u0963]कर[\\s,]").matcher(n);
        while (m.find()) part = m.end();
        if (n.matches("^[a-z]+ing\\b.*") && n.indexOf(", ") > 0) part = Math.max(part, n.indexOf(", ") + 2);
        // (v44: only when nobody plain stands before the participle: "प्रिया छाता लेकर कार से उतरती है और वनुशा का हाथ पकड़ती
        // है" is प्रिया's — the one after "लेकर" is only whose hand; "माँ की आवाज़ सुनकर राहुल…" is राहुल's)
        boolean plainBefore = false;
        for (int i = 0; i < ms.size() && part > 0; i++) {
            if (pos[i] < 0 || pos[i] >= part || objectMarked(n, pos[i], end[i])) continue;
            String after = n.substring(Math.min(n.length(), end[i])).trim();
            if (after.startsWith("का ") || after.startsWith("की ") || after.startsWith("के ") || after.startsWith("'s ")) continue;
            plainBefore = true;
        }
        if (part > 0 && !plainBefore) {
            int best = -1;
            for (int i = 0; i < ms.size(); i++)
                if (pos[i] >= part && !objectMarked(n, pos[i], end[i]) && (best < 0 || pos[i] < pos[best])) best = i;
            if (best >= 0) return best;
        }
        if (pos[0] >= 0 && objectMarked(n, pos[0], end[0])) {
            int best = -1;
            for (int i = 1; i < ms.size(); i++)
                if (pos[i] >= 0 && !objectMarked(n, pos[i], end[i]) && (best < 0 || pos[i] < pos[best])) best = i;
            if (best >= 0) return best;
        }
        return 0;
    }

    /** v43: the name at p..e is the object of its sentence ("माँ को", "माँ से", "to Mother", "from Mother"). */
    static boolean objectMarked(String n, int p, int e) {
        String after = n.substring(Math.min(n.length(), e)).trim();
        if (after.startsWith("को ") || after.startsWith("को,") || after.equals("को") || after.startsWith("से ") || after.startsWith("पर ")) return true;
        // the one gone up to, or towards ("माँ के पास जाता है", "की ओर", "की तरफ़")
        if (after.startsWith("के पास") || after.startsWith("की ओर") || after.startsWith("की तरफ") || after.startsWith("के करीब") || after.startsWith("के निकट")
                || after.startsWith("के पीछे") || after.startsWith("के सामने") || after.startsWith("के लिए") || after.startsWith("के साथ")
                || after.startsWith("के बगल") || after.startsWith("के बाजू") || after.startsWith("के बाज़ू")) return true;
        String before = n.substring(0, Math.max(0, p)).trim();
        return before.endsWith(" to") || before.endsWith(" from") || before.endsWith(" at") || before.endsWith(" with") || before.endsWith(" for") || before.endsWith(" beside")
                || before.endsWith(" next to") || before.endsWith(" behind")
                || before.endsWith(" of") || before.endsWith(" on") || before.equals("to") || before.equals("from");
    }

    /** v41/v43: one clause's everyday words; fallback: its subject when it names nobody; prevTarget: the one before's, for "उन्हें", "her". */
    private float everydayOne(String s, float t, Film.Actor fallback, Film.Actor prevTarget) {
        if (seg == null || negated(s)) return 0;
        List<Story.CharacterDef> ms = mentionsWithGroups(s, null);
        String head = s.length() > 8 ? s.substring(0, 8) : s;
        boolean pronoun = Txt.has(head, "वह ", "वो ", "वे ", "उसने", "he ", "she ", "they ") || head.startsWith("He ") || head.startsWith("She ") || head.startsWith("They ");
        boolean groupWord = Txt.has(s, "सब ", "सभी", "चारों", "तीनों", "दोनों", "everyone", "all of them", "both of them", "together");
        List<Film.Actor> who = new ArrayList<Film.Actor>();
        Film.Actor target = null;
        if (!ms.isEmpty()) {
            int si = subjectIndex(s, ms);
            Film.Actor first = actor(ms.get(si));
            if (first != null) who.add(first);
            for (int i = 0; i < ms.size(); i++) {
                if (i == si) continue;
                Film.Actor a = actor(ms.get(i));
                if (a == null || who.contains(a)) continue;
                if (groupWord) who.add(a); else if (target == null) target = a;
            }
        } else if (fallback != null) who.add(fallback);
        else if (pronoun && lastSubject != null && actor(lastSubject) != null) who.add(actor(lastSubject));
        // (the one the clause before spoke of, as "उन्हें", "उनका", "her", "him")
        if (target == null && prevTarget != null && !who.contains(prevTarget)
                && Txt.has(" " + s + " ", "उन्हें", "उनका", "उनकी", "उनके", "उसे ", "उसका", "उसकी", "उसके", " her ", " him ", " them ")) target = prevTarget;
        // v43: "कोई दरवाज़े पर दस्तक देता है" — a knock with nobody named: heard at the room's door, and everyone turns to it
        if (who.isEmpty() && Txt.has(s, "खटखटा", "दस्तक", "knocks", "knocking", "knock on", "knocked")) {
            float side = doorSide(640);
            Film.Sfx ks = new Film.Sfx(Film.SFX_KNOCK, t + 0.2f, 0.8f, 0.5f);
            ks.pan = side * 0.6f;
            film.sfx.add(ks);
            if (indoors(seg.set)) roomDoorFrame(side);
            for (Film.Actor o : seg.actors) if (o.stateAt(t).visible && o.look != null && o.look.isHumanoid()) { Film.Key lk = keyAt(o, t + 0.5f); lk.facing = side; }
            knocks++;
            return 1.4f;
        }
        if (who.isEmpty()) return 0;
        // the names are taken out first: a girl called पूजा or आरती is not a prayer
        String w = " " + s + " ";
        for (Story.CharacterDef c : story.characters) {
            if (c.fullName != null && c.fullName.length() > 0) w = w.replace(c.fullName, " ");
            if (c.displayName != null && c.displayName.length() > 0) w = w.replace(c.displayName, " ");
        }
        Object[] row = null;
        for (Object[] r : EVERYDAY) {
            // (v43: a word only where a word begins: "लगाता" sings nothing, "कराहता" is no relief)
            if (!Txt.hasStart(w, (String[]) r[0])) continue;
            int kind = (Integer) r[4];
            // a fall of something else (a leaf, the rain, a thing dropped), not of the one named
            if (kind == EV_FALL && Txt.has(w, "हाथ से", "छूट", "पत्ते", "पत्ता", "बूँद", "बूंद", "बारिश", "बर्फ", "ओले", "गिरा दे", "गिरा दि", "गिरा दी",
                    "drops the", "leaves fall", "rain falls", "snow falls", "night falls", "silence falls", "falls asleep")) continue;
            // "देता है" gives only to someone ("बिल्ली को दूध देता है"); "रख देता है", "दिखाई देता है" are other verbs
            if (kind == EV_GIVE && (Txt.has(w, "दिखाई", "सुनाई", "ध्यान", "जवाब", "धोखा", "रख दे", "छोड़ दे", "फेंक दे", "हटा दे", "खोल दे", "बंद कर दे", "बता दे", "दिखा दे")
                    || (!Txt.has(w, " को ", "gives", "hands", "offers", "तोहफ़ा", "तोहफा", "उपहार", "गिफ्ट", "थमा", "सौंप") && target == null))) continue;
            // an umbrella put away is not opened
            if (kind == EV_UMBRELLA && Txt.has(w, "बंद", "समेट", "close", "fold", "puts away", "put away")) continue;
            row = r;
            break;
        }
        Object[] mood = null;
        for (Object[] r : FEELINGS) if (Txt.hasStart(w, (String[]) r[0])) { mood = r; break; }
        if (row == null && mood == null) return 0;
        float dur = row != null ? (Float) row[3] : (Float) mood[3];
        // (a feeling the sentence names wins over the face an action row gives: "डरकर चीख़ पड़ता है" is a scared scream)
        int emo = mood != null ? (Integer) mood[1] : row != null && (Integer) row[1] >= 0 ? (Integer) row[1] : -1;
        int gesture = row != null ? (Integer) row[2] : -1;
        if (gesture < 0 && mood != null && (row == null || (Integer) row[4] == EV_NONE)) gesture = (Integer) mood[2];
        int ev = row != null ? (Integer) row[4] : EV_NONE;
        float used = 0;
        for (Film.Actor a : who) {
            Film.Key now = a.stateAt(t);
            if (!now.visible || a.look == null) continue;
            boolean human = a.look.isHumanoid();
            float t0 = t + 0.15f;
            if (ev == EV_NEAR || ev == EV_GIVE || (ev == EV_HIDE && target != null)) {
                if (target != null && target.stateAt(t).visible && human) {
                    moveNear(a, target, t, 0.7f);
                    t0 = t + 0.9f;
                }
            }
            float d = dur;
            // the face first (what follows copies it), back to the face before once the action is over
            int was = a.stateAt(t0).emotion;
            if (emo >= 0 && ev != EV_FALL) keyAt(a, t0).emotion = emo;
            switch (ev) {
                case EV_FALL: {
                    if (!human || now.body != Pose.STAND || now.anchor != Film.A_GROUND) break;
                    // v44: a stumble that does not end on the ground ("ठोकर खाकर लड़खड़ा जाता है", "stumbles"): arms out, caught
                    if (!Txt.has(w, "गिर", "फिसल", "falls", "fell", "slips", "slipped", "tumbles")) {
                        a.acts.add(new Film.Act(t0, t0 + 0.8f, Film.G_FLAIL));
                        keyAt(a, t0).emotion = Pose.PAIN;
                        keyAt(a, t0 + 1.6f).emotion = Pose.NEUTRAL;
                        gesture = -1; d = 1.6f;
                        break;
                    }
                    // v44: down onto the ground (forwards: a trip; backwards: a slip), a moment there, up again in pain
                    boolean back = Txt.has(w, "फिसल", "slips", "slipped", "पीछे");
                    float f = now.facing >= 0 ? 1 : -1;
                    Film.Act fa = new Film.Act(t0, t0 + 2.4f, Film.G_FALL); fa.item = (int) (back ? -f : f); a.acts.add(fa);
                    Film.Key down = keyAt(a, t0 + 0.4f); down.body = Pose.LIE; down.seat = -1; down.emotion = Pose.PAIN;
                    film.sfx.add(sfxAt(Film.SFX_THUD, t0 + 0.4f, 0.4f, 0.4f, a));
                    Film.Key up = keyAt(a, t0 + 1.6f); up.body = Pose.STAND; up.seat = -1; up.emotion = Pose.PAIN;
                    Film.Key ok = keyAt(a, t0 + 2.6f); ok.emotion = Pose.NEUTRAL;
                    falls++;
                    gesture = -1; d = 2.7f;
                    break;
                }
                case EV_HIDE: if (human && now.body == Pose.STAND) { Film.Key k = keyAt(a, t0 + 0.2f); k.body = Pose.CROUCH; Film.Key k2 = keyAt(a, t0 + dur); k2.body = Pose.STAND; } break;
                case EV_KNEEL: if (human && now.body == Pose.STAND) { Film.Key k = keyAt(a, t0); k.body = Pose.KNEEL; } break;
                case EV_FLOOR: if (human && now.body != Pose.SIT) { Film.Key k = keyAt(a, t0); k.body = Pose.SIT; k.seat = Film.SEAT_FLOOR; } break;
                case EV_STAND: if (now.body == Pose.SIT || now.body == Pose.LIE || now.body == Pose.KNEEL) { Film.Key k = keyAt(a, t0); k.body = Pose.STAND; k.seat = -1; k.eyesShut = false; } break;
                case EV_EYES: { Film.Key k = keyAt(a, t0 + 0.2f); k.eyesShut = true; Film.Key k2 = keyAt(a, t0 + dur); k2.eyesShut = false; break; }
                case EV_WAKE: if (now.eyesShut) { Film.Key k = keyAt(a, t0); k.eyesShut = false; } break;
                case EV_UMBRELLA: if (human && a.umbrellaFrom < 0 && (a.look == null || !a.look.umbrella)) a.umbrellaFrom = t0 + 0.4f; break;
                case EV_WALK: case EV_RUN: {
                    if (!human || now.anchor != Film.A_GROUND) break;
                    float x = xAt(a, t);
                    float far = ev == EV_RUN ? 420 : 280;
                    float dest = x < 640 ? Math.min(1100, x + far) : Math.max(180, x - far);
                    // to a free spot (never onto someone standing there)
                    List<Film.Actor> me = new ArrayList<Film.Actor>();
                    me.add(a);
                    dest = freeNear(dest, t0 + dur, me, 110);
                    if (Math.abs(dest - x) < 60) break;
                    if (now.body != Pose.STAND) { Film.Key up = keyAt(a, t0); up.body = Pose.STAND; up.seat = -1; up.eyesShut = false; t0 += 0.5f; }
                    Film.Key k = keyAt(a, t0); k.x = dest; k.moveDur = dur; k.facing = dest > x ? 1 : -1; k.run = ev == EV_RUN;
                    if (ev == EV_RUN) { Film.Key k2 = keyAt(a, t0 + dur + 0.05f); k2.run = false; }
                    break;
                }
                case EV_LOOK_ROUND: {
                    float f = now.facing;
                    a.acts.add(new Film.Act(t0, t0 + 0.5f, Film.G_TURN));
                    Film.Key k = keyAt(a, t0 + 0.5f); k.facing = -f;
                    a.acts.add(new Film.Act(t0 + 1.4f, t0 + 1.9f, Film.G_TURN));
                    Film.Key k2 = keyAt(a, t0 + 1.9f); k2.facing = f;
                    break;
                }
                case EV_READ: case EV_SWEEP: case EV_WASH: case EV_COOK: {
                    if (!human) break;
                    List<Film.Actor> one = new ArrayList<Film.Actor>();
                    one.add(a);
                    int kind = ev == EV_READ ? Film.T_READ : ev == EV_SWEEP ? Film.T_SWEEP : ev == EV_WASH ? Film.T_WASH : Film.T_COOK;
                    d = Math.max(d, oneTask(s, " " + s + " ", kind, t0 - 0.2f, one));
                    break;
                }
                case EV_THUD: film.sfx.add(sfxAt(Film.SFX_THUD, t0 + 0.45f, 0.3f, 0.35f, a)); break;
                case EV_SWEAT: if (human) { Film.Key k = keyAt(a, t0); k.sweat = true; spread(a, k); } break;
                case EV_KICK: {
                    // v43: the foot to the ball — it flies off the way they face (a thud at the kick)
                    film.sfx.add(sfxAt(Film.SFX_THUD, t0 + 0.45f, 0.3f, 0.45f, a));
                    if (Txt.has(w, "गेंद", "फ़ुटबॉल", "फुटबॉल", "बॉल", "ball", "football")) {
                        float face = now.facing >= 0 ? 1 : -1, x = xAt(a, t0);
                        Film.Fx f = fx(Film.FX_THROW, t0 + 0.45f, t0 + 3.4f, Math.max(60, Math.min(1220, x + face * 380)), ground, a, null);
                        f.kind = 1;
                        film.sfx.add(sfxAt(Film.SFX_WHOOSH, t0 + 0.5f, 0.5f, 0.3f, a));
                    }
                    break;
                }
                case EV_DOOR: {
                    // v43: indoors the door is seen: they go to it (the room's door already seen, else the nearer edge of
                    // the room), reach for it, and it swings open — or shut ("बंद", "closes", "shuts", "slams")
                    if (!human || seg == null || !indoors(seg.set)) { film.sfx.add(sfxAt(Film.SFX_DOOR, t0 + 0.5f, 0.8f, 0.4f, a)); break; }
                    float side = doorSide(xAt(a, t0)), dx = side < 0 ? 150 : 1130, ax = xAt(a, t0);
                    float wd = Math.abs(ax - dx) > 40 ? Math.min(2.4f, Math.max(0.6f, Math.abs(ax - dx) / 180f)) : 0;
                    Film.Key wk = keyAt(a, t0 + 0.05f);
                    if (wd > 0) { wk.x = dx; wk.moveDur = wd; wk.run = false; }
                    wk.facing = side;
                    float at = t0 + 0.1f + wd;
                    boolean shut = Txt.has(w, "बंद", "close", "shut", "slam");
                    roomDoorFrame(side);
                    if (shut) closeDoor(side, at + 0.35f);
                    else swingDoor(side, at + 0.3f, Math.max(at + 6f, seg.t1 + 1));
                    Film.Sfx ds = new Film.Sfx(Film.SFX_DOOR, at + 0.2f, 1.0f, Txt.has(w, "slam", "ज़ोर से", "जोर से") ? 0.6f : 0.42f);
                    ds.pan = side * 0.6f;
                    film.sfx.add(ds);
                    a.acts.add(new Film.Act(at, at + 1.0f, Film.G_REACH));
                    gesture = -1;
                    d = Math.max(d, wd + 1.5f);
                    doorsSeen++;
                    break;
                }
                case EV_KNOCK: {
                    // v43: a knock at the door — three raps on the wood (heard from the door's side); the one knocking
                    // stands at it
                    float side = doorSide(xAt(a, t0));
                    Film.Sfx ks = new Film.Sfx(Film.SFX_KNOCK, t0 + 0.3f, 0.8f, 0.5f);
                    ks.pan = side * 0.6f;
                    film.sfx.add(ks);
                    if (seg != null && indoors(seg.set)) roomDoorFrame(side);
                    knocks++;
                    break;
                }
                default:
            }
            if (gesture >= 0 && (human || gesture == Film.G_JUMP || gesture == Film.G_TREMBLE || gesture == Film.G_SHIVER)) {
                Film.Act act = new Film.Act(t0, t0 + Math.min(dur, 3f), gesture);
                if (ev == EV_NEAR || ev == EV_GIVE) act.target = target;
                a.acts.add(act);
            }
            // v43: what was carried is given (or put down): the hands are empty after it
            if (ev == EV_GIVE && gesture == Film.G_GIVE || Txt.has(w, "रख देता", "रख देती", "रख दिया", "नीचे रख", "puts down", "puts it down", "sets down"))
                handsFree(a, t0 + Math.min(dur, 3f));
            if (emo >= 0 && ev != EV_FALL && was != emo) keyAt(a, t0 + Math.max(1.2f, d + 0.5f)).emotion = was;
            if (mood != null && Txt.has(w, "शरमा", "शर्मा", "झेंप", "लजा", "blushes")) { Film.Key k = keyAt(a, t0); k.redFace = true; Film.Key k2 = keyAt(a, t0 + d); k2.redFace = false; }
            used = Math.max(used, t0 - t + d);
            everydayStaged++;
        }
        return used > 0 ? used + 0.3f : 0;
    }

    /** Stages one sentence of a stage direction; returns its duration. */
    private float sentence(String s, float t, boolean establishing) {
        rode = false;
        travelAfter = 0;
        List<Story.CharacterDef> ms = mentionsWithGroups(s, null);
        boolean pronoun = Txt.has(s.length() > 6 ? s.substring(0, Math.min(s.length(), 8)) : s, "वह ", "वो ", "उसने", "उसकी", "उसके", "उसे ", "तीनों", "दोनों");
        Story.CharacterDef subjC = (!ms.isEmpty() && !(pronoun && lastSubject != null && !Txt.has(s, "तीनों", "दोनों"))) ? ms.get(subjectIndex(s, ms)) : lastSubject;
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
        // v36: "सब / सभी / everyone" with no one named is everyone on the stage ("सब अलाव के पास बैठ जाते हैं")
        // (v44: the ones carried over from the sentence before are not named here: "… ओले पड़ने लगते हैं और सब भागकर अंदर जाते हैं")
        boolean namedHere = false;
        for (Story.CharacterDef c : ms) if (Txt.has(s, c.displayName) || (c.fullName != null && !c.fullName.isEmpty() && Txt.has(s, c.fullName))) namedHere = true;
        if (!namedHere && Txt.has(" " + s + " ", " सब ", " सभी ", "everyone", "everybody", " all of them", " they all ") && seg != null) {
            group.clear();
            if (subj != null && subj.stateAt(t).visible) group.add(subj);
            for (Film.Actor a : seg.actors) if (a != subj && a.stateAt(t).visible && a.look != null) group.add(a);
        }
        // v39: "तीनों एक दूसरे को देखकर मुस्कुराते हैं", "चारों चाय का कप उठाते हैं और चाय पीने लगते हैं": a group word is
        // that many of those on the stage — the ones named with it, then the group last named together, then the
        // others — and everything the sentence says they do, each of them does
        groupSubject = false;
        int gn = groupWordCount(s);
        if (gn == 0 && !ms.isEmpty()) gn = -1;          // "सब" with someone named: that one does it, to everyone
        if (gn >= 0 && seg != null) {
            List<Film.Actor> on = new ArrayList<Film.Actor>();
            for (Film.Actor a : seg.actors) if (a.look != null && (a.stateAt(t).visible || a.keys.get(0).t >= t - 0.01f) && !a.c.voiceOnly) on.add(a);
            List<Film.Actor> pick = new ArrayList<Film.Actor>();
            for (Story.CharacterDef c : ms) { Film.Actor a = actor(c); if (a != null && !pick.contains(a)) pick.add(a); }
            int want = gn == 0 ? on.size() : gn;
            // "तीनों और बोल्ट …", "राजू और तीनों …": the ones named beside the group word are more than the three
            if (gn > 0 && !ms.isEmpty() && Txt.norm(s).matches("(?s).*(" + groupWordRe() + ")\\s+(और|तथा|,)\\s.*|.*\\s(और|तथा)\\s+(" + groupWordRe() + ")(\\s.*|$)")) want = gn + ms.size();
            if (gn > 0 && on.size() == gn) { for (Film.Actor a : on) if (!pick.contains(a)) pick.add(a); }
            for (Story.CharacterDef c : lastGroup) { Film.Actor a = actor(c); if (pick.size() < want && a != null && on.contains(a) && !pick.contains(a)) pick.add(a); }
            // then: those on the stage now, those of the last group named together in an earlier scene, those on the
            // same side as the ones named (the three friends, not the villain who comes in later)
            final float tt = t;
            final boolean heroSide = pick.isEmpty() || pick.get(0).look == null || pick.get(0).look.hero;
            List<Film.Actor> rest = new ArrayList<Film.Actor>(on);
            java.util.Collections.sort(rest, new java.util.Comparator<Film.Actor>() {
                int score(Film.Actor a) { return (a.stateAt(tt).visible ? 4 : 0) + (storyGroup.contains(a.c) ? 2 : 0) + (a.look.hero == heroSide ? 1 : 0); }
                public int compare(Film.Actor a, Film.Actor b) { return score(b) - score(a); }
            });
            // ("सब" / "everyone" is everyone on the stage, whichever side they are on)
            for (Film.Actor a : rest) if (pick.size() < want && !pick.contains(a) && (gn == 0 || a.look.hero == heroSide || pick.size() < 2)) pick.add(a);
            if (pick.size() >= 2) {
                group = pick;
                if (subj == null || !pick.contains(subj)) subj = pick.get(0);
                others.clear();
                for (Film.Actor a : pick) if (a != subj) others.add(a);
                target = null;
                groupSubject = true;
                groupSentences++;
                if (DEBUG_GROUPS) { StringBuilder b = new StringBuilder(); for (Film.Actor a : pick) b.append(a.c.shown()).append(','); System.out.println("GROUP[" + gn + "] " + b + " ← " + s); }
            }
        } else if (group.size() > 1 && joinedPlural(s)) groupSubject = true;
        float d = 1.4f;
        boolean focusSet = false;
        // thought before action (handbook ch. 6): a sound or a sudden sight is perceived first — a pause, the head
        // turns toward it, the body holds still — and only then comes the action of the sentence
        // (v35: a deaf character does not perceive a sound — the deaf one notices it in the others' faces, loudReaction)
        if (subj != null && subj.stateAt(t).visible && Handbook.stimulus(s) && !(subj.look != null && subj.look.cannotHear() && DirectorsManual.loud(s))) {
            subj.acts.add(new Film.Act(t, t + 0.6f, Film.G_LISTEN));
            t += 0.55f;
            d += 0.55f;
            thoughtBeats++;
        }

        // ---- weather and nature that the action calls for (rain starts, thunder, a stone into the pond…)
        d = Math.max(d, natureFrom(s, t, subj));
        // ---- sounds named in the text (onomatopoeia), and the user's own effects for what happens
        userEffect(s, t);
        if (Txt.has(s, "कल-कल", "झरन")) film.sfx.add(new Film.Sfx(Film.SFX_STREAM, t, 4, 0.35f));
        if (Txt.has(s, "चह-चह", "चिड़िय", "चहचहा", "कोयल", "कूक", "chirp", "birdsong", "birds sing", "birds are singing", "cuckoo", "tweet")) film.sfx.add(new Film.Sfx(Film.SFX_BIRDS, t, 4, 0.4f));
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
            storyGroup.clear();
            storyGroup.addAll(lastGroup);
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
        if (Txt.has(s, "ताली", " claps", "clapping", "applaud") && subj != null) { subj.acts.add(new Film.Act(t, t + 1.6f, Film.G_CLAP)); film.sfx.add(new Film.Sfx(Film.SFX_CLAP, t, 1.5f, 0.6f)); d = Math.max(d, 1.8f); }
        if (Txt.has(s, "हँस", "खिलखिला") && subj != null && !Txt.has(s, "हँसी गूँज")) {
            Film.Key k = subj.at(t); k.emotion = Pose.LAUGH;
            subj.acts.add(new Film.Act(t, t + 1.8f, Film.G_LAUGH));
            Film.Key k2 = subj.at(t + 1.9f); k2.emotion = Pose.HAPPY;
            d = Math.max(d, 2f);
        }
        if (groupSubject && group.size() > 1) {
            // v39: the whole group feels it and shows it — a smile, a laugh — and "एक दूसरे को देखकर" they look at one another
            boolean laugh = Txt.has(s, "हँस", "हंस", "खिलखिला", "laugh") && !Txt.has(s, "हँसी गूँज");
            boolean smile = Txt.has(s, "मुस्कुरा", "मुस्करा", "मुस्कान", "smile", "grin");
            for (Film.Actor a : group) {
                if (a == subj && laugh) continue;          // the subject's laugh is staged above
                if (laugh) { Film.Key k = a.at(t + 0.1f); k.emotion = Pose.LAUGH; a.acts.add(new Film.Act(t + 0.1f, t + 1.9f, Film.G_LAUGH)); Film.Key k2 = a.at(t + 2.0f); k2.emotion = Pose.HAPPY; d = Math.max(d, 2f); }
                else if (smile) { Film.Key k = a.at(t + 0.1f); k.emotion = Pose.HAPPY; d = Math.max(d, 1.8f); }
            }
            if (Txt.has(s, "एक दूसरे", "एक-दूसरे", "आपस में", "each other", "one another")) {
                for (int i = 0; i < group.size(); i++) {
                    Film.Actor a = group.get(i), b = group.get((i + 1) % group.size());
                    if (a.look != null && !a.look.cannotSee()) film.watches.add(new Film.Watch(a, b, t, t + 2.6f));
                }
                d = Math.max(d, 2.4f);
            }
            if (!focusSet) { float[] xs = new float[group.size()]; for (int i = 0; i < xs.length; i++) xs[i] = xAt(group.get(i), t); frameCut(t, xs); focusSet = true; }
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
        playSentence = playKind(s, subj);
        d = Math.max(d, postureFrom(s, t, subj, group));
        if (mealCU != null) { camOn(mealCU, mealCUt, 2.4f); focusSet = true; mealCU = null; mealCloseUps++; }
        // v36: lighting diyas, candles or a fire is shown — a frame low and wide enough to see the flames catch
        if (!focusSet && Txt.has(s, "जला", "जलाती", "जलाता", "जलाते", "light", "lit ", "kindle") && (Txt.has(s, DIYA) || Txt.has(s, CANDLE) || Txt.has(s, TORCH) || Txt.has(s, FIRE))) {
            boolean high = Txt.has(s, TORCH);
            cam(t + 0.1f, 640, ground - (high ? 300 : 170), 1.0f, 0.5f);
            focusSet = true;
            lightingShots++;
        }
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
        // (v41: "घर की तरफ चल पड़ते हैं" is going home — the walk off of travelFrom, after a hug if there is one — not
        // a step toward someone; one sitting gets up first)
        if (Txt.has(s, "तरफ बढ़", "ओर बढ़", "की तरफ चल") && subj != null && !Txt.has(s, "घर की तरफ", "घर की ओर")) {
            Film.Actor dest = target;
            if (dest == null) for (Film.Actor a : seg.actors) if (!a.look.hero && a.stateAt(t).visible) dest = a;
            float x = dest != null ? xAt(dest, t) - Math.signum(xAt(dest, t) - xAt(subj, t)) * 150 : Math.min(1100, xAt(subj, t) + 260);
            Film.Key b0 = subj.stateAt(t);
            float tw = t;
            if ((b0.body == Pose.SIT || b0.body == Pose.LIE) && subj.look.isHumanoid()) { Film.Key up = subj.at(t); up.body = Pose.STAND; up.seat = -1; tw = t + 0.6f; stoodToWalk++; }
            Film.Key k = subj.at(tw); k.x = x; k.moveDur = 2.2f; k.facing = x > xAt(subj, t) ? 1 : -1;
            d = Math.max(d, 2.4f + (tw - t));
        }
        if (Txt.has(s, "टिमटिमा") && !Txt.has(s, STARS) && !Txt.has(s, "दीय", "दीप", "दिये", "दीये", "जुगनू", "बत्ती", "लाइट")) fx(Film.FX_LADDOO_GLOW, t, t + 3, 0, 0, null, null);
        if (Txt.has(s, "छुआ", "छूते")) {
            Film.Actor v = subj;
            if (v != null) v.acts.add(new Film.Act(t, t + 0.8f, Film.G_REACH));
        }
        // (v44: smoke from a crashed car's bonnet, or a fire, is no magic puff: the wreck draws its own)
        if (Txt.has(s, "धुएँ", "धुआँ", "धुआं", "smoke") && Txt.has(s, "फट", "गुबार", "फूँक", "छोड़", "निकल", "उठ", "burst", "puff")
                && !Txt.has(s, CRASH_WORDS) && !Txt.has(s, "बोनट", "इंजन", "bonnet", "hood", "engine", "गाड़ी", "कार", "car ", "आग", "fire", "चूल्ह", "chimney", "चिमनी")) {
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
        if (Txt.has(s, "नाच", "नृत्य", "ठुमके", " dances", "dancing") && subj != null) subj.acts.add(new Film.Act(t, t + 3.5f, Film.G_DANCE));
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
        if (Txt.hasStart(s, "टन ", "टनटन", "टन-टन") || Txt.has(s, "घंटे की")) {
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
        // v41: "… वनुशा का हाथ पकड़ती है" (a direction, not only a line's manner): hand in hand for a while
        if (Txt.has(s, "हाथ थाम", "हाथ पकड़", "हाथ पकड", "holds her hand", "holds his hand", "takes her hand", "takes his hand", "holds hands", "hand in hand")
                && !Txt.has(s, "हाथों से", "हाथ से", "हाथ में") && subj != null && target != null) {
            moveNear(subj, target, t, 0.6f);
            Film.Act hh = new Film.Act(t + 0.6f, t + 6, Film.G_HOLD_HAND);
            hh.target = target;
            subj.acts.add(hh);
            handsHeld++;
            d = Math.max(d, 1.6f);
        }
        // v41: a jump into a puddle — the jump, the splash at the feet and its sound
        if (subj != null && Txt.has(s, "गड्ढे", "गड्ढा", "पोखर", "puddle") && Txt.has(s, "कूद", "jump", "छपाक", "splash", "stamps")) {
            float tj = t + 0.2f, px = xAt(subj, t);
            // the puddle she lands in: one of the place's own if one is near (she skips over to it first), else one
            // where she stands
            Film.Fx pd = null;
            for (Film.Fx f : seg.fx) if (f.type == Film.FX_PUDDLE && f.t2 < 0 && Math.abs(f.x - px) < 300 && (pd == null || Math.abs(f.x - px) < Math.abs(pd.x - px))) pd = f;
            if (pd == null) { pd = fx(Film.FX_PUDDLE, Math.max(seg.t0, t - 2f), 1e6f, px, 6, subj, null); pd.color = 190; }
            else if (Math.abs(pd.x - px) > 20) {
                float wd = Math.max(0.35f, Math.min(1.1f, Math.abs(pd.x - px) / 280f));
                Film.Key k = keyAt(subj, t + 0.1f); k.x = pd.x; k.moveDur = wd; k.run = true; k.facing = pd.x > px ? 1 : -1; spread(subj, k);
                tj = t + 0.15f + wd;
                px = pd.x;
            }
            subj.acts.add(new Film.Act(tj, tj + 0.7f, Film.G_JUMP));
            pd.t2 = tj + 0.45f;
            pd.y = 6;                                   // where her feet come down (not a step in front of them)
            fx(Film.FX_SPLASH, tj + 0.45f, tj + 1.6f, px, ground - 4, subj, null);
            film.sfx.add(sfxAt(Film.SFX_SPLASH, tj + 0.45f, 0.8f, 0.45f, subj));
            // the whole figure, feet and puddle in the frame: the jump and the splash are the shot
            if (!focusSet && !establishing) {
                // the tallest one standing near her keeps a whole head in the frame, the puddle stays above its bottom
                float maxH = heightOf(subj);
                for (Film.Actor o : seg.actors) if (o.stateAt(tj).visible && Math.abs(xAt(o, tj) - px) < 380) maxH = Math.max(maxH, heightOf(o));
                float z = Math.max(1.0f, Math.min(1.5f, 720f / (maxH * 1.3f + 130)));
                cam(Math.max(t + 0.05f, tj - 0.5f), px, ground + 70 - 360f / z, z, 0.35f);
                focusSet = true;
            }
            puddleJumps++;
            d = Math.max(d, Math.max(1.8f, tj - t + 1.3f));
        }
        // v41: "गुस्से का नाटक करती है, फिर हँस पड़ती है" — a frown put on, then the laugh breaks through it
        if (subj != null && Txt.has(s, "गुस्से का नाटक", "नाराज़ होने का नाटक", "नाराज होने का नाटक", "pretends to be angry", "mock anger", "pretend anger")) {
            Film.Key ka = keyAt(subj, t + 0.1f); ka.emotion = Pose.ANGRY;
            if (Txt.has(s, "हँस", "हंस", "laugh", "smile", "मुस्कुरा")) {
                Film.Key kl = keyAt(subj, t + 1.5f); kl.emotion = Pose.LAUGH;
                subj.acts.add(new Film.Act(t + 1.5f, t + 3.1f, Film.G_LAUGH));
                Film.Key kh = keyAt(subj, t + 3.2f); kh.emotion = Pose.HAPPY;
            }
            d = Math.max(d, 3.2f);
        }
        if (Txt.has(s, "पसीना")) for (Film.Actor a : group) { Film.Key k = a.at(t); k.sweat = true; }
        // v41: "उसका बैग एक तरफ पड़ा है" — a school bag lying on the grass (thrown down on the way in), there from now on
        if (subj != null && Txt.has(s, "बैग", "बस्ता", "school bag", "schoolbag", "backpack", "rucksack") && Txt.has(s, "पड़ा", "पड़ी", "रखा", "फेंक", "lies", "lying", "dropped", "drops", "throws", "tossed")) {
            boolean have = false;
            for (Film.Fx f : seg.fx) if (f.type == Film.FX_THING && f.kind == 1) have = true;
            if (!have) {
                float x = xAt(subj, t), off = 0.9f * heightOf(subj);
                Film.Fx bag = fx(Film.FX_THING, t, 1e6f, x < 640 ? Math.max(130, x - off) : Math.min(1150, x + off), 0, subj, null);
                bag.kind = 1;
                groundThings++;
            }
        }
        if (Txt.has(s, "दौड़ते हुए आते", "दौड़ते हुए आती")) d = Math.max(d, 1.8f);
        if (Txt.has(s, "पहरा")) d = Math.max(d, 2.2f);
        // the cues nothing else hardcodes: sounds written as words, light and machines (Cues)
        d = Math.max(d, cuesFrom(s, t, subj, target, group));
        if (lightsOff && Txt.has(s, "रोशनी वापस", "लाइट जल", "फिर से खिल", "फिर से जल", "lights come back", "light returns", "back on", "lit up again", "जल उठ")) {
            for (Film.Fx f : seg.fx) if (f.type == Film.FX_LIGHTS_OFF && f.t1 > t) f.t1 = t + 0.6f;
            lightsOff = false;
        }
        d = Math.max(d, loudReaction(s, t, subj, d));
        // v41: on the swing and the slide (the sentence's own framing; no walk of its own after it)
        playStaged = false;
        playDelay = 0;
        if (playSentence != 0) { d = Math.max(d, playFrom(s, t, subj, group)); if (playStaged) focusSet = true; }
        playSentence = 0;
        if (!focusSet && !establishing && subj != null && subj.stateAt(t).visible && DirectorsManual.looksAt(s) && !Txt.has(s, "देखो", "look!", "look,")) {
            d = Math.max(d, pointOfView(s, t, subj, target));
            focusSet = true;
        }
        // v34: walking off, across or up to someone (last: its keys must come after every other key of the sentence)
        travelFramed = false;
        // v41: "… गले लगाते हैं और … घर की तरफ चल पड़ते हैं": the hug first, then the walk (not walking off mid-hug)
        if (travelAfter > playDelay) playDelay = travelAfter;
        if (!playStaged) d = Math.max(d, playDelay + travelFrom(s, t + playDelay, subj, target, group));
        if (travelFramed) focusSet = true;

        if (!focusSet && !establishing) {
            List<Film.Actor> vis = new ArrayList<Film.Actor>();
            for (Film.Actor a : group) if (a.stateAt(t + d * 0.5f).visible) vis.add(a);
            if (vis.size() == 1) camOn(vis.get(0), t, 1.35f);
            else if (vis.size() >= 2) camGroup(vis, t + 0.1f, d);
            else cam(t, 640, 360, 1.0f, 0.6f);
        }
        return d;
    }

    // ------------------------------------------------------------------ the director's manual: reactions and looks

    /**
     * The manual (3.2): a dramatic sound has a visible reaction. Everyone on stage startles a beat after it (the
     * head turns, the face changes) and the nearest face is shown in a reaction shot. Returns the time it needs.
     */
    private float loudReaction(String s, float t, Film.Actor subj, float d) {
        if (!DirectorsManual.loud(s)) return 0;
        Film.Actor react = null;
        float best = 1e9f, sx = subj != null ? xAt(subj, t) : 640;
        boolean danger = DirectorsManual.dangerous(s);
        for (Film.Actor a : seg.actors) {
            Film.Key k = a.stateAt(t + 0.4f);
            if (!k.visible || k.anchor == Film.A_HIDDEN) continue;
            // v35: a deaf character hears nothing: no startle — they notice the others' faces a beat later and turn to look
            if (a.look != null && a.look.cannotHear()) {
                a.acts.add(new Film.Act(t + 0.9f, t + 1.4f, Film.G_TURN));
                Film.Key kk = a.at(t + 1.0f);
                if (kk.emotion == Pose.NEUTRAL || kk.emotion == Pose.HAPPY) { kk.emotion = Pose.CURIOUS; a.at(t + 2.8f).emotion = Pose.NEUTRAL; }
                deafUnheard++;
                continue;
            }
            // the subject of a "hears a sound" sentence already has its thought beat; the others react now (v43: the
            // one named as making the sound — "धड़ाम से गिरता है", "slams the door" — does not startle at it)
            boolean maker = a == subj && subj != null && ScriptParser.mentions(story, s).contains(subj.c);
            if ((a != subj || !Handbook.stimulus(s)) && !maker) {
                a.acts.add(new Film.Act(t + 0.3f, t + 1.1f, Film.G_LISTEN));
                Film.Key kk = a.at(t + 0.45f);
                if (kk.emotion == Pose.NEUTRAL || kk.emotion == Pose.HAPPY || kk.emotion == Pose.CURIOUS) {
                    kk.emotion = danger ? Pose.SCARED : Pose.SURPRISED;
                    a.at(t + 2.8f).emotion = Pose.NEUTRAL;
                }
            }
            float dd = Math.abs(xAt(a, t) - sx) + (a == subj ? 300 : 0);
            if (dd < best) { best = dd; react = a; }
        }
        if (react == null) return 0;
        float rt = t + Math.max(1.0f, d * 0.5f);
        camOn(react, rt, 1.7f);
        Film.Shot sh = shot(rt, ShotPlanner.MCU, ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, react, null, ShotPlanner.ESCALATE);
        sh.reaction = true;
        sh.purpose = "Reaction to the sound (director's manual 3.2): " + react.c.shown() + " startles — the sound is seen on a face";
        sh.action = react.c.shown() + " turns toward the sound, a beat late";
        sh.face = faceOf(react.stateAt(rt + 0.1f).emotion);
        sh.body = bodyOf(react.stateAt(rt + 0.1f).emotion, react.look);
        sh.cutWhen = "the startle has landed";
        sh.emotionalPurpose = "A dramatic sound has a visible reaction (never a sound without a face)";
        sh.light = danger ? 0.75f : 0.5f;
        loudReactions++;
        return rt - t + 1.3f;
    }

    /**
     * The manual (3.4): a point-of-view shot shows what a character sees. The look first (the eyes go to the
     * thing, the head a moment later), then the thing seen from where they stand: the other character framed
     * medium, or the place ahead of them. Returns the time it needs.
     */
    private float pointOfView(String s, float t, Film.Actor subj, Film.Actor target) {
        Look sl = lookAt(subj, t);
        if (sl != null && sl.cannotSee()) {
            // v34 (the situations guide): a blindfolded character cannot look — no point-of-view shot; she turns her
            // head toward the sound and listens
            camOn(subj, t, 1.7f);
            Film.Shot a = shot(t, ShotPlanner.MCU, ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, subj, target, ShotPlanner.DEVELOP);
            a.purpose = (sl.glasses == 4 ? "Blindfolded: " : "Blind: ") + subj.c.shown() + " cannot see — turns toward the sound and listens (no point-of-view shot)";
            a.action = subj.c.shown() + " listens" + (target != null ? " toward " + target.c.shown() : "");
            a.face = "The head turned a little toward the sound, the face still";
            a.body = "Still";
            a.cutWhen = "the turn has settled";
            a.emotionalPurpose = "We share what she has: sound, not sight";
            blindNoPov++;
            return 1.4f;
        }
        camOn(subj, t, 1.7f);
        Film.Shot a = shot(t, ShotPlanner.MCU, ShotPlanner.SINGLE, 0, ShotPlanner.STATIC, subj, target, ShotPlanner.DEVELOP);
        a.purpose = "The look (director's manual 3.4): " + subj.c.shown() + "'s eyes go to " + (target != null ? target.c.shown() : "what the sentence names");
        a.action = subj.c.shown() + " looks" + (target != null ? " at " + target.c.shown() : " — " + clip(Txt.withoutParens(s), 50));
        a.face = "Eyes on the thing first, the head a moment later";
        a.body = "Still, turned toward it";
        a.cutWhen = "the eyes have settled";
        a.emotionalPurpose = "We know where the character looks before we see it";
        float pt = t + 1.2f;
        Film.Shot p;
        if (target != null && target.stateAt(pt).visible && target.stateAt(pt).anchor != Film.A_HIDDEN) {
            float tx = xAt(target, pt), ht = heightOf(target);
            Film.Key kt = target.stateAt(pt);
            cam(pt, tx, kt.body == Pose.LIE ? ground - 60 : ground - ht * 0.62f, 1.35f, 0);
            seg.cams.get(seg.cams.size() - 1).reverse = true;
            p = shot(pt, ShotPlanner.MEDIUM, ShotPlanner.POV, 0, ShotPlanner.STATIC, target, subj, ShotPlanner.DEVELOP);
            p.purpose = "Point of view: what " + subj.c.shown() + " sees — " + target.c.shown();
            // v42: looking down on them from above (a roof, a balcony): seen from above, in their picture from above
            p.above = Txt.has(s, LOOK_DOWN);
            p.action = target.c.shown() + " as " + subj.c.shown() + " sees them, from where " + subj.c.shown() + " stands";
            p.face = faceOf(kt.emotion);
            p.body = bodyOf(kt.emotion, target.look);
        } else {
            float facing = subj.stateAt(t).facing;
            float cx = Math.max(240, Math.min(1040, xAt(subj, t) + facing * 420));
            cam(pt, cx, 372, 1.15f, 0);
            p = shot(pt, ShotPlanner.WIDE, ShotPlanner.POV, 0, ShotPlanner.STATIC, null, subj, ShotPlanner.DEVELOP);
            p.subject = "what " + subj.c.shown() + " sees";
            p.purpose = "Point of view: what " + subj.c.shown() + " sees — " + clip(Txt.withoutParens(s), 50);
            p.action = "The place ahead of " + subj.c.shown() + ", from where they stand";
            p.face = "—";
            p.body = "—";
        }
        p.cutWhen = "the thing seen has registered";
        p.emotionalPurpose = "The audience sees with the character";
        povShots++;
        return 2.8f;
    }

    // ------------------------------------------------------------------ the cues of any script

    private final Map<String, Float> cueAt = new HashMap<String, Float>();

    /**
     * Stages the cues of a sentence (Cues.read): the sound named by a word is played, a thing happening to light
     * or a machine becomes an effect on the stage. Returns the time the cue deserves.
     */
    private float cuesFrom(String s, float t, Film.Actor subj, Film.Actor target, List<Film.Actor> group) {
        float d = 0;
        boolean crashing = Txt.has(s, CRASH_WORDS);
        for (Cues.Cue c : Cues.read(s)) {
            if (crashing && (c.visual == Cues.V_STEAM || c.sfx == Film.SFX_HISS_SHORT || c.sfx == Film.SFX_BOOM)) continue;      // (v44: the wreck's own steam and bang)
            // the same cue is not repeated within a second (a manner and its line, two sentences about one thing)
            Float last = cueAt.get(c.word);
            if (last != null && Math.abs(last - t) < 1f) continue;
            cueAt.put(c.word, t);
            float x = subj != null ? xAt(subj, t) : 640;
            float h = subj != null ? heightOf(subj) : 360;
            float facing = subj != null ? subj.stateAt(t).facing : 1;
            if (c.sfx >= 0 && c.visual != Cues.V_BEAM) film.sfx.add(new Film.Sfx(c.sfx, t + 0.15f, c.seconds, c.gain));
            switch (c.visual) {
                case Cues.V_GLOW: { Film.Fx f = fx(Film.FX_GLOW_AREA, t + 0.2f, t + 0.2f + Math.max(4f, c.seconds + 2), 0, 0, null, null); f.color = Txt.has(s, "हरी", "green") ? 0xFF69F0AE : Txt.has(s, "नीली", "blue") ? 0xFF40C4FF : Txt.has(s, "लाल", "red") ? 0xFFFF5252 : 0xFFFFF176; d = Math.max(d, 2.2f); break; }
                case Cues.V_TWINKLE: { fx(Film.FX_TWINKLE, t + 0.1f, t + 0.1f + Math.max(3f, c.seconds), 640, ground - 260, null, null); d = Math.max(d, 1.6f); break; }
                case Cues.V_FLICKER: { fx(Film.FX_FLICKER, t, t + Math.max(3f, c.seconds), 0, 0, null, null); break; }
                case Cues.V_OFF: { fx(Film.FX_LIGHTS_OFF, t + 0.3f, seg.t1 > t + 2 ? Float.MAX_VALUE : t + 6f, 0, 0, null, null).kind = 1; lightsOff = true; d = Math.max(d, 2.2f); break; }
                case Cues.V_GLITCH: { fx(Film.FX_GLITCH, t + 0.1f, t + 0.1f + Math.max(0.8f, c.seconds), 0, 0, null, null); if (subj != null) { Film.Key k = subj.at(t + 0.2f); k.emotion = Pose.SCARED; } d = Math.max(d, 1.6f); break; }
                case Cues.V_DATA: { Film.Fx f = fx(Film.FX_DATA, t + 0.2f, t + 0.2f + Math.max(3f, c.seconds), x, ground - 200, null, null); f.kind = facing > 0 ? 0 : 1; d = Math.max(d, 2.4f); break; }
                case Cues.V_BEAM: {
                    Film.Actor victim = target;
                    if (victim == null && group != null) for (Film.Actor a : group) if (a != subj) { victim = a; break; }
                    if (subj != null) { Film.Fx beam = fx(Film.FX_BEAM, t + 0.6f, t + 0.6f + Math.max(2.5f, c.seconds), 0, 0, subj, victim); beam.color = 0xFFFFF59D; subj.acts.add(new Film.Act(t + 0.3f, t + 3.3f, Film.G_POINT)); }
                    if (victim != null) { victim.acts.add(new Film.Act(t + 1f, t + 3f, Film.G_SHIELD_EYES)); Film.Key k = victim.at(t + 1f); k.emotion = Pose.SCARED; }
                    film.sfx.add(new Film.Sfx(Film.SFX_CHIME, t + 0.6f, 1.6f, 0.25f));
                    d = Math.max(d, 3.2f); break;
                }
                case Cues.V_DRONE: { fx(Film.FX_DRONE, t, t + c.seconds, x + facing * 200, ground - 300, subj, null); break; }
                case Cues.V_HEARTS: { if (subj != null) { fx(Film.FX_HEARTS, t + 0.2f, t + 0.2f + c.seconds, 0, 0, subj, null); Film.Key k = subj.at(t + 0.2f); k.emotion = Pose.HAPPY; } d = Math.max(d, 2f); break; }
                case Cues.V_NOTIFY: { if (subj != null) { fx(Film.FX_NOTIFY, t + 0.2f, t + 0.2f + c.seconds, 0, 0, subj, null); subj.acts.add(new Film.Act(t + 0.3f, t + 1.8f, Film.G_LOOK_UP)); } d = Math.max(d, 2f); break; }
                case Cues.V_SPARKS: { fx(Film.FX_SPARKS, t + 0.3f, t + 0.3f + Math.max(0.8f, c.seconds), x + facing * 60, ground - h * 0.45f, subj, null); if (subj != null) subj.acts.add(new Film.Act(t, t + 1.2f, Film.G_PULL)); d = Math.max(d, 1.6f); break; }
                case Cues.V_BLAST: { fx(Film.FX_FLASH, t + 0.3f, t + 1.1f, 640, 360, null, null).color = 0xFFFFE082; shake(t + 0.3f, t + 1.5f); fx(Film.FX_SMOKE, t + 0.5f, t + 4f, x + facing * 120, ground - 120, null, null).color = 0xFF616161; for (Film.Actor a : seg.actors) if (a.stateAt(t).visible) { Film.Key k = a.at(t + 0.4f); k.emotion = Pose.SCARED; } d = Math.max(d, 2.5f); break; }
                case Cues.V_STEAM: { fx(Film.FX_STEAM, t + 0.2f, t + 0.2f + Math.max(2.5f, c.seconds), x + facing * 80, ground - h * 0.3f, null, null); break; }
                default:
            }
        }
        return d;
    }

    /** The lights went out in this part (the stage stays dark until the story lights it again). */
    private boolean lightsOff;

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

    // ------------------------------------------------------------------ v40: round a campfire

    /** v40: those who walk round the fire, with what their lines say about the walk: {t, PACE_*, seconds}. */
    private final java.util.Map<Film.Actor, List<float[]>> pacers = new java.util.LinkedHashMap<Film.Actor, List<float[]>>();
    /** v40: the fire's middle and base on the stage, the seats' ring and the walkers' ring (their half-widths), the rings' depth. */
    private float fireCx, fireCy, fireRs, fireRw, fireRy;
    static final int PACE_WALK = 1, PACE_FRONT = 2, PACE_REVERSE = 3, PACE_SIT = 4, PACE_STOP = 5;
    static final String[] PACING = {"गोल गोल घूम", "गोल-गोल घूम", "चारों तरफ घूम", "चारों ओर घूम", "चक्कर लगा", "चक्कर काट", "टिक ही नहीं", "टिकता नहीं",
            "टिक नहीं सकता", "टिक नहीं सकती", "paces", "pacing", "walks round", "walks around", "circles the", "can't sit still", "cannot sit still"};

    /**
     * v40: a fire burning from the start of the part (the place has it: "बीच में अलाव जल रहा है") is the middle of the
     * stage: in the user's picture it is where the picture's own flames are; those the lists seat ("दो पर अर्जुन और समीर
     * बैठे हैं", "बैठे बैठे … पैर हिलाता है") sit on logs on either side of it, facing it; one who cannot keep still
     * ("अलाव के चारों तरफ … गोल गोल घूम रहा है") walks round it, behind it and in front of it.
     */
    private void campfire(Story.Scene sc) {
        pacers.clear();
        if (seg == null || wOpen[Film.W_FIRE] < 0 || seg.actors.isEmpty()) return;
        boolean picFire = seg.backdrop != null && seg.backdrop.scan != null && seg.backdrop.scan.fireU >= 0;
        float cx = 640, cy = 0;
        if (picFire) {
            Art.Backdrop b = seg.backdrop;
            float px = (b.scan.fireU - b.x0) / Math.max(1e-3f, b.x1 - b.x0) * 1280, py = (b.scan.fireV - b.y0) / Math.max(1e-3f, b.y1 - b.y0) * 720;
            // the place is drawn at the stage's own depth (Seg.midground): its fire is where the picture has it on the
            // stage. (The people keep to the stage's ground line at their own size: a picture taken close to the fire
            // shows its far side high up, and taking that depth for the ring made them tiny.)
            if (px > 120 && px < 1160) { cx = px; seg.midground = true; }
        }
        fireX = cx;
        fireCx = cx; fireCy = cy;
        float scale = seg.charScale;
        fireRs = 330 * scale; fireRw = 430 * scale; fireRy = 45 * scale;
        // who sits: the lists name them seated (a clause with their name and "बैठे हैं"), or their own description
        // has them sitting the whole time ("बैठे बैठे")
        List<Film.Actor> sit = new ArrayList<Film.Actor>(), walk = new ArrayList<Film.Actor>();
        StringBuilder lists = new StringBuilder();
        for (Story.PlaceDef th : story.things) lists.append(th.name).append(" । ").append(th.description).append(" । ");
        for (Film.Actor a : seg.actors) {
            if (a.look == null || !a.look.isHumanoid() || a.c.voiceOnly || !a.keys.get(0).visible) continue;
            String own = a.c.description == null ? "" : a.c.description;
            if (Txt.has(own, PACING)) { walk.add(a); continue; }
            boolean seated = Txt.has(own, "बैठे बैठे", "बैठे-बैठे", "बैठा रहता", "बैठी रहती");
            for (String clause : (lists + " । " + own).split("[।.,;!?]")) {
                if (!Txt.has(clause, SIT_DOWN) || Txt.has(clause, "नहीं", "not ")) continue;
                for (Story.CharacterDef c : ScriptParser.mentions(story, clause)) if (c == a.c) seated = true;
            }
            if (seated) sit.add(a);
        }
        if (sit.isEmpty() && walk.isEmpty()) return;
        // the seats: the first on the left, the second on the right, a little toward the camera; more round the back
        float[] seatTh = {(float) Math.PI + 0.3f, -0.3f, (float) Math.PI * 0.5f + 0.9f, (float) Math.PI * 0.5f - 0.9f};
        int si = 0;
        for (Film.Actor a : sit) {
            float th = seatTh[Math.min(si++, seatTh.length - 1)];
            float x = cx + fireRs * (float) Math.cos(th), lift = cy - fireRy * 0.7f * (float) Math.sin(th);
            for (Film.Key k : a.keys) { k.x = x; k.body = Pose.SIT; k.seat = Film.SEAT_LOG; k.facing = cx > x ? 1 : -1; }
            Film.Track tr = new Film.Track();
            tr.t0 = seg.t0; tr.t1 = 1e6f; tr.x = x; tr.lift = lift; tr.facing = cx > x ? 1 : -1;
            a.track.add(tr);
            Film.Fx f = fx(Film.FX_SEAT, seg.t0, 1e6f, x, lift, a, null); f.kind = Film.SEAT_LOG;
            sittings++;
        }
        // an empty log (the lists count three logs, two taken) behind the fire, for the walker's short sit
        for (Film.Actor a : walk) {
            List<float[]> ev = new ArrayList<float[]>();
            pacers.put(a, ev);
            float th = (float) Math.PI * 0.5f + 0.5f;
            for (Film.Key k : a.keys) { k.x = cx + fireRw * (float) Math.cos(th); k.body = Pose.STAND; k.facing = -1; }
        }
        // (the walker's short sit is on the far log of the ring — the place's picture has its own logs there; a drawn one
        // would stand over the picture's fire)
        if (!walk.isEmpty() && !picFire) {
            Film.Fx f = fx(Film.FX_SEAT, seg.t0, 1e6f, cx, cy - fireRy, walk.get(0), null); f.kind = Film.SEAT_LOG;
        }
        film.notes.add("  ↳ round the fire" + (picFire ? " (where your picture has it)" : "") + ": " + sit.size() + " seated on logs, " + walk.size() + " walking round it");
    }

    /**
     * v40: what a walker's line says about the walk — "अलाव के पीछे से सामने आते हुए" (from behind the fire to the front),
     * "अलाव के चारों तरफ उल्टा घूमते हुए" (the other way round), "दो सेकंड के लिए खाली लट्ठे पर बैठता है, फिर … खड़ा हो जाता
     * है और फिर से घूमना शुरू कर देता है" (a short sit, up again, walking on). Returns the rest of the manner.
     */
    private String paceEvents(Film.Actor a, String m, float t0, float t1) {
        List<float[]> ev = pacers.get(a);
        StringBuilder rest = new StringBuilder();
        boolean sat = false;
        for (String clause : m.split(",|،|\\s+फिर\\s+|\\s+और\\s+|।")) {
            String c = clause.trim();
            if (c.length() == 0) continue;
            if (Txt.has(c, "बैठ") && !Txt.has(c, "नहीं")) {
                float secs = 2.5f;
                java.util.regex.Matcher mm = java.util.regex.Pattern.compile("([0-9]+|एक|दो|तीन|चार|पाँच|पांच)\\s*(सेकंड|सेकेंड|second)").matcher(c);
                if (mm.find()) {
                    String n = mm.group(1);
                    secs = n.matches("[0-9]+") ? Integer.parseInt(n) : n.equals("एक") ? 1 : n.equals("दो") ? 2 : n.equals("तीन") ? 3 : n.equals("चार") ? 4 : 5;
                }
                ev.add(new float[]{t0, PACE_SIT, Math.max(1f, Math.min(8f, secs))});
                sat = true;
                continue;
            }
            if (sat && Txt.has(c, "खड़ा हो", "खड़ी हो", "उठ")) continue;            // the getting up belongs to the short sit
            if (Txt.has(c, "पीछे से") && Txt.has(c, "सामने")) { ev.add(new float[]{t0, PACE_FRONT, t1 - t0}); continue; }
            // a step back, a stop: the walk halts for the line (the step itself is the step-back gesture)
            if (Txt.has(c, "पीछे कदम", "कदम पीछे", "रुक जाता", "रुक जाती", "रुककर", "रुक कर", "ठहर")) { ev.add(new float[]{t0, PACE_STOP, t1 - t0}); rest.append(rest.length() > 0 ? ", " : "").append(c); continue; }
            if (Txt.has(c, "घूम", "चक्कर")) { ev.add(new float[]{t0, Txt.has(c, "उल्टा", "उल्टी", "उलटा") ? PACE_REVERSE : PACE_WALK, t1 - t0}); continue; }
            if (rest.length() > 0) rest.append(", ");
            rest.append(c);
        }
        return rest.toString();
    }

    /** v40: the angle after th, going dir, where the ring is at target (mod a full turn). */
    static float aheadTo(float th, float target, float dir) {
        float twoPi = (float) (2 * Math.PI);
        float d = (target - th) % twoPi;
        if (dir > 0) { if (d < 0.05f) d += twoPi; }
        else { if (d > -0.05f) d -= twoPi; }
        return th + d;
    }

    /**
     * v40: each walker's path round the fire for the whole part: walks at a changing pace (now quick, now slow) with
     * sudden stops in between, as the description says ("कभी तेज चलता है, कभी धीरे, कभी अचानक रुक जाता है"), turned by
     * what the lines say — round to the front, the other way round, to the empty log for a short sit and up again.
     */
    private void paceTracks(float tEnd) {
        for (java.util.Map.Entry<Film.Actor, List<float[]>> en : pacers.entrySet()) {
            Film.Actor a = en.getKey();
            List<float[]> ev = en.getValue();
            java.util.Collections.sort(ev, new java.util.Comparator<float[]>() { public int compare(float[] x, float[] y) { return Float.compare(x[0], y[0]); } });
            java.util.Random rnd = new java.util.Random(31 + a.order);
            float th = (float) Math.PI * 0.5f + 0.5f, dir = 1, t = seg.t0;
            int ei = 0;
            while (t < tEnd - 0.2f) {
                float[] e = ei < ev.size() ? ev.get(ei) : null;
                float limit = e != null ? Math.max(t, Math.min(tEnd, e[0])) : tEnd;
                // before a walk to the front: end the free walking behind the fire, so the walk comes from behind —
                // the walks before it share the way at an even pace (a lap more when there is time for it)
                float aim = e != null && (int) e[1] == PACE_FRONT ? aheadTo(th, (float) Math.PI * 0.5f, dir) : Float.NaN;
                if (!Float.isNaN(aim) && limit - t > 1.2f) {
                    float free = limit - t, way = Math.abs(aim - th);
                    while (way / (free * 0.7f) < 0.3f && (way + 2 * Math.PI) / (free * 0.7f) <= 0.85f) way += 2 * Math.PI;
                    int n = Math.max(1, Math.round(free / 4.5f));
                    float pause = n > 1 ? Math.min(1.4f, free * 0.3f / (n - 1)) : 0, walkT = (free - pause * (n - 1)) / n;
                    float carry = 0;
                    for (int i = 0; i < n; i++) {
                        float to = th + dir * way / n, wt = walkT + carry;
                        carry = 0;
                        walkArc(a, t, t + wt, th, to); th = to; t += wt;
                        // v42: (no pause on top of someone: the walk takes that time instead)
                        if (i < n - 1) { if (crowded(a, th, t)) carry = pause; else { hold(a, t, t + pause, th); t += pause; } }
                    }
                    t = limit;
                }
                while (limit - t > 0.6f) {
                    float d = Math.min(limit - t, 2.6f + rnd.nextFloat() * 3.4f);
                    if (limit - t - d < 0.8f) d = limit - t;
                    float w = rnd.nextFloat() < 0.25f ? 0.8f : 0.3f + rnd.nextFloat() * 0.3f;
                    float to = th + dir * w * d;
                    if (!Float.isNaN(aim) && d >= limit - t - 1e-3f) to = aim;               // the last stretch arrives behind
                    else if (!Float.isNaN(aim) && (aim - to) * dir < 0) to = aim;
                    // v42: never a standstill on top of someone (the seated man beside the fire): walk on past them
                    if (Math.abs(to - th) <= 0.05f && crowded(a, th, t)) to = th + dir * 0.3f * d;
                    if (Math.abs(to - th) > 0.05f) { walkArc(a, t, t + d, th, to); th = to; }
                    else hold(a, t, t + d, th);
                    t += d;
                    if (limit - t > 1.4f && rnd.nextFloat() < 0.75f && !crowded(a, th, t)) { float p = Math.min(limit - t, 0.7f + rnd.nextFloat() * 1.5f); hold(a, t, t + p, th); t += p; }
                }
                if (t < limit) { hold(a, t, limit, th); t = limit; }
                if (e == null) break;
                ei++;
                int type = (int) e[1];
                float len = Math.max(1.5f, Math.min(7f, e[2]));
                if (type == PACE_REVERSE) dir = -dir;
                if (type == PACE_FRONT) {
                    float to = aheadTo(th, (float) Math.PI * 1.5f, dir), d = Math.max(Math.min(len, 6f), Math.abs(to - th) / 0.9f);
                    walkArc(a, t, t + d, th, to); th = to; t += d;
                    float p = Math.min(1.5f, Math.max(0, tEnd - t)); hold(a, t, t + p, th); t += p;
                } else if (type == PACE_STOP) {
                    // (a stop the line asks for, a step or two on when it would come on top of someone)
                    for (int k = 0; k < 4 && len > 1.2f && crowded(a, th, t); k++) {
                        float to = th + dir * 0.35f;
                        walkArc(a, t, t + 0.7f, th, to); th = to; t += 0.7f; len -= 0.7f;
                    }
                    hold(a, t, t + len, th); t += len;
                } else if (type == PACE_WALK || type == PACE_REVERSE) {
                    float d = len, to = th + dir * 0.55f * d;
                    walkArc(a, t, t + d, th, to); th = to; t += d;
                } else if (type == PACE_SIT) {
                    float to = aheadTo(th, (float) Math.PI * 0.5f, dir), d = Math.max(1.2f, Math.abs(to - th) / 0.8f);
                    walkArc(a, t, t + d, th, to); th = to; t += d;
                    float secs = e[2];
                    Film.Key ks = insertKey(a, t); ks.body = Pose.SIT; ks.seat = Film.SEAT_LOG;
                    Film.Key ku = insertKey(a, t + secs); ku.body = Pose.STAND; ku.seat = -1;
                    for (Film.Key k : a.keys) if (k.t > t + secs) { k.body = Pose.STAND; k.seat = -1; }
                    hold(a, t, t + secs + 0.9f, th);
                    t += secs + 0.9f;
                    sittings++; risings++;
                }
            }
            // the keys' own places follow the path (the camera and the others' glances read them)
            for (Film.Key k : a.keys) { Film.Track r = a.trackAt(k.t); if (r != null) { k.x = r.x(k.t); k.moveDur = 0; } }
            int walks = 0;
            for (Film.Track r : a.track) if (r.walking) walks++;
            film.notes.add("  ↳ " + a.c.shown() + " walks round the fire: " + walks + " stretches, stops in between" + (ev.isEmpty() ? "" : ", " + ev.size() + " turns of the walk from the lines"));
        }
        // the seated keep their seats to the end of the part
        if (seg != null) for (Film.Actor a : seg.actors) for (Film.Track r : a.track) if (r.t1 > 1e5f) r.t1 = tEnd + 2f;
    }

    /** v43: the drawing height of the vehicle under its rider in px, as the renderer draws it (its doors and seats are measured from it). */
    private float vehiclePx(Film.Actor a, int v) {
        float h = Renderer.actorHeight(a.look, a.c, art, seg);
        return v == Film.V_BICYCLE || !a.look.isChild() ? h : h / 0.78f;
    }

    /** v43: this rider's latest ride of vehicle v (as its driver or rider) begun before t, or null. */
    private static Film.Act lastRide(Film.Actor a, int v, float t) {
        Film.Act r = null;
        for (Film.Act x : a.acts) if (x.type == Film.G_RIDE && x.item > 0 && x.item % Film.V_PASSENGER == v && x.t0 < t && (r == null || x.t0 > r.t0)) r = x;
        return r;
    }

    /** v43: where a vehicle going dir may stop with all of it in the picture — its front ahead of its rider, its back (and rear door) behind. */
    private static float inFrame(float to, float dir, int v, float vs) {
        float front = v == Film.V_CAR ? 0.95f : v == Film.V_BUS ? 1.9f : v == Film.V_AUTO ? 0.6f : 0.5f;
        float back = v == Film.V_CAR ? 1.0f : v == Film.V_BUS ? 1.9f : v == Film.V_AUTO ? 0.75f : 0.55f;
        float lo = (dir > 0 ? back : front) * vs + 20, hi = 1280 - (dir > 0 ? front : back) * vs - 20;
        if (lo > hi) return (lo + hi) / 2;
        return Math.max(lo, Math.min(hi, to));
    }

    /** v43: made visible only just now (an entrance at the edge of the picture for this very sentence). */
    private boolean justIn(Film.Actor a, float t) {
        for (Film.Key k : a.keys) if (k.visible) return k.t >= t - 0.7f && k.t > seg.t0 + 0.05f;     // (not those on the stage from the start)
        return true;
    }

    /** v43: nothing of theirs in this part before t — no line, no act, no step — so they may as well arrive now. */
    private boolean firstThing(Film.Actor a, float t) {
        for (Film.Speak x : a.speaks) if (x.t0 < t && x.t0 >= seg.t0) return false;
        for (Film.Act x : a.acts) if (x.t0 < t && x.t0 >= seg.t0) return false;
        Film.Key first = null;
        for (Film.Key k : a.keys) {
            if (k.t >= t) break;
            if (first == null) first = k;
            else if (Math.abs(k.x - first.x) > 1 || k.moveDur > 0) return false;
        }
        return true;
    }

    /** v43: an auto or a taxi hailed in this part, standing where it stopped for them (null: none waiting). */
    private Film.Fx hailed;
    static final String[] HAIL = {"ऑटो रोक", "टैक्सी रोक", "रिक्शा रोक", "ऑटो को रोक", "टैक्सी को रोक", "रिक्शे को रोक", "ऑटो बुला", "टैक्सी बुला", "रिक्शा बुला",
            "ऑटो को आवाज़", "टैक्सी को आवाज़", "hails a", "hails an", "hails the", "flags down", "waves down", "stops an auto", "stops a taxi", "calls an auto", "calls a taxi", "stops a rickshaw"};

    /** v43: the walk apart after this ride is taken back (the ride goes on): the riders stay where it stopped. */
    private void dropOutKeys(Film.Act ride) {
        List<Object[]> l = outKeys.remove(ride);
        if (l == null) return;
        for (Object[] o : l) {
            Film.Actor a = (Film.Actor) o[0];
            Film.Key ko = (Film.Key) o[1];
            int i = a.keys.indexOf(ko);
            if (i <= 0) continue;
            float back = a.keys.get(i - 1).x;
            for (int j = i + 1; j < a.keys.size(); j++) {
                Film.Key l2 = a.keys.get(j);
                if (l2.moveDur == 0 && Math.abs(l2.x - ko.x) < 0.5f) l2.x = back; else break;
            }
            a.keys.remove(i);
        }
    }

    /** v43: the ride this one runs straight on from (the same vehicle, ending as it starts), or null. */
    public static Film.Act rideBefore(Film.Actor a, Film.Act r) {
        for (Film.Act x : a.acts) if (x != r && x.type == Film.G_RIDE && x.item == r.item && Math.abs(x.t1 - r.t0) < 0.1f) return x;
        return null;
    }

    /**
     * v43: one who only got on (and waits on it, the engine running) stays on until the next thing they or the one on
     * the seat behind do — then they get off first; with nothing more to do in the part they stay on to its end.
     */
    private void settleStays() {
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors) for (Film.Act r : new ArrayList<Film.Act>(a.acts)) {
            if (r.type != Film.G_RIDE || r.how != -1 || r.item <= 0 || rideJoined(a, r, true)) continue;
            List<Film.Actor> who = new ArrayList<Film.Actor>();
            List<Film.Act> rides = new ArrayList<Film.Act>();
            who.add(a); rides.add(r);
            for (Film.Actor o : sg.actors) for (Film.Act x : o.acts) if (x.type == Film.G_RIDE && x.target == a && Math.abs(x.t0 - r.t0) < 0.05f) { who.add(o); rides.add(x); }
            float from = r.t0 + rideMount(r) + 0.3f, next = Float.MAX_VALUE;
            for (Film.Actor o : who) {
                Film.Key prev = null;
                for (Film.Key k : o.keys) {
                    if (k.t > from && prev != null && !rideKeys.contains(k)
                            && (Math.abs(k.x - prev.x) > 1 || k.moveDur > 0 || k.body != prev.body || k.visible != prev.visible)) { next = Math.min(next, k.t); break; }
                    prev = k;
                }
                for (Film.Act x : o.acts) if (x.t0 > from && x.type != Film.G_TALK && x.type != Film.G_SIGN && x.type != Film.G_RIDE && !gestureOnSeat(x.type)) next = Math.min(next, x.t0);
            }
            float end = next == Float.MAX_VALUE ? sg.t1 + 1 : Math.max(r.t0 + rideMount(r) + 1.2f, next - 0.05f);
            for (int i = 0; i < rides.size(); i++) {
                Film.Act x = rides.get(i);
                float was = x.t1;
                x.t1 = end + (x == r ? 0 : 0);
                for (Film.Fx f : sg.fx) if (f.type == Film.FX_PARKED && f.a == who.get(i) && Math.abs(f.t0 - was) < 0.06f) f.t0 = x.t1;
            }
        }
    }

    /** v43: what one sitting on a vehicle can still do without getting off: wave, nod, point, laugh, look round. */
    private static boolean gestureOnSeat(int g) {
        return g == Film.G_WAVE || g == Film.G_NOD || g == Film.G_POINT || g == Film.G_LAUGH || g == Film.G_LOOK_UP || g == Film.G_LOOK_AWAY
                || g == Film.G_SHAKE_HEAD || g == Film.G_TURN || g == Film.G_CRY;
    }

    /**
     * v43: a rider never turns on the seat (a turn to whoever speaks flipped the whole vehicle round in one frame): only
     * the ride's own keys turn them — its start, and a vehicle turning round before the family gets in.
     */
    private void steadyRiders() {
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors) for (Film.Act r : a.acts) {
            if (r.type != Film.G_RIDE) continue;
            Film.Key rule = null;
            for (Film.Key k : a.keys) {
                if (k.t > r.t1 - 0.05f) break;
                if (rideKeys.contains(k)) { if (k.t <= r.t1 - 0.05f) rule = k; continue; }
                if (k.t > r.t0 + 0.05f && rule != null && k.facing != rule.facing) { k.facing = rule.facing; steadied++; }
            }
        }
    }
    int steadied;

    // ---- v43: getting on and off — read the same by the renderer (the picture) and by vehicleDoors (the sound)

    /** v43: how long getting on takes at the start of a ride (0: a ride that starts already seated). */
    public static float rideMount(Film.Act r) { return r.a0 > 0 ? r.a0 : 0; }

    /** v43: when the vehicle stops, from the ride's start; getting off comes after it. */
    public static float rideStop(Film.Act r) { return r.a1 > 0 ? r.a1 : Math.max(0, r.t1 - r.t0 - 0.6f); }

    /** v43: a ride its rider ends out of the picture (rode away): nobody gets off in sight. */
    public static boolean rideAway(Film.Actor a, Film.Act r) { return !a.stateAt(r.t1 + 0.1f).visible; }

    /**
     * v41: a ride that runs straight on into another ride of the same vehicle (after = true), or straight on from one
     * (after = false): the car waits, its driver at the wheel, while the family gets in — no getting off, no fading.
     */
    public static boolean rideJoined(Film.Actor a, Film.Act r, boolean after) {
        for (Film.Act x : a.acts)
            if (x != r && x.type == Film.G_RIDE && x.item == r.item && Math.abs(after ? x.t0 - r.t1 : x.t1 - r.t0) < 0.1f) return true;
        return false;
    }

    /** v43: when one who rides inside is in, from the ride's start — one after another through the door (Act.how: the how-manyth). */
    public static float rideInEnd(Film.Act r) { return 0.55f + 0.3f * Math.max(0, r.how - 1); }

    /**
     * v43: how far a car's front door [0] and rear door [1] stand open at t (0 shut .. 1 open). The driver's door opens
     * as they step to it and shuts once they sit — unless the car was already being driven (the family's car waiting,
     * him at the wheel) — and opens again when it stops and shuts after them — unless it drives on, or out of the
     * picture. The rear door the same for each one riding inside, one after another, and for one who takes the car as
     * a passenger (a taxi). The car under its driver, the car parked after the ride and the door sounds read this.
     */
    public static float[] carDoors(Film.Seg s, Film.Actor driver, float t) {
        float df = 0, dr = 0;
        for (Film.Act r : driver.acts) {
            if (r.type != Film.G_RIDE || r.item <= 0 || r.item % Film.V_PASSENGER != Film.V_CAR || t < r.t0 - 0.05f || t > r.t1 + 0.5f) continue;
            float o = doorOpen(r, t, !rideJoined(driver, r, false), !rideJoined(driver, r, true) && !rideAway(driver, r), 0.55f);
            if (r.item > Film.V_PASSENGER) dr = Math.max(dr, o); else df = Math.max(df, o);
        }
        if (s != null) for (Film.Actor p : s.actors) {
            if (p == driver) continue;
            for (Film.Act r : p.acts)
                if (r.type == Film.G_RIDE && r.target == driver && r.item == -Film.V_CAR && t >= r.t0 - 0.05f && t <= r.t1 + 0.5f) {
                    float o = doorOpen(r, t, !rideJoined(p, r, false), !rideAway(p, r) && !rideJoined(p, r, true), rideInEnd(r));
                    // v44: the one beside the driver gets in and out by the front door
                    if (r.seat == Film.CAR_FRONT) df = Math.max(df, o); else dr = Math.max(dr, o);
                }
        }
        return new float[]{df, dr};
    }

    /** v43: one door for one ride: it opens over 0.3 s and shuts 0.05 s after they are in; it opens as they are about to get out (0.75 s before the ride's end) and shuts after them. */
    private static float doorOpen(Film.Act r, float t, boolean getIn, boolean getOut, float in) {
        float u = t - r.t0, o = 0;
        if (getIn && rideMount(r) > 0) o = Rig.smooth(0, 0.3f, u) * (1 - Rig.smooth(in + 0.05f, in + 0.35f, u));
        if (getOut) {
            float end = r.t1 - r.t0;
            o = Math.max(o, Rig.smooth(end - 0.75f, end - 0.5f, u) * (1 - Rig.smooth(end + 0.05f, end + 0.35f, u)));
        }
        return o;
    }

    /** v43: a car's doors heard as they open (the handle's click) and as they shut (the thunk) — once for a door several use. */
    private void vehicleDoors() {
        for (Film.Seg s : film.segs) for (Film.Actor a : s.actors) {
            float from = Float.MAX_VALUE, until = -1;
            for (Film.Act r : a.acts)
                if (r.type == Film.G_RIDE && r.item > 0 && r.item % Film.V_PASSENGER == Film.V_CAR) { from = Math.min(from, r.t0 - 0.05f); until = Math.max(until, r.t1 + 0.5f); }
            if (until < 0) continue;
            boolean[] open = new boolean[2];
            for (float t = from; t <= until; t += 0.04f) {
                float[] d = carDoors(s, a, t);
                for (int i = 0; i < 2; i++) {
                    if (!open[i] && d[i] > 0.06f) { open[i] = true; film.sfx.add(sfxAt(Film.SFX_CAR_DOOR, t - 0.04f, 0.3f, 0.3f, a)); }
                    else if (open[i] && d[i] < 0.03f) { open[i] = false; film.sfx.add(sfxAt(Film.SFX_CAR_SHUT, t - 0.02f, 0.4f, 0.36f, a)); }
                }
            }
        }
    }

    /** v42: a ride that runs straight on from another ride of the same vehicle (no fading in: the car waits). */
    private static boolean rideJoinedBefore(Film.Actor a, Film.Act r) {
        for (Film.Act x : a.acts) if (x != r && x.type == Film.G_RIDE && x.item == r.item && Math.abs(x.t1 - r.t0) < 0.1f) return true;
        return false;
    }

    /** v42: the place on the ring at th is taken at t — someone (a seated man beside the fire) is there, at about its depth. */
    private boolean crowded(Film.Actor a, float th, float t) {
        float x = fireCx + fireRw * (float) Math.cos(th), lift = fireCy - fireRy * (float) Math.sin(th);
        for (Film.Actor o : seg.actors) {
            if (o == a || !o.stateAt(t).visible || o.stateAt(t).anchor != Film.A_GROUND) continue;
            if (Math.abs(xAt(o, t) - x) < 70 && Math.abs(liftAt(o, t) - lift) < 30) return true;
        }
        return false;
    }

    private void walkArc(Film.Actor a, float t0, float t1, float th0, float th1) {
        Film.Track r = new Film.Track();
        r.t0 = t0; r.t1 = t1; r.walking = true;
        r.cx = fireCx; r.cy = fireCy; r.rx = fireRw; r.ry = fireRy; r.th0 = th0; r.th1 = th1;
        a.track.add(r);
    }

    private void hold(Film.Actor a, float t0, float t1, float th) {
        if (t1 - t0 < 0.01f) return;
        Film.Track r = new Film.Track();
        r.t0 = t0; r.t1 = t1;
        r.x = fireCx + fireRw * (float) Math.cos(th); r.lift = fireCy - fireRy * (float) Math.sin(th);
        r.facing = fireCx > r.x ? 1 : -1;
        if (Math.abs(fireCx - r.x) < 40) r.facing = a.stateAt(t0).facing;
        a.track.add(r);
    }

    /** v40: a key at t in its place among the keys (the state there copied), for a path laid after the part was staged. */
    private static Film.Key insertKey(Film.Actor a, float t) {
        int i = 0;
        while (i < a.keys.size() && a.keys.get(i).t <= t) i++;
        Film.Key p = a.keys.get(Math.max(0, i - 1)), k = p.copy();
        k.t = t; k.moveDur = 0;
        // one still on the way keeps going from where they are (as Film.Actor.at does), never a jump to where they go
        if (p.moveDur > 0 && t > p.t && t < p.t + p.moveDur) { k.moveDur = p.t + p.moveDur - t; k.run = p.run; }
        a.keys.add(i, k);
        return k;
    }

    /** v40: how far the feet are above (negative) or below the ground line at t — a walker behind the fire, the seated beside it. */
    public static float liftAt(Film.Actor a, float t) {
        Film.Track r = a.trackAt(t);
        return r != null ? r.lift(t) : 0;
    }
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

    static final String[] RAIN = {"बारिश", "वर्षा", "बरसात", "बूँदाबाँदी", "बूंदाबांदी", "बरसने", "बरस रह", "rain", "drizzl", "monsoon", "baarish", "barish",
            // v43: drops from the sky, a downpour
            "बूँदें गिर", "बूंदें गिर", "बूँदें पड़", "बूंदें पड़", "पानी बरस", "begins to pour", "starts to pour", "it pours", "it is pouring", "it's pouring"};
    /** v43: hail — icy pellets with the rain. */
    static final String[] HAILSTONES = {"ओले", "ओलों", "ओला", "hailstone", "hailstorm", "hail storm", "it hails", "hail falls", "hail is falling", "hail begins"};
    static final String[] RAIN_STOP = {"बारिश रुक", "बारिश थम", "बारिश बंद", "वर्षा रुक", "वर्षा थम", "rain stopped", "rain stops", "stopped raining",
            "rain ended", "धूप निकल", "sun came out", "बादल छँट", "बादल छंट"};
    static final String[] STORM = {"तूफ़ान", "तूफान", "storm", "cyclone", "toofan"};
    static final String[] DUST = {"आँधी", "आंधी", "dust storm", "sandstorm", "aandhi"};
    static final String[] WIND = {"हवा चल", "हवा बह", "तेज़ हवा", "तेज हवा", "ठंडी हवा", "हवा के झोंक", "हवा का झोंका", "हवा में उड़", "हवा में लहरा",
            "सरसराती हवा", "wind", "breeze", "gust", "windy", "hawa chal",
            // v43: the strong wind by what it does
            "ज़ोर की हवा", "जोर की हवा", "हवा तेज़", "हवा तेज", "डालियाँ हिल", "डालियां हिल", "पत्ते सरसरा"};
    static final String[] WIND_STRONG = {"तेज़ हवा", "तेज हवा", "ज़ोर की हवा", "जोर की हवा", "झोंक", "strong wind", "gust", "howling", "fierce wind", "डालियाँ हिल", "डालियां हिल"};
    static final String[] WIND_SOFT = {"हल्की हवा", "ठंडी हवा", "मंद हवा", "breeze", "gentle wind", "soft wind"};
    /** v42: looking down on someone from above. */
    static final String[] LOOK_DOWN = {"नीचे देख", "नीचे झाँक", "नीचे झांक", "ऊपर से देख", "ऊपर से झाँक", "छत से देख", "छत से झाँक", "बालकनी से देख", "बालकनी से झाँक",
            "looks down", "look down", "looked down", "looking down", "peers down", "from the balcony", "from the roof", "from the rooftop"};
    static final String[] WIND_STOP = {"हवा रुक", "हवा थम", "wind stopped", "wind died", "wind dropped"};
    static final String[] THUNDER = {"बिजली कड़क", "बिजली चमक", "बिजली गिर", "बादल गरज", "गड़गड़ाहट", "कड़कड़ाहट", "thunder", "lightning", "bijli"};
    /** v43: the clouds rumble with words between ("बादल ज़ोर से गरजते हैं") — never a lion's or a tiger's roar. */
    static final String[] RUMBLE = {"गरजते", "गरजता", "गरजती", "गरजने", "गरज रह", "गरज उठ", "rumble"};
    static final String[] SNOW = {"बर्फ़ गिर", "बर्फ गिर", "बर्फबारी", "बर्फ़बारी", "हिमपात",
            // v44: snow falling in other words — it comes down, flakes like cotton wool; in English only snow that falls
            // (a snowy hill, a snowman, a snowball lie on the ground: SNOW_GROUND)
            "बर्फ़ पड़", "बर्फ पड़", "फाहे गिर", "फाहे बरस", "के फाहे", "रुई जैसी बर्फ़", "रुई जैसी बर्फ", "रूई जैसी बर्फ", "बर्फ़ बरस", "बर्फ बरस", "हिमवर्षा", "बर्फ़ीला तूफ़ान", "बर्फीला तूफान",
            "snow falls", "snow fell", "snow is falling", "falling snow", "snow begins", "snow starts", "begins to snow", "starts to snow", "started to snow", "it snows",
            "it's snowing", "is snowing", "snowing", "snowfall", "snowflake", "snowstorm", "blizzard", "flurries", "snow drifts down", "snow comes down", "snow came down"};
    static final String[] FOG = {"कोहरा", "कोहरे", "धुंध", "fog", "mist", "kohra"};
    static final String[] FOG_STOP = {"कोहरा छँट", "कोहरा छंट", "धुंध छँट", "धुंध छंट", "fog lifted", "fog cleared", "mist cleared",
            "fog lifts", "fog clears", "mist lifts", "mist clears", "mist slowly clears", "fog slowly lifts", "कोहरा हट", "धुंध हट"};
    static final String[] FIRE = {"अलाव", "आग जल", "आग के पास", "आग के चारों", "आग जला", "लकड़ियाँ जल", "हवन", "campfire", "bonfire", "fire burning",
            "around the fire", "by the fire", "lit a fire"};
    static final String[] FIREFLY = {"जुगनू", "firefl"};
    static final String[] PETALS = {"फूल बरस", "फूलों की वर्षा", "फूलों की बारिश", "पुष्प वर्षा", "पंखुड़ियाँ", "पंखुड़ियां", "petals", "shower of flowers", "flowers rained"};
    static final String[] LEAVES = {"पत्ते उड़", "पत्ते गिर", "पतझड़", "सूखे पत्ते", "falling leaves", "leaves fly", "leaves flew", "autumn",
            "पत्ते झड़", "leaves fall", "dry leaves", "leaves are falling", "leaves drift"};
    static final String[] STARS = {"तारे", "सितारे", "तारों", "stars", "starry"};
    static final String[] RAINBOW = {"इंद्रधनुष", "इन्द्रधनुष", "rainbow"};
    static final String[] SEA = {"समुद्र", "सागर", "समंदर", "ocean", "sea ", "seashore", "beach", "लहरें", "लहरों", "waves", "shore", "तट पर"};
    static final String[] BOAT = {"नाव", "नौका", "किश्ती", "कश्ती", "boat", "ship", "जहाज़", "जहाज", "बेड़ा", "raft"};

    /** v40: a boat named at the start of a word ("नाव में", "boats") — "चुनाव" (an election) holds no boat. */
    static boolean boatIn(String s) {
        String t = Txt.norm(s);
        for (String w : BOAT) {
            String n = Txt.norm(w);
            int i = t.indexOf(n);
            while (i >= 0) {
                if (i == 0 || !ScriptParser.isWordChar(t.charAt(i - 1))) return true;
                i = t.indexOf(n, i + 1);
            }
        }
        return false;
    }
    static final String[] CANDLE = {"मोमबत्ती", "मोमबत्तियाँ", "candle"};
    static final String[] DIYA = {"दीया", "दीये", "दीयों", "दीपक", "दीप जल", "दीपावली", "दिवाली", "diya", "diwali", "oil lamp", "lamps",
            // v43: the aarti's flame
            "आरती उतार", "आरती कर", "आरती की थाली", "aarti"};
    static final String[] TORCH = {"मशाल", "मशालें", "लालटेन", "torch", "lantern"};
    static final String[] CLOUDS = {"बादल", "घटा", "घटाएँ", "clouds", "cloudy", "overcast"};
    static final String[] DARK_CLOUDS = {"काले बादल", "घने बादल", "dark clouds", "black clouds", "storm clouds"};
    static final String[] BIRDS_FLY = {"पक्षी उड़", "चिड़िया उड़", "चिड़ियाँ उड़", "पंछी उड़", "पक्षियों का झुंड", "birds fly", "birds flew", "flock of birds", "birds flying",
            // v43: pigeons, parrots, crows taking off
            "कबूतर उड़", "कबूतर फुर्र", "तोते उड़", "कौवे उड़", "कौए उड़", "पक्षी फुर्र", "चिड़िया फुर्र", "pigeons fly", "pigeons take off", "birds take off", "flutter away"};
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

    public int kneels;
    static final String[] KNEEL_WORDS = {"घुटनों के बल", "घुटने के बल", "घुटने टेक", "kneels", "kneel", "kneeling", "on his knees", "on her knees", "on their knees"};
    static final java.util.regex.Pattern COME_THEN_SIT = java.util.regex.Pattern.compile("(आकर|आ कर|जाकर|जा कर|comes in|enters|walks in|walks to)[^।.]*(बैठ|sits|sat)");
    static final String[] SIT_DOWN = {"बैठ गया", "बैठ गई", "बैठ गए", "बैठ गयी", "बैठ जाता", "बैठ जाती", "बैठ जाते", "बैठते हैं", "बैठा है", "बैठी है",
            "बैठे हैं", "बैठा हुआ", "बैठी हुई", "बैठे हुए", "पर बैठा", "पर बैठी", "पर बैठे", "बैठकर", "बैठ कर", "sat down", "sits down", "sat on", "sits on",
            "is sitting", "are sitting", "was sitting", "were sitting", "seated", "took a seat", "baith gaya", "baith gayi",
            // v39: the present of the user's directions — "उसके पास आकर बैठती है"
            "बैठती है", "बैठता है", "बैठती हैं", "बैठते है", "sits next", "sits beside",
            // v43: dropped onto a seat
            "धम्म से बैठ", "धप से बैठ", "flops down", "plops down", "slumps down", "sinks into"};
    /** v34: the scene and beat being directed — to look ahead (does a character who walks off speak again in this scene?). */
    private Story.Scene dirScene;
    private int dirBeat = -1;
    private int exits, crossings, approaches, returns;
    /** v34: where each character stood before walking off (they come back to it). */
    private final java.util.Map<Film.Actor, Float> leftFrom = new java.util.IdentityHashMap<Film.Actor, Float>(), goneAt = new java.util.IdentityHashMap<Film.Actor, Float>();
    /** v34: coming back after leaving. */
    static final String[] RETURN = {"comes back", "came back", "come back", "returns", "returned", "is back", "walks back in", "runs back in",
            "वापस आ", "लौट आ", "लौटकर आ", "लौटती है", "लौटता है", "लौटते हैं", "वापस लौट"};

    /** v34: leaving the place (walking or running off, going home). */
    static final String[] LEAVE = {"walks home", "walked home", "goes home", "went home", "heads home", "runs home", "ran home", "walks away", "walked away",
            "runs away", "ran away", "goes away", "went away", "walks off", "walked off", "runs off", "ran off", "walks out", "walked out", "runs out of",
            "goes out of", "leaves the", "leaves for", "leaves with", "leaves home", "left the", "घर चला", "घर चली", "घर चले", "घर की ओर चल", "घर की तरफ चल",
            "घर लौट", "चला जाता", "चली जाती", "चले जाते", "चला गया", "चली गई", "चली गयी", "चले गए", "भाग जाता", "भाग जाती", "भाग जाते", "भाग गया", "भाग गई",
            "भाग गयी", "भाग गए", "भागने लग", "भाग खड़ा", "भाग खड़ी", "भाग खड़े", "भाग निकल", "दूर चला", "दूर चली", "दूर चले",
            // v39: "भागते हुए पार्क से बाहर जाते हैं"
            "बाहर जाता", "बाहर जाती", "बाहर जाते", "बाहर चला", "बाहर चली", "बाहर चले", "बाहर भाग", "बाहर निकल जा", "से निकल जा",
            // v44: going in (out of the rain, the hail, the dark) — into the house, out of this place
            "अंदर जाता", "अंदर जाती", "अंदर जाते", "अंदर चला", "अंदर चली", "अंदर चले", "अंदर भाग", "अंदर की ओर भाग", "अंदर दौड़", "घर के अंदर", "भीतर चला", "भीतर चली", "भीतर जा",
            "goes inside", "went inside", "runs inside", "ran inside", "hurries inside", "heads inside", "rushes inside", "dashes inside", "goes back inside", "runs indoors",
            "goes indoors", "hurries indoors", "into the house"};
    /** v34: moving across the place (or round it) without a goal named. */
    static final String[] CROSS = {"runs across", "ran across", "walks across", "walked across", "dashes across", "races across", "rushes across",
            "runs around", "ran around", "runs about", "runs round", "walks around", "walks about", "दौड़ता है", "दौड़ती है", "दौड़ते हैं", "दौड़ पड़",
            "दौड़ लगा", "इधर-उधर दौड़", "इधर उधर दौड़", "इधर-उधर भाग", "चक्कर लगा"};
    /** v34: verbs of going on foot (or on wheels) that "home", "away", "across" or "to someone" can follow at a distance. */
    static final String[] WALK_VERBS = {"walks", "walked", "runs", "ran", "heads", "headed", "hurries", "hurried", "wanders", "wandered", "limps", "limped",
            "rolls", "rolled", "wheels", "wheeled", "strolls", "strolled", "trudges", "trudged", "stomps", "stomped", "storms", "stormed", "dashes", "dashed",
            "races", "raced", "rushes", "rushed", "skips", "skipped", "marches", "marched", "tiptoes", "tiptoed",
            "walk", "run", "head", "hurry", "wander", "limp", "roll", "stroll", "rush", "dash", "race", "march"};
    /** v34: going up to someone. */
    static final String[] GO_TO = {"runs to", "ran to", "walks to", "walked to", "goes to", "went to", "comes to", "came to", "runs towards", "runs toward",
            "ran towards", "walks towards", "walks toward", "walks up to", "runs up to", "goes up to", "walks over to", "runs over to", "rushes to", "hurries to",
            "moves to", "steps towards", "के पास जा", "के पास आ", "के पास दौड़", "की ओर दौड़", "की तरफ दौड़", "की ओर भाग", "की तरफ भाग", "के पास पहुँच",
            // v39: the spoken forms of the user's scripts — "खिड़की के पास जाती है", "झूले की तरफ़ दौड़ती है", "पास आकर बैठती है"
            "की तरफ़ दौड़", "की तरफ़ भाग", "की तरफ़ जा", "की तरफ जा", "की तरफ़ चल", "की तरफ चल", "की ओर जा", "की ओर चल", "की ओर बढ़", "की तरफ़ बढ़",
            "की तरफ बढ़", "के पास चल", "के पास भाग", "पास आकर", "पास जाकर", "के करीब जा", "के करीब आ", "के पास पहुंच"};

    /**
     * v34 (the director trained for any story's stage directions, not only the sample's): "Kabir runs across the
     * grass", "he turns away and walks slowly home", "Maya runs to Kabir", "रोहन घर चला जाता है" — the character
     * really moves. Leaving: off the stage on the nearer side and gone (unless they speak again in this scene: then
     * to the edge and still in view); across: to the other side, or back and forth for "around"; up to someone:
     * beside them. Running when the words say so ("runs", "दौड़", "भाग"), slowly when they say so; never faster than
     * the character's weight allows (calmMoves keeps the run under it); walking aids are slowed later (mobility).
     * Nothing moves twice: a sentence another rule has already staged as a move is left as it is; entrances are
     * the entrance rule's.
     */
    private float travelFrom(String s, float t, Film.Actor subj, Film.Actor target, List<Film.Actor> group) {
        if (subj == null || rode) return 0;           // v37: a ride already moved them (on its vehicle)
        // "They all walk home", "Both run off", "सब घर चले जाते हैं": with no names, everyone on the stage
        String head = s.trim().toLowerCase(java.util.Locale.ROOT);
        if (group.size() <= 1 && (head.startsWith("they ") || head.startsWith("both ") || head.startsWith("all ") || head.startsWith("everyone ")
                || head.startsWith("everybody ") || head.startsWith("सब") || head.startsWith("दोनों") || head.startsWith("तीनों") || head.startsWith("चारों")
                || head.startsWith("पाँचों") || head.startsWith("पांचों") || head.startsWith("वे "))) {
            List<Film.Actor> on = new ArrayList<Film.Actor>();
            for (Film.Actor a : seg.actors) if (a.stateAt(t).visible) on.add(a);
            if (on.size() > 1) group = on;
        }
        // v34 (realism): out in the rain without an umbrella, people hurry
        boolean wet = (wOpen[Film.W_RAIN] >= 0 || wOpen[Film.W_STORM] >= 0) && Sets.outdoorSet(seg.set);
        if (Txt.has(s, RETURN)) {
            // back after walking off: in from the side they left by, to where they stood (a change of clothes on the
            // way is already on them: the reveal follows); a door indoors; the others look up and watch them come in
            float d = 0;
            List<Film.Actor> back = new ArrayList<Film.Actor>(group.size() > 1 && (everyone(s) || head.startsWith("they ") || head.startsWith("वे ")) ? group : java.util.Collections.singletonList(subj));
            for (Film.Actor a : back) {
                Film.Key now = a.stateAt(t);
                Float to = leftFrom.get(a);
                if (now.visible || to == null || now.anchor != Film.A_GROUND) continue;
                Float gone = goneAt.get(a);
                // a moment off the stage first (the camera stays on the others: time passes)
                float tr = Math.max(Math.max(t + 0.1f, a.last().t + 0.01f), gone == null ? 0 : gone + 1.0f);
                float from = Math.max(-120, Math.min(1400, now.x));      // just beyond the edge they left by
                if (indoors(seg.set)) door(tr - 0.2f, from < 640 ? -0.6f : 0.6f);
                frameAround(tr - 0.25f, to);
                Film.Key in = a.at(tr);
                in.visible = true; in.x = from; in.moveDur = 0; in.facing = to > from ? 1 : -1; in.backTurned = false;
                boolean run = running(s);
                float dur = Math.min(4.0f, Math.max(0.8f, Math.abs(to - from) / ((run ? 240f : 170f) * weightSpeed(a))));
                Film.Key k = a.at(tr + 0.05f);
                k.x = to; k.moveDur = dur; k.run = run; k.facing = to > from ? 1 : -1;
                watch(a, tr, tr + 0.05f + dur + 0.4f, true);
                leftFrom.remove(a);
                goneAt.remove(a);
                returns++;
                d = Math.max(d, tr - t + 0.05f + dur);
            }
            return d;
        }
        // v39: "comes to the window" is a move for one already on the stage (an entrance only for one who is not)
        boolean goWords = Txt.has(s, GO_TO);
        if (isEntry(s) && !(goWords && subj.stateAt(t).visible)) return 0;
        // the verb and where to may have words between them ("walks slowly home", "runs happily across the grass")
        boolean walkVerb = Txt.hasWord(s, WALK_VERBS);
        boolean leave = leaving(s);
        boolean cross = Txt.has(s, CROSS) || (walkVerb && Txt.hasWord(s, "across", "around", "round", "about"));
        boolean goTo = (goWords || (walkVerb && Txt.hasWord(s, "to", "towards", "toward")))
                && !Txt.has(s, "to know", "to realise", "to realize", "to understand", "to an end", "to life", "to terms", "to sleep", "to bed", "to school");
        // whom they go up to: the one named right after "to" / "towards", or right before "के पास" / "की ओर" — not
        // just anyone the sentence mentions ("Maya runs to the door and Kabir follows" is not a run to Kabir)
        target = goTo ? goalOf(s, subj, group) : null;
        // v39: "उसके पास आकर" — up to the one spoken of (the nearest other on the stage); "खिड़की के पास जाती है" — to a
        // spot of the place (no one named there): a walk across a part of the stage, so the walk is seen
        boolean spot = false;
        if (goTo && target == null && Txt.has(s, "उसके पास", "उनके पास", "उसकी तरफ", "उनकी तरफ", "उसकी ओर", "उनकी ओर", "उसके करीब", "उनके करीब", "to her", "to him", "to them")) {
            float best = 1e9f;
            for (Film.Actor o : seg.actors) {
                if (o == subj || !o.stateAt(t).visible) continue;
                float dd = Math.abs(xAt(o, t) - xAt(subj, t));
                if (dd < best) { best = dd; target = o; }
            }
        }
        if (goTo && target == null && !Txt.has(s, CROSS)) { goTo = false; spot = true; }
        if (!leave && !cross && !goTo && !spot) return 0;
        boolean all = group.size() > 1 && (everyone(s) || head.startsWith("they ") || head.startsWith("वे "));
        List<Film.Actor> who = new ArrayList<Film.Actor>();
        // v40: names joined with a plural verb ("वृंदा और वनुशा कमरे से बाहर जाती हैं"): all of them go, not the first alone
        boolean joint = !all && group.size() > 1 && (joinedPlural(s) || groupSubject);
        if (all) who.addAll(group);
        else if (joint) { for (Film.Actor a : group) if (a != target) who.add(a); }
        else who.add(subj);
        boolean runWords = running(s);
        boolean slow = Txt.has(s, "slowly", "sadly", "धीरे", "उदास");
        boolean turning = Txt.has(s, TURN_AWAY);
        float d = 0;
        backAtEdge = 0;
        for (Film.Actor a : who) {
            // v40: a key this sentence already gave them (a look, a feeling) a moment after it starts: they set off
            // just after it, in time order (they were left standing: "वृंदा और वनुशा … बाहर जाती हैं" took only one)
            float ta = t;
            if (a.last().t > t + 0.149f && a.last().t - t < 2.5f) ta = a.last().t - 0.14f;
            Film.Key now = a.stateAt(ta);
            if (!now.visible || now.anchor != Film.A_GROUND || a.look.kind == Look.BIRD || a.look.mount >= 0 || movedFrom(a, ta)) continue;
            if (a.last().t > ta + 0.149f) continue;          // keys are kept in time order: nothing may come after one already planned
            float x = xAt(a, ta);
            Look lk = lookAt(a, ta);
            boolean aided = lk.aid != Look.AID_NONE || (lk.injury & Look.INJ_LEG) != 0;
            // v34 (realism): a blindfolded character feels the way, slowly, a hand out in front; one without an umbrella
            // hurries through the rain (not one on a walking aid)
            boolean blind = lk.cannotSee();             // v35: blind as well as blindfolded
            boolean cane = lk.aid == Look.AID_STICK;
            boolean run = runWords && !blind;
            if (!run && !blind && !slow && wet && !a.umbrellaAt(lk, ta) && !aided && lk.isHumanoid()) { run = true; hurried++; }
            // a blindfolded walk is careful but not "slowly" on top of it; no walk on the stage lasts more than 4.5 s
            // (a film shortens a long walk; a story that waits ten seconds for one loses its audience)
            float speed = (blind ? 120f : run ? 250f : slow ? 110f : 170f) * weightSpeed(a);
            // v39: one who gets up in the same sentence ("सोफे से उठकर … आता है") walks once on their feet
            float go = ta + 0.15f;
            Film.Key before = a.stateAt(Math.max(seg.t0, ta - 0.02f));
            if (before.body == Pose.SIT || before.body == Pose.KNEEL || before.body == Pose.LIE) go = ta + 1.1f;
            // v41: nobody walks off sitting (or lying): up on their feet first — from the grass after a slide, from a
            // mat, from a bed a moment longer — unless they roll in a wheelchair
            boolean seated = (before.body == Pose.SIT || before.body == Pose.KNEEL || before.body == Pose.LIE) && lk.isHumanoid() && lk.aid != Look.AID_WHEELCHAIR;
            if (seated) {
                Film.Key up = a.at(ta + 0.1f);
                up.body = Pose.STAND; up.seat = -1; up.eyesShut = false;
                if (before.body == Pose.LIE) go = ta + 1.5f;
                stoodToWalk++;
            }
            float walkFrom = seated ? go : ta + 0.15f;
            // v39: "पास आकर बैठती है" — they come first and sit there: the sit this sentence staged waits for the walk
            Film.Key sitAfter = null;
            if (Txt.has(s, "आकर बैठ", "आ कर बैठ", "जाकर बैठ", "जा कर बैठ", "and sits", "and sat")) {
                for (Film.Key kk : a.keys) if (kk.t >= ta - 0.01f && kk.body == Pose.SIT) { sitAfter = kk; break; }
                if (sitAfter != null) { sitAfter.body = Pose.STAND; go = ta + 0.15f; }
            }
            if (spot && !leave) {
                // to a spot of the place: towards the middle of the stage (or the other side), a walk worth seeing;
                // a group keeps its spacing
                float shift = x < 640 ? 300 : -300;
                float dest = Math.max(160, Math.min(1120, x + shift));
                // v40: a walk long enough to be seen as one (several steps, not a slide of a third of a body): the
                // first free spot at least 230 away — toward the middle first, then the other way
                float freeDest = Float.NaN;
                for (float cand : new float[]{x + shift * 1.3f, x + shift, x - shift * 1.3f, x - shift}) {
                    float c = Math.max(160, Math.min(1120, cand));
                    if (Math.abs(c - x) < 230) continue;
                    boolean clear = true;
                    for (Film.Actor o : seg.actors) {
                        if (o == a || (all && who.contains(o)) || !o.stateAt(ta).visible) continue;
                        float ox = xAt(o, ta);
                        if (Math.abs(c - ox) < 140 || (ox - x) * (ox - c) < 0) { clear = false; break; }     // nobody on the spot or in the way
                    }
                    if (clear) { freeDest = c; break; }
                }
                if (!Float.isNaN(freeDest)) dest = freeDest;
                // never onto another one standing there: stop beside them (an arm's length), short of them if need be
                for (int pass = 0; pass < 3; pass++) {
                    boolean moved = false;
                    for (Film.Actor o : seg.actors) {
                        if (o == a || (all && who.contains(o)) || !o.stateAt(ta).visible) continue;
                        float ox = xAt(o, ta);
                        if (Math.abs(dest - ox) < 130) { dest = ox + (x < ox ? -140 : 140); moved = true; }
                    }
                    dest = Math.max(160, Math.min(1120, dest));
                    if (!moved) break;
                }
                if (Math.abs(dest - x) < 60) continue;
                float dur = Math.min(4.0f, Math.max(0.7f, Math.abs(dest - x) / speed));
                if (!all) frameAround(go - 0.1f, x, dest);
                Film.Key k = a.at(go);
                k.x = dest; k.moveDur = dur; k.run = run; k.facing = dest > x ? 1 : -1;
                if (sitAfter != null) { Film.Key sk = a.at(go + dur + 0.05f); sk.body = Pose.SIT; sk.seat = sitAfter.seat; }
                approaches++;
                d = Math.max(d, go - t + dur + (sitAfter != null ? 1.0f : 0.3f));
                continue;
            }
            if (goTo && target != null && target != a && !all) {
                float tx = xAt(target, ta);
                // an arm's length apart, as people stand to talk (not shoulder to shoulder)
                float gap = 155 + (a.look.kind == Look.MONSTER || target.look.kind == Look.MONSTER ? 80 : 0);
                float dest = x < tx ? tx - gap : tx + gap;
                // v41: not onto a third one standing there: the other side of the one approached, else the free spot
                // nearest
                if (standsAt(dest, ta + 1f, a, target)) {
                    float other = x < tx ? tx + gap : tx - gap;
                    if (other > 150 && other < 1130 && !standsAt(other, ta + 1f, a, target)) dest = other;
                    else { List<Film.Actor> ex = new ArrayList<Film.Actor>(); ex.add(a); ex.add(target); dest = freeNear(dest, ta, ex, 110); }
                }
                if (Math.abs(dest - x) < 30) continue;
                float dur = Math.min(4.5f, Math.max(0.6f, Math.abs(dest - x) / speed));
                frameAround(go - 0.1f, x, dest);
                Film.Key k = a.at(go);
                k.x = dest; k.moveDur = dur; k.run = run; k.facing = tx > dest ? 1 : -1;
                if (blind) { if (!cane) a.acts.add(new Film.Act(go, go + dur, Film.G_REACH)); feltWay++; }
                watch(a, go, go + 0.3f + dur, false);
                if (sitAfter != null) { Film.Key sk = a.at(go + dur + 0.05f); sk.body = Pose.SIT; sk.seat = sitAfter.seat; sk.facing = tx > dest ? 1 : -1; }
                approaches++;
                // the whole walk before the story goes on (a listener turned mid-walk would walk backwards)
                d = Math.max(d, go - t + dur + (sitAfter != null ? 1.0f : 0.15f));
            } else if (leave) {
                boolean back = speaksLater(a.c);
                // v39: a group leaves together, by one side (the side most of them stand nearer)
                float side = all || joint ? groupSide(who, ta) : x;
                // v43: indoors, out through the room's door when it has been seen
                Film.Fx roomDoor = seenDoor();
                if (roomDoor != null && indoors(seg.set) && !back) side = roomDoor.x > 0 ? 1000 : 200;
                float edge = side < 640 ? (back ? 150 : -220) : (back ? 1130 : 1500);
                // v41: those who stay near the edge (they speak again later) stand side by side there, not on one spot
                if (back) { edge += (side < 640 ? 1 : -1) * 115 * backAtEdge; backAtEdge++; }
                float start = Math.max(walkFrom, ta + (turning ? 0.7f : 0.25f));           // a turned back reads for a moment before the walk
                float dur = Math.min(4.0f, Math.max(0.8f, Math.abs(edge - x) / ((blind ? 120f : run ? 245f : slow ? 150f : 200f) * weightSpeed(a))));
                // a still frame they walk out of (the camera does not chase them off the stage)
                frameAround(ta + 0.05f);
                Film.Key k = a.at(start);
                k.x = edge; k.moveDur = dur; k.run = run; k.facing = edge > x ? 1 : -1; k.backTurned = false;
                if (blind) { if (!cane) a.acts.add(new Film.Act(start, start + dur, Film.G_REACH)); feltWay++; }
                // v43: out of a door the sentence names, outdoors too (the courtyard's door, the gate) or to stand at it:
                // heard as they open it
                if ((back || !indoors(seg.set)) && Txt.has(s, "दरवाज़ा खोल", "दरवाजा खोल", "दरवाज़ा खुला", "गेट खोल", "opens the door", "opens the gate", "through the door", "out the door"))
                    door(start + 0.1f, edge < 640 ? -0.6f : 0.6f, false);
                if (!back) {
                    // gone once off the stage; a door indoors; the others watch them go and look after them a moment
                    // (the story goes on when they have gone: no key may come before these)
                    Film.Key gone = a.at(start + dur + 0.05f);
                    gone.visible = false;
                    if (indoors(seg.set)) door(start + dur - 0.3f, edge < 640 ? -0.6f : 0.6f, false);
                    if (!all) watch(a, start, start + dur + 0.6f, true);      // v39: a group leaving together watches no one go
                    // once they have gone: a cut to those left behind, still looking after them
                    int nc = seg.cams.size();
                    frameCut(start + dur + 0.1f);
                    // v41: a story cut — the later clean-up passes keep it
                    if (seg.cams.size() > nc) seg.cams.get(seg.cams.size() - 1).keep = true;
                    d = Math.max(d, start - t + dur + 0.6f);
                    leftFrom.put(a, x);
                    goneAt.put(a, gone.t);
                } else {
                    if (!all) watch(a, start, start + dur + 0.3f, false);
                    d = Math.max(d, start - t + dur);
                }
                exits++;
            } else {
                Film.Key k;
                float dur;
                if (Txt.has(s, "around", "about", "round", "इधर-उधर", "इधर उधर", "चक्कर")) {
                    // round the place: out to one side and back
                    float far = x < 640 ? Math.min(1100, x + 360) : Math.max(180, x - 360);
                    dur = Math.min(2.2f, Math.max(0.7f, Math.abs(far - x) / speed));
                    frameAround(ta + 0.05f, x, far);
                    k = a.at(walkFrom); k.x = far; k.moveDur = dur; k.run = run; k.facing = far > x ? 1 : -1;
                    Film.Key k2 = a.at(walkFrom + 0.05f + dur); k2.x = x; k2.moveDur = dur; k2.run = run; k2.facing = far > x ? -1 : 1;
                    dur = dur * 2 + 0.05f + (walkFrom - ta - 0.15f);
                } else {
                    // across: to the other side (a slow walk goes less far)
                    float reach = slow || blind ? 360 : 520;
                    float dest = x < 640 ? Math.min(1100, x + reach) : Math.max(180, x - reach);
                    // v41: never onto another one standing there: on past them to free ground (short of them where
                    // the stage ends)
                    for (int pass = 0; pass < 3; pass++) {
                        boolean moved = false;
                        for (Film.Actor o : seg.actors) {
                            if (o == a || !o.stateAt(ta).visible || o.look == null || !o.look.isHumanoid()) continue;
                            float ox = xAt(o, ta);
                            if (Math.abs(dest - ox) < 130) {
                                float past = ox + (dest > x ? 230 : -230);
                                past = Math.max(180, Math.min(1100, past));
                                dest = Math.abs(past - ox) >= 200 ? past : ox + (dest > x ? -140 : 140);
                                moved = true;
                            }
                        }
                        if (!moved) break;
                    }
                    dur = Math.min(4.5f, Math.max(0.8f, Math.abs(dest - x) / speed));
                    frameAround(ta + 0.05f, x, dest);
                    k = a.at(walkFrom); k.x = dest; k.moveDur = dur; k.run = run; k.facing = dest > x ? 1 : -1;
                    dur += walkFrom - ta - 0.15f;
                }
                if (blind) { if (!cane) a.acts.add(new Film.Act(ta + 0.15f, ta + 0.15f + dur, Film.G_REACH)); feltWay++; }
                watch(a, ta + 0.15f, ta + 0.15f + dur, false);
                crossings++;
                d = Math.max(d, ta - t + dur + 0.3f);
            }
        }
        return d;
    }

    // ------------------------------------------------------------------ v34: realism — where everyone looks, doors, steps, the sky

    private boolean travelFramed;
    private int watchCount, watchTurns, skyReactions, hurried, feltWay, doorsHeard, stepsPlaced;
    /** v35: lines said in sign language; moments a deaf character did not hear (no startle, no turn to a sound behind them). */
    private int signedLines, deafUnheard;
    /** v38: lines said together by a group ("तीनों:"), counted per extra voice. */
    public int chorusLines;
    /** v35: lines that waited until their speaker had walked onto the stage. */
    private int waitedToEnter;

    /**
     * v34 (realism): everyone on the stage looks at what matters now — someone walking in, walking off, coming back,
     * revealed in new clothes, hurt or healed. Their eyes go there (Renderer.gaze reads film.watches); one who stands
     * with their back to it and is free (not speaking, not walking, nothing planned after) turns round to watch, a
     * quick turn. A blindfolded character listens toward it instead.
     */
    private void watch(Film.Actor at, float t0, float t1, boolean turn) {
        if (at == null || seg == null) return;
        for (Film.Actor w : seg.actors) {
            if (w == at) continue;
            Film.Key k = w.stateAt(t0);
            if (!k.visible || k.anchor == Film.A_HIDDEN || w.look == null) continue;
            Look lk = lookAt(w, t0);
            if (lk != null && lk.cannotSee()) { w.acts.add(new Film.Act(t0, Math.min(t1, t0 + 1.2f), Film.G_LISTEN)); continue; }
            float side = Math.signum(xAt(at, (t0 + t1) / 2) - xAt(w, t0));
            // v35: a deaf character does not hear what happens behind them: no look, no turn (they see it when it comes in front)
            if (lk != null && lk.cannotHear() && side != 0 && (side != k.facing || k.backTurned)) { deafUnheard++; continue; }
            film.watches.add(new Film.Watch(w, at, t0, t1));
            watchCount++;
            if (!turn || k.anchor != Film.A_GROUND || k.body == Pose.LIE || k.body == Pose.SIT || k.backTurned || !w.look.isHumanoid()) continue;
            if (side == 0 || side == k.facing) continue;
            if (w.last().t > t0 + 0.01f || movingIn(w, t0, t1) || speakingIn(w, t0, t1)) continue;
            w.acts.add(new Film.Act(t0, t0 + 0.4f, Film.G_TURN));
            Film.Key kt = w.at(t0 + 0.2f);
            kt.facing = side;
            watchTurns++;
        }
    }

    private static boolean movingIn(Film.Actor a, float t0, float t1) {
        for (Film.Key k : a.keys) if (k.moveDur > 0 && k.t < t1 && k.t + k.moveDur > t0) return true;
        return false;
    }

    private static boolean speakingIn(Film.Actor a, float t0, float t1) {
        for (Film.Speak sp : a.speaks) if (sp.t0 < t1 && sp.t1 > t0) return true;
        return false;
    }

    /**
     * v34 (realism): a wide, still frame holding everyone on the stage now and the given points (where a walk starts
     * and ends): someone walking off leaves it, a run across crosses it, someone coming back walks into it.
     */
    private void frameAround(float t, float... xs) { frame(t, 0.5f, xs); }

    /** The same frame as a cut (no glide): the reaction of those left behind once someone has gone. */
    private void frameCut(float t, float... xs) { frame(t, 0f, xs); }

    private void frame(float t, float ease, float[] xs) {
        float minX = 1e9f, maxX = -1e9f, maxH = 0;
        for (Film.Actor o : seg.actors) {
            if (!o.stateAt(t).visible) continue;
            float ox = xAt(o, t);
            if (ox < -100 || ox > 1380) continue;
            minX = Math.min(minX, ox); maxX = Math.max(maxX, ox); maxH = Math.max(maxH, heightOf(o));
        }
        for (float x : xs) { float cx = Math.max(60, Math.min(1220, x)); minX = Math.min(minX, cx); maxX = Math.max(maxX, cx); }
        if (minX > maxX) return;
        float span = maxX - minX + 420;
        float zoom = Math.max(1.0f, Math.min(1.35f, 1280f / span));
        cam(Math.max(seg.t0, t), (minX + maxX) / 2, ground - Math.max(maxH, 200) * 0.55f, zoom, ease);
        travelFramed = true;
    }

    /** v34 (realism): places with doors — a room, a hall, a basement (not a cave, not under the sky). */
    static boolean indoors(int set) { return !Sets.outdoorSet(set) && set != Sets.CAVE_IN && set != Sets.CAVE_MOUTH; }

    /** v34 (realism): a door heard on that side of the stage (-0.6 left .. +0.6 right). */
    private void door(float t, float pan) { door(t, pan, true); }

    /**
     * v34 (realism): a door heard on that side of the stage (-0.6 left .. +0.6 right) as someone comes in (entering) or
     * goes out; v43: and seen there — the room's door at that edge of the picture swings open just before they pass it
     * and shuts behind them.
     */
    private void door(float t, float pan, boolean entering) {
        Film.Sfx x = new Film.Sfx(Film.SFX_DOOR, Math.max(0, t), 1.1f, 0.42f);
        x.pan = pan;
        film.sfx.add(x);
        doorsHeard++;
        if (seg == null || !indoors(seg.set)) return;
        float side = pan < 0 ? -1 : 1;
        roomDoorFrame(side);
        if (entering) swingDoor(side, t - 0.1f, t + 1.4f);
        else swingDoor(side, t - 0.25f, t + 0.75f);
        doorsSeen++;
    }

    int doorsSeen, knocks;

    static final String[] KNOCK = {"खटखटा", "दस्तक", "knocks", "knocking", "knock on", "knocked"};

    /**
     * v43: a door in a direction — knocked at, opened, shut — staged in its own right (it was only a fall-back, lost when
     * anything else in the sentence was staged: "वृंदा उठकर दरवाज़ा खोलती है" stood her up and nothing more). Indoors the
     * room's door is seen: the one named goes to it after whatever else the sentence has them do first, reaches for it,
     * and it swings open or shut; a knock is heard at it — with nobody named too — and those in the room turn to it.
     * Coming in or going out through it is the entrance's or the exit's (they open and shut it themselves).
     */
    private float doorFrom(String s, float t, List<Film.Actor> who) {
        if (seg == null || negated(s)) return 0;
        boolean knock = Txt.has(s, KNOCK);
        if (!knock && !Txt.has(s, "दरवाज़ा", "दरवाजा", "दरवाज़े", "दरवाजे", "किवाड़", "the door", "a door")) return 0;
        // (it opens or shuts by itself — "दरवाज़ा खुलता है", "the door swings open": nobody in the room does it)
        boolean byItself = Txt.has(s, "खुलता", "खुलती", "खुल गया", "खुल जाता", "खुला", "बंद हो", "door opens", "door swings", "door closes", "door shuts", "door slams", "door creaks");
        boolean shut = Txt.has(s, "बंद", "close", "shut", "slam"), open = !shut && Txt.has(s, "खोल", "खुल", "open");
        if (!knock && !shut && !open) return 0;
        if (!knock && (isEntry(s) || leaving(s) || Txt.has(s, "अंदर आ", "बाहर जा", "बाहर चल", "बाहर निकल", "comes in", "walks in", "goes out", "walks out", "steps out"))) return 0;
        boolean in = indoors(seg.set);
        // v43: someone unnamed ("कोई दरवाज़े पर दस्तक देता है"), or one not in the room, knocks from outside: nobody
        // in the room goes to the door for it (it was the last one spoken of who walked over and knocked from inside)
        boolean nobody = byItself || Txt.has(s, "कोई ", "किसी ने", "someone", "somebody") || ScriptParser.mentions(story, s).isEmpty();
        Film.Actor a = null;
        if (!nobody) for (Film.Actor o : who) if (o.look != null && o.look.isHumanoid() && o.stateAt(t).visible) { a = o; break; }
        float side;
        if (a != null) side = doorSide(xAt(a, t));
        else if (seenDoor() != null) side = seenDoor().x;
        else {
            // the door on the side away from where the people in the room are
            float sum = 0; int n = 0;
            for (Film.Actor o : seg.actors) if (o.stateAt(t).visible && o.look != null && o.look.isHumanoid()) { sum += xAt(o, t); n++; }
            side = n > 0 && sum / n >= 640 ? -1 : 1;
        }
        if (in) roomDoorFrame(side);
        // after what the sentence already has them do (standing up from the sofa first)
        float from = t;
        if (a != null) { Film.Key l = a.last(); if (l.t >= t) from = Math.min(t + 3f, l.t + Math.max(0, l.moveDur) + 0.1f); }
        float at = from + 0.1f;
        if (a != null && in) {
            float dx = side < 0 ? 150 : 1130, ax = xAt(a, from), wd = Math.abs(ax - dx) > 40 ? Math.min(2.4f, Math.max(0.6f, Math.abs(ax - dx) / 180f)) : 0;
            if (wd > 0) makeWay(a, dx, from + 0.05f, side);
            Film.Key wk = keyAt(a, from + 0.05f);
            if (wd > 0) { wk.x = dx; wk.moveDur = wd; wk.run = false; }
            wk.facing = side;
            if (wk.body == Pose.SIT || wk.body == Pose.LIE) { wk.body = Pose.STAND; wk.seat = -1; }
            at = from + 0.1f + wd;
            a.acts.add(new Film.Act(at, at + 1.0f, Film.G_REACH));
            if (open && !knock) {
                // the door opened, they step back out of the doorway into the room (to let in whoever is there),
                // still facing it
                float back = dx - side * 250;
                java.util.List<Film.Actor> self = java.util.Collections.singletonList(a);
                if (!freeAt(back, at + 1.1f, self, 84)) back = freeNear(back, at + 1.1f, self, 140);
                Film.Key bk = keyAt(a, at + 1.1f);
                bk.x = back; bk.moveDur = 0.8f; bk.run = false; bk.facing = -side;
                spread(a, bk);
                Film.Key fk = keyAt(a, at + 1.95f); fk.facing = side; spread(a, fk);
            }
        }
        if (knock) {
            Film.Sfx ks = new Film.Sfx(Film.SFX_KNOCK, at + 0.2f, 0.8f, 0.5f);
            ks.pan = side * 0.6f;
            film.sfx.add(ks);
            for (Film.Actor o : seg.actors)
                if (o != a && o.stateAt(at).visible && o.look != null && o.look.isHumanoid()) { Film.Key lk = keyAt(o, at + 0.5f); lk.facing = side; }
            knocks++;
            return at - t + 1.3f;
        }
        Film.Sfx ds = new Film.Sfx(Film.SFX_DOOR, at + 0.2f, 1.0f, Txt.has(s, "slam", "ज़ोर से", "जोर से") ? 0.6f : 0.42f);
        ds.pan = side * 0.6f;
        film.sfx.add(ds);
        if (in) {
            if (shut) closeDoor(side, at + 0.35f);
            else swingDoor(side, at + 0.3f, 1e5f);          // opened, it stays open until someone shuts it
            doorsSeen++;
        }
        return at - t + 1.2f + (a != null && open && !knock && in ? 0.9f : 0);
    }

    /**
     * v43: anyone standing where a goes (x, at t) steps aside into the room (inward from the door on side), so the
     * two never stand on one spot (तरुण went to shut the door where वृंदा stood after opening it).
     */
    private void makeWay(Film.Actor a, float x, float t, float side) {
        for (Film.Actor o : seg.actors) {
            if (o == a || !o.stateAt(t).visible || o.look == null || !o.look.isHumanoid()) continue;
            Film.Key ok = o.stateAt(t);
            if (ok.body != Pose.STAND || Math.abs(xAt(o, t) - x) > 90) continue;
            java.util.List<Film.Actor> self = java.util.Collections.singletonList(o);
            float to = x - side * 190;
            if (!freeAt(to, t + 0.9f, self, 84)) to = freeNear(to, t + 0.9f, self, 160);
            float ox = xAt(o, t);
            Film.Key mk = keyAt(o, t);
            mk.x = to; mk.moveDur = 0.8f; mk.run = false; mk.facing = to > ox ? 1 : -1;
            spread(o, mk);
            Film.Key fk = keyAt(o, t + 0.85f); fk.facing = side; spread(o, fk);
        }
    }

    /** v43: what is carried in the hand, by the words that name it (the first that fits). */
    static final Object[][] CARRIED = {
        {new String[]{"ट्रे में चाय", "चाय की ट्रे", "चाय लेकर", "चाय ले कर", "चाय के कप", "tray of tea", "cup of tea", "cups of tea", "brings tea", "with tea"}, Pose.I_TEA},
        {new String[]{"टोकरी", "basket"}, Pose.I_BASKET},
        {new String[]{"कॉपी", "नोटबुक", "notebook"}, Pose.I_NOTEBOOK},
        {new String[]{"किताब", "पुस्तक", "a book", "the book", "his book", "her book"}, Pose.I_BOOK},
        {new String[]{"थाली", "प्लेट", "plate"}, Pose.I_PLATE},
        {new String[]{"गिलास", "glass of"}, Pose.I_GLASS},
        {new String[]{"कप में", "कप लेकर", "कप लिए", "का कप", "के कप", "प्याला", "प्याली", "mug", "a cup", "the cup"}, Pose.I_CUP},
        {new String[]{"बोतल", "bottle"}, Pose.I_BOTTLE},
        {new String[]{"अख़बार", "अखबार", "newspaper"}, Pose.I_PAPER},
        {new String[]{"झाड़ू", "broom"}, Pose.I_BROOM},
        {new String[]{"फूल", "गुलदस्ता", "flowers", "bouquet", "a flower"}, Pose.I_FLOWER},
    };

    /**
     * v43: a thing carried in the direction ("ट्रे में चाय लेकर आता है", "फूलों की टोकरी लेकर आता है", "हाथ में किताब लिए
     * खड़ा है", "carries a basket"): it is in their hand from now on, until it is given, put down or eaten up.
     */
    private float carryFrom(String s, float t, List<Film.Actor> who) {
        if (seg == null || negated(s)) return 0;
        if (!Txt.has(s, "लेकर", "ले कर", "लिए", "लिये", "हाथ में", "थामे", "carries", "carrying", "holding", "holds a", "holds the", "brings", "bringing", "with a tray"))
            return 0;
        if (Txt.has(s, "छाता", "छतरी", "umbrella")) return 0;
        int item = Pose.I_NONE;
        for (Object[] r : CARRIED) if (Txt.has(s, (String[]) r[0])) { item = (Integer) r[1]; break; }
        if (item == Pose.I_NONE) return 0;
        for (Film.Actor a : who) {
            if (a.look == null || !a.look.isHumanoid() || !a.stateAt(t).visible) continue;
            Film.Key k = keyAt(a, t + 0.05f);
            if (k.holdR != Pose.I_NONE && k.holdL == Pose.I_NONE) k.holdL = item; else k.holdR = item;
            spread(a, k);
            carried++;
        }
        return 0;
    }
    int carried;

    /** v43: the hands empty again (a thing given away or put down) from t. */
    static void handsFree(Film.Actor a, float t) {
        Film.Key k = keyAt(a, t);
        if (k.holdR == Pose.I_NONE && k.holdL == Pose.I_NONE) return;
        k.holdR = Pose.I_NONE; k.holdL = Pose.I_NONE;
        spread(a, k);
    }

    /** v43: the room's door already in this part (FX_DOOR kind 0), or null. */
    private Film.Fx seenDoor() {
        if (seg != null) for (Film.Fx f : seg.fx) if (f.type == Film.FX_DOOR && f.kind == 0) return f;
        return null;
    }

    /** v43: the side of the room's door for one at x: the door already seen in this part, else the nearer edge. */
    private float doorSide(float x) {
        if (seg != null) for (Film.Fx f : seg.fx) if (f.type == Film.FX_DOOR && f.kind == 0) return f.x;
        return x < 640 ? -1 : 1;
    }

    /** v43: the room's door at that edge of the picture, there through the whole part. */
    private void roomDoorFrame(float side) {
        for (Film.Fx f : seg.fx) if (f.type == Film.FX_DOOR && f.kind == 0 && f.x == side) return;
        Film.Fx f = fx(Film.FX_DOOR, seg.t0, 1e6f, side, 0, null, null);
        f.kind = 0;
    }

    /** v43: the door at that side is shut at t: the swing open then ends there (with none open, it stood ajar from the part's start). */
    private void closeDoor(float side, float t) {
        Film.Fx open = null;
        for (Film.Fx f : seg.fx) if (f.type == Film.FX_DOOR && f.kind == 1 && f.x == side && f.t0 <= t && (open == null || f.t0 > open.t0)) open = f;
        if (open == null) { open = fx(Film.FX_DOOR, seg.t0 - 2f, t + 0.6f, side, 0, null, null); open.kind = 1; }
        open.t2 = Math.max(open.t0 + 0.5f, t); open.t1 = open.t2 + 0.6f;
    }

    /** v43: the door at that side swings open from open and shut from close (one swing with any it meets). */
    private void swingDoor(float side, float open, float close) {
        for (Film.Fx f : seg.fx) {
            if (f.type != Film.FX_DOOR || f.kind != 1 || f.x != side) continue;
            if (open <= f.t2 + 0.5f && close >= f.t0 - 0.5f) { f.t0 = Math.min(f.t0, open); f.t2 = Math.max(f.t2, close); f.t1 = f.t2 + 0.6f; return; }
        }
        Film.Fx f = fx(Film.FX_DOOR, open, close + 0.6f, side, 0, null, null);
        f.kind = 1; f.t2 = close;
    }

    /**
     * v34 (realism): rain (or snow) begins while they are out in it — everyone looks up at the sky; then whoever has
     * no umbrella covers the head with the hands (in snow: a shiver). Returns the time it takes.
     */
    private float skyReaction(float t, boolean snow) {
        int n = 0;
        for (Film.Actor a : seg.actors) {
            Film.Key k = a.stateAt(t);
            if (!k.visible || k.anchor != Film.A_GROUND || k.body == Pose.LIE || a.look == null || !a.look.isHumanoid()) continue;
            Look lk = lookAt(a, t);
            a.acts.add(new Film.Act(t + 0.25f, t + 1.05f, Film.G_LOOK_UP));
            if (snow) a.acts.add(new Film.Act(t + 1.05f, t + 2.6f, Film.G_SHIVER));
            else if (!a.umbrellaAt(lk, t)) a.acts.add(new Film.Act(t + 1.05f, t + 2.8f, Film.G_COVER_HEAD));
            n++;
        }
        skyReactions += n;
        return n > 0 ? 2.0f : 0;
    }

    /**
     * v34 (realism): footsteps are heard where the walker is — from the side the walk starts on to the side it ends on
     * — fading as they walk off the stage and growing as they come on; a walking stick, crutches, a wheelchair and
     * anklets the same.
     */
    private void placeSteps() {
        for (Film.Sfx x : film.sfx) {
            boolean step = x.type == Film.SFX_STEPS || x.type == Film.SFX_STEPS_HARD || x.type == Film.SFX_STEPS_RUN || x.type == Film.SFX_STICK
                    || x.type == Film.SFX_CRUTCH || x.type == Film.SFX_WHEELCHAIR || x.type == Film.SFX_ANKLET || x.type == Film.SFX_STEPS_LIMP;
            if (!step || !Float.isNaN(x.pan1)) continue;
            Film.Seg sg = film.segAt(x.t + 0.01f);
            if (sg == null) continue;
            Film.Actor best = null;
            Film.Key bk = null;
            float bestO = 0.05f;
            for (Film.Actor a : sg.actors) for (Film.Key k : a.keys) {
                if (k.moveDur <= 0) continue;
                float o = Math.min(k.t + k.moveDur, x.t + x.dur) - Math.max(k.t, x.t);
                if (o > bestO) { bestO = o; best = a; bk = k; }
            }
            if (best == null) continue;
            float x0 = xAt(best, Math.max(bk.t, x.t) + 0.02f), x1 = bk.x;
            x.pan = panAt(x0);
            x.pan1 = panAt(x1);
            if (x1 < 0 || x1 > 1280) x.gain1 = 0.25f;
            if (x0 < 0 || x0 > 1280) x.gain0 = 0.3f;
            stepsPlaced++;
        }
    }

    /** Where a stage position is heard: -0.6 (left) .. +0.6 (right), as Mixer.panOf places a voice. */
    static float panAt(float x) { return Math.max(-0.6f, Math.min(0.6f, (x - 640f) / 640f * 0.5f)); }

    /** v34: the sentence has someone leave ("walks slowly home", "runs off", "घर चला जाता है"); "off" only right after the verb ("takes off her hat" is not leaving). */
    static boolean leaving(String s) {
        // v43: a thing goes off ("ज़ोरदार शॉट मारता है और गेंद दूर चली जाती है", "the ball flies away"): nobody leaves
        if (Txt.has(s, "गेंद दूर", "गेंद चली", "गेंद उड़", "पतंग उड़", "पतंग चली", "गुब्बारा उड़", "ball flies", "ball goes", "ball rolls", "ball sails",
                "kite flies", "balloon flies", "balloon floats")) return false;
        if (Txt.has(s, LEAVE)) return true;
        if (Txt.hasWord(s, WALK_VERBS) && Txt.hasWord(s, "home", "away")) return true;
        for (String v : WALK_VERBS) if (Txt.has(s, v + " off")) return true;
        return Txt.hasWord(s, "goes", "went") && Txt.hasWord(s, "home", "away");
    }

    /** v34: a word for everyone in the sentence ("all", "both", "together", "सब", "दोनों") — whole words only ("small" is not "all"). */
    static boolean everyone(String s) {
        return Txt.has(s, "सब ", "सभी", "दोनों", "तीनों", "चारों", "पाँचों", "पांचों") || Txt.hasWord(s, "all", "both", "everyone", "together");
    }

    /** v34: running words ("runs", "rushed", "दौड़", "भाग", "तेज़ी से") — whole English words ("grace" is not "race"). */
    static boolean running(String s) {
        return Txt.has(s, "दौड़", "भाग", "तेज़ी से") || Txt.hasWord(s, "run", "runs", "ran", "running", "rush", "rushes", "rushed", "dash", "dashes", "dashed",
                "race", "races", "raced", "hurry", "hurries", "hurried", "sprint", "sprints", "sprinted");
    }

    /** v34: the one named right after "to" / "towards" / "up to" (English), or right before "के पास" / "की ओर" / "की तरफ" (Hindi). */
    private static Film.Actor goalOf(String s, Film.Actor subj, List<Film.Actor> group) {
        String n = Txt.norm(s).toLowerCase(java.util.Locale.ROOT);
        for (Film.Actor o : group) {
            if (o == subj) continue;
            for (String al : o.c.aliases) {
                if (al == null || al.length() < 2) continue;
                String a = Txt.norm(al).toLowerCase(java.util.Locale.ROOT);
                for (int i = n.indexOf(a); i >= 0; i = n.indexOf(a, i + 1)) {
                    String before = n.substring(Math.max(0, i - 12), i), after = n.substring(Math.min(n.length(), i + a.length()));
                    if (before.endsWith("to ") || before.endsWith("towards ") || before.endsWith("toward ") || before.endsWith("to the ")) return o;
                    if (after.startsWith(" के पास") || after.startsWith(" की ओर") || after.startsWith(" की तरफ") || after.startsWith(" के निकट")
                            || after.startsWith("जी के पास") || after.startsWith("जी की ओर")) return o;
                }
            }
        }
        return null;
    }

    /** True when a move of this actor already starts at t or later (another rule staged this sentence). */
    private static boolean movedFrom(Film.Actor a, float t) {
        for (Film.Key k : a.keys) if (k.t >= t - 0.01f && k.moveDur > 0) return true;
        return false;
    }

    /**
     * True when the character speaks, or is named in an action, later in the scene being directed — unless the story
     * first brings them back ("comes back", "वापस आती है"): then they may leave for good and return.
     */
    private boolean speaksLater(Story.CharacterDef c) {
        if (dirScene == null) return false;
        for (int i = dirBeat + 1; i < dirScene.beats.size(); i++) {
            Story.Beat b = dirScene.beats.get(i);
            if (b.type == Story.Beat.DIALOGUE && b.speaker == c) return true;
            if (b.type == Story.Beat.DIRECTION && ScriptParser.mentions(story, b.text).contains(c)) return !Txt.has(b.text, RETURN);
        }
        return false;
    }

    /** v34: words for turning the back to the camera (turning away, walking away, leaving). */
    static final String[] TURN_AWAY = {"पीठ फेर", "मुँह फेर", "मुंह फेर", "पीठ करके", "पीठ कर के", "पलटकर चल", "मुड़कर चल", "चला जाता", "चली जाती", "चले जाते",
            "चला गया", "चली गई", "चले गए", "दूर चला", "दूर चली", "turns away", "turned away", "turns his back", "turns her back", "turns their back",
            "walks away", "walked away", "leaves", "goes away", "back to the camera", "back to us"};

    static final String[] STAND_UP = {"उठ खड़", "उठकर खड़", "खड़ा हो गया", "खड़ी हो गई", "खड़े हो गए", "खड़ी हो गयी", "उठ गया", "उठ गई", "उठ गए",
            // v39: "सोफे से उठकर खिड़की के पास आता है" — getting up on the way somewhere (उठाकर, lifting, is another word)
            "उठकर", "उठ कर", "उठता है", "उठती है", "उठते हैं", "उठ जाता", "उठ जाती", "उठ जाते", "उठी और", "उठे और",
            // v41: "तुरंत खड़ा हो जाता है"
            "खड़ा हो जाता", "खड़ी हो जाती", "खड़े हो जाते", "खड़ा होता है", "खड़ी होती है", "खड़े होते हैं", "खड़ा हुआ और", "खड़ी हुई और",
            "stands up", "standing up", "gets to his feet", "gets to her feet", "gets back up", "jumps up", "springs up",
            "stood up", "stands up", "got up", "gets up", "rose to", "uth khada"};
    static final String[] LIE_DOWN = {"लेट गया", "लेट गई", "लेट गए", "लेट जाता", "लेट जाती", "लेटता", "लेटती", "सो गया", "सो गई", "सो गए", "सो जाता", "सो जाती",
            "सो रहा", "सो रही", "सो रहे", "सोता है", "सोती है", "सोने चल", "lay down", "lies down", "lie down", "fell asleep", "falls asleep", "goes to sleep",
            "went to sleep", "is asleep", "sleeping", "goes to bed", "went to bed", "gets into bed", " sleeps", " naps", "takes a nap", "dozes off",
            // v43: lying there already
            "लेटा हुआ", "लेटी हुई", "लेटे हुए", "लेटा है", "लेटी है", "लेटे हैं", "is lying", "lies on", "lying on"};
    /** v35: sleep (eyes shut, slow breathing, under a blanket in a bed) rather than only lying down. */
    static final String[] SLEEP = {"सो ", "सोता", "सोती", "सोने", "नींद", "asleep", "sleep", "goes to bed", "went to bed", "gets into bed"};
    /** v35: waking up — the eyes open, they sit up and stretch (and get up when the story says so). */
    static final String[] WAKE = {"जाग गया", "जाग गई", "जाग गए", "जाग जाता", "जाग जाती", "जागता", "जागती", "जाग उठ", "नींद खुल", "आँख खुल", "आंख खुल", "wakes up",
            "woke up", "awakens", "awoke", "wakes"};
    static final String[] THRONE = {"सिंहासन", "राजगद्दी", "गद्दी पर", "throne"};
    /** v35: a chair (with a back), a sofa, a bed (a charpai is a bed) — each its own furniture. */
    static final String[] CHAIR = {"कुर्सी", "chair", "armchair"};
    static final String[] SOFA = {"सोफ़", "सोफा", "सोफे", "दीवान", "sofa", "couch", "settee"};
    static final String[] BED = {"बिस्तर", "पलंग", "चारपाई", "खाट", "bed", "cot", "mattress"};
    static final String[] STOOL = {"मूढ़ा", "मूढ़े", "चौकी", "बेंच", "stool", "bench"};
    static final String[] ROCK = {"चट्टान", "पत्थर पर", "rock", "boulder"};
    static final String[] FLOOR = {"ज़मीन पर", "जमीन पर", "धरती पर", "फर्श पर", "दरी", "चटाई", "floor", "ground", "on the grass", "घास पर"};
    static final String[] BOW = {"प्रणाम", "नमस्ते", "नमस्कार", "झुककर", "सिर झुका", "bow", "bowed", "namaste", "pranam"};
    static final String[] WAVE = {"हाथ हिला", "टाटा", "wave", "waved", "waving"};
    static final String[] NOD = {"हाँ में सिर", "सिर हिलाकर हाँ", "nodded", "nods"};
    static final String[] TURN = {"पीछे मुड़", "मुड़कर", "मुड़ गया", "मुड़ गई", "turned around", "turns around", "turned back"};

    /** v38: small creatures and things that settle on something — "the butterfly sits on a bud" is not the girl sitting down. */
    static final String[] SETTLERS = {"तितली", "तितलियाँ", "चिड़िया", "पंछी", "पक्षी", "कबूतर", "तोता", "मैना", "गौरैया", "कौआ", "कौवा", "मक्खी", "मधुमक्खी", "भौंरा",
            "जुगनू", "पत्ता", "पत्ती", "धूल", "butterfly", "butterflies", "bird", "sparrow", "pigeon", "parrot", "crow", "bee", "fly ", "firefly", "leaf", "dust"};

    /**
     * v38: true when the one who sits in this sentence is a creature or a thing named before the character (or
     * with no character named at all): its sitting is not the character's.
     */
    public static boolean creatureSits(String s, Story.CharacterDef c) {
        int creature = -1;
        String low = s.toLowerCase(java.util.Locale.ROOT);
        for (String w : SETTLERS) { int i = low.indexOf(w); if (i >= 0 && (creature < 0 || i < creature)) creature = i; }
        if (creature < 0) return false;
        if (c == null) return true;
        String[] names = {c.displayName, c.label, c.shown()};
        int named = -1;
        for (String n : names) {
            if (n == null || n.length() == 0) continue;
            int i = low.indexOf(n.toLowerCase(java.util.Locale.ROOT));
            if (i < 0 && n.indexOf(' ') > 0) i = low.indexOf(n.substring(n.lastIndexOf(' ') + 1).toLowerCase(java.util.Locale.ROOT));
            if (i >= 0 && (named < 0 || i < named)) named = i;
        }
        return named < 0 || creature < named;
    }

    /** Sitting down, getting up, lying down, bowing, waving, nodding, turning — for the subject (or the whole group). */
    /** v39: the sentence being staged has a group as its subject (set by sentence()). */
    private boolean groupSubject;
    static final boolean DEBUG_GROUPS = System.getProperty("kahani.debugGroups") != null;
    /** v39: sentences whose subject was a group ("तीनों …", "चारों …"). */
    public int groupSentences;

    /** v39: a group word anywhere in a sentence: how many ("तीनों" 3, "चारों" 4), 0 = all of them ("सब", "सभी"), -1 = none. */
    static int groupWordCount(String s) {
        String[][] nums = {{"दोनों", "2"}, {"तीनों", "3"}, {"चारों", "4"}, {"पाँचों", "5"}, {"पांचों", "5"}, {"छहों", "6"}, {"both", "2"}, {"all three", "3"},
                {"all four", "4"}, {"all five", "5"}, {"the three of them", "3"}, {"the four of them", "4"}, {"the two of them", "2"}};
        for (String[] n : nums) if (ScriptParser.wordIn(s, n[0]) && !objectOf(s, n[0]) && !thingsAfter(s, n[0])) return Integer.parseInt(n[1]);
        // "सब / सभी" as the subject: first in the sentence, or "सब लोग", "सब मिलकर" (not "सब कुछ", not "सारे फूल")
        String h = Txt.norm(s).trim().replaceFirst("^(फिर|तब|और|अब|then|and)\\s+", "");
        for (String w : new String[]{"सब", "सभी"}) {
            String x = Txt.norm(w);
            if ((h.startsWith(x + " ") || h.equals(x) || Txt.has(s, w + " लोग", w + " मिलकर", w + " एक साथ", w + " बच्चे")) && !objectOf(s, w) && !Txt.has(s, w + " कुछ", "सबकुछ")) return 0;
        }
        if (Txt.hasWord(s, "everyone", "everybody", "all of them")) return 0;
        return -1;
    }

    static String groupWordRe() { return Txt.norm("दोनों") + "|" + Txt.norm("तीनों") + "|" + Txt.norm("चारों") + "|" + Txt.norm("पाँचों") + "|" + Txt.norm("पांचों"); }

    /** v39: "चारों तरफ", "दोनों हाथों", "तीनों solar flowers": the number counts things or sides, not people. */
    static boolean thingsAfter(String s, String w) {
        String n = Txt.norm(s), x = Txt.norm(w);
        int i = n.indexOf(x);
        if (i < 0) return false;
        String[] after = n.substring(i + x.length()).trim().split("[\\s,।.!?]+");
        if (after.length == 0 || after[0].length() == 0) return false;
        String next = after[0];
        if (next.matches("[a-z].*") && !next.equals("and")) return true;            // "तीनों solar flowers"
        String[] things = {"तरफ", "ओर", "हाथ", "हाथों", "आँखें", "आँखों", "आंखें", "आंखों", "पैर", "पैरों", "कान", "कानों", "गाल", "गालों", "फूल", "फूलों",
                "पेड़", "दरवाज़े", "दरवाजे", "कप", "कपों", "किताबें", "चीज़ें", "चीजें", "बातें", "दिन", "रात", "बार", "रंग", "चीज़ों", "कोने", "दीवारें", "पहिये",
                "sides", "hands", "eyes", "cups", "times"};
        for (String t : things) if (next.equals(Txt.norm(t))) return true;
        return false;
    }

    /** v39: the group word is the object, not the subject ("राजू दोनों को देखता है", "सब से छोटा"). */
    static boolean objectOf(String s, String w) {
        String n = Txt.norm(s), x = Txt.norm(w);
        int i = n.indexOf(x);
        while (i >= 0) {
            String after = n.substring(Math.min(n.length(), i + x.length())).trim();
            boolean obj = after.startsWith("को") || after.startsWith("से") || after.startsWith("के ") || after.startsWith("की ") || after.startsWith("का ") || after.startsWith("में ");
            if (!obj) return false;
            i = n.indexOf(x, i + 1);
        }
        return true;
    }

    /** v38: two or more names joined ("और", ",", "and") with a plural verb — the sentence is about all of them. */
    static boolean joinedPlural(String s) {
        boolean joined = Txt.has(s, " और ", ",", " तथा ", " एवं ") || Txt.hasWord(s, "and");
        boolean plural = Txt.has(s, "े हैं", "ी हैं", "ें हैं", "े थे", "ी थीं", "े गए", "ी गईं", "ते हैं", "ती हैं", "ते थे", "ती थीं", "े रहे", "ी रहीं")
                || Txt.hasWord(s, "are", "were", "they", "both", "together", "sit", "stand", "run", "walk", "lie");
        return joined && plural;
    }

    private float postureFrom(String s, float t, Film.Actor subj, List<Film.Actor> group) {
        if (subj == null) return 0;
        float d = 0;
        List<Film.Actor> who = group != null && group.size() > 1 && Txt.has(s, "सब", "सभी", "दोनों", "तीनों", "all", "both", "everyone") ? group : null;
        // v38: names joined with a plural verb ("पापा, सिया और परी सोफ़े पर बैठे हैं", "Maya and Kabir sit down"): all of them
        if (who == null && group != null && group.size() > 1 && (joinedPlural(s) || groupSubject)) who = group;
        if (who == null) { who = new ArrayList<Film.Actor>(); who.add(subj); }
        boolean birdLike = subj.look.kind == Look.MONKEY || subj.look.kind == Look.BIRD;
        if (Txt.has(s, TURN_AWAY)) {
            // v34: the back is to the camera for a while (or until the character has gone): the back picture is used
            boolean leaves = Txt.has(s, "चला जा", "चली जा", "चले जा", "चला गया", "चली गई", "चले गए", "walks away", "leaves", "goes away", "walked away") || leaving(s);
            for (Film.Actor a : who) {
                Film.Key k = a.at(t + 0.1f);
                k.backTurned = true;
                if (!leaves) { Film.Key k2 = a.at(t + 2.4f); k2.backTurned = false; }
            }
            d = Math.max(d, 0.8f);
        }
        // v44: down on the knees ("घुटनों के बल बैठकर प्रणाम करती है", "kneels before the queen") — a held posture;
        // the bow or the prayer of the same sentence is made from the knees; they rise at a later "उठ" / "rises"
        if (Txt.has(s, KNEEL_WORDS) && !negated(s) && !birdLike && !Txt.has(s, "गिर", "falls", "collapses")) {
            for (Film.Actor a : who) {
                if (a.look == null || !a.look.isHumanoid() || a.look.aid == Look.AID_WHEELCHAIR) continue;
                Film.Key now = a.stateAt(t);
                if (now.body == Pose.KNEEL) continue;
                float tk = t;
                if (now.body == Pose.SIT || now.body == Pose.LIE) { Film.Key up = a.at(t); up.body = Pose.STAND; up.seat = -1; tk = t + 0.6f; }
                Film.Key k = a.at(tk); k.body = Pose.KNEEL;
                kneels++;
            }
            d = Math.max(d, 1.2f);
        }
        // v35: waking — the eyes open; one lying sits up (0.9 s) and stretches with a yawn
        if (Txt.has(s, WAKE) && !birdLike) {
            for (Film.Actor a : who) {
                Film.Key k0 = a.stateAt(t);
                Film.Key k = a.at(t + 0.1f); k.eyesShut = false;
                if (k0.body == Pose.LIE) {
                    Film.Key up = a.at(t + 0.5f); up.body = Pose.SIT; up.eyesShut = false;
                    a.acts.add(new Film.Act(t + 1.5f, t + 3.0f, Film.G_STRETCH));
                    film.sfx.add(sfxAt(Film.SFX_BED, t + 0.5f, 1.0f, 0.35f, a));
                    wakings++;
                    d = Math.max(d, 3.1f);
                } else d = Math.max(d, 0.8f);
            }
        }
        if (playSentence != 0) {
            // v41: on the swing or the slide: the seat, the getting up and the lying down are the play's (playFrom)
        } else if (Txt.has(s, STAND_UP) && !Txt.has(s, "उठकर बैठ", "उठ कर बैठ", "उठ बैठ", "sits up", "sat up")) {
            for (Film.Actor a : who) {
                // v35: one lying sits up first (0.9 s), then stands; from the floor, a sofa or a bed the rise takes a second.
                // When the same sentence woke them, they stand after sitting up and stretching on the bed
                float tt = t + 0.1f;
                if (a.last().t > t) tt = Math.max(tt, a.last().t + 0.4f);
                for (Film.Act x : a.acts) if (x.type == Film.G_STRETCH && x.t1 > tt && x.t0 < tt + 1f) tt = Math.max(tt, x.t1);
                Film.Key k0 = a.stateAt(tt);
                if (k0.body == Pose.LIE) { Film.Key up = a.at(tt); up.body = Pose.SIT; up.eyesShut = false; tt += 1.0f; }
                int was = k0.body == Pose.SIT || k0.body == Pose.LIE ? Renderer.seatOf(a, tt - 0.01f) : -1;
                Film.Key k = a.at(tt);
                if (k.body == Pose.SIT || k.body == Pose.KNEEL || k.body == Pose.LIE) { k.body = Pose.STAND; k.eyesShut = false; }
                if (was == Film.SEAT_CHAIR || was == Film.SEAT_SOFA || was == Film.SEAT_BED || was == Film.SEAT_STOOL) {
                    film.sfx.add(sfxAt(was == Film.SEAT_SOFA ? Film.SFX_SOFA : was == Film.SEAT_BED ? Film.SFX_BED : Film.SFX_CHAIR, tt, 0.7f, 0.3f, a));
                }
                if (was >= 0) risings++;
                d = Math.max(d, tt - t + 1.0f);
            }
            d = Math.max(d, 1.0f);
        } else if (Txt.has(s, SIT_DOWN) && !birdLike && !Txt.has(s, "कंधे पर", "नाक पर", "सिर पर", "पीठ पर", "डाल पर", "shoulder") && vehicleIn(s, false) == 0
                && !creatureSits(s, subj.c) && !Txt.has(s, KNEEL_WORDS)) {
            int seat = Txt.has(s, THRONE) ? Film.SEAT_THRONE : Txt.has(s, SOFA) ? Film.SEAT_SOFA : Txt.has(s, BED) ? Film.SEAT_BED : Txt.has(s, CHAIR) ? Film.SEAT_CHAIR
                    : Txt.has(s, STOOL) ? Film.SEAT_STOOL : Txt.has(s, ROCK) ? Film.SEAT_ROCK : Txt.has(s, FLOOR) ? Film.SEAT_FLOOR : -2;
            // v39: "पास आकर बैठती है" — they come first (the walk is staged after this rule) and sit there: never at the scene's start
            boolean comeThenSit = Txt.has(s, "आकर बैठ", "आ कर बैठ", "जाकर बैठ", "जा कर बैठ", "and sits", "and sat")
                    || COME_THEN_SIT.matcher(s).find();     // v44: "दरबार में आकर सिंहासन पर बैठती है"
            for (Film.Actor a : who) {
                // v41: "from the start" only for one with nothing staged yet in this part (not under a run already made)
                float ta = comeThenSit ? t : tsFor(a, t);
                // v41: one still walking in or across ("ट्रे लेकर आती है और … बैठ जाती है") sits once there, never on the way
                float arrive = arrivalAfter(a, ta);
                if (arrive > ta) ta = arrive + 0.1f;
                // v43: one lying in bed who "उठ बैठता है" sits up in the bed (not on a chair from nowhere)
                Film.Key was0 = a.stateAt(ta);
                int lyingOn = was0.body == Pose.LIE ? Renderer.seatOf(a, ta) : -1;
                Film.Key k = a.at(ta);
                k.body = Pose.SIT;
                int st = seat == -2 && (lyingOn == Film.SEAT_BED || lyingOn == Film.SEAT_SOFA) ? lyingOn : seat;
                if (st == -2) {
                    // a king or queen in their hall sits on the throne; others on a stool, or on the ground outdoors
                    boolean royal = Txt.has(a.c.displayName + " " + a.c.fullName + " " + a.c.description, "राजा", "रानी", "महाराज", "king", "queen");
                    st = royal && (seg.set == Sets.HALL || seg.set == Sets.COURTYARD) ? Film.SEAT_THRONE : Sets.outdoorSet(seg.set) ? Film.SEAT_FLOOR
                            : seg.set == Sets.ROOM ? Film.SEAT_CHAIR : Film.SEAT_STOOL;          // v35: a room of today has chairs
                }
                k.seat = st;
                // v35: the furniture is heard as they sit (a chair's creak, a sofa's soft thump, a bed's frame)
                if (st == Film.SEAT_CHAIR || st == Film.SEAT_SOFA || st == Film.SEAT_BED || st == Film.SEAT_STOOL)
                    film.sfx.add(sfxAt(st == Film.SEAT_SOFA ? Film.SFX_SOFA : st == Film.SEAT_BED ? Film.SFX_BED : Film.SFX_CHAIR, ta + 0.45f, 0.7f, 0.3f, a));
                sittings++;
                // the seat is furniture: it stays in the place after they get up
                if (st >= Film.SEAT_STOOL) {
                    boolean have = false;
                    for (Film.Fx f : seg.fx) if (f.type == Film.FX_SEAT && f.a == a) have = true;
                    if (!have) { Film.Fx f = fx(Film.FX_SEAT, seg.t0, 1e6f, xAt(a, ta), 0, a, null); f.kind = st; }
                }
            }
            d = Math.max(d, estab ? 0 : 1.0f);
        } else if (Txt.has(s, LIE_DOWN) && !birdLike) {
            // v35: lying down is a movement, not a fall: they sit down first (on the bed, on the ground), then lie back
            // (0.9 s); asleep, the eyes close, the breath slows and in a bed a blanket covers them
            boolean sleep = Txt.has(" " + s, SLEEP);
            // v43: on the sofa when the sentence says so ("थककर सोफ़े पर ही सो जाता है", "lies down on the sofa")
            boolean sofa = Txt.has(s, SOFA) && !Txt.has(s, BED);
            boolean bed = !sofa && (Txt.has(s, BED) || (sleep && !Sets.outdoorSet(seg.set)));
            for (Film.Actor a : who) {
                if (!a.look.isHumanoid()) { Film.Key k = a.at(t + 0.2f); k.body = Pose.LIE; continue; }
                Film.Key k0 = a.stateAt(t);
                float tl = t + 0.2f;
                if (k0.body != Pose.SIT && k0.body != Pose.LIE) {
                    Film.Key sk = a.at(tl);
                    sk.body = Pose.SIT; sk.seat = bed ? Film.SEAT_BED : sofa ? Film.SEAT_SOFA : Film.SEAT_FLOOR;
                    tl += 1.0f;
                }
                if (sofa) {
                    boolean have = false;
                    for (Film.Fx f : seg.fx) if (f.type == Film.FX_SEAT && f.kind == Film.SEAT_SOFA && Math.abs(f.x - xAt(a, t)) < 120) have = true;
                    if (!have) { Film.Fx f = fx(Film.FX_SEAT, seg.t0, 1e6f, xAt(a, t), 0, a, null); f.kind = Film.SEAT_SOFA; }
                    film.sfx.add(sfxAt(Film.SFX_SOFA, tl - 0.2f, 0.8f, 0.3f, a));
                }
                if (bed) {
                    boolean have = false;
                    for (Film.Fx f : seg.fx) if (f.type == Film.FX_SEAT && f.a == a) have = true;
                    if (!have) { Film.Fx f = fx(Film.FX_SEAT, seg.t0, 1e6f, xAt(a, t), 0, a, null); f.kind = Film.SEAT_BED; }
                    film.sfx.add(sfxAt(Film.SFX_BED, tl - 0.2f, 1.0f, 0.3f, a));
                }
                Film.Key k = a.at(tl);
                k.body = Pose.LIE;
                if (bed) k.seat = Film.SEAT_BED;
                else if (sofa) k.seat = Film.SEAT_SOFA;
                if (sleep) {
                    Film.Key ks = a.at(tl + 0.9f);
                    ks.eyesShut = true;
                    film.sfx.add(sfxAt(Film.SFX_SLEEP, tl + 1.2f, 8f, 0.25f, a));
                    sleepers++;
                }
                d = Math.max(d, tl - t + (sleep ? 1.6f : 1.0f));
            }
            d = Math.max(d, 1.2f);
        }
        // v41: "प्रिया छाता लेकर कार से उतरती है": from now on she carries an umbrella (it opens over her while it rains)
        if (Txt.has(s, "छाता", "छतरी", "छाते", "umbrella") && !negated(s) && !Txt.has(s, "छाता बंद", "छतरी बंद", "closes the umbrella", "छाता भूल", "no umbrella", "without an umbrella", "बिना छात")
                && Txt.has(s, "लेकर", "ले कर", "लेती", "लेता", "लेते", "ले ली", "ले लिया", "खोल", "उठा", "पकड़", "लिए", "लिये", "take", "open", "with an umbrella", "with umbrella", "hold", "carr")) {
            for (Film.Actor a : who) if (a.look != null && a.look.isHumanoid() && !a.look.umbrella && a.umbrellaFrom < 0) { a.umbrellaFrom = t; umbrellasTaken++; }
        }
        d = Math.max(d, mealFrom(s, t, who));
        d = Math.max(d, taskFrom(s, t, who));
        d = Math.max(d, doorFrom(s, t, who));
        d = Math.max(d, carryFrom(s, t, who));
        // v44: the sword, the shield, the spear and the bow; play in the snow; the animals of the place
        d = Math.max(d, combatFrom(s, t, subj, who, group));
        d = Math.max(d, snowFrom(s, t, subj, who, group));
        d = Math.max(d, animalsFrom(s, t, subj, who));
        d = Math.max(d, activityFrom(s, t, subj, who, group));
        if (Txt.has(s, BOW) && !archery(s)) { for (Film.Actor a : who) a.acts.add(new Film.Act(t, t + 1.7f, Film.G_BOW)); d = Math.max(d, 1.7f); }
        // (v43: "Waves crash against the shore" waves no hand)
        boolean waving = Txt.has(s, "हाथ हिला", "टाटा") || Txt.has(s.toLowerCase(java.util.Locale.ROOT), "waves goodbye", "waves to ", "waves at ", "waves his", "waves her",
                "waves back", "waves bye", "waved", "wave goodbye", "wave to ", "wave at ", "waves a hand", "waves hello", "waving goodbye", "waving to", "waving at")
                || (Txt.has(s, WAVE) && !Txt.has(s, "लहर", "sea", "shore", "ocean", "beach", "the waves", "waves crash", "waves roll", "waves lap", "waves hit", "waves break"));
        if (Txt.has(s, WAVE) && waving) { for (Film.Actor a : who) a.acts.add(new Film.Act(t, t + 1.8f, Film.G_WAVE)); d = Math.max(d, 1.6f); }
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

    /** v35: what the everyday-action passes did (the shot list's line). */
    private int sittings, risings, sleepers, wakings, meals, drinks, waistUp, mealCloseUps, lightingShots;
    /**
     * v35: one drawn from the user's photo who eats or drinks: its arm cannot bend to the mouth, so the action is shown
     * in a close-up, the cup or the morsel coming up into the frame to the lips (Renderer.drawToLips).
     */
    private Film.Actor mealCU;
    private float mealCUt;

    /** v35: drawn from the user's picture (not a drawn character). */
    private boolean photo(Film.Actor a) { return art != null && a.look != null && a.look.isHumanoid() && art.sprites.containsKey(a.c.id); }

    /**
     * v35: how high a seated face is, as a share of the standing height — the seat's top plus the body above the hips
     * for a drawn character (Puppet: a chair or a wheelchair 0.30, a sofa 0.27, the floor 0.15…), about 0.72 for a photo
     * lowered onto a seat (Rig.feetRise).
     */
    private float seatedFace(Film.Actor a, float t) {
        int seat = Renderer.seatOf(a, t);
        if (photo(a)) return seat == Film.SEAT_FLOOR ? 0.55f : 0.72f;
        if (a.look != null && a.look.aid == Look.AID_WHEELCHAIR) seat = Film.SEAT_WHEELCHAIR;
        float sh = Puppet.seatHeight(seat);
        if (sh < 0 || a.look == null) return 0.64f;
        return sh * Puppet.seatScale(a.look) + (a.look.isChild() ? 0.44f : 0.45f);
    }

    /** v35: a character drawn from the user's standing photo with no picture of them sitting (it is lowered onto the seat). */
    private boolean seatedPhoto(Film.Actor a) {
        if (art == null || a.look == null || !a.look.isHumanoid()) return false;
        Art.Sprite sp = art.sprites.get(a.c.id);
        if (sp == null) return false;
        if (sp.poses != null) for (Art.PoseSprite ps : sp.poses) if (ps.pose == PoseSense.SIT) return false;
        return true;
    }

    /** v35: a sound at a character (it pans with where they stand). */
    private Film.Sfx sfxAt(int type, float t, float dur, float gain, Film.Actor a) {
        Film.Sfx x = new Film.Sfx(type, t, dur, gain);
        x.pan = Math.max(-0.6f, Math.min(0.6f, (xAt(a, t) - 640) / 640f));
        return x;
    }

    static final String[] EAT = {" खा रहा", " खा रही", " खा रहे", " खाता है", " खाती है", " खाते हैं", " खाना खा", " खाने लग", " खा लिया", " खा ली", " खा गया", " खा गई", " खाकर",
            " खाते हुए", "रोटी खा", "लड्डू खा", "भोजन कर", "नाश्ता कर", "खाना खाते", " eats", " eating", " ate ", "has breakfast", "has lunch", "has dinner", "having lunch",
            "having dinner", "having breakfast",
            // v43: licked, bitten, munched
            " चाटता", " चाटती", " चाटते", " चबा", "licks", " bites", "a bite", "munches", "munching", " chews"};
    static final String[] DRINK = {" पी रहा", " पी रही", " पी रहे", " पीता है", " पीती है", " पीते हैं", " पी लिया", " पी ली", " पीकर", " पीते हुए", "पानी पी", "चाय पी",
            "दूध पी", "शरबत पी", "लस्सी पी", "घूँट", "घूंट", " drinks", " drinking", " drank", " sips", " sipping", " gulps",
            // v43: a sip of tea, drunk up
            "चुस्की", " पी लेता", " पी लेती", " पी लेते", " पी जाता", " पी जाती", "गटक", "a sip",
            // v44: drinks of grown-ups — a peg of whisky, a beer, wine; a toast
            "शराब पी", "शराब का", "व्हिस्की", "विस्की", "बीयर", "बियर", "वाइन", "पैग", "दारू पी", "मदिरा", "whisky", "whiskey", "beer", "wine", "glass of rum", "a peg", "a toast", "cocktail"};
    /** v44: what grown-ups drink, by the glass it comes in. */
    static final String[] WHISKY = {"व्हिस्की", "विस्की", "शराब", "पैग", "दारू", "मदिरा", "whisky", "whiskey", "glass of rum", "a peg", "scotch", "brandy", "liquor", "cocktail"};
    static final String[] BEER = {"बीयर", "बियर", "beer", "lager", "ale "};
    static final String[] WINE = {"वाइन", "wine", "champagne", "शैंपेन"};
    static final String[] CLINK = {"चीयर्स", "गिलास टकरा", "जाम टकरा", "cheers", "clink", "a toast", "raise their glasses", "raises his glass", "raises her glass"};

    /** v44: the glass or cup a drink comes in, from the words. */
    static int drinkItem(String s) {
        return Txt.has(s, WINE) ? Pose.I_WINE : Txt.has(s, BEER) ? Pose.I_BEER : Txt.has(s, WHISKY) ? Pose.I_WHISKY
                : Txt.has(s, "बोतल", "bottle") ? Pose.I_BOTTLE : Txt.has(s, "चाय", "tea", "coffee", "कॉफ़ी", "कॉफी") ? Pose.I_TEA
                : Txt.has(s, "कप", "प्याला", "प्याली", "cup", "mug") ? Pose.I_CUP : Pose.I_GLASS;
    }

    /**
     * v35: eating and drinking, as the story tells it — the hand goes from the plate to the mouth and they chew (two
     * bites, heard: the spoon on the plate, quiet chewing); a drink is raised to the lips from a cup, a glass or a
     * bottle (tea steams), a sip or gulps are heard, a cup is set down with a clink.
     */
    private float mealFrom(String s, float t, List<Film.Actor> who) {
        String sp = " " + s + " ";
        float d = 0;
        if (Txt.has(sp, DRINK)) {
            // v44: "तरुण व्हिस्की पीता है और प्रिया वाइन की चुस्की लेती है": each one named drinks what their own part of
            // the sentence says (and both drink)
            List<Film.Actor> drinkers = new ArrayList<Film.Actor>(who);
            Map<Film.Actor, String> own = new HashMap<Film.Actor, String>();
            if (seg != null) for (Film.Actor o : seg.actors) {
                String c = seatClause(s, o);
                if (c == null || c.isEmpty() || !Txt.has(" " + c + " ", DRINK)) continue;
                own.put(o, c);
                if (!drinkers.contains(o) && o.look != null && o.look.isHumanoid()) drinkers.add(o);
            }
            for (Film.Actor a : drinkers) {
                if (!a.look.isHumanoid()) continue;
                String cs = own.containsKey(a) ? own.get(a) : s;
                int item = drinkItem(cs);
                if (item == Pose.I_GLASS && cs != s) item = drinkItem(s);
                Film.Act act = new Film.Act(t + 0.2f, t + 2.8f, Film.G_DRINK);
                act.item = item;
                a.acts.add(act);
                film.sfx.add(sfxAt(item == Pose.I_BOTTLE ? Film.SFX_GULP : Film.SFX_SIP, t + 0.75f, item == Pose.I_BOTTLE ? 1.3f : 1.1f, 0.4f, a));
                if (item != Pose.I_BOTTLE) film.sfx.add(sfxAt(Film.SFX_CUP, t + 2.6f, 0.3f, 0.3f, a));
                if (item == Pose.I_WHISKY || item == Pose.I_WINE || item == Pose.I_BEER) {
                    // (a glass poured first when the sentence pours it; ice clinks in a whisky)
                    if (Txt.has(cs, "डाल", "बना", "pours", "pour", "fills")) film.sfx.add(sfxAt(Film.SFX_POUR, t, 0.6f, 0.25f, a));
                    grownUpDrinks++;
                }
                drinks++;
                // (the close-up of the first one drinking: a picture's glass reaches the lips only in a close-up)
                if (photo(a) && (drinkers.size() == 1 || mealCU == null || mealCUt < t)) { mealCU = a; mealCUt = t + 0.1f; }
            }
            d = Math.max(d, 2.9f);
        }
        // v44: a toast — glasses raised and clinked ("चीयर्स!"): everyone in it raises a glass, the clink is heard
        if (Txt.has(sp, CLINK)) {
            List<Film.Actor> toast = new ArrayList<Film.Actor>(who);
            if (toast.size() < 2) for (Film.Actor o : seg.actors) if (!toast.contains(o) && o.look != null && o.look.isHumanoid() && !o.look.isChild() && o.stateAt(t).visible) toast.add(o);
            int glass = Txt.has(s, BEER) ? Pose.I_BEER : Txt.has(s, WHISKY) ? Pose.I_WHISKY : Pose.I_WINE;
            for (Film.Actor a : toast) {
                if (a.look == null || !a.look.isHumanoid()) continue;
                boolean has = false;
                for (Film.Act x : a.acts) if (x.type == Film.G_DRINK && Math.abs(x.t0 - (t + 0.2f)) < 0.05f) has = true;
                // each raises the glass they had (a whisky stays a whisky), else the one the sentence names
                int mine = glass;
                if (!Txt.has(s, BEER) && !Txt.has(s, WHISKY) && !Txt.has(s, WINE))
                    for (Film.Act x : a.acts) if (x.type == Film.G_DRINK && x.t0 < t && (x.item == Pose.I_WHISKY || x.item == Pose.I_WINE || x.item == Pose.I_BEER)) mine = x.item;
                if (!has) { Film.Act act = new Film.Act(t + 0.9f, t + 3.4f, Film.G_DRINK); act.item = mine; a.acts.add(act); }
                Film.Act up = new Film.Act(t + 0.1f, t + 1.0f, Film.G_OFFER); up.item = mine; a.acts.add(up);
            }
            film.sfx.add(sfxAt(Film.SFX_CLINK, t + 0.7f, 0.5f, 0.42f, toast.isEmpty() ? null : toast.get(0)));
            toasts++;
            d = Math.max(d, 3.5f);
        }
        // (v43: "ठोकर खाकर", "चोट खाकर", "धोखा खाता", "चक्कर खाकर" — stumbled, hurt, cheated, dizzy: nothing is eaten)
        String ep = sp.replaceAll("(ठोकर|चोट|धोखा|मार|गोता|गोते|चक्कर|कसम|क़सम|हवा|डाँट|डांट|ग़म|गम|तरस|रहम|पछाड़|पछाड़|झटका|झटके|मुँह की|मुंह की)\\s+खा", " ");
        if (Txt.has(ep, EAT)) {
            for (Film.Actor a : who) {
                if (!a.look.isHumanoid()) continue;
                a.acts.add(new Film.Act(t + 0.2f, t + 4.6f, Film.G_EAT));
                film.sfx.add(sfxAt(Film.SFX_EAT, t + 0.3f, 4.2f, 0.35f, a));
                meals++;
                if (who.size() == 1 && photo(a)) { mealCU = a; mealCUt = t + 0.1f; }
            }
            d = Math.max(d, 4.7f);
        }
        return d;
    }

    // v36: everyday tasks, as the story names them
    static final String[] COOK = {"खाना बना", "खाना पका", "पकाती", "पकाता", "पकाते", "रोटी बना", "रोटियाँ बना", "सब्ज़ी बना", "सब्जी बना", "दाल बना", "चाय बना", "हलवा बना",
            "कड़ाही में", "चूल्हे पर", "cooks", "cooking", "is making tea", "makes tea", "stirs the", "stirring the",
            // v43: rotis on the tawa, a tadka, frying, boiling, breakfast, dinner, chai, coffee, khichdi
            "रोटी सेंक", "रोटियाँ सेंक", "रोटियां सेंक", "तवे पर", "रोटी बेल", "रोटियाँ बेल", "पराठे बना", "पराठा बना", "पूरी तल", "तड़का", "छौंक", "बघार", "तलती", "तलता",
            "भूनती", "भूनता", "उबालती", "उबालता", "नाश्ता बना", "कॉफ़ी बना", "कॉफी बना", "खिचड़ी बना", "भोजन बना", "रसोई में",
            "makes rotis", "rolls out", "fries", "makes dinner", "makes lunch", "makes breakfast", "prepares dinner", "prepares lunch", "prepares food",
            "makes food", "cooks dinner", "makes chai", "makes coffee"};
    static final String[] SWEEP = {"झाड़ू लगा", "झाड़ू दे", "झाड़ू से", "झाड़ू मार", "sweeps", "sweeping"};
    static final String[] WASH = {"कपड़े धो", "बर्तन धो", "बर्तन माँज", "बर्तन मांज", "washes clothes", "washing clothes", "washes the clothes", "washes the dishes",
            "washing the dishes", "washes dishes", "does the dishes", "does the laundry",
            // v43: soaped and scrubbed
            "साबुन लगा", "रगड़-रगड़", "रगड़ रगड़", "कपड़ों को धो", "कपड़ों पर साबुन", "scrubs the", "washes his clothes", "washes her clothes"};
    static final String[] READ = {"किताब पढ़", "पुस्तक पढ़", "अख़बार पढ़", "अखबार पढ़", "पढ़ाई कर", "पढ़ रहा", "पढ़ रही", "पढ़ रहे", "reads a", "reads the", "reading a",
            "reading the", "reads his", "reads her", "reads the newspaper", "is reading", "studies", "studying",
            // v43: a poem read aloud, an exam prepared for
            "कविता पढ़", "पढ़ता है", "पढ़ती है", "पढ़ते हैं", "परीक्षा की तैयारी", "इम्तिहान की तैयारी", "the exam", "his exam", "her exam", "exams", "revises", "revising"};
    static final String[] WRITE = {"लिख रहा", "लिख रही", "लिख रहे", "लिखता है", "लिखती है", "लिखने लग", "होमवर्क कर", "चिट्ठी लिख", "पत्र लिख", "writes a", "writes in", "writing a",
            "is writing", "does her homework", "does his homework", "doing homework"};
    static final String[] PHONE = {"फ़ोन पर बात", "फोन पर बात", "फ़ोन उठा", "फोन उठा", "फ़ोन करता", "फ़ोन करती", "फोन करता", "फोन करती", "मोबाइल पर बात", "on the phone",
            "answers the phone", "picks up the phone", "makes a call", "phone call", "calls on the phone",
            // v43: a call made or taken with words between ("फ़ोन पर पापा से बात करता है", "दादी को फ़ोन लगाता है")
            "फ़ोन लगा", "फोन लगा", "फ़ोन मिला", "फोन मिला", "answers the call", "dials"};
    static final String[] BRUSH = {"ब्रश कर", "दाँत साफ", "दांत साफ", "दाँत माँज", "brushes his teeth", "brushes her teeth", "brushes their teeth", "brushing teeth",
            "brushing his teeth", "brushing her teeth", "brushes teeth",
            // v43: the tooth powder and the rinse
            "मंजन", "दातुन", "कुल्ला"};
    static final String[] COMB = {"कंघी कर", "कंघा कर", "बाल बना", "बाल सँवार", "बाल संवार", "combs", "combing"};
    static final String[] RANGOLI = {"रंगोली", "rangoli"};
    static final String[] PAINTING = {"पेंटिंग कर", "पेंटिंग बना", "चित्र बना", "तस्वीर बना", "पेंट कर", "paints a", "paints the", "painting a", "is painting", "draws a picture", "drawing a picture",
            // v43: a sketch in pencil
            "स्केच", "ड्रॉइंग", "ड्राइंग", "sketch", "draws a", "draws the"};
    static final String[] WATER_PLANTS = {"पौधों को पानी", "पौधे को पानी", "पौधों में पानी", "पेड़ों को पानी", "waters the plants", "waters the plant", "watering the plants",
            "waters the garden", "waters the flowers",
            // v43: the pots
            "गमलों में पानी", "गमले में पानी", "गमलों को पानी", "waters the pots"};

    /** v36: what the everyday-task pass did. */
    private int tasks;
    /** v37: the activities staged (the shot list's line). */
    private int rides, playground, exercises, makeups, baths, screens, pecks, hugs;
    /** v37: this sentence's ride already moved its riders (the walk of travelFrom is not added on top). */
    private boolean rode;

    /**
     * v36: an everyday task, as the story tells it: cooking (a ladle stirring a pot on a stove, oil sizzling, steam),
     * sweeping (bent over a broom, swishes, a little dust), washing clothes or dishes (bent over a bucket, water
     * sloshing), reading (a book held up, the head down, a page turned now and then), writing (a notebook and a pen,
     * pen on paper heard), a phone call (the phone at the ear), brushing teeth, combing hair, watering plants (a
     * watering can, water falling on a potted plant). Each takes about four seconds of the story; a photo character
     * holds the tool at its hand (its arm cannot bend), and a phone or a toothbrush is shown at the face in a close-up.
     */
    // ------------------------------------------------------------------ v37: activities

    static final String[] RIDE_VERBS = {"चला", "चलाते", "चलाती", "चलाता", "सवार", "बैठकर", "बैठ कर", "में बैठ", "पर बैठ", "से जा", "से आ", "से घर", "से स्कूल", "से बाज़ार",
            // v43: getting on and in — "अंदर बैठता है", "पीछे बैठती है", "बस में चढ़ते हैं", "किक मारकर स्टार्ट करता है"
            "अंदर बैठ", "पीछे बैठ", "में चढ़", "पर चढ़", "चढ़कर", "चढ़ कर", "सवारी", "स्टार्ट", "किक मार", "लेकर निकल",
            // v44: driving in Hindi, the pedals, the steering wheel, a seat in a car
            "ड्राइव", "पैडल मार", "पैडल चला", "पेडल मार", "स्टीयरिंग", "आगे की सीट", "आगे वाली सीट", "पीछे की सीट", "पिछली सीट", "बगल वाली सीट",
            "steering", "back seat", "front seat", "passenger seat", "at the wheel", "brakes", "accelerator",
            "ride", "rides", "riding", "rode", "drive", "drives", "driving", "drove", "pedal", "takes the", "goes by", "comes by", "on a", "on his", "on her", "in a", "in his", "in her", "by bus", "by car",
            "gets on", "gets in", "got on", "got in", "hops on", "hops in", "climbs in", "climbs on", "jumps on", "jumps in", "starts the", "kick-start", "kickstart"};
    static final String[] BICYCLE = {"साइकिल", "साईकिल", "सायकिल", "सायकल", "bicycle", "cycle", "cycling", "cycles"};
    /** v43: only getting on (no going anywhere yet): sitting on it, getting in, starting it. */
    static final String[] MOUNT_ONLY = {"बैठता", "बैठती", "बैठते", "बैठ जा", "बैठ गय", "बैठ गई", "चढ़ता", "चढ़ती", "चढ़ते", "चढ़ जा", "चढ़ गय", "चढ़ गई",
            "स्टार्ट", "किक", "gets on", "gets in", "got on", "got in", "sits on", "sits in", "hops on", "hops in", "climbs on", "climbs in", "jumps on",
            "starts the", "starts his", "starts her", "kick-start", "kickstart"};
    /** v43: going somewhere on it. */
    static final String[] RIDE_MOVES = {"चलाता", "चलाती", "चलाते", "चलाकर", "चला कर", "चलाने", "चला रह", "चलाया", "चलाई", "जाता", "जाती", "जाते", "जा रह", "जाने",
            "आता", "आती", "आते", "आ रह", "आ गय", "आ गई", "निकल", "पहुँच", "पहुंच", "घूम", "दौड़", "भगा", "रवाना", "की ओर", "की तरफ", "तक ",
            "आगे बढ़", "बढ़ाता", "बढ़ाती", "बढ़ाते", "सरपट", "पैडल", "drives off", "speeds", "steers", "turns the", "accelerat",
            "ride", "rides", "riding", "rode", "drive", "drives", "driving", "drove", "goes", "went", "comes", "came", "leaves", "heads", "toward",
            "pedal", "speeds", "races", "zooms", "around", " off"};
    /** v43: arriving on it. */
    static final String[] ARRIVE_WORDS = {" आता", " आती", " आते", " आ रह", " आ गय", " आ गई", " आ जात", " आकर", "पहुँच", "पहुंच", " लौट", "comes", "came", "arrives",
            "arrived", "returns", "gets home", "reaches", "pulls up"};    // (with the space: "आता" is inside "चलाता")
    /** v43: going off on it to another place (out of the picture). */
    static final String[] GO_WORDS = {" जाता", " जाती", " जाते", " जा रह", " जाने", " चला गय", " चली गई", " चले गए", "goes to", "go to", "went to", "heads to", "heads for",
            "off to", "drives to the", "rides to the", "leaves for"};    // (with the space: "जाता" is inside "बजाता")
    static final String[] DESTINATIONS = {"बाज़ार", "मार्केट", "मंडी", "दुकान", "स्कूल", "कॉलेज", "दफ़्तर", "ऑफिस", "आफिस", "काम पर", "मंदिर", "अस्पताल",
            "स्टेशन", "शहर", "गाँव", "मेले", "मेला", "घर", "market", "shop", "school", "college", "office", "work", "temple", "hospital", "station", "town",
            "city", "village", "the fair", "home"};
    static final String[] MOTORBIKE = {"मोटरसाइकिल", "मोटर साइकिल", "मोटरबाइक", "बाइक", "बुलेट", "motorbike", "motorcycle", "bike"};
    static final String[] SCOOTER = {"स्कूटर", "स्कूटी", "scooter", "scooty"};
    static final String[] CAR = {"कार", "जीप", "टैक्सी", "गाड़ी", "गाडी", "car", "jeep", "taxi", "cab"};
    static final String[] BUS = {"बस", "bus"};
    static final String[] AUTO_RICKSHAW = {"ऑटो", "आटो", "रिक्शा", "रिक्शे", "auto", "rickshaw", "tuk-tuk"};
    static final String[] SWING = {"झूला झूल", "झूले पर", "झूला", "झूलती", "झूलता", "झूलते", "झूल रह", "swing", "swings", "swinging"};
    static final String[] SLIDE = {"फिसलपट्टी", "फिसल पट्टी", "फिसलन पट्टी", "स्लाइड", "slide", "slides", "sliding"};
    /** v40: the play itself (a mere "फिसलपट्टी की तरफ़ जाती है" is a walk to it). */
    static final String[] SLIDE_PLAY = {"फिसलती", "फिसलता", "फिसलते", "फिसलने", "फिसलकर", "फिसल कर", "फिसलूँ", "फिसलू", "फिसल रह", "slides down", "slid down",
            "goes down the slide", "went down the slide", "down the slide", "sliding", "slides", "slid"};
    static final String[] SWING_PLAY = {"झूला झूल", "झूले झूल", "झूलती", "झूलता", "झूलते", "झूलने", "झूलकर", "झूल कर", "झूल रह", "झूले पर", "swings on", "swinging",
            "on the swing", "swung"};
    static final String[] SEESAW = {"सी-सॉ", "सीसॉ", "सी सॉ", "ढेंकी", "see-saw", "seesaw", "see saw"};
    static final String[] ROUNDABOUT = {"गोल झूला", "गोल झूले", "चकरी", "चक्री", "merry-go-round", "merry go round", "roundabout"};
    static final String[] DUMBBELL = {"डंबल", "डम्बल", "dumbbell", "weights", "वज़न उठा", "वजन उठा"};
    static final String[] SKIPPING = {"रस्सी कूद", "रस्सी-कूद", "skipping", "skips", "jump rope", "jumping rope", "skipping rope"};
    static final String[] SQUAT = {"उठक-बैठक", "उठक बैठक", "बैठक लगा", "squat", "squats", "कसरत", "व्यायाम", "exercise", "exercises", "workout", "work out"};
    static final String[] YOGA = {"योग कर", "योग करत", "योगा", "योगासन", "सूर्य नमस्कार", "प्राणायाम", "ध्यान लगा", "yoga", "meditates", "meditating", "meditate"};
    static final String[] PUSHUP = {"पुश-अप", "पुशअप", "पुश अप", "दंड पेल", "दंड लगा", "push-up", "pushup", "push up", "push-ups", "pushups"};
    static final String[] KAJAL = {"काजल", "सुरमा", "आईलाइनर", "kajal", "kohl", "eyeliner"};
    static final String[] LIPSTICK = {"लिपस्टिक", "लिप ग्लॉस", "lipstick", "lip gloss"};
    static final String[] BINDI_ON = {"बिंदी लगा", "बिंदी लगात", "बिंदिया लगा", "puts on a bindi", "applies a bindi", "a bindi on"};
    static final String[] MEHNDI = {"मेहंदी", "मेंहदी", "मेहँदी", "mehndi", "mehendi", "henna"};
    static final String[] POWDER = {"पाउडर", "क्रीम लगा", "powder", "face cream"};
    static final String[] FACE_PAINT = {"फेस पेंट", "चेहरे पर रंग", "चेहरा रंग", "face paint", "face-paint", "facepaint", "paints her face", "paints his face"};
    static final String[] MAKEUP = {"मेकअप", "मेक-अप", "मेक अप", "श्रृंगार", "शृंगार", "सजती", "सज रही", "सज-धज", "makeup", "make-up", "make up"};
    static final String[] BATHE = {"नहाता", "नहाती", "नहाते", "नहा रहा", "नहा रही", "नहा रहे", "नहाने", "नहाया", "नहाई", "स्नान कर", "स्नान करत",
            "takes a bath", "has a bath", "bathes", "bathing", "takes a shower", "showers", "having a bath",
            // v43
            "quick bath", "a quick shower", "takes a dip"};
    static final String[] HUG = {"गले लगा", "गले लगात", "गले मिल", "गले से लगा", "hugs", "hug", "embraces", "embrace", "लिपट", "clings to"};
    static final String[] KISS = {"चूम", "पप्पी", "पुच्ची", "kisses", "kiss", "peck"};

    /** v37: a whole word or name (Hindi vowel signs count as part of a word: "कार" is not found in "शिकार"). */
    public static boolean term(String s, String... words) {
        String h = " " + s.toLowerCase(java.util.Locale.ROOT) + " ";
        for (String w0 : words) {
            String w = w0.toLowerCase(java.util.Locale.ROOT);
            int i = h.indexOf(w);
            while (i >= 0) {
                char before = h.charAt(i - 1);
                int e = i + w.length();
                char after = e < h.length() ? h.charAt(e) : ' ';
                boolean l = !partOfWord(before);
                boolean r = !partOfWord(after) || (after == 's' && (e + 1 >= h.length() || !partOfWord(h.charAt(e + 1))))
                        || after == '\u0947' || after == '\u094B';       // a plural: "कारें", "बसों"
                if (l && r) return true;
                i = h.indexOf(w, i + 1);
            }
        }
        return false;
    }

    private static boolean partOfWord(char c) {
        if (Character.isLetterOrDigit(c)) return true;
        int ty = Character.getType(c);
        return ty == Character.NON_SPACING_MARK || ty == Character.COMBINING_SPACING_MARK || ty == Character.ENCLOSING_MARK;
    }

    /** v37: which vehicle a sentence rides or drives (Film.V_*), 0 for none. */
    /**
     * v44: what a ride brings with it beyond the going: the mishap (a skid and the fall, a crash into a pole or a tree,
     * a hard brake for a dog in the road), the close inserts a driver's hands and feet get (the steering wheel turned,
     * the accelerator, the brake, a cyclist's feet on the pedals) and a weapon used from the saddle. Returns when the
     * last of it is over.
     */
    private float rideExtras(String s, int v, Film.Act lr, List<Film.Actor> riders, int mishap, float dir, float vs, float t) {
        Film.Actor lead = riders.get(0);
        float go = lr.t0 + rideMount(lr), stop = lr.t0 + rideStop(lr), end = lr.t1;
        float tm = lr.mishapAt >= 0 ? lr.t0 + lr.mishapAt : -1;
        boolean car = v == Film.V_CAR && lr.item == Film.V_CAR;          // their own car: the hands and feet of its driver
        // ---- the mishap
        if (mishap == Film.MISHAP_SKID && tm > 0) {
            // the two-wheeler slides out from under them and lies on its side; they are thrown down beside it, lie a
            // moment, and get up hurting (a pillion rider too)
            float wx = xAt(lead, tm - 0.01f);
            Film.Fx w = fx(Film.FX_WRECK, tm - 0.3f, 1e6f, wx, 0, lead, null); w.kind = Film.MISHAP_SKID; w.color = dir > 0 ? 1 : -1; w.t2 = v;
            if (v != Film.V_BICYCLE && v != Film.V_HORSE) film.sfx.add(sfxAt(Film.SFX_SCREECH, tm - 0.6f, 0.7f, 0.34f, lead));
            film.sfx.add(sfxAt(v == Film.V_BICYCLE || v == Film.V_HORSE ? Film.SFX_THUD : Film.SFX_CRASH, tm, 0.9f, v == Film.V_BICYCLE ? 0.4f : 0.32f, lead));
            if (v == Film.V_HORSE) film.sfx.add(sfxAt(Film.SFX_NEIGH, tm - 0.4f, 1.2f, 0.4f, lead));
            int n = 0;
            for (Film.Actor a : riders) {
                boolean on = a == lead;
                for (Film.Act x : a.acts) if (x.type == Film.G_RIDE && x.target == lead && Math.abs(x.t0 - lr.t0) < 0.5f) { on = true; x.t1 = Math.min(x.t1, tm + 0.05f); x.mishap = mishap; x.mishapAt = tm - x.t0; }
                if (!on) continue;
                fallDown(a, tm, a == lead ? wx + dir * 70 : wx - dir * (40 + 50 * n), dir);
                n++;
            }
            falls += n;
            mishaps++;
            return tm + 2.6f;
        }
        if (mishap == Film.MISHAP_CRASH && tm > 0) {
            // it hits what the sentence names (a pole, a tree, a wall, another car) and stops dead: a bang, glass, the
            // bumper crumpled and smoke from under the bonnet; they lurch forward, sit stunned, then get out
            float front = (v == Film.V_CAR ? 0.95f : v == Film.V_BUS ? 1.9f : 0.6f) * vs;
            float wx = xAt(lead, tm - 0.01f) + dir * front;
            int what = Txt.has(s, "पेड़", "पेड", "tree") ? 1 : Txt.has(s, "दीवार", "wall") ? 2 : Txt.has(s, "कार से", "गाड़ी से", "ट्रक", "बस से", "another car", "a car", "truck", "the car in front") ? 3 : 0;
            Film.Fx w = fx(Film.FX_WRECK, seg.t0, 1e6f, wx, what, lead, null);          // (what it hits stands there from the start)
            w.kind = Film.MISHAP_CRASH; w.color = dir > 0 ? 1 : -1; w.t2 = tm;
            if (Txt.has(s, BRAKE_WORDS) || Txt.has(s, "अचानक", "suddenly")) {
                film.sfx.add(sfxAt(Film.SFX_SCREECH, tm - 0.8f, 0.85f, 0.36f, lead));
                if (car) insert(Film.INS_PEDALS, tm - 1.0f, tm - 0.15f, lead, Film.INS_MOVE_BRAKE | Film.INS_MOVE_HARD);
            }
            film.sfx.add(sfxAt(Film.SFX_CRASH, tm, 1.4f, 0.48f, lead));
            for (Film.Actor a : seg.actors) {
                boolean in = a == lead;
                for (Film.Act x : a.acts) if (x.type == Film.G_RIDE && x.target == lead && Math.abs(x.t0 - lr.t0) < 0.5f) { in = true; x.mishap = mishap; x.mishapAt = tm - x.t0; }
                if (!in) continue;
                Film.Key k = keyAt(a, tm); k.emotion = Pose.SCARED;
                // after getting out: a hand to the head, then the look at the damage
                Film.Act hh = new Film.Act(Math.max(tm + 1.6f, endOf(a, lr)), Math.max(tm + 1.6f, endOf(a, lr)) + 1.6f, Film.G_HOLD_HEAD);
                a.acts.add(hh);
            }
            shake(tm, tm + 0.35f);
            mishaps++;
            return Math.max(end, tm + 3.2f);
        }
        if (mishap == Film.MISHAP_SWERVE && tm > 0) {
            // a hard brake: the tyres screech, it stops short — a dog (a cow, a goat, a child's ball) in the road runs
            // off — and it goes on
            film.sfx.add(sfxAt(Film.SFX_SCREECH, tm - 0.35f, 0.8f, 0.38f, lead));
            if (car) insert(Film.INS_PEDALS, tm - 0.95f, tm - 0.1f, lead, Film.INS_MOVE_BRAKE | Film.INS_MOVE_HARD);
            int sp = Txt.has(s, "गाय", "cow") ? Look.SP_COW : Txt.has(s, "बकरी", "goat") ? Look.SP_GOAT : Txt.has(s, "बिल्ली", "cat") ? Look.SP_CAT
                    : Txt.has(s, "हिरण", "deer") ? Look.SP_DEER : Look.SP_DOG;
            float front = (v == Film.V_CAR ? 0.95f : v == Film.V_BUS ? 1.9f : 0.5f) * vs;
            Film.Fx dog = fx(Film.FX_ANIMAL, tm - 1.2f, tm + 1.6f, xAt(lead, tm) + dir * (front + 70), 1, lead, null);
            dog.kind = sp; dog.color = dir > 0 ? -1 : 1; dog.t2 = tm + 0.1f;
            for (Film.Actor a : seg.actors) {
                boolean in = a == lead;
                for (Film.Act x : a.acts) if (x.type == Film.G_RIDE && x.target == lead && Math.abs(x.t0 - lr.t0) < 0.5f) { in = true; x.mishap = mishap; x.mishapAt = tm - x.t0; }
                if (!in) continue;
                Film.Key k = keyAt(a, tm - 0.1f); k.emotion = Pose.SCARED;
                Film.Key k2 = keyAt(a, tm + 1.2f); k2.emotion = Pose.RELIEVED;
            }
            mishaps++;
        }
        // ---- the driver's hands and feet, close
        if (car && go + 1.0f < stop) {
            boolean said = false;
            float at = go + 0.15f;
            // (in the order a driver does it: the hands on the wheel, then the foot down)
            if (Txt.has(s, STEER_WORDS) || (Txt.has(s, "स्टार्ट", "starts the car", "starts the engine") && !Txt.has(s, ACCEL_WORDS))) {
                if (at + 1.2f < stop) { insert(Film.INS_STEER, at, at + 1.25f, lead, dir > 0 ? Film.INS_MOVE_TURN_R : Film.INS_MOVE_TURN_L); at += 1.35f; said = true; }
            }
            if (Txt.has(s, ACCEL_WORDS) && at + 1.0f < stop) { insert(Film.INS_PEDALS, at, at + 1.15f, lead, Film.INS_MOVE_GO); said = true; }
            if (mishap == Film.MISHAP_NONE && Txt.has(s, BRAKE_WORDS)) {
                float hard = Txt.has(s, "ज़ोर", "जोर", "अचानक", "hard", "slams", "suddenly") ? Film.INS_MOVE_HARD : 0;
                insert(Film.INS_PEDALS, stop - 1.0f, stop - 0.1f, lead, Film.INS_MOVE_BRAKE | (int) hard);
                if (hard != 0) film.sfx.add(sfxAt(Film.SFX_SCREECH, stop - 0.7f, 0.8f, 0.34f, lead));
                said = true;
            }
            // (a long drive with nothing said: once, the hands on the wheel)
            if (!said && stop - go >= 4.0f && mishap == Film.MISHAP_NONE) insert(Film.INS_STEER, go + (stop - go) * 0.45f, go + (stop - go) * 0.45f + 1.2f, lead, 0);
        }
        if (v == Film.V_BICYCLE && lr.item == Film.V_BICYCLE && Txt.has(s, PEDAL_WORDS) && go + 1.2f < stop)
            insert(Film.INS_CYCLE_FEET, go + 0.4f, go + 1.7f, lead, Txt.has(s, "ज़ोर", "जोर", "तेज़", "hard", "fast", "खड़े होकर", "standing") ? Film.INS_MOVE_HARD : 0);
        // ---- a weapon from the saddle
        if ((v == Film.V_HORSE || v == Film.V_ELEPHANT) && go + 1.0f < stop) {
            boolean bow = Txt.has(s, "तीर", "धनुष", "arrow", "bow and", "his bow", "her bow", "shoots");
            boolean sword = Txt.has(s, "तलवार", "sword", "blade", "भाला", "spear");
            if (bow || sword) {
                lr.mounted = bow ? Film.G_ARCHERY : Film.G_SWORD;
                lr.mountedAt = rideMount(lr) + 0.4f;
                lr.mountedTo = Math.max(lr.mountedAt + 1.4f, rideStop(lr) - 0.3f);
                if (bow) {
                    float at = lr.t0 + lr.mountedAt + 1.1f;
                    float hx = xAt(lead, at);
                    Film.Fx ar = fx(Film.FX_ARROW, at, at + 6f, hx + dir * 520, 0, lead, null);
                    ar.kind = Film.ARCH_TARGET; ar.t2 = at + 0.45f; ar.color = dir > 0 ? 1 : -1;
                    film.sfx.add(sfxAt(Film.SFX_ARROW, at - 0.15f, 0.8f, 0.4f, lead));
                } else {
                    for (float st = lr.t0 + lr.mountedAt + 0.3f; st < lr.t0 + lr.mountedTo - 0.2f; st += 0.9f) film.sfx.add(sfxAt(Film.SFX_SWORD, st, 0.4f, 0.3f, lead));
                }
                mountedWeapons++;
            }
        }
        return end;
    }

    /** v44: when this one's part of a ride ends (their own act on it), else the lead's ride's end. */
    private static float endOf(Film.Actor a, Film.Act lr) {
        for (Film.Act x : a.acts) if (x.type == Film.G_RIDE && Math.abs(x.t0 - lr.t0) < 0.5f) return x.t1;
        return lr.t1;
    }

    /** v44: a close insert over the whole frame (a driver's hands on the wheel, the feet on the pedals). */
    private void insert(int kind, float t0, float t1, Film.Actor a, int moves) {
        if (t1 - t0 < 0.5f) return;
        for (Film.Fx o : seg.fx) if (o.type == Film.FX_INSERT && o.t0 < t1 && o.t1 > t0) return;     // one at a time
        Film.Fx f = fx(Film.FX_INSERT, t0, t1, 0, 0, a, null);
        f.kind = kind; f.color = moves;
        inserts++;
    }

    /**
     * v44: thrown down (a skid, a trip, a slip): onto the ground at x in 0.35 s, a moment lying there, up again in pain —
     * a hand to the knee — and on their feet.
     */
    private void fallDown(Film.Actor a, float t, float x, float dir) {
        Film.Act f = new Film.Act(t, t + 2.4f, Film.G_FALL);
        f.item = dir > 0 ? 1 : -1;
        a.acts.add(f);
        // (thrown down as it goes over — a short slide on the road, never a long one lying)
        Film.Key k = keyAt(a, t); k.x = Math.max(120, Math.min(1160, x)); k.moveDur = 0.3f; k.body = Pose.LIE; k.seat = -1; k.emotion = Pose.PAIN; k.facing = dir; k.run = false;
        Film.Key up = keyAt(a, t + 1.5f); up.body = Pose.STAND; up.emotion = Pose.PAIN;
        Film.Key ok = keyAt(a, t + 3.2f); ok.emotion = Pose.SAD;
    }

    /** v44: the words of a mishap on a ride. */
    static final String[] CRASH_WORDS = {"टकरा", "टक्कर", "भिड़ ग", "भिड़ जा", "ठोक दी", "ठोक दिया", "एक्सीडेंट", "दुर्घटना", "crash", "collide", "collision", "rams into",
            "smashes into", "slams into", "hits a ", "hits the ", "bumps into", "accident"};
    static final String[] SKID_WORDS = {"फिसल", "संतुलन बिगड़", "संतुलन खो", "बैलेंस बिगड़", "उलट", "पलट जा", "पलट ग", "गिर पड़", "गिर जा", "गिर ग", "गिरता", "गिरती", "गिरते",
            "skid", "slips", "slipped", "loses his balance", "loses her balance", "lost his balance", "lost her balance", "falls off", "fell off", "falls", "topples", "overturn",
            "tips over", "comes off"};
    static final String[] SWERVE_WORDS = {"बाल-बाल", "बाल बाल", "ज़ोर से ब्रेक", "जोर से ब्रेक", "ज़ोरदार ब्रेक", "अचानक ब्रेक", "तेज़ ब्रेक", "swerve", "brakes hard", "brake hard",
            "slams on the brake", "slams the brake", "slammed on the brake", "narrowly", "just in time", "screeches to", "emergency brake"};
    static final String[] BRAKE_WORDS = {"ब्रेक", "brake", "brakes", "braking"};
    static final String[] ACCEL_WORDS = {"एक्सीलेरेटर", "एक्सिलरेटर", "रेस दे", "रफ़्तार बढ़", "रफ्तार बढ़", "तेज़ चला", "तेज चला", "accelerat", "speeds up", "speeds away", "steps on it",
            "floors it", "puts his foot down", "puts her foot down"};
    static final String[] STEER_WORDS = {"स्टीयरिंग", "स्टेयरिंग", "मोड़", "मुड़", "घुमाकर गाड़ी", "steering", "steers", "turns the wheel", "turns the car", "takes the turn", "turns left",
            "turns right"};
    static final String[] PEDAL_WORDS = {"पैडल", "पेडल", "pedal", "pedals", "pedalling", "pedaling"};


    // ================================================================== v44: the sword, the bow, the snow, the animals

    static final String[] ARROW_WORDS = {"तीर", "बाण", "धनुष", "तीरंदाज़", "तीरंदाज", "निशाना साध", "arrow", "arrows", "archery", "archer", "longbow"};
    static final String[] BOW_WEAPON = {"his bow", "her bow", "the bow", "a bow and", "bow and arrow", "draws the bow", "draws his bow", "draws her bow", "strings the bow"};
    static final String[] RELEASE = {"छोड़", "चलाता", "चलाती", "चलाते", "चला दे", "चला दिया", "मारता", "मारती", "लगता", "लगती", "जा लग", "shoots", "shot", "looses",
            "fires", "lets fly", "releases", "hits the", "strikes the"};
    static final String[] HUNT_WORDS = {"शिकार", "हिरण", "हिरन", "खरगोश", "जंगली सूअर", "hunt", "deer", "rabbit", "boar", "prey", "stag"};
    static final String[] SWORD_WORDS = {"तलवार", "तलवारबाज़", "तलवारबाज", "खड्ग", "कटार", "sword", "swords", "fencing", "swordplay", "sabre", "saber", "blade"};
    static final String[] SWORD_USE = {"चला", "घुमा", "वार", "लड़", "लड़ा", "भांज", "भाँज", "अभ्यास", "टकरा", "मुकाबला", "द्वंद्व", "भिड़", "practi", "swings", "swing", "slash",
            "fight", "duel", "parr", "strikes", "attack", "clash", "spar", "lunges", "thrust", "train"};
    static final String[] SHIELD_WORDS = {"ढाल", "shield"};
    static final String[] SPEAR_WORDS = {"भाला", "भाले", "बरछी", "spear", "javelin", "lance"};
    static final String[] SNOWBALL = {"बर्फ़ का गोला", "बर्फ का गोला", "बर्फ़ के गोले", "बर्फ के गोले", "बर्फ़ के गोलों", "बर्फ के गोलों", "स्नोबॉल", "snowball", "snow ball"};
    static final String[] SNOWMAN = {"बर्फ़ का पुतला", "बर्फ का पुतला", "बर्फ़ का आदमी", "बर्फ का आदमी", "स्नोमैन", "स्नो मैन", "हिममानव", "snowman", "snow man", "snowmen"};
    static final String[] SNOW_CATCH = {"फाहे पकड़", "फाहों को पकड़", "बर्फ़ पकड़", "बर्फ पकड़", "बर्फ़ को पकड़", "बर्फ को पकड़", "जीभ पर बर्फ़", "जीभ पर बर्फ", "catches snowflakes",
            "catch snowflakes", "catches the snowflakes", "catching snowflakes", "catches snow", "snowflakes on his", "snowflakes on her", "snowflakes in his", "snowflakes in her"};
    /** v44: snow lying on the ground (not falling): a snowy place, things done in the snow. */
    static final String[] SNOW_GROUND = {"बर्फ़ से ढक", "बर्फ से ढक", "बर्फ़ीली", "बर्फीली", "बर्फ़ीला", "बर्फीला", "बर्फ़ में", "बर्फ में", "बर्फ़ पर", "बर्फ पर", "बर्फ़ की ढलान",
            "बर्फ की ढलान", "हिमालय", "snowy", "snow-covered", "snow covered", "covered in snow", "covered with snow", "in the snow", "on the snow", "deep snow", "snowfield"};

    static boolean archery(String s) {
        s = s.replace("इंद्रधनुष", " ").replace("इन्द्रधनुष", " ").replace("rainbow", " ");      // (a rainbow is no bow)
        return Txt.has(s, ARROW_WORDS) || (Txt.has(s, BOW_WEAPON) && !Txt.has(s, "bow to", "bows to", "bow down", "bows down", "bows before", "bow before"));
    }

    static boolean snowSlide(String s) {
        return (Txt.has(s, "फिसल") && Txt.has(s, "बर्फ़", "बर्फ", "ढलान")) || Txt.has(s, "sledge", "sled ", "sled.", "sledding", "toboggan", "slides down the snow", "snowy slope",
                "slides on the snow", "slides across the snow", "down the snowy");
    }

    /** v44: a sword, a shield, a spear, the bow — practice, a duel, an arrow loosed at a target, the hunt. */
    private float combatFrom(String s, float t, Film.Actor subj, List<Film.Actor> who, List<Film.Actor> group) {
        if (subj == null || negated(s) || subj.look == null || !subj.look.isHumanoid()) return 0;
        // (from the saddle: the ride takes it — rideExtras)
        int rv = vehicleIn(s, subj.look.isChild());
        if (rv == Film.V_HORSE || rv == Film.V_ELEPHANT) return 0;
        float d = 0;
        if (archery(s)) d = Math.max(d, archeryFrom(s, t, subj, who));
        boolean sword = Txt.has(s, SWORD_WORDS) && (Txt.has(s, SWORD_USE) || Txt.has(s, "उठा", "निकाल", "draws", "raises", "holds", "takes"));
        boolean shield = Txt.has(s, SHIELD_WORDS), spear = Txt.has(s, SPEAR_WORDS);
        if (sword || shield) {
            boolean practice = Txt.has(s, "अभ्यास", "लकड़ी", "practi", "train", "wooden", "spar");
            boolean ready = !Txt.has(s, SWORD_USE) || Txt.has(s, "तैयार", "ready", "stance");
            // the fighters: the subject and whoever they fight ("वृंदा से", "with Priya", the others named)
            List<Film.Actor> f = new ArrayList<Film.Actor>(who);
            for (Film.Actor a : group) if (!f.contains(a) && a.look != null && a.look.isHumanoid() && a.c != null && Txt.has(s, a.c.displayName)) f.add(a);
            int blade = practice ? Pose.I_WOOD_SWORD : Pose.I_SWORD;
            for (Film.Actor a : f) {
                Film.Key k = keyAt(a, t);
                if (sword) k.holdR = blade;
                if (shield) k.holdL = Pose.I_SHIELD;
                if (Txt.has(s, "क्रोध", "गुस्से", "angr", "fierce")) k.emotion = Pose.ANGRY; else k.emotion = Pose.DETERMINED;
            }
            if (sword && !ready) {
                if (f.size() >= 2) {
                    // a duel: they close in and trade blows — one strikes, the other blocks, by turns — the blades ring
                    Film.Actor a = f.get(0), b = f.get(1);
                    // a blade's length apart, face to face (never one on the other's spot)
                    float xa0 = xAt(a, t), xb0 = xAt(b, t), mid = Math.max(240, Math.min(1040, (xa0 + xb0) / 2));
                    float sa = xa0 <= xb0 ? -1 : 1;
                    if (a.stateAt(t).body != Pose.STAND || b.stateAt(t).body != Pose.STAND) { moveNear(a, b, t, 0.1f); }
                    else {
                        Film.Key ka = a.at(t); ka.x = mid + sa * 105; ka.moveDur = 0.7f;
                        Film.Key kb = b.at(t); kb.x = mid - sa * 105; kb.moveDur = 0.7f;
                    }
                    float xa = xAt(a, t + 0.8f), xb = xAt(b, t + 0.8f);
                    keyAt(a, t + 0.8f).facing = xb >= xa ? 1 : -1;
                    keyAt(b, t + 0.8f).facing = xa >= xb ? 1 : -1;
                    for (int i = 0; i < 4; i++) {
                        float b0 = t + 0.9f + i * 0.85f;
                        Film.Actor hit = i % 2 == 0 ? a : b, def = i % 2 == 0 ? b : a;
                        hit.acts.add(new Film.Act(b0, b0 + 0.8f, Film.G_SWORD));
                        def.acts.add(new Film.Act(b0 + 0.1f, b0 + 0.8f, Film.G_BLOCK));
                        film.sfx.add(sfxAt(Film.SFX_SWORD, b0 + 0.35f, 0.5f, 0.4f, hit));
                    }
                    duels++;
                    d = Math.max(d, 4.4f);
                } else {
                    // practice alone: swings and cuts at the air (a straw dummy when the sentence has one)
                    Film.Actor a = f.get(0);
                    if (Txt.has(s, "पुतले", "पुतला", "dummy", "target", "निशाने")) {
                        float x = xAt(a, t), dir = a.stateAt(t).facing >= 0 ? 1 : -1;
                        boolean have = false;
                        for (Film.Fx o : seg.fx) if (o.type == Film.FX_TARGET && o.t1 > t) have = true;
                        if (!have) { Film.Fx tg = fx(Film.FX_TARGET, t, 1e6f, Math.max(140, Math.min(1140, x + dir * 150)), 0, a, null); tg.kind = 1; }
                    }
                    for (int i = 0; i < 3; i++) {
                        float b0 = t + 0.3f + i * 1.0f;
                        a.acts.add(new Film.Act(b0, b0 + 0.95f, Film.G_SWORD));
                        film.sfx.add(sfxAt(Film.SFX_WHOOSH, b0 + 0.4f, 0.35f, 0.25f, a));
                    }
                    d = Math.max(d, 3.4f);
                }
                swordPlays++;
            } else d = Math.max(d, 1.0f);
        }
        if (spear) {
            for (Film.Actor a : who) {
                Film.Key k = keyAt(a, t); k.holdR = Pose.I_SPEAR;
                if (Txt.has(s, THROW) || Txt.has(s, "फेंक", "hurls", "throws", "casts")) {
                    float x = xAt(a, t), dir = a.stateAt(t).facing >= 0 ? 1 : -1;
                    a.acts.add(new Film.Act(t + 0.2f, t + 1.2f, Film.G_THROW));
                    Film.Key off = keyAt(a, t + 0.75f); off.holdR = Pose.I_NONE;
                    Film.Fx fl = fx(Film.FX_THROW, t + 0.7f, t + 4.5f, Math.max(60, Math.min(1220, x + dir * 460)), ground, a, null);
                    fl.kind = 5;
                    film.sfx.add(sfxAt(Film.SFX_WHOOSH, t + 0.7f, 0.6f, 0.35f, a));
                    film.sfx.add(sfxAt(Film.SFX_THUD, t + 1.6f, 0.3f, 0.3f, a));
                    d = Math.max(d, 2.4f);
                } else d = Math.max(d, 1.0f);
            }
        }
        return d;
    }

    /**
     * v44: the bow: an arrow from the quiver onto the string, the string drawn back to the cheek, a breath's aim, the
     * release (the twang, the whoosh) and the follow-through — once, or arrow after arrow — into a target board, into
     * the ground where the hunted deer was (it bolts at the twang: the hunt is missed, the film stays kind), into the sky.
     */
    private float archeryFrom(String s, float t, Film.Actor subj, List<Film.Actor> who) {
        int mark = Txt.has(s, HUNT_WORDS) ? Film.ARCH_HUNT : Txt.has(s, "आसमान", "आकाश", "sky", "ऊपर की ओर", "into the air") ? Film.ARCH_SKY
                : Txt.has(s, "दुश्मन", "सैनिक", "शत्रु", "enemy", "soldier", "foe") ? Film.ARCH_ENEMY : Film.ARCH_TARGET;
        boolean loose = Txt.has(s, RELEASE) || Txt.has(s, "तीर चल", "arrow at", "an arrow", "arrows at");
        if (Txt.has(s, "निशाना साध", "aims") && !Txt.has(s, "छोड़", "चलाता", "चलाती", "shoots", "looses", "fires", "lets fly", "releases")) loose = false;
        // "the last arrow strikes the centre": the arrows already flown — only the close of where it struck
        boolean shot = Txt.has(s, "छोड़", "चलाता", "चलाती", "चलाते", "चला दे", "चला दिया", "मारता", "मारती", "shoots", "shot", "looses", "fires", "lets fly", "releases", "draws");
        if (!shot && Txt.has(s, "जा लग", "लगता है", "लगती है", "धँस", "धंस", "hits", "strikes", "lands", "thuds", "finds its mark", "pierces")) {
            float lastHit = -1;
            for (Film.Fx o : seg.fx) if (o.type == Film.FX_ARROW && o.a == subj && o.t0 > t - 8 && o.t0 <= t + 0.5f) lastHit = Math.max(lastHit, o.t2);
            if (lastHit > 0) {
                float at = Math.max(t, lastHit + 0.05f);
                insert(Film.INS_ARROW, at, at + 1.3f, subj, 0);
                return at + 1.4f - t;
            }
        }
        int n = !loose ? 0 : Txt.has(s, "तीन तीर", "three arrows", "एक के बाद एक", "one after another", "volley") ? 3 : Txt.has(s, "दो तीर", "two arrows") ? 2 : Txt.has(s, "arrows", "तीरों") ? 2 : 1;
        Film.Actor a = subj;
        float x = xAt(a, t);
        // where the mark is: a target or the animal already there, else on the side with more room
        Film.Fx tg = null;
        for (Film.Fx o : seg.fx) if (o.t1 > t && ((mark == Film.ARCH_HUNT && o.type == Film.FX_ANIMAL) || (mark == Film.ARCH_TARGET && o.type == Film.FX_TARGET && o.kind == 0))) tg = o;
        float dir = tg != null ? (tg.x >= x ? 1 : -1) : (x < 640 ? 1 : -1);
        float mx = tg != null ? tg.x : Math.max(210, Math.min(1070, x + dir * 470));
        if (Math.abs(mx - x) < 240) { dir = -dir; mx = Math.max(210, Math.min(1070, x + dir * 470)); }
        // never shoot past anyone: a free side for a new mark; those still between step back behind the archer
        if (tg == null && between(a, x, mx, t) > 0) {
            float ox = Math.max(210, Math.min(1070, x - dir * 470));
            if (Math.abs(ox - x) >= 240 && between(a, x, ox, t) == 0) { dir = -dir; mx = ox; }
        }
        int back = 0;
        for (Film.Actor o : seg.actors) {
            if (o == a || !o.stateAt(t).visible) continue;
            float ox = xAt(o, t);
            if ((ox - x) * dir > 30 && (mx - ox) * dir > -40) {
                Film.Key ok = o.at(t); ok.x = Math.max(110, Math.min(1170, x - dir * (140 + 100 * back++))); ok.moveDur = 0.8f;
                ok.facing = dir;
            }
        }
        float h = heightOf(a);
        if (tg == null && mark == Film.ARCH_TARGET) { tg = fx(Film.FX_TARGET, t, 1e6f, mx, 0, a, null); tg.kind = 0; }
        if (tg == null && mark == Film.ARCH_HUNT) {
            int sp = Txt.has(s, "खरगोश", "rabbit") ? Look.SP_RABBIT : Look.SP_DEER;
            tg = fx(Film.FX_ANIMAL, t, 1e6f, mx, 0, a, null); tg.kind = sp; tg.color = (int) dir; tg.t2 = 1e6f;
            animalsShown++;
        }
        Film.Key k = keyAt(a, t); k.facing = dir; k.holdR = Pose.I_BOW; k.emotion = Pose.DETERMINED;
        float per = 1.5f, dur = 0.5f + Math.max(1, n) * per + (n == 0 ? 0.6f : 0.3f);
        Film.Act act = new Film.Act(t + 0.1f, t + 0.1f + dur, Film.G_ARCHERY);
        act.item = mark; act.how = n; act.target = null;
        a.acts.add(act);
        float last = -1;
        for (int i = 0; i < n; i++) {
            float rel = act.t0 + 0.5f + i * per + 0.95f;
            float tx = mark == Film.ARCH_SKY ? x + dir * 300 : mark == Film.ARCH_HUNT ? mx - dir * 30 + i * 18 * dir : mx + (i - (n - 1) / 2f) * 6;
            float ty = mark == Film.ARCH_SKY ? -120 : mark == Film.ARCH_HUNT ? ground + 6 : ground - 0.52f * h + (i - (n - 1) / 2f) * 8;
            float flight = Math.abs(tx - x) / 1500f + 0.14f;
            Film.Fx ar = fx(Film.FX_ARROW, rel, 1e6f, tx, ty, a, null);
            ar.kind = mark; ar.t2 = rel + flight; ar.color = (int) dir;
            film.sfx.add(sfxAt(Film.SFX_ARROW, rel - 0.08f, flight + 0.4f, 0.42f, a));
            if (mark == Film.ARCH_HUNT && i == 0 && tg != null && tg.type == Film.FX_ANIMAL && tg.t2 > rel) { tg.t2 = rel + 0.05f; tg.t1 = rel + 3f; }
            last = rel + flight;
            arrows++;
        }
        // the close of the bow drawn and loosed (the picture's arm cannot draw it in the wide shot)
        insert(Film.INS_BOW, act.t0 + 0.55f, n > 0 ? act.t0 + 1.5f : act.t0 + 1.8f, a, n > 0 ? Film.INS_MOVE_GO : 0);
        if (last > 0 && Txt.has(s, "बीच में", "निशाने के बीच", "बीचों-बीच", "बीचोंबीच", "सटीक", "bullseye", "bull's eye", "bull's-eye", "dead centre", "dead center", "the centre", "the center"))
            insert(Film.INS_ARROW, last + 0.05f, last + 1.35f, a, 0);
        Film.Key done = keyAt(a, act.t1 + 0.5f); done.holdR = Pose.I_NONE;
        archeryActs++;
        frameAround(t, Math.min(x, mx) - 60, Math.max(x, mx) + 60);
        return dur + 0.3f + (last > 0 && last > act.t1 ? last - act.t1 : 0);
    }

    /** v44: how many others stand between x0 and x1 (exclusive of the ends) at t. */
    private int between(Film.Actor a, float x0, float x1, float t) {
        int n = 0;
        float lo = Math.min(x0, x1) + 30, hi = Math.max(x0, x1) + 40;
        for (Film.Actor o : seg.actors) if (o != a && o.stateAt(t).visible) { float ox = xAt(o, t); if (ox > lo && ox < hi) n++; }
        return n;
    }

    /** v44: play in the snow — a snowball made and thrown, a snowman built, snowflakes caught, a slide down the slope. */
    private float snowFrom(String s, float t, Film.Actor subj, List<Film.Actor> who, List<Film.Actor> group) {
        if (Txt.has(s, SNOW_GROUND) || Txt.has(s, SNOWBALL) || Txt.has(s, SNOWMAN) || snowSlide(s)) { if (Sets.outdoorSet(seg.set)) seg.snowGround = true; }
        if (subj == null || negated(s) || subj.look == null || !subj.look.isHumanoid()) return 0;
        float d = 0;
        if (Txt.has(s, SNOWBALL)) {
            for (Film.Actor a : who) {
                if (a.look == null || !a.look.isHumanoid()) continue;
                Film.Actor to = null;
                for (Film.Actor o : group) if (o != a && !who.contains(o) && o.c != null && Txt.has(s, o.c.displayName)) to = o;
                if (to == null) for (Film.Actor o : seg.actors) if (o != a && o.look != null && o.look.isHumanoid() && o.stateAt(t).visible && (to == null || Math.abs(xAt(o, t) - xAt(a, t)) < Math.abs(xAt(to, t) - xAt(a, t)))) to = o;
                Film.Act act = new Film.Act(t + 0.1f, t + 2.2f, Film.G_SNOW_PLAY); act.item = Film.SNOW_BALL; act.target = to;
                a.acts.add(act);
                float x = xAt(a, t), face = to != null ? (xAt(to, t) >= x ? 1 : -1) : (a.stateAt(t).facing >= 0 ? 1 : -1);
                keyAt(a, t + 0.1f).facing = face;
                float rel = t + 1.45f;
                Film.Fx f = fx(Film.FX_THROW, rel, rel + 3.0f, to != null ? xAt(to, rel + 0.9f) : Math.max(60, Math.min(1220, x + face * 340)), ground - (to != null ? 230 : 0), a, to);
                f.kind = 4;
                film.sfx.add(sfxAt(Film.SFX_WHOOSH, rel, 0.5f, 0.28f, a));
                film.sfx.add(sfxAt(Film.SFX_SNOW, rel + 0.85f, 0.4f, 0.4f, a));
                if (to != null) {
                    to.acts.add(new Film.Act(rel + 0.85f, rel + 1.4f, Film.G_FLAIL));
                    Film.Key hk = keyAt(to, rel + 0.9f); hk.emotion = Pose.SURPRISED;
                    Film.Key lk = keyAt(to, rel + 1.6f); lk.emotion = Pose.LAUGH;
                }
                snowPlays++;
            }
            d = Math.max(d, 3.6f);
        }
        if (Txt.has(s, SNOWMAN) && !Txt.has(s, "पिघल", "melts")) {
            Film.Actor a = subj;
            float x = xAt(a, t), face = a.stateAt(t).facing >= 0 ? 1 : -1;
            float sx = Math.max(140, Math.min(1140, x + face * 0.55f * heightOf(a)));
            boolean have = false;
            for (Film.Fx o : seg.fx) if (o.type == Film.FX_SNOWMAN && o.t1 > t) have = true;
            if (!have) { Film.Fx f = fx(Film.FX_SNOWMAN, t + 0.3f, 1e6f, sx, 0, a, null); f.t2 = t + 5.2f; }
            for (Film.Actor b : who) {
                if (b.look == null || !b.look.isHumanoid()) continue;
                Film.Act act = new Film.Act(t + 0.2f, t + 5.4f, Film.G_SNOW_PLAY); act.item = Film.SNOW_MAN;
                b.acts.add(act);
                keyAt(b, t + 0.2f).facing = sx >= xAt(b, t) ? 1 : -1;
                Film.Key hk = keyAt(b, t + 5.4f); hk.emotion = Pose.HAPPY;
            }
            for (float st = t + 0.6f; st < t + 5f; st += 1.1f) film.sfx.add(sfxAt(Film.SFX_SNOW, st, 0.4f, 0.22f, a));
            snowPlays++;
            d = Math.max(d, 5.6f);
        }
        if (Txt.has(s, SNOW_CATCH)) {
            for (Film.Actor a : who) {
                if (a.look == null || !a.look.isHumanoid()) continue;
                Film.Act act = new Film.Act(t + 0.1f, t + 3.2f, Film.G_SNOW_PLAY); act.item = Film.SNOW_CATCH;
                a.acts.add(act);
                Film.Key k = keyAt(a, t + 0.1f); k.emotion = Pose.HAPPY;
            }
            if (wOpen[Film.W_SNOW] < 0) open(Film.W_SNOW, t, 0.7f);
            snowPlays++;
            d = Math.max(d, 3.3f);
        }
        if (snowSlide(s)) {
            for (Film.Actor a : who) {
                Film.Key now = a.stateAt(t);
                if (a.look == null || !a.look.isHumanoid() || now.anchor != Film.A_GROUND) continue;
                float x = xAt(a, t), dest = x < 640 ? Math.min(1120, x + 420) : Math.max(160, x - 420);
                Film.Act act = new Film.Act(t + 0.2f, t + 2.6f, Film.G_SNOW_PLAY); act.item = Film.SNOW_SLIDE;
                a.acts.add(act);
                Film.Key sit = keyAt(a, t + 0.2f); sit.body = Pose.SIT; sit.seat = Film.SEAT_FLOOR; sit.facing = dest > x ? 1 : -1; sit.emotion = Pose.LAUGH;
                Film.Key go = keyAt(a, t + 0.6f); go.x = dest; go.moveDur = 1.7f; go.facing = dest > x ? 1 : -1;
                Film.Key up = keyAt(a, t + 2.6f); up.body = Pose.STAND; up.seat = -1;
                film.sfx.add(sfxAt(Film.SFX_WHOOSH, t + 0.6f, 1.6f, 0.3f, a));
                snowPlays++;
            }
            d = Math.max(d, 3.1f);
        }
        return d;
    }

    /** v44: animals of the place, by the words for them: {species, words…}. */
    static final Object[][] ANIMALS = {
        {Look.SP_DOG, new String[]{"कुत्ता", "कुत्ते", "कुतिया", "पिल्ला", "पिल्ले", "dog", "dogs", "puppy", "puppies", "doggy"}},
        {Look.SP_CAT, new String[]{"बिल्ली", "बिल्लियाँ", "बिलौटा", "cat", "cats", "kitten", "kitty"}},
        {Look.SP_COW, new String[]{"गाय", "गायों", "गायें", "बछड़ा", "बछड़े", "भैंस", "cow", "cows", "calf", "buffalo", "cattle"}},
        {Look.SP_GOAT, new String[]{"बकरी", "बकरा", "बकरियों", "बकरियाँ", "goat", "goats", "kid goat"}},
        {Look.SP_HEN, new String[]{"मुर्गी", "मुर्गियों", "मुर्गियाँ", "मुर्गा", "मुर्गे", "चूज़े", "चूजे", "hen", "hens", "chicken", "chickens", "rooster", "chicks"}},
        {Look.SP_HORSE, new String[]{"घोड़ा", "घोड़े", "घोड़ी", "horse", "pony", "mare"}},
        {Look.SP_DEER, new String[]{"हिरण", "हिरन", "हिरणों", "deer", "fawn", "stag"}},
        {Look.SP_RABBIT, new String[]{"खरगोश", "rabbit", "bunny"}},
        {Look.SP_DUCK, new String[]{"बत्तख", "बतख", "duck", "ducks"}},
        {Look.SP_PARROT, new String[]{"तोता", "तोते", "parrot"}},
        {Look.SP_PEACOCK, new String[]{"मोर", "peacock"}},
    };

    /**
     * v44: an animal the direction names that is not one of the cast: it is there, at the place — by the one who feeds,
     * pats or milks it, else on free ground — and it barks, moos, meows, clucks or bleats when the words say so, runs
     * off when they do, walks across when it walks.
     */
    private float animalsFrom(String s, float t, Film.Actor subj, List<Film.Actor> who) {
        if (seg == null || negated(s)) return 0;
        float d = 0;
        for (Object[] row : ANIMALS) {
            int sp = (Integer) row[0];
            String[] words = (String[]) row[1];
            if (!term(s, words)) continue;
            // (the cast's own animal of that kind is the one meant; a horse ridden is the ride's)
            boolean cast = false;
            for (Film.Actor a : seg.actors) if (a.look != null && a.look.species == sp) cast = true;
            if (cast) continue;
            if (sp == Look.SP_HORSE && subj != null && subj.look != null && vehicleIn(s, subj.look.isChild()) == Film.V_HORSE) continue;
            if (sp == Look.SP_PEACOCK && !Txt.has(s, "मोर ", "मोर।", "मोर,", "peacock")) continue;
            if (subj != null && subj.look != null && vehicleIn(s, subj.look.isChild()) != 0 && mishapIn(s, vehicleIn(s, subj.look.isChild())) != 0) continue;   // (the ride's own)
            boolean feed = Txt.has(s, "खिला", "चारा", "दाना", "feeds", "feeding", "gives food", "रोटी डाल"), pat = Txt.has(s, "सहला", "पुचकार", "pats", "strokes", "pets ", "cuddles");
            boolean milk = Txt.has(s, "दुह", "दूध निकाल", "milks", "milking");
            boolean near = subj != null && (feed || pat || milk) && subj.look != null && subj.look.isHumanoid();
            Film.Fx f = null;
            for (Film.Fx o : seg.fx) if (o.type == Film.FX_ANIMAL && o.kind == sp && o.t1 > t && o.t2 > t) f = o;
            float x;
            if (f == null) {
                if (near) {
                    float sx = xAt(subj, t), face = subj.stateAt(t).facing >= 0 ? 1 : -1;
                    x = Math.max(120, Math.min(1160, sx + face * (sp == Look.SP_COW || sp == Look.SP_HORSE ? 0.75f : 0.5f) * heightOf(subj)));
                } else x = freeSpot(t);
                f = fx(Film.FX_ANIMAL, t, 1e6f, x, 0, subj, null);
                f.kind = sp; f.t2 = 1e6f;
                f.color = subj != null && xAt(subj, t) < x ? -1 : 1;
                animalsShown++;
            }
            x = f.x;
            // walking across, running off
            if (Txt.has(s, "चलती", "चलता", "चलते", "घूम", "walks", "wanders", "strolls", "crosses", "पार")) { f.y = 2; f.t2 = t + 0.3f; f.t1 = Math.max(f.t1 == 1e6f ? 0 : f.t1, t + 6f); }
            if (Txt.has(s, "भाग", "भग", "runs away", "runs off", "flees", "bolts", "scampers", "darts", "छलाँग", "छलांग")) {
                // v44: shot at as it flees ("तीर चलाता है पर हिरण भाग जाता है"): it bolts as the arrow leaves the bow
                float shotAt = -1;
                for (Film.Fx o : seg.fx) if (o.type == Film.FX_ARROW && o.t0 >= t && (shotAt < 0 || o.t0 < shotAt)) shotAt = o.t0;
                float b = shotAt > 0 ? shotAt + 0.05f : t + 0.3f;
                f.y = 0; f.t2 = b; f.t1 = b + 3f;
            }
            // their voices
            int voice = sp == Look.SP_DOG ? Film.SFX_BARK : sp == Look.SP_COW ? Film.SFX_MOO : sp == Look.SP_CAT ? Film.SFX_MEOW : sp == Look.SP_HEN ? Film.SFX_CLUCK
                    : sp == Look.SP_GOAT ? Film.SFX_BLEAT : sp == Look.SP_HORSE ? Film.SFX_NEIGH : -1;
            if (voice >= 0 && (Txt.has(s, "भौंक", "रंभा", "म्याऊ", "म्याऊँ", "कुकड़ू", "मिमिया", "हिनहिना", "barks", "barking", "woof", "moos", "mooing", "meows", "mewing", "purrs", "clucks",
                    "crows", "bleats", "neighs", "whinnies") || feed)) film.sfx.add(sfxAt(voice, t + 0.3f, 1.4f, 0.38f, subj));
            // the one with it: down to a small animal to feed or pat it, beside the cow to milk it
            if (near) {
                boolean small = sp == Look.SP_DOG || sp == Look.SP_CAT || sp == Look.SP_HEN || sp == Look.SP_RABBIT || sp == Look.SP_DUCK || sp == Look.SP_GOAT;
                Film.Key k = keyAt(subj, t + 0.2f); k.facing = x >= xAt(subj, t) ? 1 : -1; k.emotion = Pose.HAPPY;
                if ((small || milk) && subj.stateAt(t).body == Pose.STAND) {
                    Film.Key c = keyAt(subj, t + 0.4f); c.body = Pose.CROUCH;
                    Film.Key up = keyAt(subj, t + (milk ? 4.2f : 2.6f)); up.body = Pose.STAND;
                }
                boolean has = false;
                for (Film.Act x2 : subj.acts) if ((x2.type == Film.G_PAT || x2.type == Film.G_REACH || x2.type == Film.G_THROW) && Math.abs(x2.t0 - t) < 0.6f) has = true;
                if (!has) subj.acts.add(new Film.Act(t + 0.5f, t + (milk ? 4.0f : 2.4f), pat || milk ? Film.G_PAT : Film.G_REACH));
                if (milk) film.sfx.add(sfxAt(Film.SFX_POUR, t + 0.8f, 3.0f, 0.2f, subj));
                d = Math.max(d, milk ? 4.4f : 2.8f);
            } else d = Math.max(d, 1.6f);
        }
        return d;
    }

    /**
     * v44: whether one who got off a ride has done anything since but talk, listen or nod (a walk, another act, sitting
     * down): if not, a later ride of the same vehicle carries straight on from the first.
     */
    private boolean busySince(Film.Actor a, Film.Act r, float t) {
        List<Object[]> outs = outKeys.get(r);
        for (Film.Act x : a.acts)
            if (x != r && x.t0 > r.t1 - 0.05f && x.t0 < t && x.type != Film.G_TALK && x.type != Film.G_LISTEN && x.type != Film.G_SIGN && !gestureOnSeat(x.type)) return true;
        Film.Key prev = null;
        for (Film.Key k : a.keys) {
            if (k.t > r.t1 + 0.05f && k.t < t && prev != null) {
                boolean out = false;
                if (outs != null) for (Object[] o : outs) if (o[1] == k) out = true;
                if (!out && (k.moveDur > 0 || k.body != prev.body || k.visible != prev.visible)) return true;
            }
            prev = k;
        }
        return false;
    }

    /** v44: the grown-up on the stage who would drive the family's car (a father first, then a mother), or null. */
    private Film.Actor likelyDriver(float t, List<Film.Actor> not) {
        Film.Actor best = null;
        int bestRank = 0;
        for (Film.Actor o : seg.actors) {
            if (not.contains(o) || o.look == null || !o.look.isHumanoid() || o.look.isChild() || !o.stateAt(t).visible || o.c == null) continue;
            String who = (o.c.displayName + " " + o.c.fullName + " " + o.c.role + " " + o.c.description).toLowerCase(java.util.Locale.ROOT);
            int rank = Txt.has(who, "पापा", "पिता", "पिताजी", "बाबूजी", "father", "dad", "papa", "daddy", "पति", "husband") ? 3
                    : Txt.has(who, "माँ", "मां", "मम्मी", "माता", "mother", "mom", "mum", "mummy", "पत्नी", "wife") ? 2 : 1;
            if (rank > bestRank) { best = o; bestRank = rank; }
        }
        return best;
    }

    /** v44: what goes wrong on this ride, if anything (a two-wheeler or a horse that crashes falls instead). */
    static int mishapIn(String s, int v) {
        boolean two = v == Film.V_BICYCLE || v == Film.V_MOTORBIKE || v == Film.V_SCOOTER || v == Film.V_HORSE;
        if (Txt.has(s, CRASH_WORDS) && !Txt.has(s, SWERVE_WORDS)) return two ? Film.MISHAP_SKID : Film.MISHAP_CRASH;
        if (two && Txt.has(s, SKID_WORDS) && !Txt.has(s, "falls asleep", "नींद", "falls in love")) return Film.MISHAP_SKID;
        if (Txt.has(s, SWERVE_WORDS) && v != Film.V_HORSE && v != Film.V_ELEPHANT) return Film.MISHAP_SWERVE;
        return Film.MISHAP_NONE;
    }

    /** v44: words that put one at the wheel of a car. */
    static final String[] DRIVES = {"चलाता", "चलाती", "चलाते", "चला रह", "चलाकर", "ड्राइव", "ड्राइविंग", "स्टीयरिंग", "drives", "driving", "drove", "at the wheel",
            "behind the wheel", "is driving", "takes the wheel"};
    /** v44: a seat in front beside the driver, or in the back. */
    static final String[] FRONT_SEAT = {"आगे की सीट", "आगे वाली सीट", "अगली सीट", "आगे बैठ", "बगल वाली सीट", "बगल की सीट", "बगल में बैठ", "उनके बगल", "उसके बगल",
            "ड्राइवर के बगल", "ड्राइवर के साथ वाली", "front seat", "in the front", "in front with", "passenger seat", "beside him in front", "beside her in front", "next to the driver",
            "up front", "shotgun"};
    static final String[] BACK_SEAT = {"पीछे की सीट", "पिछली सीट", "पीछे वाली सीट", "पीछे बैठ", "पीछे की ओर बैठ", "back seat", "backseat", "in the back", "rear seat", "at the back"};

    static int carSeat(String clause) {
        if (clause == null) return Film.CAR_AUTO;
        if (Txt.has(clause, FRONT_SEAT)) return Film.CAR_FRONT;
        if (Txt.has(clause, BACK_SEAT)) return Film.CAR_BACK;
        return Film.CAR_AUTO;
    }

    /**
     * v44: the part of a sentence about this one (split at commas, semicolons, colons, "और", "and", "while"): where
     * they are named, not as the one gone with or sat beside ("पापा के साथ", "with Mother") — else wherever they are named.
     */
    static String seatClause(String s, Film.Actor a) {
        if (a == null || a.c == null) return null;
        List<String> names = new ArrayList<String>(a.c.aliases);
        if (a.c.displayName != null) names.add(a.c.displayName);
        if (a.c.fullName != null) names.add(a.c.fullName);
        String any = null;
        for (String c : s.split("[,;:।]| और | तथा | एवं | and | while | whereas | as ")) {
            String n = Txt.norm(c);
            for (String nm : names) {
                String x = Txt.norm(nm);
                if (x.length() < 2) continue;
                int k = n.indexOf(x);
                if (k < 0) continue;
                if (!objectMarked(n, k, k + x.length())) return c;
                if (any == null) any = "";            // (named only as the one gone with: not their seat, not their wheel)
            }
        }
        return any;
    }

    /** v44: animals that are ridden. */
    static final String[] HORSE = {"घोड़े", "घोड़ा", "घोड़ी", "घोडे", "घोडा", "घुड़सवार", "horse", "horses", "steed", "pony", "stallion", "mare"};
    static final String[] ELEPHANT_RIDE = {"हाथी पर", "हाथी की पीठ", "हाथी की सवारी", "हाथी पे", "rides an elephant", "on an elephant", "on the elephant", "elephant ride",
            "rides the elephant", "on his elephant", "on her elephant"};

    public static int vehicleIn(String s, boolean child) {
        boolean verb = Txt.has(s, RIDE_VERBS) || Txt.has(s, CRASH_WORDS) || Txt.has(s, SWERVE_WORDS)
                || Txt.has(s, "सरपट", "दौड़ाता", "दौड़ाती", "दौड़ाते", "gallop", "trots", "mounts", "saddle", "घुड़सवारी")
                || (Txt.has(s, SKID_WORDS) && (term(s, SCOOTER) || term(s, "बाइक", "bike") || Txt.has(s, BICYCLE) || Txt.has(s, "मोटरसाइकिल", "motorcycle", "motorbike")));
        if (!verb) return 0;
        // v44: a horse or an elephant ridden (not one fed, patted or led by the reins)
        if (term(s, HORSE) && !Txt.has(s, "को चारा", "को खिला", "को सहला", "feeds the horse", "pats", "strokes", "को पानी")) return Film.V_HORSE;
        if (Txt.has(s, ELEPHANT_RIDE)) return Film.V_ELEPHANT;
        if (term(s, "बस") && Txt.has(s, "बस चला", "बस में", "बस से", "बस पर", "बस स्टॉप", "बस पकड़")) return Film.V_BUS;
        if (term(s, "bus", "buses")) return Film.V_BUS;
        if (term(s, AUTO_RICKSHAW)) return Film.V_AUTO;
        if (term(s, CAR)) return child && !Txt.has(s, "में", " in ", " into ", "से आ", "से उतर", "comes by", "out of") ? 0 : Film.V_CAR;
        if (term(s, SCOOTER)) return Film.V_SCOOTER;
        // v44: a car named only by what is done in it — driving, its seats, its wheel, its brakes ("पापा ड्राइव करते हैं",
        // "sits in the front passenger seat while Mother drives", "slams on the brakes")
        boolean noOther = !Txt.has(s, BICYCLE) && !term(s, "बाइक", "bike", "बुलेट") && !Txt.has(s, "मोटरसाइकिल", "motorcycle", "motorbike", "ट्रैक्टर", "tractor", "truck", "ट्रक")
                && !term(s, HORSE) && !Txt.has(s, "नाव", "boat", "ship", "जहाज़", "train", "ट्रेन", "plane", "विमान", "हवाई");
        if (noOther && !child && (Txt.has(s, "ड्राइव", "स्टीयरिंग", "एक्सीलेरेटर", "steering", "accelerator", "at the wheel", "behind the wheel")
                || Txt.hasWord(s, "drives", "driving", "drove", "brakes", "brake")))
            return Film.V_CAR;
        if (noOther && Txt.has(s, "आगे की सीट", "आगे वाली सीट", "पीछे की सीट", "पिछली सीट", "बगल वाली सीट", "back seat", "front seat", "passenger seat", "backseat"))
            return Film.V_CAR;
        if (Txt.has(s, "मोटरसाइकिल", "मोटर साइकिल", "motorcycle", "motorbike") || term(s, "बाइक", "बुलेट", "मोटरबाइक", "bike")) return child ? Film.V_BICYCLE : Film.V_MOTORBIKE;
        if (Txt.has(s, BICYCLE)) return Film.V_BICYCLE;
        return 0;
    }

    // ================================================================== v41: the playground as part of the place

    /** v41: this part's swing and slide (null when it has none) and where the slide's parts are. */
    private Film.Fx swingProp, slideProp;
    private Props.SlideGeo slideGeo;
    /** v41: who is on the swing now (their last act on it); where each one is on the slide (SL_*) and their act at its top. */
    private final Map<Film.Actor, Film.Act> onSwing = new HashMap<Film.Actor, Film.Act>();
    private final Map<Film.Actor, Integer> slideAt = new HashMap<Film.Actor, Integer>();
    private final Map<Film.Actor, Film.Act> onSlide = new HashMap<Film.Actor, Film.Act>();
    private static final int SL_FOOT = 1, SL_DECK = 2, SL_SEATED = 3, SL_OFF = 4;
    /** v41: the sentence was staged on the swing or the slide: no sitting down on the grass, no walk of its own. */
    private boolean playStaged;
    /** v41: how long the getting off the swing takes before the sentence's walk may start. */
    private float playDelay;
    /** v41: how long after the sentence's start a walk it names waits (the hug before it is over). */
    private float travelAfter;
    int umbrellasTaken, cameBackToSpeak, arrivalsByRide, splitSentences;
    /** v41: the sentence being staged is one of the playground (playKind): its sitting is on the seat of the swing or the top of the slide. */
    private int playSentence;
    int swingRides, swingChanges, slideClimbs, slideRuns, playProps, emptySwingShots, groundThings, stoodToWalk, familyRides, handsHeld, puddleJumps, groupHugs;
    /** v43: rides that followed another's, vehicles turned round before the family got in; riders who only got on, who
     *  arrived on it, who rode on; hired vehicles that drove up or away. */
    int following, uTurns, stays, arrivals, rodeOn, hiredMoves;
    /** v44: drivers picked out of a car's riders, passengers seated in front and in the back. */
    int carDriversChosen, frontSeats, backSeats;
    /** v44: mishaps staged, falls, close inserts, weapons used from the saddle. */
    int mishaps, falls, inserts, mountedWeapons, grownUpDrinks, toasts, duels, swordPlays, arrows, archeryActs, snowPlays, animalsShown;
    /** v43: the keys a ride sets (its start, its move, its turn round, its going out of the picture): the only ones that
     *  may turn a rider while the ride runs (steadyRiders). */
    private final Set<Film.Key> rideKeys = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Film.Key, Boolean>());
    /** v43: the keys that walk riders apart after a ride (by the lead's ride): taken back when the ride goes on. */
    private final Map<Film.Act, List<Object[]>> outKeys = new java.util.IdentityHashMap<Film.Act, List<Object[]>>();

    static final String[] SWING_NOUN = {"झूला", "झूले", "झूलों", "the swing", "a swing", "the swings", "swing set", "swingset", "swing seat"};
    static final String[] SLIDE_NOUN = {"फिसलपट्टी", "फिसल पट्टी", "फिसलन पट्टी", "स्लाइड", "the slide", "a slide", "the slides", "slide's", "down the slide"};
    static final String[] SWING_GO_ON = {"बैठ", "चढ़", "sits on", "sit on", "sat on", "gets on", "got on", "jumps on", "hops on", "climbs on", "jumped on", "hopped on", "climbed on"};
    static final String[] SWING_VERB = {"झूलना", "झूलने", "झूलता", "झूलती", "झूलते", "झूल रह", "झूलकर", "झूल कर", "झूलूँ", "झूलूं", "झूला झूल", "झूले झूल",
            "swings", "swinging", "swing higher", "swing high", "swung"};
    static final String[] STOP_SWING = {"रोक", "रुक", "थम", "धीमा कर", "धीमे कर", "stops", "stopped", "slows", "slowed", "comes to a stop"};
    static final String[] GET_OFF = {"उतर", "कूद कर नीचे", "कूदकर नीचे", "jumps off", "jumped off", "gets off", "got off", "hops off", "hopped off", "climbs off"};
    static final String[] CLIMB = {"चढ़", "climbs", "climbed", "climbing", "goes up the", "went up the", "runs up the"};
    static final String[] LADDER = {"सीढ़ी", "सीढ़ियाँ", "सीढ़ियां", "सीढ़ियों", "ladder", "steps", "stairs"};
    static final String[] TUMBLE = {"गिर", "लुढ़क", "falls", "fell", "tumbles", "tumbled", "rolls over", "lands on the grass"};

    /** v41: a key at t, put in its place among the keys when later ones are already there. */
    private static Film.Key keyAt(Film.Actor a, float t) { return t >= a.last().t - 1e-4f ? a.at(t) : insertKey(a, t); }

    /**
     * v41: a key put in among later ones (made by an earlier rule of the sentence: a laugh, a turn): what it changes —
     * where they are, sitting or lying, the seat, the way they face — holds on in the later keys that still had the
     * old state, until one of them changes it (they used to snap back: off the swing and back on its seat).
     */
    static void spread(Film.Actor a, Film.Key k) {
        int i = a.keys.indexOf(k);
        if (i <= 0 || i >= a.keys.size() - 1) return;
        Film.Key p = a.keys.get(i - 1);
        boolean x = Math.abs(k.x - p.x) > 0.5f, body = k.body != p.body, seat = k.seat != p.seat, face = k.facing != p.facing;
        for (int j = i + 1; j < a.keys.size() && (x || body || seat || face); j++) {
            Film.Key l = a.keys.get(j);
            if (x) { if (l.moveDur == 0 && Math.abs(l.x - p.x) < 0.5f) l.x = k.x; else x = false; }
            if (body) { if (l.body == p.body) l.body = k.body; else body = false; }
            if (seat) { if (l.seat == p.seat) l.seat = k.seat; else seat = false; }
            if (face) { if (l.facing == p.facing) l.facing = k.facing; else face = false; }
        }
    }

    /**
     * v41: a key made for a time before the actor's last key (a turn to the one who speaks while a feeling made earlier
     * still runs out, a step after a hug) was added at the end of the list, so its change showed only after that later
     * key (up to 3 s late) or jumped there. It is put in its place in time: a copy of the state then, with what it
     * changed (from the key it was copied from), held on in the later keys that still had the old state.
     */
    static void orderKeys(Film.Actor a) {
        for (int i = 1; i < a.keys.size(); i++) {
            Film.Key k = a.keys.get(i), l = a.keys.get(i - 1);
            if (k.t >= l.t - 1e-3f) continue;
            a.keys.remove(i);
            Film.Key in = insertKey(a, k.t);
            if (Math.abs(k.x - l.x) > 0.5f || k.moveDur > 0) { in.x = k.x; in.moveDur = k.moveDur; in.run = k.run; }
            if (k.body != l.body) in.body = k.body;
            if (k.seat != l.seat) in.seat = k.seat;
            if (k.facing != l.facing) in.facing = k.facing;
            if (k.emotion != l.emotion) in.emotion = k.emotion;
            if (k.visible != l.visible) in.visible = k.visible;
            if (k.backTurned != l.backTurned) in.backTurned = k.backTurned;
            if (k.eyesShut != l.eyesShut) in.eyesShut = k.eyesShut;
            if (k.holdR != l.holdR) in.holdR = k.holdR;
            if (k.holdL != l.holdL) in.holdL = k.holdL;
            if (k.anchor != l.anchor || k.anchorActor != l.anchorActor) { in.anchor = k.anchor; in.anchorActor = k.anchorActor; }
            if (k.depth != l.depth) in.depth = k.depth;
            if (k.tears != l.tears) in.tears = k.tears;
            if (k.sweat != l.sweat) in.sweat = k.sweat;
            if (k.costume != l.costume) in.costume = k.costume;
            spread(a, in);
            i = 0;
        }
    }

    /** v41: the words of sliding down (not slipping: "फिसल गया" on a peel is a fall). */
    static boolean slidePlay(String s) { return Txt.has(s, SLIDE_PLAY) && !Txt.has(s, "फिसल गय", "फिसल ग", "slipped"); }

    /** v41: where in the sentence a word of the list comes last (-1: none). */
    private static int lastAt(String s, String[] words) {
        String n = Txt.norm(s);
        int best = -1;
        for (String w : words) best = Math.max(best, n.lastIndexOf(Txt.norm(w)));
        return best;
    }

    /**
     * v41: a swing or a slide that the part's place or action names stands there from its first frame, not only while
     * someone plays on it: the slide toward the left with its chute running toward the middle, the swing beyond it, both
     * sized for the child of the part (the smallest one on the stage). The slide is the user's own picture of it when
     * they gave one that shows it from the side.
     */
    private void playgroundProps(Story.Scene sc, int b0, int b1, String where) {
        swingProp = slideProp = null;
        slideGeo = null;
        onSwing.clear(); slideAt.clear(); onSlide.clear();
        StringBuilder sb = new StringBuilder(where);
        for (int bi = b0; bi < b1; bi++) if (sc.beats.get(bi).type == Story.Beat.DIRECTION) sb.append(" । ").append(sc.beats.get(bi).text);
        String s = sb.toString(), noRound = s;
        for (String w : ROUNDABOUT) noRound = noRound.replace(w, " ");
        boolean swing = Txt.has(noRound, SWING_NOUN), slide = Txt.has(s, SLIDE_NOUN);
        if (!swing && !slide) return;
        Film.Actor kid = null;
        for (Film.Actor a : seg.actors) {
            if (a.look == null || !a.look.isHumanoid()) continue;
            if (kid == null || (a.look.isChild() && !kid.look.isChild()) || (a.look.isChild() == kid.look.isChild() && heightOf(a) < heightOf(kid))) kid = a;
        }
        if (kid == null) return;
        float h = heightOf(kid);
        if (slide) {
            slideGeo = PlayPics.slide(art);
            float ax = 100 - slideGeo.footX * h;
            if (!swing) ax = Math.max(ax, 360);
            slideProp = fx(Film.FX_SLIDE, seg.t0, 1e6f, ax, 0, kid, null);
            slideProp.kind = 1;
            playProps++;
        }
        if (swing) {
            float sx = slide ? slideProp.x + (slideGeo.landX + 0.6f) * h + 0.6f * h : 860;
            sx = Math.min(1280 - 0.62f * h - 30, sx);
            swingProp = fx(Film.FX_SWING, seg.t0, 1e6f, sx, 0, kid, null);
            swingProp.kind = sx > 640 ? -1 : 1;           // on it one faces toward the middle of the place
            playProps++;
        }
        film.notes.add("  ↳ the playground: " + (slide ? "the slide" + (slideGeo.pic != null ? " (your picture)" : "") : "") + (slide && swing ? " and " : "")
                + (swing ? "the swing" + (PlayPics.seat(art) != null ? " (your picture of its seat)" : "") : "") + " stand in the place from the first frame");
    }

    /**
     * v41: one on the playground at t — how far up they are (stage px: on the ladder, the platform, the chute) and
     * how much they move there (the swing's reach in degrees; sliding down counts as fast); null when not on it.
     */
    private float[] playAt(Film.Actor a, float t) {
        // still on the swing or up on the slide (its act is made longer when the story goes on with it)
        Film.Act open = onSwing.get(a);
        if (open != null && open.prop != null && t >= open.t0) return new float[]{0, Math.max(Math.abs(open.a0), Math.abs(open.a1))};
        open = onSlide.get(a);
        if (open != null && open.prop != null && t >= open.t0) {
            Props.SlideGeo g = slideGeo != null ? slideGeo : PlayPics.slide(art);
            return new float[]{g.deckY * heightOf(open.prop.a != null ? open.prop.a : a), 0};
        }
        for (Film.Act x : a.acts) {
            if (x.type != Film.G_PLAYGROUND || x.prop == null || t < x.t0 || t >= x.t1) continue;
            float hs = heightOf(x.prop.a != null ? x.prop.a : a);
            if (x.prop.type == Film.FX_SWING) return new float[]{0, Math.max(Math.abs(x.a0), Math.abs(x.a1))};
            Props.SlideGeo g = slideGeo != null ? slideGeo : PlayPics.slide(art);
            float u = Math.max(0, Math.min(1, (t - x.t0) / Math.max(0.05f, x.t1 - x.t0)));
            if ((x.how & Film.PL_CLIMB) != 0) return new float[]{g.deckY * hs * u, 0};
            if ((x.how & Film.PL_TOP) != 0) return new float[]{g.deckY * hs * ((x.how & Film.PL_SIT) != 0 ? 0.8f : 1f), 0};
            return new float[]{g.deckY * hs * (1 - u) * 0.8f, 30};
        }
        return null;
    }

    /**
     * v41: no close shot of one swinging high or sliding down — a close frame loses them in the swing's arc: every cut
     * on them (the line's close-ups, the cut-ins that keep shots short) becomes a medium shot of the swing or the slide
     * with them in it.
     */
    private void playFrames() {
        Film.Seg keepSeg = seg;
        float keepGround = ground;
        for (Film.Seg sg : film.segs) {
            if (sg.type != Film.S_SCENE) continue;
            seg = sg; ground = sg.ground;
            for (Film.Cam c : sg.cams) {
                if (c.zoom <= 1.12f) continue;
                Film.Actor who = nearestTo(sg, c.cx, c.t + 0.1f);
                if (who == null) continue;
                Film.Fx prop = null;
                for (float dt = 0.1f; dt < 2.6f && prop == null; dt += 0.5f) {
                    float tt = c.t + dt;
                    for (Film.Act x : who.acts) if (x.type == Film.G_PLAYGROUND && x.prop != null && tt >= x.t0 && tt < x.t1) {
                        boolean fast = x.prop.type == Film.FX_SWING ? Math.max(Math.abs(x.a0), Math.abs(x.a1)) > 6 : (x.how & Film.PL_DOWN) != 0;
                        if (fast) prop = x.prop;
                    }
                }
                if (prop == null) continue;
                float h = heightOf(prop.a != null ? prop.a : who);
                c.zoom = 1.1f;
                float hw = 640f / c.zoom, hh = 360f / c.zoom;
                float cx = prop.type == Film.FX_SWING ? prop.x : prop.x + prop.kind * h * 0.5f;
                c.cx = Math.max(hw, Math.min(1280 - hw, cx));
                c.cy = Math.max(hh, Math.min(720 - hh, ground - 0.7f * h));
                playFramed++;
            }
        }
        seg = keepSeg; ground = keepGround;
    }
    int playFramed;

    /** v41: the end of the part: one still on the swing or at the top of the slide stays there until the cut. */
    private void playUntil(float tEnd) {
        for (Film.Act x : onSwing.values()) if (x.t1 < tEnd) x.t1 = tEnd;
        for (Film.Act x : onSlide.values()) if (x.t1 < tEnd) x.t1 = tEnd;
    }

    /** v41: the sentence takes this one somewhere else (a walk, a run, standing up, lying down, another play). */
    private static boolean movesOff(String s) {
        return Txt.has(s, GO_TO) || running(s) || Txt.hasWord(s, WALK_VERBS) || leaving(s) || Txt.has(s, STAND_UP) || Txt.has(s, LIE_DOWN)
                || Txt.has(s, GET_OFF) || Txt.has(s, CROSS) || Txt.has(s, "चल", "जाता", "जाती", "जाते", "walks", "leaves");
    }

    /**
     * v41: the swing and the slide as things one goes to, gets on, plays on for as long as the story says and gets off.
     * On the swing the swinging goes on through the lines spoken on it, faster or slower as the story says; the feet
     * push off, the face turns up to the sky, the hands let go for a moment, the feet drag to stop it and one hops off.
     * The slide is climbed rung by rung, sat on at the top, slid down sitting (or on the belly, head first, the arms up)
     * and left at its end on the grass. Returns the sentence's time (0 when it is not about them).
     */
    private float playFrom(String s, float t, Film.Actor subj, List<Film.Actor> group) {
        playStaged = false;
        int kind = playKind(s, subj);
        if (kind == 0) return 0;
        boolean onS = onSwing.containsKey(subj), swingS = (kind & 1) != 0, slideS = (kind & 2) != 0;
        float d = 0, tt = t;
        if ((kind & 4) != 0) {
            // the sentence takes them elsewhere: off the swing first (their walk, if any, after the hop)
            float off = leaveSwing(subj, tt, true);
            tt += off; d += off;
            playStaged = slideS;
            if (!swingS && !slideS) playDelay = off;
        }
        if (swingS) {
            float sd = swingFrom(s, tt, subj);
            if (sd > 0) { d += sd; tt += sd; playStaged = true; }
        }
        if (slideS) {
            float ld = slideFrom(s, tt, subj);
            if (ld > 0) { d += ld; playStaged = true; }
        }
        return d;
    }

    /** v41: what the sentence does on the playground for this one: 1 on the swing, 2 on the slide, 4 off the swing first; 0 nothing. */
    private int playKind(String s, Film.Actor subj) {
        if (subj == null || subj.look == null || !subj.look.isHumanoid() || negated(s)) return 0;
        if (swingProp == null && slideProp == null) return 0;
        boolean onS = onSwing.containsKey(subj);
        int st = slideAt.containsKey(subj) ? slideAt.get(subj) : 0;
        boolean swingS = swingProp != null && (Txt.has(s, SWING_NOUN) || Txt.has(s, SWING_VERB) || onS && (Txt.has(s, STOP_SWING) || Txt.has(s, GET_OFF)
                || Txt.has(s, "धक्का", "तेज", "धीरे", "ऊँचा", "ऊंचा", "हाथ छोड़", "बिना हाथ", "push", "faster", "higher", "no hands", "lets go")));
        boolean slideS = slideProp != null && (Txt.has(s, SLIDE_NOUN) || slidePlay(s)
                || (Txt.has(s, LADDER) && (Txt.has(s, CLIMB) || st == SL_FOOT || Txt.has(s, GO_TO) || running(s)))
                || (st == SL_FOOT && Txt.has(s, CLIMB)) || ((st == SL_DECK || st == SL_SEATED) && (Txt.has(s, "बैठ", "sits", "sit down") || slidePlay(s)))
                || (st == SL_OFF && Txt.has(s, TUMBLE)));
        // both named ("झूले के पीछे वाली स्लाइड की तरफ भागता है"): the one the sentence ends on is where they go
        if (swingS && slideS) {
            if (lastAt(s, SLIDE_NOUN) >= lastAt(s, SWING_NOUN)) swingS = false; else slideS = false;
        }
        boolean leave = onS && !swingS && (slideS || movesOff(s));
        return (swingS ? 1 : 0) | (slideS ? 2 : 0) | (leave ? 4 : 0);
    }

    /** v41: how far the swing goes for these words (degrees): slowly 12, a plain swing 22, fast 30, faster still 38. */
    static float swingLevel(String s, float now) {
        if (Txt.has(s, "और तेज", "और ऊँचा", "और ऊंचा", "और ऊपर", "और जोर", "ज्यादा तेज", "ज़्यादा तेज", "higher", "faster", "even more")) return Math.min(40, Math.max(now + 8, 36));
        if (Txt.has(s, "धीरे", "slowly", "gently", "हल्के")) return 12;
        if (Txt.has(s, "तेज", "ऊँचा", "ऊंचा", "जोर", "fast", "high", "hard")) return Math.max(now, 30);
        return now > 4 ? now : 22;
    }

    private float swingFrom(String s, float t, Film.Actor a) {
        Film.Fx sw = swingProp;
        float h = heightOf(a), f = sw.kind;
        Film.Act cur = onSwing.get(a);
        boolean play = Txt.has(s, SWING_VERB);
        boolean sitOn = Txt.has(s, SWING_GO_ON) && !Txt.has(s, "के पास बैठ", "near the swing", "beside the swing", "next to the swing");
        boolean stop = Txt.has(s, STOP_SWING), off = Txt.has(s, GET_OFF);
        boolean go = Txt.has(s, GO_TO) || running(s) || Txt.hasWord(s, WALK_VERBS);
        int how = 0;
        if (Txt.has(s, "धक्का", "पैरों से", "push", "pushes", "kicks off")) how |= Film.PL_PUSH;
        if (Txt.has(s, "आसमान", "आकाश", "ऊपर की तरफ देख", "ऊपर देख", "sky", "looks up")) how |= Film.PL_LOOK_UP;
        if (Txt.has(s, "हाथ छोड़", "बिना हाथ", "हाथ ऊपर", "दोनों हाथ", "no hands", "lets go", "let go", "hands up", "arms up", "hands in the air")) how |= Film.PL_HANDS_FREE;
        float d = 0;
        if (cur == null && !(go || sitOn || play) && Txt.has(s, "हिल", "डोल", "झूल रहा", "sway", "moving", "moves", "rocking") && onSwing.isEmpty()) {
            // v41: "झूला अभी भी हवा में हिल रहा है": the empty seat, swaying, seen close — a breath of wind sets it going
            float hp = heightOf(sw.a);
            sw.t2 = t; sw.color = 6;
            Film.Cam c = new Film.Cam(t + 0.1f, Math.min(1280 - 640 / 1.3f, Math.max(640 / 1.3f, sw.x)), Math.max(360 / 1.3f, Math.min(720 - 360 / 1.3f, ground - hp * 0.95f)), 1.3f, 0);
            c.place = true; c.keep = true;
            seg.cams.add(c);
            emptySwingShots++;
            return 2.8f;
        }
        if (cur == null) {
            if (!(go || sitOn || play)) return 0;          // only named or looked at: nobody gets on
            float x = xAt(a, t), tt = t + 0.1f;
            Film.Key k0 = a.stateAt(t);
            if ((k0.body == Pose.SIT || k0.body == Pose.LIE) && Math.abs(x - sw.x) > 12) {
                Film.Key up = keyAt(a, tt); up.body = Pose.STAND; up.seat = -1; spread(a, up);
                tt += k0.body == Pose.LIE ? 1.4f : 0.8f;
            }
            float to = sitOn || play ? sw.x : sw.x - f * 0.62f * h;          // beside it when they only go to it
            if (Math.abs(to - x) > 12) {
                boolean run = running(s) || (a.look.isChild() && !Txt.has(s, "धीरे", "slowly", "चलकर", "चलते"));
                float dur = Math.min(3.6f, Math.max(0.7f, Math.abs(to - x) / ((run ? 260f : 170f) * weightSpeed(a))));
                Film.Key k = keyAt(a, tt); k.x = to; k.moveDur = dur; k.run = run; k.facing = to > x ? 1 : -1;
                tt += dur;
            }
            Film.Key kf = keyAt(a, tt + 0.02f); kf.facing = f;
            if (!(sitOn || play)) { camOn(a, t, 1.0f); return tt - t + 0.6f; }
            // the hop onto the seat: the back to it, a little jump, the seat takes the weight
            Film.Act m = new Film.Act(tt + 0.1f, tt + 4.1f, Film.G_PLAYGROUND);
            m.item = Film.PG_SWING; m.prop = sw; m.since = tt + 0.9f; m.a0 = 0;
            m.a1 = play ? swingLevel(s, 0) : 3;
            m.how = Film.PL_MOUNT | Film.PL_CONT | (play ? how : 0);
            a.acts.add(m);
            onSwing.put(a, m);
            film.sfx.add(sfxAt(Film.SFX_CREAK, tt + 0.7f, 1.4f, 0.2f, a));
            swingRides++;
            camOn(a, tt, 1.05f);
            d = tt - t + 1.0f + (play ? 2.4f : 0.4f);
            if (!(stop || off)) return d;
            return d + leaveSwing(a, t + d, off);
        }
        if (stop || off) return leaveSwing(a, t, off);
        // a change in the swinging: faster, slower, pushing off, the sky, the hands let go
        float tn = t + 0.15f;
        Film.Act n = new Film.Act(tn, tn + 4.0f, Film.G_PLAYGROUND);
        n.item = Film.PG_SWING; n.prop = sw; n.since = cur.since; n.a0 = cur.a1;
        n.a1 = play || how != 0 ? swingLevel(s, cur.a1) : cur.a1;
        n.how = how | Film.PL_CONT;
        cur.t1 = tn;
        cur.how |= Film.PL_CONT;
        a.acts.add(n);
        onSwing.put(a, n);
        if ((how & Film.PL_HANDS_FREE) != 0) { Film.Key k = keyAt(a, tn + 0.6f); k.emotion = Pose.LAUGH; Film.Key k2 = keyAt(a, tn + 3.0f); k2.emotion = Pose.HAPPY; }
        else if (n.a1 > 20) { Film.Key k = keyAt(a, tn); k.emotion = Pose.HAPPY; }
        swingChanges++;
        camOn(a, tn, n.a1 > 25 ? 0.95f : 1.1f);
        return 3.4f;
    }

    /** v41: the swing slows (the shoes scuff the ground) and, getting off, they hop off forward onto their feet; the time it takes. */
    private float leaveSwing(Film.Actor a, float t, boolean off) {
        Film.Act cur = onSwing.get(a);
        if (cur == null) return 0;
        float h = heightOf(a), f = swingProp != null ? swingProp.kind : a.stateAt(t).facing;
        float amp = Math.max(0, cur.a1), ts = Math.max(t + 0.1f, cur.t0 + 0.5f);
        float dur = amp < 4 ? 0.4f : Math.max(1.2f, Math.min(2.6f, amp / 13f));
        cur.t1 = ts;
        cur.how |= Film.PL_CONT;
        Film.Act stop = new Film.Act(ts, ts + dur, Film.G_PLAYGROUND);
        stop.item = Film.PG_SWING; stop.prop = cur.prop; stop.since = cur.since; stop.a0 = amp; stop.a1 = 0;
        stop.how = Film.PL_STOP;
        a.acts.add(stop);
        if (amp >= 4) film.sfx.add(sfxAt(Film.SFX_SWEEP, ts + 0.2f, dur * 0.8f, 0.16f, a));
        if (!off) {
            // stopped, and still sitting on it
            stop.how |= Film.PL_CONT;
            stop.t1 = ts + dur + 3f;
            onSwing.put(a, stop);
            return dur + 0.4f;
        }
        onSwing.remove(a);
        float th = ts + dur;
        Film.Key k = keyAt(a, th + 0.02f); k.body = Pose.STAND; k.seat = -1; k.x = xAt(a, th) + f * 0.42f * h; k.moveDur = 0.4f; k.facing = f; k.run = false;
        spread(a, k);
        a.acts.add(new Film.Act(th, th + 0.45f, Film.G_JUMP));
        film.sfx.add(sfxAt(Film.SFX_THUD, th + 0.4f, 0.3f, 0.2f, a));
        if (swingProp != null) { swingProp.t2 = th; swingProp.color = 7; }      // the empty seat swings on a little after the hop
        return dur + 0.9f;
    }

    private float slideFrom(String s, float t, Film.Actor a) {
        Props.SlideGeo g = slideGeo;
        Film.Fx sl = slideProp;
        float h = heightOf(sl.a), f = sl.kind, X = sl.x;
        int st = slideAt.containsKey(a) ? slideAt.get(a) : 0;
        boolean go = Txt.has(s, GO_TO) || running(s) || Txt.hasWord(s, WALK_VERBS);
        boolean climb = Txt.has(s, CLIMB);
        boolean sit = Txt.has(s, "बैठ", "sits", "sit down", "sat down");
        boolean down = slidePlay(s);
        boolean belly = Txt.has(s, "पेट के बल", "belly", "tummy", "head first", "headfirst", "लेट कर फिसल", "लेटकर फिसल", "लेट कर नीचे");
        boolean arms = Txt.has(s, "हाथ ऊपर", "हाथों को ऊपर", "हाथ उठा", "hands up", "arms up", "hands in the air", "arms in the air");
        boolean count = Txt.has(s, "गिन", "counting", "counts", "एक-एक", "one by one");
        if (st == SL_OFF && Txt.has(s, TUMBLE) && !down && !climb) {
            // off the end and onto the grass: down on the back, laughing
            Film.Key k = keyAt(a, t + 0.25f); k.body = Pose.LIE; k.seat = Film.SEAT_FLOOR; k.emotion = Pose.LAUGH;
            spread(a, k);
            film.sfx.add(sfxAt(Film.SFX_THUD, t + 0.5f, 0.3f, 0.22f, a));
            slideAt.put(a, 0);
            camOn(a, t, 1.2f);
            return 1.5f;
        }
        if (!(go || climb || sit || down)) return 0;          // only named or looked at
        float tt = t + 0.1f;
        if (st == SL_OFF || st == 0) {
            // to the foot of the ladder (up from the grass first)
            Film.Key k0 = a.stateAt(tt);
            if (k0.body == Pose.SIT || k0.body == Pose.LIE) { Film.Key up = keyAt(a, tt); up.body = Pose.STAND; up.seat = -1; spread(a, up); tt += k0.body == Pose.LIE ? 1.4f : 0.8f; }
            float x = xAt(a, tt), to = X + g.footX * h * f;
            if (Math.abs(to - x) > 10) {
                boolean run = running(s) || (a.look.isChild() && !Txt.has(s, "धीरे", "slowly", "चलकर", "चलते"));
                float dur = Math.min(3.6f, Math.max(0.6f, Math.abs(to - x) / ((run ? 260f : 170f) * weightSpeed(a))));
                Film.Key k = keyAt(a, tt); k.x = to; k.moveDur = dur; k.run = run; k.facing = to > x ? 1 : -1;
                camOn(a, tt, 1.0f);
                tt += dur;
            }
            Film.Key kf = keyAt(a, tt + 0.02f); kf.facing = f;
            st = SL_FOOT;
            slideAt.put(a, st);
            if (!(climb || sit || down)) return tt - t + 0.4f;
            tt += 0.25f;
        }
        if (st == SL_FOOT) {
            // up the ladder, rung by rung (counted aloud: slower)
            int rungs = Math.max(4, Math.round(g.deckY / 0.14f));
            float dur = rungs * (count ? 0.6f : 0.4f);
            Film.Act c = new Film.Act(tt, tt + dur, Film.G_PLAYGROUND);
            c.item = Film.PG_SLIDE; c.prop = sl; c.how = Film.PL_CLIMB | (count ? Film.PL_COUNT : 0);
            a.acts.add(c);
            Film.Key k = keyAt(a, tt + 0.01f); k.x = X + g.deckX * h * f; k.moveDur = dur; k.facing = f; k.run = false;
            for (int i = 0; i < rungs; i++) film.sfx.add(sfxAt(Film.SFX_TAP, tt + (i + 0.5f) * dur / rungs, 0.15f, 0.14f, a));
            cam(tt, X + (g.footX + g.deckX) * 0.5f * h * f, ground - h * (0.62f + g.deckY * 0.55f), 0.95f, 0.8f);
            slideClimbs++;
            tt += dur;
            st = SL_DECK;
            slideAt.put(a, st);
            Film.Act top = new Film.Act(tt, tt + 4f, Film.G_PLAYGROUND);
            top.item = Film.PG_SLIDE; top.prop = sl; top.how = Film.PL_TOP | Film.PL_CONT;
            a.acts.add(top);
            onSlide.put(a, top);
            if (!(sit || down)) return tt - t + 0.5f;
        }
        Film.Act top = onSlide.get(a);
        if (st == SL_DECK && (sit || (down && !belly))) {
            // sits down at the top of the chute
            if (top != null) top.t1 = tt;
            Film.Act seat = new Film.Act(tt, tt + 4f, Film.G_PLAYGROUND);
            seat.item = Film.PG_SLIDE; seat.prop = sl; seat.how = Film.PL_TOP | Film.PL_SIT | Film.PL_CONT;
            a.acts.add(seat);
            Film.Key k = keyAt(a, tt + 0.01f); k.x = X + g.sitX * h * f; k.moveDur = 0.5f; k.facing = f; k.run = false;
            onSlide.put(a, seat);
            top = seat;
            st = SL_SEATED;
            slideAt.put(a, st);
            cam(tt, X + g.sitX * h * f + f * 0.3f * h, ground - h * (0.5f + g.deckY * 0.6f), 1.15f, 0.6f);
            tt += 0.8f;
            if (!down) return tt - t + 0.6f;
        }
        if (!down) return tt - t;
        // down the chute: sitting with the arms up, or on the belly head first; off its end onto the grass
        if (top != null) top.t1 = tt;
        onSlide.remove(a);
        float len = (g.endX - g.sitX) * h;
        float dur = Math.max(1.1f, Math.min(2.2f, len / 330f)) + 0.5f;
        Film.Act dn = new Film.Act(tt, tt + dur, Film.G_PLAYGROUND);
        dn.item = Film.PG_SLIDE; dn.prop = sl; dn.how = Film.PL_DOWN | (belly ? Film.PL_BELLY : 0) | (arms || !belly ? Film.PL_ARMS_UP : 0);
        dn.a0 = st == SL_SEATED ? 1 : 0;          // already sitting at the top (else they sit, or lie down, first)
        a.acts.add(dn);
        Film.Key k = keyAt(a, tt + 0.01f); k.x = X + g.landX * h * f; k.moveDur = dur; k.facing = f; k.run = false;
        // how they end up on the grass, set while the slide still decides the pose (lying down or sitting down takes
        // a moment: done before they reach the end, so there is no pop when the slide lets go of them)
        Film.Key kl = keyAt(a, tt + 0.05f);
        kl.body = belly ? Pose.LIE : Pose.SIT; kl.seat = Film.SEAT_FLOOR; kl.facing = belly ? -f : f; kl.emotion = Pose.LAUGH;
        spread(a, kl);
        film.sfx.add(sfxAt(Film.SFX_WHOOSH, tt + 0.4f, dur - 0.5f, 0.4f, a));
        film.sfx.add(sfxAt(Film.SFX_THUD, tt + dur - 0.1f, 0.3f, 0.18f, a));
        cam(tt, X + (g.sitX + g.landX) * 0.5f * h * f, ground - h * (0.55f + g.deckY * 0.3f), 0.9f, 0.6f);
        slideRuns++;
        slideAt.put(a, SL_OFF);
        return tt - t + dur + 0.3f;
    }

    /**
     * v37: activities from a stage direction — riding and driving, the playground, exercise, make-up, a bath (always
     * behind a curtain, from the shoulders up), hugs and a peck on the cheek or the forehead. Returns their time.
     */
    private float activityFrom(String s, float t, Film.Actor subj, List<Film.Actor> who, List<Film.Actor> group) {
        if (subj == null || subj.look == null || !subj.look.isHumanoid() || negated(s)) return 0;
        float d = 0;
        // everyone named in the sentence takes part ("राजू और पिंकी साइकिल चलाते हैं")
        List<Film.Actor> all = new ArrayList<Film.Actor>(who);
        for (Film.Actor a : group) if (!all.contains(a) && a.look != null && a.look.isHumanoid() && Txt.has(s, a.c.displayName)) all.add(a);
        // ---- riding and driving
        // v43: hailing an auto or a taxi ("प्रिया हाथ दिखाकर ऑटो रोकती है", "hails a taxi"): it drives up, its driver at the
        // wheel, stops by them and waits there for them to get in
        if (Txt.has(s, HAIL) && (hailed == null || hailed.t1 < t)) {
            int hv = Txt.has(s, "ऑटो", "रिक्शा", "रिक्शे", "auto", "rickshaw") ? Film.V_AUTO : Film.V_CAR;
            float hx = xAt(subj, t), hdir = hx < 640 ? 1 : -1, hvs = vehiclePx(subj, hv);
            float stop = Math.max(170, Math.min(1110, hx + hdir * 0.35f * hvs));
            Film.Fx up = fx(Film.FX_VEHICLE, t + 0.2f, 1e6f, hdir > 0 ? -1.4f * hvs : 1280 + 1.4f * hvs, stop, subj, null);
            up.t2 = t + 0.2f; up.kind = hv + Film.V_PASSENGER; up.color = (int) hdir;
            hailed = up;
            subj.acts.add(new Film.Act(t, t + 1.4f, Film.G_WAVE));
            film.sfx.add(sfxAt(hv == Film.V_AUTO ? Film.SFX_MOTOR : Film.SFX_ENGINE, t + 0.2f, Film.VEHICLE_MOVE + 0.8f, 0.32f, subj));
            film.sfx.add(sfxAt(Film.SFX_HORN, t + 0.4f, 0.6f, 0.25f, subj));
            frameAround(t + 0.2f, hx - 260, hx + 260);
            hiredMoves++;
            return Film.VEHICLE_MOVE + 0.6f;
        }
        // v43: getting off a bus, an auto or a car nobody was seen riding ("राहुल स्कूल बस से उतरता है", "gets out of the
        // car"): it arrives with them in it, stops, and they get out — staged as the arrival it is
        if (Txt.has(s, "से उतर", "gets off", "gets out of", "got off", "got out of", "steps off", "steps out of", "climbs out of", "hops off", "hops out of")) {
            int gv = vehicleIn(s + " से आ comes by", subj.look.isChild());
            boolean rodeHere = false;
            for (Film.Act x : subj.acts) if (x.type == Film.G_RIDE && x.t1 > seg.t0 && x.t0 <= t) rodeHere = true;
            if (gv != 0 && !rodeHere && !Txt.has(s, "सीढ़ी", "सीढ़ियों", "stairs", "steps down", "पेड़", "tree", "झूले", "swing", "slide")) {
                String arrival = s.replaceAll("से उतर[^\\s।.,!?]*", "से आता").replaceAll("(?i)(gets|got|steps|climbs|hops) (off|out of) (the |a |an |his |her |their )?", "comes by ");
                if (!arrival.equals(s)) { arrivalsByRide++; return activityFrom(arrival, t, subj, who, group); }
            }
        }
        int v = vehicleIn(s, subj.look.isChild());
        if (v != 0) {
            // v44: a mishap on the way — a skid and a fall, a crash, a hard brake that just saves them
            int mishap = mishapIn(s, v);
            boolean away = leaving(s) || Txt.has(s, "चला जा", "चली जा", "चले जा", "निकल", "रवाना", "rides off", "drives off", "drives away", "rides away", "leaves");
            if (mishap == Film.MISHAP_SKID || mishap == Film.MISHAP_CRASH) away = false;
            boolean takes = Txt.has(s, " को ", "takes ", "with ");
            // v43: off to another place on it ("स्कूटर से बाज़ार जाते हैं", "drives to the office"): out of the picture
            boolean arrives = Txt.has(s, ARRIVE_WORDS);
            if (!away && !arrives && Txt.has(s, DESTINATIONS) && Txt.has(s, GO_WORDS)) away = true;
            float speed = v == Film.V_BICYCLE ? (subj.look.isChild() ? 190 : 230) : v == Film.V_BUS ? 320 : v == Film.V_AUTO ? 340
                    : v == Film.V_HORSE ? (Txt.has(s, "धीरे", "slowly", "walks", "trots", "कदम") ? 150 : 330) : v == Film.V_ELEPHANT ? 110 : 400;
            List<Film.Actor> riders = new ArrayList<Film.Actor>();
            for (Film.Actor a : all) if (a.look.isHumanoid()) riders.add(a);
            // v44: in a car the one at the wheel drives — named with the driving verb or "at the wheel" — and the ones
            // given a seat beside or behind ride with them ("राहुल पापा के साथ कार में आगे वाली सीट पर बैठता है": पापा
            // drives; a child named first is driven by the grown-up named with them)
            if (v == Film.V_CAR && riders.size() > 1) {
                Film.Actor wheel = null;
                for (Film.Actor a : riders) {
                    String c = seatClause(s, a);
                    if (c != null && Txt.has(c, DRIVES) && carSeat(c) == Film.CAR_AUTO) { wheel = a; break; }
                }
                Film.Actor first = riders.get(0);
                if (wheel == null && (carSeat(seatClause(s, first)) != Film.CAR_AUTO || first.look.isChild()))
                    for (Film.Actor a : riders) if (a != first && !a.look.isChild() && carSeat(seatClause(s, a)) == Film.CAR_AUTO) { wheel = a; break; }
                if (wheel != null && wheel != first) { riders.remove(wheel); riders.add(0, wheel); carDriversChosen++; }
            }
            // in a bus or an auto one who does not drive rides as a passenger (a driver is at the wheel) — v43: in a car only
            // in a taxi, or children alone ("तरुण कार में बैठता है" is his own car, not a taxi with a stranger driving him)
            boolean kidsOnly = true;
            for (Film.Actor a : riders) if (!a.look.isChild()) kidsOnly = false;
            boolean hired = Txt.has(s, "टैक्सी", "taxi", "cab", "ड्राइवर", "driver", "किराए", "उबर", "ओला", "uber");
            // v44: children do not drive, and do not take a taxi by themselves when a grown-up of theirs is there: the
            // grown-up drives them (a father first, then a mother, then any grown-up on the stage)
            if (v == Film.V_CAR && kidsOnly && !hired) {
                Film.Actor grown = likelyDriver(t, riders);
                if (grown != null) { riders.add(0, grown); kidsOnly = false; carDriversChosen++; }
            }
            boolean passenger = (v == Film.V_BUS || v == Film.V_AUTO || (v == Film.V_CAR && (hired || kidsOnly)))
                    && !Txt.has(s, "चला", "चलाते", "चलाती", "चलाता", "drive", "drives", "driving", "drove", "ड्राइव");
            // v43: "प्रिया स्कूटर पर पीछे बैठती है", "sits behind him on the bike": a pillion rider, on the one who rides it
            boolean pillion = (v == Film.V_SCOOTER || v == Film.V_MOTORBIKE || v == Film.V_BICYCLE || v == Film.V_HORSE || v == Film.V_ELEPHANT)
                    && Txt.has(s, "पीछे बैठ", "पीछे सवार", "sits behind", "seated behind", "behind him on", "behind her on", "pillion",
                    // v44: on a cycle's carrier or its crossbar
                    "कैरियर", "डंडे पर", "डंडे पे", "आगे डंडे", "carrier", "crossbar", "front bar", "handlebar seat");
            // v43: only getting on — "तरुण स्कूटर पर बैठता है और किक मारकर स्टार्ट करता है", "gets on his bike": they sit on it (the
            // engine running) and wait, until a later direction rides it on, or they do something else and get off first
            boolean stay = !away && Txt.has(s, MOUNT_ONLY) && !Txt.has(s, RIDE_MOVES);
            // v41: "तरुण कार चलाता है। प्रिया, वृंदा और वनुशा कार में बैठते हैं।" — the family's own car, which one of the
            // part just drove: they walk to it and get in, and he drives them (not a second car with a stranger at its wheel)
            Film.Actor driver = null;
            boolean again = false;
            // (v43: getting into a car another drives just now — "प्रिया, वृंदा और वनुशा कार में बैठते हैं" — joins it)
            boolean joinsCar = v == Film.V_CAR && !hired && (Txt.has(s, "में बैठ", "अंदर बैठ", "में चढ़", "gets in", "get in", "got in", "sit in", "sits in", "climb in", "climbs in")
                    || carSeat(s) != Film.CAR_AUTO)           // (v44: a seat named — "आगे की सीट पर बैठती है" — is a seat in the car being driven)
                    && !Txt.has(s, "चला", "चलाते", "चलाती", "चलाता", "drive", "drives", "driving", "drove", "ड्राइव");
            if (((passenger || joinsCar) && (v == Film.V_CAR || v == Film.V_SCOOTER || v == Film.V_MOTORBIKE)) || pillion) {
                for (Film.Actor o : seg.actors) {
                    // (v44: the one at the wheel may be named too — "प्रिया तरुण के बगल वाली सीट पर बैठती है")
                    if ((riders.contains(o) && !(v == Film.V_CAR && joinsCar)) || o.look == null || !o.look.isHumanoid() || !o.stateAt(t).visible) continue;
                    for (Film.Act x : o.acts) if (x.type == Film.G_RIDE && x.item == v && x.t1 >= t - 4f && x.t0 < t) driver = o;
                }
                if (driver != null) riders.remove(driver);
                // v43: one getting on behind a rider who got off a while ago: the rider gets on again first
                if (driver == null && pillion) for (Film.Actor o : seg.actors) {
                    if (riders.contains(o) || o.look == null || !o.look.isHumanoid() || !o.stateAt(t).visible) continue;
                    for (Film.Act x : o.acts) if (x.type == Film.G_RIDE && x.item == v && x.t1 < t && !rideAway(o, x)) { driver = o; again = true; }
                }
            }
            List<Film.Actor> already = new ArrayList<Film.Actor>();
            if (driver != null) {
                riders.add(0, driver);
                familyRides++;
                // (the family getting into his car: he drives them on — v44: but one only taking a seat waits there
                // with him, unless the sentence goes somewhere)
                if ((passenger || joinsCar) && !(carSeat(s) != Film.CAR_AUTO && !Txt.has(s, RIDE_MOVES))) stay = false;
                passenger = false;
                // v44: whoever already sits in his car stays in it (they got out each time another got in)
                for (Film.Actor o : seg.actors) {
                    if (riders.contains(o)) continue;
                    for (Film.Act x : o.acts) if (x.type == Film.G_RIDE && x.target == driver && x.item == -v && x.t0 < t && x.t1 >= t - 2f) { riders.add(o); already.add(o); break; }
                }
            }
            // v43: a hired auto, bus or taxi does not wait: it takes them on (away, unless they are arriving in it)
            if (passenger) { stay = false; if (!arrives) away = true; }
            // v43: riding on — the one who just got on (or stopped a moment ago) rides on, with whoever got on with them
            Film.Act cont = null;
            List<Film.Actor> carried = new ArrayList<Film.Actor>(already);
            if (driver == null && !passenger && !riders.isEmpty() && riders.get(0).stateAt(t).visible) {
                Film.Actor l0 = riders.get(0);
                cont = lastRide(l0, v, t);
                // (v44: a while ago, but nothing done since — they talked by it: still the same ride, they ride on)
                if (cont != null && ((cont.t1 < t - 4f && (cont.t1 < t - 14f || busySince(l0, cont, t))) || rideAway(l0, cont))) cont = null;
                if (cont != null) for (Film.Actor o : seg.actors) {
                    if (o == l0 || carried.contains(o)) continue;
                    boolean named = riders.contains(o);          // (v44: named again — "दोनों" — still the one behind: rides on, not on again)
                    // (v44: everyone in the car rides on — those who got in later as well as those who got in with the driver)
                    for (Film.Act x : o.acts) if (x.type == Film.G_RIDE && x.target == l0 && x.item < 0
                            && (Math.abs(x.t0 - cont.t0) < 0.05f || ((Film.cabin(v) || named) && x.t0 < t && x.t1 >= Math.min(cont.t1, t) - 2f))) { if (!named) riders.add(o); carried.add(o); break; }
                }
            }
            boolean joined = (driver != null && !again) || cont != null;            // it is already under its rider
            boolean ownBikes = v == Film.V_BICYCLE && !takes && !pillion && !joined && driver == null;     // each on a bicycle of their own
            // v43: following one who rides ("तरुण बाइक चलाकर उसके पीछे आता है", "follows her on his bike"): the same way they
            // went — at the same time, where nothing else is planned for the follower in between — stopping behind them
            Film.Act ahead = null;
            Film.Actor aheadA = null;
            if (!joined && driver == null && !pillion && !away && !stay && Txt.has(s, "पीछे", "follow", "behind him", "behind her", "after him", "after her")) {
                for (Film.Actor o : seg.actors) {
                    if (riders.contains(o) || o.look == null) continue;
                    for (Film.Act x : o.acts) if (x.type == Film.G_RIDE && x.item > 0 && x.t1 >= t - 4f && x.t0 <= t + 0.5f && (ahead == null || x.t0 > ahead.t0)) { ahead = x; aheadA = o; }
                }
            }
            Film.Actor lead = riders.isEmpty() ? subj : riders.get(0);
            float vs = vehiclePx(lead, v);
            // v43: arriving on it from beyond the edge of the picture: one not yet in it ("तरुण बाइक से घर आता है")
            boolean enters = !joined && driver == null && (!lead.stateAt(t).visible || justIn(lead, t)
                    || ((arrives || Txt.has(s, "रुकता", "रुकती", "रुकते", "पार्क", "stops", "pulls up", "parks")) && firstThing(lead, t))
                    || ((mishap == Film.MISHAP_SKID || mishap == Film.MISHAP_CRASH) && !Txt.has(s, MOUNT_ONLY)));
            float x0 = xAt(lead, t + 0.05f);
            float faces = lead.stateAt(t).facing >= 0 ? 1 : -1;
            // v43: their own vehicle parked in this part (where they got off it): they walk back to it and ride it from there
            Film.Fx parked = null;
            if (!joined && !enters && !ownBikes) for (Film.Fx o : seg.fx) if (o.type == Film.FX_PARKED && o.a == lead && o.kind % Film.V_PASSENGER == v && o.t0 <= t && o.t1 > t) parked = o;
            if (parked != null) { x0 = parked.x; faces = parked.color >= 0 ? 1 : -1; }
            // v43: the auto or taxi hailed a moment ago is the one they get into, where it stopped for them
            Film.Fx waiting = passenger && hailed != null && hailed.t1 > t && hailed.kind == v + Film.V_PASSENGER && seg.fx.contains(hailed) ? hailed : null;
            if (waiting != null) { x0 = waiting.y; faces = waiting.color >= 0 ? 1 : -1; enters = false; }
            // (one following another comes from behind them, the way they went)
            float hd = 0;
            if (ahead != null) hd = xAt(aheadA, ahead.t0 + rideStop(ahead)) >= xAt(aheadA, ahead.t0 + rideMount(ahead)) ? 1 : -1;
            float dir = away ? (x0 < 640 ? -1 : 1) : (x0 < 640 ? 1 : -1);
            float side = x0 >= 640 ? 1 : -1;
            if (enters) { if (hd != 0) side = -hd; dir = -side; x0 = side > 0 ? 1280 + 1.05f * vs : -1.05f * vs; }
            // v43: a vehicle already standing (under its rider, or parked) goes on the way it faces when there is road
            // enough that way — it does not turn round on the spot (and never with someone on the seat behind)
            boolean behindSeat = false;
            for (Film.Actor o : carried) if (!Film.cabin(v)) behindSeat = true;
            if (joined || parked != null || waiting != null) {
                // (v44: a mishap needs a run up — it turns round first when the road ahead is too short)
                float room = mishap != Film.MISHAP_NONE ? 520 : 300;
                if (away || behindSeat || waiting != null || (x0 + faces * room >= 170 && x0 + faces * room <= 1110)) dir = faces;
            }
            float to;
            if (stay) to = x0;
            else if (enters) {
                // to just short of the nearest one on the stage on their way in (the front of it stops well before them)
                Film.Actor near = null;
                for (Film.Actor o : seg.actors) {
                    if (riders.contains(o) || !o.stateAt(t).visible || o.look == null || !o.look.isHumanoid()) continue;
                    if (near == null || (side > 0 ? xAt(o, t) > xAt(near, t) : xAt(o, t) < xAt(near, t))) near = o;
                }
                float front = v == Film.V_CAR ? 0.95f : v == Film.V_BUS ? 1.9f : v == Film.V_AUTO ? 0.6f : 0.5f;
                float want = near == null ? 640 + side * 160 : xAt(near, t) + side * (front * vs + 80);
                to = freeNear(Math.max(170, Math.min(1110, want)), t, riders, 140);
            } else {
                to = away ? (dir > 0 ? 1560 : -280) : Math.max(170, Math.min(1110, 1280 - x0));
                if (!away && (Math.abs(to - x0) < 260 || (to - x0) * dir < 0)) to = Math.max(170, Math.min(1110, x0 + dir * 420));
                // v41: it stops on free ground, not on someone standing there — and still rides across (a ride of a few
                // steps is no ride): the first free ground at least 300 on, else where it was going
                if (!away) {
                    float want = to;
                    to = freeNear(want, t, riders, 140);
                    if (Math.abs(to - x0) < 260 && !behindSeat) {
                        // (the other way when the ground ahead is taken)
                        // (v41: and on a crowded stage a little nearer them and a little closer — still a ride across, never
                        // onto someone: a bike stopped on वृंदा's toes)
                        float on = Float.NaN, onDir = dir;
                        for (int pass = 0; pass < 2 && Float.isNaN(on); pass++)
                            for (int sd = 0; sd < 2 && Float.isNaN(on); sd++) {
                                float dd = sd == 0 ? dir : -dir;
                                for (float c = x0 + dd * (pass == 0 ? 300 : 260); c >= 170 && c <= 1110; c += dd * 60) if (freeAt(c, t, riders, pass == 0 ? 140 : 84)) { on = c; onDir = dd; break; }
                            }
                        if (Float.isNaN(on)) to = want;
                        else { to = on; dir = onDir; }
                    }
                    if (behindSeat && (to - x0) * dir < 60) to = Math.max(170, Math.min(1110, x0 + dir * 300));
                    if ((to - x0) * dir < 0) dir = -dir;            // (it faces the way it goes)
                }
            }
            boolean together = false;
            if (ahead != null) {
                float hb = xAt(aheadA, ahead.t0 + rideStop(ahead));
                float want = hb - hd * 230;
                if ((want - x0) * hd >= 150) {
                    dir = hd; to = want; following++;
                    together = ahead.t0 + 0.6f < t && (enters || lead.last().t <= ahead.t0 + 0.6f);
                    if (together) for (Film.Speak x : lead.speaks) if (x.t0 >= ahead.t0 && x.t0 < t) together = false;
                } else if (!enters) {
                    // (already ahead of where they stop: the same way, a little on)
                    float on = Math.max(170, Math.min(1110, x0 + hd * 300));
                    if (Math.abs(on - x0) >= 150) { dir = hd; to = on; following++; }
                }
            }
            // v43: it stops with all of it in the picture (a car stopped at the edge had its rear door off the screen,
            // and the family walked out of the picture to get in)
            if (!away && !stay) { to = inFrame(to, dir, v, vs); if ((to - x0) * dir < 40 && !enters) to = x0 + dir * 40; }
            float dur = stay ? 0 : Math.max(2.2f, Math.min(5.5f, Math.abs(to - x0) / speed));
            if (stay) mishap = Film.MISHAP_NONE;
            // v44: a drive with the driver's hands and feet shown close lasts long enough for each insert
            if (v == Film.V_CAR && !stay && !passenger && mishap == Film.MISHAP_NONE) {
                int ins = (Txt.has(s, STEER_WORDS) ? 1 : 0) + (Txt.has(s, ACCEL_WORDS) ? 1 : 0) + (Txt.has(s, BRAKE_WORDS) ? 1 : 0);
                if (ins > 0) dur = Math.max(dur, 1.35f * ins + 1.0f);
            }
            // v44: a hard brake stops it for a moment before it goes on; after a crash they sit stunned a moment
            float pause = mishap == Film.MISHAP_SWERVE ? 0.9f : 0, extraTime = mishap == Film.MISHAP_SWERVE ? 1.2f : mishap == Film.MISHAP_CRASH ? 1.0f : 0;
            boolean skid = mishap == Film.MISHAP_SKID;
            // v43: getting on before it moves. They walk up to it first — a pillion rider to the seat behind, the ones who
            // ride inside a car to its rear door one behind the other, inside a bus or an auto to its door, a rider back to
            // where they left it — then the driver opens the door or swings onto the seat, kicks the stand up, the others
            // get in one after another, and it goes. At the end it stops, the stand goes down or the doors open, and they
            // get off.
            boolean inside = v == Film.V_CAR || v == Film.V_BUS || v == Film.V_AUTO;
            // the vehicle they get into turns round first when it must go back the way it came (it turned on the spot in
            // one frame as they got in): it turns (the renderer narrows it through the turn) before they walk to its door
            float walkFrom = t + 0.1f, turnWait = 0;
            if (joined && dir != faces) {
                Film.Key turn = keyAt(lead, t + 0.15f); turn.facing = dir; turn.moveDur = 0;
                rideKeys.add(turn);
                walkFrom = t + 0.55f; turnWait = 0.45f;
                uTurns++;
            }
            float[] off = new float[riders.size()];
            int nIn = 0, nNew = 0;
            // v44: who sits where in a car: as the sentence says ("आगे की सीट पर", "in the back"); else the first grown-up
            // beside the driver and the children (and anyone after) in the back. The one in front gets in first, by the
            // front door, after the driver.
            int[] seats = new int[riders.size()];
            if (v == Film.V_CAR) {
                boolean front = false;
                for (int i = 1; i < riders.size(); i++) {
                    seats[i] = carSeat(seatClause(s, riders.get(i)));
                    if (seats[i] == Film.CAR_FRONT) { if (front) seats[i] = Film.CAR_BACK; front = true; }
                }
                // (whoever already sits in front from the ride before keeps it)
                for (Film.Actor o : carried) for (Film.Act x : o.acts) if (x.type == Film.G_RIDE && x.seat == Film.CAR_FRONT && x.t1 >= t - 0.1f) front = true;
                for (int i = 1; i < riders.size(); i++) {
                    if (seats[i] != Film.CAR_AUTO) continue;
                    boolean held = false;
                    for (Film.Act x : riders.get(i).acts) if (x.type == Film.G_RIDE && x.item == -Film.V_CAR && x.seat >= 0 && x.t1 >= t - 0.1f) { seats[i] = x.seat; held = true; }
                    if (held) continue;
                    if (!front && !passenger && !riders.get(i).look.isChild()) { seats[i] = Film.CAR_FRONT; front = true; }
                    else seats[i] = Film.CAR_BACK;
                }
                for (int i = 1; i < riders.size(); i++) { if (seats[i] == Film.CAR_FRONT) frontSeats++; else backSeats++; }
            }
            float walk = 0;
            boolean leadWalks = false;
            for (int i = 0; i < riders.size(); i++) {
                Film.Actor pa = riders.get(i);
                float spot;
                if (i == 0) {
                    if ((parked == null && waiting == null) || enters) continue;
                    spot = x0;                                         // back to where they left it (to the one waiting for them)
                } else {
                    if (ownBikes) continue;                            // each on a bicycle of their own
                    if (inside) { nIn++; off[i] = v == Film.V_CAR ? (passenger ? 0.2f + 0.16f * (nIn - 1) : seats[i] == Film.CAR_FRONT ? 0.12f * vs : (0.45f + 0.16f * (nIn - 1)) * vs) : 60 + 55 * (nIn - 1); }
                    else off[i] = 0.24f * vs;                         // on the seat behind the rider
                    if (carried.contains(pa)) continue;               // already on it
                    nNew++;
                    if (enters) continue;                             // they come with it
                    spot = x0 - dir * off[i];
                }
                float px = xAt(pa, t), dd = Math.abs(px - spot);
                if (dd < 8) continue;
                float wd = Math.min(2.4f, Math.max(0.5f, dd / 190f));
                Film.Key wk = keyAt(pa, walkFrom); wk.x = spot; wk.moveDur = wd; wk.facing = spot > px ? 1 : -1; wk.run = false; wk.body = Pose.STAND;
                walk = Math.max(walk, wd);
                if (i == 0) leadWalks = true;
            }
            // v43: a hired auto, bus or taxi first drives up to the ones it picks up and stops beside them
            float arrive = 0;
            if (waiting != null) waiting.t1 = t + 0.4f + walk + turnWait;      // (it takes over from the hailed one there)
            else if (passenger && !enters) {
                arrive = Film.VEHICLE_MOVE + 0.2f;
                float from = dir > 0 ? -1.4f * vs : 1280 + 1.4f * vs;
                Film.Fx up = fx(Film.FX_VEHICLE, t + 0.1f, t + 0.4f + Math.max(walk, arrive) + turnWait, from, x0, lead, null);
                up.t2 = t + 0.1f; up.kind = v + Film.V_PASSENGER; up.color = dir > 0 ? 1 : -1;
                film.sfx.add(sfxAt(v == Film.V_AUTO ? Film.SFX_MOTOR : Film.SFX_ENGINE, t + 0.1f, Film.VEHICLE_MOVE + 0.6f, 0.32f, lead));
                if (v != Film.V_BUS) film.sfx.add(sfxAt(Film.SFX_HORN, t + 0.3f, 0.6f, 0.25f, lead));
                hiredMoves++;
            }
            float t0 = together ? ahead.t0 + 0.6f : t + 0.4f + Math.max(walk, arrive) + turnWait;
            if (!riders.isEmpty() && !together && !enters && !leadWalks) { Film.Key st = keyAt(lead, t + 0.05f); st.x = x0; st.moveDur = 0; }
            if (joined) {
                // v41: the car stays where he stopped it, him at the wheel, while they walk to it and get in (it was
                // gone between the two drives: he stood in the road and they walked into him) — v43: and so for whoever
                // rides on with him; it is not also parked under them meanwhile, and they do not get off and walk apart
                List<Film.Actor> ext = new ArrayList<Film.Actor>();
                ext.add(lead); ext.addAll(carried);
                float contEnd = cont != null ? cont.t1 : t;
                for (Film.Actor a : ext) {
                    Film.Act x = a == lead ? (cont != null ? cont : lastRide(a, v, t)) : null;
                    if (x == null) for (Film.Act y : a.acts) if (y.type == Film.G_RIDE && y.target == lead && y.item < 0 && y.t0 < t && (x == null || y.t0 > x.t0)
                            && ((cont != null && Math.abs(y.t0 - cont.t0) < 0.05f) || y.t1 >= Math.min(t, contEnd) - 2f)) x = y;
                    if (x == null) continue;
                    float was = x.t1;
                    x.t1 = t0;                        // (it runs straight on into this one: no getting off, no fading)
                    for (java.util.Iterator<Film.Fx> it = seg.fx.iterator(); it.hasNext(); ) { Film.Fx o = it.next(); if (o.type == Film.FX_PARKED && o.a == a && Math.abs(o.t0 - was) < 0.06f) it.remove(); }
                    dropOutKeys(x);
                }
            }
            float mount = enters ? 0 : joined ? (nNew == 0 ? 0.45f : v == Film.V_CAR ? 0.95f + 0.3f * Math.max(0, nNew - 1) : inside ? 0.7f + 0.3f * Math.max(0, nNew - 1) : 0.75f)
                    : v == Film.V_CAR ? 0.95f + 0.3f * Math.max(0, nIn - 1) : inside ? 0.7f + 0.3f * Math.max(0, nIn - 1) : 0.75f;
            float hold = stay ? 1.6f : 0;
            List<Object[]> outs = new ArrayList<Object[]>();
            Film.Act leadRide = null;
            for (int i = 0; i < riders.size(); i++) {
                Film.Actor a = riders.get(i);
                boolean own = i == 0 || ownBikes;
                float lag = own ? i * 0.35f : 0;
                float extra = own ? 0 : Math.max(0, i - 1) * 0.35f;
                // (arriving: not in the picture until they ride in)
                if (enters) for (Film.Key kk : a.keys) if (kk.t < t0 + lag - 0.001f && kk.t >= seg.t0 - 0.001f) kk.visible = false;
                Film.Key k0 = keyAt(a, t0 + lag);
                k0.visible = true; k0.body = Pose.STAND; k0.facing = dir; k0.run = false;
                k0.x = own ? (enters ? x0 - (ownBikes ? i * 150 * dir : 0) : xAt(a, t0 + lag)) : x0 - dir * off[i];
                k0.moveDur = 0;
                rideKeys.add(k0);
                float go = stay ? k0.x : own ? to - (ownBikes ? i * 150 * dir : 0) : to - dir * off[i];
                boolean hit = (i == 0 || !own) && mishap != Film.MISHAP_NONE;      // (each on a cycle of their own: the lead's mishap only)
                if (!stay && hit && mishap == Film.MISHAP_SWERVE) {
                    float mid = k0.x + (go - k0.x) * 0.55f;
                    Film.Key k = keyAt(a, t0 + lag + Math.max(mount, 0.02f)); k.x = mid; k.moveDur = dur * 0.55f; k.facing = dir; k.run = false; rideKeys.add(k);
                    Film.Key k2 = keyAt(a, t0 + lag + mount + dur * 0.55f + pause); k2.x = go; k2.moveDur = dur * 0.45f + 0.3f; k2.facing = dir; k2.run = false; rideKeys.add(k2);
                } else if (!stay) {
                    Film.Key k = keyAt(a, t0 + lag + Math.max(mount, 0.02f)); k.x = go; k.moveDur = dur; k.facing = dir; k.run = false; rideKeys.add(k);
                    k.hardStop = hit && (mishap == Film.MISHAP_SKID || mishap == Film.MISHAP_CRASH);
                }
                float more = hit ? extraTime : 0;
                Film.Act ride = new Film.Act(t0 + lag, hit && skid ? t0 + lag + mount + dur + 0.05f : t0 + lag + mount + dur + more + hold + (away ? 0 : 0.8f + extra), Film.G_RIDE);
                if (hit) { ride.mishap = mishap; ride.mishapAt = mount + (mishap == Film.MISHAP_SWERVE ? dur * 0.55f - 0.15f : dur); }
                ride.item = own ? (passenger ? v + Film.V_PASSENGER : v) : -v;
                ride.target = own ? null : riders.get(0);
                ride.a0 = mount;                   // v43: getting on takes this long; it moves after it
                ride.a1 = mount + dur + hold + (hit && mishap == Film.MISHAP_SWERVE ? pause + 0.3f : 0);      // v43: and stops here (getting off after it)
                ride.how = stay ? -1 : own || !inside ? 0 : i;     // v43: only getting on (-1); the i-th to get in through the door
                if (!own && v == Film.V_CAR) ride.seat = seats[i];
                a.acts.add(ride);
                if (i == 0) leadRide = ride;
                if (away) { Film.Key gone = a.at(t0 + lag + mount + dur + 0.05f); gone.visible = false; rideKeys.add(gone); leftFrom.put(a, dir > 0 ? 1400f : -120f); goneAt.put(a, t0 + lag + mount + dur); }
                // v42: a car, a scooter or a bike ridden in stays where its rider got off — parked, until it is ridden
                // again (it faded away while the family stood beside it)
                // v43: ... and one ridden from here stands parked there from the start of the part (it appeared from
                // nowhere under its rider); the parked one and the ridden one take over from each other at once
                if (own && ride.item > 0 && ride.item < Film.V_PASSENGER && (v == Film.V_CAR || v == Film.V_SCOOTER || v == Film.V_MOTORBIKE || v == Film.V_BICYCLE || v == Film.V_HORSE || v == Film.V_ELEPHANT)) {
                    boolean joinedBefore = rideJoinedBefore(a, ride), hadParked = false;
                    for (Film.Fx o : seg.fx) if (o.type == Film.FX_PARKED && o.a == a && o.t1 > ride.t0 && o.t0 <= ride.t0) { o.t1 = ride.t0; hadParked = true; }
                    if (!hadParked && !joinedBefore && !enters && Sets.outdoorSet(seg.set)) {
                        Film.Fx pre = fx(Film.FX_PARKED, seg.t0, ride.t0, xAt(a, ride.t0), 0, a, null); pre.kind = ride.item; pre.color = dir > 0 ? 1 : -1;
                    }
                    if (!away && !(hit && skid)) { Film.Fx pk = fx(Film.FX_PARKED, ride.t1, 1e6f, go, 0, a, null); pk.kind = ride.item; pk.color = dir > 0 ? 1 : -1; }
                }
                if (own) {
                    float mid = t0 + lag + mount;
                    if (stay) {
                        // (getting on and starting it: the engine heard idling while they wait)
                        if (v == Film.V_MOTORBIKE || v == Film.V_SCOOTER) film.sfx.add(sfxAt(Film.SFX_MOTOR, mid - 0.2f, 2.2f, 0.26f, a));
                        else if (v == Film.V_CAR) film.sfx.add(sfxAt(Film.SFX_ENGINE, mid - 0.2f, 2.2f, 0.26f, a));
                        else if (v == Film.V_HORSE) film.sfx.add(sfxAt(Film.SFX_NEIGH, mid - 0.3f, 1.2f, 0.3f, a));
                    } else switch (v) {
                        case Film.V_BICYCLE: film.sfx.add(sfxAt(Film.SFX_CYCLE_BELL, mid - 0.3f, 0.8f, 0.4f, a)); film.sfx.add(sfxAt(Film.SFX_CHAIN, mid, dur, 0.3f, a)); break;
                        // v44: a horse snorts and neighs as it is mounted, its hooves as it goes; an elephant trumpets and pads
                        case Film.V_HORSE:
                            film.sfx.add(sfxAt(Film.SFX_NEIGH, mid - 0.5f, 1.2f, 0.36f, a));
                            film.sfx.add(sfxAt(Film.SFX_HOOVES, mid, dur, speed > 200 ? 0.42f : 0.3f, a));
                            break;
                        case Film.V_ELEPHANT:
                            film.sfx.add(sfxAt(Film.SFX_TRUMPET, mid - 0.4f, 1.6f, 0.4f, a));
                            film.sfx.add(sfxAt(Film.SFX_STEPS_HARD, mid, dur, 0.28f, a));
                            break;
                        case Film.V_MOTORBIKE: case Film.V_SCOOTER: case Film.V_AUTO:
                            film.sfx.add(sfxAt(Film.SFX_MOTOR, mid - 0.5f, dur + 0.8f, 0.38f, a));
                            if (v == Film.V_AUTO) film.sfx.add(sfxAt(Film.SFX_HORN, mid - 0.2f, 0.8f, 0.3f, a));
                            break;
                        default:
                            film.sfx.add(sfxAt(Film.SFX_ENGINE, mid - 0.6f, dur + 1.0f, 0.4f, a));
                            film.sfx.add(sfxAt(Film.SFX_HORN, mid - 0.2f, 0.8f, 0.32f, a));
                    }
                    rides++;
                }
                d = Math.max(d, stay ? t0 + lag + mount + 0.5f - t : t0 + lag + mount + dur + extra + more - t + 0.3f);
                // v41: a car (a scooter, an auto) that stops here lets them out side by side, not one on top of another
                if (!away && !stay && riders.size() > 1 && v != Film.V_BICYCLE && !(hit && skid)) {
                    float out = t0 + lag + mount + dur + 0.85f + extra + more;
                    float spot = Math.max(150, Math.min(1130, to + (i - (riders.size() - 1) / 2f) * 115));
                    Film.Key ko = keyAt(a, out);
                    ko.x = spot; ko.moveDur = 0.6f; ko.facing = spot >= go ? 1 : -1; ko.run = false; ko.body = Pose.STAND;
                    spread(a, ko);
                    outs.add(new Object[]{a, ko});
                    d = Math.max(d, out + 0.6f - t);
                }
            }
            if (leadRide != null && !outs.isEmpty()) outKeys.put(leadRide, outs);
            if (leadRide != null && !stay) d = Math.max(d, rideExtras(s, v, leadRide, riders, mishap, dir, vs, t) - t);
            // v43: the hired vehicle that dropped them drives off once the last one is out (it faded away where it stood)
            if (passenger && !away && leadRide != null) {
                float last = leadRide.t1;
                for (Film.Actor a : riders) for (Film.Act x : a.acts) if (x.type == Film.G_RIDE && Math.abs(x.t0 - leadRide.t0) < 0.05f) last = Math.max(last, x.t1);
                float from = to, go2 = dir > 0 ? 1280 + 1.4f * vs : -1.4f * vs;
                Film.Fx off2 = fx(Film.FX_VEHICLE, leadRide.t1, last + 0.4f + Film.VEHICLE_MOVE, from, go2, lead, null);
                off2.t2 = last + 0.4f; off2.kind = v + Film.V_PASSENGER; off2.color = dir > 0 ? 1 : -1;
                film.sfx.add(sfxAt(v == Film.V_AUTO ? Film.SFX_MOTOR : Film.SFX_ENGINE, last + 0.2f, Film.VEHICLE_MOVE + 0.4f, 0.3f, lead));
                hiredMoves++;
            }
            if (stay) stays++;
            if (enters) arrivals++;
            if (joined && cont != null) rodeOn++;
            frameAround(t + 0.2f, enters ? (dir > 0 ? 60 : 1220) : stay ? x0 - 200 : x0, away ? (dir > 0 ? 1180 : 100) : stay ? x0 + 200 : to);
            rode = true;
            return d;
        }
        // ---- the playground
        int pg = Txt.has(s, ROUNDABOUT) ? Film.PG_ROUND : Txt.has(s, SEESAW) ? Film.PG_SEESAW : Txt.has(s, SLIDE) && !Txt.has(s, "फिसल गय", "फिसल ग", "slipped") && !snowSlide(s) ? Film.PG_SLIDE
                : (Txt.has(s, SWING) && !Txt.has(s, "तलवार", "sword", "bat ", "बल्ला", "axe", "कुल्हाड़ी", "arms", "हाथ")) ? Film.PG_SWING : 0;
        // v40: going to the slide or the swing is a walk, not yet a slide or a swing ("वनुशा फिसलपट्टी की तरफ़ जाती है"
        // slid where she stood); the play needs its own verb ("फिसलती है", "झूलने लगता है")
        boolean goThere = Txt.has(s, GO_TO);
        if (goThere && (pg == Film.PG_SLIDE || pg == Film.PG_SWING) && !Txt.has(s, pg == Film.PG_SLIDE ? SLIDE_PLAY : SWING_PLAY)) pg = 0;
        // v41: the place's own swing and slide are played on as sessions (playFrom), never a one-sentence play of v37
        if ((pg == Film.PG_SWING && swingProp != null) || (pg == Film.PG_SLIDE && slideProp != null)) pg = 0;
        if (pg != 0) {
            float dur = pg == Film.PG_SLIDE ? 5.2f : 6.5f;
            List<Film.Actor> kids = new ArrayList<Film.Actor>(all);
            if (pg == Film.PG_SEESAW && kids.size() < 2) {
                // the other end: someone else on the stage (a child first)
                Film.Actor best = null;
                for (Film.Actor o : seg.actors) {
                    if (o == subj || !o.stateAt(t).visible || o.look == null || !o.look.isHumanoid()) continue;
                    if (best == null || (o.look.isChild() && !best.look.isChild())) best = o;
                }
                if (best != null) kids.add(best);
            }
            float t0 = t + 0.9f;
            if (pg == Film.PG_SEESAW && kids.size() >= 2) {
                Film.Actor a = kids.get(0), o = kids.get(1);
                float gap = 0.8f * (heightOf(a) + heightOf(o));
                float xa = Math.max(240, Math.min(1040 - gap * 0.5f, xAt(a, t))), xo = xa + gap;
                if (xo > 1180) { xo = 1180; xa = xo - gap; }
                // (v42: they walk to it standing — "दोनों सी-सॉ पर बैठते हैं" had seated them first, and they slid there seated)
                Film.Key ka = a.at(t + 0.1f); ka.x = xa; ka.moveDur = 0.7f; ka.facing = 1; ka.body = Pose.STAND; ka.seat = -1;
                Film.Key ko = o.at(t + 0.1f); ko.x = xo; ko.moveDur = 0.7f; ko.facing = -1; ko.body = Pose.STAND; ko.seat = -1;
                Film.Act sa = new Film.Act(t0, t0 + dur, Film.G_PLAYGROUND); sa.item = Film.PG_SEESAW; sa.target = o; a.acts.add(sa);
                Film.Act so = new Film.Act(t0, t0 + dur, Film.G_PLAYGROUND); so.item = -Film.PG_SEESAW; so.target = a; o.acts.add(so);
                film.sfx.add(sfxAt(Film.SFX_CREAK, t0, dur, 0.22f, a));
                playground++;
                frameAround(t0, xa, xo);
                return dur + 1.0f;
            }
            float lag = 0;
            for (Film.Actor a : kids) {
                if (pg == Film.PG_SEESAW) break;
                float h = heightOf(a), x = xAt(a, t + lag), from = x;
                // v40: "झूले की तरफ़ दौड़ता है और झूले पर बैठकर झूलने लगता है": first the run to it, then the play (the
                // swing or the slide stands toward the middle of the stage, a walk worth seeing away)
                if (goThere) x = Math.abs(640 - x) > 260 ? x + (x < 640 ? 300 : -300) : x + (x < 640 ? -280 : 280);
                float f = x < 640 ? 1 : -1;
                if (pg == Film.PG_SLIDE) x = f > 0 ? Math.max(180, Math.min(1100 - Props.SLIDE_LEN * h, x)) : Math.min(1100, Math.max(180 + Props.SLIDE_LEN * h, x));
                else x = Math.max(200, Math.min(1080, x));
                // a "बैठकर" of this sentence is the seat of the swing itself (drawn by the play): no sitting on the grass first
                for (Film.Key kk : a.keys) if (kk.t >= t - 0.01f && kk.body == Pose.SIT) kk.body = Pose.STAND;
                float go = goThere ? Math.min(3.5f, Math.max(0.8f, Math.abs(x - from) / (running(s) ? 250f : 170f))) : 0.6f;
                Film.Key k = a.at(t + 0.1f + lag); k.x = x; k.moveDur = go; k.facing = goThere ? (x > from ? 1 : -1) : f; k.run = goThere && running(s);
                if (goThere) {
                    t0 = Math.max(t0, t + 0.1f + lag + go + 0.3f);
                    Film.Key turn = a.at(t + 0.1f + lag + go + 0.05f); turn.facing = f;
                }
                Film.Act act = new Film.Act(t0 + lag, t0 + lag + dur, Film.G_PLAYGROUND);
                act.item = pg;
                a.acts.add(act);
                if (pg == Film.PG_SLIDE) {
                    // down the chute: the stage position moves with the slide (so they stay at its foot afterwards);
                    // the laugh from the moment they set off (its keys after the move's: keys go in time order)
                    Film.Key down = a.at(t0 + lag + dur * 0.5f); down.x = x + f * Props.SLIDE_LEN * h; down.moveDur = dur * 0.3f; down.facing = f;
                    feel(a, t0 + lag + dur * 0.5f, dur * 0.4f, Pose.LAUGH);
                    film.sfx.add(sfxAt(Film.SFX_WHOOSH, t0 + lag + dur * 0.5f, dur * 0.3f, 0.4f, a));
                } else {
                    film.sfx.add(sfxAt(Film.SFX_CREAK, t0 + lag, dur, 0.25f, a));
                    feel(a, t0 + lag, dur, Pose.HAPPY);
                }
                // v40: off the swing (or the slide) and standing again when the play is over — the seat is the play's
                if (a.last().t <= t0 + lag + dur + 0.05f) { Film.Key off = a.at(t0 + lag + dur + 0.05f); off.body = Pose.STAND; off.seat = -1; }
                playground++;
                lag += 0.5f;
                camOn(a, t0, pg == Film.PG_SLIDE ? 0.95f : 1.1f);
            }
            return t0 - (t + 0.9f) + dur + 1.0f + lag;
        }
        // ---- exercise
        int ex = Txt.has(s, PUSHUP) ? Film.EX_PUSHUP : Txt.has(s, SKIPPING) ? Film.EX_SKIP : Txt.has(s, DUMBBELL) ? Film.EX_DUMBBELL
                : Txt.has(s, YOGA) ? Film.EX_YOGA : Txt.has(s, SQUAT) ? Film.EX_SQUAT : 0;
        if (ex != 0) {
            float dur = ex == Film.EX_YOGA ? 6.6f : 5f;
            for (Film.Actor a : all) {
                Film.Act act = new Film.Act(t + 0.3f, t + 0.3f + dur, Film.G_EXERCISE);
                act.item = ex;
                a.acts.add(act);
                if (ex == Film.EX_SKIP) film.sfx.add(sfxAt(Film.SFX_ROPE, t + 0.4f, dur - 0.2f, 0.35f, a));
                exercises++;
            }
            return dur + 0.5f;
        }
        // ---- make-up with a mirror (each thing put on in turn; it stays on)
        List<Integer> mk = new ArrayList<Integer>();
        if (Txt.has(s, POWDER)) mk.add(Film.MK_POWDER);
        if (Txt.has(s, KAJAL)) mk.add(Film.MK_KAJAL);
        if (Txt.has(s, LIPSTICK)) mk.add(Film.MK_LIPSTICK);
        if (Txt.has(s, BINDI_ON)) mk.add(Film.MK_BINDI);
        if (Txt.has(s, MEHNDI)) mk.add(Film.MK_MEHNDI);
        if (Txt.has(s, FACE_PAINT)) mk.add(Film.MK_FACEPAINT);
        if (mk.isEmpty() && Txt.has(s, MAKEUP)) { mk.add(Film.MK_POWDER); mk.add(Film.MK_KAJAL); mk.add(Film.MK_LIPSTICK); }
        if (!mk.isEmpty()) {
            float tt = t + 0.3f;
            for (int m : mk) {
                float dur = m == Film.MK_MEHNDI ? 4f : 2.6f;
                for (Film.Actor a : all) {
                    Film.Act act = new Film.Act(tt, tt + dur, Film.G_MAKEUP);
                    act.item = m;
                    a.acts.add(act);
                    if (m == Film.MK_POWDER) film.sfx.add(sfxAt(Film.SFX_BRUSH, tt + 0.4f, dur - 0.6f, 0.12f, a));
                    makeups++;
                }
                tt += dur + 0.2f;
            }
            if (all.size() == 1) { mealCU = all.get(0); mealCUt = t + 0.2f; }
            return tt - t + 0.3f;
        }
        // ---- a bath: behind a curtain from the shoulders down (indoors a tiled corner; outdoors in the water up to the shoulders)
        if (Txt.has(s, BATHE)) {
            float dur = 5.5f;
            for (Film.Actor a : all) {
                Film.Act act = new Film.Act(t + 0.4f, t + 0.4f + dur, Film.G_BATHE);
                act.item = Sets.outdoorSet(seg.set) ? 1 : 0;
                a.acts.add(act);
                film.sfx.add(sfxAt(Film.SFX_POUR, t + 0.9f, dur - 1f, 0.32f, a));
                for (float st = t + 1.2f; st < t + dur; st += 1.4f) film.sfx.add(sfxAt(Film.SFX_SPLASH, st, 0.5f, 0.18f, a));
                baths++;
            }
            if (all.size() == 1) camOn(all.get(0), t + 0.4f, 1.45f);
            return dur + 0.8f;
        }
        // ---- affection: a hug, or a peck on the cheek or the forehead
        if (Txt.has(s, HUG) && all.size() >= 3) {
            // v41: "चारों एक दूसरे को गले लगाते हैं" — all of them, close together in one hug (not the first two only)
            List<Film.Actor> hug = new ArrayList<Film.Actor>();
            for (Film.Actor a : all) if (a.look != null && a.look.isHumanoid() && a.stateAt(t).visible) hug.add(a);
            if (hug.size() >= 3) {
                final float tt = t;
                java.util.Collections.sort(hug, new java.util.Comparator<Film.Actor>() { public int compare(Film.Actor x, Film.Actor y) { return Float.compare(xAt(x, tt), xAt(y, tt)); } });
                float cx = 0, hs = 0;
                for (Film.Actor a : hug) { cx += xAt(a, t); hs += heightOf(a); }
                cx /= hug.size(); hs /= hug.size();
                float gap = 0.3f * hs;
                cx = Math.max(160 + gap * (hug.size() - 1) / 2, Math.min(1120 - gap * (hug.size() - 1) / 2, cx));
                for (int i = 0; i < hug.size(); i++) {
                    Film.Actor a = hug.get(i);
                    float x = cx + (i - (hug.size() - 1) / 2f) * gap, x0 = xAt(a, t);
                    Film.Key k = keyAt(a, t + 0.1f);
                    if (k.body == Pose.LIE || k.body == Pose.SIT || k.body == Pose.KNEEL) k.body = Pose.STAND;
                    k.x = x; k.moveDur = Math.max(0.4f, Math.min(1.2f, Math.abs(x - x0) / 200f)); k.facing = x <= cx ? 1 : -1; k.run = false;
                    spread(a, k);
                    Film.Act h = new Film.Act(t + 1.0f, t + 3.4f, Film.G_HUG);
                    h.target = hug.get(i == 0 ? 1 : i - 1);
                    a.acts.add(h);
                    Film.Key kh = keyAt(a, t + 1.0f); kh.emotion = Pose.HAPPY;
                }
                frameAround(t + 0.2f, cx - gap * hug.size() / 2, cx + gap * hug.size() / 2);
                travelAfter = 3.5f;
                groupHugs++;
                hugs++;
                return 3.6f;
            }
        }
        if (Txt.has(s, HUG) && all.size() >= 2) {
            Film.Actor a = all.get(0), to = all.get(1);
            moveNear(a, to, t, 0.6f);
            Film.Key kt = to.at(t + 0.1f); if (kt.body == Pose.LIE || kt.body == Pose.SIT || kt.body == Pose.KNEEL) { kt.body = Pose.STAND; kt.seat = -1; }
            Film.Act h1 = new Film.Act(t + 0.7f, t + 3.0f, Film.G_HUG); h1.target = to; a.acts.add(h1);
            Film.Act h2 = new Film.Act(t + 0.7f, t + 3.0f, Film.G_HUG); h2.target = a; to.acts.add(h2);
            feel(a, t + 0.7f, 2.3f, Pose.HAPPY); feel(to, t + 0.7f, 2.3f, Pose.HAPPY);
            travelAfter = 3.1f;
            hugs++;
            return 3.2f;
        }
        if (Txt.has(s, KISS) && all.size() >= 2) {
            Film.Actor a = all.get(0), to = all.get(1);
            moveNear(a, to, t, 0.6f);
            Film.Act k = new Film.Act(t + 0.8f, t + 2.4f, Film.G_KISS);
            k.target = to;
            // the forehead for a child (or when the story says so), the cheek otherwise
            k.item = Txt.has(s, "माथे", "माथा", "ललाट", "forehead") || (to.look.isChild() && !a.look.isChild()) ? 1 : 0;
            a.acts.add(k);
            feel(to, t + 1.2f, 1.1f, Pose.HAPPY);          // (ends before the next line can begin)
            film.sfx.add(sfxAt(Film.SFX_KISS, t + 1.45f, 0.3f, 0.3f, a));
            pecks++;
            return 2.6f;
        }
        return d;
    }

    /**
     * v41: every everyday task a sentence names, one after the other in its order ("झाड़ू लगाती है और फिर पौधों में
     * पानी डालती है": the sweeping, then the watering — the second was left out).
     */
    private float taskFrom(String s, float t, List<Film.Actor> who) {
        if (negated(s)) return 0;
        String sp = " " + s + " ";
        String[][] words = {RANGOLI, PAINTING, COOK, SWEEP, WASH, WRITE, READ, PHONE, BRUSH, COMB, WATER_PLANTS};
        int[] kinds = {Film.T_RANGOLI, Film.T_PAINT, Film.T_COOK, Film.T_SWEEP, Film.T_WASH, Film.T_WRITE, Film.T_READ, Film.T_PHONE, Film.T_BRUSH, Film.T_COMB, Film.T_WATER};
        List<int[]> found = new ArrayList<int[]>();
        String n = Txt.norm(sp);
        // v43: a call with words between ("फ़ोन पर पापा से बात करता है"), or the phone ringing and answered
        boolean phoneLoose = Txt.has(sp, "फ़ोन", "फोन", "मोबाइल", "phone")
                && Txt.has(sp, "बात कर", "बात करता", "बात करती", "बात करते", "answers", "रिसीव", "receives the call", "picks it up", "उठाकर बात");
        for (int i = 0; i < kinds.length; i++) {
            if (!(Txt.has(sp, words[i]) || (kinds[i] == Film.T_PHONE && phoneLoose)) || (kinds[i] == Film.T_PAINT && Txt.has(sp, FACE_PAINT))) continue;
            // (v43: "a dust storm sweeps across the village" sweeps no floor)
            if (kinds[i] == Film.T_SWEEP && Txt.has(sp, "storm", "wind", "dust storm", "आँधी", "आंधी", "तूफ़ान", "तूफान", "हवा", "wave", "fire sweeps", "sweeps across", "sweeps over")) continue;
            // a rangoli or a painting also names colours and brushes: it is one task
            if ((kinds[i] == Film.T_BRUSH || kinds[i] == Film.T_PAINT) && !found.isEmpty() && (found.get(0)[1] == Film.T_RANGOLI || found.get(0)[1] == Film.T_PAINT)) continue;
            int at = Integer.MAX_VALUE;
            for (String w : words[i]) { int k = n.indexOf(Txt.norm(w)); if (k >= 0) at = Math.min(at, k); }
            if (at == Integer.MAX_VALUE) at = n.length();
            found.add(new int[]{at, kinds[i]});
            if (kinds[i] == Film.T_RANGOLI || kinds[i] == Film.T_PAINT) break;
        }
        if (found.isEmpty()) return 0;
        java.util.Collections.sort(found, new java.util.Comparator<int[]>() { public int compare(int[] a, int[] b) { return Integer.compare(a[0], b[0]); } });
        float total = 0;
        for (int[] f : found) {
            if (f[1] == Film.T_COOK) {
                // v43: "कुकर में दाल बनाती है और फिर दाल में तड़का लगाती है": each dish cooked in its turn
                for (String part : dishParts(sp)) { total += oneTask(s, " " + part + " ", Film.T_COOK, t + total, who) + 0.2f; }
                continue;
            }
            float d = oneTask(s, sp, f[1], t + total, who);
            // v44: on the phone while walking about ("फ़ोन पर बात करते हुए टहलता है", "paces the room on the phone")
            if (f[1] == Film.T_PHONE && Txt.has(sp, "टहल", "चहलकदमी", "घूमते हुए", "चलते हुए", "इधर-उधर", "walks", "walking", "paces", "pacing", "strolls", "wanders"))
                for (Film.Actor a : who) {
                    Film.Key now = a.stateAt(t + total);
                    if (now.anchor != Film.A_GROUND || now.body != Pose.STAND || a.look == null || !a.look.isHumanoid()) continue;
                    float x = xAt(a, t + total), dest = x < 640 ? Math.min(1100, x + 300) : Math.max(180, x - 300);
                    List<Film.Actor> me = new ArrayList<Film.Actor>(); me.add(a);
                    dest = freeNear(dest, t + total + d, me, 110);
                    if (Math.abs(dest - x) < 60) continue;
                    Film.Key k = keyAt(a, t + total + 0.3f); k.x = dest; k.moveDur = Math.max(1.6f, d - 0.5f); k.facing = dest > x ? 1 : -1; k.run = false;
                }
            total += d;
            if (found.size() > 1 && f != found.get(found.size() - 1)) total += 0.2f;
        }
        return total;
    }

    /** v43: what the cooking words make (Film.COOK_*): tea, rotis, a tadka, dal, chopping only, a whole meal, else a sabzi stirred. */
    static int dishOf(String sp) {
        boolean cookVerb = Txt.has(sp, "बना", "पका", "पकाती", "पकाता", "छौंक", "बघार", "cook", "makes", "making", "prepar", "stirs", "stirring");
        if (Txt.has(sp, "चाय", "कॉफ़ी", "कॉफी", " tea", "chai", "coffee")) return Film.COOK_TEA;
        if (Txt.has(sp, "रोटी", "रोटियाँ", "रोटियां", "चपाती", "पराठ", "पूरी", " तवे", " तवा", "बेलती", "बेलता", "roti", "chapati", "paratha", "rolls out", "flatbread")) return Film.COOK_ROTI;
        if (Txt.has(sp, "तड़का", "तडका", "छौंक", "बघार", "tadka", "tempering")) return Film.COOK_TADKA;
        if (Txt.has(sp, "दाल", " dal ", " dal.", "dal,", "कुकर", "cooker", "खिचड़ी", "राजमा", "छोले", "khichdi")) return Film.COOK_DAL;
        boolean chop = Txt.has(sp, "काट", "chop", "cuts the", "slices", "dices");
        if (chop && !cookVerb) return Film.COOK_CHOP;
        if (chop || Txt.has(sp, "खाना बना", "खाना पका", "भोजन बना", "भोजन पका", "रसोई में", "dinner", "lunch", "breakfast", "a meal", "the meal", "food")) return Film.COOK_MEAL;
        return Film.COOK_STIR;
    }

    /** v43: the sentence cut where one cooking ends and the next begins ("... बनाती है और फिर ... लगाती है"), each with a dish. */
    static List<String> dishParts(String sp) {
        List<String> out = new ArrayList<String>();
        String[] cuts = sp.split("और फिर|, फिर| फिर | और | then |, and | and then ");
        int last = -1;
        StringBuilder cur = new StringBuilder();
        for (String c : cuts) {
            cur.append(' ').append(c);
            boolean names = Txt.has(c, COOK) || Txt.has(c, "चाय", "रोटी", "रोटियाँ", "दाल", "तड़का", "सब्ज़ी", "सब्जी", "काट", "tea", "roti", "dal", "tadka", "chop");
            if (names) {
                int dish = dishOf(" " + cur + " ");
                if (dish != last || out.isEmpty()) { out.add(cur.toString()); last = dish; }
                else out.set(out.size() - 1, out.get(out.size() - 1) + " " + cur);
                cur.setLength(0);
            }
        }
        if (out.isEmpty()) out.add(sp);
        else if (cur.length() > 0) out.set(out.size() - 1, out.get(out.size() - 1) + " " + cur);
        return out;
    }

    /** v43: how long that cooking takes on the screen (s). */
    static float dishTime(int dish) {
        switch (dish) {
            case Film.COOK_MEAL: return 7.0f;
            case Film.COOK_DAL: return 6.0f;
            case Film.COOK_TEA: return 5.5f;
            case Film.COOK_ROTI: return 6.5f;
            case Film.COOK_TADKA: return 4.5f;
            case Film.COOK_CHOP: return 4.2f;
            default: return 5.0f;
        }
    }

    int dishesCooked;

    private float oneTask(String s, String sp, int kind, float t, List<Film.Actor> who) {
        if (kind == Film.T_READ && Txt.has(sp, "अख़बार", "अखबार", "newspaper")) kind = Film.T_PAPER;
        int dish = kind == Film.T_COOK ? dishOf(sp) : 0;
        float dur = kind == Film.T_PHONE ? 3.2f : kind == Film.T_COMB ? 3f : kind == Film.T_RANGOLI || kind == Film.T_PAINT ? 5f : kind == Film.T_COOK ? dishTime(dish) : 4.2f;
        float most = 0;
        for (Film.Actor a : who) {
            if (a.look == null || !a.look.isHumanoid()) continue;
            // v42: a task on the floor in front of them (the plants watered, the floor swept, a bucket, a stove, a
            // rangoli) is done on open floor: standing at a drawn sofa, प्रिया watered its arm — the water fell on it
            float walk = 0;
            Film.Key k0 = a.stateAt(t);
            if (seg != null && k0.visible && k0.body == Pose.STAND && k0.anchor == Film.A_GROUND
                    && (kind == Film.T_WATER || kind == Film.T_SWEEP || kind == Film.T_WASH || kind == Film.T_COOK || kind == Film.T_RANGOLI)) {
                float x = xAt(a, t);
                Film.Fx seat = seatNear(x, 330);
                if (seat != null) {
                    // turned away from it, the pot, the bucket or the stove stands on the open side; with no room there,
                    // a few steps out to open floor first
                    float away = x >= seat.x ? 1 : -1;
                    if (x + away * 150 >= 60 && x + away * 150 <= 1220 && seatNear(x + away * 150, 120) == null) {
                        if (k0.facing != away) { Film.Key tk = keyAt(a, t + 0.05f); tk.facing = away; walk = 0.35f; }
                    } else {
                        List<Film.Actor> self = java.util.Collections.singletonList(a);
                        float to = Float.NaN;
                        for (int k = 1; k <= 16 && Float.isNaN(to); k++) {
                            float c = x + (k % 2 == 1 ? 1 : -1) * ((k + 1) / 2) * 60f;
                            if (c >= 170 && c <= 1110 && seatNear(c, 380) == null && freeAt(c, t, self, 100)) to = c;
                        }
                        if (!Float.isNaN(to)) {
                            walk = Math.max(0.8f, Math.min(2.5f, Math.abs(to - x) / 170f));
                            Film.Key wk = keyAt(a, t + 0.05f);
                            wk.x = to; wk.moveDur = walk; wk.facing = to > x ? 1 : -1; wk.run = false;
                            walk += 0.1f;
                        }
                    }
                }
            }
            most = Math.max(most, walk);
            t += walk;
            // v43: cooking at the kitchen counter already in this part (they go to it), else at one set up where they
            // stand — it stays for the rest of the part, the dish left on it. The cook stands behind it (it hides
            // them from the hips down, their hands at the work on its top), facing the room.
            if (kind == Film.T_COOK && seg != null && a.stateAt(t).visible) {
                float x = xAt(a, t), face = a.stateAt(t).facing >= 0 ? 1 : -1;
                Film.Fx kit = null, old = null;
                for (Film.Fx o : seg.fx) if (o.type == Film.FX_KITCHEN) old = o;
                // (each cooking its own record at the same counter: the renderer shows the one under way)
                kit = fx(Film.FX_KITCHEN, seg.t0, 1e6f, old != null ? old.x : Math.max(200, Math.min(1080, x)), 0, a, null);
                kit.color = old != null ? old.color : (int) face;
                if (Math.abs(kit.x - x) > 30) {
                    float wd = Math.max(0.6f, Math.min(2.5f, Math.abs(kit.x - x) / 170f));
                    Film.Key wk = keyAt(a, t + 0.05f); wk.x = kit.x; wk.moveDur = wd; wk.facing = kit.x > x ? 1 : -1; wk.run = false;
                    if (wk.body == Pose.SIT || wk.body == Pose.LIE) { wk.body = Pose.STAND; wk.seat = -1; }
                    walk += wd + 0.1f; t += wd + 0.1f; most = Math.max(most, walk);
                }
                if (old != null) { Film.Key fk = keyAt(a, t + 0.05f); fk.facing = kit.color; }
                kit.a = a; kit.kind = dish; kit.y = t + 0.2f; kit.t2 = t + 0.2f + dur;
                dishesCooked++;
            }
            Film.Act act = new Film.Act(t + 0.2f, t + 0.2f + dur, Film.G_TASK);
            act.item = kind;
            act.how = dish;
            a.acts.add(act);
            float st = t + 0.35f, len = dur - 0.3f;
            switch (kind) {
                case Film.T_COOK: {
                    // v43: the sounds of what is cooked, in its stages: the knife on the board, the sizzle in the kadhai,
                    // the boil, the cooker's whistle, the tea poured
                    float c0 = t + 0.2f;
                    switch (dish) {
                        case Film.COOK_MEAL: film.sfx.add(sfxAt(Film.SFX_CHOP, c0 + 0.1f, 0.3f * dur - 0.15f, 0.32f, a)); film.sfx.add(sfxAt(Film.SFX_SIZZLE, c0 + 0.3f * dur, 0.55f * dur, 0.3f, a)); break;
                        case Film.COOK_DAL: film.sfx.add(sfxAt(Film.SFX_BOIL, c0, 0.7f * dur, 0.2f, a)); film.sfx.add(sfxAt(Film.SFX_WHISTLE, c0 + 0.5f * dur, 1.6f, 0.4f, a)); break;
                        case Film.COOK_TEA: film.sfx.add(sfxAt(Film.SFX_BOIL, c0 + 0.2f * dur, 0.55f * dur, 0.26f, a)); film.sfx.add(sfxAt(Film.SFX_POUR, c0 + 0.78f * dur, 0.17f * dur, 0.28f, a)); break;
                        case Film.COOK_ROTI: film.sfx.add(sfxAt(Film.SFX_SIZZLE, c0 + 0.42f * dur, 0.38f * dur, 0.16f, a)); break;
                        case Film.COOK_CHOP: film.sfx.add(sfxAt(Film.SFX_CHOP, c0 + 0.1f, 0.85f * dur - 0.1f, 0.34f, a)); break;
                        case Film.COOK_TADKA: film.sfx.add(sfxAt(Film.SFX_SIZZLE, c0 + 0.05f * dur, 0.6f * dur, 0.4f, a)); film.sfx.add(sfxAt(Film.SFX_SIZZLE, c0 + 0.68f * dur, 0.6f, 0.5f, a)); break;
                        default: film.sfx.add(sfxAt(Film.SFX_SIZZLE, st, 0.85f * dur, 0.3f, a));
                    }
                    break;
                }
                case Film.T_SWEEP: film.sfx.add(sfxAt(Film.SFX_SWEEP, st, len, 0.35f, a)); break;
                case Film.T_WASH: film.sfx.add(sfxAt(Film.SFX_SCRUB, st, len, 0.35f, a)); break;
                case Film.T_READ: case Film.T_PAPER: for (float pt = 1.4f; pt < dur; pt += 2.6f) film.sfx.add(sfxAt(Film.SFX_PAGE, t + 0.2f + pt, 0.4f, 0.3f, a)); break;
                case Film.T_WRITE: film.sfx.add(sfxAt(Film.SFX_SCRIBBLE, st, len, 0.3f, a)); break;
                case Film.T_BRUSH: film.sfx.add(sfxAt(Film.SFX_BRUSH, st, len, 0.3f, a)); break;
                case Film.T_WATER: film.sfx.add(sfxAt(Film.SFX_POUR, st + 0.3f, len - 0.3f, 0.3f, a)); break;
                default:
            }
            tasks++;
            if (who.size() == 1 && photo(a) && (kind == Film.T_PHONE || kind == Film.T_BRUSH)) { mealCU = a; mealCUt = t + 0.1f; }
            t -= walk;
        }
        return dur + 0.3f + most;
    }

    /** v42: the nearest drawn seat (a sofa, a chair, a bed, a stool, a log) within gap of x, or null. */
    private Film.Fx seatNear(float x, float gap) {
        Film.Fx best = null;
        for (Film.Fx f : seg.fx) if (f.type == Film.FX_SEAT && Math.abs(f.x - x) < gap && (best == null || Math.abs(f.x - x) < Math.abs(best.x - x))) best = f;
        return best;
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

    /** v41: a rainbow needs the sun behind the watcher: by day or in the evening light, never in a night sky. */
    private void rainbow(float t) {
        if (seg != null && seg.tod == Sets.NIGHT) { if (!nightRainbowNoted) film.notes.add("  ↳ no rainbow at night (it needs the sun)"); nightRainbowNoted = true; return; }
        open(Film.W_RAINBOW, t, 1f);
    }
    private boolean nightRainbowNoted;
    int skyChanges;

    /** Scenery named in a line of dialogue: it is there to be seen (no weather starts from talk). */
    /** v43: the text with waving ("waves goodbye", "waves to Mother", "waves his hand") taken out: what is left of "waves" is the sea's. */
    static String seaWords(String s) {
        return s.replaceAll("(?i)waves? (goodbye|bye|to |at |his |her |their |back|and |a hand|hello)", " ");
    }

    private void sceneryFrom(String s, float t) {
        if (s == null) return;
        if (boatIn(s)) { open(Film.W_BOAT, t, 1f); if (wOpen[Film.W_SEA] < 0 && !Txt.has(s, "नदी", "river", "तालाब", "lake", "झील")) open(Film.W_SEA, t, 0.6f); }
        if (Txt.has(seaWords(s), SEA) && !Txt.has(s, "जाएँगे", "जाएंगे", "चलेंगे", "will go", "let's go")) open(Film.W_SEA, t, 0.8f);
        if (Txt.has(s, RAINBOW)) rainbow(t);
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
        boolean rainBefore = wOpen[Film.W_RAIN] >= 0 || wOpen[Film.W_STORM] >= 0, snowBefore = wOpen[Film.W_SNOW] >= 0;
        // v43: words that only look like weather — a rainbow is no rain, flowers showered ("फूल बरसाता", "petals rain
        // down") are no rain, "after the rain" is the rain over, a window is no wind, waving goodbye is no sea
        String rs = s.replace("rainbow", " ").replace("Rainbow", " ").replace("इंद्रधनुष", " ").replace("इन्द्रधनुष", " ");
        boolean flowersRain = Txt.has(s, PETALS) || (Txt.has(s, "फूल", "फूलों", "पंखुड़ि", "petal", "flower") && Txt.has(s, "बरसा", "rain down", "rained down"));
        boolean afterRain = Txt.has(s, "बारिश के बाद", "बारिश रुकने के बाद", "बारिश थमने के बाद", "after the rain");
        if (afterRain) { closeWeather(Film.W_RAIN, t); closeWeather(Film.W_STORM, t); }
        if (Txt.has(s, RAIN_STOP)) { closeWeather(Film.W_RAIN, t); closeWeather(Film.W_STORM, t); }
        else if (Txt.has(s, HAILSTONES) && !negated(s)) {
            // v43: hail — hard rain with icy pellets in it
            open(Film.W_RAIN, t, 1.25f);
            open(Film.W_SNOW, t, 0.45f);
            d = 2.2f;
        }
        else if (Txt.hasStart(rs, RAIN) && !negated(s) && !flowersRain && !afterRain) {
            float st = Txt.has(s, "तेज़ बारिश", "तेज बारिश", "मूसलाधार", "heavy rain", "pouring", "downpour") ? 1.35f
                    : Txt.has(s, "हल्की", "बूँदाबाँदी", "बूंदाबांदी", "drizzl", "light rain") ? 0.5f : 1f;
            open(Film.W_RAIN, t, st);
            d = 2.2f;
        }
        if (Txt.has(s, DUST) && !negated(s)) { open(Film.W_DUST, t, 1f); open(Film.W_WIND, t, 1f); d = 2.2f; }
        else if (Txt.has(s, STORM) && !negated(s)) { open(Film.W_STORM, t, 1f); open(Film.W_RAIN, t, 1.3f); d = 2.2f; }
        String ws = s.replace("window", " ").replace("Window", " ").replace("rewind", " ").replace("unwind", " ").replace("winding", " ");
        if (Txt.has(s, WIND_STOP)) closeWeather(Film.W_WIND, t);
        else if (Txt.has(ws, WIND) && !negated(s)) {
            open(Film.W_WIND, t, Txt.has(s, WIND_STRONG) ? 1f : Txt.has(s, WIND_SOFT) ? 0.35f : 0.6f);
            d = Math.max(d, 1.6f);
        }
        if (Txt.has(s, SNOW) && !negated(s)) { open(Film.W_SNOW, t, 1f); d = Math.max(d, 2f); }
        // v44: a snowy place has snow on the ground (it is not falling unless the words say so)
        if (place && seg != null && Sets.outdoorSet(seg.set) && Txt.has(s, SNOW_GROUND)) seg.snowGround = true;
        // v34 (realism): rain or snow beginning while the characters are out in it — they look up, then cover their heads
        // (no umbrella) or shiver
        if (!place && seg != null && Sets.outdoorSet(seg.set)) {
            if (!rainBefore && (wOpen[Film.W_RAIN] >= 0 || wOpen[Film.W_STORM] >= 0)) d = Math.max(d, skyReaction(t, false));
            if (!snowBefore && wOpen[Film.W_SNOW] >= 0) d = Math.max(d, skyReaction(t, true));
        }
        // (fog in which "nothing can be seen" is fog all the same: that "नहीं" is the seeing's)
        boolean seeNot = Txt.has(s, "दिखाई नहीं", "नहीं दिख", "दिखता नहीं", "दिखती नहीं", "नज़र नहीं", "नजर नहीं", "can't see", "cannot see", "can not see", "nothing can be seen");
        if (Txt.has(s, FOG_STOP)) closeWeather(Film.W_FOG, t);
        else if (Txt.has(s, FOG) && (!negated(s) || seeNot) && !Txt.has(s, "धुंधला")) open(Film.W_FOG, t, Txt.has(s, "घना", "घने", "thick", "dense") || seeNot ? 1f : 0.7f);
        if (Txt.has(s, FIRE) && !Txt.has(s, "आग बबूला", "आग-बबूला")) {
            if (wOpen[Film.W_FIRE] < 0) fireX = freeSpot(t + 0.5f);
            open(Film.W_FIRE, t, 1f);
        }
        if (Txt.has(s, FIREFLY)) open(Film.W_FIREFLIES, t, 1f);
        // v43: the sky changing within the part — the sun going down, night falling, the sun rising, the full moon up
        boolean nightFalls = false;
        if (!place && seg != null && !negated(s)) {
            int sky = -1;
            if (Txt.has(s, "सूरज उग", "सूर्योदय", "सूरज निकल", "सुबह होने लग", "पौ फट", "भोर हो", "sun rises", "sunrise", "dawn breaks", "the sun comes up", "the sun is rising"))
                sky = Film.SKY_SUNRISE;
            else if (Txt.has(s, "रात हो जा", "रात होने लग", "रात हो गई", "रात हो चुकी", "अंधेरा होने लग", "अँधेरा होने लग", "night falls", "night has fallen", "it gets dark", "it grows dark"))
                sky = Film.SKY_NIGHTFALL;
            else if (Txt.has(s, "सूरज ढल", "सूर्यास्त", "सूरज डूब", "शाम होने लग", "शाम ढल", "आसमान नारंगी", "sun sets", "sunset", "the sun is setting", "the sun goes down", "dusk falls", "evening falls"))
                sky = Film.SKY_SUNSET;
            else if (Txt.has(s, "चाँद", "चांद", "चंद्रमा", "चन्द्रमा", "full moon", "the moon", "moonlight"))
                sky = Film.SKY_MOON;
            boolean have = false;
            if (sky >= 0) for (Film.Fx f : seg.fx) if (f.type == Film.FX_SKY && f.kind == sky) have = true;
            if (sky >= 0 && !have && !(sky == Film.SKY_SUNSET && seg.tod == Sets.NIGHT) && !(sky == Film.SKY_NIGHTFALL && seg.tod == Sets.NIGHT)) {
                fx(Film.FX_SKY, t + 0.1f, 1e6f, 0, 0, null, null).kind = sky;
                skyChanges++;
                d = Math.max(d, 2.6f);
                if (sky == Film.SKY_NIGHTFALL) nightFalls = true;
                if (sky == Film.SKY_SUNRISE) closeWeather(Film.W_STARS, t + 2f);
                if (sky == Film.SKY_MOON && Sets.outdoorSet(seg.set) && seg.tod != Sets.DAY) open(Film.W_STARS, t, 0.8f);
            }
        }
        // v41: nature as it is — stars in a dark sky only, a rainbow only with the sun up (not at night)
        if (nightFalls && Txt.has(s, STARS)) open(Film.W_STARS, t + 2.5f, 1f);
        else if (nightFalls && seg != null && Sets.outdoorSet(seg.set)) open(Film.W_STARS, t + 3.5f, 0.7f);
        if (Txt.has(s, STARS) && (seg == null || seg.tod == Sets.NIGHT || seg.tod == Sets.EVENING)) open(Film.W_STARS, t, 1f);
        if (Txt.has(s, RAINBOW)) rainbow(t);
        if (Txt.has(s, LEAVES)) open(Film.W_LEAVES, t, 1f);
        if (Txt.has(s, PETALS) || flowersRain) {
            if (place) open(Film.W_PETALS, t, 1f);
            else film.weather.add(new Film.Weather(Film.W_PETALS, t, t + 7, 1f));
            d = Math.max(d, 2.5f);
        }
        boolean rumble = Txt.has(s, "बादल", "आसमान", "आकाश", "cloud", "sky") && Txt.has(s, RUMBLE) && !Txt.has(s, "शेर", "बाघ", "lion", "tiger");
        if (!place && (Txt.has(s, THUNDER) || rumble) && !negated(s)) { lightning(t + 0.2f, film.weather.size() + (int) t); d = Math.max(d, 1.8f); }
        if (Txt.has(seaWords(s), SEA)) open(Film.W_SEA, t, Txt.has(s, "ऊँची लहर", "ऊंची लहर", "big waves", "high waves", "rough sea") ? 1.3f : 0.8f);
        if (boatIn(s)) { open(Film.W_BOAT, t, 1f); if (wOpen[Film.W_SEA] < 0 && !Txt.has(s, "नदी", "river", "तालाब", "lake", "झील")) open(Film.W_SEA, t, 0.6f); }
        if (Txt.has(s, CANDLE) && !negated(s)) { open(Film.W_CANDLES, t, 1f); candleKind = 0; }
        else if (Txt.has(s, DIYA) && !negated(s)) { open(Film.W_CANDLES, t, 1f); candleKind = 1; }
        else if (Txt.has(s, TORCH) && !negated(s)) { open(Film.W_CANDLES, t, 1f); candleKind = 2; }
        if (Txt.has(s, DARK_CLOUDS)) { open(Film.W_CLOUDS, t, 1f); cloudsDark = true; }
        else if (Txt.has(s, CLOUDS) && !Txt.has(s, "बादल गरज", "बादल छँट", "बादल छंट")) open(Film.W_CLOUDS, t, 0.8f);
        if (Txt.has(s, BIRDS_FLY)) open(Film.W_BIRDS, t, 1f);
        // v43: smoke spreading from something burning
        if (!place && Txt.has(s, "धुआँ फैल", "धुआं फैल", "धुंआ फैल", "धुएँ से भर", "धुएं से भर", "smoke fills", "smoke spreads", "smoke everywhere", "थोड़ा धुआँ", "धुआँ उठ", "धुआं उठ", "smoke rises")
                && !negated(s)) {
            float x = freeSpot(t + 0.5f);
            fx(Film.FX_SMOKE, t + 0.2f, t + 7f, x, ground - 40, null, null);
            d = Math.max(d, 2.0f);
        }
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
        } else if ((Txt.has(s, BALL) || Txt.has(s, "फल फेंक", "आम फेंक", "फूल फेंक", "fruit", "flower")) && Txt.has(s, THROW) && !Txt.has(s, STONE) && !Txt.has(s, SNOWBALL)) {
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
        if (!a.track.isEmpty()) { Film.Track r = a.trackAt(t); if (r != null) return r.x(t); }
        Film.Key prev = null, cur = a.keys.get(0);
        for (Film.Key k : a.keys) { if (k.t <= t) { prev = cur; cur = k; } else break; }
        if (cur.moveDur > 0 && t < cur.t + cur.moveDur && prev != null) {
            float u = (t - cur.t) / cur.moveDur;
            // v44: a move that ends in a crash or a skid speeds up and keeps its speed to the end (it stops dead)
            if (cur.hardStop) u = u < 0.3f ? u * u / (0.3f * 1.7f) : (2 * u - 0.3f) / 1.7f;
            else u = u * u * (3 - 2 * u);
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
        // v41: to the side of the one approached that is free — not onto a third one standing there
        if (standsAt(dest, t + dur, a, to)) {
            float other = ax < tx ? tx + gap : tx - gap;
            if (other > 150 && other < 1130 && !standsAt(other, t + dur, a, to)) dest = other;
        }
        if (Math.abs(dest - ax) < 10) return;
        // v41: one sitting or lying gets up first (never slides across the ground seated or lying down)
        Film.Key st = a.stateAt(t);
        float tm = t;
        if ((st.body == Pose.SIT || st.body == Pose.LIE || st.body == Pose.KNEEL) && st.anchor == Film.A_GROUND && a.look.isHumanoid()) {
            Film.Key up = a.at(t); up.body = Pose.STAND; up.seat = -1; up.eyesShut = false;
            tm = t + (st.body == Pose.LIE ? 0.8f : 0.45f);
            stoodToWalk++;
        }
        Film.Key k = a.at(tm);
        k.x = dest; k.moveDur = Math.max(0.3f, dur); k.facing = tx > dest ? 1 : -1;
        if (k.anchor != Film.A_GROUND && k.anchor != Film.A_CAGE) k.anchor = Film.A_GROUND;
    }

    /** v41: someone other than a and b stands within 70 px of x at t. */
    private boolean standsAt(float x, float t, Film.Actor a, Film.Actor b) {
        for (Film.Actor o : seg.actors) {
            if (o == a || o == b || o.look == null || !o.look.isHumanoid() || !o.stateAt(t).visible) continue;
            if (Math.abs(xAt(o, t) - x) < 70) return true;
        }
        return false;
    }

    private void runBackForth(Film.Actor a, float t, float dur) {
        float x0 = xAt(a, t);
        // v41: one sitting or lying gets up before running off after it
        Film.Key st = a.stateAt(t);
        if ((st.body == Pose.SIT || st.body == Pose.LIE) && a.look.isHumanoid()) { Film.Key up = a.at(t); up.body = Pose.STAND; up.seat = -1; up.eyesShut = false; t += 0.5f; stoodToWalk++; }
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
        // v41: up on the slide the camera looks up with them; on a swing going high it stays a medium shot
        float[] pl = playAt(a, t + 0.5f);
        if (pl != null && pl[1] > 6) zoom = Math.min(zoom, 1.1f);
        float y = ground - h * (zoom > 1.6f ? 0.8f : 0.62f) - (pl != null ? pl[0] : 0);
        // v34 (the situations guide): a seated character — in a wheelchair, on a chair — is framed at their own eye
        // level, not at a standing person's (the camera never looks down on them)
        if (k.body == Pose.SIT) { float fy = seatedFace(a, t + 0.5f); y = ground - h * (zoom > 1.6f ? fy : fy * 0.75f); eyeLevelShots++; }
        if (k.anchor == Film.A_BRANCH) y = ground - 450;
        if (k.body == Pose.LIE && pl == null) y = ground - 60;
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
        if (st.body == Pose.LIE) faceY = ground - h * 0.35f;
        // v34: a seated face (a chair, a throne, a wheelchair) is about two thirds of the standing height up, not a third:
        // the close shot is at the sitter's own eye level and the face is never cut
        else if (st.body == Pose.SIT) { faceY = ground - h * seatedFace(sp, t); eyeLevelShots++; }
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
        // v35: a user's standing photo lowered onto a seat reads best from the waist up: a seated speaker with no picture
        // of their own sitting is not framed wider than a medium shot (the shortened legs stay out of the frame)
        if (st.body == Pose.SIT && seatedPhoto(sp) && size < ShotPlanner.MEDIUM && !isolated) { size = ShotPlanner.MEDIUM; waistUp++; }
        float zoom = ShotPlanner.zoomFor(size, h);
        // a Dutch angle for menace, used sparingly (handbook ch. 5): at most once per scene
        float roll = menace && dutchUsed == 0 ? (dlgCount % 2 == 0 ? 1 : -1) * 2.4f : 0;
        if (roll != 0) { dutchUsed++; dutchCount++; }
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

    /**
     * An over-the-shoulder reverse is possible when the speaker stands on the ground near the listener and the
     * speaker's back view exists (made from the speaker's own picture, or given by the user).
     */
    private boolean overShoulder(Film.Actor sp, Film.Actor to, float t) {
        if (sp == null || to == null || art == null) return false;
        Film.Key ks = sp.stateAt(t), kt = to.stateAt(t);
        if (!ks.visible || ks.anchor != Film.A_GROUND || ks.body == Pose.LIE || kt.anchor != Film.A_GROUND) return false;
        Art.Sprite s = art.sprites.get(sp.c.id);
        if (s == null) return false;
        if (s.view(2) != null) return true;
        if (s.poses != null) for (Art.PoseSprite q : s.poses) if (Math.abs(Math.abs(q.angle) - 180) < 1 && q.pose == PoseSense.STAND) return true;   // v27: the user's own back picture
        return false;
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
            case Pose.SUSPICIOUS: return "One brow down, eyes narrowed, the head turned a little (asymmetrical brows)";
            case Pose.RELIEVED: return "Tension gone, a soft smile, the gaze softened";
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
            case Pose.SUSPICIOUS: return "Head angled, held back a little, arms in" + p;
            case Pose.RELIEVED: return "Shoulders drop, a breath out, the posture released" + p;
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
            case Sets.ROOFTOP: w = s.tod == Sets.NIGHT ? "city night rooftop wind traffic distant" : "city rooftop wind traffic distant drone"; break;
            case Sets.BASEMENT: w = "basement hum electric drip machine"; break;
            case Sets.ROOM: w = "room indoor quiet clock"; break;
            case Sets.STREET: w = "street traffic city horns crowd"; break;
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
        // the sounds of today's places (Nolan: real sounds): traffic and drones on a rooftop, a hum and a drip in a basement
        if (seg.set == Sets.ROOFTOP || seg.set == Sets.STREET) { film.sfx.add(new Film.Sfx(Film.SFX_TRAFFIC, t, 30, seg.set == Sets.STREET ? 0.35f : 0.18f)); film.sfx.add(new Film.Sfx(Film.SFX_WIND, t, 12, 0.12f)); }
        if (seg.set == Sets.ROOFTOP && Txt.has(where, "drone", "ड्रोन")) film.sfx.add(new Film.Sfx(Film.SFX_DRONE, t + 2f, 3.5f, 0.2f));
        if (seg.set == Sets.BASEMENT) { film.sfx.add(new Film.Sfx(Film.SFX_HUM, t, 30, 0.22f)); film.sfx.add(new Film.Sfx(Film.SFX_DRIP, t + 1f, 30, 0.18f)); }
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
