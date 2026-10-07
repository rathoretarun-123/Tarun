package com.tarun.kahani.app;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.speech.tts.Voice;

import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.Mixer;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.Synth;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/** Character voices using the phone's text-to-speech engine (Hindi or English). */
public final class Voices {
    private TextToSpeech tts;
    private volatile boolean ready;
    public boolean languageOk;
    public final List<Voice> voices = new ArrayList<Voice>();
    private final Map<String, CountDownLatch> waits = new ConcurrentHashMap<String, CountDownLatch>();
    private final Map<String, Boolean> ok = new ConcurrentHashMap<String, Boolean>();
    private Locale locale;

    /** Blocks (max ~8 s) until the speech engine is ready. Call from a background thread. */
    public boolean init(Context ctx, boolean hindi) {
        final CountDownLatch latch = new CountDownLatch(1);
        final boolean[] success = {false};
        try {
            tts = new TextToSpeech(ctx.getApplicationContext(), new TextToSpeech.OnInitListener() {
                public void onInit(int status) {
                    success[0] = status == TextToSpeech.SUCCESS;
                    latch.countDown();
                }
            });
            latch.await(8, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
        if (!success[0]) return false;
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            public void onStart(String id) {}
            public void onDone(String id) { finish(id, true); }
            public void onError(String id) { finish(id, false); }
            public void onError(String id, int code) { finish(id, false); }
        });
        locale = hindi ? new Locale("hi", "IN") : new Locale("en", "IN");
        int r = tts.setLanguage(locale);
        languageOk = r != TextToSpeech.LANG_MISSING_DATA && r != TextToSpeech.LANG_NOT_SUPPORTED;
        if (!languageOk && !hindi) { locale = Locale.US; r = tts.setLanguage(locale); languageOk = r >= 0; }
        try {
            Set<Voice> all = tts.getVoices();
            if (all != null) {
                List<Voice> local = new ArrayList<Voice>(), net = new ArrayList<Voice>();
                for (Voice v : all) {
                    if (v.getLocale() == null || !v.getLocale().getLanguage().equals(locale.getLanguage())) continue;
                    Set<String> feats = v.getFeatures();
                    if (feats != null && feats.contains("notInstalled")) continue;
                    if (v.isNetworkConnectionRequired()) net.add(v); else local.add(v);
                }
                Comparator<Voice> byName = new Comparator<Voice>() {
                    public int compare(Voice a, Voice b) { return a.getName().compareTo(b.getName()); }
                };
                Collections.sort(local, byName);
                Collections.sort(net, byName);
                voices.addAll(local);
                if (voices.isEmpty()) voices.addAll(net);
            }
        } catch (Exception ignored) {
        }
        ready = true;
        return true;
    }

    private void finish(String id, boolean good) {
        ok.put(id, good);
        CountDownLatch l = waits.get(id);
        if (l != null) l.countDown();
    }

    public void shutdown() {
        try { if (tts != null) { tts.stop(); tts.shutdown(); } } catch (Exception ignored) {}
        tts = null;
        ready = false;
    }

    // -------------------------------------------------------------- casting voices

    public static final class Cast {
        public int voice = -1;     // index into voices, -1 = engine default
        public float pitch = 1f, rate = 1f;
        public float shift = 1f;   // extra pitch shift by resampling (monster)
    }

