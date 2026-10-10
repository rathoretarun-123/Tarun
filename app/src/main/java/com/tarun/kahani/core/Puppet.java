package com.tarun.kahani.core;

/**
 * Draws a character as a 2D cartoon puppet. Origin = point between the feet, y grows downward.
 * All characters are drawn fully clothed and child-friendly.
 */
public final class Puppet {
    private Puppet() {}

    static final int INK = 0xFF2B1D16;

    static int shade(int c, float f) { return LookDesigner.darker(c, f); }

    static int lighten(int c, float f) {
        int r = (c >> 16) & 255, g = (c >> 8) & 255, b = c & 255;
        r = (int) (r + (255 - r) * f); g = (int) (g + (255 - g) * f); b = (int) (b + (255 - b) * f);
        return 0xFF000000 | (r << 16) | (g << 8) | b;
    }

    static int mix(int a, int b, float t) {
        int ar = (a >> 16) & 255, ag = (a >> 8) & 255, ab = a & 255;
        int br = (b >> 16) & 255, bg = (b >> 8) & 255, bb = b & 255;
        return 0xFF000000 | ((int) (ar + (br - ar) * t) << 16) | ((int) (ag + (bg - ag) * t) << 8) | (int) (ab + (bb - ab) * t);
    }

    static int alpha(int c, float a) { return (((int) (Math.max(0, Math.min(1, a)) * 255)) << 24) | (c & 0xFFFFFF); }

    /** Pixel height of a character for a given stage scale. */
    public static float heightPx(Look l, float stageUnit) { return l.height * stageUnit; }

    // ------------------------------------------------------------------ entry point

    public static void draw(Gfx g, Look l, Pose p, float H) {
        g.save();
        g.translate(0, p.bob);
        if (p.body == Pose.LIE) {
            // v35: by the lie amount (lying back, sitting up), onto the mattress of a bed
            float L = p.lie > 0 ? p.lie : 1f;
            if (p.seat == Film.SEAT_BED) g.translate(0, -H * 0.27f * L);
            g.translate(0, -H * 0.09f * L);
            g.rotate((p.facing > 0 ? -88 : 88) * L);
            g.translate(0, H * 0.5f * L);
        } else if (p.body == Pose.HANG) {
            g.rotate(180);
        }
        if (p.tilt != 0) g.rotate(p.tilt * p.facing);
        if (p.squash != 1f) g.scale(1f / p.squash, p.squash);
        if (l.kind == Look.MONKEY) drawMonkey(g, l, p, H);
        else if (l.kind == Look.ANIMAL) drawAnimal(g, l, p, H);
        else if (l.kind == Look.BIRD) drawBird(g, l, p, H);
        else if (l.kind == Look.WITCH && p.disguised) drawHuman(g, disguise(l), p, H * 0.95f);
        else if (l.mount >= 0 && p.body != Pose.LIE && p.body != Pose.HANG) drawRider(g, l, p, H);
        else drawHuman(g, l, p, H);
        g.restore();
    }

    private static Look disguiseCache, disguiseSrc;

    /** The witch's "sweet old seller" disguise: grey saree, yellow shawl over the head, kind face. */
    static Look disguise(Look w) {
        if (disguiseSrc == w && disguiseCache != null) return disguiseCache;
        Look d = w.copy();
        d.kind = Look.WOMAN; d.female = true; d.girth = 0.95f;
        d.outfit = Look.O_SAREE; d.primary = 0xFF7D7A80; d.secondary = 0xFFE0B040;
        d.headwear = Look.HW_PALLU; d.hair = Look.H_BUN; d.hairColor = 0xFFBDBDBD;
        d.skin = 0xFFC99772; d.crookedNose = false; d.glowEyes = false; d.wrinkles = true; d.eyeColor = 0xFF4E342E;
        d.wand = false; d.longNails = false; d.necklace = 0; d.bindi = 0; d.earrings = true;
        disguiseSrc = w; disguiseCache = d;
        return d;
    }

    // ------------------------------------------------------------------ geometry helpers

    static final class Body {
        float H, headR, L, T, neck, sw, hw, hipY, shY, headY, armLen, armW, legW;
        /** v35: where the floor is in the body's own frame (0 standing; above 0 for one seated on a seat). */
        float floorY;
        /** v35: seated on a known seat (hips on its top, feet on the floor): its height above the floor, else -1. */
        float seatH = -1;
        /** v35: how far down onto the seat (0 standing .. 1 seated): sitting down and getting up move through it. */
        float sitU;
    }

    /** v35: furniture is drawn for the sitter's legs — a child's chair is smaller than a grown-up's (legs 0.29 of the height, not 0.40). */
    public static float seatScale(Look l) {
        if (l == null) return 1;
        float L = l.isChild() ? 0.29f : l.kind == Look.MONSTER ? 0.33f : l.kind == Look.WITCH ? 0.37f : 0.40f;
        return L / 0.40f;
    }

    /** v35: how far down onto a known seat the pose is — sitting down and getting up pass through it (0 = not seated). */
    static float sitShare(Pose p) {
        if (seatHeight(p.seat) < 0) return 0;
        if (p.body == Pose.SIT) return p.sit > 0 ? Math.min(1, p.sit) : 1;
        if (p.body == Pose.STAND && p.sit > 0) return Math.min(1, p.sit);
        return 0;
    }

    /**
     * v35: the height of a seat's top as a share of the sitter's height — where the hips rest. A chair, a stool and a
     * wheelchair 0.30, a throne's cushion 0.335, a bed's mattress 0.28, a sofa's cushion 0.27 (it gives a little), a
     * rock 0.28, the floor 0.15 (cross-legged, the folded legs under the hips); -1 for no seat (a rider's legs hang
     * down the animal's sides). A child's are smaller in proportion (seatScale).
     */
    static float seatHeight(int seat) {
        switch (seat) {
            case Film.SEAT_FLOOR: return 0.15f;
            case Film.SEAT_CHAIR: case Film.SEAT_STOOL: case Film.SEAT_WHEELCHAIR: return 0.30f;
            case Film.SEAT_THRONE: return 0.335f;
            case Film.SEAT_SOFA: return 0.27f;
            case Film.SEAT_BED: case Film.SEAT_ROCK: return 0.28f;
            default: return -1;
        }
    }

    static Body body(Look l, float H) {
        Body b = new Body();
        b.H = H;
        if (l.isChild()) { b.headR = 0.165f * H; b.L = 0.29f * H; b.T = 0.29f * H; b.neck = 0.02f * H; }
        else if (l.kind == Look.MONSTER) { b.headR = 0.125f * H; b.L = 0.33f * H; b.T = 0.38f * H; b.neck = 0f; }
        else if (l.kind == Look.WITCH) { b.headR = 0.125f * H; b.L = 0.37f * H; b.T = 0.31f * H; b.neck = 0.02f * H; }
        else { b.headR = 0.12f * H; b.L = 0.40f * H; b.T = 0.31f * H; b.neck = 0.03f * H; }
        float girth = l.girth;
        b.sw = b.T * (l.isChild() ? 0.42f : 0.46f) * girth;
        if (l.female && !l.isChild()) b.sw *= 0.9f;
        b.hw = b.sw * (l.female ? 0.95f : 0.82f);
        b.hipY = -b.L;
        b.shY = -(b.L + b.T);
        b.headY = b.shY - b.neck - b.headR * 0.92f;
        b.armLen = b.T * 1.02f;
        b.armW = b.headR * (l.kind == Look.MONSTER ? 0.5f : 0.36f);
        b.legW = b.headR * (l.kind == Look.MONSTER ? 0.5f : 0.42f);
        return b;
    }

    static float[] hand(Body b, float side, float armDeg, float elbowDeg) {
        float sx = side * b.sw * 0.92f, sy = b.shY + b.armW * 0.6f;
        double a = Math.toRadians(armDeg);
        float ex = sx + side * (float) Math.sin(a) * b.armLen * 0.5f;
        float ey = sy + (float) Math.cos(a) * b.armLen * 0.5f;
        double a2 = Math.toRadians(armDeg + elbowDeg);
        float hx = ex + side * (float) Math.sin(a2) * b.armLen * 0.5f;
        float hy = ey + (float) Math.cos(a2) * b.armLen * 0.5f;
        return new float[]{sx, sy, ex, ey, hx, hy};
    }

    // ------------------------------------------------------------------ a rider (v34)

    /** The animal the character rides, from the species: its fur colour, its kind (a bird for a peacock, a swan, an owl). */
    static Look mountLook(Look l) {
        Look m = new Look();
        m.kind = l.mount >= 20 ? Look.BIRD : Look.ANIMAL;
        m.species = l.mount;
        m.furColor = Look.furOf(l.mount);
        m.skin = shade(m.furColor, 1.25f);
        m.eyeColor = 0xFF2B1B10;
        m.hero = l.hero;
        return m;
    }

    /**
     * v34: Durga on her lion — the animal walks or stands below, the character sits on its back (legs folded) at
     * three quarters of the height; the whole stays H tall. The mount faces the way the rider faces.
     */
    static void drawRider(Gfx g, Look l, Pose p, float H) {
        Look m = mountLook(l);
        float Hm = H * (m.kind == Look.BIRD ? 0.5f : 0.55f);
        Pose pm = new Pose();
        pm.facing = p.facing; pm.walk = p.walk; pm.walkAmt = p.walkAmt; pm.time = p.time; pm.emotion = p.emotion; pm.blink = p.blink;
        pm.mouth = p.mountMouth;                         // the animal speaks with its own jaw
        Pose pr0 = p;
        if (m.kind == Look.BIRD) drawBird(g, m, pm, Hm); else drawAnimal(g, m, pm, Hm);
        // the back of the animal: its body's top (the body sits at 0.6 of its height with a 0.27 radius; a rabbit or a mouse lower)
        float back = (m.species == Look.SP_RABBIT || m.species == Look.SP_MOUSE) ? Hm * 0.72f : m.kind == Look.BIRD ? Hm * 0.6f : Hm * 0.85f;
        Pose pr = pr0.copyFor(Pose.SIT);
        pr.mountMouth = 0;
        pr.seat = -1;                                    // v35: astride, the legs hang down the animal's sides
        g.save();
        g.translate(0, -back);
        drawHuman(g, l, pr, H * 0.78f);
        g.restore();
    }

    // ------------------------------------------------------------------ human

    static void drawHuman(Gfx g, Look l, Pose p, float H) {
        Body b = body(l, H);
        boolean witch = l.kind == Look.WITCH;
        boolean monster = l.kind == Look.MONSTER;
        float lift = 0;
        // v35: seated on a known seat the hips rest on its top and the feet on the floor (they used to sink below it)
        float su = sitShare(p);
        if (su > 0) { b.seatH = seatHeight(p.seat) * H * seatScale(l); b.sitU = su; lift = (b.L - b.seatH) * su; b.floorY = -lift; }
        else if (p.body == Pose.SIT) lift = b.L * 0.55f;
        else if (p.body == Pose.KNEEL) lift = b.L * 0.48f;
        else if (p.body == Pose.CROUCH) lift = b.L * 0.22f;
        g.save();
        g.translate(0, lift);

        // Shadow on ground
        if (p.body != Pose.LIE && p.body != Pose.HANG) {
            g.color(0x33000000);
            g.oval(0, -lift + 2, b.sw * 1.6f, b.headR * 0.28f);
        }

        // Hair behind head (long braid / pigtails hanging down back)
        drawBackHair(g, l, p, b);

        // Legs
        drawLegs(g, l, p, b);

        // v34: a many-armed character (Durga): the other pairs of arms fan out behind the body, raised a little more
        // with each pair, the farthest drawn first; the front pair (below) acts and holds things
        int pairs = Math.max(1, l.arms / 2);
        for (int k = pairs - 1; k >= 1; k--) {
            float raise = Math.min(150, 30 + 24 * k);
            drawArm(g, l, p, b, -1, raise, 18, 0);
            drawArm(g, l, p, b, 1, raise, 18, 0);
        }

        // Outfit body
        drawOutfit(g, l, p, b);

        // Props on body (belt, scabbard, satchel)
        drawBodyProps(g, l, p, b);

        // Head (the witch is hunched: head forward and lower, with a hump)
        g.save();
        if (witch && !p.disguised) {
            g.color(shade(l.primary, 0.9f));
            g.oval(-p.facing * b.sw * 0.45f, b.shY + b.T * 0.08f, b.sw * 0.75f, b.T * 0.28f);
            g.translate(p.facing * b.headR * 0.35f, b.headR * 0.3f);
        }
        g.translate(p.facing * b.headR * 0.04f, b.headY);
        if (p.headTilt != 0) g.rotate(p.headTilt * p.facing);
        // v34: a many-headed character (Ravana): the other heads in a row behind the main one, the farther drawn
        // first, a little lower and smaller; they share the face but keep their mouths shut — the central head speaks
        int extra = Math.min(9, l.heads - 1);
        if (extra > 0) {
            Pose q = new Pose();
            q.emotion = p.emotion; q.facing = p.facing; q.time = p.time; q.blink = p.blink; q.disguised = p.disguised;
            for (int i = extra; i >= 1; i--) {
                int side = i % 2 == 1 ? -1 : 1, k = (i + 1) / 2;
                g.save();
                g.translate(side * k * b.headR * 1.7f, k * b.headR * 0.12f);
                float sc = 1f - 0.04f * k;
                g.scale(sc, sc);
                drawHead(g, l, q, b);
                g.restore();
            }
        }
        drawHead(g, l, p, b);
        g.restore();

        // Arms in front (v34: an arm in a sling stays across the waist; an open umbrella is held up over the head)
        int noArm = l.missingArm();
        if (noArm == -1) drawEmptySleeve(g, l, p, b, -1);
        else if ((l.injury & Look.INJ_ARM) != 0 && p.body != Pose.LIE && p.body != Pose.HANG) drawSlingArm(g, l, p, b);
        else drawArm(g, l, p, b, -1, p.armL, p.elbowL, noArm == 1 && p.holdL == Pose.I_NONE ? p.holdR : p.holdL);
        if (noArm == 1) drawEmptySleeve(g, l, p, b, 1);
        else if (p.umbrellaOpen) drawUmbrellaArm(g, l, p, b);
        else drawArm(g, l, p, b, 1, p.armR, p.elbowR, noArm == -1 && p.holdR == Pose.I_NONE ? p.holdL : p.holdR);
        if (l.aid == Look.AID_WALKER && (p.body == Pose.STAND || p.body == Pose.CROUCH)) drawWalker(g, l, p, b);

        // Spear or wand held at the side when not gesturing
        g.restore();
    }

    /**
     * v35: a missing arm — the sleeve is there, short and empty, its end folded up and pinned at the shoulder (a
     * sleeveless outfit shows the rounded shoulder only). It sways a little as the body walks.
     */
    static void drawEmptySleeve(Gfx g, Look l, Pose p, Body b, int side) {
        float sx = side * b.sw * 0.92f, sy = b.shY + b.armW * 0.6f;
        boolean sleeveless = l.outfit == Look.O_LEHENGA || l.outfit == Look.O_SAREE || l.outfit == Look.O_TSHIRT;
        int sleeve = l.outfit == Look.O_ARMOR ? l.furColor : l.primary;
        float sway = (float) Math.sin(p.walk) * p.walkAmt * b.armW * 0.4f;
        float ex = sx + side * b.armW * 0.15f + sway, ey = sy + b.armLen * (sleeveless ? 0.12f : 0.3f);
        g.color(shade(sleeve, 0.7f));
        g.line(sx, sy, ex, ey, b.armW * 2.1f);
        g.color(sleeve);
        g.line(sx, sy, ex, ey, b.armW * 1.8f);
        if (!sleeveless) {
            // the fold and the pin
            g.color(shade(sleeve, 0.8f));
            g.line(ex - b.armW * 0.75f, ey, ex + b.armW * 0.75f, ey - b.armW * 0.3f, b.armW * 0.45f);
            g.color(0xFFB0BEC5);
            g.oval(ex, ey - b.armW * 0.35f, b.armW * 0.18f, b.armW * 0.18f);
        }
    }

    /** v34: the left arm in a sling — the upper arm hangs, the forearm in white plaster lies across the waist in a pale blue sling tied round the neck. */
    static void drawSlingArm(Gfx g, Look l, Pose p, Body b) {
        float sx = -b.sw * 0.92f, sy = b.shY + b.armW * 0.6f;
        float ex = sx - b.armW * 0.15f, ey = sy + b.armLen * 0.47f;
        float hx = b.sw * 0.38f, hy = ey - b.armLen * 0.07f;
        int sleeve = l.outfit == Look.O_ARMOR ? l.furColor : l.primary;
        g.color(shade(sleeve, 0.7f));
        g.line(sx, sy, ex, ey, b.armW * 2.15f);
        g.color(sleeve);
        g.line(sx, sy, ex, ey, b.armW * 1.85f);
        // the strap round the neck
        g.color(0xFF6F98C4);
        g.line(hx - b.armW * 0.3f, hy - b.armW * 0.5f, b.headR * 0.32f, b.shY + b.armW * 0.2f, Math.max(2, b.armW * 0.42f));
        g.line(ex + b.armW * 0.4f, ey - b.armW * 0.4f, -b.headR * 0.32f, b.shY + b.armW * 0.2f, Math.max(2, b.armW * 0.42f));
        // the sling: a narrow cloth triangle, its point at the elbow, just under the forearm
        g.color(0xFF90B8E0);
        g.begin();
        g.moveTo(ex - b.armW * 0.9f, ey + b.armW * 0.15f);
        g.lineTo(hx + b.armW * 0.35f, hy - b.armW * 0.55f);
        g.lineTo(hx + b.armW * 0.35f, hy + b.armW * 1.05f);
        g.close(); g.fillPath();
        g.color(0xFF6F98C4);
        g.begin();
        g.moveTo(ex - b.armW * 0.9f, ey + b.armW * 0.15f);
        g.lineTo(hx + b.armW * 0.35f, hy + b.armW * 1.05f);
        g.strokePath(Math.max(1.5f, b.armW * 0.16f));
        // the forearm in white plaster over it, the hand coming out of the plaster
        g.color(0xFFB8B6AE);
        g.line(ex, ey - b.armW * 0.1f, hx, hy - b.armW * 0.1f, b.armW * 1.75f);
        g.color(0xFFF7F6F2);
        g.line(ex, ey - b.armW * 0.1f, hx, hy - b.armW * 0.1f, b.armW * 1.5f);
        g.color(0xFFE0D2B4);
        g.line(hx - b.armW * 0.1f, hy - b.armW * 0.85f, hx - b.armW * 0.1f, hy + b.armW * 0.65f, b.armW * 0.3f);
        g.color(faceSkin(l, p));
        g.oval(hx + b.armW * 0.6f, hy - b.armW * 0.1f, b.armW * 0.75f, b.armW * 0.68f);
    }

    /** v34: the right arm raised, holding an open umbrella over the head (its colour against the clothes). */
    static void drawUmbrellaArm(Gfx g, Look l, Pose p, Body b) {
        drawArm(g, l, p, b, 1, 152, 14, Pose.I_NONE);
        float[] h = hand(b, 1, 152, 14);
        float top = b.headY - b.headR - b.H * 0.12f, cx = h[4] * 0.45f, R = b.H * 0.3f;
        drawUmbrella(g, cx, top, R, h[4], h[5] + b.H * 0.07f, umbrellaColor(l), p.facing, p.time, p.wind);
    }

    static int umbrellaColor(Look l) {
        int c = l.primary;
        int r = (c >> 16) & 255, gg = (c >> 8) & 255, bl = c & 255;
        boolean reddish = r > gg + 40 && r > bl + 40;
        return reddish ? 0xFF1E6FD0 : 0xFFD8322F;
    }

    /** An open umbrella: a scalloped dome (apex at cx, top) of radius R, its shaft down to (hx, hy) with a crook. */
    static void drawUmbrella(Gfx g, float cx, float top, float R, float hx, float hy, int color, float facing, float time, float wind) {
        float sway = (float) Math.sin(time * 1.7f) * 0.025f + wind * 0.04f;
        float ax = cx + sway * R, base = top + R * 0.42f;
        g.color(0xFF4E342E);
        g.line(ax, top, hx, hy, Math.max(2, R * 0.03f));
        g.begin(); g.moveTo(hx, hy); g.quadTo(hx, hy + R * 0.14f, hx - facing * R * 0.1f, hy + R * 0.1f); g.strokePath(Math.max(2, R * 0.035f));
        g.begin();
        g.moveTo(ax - R, base);
        g.cubicTo(ax - R * 0.95f, top - R * 0.05f, ax + R * 0.95f, top - R * 0.05f, ax + R, base);
        for (int i = 4; i >= 1; i--) {
            float x0 = ax - R + R * 0.5f * i, x1 = ax - R + R * 0.5f * (i - 1);
            g.quadTo((x0 + x1) / 2, base - R * 0.14f, x1, base);
        }
        g.close();
        g.color(color); g.fillPath();
        g.color(shade(color, 0.72f)); g.strokePath(Math.max(1.5f, R * 0.02f));
        // the ribs and a light sheen
        g.color(shade(color, 0.8f));
        for (int i = 1; i <= 3; i++) {
            float x = ax - R + R * 0.5f * i;
            g.begin(); g.moveTo(ax, top + R * 0.02f); g.quadTo((ax + x) / 2 + (x - ax) * 0.2f, top + R * 0.12f, x, base); g.strokePath(Math.max(1, R * 0.015f));
        }
        g.color(alpha(0xFFFFFFFF, 0.22f));
        g.oval(ax - R * 0.35f, top + R * 0.15f, R * 0.22f, R * 0.08f);
        g.color(0xFF4E342E);
        g.oval(ax, top - R * 0.03f, R * 0.035f, R * 0.05f);
    }

    /** v34: a walking frame in front: two side frames from the hands down to rubber feet, joined at the front. */
    static void drawWalker(Gfx g, Look l, Pose p, Body b) {
        float[] hl = hand(b, -1, p.armL, p.elbowL), hr = hand(b, 1, p.armR, p.elbowR);
        float fwd = p.facing * b.H * 0.1f, w = Math.max(2.5f, b.armW * 0.42f);
        float gy = Math.min(hl[5], hr[5]);
        for (int s2 = 0; s2 < 2; s2++) {
            float x = s2 == 0 ? hl[4] : hr[4];
            g.color(0xFF78909C);
            g.line(x, gy, x - p.facing * b.armW * 0.2f, -b.legW * 0.3f, w);                // the back leg
            g.line(x + fwd, gy + b.armW * 0.15f, x + fwd * 1.15f, -b.legW * 0.3f, w);         // the front leg
            g.color(0xFFB0BEC5);
            g.line(x - p.facing * b.armW * 0.3f, gy, x + fwd, gy + b.armW * 0.15f, w * 1.1f); // the side rail
            g.color(0xFF37474F);
            g.line(x - p.facing * b.armW * 0.35f, gy - w * 0.2f, x + p.facing * b.armW * 0.5f, gy - w * 0.2f, w * 1.6f);   // the grip
            g.oval(x - p.facing * b.armW * 0.2f, -b.legW * 0.2f, w * 1.1f, w * 0.7f);
            g.oval(x + fwd * 1.15f, -b.legW * 0.2f, w * 1.1f, w * 0.7f);
        }
        g.color(0xFF90A4AE);
        g.line(hl[4] + fwd, gy + b.armW * 0.2f, hr[4] + fwd, gy + b.armW * 0.2f, w);
        g.line(hl[4] + fwd * 1.08f, gy * 0.45f, hr[4] + fwd * 1.08f, gy * 0.45f, w);
    }

