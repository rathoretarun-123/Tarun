package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads a screenplay written in the common Hindi/English format:
 * character & place descriptions (reference only, never narrated), a title line,
 * then "दृश्य N: title" blocks with (stage directions) and  Name (manner): "dialogue".
 */
public final class ScriptParser {

    private static final Pattern SCENE = Pattern.compile(
            "^\\s*[#*]*\\s*(दृश्य|द्रश्य|सीन|Scene|SCENE|scene|अंक|भाग)\\s*[-–]?\\s*([0-9]+)\\s*[:：.\\-–—)]*\\s*(.*)$");
    private static final Pattern NUMBERED = Pattern.compile("^\\s*([0-9]+)\\s*[.)]\\s*(.+)$");
    private static final Pattern AGE = Pattern.compile("([0-9]+)\\s*(वर्ष|साल|बरस|years?|yrs?|yo)", Pattern.CASE_INSENSITIVE);

    static final String[] TITLE_WORDS = {"बड़ी", "छोटी", "बड़ा", "छोटा", "बाकी", "राजकुमारी", "राजकुमार", "गार्ड", "गार्ड्स",
            "the", "princess", "prince", "guard", "guards", "little", "big", "old", "young", "और", "का", "की", "के"};

    public static Story parse(String raw) {
        Story story = new Story();
        String text = Txt.digitsToAscii(raw.replace("\r\n", "\n").replace('\r', '\n'));
        String[] lines = text.split("\n");

        int firstScene = -1;
        for (int i = 0; i < lines.length; i++) {
            if (SCENE.matcher(Txt.clean(lines[i])).matches()) { firstScene = i; break; }
        }
        if (firstScene < 0) {
            // No scene headings: treat the whole thing as one scene.
            story.warnings.add("कोई 'दृश्य 1' शीर्षक नहीं मिला — पूरी कहानी को एक दृश्य माना गया।");
            firstScene = 0;
            parsePreamble(story, new String[0]);
            Story.Scene sc = new Story.Scene();
            sc.number = 1; sc.heading = "दृश्य 1";
            story.scenes.add(sc);
            parseSceneLines(story, sc, lines, 0, lines.length);
        } else {
            String[] pre = new String[firstScene];
            System.arraycopy(lines, 0, pre, 0, firstScene);
            parsePreamble(story, pre);
            Story.Scene cur = null;
            int start = -1;
            for (int i = firstScene; i <= lines.length; i++) {
                Matcher m = i < lines.length ? SCENE.matcher(Txt.clean(lines[i])) : null;
                if (i == lines.length || m.matches()) {
                    if (cur != null) parseSceneLines(story, cur, lines, start, i);
                    if (i == lines.length) break;
                    cur = new Story.Scene();
                    cur.number = Integer.parseInt(m.group(2));
                    cur.heading = m.group(1).trim() + " " + cur.number;
                    cur.title = Txt.stripQuotes(m.group(3).trim());
                    story.scenes.add(cur);
                    start = i + 1;
                }
            }
            String w = Txt.clean(lines[firstScene]);
            Matcher m = SCENE.matcher(w);
            if (m.matches()) story.sceneWord = m.group(1);
        }

        story.hindi = Txt.mostlyHindi(raw);
        if (story.title.length() == 0) {
            story.title = story.scenes.size() > 0 && story.scenes.get(0).title.length() > 0
                    ? story.scenes.get(0).title : (story.hindi ? "मेरी कहानी" : "My Story");
        }
        resolveSpeakers(story);
        LookDesigner.designAll(story);
        return story;
    }

    // ------------------------------------------------------------------ preamble

    private static boolean isCharHeader(String l) {
        return !NUMBERED.matcher(l).matches() && Txt.has(l, "पात्र", "किरदार", "character", "cast") && l.length() < 120;
    }

    private static boolean isPlaceHeader(String l) {
        return !NUMBERED.matcher(l).matches() && Txt.has(l, "स्थानों", "स्थान का", "स्थान :", "स्थान:", "जगहों", "places", "locations", "settings")
                && l.length() < 120;
    }