    /** Picks a voice, pitch and speed for every character (overridable per project). */
    public Map<Story.CharacterDef, Cast> castAll(Story story, Project p) {
        Map<Story.CharacterDef, Cast> m = new HashMap<Story.CharacterDef, Cast>();
        int n = Math.max(1, voices.size());
        int fi = 0, mi = 1;
        for (Story.CharacterDef c : story.characters) {
            Cast k = defaultCast(c.look);
            if (!voices.isEmpty()) {
                // alternate the installed voices so characters sound different
                if (c.look.female || c.look.kind == Look.GIRL) { k.voice = fi % n; fi += 2; }
                else { k.voice = mi % n; mi += 2; }
            }
            if (p != null) {
                String v = p.setting("voice." + c.displayName, "");
                try { if (v.length() > 0) k.voice = Integer.parseInt(v); } catch (NumberFormatException ignored) {}
                String pi = p.setting("pitch." + c.displayName, "");
                try { if (pi.length() > 0) k.pitch = Float.parseFloat(pi); } catch (NumberFormatException ignored) {}
            }
            if (k.voice >= voices.size()) k.voice = voices.isEmpty() ? -1 : k.voice % voices.size();
            m.put(c, k);
        }
        return m;
    }

    public static Cast defaultCast(Look l) {
        Cast k = new Cast();
        switch (l.kind) {
            case Look.GIRL: k.pitch = l.height < 0.68f ? 1.55f : 1.32f; k.rate = 1.0f; break;
            case Look.BOY: k.pitch = l.height < 0.68f ? 1.45f : 1.22f; break;
            case Look.WOMAN: k.pitch = 1.1f; k.rate = 0.95f; break;
            case Look.WITCH: k.pitch = 1.25f; k.rate = 0.88f; break;
            case Look.MONSTER: k.pitch = 0.6f; k.rate = 0.85f; k.shift = 0.86f; break;
            case Look.MONKEY: k.pitch = 1.9f; k.rate = 1.2f; break;
            case Look.OLD_MAN: k.pitch = 0.72f; k.rate = 0.85f; break;
            case Look.ANIMAL: case Look.BIRD:
                k.pitch = l.height < 0.3f ? 1.6f : l.height < 0.5f ? 1.25f : l.height < 0.8f ? 0.9f : 0.7f;
                k.rate = l.height < 0.3f ? 1.1f : 0.95f;
                break;
            default: k.pitch = l.girth > 1.1f ? 0.88f : 0.8f; k.rate = 0.97f;
        }
        return k;
    }

    // -------------------------------------------------------------- synthesis

    /** Speaks one line into PCM at Synth.SR, or returns null if the engine fails. */
    public float[] synth(Film.Line line, Cast cast, File tmpDir, int idx) {
        if (!ready || tts == null || line.text.trim().length() == 0) return null;
        Cast k = cast != null ? cast : new Cast();
        float pitch = k.pitch, rate = k.rate;
        switch (line.emotion) {
            case Pose.ANGRY: rate *= 1.05f; pitch *= 0.95f; break;
            case Pose.SAD: rate *= 0.9f; pitch *= 0.97f; break;
            case Pose.SCARED: rate *= 1.08f; pitch *= 1.04f; break;
            case Pose.LAUGH: case Pose.HAPPY: pitch *= 1.04f; break;
            case Pose.WHISPER: rate *= 0.9f; break;
            case Pose.PAIN: rate *= 1.1f; pitch *= 1.08f; break;
            case Pose.EVIL: rate *= 0.92f; break;
            default:
        }
        try {
            if (k.voice >= 0 && k.voice < voices.size()) tts.setVoice(voices.get(k.voice));
            else tts.setLanguage(locale);
            tts.setPitch(pitch);
            tts.setSpeechRate(rate);
            File out = new File(tmpDir, "line" + idx + ".wav");
            if (out.exists()) out.delete();
            String id = "u" + idx + "_" + System.nanoTime();
            CountDownLatch latch = new CountDownLatch(1);
            waits.put(id, latch);
            Bundle params = new Bundle();
            int r = tts.synthesizeToFile(line.text, params, out, id);
            if (r != TextToSpeech.SUCCESS) { waits.remove(id); return null; }
            long timeout = 20000 + line.text.length() * 300L;
            boolean done = latch.await(timeout, TimeUnit.MILLISECONDS);
            waits.remove(id);
            Boolean good = ok.remove(id);
            if (!done || good == null || !good || !out.exists() || out.length() < 100) return null;
            float[] pcm = readWav(out);
            out.delete();
            if (pcm == null || pcm.length < Synth.SR / 10) return null;
            if (k.shift != 1f) pcm = shift(pcm, k.shift);
            return trim(pcm);
        } catch (Exception e) {
            return null;
        }
    }

