package com.tarun.kahani.app;

import android.content.Context;
import android.graphics.Bitmap;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Cloud;
import com.tarun.kahani.core.Edits;
import com.tarun.kahani.core.Grade;
import com.tarun.kahani.core.VoiceFx;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Mixer;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.Synth;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * Makes the film in the background: read script -> director -> voices -> sound mix -> frames -> MP4.
 * Lives outside the Activity so turning the phone does not stop it.
 */
public final class FilmJob implements Runnable {
    public static volatile FilmJob current;

    public final Project project;
    private final Context ctx;
    public volatile String stage = "तैयारी…";
    public volatile float progress;          // 0..1 overall
    public volatile boolean done, failed, cancelled;
    public volatile String error = "";
    public volatile String warning = "";
    public volatile Bitmap preview;         // latest rendered frame (small)
    public volatile long startedAt = System.currentTimeMillis();
    public volatile float filmSeconds;
    public volatile int voicedLines;        // lines that got a real voice (recording, sample, AI or phone)

    public FilmJob(Context ctx, Project p) {
        this.ctx = ctx.getApplicationContext();
        this.project = p;
    }

    public static synchronized FilmJob start(Context ctx, Project p) {
        if (current != null && !current.done && !current.failed && !current.cancelled) return current;
        current = new FilmJob(ctx, p);
        FilmService.start(ctx);
        Thread t = new Thread(current, "film-job");
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();
        return current;
    }

    public void cancel() { cancelled = true; }

    private void step(String s, float p) { stage = s; progress = p; }