    static void drawLegs(Gfx g, Look l, Pose p, Body b) {
        boolean longSkirt = l.outfit == Look.O_LEHENGA || l.outfit == Look.O_SAREE || l.outfit == Look.O_CLOAK;
        float stride = (float) Math.sin(p.walk) * p.walkAmt;
        int legColor;
        switch (l.outfit) {
            case Look.O_SALWAR: legColor = l.secondary; break;
            case Look.O_ACHKAN: legColor = 0xFFF1EBDD; break;
            case Look.O_UNIFORM: legColor = l.secondary; break;
            case Look.O_ARMOR: legColor = l.furColor; break;
            case Look.O_KURTA: legColor = 0xFFF1EBDD; break;
            case Look.O_HOODIE: case Look.O_JEANS: legColor = 0xFF3B5B8A; break;          // denim
            case Look.O_TSHIRT: legColor = faceSkin(l, p); break;                        // bare legs under shorts
            case Look.O_SUIT: legColor = shade(l.primary, 0.9f); break;
            case Look.O_COAT: legColor = 0xFF26262B; break;
            default: legColor = shade(l.primary, 0.8f);
        }
        if (l.kind == Look.MONSTER) legColor = l.furColor;
        if (l.kind == Look.MONSTER && p.body != Pose.SIT && p.body != Pose.KNEEL) { monsterLegs(g, l, p, b, stride); return; }
        int noLeg = l.missingLeg();
        boolean artificial = (l.condition & Look.C_ARTIFICIAL_LEG) != 0;
        for (int side = -1; side <= 1; side += 2) {
            float hx = side * b.hw * 0.45f;
            float fx, fy, kneeX = Float.NaN, kneeY = 0;
            if (side == noLeg && !artificial && l.kind != Look.MONSTER) {
                // v35: a leg missing above the knee: the trouser leg folded under and pinned, no foot (crutches carry the step)
                if (!longSkirt || p.body == Pose.SIT || p.body == Pose.LIE) {
                    float kx = p.body == Pose.SIT ? hx + p.facing * b.L * 0.32f : hx + side * stride * b.L * 0.06f;
                    float ky = p.body == Pose.SIT ? b.hipY + b.legW * 0.2f : b.hipY * 0.52f;
                    g.color(legColor);
                    g.line(hx, b.hipY, kx, ky, b.legW * 2f);
                    g.color(shade(legColor, 0.8f));
                    g.oval(kx, ky, b.legW * 1.05f, b.legW * 0.7f);
                    g.color(0xFFB0BEC5);
                    g.oval(kx + b.legW * 0.4f, ky - b.legW * 0.5f, b.legW * 0.16f, b.legW * 0.16f);
                }
                continue;
            }
            if (b.seatH >= 0) {
                // v35: seated (or on the way down or up, by sitU) — on a chair, a sofa, a bed, a stool: the thigh along
                // the seat to the knee, the shin straight down to the floor (to the footrest in a wheelchair); on the
                // floor cross-legged: the knees out to both sides near the floor, the feet tucked in under them
                float u = b.sitU, kx, ky, sfx, sfy;
                if (p.seat == Film.SEAT_FLOOR) {
                    kx = side * (b.hw * 0.9f + b.L * 0.42f) + p.facing * b.L * 0.08f; ky = b.floorY - b.legW * 1.15f;
                    sfx = -side * b.hw * 0.35f + p.facing * b.L * 0.06f; sfy = b.floorY - b.legW * 0.1f;
                } else {
                    boolean wc = p.seat == Film.SEAT_WHEELCHAIR;
                    kx = hx * 0.7f + p.facing * b.L * 0.55f; ky = b.hipY + b.legW * 0.25f;
                    sfx = wc ? p.facing * b.H * 0.27f + hx * 0.3f : kx + p.facing * b.L * 0.05f;
                    sfy = wc ? b.floorY - b.H * 0.065f : b.floorY;
                }
                // standing: the knee half way down the straight leg, the foot under the hip, on the floor
                kx = hx + (kx - hx) * u; ky = b.hipY * 0.5f + b.floorY * 0.5f + (ky - (b.hipY * 0.5f + b.floorY * 0.5f)) * u;
                fx = hx + (sfx - hx) * u; fy = b.floorY + (sfy - b.floorY) * u;
                kneeX = kx; kneeY = ky;
                if (!longSkirt || u > 0.25f) {
                    g.color(legColor);
                    g.line(hx, b.hipY, kx, ky, b.legW * (p.seat == Film.SEAT_FLOOR ? 2.1f : 2f));
                    g.line(kx, ky, fx, fy - b.legW * 0.6f, b.legW * 1.8f);
                }
            } else if (p.body == Pose.SIT) {
                fx = hx * 0.6f + p.facing * b.L * 0.75f; fy = -b.L * 0.45f + b.L * 0.5f;
                g.color(legColor);
                g.line(hx, b.hipY, fx - p.facing * b.L * 0.15f, b.hipY + b.legW * 0.2f, b.legW * 2f);
                g.line(fx - p.facing * b.L * 0.15f, b.hipY + b.legW * 0.2f, fx, fy, b.legW * 1.8f);
            } else if (p.body == Pose.KNEEL) {
                fx = hx - p.facing * b.L * 0.45f; fy = -b.L * 0.52f + b.L * 0.5f;
                g.color(legColor);
                g.line(hx, b.hipY, hx + p.facing * b.L * 0.1f, b.hipY + b.L * 0.5f, b.legW * 2f);
                g.line(hx + p.facing * b.L * 0.1f, b.hipY + b.L * 0.5f, fx, fy, b.legW * 1.8f);
            } else {
                // v34: a leg in plaster swings less (a stiff, short step)
                float sw = side * stride * b.L * 0.38f * (side > 0 && (l.injury & Look.INJ_LEG) != 0 ? 0.55f : 1f);
                fx = hx + sw; fy = 0;
                if (!longSkirt || p.walkAmt > 0.01f || p.body == Pose.LIE) {
                    g.color(legColor);
                    g.line(hx, b.hipY, fx, fy - b.legW * 0.6f, b.legW * 2f);
                }
            }
            if (side > 0 && (l.injury & Look.INJ_LEG) != 0 && p.body != Pose.KNEEL && (!longSkirt || p.walkAmt > 0.01f)) {
                // v34: the plaster from below the knee to the toes
                float kx, ky;
                if (!Float.isNaN(kneeX)) { kx = kneeX; ky = kneeY; }
                else if (p.body == Pose.SIT) { kx = fx - p.facing * b.L * 0.15f; ky = b.hipY + b.legW * 0.2f; }
                else { kx = hx + (fx - hx) * 0.5f; ky = b.hipY * 0.5f; }
                g.color(0xFFC9C7C0);
                g.line(kx, ky, fx, fy - b.legW * 0.5f, b.legW * 2.55f);
                g.color(0xFFF4F3EE);
                g.line(kx, ky, fx, fy - b.legW * 0.5f, b.legW * 2.3f);
                g.color(0xFFE0D2B4);
                g.line(kx - b.legW * 1.1f, ky, kx + b.legW * 1.1f, ky, b.legW * 0.35f);
            }
            if (side == noLeg && artificial && l.outfit == Look.O_TSHIRT && p.body != Pose.SIT && p.body != Pose.KNEEL) {
                // v35: an artificial leg below the knee shows under shorts: a slim metal pylon down to the shoe
                float kx = hx + (fx - hx) * 0.5f, ky = b.hipY * 0.5f;
                g.color(0xFF78909C);
                g.line(kx, ky, fx, fy - b.legW * 0.6f, b.legW * 0.9f);
                g.color(0xFFB0BEC5);
                g.line(kx, ky, fx, fy - b.legW * 0.6f, b.legW * 0.5f);
                g.color(0xFF455A64);
                g.oval(kx, ky, b.legW * 1.0f, b.legW * 0.55f);
            }
            if (l.outfit == Look.O_TSHIRT && p.body != Pose.SIT && p.body != Pose.KNEEL) {
                // shorts: the upper half of the leg in the shorts' colour
                g.color(l.secondary == 0 ? 0xFFC62828 : l.secondary);
                g.line(hx, b.hipY, hx + (fx - hx) * 0.42f, b.hipY + (fy - b.legW * 0.6f - b.hipY) * 0.42f, b.legW * 2.3f);
            }
            // foot / shoe
            if (l.kind == Look.MONSTER) {
                g.color(shade(l.furColor, 0.7f));
                g.oval(fx + p.facing * b.legW * 0.5f, fy - b.legW * 0.45f, b.legW * 1.5f, b.legW * 0.7f);
                g.color(0xFF4A2E1A);
                g.rect(fx - b.legW, fy - b.L * 0.45f, b.legW * 2f, b.legW * 0.35f);
                g.rect(fx - b.legW, fy - b.L * 0.25f, b.legW * 2f, b.legW * 0.35f);
            } else {
                g.color(l.shoeColor);
                g.begin();
                float fw = b.legW * 1.5f;
                g.moveTo(fx - fw * 0.7f, fy);
                g.lineTo(fx + p.facing * fw * 1.25f, fy);
                g.quadTo(fx + p.facing * fw * 1.5f, fy - fw * 0.45f, fx + p.facing * fw * 0.6f, fy - fw * 0.75f);
                g.lineTo(fx - fw * 0.7f, fy - fw * 0.75f);
                g.close();
                g.fillPath();
                if (l.outfit == Look.O_LEHENGA || l.outfit == Look.O_ACHKAN) { // juti curl
                    g.color(l.secondary);
                    g.oval(fx + p.facing * fw * 1.3f, fy - fw * 0.3f, fw * 0.18f, fw * 0.18f);
                }
                if (l.lightShoes) {
                    // light-up shoes: a little LED on the side that blinks with each step ("टिम-टिम")
                    float blink = p.walkAmt > 0.05f ? 0.5f + 0.5f * (float) Math.sin(p.walk * 2 + side) : 0.35f + 0.15f * (float) Math.sin(p.time * 3 + side);
                    g.color(alpha(0xFF40C4FF, 0.4f + 0.6f * blink));
                    g.oval(fx + p.facing * fw * 0.3f, fy - fw * 0.42f, fw * 0.22f, fw * 0.14f);
                    g.color(alpha(0xFFFFFFFF, 0.7f * blink));
                    g.oval(fx + p.facing * fw * 0.3f, fy - fw * 0.42f, fw * 0.1f, fw * 0.07f);
                }
            }
        }
    }

    static void monsterLegs(Gfx g, Look l, Pose p, Body b, float stride) {
        for (int side = -1; side <= 1; side += 2) {
            float hx = side * b.hw * 0.62f;
            float fx = hx + side * stride * b.L * 0.3f + side * b.legW * 0.2f;
            float kx = (hx + fx) / 2 + side * b.legW * 0.25f, ky = b.hipY * 0.48f;
            float tw = b.legW * 1.15f, sw2 = b.legW * 0.85f;
            g.begin();
            g.moveTo(hx - tw, b.hipY); g.lineTo(hx + tw, b.hipY);
            g.quadTo(kx + tw * 1.1f, ky, fx + sw2, -b.legW * 0.4f);
            g.lineTo(fx - sw2, -b.legW * 0.4f);
            g.quadTo(kx - tw * 1.1f, ky, hx - tw, b.hipY);
            g.close();
            g.color(shade(l.furColor, 0.55f)); g.strokePath(3);
            g.linear(hx - tw, 0, hx + tw, 0, shade(l.furColor, 0.85f), lighten(l.furColor, 0.08f)); g.fillPath();
            furStrokes(g, l.furColor, hx - tw, b.hipY, tw * 2, -b.hipY, p.seed + side);
            // leather straps
            g.color(0xFF3E2716);
            g.line(kx - tw, ky + b.L * 0.12f, kx + tw, ky + b.L * 0.08f, 5);
            g.line(fx - sw2, -b.L * 0.2f, fx + sw2, -b.L * 0.23f, 5);
            // clawed foot
            g.color(shade(l.furColor, 0.7f));
            g.oval(fx + p.facing * b.legW * 0.35f, -b.legW * 0.32f, b.legW * 1.15f, b.legW * 0.5f);
            g.color(0xFFEDE3C8);
            for (int k = 0; k < 3; k++) {
                float cx = fx + p.facing * (b.legW * 0.9f) + (k - 1) * b.legW * 0.35f;
                g.begin(); g.moveTo(cx - 4, -b.legW * 0.2f); g.lineTo(cx + p.facing * 9, 0); g.lineTo(cx + 4, -b.legW * 0.15f); g.close(); g.fillPath();
            }
        }
    }

