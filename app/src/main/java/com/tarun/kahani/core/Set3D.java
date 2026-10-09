package com.tarun.kahani.core;

/**
 * A place built in three dimensions for a scene without a picture — the same kinds of place the painted sets
 * know (Sets), at the scene's hour — rendered by Studio3D as a background plate with its floor line known
 * exactly (where the characters' feet go). A wide lens, atmospheric perspective, a soft depth of field far away,
 * the key light and sky of the hour (handbook ch. 7: time of day, key direction, warm-cool relationship).
 */
public final class Set3D {
    private Set3D() {}

    public static final class Result {
        public int[] px;
        public int w, h;
        /** Where the characters stand: a fraction of the picture's height. */
        public float ground;
    }

    /** The camera's stage: characters stand on the line z = 0; the ground runs from the camera into the distance. */
    static final float CAM_Z = 7.5f, FAR = -130f;

    public static Result make(int set, int tod, int w, int h, int seed) { return make(set, tod, w, h, seed, null); }

    /** With a style cue from the user's own pictures: the key light from their side, graded onto their line. */
    public static Result make(int set, int tod, int w, int h, int seed, StyleCue cue) {
        Studio3D.Scene s = new Studio3D.Scene();
        java.util.Random rnd = new java.util.Random(seed * 7919L + set * 131L + tod);
        boolean indoor = !Sets.outdoorSet(set) && set != Sets.CAVE_MOUTH;
        hour(s, set, tod);
        s.camX = 0; s.camY = 1.55f; s.camZ = CAM_Z;
        s.lookX = 0; s.lookY = 1.15f; s.lookZ = -8f;
        s.fovDeg = 48f;
        s.dof = indoor ? 0 : 1.2f;
        s.ao = true; s.shadows = true;
        s.aoRadius = 0.35f;
        // the shadow map stays sharp where the characters will stand
        s.shadowRadius = 26f; s.shadowX = 0; s.shadowY = 3f; s.shadowZ = -8f;
        switch (set) {
            case Sets.GARDEN: garden(s, tod, rnd, true); break;
            case Sets.COURTYARD: courtyard(s, tod, rnd); break;
            case Sets.GATE: gate(s, tod, rnd); break;
            case Sets.CAVE_IN: cave(s, tod, rnd, false); break;
            case Sets.CAVE_MOUTH: cave(s, tod, rnd, true); break;
            case Sets.FOREST: forest(s, tod, rnd); break;
            case Sets.CELEBRATION: garden(s, tod, rnd, true); lights(s, rnd); break;
            case Sets.HALL: hall(s, tod, rnd); break;
            case Sets.VILLAGE: village(s, tod, rnd); break;
            case Sets.ROOFTOP: rooftop(s, tod, rnd); break;
            case Sets.BASEMENT: basement(s, tod, rnd); break;
            case Sets.ROOM: room(s, tod, rnd); break;
            case Sets.STREET: street(s, tod, rnd); break;
            default: garden(s, tod, rnd, false);
        }
        if (cue != null && cue.lightSure > 0.3f && Math.signum(s.keyX) != Math.signum(cue.lightSide) && s.keyX != 0) s.keyX = -s.keyX;
        int floor = s.mark(0, 0, 0);
        Studio3D.Picture p = Studio3D.render(s, w, h, 2);
        if (cue != null) cue.grade(p.px, p.w, p.h);
        Result r = new Result();
        r.px = p.px; r.w = p.w; r.h = p.h;
        r.ground = Math.max(0.5f, Math.min(0.98f, p.marks[floor][1]));
        for (int i = 0; i < r.px.length; i++) r.px[i] |= 0xFF000000;
        return r;
    }

    // ------------------------------------------------------------------ the hour: sky, key light, fog

    static void hour(Studio3D.Scene s, int set, int tod) {
        boolean indoor = !Sets.outdoorSet(set) && set != Sets.CAVE_MOUTH;
        switch (tod) {
            case Sets.MORNING:
                s.skyTop = 0xFF7FB2E8; s.skyBottom = 0xFFFFD9B0; s.fogColor = 0xFFFFE0C0;
                s.keyX = 0.8f; s.keyY = 0.45f; s.keyZ = 0.5f; s.keyColor = 0xFFFFE2B8; s.keyStrength = 1.15f;
                s.skyColor = 0xFFBFD6F0; s.groundColor = 0xFF7A8A50; break;
            case Sets.EVENING:
                s.skyTop = 0xFF6B4E9B; s.skyBottom = 0xFFFF9A5C; s.fogColor = 0xFFF2A070;
                s.keyX = -0.85f; s.keyY = 0.35f; s.keyZ = 0.45f; s.keyColor = 0xFFFFB070; s.keyStrength = 1.2f;
                s.skyColor = 0xFFB08AD0; s.groundColor = 0xFF6A5A40; break;
            case Sets.NIGHT:
                s.skyTop = 0xFF0B1230; s.skyBottom = 0xFF2A3A6A; s.fogColor = 0xFF1C2850;
                s.keyX = -0.4f; s.keyY = 0.9f; s.keyZ = 0.5f; s.keyColor = 0xFF9CB4E8; s.keyStrength = 0.75f;
                s.skyColor = 0xFF3A4A80; s.groundColor = 0xFF202838; s.ambient = 0.3f; s.fillStrength = 0.3f; break;
            default:
                s.skyTop = 0xFF4F94E0; s.skyBottom = 0xFFC8E6FA; s.fogColor = 0xFFD8E8F8;
                s.keyX = -0.5f; s.keyY = 1f; s.keyZ = 0.6f; s.keyColor = 0xFFFFF4E0; s.keyStrength = 1.2f;
                s.skyColor = 0xFFC4DCF4; s.groundColor = 0xFF8C9A5E;
        }
        s.fogStart = 25f; s.fogEnd = 120f;
        if (indoor) { s.fogColor = Studio3D.mix(s.skyBottom, 0xFF000000, 0.3f); s.fogStart = 40f; s.fogEnd = 200f; }
        if (set == Sets.CAVE_IN || set == Sets.BASEMENT) {
            s.skyTop = 0xFF0A0C10; s.skyBottom = 0xFF14181E; s.fogColor = set == Sets.BASEMENT ? 0xFF101418 : 0xFF1A2420; s.fogStart = 10f; s.fogEnd = 45f;
            s.keyColor = set == Sets.BASEMENT ? 0xFFB8F0C0 : 0xFFC8D8C0; s.keyStrength = 1.25f; s.ambient = 0.62f; s.fillStrength = 0.45f;
            s.keyX = 0.25f; s.keyY = 1f; s.keyZ = 0.55f;
            s.skyColor = set == Sets.BASEMENT ? 0xFF4A6A60 : 0xFF5A7060; s.groundColor = 0xFF3A4044;
        }
    }