    private static void parsePreamble(Story story, String[] pre) {
        // Find explicit or implicit title: last short non-description line before the first scene.
        int titleIdx = -1;
        for (int i = pre.length - 1; i >= 0; i--) {
            String l = Txt.clean(pre[i]);
            if (l.length() == 0) continue;
            if (Txt.has(l, "शीर्षक", "title:")) { titleIdx = i; break; }
            boolean bullet = l.startsWith("*") || l.startsWith("-") || l.startsWith("•");
            boolean sentence = l.endsWith("।") || l.endsWith("|") || (l.endsWith(".") && l.length() > 60);
            if (!bullet && !sentence && !NUMBERED.matcher(l).matches() && l.length() <= 100
                    && !isCharHeader(l) && !isPlaceHeader(l) && !l.endsWith(":")) {
                titleIdx = i;
            }
            break;
        }
        if (titleIdx >= 0) {
            String t = Txt.clean(pre[titleIdx]).replaceFirst("^(शीर्षक|Title|TITLE)\\s*[:：]\\s*", "");
            t = t.replaceAll("^[#*\\s]+", "").replaceAll("[*\\s]+$", "");
            int p = t.indexOf('(');
            if (p > 0 && t.endsWith(")")) {
                story.subtitle = t.substring(p + 1, t.length() - 1).trim();
                t = t.substring(0, p).trim();
            }
            story.title = Txt.stripQuotes(t);
        }

        int mode = 0; // 0 unknown, 1 chars, 2 places
        String header = null;
        StringBuilder desc = new StringBuilder();
        int entryMode = 0;
        for (int i = 0; i < pre.length; i++) {
            if (i == titleIdx) continue;
            String l = Txt.clean(pre[i]);
            if (l.length() == 0) continue;
            if (isPlaceHeader(l)) { flushEntry(story, entryMode, header, desc); header = null; mode = 2; continue; }
            if (isCharHeader(l)) { flushEntry(story, entryMode, header, desc); header = null; mode = 1; continue; }
            Matcher m = NUMBERED.matcher(l);
            if (m.matches()) {
                flushEntry(story, entryMode, header, desc);
                header = m.group(2).trim();
                if (header.endsWith(":") || header.endsWith("：")) header = header.substring(0, header.length() - 1).trim();
                // "Name: description" on one line
                int colon = colonOutsideParens(header);
                if (colon > 0) {
                    desc.append(header.substring(colon + 1).trim()).append('\n');
                    header = header.substring(0, colon).trim();
                }
                entryMode = mode == 0 ? 1 : mode;
                continue;
            }
            if (header != null) desc.append(l.replaceFirst("^[*•\\-]\\s*", "")).append('\n');
        }
        flushEntry(story, entryMode, header, desc);
    }

    private static int colonOutsideParens(String s) {
        int depth = 0;
        for (int i = 0; i < s.length(); i++) {
            char c = s.charAt(i);
            if (c == '(') depth++;
            else if (c == ')') depth = Math.max(0, depth - 1);
            else if ((c == ':' || c == '：') && depth == 0) return i;
            else if (c == '"' || c == '“') return -1;
        }
        return -1;
    }

    private static void flushEntry(Story story, int mode, String header, StringBuilder desc) {
        if (header == null) { desc.setLength(0); return; }
        String d = desc.toString().trim();
        desc.setLength(0);
        if (mode == 2) {
            Story.PlaceDef p = new Story.PlaceDef();
            p.name = header;
            p.description = d;
            story.places.add(p);
            return;
        }
        String name = Txt.withoutParens(header);
        String paren = "";
        int a = header.indexOf('('), b = header.lastIndexOf(')');
        if (a >= 0 && b > a) paren = header.substring(a + 1, b);
        int age = findAge(header + " " + d);

        // Group entry: "बाकी गार्ड्स (सिम्बा, सिंघम, रोहू)"
        String[] parts = paren.split("[,،]|\\s+और\\s+|\\s+and\\s+");
        boolean group = parts.length >= 2;
        for (String p : parts) {
            String q = p.trim();
            if (q.length() == 0 || q.split("\\s+").length > 2 || q.matches(".*[0-9].*")) group = false;
        }
        if (group) {
            int v = 0;
            for (String p : parts) {
                Story.CharacterDef c = newChar(story, p.trim(), header + "\n" + d, age);
                c.aliases.add(p.trim());
                c.look = null;
                c.description = header + "\n" + d;
                c.id = c.id + "";
                v++;
            }
            return;
        }
        Story.CharacterDef c = newChar(story, name, header + "\n" + d, age);
        c.fullName = name;
    }

    private static Story.CharacterDef newChar(Story story, String name, String desc, int age) {
        Story.CharacterDef c = new Story.CharacterDef();
        c.id = "c" + story.characters.size();
        c.fullName = name;
        c.displayName = name;
        c.description = desc;
        c.age = age;
        c.fromScript = true;
        story.characters.add(c);
        return c;
    }

