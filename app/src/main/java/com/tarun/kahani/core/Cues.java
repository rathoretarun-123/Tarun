package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * The cues of any script that nothing else hardcodes: a sound written as a word ("टिम-टिम", "खट-खट", "फुस्स",
 * "खटाक", "beep", "crash") becomes a sound made from its letters, and a thing happening to light or to a
 * machine (lights coming on, going out, flickering; a glitch, a beam of light, a drone, a spark, an explosion,
 * a notification, hearts) becomes an effect on the stage and the sound that goes with it. The director reads
 * every stage direction and every manner through this, so a story about solar flowers and servers gets its
 * effects as naturally as one about rain and rivers.
 */
public final class Cues {
    private Cues() {}

    /** Visual kinds. */
    public static final int V_NONE = 0, V_GLOW = 1, V_TWINKLE = 2, V_FLICKER = 3, V_OFF = 4, V_GLITCH = 5, V_BEAM = 6, V_DRONE = 7,
            V_HEARTS = 8, V_NOTIFY = 9, V_DATA = 10, V_SPARKS = 11, V_BLAST = 12, V_STEAM = 13;

    /** One cue: what is seen, what is heard, how strong, how long. */
    public static final class Cue {
        public int visual = V_NONE;
        public int sfx = -1;
        public float strength = 1f, seconds = 2f, gain = 0.5f;
        public int color;
        public String word = "";
        Cue(int visual, int sfx, float seconds, float gain, String word) { this.visual = visual; this.sfx = sfx; this.seconds = seconds; this.gain = gain; this.word = word; }
    }

    // ------------------------------------------------------------------ the words

    static final String[] LIGHT_ON = {"जगमगा", "चमक उठ", "खिल उठ", "रोशनी वापस", "लाइट जल", "जल उठ", "जलने लग", "रोशन हो", "दमक", "light up", "lights come on",
            "light comes on", "lights up", "glow", "glows", "glowing", "shine", "shines", "shining", "bright", "lit up", "charge होने", "charge हो", "charging", "sparkle"};
    static final String[] TWINKLE = {"झिलमिल", "टिमटिम", "टिम-टिम", "जगमग", "twinkle", "twinkling", "glitter", "sparkl", "shimmer", "blink"};
    static final String[] LIGHT_OFF = {"लाइट बंद", "लाइट बुझ", "बुझ ", "बुझ जा", "फुस्स", "बत्ती गुल", "अंधेरा हो", "अँधेरा हो", "lights go out", "light goes out", "goes dark",
            "go dark", "blackout", "switch off", "switched off", "power cut", "बिजली चली", "बिजली गुल", "turn off", "turned off", "बंद हो जाती", "बंद हो जाते", "shut down", "down हो"};
    static final String[] FLICKER = {"टिमटिमाती", "टिमटिमाता", "flicker", "flickers", "flickering", "फड़फड़ाती लाइट", "झपकती"};
    static final String[] GLITCH = {"hack", "हैक", "virus", "वायरस", "glitch", "ग्लिच", "freeze", "फ्रीज़", "हैंग", "hang हो", "error", "एरर", "crash", "क्रैश", "static", "jam हो", "corrupt"};
    static final String[] DATA = {"data", "डेटा", "डाटा", "download", "upload", "signal", "सिग्नल", "transfer", "leak", "चोरी होकर", "stream", "file", "फ़ाइल"};
    static final String[] BEAM = {"flashlight", "टॉर्च", "torch", "laser", "लेज़र", "लेजर", "reflection", "किरण", "beam", "रोशनी मार", "रोशनी फेंक", "रोशनी डाल", "focus कर", "spotlight", "flash मार"};
    static final String[] DRONE = {"drone", "ड्रोन", "quadcopter", "helicopter", "हेलीकॉप्टर", "uav"};
    static final String[] NOTIFY = {"notification", "नोटिफिकेशन", "पॉप होता", "पॉप हो", "pop up", "pops up", "message आता", "मैसेज आता", "message आया", "फ़ोन बजता", "फोन बजता", "phone rings", "रिंग", "alert", "अलर्ट"};
    static final String[] HEARTS = {"❤", "💕", "💖", "दिल वाला", "heart emoji", "दिल बन", "hearts"};
    static final String[] SPARK = {"तार खींच", "तारें खींच", "plug खींच", "wire", "वायर", "spark", "चिंगारी", "short circuit", "शॉर्ट सर्किट", "बिजली का झटका", "झटका लगता", "sparks"};
    static final String[] BLAST = {"धमाका", "विस्फोट", "explosion", "explode", "blast", "फट जाता", "फट गया", "boom"};
    static final String[] STEAM = {"धुआँ निकल", "धुआं निकल", "भाप", "steam", "smoke comes", "smoke rises", "धुआँ उठ"};
    static final String[] TYPING = {"टाइपिंग", "typing", "types", "टाइप कर", "keyboard", "कीबोर्ड", "coding", "कोड लिख"};
    static final String[] BEEP = {"बीप", "beep", "बूप", "boop", "बीप-बूप"};
    static final String[] CLICK = {"click", "क्लिक", "खटाक", "खट ", "clack", "switch", "स्विच दबा", "बटन दबा", "press", "दबाता", "दबाती"};
    static final String[] CRACKLE = {"कर-कर", "crackle", "चरमरा", "crackling", "कड़कड़"};
    static final String[] HISS = {"फूँ", "फू...", "hiss", "सिसकार", "sss"};
    static final String[] TAP = {"टप-टप", "tap tap", "tap-tap", "pitter", "टिप-टिप"};
    static final String[] BUZZ = {"भिन-भिन", "भिनभिना", "buzz", "bzz", "गूँज"};
    static final String[] HUM = {"गुन-गुन", "hum of", "humming of", "घर्र"};
    static final String[] POP = {"पप-पप", "pop-pop", "पॉप-पॉप", "टप-टप करके खिल"};
    static final String[] RING = {"घंटी बज", "bell rings", "ring ring", "ट्रिन", "टन-टन", "tring"};

