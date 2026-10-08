package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Turns a free-text Hindi/English character description into a {@link Look}.
 * Every character is always drawn fully clothed and age-appropriate.
 */
public final class LookDesigner {
    private LookDesigner() {}

    private static final Object[][] COLORS = {
            {"फिरोज़ी", 0xFF19AFA6}, {"फ़िरोज़ी", 0xFF19AFA6}, {"turquoise", 0xFF19AFA6}, {"teal", 0xFF19AFA6},
            {"आसमानी", 0xFF6EC3F0}, {"sky blue", 0xFF6EC3F0},
            {"royal blue", 0xFF1E3C9A},
            {"सुनहरे", 0xFFE0A526}, {"सुनहरी", 0xFFE0A526}, {"सुनहरा", 0xFFE0A526}, {"golden", 0xFFE0A526}, {"gold", 0xFFE0A526},
            {"गुलाबी", 0xFFEC5FA0}, {"pink", 0xFFEC5FA0},
            {"तोते जैसे हरे", 0xFF56C23A}, {"तोतई", 0xFF56C23A}, {"parrot green", 0xFF56C23A},
            {"हरे", 0xFF2E9B48}, {"हरा", 0xFF2E9B48}, {"हरी", 0xFF2E9B48}, {"green", 0xFF2E9B48},
            {"नीले", 0xFF2F5DB5}, {"नीला", 0xFF2F5DB5}, {"नीली", 0xFF2F5DB5}, {"blue", 0xFF2F5DB5},
            {"सफ़ेद", 0xFFF4F1EA}, {"सफेद", 0xFFF4F1EA}, {"white", 0xFFF4F1EA},
            {"केसरिया", 0xFFF08A24}, {"नारंगी", 0xFFF08A24}, {"saffron", 0xFFF08A24}, {"orange", 0xFFF08A24},
            {"मैरून", 0xFF7A1C2E}, {"maroon", 0xFF7A1C2E},
            {"लाल", 0xFFC62828}, {"red", 0xFFC62828},
            {"काले", 0xFF2A2730}, {"काला", 0xFF2A2730}, {"काली", 0xFF2A2730}, {"black", 0xFF2A2730},
            {"पीले", 0xFFF2C230}, {"पीला", 0xFFF2C230}, {"पीली", 0xFFF2C230}, {"yellow", 0xFFF2C230},
            {"बैंगनी", 0xFF7B3FA0}, {"जामुनी", 0xFF7B3FA0}, {"purple", 0xFF7B3FA0}, {"violet", 0xFF7B3FA0},
            {"भूरे", 0xFF7A4E2D}, {"भूरा", 0xFF7A4E2D}, {"brown", 0xFF7A4E2D},
            {"मटमैले", 0xFF6E6A5E}, {"स्लेटी", 0xFF77797E}, {"grey", 0xFF77797E}, {"gray", 0xFF77797E},
            {"क्रीम", 0xFFF0E2C0}, {"cream", 0xFFF0E2C0},
            {"चमड़े", 0xFF6B4226}, {"leather", 0xFF6B4226},
    };

