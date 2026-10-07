package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * The production file: after reading the script the studio writes down every character, place, shot, voice and
 * sound it needs, with ready-to-paste prompts so the user can make pictures elsewhere (ChatGPT, Grok, Meta AI…)
 * and upload them. File names that contain a character's or place's name are recognised automatically.
 */
public final class Bible {
    private Bible() {}

    /** Emotion -> words for the shot list and for voice direction. */
    public static String emotionWord(int e, boolean hindi) {
        switch (e) {
            case Pose.HAPPY: return hindi ? "खुश" : "happy";
            case Pose.LAUGH: return hindi ? "हँसते हुए" : "laughing";
            case Pose.ANGRY: return hindi ? "गुस्से में" : "angry";
            case Pose.SAD: return hindi ? "दुखी" : "sad";
            case Pose.SCARED: return hindi ? "डरे हुए" : "scared";
            case Pose.SURPRISED: return hindi ? "हैरान" : "surprised";
            case Pose.EVIL: return hindi ? "कुटिल/शैतानी" : "evil";
            case Pose.DETERMINED: return hindi ? "दृढ़" : "determined";
            case Pose.DIZZY: return hindi ? "चकराए हुए" : "dizzy";
            case Pose.WHISPER: return hindi ? "फुसफुसाते हुए" : "whispering";
            case Pose.PROUD: return hindi ? "गर्व से" : "proud";
            case Pose.CURIOUS: return hindi ? "जिज्ञासा से" : "curious";
            case Pose.PAIN: return hindi ? "दर्द में" : "in pain";
            default: return hindi ? "सामान्य" : "calm";
        }
    }

    static String kindWord(Look l, boolean hi) {
        if (l == null) return "";
        switch (l.kind) {
            case Look.GIRL: return hi ? "लड़की" : "girl";
            case Look.WOMAN: return hi ? "महिला" : "woman";
            case Look.BOY: return hi ? "लड़का" : "boy";
            case Look.OLD_MAN: return hi ? "बुज़ुर्ग" : "old man";
            case Look.WITCH: return hi ? "जादूगरनी" : "witch";
            case Look.MONSTER: return hi ? "राक्षस" : "monster";
            case Look.MONKEY: return hi ? "बंदर" : "monkey";
            case Look.ANIMAL: return hi ? "जानवर" : "animal";
            case Look.BIRD: return hi ? "पक्षी" : "bird";
            default: return hi ? "पुरुष" : "man";
        }
    }

    /** Suggested voice for a character (used when no sample is given). */
    public static String voiceHint(Story.CharacterDef c, boolean hi) {
        Look l = c.look;
        if (l == null) return hi ? "सामान्य आवाज़" : "normal voice";
        String age = c.age > 0 ? (hi ? c.age + " वर्ष" : c.age + " years") : "";
        switch (l.kind) {
            case Look.GIRL: return (hi ? "बच्ची की ऊँची, मीठी आवाज़ " : "young girl's high, sweet voice ") + age;
            case Look.BOY: return (hi ? "बच्चे की चंचल आवाज़ " : "young boy's lively voice ") + age;
            case Look.WOMAN: return hi ? "महिला की कोमल आवाज़" : "woman's soft voice";
            case Look.OLD_MAN: return hi ? "बुज़ुर्ग की भारी, धीमी आवाज़" : "old man's deep, slow voice";
            case Look.WITCH: return hi ? "जादूगरनी की खरखरी, रहस्यमयी आवाज़" : "witch's raspy, mysterious voice";
            case Look.MONSTER: return hi ? "राक्षस की बहुत भारी, गूँजती आवाज़" : "monster's very deep, booming voice";
            case Look.MONKEY: case Look.ANIMAL: case Look.BIRD: return hi ? "जानवर जैसी मज़ेदार आवाज़" : "funny animal voice";
            default: return hi ? "पुरुष की गंभीर आवाज़" : "man's firm voice";
        }
    }

