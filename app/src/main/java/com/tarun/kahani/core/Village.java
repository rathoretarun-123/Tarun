package com.tarun.kahani.core;

/**
 * v46: the things of a village story, drawn: a mango in the hand, a clay water pot (घड़ा), a bundle of grass, a string
 * of little flags (झंडियाँ), a sack for rubbish, a white cap (टोपी), a patch of mud an animal is stuck in, the flags
 * hung across the place for a fair, rubbish lying on the road, the rope a stuck cow is pulled out with, and a thatched
 * shelter (छप्पर) on posts.
 */
public final class Village {
    private Village() {}

    /** A held thing at the hand (0, 0), in the units of Renderer.drawItem (an actor 400 high is 1.6 px a unit). */
    static void item(Gfx g, int item, float time, float facing) {
        switch (item) {
            case Pose.I_MANGO:
                g.color(0xFF6D8B2F); g.begin(); g.moveTo(0, -12); g.quadTo(facing * 9, -18, facing * 13, -12); g.quadTo(facing * 6, -10, 0, -12); g.close(); g.fillPath();
                g.color(0xFFE59A1C); g.oval(0, -1, 9.5f, 12);
                g.color(0xFFF4C13A); g.oval(-facing * 2, -3, 6, 8);
                g.color(0x55FFFFFF); g.oval(-facing * 3.5f, -6, 2, 3);
                break;
            case Pose.I_POT: {
                // held by its neck: the round body hangs below the hand
                g.color(0xFF7A3B1F); g.oval(0, 22, 22, 20);
                g.color(0xFFA9532D); g.oval(-2, 20, 19, 17);
                g.color(0xFFC46A3C); g.oval(-6, 16, 8, 7);
                g.color(0xFF6B3018); g.roundRect(-8, -2, 16, 7, 3);
                g.color(0xFF8E4425); g.oval(0, -2, 10, 3.2f);
                g.color(0xFF3B6E8F); g.oval(0, -2, 7, 1.8f);          // the water inside
                g.color(0xFFE8D7A8); g.line(-19, 24, 19, 24, 1.6f);   // a painted band
                break;
            }
            case Pose.I_GRASS: {
                g.color(0xFF8D6E3F); g.roundRect(-9, -3, 18, 6, 3);   // the tie
                for (int i = 0; i < 11; i++) {
                    float x = -10 + i * 2f, sway = (float) Math.sin(time * 2 + i) * 1.5f;
                    g.color(i % 3 == 0 ? 0xFF4F8A2B : i % 3 == 1 ? 0xFF6BA539 : 0xFF86BC4A);
                    g.begin(); g.moveTo(x, 4); g.quadTo(x * 1.2f + sway, -16, x * 1.6f + sway * 2, -30 - (i % 4) * 3); g.strokePath(1.6f);
                    g.begin(); g.moveTo(x, 2); g.quadTo(x * 1.1f, 12, x * 1.3f, 20 + (i % 3) * 2); g.strokePath(1.6f);
                }
                break;
            }
            case Pose.I_FLAGS: {
                g.color(0xFF5D4037);
                g.begin(); g.moveTo(0, 0); g.quadTo(facing * 14, 26, facing * 30, 40); g.strokePath(1.2f);
                int[] cols = {0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFFFB8C00};
                for (int i = 0; i < 5; i++) {
                    float u = (i + 0.5f) / 5f, x = facing * (14 * 2 * u * (1 - u) + 30 * u * u), y = 26 * 2 * u * (1 - u) + 40 * u * u;
                    g.color(cols[i]); g.begin(); g.moveTo(x - 3, y); g.lineTo(x + 3, y); g.lineTo(x, y + 8); g.close(); g.fillPath();
                }
                break;
            }
            case Pose.I_SACK:
                g.color(0xFF9C8458); g.begin(); g.moveTo(-6, 0); g.quadTo(-16, 18, -12, 30); g.quadTo(0, 36, 12, 30); g.quadTo(16, 18, 6, 0); g.close(); g.fillPath();
                g.color(0xFF7D6A44); g.roundRect(-7, -3, 14, 5, 2);
                g.color(0xFFB59B6A); g.line(-9, 14, 9, 16, 1); g.line(-10, 22, 10, 23, 1);
                break;
            default:
        }
    }

    /** A white Gandhi cap (टोपी) on the head: centred at x, its lower edge at y, r: half its length. */
    public static void cap(Gfx g, float x, float y, float r, int color, float facing) {
        g.color(Puppet.shade(color, 0.72f));
        g.begin(); g.moveTo(x - r, y); g.quadTo(x, y + r * 0.12f, x + r, y); g.lineTo(x + r * 0.86f, y - r * 0.36f);
        g.quadTo(x, y - r * 0.5f, x - r * 0.86f, y - r * 0.36f); g.close(); g.fillPath();
        g.color(color);
        g.begin(); g.moveTo(x - r * 0.95f, y - r * 0.04f); g.quadTo(x, y + r * 0.06f, x + r * 0.95f, y - r * 0.04f); g.lineTo(x + r * 0.8f, y - r * 0.38f);
        g.quadTo(x, y - r * 0.56f, x - r * 0.8f, y - r * 0.38f); g.close(); g.fillPath();
        g.color(Puppet.shade(color, 0.85f));
        g.begin(); g.moveTo(x - r * 0.8f, y - r * 0.36f); g.quadTo(x, y - r * 0.24f, x + r * 0.8f, y - r * 0.36f); g.strokePath(Math.max(1, r * 0.05f));
        g.color(0x40FFFFFF); g.oval(x - facing * r * 0.3f, y - r * 0.3f, r * 0.3f, r * 0.07f);
    }