    static void drawOutfit(Gfx g, Look l, Pose p, Body b) {
        float sw = b.sw, hw = b.hw, shY = b.shY, hipY = b.hipY;
        float waistY = shY + b.T * 0.62f;
        float sway = (float) Math.sin(p.time * 2.2f) * 0.03f + (float) Math.sin(p.walk) * p.walkAmt * 0.06f
                + p.wind * 0.07f * (1 + 0.35f * (float) Math.sin(p.time * 7 + p.seed));   // the dress blows in the wind
        switch (l.outfit) {
            case Look.O_LEHENGA: {
                // flared skirt to the floor
                float skirtW = hw * (l.isChild() ? 2.5f : 2.2f);
                float topY = waistY;
                // v35: seated, the skirt spans the waist to the floor (squeezed into the shorter drop)
                boolean seated = b.seatH >= 0 && topY < b.floorY;
                if (seated) { g.save(); g.translate(0, topY); g.scale(1, (b.floorY - topY) / -topY); g.translate(0, -topY); }
                g.begin();
                g.moveTo(-hw * 0.95f, topY);
                g.lineTo(hw * 0.95f, topY);
                g.quadTo(skirtW * 0.9f, -b.L * 0.4f, skirtW * (1 + sway), -2);
                g.quadTo(0, b.L * 0.06f, -skirtW * (1 - sway), -2);
                g.quadTo(-skirtW * 0.9f, -b.L * 0.4f, -hw * 0.95f, topY);
                g.close();
                g.color(shade(l.primary, 0.6f)); g.strokePath(3);
                g.linear(0, topY, 0, 0, lighten(l.primary, 0.12f), shade(l.primary, 0.85f));
                g.fillPath();
                // pleats
                g.color(alpha(shade(l.primary, 0.6f), 0.45f));
                for (int i = -3; i <= 3; i++) {
                    float x = i * skirtW * 0.27f;
                    g.line(x * 0.35f, topY + 6, x, -8, 2.2f);
                }
                // gota border band (secondary) with bells
                g.color(l.secondary);
                g.begin();
                g.moveTo(-skirtW * (1 - sway), -2);
                g.quadTo(0, b.L * 0.06f, skirtW * (1 + sway), -2);
                g.lineTo(skirtW * (1 + sway) * 0.97f, -b.L * 0.17f);
                g.quadTo(0, -b.L * 0.11f, -skirtW * (1 - sway) * 0.97f, -b.L * 0.17f);
                g.close();
                g.fillPath();
                g.color(0xFFE8C04A);
                g.line(-skirtW * 0.97f, -b.L * 0.17f, skirtW * 0.97f, -b.L * 0.17f, 2.5f);
                if (l.anklets) {
                    g.color(0xFFD9A93A);
                    for (int i = -4; i <= 4; i++) g.oval(i * skirtW * 0.22f, -1, 3.2f, 3.2f);
                }
                if (seated) g.restore();
                // choli + dupatta: full coverage top
                drawTop(g, l, p, b, waistY + b.T * 0.06f, l.secondary, true);
                break;
            }
            case Look.O_FROCK: {
                float skirtW = hw * 1.9f;
                g.begin();
                g.moveTo(-hw, waistY); g.lineTo(hw, waistY);
                g.lineTo(skirtW, -b.L * 0.45f); g.lineTo(-skirtW, -b.L * 0.45f); g.close();
                g.color(l.primary); g.fillPath();
                drawTop(g, l, p, b, waistY, l.primary, false);
                break;
            }
            case Look.O_SAREE: {
                // v35: seated, the saree falls from the waist over the knees to the floor (fl), not below it
                float skirtW = hw * 1.45f * (1 + 0.25f * b.sitU), fl = b.floorY, mid = -b.L * 0.5f + ((waistY + fl) / 2 + b.L * 0.5f) * b.sitU;
                g.begin();
                g.moveTo(-hw, waistY); g.lineTo(hw, waistY);
                g.quadTo(skirtW, mid, skirtW * (1 + sway), fl - 2);
                g.lineTo(-skirtW * (1 - sway), fl - 2);
                g.quadTo(-skirtW, mid, -hw, waistY);
                g.close();
                g.color(shade(l.primary, 0.55f)); g.strokePath(3);
                g.linear(0, waistY, 0, fl, l.primary, shade(l.primary, 0.8f)); g.fillPath();
                // pleats
                g.color(alpha(shade(l.primary, 0.55f), 0.5f));
                for (int i = 0; i < 4; i++) g.line(-hw * 0.2f + i * 6, waistY + b.T * 0.3f, -skirtW * 0.3f + i * 9, fl - 6, 2);
                // gold border
                g.color(l.secondary);
                g.rect(-skirtW * (1 - sway), fl - b.L * 0.12f, skirtW * 2 * (1 + sway * 0.5f), b.L * 0.1f);
                g.color(alpha(0xFFFFE9A0, 0.8f));
                for (int i = -5; i <= 5; i++) g.oval(i * skirtW * 0.18f, fl - b.L * 0.07f, 2.5f, 2.5f);
                drawTop(g, l, p, b, waistY, shade(l.primary, 0.9f), false);
                // pallu: diagonal drape from left hip over right shoulder
                g.begin();
                g.moveTo(-hw * 1.05f, waistY + b.T * 0.35f);
                g.lineTo(-hw * 0.3f, waistY + b.T * 0.42f);
                g.lineTo(sw * 0.95f, shY + b.T * 0.05f);
                g.lineTo(sw * 0.55f, shY - b.T * 0.02f);
                g.close();
                g.color(lighten(l.primary, 0.08f)); g.fillPath();
                g.color(l.secondary);
                g.line(-hw * 0.3f, waistY + b.T * 0.42f, sw * 0.95f, shY + b.T * 0.05f, 5);
                // v36: the pallu's free end streams from the shoulder in the wind
                clothTail(g, sw * 0.9f, shY + b.T * 0.05f, b.T * 1.3f, b.T * 0.16f, p.wind, p.time, p.seed + 3, lighten(l.primary, 0.08f), l.secondary);
                break;
            }
            case Look.O_SALWAR: {
                // kurta to knees
                float kw = hw * 1.25f;
                g.begin();
                g.moveTo(-sw, shY + b.T * 0.06f); g.lineTo(sw, shY + b.T * 0.06f);
                g.lineTo(kw * (1 + sway), hipY + b.L * 0.55f);
                g.lineTo(-kw * (1 - sway), hipY + b.L * 0.55f); g.close();
                g.color(shade(l.primary, 0.6f)); g.strokePath(3);
                g.linear(0, shY, 0, hipY + b.L * 0.5f, lighten(l.primary, 0.1f), l.primary); g.fillPath();
                g.color(l.secondary);
                g.rect(-kw * (1 - sway), hipY + b.L * 0.47f, kw * 2, b.L * 0.08f);
                g.line(0, shY + b.T * 0.1f, 0, shY + b.T * 0.42f, 3);
                // white dupatta over both shoulders
                g.color(alpha(l.secondary, 0.95f));
                g.begin();
                g.moveTo(-sw * 1.02f, shY + b.T * 0.05f);
                g.quadTo(0, shY + b.T * 0.45f, sw * 1.02f, shY + b.T * 0.05f);
                g.lineTo(sw * 0.8f, shY + b.T * 0.22f);
                g.quadTo(0, shY + b.T * 0.6f, -sw * 0.8f, shY + b.T * 0.22f);
                g.close();
                g.fillPath();
                break;
            }
            case Look.O_ACHKAN: {
                float cw = hw * 1.3f;
                g.begin();
                g.moveTo(-sw, shY + b.T * 0.04f); g.lineTo(sw, shY + b.T * 0.04f);
                g.lineTo(cw * (1 + sway), hipY + b.L * 0.6f); g.lineTo(-cw * (1 - sway), hipY + b.L * 0.6f); g.close();
                g.color(shade(l.primary, 0.55f)); g.strokePath(3);
                g.linear(0, shY, 0, hipY + b.L * 0.6f, lighten(l.primary, 0.1f), shade(l.primary, 0.85f)); g.fillPath();
                // zari trims
                g.color(l.secondary);
                g.line(0, shY + b.T * 0.06f, 0, hipY + b.L * 0.6f, 5);
                g.rect(-cw, hipY + b.L * 0.52f, cw * 2, b.L * 0.08f);
                g.color(0xFFFFE08A);
                for (int i = 0; i < 6; i++) g.oval(b.headR * 0.12f, shY + b.T * (0.15f + i * 0.13f), 3, 3);
                // cummerbund
                g.color(l.secondary);
                g.rect(-hw * 1.05f, waistY + b.T * 0.18f, hw * 2.1f, b.T * 0.1f);
                break;
            }
            case Look.O_UNIFORM: {
                float tw = hw * 1.15f;
                g.begin();
                g.moveTo(-sw, shY + b.T * 0.04f); g.lineTo(sw, shY + b.T * 0.04f);
                g.lineTo(tw, hipY + b.L * 0.3f); g.lineTo(-tw, hipY + b.L * 0.3f); g.close();
                g.color(shade(l.primary, 0.55f)); g.strokePath(3);
                g.linear(0, shY, 0, hipY, lighten(l.primary, 0.08f), l.primary); g.fillPath();
                // white cross belts
                g.color(l.secondary);
                g.line(-sw * 0.8f, shY + b.T * 0.1f, hw * 0.8f, waistY + b.T * 0.15f, 5);
                // waist sash and brass crest
                g.color(l.secondary);
                g.rect(-hw * 1.05f, waistY + b.T * 0.15f, hw * 2.1f, b.T * 0.11f);
                g.color(0xFFD9A93A);
                g.oval(0, waistY + b.T * 0.2f, b.headR * 0.2f, b.headR * 0.2f);
                g.color(0xFFF7DE8A);
                g.oval(-b.headR * 0.05f, waistY + b.T * 0.18f, b.headR * 0.07f, b.headR * 0.07f);
                break;
            }
            case Look.O_CLOAK: {
                float cw = sw * 1.5f;
                // v35: seated, the cloak spans the shoulders to the floor (squeezed into the shorter drop)
                boolean seated = b.seatH >= 0 && shY < b.floorY;
                if (seated) { g.save(); g.translate(0, shY); g.scale(1, (b.floorY - shY) / -shY); g.translate(0, -shY); }
                g.begin();
                g.moveTo(-sw * 0.9f, shY - b.headR * 0.1f);
                g.lineTo(sw * 0.9f, shY - b.headR * 0.1f);
                g.quadTo(cw * 1.1f, -b.L, cw * (1 + sway), -4);
                // ragged hem
                int n = 9;
                for (int i = n; i >= 0; i--) {
                    float x = -cw + (2 * cw) * i / n;
                    float y = (i % 2 == 0) ? -2 : -b.L * 0.12f;
                    g.lineTo(x * (1 + (i % 3) * 0.03f), y);
                }
                g.quadTo(-cw * 1.1f, -b.L, -sw * 0.9f, shY - b.headR * 0.1f);
                g.close();
                g.color(0xFF141216); g.strokePath(3);
                g.linear(0, shY, 0, 0, l.primary, shade(l.primary, 0.75f)); g.fillPath();
                if (seated) g.restore();
                // second layer (dark green rag)
                g.begin();
                g.moveTo(-sw * 0.95f, shY);
                g.lineTo(sw * 0.95f, shY);
                g.lineTo(sw * 1.1f, waistY + b.T * 0.3f);
                for (int i = 6; i >= 0; i--) {
                    float x = -sw * 1.1f + sw * 2.2f * i / 6f;
                    g.lineTo(x, waistY + b.T * ((i % 2 == 0) ? 0.45f : 0.3f));
                }
                g.close();
                g.color(l.secondary); g.fillPath();
                g.color(alpha(0xFF000000, 0.35f));
                for (int i = -2; i <= 2; i++) g.line(i * cw * 0.28f, waistY + b.T * 0.5f, i * cw * 0.42f, -b.L * 0.08f, 3);
                g.color(alpha(l.accent == 0xFFE0A526 ? 0xFF6E6A5E : l.accent, 0.8f));
                g.line(-sw * 0.3f, shY + b.T * 0.3f, -sw * 0.5f, waistY + b.T * 0.4f, 3);
                g.line(sw * 0.4f, shY + b.T * 0.35f, sw * 0.2f, -b.L * 0.4f, 3);
                // patches
                g.color(0xFF4E4A3A);
                g.rect(-cw * 0.55f, -b.L * 0.5f, cw * 0.22f, cw * 0.18f);
                g.color(0xFF2F5A3A);
                g.rect(cw * 0.3f, -b.L * 0.75f, cw * 0.18f, cw * 0.16f);
                if (p.disguised) {
                    // a soft orange shawl over the cloak as the "sweet seller" disguise
                    g.color(0xFFC9772E);
                    g.begin();
                    g.moveTo(-sw * 1.2f, shY - b.headR * 0.2f);
                    g.lineTo(sw * 1.2f, shY - b.headR * 0.2f);
                    g.lineTo(sw * 1.35f, waistY + b.T * 0.6f);
                    g.lineTo(-sw * 1.35f, waistY + b.T * 0.6f);
                    g.close(); g.fillPath();
                    g.color(0xFFF2C230);
                    g.line(-sw * 1.35f, waistY + b.T * 0.6f, sw * 1.35f, waistY + b.T * 0.6f, 4);
                }
                break;
            }
            case Look.O_ARMOR: {
                // furry torso
                g.begin();
                g.moveTo(-sw, shY); g.quadTo(0, shY - b.headR * 0.4f, sw, shY);
                g.quadTo(sw * 1.2f, waistY, hw * 1.05f, hipY + b.L * 0.15f);
                g.lineTo(-hw * 1.05f, hipY + b.L * 0.15f);
                g.quadTo(-sw * 1.2f, waistY, -sw, shY);
                g.close();
                g.color(shade(l.furColor, 0.6f)); g.strokePath(4);
                g.radial(-sw * 0.2f, shY + b.T * 0.3f, sw * 1.6f, lighten(l.furColor, 0.1f), shade(l.furColor, 0.8f)); g.fillPath();
                furStrokes(g, l.furColor, -sw, shY, sw * 2, hipY + b.L * 0.1f - shY, p.seed);
                // lighter belly fur
                g.color(alpha(lighten(l.furColor, 0.22f), 0.8f));
                g.oval(0, waistY + b.T * 0.15f, hw * 0.75f, b.T * 0.3f);
                // leather chest plate + shoulder pads
                g.color(shade(l.primary, 0.9f));
                g.roundRect(-sw * 0.7f, shY + b.T * 0.15f, sw * 1.4f, b.T * 0.5f, 10);
                g.color(shade(l.primary, 0.7f));
                g.oval(-sw * 0.95f, shY + b.T * 0.06f, sw * 0.38f, b.T * 0.16f);
                g.oval(sw * 0.95f, shY + b.T * 0.06f, sw * 0.38f, b.T * 0.16f);
                // spikes
                g.color(0xFFBDBDBD);
                for (int s = -1; s <= 1; s += 2) {
                    for (int i = 0; i < 3; i++) {
                        float x = s * sw * (0.75f + i * 0.13f), y = shY - b.T * 0.02f;
                        g.begin(); g.moveTo(x - 6, y); g.lineTo(x, y - b.T * 0.16f); g.lineTo(x + 6, y); g.close(); g.fillPath();
                    }
                }
                // chains & bones
                if (l.chains) {
                    g.color(0xFF9E9E9E);
                    for (int i = 0; i < 9; i++) {
                        float t = i / 8f;
                        g.strokeOval(-sw * 0.8f + t * sw * 1.6f, shY + b.T * (0.12f + t * 0.5f), 6, 4, 2.5f);
                    }
                }
                g.color(0xFFEDE3C8);
                g.roundRect(-sw * 0.25f, shY + b.T * 0.32f, sw * 0.5f, 7, 4);
                g.oval(-sw * 0.25f, shY + b.T * 0.32f + 3.5f, 6, 6);
                g.oval(sw * 0.25f, shY + b.T * 0.32f + 3.5f, 6, 6);
                // belt with axe
                g.color(0xFF3E2716);
                g.rect(-hw * 1.1f, waistY + b.T * 0.2f, hw * 2.2f, b.T * 0.12f);
                g.color(0xFF8D8D8D);
                g.roundRect(-b.headR * 0.25f, waistY + b.T * 0.19f, b.headR * 0.5f, b.T * 0.14f, 4);
                // loin cloth
                g.color(shade(l.primary, 0.75f));
                g.begin(); g.moveTo(-hw, hipY - b.L * 0.05f); g.lineTo(hw, hipY - b.L * 0.05f);
                g.lineTo(hw * 0.8f, hipY + b.L * 0.4f); g.lineTo(0, hipY + b.L * 0.3f); g.lineTo(-hw * 0.8f, hipY + b.L * 0.4f); g.close();
                g.fillPath();
                break;
            }
            case Look.O_HOODIE: {
                // an oversized hoodie: a wide soft body to below the hips, a kangaroo pocket, drawstrings, the hood folded at the neck
                float kw = hw * 1.3f;
                g.begin();
                g.moveTo(-sw * 1.05f, shY + b.T * 0.02f); g.lineTo(sw * 1.05f, shY + b.T * 0.02f);
                g.quadTo(kw * 1.08f, waistY, kw * (1 + sway * 0.5f), hipY + b.L * 0.22f);
                g.lineTo(-kw * (1 - sway * 0.5f), hipY + b.L * 0.22f);
                g.quadTo(-kw * 1.08f, waistY, -sw * 1.05f, shY + b.T * 0.02f); g.close();
                g.color(shade(l.primary, 0.6f)); g.strokePath(3);
                g.linear(0, shY, 0, hipY + b.L * 0.2f, lighten(l.primary, 0.08f), shade(l.primary, 0.9f)); g.fillPath();
                // ribbed hem and the pocket
                g.color(shade(l.primary, 0.78f));
                g.rect(-kw * 0.98f, hipY + b.L * 0.14f, kw * 1.96f, b.L * 0.08f);
                g.roundRect(-hw * 0.7f, waistY + b.T * 0.12f, hw * 1.4f, b.T * 0.26f, 6);
                g.color(shade(l.primary, 0.62f));
                g.line(-hw * 0.7f, waistY + b.T * 0.12f, -hw * 0.45f, waistY + b.T * 0.38f, 2.5f);
                g.line(hw * 0.7f, waistY + b.T * 0.12f, hw * 0.45f, waistY + b.T * 0.38f, 2.5f);
                // the folded hood behind the neck and the drawstrings
                if (l.headwear != Look.HW_HOOD) {
                    g.color(shade(l.primary, 0.85f));
                    g.oval(0, shY - b.headR * 0.05f, sw * 0.9f, b.T * 0.14f);
                }
                g.color(l.secondary == 0 ? 0xFFEEEEEE : l.secondary);
                g.line(-sw * 0.15f, shY + b.T * 0.08f, -sw * 0.2f, shY + b.T * 0.42f, 2.2f);
                g.line(sw * 0.15f, shY + b.T * 0.08f, sw * 0.22f, shY + b.T * 0.44f, 2.2f);
                // a word of code across the chest
                g.color(alpha(l.secondary == 0 ? 0xFFFFFFFF : l.secondary, 0.85f));
                for (int i = 0; i < 4; i++) g.rect(-sw * 0.5f + i * sw * 0.27f, shY + b.T * 0.5f, sw * 0.18f, b.T * 0.045f);
                break;
            }
            case Look.O_TSHIRT: {
                // a bright t-shirt to the hips with a print on the chest
                float tw = hw * 1.12f;
                g.begin();
                g.moveTo(-sw, shY + b.T * 0.04f); g.lineTo(sw, shY + b.T * 0.04f);
                g.lineTo(tw * (1 + sway * 0.4f), hipY + b.L * 0.06f); g.lineTo(-tw * (1 - sway * 0.4f), hipY + b.L * 0.06f); g.close();
                g.color(shade(l.primary, 0.6f)); g.strokePath(3);
                g.linear(0, shY, 0, hipY, lighten(l.primary, 0.1f), l.primary); g.fillPath();
                // round neck
                g.color(shade(l.primary, 0.8f));
                arc(g, 0, shY + b.T * 0.02f, sw * 0.35f, b.T * 0.09f, 3);
                // the print (a little dinosaur-ish blob in the second colour)
                int print = l.secondary == 0 || l.secondary == l.primary ? lighten(l.primary, 0.35f) : l.secondary;
                g.color(print);
                g.oval(-sw * 0.05f, shY + b.T * 0.4f, sw * 0.34f, b.T * 0.16f);
                g.oval(sw * 0.3f, shY + b.T * 0.3f, sw * 0.16f, b.T * 0.11f);
                g.begin(); g.moveTo(-sw * 0.35f, shY + b.T * 0.42f); g.lineTo(-sw * 0.6f, shY + b.T * 0.36f); g.lineTo(-sw * 0.32f, shY + b.T * 0.5f); g.close(); g.fillPath();
                for (int i = 0; i < 3; i++) { g.begin(); g.moveTo(-sw * 0.15f + i * sw * 0.14f, shY + b.T * 0.33f); g.lineTo(-sw * 0.08f + i * sw * 0.14f, shY + b.T * 0.24f); g.lineTo(sw * 0.0f + i * sw * 0.14f, shY + b.T * 0.33f); g.close(); g.fillPath(); }
                break;
            }
            case Look.O_SUIT: {
                // a formal suit: jacket with lapels over a white shirt and a tie, buttons, a breast pocket
                float cw = hw * 1.2f;
                g.begin();
                g.moveTo(-sw * 1.05f, shY + b.T * 0.02f); g.lineTo(sw * 1.05f, shY + b.T * 0.02f);
                g.lineTo(cw, hipY + b.L * 0.12f); g.lineTo(-cw, hipY + b.L * 0.12f); g.close();
                g.color(shade(l.primary, 0.5f)); g.strokePath(3);
                g.linear(-cw, 0, cw, 0, lighten(l.primary, 0.06f), shade(l.primary, 0.85f)); g.fillPath();
                // shirt and tie
                g.color(0xFFF7F7F2);
                g.begin(); g.moveTo(-sw * 0.3f, shY + b.T * 0.02f); g.lineTo(sw * 0.3f, shY + b.T * 0.02f); g.lineTo(0, waistY + b.T * 0.15f); g.close(); g.fillPath();
                int tie = l.secondary == 0 || l.secondary == l.primary ? 0xFFB71C1C : l.secondary;
                g.color(tie);
                g.begin(); g.moveTo(-sw * 0.08f, shY + b.T * 0.06f); g.lineTo(sw * 0.08f, shY + b.T * 0.06f); g.lineTo(sw * 0.1f, waistY + b.T * 0.1f); g.lineTo(0, waistY + b.T * 0.18f); g.lineTo(-sw * 0.1f, waistY + b.T * 0.1f); g.close(); g.fillPath();
                // lapels
                g.color(shade(l.primary, 0.7f));
                g.begin(); g.moveTo(-sw * 0.62f, shY + b.T * 0.02f); g.lineTo(-sw * 0.3f, shY + b.T * 0.02f); g.lineTo(-sw * 0.18f, waistY); g.lineTo(-sw * 0.45f, waistY - b.T * 0.1f); g.close(); g.fillPath();
                g.begin(); g.moveTo(sw * 0.62f, shY + b.T * 0.02f); g.lineTo(sw * 0.3f, shY + b.T * 0.02f); g.lineTo(sw * 0.18f, waistY); g.lineTo(sw * 0.45f, waistY - b.T * 0.1f); g.close(); g.fillPath();
                // buttons and an LED lining line (a modern suit)
                g.color(0xFFE0E0E0);
                g.oval(sw * 0.08f, waistY + b.T * 0.1f, 3, 3); g.oval(sw * 0.08f, waistY + b.T * 0.24f, 3, 3);
                if (l.accent != 0 && l.accent != 0xFFE0A526) { g.color(alpha(l.accent, 0.8f)); g.line(-cw * 0.9f, hipY + b.L * 0.1f, cw * 0.9f, hipY + b.L * 0.1f, 2); }
                break;
            }
            case Look.O_COAT: {
                // a long coat to below the knees, wide lapels, a belt; wires may hang from it
                float cw = sw * 1.35f;
                g.begin();
                g.moveTo(-sw * 1.05f, shY); g.lineTo(sw * 1.05f, shY);
                g.quadTo(cw * 1.1f, waistY + b.T * 0.3f, cw * (1 + sway * 0.6f), -b.L * 0.2f);
                g.lineTo(-cw * (1 - sway * 0.6f), -b.L * 0.2f);
                g.quadTo(-cw * 1.1f, waistY + b.T * 0.3f, -sw * 1.05f, shY); g.close();
                g.color(0xFF141216); g.strokePath(3);
                g.linear(0, shY, 0, 0, l.primary, shade(l.primary, 0.75f)); g.fillPath();
                // layers: the opening down the middle and a belt
                g.color(shade(l.primary, 0.65f));
                g.line(0, waistY, 0, -b.L * 0.2f, 3);
                g.rect(-hw * 1.12f, waistY + b.T * 0.16f, hw * 2.24f, b.T * 0.1f);
                g.color(shade(l.primary, 0.8f));
                g.begin(); g.moveTo(-sw * 0.6f, shY); g.lineTo(-sw * 0.25f, shY); g.lineTo(-sw * 0.15f, waistY + b.T * 0.05f); g.lineTo(-sw * 0.5f, waistY - b.T * 0.15f); g.close(); g.fillPath();
                g.begin(); g.moveTo(sw * 0.6f, shY); g.lineTo(sw * 0.25f, shY); g.lineTo(sw * 0.15f, waistY + b.T * 0.05f); g.lineTo(sw * 0.5f, waistY - b.T * 0.15f); g.close(); g.fillPath();
                // wires and lights hanging from the coat (a witch of the machines)
                if (l.kind == Look.WITCH || l.earphones) {
                    int[] wire = {0xFFE53935, 0xFF1E88E5, 0xFF43A047, 0xFFFDD835};
                    for (int i = 0; i < 4; i++) {
                        float x0 = -cw * 0.6f + i * cw * 0.4f, sw2 = (float) Math.sin(p.time * 2.5f + i) * 4;
                        g.color(wire[i]);
                        g.line(x0, waistY + b.T * 0.3f, x0 + sw2, -b.L * 0.35f + i * 6, 2);
                    }
                }
                break;
            }
            case Look.O_JEANS: {
                // a short kurta or top with jeans
                float kw = hw * 1.15f;
                g.begin();
                g.moveTo(-sw, shY + b.T * 0.04f); g.lineTo(sw, shY + b.T * 0.04f);
                g.lineTo(kw * (1 + sway * 0.4f), hipY + b.L * 0.2f); g.lineTo(-kw * (1 - sway * 0.4f), hipY + b.L * 0.2f); g.close();
                g.color(shade(l.primary, 0.6f)); g.strokePath(3);
                g.linear(0, shY, 0, hipY + b.L * 0.2f, lighten(l.primary, 0.1f), l.primary); g.fillPath();
                g.color(l.secondary == 0 ? 0xFFF4F1EA : l.secondary);
                g.line(0, shY + b.T * 0.08f, 0, shY + b.T * 0.45f, 3);
                g.rect(-kw * 0.95f, hipY + b.L * 0.14f, kw * 1.9f, b.L * 0.05f);
                break;
            }
            default: { // kurta (men)
                float kw = hw * 1.18f;
                g.begin();
                g.moveTo(-sw, shY + b.T * 0.04f); g.lineTo(sw, shY + b.T * 0.04f);
                g.lineTo(kw, hipY + b.L * 0.45f); g.lineTo(-kw, hipY + b.L * 0.45f); g.close();
                g.color(shade(l.primary, 0.6f)); g.strokePath(3);
                g.color(l.primary); g.fillPath();
                g.color(l.secondary);
                g.line(0, shY + b.T * 0.06f, 0, shY + b.T * 0.4f, 3);
            }
        }
        // earphones hanging around the neck
        if (l.earphones && l.isHumanoid()) {
            g.color(0xFFF5F5F5);
            arc(g, 0, shY - b.T * 0.02f, sw * 0.4f, b.T * 0.2f, 2.5f);
            g.line(-sw * 0.25f, shY + b.T * 0.14f, -sw * 0.3f, shY + b.T * 0.4f, 2);
            g.oval(-sw * 0.3f, shY + b.T * 0.42f, 3.5f, 4);
            g.oval(sw * 0.33f, shY + b.T * 0.1f, 3.5f, 4);
        }
        // necklaces
        float ny = shY + b.T * 0.1f;
        switch (l.necklace) {
            case 1:
                g.color(0xFFE5B530);
                arc(g, 0, ny - b.T * 0.06f, sw * 0.42f, b.T * 0.16f, 2.5f);
                g.oval(0, ny + b.T * 0.1f, b.headR * 0.09f, b.headR * 0.1f);
                break;
            case 2:
                g.color(0xFFF7F2E6);
                for (int i = 0; i <= 10; i++) {
                    double a = Math.PI * i / 10.0;
                    g.oval((float) Math.cos(a) * sw * 0.4f, ny - b.T * 0.06f + (float) Math.sin(a) * b.T * 0.16f, 2.6f, 2.6f);
                }
                break;
            case 3:
                g.color(0xFFE9DFC4);
                for (int i = 0; i <= 8; i++) {
                    double a = Math.PI * i / 8.0;
                    float x = (float) Math.cos(a) * sw * 0.48f, y = ny - b.T * 0.04f + (float) Math.sin(a) * b.T * 0.2f;
                    g.roundRect(x - 3, y - 6, 6, 12, 3);
                }
                break;
            case 4:
                g.color(0xFFF7F2E6);
                arc(g, 0, ny - b.T * 0.06f, sw * 0.45f, b.T * 0.2f, 3f);
                arc(g, 0, ny - b.T * 0.06f, sw * 0.52f, b.T * 0.3f, 3f);
                g.color(0xFF1E9E5A);
                g.oval(0, ny + b.T * 0.24f, b.headR * 0.13f, b.headR * 0.15f);
                g.color(0xFFE5B530);
                g.strokeOval(0, ny + b.T * 0.24f, b.headR * 0.14f, b.headR * 0.16f, 2);
                break;
            default:
        }
    }

    static void arc(Gfx g, float cx, float cy, float rx, float ry, float w) {
        g.begin();
        for (int i = 0; i <= 12; i++) {
            double a = Math.PI * i / 12.0;
            float x = cx + (float) Math.cos(a) * rx, y = cy + (float) Math.sin(a) * ry;
            if (i == 0) g.moveTo(x, y); else g.lineTo(x, y);
        }
        g.strokePath(w);
    }

    static void drawTop(Gfx g, Look l, Pose p, Body b, float bottomY, int dupattaColor, boolean dupatta) {
        float sw = b.sw, shY = b.shY;
        // blouse covering whole torso down to the skirt (no bare midriff)
        g.begin();
        g.moveTo(-sw, shY + b.T * 0.05f);
        g.quadTo(0, shY - b.T * 0.02f, sw, shY + b.T * 0.05f);
        g.lineTo(b.hw * 0.98f, bottomY + 2);
        g.lineTo(-b.hw * 0.98f, bottomY + 2);
        g.close();
        g.color(shade(l.primary, 0.6f)); g.strokePath(3);
        g.linear(0, shY, 0, bottomY, lighten(l.primary, 0.15f), l.primary); g.fillPath();
        // embroidered neckline
        g.color(l.secondary);
        arc(g, 0, shY + b.T * 0.02f, sw * 0.38f, b.T * 0.16f, 3);
        if (dupatta) {
            // odhni across the chest from right shoulder to left hip
            g.begin();
            g.moveTo(sw * 0.95f, shY + b.T * 0.02f);
            g.lineTo(sw * 0.55f, shY - b.T * 0.02f);
            g.lineTo(-b.hw * 1.0f, bottomY - b.T * 0.06f);
            g.lineTo(-b.hw * 0.75f, bottomY + b.T * 0.12f);
            g.close();
            g.color(alpha(dupattaColor, 0.92f)); g.fillPath();
            g.color(0xFFE8C04A);
            g.line(sw * 0.95f, shY + b.T * 0.02f, -b.hw * 0.75f, bottomY + b.T * 0.12f, 2.5f);
            // v36: its free end over the shoulder streams in the wind
            clothTail(g, sw * 0.92f, shY + b.T * 0.04f, b.T * 1.15f, b.T * 0.13f, p.wind, p.time, p.seed, alpha(dupattaColor, 0.92f), 0xFFE8C04A);
        }
    }

