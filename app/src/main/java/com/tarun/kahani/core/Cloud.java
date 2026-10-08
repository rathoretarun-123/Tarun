package com.tarun.kahani.core;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Free online helpers, all optional (the studio works offline without them):
 *  - Gemini (free API key from aistudio.google.com): script reading, picture recognition, expressive voices.
 *  - Pollinations (no key): text model fallback and picture generation.
 *  - Openverse / Wikimedia Commons (no key): free-licence pictures and sounds.
 *  - Freesound, Pixabay, Pexels (optional free keys the user types into the app; kept only on the phone):
 *    more and better recordings and photos. Nothing works less without them.
 * Pure Java (HttpURLConnection) so it can be tested on a desktop.
 */
public final class Cloud {
    public String geminiKey = "";
    public String geminiBase = "https://generativelanguage.googleapis.com/v1beta";
    public String textModel = "gemini-2.5-flash";
    public String ttsModel = "gemini-2.5-flash-preview-tts";
    public String pollinationsText = "https://text.pollinations.ai/openai";
    public String pollinationsImage = "https://image.pollinations.ai/prompt/";
    public String openverse = "https://api.openverse.org/v1/";
    public String commons = "https://commons.wikimedia.org/w/api.php";
    public String freesoundKey = "", pixabayKey = "", pexelsKey = "";
    public String freesound = "https://freesound.org/apiv2/", pixabay = "https://pixabay.com/api/", pexels = "https://api.pexels.com/v1/";
    /** One extra header for the next requests (Pexels wants its key as a header), or null. */
    private String authHeader;
    public int timeoutMs = 90000;
    public volatile String lastError = "";

    public boolean hasGemini() { return geminiKey != null && geminiKey.trim().length() > 20; }

    // ------------------------------------------------------------------ HTTP

    public static final class HttpError extends IOException {
        public final int code;
        public final String body;
        HttpError(int code, String body) { super("HTTP " + code + ": " + (body.length() > 300 ? body.substring(0, 300) : body)); this.code = code; this.body = body; }
    }

    public byte[] request(String method, String url, String contentType, byte[] body) throws IOException {
        return request(method, url, contentType, body, null);
    }

    /** headers: {name, value, name, value…} sent with this one request (e.g. a service's own key header). */
    public byte[] request(String method, String url, String contentType, byte[] body, String[] headers) throws IOException {
        HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
        c.setConnectTimeout(20000);
        c.setReadTimeout(timeoutMs);
        c.setRequestMethod(method);
        c.setRequestProperty("User-Agent", "KahaniFilm/6 (Android; children's film maker)");
        c.setRequestProperty("Accept", "*/*");
        if (authHeader != null) c.setRequestProperty("Authorization", authHeader);
        if (headers != null) for (int i = 0; i + 1 < headers.length; i += 2) c.setRequestProperty(headers[i], headers[i + 1]);
        if (body != null) {
            c.setDoOutput(true);
            c.setRequestProperty("Content-Type", contentType);
            c.setFixedLengthStreamingMode(body.length);
            OutputStream os = c.getOutputStream();
            os.write(body);
            os.close();
        }
        int code = c.getResponseCode();
        InputStream in = code >= 400 ? c.getErrorStream() : c.getInputStream();
        byte[] data = in == null ? new byte[0] : readAll(in, 60 << 20);
        c.disconnect();
        if (code >= 400) throw new HttpError(code, new String(data, "UTF-8"));
        return data;
    }

    static byte[] readAll(InputStream in, int max) throws IOException {
        ByteArrayOutputStream bo = new ByteArrayOutputStream();
        byte[] buf = new byte[16384];
        int n;
        while ((n = in.read(buf)) > 0) {
            bo.write(buf, 0, n);
            if (bo.size() > max) throw new IOException("response too large");
        }
        in.close();
        return bo.toByteArray();
    }

    public Object postJson(String url, Object body) throws IOException {
        byte[] r = request("POST", url, "application/json; charset=utf-8", Json.write(body).getBytes("UTF-8"));
        return Json.parseLoose(new String(r, "UTF-8"));
    }

    public Object getJson(String url) throws IOException {
        return Json.parseLoose(new String(request("GET", url, null, null), "UTF-8"));
    }

    public byte[] download(String url) throws IOException { return request("GET", url, null, null); }

    static String enc(String s) {
        try { return URLEncoder.encode(s, "UTF-8").replace("+", "%20"); } catch (Exception e) { return s; }
    }