    /** A patch of wet mud on the ground at x (w wide), behind those standing in it. */
    static void mudPatch(Gfx g, float x, float gy, float w, float time) {
        float h = w * 0.085f;
        // a soft wet edge into the dry road, the dark wet mud, its lighter clods and the sky's sheen on the water
        g.color(0x557A5536); g.oval(x, gy, w * 0.56f, h * 0.72f);
        g.color(0xFF6A4A2E); g.oval(x, gy, w * 0.5f, h * 0.58f);
        g.color(0xFF573A22); g.oval(x + w * 0.04f, gy + h * 0.06f, w * 0.4f, h * 0.42f);
        g.color(0xFF7E5A3A);
        for (int i = 0; i < 7; i++) g.oval(x - w * 0.38f + i * w * 0.125f, gy - h * 0.18f + ((i * 5) % 3) * h * 0.16f, w * 0.035f, h * 0.1f);
        g.color(0x66D8E4EC); g.oval(x - w * 0.16f, gy - h * 0.1f, w * 0.13f, h * 0.07f);
        g.color(0x40D8E4EC); g.oval(x + w * 0.22f, gy + h * 0.12f, w * 0.09f, h * 0.05f);
    }

    /**
     * The mud in front of one stuck in it: the legs sunk to the knees (H: the height of the one stuck, len: how far its
     * body reaches either side of x), a little wobble as they struggle; out (sink 0) once pulled free.
     */
    static void mudFront(Gfx g, float x, float gy, float len, float H, float sink, float time) {
        if (sink <= 0.01f) return;
        float top = gy - H * 0.17f * sink;
        g.color(0xFF5E4029);
        g.begin(); g.moveTo(x - len, gy + 4);
        for (int i = 0; i <= 8; i++) {
            float u = i / 8f, xx = x - len + 2 * len * u;
            float bump = (float) Math.sin(u * Math.PI) * 0.6f + 0.4f + (float) Math.sin(time * 5 + i * 1.7f) * 0.05f;
            g.lineTo(xx, gy - (gy - top) * bump);
        }
        g.lineTo(x + len, gy + 4); g.close(); g.fillPath();
        g.color(0xFF7E5A3A);
        for (int i = 0; i < 6; i++) g.oval(x - len * 0.7f + i * len * 0.28f, top + (gy - top) * 0.35f + (i % 2) * 4, len * 0.08f, (gy - top) * 0.12f + 2);
        g.color(0x50D8E4EC);
        g.oval(x - len * 0.3f, top + (gy - top) * 0.25f, len * 0.12f, Math.max(1.5f, (gy - top) * 0.05f));
        // splashes of mud as it struggles
        float s = (float) Math.sin(time * 6);
        if (s > 0.6f) { g.color(0xFF5A3D26); g.oval(x - len * 0.5f, top - 8 * s, 4, 3); g.oval(x + len * 0.45f, top - 12 * s, 3, 3); }
    }

    /** Strings of little flags across the top of the place from x0 to x1, put up as far as p (0..1). */
    static void bunting(Gfx g, float x0, float x1, float y, float p, float time) {
        if (p <= 0) return;
        int[] cols = {0xFFE53935, 0xFFFDD835, 0xFF43A047, 0xFF1E88E5, 0xFFFB8C00, 0xFF8E24AA};
        for (int row = 0; row < 2; row++) {
            float yy = y + row * 46, sag = 34 + row * 10, xs = x0 + row * 40, xe = x1 - row * 40;
            float end = xs + (xe - xs) * Math.min(1, p);
            g.color(0xFF4E342E);
            g.begin(); g.moveTo(xs, yy);
            int n = 24;
            for (int i = 1; i <= n; i++) { float u = i / (float) n, xx = xs + (xe - xs) * u; if (xx > end) break; g.lineTo(xx, yy + sag * 4 * u * (1 - u)); }
            g.strokePath(1.6f);
            int flags = 18;
            for (int i = 0; i < flags; i++) {
                float u = (i + 0.5f) / flags, xx = xs + (xe - xs) * u;
                if (xx > end) break;
                float yt = yy + sag * 4 * u * (1 - u), sw = (float) Math.sin(time * 2.2f + i * 0.9f + row) * 3;
                g.color(cols[(i + row * 2) % cols.length]);
                g.begin(); g.moveTo(xx - 9, yt); g.lineTo(xx + 9, yt); g.lineTo(xx + sw, yt + 22); g.close(); g.fillPath();
            }
        }
    }

