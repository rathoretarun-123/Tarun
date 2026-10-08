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
            y += (v > y ? 0.5f : 0.25f) * (v - y);   // quick to open, slower to close
            e[i] = y;
        }
        // smoothed backwards too (no flicker between frames: real lips glide from syllable to syllable) ...
        y = 0;
        for (int i = n - 1; i >= 0; i--) { y += 0.4f * (e[i] - y); e[i] = 0.5f * (e[i] + y); }
        // ... and the lips move a moment before the sound is heard (about 40 ms), as real speakers' do
        int lead = 4;
        float[] o = new float[n];
        for (int i = 0; i < n; i++) o[i] = e[Math.min(n - 1, i + lead)];
        return o;
    }

    /**
     * The mouth's shape along a voice, 100 values a second: how bright the sound is. Round vowels (o, u) are
     * dark, spread vowels (e, i) and s-sounds are bright. 0 = round .. 1 = wide.
     */
    public static float[] shape(float[] pcm, int sr) {
        int win = sr / 100;
        int n = pcm.length / win + 1;
        float[] out = new float[n];
        float y = 0.5f;
        for (int i = 0; i < n; i++) {
            double e = 0, d = 0;
            int a = i * win, b = Math.min(pcm.length, a + win);
            for (int j = Math.max(1, a); j < b; j++) { e += pcm[j] * pcm[j]; float df = pcm[j] - pcm[j - 1]; d += df * df; }
            float v = 0.5f;
            if (e > 1e-7 * Math.max(1, b - a)) {
                // the "centre" frequency of the sound from how fast it changes
                double fc = sr / (2 * Math.PI) * Math.sqrt(d / e);
                v = (float) Math.max(0, Math.min(1, (fc - 650) / 1500));
            }
            y += 0.3f * (v - y);
            out[i] = y;
        }
        return out;
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

    /** Linear resample from one rate to another (a light average first when going down, against harshness). */
    public static float[] resampleTo(float[] in, int from, int to) {
        if (from == to || in.length == 0) return in;
        int n = (int) ((long) in.length * to / from);
        float[] o = new float[n];
        double step = from / (double) to;
        int span = Math.max(1, (int) Math.floor(step));
        for (int i = 0; i < n; i++) {
            int a = (int) (i * step);
            float sum = 0;
            int c = 0;
            for (int k = 0; k < span && a + k < in.length; k++) { sum += in[a + k]; c++; }
            o[i] = c == 0 ? 0 : sum / c;
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

    /** Words that find music for a mood (the user's tagged music first, then the built-in tracks). */
    static String moodWords(int mood) {
        switch (mood) {
            case Film.M_TITLE: return "adventure exciting journey title";
            case Film.M_HAPPY: return "calm peaceful gentle morning";
            case Film.M_PLAYFUL: return "happy cheerful playful funny";
            case Film.M_TENSE: return "tense suspense danger chase";
            case Film.M_VILLAIN: return "villain evil menacing dark mystery";
            case Film.M_SAD: return "sad sorrow emotional";
            case Film.M_ACTION: return "epic battle heroic climax action";
            case Film.M_CELEBRATE: return "festive festival celebration";
            case Film.M_NIGHT: return "night mystery";
            case Film.M_END: return "happy ending joyful";
            default: return "calm";
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
            case Film.SFX_STEPS_RUN: return "running_footsteps.ogg";
            case Film.SFX_CROWD: return "crowd_market.ogg";
            case Film.SFX_SWORD: return "sword_clash.ogg";
            case Film.SFX_NET: return "whoosh.ogg";
            case Film.SFX_THUNDER: return "thunder.ogg";
            default: return null;
        }
    }

    /** Gives the voice of film.lines[i] at Synth.SR (null = silent). Lets long films keep voices on storage. */
    public interface VoiceSource { float[] voice(int i); }

    /** The soundtrack is stereo: left and right samples take turns (L, R, L, R…). */
    public static final int CHANNELS = 2;

    /** Receives the finished soundtrack piece by piece (16-bit stereo at Synth.SR, interleaved; n = values). */
    public interface Sink { void write(short[] buf, int n) throws java.io.IOException; }

    public static short[] mix(Film film, final float[][] voices, SoundLib lib, Edits ed, Progress pr) {
        final int n = ((int) (film.duration * Synth.SR) + Synth.SR) * CHANNELS;
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
        float pan;           // -1 left .. +1 right (where the speaker stands)
        float send;          // how much goes into the room (echo of the place)
        Film.Music music;    // loudness curve and softness of a music cue
        float lpA, lp;       // one-pole low-pass (soft moods)
        abstract void prepare();
        abstract float at(int i);  // i in [0, len)
        /** The right channel (the same as the left unless the clip is spread wide). */
        float atR(int i) { return at(i); }
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
        /** Right channel starts this many samples later in the recording: a wide, natural stereo bed. */
        int wide;
        float core(int i) {
            int k = i / step, j = i - k * step;
            float v = src[j] * (k > 0 && j < xf ? j / (float) xf : 1f);
            if (k > 0 && j < xf) v += src[j + step] * ((src.length - (j + step)) / (float) xf);
            return v;
        }
        float fadeAt(int i) {
            if (i < fade) return i / (float) fade;
            if (i >= len - fade) return (len - 1 - i) / (float) fade;
            return 1f;
        }
        float at(int i) { return core(i) * fadeAt(i); }
        float atR(int i) { return wide > 0 ? core(i + wide) * fadeAt(i) : at(i); }
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
            float[] src = null;
            if (lib != null) {
                // the user's own music for this mood (from any earlier story) wins over the built-in track
                SoundLib.Entry e = lib.best(moodWords(m.mood), "music", null);
                if (e == null || e.path.startsWith("asset:")) e = lib.byFile(musicFile(m.mood));
                src = lib.pcm(e);
            }
            Clip c;
            if (src != null && src.length >= Synth.SR / 4) c = new LoopClip(src, (int) (m.t0 * Synth.SR), dur, 1.2f, g);
            else c = new LazyClip((int) (m.t0 * Synth.SR), (int) (dur * Synth.SR), g, new LazyClip.Maker() {
                public float[] make() { return syn.music(m.mood, dur); }
            });
            c.music = m;
            if (m.soft) c.lpA = 0.18f;   // ~1.1 kHz: muffled, gentle
            clips.add(c);
        }
        // ---- ambience beds
        if (lib != null) for (Film.Amb a : film.ambience) {
            // "#path" = the background sound the user chose for this part of the story
            SoundLib.Entry e = a.words.startsWith("#") ? lib.byFile(a.words.substring(1)) : lib.best(a.words, "amb", null);
            float[] src = lib.pcm(e);
            if (src != null && src.length >= Synth.SR / 4 && a.t1 > a.t0) {
                LoopClip lc = new LoopClip(src, (int) (a.t0 * Synth.SR), a.t1 - a.t0, 1.5f, 0.32f * ed.ambience);
                lc.wide = Math.max(1, (int) (src.length * 0.37f));
                clips.add(lc);
            }
        }
        // ---- effects
        for (final Film.Sfx s : film.sfx) {
            if (s.gain <= 0) continue;
            final boolean ambient = s.type == Film.SFX_STREAM || s.type == Film.SFX_BIRDS || s.type == Film.SFX_WIND || s.type == Film.SFX_NIGHT
                    || s.type == Film.SFX_DRIP || s.type == Film.SFX_CROWD;
            float g = s.gain * 0.55f * (ambient ? ed.ambience : ed.sfx);
            String f = lib == null ? null : sfxFile(s.type);
            final float[] src = lib == null ? null : s.file != null ? lib.pcm(lib.byPath(s.file)) : f == null ? null : lib.pcm(lib.byFile(f));
            if (s.type == Film.SFX_USER && src == null) continue;     // the user's sound is gone: nothing else fits
            int st = (int) (s.t * Synth.SR);
            if (src != null && src.length >= Synth.SR / 4 && (ambient || s.dur > src.length / (float) Synth.SR)) {
                LoopClip lc = new LoopClip(src, st, s.dur, ambient ? 1f : 0.05f, g);
                if (ambient) lc.wide = Math.max(1, (int) (src.length * 0.41f));
                else lc.send = 0.5f;
                clips.add(lc);
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
                clips.get(clips.size() - 1).send = ambient ? 0 : 0.5f;   // a door, a bell, thunder ring in the room
            }
        }
        // ---- voices (loaded from the source only when they start), each from where its speaker stands, every
        // line brought to the same loudness, with a soft breath before long or emotional lines
        float prevEnd = -10f;
        for (int i = 0; i < film.lines.size(); i++) {
            final Film.Line l = film.lines.get(i);
            final int idx = i;
            float g = ed.gainFor(l.who == null ? null : l.who.displayName) * (l.whisper ? 0.8f : 1.0f);
            LazyClip c = new LazyClip((int) (l.start * Synth.SR), (int) (Math.max(0.1f, l.dur) * Synth.SR), g, new LazyClip.Maker() {
                public float[] make() {
                    float[] v = vs == null ? null : vs.voice(idx);
                    v = dialogue(v, l.whisper);
                    return v != null && l.echo ? echo(v) : v;
                }
            });
            c.voice = true;
            c.pan = panOf(film, l);
            c.send = 1f;
            clips.add(c);
            boolean feeling = l.emotion == Pose.SCARED || l.emotion == Pose.ANGRY || l.emotion == Pose.SURPRISED || l.emotion == Pose.PAIN
                    || l.emotion == Pose.SAD;
            if (l.env != null && (l.dur > 2.2f || feeling) && l.start - prevEnd > 0.55f && l.start > 0.5f) {
                final int seed = i * 31 + 7;
                final boolean deep = feeling;
                LazyClip br = new LazyClip((int) ((l.start - 0.42f) * Synth.SR), (int) (0.34f * Synth.SR), g, new LazyClip.Maker() {
                    public float[] make() { return breath(seed, 0.34f, deep ? 0.016f : 0.011f); }
                });
                br.voice = true;
                br.pan = c.pan;
                br.send = 0.6f;
                clips.add(br);
            }
            prevEnd = Math.max(prevEnd, l.start + l.dur);
        }
        java.util.Collections.sort(clips, new java.util.Comparator<Clip>() {
            public int compare(Clip a, Clip b) { return a.start < b.start ? -1 : a.start > b.start ? 1 : 0; }
        });

        final int chunk = Synth.SR * 8;
        float[] bedL = new float[chunk], bedR = new float[chunk], voiceL = new float[chunk], voiceR = new float[chunk], send = new float[chunk];
        short[] out = new short[chunk * CHANNELS];
        java.util.ArrayList<Clip> active = new java.util.ArrayList<Clip>();
        int next = 0;
        float env = 0, lim = 1f, comp = 0;
        Room room = new Room(Synth.SR);
        float[] wetLR = new float[2];
        for (int a = 0; a < total; a += chunk) {
            int n = Math.min(chunk, total - a), b = a + n;
            while (next < clips.size() && clips.get(next).start < b) {
                Clip c = clips.get(next++);
                c.prepare();
                if (c.len > 0 && c.start + c.len > a) active.add(c);
                else c.free();
            }
            java.util.Arrays.fill(bedL, 0, n, 0f);
            java.util.Arrays.fill(bedR, 0, n, 0f);
            java.util.Arrays.fill(voiceL, 0, n, 0f);
            java.util.Arrays.fill(voiceR, 0, n, 0f);
            java.util.Arrays.fill(send, 0, n, 0f);
            for (int k = active.size() - 1; k >= 0; k--) {
                Clip c = active.get(k);
                float[] dl = c.voice ? voiceL : bedL, dr = c.voice ? voiceR : bedR;
                int from = Math.max(a, c.start), to = Math.min(b, c.start + c.len);
                // balance: the far side gets quieter, the near side stays as it is
                float gl = c.gain * Math.min(1f, 1f - c.pan), gr = c.gain * Math.min(1f, 1f + c.pan);
                for (int t = from; t < to; t++) {
                    int i = t - c.start;
                    float v = c.at(i), vr = c.atR(i);
                    if (c.lpA > 0) { c.lp += c.lpA * (v - c.lp); v = c.lp * 1.25f; vr = v; }
                    float lv = c.music == null ? 1f : c.music.level(t / (float) Synth.SR);
                    dl[t - a] += v * gl * lv;
                    dr[t - a] += vr * gr * lv;
                    if (c.send > 0) send[t - a] += (v + vr) * 0.5f * c.gain * c.send;
                }
                if (c.start + c.len <= b) { c.free(); active.remove(k); }
            }
            int sub = Synth.SR / 4;
            for (int i = 0; i < n; i++) {
                // the room of the place on screen now (looked up four times a second, glides between places)
                if (i % sub == 0) room.target(Room.of(film.segAt((a + i) / (float) Synth.SR)));
                // dialogue: a gentle compressor keeps every word clear and even
                float vl = voiceL[i], vr = voiceR[i];
                float lv = Math.max(Math.abs(vl), Math.abs(vr));
                comp += (lv > comp ? 0.02f : 0.0003f) * (lv - comp);
                float cg = comp > 0.2f ? (float) Math.pow(0.2f / comp, 0.6f) : 1f;
                vl *= cg * 1.12f; vr *= cg * 1.12f;
                float av = Math.max(Math.abs(vl), Math.abs(vr));
                env += (av > env ? 0.01f : 0.00012f) * (av - env);
                float duck = 1f - Math.min(0.6f, env * 6f);
                room.process(send[i], wetLR);
                float sl = vl + bedL[i] * duck + wetLR[0], sr = vr + bedR[i] * duck + wetLR[1];
                // limiter on both sides together: instant attack, ~0.5 s release, keeps peaks under 0.95
                float as = Math.max(Math.abs(sl), Math.abs(sr)) * lim;
                if (as > 0.95f) lim = 0.95f / Math.max(Math.abs(sl), Math.abs(sr));
                else lim += (1f - lim) * 0.00006f;
                sl *= lim; sr *= lim;
                out[2 * i] = (short) (soft(sl) * 32000);
                out[2 * i + 1] = (short) (soft(sr) * 32000);
            }
            sink.write(out, n * CHANNELS);
            if (pr != null) pr.update(b / (float) total);
        }
        if (pr != null) pr.update(1f);
    }

    static float soft(float s) {
        if (s > 0.8f) s = 0.8f + (s - 0.8f) * 0.4f;
        else if (s < -0.8f) s = -0.8f + (s + 0.8f) * 0.4f;
        return Math.max(-1f, Math.min(1f, s));
    }

    /** Where the speaker stands on the stage: -0.5 (left) .. +0.5 (right); the narrator is in the middle. */
    static float panOf(Film film, Film.Line l) {
        if (l.who == null) return 0;
        float t = l.start + 0.05f;
        Film.Seg s = film.segAt(t);
        if (s == null) return 0;
        for (Film.Actor a : s.actors) {
            if (a.c != l.who) continue;
            float x = Director.xAt(a, t);
            return Math.max(-0.5f, Math.min(0.5f, (x - 640f) / 640f * 0.5f));
        }
        return 0;
    }

    /**
     * Dialogue polish, the way a film's sound editor prepares each line: low rumble and pops below ~90 Hz are
     * removed and every line is brought to the same speaking loudness (phone voice, natural voice, AI voice
     * or the user's own recording all sit together). Whispers stay a little quieter.
     */
    public static float[] dialogue(float[] v, boolean whisper) {
        if (v == null || v.length == 0) return v;
        float[] y = new float[v.length];
        float a = (float) Math.exp(-2 * Math.PI * 90 / Synth.SR), px = 0, py = 0;
        for (int i = 0; i < v.length; i++) { py = a * (py + v[i] - px); px = v[i]; y[i] = py; }
        int win = Synth.SR / 50;
        int frames = Math.max(1, y.length / win);
        float[] rms = new float[frames];
        float maxR = 0, peak = 0;
        for (int f = 0; f < frames; f++) {
            double sum = 0;
            for (int i = f * win; i < Math.min(y.length, (f + 1) * win); i++) sum += y[i] * y[i];
            rms[f] = (float) Math.sqrt(sum / win);
            maxR = Math.max(maxR, rms[f]);
        }
        for (float x : y) peak = Math.max(peak, Math.abs(x));
        if (maxR < 1e-5f) return y;
        double sum = 0;
        int n = 0;
        for (float r : rms) if (r > maxR * 0.12f) { sum += r * r; n++; }
        float speech = (float) Math.sqrt(sum / Math.max(1, n));
        float target = whisper ? 0.075f : 0.11f;
        float g = Math.max(0.25f, Math.min(5f, target / Math.max(1e-5f, speech)));
        g = Math.min(g, 0.97f / Math.max(1e-5f, peak));
        for (int i = 0; i < y.length; i++) y[i] *= g;
        return y;
    }

    /** A soft breath in (filtered air noise that swells and stops), rms = how loud. */
    public static float[] breath(int seed, float dur, float rms) {
        int n = Math.max(16, (int) (dur * Synth.SR));
        float[] b = new float[n];
        java.util.Random r = new java.util.Random(seed);
        float lp = 0, lp2 = 0, hp = 0, prev = 0;
        double sum = 0;
        for (int i = 0; i < n; i++) {
            float w = r.nextFloat() * 2 - 1;
            lp += 0.32f * (w - lp);
            lp2 += 0.5f * (lp - lp2);
            hp = 0.96f * (hp + lp2 - prev);
            prev = lp2;
            float u = i / (float) n;
            float e = u < 0.62f ? (float) Math.pow(Math.sin(u / 0.62f * Math.PI / 2), 2) : (float) Math.pow(Math.cos((u - 0.62f) / 0.38f * Math.PI / 2), 2);
            b[i] = hp * e;
            sum += b[i] * b[i];
        }
        float g = rms / (float) Math.max(1e-6, Math.sqrt(sum / n));
        for (int i = 0; i < n; i++) b[i] *= g;
        return b;
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
