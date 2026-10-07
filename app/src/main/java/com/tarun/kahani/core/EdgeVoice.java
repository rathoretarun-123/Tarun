package com.tarun.kahani.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;
import java.util.UUID;

/**
 * Natural neural voices (Microsoft Edge "Read aloud" service): free, no key, very natural Hindi and English.
 * Talks the same WebSocket protocol as the Edge browser. Pure Java (own tiny WebSocket client) so it runs on
 * every Android version and can be tested on a desktop. Returns MP3 bytes (24 kHz mono).
 */
public final class EdgeVoice {
    public static final String TOKEN = "6A5AA1D4EAFF4E9FB37E23D68491D6F4";
    public String host = "speech.platform.bing.com";
    public int port = 443;
    public boolean tls = true;
    public String chromium = "143.0.3650.75";
    public int timeoutMs = 30000;
    /** Seconds to add to the phone clock (corrected from the server's Date when the token is refused). */
    public static volatile double clockSkew;

    /** Voices used for Hindi and English stories (the free service's Indian voices + child-like English voices). */
    public static final String[] HINDI_FEMALE = {"hi-IN-SwaraNeural"}, HINDI_MALE = {"hi-IN-MadhurNeural"};
    public static final String[] ENGLISH_FEMALE = {"en-IN-NeerjaNeural", "en-GB-SoniaNeural", "en-US-JennyNeural"},
            ENGLISH_MALE = {"en-IN-PrabhatNeural", "en-GB-RyanNeural", "en-US-GuyNeural"},
            ENGLISH_CHILD = {"en-US-AnaNeural", "en-GB-MaisieNeural"};

    /** One character's voice: which neural voice, plus pitch (Hz offset) and speed (%) adjustments. */
    public static final class Cast {
        public String voice;
        public int pitchHz, ratePct;
        public Cast(String v, int p, int r) { voice = v; pitchHz = p; ratePct = r; }
        public String key() { return voice + "|" + pitchHz + "|" + ratePct; }
        public static Cast parse(String s) {
            if (s == null) return null;
            String[] f = s.split("\\|");
            if (f.length < 3) return null;
            try { return new Cast(f[0], Integer.parseInt(f[1]), Integer.parseInt(f[2])); } catch (NumberFormatException e) { return null; }
        }
    }

    /** A sensible voice for a character; index varies adults of the same kind so they don't sound identical. */
    public static Cast castFor(Look l, int age, boolean hindi, int index) {
        int v = (index * 7) % 5 - 2;                  // -2..2 small personal variation
        String f = hindi ? HINDI_FEMALE[0] : ENGLISH_FEMALE[Math.abs(index) % ENGLISH_FEMALE.length];
        String m = hindi ? HINDI_MALE[0] : ENGLISH_MALE[Math.abs(index) % ENGLISH_MALE.length];
        if (l == null) return new Cast(m, 0, 0);
        switch (l.kind) {
            case Look.GIRL:
                if (!hindi) return new Cast(ENGLISH_CHILD[Math.abs(index) % ENGLISH_CHILD.length], v * 2, 0);
                return new Cast(f, age > 0 && age <= 8 ? 38 : 24, age > 0 && age <= 8 ? 6 : 3);
            case Look.BOY:
                if (!hindi) return new Cast(ENGLISH_CHILD[0], -6 + v * 2, 2);
                return new Cast(m, age > 0 && age <= 8 ? 48 : 30, 6);
            case Look.WOMAN: return new Cast(f, v * 4, v);
            case Look.WITCH: return new Cast(f, -14, -8);
            case Look.MONSTER: return new Cast(m, -32, -12);
            case Look.OLD_MAN: return new Cast(m, -16, -12);
            case Look.MONKEY: return new Cast(m, 60, 14);
            case Look.ANIMAL: case Look.BIRD:
                return l.height < 0.4f ? new Cast(f, 45, 10) : l.height < 0.8f ? new Cast(l.female ? f : m, 10, 0) : new Cast(m, -20, -8);
            default:
                return new Cast(l.female ? f : m, l.girth > 1.1f ? -10 + v * 3 : v * 5, v * 2);
        }
    }

    /** Choices offered when the user taps 🔊 again: the suggested voice first, then variations. */
    public static List<Cast> options(Look l, int age, boolean hindi, int index) {
        List<Cast> out = new ArrayList<Cast>();
        Cast base = castFor(l, age, hindi, index);
        out.add(base);
        int[][] var = {{14, 0}, {-14, 0}, {28, 6}, {-26, -8}, {0, 10}, {0, -10}};
        for (int[] v : var) out.add(new Cast(base.voice, base.pitchHz + v[0], base.ratePct + v[1]));
        String[] other = hindi ? (base.voice.equals(HINDI_FEMALE[0]) ? HINDI_MALE : HINDI_FEMALE)
                : (l != null && (l.female || l.kind == Look.GIRL) ? ENGLISH_FEMALE : ENGLISH_MALE);
        for (String o : other) if (!o.equals(base.voice)) out.add(new Cast(o, base.pitchHz, base.ratePct));
        return out;
    }

