package com.tarun.kahani.core;

/**
 * v37: the things everyday activities need, drawn in a character's own frame (the feet at 0,0, up is negative y,
 * h = the character's height, f = the way they face): the vehicles they ride or drive, the playground's swing,
 * slide, see-saw and merry-go-round, an exercise mat and a skipping rope, a bath's tiled corner, curtain, mug and
 * towel, a folding screen to change behind, and a small heart for a peck on the cheek.
 * Everything a vehicle covers (a car's door, a bus's side) is drawn in front of the character; what is behind
 * them (a bicycle's frame, a car's dark inside) is drawn before them.
 */
public final class Props {
    private Props() {}

    static int alpha(int c, float a) { return Puppet.alpha(c, a); }

    // ------------------------------------------------------------------ vehicles

    /**
     * The seat of a vehicle: {x of the seat from the vehicle's centre (towards its front), the seat's height above
     * the road} in units of the drawing height (the rider's height for a bicycle, a grown-up's for the others).
     */
    public static float[] seatOf(int kind) {
        // a passenger's seat: the back seat of a car or an auto, a window in the middle of a bus
        if (kind > Film.V_PASSENGER) {
            switch (kind - Film.V_PASSENGER) {
                case Film.V_CAR: return new float[]{-0.36f, 0.33f};
                case Film.V_BUS: return new float[]{0.2f, 0.62f};
                case Film.V_AUTO: return new float[]{-0.34f, 0.42f};
                default:
            }
        }
        switch (kind) {
            case Film.V_BICYCLE: return new float[]{-0.12f, 0.46f};
            case Film.V_MOTORBIKE: return new float[]{-0.1f, 0.44f};
            case Film.V_SCOOTER: return new float[]{-0.14f, 0.42f};
            case Film.V_CAR: return new float[]{0.05f, 0.33f};
            case Film.V_BUS: return new float[]{1.35f, 0.62f};
            case Film.V_AUTO: return new float[]{0.12f, 0.4f};
            default: return new float[]{0, 0.3f};
        }
    }

    /**
     * Draws a vehicle with its seat at the origin's x (so the rider sits on it): the back part (front = false:
     * wheels, frame, the dark inside of a car) or the front part (front = true: a car's door and glass, a bus's side,
     * a scooter's apron). s = the drawing height, dist = how far it has gone (the wheels turn with it), t = time.
     */
    public static void vehicle(Gfx g, int kind, float s, float f, float dist, float t, int color, boolean front) {
        f = f < 0 ? -1 : 1;
        float[] seat = seatOf(kind);
        boolean pass = kind > Film.V_PASSENGER;
        if (pass) kind -= Film.V_PASSENGER;
        g.save();
        g.translate(-seat[0] * s * f, 0);         // the seat over the origin
        switch (kind) {
            case Film.V_BICYCLE: bicycle(g, s, f, dist, color, front); break;
            case Film.V_MOTORBIKE: motorbike(g, s, f, dist, t, color, front); break;
            case Film.V_SCOOTER: scooter(g, s, f, dist, t, color, front); break;
            case Film.V_CAR: car(g, s, f, dist, t, color, front); break;
            case Film.V_BUS: bus(g, s, f, dist, t, front); break;
            case Film.V_AUTO: auto(g, s, f, dist, t, front); break;
            default:
        }
        if (pass && !front) {
            // the driver at the wheel of a passenger's ride: a shape seen through the glass of a car or a bus, a
            // driver in a khaki shirt in an open auto
            float dx = seatOf(kind)[0] * s * f, dy = -seatOf(kind)[1] * s;
            g.save(); g.translate(dx, dy);
            if (kind == Film.V_AUTO) {
                g.color(0xFFC8B273); g.roundRect(-0.08f * s, -0.36f * s, 0.16f * s, 0.36f * s, 0.05f * s);
                g.color(0xFFC68B59); g.oval(0, -0.44f * s, 0.07f * s, 0.08f * s);
                g.color(0xFF3E2723); g.oval(0, -0.49f * s, 0.07f * s, 0.04f * s);
            } else {
                g.color(0xFF455A64); g.roundRect(-0.09f * s, -0.36f * s, 0.18f * s, 0.36f * s, 0.06f * s);
                g.oval(0, -0.45f * s, 0.075f * s, 0.085f * s);
            }
            g.restore();
        }
        g.restore();
    }

    /** A wheel: the tyre, the rim, spokes (or a hub cap) turned by the distance gone. */
    static void wheel(Gfx g, float cx, float cy, float r, float dist, boolean spokes) {
        g.color(0xFF212121); g.oval(cx, cy, r, r);
        g.color(0xFF9E9E9E); g.oval(cx, cy, r * 0.78f, r * 0.78f);
        g.color(spokes ? 0xFF303030 : 0xFF616161); g.oval(cx, cy, r * 0.7f, r * 0.7f);
        double a = dist / Math.max(1, r);
        if (spokes) {
            g.color(0xFFBDBDBD);
            for (int i = 0; i < 8; i++) {
                double an = a + i * Math.PI / 4;
                g.line(cx, cy, cx + (float) Math.cos(an) * r * 0.7f, cy + (float) Math.sin(an) * r * 0.7f, Math.max(0.8f, r * 0.03f));
            }
        } else {
            g.color(0xFFBDBDBD); g.oval(cx, cy, r * 0.45f, r * 0.45f);
            g.color(0xFF757575);
            for (int i = 0; i < 5; i++) {
                double an = a + i * Math.PI * 2 / 5;
                g.oval(cx + (float) Math.cos(an) * r * 0.27f, cy + (float) Math.sin(an) * r * 0.27f, r * 0.06f, r * 0.06f);
            }
        }
        g.color(0xFF424242); g.oval(cx, cy, r * 0.1f, r * 0.1f);
    }