    /**
     * v36: the loose end of a saree's pallu or a dupatta streaming in the wind. From the shoulder (x0, y0) it lifts
     * towards where the wind blows, higher the stronger it is, and ripples along its length in waves that run out to
     * the free end, its edge trembling; in still air it is not drawn (it hangs behind the back).
     */
    static void clothTail(Gfx g, float x0, float y0, float len, float wd, float wind, float t, long seedL, int color, int edge) {
        int seed = (int) (seedL % 1000);
        float w = Math.abs(wind);
        if (w < 0.08f) return;
        float a = Math.min(1, (w - 0.08f) / 0.25f), dir = wind >= 0 ? 1 : -1;
        float lift = Math.min(1.25f, 0.35f + w * 0.9f);          // radians from hanging straight down
        final int N = 8;
        float[] xs = new float[N + 1], ys = new float[N + 1];
        float x = x0, y = y0, step = len / N;
        for (int i = 0; i <= N; i++) {
            float f = i / (float) N;
            float ang = lift * (0.6f + 0.4f * f) + 0.35f * f * (float) Math.sin(6.283f * (f * 1.4f - t * (1.6f + w)) + seed);
            xs[i] = x; ys[i] = y;
            x += dir * (float) Math.sin(ang) * step;
            y += (float) Math.cos(ang) * step;
        }
        float[] nx = new float[N + 1], ny = new float[N + 1];
        for (int i = 0; i <= N; i++) {
            int i0 = Math.max(0, i - 1), i1 = Math.min(N, i + 1);
            float dx = xs[i1] - xs[i0], dy = ys[i1] - ys[i0], d = (float) Math.max(1e-3, Math.hypot(dx, dy));
            nx[i] = -dy / d; ny[i] = dx / d;
        }
        g.save();
        g.setAlpha(a);
        g.begin();
        for (int i = 0; i <= N; i++) {
            float hw = wd * (1 - 0.35f * i / (float) N) * (1 + 0.18f * (float) Math.sin(t * 9 + i + seed));
            if (i == 0) g.moveTo(xs[i] + nx[i] * hw, ys[i] + ny[i] * hw); else g.lineTo(xs[i] + nx[i] * hw, ys[i] + ny[i] * hw);
        }
        for (int i = N; i >= 0; i--) {
            float hw = wd * (1 - 0.35f * i / (float) N);
            g.lineTo(xs[i] - nx[i] * hw, ys[i] - ny[i] * hw);
        }
        g.close();
        g.color(color);
        g.fillPath();
        g.color(edge);
        for (int i = 1; i <= N; i++) {
            float h0 = wd * (1 - 0.35f * (i - 1) / (float) N), h1 = wd * (1 - 0.35f * i / (float) N);
            g.line(xs[i - 1] - nx[i - 1] * h0, ys[i - 1] - ny[i - 1] * h0, xs[i] - nx[i] * h1, ys[i] - ny[i] * h1, 2.5f);
        }
        g.restore();
    }

    static void furStrokes(Gfx g, int fur, float x, float y, float w, float h, long seed) {
        g.color(alpha(lighten(fur, 0.18f), 0.55f));
        long s = seed * 31 + 7;
        for (int i = 0; i < 26; i++) {
            s = s * 1103515245L + 12345L;
            float fx = x + ((s >>> 8) % 1000) / 1000f * w;
            s = s * 1103515245L + 12345L;
            float fy = y + ((s >>> 8) % 1000) / 1000f * h;
            g.line(fx, fy, fx + 3, fy + 9, 2f);
        }
    }

    static void drawBodyProps(Gfx g, Look l, Pose p, Body b) {
        float waistY = b.shY + b.T * 0.62f;
        if (l.sword || l.katar) {
            if (l.outfit == Look.O_LEHENGA || l.outfit == Look.O_SALWAR) {
                // wide leather belt
                g.color(0xFF6B4226);
                g.rect(-b.hw * 1.0f, waistY - b.T * 0.02f, b.hw * 2.0f, b.T * 0.09f);
                g.color(0xFFE5B530);
                g.roundRect(-b.headR * 0.13f, waistY - b.T * 0.03f, b.headR * 0.26f, b.T * 0.11f, 3);
            }
            if (l.sword && p.holdR != Pose.I_SWORD && p.holdR != Pose.I_WOOD_SWORD) {
                // scabbard hanging diagonally at the hip
                float sx = -p.facing * b.hw * 0.9f, sy = waistY + b.T * 0.05f;
                g.color(0xFF4A2C17);
                g.line(sx, sy, sx - p.facing * b.L * 0.25f, sy + b.L * 0.75f, b.headR * 0.13f);
                g.color(0xFFE5B530);
                g.line(sx + p.facing * 5, sy - 6, sx - p.facing * 5, sy + 4, 4);
                g.oval(sx + p.facing * 2, sy - 10, 4, 4);
            }
            if (l.katar) {
                float kx = p.facing * b.hw * 0.55f, ky = waistY + b.T * 0.02f;
                g.color(0xFF9E9E9E);
                g.line(kx, ky, kx + p.facing * 4, ky + b.T * 0.22f, 4);
                g.color(0xFFE5B530);
                g.rect(kx - 6, ky - 4, 12, 5);
            }
        }
        if (l.satchel) {
            g.color(0xFFC9A66B);
            g.line(b.sw * 0.8f, b.shY + b.T * 0.05f, -b.hw * 0.9f, waistY + b.T * 0.3f, 4.5f);
            float bx = -b.hw * 0.95f, by = waistY + b.T * 0.32f;
            g.color(0xFFB5895A);
            g.roundRect(bx - b.headR * 0.38f, by - b.headR * 0.2f, b.headR * 0.76f, b.headR * 0.62f, 8);
            g.color(0xFF8A6A3A);
            g.roundRect(bx - b.headR * 0.38f, by - b.headR * 0.2f, b.headR * 0.76f, b.headR * 0.22f, 8);
            g.color(0xFF2F5DB5);
            g.oval(bx, by + b.headR * 0.18f, 4, 4);
        }
        if (l.kind == Look.MONSTER && l.axe) {
            float ax = -p.facing * b.hw * 1.05f, ay = waistY + b.T * 0.2f;
            g.color(0xFF5D4037);
            g.line(ax, ay - b.T * 0.2f, ax, ay + b.L * 0.6f, 7);
            g.color(0xFF8D6E63);
            g.begin(); g.moveTo(ax, ay); g.quadTo(ax - p.facing * b.headR * 0.9f, ay + b.headR * 0.4f, ax, ay + b.headR * 0.9f);
            g.close(); g.fillPath();
            g.color(0xFFA1887F);
            g.line(ax - p.facing * b.headR * 0.55f, ay + b.headR * 0.15f, ax - p.facing * b.headR * 0.55f, ay + b.headR * 0.75f, 3);
        }
    }

    // ------------------------------------------------------------------ arms

    static void drawArm(Gfx g, Look l, Pose p, Body b, int side, float armDeg, float elbow, int hold) {
        if (p.carrying) { armDeg = 165; elbow = 25; }
        float[] h = hand(b, side, armDeg, elbow);
        int sleeve;
        boolean fullSleeve;
        switch (l.outfit) {
            case Look.O_LEHENGA: case Look.O_SAREE: sleeve = l.primary; fullSleeve = false; break;
            case Look.O_SALWAR: sleeve = l.primary; fullSleeve = true; break;
            case Look.O_ACHKAN: case Look.O_UNIFORM: case Look.O_KURTA: sleeve = l.primary; fullSleeve = true; break;
            case Look.O_CLOAK: sleeve = l.primary; fullSleeve = true; break;
            case Look.O_ARMOR: sleeve = l.furColor; fullSleeve = true; break;
            case Look.O_TSHIRT: sleeve = l.primary; fullSleeve = false; break;
            case Look.O_SUIT: sleeve = shade(l.primary, 0.95f); fullSleeve = true; break;
            case Look.O_COAT: sleeve = l.primary; fullSleeve = true; break;
            default: sleeve = l.primary; fullSleeve = true;
        }
        if (p.disguised && l.kind == Look.WITCH) sleeve = 0xFFC9772E;
        int skin = l.kind == Look.MONSTER ? l.furColor : faceSkin(l, p);
        // upper arm
        g.color(shade(sleeve, 0.7f));
        g.line(h[0], h[1], h[2], h[3], b.armW * 2.15f);
        g.color(sleeve);
        g.line(h[0], h[1], h[2], h[3], b.armW * 1.85f);
        // forearm
        g.color(fullSleeve ? sleeve : skin);
        g.line(h[2], h[3], h[4], h[5], b.armW * 1.6f);
        if (!fullSleeve && l.secondary != 0) { // sleeve trim
            g.color(l.secondary);
            g.oval(h[2], h[3], b.armW * 0.95f, b.armW * 0.95f);
        }
        if (l.bangles && !fullSleeve) {
            g.color(l.kind == Look.GIRL && l.necklace == 2 ? 0xFFE53935 : 0xFFE5B530);
            float bx = h[2] + (h[4] - h[2]) * 0.78f, by = h[3] + (h[5] - h[3]) * 0.78f;
            g.strokeOval(bx, by, b.armW * 0.85f, b.armW * 0.5f, 2.5f);
            if (l.kind == Look.GIRL) { g.color(0xFF43A047); g.strokeOval(bx, by + 4, b.armW * 0.85f, b.armW * 0.5f, 2.5f); }
        }
        if (l.kind == Look.MONSTER) {
            g.color(0xFF3E2716);
            g.line(h[2], h[3], h[2] + (h[4] - h[2]) * 0.3f, h[3] + (h[5] - h[3]) * 0.3f, b.armW * 1.9f);
        }
        // hand (v35: fingers missing — the left hand narrower, the stumps' line across it)
        float hr = b.armW * (p.fist ? 0.95f : 1.05f);
        g.color(skin);
        boolean fewer = side == -1 && (l.condition & Look.C_FINGERS) != 0;
        g.oval(h[4], h[5], fewer ? hr * 0.72f : hr, hr);
        if (fewer) { g.color(shade(skin, 0.8f)); g.line(h[4] - hr * 0.5f, h[5] + hr * 0.45f, h[4] + hr * 0.5f, h[5] + hr * 0.45f, Math.max(1.2f, hr * 0.12f)); }
        if (l.longNails) {
            g.color(0xFF3B3B2E);
            for (int i = -1; i <= 1; i++) g.line(h[4] + i * 3, h[5] + hr * 0.6f, h[4] + i * 4, h[5] + hr * 1.5f, 2);
        }
        if (l.kind == Look.MONSTER) {
            g.color(0xFFEDE3C8);
            for (int i = -1; i <= 1; i++) g.line(h[4] + i * 5, h[5] + hr * 0.8f, h[4] + i * 6, h[5] + hr * 1.4f, 3);
        }
        drawHeld(g, l, p, b, side, h, hold);
    }

    static int faceSkin(Look l, Pose p) {
        if (l.kind == Look.WITCH && p.disguised) return 0xFFD9A77A;
        return l.skin;
    }

    static void drawHeld(Gfx g, Look l, Pose p, Body b, int side, float[] h, int hold) {
        float hx = h[4], hy = h[5];
        // v34: a walking stick from the right hand to the ground (its crook over the hand); crutches under both arms
        boolean onFeet = p.body != Pose.SIT && p.body != Pose.LIE && p.body != Pose.HANG && p.body != Pose.KNEEL;
        if (onFeet && hold == Pose.I_NONE && side == (l.missingArm() == 1 ? -1 : 1) && l.aid == Look.AID_STICK) {
            float sw = Math.max(3, b.armW * 0.5f), gx = hx + p.facing * b.headR * 0.22f;
            g.color(0xFF5D4037);
            g.line(hx, hy, gx, -1, sw);
            g.begin(); g.moveTo(hx - p.facing * b.armW * 1.3f, hy + b.armW * 0.3f); g.quadTo(hx - p.facing * b.armW * 0.6f, hy - b.armW * 1.5f, hx, hy); g.strokePath(sw);
            g.color(0xFF3E2723); g.oval(gx, -2, sw * 0.8f, sw * 0.4f);
        }
        if (onFeet && l.aid == Look.AID_CRUTCHES) {
            float tx = h[0] + side * b.armW * 0.5f, ty = h[1] + b.armW * 1.3f, fx = hx + side * b.armW * 0.7f, cw = Math.max(3, b.armW * 0.45f);
            g.color(0xFF90A4AE);
            g.line(tx, ty, fx, -1, cw);
            g.color(0xFF455A64);
            g.line(tx - b.armW * 0.7f, ty, tx + b.armW * 0.7f, ty, b.armW * 0.55f);           // the pad under the arm
            g.line(hx - b.armW * 0.6f, hy, hx + b.armW * 0.6f, hy, b.armW * 0.42f);           // the grip in the hand
            g.color(0xFF263238); g.oval(fx, -2, cw * 0.9f, cw * 0.45f);
        }
        // default props when not holding anything special
        if (hold == Pose.I_NONE) {
            if (side == 1 && l.spear) {
                g.color(0xFF8D6E63);
                g.line(hx, hy - b.H * 0.75f, hx, hy + b.H * 0.25f, 5);
                g.color(0xFFCFD8DC);
                g.begin(); g.moveTo(hx - 8, hy - b.H * 0.75f); g.lineTo(hx, hy - b.H * 0.9f); g.lineTo(hx + 8, hy - b.H * 0.75f); g.close();
                g.fillPath();
                g.color(0xFFC62828);
                g.rect(hx - 6, hy - b.H * 0.75f, 12, 6);
            }
            if (side == -1 && l.shield) {
                g.color(0xFF8D6E63);
                g.oval(hx, hy - 4, b.headR * 0.62f, b.headR * 0.62f);
                g.color(0xFFE5B530);
                g.strokeOval(hx, hy - 4, b.headR * 0.62f, b.headR * 0.62f, 3);
                g.oval(hx, hy - 4, 5, 5);
            }
            if (side == 1 && l.wand && !p.disguised) { if (l.techWand) drawTechWand(g, hx, hy, b, p.glowWand, p.time); else drawWand(g, hx, hy, b, p.glowWand, p.time); }
            if (side == 1 && l.gadget == Look.GD_PHONE) {
                // a phone in the hand, its screen lit
                g.color(0xFF1E1E22); g.roundRect(hx - b.headR * 0.12f, hy - b.headR * 0.22f, b.headR * 0.24f, b.headR * 0.42f, 3);
                g.color(0xFF80D8FF); g.roundRect(hx - b.headR * 0.09f, hy - b.headR * 0.18f, b.headR * 0.18f, b.headR * 0.34f, 2);
            }
            if (side == 1 && l.gadget == Look.GD_CONTROLLER) {
                // a game controller held in front
                g.color(0xFF263238); g.roundRect(hx - b.headR * 0.32f, hy - b.headR * 0.1f, b.headR * 0.64f, b.headR * 0.26f, 8);
                g.color(0xFFE53935); g.oval(hx + b.headR * 0.18f, hy - b.headR * 0.02f, 3, 3);
                g.color(0xFF43A047); g.oval(hx + b.headR * 0.24f, hy + b.headR * 0.04f, 3, 3);
                g.color(0xFF90A4AE); g.rect(hx - b.headR * 0.25f, hy - b.headR * 0.01f, b.headR * 0.1f, 3); g.rect(hx - b.headR * 0.215f, hy - b.headR * 0.05f, 3, b.headR * 0.1f);
            }
            if (side == -1 && l.gadget == Look.GD_LAPTOP) {
                // a slim laptop under the arm
                g.color(0xFF90A4AE); g.roundRect(hx - b.headR * 0.5f, hy - b.headR * 0.08f, b.headR * 0.9f, b.headR * 0.16f, 3);
                g.color(0xFF546E7A); g.rect(hx - b.headR * 0.5f, hy + b.headR * 0.02f, b.headR * 0.9f, 2);
            }
            if (side == -1 && l.kind == Look.MONSTER && l.mace) {
                g.color(0xFF5D4037);
                g.line(hx, hy, hx, hy + b.L * 0.55f, 7);
                g.color(0xFF616161);
                g.oval(hx, hy + b.L * 0.6f, b.headR * 0.35f, b.headR * 0.35f);
            }
            return;
        }
        switch (hold) {
            case Pose.I_RIBBON:
                g.color(p.holdColor);
                g.begin(); g.moveTo(hx, hy);
                g.quadTo(hx + side * 14, hy + 18 + (float) Math.sin(p.time * 4) * 4, hx + side * 4, hy + 34);
                g.strokePath(5);
                break;
            case Pose.I_BANANA:
                g.color(0xFFFDD835);
                g.begin(); g.moveTo(hx - 6, hy - 18); g.quadTo(hx + 14, hy - 4, hx - 2, hy + 14);
                g.quadTo(hx + 4, hy - 2, hx - 6, hy - 18); g.close(); g.fillPath();
                break;
            case Pose.I_MIRROR:
                g.color(0xFF8D6E63); g.line(hx, hy, hx, hy + 12, 5);
                g.color(0xFFB0BEC5); g.oval(hx, hy - 8, 13, 13);
                g.color(0xFFFFFFFF); g.oval(hx - 3, hy - 11, 5, 5);
                break;
            case Pose.I_WOOD_SWORD: case Pose.I_SWORD: {
                float ang = p.armR > 60 ? -1 : 1;
                int blade = hold == Pose.I_SWORD ? 0xFFE0E0E0 : 0xFFB98A55;
                g.color(0xFF5D4037);
                g.line(hx, hy, hx + side * 6, hy + 10 * ang, 6);
                g.color(blade);
                float dx = side * (p.armR > 60 ? b.H * 0.12f : b.H * 0.05f), dy = -(p.armR > 60 ? b.H * 0.25f : b.H * 0.3f);
                g.line(hx, hy, hx + dx, hy + dy, 6);
                g.color(0xFFE5B530);
                g.line(hx - 8, hy + 2, hx + 8, hy - 2, 4);
                break;
            }
            case Pose.I_BASKET:
                g.color(0xFFA1887F);
                g.roundRect(hx - 24, hy - 6, 48, 22, 8);
                g.color(0xFF6D4C41);
                g.strokeOval(hx, hy - 6, 22, 14, 3);
                int[] cols = {0xFFFFB300, 0xFFE91E63, 0xFF7CB342, 0xFFFF7043};
                for (int i = 0; i < 4; i++) {
                    g.color(cols[i]);
                    g.oval(hx - 15 + i * 10, hy - 8, 7, 7);
                }
                g.color(alpha(0xFFFFFFFF, 0.6f + 0.4f * (float) Math.sin(p.time * 7)));
                g.oval(hx - 9, hy - 11, 2, 2);
                g.oval(hx + 11, hy - 10, 2, 2);
                break;
            case Pose.I_FLOWER:
                g.color(0xFF6D4C41); g.line(hx, hy, hx, hy + 14, 3);
                g.color(0xFF8D6E63); g.oval(hx, hy - 4, 7, 7);
                break;
            case Pose.I_BOTTLE:
                g.color(0xFF6D4C41); g.roundRect(hx - 7, hy - 16, 14, 24, 5);
                g.color(0xFF90CAF9); g.rect(hx - 4, hy - 20, 8, 5);
                break;
            case Pose.I_CUP: case Pose.I_TEA: case Pose.I_GLASS: case Pose.I_PLATE:
                drawVessel(g, hold, hx, hy, b.headR / 34f, p.time);
                break;
            case Pose.I_TURBAN:
                drawTurbanShape(g, hx, hy - 6, b.headR * 1.0f, p.turbanColor, p.turbanBand, false);
                break;
            default:
                if (hold >= Pose.I_LADLE && hold <= Pose.I_PAPER) drawTool(g, hold, hx, hy, b.headR / 34f, p.facing, p.time, b.floorY);
        }
    }

    /**
     * v36: the tool of an everyday task held at the hand (x, y), k = scale (1 ≈ a child's head of 34 px), facing the
     * way the character looks, floorY = where the floor is (a broom's bristles and a watering can's water reach it).
     */
    public static void drawTool(Gfx g, int item, float x, float y, float k, float facing, float t, float floorY) {
        float f = facing < 0 ? -1 : 1;
        switch (item) {
            case Pose.I_LADLE: {
                // a long-handled ladle: the handle up behind the hand, the bowl down in the pot
                g.color(0xFF8D6E63); g.line(x - f * 10 * k, y - 16 * k, x + f * 10 * k, y + 20 * k, 3.2f * k);
                g.color(0xFFB0BEC5); g.oval(x + f * 12 * k, y + 23 * k, 6 * k, 3.5f * k);
                break;
            }
            case Pose.I_BROOM: {
                // a jhadu: a bundle of grass from the hand down to the floor ahead, its bristles fanning on the floor
                float bx = x + f * 26 * k, by = Math.max(y + 20 * k, floorY - 2);
                g.color(0xFF8D6E63); g.line(x, y - 6 * k, x + f * 9 * k, y + 14 * k, 4.5f * k);
                g.color(0xFFC8A464);
                for (int i = -3; i <= 3; i++) g.line(x + f * 8 * k, y + 12 * k, bx + i * 4 * k + f * (float) Math.sin(t * 9) * 3 * k, by, 1.6f * k);
                g.color(0xFF6D4C41); g.line(x + f * 6 * k, y + 10 * k, x + f * 11 * k, y + 16 * k, 3 * k);
                break;
            }
            case Pose.I_BOOK: case Pose.I_NOTEBOOK: {
                // an open book (or a notebook) held up, its two pages; a page lifts now and then
                int cover = item == Pose.I_BOOK ? 0xFF8E2D2D : 0xFF2E5C8A;
                g.color(cover); g.roundRect(x - 15 * k, y - 11 * k, 30 * k, 21 * k, 2 * k);
                g.color(0xFFF7F3E8); g.rect(x - 13.5f * k, y - 10 * k, 13 * k, 18 * k); g.rect(x + 0.5f * k, y - 10 * k, 13 * k, 18 * k);
                g.color(0x55000000); for (int i = 0; i < 5; i++) { g.line(x - 12 * k, y - 7 * k + i * 3 * k, x - 2 * k, y - 7 * k + i * 3 * k, 0.6f * k); g.line(x + 2 * k, y - 7 * k + i * 3 * k, x + 12 * k, y - 7 * k + i * 3 * k, 0.6f * k); }
                float flip = (t % 2.6f) / 2.6f;
                if (item == Pose.I_BOOK && flip < 0.18f) {
                    float u = flip / 0.18f;
                    g.color(0xFFFFFDF6); g.rect(x + 0.5f * k - 13 * k * u, y - 10 * k, 13 * k * (1 - 2 * Math.abs(u - 0.5f)) + 1, 18 * k);
                }
                break;
            }
            case Pose.I_PAPER: {
                // a newspaper opened wide in both hands: grey columns, a headline, a photo block; it sways a little
                k *= 1.35f;
                float sw = (float) Math.sin(t * 1.3f) * 1.2f * k;
                g.color(0xFFE9E6DC); g.rect(x - 30 * k, y - 22 * k + sw, 60 * k, 36 * k);
                g.color(0x22000000); g.line(x, y - 22 * k + sw, x, y + 14 * k + sw, 1 * k);
                g.color(0xFF3A3A3A); g.rect(x - 27 * k, y - 19 * k + sw, 24 * k, 3.2f * k); g.rect(x + 3 * k, y - 19 * k + sw, 22 * k, 3.2f * k);
                g.color(0xFF9E9E9E); g.rect(x + 3 * k, y - 13 * k + sw, 11 * k, 9 * k);
                g.color(0x66000000);
                for (int i = 0; i < 7; i++) {
                    float ly = y - 12 * k + i * 3.4f * k + sw;
                    g.line(x - 27 * k, ly, x - 15 * k, ly, 0.7f * k); g.line(x - 14 * k, ly, x - 3 * k, ly, 0.7f * k);
                    if (ly > y - 2 * k + sw) g.line(x + 3 * k, ly, x + 25 * k, ly, 0.7f * k);
                }
                break;
            }
            case Pose.I_PEN: { g.color(0xFF1565C0); g.line(x, y, x + f * 4 * k, y - 11 * k, 2 * k); g.color(0xFF212121); g.line(x, y, x - f * 0.6f * k, y + 2 * k, 1.2f * k); break; }
            case Pose.I_PHONE: {
                g.color(0xFF1E1E22); g.roundRect(x - 3.5f * k, y - 7 * k, 7 * k, 13 * k, 1.5f * k);
                g.color(0xFF80D8FF); g.roundRect(x - 2.6f * k, y - 6 * k, 5.2f * k, 10.5f * k, 1 * k);
                break;
            }
            case Pose.I_BRUSH: {
                // a toothbrush: the handle and the bristles, a little foam
                g.color(0xFF26A69A); g.line(x - f * 8 * k, y + 2 * k, x + f * 8 * k, y - 1 * k, 2.2f * k);
                g.color(0xFFFFFFFF); g.rect(x + f * 6 * k - 2 * k, y - 4 * k, 4 * k, 3 * k);
                g.color(0xCCFFFFFF); g.oval(x + f * 9 * k, y - 1 * k, 2.2f * k, 1.8f * k);
                break;
            }
            case Pose.I_COMB: {
                g.color(0xFF6D4C41); g.rect(x - 2 * k, y - 9 * k, 3 * k, 18 * k);
                g.color(0xFF8D6E63); for (int i = 0; i < 8; i++) g.line(x + 1 * k, y - 8 * k + i * 2.2f * k, x + 5 * k, y - 8 * k + i * 2.2f * k, 0.9f * k);
                break;
            }
            case Pose.I_CAN: {
                // a watering can, tipped: the body, the handle, the spout and a fan of water falling to the floor ahead
                g.color(0xFF43A047); g.roundRect(x + (f > 0 ? -4 : -14) * k, y - 4 * k, 18 * k, 12 * k, 3 * k);
                g.color(0xFF2E7D32); g.line(x + f * 12 * k, y, x + f * 24 * k, y - 6 * k, 2.4f * k);
                float sx = x + f * 25 * k, sy = y - 6 * k;
                g.color(0x8890CAF9);
                for (int i = 0; i < 6; i++) {
                    float ph = (t * 3 + i * 0.17f) % 1f;
                    float ex = sx + f * (8 + i * 2) * k, ey = Math.max(sy + 4 * k, floorY - 2);
                    g.line(sx + f * i * 0.6f * k, sy, sx + f * (3 + i) * k + (ex - sx) * 0.3f, sy + (ey - sy) * 0.55f, 0.9f * k);
                    g.line(sx + f * (3 + i) * k + (ex - sx) * 0.3f, sy + (ey - sy) * 0.55f, ex, ey, 0.9f * k);
                    g.color(alpha(0xFFBBDEFB, 0.7f * (1 - ph))); g.oval(ex + (ph - 0.5f) * 6 * k, ey - ph * 4 * k, 1.4f * k, 1.4f * k);
                    g.color(0x8890CAF9);
                }
                break;
            }
            default:
        }
    }

