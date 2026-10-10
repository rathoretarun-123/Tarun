package com.tarun.kahani.core;

/**
 * A character built in three dimensions from its Look (the same design the drawn puppet uses, so a 3D picture
 * and a drawn one agree on height, build, costume and colours), rendered by Studio3D into a picture the rig can
 * animate: people, witches, monsters and monkeys standing front-on; animals and birds side-on (as the animal rig
 * expects), robots of metal with screen eyes. The picture comes with its eye and mouth points, so the face is
 * known exactly — no guessing from pixels.
 */
public final class Doll3D {
    private Doll3D() {}

    /** The picture of a character: pixels and the manifest's face points (fractions of the picture). */
    public static final class Result {
        public int[] px;
        public int w, h;
        public float mouthX, mouthY, mouthHW, eyeLX, eyeLY, eyeRX, eyeRY, eyeR, turbanY;
        public boolean sideView, faceKnown = true;
    }

    /** Makes the picture of a character, about 'size' pixels tall (or wide for an animal): the front view, neutral. */
    public static Result make(Look look, int size, int seed) { return make(look, size, seed, 0, Pose.NEUTRAL); }

    /**
     * The character seen from an angle (0 = the front the rig uses, 45 three-quarter, 90 side, 180 the back — the
     * master-sheet views of the handbook, ch. 3) with an expression (Pose.NEUTRAL, HAPPY, SAD, ANGRY, SURPRISED,
     * SCARED). The face points are only valid for the front view.
     */
    public static Result make(Look look, int size, int seed, float angleDeg, int emotion) { return make(look, size, seed, angleDeg, emotion, null); }

    /** With a style cue from the user's own pictures: lit from their side, graded onto their line, their skin tones. */
    public static Result make(Look look, int size, int seed, float angleDeg, int emotion, StyleCue cue) { return make(look, size, seed, angleDeg, emotion, cue, 1f); }