    /** "about 3 min 20 s left" while making the video. */
    public String eta() {
        if (etaSeconds < 0) return "";
        return "लगभग " + fmt(etaSeconds) + " बाकी";
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
        File tmp = new File(ctx.getCacheDir(), "film_tmp");
        try {
            tmp.mkdirs();
            File[] old = tmp.listFiles();
            if (old != null) for (File f : old) f.delete();
            step("कहानी पढ़ी जा रही है…", 0.01f);
            String script = scriptOf(project);
            if (script.trim().length() < 10) throw new IllegalStateException("कहानी खाली है। पहले कहानी लिखें या चिपकाएँ।");
            Story story = ScriptParser.parse(script);
            if (story.dialogueCount() == 0 && story.scenes.size() <= 1)
                throw new IllegalStateException("कहानी में कोई संवाद नहीं मिला। संवाद ऐसे लिखें:  नाम: \"संवाद\"  — या \"AI से पढ़वाएँ\" दबाएँ।");
            Edits ed = Edits.fromJson(project.read("edits.json"));

            step("चित्र तैयार हो रहे हैं (पृष्ठभूमि हटाना)…", 0.03f);
            Art art = Art.fromManifest(project.read("cast.txt"), story, project.loader());
            check();

            Director.Options opt = new Director.Options();
            opt.subtitles = ed.subtitles;
            opt.narrator = false;
            opt.narrateTitle = false;
            opt.pace = ed.speed;
            Director dir = new Director(story, opt);
            Film film = dir.prepare();

            // ---------------- voices
            step("आवाज़ें तैयार हो रही हैं…", 0.06f);
            voices = new Voices();
            boolean ttsOk = voices.init(ctx, story.hindi);
            Cloud cloud = Prefs.online(ctx) && Prefs.aiVoices(ctx) ? Prefs.cloud(ctx) : null;
            boolean aiVoices = cloud != null && cloud.hasGemini();
            if (!ttsOk && !aiVoices) warning = "फ़ोन में बोलने वाला इंजन (Text-to-Speech) नहीं मिला — सेटिंग्स > Text-to-speech देखें।";
            else if (ttsOk && !voices.languageOk && !aiVoices) warning = (story.hindi ? "हिंदी" : "English") + " आवाज़ फ़ोन में डाउनलोड नहीं है — सेटिंग्स > Text-to-speech में जाकर डाउनलोड करें।";
            Map<Story.CharacterDef, Voices.Cast> cast = voices.castAll(story, project);
            Library lib = new Library(ctx);
            int ci = 0;
            for (Story.CharacterDef c : story.characters) {
                Voices.Cast k = cast.get(c);
                if (k == null) { k = Voices.defaultCast(c.look); cast.put(c, k); }
                String sid = project.setting("vsample." + c.displayName, "");
                Library.Item it = lib.byId(sid);
                if (it != null) {
                    k.sample = AudioIO.decode(ctx, it.path);
                    if (k.sample != null) k.sampleId = it.id;
                }
                if (aiVoices) {
                    String gv = project.setting("gvoice." + c.displayName, "");
                    k.gemini = gv.length() > 0 ? gv : Cloud.geminiVoiceFor(c.look, ci);
                }
                ci++;
            }
            Voices.Cast narratorCast = new Voices.Cast();
            narratorCast.pitch = 1.0f; narratorCast.rate = 0.92f;
            if (aiVoices) narratorCast.gemini = "Charon";
            Library.Item ns = lib.byId(project.setting("vsample.narrator", ""));
            if (ns != null) { narratorCast.sample = AudioIO.decode(ctx, ns.path); narratorCast.sampleId = ns.id; }

            File vdir = new File(project.dir, "voices");
            vdir.mkdirs();
            final File[] lineFiles = new File[film.lines.size()];
            int failedLines = 0, aiLines = 0;
            String[] err = new String[1];
            for (int i = 0; i < film.lines.size(); i++) {
                check();
                Film.Line l = film.lines.get(i);
                String who = l.who == null ? "कथावाचक" : l.who.displayName;
                step("आवाज़: " + who + " (" + (i + 1) + "/" + film.lines.size() + ")", 0.06f + 0.22f * i / Math.max(1, film.lines.size()));
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
                if (v == null && (ttsOk || aiVoices)) {
                    v = voices.speak(l, k, tmp, i, aiVoices ? cloud : null, err);
                    if (v != null) {
                        if (voices.lastEngine.startsWith("AI")) aiLines++;
                        try { AudioIO.writeRaw(cached, v); } catch (IOException ignored) {}
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
                } else {
                    failedLines++;
                    l.dur = Director.estimate(l.text);
                    l.env = null;
                }
            }
            if (aiVoices && voices.aiOff && warning.length() == 0)
                warning = "AI आवाज़ की आज की सीमा पूरी हो गई या कुंजी गलत है — बाकी संवाद फ़ोन की आवाज़ में बने। " + (err[0] == null ? "" : err[0]);
            if (failedLines > 0 && warning.length() == 0)
                warning = failedLines + " संवाद बोले नहीं जा सके — फ़ोन की Text-to-Speech सेटिंग देखें।";
            voices.shutdown();
            voices = null;

            // ---------------- direction & sound
            step("निर्देशक दृश्य सजा रहा है…", 0.29f);
            film = dir.direct(art);
            // background sounds the user picked for parts of the story
            for (Film.Amb a : film.ambience) {
                Film.Seg sg = film.segAt(a.t0 + 0.05f);
                if (sg == null || sg.scene < 0 || sg.scene >= story.scenes.size()) continue;
                Library.Item it = lib.byId(project.setting("amb." + story.scenes.get(sg.scene).number, ""));
                if (it != null) a.words = "#" + it.path;
            }
            if (film.duration > 30 * 60 + 30) warning = "फ़िल्म 30 मिनट से लंबी है (" + fmt((long) film.duration) + ") — बनने में ज़्यादा समय लगेगा।";
            film.subtitles = ed.subtitles;
            filmSeconds = film.duration;
            check();
            step("संगीत और ध्वनि मिलाई जा रही है…", 0.30f);
            final File mix = new File(tmp, "mix.pcm");
            final java.io.OutputStream mo = new java.io.BufferedOutputStream(new java.io.FileOutputStream(mix), 1 << 16);
            final byte[] mb = new byte[Synth.SR * 8 * 2];
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
                    warning = "वीडियो " + size[0] + "x" + size[1] + " में नहीं बन सका, छोटे आकार में कोशिश हो रही है…";
                }
            }
            if (!ok) throw last != null ? last : new IllegalStateException("वीडियो नहीं बन सका");
            check();
            File fin = project.film();
            if (fin.exists()) fin.delete();
            if (!out.renameTo(fin)) throw new IllegalStateException("फ़िल्म सहेजी नहीं जा सकी");
            project.setSetting("filmSeconds", String.valueOf((int) film.duration));
            project.setSetting("madeAt", String.valueOf(System.currentTimeMillis()));
            project.setSetting("saved", "0");
            if (aiLines > 0) project.setSetting("aiLines", String.valueOf(aiLines));
            step("फ़िल्म तैयार है!", 1f);
            etaSeconds = -1;
            done = true;
        } catch (CancelledException e) {
            stage = "रोक दिया गया";
        } catch (OutOfMemoryError e) {
            error = "फ़ोन की मेमोरी कम पड़ गई। \"फ़ाइल का आकार छोटा करो\" या 720p चुनकर दोबारा कोशिश करें।";
            failed = true;
        } catch (Throwable e) {
            error = e.getMessage() != null && e.getMessage().length() > 0 ? e.getMessage() : ("त्रुटि: " + e.getClass().getSimpleName());
            failed = true;
        } finally {
            if (voices != null) voices.shutdown();
            File[] fs = tmp.listFiles();
            if (fs != null) for (File f : fs) f.delete();
        }
    }

    static final class CancelledException extends RuntimeException {}

    private void check() { if (cancelled) throw new CancelledException(); }

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
                        int[] px = new int[w * h];
                        for (int f = id; f < frames; f += workers) {
                            synchronized (lock) {
                                while (f - nextToEncode[0] >= slots.length && !cancelled && workerError[0] == null) lock.wait(200);
                            }
                            if (cancelled || workerError[0] != null) return;
                            r.render(g, f / fpsF);
                            int[] buf = px;
                            bmp.getPixels(buf, 0, w, 0, 0, w, h);
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
            step("वीडियो बन रहा है…", 0.36f);
            vw.start(out, new VideoWriter.FileSource(audio), Synth.SR, bpp);
            long t0 = System.currentTimeMillis();
            for (int f = 0; f < frames; f++) {
                int[] px;
                int slot = f % slots.length;
                synchronized (lock) {
                    while (slotFrame[slot] != f) {
                        if (workerError[0] != null) throw new RuntimeException("चित्र बनाने में त्रुटि: " + workerError[0]);
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
                    long el = System.currentTimeMillis() - t0;
                    long eta = f > 30 ? el * (frames - f) / f / 1000 : -1;
                    etaSeconds = eta;
                    stage = "वीडियो बन रहा है… " + (f * 100 / frames) + "%";
                    progress = 0.36f + 0.63f * f / frames;
                }
            }
            step("फ़िल्म सहेजी जा रही है…", 0.995f);
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
        if (s >= 3600) return (s / 3600) + " घंटा " + (s % 3600 / 60) + " मिनट";
        if (s >= 60) return (s / 60) + " मिनट " + (s % 60) + " सेकंड";
        return s + " सेकंड";
    }
}
