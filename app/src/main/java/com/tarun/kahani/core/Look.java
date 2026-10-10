package com.tarun.kahani.core;

/** Visual design of one cartoon character, derived from the script's description. */
public final class Look {
    // body kinds
    public static final int GIRL = 0, WOMAN = 1, MAN = 2, BOY = 3, WITCH = 4, MONSTER = 5, MONKEY = 6, OLD_MAN = 7, ANIMAL = 8, BIRD = 9;
    // species for ANIMAL / BIRD
    public static final int SP_FOX = 0, SP_LION = 1, SP_BEAR = 2, SP_ELEPHANT = 3, SP_RABBIT = 4, SP_CAT = 5, SP_DOG = 6, SP_DEER = 7,
            SP_GOAT = 8, SP_COW = 9, SP_TIGER = 10, SP_WOLF = 11, SP_MOUSE = 12, SP_TORTOISE = 13, SP_HORSE = 14,
            SP_CROW = 20, SP_SPARROW = 21, SP_PARROT = 22, SP_PEACOCK = 23, SP_OWL = 24, SP_HEN = 25, SP_EAGLE = 26, SP_DUCK = 27;
    // outfits
    public static final int O_LEHENGA = 0, O_SALWAR = 1, O_SAREE = 2, O_ACHKAN = 3, O_UNIFORM = 4, O_CLOAK = 5,
            O_ARMOR = 6, O_JACKET = 7, O_KURTA = 8, O_FROCK = 9,
            O_HOODIE = 10,   // oversized hoodie with a pocket and drawstrings, jeans
            O_TSHIRT = 11,   // t-shirt (short sleeves) and shorts
            O_SUIT = 12,     // formal suit: jacket with lapels, shirt, tie, trousers
            O_COAT = 13,     // long coat (a trench coat), trousers
            O_JEANS = 14;    // a kurta or top with jeans
    // hair
    public static final int H_NONE = 0, H_BRAID = 1, H_PIGTAILS = 2, H_BUN = 3, H_SHORT = 4, H_LONG = 5, H_PONYTAIL = 6;
    // headwear
    public static final int HW_NONE = 0, HW_TURBAN = 1, HW_WITCH_HAT = 2, HW_PALLU = 3, HW_HORNS = 4, HW_CROWN = 5, HW_HOOD = 6;
    // gadgets held when nothing else is in the hands
    public static final int GD_NONE = 0, GD_PHONE = 1, GD_CONTROLLER = 2, GD_LAPTOP = 3;

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
    /** Wings on the back (a fairy, an angel, a butterfly child): drawn behind the figure in its views. */
    public boolean wings;
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
    public int species = -1;
    /** Body language (§26): 1 normal, more for lively characters (big, quick gestures), less for calm or shy ones. */
    public float energy = 1f;
    /** 1 = upright and proud (chin up, chest out), 0 normal, -1 = closed and shy (shoulders in, head down). */
    public float poise = 0f;
    public boolean hero = true;      // villains placed right side
    /** 0 none, 1 round spectacles, 2 dark / AR glasses (glowing red when glowGlasses). */
    public int glasses;
    public boolean glowGlasses;
    /** A gadget in the hand when idle (GD_*); a selfie stick is a wand with techWand. */
    public int gadget;
    public boolean techWand;
    /** A robot (an animal of metal with LED screens for eyes). */
    public boolean robot;
    /** Small lights: an LED clip in the hair, earphones around the neck, light-up shoes. */
    public boolean ledClip, earphones, lightShoes;
    /** v34: how many heads (Ravana's ten, a three-headed dragon's three); the central head is the face that speaks. */
    public int heads = 1;
    /** v34: how many arms (Durga's eight, a four-armed god's four); the front pair holds things, the others fan out behind. */
    public int arms = 2;
    /** v34: the animal the character rides or sits on (a species, SP_*), or -1: Durga on her lion, Ganesha on his mouse. */
    public int mount = -1;

    /** The word for a species, in English or Hindi. */
    public static String speciesWord(int sp, boolean hi) {
        switch (sp) {
            case SP_FOX: return hi ? "लोमड़ी" : "fox"; case SP_LION: return hi ? "शेर" : "lion"; case SP_BEAR: return hi ? "भालू" : "bear";
            case SP_ELEPHANT: return hi ? "हाथी" : "elephant"; case SP_RABBIT: return hi ? "खरगोश" : "rabbit"; case SP_CAT: return hi ? "बिल्ली" : "cat";
            case SP_DOG: return hi ? "कुत्ता" : "dog"; case SP_DEER: return hi ? "हिरण" : "deer"; case SP_GOAT: return hi ? "बकरी" : "goat";
            case SP_COW: return hi ? "बैल" : "bull"; case SP_TIGER: return hi ? "बाघ" : "tiger"; case SP_WOLF: return hi ? "भेड़िया" : "wolf";
            case SP_MOUSE: return hi ? "चूहा" : "mouse"; case SP_TORTOISE: return hi ? "कछुआ" : "tortoise"; case SP_HORSE: return hi ? "घोड़ा" : "horse";
            case SP_CROW: return hi ? "कौआ" : "crow"; case SP_SPARROW: return hi ? "चिड़िया" : "sparrow"; case SP_PARROT: return hi ? "तोता" : "parrot";
            case SP_PEACOCK: return hi ? "मोर" : "peacock"; case SP_OWL: return hi ? "उल्लू" : "owl"; case SP_HEN: return hi ? "मुर्गी" : "hen";
            case SP_EAGLE: return hi ? "चील" : "eagle"; case SP_DUCK: return hi ? "हंस" : "swan";
            default: return hi ? "जानवर" : "animal";
        }
    }

    /** The fur colour an animal of this species usually has (for a mount drawn from words). */
    public static int furOf(int sp) {
        switch (sp) {
            case SP_LION: return 0xFFC9963A; case SP_TIGER: return 0xFFE08A2E; case SP_BEAR: return 0xFF5A3A22; case SP_ELEPHANT: return 0xFF8E8E8E;
            case SP_RABBIT: return 0xFFE8E0D6; case SP_CAT: return 0xFF9A8A7A; case SP_DOG: return 0xFFB08A5A; case SP_DEER: return 0xFFB5793F;
            case SP_GOAT: return 0xFFD9D2C6; case SP_COW: return 0xFFE8E4DC; case SP_WOLF: return 0xFF7A7A80; case SP_MOUSE: return 0xFF9E9E9E;
            case SP_TORTOISE: return 0xFF5E7A3A; case SP_HORSE: return 0xFF6B4A2A; case SP_PEACOCK: return 0xFF1E6FA8; case SP_OWL: return 0xFF8A6A4A;
            case SP_DUCK: return 0xFFF4F4F0; case SP_PARROT: return 0xFF3FA34D; case SP_EAGLE: return 0xFF6A4A2A; case SP_CROW: return 0xFF222222;
            default: return 0xFF3A2A20;
        }
    }

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
        l.anklets = anklets; l.shoeColor = shoeColor; l.variant = variant; l.hero = hero; l.species = species;
        l.energy = energy; l.poise = poise; l.heads = heads; l.arms = arms; l.mount = mount;
        l.glasses = glasses; l.glowGlasses = glowGlasses; l.gadget = gadget; l.techWand = techWand; l.robot = robot;
        l.ledClip = ledClip; l.earphones = earphones; l.lightShoes = lightShoes;
        return l;
    }

    public boolean isChild() { return kind == GIRL || kind == BOY; }
    public boolean isHumanoid() { return kind != MONKEY && kind != ANIMAL && kind != BIRD; }
}
