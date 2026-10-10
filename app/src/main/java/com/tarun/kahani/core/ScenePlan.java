package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;

/**
 * The scenes the director adds to the script's own (v26): a journey into every new place — the place alone,
 * established before anyone speaks there (the phone guide 8.2, the manual 3.2). Each added scene has a picture
 * of its own the user can give (up to 10 pictures, 10 angles each) at the film-making screen; without one the
 * director uses the place's own picture with a slow push.
 */
public final class ScenePlan {
    private ScenePlan() {}

    /** One scene the director adds before a scene of the script. */
    public static final class Extra {
        /** The script scene it comes before. */
        public int before;
        /** The manifest key of its picture: scene|<key>|file ("3j" = the journey into part 3). */
        public String key;
        /** What it shows, for the user. */
        public String label;
        /** Why the director adds it. */
        public String why;
    }

    /** The key of the picture of the journey into a scene. */
    public static String journeyKey(int sceneNumber) { return sceneNumber + "j"; }

    /** True when the director establishes the place alone before scene index si (the place changes there). */
    public static boolean bridgeBefore(Story story, int si) {
        if (si <= 0 || si >= story.scenes.size()) return false;
        return setOf(story.scenes.get(si)) != setOf(story.scenes.get(si - 1));
    }

    static int setOf(Story.Scene sc) {
        return Sets.forScene(sc);
    }

    /** Every scene the director adds to this story, in order. */
    public static List<Extra> extras(Story story) {
        List<Extra> out = new ArrayList<Extra>();
        for (int si = 0; si < story.scenes.size(); si++) {
            if (!bridgeBefore(story, si)) continue;
            Story.Scene sc = story.scenes.get(si);
            Extra e = new Extra();
            e.before = sc.number;
            e.key = journeyKey(sc.number);
            String place = Bible.firstClauseOf(sc.setting.length() > 0 ? sc.setting : sc.title);
            e.label = "On the way to " + place + " — the director's added scene before part " + sc.number;
            e.why = "the place changes here: it is shown on its own (a road, a gate, the place from afar) before anyone speaks in it";
            out.add(e);
        }
        return out;
    }
}
