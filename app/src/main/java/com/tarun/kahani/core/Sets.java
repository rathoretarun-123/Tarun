package com.tarun.kahani.core;

/** Painted backgrounds ("sets") for scenes, in a 1280x720 virtual stage. */
public final class Sets {
    private Sets() {}

    public static final float W = 1280, H = 720, GROUND = 640;

    public static final int GARDEN = 0, COURTYARD = 1, GATE = 2, CAVE_IN = 3, CAVE_MOUTH = 4, FOREST = 5,
            CELEBRATION = 6, HALL = 7, VILLAGE = 8, GENERIC_OUT = 9;
    public static final int MORNING = 0, DAY = 1, EVENING = 2, NIGHT = 3;

    /** Sets under the open sky (weather, stars and fireflies belong there). */
    public static boolean outdoorSet(int set) {
        return set == GARDEN || set == FOREST || set == VILLAGE || set == COURTYARD || set == GATE || set == CELEBRATION || set == CAVE_MOUTH || set == GENERIC_OUT;
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
            default: return "खुला स्थान";
        }
    }

    /** Guesses the set from a free-text location description. */
    public static int detect(String text) {
        if (Txt.has(text, "गुफा के अंदर", "गुफा में", "inside the cave", "अपनी गुफा")) return CAVE_IN;
        if (Txt.has(text, "मुहान", "गुफा के बाहर", "गुफा के दरवाज", "cave entrance")) return CAVE_MOUTH;
        if (Txt.has(text, "गुफा", "cave", "तहखान", "कैदखान")) return CAVE_IN;
        if (Txt.has(text, "जंगल", "वन ", "forest", "jungle", "woods")) return FOREST;
        if (Txt.has(text, "सजा", "उत्सव", "जश्न", "रोशनियों", "ढोल", "celebration", "festival")) return CELEBRATION;
        if (Txt.has(text, "मुख्य द्वार", "द्वार", "गेट", "फाटक", "gate")) return GATE;
        if (Txt.has(text, "प्रांगण", "आँगन", "मैदान", "courtyard", "arena")) return COURTYARD;
        if (Txt.has(text, "बगीच", "बाग", "उद्यान", "garden")) return GARDEN;
        if (Txt.has(text, "दरबार", "कक्ष", "कमरे", "hall", "room", "throne")) return HALL;
        if (Txt.has(text, "गाँव", "गांव", "बाज़ार", "village", "market")) return VILLAGE;
        if (Txt.has(text, "महल", "palace")) return GARDEN;
        return GENERIC_OUT;
    }

    public static int detectTime(String text, int fallback) {
        if (Txt.has(text, "सूर्योदय", "सुबह", "भोर", "sunrise", "morning")) return MORNING;
        if (Txt.has(text, "रात", "अँधेरा घिर", "night", "midnight")) return NIGHT;
        if (Txt.has(text, "शाम", "सूर्यास्त", "evening", "sunset", "संध्या")) return EVENING;
        if (Txt.has(text, "दोपहर", "दिन", "noon", "afternoon")) return DAY;
        return fallback;
    }

    public static boolean indoorDark(int set) { return set == CAVE_IN; }

    static int lerp(int a, int b, float t) { return Puppet.mix(a, b, t); }

    // ------------------------------------------------------------------ static layer

    public static void paintStatic(Gfx g, int set, int tod) {
        switch (set) {
            case CAVE_IN: caveInside(g); return;
            case CAVE_MOUTH: caveMouth(g, tod); return;
            case FOREST: forest(g, tod); return;
            case HALL: hall(g); return;
            default:
        }
        sky(g, tod);
        hills(g, tod);
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

    static void hills(Gfx g, int tod) {
        int far = tod == NIGHT ? 0xFF1C2A3A : tod == EVENING ? 0xFF6A4E7A : 0xFF7FB27A;
        int near = tod == NIGHT ? 0xFF16301F : tod == EVENING ? 0xFF4F6A4A : 0xFF5E9E4E;
        g.color(far);
        g.begin(); g.moveTo(0, 420);
        g.quadTo(160, 300, 330, 380); g.quadTo(520, 250, 720, 370); g.quadTo(930, 280, 1100, 360); g.quadTo(1200, 330, 1280, 360);
        g.lineTo(1280, GROUND); g.lineTo(0, GROUND); g.close(); g.fillPath();
        // waterfall on the far hill
        if (tod != NIGHT) {
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
        int leaf = tod == NIGHT ? 0xFF1E3A24 : tod == EVENING ? 0xFF4C6B34 : 0xFF3F8F3A;
        g.color(tod == NIGHT ? 0xFF2B1E16 : 0xFF6D4C41);
        g.begin(); g.moveTo(x - 18 * s, y); g.lineTo(x - 10 * s, y - 230 * s); g.lineTo(x + 10 * s, y - 230 * s); g.lineTo(x + 18 * s, y); g.close(); g.fillPath();
        g.line(x, y - 160 * s, x + 70 * s, y - 230 * s, 9 * s);
        g.line(x, y - 140 * s, x - 70 * s, y - 210 * s, 9 * s);
        g.color(Puppet.shade(leaf, 0.8f));
        g.oval(x, y - 270 * s, 120 * s, 85 * s);
        g.color(leaf);
        g.oval(x - 50 * s, y - 250 * s, 70 * s, 55 * s);
        g.oval(x + 55 * s, y - 255 * s, 70 * s, 55 * s);
        g.oval(x, y - 300 * s, 80 * s, 55 * s);
        g.color(Puppet.lighten(leaf, 0.15f));
        g.oval(x - 25 * s, y - 310 * s, 30 * s, 18 * s);
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
    public static void paintLive(Gfx g, int set, int tod, float t) {
        switch (set) {
            case GARDEN: case CELEBRATION: case GENERIC_OUT: case GATE: {
                if (tod != NIGHT) {
                    // waterfall shimmer
                    if (set != GATE) {
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
    public static int tint(int set, int tod) {
        if (set == CAVE_IN) return 0x220E2A10;
        if (set == FOREST) return tod == NIGHT || tod == EVENING ? 0x55101C3A : 0;
        if (tod == NIGHT) return 0x50101C3A;
        if (tod == EVENING) return 0x22FF7043;
        if (tod == MORNING) return 0x10FFC107;
        return 0;
    }
}