    static Map<String, Object> map(Object... kv) {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        for (int i = 0; i + 1 < kv.length; i += 2) m.put((String) kv[i], kv[i + 1]);
        return m;
    }

    static List<Object> list(Object... xs) {
        List<Object> l = new ArrayList<Object>();
        for (Object x : xs) l.add(x);
        return l;
    }

    /** Waits and retries on "too many requests" / server busy; other errors are thrown. */
    interface Call<T> { T run() throws IOException; }

    <T> T retry(Call<T> c, int tries) throws IOException {
        IOException last = null;
        for (int i = 0; i < tries; i++) {
            try {
                return c.run();
            } catch (HttpError e) {
                last = e;
                if (e.code != 429 && e.code != 500 && e.code != 502 && e.code != 503 && e.code != 504) throw e;
                // a daily quota is not worth waiting for
                if (e.code == 429 && e.body.contains("PerDay")) throw e;
                sleep(Math.min(40000, 4000L << i));
            } catch (java.net.SocketTimeoutException e) {
                last = e;
            }
        }
        throw last != null ? last : new IOException("failed");
    }

    static void sleep(long ms) {
        try { Thread.sleep(ms); } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
    }

    // ------------------------------------------------------------------ text (LLM)

    /** Asks the language model. Uses Gemini when a key is set, otherwise the free keyless Pollinations model. */
    public String ask(final String system, final String prompt, final boolean json) throws IOException {
        if (hasGemini()) {
            try {
                return retry(new Call<String>() { public String run() throws IOException { return gemini(system, prompt, json, null, null); } }, 3);
            } catch (IOException e) {
                lastError = e.getMessage();
                // fall through to the keyless model
            }
        }
        return retry(new Call<String>() { public String run() throws IOException { return pollinations(system, prompt, json); } }, 2);
    }

    /** Gemini generateContent with optional pictures (JPEG bytes). */
    public String gemini(String system, String prompt, boolean json, List<byte[]> images, String model) throws IOException {
        List<Object> parts = new ArrayList<Object>();
        if (images != null) for (byte[] im : images)
            parts.add(map("inlineData", map("mimeType", "image/jpeg", "data", b64(im))));
        parts.add(map("text", prompt));
        Map<String, Object> body = map("contents", list(map("role", "user", "parts", parts)));
        if (system != null && system.length() > 0) body.put("systemInstruction", map("parts", list(map("text", system))));
        Map<String, Object> gen = map("temperature", 0.4);
        if (json) gen.put("responseMimeType", "application/json");
        body.put("generationConfig", gen);
        Object r = postJson(geminiBase + "/models/" + (model == null ? textModel : model) + ":generateContent?key=" + enc(geminiKey.trim()), body);
        String t = geminiText(r);
        if (t == null) throw new IOException("Gemini: empty answer");
        return t;
    }

    /** Gemini listens to a sound (WAV bytes) and answers the prompt. Needs a Gemini key. */
    public String listen(String prompt, byte[] wav) throws IOException {
        if (!hasGemini()) throw new IOException("listening needs a Gemini key");
        List<Object> parts = new ArrayList<Object>();
        parts.add(map("inlineData", map("mimeType", "audio/wav", "data", b64(wav))));
        parts.add(map("text", prompt));
        Map<String, Object> body = map("contents", list(map("role", "user", "parts", parts)));
        body.put("generationConfig", map("temperature", 0.2, "responseMimeType", "application/json"));
        Object r = postJson(geminiBase + "/models/" + textModel + ":generateContent?key=" + enc(geminiKey.trim()), body);
        String t = geminiText(r);
        if (t == null) throw new IOException("Gemini: empty answer");
        return t;
    }

    static String geminiText(Object r) {
        List<Object> cands = Json.arr(r, "candidates");
        if (cands == null || cands.isEmpty()) return null;
        Map<String, Object> content = Json.obj(cands.get(0), "content");
        List<Object> parts = Json.arr(content, "parts");
        if (parts == null) return null;
        StringBuilder b = new StringBuilder();
        for (Object p : parts) b.append(Json.str(p, "text", ""));
        return b.length() == 0 ? null : b.toString();
    }