    static int findAge(String s) {
        Matcher m = AGE.matcher(s);
        if (m.find()) {
            try { return Integer.parseInt(m.group(1)); } catch (NumberFormatException e) { return -1; }
        }
        return -1;
    }

    // ------------------------------------------------------------------ scenes

    private static void parseSceneLines(Story story, Story.Scene sc, String[] lines, int from, int to) {
        for (int i = from; i < to; i++) {
            String l = Txt.clean(lines[i]);
            if (l.length() == 0) continue;
            l = l.replaceAll("^[*#]+\\s*", "").replaceAll("\\*\\*", "");
            if (l.startsWith("(") || l.startsWith("[")) {
                String inner = l.substring(1);
                if (inner.endsWith(")") || inner.endsWith("]")) inner = inner.substring(0, inner.length() - 1);
                inner = inner.trim();
                String low = Txt.norm(inner);
                if (sc.setting.length() == 0 && (low.startsWith(Txt.norm("स्थान")) || low.startsWith("location") || low.startsWith("place")
                        || low.startsWith("setting"))) {
                    int c = inner.indexOf(':');
                    if (c < 0) c = inner.indexOf('：');
                    sc.setting = c >= 0 ? inner.substring(c + 1).trim() : inner;
                }
                sc.beats.add(Story.Beat.direction(inner));
                continue;
            }
            int colon = colonOutsideParens(l);
            if (colon > 0 && colon <= 260) {
                String who = l.substring(0, colon).trim();
                String said = l.substring(colon + 1).trim();
                if (!who.contains("।") && said.length() > 0 && Txt.withoutParens(who).length() > 0
                        && Txt.withoutParens(who).length() <= 40 && Txt.withoutParens(who).split("\\s+").length <= 5) {
                    Story.Beat b = new Story.Beat();
                    b.type = Story.Beat.DIALOGUE;
                    b.speakerRaw = Txt.withoutParens(who);
                    int a = who.indexOf('('), z = who.lastIndexOf(')');
                    if (a >= 0 && z > a) b.manner = who.substring(a + 1, z).trim();
                    b.text = Txt.stripQuotes(said);
                    // A dialogue may itself contain (action) parts: move them to manner.
                    if (b.text.startsWith("(")) {
                        int zz = b.text.indexOf(')');
                        if (zz > 0) {
                            b.manner = (b.manner + ", " + b.text.substring(1, zz)).trim();
                            b.text = Txt.stripQuotes(b.text.substring(zz + 1));
                        }
                    }
                    if (b.text.length() > 0) { sc.beats.add(b); continue; }
                }
            }
            if (Txt.has(l, "स्थान:") && sc.setting.length() == 0) {
                sc.setting = l.substring(l.indexOf(':') + 1).trim();
            }
            sc.beats.add(Story.Beat.direction(l));
        }
    }

    // ------------------------------------------------------------------ speakers & aliases

    static boolean isTitleWord(String w) {
        String n = Txt.norm(w);
        for (String t : TITLE_WORDS) if (Txt.norm(t).equals(n)) return true;
        return false;
    }

    private static void buildAliases(Story story) {
        Map<String, Integer> freq = new HashMap<String, Integer>();
        for (Story.CharacterDef c : story.characters) {
            for (String tok : c.fullName.split("[\\s\\-]+")) {
                String n = Txt.norm(tok);
                if (n.length() < 2) continue;
                Integer f = freq.get(n);
                freq.put(n, f == null ? 1 : f + 1);
            }
        }
        for (Story.CharacterDef c : story.characters) {
            if (!c.aliases.contains(c.fullName)) c.aliases.add(0, c.fullName);
            for (String tok : c.fullName.split("[\\s\\-]+")) {
                String n = Txt.norm(tok);
                if (n.length() < 2 || isTitleWord(tok)) continue;
                Integer f = freq.get(n);
                if (f != null && f == 1 && !c.aliases.contains(tok)) c.aliases.add(tok);
            }
        }
    }

    static Story.CharacterDef resolve(Story story, String speaker) {
        String s = Txt.norm(speaker);
        Story.CharacterDef best = null;
        int bestScore = 0;
        for (Story.CharacterDef c : story.characters) {
            int score = 0;
            String full = Txt.norm(c.fullName);
            if (full.equals(s)) score = 1000;
            else if (full.contains(s) && s.length() >= 2) score = 500 + s.length();
            for (String a : c.aliases) {
                String na = Txt.norm(a);
                if (na.length() >= 2 && s.contains(na)) score = Math.max(score, 100 + na.length() * 4);
                if (na.equals(s)) score = Math.max(score, 900);
            }
            if (score > bestScore) { bestScore = score; best = c; }
        }
        return best;
    }

