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

    /** v34: a change of clothes in the action ("मीरा लाल लहंगा पहनकर आती है", "Kabir changes into his uniform"). */
    public static final class Costume {
        public int scene, beat;          // where it happens: the scene's number and the action's index in it
        public String text = "";         // the sentence that says it
        public Look look;                // the character's look from then on
        /** A few words for buttons and lists ("लाल लहंगा"). */
        public String label() { String t = text.length() > 48 ? text.substring(0, 48) + "…" : text; return t; }
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
        /** v34: the clothes the character changes into during the story, in order (the film's costume 1, 2, …). */
        public final List<Costume> costumes = new ArrayList<Costume>();
        public String label;          // name as the user wrote it (Hinglish), or null
        /** "The Coder Didi", "Robo-Dog": the role written after the name in the character list (never used for the look). */
        public String role = "";
        /** A voice only ("मधुर AI आवाज़", an announcer, a radio): heard, never standing on the stage. */
        public boolean voiceOnly;
        /** From the verbs the script uses for this character: +1 feminine forms, -1 masculine, 0 unknown. */
        public int genderHint;
        /** v34: the character riding this one (Durga for her lion), and the animal character this one rides; null when none. */
        public CharacterDef rider, mountChar;
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
        /** Spoken from off-screen ("एल्गोरा की आवाज़ …:"): the speaker is heard, not brought on to the stage. */
        public boolean offScreen;
        public String cue = "";          // nature cues read by the AI ("rain", "boat"…), not shown
        public static Beat direction(String t) { Beat b = new Beat(); b.type = DIRECTION; b.text = t; return b; }
    }

    /** The characters who stand in the film (voices from the air, a radio or a device are never pictured). */
    public List<CharacterDef> cast() {
        List<CharacterDef> out = new ArrayList<CharacterDef>();
        for (CharacterDef c : characters) if (!c.voiceOnly) out.add(c);
        return out;
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