    /** A quoted sound in a direction: "सन-सन", "टिम-टिम", "कर-कर". */
    static final Pattern QUOTED = Pattern.compile("[\"“']([^\"”'\\s]{1,12}([\\-–][^\"”'\\s]{1,12}){0,3})[\"”']");

    /** The cues in a sentence (empty when nothing in it is a cue). */
    public static List<Cue> read(String s) {
        List<Cue> out = new ArrayList<Cue>();
        if (s == null || s.length() == 0) return out;
        // ---- things done to light and to machines
        if (Txt.has(s, LIGHT_OFF) && !Txt.has(s, "फिर से", "again", "वापस")) out.add(new Cue(V_OFF, Film.SFX_POWER_DOWN, 1.6f, 0.45f, "lights off"));
        else if (Txt.has(s, FLICKER)) out.add(new Cue(V_FLICKER, Film.SFX_HUM, 2.5f, 0.2f, "flicker"));
        else if (Txt.has(s, TWINKLE) && !Txt.has(s, LIGHT_OFF)) out.add(new Cue(V_TWINKLE, Film.SFX_TWINKLE, 3f, 0.3f, "twinkle"));
        else if (Txt.has(s, LIGHT_ON) && !Txt.has(s, "आँखें चमक", "आंखें चमक", "eyes shine", "eyes glow", "eyes light")) out.add(new Cue(V_GLOW, Film.SFX_TWINKLE, 2.6f, 0.35f, "light on"));
        if (Txt.has(s, GLITCH)) out.add(new Cue(V_GLITCH, Film.SFX_GLITCH, 1.2f, 0.4f, "glitch"));
        if (Txt.has(s, DATA) && Txt.has(s, "जाने", "जाता", "जाती", "चोरी", "flow", "goes", "leak", "transfer", "download", "upload", "खिंच")) out.add(new Cue(V_DATA, Film.SFX_GLITCH, 3f, 0.25f, "data"));
        if (Txt.has(s, BEAM)) out.add(new Cue(V_BEAM, Film.SFX_CHIME, 3f, 0.25f, "beam"));
        if (Txt.has(s, DRONE) && !Txt.has(s, "drone controller", "ड्रोन कंट्रोलर", "drone remote")) out.add(new Cue(V_DRONE, Film.SFX_DRONE, 6f, 0.25f, "drone"));
        if (Txt.has(s, NOTIFY)) out.add(new Cue(V_NOTIFY, Film.SFX_BLIP, 1.8f, 0.45f, "notification"));
        if (Txt.has(s, HEARTS)) out.add(new Cue(V_HEARTS, Film.SFX_TWINKLE, 2.4f, 0.3f, "hearts"));
        if (Txt.has(s, SPARK)) out.add(new Cue(V_SPARKS, Film.SFX_SPARK, 1f, 0.5f, "spark"));
        if (Txt.has(s, BLAST)) out.add(new Cue(V_BLAST, Film.SFX_BOOM, 1.5f, 0.8f, "blast"));
        if (Txt.has(s, STEAM)) out.add(new Cue(V_STEAM, Film.SFX_HISS_SHORT, 2.5f, 0.3f, "steam"));
        // ---- sounds named by their words
        if (Txt.has(s, TYPING)) out.add(new Cue(V_NONE, Film.SFX_TYPING, 3f, 0.3f, "typing"));
        if (Txt.has(s, BEEP)) out.add(new Cue(V_NONE, Film.SFX_BEEP, 0.9f, 0.4f, "beep"));
        if (Txt.has(s, CLICK)) out.add(new Cue(V_NONE, Film.SFX_CLICK, 0.4f, 0.5f, "click"));
        if (Txt.has(s, CRACKLE)) out.add(new Cue(V_NONE, Film.SFX_CRACKLE, 2.5f, 0.3f, "crackle"));
        if (Txt.has(s, HISS)) out.add(new Cue(V_NONE, Film.SFX_HISS_SHORT, 0.8f, 0.4f, "hiss"));
        if (Txt.has(s, TAP)) out.add(new Cue(V_NONE, Film.SFX_TAP, 2f, 0.35f, "tap"));
        if (Txt.has(s, BUZZ)) out.add(new Cue(V_NONE, Film.SFX_BUZZ, 2f, 0.25f, "buzz"));
        if (Txt.has(s, HUM)) out.add(new Cue(V_NONE, Film.SFX_HUM, 3f, 0.2f, "hum"));
        if (Txt.has(s, POP)) out.add(new Cue(V_GLOW, Film.SFX_POP, 1.5f, 0.4f, "pop"));
        if (Txt.has(s, RING)) out.add(new Cue(V_NONE, Film.SFX_BELL, 2f, 0.4f, "ring"));
        // ---- any other sound written as a word in quotes: made from its letters
        Matcher m = QUOTED.matcher(s);
        while (m.find()) {
            String w = m.group(1);
            if (known(out, w)) continue;
            int sfx = soundFromLetters(w);
            if (sfx >= 0) out.add(new Cue(V_NONE, sfx, w.contains("-") || w.contains("–") ? 1.4f : 0.6f, 0.35f, w));
        }
        return out;
    }