    static void bicycle(Gfx g, float h, float f, float dist, int color, boolean front) {
        if (front) {
            // the near pedal and crank in front of the leg
            float rw = 0.16f * h, bbx = -0.02f * h * f, bby = -rw * 0.95f;
            double ca = dist / (rw * 1.6f);
            float px = bbx + (float) Math.cos(ca) * 0.065f * h, py = bby + (float) Math.sin(ca) * 0.065f * h;
            g.color(0xFF9E9E9E); g.line(bbx, bby, px, py, 0.014f * h);
            g.color(0xFF212121); g.roundRect(px - 0.025f * h, py - 0.008f * h, 0.05f * h, 0.016f * h, 0.004f * h);
            return;
        }
        float rw = 0.16f * h;
        float rx = -0.33f * h * f, fx = 0.33f * h * f, wy = -rw;
        wheel(g, rx, wy, rw, dist, true);
        wheel(g, fx, wy, rw, dist, true);
        float bbx = -0.02f * h * f, bby = -rw * 0.95f;
        float sx = -0.12f * h * f, sy = -0.44f * h;
        float htx = 0.25f * h * f, hty = -0.5f * h, hbx = 0.28f * h * f, hby = -0.38f * h;
        float w = 0.022f * h;
        g.color(Puppet.shade(color, 0.75f));
        g.line(bbx, bby, rx, wy, w * 0.8f); g.line(sx, sy, rx, wy, w * 0.8f);
        g.color(color);
        g.line(bbx, bby, sx, sy, w); g.line(sx + 0.01f * h * f, sy + 0.02f * h, htx, hty + 0.02f * h, w); g.line(bbx, bby, hbx, hby, w * 1.1f);
        g.line(htx, hty, hbx, hby, w * 1.1f);
        g.color(0xFF9E9E9E); g.line(hbx, hby, fx, wy, w * 0.75f);
        // the handlebar and its grips, the saddle
        g.color(0xFF616161); g.line(htx, hty, htx - 0.02f * h * f, hty - 0.08f * h, w * 0.7f);
        g.color(0xFF212121); g.line(htx - 0.02f * h * f, hty - 0.08f * h, htx - 0.07f * h * f, hty - 0.075f * h, w * 0.9f);
        g.color(0xFF616161); g.line(sx, sy, sx, sy - 0.02f * h, w * 0.7f);
        g.color(0xFF263238); g.roundRect(sx - 0.05f * h, sy - 0.035f * h, 0.1f * h, 0.025f * h, 0.012f * h);
        // the chain ring and the chain
        g.color(0xFF757575); g.strokeOval(bbx, bby, 0.04f * h, 0.04f * h, 0.008f * h);
        g.line(bbx, bby - 0.04f * h, rx, wy - 0.02f * h, 0.005f * h); g.line(bbx, bby + 0.04f * h, rx, wy + 0.02f * h, 0.005f * h);
        // the far crank and pedal (behind)
        double ca = dist / (rw * 1.6f) + Math.PI;
        float px = bbx + (float) Math.cos(ca) * 0.065f * h, py = bby + (float) Math.sin(ca) * 0.065f * h;
        g.color(0xFF757575); g.line(bbx, bby, px, py, 0.012f * h);
        g.color(0xFF424242); g.roundRect(px - 0.022f * h, py - 0.007f * h, 0.044f * h, 0.014f * h, 0.004f * h);
        // a bell on the bar
        g.color(0xFFCFD8DC); g.oval(htx - 0.035f * h * f, hty - 0.085f * h, 0.012f * h, 0.009f * h);
    }

    static void motorbike(Gfx g, float s, float f, float dist, float t, int color, boolean front) {
        float rw = 0.13f * s, rx = -0.42f * s * f, fx = 0.42f * s * f;
        if (front) {
            // the tank and the engine's side cover in front of the rider's knee
            g.color(color); g.begin(); g.moveTo(0.02f * s * f, -0.47f * s); g.quadTo(0.14f * s * f, -0.52f * s, 0.26f * s * f, -0.45f * s);
            g.lineTo(0.2f * s * f, -0.36f * s); g.lineTo(0.03f * s * f, -0.37f * s); g.close(); g.fillPath();
            g.color(alpha(0xFFFFFFFF, 0.35f)); g.oval(0.12f * s * f, -0.47f * s, 0.05f * s, 0.012f * s);
            return;
        }
        wheel(g, rx, -rw, rw, dist, false);
        wheel(g, fx, -rw, rw, dist, false);
        // engine, exhaust, frame
        g.color(0xFF455A64); g.roundRect(-0.12f * s, -0.33f * s, 0.26f * s, 0.16f * s, 0.03f * s);
        g.color(0xFFB0BEC5); g.line(-0.05f * s * f, -0.18f * s, -0.5f * s * f, -0.2f * s, 0.035f * s);
        g.color(0xFF263238); g.line(rx, -rw, -0.05f * s * f, -0.3f * s, 0.03f * s);
        // the seat (long, black), the rear fender and tail light
        g.color(0xFF212121); g.roundRect(-0.32f * s, -0.47f * s, 0.36f * s, 0.06f * s, 0.025f * s);
        if (f < 0) g.roundRect(-0.04f * s, -0.47f * s, 0.36f * s, 0.06f * s, 0.025f * s);
        g.color(color); g.begin(); g.moveTo(-0.3f * s * f, -0.42f * s); g.quadTo(-0.5f * s * f, -0.42f * s, -0.55f * s * f, -0.3f * s); g.lineTo(-0.48f * s * f, -0.3f * s); g.close(); g.fillPath();
        g.color(0xFFE53935); g.oval(-0.53f * s * f, -0.36f * s, 0.015f * s, 0.012f * s);
        // the fork, the handlebar, the headlamp
        g.color(0xFF9E9E9E); g.line(0.3f * s * f, -0.56f * s, fx, -rw, 0.025f * s);
        g.color(0xFF212121); g.line(0.3f * s * f, -0.58f * s, 0.24f * s * f, -0.64f * s, 0.022f * s);
        g.color(0xFFFFF59D); g.oval(0.37f * s * f, -0.52f * s, 0.035f * s, 0.035f * s);
        g.color(0xFF616161); g.strokeOval(0.37f * s * f, -0.52f * s, 0.035f * s, 0.035f * s, 0.006f * s);
        // a little exhaust smoke
        for (int i = 0; i < 3; i++) {
            float ph = (t * 1.5f + i / 3f) % 1f;
            g.color(alpha(0xFF9E9E9E, 0.25f * (1 - ph)));
            g.oval(-0.55f * s * f - ph * 0.25f * s * f, -0.2f * s - ph * 0.08f * s, 0.03f * s + ph * 0.05f * s, 0.025f * s + ph * 0.04f * s);
        }
    }

