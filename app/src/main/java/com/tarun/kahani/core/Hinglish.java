package com.tarun.kahani.core;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * Hinglish = Hindi written in English letters ("Vanusha (hanste hue): Arre ruk jao!").
 * The studio turns it into Devanagari so the Hindi voices pronounce it properly and the director understands
 * feelings and actions; names, the title and subtitles are shown back in the user's own spelling.
 * English words inside Hinglish ("butterfly", "sorry") are kept in English: the Hindi voices read them in English.
 */
public final class Hinglish {
    private Hinglish() {}

    /** Common Hinglish words with their usual Devanagari spelling (rules alone get these wrong). */
    static final String[] DICT = {
            "hai", "है", "hain", "हैं", "hei", "है", "nahi", "नहीं", "nahin", "नहीं", "nai", "नहीं", "kya", "क्या", "kyun", "क्यों", "kyon", "क्यों",
            "kyu", "क्यों", "main", "मैं", "mai", "मैं", "mein", "में", "me", "में", "is", "इस", "us", "उस", "isko", "इसको", "usko", "उसको", "iske", "इसके", "uske", "उसके", "ka", "का", "ki", "की", "ke", "के", "ko", "को", "se", "से",
            "tum", "तुम", "tu", "तू", "aap", "आप", "ap", "आप", "hum", "हम", "ham", "हम", "mera", "मेरा", "meri", "मेरी", "mere", "मेरे",
            "tera", "तेरा", "teri", "तेरी", "tere", "तेरे", "tumhara", "तुम्हारा", "tumhari", "तुम्हारी", "tumhare", "तुम्हारे",
            "hamara", "हमारा", "hamari", "हमारी", "hamare", "हमारे", "humara", "हमारा", "humari", "हमारी", "humare", "हमारे",
            "uska", "उसका", "uski", "उसकी", "uske", "उसके", "unka", "उनका", "unki", "उनकी", "unke", "उनके", "apna", "अपना", "apni", "अपनी",
            "apne", "अपने", "yeh", "यह", "ye", "ये", "yah", "यह", "woh", "वह", "wo", "वो", "vo", "वो", "voh", "वह", "aur", "और", "or", "और",
            "bhi", "भी", "toh", "तो", "to", "तो", "ho", "हो", "tha", "था", "thi", "थी", "the", "थे", "raha", "रहा", "rahi", "रही", "rahe", "रहे",
            "karo", "करो", "kar", "कर", "karna", "करना", "karte", "करते", "karti", "करती", "karta", "करता", "kiya", "किया", "kiye", "किए",
            "chalo", "चलो", "chal", "चल", "jao", "जाओ", "ja", "जा", "jaa", "जा", "aao", "आओ", "aa", "आ", "aaja", "आजा", "dekho", "देखो",
            "dekh", "देख", "acha", "अच्छा", "accha", "अच्छा", "achha", "अच्छा", "achchha", "अच्छा", "achi", "अच्छी", "acchi", "अच्छी",
            "theek", "ठीक", "thik", "ठीक", "bahut", "बहुत", "bohot", "बहुत", "bahot", "बहुत", "kitna", "कितना", "kitni", "कितनी",
            "kitne", "कितने", "kaise", "कैसे", "kaisa", "कैसा", "kaisi", "कैसी", "kahan", "कहाँ", "kaha", "कहा", "abhi", "अभी", "phir", "फिर",
            "fir", "फिर", "lekin", "लेकिन", "par", "पर", "pe", "पे", "ek", "एक", "do", "दो", "teen", "तीन", "char", "चार", "chaar", "चार",
            "paanch", "पाँच", "beta", "बेटा", "beti", "बेटी", "didi", "दीदी", "bhaiya", "भैया", "bhai", "भाई", "maa", "माँ", "ma", "माँ",
            "papa", "पापा", "raja", "राजा", "rani", "रानी", "ji", "जी", "haan", "हाँ", "han", "हाँ", "ha", "हा", "mat", "मत", "sab", "सब",
            "kuch", "कुछ", "kuchh", "कुछ", "koi", "कोई", "wala", "वाला", "wali", "वाली", "wale", "वाले", "gaya", "गया", "gayi", "गई",
            "gaye", "गए", "aaya", "आया", "aayi", "आई", "aaye", "आए", "diya", "दिया", "liya", "लिया", "lo", "लो", "do", "दो", "dena", "देना",
            "lena", "लेना", "hua", "हुआ", "hui", "हुई", "hue", "हुए", "hote", "होते", "hota", "होता", "hoti", "होती", "hoga", "होगा",
            "hogi", "होगी", "honge", "होंगे", "sakta", "सकता", "sakti", "सकती", "sakte", "सकते", "chahiye", "चाहिए", "chahie", "चाहिए",
            "pyaar", "प्यार", "pyar", "प्यार", "dost", "दोस्त", "dosti", "दोस्ती", "ghar", "घर", "paani", "पानी", "pani", "पानी",
            "khana", "खाना", "khel", "खेल", "khelna", "खेलना", "khelo", "खेलो", "sundar", "सुंदर", "suno", "सुनो", "sun", "सुन",
            "bolo", "बोलो", "bol", "बोल", "jaldi", "जल्दी", "dheere", "धीरे", "dhire", "धीरे", "zor", "ज़ोर", "jor", "ज़ोर", "dar", "डर",
            "darr", "डर", "daro", "डरो", "darna", "डरना", "arre", "अरे", "are", "अरे", "arey", "अरे", "oh", "ओह", "wah", "वाह", "waah", "वाह",
            "hanste", "हँसते", "haste", "हँसते", "hansti", "हँसती", "hansta", "हँसता", "hue", "हुए", "huye", "हुए", "rote", "रोते",
            "roti", "रोती", "gusse", "गुस्से", "gussa", "गुस्सा", "khush", "खुश", "khushi", "खुशी", "dukhi", "दुखी", "udaas", "उदास",
            "udas", "उदास", "darte", "डरते", "darti", "डरती", "chillate", "चिल्लाते", "chillakar", "चिल्लाकर", "dheere se", "धीरे से",
            "muskurate", "मुस्कुराते", "muskurakar", "मुस्कुराकर", "phusphusate", "फुसफुसाते", "fusfusate", "फुसफुसाते",
            "hairani", "हैरानी", "hairan", "हैरान", "gussa", "गुस्सा", "pyaar se", "प्यार से", "saal", "साल", "sal", "साल", "varsh", "वर्ष",
            "baras", "बरस", "umar", "उम्र", "umr", "उम्र", "patra", "पात्र", "kirdar", "किरदार", "jagah", "जगह", "jagahen", "जगहें",
            "sthan", "स्थान", "drishya", "दृश्य", "dRishya", "दृश्य", "kathavachak", "कथावाचक", "sutradhar", "सूत्रधार",
            "rajkumari", "राजकुमारी", "rajkumar", "राजकुमार", "mahal", "महल", "bagicha", "बगीचा", "bageecha", "बगीचा", "baag", "बाग",
            "jungle", "जंगल", "jangal", "जंगल", "gufa", "गुफा", "nadi", "नदी", "pahad", "पहाड़", "pahaad", "पहाड़", "gaon", "गाँव",
            "gaanv", "गाँव", "subah", "सुबह", "sham", "शाम", "shaam", "शाम", "raat", "रात", "rat", "रात", "din", "दिन", "dopahar", "दोपहर",
            "suraj", "सूरज", "chand", "चाँद", "chaand", "चाँद", "taare", "तारे", "phool", "फूल", "ped", "पेड़", "pedon", "पेड़ों",
            "titli", "तितली", "chidiya", "चिड़िया", "bandar", "बंदर", "sher", "शेर", "haathi", "हाथी", "hathi", "हाथी", "lomdi", "लोमड़ी",
            "kauwa", "कौआ", "kaua", "कौआ", "mor", "मोर", "tota", "तोता", "khargosh", "खरगोश", "kachua", "कछुआ", "kutta", "कुत्ता",
            "billi", "बिल्ली", "rakshas", "राक्षस", "rakshasa", "राक्षस", "chudail", "चुड़ैल", "jaadu", "जादू", "jadu", "जादू",
            "jaadui", "जादुई", "jadui", "जादुई", "talwar", "तलवार", "sipahi", "सिपाही", "darwaza", "दरवाज़ा", "darwaja", "दरवाज़ा",
            "lehenga", "लहंगा", "lehnga", "लहंगा", "choli", "चोली", "kurta", "कुर्ता", "saree", "साड़ी", "sari", "साड़ी", "pagdi", "पगड़ी",
            "lal", "लाल", "laal", "लाल", "hara", "हरा", "hari", "हरी", "neela", "नीला", "neeli", "नीली", "peela", "पीला", "peeli", "पीली",
            "gulabi", "गुलाबी", "kala", "काला", "kaala", "काला", "kaali", "काली", "safed", "सफ़ेद", "sunehra", "सुनहरा", "sunahra", "सुनहरा",
            "baal", "बाल", "aankh", "आँख", "aankhen", "आँखें", "chehra", "चेहरा", "haath", "हाथ", "pair", "पैर", "sir", "सिर",
            "ladki", "लड़की", "ladka", "लड़का", "aurat", "औरत", "aadmi", "आदमी", "buddha", "बूढ़ा", "budha", "बूढ़ा", "bachcha", "बच्चा",
            "bacche", "बच्चे", "bachche", "बच्चे", "log", "लोग", "naam", "नाम", "nam", "नाम", "kahani", "कहानी", "samaapt", "समाप्त",
            "samapt", "समाप्त", "shukriya", "शुक्रिया", "dhanyavaad", "धन्यवाद", "namaste", "नमस्ते", "namaskar", "नमस्कार",
            "kab", "कब", "kaun", "कौन", "kis", "किस", "kisi", "किसी", "jab", "जब", "tab", "तब", "agar", "अगर", "magar", "मगर",
            "kyunki", "क्योंकि", "isliye", "इसलिए", "sirf", "सिर्फ़", "bas", "बस", "zaroor", "ज़रूर", "jarur", "ज़रूर", "sach", "सच",
            "sachmuch", "सचमुच", "pata", "पता", "maloom", "मालूम", "yahan", "यहाँ", "yaha", "यहाँ", "wahan", "वहाँ", "waha", "वहाँ",
            "andar", "अंदर", "bahar", "बाहर", "upar", "ऊपर", "neeche", "नीचे", "niche", "नीचे", "paas", "पास", "pas", "पास", "door", "दूर",
            "dur", "दूर", "saath", "साथ", "sath", "साथ", "liye", "लिए", "lie", "लिए", "wajah", "वजह", "bina", "बिना", "naya", "नया",
            "nayi", "नई", "naye", "नए", "purana", "पुराना", "bada", "बड़ा", "badi", "बड़ी", "bade", "बड़े", "chota", "छोटा", "chhota", "छोटा",
            "choti", "छोटी", "chhoti", "छोटी", "chote", "छोटे", "achanak", "अचानक", "dhyan", "ध्यान", "himmat", "हिम्मत", "madad", "मदद",
            "bachao", "बचाओ", "bachaao", "बचाओ", "ruko", "रुको", "ruk", "रुक", "bhaago", "भागो", "bhago", "भागो", "pakdo", "पकड़ो",
            "chhodo", "छोड़ो", "chodo", "छोड़ो", "lao", "लाओ", "le", "ले", "de", "दे", "hamesha", "हमेशा", "kabhi", "कभी", "aaj", "आज",
            "kal", "कल", "abhi", "अभी", "yaar", "यार", "mujhe", "मुझे", "mujhse", "मुझसे", "tumhe", "तुम्हें", "tumhen", "तुम्हें",
            "tujhe", "तुझे", "use", "उसे", "unhe", "उन्हें", "hume", "हमें", "humein", "हमें", "aapko", "आपको", "sabko", "सबको",
            "khelna", "खेलना", "chahti", "चाहती", "chahta", "चाहता", "chahte", "चाहते", "lagta", "लगता", "lagti", "लगती", "lag", "लग",
            "rehna", "रहना", "awaaz", "आवाज़", "awaz", "आवाज़", "aawaz", "आवाज़", "aawaaz", "आवाज़", "avaaz", "आवाज़", "tez", "तेज़",
            "dheema", "धीमा", "dheemi", "धीमी", "dhima", "धीमा", "dhimi", "धीमी", "badhao", "बढ़ाओ", "badhaao", "बढ़ाओ", "badha", "बढ़ा",
            "badhaa", "बढ़ा", "ghatao", "घटाओ", "ghata", "घटा", "chamak", "चमक", "roshni", "रोशनी", "gaana", "गाना", "gana", "गाना",
            "sangeet", "संगीत", "mota", "मोटा", "moti", "मोटी", "motee", "मोटी", "patla", "पतला", "patli", "पतली", "band", "बंद",
            "lagao", "लगाओ", "lagaao", "लगाओ", "hatao", "हटाओ", "hata", "हटा", "hatado", "हटा दो", "karo", "करो", "kardo", "कर दो",
            "thoda", "थोड़ा", "thodi", "थोड़ी", "zara", "ज़रा", "jara", "ज़रा", "rang", "रंग", "garam", "गर्म", "garm", "गर्म",
            "thanda", "ठंडा", "awaazein", "आवाज़ें", "aawazein", "आवाज़ें", "dhwani", "ध्वनि", "kehna", "कहना", "peeche", "पीछे", "piche", "पीछे", "peechhe", "पीछे", "aage", "आगे", "age", "आगे",
            "pehle", "पहले", "pakad", "पकड़", "pakdo", "पकड़ो", "shaitani", "शैतानी", "shaitan", "शैतान", "shaitaan", "शैतान", "pahle", "पहले", "baad", "बाद", "wapas", "वापस", "vapas", "वापस", "bagiche", "बगीचे", "samay", "समय",
            "hokar", "होकर", "kar ke", "करके", "karke", "करके", "dikhao", "दिखाओ", "batao", "बताओ", "sabse", "सबसे", "zyada", "ज़्यादा",
            "jyada", "ज़्यादा", "kam", "कम", "aam", "आम", "kela", "केला", "seb", "सेब", "chahchaha", "चहचहा", "bhaag", "भाग", "rakho", "रखो", "socho", "सोचो", "samjho", "समझो", "samajh", "समझ", "seekh", "सीख", "sabak", "सबक",
    };