    /** Neutral voice that fits a voice sample (by its pitch), so matching the sample needs only a small change. */
    public static Cast forSample(float samplePitch, boolean hindi) {
        boolean high = samplePitch > 165;
        if (hindi) return new Cast(high ? HINDI_FEMALE[0] : HINDI_MALE[0], 0, 0);
        return new Cast(high ? ENGLISH_FEMALE[0] : ENGLISH_MALE[0], 0, 0);
    }

    public static Cast narrator(boolean hindi) { return new Cast(hindi ? HINDI_MALE[0] : ENGLISH_MALE[0], -4, -6); }

    /** Emotion -> extra speed (%) and pitch (Hz). */
    public static int[] emotion(int e) {
        switch (e) {
            case Pose.ANGRY: return new int[]{8, -6};
            case Pose.SAD: return new int[]{-12, -8};
            case Pose.SCARED: return new int[]{10, 10};
            case Pose.LAUGH: return new int[]{5, 12};
            case Pose.HAPPY: return new int[]{3, 8};
            case Pose.SURPRISED: return new int[]{4, 14};
            case Pose.WHISPER: return new int[]{-10, -4};
            case Pose.PAIN: return new int[]{8, 10};
            case Pose.EVIL: return new int[]{-8, -8};
            case Pose.PROUD: return new int[]{-4, -2};
            case Pose.DETERMINED: return new int[]{2, -2};
            default: return new int[]{0, 0};
        }
    }

    // ------------------------------------------------------------------ token and messages

    /** The Sec-MS-GEC token: SHA-256 of the 5-minute-rounded Windows file time + client token. */
    public static String secMsGec(double unixSeconds) {
        double t = unixSeconds + 11644473600.0;
        t -= t % 300;
        long ticks = (long) t * 10000000L;
        try {
            byte[] d = MessageDigest.getInstance("SHA-256").digest((ticks + TOKEN).getBytes("US-ASCII"));
            StringBuilder b = new StringBuilder();
            for (byte x : d) b.append(String.format("%02X", x & 255));
            return b.toString();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    static String jsDate() {
        SimpleDateFormat f = new SimpleDateFormat("EEE MMM dd yyyy HH:mm:ss", Locale.US);
        f.setTimeZone(TimeZone.getTimeZone("UTC"));
        return f.format(new java.util.Date()) + " GMT+0000 (Coordinated Universal Time)";
    }

    static String id() { return UUID.randomUUID().toString().replace("-", ""); }

    public static String longName(String shortName) {
        // hi-IN-SwaraNeural -> Microsoft Server Speech Text to Speech Voice (hi-IN, SwaraNeural)
        int a = shortName.indexOf('-'), b = a < 0 ? -1 : shortName.indexOf('-', a + 1);
        if (b < 0) return shortName;
        return "Microsoft Server Speech Text to Speech Voice (" + shortName.substring(0, b) + ", " + shortName.substring(b + 1) + ")";
    }

    static String xml(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '&') b.append("&amp;");
            else if (c == '<') b.append("&lt;");
            else if (c == '>') b.append("&gt;");
            else if (c == '"') b.append("&quot;");
            else if (c == '\'') b.append("&apos;");
            else if (c < 32 && c != '\n') b.append(' ');
            else b.append(c);
        }
        return b.toString();
    }

    public static String ssml(String text, String voice, int ratePct, int pitchHz, int volumePct) {
        return "<speak version='1.0' xmlns='http://www.w3.org/2001/10/synthesis' xml:lang='en-US'>"
                + "<voice name='" + longName(voice) + "'>"
                + "<prosody pitch='" + sign(pitchHz) + "Hz' rate='" + sign(ratePct) + "%' volume='" + sign(volumePct) + "%'>"
                + xml(text) + "</prosody></voice></speak>";
    }

    static String sign(int v) { return v >= 0 ? "+" + v : String.valueOf(v); }

    // ------------------------------------------------------------------ speaking

    /** Speaks text; returns MP3 bytes. Retries once after fixing the clock if the service refuses the token. */
    public byte[] speak(String text, Cast c, int[] emo) throws IOException {
        int rate = c.ratePct + (emo == null ? 0 : emo[0]), pitch = c.pitchHz + (emo == null ? 0 : emo[1]);
        rate = Math.max(-50, Math.min(100, rate));
        String s = ssml(text, c.voice, rate, pitch, 0);
        try {
            return speakSsml(s);
        } catch (HandshakeError e) {
            if (e.code == 403 && e.serverDate > 0) {
                clockSkew = e.serverDate - System.currentTimeMillis() / 1000.0;
                return speakSsml(s);
            }
            throw e;
        }
    }

