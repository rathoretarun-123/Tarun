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
    /** v34: the outfit a text names (Look.O_*), or -1 when it names none. */
    static int outfitIn(String clothes, boolean female) {
        if (Txt.has(clothes, "trench", "overcoat", "ओवरकोट", "long coat", "लंबा कोट", "coat", "कोट")) return Look.O_COAT;
        if (Txt.has(clothes, "hoodie", "हुडी", "हूडी", "sweatshirt")) return Look.O_HOODIE;
        if (Txt.has(clothes, "t-shirt", "tshirt", "टी-शर्ट", "टीशर्ट", "tee ", "shorts", "हाफ पैंट", "निकर")) return Look.O_TSHIRT;
        if (Txt.has(clothes, "suit", "सूट", "blazer", "ब्लेज़र", "tuxedo", "formal")) return Look.O_SUIT;
        if (Txt.has(clothes, "jeans", "जींस", "जीन्स", "trousers", "pants", "पैंट", "denim")) return Look.O_JEANS;
        if (Txt.has(clothes, "घाघरा", "लहंगा", "lehenga", "ghagra")) return Look.O_LEHENGA;
        if (Txt.has(clothes, "साड़ी", "saree", "sari")) return Look.O_SAREE;
        if (Txt.has(clothes, "सलवार", "salwar")) return Look.O_SALWAR;
        if (Txt.has(clothes, "अचकन", "शेरवानी", "achkan", "sherwani")) return Look.O_ACHKAN;
        if (Txt.has(clothes, "वर्दी", "uniform", "सैनिक पोशाक")) return Look.O_UNIFORM;
        if (Txt.has(clothes, "लबादा", "cloak", "robe", "चोगा")) return Look.O_CLOAK;
        if (Txt.has(clothes, "कवच", "armor", "armour")) return Look.O_ARMOR;
        if (Txt.has(clothes, "बंडी", "jacket", "जैकेट")) return Look.O_JACKET;
        if (Txt.has(clothes, "फ्रॉक", "frock", "gown", "गाउन", "dress", "ड्रेस")) return female ? Look.O_FROCK : -1;
        if (Txt.has(clothes, "कुर्ता", "kurta")) return female ? Look.O_SALWAR : Look.O_KURTA;
        return -1;
    }

    /**
     * v34: the look after a change of clothes named in a sentence — the outfit and its colours, glasses or goggles
     * put on or taken off; everything else (the face, the hair, the build) stays the character's own.
     */
    public static Look restyle(Look base, String text) {
        Look l = base.copy();
        int o = outfitIn(text, base.female);
        if (o >= 0) l.outfit = o;
        // v34: a bandage, a plaster cast, a sling, a blindfold or an eye patch put on (or taken off): the clothes stay
        // as they are unless the sentence names new ones ("सफ़ेद पट्टी" is the bandage's colour, not the shirt's)
        int inj = injuryIn(text), cover = coverIn(text);
        boolean healed = Txt.has(text, "प्लास्टर कट", "प्लास्टर उतर", "प्लास्टर खुल", "पट्टी खुल", "plaster comes off", "cast comes off", "cast is removed", "plaster is removed", "cast is cut",
                "bandage comes off", "bandage is removed", "removes the bandage", "takes off the bandage", "out of the sling", "sling comes off");
        boolean unblind = Txt.has(text, "पट्टी खोल", "पट्टी हटा", "पट्टी उतार", "removes the blindfold", "takes off the blindfold", "took off the blindfold", "blindfold off", "unties the blindfold",
                "removes the eye patch", "takes off the eye patch", "removes his eye patch", "removes her eye patch");
        boolean body = inj != 0 || cover > 0 || healed || unblind;
        if (inj != 0) l.injury |= inj;
        if (cover > 0) l.glasses = cover;
        if (unblind && (base.glasses == 4 || base.glasses == 5)) l.glasses = 0;
        else if (unblind && base.injury != 0) healed = true;           // "मोनू के सिर की पट्टी खोलते हैं": the bandage comes off
        if (healed) {
            // only the part named heals ("सिर की पट्टी" — the head); with no part named, every bandage comes off
            int part = (Txt.has(text, "सिर", "माथ", "head", "forehead") ? Look.INJ_HEAD : 0) | (Txt.has(text, "पैर", "टांग", "टाँग", "पाँव", "पांव", "टखन", "leg", "foot", "ankle") ? Look.INJ_LEG : 0)
                    | (Txt.has(text, "हाथ", "बाँह", "बांह", "arm", "sling") ? Look.INJ_ARM : 0);
            l.injury = part == 0 ? 0 : l.injury & ~part;
            // crutches were for the leg: they go with its plaster (an old person's stick stays)
            if ((base.injury & Look.INJ_LEG) != 0 && (l.injury & Look.INJ_LEG) == 0 && l.aid == Look.AID_CRUTCHES) l.aid = Look.AID_NONE;
        }
        List<Integer> cc = body && o < 0 ? new java.util.ArrayList<Integer>() : colorsIn(text);
        if (!cc.isEmpty()) { l.primary = cc.get(0); l.secondary = cc.size() > 1 ? cc.get(1) : darker(cc.get(0), 0.7f); }
        if (body) return l;
        if (Txt.has(text, "चश्मा उतार", "चश्मा हटा", "takes off his glasses", "takes off her glasses", "took off his glasses", "took off her glasses", "removes his glasses", "removes her glasses",
                "takes off the goggles", "takes off his goggles", "takes off her goggles")) l.glasses = 0;
        else if (Txt.has(text, "गॉगल", "goggle")) l.glasses = 3;
        else if (Txt.has(text, "काला चश्मा", "sunglass", "धूप का चश्मा", "dark glasses")) l.glasses = 2;
        else if (Txt.has(text, "चश्मा लगा", "चश्मा पहन", "puts on his glasses", "puts on her glasses", "put on his glasses", "put on her glasses", "wearing glasses", "with glasses")) l.glasses = Math.max(1, l.glasses);
        return l;
    }

    /** v34: true when a sentence changes what a character wears ("पहनकर आती है", "changes into", "puts on"). */
    public static boolean changesClothes(String s) {
        if (bodyChange(s)) return true;
        return Txt.has(s, "कपड़े बदल", "ड्रेस बदल", "पोशाक बदल", "वेशभूषा बदल", "वस्त्र बदल", "पहनकर", "पहन कर", "पहन लेत", "पहन लिय", "पहन ली", "पहने हुए", "पहनती है", "पहनता है",
                "चश्मा लगा", "चश्मा पहन", "चश्मा उतार", "चश्मा हटा", "changes into", "changed into", "change into", "changes her clothes", "changes his clothes", "puts on", "put on",
                "now wearing", "now in a", "dressed in", "dresses up", "dressed up", "wearing", "takes off his glasses", "takes off her glasses", "removes his glasses", "removes her glasses");
    }

    /**
     * v34: a walking stick, crutches or a wheelchair named in a description or an action ("लाठी टेकते हुए",
     * "छड़ी के सहारे", "बैसाखी", "व्हीलचेयर पर", "walks with a cane", "in a wheelchair"); a bare "छड़ी" is a walking
     * stick only for an old person (a magic wand otherwise).
     */
    public static int aidIn(String t, boolean old) {
        if (Txt.has(t, "व्हीलचेयर", "व्हील चेयर", "व्हील-चेयर", "पहिया कुर्सी", "पहियों वाली कुर्सी", "wheelchair", "wheel chair", "wheel-chair")) return Look.AID_WHEELCHAIR;
        if (Txt.has(t, "वॉकर", "वाकर के सहारे", "walking frame", "zimmer", "with a walker", "with her walker", "with his walker", "on a walker", "on her walker", "on his walker",
                "behind a walker", "uses a walker", "pushes her walker", "pushes his walker")) return Look.AID_WALKER;
        if (Txt.has(t, "बैसाखी", "बैसाखियों", "बैसाखियाँ", "crutch")) return Look.AID_CRUTCHES;
        if (Txt.has(t, "जादुई छड़ी", "magic wand", "magic stick", "wand")) return Look.AID_NONE;
        if (Txt.has(t, "लाठी", "छड़ी के सहारे", "छड़ी टेक", "छड़ी लेकर चल", "छड़ी से चल", "walking stick", "walking-stick", "walking cane", "with a cane", "a cane", "his cane", "her cane",
                "leans on a stick", "leaning on a stick", "with a stick", "his stick", "her stick")) return Look.AID_STICK;
        if (old && Txt.has(t, "छड़ी", "stick", "cane")) return Look.AID_STICK;
        return Look.AID_NONE;
    }

    /**
     * v34: what is hurt and bandaged, named in a description or an action — an arm in plaster or a sling ("हाथ पर
     * प्लास्टर", "arm in a sling"), a leg in plaster or a sprain ("पैर में प्लास्टर", "broken leg"), a bandage round the
     * head ("सिर पर पट्टी") — as Look.INJ_* bits. A slingshot (गुलेल) is no sling.
     */
    public static int injuryIn(String t) {
        int inj = 0;
        if (Txt.has(t, "हाथ पर प्लास्टर", "हाथ में प्लास्टर", "बाँह पर प्लास्टर", "बांह पर प्लास्टर", "बाँह में प्लास्टर", "बांह में प्लास्टर", "हाथ टूट", "टूटा हाथ", "टूटी बाँह", "टूटी बांह",
                "हाथ पर पट्टी", "बाँह पर पट्टी", "बांह पर पट्टी", "हाथ गले में लटका", "arm in a sling", "arm in sling", "arm in a cast", "arm in plaster", "broken arm", "plaster on his arm",
                "plaster on her arm", "cast on his arm", "cast on her arm", "bandaged arm", "fractured arm")) inj |= Look.INJ_ARM;
        if (Txt.has(t, "पैर में प्लास्टर", "पैर पर प्लास्टर", "टांग में प्लास्टर", "टाँग में प्लास्टर", "टांग पर प्लास्टर", "टाँग पर प्लास्टर", "पैर टूट", "टूटा पैर", "टूटी टांग", "टूटी टाँग",
                "टांग टूट", "टाँग टूट", "पैर पर पट्टी", "पैर में पट्टी", "पैर में मोच", "टखने में मोच", "पाँव में मोच", "पांव में मोच", "leg in a cast", "leg in plaster", "broken leg",
                "plaster on his leg", "plaster on her leg", "cast on his leg", "cast on her leg", "bandaged leg", "bandaged foot", "fractured leg", "sprained ankle", "sprained her ankle",
                "sprained his ankle", "twisted ankle")) inj |= Look.INJ_LEG;
        if (Txt.has(t, "सिर पर पट्टी", "माथे पर पट्टी", "सिर में पट्टी", "सिर पर बैंडेज", "माथे पर बैंडेज", "सिर में चोट", "सिर पर चोट", "माथे पर चोट", "bandage on his head",
                "bandage on her head", "bandage round his head", "bandage around his head", "bandage round her head", "bandage around her head", "bandaged head", "head bandage",
                "head injury", "bandage on his forehead", "bandage on her forehead", "plaster on his forehead", "plaster on her forehead")) inj |= Look.INJ_HEAD;
        // "the doctor bandages Rohan's leg", "डॉक्टर ने हाथ पर प्लास्टर चढ़ाया": a bandage or a plaster put on, and the part it goes on
        if (inj == 0 && Txt.has(t, "bandage", "plaster cast", "in plaster", "a cast", "पट्टी बाँध", "पट्टी बांध", "पट्टी लगा", "प्लास्टर चढ़", "प्लास्टर लग", "प्लास्टर बँध", "प्लास्टर बंध")) {
            if (Txt.has(t, " leg", " foot", " knee", "ankle", "पैर", "टांग", "टाँग", "पाँव", "पांव", "टखन", "घुटन")) inj |= Look.INJ_LEG;
            if (Txt.has(t, " arm", " wrist", " elbow", "हाथ", "बाँह", "बांह", "कलाई", "कोहनी")) inj |= Look.INJ_ARM;
            if (Txt.has(t, " head", "forehead", "सिर", "माथ")) inj |= Look.INJ_HEAD;
        }
        return inj;
    }

    /**
     * v35: the body as a description (or an action) tells it, as Look.C_* bits: a limp; an arm or a leg missing (the
     * right one when the sentence says so, else the left); an artificial leg; fingers missing; one eye; blind; deaf;
     * unable to speak, or speaking in signs; a hearing aid. Phrases that only describe a moment ("closes one eye",
     * "holds up four fingers", "in the dark he cannot see") are not read as the body.
     */
    public static int conditionIn(String t) {
        int c = 0;
        if (Txt.has(t, "लंगड़ा", "लंगड़ी", "लँगड़ा", "लँगड़ी", "लंगड़ाता", "लंगड़ाती", "लंगड़ाकर", "लँगड़ाकर", "लंगड़ाते", "लँगड़ाते", "लंगड़ाहट", "limps", "limping",
                "with a limp", "lame leg", "bad leg", "one leg shorter", "polio")) c |= Look.C_LIMP;
        String arm = sentenceWith(t, "one-armed", "one armed", "only one arm", "has one arm", "lost an arm", "lost his arm", "lost her arm", "lost his left arm",
                "lost his right arm", "lost her left arm", "lost her right arm", "missing an arm", "missing arm", "missing his arm", "missing her arm", "without an arm",
                "no left arm", "no right arm", "empty sleeve", "एक हाथ नहीं", "एक ही हाथ", "एक हाथ वाला", "एक हाथ वाली", "एक बाँह नहीं", "एक बांह नहीं", "हाथ कटा", "हाथ कट गया",
                "कटा हुआ हाथ", "बाँह कटी", "बांह कटी");
        if (arm.length() > 0) {
            c |= Look.C_NO_ARM;
            if (Txt.has(arm, "right", "दायाँ", "दायां", "दाएँ", "दाएं", "दाहिना", "दाहिने", "दायें")) c |= Look.C_RIGHT_ARM;
        }
        String leg = sentenceWith(t, "one-legged", "one legged", "only one leg", "has one leg", "lost a leg", "lost his leg", "lost her leg", "missing a leg", "missing leg",
                "without a leg", "wooden leg", "peg leg", "peg-leg", "prosthetic leg", "prosthetic foot", "artificial leg", "metal leg", "jaipur foot", "जयपुर फुट", "एक पैर नहीं",
                "एक ही पैर", "एक पैर वाला", "एक पैर वाली", "एक टांग नहीं", "एक टाँग नहीं", "एक टांग वाला", "एक टाँग वाला", "लकड़ी का पैर", "लकड़ी की टांग", "लकड़ी की टाँग",
                "नकली पैर", "कृत्रिम पैर", "पैर कटा", "पैर कट गया", "टांग कटी", "टाँग कटी");
        if (leg.length() > 0) {
            c |= Look.C_NO_LEG;
            if (Txt.has(leg, "right", "दायाँ", "दायां", "दाएँ", "दाएं", "दाहिना", "दाहिने", "दायें")) c |= Look.C_RIGHT_LEG;
            if (Txt.has(leg, "wooden", "peg", "prosthetic", "artificial", "metal leg", "jaipur", "जयपुर", "लकड़ी", "नकली", "कृत्रिम")) c |= Look.C_ARTIFICIAL_LEG;
        }
        if (Txt.has(t, "missing finger", "lost a finger", "lost two fingers", "lost fingers", "lost his finger", "lost her finger", "fingers missing", "a finger missing",
                "only three fingers", "only four fingers", "has three fingers", "has four fingers", "उंगली कटी", "उँगली कटी", "उंगलियाँ कटी", "उंगलियां कटी", "कटी हुई उंगली",
                "कटी हुई उँगली", "सिर्फ़ तीन उंगलियाँ", "सिर्फ तीन उंगलियाँ", "सिर्फ़ चार उंगलियाँ", "सिर्फ चार उंगलियाँ", "चार ही उंगलियाँ", "तीन ही उंगलियाँ")) c |= Look.C_FINGERS;
        if (wordIn(t, "काना") || wordIn(t, "कानी") || Txt.has(t, "एक आँख वाला", "एक आँख वाली", "एक आंख वाला", "एक आंख वाली", "एक ही आँख", "एक ही आंख", "एक आँख नहीं",
                "एक आंख नहीं", "एक आँख से अंधा", "एक आंख से अंधा", "एक आँख से अंधी", "एक आंख से अंधी", "one-eyed", "one eyed", "only one eye", "has one eye", "lost an eye",
                "lost his eye", "lost her eye", "blind in one eye", "glass eye", "missing eye")) c |= Look.C_ONE_EYE;
        else if (Txt.has(t, "अंधा", "अंधी", "अन्धा", "अन्धी", "नेत्रहीन", "दृष्टिहीन", "blind man", "blind woman", "blind boy", "blind girl", "blind old", "is blind", "who is blind",
                "visually impaired", "born blind", "जन्म से अंध")
                && !Txt.has(t, "अँधेरे में", "अंधेरे में", "in the dark", "blindfold")) c |= Look.C_BLIND;
        if (Txt.has(t, "बहरा", "बहरी", "बधिर", "सुन नहीं सकता", "सुन नहीं सकती", "hard of hearing", "cannot hear", "can't hear", "hearing aid", "कान की मशीन",
                "सुनने की मशीन", "हियरिंग एड") || wordIn(t, "deaf")) c |= Look.C_DEAF;
        if (Txt.has(t, "hearing aid", "कान की मशीन", "सुनने की मशीन", "हियरिंग एड")) c |= Look.C_HEARING_AID;
        if ((Txt.has(t, "गूंगा", "गूँगा", "गूंगी", "गूँगी", "बोल नहीं सकता", "बोल नहीं सकती", "बोल नहीं पाता", "बोल नहीं पाती", "cannot speak", "can't speak", "speechless from birth")
                || wordIn(t, "मूक") || wordIn(t, "mute")) && !Txt.has(t, "मूकदर्शक")) c |= Look.C_MUTE;
        if (Txt.has(t, "sign language", "signs to", "in signs", "सांकेतिक भाषा", "साइन लैंग्वेज", "इशारों में बात", "इशारों से बात", "इशारों की भाषा")) c |= Look.C_SIGNS;
        return c;
    }

    /** v34: a sentence that puts on or takes off a bandage, a plaster, a sling, a blindfold or an eye patch. */
    static boolean bodyChange(String s) {
        return injuryIn(s) != 0 || coverIn(s) > 0 || Txt.has(s, "प्लास्टर कट", "प्लास्टर उतर", "प्लास्टर खुल", "पट्टी खुल", "पट्टी खोल", "पट्टी हटा", "पट्टी उतार", "plaster comes off",
                "cast comes off", "cast is removed", "plaster is removed", "cast is cut", "bandage comes off", "bandage is removed", "removes the bandage", "takes off the bandage",
                "out of the sling", "sling comes off", "removes the blindfold", "takes off the blindfold", "took off the blindfold", "blindfold off", "unties the blindfold",
                "removes the eye patch", "takes off the eye patch", "removes his eye patch", "removes her eye patch");
    }

    /** v34: a cloth blindfold over both eyes (4: Gandhari, a game of blind man's buff) or a patch over one eye (5: a pirate); 0 none. */
    public static int coverIn(String t) {
        if (Txt.has(t, "eye patch", "eyepatch", "eye-patch", "एक आँख पर पट्टी", "एक आंख पर पट्टी", "आँख पर काली पट्टी", "आंख पर काली पट्टी", "patch over one eye", "patch over his eye",
                "patch over her eye", "आई पैच", "आईपैच", "आँख पर पैच", "आंख पर पैच")) return 5;
        if (Txt.has(t, "आँखों पर पट्टी", "आंखों पर पट्टी", "आँखों पर बँधी", "आंखों पर बंधी", "आँखों पर कपड़ा", "आंखों पर कपड़ा", "blindfold", "eyes covered with", "eyes bandaged",
                "cloth over her eyes", "cloth over his eyes", "cloth tied over")) return 4;
        return 0;
    }

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
                {Look.SP_GOAT, "बकरी", "बकरा", "goat"}, {Look.SP_COW, "गाय", "बैल", "cow", "ox", "bull"}, {Look.SP_WOLF, "भेड़िया", "wolf"},
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
        // a robot animal: metal where the fur would be, screens for eyes
        l.robot = wordIn(all, "robot") || wordIn(all, "रोबोट") || wordIn(all, "रोबो") || wordIn(all, "robo") || Txt.has(all, "robot dog", "robo-dog", "mechanical dog");
        if (l.robot && cols.isEmpty()) { l.furColor = 0xFFB0B8C4; l.skin = 0xFFDDE3EA; }
        // a little jacket and a bandana (a dog's, a monkey's)
        String clothes = sentenceWith(all, "jacket", "जैकेट", "बंडी", "कोट", "sweater", "स्वेटर");
        if (clothes.length() > 0) { l.outfit = Look.O_JACKET; List<Integer> jc = colorsIn(clothes); if (!jc.isEmpty()) l.primary = jc.get(0); }
        String band = sentenceWith(all, "bandana", "रुमाल", "scarf", "गले में");
        if (band.length() > 0) { List<Integer> bc = colorsIn(band.substring(Math.max(0, Txt.firstIndex(band, "गले", "bandana", "रुमाल", "scarf")))); if (!bc.isEmpty()) l.secondary = bc.get(0); }
    }

    /**
     * v34: how many heads the name or the description gives a character: Ravana (दशानन) has ten; "दस सिर", "तीन सिरों
     * वाला", "three-headed", "2 heads" are read as the number; "कई सिर" / "many heads" as three. Else 1.
     */
    public static int headsIn(String text) {
        if (text == null) return 1;
        String t = text.toLowerCase(java.util.Locale.ROOT);
        if (Txt.has(t, "रावण", "दशानन", "ravan", "raavan", "dashanan")) return 10;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([\\p{L}\\p{M}]+|\\d+)[\\s\\-]*(?:सिरों|सिरो|सिर|मुखों|मुख|शीश|heads?|headed)(?![\\p{L}\\p{M}])").matcher(t);
        int best = 1;
        while (m.find()) {
            String n = m.group(1);
            int v = 0;
            if (n.matches("\\d+")) { try { v = Integer.parseInt(n); } catch (NumberFormatException ignored) { v = 0; } }
            else {
                String[][] words = {{"दो", "two"}, {"तीन", "three"}, {"चार", "four"}, {"पाँच", "पांच", "five"}, {"छह", "छः", "six"}, {"सात", "seven"},
                        {"आठ", "eight"}, {"नौ", "nine"}, {"दस", "ten"}, {"कई", "अनेक", "many", "several", "multi"}};
                int[] vals = {2, 3, 4, 5, 6, 7, 8, 9, 10, 3};
                for (int i = 0; i < words.length && v == 0; i++) for (String w : words[i]) if (n.equals(w)) v = vals[i];
            }
            if (v > best) best = v;
        }
        return Math.min(100, best);
    }

    /**
     * v34: how many arms the name or the description gives a character: Durga has eight; "आठ भुजाएँ", "चार हाथों वाला",
     * "अष्टभुजा", "चतुर्भुज", "ten-armed", "4 arms" are read as the number. Else 2.
     */
    public static int armsIn(String text) {
        if (text == null) return 2;
        String t = text.toLowerCase(java.util.Locale.ROOT);
        int best = 2;
        if (Txt.has(t, "अष्टभुज", "ashtabhuj")) best = 8;
        if (Txt.has(t, "चतुर्भुज", "chaturbhuj")) best = Math.max(best, 4);
        if (Txt.has(t, "दशभुज")) best = 10;
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([\\p{L}\\p{M}]+|\\d+)[\\s\\-]*(?:भुजाओं|भुजाएँ|भुजाएं|भुजाओ|भुजा|हाथों|हाथ|arms|armed|hands|handed)(?![\\p{L}\\p{M}])").matcher(t);
        while (m.find()) {
            String n = m.group(1);
            int v = 0;
            if (n.matches("\\d+")) { try { v = Integer.parseInt(n); } catch (NumberFormatException ignored) { v = 0; } }
            else {
                String[][] words = {{"चार", "four"}, {"छह", "छः", "six"}, {"आठ", "eight"}, {"दस", "ten"}, {"बारह", "twelve"}, {"सोलह", "sixteen"}, {"अठारह", "eighteen"}, {"बीस", "twenty"}, {"कई", "अनेक", "many", "several", "multi"}};
                int[] vals = {4, 6, 8, 10, 12, 16, 18, 20, 6};
                for (int i = 0; i < words.length && v == 0; i++) for (String w : words[i]) if (n.equals(w)) v = vals[i];
            }
            if (v > best) best = v;
        }
        if (best == 2 && Txt.has(t, "दुर्गा", "durga", "दुर्गे")) best = 8;
        return Math.min(20, best);
    }

    /**
     * v34: the animal a character rides or sits on, from "शेर पर सवार", "सिंह पर बैठी", "बाघ की सवारी", "riding a
     * lion", "on a tiger", "mounted on an elephant"; Durga's lion when nothing else is said. -1 when none.
     */
    public static int mountIn(String text) {
        if (text == null) return -1;
        String t = text.toLowerCase(java.util.Locale.ROOT);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("([\\p{L}\\p{M}]+)\\s+(?:पर|पे)\\s+(?:सवार|बैठी|बैठा|बैठे|बैठकर|खड़ी|खड़ा|विराजमान|आरूढ़)").matcher(t);
        while (m.find()) { int sp = speciesWord(m.group(1)); if (sp >= 0) return sp; }
        m = java.util.regex.Pattern.compile("([\\p{L}\\p{M}]+)\\s+(?:की|के)\\s+(?:सवारी|पीठ पर)").matcher(t);
        while (m.find()) { int sp = speciesWord(m.group(1)); if (sp >= 0) return sp; }
        m = java.util.regex.Pattern.compile("(?:riding|rides|astride|mounted on|sitting on|seated on|sits on|on the back of|on)\\s+(?:a |an |the |her |his |its )?([a-z]+)(?:\\s+([a-z]+))?").matcher(t);
        while (m.find()) {
            int sp = speciesWord(m.group(1));                                       // "on a lion"
            if (sp < 0 && m.group(2) != null) sp = speciesWord(m.group(2));         // "on a white bull"
            if (sp >= 0) return sp;
        }
        if (Txt.has(t, "दुर्गा", "durga", "दुर्गे")) return Look.SP_LION;
        return -1;
    }

    /** The species one word names (the mount's word), or -1. */
    static int speciesWord(String word) {
        String w = word.trim();
        if (w.length() < 2) return -1;
        // the species table's words are matched whole; a Hindi word before "पर" / "की" is in its oblique form
        // ("चूहे", "घोड़े", "हाथी"), an English one may be a plural
        int sp = speciesOf(w + "।");
        if (sp < 0 && w.endsWith("े")) sp = speciesOf(w.substring(0, w.length() - 1) + "ा।");
        if (sp < 0 && w.endsWith("ों")) sp = speciesOf(w.substring(0, w.length() - 2) + "ा।");
        if (sp < 0 && w.endsWith("s") && w.length() > 3) sp = speciesOf(w.substring(0, w.length() - 1) + "।");
        return sp;
    }

    public static Look design(Story.CharacterDef c, Look base) {
        String all = c.fullName + "\n" + c.description;
        String face = section(c.description, "चेहरा", "face", "शरीर", "body", "रूप");
        String clothes = section(c.description, "पहनावा", "पोशाक", "कपड़े", "outfit", "dress", "clothes", "costume");
        if (face.length() == 0) face = all;
        if (clothes.length() == 0) clothes = all;
        Look l = base != null ? base.copy() : new Look();

        // ---- kind
        // whole words only: a "डायनासोर" (dinosaur) on a t-shirt is no "डायन" (witch), a "switch" no witch
        boolean monster = wordIn(all, "राक्षस") || wordIn(all, "दानव") || wordIn(all, "दैत्य") || wordIn(all, "असुर") || wordIn(all, "monster") || wordIn(all, "demon") || wordIn(all, "ogre");
        boolean witch = wordIn(all, "चुड़ैल") || wordIn(all, "डायन") || wordIn(all, "witch") || wordIn(all, "जादूगरनी")
                || (c.role != null && (wordIn(c.role, "witch") || wordIn(c.role, "डायन") || wordIn(c.role, "चुड़ैल")));
        boolean monkey = Txt.has(all, "बंदर", "वानर", "मकाक", "monkey", "macaque");
        // v34: the animal a character rides (Durga's lion) is a mount, never the character's own species
        int mount = mountIn(all);
        int species = monkey ? -1 : speciesOf(c.fullName + " " + c.description);
        if (mount >= 0 && species == mount) species = -1;
        String noPrincess = Txt.norm(all).replace(Txt.norm("राजकुमारी"), "");
        int fem = 0, mal = 0;
        String[] femW = {"राजकुमारी", "रानी", "लड़की", "बेटी", "सहेली", "माँ", "दादी", "नानी", "बहन", "दीदी", "बिंदी", "साड़ी", "देवी", "goddess", "माता",
                "लहंगा", "घाघरा", "चोली", "सिंदूर", "चूड़ि", "princess", "queen", "girl", "woman", "mother", "lady", "aunt", "आंटी", "she ", " her ",
                "गोरी", "दुबली", "मोटी", "सुंदरी", "बहादुर लड़की", "sister", "daughter", "grandmother", "बुआ", "मौसी", "चाची", "मैडम", "madam", "miss ",
                // v34: what she wears says it when no word does ("two ponytails with yellow ribbons")
                "ponytails", "pigtails", "ribbons", "ribbon", "रिबन", "frock", "फ्रॉक", "skirt", "स्कर्ट", "a dress", "her dress", "gown", "earrings", "झुमके", "hair clip", "hairband"};
        String[] malW = {"राजा", "राजकुमार", "लड़का", "बेटा", "पिता", "दादा", "भाई", "भैया", "गार्ड", "सिपाही", "सैनिक", "मूँछ", "मूंछ", "देवता", " god ",
                "दाढ़ी", "पगड़ी", "अचकन", "king", "prince", "boy", "man ", "father", "guard", "soldier", " he ", " his ",
                "गोरा ", "दुबला", "मोटा", "mister", "मिस्टर", "sir ", "uncle", "अंकल", "चाचा", "मामा", "grandfather", "brother", "son "};
        for (String w : femW) if (Txt.has(all, w)) fem++;
        for (String w : malW) if (noPrincess.contains(Txt.norm(w))) mal++;
        // the script's own verbs decide a tie ("निकालती है" is a she, "दौड़ता है" a he)
        if (c.genderHint > 0) fem += fem == mal ? 1 : 0; else if (c.genderHint < 0) mal += fem == mal ? 1 : 0;
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
            // the role after the name says which side they are on ("The Corporate Rakshas", "AI Witch"), without changing what they are
            if (c.role != null && Txt.has(c.role, "राक्षस", "rakshas", "villain", "witch", "डायन", "चुड़ैल", "evil", "thief", "चोर", "dark lord", "demon", "boss", "khalnayak")) l.hero = false;
            if (species == Look.SP_FOX || species == Look.SP_WOLF) l.hero = !Txt.has(all, "चालाक", "धूर्त", "दुष्ट", "cunning", "wicked", "sly") && l.hero;
            // v34: the well-known villains of the epics are on the other side whatever their description says
            // ("रावण — दस सिर वाला लंका का राजा" names no evil, yet he never stands with the heroes on the last page)
            if (Txt.has(c.displayName + " " + (c.role == null ? "" : c.role), "रावण", "दशानन", "कंस", "महिषासुर", "हिरण्यकश्यप", "हिरण्यकशिपु", "शूर्पणखा",
                    "दुर्योधन", "शकुनि", "ताड़का", "ताडका", "पूतना", "मारीच", "बकासुर", "रक्तबीज", "शुंभ", "निशुंभ", "ravana", "ravan", "kansa", "mahishasur",
                    "hiranyakashipu", "shurpanakha", "duryodhan", "shakuni", "tadaka", "putana")) l.hero = false;
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
            // v34: a vanara (Hanuman, Sugriva, Angad) or a mighty monkey warrior stands as tall as a man; a pet monkey is small
            case Look.MONKEY: l.height = Txt.has(all, "हनुमान", "वानर", "सुग्रीव", "बाली", "अंगद", "बलवान", "महाबली", "hanuman", "vanara", "sugriva", "angad",
                    "mighty", "monkey god", "monkey warrior", "monkey king", "giant monkey", "gorilla") ? 0.98f : 0.36f; break;
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
        String hairS = sentenceWith(face, "बाल", "चोटी", "चोटि", "hair", "braid", "जूड़ा", "ponytail", "पोनीटेल", "bun");
        l.curly = Txt.has(hairS, "घुंघराले", "घुँघराले", "curly");
        l.wings = Txt.has(c.description == null ? "" : c.description, "पंख", "परी", "fairy", "wings", "winged", "angel", "फ़रिश्ता", "फरिश्ता");
        if (Txt.has(hairS, "दो चोटि", "दो साधारण चोटि", "two braids", "pigtail", "ponytails", "two pony", "दो पोनी", "twin tails", "bunches")) l.hair = Look.H_PIGTAILS;
        else if (Txt.has(hairS, "ponytail", "पोनीटेल", "पोनी")) l.hair = Look.H_PONYTAIL;
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
        l.heads = Math.max(1, headsIn(all));                    // v34: Ravana's ten heads, a three-headed dragon's three
        l.arms = Math.max(2, armsIn(all));                      // v34: Durga's eight arms
        l.mount = l.isHumanoid() ? mount : -1;                  // v34: the lion she sits on
        l.longNails = Txt.has(face, "नाखून", "nails");
        String eyeS = sentenceWith(face, "आँखें", "आंखें", "आँखों", "eyes");
        List<Integer> ec = colorsIn(eyeS);
        if (l.glowEyes && !ec.isEmpty()) {
            l.eyeColor = Txt.has(eyeS, "लाल") ? 0xFFFF5A1F : Txt.has(eyeS, "हरी") ? 0xFFB8E04A : ec.get(0);
        } else if (Txt.has(eyeS, "बादामी", "hazel")) l.eyeColor = 0xFF6B4423;

        // ---- outfit (today's clothes first: a hoodie, a t-shirt, a suit, a coat, jeans)
        if (Txt.has(clothes, "trench", "overcoat", "ओवरकोट", "long coat", "लंबा कोट", "coat", "कोट")) l.outfit = Look.O_COAT;
        else if (Txt.has(clothes, "hoodie", "हुडी", "हूडी", "sweatshirt")) l.outfit = Look.O_HOODIE;
        else if (Txt.has(clothes, "t-shirt", "tshirt", "टी-शर्ट", "टीशर्ट", "tee ", "shorts", "हाफ पैंट", "निकर")) l.outfit = Look.O_TSHIRT;
        else if (Txt.has(clothes, "suit", "सूट", "blazer", "ब्लेज़र", "tuxedo", "formal")) l.outfit = Look.O_SUIT;
        else if (Txt.has(clothes, "jeans", "जींस", "जीन्स", "trousers", "pants", "पैंट", "denim")) l.outfit = Look.O_JEANS;
        else if (Txt.has(clothes, "घाघरा", "लहंगा", "lehenga", "ghagra")) l.outfit = Look.O_LEHENGA;
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
                "लबादा", "कवच", "बंडी", "साड़ी", "अचकन", "hoodie", "t-shirt", "suit", "coat", "kurta");
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
        String shoeS = sentenceWith(clothes, "जूति", "जूते", "shoes", "juti", "sneakers", "स्नीकर", "boots", "बूट", "sandals", "चप्पल");
        List<Integer> sc = colorsIn(shoeS);
        if (!sc.isEmpty()) l.shoeColor = sc.get(0);
        l.lightShoes = Txt.has(shoeS, "लाइट", "light", "led", "चमक", "glow");
        // ---- glasses, gadgets, little lights
        String glassS = sentenceWith(face + "\n" + clothes, "चश्म", "glass", "spectacle", "specs", "goggle", "visor", "गॉगल", "ऐनक");
        if (glassS.length() > 0 && !Txt.has(glassS, "glass of", "गिलास", "wine glass", "glass door", "glass window")) {
            // v34: 1 spectacles, 2 dark glasses or a visor, 3 goggles (big round lenses on a strap: swimming, flying, welding)
            l.glasses = Txt.has(glassS, "goggle", "गॉगल", "swimming glass", "तैराकी") && !Txt.has(glassS, "sunglass", "धूप का चश्मा", "काला चश्मा") ? 3
                    : Txt.has(glassS, "AR glass", "ar glass", "काला चश्मा", "dark glass", "sunglass", "धूप का चश्मा", "visor", "काले चश्म", "black glass") ? 2 : 1;
            l.glowGlasses = l.glasses == 2 && Txt.has(glassS, "चमक", "glow", "लाल");
        }
        // v34: a blindfold or an eye patch; a bandage, a plaster cast or a sling; an umbrella for the rain
        int cover = coverIn(face + "\n" + clothes + "\n" + all);
        if (cover > 0) l.glasses = cover;
        l.injury = injuryIn(all);
        l.umbrella = Txt.has(all, "छाता", "छतरी", "umbrella");
        // v34: what helps the character walk
        l.aid = aidIn(all, l.kind == Look.OLD_MAN || Txt.has(all, "बूढ़", "बुज़ुर्ग", "बुजुर्ग", "दादा", "दादी", "नाना", "नानी", "old ", "elderly", "grandpa", "grandma"));
        // v35: the body as the description tells it (a limp, an arm or a leg missing, fingers, one eye, blind, deaf, mute)
        l.condition = l.isHumanoid() ? conditionIn(all) : 0;
        // v34: a blind character: dark glasses and a white cane
        if ((l.condition & Look.C_BLIND) != 0) {
            if (l.glasses == 0) l.glasses = 2;
            if (l.aid == Look.AID_NONE) l.aid = Look.AID_STICK;
        }
        // v35: a leg missing and no artificial one: crutches (an artificial leg walks with a limp, on its own)
        if (l.missingLeg() != 0 && (l.condition & Look.C_ARTIFICIAL_LEG) == 0 && l.aid == Look.AID_NONE) l.aid = Look.AID_CRUTCHES;
        // (v35: one eye with nothing said about a patch is drawn closed, a small scar over it — Puppet.drawEyes)
        if (Txt.has(all, "controller", "कंट्रोलर", "joystick", "जॉयस्टिक", "gamepad")) l.gadget = Look.GD_CONTROLLER;
        else if (wordIn(all, "iphone") || wordIn(all, "phone") || wordIn(all, "फ़ोन") || wordIn(all, "फोन") || wordIn(all, "मोबाइल") || wordIn(all, "mobile") || wordIn(all, "smartphone")) l.gadget = Look.GD_PHONE;
        else if (Txt.has(all, "laptop", "लैपटॉप", "tablet", "टैबलेट")) l.gadget = Look.GD_LAPTOP;
        if (Txt.has(all, "selfie stick", "सेल्फी स्टिक", "सेल्फी-स्टिक")) { l.wand = true; l.techWand = true; }
        l.ledClip = Txt.has(all, "LED क्लिप", "led clip", "hair light", "बालों में लाइट", "चमकती क्लिप");
        l.earphones = Txt.has(all, "earphone", "ईयरफोन", "headphone", "हेडफोन", "earbuds");

        // ---- headwear
        String headS = sentenceWith(all, "पगड़ी", "साफ़ा", "साफा", "turban", "टोपी", "hat", "cap", "मुकुट", "crown", "पल्लू", "घूँघट", "सींग", "horns");
        // a cap (a guard's or a soldier's) comes off and goes on like a turban; a witch's hat stays her own
        boolean cap = Txt.has(headS, "टोपी", "cap", "topi") && l.kind != Look.WITCH && !Txt.has(headS, "hoodie cap", "hood");
        if (Txt.has(headS, "पगड़ी", "साफ़ा", "साफा", "turban", "pagdi", "pagri") || cap) {
            l.headwear = Look.HW_TURBAN;
            String ts = headS.substring(Math.max(0, Txt.firstIndex(headS, "पगड़ी", "turban", "साफ", "टोपी", "cap") - 30));
            List<Integer> tc = colorsIn(ts);
            int pg = Txt.firstIndex(ts, "पगड़ी", "turban", "साफ", "टोपी", "cap");
            List<Integer> tcBefore = colorsIn(ts.substring(0, Math.max(0, pg)));
            l.headColor = !tcBefore.isEmpty() ? tcBefore.get(tcBefore.size() - 1) : (!tc.isEmpty() ? tc.get(0) : 0xFFC62828);
            List<Integer> after = colorsIn(ts.substring(Math.max(0, pg)));
            l.headBand = Txt.has(ts, "पट्टी", "band", "stripe") && !after.isEmpty() ? after.get(after.size() - 1) : 0;
            l.kalgi = Txt.has(headS, "कलगी", "plume", "रत्नजड़ित");
        } else if (Txt.has(headS, "मुकुट", "crown")) l.headwear = Look.HW_CROWN;
        else if (Txt.has(headS, "सींग", "horns")) l.headwear = Look.HW_HORNS;
        else if (Txt.has(all, "hoodie cap", "hood ", "हुड", "hooded", "hood,", "hood.", "hood up")) l.headwear = Look.HW_HOOD;
        else if (Txt.has(headS, "टोपी", "hat") && l.kind == Look.WITCH) l.headwear = Look.HW_WITCH_HAT;
        else if (Txt.has(headS, "पल्लू", "घूँघट", "दुपट्टा")) l.headwear = Look.HW_PALLU;
        else if (base == null && l.kind == Look.WITCH) l.headwear = Look.HW_WITCH_HAT;
        if (l.kind == Look.MONSTER && Txt.has(all, "सींग", "horn")) l.headwear = Look.HW_HORNS;

        // ---- props & jewellery
        if (Txt.has(all, "तलवार", "sword")) l.sword = true;
        if (Txt.has(all, "कटार", "dagger")) l.katar = true;
        if (Txt.has(all, "भाला", "भाले", "spear")) l.spear = true;
        if (Txt.has(all, "ढाल", "shield")) l.shield = true;
        if (Txt.has(all, "छड़ी", "wand", "staff") && l.aid != Look.AID_STICK) l.wand = true;
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
