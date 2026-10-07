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
            else {
                l.female = fem > mal;
                boolean child = (age > 0 && age < 14) || Txt.has(all, "बच्ची", "बच्चा", "child", "kid");
                if (child) l.kind = l.female ? Look.GIRL : Look.BOY;
                else if (old && !l.female) l.kind = Look.OLD_MAN;
                else l.kind = l.female ? Look.WOMAN : Look.MAN;
            }
            l.hero = !(monster || witch || Txt.has(all, "दुष्ट", "खलनायक", "villain", "evil"));
        }

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
