package com.tarun.kahani.core;

/** Painted backgrounds ("sets") for scenes, in a 1280x720 virtual stage. */
public final class Sets {
    private Sets() {}

    public static final float W = 1280, H = 720, GROUND = 640;

    public static final int GARDEN = 0, COURTYARD = 1, GATE = 2, CAVE_IN = 3, CAVE_MOUTH = 4, FOREST = 5,
            CELEBRATION = 6, HALL = 7, VILLAGE = 8, GENERIC_OUT = 9,
            ROOFTOP = 10,     // a city rooftop or terrace: skyline, glass towers, planters
            BASEMENT = 11,    // a dark basement, a closed mall, a server room: machines, a flickering tubelight
            ROOM = 12,        // a room of today: a home, a classroom, an office
            STREET = 13;      // a city street
    public static final int MORNING = 0, DAY = 1, EVENING = 2, NIGHT = 3;

    /** Sets under the open sky (weather, stars and fireflies belong there). */
    public static boolean outdoorSet(int set) {
        return set == GARDEN || set == FOREST || set == VILLAGE || set == COURTYARD || set == GATE || set == CELEBRATION || set == CAVE_MOUTH || set == GENERIC_OUT
                || set == ROOFTOP || set == STREET;
    }

    /** English name of a set for the app's screens (name() stays Hindi: it is also used to match sounds). */
    public static String label(int set) {
        switch (set) {
            case GARDEN: return "garden";
            case COURTYARD: return "courtyard";
            case GATE: return "main gate";
            case CAVE_IN: return "inside a cave";
            case CAVE_MOUTH: return "cave entrance";
            case FOREST: return "forest";
            case CELEBRATION: return "celebration";
            case HALL: return "palace hall";
            case VILLAGE: return "village";
            case ROOFTOP: return "city rooftop";
            case BASEMENT: return "dark basement";
            case ROOM: return "room";
            case STREET: return "city street";
            default: return "open place";
        }
    }

    public static String name(int set) {
        switch (set) {
            case GARDEN: return "बगीचा";
            case COURTYARD: return "प्रांगण";
            case GATE: return "मुख्य द्वार";
            case CAVE_IN: return "गुफा (अंदर)";
            case CAVE_MOUTH: return "गुफा का मुहाना";
            case FOREST: return "जंगल";
            case CELEBRATION: return "उत्सव";
            case HALL: return "महल (अंदर)";
            case VILLAGE: return "गाँव";
            case ROOFTOP: return "शहर की छत";
            case BASEMENT: return "अँधेरा तहखाना";
            case ROOM: return "कमरा";
            case STREET: return "शहर की सड़क";
            default: return "खुला स्थान";
        }
    }

    /** Guesses the set from a free-text location description. */
    /** v34: words of open land (a battlefield, mountains, a valley, a desert, fields) — a place of its own, though drawn as the open place. */
    public static boolean openLand(String text) {
        return Txt.has(text, "युद्ध का मैदान", "युद्धभूमि", "युद्ध भूमि", "रणभूमि", "रणक्षेत्र", "पहाड़", "पर्वत", "घाटी", "रेगिस्तान", "मरुस्थल", "खेत")
                // v40: an open, empty field out of town ("शहर से बाहर एकदम खुला और सुनसान मैदान") is open land, not a courtyard
                || (Txt.has(text, "मैदान") && Txt.has(text, "खुला", "खुले", "सुनसान", "वीरान", "शहर से बाहर", "दूर दूर तक", "दूर-दूर तक"))
                || Txt.hasWord(text, "battlefield", "battleground", "mountain", "mountains", "hill", "hills", "valley", "desert", "meadow", "field", "fields");
    }

    public static int detect(String text) {
        if (Txt.has(text, "गुफा के अंदर", "गुफा में", "inside the cave", "अपनी गुफा")) return CAVE_IN;
        if (Txt.has(text, "मुहान", "गुफा के बाहर", "गुफा के दरवाज", "cave entrance")) return CAVE_MOUTH;
        // places of today, before the old ones ("rooftop garden" is a rooftop, not the palace garden)
        // (English words as whole words: a "small living room" is no "mall", a "proof" no "roof")
        // v45: a shopping mall open for the day is a bright room of today (shops, an escalator); the closed one stays dark
        if ((Txt.has(text, "मॉल") || Txt.hasWord(text, "mall", "malls")) && (Txt.has(text, "शॉपिंग", "शहर का मॉल", "दुकानों", "एस्केलेटर") || Txt.hasWord(text, "shopping", "shops", "escalator", "the mall"))
                && !Txt.has(text, "बंद", "बेसमेंट", "तहखान") && !Txt.hasWord(text, "closed", "abandoned", "basement", "dark")) return ROOM;
        if (Txt.has(text, "बेसमेंट", "तहखान", "मॉल", "सर्वर रूम", "पार्किंग", "गोदाम", "गैराज", "फैक्ट्री", "कारखान", "लैब")
                || Txt.hasWord(text, "basement", "basements", "mall", "malls", "server room", "parking", "warehouse", "garage", "factory", "arcade", "bunker",
                "lab", "laboratory")) return BASEMENT;
        boolean roof = Txt.has(text, "छत पर", "की छत", "टैरेस", "बालकनी", "स्काई-लाइन", "स्काईलाइन", "20वें फ्लोर")
                || Txt.hasWord(text, "rooftop", "rooftops", "roof", "roofs", "terrace", "balcony", "high-rise", "highrise", "skyline", "20th floor");
        boolean building = Txt.has(text, "सोसाइटी", "अपार्टमेंट", "टावर", "बिल्डिंग") || Txt.hasWord(text, "society", "apartment", "apartments", "tower", "towers", "building", "buildings");
        // v41: a housing society's park, lane or road is that park, lane or road ("सोसाइटी का पार्क", "सोसाइटी की गली",
        // "सोसाइटी के बाहर की सड़क"); a building with nothing on the ground named is its rooftop
        if (roof || (building && !Txt.has(text, "पार्क", "बगीच", "गली", "सड़क", "रोड", "कमरा", "कमरे", "लिविंग रूम", "फ्लैट", "घर के अंदर", "बाज़ार", "बाजार")
                && !Txt.hasWord(text, "park", "garden", "playground", "lane", "street", "road", "room", "flat", "inside", "market"))) return ROOFTOP;
        if (Txt.has(text, "गुफा", "cave", "कैदखान")) return CAVE_IN;
        if (Txt.has(text, "जंगल", "वन ", "forest", "jungle", "woods")) return FOREST;
        if (Txt.has(text, "सजा", "उत्सव", "जश्न", "रोशनियों", "ढोल", "celebration", "festival")) return CELEBRATION;
        // v34: open land — a battlefield, mountains, a valley, a desert — unless a built place is named with it
        // ("पहाड़ों के बीच युद्ध का मैदान" is no palace courtyard; "पहाड़ी पर महल का द्वार" stays the gate)
        if (openLand(text) && !Txt.has(text, "महल", "palace", "द्वार", "gate", "आँगन", "प्रांगण", "courtyard", "बगीच", "garden", "दरबार", "hall", "गाँव", "village", "मंदिर", "temple"))
            return GENERIC_OUT;
        if (Txt.has(text, "मुख्य द्वार", "द्वार", "गेट", "फाटक", "gate")) return GATE;
        if (Txt.has(text, "प्रांगण", "आँगन", "मैदान", "courtyard", "arena")) return COURTYARD;
        if (Txt.has(text, "बगीच", "बाग", "उद्यान", "वाटिका", "फुलवारी", "garden", "पार्क") || Txt.hasWord(text, "park", "parks", "playground")) return GARDEN;   // v37: a park, a playground
        if (Txt.has(text, "दरबार", "सिंहासन", "throne", "palace hall", "महल के अंदर", "राजमहल", "दरबार") || (Txt.has(text, "महल", "palace") && Txt.has(text, "कक्ष", "कमरे", "hall", "room"))) return HALL;
        if (Txt.has(text, "बाथरूम", "बाथ रूम", "स्नानघर", "स्नानागार", "गुसलखाना", "ग़ुसलखाना") || Txt.hasWord(text, "bathroom", "washroom")) return ROOM;     // v37: a bathroom is indoors
        // v38: a home's rooms written in Hindi letters ("घर का लिविंग रूम", "ड्राइंग रूम", "बैठक", "किचन"), a sofa, "घर में"
        if (Txt.has(text, "लिविंग रूम", "लिविंगरूम", "ड्राइंग रूम", "ड्रॉइंग रूम", "बेडरूम", "बेड रूम", "बैठक", "किचन", "डाइनिंग", "सोफ़े", "सोफे", "सोफ़ा", "सोफा",
                "घर का", "घर की", "घर में") && !Txt.has(text, "घर के बाहर", "घर के आँगन", "घर के आंगन", "घर का आँगन", "घर का आंगन", "घर की छत", "बगीच", "garden")
                || Txt.hasWord(text, "drawing room", "sofa", "couch", "dining room")) return ROOM;
        if (Txt.has(text, "कमरा", "कमरे", "कक्ष", "ऑफिस", "दफ़्तर", "दफ्तर", "कक्षा", "स्कूल", "क्लास", "रसोई", "अस्पताल", "दुकान", "पुस्तकालय", "घर के अंदर", "स्टूडियो")
                || Txt.hasWord(text, "living room", "bedroom", "bedrooms", "office", "offices", "classroom", "classrooms", "school", "schools", "kitchen", "hospital",
                "shop", "shops", "library", "inside the house", "indoors", "studio", "hall", "halls", "room", "rooms", "flat", "flats")) return ROOM;
        if (Txt.has(text, "सड़क", "गली", "चौराहा", "स्टेशन", "शहर", "मुंबई", "दिल्ली")
                || Txt.hasWord(text, "street", "streets", "road", "roads", "lane", "lanes", "traffic", "crossing", "station", "bus stop", "city", "mumbai", "delhi",
                "metro")) return STREET;
        if (Txt.has(text, "गाँव", "गांव", "बाज़ार", "village", "market")) return VILLAGE;
        if (Txt.has(text, "महल", "palace")) return GARDEN;
        return GENERIC_OUT;
    }