    /** English words to keep in English inside Hinglish (Hindi voices read them as English). */
    static final String[] ENGLISH = ("butterfly flower garden school teacher friend friends sorry please thank thanks you okay ok hello hi bye "
            + "good morning night mummy daddy mom dad uncle aunty auntie cake chocolate ball game games party birthday gift happy "
            + "phone mobile tv car bus train cycle bike doctor police king queen princess prince magic mirror sword cave forest "
            + "superhero hero team class homework exam test sir madam miss baby cute wow cool super great nice love lovely "
            + "beautiful bad very really what why how when where yes no not just also so but because then now today tomorrow "
            + "yesterday always never maybe sure right wrong help stop wait come go look see run fast slow big small little "
            + "new old one two three four five red blue green yellow pink white black brown orange purple guard guards palace "
            + "jungle river mountain sky sun moon star stars rain water fire ice snow tree trees bird birds dog cat lion tiger "
            + "monkey elephant rabbit fox bear horse cow goat mouse ice-cream icecream biscuit sandwich pizza burger juice milk "
            + "ribbon frock dress shirt shoes cap bag bottle box toy toys doll robot computer video photo camera picnic zoo park "
            + "market shop hospital station airport plane ship boat rocket space planet dinosaur "
            + "is am are was were be been it this that these those my your his her our their me him us them we they he she "
            + "and or of in on at for with to from by the a an").split(" ");