    private static void resolveSpeakers(Story story) {
        buildAliases(story);
        Map<Story.CharacterDef, Map<String, Integer>> used = new HashMap<Story.CharacterDef, Map<String, Integer>>();
        for (Story.Scene sc : story.scenes) {
            for (Story.Beat b : sc.beats) {
                if (b.type != Story.Beat.DIALOGUE) continue;
                Story.CharacterDef c = resolve(story, b.speakerRaw);
                if (c == null) {
                    c = newChar(story, b.speakerRaw, "", -1);
                    c.fromScript = false;
                    c.aliases.add(b.speakerRaw);
                    story.warnings.add("'" + b.speakerRaw + "' का विवरण पात्र-सूची में नहीं है — रूप अपने आप बनाया गया।");
                }
                b.speaker = c;
                Map<String, Integer> m = used.get(c);
                if (m == null) { m = new HashMap<String, Integer>(); used.put(c, m); }
                Integer n = m.get(b.speakerRaw);
                m.put(b.speakerRaw, n == null ? 1 : n + 1);
            }
        }
        for (Story.CharacterDef c : story.characters) {
            Map<String, Integer> m = used.get(c);
            if (m != null) {
                String best = null; int bn = 0;
                for (Map.Entry<String, Integer> e : m.entrySet()) if (e.getValue() > bn) { bn = e.getValue(); best = e.getKey(); }
                c.displayName = best;
            } else {
                StringBuilder sb = new StringBuilder();
                for (String tok : c.fullName.split("\\s+")) {
                    if (isTitleWord(tok)) continue;
                    if (sb.length() > 0) sb.append(' ');
                    sb.append(tok);
                }
                c.displayName = sb.length() > 0 ? sb.toString() : c.fullName;
            }
        }
    }

    /** Characters mentioned in a piece of text (word-boundary aware), in order of first mention. */
    public static List<Story.CharacterDef> mentions(Story story, String text) { return mentions(story, text, false); }

    /**
     * Characters physically present in a stage direction: like mentions(), but a name used only as a
     * possessive ("वानुषा का रिबन", "वानुषा के चेहरे") does not put that character on stage.
     */
    public static List<Story.CharacterDef> present(Story story, String text) { return mentions(story, text, true); }

    private static boolean possessive(String t, int end) {
        String rest = t.substring(Math.min(end, t.length())).trim();
        if (!(rest.startsWith("का ") || rest.startsWith("की ") || rest.startsWith("के "))) return false;
        String after = rest.substring(3).trim();
        // "X के पास/साथ/सामने" means X is there
        return !(after.startsWith("पास") || after.startsWith("साथ") || after.startsWith("सामने") || after.startsWith("कंधे")
                || after.startsWith("हाथ") || after.startsWith("कान") || after.startsWith("सिर"));
    }

    private static List<Story.CharacterDef> mentions(Story story, String text, boolean presenceOnly) {
        List<Story.CharacterDef> out = new ArrayList<Story.CharacterDef>();
        final List<int[]> pos = new ArrayList<int[]>();
        String t = Txt.norm(text);
        for (int ci = 0; ci < story.characters.size(); ci++) {
            Story.CharacterDef c = story.characters.get(ci);
            int first = -1;
            for (String a : c.aliases) {
                String na = Txt.norm(a);
                if (na.length() < 2) continue;
                int from = 0;
                while (true) {
                    int i = t.indexOf(na, from);
                    if (i < 0) break;
                    boolean okL = i == 0 || !Txt.isDevanagari(t.charAt(i - 1)) && !Character.isLetter(t.charAt(i - 1));
                    int e = i + na.length();
                    boolean okR = e >= t.length() || !isWordChar(t.charAt(e));
                    if (okL && okR && !(presenceOnly && possessive(t, e))) { if (first < 0 || i < first) first = i; break; }
                    from = i + 1;
                }
            }
            if (first >= 0) pos.add(new int[]{first, ci});
        }
        java.util.Collections.sort(pos, new java.util.Comparator<int[]>() {
            public int compare(int[] a, int[] b) { return a[0] - b[0]; }
        });
        for (int[] p : pos) out.add(story.characters.get(p[1]));
        return out;
    }

    private static boolean isWordChar(char c) {
        if (Character.isLetterOrDigit(c) && !Txt.isDevanagari(c)) return true;
        // Devanagari letters (not punctuation like danda)
        return c >= 'ऀ' && c <= 'ॣ' && c != '।' && c != '॥';
    }
}
