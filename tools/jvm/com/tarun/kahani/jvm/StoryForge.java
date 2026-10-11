package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.*;

/**
 * v44 mass story generator and checker. Builds whole stories (a cast of 2–5, 1–4 scenes) out of the one-sentence rows
 * of PhenomenaProbe's TSV files, directs them with dolls (an empty Art) and checks every film: no crash, SoakCheck's
 * invariants, and every stage direction staged as its row expects, judged by PhenomenaProbe's own evaluator in the
 * window between the dialogue lines around it.
 *
 * <pre>
 * StoryForge &lt;pool.tsv[,pool2.tsv…]&gt; run &lt;fromSeed&gt; &lt;count&gt;      generate, direct, check; prints a compact report
 * StoryForge &lt;pools&gt; write &lt;fromSeed&gt; &lt;count&gt; &lt;dir&gt;              writes story_00001.txt … in the user's script format
 * StoryForge &lt;pools&gt; one &lt;seed&gt;                                  prints the script and each direction's verdict
 * Env: OUT=path  the full report (every template, every invariant example) also goes to this file
 *      THREADS=n  worker threads for run (default 3; the core keeps no shared mutable state on the directing path)
 *      TOP=n      rows of the failure table printed (default 25)
 *      SKIP_CATS=a,b  leave those row categories out of the pool (e.g. SKIP_CATS=drinks for family-only stories)
 *      DUMP=1|all (one mode) PhenomenaProbe's key/act dump for the failing (or all) directions
 * </pre>
 * A pool file that does not exist is skipped with a note. Everything is deterministic per seed (java.util.Random).
 *
 * Verdicts per direction: PASS (an expectation group holds in the window), CONTEXT (it fails only because the state
 * it asks for held already just before the window: already sitting, already riding, already awake, the rain already
 * gone), FAIL, UNMATCHED (the harness could not find the window or the character in the film), BADCODE (the row's
 * codes name constants the core does not have), EVALERR (the evaluator threw).
 *
 * Note: unlike PhenomenaProbe there is no neutral film to subtract, so a key or act in the window that the story
 * around it caused (a reaction to the line before) counts for the direction; passes are a little lenient.
 */
public class StoryForge {

    // ================================================================== pool rows

    static final class Row {
        String lang, sentence, expect, file, cat = "misc", place = "out", start = "stand", tod = "day", age = "kid";
        boolean fam, girl, hasBoy, hasGirl, mom, dad;
        int line, idx;
        String bad;      // why the codes are not valid (null: valid)
        String key() { return lang + "|" + place + "/" + start + "/" + tod + "/" + age + (fam ? "/fam" : ""); }
        String id() { return Paths.get(file).getParent().getFileName() + ":" + line; }
    }

    static final List<Row> ROWS = new ArrayList<Row>();
    static final Map<String, List<Row>> GROUPS = new LinkedHashMap<String, List<Row>>();
    static final Map<String, List<Row>> BY_LANG = new HashMap<String, List<Row>>();
    static final List<String> LOAD_NOTES = new ArrayList<String>();
    /** SKIP_CATS=drinks,hunt leaves those categories out of the pool (e.g. a family-only run). */
    static final Set<String> SKIP_CATS = new HashSet<String>(Arrays.asList(System.getenv("SKIP_CATS") == null ? new String[0] : System.getenv("SKIP_CATS").split(",")));

    static void load(String files) throws Exception {
        for (String fn : files.split(",")) {
            fn = fn.trim();
            if (fn.isEmpty()) continue;
            Path p = Paths.get(fn);
            if (!Files.exists(p)) { LOAD_NOTES.add("pool " + fn + " missing: skipped"); continue; }
            Row ctx = new Row();
            int no = 0, n = 0;
            for (String line : Files.readAllLines(p, StandardCharsets.UTF_8)) {
                no++;
                String t = line.trim();
                if (t.startsWith("#@")) {
                    ctx = new Row();
                    for (String kv : t.substring(2).trim().split("\\s+")) {
                        int e = kv.indexOf('=');
                        if (e < 0) continue;
                        String k = kv.substring(0, e), v = kv.substring(e + 1);
                        switch (k) {
                            case "cat": ctx.cat = v; break;
                            case "place": ctx.place = v; break;
                            case "start": ctx.start = v; break;
                            case "tod": ctx.tod = v; break;
                            case "age": ctx.age = v; break;
                            case "fam": ctx.fam = v.equals("1"); break;
                            default: LOAD_NOTES.add(fn + ":" + no + " unknown context key " + k);
                        }
                    }
                    continue;
                }
                if (t.isEmpty() || t.startsWith("#")) continue;
                String[] f = line.split("\t");
                if (f.length < 3) { LOAD_NOTES.add(fn + ":" + no + " not lang<TAB>sentence<TAB>expect"); continue; }
                Row r = new Row();
                r.lang = f[0].trim(); r.sentence = f[1].trim(); r.expect = f[2].trim(); r.file = fn; r.line = no;
                r.cat = ctx.cat; r.place = ctx.place; r.start = ctx.start; r.tod = ctx.tod; r.age = ctx.age; r.fam = ctx.fam;
                if (!r.lang.equals("hi") && !r.lang.equals("en")) { LOAD_NOTES.add(fn + ":" + no + " lang " + r.lang); continue; }
                boolean en = r.lang.equals("en");
                r.hasBoy = r.sentence.contains(en ? "Rahul" : "राहुल");
                r.hasGirl = r.sentence.contains(en ? "Priya" : "प्रिया");
                r.girl = r.hasGirl && !r.hasBoy;
                if (!r.hasBoy && !r.hasGirl) r.hasBoy = true;   // a sentence about the weather: the boy is the one on the stage
                String s = r.sentence;
                r.mom = s.contains("माँ") || s.contains("मम्मी") || s.contains("Mother") || s.contains("mother") || s.contains("Mom") || s.contains("mom");
                r.dad = r.fam || s.contains("पापा") || s.contains("पिता") || s.contains("Father") || s.contains("father") || s.contains("Dad") || s.contains("dad");
                if (SKIP_CATS.contains(r.cat)) continue;
                try { PhenomenaProbe.validate(r.expect); } catch (Throwable e) { r.bad = e.getMessage(); }
                r.idx = ROWS.size();
                ROWS.add(r);
                GROUPS.computeIfAbsent(r.key(), k -> new ArrayList<Row>()).add(r);
                BY_LANG.computeIfAbsent(r.lang, k -> new ArrayList<Row>()).add(r);
                n++;
            }
            LOAD_NOTES.add("pool " + fn + ": " + n + " rows");
        }
        int bad = 0;
        for (Row r : ROWS) if (r.bad != null) bad++;
        if (bad > 0) LOAD_NOTES.add(bad + " rows name codes the core does not have (verdict BADCODE), e.g. " + firstBad());
        if (ROWS.isEmpty()) throw new IllegalArgumentException("no rows in " + files);
    }

    static String firstBad() { for (Row r : ROWS) if (r.bad != null) return r.id() + " " + r.bad; return ""; }

    // ================================================================== people and places

    static final class Person {
        String name, role, look;
        boolean male, kid;
        int age;
        String slot;     // boy, girl, mom, dad, extra
    }

    static final String[] HI_BOY = {"आरव", "विहान", "अयान", "ईशान", "रोहन", "आदित्य", "निखिल", "तनय", "ऋषभ", "कुणाल"};
    static final String[] HI_GIRL = {"अनाया", "सिया", "मीरा", "पीहू", "ईशा", "रिया", "काव्या", "ज़ोया", "अदिति", "नायरा"};
    static final String[] HI_MAN = {"विक्रम", "अजय", "करण", "नितिन", "सुरेश", "मनोज", "राजेश", "अनिल", "संजय", "विनोद"};
    static final String[] HI_WOMAN = {"सुनीता", "कविता", "अंजलि", "नीता", "अनीता", "शालिनी", "मीनाक्षी", "पल्लवी", "सोनाली", "हेमा"};
    static final String[] HI_GRANNY = {"शारदा", "सावित्री", "विमला"};
    static final String[] EN_BOY = {"Aarav", "Vihaan", "Ayaan", "Ishaan", "Rohan", "Aditya", "Nikhil", "Tanay", "Rishabh", "Kunal"};
    static final String[] EN_GIRL = {"Ananya", "Siya", "Meera", "Pihu", "Isha", "Riya", "Kavya", "Zoya", "Aditi", "Naira"};
    static final String[] EN_MAN = {"Vikram", "Ajay", "Karan", "Nitin", "Suresh", "Manoj", "Rajesh", "Anil", "Sanjay", "Vinod"};
    static final String[] EN_WOMAN = {"Sunita", "Kavita", "Anjali", "Neeta", "Anita", "Shalini", "Meenakshi", "Pallavi", "Sonali", "Hema"};
    static final String[] EN_GRANNY = {"Sharda", "Savitri", "Vimla"};