    static void scooter(Gfx g, float s, float f, float dist, float t, int color, boolean front) {
        float rw = 0.09f * s, rx = -0.36f * s * f, fx = 0.36f * s * f;
        if (front) {
            // the front apron over the rider's shins, the headlamp on the handlebar
            g.color(color);
            g.begin(); g.moveTo(0.16f * s * f, -0.12f * s); g.lineTo(0.3f * s * f, -0.15f * s); g.quadTo(0.34f * s * f, -0.45f * s, 0.26f * s * f, -0.6f * s);
            g.lineTo(0.2f * s * f, -0.58f * s); g.quadTo(0.24f * s * f, -0.4f * s, 0.16f * s * f, -0.12f * s); g.close(); g.fillPath();
            g.color(0xFF212121); g.line(0.24f * s * f, -0.62f * s, 0.15f * s * f, -0.64f * s, 0.022f * s);
            g.color(0xFFFFF59D); g.oval(0.28f * s * f, -0.6f * s, 0.028f * s, 0.024f * s);
            return;
        }
        wheel(g, rx, -rw, rw, dist, false);
        wheel(g, fx, -rw, rw, dist, false);
        // the rear cowl, the seat, the floorboard
        g.color(color);
        g.begin(); g.moveTo(-0.42f * s * f, -0.2f * s); g.quadTo(-0.42f * s * f, -0.4f * s, -0.2f * s * f, -0.41f * s); g.lineTo(0.0f, -0.4f * s);
        g.lineTo(0.0f, -0.18f * s); g.lineTo(-0.3f * s * f, -0.14f * s); g.close(); g.fillPath();
        g.color(0xFF3E2723); g.roundRect(-0.3f * s, -0.45f * s, 0.32f * s, 0.05f * s, 0.022f * s);
        if (f < 0) { g.color(0xFF3E2723); g.roundRect(-0.02f * s, -0.45f * s, 0.32f * s, 0.05f * s, 0.022f * s); }
        g.color(0xFF616161); g.rect(Math.min(0, 0.2f * s * f), -0.14f * s, 0.2f * s, 0.03f * s);
        g.color(0xFF9E9E9E); g.line(0.3f * s * f, -0.15f * s, fx, -rw, 0.02f * s);
        g.color(0xFFE53935); g.oval(-0.42f * s * f, -0.3f * s, 0.012f * s, 0.016f * s);
    }

