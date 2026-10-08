package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;

/** Parsed script. Pure data, no Android dependencies. */
public final class Story {
    public String title = "";
    public String subtitle = "";
    public boolean hindi = true;
    public String sceneWord = "दृश्य";
    public final List<CharacterDef> characters = new ArrayList<CharacterDef>();
    public final List<PlaceDef> places = new ArrayList<PlaceDef>();
    public final List<Scene> scenes = new ArrayList<Scene>();
    public final List<String> warnings = new ArrayList<String>();
    public boolean hasNarrator;
    /** Written in Hinglish (Hindi in English letters): the text was turned into Devanagari for voices and staging. */
    public boolean hinglish;
    /** Devanagari word -> the user's own spelling (Hinglish stories), for names, title and subtitles. */
    public java.util.Map<String, String> back;
    /** Lines that were converted from Hinglish (only text from these is shown back in English letters). */
    public final List<String> converted = new ArrayList<String>();

    /** How the user wrote it: text from Hinglish lines is shown in English letters again; Hindi stays Hindi. */
    public String shown(String text) {
        if (!hinglish || text == null) return text;
        String t = text.trim();
        if (t.length() == 0) return text;
        for (String line : converted) if (line.contains(t)) return Hinglish.back(text, back);
        return text;
    }

    public static final class CharacterDef {
        public String id;            // stable key
        public String displayName;   // e.g. "वृंदा"
        public String fullName;      // e.g. "बड़ी राजकुमारी वृंदा"
        public final List<String> aliases = new ArrayList<String>();
        public String description = "";
        public int age = -1;
        public boolean fromScript;   // false when invented for an unknown speaker
        public Look look;
        public String label;          // name as the user wrote it (Hinglish), or null
        public String shown() { return label != null ? label : displayName; }
        public String toString() { return displayName; }
    }

    public static final class PlaceDef {
        public String name = "";
        public String description = "";
    }

    public static final class Scene {
        public int number;
        public String heading = "";      // "दृश्य 1"
        public String title = "";        // "रत्नगढ़ की जादुई सुबह"
        public String setting = "";      // text of (स्थान: ...)
        public String cues = "";         // nature cues of the whole scene read by the AI, not shown
        public final List<Beat> beats = new ArrayList<Beat>();
    }

    public static final class Beat {
        public static final int DIRECTION = 0, DIALOGUE = 1;
        public int type;
        public String speakerRaw = "";
        public CharacterDef speaker;
        public String manner = "";       // text inside parentheses after speaker
        public String text = "";
        public boolean narrator;         // voice-over line (no character on screen)
        public String cue = "";          // nature cues read by the AI ("rain", "boat"…), not shown
        public static Beat direction(String t) { Beat b = new Beat(); b.type = DIRECTION; b.text = t; return b; }
    }

    public CharacterDef find(String id) {
        for (CharacterDef c : characters) if (c.id.equals(id)) return c;
        return null;
    }

    public int dialogueCount() {
        int n = 0;
        for (Scene s : scenes) for (Beat b : s.beats) if (b.type == Beat.DIALOGUE) n++;
        return n;
    }
}
