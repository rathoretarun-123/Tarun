package com.tarun.kahani.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Everything the user can change after previewing the film, through the command box
 * ("background music कम करो", "वृंदा की आवाज़ तेज़ करो", "brightness बढ़ाओ", "file size कम करो" ...).
 * Stored per project as JSON; re-rendering applies it without re-generating voices or pictures.
 */
public final class Edits {
    public float brightness = 0f;   // -1..1
    public float contrast = 1f;     // 0.5..1.6
    public float saturation = 1f;   // 0..2
    public float warmth = 0f;       // -1..1 (cool..warm)
    public float music = 1f, sfx = 1f, ambience = 1f, voices = 1f, narrator = 1f;
    public float speed = 1f;        // overall pace of pauses/actions (0.7..1.3)
    public boolean subtitles = false;
    public int height = 1080;       // output height for 16:9 (or width for 9:16)
    public String aspect = "16:9";  // 16:9 | 9:16 | 1:1
    public int quality = 2;         // 1 small file, 2 normal, 3 high
    public final Map<String, Float> voiceGain = new LinkedHashMap<String, Float>();
    public final Map<String, Float> voicePitch = new LinkedHashMap<String, Float>();
    public final Map<String, Float> voiceRate = new LinkedHashMap<String, Float>();
    /** Voice effects asked for in the edit box, per character: "raspy,booming" ("-raspy" = switched off). */
    public final Map<String, String> voiceStyle = new LinkedHashMap<String, String>();

    public String styleFor(String who) {
        if (who == null) return "";
        String s = voiceStyle.get(who);
        return s == null ? "" : s;
    }

    public float gainFor(String who) {
        if (who == null || who.length() == 0) return voices * narrator;
        Float g = find(voiceGain, who);
        return voices * (g == null ? 1f : g);
    }

    public float pitchFor(String who) {
        Float p = find(voicePitch, who);
        return p == null ? 1f : p;
    }

    public float rateFor(String who) {
        Float p = find(voiceRate, who);
        return p == null ? 1f : p;
    }

    private static Float find(Map<String, Float> m, String who) {
        if (who == null) return null;
        String w = Txt.norm(who);
        for (Map.Entry<String, Float> e : m.entrySet()) {
            String k = Txt.norm(e.getKey());
            if (k.equals(w) || w.contains(k) || k.contains(w)) return e.getValue();
        }
        return null;
    }

    public int[] size() {
        int h = Math.max(360, Math.min(1080, height));
        if (aspect.equals("9:16")) return new int[]{h, h * 16 / 9 - (h * 16 / 9) % 16};
        if (aspect.equals("1:1")) return new int[]{h, h};
        int w = h * 16 / 9;
        return new int[]{w - w % 16, h};
    }

    public float bitrateFactor() { return quality == 1 ? 0.07f : quality == 3 ? 0.2f : 0.12f; }

    // ---------------------------------------------------------------- persistence

    public String toJson() {
        Map<String, Object> m = new LinkedHashMap<String, Object>();
        m.put("brightness", (double) brightness); m.put("contrast", (double) contrast); m.put("saturation", (double) saturation);
        m.put("warmth", (double) warmth); m.put("music", (double) music); m.put("sfx", (double) sfx); m.put("ambience", (double) ambience);
        m.put("voices", (double) voices); m.put("narrator", (double) narrator); m.put("speed", (double) speed);
        m.put("subtitles", subtitles); m.put("height", (long) height); m.put("aspect", aspect); m.put("quality", (long) quality);
        m.put("voiceGain", toObj(voiceGain)); m.put("voicePitch", toObj(voicePitch)); m.put("voiceRate", toObj(voiceRate));
        Map<String, Object> vs = new LinkedHashMap<String, Object>();
        vs.putAll(voiceStyle);
        m.put("voiceStyle", vs);
        return Json.write(m);
    }

    private static Map<String, Object> toObj(Map<String, Float> m) {
        Map<String, Object> o = new LinkedHashMap<String, Object>();
        for (Map.Entry<String, Float> e : m.entrySet()) o.put(e.getKey(), (double) e.getValue());
        return o;
    }

    public static Edits fromJson(String s) {
        Edits e = new Edits();
        Object o = Json.parseLoose(s);
        if (o == null) return e;
        e.brightness = (float) Json.num(o, "brightness", 0); e.contrast = (float) Json.num(o, "contrast", 1);
        e.saturation = (float) Json.num(o, "saturation", 1); e.warmth = (float) Json.num(o, "warmth", 0);
        e.music = (float) Json.num(o, "music", 1); e.sfx = (float) Json.num(o, "sfx", 1); e.ambience = (float) Json.num(o, "ambience", 1);
        e.voices = (float) Json.num(o, "voices", 1); e.narrator = (float) Json.num(o, "narrator", 1); e.speed = (float) Json.num(o, "speed", 1);
        e.subtitles = Json.bool(o, "subtitles", false); e.height = (int) Json.num(o, "height", 1080);
        e.aspect = Json.str(o, "aspect", "16:9"); e.quality = (int) Json.num(o, "quality", 2);
        readMap(Json.obj(o, "voiceGain"), e.voiceGain);
        readMap(Json.obj(o, "voicePitch"), e.voicePitch);
        readMap(Json.obj(o, "voiceRate"), e.voiceRate);
        Map<String, Object> vs = Json.obj(o, "voiceStyle");
        if (vs != null) for (Map.Entry<String, Object> x : vs.entrySet()) if (x.getValue() instanceof String) e.voiceStyle.put(x.getKey(), (String) x.getValue());
        return e;
    }