    /**
     * v34: the one place of a scene, the same for the director, the scene plan, the 3D place maker and the scene card:
     * the setting's first sentence (later sentences mention other places), then the whole setting; a title that names
     * the place more surely wins over a bare "palace" (read as its garden) or an open place ("स्वर्ग का दरबार" with
     * "स्वर्ग का सुनहरा महल" is the hall).
     */
    public static int forScene(Story.Scene sc) {
        String setting = sc.setting == null ? "" : sc.setting, title = sc.title == null ? "" : sc.title;
        String first = Director.firstSentence(setting.length() > 0 ? setting : title);
        int s = detect(first);
        if (s == GENERIC_OUT && setting.length() > 0) s = detect(setting);
        if (title.length() > 0 && setting.length() > 0 && (s == GENERIC_OUT || (s == GARDEN && !Txt.has(first, "बगीच", "बाग", "उद्यान", "वाटिका", "फुलवारी", "garden")))) {
            int t = detect(title);
            if (t != GENERIC_OUT && t != GARDEN) s = t;
        }
        return s;
    }

    public static int detectTime(String text, int fallback) {
        if (Txt.has(text, "सूर्योदय", "सुबह", "भोर", "sunrise", "morning")) return MORNING;
        if (Txt.has(text, "रात", "अँधेरा घिर", "night", "midnight")) return NIGHT;
        if (Txt.has(text, "शाम", "सूर्यास्त", "evening", "sunset", "संध्या")) return EVENING;
        if (Txt.has(text, "दोपहर", "दिन", "noon", "afternoon")) return DAY;
        return fallback;
    }

    public static boolean indoorDark(int set) { return set == CAVE_IN || set == BASEMENT; }

    static int lerp(int a, int b, float t) { return Puppet.mix(a, b, t); }

    // ------------------------------------------------------------------ static layer

    /**
     * v35: the trees of a painted place sway in the wind, so they are drawn live, every frame (paintLive), and left out
     * of the cached still layer: true while the still layer of such a place is painted (per drawing thread).
     */
    private static final ThreadLocal<boolean[]> STILL_LAYER = new ThreadLocal<boolean[]>() {
        protected boolean[] initialValue() { return new boolean[1]; }
    };

    /** v35: the trees of each painted place: {x, foot y, size}, or null for a place without trees. */
    static float[][] trees(int set) {
        switch (set) {
            case GARDEN: case CELEBRATION: return new float[][]{{70, 560, 1.25f}, {1210, 560, 1.15f}};
            case GATE: return new float[][]{{130, 560, 1.3f}, {1160, 560, 1.2f}};
            case VILLAGE: case GENERIC_OUT: return new float[][]{{140, 560, 1.1f}, {1120, 560, 1.0f}};
            case CAVE_MOUTH: return new float[][]{{140, 600, 1.1f}};
            default: return null;
        }
    }

    // ------------------------------------------------------------------ v45: what makes a place itself

    /** v45: the details of a place drawn over its set (a metro platform over the street, desks over the room …). */
    public static final int D_NONE = 0, D_METRO = 1, D_OFFICE = 2, D_HOSPITAL = 3, D_CLASS = 4, D_MALL = 5, D_AIRPORT = 6, D_GYM = 7, D_CAFE = 8,
            D_WELL = 9, D_FIELDS = 10, D_POTTER = 11, D_TEMPLE = 12, D_GHAT = 13, D_CAMP = 14, D_BAZAAR = 15;

    /** v45: the place's own details, from its name or its first sentence. */
    public static int decorFor(String text) {
        if (text == null) return D_NONE;
        if (Txt.has(text, "मेट्रो", "प्लेटफ़ॉर्म", "प्लेटफॉर्म", "रेलवे स्टेशन") || Txt.hasWord(text, "metro", "platform", "railway station", "train station", "subway")) return D_METRO;
        if (Txt.has(text, "हवाई अड्डा", "हवाई अड्डे", "एयरपोर्ट") || Txt.hasWord(text, "airport", "terminal")) return D_AIRPORT;
        if (Txt.has(text, "अस्पताल", "हॉस्पिटल", "दवाखान", "क्लिनिक", "वार्ड") || Txt.hasWord(text, "hospital", "ward", "clinic")) return D_HOSPITAL;
        if (Txt.has(text, "दफ़्तर", "दफ्तर", "ऑफिस", "ऑफ़िस") || Txt.hasWord(text, "office", "offices")) return D_OFFICE;
        if (Txt.has(text, "कक्षा", "क्लास", "स्कूल") || Txt.hasWord(text, "classroom", "class", "school")) return D_CLASS;
        if (Txt.has(text, "मॉल") || Txt.hasWord(text, "mall", "malls")) return D_MALL;
        if (Txt.has(text, "जिम") || Txt.hasWord(text, "gym", "gymnasium")) return D_GYM;
        if (Txt.has(text, "रेस्टोरेंट", "रेस्तरां", "कैफ़े", "कैफे", "ढाबा") || Txt.hasWord(text, "restaurant", "cafe", "café", "dhaba", "diner")) return D_CAFE;
        if (Txt.has(text, "घाट") || Txt.hasWord(text, "ghat", "ghats", "riverbank", "river bank")) return D_GHAT;
        if (Txt.has(text, "मंदिर", "शिवालय", "देवालय") || Txt.hasWord(text, "temple", "shrine")) return D_TEMPLE;
        if (Txt.has(text, "कुआँ", "कुएँ", "कुआं", "कुएं") || Txt.hasWord(text, "well")) return D_WELL;
        if (Txt.has(text, "कुम्हार", "चाक") || Txt.hasWord(text, "potter", "potter's", "pottery")) return D_POTTER;
        if (Txt.has(text, "युद्ध", "रणभूमि", "छावनी", "शिविर") || Txt.hasWord(text, "battlefield", "battle", "camp", "war camp")) return D_CAMP;
        if (Txt.has(text, "बाज़ार", "बाजार", "हाट", "मंडी") || Txt.hasWord(text, "bazaar", "market", "marketplace")) return D_BAZAAR;
        if (Txt.has(text, "खेत", "खलिहान") || Txt.hasWord(text, "fields", "field", "farm", "farmland")) return D_FIELDS;
        return D_NONE;
    }

    /** v45: an indoor place of today is a room (a gym, an airport hall, a restaurant), an old bazaar a village's lanes, a platform the city. */
    public static int setForDecor(int set, int decor, String text) {
        switch (decor) {
            case D_OFFICE: case D_HOSPITAL: case D_CLASS: case D_MALL: case D_GYM: case D_CAFE: case D_AIRPORT:
                return set == BASEMENT && Txt.has(text, "बंद", "closed", "dark", "अँधेर") ? set : ROOM;
            case D_METRO: return STREET;
            case D_BAZAAR: return Txt.has(text, "पुरान", "old", "गाँव", "गांव", "village", "हाट") ? VILLAGE : set;
            case D_WELL: case D_FIELDS: case D_POTTER: return set == STREET || set == ROOM ? VILLAGE : set;
            default: return set;
        }
    }

    /** v45: the set and its details as one cached picture. */
    public static void paintStatic(Gfx g, int set, int tod, int decor) {
        paintStatic(g, set, tod);
        if (decor != D_NONE) paintDecor(g, decor, tod);
    }