    public static String characterPrompt(Story.CharacterDef c) {
        StringBuilder b = new StringBuilder();
        b.append("Create ONE character for a premium 3D animated Indian film for children (6-15 years), in the style of a modern 3D animation studio feature (soft global illumination, subsurface skin, detailed fabric). ");
        b.append("Name: ").append(c.displayName);
        if (c.age > 0) b.append(", age ").append(c.age);
        if (c.look != null) b.append(", ").append(kindWord(c.look, false));
        b.append(". Description: ").append(oneLine(c.description));
        b.append(" Pose: full body from head to feet, standing straight and facing the camera, mouth closed, eyes open, ");
        b.append("fully and modestly dressed, plain pure-white background, soft studio light, no text, no other people.");
        return b.toString();
    }

    public static String placePrompt(String name, String description, String aspect) {
        return "Cinematic 3D animated film background for a premium Indian children's film, " + (aspect == null ? "16:9" : aspect)
                + " wide establishing shot, no people, no text. Place: " + name + ". " + oneLine(description)
                + " Style: rich 3D render, volumetric light, depth of field, atmospheric perspective, vivid but natural colours.";
    }

    public static String oneLine(String s) {
        if (s == null) return "";
        return s.replace('\n', ' ').replace("  *", " ").replaceAll("\\s+", " ").trim();
    }

    /** Camera choice for a dialogue/direction, mirroring what the renderer does. */
    static String camera(int beatInScene, boolean speakerChanged, int emotion, boolean action, boolean hi) {
        if (beatInScene == 0) return hi ? "वाइड शॉट (पूरी जगह)" : "Wide establishing shot";
        if (action) return hi ? "वाइड/मीडियम शॉट, कैमरा एक्शन के साथ" : "Wide/medium shot following the action";
        if (emotion == Pose.ANGRY || emotion == Pose.SCARED || emotion == Pose.SURPRISED || emotion == Pose.SAD || emotion == Pose.EVIL)
            return hi ? "क्लोज़-अप (चेहरे के भाव)" : "Close-up on the face";
        if (speakerChanged) return hi ? "मीडियम क्लोज़-अप, बोलने वाले पर फ़ोकस" : "Medium close-up on the speaker";
        return hi ? "मीडियम शॉट" : "Medium shot";
    }