    static final class HandshakeError extends IOException {
        final int code;
        final double serverDate;
        HandshakeError(int code, double serverDate, String msg) { super(msg); this.code = code; this.serverDate = serverDate; }
    }

    public byte[] speakSsml(String ssml) throws IOException {
        Socket sock = null;
        try {
            sock = open();
            OutputStream out = sock.getOutputStream();
            InputStream in = new java.io.BufferedInputStream(sock.getInputStream(), 65536);
            handshake(out, in);
            sendText(out, "X-Timestamp:" + jsDate() + "\r\nContent-Type:application/json; charset=utf-8\r\nPath:speech.config\r\n\r\n"
                    + "{\"context\":{\"synthesis\":{\"audio\":{\"metadataoptions\":{\"sentenceBoundaryEnabled\":\"false\",\"wordBoundaryEnabled\":\"false\"},"
                    + "\"outputFormat\":\"audio-24khz-48kbitrate-mono-mp3\"}}}}\r\n");
            sendText(out, "X-RequestId:" + id() + "\r\nContent-Type:application/ssml+xml\r\nX-Timestamp:" + jsDate() + "Z\r\nPath:ssml\r\n\r\n" + ssml);
            ByteArrayOutputStream audio = new ByteArrayOutputStream();
            while (true) {
                Frame f = readFrame(in, out);
                if (f == null) break;
                if (f.opcode == 1) {
                    String t = new String(f.data, "UTF-8");
                    if (t.contains("Path:turn.end")) break;
                } else if (f.opcode == 2) {
                    if (f.data.length < 2) continue;
                    int hl = ((f.data[0] & 255) << 8) | (f.data[1] & 255);
                    if (hl + 2 > f.data.length) continue;
                    String head = new String(f.data, 2, hl, "US-ASCII");
                    if (head.contains("Path:audio") && head.contains("audio/mpeg")) audio.write(f.data, 2 + hl, f.data.length - 2 - hl);
                } else if (f.opcode == 8) {
                    break;
                }
            }
            if (audio.size() == 0) throw new IOException("voice service sent no audio");
            try { sendClose(out); } catch (IOException ignored) {}
            return audio.toByteArray();
        } finally {
            if (sock != null) try { sock.close(); } catch (IOException ignored) {}
        }
    }

    private Socket open() throws IOException {
        Socket s = new Socket();
        s.connect(new InetSocketAddress(host, port), 15000);
        s.setSoTimeout(timeoutMs);
        if (!tls) return s;
        javax.net.ssl.SSLSocket ss = (javax.net.ssl.SSLSocket) ((javax.net.ssl.SSLSocketFactory) javax.net.ssl.SSLSocketFactory.getDefault())
                .createSocket(s, host, port, true);
        ss.setSoTimeout(timeoutMs);
        ss.startHandshake();
        if (!javax.net.ssl.HttpsURLConnection.getDefaultHostnameVerifier().verify(host, ss.getSession())) {
            ss.close();
            throw new IOException("voice service certificate does not match " + host);
        }
        return ss;
    }

    private void handshake(OutputStream out, InputStream in) throws IOException {
        byte[] nonce = new byte[16];
        new SecureRandom().nextBytes(nonce);
        String key = Cloud.b64(nonce);
        String major = chromium.split("\\.")[0];
        double now = System.currentTimeMillis() / 1000.0 + clockSkew;
        String path = "/consumer/speech/synthesize/readaloud/edge/v1?TrustedClientToken=" + TOKEN + "&ConnectionId=" + id()
                + "&Sec-MS-GEC=" + secMsGec(now) + "&Sec-MS-GEC-Version=1-" + chromium;
        String req = "GET " + path + " HTTP/1.1\r\n"
                + "Host: " + host + "\r\n"
                + "Upgrade: websocket\r\n"
                + "Connection: Upgrade\r\n"
                + "Sec-WebSocket-Key: " + key + "\r\n"
                + "Sec-WebSocket-Version: 13\r\n"
                + "Pragma: no-cache\r\n"
                + "Cache-Control: no-cache\r\n"
                + "Origin: chrome-extension://jdiccldimpdaibmpdkjnbmckianbfold\r\n"
                + "User-Agent: Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/" + major
                + ".0.0.0 Safari/537.36 Edg/" + major + ".0.0.0\r\n"
                + "Accept-Language: en-US,en;q=0.9\r\n"
                + "Cookie: muid=" + id().toUpperCase(Locale.US) + ";\r\n"
                + "\r\n";
        out.write(req.getBytes("UTF-8"));
        out.flush();
        // read the HTTP response head
        StringBuilder head = new StringBuilder();
        int c, crlf = 0;
        while ((c = in.read()) >= 0) {
            head.append((char) c);
            crlf = (c == '\r' || c == '\n') ? crlf + 1 : 0;
            if (crlf == 4) break;
            if (head.length() > 16384) throw new IOException("bad handshake");
        }
        String h = head.toString();
        int code = 0;
        try { code = Integer.parseInt(h.substring(9, 12)); } catch (Exception ignored) {}
        if (code != 101) {
            double date = 0;
            for (String line : h.split("\r\n")) {
                if (line.toLowerCase(Locale.US).startsWith("date:")) {
                    try {
                        SimpleDateFormat f = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
                        date = f.parse(line.substring(5).trim()).getTime() / 1000.0;
                    } catch (Exception ignored) {}
                }
            }
            throw new HandshakeError(code, date, "voice service refused (" + (h.length() > 12 ? h.substring(0, Math.min(h.length(), 40)).trim() : "no reply") + ")");
        }
    }

