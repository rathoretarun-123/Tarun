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
    public static short[] mix(Film film, float[][] voices, Progress pr) { return mix(film, voices, null, null, pr); }

    /** Music mood -> recorded track in the sound library. */
    static String musicFile(int mood) {
        switch (mood) {
            case Film.M_TITLE: return "music_adventure.ogg";
            case Film.M_HAPPY: return "music_calm.ogg";
            case Film.M_PLAYFUL: return "music_happy.ogg";
            case Film.M_TENSE: return "music_tense.ogg";
            case Film.M_VILLAIN: return "music_mystery.ogg";
            case Film.M_SAD: return "music_sad.ogg";
            case Film.M_ACTION: return "music_epic.ogg";
            case Film.M_CELEBRATE: return "music_festive.ogg";
            case Film.M_NIGHT: return "music_mystery.ogg";
            case Film.M_END: return "music_happy.ogg";
            default: return "music_calm.ogg";
        }
    }

    /** Sound effect -> recorded file (null = use the synthesiser). */
    static String sfxFile(int type) {
        switch (type) {
            case Film.SFX_STREAM: return "river.ogg";
            case Film.SFX_BIRDS: return "forest_birds.ogg";
            case Film.SFX_WIND: return "wind.ogg";
            case Film.SFX_CHIME: case Film.SFX_MAGIC: case Film.SFX_GLASS: return "magic_chime.ogg";
            case Film.SFX_THUD: return "footsteps.ogg";
            case Film.SFX_WHOOSH: case Film.SFX_WHOOSH_CARD: return "whoosh.ogg";
            case Film.SFX_BELL: return "temple_bell.ogg";
            case Film.SFX_DRUMS: return "drum_roll.ogg";
            case Film.SFX_NIGHT: return "night_crickets.ogg";
            case Film.SFX_ROAR: return "monster_growl.ogg";
            case Film.SFX_ANKLET: return "small_bells.ogg";
            case Film.SFX_DRIP: return "cave.ogg";
            case Film.SFX_SPLASH: return "water_splash.ogg";
            case Film.SFX_STEPS: return "footsteps.ogg";
            case Film.SFX_CROWD: return "crowd_market.ogg";
            case Film.SFX_SWORD: return "sword_clash.ogg";
            case Film.SFX_NET: return "whoosh.ogg";
            default: return null;
        }
    }

    public static short[] mix(Film film, float[][] voices, SoundLib lib, Edits ed, Progress pr) {
        if (ed == null) ed = new Edits();
        int n = (int) (film.duration * Synth.SR) + Synth.SR;
        float[] bed = new float[n];
        Synth syn = new Synth();
        int total = film.music.size() + film.sfx.size() + film.lines.size() + film.ambience.size() + 1;
        int done = 0;
        // ---- music (recorded tracks when available)
        for (Film.Music m : film.music) {
            float dur = m.t1 - m.t0 + 0.6f;
            float[] mus = null;
            if (lib != null) {
                SoundLib.Entry e = lib.byFile(musicFile(m.mood));
                float[] src = lib.pcm(e);
                if (src != null) mus = SoundLib.loop(src, dur, 1.2f);
            }
            if (mus == null) mus = syn.music(m.mood, dur);
            add(bed, mus, m.t0, (lib != null ? 0.22f : 0.30f) * ed.music);
            if (pr != null) pr.update(++done / (float) total);
        }
        // ---- ambience beds
        if (lib != null) for (Film.Amb a : film.ambience) {
            SoundLib.Entry e = lib.best(a.words, "amb", null);
            float[] src = lib.pcm(e);
            if (src != null) add(bed, SoundLib.loop(src, a.t1 - a.t0, 1.5f), a.t0, 0.32f * ed.ambience);
            if (pr != null) pr.update(++done / (float) total);
        }
        // ---- effects
        for (Film.Sfx s : film.sfx) {
            if (s.gain <= 0) { done++; continue; }
            float[] fx = null;
            boolean ambient = s.type == Film.SFX_STREAM || s.type == Film.SFX_BIRDS || s.type == Film.SFX_WIND || s.type == Film.SFX_NIGHT
                    || s.type == Film.SFX_DRIP || s.type == Film.SFX_CROWD;
            if (lib != null) {
                String f = sfxFile(s.type);
                float[] src = f == null ? null : lib.pcm(lib.byFile(f));
                if (src != null) {
                    if (ambient || s.dur > src.length / (float) Synth.SR) fx = SoundLib.loop(src, s.dur, ambient ? 1f : 0.05f);
                    else {
                        int len = Math.min(src.length, (int) (Math.max(s.dur, 0.6f) * Synth.SR));
                        fx = new float[len];
                        System.arraycopy(src, 0, fx, 0, len);
                        int f2 = Math.min(len / 3, Synth.SR / 5);
                        for (int i = 0; i < f2; i++) fx[len - 1 - i] *= i / (float) f2;
                    }
                }
            }
            if (fx == null) fx = syn.sfx(s.type, s.dur);
            add(bed, fx, s.t, s.gain * 0.55f * (ambient ? ed.ambience : ed.sfx));
            if (pr != null) pr.update(++done / (float) total);
        }
        // ---- voices into their own buffer for ducking
        float[] voice = new float[n];
        for (int i = 0; i < film.lines.size(); i++) {
            Film.Line l = film.lines.get(i);
            float[] v = voices != null && i < voices.length ? voices[i] : null;
            if (v == null) { done++; continue; }
            if (l.echo) v = echo(v);
            float g = ed.gainFor(l.who == null ? null : l.who.displayName) * (l.whisper ? 0.8f : 1.0f);
            add(voice, v, l.start, g);
            if (pr != null) pr.update(++done / (float) total);
        }
        // ---- duck the bed under speech, then combine
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
        if (pr != null) pr.update(1f);
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