    /** Words kept as they are because the script reader looks for them. */
    static final String[] STRUCTURE = ("scene characters character cast places place locations location settings setting title "
            + "narrator voiceover").split(" ");

    static final Map<String, String> dict = new HashMap<String, String>();
    static final Set<String> english = new HashSet<String>(), structure = new HashSet<String>(), hindiWords = new HashSet<String>();
    static {
        for (int i = 0; i + 1 < DICT.length; i += 2) {
            if (!dict.containsKey(DICT[i].toLowerCase())) dict.put(DICT[i].toLowerCase(), DICT[i + 1]);
            hindiWords.add(DICT[i].toLowerCase());
        }
        for (String w : ENGLISH) english.add(w);
        for (String w : STRUCTURE) structure.add(w);
    }

    /** True for text written mostly in English letters but in the Hindi language. */
    public static boolean isHinglish(String text) {
        int latin = 0, dev = 0, words = 0, hindi = 0;
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if (Txt.isDevanagari(c)) dev++;
            else if ((c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z')) latin++;
        }
        if (latin < 20 || dev > latin / 4) return false;
        for (String w : text.toLowerCase().split("[^a-z]+")) {
            if (w.length() == 0) continue;
            words++;
            if (hindiWords.contains(w) && !english.contains(w) && !AMBIGUOUS.contains(w)) hindi++;
        }
        return words > 0 && hindi * 100 >= words * 12;
    }

    // ------------------------------------------------------------------ transliteration

    static final String[][] CONS = {
            {"ksh", "क्ष"}, {"chh", "छ"}, {"tth", "त्थ"}, {"ddh", "द्ध"}, {"ch", "च"}, {"kh", "ख"}, {"gh", "घ"}, {"jh", "झ"}, {"th", "थ"},
            {"dh", "ध"}, {"ph", "फ"}, {"bh", "भ"}, {"sh", "श"}, {"gy", "ज्ञ"}, {"rh", "ढ़"},
            {"k", "क"}, {"g", "ग"}, {"c", "क"}, {"j", "ज"}, {"t", "त"}, {"d", "द"}, {"n", "न"}, {"p", "प"}, {"f", "फ़"}, {"b", "ब"},
            {"m", "म"}, {"y", "य"}, {"r", "र"}, {"l", "ल"}, {"v", "व"}, {"w", "व"}, {"s", "स"}, {"h", "ह"}, {"z", "ज़"}, {"q", "क"},
            {"x", "क्स"}};
    // vowel: {latin, independent, matra}
    static final String[][] VOW = {
            {"aa", "आ", "ा"}, {"ao", "आओ", "ाओ"}, {"ae", "आए", "ाए"}, {"aye", "आए", "ाए"}, {"ui", "उई", "ुई"}, {"ai", "ऐ", "ै"}, {"au", "औ", "ौ"}, {"ee", "ई", "ी"}, {"ii", "ई", "ी"}, {"oo", "ऊ", "ू"}, {"uu", "ऊ", "ू"},
            {"ei", "ए", "े"}, {"a", "अ", ""}, {"i", "इ", "ि"}, {"u", "उ", "ु"}, {"e", "ए", "े"}, {"o", "ओ", "ो"}};

    /** Rule-based romanised Hindi -> Devanagari for one word (lower case letters only). */
    public static String word(String w) {
        StringBuilder o = new StringBuilder();
        int i = 0, n = w.length();
        boolean afterCons = false;
        int syll = 0;                       // vowels written so far
        while (i < n) {
            String[] v = match(VOW, w, i);
            if (v != null) {
                int end = i + v[0].length();
                boolean last = end == n;
                String vv = v[0];
                if (afterCons) {
                    if (vv.equals("a") && last && n > 2) o.append("ा");                      // kya, accha
                    else if (vv.equals("a") && syll == 0 && longA(w, end)) o.append("ा");     // raju, dadi, wapas
                    else if (vv.equals("e") && end + 1 < n && w.charAt(end) == 'h' && match(VOW, w, end + 1) == null) { /* pehle -> पहले */ }
                    else if (vv.equals("i") && last && n > 2) o.append("ी");                 // rani, nahi
                    else if (vv.equals("u") && last && n > 2) o.append("ू");                 // raju, ramu
                    else o.append(v[2]);
                } else {
                    if (vv.equals("i") && last) o.append("ई");
                    else o.append(v[1]);
                }
                afterCons = false;
                syll++;
                i = end;
                continue;
            }
            String[] c = match(CONS, w, i);
            if (c == null) { i++; continue; }
            int end = i + c[0].length();
            // "n" before another consonant -> anusvara (sundar -> सुंदर)
            if (c[0].equals("n") && end < n && match(VOW, w, end) == null && !afterCons && o.length() > 0) {
                o.append("ं");
                i = end;
                continue;
            }
            if (afterCons) o.append("्");          // consonant cluster
            o.append(c[1]);
            afterCons = true;
            i = end;
        }
        return o.toString();
    }

    /** First-syllable "a" before one consonant and a non-"a" vowel is usually long in names: raju, dadi, rani. */
    static boolean longA(String w, int at) {
        String[] c = match(CONS, w, at);
        if (c == null) return false;
        int next = at + c[0].length();
        if (next >= w.length()) return false;
        String[] v = match(VOW, w, next);
        if (v == null) return false;
        return !v[0].startsWith("a") && !(v[0].equals("e") && next + 1 < w.length() && w.charAt(next + 1) == 'h');
    }

    static String[] match(String[][] table, String w, int i) {
        for (String[] e : table) if (w.startsWith(e[0], i)) return e;
        return null;
    }

    /** Converts a whole Hinglish text; back receives Devanagari word -> original spelling (for names and subtitles). */
    public static String toDevanagari(String text, Map<String, String> back) {
        StringBuilder o = new StringBuilder();
        int i = 0, n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (!isLatin(c)) { o.append(c); i++; continue; }
            int j = i;
            while (j < n && (isLatin(text.charAt(j)) || (text.charAt(j) == '\'' && j + 1 < n && isLatin(text.charAt(j + 1))))) j++;
            String orig = text.substring(i, j);
            String low = orig.toLowerCase();
            String out;
            if (structure.contains(low)) out = orig;
            else if (dict.containsKey(low)) out = dict.get(low);
            else if (english.contains(low)) out = orig;
            else out = word(low.replace("'", ""));
            if (back != null && !out.equals(orig) && !back.containsKey(out)) back.put(out, orig);
            o.append(out);
            i = j;
        }
        return o.toString();
    }