    static void paintDecor(Gfx g, int decor, int tod) {
        boolean night = tod == NIGHT, eve = tod == EVENING, dark = night || eve;
        switch (decor) {
            case D_METRO: {
                // the station's roof, a train standing at the platform, the platform's edge and the rails before it
                g.color(dark ? 0xFF2B3140 : 0xFF6F7C8A); g.rect(0, 0, W, 70);
                for (int i = 0; i < 9; i++) { g.color(dark ? 0xFF1E2430 : 0xFF55606C); g.rect(i * 160, 70, 10, 230); }
                g.color(dark ? 0xFF8C96A6 : 0xFFD6DCE3); g.roundRect(-20, 300, W + 40, 220, 26);
                g.color(0xFF1565C0); g.rect(-20, 440, W + 40, 18);
                for (int i = 0; i < 8; i++) {
                    float x = 30 + i * 165;
                    g.color(dark ? 0xFFFFE9A8 : 0xFF2B3A4A); g.roundRect(x, 330, 110, 80, 8);
                    if (i % 3 == 1) { g.color(dark ? 0xFF5E6A7A : 0xFFAEB8C4); g.rect(x + 120, 320, 36, 190); g.color(0xFF37474F); g.rect(x + 137, 320, 2, 190); }
                }
                g.color(dark ? 0xFF3A3F48 : 0xFFB9BEC4); g.rect(0, 520, W, 170);
                g.color(0xFFFFD54F); g.rect(0, 676, W, 10);
                g.color(0xFF2E2A26); g.rect(0, 686, W, 34);
                g.color(0xFF9E9E9E); g.rect(0, 696, W, 4); g.rect(0, 712, W, 4);
                // the display board
                g.color(0xFF101418); g.roundRect(520, 90, 240, 60, 6);
                g.color(0xFFFFB300); g.text("METRO  2 min", 640, 130, 26, true, 1);
                break;
            }
            case D_OFFICE: {
                wallOver(g, dark, 0xFFE8EEF2, 0xFF39414E);
                // a glass partition, desks with screens, a clock
                g.color(dark ? 0x3390CAF9 : 0x5590CAF9); g.rect(420, 140, 360, 380);
                g.color(dark ? 0xFF607080 : 0xFFB0BEC5); g.rect(420, 140, 360, 6); g.rect(598, 140, 4, 380);
                for (int i = 0; i < 2; i++) {
                    float x = 60 + i * 180;
                    g.color(0xFF8D6E63); g.rect(x, 440, 160, 14); g.rect(x + 8, 454, 10, 80); g.rect(x + 142, 454, 10, 80);
                    g.color(0xFF263238); g.roundRect(x + 40, 370, 80, 56, 4); g.rect(x + 76, 426, 8, 14);
                    g.color(dark ? 0xFF4FC3F7 : 0xFF81D4FA); g.rect(x + 45, 375, 70, 46);
                }
                clock(g, 1000, 80);
                break;
            }
            case D_HOSPITAL: {
                wallOver(g, dark, 0xFFE3F2EC, 0xFF34433E);
                // a drip on its stand, a curtain on its rail, a cabinet with a red cross
                g.color(0xFF90A4AE); g.rect(330, 300, 5, 300); g.rect(305, 300, 55, 4); g.oval(332, 600, 26, 6);
                g.color(0xCCE1F5FE); g.roundRect(312, 306, 22, 40, 6); g.color(0xFF90A4AE); g.line(323, 346, 323, 420, 2);
                g.color(0xFF9E9E9E); g.rect(40, 96, 300, 6);
                g.color(dark ? 0xFF5D7A8A : 0xFF9FD3E6); g.rect(46, 102, 120, 400);
                for (int i = 0; i < 6; i++) { g.color(dark ? 0xFF4C6878 : 0xFF86C4DA); g.rect(52 + i * 20, 102, 6, 400); }
                g.color(0xFFF5F5F5); g.roundRect(200, 180, 110, 130, 6);
                g.color(0xFFE53935); g.rect(246, 200, 18, 60); g.rect(225, 221, 60, 18);
                floorTiles(g, dark, 0xFFDDE6E3, 0xFF4A5551);
                break;
            }
            case D_CLASS: {
                wallOver(g, dark, 0xFFF3E9D2, 0xFF3E3A33);
                g.color(0xFF6D4C41); g.rect(372, 132, 456, 236);
                g.color(0xFF2E4A3A); g.rect(382, 142, 436, 216);
                g.color(0xCCFFFFFF); g.text("2 + 3 = 5", 600, 230, 40, false, 1); g.text("अ आ इ ई", 600, 300, 34, false, 1);
                for (int i = 0; i < 2; i++) {
                    float x = 70 + i * 170;
                    g.color(0xFF8D6E63); g.rect(x, 450, 140, 14); g.rect(x + 8, 464, 10, 70); g.rect(x + 122, 464, 10, 70);
                    g.color(0xFF6D4C41); g.rect(x - 4, 500, 148, 10);
                }
                clock(g, 1000, 80);
                break;
            }
            case D_MALL: {
                wallOver(g, dark, 0xFFF4F1EC, 0xFF3A3640);
                int[] sh = {0xFFE91E63, 0xFF3F51B5, 0xFF009688, 0xFFFF9800};
                for (int i = 0; i < 4; i++) {
                    float x = 20 + i * 320;
                    g.color(sh[i]); g.rect(x, 130, 280, 40);
                    g.color(dark ? 0xFF2C3440 : 0xFFCFE8F2); g.rect(x, 170, 280, 330);
                    g.color(0xFFB0BEC5); g.rect(x + 138, 170, 4, 330);
                    for (int m = 0; m < 2; m++) { g.color(sh[(i + m + 1) % 4]); g.oval(x + 70 + m * 140, 250, 14, 14); g.roundRect(x + 56 + m * 140, 266, 28, 90, 8); }
                }
                g.color(dark ? 0xFF9E9E9E : 0xFFE0E0E0); g.begin(); g.moveTo(1000, 520); g.lineTo(1280, 300); g.lineTo(1280, 340); g.lineTo(1040, 520); g.close(); g.fillPath();
                floorTiles(g, dark, 0xFFF0EEEA, 0xFF55525A);
                break;
            }
            case D_AIRPORT: {
                // a wall of glass with a plane on the apron, the departures board, a counter
                g.color(dark ? 0xFF1C2638 : 0xFFAEDCF5); g.rect(0, 80, W, 440);
                g.color(dark ? 0xFF3A4458 : 0xFF90A4AE); for (int i = 0; i <= 8; i++) g.rect(i * 160, 80, 8, 440); g.rect(0, 80, W, 8);
                g.color(0xFFF5F5F5); g.oval(760, 400, 260, 36); g.color(0xFF1565C0); g.rect(560, 396, 400, 8);
                g.color(0xFFF5F5F5); g.begin(); g.moveTo(760, 400); g.lineTo(700, 330); g.lineTo(740, 330); g.lineTo(840, 400); g.close(); g.fillPath();
                g.begin(); g.moveTo(980, 392); g.lineTo(1010, 330); g.lineTo(1030, 330); g.lineTo(1015, 396); g.close(); g.fillPath();
                g.color(0xFF101418); g.roundRect(80, 110, 300, 120, 6);
                g.color(0xFFFFB300); g.text("DEL  10:40", 230, 150, 24, true, 1); g.text("BOM  11:15", 230, 185, 24, true, 1); g.text("BLR  12:05", 230, 218, 24, true, 1);
                g.color(0xFF607D8B); g.roundRect(860, 470, 360, 70, 6); g.color(0xFF37474F); g.rect(860, 470, 360, 10);
                floorTiles(g, dark, 0xFFEDEDED, 0xFF4E4E55);
                break;
            }
            case D_GYM: {
                wallOver(g, dark, 0xFFE0E0E0, 0xFF33363C);
                g.color(dark ? 0x553E5060 : 0x88B3E5FC); g.rect(420, 120, 440, 380);
                g.color(0xFF424242); g.rect(60, 440, 260, 10); g.rect(70, 450, 8, 90); g.rect(300, 450, 8, 90);
                for (int i = 0; i < 5; i++) { float x = 90 + i * 46; g.color(0xFF212121); g.oval(x, 432, 10, 10); g.oval(x + 26, 432, 10, 10); g.rect(x, 429, 26, 6); }
                g.color(0xFF37474F); g.begin(); g.moveTo(960, 530); g.lineTo(1180, 530); g.lineTo(1160, 500); g.lineTo(980, 500); g.close(); g.fillPath();
                g.rect(1150, 400, 10, 110); g.roundRect(1120, 380, 60, 26, 4);
                floorTiles(g, dark, 0xFF6D6D6D, 0xFF2E2E2E);
                break;
            }
            case D_CAFE: {
                wallOver(g, dark, 0xFFF1E3CF, 0xFF3D3128);
                g.color(0xFF2E2E2E); g.roundRect(80, 140, 220, 160, 6);
                g.color(0xFFFFFFFF); g.text("MENU", 190, 180, 26, true, 1); g.text("चाय  20", 190, 220, 22, false, 1); g.text("कॉफ़ी 40", 190, 255, 22, false, 1);
                for (int i = 0; i < 3; i++) {
                    float x = 420 + i * 300;
                    g.color(0xFF5D4037); g.line(x, 0, x, 120, 2);
                    g.color(0xFFFFCC80); g.begin(); g.moveTo(x - 30, 150); g.lineTo(x + 30, 150); g.lineTo(x + 16, 120); g.lineTo(x - 16, 120); g.close(); g.fillPath();
                    if (dark) { g.radial(x, 160, 110, 0x66FFE0A0, 0x00FFE0A0); g.oval(x, 160, 110, 110); }
                }
                for (int i = 0; i < 2; i++) { float x = 140 + i * 200; g.color(0xFFC62828); g.oval(x, 470, 70, 14); g.color(0xFF6D4C41); g.rect(x - 5, 476, 10, 60); }
                break;
            }
            case D_WELL: {
                // a stone well with its frame, pulley, rope and bucket
                float x = 980;
                g.color(0xFF9E9A92); g.rect(x - 90, 520, 180, 90);
                g.color(0xFF7D786F); for (int r = 0; r < 3; r++) for (int c = 0; c < 4; c++) g.rect(x - 86 + c * 45 + (r % 2) * 20, 524 + r * 30, 38, 24);
                g.color(0xFF5A564F); g.oval(x, 520, 90, 18); g.color(0xFF1E2A30); g.oval(x, 520, 76, 12);
                g.color(0xFF6D4C41); g.rect(x - 84, 380, 12, 140); g.rect(x + 72, 380, 12, 140); g.rect(x - 90, 372, 180, 12);
                g.color(0xFF4E342E); g.oval(x, 396, 14, 14);
                g.color(0xFF8D6E63); g.line(x, 396, x, 470, 2);
                g.color(0xFF78909C); g.begin(); g.moveTo(x - 16, 470); g.lineTo(x + 16, 470); g.lineTo(x + 12, 500); g.lineTo(x - 12, 500); g.close(); g.fillPath();
                break;
            }
            case D_FIELDS: {
                // rows of wheat to the far edge, a scarecrow
                for (int r = 0; r < 6; r++) {
                    float y = 460 + r * 14;
                    g.color(r % 2 == 0 ? (dark ? 0xFF8A7330 : 0xFFE6C35C) : (dark ? 0xFF6E5B26 : 0xFFD4AE44));
                    g.rect(0, y, W, 12);
                }
                g.color(0xFF6D4C41); g.rect(1150, 360, 8, 160); g.rect(1110, 400, 90, 8);
                g.color(0xFFE53935); g.rect(1132, 404, 44, 50); g.color(0xFFFFE0B2); g.oval(1154, 384, 18, 18);
                g.color(0xFF8D6E63); g.begin(); g.moveTo(1126, 372); g.lineTo(1182, 372); g.lineTo(1154, 352); g.close(); g.fillPath();
                break;
            }
            case D_POTTER: {
                float x = 1000;
                for (int i = 0; i < 5; i++) { float px = 860 + i * 60, sz = 26 + (i % 2) * 8; g.color(i % 2 == 0 ? 0xFFB5651D : 0xFFA0522D); g.oval(px, 560 - sz, sz, sz * 1.1f); g.rect(px - sz * 0.35f, 560 - sz * 2.05f, sz * 0.7f, 10); }
                g.color(0xFF5D4037); g.oval(x - 260, 600, 70, 14); g.color(0xFF8D6E63); g.oval(x - 260, 594, 54, 10);
                g.color(0xFFB5651D); g.begin(); g.moveTo(x - 280, 594); g.lineTo(x - 240, 594); g.lineTo(x - 246, 560); g.lineTo(x - 274, 560); g.close(); g.fillPath();
                break;
            }
            case D_TEMPLE: {
                // a shikhara behind, its steps, a flag on top, a bell hung at the side
                float x = 640;
                g.color(dark ? 0xFFB08A60 : 0xFFF0C890); g.rect(x - 200, 330, 400, 190);
                g.color(dark ? 0xFF9A7650 : 0xFFE3B577); g.begin(); g.moveTo(x - 150, 330); g.quadTo(x - 120, 160, x, 110); g.quadTo(x + 120, 160, x + 150, 330); g.close(); g.fillPath();
                g.color(dark ? 0xFF7A5C3A : 0xFFC9975A); for (int i = 0; i < 5; i++) g.rect(x - 140 + i * 6, 180 + i * 30, 280 - i * 12, 6);
                g.color(0xFF5D4037); g.rect(x - 50, 400, 100, 120); g.color(0xFFFFB300); g.oval(x, 120, 10, 10);
                g.color(0xFF6D4C41); g.rect(x - 2, 60, 4, 60); g.color(0xFFFF6F00); g.begin(); g.moveTo(x + 2, 60); g.lineTo(x + 52, 74); g.lineTo(x + 2, 88); g.close(); g.fillPath();
                for (int i = 0; i < 3; i++) { g.color(i % 2 == 0 ? 0xFFD7CCC8 : 0xFFBCAAA4); g.rect(x - 230 - i * 20, 520 + i * 10, 460 + i * 40, 10); }
                g.color(0xFF6D4C41); g.rect(220, 300, 80, 8); g.color(0xFFC9A227); g.begin(); g.moveTo(246, 308); g.lineTo(274, 308); g.lineTo(282, 346); g.lineTo(238, 346); g.close(); g.fillPath();
                break;
            }
            case D_GHAT: {
                // the river behind, a far bank with small shrines, the stone steps coming down to it
                g.linear(0, 400, 0, 520, dark ? 0xFF2A3E5C : 0xFF6FA8C8, dark ? 0xFF1C2A40 : 0xFF4E86A6); g.rect(0, 400, W, 120);
                g.color(dark ? 0x55FFE0A0 : 0x66FFFFFF); for (int i = 0; i < 10; i++) g.rect(60 + i * 128, 430 + (i % 3) * 22, 60, 3);
                g.color(dark ? 0xFF3E4A3A : 0xFF8BA67A); g.rect(0, 380, W, 22);
                for (int i = 0; i < 4; i++) { float x = 160 + i * 300; g.color(dark ? 0xFF8A6E50 : 0xFFE0C090); g.rect(x - 20, 350, 40, 32); g.begin(); g.moveTo(x - 24, 352); g.lineTo(x, 320); g.lineTo(x + 24, 352); g.close(); g.fillPath(); }
                for (int i = 0; i < 6; i++) { g.color(i % 2 == 0 ? (dark ? 0xFF7A7064 : 0xFFCDBFAA) : (dark ? 0xFF6A6056 : 0xFFB8A992)); g.rect(0, 520 + i * 34, W, 34); }
                if (dark) for (int i = 0; i < 5; i++) { float x = 200 + i * 210; g.radial(x, 470, 30, 0xAAFFC107, 0x00FFC107); g.oval(x, 470, 30, 30); g.color(0xFFFFE082); g.oval(x, 470, 4, 3); }
                break;
            }
            case D_CAMP: {
                // tents and flags across the field, dust
                for (int i = 0; i < 5; i++) {
                    float x = 90 + i * 270, w = 140 - (i % 2) * 30;
                    g.color(i % 2 == 0 ? (dark ? 0xFF8C7A5C : 0xFFE8D8B0) : (dark ? 0xFF7A5C4C : 0xFFD9B79A));
                    g.begin(); g.moveTo(x - w / 2, 480); g.lineTo(x, 380); g.lineTo(x + w / 2, 480); g.close(); g.fillPath();
                    g.color(0x55000000); g.begin(); g.moveTo(x - 14, 480); g.lineTo(x, 430); g.lineTo(x + 14, 480); g.close(); g.fillPath();
                    g.color(0xFF5D4037); g.rect(x - 1, 340, 3, 42);
                    g.color(i % 2 == 0 ? 0xFFFF6F00 : 0xFFC62828); g.begin(); g.moveTo(x + 2, 340); g.lineTo(x + 34, 350); g.lineTo(x + 2, 360); g.close(); g.fillPath();
                }
                g.color(dark ? 0x22FFFFFF : 0x33D7CCC8); g.rect(0, 460, W, 60);
                break;
            }
            case D_BAZAAR: {
                // stalls with striped awnings, heaps of fruit and pots
                int[] aw = {0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFB8C00};
                for (int i = 0; i < 4; i++) {
                    float x = 20 + i * 320;
                    g.color(0xFF6D4C41); g.rect(x + 10, 360, 8, 160); g.rect(x + 262, 360, 8, 160);
                    for (int k = 0; k < 7; k++) { g.color(k % 2 == 0 ? aw[i] : 0xFFFFFFFF); g.begin(); g.moveTo(x + k * 40, 360); g.lineTo(x + k * 40 + 40, 360); g.lineTo(x + k * 40 + 40, 330); g.lineTo(x + k * 40, 330); g.close(); g.fillPath(); }
                    g.color(0xFF8D6E63); g.rect(x, 460, 280, 50);
                    int[] fr = {0xFFFF7043, 0xFFFFEB3B, 0xFF7CB342, 0xFFE53935};
                    for (int k = 0; k < 9; k++) { g.color(fr[(k + i) % 4]); g.oval(x + 30 + k * 28, 452 - (k % 2) * 8, 13, 13); }
                }
                break;
            }
            default:
        }
    }

