package com.tarun.kahani.core;

/**
 * Voice processing used to make generated dialogue sound like a character's voice sample:
 * pitch estimation, pitch shifting that keeps the duration (resample + WSOLA time-stretch),
 * and a tone (timbre) match that copies the average spectrum shape of the sample.
 */
public final class VoiceFx {
    private VoiceFx() {}

    /** Median fundamental frequency (Hz) of voiced parts, by autocorrelation. 0 if unvoiced. */
    public static float medianPitch(float[] x, int sr) {
        int win = sr / 25, hop = sr / 100;
        int minLag = sr / 450, maxLag = sr / 70;
        float[] found = new float[Math.max(1, x.length / hop)];
        int n = 0;
        for (int start = 0; start + win + maxLag < x.length; start += hop) {
            double energy = 0;
            for (int i = 0; i < win; i++) energy += x[start + i] * x[start + i];
            if (energy / win < 1e-4) continue;
            double best = 0;
            int bestLag = 0;
            for (int lag = minLag; lag <= maxLag; lag++) {
                double s = 0, e2 = 0;
                for (int i = 0; i < win; i += 2) { s += x[start + i] * x[start + i + lag]; e2 += x[start + i + lag] * x[start + i + lag]; }
                double c = s / Math.sqrt(energy / 2 * e2 + 1e-12);
                if (c > best) { best = c; bestLag = lag; }
            }
            if (best > 0.55 && bestLag > 0) found[n++] = sr / (float) bestLag;
        }
        if (n == 0) return 0;
        java.util.Arrays.sort(found, 0, n);
        return found[n / 2];
    }