    /**
     * v36: what an everyday task needs on the floor or a counter in front of the character (feet at 0,0, h = their
     * height): a counter with a gas stove, its blue-orange flame and a pot that steams (cooking); a bucket of water
     * with suds (washing); a potted plant, wet and shining where the water falls (watering).
     */
    public static void drawTaskProp(Gfx g, int task, float h, float facing, float t) {
        float f = facing < 0 ? -1 : 1;
        switch (task) {
            case Film.T_COOK: {
                float cx = f * h * 0.36f, top = -h * 0.44f, w = h * 0.34f;
                g.color(0xFF8D6E63); g.rect(cx - w / 2, top, w, -top);                         // the counter
                g.color(0xFFA1887F); g.rect(cx - w / 2 - h * 0.01f, top - h * 0.015f, w + h * 0.02f, h * 0.03f);
                g.color(0xFF37474F); g.roundRect(cx - w * 0.36f, top - h * 0.035f, w * 0.72f, h * 0.025f, h * 0.008f);    // the stove
                Nature.flame(g, cx, top - h * 0.035f, t, h / 900f, 0, 0, 7);
                g.color(0xFF455A64);                                                              // the pot (a kadhai)
                g.begin(); g.moveTo(cx - w * 0.3f, top - h * 0.075f); g.quadTo(cx, top - h * 0.01f, cx + w * 0.3f, top - h * 0.075f); g.close(); g.fillPath();
                g.color(0xFF263238); g.line(cx - w * 0.3f, top - h * 0.075f, cx + w * 0.3f, top - h * 0.075f, h * 0.008f);
                for (int i = 0; i < 3; i++) {                                                     // steam
                    float ph = (t * 0.5f + i / 3f) % 1f;
                    g.color(Puppet.alpha(0xFFFFFFFF, 0.3f * (1 - ph)));
                    g.oval(cx + (i - 1) * w * 0.12f + (float) Math.sin(t * 2 + i) * w * 0.05f, top - h * 0.09f - ph * h * 0.14f, w * (0.06f + ph * 0.06f), w * (0.05f + ph * 0.05f));
                }
                break;
            }
            case Film.T_WASH: {
                float cx = f * h * 0.3f, r = h * 0.11f;
                g.color(0xFF1E88E5); g.begin(); g.moveTo(cx - r, -h * 0.17f); g.lineTo(cx + r, -h * 0.17f); g.lineTo(cx + r * 0.8f, 0); g.lineTo(cx - r * 0.8f, 0); g.close(); g.fillPath();
                g.color(0xFF90CAF9); g.oval(cx, -h * 0.17f, r, h * 0.022f);
                for (int i = 0; i < 5; i++) { g.color(0xDDFFFFFF); g.oval(cx + (i - 2) * r * 0.35f, -h * 0.175f - (float) Math.abs(Math.sin(t * 3 + i)) * h * 0.012f, r * 0.13f, r * 0.11f); }
                break;
            }
            case Film.T_WATER: {
                float cx = f * h * 0.62f;
                g.color(0xFFB5652B); g.begin(); g.moveTo(cx - h * 0.07f, -h * 0.13f); g.lineTo(cx + h * 0.07f, -h * 0.13f); g.lineTo(cx + h * 0.05f, 0); g.lineTo(cx - h * 0.05f, 0); g.close(); g.fillPath();
                g.color(0xFF2E7D32);
                for (int i = -3; i <= 3; i++) {
                    float sway = (float) Math.sin(t * 1.7 + i) * h * 0.01f;
                    g.oval(cx + i * h * 0.025f + sway, -h * 0.2f - Math.abs(i) * -h * 0.012f - h * 0.03f * (3 - Math.abs(i)) / 3f, h * 0.03f, h * 0.05f);
                }
                g.color(0x66BBDEFB); g.oval(cx, -h * 0.14f, h * 0.05f, h * 0.012f);
                break;
            }
            default:
        }
    }

    /**
     * v35: what a hand holds to eat or drink, at (x, y) (the hand), k = size: a cup (tea steams in thin rising wisps),
     * a glass of water (clear, the water line catching the light), a steel plate of food (a thali: a roti, rice, dal).
     */
    static void drawVessel(Gfx g, int item, float x, float y, float k, float t) {
        switch (item) {
            case Pose.I_CUP: case Pose.I_TEA: {
                g.color(0xFFF5F2EA); g.roundRect(x - 7 * k, y - 15 * k, 14 * k, 15 * k, 3 * k);
                g.color(0xFFD9D2C4); g.strokeOval(x + 9 * k, y - 8 * k, 3.5f * k, 4.5f * k, 2 * k);
                g.color(item == Pose.I_TEA ? 0xFFB07A4A : 0xFF6D4C41); g.oval(x, y - 14 * k, 6 * k, 1.8f * k);
                if (item == Pose.I_TEA) {
                    // steam: two thin wisps drifting up and fading
                    for (int i = 0; i < 2; i++) {
                        float ph = (t * 0.6f + i * 0.5f) % 1f;
                        g.color(alpha(0xFFFFFFFF, 0.35f * (1 - ph)));
                        float sx = x + (i == 0 ? -2.5f : 2.5f) * k, sy = y - 17 * k - ph * 16 * k;
                        g.begin(); g.moveTo(sx, sy + 6 * k); g.quadTo(sx + 3 * k * (float) Math.sin(t * 3 + i), sy + 3 * k, sx, sy); g.strokePath(1.3f * k);
                    }
                }
                break;
            }
            case Pose.I_GLASS: {
                g.color(0x66D6EEF8);
                g.begin(); g.moveTo(x - 6 * k, y - 18 * k); g.lineTo(x + 6 * k, y - 18 * k); g.lineTo(x + 4.6f * k, y); g.lineTo(x - 4.6f * k, y); g.close(); g.fillPath();
                g.color(0x8890CAF9);
                g.begin(); g.moveTo(x - 5.5f * k, y - 12 * k); g.lineTo(x + 5.5f * k, y - 12 * k); g.lineTo(x + 4.6f * k, y); g.lineTo(x - 4.6f * k, y); g.close(); g.fillPath();
                g.color(0xCCFFFFFF); g.line(x - 4 * k, y - 16 * k, x - 3.4f * k, y - 3 * k, 1.1f * k);
                g.color(0x99B0BEC5); g.strokeOval(x, y - 18 * k, 6 * k, 1.2f * k, 0.9f * k);
                break;
            }
            case Pose.I_PLATE: {
                g.color(0xFFB0BEC5); g.oval(x, y - 2 * k, 17 * k, 4.5f * k);
                g.color(0xFFD5DBDF); g.oval(x, y - 2.6f * k, 14 * k, 3.5f * k);
                g.color(0xFFD7A86E); g.oval(x - 6 * k, y - 3.2f * k, 5.5f * k, 2 * k);         // a roti
                g.color(0xFFFAFAF5); g.oval(x + 2 * k, y - 3.6f * k, 4.5f * k, 1.8f * k);      // rice
                g.color(0xFFE8B230); g.oval(x + 8 * k, y - 3.2f * k, 3 * k, 1.4f * k);         // dal
                break;
            }
            default:
        }
    }

    /** A selfie stick with RGB lights (a witch's wand of today): the glow cycles through the colours. */
    static void drawTechWand(Gfx g, float hx, float hy, Body b, boolean glow, float t) {
        g.color(0xFF37474F);
        g.begin(); g.moveTo(hx, hy + 16); g.quadTo(hx - 5, hy - b.H * 0.14f, hx + 3, hy - b.H * 0.3f); g.strokePath(4);
        float gx = hx + 3, gy = hy - b.H * 0.33f;
        float h = (t * 0.7f) % 1f;
        int c = h < 0.33f ? 0xFFFF1744 : h < 0.66f ? 0xFF00E676 : 0xFF2979FF;
        if (glow) { g.radial(gx, gy, 42, alpha(c, 0.6f), alpha(c, 0f)); g.oval(gx, gy, 42, 42); }
        g.color(0xFF1E1E22); g.roundRect(gx - 7, gy - 11, 14, 22, 3);
        g.color(c); g.roundRect(gx - 5, gy - 9, 10, 18, 2);
        g.color(alpha(0xFFFFFFFF, 0.6f)); g.oval(gx - 2, gy - 5, 2.5f, 2.5f);
    }

    static void drawWand(Gfx g, float hx, float hy, Body b, boolean glow, float t) {
        g.color(0xFF6D4C41);
        g.begin(); g.moveTo(hx, hy + 20);
        g.quadTo(hx - 6, hy - b.H * 0.15f, hx + 4, hy - b.H * 0.3f);
        g.strokePath(5);
        float gx = hx + 4, gy = hy - b.H * 0.32f;
        if (glow) {
            g.radial(gx, gy, 38, 0xAA76FF03, 0x0076FF03);
            g.oval(gx, gy, 38, 38);
        }
        g.color(0xFF64DD17);
        g.oval(gx, gy, 8, 8);
        g.color(alpha(0xFFFFFFFF, 0.7f));
        g.oval(gx - 2, gy - 3, 2.5f, 2.5f);
    }

    // ------------------------------------------------------------------ head

    static void drawBackHair(Gfx g, Look l, Pose p, Body b) {
        if (l.kind == Look.MONSTER || l.hair == Look.H_NONE) return;
        float hy = b.headY, r = b.headR;
        float swing = (float) Math.sin(p.time * 2.4f + p.seed) * 4 + (float) Math.sin(p.walk) * p.walkAmt * 8
                + p.wind * r * 0.7f * (1 + 0.3f * (float) Math.sin(p.time * 6 + p.seed));     // hair flies in the wind
        g.color(l.hairColor);
        if (l.hair == Look.H_BRAID && l.headwear != Look.HW_PALLU) {
            // thick single braid falling behind one shoulder
            float bx = -p.facing * r * 0.75f;
            for (int i = 0; i < 7; i++) {
                float y = hy + r * 0.5f + i * r * 0.32f;
                g.oval(bx + (i % 2 == 0 ? -3 : 3) + swing * i / 7f, y, r * 0.24f, r * 0.2f);
            }
            g.color(0xFFE5B530);
            g.oval(bx + swing, hy + r * 0.5f + 7 * r * 0.32f, r * 0.12f, r * 0.08f);
        } else if (l.hair == Look.H_LONG) {
            g.begin();
            g.moveTo(-r * 0.95f, hy);
            g.quadTo(-r * 1.25f, hy + r * 1.6f, -r * 0.9f + swing, hy + r * 2.4f);
            g.lineTo(r * 0.9f + swing, hy + r * 2.4f);
            g.quadTo(r * 1.25f, hy + r * 1.6f, r * 0.95f, hy);
            g.close(); g.fillPath();
        }
    }

    static void drawHead(Gfx g, Look l, Pose p, Body b) {
        float r = b.headR;
        float f = p.facing;
        if (l.kind == Look.MONSTER) { drawMonsterHead(g, l, p, r); return; }
        boolean witch = l.kind == Look.WITCH;
        int skin = faceSkin(l, p);

        if (p.disguised && l.kind == Look.WITCH) {
            // the sweet-seller's shawl: a hood behind the head, face stays visible
            g.color(0xFFC9772E);
            g.begin();
            g.moveTo(-r * 1.25f, r * 1.25f);
            g.cubicTo(-r * 1.45f, -r * 1.55f, r * 1.45f, -r * 1.55f, r * 1.25f, r * 1.25f);
            g.close(); g.fillPath();
            g.color(0xFFF2C230);
            g.begin();
            g.moveTo(-r * 1.25f, r * 1.25f);
            g.cubicTo(-r * 1.45f, -r * 1.55f, r * 1.45f, -r * 1.55f, r * 1.25f, r * 1.25f);
            g.strokePath(3);
        }
        // a hood over the head (behind it; its edge comes over the hair in drawHeadwear)
        if (l.headwear == Look.HW_HOOD && !p.noHeadwear) {
            g.color(shade(l.primary, 0.85f));
            g.begin();
            g.moveTo(-r * 1.3f, r * 1.3f);
            g.cubicTo(-r * 1.5f, -r * 1.6f, r * 1.5f, -r * 1.6f, r * 1.3f, r * 1.3f);
            g.close(); g.fillPath();
        }
        // a ponytail swinging behind the head (follow-through: it settles after the head)
        if (l.hair == Look.H_PONYTAIL) {
            float px = -f * r * 0.72f, swing = (float) Math.sin(p.time * 2.6f) * 4 + p.wind * 10 - p.nod * 6;
            g.color(l.hairColor);
            g.begin(); g.moveTo(px, -r * 0.55f);
            g.quadTo(px - f * r * 0.45f + swing * 0.3f, r * 0.3f, px - f * r * 0.2f + swing, r * 1.25f);
            g.quadTo(px - f * r * 0.05f + swing * 0.6f, r * 0.5f, px + f * r * 0.1f, -r * 0.45f);
            g.close(); g.fillPath();
            if (l.curly) for (int i = 0; i < 3; i++) g.oval(px - f * r * 0.2f + swing * (0.4f + i * 0.2f), r * (0.5f + i * 0.28f), r * 0.17f, r * 0.17f);
            // the band and an LED clip
            g.color(l.ledClip ? 0xFF40C4FF : 0xFFC62828);
            g.oval(px, -r * 0.5f, r * 0.13f, r * 0.1f);
            if (l.ledClip) { g.color(alpha(0xFF40C4FF, 0.35f + 0.25f * (float) Math.sin(p.time * 4))); g.oval(px, -r * 0.5f, r * 0.3f, r * 0.24f); }
        }
        // pigtails (behind ears)
        if (l.hair == Look.H_PIGTAILS) {
            for (int s = -1; s <= 1; s += 2) {
                float sx = s * r * 0.95f;
                float swing = (float) Math.sin(p.time * 3 + s) * 3 + p.wind * 8 * (1 + 0.3f * (float) Math.sin(p.time * 6 + s));
                g.color(l.hairColor);
                for (int i = 0; i < 4; i++) g.oval(sx + s * 2 + swing * i / 4f, r * (0.35f + i * 0.33f), r * 0.2f, r * 0.19f);
                if (l.ribbon1 != 0) {
                    int rc = s < 0 ? l.ribbon1 : l.ribbon2;
                    g.color(rc);
                    float ry = r * 0.25f;
                    g.begin(); g.moveTo(sx, ry); g.lineTo(sx - r * 0.25f, ry - r * 0.15f); g.lineTo(sx - r * 0.25f, ry + r * 0.15f); g.close(); g.fillPath();
                    g.begin(); g.moveTo(sx, ry); g.lineTo(sx + r * 0.25f, ry - r * 0.15f); g.lineTo(sx + r * 0.25f, ry + r * 0.15f); g.close(); g.fillPath();
                    g.oval(sx, ry, r * 0.07f, r * 0.07f);
                }
            }
        }
        // ears
        g.color(shade(skin, 0.9f));
        g.oval(-r * 0.93f, r * 0.08f, r * 0.15f, r * 0.2f);
        g.oval(r * 0.93f, r * 0.08f, r * 0.15f, r * 0.2f);
        if ((l.condition & Look.C_HEARING_AID) != 0) {
            // v35: a hearing aid: a small beige case curved behind each ear, its clear tube into the ear
            for (int s2 = -1; s2 <= 1; s2 += 2) {
                g.color(0xFFD8C3A5);
                g.begin(); g.moveTo(s2 * r * 0.98f, -r * 0.08f); g.quadTo(s2 * r * 1.13f, r * 0.06f, s2 * r * 1.0f, r * 0.24f); g.strokePath(Math.max(2f, r * 0.07f));
                g.color(0x99E0F2F1);
                g.line(s2 * r * 0.98f, -r * 0.08f, s2 * r * 0.9f, r * 0.06f, Math.max(1.2f, r * 0.025f));
            }
        }
        if (l.earrings) {
            g.color(0xFFE5B530);
            if (l.kind == Look.WOMAN) {
                for (int s = -1; s <= 1; s += 2) {
                    g.oval(s * r * 0.95f, r * 0.32f, r * 0.08f, r * 0.05f);
                    g.begin(); g.moveTo(s * r * 0.95f - r * 0.1f, r * 0.46f); g.lineTo(s * r * 0.95f + r * 0.1f, r * 0.46f);
                    g.lineTo(s * r * 0.95f, r * 0.33f); g.close(); g.fillPath();
                }
            } else {
                g.oval(-r * 0.95f, r * 0.27f, r * 0.05f, r * 0.05f);
                g.oval(r * 0.95f, r * 0.27f, r * 0.05f, r * 0.05f);
            }
        }
        // face
        float jaw = l.isChild() ? 1.0f : (witch ? 1.12f : 1.04f);
        g.begin();
        g.moveTo(-r * 0.92f, -r * 0.1f);
        g.cubicTo(-r * 0.92f, -r * 1.12f, r * 0.92f, -r * 1.12f, r * 0.92f, -r * 0.1f);
        g.cubicTo(r * 0.92f, r * 0.62f * jaw, r * 0.45f, r * 0.98f * jaw, 0, r * 1.0f * jaw);
        g.cubicTo(-r * 0.45f, r * 0.98f * jaw, -r * 0.92f, r * 0.62f * jaw, -r * 0.92f, -r * 0.1f);
        g.close();
        g.color(shade(skin, 0.7f)); g.strokePath(2.5f);
        g.radial(-r * 0.2f, -r * 0.3f, r * 1.4f, lighten(skin, 0.1f), shade(skin, 0.9f)); g.fillPath();

        if (p.redFace) { g.color(0x55FF1A1A); g.fillPath(); }
        if (witch && !p.disguised) {
            g.color(0x2224331A); g.fillPath();
        }

        // front hair (fringe & parting)
        drawFrontHair(g, l, p, r);

        float ex = f * r * 0.12f; // features shifted toward facing side
        // cheeks blush
        if (!witch) {
            g.color(alpha(0xFFFF6F61, l.isChild() ? 0.28f : 0.16f));
            g.oval(-r * 0.5f + ex, r * 0.35f, r * 0.17f, r * 0.1f);
            g.oval(r * 0.5f + ex, r * 0.35f, r * 0.17f, r * 0.1f);
        }
        if (l.wrinkles && !p.disguised) {
            g.color(alpha(shade(skin, 0.55f), 0.8f));
            g.line(-r * 0.6f + ex, r * 0.05f, -r * 0.45f + ex, r * 0.12f, 1.6f);
            g.line(r * 0.45f + ex, r * 0.12f, r * 0.6f + ex, r * 0.05f, 1.6f);
            g.line(-r * 0.25f + ex, -r * 0.5f, r * 0.25f + ex, -r * 0.5f, 1.4f);
            g.line(-r * 0.4f + ex, r * 0.5f, -r * 0.3f + ex, r * 0.62f, 1.4f);
            g.color(0xFF6B6B3A);
            g.oval(r * 0.45f + ex, r * 0.4f, r * 0.05f, r * 0.05f);
        }

        drawEyes(g, l, p, r, ex);
        drawNose(g, l, p, r, ex);
        drawMouth(g, l, p, r, ex);

        if (l.mustache > 0) {
            g.color(l.hairColor);
            float my = r * 0.48f;
            if (l.mustache == 2) {
                for (int s = -1; s <= 1; s += 2) {
                    g.begin();
                    g.moveTo(ex, my - r * 0.04f);
                    g.quadTo(ex + s * r * 0.35f, my - r * 0.12f, ex + s * r * 0.55f, my + r * 0.02f);
                    g.quadTo(ex + s * r * 0.72f, my - r * 0.05f, ex + s * r * 0.7f, my - r * 0.22f);
                    g.quadTo(ex + s * r * 0.62f, my + r * 0.14f, ex, my + r * 0.07f);
                    g.close(); g.fillPath();
                }
            } else {
                g.roundRect(ex - r * 0.32f, my - r * 0.05f, r * 0.64f, r * 0.12f, r * 0.06f);
            }
        }
        if (l.beard) {
            g.color(l.hairColor);
            g.begin();
            g.moveTo(-r * 0.85f, r * 0.1f);
            g.quadTo(-r * 0.75f, r * 1.05f, 0, r * 1.15f);
            g.quadTo(r * 0.75f, r * 1.05f, r * 0.85f, r * 0.1f);
            g.quadTo(r * 0.6f, r * 0.7f, ex + r * 0.25f, r * 0.72f);
            g.lineTo(ex - r * 0.25f, r * 0.72f);
            g.quadTo(-r * 0.6f, r * 0.7f, -r * 0.85f, r * 0.1f);
            g.close(); g.fillPath();
            // keep the mouth visible
            drawMouth(g, l, p, r, ex);
        }
        // bindi / tilak / sindoor
        if (l.sindoor && !(l.headwear == Look.HW_PALLU && false)) {
            g.color(0xFFD32F2F);
            g.line(0, -r * 0.95f, 0, -r * 0.7f, 3);
        }
        if (l.bindi != 0 && !p.disguised) {
            g.color(l.bindi);
            g.oval(ex * 0.6f, -r * 0.32f, r * (l.kind == Look.WOMAN ? 0.075f : 0.06f), r * (l.kind == Look.WOMAN ? 0.075f : 0.06f));
        }
        if (l.tilak != 0) {
            g.color(l.tilak);
            g.line(ex * 0.6f, -r * 0.55f, ex * 0.6f, -r * 0.28f, r * 0.07f);
            if (l.kind == Look.MAN && l.headwear == Look.HW_TURBAN && l.necklace == 4) {
                g.color(0xFFF08A24);
                g.line(ex * 0.6f - r * 0.06f, -r * 0.55f, ex * 0.6f - r * 0.06f, -r * 0.3f, 2);
                g.line(ex * 0.6f + r * 0.06f, -r * 0.55f, ex * 0.6f + r * 0.06f, -r * 0.3f, 2);
            }
        }
        if (p.tears) {
            g.color(0xCC64B5F6);
            float ty = r * 0.15f + ((p.time * 60) % 30);
            g.oval(-r * 0.38f + ex, ty, r * 0.06f, r * 0.09f);
            g.oval(r * 0.38f + ex, ty + 8, r * 0.06f, r * 0.09f);
        }
        if (p.sweat) {
            g.color(0xCC81D4FA);
            g.oval(r * 0.7f, -r * 0.45f + ((p.time * 20) % 10), r * 0.07f, r * 0.11f);
        }

        // glasses over the eyes
        if (l.glasses == 1) {
            g.color(0xFF37474F);
            g.strokeOval(ex - r * 0.36f, -r * 0.08f, r * 0.24f, r * 0.2f, 2.2f);
            g.strokeOval(ex + r * 0.36f, -r * 0.08f, r * 0.24f, r * 0.2f, 2.2f);
            g.line(ex - r * 0.12f, -r * 0.1f, ex + r * 0.12f, -r * 0.1f, 2);
            g.color(alpha(0xFFFFFFFF, 0.25f));
            g.oval(ex - r * 0.42f, -r * 0.14f, r * 0.08f, r * 0.05f);
            g.oval(ex + r * 0.3f, -r * 0.14f, r * 0.08f, r * 0.05f);
        } else if (l.glasses == 2) {
            // dark AR glasses: one visor across the eyes, a red line of light when they glow
            g.color(0xFF101418);
            g.roundRect(ex - r * 0.68f, -r * 0.24f, r * 1.36f, r * 0.34f, r * 0.12f);
            g.color(alpha(0xFF4FC3F7, 0.18f));
            g.roundRect(ex - r * 0.62f, -r * 0.2f, r * 0.5f, r * 0.12f, r * 0.05f);
            if (l.glowGlasses) {
                g.color(alpha(0xFFFF1744, 0.55f + 0.3f * (float) Math.sin(p.time * 3)));
                g.rect(ex - r * 0.6f, -r * 0.1f, r * 1.2f, r * 0.05f);
            }
        } else if (l.glasses == 3) {
            // v34: goggles — two big round lenses with thick rims on a strap round the head; the eyes show through
            g.color(0xFF3E2723);
            g.line(-r * 0.98f, -r * 0.1f, r * 0.98f, -r * 0.1f, Math.max(2, r * 0.11f));
            for (int s2 = -1; s2 <= 1; s2 += 2) {
                float gx = ex + s2 * r * 0.37f;
                g.color(0xFF5D4037);
                g.oval(gx, -r * 0.08f, r * 0.3f, r * 0.27f);
                g.color(alpha(0xFF80DEEA, 0.5f));
                g.oval(gx, -r * 0.08f, r * 0.22f, r * 0.2f);
                g.color(alpha(0xFFFFFFFF, 0.45f));
                g.oval(gx - r * 0.08f, -r * 0.15f, r * 0.06f, r * 0.04f);
            }
        } else if (l.glasses == 4) {
            // v34: a cloth blindfold over both eyes, knotted behind with two ends hanging
            g.color(0xFF2B2B33);
            g.begin(); g.moveTo(-r * 0.99f, -r * 0.24f); g.quadTo(0, -r * 0.3f, r * 0.99f, -r * 0.24f); g.lineTo(r * 0.97f, r * 0.06f); g.quadTo(0, r * 0.02f, -r * 0.97f, r * 0.06f); g.close(); g.fillPath();
            g.color(0xFF3C3C46);
            g.line(-r * 0.9f, -r * 0.16f, r * 0.9f, -r * 0.16f, Math.max(1, r * 0.03f));
            float kx = -f * r * 0.98f;
            g.color(0xFF2B2B33);
            g.oval(kx, -r * 0.09f, r * 0.12f, r * 0.11f);
            g.line(kx, -r * 0.05f, kx - f * r * 0.15f, r * 0.45f, r * 0.09f);
            g.line(kx, -r * 0.05f, kx - f * r * 0.02f, r * 0.5f, r * 0.08f);
        } else if (l.glasses == 5) {
            // v34: a black patch over the left eye on a thin strap
            g.color(0xFF15161A);
            g.line(ex - r * 0.6f, -r * 0.3f, ex + r * 0.85f, -r * 0.62f, Math.max(1.5f, r * 0.05f));
            g.oval(ex - r * 0.36f, -r * 0.08f, r * 0.21f, r * 0.19f);
        }
        if ((l.injury & Look.INJ_HEAD) != 0) {
            // v34: a white bandage round the forehead with a pad over the temple
            g.color(0xFFC9C7C0);
            g.begin(); g.moveTo(-r * 0.9f, -r * 0.47f); g.quadTo(0, -r * 0.4f, r * 0.9f, -r * 0.47f); g.strokePath(r * 0.22f);
            g.color(0xFFF6F5F0);
            g.begin(); g.moveTo(-r * 0.9f, -r * 0.47f); g.quadTo(0, -r * 0.4f, r * 0.9f, -r * 0.47f); g.strokePath(r * 0.19f);
            g.oval(ex + f * r * 0.45f, -r * 0.47f, r * 0.15f, r * 0.13f);
            g.color(0xFFE0D8C8);
            g.line(ex + f * r * 0.36f, -r * 0.47f, ex + f * r * 0.54f, -r * 0.47f, Math.max(1, r * 0.025f));
        }
        // headwear on top
        drawHeadwear(g, l, p, r);
    }