    // ------------------------------------------------------------------ building blocks

    static void ground(Studio3D.Scene s, Studio3D.Material mat) {
        int m = s.mesh.mat(mat);
        float near = CAM_Z - 1.5f, far = FAR;
        s.mesh.box(0, -0.5f, (near + far) / 2, 260f, 1f, near - far, m);
    }

    static void hills(Studio3D.Scene s, java.util.Random rnd, int color, int count, float zBase) {
        int m = s.mesh.mat(Studio3D.leaf(color));
        for (int i = 0; i < count; i++) {
            float x = -60 + 120f * i / Math.max(1, count - 1) + rnd.nextFloat() * 14 - 7, z = zBase - rnd.nextFloat() * 30;
            float r = 22 + rnd.nextFloat() * 16;
            s.mesh.sphere(x, -r * 0.55f, z, r * 1.5f, r, r, 12, m);
        }
    }

    static void tree(Studio3D.Scene s, java.util.Random rnd, float x, float z, float size, int leafColor, int trunkColor) {
        int trunk = s.mesh.mat(Studio3D.wood(trunkColor)), leaf = s.mesh.mat(Studio3D.leaf(leafColor));
        float h = 2.6f * size;
        s.mesh.cylinder(x, 0, z, x + 0.1f * size, h * 0.55f, z, 0.16f * size, 0.1f * size, 8, trunk);
        s.mesh.sphere(x + 0.1f * size, h * 0.78f, z, 1.05f * size, 0.95f * size, 1.05f * size, 10, leaf);
        s.mesh.sphere(x - 0.5f * size, h * 0.62f, z + 0.2f * size, 0.7f * size, 0.6f * size, 0.7f * size, 9, s.mesh.mat(Studio3D.leaf(Studio3D.shade(leafColor, 0.88f))));
        s.mesh.sphere(x + 0.65f * size, h * 0.66f, z - 0.1f * size, 0.72f * size, 0.62f * size, 0.72f * size, 9, s.mesh.mat(Studio3D.leaf(Studio3D.shade(leafColor, 1.08f))));
    }

    static void flowers(Studio3D.Scene s, java.util.Random rnd, int count, float zMin, float zMax) {
        int[] cols = {0xFFF06292, 0xFFFFD54F, 0xFFFFFFFF, 0xFFFF7043, 0xFFBA68C8};
        for (int i = 0; i < count; i++) {
            float x = rnd.nextFloat() * 16 - 8, z = zMin + rnd.nextFloat() * (zMax - zMin);
            if (Math.abs(x) < 1.6f && z > -3f) continue;     // not on the stage itself
            int m = s.mesh.mat(Studio3D.silk(cols[rnd.nextInt(cols.length)]));
            s.mesh.sphere(x, 0.12f, z, 0.08f, 0.06f, 0.08f, 5, m);
            s.mesh.capsule(x, 0, z, x, 0.1f, z, 0.012f, 0.012f, 4, s.mesh.mat(Studio3D.leaf(0xFF4C8F3A)));
        }
    }