    /** Lower (factor < 1) or raise the voice by resampling. */
    static float[] shift(float[] in, float f) {
        int n = (int) (in.length / f);
        float[] o = new float[n];
        for (int i = 0; i < n; i++) {
            float p = i * f;
            int a = (int) p;
            float t = p - a;
            float x0 = in[Math.min(a, in.length - 1)], x1 = in[Math.min(a + 1, in.length - 1)];
            o[i] = x0 + (x1 - x0) * t;
        }
        return o;
    }

    /** Removes long silences at both ends so dialogue flows. */
    static float[] trim(float[] p) {
        int a = 0, b = p.length - 1;
        float th = 0.008f;
        while (a < b && Math.abs(p[a]) < th) a++;
        while (b > a && Math.abs(p[b]) < th) b--;
        a = Math.max(0, a - Synth.SR / 50);
        b = Math.min(p.length - 1, b + Synth.SR / 12);
        if (b - a < Synth.SR / 10) return p;
        float[] o = new float[b - a + 1];
        System.arraycopy(p, a, o, 0, o.length);
        return o;
    }

    /** Preview a character voice on the speaker. */
    public void preview(String text, Cast k) {
        if (!ready || tts == null) return;
        try {
            if (k.voice >= 0 && k.voice < voices.size()) tts.setVoice(voices.get(k.voice));
            tts.setPitch(k.pitch);
            tts.setSpeechRate(k.rate);
            tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "preview");
        } catch (Exception ignored) {
        }
    }

    /** Reads a PCM WAV (8/16-bit, mono/stereo) and converts to mono floats at Synth.SR. */
    static float[] readWav(File f) throws IOException {
        byte[] b = Project.readAll(new FileInputStream(f));
        if (b.length < 44 || b[0] != 'R' || b[8] != 'W') return null;
        int pos = 12, channels = 1, sr = 22050, bits = 16, dataPos = -1, dataLen = 0;
        while (pos + 8 <= b.length) {
            String id = new String(b, pos, 4, "US-ASCII");
            int len = (b[pos + 4] & 255) | (b[pos + 5] & 255) << 8 | (b[pos + 6] & 255) << 16 | (b[pos + 7] & 255) << 24;
            if (id.equals("fmt ")) {
                channels = (b[pos + 10] & 255) | (b[pos + 11] & 255) << 8;
                sr = (b[pos + 12] & 255) | (b[pos + 13] & 255) << 8 | (b[pos + 14] & 255) << 16 | (b[pos + 15] & 255) << 24;
                bits = (b[pos + 22] & 255) | (b[pos + 23] & 255) << 8;
            } else if (id.equals("data")) {
                dataPos = pos + 8;
                dataLen = (len <= 0 || dataPos + len > b.length) ? b.length - dataPos : len;
                break;
            }
            if (len < 0 || len > b.length) break;
            pos += 8 + len + (len & 1);
        }
        if (dataPos < 0 || channels < 1 || sr < 4000) return null;
        int bytes = bits / 8;
        int frames = dataLen / (bytes * channels);
        float[] o = new float[frames];
        for (int i = 0; i < frames; i++) {
            float s = 0;
            for (int ch = 0; ch < channels; ch++) {
                int at = dataPos + (i * channels + ch) * bytes;
                if (bits == 16) s += (short) ((b[at] & 255) | (b[at + 1] << 8)) / 32768f;
                else if (bits == 8) s += ((b[at] & 255) - 128) / 128f;
            }
            o[i] = s / channels;
        }
        return Mixer.resample(o, sr);
    }
}