    static void drawFrontHair(Gfx g, Look l, Pose p, float r) {
        if (l.hair == Look.H_NONE) return;
        boolean turban = l.headwear == Look.HW_TURBAN;
        if (turban && !p.noHeadwear) return;
        g.color(l.hairColor);
        if (l.kind == Look.WITCH && p.disguised) return;
        if (l.kind == Look.WITCH && !p.disguised) {
            // stringy grey hair
            g.begin();
            g.moveTo(-r * 0.98f, r * 0.4f);
            g.cubicTo(-r * 1.05f, -r * 1.2f, r * 1.05f, -r * 1.2f, r * 0.98f, r * 0.4f);
            g.lineTo(r * 0.8f, -r * 0.3f);
            g.quadTo(0, -r * 0.75f, -r * 0.8f, -r * 0.3f);
            g.close(); g.fillPath();
            return;
        }
        if (l.hair == Look.H_SHORT || l.hair == Look.H_PONYTAIL || p.noHeadwear) {
            if (p.noHeadwear && l.headwear == Look.HW_TURBAN) {
                // bald-ish head with a little hair (turban stolen)
                g.color(shade(l.skin, 0.95f));
                g.oval(0, -r * 0.6f, r * 0.85f, r * 0.45f);
                g.color(l.hairColor);
                g.oval(-r * 0.82f, -r * 0.15f, r * 0.15f, r * 0.3f);
                g.oval(r * 0.82f, -r * 0.15f, r * 0.15f, r * 0.3f);
                g.color(alpha(0xFFFFFFFF, 0.4f));
                g.oval(-r * 0.2f, -r * 0.8f, r * 0.25f, r * 0.1f);
                return;
            }
            g.begin();
            g.moveTo(-r * 0.95f, -r * 0.05f);
            g.cubicTo(-r * 1.0f, -r * 1.25f, r * 1.0f, -r * 1.25f, r * 0.95f, -r * 0.05f);
            g.quadTo(r * 0.6f, -r * 0.65f, 0, -r * 0.62f);
            g.quadTo(-r * 0.6f, -r * 0.65f, -r * 0.95f, -r * 0.05f);
            g.close(); g.fillPath();
            return;
        }
        // long hair parted in the middle (girls & women)
        g.begin();
        g.moveTo(-r * 0.98f, r * 0.15f);
        g.cubicTo(-r * 1.08f, -r * 1.28f, r * 1.08f, -r * 1.28f, r * 0.98f, r * 0.15f);
        g.quadTo(r * 0.8f, -r * 0.5f, 0, -r * 0.68f);
        g.quadTo(-r * 0.8f, -r * 0.5f, -r * 0.98f, r * 0.15f);
        g.close(); g.fillPath();
        if (l.curly) {
            for (int i = -3; i <= 3; i++) g.oval(i * r * 0.27f, -r * 0.82f + Math.abs(i) * r * 0.08f, r * 0.16f, r * 0.16f);
        }
        g.color(alpha(0xFFFFFFFF, 0.18f));
        g.oval(-r * 0.35f, -r * 0.85f, r * 0.25f, r * 0.08f);
        if (l.hair == Look.H_BUN && l.headwear != Look.HW_PALLU) {
            g.color(l.hairColor);
            g.oval(0, -r * 1.05f, r * 0.38f, r * 0.3f);
            g.color(0xFFF7F2E6);
            for (int i = 0; i < 5; i++) g.oval(-r * 0.3f + i * r * 0.15f, -r * 0.88f, 2.5f, 2.5f);
        }
    }

    static void drawEyes(Gfx g, Look l, Pose p, float r, float ex) {
        boolean child = l.isChild();
        float ew = r * (child ? 0.2f : 0.16f), eh = r * (child ? 0.24f : 0.17f);
        float ey = -r * 0.08f;
        float sep = r * 0.36f;
        float closed = p.eyesClosed ? 1f : p.blink;
        int em = p.emotion;
        if (em == Pose.LAUGH) closed = Math.max(closed, 0.65f);
        if (em == Pose.SURPRISED || em == Pose.SCARED) { ew *= 1.15f; eh *= 1.25f; }
        boolean witch = l.kind == Look.WITCH && !p.disguised;
        for (int s = -1; s <= 1; s += 2) {
            float cx = s * sep + ex;
            if (s == -1 && (l.condition & Look.C_ONE_EYE) != 0 && l.glasses != 5) {
                // v35: one eye lost (no patch told): that eye stays closed, the lid a gentle line, a faint scar across it
                g.color(INK);
                g.begin(); g.moveTo(cx - ew, ey); g.quadTo(cx, ey + eh * 0.45f, cx + ew, ey); g.strokePath(3);
                g.color(shade(faceSkin(l, p), 0.72f));
                g.line(cx - ew * 0.45f, ey - eh * 1.15f, cx + ew * 0.35f, ey + eh * 0.95f, Math.max(1.5f, ew * 0.16f));
            } else if (closed > 0.85f) {
                g.color(INK);
                g.begin();
                if (em == Pose.LAUGH || em == Pose.HAPPY) { g.moveTo(cx - ew, ey + 2); g.quadTo(cx, ey - eh, cx + ew, ey + 2); }
                else { g.moveTo(cx - ew, ey); g.quadTo(cx, ey + eh * 0.5f, cx + ew, ey); }
                g.strokePath(3);
            } else {
                float h = eh * (1 - closed);
                if (witch) {
                    g.color(0xFF1B1B12);
                    g.oval(cx, ey, ew * 1.1f, h * 1.05f);
                    g.color(l.eyeColor);
                    g.oval(cx + p.facing * ew * 0.15f, ey, ew * 0.7f, h * 0.7f);
                    g.color(0xFF1B1B12);
                    g.oval(cx + p.facing * ew * 0.15f, ey, ew * 0.18f, h * 0.6f);
                } else {
                    g.color(0xFFFFFFFF);
                    g.oval(cx, ey, ew, h);
                    g.color(l.eyeColor);
                    // v34: the eyes look at whoever speaks (otherwise the way the head faces), with small shifts
                    float gx = Math.max(-1f, Math.min(1f, p.gazeX + (1 - p.gazeHeld) * p.facing * 0.78f));
                    float ix = cx + gx * ew * 0.32f, iy = ey + h * 0.05f + p.gazeY * h * 0.22f;
                    g.oval(ix, iy, ew * 0.62f, Math.min(h, ew * 0.7f));
                    g.color(0xFF120A06);
                    g.oval(ix, iy, ew * 0.32f, Math.min(h * 0.8f, ew * 0.38f));
                    g.color(0xFFFFFFFF);
                    g.oval(ix - ew * 0.18f, iy - h * 0.3f, ew * 0.16f, ew * 0.16f);
                    g.color(INK);
                    g.begin(); g.moveTo(cx - ew * 1.1f, ey - h * 0.35f); g.quadTo(cx, ey - h * 1.25f, cx + ew * 1.1f, ey - h * 0.35f);
                    g.strokePath(l.female ? 2.8f : 2.2f);
                    if (l.female && !witch) { // lashes
                        g.line(cx + s * ew * 0.9f, ey - h * 0.55f, cx + s * ew * 1.25f, ey - h * 0.85f, 2);
                    }
                }
            }
            // brows
            float by = ey - eh - r * 0.12f;
            float inner = 0, outer = 0;
            switch (em) {
                case Pose.ANGRY: case Pose.DETERMINED: case Pose.EVIL: inner = r * 0.1f; outer = -r * 0.05f; break;
                case Pose.SUSPICIOUS: inner = r * 0.06f; outer = r * 0.02f; break;
                case Pose.RELIEVED: inner = -r * 0.02f; outer = -r * 0.02f; break;
                case Pose.SAD: case Pose.SCARED: case Pose.PAIN: inner = -r * 0.09f; outer = r * 0.04f; break;
                case Pose.SURPRISED: case Pose.CURIOUS: inner = -r * 0.08f; outer = -r * 0.08f; break;
                case Pose.HAPPY: case Pose.LAUGH: inner = -r * 0.03f; outer = -r * 0.03f; break;
                default:
            }
            float bw = l.kind == Look.WOMAN ? 2.6f : (l.isChild() ? 3.2f : 4.2f);
            if (l.kind == Look.MAN || l.kind == Look.OLD_MAN) bw = 5f;
            if (child && l.female && "".equals("")) bw = l.hair == Look.H_BRAID ? 4.2f : 3.0f; // Vrinda's thick brows
            g.color(witch ? 0xFF55585A : l.hairColor);
            g.line(cx - s * ew * 1.1f, by + inner, cx + s * ew * 1.1f, by + outer, bw);
        }
    }

    static void drawNose(Gfx g, Look l, Pose p, float r, float ex) {
        int skin = faceSkin(l, p);
        if (l.crookedNose && !p.disguised) {
            g.color(shade(skin, 0.85f));
            float f = p.facing;
            g.begin();
            g.moveTo(ex - f * r * 0.05f, -r * 0.05f);
            g.quadTo(ex + f * r * 0.35f, r * 0.15f, ex + f * r * 0.45f, r * 0.42f);
            g.quadTo(ex + f * r * 0.2f, r * 0.38f, ex + f * r * 0.02f, r * 0.33f);
            g.close();
            g.fillPath();
            g.color(shade(skin, 0.6f)); g.strokePath(2);
            g.color(0xFF6B6B3A);
            g.oval(ex + f * r * 0.25f, r * 0.18f, r * 0.04f, r * 0.04f);
            return;
        }
        g.color(shade(skin, 0.72f));
        g.begin();
        g.moveTo(ex + p.facing * r * 0.04f, r * 0.08f);
        g.quadTo(ex + p.facing * r * 0.13f, r * 0.24f, ex - p.facing * r * 0.02f, r * 0.27f);
        g.strokePath(2.4f);
    }

    static void drawMouth(Gfx g, Look l, Pose p, float r, float ex) {
        float my = r * 0.6f;
        float mw = r * (l.isChild() ? 0.2f : 0.22f);
        float open = Math.max(0, Math.min(1, p.mouth));
        int em = p.emotion;
        boolean witch = l.kind == Look.WITCH && !p.disguised;
        float smile = 0;
        switch (em) {
            case Pose.HAPPY: case Pose.PROUD: smile = 1; break;
            case Pose.LAUGH: smile = 1.4f; open = Math.max(open, 0.55f); break;
            case Pose.EVIL: smile = 0.9f; break;
            case Pose.SAD: case Pose.SCARED: case Pose.PAIN: smile = -0.8f; break;
            case Pose.ANGRY: smile = -0.5f; break;
            case Pose.SURPRISED: open = Math.max(open, 0.4f); break;
            case Pose.CURIOUS: smile = 0.3f; break;
            case Pose.RELIEVED: smile = 0.6f; break;
            case Pose.SUSPICIOUS: smile = -0.3f; break;
            default:
        }
        if (open < 0.08f) {
            g.color(INK);
            g.begin();
            g.moveTo(ex - mw, my - smile * r * 0.04f);
            g.quadTo(ex, my + smile * r * 0.14f, ex + mw, my - smile * r * 0.04f);
            g.strokePath(2.6f);
            if (l.dimples && smile > 0.5f) {
                g.color(shade(l.skin, 0.75f));
                g.oval(ex - mw * 1.6f, my - r * 0.02f, 2.2f, 2.2f);
                g.oval(ex + mw * 1.6f, my - r * 0.02f, 2.2f, 2.2f);
            }
            return;
        }
        float h = r * (0.06f + open * 0.26f);
        float w = mw * (1.0f + (smile > 0 ? 0.25f : 0) - open * 0.15f);
        g.begin();
        g.moveTo(ex - w, my - smile * r * 0.03f);
        g.quadTo(ex, my - r * 0.04f, ex + w, my - smile * r * 0.03f);
        g.quadTo(ex, my + h * 2f, ex - w, my - smile * r * 0.03f);
        g.close();
        g.color(0xFF5A1A1A); g.fillPath();
        // teeth
        g.color(witch ? 0xFF3A3A2A : 0xFFFFFFFF);
        if (witch) {
            g.rect(ex - w * 0.4f, my - r * 0.02f, w * 0.18f, h * 0.45f);
            g.rect(ex + w * 0.2f, my - r * 0.02f, w * 0.18f, h * 0.35f);
        } else {
            g.rect(ex - w * 0.62f, my - r * 0.025f, w * 1.24f, Math.min(h * 0.32f, r * 0.06f));
        }
        // tongue
        if (open > 0.35f) {
            g.color(0xFFE57373);
            g.oval(ex, my + h * 1.35f, w * 0.45f, h * 0.35f);
        }
        if (l.dimples && smile > 0.5f) {
            g.color(shade(l.skin, 0.75f));
            g.oval(ex - w * 1.35f, my, 2.2f, 2.2f);
            g.oval(ex + w * 1.35f, my, 2.2f, 2.2f);
        }
    }

    static void drawHeadwear(Gfx g, Look l, Pose p, float r) {
        int hw = l.headwear;
        if (p.wearsTurban) { drawTurbanShape(g, 0, -r * 0.75f, r * 1.2f, p.turbanColor, p.turbanBand, false); return; }
        if (p.noHeadwear && hw == Look.HW_TURBAN) return;
        if (p.disguised && l.kind == Look.WITCH) {
            // front edge of the shawl over the hair line
            g.color(0xFFC9772E);
            g.begin();
            g.moveTo(-r * 1.0f, -r * 0.2f);
            g.cubicTo(-r * 1.0f, -r * 1.2f, r * 1.0f, -r * 1.2f, r * 1.0f, -r * 0.2f);
            g.cubicTo(r * 0.8f, -r * 0.75f, -r * 0.8f, -r * 0.75f, -r * 1.0f, -r * 0.2f);
            g.close(); g.fillPath();
            g.color(0xFFF2C230);
            g.begin();
            g.moveTo(-r * 1.0f, -r * 0.2f);
            g.cubicTo(-r * 0.8f, -r * 0.75f, r * 0.8f, -r * 0.75f, r * 1.0f, -r * 0.2f);
            g.strokePath(3);
            return;
        }
        switch (hw) {
            case Look.HW_TURBAN:
                drawTurbanShape(g, 0, -r * 0.62f, r, l.headColor, l.headBand, l.kalgi);
                break;
            case Look.HW_HOOD: {
                // the front edge of the hood over the hair line
                g.color(shade(l.primary, 0.85f));
                g.begin();
                g.moveTo(-r * 1.05f, -r * 0.15f);
                g.cubicTo(-r * 1.05f, -r * 1.25f, r * 1.05f, -r * 1.25f, r * 1.05f, -r * 0.15f);
                g.cubicTo(r * 0.85f, -r * 0.72f, -r * 0.85f, -r * 0.72f, -r * 1.05f, -r * 0.15f);
                g.close(); g.fillPath();
                g.color(shade(l.primary, 0.65f));
                g.begin(); g.moveTo(-r * 1.05f, -r * 0.15f); g.cubicTo(-r * 0.85f, -r * 0.72f, r * 0.85f, -r * 0.72f, r * 1.05f, -r * 0.15f); g.strokePath(2.5f);
                break;
            }
            case Look.HW_PALLU: {
                g.color(l.primary);
                g.begin();
                g.moveTo(-r * 1.05f, r * 0.7f);
                g.cubicTo(-r * 1.2f, -r * 1.45f, r * 1.2f, -r * 1.45f, r * 1.05f, r * 0.7f);
                g.lineTo(r * 0.88f, r * 0.6f);
                g.cubicTo(r * 0.9f, -r * 1.0f, -r * 0.9f, -r * 1.0f, -r * 0.88f, r * 0.6f);
                g.close();
                g.fillPath();
                g.color(l.secondary);
                g.begin();
                g.moveTo(-r * 0.88f, r * 0.6f);
                g.cubicTo(-r * 0.9f, -r * 1.0f, r * 0.9f, -r * 1.0f, r * 0.88f, r * 0.6f);
                g.strokePath(4);
                // maang tikka
                g.color(0xFFE5B530);
                g.line(0, -r * 0.82f, 0, -r * 0.55f, 2);
                g.oval(0, -r * 0.5f, r * 0.07f, r * 0.07f);
                break;
            }
            case Look.HW_WITCH_HAT: {
                g.color(0xFF1A171C);
                g.oval(0, -r * 0.62f, r * 1.55f, r * 0.28f);
                g.begin();
                g.moveTo(-r * 0.85f, -r * 0.65f);
                g.quadTo(-r * 0.4f, -r * 1.6f, r * 0.15f, -r * 2.2f);
                g.quadTo(r * 0.9f, -r * 2.2f, r * 1.1f, -r * 1.7f);
                g.quadTo(r * 0.6f, -r * 1.75f, r * 0.45f, -r * 1.5f);
                g.quadTo(r * 0.6f, -r * 1.0f, r * 0.85f, -r * 0.65f);
                g.close(); g.fillPath();
                g.color(0xFF2F5A3A);
                g.rect(-r * 0.8f, -r * 0.85f, r * 1.6f, r * 0.16f);
                g.color(0xFF6E6A5E);
                g.line(-r * 0.3f, -r * 1.2f, -r * 0.1f, -r * 1.05f, 2);
                break;
            }
            case Look.HW_HORNS: {
                for (int s = -1; s <= 1; s += 2) {
                    g.begin();
                    g.moveTo(s * r * 0.55f, -r * 0.65f);
                    g.cubicTo(s * r * 1.5f, -r * 0.9f, s * r * 1.75f, -r * 0.25f, s * r * 1.35f, r * 0.15f);
                    g.cubicTo(s * r * 1.45f, -r * 0.4f, s * r * 1.1f, -r * 0.55f, s * r * 0.65f, -r * 0.35f);
                    g.close();
                    g.color(0xFF141012); g.fillPath();
                    g.color(0xFF3A3236); g.strokePath(2);
                }
                break;
            }
            case Look.HW_CROWN: {
                g.color(0xFFE5B530);
                g.begin();
                g.moveTo(-r * 0.7f, -r * 0.65f); g.lineTo(-r * 0.75f, -r * 1.25f); g.lineTo(-r * 0.35f, -r * 0.95f);
                g.lineTo(0, -r * 1.4f); g.lineTo(r * 0.35f, -r * 0.95f); g.lineTo(r * 0.75f, -r * 1.25f); g.lineTo(r * 0.7f, -r * 0.65f);
                g.close(); g.fillPath();
                g.color(0xFFC62828); g.oval(0, -r * 0.85f, r * 0.08f, r * 0.08f);
                break;
            }
            default:
        }
    }