    static void palace(Studio3D.Scene s, float x, float z, float scale) {
        int marble = s.mesh.mat(Studio3D.stone(0xFFF2EEE6)), dome = s.mesh.mat(Studio3D.silk(0xFFE8E2D4)), gold = s.mesh.mat(Studio3D.metal(0xFFE2B84A));
        int dark = s.mesh.mat(Studio3D.stone(0xFF6B5A48));
        float w = 12 * scale, h = 5 * scale, d = 6 * scale;
        s.mesh.box(x, h / 2, z, w, h, d, marble);
        s.mesh.box(x, h + 0.3f * scale, z, w * 1.04f, 0.6f * scale, d * 1.04f, marble);
        s.mesh.sphere(x, h + 0.6f * scale, z, 2.4f * scale, 2.2f * scale, 2.4f * scale, 14, dome);
        s.mesh.capsule(x, h + 2.6f * scale, z, x, h + 3.6f * scale, z, 0.14f * scale, 0.02f, 6, gold);
        for (int side = -1; side <= 1; side += 2) {
            s.mesh.cylinder(x + side * w * 0.45f, 0, z + d * 0.45f, x + side * w * 0.45f, h * 1.45f, z + d * 0.45f, 0.6f * scale, 0.55f * scale, 12, marble);
            s.mesh.sphere(x + side * w * 0.45f, h * 1.5f, z + d * 0.45f, 0.75f * scale, 0.7f * scale, 0.75f * scale, 10, dome);
        }
        // the door and the windows (dark arches)
        s.mesh.box(x, h * 0.3f, z + d / 2 + 0.02f * scale, 1.6f * scale, h * 0.6f, 0.08f * scale, dark);
        for (int i = -2; i <= 2; i++) if (i != 0) s.mesh.box(x + i * w * 0.18f, h * 0.55f, z + d / 2 + 0.02f * scale, 0.7f * scale, 1.2f * scale, 0.08f * scale, dark);
    }

    static void lights(Studio3D.Scene s, java.util.Random rnd) {
        int[] cols = {0xFFFFD54F, 0xFFFF7043, 0xFF4FC3F7, 0xFFF06292, 0xFF9CCC65};
        for (int i = 0; i < 40; i++) {
            float x = -14 + 28f * i / 39f, y = 3.6f + (float) Math.sin(i * 0.9) * 0.25f, z = -4 - (i % 2) * 3;
            s.mesh.sphere(x, y, z, 0.09f, 0.09f, 0.09f, 5, s.mesh.mat(Studio3D.glowing(cols[i % cols.length], cols[i % cols.length] & 0xB0FFFFFF)));
        }
        for (int i = 0; i < 12; i++) {
            float x = -7 + 14f * i / 11f, z = -1.5f - rnd.nextFloat() * 2;
            s.mesh.sphere(x, 0.06f, z, 0.12f, 0.06f, 0.12f, 6, s.mesh.mat(Studio3D.plastic(0xFFB45A2A)));
            s.mesh.sphere(x, 0.17f, z, 0.03f, 0.06f, 0.03f, 5, s.mesh.mat(Studio3D.glowing(0xFFFFF59D, 0xE0FFE082)));
        }
    }

    // ------------------------------------------------------------------ the places

    static void garden(Studio3D.Scene s, int tod, java.util.Random rnd, boolean withPalace) {
        ground(s, Studio3D.leaf(0xFF7CB342));
        hills(s, rnd, 0xFF689F38, 5, -60);
        hills(s, rnd, 0xFF8BC34A, 4, -38);
        if (withPalace) palace(s, 0, -34, 1.4f);
        for (int i = 0; i < 7; i++) tree(s, rnd, -16 + 32f * i / 6 + rnd.nextFloat() * 3, -12 - rnd.nextFloat() * 10, 1.2f + rnd.nextFloat() * 0.6f, 0xFF43A047, 0xFF6D4C41);
        tree(s, rnd, -6.5f, -2.5f, 1.5f, 0xFF4CAF50, 0xFF5D4037);
        tree(s, rnd, 6.8f, -3.2f, 1.4f, 0xFF388E3C, 0xFF5D4037);
        // a path and a pond
        s.mesh.box(0, 0.01f, -8, 2.6f, 0.02f, 26, s.mesh.mat(Studio3D.stone(0xFFE8DCC0)));
        s.mesh.disc(-4.5f, 0.012f, -9, 2.2f, 1, 16, s.mesh.mat(Studio3D.silk(0xFF64B5F6)));
        flowers(s, rnd, 70, -11, 1.5f);
        // bushes
        for (int i = 0; i < 6; i++) s.mesh.sphere(-9 + 18f * i / 5 + rnd.nextFloat(), 0.45f, -6.5f - rnd.nextFloat() * 2, 0.9f, 0.55f, 0.8f, 8, s.mesh.mat(Studio3D.leaf(0xFF558B2F)));
    }

    static void courtyard(Studio3D.Scene s, int tod, java.util.Random rnd) {
        ground(s, Studio3D.stone(0xFFD9C9A8));
        int wall = s.mesh.mat(Studio3D.stone(0xFFE6D8C0)), dark = s.mesh.mat(Studio3D.stone(0xFF7A6A58));
        // the walls with arches on three sides
        s.mesh.box(0, 2.2f, -18, 44, 4.4f, 1.2f, wall);
        s.mesh.box(-20, 2.2f, -4, 1.2f, 4.4f, 30, wall);
        s.mesh.box(20, 2.2f, -4, 1.2f, 4.4f, 30, wall);
        for (int i = -4; i <= 4; i++) s.mesh.box(i * 4.6f, 1.5f, -17.3f, 1.8f, 3f, 0.3f, dark);
        for (int i = -4; i <= 4; i++) s.mesh.cylinder(i * 4.6f + 2.3f, 0, -17.2f, i * 4.6f + 2.3f, 4.4f, -17.2f, 0.35f, 0.32f, 10, wall);
        // a fountain in the middle distance
        s.mesh.cylinder(0, 0, -9, 0, 0.5f, -9, 2.2f, 2.2f, 18, wall);
        s.mesh.disc(0, 0.52f, -9, 2.0f, 1, 18, s.mesh.mat(Studio3D.silk(0xFF64B5F6)));
        s.mesh.cylinder(0, 0.5f, -9, 0, 1.6f, -9, 0.3f, 0.25f, 10, wall);
        s.mesh.sphere(0, 1.75f, -9, 0.5f, 0.2f, 0.5f, 10, wall);
        // paving lines
        int seam = s.mesh.mat(Studio3D.stone(0xFFC9B99A));
        for (int i = -6; i <= 6; i++) s.mesh.box(i * 3f, 0.012f, -6, 0.06f, 0.02f, 26, seam);
        for (int i = 0; i < 4; i++) { s.mesh.sphere(-14 + i * 9.3f, 0.5f, -14, 1f, 0.7f, 1f, 8, s.mesh.mat(Studio3D.leaf(0xFF558B2F))); }
    }

