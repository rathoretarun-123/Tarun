package com.tarun.kahani.core;

/**
 * The sound of the place: a small stereo room (in the style of the classic "Freeverb": eight damped comb
 * filters and four all-passes per side, slightly different on the left and right so the echo is wide).
 * A cave rings long and dark, a palace hall answers clearly, a courtyard gives a short slap, outdoors stays
 * almost dry. Voices and effects are sent into it; the music is not.
 */
public final class Room {
    static final int[] COMB = {1116, 1188, 1277, 1356, 1422, 1491, 1557, 1617};
    static final int[] AP = {556, 441, 341, 225};
    static final int SPREAD = 23;

    private final float[][] cl = new float[8][], cr = new float[8][];
    private final int[] il = new int[8], ir = new int[8];
    private final float[] fl = new float[8], fr = new float[8];
    private final float[][] al = new float[4][], ar = new float[4][];
    private final int[] jl = new int[4], jr = new int[4];

    /** Current settings (they glide to the target of each place, so scene changes never click). */
    public float feedback = 0.6f, damp = 0.5f, wet = 0f;
    private float tFeedback = 0.6f, tDamp = 0.5f, tWet = 0f;

    public Room(int sr) {
        float k = sr / 44100f;
        for (int i = 0; i < 8; i++) {
            cl[i] = new float[Math.max(8, (int) (COMB[i] * k))];
            cr[i] = new float[Math.max(8, (int) ((COMB[i] + SPREAD) * k))];
        }
        for (int i = 0; i < 4; i++) {
            al[i] = new float[Math.max(4, (int) (AP[i] * k))];
            ar[i] = new float[Math.max(4, (int) ((AP[i] + SPREAD) * k))];
        }
    }

    /** {feedback (size), damping (darkness), wet (how much of the room is heard)} for a part of the film. */
    public static float[] of(Film.Seg s) {
        if (s == null || s.type != Film.S_SCENE) return new float[]{0.6f, 0.5f, 0f};
        switch (s.set) {
            case Sets.CAVE_IN: return new float[]{0.885f, 0.22f, 0.30f};
            case Sets.CAVE_MOUTH: return new float[]{0.84f, 0.3f, 0.19f};
            case Sets.HALL: return new float[]{0.835f, 0.38f, 0.17f};
            case Sets.COURTYARD: case Sets.GATE: return new float[]{0.74f, 0.45f, 0.075f};
            default: return new float[]{0.62f, 0.6f, 0.035f};       // open air: a whisper of reflections
        }
    }

    public void target(float[] p) { tFeedback = p[0]; tDamp = p[1]; tWet = p[2]; }

    /** One sample: in = the mono send; out[0], out[1] = the room's left and right (already scaled by wet). */
    public void process(float in, float[] out) {
        feedback += 0.0004f * (tFeedback - feedback);
        damp += 0.0004f * (tDamp - damp);
        wet += 0.0004f * (tWet - wet);
        if (wet < 1e-4f && tWet < 1e-4f && Math.abs(in) < 1e-6f) {
            // nothing to hear: let the tail ring out quietly without spending time
            out[0] = 0; out[1] = 0;
            return;
        }
        float x = in * 0.02f, sl = 0, sr = 0, d1 = 1 - damp;
        for (int i = 0; i < 8; i++) {
            float[] b = cl[i];
            float y = b[il[i]];
            fl[i] = y * d1 + fl[i] * damp;
            b[il[i]] = x + fl[i] * feedback;
            if (++il[i] >= b.length) il[i] = 0;
            sl += y;
            b = cr[i];
            y = b[ir[i]];
            fr[i] = y * d1 + fr[i] * damp;
            b[ir[i]] = x + fr[i] * feedback;
            if (++ir[i] >= b.length) ir[i] = 0;
            sr += y;
        }
        for (int i = 0; i < 4; i++) {
            float[] b = al[i];
            float v = b[jl[i]];
            b[jl[i]] = sl + v * 0.5f;
            if (++jl[i] >= b.length) jl[i] = 0;
            sl = v - sl;
            b = ar[i];
            v = b[jr[i]];
            b[jr[i]] = sr + v * 0.5f;
            if (++jr[i] >= b.length) jr[i] = 0;
            sr = v - sr;
        }
        out[0] = sl * wet;
        out[1] = sr * wet;
    }
}
