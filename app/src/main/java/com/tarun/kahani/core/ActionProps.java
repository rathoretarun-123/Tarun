package com.tarun.kahani.core;

/**
 * v44: the things of the royal stories, of grown-ups' evenings, of the snow and of the road, drawn: the bow (its string
 * drawn back, an arrow on it), a shield, a spear, glasses of wine, beer and whisky, a snowball; an archery target and a
 * straw dummy; arrows in flight and stuck in their mark; a snowman being built; the animals of a place (a dog, a cow,
 * a goat, a hen, a deer…) grazing, walking across or bolting; and the close inserts a film cuts to — a driver's hands
 * on the steering wheel, the feet on the pedals, a cyclist's feet going round, the arrow in the bull's eye.
 */
public final class ActionProps {
    private ActionProps() {}

    static int alpha(int c, float a) { return Puppet.alpha(c, a); }

    // ------------------------------------------------------------------ held things

    /**
     * The bow held upright in the hand at (x, y), its belly towards f (+1 right): L = half its length. draw: how far
     * the string is pulled back (0 slack .. 1 to the cheek); arrow: one on the string.
     */
    public static void bow(Gfx g, float x, float y, float L, float f, float draw, boolean arrow) {
        float bulge = L * (0.34f - 0.1f * draw);
        float tx = x - f * L * 0.08f * draw;              // the tips come back a little as it bends
        // the limbs: a dark wooden curve, a lighter grip
        g.color(0xFF5D3A1A);
        g.begin(); g.moveTo(tx, y - L); g.quadTo(x + f * bulge * 2, y, tx, y + L); g.strokePath(Math.max(1.6f, L * 0.06f));
        g.color(0xFFD7A86E); g.line(x + f * bulge * 0.95f, y - L * 0.09f, x + f * bulge * 0.95f, y + L * 0.09f, Math.max(2f, L * 0.09f));
        // the string, straight when slack, a V to the nocking point when drawn
        float nx = tx - f * L * (0.05f + 0.75f * draw);
        g.color(0xFFEEEEEE);
        if (draw < 0.02f && !arrow) g.line(tx, y - L, tx, y + L, Math.max(0.8f, L * 0.012f));
        else { g.line(tx, y - L, nx, y, Math.max(0.8f, L * 0.012f)); g.line(nx, y, tx, y + L, Math.max(0.8f, L * 0.012f)); }
        if (arrow) arrowShape(g, nx, y, x + f * (bulge + L * 0.32f), y, L * 0.03f);
    }

