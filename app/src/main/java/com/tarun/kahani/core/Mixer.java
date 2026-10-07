package com.tarun.kahani.core;

/** Mixes voices, music and sound effects into the film's soundtrack, and builds lip-sync envelopes. */
public final class Mixer {
    private Mixer() {}

    public interface Progress { void update(float fraction); }

    /** Lip-sync: mouth openness at 100 Hz from the voice samples. */
    public static float[] envelope(float[] pcm, int sr) {
        int win = sr / 100;
        int n = pcm.length / win + 1;
        float[] e = new float[n];
        float max = 1e-6f;
        for (int i = 0; i < n; i++) {
            double s = 0;
            int a = i * win, b = Math.min(pcm.length, a + win);
            for (int j = a; j < b; j++) s += pcm[j] * pcm[j];
            e[i] = (float) Math.sqrt(s / Math.max(1, b - a));
        }
        float[] sorted = e.clone();
        java.util.Arrays.sort(sorted);
        float ref = Math.max(1e-4f, sorted[(int) (sorted.length * 0.92f)]);
        float y = 0;
        for (int i = 0; i < n; i++) {
            float v = (e[i] / ref - 0.14f) / 0.8f;
            v = v < 0 ? 0 : v > 1 ? 1 : v;
            y += (v > y ? 0.65f : 0.3f) * (v - y);   // fast open, slower close
            e[i] = y;
        }
        return e;
    }

    /** Linear resample to Synth.SR. */
    public static float[] resample(float[] in, int sr) {
        if (sr == Synth.SR || in.length == 0) return in;
        int n = (int) ((long) in.length * Synth.SR / sr);
        float[] o = new float[n];
        double step = sr / (double) Synth.SR;
        for (int i = 0; i < n; i++) {
            double p = i * step;
            int a = (int) p;
            float f = (float) (p - a);
            float x0 = in[Math.min(a, in.length - 1)], x1 = in[Math.min(a + 1, in.length - 1)];
            o[i] = x0 + (x1 - x0) * f;
        }
        return o;
    }

    /** Adds a cave-like echo. */
    static float[] echo(float[] v) {
        int d1 = (int) (0.11f * Synth.SR), d2 = (int) (0.23f * Synth.SR);
        float[] o = new float[v.length + d2];
        for (int i = 0; i < v.length; i++) {
            o[i] += v[i];
            o[i + d1] += v[i] * 0.32f;
            o[i + d2] += v[i] * 0.16f;
        }
        return o;
    }

    /**
     * Builds the 16-bit mono soundtrack. voices[i] is the PCM of film.lines[i] at Synth.SR (may be null).
     */
    public static short[] mix(Film film, float[][] voices, Progress pr) {
        int n = (int) (film.duration * Synth.SR) + Synth.SR;
        float[] bed = new float[n];
        Synth syn = new Synth();
        int total = film.music.size() + film.sfx.size() + film.lines.size();
        int done = 0;
        for (Film.Music m : film.music) {
            float[] mus = syn.music(m.mood, m.t1 - m.t0 + 0.6f);
            add(bed, mus, m.t0, 0.30f);
            if (pr != null) pr.update(++done / (float) total);
        }
        for (Film.Sfx s : film.sfx) {
            if (s.gain <= 0) { done++; continue; }
            float[] fx = syn.sfx(s.type, s.dur);
            add(bed, fx, s.t, s.gain * 0.55f);
            if (pr != null) pr.update(++done / (float) total);
        }
        // voices into their own buffer for ducking
        float[] voice = new float[n];
        for (int i = 0; i < film.lines.size(); i++) {
            Film.Line l = film.lines.get(i);
            float[] v = voices != null && i < voices.length ? voices[i] : null;
            if (v == null) { done++; continue; }
            if (l.echo) v = echo(v);
            add(voice, v, l.start, l.whisper ? 0.8f : 1.0f);
            if (pr != null) pr.update(++done / (float) total);
        }
        // duck the bed under speech, then combine
        short[] out = new short[n];
        float env = 0;
        float peak = 0.0001f;
        for (int i = 0; i < n; i++) {
            float a = Math.abs(voice[i]);
            env += (a > env ? 0.01f : 0.00012f) * (a - env);
            float duck = 1f - Math.min(0.6f, env * 6f);
            float s = voice[i] + bed[i] * duck;
            bed[i] = s;
            float as = Math.abs(s);
            if (as > peak) peak = as;
        }
        voice = null;
        float gain = peak > 0.95f ? 0.95f / peak : 1f;
        for (int i = 0; i < n; i++) {
            float s = bed[i] * gain;
            // soft knee
            if (s > 0.8f) s = 0.8f + (s - 0.8f) * 0.4f;
            else if (s < -0.8f) s = -0.8f + (s + 0.8f) * 0.4f;
            out[i] = (short) (Math.max(-1f, Math.min(1f, s)) * 32000);
        }
        return out;
    }

    static void add(float[] dst, float[] src, float t, float g) {
        int a = (int) (t * Synth.SR);
        for (int i = 0; i < src.length; i++) {
            int j = a + i;
            if (j < 0) continue;
            if (j >= dst.length) break;
            dst[j] += src[i] * g;
        }
    }
}
