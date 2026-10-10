package com.tarun.kahani.app;

import android.content.Context;
import android.graphics.Bitmap;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Cloud;
import com.tarun.kahani.core.Edits;
import com.tarun.kahani.core.EdgeVoice;
import com.tarun.kahani.core.Grade;
import com.tarun.kahani.core.VoiceFx;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Mixer;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptAI;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.Synth;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Makes the film in the background: read script -> director -> voices -> sound mix -> frames -> MP4.
 * Lives outside the Activity so turning the phone does not stop it.
 */
public final class FilmJob implements Runnable {
    public static volatile FilmJob current;

    public final Project project;
    private final Context ctx;
    public volatile String stage = "Getting ready…";
    public volatile float progress;          // 0..1 overall
    public volatile boolean done, failed, cancelled;
    public volatile String error = "";
    public volatile String warning = "";
    /** What the director took from the library by itself ("Vrinda ← picture …"). */
    public volatile String info = "";
    public volatile Bitmap preview;         // latest rendered frame (small)
    public volatile long startedAt = System.currentTimeMillis();
    public volatile float filmSeconds;
    public volatile int voicedLines;        // lines that got a real voice (recording, sample, AI or phone)

    public FilmJob(Context ctx, Project p) {
        this.ctx = ctx.getApplicationContext();
        this.project = p;
    }

    private static Thread thread;
    /** Set once the finished film has been shown, so the app does not keep reopening it. */
    public volatile boolean seen;

    /** True while a job's thread is still working (also during the moments after "stop" is pressed). */
    public static synchronized boolean busy() { return thread != null && thread.isAlive(); }

    /**
     * Starts making the film. Returns the running job of the same project if there is one, or null when another
     * job (or a just-stopped one that is still finishing) is using the studio.
     */
    public static synchronized FilmJob start(Context ctx, Project p) {
        if (busy()) {
            if (current != null && !current.cancelled && current.project.dir.equals(p.dir)) return current;
            return null;
        }
        current = new FilmJob(ctx, p);
        current.askForPictures = Prefs.askUploads(ctx);           // v34: the user is asked for pictures before any 3D one is made
        FilmService.start(ctx);
        thread = new Thread(current, "film-job");
        thread.setPriority(Thread.NORM_PRIORITY);
        thread.start();
        return current;
    }

    public void cancel() { cancelled = true; }

    /** Paused by the user: every stage waits (drawing, voices, saving) until resumed, nothing is lost. */
    public volatile boolean paused;
    private volatile long pausedMs;

    public void pause(boolean p) { paused = p; }

    /** Waits here while paused (called between steps and before every frame). */
    void waitWhilePaused() {
        if (!paused) return;
        long t = System.currentTimeMillis();
        while (paused && !cancelled) {
            try { Thread.sleep(250); } catch (InterruptedException e) { break; }
        }
        pausedMs += System.currentTimeMillis() - t;
    }

    private void step(String s, float p) { stage = s; progress = p; }

    /** "about 3 min 20 s left" while making the video. */
    public String eta() {
        if (etaSeconds < 0) return "";
        return "About " + fmt(etaSeconds) + " left";
    }

    public volatile long etaSeconds = -1;

    /** Rough time-left estimate for the whole job from the overall progress. */
    /** The frames and the frame size of the film being made (known once the director has staged it), for the time left. */
    private int plannedFrames, plannedW, plannedH;

    /**
     * The time left (item 9): the steps before the video from their own progress so far, plus the video from the
     * speed this phone showed on its last film (milliseconds per frame per megapixel, measured and saved after
     * every render) — never the overall percentage, which the video dominates.
     */
    private void updateEta() {
        long el = (System.currentTimeMillis() - startedAt - pausedMs) / 1000;
        float pre = Math.min(progress, 0.36f) / 0.36f;
        long preLeft = pre > 0.05f && el > 5 ? (long) (el * (1 - pre) / pre) : -1;
        long video = estimatedRenderSeconds();
        etaSeconds = preLeft < 0 || video < 0 ? -1 : preLeft + video;
    }

    /** Seconds the video will take at this phone's measured speed, or -1 before the first film ever made here. */
    private long estimatedRenderSeconds() {
        if (plannedFrames <= 0) return -1;
        float ms;
        try { ms = Float.parseFloat(Prefs.get(ctx, "renderMsPerMpFrame", "0")); } catch (NumberFormatException e) { ms = 0; }
        if (ms <= 0) return -1;
        float mp = plannedW * (float) plannedH / 1e6f;
        return (long) (plannedFrames * ms * mp / 1000f) + 20;
    }