    static final String[] HI_LOOK_BOY = {"नीली टी-शर्ट, खाकी निकर", "लाल टी-शर्ट, जींस", "चश्मा, हरी शर्ट, काली पैंट", "पीली टी-शर्ट, नीले शॉर्ट्स", "धारीदार टी-शर्ट, भूरी पैंट"};
    static final String[] HI_LOOK_GIRL = {"पीली फ्रॉक, दो चोटियाँ", "गुलाबी टी-शर्ट, पोनीटेल", "चश्मा, नीली ड्रेस", "हरा सलवार-कुर्ता, छोटे बाल", "लाल फ्रॉक, बालों में क्लिप"};
    static final String[] HI_LOOK_MAN = {"सफ़ेद कुर्ता-पजामा", "नीली शर्ट, काली पैंट", "चश्मा, भूरा स्वेटर", "हल्की दाढ़ी, हरी टी-शर्ट", "मूँछें, चेक वाली शर्ट"};
    static final String[] HI_LOOK_WOMAN = {"हरी साड़ी", "लाल सलवार-कमीज़", "पीला कुर्ता, जींस", "नीली साड़ी, जूड़ा", "गुलाबी कुर्ता, चोटी"};
    static final String[] HI_LOOK_GRANNY = {"सफ़ेद साड़ी, चश्मा", "हल्की नीली साड़ी, सफ़ेद बाल"};
    static final String[] HI_LOOK_ROYAL_M = {"सुनहरा मुकुट, लाल शाही पोशाक", "पगड़ी, नीला अंगरखा, कमरबंद"};
    static final String[] HI_LOOK_GENERAL = {"लोहे का कवच, लाल पगड़ी", "कवच, कमर में तलवार, घनी मूँछें"};
    static final String[] EN_LOOK_GENERAL = {"iron armour, red turban", "armour, a sword at the waist, thick moustache"};
    static final String[] HI_LOOK_ROYAL_F = {"छोटा मुकुट, गुलाबी लहँगा", "सुनहरे गहने, हरी शाही पोशाक"};
    static final String[] EN_LOOK_BOY = {"blue T-shirt, khaki shorts", "red T-shirt, jeans", "glasses, green shirt, black trousers", "yellow T-shirt, blue shorts", "striped T-shirt, brown trousers"};
    static final String[] EN_LOOK_GIRL = {"yellow frock, two plaits", "pink T-shirt, ponytail", "glasses, blue dress", "green salwar-kurta, short hair", "red frock, a clip in her hair"};
    static final String[] EN_LOOK_MAN = {"white kurta-pyjama", "blue shirt, black trousers", "glasses, brown sweater", "light beard, green T-shirt", "moustache, checked shirt"};
    static final String[] EN_LOOK_WOMAN = {"green saree", "red salwar-kameez", "yellow kurta, jeans", "blue saree, hair in a bun", "pink kurta, a long plait"};
    static final String[] EN_LOOK_GRANNY = {"white saree, glasses", "pale blue saree, white hair"};
    static final String[] EN_LOOK_ROYAL_M = {"golden crown, red royal robe", "turban, blue angarkha, a sash"};
    static final String[] EN_LOOK_ROYAL_F = {"small crown, pink lehenga", "golden jewellery, green royal dress"};

    /** Place key → Hindi heading, Hindi "where", Hindi description, English heading, English "where", English description. */
    static final String[][] PLACES = {
        {"out", "घर का आँगन", "आँगन में", "ईंटों का फ़र्श, तुलसी का पौधा", "The courtyard", "in the courtyard", "a brick floor, a tulsi plant"},
        {"room", "घर का कमरा", "कमरे में", "सोफ़ा, मेज़, बड़ी खिड़की", "The living room", "in the living room", "a sofa, a table, a big window"},
        {"bedroom", "घर का बेडरूम", "बेडरूम में", "बड़ा बिस्तर, लकड़ी की अलमारी", "The bedroom", "in the bedroom", "a big bed, a wooden cupboard"},
        {"kitchen", "घर की रसोई", "रसोई में", "गैस का चूल्हा, स्टील के बर्तन", "The kitchen", "in the kitchen", "a gas stove, steel pots"},
        {"bathroom", "घर का बाथरूम", "बाथरूम में", "नल, बाल्टी, आईना", "The bathroom", "in the bathroom", "a tap, a bucket, a mirror"},
        {"street", "घर के बाहर की सड़क", "सड़क के किनारे", "पक्की सड़क, किनारे पर पेड़", "The road", "by the road", "a paved road, trees along it"},
        {"park", "मोहल्ले का पार्क", "पार्क में", "हरी घास, झूले, बेंच", "The park", "in the park", "green grass, swings, benches"},
        {"terrace", "घर की छत", "छत पर", "पानी की टंकी, कपड़े सुखाने की रस्सी", "The terrace", "on the terrace", "a water tank, a clothes line"},
        {"garden", "घर का बगीचा", "बगीचे में", "फूलों की क्यारियाँ, आम का पेड़", "The garden", "in the garden", "flower beds, a mango tree"},
        {"shore", "मछुआरों का गाँव", "गाँव में", "नावें, जाल, नारियल के पेड़", "The fishermen's village", "in the village", "boats, nets, coconut palms"},
        {"palace", "राजमहल का दरबार", "दरबार में", "ऊँचे खंभे, सुनहरा सिंहासन", "The royal court", "in the court", "tall pillars, a golden throne"},
        {"arena", "महल का अखाड़ा", "अखाड़े में", "मिट्टी का मैदान, हथियारों का स्टैंड", "The training ground", "on the training ground", "an earth ground, a rack of weapons"},
        {"forest", "घना जंगल", "जंगल में", "ऊँचे पेड़, घनी झाड़ियाँ", "The forest", "in the forest", "tall trees, thick bushes"},
        {"snow", "बर्फ़ से ढकी पहाड़ी", "बर्फ़ में", "सफ़ेद बर्फ़, देवदार के पेड़", "The snowy hillside", "in the snow", "white snow, deodar trees"},
        {"road", "शहर की बड़ी सड़क", "सड़क पर", "चौड़ी सड़क, ट्रैफ़िक लाइट", "The main road", "on the road", "a wide road, traffic lights"},
    };

    static String[] place(String key) {
        for (String[] p : PLACES) if (p[0].equals(key)) return p;
        // a place this table does not know: PhenomenaProbe's (it may have grown), else the word itself
        try {
            for (String[] p : PhenomenaProbe.PLACES) if (p[0].equals(key))
                return new String[]{key, p[1], p[2], "", p[4], p[5], ""};
        } catch (Throwable e) { /* older probe */ }
        return new String[]{key, key, key + " में", "", key, "at the " + key, ""};
    }

    /** In a royal story the house is the palace ("घर की रसोई" → "महल की रसोई"). */
    static String home(String place, boolean royal) { return royal && place.startsWith("घर ") ? "महल " + place.substring(3) : place; }

    static String hiTod(String t) {
        switch (t) { case "night": return "रात"; case "evening": return "शाम"; case "morning": return "सुबह"; default: return "दिन"; }
    }

    static String enTod(String t) {
        switch (t) { case "night": return "Night"; case "evening": return "Evening"; case "morning": return "Morning"; default: return "Day"; }
    }

    // ================================================================== dialogue

    static final String[] HI_LINES = {"वाह, कितना अच्छा!", "ध्यान से!", "अरे, देखो तो!", "हाँ, ठीक है।", "मुझे भी दिखाओ!", "क्या बात है!", "आराम से, धीरे-धीरे।",
            "बहुत बढ़िया!", "यह तो कमाल है!", "अब आगे क्या करें?", "क्या हुआ?", "{n}, सुनो!", "^शाबाश, {n}!", "अच्छा, फिर?", "मज़ा आ गया!", "अरे वाह!",
            "हम्म, अच्छा।", "सच में?", "{n}, तुम ठीक हो?", "बिल्कुल सही।", "एक बार और!", "देखते हैं।", "कितना सुंदर है!", "हाँ, बिल्कुल।", "{n}, यह देखो।",
            "मैं अभी आया!|मैं अभी आई!", "मैं तैयार हूँ!", "मुझे पता था!", "यह तो आसान है।", "ठीक है, {n}।"};
    static final String[] EN_LINES = {"Wow, how lovely!", "Careful!", "Hey, look at that!", "Yes, all right.", "Show me too!", "That's great!", "Slowly, take it easy.",
            "^Well done!", "This is amazing!", "What next?", "What happened?", "{n}, listen!", "^Good job, {n}!", "And then?", "That was fun!", "Oh wow!",
            "Hmm, I see.", "Really?", "{n}, are you okay?", "Exactly right.", "One more time!", "Let's see.", "How beautiful!", "Yes, of course.", "{n}, look at this.",
            "I'm ready!", "I knew it!", "That's easy.", "Okay, {n}."};
    static final String[] HI_OPEN = {"आज का दिन बहुत अच्छा लग रहा है।", "चलो, आज कुछ मज़ेदार करते हैं।", "{n}, आज क्या इरादा है?", "कितना सुहाना मौसम है!"};
    static final String[] HI_OPEN_MORNING = {"सुप्रभात, {n}!", "सुबह हो गई, {n}!"};
    static final String[] HI_OPEN_NIGHT = {"रात कितनी शांत है।", "आज तो बहुत देर हो गई।"};
    static final String[] HI_SCENE = {"तो, अब यहाँ!", "यह जगह कितनी अच्छी है।", "{n}, यहाँ देखो।", "चलो, शुरू करें।"};
    static final String[] HI_CLOSE = {"आज का दिन यादगार रहा!", "कितना मज़ेदार दिन था!", "कल फिर ऐसा ही करेंगे!", "सच में, बहुत अच्छा लगा।"};
    static final String[] EN_OPEN = {"Today feels like a good day.", "Let's do something fun today.", "{n}, what's the plan today?", "What lovely weather!"};
    static final String[] EN_OPEN_MORNING = {"Good morning, {n}!", "It's morning already, {n}!"};
    static final String[] EN_OPEN_NIGHT = {"The night is so quiet.", "It's very late tonight."};
    static final String[] EN_SCENE = {"So, here we are!", "What a nice place.", "{n}, look over here.", "Let's begin."};
    static final String[] EN_CLOSE = {"What a day to remember!", "That was such a fun day!", "Let's do this again tomorrow!", "I really enjoyed today."};
    static final String[] HI_MANNER = {"खुश होकर", "हँसते हुए", "हैरान होकर", "प्यार से", "धीरे से", "उत्साह से"};
    static final String[] EN_MANNER = {"happily", "laughing", "surprised", "gently", "softly", "excited"};