    /** Looks at a picture (JPEG) and answers: Gemini when a key is set, otherwise the free keyless vision model. */
    public String vision(String prompt, byte[] jpeg) throws IOException {
        if (hasGemini()) {
            try {
                List<byte[]> ims = new ArrayList<byte[]>();
                ims.add(jpeg);
                return gemini("You are a careful film director's assistant.", prompt, true, ims, null);
            } catch (IOException e) {
                lastError = e.getMessage();
            }
        }
        List<Object> content = new ArrayList<Object>();
        content.add(map("type", "text", "text", prompt));
        content.add(map("type", "image_url", "image_url", map("url", "data:image/jpeg;base64," + b64(jpeg))));
        final Map<String, Object> body = map("model", "openai", "messages", list(map("role", "user", "content", content)), "private", true);
        return retry(new Call<String>() {
            public String run() throws IOException {
                Object r = postJson(pollinationsText, body);
                List<Object> ch = Json.arr(r, "choices");
                if (ch == null || ch.isEmpty()) throw new IOException("vision: empty answer");
                return Json.str(Json.obj(ch.get(0), "message"), "content", "");
            }
        }, 2);
    }

    public String pollinations(String system, String prompt, boolean json) throws IOException {
        List<Object> msgs = new ArrayList<Object>();
        if (system != null && system.length() > 0) msgs.add(map("role", "system", "content", system));
        msgs.add(map("role", "user", "content", prompt));
        Map<String, Object> body = map("model", "openai", "messages", msgs, "private", true);
        if (json) body.put("response_format", map("type", "json_object"));
        Object r = postJson(pollinationsText, body);
        List<Object> ch = Json.arr(r, "choices");
        if (ch == null || ch.isEmpty()) throw new IOException("text model: empty answer");
        String t = Json.str(Json.obj(ch.get(0), "message"), "content", "");
        if (t.length() == 0) throw new IOException("text model: empty answer");
        return t;
    }

    /** Pulls the first {...} or [...] block out of a model answer (models sometimes add ```json fences). */
    public static Object jsonIn(String answer) {
        if (answer == null) return null;
        Object o = Json.parseLoose(answer.trim());
        if (o != null) return o;
        int a = answer.indexOf('{'), b = answer.lastIndexOf('}');
        int a2 = answer.indexOf('['), b2 = answer.lastIndexOf(']');
        if (a2 >= 0 && (a < 0 || a2 < a) && b2 > a2) { a = a2; b = b2; }
        if (a < 0 || b <= a) return null;
        return Json.parseLoose(answer.substring(a, b + 1));
    }

    // ------------------------------------------------------------------ voices (Gemini TTS)

    /** Gemini prebuilt voices that suit each kind of character. */
    public static String geminiVoiceFor(Look l, int seed) {
        String[] girl = {"Leda", "Aoede", "Zephyr"}, woman = {"Kore", "Despina", "Sulafat"}, boy = {"Puck", "Fenrir"},
                man = {"Charon", "Orus", "Iapetus", "Rasalgethi"}, old = {"Gacrux", "Algieba"}, monster = {"Algenib"};
        String[] pool;
        if (l == null) pool = man;
        else switch (l.kind) {
            case Look.GIRL: pool = girl; break;
            case Look.WOMAN: case Look.WITCH: pool = woman; break;
            case Look.BOY: case Look.MONKEY: pool = boy; break;
            case Look.OLD_MAN: pool = old; break;
            case Look.MONSTER: pool = monster; break;
            default: pool = l.female ? woman : man;
        }
        return pool[Math.abs(seed) % pool.length];
    }

    /** Speaks text with an acting direction. Returns mono PCM at 24000 Hz as floats. */
    public float[] geminiSpeak(String direction, String text, String voiceName) throws IOException {
        String prompt = (direction == null || direction.length() == 0 ? "" : direction + ": ") + text;
        final Map<String, Object> body = map("contents", list(map("parts", list(map("text", prompt)))),
                "generationConfig", map("responseModalities", list("AUDIO"),
                        "speechConfig", map("voiceConfig", map("prebuiltVoiceConfig", map("voiceName", voiceName)))));
        Object r = retry(new Call<Object>() {
            public Object run() throws IOException {
                return postJson(geminiBase + "/models/" + ttsModel + ":generateContent?key=" + enc(geminiKey.trim()), body);
            }
        }, 3);
        List<Object> cands = Json.arr(r, "candidates");
        if (cands == null || cands.isEmpty()) throw new IOException("Gemini voice: empty answer");
        List<Object> parts = Json.arr(Json.obj(cands.get(0), "content"), "parts");
        if (parts == null) throw new IOException("Gemini voice: no audio");
        for (Object p : parts) {
            Map<String, Object> d = Json.obj(p, "inlineData");
            if (d == null) continue;
            byte[] pcm = unb64(Json.str(d, "data", ""));
            float[] f = new float[pcm.length / 2];
            for (int i = 0; i < f.length; i++) f[i] = (short) ((pcm[2 * i] & 0xFF) | (pcm[2 * i + 1] << 8)) / 32768f;
            return f;
        }
        throw new IOException("Gemini voice: no audio");
    }