    static void car(Gfx g, float s, float f, float dist, float t, int color, boolean front) {
        float L = 0.95f * s;           // half length
        float rw = 0.12f * s;
        float base = -0.045f * s, belt = -0.55f * s, roof = -0.98f * s;      // the sills low: nobody's feet show under the car
        float back = -0.6f * s * f, wsBot = 0.38f * s * f, wsTop = 0.2f * s * f, rearTop = -0.48f * s * f;
        if (!front) {
            // the dark inside of the cabin, and the seat back behind the driver
            g.color(0xFF263238);
            g.begin(); g.moveTo(back, belt); g.lineTo(rearTop, roof + 0.03f * s); g.lineTo(wsTop, roof + 0.03f * s); g.lineTo(wsBot, belt); g.close(); g.fillPath();
            g.color(0xFF37474F); g.roundRect(-0.14f * s * f - 0.05f * s, -0.82f * s, 0.1f * s, 0.5f * s, 0.03f * s);
            return;
        }
        // the cabin's glass (the driver seen through it), the pillars and the roof
        g.color(alpha(0xFFB3E5FC, 0.22f));
        g.begin(); g.moveTo(back, belt); g.lineTo(rearTop, roof + 0.03f * s); g.lineTo(wsTop, roof + 0.03f * s); g.lineTo(wsBot, belt); g.close(); g.fillPath();
        g.color(alpha(0xFFFFFFFF, 0.18f)); g.line(wsTop - 0.04f * s * f, roof + 0.08f * s, wsBot - 0.1f * s * f, belt - 0.06f * s, 0.03f * s);
        g.color(Puppet.shade(color, 0.85f));
        g.line(back, belt, rearTop, roof + 0.02f * s, 0.05f * s);
        g.line(wsTop, roof + 0.02f * s, wsBot, belt, 0.035f * s);
        g.line(-0.2f * s * f, roof + 0.02f * s, -0.22f * s * f, belt, 0.03f * s);
        g.color(color); g.line(rearTop, roof + 0.02f * s, wsTop, roof + 0.02f * s, 0.045f * s);
        // the body below the windows: bonnet, boot, the door and its handle, lamps, bumpers
        g.color(color);
        g.begin(); g.moveTo(-L * f, base); g.lineTo(-L * f, belt + 0.12f * s); g.quadTo(-L * f, belt, back - 0.1f * s * f, belt);
        g.lineTo(wsBot + 0.05f * s * f, belt); g.quadTo(L * f, belt + 0.05f * s, L * f, belt + 0.2f * s); g.lineTo(L * f, base); g.close(); g.fillPath();
        g.color(alpha(0xFFFFFFFF, 0.22f)); g.rect(Math.min(-L * f, L * f), belt + 0.02f * s, 2 * L, 0.02f * s);
        g.color(Puppet.shade(color, 0.7f)); g.line(-0.2f * s * f, belt, -0.2f * s * f, base - 0.02f * s, 0.006f * s); g.line(0.36f * s * f, belt + 0.02f * s, 0.36f * s * f, base - 0.02f * s, 0.006f * s);
        g.color(0xFFE0E0E0); g.roundRect(0.18f * s * f - 0.03f * s, belt + 0.07f * s, 0.06f * s, 0.015f * s, 0.006f * s);
        g.color(0xFFFFF59D); g.oval((L - 0.03f * s) * f, belt + 0.12f * s, 0.03f * s, 0.022f * s);
        g.color(0xFFE53935); g.oval(-(L - 0.02f * s) * f, belt + 0.1f * s, 0.02f * s, 0.025f * s);
        g.color(0xFF9E9E9E); g.roundRect(Math.min(-L * f, L * f) - 0.02f * s, base - 0.035f * s, 2 * L + 0.04f * s, 0.04f * s, 0.015f * s);
        // the wheels in their arches
        g.color(0xFF1A1A1A); g.oval(-0.6f * s * f, -rw, rw * 1.18f, rw * 1.18f); g.oval(0.6f * s * f, -rw, rw * 1.18f, rw * 1.18f);
        wheel(g, -0.6f * s * f, -rw, rw, dist, false);
        wheel(g, 0.6f * s * f, -rw, rw, dist, false);
        // the steering wheel in the driver's hands
        g.color(0xFF212121); g.line(0.2f * s * f, belt - 0.1f * s, 0.26f * s * f, belt + 0.02f * s, 0.022f * s);
    }

    static void bus(Gfx g, float s, float f, float dist, float t, boolean front) {
        float L = 1.85f * s, rw = 0.17f * s;
        float base = -0.06f * s, belt = -0.82f * s, top = -1.55f * s, winTop = -1.36f * s;
        int body = 0xFFFFB300, stripe = 0xFFC62828;
        if (!front) {
            g.color(0xFF263238); g.rect(-L, winTop, 2 * L, belt - winTop);
            // passengers seen through the windows (only their heads and shoulders, dark against the light)
            g.color(0xFF37474F);
            for (int i = 0; i < 5; i++) {
                float px = (-1.5f + i * 0.55f) * s * f;
                g.oval(px, belt - 0.24f * s, 0.07f * s, 0.085f * s);
                g.roundRect(px - 0.11f * s, belt - 0.16f * s, 0.22f * s, 0.2f * s, 0.06f * s);
            }
            return;
        }
        // glass over the windows; the body round them; the door, the stripe, lamps, the number board
        g.color(alpha(0xFFB3E5FC, 0.2f)); g.rect(-L, winTop, 2 * L, belt - winTop);
        g.color(body);
        g.rect(-L, top, 2 * L, winTop - top);
        g.rect(-L, belt, 2 * L, base - belt);
        for (int i = 0; i <= 6; i++) { float px = -L + i * (2 * L / 6); g.rect(px - 0.03f * s, winTop, 0.06f * s, belt - winTop); }
        g.color(alpha(0xFFFFFFFF, 0.25f)); g.rect(-L, top + 0.02f * s, 2 * L, 0.04f * s);
        g.color(stripe); g.rect(-L, belt + 0.1f * s, 2 * L, 0.07f * s);
        g.color(0xFF5D4037); g.rect(0.95f * s * f - 0.14f * s, belt - 0.5f * s, 0.28f * s, (base - belt) + 0.5f * s);
        g.color(alpha(0xFFB3E5FC, 0.35f)); g.rect(0.95f * s * f - 0.1f * s, belt - 0.46f * s, 0.2f * s, 0.38f * s);
        g.color(0xFF263238); g.roundRect(L * f - (f > 0 ? 0.55f * s : -0.05f * s), top + 0.03f * s, 0.5f * s, 0.14f * s, 0.02f * s);
        g.color(0xFFFFEB3B); g.oval((L - 0.04f * s) * f, base - 0.12f * s, 0.035f * s, 0.035f * s);
        g.color(0xFFE53935); g.oval(-(L - 0.03f * s) * f, base - 0.12f * s, 0.025f * s, 0.035f * s);
        g.color(0xFF616161); g.rect(-L - 0.02f * s, base - 0.03f * s, 2 * L + 0.04f * s, 0.05f * s);
        g.color(0xFF1A1A1A); g.oval(-1.25f * s * f, -rw, rw * 1.15f, rw * 1.15f); g.oval(1.15f * s * f, -rw, rw * 1.15f, rw * 1.15f);
        wheel(g, -1.25f * s * f, -rw, rw, dist, false);
        wheel(g, 1.15f * s * f, -rw, rw, dist, false);
        // the big steering wheel at the front window
        g.color(0xFF212121); g.line(1.5f * s * f, belt - 0.12f * s, 1.58f * s * f, belt + 0.02f * s, 0.03f * s);
    }