    static void gate(Studio3D.Scene s, int tod, java.util.Random rnd) {
        ground(s, Studio3D.stone(0xFFD6C6A4));
        int wall = s.mesh.mat(Studio3D.stone(0xFFEDE4D2)), door = s.mesh.mat(Studio3D.wood(0xFF7B4F2A)), gold = s.mesh.mat(Studio3D.metal(0xFFE2B84A));
        s.mesh.box(0, 3, -10, 30, 6, 1.6f, wall);
        for (int side = -1; side <= 1; side += 2) {
            s.mesh.box(side * 3.6f, 3.8f, -9.6f, 1.6f, 7.6f, 2.2f, wall);
            s.mesh.sphere(side * 3.6f, 7.9f, -9.6f, 1.1f, 0.9f, 1.1f, 10, s.mesh.mat(Studio3D.silk(0xFFE8E2D4)));
        }
        s.mesh.box(0, 7.0f, -9.6f, 8.8f, 1.2f, 2.0f, wall);
        s.mesh.box(0, 2.9f, -9.1f, 5.4f, 5.8f, 0.3f, door);
        for (int i = 0; i < 6; i++) for (int j = 0; j < 4; j++) s.mesh.sphere(-2.1f + j * 1.4f, 0.8f + i * 0.9f, -8.9f, 0.08f, 0.08f, 0.06f, 5, gold);
        s.mesh.sphere(0, 6.2f, -8.9f, 2.7f, 1.0f, 0.3f, 12, s.mesh.mat(Studio3D.stone(0xFFDCCFB8)));
        for (int side = -1; side <= 1; side += 2) { s.mesh.capsule(side * 2.6f, 0, -7.6f, side * 2.6f, 2.6f, -7.6f, 0.08f, 0.08f, 6, gold); s.mesh.sphere(side * 2.6f, 2.8f, -7.6f, 0.22f, 0.3f, 0.22f, 8, s.mesh.mat(Studio3D.glowing(0xFFFFE082, tod == Sets.NIGHT ? 0xE0FFD54F : 0x30FFD54F))); }
        hills(s, rnd, 0xFF7CB342, 5, -70);
        tree(s, rnd, -11, -6, 1.5f, 0xFF43A047, 0xFF5D4037);
        tree(s, rnd, 11.5f, -6.5f, 1.4f, 0xFF388E3C, 0xFF5D4037);
    }

    static void cave(Studio3D.Scene s, int tod, java.util.Random rnd, boolean mouth) {
        ground(s, Studio3D.stone(mouth ? 0xFF8A7A66 : 0xFF4A4238));
        int rock = s.mesh.mat(Studio3D.stone(0xFF5A4E44)), rock2 = s.mesh.mat(Studio3D.stone(0xFF6E6054));
        // walls of rock round the stage
        for (int i = 0; i < 14; i++) {
            double a = Math.PI * (0.05 + 0.9 * i / 13);
            float x = (float) Math.cos(a) * 16, z = -6 - (float) Math.sin(a) * 14;
            float r = 3 + rnd.nextFloat() * 2.5f;
            s.mesh.sphere(x, r * 0.5f, z, r * 1.3f, r * 1.6f, r, 10, i % 2 == 0 ? rock : rock2);
        }
        // the roof and stalactites
        if (!mouth) {
            s.mesh.box(0, 7.5f, -10, 60, 3, 50, rock);
            for (int i = 0; i < 9; i++) { float x = -9 + 18f * i / 8 + rnd.nextFloat(), z = -5 - rnd.nextFloat() * 10; s.mesh.capsule(x, 6.2f, z, x, 6.2f - 1.2f - rnd.nextFloat() * 1.5f, z, 0.35f, 0.02f, 8, rock2); }
            for (int i = 0; i < 5; i++) { float x = -7 + 14f * i / 4 + rnd.nextFloat(), z = -4 - rnd.nextFloat() * 6; s.mesh.capsule(x, 0, z, x, 0.6f + rnd.nextFloat() * 0.8f, z, 0.3f, 0.03f, 8, rock); }
            // crystals that glow, and a shaft of light from a crack in the roof
            for (int i = 0; i < 8; i++) { float x = -9 + 18f * i / 7, z = -7 - rnd.nextFloat() * 5; s.mesh.capsule(x, 0, z, x + 0.15f, 1.1f + rnd.nextFloat() * 0.6f, z, 0.26f, 0.03f, 6, s.mesh.mat(Studio3D.glowing(0xFF80DEEA, 0xA080DEEA))); }
            s.mesh.cylinder(2.5f, 6.2f, -9, 1.5f, 0.05f, -8, 0.6f, 1.6f, 10, s.mesh.mat(Studio3D.glowing(0xFFE8F4D8, 0x50E8F4D8)));
            s.skyTop = 0xFF08090C; s.skyBottom = 0xFF101418;
        } else {
            // the mouth: a rock arch with the bright world beyond
            s.mesh.sphere(-7, 4, -12, 6, 7, 5, 12, rock);
            s.mesh.sphere(7, 4, -12, 6, 7, 5, 12, rock2);
            s.mesh.box(0, 8.5f, -12, 18, 4, 6, rock);
            hills(s, rnd, 0xFF7CB342, 4, -60);
            tree(s, rnd, -2, -22, 1.6f, 0xFF43A047, 0xFF5D4037);
            tree(s, rnd, 3.5f, -24, 1.4f, 0xFF66BB6A, 0xFF5D4037);
        }
    }