    // ------------------------------------------------------------------ pictures and sounds

    /** Free picture generation (no key). width/height in pixels. */
    public byte[] makePicture(String prompt, int width, int height, int seed) throws IOException {
        String p = prompt.length() > 900 ? prompt.substring(0, 900) : prompt;
        final String url = pollinationsImage + enc(p) + "?width=" + width + "&height=" + height + "&seed=" + seed + "&nologo=true&safe=true";
        byte[] b = retry(new Call<byte[]>() { public byte[] run() throws IOException { return download(url); } }, 2);
        if (b.length < 2000) throw new IOException("picture service returned no picture");
        return b;
    }

    public static final class Found {
        public String title = "", url = "", thumb = "", license = "", creator = "", source = "";
        public float seconds;
    }

    /** Searches free-licence pictures (Pixabay / Pexels with the user's keys, Openverse, then Wikimedia Commons). */
    public List<Found> searchPictures(String q, int max) {
        List<Found> out = new ArrayList<Found>();
        if (pixabayKey != null && pixabayKey.trim().length() > 10) {
            try {
                Object r = getJson(pixabay + "?key=" + enc(pixabayKey.trim()) + "&q=" + enc(q) + "&safesearch=true&per_page=" + Math.max(3, Math.min(max, 50)));
                List<Object> hits = Json.arr(r, "hits");
                if (hits != null) for (Object x : hits) {
                    Found f = new Found();
                    f.title = Json.str(x, "tags", q); f.url = Json.str(x, "largeImageURL", Json.str(x, "webformatURL", ""));
                    f.thumb = Json.str(x, "previewURL", f.url); f.license = "Pixabay"; f.creator = Json.str(x, "user", ""); f.source = "Pixabay";
                    if (f.url.length() > 0 && out.size() < max) out.add(f);
                }
            } catch (IOException e) { lastError = e.getMessage(); }
        }
        if (pexelsKey != null && pexelsKey.trim().length() > 10 && out.size() < max) {
            try {
                authHeader = pexelsKey.trim();
                Object r = getJson(pexels + "search?query=" + enc(q) + "&per_page=" + Math.max(3, Math.min(max, 40)));
                List<Object> ph = Json.arr(r, "photos");
                if (ph != null) for (Object x : ph) {
                    Found f = new Found();
                    Object src = Json.obj(x, "src");
                    f.title = Json.str(x, "alt", q); f.url = Json.str(src, "large", Json.str(src, "original", ""));
                    f.thumb = Json.str(src, "medium", f.url); f.license = "Pexels"; f.creator = Json.str(x, "photographer", ""); f.source = "Pexels";
                    if (f.url.length() > 0 && out.size() < max) out.add(f);
                }
            } catch (IOException e) { lastError = e.getMessage(); }
            finally { authHeader = null; }
        }
        if (out.size() >= max) return out;
        try {
            Object r = getJson(openverse + "images/?q=" + enc(q) + "&page_size=" + max + "&mature=false");
            List<Object> rs = Json.arr(r, "results");
            if (rs != null) for (Object x : rs) {
                Found f = new Found();
                f.title = Json.str(x, "title", q); f.url = Json.str(x, "url", ""); f.thumb = Json.str(x, "thumbnail", f.url);
                f.license = Json.str(x, "license", ""); f.creator = Json.str(x, "creator", ""); f.source = "Openverse";
                if (f.url.length() > 0 && out.size() < max) out.add(f);
            }
        } catch (IOException e) { lastError = e.getMessage(); }
        if (out.size() < max) {
            try {
                Object r = getJson(commons + "?action=query&format=json&generator=search&gsrnamespace=6&gsrlimit=" + max
                        + "&gsrsearch=" + enc(q + " filetype:bitmap") + "&prop=imageinfo&iiprop=url&iiurlwidth=640");
                Map<String, Object> pages = Json.obj(Json.obj(r, "query"), "pages");
                if (pages != null) for (Object x : pages.values()) {
                    List<Object> ii = Json.arr(x, "imageinfo");
                    if (ii == null || ii.isEmpty()) continue;
                    Found f = new Found();
                    f.title = Json.str(x, "title", q).replace("File:", "");
                    f.url = Json.str(ii.get(0), "thumburl", Json.str(ii.get(0), "url", ""));
                    f.thumb = f.url; f.source = "Wikimedia Commons"; f.license = "see Commons";
                    if (f.url.length() > 0 && out.size() < max) out.add(f);
                }
            } catch (IOException e) { lastError = e.getMessage(); }
        }
        return out;
    }