    /** How much memory the frames in flight may use: the heap's free room and half of what the phone has free, never under 48 MB. */
    private long memoryBudget() {
        Runtime rt = Runtime.getRuntime();
        long heapFree = rt.maxMemory() - (rt.totalMemory() - rt.freeMemory());
        long sysFree = Long.MAX_VALUE;
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
            if (am != null) { am.getMemoryInfo(mi); sysFree = Math.max(0, mi.availMem - mi.threshold) / 2; }
        } catch (Throwable ignored) { /* no activity manager here */ }
        // v25: the screen keeps room of its own (to open, cut out and save new pictures while the film is drawn)
        long reserve = Math.max(96L * 1024 * 1024, rt.maxMemory() / 5);
        return Math.max(48L * 1024 * 1024, Math.min((heapFree - reserve) * 3 / 4, sysFree));
    }

    /** v35: the memory the film's pictures may use: their pixels live outside the app's heap (Android 8+), in the phone's free memory. */
    private long pictureMemory() {
        try {
            android.app.ActivityManager am = (android.app.ActivityManager) ctx.getSystemService(Context.ACTIVITY_SERVICE);
            android.app.ActivityManager.MemoryInfo mi = new android.app.ActivityManager.MemoryInfo();
            if (am != null) {
                am.getMemoryInfo(mi);
                long free = mi.availMem - mi.threshold;
                if (free > 0) return free;
            }
        } catch (Throwable ignored) { /* no activity manager here */ }
        return Runtime.getRuntime().maxMemory();
    }

    static String hash(String s) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-1");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder b = new StringBuilder();
            for (int i = 0; i < 10; i++) b.append(String.format("%02x", d[i]));
            return b.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }

    /** The script to film: the AI's screenplay when the user chose it, else what they wrote. */
    static String scriptOf(Project p) {
        if ("1".equals(p.setting("useAi", "0")) && p.has("script_ai.txt")) {
            String ai = p.read("script_ai.txt");
            if (ai.trim().length() > 20) return ai;
        }
        return p.read("script.txt");
    }

    /** v34: the story as the Studio and the director read it — the script, with the user's own decisions on it applied. */
    static Story storyOf(Project p) { return overrides(p, ScriptParser.parse(scriptOf(p))); }

    /**
     * v34: the user's decisions kept in the story's settings: a character whose name reads like a voice (a radio, a
     * phone, an announcer) is pictured after all when the user chose "Picture it anyway" (pictured.<name> = 1).
     */
    static Story overrides(Project p, Story st) {
        if (p == null || st == null) return st;
        for (Story.CharacterDef c : st.characters) if (c.voiceOnly && "1".equals(p.setting("pictured." + c.displayName, ""))) c.voiceOnly = false;
        return st;
    }

    public void run() {
        Voices voices = null;
        // every job has its own working folder; old leftovers (e.g. after the app was killed) are removed
        File[] stale = ctx.getCacheDir().listFiles();
        if (stale != null) for (File d : stale) if (d.getName().startsWith("film_tmp")) MainActivity.deleteDir(d);
        File tmp = new File(ctx.getCacheDir(), "film_tmp_" + startedAt);
        try {
            tmp.mkdirs();
            // the meshes: one cell per screen pixel (the default), or two for a faster film on a slow phone
            float cell = Prefs.fastMesh(ctx) ? 2f : 1f;
            com.tarun.kahani.core.Rig.CELL_PX = cell;
            com.tarun.kahani.core.Nature.CELL_PX = cell;
            step("Reading the story…", 0.01f);
            String script = scriptOf(project);
            if (script.trim().length() < 10) throw new IllegalStateException("The story is empty. Write or paste a story first.");
            Story story = overrides(project, ScriptParser.parse(script));
            if (story.dialogueCount() == 0 && story.scenes.size() <= 1)
                throw new IllegalStateException("No dialogue found in the story. Write lines like  Name: \"dialogue\"  — or tap \"Read with AI\".");
            Edits ed = Edits.fromJson(project.read("edits.json"));
            // v33: a remake after an instruction (the story and the pictures unchanged) keeps every picture and every
            // approval: no library search, no proposals, no shot check again — only the change is applied
            String sig = hash(script) + "|" + hash(project.read("cast.txt"));
            remake = sig.equals(project.setting("made.sig", ""));
            if (remake) info = "Made again with your change — the pictures and approvals of the last film kept";
            com.tarun.kahani.core.TechnicalDirector.CUT_SECONDS = com.tarun.kahani.core.TechnicalDirector.CUT_SECONDS_DEFAULT * ed.shotLength;
            for (Story.CharacterDef c : story.cast()) {
                Float sc = ed.charScale.get(c.displayName);
                if (sc == null) for (java.util.Map.Entry<String, Float> e : ed.charScale.entrySet()) if (c.displayName.contains(e.getKey()) || c.aliases.contains(e.getKey())) sc = e.getValue();
                if (sc != null && c.look != null) c.look.height *= sc;
            }
            // the AI reads the story once more for nature and physics, also what is only implied (kept per script)
            String cueFile = "cues_" + hash(script) + ".json";
            if (project.has(cueFile)) ScriptAI.applyCues(story, project.read(cueFile));
            else if (Prefs.online(ctx)) {
                step("Director is reading the story for weather and nature…", 0.02f);
                try {
                    String cues = ScriptAI.natureCues(Prefs.cloud(ctx), story);
                    project.write(cueFile, cues);
                    ScriptAI.applyCues(story, cues);
                } catch (Exception e) {
                    // the director's own reading of the words still works
                }
            }

            // the phone's library first: pictures and voices that clearly fit, no button needed
            step("Director is looking through your library…", 0.025f);
            String fromLib = remake ? "" : AutoLibrary.fill(ctx, project, story);
            if (fromLib.length() > 0) info = "Taken from your library: " + fromLib;
            check();
            if (!remake && Prefs.online(ctx) && Prefs.autoArt(ctx)) makeMissingPictures(story, ed);
            // free pictures of the story's important objects (Fluent Emoji 3D on GitHub, MIT), for inserts
            if (Prefs.online(ctx) && Prefs.freeObjects(ctx)) {
                check();
                step("Fetching free pictures of the story's objects…", 0.022f);
                java.util.List<String> got = new java.util.ArrayList<String>();
                int n = FreeArt.fetchFor(project, story, Prefs.cloud(ctx), Library.get(ctx), 6, got);
                if (n > 0) objectNotes = got;
            }
            // v34: the 3D maker only when it must — whatever still has no picture after the library is asked of the
            // user first: the film waits on this screen while they add pictures (each row has its button); it goes on
            // the moment everything has one, or when they choose to have the rest built in 3D or drawn
            boolean build3d = Prefs.studio3d(ctx);
            if (askForPictures && !remake) {
                java.util.List<String> needed = neededPictures(story);
                if (!needed.isEmpty()) {
                    StringBuilder nb = new StringBuilder();
                    for (int i = 0; i < needed.size() && i < 8; i++) nb.append(i > 0 ? ", " : "").append(needed.get(i));
                    if (needed.size() > 8) nb.append(" and ").append(needed.size() - 8).append(" more");
                    picturesNeeded = nb.toString();
                    stage = "Waiting for you: please add pictures of " + picturesNeeded;
                    pictureChoice = 0;
                    picturesWaiting = true;
                    while (picturesWaiting && !cancelled) {
                        try { Thread.sleep(400); } catch (InterruptedException e) { break; }
                        if (neededPictures(story).isEmpty()) break;
                    }
                    picturesWaiting = false;
                    check();
                    if (pictureChoice == 2) build3d = false;
                    else if (pictureChoice == 1) build3d = true;
                    if (pictureChoice == 0) info = "Thank you — every picture is there now";
                }
            }
            // Studio 3D: whatever still has no picture (AI off, offline, or the service down) is built in three
            // dimensions on the phone — characters with their face points, places with their floor line
            if (build3d && !remake) {
                check();
                step("Reading the style of your pictures…", 0.024f);
                final com.tarun.kahani.core.StyleCue cue = Studio3DArt.styleCue(project, story, Library.get(ctx));
                styleNote = cue.describe();
                boolean ask = Prefs.ask3d(ctx);
                Studio3DArt.Progress sp = new Studio3DArt.Progress() {
                    public void at(String what) { check(); step(what + "…", 0.025f); }
                };
                boolean online = Prefs.online(ctx);
                com.tarun.kahani.core.ImageTo3D.forgetFailures();
                int made = Studio3DArt.makeMissing(project, story, ed, Library.get(ctx), ctx, cue, ask, sp, online && Prefs.freeModels(ctx) ? Prefs.cloud(ctx) : null, online && Prefs.freeModels(ctx));
                // every character with a picture gets its views from that picture (three-quarter, side, back)
                made += Studio3DArt.makeAllViews(project, story, Library.get(ctx), ctx, cue, ask, Prefs.meshyKey(ctx), online ? Prefs.cloud(ctx) : null, sp, online && Prefs.freeSpaces(ctx));
                if (made > 0) notes3d = made;
                // the director asks: every picture the studio made is a proposal until the user accepts it (in the
                // Studio, where pictures are chosen, or on the progress screen); a rejected one is deleted and never used
                if (ask && !Studio3DArt.proposals(project).isEmpty()) {
                    int n = Studio3DArt.proposals(project).size();
                    stage = "Waiting for you: " + n + " picture(s) the studio made in 3D need your decision (✔ Use / ✖ Reject)";
                    proposalsWaiting = true;
                    while (proposalsWaiting && !cancelled && !Studio3DArt.proposals(project).isEmpty()) {
                        try { Thread.sleep(300); } catch (InterruptedException e) { break; }
                    }
                    proposalsWaiting = false;
                    check();
                }
            }
            step("Preparing pictures (removing backgrounds)…", 0.03f);
            // v26: pictures are read at a size that fits the output (sharper at 1080p, lighter at 480p), within the memory left
            // v30: a phone with a large heap reads the pictures larger (sharper close-ups); a small one stays light
            // v35: characters at twice the output's height and places at 1.6 times its width (sharper close-ups and
            // push-ins), as far as the phone's memory allows for this film's number of pictures (Art.sizesFor)
            String castText = project.read("cast.txt");
            int[] counts = Art.pictureCounts(castText);
            int[] sides = Art.sizesFor(ed.height, Runtime.getRuntime().maxMemory(), pictureMemory(), counts[0], counts[1]);
            Art.spriteSide = sides[0];
            Art.backdropSide = sides[1];
            Art art = Art.fromManifest(castText, story, project.loader());
            check();

            Director.Options opt = new Director.Options();
            opt.aspect = ed.aspect;
            opt.subtitles = ed.subtitles;
            opt.narrator = false;
            opt.narrateTitle = false;
            opt.pace = ed.speed;
            opt.sounds = Library.get(ctx).soundLib();   // the user's own effects play where the story mentions them
            opt.onTwos = Prefs.onTwos(ctx);              // Spider-Verse stepping, only when the user asks for it
            opt.outHeight = ed.height;                   // v35: close-ups never enlarge a picture past 1.5x at this size
            Director dir = new Director(story, opt);
            Film film = dir.prepare();
            // v33: "scene 2 brighter", "the cave darker": the instruction's light on that part alone
            for (Film.Seg sg : film.segs) {
                if (sg.scene < 0 || sg.scene >= story.scenes.size()) continue;
                Story.Scene sc = story.scenes.get(sg.scene);
                for (java.util.Map.Entry<String, Float> e : ed.sceneBright.entrySet()) {
                    String k = e.getKey();
                    boolean byNumber = k.matches("\\d+") && String.valueOf(sc.number).equals(k);
                    boolean byPlace = !k.matches("\\d+") && (com.tarun.kahani.core.Txt.has(sc.setting, k) || com.tarun.kahani.core.Txt.has(sc.title, k) || com.tarun.kahani.core.Txt.has(com.tarun.kahani.core.Sets.label(sg.set), k));
                    if (byNumber || byPlace) sg.bright = e.getValue();
                }
            }

            // ---------------- voices
            step("Preparing voices…", 0.06f);
            voices = new Voices();
            boolean ttsOk = voices.init(ctx, story.hindi);
            Cloud cloud = Prefs.online(ctx) && Prefs.aiVoices(ctx) ? Prefs.cloud(ctx) : null;
            boolean aiVoices = cloud != null && cloud.hasGemini();
            boolean natural = Prefs.online(ctx) && Prefs.naturalVoices(ctx);
            if (natural) voices.edge = new EdgeVoice();
            if (!ttsOk && !aiVoices && !natural) warning = "No Text-to-Speech engine found on this phone — check Settings > Text-to-speech.";
            else if (ttsOk && !voices.languageOk && !aiVoices && !natural) warning = (story.hindi ? "Hindi" : "English") + " voice is not downloaded on this phone — download it in Settings > Text-to-speech.";
            Map<Story.CharacterDef, Voices.Cast> cast = voices.castAll(story, project);
            Library lib = Library.get(ctx);
            // ElevenLabs with the user's own key: the most lifelike voices; each character keeps its voice
            java.util.List<com.tarun.kahani.core.Eleven.Voice> elv = null;
            String elKey = Prefs.get(ctx, "elevenKey", "");
            if (Prefs.online(ctx) && elKey.trim().length() > 10) {
                try {
                    voices.eleven = new com.tarun.kahani.core.Eleven(Prefs.cloud(ctx), elKey);
                    elv = voices.eleven.voices();
                    if (elv.isEmpty()) { voices.eleven = null; elv = null; }
                } catch (Exception e) {
                    voices.eleven = null;
                    warning = "ElevenLabs could not be reached (" + e.getMessage() + ") — the natural voices were used.";
                }
            }
            java.util.Set<String> elUsed = new java.util.HashSet<String>();
            int ci = 0;
            for (Story.CharacterDef c : story.characters) {
                Voices.Cast k = cast.get(c);
                if (k == null) { k = Voices.defaultCast(c.look); cast.put(c, k); }
                // voice effects asked for in the edit box ("make the king's voice raspy")
                k.style = com.tarun.kahani.core.VoiceStyle.withEdits(k.style != null ? k.style : com.tarun.kahani.core.VoiceStyle.forCharacter(c), ed.styleFor(c.displayName));
                String sid = project.setting("vsample." + c.displayName, "");
                Library.Item it = lib.byId(sid);
                if (it != null) {
                    float[] pcm = AudioIO.decode(ctx, it.path, 60);
                    if (pcm != null) { k.sample = VoiceFx.profile(pcm, Synth.SR); k.sampleId = it.id; }
                }
                if (natural) {
                    EdgeVoice.Cast chosen = EdgeVoice.Cast.parse(project.setting("evoice." + c.displayName, ""));
                    k.edge = k.sample != null ? EdgeVoice.forSample(k.sample.pitch, story.hindi)
                            : chosen != null ? chosen
                            : com.tarun.kahani.core.VoiceMatch.adjust(EdgeVoice.castFor(c.look, c.age, story.hindi, ci), com.tarun.kahani.core.VoiceMatch.want(c));
                }
                if (aiVoices) {
                    String gv = project.setting("gvoice." + c.displayName, "");
                    k.gemini = gv.length() > 0 ? gv : Cloud.geminiVoiceFor(c.look, ci);
                }
                ci++;
            }
            if (elv != null) {
                // the whole cast at once, closest fits first; a character keeps its voice in every later film
                List<Story.CharacterDef> need = new java.util.ArrayList<Story.CharacterDef>();
                for (Story.CharacterDef c : story.characters) {
                    String id = project.setting("elvoice." + c.displayName, "");
                    boolean known = false;
                    for (com.tarun.kahani.core.Eleven.Voice v : elv) if (v.id.equals(id)) known = true;
                    if (known) { cast.get(c).eleven = id; elUsed.add(id); } else need.add(c);
                }
                if (!need.isEmpty()) {
                    java.util.List<com.tarun.kahani.core.Eleven.Voice> free = new java.util.ArrayList<com.tarun.kahani.core.Eleven.Voice>();
                    for (com.tarun.kahani.core.Eleven.Voice v : elv) if (!elUsed.contains(v.id)) free.add(v);
                    if (free.isEmpty()) free = elv;
                    com.tarun.kahani.core.Look[] ls = new com.tarun.kahani.core.Look[need.size()];
                    int[] ages = new int[need.size()];
                    String[] ws = new String[need.size()];
                    for (int i = 0; i < need.size(); i++) {
                        Story.CharacterDef c = need.get(i);
                        Voices.Cast k = cast.get(c);
                        ls[i] = c.look; ages[i] = c.age;
                        ws[i] = com.tarun.kahani.core.VoiceMatch.voiceText(c.description) + " " + (k.style == null ? "" : k.style.label());
                    }
                    int[] got = com.tarun.kahani.core.Eleven.assign(free, ls, ages, ws, story.hindi);
                    for (int i = 0; i < need.size(); i++) {
                        if (got[i] < 0) continue;
                        String id = free.get(got[i]).id;
                        cast.get(need.get(i)).eleven = id;
                        elUsed.add(id);
                        project.setSetting("elvoice." + need.get(i).displayName, id);
                    }
                }
            }
            Voices.Cast narratorCast = new Voices.Cast();
            narratorCast.pitch = 1.0f; narratorCast.rate = 0.92f;
            if (aiVoices) narratorCast.gemini = "Charon";
            if (natural) narratorCast.edge = EdgeVoice.narrator(story.hindi);
            if (elv != null) {
                com.tarun.kahani.core.Look nl = new com.tarun.kahani.core.Look();
                nl.kind = com.tarun.kahani.core.Look.MAN;
                com.tarun.kahani.core.Eleven.Voice nv = com.tarun.kahani.core.Eleven.pick(elv, nl, 45, "warm calm deep", story.hindi, elUsed);
                if (nv != null) narratorCast.eleven = nv.id;
            }
            Library.Item ns = lib.byId(project.setting("vsample.narrator", ""));
            if (ns != null) {
                float[] pcm = AudioIO.decode(ctx, ns.path, 60);
                if (pcm != null) { narratorCast.sample = VoiceFx.profile(pcm, Synth.SR); narratorCast.sampleId = ns.id; }
                if (natural && narratorCast.sample != null) narratorCast.edge = EdgeVoice.forSample(narratorCast.sample.pitch, story.hindi);
            }

            File vdir = new File(project.dir, "voices");
            vdir.mkdirs();
            final File[] lineFiles = new File[film.lines.size()];
            int failedLines = 0, aiLines = 0, naturalLines = 0;
            String[] err = new String[1];
            for (int i = 0; i < film.lines.size(); i++) {
                check();
                Film.Line l = film.lines.get(i);
                String who = l.who == null ? "कथावाचक" : l.who.displayName;
                step("Voice: " + (l.who == null ? "Narrator" : who) + " (" + (i + 1) + "/" + film.lines.size() + ")", 0.06f + 0.22f * i / Math.max(1, film.lines.size()));
                updateEta();
                Voices.Cast k = l.who == null ? narratorCast : cast.get(l.who);
                float[] v = null;
                // 1) the user's own recording of this exact line
                File rec = new File(project.dir, "lines/" + hash(who + "|" + l.text) + ".wav");
                if (rec.exists()) v = AudioIO.decode(ctx, rec.getAbsolutePath());
                if (v != null) v = Voices.trim(v);
                // 2) made before with the same voice settings (re-render after an edit)
                File cached = new File(vdir, hash(who + "|" + l.text + "|" + l.emotion + "|" + l.manner + "|" + (k == null ? "" : k.signature())) + ".wav");
                if (v == null && cached.exists()) v = AudioIO.readRaw(cached);
                // 3) speak it now
                if (v == null && (ttsOk || aiVoices || natural)) {
                    v = voices.speak(l, k, tmp, i, aiVoices ? cloud : null, err);
                    if (v != null) {
                        if (voices.lastEngine.startsWith("AI")) aiLines++;
                        if (voices.lastEngine.startsWith("natural")) naturalLines++;
                        // a fallback voice is not kept, so the next "Make again" tries the natural voice again
                        if (!voices.usedFallback) try { AudioIO.writeRaw(cached, v); } catch (IOException ignored) {}
                    }
                }
                if (v != null) {
                    float rate = ed.rateFor(who), pitch = ed.pitchFor(who);
                    if (rate != 1f) v = VoiceFx.stretch(v, 1f / rate);
                    if (pitch != 1f) v = VoiceFx.pitch(v, pitch);
                    File lf = new File(tmp, "v" + i + ".wav");
                    AudioIO.writeRaw(lf, v);
                    lineFiles[i] = lf;
                    voicedLines++;
                    l.dur = v.length / (float) Synth.SR;
                    l.env = Mixer.envelope(v, Synth.SR);
                    l.shape = Mixer.shape(v, Synth.SR);
                } else {
                    failedLines++;
                    l.dur = Director.estimate(l.text);
                    l.env = null;
                }
            }
            if (aiVoices && voices.aiOff && warning.length() == 0)
                warning = "The AI voice limit for today is used up or the key is wrong — remaining lines used the phone voice. " + (err[0] == null ? "" : err[0]);
            if (natural && voices.edgeOff && warning.length() == 0)
                warning = "Could not reach the natural voice service (check internet) — some lines used the phone voice. Tap \"Make again\" with internet on. " + (err[0] == null ? "" : err[0]);
            if (failedLines > 0 && warning.length() == 0)
                warning = failedLines + " lines could not be spoken — check the phone's Text-to-Speech settings.";
            voices.shutdown();
            voices = null;

            // ---------------- direction & sound
            step("Director is staging the scenes…", 0.29f);
            film = dir.direct(art);
            // background sounds the user picked for parts of the story
            for (Film.Amb a : film.ambience) {
                Film.Seg sg = film.segAt(a.t0 + 0.05f);
                if (sg == null || sg.scene < 0 || sg.scene >= story.scenes.size()) continue;
                Library.Item it = lib.byId(project.setting("amb." + story.scenes.get(sg.scene).number, ""));
                if (it != null) a.words = "#" + it.path;
            }
            // real recordings for backgrounds the library has none of yet (free, saved for every later film)
            if (Prefs.online(ctx)) {
                step("Finding real recordings for the backgrounds…", 0.295f);
                java.util.List<String> got = new java.util.ArrayList<String>();
                FreeSounds.fetchFor(ctx, film, lib, Prefs.cloud(ctx), 3, got);
                if (!got.isEmpty()) {
                    StringBuilder b = new StringBuilder();
                    for (String g : got) b.append(b.length() > 0 ? ", " : "").append(g);
                    info = (info.length() > 0 ? info + "\n" : "") + "New in your library: " + b;
                }
                check();
            }
            if (film.duration > 30 * 60 + 30) warning = "The film is longer than 30 minutes (" + fmt((long) film.duration) + ") — it will take longer to make.";
            film.subtitles = ed.subtitles;
            filmSeconds = film.duration;
            { int[] psz = ed.size(); plannedFrames = (int) Math.ceil(film.duration * (Prefs.fps30(ctx) ? 30 : 24)); plannedW = psz[0]; plannedH = psz[1]; }
            updateEta();
            check();
            // pipeline steps 1-2: the Character Lock Sheet of every character and the Location Lock Plate of every
            // place, saved with the film (and shown first in Human QC)
            lockSheets(film, art, dir, ed);
            // the director's manual (1.1, 2.3): the project asset inventory and the facial identity specification of
            // every character, written with the film
            step("Writing the asset inventory and the facial identity specifications…", 0.294f);
            film.shotList += "\n" + com.tarun.kahani.core.DirectorsManual.inventory(Studio3DArt.inventory(project, story, lib)) + "\n" + Studio3DArt.facialSpecs(project, story, Prefs.humanQc(ctx));
            // the mix comes before Human QC now: the animatic (the manual 3.7) plays the real voices, music and sounds
            step("Mixing music and sounds…", 0.295f);
            final File mix = new File(tmp, "mix.pcm");
            final java.io.OutputStream mo = new java.io.BufferedOutputStream(new java.io.FileOutputStream(mix), 1 << 16);
            final byte[] mb = new byte[Synth.SR * 8 * 2 * Mixer.CHANNELS];
            Mixer.mixTo(film, new Mixer.VoiceSource() {
                public float[] voice(int i) { return i < lineFiles.length && lineFiles[i] != null ? AudioIO.readRaw(lineFiles[i]) : null; }
            }, lib.soundLib(), ed, new Mixer.Sink() {
                public void write(short[] buf, int n) throws IOException {
                    for (int i = 0; i < n; i++) { mb[2 * i] = (byte) buf[i]; mb[2 * i + 1] = (byte) (buf[i] >> 8); }
                    mo.write(mb, 0, n * 2);
                    check();
                }
            }, new Mixer.Progress() {
                public void update(float f) { progress = 0.30f + 0.05f * f; }
            });
            mo.close();
            check();
            // the director's manual (V): the mix checked for missing dialogue, gaps and clipping
            audioCheck(film, mix, lineFiles);
            // Human QC (protocol pipeline step 4): the animatic and the first frame of every shot, checked by the user
            // before the film is made; their fixes are applied, then the film is made
            if (Prefs.humanQc(ctx) && !remake) humanQc(film, art, dir, ed, tmp, mix);
            else if (remake) film.shotList += "\nHUMAN QC: a remake with your change — the first frames approved with the last film stand.\n";
            // pipeline step 6: every shot played frame by frame at check size (the frames seen one by one at 0.25x
            // speed); a shot that boils or shakes has its motion cut by 80% and is played again from the same first frame
            finalCheck(film, art, ed);
            // the scene maker guide (9.3): a keyframe for every shot — one picture per shot, saved with the story
            shotPictures(film, art, ed);

            // ---------------- video
            int[] want = ed.size();
            int fps = Prefs.fps30(ctx) ? 30 : 24;          // smooth motion (v23): 30 fps unless switched off in Settings
            File out = new File(project.dir, "film_new.mp4");
            Grade grade = new Grade(ed);
            // if this phone's encoder refuses a size, try smaller ones automatically
            float[] scales = {1f, 0.6667f, 0.4444f};
            Exception last = null;
            boolean ok = false;
            for (int i = 0; i < scales.length && !ok; i++) {
                int[] size = VideoWriter.supportedSize(Math.round(want[0] * scales[i]), Math.round(want[1] * scales[i]));
                if (i > 0 && size[1] >= want[1] * scales[i - 1] - 1) continue;
                try {
                    renderVideo(film, art, mix, grade, size[0], size[1], fps, ed.bitrateFactor(), out);
                    ok = true;
                } catch (CancelledException e) {
                    throw e;
                } catch (Exception e) {
                    if (cancelled) throw new CancelledException();
                    last = e;
                    warning = "Video " + size[0] + "x" + size[1] + " could not be made, trying a smaller size…";
                }
            }
            if (!ok) throw last != null ? last : new IllegalStateException("The video could not be made");
            check();
            File fin = project.film();
            // version discipline (handbook ch. 15): the previous film is kept as film_previous.mp4, never overwritten
            if (fin.exists()) {
                File prev = project.file("film_previous.mp4");
                if (prev.exists()) prev.delete();
                if (!fin.renameTo(prev)) fin.delete();
            }
            if (!out.renameTo(fin)) throw new IllegalStateException("The film could not be saved");
            project.setSetting("filmSeconds", String.valueOf((int) film.duration));
            project.setSetting("madeAt", String.valueOf(System.currentTimeMillis()));
            project.setSetting("made.sig", hash(script) + "|" + hash(project.read("cast.txt")));     // v33: a remake with the same story and pictures keeps everything
            // the director's manual (IV stage 9, 3.10, V): the exported file verified, then the three-level review, the
            // five gates and the QA checklist from everything that was checked — never a finished claim before this
            film.shotList += "\n" + exportCheck(fin, film);
            film.shotList += "\n" + review(film);
            // the director's shot list with every check (validation, Human QC, the final QC of steps 6 and 8), kept with the film
            project.write("qc.txt", film.shotList);
            // v38 (the animated director's craft): the edit list with scene markers, beside the film (subtitles are added
            // after the film by an instruction — "add subtitles" — and burnt into the picture)
            try {
                project.write("edit_list.txt", com.tarun.kahani.core.FilmCraft.editList(film, fps, film.story != null ? film.story.title : project.name()));
            } catch (Exception e) { android.util.Log.w("Kahani", "delivery files: " + e); }
            project.setSetting("saved", "0");
            if (aiLines > 0) project.setSetting("aiLines", String.valueOf(aiLines));
            // the thumbnail and the poster, made separately in their own formats (extras: the film is done without them)
            try { stillPages(film, art, dir); } catch (CancelledException e) { throw e; } catch (Throwable e) { android.util.Log.w("Kahani", "still pages: " + e); }
            step("Your film is ready!", 1f);
            etaSeconds = -1;
            done = true;
        } catch (CancelledException e) {
            stage = "Stopped";
        } catch (OutOfMemoryError e) {
            error = "The phone ran out of memory. Choose 720p (or type \"smaller file size\") and try again.";
            failed = true;
        } catch (Throwable e) {
            error = e.getMessage() != null && e.getMessage().length() > 0 ? e.getMessage() : ("Error: " + e.getClass().getSimpleName());
            failed = true;
        } finally {
            if (voices != null) voices.shutdown();
            MainActivity.deleteDir(tmp);
        }
    }

    // ---------------------------------------------------------------- Human QC

    /** One still to check: {file, caption, kind ("char" or "shot")}. */
    public final java.util.List<String[]> qcItems = new java.util.ArrayList<String[]>();
    /** True while the job waits for the user to check the shots. */
    public volatile boolean qcWaiting;
    /** v33: true when this run only applies an instruction to the last film (story and pictures unchanged). */
    public boolean remake;
    /** v34: the still-picture checklist of each picture the AI service made for this film. */
    final java.util.List<String> stillQaNotes = new java.util.ArrayList<String>();
    /** The job waits for the user's decision on the pictures the studio made in 3D (proposals in the manifest). */
    public volatile boolean proposalsWaiting;
    /** v34: started from the app: before any 3D picture is made, the user is asked for the missing pictures. */
    public boolean askForPictures;
    /** v34: the film waits for the user's pictures (picturesNeeded); pictureChoice: 0 waiting, 1 build the rest in 3D, 2 draw them. */
    public volatile boolean picturesWaiting;
    public volatile String picturesNeeded = "";
    public volatile int pictureChoice;
    public void choosePictures(int choice) { pictureChoice = choice; picturesWaiting = false; }

    /** v34: the characters and places of the story with no picture yet (names to show). */
    java.util.List<String> neededPictures(com.tarun.kahani.core.Story story) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        try {
            for (String[] t : AutoLibrary.missingTargets(project, story)) if (t[0].startsWith("char:") || t[0].startsWith("place:")) out.add(t[1]);
        } catch (Throwable ignored) { }
        return out;
    }
    public void proposalsDone() { proposalsWaiting = false; }
    private String styleNote = "";
    /** The user's fix per shot (index into film.shots → Director.FIX_*), set by the screen before approving. */
    public final java.util.Map<Integer, Integer> qcFixes = new java.util.concurrent.ConcurrentHashMap<Integer, Integer>();
    /** Shot index of each "shot" item in qcItems. */
    public final java.util.List<Integer> qcShotIndex = new java.util.ArrayList<Integer>();

    /** The user has checked the shots: the fixes are applied and the film is made. */
    public void approve() { qcWaiting = false; }

    /**
     * RULE_RESIZE_2 / 4 / 7: the picture is asked for natively in its shape; its real width and height are checked
     * (a stretched or cropped picture is never used — within 5% of the shape asked for); a wrong one is made again
     * once; a size the service refuses falls back to a smaller native size of the same shape.
     */
    static byte[] makePictureNative(Cloud cloud, String prompt, int w, int h, int seed) throws IOException {
        int[][] tries = {{w, h, seed}, {w, h, seed + 1000}, {w > h ? 1280 : Math.round(1280f * w / h), w > h ? Math.round(1280f * h / w) : 1280, seed + 2000}};
        IOException last = null;
        for (int[] tr : tries) {
            try {
                byte[] img = cloud.makePicture(prompt, tr[0], tr[1], tr[2]);
                android.graphics.BitmapFactory.Options o = new android.graphics.BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                android.graphics.BitmapFactory.decodeByteArray(img, 0, img.length, o);
                if (o.outWidth <= 0 || o.outHeight <= 0) throw new IOException("not a picture");
                float want = tr[0] / (float) tr[1], got = o.outWidth / (float) o.outHeight;
                if (Math.abs(got / want - 1) <= com.tarun.kahani.core.PixarLead.STRETCH_TOLERANCE) return img;
                last = new IOException("picture came back in the wrong shape (" + o.outWidth + "x" + o.outHeight + ")");
            } catch (IOException e) { last = e; }
        }
        throw last == null ? new IOException("no picture") : last;
    }

    /** The thumbnail (1280x720) and the poster (1080x1920), made natively from the hero (RULE_RESIZE_8), saved next to the film. */
    private void stillPages(Film film, Art art, Director dir) throws IOException {
        Film.Seg[] pages = dir.stills();
        String[] names = {"thumbnail.jpg", "poster.jpg"};
        int[][] sizes = {{1280, 720}, {1080, 1920}};
        for (int i = 0; i < pages.length; i++) {
            step(i == 0 ? "Making the thumbnail…" : "Making the poster…", 0.99f);
            int w = sizes[i][0], h = sizes[i][1];
            Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
            AndroidGfx g = new AndroidGfx(bmp, 4);
            try {
                Renderer r = new Renderer(film, art);
                r.renderSeg(g, pages[i], 0.5f);
                int[] px = new int[w * h];
                bmp.getPixels(px, 0, w, 0, 0, w, h);
                new com.tarun.kahani.core.FilmLook(w, h).apply(px, com.tarun.kahani.core.FilmLook.forSeg(pages[i], new com.tarun.kahani.core.FilmLook.Params()));
                bmp.setPixels(px, 0, w, 0, 0, w, h);
                java.io.FileOutputStream o = new java.io.FileOutputStream(project.file(names[i]));
                bmp.compress(Bitmap.CompressFormat.JPEG, 90, o);
                o.close();
            } finally {
                g.release();
                bmp.recycle();
            }
        }
    }

    /** How many pictures Studio 3D made for this film (shown in the notes). */
    private int notes3d;
    /** The free object pictures fetched for this film (shown in the notes). */
    private java.util.List<String> objectNotes;

    /** The lock sheets and location plates made for this film: {file, caption, kind ("char" or "place")}. */
    public final java.util.List<String[]> lockItems = new java.util.ArrayList<String[]>();

    /** One part rendered on its own into a JPEG (a lock sheet, a location plate), with the film's look. */
    private void renderStill(Film film, Art art, Film.Seg s, int w, int h, File out) throws IOException {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(bmp, 4);
        try {
            Renderer r = new Renderer(film, art);
            r.renderSeg(g, s, 0.5f);
            int[] px = new int[w * h];
            bmp.getPixels(px, 0, w, 0, 0, w, h);
            new com.tarun.kahani.core.FilmLook(w, h).apply(px, com.tarun.kahani.core.FilmLook.forSeg(s, new com.tarun.kahani.core.FilmLook.Params()));
            bmp.setPixels(px, 0, w, 0, 0, w, h);
            java.io.FileOutputStream o = new java.io.FileOutputStream(out);
            bmp.compress(Bitmap.CompressFormat.JPEG, 88, o);
            o.close();
        } finally {
            g.release();
            bmp.recycle();
        }
    }

    /**
     * Pipeline steps 1 and 2 (C1): the Character Lock Sheet of every character — standing, front view, exactly as
     * the film draws them — and the Location Lock Plate of every place with no characters, saved with the film
     * (lock_char_N.jpg, lock_place_N.jpg) before any shot is made, and shown first in Human QC.
     */
    private void lockSheets(Film film, Art art, Director dir, Edits ed) throws IOException {
        lockItems.clear();
        int n = 0;
        for (Story.CharacterDef c : film.story == null ? java.util.Collections.<Story.CharacterDef>emptyList() : film.story.cast()) {
            check();
            step("Character Lock Sheet: " + c.shown() + "…", 0.292f);
            File f = project.file("lock_char_" + n++ + ".jpg");
            int[] sz = dir.lockSheetSize(c);
            renderStill(film, art, dir.lockSheet(c, sz[0] / (float) sz[1]), sz[0], sz[1], f);
            lockItems.add(new String[]{f.getAbsolutePath(), "LOCK SHEET: " + c.shown() + (c.role == null || c.role.isEmpty() ? "" : " · " + c.role), "char"});
        }
        int[] size = ed.size();
        int w = 640, h = Math.max(120, Math.round(640f * size[1] / size[0])) & ~1;
        int m = 0;
        for (Object[] p : dir.locationPlates()) {
            check();
            step("Location Lock Plate: " + p[0] + "…", 0.293f);
            File f = project.file("lock_place_" + m++ + ".jpg");
            renderStill(film, art, (Film.Seg) p[1], w, h, f);
            lockItems.add(new String[]{f.getAbsolutePath(), "LOCATION PLATE (no characters): " + p[0], "place"});
        }
        // sheets of a bigger cast or more places from an earlier film of this story
        for (int i = n; project.has("lock_char_" + i + ".jpg"); i++) project.file("lock_char_" + i + ".jpg").delete();
        for (int i = m; project.has("lock_place_" + i + ".jpg"); i++) project.file("lock_place_" + i + ".jpg").delete();
        lockSheetsMade = n; platesMade = m;
        film.shotList += String.format(java.util.Locale.US, "%nLOCK SHEETS (pipeline steps 1-2): %d Character Lock Sheets and %d Location Lock Plates made before any shot, saved with the film (lock_char_N.jpg, lock_place_N.jpg)%n", n, m);
        if (notes3d > 0) film.shotList += String.format(java.util.Locale.US, "STUDIO 3D: %d picture(s) made in three dimensions on the phone (dolls for characters without a picture, places with their floor line, the views of every character from its own picture), each accepted by you%n", notes3d);
        java.util.List<String> credits = film.story == null ? new java.util.ArrayList<String>() : Studio3DArt.credits(project, film.story);
        if (!credits.isEmpty()) { film.shotList += "FREE 3D MODELS AND SERVICES USED (with thanks; CC-BY needs this credit):\n"; for (String cr : credits) film.shotList += "  • " + cr + "\n"; }
        if (styleNote.length() > 0) film.shotList += styleNote + "\n";
        if (objectNotes != null) film.shotList += "FREE OBJECT PICTURES (inserts, each shown once when the story first brings the thing in): " + objectNotes + "\n";
    }

    /** Pipeline step 6 (and the floating check of step 8): every shot played frame by frame at check size (FinalQc). */
    private void finalCheck(final Film film, Art art, Edits ed) {
        int[] size = ed.size();
        final int w = 256, h = Math.max(96, Math.round(256f * size[1] / size[0])) & ~1;
        final Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final AndroidGfx g = new AndroidGfx(bmp, 4);
        final int[] px = new int[w * h];
        try {
            com.tarun.kahani.core.FinalQc.Result r = com.tarun.kahani.core.FinalQc.check(film, art, new com.tarun.kahani.core.FinalQc.Surface() {
                public com.tarun.kahani.core.Gfx gfx() { return g; }
                public int[] pixels() { bmp.getPixels(px, 0, w, 0, 0, w, h); return px; }
                public int width() { return w; }
                public int height() { return h; }
            }, new com.tarun.kahani.core.FinalQc.Progress() {
                public void at(int done, int total) { check(); step("Final check: playing shot " + (done + 1) + " of " + total + " frame by frame…", 0.30f); }
                public boolean cancelled() { return cancelled; }
            });
            film.shotList += "\n" + r.text();
            film.shotList += "\n" + com.tarun.kahani.core.DirectorTraining.scoreCard(film, art, null, r);     // v30: the training guide's score with what the QC saw
            if (!stillQaNotes.isEmpty()) {
                StringBuilder sq = new StringBuilder("\nSTILL-PICTURE CHECKLIST (the pictures made by the AI service this time):\n");
                for (String q : stillQaNotes) sq.append("  • ").append(q).append('\n');
                film.shotList += sq.toString();
            }
            qcBoiling = r.boilingLeft; qcShaking = r.shakingLeft; qcFloating = r.floating;
            // the handbook's approval gates and scores (ch. 13) and the delivery checklist (ch. 16), from what was checked
            if (film.stats != null) {
                com.tarun.kahani.core.Handbook.Card card = com.tarun.kahani.core.Handbook.score(film, film.stats, r.boilingLeft, r.shakingLeft, r.floating);
                film.shotList += "\n" + card.text() + "\n" + com.tarun.kahani.core.Handbook.delivery(card, film.stats);
            }
        } finally {
            g.release();
            bmp.recycle();
        }
    }

    private void humanQc(Film film, Art art, Director dir, Edits ed, File tmp, File mix) throws IOException {
        File qd = new File(tmp, "qc");
        qd.mkdirs();
        qcItems.clear();
        qcShotIndex.clear();
        qcFixes.clear();
        // 1. the character lock sheets and the location plates (pipeline steps 1-2), as made for this film
        for (String[] it : lockItems) {
            qcItems.add(it);
            qcShotIndex.add(-1);
        }
        // 2. the first frame of every shot
        int[] size = ed.size();
        int w = 320, h = Math.max(120, Math.round(320f * size[1] / size[0]));
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(bmp, 4);
        Renderer r = new Renderer(film, art);
        r.safeZoneOverlay = true;        // the format's safe zones drawn over every first frame (RULE_RESIZE_3)
        com.tarun.kahani.core.FilmLook qlook = new com.tarun.kahani.core.FilmLook(w, h);
        com.tarun.kahani.core.FilmLook.Params qlp = new com.tarun.kahani.core.FilmLook.Params();
        int[] qpx = new int[w * h];
        try {
            int n = film.shots.size();
            for (int i = 0; i < n; i++) {
                check();
                Film.Shot sh = film.shots.get(i);
                step("Making the first frame of every shot for you to check (" + (i + 1) + "/" + n + ")…", 0.296f + 0.003f * i / Math.max(1, n));
                r.render(g, sh.t + 0.05f);
                bmp.getPixels(qpx, 0, w, 0, 0, w, h);
                qlook.apply(qpx, com.tarun.kahani.core.FilmLook.at(film, sh.t + 0.05f, qlp));
                bmp.setPixels(qpx, 0, w, 0, 0, w, h);
                File f = new File(qd, "s" + i + ".jpg");
                java.io.FileOutputStream o = new java.io.FileOutputStream(f);
                bmp.compress(Bitmap.CompressFormat.JPEG, 82, o);
                o.close();
                String what = sh.speech && sh.spoken.length() > 0 ? sh.subject + ": \"" + sh.spoken + "\"" : sh.action;
                qcItems.add(new String[]{f.getAbsolutePath(), String.format(java.util.Locale.US, "SHOT %03d · %.1f s · %s\n%s",
                        i + 1, sh.dur, com.tarun.kahani.core.ShotPlanner.SIZE_NAME[Math.max(0, Math.min(com.tarun.kahani.core.ShotPlanner.SIZE_NAME.length - 1, sh.size))],
                        what.length() > 90 ? what.substring(0, 90) + "…" : what), "shot"});
                qcShotIndex.add(i);
            }
        } finally {
            g.release();
            bmp.recycle();
        }
        // 3. the animatic (the director's manual 3.7): the whole film as its first frames with the real sound, to be
        // approved before the expensive render — the first card of the check
        File an = animatic(film, qd, mix, ed);
        animaticMade = an != null;
        if (an != null && !qcShotIndex.isEmpty()) {
            int first = -1;
            for (int i = 0; i < qcShotIndex.size(); i++) if (qcShotIndex.get(i) >= 0) { first = i; break; }
            qcItems.add(0, new String[]{first >= 0 ? qcItems.get(first)[0] : "", "ANIMATIC (the director's manual 3.7): the whole film as the first frame of every shot held for its length, "
                    + "with the real voices, music and sounds — " + fmt((long) film.duration) + ". Tap to play it; approve below when the story reads, the pacing breathes and every event is covered.", "animatic"});
            qcShotIndex.add(0, -1);
        } else if (an == null) film.shotList += "\nANIMATIC: could not be made on this phone (" + animaticNote + "); the first frames stand in for it.\n";
        // 4. wait for the user (the notification says so); then their fixes
        stage = "Waiting for you: check the animatic and the first frame of every shot";
        qcWaiting = true;
        while (qcWaiting && !cancelled) {
            try { Thread.sleep(300); } catch (InterruptedException e) { break; }
        }
        check();
        int fixed = 0;
        StringBuilder note = new StringBuilder();
        for (java.util.Map.Entry<Integer, Integer> e : qcFixes.entrySet()) {
            String done = dir.fixShot(e.getKey(), e.getValue());
            if (done.length() > 0) { fixed++; note.append(String.format(java.util.Locale.US, "\n• Shot %03d: %s", e.getKey() + 1, done)); }
        }
        animaticApproved = animaticMade && !cancelled;
        fixesApplied = fixed > 0;
        if (fixed > 0) film.shotList += "\nHUMAN QC (your check of every first frame)\n" + fixed + " shot(s) fixed:" + note + "\n";
        else film.shotList += "\nHUMAN QC: every first frame approved as it was.\n";
        if (animaticMade) film.shotList += "ANIMATIC (the director's manual 3.7): made before the film (animatic.mp4, " + fmt((long) film.duration) + ") and approved by you with the first frames — Gate 3.\n";
    }

    // ------------------------------------------------------------------ the director's manual: animatic, audio, export, review

    /** What the checks found, for the manual's gates (-1 = the check did not run). */
    private int qcBoiling = -1, qcShaking = -1, qcFloating = -1, lockSheetsMade, platesMade, linesTotal, voicedTotal, missingVoices;
    private boolean animaticMade, animaticApproved, fixesApplied, audioChecked, exportExists, exportKnown, exportOk;
    private float longestGap;
    private String animaticNote = "";

    /**
     * The animatic (the director's manual 3.7): the first complete editorial version — every shot's first frame
     * held for the shot's length, the title and end cards, with the real voices, music and sounds — made before
     * the film and approved in Human QC. Small (320 px wide, 12 fps), saved with the story as animatic.mp4.
     */
    private File animatic(Film film, File qd, File mix, Edits ed) {
        File out = project.file("animatic.mp4");
        int fps = 12;
        int[] size = ed.size();
        int[] sz = VideoWriter.supportedSize(320, Math.max(120, Math.round(320f * size[1] / size[0])) & ~1);
        final int w = sz[0], h = sz[1];
        Bitmap frame = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas cv = new android.graphics.Canvas(frame);
        android.graphics.Paint paint = new android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG);
        paint.setColor(0xFFFFFFFF);
        paint.setTextSize(h / 12f);
        paint.setTextAlign(android.graphics.Paint.Align.CENTER);
        int[] px = new int[w * h];
        VideoWriter vw = new VideoWriter(w, h, fps);
        try {
            vw.start(out, new VideoWriter.FileSource(mix), Synth.SR, 0.08f);
            int frames = Math.max(1, Math.round(film.duration * fps));
            int shown = Integer.MIN_VALUE;
            for (int f = 0; f < frames; f++) {
                float t = f / (float) fps;
                int cur = Integer.MIN_VALUE;
                for (int i = 0; i < film.shots.size(); i++) { Film.Shot sh = film.shots.get(i); if (t >= sh.t && t < sh.t + sh.dur + 0.04f) { cur = i; break; } }
                Film.Seg sg = film.segAt(t);
                if (cur == Integer.MIN_VALUE) cur = -1 - (sg == null ? 0 : film.segs.indexOf(sg));
                if (cur != shown) {
                    Bitmap b = cur >= 0 ? android.graphics.BitmapFactory.decodeFile(new File(qd, "s" + cur + ".jpg").getAbsolutePath()) : null;
                    cv.drawColor(0xFF101010);
                    if (b != null) { cv.drawBitmap(b, null, new android.graphics.Rect(0, 0, w, h), null); b.recycle(); }
                    else if (sg != null) {
                        String t1 = sg.text1 == null ? "" : sg.text1, t2 = sg.text2 == null ? "" : sg.text2;
                        if (t1.length() > 0) cv.drawText(t1.length() > 40 ? t1.substring(0, 40) + "…" : t1, w / 2f, h / 2f, paint);
                        if (t2.length() > 0) cv.drawText(t2.length() > 50 ? t2.substring(0, 50) + "…" : t2, w / 2f, h / 2f + h / 9f, paint);
                    }
                    frame.getPixels(px, 0, w, 0, 0, w, h);
                    shown = cur;
                }
                vw.frame(px);
                if ((f & 31) == 0) { check(); step("Making the animatic for you to check (" + (f * 100 / frames) + "%)…", 0.3f); }
            }
            vw.finish();
            return out;
        } catch (CancelledException e) {
            vw.release();
            out.delete();
            throw e;
        } catch (Throwable e) {
            vw.release();
            out.delete();
            animaticNote = String.valueOf(e.getMessage());
            return null;
        } finally {
            frame.recycle();
        }
    }

    /** The director's manual (V, audio): lines without a voice, the longest silence inside the scenes, clipping — read from the mix. */
    private void audioCheck(Film film, File mix, File[] lineFiles) {
        int lines = film.lines.size(), voiced = 0;
        StringBuilder missing = new StringBuilder();
        for (int i = 0; i < lines; i++) {
            if (i < lineFiles.length && lineFiles[i] != null) { voiced++; continue; }
            Film.Line l = film.lines.get(i);
            if (missing.length() < 200) missing.append(missing.length() > 0 ? ", " : "").append(l.who == null ? "narrator" : l.who.shown()).append(" \"").append(l.text.length() > 30 ? l.text.substring(0, 30) + "…" : l.text).append('"');
        }
        missingVoices = lines - voiced;
        linesTotal = lines; voicedTotal = voiced;
        float longest = 0, gapAt = 0;
        int clipped = 0;
        long samples = 0;
        boolean checked = false;
        try {
            java.io.InputStream in = new java.io.BufferedInputStream(new java.io.FileInputStream(mix), 1 << 16);
            java.util.List<float[]> scenes = new java.util.ArrayList<float[]>();
            for (Film.Seg sg : film.segs) if (sg.type == Film.S_SCENE) scenes.add(new float[]{sg.t0, sg.t1});
            byte[] buf = new byte[Synth.SR / 10 * Mixer.CHANNELS * 2];     // 100 ms of the interleaved mix
            float t = 0, silentFrom = -1;
            while (true) {
                int n = 0;
                while (n < buf.length) { int r = in.read(buf, n, buf.length - n); if (r < 0) break; n += r; }
                if (n < 4) break;
                double sum = 0;
                int cnt = n / 2;
                for (int i = 0; i + 1 < n; i += 2) { int v = (short) ((buf[i] & 255) | (buf[i + 1] << 8)); sum += (double) v * v; if (v >= 32700 || v <= -32700) clipped++; }
                samples += cnt;
                double rms = Math.sqrt(sum / Math.max(1, cnt));
                boolean inScene = false;
                for (float[] sc : scenes) if (t >= sc[0] && t < sc[1]) { inScene = true; break; }
                if (inScene && rms < 60) { if (silentFrom < 0) silentFrom = t; }
                else { if (silentFrom >= 0 && t - silentFrom > longest) { longest = t - silentFrom; gapAt = silentFrom; } silentFrom = -1; }
                t += 0.1f;
                if (n < buf.length) break;
            }
            if (silentFrom >= 0 && t - silentFrom > longest) { longest = t - silentFrom; gapAt = silentFrom; }
            in.close();
            checked = true;
        } catch (Throwable e) {
            checked = false;
        }
        audioChecked = checked;
        longestGap = longest;
        film.shotList += "\n" + com.tarun.kahani.core.DirectorsManual.audioReport(lines, voiced, missing.toString(), longest, gapAt, clipped, samples, checked);
    }

    /** The director's manual (IV stage 9): the exported file decodes, its length matches the plan, audio is present. */
    private String exportCheck(File fin, Film film) {
        boolean readable = false;
        float secs = 0;
        int hasAudio = -1, w = 0, h = 0;
        android.media.MediaMetadataRetriever mr = null;
        try {
            mr = new android.media.MediaMetadataRetriever();
            mr.setDataSource(fin.getAbsolutePath());
            String d = mr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_DURATION);
            String a = mr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_HAS_AUDIO);
            String ws = mr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_WIDTH);
            String hs = mr.extractMetadata(android.media.MediaMetadataRetriever.METADATA_KEY_VIDEO_HEIGHT);
            if (d != null) { secs = Long.parseLong(d.trim()) / 1000f; readable = true; }
            if (a != null) hasAudio = "yes".equalsIgnoreCase(a.trim()) ? 1 : 0;
            if (ws != null && hs != null) { w = Integer.parseInt(ws.trim()); h = Integer.parseInt(hs.trim()); }
        } catch (Throwable e) {
            readable = false;
        } finally {
            if (mr != null) try { mr.release(); } catch (Throwable ignored) { /* nothing to free */ }
        }
        exportExists = fin.exists() && fin.length() > 0;
        exportKnown = readable;
        exportOk = com.tarun.kahani.core.DirectorsManual.exportOk(exportExists, secs, film.duration, hasAudio, readable);
        return com.tarun.kahani.core.DirectorsManual.exportReport(exportExists, fin.length(), secs, film.duration, hasAudio, w, h, readable);
    }

    /** The three-level review, the five gates and the QA checklist (the director's manual 3.10, IV, V) from what was checked. */
    private String review(Film film) {
        com.tarun.kahani.core.DirectorsManual.Checks ck = new com.tarun.kahani.core.DirectorsManual.Checks();
        ck.stats = film.stats;
        ck.boiling = qcBoiling; ck.shaking = qcShaking; ck.floating = qcFloating;
        ck.humanQc = Prefs.humanQc(ctx); ck.animaticMade = animaticMade; ck.animaticApproved = animaticApproved; ck.fixesApplied = fixesApplied;
        ck.lines = linesTotal; ck.voiced = voicedTotal; ck.missingVoices = missingVoices; ck.longestGap = longestGap; ck.audioChecked = audioChecked;
        ck.lockSheets = lockSheetsMade; ck.plates = platesMade;
        if (film.story != null) {
            ck.cast = film.story.cast().size();
            for (Story.CharacterDef c : film.story.cast()) if (Studio3DArt.charFile(project, film.story, c) != null) ck.charsWithPicture++;
        }
        ck.proposalsOpen = Studio3DArt.proposals(project).size();
        ck.exportExists = exportExists; ck.exportKnown = exportKnown; ck.exportOk = exportOk;
        return com.tarun.kahani.core.DirectorsManual.review(film, film.story, ck);
    }

    /** One picture per shot: the first frame of every shot, saved as shots/<shot id>.jpg (reverse shots, movements, everything). */
    private void shotPictures(Film film, Art art, Edits ed) throws IOException {
        File sd = project.file("shots");
        sd.mkdirs();
        File[] old = sd.listFiles();
        if (old != null) for (File f : old) f.delete();
        int[] size = ed.size();
        int w = 400, h = Math.max(120, Math.round(400f * size[1] / size[0]));
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(bmp, 4);
        Renderer r = new Renderer(film, art);
        com.tarun.kahani.core.FilmLook look = new com.tarun.kahani.core.FilmLook(w, h);
        com.tarun.kahani.core.FilmLook.Params lp = new com.tarun.kahani.core.FilmLook.Params();
        int[] px = new int[w * h];
        int n = film.shots.size();
        try {
            for (int i = 0; i < n; i++) {
                check();
                Film.Shot sh = film.shots.get(i);
                if (i % 10 == 0) step("Making one picture per shot (" + (i + 1) + "/" + n + ")…", 0.298f);
                r.render(g, sh.t + 0.05f);
                bmp.getPixels(px, 0, w, 0, 0, w, h);
                look.apply(px, com.tarun.kahani.core.FilmLook.at(film, sh.t + 0.05f, lp));
                bmp.setPixels(px, 0, w, 0, 0, w, h);
                String name = (sh.id == null || sh.id.length() == 0 ? String.format(java.util.Locale.US, "SHOT_%03d", i + 1) : sh.id) + ".jpg";
                java.io.FileOutputStream o = new java.io.FileOutputStream(new File(sd, name));
                bmp.compress(Bitmap.CompressFormat.JPEG, 82, o);
                o.close();
            }
        } finally {
            g.release();
            bmp.recycle();
        }
        film.shotList += "\nSHOT PICTURES: one picture per shot — " + n + " files in shots/, named by shot ID (the keyframe of every shot: the reverse shots, the movements, everything).\n";
    }

    /**
     * Every character, place, title and end page without a picture is created by the studio with a free AI
     * image service in a consistent 3D animated-film style, saved to the film and to the library (for reuse).
     * Without internet the studio's own cartoon drawing is used instead.
     */
    private void makeMissingPictures(Story story, Edits ed) {
        String cast = project.read("cast.txt");
        java.util.Set<String> haveChar = new java.util.HashSet<String>(), haveScene = new java.util.HashSet<String>();
        boolean haveTitle = false, haveEnd = false;
        for (String line : cast.split("\n")) {
            String[] f = line.trim().split("\\|");
            if (f.length >= 3 && f[0].equals("char")) {
                Story.CharacterDef c = ScriptParser.resolve(story, f[1]);
                if (c != null && project.has(f[2])) haveChar.add(c.id);
            } else if (f.length >= 3 && f[0].equals("scene") && project.has(f[2])) haveScene.add(f[1].replaceAll("[a-z]$", ""));
            else if (f.length >= 2 && f[0].equals("title")) haveTitle = project.has(f[1]);
            else if (f.length >= 2 && f[0].equals("end")) haveEnd = project.has(f[1]);
        }
        java.util.List<Runnable> jobs = new java.util.ArrayList<Runnable>();
        final Cloud cloud = Prefs.cloud(ctx);
        final Library lib = Library.get(ctx);
        final int seed = Math.abs(story.title.hashCode() % 100000);
        final int[] fails = {0};
        final int[] made = {0};
        final java.util.List<String[]> todo = new java.util.ArrayList<String[]>();   // {kind, key, prompt, w, h, label, desc}
        for (Story.CharacterDef c : story.cast()) {
            if (haveChar.contains(c.id)) continue;
            todo.add(new String[]{"char", c.displayName, com.tarun.kahani.core.Bible.characterPrompt(c), "768", "1152", c.shown(), c.description});
        }
        java.util.Map<String, String> placeFile = new java.util.HashMap<String, String>();
        // places, title and end are made natively in the film's shape (FINAL_AR), never cropped from another shape, at
        // the format's native size (1920x1080, 1080x1920, …) so nothing is ever upscaled blurry (RULE_RESIZE_4)
        final int[] plate = com.tarun.kahani.core.TechnicalDirector.sizeFor(ed.aspect, ed.size()[1]);
        for (Story.Scene sc : story.scenes) {
            if (haveScene.contains(String.valueOf(sc.number))) continue;
            String where = sc.setting.length() > 0 ? sc.setting : sc.title;
            todo.add(new String[]{"scene", String.valueOf(sc.number), com.tarun.kahani.core.Bible.placePrompt(
                    com.tarun.kahani.core.Bible.firstClauseOf(where), where, ed.aspect), String.valueOf(plate[0]), String.valueOf(plate[1]), com.tarun.kahani.core.Bible.firstClauseOf(where), where});
        }
        if (!haveTitle) todo.add(new String[]{"title", "title", "3D animated movie poster for a premium Indian children's film named '" + story.title
                + "', main characters together, cinematic lighting, depth of field, no text, no letters", String.valueOf(plate[0]), String.valueOf(plate[1]), "Title", story.title});
        if (!haveEnd) todo.add(new String[]{"end", "end", "Beautiful calm sunset landscape, cinematic 3D animated film style, volumetric light, "
                + "for the ending of a children's film, no text, no letters", String.valueOf(plate[0]), String.valueOf(plate[1]), "End", "ending"});
        for (int i = 0; i < todo.size(); i++) {
            check();
            if (fails[0] >= 2) { warning = "The free AI picture service could not be reached — the studio drew the missing pictures itself."; break; }
            String[] t = todo.get(i);
            step("Studio is creating pictures with AI (" + (i + 1) + "/" + todo.size() + ")…", 0.01f + 0.02f * i / Math.max(1, todo.size()));
            try {
                String reuse = t[0].equals("scene") ? findSimilarPlace(placeFile, t[5]) : null;
                String file;
                if (reuse != null) file = reuse;
                else {
                    byte[] img = makePictureNative(cloud, com.tarun.kahani.core.TechnicalDirector.clean(t[2]), Integer.parseInt(t[3]), Integer.parseInt(t[4]), seed + i);
                    file = project.savePicture(img, "ai_" + t[0]);
                    try {
                        // v34: the still-picture manual's checklist on what the service made (its hands are looked at by the user)
                        int[] d = project.loader().decode(file, 1024);
                        if (d != null) {
                            int[] qpx = new int[d[0] * d[1]];
                            System.arraycopy(d, 2, qpx, 0, qpx.length);
                            stillQaNotes.add(t[5] + ": " + com.tarun.kahani.core.StillQa.check(qpx, d[0], d[1], null, null, !t[0].equals("char"), com.tarun.kahani.core.StillQa.HANDS_LOOK).summary());
                        }
                    } catch (Throwable ignored) { /* the picture is used either way */ }
                    try { lib.addBytes(Library.PIC, t[0].equals("char") ? "person" : t[0].equals("scene") ? "place" : t[0], t[5],
                            t[6].length() > 200 ? t[6].substring(0, 200) : t[6], img, ".jpg", "AI (studio)"); } catch (Exception ignored) {}
                    if (t[0].equals("scene")) placeFile.put(t[5], file);
                    made[0]++;
                }
                if (t[0].equals("char")) project.setManifest("char", t[1], "char|" + t[1] + "|" + file);
                else if (t[0].equals("scene")) project.setManifest("scene", t[1], "scene|" + t[1] + "|" + file);
                else project.setManifest(t[0], t[0], t[0] + "|" + file + "|1");
                fails[0] = 0;
            } catch (CancelledException e) {
                throw e;
            } catch (Exception e) {
                fails[0]++;
            }
        }
    }

    static String findSimilarPlace(java.util.Map<String, String> made, String place) {
        for (java.util.Map.Entry<String, String> e : made.entrySet()) if (com.tarun.kahani.core.Bible.similar(place, e.getKey())) return e.getValue();
        return null;
    }

    static final class CancelledException extends RuntimeException {}

    private void check() { waitWhilePaused(); if (cancelled) throw new CancelledException(); }

    // ---------------------------------------------------------------- parallel frame rendering

    private void renderVideo(final Film film, final Art art, File audio, final Grade grade, final int w, final int h, int fps, float bpp, File out) throws Exception {
        final int frames = (int) Math.ceil(film.duration * fps);
        // item 13 / the phone guide §9.4: as many drawing threads as the phone's cores and free memory allow (each
        // worker holds its frame bitmap and two finished frames), never more than the cores, never a frozen phone
        int cores = Runtime.getRuntime().availableProcessors();
        long perWorker = (long) w * h * 4 * 5 + 40L * 1024 * 1024;
        long budget = memoryBudget();
        int byMem = (int) Math.max(1, Math.min(cores, budget / perWorker));
        final int workers = Math.max(1, Math.min(Math.min(cores, 8), byMem));          // v24: every core the memory allows, four frames in flight each
        film.shotList += String.format(java.util.Locale.US, "%nRENDER: %d drawing threads (%d cores, %d MB free for frames), %dx%d at %d fps%n", workers, cores, budget / (1024 * 1024), w, h, fps);
        final int[][] slots = new int[workers * 4][];
        final int[] slotFrame = new int[workers * 4];
        for (int i = 0; i < slotFrame.length; i++) slotFrame[i] = -1;
        final Object lock = new Object();
        final int[] nextToEncode = {0};
        final Throwable[] workerError = {null};
        final float fpsF = fps;
        Thread[] ts = new Thread[workers];
        for (int wi = 0; wi < workers; wi++) {
            final int id = wi;
            ts[wi] = new Thread(new Runnable() {
                public void run() {
                    Bitmap bmp = null;
                    AndroidGfx g = null;
                    try {
                        bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                        g = new AndroidGfx(bmp, 4);
                        Renderer r = new Renderer(film, art);
                        // the cinematic finish: filmic curve, bloom, colour script by mood
                        com.tarun.kahani.core.FilmLook look = new com.tarun.kahani.core.FilmLook(w, h);
                        com.tarun.kahani.core.FilmLook.Params lp = new com.tarun.kahani.core.FilmLook.Params();
                        int[] px = new int[w * h];
                        for (int f = id; f < frames; f += workers) {
                            synchronized (lock) {
                                while (f - nextToEncode[0] >= slots.length && !cancelled && workerError[0] == null) lock.wait(200);
                            }
                            waitWhilePaused();
                            if (cancelled || workerError[0] != null) return;
                            r.render(g, f / fpsF);
                            int[] buf = px;
                            bmp.getPixels(buf, 0, w, 0, 0, w, h);
                            look.apply(buf, com.tarun.kahani.core.FilmLook.at(film, f / fpsF, lp));
                            grade.apply(buf, w * h);
                            int slot = f % slots.length;
                            synchronized (lock) {
                                while (slotFrame[slot] != -1 && !cancelled && workerError[0] == null) lock.wait(200);
                                slots[slot] = buf;
                                slotFrame[slot] = f;
                                lock.notifyAll();
                            }
                            px = new int[w * h];
                            if (f % (fps * 2) == 0) preview = Bitmap.createScaledBitmap(bmp, w / 3, h / 3, true);
                        }
                    } catch (Throwable e) {
                        synchronized (lock) { workerError[0] = e; lock.notifyAll(); }
                    } finally {
                        if (g != null) g.release();
                        if (bmp != null) bmp.recycle();
                    }
                }
            }, "render-" + wi);
            ts[wi].start();
        }
        VideoWriter vw = new VideoWriter(w, h, fps);
        // pipeline step 8: the finished film metered frame by frame as it is written (FinalQc)
        final com.tarun.kahani.core.FinalQc.Meter meter = new com.tarun.kahani.core.FinalQc.Meter(film, w, h, fps);
        try {
            step("Making the video…", 0.36f);
            vw.start(out, new VideoWriter.FileSource(audio), Synth.SR, bpp);
            long t0 = System.currentTimeMillis();
            pausedMs = 0;
            for (int f = 0; f < frames; f++) {
                int[] px;
                int slot = f % slots.length;
                synchronized (lock) {
                    while (slotFrame[slot] != f) {
                        if (workerError[0] != null) throw new RuntimeException("Error while drawing: " + workerError[0]);
                        if (cancelled) throw new CancelledException();
                        lock.wait(200);
                    }
                    px = slots[slot];
                }
                vw.frame(px);
                meter.frame(f, px);
                synchronized (lock) {
                    slots[slot] = null;
                    slotFrame[slot] = -1;
                    nextToEncode[0] = f + 1;
                    lock.notifyAll();
                }
                if (f % 12 == 0) {
                    long el = System.currentTimeMillis() - t0 - pausedMs;
                    long eta = f > 30 ? el * (frames - f) / f / 1000 : -1;
                    etaSeconds = eta;
                    stage = "Making the video… " + (f * 100 / frames) + "%";
                    progress = 0.36f + 0.63f * f / frames;
                }
            }
            step("Saving the film…", 0.995f);
            vw.finish();
            film.shotList += meter.report();
            // this phone's speed, for the next film's time left
            long tookMs = System.currentTimeMillis() - t0 - pausedMs;
            if (frames > 30 && tookMs > 0) Prefs.put(ctx, "renderMsPerMpFrame", String.valueOf(tookMs / (float) frames / (w * (float) h / 1e6f)));
        } catch (Throwable e) {
            vw.release();
            synchronized (lock) { if (workerError[0] == null) workerError[0] = e; lock.notifyAll(); }
            for (Thread t : ts) t.join(3000);
            out.delete();
            if (e instanceof Exception) throw (Exception) e;
            throw new RuntimeException(e);
        }
        for (Thread t : ts) t.join(5000);
    }

    static String fmt(long s) {
        if (s >= 3600) return (s / 3600) + " h " + (s % 3600 / 60) + " min";
        if (s >= 60) return (s / 60) + " min " + (s % 60) + " s";
        return s + " s";
    }
}