    /** v45: the back wall painted again over the room's home things (sofa, shelf), its window kept. */
    static void wallOver(Gfx g, boolean dark, int day, int nightC) {
        g.color(dark ? nightC : day); g.rect(0, 0, 770, 520); g.rect(1170, 0, W - 1170, 520); g.rect(770, 0, 400, 100); g.rect(770, 400, 400, 120);
        g.color(dark ? 0x22000000 : 0x14000000); g.rect(0, 500, W, 20);
    }

    static void floorTiles(Gfx g, boolean dark, int day, int nightC) {
        g.color(dark ? nightC : day); g.rect(0, 520, W, H - 520);
        g.color(dark ? 0x22FFFFFF : 0x22000000);
        for (int i = 0; i <= 10; i++) g.line(i * 128, 520, i * 128 - 200 + i * 40, H, 1.5f);
        for (int j = 0; j < 4; j++) g.line(0, 560 + j * 45, W, 560 + j * 45, 1.5f);
    }

    static void clock(Gfx g, float x, float y) {
        g.color(0xFFFFFFFF); g.oval(x, y, 30, 30); g.color(0xFF37474F); g.strokeOval(x, y, 30, 30, 3);
        g.line(x, y, x, y - 18, 3); g.line(x, y, x + 12, y + 4, 3);
    }

    public static void paintStatic(Gfx g, int set, int tod) {
        boolean[] still = STILL_LAYER.get();
        boolean was = still[0];
        still[0] = trees(set) != null && set >= GARDEN && set <= STREET;
        try {
            paintStill(g, set, tod);
        } finally {
            still[0] = was;
        }
    }

    private static void paintStill(Gfx g, int set, int tod) {
        switch (set) {
            case CAVE_IN: caveInside(g); return;
            case CAVE_MOUTH: caveMouth(g, tod); return;
            case FOREST: forest(g, tod); return;
            case HALL: hall(g); return;
            case BASEMENT: basement(g); return;
            case ROOM: room(g, tod); return;
            case ROOFTOP: sky(g, tod); rooftop(g, tod); return;
            case STREET: sky(g, tod); street(g, tod); return;
            default:
        }
        sky(g, tod);
        hills(g, tod, set == GARDEN);        // only the palace garden has the waterfall on the far hill
        switch (set) {
            case GARDEN: palaceFar(g, tod, 0.82f); garden(g, tod); break;
            case COURTYARD: courtyard(g, tod); break;
            case GATE: gate(g, tod); break;
            case CELEBRATION: palaceFar(g, tod, 1.0f); garden(g, tod); break;
            case VILLAGE: village(g, tod); break;
            default: meadow(g, tod);
        }
    }

    static void sky(Gfx g, int tod) {
        int top, bot;
        switch (tod) {
            case MORNING: top = 0xFF7EC8F2; bot = 0xFFFFD9A8; break;
            case EVENING: top = 0xFF3B2F7A; bot = 0xFFFF9E5E; break;
            case NIGHT: top = 0xFF0B1030; bot = 0xFF27305E; break;
            default: top = 0xFF5CB3F0; bot = 0xFFCDEBFF;
        }
        g.linear(0, 0, 0, GROUND, top, bot);
        g.rect(0, 0, W, GROUND + 10);
        if (tod == MORNING) {
            g.radial(1000, 360, 260, 0x88FFE9A0, 0x00FFE9A0); g.oval(1000, 360, 260, 260);
            g.color(0xFFFFE082); g.oval(1000, 360, 62, 62);
        } else if (tod == DAY) {
            g.radial(1080, 120, 160, 0x77FFFFFF, 0x00FFFFFF); g.oval(1080, 120, 160, 160);
            g.color(0xFFFFF59D); g.oval(1080, 120, 48, 48);
        } else if (tod == EVENING) {
            g.radial(260, 420, 300, 0x99FF7043, 0x00FF7043); g.oval(260, 420, 300, 300);
            g.color(0xFFFF8A50); g.oval(260, 420, 70, 70);
        } else if (tod == NIGHT) {
            g.color(0xFFFFFDE7); g.oval(1050, 110, 40, 40);
            g.color(0xFF0B1030); g.oval(1068, 98, 36, 36);
            g.color(0xCCFFFFFF);
            long s = 99;
            for (int i = 0; i < 70; i++) {
                s = s * 6364136223846793005L + 1442695040888963407L;
                float x = ((s >>> 33) % 1280), y = ((s >>> 13) % 380);
                g.oval(x, y, 1.6f, 1.6f);
            }
        }
        if (tod != NIGHT) {
            g.color(tod == EVENING ? 0x66FFC4A3 : 0xCCFFFFFF);
            cloud(g, 220, 110, 1.0f); cloud(g, 640, 70, 0.8f); cloud(g, 880, 160, 0.6f);
        }
    }

    static void cloud(Gfx g, float x, float y, float s) {
        g.oval(x, y, 70 * s, 26 * s); g.oval(x - 45 * s, y + 8 * s, 45 * s, 20 * s); g.oval(x + 50 * s, y + 6 * s, 50 * s, 22 * s);
        g.oval(x + 10 * s, y - 16 * s, 40 * s, 24 * s);
    }

    static void hills(Gfx g, int tod) { hills(g, tod, false); }

    /** The far hills; falls = a waterfall on the far hill (only the palace garden has one). */
    static void hills(Gfx g, int tod, boolean falls) {
        int far = tod == NIGHT ? 0xFF1C2A3A : tod == EVENING ? 0xFF6A4E7A : 0xFF7FB27A;
        int near = tod == NIGHT ? 0xFF16301F : tod == EVENING ? 0xFF4F6A4A : 0xFF5E9E4E;
        g.color(far);
        g.begin(); g.moveTo(0, 420);
        g.quadTo(160, 300, 330, 380); g.quadTo(520, 250, 720, 370); g.quadTo(930, 280, 1100, 360); g.quadTo(1200, 330, 1280, 360);
        g.lineTo(1280, GROUND); g.lineTo(0, GROUND); g.close(); g.fillPath();
        // waterfall on the far hill
        if (falls && tod != NIGHT) {
            g.color(0xCCE3F6FF);
            g.rect(548, 300, 18, 120);
            g.color(0x88FFFFFF); g.oval(557, 425, 30, 8);
        }
        g.color(near);
        g.begin(); g.moveTo(0, 470);
        g.quadTo(250, 400, 520, 460); g.quadTo(820, 390, 1280, 450);
        g.lineTo(1280, GROUND); g.lineTo(0, GROUND); g.close(); g.fillPath();
    }