    // ------------------------------------------------------------------ minimal WebSocket framing (RFC 6455)

    static final class Frame {
        int opcode;
        byte[] data;
    }

    private final SecureRandom rnd = new SecureRandom();

    void sendText(OutputStream out, String s) throws IOException { send(out, 1, s.getBytes("UTF-8")); }

    void sendClose(OutputStream out) throws IOException { send(out, 8, new byte[]{3, (byte) 232}); }

    void send(OutputStream out, int opcode, byte[] p) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream(p.length + 14);
        b.write(0x80 | opcode);
        int n = p.length;
        if (n < 126) b.write(0x80 | n);
        else if (n < 65536) { b.write(0x80 | 126); b.write(n >> 8); b.write(n); }
        else { b.write(0x80 | 127); for (int i = 7; i >= 0; i--) b.write((int) ((long) n >> (8 * i))); }
        byte[] mask = new byte[4];
        rnd.nextBytes(mask);
        b.write(mask);
        for (int i = 0; i < n; i++) b.write(p[i] ^ mask[i & 3]);
        out.write(b.toByteArray());
        out.flush();
    }

    /** Reads one whole message (joining continuation frames); answers pings. null at end of stream. */
    Frame readFrame(InputStream in, OutputStream out) throws IOException {
        ByteArrayOutputStream msg = null;
        int msgOp = 0;
        while (true) {
            int b0 = in.read();
            if (b0 < 0) return null;
            int b1 = in.read();
            if (b1 < 0) return null;
            boolean fin = (b0 & 0x80) != 0;
            int op = b0 & 0x0F;
            long len = b1 & 0x7F;
            if (len == 126) len = (readByte(in) << 8) | readByte(in);
            else if (len == 127) { len = 0; for (int i = 0; i < 8; i++) len = (len << 8) | readByte(in); }
            if (len > (64 << 20)) throw new IOException("frame too large");
            byte[] mask = null;
            if ((b1 & 0x80) != 0) { mask = new byte[4]; readFully(in, mask); }
            byte[] data = new byte[(int) len];
            readFully(in, data);
            if (mask != null) for (int i = 0; i < data.length; i++) data[i] ^= mask[i & 3];
            if (op == 9) { send(out, 10, data); continue; }   // ping -> pong
            if (op == 10) continue;                           // pong
            if (op == 8) { Frame f = new Frame(); f.opcode = 8; f.data = data; return f; }
            if (op != 0) { msgOp = op; msg = new ByteArrayOutputStream(); }
            if (msg == null) continue;
            msg.write(data);
            if (fin) { Frame f = new Frame(); f.opcode = msgOp; f.data = msg.toByteArray(); return f; }
        }
    }

    static int readByte(InputStream in) throws IOException {
        int b = in.read();
        if (b < 0) throw new IOException("connection closed");
        return b;
    }

    static void readFully(InputStream in, byte[] d) throws IOException {
        int o = 0;
        while (o < d.length) {
            int n = in.read(d, o, d.length - o);
            if (n < 0) throw new IOException("connection closed");
            o += n;
        }
    }

    /** Splits long text at sentence ends so each request stays small. */
    public static List<String> chunks(String text, int max) {
        List<String> out = new ArrayList<String>();
        String t = text.trim();
        while (t.length() > max) {
            int cut = -1;
            for (int i = max; i > max / 3; i--) {
                char c = t.charAt(i);
                if (c == '।' || c == '.' || c == '!' || c == '?' || c == ',') { cut = i + 1; break; }
            }
            if (cut < 0) cut = t.lastIndexOf(' ', max);
            if (cut <= 0) cut = max;
            out.add(t.substring(0, cut).trim());
            t = t.substring(cut).trim();
        }
        if (t.length() > 0) out.add(t);
        return out;
    }
}
