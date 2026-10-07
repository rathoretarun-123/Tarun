package com.tarun.kahani.app;

import android.media.Image;
import android.media.MediaCodec;
import android.media.MediaCodecInfo;
import android.media.MediaFormat;
import android.media.MediaMuxer;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;

/** Encodes ARGB frames + a mono PCM soundtrack into an MP4 (H.264 + AAC) with exact timestamps. */
public final class VideoWriter {
    private final int w, h, fps;
    private MediaCodec video;
    private MediaMuxer muxer;
    private int videoTrack = -1, audioTrack = -1;
    private boolean muxing;
    private final MediaCodec.BufferInfo info = new MediaCodec.BufferInfo();
    private MediaFormat audioFormat;
    private final List<ByteBuffer> audioData = new ArrayList<ByteBuffer>();
    private final List<long[]> audioMeta = new ArrayList<long[]>(); // {pts, flags, size}
    private int audioNext;
    private long frameIndex;
    private byte[] yRow;

    public VideoWriter(int w, int h, int fps) {
        this.w = w; this.h = h; this.fps = fps;
    }

    /** Encodes the whole soundtrack first (fast), then prepares the video encoder. */
    public void start(File out, short[] pcm, int sr) throws IOException { start(out, new ArraySource(pcm), sr, 0.16f); }

    /** Soundtrack source: fills buf with up to buf.length samples, returns how many (0 = end). */
    public interface PcmSource { int read(short[] buf) throws IOException; }

    static final class ArraySource implements PcmSource {
        final short[] a; int pos;
        ArraySource(short[] a) { this.a = a; }
        public int read(short[] buf) { int n = Math.min(buf.length, a.length - pos); System.arraycopy(a, pos, buf, 0, n); pos += n; return n; }
    }

    /** Reads 16-bit little-endian mono PCM from a file. */
    public static final class FileSource implements PcmSource {
        final java.io.InputStream in; final byte[] b = new byte[8192];
        public FileSource(File f) throws IOException { in = new java.io.BufferedInputStream(new java.io.FileInputStream(f), 1 << 16); }
        public int read(short[] buf) throws IOException {
            int want = Math.min(buf.length, b.length / 2) * 2, got = 0;
            while (got < want) { int n = in.read(b, got, want - got); if (n <= 0) break; got += n; }
            for (int i = 0; i < got / 2; i++) buf[i] = (short) ((b[2 * i] & 0xFF) | (b[2 * i + 1] << 8));
            if (got == 0) in.close();
            return got / 2;
        }
    }