    /** Builds the whole production file. lib may be null. have = names that already have a picture/voice. */
    public static String write(Story st, SoundLib lib, Set<String> havePicture, Set<String> haveVoice) {
        boolean hi = false;   // the production file is written in English; story content stays in its own language
        StringBuilder b = new StringBuilder();
        String title = st.title.length() > 0 ? st.title : "My story";
        b.append(hi ? "प्रोडक्शन फ़ाइल — " : "Production file — ").append(title).append("\n");
        b.append("============================================================\n\n");
        b.append(hi ? "कैसे इस्तेमाल करें:\n"
                + "1. नीचे हर पात्र और जगह का विवरण और \"Prompt\" है। Prompt को ChatGPT / Grok / Meta AI में चिपकाकर चित्र बनवाएँ।\n"
                + "2. चित्र को पात्र/जगह के नाम से सेव करें (जैसे  वृंदा.jpg ) और ऐप में \"चित्र जोड़ें\" से अपलोड करें — ऐप नाम से पहचान लेगा।\n"
                + "3. आवाज़ के लिए हर पात्र की 10–20 सेकंड की रिकॉर्डिंग दें; उसी आवाज़ में पूरे संवाद बनेंगे।\n"
                + "4. जो चीज़ आप नहीं देंगे, स्टूडियो अपनी लाइब्रेरी से खुद चुन लेगा।\n\n"
                : "How to use:\n"
                + "1. Each character and place below has a description and a \"Prompt\". Paste the prompt into ChatGPT / Grok / Meta AI to make the picture.\n"
                + "2. Save the picture with the character/place name (e.g. Vrinda.jpg) and use \"Add many pictures at once\" in the studio — the app recognises the name.\n"
                + "3. For voices give a 10–20 second recording per character; all their dialogue will be made in that voice.\n"
                + "4. Anything you do not give, the studio picks from its own library.\n\n");

        // ---- characters
        b.append(hi ? "पात्र (" : "CHARACTERS (").append(st.characters.size()).append(")\n------------------------------------------------------------\n");
        int i = 1;
        for (Story.CharacterDef c : st.characters) {
            int lines = 0;
            for (Story.Scene sc : st.scenes) for (Story.Beat bt : sc.beats) if (bt.speaker == c) lines++;
            b.append(i++).append(". ").append(st.shown(c.fullName != null && c.fullName.length() > 0 ? c.fullName : c.displayName));
            if (c.age > 0) b.append(" (").append(c.age).append(hi ? " वर्ष)" : " yrs)");
            b.append("\n");
            if (c.description.length() > 0) b.append("   ").append(hi ? "विवरण: " : "Description: ").append(oneLine(c.description)).append("\n");
            b.append("   ").append(hi ? "संवाद: " : "Lines: ").append(lines).append("\n");
            b.append("   ").append(hi ? "आवाज़: " : "Voice: ").append(voiceHint(c, hi));
            if (haveVoice != null && contains(haveVoice, c)) b.append(hi ? "  ✔ नमूना मिला" : "  ✔ sample given");
            b.append("\n");
            b.append("   ").append(hi ? "चित्र: " : "Picture: ").append(havePicture != null && contains(havePicture, c)
                    ? (hi ? "✔ मिल गया" : "✔ given") : (hi ? "✘ चाहिए (या स्टूडियो बनाएगा)" : "✘ needed (or studio makes it)")).append("\n");
            b.append("   Prompt: ").append(characterPrompt(c)).append("\n\n");
        }

        // ---- places
        List<String[]> places = places(st);
        b.append(hi ? "जगहें (" : "PLACES (").append(places.size()).append(")\n------------------------------------------------------------\n");
        i = 1;
        for (String[] p : places) {
            b.append(i++).append(". ").append(p[0]).append("\n");
            if (p[1].length() > 0) b.append("   ").append(hi ? "विवरण: " : "Description: ").append(oneLine(p[1])).append("\n");
            if (havePicture != null && havePicture.contains(Txt.norm(p[0]))) b.append(hi ? "   ✔ चित्र मिल गया\n" : "   ✔ picture given\n");
            b.append("   Prompt: ").append(placePrompt(p[0], p[1], "16:9")).append("\n\n");
        }

        // ---- shots
        b.append(hi ? "शॉट सूची (निर्देशक की योजना)\n" : "SHOT LIST (director's plan)\n");
        b.append("------------------------------------------------------------\n");
        int shot = 1;
        for (Story.Scene sc : st.scenes) {
            b.append("\n").append(hi ? "भाग " : "Part ").append(sc.number);
            if (sc.title.length() > 0) b.append(": ").append(sc.title);
            b.append("\n");
            if (sc.setting.length() > 0) b.append("   ").append(hi ? "जगह: " : "Place: ").append(oneLine(sc.setting)).append("\n");
            if (lib != null) {
                SoundLib.Entry amb = lib.best(sc.setting + " " + sc.title, "amb", null);
                if (amb != null) b.append("   ").append(hi ? "पृष्ठभूमि ध्वनि: " : "Background sound: ").append(amb.title).append("\n");
            }
            Story.CharacterDef last = null;
            int k = 0;
            for (Story.Beat bt : sc.beats) {
                if (bt.type == Story.Beat.DIALOGUE) {
                    int emo = Director.emotionOf(bt.manner, bt.text, bt.speaker);
                    boolean changed = bt.speaker != last;
                    String who = bt.narrator || bt.speaker == null ? "Narrator (voice only)" : bt.speaker.shown();
                    b.append("   ").append(hi ? "शॉट " : "Shot ").append(shot++).append(" — ")
                            .append(bt.narrator ? (hi ? "दृश्य पर कथावाचक की आवाज़" : "Voice-over on the scene") : camera(k, changed, emo, false, hi))
                            .append("\n      ").append(who).append(" [").append(emotionWord(emo, hi)).append("]");
                    if (bt.manner.length() > 0) b.append(" (").append(st.shown(bt.manner)).append(")");
                    b.append(": \"").append(st.shown(oneLine(bt.text))).append("\"\n");
                    last = bt.speaker;
                } else {
                    String t = oneLine(bt.text);
                    if (t.length() == 0) continue;
                    b.append("   ").append(hi ? "शॉट " : "Shot ").append(shot++).append(" — ").append(camera(k, true, Pose.NEUTRAL, true, hi))
                            .append("\n      ").append(hi ? "एक्शन: " : "Action: ").append(t).append("\n");
                    String sfx = sfxFor(t, lib);
                    if (sfx != null) b.append("      ").append(hi ? "ध्वनि: " : "Sound: ").append(sfx).append("\n");
                }
                k++;
            }
        }

        // ---- title / end
        b.append("\n").append(hi ? "शुरुआत और अंत\n" : "TITLE AND END\n").append("------------------------------------------------------------\n");
        b.append(hi ? "शीर्षक पृष्ठ: \"" : "Title page: \"").append(title).append("\" — ")
                .append(hi ? "कहानी का मुख्य चित्र + संगीत। Prompt: " : "main picture of the story + music. Prompt: ")
                .append("3D animated movie poster for a children's film named '").append(title)
                .append("', main characters together, cinematic lighting, depth of field, no text.\n");
        b.append("End page: \"").append(st.hindi ? "समाप्त" : "The End").append("\" — with gentle music.\n");
        return b.toString();
    }

