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
    private boolean storyHindi = true;

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
        if (!success[0]) {
            try { if (tts != null) tts.shutdown(); } catch (Exception ignored) {}
            tts = null;
            return false;
        }
        tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
            public void onStart(String id) {}
            public void onDone(String id) { finish(id, true); }
            public void onError(String id) { finish(id, false); }
            public void onError(String id, int code) { finish(id, false); }
        });
        storyHindi = hindi;
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
        public com.tarun.kahani.core.VoiceFx.Profile sample; // the user's voice sample for this character, or null
        public String sampleId = "";
        public String gemini;      // Gemini voice name when AI voices are used
        public com.tarun.kahani.core.EdgeVoice.Cast edge;   // natural neural voice (online), or null
        /** Raspy, trembling, booming… as the script describes the voice (applied as sound processing). */
        public com.tarun.kahani.core.VoiceStyle style;
        public String signature() {
            return voice + "|" + pitch + "|" + rate + "|" + shift + "|" + sampleId + "|" + (gemini == null ? "" : gemini)
                    + "|" + (edge == null ? "" : edge.key()) + "|" + (style == null ? "" : style.signature());
        }
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
            // the voice the script describes: deeper / higher / slower / faster, and raspy, trembling, booming…
            com.tarun.kahani.core.VoiceMatch.Want w = com.tarun.kahani.core.VoiceMatch.want(c);
            if (w.deep && !w.high) k.pitch *= 0.93f;
            if (w.high && !w.deep) k.pitch *= 1.07f;
            if (w.slow && !w.fast) k.rate *= 0.92f;
            if (w.fast && !w.slow) k.rate *= 1.08f;
            k.style = com.tarun.kahani.core.VoiceStyle.forCharacter(c);
            m.put(c, k);
        }
        return m;
    }

    public static Cast defaultCast(Look l) {
        Cast k = new Cast();
        // kept close to the engine's natural pitch: large jumps make phone voices sound robotic
        switch (l.kind) {
            case Look.GIRL: k.pitch = l.height < 0.68f ? 1.22f : 1.12f; k.rate = 1.0f; break;
            case Look.BOY: k.pitch = l.height < 0.68f ? 1.18f : 1.08f; break;
            case Look.WOMAN: k.pitch = 1.03f; k.rate = 0.96f; break;
            case Look.WITCH: k.pitch = 0.95f; k.rate = 0.9f; break;
            case Look.MONSTER: k.pitch = 0.8f; k.rate = 0.88f; k.shift = 0.9f; break;
            case Look.MONKEY: k.pitch = 1.3f; k.rate = 1.1f; break;
            case Look.OLD_MAN: k.pitch = 0.88f; k.rate = 0.88f; break;
            case Look.ANIMAL: case Look.BIRD:
                k.pitch = l.height < 0.3f ? 1.3f : l.height < 0.5f ? 1.15f : l.height < 0.8f ? 0.95f : 0.85f;
                k.rate = l.height < 0.3f ? 1.08f : 0.96f;
                break;
            default: k.pitch = l.girth > 1.1f ? 0.92f : 0.97f; k.rate = 0.98f;
        }
        return k;
    }

    // -------------------------------------------------------------- synthesis

    /** How an emotion changes speed and pitch (applied after matching the user's sample, so it is never lost). */
    static float[] emotionFactors(int emotion) {
        switch (emotion) {
            case Pose.ANGRY: return new float[]{1.06f, 0.94f};
            case Pose.SAD: return new float[]{0.88f, 0.96f};
            case Pose.SCARED: return new float[]{1.1f, 1.06f};
            case Pose.LAUGH: return new float[]{1.04f, 1.07f};
            case Pose.HAPPY: return new float[]{1.02f, 1.04f};
            case Pose.SURPRISED: return new float[]{1.03f, 1.08f};
            case Pose.WHISPER: return new float[]{0.9f, 1f};
            case Pose.PAIN: return new float[]{1.1f, 1.08f};
            case Pose.EVIL: return new float[]{0.92f, 0.95f};
            case Pose.PROUD: return new float[]{0.96f, 0.98f};
            default: return new float[]{1f, 1f};
        }
    }

    /** Acting direction for the AI voice, e.g. "Say in a scared, trembling whisper (डरते हुए)". */
    static String direction(Film.Line line, Story.CharacterDef who) {
        String e = com.tarun.kahani.core.Bible.emotionWord(line.emotion, false);
        StringBuilder b = new StringBuilder("Say this line ");
        if (line.whisper) b.append("in a soft whisper, ");
        b.append("in a ").append(e).append(" voice");
        if (who != null && who.look != null) {
            if (who.look.isChild()) b.append(", like a ").append(who.age > 0 ? who.age + "-year-old " : "young ").append(who.look.female ? "girl" : "boy");
            else if (who.look.kind == Look.MONSTER) b.append(", like a huge scary monster in a children's cartoon");
            else if (who.look.kind == Look.WITCH) b.append(", like a cunning witch in a children's cartoon");
            else if (who.look.kind == Look.OLD_MAN) b.append(", like an old man");
        } else if (who == null) b.append(", like a warm storyteller");
        if (line.manner != null && line.manner.length() > 0) b.append(" (").append(line.manner).append(")");
        return b.toString();
    }

    /** Phone text-to-speech for one line at the given pitch and speed. */
    float[] phoneTts(String text, int voice, float pitch, float rate, File tmpDir, int idx) {
        return phoneTts(text, voice, pitch, rate, tmpDir, idx, storyHindi);
    }

    /** Phone voice in the line's own language (a story may mix Hindi and English lines). */
    float[] phoneTts(String text, int voice, float pitch, float rate, File tmpDir, int idx, boolean hindi) {
        if (!ready || tts == null || text.trim().length() == 0) return null;
        try {
            if (voice >= 0 && voice < voices.size()) tts.setVoice(voices.get(voice));
            else if (hindi == storyHindi) tts.setLanguage(locale);
            else tts.setLanguage(hindi ? new Locale("hi", "IN") : new Locale("en", "IN"));
            tts.setPitch(pitch);
            tts.setSpeechRate(rate);
            File out = new File(tmpDir, "line" + idx + ".wav");
            if (out.exists()) out.delete();
            String id = "u" + idx + "_" + System.nanoTime();
            CountDownLatch latch = new CountDownLatch(1);
            waits.put(id, latch);
            Bundle params = new Bundle();
            int r = tts.synthesizeToFile(text, params, out, id);
            if (r != TextToSpeech.SUCCESS) { waits.remove(id); return null; }
            long timeout = 20000 + text.length() * 300L;
            boolean done = latch.await(timeout, TimeUnit.MILLISECONDS);
            waits.remove(id);
            Boolean good = ok.remove(id);
            if (!done || good == null || !good || !out.exists() || out.length() < 100) return null;
            float[] pcm = readWav(out);
            out.delete();
            if (pcm == null || pcm.length < Synth.SR / 10) return null;
            return pcm;
        } catch (Exception e) {
            return null;
        }
    }

    /** Speaks one line into PCM at Synth.SR, or returns null if the engine fails. */
    public float[] synth(Film.Line line, Cast cast, File tmpDir, int idx) {
        return speak(line, cast, tmpDir, idx, null, null);
    }

    /** Result note from the last speak(): which engine made the voice. */
    public volatile String lastEngine = "";
    /** Set when the AI voice service refused (no key, quota used up); further lines use the phone voice. */
    public volatile boolean aiOff;

    /**
     * Makes one line of dialogue: AI voice (Gemini, with acting direction) or the phone's voice, then — when the
     * user gave a voice sample for this character — moved to the sample's pitch and tone, and finally the emotion's
     * speed/pitch modulation. Returns PCM at Synth.SR or null.
     */
    /** Natural neural voices (online, free). Set by the caller; null = not used. */
    public com.tarun.kahani.core.EdgeVoice edge;
    /** Set after repeated failures of the natural voice service; later lines use the phone voice. */
    public volatile boolean edgeOff;
    private int edgeFails;
    /** True when the last line came from a fallback engine (should not be cached as final). */
    public volatile boolean usedFallback;

    /** Natural voice for one line: MP3 from the service, decoded to PCM at Synth.SR. */
    float[] edgeSpeak(String text, com.tarun.kahani.core.EdgeVoice.Cast c, int[] prosody, File tmpDir, int idx) throws Exception {
        java.io.ByteArrayOutputStream all = new java.io.ByteArrayOutputStream();
        for (String part : com.tarun.kahani.core.EdgeVoice.chunks(text, 700)) {
            byte[] mp3 = null;
            Exception last = null;
            for (int attempt = 0; attempt < 2 && mp3 == null; attempt++) {
                try { mp3 = edge.speak(part, c, prosody); }
                catch (Exception e) { last = e; Thread.sleep(800); }
            }
            if (mp3 == null) throw last;
            all.write(mp3);
        }
        File f = new File(tmpDir, "edge" + idx + ".mp3");
        java.io.FileOutputStream o = new java.io.FileOutputStream(f);
        o.write(all.toByteArray());
        o.close();
        float[] pcm = AudioIO.decode(null, f.getAbsolutePath());
        f.delete();
        if (pcm == null || pcm.length < Synth.SR / 10) throw new java.io.IOException("could not decode the natural voice");
        return pcm;
    }

    /**
     * Makes one line of dialogue: AI voice (Gemini, optional) or the natural neural voice (online, free) or the
     * phone's own voice. When the user gave a voice sample for this character it is then moved to the sample's
     * pitch and tone, and finally the emotion's pitch modulation is applied. Returns PCM at Synth.SR or null.
     */
    public float[] speak(Film.Line line, Cast cast, File tmpDir, int idx, com.tarun.kahani.core.Cloud cloud, String[] err) {
        Cast k = cast != null ? cast : new Cast();
        float[] ef = emotionFactors(line.emotion);
        // how this line is said (shouting, crying, slowly…) on top of the character's own voice qualities
        com.tarun.kahani.core.VoiceStyle ls = com.tarun.kahani.core.VoiceStyle.forLine(k.style, line.manner, line.whisper, false);
        boolean ai = false;
        float[] pcm = null;
        boolean shaped = false;      // the engine already applied emotion and character pitch
        usedFallback = false;
        if (cloud != null && !aiOff && k.gemini != null && cloud.hasGemini()) {
            try {
                float[] raw = cloud.geminiSpeak(direction(line, line.who), line.text, k.gemini);
                pcm = Mixer.resample(raw, 24000);
                shaped = true;
                ai = true;
                lastEngine = "AI";
            } catch (Exception e) {
                if (err != null) err[0] = e.getMessage();
                String m = e.getMessage() == null ? "" : e.getMessage();
                if (m.contains("429") || m.contains("403") || m.contains("400") || m.contains("API key")) aiOff = true;
            }
        }
        if (pcm == null && edge != null && !edgeOff && k.edge != null) {
            try {
                com.tarun.kahani.core.EdgeVoice.Cast ec = com.tarun.kahani.core.EdgeVoice.forLanguage(k.edge, line.hindi);
                // with a sample, speak plainly in the matching gender; the sample decides the pitch
                if (k.sample != null) ec = new com.tarun.kahani.core.EdgeVoice.Cast(ec.voice, 0, ec.ratePct);
                int[] pro = com.tarun.kahani.core.EdgeVoice.emotion(k.sample != null ? Pose.NEUTRAL : line.emotion);
                pro = new int[]{pro[0] + ls.ratePct, pro[1] + (k.sample != null ? 0 : ls.pitchHz)};
                pcm = edgeSpeak(line.text, ec, pro, tmpDir, idx);
                shaped = k.sample == null;
                lastEngine = "natural";
                edgeFails = 0;
            } catch (Exception e) {
                if (err != null) err[0] = e.getMessage();
                if (++edgeFails >= 3) edgeOff = true;
                usedFallback = true;
            }
        }
        if (pcm == null) {
            boolean useSample = k.sample != null;
            // with a sample the phone voice is only the "words"; pitch comes from the sample
            float pitch = useSample ? 1f : k.pitch * ef[1] * (1 + ls.pitchHz / 150f);
            float rate = k.rate * ef[0] * (1 + ls.ratePct / 100f);
            pcm = phoneTts(line.text, line.hindi == storyHindi ? k.voice : -1, pitch, rate, tmpDir, idx, line.hindi);
            if (pcm == null) return null;
            lastEngine = "phone";
            if (!useSample && k.shift != 1f) pcm = shift(pcm, k.shift);
            shaped = !useSample;
        }
        if (k.sample != null) {
            pcm = com.tarun.kahani.core.VoiceFx.matchVoice(pcm, k.sample, Synth.SR);
            if (!shaped && ef[1] != 1f) pcm = com.tarun.kahani.core.VoiceFx.pitch(pcm, ef[1]);
            lastEngine += "+sample";
        }
        pcm = trim(pcm);
        if (ls.any()) pcm = ls.apply(pcm, Synth.SR, ai);
        return pcm;
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
        int pos = 12, channels = 1, sr = 22050, bits = 16, dataPos = -1, dataLen = 0, format = 1;
        while (pos + 8 <= b.length) {
            String id = new String(b, pos, 4, "US-ASCII");
            int len = (b[pos + 4] & 255) | (b[pos + 5] & 255) << 8 | (b[pos + 6] & 255) << 16 | (b[pos + 7] & 255) << 24;
            if (id.equals("fmt ")) {
                format = (b[pos + 8] & 255) | (b[pos + 9] & 255) << 8;
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
        // only plain 8/16-bit PCM here; anything else (24-bit, float…) goes to the phone's decoder
        if (dataPos < 0 || channels < 1 || sr < 4000 || (bits != 8 && bits != 16) || (format != 1 && format != 0xFFFE)) return null;
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