    static void drawTurbanShape(Gfx g, float cx, float cy, float r, int color, int band, boolean kalgi) {
        g.color(shade(color, 0.7f));
        g.oval(cx, cy + r * 0.05f, r * 1.08f, r * 0.62f);
        g.color(color);
        g.oval(cx, cy, r * 1.02f, r * 0.58f);
        // wraps
        g.color(shade(color, 0.78f));
        for (int i = 0; i < 3; i++) {
            g.begin();
            g.moveTo(cx - r * 0.98f, cy + r * (0.12f - i * 0.18f));
            g.quadTo(cx, cy - r * (0.05f + i * 0.2f), cx + r * 0.98f, cy + r * (0.25f - i * 0.18f));
            g.strokePath(3);
        }
        if (band != 0) {
            g.color(band);
            g.begin();
            g.moveTo(cx - r * 1.0f, cy + r * 0.22f);
            g.quadTo(cx, cy + r * 0.05f, cx + r * 1.0f, cy + r * 0.32f);
            g.strokePath(r * 0.14f);
        }
        if (kalgi) {
            g.color(0xFFE5B530);
            g.oval(cx + r * 0.1f, cy - r * 0.15f, r * 0.14f, r * 0.14f);
            g.color(0xFF1E9E5A);
            g.oval(cx + r * 0.1f, cy - r * 0.15f, r * 0.08f, r * 0.08f);
            g.color(0xFFF7F2E6);
            g.begin();
            g.moveTo(cx + r * 0.1f, cy - r * 0.2f);
            g.quadTo(cx + r * 0.05f, cy - r * 0.95f, cx + r * 0.45f, cy - r * 1.05f);
            g.quadTo(cx + r * 0.2f, cy - r * 0.6f, cx + r * 0.18f, cy - r * 0.2f);
            g.close(); g.fillPath();
        }
    }

    // ------------------------------------------------------------------ monster head

    static void drawMonsterHead(Gfx g, Look l, Pose p, float r) {
        float f = p.facing;
        // torn ears
        g.color(shade(l.furColor, 0.8f));
        for (int s = -1; s <= 1; s += 2) {
            g.begin();
            g.moveTo(s * r * 0.8f, -r * 0.3f);
            g.lineTo(s * r * 1.3f, -r * 0.55f);
            g.lineTo(s * r * 1.18f, -r * 0.3f);
            g.lineTo(s * r * 1.32f, -r * 0.2f);
            g.lineTo(s * r * 0.85f, r * 0.05f);
            g.close(); g.fillPath();
        }
        // head
        g.begin();
        g.moveTo(-r * 1.0f, -r * 0.2f);
        g.cubicTo(-r * 1.05f, -r * 1.15f, r * 1.05f, -r * 1.15f, r * 1.0f, -r * 0.2f);
        g.cubicTo(r * 1.15f, r * 0.7f, r * 0.7f, r * 1.1f, 0, r * 1.12f);
        g.cubicTo(-r * 0.7f, r * 1.1f, -r * 1.15f, r * 0.7f, -r * 1.0f, -r * 0.2f);
        g.close();
        g.color(shade(l.furColor, 0.55f)); g.strokePath(4);
        g.radial(-r * 0.2f, -r * 0.2f, r * 1.4f, lighten(l.furColor, 0.12f), shade(l.furColor, 0.85f)); g.fillPath();
        furStrokes(g, l.furColor, -r, -r, r * 2, r * 2, p.seed + 3);
        // muzzle
        g.color(0xFF6D5546);
        g.oval(f * r * 0.12f, r * 0.45f, r * 0.62f, r * 0.45f);
        float ex = f * r * 0.12f;
        // eyes: glowing embers
        int em = p.emotion;
        for (int s = -1; s <= 1; s += 2) {
            float cx = s * r * 0.4f + ex, cy = -r * 0.12f;
            g.radial(cx, cy, r * 0.42f, alpha(l.eyeColor, 0.55f), alpha(l.eyeColor, 0f));
            g.oval(cx, cy, r * 0.42f, r * 0.42f);
            float eh = r * 0.13f * (1 - (p.eyesClosed ? 1 : p.blink));
            g.color(0xFF1A0E08);
            g.oval(cx, cy, r * 0.19f, Math.max(eh, 1.5f) + 2);
            g.color(l.eyeColor);
            g.oval(cx, cy, r * 0.15f, Math.max(eh, 1.5f));
            g.color(0xFFFFF59D);
            g.oval(cx + f * r * 0.03f, cy, r * 0.05f, Math.max(eh * 0.6f, 1f));
            // heavy brow
            g.color(0xFF1C130E);
            float inner = (em == Pose.PAIN || em == Pose.SCARED) ? -r * 0.12f : r * 0.08f;
            g.line(cx - s * r * 0.25f, cy - r * 0.22f + inner, cx + s * r * 0.25f, cy - r * 0.3f, 7);
        }
        // scar across the eye
        if (l.scar) {
            g.color(0xFFB0645A);
            g.line(-r * 0.75f + ex, -r * 0.5f, -r * 0.05f + ex, r * 0.25f, 4);
            for (int i = 0; i < 4; i++) {
                float t = 0.15f + i * 0.22f;
                float x = -r * 0.75f + ex + t * r * 0.7f, y = -r * 0.5f + t * r * 0.75f;
                g.line(x - 6, y + 5, x + 6, y - 5, 2);
            }
        }
        // nose
        g.color(0xFF1C130E);
        g.oval(ex, r * 0.25f, r * 0.2f, r * 0.12f);
        // jaw & mouth
        float open = Math.max(p.mouth, em == Pose.ANGRY || em == Pose.LAUGH || em == Pose.PAIN ? 0.45f : 0f);
        float my = r * 0.62f;
        float w = r * 0.5f;
        float h = r * (0.08f + open * 0.35f);
        g.begin();
        g.moveTo(ex - w, my);
        g.quadTo(ex, my - r * 0.08f, ex + w, my);
        g.quadTo(ex, my + h * 2, ex - w, my);
        g.close();
        g.color(0xFF3A0E0E); g.fillPath();
        // fangs (always visible)
        g.color(0xFFF5F0E0);
        for (int s = -1; s <= 1; s += 2) {
            float fx = ex + s * w * 0.55f;
            g.begin(); g.moveTo(fx - r * 0.08f, my - r * 0.04f); g.lineTo(fx, my + r * 0.3f); g.lineTo(fx + r * 0.08f, my - r * 0.04f); g.close();
            g.fillPath();
        }
        if (l.headwear == Look.HW_HORNS) drawHeadwear(g, l, p, r);
    }

    // ------------------------------------------------------------------ monkey

    static void drawMonkey(Gfx g, Look l, Pose p, float H) {
        float r = H * 0.2f;          // head radius
        float bodyH = H * 0.45f;
        float f = p.facing;
        float hop = 0;
        g.save();
        // shadow
        if (p.body != Pose.HANG && p.body != Pose.LIE) { g.color(0x33000000); g.oval(0, 2, r * 1.3f, r * 0.22f); }
        // tail
        float ts = (float) Math.sin(p.time * 3) * 8;
        g.color(shade(l.furColor, 0.85f));
        g.begin();
        g.moveTo(-f * r * 0.4f, -bodyH * 0.35f);
        g.cubicTo(-f * r * 1.8f, -bodyH * 0.2f, -f * r * 2.0f, -bodyH * 1.2f + ts, -f * r * 1.2f, -bodyH * 1.35f + ts);
        g.strokePath(r * 0.22f);
        // legs
        float stride = (float) Math.sin(p.walk) * p.walkAmt;
        g.color(l.furColor);
        g.line(-r * 0.3f, -bodyH * 0.3f, -r * 0.35f + stride * 10, -4, r * 0.3f);
        g.line(r * 0.3f, -bodyH * 0.3f, r * 0.35f - stride * 10, -4, r * 0.3f);
        g.color(l.skin);
        g.oval(-r * 0.35f + stride * 10 + f * 4, -3, r * 0.22f, r * 0.12f);
        g.oval(r * 0.35f - stride * 10 + f * 4, -3, r * 0.22f, r * 0.12f);
        // body
        g.color(l.furColor);
        g.oval(0, -bodyH * 0.62f, r * 0.72f, bodyH * 0.46f);
        g.color(lighten(l.skin, 0.1f));
        g.oval(0, -bodyH * 0.55f, r * 0.42f, bodyH * 0.3f);
        // vest (bandi)
        g.color(l.primary);
        g.begin();
        g.moveTo(-r * 0.72f, -bodyH * 0.95f);
        g.lineTo(-r * 0.15f, -bodyH * 0.95f);
        g.lineTo(-r * 0.2f, -bodyH * 0.3f);
        g.lineTo(-r * 0.7f, -bodyH * 0.35f);
        g.close(); g.fillPath();
        g.begin();
        g.moveTo(r * 0.72f, -bodyH * 0.95f);
        g.lineTo(r * 0.15f, -bodyH * 0.95f);
        g.lineTo(r * 0.2f, -bodyH * 0.3f);
        g.lineTo(r * 0.7f, -bodyH * 0.35f);
        g.close(); g.fillPath();
        g.color(0xFFE5B530);
        g.line(-r * 0.15f, -bodyH * 0.95f, -r * 0.2f, -bodyH * 0.3f, 2);
        g.line(r * 0.15f, -bodyH * 0.95f, r * 0.2f, -bodyH * 0.3f, 2);
        // arms
        Body b = new Body();
        b.H = H; b.headR = r; b.sw = r * 0.62f; b.shY = -bodyH * 0.92f; b.armLen = bodyH * 0.85f; b.armW = r * 0.14f; b.L = bodyH * 0.3f; b.T = bodyH;
        for (int s = -1; s <= 1; s += 2) {
            float ang = s < 0 ? p.armL : p.armR, el = s < 0 ? p.elbowL : p.elbowR;
            float[] h = hand(b, s, ang, el);
            g.color(l.furColor);
            g.line(h[0], h[1], h[2], h[3], r * 0.26f);
            g.line(h[2], h[3], h[4], h[5], r * 0.24f);
            g.color(l.skin);
            g.oval(h[4], h[5], r * 0.15f, r * 0.15f);
            int hold = s < 0 ? p.holdL : p.holdR;
            if (hold != Pose.I_NONE) drawHeld(g, l, p, b, s, h, hold);
        }
        // head
        float hy = -bodyH - r * 0.75f;
        g.save();
        g.translate(0, hy);
        if (p.headTilt != 0) g.rotate(p.headTilt * f);
        g.color(l.furColor);
        g.oval(-r * 0.95f, -r * 0.05f, r * 0.3f, r * 0.32f);
        g.oval(r * 0.95f, -r * 0.05f, r * 0.3f, r * 0.32f);
        g.color(l.skin);
        g.oval(-r * 0.95f, -r * 0.05f, r * 0.17f, r * 0.2f);
        g.oval(r * 0.95f, -r * 0.05f, r * 0.17f, r * 0.2f);
        g.color(shade(l.furColor, 0.7f));
        g.oval(0, 0, r * 0.92f, r * 0.88f);
        g.color(l.furColor);
        g.oval(0, 0, r * 0.88f, r * 0.84f);
        // face mask (heart shape)
        float ex = f * r * 0.12f;
        g.color(l.skin);
        g.oval(-r * 0.3f + ex, -r * 0.1f, r * 0.36f, r * 0.38f);
        g.oval(r * 0.3f + ex, -r * 0.1f, r * 0.36f, r * 0.38f);
        g.oval(ex, r * 0.35f, r * 0.48f, r * 0.38f);
        // eyes (big, expressive, human-like)
        for (int s = -1; s <= 1; s += 2) {
            float cx = s * r * 0.3f + ex, cy = -r * 0.1f;
            float eh = r * 0.17f * (1 - (p.eyesClosed ? 1 : p.blink));
            g.color(0xFFFFFFFF);
            g.oval(cx, cy, r * 0.16f, Math.max(eh, 1));
            g.color(0xFF4E2A12);
            g.oval(cx + f * r * 0.04f, cy, r * 0.11f, Math.max(Math.min(eh, r * 0.12f), 1));
            g.color(0xFF000000);
            g.oval(cx + f * r * 0.04f, cy, r * 0.05f, Math.max(Math.min(eh, r * 0.06f), 1));
            g.color(0xFFFFFFFF);
            g.oval(cx + f * r * 0.01f, cy - r * 0.05f, r * 0.03f, r * 0.03f);
        }
        // nose
        g.color(shade(l.skin, 0.6f));
        g.oval(ex - r * 0.06f, r * 0.25f, r * 0.03f, r * 0.025f);
        g.oval(ex + r * 0.06f, r * 0.25f, r * 0.03f, r * 0.025f);
        // mouth
        float open = Math.max(p.mouth, p.emotion == Pose.LAUGH || p.emotion == Pose.HAPPY ? 0.3f : 0);
        if (open > 0.08f) {
            g.color(0xFF5A1A1A);
            g.oval(ex, r * 0.48f, r * 0.2f, r * 0.08f + r * 0.14f * open);
            g.color(0xFFFFFFFF);
            g.rect(ex - r * 0.13f, r * 0.42f, r * 0.26f, r * 0.04f);
        } else {
            g.color(INK);
            g.begin(); g.moveTo(ex - r * 0.2f, r * 0.45f); g.quadTo(ex, r * 0.55f, ex + r * 0.2f, r * 0.45f); g.strokePath(2);
        }
        if (p.wearsTurban) drawTurbanShape(g, 0, -r * 0.7f, r * 0.95f, p.turbanColor, p.turbanBand, false);
        g.restore();
        // bandana at neck
        g.color(l.secondary);
        g.begin();
        g.moveTo(-r * 0.55f, -bodyH - r * 0.05f);
        g.lineTo(r * 0.55f, -bodyH - r * 0.05f);
        g.lineTo(f * r * 0.1f, -bodyH + r * 0.4f);
        g.close(); g.fillPath();
        g.restore();
    }

    // ------------------------------------------------------------------ animals (side view)