    /** An arrow from its nock (x0, y0) to its head (x1, y1): the shaft, the steel head, the feathers. w ≈ its thickness. */
    public static void arrowShape(Gfx g, float x0, float y0, float x1, float y1, float w) {
        float dx = x1 - x0, dy = y1 - y0, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-3f) return;
        float ux = dx / len, uy = dy / len, px = -uy, py = ux;
        w = Math.max(1.0f, w);
        g.color(0xFF8D6E63); g.line(x0, y0, x1, y1, w);
        // the head
        float hl = Math.max(5, len * 0.1f), hw = Math.max(2.5f, w * 2.2f);
        g.color(0xFF90A4AE);
        g.begin(); g.moveTo(x1 + ux * hl * 0.6f, y1 + uy * hl * 0.6f); g.lineTo(x1 - ux * hl * 0.4f + px * hw, y1 - uy * hl * 0.4f + py * hw);
        g.lineTo(x1 - ux * hl * 0.4f - px * hw, y1 - uy * hl * 0.4f - py * hw); g.close(); g.fillPath();
        // the feathers at the nock
        float fl = Math.max(6, len * 0.14f), fw = Math.max(2.5f, w * 2.6f);
        g.color(0xFFC62828);
        g.begin(); g.moveTo(x0 + ux * fl, y0 + uy * fl); g.lineTo(x0 + px * fw, y0 + py * fw); g.lineTo(x0 + ux * fl * 0.2f, y0 + uy * fl * 0.2f); g.close(); g.fillPath();
        g.begin(); g.moveTo(x0 + ux * fl, y0 + uy * fl); g.lineTo(x0 - px * fw, y0 - py * fw); g.lineTo(x0 + ux * fl * 0.2f, y0 + uy * fl * 0.2f); g.close(); g.fillPath();
    }

    /** A round shield held on the arm at (x, y), radius r: rim, painted face, a brass boss and studs. */
    public static void shield(Gfx g, float x, float y, float r) {
        g.color(0xFF4E342E); g.oval(x, y, r * 1.06f, r * 1.06f);
        g.radial(x - r * 0.3f, y - r * 0.3f, r * 1.3f, 0xFFB71C1C, 0xFF6D0F0F); g.oval(x, y, r, r);
        g.color(0xFFFFC107); g.strokeOval(x, y, r * 0.72f, r * 0.72f, Math.max(1, r * 0.06f));
        g.radial(x - r * 0.08f, y - r * 0.08f, r * 0.3f, 0xFFFFF8E1, 0xFFC79A2C); g.oval(x, y, r * 0.24f, r * 0.24f);
        g.color(0xFFE0C068);
        for (int i = 0; i < 8; i++) {
            double a = i * Math.PI / 4;
            g.oval(x + (float) Math.cos(a) * r * 0.86f, y + (float) Math.sin(a) * r * 0.86f, r * 0.05f, r * 0.05f);
        }
    }

    /** A spear held at (x, y): the shaft upright and leaning forward (f), the steel head up top, a red tassel. len = its length. */
    public static void spear(Gfx g, float x, float y, float len, float f) {
        float tx = x + f * len * 0.12f, ty = y - len * 0.7f, bx = x - f * len * 0.05f, by = y + len * 0.3f;
        g.color(0xFF6D4C41); g.line(bx, by, tx, ty, Math.max(1.5f, len * 0.02f));
        float dx = tx - bx, dy = ty - by, l = (float) Math.sqrt(dx * dx + dy * dy), ux = dx / l, uy = dy / l, px = -uy, py = ux, hl = len * 0.1f;
        g.color(0xFFB0BEC5);
        g.begin(); g.moveTo(tx + ux * hl, ty + uy * hl); g.lineTo(tx + px * hl * 0.25f, ty + py * hl * 0.25f); g.lineTo(tx - px * hl * 0.25f, ty - py * hl * 0.25f); g.close(); g.fillPath();
        g.color(0xFFD32F2F); g.oval(tx - ux * len * 0.02f, ty - uy * len * 0.02f, len * 0.025f, len * 0.025f);
    }

    /** v44: a glass of wine (stemmed, red), a mug of beer (amber, foam), a whisky tumbler (amber, ice) at the hand (x, y), k = size. */
    public static void drink(Gfx g, int item, float x, float y, float k) {
        switch (item) {
            case Pose.I_WINE: {
                g.color(0x66E3F2FA);
                g.begin(); g.moveTo(x - 6 * k, y - 22 * k); g.quadTo(x - 6 * k, y - 11 * k, x, y - 10 * k); g.quadTo(x + 6 * k, y - 11 * k, x + 6 * k, y - 22 * k); g.close(); g.fillPath();
                g.color(0xCC8E1B2E);
                g.begin(); g.moveTo(x - 5.4f * k, y - 17 * k); g.quadTo(x - 5 * k, y - 11.5f * k, x, y - 10.8f * k); g.quadTo(x + 5 * k, y - 11.5f * k, x + 5.4f * k, y - 17 * k); g.close(); g.fillPath();
                g.color(0x99E3F2FA); g.line(x, y - 10 * k, x, y - 1 * k, 1.1f * k); g.oval(x, y, 4.5f * k, 1.1f * k);
                g.color(0xCCFFFFFF); g.line(x - 4 * k, y - 20 * k, x - 3.5f * k, y - 14 * k, 0.9f * k);
                break;
            }
            case Pose.I_BEER: {
                g.color(0x77E3F2FA); g.roundRect(x - 6 * k, y - 18 * k, 12 * k, 18 * k, 2 * k);
                g.color(0xDDE0A030); g.rect(x - 5.2f * k, y - 14 * k, 10.4f * k, 13 * k);
                g.color(0xFFFFFDF2); g.oval(x, y - 15 * k, 6.2f * k, 2.4f * k); g.oval(x - 3 * k, y - 16 * k, 2.5f * k, 1.6f * k); g.oval(x + 2.5f * k, y - 16.2f * k, 2.4f * k, 1.5f * k);
                g.color(0x99E3F2FA); g.strokeOval(x + 8 * k, y - 9 * k, 2.6f * k, 4.5f * k, 1.6f * k);
                g.color(0x88FFFFFF);
                for (int i = 0; i < 4; i++) g.oval(x - 3 * k + i * 2 * k, y - 4 * k - (i % 2) * 4 * k, 0.6f * k, 0.6f * k);
                break;
            }
            case Pose.I_WHISKY: {
                g.color(0x77E3F2FA); g.roundRect(x - 6 * k, y - 11 * k, 12 * k, 11 * k, 1.5f * k);
                g.color(0xDDB8681C); g.rect(x - 5.3f * k, y - 6.5f * k, 10.6f * k, 6 * k);
                g.color(0xCCE3F2FA); g.roundRect(x - 3.8f * k, y - 8.5f * k, 3.6f * k, 3.4f * k, 0.7f * k); g.roundRect(x + 0.6f * k, y - 7.6f * k, 3.4f * k, 3.2f * k, 0.7f * k);
                g.color(0xAAFFFFFF); g.line(x - 4.5f * k, y - 10 * k, x - 4.2f * k, y - 2 * k, 0.9f * k);
                break;
            }
            case Pose.I_SNOWBALL: {
                g.color(0xFFF4F8FC); g.oval(x, y - 4 * k, 5.5f * k, 5 * k);
                g.color(0xFFD6E2EE); g.oval(x + 1.5f * k, y - 2.5f * k, 3 * k, 2.4f * k);
                break;
            }
            default:
        }
    }

    // ------------------------------------------------------------------ the archery ground

    /**
     * An archery target on its wooden stand (kind 0: rings of gold, red, blue, black and white on a straw boss) or a
     * straw dummy on a post (kind 1), feet at (x, gy), sized for a character h tall.
     */
    public static void target(Gfx g, float x, float gy, float h, int kind) {
        if (kind == 1) {
            float top = gy - h * 0.95f;
            g.color(0xFF6D4C41); g.line(x, gy, x, top + h * 0.1f, h * 0.035f); g.line(x - h * 0.2f, top + h * 0.32f, x + h * 0.2f, top + h * 0.32f, h * 0.03f);
            g.color(0xFFD4B26A); g.roundRect(x - h * 0.11f, top + h * 0.2f, h * 0.22f, h * 0.38f, h * 0.05f);
            g.color(0xFFC39A50); g.oval(x, top + h * 0.12f, h * 0.085f, h * 0.095f);
            g.color(0xFF8D6E3A); for (int i = 0; i < 5; i++) g.line(x - h * 0.1f, top + h * (0.25f + i * 0.07f), x + h * 0.1f, top + h * (0.27f + i * 0.07f), h * 0.006f);
            g.color(0xFF5D4037); g.line(x - h * 0.12f, top + h * 0.4f, x + h * 0.12f, top + h * 0.4f, h * 0.012f);
            return;
        }
        float cy = gy - h * 0.52f, r = h * 0.2f;
        g.color(0xFF6D4C41);
        g.line(x - r * 0.6f, gy, x - r * 0.15f, cy, h * 0.022f); g.line(x + r * 0.6f, gy, x + r * 0.15f, cy, h * 0.022f); g.line(x + r * 0.1f, gy + 2, x, cy, h * 0.018f);
        g.color(0xFFC9A86A); g.oval(x, cy, r * 1.08f, r * 1.08f);
        int[] rings = {0xFFF5F5F5, 0xFF212121, 0xFF1E88E5, 0xFFE53935, 0xFFFFD600};
        for (int i = 0; i < rings.length; i++) { g.color(rings[i]); g.oval(x, cy, r * (1 - i * 0.18f), r * (1 - i * 0.18f)); }
        g.color(0x55000000); g.oval(x, gy + 2, r * 0.9f, h * 0.02f);
    }

    /**
     * An arrow in flight from (x0, y0) to (x1, y1) (u: how far along, 0..1, along a low arc), or stuck in its mark at
     * (x1, y1) when u ≥ 1 (its tail end out of it, quivering a moment after it strikes: since = seconds since).
     */
    public static void arrow(Gfx g, float x0, float y0, float x1, float y1, float u, float len, float since) {
        float f = x1 >= x0 ? 1 : -1;
        if (u < 1) {
            float x = x0 + (x1 - x0) * u, arc = -Math.abs(x1 - x0) * 0.06f * 4 * u * (1 - u), y = y0 + (y1 - y0) * u + arc;
            float slope = (y1 - y0) / Math.max(1, Math.abs(x1 - x0)) + -0.24f * (1 - 2 * u);
            float ang = (float) Math.atan(slope);
            float hx = x, hy = y, tx = x - f * len * (float) Math.cos(ang), ty = y - len * (float) Math.sin(ang) * f * f;
            // a faint streak behind it
            g.color(alpha(0xFFFFFFFF, 0.25f)); g.line(tx - f * len * 0.6f, ty, tx, ty, Math.max(1, len * 0.02f));
            arrowShape(g, tx, ty, hx, hy, len * 0.025f);
            return;
        }
        float q = since < 0.5f ? (float) Math.sin(since * 60) * (0.5f - since) * 8 : 0;      // the quiver
        float tx = x1 - f * len * 0.75f, ty = y1 - len * 0.04f + q;
        arrowShape(g, tx, ty, x1 - f * len * 0.05f, y1, len * 0.025f);
    }

    // ------------------------------------------------------------------ the snow

    /**
     * A snowman being built at (x, gy) for a character h tall: u 0..1 how far along (the big ball rolled up first, the
     * middle one set on, the head, then the coal eyes, the carrot nose, the stick arms and a red scarf).
     */
    public static void snowman(Gfx g, float x, float gy, float h, float u, float t) {
        u = Math.max(0, Math.min(1, u));
        float b1 = Rig.smooth(0, 0.35f, u), b2 = Rig.smooth(0.35f, 0.6f, u), b3 = Rig.smooth(0.6f, 0.78f, u), face = Rig.smooth(0.78f, 0.95f, u);
        float r1 = h * 0.2f * Math.max(0.15f, b1), r2 = h * 0.14f, r3 = h * 0.1f;
        g.color(0x33000000); g.oval(x, gy + 2, r1 * 1.2f, h * 0.025f);
        if (b1 > 0) { g.radial(x - r1 * 0.35f, gy - r1 - r1 * 0.35f, r1 * 1.4f, 0xFFFFFFFF, 0xFFCFDDEA); g.oval(x, gy - r1 * 0.95f, r1, r1); }
        float y2 = gy - r1 * 1.8f - r2 * 0.8f;
        if (b2 > 0) { g.setAlpha(Math.min(1, b2 * 2)); g.radial(x - r2 * 0.35f, y2 - r2 * 0.35f - (1 - b2) * h * 0.2f, r2 * 1.4f, 0xFFFFFFFF, 0xFFCFDDEA); g.oval(x, y2 - (1 - b2) * h * 0.2f, r2, r2); g.setAlpha(1); }
        float y3 = y2 - r2 * 0.95f - r3 * 0.8f;
        if (b3 > 0) { g.setAlpha(Math.min(1, b3 * 2)); g.radial(x - r3 * 0.35f, y3 - r3 * 0.35f - (1 - b3) * h * 0.15f, r3 * 1.4f, 0xFFFFFFFF, 0xFFCFDDEA); g.oval(x, y3 - (1 - b3) * h * 0.15f, r3, r3); g.setAlpha(1); }
        if (face > 0) {
            g.setAlpha(face);
            g.color(0xFF212121); g.oval(x - r3 * 0.35f, y3 - r3 * 0.2f, r3 * 0.1f, r3 * 0.1f); g.oval(x + r3 * 0.35f, y3 - r3 * 0.2f, r3 * 0.1f, r3 * 0.1f);
            for (int i = 0; i < 5; i++) g.oval(x - r3 * 0.4f + i * r3 * 0.2f, y3 + r3 * 0.45f - (float) Math.sin(i * 0.8f) * r3 * 0.1f, r3 * 0.06f, r3 * 0.06f);
            g.color(0xFFFF7A1A); g.begin(); g.moveTo(x, y3 - r3 * 0.05f); g.lineTo(x + r3 * 0.75f, y3 + r3 * 0.05f); g.lineTo(x, y3 + r3 * 0.15f); g.close(); g.fillPath();
            g.color(0xFF212121); for (int i = 0; i < 3; i++) g.oval(x, y2 - r2 * 0.4f + i * r2 * 0.4f, r2 * 0.08f, r2 * 0.08f);
            g.color(0xFF6D4C41); g.line(x - r2 * 0.9f, y2 - r2 * 0.1f, x - r2 * 1.9f, y2 - r2 * 0.8f, h * 0.012f); g.line(x + r2 * 0.9f, y2 - r2 * 0.1f, x + r2 * 1.9f, y2 - r2 * 0.9f, h * 0.012f);
            g.color(0xFFD32F2F); g.roundRect(x - r3 * 0.95f, y3 + r3 * 0.75f, r3 * 1.9f, r3 * 0.32f, r3 * 0.12f);
            float sw = (float) Math.sin(t * 2.2f) * r3 * 0.08f;
            g.roundRect(x + r3 * 0.35f + sw, y3 + r3 * 0.8f, r3 * 0.3f, r3 * 0.9f, r3 * 0.1f);
            g.setAlpha(1);
        }
    }

    /** The snow lying on the ground (snowGround / snowfall settling): a white blanket with soft bumps and a sparkle, from y down. */
    public static void snowCover(Gfx g, float x0, float x1, float y, float bottom, float amount, float t) {
        if (amount <= 0.01f) return;
        g.color(alpha(0xFFF4F8FC, 0.82f * amount)); g.rect(x0, y - 4, x1 - x0, bottom - y + 4);
        g.color(alpha(0xFFFFFFFF, 0.6f * amount));
        for (int i = 0; i < 24; i++) {
            float bx = x0 + (x1 - x0) * ((i * 0.618034f) % 1f), bw = 40 + (i * 37) % 70;
            g.oval(bx, y - 2, bw, 6 + (i % 3) * 2);
        }
        g.color(alpha(0xFFDCE7F2, 0.5f * amount));
        for (int i = 0; i < 18; i++) {
            float bx = x0 + (x1 - x0) * ((i * 0.7548777f + 0.3f) % 1f), by = y + (bottom - y) * ((i * 0.5698403f) % 1f);
            g.oval(bx, by, 26 + (i * 13) % 30, 3);
        }
        // a few glints that come and go
        for (int i = 0; i < 10; i++) {
            float ph = (t * 0.7f + i * 0.37f) % 1f;
            if (ph > 0.25f) continue;
            float bx = x0 + (x1 - x0) * ((i * 0.381966f + 0.11f) % 1f), by = y + 6 + (bottom - y - 8) * ((i * 0.7071f) % 1f);
            g.color(alpha(0xFFFFFFFF, amount * (1 - ph * 4)));
            g.line(bx - 3, by, bx + 3, by, 1); g.line(bx, by - 3, bx, by + 3, 1);
        }
    }

    // ------------------------------------------------------------------ the animals of a place

    private static final java.util.Map<Integer, Look> ANIMAL_LOOKS = new java.util.concurrent.ConcurrentHashMap<Integer, Look>();

    /** The look of an animal of this species (its usual colour). */
    public static Look animalLook(int species) {
        Look l = ANIMAL_LOOKS.get(species);
        if (l != null) return l;
        l = new Look();
        l.kind = species >= 20 ? Look.BIRD : Look.ANIMAL;
        l.species = species;
        l.furColor = species == Look.SP_HEN ? 0xFFF5F0E6 : species == Look.SP_COW ? 0xFFF1EDE6 : species == Look.SP_GOAT ? 0xFF8D6E63
                : species == Look.SP_DOG ? 0xFFC08A4A : species == Look.SP_CAT ? 0xFF9E9E9E : Look.furOf(species);
        l.skin = Puppet.shade(l.furColor, 1.15f);
        l.eyeColor = 0xFF2B1B10;
        ANIMAL_LOOKS.put(species, l);
        return l;
    }

    /** How tall an animal of this species stands beside a grown-up h tall (its drawing height). */
    public static float animalHeight(int species, float h) {
        switch (species) {
            case Look.SP_COW: return 0.85f * h;
            case Look.SP_HORSE: return 0.94f * h;
            case Look.SP_ELEPHANT: return 1.3f * h;
            case Look.SP_DEER: return 0.72f * h;
            case Look.SP_GOAT: return 0.5f * h;
            case Look.SP_DOG: return 0.42f * h;
            case Look.SP_CAT: return 0.28f * h;
            case Look.SP_RABBIT: return 0.22f * h;
            case Look.SP_HEN: case Look.SP_DUCK: return 0.3f * h;
            case Look.SP_PARROT: return 0.22f * h;
            case Look.SP_PEACOCK: return 0.6f * h;
            default: return 0.5f * h;
        }
    }

    /**
     * An animal at (x, gy) facing f, H its drawing height: still (grazing — the head bobbing down to the grass — or
     * sitting up), walking (walkAmt 0..1, its legs with walk), running (a faster stride).
     */
    public static void animal(Gfx g, int species, float x, float gy, float H, float f, float walk, float walkAmt, float t, boolean graze) {
        Look l = animalLook(species);
        Pose p = new Pose();
        p.facing = f; p.walk = walk; p.walkAmt = walkAmt; p.time = t;
        p.blink = (t % 4.1f) < 0.12f ? 1 : 0;
        if (graze && walkAmt < 0.1f) p.nod = 0.35f + 0.15f * (float) Math.sin(t * 1.3f);
        g.save();
        g.translate(x, gy);
        if (l.kind == Look.BIRD) {
            if (walkAmt > 0.1f) g.translate(0, -Math.abs((float) Math.sin(walk)) * H * 0.04f);
            Puppet.drawBird(g, l, p, H);
        } else Puppet.drawAnimal(g, l, p, H);
        g.restore();
    }

    // ------------------------------------------------------------------ the inserts (whole frame)

    /**
     * The driver's view down onto the steering wheel, both hands on it at ten to two, the dashboard and the road
     * through the windscreen; the wheel turns (moves: Film.INS_MOVE_TURN_L / _R) and comes back. w, h: the frame.
     */
    public static void insertSteer(Gfx g, float w, float h, float u, float d, int moves, int skin, int sleeve) {
        // the road through the glass, rushing by
        g.linear(0, 0, 0, h * 0.55f, 0xFF9EC9EB, 0xFFDDEBF3); g.rect(0, 0, w, h * 0.55f);
        g.color(0xFF7CB342); g.rect(0, h * 0.42f, w, h * 0.2f);
        g.color(0xFF616161);
        g.begin(); g.moveTo(w * 0.42f, h * 0.42f); g.lineTo(w * 0.58f, h * 0.42f); g.lineTo(w * 0.95f, h * 0.62f); g.lineTo(w * 0.05f, h * 0.62f); g.close(); g.fillPath();
        g.color(0xFFF5F5F5);
        for (int i = 0; i < 4; i++) {
            float k = ((u * 1.4f + i * 0.25f) % 1f), y0 = h * (0.42f + 0.2f * k * k), y1 = h * (0.42f + 0.2f * Math.min(1, (k + 0.08f) * (k + 0.08f)));
            g.line(w * 0.5f, y0, w * 0.5f, y1, 1 + 6 * k);
        }
        // the dashboard: dark, with the dials glowing
        g.color(0xFF263238);
        g.begin(); g.moveTo(0, h * 0.5f); g.quadTo(w * 0.5f, h * 0.43f, w, h * 0.5f); g.lineTo(w, h); g.lineTo(0, h); g.close(); g.fillPath();
        g.color(0xFF37474F); g.roundRect(w * 0.32f, h * 0.53f, w * 0.36f, h * 0.12f, h * 0.03f);
        for (int i = 0; i < 2; i++) {
            float cx = w * (0.42f + i * 0.16f), cy = h * 0.59f, r = h * 0.045f;
            g.color(0xFF102027); g.oval(cx, cy, r, r);
            g.color(0xFF4FC3F7); g.strokeOval(cx, cy, r, r, 1.5f);
            float a = (float) Math.toRadians(200 + 120 * Math.min(1, u / d * 1.5f) + i * 20);
            g.color(0xFFFF7043); g.line(cx, cy, cx + (float) Math.cos(a) * r * 0.85f, cy + (float) Math.sin(a) * r * 0.85f, 2);
        }
        // the wheel, turned and back
        float dir = (moves & Film.INS_MOVE_TURN_L) != 0 ? -1 : (moves & Film.INS_MOVE_TURN_R) != 0 ? 1 : 0;
        float turn = dir != 0 ? dir * 75 * (float) Math.sin(Math.PI * Math.min(1, u / Math.max(0.3f, d))) : (float) Math.sin(u * 3.1f) * 8;
        float cx = w * 0.5f, cy = h * 0.93f, R = h * 0.42f;
        g.save(); g.translate(cx, cy); g.rotate(turn);
        g.color(0xFF1B1B1B); g.strokeOval(0, 0, R, R * 0.92f, h * 0.05f);
        g.color(0xFF2E2E2E); g.line(-R * 0.95f, 0, R * 0.95f, 0, h * 0.04f); g.line(0, 0, 0, R * 0.9f, h * 0.04f);
        g.color(0xFF424242); g.oval(0, 0, R * 0.22f, R * 0.2f);
        g.color(0xFFB0BEC5); g.oval(0, -R * 0.02f, R * 0.07f, R * 0.06f);
        // the hands round the rim at ten to two, the sleeves coming in from below
        for (int sdx = -1; sdx <= 1; sdx += 2) {
            double a = Math.toRadians(sdx < 0 ? 215 : 325);
            float hx = (float) Math.cos(a) * R, hy = (float) Math.sin(a) * R * 0.92f;
            g.color(sleeve); g.line(hx, hy, hx + sdx * R * 0.25f, hy + R * 1.2f, h * 0.11f);
            g.color(skin); g.roundRect(hx - h * 0.055f, hy - h * 0.05f, h * 0.11f, h * 0.1f, h * 0.035f);
            g.color(Puppet.shade(skin, 0.85f));
            for (int i = 0; i < 3; i++) g.line(hx - h * 0.035f + i * h * 0.03f, hy - h * 0.045f, hx - h * 0.035f + i * h * 0.03f, hy + h * 0.02f, 1.2f);
        }
        g.restore();
    }

    /**
     * The footwell: the brake pedal (wide) and the accelerator (tall), a shoe on them. GO: the foot presses the
     * accelerator down; BRAKE: it lifts off, moves across and presses the brake (HARD: fast and right down).
     */
    public static void insertPedals(Gfx g, float w, float h, float u, float d, int moves, int shoe, int trouser) {
        g.linear(0, 0, 0, h, 0xFF1C2328, 0xFF0E1214); g.rect(0, 0, w, h);
        g.color(0xFF263238); g.rect(0, h * 0.82f, w, h * 0.18f);
        for (int i = 0; i < 9; i++) { g.color(0xFF1A2126); g.rect(w * 0.08f + i * w * 0.1f, h * 0.84f, w * 0.06f, h * 0.14f); }
        boolean brake = (moves & Film.INS_MOVE_BRAKE) != 0, hard = (moves & Film.INS_MOVE_HARD) != 0;
        float k = u / Math.max(0.3f, d);
        float bx = w * 0.4f, ax = w * 0.66f;
        // how far each pedal is pressed, and where the foot is
        float accel, br, fx;
        if (!brake) { accel = Rig.smooth(0.1f, 0.45f, k); br = 0; fx = ax; }
        else {
            float lift = Rig.smooth(0.05f, hard ? 0.18f : 0.3f, k), on = Rig.smooth(hard ? 0.18f : 0.3f, hard ? 0.32f : 0.55f, k);
            accel = 0.5f * (1 - lift); fx = ax + (bx - ax) * Rig.smooth(hard ? 0.12f : 0.2f, hard ? 0.28f : 0.45f, k); br = on * (hard ? 1 : 0.7f);
        }
        // the brake pedal
        float by0 = h * 0.42f + br * h * 0.12f;
        g.color(0xFF455A64); g.line(bx, h * 0.05f, bx, by0, w * 0.018f);
        g.color(0xFF212121); g.roundRect(bx - w * 0.07f, by0 - h * 0.05f, w * 0.14f, h * 0.11f, h * 0.02f);
        g.color(0xFF424242); for (int i = 0; i < 4; i++) g.line(bx - w * 0.06f, by0 - h * 0.03f + i * h * 0.022f, bx + w * 0.06f, by0 - h * 0.03f + i * h * 0.022f, 1.2f);
        // the accelerator
        float ay0 = h * 0.5f + accel * h * 0.1f;
        g.color(0xFF455A64); g.line(ax, h * 0.1f, ax, ay0, w * 0.014f);
        g.color(0xFF212121); g.roundRect(ax - w * 0.035f, ay0 - h * 0.1f, w * 0.07f, h * 0.2f, h * 0.02f);
        // the foot: the trouser leg from above, the shoe pressing
        float press = brake ? br : accel, lifted = brake ? (1 - Rig.smooth(hard ? 0.18f : 0.3f, hard ? 0.3f : 0.5f, k)) * Rig.smooth(0.05f, 0.2f, k) : 0;
        float fy = (Math.abs(fx - bx) < Math.abs(fx - ax) ? by0 : ay0) - h * 0.08f - lifted * h * 0.08f + press * h * 0.02f;
        g.color(trouser);
        g.begin(); g.moveTo(fx - w * 0.09f, 0); g.lineTo(fx + w * 0.07f, 0); g.lineTo(fx + w * 0.06f, fy - h * 0.12f); g.lineTo(fx - w * 0.07f, fy - h * 0.12f); g.close(); g.fillPath();
        g.color(shoe); g.roundRect(fx - w * 0.075f, fy - h * 0.16f, w * 0.15f, h * 0.26f, w * 0.05f);
        g.color(Puppet.shade(shoe, 1.3f)); g.oval(fx - w * 0.02f, fy - h * 0.1f, w * 0.03f, h * 0.05f);
        if (brake && hard && k > 0.3f) { g.color(alpha(0xFFFFFFFF, 0.12f * (1 - Rig.smooth(0.3f, 0.6f, k)))); g.rect(0, 0, w, h); }
    }

    /** A cyclist's feet on the pedals, close and low: the chain ring and the cranks going round, the road streaming below. */
    public static void insertCycleFeet(Gfx g, float w, float h, float u, float t, boolean hard, int shoe, int trouser, int frame) {
        g.linear(0, 0, 0, h, 0xFFB9D7E8, 0xFFE8F1F5); g.rect(0, 0, w, h * 0.7f);
        g.color(0xFF757575); g.rect(0, h * 0.7f, w, h * 0.3f);
        g.color(0xFFBDBDBD);
        for (int i = 0; i < 6; i++) { float x = w - ((u * (hard ? 900 : 600) + i * w / 5f) % (w * 1.2f)); g.rect(x, h * 0.84f, w * 0.08f, 4); }
        float cx = w * 0.5f, cy = h * 0.55f, R = h * 0.2f, ang = u * (hard ? 9f : 6f);
        // the frame tubes into the bottom bracket, the chain ring with its teeth, the chain back to the wheel
        g.color(frame); g.line(cx, cy, cx - w * 0.45f, cy - h * 0.2f, h * 0.05f); g.line(cx, cy, cx + w * 0.2f, cy - h * 0.6f, h * 0.05f); g.line(cx, cy, cx - w * 0.15f, cy - h * 0.7f, h * 0.045f);
        g.color(0xFF9E9E9E); g.strokeOval(cx, cy, R * 0.7f, R * 0.7f, h * 0.02f);
        for (int i = 0; i < 20; i++) { double a = ang + i * Math.PI / 10; g.oval(cx + (float) Math.cos(a) * R * 0.72f, cy + (float) Math.sin(a) * R * 0.72f, 2, 2); }
        g.color(0xFF616161); g.line(cx, cy - R * 0.7f, 0, cy - R * 0.3f, 3); g.line(cx, cy + R * 0.7f, 0, cy + R * 0.2f, 3);
        for (int sdx = 0; sdx < 2; sdx++) {
            double a = ang + sdx * Math.PI;
            float px = cx + (float) Math.cos(a) * R, py = cy + (float) Math.sin(a) * R;
            g.color(sdx == 0 ? 0xFF9E9E9E : 0xFF757575); g.line(cx, cy, px, py, h * 0.03f);
            // the leg from above the frame to the foot on the pedal
            float kx = px - w * 0.04f, ky = py - h * 0.45f;
            g.color(Puppet.shade(trouser, sdx == 0 ? 1f : 0.8f)); g.line(kx, -h * 0.05f, kx + w * 0.03f, ky, h * 0.1f); g.line(kx + w * 0.03f, ky, px, py - h * 0.04f, h * 0.085f);
            g.color(0xFF212121); g.roundRect(px - w * 0.05f, py - h * 0.012f, w * 0.1f, h * 0.024f, 2);
            g.color(sdx == 0 ? shoe : Puppet.shade(shoe, 0.8f)); g.roundRect(px - w * 0.065f, py - h * 0.07f, w * 0.13f, h * 0.065f, h * 0.025f);
        }
        g.color(0xFF424242); g.oval(cx, cy, R * 0.12f, R * 0.12f);
    }

    /** The target close: the rings, an arrow just struck into the gold, quivering. */
    public static void insertArrow(Gfx g, float w, float h, float u) {
        g.color(0xFF8D6E63); g.rect(0, 0, w, h);
        g.color(0xFFC9A86A); g.oval(w * 0.5f, h * 0.5f, h * 0.48f, h * 0.48f);
        int[] rings = {0xFFF5F5F5, 0xFF212121, 0xFF1E88E5, 0xFFE53935, 0xFFFFD600};
        for (int i = 0; i < rings.length; i++) { g.color(rings[i]); g.oval(w * 0.5f, h * 0.5f, h * 0.45f * (1 - i * 0.18f), h * 0.45f * (1 - i * 0.18f)); }
        float q = u < 0.6f ? (float) Math.sin(u * 55) * (0.6f - u) * 14 : 0;
        float x1 = w * 0.5f + 4, y1 = h * 0.5f + 2;
        arrowShape(g, x1 - w * 0.36f, y1 - h * 0.3f + q, x1, y1, h * 0.012f);
        g.color(0x55000000); g.oval(x1, y1, h * 0.015f, h * 0.012f);
    }
}
