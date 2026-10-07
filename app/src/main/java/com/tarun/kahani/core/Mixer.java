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

    /** Gives the voice of film.lines[i] at Synth.SR (null = silent). Lets long films keep voices on storage. */
    public interface VoiceSource { float[] voice(int i); }

    /** Receives the finished soundtrack piece by piece (16-bit mono at Synth.SR). */
    public interface Sink { void write(short[] buf, int n) throws java.io.IOException; }

    public static short[] mix(Film film, final float[][] voices, SoundLib lib, Edits ed, Progress pr) {
        final int n = (int) (film.duration * Synth.SR) + Synth.SR;
        final short[] out = new short[n];
        final int[] pos = {0};
        try {
            mixTo(film, new VoiceSource() {
                public float[] voice(int i) { return voices != null && i < voices.length ? voices[i] : null; }
            }, lib, ed, new Sink() {
                public void write(short[] buf, int len) {
                    int k = Math.min(len, n - pos[0]);
                    System.arraycopy(buf, 0, out, pos[0], k);
                    pos[0] += k;
                }
            }, pr);
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        }
        return out;
    }

    /** A sound placed on the timeline. Samples are produced lazily so only sounds playing "now" use memory. */
    static abstract class Clip {
        int start, len;      // in samples
        float gain;
        boolean voice;
        abstract void prepare();
        abstract float at(int i);  // i in [0, len)
        void free() {}
    }

    static final class ArrayClip extends Clip {
        float[] data;
        ArrayClip(float[] d, int start, float gain) { data = d; this.start = start; len = d == null ? 0 : d.length; this.gain = gain; }
        void prepare() {}
        float at(int i) { return data[i]; }
        void free() { data = null; }
    }

    /** Repeats a recording to fill a duration with 1 s crossfades and an overall fade in/out (no big buffer). */
    static final class LoopClip extends Clip {
        final float[] src;
        final int xf, step, fade;
        LoopClip(float[] src, int start, float dur, float fadeSec, float gain) {
            this.src = src; this.start = start; this.gain = gain;
            len = Math.max(1, (int) (dur * Synth.SR));
            xf = Math.min(src.length / 4, Synth.SR);
            step = src.length - xf;
            fade = Math.min(len / 2, (int) (fadeSec * Synth.SR));
        }
        void prepare() {}
        float at(int i) {
            int k = i / step, j = i - k * step;
            float v = src[j] * (k > 0 && j < xf ? j / (float) xf : 1f);
            if (k > 0 && j < xf) v += src[j + step] * ((src.length - (j + step)) / (float) xf);
            if (i < fade) v *= i / (float) fade;
            else if (i >= len - fade) v *= (len - 1 - i) / (float) fade;
            return v;
        }
    }

    /** Builds a clip's samples only when it starts playing. */
    static final class LazyClip extends Clip {
        interface Maker { float[] make(); }
        final Maker maker;
        float[] data;
        LazyClip(int start, int estLen, float gain, Maker m) { this.start = start; this.len = estLen; this.gain = gain; maker = m; }
        void prepare() {
            if (data != null) return;
            data = maker.make();
            len = data == null ? 0 : data.length;
        }
        float at(int i) { return data[i]; }
        void free() { data = null; }
    }

    /**
     * Streams the soundtrack in 8-second pieces: music, ambience and effects form a bed that ducks under speech,
     * then a smooth limiter keeps it from clipping. Memory stays small even for a 30-minute film.
     */
    public static void mixTo(final Film film, final VoiceSource vs, final SoundLib lib, Edits edits, Sink sink, Progress pr) throws java.io.IOException {
        final Edits ed = edits == null ? new Edits() : edits;
        final int total = (int) (film.duration * Synth.SR) + Synth.SR;
        final Synth syn = new Synth();
        java.util.ArrayList<Clip> clips = new java.util.ArrayList<Clip>();
        // ---- music (recorded tracks when available)
        for (final Film.Music m : film.music) {
            final float dur = m.t1 - m.t0 + 0.6f;
            float g = (lib != null ? 0.22f : 0.30f) * ed.music;
            float[] src = lib == null ? null : lib.pcm(lib.byFile(musicFile(m.mood)));
            if (src != null && src.length >= Synth.SR / 4) clips.add(new LoopClip(src, (int) (m.t0 * Synth.SR), dur, 1.2f, g));
            else clips.add(new LazyClip((int) (m.t0 * Synth.SR), (int) (dur * Synth.SR), g, new LazyClip.Maker() {
                public float[] make() { return syn.music(m.mood, dur); }
            }));
        }
        // ---- ambience beds
        if (lib != null) for (Film.Amb a : film.ambience) {
            // "#path" = the background sound the user chose for this part of the story
            SoundLib.Entry e = a.words.startsWith("#") ? lib.byFile(a.words.substring(1)) : lib.best(a.words, "amb", null);
            float[] src = lib.pcm(e);
            if (src != null && src.length >= Synth.SR / 4 && a.t1 > a.t0)
                clips.add(new LoopClip(src, (int) (a.t0 * Synth.SR), a.t1 - a.t0, 1.5f, 0.32f * ed.ambience));
        }
        // ---- effects
        for (final Film.Sfx s : film.sfx) {
            if (s.gain <= 0) continue;
            final boolean ambient = s.type == Film.SFX_STREAM || s.type == Film.SFX_BIRDS || s.type == Film.SFX_WIND || s.type == Film.SFX_NIGHT
                    || s.type == Film.SFX_DRIP || s.type == Film.SFX_CROWD;
            float g = s.gain * 0.55f * (ambient ? ed.ambience : ed.sfx);
            String f = lib == null ? null : sfxFile(s.type);
            final float[] src = f == null ? null : lib.pcm(lib.byFile(f));
            int st = (int) (s.t * Synth.SR);
            if (src != null && src.length >= Synth.SR / 4 && (ambient || s.dur > src.length / (float) Synth.SR)) {
                clips.add(new LoopClip(src, st, s.dur, ambient ? 1f : 0.05f, g));
            } else {
                clips.add(new LazyClip(st, (int) (Math.max(s.dur, 0.6f) * Synth.SR), g, new LazyClip.Maker() {
                    public float[] make() {
                        if (src == null) return syn.sfx(s.type, s.dur);
                        int len = Math.min(src.length, (int) (Math.max(s.dur, 0.6f) * Synth.SR));
                        float[] fx = new float[len];
                        System.arraycopy(src, 0, fx, 0, len);
                        int f2 = Math.min(len / 3, Synth.SR / 5);
                        for (int i = 0; i < f2; i++) fx[len - 1 - i] *= i / (float) f2;
                        return fx;
                    }
                }));
            }
        }
        // ---- voices (loaded from the source only when they start)
        for (int i = 0; i < film.lines.size(); i++) {
            final Film.Line l = film.lines.get(i);
            final int idx = i;
            float g = ed.gainFor(l.who == null ? null : l.who.displayName) * (l.whisper ? 0.8f : 1.0f);
            LazyClip c = new LazyClip((int) (l.start * Synth.SR), (int) (Math.max(0.1f, l.dur) * Synth.SR), g, new LazyClip.Maker() {
                public float[] make() {
                    float[] v = vs == null ? null : vs.voice(idx);
                    return v != null && l.echo ? echo(v) : v;
                }
            });
            c.voice = true;
            clips.add(c);
        }
        java.util.Collections.sort(clips, new java.util.Comparator<Clip>() {
            public int compare(Clip a, Clip b) { return a.start < b.start ? -1 : a.start > b.start ? 1 : 0; }
        });

        final int chunk = Synth.SR * 8;
        float[] bed = new float[chunk], voice = new float[chunk];
        short[] out = new short[chunk];
        java.util.ArrayList<Clip> active = new java.util.ArrayList<Clip>();
        int next = 0;
        float env = 0, lim = 1f;
        for (int a = 0; a < total; a += chunk) {
            int n = Math.min(chunk, total - a), b = a + n;
            while (next < clips.size() && clips.get(next).start < b) {
                Clip c = clips.get(next++);
                c.prepare();
                if (c.len > 0 && c.start + c.len > a) active.add(c);
                else c.free();
            }
            java.util.Arrays.fill(bed, 0, n, 0f);
            java.util.Arrays.fill(voice, 0, n, 0f);
            for (int k = active.size() - 1; k >= 0; k--) {
                Clip c = active.get(k);
                float[] dst = c.voice ? voice : bed;
                int from = Math.max(a, c.start), to = Math.min(b, c.start + c.len);
                for (int t = from; t < to; t++) dst[t - a] += c.at(t - c.start) * c.gain;
                if (c.start + c.len <= b) { c.free(); active.remove(k); }
            }
            for (int i = 0; i < n; i++) {
                float av = Math.abs(voice[i]);
                env += (av > env ? 0.01f : 0.00012f) * (av - env);
                float duck = 1f - Math.min(0.6f, env * 6f);
                float s = voice[i] + bed[i] * duck;
                // limiter: instant attack, ~0.5 s release, keeps peaks under 0.95
                float as = Math.abs(s) * lim;
                if (as > 0.95f) lim = 0.95f / Math.abs(s);
                else lim += (1f - lim) * 0.00006f;
                s *= lim;
                if (s > 0.8f) s = 0.8f + (s - 0.8f) * 0.4f;
                else if (s < -0.8f) s = -0.8f + (s + 0.8f) * 0.4f;
                out[i] = (short) (Math.max(-1f, Math.min(1f, s)) * 32000);
            }
            sink.write(out, n);
            if (pr != null) pr.update(b / (float) total);
        }
        if (pr != null) pr.update(1f);
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