    static int marble(int tod) { return tod == NIGHT ? 0xFF8E96B5 : tod == EVENING ? 0xFFF2D7C8 : 0xFFF8F6F0; }

    static void palaceFar(Gfx g, int tod, float scale) {
        int m = marble(tod), sh = Puppet.shade(m, 0.82f);
        float base = 470, cx = 640;
        g.save();
        g.translate(cx, base);
        g.scale(scale, scale);
        // main body
        g.color(sh); g.rect(-300, -150, 600, 150);
        g.color(m); g.rect(-290, -150, 580, 145);
        // arches
        g.color(Puppet.shade(m, 0.7f));
        for (int i = -4; i <= 4; i++) {
            float x = i * 62;
            g.begin(); g.moveTo(x - 20, -10); g.lineTo(x - 20, -70); g.quadTo(x, -100, x + 20, -70); g.lineTo(x + 20, -10); g.close(); g.fillPath();
        }
        // central dome
        g.color(m);
        g.rect(-90, -230, 180, 85);
        g.begin(); g.moveTo(-100, -230); g.cubicTo(-100, -340, 100, -340, 100, -230); g.close(); g.fillPath();
        g.color(0xFFE5B530); g.line(0, -320, 0, -350, 4); g.oval(0, -352, 6, 6);
        // side towers with chhatris
        for (int s = -1; s <= 1; s += 2) {
            float tx = s * 270;
            g.color(m); g.rect(tx - 35, -260, 70, 260);
            g.color(sh); g.rect(tx - 35, -260, 8, 260);
            g.color(m);
            g.begin(); g.moveTo(tx - 45, -260); g.cubicTo(tx - 45, -330, tx + 45, -330, tx + 45, -260); g.close(); g.fillPath();
            g.color(0xFFE5B530); g.line(tx, -318, tx, -340, 3);
            g.color(Puppet.shade(m, 0.7f));
            for (int k = 0; k < 3; k++) g.roundRect(tx - 12, -235 + k * 70, 24, 40, 12);
            // flags
            g.color(0xFFF08A24);
            g.begin(); g.moveTo(tx, -340); g.lineTo(tx + 26, -334); g.lineTo(tx, -326); g.close(); g.fillPath();
        }
        g.color(Puppet.shade(m, 0.7f));
        for (int k = -1; k <= 1; k++) g.roundRect(k * 40 - 12, -215, 24, 50, 12);
        g.restore();
    }

    static void garden(Gfx g, int tod) {
        int grass = tod == NIGHT ? 0xFF2E4D2E : tod == EVENING ? 0xFF6E8E46 : 0xFF7CC35A;
        g.linear(0, 470, 0, H, grass, Puppet.shade(grass, 0.75f));
        g.rect(0, 470, W, H - 470);
        // stone path
        g.color(tod == NIGHT ? 0xFF6B6B7A : 0xFFE8DCC4);
        g.begin(); g.moveTo(560, 480); g.lineTo(720, 480); g.lineTo(900, H); g.lineTo(380, H); g.close(); g.fillPath();
        // hedges and flower beds
        for (int i = 0; i < 7; i++) {
            float x = 60 + i * 200;
            if (x > 470 && x < 820) continue;
            g.color(Puppet.shade(grass, 0.7f));
            g.oval(x, 500, 70, 34);
            g.color(Puppet.shade(grass, 0.85f));
            g.oval(x - 20, 492, 40, 22);
        }
        // fountain far back
        g.color(marble(tod));
        g.oval(640, 486, 90, 14);
        g.color(0xFF90CAF9); g.oval(640, 484, 78, 9);
        // trees left and right
        tree(g, 70, 560, 1.25f, tod);
        tree(g, 1210, 560, 1.15f, tod);
        // flowers
        long s = 7;
        int[] fc = {0xFFFF6F91, 0xFFFFC75F, 0xFFF9F871, 0xFFFF9671, 0xFFD65DB1, 0xFFFFFFFF};
        for (int i = 0; i < 60; i++) {
            s = s * 6364136223846793005L + 1442695040888963407L;
            float x = (s >>> 33) % 1280;
            float y = 500 + ((s >>> 20) % 120);
            if (x > 430 && x < 850 && y > 520) continue;
            g.color(fc[(int) ((s >>> 40) % fc.length)]);
            g.oval(x, y, 4, 4);
        }
    }

    static void tree(Gfx g, float x, float y, float s, int tod) {
        if (STILL_LAYER.get()[0]) return;          // v35: drawn live, swaying (paintLive)
        tree(g, x, y, s, tod, 0, 0, false);
    }

    /**
     * A painted tree. live (v35): it sways in the wind like the plants of a picture (Nature.windPush: a lean with
     * real wind, the gusts travelling across the place, its own slow rocking) — the whole tree turns a little about
     * its foot and its top goes furthest — and its clumps of leaves tremble, each on its own.
     */
    static void tree(Gfx g, float x, float y, float s, int tod, float t, float wind, boolean live) {
        int leaf = tod == NIGHT ? 0xFF1E3A24 : tod == EVENING ? 0xFF4C6B34 : 0xFF3F8F3A;
        float lean = 0, f = 0;
        if (live) {
            lean = Nature.windPush(x / W, 0.4f, t, wind, 0.3f) * 2.2f;       // degrees: a quarter of a degree in still air, 2-4 in a strong wind
            f = (0.6f + 1.6f * Math.min(1.5f, Math.abs(wind))) * s;           // how far the clumps of leaves tremble
            g.save();
            g.translate(x, y); g.rotate(lean); g.translate(-x, -y);
        }
        g.color(tod == NIGHT ? 0xFF2B1E16 : 0xFF6D4C41);
        g.begin(); g.moveTo(x - 18 * s, y); g.lineTo(x - 10 * s, y - 230 * s); g.lineTo(x + 10 * s, y - 230 * s); g.lineTo(x + 18 * s, y); g.close(); g.fillPath();
        g.line(x, y - 160 * s, x + 70 * s, y - 230 * s, 9 * s);
        g.line(x, y - 140 * s, x - 70 * s, y - 210 * s, 9 * s);
        float u = x / W;
        g.color(Puppet.shade(leaf, 0.8f));
        g.oval(x + f * Nature.flutter(u, 0.1f, t), y - 270 * s, 120 * s, 85 * s);
        g.color(leaf);
        g.oval(x - 50 * s + f * Nature.flutter(u, 0.3f, t), y - 250 * s + f * 0.5f * Nature.flutter(0.3f, u, t), 70 * s, 55 * s);
        g.oval(x + 55 * s + f * Nature.flutter(u, 0.5f, t), y - 255 * s + f * 0.5f * Nature.flutter(0.5f, u, t), 70 * s, 55 * s);
        g.oval(x + f * 1.3f * Nature.flutter(u, 0.7f, t), y - 300 * s + f * 0.5f * Nature.flutter(0.7f, u, t), 80 * s, 55 * s);
        g.color(Puppet.lighten(leaf, 0.15f));
        g.oval(x - 25 * s + f * 1.4f * Nature.flutter(u, 0.9f, t), y - 310 * s, 30 * s, 18 * s);
        if (live) g.restore();
    }

    static void courtyard(Gfx g, int tod) {
        int m = marble(tod);
        // back wall with arches
        g.color(Puppet.shade(m, 0.9f)); g.rect(0, 250, W, 260);
        g.color(Puppet.shade(m, 0.72f));
        for (int i = 0; i < 9; i++) {
            float x = 70 + i * 145;
            g.begin(); g.moveTo(x - 45, 500); g.lineTo(x - 45, 340); g.quadTo(x, 280, x + 45, 340); g.lineTo(x + 45, 500); g.close(); g.fillPath();
        }
        g.color(m); g.rect(0, 238, W, 22);
        g.color(0xFFE5B530); g.rect(0, 258, W, 4);
        // floor tiles
        g.linear(0, 500, 0, H, 0xFFE9D9B8, 0xFFC8B08A);
        g.rect(0, 500, W, H - 500);
        g.color(0x22000000);
        for (int i = -10; i <= 20; i++) g.line(640 + i * 90, 500, 640 + i * 220, H, 2);
        for (int j = 0; j < 5; j++) { float y = 500 + j * j * 10 + j * 22; g.line(0, y, W, y, 2); }
        // practice dummies & weapon rack
        g.color(0xFF8D6E63);
        g.rect(1110, 420, 12, 140);
        g.line(1080, 450, 1150, 450, 10);
        g.color(0xFFD7B98E); g.oval(1116, 405, 22, 22);
        g.color(0xFF6D4C41);
        g.rect(80, 430, 120, 10); g.rect(80, 500, 120, 10);
        for (int i = 0; i < 5; i++) { g.color(0xFFB98A55); g.line(95 + i * 24, 415, 95 + i * 24, 520, 5); }
        // potted plants
        pot(g, 330, 520); pot(g, 950, 520);
    }

    static void pot(Gfx g, float x, float y) {
        g.color(0xFFBF6B3F); g.begin(); g.moveTo(x - 26, y - 40); g.lineTo(x + 26, y - 40); g.lineTo(x + 18, y); g.lineTo(x - 18, y); g.close(); g.fillPath();
        g.color(0xFF3F8F3A); g.oval(x, y - 58, 34, 26); g.oval(x - 18, y - 70, 18, 16); g.oval(x + 18, y - 72, 18, 16);
        g.color(0xFFFF6F91); g.oval(x - 8, y - 74, 6, 6); g.oval(x + 12, y - 62, 6, 6);
    }

