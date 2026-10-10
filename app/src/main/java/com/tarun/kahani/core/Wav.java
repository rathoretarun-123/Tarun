package com.tarun.kahani.core;

/** Writes mono 16-bit WAV bytes (for sending a sound to an AI that listens). */
public final class Wav {
    private Wav() {}

    public static byte[] encode16(float[] x, int sr) {
        int n = x.length;
        byte[] b = new byte[44 + n * 2];
        put(b, 0, "RIFF"); le32(b, 4, 36 + n * 2); put(b, 8, "WAVE"); put(b, 12, "fmt ");
        le32(b, 16, 16); le16(b, 20, 1); le16(b, 22, 1); le32(b, 24, sr); le32(b, 28, sr * 2); le16(b, 32, 2); le16(b, 34, 16);
        put(b, 36, "data"); le32(b, 40, n * 2);
        float peak = 1e-6f;
        for (float v : x) peak = Math.max(peak, Math.abs(v));
        float g = peak > 1 ? 1 / peak : 1;
        for (int i = 0; i < n; i++) le16(b, 44 + i * 2, (int) Math.max(-32767, Math.min(32767, x[i] * g * 32767)));
        return b;
    }

    static void put(byte[] b, int at, String s) { for (int i = 0; i < 4; i++) b[at + i] = (byte) s.charAt(i); }
    static void le16(byte[] b, int at, int v) { b[at] = (byte) v; b[at + 1] = (byte) (v >> 8); }
    static void le32(byte[] b, int at, int v) { b[at] = (byte) v; b[at + 1] = (byte) (v >> 8); b[at + 2] = (byte) (v >> 16); b[at + 3] = (byte) (v >> 24); }
}
