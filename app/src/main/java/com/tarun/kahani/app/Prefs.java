package com.tarun.kahani.app;

import android.content.Context;
import android.content.SharedPreferences;

import com.tarun.kahani.core.Cloud;

/** App-wide settings kept privately on the phone: signed-in Gmail account and optional free AI key. */
public final class Prefs {
    private Prefs() {}

    static SharedPreferences sp(Context c) { return c.getSharedPreferences("kahani", Context.MODE_PRIVATE); }

    public static String get(Context c, String k, String def) { return sp(c).getString(k, def); }
    public static void put(Context c, String k, String v) { sp(c).edit().putString(k, v).apply(); }

    public static String account(Context c) { return get(c, "account", ""); }
    public static void setAccount(Context c, String email) { put(c, "account", email == null ? "" : email); }
    public static boolean skippedLogin(Context c) { return "1".equals(get(c, "skipLogin", "0")); }

    public static String geminiKey(Context c) { return get(c, "geminiKey", ""); }
    public static boolean online(Context c) { return !"0".equals(get(c, "online", "1")); }
    /** Natural neural voices over the internet (free, no key). On by default. */
    public static boolean naturalVoices(Context c) { return !"0".equals(get(c, "naturalVoices", "1")); }
    /** The studio creates missing character/background pictures with free AI (3D animated style) when online. */
    public static boolean autoArt(Context c) { return !"0".equals(get(c, "autoArt", "1")); }
    /** Free pictures of the story's objects from GitHub (Fluent Emoji 3D, MIT) for inserts. */
    public static boolean freeObjects(Context c) { return !"0".equals(get(c, "freeObjects", "1")); }
    /** Studio 3D: whatever still has no picture is built in three dimensions on the phone (no internet needed). */
    public static boolean studio3d(Context c) { return !"0".equals(get(c, "studio3d", "1")); }
    /** Faster drawing: a mesh cell of two pixels instead of one (about twice as fast, a little less smooth). */
    public static boolean fastMesh(Context c) { return "1".equals(get(c, "fastMesh", "0")); }
    /** The director asks before any picture the studio made in 3D is used (a proposal with Use / Reject). */
    public static boolean ask3d(Context c) { return !"0".equals(get(c, "ask3d", "1")); }
    /** An image-to-3D model service key (Meshy), entered by the user, kept only on the phone. */
    public static String meshyKey(Context c) { return get(c, "meshyKey", "").trim(); }
    /** Free 3D character models from GitHub (CC0 / CC-BY) for characters without a picture, when online. */
    public static boolean freeModels(Context c) { return !"0".equals(get(c, "freeModels", "1")); }
    /** The free image-to-3D demos on Hugging Face Spaces (no key, slow, may be asleep) for the views of a picture, when online. */
    public static boolean freeSpaces(Context c) { return !"0".equals(get(c, "freeSpaces", "1")); }
    /** Human QC (protocol step 4): the director shows the first frame of every shot and waits for the user's check. */
    public static boolean humanQc(Context c) { return !"0".equals(get(c, "humanQc", "1")); }
    public static boolean aiVoices(Context c) { return "1".equals(get(c, "aiVoices", "0")); }
    /** Spider-Verse animation on twos: characters step by skill (experts 24, learners 12, rebels 8 fps). Off by default (smooth). */
    public static boolean onTwos(Context c) { return "1".equals(get(c, "onTwos", "0")); }

    /** A Cloud client configured from the settings. */
    public static Cloud cloud(Context c) {
        Cloud cl = new Cloud();
        cl.geminiKey = geminiKey(c);
        cl.freesoundKey = get(c, "freesoundKey", "");
        cl.pixabayKey = get(c, "pixabayKey", "");
        cl.pexelsKey = get(c, "pexelsKey", "");
        return cl;
    }
}