    static void gate(Gfx g, int tod) {
        int m = marble(tod);
        int grass = tod == NIGHT ? 0xFF2E4D2E : 0xFF7CC35A;
        g.linear(0, 500, 0, H, grass, Puppet.shade(grass, 0.75f)); g.rect(0, 500, W, H - 500);
        g.color(0xFFE8DCC4); g.rect(420, 500, 440, H - 500);
        // walls
        g.color(Puppet.shade(m, 0.85f)); g.rect(0, 300, 420, 220); g.rect(860, 300, 420, 220);
        g.color(m);
        for (int i = 0; i < 9; i++) { g.rect(i * 48, 280, 30, 24); g.rect(870 + i * 48, 280, 30, 24); }
        // big arched gate
        g.color(m);
        g.rect(400, 140, 480, 380);
        g.begin(); g.moveTo(400, 140); g.quadTo(640, 30, 880, 140); g.close(); g.fillPath();
        g.color(Puppet.shade(m, 0.7f));
        g.begin(); g.moveTo(480, 520); g.lineTo(480, 270); g.quadTo(640, 150, 800, 270); g.lineTo(800, 520); g.close(); g.fillPath();
        // wooden doors (open)
        g.color(0xFF8D5A3B);
        g.begin(); g.moveTo(480, 520); g.lineTo(480, 270); g.lineTo(540, 290); g.lineTo(540, 520); g.close(); g.fillPath();
        g.begin(); g.moveTo(800, 520); g.lineTo(800, 270); g.lineTo(740, 290); g.lineTo(740, 520); g.close(); g.fillPath();
        g.color(0xFFE5B530);
        for (int k = 0; k < 4; k++) { g.oval(510, 320 + k * 50, 5, 5); g.oval(770, 320 + k * 50, 5, 5); }
        // view through the gate
        g.color(tod == NIGHT ? 0xFF1C2A3A : 0xFFA5D6A7);
        g.begin(); g.moveTo(540, 520); g.lineTo(540, 290); g.quadTo(640, 210, 740, 290); g.lineTo(740, 520); g.close(); g.fillPath();
        g.color(tod == NIGHT ? 0xFF16301F : 0xFF66BB6A);
        g.oval(640, 520, 110, 40);
        g.color(0xFFE5B530); g.rect(400, 190, 480, 8);
        g.color(0xFFC62828); g.oval(640, 120, 20, 20);
        tree(g, 130, 560, 1.3f, tod);
        tree(g, 1160, 560, 1.2f, tod);
    }

    static void meadow(Gfx g, int tod) {
        int grass = tod == NIGHT ? 0xFF2E4D2E : 0xFF8BC34A;
        g.linear(0, 480, 0, H, grass, Puppet.shade(grass, 0.75f)); g.rect(0, 480, W, H - 480);
        tree(g, 140, 560, 1.1f, tod); tree(g, 1120, 560, 1.0f, tod);
    }

    static void village(Gfx g, int tod) {
        meadow(g, tod);
        for (int i = 0; i < 4; i++) {
            float x = 250 + i * 250;
            g.color(0xFFD7B98E); g.rect(x - 70, 380, 140, 120);
            g.color(0xFF8D6E63); g.begin(); g.moveTo(x - 90, 385); g.lineTo(x, 320); g.lineTo(x + 90, 385); g.close(); g.fillPath();
            g.color(0xFF6D4C41); g.rect(x - 18, 440, 36, 60);
        }
    }

    /** A city skyline: towers of glass with lit windows, drawn from a seed so it is the same every frame. */
    static void skyline(Gfx g, int tod, float base, float scale) {
        boolean night = tod == NIGHT, eve = tod == EVENING;
        int far = night ? 0xFF1A2238 : eve ? 0xFF5A4A78 : 0xFF9FB8CC, near = night ? 0xFF11182A : eve ? 0xFF3C3458 : 0xFF7E97AE;
        long sd = 7;
        for (int i = 0; i < 18; i++) {
            sd = sd * 6364136223846793005L + 1442695040888963407L;
            float w = (60 + ((sd >>> 33) % 90)) * scale, h = (120 + ((sd >>> 13) % 260)) * scale;
            float x = -40 + i * 76 * scale;
            g.color(i % 2 == 0 ? far : near);
            g.rect(x, base - h, w, h);
            // windows
            g.color(night ? 0xCCFFE082 : eve ? 0x88FFD180 : 0x66FFFFFF);
            for (int r = 0; r < (int) (h / (26 * scale)); r++) for (int c = 0; c < (int) (w / (18 * scale)); c++) {
                sd = sd * 6364136223846793005L + 1442695040888963407L;
                if (((sd >>> 40) % 3) == 0) g.rect(x + 6 * scale + c * 18 * scale, base - h + 10 * scale + r * 26 * scale, 8 * scale, 12 * scale);
            }
        }
        // the tallest glass tower
        g.color(night ? 0xFF1E2A48 : eve ? 0xFF6E5A90 : 0xFFC9DBE8);
        g.begin(); g.moveTo(900, base); g.lineTo(900, base - 420 * scale); g.lineTo(960, base - 470 * scale); g.lineTo(1020, base - 420 * scale); g.lineTo(1020, base); g.close(); g.fillPath();
        g.color(night ? 0x55FFFFFF : 0x44FFFFFF);
        for (int r = 0; r < 14; r++) g.rect(908, base - 400 * scale + r * 28 * scale, 104, 4 * scale);
    }

    /** A rooftop garden on a high-rise: the skyline behind, a railing, planters with solar flowers. */
    static void rooftop(Gfx g, int tod) {
        boolean night = tod == NIGHT;
        skyline(g, tod, 520, 1f);
        // haze over the city
        g.linear(0, 380, 0, 520, 0x00FFFFFF, night ? 0x33101830 : 0x55E6F0F8); g.rect(0, 380, W, 140);
        // the terrace: concrete floor with tiles
        g.linear(0, 520, 0, H, night ? 0xFF3A3F4A : 0xFFD6D2C8, night ? 0xFF22262E : 0xFFA9A49A); g.rect(0, 520, W, H - 520);
        g.color(night ? 0x22FFFFFF : 0x33000000);
        for (int i = 0; i < 9; i++) g.line(i * 160, 520, i * 160 - 120, H, 2);
        for (int i = 1; i < 5; i++) g.line(0, 520 + i * 50, W, 520 + i * 50, 2);
        // glass railing with posts
        g.color(night ? 0x5580B0D0 : 0x6690C0E0); g.rect(0, 455, W, 65);
        g.color(night ? 0xFF8A94A6 : 0xFFE8ECF0);
        for (int i = 0; i <= 8; i++) g.rect(i * 160 - 4, 450, 8, 72);
        g.rect(0, 450, W, 6);
        // planters with solar flowers (they glow at night: paintLive adds the pulse)
        for (int i = 0; i < 5; i++) {
            float x = 120 + i * 260;
            g.color(night ? 0xFF4A4E58 : 0xFF8D8D8D); g.roundRect(x - 70, 560, 140, 60, 8);
            g.color(night ? 0xFF2E5A2E : 0xFF4C9A3E); g.oval(x, 562, 66, 12);
            for (int k = -1; k <= 1; k++) {
                g.color(0xFF3E6B3E); g.line(x + k * 36, 560, x + k * 36 + k * 4, 505, 4);
                g.radial(x + k * 36 + k * 4, 500, 26, night ? 0x9900E5FF : 0x66FFF59D, 0x0000E5FF); g.oval(x + k * 36 + k * 4, 500, 26, 26);
                g.color(night ? 0xFF80DEEA : 0xFFFFD54F); g.oval(x + k * 36 + k * 4, 500, 14, 14);
                g.color(0xFF1E88E5); g.oval(x + k * 36 + k * 4, 500, 6, 6);
            }
        }
        // a water tank and an antenna
        g.color(night ? 0xFF2A2E38 : 0xFF6E7F8E); g.rect(1150, 400, 90, 110); g.oval(1195, 400, 45, 14);
        g.color(night ? 0xFF8A94A6 : 0xFF455A64); g.line(1230, 400, 1230, 300, 3); g.line(1215, 320, 1245, 320, 3);
    }

    /** A closed mall's basement: concrete, a rusty shutter, broken arcade machines, a tubelight, pipes. */
    static void basement(Gfx g) {
        g.linear(0, 0, 0, H, 0xFF0C1410, 0xFF101A14); g.rect(0, 0, W, H);
        // back wall of concrete with damp patches and a rusty shutter
        g.linear(0, 90, 0, 500, 0xFF1E2A22, 0xFF151E18); g.rect(0, 90, W, 410);
        g.color(0x22000000);
        for (int i = 0; i < 6; i++) g.oval(150 + i * 200, 200 + (i % 3) * 60, 80, 40);
        g.color(0xFF4E3A2A); g.rect(540, 170, 220, 330);
        g.color(0xFF3B2B1E); for (int i = 0; i < 11; i++) g.rect(540, 170 + i * 30, 220, 4);
        g.color(0xFF7A5230); g.oval(600, 230, 18, 12); g.oval(700, 400, 24, 14);
        // arcade machines: boxes with dark screens and a crack
        int[] xs = {120, 300, 950, 1120};
        for (int i = 0; i < xs.length; i++) {
            int x = xs[i];
            g.color(i % 2 == 0 ? 0xFF2A2A36 : 0xFF33222A); g.rect(x - 60, 280, 120, 220);
            g.color(0xFF1A1E24); g.rect(x - 45, 300, 90, 70);
            g.color(0xFF2E3A44); g.line(x - 30, 310, x + 20, 360, 1.5f);
            g.color(0xFF5C6B73); g.rect(x - 45, 390, 90, 12);
            g.color(0xFFE53935); g.oval(x - 20, 420, 8, 8); g.color(0xFF1E88E5); g.oval(x + 10, 420, 8, 8);
        }
        // pipes and a CCTV camera
        g.color(0xFF37474F); g.rect(0, 60, W, 14); g.rect(200, 60, 14, 120); g.rect(1000, 60, 14, 90);
        g.color(0xFF263238); g.rect(1180, 120, 40, 22); g.oval(1218, 131, 10, 10);
        g.color(0xFFFF1744); g.oval(1186, 126, 3, 3);
        // the tubelight (its flicker is in paintLive)
        g.color(0xFFB9F6CA); g.roundRect(480, 100, 320, 12, 6);
        // floor: cracked concrete with puddles
        g.linear(0, 500, 0, H, 0xFF2A3430, 0xFF141B18); g.rect(0, 500, W, H - 500);
        g.color(0x442A5A48); g.oval(380, 640, 120, 18); g.oval(900, 680, 150, 20);
        g.color(0xFF0E1512); g.line(100, 520, 260, 700, 2); g.line(760, 560, 820, 700, 2);
        // a server rack with cables (the flowers' light is drawn by the story's effects)
        g.color(0xFF1C2228); g.rect(790, 330, 110, 170);
        for (int i = 0; i < 6; i++) { g.color(i % 2 == 0 ? 0xFF00E676 : 0xFF1E88E5); g.oval(805, 350 + i * 25, 5, 5); }
        g.color(0xFF263238); g.line(900, 420, 1000, 500, 3); g.line(790, 480, 700, 500, 3);
    }