    /** Changes length without changing pitch (WSOLA). factor > 1 = longer/slower. */
    public static float[] stretch(float[] x, float factor) {
        if (Math.abs(factor - 1) < 0.01f || x.length < 2048) return x;
        int frame = 1024, overlap = 512, hopOut = frame - overlap;
        int hopIn = Math.max(1, Math.round(hopOut / factor));
        int tol = 256;
        int outLen = (int) (x.length * factor) + frame;
        float[] out = new float[outLen];
        float[] win = new float[frame];
        for (int i = 0; i < frame; i++) win[i] = (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / (frame - 1)));
        int inPos = 0, outPos = 0, prev = 0;
        while (inPos + frame + tol < x.length && outPos + frame < outLen) {
            int bestOff = 0;
            if (outPos > 0) {
                double best = -1e18;
                for (int off = -tol; off <= tol; off += 4) {
                    int p = inPos + off;
                    if (p < 0) continue;
                    double c = 0;
                    for (int i = 0; i < overlap; i += 4) c += x[p + i] * x[prev + hopOut + i < x.length ? prev + hopOut + i : x.length - 1];
                    if (c > best) { best = c; bestOff = off; }
                }
            }
            int p = Math.max(0, inPos + bestOff);
            for (int i = 0; i < frame && p + i < x.length; i++) out[outPos + i] += x[p + i] * win[i];
            prev = p;
            inPos += hopIn;
            outPos += hopOut;
        }
        int n = Math.min(outLen, (int) (x.length * factor));
        float[] o = new float[n];
        System.arraycopy(out, 0, o, 0, n);
        return o;
    }

    /** Shifts pitch by a factor (1.2 = higher) keeping the same duration. */
    public static float[] pitch(float[] x, float factor) {
        if (Math.abs(factor - 1) < 0.01f) return x;
        float[] s = stretch(x, factor);           // longer by factor
        int n = x.length;
        float[] o = new float[n];
        for (int i = 0; i < n; i++) {             // then play faster -> original length, pitch * factor
            float p = i * factor;
            int a = (int) p;
            float f = p - a;
            float x0 = a < s.length ? s[a] : 0, x1 = a + 1 < s.length ? s[a + 1] : 0;
            o[i] = x0 + (x1 - x0) * f;
        }
        return o;
    }

    // ---------------------------------------------------------------- timbre matching

    static final int BANDS = 24;

    /** Average energy in log-spaced bands (a "tone fingerprint" of a voice). */
    public static float[] toneProfile(float[] x, int sr) {
        float[] bands = new float[BANDS];
        int n = 1024;
        float[] re = new float[n], im = new float[n];
        int frames = 0;
        for (int start = 0; start + n < x.length; start += n / 2) {
            double e = 0;
            for (int i = 0; i < n; i++) e += x[start + i] * x[start + i];
            if (e / n < 1e-4) continue;
            for (int i = 0; i < n; i++) { re[i] = x[start + i] * (float) (0.5 - 0.5 * Math.cos(2 * Math.PI * i / n)); im[i] = 0; }
            fft(re, im);
            for (int k = 1; k < n / 2; k++) {
                float f = k * sr / (float) n;
                int b = band(f);
                if (b >= 0) bands[b] += re[k] * re[k] + im[k] * im[k];
            }
            frames++;
        }
        if (frames == 0) return null;
        for (int b = 0; b < BANDS; b++) bands[b] = (float) Math.log10(bands[b] / frames + 1e-9);
        return bands;
    }

    static int band(float f) {
        if (f < 80 || f > 8000) return -1;
        double t = Math.log(f / 80.0) / Math.log(8000 / 80.0);
        return Math.min(BANDS - 1, (int) (t * BANDS));
    }

    /** Equalises x so its average tone moves toward the target profile (strength 0..1, limited to ±9 dB). */
    public static float[] matchTone(float[] x, int sr, float[] target, float strength) {
        float[] src = toneProfile(x, sr);
        if (src == null || target == null) return x;
        float[] gainDb = new float[BANDS];
        float mean = 0;
        for (int b = 0; b < BANDS; b++) { gainDb[b] = (target[b] - src[b]) * 10f; mean += gainDb[b]; }
        mean /= BANDS;
        for (int b = 0; b < BANDS; b++) gainDb[b] = Math.max(-9, Math.min(9, (gainDb[b] - mean) * strength));
        // smooth across bands
        float[] sm = new float[BANDS];
        for (int b = 0; b < BANDS; b++) {
            float s = gainDb[b] * 2, w = 2;
            if (b > 0) { s += gainDb[b - 1]; w++; }
            if (b < BANDS - 1) { s += gainDb[b + 1]; w++; }
            sm[b] = s / w;
        }
        int n = 1024, hop = n / 2;
        float[] out = new float[x.length + n];
        float[] re = new float[n], im = new float[n];
        float[] win = new float[n];
        for (int i = 0; i < n; i++) win[i] = (float) Math.sqrt(0.5 - 0.5 * Math.cos(2 * Math.PI * i / n));
        float[] g = new float[n / 2 + 1];
        for (int k = 0; k <= n / 2; k++) {
            int b = band(k * sr / (float) n);
            g[k] = b < 0 ? 1f : (float) Math.pow(10, sm[b] / 20);
        }
        for (int start = 0; start < x.length; start += hop) {
            for (int i = 0; i < n; i++) { re[i] = (start + i < x.length ? x[start + i] : 0) * win[i]; im[i] = 0; }
            fft(re, im);
            for (int k = 0; k <= n / 2; k++) {
                re[k] *= g[k]; im[k] *= g[k];
                if (k > 0 && k < n / 2) { re[n - k] = re[k]; im[n - k] = -im[k]; }
            }
            ifft(re, im);
            for (int i = 0; i < n && start + i < out.length; i++) out[start + i] += re[i] * win[i];
        }
        float[] o = new float[x.length];
        System.arraycopy(out, 0, o, 0, x.length);
        // keep loudness
        double e0 = 0, e1 = 0;
        for (int i = 0; i < x.length; i++) { e0 += x[i] * x[i]; e1 += o[i] * o[i]; }
        float k = e1 > 0 ? (float) Math.sqrt(e0 / e1) : 1;
        for (int i = 0; i < o.length; i++) o[i] *= k;
        return o;
    }

    /** What we learn from a voice sample once: its median pitch and tone colour. */
    public static final class Profile {
        public float pitch;
        public float[] tone;
    }

    /** Analyses (at most the first 40 seconds of) a voice sample. */
    public static Profile profile(float[] sample, int sr) {
        float[] s = sample;
        if (s.length > sr * 40) s = java.util.Arrays.copyOf(s, sr * 40);
        Profile p = new Profile();
        p.pitch = medianPitch(s, sr);
        p.tone = toneProfile(s, sr);
        return p;
    }

    /** Makes generated speech closer to a voice sample: same median pitch and similar tone colour. */
    public static float[] matchVoice(float[] speech, float[] sample, int sr) { return matchVoice(speech, profile(sample, sr), sr); }

    public static float[] matchVoice(float[] speech, Profile target, int sr) {
        if (target == null) return speech;
        float p0 = medianPitch(speech, sr);
        float[] y = speech;
        if (p0 > 0 && target.pitch > 0) {
            // beyond about ±0.8 octave any pitch shifter sounds artificial, so stay within that
            float f = Math.max(0.6f, Math.min(1.8f, target.pitch / p0));
            y = pitch(y, f);
        }
        return matchTone(y, sr, target.tone, 0.6f);
    }

    // ---------------------------------------------------------------- FFT (radix 2, in place)

    static void fft(float[] re, float[] im) {
        int n = re.length;
        for (int i = 1, j = 0; i < n; i++) {
            int bit = n >> 1;
            for (; (j & bit) != 0; bit >>= 1) j ^= bit;
            j ^= bit;
            if (i < j) { float t = re[i]; re[i] = re[j]; re[j] = t; t = im[i]; im[i] = im[j]; im[j] = t; }
        }
        for (int len = 2; len <= n; len <<= 1) {
            double ang = -2 * Math.PI / len;
            float wr = (float) Math.cos(ang), wi = (float) Math.sin(ang);
            for (int i = 0; i < n; i += len) {
                float cr = 1, ci = 0;
                for (int k = 0; k < len / 2; k++) {
                    int a = i + k, b = i + k + len / 2;
                    float xr = re[b] * cr - im[b] * ci, xi = re[b] * ci + im[b] * cr;
                    re[b] = re[a] - xr; im[b] = im[a] - xi;
                    re[a] += xr; im[a] += xi;
                    float nr = cr * wr - ci * wi;
                    ci = cr * wi + ci * wr;
                    cr = nr;
                }
            }
        }
    }

    static void ifft(float[] re, float[] im) {
        for (int i = 0; i < im.length; i++) im[i] = -im[i];
        fft(re, im);
        int n = re.length;
        for (int i = 0; i < n; i++) { re[i] /= n; im[i] = -im[i] / n; }
    }
}