    // ================================================================== one generated story

    static final class Dir {
        Row row;
        String text;
        Person main;
        int lineBefore, lineAfter, scene;
    }

    static final class Gen {
        long seed;
        boolean en;
        String script;
        List<Person> cast = new ArrayList<Person>();
        List<Dir> dirs = new ArrayList<Dir>();
        List<String> lines = new ArrayList<String>();
    }

    static <T> T pick(Random r, T[] a) { return a[r.nextInt(a.length)]; }
    static <T> T pick(Random r, List<T> a) { return a.get(r.nextInt(a.size())); }

    static final Set<String> ROYAL = new HashSet<String>(Arrays.asList("palace", "arena"));
    static final Set<String> MODERN = new HashSet<String>(Arrays.asList("street", "road", "park"));
    static final Set<String> SITTING = new HashSet<String>(Arrays.asList("bed", "chair", "sofa", "floor", "throne"));

    static Person person(Random r, boolean en, boolean male, boolean kid, String slot, Set<String> used, int age, String role, boolean royal) {
        Person p = new Person();
        p.male = male; p.kid = kid; p.slot = slot; p.age = age; p.role = role;
        String[] pool = slot.equals("granny") ? (en ? EN_GRANNY : HI_GRANNY)
                : kid ? (male ? (en ? EN_BOY : HI_BOY) : (en ? EN_GIRL : HI_GIRL)) : (male ? (en ? EN_MAN : HI_MAN) : (en ? EN_WOMAN : HI_WOMAN));
        do { p.name = pick(r, pool); } while (used.contains(p.name));
        used.add(p.name);
        String[] looks = slot.equals("granny") ? (en ? EN_LOOK_GRANNY : HI_LOOK_GRANNY)
                : royal ? (male ? (en ? EN_LOOK_ROYAL_M : HI_LOOK_ROYAL_M) : (en ? EN_LOOK_ROYAL_F : HI_LOOK_ROYAL_F))
                : kid ? (male ? (en ? EN_LOOK_BOY : HI_LOOK_BOY) : (en ? EN_LOOK_GIRL : HI_LOOK_GIRL))
                : (male ? (en ? EN_LOOK_MAN : HI_LOOK_MAN) : (en ? EN_LOOK_WOMAN : HI_LOOK_WOMAN));
        if (role.equals("सेनापति") || role.equals("general")) looks = en ? EN_LOOK_GENERAL : HI_LOOK_GENERAL;
        p.look = pick(r, looks);
        return p;
    }

    static int between(Random r, int a, int b) { return a + r.nextInt(b - a + 1); }

    static Gen generate(long seed) {
        // (java.util.Random's first draws for neighbouring seeds are nearly equal: the seed is scrambled first)
        Random r = new Random(new SplittableRandom(seed).nextLong());
        Gen g = new Gen();
        g.seed = seed;
        g.en = r.nextDouble() < 0.3;
        List<Row> pool = BY_LANG.get(g.en ? "en" : "hi");
        if (pool == null || pool.isEmpty()) { g.en = !g.en; pool = BY_LANG.get(g.en ? "en" : "hi"); }
        boolean en = g.en;
        Row first = pick(r, pool);
        boolean adult = first.age.equals("adult");
        // a royal story (it opens in the court or the training ground) stays off the city's roads; a modern one out of the palace
        boolean royalStory = ROYAL.contains(first.place);
        List<Row> sameAge = new ArrayList<Row>();
        for (Row x : pool) if (x.age.equals(first.age) && (royalStory ? !MODERN.contains(x.place) : !ROYAL.contains(x.place))) sameAge.add(x);

        // the scenes: a context group each, 2–7 of its rows in the file's order
        int nScenes = between(r, 1, 4);
        List<List<Row>> scenes = new ArrayList<List<Row>>();
        Set<String> usedGroups = new HashSet<String>();
        for (int s = 0; s < nScenes; s++) {
            Row seedRow = s == 0 ? first : null;
            for (int tries = 0; seedRow == null || (tries < 6 && usedGroups.contains(seedRow.key())); tries++) seedRow = pick(r, sameAge);
            usedGroups.add(seedRow.key());
            List<Row> grp = GROUPS.get(seedRow.key());
            if (SITTING.contains(seedRow.start)) {      // the one in bed / on the chair is the scene's lead: their rows only
                List<Row> same = new ArrayList<Row>();
                for (Row x : grp) if (x.girl == seedRow.girl) same.add(x);
                grp = same;
            }
            if (r.nextDouble() < 0.6) {      // mostly one category (one kind of happening) per scene, when it has rows enough
                List<Row> cat = new ArrayList<Row>();
                for (Row x : grp) if (x.cat.equals(seedRow.cat)) cat.add(x);
                if (cat.size() >= 2) grp = cat;
            }
            int k = Math.min(grp.size(), between(r, 2, 7));
            List<Integer> idx = new ArrayList<Integer>();
            for (int i = 0; i < grp.size(); i++) idx.add(i);
            Collections.shuffle(idx, r);
            List<Integer> chosen = new ArrayList<Integer>(idx.subList(0, k));
            if (!chosen.contains(grp.indexOf(seedRow)) && grp.contains(seedRow)) chosen.set(0, grp.indexOf(seedRow));
            Collections.sort(chosen);
            List<Row> rows = new ArrayList<Row>();
            for (int i : chosen) rows.add(grp.get(i));
            scenes.add(rows);
        }

        // the cast: who the rows need, then family and friends up to 2–5
        boolean needBoy = false, needGirl = false, needDad = false, needMom = !adult, royal = false;
        for (List<Row> sc : scenes) for (Row x : sc) {
            needBoy |= x.hasBoy; needGirl |= x.hasGirl; needDad |= x.dad; needMom |= x.mom;
            royal |= ROYAL.contains(x.place);
        }
        if (adult && !needMom && r.nextDouble() < 0.5) needMom = true;
        if (!needDad && r.nextDouble() < 0.3) needDad = true;
        Set<String> used = new HashSet<String>();
        Person boy = null, girl = null, mom = null, dad = null;
        if (needBoy) boy = person(r, en, true, !adult, "boy", used, adult ? between(r, 28, 36) : between(r, 8, 12),
                royal ? (en ? "prince, son" : "राजकुमार, बेटा") : (en ? "son" : "बेटा"), royal);
        if (needGirl) girl = person(r, en, false, !adult, "girl", used, adult ? between(r, 25, 32) : between(r, 8, 13),
                royal ? (en ? "princess, daughter" : "राजकुमारी, बेटी") : (en ? "daughter" : "बेटी"), royal);
        if (needMom) mom = person(r, en, false, false, "mom", used, adult ? between(r, 55, 65) : between(r, 34, 42),
                royal ? (en ? "queen, mother" : "रानी, माँ") : (en ? "mother" : "माँ"), royal);
        if (needDad) dad = person(r, en, true, false, "dad", used, adult ? between(r, 58, 68) : between(r, 36, 46),
                royal ? (en ? "king, father" : "राजा, पिता") : (en ? "father" : "पिता"), royal);
        for (Person p : new Person[]{boy, girl, mom, dad}) if (p != null) g.cast.add(p);
        int target = Math.max(g.cast.size(), between(r, 2, 5));
        List<String> extras = new ArrayList<String>();
        if (boy == null) extras.add("boy");
        if (girl == null) extras.add("girl");
        if (mom == null) extras.add("mom");
        extras.add("friendM"); extras.add("friendF");
        if (!adult) extras.add("granny");
        if (royal) extras.add("general");
        Collections.shuffle(extras, r);
        for (String e : extras) {
            if (g.cast.size() >= target || g.cast.size() >= 5) break;
            Person p;
            switch (e) {
                case "boy": p = boy = person(r, en, true, !adult, "boy", used, adult ? between(r, 28, 36) : between(r, 8, 12), royal ? (en ? "prince, son" : "राजकुमार, बेटा") : (en ? "son" : "बेटा"), royal); break;
                case "girl": p = girl = person(r, en, false, !adult, "girl", used, adult ? between(r, 25, 32) : between(r, 8, 13), royal ? (en ? "princess, daughter" : "राजकुमारी, बेटी") : (en ? "daughter" : "बेटी"), royal); break;
                case "mom": p = mom = person(r, en, false, false, "mom", used, adult ? between(r, 55, 65) : between(r, 34, 42), royal ? (en ? "queen, mother" : "रानी, माँ") : (en ? "mother" : "माँ"), royal); break;
                case "friendM": p = person(r, en, true, !adult, "extra", used, adult ? between(r, 26, 38) : between(r, 8, 12), en ? "friend" : "दोस्त", false); break;
                case "friendF": p = person(r, en, false, !adult, "extra", used, adult ? between(r, 25, 36) : between(r, 8, 12), en ? "friend" : "सहेली", false); break;
                case "granny": p = person(r, en, false, false, "granny", used, between(r, 62, 75), en ? "grandmother" : "दादी", false); break;
                default: p = person(r, en, true, false, "extra", used, between(r, 35, 50), en ? "general" : "सेनापति", false); break;
            }
            g.cast.add(p);
        }
        if (g.cast.size() < 2) g.cast.add(person(r, en, false, false, "mom", used, adult ? 60 : 38, en ? "mother" : "माँ", false));

        // the script
        StringBuilder b = new StringBuilder();
        Person lead0 = boy != null ? boy : girl;
        String title = en ? pick(r, new String[]{lead0.name + "'s Day", "A Day to Remember", lead0.name + " and the Family", "One Fine Day", lead0.name + "'s Little Adventure"})
                : pick(r, new String[]{lead0.name + (lead0.male ? " का दिन" : " का दिन"), "एक यादगार दिन", lead0.name + " और परिवार", "एक सुहाना दिन", lead0.name + (lead0.male ? " का छोटा सा सफ़र" : " का छोटा सा सफ़र")});
        b.append(en ? "Title: " : "फिल्म का नाम: ").append(title).append("\n\n").append(en ? "Cast" : "कलाकार").append("\n\n");
        for (int i = 0; i < g.cast.size(); i++) {
            Person p = g.cast.get(i);
            b.append(i + 1).append(". ").append(p.name).append(" - ").append(p.age).append(en ? " years, " : " साल, ").append(p.role).append('\n')
                    .append(en ? "Look: " : "रूप: ").append(p.look).append('\n');
        }
        b.append('\n').append(en ? "Places" : "जगह").append("\n\n");
        List<String> placeKeys = new ArrayList<String>();
        for (List<Row> sc : scenes) if (!placeKeys.contains(sc.get(0).place)) placeKeys.add(sc.get(0).place);
        for (int i = 0; i < placeKeys.size(); i++) {
            String[] p = place(placeKeys.get(i));
            String desc = en ? p[6] : p[3];
            b.append(i + 1).append(". ").append(en ? p[4] : home(p[1], royal)).append(desc.isEmpty() ? "" : " - " + desc).append('\n');
        }
        b.append('\n');
        for (int s = 0; s < scenes.size(); s++) {
            List<Row> sc = scenes.get(s);
            Row c = sc.get(0);
            String[] p = place(c.place);
            b.append(en ? "Scene " : "सीन ").append(s + 1).append(": ").append(en ? p[4] : home(p[1], royal)).append(" - ").append(en ? enTod(c.tod) : hiTod(c.tod)).append("\n\n");
            // who is in it: the rows' people, Mother (always with a child's rows, as in the probe), Father with fam, others sometimes
            List<Person> in = new ArrayList<Person>();
            boolean wantMom = !adult, wantDad = false;
            for (Row x : sc) {
                if (x.hasBoy && boy != null && !in.contains(boy)) in.add(boy);
                if (x.hasGirl && girl != null && !in.contains(girl)) in.add(girl);
                wantMom |= x.mom; wantDad |= x.dad;
            }
            Person lead = sc.get(0).girl ? girl : boy;
            if (lead == null) lead = in.isEmpty() ? g.cast.get(0) : in.get(0);
            if (!in.contains(lead)) in.add(0, lead);
            if (mom != null && !in.contains(mom) && (wantMom || r.nextDouble() < 0.7)) in.add(mom);
            if (dad != null && !in.contains(dad) && (wantDad || r.nextDouble() < 0.4)) in.add(dad);
            for (Person q : g.cast) if (!in.contains(q) && in.size() < 4 && r.nextDouble() < 0.35) in.add(q);
            if (in.size() < 2) for (Person q : g.cast) if (!in.contains(q)) { in.add(q); break; }
            List<Person> others = new ArrayList<Person>(in);
            others.remove(lead);
            boolean quietStart = s > 0 && c.start.equals("stand") && r.nextDouble() < 0.3;
            if (!quietStart) b.append('[').append(opening(en, c, p, lead, others)).append("]\n\n");
            // the first line: the story's opening, or the scene's
            String[] openPool = s == 0 ? (c.tod.equals("morning") ? (en ? EN_OPEN_MORNING : HI_OPEN_MORNING) : c.tod.equals("night") ? (en ? EN_OPEN_NIGHT : HI_OPEN_NIGHT) : (en ? EN_OPEN : HI_OPEN))
                    : (en ? EN_SCENE : HI_SCENE);
            line(g, b, r, en, in, null, openPool, false);
            for (int i = 0; i < sc.size(); i++) {
                Row x = sc.get(i);
                Dir d = new Dir();
                d.row = x; d.scene = s;
                d.main = x.girl ? girl : boy;
                d.text = subst(x.sentence, en, boy, girl);
                d.lineBefore = g.lines.size() - 1;
                b.append('[').append(d.text).append("]\n\n");
                boolean last = s == scenes.size() - 1 && i == sc.size() - 1;
                line(g, b, r, en, in, r.nextDouble() < 0.35 ? d.main : null, last ? (en ? EN_CLOSE : HI_CLOSE) : (en ? EN_LINES : HI_LINES), true);
                d.lineAfter = g.lines.size() - 1;
                g.dirs.add(d);
            }
        }
        g.script = b.toString();
        return g;
    }