    private static boolean known(List<Cue> out, String w) {
        String n = Txt.norm(w);
        for (Cue c : out) if (c.sfx >= 0 && (Txt.has(w, BEEP) || Txt.has(w, CLICK) || Txt.has(w, CRACKLE) || Txt.has(w, HISS) || Txt.has(w, TAP) || Txt.has(w, BUZZ)
                || Txt.has(w, POP) || Txt.has(w, HUM) || Txt.has(w, TWINKLE) || Txt.has(w, RING))) return true;
        return n.length() == 0;
    }

    /**
     * A sound from the letters of a word nobody listed: taps for ट/त/t, pops for प/ब/p/b, clicks for क/ख/k, thuds
     * for ध/ड/d, hisses for स/श/फ/ह/s/f/h, jingles for झ/ज/छ/j/ch, buzzes for भ/घ/ग/g, rumbles for ढ/ड़/r.
     */
    public static int soundFromLetters(String w) {
        if (w == null || w.length() == 0) return -1;
        String n = Txt.norm(w).toLowerCase(Locale.ROOT);
        char c = n.charAt(0);
        if (n.startsWith("dh") || n.startsWith("ढ") || n.startsWith("ध") || n.startsWith("द") || n.startsWith("ड़") || n.startsWith("ड")) return Film.SFX_THUD;
        if (n.startsWith("th") || c == 'ट' || c == 'त' || c == 'ठ' || c == 't') return Film.SFX_TAP;
        if (c == 'प' || c == 'ब' || c == 'p' || c == 'b') return Film.SFX_POP;
        if (c == 'क' || c == 'ख' || c == 'k' || c == 'c' || c == 'q') return Film.SFX_CLICK;
        if (c == 'स' || c == 'श' || c == 'ष' || c == 'फ' || c == 'ह' || c == 's' || c == 'f' || c == 'h' || c == 'w') return Film.SFX_HISS_SHORT;
        if (c == 'झ' || c == 'ज' || c == 'छ' || c == 'च' || c == 'j' || c == 'z') return Film.SFX_TWINKLE;
        if (c == 'भ' || c == 'घ' || c == 'ग' || c == 'g' || c == 'v' || c == 'm' || c == 'n' || c == 'ं') return Film.SFX_BUZZ;
        if (c == 'र' || c == 'r' || c == 'l' || c == 'ल') return Film.SFX_CRACKLE;
        return -1;
    }

    /** True when the sentence has any cue at all (used to decide whether a direction deserves a beat of time). */
    public static boolean any(String s) { return !read(s).isEmpty(); }
}