    /** A room of today: a wall with a window, a sofa, a shelf, a table with a lamp. */
    static void room(Gfx g, int tod) {
        boolean night = tod == NIGHT || tod == EVENING;
        g.linear(0, 0, 0, 520, night ? 0xFF3A3F55 : 0xFFF2E8D8, night ? 0xFF2A2E40 : 0xFFE3D6C2); g.rect(0, 0, W, 520);
        // window
        g.color(night ? 0xFF1B2342 : 0xFF9ED7F2); g.rect(820, 120, 300, 260);
        if (night) { g.color(0xCCFFFFFF); for (int i = 0; i < 14; i++) g.oval(840 + (i * 61) % 280, 135 + (i * 37) % 120, 2, 2); }
        else { g.color(0xCCFFFFFF); cloud(g, 900, 180, 0.7f); }
        g.color(night ? 0xFF8A94A6 : 0xFFF7F7F2); g.rect(812, 112, 316, 8); g.rect(812, 372, 316, 8); g.rect(812, 112, 8, 268); g.rect(1120, 112, 8, 268); g.rect(966, 112, 6, 268);
        // curtains
        g.color(night ? 0xFF4A3A5A : 0xFFE57373); g.rect(770, 100, 50, 300); g.rect(1120, 100, 50, 300);
        // shelf with books
        g.color(0xFF8D6E63); g.rect(120, 180, 260, 10); g.rect(120, 290, 260, 10);
        int[] bc = {0xFFC62828, 0xFF1565C0, 0xFF2E7D32, 0xFFF9A825, 0xFF6A1B9A};
        for (int i = 0; i < 9; i++) { g.color(bc[i % 5]); g.rect(130 + i * 26, 200, 20, 88); }
        for (int i = 0; i < 7; i++) { g.color(bc[(i + 2) % 5]); g.rect(140 + i * 32, 240 + 50, 24, 40 - 40); }
        // floor and a rug
        g.linear(0, 520, 0, H, night ? 0xFF5A4634 : 0xFFC9A77A, night ? 0xFF3A2E22 : 0xFFA98A5E); g.rect(0, 520, W, H - 520);
        g.color(night ? 0xFF4A3550 : 0xFFB0574F); g.roundRect(380, 600, 520, 100, 10);
        g.color(night ? 0x33FFFFFF : 0x44FFFFFF); g.roundRect(400, 615, 480, 70, 8);
        // sofa and a table with a lamp
        g.color(night ? 0xFF4C5C6C : 0xFF5C8DB8); g.roundRect(60, 430, 300, 110, 16); g.roundRect(60, 390, 300, 60, 16);
        g.color(night ? 0xFF3A4858 : 0xFF4A78A0); g.roundRect(80, 400, 120, 50, 12); g.roundRect(220, 400, 120, 50, 12);
        g.color(0xFF8D6E63); g.rect(1080, 470, 150, 12); g.rect(1095, 482, 10, 60); g.rect(1215, 482, 10, 60);
        g.color(0xFFFFCC80); g.begin(); g.moveTo(1130, 470); g.lineTo(1180, 470); g.lineTo(1192, 420); g.lineTo(1118, 420); g.close(); g.fillPath();
        if (night) { g.radial(1155, 445, 120, 0x77FFE0A0, 0x00FFE0A0); g.oval(1155, 445, 120, 120); }
    }

    /** A city street: buildings, shop fronts, a road with lane marks, a streetlight. */
    static void street(Gfx g, int tod) {
        boolean night = tod == NIGHT;
        skyline(g, tod, 420, 0.8f);
        // the near buildings with shop fronts
        int[] cols = {0xFFE0C9A6, 0xFFB7C9D6, 0xFFD9B2A0, 0xFFC8D6B9};
        for (int i = 0; i < 4; i++) {
            float x = i * 330 - 40;
            g.color(night ? shade(cols[i], 0.4f) : cols[i]); g.rect(x, 230, 300, 290);
            g.color(night ? 0xFF2A2E38 : 0xFF5C6B73); g.rect(x + 30, 420, 240, 100);
            g.color(night ? 0xCCFFE082 : 0xFF80DEEA); g.rect(x + 45, 435, 210, 70);
            for (int r = 0; r < 3; r++) for (int c = 0; c < 3; c++) { g.color(night ? 0xAAFFE082 : 0xFF8FB8D0); g.rect(x + 40 + c * 90, 255 + r * 50, 50, 34); }
            g.color(i % 2 == 0 ? 0xFFE53935 : 0xFF1E88E5); g.rect(x + 30, 395, 240, 26);
        }
        // pavement and the road
        g.color(night ? 0xFF4A4E58 : 0xFFBDB8AE); g.rect(0, 520, W, 40);
        g.linear(0, 560, 0, H, night ? 0xFF2A2E36 : 0xFF6E7278, night ? 0xFF1A1D24 : 0xFF4E5258); g.rect(0, 560, W, H - 560);
        g.color(0xFFF5F5DC); for (int i = 0; i < 8; i++) g.rect(i * 170 + 20, 640, 90, 8);
        // a streetlight and a scooter parked at the kerb on its stand
        g.color(0xFF455A64); g.rect(1080, 300, 8, 260); g.rect(1040, 300, 60, 8);
        if (night) { g.radial(1045, 310, 150, 0x88FFE0A0, 0x00FFE0A0); g.oval(1045, 310, 150, 150); }
        // (v43: at its true size on the far kerb — it was a toy-sized red box at the people's feet)
        g.save(); g.translate(250, 556);
        Props.vehicle(g, Film.V_SCOOTER, 250, 1, 0, 0, night ? 0xFF7A2626 : 0xFFC62828, false, 0, 0, 1);
        Props.vehicle(g, Film.V_SCOOTER, 250, 1, 0, 0, night ? 0xFF7A2626 : 0xFFC62828, true, 0, 0, 1);
        g.restore();
    }

    static void hall(Gfx g) {
        g.linear(0, 0, 0, H, 0xFF8E2C2C, 0xFF4A1414); g.rect(0, 0, W, H);
        g.color(0xFFE5B530);
        for (int i = 0; i < 6; i++) { g.rect(80 + i * 230, 120, 30, 420); }
        g.linear(0, 520, 0, H, 0xFFE9D9B8, 0xFFB8A07A); g.rect(0, 520, W, H - 520);
        g.color(0xFFC62828); g.rect(540, 520, 200, H - 520);
    }

    static void caveInside(Gfx g) {
        g.radial(640, 380, 820, 0xFF3F5A44, 0xFF101612); g.rect(0, 0, W, H);
        // rock layers
        g.color(0xFF141A16);
        g.begin(); g.moveTo(0, 0); g.lineTo(W, 0); g.lineTo(W, 150);
        for (int i = 20; i >= 0; i--) g.lineTo(i * 64, (i % 2 == 0) ? 120 : 210 + (i % 3) * 20);
        g.close(); g.fillPath();
        // stalactites
        g.color(0xFF1D241F);
        for (int i = 0; i < 14; i++) {
            float x = 40 + i * 92, len = 60 + (i * 37 % 70);
            g.begin(); g.moveTo(x - 18, 150); g.lineTo(x, 150 + len); g.lineTo(x + 18, 150); g.close(); g.fillPath();
        }
        // floor
        g.linear(0, 520, 0, H, 0xFF39443B, 0xFF1A211C); g.rect(0, 520, W, H - 520);
        // sharp rocks
        g.color(0xFF1A201C);
        for (int i = 0; i < 9; i++) {
            float x = 60 + i * 150;
            g.begin(); g.moveTo(x - 50, 560); g.lineTo(x - 10, 470 - (i % 3) * 20); g.lineTo(x + 40, 560); g.close(); g.fillPath();
        }
        // big sitting boulder on the right
        g.color(0xFF2C332E); g.oval(1000, 600, 120, 45);
        g.color(0xFF353D37); g.oval(990, 588, 100, 34);
        // bones
        g.color(0xFFBDB59A);
        g.roundRect(230, 640, 50, 8, 4); g.oval(230, 644, 7, 7); g.oval(280, 644, 7, 7);
        g.oval(330, 650, 14, 11);
        // cave opening light (left)
        g.radial(60, 420, 240, 0x55FFF3C4, 0x00FFF3C4); g.oval(60, 420, 240, 240);
    }

    static void caveMouth(Gfx g, int tod) {
        sky(g, tod);
        g.color(tod == NIGHT ? 0xFF16301F : 0xFF4F7A45); g.rect(0, 470, W, H - 470);
        // mountain rock with cave mouth
        g.color(0xFF2B2F35);
        g.begin(); g.moveTo(380, H); g.lineTo(420, 260); g.quadTo(700, 30, 1000, 240); g.lineTo(1280, 300); g.lineTo(1280, H); g.close(); g.fillPath();
        g.color(0xFF22262B);
        g.begin(); g.moveTo(650, H); g.lineTo(650, 330); g.quadTo(860, 180, 1070, 330); g.lineTo(1070, H); g.close(); g.fillPath();
        g.color(0xFF0A0D0B);
        g.begin(); g.moveTo(690, 620); g.lineTo(690, 360); g.quadTo(860, 230, 1030, 360); g.lineTo(1030, 620); g.close(); g.fillPath();
        g.radial(860, 470, 200, 0x6676FF03, 0x0076FF03); g.oval(860, 470, 200, 200);
        // huge stone beside the mouth
        g.color(0xFF4A4F55); g.oval(1150, 570, 110, 85);
        g.color(0xFF5A6066); g.oval(1135, 545, 70, 45);
        // bell frame
        g.color(0xFF5D4037);
        g.rect(560, 250, 14, 380); g.rect(560, 250, 140, 14);
        g.color(0xFF3E2716); g.line(640, 264, 640, 310, 4);
        g.color(0xFFC8A951);
        g.line(640, 330, 640, 520, 3);
        tree(g, 140, 600, 1.1f, tod);
        g.linear(0, 600, 0, H, 0xFF3D5E36, 0xFF263D22); g.rect(0, 600, W, H - 600);
    }

