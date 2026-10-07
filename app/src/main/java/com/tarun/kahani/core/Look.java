package com.tarun.kahani.core;

/** Visual design of one cartoon character, derived from the script's description. */
public final class Look {
    // body kinds
    public static final int GIRL = 0, WOMAN = 1, MAN = 2, BOY = 3, WITCH = 4, MONSTER = 5, MONKEY = 6, OLD_MAN = 7;
    // outfits
    public static final int O_LEHENGA = 0, O_SALWAR = 1, O_SAREE = 2, O_ACHKAN = 3, O_UNIFORM = 4, O_CLOAK = 5,
            O_ARMOR = 6, O_JACKET = 7, O_KURTA = 8, O_FROCK = 9;
    // hair
    public static final int H_NONE = 0, H_BRAID = 1, H_PIGTAILS = 2, H_BUN = 3, H_SHORT = 4, H_LONG = 5;
    // headwear
    public static final int HW_NONE = 0, HW_TURBAN = 1, HW_WITCH_HAT = 2, HW_PALLU = 3, HW_HORNS = 4, HW_CROWN = 5;

    public int kind = MAN;
    public boolean female;
    public float height = 1f;        // relative: adult man = 1
    public float girth = 1f;
    public int skin = 0xFFD9A066;
    public int hairColor = 0xFF1E1410;
    public int hair = H_SHORT;
    public boolean curly;
    public int ribbon1 = 0, ribbon2 = 0;
    public int outfit = O_KURTA;
    public int primary = 0xFF3F6FB5, secondary = 0xFFE8C04A, accent = 0xFFFFFFFF;
    public int headwear = HW_NONE;
    public int headColor = 0xFFC62828, headBand = 0;
    public boolean kalgi;
    public int mustache = 0;         // 0 none, 1 soldier, 2 big curled
    public boolean beard;
    public int bindi = 0, tilak = 0;
    public boolean sindoor;
    public boolean scar, fangs, glowEyes, wrinkles, crookedNose, dimples, longNails;
    public int eyeColor = 0xFF2B1B10;
    public int furColor = 0xFF3A2A20;
    public boolean sword, katar, spear, shield, wand, axe, mace, satchel, chains;
    public int necklace = 0;         // 0 none, 1 gold, 2 pearl, 3 bones, 4 royal
    public boolean bangles, earrings, anklets;
    public int shoeColor = 0xFF5A3A22;
    public int variant;              // small per-character variation (guards)
    public boolean hero = true;      // villains placed right side

    public Look copy() {
        Look l = new Look();
        l.kind = kind; l.female = female; l.height = height; l.girth = girth; l.skin = skin; l.hairColor = hairColor;
        l.hair = hair; l.curly = curly; l.ribbon1 = ribbon1; l.ribbon2 = ribbon2; l.outfit = outfit; l.primary = primary;
        l.secondary = secondary; l.accent = accent; l.headwear = headwear; l.headColor = headColor; l.headBand = headBand;
        l.kalgi = kalgi; l.mustache = mustache; l.beard = beard; l.bindi = bindi; l.tilak = tilak; l.sindoor = sindoor;
        l.scar = scar; l.fangs = fangs; l.glowEyes = glowEyes; l.wrinkles = wrinkles; l.crookedNose = crookedNose;
        l.dimples = dimples; l.longNails = longNails; l.eyeColor = eyeColor; l.furColor = furColor; l.sword = sword;
        l.katar = katar; l.spear = spear; l.shield = shield; l.wand = wand; l.axe = axe; l.mace = mace;
        l.satchel = satchel; l.chains = chains; l.necklace = necklace; l.bangles = bangles; l.earrings = earrings;
        l.anklets = anklets; l.shoeColor = shoeColor; l.variant = variant; l.hero = hero;
        return l;
    }

    public boolean isChild() { return kind == GIRL || kind == BOY; }
    public boolean isHumanoid() { return kind != MONKEY; }
}
