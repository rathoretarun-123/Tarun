package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Listens to a sound offline and guesses what it is, so sounds added without a useful name can still be used
 * in the right places: whether it is a background (loops under a scene), a one-off effect, music or people
 * talking, and what it sounds like (water / rain, wind, fire, birds, insects, thunder, bells, steps…).
 * These are guesses from the sound's loudness, pitch and spectrum; the name the user gives always counts more.
 */
public final class SoundSense {
    private SoundSense() {}

    public static final class Info {
        public String kind = "amb";                         // amb | sfx | music | voices
        public final List<String> like = new ArrayList<String>();   // English words, e.g. "water", "rain"
        public float seconds;
        // measurements (kept for checking)
        public float flat, centroid, low, high, harm, onsets, syllabic, range, peaky, beat, silent, events;
        public String words() {
            StringBuilder b = new StringBuilder();
            for (String w : like) { if (b.length() > 0) b.append(", "); b.append(w); }
            return b.toString();
        }
    }

    /** Analyses (at most the first 30 seconds of) mono PCM. */
    public static Info analyse(float[] x, int sr) {
        Info in = new Info();
        in.seconds = x.length / (float) sr;
        if (x.length > sr * 30) x = java.util.Arrays.copyOf(x, sr * 30);
        int n = 2048, hop = 1024;
        int frames = Math.max(0, (x.length - n) / hop);
        if (frames < 4) { in.kind = "sfx"; return in; }
        float[] db = new float[frames], flat = new float[frames], cen = new float[frames], peak = new float[frames];
        float[] re = new float[n], im = new float[n];
        double lowE = 0, highE = 0, allE = 0;
        float harmSum = 0;
        int harmN = 0;
        for (int f = 0; f < frames; f++) {
            int s0 = f * hop;
            double e = 0;
            for (int i = 0; i < n; i++) {
                float v = x[s0 + i];
                e += v * v;
                re[i] = v * (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / n));
                im[i] = 0;
            }
            db[f] = (float) (10 * Math.log10(e / n + 1e-12));
            VoiceFx.fft(re, im);
            double sumLog = 0, sum = 0, num = 0, mx = 0;
            int bins = 0;
            int b0 = (int) (100f * n / sr), b1 = Math.min(n / 2, (int) (9000f * n / sr));
            for (int k = b0; k < b1; k++) {
                double p = re[k] * re[k] + im[k] * im[k] + 1e-12;
                sumLog += Math.log(p);
                sum += p;
                num += p * k * sr / (double) n;
                mx = Math.max(mx, p);
                bins++;
                float hz = k * sr / (float) n;
                if (hz < 300) lowE += p; else if (hz > 4000) highE += p;
                allE += p;
            }
            flat[f] = (float) (Math.exp(sumLog / bins) / (sum / bins));
            cen[f] = (float) (num / sum);
            peak[f] = (float) (mx / (sum / bins));
            // periodicity (voiced speech, music, tonal sounds): best normalised self-similarity 80–1000 Hz
            if (f % 2 == 0 && e / n > 1e-6) {
                double best = 0;
                for (int lag = sr / 1000; lag <= sr / 80; lag += 2) {
                    double a = 0, b2 = 0, c = 0;
                    for (int i = 0; i + lag < n; i += 3) { a += x[s0 + i] * x[s0 + i + lag]; b2 += x[s0 + i] * x[s0 + i]; c += x[s0 + i + lag] * x[s0 + i + lag]; }
                    double r = a / Math.sqrt(b2 * c + 1e-12);
                    if (r > best) best = r;
                }
                harmSum += best;
                harmN++;
            }
        }
        // loudness statistics over the frames
        float[] sorted = db.clone();
        java.util.Arrays.sort(sorted);
        float top = sorted[(int) (frames * 0.95f)], p10 = sorted[(int) (frames * 0.1f)], p90 = sorted[(int) (frames * 0.9f)];
        in.range = p90 - p10;
        int loud = 0, silent = 0;
        double fl = 0, ce = 0, pk = 0;
        for (int f = 0; f < frames; f++) {
            if (db[f] < top - 35) { silent++; continue; }
            loud++;
            fl += flat[f]; ce += cen[f]; pk += Math.log(peak[f]);
        }
        loud = Math.max(1, loud);
        in.silent = silent / (float) frames;
        in.flat = (float) (fl / loud);
        in.centroid = (float) (ce / loud);
        in.peaky = (float) (pk / loud);
        in.low = (float) (lowE / Math.max(1e-12, allE));
        in.high = (float) (highE / Math.max(1e-12, allE));
        in.harm = harmN == 0 ? 0 : harmSum / harmN;
        // onsets: the loudness jumps up clearly
        int ons = 0;
        float floor = db[0];
        for (int f = 1; f < frames; f++) {
            floor = Math.min(floor + 0.6f, db[f]);
            if (db[f] - floor > 8 && db[f] > top - 30) { ons++; floor = db[f]; }
        }
        float secs = frames * hop / (float) sr;
        in.onsets = ons / secs;
        // separate events: loud stretches with quiet between them
        int ev = 0;
        boolean on = false;
        for (int f = 0; f < frames; f++) {
            if (!on && db[f] > top - 12) { ev++; on = true; } else if (on && db[f] < top - 24) on = false;
        }
        in.events = ev;
        // how much the loudness pulses at syllable speed (3–7 Hz: speech) and at a steady beat (music, steps)
        float fr = sr / (float) hop;           // envelope sample rate
        double mean = 0;
        for (float v : db) mean += v;
        mean /= frames;
        double syl = 0, slow = 0, tot = 0, bestBeat = 0;
        for (int k = 1; k < frames / 2; k++) {
            double hz = k * fr / frames, cr = 0, ci = 0;
            for (int f = 0; f < frames; f++) { double a = 2 * Math.PI * k * f / frames; cr += (db[f] - mean) * Math.cos(a); ci -= (db[f] - mean) * Math.sin(a); }
            double p = cr * cr + ci * ci;
            tot += p;
            if (hz >= 3 && hz <= 7) syl += p;
            if (hz < 1) slow += p;
            if (hz >= 0.8 && hz <= 4) bestBeat = Math.max(bestBeat, p);
        }
        in.syllabic = (float) (syl / Math.max(1e-9, tot));
        in.beat = (float) (bestBeat / Math.max(1e-9, tot));
        classify(in, slow / Math.max(1e-9, tot));
        return in;
    }

    static void classify(Info in, double slowShare) {
        List<String> l = in.like;
        boolean noise = in.harm < 0.4f;
        // ---- a one-off effect or something that can loop under a scene
        boolean effect = in.seconds < 4.6f || (in.seconds < 10 && in.range > 10) || (in.events >= 3 && in.silent > 0.15f);
        if (effect) {
            in.kind = "sfx";
            if (in.events >= 4) { l.add("steps"); l.add("knocks"); l.add("ticking"); }
            else if (in.low > 0.55f && in.centroid < 450) { l.add("boom"); l.add("thunder"); l.add("thump"); }
            else if (!noise && in.centroid > 1000 && in.peaky > 4.4f) { l.add("bell"); l.add("chime"); l.add("clang"); }
            else if (noise && in.centroid > 1400) { l.add("whoosh"); l.add("splash"); }
            else if (!noise) { l.add("tone"); l.add("bell"); }
            else l.add("effect");
            return;
        }
        // ---- long sounds
        if (in.syllabic > 0.3f && in.harm >= 0.2f && in.harm <= 0.5f && in.centroid > 500 && in.centroid < 2600) {
            in.kind = "voices";
            l.add("people talking"); l.add("crowd"); l.add("voices");
        } else if (!noise && in.harm > 0.5f && in.centroid > 2800) {
            in.kind = "amb";
            l.add("insects"); l.add("crickets"); l.add("night");
        } else if (in.harm > 0.5f) {
            in.kind = "music";
            l.add("music"); l.add("tones");
        } else {
            in.kind = "amb";
            if (in.onsets > 0.5f && in.range > 10) { l.add("birds"); l.add("chirping"); }
            else if (in.centroid > 3500) { l.add("rain"); l.add("water"); }
            else if (in.centroid > 1800) { l.add("water"); l.add("river"); l.add("waves"); }
            else { l.add("wind"); l.add("rumble"); if (slowShare > 0.5) l.add("storm"); }
        }
    }
}