    static void auto(Gfx g, float s, float f, float dist, float t, boolean front) {
        // an auto-rickshaw: a green lower body, a yellow upper, a black canopy; open at the side (the driver shows)
        float rw = 0.1f * s;
        int green = 0xFF2E7D32, yellow = 0xFFFDD835;
        if (!front) {
            g.color(0xFF212121);
            g.begin(); g.moveTo(-0.62f * s * f, -0.45f * s); g.lineTo(-0.62f * s * f, -1.12f * s); g.quadTo(-0.1f * s * f, -1.3f * s, 0.42f * s * f, -1.15f * s);
            g.lineTo(0.42f * s * f, -1.08f * s); g.lineTo(-0.55f * s * f, -1.05f * s); g.lineTo(-0.55f * s * f, -0.45f * s); g.close(); g.fillPath();
            g.color(0xFF4E342E); g.roundRect(-0.6f * s * f - 0.0f, -0.62f * s, 0.01f, 0.01f, 0);
            g.color(0xFF3E2723); g.rect(Math.min(-0.55f * s * f, -0.15f * s * f), -0.55f * s, 0.4f * s, 0.12f * s);       // the passenger seat behind
            wheel(g, -0.42f * s * f, -rw, rw, dist, false);
            return;
        }
        // the lower body (the floor and the rear), the nose and the windscreen in front of the driver
        g.color(green);
        g.begin(); g.moveTo(-0.62f * s * f, -0.12f * s); g.lineTo(-0.62f * s * f, -0.5f * s); g.lineTo(-0.2f * s * f, -0.5f * s);
        g.lineTo(-0.2f * s * f, -0.3f * s); g.lineTo(0.3f * s * f, -0.3f * s); g.lineTo(0.42f * s * f, -0.15f * s); g.close(); g.fillPath();
        g.color(yellow);
        g.begin(); g.moveTo(0.3f * s * f, -0.3f * s); g.quadTo(0.52f * s * f, -0.32f * s, 0.55f * s * f, -0.15f * s); g.lineTo(0.42f * s * f, -0.15f * s); g.close(); g.fillPath();
        g.begin(); g.moveTo(0.36f * s * f, -0.3f * s); g.lineTo(0.42f * s * f, -1.08f * s); g.lineTo(0.47f * s * f, -1.08f * s); g.lineTo(0.44f * s * f, -0.3f * s); g.close(); g.fillPath();
        g.color(alpha(0xFFB3E5FC, 0.28f)); g.begin(); g.moveTo(0.38f * s * f, -0.62f * s); g.lineTo(0.42f * s * f, -1.04f * s); g.lineTo(0.45f * s * f, -1.04f * s); g.lineTo(0.42f * s * f, -0.62f * s); g.close(); g.fillPath();
        g.color(0xFFFFF59D); g.oval(0.53f * s * f, -0.24f * s, 0.025f * s, 0.022f * s);
        g.color(0xFF212121); g.line(0.3f * s * f, -0.62f * s, 0.24f * s * f, -0.66f * s, 0.02f * s);
        wheel(g, 0.48f * s * f, -rw, rw, dist, false);
        wheel(g, -0.42f * s * f, -rw, rw, dist, false);
    }

    // ------------------------------------------------------------------ the playground

    /** The swing's height above the ground, where its chains hang from (a share of the child's height). */
    public static final float SWING_TOP = 1.32f, SWING_SEAT = 0.42f;

    /** The swing's frame (an A of metal tubes each side, the top bar seen end-on); it does not move. */
    public static void swingFrame(Gfx g, float h) {
        float top = -SWING_TOP * h;
        g.color(0xFF90A4AE);
        g.line(-0.5f * h + 0.08f * h, 0, 0.08f * h, top - 0.03f * h, 0.03f * h); g.line(0.5f * h + 0.08f * h, 0, 0.08f * h, top - 0.03f * h, 0.03f * h);
        g.color(0xFF1565C0);
        g.line(-0.55f * h, 0, 0, top, 0.035f * h); g.line(0.55f * h, 0, 0, top, 0.035f * h);
        g.color(0xFF0D47A1); g.oval(0, top, 0.035f * h, 0.035f * h);
        g.color(alpha(0xFF000000, 0.15f)); g.oval(0, 0.005f * h, 0.6f * h, 0.03f * h);
    }

    /** The swing's chains and seat, in the swinging frame (the seat at SWING_SEAT, the chains up to the top bar). */
    public static void swingSeat(Gfx g, float h) {
        float top = -SWING_TOP * h, seat = -SWING_SEAT * h;
        g.color(0xFF6D4C41); g.roundRect(-0.19f * h, seat - 0.005f * h, 0.38f * h, 0.035f * h, 0.008f * h);
        g.color(0xFF9E9E9E);
        // the chains at the sides, a link pattern catching the light
        for (int side = -1; side <= 1; side += 2) {
            float x0 = side * 0.05f * h, x1 = side * 0.17f * h;
            g.line(x0, top, x1, seat, 0.009f * h);
            g.color(0xFFCFD8DC);
            for (int i = 1; i < 12; i++) { float u = i / 12f; g.oval(x0 + (x1 - x0) * u, top + (seat - top) * u, 0.005f * h, 0.008f * h); }
            g.color(0xFF9E9E9E);
        }
    }