    static int darker(int c, float f) {
        int r = (int) (((c >> 16) & 255) * f), g = (int) (((c >> 8) & 255) * f), b = (int) ((c & 255) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    /** Colours in order of appearance; "गहरे/dark" darkens the following colour. */
    static List<Integer> colorsIn(String text) {
        List<Integer> out = new ArrayList<Integer>();
        String t = Txt.norm(Txt.withoutParens(text));
        int i = 0;
        while (i < t.length()) {
            int bestLen = 0, bestCol = 0;
            for (Object[] e : COLORS) {
                String w = Txt.norm((String) e[0]);
                if (w.length() > bestLen && t.startsWith(w, i)) {
                    boolean okL = i == 0 || !Character.isLetter(t.charAt(i - 1)) || !Txt.isDevanagari(t.charAt(i - 1)) && !Character.isLetter(t.charAt(i - 1));
                    if (i > 0 && Txt.isDevanagari(t.charAt(i - 1)) && t.charAt(i - 1) != ' ') okL = false;
                    if (okL) { bestLen = w.length(); bestCol = (Integer) e[1]; }
                }
            }
            if (bestLen > 0) {
                String before = t.substring(Math.max(0, i - 8), i);
                if (before.contains(Txt.norm("गहर")) || before.contains("dark")) bestCol = darker(bestCol, 0.72f);
                if (!out.contains(bestCol)) out.add(bestCol);
                i += bestLen;
            } else i++;
        }
        return out;
    }

    /** The sentence (split on । . ,) containing a keyword. */
    static String sentenceWith(String text, String... keys) {
        String[] sents = text.split("[।\\n]|\\. ");
        for (String s : sents) if (Txt.has(s, keys)) return s;
        return "";
    }

    static String section(String desc, String... keys) {
        String[] lines = desc.split("\n");
        StringBuilder b = new StringBuilder();
        boolean on = false;
        for (String l : lines) {
            String h = l.length() > 40 ? l.substring(0, 40) : l;
            boolean isHeadered = h.contains(":");
            if (isHeadered) on = Txt.has(h, keys);
            if (on) b.append(l).append('\n');
        }
        return b.toString();
    }

    public static void designAll(Story story) {
        // First pass: independent looks. Second pass: "X जैसे ही" copies.
        for (Story.CharacterDef c : story.characters) c.look = design(c, null);
        int guardIdx = 0;
        for (Story.CharacterDef c : story.characters) {
            String d = c.description;
            for (Story.CharacterDef o : story.characters) {
                if (o == c) continue;
                for (String a : o.aliases) {
                    if (a.length() < 2) continue;
                    if (Txt.has(d, a + " जैसे", a + " जैसी", a + " जैसा", "like " + a, "same as " + a)) {
                        c.look = design(c, o.look);
                        break;
                    }
                }
            }
            if (c.look.outfit == Look.O_UNIFORM) c.look.variant = guardIdx++;
        }
    }

    static int speciesOf(String t) {
        Object[][] sp = {
                {Look.SP_FOX, "लोमड़ी", "fox"}, {Look.SP_LION, "शेर", "सिंह", "lion"}, {Look.SP_TIGER, "बाघ", "tiger"},
                {Look.SP_BEAR, "भालू", "रीछ", "bear"}, {Look.SP_ELEPHANT, "हाथी", "elephant"}, {Look.SP_RABBIT, "खरगोश", "rabbit", "hare"},
                {Look.SP_CAT, "बिल्ली", "cat"}, {Look.SP_DOG, "कुत्ता", "कुत्ते", "dog"}, {Look.SP_DEER, "हिरण", "deer"},
                {Look.SP_GOAT, "बकरी", "बकरा", "goat"}, {Look.SP_COW, "गाय", "बैल", "cow", "ox"}, {Look.SP_WOLF, "भेड़िया", "wolf"},
                {Look.SP_MOUSE, "चूहा", "चुहिया", "mouse", "rat"}, {Look.SP_TORTOISE, "कछुआ", "tortoise", "turtle"}, {Look.SP_HORSE, "घोड़ा", "घोड़ी", "horse"},
                {Look.SP_CROW, "कौआ", "कौवा", "crow"}, {Look.SP_SPARROW, "गौरैया", "चिड़िया", "sparrow"}, {Look.SP_PARROT, "तोता", "मैना", "parrot"},
                {Look.SP_PEACOCK, "मोर", "peacock"}, {Look.SP_OWL, "उल्लू", "owl"}, {Look.SP_HEN, "मुर्गी", "मुर्गा", "hen", "rooster"},
                {Look.SP_EAGLE, "चील", "बाज़", "गिद्ध", "eagle", "hawk"}, {Look.SP_DUCK, "बत्तख", "हंस", "duck", "swan"},
        };
        // the name and the first sentence of the description decide (e.g. "चालाक लोमड़ी", "शेरू: जंगल का राजा, एक बड़ा
        // सुनहरा शेर।"); later details ("a dog sits at her feet") and likenesses ("शेर जैसा बहादुर") do not
        int cut = t.length();
        int nl = t.indexOf('\n');
        int from = nl >= 0 && nl < 60 ? nl + 1 : 0;
        for (char ch : new char[]{'।', '.', '!', '?', '\n'}) { int k = t.indexOf(ch, from); if (k > 0 && k < cut) cut = k; }
        String head = t.substring(0, Math.min(cut, 160));
        int best = -1, at = Integer.MAX_VALUE;
        String hn = Txt.norm(head);
        for (Object[] e : sp) for (int i = 1; i < e.length; i++) {
            String w = (String) e[i];
            if (!wordIn(head, w) || likeness(head, w) || owned(head, w)) continue;
            int k = hn.indexOf(Txt.norm(w));
            if (k >= 0 && k < at) { at = k; best = (Integer) e[0]; }
        }
        // "a girl who loves cats": a person named first is a person ("king of the jungle" is still a lion)
        if (best >= 0) for (String hw : HUMAN) {
            int k = hn.indexOf(Txt.norm(hw));
            if (k >= 0 && k < at && wordIn(head, hw)) return -1;
        }
        return best;
    }

    static final String[] HUMAN = {"girl", "boy", "woman", "man", "lady", "princess", "prince", "queen", "child", "student", "farmer",
            "लड़की", "लड़का", "औरत", "आदमी", "महिला", "राजकुमारी", "राजकुमार", "रानी", "बच्चा", "बच्ची", "किसान", "छात्र", "छात्रा"};

    /** "her cat", "उसकी बिल्ली", "his pet dog": an animal the character has, not what the character is. */
    static boolean owned(String t, String w) {
        String n = Txt.norm(t), x = Txt.norm(w);
        int i = n.indexOf(x);
        while (i >= 0) {
            String before = n.substring(0, i).trim();
            boolean own = before.endsWith("her") || before.endsWith("his") || before.endsWith("their") || before.endsWith("pet") || before.endsWith("my")
                    || before.endsWith("उसकी") || before.endsWith("उसका") || before.endsWith("उसके") || before.endsWith("अपनी") || before.endsWith("अपना")
                    || before.endsWith("अपने") || before.endsWith("पालतू") || before.endsWith("with a") || before.endsWith("और उसका") || before.endsWith("और उसकी");
            if (!own) return false;
            i = n.indexOf(x, i + 1);
        }
        return true;
    }

    /** "शेर जैसा", "शेर-सा", "like a lion", "lion-hearted": a comparison, not what the character is. */
    static boolean likeness(String t, String w) {
        String n = Txt.norm(t), x = Txt.norm(w);
        int i = n.indexOf(x);
        while (i >= 0) {
            String after = n.substring(i + x.length()).replaceFirst("^[\\s\\-]+", "");
            String before = n.substring(0, i).trim();
            boolean like = after.startsWith("जैस") || after.startsWith("सा ") || after.startsWith("सी ") || after.startsWith("से ") || after.startsWith("की तरह")
                    || after.startsWith("hearted") || after.startsWith("heart") || before.endsWith("like a") || before.endsWith("like an") || before.endsWith("like")
                    || before.endsWith("as a") || after.startsWith("दिल");
            if (!like) return false;
            i = n.indexOf(x, i + 1);
        }
        return true;
    }

    private static boolean wordIn(String t, String w) {
        String n = Txt.norm(t), x = Txt.norm(w);
        int i = n.indexOf(x);
        while (i >= 0) {
            boolean l = i == 0 || !Character.isLetter(n.charAt(i - 1)) && !Txt.isDevanagari(n.charAt(i - 1)) || n.charAt(i - 1) == ' ';
            int e = i + x.length();
            boolean r = e >= n.length() || n.charAt(e) == ' ' || !Character.isLetterOrDigit(n.charAt(e)) && !(n.charAt(e) >= '\u0900' && n.charAt(e) <= '\u0963');
            if (l && r) return true;
            i = n.indexOf(x, i + 1);
        }
        return false;
    }

    /** Colours and size of animals and birds. */
    static void animal(Look l, String all) {
        int fur, belly = 0xFFF5EBDD;
        float h;
        switch (l.species) {
            case Look.SP_FOX: fur = 0xFFE07B28; h = 0.42f; break;
            case Look.SP_LION: fur = 0xFFD9A441; h = 0.62f; break;
            case Look.SP_TIGER: fur = 0xFFEE8A22; h = 0.6f; break;
            case Look.SP_BEAR: fur = 0xFF6D4C35; h = 0.75f; belly = 0xFF9C7A5E; break;
            case Look.SP_ELEPHANT: fur = 0xFF8E8E96; h = 1.05f; belly = 0xFFA8A8B0; break;
            case Look.SP_RABBIT: fur = 0xFFF4F1EA; h = 0.3f; break;
            case Look.SP_CAT: fur = 0xFF8D8D8D; h = 0.3f; break;
            case Look.SP_DOG: fur = 0xFFB07A48; h = 0.4f; break;
            case Look.SP_DEER: fur = 0xFFC08A55; h = 0.6f; break;
            case Look.SP_GOAT: fur = 0xFFEDE7DC; h = 0.45f; break;
            case Look.SP_COW: fur = 0xFFF2EEE6; h = 0.7f; break;
            case Look.SP_WOLF: fur = 0xFF7D7F86; h = 0.48f; break;
            case Look.SP_MOUSE: fur = 0xFF9E9E9E; h = 0.16f; break;
            case Look.SP_TORTOISE: fur = 0xFF6E8B3D; h = 0.22f; belly = 0xFF8D6E4A; break;
            case Look.SP_HORSE: fur = 0xFF8B5A2B; h = 0.85f; break;
            case Look.SP_CROW: fur = 0xFF26262B; h = 0.25f; belly = 0xFF3A3A42; break;
            case Look.SP_SPARROW: fur = 0xFF9C6B3E; h = 0.18f; break;
            case Look.SP_PARROT: fur = 0xFF3CB043; h = 0.24f; belly = 0xFF7CD657; break;
            case Look.SP_PEACOCK: fur = 0xFF1F5FBF; h = 0.5f; belly = 0xFF2B8A8A; break;
            case Look.SP_OWL: fur = 0xFF8D6E4A; h = 0.28f; belly = 0xFFD8C3A0; break;
            case Look.SP_HEN: fur = 0xFFF5F0E6; h = 0.28f; break;
            case Look.SP_EAGLE: fur = 0xFF6B4423; h = 0.36f; belly = 0xFFF5F0E6; break;
            case Look.SP_DUCK: fur = 0xFFF8F8F0; h = 0.26f; break;
            default: fur = 0xFF9C7A5E; h = 0.45f;
        }
        List<Integer> cols = colorsIn(all);
        if (!cols.isEmpty() && l.species != Look.SP_CROW) fur = cols.get(0);
        l.furColor = fur;
        l.skin = belly;
        l.primary = cols.size() > 1 ? cols.get(1) : 0;
        l.height = h;
        l.hair = Look.H_NONE;
        l.outfit = -1;
        l.female = Txt.has(all, "मादा", "चिड़िया", "बिल्ली", "गाय", "बकरी", "मुर्गी", "she ", " her ");
    }

    public static Look design(Story.CharacterDef c, Look base) {
        String all = c.fullName + "\n" + c.description;
        String face = section(c.description, "चेहरा", "face", "शरीर", "body", "रूप");
        String clothes = section(c.description, "पहनावा", "पोशाक", "कपड़े", "outfit", "dress", "clothes", "costume");
        if (face.length() == 0) face = all;
        if (clothes.length() == 0) clothes = all;
        Look l = base != null ? base.copy() : new Look();

        // ---- kind
        boolean monster = Txt.has(all, "राक्षस", "दानव", "दैत्य", "असुर", "monster", "demon", "ogre");
        boolean witch = Txt.has(all, "चुड़ैल", "डायन", "witch", "जादूगरनी");
        boolean monkey = Txt.has(all, "बंदर", "वानर", "मकाक", "monkey", "macaque");
        int species = monkey ? -1 : speciesOf(c.fullName + " " + c.description);
        String noPrincess = Txt.norm(all).replace(Txt.norm("राजकुमारी"), "");
        int fem = 0, mal = 0;
        String[] femW = {"राजकुमारी", "रानी", "लड़की", "बेटी", "सहेली", "माँ", "दादी", "नानी", "बहन", "दीदी", "बिंदी", "साड़ी",
                "लहंगा", "घाघरा", "चोली", "सिंदूर", "चूड़ि", "princess", "queen", "girl", "woman", "mother", "lady", "aunt", "आंटी", "she ", " her "};
        String[] malW = {"राजा", "राजकुमार", "लड़का", "बेटा", "पिता", "दादा", "भाई", "भैया", "गार्ड", "सिपाही", "सैनिक", "मूँछ", "मूंछ",
                "दाढ़ी", "पगड़ी", "अचकन", "king", "prince", "boy", "man ", "father", "guard", "soldier", " he ", " his "};
        for (String w : femW) if (Txt.has(all, w)) fem++;
        for (String w : malW) if (noPrincess.contains(Txt.norm(w))) mal++;
        int age = c.age;
        boolean old = Txt.has(all, "बूढ़", "बुज़ुर्ग", "old ", "elderly") || age >= 60;

        if (base == null) {
            if (monster) { l.kind = Look.MONSTER; l.female = false; }
            else if (witch) { l.kind = Look.WITCH; l.female = true; }
            else if (monkey) { l.kind = Look.MONKEY; l.female = false; }
            else if (species >= 20) { l.kind = Look.BIRD; l.species = species; }
            else if (species >= 0) { l.kind = Look.ANIMAL; l.species = species; }
            else {
                l.female = fem > mal;
                boolean child = (age > 0 && age < 14) || (age < 0 && (Txt.has(all, "बच्ची", "बच्चा", "बालक", "बालिका", "child", "kid")
                        || wordIn(all, "girl") || wordIn(all, "boy") || wordIn(all, "लड़की") || wordIn(all, "लड़का")));
                if (child) l.kind = l.female ? Look.GIRL : Look.BOY;
                else if (old && !l.female) l.kind = Look.OLD_MAN;
                else l.kind = l.female ? Look.WOMAN : Look.MAN;
            }
            l.hero = !(monster || witch || Txt.has(all, "दुष्ट", "खलनायक", "villain", "evil", "चालाक", "धूर्त", "cunning"));
            if (species == Look.SP_FOX || species == Look.SP_WOLF) l.hero = !Txt.has(all, "चालाक", "धूर्त", "दुष्ट", "cunning", "wicked", "sly") && l.hero;
        }

        // ---- body language (§26): every character moves in its own way
        if (Txt.has(all, "चंचल", "शरारती", "फुर्तीला", "फुर्तीली", "फुर्तीले", "नटखट", "उत्साही", "चुलबुल", "playful", "naughty", "energetic", "excited",
                "lively", "mischievous", "bubbly", "cheerful", "restless", "hyper")) l.energy = 1.35f;
        else if (Txt.has(all, "शांत", "गंभीर", "शर्मीला", "शर्मीली", "सौम्य", "धीर", "बूढ़", "बुज़ुर्ग", "थका", "calm", "shy", "serious", "gentle", "quiet",
                "wise", "elderly", "tired", "timid")) l.energy = 0.7f;
        if (Txt.has(all, "रोबदार", "शाही", "घमंडी", "गर्व", "आत्मविश्वासी", "proud", "royal", "majestic", "confident", "arrogant", "राजा", "रानी", "king", "queen")) l.poise = 1f;
        else if (Txt.has(all, "शर्मीला", "शर्मीली", "डरपोक", "सहमा", "shy", "timid", "nervous", "frightened")) l.poise = -1f;
        if (l.kind == Look.ANIMAL || l.kind == Look.BIRD) { animal(l, all); return l; }
        // ---- size
        switch (l.kind) {
            case Look.GIRL: case Look.BOY:
                int a = age > 0 ? age : 9;
                l.height = 0.5f + Math.min(13, Math.max(4, a)) * 0.022f; break;
            case Look.WOMAN: l.height = 0.93f; break;
            case Look.MONSTER: l.height = Txt.has(all, "8 फीट", "8 feet", "विशाल", "भीमकाय", "giant") ? 1.42f : 1.3f; break;
            case Look.WITCH: l.height = 0.92f; break;
            case Look.MONKEY: l.height = 0.36f; break;
            default: l.height = 1f;
        }
        if (Txt.has(face, "गोल-मटोल", "मोटा", "भारी", "chubby", "fat") && !l.isChild()) l.girth = 1.22f;
        else if (Txt.has(face, "दुबली", "दुबला", "पतली", "thin", "skinny")) l.girth = 0.82f;
        else if (l.kind == Look.MONSTER) l.girth = 1.35f;
        else if (base == null) l.girth = 1f;

        // ---- skin
        if (l.kind == Look.MONSTER) l.skin = 0xFF5B4636;
        else if (l.kind == Look.WITCH) l.skin = 0xFFA9A866;
        else if (l.kind == Look.MONKEY) l.skin = 0xFFE3A98C;
        else if (Txt.has(face, "गेहुँआ", "गेहुंआ", "wheatish")) l.skin = 0xFFCB905C;
        else if (Txt.has(face, "गोरा", "गोरी", "fair")) l.skin = 0xFFF2C9A3;
        else if (Txt.has(face, "साँवला", "साँवली", "सांवला", "dusky", "dark skin")) l.skin = 0xFF9C6A43;
        else if (base == null) l.skin = 0xFFDBA474;

        if (l.kind == Look.MONSTER || l.kind == Look.MONKEY) {
            List<Integer> fc = colorsIn(sentenceWith(face, "बाल", "fur", "hair"));
            l.furColor = l.kind == Look.MONKEY ? 0xFFA4552E : 0xFF3B2B22;
            if (Txt.has(face, "लाल-भूरे", "reddish")) l.furColor = 0xFFA4552E;
            else if (!fc.isEmpty() && l.kind == Look.MONSTER) l.furColor = darker(fc.get(0) == 0xFF2A2730 ? 0xFF4A362A : fc.get(0), 0.9f);
        }

        // ---- hair
        String hairS = sentenceWith(face, "बाल", "चोटी", "चोटि", "hair", "braid", "जूड़ा");
        l.curly = Txt.has(hairS, "घुंघराले", "घुँघराले", "curly");
        if (Txt.has(hairS, "दो चोटि", "दो साधारण चोटि", "two braids", "pigtail")) l.hair = Look.H_PIGTAILS;
        else if (Txt.has(hairS, "चोटी", "braid")) l.hair = Look.H_BRAID;
        else if (Txt.has(hairS, "जूड़ा", "bun")) l.hair = Look.H_BUN;
        else if (base == null) {
            if (l.kind == Look.GIRL) l.hair = Look.H_PIGTAILS;
            else if (l.kind == Look.WOMAN) l.hair = Look.H_BUN;
            else if (l.kind == Look.WITCH) l.hair = Look.H_LONG;
            else if (l.kind == Look.MONSTER || l.kind == Look.MONKEY) l.hair = Look.H_NONE;
            else l.hair = Look.H_SHORT;
        }
        if (Txt.has(hairS, "सफ़ेद बाल", "white hair", "grey hair")) l.hairColor = 0xFFDDDDDD;
        if (l.kind == Look.WITCH) l.hairColor = 0xFF55585A;
        String ribS = sentenceWith(face + "\n" + clothes, "रिबन", "ribbon");
        if (ribS.length() > 0) {
            List<Integer> rc = colorsIn(ribS);
            l.ribbon1 = rc.size() > 0 ? rc.get(0) : 0xFFC62828;
            l.ribbon2 = rc.size() > 1 ? rc.get(1) : l.ribbon1;
        }

        // ---- face details
        String bindiS = sentenceWith(face, "बिंदी", "bindi");
        if (bindiS.length() > 0) { List<Integer> bc = colorsIn(bindiS); l.bindi = bc.isEmpty() ? 0xFFC62828 : bc.get(0); }
        String tilakS = sentenceWith(face + "\n" + clothes, "तिलक", "tilak");
        if (tilakS.length() > 0) { List<Integer> tc = colorsIn(tilakS); l.tilak = tc.isEmpty() ? 0xFFD32F2F : tc.get(0); }
        l.sindoor = Txt.has(face, "सिंदूर", "sindoor");
        if (Txt.has(face, "मूँछ", "मूंछ", "mustache", "moustache")) {
            l.mustache = Txt.has(sentenceWith(face, "मूँछ", "मूंछ", "mustache"), "रोबीली", "रोबदार", "ऊपर उठी", "बड़ी", "घनी", "big", "curl") ? 2 : 1;
        }
        l.beard = Txt.has(face, "दाढ़ी", "beard");
        l.scar = Txt.has(face, "घाव", "scar", "निशान");
        l.fangs = Txt.has(face, "fangs", "नुकीले दाँत", "नुकीले दांत", "दाँत (fangs)");
        l.glowEyes = Txt.has(face, "चमकती", "दहकत", "अंगारे", "glow");
        l.wrinkles = Txt.has(face, "झुर्रि", "wrinkle");
        l.crookedNose = Txt.has(face, "टेढ़ी", "नुकीली नाक", "crooked", "hooked nose");
        l.dimples = Txt.has(face, "डिंपल", "गड्ढे", "dimple");
        l.longNails = Txt.has(face, "नाखून", "nails");
        String eyeS = sentenceWith(face, "आँखें", "आंखें", "आँखों", "eyes");
        List<Integer> ec = colorsIn(eyeS);
        if (l.glowEyes && !ec.isEmpty()) {
            l.eyeColor = Txt.has(eyeS, "लाल") ? 0xFFFF5A1F : Txt.has(eyeS, "हरी") ? 0xFFB8E04A : ec.get(0);
        } else if (Txt.has(eyeS, "बादामी", "hazel")) l.eyeColor = 0xFF6B4423;

        // ---- outfit
        if (Txt.has(clothes, "घाघरा", "लहंगा", "lehenga", "ghagra")) l.outfit = Look.O_LEHENGA;
        else if (Txt.has(clothes, "साड़ी", "saree", "sari")) l.outfit = Look.O_SAREE;
        else if (Txt.has(clothes, "सलवार", "salwar")) l.outfit = Look.O_SALWAR;
        else if (Txt.has(clothes, "अचकन", "शेरवानी", "achkan", "sherwani")) l.outfit = Look.O_ACHKAN;
        else if (Txt.has(clothes, "वर्दी", "uniform", "सैनिक पोशाक", "सैनिक")) l.outfit = Look.O_UNIFORM;
        else if (Txt.has(clothes, "लबादा", "cloak", "robe", "चोगा")) l.outfit = Look.O_CLOAK;
        else if (Txt.has(clothes, "कवच", "armor", "armour")) l.outfit = Look.O_ARMOR;
        else if (Txt.has(clothes, "बंडी", "jacket", "जैकेट")) l.outfit = Look.O_JACKET;
        else if (Txt.has(clothes, "फ्रॉक", "frock")) l.outfit = Look.O_FROCK;
        else if (Txt.has(clothes, "कुर्ता", "kurta")) l.outfit = l.female ? Look.O_SALWAR : Look.O_KURTA;
        else if (base == null) {
            switch (l.kind) {
                case Look.GIRL: l.outfit = Look.O_LEHENGA; break;
                case Look.WOMAN: l.outfit = Look.O_SAREE; break;
                case Look.WITCH: l.outfit = Look.O_CLOAK; break;
                case Look.MONSTER: l.outfit = Look.O_ARMOR; break;
                case Look.MONKEY: l.outfit = Look.O_JACKET; break;
                default: l.outfit = Look.O_KURTA;
            }
        }
        String firstClothes = sentenceWith(clothes.replaceFirst("^[^:]*:", ""), "रंग", "का", "की", "colour", "color", "वर्दी", "पोशाक",
                "लबादा", "कवच", "बंडी", "साड़ी", "अचकन");
        List<Integer> cc = colorsIn(firstClothes);
        if (cc.isEmpty()) cc = colorsIn(clothes);
        if (!cc.isEmpty()) {
            l.primary = cc.get(0);
            l.secondary = cc.size() > 1 ? cc.get(1) : darker(cc.get(0), 0.7f);
            l.accent = cc.size() > 2 ? cc.get(2) : 0xFFE0A526;
        } else if (base == null) {
            if (l.kind == Look.MONSTER) { l.primary = 0xFF6B4226; l.secondary = 0xFF4A4A4A; }
            else if (l.kind == Look.WITCH) { l.primary = 0xFF2A2730; l.secondary = 0xFF2F5A3A; }
            else if (l.female) { l.primary = 0xFFD8457A; l.secondary = 0xFFE0A526; }
            else { l.primary = 0xFFF4F1EA; l.secondary = 0xFF8A6A3A; }
        }
        if (l.kind == Look.MONKEY) {
            List<Integer> kc = colorsIn(clothes);
            l.primary = kc.isEmpty() ? 0xFF9B1B1B : kc.get(0);
            String band = sentenceWith(clothes, "रुमाल", "bandana", "scarf");
            List<Integer> bc = colorsIn(band.length() > 0 ? band.substring(Math.max(0, Txt.firstIndex(band, "गले", "रुमाल", "bandana"))) : "");
            l.secondary = bc.isEmpty() ? 0xFFF2C230 : bc.get(0);
        }
        String shoeS = sentenceWith(clothes, "जूति", "जूते", "shoes", "juti");
        List<Integer> sc = colorsIn(shoeS);
        if (!sc.isEmpty()) l.shoeColor = sc.get(0);

        // ---- headwear
        String headS = sentenceWith(all, "पगड़ी", "साफ़ा", "साफा", "turban", "टोपी", "hat", "मुकुट", "crown", "पल्लू", "घूँघट", "सींग", "horns");
        if (Txt.has(headS, "पगड़ी", "साफ़ा", "साफा", "turban")) {
            l.headwear = Look.HW_TURBAN;
            String ts = headS.substring(Math.max(0, Txt.firstIndex(headS, "पगड़ी", "turban", "साफ") - 30));
            List<Integer> tc = colorsIn(ts);
            int pg = Txt.firstIndex(ts, "पगड़ी", "turban", "साफ");
            List<Integer> tcBefore = colorsIn(ts.substring(0, Math.max(0, pg)));
            l.headColor = !tcBefore.isEmpty() ? tcBefore.get(tcBefore.size() - 1) : (!tc.isEmpty() ? tc.get(0) : 0xFFC62828);
            List<Integer> after = colorsIn(ts.substring(Math.max(0, pg)));
            l.headBand = Txt.has(ts, "पट्टी", "band", "stripe") && !after.isEmpty() ? after.get(after.size() - 1) : 0;
            l.kalgi = Txt.has(headS, "कलगी", "plume", "रत्नजड़ित");
        } else if (Txt.has(headS, "मुकुट", "crown")) l.headwear = Look.HW_CROWN;
        else if (Txt.has(headS, "सींग", "horns")) l.headwear = Look.HW_HORNS;
        else if (Txt.has(headS, "टोपी", "hat") && l.kind == Look.WITCH) l.headwear = Look.HW_WITCH_HAT;
        else if (Txt.has(headS, "पल्लू", "घूँघट", "दुपट्टा")) l.headwear = Look.HW_PALLU;
        else if (base == null && l.kind == Look.WITCH) l.headwear = Look.HW_WITCH_HAT;
        if (l.kind == Look.MONSTER && Txt.has(all, "सींग", "horn")) l.headwear = Look.HW_HORNS;

        // ---- props & jewellery
        if (Txt.has(all, "तलवार", "sword")) l.sword = true;
        if (Txt.has(all, "कटार", "dagger")) l.katar = true;
        if (Txt.has(all, "भाला", "भाले", "spear")) l.spear = true;
        if (Txt.has(all, "ढाल", "shield")) l.shield = true;
        if (Txt.has(all, "छड़ी", "wand", "staff")) l.wand = true;
        if (Txt.has(all, "परशु", "कुल्हाड़ी", "axe")) l.axe = true;
        if (Txt.has(all, "गदा", "mace")) l.mace = true;
        if (Txt.has(all, "पोटली", "थैला", "satchel", "bag")) l.satchel = true;
        if (Txt.has(all, "जंजीर", "chain")) l.chains = true;
        if (Txt.has(all, "हड्डियों की माला", "bones")) l.necklace = 3;
        else if (Txt.has(all, "पन्ने", "royal necklace", "रत्न")) l.necklace = 4;
        else if (Txt.has(all, "मोतियों", "pearl")) l.necklace = 2;
        else if (Txt.has(all, "लॉकेट", "सोने का भारी हार", "हार", "necklace")) l.necklace = 1;
        l.bangles = Txt.has(all, "चूड़ि", "कड़े", "bangle");
        l.earrings = Txt.has(all, "झुमके", "बूंदे", "बाली", "earring");
        l.anklets = Txt.has(all, "घंटियाँ", "पायल", "घुंघरू", "anklet");
        return l;
    }
}