    /**
     * v34: light: 1 as designed; above 1 a relight after the still-picture checklist found the doll too dark or
     * flat (more exposure, a stronger fill and key); below 1 a softer light when it was harsh.
     */
    public static Result make(Look look, int size, int seed, float angleDeg, int emotion, StyleCue cue, float light) {
        if (look == null) look = new Look();
        if (cue != null && cue.skins.length > 0 && !look.skinFixed && look.kind != Look.ANIMAL && look.kind != Look.BIRD && look.kind != Look.MONSTER) {
            Look l2 = look.copy();
            l2.skin = cue.skin(seed, look.skin);
            look = l2;
        }
        Studio3D.Scene s = new Studio3D.Scene();
        boolean beast = look.kind == Look.ANIMAL || look.kind == Look.BIRD;
        float H = 1f;
        int[] marks;
        boolean rider = look.mount >= 0 && !beast && look.kind != Look.MONKEY;
        float extentW = beast ? 1.9f * H : 0.9f * H, extentH = beast ? 1.3f * H : 1.08f * H;
        if (look.kind == Look.ANIMAL) marks = animal(s, look, H);
        else if (look.kind == Look.BIRD) marks = bird(s, look, H);
        else if (rider) {
            // v34: Durga on her lion — the animal (side-on, as the doll maker draws animals) at 0.55 of the height,
            // the character built seated (thighs forward, shins hanging) and lifted so she sits on its back, her legs along its side
            Look ml = new Look();
            ml.kind = look.mount >= 20 ? Look.BIRD : Look.ANIMAL; ml.species = look.mount; ml.furColor = Look.furOf(look.mount);
            ml.skin = Studio3D.shade(ml.furColor, 1.25f); ml.eyeColor = 0xFF2B1B10;
            float Hm = H * (ml.kind == Look.BIRD ? 0.5f : 0.55f);
            // the animal first, then the rider: the builders put their eyes and mouth first in the marks, so the
            // rider's become the picture's face points; only the rider's own vertices and marks are lifted onto the back
            if (ml.kind == Look.BIRD) bird(s, ml, Hm); else animal(s, ml, Hm);
            java.util.Set<float[]> animalMarks = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<float[], Boolean>());
            animalMarks.addAll(s.marks);
            int v0 = s.mesh.nv;
            float Hr = 0.78f * H;
            marks = human(s, look, Hr, emotion, true);
            float back = (ml.species == Look.SP_RABBIT || ml.species == Look.SP_MOUSE) ? Hm * 0.72f : ml.kind == Look.BIRD ? Hm * 0.6f : Hm * 0.85f;
            float lift = back - Hr * 0.37f;                                   // the seat on the animal's back, the shins along its side
            for (int i = v0; i < s.mesh.nv; i++) { s.mesh.v[i * 3 + 1] += lift; s.mesh.v[i * 3 + 2] += 0.06f * H; }
            for (float[] mk : s.marks) if (!animalMarks.contains(mk)) { mk[1] += lift; mk[2] += 0.06f * H; }
            extentW = 1.9f * Hm; extentH = back + Hr * 0.66f + 0.02f * H;
        }
        else if (look.aid == Look.AID_WHEELCHAIR) {
            // v34: seated in a wheelchair — the seated figure lowered onto the chair's seat, the chair built round it
            int v0 = s.mesh.nv;
            java.util.Set<float[]> before = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<float[], Boolean>());
            before.addAll(s.marks);
            marks = human(s, look, H, emotion, true);
            float drop = look.isChild() ? -0.09f * H : -0.13f * H;
            for (int i = v0; i < s.mesh.nv; i++) s.mesh.v[i * 3 + 1] += drop;
            for (float[] mk : s.marks) if (!before.contains(mk)) mk[1] += drop;
            wheelchair(s.mesh, H, look.isChild() ? 0.8f : 1f);
            extentH = 0.95f * H;
        }
        else marks = human(s, look, H, emotion);
        // a long lens far away: no distortion of the face (handbook ch. 5), the same as a drawn front view;
        // the camera (with its lights) walks round the character for the other views
        float dist = 12f * H;
        double a = Math.toRadians(angleDeg);
        float sn = (float) Math.sin(a), cs = (float) Math.cos(a);
        s.camX = dist * sn; s.camY = extentH * 0.48f; s.camZ = dist * cs;
        s.lookX = 0; s.lookY = extentH * 0.48f; s.lookZ = 0;
        if (!beast && !rider && look.heads > 1) {
            // v34: the row of heads is wider than the shoulders: the picture widens to hold it (a child's head is larger)
            float hr = (look.isChild() ? 0.165f : look.kind == Look.MONSTER ? 0.125f : 0.12f) * H;
            extentW = Math.max(extentW, 2 * (headSpacing((Math.min(9, look.heads - 1) + 1) / 2, hr) + 1.25f * hr));
        }
        int w = beast ? size : rider ? size : Math.round(size * 2 / 3f), h = beast ? Math.round(size * 2 / 3f) : rider ? Math.round(size * 0.9f) : size;
        if (angleDeg != 0 && !beast && !rider) w = Math.round(size * 0.75f);
        if (!beast && !rider && look.heads > 1) w = Math.max(w, Math.round(size * Math.min(2.4f, extentW / extentH)));
        float half = Math.max(extentH / 2f, extentW / 2f * h / (float) w) * 1.04f;
        s.fovDeg = (float) Math.toDegrees(2 * Math.atan(half / dist));
        float side = cue != null && cue.lightSure > 0.3f ? cue.lightSide : -1;      // the key light from the references' side
        float kx = 0.5f * side, ky = 0.9f, kz = 0.8f, rx = -0.75f * side, ry = 0.4f, rz = -0.5f;
        s.keyX = kx * cs + kz * sn; s.keyY = ky; s.keyZ = -kx * sn + kz * cs;
        s.rimX = rx * cs + rz * sn; s.rimY = ry; s.rimZ = -rx * sn + rz * cs;
        s.skyTop = 0;
        s.ao = true; s.shadows = true;
        s.aoRadius = 0.045f * H;
        // v34: soft shadow edges on every doll — the hair's shadow across a face read as a hard line (a scar), and a
        // rider's shadow on her animal as a dark stain; a rider's animal gets a little more fill too
        s.shadowSoft = 2;
        if (rider) s.fillStrength *= 1.25f;
        if (light > 1f) { s.exposure = Math.min(1.5f, light); s.fillStrength *= 1.35f; s.keyStrength *= 1.1f; }
        else if (light < 1f) { s.fillStrength *= 1.3f; s.keyStrength *= Math.max(0.7f, light); s.rimStrength *= 0.85f; }
        // v34 (the still-picture manual: at least 2048 px): a large doll is drawn without supersampling (the same
        // work as a 1100-px doll at 2x2; the film shrinks it anyway); a small one keeps the 2x2 for clean edges
        Studio3D.Picture p = Studio3D.crop(Studio3D.render(s, w, h, Math.max(w, h) > 1400 ? 1 : 2), 3);
        if (cue != null) cue.grade(p.px, p.w, p.h);
        Result r = new Result();
        r.px = p.px; r.w = p.w; r.h = p.h;
        r.sideView = beast;
        r.faceKnown = angleDeg == 0;
        // marks: 0 eye L, 1 eye R, 2 mouth, 3 mouth corner, 4 eye edge, 5 turban bottom (or none)
        r.eyeLX = p.marks[0][0]; r.eyeLY = p.marks[0][1];
        r.eyeRX = p.marks[1][0]; r.eyeRY = p.marks[1][1];
        r.mouthX = p.marks[2][0]; r.mouthY = p.marks[2][1];
        r.mouthHW = Math.abs(p.marks[3][0] - p.marks[2][0]);
        r.eyeR = Math.abs(p.marks[4][0] - p.marks[0][0]);
        r.turbanY = marks[0] == 1 ? p.marks[5][1] : 0;
        return r;
    }

    /** The handbook's master-sheet views: front, three-quarter, side and back, side by side on one plate (fractions of 'size' tall). */
    public static Result masterSheet(Look look, int size, int seed) {
        float[] angles = {0, 45, 90, 180};
        Result[] views = new Result[angles.length];
        int w = 0, h = 0;
        for (int i = 0; i < angles.length; i++) { views[i] = make(look, size, seed, angles[i], Pose.NEUTRAL); w += views[i].w + size / 12; h = Math.max(h, views[i].h); }
        w += size / 12;
        h += size / 8;
        Result r = new Result();
        r.w = w; r.h = h; r.px = new int[w * h];
        java.util.Arrays.fill(r.px, 0xFFE4EAF0);
        int x = size / 12;
        for (Result v : views) {
            int top = h - size / 16 - v.h;
            for (int yy = 0; yy < v.h; yy++) for (int xx = 0; xx < v.w; xx++) {
                int c = v.px[yy * v.w + xx], al = c >>> 24;
                if (al == 0) continue;
                int i = (top + yy) * w + x + xx;
                r.px[i] = al == 255 ? c : Studio3D.mix(r.px[i], c, al / 255f);
            }
            x += v.w + size / 12;
        }
        return r;
    }

    // ================================================================== people

    /**
     * v34: the torso from y0 up to the shoulders: a tube with a round bottom and LOW rounded shoulders (an ellipsoid
     * at most a third of the head's radius tall). A capsule's half-sphere top (as wide as the shoulders) rose into
     * the face of an adult and buried a monster's whole head — found by the still-picture checklist (no catch-light,
     * a face that was not there).
     */
    static void torso(Studio3D.Mesh m, float y0, float shY, float r0, float r1, float headR, int seg, int mat) {
        m.cylinder(0, y0, 0, 0, shY, 0, r0, r1, seg, mat);
        m.sphere(0, y0, 0, r0, r0, r0, seg, mat);
        float ry = Math.min(r1 * 0.35f, headR * 0.35f);
        m.sphere(0, shY, 0, r1, ry, r1, seg + 4, mat);
    }

    /** Builds a standing person (or witch, monster, monkey); returns {1 if a turban mark was added}. */
    static int[] human(Studio3D.Scene s, Look l, float H) { return human(s, l, H, Pose.NEUTRAL); }

    static int[] human(Studio3D.Scene s, Look l, float H, int emotion) { return human(s, l, H, emotion, false); }

    /** seated: a rider — the thighs come forward to the knees and the shins hang (the saree drapes over the lap). */
    static int[] human(Studio3D.Scene s, Look l, float H, int emotion, boolean seated) {
        Studio3D.Mesh m = s.mesh;
        // the expression: how the brows tilt (inner ends up = sad, down = angry), how the mouth curves, how open the eyes are
        float browIn = emotion == Pose.SAD || emotion == Pose.SCARED ? 0.22f : emotion == Pose.ANGRY || emotion == Pose.EVIL ? -0.25f : emotion == Pose.SURPRISED ? 0.12f : 0;
        float browUp = emotion == Pose.SURPRISED || emotion == Pose.SCARED ? 0.18f : emotion == Pose.ANGRY ? -0.1f : 0;
        float smile = emotion == Pose.HAPPY || emotion == Pose.LAUGH ? 1f : emotion == Pose.SAD || emotion == Pose.SCARED ? -0.8f : emotion == Pose.ANGRY ? -0.45f : 0;
        float open = emotion == Pose.LAUGH || emotion == Pose.SURPRISED || emotion == Pose.SCARED ? 1f : 0;
        float lidDown = emotion == Pose.ANGRY || emotion == Pose.EVIL ? 0.35f : emotion == Pose.SAD ? 0.2f : 0;
        boolean child = l.isChild(), monster = l.kind == Look.MONSTER, witch = l.kind == Look.WITCH, monkey = l.kind == Look.MONKEY, old = l.kind == Look.OLD_MAN;
        float headR, L, T, neck;
        if (child) { headR = 0.165f * H; L = 0.29f * H; T = 0.29f * H; neck = 0.02f * H; }
        else if (monster) { headR = 0.125f * H; L = 0.33f * H; T = 0.38f * H; neck = 0f; }
        else if (witch) { headR = 0.125f * H; L = 0.37f * H; T = 0.31f * H; neck = 0.02f * H; }
        else if (monkey) { headR = 0.15f * H; L = 0.30f * H; T = 0.30f * H; neck = 0.02f * H; }
        else { headR = 0.12f * H; L = 0.40f * H; T = 0.31f * H; neck = 0.03f * H; }
        float girth = l.girth;
        float sw = T * (child ? 0.42f : 0.46f) * girth;
        if (l.female && !child) sw *= 0.9f;
        float hw = sw * (l.female ? 0.95f : 0.82f);
        float hipY = L, shY = L + T, headY = shY + neck + headR * 0.92f;
        float armLen = T * 1.02f, armW = headR * (monster ? 0.5f : 0.36f), legW = headR * (monster ? 0.5f : 0.42f);
        int skin = m.mat(monkey ? Studio3D.fur(l.furColor) : Studio3D.skin(l.skin));
        int hair = m.mat(Studio3D.hair(l.hairColor));
        boolean silky = l.outfit == Look.O_LEHENGA || l.outfit == Look.O_SAREE || l.outfit == Look.O_ACHKAN;
        int top = m.mat(l.outfit == Look.O_ARMOR ? Studio3D.metal(l.primary) : silky ? Studio3D.silk(l.primary) : Studio3D.cloth(l.primary));
        int bottom = m.mat(silky ? Studio3D.silk(l.secondary) : Studio3D.cloth(l.secondary));
        int accent = m.mat(Studio3D.silk(l.accent));
        int shoe = m.mat(Studio3D.plastic(l.shoeColor));
        int gold = m.mat(Studio3D.metal(0xFFE2B84A));
        // v34 (the still-picture manual: big expressive eyes with highlights): the whites glow a little so they read
        // white in the shade of the brow, and each eye gets a strong catch-light and a small second one
        Studio3D.Material whiteM = Studio3D.eye(0xFFFAFAF8);
        whiteM.emissive = 0x30FFFFFF;
        int white = m.mat(whiteM);
        int catchLight = m.mat(Studio3D.glow(0xFFFFFFFF));
        int iris = m.mat(l.glowEyes ? Studio3D.glowing(l.eyeColor, 0xC0FF5030) : Studio3D.eye(l.eyeColor));
        int pupil = m.mat(Studio3D.eye(0xFF101010));
        int lip = m.mat(Studio3D.skin(Studio3D.mix(l.skin, 0xFFB03A3A, 0.45f)));
        int seg = 14;

        // ---- legs and feet (trousers, a skirt, bare legs)
        boolean skirt = l.outfit == Look.O_LEHENGA || l.outfit == Look.O_SAREE || l.outfit == Look.O_FROCK || (l.outfit == Look.O_KURTA && l.female && !child);
        boolean shorts = l.outfit == Look.O_TSHIRT;
        boolean trousers = !skirt && !shorts;
        float kneeY = hipY - 0.01f * H, kneeZ = 0.19f * H, footY = hipY - 0.52f * L;    // a seated rider's knees and feet
        for (int side = -1; side <= 1; side += 2) {
            float lx = side * hw * 0.5f;
            int legMat = trousers ? bottom : skin;
            if (seated) {
                m.capsule(lx, hipY + 0.02f * H, 0, lx * 1.05f, kneeY, kneeZ, legW * 0.56f, legW * 0.5f, seg, legMat);
                m.capsule(lx * 1.05f, kneeY, kneeZ, lx * 1.05f, footY + 0.05f * H, kneeZ + 0.01f * H, legW * 0.5f, legW * 0.42f, seg, legMat);
                int shoeMat = l.lightShoes ? m.mat(Studio3D.glowing(l.shoeColor, 0x8040C8FF)) : shoe;
                m.sphere(lx * 1.05f, footY + 0.035f * H, kneeZ + 0.035f * H, legW * 0.62f, 0.035f * H, legW * 1.05f, seg, shoeMat);
                if (l.anklets) m.torus(lx * 1.05f, footY + 0.075f * H, kneeZ + 0.01f * H, legW * 0.5f, legW * 0.06f, 1, 8, gold);
                continue;
            }
            m.capsule(lx, hipY + 0.02f * H, 0, lx * 1.05f, 0.06f * H, 0.01f * H, legW * 0.52f, legW * 0.42f, seg, legMat);
            if (shorts) m.capsule(lx, hipY + 0.02f * H, 0, lx * 1.03f, hipY - 0.45f * L, 0, legW * 0.6f, legW * 0.56f, seg, bottom);
            // the shoe
            int shoeMat = l.lightShoes ? m.mat(Studio3D.glowing(l.shoeColor, 0x8040C8FF)) : shoe;
            m.sphere(lx * 1.05f, 0.035f * H, 0.035f * H, legW * 0.62f, 0.035f * H, legW * 1.05f, seg, shoeMat);
            if (l.anklets) m.torus(lx * 1.05f, 0.075f * H, 0.01f * H, legW * 0.5f, legW * 0.06f, 1, 8, gold);
        }
        // ---- the body
        if (skirt) {
            float waist = hipY + T * 0.25f;
            boolean wide = l.outfit == Look.O_LEHENGA || l.outfit == Look.O_FROCK;
            if (seated) {
                // to the seat, over the lap to the knees, then falling over the shins
                m.cylinder(0, waist, 0, 0, hipY - 0.02f * H, 0, hw * 1.05f, hw * 1.2f, 24, bottom);
                m.capsule(0, hipY, 0.01f * H, 0, kneeY, kneeZ, hw * 1.0f, hw * 1.02f, 20, bottom);
                m.cylinder(0, kneeY, kneeZ, 0, footY + 0.07f * H, kneeZ + 0.02f * H, hw * 1.02f, hw * (wide ? 1.7f : 1.3f), 24, bottom);
            } else
            m.cylinder(0, waist, 0, 0, 0.03f * H, 0, hw * 1.05f, hw * (wide ? 2.4f : 1.35f), 24, bottom);
            torso(m, hipY, shY, hw * 1.0f, sw * 0.98f, headR, seg, top);
            if (l.outfit == Look.O_SAREE) m.capsule(-sw * 0.8f, shY + 0.02f * H, 0.08f * H, hw * 0.6f, waist - 0.02f * H, 0.1f * H, 0.04f * H, 0.05f * H, seg, accent);
            if (l.outfit == Look.O_LEHENGA) m.torus(0, waist, 0, hw * 1.05f, 0.012f * H, 1, 10, gold);
        } else {
            boolean longTop = l.outfit == Look.O_KURTA || l.outfit == Look.O_ACHKAN || l.outfit == Look.O_COAT || l.outfit == Look.O_CLOAK;
            float bottomY = longTop ? hipY - L * (l.outfit == Look.O_COAT || l.outfit == Look.O_ACHKAN ? 0.55f : 0.3f) : hipY - 0.03f * H;
            if (seated) bottomY = Math.max(bottomY, hipY - 0.03f * H);               // a rider's coat ends at the seat
            if (longTop) {
                // the chest, then a coat that widens to its hem, with a sash or a belt at the waist
                torso(m, hipY + T * 0.15f, shY, hw * 1.0f, sw * 0.98f, headR, seg, top);
                m.cylinder(0, hipY + T * 0.2f, 0, 0, bottomY, 0, hw * 1.02f, hw * 1.45f, 20, top);
                m.torus(0, hipY + T * 0.2f, 0, hw * 1.04f, 0.014f * H, 1, 12, l.outfit == Look.O_ACHKAN ? gold : accent);
                m.capsule(0, bottomY - 0.004f * H, 0, 0, bottomY + 0.004f * H, 0, hw * 1.45f, hw * 1.45f, 20, top);
            } else torso(m, bottomY, shY, hw * 1.02f, sw * 0.98f, headR, seg, top);
            if (l.outfit == Look.O_SUIT) {
                m.capsule(0, shY - T * 0.1f, sw * 0.95f, 0, hipY + T * 0.2f, hw * 0.95f, 0.025f * H, 0.028f * H, 8, accent);   // the tie
                m.capsule(0, shY - T * 0.05f, sw * 0.9f, 0, hipY + T * 0.1f, hw * 0.9f, 0.02f * H, 0.02f * H, 8, m.mat(Studio3D.cloth(0xFFF4F4F4)));
            }
            if (l.outfit == Look.O_HOODIE) m.box(0, hipY + T * 0.12f, hw * 0.9f, hw * 1.2f, T * 0.22f, 0.03f * H, top);           // the pocket
            if (l.outfit == Look.O_ARMOR) m.torus(0, hipY + T * 0.05f, 0, hw * 1.1f, 0.02f * H, 1, 10, gold);
            if (l.outfit == Look.O_UNIFORM || l.outfit == Look.O_ACHKAN) {
                for (int i = 0; i < 4; i++) m.sphere(0, shY - T * (0.18f + 0.18f * i), hw * 1.05f, 0.012f * H, 0.012f * H, 0.012f * H, 6, gold);
            }
            if (l.outfit == Look.O_CLOAK) m.capsule(0, shY + 0.02f * H, -sw * 0.3f, 0, 0.05f * H, -hw * 0.9f, sw * 1.15f, hw * 1.6f, seg, bottom);
        }
        if (l.satchel) { m.box(hw * 0.95f, hipY + T * 0.05f, 0.02f * H, 0.11f * H, 0.1f * H, 0.05f * H, m.mat(Studio3D.wood(0xFF8D6E4A))); m.capsule(-sw * 0.9f, shY + 0.02f * H, 0.03f * H, hw * 0.95f, hipY + T * 0.1f, 0.05f * H, 0.012f * H, 0.012f * H, 6, m.mat(Studio3D.wood(0xFF6D4C2A))); }
        if (l.chains) for (int i = 0; i < 3; i++) m.torus(hw * 0.2f * (i - 1), hipY + T * 0.5f - 0.02f * H * i, hw * 0.95f, 0.03f * H, 0.008f * H, 2, 8, m.mat(Studio3D.metal(0xFF7A7A7A)));

        // ---- arms: an open A-pose so the rig finds them (short sleeves show the skin)
        boolean shortSleeves = l.outfit == Look.O_TSHIRT || l.outfit == Look.O_LEHENGA || l.outfit == Look.O_FROCK || (l.outfit == Look.O_SAREE);
        // v34: a many-armed character (Durga): the other pairs fan out behind the front pair, each raised more
        int pairs = Math.max(1, l.arms / 2);
        for (int k = 1; k < pairs; k++) for (int side = -1; side <= 1; side += 2) {
            float sx = side * sw * 0.95f, sy = shY - armW * 0.3f;
            double th = Math.toRadians(Math.min(150, 30 + 24 * k));
            float ux = (float) Math.sin(th) * side, uy = (float) Math.cos(th);                    // the upper arm's direction, turned up by th
            float ex = sx + ux * armLen * 0.5f, ey = sy + uy * armLen * 0.5f;
            double th2 = th + Math.toRadians(18);
            float hx = ex + (float) Math.sin(th2) * side * armLen * 0.5f, hy = ey + (float) Math.cos(th2) * armLen * 0.5f;
            float z = -0.03f * H * k;
            m.capsule(sx, sy, z, ex, ey, z, armW * 0.5f, armW * 0.44f, seg, top);
            m.capsule(ex, ey, z, hx, hy, z, armW * 0.44f, armW * 0.36f, seg, shortSleeves ? skin : top);
            m.sphere(hx, hy + armW * 0.1f, z, armW * 0.56f, armW * 0.64f, armW * 0.4f, seg, skin);
        }
        for (int side = -1; side <= 1; side += 2) {
            float sx = side * sw * 0.95f, sy = shY - armW * 0.3f;
            float ex = side * (sw + armLen * 0.22f), ey = sy - armLen * 0.45f;
            float hx = side * (sw + armLen * 0.34f), hy = ey - armLen * 0.46f;
            m.capsule(sx, sy, 0, ex, ey, 0.01f * H, armW * 0.55f, armW * 0.48f, seg, top);
            m.capsule(ex, ey, 0.01f * H, hx, hy, 0.03f * H, armW * 0.48f, armW * 0.4f, seg, shortSleeves ? skin : top);
            // the hand
            m.sphere(hx, hy - armW * 0.3f, 0.03f * H, armW * 0.62f, armW * 0.72f, armW * 0.45f, seg, skin);
            if (l.bangles) for (int i = 0; i < 2; i++) m.torus(hx, hy + armW * 0.25f + i * armW * 0.3f, 0.03f * H, armW * 0.5f, armW * 0.07f, 1, 8, gold);
            if (l.longNails) for (int i = 0; i < 3; i++) m.capsule(hx + (i - 1) * armW * 0.2f, hy - armW * 0.9f, 0.04f * H, hx + (i - 1) * armW * 0.24f, hy - armW * 1.35f, 0.05f * H, armW * 0.07f, 0.01f, 6, m.mat(Studio3D.plastic(0xFF3A2A2A)));
            // things held
            if (side == 1) props(m, l, hx, hy, H, armW, gold);
            if (side == -1 && l.shield) m.disc(hx - armW * 0.2f, hy + armW * 0.2f, 0.09f * H, 0.1f * H, 2, 16, m.mat(Studio3D.metal(0xFF3F6FB5)));
        }
        // v34: a walking stick in the right hand down to the ground (its crook over the hand); crutches under both arms
        if (!seated && (l.aid == Look.AID_STICK || l.aid == Look.AID_CRUTCHES)) {
            int wood = m.mat(l.aid == Look.AID_STICK ? Studio3D.wood(0xFF6D4C41) : Studio3D.metal(0xFF90A4AE));
            for (int side = -1; side <= 1; side += 2) {
                if (l.aid == Look.AID_STICK && side < 0) continue;
                float sy = shY - armW * 0.3f, ey = sy - armLen * 0.45f;
                float hx = side * (sw + armLen * 0.34f), hy = ey - armLen * 0.46f;
                if (l.aid == Look.AID_STICK) {
                    m.capsule(hx, hy, 0.045f * H, hx + 0.02f * H, 0.012f * H, 0.07f * H, 0.011f * H, 0.011f * H, 8, wood);
                    m.capsule(hx, hy, 0.045f * H, hx - 0.045f * H, hy + 0.02f * H, 0.045f * H, 0.011f * H, 0.011f * H, 8, wood);
                    m.sphere(hx + 0.02f * H, 0.012f * H, 0.07f * H, 0.014f * H, 0.01f * H, 0.014f * H, 6, m.mat(Studio3D.plastic(0xFF263238)));
                } else {
                    float tx = side * sw * 0.95f, ty = sy - 0.05f * H;
                    m.capsule(tx, ty, 0.02f * H, hx + side * 0.01f * H, 0.012f * H, 0.05f * H, 0.01f * H, 0.01f * H, 8, wood);
                    m.capsule(tx - 0.03f * H, ty, 0.02f * H, tx + 0.03f * H, ty, 0.02f * H, 0.016f * H, 0.016f * H, 8, m.mat(Studio3D.plastic(0xFF455A64)));
                    m.capsule(hx - 0.025f * H, hy, 0.04f * H, hx + 0.025f * H, hy, 0.04f * H, 0.012f * H, 0.012f * H, 8, m.mat(Studio3D.plastic(0xFF455A64)));
                }
            }
        }
        // ---- neck and head
        m.capsule(0, shY - 0.01f * H, 0, 0, headY - headR * 0.6f, 0, headR * 0.34f, headR * 0.36f, seg, skin);
        int hv0 = m.nv, ht0 = m.nt;                         // v34: where the head begins (copied for the other heads)
        m.sphere(0, headY, 0, headR * 0.98f, headR * 1.02f, headR, 22, skin);
        // ears
        m.sphere(-headR * 0.97f, headY - headR * 0.05f, 0, headR * 0.14f, headR * 0.22f, headR * 0.1f, 8, skin);
        m.sphere(headR * 0.97f, headY - headR * 0.05f, 0, headR * 0.14f, headR * 0.22f, headR * 0.1f, 8, skin);
        if (monkey) { m.sphere(-headR * 1.05f, headY, 0, headR * 0.3f, headR * 0.32f, headR * 0.12f, 8, skin); m.sphere(headR * 1.05f, headY, 0, headR * 0.3f, headR * 0.32f, headR * 0.12f, 8, skin); }
        // the face
        float eyeY = headY + headR * 0.08f, eyeX = headR * 0.38f, eyeZ = headR * 0.84f, eyeRad = headR * (child ? 0.19f : 0.16f);
        if (monster) eyeRad = headR * 0.14f;
        for (int side = -1; side <= 1; side += 2) {
            float ex = side * eyeX;
            m.sphere(ex, eyeY, eyeZ, eyeRad, eyeRad * 1.05f, eyeRad * 0.75f, 12, white);
            m.sphere(ex, eyeY, eyeZ + eyeRad * 0.62f, eyeRad * 0.5f, eyeRad * 0.5f, eyeRad * 0.22f, 10, iris);
            m.sphere(ex, eyeY, eyeZ + eyeRad * 0.8f, eyeRad * 0.24f, eyeRad * 0.24f, eyeRad * 0.1f, 8, pupil);
            m.sphere(ex - side * eyeRad * 0.18f, eyeY + eyeRad * 0.2f, eyeZ + eyeRad * 0.92f, eyeRad * 0.15f, eyeRad * 0.15f, eyeRad * 0.06f, 8, catchLight);
            m.sphere(ex + side * eyeRad * 0.16f, eyeY - eyeRad * 0.2f, eyeZ + eyeRad * 0.9f, eyeRad * 0.07f, eyeRad * 0.07f, eyeRad * 0.04f, 6, catchLight);
            // the eyelid (a half-closed eye for anger or sorrow)
            if (lidDown > 0) m.sphere(ex, eyeY + eyeRad * (1.05f - lidDown * 0.9f), eyeZ + eyeRad * 0.1f, eyeRad * 1.08f, eyeRad * 0.55f, eyeRad * 0.8f, 10, skin);
            // eyebrow: tilted by the expression (a lowered one for anger-prone looks: villains)
            float browY = eyeY + eyeRad * (1.45f + browUp * 2) + (l.hero ? 0 : -eyeRad * 0.15f);
            float inner = ex - side * eyeRad * 1.1f, outer = ex + side * eyeRad * 1.0f;
            // the inner end is the one nearer the nose: side -1 is the left eye, whose inner end is toward +x
            float innerX = ex + side * eyeRad * 1.05f * (side < 0 ? 1 : -1) * -1, outerX = ex - side * eyeRad * 1.1f * (side < 0 ? 1 : -1) * -1;
            innerX = ex + (side < 0 ? 1 : -1) * eyeRad * 1.05f; outerX = ex - (side < 0 ? 1 : -1) * eyeRad * 1.1f;
            m.capsule(innerX, browY + eyeRad * browIn * 2 - (l.hero ? 0 : eyeRad * 0.25f), eyeZ * 1.02f, outerX, browY - eyeRad * browIn + (l.hero ? 0 : eyeRad * 0.15f), eyeZ * 1.02f, eyeRad * 0.14f, eyeRad * 0.1f, 6, hair);
        }
        // nose, mouth, cheeks
        float noseX = l.crookedNose ? headR * 0.06f : 0;
        m.sphere(noseX, headY - headR * 0.12f, headR * 0.97f, headR * (l.crookedNose ? 0.13f : 0.1f), headR * (l.crookedNose ? 0.16f : 0.11f), headR * 0.1f, 8, skin);
        float mouthY = headY - headR * 0.42f, mouthHW = headR * 0.24f;
        if (open > 0) {
            m.sphere(0, mouthY - headR * 0.03f, headR * 0.86f, mouthHW * 0.8f, headR * 0.1f, headR * 0.06f, 10, m.mat(Studio3D.skin(0xFF5A2A2A)));
            m.capsule(-mouthHW * 0.85f, mouthY + headR * 0.06f, headR * 0.9f, mouthHW * 0.85f, mouthY + headR * 0.06f, headR * 0.9f, headR * 0.035f, headR * 0.035f, 6, lip);
        } else {
            // three pieces, the ends lifted for a smile or dropped for a frown
            float lift = headR * 0.09f * smile, wide = mouthHW * (1 + 0.25f * Math.max(0, smile));
            m.capsule(-wide, mouthY + lift, headR * 0.88f, -wide * 0.35f, mouthY - lift * 0.15f, headR * 0.9f, headR * 0.03f, headR * 0.04f, 6, lip);
            m.capsule(-wide * 0.35f, mouthY - lift * 0.15f, headR * 0.9f, wide * 0.35f, mouthY - lift * 0.15f, headR * 0.9f, headR * 0.04f, headR * 0.04f, 6, lip);
            m.capsule(wide * 0.35f, mouthY - lift * 0.15f, headR * 0.9f, wide, mouthY + lift, headR * 0.88f, headR * 0.04f, headR * 0.03f, 6, lip);
            if (smile > 0.5f) m.capsule(-wide * 0.6f, mouthY - lift * 0.1f, headR * 0.9f, wide * 0.6f, mouthY - lift * 0.1f, headR * 0.9f, headR * 0.025f, headR * 0.025f, 5, m.mat(Studio3D.eye(0xFFFFFFF4)));
        }
        if (l.dimples) { m.sphere(-headR * 0.42f, mouthY - headR * 0.02f, headR * 0.86f, headR * 0.03f, headR * 0.03f, headR * 0.02f, 5, lip); m.sphere(headR * 0.42f, mouthY - headR * 0.02f, headR * 0.86f, headR * 0.03f, headR * 0.03f, headR * 0.02f, 5, lip); }
        if (l.fangs) { int w2 = m.mat(Studio3D.eye(0xFFFFFFF0)); m.capsule(-mouthHW * 0.5f, mouthY - headR * 0.02f, headR * 0.9f, -mouthHW * 0.5f, mouthY - headR * 0.16f, headR * 0.9f, headR * 0.035f, 0.001f, 6, w2); m.capsule(mouthHW * 0.5f, mouthY - headR * 0.02f, headR * 0.9f, mouthHW * 0.5f, mouthY - headR * 0.16f, headR * 0.9f, headR * 0.035f, 0.001f, 6, w2); }
        if (l.mustache > 0) {
            float my = mouthY + headR * 0.12f;
            m.capsule(-headR * (l.mustache == 2 ? 0.42f : 0.3f), my + (l.mustache == 2 ? headR * 0.06f : 0), headR * 0.9f, 0, my - headR * 0.02f, headR * 0.93f, headR * 0.04f, headR * 0.06f, 6, hair);
            m.capsule(headR * (l.mustache == 2 ? 0.42f : 0.3f), my + (l.mustache == 2 ? headR * 0.06f : 0), headR * 0.9f, 0, my - headR * 0.02f, headR * 0.93f, headR * 0.04f, headR * 0.06f, 6, hair);
        }
        if (l.beard) m.sphere(0, headY - headR * 0.72f, headR * 0.45f, headR * 0.62f, headR * 0.42f, headR * 0.5f, 12, hair);
        if (l.bindi > 0) m.sphere(0, headY + headR * 0.42f, headR * 0.93f, headR * 0.05f, headR * 0.05f, headR * 0.03f, 6, m.mat(Studio3D.plastic(0xFFC62828)));
        if (l.tilak > 0) m.capsule(0, headY + headR * 0.3f, headR * 0.95f, 0, headY + headR * 0.55f, headR * 0.9f, headR * 0.035f, headR * 0.035f, 6, m.mat(Studio3D.plastic(0xFFE65100)));
        if (l.wrinkles || old) { for (int i = 0; i < 2; i++) m.capsule(-headR * 0.2f, headY + headR * (0.5f + 0.1f * i), headR * 0.86f, headR * 0.2f, headY + headR * (0.5f + 0.1f * i), headR * 0.86f, headR * 0.015f, headR * 0.015f, 4, m.mat(Studio3D.skin(Studio3D.shade(l.skin, 0.8f)))); }
        if (l.scar) m.capsule(headR * 0.3f, headY + headR * 0.3f, headR * 0.92f, headR * 0.5f, headY - headR * 0.05f, headR * 0.85f, headR * 0.02f, headR * 0.02f, 4, m.mat(Studio3D.skin(Studio3D.shade(l.skin, 0.75f))));
        if (l.glasses > 0) {
            int frame = m.mat(l.glasses == 2 ? (l.glowGlasses ? Studio3D.glowing(0xFF202428, 0x90FF3020) : Studio3D.plastic(0xFF202428)) : Studio3D.metal(0xFF8A7A3A));
            for (int side = -1; side <= 1; side += 2) m.torus(side * eyeX, eyeY, eyeZ + eyeRad * 0.95f, eyeRad * 1.25f, eyeRad * 0.09f, 2, 10, frame);
            m.capsule(-eyeX + eyeRad * 1.25f, eyeY, eyeZ + eyeRad * 0.95f, eyeX - eyeRad * 1.25f, eyeY, eyeZ + eyeRad * 0.95f, eyeRad * 0.07f, eyeRad * 0.07f, 4, frame);
            if (l.glasses == 2) for (int side = -1; side <= 1; side += 2) m.disc(side * eyeX, eyeY, eyeZ + eyeRad * 0.97f, eyeRad * 1.2f, 2, 10, m.mat(Studio3D.plastic(0xFF1A1E24)));
            if (l.glasses == 3) {
                // v34: goggles — thick round rims on a strap round the head
                int rim = m.mat(Studio3D.plastic(0xFF5D4037));
                for (int side = -1; side <= 1; side += 2) m.torus(side * eyeX, eyeY, eyeZ + eyeRad * 1.05f, eyeRad * 1.55f, eyeRad * 0.26f, 2, 12, rim);
                m.torus(0, eyeY, 0, headR * 1.03f, headR * 0.06f, 1, 20, m.mat(Studio3D.cloth(0xFF3E2723)));
            }
        }
        if (l.earrings) for (int side = -1; side <= 1; side += 2) m.sphere(side * headR * 1.0f, headY - headR * 0.36f, 0, headR * 0.05f, headR * 0.08f, headR * 0.05f, 6, gold);
        if (l.necklace > 0) m.torus(0, shY + 0.005f * H, 0.02f * H, headR * 0.55f, headR * (l.necklace == 2 ? 0.05f : 0.035f), 1, 12,
                m.mat(l.necklace == 2 ? Studio3D.eye(0xFFF2EEE4) : l.necklace == 3 ? Studio3D.stone(0xFFE8E0C8) : Studio3D.metal(0xFFE2B84A)));
        if (l.earphones) m.torus(0, shY + 0.01f * H, 0.01f * H, headR * 0.62f, headR * 0.03f, 1, 12, m.mat(Studio3D.plastic(0xFFF8F8F8)));
        // ---- hair and headwear
        int turban = 0;
        float hairTop = headY + headR * 1.02f;
        if (l.hair != Look.H_NONE && !monster) {
            m.sphere(0, headY + headR * 0.12f, -headR * 0.14f, headR * 1.1f, headR * 1.06f, headR * 1.06f, 20, hair);
            hairTop = headY + headR * 1.18f;
            if (l.hair == Look.H_LONG) for (int side = -1; side <= 1; side += 2) m.capsule(side * headR * 0.85f, headY - headR * 0.1f, -headR * 0.35f, side * headR * 0.95f, shY - 0.04f * H, -headR * 0.3f, headR * 0.28f, headR * 0.26f, 10, hair);
            if (l.hair == Look.H_BRAID) { float bx = headR * 0.8f; for (int k = 0; k < 4; k++) { float y0 = headY - headR * 0.35f - k * headR * 0.42f; m.sphere(bx + k * headR * 0.04f, y0, headR * 0.25f, headR * 0.13f, headR * 0.24f, headR * 0.12f, 8, hair); } if (l.ribbon1 != 0) m.sphere(bx + headR * 0.16f, headY - headR * 0.35f - 3.6f * headR * 0.42f, headR * 0.25f, headR * 0.09f, headR * 0.09f, headR * 0.09f, 6, m.mat(Studio3D.silk(l.ribbon1))); }
            if (l.hair == Look.H_PIGTAILS) for (int side = -1; side <= 1; side += 2) { for (int k = 0; k < 3; k++) m.sphere(side * (headR * 1.0f + k * headR * 0.06f), headY - headR * 0.05f - k * headR * 0.36f, -headR * 0.05f, headR * 0.14f, headR * 0.22f, headR * 0.13f, 8, hair); if (l.ribbon1 != 0) m.sphere(side * headR * 1.0f, headY + headR * 0.15f, 0, headR * 0.09f, headR * 0.09f, headR * 0.09f, 6, m.mat(Studio3D.silk(side < 0 ? l.ribbon1 : l.ribbon2 != 0 ? l.ribbon2 : l.ribbon1))); }
            if (l.hair == Look.H_BUN) { m.sphere(0, headY + headR * 1.12f, -headR * 0.35f, headR * 0.36f, headR * 0.3f, headR * 0.36f, 10, hair); hairTop = headY + headR * 1.42f; }
            if (l.hair == Look.H_PONYTAIL) m.capsule(0, headY + headR * 0.3f, -headR * 0.9f, headR * 0.5f, headY - headR * 1.0f, -headR * 0.9f, headR * 0.22f, headR * 0.12f, 10, hair);
            if (l.curly) for (int i = 0; i < 11; i++) { double a = Math.PI * (0.05 + 0.9 * i / 10); m.sphere((float) Math.cos(a) * headR * 1.0f, headY + headR * 0.25f + (float) Math.sin(a) * headR * 0.92f, -headR * 0.2f + (i % 2) * headR * 0.1f, headR * 0.17f, headR * 0.17f, headR * 0.17f, 7, hair); }
            if (l.ledClip) m.sphere(headR * 0.5f, headY + headR * 0.95f, headR * 0.5f, headR * 0.07f, headR * 0.07f, headR * 0.07f, 6, m.mat(Studio3D.glowing(0xFF40C8FF, 0xA040C8FF)));
        }
        int headMat = m.mat(Studio3D.silk(l.headColor));
        switch (l.headwear) {
            case Look.HW_TURBAN: {
                m.torus(0, headY + headR * 0.55f, 0, headR * 1.02f, headR * 0.3f, 1, 14, headMat);
                m.sphere(0, headY + headR * 0.75f, -headR * 0.05f, headR * 1.08f, headR * 0.75f, headR * 1.05f, 16, headMat);
                if (l.headBand != 0) m.torus(0, headY + headR * 0.62f, 0, headR * 1.12f, headR * 0.06f, 1, 12, m.mat(Studio3D.silk(l.headBand)));
                if (l.kalgi) { m.sphere(headR * 0.3f, headY + headR * 1.3f, headR * 0.6f, headR * 0.12f, headR * 0.12f, headR * 0.08f, 6, gold); m.capsule(headR * 0.3f, headY + headR * 1.35f, headR * 0.6f, headR * 0.5f, headY + headR * 1.9f, headR * 0.5f, headR * 0.04f, headR * 0.08f, 6, m.mat(Studio3D.silk(0xFFF5F5F5))); }
                s.mark(0, headY + headR * 0.28f, headR);
                turban = 1;
                hairTop = headY + headR * 1.5f;
                break;
            }
            case Look.HW_CROWN: {
                m.cylinder(0, headY + headR * 0.7f, 0, 0, headY + headR * 1.05f, 0, headR * 0.85f, headR * 0.9f, 16, gold);
                for (int i = 0; i < 5; i++) { double a = Math.PI * 2 * i / 5 + 0.3; m.capsule((float) Math.cos(a) * headR * 0.85f, headY + headR * 1.0f, (float) Math.sin(a) * headR * 0.85f, (float) Math.cos(a) * headR * 0.9f, headY + headR * 1.4f, (float) Math.sin(a) * headR * 0.9f, headR * 0.1f, 0.002f, 6, gold); }
                m.sphere(0, headY + headR * 1.05f, headR * 0.85f, headR * 0.09f, headR * 0.09f, headR * 0.06f, 6, m.mat(Studio3D.eye(0xFFC62828)));
                hairTop = headY + headR * 1.45f;
                break;
            }
            case Look.HW_WITCH_HAT: {
                int hat = m.mat(Studio3D.cloth(l.headColor == 0 ? 0xFF1F1A2E : l.headColor));
                m.disc(0, headY + headR * 0.7f, 0, headR * 1.7f, 1, 18, hat);
                m.cylinder(0, headY + headR * 0.7f, 0, headR * 0.25f, headY + headR * 2.6f, -headR * 0.1f, headR * 0.95f, headR * 0.03f, 16, hat);
                m.torus(0, headY + headR * 0.85f, 0, headR * 0.95f, headR * 0.06f, 1, 12, m.mat(Studio3D.silk(0xFF7E57C2)));
                hairTop = headY + headR * 2.6f;
                break;
            }
            case Look.HW_HOOD: case Look.HW_PALLU: {
                int cloth = m.mat(l.headwear == Look.HW_HOOD ? Studio3D.cloth(l.primary) : Studio3D.silk(l.headColor));
                m.sphere(0, headY + headR * 0.15f, -headR * 0.3f, headR * 1.28f, headR * 1.22f, headR * 1.18f, 20, cloth);
                m.capsule(-headR * 1.1f, headY - headR * 0.4f, -headR * 0.1f, -sw * 0.9f, shY - 0.02f * H, 0.05f * H, headR * 0.3f, headR * 0.35f, 10, cloth);
                m.capsule(headR * 1.1f, headY - headR * 0.4f, -headR * 0.1f, sw * 0.9f, shY - 0.02f * H, 0.05f * H, headR * 0.3f, headR * 0.35f, 10, cloth);
                hairTop = headY + headR * 1.4f;
                break;
            }
            case Look.HW_HORNS: {
                int horn = m.mat(Studio3D.stone(0xFF5D4037));
                for (int side = -1; side <= 1; side += 2) m.capsule(side * headR * 0.6f, headY + headR * 0.75f, 0, side * headR * 0.95f, headY + headR * 1.6f, -headR * 0.1f, headR * 0.14f, 0.004f, 8, horn);
                hairTop = headY + headR * 1.6f;
                break;
            }
            default:
        }
        if (monster) {
            int horn = m.mat(Studio3D.stone(0xFF4E342E));
            if (l.headwear != Look.HW_HORNS) for (int side = -1; side <= 1; side += 2) m.capsule(side * headR * 0.55f, headY + headR * 0.8f, 0, side * headR * 0.8f, headY + headR * 1.35f, 0, headR * 0.12f, 0.004f, 8, horn);
        }
        int hv1 = m.nv, ht1 = m.nt;                         // the head ends here
        // v34: a many-headed character (Ravana): the other heads in a row beside the main one, each a little further
        // back, lower and smaller (as the drawn puppet has them); the face points stay on the central head, which speaks
        int extraHeads = Math.min(9, l.heads - 1);
        for (int i = 1; i <= extraHeads; i++) {
            int side = i % 2 == 1 ? -1 : 1, k = (i + 1) / 2;
            float sc = 1f - 0.05f * k;
            m.copyPart(hv0, hv1, ht0, ht1, 0, headY, 0, sc, side * headSpacing(k, headR), -k * headR * 0.1f, -k * headR * 0.4f);
        }
        if (monster) {
            for (int side = -1; side <= 1; side += 2) for (int i = 0; i < 3; i++) m.sphere(side * sw * (0.5f + 0.2f * i), shY + 0.02f * H, 0.03f * H, 0.014f * H, 0.014f * H, 0.014f * H, 5, m.mat(Studio3D.metal(0xFFB0BEC5)));
        }
        if (monkey) {
            m.capsule(0, hipY + 0.02f * H, -hw * 0.9f, hw * 1.8f, hipY + T * 0.9f, -hw * 1.2f, 0.025f * H, 0.018f * H, 10, skin);
            m.sphere(0, headY - headR * 0.3f, headR * 0.8f, headR * 0.42f, headR * 0.32f, headR * 0.3f, 10, m.mat(Studio3D.skin(Studio3D.mix(l.furColor, 0xFFFFE0C0, 0.55f))));
        }
        // the marks the rig needs: the eyes, the mouth, its corner, the eye's edge
        s.marks.add(0, new float[]{-eyeX, eyeY, eyeZ + eyeRad});
        s.marks.add(1, new float[]{eyeX, eyeY, eyeZ + eyeRad});
        s.marks.add(2, new float[]{0, mouthY, headR * 0.92f});
        s.marks.add(3, new float[]{mouthHW, mouthY, headR * 0.92f});
        s.marks.add(4, new float[]{-eyeX + eyeRad, eyeY, eyeZ + eyeRad});
        if (turban == 0) s.marks.add(new float[]{0, hairTop, 0});
        return new int[]{turban};
    }

    /** v34: a wheelchair round a seated doll: two big wheels with hand-rims and spokes, seat, backrest with push handles, armrests, footrest and front casters. */
    static void wheelchair(Studio3D.Mesh m, float H, float k) {
        int frame = m.mat(Studio3D.metal(0xFF78909C)), tyre = m.mat(Studio3D.plastic(0xFF212121)), fabric = m.mat(Studio3D.cloth(0xFF37474F));
        float R = 0.2f * H * k, wx = 0.175f * H * k, cy = R, cz = -0.04f * H * k, seatY = 0.26f * H * k;
        for (int side = -1; side <= 1; side += 2) {
            float x = side * wx;
            m.torus(x, cy, cz, R, 0.018f * H * k, 0, 24, tyre);
            m.torus(x + side * 0.012f * H * k, cy, cz, R * 0.84f, 0.006f * H * k, 0, 20, frame);
            for (int i = 0; i < 6; i++) {
                double a = i * Math.PI / 3;
                m.capsule(x, cy, cz, x, cy + (float) Math.sin(a) * R * 0.9f, cz + (float) Math.cos(a) * R * 0.9f, 0.003f * H * k, 0.003f * H * k, 4, frame);
            }
            m.sphere(x, cy, cz, 0.02f * H * k, 0.02f * H * k, 0.02f * H * k, 6, frame);
            m.capsule(x * 0.92f, seatY + 0.13f * H * k, -0.14f * H * k, x * 0.92f, seatY + 0.13f * H * k, 0.1f * H * k, 0.008f * H * k, 0.008f * H * k, 6, frame);
            m.capsule(x * 0.9f, seatY, -0.15f * H * k, x * 0.9f, seatY + 0.33f * H * k, -0.17f * H * k, 0.008f * H * k, 0.008f * H * k, 6, frame);
            m.capsule(x * 0.9f, seatY + 0.33f * H * k, -0.17f * H * k, x * 0.9f, seatY + 0.34f * H * k, -0.24f * H * k, 0.01f * H * k, 0.01f * H * k, 6, tyre);
            m.capsule(x * 0.8f, seatY, 0.12f * H * k, x * 0.7f, 0.06f * H * k, 0.22f * H * k, 0.007f * H * k, 0.007f * H * k, 6, frame);
            m.sphere(x * 0.75f, 0.035f * H * k, 0.2f * H * k, 0.035f * H * k, 0.035f * H * k, 0.012f * H * k, 8, tyre);
        }
        m.box(0, seatY - 0.012f * H * k, -0.01f * H * k, wx * 1.85f, 0.02f * H * k, 0.27f * H * k, fabric);
        m.box(0, seatY + 0.18f * H * k, -0.165f * H * k, wx * 1.75f, 0.25f * H * k, 0.012f * H * k, fabric);
        m.box(0, 0.06f * H * k, 0.23f * H * k, wx * 1.4f, 0.012f * H * k, 0.07f * H * k, frame);
    }

    /** How far the k-th head out from the centre stands (heads touch and overlap a little, the farther behind). */
    static float headSpacing(int k, float headR) { return k * headR * 1.5f; }

    private static void props(Studio3D.Mesh m, Look l, float hx, float hy, float H, float armW, int gold) {
        float gx = hx + armW * 0.3f, gy = hy - armW * 0.3f, gz = 0.06f * H;
        if (l.sword) {
            int blade = m.mat(Studio3D.metal(0xFFCFD8DC));
            m.capsule(gx - 0.02f * H, gy + 0.04f * H, gz, gx + 0.1f * H, gy - 0.3f * H, gz + 0.02f * H, 0.016f * H, 0.006f * H, 8, m.mat(Studio3D.metal(0xFFB08A3A)));
            m.capsule(gx - 0.04f * H, gy + 0.09f * H, gz, gx - 0.015f * H, gy + 0.045f * H, gz, 0.012f * H, 0.012f * H, 6, gold);
            m.sphere(gx - 0.045f * H, gy + 0.1f * H, gz, 0.016f * H, 0.016f * H, 0.016f * H, 6, gold);
            m.box(gx - 0.02f * H, gy + 0.04f * H, gz, 0.06f * H, 0.012f * H, 0.012f * H, blade);
        }
        else if (l.spear) { m.capsule(gx, gy - 0.3f * H, gz, gx, gy + 0.65f * H, gz, 0.009f * H, 0.009f * H, 6, m.mat(Studio3D.wood(0xFF8D6E4A))); m.capsule(gx, gy + 0.65f * H, gz, gx, gy + 0.78f * H, gz, 0.028f * H, 0.001f, 8, m.mat(Studio3D.metal(0xFFCFD8DC))); }
        else if (l.axe) { m.capsule(gx, gy - 0.15f * H, gz, gx, gy + 0.35f * H, gz, 0.012f * H, 0.012f * H, 6, m.mat(Studio3D.wood(0xFF6D4C2A))); m.box(gx + 0.04f * H, gy + 0.3f * H, gz, 0.09f * H, 0.1f * H, 0.01f * H, m.mat(Studio3D.metal(0xFFB0BEC5))); }
        else if (l.mace) { m.capsule(gx, gy - 0.1f * H, gz, gx, gy + 0.3f * H, gz, 0.012f * H, 0.012f * H, 6, m.mat(Studio3D.wood(0xFF6D4C2A))); m.sphere(gx, gy + 0.36f * H, gz, 0.06f * H, 0.06f * H, 0.06f * H, 10, m.mat(Studio3D.metal(0xFF9E9E9E))); }
        else if (l.wand) { int w = m.mat(l.techWand ? Studio3D.glowing(0xFF202428, 0x80FF40FF) : Studio3D.wood(0xFF4E342E)); m.capsule(gx, gy - 0.05f * H, gz, gx + 0.03f * H, gy + 0.28f * H, gz, 0.008f * H, 0.006f * H, 6, w); if (!l.techWand) m.sphere(gx + 0.03f * H, gy + 0.3f * H, gz, 0.018f * H, 0.018f * H, 0.018f * H, 6, m.mat(Studio3D.glowing(0xFFFFF59D, 0xB0FFE082))); }
        else if (l.katar) m.box(gx, gy - 0.04f * H, gz, 0.03f * H, 0.16f * H, 0.006f * H, m.mat(Studio3D.metal(0xFFCFD8DC)));
        else if (l.gadget == Look.GD_PHONE) m.box(gx, gy, gz + 0.02f * H, 0.045f * H, 0.085f * H, 0.008f * H, m.mat(Studio3D.glowing(0xFF1A1E24, 0x5060B0FF)));
        else if (l.gadget == Look.GD_CONTROLLER) m.box(gx, gy, gz + 0.02f * H, 0.1f * H, 0.05f * H, 0.02f * H, m.mat(Studio3D.plastic(0xFF2E2E36)));
        else if (l.gadget == Look.GD_LAPTOP) { m.box(gx, gy - 0.01f * H, gz + 0.03f * H, 0.16f * H, 0.012f * H, 0.11f * H, m.mat(Studio3D.plastic(0xFFB0BEC5))); m.box(gx, gy + 0.05f * H, gz - 0.02f * H, 0.16f * H, 0.11f * H, 0.01f * H, m.mat(Studio3D.glowing(0xFF1A1E24, 0x6060B0FF))); }
    }

    // ================================================================== animals (side view, facing right)

    static int[] animal(Studio3D.Scene s, Look l, float H) {
        Studio3D.Mesh m = s.mesh;
        int sp = l.species;
        boolean robot = l.robot;
        int fur = m.mat(robot ? Studio3D.metal(l.furColor) : Studio3D.fur(l.furColor));
        int dark = m.mat(robot ? Studio3D.metal(Studio3D.shade(l.furColor, 0.7f)) : Studio3D.fur(Studio3D.shade(l.furColor, 0.72f)));
        int belly = m.mat(robot ? Studio3D.plastic(0xFF37474F) : Studio3D.fur(l.skin));
        int white = m.mat(Studio3D.eye(0xFFFAFAF8)), pupil = m.mat(Studio3D.eye(0xFF101010));
        int eyeM = m.mat(robot ? Studio3D.glowing(0xFF1A1E24, 0xC040C8FF) : Studio3D.eye(l.eyeColor));
        float bodyY = 0.6f * H, bodyRx = 0.52f * H, bodyRy = 0.27f * H, bodyRz = 0.24f * H;
        if (sp == Look.SP_ELEPHANT) { bodyRx = 0.55f * H; bodyRy = 0.33f * H; bodyRz = 0.3f * H; bodyY = 0.62f * H; }
        if (sp == Look.SP_RABBIT || sp == Look.SP_MOUSE) { bodyRx = 0.45f * H; bodyRy = 0.3f * H; bodyY = 0.45f * H; }
        if (sp == Look.SP_TORTOISE) { bodyRx = 0.5f * H; bodyRy = 0.2f * H; bodyY = 0.3f * H; }
        float legTop = bodyY - bodyRy * 0.4f;
        float legW = sp == Look.SP_ELEPHANT ? 0.08f * H : (sp == Look.SP_HORSE || sp == Look.SP_DEER ? 0.035f * H : 0.045f * H);
        // body
        if (robot) m.box(0, bodyY, 0, bodyRx * 2, bodyRy * 2, bodyRz * 2, fur);
        else m.sphere(0, bodyY, 0, bodyRx, bodyRy, bodyRz, 20, fur);
        // the lighter belly: a patch that stands clear of the body's surface (two surfaces at the same depth would flicker)
        if (!robot && sp != Look.SP_TORTOISE) m.sphere(bodyRx * 0.1f, bodyY - bodyRy * 0.45f, bodyRz * 0.5f, bodyRx * 0.6f, bodyRy * 0.5f, bodyRz * 0.6f, 14, belly);
        if (sp == Look.SP_TORTOISE) m.sphere(0, bodyY + bodyRy * 0.3f, 0, bodyRx * 1.05f, bodyRy * 1.5f, bodyRz * 1.1f, 18, m.mat(Studio3D.stone(0xFF5D6B3A)));
        // legs: two near, two far
        for (int i = 0; i < 4; i++) {
            float lx = (i % 2 == 0 ? -0.55f : 0.55f) * bodyRx, lz = (i < 2 ? 0.55f : -0.55f) * bodyRz;
            if (robot) m.box(lx, legTop / 2, lz, legW * 1.6f, legTop, legW * 1.6f, dark);
            else m.capsule(lx, legTop, lz, lx, legW * 0.9f, lz, legW, legW * 0.9f, 10, i < 2 ? fur : dark);
            m.sphere(lx + legW * 0.3f, legW * 0.6f, lz, legW * 1.25f, legW * 0.6f, legW * 1.1f, 8, i < 2 ? dark : dark);
        }
        // head
        float headR = sp == Look.SP_ELEPHANT ? 0.3f * H : sp == Look.SP_MOUSE ? 0.2f * H : 0.24f * H;
        float headX = bodyRx * 0.95f, headY = bodyY + bodyRy * 0.65f;
        if (sp == Look.SP_TORTOISE) { headX = bodyRx * 1.15f; headY = bodyY + bodyRy * 0.2f; headR = 0.14f * H; }
        if (sp == Look.SP_HORSE || sp == Look.SP_DEER) { headY = bodyY + bodyRy * 1.3f; m.capsule(bodyRx * 0.6f, bodyY + bodyRy * 0.5f, 0, headX, headY - headR * 0.3f, 0, 0.09f * H, 0.07f * H, 10, fur); }
        if (robot) m.box(headX, headY, 0, headR * 1.8f, headR * 1.6f, headR * 1.6f, fur);
        else m.sphere(headX, headY, 0, headR, headR * 0.95f, headR * 0.9f, 16, fur);
        // v34: the mane sits behind the face (it was wider than the head and hid the eye and the muzzle)
        if (sp == Look.SP_LION) m.sphere(headX - headR * 0.35f, headY + headR * 0.05f, -headR * 0.05f, headR * 1.2f, headR * 1.45f, headR * 0.85f, 16, dark);
        // muzzle, nose, mouth
        float muzX = headX + headR * 0.75f, muzY = headY - headR * 0.2f;
        if (!robot) m.sphere(muzX, muzY, 0, headR * 0.5f, headR * 0.38f, headR * 0.42f, 12, belly);
        m.sphere(muzX + headR * 0.42f, muzY + headR * 0.12f, 0, headR * 0.12f, headR * 0.1f, headR * 0.12f, 6, pupil);
        float mouthX = muzX + headR * 0.3f, mouthY = muzY - headR * 0.22f;
        if (sp == Look.SP_ELEPHANT) { float tx = headX + headR * 0.8f; m.capsule(tx, headY - headR * 0.2f, 0, tx + headR * 0.3f, headY - headR * 1.2f, 0, headR * 0.22f, headR * 0.16f, 10, fur); m.capsule(tx + headR * 0.3f, headY - headR * 1.2f, 0, tx + headR * 0.9f, headY - headR * 1.6f, 0, headR * 0.16f, headR * 0.1f, 10, fur); mouthX = tx + headR * 0.2f; mouthY = headY - headR * 0.75f; }
        // eye (the near one; the far one is hidden) with a highlight
        float eyeX = headX + headR * 0.35f, eyeY = headY + headR * 0.2f, eyeZ = headR * 0.72f, eyeRad = headR * (sp == Look.SP_MOUSE ? 0.2f : 0.16f);
        if (robot) { m.box(eyeX, eyeY, headR * 0.82f, headR * 0.5f, headR * 0.3f, headR * 0.05f, eyeM); }
        else {
            m.sphere(eyeX, eyeY, eyeZ, eyeRad, eyeRad, eyeRad * 0.7f, 10, white);
            m.sphere(eyeX + eyeRad * 0.1f, eyeY, eyeZ + eyeRad * 0.55f, eyeRad * 0.52f, eyeRad * 0.52f, eyeRad * 0.25f, 8, eyeM);
            m.sphere(eyeX + eyeRad * 0.15f, eyeY, eyeZ + eyeRad * 0.75f, eyeRad * 0.26f, eyeRad * 0.26f, eyeRad * 0.1f, 6, pupil);
            m.sphere(eyeX - eyeRad * 0.1f, eyeY + eyeRad * 0.22f, eyeZ + eyeRad * 0.85f, eyeRad * 0.09f, eyeRad * 0.09f, eyeRad * 0.05f, 5, white);
        }
        // ears
        int earM = fur;
        float earX = headX - headR * 0.25f, earY = headY + headR * 0.75f;
        switch (sp) {
            case Look.SP_RABBIT: for (int side = -1; side <= 1; side += 2) m.sphere(earX, earY + headR * 0.7f, side * headR * 0.35f, headR * 0.16f, headR * 0.9f, headR * 0.12f, 10, earM); break;
            case Look.SP_ELEPHANT: for (int side = -1; side <= 1; side += 2) m.sphere(earX - headR * 0.3f, headY + headR * 0.1f, side * headR * 0.9f, headR * 0.65f, headR * 0.8f, headR * 0.12f, 12, earM); break;
            case Look.SP_BEAR: case Look.SP_MOUSE: case Look.SP_LION: for (int side = -1; side <= 1; side += 2) m.sphere(earX, earY, side * headR * (sp == Look.SP_LION ? 0.78f : 0.5f), headR * (sp == Look.SP_MOUSE ? 0.4f : 0.26f), headR * (sp == Look.SP_MOUSE ? 0.4f : 0.26f), headR * 0.12f, 8, earM); break;
            case Look.SP_COW: case Look.SP_GOAT: for (int side = -1; side <= 1; side += 2) { m.sphere(earX, earY - headR * 0.2f, side * headR * 0.75f, headR * 0.3f, headR * 0.14f, headR * 0.1f, 8, earM); m.capsule(earX, earY, side * headR * 0.4f, earX - headR * 0.2f, earY + headR * (sp == Look.SP_COW ? 0.45f : 0.55f), side * headR * 0.55f, headR * 0.09f, 0.003f, 6, m.mat(Studio3D.stone(0xFFD7CCC8))); } break;
            case Look.SP_HORSE: case Look.SP_DEER: for (int side = -1; side <= 1; side += 2) m.capsule(earX, earY, side * headR * 0.4f, earX - headR * 0.1f, earY + headR * 0.5f, side * headR * 0.45f, headR * 0.13f, headR * 0.04f, 6, earM); if (sp == Look.SP_DEER) for (int side = -1; side <= 1; side += 2) { int ant = m.mat(Studio3D.wood(0xFF8D6E4A)); m.capsule(earX - headR * 0.1f, earY, side * headR * 0.3f, earX - headR * 0.3f, earY + headR * 0.9f, side * headR * 0.5f, headR * 0.05f, headR * 0.04f, 5, ant); m.capsule(earX - headR * 0.2f, earY + headR * 0.45f, side * headR * 0.4f, earX + headR * 0.1f, earY + headR * 0.8f, side * headR * 0.55f, headR * 0.04f, headR * 0.03f, 5, ant); } break;
            case Look.SP_TORTOISE: break;
            case Look.SP_DOG: for (int side = -1; side <= 1; side += 2) m.sphere(earX - headR * 0.15f, headY + headR * 0.25f, side * headR * 0.8f, headR * 0.2f, headR * 0.45f, headR * 0.1f, 10, dark); break;
            default: for (int side = -1; side <= 1; side += 2) m.capsule(earX, earY - headR * 0.1f, side * headR * 0.45f, earX - headR * 0.05f, earY + headR * 0.5f, side * headR * 0.5f, headR * 0.17f, 0.004f, 8, earM);
        }
        if (robot) { m.capsule(headX, headY + headR * 0.8f, 0, headX - headR * 0.1f, headY + headR * 1.5f, 0, headR * 0.04f, headR * 0.04f, 5, dark); m.sphere(headX - headR * 0.1f, headY + headR * 1.55f, 0, headR * 0.1f, headR * 0.1f, headR * 0.1f, 6, m.mat(Studio3D.glowing(0xFFFF5252, 0xB0FF5252))); }
        // tail
        float tx = -bodyRx * 0.95f, ty = bodyY + bodyRy * 0.2f;
        switch (sp) {
            case Look.SP_FOX: case Look.SP_WOLF: m.capsule(tx, ty, 0, tx - bodyRx * 0.6f, ty + bodyRy * 0.6f, 0, 0.07f * H, 0.1f * H, 10, fur); m.sphere(tx - bodyRx * 0.75f, ty + bodyRy * 0.75f, 0, 0.1f * H, 0.1f * H, 0.1f * H, 8, belly); break;
            case Look.SP_CAT: case Look.SP_TIGER: case Look.SP_LION: case Look.SP_MOUSE: m.capsule(tx, ty, 0, tx - bodyRx * 0.5f, ty + bodyRy * 1.1f, 0, 0.03f * H, 0.022f * H, 8, fur); if (sp == Look.SP_LION) m.sphere(tx - bodyRx * 0.5f, ty + bodyRy * 1.15f, 0, 0.05f * H, 0.05f * H, 0.05f * H, 6, dark); break;
            case Look.SP_DOG: m.capsule(tx, ty, 0, tx - bodyRx * 0.4f, ty + bodyRy * 0.9f, 0, 0.04f * H, 0.025f * H, 8, fur); break;
            case Look.SP_RABBIT: m.sphere(tx, ty, 0, 0.06f * H, 0.06f * H, 0.06f * H, 8, belly); break;
            case Look.SP_HORSE: for (int i = 0; i < 3; i++) m.capsule(tx, ty + 0.02f * H, (i - 1) * 0.03f * H, tx - bodyRx * 0.3f, ty - bodyRy * 1.3f, (i - 1) * 0.04f * H, 0.03f * H, 0.02f * H, 6, dark); for (int i = 0; i < 4; i++) m.sphere(bodyRx * 0.55f + i * 0.08f * H, bodyY + bodyRy * 0.95f + i * 0.08f * H, 0, 0.06f * H, 0.08f * H, 0.05f * H, 7, dark); break;
            case Look.SP_ELEPHANT: m.capsule(tx, ty, 0, tx - bodyRx * 0.2f, ty - bodyRy * 0.9f, 0, 0.025f * H, 0.015f * H, 6, fur); break;
            case Look.SP_TORTOISE: m.capsule(tx, ty - bodyRy * 0.5f, 0, tx - bodyRx * 0.3f, ty - bodyRy * 0.9f, 0, 0.03f * H, 0.01f * H, 6, fur); break;
            default: m.capsule(tx, ty, 0, tx - bodyRx * 0.35f, ty + bodyRy * 0.7f, 0, 0.035f * H, 0.02f * H, 8, fur);
        }
        if (sp == Look.SP_TIGER) for (int i = 0; i < 5; i++) m.box(-bodyRx * 0.7f + i * bodyRx * 0.33f, bodyY + bodyRy * 0.45f, 0, 0.035f * H, bodyRy * 0.9f, bodyRz * 2.05f, dark);
        // a little jacket when the story dresses the animal; a collar or bandana in the second colour
        if (l.outfit == Look.O_JACKET && !robot) m.sphere(bodyRx * 0.15f, bodyY + bodyRy * 0.3f, 0, bodyRx * 0.5f, bodyRy * 0.9f, bodyRz * 1.06f, 14, m.mat(Studio3D.cloth(l.primary)));
        if (sp == Look.SP_DOG || sp == Look.SP_CAT || robot) m.torus(headX - headR * 0.5f, headY - headR * 0.75f, 0, headR * 0.55f, headR * 0.07f, 0, 10, m.mat(Studio3D.silk(l.secondary)));
        // marks: eye (both the same), mouth, mouth corner, eye edge
        s.marks.add(0, new float[]{eyeX, eyeY, eyeZ + eyeRad});
        s.marks.add(1, new float[]{eyeX + eyeRad * 0.6f, eyeY, eyeZ + eyeRad});
        s.marks.add(2, new float[]{mouthX, mouthY, headR * 0.3f});
        s.marks.add(3, new float[]{mouthX + headR * 0.25f, mouthY, headR * 0.3f});
        s.marks.add(4, new float[]{eyeX + eyeRad, eyeY, eyeZ + eyeRad});
        s.marks.add(new float[]{headX, headY + headR, 0});
        return new int[]{0};
    }

    // ================================================================== birds (side view, facing right)

    static int[] bird(Studio3D.Scene s, Look l, float H) {
        Studio3D.Mesh m = s.mesh;
        int sp = l.species;
        int fe = m.mat(Studio3D.fur(l.furColor));
        int dark = m.mat(Studio3D.fur(Studio3D.shade(l.furColor, 0.7f)));
        int belly = m.mat(Studio3D.fur(l.skin));
        int beak = m.mat(Studio3D.plastic(sp == Look.SP_CROW ? 0xFF37474F : 0xFFF9A825));
        int white = m.mat(Studio3D.eye(0xFFFAFAF8)), pupil = m.mat(Studio3D.eye(0xFF101010));
        float bodyY = 0.5f * H, bodyRx = 0.42f * H, bodyRy = 0.3f * H, bodyRz = 0.26f * H;
        m.sphere(0, bodyY, 0, bodyRx, bodyRy, bodyRz, 18, fe);
        m.sphere(bodyRx * 0.15f, bodyY - bodyRy * 0.4f, bodyRz * 0.5f, bodyRx * 0.5f, bodyRy * 0.5f, bodyRz * 0.6f, 12, belly);
        // wings
        for (int side = -1; side <= 1; side += 2) m.sphere(-bodyRx * 0.1f, bodyY + bodyRy * 0.1f, side * bodyRz * 0.85f, bodyRx * 0.7f, bodyRy * 0.5f, bodyRz * 0.18f, 12, dark);
        // tail
        if (sp == Look.SP_PEACOCK) {
            for (int i = 0; i < 7; i++) {
                double a = Math.PI * (0.15 + 0.7 * i / 6);
                float ex = -bodyRx * 0.6f - (float) Math.cos(a) * 0.5f * H, ey = bodyY + (float) Math.sin(a) * 0.55f * H;
                m.sphere(ex, ey, -bodyRz * 0.5f, 0.13f * H, 0.13f * H, 0.02f * H, 10, m.mat(Studio3D.silk(i % 2 == 0 ? 0xFF1E88E5 : 0xFF2E7D32)));
                m.sphere(ex, ey, -bodyRz * 0.45f, 0.05f * H, 0.05f * H, 0.02f * H, 8, m.mat(Studio3D.silk(0xFF5E35B1)));
            }
        } else m.capsule(-bodyRx * 0.8f, bodyY, 0, -bodyRx * 1.4f, bodyY + bodyRy * 0.5f, 0, bodyRy * 0.35f, bodyRy * 0.15f, 8, dark);
        // head
        float headR = 0.2f * H, headX = bodyRx * 0.85f, headY = bodyY + bodyRy * 0.95f;
        m.sphere(headX, headY, 0, headR, headR, headR * 0.95f, 14, fe);
        if (sp == Look.SP_HEN) for (int i = 0; i < 3; i++) m.sphere(headX - headR * 0.3f + i * headR * 0.3f, headY + headR * 1.0f, 0, headR * 0.16f, headR * 0.22f, headR * 0.08f, 6, m.mat(Studio3D.silk(0xFFD32F2F)));
        if (sp == Look.SP_PEACOCK) for (int i = 0; i < 3; i++) m.capsule(headX, headY + headR * 0.8f, 0, headX - headR * 0.2f + i * headR * 0.2f, headY + headR * 1.5f, 0, headR * 0.03f, headR * 0.1f, 5, m.mat(Studio3D.silk(0xFF1E88E5)));
        // beak
        float bx = headX + headR * 0.8f, by = headY - headR * 0.1f;
        if (sp == Look.SP_DUCK) m.sphere(bx + headR * 0.2f, by, 0, headR * 0.5f, headR * 0.16f, headR * 0.35f, 10, beak);
        else if (sp == Look.SP_PARROT || sp == Look.SP_EAGLE || sp == Look.SP_OWL) { m.capsule(bx, by + headR * 0.1f, 0, bx + headR * 0.45f, by - headR * 0.35f, 0, headR * 0.2f, 0.004f, 8, beak); }
        else m.capsule(bx, by, 0, bx + headR * 0.55f, by - headR * 0.05f, 0, headR * 0.17f, 0.004f, 8, beak);
        // eye
        float eyeX = headX + headR * 0.35f, eyeY = headY + headR * 0.2f, eyeZ = headR * 0.75f, eyeRad = headR * (sp == Look.SP_OWL ? 0.32f : 0.18f);
        m.sphere(eyeX, eyeY, eyeZ, eyeRad, eyeRad, eyeRad * 0.6f, 10, white);
        m.sphere(eyeX + eyeRad * 0.1f, eyeY, eyeZ + eyeRad * 0.5f, eyeRad * 0.5f, eyeRad * 0.5f, eyeRad * 0.2f, 8, m.mat(Studio3D.eye(l.eyeColor)));
        m.sphere(eyeX + eyeRad * 0.15f, eyeY, eyeZ + eyeRad * 0.65f, eyeRad * 0.25f, eyeRad * 0.25f, eyeRad * 0.1f, 6, pupil);
        // legs
        for (int i = 0; i < 2; i++) {
            float lz = (i == 0 ? 0.3f : -0.3f) * bodyRz, lx = bodyRx * 0.1f;
            m.capsule(lx, bodyY - bodyRy * 0.8f, lz, lx + 0.02f * H, 0.03f * H, lz, 0.018f * H, 0.014f * H, 6, beak);
            m.capsule(lx - 0.04f * H, 0.02f * H, lz, lx + 0.09f * H, 0.02f * H, lz, 0.014f * H, 0.01f * H, 5, beak);
        }
        float mouthX = bx + headR * 0.25f, mouthY = by - headR * 0.08f;
        s.marks.add(0, new float[]{eyeX, eyeY, eyeZ + eyeRad});
        s.marks.add(1, new float[]{eyeX + eyeRad * 0.6f, eyeY, eyeZ + eyeRad});
        s.marks.add(2, new float[]{mouthX, mouthY, headR * 0.2f});
        s.marks.add(3, new float[]{mouthX + headR * 0.3f, mouthY, headR * 0.2f});
        s.marks.add(4, new float[]{eyeX + eyeRad, eyeY, eyeZ + eyeRad});
        s.marks.add(new float[]{headX, headY + headR, 0});
        return new int[]{0};
    }
}