    /** One piece of rubbish lying on the road at x, y (kind 0..3: a paper, a plastic bag, a peel, a bottle), ps: perspective. */
    static void litter(Gfx g, float x, float y, int kind, float ps) {
        float k = ps * 2.4f;     // (big enough to read in a wide shot: a crumpled paper is a hand's size)
        g.color(0x30000000); g.oval(x, y + 2 * k, 13 * k, 3 * k);
        switch (kind & 3) {
            case 0:
                g.color(0xFFE8E6DF); g.begin(); g.moveTo(x - 9 * k, y); g.lineTo(x - 5 * k, y - 8 * k); g.lineTo(x + 3 * k, y - 10 * k); g.lineTo(x + 9 * k, y - 3 * k); g.lineTo(x + 6 * k, y + 1 * k); g.close(); g.fillPath();
                g.color(0xFFB9B5AA); g.line(x - 4 * k, y - 6 * k, x + 4 * k, y - 2 * k, 1);
                break;
            case 1:
                g.color(0xFF5C8FD6); g.begin(); g.moveTo(x - 10 * k, y); g.quadTo(x - 6 * k, y - 12 * k, x + 2 * k, y - 9 * k); g.quadTo(x + 11 * k, y - 6 * k, x + 8 * k, y); g.close(); g.fillPath();
                g.color(0x66FFFFFF); g.oval(x - 3 * k, y - 6 * k, 3 * k, 1.5f * k);
                break;
            case 2:
                g.color(0xFFE7C33A); g.begin(); g.moveTo(x - 8 * k, y); g.quadTo(x - 2 * k, y - 7 * k, x + 2 * k, y - 1 * k); g.quadTo(x + 6 * k, y - 8 * k, x + 9 * k, y); g.close(); g.fillPath();
                g.color(0xFF6D5A1E); g.oval(x + 9 * k, y, 1.5f * k, 1.5f * k);
                break;
            default:
                g.color(0xFF4F9A5C); g.roundRect(x - 10 * k, y - 4 * k, 16 * k, 7 * k, 3 * k);
                g.color(0xFF3D7A47); g.rect(x + 6 * k, y - 2.5f * k, 4 * k, 4 * k);
                g.color(0x66FFFFFF); g.line(x - 8 * k, y - 2 * k, x + 4 * k, y - 2 * k, 1);
        }
    }

    /** A rope from (x0, y0) to (x1, y1), sagging a little (taut: 0 sag). */
    static void rope(Gfx g, float x0, float y0, float x1, float y1, float sag) {
        g.color(0xFF4E3B22);
        g.begin(); g.moveTo(x0, y0); g.quadTo((x0 + x1) / 2, (y0 + y1) / 2 + sag, x1, y1); g.strokePath(7);
        g.color(0xFFB89A62);
        g.begin(); g.moveTo(x0, y0 - 1); g.quadTo((x0 + x1) / 2, (y0 + y1) / 2 + sag - 1, x1, y1 - 1); g.strokePath(3);
    }

    /** A thatched shelter on four posts at x, w wide, its roof's front edge at a height h above the ground. */
    static void shed(Gfx g, float x, float gy, float w, float h, float time) {
        float l = x - w / 2, r = x + w / 2;
        g.color(0xFF5D4037);
        for (float px : new float[]{l + 14, r - 14}) g.rect(px - 6, gy - h, 12, h);
        g.color(0xFF4E342E); g.rect(l + 10, gy - h - 6, w - 20, 10);
        // the thatch: a sloped roof of straw, darker underneath
        g.color(0xFF8D6E3F);
        g.begin(); g.moveTo(l - 30, gy - h + 6); g.lineTo(r + 30, gy - h + 6); g.lineTo(r - 10, gy - h - h * 0.42f); g.lineTo(l + 10, gy - h - h * 0.42f); g.close(); g.fillPath();
        g.color(0xFFC8A15A);
        g.begin(); g.moveTo(l - 26, gy - h + 2); g.lineTo(r + 26, gy - h + 2); g.lineTo(r - 12, gy - h - h * 0.4f); g.lineTo(l + 12, gy - h - h * 0.4f); g.close(); g.fillPath();
        // the straw: thick strands down the slope, in three shades, and a ragged fringe hanging from the eave
        int[] straw = {0xFFA9864A, 0xFF8F6F38, 0xFFC9A55E};
        for (int i = 0; i < 70; i++) {
            float u = i / 69f, bx = l - 22 + (w + 44) * u, tx = l + 12 + (w - 24) * u + (float) Math.sin(i * 1.7f) * 6;
            g.color(straw[i % 3]);
            g.line(bx, gy - h + 2, tx, gy - h - h * 0.4f, 3.2f);
        }
        g.color(0xFF6E5428); g.line(l - 26, gy - h + 3, r + 26, gy - h + 3, 4);
        for (int i = 0; i < 90; i++) {
            float bx = l - 24 + (w + 48) * i / 89f;
            g.color(straw[(i * 7) % 3]);
            g.line(bx, gy - h + 2, bx + (float) Math.sin(i * 2.1f) * 4, gy - h + 12 + (i % 4) * 4, 2.6f);
        }
        // the shade under the roof
        g.color(0x22000000); g.rect(l, gy - h + 6, w, h - 6);
    }
}