    /** Real English words that are also Hinglish words; they don't count when deciding if a text is Hinglish. */
    static final Set<String> AMBIGUOUS = new HashSet<String>(java.util.Arrays.asList("do", "main", "age", "use", "band", "par", "log", "door",
            "pair", "sir", "bas", "pas", "sab", "lag", "le", "de", "kal", "mat", "bhai", "hue", "ha", "han", "lo", "tab", "jab", "jungle",
            "sun", "chal", "mor", "dar", "sher", "seb", "aam", "din", "rat", "chand", "ped"));

    static final Set<String> COMMAND_SKIP = new HashSet<String>(java.util.Arrays.asList("do", "main", "age", "use", "par", "pe", "band", "ho", "ye", "kal"));

    /** Only the well-known Hinglish words are turned into Devanagari (for short commands like "music kam karo"). */
    public static String knownWords(String text) {
        StringBuilder o = new StringBuilder();
        int i = 0, n = text.length();
        while (i < n) {
            char c = text.charAt(i);
            if (!isLatin(c)) { o.append(c); i++; continue; }
            int j = i;
            while (j < n && isLatin(text.charAt(j))) j++;
            String orig = text.substring(i, j), low = orig.toLowerCase();
            String d = dict.get(low);
            o.append(d != null && !english.contains(low) && !COMMAND_SKIP.contains(low) ? d : orig);
            i = j;
        }
        return o.toString();
    }

    static boolean isLatin(char c) { return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z'); }

    /** Devanagari text made from Hinglish -> the user's own spelling again (word by word). */
    public static String back(String dev, Map<String, String> back) {
        if (back == null || back.isEmpty()) return dev;
        StringBuilder o = new StringBuilder();
        int i = 0, n = dev.length();
        while (i < n) {
            char c = dev.charAt(i);
            if (!Txt.isDevanagari(c)) { o.append(c); i++; continue; }
            int j = i;
            while (j < n && Txt.isDevanagari(dev.charAt(j))) j++;
            String w = dev.substring(i, j);
            String b = back.get(w);
            o.append(b != null ? b : w);
            i = j;
        }
        return o.toString();
    }
}