    static String subst(String s, boolean en, Person boy, Person girl) {
        String a = en ? "Rahul" : "राहुल", c = en ? "Priya" : "प्रिया";
        s = s.replace(a, "\u0001").replace(c, "\u0002");
        return s.replace("\u0001", boy != null ? boy.name : a).replace("\u0002", girl != null ? girl.name : c);
    }

    /** One dialogue line by someone in the scene (the given speaker when not null); "{n}" names another one there. */
    static void line(Gen g, StringBuilder b, Random r, boolean en, List<Person> in, Person who, String[] pool, boolean manner) {
        if (who == null || !in.contains(who)) who = pick(r, in);
        String t = null;
        Person o = null;
        for (int tries = 0; tries < 12; tries++) {
            t = pick(r, pool);
            o = null;
            if (t.contains("{n}")) for (int k = 0; k < 4 && (o == null || o == who); k++) o = pick(r, in);
            if (o == who) o = null;
            // praise ("शाबाश", "Well done") is said by an elder to a younger one
            if (t.startsWith("^") && !(who.age >= 18 && (o == null ? true : who.age > o.age + 10))) continue;
            break;
        }
        if (t.startsWith("^")) t = t.substring(1);
        if (t.contains("|")) t = t.split("\\|")[who.male ? 0 : 1];
        if (t.contains("{n}")) {
            if (o == null) t = t.replace(", {n}", "").replace("{n}, ", "").replace("{n}", "");
            else t = t.replace("{n}", callName(en, who, o));
            if (!t.isEmpty()) t = t.substring(0, 1).toUpperCase(Locale.ROOT) + t.substring(1);
        }
        String m = manner && r.nextDouble() < 0.3 ? " (" + pick(r, en ? EN_MANNER : HI_MANNER) + ")" : "";
        b.append(who.name).append(m).append(": \"").append(t).append("\"\n\n");
        g.lines.add(t);
    }

    /** What the speaker calls the other one: children call their parents and grandmother माँ / पापा / दादी, friends call
     *  them aunty and uncle; everyone else by name. */
    static String callName(boolean en, Person who, Person o) {
        boolean child = who.slot.equals("boy") || who.slot.equals("girl"), friend = who.slot.equals("extra");
        boolean royal = o.role.contains("रानी") || o.role.contains("राजा") || o.role.contains("queen") || o.role.contains("king");
        switch (o.slot) {
            case "mom": return child ? (en ? (royal ? "Mother" : "Mom") : "माँ") : friend ? (en ? (royal ? "Your Majesty" : "Aunty") : (royal ? "महारानी" : "आंटी")) : o.name;
            case "dad": return child ? (en ? (royal ? "Father" : "Dad") : (royal ? "पिताजी" : "पापा")) : friend ? (en ? (royal ? "Your Majesty" : "Uncle") : (royal ? "महाराज" : "अंकल")) : o.name;
            case "granny": return who.slot.equals("mom") || who.slot.equals("dad") ? (en ? "Mother" : "माँजी") : en ? "Grandma" : "दादी";
            default: return o.name;
        }
    }