    public void start(File out, PcmSource pcm, int sr, float bitsPerPixel) throws IOException {
        encodeAudio(pcm, sr);
        MediaFormat f = MediaFormat.createVideoFormat(MediaFormat.MIMETYPE_VIDEO_AVC, w, h);
        f.setInteger(MediaFormat.KEY_COLOR_FORMAT, MediaCodecInfo.CodecCapabilities.COLOR_FormatYUV420Flexible);
        f.setInteger(MediaFormat.KEY_BIT_RATE, Math.max(1000000, (int) (w * (long) h * fps * bitsPerPixel)));
        f.setInteger(MediaFormat.KEY_FRAME_RATE, fps);
        f.setInteger(MediaFormat.KEY_I_FRAME_INTERVAL, 2);
        video = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
        video.configure(f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        video.start();
        muxer = new MediaMuxer(out.getAbsolutePath(), MediaMuxer.OutputFormat.MUXER_OUTPUT_MPEG_4);
        yRow = new byte[w];
    }

    private void encodeAudio(PcmSource src, int sr) throws IOException {
        MediaFormat f = MediaFormat.createAudioFormat(MediaFormat.MIMETYPE_AUDIO_AAC, sr, 1);
        f.setInteger(MediaFormat.KEY_AAC_PROFILE, MediaCodecInfo.CodecProfileLevel.AACObjectLC);
        f.setInteger(MediaFormat.KEY_BIT_RATE, 128000);
        f.setInteger(MediaFormat.KEY_MAX_INPUT_SIZE, 16384);
        MediaCodec a = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_AUDIO_AAC);
        a.configure(f, null, null, MediaCodec.CONFIGURE_FLAG_ENCODE);
        a.start();
        long pos = 0;
        short[] chunk = new short[4096];
        boolean inputDone = false, outputDone = false;
        MediaCodec.BufferInfo bi = new MediaCodec.BufferInfo();
        long guard = System.currentTimeMillis();
        while (!outputDone) {
            if (!inputDone) {
                int ix = a.dequeueInputBuffer(10000);
                if (ix >= 0) {
                    ByteBuffer buf = a.getInputBuffer(ix);
                    buf.clear();
                    if (chunk.length > buf.capacity() / 2) chunk = new short[buf.capacity() / 2];
                    int samples = src.read(chunk);
                    long pts = pos * 1000000L / sr;
                    if (samples <= 0) {
                        a.queueInputBuffer(ix, 0, 0, pts, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
                        inputDone = true;
                    } else {
                        for (int i = 0; i < samples; i++) {
                            short s = chunk[i];
                            buf.put((byte) (s & 0xFF));
                            buf.put((byte) ((s >> 8) & 0xFF));
                        }
                        a.queueInputBuffer(ix, 0, samples * 2, pts, 0);
                        pos += samples;
                    }
                }
            }
            int ox = a.dequeueOutputBuffer(bi, 10000);
            if (ox == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                audioFormat = a.getOutputFormat();
            } else if (ox >= 0) {
                ByteBuffer ob = a.getOutputBuffer(ox);
                if ((bi.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) == 0 && bi.size > 0 && ob != null) {
                    ByteBuffer copy = ByteBuffer.allocate(bi.size);
                    ob.position(bi.offset);
                    ob.limit(bi.offset + bi.size);
                    copy.put(ob);
                    copy.flip();
                    audioData.add(copy);
                    audioMeta.add(new long[]{bi.presentationTimeUs, bi.flags & ~MediaCodec.BUFFER_FLAG_END_OF_STREAM, bi.size});
                }
                a.releaseOutputBuffer(ox, false);
                if ((bi.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0) outputDone = true;
            }
            if (System.currentTimeMillis() - guard > 900000) throw new IOException("आवाज़ एन्कोड नहीं हो सकी");
        }
        a.stop();
        a.release();
    }

    /** Adds one frame (ARGB pixels, width*height). */
    public void frame(int[] argb) throws IOException {
        long deadline = System.currentTimeMillis() + 10000;
        int ix;
        while ((ix = video.dequeueInputBuffer(5000)) < 0) {
            drain(false);
            if (System.currentTimeMillis() > deadline) throw new IOException("वीडियो एन्कोडर रुक गया");
        }
        Image img = null;
        try { img = video.getInputImage(ix); } catch (Exception ignored) {}
        if (img != null) fill(img, argb);
        else fillNV12(video.getInputBuffer(ix), argb);
        long pts = frameIndex * 1000000L / fps;
        video.queueInputBuffer(ix, 0, w * h * 3 / 2, pts, 0);
        frameIndex++;
        drain(false);
    }

    private void fill(Image img, int[] argb) {
        Image.Plane[] p = img.getPlanes();
        ByteBuffer yb = p[0].getBuffer(), ub = p[1].getBuffer(), vb = p[2].getBuffer();
        int yRs = p[0].getRowStride(), yPs = p[0].getPixelStride();
        int uRs = p[1].getRowStride(), uPs = p[1].getPixelStride();
        int vRs = p[2].getRowStride(), vPs = p[2].getPixelStride();
        for (int y = 0; y < h; y++) {
            int row = y * w;
            if (yPs == 1) {
                for (int x = 0; x < w; x++) {
                    int c = argb[row + x];
                    int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                    yRow[x] = (byte) (((66 * r + 129 * g + 25 * b + 128) >> 8) + 16);
                }
                yb.position(y * yRs);
                yb.put(yRow, 0, w);
            } else {
                for (int x = 0; x < w; x++) {
                    int c = argb[row + x];
                    int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                    yb.put(y * yRs + x * yPs, (byte) (((66 * r + 129 * g + 25 * b + 128) >> 8) + 16));
                }
            }
        }
        int cw = w / 2, ch = h / 2;
        for (int y = 0; y < ch; y++) {
            int row = (y * 2) * w;
            for (int x = 0; x < cw; x++) {
                int c0 = argb[row + x * 2], c1 = argb[row + x * 2 + 1], c2 = argb[row + w + x * 2], c3 = argb[row + w + x * 2 + 1];
                int r = (((c0 >> 16) & 255) + ((c1 >> 16) & 255) + ((c2 >> 16) & 255) + ((c3 >> 16) & 255)) >> 2;
                int g = (((c0 >> 8) & 255) + ((c1 >> 8) & 255) + ((c2 >> 8) & 255) + ((c3 >> 8) & 255)) >> 2;
                int b = ((c0 & 255) + (c1 & 255) + (c2 & 255) + (c3 & 255)) >> 2;
                ub.put(y * uRs + x * uPs, (byte) (((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128));
                vb.put(y * vRs + x * vPs, (byte) (((112 * r - 94 * g - 18 * b + 128) >> 8) + 128));
            }
        }
    }

    /** Fallback for encoders that do not hand out an Image: plain NV12 in the byte buffer. */
    private void fillNV12(ByteBuffer buf, int[] argb) throws IOException {
        if (buf == null) throw new IOException("वीडियो एन्कोडर ने चित्र स्वीकार नहीं किया");
        buf.clear();
        byte[] y = new byte[w * h], uv = new byte[w * h / 2];
        for (int i = 0; i < w * h; i++) {
            int c = argb[i];
            int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
            y[i] = (byte) (((66 * r + 129 * g + 25 * b + 128) >> 8) + 16);
        }
        for (int yy = 0; yy < h / 2; yy++) {
            for (int xx = 0; xx < w / 2; xx++) {
                int c = argb[(yy * 2) * w + xx * 2];
                int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
                uv[yy * w + xx * 2] = (byte) (((-38 * r - 74 * g + 112 * b + 128) >> 8) + 128);
                uv[yy * w + xx * 2 + 1] = (byte) (((112 * r - 94 * g - 18 * b + 128) >> 8) + 128);
            }
        }
        if (buf.capacity() < y.length + uv.length) throw new IOException("वीडियो एन्कोडर का बफ़र छोटा है");
        buf.put(y);
        buf.put(uv);
    }

    private void drain(boolean end) throws IOException {
        long deadline = System.currentTimeMillis() + 15000;
        while (true) {
            int ox = video.dequeueOutputBuffer(info, end ? 10000 : 0);
            if (ox == MediaCodec.INFO_TRY_AGAIN_LATER) {
                if (!end) return;
                if (System.currentTimeMillis() > deadline) throw new IOException("वीडियो पूरा नहीं हो सका");
                continue;
            }
            if (ox == MediaCodec.INFO_OUTPUT_FORMAT_CHANGED) {
                if (muxing) continue;
                videoTrack = muxer.addTrack(video.getOutputFormat());
                if (audioFormat != null) audioTrack = muxer.addTrack(audioFormat);
                muxer.start();
                muxing = true;
                continue;
            }
            if (ox < 0) continue;
            ByteBuffer ob = video.getOutputBuffer(ox);
            if ((info.flags & MediaCodec.BUFFER_FLAG_CODEC_CONFIG) != 0) info.size = 0;
            if (info.size > 0 && muxing && ob != null) {
                writeAudioUpTo(info.presentationTimeUs);
                ob.position(info.offset);
                ob.limit(info.offset + info.size);
                muxer.writeSampleData(videoTrack, ob, info);
            }
            boolean eos = (info.flags & MediaCodec.BUFFER_FLAG_END_OF_STREAM) != 0;
            video.releaseOutputBuffer(ox, false);
            if (eos) return;
        }
    }

    private final MediaCodec.BufferInfo ai = new MediaCodec.BufferInfo();

    private void writeAudioUpTo(long pts) {
        if (audioTrack < 0) return;
        while (audioNext < audioData.size() && audioMeta.get(audioNext)[0] <= pts) {
            long[] m = audioMeta.get(audioNext);
            ByteBuffer b = audioData.get(audioNext);
            ai.set(0, (int) m[2], m[0], (int) m[1]);
            b.position(0);
            muxer.writeSampleData(audioTrack, b, ai);
            audioData.set(audioNext, null);
            audioNext++;
        }
    }

    public void finish() throws IOException {
        long deadline = System.currentTimeMillis() + 10000;
        int ix;
        while ((ix = video.dequeueInputBuffer(5000)) < 0) {
            drain(false);
            if (System.currentTimeMillis() > deadline) break;
        }
        if (ix >= 0) video.queueInputBuffer(ix, 0, 0, frameIndex * 1000000L / fps, MediaCodec.BUFFER_FLAG_END_OF_STREAM);
        drain(true);
        writeAudioUpTo(Long.MAX_VALUE);
        release();
    }

    public void release() {
        try { if (video != null) { video.stop(); video.release(); } } catch (Exception ignored) {}
        video = null;
        try { if (muxer != null) { if (muxing) muxer.stop(); muxer.release(); } } catch (Exception ignored) {}
        muxer = null;
    }

    /** Largest size of the wanted shape this phone's H.264 encoder supports, starting from the wanted size. */
    public static int[] supportedSize(int wantW, int wantH) {
        float[] scales = {1f, 0.75f, 0.6667f, 0.5f, 0.4444f, 0.3333f};
        MediaCodecInfo.VideoCapabilities vc = null;
        try {
            MediaCodec c = MediaCodec.createEncoderByType(MediaFormat.MIMETYPE_VIDEO_AVC);
            vc = c.getCodecInfo().getCapabilitiesForType(MediaFormat.MIMETYPE_VIDEO_AVC).getVideoCapabilities();
            c.release();
        } catch (Exception ignored) {
        }
        for (float s : scales) {
            int ww = Math.round(wantW * s), hh = Math.round(wantH * s);
            ww -= ww % 16; hh -= hh % 16;
            if (ww < 160 || hh < 160) break;
            if (vc == null) return new int[]{ww, hh};
            try { if (vc.isSizeSupported(ww, hh)) return new int[]{ww, hh}; } catch (Exception ignored) {}
        }
        return new int[]{wantW >= wantH ? 1280 : 720, wantW >= wantH ? 720 : 1280};
    }

    public static int[] supportedSize(int wantH) {
        int w = wantH * 16 / 9;
        return supportedSize(w - w % 16, wantH);
    }
}