    static void forest(Studio3D.Scene s, int tod, java.util.Random rnd) {
        ground(s, Studio3D.leaf(0xFF5E8F3A));
        for (int i = 0; i < 26; i++) {
            float x = -22 + 44f * i / 25 + rnd.nextFloat() * 3, z = -6 - rnd.nextFloat() * 22;
            if (Math.abs(x) < 3.2f && z > -9) x += x < 0 ? -3 : 3;
            tree(s, rnd, x, z, 1.4f + rnd.nextFloat() * 1.2f, i % 3 == 0 ? 0xFF2E7D32 : i % 3 == 1 ? 0xFF43A047 : 0xFF66BB6A, 0xFF5D4037);
        }
        for (int i = 0; i < 10; i++) s.mesh.sphere(-10 + 20f * i / 9 + rnd.nextFloat(), 0.35f, -4 - rnd.nextFloat() * 3, 0.8f, 0.45f, 0.7f, 8, s.mesh.mat(Studio3D.leaf(0xFF33691E)));
        for (int i = 0; i < 5; i++) s.mesh.sphere(-7 + 14f * i / 4, 0.25f, -3 - rnd.nextFloat() * 2, 0.5f, 0.35f, 0.45f, 8, s.mesh.mat(Studio3D.stone(0xFF8D8D8D)));
        s.fogStart = 10f; s.fogEnd = 45f;
        s.fogColor = Studio3D.mix(s.fogColor, 0xFF9CCC65, 0.35f);
    }

    static void hall(Studio3D.Scene s, int tod, java.util.Random rnd) {
        ground(s, Studio3D.silk(0xFFE3D5B8));
        int marble = s.mesh.mat(Studio3D.stone(0xFFF0E8DA)), gold = s.mesh.mat(Studio3D.metal(0xFFE2B84A)), red = s.mesh.mat(Studio3D.silk(0xFFB71C1C));
        s.mesh.box(0, 4.5f, -16, 40, 9, 1, marble);
        s.mesh.box(-14, 4.5f, -4, 1, 9, 26, marble);
        s.mesh.box(14, 4.5f, -4, 1, 9, 26, marble);
        s.mesh.box(0, 9.2f, -6, 40, 0.6f, 22, marble);
        for (int side = -1; side <= 1; side += 2) for (int i = 0; i < 4; i++) {
            float z = -2 - i * 4f;
            s.mesh.cylinder(side * 9f, 0, z, side * 9f, 9f, z, 0.5f, 0.45f, 12, marble);
            s.mesh.torus(side * 9f, 8.6f, z, 0.6f, 0.1f, 1, 10, gold);
            s.mesh.torus(side * 9f, 0.3f, z, 0.6f, 0.1f, 1, 10, gold);
        }
        // a red carpet to the throne
        s.mesh.box(0, 0.015f, -7, 3.2f, 0.03f, 22, red);
        s.mesh.box(0, 0.35f, -13.5f, 6, 0.7f, 3, marble);
        s.mesh.box(0, 1.2f, -14.2f, 2.2f, 1.2f, 1.2f, gold);
        s.mesh.box(0, 2.4f, -14.7f, 2.2f, 2.4f, 0.3f, red);
        s.mesh.sphere(0, 3.7f, -14.7f, 1.1f, 0.4f, 0.2f, 10, gold);
        // chandeliers
        for (int i = 0; i < 2; i++) { float z = -5 - i * 6; s.mesh.capsule(0, 9f, z, 0, 7.2f, z, 0.04f, 0.04f, 5, gold); s.mesh.sphere(0, 6.8f, z, 0.9f, 0.5f, 0.9f, 10, gold); for (int k = 0; k < 6; k++) { double a = Math.PI * 2 * k / 6; s.mesh.sphere((float) Math.cos(a) * 0.8f, 7.0f, z + (float) Math.sin(a) * 0.8f, 0.12f, 0.16f, 0.12f, 5, s.mesh.mat(Studio3D.glowing(0xFFFFF3C4, 0xC0FFE082))); } }
        // windows with light
        for (int side = -1; side <= 1; side += 2) for (int i = 0; i < 3; i++) s.mesh.box(side * 13.4f, 5.5f, -3 - i * 5f, 0.1f, 3.6f, 1.6f, s.mesh.mat(Studio3D.glowing(0xFFBFD8F0, tod == Sets.NIGHT ? 0x4060708F : 0x90FFF8E8)));
    }