    /** The slide's height (its platform, a share of the child's height) and the chute's length along the ground. */
    public static final float SLIDE_TOP = 0.85f, SLIDE_END = 0.3f, SLIDE_LEN = 1.25f;

    /** A slide with its ladder at the origin and the chute running down towards f. */
    public static void slide(Gfx g, float h, float f) {
        float top = -SLIDE_TOP * h, end = -SLIDE_END * h, len = SLIDE_LEN * h;
        // the ladder: two rails and rungs
        g.color(0xFF78909C);
        g.line(-0.2f * h * f, 0, -0.12f * h * f, top - 0.12f * h, 0.022f * h); g.line(-0.08f * h * f, 0, 0.0f, top - 0.12f * h, 0.022f * h);
        for (float y = -0.12f * h; y > top; y -= 0.12f * h) {
            float u = y / top;
            g.line(-0.2f * h * f + 0.08f * h * f * u, y, -0.08f * h * f + 0.08f * h * f * u, y, 0.015f * h);
        }
        // the platform with its rail
        g.color(0xFF1E88E5); g.rect(Math.min(-0.14f * h * f, 0.14f * h * f), top, 0.28f * h, 0.035f * h);
        g.color(0xFF90A4AE); g.line(0.12f * h * f, top, 0.12f * h * f, 0, 0.02f * h);
        // the chute: a curved red slope with raised sides, then a short level run-out on a leg
        g.color(0xFFC62828);
        g.begin(); g.moveTo(0.12f * h * f, top - 0.02f * h); g.cubicTo(0.5f * h * f, top + 0.1f * h, 0.7f * h * f, end - 0.02f * h, len * f, end - 0.02f * h);
        g.lineTo((len + 0.15f * h) * f, end - 0.02f * h); g.lineTo((len + 0.15f * h) * f, end + 0.04f * h); g.lineTo(len * f, end + 0.04f * h);
        g.cubicTo(0.7f * h * f, end + 0.04f * h, 0.5f * h * f, top + 0.16f * h, 0.12f * h * f, top + 0.04f * h); g.close(); g.fillPath();
        g.color(0xFFFFCDD2);
        g.begin(); g.moveTo(0.14f * h * f, top + 0.005f * h); g.cubicTo(0.5f * h * f, top + 0.12f * h, 0.7f * h * f, end + 0.0f, len * f, end);
        g.lineTo((len + 0.14f * h) * f, end); g.lineTo((len + 0.14f * h) * f, end + 0.012f * h); g.lineTo(len * f, end + 0.012f * h);
        g.cubicTo(0.7f * h * f, end + 0.012f * h, 0.5f * h * f, top + 0.135f * h, 0.14f * h * f, top + 0.02f * h); g.close(); g.fillPath();
        g.color(0xFF90A4AE); g.line((len + 0.08f * h) * f, end + 0.04f * h, (len + 0.08f * h) * f, 0, 0.02f * h);
        g.color(alpha(0xFF000000, 0.14f)); g.oval(len * 0.5f * f, 0.005f * h, len * 0.6f, 0.03f * h);
    }

    /** The height of the chute's sliding surface at a distance along it (0 at the top, SLIDE_LEN·h at the end), a share of h. */
    public static float slideHeight(float u) {
        u = Math.max(0, Math.min(1, u));
        float e = u * u * (3 - 2 * u);
        return SLIDE_TOP + (SLIDE_END - SLIDE_TOP) * e;
    }

    /** A see-saw between two children: its fulcrum at x mid, the plank tilted by deg, from x0 to x1 (a share 0.3 of h high). */
    public static void seesaw(Gfx g, float x0, float x1, float h, float deg) {
        float mid = (x0 + x1) / 2, half = Math.abs(x1 - x0) / 2 + 0.12f * h, py = -0.3f * h;
        g.color(0xFF5D4037);
        g.begin(); g.moveTo(mid - 0.1f * h, 0); g.lineTo(mid + 0.1f * h, 0); g.lineTo(mid, py); g.close(); g.fillPath();
        g.save(); g.translate(mid, py); g.rotate(deg);
        g.color(0xFFFFA000); g.roundRect(-half, -0.025f * h, 2 * half, 0.04f * h, 0.015f * h);
        g.color(0xFF616161);
        float hx = Math.abs(x1 - x0) / 2 - 0.12f * h;
        g.line(-hx, -0.02f * h, -hx, -0.14f * h, 0.015f * h); g.line(hx, -0.02f * h, hx, -0.14f * h, 0.015f * h);
        g.line(-hx - 0.03f * h, -0.14f * h, -hx + 0.03f * h, -0.14f * h, 0.018f * h); g.line(hx - 0.03f * h, -0.14f * h, hx + 0.03f * h, -0.14f * h, 0.018f * h);
        g.restore();
        g.color(alpha(0xFF000000, 0.14f)); g.oval(mid, 0.005f * h, half, 0.03f * h);
    }