    private static void readMap(Map<String, Object> src, Map<String, Float> dst) {
        if (src == null) return;
        for (Map.Entry<String, Object> x : src.entrySet()) {
            if (x.getValue() instanceof Number) dst.put(x.getKey(), ((Number) x.getValue()).floatValue());
        }
    }

    static float clamp(float v, float lo, float hi) { return v < lo ? lo : v > hi ? hi : v; }

    /** Applies one JSON edit command, e.g. {"op":"voice_gain","who":"वृंदा","factor":1.4}. Returns a human description. */
    public String apply(Map<String, Object> c) {
        String op = Json.str(c, "op", "");
        String who = Json.str(c, "who", "");
        float f = (float) Json.num(c, "factor", 1);
        float v = (float) Json.num(c, "value", Float.NaN);
        switch (op) {
            case "brightness": brightness = clamp(Float.isNaN(v) ? brightness + (f - 1) : v, -0.8f, 0.8f); return "Brightness: " + pct(brightness);
            case "contrast": contrast = clamp(Float.isNaN(v) ? contrast * f : v, 0.5f, 1.8f); return "Contrast: " + fmt(contrast);
            case "saturation": saturation = clamp(Float.isNaN(v) ? saturation * f : v, 0f, 2f); return "Colour: " + fmt(saturation);
            case "warmth": warmth = clamp(Float.isNaN(v) ? warmth + (f - 1) : v, -1f, 1f); return "Warmth: " + fmt(warmth);
            case "music": music = clamp(Float.isNaN(v) ? music * f : v, 0f, 3f); return "Music: " + pct1(music);
            case "sfx": sfx = clamp(Float.isNaN(v) ? sfx * f : v, 0f, 3f); return "Sound effects: " + pct1(sfx);
            case "ambience": ambience = clamp(Float.isNaN(v) ? ambience * f : v, 0f, 3f); return "Background sounds: " + pct1(ambience);
            case "voices": voices = clamp(Float.isNaN(v) ? voices * f : v, 0.2f, 3f); return "All voices: " + pct1(voices);
            case "narrator": narrator = clamp(Float.isNaN(v) ? narrator * f : v, 0f, 3f); return "Narrator: " + pct1(narrator);
            case "voice_gain": {
                Float cur = voiceGain.get(who);
                float nv = clamp(Float.isNaN(v) ? (cur == null ? 1 : cur) * f : v, 0f, 3f);
                voiceGain.put(who, nv); return who + " voice volume: " + pct1(nv);
            }
            case "voice_pitch": {
                Float cur = voicePitch.get(who);
                float nv = clamp(Float.isNaN(v) ? (cur == null ? 1 : cur) * f : v, 0.5f, 2f);
                voicePitch.put(who, nv); return who + " voice pitch: " + fmt(nv);
            }
            case "voice_rate": {
                Float cur = voiceRate.get(who);
                float nv = clamp(Float.isNaN(v) ? (cur == null ? 1 : cur) * f : v, 0.5f, 2f);
                voiceRate.put(who, nv); return who + " speaking speed: " + fmt(nv);
            }
            case "voice_style": {
                String st = Json.str(c, "style", "").toLowerCase(java.util.Locale.ROOT).trim();
                if (st.length() == 0) return null;
                boolean on = Json.bool(c, "on", true);
                java.util.List<String> keep = new java.util.ArrayList<String>();
                for (String x : styleFor(who).split(",")) if (x.length() > 0 && !x.replace("-", "").equals(st)) keep.add(x);
                keep.add(on ? st : "-" + st);
                StringBuilder b = new StringBuilder();
                for (String x : keep) { if (b.length() > 0) b.append(','); b.append(x); }
                voiceStyle.put(who, b.toString());
                return who + " voice: " + (on ? st : "not " + st);
            }
            case "speed": speed = clamp(Float.isNaN(v) ? speed * f : v, 0.6f, 1.5f); return "Film pace: " + fmt(speed);
            case "subtitles": subtitles = Json.bool(c, "on", !subtitles); return subtitles ? "Subtitles: on" : "Subtitles: off";
            case "file_size": quality = (int) clamp(Float.isNaN(v) ? quality + (f > 1 ? 1 : -1) : v, 1, 3);
                return "File size: " + (quality == 1 ? "small" : quality == 2 ? "normal" : "large (sharpest)");
            case "resolution": height = (int) (Float.isNaN(v) ? (f > 1 ? 1080 : 720) : v); return "Resolution: " + height + "p";
            case "aspect": aspect = Json.str(c, "value", aspect); return "Shape: " + aspect;
            case "reset": { Edits d = new Edits(); copyFrom(d); return "All changes removed"; }
            default: return null;
        }
    }

    private void copyFrom(Edits d) {
        brightness = d.brightness; contrast = d.contrast; saturation = d.saturation; warmth = d.warmth; music = d.music; sfx = d.sfx;
        ambience = d.ambience; voices = d.voices; narrator = d.narrator; speed = d.speed; subtitles = d.subtitles;
        voiceGain.clear(); voicePitch.clear(); voiceRate.clear(); voiceStyle.clear();
    }

    private static String pct(float v) { return (v >= 0 ? "+" : "") + Math.round(v * 100) + "%"; }
    private static String pct1(float v) { return Math.round(v * 100) + "%"; }
    private static String fmt(float v) { return String.valueOf(Math.round(v * 100) / 100f); }

    public static boolean isList(Object o) { return o instanceof List; }
}