    static void village(Studio3D.Scene s, int tod, java.util.Random rnd) {
        ground(s, Studio3D.stone(0xFFC9A87A));
        int mud = s.mesh.mat(Studio3D.stone(0xFFD7B38C)), thatch = s.mesh.mat(Studio3D.wood(0xFFB08A4A)), dark = s.mesh.mat(Studio3D.wood(0xFF5D4037));
        for (int i = 0; i < 6; i++) {
            float x = -16 + 32f * i / 5 + rnd.nextFloat() * 2, z = -9 - rnd.nextFloat() * 8, r = 1.8f + rnd.nextFloat() * 0.6f;
            s.mesh.cylinder(x, 0, z, x, 2.2f, z, r, r, 14, mud);
            s.mesh.cylinder(x, 2.1f, z, x, 4.2f, z, r * 1.3f, 0.05f, 14, thatch);
            s.mesh.box(x, 0.8f, z + r, 0.8f, 1.6f, 0.1f, dark);
        }
        // a well and a tree
        s.mesh.cylinder(5, 0, -5, 5, 0.9f, -5, 0.9f, 0.9f, 12, s.mesh.mat(Studio3D.stone(0xFF9E9E9E)));
        s.mesh.capsule(4.2f, 0.9f, -5, 4.2f, 2.3f, -5, 0.06f, 0.06f, 5, dark); s.mesh.capsule(5.8f, 0.9f, -5, 5.8f, 2.3f, -5, 0.06f, 0.06f, 5, dark);
        s.mesh.cylinder(4.2f, 2.3f, -5, 5.8f, 2.3f, -5, 0.1f, 0.1f, 6, dark);
        s.mesh.cylinder(5, 2.3f, -5, 5, 2.9f, -5, 1.2f, 0.05f, 10, thatch);
        tree(s, rnd, -7, -4, 1.8f, 0xFF43A047, 0xFF5D4037);
        hills(s, rnd, 0xFF8BC34A, 5, -50);
        for (int i = 0; i < 5; i++) s.mesh.sphere(-9 + 18f * i / 4, 0.25f, -3f - rnd.nextFloat(), 0.4f, 0.25f, 0.35f, 7, s.mesh.mat(Studio3D.plastic(0xFF8D6E63)));
    }

    static void rooftop(Studio3D.Scene s, int tod, java.util.Random rnd) {
        ground(s, Studio3D.stone(0xFFB9B4A8));
        int tile = s.mesh.mat(Studio3D.stone(0xFFA8A398)), rail = s.mesh.mat(Studio3D.metal(0xFFCFD8DC)), planter = s.mesh.mat(Studio3D.stone(0xFF8D8D8D));
        for (int i = -8; i <= 8; i++) s.mesh.box(i * 1.5f, 0.012f, -4, 0.04f, 0.02f, 20, tile);
        for (int i = 0; i < 10; i++) s.mesh.box(0, 0.012f, -12 + i * 1.5f, 26, 0.02f, 0.04f, tile);
        // the railing at the edge
        s.mesh.box(0, 1.05f, -8.5f, 26, 0.08f, 0.08f, rail);
        for (int i = -8; i <= 8; i++) s.mesh.capsule(i * 1.6f, 0, -8.5f, i * 1.6f, 1.05f, -8.5f, 0.03f, 0.03f, 5, rail);
        // planters with solar flowers that glow at night
        for (int i = 0; i < 6; i++) {
            float x = -9 + 18f * i / 5, z = -6.8f;
            s.mesh.box(x, 0.3f, z, 1.4f, 0.6f, 0.8f, planter);
            for (int k = 0; k < 3; k++) {
                float fx = x - 0.4f + 0.4f * k;
                s.mesh.capsule(fx, 0.6f, z, fx, 1.25f, z, 0.025f, 0.025f, 4, s.mesh.mat(Studio3D.leaf(0xFF4C8F3A)));
                s.mesh.sphere(fx, 1.3f, z, 0.14f, 0.14f, 0.06f, 8, s.mesh.mat(Studio3D.glowing(0xFF40C8FF, tod == Sets.NIGHT ? 0xD040C8FF : 0x3040C8FF)));
                s.mesh.sphere(fx, 1.3f, z + 0.05f, 0.06f, 0.06f, 0.04f, 6, s.mesh.mat(Studio3D.glowing(0xFFFFD54F, 0x80FFD54F)));
            }
        }
        // the skyline: towers with windows
        int glass = s.mesh.mat(Studio3D.metal(0xFF5C6BC0));
        for (int i = 0; i < 16; i++) {
            float x = -46 + 92f * i / 15 + rnd.nextFloat() * 4, z = -30 - rnd.nextFloat() * 50, w = 4 + rnd.nextFloat() * 5, h = 10 + rnd.nextFloat() * 28;
            int b = s.mesh.mat(Studio3D.stone(i % 3 == 0 ? 0xFF5D5F78 : i % 3 == 1 ? 0xFF6E7290 : 0xFF4E5068));
            s.mesh.box(x, h / 2 - 2, z, w, h, w, i % 4 == 0 ? glass : b);
            int win = s.mesh.mat(Studio3D.glowing(0xFFFFE082, tod == Sets.NIGHT || tod == Sets.EVENING ? 0xC0FFD54F : 0x20FFD54F));
            for (int r = 0; r < (int) (h / 2.5f); r++) for (int c = 0; c < 3; c++) if (rnd.nextFloat() < 0.7f) s.mesh.box(x - w * 0.3f + c * w * 0.3f, 0.2f + r * 2.5f, z + w / 2 + 0.03f, w * 0.14f, 1f, 0.02f, win);
        }
        s.fogStart = 20f; s.fogEnd = 90f;
    }