    /** The scene's opening direction: the lead in the context's start state (as PhenomenaProbe sets it), the others near. */
    static String opening(boolean en, Row c, String[] p, Person lead, List<Person> others) {
        String n = lead.name;
        boolean f = !lead.male;
        String open;
        if (!en) {
            String stand = f ? "खड़ी" : "खड़ा", sit = f ? "बैठी" : "बैठा", sleep = f ? "सो रही" : "सो रहा";
            String here = n + " " + p[2] + " " + stand + " है।";
            switch (c.start) {
                case "chair": open = n + " " + p[2] + " कुर्सी पर " + sit + " है।"; break;
                case "sofa": open = n + " " + p[2] + " सोफ़े पर " + sit + " है।"; break;
                case "floor": open = n + " " + p[2] + " ज़मीन पर " + sit + " है।"; break;
                case "bed": open = n + " " + p[2] + " बिस्तर पर " + sleep + " है।"; break;
                case "throne": open = n + " " + p[2] + " सिंहासन पर " + sit + " है।"; break;
                case "rain": open = "तेज़ बारिश हो रही है। " + here; break;
                case "fog": open = "चारों ओर घना कोहरा छाया हुआ है। " + here; break;
                case "wind": open = "तेज़ हवा चल रही है। " + here; break;
                default: open = here;
            }
            if (others.isEmpty()) return open;
            boolean allF = true;
            for (Person q : others) allF &= !q.male;
            if (others.size() == 1) return open + " " + others.get(0).name + " पास में " + (allF ? "खड़ी" : "खड़ा") + " है।";
            return open + " " + names(others, " और ") + " पास में " + (allF ? "खड़ी" : "खड़े") + " हैं।";
        }
        String here = n + " stands " + p[5] + ".";
        switch (c.start) {
            case "chair": open = n + " is sitting on a chair " + p[5] + "."; break;
            case "sofa": open = n + " is sitting on the sofa " + p[5] + "."; break;
            case "floor": open = n + " is sitting on the floor " + p[5] + "."; break;
            case "bed": open = n + " is asleep in bed " + p[5] + "."; break;
            case "throne": open = n + " is sitting on the throne " + p[5] + "."; break;
            case "rain": open = "It is raining heavily. " + here; break;
            case "fog": open = "Thick fog lies all around. " + here; break;
            case "wind": open = "A strong wind is blowing. " + here; break;
            default: open = here;
        }
        if (others.isEmpty()) return open;
        String him = f ? "her" : "him";
        return open + " " + names(others, " and ") + (others.size() == 1 ? " stands" : " stand") + " near " + him + ".";
    }

