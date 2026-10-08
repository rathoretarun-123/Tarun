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
    private void updateEta() {
        long el = (System.currentTimeMillis() - startedAt) / 1000;
        if (progress > 0.05f && el > 10) etaSeconds = (long) (el * (1 - progress) / progress);
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

    public void run() {
        Voices voices = null;
        // every job has its own working folder; old leftovers (e.g. after the app was killed) are removed
        File[] stale = ctx.getCacheDir().listFiles();
        if (stale != null) for (File d : stale) if (d.getName().startsWith("film_tmp")) MainActivity.deleteDir(d);
        File tmp = new File(ctx.getCacheDir(), "film_tmp_" + startedAt);
        try {
            tmp.mkdirs();
            step("Reading the story…", 0.01f);
            String script = scriptOf(project);
            if (script.trim().length() < 10) throw new IllegalStateException("The story is empty. Write or paste a story first.");
            Story story = ScriptParser.parse(script);
            if (story.dialogueCount() == 0 && story.scenes.size() <= 1)
                throw new IllegalStateException("No dialogue found in the story. Write lines like  Name: \"dialogue\"  — or tap \"Read with AI\".");
            Edits ed = Edits.fromJson(project.read("edits.json"));
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
            String fromLib = AutoLibrary.fill(ctx, project, story);
            if (fromLib.length() > 0) info = "Taken from your library: " + fromLib;
            check();
            if (Prefs.online(ctx) && Prefs.autoArt(ctx)) makeMissingPictures(story, ed);
            step("Preparing pictures (removing backgrounds)…", 0.03f);
            Art art = Art.fromManifest(project.read("cast.txt"), story, project.loader());
            check();

            Director.Options opt = new Director.Options();
            opt.aspect = ed.aspect;
            opt.subtitles = ed.subtitles;
            opt.narrator = false;
            opt.narrateTitle = false;
            opt.pace = ed.speed;
            opt.sounds = Library.get(ctx).soundLib();   // the user's own effects play where the story mentions them
            opt.onTwos = Prefs.onTwos(ctx);              // Spider-Verse stepping, only when the user asks for it
            Director dir = new Director(story, opt);
            Film film = dir.prepare();

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
            check();
            // Human QC (protocol pipeline step 4): the first frame of every shot, checked by the user before the film
            // is made; their fixes are applied, then the film is made
            if (Prefs.humanQc(ctx)) humanQc(film, art, dir, ed, tmp);
            step("Mixing music and sounds…", 0.30f);
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

            // ---------------- video
            int[] want = ed.size();
            int fps = 24;
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
            if (fin.exists()) fin.delete();
            if (!out.renameTo(fin)) throw new IllegalStateException("The film could not be saved");
            project.setSetting("filmSeconds", String.valueOf((int) film.duration));
            project.setSetting("madeAt", String.valueOf(System.currentTimeMillis()));
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

    private void humanQc(Film film, Art art, Director dir, Edits ed, File tmp) throws IOException {
        File qd = new File(tmp, "qc");
        qd.mkdirs();
        qcItems.clear();
        qcShotIndex.clear();
        qcFixes.clear();
        // 1. the character lock sheets: every character's picture as the film will use it
        for (Story.CharacterDef c : film.story == null ? java.util.Collections.<Story.CharacterDef>emptyList() : film.story.characters) {
            Art.Sprite sp = art.sprites.get(c.id);
            if (sp == null || !(sp.img instanceof Bitmap)) continue;
            Bitmap src = (Bitmap) sp.img;
            int th = 240, tw = Math.max(40, Math.round(th * src.getWidth() / (float) src.getHeight()));
            Bitmap b = Bitmap.createBitmap(tw, th, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas cv = new android.graphics.Canvas(b);
            cv.drawColor(0xFFE8EEF4);
            cv.drawBitmap(src, null, new android.graphics.Rect(0, 0, tw, th), new android.graphics.Paint(android.graphics.Paint.FILTER_BITMAP_FLAG));
            File f = new File(qd, "c" + qcItems.size() + ".jpg");
            java.io.FileOutputStream o = new java.io.FileOutputStream(f);
            b.compress(Bitmap.CompressFormat.JPEG, 85, o);
            o.close();
            b.recycle();
            qcItems.add(new String[]{f.getAbsolutePath(), "LOCK SHEET: " + c.shown(), "char"});
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
        // 3. wait for the user (the notification says so); then their fixes
        stage = "Waiting for you: check the first frame of every shot";
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
        if (fixed > 0) film.shotList += "\nHUMAN QC (your check of every first frame)\n" + fixed + " shot(s) fixed:" + note + "\n";
        else film.shotList += "\nHUMAN QC: every first frame approved as it was.\n";
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
        final int workers = Math.max(1, Math.min(4, Runtime.getRuntime().availableProcessors() - 1));
        final int[][] slots = new int[workers * 2][];
        final int[] slotFrame = new int[workers * 2];
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