    /** A merry-go-round: a turning disc with a central pole and a ring to hold (turned by angle). */
    public static void roundabout(Gfx g, float h, float angle, boolean front) {
        float rx = 0.8f * h, ry = 0.13f * h, cy = -0.09f * h;
        if (!front) {
            g.color(alpha(0xFF000000, 0.15f)); g.oval(0, 0.01f * h, rx * 1.05f, ry * 0.7f);
            g.color(0xFF455A64); g.rect(-rx, cy, 2 * rx, 0.06f * h);
            g.color(0xFF1976D2); g.oval(0, cy, rx, ry);
            int[] cols = {0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFFFB8C00};
            for (int i = 0; i < 8; i++) {
                double a0 = angle + i * Math.PI / 4;
                g.color(cols[i % 4]);
                g.line(0, cy, (float) Math.cos(a0) * rx * 0.95f, cy + (float) Math.sin(a0) * ry * 0.95f, 0.012f * h);
            }
            g.color(0xFF90A4AE); g.line(0, cy, 0, -0.95f * h, 0.03f * h);
            g.strokeOval(0, -0.6f * h, rx * 0.8f, ry * 0.8f, 0.012f * h);
            return;
        }
        // the near half of the ring in front of a rider on the near side
        g.color(0xFFB0BEC5);
        g.begin(); g.moveTo(-rx * 0.8f, -0.6f * h); g.quadTo(0, -0.6f * h + ry * 1.6f, rx * 0.8f, -0.6f * h); g.strokePath(0.012f * h);
    }

    // ------------------------------------------------------------------ exercise

    /** An exercise or yoga mat under the feet. */
    public static void mat(Gfx g, float h, int color) {
        g.color(Puppet.shade(color, 0.7f)); g.roundRect(-0.48f * h, -0.006f * h, 0.96f * h, 0.03f * h, 0.01f * h);
        g.color(color); g.roundRect(-0.48f * h, -0.014f * h, 0.96f * h, 0.022f * h, 0.01f * h);
    }

    /**
     * A skipping rope from the left hand to the right hand, turned by angle (0 = over the head, π = under the feet);
     * front = draw it only while it passes in front of the body.
     */
    public static void rope(Gfx g, float lx, float ly, float rx, float ry, float h, double angle, boolean front) {
        boolean inFront = Math.sin(angle) > 0;
        if (inFront != front) return;
        float yc = -0.5f * h, r = 0.56f * h;
        float midY = yc - r * (float) Math.cos(angle);
        float cx = (lx + rx) / 2, cy = 2 * midY - (ly + ry) / 2;
        g.color(0xFFE91E63);
        g.begin(); g.moveTo(lx, ly); g.quadTo(cx, cy, rx, ry); g.strokePath(Math.max(1.2f, 0.01f * h));
    }

    // ------------------------------------------------------------------ a bath, a change of clothes, affection

    /** A bath's tiled corner behind the character, a tap and a bucket (the bucket at the side, outside the curtain). */
    public static void bathCorner(Gfx g, float h, float f) {
        float w = 0.62f * h, top = -1.25f * h;
        g.color(0xFFE1F5FE); g.rect(-w, top, 2 * w, -top);
        g.color(0xFFB3E5FC);
        for (float y = top; y < 0; y += 0.12f * h) g.rect(-w, y, 2 * w, 0.006f * h);
        for (float x = -w; x <= w; x += 0.12f * h) g.rect(x, top, 0.006f * h, -top);
        g.color(0xFF90A4AE); g.rect(-0.04f * h * f - 0.02f * h, -1.05f * h, 0.04f * h, 0.05f * h); g.line(0, -1.02f * h, 0.07f * h * f, -1.02f * h, 0.015f * h);
        float bx = 0.5f * h * f;
        g.color(0xFF1E88E5); g.begin(); g.moveTo(bx - 0.11f * h, -0.26f * h); g.lineTo(bx + 0.11f * h, -0.26f * h); g.lineTo(bx + 0.085f * h, 0); g.lineTo(bx - 0.085f * h, 0); g.close(); g.fillPath();
        g.color(0xFF90CAF9); g.oval(bx, -0.26f * h, 0.11f * h, 0.02f * h);
        g.color(0xFF1565C0); g.strokeOval(bx, -0.26f * h, 0.11f * h, 0.02f * h, 0.006f * h);
    }

    /** A bath curtain on its rod from just below the shoulders (y = top) to the floor; it sways a little. */
    public static void curtain(Gfx g, float h, float top, float t) {
        float w = 0.44f * h;
        g.color(0xFF9E9E9E); g.line(-w - 0.04f * h, top - 0.01f * h, w + 0.04f * h, top - 0.01f * h, 0.012f * h);
        int[] cols = {0xFFF8BBD0, 0xFFF48FB1};
        int n = 8;
        // one solid sheet first (the folds sway on it, never apart: nothing shows through the curtain)
        g.color(cols[0]); g.rect(-w - 0.01f * h, top, 2 * w + 0.02f * h, -top + 0.015f * h);
        for (int i = 0; i < n; i++) {
            float x0 = -w + i * 2 * w / n, sway = (float) Math.sin(t * 1.3f + i * 0.9f) * 0.01f * h;
            g.color(cols[i % 2]);
            g.begin(); g.moveTo(x0, top); g.lineTo(x0 + 2 * w / n, top); g.lineTo(x0 + 2 * w / n + sway, 0.015f * h); g.lineTo(x0 + sway, 0.015f * h); g.close(); g.fillPath();
            g.color(0xFF9E9E9E); g.strokeOval(x0 + w / n, top, 0.012f * h, 0.012f * h, 0.004f * h);
        }
        g.color(alpha(0xFFFFFFFF, 0.5f));
        for (float y = top + 0.12f * h; y < -0.05f * h; y += 0.16f * h)
            for (float x = -w + 0.06f * h; x < w; x += 0.16f * h) g.oval(x, y, 0.02f * h, 0.02f * h);
    }

