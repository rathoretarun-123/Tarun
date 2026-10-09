package com.tarun.kahani.core;

import java.util.Locale;

/** Small text helpers that work for Hindi (Devanagari) and English scripts. */
public final class Txt {
    private Txt() {}

    public static String digitsToAscii(String s) {
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c >= '०' && c <= '९') b.append((char) ('0' + (c - '०')));
            else b.append(c);
        }
        return b.toString();
    }

    /** Normalises nukta / chandrabindu variations so "ज़" matches "ज", "हँसी" matches "हंसी" etc. */
    public static String norm(String s) {
        if (s == null) return "";
        StringBuilder b = new StringBuilder(s.length());
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '़') continue;                 // nukta
            if (c == 'ँ') c = 'ं';             // chandrabindu -> anusvara
            if (c == '‌' || c == '‍') continue; // zero width joiners
            b.append(c);
        }
        return b.toString().toLowerCase(Locale.ROOT);
    }

    public static boolean has(String hay, String... needles) {
        String h = norm(hay);
        for (String n : needles) if (h.contains(norm(n))) return true;
        return false;
    }

    /** True if any needle is in the text as a whole word (no letter right before or after it): "mall" is not in "small". */
    public static boolean hasWord(String hay, String... needles) {
        String h = norm(hay);
        for (String n : needles) {
            String x = norm(n);
            if (x.isEmpty()) continue;
            int i = h.indexOf(x);
            while (i >= 0) {
                boolean l = i == 0 || !Character.isLetterOrDigit(h.charAt(i - 1));
                int e = i + x.length();
                boolean r = e >= h.length() || !Character.isLetterOrDigit(h.charAt(e));
                if (l && r) return true;
                i = h.indexOf(x, i + 1);
            }
        }
        return false;
    }

    public static int firstIndex(String hay, String... needles) {
        String h = norm(hay);
        int best = -1;
        for (String n : needles) {
            int i = h.indexOf(norm(n));
            if (i >= 0 && (best < 0 || i < best)) best = i;
        }
        return best;
    }

    public static boolean isDevanagari(char c) { return c >= 'ऀ' && c <= 'ॿ'; }

    public static boolean mostlyHindi(String s) {
        int dev = 0, lat = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (isDevanagari(c)) dev++;
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) lat++;
        }
        return dev >= lat;
    }

    public static String stripQuotes(String s) {
        s = s.trim();
        while (s.length() > 0 && "\"“”'‘’«»".indexOf(s.charAt(0)) >= 0) s = s.substring(1).trim();
        while (s.length() > 0 && "\"“”'‘’«»".indexOf(s.charAt(s.length() - 1)) >= 0) s = s.substring(0, s.length() - 1).trim();
        return s;
    }

    /** Removes "(...)" groups. */
    public static String withoutParens(String s) {
        StringBuilder b = new StringBuilder();
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') { if (depth > 0) depth--; }
            else if (depth == 0) b.append(c);
        }
        return b.toString().replaceAll("\\s+", " ").trim();
    }

    public static String clean(String s) {
        return s.replace(' ', ' ').replaceAll("[ \\t]+", " ").trim();
    }

    /** Text suitable for a speech engine: drop stage noise, keep pauses. */
    public static String forSpeech(String s) {
        String t = s.replace("…", ", ").replace("...", ", ").replace("—", ", ").replace("–", ", ");
        t = t.replaceAll("[\"“”«»]", "");
        t = t.replaceAll("\\s+", " ").trim();
        return t;
    }
}
