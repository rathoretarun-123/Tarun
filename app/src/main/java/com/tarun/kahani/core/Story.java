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

    public static final class CharacterDef {
        public String id;            // stable key
        public String displayName;   // e.g. "वृंदा"
        public String fullName;      // e.g. "बड़ी राजकुमारी वृंदा"
        public final List<String> aliases = new ArrayList<String>();
        public String description = "";
        public int age = -1;
        public boolean fromScript;   // false when invented for an unknown speaker
        public Look look;
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
        public final List<Beat> beats = new ArrayList<Beat>();
    }

    public static final class Beat {
        public static final int DIRECTION = 0, DIALOGUE = 1;
        public int type;
        public String speakerRaw = "";
        public CharacterDef speaker;
        public String manner = "";       // text inside parentheses after speaker
        public String text = "";
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
