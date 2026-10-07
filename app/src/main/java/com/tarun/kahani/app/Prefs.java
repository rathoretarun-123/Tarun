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
    public static boolean aiVoices(Context c) { return "1".equals(get(c, "aiVoices", "1")); }

    /** A Cloud client configured from the settings. */
    public static Cloud cloud(Context c) {
        Cloud cl = new Cloud();
        cl.geminiKey = geminiKey(c);
        return cl;
    }
}