    /** Searches free-licence sounds (Freesound with the user's key first, then Openverse audio: Freesound, Jamendo, Wikimedia…). */
    public List<Found> searchSounds(String q, int max) {
        List<Found> out = new ArrayList<Found>();
        if (freesoundKey != null && freesoundKey.trim().length() > 10) {
            try {
                Object r = getJson(freesound + "search/text/?query=" + enc(q) + "&token=" + enc(freesoundKey.trim())
                        + "&fields=name,previews,license,duration,username&sort=rating_desc&page_size=" + Math.max(3, Math.min(max, 30)));
                List<Object> rs = Json.arr(r, "results");
                if (rs != null) for (Object x : rs) {
                    Found f = new Found();
                    Object pv = Json.obj(x, "previews");
                    f.title = Json.str(x, "name", q); f.url = Json.str(pv, "preview-hq-mp3", Json.str(pv, "preview-lq-mp3", ""));
                    f.license = licenceOf(Json.str(x, "license", "")); f.creator = Json.str(x, "username", ""); f.source = "Freesound";
                    f.seconds = (float) Json.num(x, "duration", 0);
                    if (f.url.length() > 0 && out.size() < max) out.add(f);
                }
            } catch (IOException e) { lastError = e.getMessage(); }
        }
        if (out.size() >= max) return out;
        try {
            Object r = getJson(openverse + "audio/?q=" + enc(q) + "&page_size=" + max + "&mature=false");
            List<Object> rs = Json.arr(r, "results");
            if (rs != null) for (Object x : rs) {
                Found f = new Found();
                f.title = Json.str(x, "title", q); f.url = Json.str(x, "url", "");
                f.license = Json.str(x, "license", ""); f.creator = Json.str(x, "creator", ""); f.source = Json.str(x, "source", "Openverse");
                f.seconds = (float) (Json.num(x, "duration", 0) / 1000.0);
                if (f.url.length() > 0 && out.size() < max) out.add(f);
            }
        } catch (IOException e) { lastError = e.getMessage(); }
        return out;
    }

    /** "http://creativecommons.org/licenses/by/4.0/" → "by"; the public-domain dedication → "cc0". */
    static String licenceOf(String url) {
        String u = url.toLowerCase(java.util.Locale.ROOT);
        if (u.contains("zero") || u.contains("cc0")) return "cc0";
        if (u.contains("by-nc")) return "by-nc";
        if (u.contains("sampling")) return "sampling+";
        if (u.contains("/by/")) return "by";
        return u;
    }

    // ------------------------------------------------------------------ base64 (own copy: works on every Android version)

    static final char[] B64 = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789+/".toCharArray();

    static String b64(byte[] b) {
        StringBuilder o = new StringBuilder((b.length + 2) / 3 * 4);
        for (int i = 0; i < b.length; i += 3) {
            int n = (b[i] & 255) << 16 | (i + 1 < b.length ? (b[i + 1] & 255) << 8 : 0) | (i + 2 < b.length ? b[i + 2] & 255 : 0);
            o.append(B64[n >> 18 & 63]).append(B64[n >> 12 & 63]);
            o.append(i + 1 < b.length ? B64[n >> 6 & 63] : '=');
            o.append(i + 2 < b.length ? B64[n & 63] : '=');
        }
        return o.toString();
    }

    static byte[] unb64(String s) {
        int[] v = new int[128];
        java.util.Arrays.fill(v, -1);
        for (int i = 0; i < 64; i++) v[B64[i]] = i;
        v['-'] = 62; v['_'] = 63;
        java.io.ByteArrayOutputStream o = new java.io.ByteArrayOutputStream(s.length() * 3 / 4);
        int acc = 0, bits = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= 128 || v[c] < 0) continue;
            acc = acc << 6 | v[c];
            bits += 6;
            if (bits >= 8) { bits -= 8; o.write(acc >> bits & 255); }
        }
        return o.toByteArray();
    }
}