    static void forest(Gfx g, int tod) {
        boolean night = tod == NIGHT || tod == EVENING;
        g.linear(0, 0, 0, H, night ? 0xFF0B1328 : 0xFF6FA8C9, night ? 0xFF1E2E3A : 0xFFB9D8B0);
        g.rect(0, 0, W, H);
        if (night) {
            g.color(0xFFFFFDE7); g.oval(980, 100, 38, 38);
            g.color(0xFF0B1328); g.oval(996, 90, 34, 34);
        }
        int[] layers = night ? new int[]{0xFF15233A, 0xFF101C26, 0xFF0B141A} : new int[]{0xFF4E8B57, 0xFF3C7445, 0xFF2C5C34};
        for (int L = 0; L < 3; L++) {
            g.color(layers[L]);
            for (int i = 0; i < 12; i++) {
                float x = i * 120 + (L * 47 % 120) - 40;
                float h = 300 + ((i * 53 + L * 31) % 160) + L * 60;
                float base = 560 + L * 30;
                g.begin(); g.moveTo(x - 70, base); g.lineTo(x, base - h); g.lineTo(x + 70, base); g.close(); g.fillPath();
                g.rect(x - 6, base - 10, 12, 40);
            }
        }
        g.linear(0, 580, 0, H, night ? 0xFF14201A : 0xFF3E6B3A, night ? 0xFF080D0A : 0xFF2A4A28);
        g.rect(0, 580, W, H - 580);
        // thorny bushes
        g.color(night ? 0xFF0F1A12 : 0xFF2E5530);
        for (int i = 0; i < 8; i++) g.oval(80 + i * 170, 650, 80, 34);
        // big rock on the right where the monkey can jump
        g.color(night ? 0xFF2A2F36 : 0xFF6E7378); g.oval(1100, 610, 90, 55);
        if (night) {
            // distant green glow of the cave
            g.radial(1180, 420, 160, 0x5576FF03, 0x0076FF03); g.oval(1180, 420, 160, 160);
        }
    }

    // ------------------------------------------------------------------ animated overlays

    /** Drawn every frame on top of the static layer (cheap animated details). */
    /** A flicker that is mostly on: a few drops a second, from a time-based hash (the same every render). */
    public static float flicker(float t) {
        float a = (float) Math.sin(t * 37.1) * (float) Math.sin(t * 5.3) * (float) Math.sin(t * 0.7 + 1);
        float f = 0.82f + 0.18f * (float) Math.sin(t * 23);
        if (a > 0.55f) f *= 0.15f;
        return f;
    }

    public static void paintLive(Gfx g, int set, int tod, float t) { paintLive(g, set, tod, t, 0); }

    /** wind (v35): the film's wind now (+ towards the right): the place's trees sway in it. */
    public static void paintLive(Gfx g, int set, int tod, float t, float wind) {
        float[][] tr = trees(set);
        if (tr != null) for (float[] q : tr) tree(g, q[0], q[1], q[2], tod, t, wind, true);
        if (set == CAVE_MOUTH && tr != null) {
            // the cave mouth's ground runs in front of its tree's foot
            g.linear(0, 600, 0, H, 0xFF3D5E36, 0xFF263D22); g.rect(0, 600, W, H - 600);
        }
        switch (set) {
            case ROOFTOP: {
                // drones hum across the sky, their lights blinking; the solar flowers pulse at night
                for (int i = 0; i < 3; i++) {
                    float x = (t * (40 + i * 25) + i * 500) % (W + 200) - 100, y = 120 + i * 70 + (float) Math.sin(t * 1.5 + i) * 12;
                    g.color(0xFF37474F); g.oval(x, y, 10, 4); g.line(x - 12, y - 2, x + 12, y - 2, 2);
                    g.color(0x8837474F); g.oval(x - 12, y - 3, 8, 2); g.oval(x + 12, y - 3, 8, 2);
                    g.color(((int) (t * 4 + i)) % 2 == 0 ? 0xFFFF1744 : 0xFF00E676); g.oval(x, y + 1, 2.5f, 2.5f);
                }
                if (tod == NIGHT || tod == EVENING) {
                    float pulse = 0.5f + 0.5f * (float) Math.sin(t * 2.2);
                    for (int i = 0; i < 5; i++) for (int k = -1; k <= 1; k++) {
                        float x = 120 + i * 260 + k * 36 + k * 4;
                        g.radial(x, 500, 40, alpha(0xFF00E5FF, 0.35f * pulse), 0x0000E5FF); g.oval(x, 500, 40, 40);
                    }
                }
                break;
            }
            case BASEMENT: {
                // the green tubelight flickers, and the arcade screens with it
                float f = flicker(t);
                g.radial(640, 110, 420, alpha(0xFF69F0AE, 0.28f * f), 0x0069F0AE); g.oval(640, 110, 420, 420);
                g.color(alpha(0xFFB9F6CA, 0.5f + 0.5f * f)); g.roundRect(480, 100, 320, 12, 6);
                g.color(alpha(0xFF00E676, 0.08f * f)); g.rect(0, 0, W, H);
                int[] xs = {120, 300, 950, 1120};
                for (int x : xs) { g.color(alpha(0xFF2E7DFF, 0.1f + 0.15f * (float) Math.max(0, Math.sin(t * 3 + x)))); g.rect(x - 45, 300, 90, 70); }
                break;
            }
            case STREET: {
                // a car passes along the far side of the road now and then, behind everyone, someone at its wheel
                // (v43: at its true size in the far lane — it was a toy at the people's feet)
                float x = (t * 210) % (W + 1400) - 700;
                g.save(); g.translate(x, 566);
                int col = tod == NIGHT ? 0xFF1E2A48 : 0xFF5C6BC0;
                Props.vehicle(g, Film.V_CAR + Film.V_PASSENGER, 230, 1, x, t, col, false);
                Props.vehicle(g, Film.V_CAR + Film.V_PASSENGER, 230, 1, x, t, col, true);
                if (tod == NIGHT) { g.color(0x55FFF59D); g.oval(0.9f * 230, -0.4f * 230, 60, 18); }
                g.restore();
                break;
            }
            default:
        }
        switch (set) {
            case GARDEN: case CELEBRATION: case GENERIC_OUT: case GATE: {
                if (tod != NIGHT) {
                    // waterfall shimmer (the palace garden's waterfall)
                    if (set == GARDEN) {
                        g.color(0x88FFFFFF);
                        for (int i = 0; i < 4; i++) {
                            float y = 300 + ((t * 120 + i * 30) % 120);
                            g.rect(551, y, 12, 6);
                        }
                    }
                    // birds
                    g.color(0xFF37474F);
                    for (int i = 0; i < 3; i++) {
                        float x = ((t * 40 + i * 300) % 1500) - 100, y = 140 + i * 30 + (float) Math.sin(t * 3 + i) * 8;
                        float w = (float) Math.sin(t * 10 + i) * 6;
                        g.begin(); g.moveTo(x - 10, y - w); g.quadTo(x - 4, y - 4, x, y); g.quadTo(x + 4, y - 4, x + 10, y - w); g.strokePath(2);
                    }
                }
                if (set == CELEBRATION) celebrationLights(g, t);
                break;
            }
            case CAVE_IN: {
                float fl = 0.5f + 0.25f * (float) Math.sin(t * 2.7f) + 0.1f * (float) Math.sin(t * 7.1f);
                g.radial(980, 300, 520, Puppet.alpha(0xFF76FF03, 0.18f * fl), 0x0076FF03);
                g.oval(980, 300, 520, 520);
                // dripping water
                g.color(0x8890CAF9);
                for (int i = 0; i < 3; i++) {
                    float x = 132 + i * 368, y = 230 + ((t * 160 + i * 90) % 300);
                    g.oval(x, y, 2.5f, 5);
                }
                break;
            }
            case FOREST: {
                // fireflies
                for (int i = 0; i < 14; i++) {
                    float x = (i * 97 + t * 18 * ((i % 3) + 1)) % 1280;
                    float y = 420 + (float) Math.sin(t * 1.3f + i) * 60 + (i % 5) * 30;
                    float a = 0.4f + 0.6f * (float) Math.abs(Math.sin(t * 2 + i));
                    g.radial(x, y, 8, Puppet.alpha(0xFFE6EE9C, a), 0x00E6EE9C);
                    g.oval(x, y, 8, 8);
                }
                break;
            }
            case CAVE_MOUTH: {
                float fl = 0.6f + 0.3f * (float) Math.sin(t * 3);
                g.radial(860, 470, 200, Puppet.alpha(0xFF76FF03, 0.25f * fl), 0x0076FF03);
                g.oval(860, 470, 200, 200);
                break;
            }
            default:
        }
    }

    static void celebrationLights(Gfx g, float t) {
        int[] c = {0xFFFFEB3B, 0xFFFF7043, 0xFFE91E63, 0xFF76FF03, 0xFF40C4FF};
        for (int row = 0; row < 2; row++) {
            for (int i = 0; i < 26; i++) {
                float x = i * 52, y = 60 + row * 70 + (float) Math.sin(i * 0.5f) * 20;
                float on = (float) Math.abs(Math.sin(t * 3 + i * 0.7f + row));
                g.radial(x, y, 14, Puppet.alpha(c[(i + row) % c.length], 0.5f * on + 0.3f), 0x00FFFFFF);
                g.oval(x, y, 14, 14);
            }
        }
        // marigold garlands (torans)
        g.color(0xFFFF9800);
        for (int i = 0; i < 40; i++) {
            float x = i * 33, y = 30 + (float) Math.abs(Math.sin(i * 0.39)) * 30;
            g.oval(x, y, 7, 7);
        }
        // diyas on the ground
        for (int i = 0; i < 10; i++) {
            float x = 40 + i * 136, y = 690;
            g.color(0xFFBF6B3F); g.oval(x, y, 14, 6);
            float fl = 1 + 0.2f * (float) Math.sin(t * 9 + i);
            g.color(0xFFFFC107); g.oval(x, y - 9 * fl, 4, 8 * fl);
        }
    }

    /** Foreground pieces drawn in front of characters (depth). */
    public static void paintFront(Gfx g, int set, int tod) {
        if (set == GARDEN || set == CELEBRATION) {
            g.color(tod == NIGHT ? 0xFF1E3A24 : 0xFF4C9A3E);
            g.oval(-20, 720, 160, 60);
            g.oval(1300, 720, 160, 60);
        } else if (set == FOREST) {
            g.color(0xFF06090A);
            g.oval(-30, 730, 200, 80);
            g.oval(1320, 730, 220, 90);
        }
    }

    /** Ambient light tint applied over characters so they sit in the scene. */
    static int alpha(int c, float a) { return Puppet.alpha(c, a); }
    static int shade(int c, float f) { return Puppet.shade(c, f); }

    public static int tint(int set, int tod) {
        if (set == CAVE_IN) return 0x220E2A10;
        if (set == BASEMENT) return 0x3A0E2A18;
        if (set == ROOM && (tod == NIGHT || tod == EVENING)) return 0x30201830;
        if (set == FOREST) return tod == NIGHT || tod == EVENING ? 0x55101C3A : 0;
        if (tod == NIGHT) return 0x50101C3A;
        if (tod == EVENING) return 0x22FF7043;
        if (tod == MORNING) return 0x10FFC107;
        return 0;
    }
}