    /** Bathing outdoors (a river, a pond): the water up to just below the shoulders, its ripples moving. */
    public static void bathWater(Gfx g, float h, float top, float t) {
        float w = 0.6f * h;
        g.linear(0, top, 0, 0.02f * h, 0xFF4FC3F7, 0xFF0277BD);
        g.rect(-w, top, 2 * w, -top + 0.02f * h);
        g.color(alpha(0xFFFFFFFF, 0.45f));
        for (int i = 0; i < 5; i++) {
            float ph = (t * 0.6f + i * 0.2f) % 1f;
            g.strokeOval((float) Math.sin(i * 2.1f) * 0.1f * h, top + 0.01f * h, 0.12f * h + ph * 0.35f * h, 0.012f * h + ph * 0.02f * h, 0.006f * h * (1 - ph) + 0.5f);
        }
    }

    /** A mug of water held up and poured over the head, in a hand of the character's skin (for a picture's arm). */
    public static void mugPour(Gfx g, float x, float y, float h, float f, float t, int skin, float headTop) {
        float k = h / 160f;
        Puppet.drawTool(g, Pose.I_MUG, x, y, k, f, t, 1e6f);
        g.color(skin); g.roundRect(x - 5 * k, y + 2 * k, 10 * k, 7 * k, 3 * k);
        // the water falls on the head and runs off it in drops
        g.color(alpha(0xFF90CAF9, 0.55f));
        for (int i = 0; i < 4; i++) g.line(x + f * (6 + i) * k, y - 4 * k, x + f * (4 + i * 2) * k - f * 4 * k, headTop, 1.2f * k);
        for (int i = 0; i < 8; i++) {
            float ph = (t * 2.2f + i * 0.13f) % 1f, dx = ((i * 37) % 11 - 5) * 3.2f * k;
            g.color(alpha(0xFFBBDEFB, 0.85f * (1 - ph)));
            g.oval(x - f * 10 * k + dx, headTop + ph * 0.3f * h, 1.4f * k, 1.9f * k);
        }
    }

    /** A towel round the shoulders (y = the shoulders, w = their half width), after a bath. */
    public static void towel(Gfx g, float sy, float w, float h, float a) {
        g.save(); g.setAlpha(a);
        g.color(0xFFFFFFFF);
        g.begin(); g.moveTo(-w * 1.15f, sy + 0.01f * h); g.quadTo(0, sy - 0.05f * h, w * 1.15f, sy + 0.01f * h); g.lineTo(w * 1.05f, sy + 0.05f * h); g.quadTo(0, sy + 0.01f * h, -w * 1.05f, sy + 0.05f * h); g.close(); g.fillPath();
        g.roundRect(-w * 1.1f, sy, w * 0.42f, 0.2f * h, 0.02f * h);
        g.roundRect(w * 0.68f, sy, w * 0.42f, 0.2f * h, 0.02f * h);
        g.color(0xFF4FC3F7);
        g.rect(-w * 1.1f, sy + 0.16f * h, w * 0.42f, 0.015f * h); g.rect(w * 0.68f, sy + 0.16f * h, w * 0.42f, 0.015f * h);
        g.restore();
    }

    /** A three-panel folding screen to change behind: up to y = top (above the shoulders), clothes hung over it. */
    public static void screen(Gfx g, float h, float top, int clothes, float t) {
        float w = 0.5f * h, pw = 2 * w / 3;
        g.color(0xFF6D4C41); g.rect(-w, top, 2 * w, -top + 0.01f * h);
        for (int i = 0; i < 3; i++) {
            float x0 = -w + i * pw, skew = i == 1 ? 0 : 0.015f * h;
            g.color(0xFF6D4C41); g.rect(x0, top - skew, pw, -top + skew);
            g.color(i == 1 ? 0xFFFFF3E0 : 0xFFFFE0B2); g.rect(x0 + 0.02f * h, top - skew + 0.02f * h, pw - 0.04f * h, -top + skew - 0.06f * h);
            g.color(0xFFE57373);
            float cx = x0 + pw / 2, cy = top + (-top) * 0.4f;
            for (int p = 0; p < 5; p++) { double an = p * Math.PI * 2 / 5; g.oval(cx + (float) Math.cos(an) * 0.03f * h, cy + (float) Math.sin(an) * 0.03f * h, 0.02f * h, 0.02f * h); }
            g.color(0xFF81C784); g.line(cx, cy + 0.04f * h, cx, cy + 0.16f * h, 0.008f * h);
            g.color(0xFF5D4037); g.rect(x0 + 0.02f * h, -0.05f * h, 0.03f * h, 0.05f * h); g.rect(x0 + pw - 0.05f * h, -0.05f * h, 0.03f * h, 0.05f * h);
        }
        // clothes thrown over the top edge
        if (clothes != 0) {
            float sway = (float) Math.sin(t * 1.7f) * 0.008f * h;
            g.color(clothes);
            g.begin(); g.moveTo(w * 0.1f, top - 0.005f * h); g.lineTo(w * 0.55f, top - 0.005f * h); g.lineTo(w * 0.5f + sway, top + 0.16f * h); g.lineTo(w * 0.2f + sway, top + 0.12f * h); g.close(); g.fillPath();
        }
    }

    /** A small heart rising from a peck on the cheek. */
    public static void heart(Gfx g, float x, float y, float r, float a) {
        g.color(alpha(0xFFE91E63, a));
        g.oval(x - r * 0.5f, y, r * 0.55f, r * 0.55f); g.oval(x + r * 0.5f, y, r * 0.55f, r * 0.55f);
        g.begin(); g.moveTo(x - r * 1.02f, y + r * 0.15f); g.lineTo(x + r * 1.02f, y + r * 0.15f); g.lineTo(x, y + r * 1.25f); g.close(); g.fillPath();
    }
}
