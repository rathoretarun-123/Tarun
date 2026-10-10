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
            "the", "princess", "prince", "guard", "guards", "little", "big", "old", "young", "और", "का", "की", "के",
            // honorifics are never a name on their own ("गेम वाली आंटी हूँ" does not call Aunty Algora on to the stage)
            "आंटी", "आँटी", "अंकल", "दीदी", "भैया", "भाई", "मिस्टर", "मिस", "मिसेज", "सर", "मैडम", "डॉक्टर", "बाबा", "दादा", "दादी", "नानी", "नाना", "चाचा", "चाची", "मामा", "मामी", "बुआ", "मौसी",
            "aunty", "auntie", "aunt", "uncle", "mister", "mr", "mrs", "ms", "miss", "sir", "madam", "dr", "doctor", "grandpa", "grandma", "papa", "mama", "mummy", "daddy"};

    /**
     * Reads a script in Hindi, English, Hinglish (Hindi in English letters) or a mix: every Hinglish line is turned
     * into Devanagari for the Hindi voices and the director, English lines stay English (spoken by English voices).
     */
    public static Story parse(String raw) {
        if (raw == null) raw = "";
        java.util.Map<String, String> back = new java.util.HashMap<String, String>();
        List<String> converted = new ArrayList<String>();
        String text = Hinglish.convertScript(raw, back, converted);
        Story s = parseText(text);
        if (!back.isEmpty()) {
            s.hinglish = true;
            s.back = back;
            s.converted.addAll(converted);
            s.title = Hinglish.back(s.title, back);
            s.subtitle = Hinglish.back(s.subtitle, back);
        }
        // the film's main language: the language most dialogue is in
        int hi = 0, en = 0;
        for (Story.Scene sc : s.scenes) for (Story.Beat b : sc.beats) {
            if (b.type != Story.Beat.DIALOGUE) continue;
            if (Txt.mostlyHindi(b.text)) hi++; else en++;
        }
        if (hi + en > 0) s.hindi = hi >= en;
        return s;
    }

    static Story parseText(String raw) {
        Story story = new Story();
        String text = Txt.digitsToAscii(raw.replace("\r\n", "\n").replace('\r', '\n'));
        String[] lines = text.split("\n");

        int firstScene = -1;
        for (int i = 0; i < lines.length; i++) {
            if (SCENE.matcher(Txt.clean(lines[i])).matches()) { firstScene = i; break; }
        }
        if (firstScene < 0) {
            // No scene headings: treat the whole thing as one scene.
            story.warnings.add("No 'Scene 1' / 'दृश्य 1' heading found — the whole story is treated as one scene.");
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
            buildAliases(story);      // the scenes are read knowing the names ("तारा अपनी LED क्लिप ठीक करते हुए: …")
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
        placesOfScenes(story);
        genderFromVerbs(story);
        LookDesigner.designAll(story);
        linkMounts(story);
        aidsFromActions(story);
        costumesFromActions(story);
        return story;
    }

    /**
     * v34: a rider (Durga, look.mount = lion) and the animal character of the story that is her mount (a lion of the
     * cast, or a character whose name says "शेर" / "lion"): the two are one picture on the stage, and each speaks with
     * its own mouth — the rider's lips and the animal's jaw.
     */
    /**
     * v34: a walking stick, crutches or a wheelchair named only in the action ("दादाजी लाठी टेकते हुए आते हैं",
     * "Grandpa rolls in on his wheelchair") belongs to the one character the sentence names.
     */
    static void aidsFromActions(Story story) {
        for (Story.Scene sc : story.scenes) for (Story.Beat b : sc.beats) {
            if (b.type != Story.Beat.DIRECTION) continue;
            for (String sent : b.text.split("[।.!?]")) {
                List<Story.CharacterDef> who = mentions(story, sent);
                // v34: an umbrella carried in the action ("मीरा छाता लेकर निकलती है") opens over her head in the rain
                if (who.size() == 1 && who.get(0).look != null && who.get(0).look.isHumanoid() && Txt.has(sent, "छाता", "छतरी", "umbrella")) who.get(0).look.umbrella = true;
                // v35: a limp, a missing arm or leg, blindness or deafness told only by the action ("राजू लंगड़ाता हुआ आता है")
                if (who.size() == 1 && who.get(0).look != null && who.get(0).look.isHumanoid()) {
                    int cond = LookDesigner.conditionIn(sent) & (Look.C_LIMP | Look.C_NO_ARM | Look.C_RIGHT_ARM | Look.C_NO_LEG | Look.C_RIGHT_LEG | Look.C_ARTIFICIAL_LEG | Look.C_DEAF | Look.C_SIGNS);
                    who.get(0).look.condition |= cond;
                }
                if (who.size() != 1 || who.get(0).look == null || who.get(0).look.aid != Look.AID_NONE) continue;
                Look l = who.get(0).look;
                if (l.kind == Look.ANIMAL || l.kind == Look.BIRD) continue;
                int aid = LookDesigner.aidIn(sent, l.kind == Look.OLD_MAN);
                if (aid != Look.AID_NONE) { l.aid = aid; if (aid == Look.AID_STICK) l.wand = false; }
            }
        }
    }

    /**
     * v34: a change of clothes in the action ("मीरा लाल लहंगा पहनकर आती है", "Kabir changes into his school uniform",
     * "दादाजी चश्मा उतारते हैं"): the character the sentence names wears that look from then on. A turban or a crown
     * has its own handling (taken, put on); a sentence that changes nothing is no change.
     */
    static void costumesFromActions(Story story) {
        for (Story.Scene sc : story.scenes) for (int bi = 0; bi < sc.beats.size(); bi++) {
            Story.Beat b = sc.beats.get(bi);
            if (b.type != Story.Beat.DIRECTION) continue;
            for (String sent : b.text.split("[।.!?]")) {
                if (!LookDesigner.changesClothes(sent) || Txt.has(sent, "पगड़ी", "टोपी", "turban", " cap", "मुकुट", "crown")) continue;
                List<Story.CharacterDef> who = mentions(story, sent);
                // v34: "डॉक्टर रोहन के पैर पर पट्टी बाँधते हैं", "the nurse puts Asha's arm in a sling": the hurt one is the
                // one whose arm, leg or head it is
                if (who.size() > 1) who = owners(sent, who);
                if (who.size() != 1) continue;
                Story.CharacterDef c = who.get(0);
                if (c.look == null || !c.look.isHumanoid()) continue;
                Look cur = c.costumes.isEmpty() ? c.look : c.costumes.get(c.costumes.size() - 1).look;
                Look nl = LookDesigner.restyle(cur, sent);
                if (nl.outfit == cur.outfit && nl.primary == cur.primary && nl.secondary == cur.secondary && nl.glasses == cur.glasses && nl.injury == cur.injury) continue;
                Story.Costume k = new Story.Costume();
                k.scene = sc.number; k.beat = bi; k.text = sent.trim(); k.look = nl;
                c.costumes.add(k);
            }
        }
    }

    /** v34: of the characters a sentence names, the ones named as owners ("रोहन के", "आशा की", "Asha's"). */
    static List<Story.CharacterDef> owners(String sent, List<Story.CharacterDef> who) {
        List<Story.CharacterDef> out = new ArrayList<Story.CharacterDef>();
        String t = Txt.norm(sent);
        for (Story.CharacterDef c : who) for (String a : c.aliases) {
            String na = Txt.norm(a);
            if (na.length() < 2) continue;
            if (t.contains(na + " के ") || t.contains(na + " की ") || t.contains(na + "'s ") || t.contains(na + "’s ")) { out.add(c); break; }
        }
        return out;
    }

    static void linkMounts(Story story) {
        for (Story.CharacterDef r : story.characters) {
            if (r.look == null || r.look.mount < 0 || r.voiceOnly) continue;
            Story.CharacterDef best = null;
            for (Story.CharacterDef m : story.characters) {
                if (m == r || m.look == null || m.voiceOnly || m.rider != null) continue;
                boolean beast = m.look.kind == Look.ANIMAL || m.look.kind == Look.BIRD;
                boolean named = Txt.has(m.fullName + " " + m.displayName, Look.speciesWord(r.look.mount, true), Look.speciesWord(r.look.mount, false));
                if ((beast && m.look.species == r.look.mount) || named) { best = m; if (beast && m.look.species == r.look.mount) break; }
            }
            if (best != null) { r.mountChar = best; best.rider = r; }
        }
    }

    // ------------------------------------------------------------------ what the scenes leave unsaid

    static final String[] PLACE_STOP = {"का", "की", "के", "और", "में", "पर", "से", "एक", "the", "of", "and", "a", "an", "in", "at", "on"};

    /**
     * A scene without a (स्थान: …) line happens at the known place its title or first paragraph names
     * ("नियॉन गार्डन की सुबह" → the Neon Garden), else where the last scene was (the story simply goes on there).
     */
    static void placesOfScenes(Story st) {
        String prev = "";
        for (Story.Scene sc : st.scenes) {
            if (sc.setting.length() == 0) {
                String first = "";
                for (Story.Beat b : sc.beats) if (b.type == Story.Beat.DIRECTION) { first = b.text; break; }
                String head = sc.title + " । " + (first.length() > 240 ? first.substring(0, 240) : first);
                Story.PlaceDef p = placeNamedIn(st, head);
                if (p != null) sc.setting = p.name + (p.description.length() > 0 ? "। " + p.description : "");
                else if (prev.length() > 0 && Sets.detect(head) == Sets.GENERIC_OUT && !Sets.openLand(head) && !Txt.has(head, "बाहर", "outside", "सड़क", "street", "रास्त", "road", "सफ़र", "journey"))
                    sc.setting = prev;
            }
            prev = sc.setting;
        }
    }

    /** The place from the list whose name the text mentions (its distinctive words, whole), or null. */
    static Story.PlaceDef placeNamedIn(Story st, String text) {
        String t = " " + Txt.norm(text) + " ";
        Story.PlaceDef best = null;
        int bestN = 0;
        for (Story.PlaceDef p : st.places) {
            int n = 0;
            for (String tok : p.name.split("[\\s,،\\-–—()/]+")) {
                String w = Txt.norm(tok);
                if (w.length() < 3 || isStop(w)) continue;
                int i = t.indexOf(w);
                while (i >= 0) {
                    boolean l = !isWordChar(t.charAt(i - 1)), r = i + w.length() >= t.length() || !isWordChar(t.charAt(i + w.length()));
                    if (l && r) { n++; break; }
                    i = t.indexOf(w, i + 1);
                }
            }
            if (n > bestN) { bestN = n; best = p; }
        }
        return best;
    }

    static boolean isStop(String w) {
        for (String s : PLACE_STOP) if (Txt.norm(s).equals(w)) return true;
        return false;
    }

    static final String[] FEM_VERBS = {"ती है", "ती हैं", "ती हुई", "ती हुईं", "ती हूँ", "ती थी", "ती रही", "ती चली", "ई है", "ई थी", "ई और", "गई", "बैठी", "खड़ी", "रही है",
            "रही थी", "रही हो", "आई", "गयी", "चली", "लेती", "देती", "करती", "निकालती", "सोचती", "फुसफुसाती", "दौड़ती", "हँसती", "रोती", "चिल्लाती"};
    static final String[] MAL_VERBS = {"ता है", "ते हैं", "ता हुआ", "ते हुए", "ता हूँ", "ता था", "ता रहा", "ता चला", "या है", "या था", "गया", "बैठा", "खड़ा", "रहा है",
            "रहा था", "रहा हो", "आया", "चला", "लेता", "देता", "करता", "निकालता", "सोचता", "फुसफुसाता", "दौड़ता", "हँसता", "रोता", "चिल्लाता"};

    /** The script's own grammar says who is a she and who a he: the verb forms used where a character acts. */
    static void genderFromVerbs(Story st) {
        for (Story.CharacterDef c : st.characters) {
            int fem = 0, mal = 0;
            for (Story.Scene sc : st.scenes) for (Story.Beat b : sc.beats) {
                List<String> texts = new ArrayList<String>();
                if (b.type == Story.Beat.DIRECTION) for (String sent : b.text.split("[।.!?\\n]")) texts.add(sent);
                else if (b.speaker == c && b.manner.length() > 0) texts.add(b.manner);
                for (String sent : texts) {
                    List<Story.CharacterDef> ms = b.type == Story.Beat.DIRECTION ? mentions(st, sent) : java.util.Collections.singletonList(c);
                    if (ms.isEmpty() || ms.get(0) != c || (ms.size() > 1 && b.type == Story.Beat.DIRECTION && Txt.has(sent, "और", "and"))) continue;
                    String x = " " + Txt.norm(sent) + " ";
                    for (String v : FEM_VERBS) if (x.contains(Txt.norm(v) + " ") || x.contains(Txt.norm(v) + ",")) fem++;
                    for (String v : MAL_VERBS) if (x.contains(Txt.norm(v) + " ") || x.contains(Txt.norm(v) + ",")) mal++;
                }
            }
            c.genderHint = fem > mal ? 1 : mal > fem ? -1 : 0;
        }
    }

    // ------------------------------------------------------------------ preamble

    private static boolean isCharHeader(String l) {
        return !NUMBERED.matcher(l).matches() && Txt.has(l, "पात्र", "किरदार", "character", "cast") && l.length() < 120;
    }

    private static boolean isPlaceHeader(String l) {
        if (NUMBERED.matcher(l).matches()) return false;
        if (Txt.has(l, "स्थानों", "स्थान का", "स्थान :", "स्थान:", "जगहों", "places", "locations", "settings") && l.length() < 120) return true;
        // v39: a short heading line ("जगह का परिचय", "लोकेशन", "Location") — never a sentence of a description
        String h = l.replaceAll("[#*:：\\-–—=_\\s]+$", "").replaceAll("^[#*\\s]+", "").trim();
        return h.length() <= 30 && !h.contains("।") && h.split("\\s+").length <= 4
                && (Txt.has(h, "जगह", "लोकेशन", "स्थान परिचय", "सेटिंग") || Txt.hasWord(h, "location", "place", "setting"))
                && !Txt.has(h, "मुख्य जगह", "main location", "main place");
    }

    /** v39: the end of the lists before the script ("---", "स्क्रिप्ट शुरू", "Script starts"). */
    private static boolean listsEnd(String l) {
        String h = l.trim();
        return h.matches("^[-–—_*=~]{3,}$") || Txt.has(h, "स्क्रिप्ट शुरू", "कहानी शुरू", "पटकथा शुरू") || h.toLowerCase(java.util.Locale.ROOT).matches("^(the )?(script|story) (starts|begins).*");
    }

    /** A line that can be the film's title: short, not a sentence, list entry, heading or "Name: words" line. */
    private static boolean titleLike(String l, boolean allowColon) {
        boolean bullet = l.startsWith("*") || l.startsWith("-") || l.startsWith("•");
        boolean sentence = l.endsWith("।") || l.endsWith("|") || (l.endsWith(".") && l.length() > 60);
        if (bullet || sentence || NUMBERED.matcher(l).matches() || l.length() > 100 || isCharHeader(l) || isPlaceHeader(l) || l.endsWith(":")) return false;
        if (l.startsWith("\"") || l.startsWith("“") || l.startsWith("(")) return false;
        int colon = colonOutsideParens(l);
        if (colon > 0 && !allowColon) return false;
        // "Meera: "Hello"" is a spoken line, never a title
        return !(colon > 0 && l.substring(colon + 1).trim().matches("^[\"“].*"));
    }

    /** Words that start a description line ("Face: …", "पहनावा: …"), never a new character or place. */
    static final String[] FIELDS = {"चेहरा", "पहनावा", "पोशाक", "कपड़े", "शरीर", "रूप", "आवाज़", "आवाज", "उम्र", "स्वभाव", "बाल", "आँखें", "कद",
            "face", "dress", "outfit", "clothes", "costume", "body", "hair", "voice", "age", "personality", "nature", "look", "looks",
            "appearance", "height", "eyes", "skin", "role", "note", "notes", "description", "features", "behaviour", "behavior", "accessories",
            "सपना", "शौक", "पसंद", "आदत", "रिश्ता", "dream", "dreams", "hobby", "hobbies", "likes", "habit", "relation"};   // v39

    private static boolean fieldLabel(String head) {
        String h = Txt.norm(head.replaceAll("[*•\\-&]", " ")).trim();
        for (String f : FIELDS) if (h.equals(Txt.norm(f)) || h.startsWith(Txt.norm(f) + " ")) return true;
        return false;
    }

    private static boolean narratorName(String head) {
        return Txt.has(head, "narrator", "कथावाचक", "सूत्रधार", "वाचक", "writer", "लेखक", "note", "नोट");
    }

    private static void parsePreamble(Story story, String[] pre) {
        // The title: "Title: …" anywhere, else the last short line before the first scene, else the first line
        int titleIdx = -1;
        for (int i = 0; i < pre.length && titleIdx < 0; i++) if (Txt.has(Txt.clean(pre[i]), "शीर्षक", "title:")) titleIdx = i;
        for (int i = pre.length - 1; i >= 0 && titleIdx < 0; i--) {
            String l = Txt.clean(pre[i]);
            if (l.length() == 0) continue;
            if (titleLike(l, false)) titleIdx = i;
            break;
        }
        for (int i = 0; i < pre.length && titleIdx < 0; i++) {
            String l = Txt.clean(pre[i]);
            if (l.length() == 0) continue;
            if (titleLike(l, true)) titleIdx = i;
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
            // "कहानी - स्काई-लाइन के सोलर फूलों की चोरी": the story's name; a short line at the very top is then the title
            boolean named = t.matches("^(कहानी|Story|STORY)\\s*[-–—:：]\\s*.+");
            if (named) t = t.replaceFirst("^(कहानी|Story|STORY)\\s*[-–—:：]\\s*", "");
            story.title = Txt.stripQuotes(t);
            if (named) {
                int firstIdx = -1;
                for (int i = 0; i < titleIdx; i++) if (Txt.clean(pre[i]).length() > 0) { firstIdx = i; break; }
                if (firstIdx >= 0) {
                    String f = Txt.clean(pre[firstIdx]).replaceAll("^[#*\\s]+", "").replaceAll("[*\\s]+$", "");
                    if (titleLike(f, false) && f.length() <= 60 && !isCharHeader(f) && !isPlaceHeader(f) && !Txt.norm(f).equals(Txt.norm(story.title))) {
                        story.subtitle = story.subtitle.length() > 0 ? story.subtitle : story.title;
                        story.title = Txt.stripQuotes(f);
                    }
                }
            }
        }

        int mode = 0; // 0 unknown, 1 chars, 2 places
        String header = null;
        StringBuilder desc = new StringBuilder();
        int entryMode = 0;
        boolean numbered = false;   // the list numbers its entries ("1. Meera"): other "x: y" lines describe them
        for (int i = 0; i < pre.length; i++) {
            if (i == titleIdx) continue;
            String l = Txt.clean(pre[i]);
            if (l.length() == 0) continue;
            if (listsEnd(l)) { flushEntry(story, entryMode, header, desc); header = null; mode = 0; numbered = false; continue; }
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
                numbered = true;
                continue;
            }
            // unnumbered lists under a heading: "Meera (9 years): a curious girl…", "Village house – a mud house…"
            boolean bullet = l.startsWith("*") || l.startsWith("•");
            if (mode != 0 && !(numbered && header != null)) {
                String body = bullet ? l.substring(1).trim() : l;
                int colon = colonOutsideParens(body);
                String head = null, rest = "";
                if (colon > 0) { head = body.substring(0, colon).trim(); rest = body.substring(colon + 1).trim(); }
                else {
                    Matcher dm = Pattern.compile("^(.{2,40}?)\\s+[-–—]\\s+(.+)$").matcher(body);
                    if (dm.matches()) { head = dm.group(1).trim(); rest = dm.group(2).trim(); }
                }
                if (head != null && head.length() <= 40 && Txt.withoutParens(head).trim().split("\\s+").length <= 5 && !fieldLabel(head)) {
                    flushEntry(story, entryMode, header, desc);
                    header = null;
                    // a spoken line or a writer's note before the story starts: never a character, never acted
                    if (rest.startsWith("\"") || rest.startsWith("“") || narratorName(head)) continue;
                    header = head;
                    if (rest.length() > 0) desc.append(rest).append('\n');
                    entryMode = mode;
                    continue;
                }
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
            // v39: "मुख्य जगह: एक मॉडर्न फ्लैट का लिविंग रूम" — the label is no name; the words after it are
            if (Txt.has(header, "मुख्य जगह", "मुख्य स्थान", "जगह", "स्थान", "लोकेशन") && header.split("\\s+").length <= 2 || Txt.hasWord(header, "main location", "main place", "location", "place")) {
                String first = d.split("\n")[0].trim();
                if (first.length() > 0 && first.length() <= 80) p.name = first;
            }
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
        // "तारा मल्होत्रा - The Coder Didi", "बोल्ट (Robo-Dog)": a role written after the name is kept apart from the
        // look (a "Corporate Rakshas" is a man in a suit, not a monster)
        String role = "";
        Matcher rm = Pattern.compile("^(.{2,40}?)\\s+[-–—]\\s+(.{2,60})$").matcher(name.trim());
        if (rm.matches() && rm.group(2).trim().split("\\s+").length <= 6 && !rm.group(2).contains(":")) { name = rm.group(1).trim(); role = rm.group(2).trim(); }
        else if (paren.length() > 0 && paren.split("\\s+").length <= 4 && findAge(paren) < 0 && !paren.matches(".*[0-9].*")
                && Txt.has(paren, "robo", "robot", "dog", "witch", "kid", "boss", "chief", "captain", "रोबो", "बॉस", "कप्तान")) role = paren;
        // the description keeps the name, age and details; the role stays apart (c.role)
        String hdr = role.length() > 0 ? name + (paren.length() > 0 && !paren.equals(role) ? " (" + paren + ")" : "") : header;
        // v39: "वृंदा - 11 साल, बड़ी बेटी": an age and a relation after the name are who they are (a daughter is a girl),
        // not a role like "The Corporate Rakshas" — they stay in the description the look is read from
        if (role.length() > 0 && relationIn(role).length() > 0) hdr = name + " — " + role;
        Story.CharacterDef c = newChar(story, name, hdr + "\n" + d, age);
        c.fullName = name;
        c.role = role;
        c.voiceOnly = voiceOnlyName(name);
        // v39: what the family calls them is a name for them too ("पिता" → पापा, "माँ" → मम्मी): the library's pictures
        // of "पापा" are his, and "पापा" in a direction is him
        for (String al : relationAliases(relationIn(header + " " + role + " " + paren))) if (!c.aliases.contains(al)) c.aliases.add(al);
    }

    /** v39: relation words and what the family calls such a one. */
    static final String[][] RELATIONS = {
            {"पिता", "पापा", "पिताजी", "पापाजी", "डैडी", "papa", "dad", "daddy", "father"},
            {"पापा", "पिता", "पिताजी", "papa", "dad", "daddy", "father"},
            {"father", "papa", "dad", "daddy", "पापा", "पिता"},
            {"माँ", "मम्मी", "मां", "माता", "मम्मा", "मॉम", "mummy", "mom", "mommy", "mother", "maa"},
            {"माता", "मम्मी", "माँ", "मां", "mummy", "mom", "mother"},
            {"मम्मी", "माँ", "मां", "माता", "mummy", "mom", "mother"},
            {"mother", "mummy", "mom", "माँ", "मम्मी"},
            {"दादी", "दादी माँ", "grandma", "granny"}, {"दादा", "दादाजी", "grandpa"}, {"नानी", "नानी माँ", "nani"}, {"नाना", "नानाजी", "nana"}};

    /** The first relation word named in a header or role ("42 साल, पिता" → "पिता"), or "". */
    static String relationIn(String s) {
        if (s == null) return "";
        for (String[] r : RELATIONS) if (wordIn(s, r[0])) return r[0];
        if (Txt.has(s, "बेटी", "बेटा", "पत्नी", "पति", "बहन", "भाई", "दीदी", "भैया", "daughter", "son", "wife", "husband", "sister", "brother", "साल", "वर्ष", "years"))
            return "_";
        return "";
    }

    static List<String> relationAliases(String rel) {
        List<String> out = new ArrayList<String>();
        if (rel.length() == 0 || rel.equals("_")) return out;
        for (String[] r : RELATIONS) if (r[0].equals(rel)) for (int i = 1; i < r.length; i++) out.add(r[i]);
        return out;
    }

    /** Sound words: a quoted "खटाक!" or "धड़ाम!" is a noise in the direction, not a line someone says. */
    static final String[] SOUND_WORDS = {"खटाक", "खट", "खटखट", "धड़ाम", "धड़ाम", "धम", "धम्म", "छपाक", "छप", "टन", "टनटन", "ठक", "ठकठक", "फुस्स", "फूँ", "फू", "भौं", "भौ",
            "बीप", "बूप", "टिम", "झिलमिल", "सन", "पप", "कर", "भिन", "छन", "घर्र", "चटाक", "चट", "धड़", "टप", "टक", "ठप", "क्लिक", "झन", "झनझन", "खड़", "खड़खड़",
            "सर्र", "फट", "फड़फड़", "गड़गड़", "कड़क", "कड़", "तड़", "तड़ाक", "चीं", "म्याऊँ", "भों", "क्वैक", "कूकू", "click", "clack", "bang", "boom", "crash", "splash",
            "beep", "boop", "thud", "crack", "snap", "whoosh", "zap", "buzz", "ding", "ring", "tick", "tock", "pop", "clang", "vroom", "swoosh", "ting", "tap", "knock"};

    static boolean soundWord(String q) {
        String[] ws = q.replaceAll("[!.…?,\\-–—\"“”']+", " ").trim().split("\\s+");
        if (ws.length == 0 || ws.length > 3) return false;
        for (String w : ws) {
            if (w.length() == 0) continue;
            boolean ok = false;
            for (String sw : SOUND_WORDS) if (Txt.norm(w).equals(Txt.norm(sw)) || Txt.norm(w).startsWith(Txt.norm(sw) + Txt.norm("-"))) { ok = true; break; }
            if (!ok) return false;
        }
        return true;
    }

    /** "मधुर AI आवाज़", "Announcer", "Radio": a voice, never a body on the stage. */
    static boolean voiceOnlyName(String name) {
        if (name == null) return false;
        return Txt.has(name, "आवाज़", "आवाज", "voice", "announcer", "announcement", "घोषणा", "radio", "रेडियो", "loudspeaker", "लाउडस्पीकर", "narration")
                || name.matches("(?s).*\\bAI\\b.*") || name.matches("(?s).*\\b(TV|टीवी|phone|फ़ोन|फोन|computer|कंप्यूटर|app|ऐप)\\b.*");
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
        String lastSpeaker = null;
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
                boolean quoted = said.startsWith("\"") || said.startsWith("“");
                // the whole prefix names a character ("राजा तरुण (गंभीरता से)", "बड़ी राजकुमारी वृंदा"): the plain form below
                String bare = Txt.withoutParens(who).trim();
                Story.CharacterDef direct = bare.length() > 0 ? resolve(story, bare) : null;
                boolean directOk = direct != null && (Txt.norm(direct.fullName).contains(Txt.norm(bare)) || aliasEquals(direct, bare));
                // "इनाया अपने tote bag से diary निकालते हुए: "…"" — a known character's name, then what they do, then the line
                // v38: "वृंदा और कृपा:", "वानुषा, वृंदा और कृपा (हँसते हुए):" — names joined: all of them say it (not the first one
                // doing something called "और कृपा")
                boolean joinedNames = !directOk && namesJoined(story, bare);
                String lead = quoted && !directOk && !joinedNames ? leadingCharacter(story, who) : null;
                if (lead != null) {
                    Story.Beat b = new Story.Beat();
                    b.type = Story.Beat.DIALOGUE;
                    b.speakerRaw = lead;
                    String rest = who.substring(lead.length()).trim().replaceFirst("^[,،\\-–—]+\\s*", "");
                    int a = rest.indexOf('('), z = rest.lastIndexOf(')');
                    String inside = a >= 0 && z > a ? rest.substring(a + 1, z).trim() : "";
                    rest = Txt.withoutParens(rest).trim();
                    b.manner = (rest + (inside.length() > 0 ? (rest.length() > 0 ? ", " : "") + inside : "")).trim();
                    // "एल्गोरा की आवाज़ मीठी होकर": heard from somewhere else, not brought on to the stage
                    b.offScreen = Txt.has(rest, "की आवाज़", "की आवाज", "'s voice", "voice of", "फ़ोन से", "फोन से", "स्पीकर से", "headset", "हेडसेट", "रेडियो से", "दूर से आवाज़");
                    b.text = Txt.stripQuotes(said);
                    if (b.text.length() > 0) { sc.beats.add(b); lastSpeaker = lead; continue; }
                }
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
                    if (b.text.length() > 0) { sc.beats.add(b); lastSpeaker = b.speakerRaw; continue; }
                }
            }
            if (Txt.has(l, "स्थान:") && sc.setting.length() == 0) {
                sc.setting = l.substring(l.indexOf(':') + 1).trim();
            }
            // a line someone speaks inside a stage direction: वो हँसता है - "हा हा हा! पकड़ लिया!" - और …
            List<Story.Beat> split = splitQuotes(story, l, lastSpeaker);
            if (split != null) {
                for (Story.Beat b : split) { sc.beats.add(b); if (b.type == Story.Beat.DIALOGUE) lastSpeaker = b.speakerRaw; }
                continue;
            }
            List<Story.CharacterDef> ms = mentions(story, l);
            if (!ms.isEmpty()) lastSpeaker = ms.get(0).fullName;
            sc.beats.add(Story.Beat.direction(l));
        }
    }

    /** v38: two or more character names joined by "और" / "," / "and" / "&" and nothing else ("वृंदा और कृपा"). */
    static boolean namesJoined(Story story, String bare) {
        String[] parts = bare.split("\\s+और\\s+|\\s*[,،&]\\s*|\\s+and\\s+|\\s+तथा\\s+|\\s+एवं\\s+");
        if (parts.length < 2) return false;
        for (String p : parts) {
            String q = p.trim();
            if (q.isEmpty() || q.split("\\s+").length > 3) return false;
            if (resolve(story, q) == null && looseResolve(story, q) == null) return false;
        }
        return true;
    }

    static boolean aliasEquals(Story.CharacterDef c, String s) {
        String n = Txt.norm(s);
        for (String a : c.aliases) if (Txt.norm(a).equals(n)) return true;
        return Txt.norm(c.fullName).equals(n) || (c.displayName != null && Txt.norm(c.displayName).equals(n));
    }

    /** The character whose name (or alias) the text starts with, as written there; null when it starts with no known name. */
    static String leadingCharacter(Story story, String who) {
        String w = Txt.norm(who);
        String best = null;
        for (Story.CharacterDef c : story.characters) {
            List<String> names = new ArrayList<String>(c.aliases);
            if (!names.contains(c.fullName)) names.add(c.fullName);
            for (String a : names) {
                String na = Txt.norm(a);
                if (na.length() < 2 || !w.startsWith(na)) continue;
                boolean okR = w.length() == na.length() || !isWordChar(w.charAt(na.length()));
                if (okR && (best == null || na.length() > Txt.norm(best).length())) best = who.substring(0, a.length()).trim().length() == a.length() ? a : who.substring(0, Math.min(who.length(), a.length()));
            }
        }
        if (best == null) return null;
        // the written form, with the same length as the alias (the text may differ in case or spelling marks)
        int n = best.length();
        return who.substring(0, Math.min(who.length(), n));
    }

    /** Words that make a quoted text something read on a screen or a sign, not something spoken. */
    static final String[] DISPLAY = {"notification", "नोटिफिकेशन", "screen", "स्क्रीन", "message", "मैसेज", "संदेश", "लिखा", "written", "error", "sign", "board",
            "बोर्ड", "letter", "चिट्ठी", "पत्र", "headset पर", "phone पर", "फ़ोन पर", "laptop पर", "लैपटॉप पर", "पॉप", "pop", "display", "banner", "poster", "पोस्टर"};
    static final String[] SAY_VERBS = {"कह", "बोल", "चिल्ला", "पूछ", "हँस", "हंस", "भौंक", "गा", "फुसफुसा", "गुर्रा", "चीख", "जवाब", "say", "said", "shout", "laugh",
            "bark", "whisper", "growl", "scream", "sing", "ask", "repl", "call", "cried", "cries", "yell"};

    /**
     * A stage direction with a spoken line inside it ("… हँसता है - "हा हा हा!" - और …") becomes: the direction before,
     * the line (spoken by the character the sentence is about, with the direction's words as the manner), and the
     * direction after. Quotes that are sounds ("टिम-टिम" करती) or texts on a screen stay in the direction. Returns
     * null when nothing in the line is spoken.
     */
    static List<Story.Beat> splitQuotes(Story story, String l, String lastSpeaker) {
        Matcher qm = Pattern.compile("[\"“]([^\"”]{2,200})[\"”]").matcher(l);
        List<Story.Beat> out = new ArrayList<Story.Beat>();
        int from = 0;
        boolean any = false;
        while (qm.find()) {
            String q = qm.group(1).trim();
            String before = l.substring(from, qm.start()), after = l.substring(qm.end());
            String b40 = before.length() > 60 ? before.substring(before.length() - 60) : before;
            boolean dash = before.trim().endsWith("-") || before.trim().endsWith("–") || before.trim().endsWith("—") || before.trim().endsWith(":");
            String a20 = after.length() > 25 ? after.substring(0, 25) : after;
            boolean sound = Txt.has(a20, "करके", "करता", "करती", "करते", "कर रह", "की आवाज़", "की आवाज", "होती", "होता") || q.matches("^[^\\s]{1,8}([\\-][^\\s]{1,8}){1,3}[!.…]*$")
                    || soundWord(q);
            boolean display = Txt.has(b40, DISPLAY);
            int words = q.split("\\s+").length;
            boolean speech = !sound && !display && (dash && (words >= 2 || q.matches(".*[!?।]$")) || (words >= 3 && Txt.has(b40, SAY_VERBS)));
            if (!speech) continue;
            // who speaks: the character the words before are about, else the one who spoke last
            String speaker = null;
            List<Story.CharacterDef> ms = mentions(story, before);
            for (Story.CharacterDef c : ms) if (!c.voiceOnly) { speaker = c.fullName; break; }
            if (speaker == null) speaker = lastSpeaker;
            if (speaker == null) continue;
            String pre = before.replaceAll("[\\s\\-–—:]+$", "").trim();
            if (pre.length() > 0) out.add(Story.Beat.direction(pre));
            Story.Beat b = new Story.Beat();
            b.type = Story.Beat.DIALOGUE;
            b.speakerRaw = speaker;
            b.text = Txt.stripQuotes(q);
            // the manner: the clause before the line ("जोर से हँसता है")
            String[] cl = pre.split("[।.!?]");
            String last = cl.length > 0 ? cl[cl.length - 1].trim() : "";
            if (last.length() > 0 && last.length() <= 80) b.manner = last;
            out.add(b);
            from = qm.end();
            any = true;
        }
        if (!any) return null;
        String tail = l.substring(from).replaceFirst("^[\\s\\-–—,]+", "").trim();
        if (tail.split("\\s+").length >= 3) out.add(Story.Beat.direction(tail));
        return out;
    }

    // ------------------------------------------------------------------ speakers & aliases

    /** "कथावाचक", "सूत्रधार", "Narrator", "वाचक" … speak as a voice-over, never stand on screen. */
    public static boolean isNarrator(String who) {
        return Txt.has(who, "कथावाचक", "सूत्रधार", "वाचक", "narrator", "voice over", "voiceover", "वॉयस ओवर", "कथाकार");
    }

    public static boolean isTitleWord(String w) {
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

    public static Story.CharacterDef resolve(Story story, String speaker) {
        String s = Txt.norm(speaker);
        Story.CharacterDef best = null;
        int bestScore = 0;
        for (Story.CharacterDef c : story.characters) {
            int score = 0;
            String full = Txt.norm(c.fullName);
            if (full.equals(s)) score = 1000;
            else if (full.contains(s) && s.length() >= 2 && wordIn(c.fullName, speaker.trim())) score = 500 + s.length();   // v38: whole words ("राजकुमार" is not in "राजकुमारी")
            for (String a : c.aliases) {
                String na = Txt.norm(a);
                if (na.length() >= 2 && s.contains(na)) score = Math.max(score, 100 + na.length() * 4);
                if (na.equals(s)) score = Math.max(score, 900);
            }
            if (score > bestScore) { bestScore = score; best = c; }
        }
        return best;
    }

    /** v38: words that name several characters at once ("all three", "both", "everyone") and how many (0 = all of them). */
    static final String[][] GROUP_WORDS = {
            {"दोनों", "2"}, {"तीनों", "3"}, {"चारों", "4"}, {"पाँचों", "5"}, {"पांचों", "5"}, {"छहों", "6"}, {"छः", "6"},
            {"both", "2"}, {"the two", "2"}, {"two of them", "2"}, {"all three", "3"}, {"the three", "3"}, {"three of them", "3"},
            {"all four", "4"}, {"the four", "4"}, {"four of them", "4"}, {"all five", "5"}, {"five of them", "5"},
            {"सब", "0"}, {"सभी", "0"}, {"सारे", "0"}, {"सब लोग", "0"}, {"हम सब", "0"}, {"सब मिलकर", "0"}, {"एक साथ", "0"}, {"मिलकर", "0"},
            {"कोरस", "0"}, {"all", "0"}, {"everyone", "0"}, {"everybody", "0"}, {"all of them", "0"}, {"together", "0"}, {"chorus", "0"}, {"they", "0"}, {"वे सब", "0"}};

    /**
     * v38: how many characters a speaker's name stands for when it is a group word ("तीनों" / "तीनो" → 3, "सब" → 0 = all
     * of them); -1 when it is not one. Spelling marks are ignored (Txt.norm: "तीनो" = "तीनों").
     */
    public static int groupCount(String raw) {
        String w = Txt.norm(Txt.withoutParens(raw)).replaceAll("[\\s,،:।.!\\-–—]+", " ").trim();
        if (w.isEmpty()) return -1;
        String[] ws = w.split(" ");
        for (String[] g : GROUP_WORDS) {
            String n = Txt.norm(g[0]);
            // the whole name ("तीनों", "सब लोग") or the group word first ("तीनों बहनें", "दोनों दोस्त", "all three kids")
            if (w.equals(n) || (w.startsWith(n + " ") && ws.length <= 4)) return Integer.parseInt(g[1]);
        }
        return -1;
    }

    /** v38: a looser form of a name for spelling variants ("वृन्दा" = "वृंदा", "मीणा" ≈ "मीना" is not merged; long and short vowels are). */
    public static String loose(String s) {
        String n = Txt.norm(s).replaceAll("[\\s\\-_.']+", "");
        n = n.replaceAll("[नमणङञ]्(?=[क-ह])", "");      // a half nasal before a consonant is the anusvara
        n = n.replace("ं", "").replace("ी", "ि").replace("ू", "ु").replace("ई", "इ").replace("ऊ", "उ");
        // v39: the sounds written several ways ("वनुशा" = "वानुषा" = "वाणुशा"): ण → न, ष / श → स, the long ा dropped
        n = n.replace("ण", "न").replace("ष", "स").replace("श", "स").replace("ा", "").replace("आ", "अ");
        return n;
    }

    /** v38: role words a speaker may be called by instead of a name. */
    static final String[][] ROLES = {
            {"सिपाही", "गार्ड", "पहरेदार", "सैनिक", "संतरी", "दरबान", "guard", "soldier", "sentry"},
            {"राजा", "महाराज", "king"}, {"रानी", "महारानी", "queen"}, {"राजकुमारी", "princess"}, {"राजकुमार", "prince"},
            {"चुड़ैल", "डायन", "witch"}, {"राक्षस", "दैत्य", "monster", "demon"}, {"बंदर", "monkey"},
            {"माँ", "मां", "मम्मी", "माता", "mother", "mom", "mummy"}, {"पिता", "पापा", "पिताजी", "father", "dad", "papa"},
            {"दादी", "नानी", "grandma", "granny"}, {"दादा", "नाना", "grandpa"}, {"टीचर", "अध्यापक", "शिक्षक", "मास्टर", "teacher"},
            {"डॉक्टर", "वैद्य", "doctor"}};

    /**
     * v38: a speaker called by a role ("सिपाही", "गार्ड", "माँ") when the list names one character of that role ("गार्ड
     * रतनलाल", "रानी प्रिया राठौर (माँ)"): that character (the one seen most recently when several fit); null when none
     * fits.
     */
    static Story.CharacterDef byRole(Story story, String raw, List<Story.CharacterDef> recent) {
        String bare = Txt.withoutParens(raw).trim();
        if (bare.split("\\s+").length > 3) return null;
        for (String[] role : ROLES) {
            if (!wordIn(bare, role)) continue;
            List<Story.CharacterDef> fit = new ArrayList<Story.CharacterDef>();
            for (Story.CharacterDef c : story.characters) {
                if (!c.fromScript || c.voiceOnly) continue;
                String head = c.fullName + " " + c.description.split("\n")[0];
                if (wordIn(head, role)) fit.add(c);
            }
            if (fit.isEmpty()) return null;
            for (Story.CharacterDef c : recent) if (fit.contains(c)) return c;
            return fit.get(0);
        }
        return null;
    }

    /** v38: any of the words in the text as a whole word (Devanagari vowel signs count as part of the word). */
    static boolean wordIn(String text, String... words) {
        String t = Txt.norm(text);
        for (String w : words) {
            String n = Txt.norm(w);
            int i = t.indexOf(n);
            while (i >= 0) {
                int e = i + n.length();
                boolean okL = i == 0 || !isWordChar(t.charAt(i - 1));
                boolean okR = e >= t.length() || !isWordChar(t.charAt(e));
                if (okL && okR) return true;
                i = t.indexOf(n, i + 1);
            }
        }
        return false;
    }

    /** v38: the listed character a speaker's name means, allowing spelling variants; null when none. */
    static Story.CharacterDef looseResolve(Story story, String raw) {
        String r = loose(raw);
        if (r.length() < 2) return null;
        Story.CharacterDef best = null;
        int bl = 0;
        for (Story.CharacterDef c : story.characters) {
            if (!c.fromScript) continue;
            List<String> names = new ArrayList<String>(c.aliases);
            names.add(c.fullName);
            for (String a : names) {
                String la = loose(a);
                if (la.length() < 2) continue;
                if ((r.equals(la) || (la.length() >= 3 && (r.startsWith(la) || (la.startsWith(r) && r.length() >= 3 && la.length() - r.length() <= 1)))) && la.length() > bl) { best = c; bl = la.length(); }
            }
        }
        return best;
    }

    /**
     * Every spoken line gets its speaker. v38 (the user: "तीनो means all three, not a character — take only the
     * characters of the character list"): a group word ("तीनों", "तीनो", "दोनों", "सब", "all three") or names joined by
     * "और" / "," / "and" never becomes a character: the line is said together by those characters (the ones in the
     * scene, the most recent first; Beat.chorus). When the script has a character list, a speaker not in it is matched to
     * a listed character by its spelling variants; failing that it is a voice heard from off the stage — never pictured,
     * never asked for a picture. Only a script with no character list at all gets a new character for a new name.
     */
    private static void resolveSpeakers(Story story) {
        buildAliases(story);
        boolean listed = false;
        for (Story.CharacterDef c : story.characters) if (c.fromScript) { listed = true; break; }
        Map<Story.CharacterDef, Map<String, Integer>> used = new HashMap<Story.CharacterDef, Map<String, Integer>>();
        for (Story.Scene sc : story.scenes) {
            // who is in this scene (named in its directions or speaking under their own name), in order
            List<Story.CharacterDef> inScene = new ArrayList<Story.CharacterDef>();
            for (Story.Beat b : sc.beats) {
                List<Story.CharacterDef> ms = b.type == Story.Beat.DIALOGUE ? new ArrayList<Story.CharacterDef>() : mentions(story, b.text);
                if (b.type == Story.Beat.DIALOGUE && groupCount(b.speakerRaw) < 0) {
                    Story.CharacterDef d = resolve(story, b.speakerRaw);
                    if (d != null) ms.add(d);
                }
                for (Story.CharacterDef m : ms) if (!m.voiceOnly && !inScene.contains(m)) inScene.add(m);
            }
            List<Story.CharacterDef> recent = new ArrayList<Story.CharacterDef>();
            List<List<Story.CharacterDef>> together = new ArrayList<List<Story.CharacterDef>>();   // named together in one sentence, the latest first
            for (Story.Beat b : sc.beats) {
                if (b.type != Story.Beat.DIALOGUE) {
                    for (String sent : b.text.split("[।.!?]")) {
                        List<Story.CharacterDef> named = new ArrayList<Story.CharacterDef>();
                        for (Story.CharacterDef m : mentions(story, sent)) if (!m.voiceOnly) named.add(m);
                        if (named.size() >= 2) together.add(0, named);
                    }
                    List<Story.CharacterDef> ms = mentions(story, b.text);
                    for (int k = ms.size() - 1; k >= 0; k--) { Story.CharacterDef m = ms.get(k); if (m.voiceOnly) continue; recent.remove(m); recent.add(0, m); }
                    continue;
                }
                if (isNarrator(b.speakerRaw)) { b.speaker = null; b.narrator = true; story.hasNarrator = true; continue; }
                List<Story.CharacterDef> group = groupOf(story, b.speakerRaw, inScene, recent, b.text, together);
                if (group != null) {
                    b.speaker = group.get(0);
                    b.chorus.clear();
                    for (int k = 1; k < group.size(); k++) b.chorus.add(group.get(k));
                    for (Story.CharacterDef g : group) { recent.remove(g); recent.add(0, g); }
                    continue;           // a group word never names anyone (not counted for the display name)
                }
                // "मीना और राजू" in a script without a character list: each name is a character, the line is said together
                String[] parts = Txt.withoutParens(b.speakerRaw).split("\\s+और\\s+|\\s*[,،&]\\s*|\\s+and\\s+|\\s+तथा\\s+");
                if (parts.length >= 2 && groupCount(b.speakerRaw) < 0) {
                    List<Story.CharacterDef> each = new ArrayList<Story.CharacterDef>();
                    for (String part : parts) {
                        String q = part.trim();
                        if (q.isEmpty() || q.split("\\s+").length > 3) { each.clear(); break; }
                        Story.CharacterDef d = resolve(story, q);
                        if (d == null && listed) d = looseResolve(story, q);
                        if (d == null && !listed) { d = newChar(story, q, "", -1); d.fromScript = false; d.aliases.add(q); d.voiceOnly = voiceOnlyName(q); }
                        if (d != null && !d.voiceOnly && !each.contains(d)) each.add(d);
                    }
                    if (each.size() >= 2) {
                        b.speaker = each.get(0);
                        b.chorus.clear();
                        for (int k = 1; k < each.size(); k++) b.chorus.add(each.get(k));
                        for (Story.CharacterDef g : each) { recent.remove(g); recent.add(0, g); }
                        continue;
                    }
                }
                if (groupCount(b.speakerRaw) >= 0) {
                    // a group word with nobody to stand for (no characters at all yet): a voice-over, never a character
                    b.speaker = null; b.narrator = true; story.hasNarrator = true;
                    continue;
                }
                Story.CharacterDef c = resolve(story, b.speakerRaw);
                if (c == null && listed) c = looseResolve(story, b.speakerRaw);
                if (c == null && listed) c = byRole(story, b.speakerRaw, recent);
                if (c == null) {
                    c = newChar(story, b.speakerRaw, "", -1);
                    c.fromScript = false;
                    c.aliases.add(b.speakerRaw);
                    if (listed) {
                        // not in the character list: heard, never pictured, never asked for a picture
                        c.voiceOnly = true;
                        story.warnings.add("'" + b.speakerRaw + "' is not in the character list — heard as a voice, not shown and no picture asked. Add them to the list to see them.");
                    } else {
                        c.voiceOnly = voiceOnlyName(b.speakerRaw);
                        story.warnings.add("'" + b.speakerRaw + "' is not described in the character list — the studio designed a look.");
                    }
                }
                b.speaker = c;
                if (!c.voiceOnly) { recent.remove(c); recent.add(0, c); }
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

    /**
     * v38: the characters a group speaker stands for, or null when the name is not a group. "मीना और राजू", "Meena,
     * Raju": the named ones. "तीनों" / "तीनो" / "दोनों" / "all three": that many of the scene's characters, the most
     * recently seen or heard first (then the others of the scene, then the listed cast). "सब" / "everyone": everyone in
     * the scene. The order follows the scene, so the first one named in it leads (their voice is heard).
     */
    static List<Story.CharacterDef> groupOf(Story story, String raw, List<Story.CharacterDef> inScene, List<Story.CharacterDef> recent) {
        return groupOf(story, raw, inScene, recent, "", new ArrayList<List<Story.CharacterDef>>());
    }

    /**
     * With the line's words and the groups named together earlier in the scene: the one the line speaks to ("धन्यवाद
     * मम्मी!") is not among those saying it, and a group of that size named together ("पापा, सिया और परी सोफ़े पर बैठे
     * हैं") is the group.
     */
    static List<Story.CharacterDef> groupOf(Story story, String raw, List<Story.CharacterDef> inScene, List<Story.CharacterDef> recent,
                                            String line, List<List<Story.CharacterDef>> together) {
        String bare = Txt.withoutParens(raw).trim();
        // a listed name that happens to be a group word in another language is the character
        for (Story.CharacterDef c : story.characters) if (aliasEquals(c, bare)) return null;
        List<Story.CharacterDef> named = new ArrayList<Story.CharacterDef>();
        for (Story.CharacterDef c : mentions(story, bare)) if (!c.voiceOnly) named.add(c);
        boolean joined = Txt.has(bare, " और ", ",", "،", " & ", " तथा ", " एवं ") || Txt.hasWord(bare, "and");
        if (named.size() >= 2 && joined) return named;
        int n = groupCount(bare);
        if (n < 0) return null;
        List<Story.CharacterDef> addressed = new ArrayList<Story.CharacterDef>();
        for (Story.CharacterDef c : mentions(story, line == null ? "" : line)) if (!named.contains(c)) addressed.add(c);
        List<Story.CharacterDef> pool = new ArrayList<Story.CharacterDef>();
        if (!named.isEmpty()) pool.addAll(named);        // "तीनों, मीना के साथ"
        if (n > 0 && named.isEmpty()) for (List<Story.CharacterDef> g : together) {
            boolean ok = g.size() == n;
            for (Story.CharacterDef c : g) if (addressed.contains(c)) ok = false;
            if (ok) { pool.addAll(g); break; }
        }
        if (n == 0) { for (Story.CharacterDef c : inScene) if (!pool.contains(c) && !addressed.contains(c)) pool.add(c); }
        else if (inScene.size() == n) { for (Story.CharacterDef c : inScene) if (!pool.contains(c) && !addressed.contains(c)) pool.add(c); }
        for (Story.CharacterDef c : recent) if (!pool.contains(c) && !c.voiceOnly && !addressed.contains(c)) pool.add(c);
        for (Story.CharacterDef c : inScene) if (!pool.contains(c) && !addressed.contains(c)) pool.add(c);
        for (Story.CharacterDef c : story.characters) if (!c.voiceOnly && !pool.contains(c) && !addressed.contains(c)) pool.add(c);
        // only when there is nobody else: the one spoken to as well
        for (Story.CharacterDef c : addressed) if (!c.voiceOnly && !pool.contains(c)) pool.add(c);
        int want = n == 0 ? Math.max(2, inScene.size() - addressed.size()) : n;
        List<Story.CharacterDef> out = new ArrayList<Story.CharacterDef>(pool.subList(0, Math.min(want, pool.size())));
        if (out.isEmpty()) return null;
        // in the scene's order (the first one in the scene leads)
        final List<Story.CharacterDef> ord = inScene;
        java.util.Collections.sort(out, new java.util.Comparator<Story.CharacterDef>() {
            public int compare(Story.CharacterDef a, Story.CharacterDef b) {
                int ia = ord.indexOf(a), ib = ord.indexOf(b);
                return (ia < 0 ? 999 : ia) - (ib < 0 ? 999 : ib);
            }
        });
        return out;
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