    static void basement(Studio3D.Scene s, int tod, java.util.Random rnd) {
        ground(s, Studio3D.stone(0xFF3A3E42));
        int wall = s.mesh.mat(Studio3D.stone(0xFF2E3338)), pillar = s.mesh.mat(Studio3D.stone(0xFF3C4248));
        s.mesh.box(0, 3, -14, 40, 6, 1, wall);
        s.mesh.box(-12, 3, -4, 1, 6, 22, wall);
        s.mesh.box(12, 3, -4, 1, 6, 22, wall);
        s.mesh.box(0, 6.2f, -4, 40, 0.5f, 22, wall);
        for (int side = -1; side <= 1; side += 2) for (int i = 0; i < 3; i++) s.mesh.box(side * 7.5f, 3, -3 - i * 4f, 0.8f, 6, 0.8f, pillar);
        // arcade machines with their screens
        for (int i = 0; i < 5; i++) {
            float x = -8 + 16f * i / 4, z = -12.6f;
            s.mesh.box(x, 1.0f, z, 1.4f, 2.0f, 1.2f, s.mesh.mat(Studio3D.plastic(i % 2 == 0 ? 0xFF4A3A6A : 0xFF3A4A7A)));
            s.mesh.box(x, 1.45f, z + 0.62f, 1.0f, 0.7f, 0.04f, s.mesh.mat(Studio3D.glowing(0xFF203050, 0x9040A0FF)));
            s.mesh.sphere(x - 0.25f, 0.95f, z + 0.65f, 0.07f, 0.07f, 0.04f, 5, s.mesh.mat(Studio3D.glowing(0xFFFF5252, 0xA0FF5252)));
            s.mesh.sphere(x + 0.25f, 0.95f, z + 0.65f, 0.07f, 0.07f, 0.04f, 5, s.mesh.mat(Studio3D.glowing(0xFF40C8FF, 0xA040C8FF)));
        }
        // a rusty shutter, server racks with blinking lights, the tubelight
        s.mesh.box(-10.5f, 1.6f, -13.4f, 3.2f, 3.2f, 0.2f, s.mesh.mat(Studio3D.metal(0xFF6D4C41)));
        for (int i = 0; i < 2; i++) { float x = 9 + i * 1.6f; s.mesh.box(x, 1.5f, -12.8f, 1.2f, 3, 1, s.mesh.mat(Studio3D.metal(0xFF37474F))); for (int k = 0; k < 6; k++) s.mesh.sphere(x - 0.3f + (k % 2) * 0.3f, 0.5f + k * 0.4f, -12.25f, 0.04f, 0.04f, 0.03f, 4, s.mesh.mat(Studio3D.glowing(k % 3 == 0 ? 0xFF69F0AE : 0xFF40C8FF, 0xA0FFFFFF))); }
        s.mesh.capsule(-3, 5.9f, -6, 3, 5.9f, -6, 0.08f, 0.08f, 6, s.mesh.mat(Studio3D.glowing(0xFFC8FFD8, 0xE0A8F0B8)));
        s.mesh.box(0, 0.012f, -6, 20, 0.02f, 0.06f, pillar);
    }

