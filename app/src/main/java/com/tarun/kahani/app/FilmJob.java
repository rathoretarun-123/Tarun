package com.tarun.kahani.app;

import android.content.Context;
import android.graphics.Bitmap;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Mixer;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.Synth;

import java.io.File;
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

    public FilmJob(Context ctx, Project p) {
        this.ctx = ctx.getApplicationContext();
        this.project = p;
    }

    public static synchronized FilmJob start(Context ctx, Project p) {
        if (current != null && !current.done && !current.failed && !current.cancelled) return current;
        current = new FilmJob(ctx, p);
        Thread t = new Thread(current, "film-job");
        t.setPriority(Thread.NORM_PRIORITY);
        t.start();
        return current;
    }

    public void cancel() { cancelled = true; }

    private void step(String s, float p) { stage = s; progress = p; }

    public void run() {
        Voices voices = null;
        File tmp = new File(ctx.getCacheDir(), "film_tmp");
        try {
            tmp.mkdirs();
            step("कहानी पढ़ी जा रही है…", 0.01f);
            String script = project.read("script.txt");
            if (script.trim().length() < 10) throw new IllegalStateException("कहानी खाली है। पहले कहानी लिखें या चिपकाएँ।");
            Story story = ScriptParser.parse(script);
            if (story.dialogueCount() == 0 && story.scenes.size() <= 1)
                throw new IllegalStateException("कहानी में कोई संवाद नहीं मिला। संवाद ऐसे लिखें:  नाम: \"संवाद\"");

            step("चित्र तैयार हो रहे हैं (पृष्ठभूमि हटाना)…", 0.03f);
            Art art = Art.fromManifest(project.read("cast.txt"), story, project.loader());
            check();

            Director.Options opt = new Director.Options();
            opt.subtitles = !"0".equals(project.setting("subtitles", "1"));
            opt.narrator = "1".equals(project.setting("narrator", "0"));
            opt.narrateTitle = !"0".equals(project.setting("narrateTitle", "1"));
            Director dir = new Director(story, opt);
            Film film = dir.prepare();

            // ---------------- voices
            step("आवाज़ें तैयार हो रही हैं…", 0.06f);
            voices = new Voices();
            boolean ttsOk = voices.init(ctx, story.hindi);
            if (!ttsOk) warning = "फ़ोन में बोलने वाला इंजन (Text-to-Speech) नहीं मिला — फ़िल्म बिना आवाज़ के, उपशीर्षक के साथ बनेगी।";
            else if (!voices.languageOk) warning = (story.hindi ? "हिंदी" : "English") + " आवाज़ फ़ोन में डाउनलोड नहीं है — सेटिंग्स > Text-to-speech में जाकर डाउनलोड करें।";
            Map<Story.CharacterDef, Voices.Cast> cast = ttsOk ? voices.castAll(story, project) : new HashMap<Story.CharacterDef, Voices.Cast>();
            Voices.Cast narratorCast = new Voices.Cast();
            narratorCast.pitch = 1.0f; narratorCast.rate = 0.92f;
            float[][] pcm = new float[film.lines.size()][];
            int failedLines = 0;
            for (int i = 0; i < film.lines.size(); i++) {
                check();
                Film.Line l = film.lines.get(i);
                step("आवाज़: " + (l.who == null ? "कथावाचक" : l.who.displayName) + " (" + (i + 1) + "/" + film.lines.size() + ")",
                        0.06f + 0.22f * i / Math.max(1, film.lines.size()));
                float[] v = ttsOk ? voices.synth(l, l.who == null ? narratorCast : cast.get(l.who), tmp, i) : null;
                if (v != null) {
                    pcm[i] = v;
                    l.dur = v.length / (float) Synth.SR;
                    l.env = Mixer.envelope(v, Synth.SR);
                } else {
                    failedLines++;
                    l.dur = Director.estimate(l.text);
                    l.env = null;
                }
            }
            if (ttsOk && failedLines > 0 && warning.length() == 0)
                warning = failedLines + " संवाद बोले नहीं जा सके — वे उपशीर्षक के साथ दिखेंगे।";
            voices.shutdown();
            voices = null;

            // ---------------- direction & sound
            step("निर्देशक दृश्य सजा रहा है…", 0.29f);
            film = dir.direct(art);
            filmSeconds = film.duration;
            check();
            step("संगीत और ध्वनि मिलाई जा रही है…", 0.30f);
            short[] audio = Mixer.mix(film, pcm, new Mixer.Progress() {
                public void update(float f) { progress = 0.30f + 0.05f * f; }
            });
            pcm = null;
            check();

            // ---------------- video
            int wantH = 720;
            try { wantH = Integer.parseInt(project.setting("quality", "720")); } catch (NumberFormatException ignored) {}
            int fps = 24;
            File out = new File(project.dir, "film_new.mp4");
            // if this phone's encoder refuses a size, try the next smaller one automatically
            int[] tries = {wantH, 720, 480};
            Exception last = null;
            boolean ok = false;
            for (int i = 0; i < tries.length && !ok; i++) {
                if (i > 0 && tries[i] >= tries[i - 1]) continue;
                int[] size = VideoWriter.supportedSize(tries[i]);
                try {
                    renderVideo(film, art, audio, size[0], size[1], fps, out);
                    ok = true;
                } catch (CancelledException e) {
                    throw e;
                } catch (Exception e) {
                    if (cancelled) throw new CancelledException();
                    last = e;
                    warning = "वीडियो " + size[1] + "p में नहीं बन सका, छोटे आकार में कोशिश हो रही है…";
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
            step("फ़िल्म तैयार है!", 1f);
            done = true;
        } catch (CancelledException e) {
            stage = "रोक दिया गया";
        } catch (OutOfMemoryError e) {
            error = "फ़ोन की मेमोरी कम पड़ गई। गुणवत्ता 480p करके दोबारा कोशिश करें।";
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

    private void renderVideo(final Film film, final Art art, short[] audio, final int w, final int h, int fps, File out) throws Exception {
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
            vw.start(out, audio, Synth.SR);
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
                    stage = "वीडियो बन रहा है… " + (f * 100 / frames) + "%" + (eta >= 0 ? "  (लगभग " + fmt(eta) + " बाकी)" : "");
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
        if (s >= 60) return (s / 60) + " मिनट " + (s % 60) + " सेकंड";
        return s + " सेकंड";
    }
}