    static boolean contains(Set<String> have, Story.CharacterDef c) {
        if (have.contains(Txt.norm(c.displayName)) || have.contains(c.id)) return true;
        for (String a : c.aliases) if (have.contains(Txt.norm(a))) return true;
        return false;
    }

    /** Distinct places: the script's place list, then each scene's setting. */
    public static List<String[]> places(Story st) {
        List<String[]> out = new ArrayList<String[]>();
        Set<String> seen = new LinkedHashSet<String>();
        for (Story.PlaceDef p : st.places) {
            if (p.name.length() == 0 || !seen.add(Txt.norm(p.name))) continue;
            out.add(new String[]{p.name, p.description});
        }
        for (Story.Scene sc : st.scenes) {
            if (sc.setting.length() == 0) continue;
            String first = firstClause(sc.setting, "।.!?");
            String name = firstClause(first, ",;—-(");
            if (name.length() > 50) name = name.substring(0, 50) + "…";
            boolean known = false;
            for (String[] p : out) if (similar(name, p[0])) { known = true; break; }
            if (known || !seen.add(Txt.norm(name))) continue;
            out.add(new String[]{name, first});
        }
        return out;
    }

    public static String firstClauseOf(String s) { return firstClause(firstClause(s, "।.!?"), ",;—-("); }

    static String firstClause(String s, String stops) {
        int cut = s.length();
        for (int i = 0; i < stops.length(); i++) {
            int k = s.indexOf(stops.charAt(i));
            if (k > 0 && k < cut) cut = k;
        }
        return s.substring(0, cut).trim();
    }

    /** Most words of a also appear in b (so "रत्नगढ़ का विशाल महल और बगीचा" ~ "रत्नगढ़ का महल और बगीचा"). */
    public static boolean similar(String a, String b) {
        String[] wa = Txt.norm(a).split("\\s+");
        String nb = " " + Txt.norm(b) + " ";
        int n = 0, hit = 0;
        for (String w : wa) {
            if (w.length() < 2 || w.equals("का") || w.equals("की") || w.equals("के") || w.equals("और")) continue;
            n++;
            if (nb.contains(" " + w + " ")) hit++;
        }
        return n > 0 && hit * 10 >= n * 7;
    }

    static String sfxFor(String text, SoundLib lib) {
        if (lib == null) return null;
        SoundLib.Entry e = lib.best(text, "sfx", null);
        return e == null ? null : e.title;
    }
}