    static void drawAnimal(Gfx g, Look l, Pose p, float H) {
        int sp = l.species, fur = l.furColor, dark = shade(fur, 0.72f), belly = l.skin;
        g.save();
        g.scale(p.facing < 0 ? -1 : 1, 1);
        g.color(0x33000000);
        g.oval(0, 2, H * 0.62f, H * 0.07f);
        if (sp == Look.SP_TORTOISE) { tortoise(g, l, p, H); g.restore(); return; }
        float stride = (float) Math.sin(p.walk) * p.walkAmt;
        float bodyY = -H * 0.6f, bodyRx = H * 0.52f, bodyRy = H * 0.27f;
        if (sp == Look.SP_ELEPHANT) { bodyRx = H * 0.55f; bodyRy = H * 0.33f; }
        if (sp == Look.SP_RABBIT || sp == Look.SP_MOUSE) { bodyRx = H * 0.45f; bodyRy = H * 0.3f; bodyY = -H * 0.45f; }
        float legTop = bodyY + bodyRy * 0.6f;
        float legW = sp == Look.SP_ELEPHANT ? H * 0.16f : (sp == Look.SP_HORSE || sp == Look.SP_DEER ? H * 0.06f : H * 0.09f);
        // far legs
        g.color(dark);
        g.line(-bodyRx * 0.55f, legTop, -bodyRx * 0.55f - stride * H * 0.12f, -legW * 0.5f, legW);
        g.line(bodyRx * 0.55f, legTop, bodyRx * 0.55f + stride * H * 0.12f, -legW * 0.5f, legW);
        // tail
        g.color(fur);
        // v36: the wind in this mirrored frame (+ = towards the head); it swings the tail, ruffles the fur and the mane
        float lw = p.wind * (p.facing < 0 ? -1 : 1);
        float tw = (float) Math.sin(p.time * 4) * H * 0.04f + lw * H * 0.06f * (0.8f + 0.4f * (float) Math.sin(p.time * 6.5f));
        switch (sp) {
            case Look.SP_FOX: case Look.SP_WOLF:
                g.begin(); g.moveTo(-bodyRx * 0.9f, bodyY - bodyRy * 0.1f);
                g.quadTo(-bodyRx * 1.6f, bodyY - bodyRy * 0.4f + tw, -bodyRx * 1.75f, bodyY + bodyRy * 0.5f);
                g.quadTo(-bodyRx * 1.3f, bodyY + bodyRy * 0.6f, -bodyRx * 0.85f, bodyY + bodyRy * 0.3f); g.close(); g.fillPath();
                g.color(sp == Look.SP_FOX ? 0xFFFFFFFF : 0xFFBDBDBD);
                g.oval(-bodyRx * 1.68f, bodyY + bodyRy * 0.45f, H * 0.07f, H * 0.06f);
                break;
            case Look.SP_RABBIT: g.color(0xFFFFFFFF); g.oval(-bodyRx * 0.95f, bodyY - bodyRy * 0.2f, H * 0.09f, H * 0.09f); break;
            default:
                g.line(-bodyRx * 0.9f, bodyY - bodyRy * 0.2f, -bodyRx * 1.35f, bodyY + bodyRy * 0.6f + tw, H * 0.035f);
                if (sp == Look.SP_LION || sp == Look.SP_COW) { g.color(dark); g.oval(-bodyRx * 1.37f, bodyY + bodyRy * 0.7f + tw, H * 0.05f, H * 0.07f); }
                if (sp == Look.SP_HORSE) { g.color(0xFF3E2716); g.begin(); g.moveTo(-bodyRx * 0.9f, bodyY - bodyRy * 0.3f); g.quadTo(-bodyRx * 1.5f, bodyY, -bodyRx * 1.3f, bodyY + bodyRy * 1.2f + tw); g.strokePath(H * 0.06f); }
        }
        // body
        g.color(shade(fur, 0.6f));
        g.oval(0, bodyY, bodyRx + 2, bodyRy + 2);
        g.radial(H * 0.1f, bodyY - bodyRy * 0.4f, bodyRx * 1.3f, lighten(fur, 0.12f), shade(fur, 0.88f));
        g.oval(0, bodyY, bodyRx, bodyRy);
        g.color(belly);
        g.oval(H * 0.05f, bodyY + bodyRy * 0.45f, bodyRx * 0.7f, bodyRy * 0.42f);
        if (!l.robot && sp != Look.SP_ELEPHANT && Math.abs(lw) > 0.04f) {
            // v36: fur along the back lifts and ripples in the wind, a wave running along it
            float fa = Math.min(1, (Math.abs(lw) - 0.04f) / 0.3f);
            g.color(alpha(shade(fur, 0.82f), 0.85f * fa));
            for (int i = 0; i < 18; i++) {
                double an = Math.PI * (1.1 + 0.8 * i / 17.0);
                float bx0 = (float) Math.cos(an) * bodyRx * 0.97f, by0 = bodyY + (float) Math.sin(an) * bodyRy * 0.97f;
                float rip = 0.6f + 0.4f * (float) Math.sin(p.time * 8.5f - i * 0.7f);
                float len = H * 0.035f * (0.6f + Math.abs(lw)) * rip;
                g.line(bx0, by0, bx0 + lw * len * 1.4f, by0 - len * 0.7f, H * 0.012f);
            }
        }
        if (l.robot) {
            // a robot: panel seams, rivets and a small light on the back
            g.color(alpha(0xFF000000, 0.35f));
            g.line(-bodyRx * 0.3f, bodyY - bodyRy * 0.9f, -bodyRx * 0.35f, bodyY + bodyRy * 0.8f, 2);
            g.line(bodyRx * 0.3f, bodyY - bodyRy * 0.9f, bodyRx * 0.25f, bodyY + bodyRy * 0.8f, 2);
            g.color(alpha(0xFFFFFFFF, 0.5f));
            for (int i = -2; i <= 2; i++) g.oval(i * bodyRx * 0.3f, bodyY - bodyRy * 0.55f, 2.5f, 2.5f);
            g.color(alpha(0xFF00E5FF, 0.5f + 0.5f * (float) Math.sin(p.time * 5)));
            g.oval(-bodyRx * 0.1f, bodyY - bodyRy * 0.98f, H * 0.035f, H * 0.035f);
        }
        if (l.outfit == Look.O_JACKET && l.primary != 0) {
            // a little jacket on the back
            g.color(l.primary);
            g.begin(); g.moveTo(-bodyRx * 0.55f, bodyY - bodyRy * 0.95f); g.quadTo(0, bodyY - bodyRy * 1.15f, bodyRx * 0.6f, bodyY - bodyRy * 0.9f);
            g.lineTo(bodyRx * 0.5f, bodyY + bodyRy * 0.1f); g.quadTo(0, bodyY + bodyRy * 0.2f, -bodyRx * 0.5f, bodyY + bodyRy * 0.1f); g.close(); g.fillPath();
            g.color(shade(l.primary, 0.7f));
            g.line(-bodyRx * 0.5f, bodyY + bodyRy * 0.08f, bodyRx * 0.5f, bodyY + bodyRy * 0.08f, 2.5f);
        }
        if (sp == Look.SP_TIGER) {
            g.color(0xFF2A1A12);
            for (int i = -3; i <= 3; i++) g.line(i * bodyRx * 0.22f, bodyY - bodyRy * 0.95f, i * bodyRx * 0.22f - H * 0.03f, bodyY - bodyRy * 0.2f, H * 0.035f);
        }
        if (sp == Look.SP_COW) {
            g.color(0xFF3E2C22);
            g.oval(-bodyRx * 0.3f, bodyY - bodyRy * 0.3f, bodyRx * 0.25f, bodyRy * 0.35f);
            g.oval(bodyRx * 0.35f, bodyY + bodyRy * 0.05f, bodyRx * 0.18f, bodyRy * 0.28f);
        }
        if (sp == Look.SP_DEER) {
            g.color(0xFFF5EBDD);
            for (int i = 0; i < 6; i++) g.oval(-bodyRx * 0.5f + i * bodyRx * 0.18f, bodyY - bodyRy * 0.35f + (i % 2) * bodyRy * 0.25f, H * 0.018f, H * 0.018f);
        }
        if (l.primary != 0 && sp != Look.SP_COW) { // a little scarf / vest colour from the description
            g.color(l.primary);
            g.oval(bodyRx * 0.62f, bodyY - bodyRy * 0.55f, H * 0.08f, H * 0.12f);
        }
        // near legs
        g.color(fur);
        g.line(-bodyRx * 0.45f, legTop, -bodyRx * 0.45f + stride * H * 0.12f, -legW * 0.5f, legW);
        g.line(bodyRx * 0.62f, legTop, bodyRx * 0.62f - stride * H * 0.12f, -legW * 0.5f, legW);
        g.color(sp == Look.SP_HORSE || sp == Look.SP_DEER || sp == Look.SP_GOAT || sp == Look.SP_COW ? 0xFF2A1A12 : dark);
        g.oval(-bodyRx * 0.45f + stride * H * 0.12f + legW * 0.2f, -legW * 0.3f, legW * 0.75f, legW * 0.4f);
        g.oval(bodyRx * 0.62f - stride * H * 0.12f + legW * 0.2f, -legW * 0.3f, legW * 0.75f, legW * 0.4f);
        // head
        float hx = bodyRx * 0.95f, hy = bodyY - bodyRy * 1.05f, hr = H * (sp == Look.SP_ELEPHANT ? 0.25f : sp == Look.SP_MOUSE ? 0.26f : 0.2f);
        if (sp == Look.SP_RABBIT) { hx = bodyRx * 0.8f; hy = bodyY - bodyRy * 1.1f; hr = H * 0.22f; }
        hy += p.bob * 0.3f + (float) Math.sin(p.time * 2) * 1.5f;
        g.save();
        g.translate(hx, hy);
        if (p.headTilt != 0) g.rotate(p.headTilt);
        if (sp == Look.SP_LION) {
            g.color(0xFF8D5524);
            for (int i = 0; i < 12; i++) {
                double a = i * Math.PI / 6;
                // v36: the mane blows back from the wind, the tufts at the back most, each at its own beat
                float blow = lw * hr * 0.16f * (1 - 0.6f * (float) Math.cos(a)) * (0.7f + 0.3f * (float) Math.sin(p.time * 7 + i * 1.3f));
                g.oval((float) Math.cos(a) * hr * 1.05f + blow, (float) Math.sin(a) * hr * 1.05f - Math.abs(blow) * 0.3f, hr * 0.5f, hr * 0.5f);
            }
            g.oval(0, 0, hr * 1.35f, hr * 1.35f);
        }
        // ears behind
        g.color(dark);
        switch (sp) {
            case Look.SP_FOX: case Look.SP_WOLF: case Look.SP_CAT: case Look.SP_TIGER:
                g.begin(); g.moveTo(-hr * 0.55f, -hr * 0.5f); g.lineTo(-hr * 0.35f, -hr * 1.45f); g.lineTo(hr * 0.05f, -hr * 0.75f); g.close(); g.fillPath();
                g.begin(); g.moveTo(hr * 0.05f, -hr * 0.7f); g.lineTo(hr * 0.35f, -hr * 1.5f); g.lineTo(hr * 0.6f, -hr * 0.5f); g.close(); g.fillPath();
                break;
            case Look.SP_RABBIT:
                g.color(fur);
                g.oval(-hr * 0.25f, -hr * 1.6f, hr * 0.22f, hr * 0.8f);
                g.oval(hr * 0.2f, -hr * 1.7f, hr * 0.22f, hr * 0.85f);
                g.color(0xFFF8BBD0);
                g.oval(hr * 0.2f, -hr * 1.65f, hr * 0.1f, hr * 0.6f);
                break;
            case Look.SP_BEAR: case Look.SP_MOUSE: case Look.SP_LION:
                g.oval(-hr * 0.55f, -hr * 0.8f, hr * (sp == Look.SP_MOUSE ? 0.5f : 0.3f), hr * (sp == Look.SP_MOUSE ? 0.5f : 0.3f));
                g.oval(hr * 0.35f, -hr * 0.85f, hr * (sp == Look.SP_MOUSE ? 0.5f : 0.3f), hr * (sp == Look.SP_MOUSE ? 0.5f : 0.3f));
                break;
            case Look.SP_ELEPHANT:
                g.color(shade(fur, 0.85f));
                g.oval(-hr * 0.7f, hr * 0.05f, hr * 0.75f, hr * 0.95f);
                break;
            case Look.SP_DOG: case Look.SP_GOAT: case Look.SP_COW: case Look.SP_DEER: case Look.SP_HORSE:
                g.oval(-hr * 0.45f, -hr * 0.3f, hr * 0.22f, hr * 0.55f);
                break;
            default:
        }
        if (sp == Look.SP_DEER) {
            g.color(0xFF8D6E4A);
            g.line(-hr * 0.1f, -hr * 0.8f, -hr * 0.3f, -hr * 1.8f, hr * 0.1f);
            g.line(-hr * 0.25f, -hr * 1.4f, -hr * 0.65f, -hr * 1.7f, hr * 0.08f);
            g.line(hr * 0.2f, -hr * 0.8f, hr * 0.3f, -hr * 1.8f, hr * 0.1f);
            g.line(hr * 0.27f, -hr * 1.4f, hr * 0.65f, -hr * 1.65f, hr * 0.08f);
        }
        if (sp == Look.SP_GOAT || sp == Look.SP_COW) {
            g.color(0xFFD7CCC8);
            g.begin(); g.moveTo(-hr * 0.2f, -hr * 0.8f); g.quadTo(-hr * 0.6f, -hr * 1.5f, -hr * 0.9f, -hr * 1.2f); g.strokePath(hr * 0.15f);
            g.begin(); g.moveTo(hr * 0.15f, -hr * 0.85f); g.quadTo(hr * 0.2f, -hr * 1.55f, -hr * 0.2f, -hr * 1.45f); g.strokePath(hr * 0.15f);
        }
        // head
        g.color(shade(fur, 0.6f));
        g.oval(0, 0, hr + 2, hr * 0.95f + 2);
        g.radial(-hr * 0.2f, -hr * 0.3f, hr * 1.4f, lighten(fur, 0.12f), shade(fur, 0.9f));
        g.oval(0, 0, hr, hr * 0.95f);
        if (sp == Look.SP_HORSE) { g.color(0xFF3E2716); g.oval(-hr * 0.5f, -hr * 0.6f, hr * 0.3f, hr * 0.6f); }
        // snout & mouth (opens while speaking)
        float open = Math.max(0, Math.min(1, p.mouth)) + (p.emotion == Pose.LAUGH ? 0.35f : 0);
        float snx = hr * 0.75f, sny = hr * 0.25f;
        if (sp == Look.SP_ELEPHANT) {
            g.color(fur);
            g.begin(); g.moveTo(hr * 0.55f, -hr * 0.1f);
            g.quadTo(hr * 1.3f, hr * 0.3f, hr * 1.1f + (float) Math.sin(p.time * 2) * hr * 0.15f, hr * 1.4f);
            g.strokePath(hr * 0.35f);
            g.color(0xFFF5F0E0);
            g.begin(); g.moveTo(hr * 0.6f, hr * 0.4f); g.quadTo(hr * 0.95f, hr * 0.6f, hr * 0.8f, hr * 0.85f); g.strokePath(hr * 0.12f);
            g.color(0xFF5A1A1A);
            g.oval(hr * 0.45f, hr * 0.55f, hr * 0.2f, hr * (0.06f + 0.18f * open));
        } else {
            float snLen = (sp == Look.SP_FOX || sp == Look.SP_WOLF || sp == Look.SP_DOG || sp == Look.SP_HORSE || sp == Look.SP_DEER) ? hr * 0.75f : hr * 0.45f;
            g.color(sp == Look.SP_FOX ? 0xFFF5EBDD : lighten(fur, 0.2f));
            g.oval(snx, sny, snLen, hr * 0.38f);
            if (open > 0.08f) {
                g.color(0xFF5A1A1A);
                g.begin(); g.moveTo(snx - snLen * 0.3f, sny + hr * 0.12f);
                g.lineTo(snx + snLen * 0.95f, sny + hr * 0.05f);
                g.lineTo(snx + snLen * 0.8f, sny + hr * (0.15f + 0.4f * open));
                g.close(); g.fillPath();
                g.color(0xFFFFFFFF);
                g.line(snx + snLen * 0.2f, sny + hr * 0.12f, snx + snLen * 0.7f, sny + hr * 0.08f, hr * 0.05f);
            } else {
                g.color(INK);
                g.line(snx + snLen * 0.1f, sny + hr * 0.2f, snx + snLen * 0.8f, sny + hr * 0.12f, 2);
            }
            g.color(0xFF2A1A12);
            g.oval(snx + snLen * 0.92f, sny - hr * 0.08f, hr * 0.13f, hr * 0.1f);
            if (sp == Look.SP_CAT || sp == Look.SP_TIGER || sp == Look.SP_MOUSE || sp == Look.SP_RABBIT || sp == Look.SP_LION) {
                g.color(0x99FFFFFF);
                for (int i = -1; i <= 1; i++) g.line(snx + snLen * 0.5f, sny + i * hr * 0.06f, snx + snLen * 1.5f, sny + i * hr * 0.14f, 1.2f);
            }
            if (sp == Look.SP_GOAT) { g.color(0xFFEDE7DC); g.begin(); g.moveTo(snx, sny + hr * 0.3f); g.lineTo(snx + hr * 0.1f, sny + hr * 0.8f); g.lineTo(snx + hr * 0.3f, sny + hr * 0.3f); g.close(); g.fillPath(); }
        }
        // a bandana around the neck
        if (l.outfit == Look.O_JACKET && l.secondary != 0 && l.secondary != l.primary) {
            g.color(l.secondary);
            g.begin(); g.moveTo(-hr * 0.8f, hr * 0.55f); g.lineTo(hr * 0.5f, hr * 0.6f); g.lineTo(-hr * 0.2f, hr * 1.15f); g.close(); g.fillPath();
        }
        // eye
        float ex = hr * 0.25f, ey = -hr * 0.18f;
        float er = hr * 0.17f * (1 - (p.eyesClosed ? 1 : p.blink));
        if (l.robot) {
            // LED screens for eyes: a bright pupil bar; a small frown when sad, an antenna on the head
            g.color(0xFF0D2137);
            g.roundRect(ex - hr * 0.24f, ey - hr * 0.16f, hr * 0.48f, hr * 0.32f, hr * 0.06f);
            int led = p.emotion == Pose.SCARED || p.emotion == Pose.ANGRY ? 0xFFFF5252 : 0xFF40C4FF;
            g.color(alpha(led, 0.35f)); g.roundRect(ex - hr * 0.2f, ey - hr * 0.12f, hr * 0.4f, hr * 0.24f, hr * 0.05f);
            g.color(led);
            if (p.emotion == Pose.SAD) { g.begin(); g.moveTo(ex - hr * 0.14f, ey + hr * 0.06f); g.quadTo(ex, ey - hr * 0.08f, ex + hr * 0.14f, ey + hr * 0.06f); g.strokePath(hr * 0.05f); }
            else if (p.emotion == Pose.HAPPY || p.emotion == Pose.LAUGH) { g.begin(); g.moveTo(ex - hr * 0.14f, ey - hr * 0.05f); g.quadTo(ex, ey + hr * 0.1f, ex + hr * 0.14f, ey - hr * 0.05f); g.strokePath(hr * 0.05f); }
            else g.oval(ex + hr * 0.02f, ey, hr * 0.09f * (1 - 0.8f * (p.eyesClosed ? 1 : p.blink)) + 1, hr * 0.09f * (1 - 0.8f * (p.eyesClosed ? 1 : p.blink)) + 1);
            g.color(0xFF78909C); g.line(-hr * 0.3f, -hr * 0.85f, -hr * 0.45f, -hr * 1.35f, hr * 0.06f);
            g.color(alpha(0xFFFF1744, 0.5f + 0.5f * (float) Math.sin(p.time * 6))); g.oval(-hr * 0.45f, -hr * 1.38f, hr * 0.09f, hr * 0.09f);
        } else {
            g.color(0xFFFFFFFF);
            g.oval(ex, ey, hr * 0.17f, Math.max(1, er));
            g.color(0xFF1A120C);
            g.oval(ex + hr * 0.05f, ey, hr * 0.1f, Math.max(1, Math.min(er, hr * 0.12f)));
            g.color(0xFFFFFFFF);
            g.oval(ex + hr * 0.02f, ey - hr * 0.04f, hr * 0.03f, hr * 0.03f);
        }
        // brow for emotion
        g.color(shade(fur, 0.5f));
        float bi = p.emotion == Pose.ANGRY || p.emotion == Pose.EVIL || p.emotion == Pose.DETERMINED ? hr * 0.08f : p.emotion == Pose.SCARED || p.emotion == Pose.SAD ? -hr * 0.08f : 0;
        g.line(ex - hr * 0.18f, ey - hr * 0.22f - bi * 0.3f, ex + hr * 0.2f, ey - hr * 0.24f + bi, hr * 0.06f);
        if (p.tears) { g.color(0xCC64B5F6); g.oval(ex, ey + hr * 0.3f + (p.time * 40) % (hr * 0.5f), hr * 0.05f, hr * 0.08f); }
        g.restore();
        g.restore();
    }

    static void tortoise(Gfx g, Look l, Pose p, float H) {
        float stride = (float) Math.sin(p.walk) * p.walkAmt;
        g.color(shade(l.skin, 0.9f));
        for (int i = -1; i <= 1; i += 2) g.oval(i * H * 0.45f + stride * 6 * i, -H * 0.12f, H * 0.14f, H * 0.12f);
        g.color(0xFF9CCC65);
        g.oval(H * 0.85f, -H * 0.45f, H * 0.22f, H * 0.18f);
        g.color(0xFF1A120C);
        g.oval(H * 0.92f, -H * 0.5f, H * 0.04f, H * 0.04f * (1 - p.blink));
        if (p.mouth > 0.08f) { g.color(0xFF5A1A1A); g.oval(H * 1.0f, -H * 0.38f, H * 0.06f, H * 0.03f + H * 0.05f * p.mouth); }
        g.color(shade(l.furColor, 0.65f));
        g.begin(); g.moveTo(-H * 0.75f, -H * 0.18f); g.cubicTo(-H * 0.7f, -H * 1.05f, H * 0.7f, -H * 1.05f, H * 0.75f, -H * 0.18f); g.close(); g.fillPath();
        g.color(l.furColor);
        g.begin(); g.moveTo(-H * 0.68f, -H * 0.22f); g.cubicTo(-H * 0.62f, -H * 0.95f, H * 0.62f, -H * 0.95f, H * 0.68f, -H * 0.22f); g.close(); g.fillPath();
        g.color(shade(l.furColor, 0.75f));
        g.strokeOval(0, -H * 0.5f, H * 0.18f, H * 0.14f, 3);
        g.strokeOval(-H * 0.38f, -H * 0.38f, H * 0.14f, H * 0.11f, 3);
        g.strokeOval(H * 0.38f, -H * 0.38f, H * 0.14f, H * 0.11f, 3);
    }

    // ------------------------------------------------------------------ birds (side view)

    static void drawBird(Gfx g, Look l, Pose p, float H) {
        int sp = l.species, c = l.furColor, belly = l.skin;
        g.save();
        g.scale(p.facing < 0 ? -1 : 1, 1);
        if (p.body != Pose.HANG) { g.color(0x33000000); g.oval(0, 2, H * 0.35f, H * 0.05f); }
        float by = -H * 0.48f, brx = H * 0.36f, bry = H * 0.28f;
        if (sp == Look.SP_PEACOCK) {
            float fan = 0.85f + 0.15f * (float) Math.sin(p.time * 1.5f);
            for (int i = 0; i < 13; i++) {
                double a = Math.PI * (0.12 + 0.76 * i / 12.0);
                float fx = -brx * 0.4f + (float) Math.cos(a) * H * 0.95f * fan, fy = by + (float) -Math.sin(a) * H * 0.85f * fan;
                g.color(0xFF2E7D32);
                g.line(-brx * 0.4f, by, fx, fy, H * 0.03f);
                g.color(0xFF1B5E20); g.oval(fx, fy, H * 0.09f, H * 0.11f);
                g.color(0xFF00897B); g.oval(fx, fy, H * 0.06f, H * 0.075f);
                g.color(0xFF0D47A1); g.oval(fx, fy, H * 0.03f, H * 0.04f);
            }
        }
        // legs
        g.color(sp == Look.SP_CROW ? 0xFF3A3A3A : 0xFFF9A825);
        float stride = (float) Math.sin(p.walk) * p.walkAmt * H * 0.06f;
        g.line(-brx * 0.15f, by + bry * 0.7f, -brx * 0.2f + stride, -2, H * 0.025f);
        g.line(brx * 0.15f, by + bry * 0.7f, brx * 0.15f - stride, -2, H * 0.025f);
        g.line(-brx * 0.2f + stride, -2, -brx * 0.2f + stride + H * 0.08f, -2, H * 0.02f);
        g.line(brx * 0.15f - stride, -2, brx * 0.15f - stride + H * 0.08f, -2, H * 0.02f);
        // tail
        g.color(shade(c, 0.8f));
        g.begin(); g.moveTo(-brx * 0.7f, by - bry * 0.1f); g.lineTo(-brx * 1.6f, by - bry * 0.6f); g.lineTo(-brx * 1.5f, by + bry * 0.3f); g.close(); g.fillPath();
        if (sp == Look.SP_HEN) { g.color(0xFF5D4037); g.begin(); g.moveTo(-brx * 0.7f, by - bry * 0.4f); g.quadTo(-brx * 1.4f, by - bry * 1.8f, -brx * 1.2f, by - bry * 0.2f); g.strokePath(H * 0.05f); }
        // body
        g.color(shade(c, 0.6f));
        g.oval(0, by, brx + 2, bry + 2);
        g.radial(brx * 0.2f, by - bry * 0.4f, brx * 1.4f, lighten(c, 0.15f), shade(c, 0.9f));
        g.oval(0, by, brx, bry);
        g.color(belly);
        g.oval(brx * 0.25f, by + bry * 0.25f, brx * 0.55f, bry * 0.6f);
        // wing (flaps when talking or moving)
        float flap = (p.mouth > 0.1f || p.walkAmt > 0) ? (float) Math.sin(p.time * 18) * 18 : (float) Math.sin(p.time * 2) * 3;
        g.save();
        g.translate(-brx * 0.05f, by - bry * 0.2f);
        g.rotate(-10 - flap);
        g.color(shade(c, 0.82f));
        g.begin(); g.moveTo(brx * 0.4f, 0); g.quadTo(-brx * 0.2f, -bry * 0.6f, -brx * 0.95f, bry * 0.35f); g.quadTo(-brx * 0.1f, bry * 0.55f, brx * 0.4f, 0); g.close(); g.fillPath();
        g.restore();
        // head
        float hx = brx * 0.75f, hy = by - bry * 1.0f, hr = H * (sp == Look.SP_OWL ? 0.22f : 0.17f);
        g.color(sp == Look.SP_PEACOCK ? 0xFF1565C0 : c);
        g.oval(hx, hy, hr, hr);
        if (sp == Look.SP_PEACOCK) {
            g.color(0xFF1565C0);
            for (int i = -1; i <= 1; i++) { g.line(hx, hy - hr, hx + i * hr * 0.3f, hy - hr * 1.7f, 2); g.oval(hx + i * hr * 0.3f, hy - hr * 1.75f, hr * 0.1f, hr * 0.1f); }
        }
        if (sp == Look.SP_HEN) { g.color(0xFFE53935); for (int i = 0; i < 3; i++) g.oval(hx - hr * 0.3f + i * hr * 0.3f, hy - hr * 0.95f, hr * 0.2f, hr * 0.25f); g.oval(hx + hr * 0.75f, hy + hr * 0.55f, hr * 0.12f, hr * 0.2f); }
        // beak opens with the voice
        float open = Math.max(0, Math.min(1, p.mouth)) + (p.emotion == Pose.LAUGH ? 0.3f : 0);
        int beak = sp == Look.SP_PARROT ? 0xFFD32F2F : sp == Look.SP_CROW ? 0xFF4A4A4A : 0xFFF9A825;
        float bx = hx + hr * 0.8f, byk = hy + hr * 0.1f, bl = hr * (sp == Look.SP_EAGLE || sp == Look.SP_PARROT ? 0.8f : sp == Look.SP_DUCK ? 0.9f : 0.75f);
        g.color(beak);
        g.begin(); g.moveTo(bx, byk - hr * 0.22f); g.lineTo(bx + bl, byk - open * hr * 0.15f); g.lineTo(bx, byk + hr * 0.02f); g.close(); g.fillPath();
        g.color(shade(beak, 0.8f));
        g.begin(); g.moveTo(bx, byk + hr * 0.05f); g.lineTo(bx + bl * 0.85f, byk + hr * 0.05f + open * hr * 0.45f); g.lineTo(bx, byk + hr * 0.22f); g.close(); g.fillPath();
        // eye
        float ex = hx + hr * (sp == Look.SP_OWL ? 0.25f : 0.35f), ey = hy - hr * 0.2f, er = hr * (sp == Look.SP_OWL ? 0.32f : 0.2f);
        g.color(sp == Look.SP_OWL ? 0xFFFFC107 : 0xFFFFFFFF);
        g.oval(ex, ey, er, er * (1 - (p.eyesClosed ? 1 : p.blink)) + 0.5f);
        g.color(0xFF120A06);
        g.oval(ex + er * 0.2f, ey, er * 0.55f, er * 0.55f * (1 - (p.eyesClosed ? 1 : p.blink)) + 0.5f);
        g.color(0xFFFFFFFF);
        g.oval(ex + er * 0.05f, ey - er * 0.2f, er * 0.18f, er * 0.18f);
        if (p.emotion == Pose.ANGRY || p.emotion == Pose.EVIL) { g.color(shade(c, 0.4f)); g.line(ex - er, ey - er * 1.2f, ex + er, ey - er * 0.6f, hr * 0.08f); }
        g.restore();
    }
}
