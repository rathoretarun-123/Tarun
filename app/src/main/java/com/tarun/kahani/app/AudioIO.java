package com.tarun.kahani.app;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.AudioFormat;
import android.media.AudioRecord;
import android.media.MediaCodec;
import android.media.MediaExtractor;
import android.media.MediaFormat;
import android.media.MediaRecorder;

import com.tarun.kahani.core.Mixer;
import com.tarun.kahani.core.SoundLib;
import com.tarun.kahani.core.Synth;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;

/** Sound files in and out: decode any phone audio (mp3, m4a, ogg, wav…) to mono floats, write WAV, record the mic. */
public final class AudioIO {
    private AudioIO() {}

    /** Decodes an asset ("asset:sounds/x.ogg") or a file path to mono floats at Synth.SR; null when it cannot. */
    public static float[] decode(Context c, String path) { return decode(c, path, 180); }

    /** Same, but never more than maxSeconds (long files would only waste memory: sounds are looped anyway). */
    public static float[] decode(Context c, String path, int maxSeconds) {
        MediaExtractor ex = new MediaExtractor();
        MediaCodec dec = null;
        try {
            if (path.startsWith("asset:")) {
                AssetFileDescriptor fd = c.getAssets().openFd(path.substring(6));
                ex.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                fd.close();
            } else {
                if (path.toLowerCase().endsWith(".wav") && new File(path).length() < 64L << 20) {
                    float[] w = Voices.readWav(new File(path));
                    if (w != null) return w.length > maxSeconds * Synth.SR ? java.util.Arrays.copyOf(w, maxSeconds * Synth.SR) : w;
                }
                ex.setDataSource(path);
            }
            int track = -1;
            MediaFormat fmt = null;
            for (int i = 0; i < ex.getTrackCount(); i++) {
                MediaFormat f = ex.getTrackFormat(i);
                String mime = f.getString(MediaFormat.KEY_MIME);
                if (mime != null && mime.startsWith("audio/")) { track = i; fmt = f; break; }
            }
            if (track < 0) return null;
            ex.selectTrack(track);
            int sr = fmt.getInteger(MediaFormat.KEY_SAMPLE_RATE);
            int ch = fmt.containsKey(MediaFormat.KEY_CHANNEL_COUNT) ? fmt.getInteger(MediaFormat.KEY_CHANNEL_COUNT) : 1;
            long durUs = fmt.containsKey(MediaFormat.KEY_DURATION) ? fmt.getLong(MediaFormat.KEY_DURATION) : 0;
            long maxUs = maxSeconds * 1000000L;
            if (durUs <= 0 || durUs > maxUs) durUs = maxUs;
            dec = MediaCodec.createDecoderByType(fmt.getString(MediaFormat.KEY_MIME));
            dec.configure(fmt, null, null, 0);
            dec.start();
            int cap = (int) (durUs * sr / 1000000L) + sr;
            float[] out = new float[cap];
            int n = 0;
            MediaCodec.BufferInfo bi = new MediaCodec.BufferInfo();
            boolean inDone = false, outDone = false;
            long guard = System.currentTimeMillis();
            int outCh = ch;
            while (!outDone) {
                if (!inDone) {
                    int ix = dec.dequeueInputBuffer(10000);
                    if (ix >= 0) {
                        ByteBuffer b = dec.getInputBuffer(ix);
                        int sz = b == null ? -1 : ex.readSampleData(b, 0);
                        if (sz < 0 || (durUs > 0 && ex.getSampleTime() > durUs)) {
                            dec.queueInputBuffer(ix, 0, 0, 0, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                            inDone = true;
                        } else {
                            dec.queueInputBuffer(ix, 0, sz, ex.getSampleTime(), 0);
                            ex.advance();
                        }
                    }
                }
                int ox = dec.dequeueOutputBuffer(bi, 10000);
                if (ox == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                    MediaFormat of = dec.getOutputFormat();
                    if (of.containsKey(MediaFormat.KEY_CHANNEL_COUNT)) outCh = of.getInteger(MediaFormat.KEY_CHANNEL_COUNT);
                    if (of.containsKey(MediaFormat.KEY_SAMPLE_RATE)) sr = of.getInteger(MediaFormat.KEY_SAMPLE_RATE);
                } else if (ox >= 0) {
                    ByteBuffer ob = dec.getOutputBuffer(ox);
                    if (ob != null && bi.size > 0) {
                        ob.position(bi.offset);
                        ob.limit(bi.offset + bi.size);
                        java.nio.ShortBuffer sb = ob.order(ByteOrder.nativeOrder()).asShortBuffer();
                        int frames = sb.remaining() / Math.max(1, outCh);
                        frames = Math.min(frames, out.length - n);
                        for (int i = 0; i < frames; i++) {
                            float s = 0;
                            for (int k = 0; k < outCh; k++) s += sb.get(i * outCh + k);
                            out[n++] = s / outCh / 32768f;
                        }
                    }
                    dec.releaseOutputBuffer(ox, false);
                    if ((bi.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outDone = true;
                }
                if (System.currentTimeMillis() - guard > 120000) break;
            }
            float[] o = java.util.Arrays.copyOf(out, n);
            return n == 0 ? null : Mixer.resample(o, sr);
        } catch (Throwable e) {
            return null;
        } finally {
            try { if (dec != null) { dec.stop(); dec.release(); } } catch (Throwable ignored) {}
            ex.release();
        }
    }

    public static SoundLib.Decoder decoder(final Context c) {
        return new SoundLib.Decoder() {
            public float[] decode(String path) { return AudioIO.decode(c, path); }
        };
    }

    // ------------------------------------------------------------------ WAV and raw PCM files

    public static void writeWav(File f, float[] pcm, int sr) throws IOException {
        OutputStream o = new java.io.BufferedOutputStream(new FileOutputStream(f), 65536);
        int n = pcm.length;
        ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
        h.put("RIFF".getBytes("US-ASCII")).putInt(36 + n * 2).put("WAVEfmt ".getBytes("US-ASCII")).putInt(16).putShort((short) 1)
                .putShort((short) 1).putInt(sr).putInt(sr * 2).putShort((short) 2).putShort((short) 16).put("data".getBytes("US-ASCII")).putInt(n * 2);
        o.write(h.array());
        byte[] buf = new byte[8192];
        int k = 0;
        for (int i = 0; i < n; i++) {
            float v = pcm[i];
            int s = (int) (Math.max(-1f, Math.min(1f, v)) * 32767);
            buf[k++] = (byte) s;
            buf[k++] = (byte) (s >> 8);
            if (k == buf.length) { o.write(buf); k = 0; }
        }
        o.write(buf, 0, k);
        o.close();
    }

    /** Stores floats as 16-bit little-endian raw (half the size of floats). */
    public static void writeRaw(File f, float[] pcm) throws IOException { writeWav(f, pcm, Synth.SR); }

    public static float[] readRaw(File f) {
        try {
            return f.exists() ? Voices.readWav(f) : null;
        } catch (IOException e) {
            return null;
        }
    }

    // ------------------------------------------------------------------ microphone

    /** Records from the microphone into a WAV file until stop() is called. */
    public static final class Recorder implements Runnable {
        private final File out;
        private volatile boolean running;
        private Thread thread;
        public volatile float level;       // 0..1 for the meter
        public volatile long startedAt;
        public volatile String error;
        static final int SR = 32000;

        public Recorder(File out) { this.out = out; }

        public void start() {
            running = true;
            startedAt = System.currentTimeMillis();
            thread = new Thread(this, "recorder");
            thread.start();
        }

        public void stop() {
            running = false;
            if (thread != null) try { thread.join(3000); } catch (InterruptedException ignored) {}
        }

        public void run() {
            AudioRecord rec = null;
            RandomAccessFile raf = null;
            try {
                int min = AudioRecord.getMinBufferSize(SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT);
                rec = new AudioRecord(MediaRecorder.AudioSource.MIC, SR, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, Math.max(min, SR));
                if (rec.getState() != AudioRecord.STATE_INITIALIZED) throw new IOException("माइक नहीं खुला");
                raf = new RandomAccessFile(out, "rw");
                raf.setLength(0);
                raf.write(new byte[44]);
                short[] buf = new short[SR / 10];
                byte[] bytes = new byte[buf.length * 2];
                long total = 0;
                rec.startRecording();
                while (running) {
                    int n = rec.read(buf, 0, buf.length);
                    if (n <= 0) continue;
                    int peak = 0;
                    for (int i = 0; i < n; i++) {
                        short s = buf[i];
                        bytes[2 * i] = (byte) s;
                        bytes[2 * i + 1] = (byte) (s >> 8);
                        peak = Math.max(peak, Math.abs(s));
                    }
                    level = peak / 32768f;
                    raf.write(bytes, 0, n * 2);
                    total += n * 2;
                    if (total > SR * 2L * 600) break;   // 10 minutes max per recording
                }
                ByteBuffer h = ByteBuffer.allocate(44).order(ByteOrder.LITTLE_ENDIAN);
                h.put("RIFF".getBytes("US-ASCII")).putInt((int) (36 + total)).put("WAVEfmt ".getBytes("US-ASCII")).putInt(16).putShort((short) 1)
                        .putShort((short) 1).putInt(SR).putInt(SR * 2).putShort((short) 2).putShort((short) 16).put("data".getBytes("US-ASCII")).putInt((int) total);
                raf.seek(0);
                raf.write(h.array());
            } catch (Throwable e) {
                error = e.getMessage() == null ? e.toString() : e.getMessage();
            } finally {
                try { if (rec != null) { rec.stop(); rec.release(); } } catch (Throwable ignored) {}
                try { if (raf != null) raf.close(); } catch (IOException ignored) {}
                running = false;
            }
        }

        public boolean isRunning() { return running; }
    }

    static byte[] readFile(File f) throws IOException {
        byte[] b = new byte[(int) f.length()];
        DataInputStream in = new DataInputStream(new FileInputStream(f));
        in.readFully(b);
        in.close();
        return b;
    }
}
