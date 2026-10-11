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

    // ================================================================== v43: a room's door

    /**
     * v43: a room's door in the side wall at the edge of the picture (side -1 the left edge, +1 the right; ground the
     * floor line; dh the door's height). Seen steeply, the doorway is narrow; open 0..1 swings the door away into the
     * passage (its free edge going back to the hinge) and the lit passage behind it shows. front = false: the doorway and the door
     * (behind the people); front = true: the near post of its frame (in front of whoever goes through it).
     */
    public static void roomDoor(Gfx g, float side, float ground, float dh, float open, boolean front) {
        g.save();
        if (side > 0) { g.translate(1280, 0); g.scale(-1, 1); }       // the right edge: the left one mirrored
        float nx = 12, fx = 74;                                         // the near and the far post of the doorway
        float nTop = ground - dh * 1.03f, nBot = ground + 6, fTop = ground - dh * 0.95f, fBot = ground - 6;
        if (front) {
            g.color(0xFF6D4C3D); g.rect(0, nTop - 16, nx, nBot - nTop + 20);
            g.color(alpha(0xFFFFFFFF, 0.12f)); g.rect(nx - 4, nTop - 16, 3, nBot - nTop + 20);
            g.restore();
            return;
        }
        // the passage behind the doorway, lit from the next room (seen as the door swings away)
        g.color(0xFF4A3A2E);
        g.begin(); g.moveTo(nx, nTop); g.lineTo(fx, fTop); g.lineTo(fx, fBot); g.lineTo(nx, nBot); g.close(); g.fillPath();
        g.color(alpha(0xFFFFE0B2, 0.28f));
        g.begin(); g.moveTo(nx + (fx - nx) * 0.35f, nTop + (fTop - nTop) * 0.35f + 20); g.lineTo(fx, fTop + 20); g.lineTo(fx, fBot - 30); g.lineTo(nx + (fx - nx) * 0.35f, nBot + (fBot - nBot) * 0.35f - 30); g.close(); g.fillPath();
        g.color(alpha(0xFF8D6E63, 0.7f));
        g.begin(); g.moveTo(nx, nBot - 30); g.lineTo(fx, fBot - 22); g.lineTo(fx, fBot); g.lineTo(nx, nBot); g.close(); g.fillPath();
        // the far post and the lintel
        g.color(0xFF6D4C3D);
        g.rect(fx, fTop - 10, 9, fBot - fTop + 10);
        g.begin(); g.moveTo(0, nTop - 16); g.lineTo(fx + 9, fTop - 12); g.lineTo(fx + 9, fTop - 2); g.lineTo(0, nTop - 4); g.close(); g.fillPath();
        // the door, hinged on the far post: shut it lies across the doorway; opening, it swings away into the passage —
        // its free edge going back to the hinge, the doorway opening up beside it
        float o = Math.max(0, Math.min(1, open));
        double th = Math.toRadians(88 * o);
        float c = (float) Math.cos(th), sn = (float) Math.sin(th);
        float k = 1 - c;                                   // how far the free edge has gone back towards the far post
        float freeX = nx + (fx - nx) * k, freeTop = nTop + (fTop - nTop) * k, freeBot = nBot + (fBot - nBot) * k;
        g.color(Puppet.shade(0xFF8D5A3B, 1f - 0.35f * sn));
        g.begin(); g.moveTo(fx, fTop); g.lineTo(freeX, freeTop); g.lineTo(freeX, freeBot); g.lineTo(fx, fBot); g.close(); g.fillPath();
        // its edge, seen as it turns
        if (o > 0.05f) { g.color(0xFF4E342E); g.line(freeX, freeTop, freeX, freeBot, 2 + 3 * sn); }
        // its panels and its handle near the free edge (fading as the door turns away)
        if (fx - freeX > 8) {
            g.color(alpha(0xFF4E342E, 0.55f * (1 - sn)));
            for (int i = 0; i < 2; i++) {
                float a0 = 0.18f + i * 0.42f, a1 = a0 + 0.32f;
                float x0 = fx + (freeX - fx) * 0.22f, x1 = fx + (freeX - fx) * 0.8f;
                float y00 = fTop + (fBot - fTop) * a0, y01 = freeTop + (freeBot - freeTop) * a0, y10 = fTop + (fBot - fTop) * a1, y11 = freeTop + (freeBot - freeTop) * a1;
                g.begin(); g.moveTo(x0, y00 + (y01 - y00) * 0.22f); g.lineTo(x1, y00 + (y01 - y00) * 0.8f); g.lineTo(x1, y10 + (y11 - y10) * 0.8f); g.lineTo(x0, y10 + (y11 - y10) * 0.22f); g.close(); g.strokePath(2);
            }
            float hx = fx + (freeX - fx) * 0.86f, hy = fTop + (fBot - fTop) * 0.52f + ((freeTop + (freeBot - freeTop) * 0.52f) - (fTop + (fBot - fTop) * 0.52f)) * 0.86f;
            g.color(alpha(0xFFFFD54F, 1 - 0.8f * sn)); g.oval(hx, hy, 4, 4);
        }
        g.restore();
    }

    // ================================================================== v43: the kitchen

    /**
     * v43: what a cook does at this point of the cooking (kind Film.COOK_*, done 0..1): 0 chops on the board, 1 stirs the
     * pot, 2 rolls a roti, 3 serves (pours, plates), 4 tends the stove (the cooker whistling, the roti on the tawa).
     */
    public static int cookPhase(int kind, float done) {
        switch (kind) {
            case Film.COOK_MEAL: return done < 0.3f ? 0 : done < 0.85f ? 1 : 3;
            case Film.COOK_DAL: return done < 0.75f ? 4 : 3;
            case Film.COOK_TEA: return done < 0.75f ? 1 : 3;
            case Film.COOK_ROTI: return done < 0.4f ? 2 : done < 0.8f ? 4 : 3;
            case Film.COOK_CHOP: return done < 0.85f ? 0 : 3;
            case Film.COOK_TADKA: return done < 0.65f ? 1 : 3;
            default: return done < 0.85f ? 1 : 3;
        }
    }

    /**
     * v43: a kitchen counter with a gas hob (its centre at the origin, the floor at y = 0; s = the cook's height, who
     * stands on the side -f): the meal made on it in stages — vegetables chopped on the board, the kadhai or the
     * pressure cooker or the tea pan on the flame, rotis rolled and puffed on the tawa — then the dish served on the
     * counter. done 0..1 is how far the cooking has gone; after it (since >= 0, seconds since it was done) the dish
     * stays there, the flame off, its steam dying away.
     */
    public static void kitchen(Gfx g, float s, float f, float t, int kind, float done, float since) { kitchen(g, s, f, t, kind, done, since, false, 0, false); }

    /** v43: the same; idle: the counter before anything is cooked on it (the hob cold, the kadhai empty on it). */
    public static void kitchen(Gfx g, float s, float f, float t, int kind, float done, float since, boolean idle) { kitchen(g, s, f, t, kind, done, since, idle, 0, false); }

    /**
     * v43: the same, the cook standing behind the counter (it hides them from the hips down, as at a real kitchen
     * counter or a cooking show): hand != 0 is the cook's skin, and their hands are seen at the work on the counter
     * top — the knife rocking on the board, the ladle going round the kadhai, the belan rolling the roti, the chimta
     * turning it on the tawa, the serving spoon to the plate; outdoor: a wooden stall with a gas cylinder under it.
     */
    public static void kitchen(Gfx g, float s, float f, float t, int kind, float done, float since, boolean idle, int hand, boolean outdoor) {
        kitchen(g, s, f, t, kind, done, since, idle, hand, outdoor, 0.54f);
    }

    /** v43: the same, the counter's top at topFrac of the cook's height (just above where their hands hang). */
    public static void kitchen(Gfx g, float s, float f, float t, int kind, float done, float since, boolean idle, int hand, boolean outdoor, float topFrac) {
        f = f < 0 ? -1 : 1;
        boolean cooking = since < 0 && !idle;
        if (since >= 0) done = 1;
        float top = -Math.max(0.34f, Math.min(0.58f, topFrac)) * s, w = 0.56f * s, sx = f * 0.12f * s, bx = -f * 0.14f * s;
        int phase = cookPhase(kind, done);
        if (outdoor) {
            // a street stall: a plank top on four legs, a cloth skirt at the front, the red gas cylinder beneath
            g.color(0xFF6D4C41); g.rect(-w / 2 + 0.02f * s, top, 0.03f * s, -top); g.rect(w / 2 - 0.05f * s, top, 0.03f * s, -top);
            float cyl = Math.min(0.24f * s, -top - 0.17f * s);
            g.color(0xFFC62828); g.roundRect(-0.07f * s, -cyl, 0.12f * s, cyl, 0.04f * s);
            g.color(0xFF8E1B1B); g.rect(-0.03f * s, -cyl - 0.03f * s, 0.04f * s, 0.035f * s);
            g.color(0xFF5D4037); g.rect(-w / 2, -0.12f * s, w, 0.02f * s);
            g.color(0xFFE6B85C); g.rect(-w / 2, top, w, 0.16f * s);
            g.color(alpha(0xFF8D6E63, 0.5f)); for (int i = 0; i < 6; i++) g.rect(-w / 2 + (i + 0.5f) * w / 6 - 0.004f * s, top, 0.008f * s, 0.16f * s);
            g.color(0xFF8D6E63); g.rect(-w / 2 - 0.012f * s, top - 0.022f * s, w + 0.024f * s, 0.026f * s);
            g.color(alpha(0xFFFFFFFF, 0.2f)); g.rect(-w / 2 - 0.012f * s, top - 0.022f * s, w + 0.024f * s, 0.005f * s);
        } else {
            // the cabinet: two doors with their knobs, a drawer, a plinth
            g.color(0xFF8D6E63); g.rect(-w / 2, top, w, -top);
            g.color(0xFF7B5E52);
            g.rect(-w / 2 + 0.015f * s, top + 0.1f * s, w / 2 - 0.022f * s, -top - 0.13f * s);
            g.rect(0.007f * s, top + 0.1f * s, w / 2 - 0.022f * s, -top - 0.13f * s);
            g.rect(-w / 2 + 0.015f * s, top + 0.02f * s, w - 0.03f * s, 0.065f * s);
            g.color(0xFFD7CCC8); g.oval(-0.025f * s, top * 0.45f, 0.008f * s, 0.008f * s); g.oval(0.025f * s, top * 0.45f, 0.008f * s, 0.008f * s);
            g.roundRect(-0.04f * s, top + 0.045f * s, 0.08f * s, 0.012f * s, 0.005f * s);
            g.color(0xFF4E342E); g.rect(-w / 2, -0.03f * s, w, 0.03f * s);
            // the granite top
            g.color(0xFF78909C); g.rect(-w / 2 - 0.012f * s, top - 0.022f * s, w + 0.024f * s, 0.026f * s);
            g.color(alpha(0xFFFFFFFF, 0.25f)); g.rect(-w / 2 - 0.012f * s, top - 0.022f * s, w + 0.024f * s, 0.005f * s);
        }
        float ct = top - 0.022f * s;                       // the counter's surface
        // the hob: two burners, the near one lit while something cooks on it
        g.color(0xFF263238); g.roundRect(sx - 0.12f * s, ct - 0.022f * s, 0.24f * s, 0.022f * s, 0.006f * s);
        g.color(0xFF9E9E9E); g.oval(sx - 0.085f * s, ct - 0.011f * s, 0.008f * s, 0.006f * s); g.oval(sx + 0.085f * s, ct - 0.011f * s, 0.008f * s, 0.006f * s);
        float hob = ct - 0.022f * s;
        boolean flameOn = cooking && phase != 0 && phase != 2 && !(phase == 3 && done > 0.92f);
        if (flameOn) Nature.flame(g, sx, hob, t, s / 1000f, 0, 0, 7);
        if (idle) { kadhai(g, sx, hob, s, f, 0, 0, t, false); return; }
        switch (kind) {
            case Film.COOK_TEA: {
                // a pan of milk on the flame: the tea leaves and sugar go in, it turns brown, boils up; poured into two cups
                float boil = Rig.smooth(0.45f, 0.6f, done) * (1 - Rig.smooth(0.72f, 0.78f, done));
                int col = mix(0xFFF5F5F0, 0xFFB07A4A, Rig.smooth(0.1f, 0.5f, done));
                float level = cooking ? 1 - 0.7f * Rig.smooth(0.78f, 0.95f, done) : 0.3f;
                pan(g, sx, hob, s, f, col, level, boil, t);
                if (done > 0.75f) cups(g, bx, ct, s, f, col, Rig.smooth(0.78f, 0.95f, done), t, since);
                break;
            }
            case Film.COOK_DAL: {
                // the pressure cooker on the flame: it whistles; then dal in a steel bowl
                float whistle = cooking ? Rig.smooth(0.5f, 0.53f, done) * (1 - Rig.smooth(0.6f, 0.64f, done)) : 0;
                cooker(g, sx, hob, s, f, t, whistle);
                if (done > 0.75f) bowl(g, bx, ct, s, 0xFFF2B33D, Rig.smooth(0.75f, 0.85f, done), t, since, false);
                break;
            }
            case Film.COOK_ROTI: {
                // a ball of dough rolled out on the chakla, then on the tawa: brown spots, it puffs up; a stack on a plate
                tawa(g, sx, hob, s);
                float roll = Rig.smooth(0.0f, 0.38f, done);
                if (done < 0.42f) chakla(g, bx, ct, s, roll);
                else if (done < 0.82f) {
                    chakla(g, bx, ct, s, -1);
                    float cookU = (done - 0.42f) / 0.4f;
                    roti(g, sx, hob - 0.012f * s, s, Rig.smooth(0.15f, 0.7f, cookU), Rig.smooth(0.5f, 0.85f, cookU) * (1 - Rig.smooth(0.92f, 1f, cookU)));
                }
                if (done >= 0.8f) {
                    chakla(g, bx, ct, s, -1);
                    rotiStack(g, bx + f * 0.02f * s, ct, s, 1 + (int) (Rig.smooth(0.8f, 1f, done) * 4), t, since);
                }
                break;
            }
            case Film.COOK_CHOP: {
                board(g, bx, ct, s, f, Rig.smooth(0, 0.85f, done), done < 0.85f);
                if (done > 0.85f) bowl(g, bx + f * 0.06f * s, ct, s, 0xFFE57373, Rig.smooth(0.85f, 0.95f, done), t, -1, true);
                break;
            }
            case Film.COOK_TADKA: {
                // a small pan of hot oil: the mustard seeds splutter; poured over the dal in its bowl
                bowl(g, bx, ct, s, 0xFFF2B33D, 1, t, since, false);
                float pour = Rig.smooth(0.65f, 0.85f, done);
                tadkaPan(g, sx, hob, s, f, t, cooking && done < 0.7f, pour);
                if (done > 0.8f) {
                    // the tadka on the dal: red chillies and seeds on top
                    g.color(0xFFB71C1C); g.oval(bx - 0.02f * s, ct - 0.052f * s, 0.008f * s, 0.003f * s);
                    g.color(0xFF3E2723); for (int i = 0; i < 6; i++) g.oval(bx + (i - 3) * 0.009f * s, ct - 0.05f * s - (i % 2) * 0.002f * s, 0.0025f * s, 0.0025f * s);
                }
                break;
            }
            default: {
                // a sabzi: vegetables chopped on the board (a whole meal starts there), into the kadhai and stirred on the
                // flame, turning from raw green to a cooked orange-brown; served on a steel plate
                boolean meal = kind == Film.COOK_MEAL;
                float c0 = meal ? 0.3f : 0f;
                if (meal && done < 0.34f) board(g, bx, ct, s, f, Rig.smooth(0, 0.3f, done), true);
                float cookU = Math.max(0, (done - c0) / (0.85f - c0));
                int col = mix(0xFF9CCC65, 0xFFD2691E, Rig.smooth(0.1f, 0.85f, cookU));
                boolean inPot = done >= c0;
                kadhai(g, sx, hob, s, f, inPot ? col : 0, inPot ? (cooking ? 1 - 0.75f * Rig.smooth(0.86f, 0.97f, done) : 0.25f) : 0, t, cooking && inPot && done < 0.9f);
                if (done > 0.85f) plate(g, bx, ct, s, col, Rig.smooth(0.86f, 0.97f, done), t, since);
            }
        }
        if (cooking && hand != 0) cookHands(g, kind, phase, done, sx, bx, hob, ct, s, f, t, hand);
    }

    /**
     * v43: the cook's hands at the work on the counter (the cook behind it): each with its forearm coming from the
     * body behind the counter's far edge, holding the tool of the moment.
     */
    static void cookHands(Gfx g, int kind, int phase, float done, float sx, float bx, float hob, float ct, float s, float f, float t, int skin) {
        float elbowY = ct - 0.075f * s, el = 0.15f * s;           // the elbows at the body's sides, a little above the top
        switch (phase) {
            case 0: {
                // chopping: one hand steadies the vegetables on the board, the other rocks the knife on its tip
                float lift = 8 + 24 * (float) Math.abs(Math.sin(t * 9));
                float tx = bx + f * 0.02f * s, ty = ct - 0.018f * s;
                hand(g, bx + f * 0.06f * s, ct - 0.03f * s, s, skin, f * el * 0.4f, elbowY, false);
                g.save(); g.translate(tx, ty); g.rotate(f * lift);
                g.color(0xFFCFD8DC);
                g.begin(); g.moveTo(0, 0); g.lineTo(-f * 0.085f * s, 0); g.lineTo(-f * 0.085f * s, -0.02f * s); g.lineTo(-f * 0.02f * s, -0.014f * s); g.close(); g.fillPath();
                g.color(0xFF90A4AE); g.line(0, 0, -f * 0.085f * s, 0, 0.003f * s);
                g.color(0xFF3E2723); g.roundRect(f > 0 ? -0.135f * s : 0.085f * s, -0.017f * s, 0.05f * s, 0.013f * s, 0.005f * s);
                float hx = -f * 0.11f * s, hy = -0.011f * s;
                g.restore();
                // the knife hand at the handle (where the handle is after the rock)
                double a = Math.toRadians(f * lift);
                float wx = tx + (float) (hx * Math.cos(a) - hy * Math.sin(a)), wy = ty + (float) (hx * Math.sin(a) + hy * Math.cos(a));
                hand(g, wx, wy, s, skin, -f * el, elbowY, true);
                break;
            }
            case 1: case 3: {
                if (kind == Film.COOK_TADKA) {
                    // the small pan held by its handle (tipped over the dal as it is poured)
                    float pour = Rig.smooth(0.65f, 0.85f, done);
                    float r = 0.05f * s, top = hob - 0.05f * s, px = sx - f * (r + 0.07f * s), py = top + 0.006f * s;
                    double a = Math.toRadians(-f * 50 * pour);
                    float ox = sx, oy = hob - 0.03f * s, rx = px - ox, ry = py - oy;
                    hand(g, ox + (float) (rx * Math.cos(a) - ry * Math.sin(a)), oy + (float) (rx * Math.sin(a) + ry * Math.cos(a)), s, skin, f * el * 0.3f, elbowY, true);
                    break;
                }
                // stirring round the pot (or, serving, the spoon carried from the pot to the plate or the bowl)
                boolean serve = phase == 3;
                float u = serve ? Rig.smooth(0, 1, (float) ((t * 0.7f) % 1f)) : 0;
                float px = serve ? sx + (bx - sx) * u : sx + 0.05f * s * (float) Math.cos(t * 4.6f);
                float py = serve ? hob - 0.07f * s - 0.06f * s * (float) Math.sin(Math.PI * u) : hob - 0.05f * s + 0.008f * s * (float) Math.sin(t * 4.6f);
                if (kind == Film.COOK_TEA && !serve) py = hob - 0.06f * s;
                float ex = px - f * 0.045f * s, ey = py - 0.1f * s;          // the handle's end, towards the cook
                g.color(0xFF8D6E63); g.line(px, py, ex, ey, 0.009f * s);
                g.color(0xFFB0BEC5); g.oval(px, py + 0.004f * s, 0.016f * s, 0.008f * s);
                hand(g, ex, ey + 0.012f * s, s, skin, f * el, elbowY, true);
                if (!serve && kind != Film.COOK_TEA) hand(g, sx - f * 0.13f * s, hob - 0.05f * s, s, skin, -f * el, elbowY, true);   // the other steadies the kadhai's handle
                break;
            }
            case 2: {
                // rolling: the belan rolls back and forth over the roti, a hand at each end
                float rx = bx + 0.028f * s * (float) Math.sin(t * 4.5f), ry = ct - 0.034f * s;
                g.color(0xFFD7A86E); g.roundRect(rx - 0.065f * s, ry - 0.007f * s, 0.13f * s, 0.014f * s, 0.007f * s);
                g.color(0xFFA1887F); g.rect(rx - 0.085f * s, ry - 0.004f * s, 0.02f * s, 0.008f * s); g.rect(rx + 0.065f * s, ry - 0.004f * s, 0.02f * s, 0.008f * s);
                hand(g, rx - 0.075f * s, ry - 0.004f * s, s, skin, -el, elbowY, true);
                hand(g, rx + 0.075f * s, ry - 0.004f * s, s, skin, el, elbowY, true);
                break;
            }
            case 4: {
                if (kind == Film.COOK_ROTI) {
                    // the chimta (tongs) turning the roti on the tawa
                    float jx = sx - f * 0.04f * s + 0.01f * s * (float) Math.sin(t * 3), jy = hob - 0.016f * s;
                    float ex = jx - f * 0.08f * s, ey = jy - 0.07f * s;
                    g.color(0xFF78909C); g.line(jx, jy, ex, ey, 0.004f * s); g.line(jx + 0.008f * s, jy, ex + 0.003f * s, ey, 0.004f * s);
                    hand(g, ex, ey, s, skin, -f * el, elbowY, true);
                } else {
                    // the cooker: a hand on its long handle, waiting for the whistle
                    hand(g, sx - f * 0.13f * s, hob - 0.135f * s, s, skin, -f * el, elbowY, true);
                }
                break;
            }
            default:
        }
    }

    /**
     * v43: a hand at (x, y) of a cook seen from the front, gripping (a fist round a handle) or flat (steadying), with
     * its forearm going back to the elbow at (ex, ey) — the body behind the counter.
     */
    static void hand(Gfx g, float x, float y, float s, int skin, float ex, float ey, boolean grip) {
        int dark = Puppet.shade(skin, 0.82f);
        float fw = 0.025f * s;
        // the forearm, a little narrower at the wrist
        float dx = ex - x, dy = ey - y, len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len > 1) {
            float nx = -dy / len, ny = dx / len;
            g.color(dark);
            g.begin(); g.moveTo(x + nx * fw * 0.8f, y + ny * fw * 0.8f); g.lineTo(ex + nx * fw * 1.1f, ey + ny * fw * 1.1f);
            g.lineTo(ex - nx * fw * 1.1f, ey - ny * fw * 1.1f); g.lineTo(x - nx * fw * 0.8f, y - ny * fw * 0.8f); g.close(); g.fillPath();
            g.color(skin);
            g.begin(); g.moveTo(x + nx * fw * 0.6f, y + ny * fw * 0.6f); g.lineTo(ex + nx * fw * 0.9f, ey + ny * fw * 0.9f);
            g.lineTo(ex - nx * fw * 0.9f, ey - ny * fw * 0.9f); g.lineTo(x - nx * fw * 0.6f, y - ny * fw * 0.6f); g.close(); g.fillPath();
        }
        g.color(dark); g.oval(x, y, 0.022f * s, (grip ? 0.019f : 0.014f) * s);
        g.color(skin); g.oval(x, y - 0.001f * s, 0.019f * s, (grip ? 0.016f : 0.012f) * s);
        g.color(dark);
        if (grip) for (int i = 0; i < 3; i++) g.line(x - 0.012f * s + i * 0.009f * s, y + 0.004f * s, x - 0.01f * s + i * 0.009f * s, y + 0.011f * s, 0.0025f * s);
        else for (int i = 0; i < 3; i++) g.line(x - 0.01f * s + i * 0.009f * s, y + 0.002f * s, x - 0.012f * s + i * 0.01f * s, y + 0.012f * s, 0.0025f * s);
    }

    /** v43: steam rising (strength 0..1). */
    static void steam(Gfx g, float x, float y, float s, float t, float k, int n) {
        if (k <= 0.01f) return;
        for (int i = 0; i < n; i++) {
            float ph = (t * 0.45f + i / (float) n) % 1f;
            g.color(alpha(0xFFFFFFFF, 0.32f * k * (1 - ph)));
            g.oval(x + (i - (n - 1) / 2f) * 0.025f * s + (float) Math.sin(t * 2 + i * 1.7f) * 0.012f * s, y - ph * 0.13f * s, (0.014f + ph * 0.02f) * s, (0.012f + ph * 0.016f) * s);
        }
    }

    /** v43: the kadhai on the hob, its food (col, 0: empty) to fill (0..1), bubbling and steaming while it cooks. */
    static void kadhai(Gfx g, float x, float hob, float s, float f, int col, float fill, float t, boolean hot) {
        float r = 0.105f * s, rim = hob - 0.055f * s;
        g.color(0xFF37474F); g.line(x - r - 0.018f * s, rim + 0.006f * s, x - r + 0.004f * s, rim + 0.012f * s, 0.012f * s); g.line(x + r + 0.018f * s, rim + 0.006f * s, x + r - 0.004f * s, rim + 0.012f * s, 0.012f * s);
        g.color(0xFF455A64);
        g.begin(); g.moveTo(x - r, rim); g.quadTo(x, hob + 0.012f * s, x + r, rim); g.close(); g.fillPath();
        if (col != 0 && fill > 0.02f) {
            g.color(col); g.oval(x, rim + 0.004f * s, r * 0.86f, 0.018f * s * fill + 0.004f * s);
            if (hot) for (int i = 0; i < 4; i++) {
                float ph = (t * 1.3f + i * 0.31f) % 1f;
                g.color(alpha(Puppet.shade(col, 1.25f), 0.7f * (1 - ph))); g.oval(x + (i - 1.5f) * 0.04f * s, rim + 0.002f * s - ph * 0.006f * s, 0.006f * s, 0.004f * s);
            }
        }
        g.color(0xFF263238); g.line(x - r, rim, x + r, rim, 0.006f * s);
        steam(g, x, rim - 0.01f * s, s, t, hot ? 0.9f : 0, 3);
    }

    /** v43: a steel pan with a long handle (the tea), its liquid col filled to level, boiling up (0..1). */
    static void pan(Gfx g, float x, float hob, float s, float f, int col, float level, float boil, float t) {
        float r = 0.075f * s, top = hob - 0.075f * s;
        g.color(0xFF263238); g.line(x - f * r, top + 0.015f * s, x - f * (r + 0.1f * s), top + 0.005f * s, 0.012f * s);       // the handle, towards the cook
        g.color(0xFFB0BEC5); g.rect(x - r, top, 2 * r, hob - top);
        g.color(alpha(0xFFFFFFFF, 0.35f)); g.rect(x - r + 0.01f * s, top + 0.005f * s, 0.012f * s, hob - top - 0.01f * s);
        if (level > 0.02f) {
            float ly = top + (1 - level) * 0.045f * s + 0.008f * s - boil * 0.012f * s;
            g.color(col); g.oval(x, ly, r * 0.92f, 0.012f * s);
            if (boil > 0.05f) for (int i = 0; i < 6; i++) {
                float ph = (t * 2.1f + i * 0.17f) % 1f;
                g.color(alpha(Puppet.shade(col, 1.2f), boil * (1 - ph))); g.oval(x + (i - 2.5f) * 0.022f * s, ly - ph * 0.01f * s, 0.006f * s * (1 + boil), 0.005f * s);
            }
        }
        g.color(0xFF90A4AE); g.line(x - r, top, x + r, top, 0.005f * s);
        steam(g, x, top - 0.01f * s, s, t, 0.4f + 0.6f * boil, 3);
    }

    /** v43: two cups on the counter, filled (0..1) with col, steaming. */
    static void cups(Gfx g, float x, float ct, float s, float f, int col, float fill, float t, float since) {
        for (int i = 0; i < 2; i++) {
            float cx = x + (i - 0.5f) * 0.05f * s;
            g.color(0xFFF5F2EA); g.roundRect(cx - 0.016f * s, ct - 0.036f * s, 0.032f * s, 0.036f * s, 0.006f * s);
            g.color(0xFFE0DCD0); g.strokeOval(cx + 0.02f * s, ct - 0.02f * s, 0.007f * s, 0.009f * s, 0.003f * s);
            if (fill > 0.02f) { g.color(col); g.oval(cx, ct - 0.034f * s, 0.014f * s, 0.004f * s * fill + 0.001f * s); }
        }
        steam(g, x, ct - 0.05f * s, s, t, fill * (since < 0 ? 1 : Math.max(0, 1 - since / 10f)), 2);
    }

    /** v43: a steel bowl on the counter with food col (dal: smooth; chopped vegetables: pieces), shown 0..1. */
    static void bowl(Gfx g, float x, float ct, float s, int col, float shown, float t, float since, boolean pieces) {
        if (shown <= 0.01f) return;
        g.save(); g.setAlpha(Math.min(1, shown * 1.5f));
        float r = 0.045f * s;
        g.color(0xFFB0BEC5);
        g.begin(); g.moveTo(x - r, ct - 0.035f * s); g.quadTo(x, ct + 0.008f * s, x + r, ct - 0.035f * s); g.close(); g.fillPath();
        g.color(col); g.oval(x, ct - 0.037f * s, r * 0.88f, 0.008f * s);
        if (pieces) { g.color(0xFF7CB342); for (int i = 0; i < 5; i++) g.oval(x + (i - 2) * 0.012f * s, ct - 0.04f * s, 0.004f * s, 0.003f * s); }
        g.color(0xFF78909C); g.line(x - r, ct - 0.035f * s, x + r, ct - 0.035f * s, 0.003f * s);
        g.restore();
        if (!pieces) steam(g, x, ct - 0.05f * s, s, t, shown * (since < 0 ? 0.8f : Math.max(0, 0.8f - since / 12f)), 2);
    }

    /** v43: a steel plate (thali) with the cooked sabzi, shown 0..1. */
    static void plate(Gfx g, float x, float ct, float s, int col, float shown, float t, float since) {
        if (shown <= 0.01f) return;
        g.save(); g.setAlpha(Math.min(1, shown * 1.5f));
        g.color(0xFFCFD8DC); g.oval(x, ct - 0.006f * s, 0.07f * s, 0.012f * s);
        g.color(0xFF90A4AE); g.strokeOval(x, ct - 0.006f * s, 0.07f * s, 0.012f * s, 0.003f * s);
        g.color(col); g.oval(x - 0.01f * s, ct - 0.014f * s, 0.032f * s, 0.012f * s);
        g.color(Puppet.shade(col, 0.8f)); g.oval(x - 0.016f * s, ct - 0.016f * s, 0.01f * s, 0.005f * s);
        g.color(0xFFF3E0C0); g.oval(x + 0.035f * s, ct - 0.012f * s, 0.022f * s, 0.008f * s);       // a roti beside it
        g.restore();
        steam(g, x, ct - 0.03f * s, s, t, shown * (since < 0 ? 0.8f : Math.max(0, 0.8f - since / 12f)), 2);
    }

    /** v43: the pressure cooker: its weight jiggles as the steam builds, then it whistles (a jet of steam). */
    static void cooker(Gfx g, float x, float hob, float s, float f, float t, float whistle) {
        float r = 0.07f * s, top = hob - 0.13f * s;
        g.color(0xFFB0BEC5); g.roundRect(x - r, top, 2 * r, hob - top, 0.012f * s);
        g.color(alpha(0xFFFFFFFF, 0.35f)); g.rect(x - r + 0.012f * s, top + 0.01f * s, 0.014f * s, hob - top - 0.02f * s);
        g.color(0xFF90A4AE); g.roundRect(x - r * 0.9f, top - 0.012f * s, 1.8f * r, 0.014f * s, 0.006f * s);
        // the long handle on the lid towards the cook, the short one on the far side
        g.color(0xFF212121); g.roundRect(f > 0 ? x - r - 0.09f * s : x + r, top - 0.008f * s, 0.09f * s, 0.012f * s, 0.004f * s);
        g.roundRect(f > 0 ? x + r : x - r - 0.07f * s, top + 0.03f * s, 0.07f * s, 0.01f * s, 0.004f * s);
        float jig = (float) Math.sin(t * 40) * 0.004f * s * (0.3f + whistle);
        g.color(0xFF424242); g.roundRect(x - 0.012f * s, top - 0.04f * s + jig, 0.024f * s, 0.03f * s, 0.006f * s);
        if (whistle > 0.02f) for (int i = 0; i < 7; i++) {
            float ph = (t * 3 + i / 7f) % 1f;
            g.color(alpha(0xFFFFFFFF, 0.5f * whistle * (1 - ph)));
            g.oval(x + (float) Math.sin(t * 9 + i) * 0.01f * s, top - 0.05f * s - ph * 0.22f * s, (0.01f + ph * 0.03f) * s, (0.01f + ph * 0.025f) * s);
        }
        else steam(g, x, top - 0.045f * s, s, t, 0.25f, 2);
    }

    /** v43: the tawa on the hob. */
    static void tawa(Gfx g, float x, float hob, float s) {
        g.color(0xFF212121); g.oval(x, hob - 0.008f * s, 0.1f * s, 0.014f * s);
        g.color(0xFF424242); g.oval(x, hob - 0.011f * s, 0.085f * s, 0.009f * s);
    }

    /** v43: the chakla (a round board) with the dough: a ball rolled out flat (roll 0..1); roll < 0: an empty board. */
    static void chakla(Gfx g, float x, float ct, float s, float roll) {
        g.color(0xFF8D6E63); g.rect(x - 0.03f * s, ct - 0.014f * s, 0.06f * s, 0.014f * s);
        g.color(0xFFD7B98E); g.oval(x, ct - 0.018f * s, 0.075f * s, 0.012f * s);
        if (roll < 0) return;
        float r = 0.02f + 0.045f * roll, th = 0.018f * (1 - roll) + 0.003f;
        g.color(0xFFF1DCB4); g.oval(x, ct - 0.022f * s - th * 0.5f * s, r * s, (0.006f + th) * s);
    }

    /** v43: a roti on the tawa: browning (0..1) with spots, puffed up (0..1). */
    static void roti(Gfx g, float x, float y, float s, float brown, float puff) {
        int c = mix(0xFFF1DCB4, 0xFFE0B37A, brown);
        g.color(c); g.oval(x, y, 0.065f * s, (0.008f + 0.03f * puff) * s);
        g.color(alpha(0xFF8D5524, 0.7f * brown));
        for (int i = 0; i < 5; i++) g.oval(x + (i - 2) * 0.022f * s, y - (i % 2) * 0.006f * s * (1 + puff) - puff * 0.012f * s, 0.006f * s, 0.003f * s);
    }

    /** v43: n rotis stacked on a plate, the top one steaming. */
    static void rotiStack(Gfx g, float x, float ct, float s, int n, float t, float since) {
        g.color(0xFFCFD8DC); g.oval(x, ct - 0.005f * s, 0.065f * s, 0.011f * s);
        for (int i = 0; i < n; i++) {
            g.color(i % 2 == 0 ? 0xFFE8C38E : 0xFFE0B37A); g.oval(x, ct - 0.01f * s - i * 0.005f * s, 0.055f * s, 0.008f * s);
            g.color(alpha(0xFF8D5524, 0.5f)); g.oval(x - 0.015f * s + i * 0.004f * s, ct - 0.012f * s - i * 0.005f * s, 0.005f * s, 0.002f * s);
        }
        steam(g, x, ct - 0.03f * s - n * 0.005f * s, s, t, since < 0 ? 0.6f : Math.max(0, 0.6f - since / 12f), 2);
    }

    /** v43: the chopping board with a tomato, an onion and green chillies, cut into pieces as chopped goes 0..1. */
    static void board(Gfx g, float x, float ct, float s, float f, float chopped, boolean whole) {
        g.color(0xFFC49A6C); g.roundRect(x - 0.085f * s, ct - 0.016f * s, 0.17f * s, 0.016f * s, 0.004f * s);
        g.color(0xFFA67C52); g.rect(x - 0.085f * s, ct - 0.004f * s, 0.17f * s, 0.004f * s);
        float k = 1 - chopped;
        if (whole && k > 0.05f) {
            g.color(0xFFE53935); g.oval(x + f * 0.04f * s, ct - 0.016f * s - 0.02f * s * k, 0.02f * s * k, 0.019f * s * k);
            g.color(0xFFAB47BC); g.oval(x + f * 0.012f * s, ct - 0.016f * s - 0.017f * s * k, 0.017f * s * k, 0.016f * s * k);
            g.color(0xFF43A047); g.line(x + f * 0.055f * s, ct - 0.02f * s, x + f * 0.075f * s, ct - 0.024f * s, 0.005f * s * Math.max(0.3f, k));
        }
        int n = (int) (chopped * 14);
        for (int i = 0; i < n; i++) {
            g.color(i % 3 == 0 ? 0xFFE53935 : i % 3 == 1 ? 0xFFCE93D8 : 0xFF66BB6A);
            g.oval(x - f * 0.06f * s + f * (i % 7) * 0.012f * s, ct - 0.019f * s - (i / 7) * 0.004f * s, 0.005f * s, 0.003f * s);
        }
    }

    /** v43: a small pan of hot oil for the tadka: mustard seeds spluttering; tipped over the dal as it is poured (0..1). */
    static void tadkaPan(Gfx g, float x, float hob, float s, float f, float t, boolean hot, float pour) {
        g.save();
        float tilt = -f * 50 * pour;
        g.translate(x, hob - 0.03f * s); g.rotate(tilt); g.translate(-x, -(hob - 0.03f * s));
        float r = 0.05f * s, top = hob - 0.05f * s;
        g.color(0xFF263238); g.line(x - f * r, top + 0.012f * s, x - f * (r + 0.08f * s), top + 0.004f * s, 0.01f * s);
        g.color(0xFF455A64); g.begin(); g.moveTo(x - r, top); g.quadTo(x, hob + 0.004f * s, x + r, top); g.close(); g.fillPath();
        g.color(0xFFF9A825); g.oval(x, top + 0.004f * s, r * 0.8f, 0.005f * s);
        g.restore();
        if (hot) for (int i = 0; i < 9; i++) {
            float ph = (t * 2.7f + i * 0.13f) % 1f, jx = (float) Math.sin(i * 7.3f) * r * 0.9f;
            g.color(alpha(0xFF3E2723, 1 - ph)); g.oval(x + jx, top - ph * (0.03f + (i % 3) * 0.01f) * s, 0.003f * s, 0.003f * s);
        }
    }

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
            // v44: on a horse's back just behind the withers, in an elephant's howdah
            case Film.V_HORSE: return new float[]{-0.08f, 0.8f};
            case Film.V_ELEPHANT: return new float[]{-0.1f, 1.22f};
            default: return new float[]{0, 0.3f};
        }
    }

    /**
     * Draws a vehicle with its seat at the origin's x (so the rider sits on it): the back part (front = false:
     * wheels, frame, the dark inside of a car) or the front part (front = true: a car's door and glass, a bus's side,
     * a scooter's apron). s = the drawing height, dist = how far it has gone (the wheels turn with it), t = time.
     */
    public static void vehicle(Gfx g, int kind, float s, float f, float dist, float t, int color, boolean front) {
        vehicle(g, kind, s, f, dist, t, color, front, 0, 0, 0);
    }

    /**
     * v43: the same, with a car's front and rear doors open by doorF / doorR (0 shut .. 1 wide open, swung towards
     * the camera on their front hinges) and a two-wheeler's stand down by stand (0 up, folded along the frame .. 1
     * down on the ground).
     */
    public static void vehicle(Gfx g, int kind, float s, float f, float dist, float t, int color, boolean front, float doorF, float doorR, float stand) {
        f = f < 0 ? -1 : 1;
        float[] seat = seatOf(kind);
        boolean pass = kind > Film.V_PASSENGER;
        if (pass) kind -= Film.V_PASSENGER;
        g.save();
        g.translate(-seat[0] * s * f, 0);         // the seat over the origin
        switch (kind) {
            case Film.V_BICYCLE: if (!front) kickstand(g, s, f, kind, stand); bicycle(g, s, f, dist, color, front); break;
            case Film.V_MOTORBIKE: if (!front) kickstand(g, s, f, kind, stand); motorbike(g, s, f, dist, t, color, front); break;
            case Film.V_SCOOTER: if (!front) kickstand(g, s, f, kind, stand); scooter(g, s, f, dist, t, color, front); break;
            case Film.V_CAR: car(g, s, f, dist, t, color, front, doorF, doorR); break;
            case Film.V_BUS: bus(g, s, f, dist, t, front); break;
            case Film.V_AUTO: auto(g, s, f, dist, t, front); break;
            case Film.V_HORSE: case Film.V_ELEPHANT: mount(g, kind, s, f, dist, t, front, stand); break;
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

    /**
     * v43: a two-wheeler's stand: a side stand under a bicycle's or a motorbike's middle, a centre stand under a
     * scooter's floor — down to the ground at 1 (parked, the rider getting on or off), folded back along the frame
     * at 0 (riding). Drawn behind the frame.
     */
    static void kickstand(Gfx g, float s, float f, int kind, float down) {
        down = Math.max(0, Math.min(1, down));
        float px, py;
        if (kind == Film.V_BICYCLE) { px = -0.07f * s * f; py = -0.14f * s; }
        else if (kind == Film.V_MOTORBIKE) { px = -0.1f * s * f; py = -0.17f * s; }
        else { px = -0.04f * s * f; py = -0.12f * s; }
        // the angle from straight back along the frame (up) to down and a little back onto the ground
        double up = Math.toRadians(172), dn = Math.toRadians(108);
        double an = up + (dn - up) * (down * down * (3 - 2 * down));
        float len = -py / (float) Math.sin(dn);
        float ex = px + (float) Math.cos(an) * len * f, ey = py + (float) Math.sin(an) * len;
        g.color(0xFF37474F); g.line(px, py, ex, ey, Math.max(1.5f, 0.016f * s));
        if (down > 0.6f) { g.color(0xFF263238); g.oval(ex, ey, 0.018f * s, 0.006f * s); }      // its foot on the ground
    }

    private static Look horseLook, elephantLook;

    /** v44: the look of a ridden animal: a bay horse, a grey elephant. */
    static Look mountAnimal(int kind) {
        if (kind == Film.V_HORSE && horseLook != null) return horseLook;
        if (kind == Film.V_ELEPHANT && elephantLook != null) return elephantLook;
        Look m = new Look();
        m.kind = Look.ANIMAL;
        m.species = kind == Film.V_HORSE ? Look.SP_HORSE : Look.SP_ELEPHANT;
        m.furColor = kind == Film.V_HORSE ? 0xFF7B4A2A : 0xFF8A8580;
        m.skin = Puppet.shade(m.furColor, 1.25f);
        m.eyeColor = 0xFF2B1B10;
        if (kind == Film.V_HORSE) horseLook = m; else elephantLook = m;
        return m;
    }

    /**
     * v44: a horse or an elephant under its rider (s = the rider's height): it walks, trots or gallops with the ground
     * covered (its legs stride with dist), stands breathing when still (still = 1), a saddle cloth on the horse, a
     * howdah on the elephant (its front rail in front of the rider: front = true).
     */
    static void mount(Gfx g, int kind, float s, float f, float dist, float t, boolean front, float still) {
        boolean horse = kind == Film.V_HORSE;
        float Hm = horse ? 0.94f * s : 1.32f * s;
        if (front) {
            if (!horse) {
                // the howdah's front rail and its cloth over the elephant's back, in front of the rider's legs
                float top = -0.93f * Hm;
                g.color(0xFFB71C1C); g.roundRect(-0.36f * Hm, top - 0.02f * Hm, 0.72f * Hm, 0.16f * Hm, 0.03f * Hm);
                g.color(0xFFFFC107); g.rect(-0.36f * Hm, top + 0.1f * Hm, 0.72f * Hm, 0.02f * Hm);
                g.color(0xFF8D6E63); g.rect(-0.3f * Hm, top - 0.16f * Hm, 0.6f * Hm, 0.03f * Hm);
            }
            return;
        }
        Pose pm = new Pose();
        pm.facing = f; pm.time = t;
        float move = Math.max(0, Math.min(1, 1 - still));
        pm.walkAmt = move;
        pm.walk = dist / (horse ? 0.2f * Hm : 0.12f * Hm);
        pm.blink = (t % 3.7f) < 0.12f ? 1 : 0;
        g.save();
        // a gallop rocks the body; standing still it breathes
        float bob = move * (float) Math.abs(Math.sin(pm.walk)) * 0.03f * Hm;
        g.translate(0, -bob);
        if (move > 0.5f && horse) g.rotate((float) Math.sin(pm.walk * 2) * 2.5f * f);
        Puppet.drawAnimal(g, mountAnimal(kind), pm, Hm);
        // the saddle cloth (red with a gold edge) and the saddle on the horse; the howdah's back and seat on the elephant
        float backY = -0.86f * Hm;
        if (horse) {
            g.color(0xFFB71C1C); g.roundRect(-0.2f * Hm, backY - 0.01f * Hm, 0.32f * Hm, 0.2f * Hm, 0.04f * Hm);
            g.color(0xFFFFC107); g.rect(-0.2f * Hm, backY + 0.17f * Hm, 0.32f * Hm, 0.02f * Hm);
            g.color(0xFF4E342E); g.roundRect(-0.15f * Hm, backY - 0.05f * Hm, 0.22f * Hm, 0.07f * Hm, 0.03f * Hm);
            // the stirrup hanging
            g.color(0xFF616161); g.line(-0.06f * Hm, backY, -0.06f * Hm, backY + 0.36f * Hm, 0.008f * Hm);
            g.strokeOval(-0.06f * Hm, backY + 0.38f * Hm, 0.025f * Hm, 0.015f * Hm, 0.008f * Hm);
        } else {
            float top = -0.93f * Hm;
            g.color(0xFF6A1B9A); g.roundRect(-0.42f * Hm, top - 0.02f * Hm, 0.84f * Hm, 0.34f * Hm, 0.06f * Hm);
            g.color(0xFFFFC107); g.rect(-0.42f * Hm, top + 0.28f * Hm, 0.84f * Hm, 0.03f * Hm);
            g.color(0xFF8D6E63); g.roundRect(-0.34f * Hm, top - 0.3f * Hm, 0.68f * Hm, 0.3f * Hm, 0.02f * Hm);
            g.color(0xFFD32F2F); g.begin(); g.moveTo(-0.38f * Hm, top - 0.3f * Hm); g.quadTo(0, top - 0.55f * Hm, 0.38f * Hm, top - 0.3f * Hm); g.close(); g.fillPath();
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

    static void car(Gfx g, float s, float f, float dist, float t, int color, boolean front) { car(g, s, f, dist, t, color, front, 0, 0); }

    static void car(Gfx g, float s, float f, float dist, float t, int color, boolean front, float doorF, float doorR) {
        float L = 0.95f * s;           // half length
        float rw = 0.12f * s;
        float base = -0.045f * s, belt = -0.55f * s, roof = -0.98f * s;      // the sills low: nobody's feet show under the car
        float back = -0.6f * s * f, wsBot = 0.38f * s * f, wsTop = 0.2f * s * f, rearTop = -0.48f * s * f;
        float bx = -0.2f * s * f;      // v43: the pillar between the front door and the rear door
        doorF = Math.max(0, Math.min(1, doorF)); doorR = Math.max(0, Math.min(1, doorR));
        boolean openF = doorF > 0.01f, openR = doorR > 0.01f;
        if (!front) {
            // the dark inside of the cabin, and the seat back behind the driver
            g.color(0xFF263238);
            g.begin(); g.moveTo(back, belt); g.lineTo(rearTop, roof + 0.03f * s); g.lineTo(wsTop, roof + 0.03f * s); g.lineTo(wsBot, belt); g.close(); g.fillPath();
            g.color(0xFF37474F); g.roundRect(-0.14f * s * f - 0.05f * s, -0.82f * s, 0.1f * s, 0.5f * s, 0.03f * s);
            // v43: through an open door, the seats down to the floor
            if (openF || openR) {
                g.color(0xFF2E3B42);
                if (openF) g.rect(Math.min(bx, 0.36f * s * f), belt, Math.abs(0.36f * s * f - bx), base - belt);
                if (openR) g.rect(Math.min(back, bx), belt, Math.abs(bx - back), base - belt);
            }
            return;
        }
        // the cabin's glass (the driver seen through it) — the window of an open door goes with its door
        g.color(alpha(0xFFB3E5FC, 0.22f));
        if (!openR) { g.begin(); g.moveTo(back, belt); g.lineTo(rearTop, roof + 0.03f * s); g.lineTo(bx, roof + 0.03f * s); g.lineTo(bx - 0.02f * s * f, belt); g.close(); g.fillPath(); }
        if (!openF) { g.begin(); g.moveTo(bx - 0.02f * s * f, belt); g.lineTo(bx, roof + 0.03f * s); g.lineTo(wsTop, roof + 0.03f * s); g.lineTo(wsBot, belt); g.close(); g.fillPath(); }
        else { g.begin(); g.moveTo(0.36f * s * f, belt); g.lineTo(wsTop, roof + 0.03f * s); g.lineTo(wsTop + 0.06f * s * f, roof + 0.03f * s); g.lineTo(wsBot, belt); g.close(); g.fillPath(); }
        g.color(alpha(0xFFFFFFFF, 0.18f)); g.line(wsTop - 0.04f * s * f, roof + 0.08f * s, wsBot - 0.1f * s * f, belt - 0.06f * s, 0.03f * s);
        g.color(Puppet.shade(color, 0.85f));
        g.line(back, belt, rearTop, roof + 0.02f * s, 0.05f * s);
        g.line(wsTop, roof + 0.02f * s, wsBot, belt, 0.035f * s);
        g.line(bx, roof + 0.02f * s, bx - 0.02f * s * f, belt, 0.03f * s);
        g.color(color); g.line(rearTop, roof + 0.02f * s, wsTop, roof + 0.02f * s, 0.045f * s);
        // the body below the windows: bonnet, boot, the doors and their handles, lamps, bumpers — an open door's part
        // of it is gone with the door (only its sill stays)
        g.color(color);
        float[] cuts = {-0.95f, 0.95f};
        if (openR && openF) cuts = new float[]{-0.95f, -0.6f, 0.36f, 0.95f};
        else if (openR) cuts = new float[]{-0.95f, -0.6f, -0.2f, 0.95f};
        else if (openF) cuts = new float[]{-0.95f, -0.2f, 0.36f, 0.95f};
        for (int i = 0; i + 1 < cuts.length; i += 2) carBody(g, s, f, cuts[i], cuts[i + 1], base, belt);
        if (openF || openR) {
            g.color(Puppet.shade(color, 0.8f));
            if (openF) g.rect(Math.min(bx, 0.36f * s * f), base - 0.07f * s, Math.abs(0.36f * s * f - bx), 0.07f * s);
            if (openR) g.rect(Math.min(back, bx), base - 0.07f * s, Math.abs(bx - back), 0.07f * s);
            g.color(color);
        }
        g.color(alpha(0xFFFFFFFF, 0.22f)); g.rect(Math.min(-L * f, L * f), belt + 0.02f * s, 2 * L, 0.02f * s);
        g.color(Puppet.shade(color, 0.7f));
        if (!openF || !openR) g.line(bx, belt, bx, base - 0.02f * s, 0.006f * s);
        if (!openF) g.line(0.36f * s * f, belt + 0.02f * s, 0.36f * s * f, base - 0.02f * s, 0.006f * s);
        if (!openR) g.line(back, belt + 0.02f * s, back, base - 0.02f * s, 0.006f * s);
        g.color(0xFFE0E0E0);
        if (!openF) g.roundRect(0.18f * s * f - 0.03f * s, belt + 0.07f * s, 0.06f * s, 0.015f * s, 0.006f * s);
        if (!openR) g.roundRect(-0.42f * s * f - 0.03f * s, belt + 0.07f * s, 0.06f * s, 0.015f * s, 0.006f * s);
        g.color(0xFFFFF59D); g.oval((L - 0.03f * s) * f, belt + 0.12f * s, 0.03f * s, 0.022f * s);
        g.color(0xFFE53935); g.oval(-(L - 0.02f * s) * f, belt + 0.1f * s, 0.02f * s, 0.025f * s);
        g.color(0xFF9E9E9E); g.roundRect(Math.min(-L * f, L * f) - 0.02f * s, base - 0.035f * s, 2 * L + 0.04f * s, 0.04f * s, 0.015f * s);
        // the wheels in their arches
        g.color(0xFF1A1A1A); g.oval(-0.6f * s * f, -rw, rw * 1.18f, rw * 1.18f); g.oval(0.6f * s * f, -rw, rw * 1.18f, rw * 1.18f);
        wheel(g, -0.6f * s * f, -rw, rw, dist, false);
        wheel(g, 0.6f * s * f, -rw, rw, dist, false);
        // the steering wheel in the driver's hands
        g.color(0xFF212121); g.line(0.2f * s * f, belt - 0.1f * s, 0.26f * s * f, belt + 0.02f * s, 0.022f * s);
        // v43: the open doors, swung out towards the camera on their front hinges (narrower as they turn, their free
        // edge nearer and so a little larger), in front of whoever gets in or out
        if (openR) carDoor(g, s, f, -0.2f, -0.6f, -0.48f, -0.2f, doorR, color, base, belt, roof);
        if (openF) carDoor(g, s, f, 0.36f, -0.2f, -0.2f, 0.2f, doorF, color, base, belt, roof);
    }

    /** v43: the car's body below the windows between x0 and x1 (fractions of the drawing height, before the facing). */
    private static void carBody(Gfx g, float s, float f, float x0, float x1, float base, float belt) {
        float sf = s * f;
        g.begin();
        g.moveTo(x0 * sf, base);
        if (x0 <= -0.949f) { g.lineTo(-0.95f * sf, belt + 0.12f * s); g.quadTo(-0.95f * sf, belt, -0.7f * sf, belt); }
        else g.lineTo(x0 * sf, belt);
        if (x1 >= 0.949f) { g.lineTo(0.43f * sf, belt); g.quadTo(0.95f * sf, belt + 0.05f * s, 0.95f * sf, belt + 0.2f * s); g.lineTo(0.95f * sf, base); }
        else { g.lineTo(x1 * sf, belt); g.lineTo(x1 * sf, base); }
        g.close(); g.fillPath();
    }

    /**
     * v43: one open car door: hinged at hinge (its front edge), its free edge at free; its window from winBack to
     * winFront under the roof. open 0..1 turns it up to 70 degrees: seen from the side it narrows to the cosine of
     * that, and its free edge, nearer the camera, reaches a little higher and lower.
     */
    private static void carDoor(Gfx g, float s, float f, float hinge, float free, float winBack, float winFront, float open,
                                int color, float base, float belt, float roof) {
        double th = Math.toRadians(70 * (open * open * (3 - 2 * open)));
        float c = (float) Math.cos(th), sn = (float) Math.sin(th);
        float w = hinge - free;                                          // the door's width (its free edge behind the hinge)
        float[] xs = {free, winBack, winFront, hinge, hinge, free};
        float[] ys = {belt, roof + 0.03f * s, roof + 0.03f * s, belt, base, base};
        float[] px = new float[6], py = new float[6];
        for (int i = 0; i < 6; i++) {
            float k = w == 0 ? 0 : (hinge - xs[i]) / w;                 // 0 at the hinge, 1 at the free edge
            px[i] = (hinge - (hinge - xs[i]) * c) * s * f;
            float grow = 1 + 0.12f * sn * k;
            float midY = (belt + base) * 0.5f;
            py[i] = midY + (ys[i] - midY) * grow;
        }
        // the door's lower panel (turned from the light: a shade darker) and its window frame with the glass
        g.color(Puppet.shade(color, 0.82f - 0.12f * sn));
        g.begin(); g.moveTo(px[0], py[0]); g.lineTo(px[3], py[3]); g.lineTo(px[4], py[4]); g.lineTo(px[5], py[5]); g.close(); g.fillPath();
        g.color(alpha(0xFFB3E5FC, 0.3f));
        g.begin(); g.moveTo(px[0], py[0]); g.lineTo(px[1], py[1]); g.lineTo(px[2], py[2]); g.lineTo(px[3], py[3]); g.close(); g.fillPath();
        g.color(Puppet.shade(color, 0.7f));
        g.begin(); g.moveTo(px[0], py[0]); g.lineTo(px[1], py[1]); g.lineTo(px[2], py[2]); g.lineTo(px[3], py[3]); g.strokePath(0.025f * s);
        g.line(px[0], py[0], px[5], py[5], 0.012f * s);
        // the handle near the free edge
        g.color(0xFFE0E0E0);
        float hx = px[0] + (px[3] - px[0]) * 0.2f, hy = py[0] + (py[3] - py[0]) * 0.2f + 0.07f * s;
        g.roundRect(hx - 0.025f * s, hy, 0.05f * s, 0.014f * s, 0.006f * s);
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
    public static void swingFrame(Gfx g, float h) { swingFrame(g, h, 0xFF1565C0); }

    /** v41: a school bag lying on its back on the ground (h: the child's height): the body, the flap, the straps, a pocket. */
    public static void schoolBag(Gfx g, float h) {
        float w = 0.3f * h, d = 0.11f * h;
        g.color(alpha(0xFF000000, 0.18f)); g.oval(0, 0.006f * h, w * 0.62f, 0.022f * h);
        g.color(0xFF1A3A6B); g.roundRect(-w / 2, -d, w, d, 0.03f * h);
        g.color(0xFF24508F); g.roundRect(-w / 2 + 0.01f * h, -d - 0.012f * h, w * 0.62f, d * 0.7f, 0.025f * h);
        g.color(0xFFE53935); g.roundRect(w * 0.08f, -d * 0.8f, w * 0.3f, d * 0.55f, 0.015f * h);
        g.color(0xFF0E2747); g.line(-w * 0.35f, -d * 0.2f, -w * 0.6f, -d * 0.05f, 0.012f * h); g.line(-w * 0.2f, -d * 0.15f, -w * 0.45f, 0.004f * h, 0.012f * h);
        g.color(0xFFFFD54F); g.oval(-w * 0.05f, -d * 0.55f, 0.012f * h, 0.012f * h);
    }

    /** v41: a colour a share of the way to another. */
    static int mix(int a, int b, float k) {
        int r = Math.round(((a >> 16) & 255) * (1 - k) + ((b >> 16) & 255) * k), gg = Math.round(((a >> 8) & 255) * (1 - k) + ((b >> 8) & 255) * k);
        int bl = Math.round((a & 255) * (1 - k) + (b & 255) * k);
        return 0xFF000000 | (r << 16) | (gg << 8) | bl;
    }

    /** v41: the frame in a colour (the seat's own: a red seat hangs from a red frame). */
    public static void swingFrame(Gfx g, float h, int col) {
        float top = -SWING_TOP * h;
        int far = mix(col, 0xFF607D8B, 0.55f), dark = mix(col, 0xFF000000, 0.35f);
        g.color(far);
        g.line(-0.5f * h + 0.08f * h, 0, 0.08f * h, top - 0.03f * h, 0.03f * h); g.line(0.5f * h + 0.08f * h, 0, 0.08f * h, top - 0.03f * h, 0.03f * h);
        g.color(col);
        g.line(-0.55f * h, 0, 0, top, 0.035f * h); g.line(0.55f * h, 0, 0, top, 0.035f * h);
        // a light catching the front tubes (rounded metal)
        g.color(mix(col, 0xFFFFFFFF, 0.35f));
        g.line(-0.55f * h + 0.008f * h, -0.01f * h, -0.004f * h, top + 0.02f * h, 0.008f * h); g.line(0.55f * h - 0.004f * h, -0.01f * h, 0.006f * h, top + 0.02f * h, 0.008f * h);
        g.color(dark); g.oval(0, top, 0.035f * h, 0.035f * h);
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

    /**
     * v41: where a slide's parts are, as shares of the child's height h, along the chute's direction from the slide's
     * anchor (x) and above the ground (y): where one waits at the foot of the ladder, the first rung, the platform, the
     * top of the chute (where one sits), its surface down to its end, and where one lands on the grass past it. The
     * drawn slide's (its ladder at the anchor), or one read from the user's own picture of their slide.
     */
    public static final class SlideGeo {
        public float footX, rungX, deckX, deckY, sitX, endX, endY, landX;
        /** The chute's surface height from sitX to endX, in 32 even steps. */
        public final float[] surf = new float[33];
        /** v41: the user's picture of the slide (null: drawn) and where it goes: its left, top, width and height in shares of h, x as above, mirrored when the chute runs the other way. */
        public Object pic;
        public float picX, picTop, picW, picH;
        public boolean picFlip;

        public float surfAt(float x) {
            float u = (x - sitX) / Math.max(1e-3f, endX - sitX) * 32;
            if (u <= 0) return surf[0];
            if (u >= 32) return surf[32];
            int i = (int) u;
            return surf[i] + (surf[Math.min(32, i + 1)] - surf[i]) * (u - i);
        }

        /** The drawn slide (Props.slide): the ladder's rails at the anchor, the platform at SLIDE_TOP, the chute from 0.12. */
        public static SlideGeo drawn() {
            SlideGeo g = new SlideGeo();
            g.footX = -0.38f; g.rungX = -0.15f; g.deckX = -0.05f; g.deckY = SLIDE_TOP; g.sitX = 0.1f;
            g.endX = SLIDE_LEN + 0.12f; g.endY = SLIDE_END; g.landX = SLIDE_LEN + 0.48f;
            for (int i = 0; i <= 32; i++) {
                float x = g.sitX + (g.endX - g.sitX) * i / 32f;
                g.surf[i] = x >= SLIDE_LEN ? SLIDE_END : slideHeight(x / SLIDE_LEN);
            }
            return g;
        }
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