    static void room(Studio3D.Scene s, int tod, java.util.Random rnd) {
        ground(s, Studio3D.wood(0xFFB08A5A));
        int wall = s.mesh.mat(Studio3D.cloth(0xFFE3D7C8)), wood = s.mesh.mat(Studio3D.wood(0xFF8D6E4A));
        s.mesh.box(0, 2.6f, -12, 30, 5.2f, 0.6f, wall);
        s.mesh.box(-11, 2.6f, -4, 0.6f, 5.2f, 18, wall);
        s.mesh.box(11, 2.6f, -4, 0.6f, 5.2f, 18, wall);
        s.mesh.box(0, 5.3f, -4, 30, 0.4f, 18, wall);
        // a window with the sky (or night) in it
        boolean night = tod == Sets.NIGHT;
        s.mesh.box(0, 3.0f, -11.65f, 3.6f, 2.6f, 0.1f, s.mesh.mat(Studio3D.glowing(night ? 0xFF1A2A5A : 0xFFBFE0FF, night ? 0x40304880 : 0xA0FFFFFF)));
        s.mesh.box(0, 3.0f, -11.6f, 0.12f, 2.6f, 0.14f, wood); s.mesh.box(0, 3.0f, -11.6f, 3.6f, 0.12f, 0.14f, wood);
        s.mesh.box(0, 3.0f, -11.6f, 3.9f, 2.9f, 0.08f, wood);
        // shelves with books, a table with a candle, a chair, a rug
        s.mesh.box(-6.5f, 2.2f, -11.5f, 2.6f, 0.08f, 0.5f, wood); s.mesh.box(-6.5f, 3.3f, -11.5f, 2.6f, 0.08f, 0.5f, wood);
        int[] bookCols = {0xFFC62828, 0xFF1565C0, 0xFF2E7D32, 0xFFF9A825, 0xFF6A1B9A};
        for (int r = 0; r < 2; r++) for (int i = 0; i < 9; i++) s.mesh.box(-7.6f + i * 0.28f, (r == 0 ? 2.5f : 3.6f), -11.5f, 0.22f, 0.55f + (i % 3) * 0.08f, 0.4f, s.mesh.mat(Studio3D.cloth(bookCols[(i + r) % bookCols.length])));
        s.mesh.box(5.5f, 0.75f, -8.5f, 2.6f, 0.1f, 1.4f, wood);
        for (int i = 0; i < 4; i++) s.mesh.box(5.5f + (i % 2 == 0 ? -1.2f : 1.2f), 0.36f, -8.5f + (i < 2 ? -0.6f : 0.6f), 0.1f, 0.72f, 0.1f, wood);
        s.mesh.capsule(5.5f, 0.8f, -8.5f, 5.5f, 1.25f, -8.5f, 0.06f, 0.05f, 6, s.mesh.mat(Studio3D.plastic(0xFFFFF8E1)));
        s.mesh.sphere(5.5f, 1.35f, -8.5f, 0.06f, 0.11f, 0.06f, 6, s.mesh.mat(Studio3D.glowing(0xFFFFD54F, night ? 0xE0FFC107 : 0x60FFC107)));
        s.mesh.box(-3.5f, 0.5f, -9.5f, 1.0f, 0.1f, 1.0f, wood); s.mesh.box(-3.5f, 1.1f, -10f, 1.0f, 1.2f, 0.1f, wood);
        s.mesh.box(0, 0.015f, -5, 7, 0.03f, 5, s.mesh.mat(Studio3D.cloth(0xFFA23B3B)));
        s.mesh.box(0, 0.025f, -5, 6.2f, 0.03f, 4.2f, s.mesh.mat(Studio3D.cloth(0xFFD9A05B)));
        if (night) { s.keyStrength = 0.9f; s.ambient = 0.5f; s.keyColor = 0xFFFFD9A0; s.keyX = 0.8f; s.keyY = 0.5f; s.keyZ = 0.5f; s.skyColor = 0xFF6A6A90; s.groundColor = 0xFF5A4A3A; s.fillStrength = 0.4f; }
    }

    static void street(Studio3D.Scene s, int tod, java.util.Random rnd) {
        ground(s, Studio3D.stone(0xFF6E6E70));
        int kerb = s.mesh.mat(Studio3D.stone(0xFFB0ACA4)), line = s.mesh.mat(Studio3D.plastic(0xFFF5F5F5));
        s.mesh.box(0, 0.08f, -4, 60, 0.16f, 3.2f, kerb);
        for (int i = 0; i < 12; i++) s.mesh.box(-20 + i * 3.6f, 0.012f, -11, 1.8f, 0.02f, 0.14f, line);
        // buildings along the far side and the sides
        for (int i = 0; i < 9; i++) {
            float x = -26 + 52f * i / 8, w = 5f + rnd.nextFloat() * 2, h = 6 + rnd.nextFloat() * 8, z = -16 - rnd.nextFloat() * 3;
            int b = s.mesh.mat(Studio3D.stone(i % 3 == 0 ? 0xFFE0C9A6 : i % 3 == 1 ? 0xFFD4A5A5 : 0xFFB8C8D8));
            s.mesh.box(x, h / 2, z, w, h, 5, b);
            int win = s.mesh.mat(Studio3D.glowing(0xFF9FC5E8, tod == Sets.NIGHT ? 0xB0FFE082 : 0x30FFFFFF));
            for (int r = 0; r < (int) (h / 2.2f); r++) for (int c = 0; c < 3; c++) s.mesh.box(x - w * 0.3f + c * w * 0.3f, 1.2f + r * 2.2f, z + 2.53f, w * 0.16f, 1.0f, 0.04f, win);
            s.mesh.box(x, 0.9f, z + 2.55f, w * 0.3f, 1.8f, 0.06f, s.mesh.mat(Studio3D.wood(0xFF5D4037)));
        }
        // lamp posts and a tree
        for (int i = 0; i < 4; i++) { float x = -12 + i * 8; s.mesh.capsule(x, 0, -5.8f, x, 3.6f, -5.8f, 0.07f, 0.06f, 6, s.mesh.mat(Studio3D.metal(0xFF37474F))); s.mesh.sphere(x, 3.75f, -5.8f, 0.22f, 0.26f, 0.22f, 8, s.mesh.mat(Studio3D.glowing(0xFFFFF3C4, tod == Sets.NIGHT ? 0xE0FFE082 : 0x30FFE082))); }
        tree(s, rnd, 8.5f, -6.5f, 1.3f, 0xFF43A047, 0xFF5D4037);
        // a parked auto-rickshaw
        s.mesh.box(-7, 0.9f, -7.5f, 1.4f, 1.3f, 2.2f, s.mesh.mat(Studio3D.plastic(0xFFFBC02D)));
        s.mesh.box(-7, 1.65f, -7.5f, 1.3f, 0.2f, 2.0f, s.mesh.mat(Studio3D.plastic(0xFF2E7D32)));
        for (int k = 0; k < 2; k++) s.mesh.torus(-7 + (k == 0 ? -0.6f : 0.6f), 0.3f, -6.6f, 0.26f, 0.08f, 0, 8, s.mesh.mat(Studio3D.plastic(0xFF212121)));
        s.fogStart = 18f; s.fogEnd = 70f;
    }
}
