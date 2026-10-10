package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Library of real recorded sounds (ambience, effects, music, user samples) with keywords in Hindi and English.
 * The mixer asks for "the best sound for this situation"; when nothing matches it falls back to Synth.
 */
public final class SoundLib {

    public interface Decoder {
        /** Decodes a sound file into mono floats at Synth.SR, or null. */
        float[] decode(String path);
    }

    public static final class Entry {
        public String path;      // decoder-specific path (asset:sounds/x.ogg or a file path)
        public String type;      // amb | sfx | music | voice
        public String title = "";
        public String[] words = new String[0];
        public float seconds;
        public String owner = ""; // character name for voice/character sounds ("" = any)
        public boolean user;      // added by the user (recorded, from a file or a search)
    }

    public final List<Entry> entries = new ArrayList<Entry>();
    private final Decoder decoder;
    private final Map<String, float[]> cache = new LinkedHashMap<String, float[]>(16, 0.75f, true) {
        protected boolean removeEldestEntry(Map.Entry<String, float[]> e) { return size() > 24; }
    };

    public SoundLib(Decoder d) { this.decoder = d; }

    /** Adds the entries of an index.json (array of {file,type,words,title,seconds}). */
    public void addIndex(String json, String pathPrefix) {
        Object o = Json.parseLoose(json);
        if (!(o instanceof List)) return;
        for (Object x : (List<?>) o) {
            Entry e = new Entry();
            e.path = pathPrefix + Json.str(x, "file", "");
            e.type = Json.str(x, "type", "sfx");
            e.title = Json.str(x, "title", "");
            e.seconds = (float) Json.num(x, "seconds", 0);
            e.owner = Json.str(x, "owner", "");
            e.words = splitWords(Json.str(x, "words", "") + "," + e.title);
            entries.add(e);
        }
    }

    public static String[] splitWords(String s) {
        List<String> out = new ArrayList<String>();
        for (String w : s.split("[,;|\\n]")) {
            String t = Txt.norm(w.trim());
            if (t.length() >= 2) out.add(t);
        }
        return out.toArray(new String[0]);
    }

    /** Best entry of a type whose keywords appear in the text (longer keyword matches score higher). */
    public Entry best(String text, String type, String owner) {
        String t = Txt.norm(text);
        Entry best = null;
        int bestScore = 0;
        for (Entry e : entries) {
            if (type != null && !type.equals(e.type)) continue;
            int score = 0;
            for (String w : e.words) if (mentions(t, w)) score += 10 + w.length();
            if (owner != null && owner.length() > 0 && e.owner.length() > 0) {
                if (Txt.norm(owner).contains(Txt.norm(e.owner)) || Txt.norm(e.owner).contains(Txt.norm(owner))) score += 50;
                else continue;
            }
            if (e.path.startsWith("user:") || e.path.startsWith("/")) score += 3; // prefer the user's own sounds
            if (score > bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    /** A word in a text: English words must stand alone ("rain" is not in "brain"), Hindi words may carry endings. */
    static boolean mentions(String text, String w) {
        int at = text.indexOf(w);
        if (at < 0) return false;
        if (w.charAt(0) >= 0x0900) return true;
        for (; at >= 0; at = text.indexOf(w, at + 1)) {
            boolean before = at == 0 || !Character.isLetterOrDigit(text.charAt(at - 1));
            int end = at + w.length();
            boolean after = end >= text.length() || !Character.isLetterOrDigit(text.charAt(end)) || (text.charAt(end) == 's' && (end + 1 >= text.length() || !Character.isLetter(text.charAt(end + 1))));
            if (before && after) return true;
        }
        return false;
    }

    /** The user's own sound of a type that fits the text best, or null. */
    public Entry bestUser(String text, String type) {
        String t = Txt.norm(text);
        Entry best = null;
        int bestScore = 0;
        for (Entry e : entries) {
            if (!e.user || (type != null && !type.equals(e.type))) continue;
            int score = 0;
            for (String w : e.words) if (mentions(t, w)) score += 10 + w.length();
            if (score > bestScore) { bestScore = score; best = e; }
        }
        return best;
    }

    public Entry byPath(String path) {
        for (Entry e : entries) if (e.path.equals(path)) return e;
        return null;
    }

    public Entry byFile(String name) {
        for (Entry e : entries) if (e.path.endsWith("/" + name) || e.path.endsWith(":" + name) || e.path.equals(name)) return e;
        return null;
    }

    public float[] pcm(Entry e) {
        if (e == null || decoder == null) return null;
        float[] p = cache.get(e.path);
        if (p == null) {
            try { p = decoder.decode(e.path); } catch (RuntimeException ex) { p = null; }
            if (p != null) cache.put(e.path, p);
        }
        return p;
    }

    /** Repeats a clip to fill dur seconds with short crossfades, fading in and out. */
    public static float[] loop(float[] src, float dur, float fade) {
        int n = Math.max(1, (int) (dur * Synth.SR));
        float[] o = new float[n];
        if (src == null || src.length < Synth.SR / 4) return o;
        int xf = Math.min(src.length / 4, Synth.SR); // 1 s crossfade
        int step = src.length - xf;
        for (int start = 0; start < n; start += step) {
            for (int i = 0; i < src.length && start + i < n; i++) {
                float g = 1f;
                if (start > 0 && i < xf) g = i / (float) xf;
                if (i >= src.length - xf && start + step < n) g *= (src.length - i) / (float) xf;
                o[start + i] += src[i] * g;
            }
        }
        int f = Math.min(n / 2, (int) (fade * Synth.SR));
        for (int i = 0; i < f; i++) { float g = i / (float) f; o[i] *= g; o[n - 1 - i] *= g; }
        return o;
    }
}