    static String names(List<Person> ps, String and) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < ps.size(); i++) {
            if (i > 0) b.append(i == ps.size() - 1 ? and : ", ");
            b.append(ps.get(i).name);
        }
        return b.toString();
    }

    // ================================================================== checking a film

    static final int PASS = 0, CONTEXT = 1, FAIL = 2, UNMATCHED = 3, BADCODE = 4, EVALERR = 5;
    static final String[] VERDICT = {"PASS", "CONTEXT", "FAIL", "UNMATCHED", "BADCODE", "EVALERR"};

    static final class DirRes { int row, verdict; String got; float tA, tB; PhenomenaProbe.Run run; }

    static final class Result {
        long seed;
        double ms;
        float filmSec;
        String crash, crashTrace;
        Map<String, Integer> inv = new TreeMap<String, Integer>();
        Map<String, String> invNote = new TreeMap<String, String>();
        List<DirRes> dirs = new ArrayList<DirRes>();
    }

    static String norm(String s) {
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < s.length(); i++) { char ch = s.charAt(i); if (Character.isLetterOrDigit(ch) || Character.getType(ch) == Character.NON_SPACING_MARK || Character.getType(ch) == Character.COMBINING_SPACING_MARK) b.append(Character.toLowerCase(ch)); }
        return b.toString();
    }

    /** The film's line for each script line (by order when the counts agree, else by text), -1 when not found. */
    static int[] alignLines(Gen g, Film f) {
        int[] m = new int[g.lines.size()];
        if (f.lines.size() == g.lines.size()) { for (int i = 0; i < m.length; i++) m[i] = i; return m; }
        int j = 0;
        for (int i = 0; i < m.length; i++) {
            String want = norm(g.lines.get(i));
            String head = want.length() > 6 ? want.substring(0, 6) : want;
            m[i] = -1;
            for (int k = j; k < f.lines.size(); k++) {
                String have = norm(f.lines.get(k).text == null ? "" : f.lines.get(k).text);
                if (!head.isEmpty() && (have.startsWith(head) || have.contains(head) || want.contains(have) && have.length() > 3)) { m[i] = k; j = k + 1; break; }
            }
        }
        return m;
    }

    static Film.Actor findActor(Film f, float t, String name) {
        for (Film.Seg s : f.segs) if (s.type == Film.S_SCENE && t >= s.t0 - 0.01f && t < s.t1) { Film.Actor a = PhenomenaProbe.mainActor(s, name); if (a != null) return a; }
        for (Film.Seg s : f.segs) if (t >= s.t0 - 0.01f && t < s.t1) { Film.Actor a = PhenomenaProbe.mainActor(s, name); if (a != null) return a; }
        return null;
    }

    static Film.Seg segOf(Film f, Film.Actor a) {
        for (Film.Seg s : f.segs) if (s.actors.contains(a)) return s;
        return null;
    }

    /** PhenomenaProbe's Run with the film as its own baseline (nothing subtracted). */
    static PhenomenaProbe.Run run(Film f, Film.Seg seg, Film.Actor main, float tA, float tB) {
        PhenomenaProbe.Run r = new PhenomenaProbe.Run();
        r.f = f; r.base = f; r.seg = seg; r.baseSeg = null; r.main = main; r.baseMain = main;
        r.tA = tA; r.tB = tB; r.sig = "story"; r.sig0 = "-"; r.sig1 = "-";
        return r;
    }

    static boolean holds(String code, PhenomenaProbe.Run r) {
        int c = code.indexOf(':');
        String kind = c < 0 ? code : code.substring(0, c), arg = c < 0 ? "" : code.substring(c + 1);
        if (kind.equals("FX")) {     // only an effect that begins with the sentence (no neutral scene to count against)
            int ty = PhenomenaProbe.film("FX_", arg);
            for (Film.Fx x : PhenomenaProbe.windowFx(r)) if (x.type == ty) return true;
            return false;
        }
        if (kind.equals("ANY")) {
            for (Film.Act a : PhenomenaProbe.windowActs(r)) if (a.type != Film.G_TALK) return true;
            if (!PhenomenaProbe.windowFx(r).isEmpty() || !PhenomenaProbe.windowSfx(r).isEmpty() || PhenomenaProbe.moved(r) > 40) return true;
            Film.Key b0 = PhenomenaProbe.before(r);
            for (Film.Key k : PhenomenaProbe.windowKeys(r)) if (k.body != b0.body || k.holdR != b0.holdR || k.holdL != b0.holdL || k.visible != b0.visible) return true;
            return false;
        }
        return PhenomenaProbe.holds(code, r);
    }

    /** Whether what a failed code asks for held already just before the window (so the sentence had nothing to change). */
    static boolean preHeld(String code, PhenomenaProbe.Run r) {
        int c = code.indexOf(':');
        String kind = c < 0 ? code : code.substring(0, c), arg = c < 0 ? "" : code.substring(c + 1);
        Film.Key b = PhenomenaProbe.before(r);
        float t = r.tA - 0.06f;
        switch (kind) {
            case "BODY": return b.body == PhenomenaProbe.pose(arg, PhenomenaProbe.BODIES);
            case "SEAT": return b.seat == PhenomenaProbe.film("SEAT_", arg) && (b.body == Pose.SIT || b.body == Pose.LIE);
            case "EMO": return b.emotion == PhenomenaProbe.pose(arg, PhenomenaProbe.EMOTIONS);
            case "HOLD": { int i = PhenomenaProbe.constant(Pose.class, "I_" + arg); return b.holdR == i || b.holdL == i; }
            case "EYES": return b.eyesShut;
            case "AWAKE": return !b.eyesShut;
            case "TEARS": return b.tears;
            case "SWEAT": return b.sweat;
            case "HIDE": return PhenomenaProbe.hidden(b);
            case "UMBRELLA": return r.main.umbrellaAt(r.main.look, t);
            case "WEND": {
                int ty = PhenomenaProbe.film("W_", arg);
                for (Film.Weather w : r.f.weather) if (w.type == ty && w.t0 <= t && w.t1 >= t) return false;
                return true;     // nothing of the kind on the screen to end
            }
            case "ACT": return activeAct(r, PhenomenaProbe.film("G_", arg), 0, -1, t);
            case "TASK": return activeAct(r, Film.G_TASK, PhenomenaProbe.film("T_", arg), 0, t);
            case "RIDE": return activeAct(r, Film.G_RIDE, PhenomenaProbe.film("V_", arg), 1, t);
            case "PG": return activeAct(r, Film.G_PLAYGROUND, PhenomenaProbe.film("PG_", arg), 2, t);
            case "EX": return activeAct(r, Film.G_EXERCISE, PhenomenaProbe.film("EX_", arg), 0, t);
            case "MK": return activeAct(r, Film.G_MAKEUP, PhenomenaProbe.film("MK_", arg), 0, t);
            case "DRINK": return activeAct(r, Film.G_DRINK, PhenomenaProbe.constant(Pose.class, "I_" + arg), 0, t);
            case "ARCH": return activeAct(r, Film.G_ARCHERY, PhenomenaProbe.film("ARCH_", arg), 0, t);
            case "SNOW": return activeAct(r, Film.G_SNOW_PLAY, PhenomenaProbe.film("SNOW_", arg), 0, t);
            default: return false;
        }
    }

    /** mode 0: item equal (-1 item: any); 1: |item| % V_PASSENGER; 2: |item|. */
    static boolean activeAct(PhenomenaProbe.Run r, int type, int item, int mode, float t) {
        for (Film.Act a : r.main.acts) {
            if (a.type != type || a.t0 > t || a.t1 < t) continue;
            int it = mode == 1 ? Math.abs(a.item) % Film.V_PASSENGER : mode == 2 ? Math.abs(a.item) : a.item;
            if (item == -1 || it == item) return true;
        }
        return false;
    }

    /** A negated code that fails because its state was already there (the rain that was falling before). */
    static boolean preHeldNot(String code, PhenomenaProbe.Run r) {
        if (!code.startsWith("W:")) return false;
        int ty = PhenomenaProbe.film("W_", code.substring(2));
        float t = r.tA - 0.06f;
        for (Film.Weather w : r.f.weather) if (w.type == ty && w.t0 <= t && w.t1 >= t) return true;
        return false;
    }

    static int verdict(String exp, PhenomenaProbe.Run r) {
        boolean ctx = false;
        for (String grp : exp.split("\\|")) {
            boolean all = true, allCtx = true;
            for (String code : grp.split("&")) {
                code = code.trim();
                boolean not = code.startsWith("!");
                String cc = not ? code.substring(1) : code;
                boolean h = holds(cc, r);
                if (h != not) continue;
                all = false;
                if (!(not ? preHeldNot(cc, r) : preHeld(cc, r))) { allCtx = false; break; }
            }
            if (all) return PASS;
            if (allCtx) ctx = true;
        }
        return ctx ? CONTEXT : FAIL;
    }

    static boolean DUMP;

    static Result check(long seed, boolean keepGot) {
        Result res = new Result();
        res.seed = seed;
        long t0 = System.nanoTime();
        Gen g = null;
        try {
            g = generate(seed);
            Story st = ScriptParser.parse(g.script);
            Director d = new Director(st, new Director.Options());
            d.prepare();
            Film film = d.direct(new Art());
            res.filmSec = film.duration;
            List<String> notes = new ArrayList<String>();
            invariants(st, film, res.inv, notes, res.invNote);
            int[] m = alignLines(g, film);
            for (Dir dir : g.dirs) {
                DirRes dr = new DirRes();
                dr.row = dir.row.idx;
                res.dirs.add(dr);
                if (dir.row.bad != null) { dr.verdict = BADCODE; continue; }
                int a = dir.lineBefore >= 0 ? m[dir.lineBefore] : -1, bI = m[dir.lineAfter];
                if (a < 0 || bI < 0 || dir.main == null) { dr.verdict = UNMATCHED; dr.got = "window lines not found in the film (" + film.lines.size() + " film lines, " + g.lines.size() + " written)"; continue; }
                Film.Line l0 = film.lines.get(a), l1 = film.lines.get(bI);
                float tA = l0.start + l0.dur - 0.05f, tB = l1.start;
                dr.tA = tA; dr.tB = tB;
                Film.Actor main = findActor(film, tA, dir.main.name);
                boolean stand = false;
                if (main == null) {     // gone off earlier (a weather row needs no one in particular): anyone of the scene stands in
                    for (Film.Seg s : film.segs) if (s.type == Film.S_SCENE && tA >= s.t0 - 0.01f && tA < s.t1 && !s.actors.isEmpty()) { main = s.actors.get(0); stand = true; break; }
                    if (main == null) { dr.verdict = UNMATCHED; dr.got = dir.main.name + " not on the stage at " + String.format(Locale.US, "%.1f", tA); continue; }
                }
                PhenomenaProbe.Run r = run(film, segOf(film, main), main, tA, tB);
                if (DUMP) dr.run = r;
                try {
                    dr.verdict = verdict(dir.row.expect, r);
                    if (stand && dr.verdict != PASS) { dr.verdict = UNMATCHED; dr.got = dir.main.name + " not on the stage at " + String.format(Locale.US, "%.1f", tA); continue; }
                    if (dr.verdict != PASS && keepGot) dr.got = PhenomenaProbe.summary(r);
                } catch (Throwable e) {
                    dr.verdict = EVALERR;
                    dr.got = String.valueOf(e);
                }
            }
        } catch (Throwable e) {
            StackTraceElement top = null;
            for (StackTraceElement el : e.getStackTrace()) if (el.getClassName().startsWith("com.tarun.kahani.core")) { top = el; break; }
            if (top == null && e.getStackTrace().length > 0) top = e.getStackTrace()[0];
            res.crash = e.getClass().getSimpleName() + " at " + (top == null ? "?" : top.getClassName().replace("com.tarun.kahani.core.", "") + "." + top.getMethodName() + ":" + top.getLineNumber());
            StringBuilder tr = new StringBuilder(String.valueOf(e));
            StackTraceElement[] st = e.getStackTrace();
            for (int i = 0; i < Math.min(6, st.length); i++) tr.append("\n      at ").append(st[i]);
            res.crashTrace = tr.toString();
        }
        res.ms = (System.nanoTime() - t0) / 1e6;
        return res;
    }

    // ================================================================== SoakCheck's invariants (copied: its checks live in its main)

    static void inv(Map<String, Integer> issues, Map<String, String> first, String kind, String note) {
        issues.merge(kind, 1, Integer::sum);
        if (note != null && !first.containsKey(kind)) first.put(kind, note);
    }

    static void invariants(Story st, Film film, Map<String, Integer> issues, List<String> notes, Map<String, String> first) {
        if (st.cast().isEmpty()) inv(issues, first, "no cast", null);
        if (st.scenes.isEmpty()) inv(issues, first, "no scenes", null);
        if (film.stats != null) {
            Handbook.Card card = Handbook.score(film, film.stats, -1, -1, -1);
            if (!card.critical.isEmpty()) inv(issues, first, "protocol critical", String.valueOf(card.critical));
        }
        if (film.duration < 5 || film.duration > 3600 || SoakCheck.bad(film.duration)) inv(issues, first, "film length", "duration " + film.duration);
        if (film.shots.isEmpty()) inv(issues, first, "no shots", null);
        float prevEnd = -1;
        for (Film.Seg sg : film.segs) {
            if (sg.t1 < sg.t0) inv(issues, first, "segment ends before it starts", null);
            if (prevEnd > sg.t0 + 1.5f) inv(issues, first, "segments overlap", null);
            prevEnd = sg.t1;
            for (Film.Actor ac : sg.actors) {
                for (int i = 0; i < ac.keys.size(); i++) {
                    Film.Key k = ac.keys.get(i);
                    if (SoakCheck.bad(k.t) || SoakCheck.bad(k.x) || SoakCheck.bad(k.moveDur) || SoakCheck.bad(k.facing)) inv(issues, first, "NaN key", ac.c.fullName + " t=" + k.t);
                    if (i > 0 && k.t < ac.keys.get(i - 1).t - 1e-3f) inv(issues, first, "key out of order", ac.c.fullName + " t=" + k.t + " after " + ac.keys.get(i - 1).t);
                    if (k.moveDur < 0) inv(issues, first, "negative move", ac.c.fullName + " t=" + k.t);
                    if (k.moveDur > 0.3f && k.visible && k.body == Pose.LIE && !SoakCheck.playing(ac, k.t) && !SoakCheck.thrown(ac, k.t) && Math.abs(k.x - Director.xAt(ac, k.t + 0.01f)) > 40)
                        inv(issues, first, "moves while lying", ac.c.fullName + " moves lying at " + k.t);
                    if (k.moveDur > 0.3f && k.visible && k.body == Pose.SIT && Math.abs(k.x - Director.xAt(ac, k.t + 0.01f)) > 40) {
                        boolean riding = false;
                        for (Film.Act x : ac.acts) if ((x.type == Film.G_RIDE || x.type == Film.G_PLAYGROUND || (x.type == Film.G_SNOW_PLAY && x.item == Film.SNOW_SLIDE)) && k.t >= x.t0 - 0.5f && k.t <= x.t1 + 0.5f) riding = true;
                        if (!riding && ac.look != null && ac.look.aid != Look.AID_WHEELCHAIR) inv(issues, first, "walks seated", ac.c.fullName + " walks seated at " + k.t);
                    }
                }
                for (float tt = sg.t0 + 0.5f; tt < sg.t1 - 0.5f; tt += 1f) {
                    Film.Key kk = ac.stateAt(tt);
                    if (!kk.visible || kk.anchor != Film.A_GROUND) continue;
                    float xx = Director.xAt(ac, tt);
                    boolean moving = false;
                    for (Film.Key mm : ac.keys) if (mm.moveDur > 0 && tt >= mm.t && tt <= mm.t + mm.moveDur + 0.6f) moving = true;
                    if ((xx < -150 || xx > 1430) && !moving) { inv(issues, first, "lingers off the stage", String.format(Locale.US, "%s at x=%.0f t=%.1f", ac.c.fullName, xx, tt)); break; }
                }
                for (Film.Act x : ac.acts) {
                    if (SoakCheck.bad(x.t0) || SoakCheck.bad(x.t1)) inv(issues, first, "NaN act", ac.c.fullName + " act " + x.type);
                    if (x.t1 < x.t0) inv(issues, first, "act ends before it starts", ac.c.fullName + " act " + PhenomenaProbe.nameOf("G_", x.type) + " " + x.t0 + ".." + x.t1);
                }
                // [forge] checks SoakCheck does not have: one person on two vehicles at once
                for (int i = 0; i < ac.acts.size(); i++) for (int j = i + 1; j < ac.acts.size(); j++) {
                    Film.Act x = ac.acts.get(i), y = ac.acts.get(j);
                    if (x.type == Film.G_RIDE && y.type == Film.G_RIDE && Math.min(x.t1, y.t1) - Math.max(x.t0, y.t0) > 0.3f)
                        inv(issues, first, "[forge] two rides at once", String.format(Locale.US, "%s %s %.2f..%.2f and %s %.2f..%.2f", ac.c.fullName,
                                PhenomenaProbe.actName(x), x.t0, x.t1, PhenomenaProbe.actName(y), y.t0, y.t1));
                }
            }
            for (Film.Fx x : sg.fx) {
                if (SoakCheck.bad(x.t0) || SoakCheck.bad(x.t1)) inv(issues, first, "[forge] NaN fx", PhenomenaProbe.nameOf("FX_", x.type));
                else if (x.t1 < x.t0 - 1e-3f) inv(issues, first, "[forge] fx ends before it starts", String.format(Locale.US, "%s %.2f..%.2f", PhenomenaProbe.nameOf("FX_", x.type), x.t0, x.t1));
            }
            Film.Cam prev = null;
            for (Film.Cam c : sg.cams) {
                if (SoakCheck.bad(c.t) || SoakCheck.bad(c.cx) || SoakCheck.bad(c.cy) || SoakCheck.bad(c.zoom)) inv(issues, first, "NaN camera", "t=" + c.t);
                if (c.zoom < 0.9f || c.zoom > 4.6f) inv(issues, first, "wild zoom", "zoom " + c.zoom + " at " + c.t);
                if (prev != null && c.ease == 0 && c.t - prev.t < 0.25f && c.t - prev.t > 1e-3f && Math.abs(c.zoom - prev.zoom) + Math.abs(c.cx - prev.cx) / 200 > 0.15f)
                    inv(issues, first, "flash cut", String.format(Locale.US, "flash cut %.2f→%.2f", prev.t, c.t));
                prev = c;
            }
        }
        for (Film.Seg sg : film.segs) {
            for (int p = 0; p < sg.actors.size(); p++) for (int q = p + 1; q < sg.actors.size(); q++) {
                Film.Actor A = sg.actors.get(p), B = sg.actors.get(q);
                if (A.look == null || B.look == null || !A.look.isHumanoid() || !B.look.isHumanoid()) continue;
                float run = 0;
                for (float tt = sg.t0 + 0.25f; tt < sg.t1; tt += 0.25f) {
                    Film.Key ka = A.stateAt(tt), kb = B.stateAt(tt);
                    float dl = Math.abs(Director.liftAt(A, tt) - Director.liftAt(B, tt));
                    boolean still = Math.abs(Director.xAt(A, tt) - Director.xAt(A, tt - 0.25f)) < 3 && Math.abs(Director.xAt(B, tt) - Director.xAt(B, tt - 0.25f)) < 3;
                    boolean close = ka.visible && kb.visible && ka.anchor == Film.A_GROUND && kb.anchor == Film.A_GROUND
                            && Math.abs(Director.xAt(A, tt) - Director.xAt(B, tt)) < 45 && !SoakCheck.together(A, B, tt)
                            && Math.abs(ka.depth - kb.depth) < 0.2f && dl < 20 && (dl < 10 || still);
                    run = close ? run + 0.25f : 0;
                    if (run >= 1.5f) { inv(issues, first, "stand on each other", String.format(Locale.US, "%s+%s t=%.1f", A.c.fullName, B.c.fullName, tt)); break; }
                }
            }
        }
        for (Film.Line l : film.lines) {
            if (l.who == null || l.who.voiceOnly) continue;
            Film.Seg in = null;
            for (Film.Seg sg : film.segs) if (l.start >= sg.t0 - 1e-3f && l.start < sg.t1) in = sg;
            if (in == null) { inv(issues, first, "line outside the film", l.text); continue; }
            Film.Actor sp = null;
            for (Film.Actor ac : in.actors) if (ac.c == l.who) sp = ac;
            if (sp == null) { inv(issues, first, "speaker not in the scene", l.who.fullName + " not in scene at " + l.start); continue; }
            float tm = l.start + Math.min(0.5f, l.dur * 0.5f);
            Film.Key k = sp.stateAt(tm);
            if (!k.visible) { inv(issues, first, "speaks unseen", l.who.fullName + " unseen at " + tm); continue; }
            float x = Director.xAt(sp, tm);
            if (x < -60 || x > 1340) inv(issues, first, "speaks off the stage", l.who.fullName + " at x=" + (int) x + " t=" + tm);
            int seen = 0;
            for (float u : new float[]{0.3f, 0.6f, 0.9f}) {
                float tt = l.start + l.dur * u;
                Film.Cam cc = null;
                for (Film.Cam c : in.cams) if (c.t <= tt) cc = c;
                if (cc == null) { seen++; continue; }
                float half = 640f / cc.zoom, xs = Director.xAt(sp, tt);
                if (xs > cc.cx - half + 20 && xs < cc.cx + half - 20) seen++;
            }
            if (seen == 0) inv(issues, first, "speaker never in the frame", String.format(Locale.US, "%s unframed %.1f-%.1f", l.who.fullName, l.start, l.start + l.dur));
        }
    }

    // ================================================================== the report

    static final class Agg {
        int stories, crashed, invStories, dirs;
        int[] verdicts = new int[VERDICT.length];
        double msSum, filmSec;
        List<Double> ms = new ArrayList<Double>();
        List<long[]> slow = new ArrayList<long[]>();    // seed, ms
        Map<String, List<Long>> crashes = new TreeMap<String, List<Long>>();
        Map<String, Integer> crashCount = new TreeMap<String, Integer>();
        Map<String, String> crashTrace = new HashMap<String, String>();
        Map<String, Integer> invCount = new TreeMap<String, Integer>();
        Map<String, List<Long>> invSeeds = new TreeMap<String, List<Long>>();
        Map<String, String> invNote = new HashMap<String, String>();
        int[][] row;            // per row: verdict counts
        long[] rowSeed;         // an example seed of a FAIL
        String[] rowGot;
        long[] rowCtxSeed;

        Agg() { row = new int[ROWS.size()][VERDICT.length]; rowSeed = new long[ROWS.size()]; rowGot = new String[ROWS.size()]; rowCtxSeed = new long[ROWS.size()]; Arrays.fill(rowSeed, -1); Arrays.fill(rowCtxSeed, -1); }

        void add(Result r) {
            stories++;
            msSum += r.ms; ms.add(r.ms); filmSec += r.filmSec;
            slow.add(new long[]{r.seed, (long) r.ms});
            if (slow.size() > 50) { slow.sort((a, b) -> Long.compare(b[1], a[1])); slow.subList(10, slow.size()).clear(); }
            if (r.crash != null) {
                crashed++;
                crashCount.merge(r.crash, 1, Integer::sum);
                List<Long> s = crashes.computeIfAbsent(r.crash, k -> new ArrayList<Long>());
                if (s.size() < 3) s.add(r.seed);
                crashTrace.putIfAbsent(r.crash, r.crashTrace);
            }
            if (!r.inv.isEmpty()) invStories++;
            for (Map.Entry<String, Integer> e : r.inv.entrySet()) {
                invCount.merge(e.getKey(), 1, Integer::sum);
                List<Long> s = invSeeds.computeIfAbsent(e.getKey(), k -> new ArrayList<Long>());
                if (s.size() < 3) s.add(r.seed);
                if (r.invNote.containsKey(e.getKey())) invNote.putIfAbsent(e.getKey(), r.invNote.get(e.getKey()) + " (seed " + r.seed + ")");
            }
            for (DirRes d : r.dirs) {
                dirs++;
                verdicts[d.verdict]++;
                row[d.row][d.verdict]++;
                if ((d.verdict == FAIL || d.verdict == UNMATCHED || d.verdict == EVALERR) && rowSeed[d.row] < 0) { rowSeed[d.row] = r.seed; rowGot[d.row] = d.got; }
                if (d.verdict == CONTEXT && rowCtxSeed[d.row] < 0) rowCtxSeed[d.row] = r.seed;
            }
        }

        void print(PrintStream o, int top, boolean full, double wallSec, int threads) {
            Collections.sort(ms);
            o.printf(Locale.US, "FORGE %d stories, %d crashed, %d with invariant failures; %d directions: %s%n", stories, crashed, invStories, dirs, verdictLine(verdicts));
            o.printf(Locale.US, "TIME per story: mean %.0f ms, median %.0f, p95 %.0f, max %.0f; wall %.1f s with %d threads (%.1f stories/s); %.0f s of film%n",
                    msSum / Math.max(1, stories), q(0.5), q(0.95), q(1.0), wallSec, threads, stories / Math.max(1e-9, wallSec), filmSec);
            slow.sort((a, b) -> Long.compare(b[1], a[1]));
            StringBuilder sl = new StringBuilder();
            for (int i = 0; i < Math.min(5, slow.size()); i++) sl.append(" ").append(slow.get(i)[0]).append("(").append(slow.get(i)[1]).append("ms)");
            o.println("SLOWEST seeds:" + sl);
            for (String n : LOAD_NOTES) o.println("NOTE " + n);
            if (!crashCount.isEmpty()) {
                o.println("CRASHES:");
                for (Map.Entry<String, Integer> e : sortDesc(crashCount)) {
                    o.println("  " + e.getValue() + "x " + e.getKey() + "  seeds " + crashes.get(e.getKey()));
                    o.println("      " + crashTrace.get(e.getKey()));
                }
            }
            if (!invCount.isEmpty()) {
                o.println("INVARIANTS (stories):");
                for (Map.Entry<String, Integer> e : sortDesc(invCount))
                    o.println("  " + e.getValue() + "x " + e.getKey() + "  seeds " + invSeeds.get(e.getKey()) + (invNote.containsKey(e.getKey()) ? "  e.g. " + invNote.get(e.getKey()) : ""));
            }
            // per category
            Map<String, int[]> cats = new TreeMap<String, int[]>();
            for (Row r : ROWS) {
                int[] c = cats.computeIfAbsent(r.cat + (r.file.contains("v44") ? "(v44)" : ""), k -> new int[VERDICT.length]);
                for (int v = 0; v < VERDICT.length; v++) c[v] += row[r.idx][v];
            }
            o.println("CATEGORIES (pass/context/fail/other):");
            StringBuilder cl = new StringBuilder();
            int col = 0;
            for (Map.Entry<String, int[]> e : cats.entrySet()) {
                int[] c = e.getValue();
                int n = 0; for (int v : c) n += v;
                if (n == 0) continue;
                cl.append(String.format(Locale.US, "  %-16s %5d/%d/%d/%d", e.getKey(), c[PASS], c[CONTEXT], c[FAIL], c[UNMATCHED] + c[BADCODE] + c[EVALERR]));
                if (++col % 3 == 0) cl.append('\n');
            }
            o.println(cl.toString().replaceAll("\n$", ""));
            // the templates that fail most
            List<Row> bad = new ArrayList<Row>();
            for (Row r : ROWS) if (row[r.idx][FAIL] + row[r.idx][UNMATCHED] + row[r.idx][EVALERR] + row[r.idx][BADCODE] > 0) bad.add(r);
            bad.sort((a, b) -> {
                int fa = row[a.idx][FAIL] + row[a.idx][UNMATCHED] + row[a.idx][EVALERR] + row[a.idx][BADCODE], fb = row[b.idx][FAIL] + row[b.idx][UNMATCHED] + row[b.idx][EVALERR] + row[b.idx][BADCODE];
                return fb != fa ? fb - fa : a.idx - b.idx;
            });
            int shown = full ? bad.size() : Math.min(top, bad.size());
            o.println("FAILING TEMPLATES (" + bad.size() + " of " + ROWS.size() + " rows fail at least once; " + (full ? "all" : "top " + shown) + "): fails/n rate  row  expect  sentence  -> got (example seed)");
            for (int i = 0; i < shown; i++) {
                Row r = bad.get(i);
                int[] c = row[r.idx];
                int n = 0; for (int v : c) n += v;
                int f = c[FAIL] + c[UNMATCHED] + c[EVALERR] + c[BADCODE];
                String what = c[BADCODE] > 0 ? "BADCODE " + r.bad : (rowGot[r.idx] == null ? "" : rowGot[r.idx]);
                if (!full && what.length() > 110) what = what.substring(0, 110) + "…";
                o.printf(Locale.US, "  %4d/%-4d %3.0f%% %s %s %s  %s  -> %s (seed %d)%n", f, n, 100.0 * f / n, r.id(), r.lang, r.expect, r.sentence, what, rowSeed[r.idx]);
            }
            if (full) {
                o.println("CONTEXT-ONLY TEMPLATES (judged CONTEXT at least once): n context row sentence (example seed)");
                for (Row r : ROWS) if (row[r.idx][CONTEXT] > 0) {
                    int n = 0; for (int v : row[r.idx]) n += v;
                    o.printf(Locale.US, "  %4d %4d %s %s %s (seed %d)%n", n, row[r.idx][CONTEXT], r.id(), r.expect, r.sentence, rowCtxSeed[r.idx]);
                }
                o.println("ROWS NEVER USED:");
                for (Row r : ROWS) { int n = 0; for (int v : row[r.idx]) n += v; if (n == 0) o.println("  " + r.id() + " " + r.sentence); }
            }
        }

        double q(double p) { return ms.isEmpty() ? 0 : ms.get((int) Math.min(ms.size() - 1, Math.round(p * (ms.size() - 1)))); }
    }

    static String verdictLine(int[] v) {
        int n = 0; for (int x : v) n += x;
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < v.length; i++) { if (i > 0) b.append(", "); b.append(VERDICT[i].toLowerCase(Locale.ROOT)).append(' ').append(v[i]).append(String.format(Locale.US, " (%.1f%%)", 100.0 * v[i] / Math.max(1, n))); }
        return b.toString();
    }

    static List<Map.Entry<String, Integer>> sortDesc(Map<String, Integer> m) {
        List<Map.Entry<String, Integer>> l = new ArrayList<Map.Entry<String, Integer>>(m.entrySet());
        l.sort((a, b) -> b.getValue() - a.getValue());
        return l;
    }

    // ================================================================== main

    public static void main(String[] a) throws Exception {
        if (a.length < 3) {
            System.err.println("usage: StoryForge <pool.tsv,...> run <fromSeed> <count> | write <fromSeed> <count> <dir> | one <seed>");
            System.exit(2);
        }
        load(a[0]);
        String mode = a[1];
        switch (mode) {
            case "one": {
                long seed = Long.parseLong(a[2]);
                Gen g = generate(seed);
                System.out.println(g.script);
                System.out.println("==== seed " + seed + ": " + g.dirs.size() + " directions, " + g.lines.size() + " lines");
                DUMP = System.getenv("DUMP") != null;
                Result r = check(seed, true);
                for (String n : LOAD_NOTES) System.out.println("NOTE " + n);
                if (r.crash != null) System.out.println("CRASH " + r.crashTrace);
                if (!r.inv.isEmpty()) System.out.println("INVARIANTS " + r.inv + " " + r.invNote);
                for (int i = 0; i < r.dirs.size(); i++) {
                    DirRes d = r.dirs.get(i);
                    Row row = ROWS.get(d.row);
                    System.out.printf(Locale.US, "%-9s [%.2f..%.2f] %s %s  %s%s%n", VERDICT[d.verdict], d.tA, d.tB, row.expect, g.dirs.get(i).text,
                            row.id(), d.got == null ? "" : "  -> " + d.got);
                    if (d.run != null && (System.getenv("DUMP").equals("all") || d.verdict != PASS)) PhenomenaProbe.dump(d.run);
                }
                System.out.printf(Locale.US, "%.0f ms, %.1f s of film%n", r.ms, r.filmSec);
                break;
            }
            case "write": {
                long from = Long.parseLong(a[2]);
                int count = Integer.parseInt(a[3]);
                Path dir = Paths.get(a[4]);
                Files.createDirectories(dir);
                for (int i = 0; i < count; i++) {
                    Gen g = generate(from + i);
                    Files.write(dir.resolve(String.format(Locale.US, "story_%05d.txt", i + 1)), g.script.getBytes(StandardCharsets.UTF_8));
                }
                System.out.println("wrote " + count + " stories (seeds " + from + ".." + (from + count - 1) + ") to " + dir);
                break;
            }
            case "run": {
                long from = Long.parseLong(a[2]);
                int count = Integer.parseInt(a[3]);
                int threads = System.getenv("THREADS") != null ? Integer.parseInt(System.getenv("THREADS")) : 3;
                int top = System.getenv("TOP") != null ? Integer.parseInt(System.getenv("TOP")) : 25;
                Agg agg = new Agg();
                long w0 = System.nanoTime();
                ExecutorService ex = Executors.newFixedThreadPool(threads);
                int batch = 240, every = Math.max(1000, count / 50);
                int done = 0;
                for (int s = 0; s < count; s += batch) {
                    List<Future<Result>> fs = new ArrayList<Future<Result>>();
                    for (int i = s; i < Math.min(count, s + batch); i++) { final long seed = from + i; fs.add(ex.submit(() -> check(seed, true))); }
                    for (Future<Result> f : fs) {
                        agg.add(f.get());
                        if (++done % every == 0)
                            System.err.printf(Locale.US, "progress %d/%d  %.1f s  crashed %d  fail %d/%d directions%n", done, count, (System.nanoTime() - w0) / 1e9, agg.crashed, agg.verdicts[FAIL], agg.dirs);
                    }
                }
                ex.shutdown();
                double wall = (System.nanoTime() - w0) / 1e9;
                agg.print(System.out, top, false, wall, threads);
                String out = System.getenv("OUT");
                if (out != null) {
                    try (PrintStream ps = new PrintStream(Files.newOutputStream(Paths.get(out)), true, "UTF-8")) {
                        ps.println("StoryForge " + String.join(" ", a));
                        agg.print(ps, top, true, wall, threads);
                    }
                    System.out.println("full report: " + out);
                }
                break;
            }
            default:
                System.err.println("unknown mode " + mode);
                System.exit(2);
        }
    }
}
