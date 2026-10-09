package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/** Draws any moment of the film. Same code renders on the phone and in the desktop test harness. */
public final class Renderer {
    public static final float W = 1280, H = 720;

    private final Film film;
    private final Art art;
    /** Screen in stage units: always 720 high, width follows the output aspect (1280 for 16:9, 405 for 9:16). */
    private float vw = W, vh = H;
    private final Pose pose = new Pose();

    public Renderer(Film film, Art art) {
        this.film = film;
        this.art = art == null ? new Art() : art;
    }

    /** On-screen height (stage pixels) of a character standing on the floor. */
    /**
     * A character's height on the stage (720 units high): the format's character scale lock (PixarLead
     * RULE_RESIZE_6 — a standing adult is 60 % of a 16:9 frame, 50 % of a 9:16 one, 65 % of a square) times the
     * character's own height; a drawn puppet is a little shorter than a picture of the same person.
     */
    public static float actorHeight(Look l, Story.CharacterDef c, Art art, Film.Seg seg) {
        boolean sprite = art != null && c != null && art.sprites.containsKey(c.id);
        float lock = seg == null ? 0.6f : seg.charScale;
        float base = (sprite ? 1f : 0.8625f) * 720f * lock;
        float h = l.height;
        if (sprite && l.kind == Look.MONKEY) h = 0.42f;
        return base * h;
    }

    // ================================================================== frame

    public void render(Gfx g, float t) {
        curT = t;
        curGw = g.width();
        if (feetLog != null) feetLog.clear();
        vh = H;
        vw = H * g.width() / (float) g.height();
        g.save();
        g.scale(g.width() / vw, g.height() / vh);
        Film.Seg s = film.segAt(t);
        g.color(0xFF000000);
        g.rect(0, 0, vw, vh);
        if (s != null) {
            int si = film.segs.indexOf(s);
            Film.Seg prev = si > 0 ? film.segs.get(si - 1) : null, next = si + 1 < film.segs.size() ? film.segs.get(si + 1) : null;
            // one scene melts into the next (a dissolve), like a film, instead of going through black
            boolean dissolveIn = prev != null && prev.type == Film.S_SCENE && s.type == Film.S_SCENE && t - s.t0 < DISSOLVE;
            boolean dissolveOut = next != null && next.type == Film.S_SCENE && s.type == Film.S_SCENE;
            if (dissolveIn && s.transition != 0) {
                // a dip: the last scene goes to black (time passes) or to white (magic, a dream, a memory) and the
                // new one comes up out of it
                float u = (t - s.t0) / DISSOLVE;
                int col = s.transition == 1 ? 0xFF000000 : 0xFFFFF8F0;
                if (u < 0.5f) { drawScene(g, prev, t); g.color(Puppet.alpha(col, Math.min(1, u * 2))); }
                else { drawScene(g, s, t); g.color(Puppet.alpha(col, Math.min(1, (1 - u) * 2))); }
                g.rect(0, 0, vw, vh);
            } else if (dissolveIn) {
                // a plain scene change: a quick dip through black (the last scene goes down, the new one comes up) —
                // never two scenes drawn over each other
                float u = (t - s.t0) / DISSOLVE;
                if (u < 0.5f) { float keepFollow = followX; drawScene(g, prev, t); followX = keepFollow; g.color(Puppet.alpha(0xFF000000, Math.min(1, u * 2))); }
                else { drawScene(g, s, t); g.color(Puppet.alpha(0xFF000000, Math.min(1, (1 - u) * 2))); }
                g.rect(0, 0, vw, vh);
            } else {
                switch (s.type) {
                    case Film.S_TITLE: drawTitle(g, s, t); break;
                    case Film.S_CARD: drawCard(g, s, t); break;
                    case Film.S_END: drawEnd(g, s, t); break;
                    default: drawScene(g, s, t);
                }
            }
            float a = 1f;
            if (s.fadeIn > 0 && !dissolveIn && !(prev != null && prev.type == Film.S_SCENE && s.type == Film.S_SCENE)) a = Math.min(a, (t - s.t0) / s.fadeIn);
            if (s.fadeOut > 0 && !dissolveOut) a = Math.min(a, (s.t1 - t) / s.fadeOut);
            if (a < 1f) { g.color(Puppet.alpha(0xFF000000, 1f - Math.max(0, a))); g.rect(0, 0, vw, vh); }
        }
        g.restore();
    }

    static final float DISSOLVE = 0.6f;

    /** Renders one part on its own, outside the film's timeline (the thumbnail and the poster pages). */
    public void renderSeg(Gfx g, Film.Seg s, float t) {
        curT = t;
        vh = H;
        vw = H * g.width() / (float) g.height();
        g.save();
        g.scale(g.width() / vw, g.height() / vh);
        g.color(0xFF000000);
        g.rect(0, 0, vw, vh);
        drawScene(g, s, t);
        g.restore();
    }

    // ================================================================== title, cards, end

    /**
     * The picture fills the frame without ever being stretched ("object-fit: cover"): the part of the picture
     * with the frame's own shape, centred on the picture's chosen crop, as large as the picture allows.
     * Returns {x0, y0, x1, y1} as fractions of the picture.
     */
    private float[] coverCrop(Art.Backdrop b, float frameAR) {
        float cx = (b.x0 + b.x1) / 2 * b.w, cy = (b.y0 + b.y1) / 2 * b.h;
        float w = b.w, h = w / frameAR;
        if (h > b.h) { h = b.h; w = h * frameAR; }
        // keep the chosen crop's size if it is smaller and has the right shape already
        float x0 = Math.max(0, Math.min(b.w - w, cx - w / 2)), y0 = Math.max(0, Math.min(b.h - h, cy - h / 2));
        return new float[]{x0 / b.w, y0 / b.h, (x0 + w) / b.w, (y0 + h) / b.h};
    }

    private void cover(Gfx g, Art.Backdrop b, float zoom, float panX, float panY) {
        float[] cr = coverCrop(b, vw / vh);
        float sx = cr[0] * b.w, sy = cr[1] * b.h, sw = (cr[2] - cr[0]) * b.w, sh = (cr[3] - cr[1]) * b.h;
        g.save();
        g.translate(vw / 2 + panX, vh / 2 + panY);
        g.scale(zoom, zoom);
        Nature.Scan sc = b.scan;
        // every picture is drawn through a mesh fine to the pixel: title pages and close-up shots live too
        // (leaves sway, water flows, falls stream)
        g.translate(-vw / 2, -vh / 2);
        float wind = film == null ? 0 : film.wind(curT), sea = film == null ? 0 : film.weather(Film.W_SEA, curT);
        int[] ms = Nature.meshSize(sc, vw / Math.max(1e-3f, cr[2] - cr[0]) * zoom * g.width() / vw, b.w, b.h);
        float[] mesh = bdMesh(ms);
        Nature.backdropMesh(sc, cr[0], cr[1], cr[2], cr[3], vw, vh, curT, wind, sea, mesh, ms[0], ms[1]);
        g.imageMesh(b.img, ms[0], ms[1], mesh);
        if (sc != null) Nature.waterLife(g, sc, cr[0], cr[1], cr[2], cr[3], vw, vh, curT);
        g.restore();
    }

    /** The time of the frame being drawn (for living pictures drawn by helpers). */
    private float curT;

    /**
     * Title and end pages (pictures often carry their own lettering): when the picture's shape is close to the
     * frame's it fills the frame; otherwise the whole picture is shown, never stretched, over a soft, dark,
     * enlarged copy of itself (no black bars).
     */
    private void contain(Gfx g, Art.Backdrop b, float zoom) {
        float ar = b.w / (float) b.h, frame = vw / vh;
        if (Math.abs(Math.log(ar / frame)) < 0.22) { cover(g, b, zoom, 0, 0); return; }
        // the fill: the same picture, enlarged to cover the frame, darkened
        float[] cr = coverCrop(b, frame);
        g.save();
        g.setAlpha(0.6f);
        g.imageRect(b.img, cr[0] * b.w, cr[1] * b.h, (cr[2] - cr[0]) * b.w, (cr[3] - cr[1]) * b.h, -vw * 0.1f, -vh * 0.1f, vw * 1.2f, vh * 1.2f);
        g.restore();
        g.color(0x99000000);
        g.rect(0, 0, vw, vh);
        // the whole picture, fitted inside the frame with its own proportions
        float w, h;
        if (ar > frame) { w = vw * 0.98f * zoom; h = w / ar; } else { h = vh * 0.98f * zoom; w = h * ar; }
        if (w > vw) { w = vw; h = w / ar; }
        if (h > vh) { h = vh; w = h * ar; }
        g.image(b.img, (vw - w) / 2, (vh - h) / 2, w, h);
    }

    private void drawTitle(Gfx g, Film.Seg s, float t) {
        float u = (t - s.t0) / Math.max(0.1f, s.t1 - s.t0);
        boolean text = art.titleText || s.backdrop == null;
        if (s.backdrop != null) {
            contain(g, s.backdrop, 1.0f + 0.05f * u);
        } else {
            g.save();
            stageView(g, 1 + 0.05f * u);
            g.layer("set:title", W, H, new Gfx.Painter() { public void paint(Gfx gg) { Sets.paintStatic(gg, Sets.GARDEN, Sets.MORNING); } });
            Sets.paintLive(g, Sets.GARDEN, Sets.MORNING, t);
            heroesLineup(g, t, vw < 900 ? 1 : 2, 640, Sets.GROUND + 20, 1.0f);
            g.restore();
        }
        sparkles(g, t, 0, 0, vw, vh, 26, 0xFFFFE082);
        if (text) {
            float a = Math.min(1, Math.max(0, (t - s.t0 - 0.6f) / 1.2f));
            g.save();
            g.setAlpha(a);
            g.linear(0, 40, 0, 240, 0xAA000000, 0x00000000);
            g.rect(0, 0, vw, 260);
            bigText(g, s.text1, vw / 2, 150, fitSize(g, s.text1, 78, vw - 80), 0xFFFFD54F, 0xFF5D2E00);
            if (s.text2.length() > 0) bigText(g, s.text2, vw / 2, 210, 34, 0xFFFFFFFF, 0xFF000000);
            g.restore();
        }
    }

    private void drawCard(Gfx g, Film.Seg s, float t) {
        float u = (t - s.t0) / Math.max(0.1f, s.t1 - s.t0);
        if (s.backdrop != null) cover(g, s.backdrop, 1.08f + 0.04f * u, 0, 0);
        else {
            final int set = s.set, tod = s.tod;
            g.save();
            stageView(g, 1.08f + 0.04f * u);
            g.layer("set:" + set + ":" + tod, W, H, new Gfx.Painter() { public void paint(Gfx gg) { Sets.paintStatic(gg, set, tod); } });
            g.restore();
        }
        if (s.text1.length() == 0 && s.text2.length() == 0) return;     // an establishing bridge: the place alone
        g.color(0xB0000000);
        g.rect(0, 0, vw, vh);
        float a = Math.min(1, (t - s.t0) / 0.5f);
        g.save();
        g.setAlpha(a);
        // ornament lines
        g.color(0xFFE5B530);
        float lw = 280 * Math.min(1, (t - s.t0) / 0.8f);
        g.line(vw / 2 - 40 - lw, 300, vw / 2 - 40, 300, 3);
        g.line(vw / 2 + 40, 300, vw / 2 + 40 + lw, 300, 3);
        g.oval(vw / 2, 300, 7, 7);
        bigText(g, s.text1, vw / 2, 400, fitSize(g, s.text1, 92, vw - 80), 0xFFFFD54F, 0xFF4E2600);
        if (s.text2.length() > 0) bigText(g, s.text2, vw / 2, 478, fitSize(g, s.text2, 46, vw - 80), 0xFFFFFFFF, 0xFF000000);
        g.color(0xFFE5B530);
        g.line(vw / 2 - 40 - lw, 520, vw / 2 + 40 + lw, 520, 2);
        g.restore();
    }

    private void drawEnd(Gfx g, Film.Seg s, float t) {
        float u = (t - s.t0) / Math.max(0.1f, s.t1 - s.t0);
        boolean text = art.endText || s.backdrop == null;
        if (s.backdrop != null) contain(g, s.backdrop, 1.0f + 0.06f * u);
        else {
            g.save();
            stageView(g, 1 + 0.04f * u);
            g.layer("set:end", W, H, new Gfx.Painter() { public void paint(Gfx gg) { Sets.paintStatic(gg, Sets.CELEBRATION, Sets.EVENING); } });
            Sets.paintLive(g, Sets.CELEBRATION, Sets.EVENING, t);
            heroesLineup(g, t, vw < 900 ? 3 : 6, 640, Sets.GROUND + 30, vw < 900 ? 0.75f : 0.85f);
            g.restore();
        }
        sparkles(g, t, 0, 0, vw, vh, 40, 0xFFFFE082);
        if (text) {
            float a = Math.min(1, Math.max(0, (t - s.t0 - 0.8f) / 1.2f));
            float sc = 0.8f + 0.2f * a;
            g.save();
            g.setAlpha(a);
            g.color(0x77000000);
            g.roundRect(vw / 2 - Math.min(300, vw / 2 - 20), 70, Math.min(600, vw - 40), 170, 30);
            g.translate(vw / 2, 200);
            g.scale(sc, sc);
            bigText(g, s.text1, 0, 0, fitSize(g, s.text1, 120, vw - 60), 0xFFFFD54F, 0xFF4E2600);
            g.restore();
        }
    }

    /** Maps the 1280x720 stage onto the screen, centre-cropped, with a zoom. */
    private void stageView(Gfx g, float zoom) {
        float k = Math.max(vw / W, vh / H) * zoom;
        g.translate(vw / 2, vh / 2);
        g.scale(k, k);
        g.translate(-W / 2, -H / 2);
    }

    private void bigText(Gfx g, String s, float x, float y, float size, int fill, int outline) {
        g.color(Puppet.alpha(outline, 0.85f));
        float o = Math.max(2, size / 22);
        for (int i = 0; i < 8; i++) {
            double a = Math.PI * 2 * i / 8;
            g.text(s, x + (float) Math.cos(a) * o, y + (float) Math.sin(a) * o, size, true, 1);
        }
        g.color(0x66000000);
        g.text(s, x + o * 1.5f, y + o * 2f, size, true, 1);
        g.color(fill);
        g.text(s, x, y, size, true, 1);
    }

    private float fitSize(Gfx g, String s, float size, float maxW) {
        float w = g.textWidth(s, size, true);
        return w > maxW ? size * maxW / w : size;
    }

    private void sparkles(Gfx g, float t, float x, float y, float w, float h, int n, int color) {
        for (int i = 0; i < n; i++) {
            float px = x + ((i * 7919) % 1000) / 1000f * w;
            float py = y + ((i * 104729 + 37) % 1000) / 1000f * h;
            float a = (float) Math.max(0, Math.sin(t * (1.5f + (i % 5) * 0.4f) + i));
            if (a < 0.3f) continue;
            float r = 2 + 4 * a;
            g.color(Puppet.alpha(color, a));
            g.begin(); g.moveTo(px, py - r * 2); g.lineTo(px + r * 0.4f, py); g.lineTo(px, py + r * 2); g.lineTo(px - r * 0.4f, py); g.close(); g.fillPath();
            g.begin(); g.moveTo(px - r * 2, py); g.lineTo(px, py + r * 0.4f); g.lineTo(px + r * 2, py); g.lineTo(px, py - r * 0.4f); g.close(); g.fillPath();
        }
    }

    /** The main heroes standing together (procedural title / ending). */
    private void heroesLineup(Gfx g, float t, int max, float cx, float ground, float scale) {
        List<Story.CharacterDef> heroes = new ArrayList<Story.CharacterDef>();
        final java.util.Map<Story.CharacterDef, Integer> count = new java.util.HashMap<Story.CharacterDef, Integer>();
        for (Film.Line l : film.lines) if (l.who != null) { Integer c = count.get(l.who); count.put(l.who, c == null ? 1 : c + 1); }
        for (Story.CharacterDef c : film.story.characters) if (c.look.hero && count.containsKey(c)) heroes.add(c);
        Collections.sort(heroes, new Comparator<Story.CharacterDef>() {
            public int compare(Story.CharacterDef a, Story.CharacterDef b) { return count.get(b) - count.get(a); }
        });
        while (heroes.size() > max) heroes.remove(heroes.size() - 1);
        int n = heroes.size();
        for (int i = 0; i < n; i++) {
            Story.CharacterDef c = heroes.get(i);
            float x = cx + (i - (n - 1) / 2f) * Math.min(220, 1000f / Math.max(1, n));
            pose.reset();
            pose.time = t; pose.seed = i;
            pose.facing = x < cx ? 1 : -1;
            pose.emotion = Pose.HAPPY;
            pose.armL = 20; pose.armR = 20;
            pose.bob = (float) Math.sin(t * 2 + i) * 2;
            pose.blink = blink(t, i);
            float h = actorHeight(c.look, c, art, null) * scale;
            g.save();
            g.translate(x, ground);
            Art.Sprite sp = art.sprites.get(c.id);
            if (sp != null) drawSprite(g, sp, c.look, pose, h, 0, null);
            else Puppet.draw(g, c.look, pose, h);
            g.restore();
        }
    }

    // ================================================================== scene

    private float camX, camY, camZ, camRoll;
    /** -1 high angle .. +1 low angle, and how hard the light is (0 soft .. 1 hard), for the shot on screen now. */
    private float camAngle, camLight = 0.4f;
    private boolean camStill;
    /** One output pixel in stage units for the shot on screen (0 = unknown), so still shots can land pictures on whole pixels. */
    private float pixelStep;
    private int curGw;
    /** The shot on screen is a reverse shot (the listener's face): the place's reverse angle is drawn when the user gave one. */
    private boolean camReverse;

    /** The place picture behind this moment: the reverse angle in a reverse shot when there is one, else the plate. */
    private Art.Backdrop bd(Film.Seg s) { return camReverse && s.backdropReverse != null ? s.backdropReverse : s.backdrop; }

    private void camera(Film.Seg s, float t) {
        Film.Cam cur = null, prev = null;
        for (Film.Cam c : s.cams) { if (c.t <= t) { prev = cur; cur = c; } else break; }
        camReverse = cur != null && cur.reverse;
        float moodLight = s.mood == Film.M_TENSE || s.mood == Film.M_VILLAIN || s.mood == Film.M_ACTION ? 0.75f
                : s.mood == Film.M_HAPPY || s.mood == Film.M_CELEBRATE || s.mood == Film.M_PLAYFUL ? 0.2f : 0.4f;
        if (cur == null) { camX = 640; camY = 360; camZ = 1; camRoll = 0; camAngle = 0; camLight = moodLight; camStill = false; }
        else if (cur.ease > 0 && prev != null && t < cur.t + cur.ease) {
            float u = (t - cur.t) / cur.ease;
            u = u * u * (3 - 2 * u);
            camX = prev.cx + (cur.cx - prev.cx) * u;
            camY = prev.cy + (cur.cy - prev.cy) * u;
            camZ = prev.zoom + (cur.zoom - prev.zoom) * u;
            camRoll = prev.roll + (cur.roll - prev.roll) * u;
            camAngle = prev.angle + (cur.angle - prev.angle) * u;
            float pl = prev.light < 0 ? moodLight : prev.light, cl = cur.light < 0 ? moodLight : cur.light;
            camLight = pl + (cl - pl) * u;
            camStill = false;
        } else {
            camX = cur.cx; camY = cur.cy; camZ = cur.zoom; camRoll = cur.roll; camAngle = cur.angle;
            camLight = cur.light < 0 ? moodLight : cur.light;
            camStill = cur.still;
        }
        // a living camera: very slow drift and breathing, like a camera operator holding the shot — but a strong
        // performance gets a camera that holds perfectly still (the performance comes first)
        float live = camStill ? 0f : 1f;     // a locked tripod: no drift at all
        camX += live * (float) (Math.sin(t * 0.31) * 5 + Math.sin(t * 0.73) * 2) / camZ;
        camY += live * (float) (Math.cos(t * 0.27) * 3) / camZ;
        camZ *= 1f + live * 0.008f * (float) Math.sin(t * 0.21);
        if (camZ < 1) camZ = 1;
        float hw = vw / 2 / camZ, hh = vh / 2 / camZ;
        camX = Math.max(hw, Math.min(W - hw, camX));
        camY = Math.max(hh, Math.min(H - hh, camY));
        for (Film.Fx f : s.fx) {
            if (f.type == Film.FX_SHAKE && t >= f.t0 && t < f.t1) {
                float k = 1 - (t - f.t0) / (f.t1 - f.t0);
                camX += (float) Math.sin(t * 61) * 7 * k / camZ;
                camY += (float) Math.cos(t * 47) * 5 * k / camZ;
            }
        }
    }

    /** On vertical / square screens the frame is narrow: keep the speaking (or moving) character in it. */
    private float followX = -1;

    private void followSpeaker(Film.Seg s, float t) {
        Film.Actor best = null;
        for (Film.Actor a : s.actors) {
            if (!a.stateAt(t).visible) continue;
            if (speakingAt(a, t) != null) { best = a; break; }
            if (best == null && moving(a, t) != null) best = a;
        }
        if (best == null) {
            // nobody talking: frame the character nearest to where the director pointed the camera
            float bd = 1e9f;
            for (Film.Actor a : s.actors) {
                if (!a.stateAt(t).visible) continue;
                float d = Math.abs(Director.xAt(a, t) - camX);
                if (d < bd) { bd = d; best = a; }
            }
        }
        if (best != null) {
            float x = Director.xAt(best, t);
            followX = followX < 0 ? x : followX + (x - followX) * 0.08f;
        }
        if (followX >= 0) camX = camX * 0.25f + followX * 0.75f;
        float hw = vw / 2 / camZ;
        camX = Math.max(hw, Math.min(W - hw, camX));
    }

    static boolean outdoor(int set) {
        return set == Sets.GARDEN || set == Sets.FOREST || set == Sets.VILLAGE || set == Sets.COURTYARD || set == Sets.GATE;
    }

    /** Light in the air: sun rays through the scene in the morning/day, a warm key light, haze in caves. */
    /**
     * Deakins: two lights only. The key (one light, from one side, in the colour of the place and the hour) and
     * its bounce (soft, from the ground, in the ground's own colour). Nothing else lights the frame — the rim
     * light on the characters is the key's own edge.
     */
    private void light(Gfx g, Film.Seg s, float t) {
        PixarLead.Key key = PixarLead.keyLight(s.set, s.tod);
        boolean day = s.tod == Sets.MORNING || s.tod == Sets.DAY;
        boolean cave = s.set == Sets.CAVE_IN || s.set == Sets.CAVE_MOUTH || s.set == Sets.BASEMENT;
        // 1. the key (a basement's tubelight flickers)
        float kx = vw * (0.5f + 0.42f * key.dir) - (camX - 640) * 0.1f;
        int kc = key.color & 0xFFFFFF;
        float ka = key.strength * (cave ? 0.22f : s.tod == Sets.NIGHT ? 0.3f : day && outdoor(s.set) ? 0.38f : 0.28f);
        if (s.set == Sets.BASEMENT) ka *= 0.6f + 0.6f * Sets.flicker(t);
        g.radial(kx, -vh * 0.12f, vw * (cave ? 0.6f : 0.95f), Puppet.alpha(0xFF000000 | kc, ka), kc);
        g.rect(0, 0, vw, vh);
        if (day && outdoor(s.set)) {
            // the key's rays through the air (god rays), from the key's side
            for (int i = 0; i < 5; i++) {
                float sway = (float) Math.sin(t * 0.25 + i * 1.7) * 18;
                float x0 = kx + (i - 2) * vw * 0.13f - 170 + sway;
                float w0 = 26 + i * 9, w1 = 120 + i * 30;
                float lean = -key.dir * 330;
                g.begin();
                g.moveTo(x0, -10);
                g.lineTo(x0 + w0, -10);
                g.lineTo(x0 + w0 + lean + w1 * Math.signum(lean == 0 ? 1 : lean), vh + 10);
                g.lineTo(x0 + lean, vh + 10);
                g.close();
                g.linear(x0, 0, x0 + lean, vh, Puppet.alpha(0xFF000000 | kc, 0.13f), kc);
                g.fillPath();
            }
        }
        // 2. the bounce: from the ground, in its own colour (the picture's lower part, or the place's floor)
        int bc = bd(s) != null && bd(s).avgLow != 0 ? bd(s).avgLow : bounceColor(s.set, s.tod);
        float ba = cave ? 0.3f : s.tod == Sets.NIGHT ? 0.12f : 0.17f;
        g.linear(0, vh * 0.5f, 0, vh, Puppet.alpha(bc, 0f), Puppet.alpha(bc, ba));
        g.rect(0, 0, vw, vh);
    }

    /** The colour the floor of a painted place bounces back (the bounce light). */
    static int bounceColor(int set, int tod) {
        if (set == Sets.CAVE_IN || set == Sets.CAVE_MOUTH) return 0xFF30584A;
        if (set == Sets.BASEMENT) return 0xFF22362A;
        if (tod == Sets.NIGHT) return set == Sets.ROOFTOP || set == Sets.STREET ? 0xFF2A3A5A : 0xFF203050;
        switch (set) {
            case Sets.ROOFTOP: return 0xFFB8B4A8;
            case Sets.ROOM: return 0xFFD8CFC0;
            case Sets.STREET: return 0xFF9A9A94;
            case Sets.HALL: return 0xFFE8D8C0;
            case Sets.COURTYARD: case Sets.GATE: return 0xFFD9C8A8;
            case Sets.VILLAGE: return 0xFFC8A880;
            case Sets.CELEBRATION: return 0xFFE8B070;
            case Sets.FOREST: return 0xFF5C8A48;
            default: return 0xFF9CB860;
        }
    }

    private float[] bdMesh = new float[(Nature.MW + 1) * (Nature.MH + 1) * 2];

    /** The mesh buffer for a place picture of this many cells (grown when a finer mesh is needed). */
    private float[] bdMesh(int[] ms) {
        int need = (ms[0] + 1) * (ms[1] + 1) * 2;
        if (bdMesh.length < need) bdMesh = new float[need];
        return bdMesh;
    }

    /** In the sky of the far layer: stars, a rainbow, and the bolts of lightning. */
    private void skyNature(Gfx g, Film.Seg s, float t, float skyBottom) {
        if (film == null) return;
        float skyY = Math.max(120, skyBottom * H);
        float stars = film.weather(Film.W_STARS, t);
        if (stars > 0 && (s.tod == Sets.NIGHT || s.tod == Sets.EVENING) && skyBottom > 0.12f) Nature.stars(g, W, skyY * 0.95f, t, stars);
        float bow = film.weather(Film.W_RAINBOW, t);
        if (bow > 0) Nature.rainbow(g, W * 0.62f, skyY + 60, W * 0.42f, bow);
        float storm = film.weather(Film.W_STORM, t), rain = film.weather(Film.W_RAIN, t);
        float cl = Math.max(film.weather(Film.W_CLOUDS, t), Math.max(storm, rain * 0.8f));
        boolean dark = storm > 0 || rain > 0.9f;
        for (Film.Weather w : film.weather) if (w.type == Film.W_CLOUDS && w.kind == 1 && t >= w.t0 && t <= w.t1) dark = true;
        if (cl > 0 && Sets.outdoorSet(s.set)) Nature.clouds(g, W, skyY, t, cl, dark);
        float birds = film.weather(Film.W_BIRDS, t);
        if (birds <= 0 && s.tod == Sets.MORNING && Sets.outdoorSet(s.set) && rain <= 0 && storm <= 0) birds = 0.45f;   // birds are about in the morning
        if (birds > 0 && Sets.outdoorSet(s.set)) Nature.birds(g, W, skyY, t, birds);
        for (Film.Fx f : s.fx) {
            if (f.type == Film.FX_LIGHTNING && t >= f.t0 && t < f.t1) Nature.lightningBolt(g, f.x, -20, skyY + 40, t - f.t0, f.color + 7);
        }
    }

    /** The water's band {top, bottom} on the stage (the picture's water, or the painted sea), or null when there is none. */
    private float[] waterBand(Film.Seg s, Nature.Scan sc, Art.Backdrop b) {
        if (sc != null && sc.anyWater && sc.waterTop >= 0) return new float[]{(sc.waterTop - b.y0) / (b.y1 - b.y0) * H, (sc.waterBottom - b.y0) / (b.y1 - b.y0) * H};
        if (b == null) return new float[]{s.ground - 200, s.ground - 35};
        return null;
    }

    /** The sea's roughness now: wind and storm. */
    private float rough(float t) { return film == null ? 0 : Math.min(1.5f, Math.abs(film.wind(t)) + film.weather(Film.W_STORM, t)); }

    /** Water life: sea waves rolling to the shore and a floating boat (on the picture's water, or a painted sea). */
    private void waterNature(Gfx g, Film.Seg s, float t, Nature.Scan sc, Art.Backdrop b) {
        if (film == null) return;
        float sea = film.weather(Film.W_SEA, t), boat = film.weather(Film.W_BOAT, t);
        if (sea <= 0 && boat <= 0 && !s.inBoat) return;
        float rough = rough(t);
        float[] band = waterBand(s, sc, b);
        if (band == null) return;     // the picture shows no water: no boat on dry land
        float top = band[0], bottom = band[1];
        if (b == null) Nature.paintedSea(g, W, top, bottom, t, Math.max(sea, s.inBoat ? 0.6f : 0) + rough * 0.5f);
        if (sea > 0 || s.inBoat) Nature.shoreWaves(g, sc, b == null ? 0 : b.x0, b == null ? 0 : b.y0, b == null ? 1 : b.x1, b == null ? 1 : b.y1, W, H, b == null ? top + 20 : top, bottom, t, Math.max(sea, s.inBoat ? 0.6f : 0) + rough * 0.5f);
        // the boat far out on the water; when the characters are in it, it is drawn with them (near layer)
        if (boat > 0 && !s.inBoat) Nature.boat(g, W * 0.72f, top + (bottom - top) * 0.45f, t, 0.9f + 0.4f * (bottom - top) / H, rough);
    }

    /** The boat the characters are in: its deck's y on the stage, its bob and its roll (degrees) at t. */
    private float[] deck(Film.Seg s, float t) {
        float[] band = waterBand(s, bd(s) == null ? null : bd(s).scan, bd(s));
        if (band == null) band = new float[]{s.ground - 200, s.ground - 35};
        float waterY = band[0] + (band[1] - band[0]) * 0.55f, size = 2.4f, rough = rough(t);
        float bob = (float) Math.sin(t * 1.4) * 5 * size * (1 + 2 * rough);
        float roll = (float) (Math.sin(t * 1.1 + 0.7) * (3 + 10 * rough) + (rough > 0.5f ? Math.sin(t * 23) * 1.5 * rough : 0));
        float drift = (float) Math.sin(t * 0.2) * 20 * size;
        return new float[]{waterY, bob, roll, drift, size};
    }

    /** Candles, diyas or torches, flickering and lighting the place. */
    private void candles(Gfx g, Film.Seg s, float t, int kind, float amount) {
        float wind = film.wind(t);
        if (kind == 1) {
            for (int i = 0; i < 7; i++) Nature.flame(g, 140 + i * 166, s.ground + 18, t, 1.7f * amount, wind, 1, i);
        } else if (kind == 2) {
            Nature.flame(g, 90, s.ground - 260, t, 1.1f * amount, wind, 2, 1);
            Nature.flame(g, 1190, s.ground - 260, t, 1.1f * amount, wind, 2, 2);
        } else {
            Nature.flame(g, 150, s.ground - 30, t, 1.3f * amount, wind, 0, 3);
            Nature.flame(g, 1130, s.ground - 30, t, 1.3f * amount, wind, 0, 4);
        }
    }

    /** Rain, snow, blowing leaves and petals, dust and fog, in front of everything. */
    private void weatherScreen(Gfx g, Film.Seg s, float t) {
        if (film == null) return;
        boolean out = Sets.outdoorSet(s.set);
        float wind = film.wind(t);
        float rain = Math.max(film.weather(Film.W_RAIN, t), film.weather(Film.W_STORM, t) * 1.3f);
        if (rain > 0 && out) Nature.rain(g, vw, vh, t, rain, wind);
        else if (rain > 0) {
            // indoors: only the grey light and rain beyond the windows
            g.color(Puppet.alpha(0xFF40506A, 0.1f * Math.min(1, rain)));
            g.rect(0, 0, vw, vh);
        }
        // a storm (and heavy rain) darkens the whole frame: the sky closes in
        float storm = film.weather(Film.W_STORM, t);
        if (storm > 0 || rain > 0.5f) { g.color(Puppet.alpha(0xFF101828, Math.min(0.45f, 0.3f * storm + 0.28f * Math.max(0, rain - 0.5f)))); g.rect(0, 0, vw, vh); }
        float snow = film.weather(Film.W_SNOW, t);
        if (snow > 0 && out) Nature.snow(g, vw, vh, t, snow, wind);
        float leaves = Math.max(film.weather(Film.W_LEAVES, t), out ? (Math.abs(wind) - 0.45f) * 1.4f : 0);
        if (leaves > 0 && out) Nature.leaves(g, vw, vh, t, leaves, wind, false);
        float petals = film.weather(Film.W_PETALS, t);
        if (petals > 0) Nature.leaves(g, vw, vh, t, petals, wind, true);
        float dust = film.weather(Film.W_DUST, t);
        if (dust > 0 && out) Nature.dust(g, vw, vh, t, dust, wind);
        float fog = film.weather(Film.W_FOG, t);
        if (fog > 0) Nature.fog(g, vw, vh, t, fog);
        float quake = film.weather(Film.W_QUAKE, t);
        if (quake > 0) Nature.quakeDust(g, vw, vh, t, quake);
        float fire = film.weather(Film.W_FIRE, t);
        if (fire > 0) Nature.fireLight(g, vw, vh, t, fire);
    }

    /** Out-of-focus leaves and flowers right in front of the lens: they slide faster than the scene (depth). */
    private void foreground(Gfx g, final Film.Seg s, float t) {
        // only on wide films and wide shots: narrow (vertical/square) frames need every pixel for the characters
        if (!outdoor(s.set) || camZ > 1.6f || vw / vh < 1.6f) return;
        final boolean night = s.tod == Sets.NIGHT || s.tod == Sets.EVENING;
        final float W2 = vw, H2 = vh;
        float a = Math.max(0, Math.min(1, (1.6f - camZ) / 0.4f));
        g.save();
        g.setAlpha(a * 0.8f);
        g.translate(-(camX - 640) * 0.45f, 0);
        g.layerLow("fg2:" + s.set + ":" + night + ":" + (int) W2, W2, H2, 0.12f, new Gfx.Painter() {
            public void paint(Gfx gg) {
                int leaf = night ? 0xFF0E2416 : 0xFF2E6B2A, leaf2 = night ? 0xFF16301C : 0xFF4E8F34;
                // a bush in the bottom-left corner and leaves in the bottom-right corner, never across the middle
                float[][] l = {{-40, H2 + 10, 120, 80}, {40, H2 + 30, 90, 60}, {-20, H2 - 40, 70, 50}, {110, H2 + 40, 60, 40}};
                float[][] r = {{W2 + 30, H2 + 10, 110, 80}, {W2 - 50, H2 + 35, 80, 55}, {W2 + 10, H2 - 45, 60, 45}};
                for (int i = 0; i < l.length; i++) { gg.color(i % 2 == 0 ? leaf : leaf2); gg.oval(l[i][0], l[i][1], l[i][2], l[i][3]); }
                for (int i = 0; i < r.length; i++) { gg.color(i % 2 == 0 ? leaf2 : leaf); gg.oval(r[i][0], r[i][1], r[i][2], r[i][3]); }
                if (!night) {
                    gg.color(0xFFE85A8C); gg.oval(W2 - 60, H2 - 35, 20, 17);
                    gg.color(0xFFF2C230); gg.oval(70, H2 - 25, 17, 14);
                }
            }
        });
        g.restore();
    }

    /** Fine moving film grain (very light) for a cinema finish. */
    private void grain(Gfx g, float t) {
        final float W2 = vw, H2 = vh;
        g.save();
        g.setAlpha(0.05f);
        int k = ((int) (t * 24)) % 4;
        g.translate(-(k % 2) * 3, -(k / 2) * 3);
        g.layer("grain:" + (int) W2, W2 + 6, H2 + 6, new Gfx.Painter() {
            public void paint(Gfx gg) {
                java.util.Random r = new java.util.Random(7);
                for (int i = 0; i < 2600; i++) {
                    gg.color(r.nextBoolean() ? 0xFFFFFFFF : 0xFF000000);
                    gg.rect(r.nextFloat() * (W2 + 6), r.nextFloat() * (H2 + 6), 1.6f, 1.6f);
                }
            }
        });
        g.restore();
    }

    /** Slim cinema bars on wide (16:9) films. */
    private void letterbox(Gfx g) {
        if (vw / vh < 1.6f || (curSeg != null && curSeg.solid != 0)) return;
        float b = vh * 0.055f;
        g.color(0xFF000000);
        g.rect(0, 0, vw, b);
        g.rect(0, vh - b, vw, b);
    }

    /** Cinematic finish: vignette and a mood colour grade. */
    private void grade(Gfx g, Film.Seg s) {
        int tint;
        switch (s.mood) {
            case Film.M_TENSE: case Film.M_VILLAIN: tint = 0x1E102A60; break;
            case Film.M_SAD: tint = 0x22505A70; break;
            case Film.M_NIGHT: tint = 0x1A0A1640; break;
            case Film.M_CELEBRATE: case Film.M_HAPPY: case Film.M_PLAYFUL: tint = 0x10FFB347; break;
            case Film.M_ACTION: tint = 0x14FF5A2A; break;
            default: tint = 0;
        }
        if (tint != 0) { g.color(tint); g.rect(0, 0, vw, vh); }
        // light follows the moment (§19): hard, directional light and deeper shadows for conflict and fear,
        // soft warm light for warmth and safety
        float hard = Math.max(0, Math.min(1, camLight));
        if (hard > 0.55f) {
            float k = (hard - 0.55f) / 0.45f;
            g.linear(0, 0, vw, 0, Puppet.alpha(0xFF000000, 0.0f), Puppet.alpha(0xFF000000, 0.28f * k));
            g.rect(0, 0, vw, vh);
        } else if (hard < 0.3f) {
            // a soft moment: a warm veil over the whole frame (not a third light — two lights only)
            float k = (0.3f - hard) / 0.3f;
            g.color(Puppet.alpha(0xFFFFE6C0, 0.07f * k));
            g.rect(0, 0, vw, vh);
        }
        float r = Math.max(vw, vh) * (0.78f - 0.12f * Math.max(0, hard - 0.5f));
        g.radial(vw / 2, vh / 2, r, 0x00000000, Puppet.alpha(0xFF000000, 0.44f + 0.2f * Math.max(0, hard - 0.5f)));
        g.rect(0, 0, vw, vh);
    }

    /**
     * Camera transform. depth 1 = characters; depth < 1 = the far background, which zooms and pans less than the
     * characters (parallax). The far layer pivots on the ground line, so feet always stay on the floor.
     */
    private void applyCam(Gfx g, float depth, float ground) {
        g.translate(vw / 2, vh / 2);
        if (camRoll != 0) g.rotate(camRoll);
        g.scale(camZ, camZ);
        g.translate(-camX, -camY);
        if (depth < 1) {
            float k = (1 + (camZ - 1) * depth) / camZ * 1.1f;       // 1.1: a little larger so edges never show
            float lag = (camX - 640) * (1 - depth) * 0.8f;          // background follows the camera a bit
            // camera height: from low down the far world sinks behind the characters, from high up it rises
            g.translate(0, camAngle * 34 * (1 - depth) / camZ);
            g.translate(camX + lag, ground);
            g.scale(k, k);
            g.translate(-camX, -ground);
        }
    }

    /** The part being drawn (for its light). */
    private Film.Seg curSeg;

    private void drawScene(Gfx g, final Film.Seg s, float t) {
        curSeg = s;
        camera(s, t);
        pixelStep = curGw > 0 ? vw / curGw / Math.max(0.1f, camZ) : 0;
        // a narrow frame follows the speaker only when the camera is free; a locked shot (Technical Director)
        // was framed for this shape by the director and never moves
        if (vw < W * 0.9f && !camStill) followSpeaker(s, t);
        // ---- far layer: the background (parallax)
        g.save();
        applyCam(g, 0.78f, s.ground);
        float wind = film == null ? 0 : film.wind(t);
        if (s.solid != 0) {
            // a solid colour (the thumbnail and the poster): the hero's palette, nothing behind to distract
            g.color(s.solid);
            g.rect(-W, -H, W * 3, H * 3);
            g.linear(0, s.ground - 40, 0, s.ground + 60, Puppet.alpha(0xFF000000, 0f), Puppet.alpha(0xFF000000, 0.25f));
            g.rect(-W, s.ground - 40, W * 3, H);
        } else if (bd(s) != null) {
            final Art.Backdrop b = bd(s);
            Gfx.Painter bp = new Gfx.Painter() {
                public void paint(Gfx gg) {
                    gg.imageRect(b.img, b.x0 * b.w, b.y0 * b.h, (b.x1 - b.x0) * b.w, (b.y1 - b.y0) * b.h, 0, 0, W, H);
                }
            };
            Nature.Scan sc = b.scan;
            {   // every place picture is drawn through a mesh fine to the pixel
                // a living picture: plants sway (more in the wind), water ripples along, a waterfall streams down
                float onScreen = W / Math.max(1e-3f, b.x1 - b.x0) * (1 + (camZ - 1) * 0.78f) * 1.1f * g.width() / vw;
                int[] ms = Nature.meshSize(sc, onScreen, b.w, b.h);
                float[] mesh = bdMesh(ms);
                Nature.backdropMesh(sc, b.x0, b.y0, b.x1, b.y1, W, H, t, wind, film == null ? 0 : film.weather(Film.W_SEA, t), mesh, ms[0], ms[1]);
                g.imageMesh(b.img, ms[0], ms[1], mesh);
                if (sc != null) Nature.waterLife(g, sc, b.x0, b.y0, b.x1, b.y1, W, H, t);
            }
            // a close-up softens the place behind the face, never melts it: a half-size layer, at most two thirds in,
            // only once the shot is a real close-up
            float dof = Math.max(0, Math.min(0.66f, (camZ - 1.6f) / 0.7f));
            if (dof > 0.02f) {
                g.save();
                g.setAlpha(dof);
                g.layerLow("bdblur:" + System.identityHashCode(b), W, H, 0.5f, bp);
                g.restore();
            }
            if (s.tod == Sets.EVENING) { g.color(0x40FF7043); g.rect(0, 0, W, H); g.color(0x30301060); g.rect(0, 0, W, H); }
            if (s.tod == Sets.NIGHT && s.set != Sets.FOREST) { g.color(0x50101C3A); g.rect(0, 0, W, H); }
            if (s.festive) Sets.celebrationLights(g, t);
            skyNature(g, s, t, sc == null ? 0.35f : sc.skyBottom);
            waterNature(g, s, t, sc, b);
        } else {
            final int set = s.set, tod = s.tod;
            g.layer("set:" + set + ":" + tod, W, H, new Gfx.Painter() { public void paint(Gfx gg) { Sets.paintStatic(gg, set, tod); } });
            Sets.paintLive(g, set, tod, t);
            skyNature(g, s, t, 0.38f);
            waterNature(g, s, t, null, null);
        }
        g.restore();
        // ---- near layer: characters and effects
        g.save();
        applyCam(g, 1f, s.ground);
        // tree branch for monkeys
        for (Film.Actor a : s.actors) {
            Film.Key k = a.stateAt(t);
            if (k.visible && k.anchor == Film.A_BRANCH) { branch(g, s, k.x); break; }
        }
        // the boat the characters are in (they stand on its deck, rolling and bobbing with it)
        if (s.inBoat) { float[] d = deck(s, t); Nature.boat(g, 640, d[0], t, d[4], rough(t)); }
        drawFxLayer(g, s, t, true);
        List<Film.Actor> list = new ArrayList<Film.Actor>(s.actors);
        final float tt = t;
        // the occlusion rule of a close-up: the face the shot is about is never hidden by a neighbour who
        // happens to stand in front; that character is drawn last
        String focus = null;
        for (Film.Shot sh : film.shots) if (t >= sh.t && t < sh.t + sh.dur && (sh.speech || sh.reaction)) { focus = sh.subject; break; }
        final String front = focus;
        Collections.sort(list, new Comparator<Film.Actor>() {
            public int compare(Film.Actor a, Film.Actor b) {
                Film.Key ka = a.stateAt(tt), kb = b.stateAt(tt);
                int la = layerOf(ka), lb = layerOf(kb);
                if (la != lb) return la - lb;
                if (front != null) {
                    boolean fa = a.c.shown().equals(front), fb = b.c.shown().equals(front);
                    if (fa != fb) return fa ? 1 : -1;
                }
                return a.order - b.order;
            }
        });
        for (Film.Actor a : list) drawActor(g, s, a, t);
        drawFxLayer(g, s, t, false);
        if (film != null) {
            float rain = Math.max(film.weather(Film.W_RAIN, t), film.weather(Film.W_STORM, t));
            if (rain > 0 && Sets.outdoorSet(s.set)) Nature.rainSplashes(g, s.ground, t, rain);
            // night: the whole stage darkens; flames, fire and fireflies then shine on top of it
            if (s.tod == Sets.NIGHT || s.tod == Sets.EVENING) {
                boolean photo = bd(s) != null;
                int dark = s.set == Sets.BASEMENT ? (photo ? 0x80061410 : 0x40061410) : s.tod == Sets.NIGHT ? (photo ? 0x8C081026 : 0x46081026) : (photo ? 0x30301030 : 0x18301030);
                g.color(dark);
                g.rect(-W, -H, W * 3, H * 3);
            }
            float fire = film.weather(Film.W_FIRE, t);
            if (fire > 0) for (Film.Weather w : film.weather) if (w.type == Film.W_FIRE && t >= w.t0 && t <= w.t1 + 1.5f) Nature.fire(g, w.x, s.ground + 6, t, 1.5f * fire);
            float cand = film.weather(Film.W_CANDLES, t);
            if (cand > 0) for (Film.Weather w : film.weather) if (w.type == Film.W_CANDLES && t >= w.t0 && t <= w.t1 + 1.5f) candles(g, s, t, w.kind, cand);
            float ff = film.weather(Film.W_FIREFLIES, t);
            if (ff > 0) Nature.fireflies(g, s.ground, t, ff);
            float wet = film.wetness(t);
            if (wet > 0.05f) for (Film.Actor a : s.actors) {
                Film.Key k = a.stateAt(t);
                if (!k.visible) continue;
                float h = actorHeight(a.look, a.c, art, s);
                Nature.drips(g, Director.xAt(a, t), s.ground - h, s.ground, h * 0.4f, t, wet, a.order);
            }
        }
        if (bd(s) == null && s.solid == 0) Sets.paintFront(g, s.set, s.tod);
        g.restore();
        if (bd(s) == null && s.solid == 0) {
            int tint = Sets.tint(s.set, s.tod);
            if (tint != 0) { g.color(tint); g.rect(0, 0, vw, vh); }
        }
        overShoulder(g, s, t);
        weatherScreen(g, s, t);
        light(g, s, t);
        foreground(g, s, t);
        grade(g, s);
        drawScreenFx(g, s, t);
        grain(g, t);
        letterbox(g);
        if (film.subtitles) drawSubs(g, s, t);
        if (safeZoneOverlay) safeZone(g);
    }

    /** Draws the format's safe zones over the frame (Human QC stills): the side margins, the headroom, the caption zone, the eye line. */
    public boolean safeZoneOverlay;
    /**
     * When set, every standing character drawn in a frame leaves a record {the feet's y as drawn, the ground line
     * it stands on, its height, 1 if it is walking or in an action (a jump, a bounce, getting up) else 0}: the
     * final QC's floating check reads where the feet really went (FinalQc).
     */
    public List<float[]> feetLog;

    private static boolean acting(Film.Actor a, float t) {
        for (Film.Act ac : a.acts) if (t >= ac.t0 - 0.3f && t <= ac.t1 + 0.6f) return true;
        return false;
    }

    private void safeZone(Gfx g) {
        PixarLead.Format f = PixarLead.specFor(vw / vh);
        int c = 0x90FFD54F;
        float l = vw * f.side, r = vw * (1 - f.side), tp = vh * f.top, bt = vh * (1 - f.bottom);
        g.color(c);
        g.rect(l, tp, r - l, 2); g.rect(l, bt - 2, r - l, 2); g.rect(l, tp, 2, bt - tp); g.rect(r - 2, tp, 2, bt - tp);
        g.color(0x9080DEEA);
        g.rect(l, vh * f.eyeLine - 1, r - l, 2);
        g.color(0x60FF8A65);
        g.rect(0, vh * PixarLead.FEET_MIN - 1, vw, 2); g.rect(0, vh * PixarLead.FEET_MAX - 1, vw, 2);
    }

    private static int layerOf(Film.Key k) {
        if (k.anchor == Film.A_SHOULDER || k.anchor == Film.A_CARRIED || k.anchor == Film.A_ON_FACE) return 3;
        if (k.depth > 0) return 0;
        return 1;
    }

    static final float BRANCH_UP = 470;

    private void branch(Gfx g, Film.Seg s, float x) {
        float y = s.ground - BRANCH_UP;
        g.color(0xFF4E342E);
        g.begin();
        g.moveTo(x - 420, y - 60); g.quadTo(x - 150, y - 10, x + 170, y + 4);
        g.quadTo(x - 120, y + 6, x - 420, y - 36); g.close();
        g.fillPath();
        g.color(0xFF6D4C41);
        g.line(x - 400, y - 46, x + 150, y + 1, 4);
        int[] greens = {0xFF2E7D32, 0xFF388E3C, 0xFF43A047, 0xFF1B5E20};
        for (int i = 0; i < 26; i++) {
            float lx = x - 400 + (i * 37 % 540), ly = y - 46 + (i * 13 % 50) - (lx - x + 400) * 0.06f;
            g.save();
            g.translate(lx, ly);
            g.rotate((i * 47) % 360);
            g.color(greens[i % greens.length]);
            g.oval(0, 0, 14, 6);
            g.restore();
        }
    }

    // ================================================================== actors

    private float blink(float t, int seed) {
        float p = (t + seed * 1.37f) % 3.9f;
        return p < 0.13f ? 1f : 0f;
    }

    private Film.Key moving(Film.Actor a, float t) {
        for (Film.Key k : a.keys) if (k.moveDur > 0 && t >= k.t && t < k.t + k.moveDur) return k;
        return null;
    }

    float mouthAt(Film.Actor a, float t) {
        for (Film.Speak sp : a.speaks) {
            if (t >= sp.t0 && t < sp.t1) {
                Film.Line l = film.lines.get(sp.line);
                if (l.env == null || l.env.length == 0) {
                    // fallback: syllable-like motion
                    return 0.3f + 0.3f * (0.5f - 0.5f * (float) Math.cos((t - sp.t0) * 22));
                }
                // between the 10 ms steps, and closing gently at the very start and end of the line
                float fi = (t - l.start) * 100;
                int i = (int) Math.floor(fi);
                if (i < 0 || i >= l.env.length) return 0;
                float v = l.env[i] + (l.env[Math.min(l.env.length - 1, i + 1)] - l.env[i]) * (fi - i);
                float edge = Math.min(t - sp.t0, sp.t1 - t);
                return edge < 0.05f ? v * Math.max(0, edge) / 0.05f : v;
            }
        }
        return 0;
    }

    /** How wide (ee) or round (oo) the speaker's mouth is now (0.5 when unknown). */
    float mouthShapeAt(Film.Actor a, float t) {
        for (Film.Speak sp : a.speaks) {
            if (t >= sp.t0 && t < sp.t1) {
                Film.Line l = film.lines.get(sp.line);
                if (l.shape == null || l.shape.length == 0) return 0.5f + 0.3f * (float) Math.sin((t - sp.t0) * 7);
                float fi = (t - l.start) * 100;
                int i = (int) Math.floor(fi);
                if (i < 0 || i >= l.shape.length) return 0.5f;
                return l.shape[i] + (l.shape[Math.min(l.shape.length - 1, i + 1)] - l.shape[i]) * (fi - i);
            }
        }
        return 0.5f;
    }

    private Film.Speak speakingAt(Film.Actor a, float t) {
        for (Film.Speak sp : a.speaks) if (t >= sp.t0 && t < sp.t1) return sp;
        return null;
    }

    /** Body motion for cut-out pictures (which cannot bend their arms). */
    static final class Motion { float dx, dy, rot, sx = 1, sy = 1; }
    private final Motion mo = new Motion();

    private void drawActor(Gfx g, Film.Seg s, Film.Actor a, float t) {
        Film.Key k = a.stateAt(t);
        if (!k.visible) return;
        // Spider-Verse: a character animated on twos or threes holds each pose for 2-3 frames (the walk, the
        // gestures, the breathing); the place, the camera and the lip-sync stay on ones
        // (Spider-Verse: a scared run steps on twos even for an expert — the fear shows in the choppiness)
        int stepFps = a.stepFps;
        if (stepFps > 0 && stepFps >= 24 && k.emotion == Pose.SCARED && moving(a, t) != null && moving(a, t).run) stepFps = 12;
        final float tp = stepFps > 0 && stepFps < 24 ? (float) (Math.floor(t * stepFps) / stepFps) : t;
        Art.Sprite sp = art.sprites.get(a.c.id);
        float h = actorHeight(a.look, a.c, art, s);
        float x = Director.xAt(a, t);
        // nobody far outside the frame is drawn (their pixel-level mesh would cost time for nothing); anyone
        // carried or held by someone keeps being drawn with them
        if (k.anchor == Film.A_GROUND && Math.abs(x - camX) > vw / 2 / camZ + h * 1.5f + 200) return;
        float y = s.ground;
        float scale = 1f;
        if (k.depth > 0) { y -= 26; scale = 0.88f; }
        pxPerUnit = g.height() / (float) H * camZ * scale;      // how big the picture is on screen: how fine its mesh

        // ---- pose
        Pose p = pose;
        p.reset();
        p.time = tp;
        p.seed = a.order * 13 + 5;
        p.facing = k.facing;
        p.body = k.body;
        p.emotion = k.emotion;
        p.disguised = k.disguised;
        p.noHeadwear = k.noHeadwear;
        p.wearsTurban = k.wearsTurban;
        p.redFace = k.redFace;
        p.tears = k.tears;
        p.sweat = k.sweat;
        p.holdR = k.holdR;
        p.holdL = k.holdL;
        p.eyesClosed = k.eyesShut;
        p.blink = blink(tp, a.order);
        if (film != null && Sets.outdoorSet(s.set)) { p.wind = film.wind(t); p.wet = film.wetness(t); }
        float quakeNow = film == null ? 0 : film.weather(Film.W_QUAKE, t);
        p.sit = sitAmount(a, t);
        if (p.sit > 0 && p.sit < 1 && k.body != Pose.SIT) p.body = Pose.STAND;        // getting up: still rising
        int seat = seatOf(a, t);
        p.turbanColor = 0xFF2F5DB5;
        p.turbanBand = 0xFFC62828;
        for (Film.Actor o : s.actors) if (o != a && o.look.headwear == Look.HW_TURBAN && o.stateAt(t).noHeadwear) { p.turbanColor = o.look.headColor; p.turbanBand = o.look.headBand; p.turbanOwner = o.c.id; }
        Film.Speak spk = speakingAt(a, t);
        if (spk != null) {
            p.mouth = mouthAt(a, t);
            p.mouthWide = mouthShapeAt(a, t);
            if (spk.emotion != Pose.NEUTRAL) p.emotion = spk.emotion;
            if (p.emotion == Pose.SAD) p.tears = true;
        }
        Film.Key mv = moving(a, t);
        mo.dx = 0; mo.dy = 0; mo.rot = 0; mo.sx = 1; mo.sy = 1;
        if (mv != null) {
            float speed = mv.run ? 15f : 9f;
            // the steps start and stop gently (no snap into a stride), over a fifth of a second
            float amt = Rig.smooth(0, 0.2f, tp - mv.t) * Rig.smooth(0, 0.25f, mv.t + mv.moveDur - tp);
            p.walk = (tp - mv.t) * speed;
            p.walkAmt = amt;
            p.facing = mv.facing;
            // a calm, weighted step: a small rise and fall (no hopping) and a slight sway of the body
            mo.dy = -bump(p.walk) * (mv.run ? 6 : 3.5f) * amt;
            mo.rot = (float) Math.sin(p.walk) * (mv.run ? 1.8f : 1.1f) * amt;
            p.armL = 8 + (17 + (float) Math.sin(p.walk) * 25) * amt;
            p.armR = 8 + (17 - (float) Math.sin(p.walk) * 25) * amt;
        } else {
            // anticipation: just before setting off the body dips and leans back a little; after arriving it
            // settles forward and back once (slow in, slow out, follow-through)
            for (Film.Key mk : a.keys) {
                if (mk.moveDur <= 0) continue;
                float dir = Math.signum(mk.x - Director.prevX(a, mk));
                if (dir == 0) continue;
                float before = mk.t - tp, after = tp - (k.t + mk.moveDur);
                if (before > 0 && before < 0.25f) {
                    float u = Rig.smooth(0.25f, 0, before);
                    mo.dy += 2.5f * u;
                    mo.rot -= dir * 1.2f * u;
                } else if (after >= 0 && after < 0.4f) {
                    float u = after / 0.4f;
                    mo.rot += dir * 1.3f * (float) Math.sin(u * Math.PI) * (1 - u);
                }
            }
            // idle breathing
            float br = (float) Math.sin(tp * 2.1f + a.order);
            // (a rigged picture breathes through its own mesh: chest and shoulders, not the whole picture)
            if (sp == null || sp.rig == null) mo.sy = 1 + br * 0.006f;
            p.bob = br * 1.2f;
            // secondary action (the 12 principles): while idle the weight shifts slowly from one foot to the
            // other, and now and then the head turns a little — never while speaking (the head stays almost
            // still in a lip-sync shot), and smaller the closer the camera is
            if (spk == null && k.anchor == Film.A_GROUND && k.body == Pose.STAND) {
                float ws = (float) Math.sin(tp * 0.45f + a.order * 1.3f), close = 1f / Math.max(1f, camZ);
                mo.dx += ws * 2.2f * close; mo.rot += ws * 0.35f * close;
                float gl = (float) Math.sin(tp * 0.21f + a.order * 2.1f);
                if (gl > 0.93f) p.headTilt += (gl - 0.93f) / 0.07f * 3f * close * (p.facing < 0 ? -1 : 1);
            }
        }
        applyActs(a, p, tp);
        // lip-sync protocol: a speaker seen close keeps the head still — a gesture may still move the arms, but
        // never bob or rock the whole body (on a narrow screen the face is the whole frame, and a bob reads as
        // a shake; the frame-by-frame check measured it at 15–19 per cell on 9:16 before this rule)
        if (spk != null && camZ >= 2.5f) { mo.dx *= 0.15f; mo.dy *= 0.15f; mo.rot *= 0.15f; }
        // a shot the user asked to be calmer (Human QC): every movement there is cut by 80%
        float calm = film == null ? 1f : film.calmAt(t);
        if (calm < 1f) {
            mo.dx *= calm; mo.dy *= calm; mo.rot *= calm;
            mo.sx = 1 + (mo.sx - 1) * calm; mo.sy = 1 + (mo.sy - 1) * calm;
            p.armL = 8 + (p.armL - 8) * calm; p.armR = 8 + (p.armR - 8) * calm;
            p.walkAmt *= calm;
        }
        if (p.holdR == Pose.I_WOOD_SWORD || p.holdR == Pose.I_SWORD) {
            // a sword is carried for its fight only: swung while fighting, resting at the side just before and
            // after, and put away once the fight is a few seconds over
            boolean near = false;
            for (Film.Act ac : a.acts) {
                if (ac.type != Film.G_SWORD && ac.type != Film.G_BLOCK) continue;
                if (t >= ac.t0 && t < ac.t1) { p.swing = true; near = true; }
                else if (t >= ac.t0 - 1.5f && t < ac.t1 + 5) near = true;
            }
            if (!near) p.holdR = Pose.I_NONE;
        }
        if (quakeNow > 0) {
            // the ground shakes: everyone staggers and is frightened
            mo.dx += (float) Math.sin(t * 47 + a.order) * 5 * quakeNow;
            mo.rot += (float) Math.sin(t * 31 + a.order * 2) * 2.5f * quakeNow;
            if (p.emotion == Pose.NEUTRAL || p.emotion == Pose.HAPPY) p.emotion = Pose.SCARED;
        }
        if (spk != null) {
            // talking: the body stays planted (no bobbing with every syllable, which reads as shaking on screen);
            // a drawn puppet sways slowly with the phrase, a picture only moves its head and lips
            boolean picture = sp != null && sp.rig != null;
            if (!picture) mo.rot += (float) Math.sin(tp * 1.6f + a.order) * 0.8f;
            if (p.armR < 30 && a.look.kind != Look.MONKEY) {
                p.armR = picture ? 14 + (float) Math.sin(tp * 1.1f + a.order) * 5 : 30 + (float) Math.sin(tp * 2.7f) * 15;
                p.elbowR = 40;
            }
        }

        // ---- placement by anchor
        g.save();
        switch (k.anchor) {
            case Film.A_BRANCH:
                y = s.ground - BRANCH_UP;
                if (k.body != Pose.HANG) y += 4;
                break;
            case Film.A_SHOULDER: {
                Film.Actor t2 = k.anchorActor;
                if (t2 != null) {
                    float th = actorHeight(t2.look, t2.c, art, s);
                    x = Director.xAt(t2, t) + t2.stateAt(t).facing * th * 0.12f;
                    y = s.ground - th * 0.74f;
                    scale *= 0.62f;
                }
                break;
            }
            case Film.A_CARRIED: {
                Film.Actor t2 = k.anchorActor;
                if (t2 != null) {
                    float th = actorHeight(t2.look, t2.c, art, s);
                    x = Director.xAt(t2, t);
                    y = s.ground - th * 0.86f;
                    p.body = Pose.LIE;
                    scale *= 0.85f;
                    Film.Key mv2 = moving(t2, t);
                    if (mv2 != null) y -= bump(t * 9) * 8;
                }
                break;
            }
            case Film.A_ON_FACE: {
                Film.Actor t2 = k.anchorActor;
                if (t2 != null) {
                    float th = actorHeight(t2.look, t2.c, art, s);
                    x = Director.xAt(t2, t) + (float) Math.sin(t * 13) * 6;
                    y = s.ground - th * 0.72f;
                    scale *= 0.7f;
                }
                break;
            }
            case Film.A_ROCK_HIDE:
                y = s.ground + 10;
                break;
            default:
        }
        if (s.inBoat && k.anchor == Film.A_GROUND) {
            // in the boat: on its deck, spread along its length, moving with its bob and roll
            float[] d = deck(s, t);
            int n = 0, idx = 0;
            for (Film.Actor o : s.actors) if (o.stateAt(t).visible && o.stateAt(t).anchor == Film.A_GROUND) { if (o == a) idx = n; n++; }
            float along = (idx - (n - 1) / 2f) * Math.min(150, 360 / Math.max(1, n)) ;
            float rollRad = (float) Math.toRadians(d[2]);
            x = 640 + d[3] + along * (float) Math.cos(rollRad);
            y = d[0] + d[1] - 4 + along * (float) Math.sin(rollRad);
            mo.rot += d[2];
            p.walkAmt = 0;
        }
        // the final QC's floating check: where the feet are drawn against the ground line (FinalQc)
        if (feetLog != null && k.anchor == Film.A_GROUND)
            feetLog.add(new float[]{y + mo.dy, y, h, mv != null || acting(a, t) || (p.sit > 0 && p.sit < 1) ? 1 : 0});
        // a locked shot never shimmers: the picture lands on whole screen pixels (sub-pixel drift re-samples the
        // mesh differently every frame and reads as a shake)
        float px = x + mo.dx, py = y + mo.dy;
        if (camStill && pixelStep > 0) { px = Math.round(px / pixelStep) * pixelStep; py = Math.round(py / pixelStep) * pixelStep; }
        g.translate(px, py);
        if (scale != 1f) g.scale(scale, scale);
        if (k.netted) mo.sy *= 0.97f;
        if (seat == Film.SEAT_FLOOR && p.sit > 0.05f) { g.color(0x30000000); g.oval(0, -h * 0.02f, h * 0.45f, h * 0.035f); }
        g.save();
        if (sp != null) drawSprite(g, viewOf(sp, a, s, p, k, spk != null, t), a.look, p, h, mo.rot, a);
        else {
            if (mo.rot != 0) g.rotate(mo.rot * 0.5f);
            // a drawn character keeps a little of the hand (Miyazaki's 10 %): its lines wobble a hair's breadth, on twos
            int boil = (int) (t * 12) * 31 + a.order * 17;
            float wx = ((boil * 1103515245 + 12345) >>> 16 & 255) / 255f - 0.5f, wy = ((boil * 22695477 + 1) >>> 16 & 255) / 255f - 0.5f;
            g.translate(wx * 0.9f, wy * 0.9f);
            Puppet.draw(g, a.look, p, h);
        }
        g.restore();
        // seated on a throne or stool: its front (cushion edge, armrests) is in front of the sitter's legs
        if (seat >= Film.SEAT_STOOL && p.sit > 0.35f && p.body != Pose.LIE) {
            g.save();
            g.setAlpha(Math.min(1, (p.sit - 0.35f) / 0.4f));
            drawSeat(g, seat, h, true);
            g.restore();
        }
        g.restore();
    }

    /** 0..1..0 with the rhythm of |sin x| but rounded at the bottom: a bounce that never jerks. */
    static float bump(double x) { return (float) (0.5 - 0.5 * Math.cos(2 * x)); }

    /** Converts gestures into arm angles (cartoon puppets) and body motion (picture sprites). */
    private void applyActs(Film.Actor a, Pose p, float t) {
        for (Film.Act act : a.acts) {
            if (t < act.t0 || t >= act.t1) continue;
            float u = t - act.t0;
            float s = (float) Math.sin(u * 10);
            // slow in, slow out: every gesture grows in over a quarter second and settles back at its end,
            // so nothing snaps into place (anticipation and follow-through of the arms and body)
            float env = Rig.smooth(0, 0.28f, u) * Rig.smooth(0, 0.32f, act.t1 - t);
            float aL = p.armL, aR = p.armR, eL = p.elbowL, eR = p.elbowR, dx0 = mo.dx, dy0 = mo.dy, r0 = mo.rot, sx0 = mo.sx, sy0 = mo.sy;
            applyAct(a, p, act, t, u, s);
            if (env < 1) {
                p.armL = aL + (p.armL - aL) * env; p.armR = aR + (p.armR - aR) * env;
                p.elbowL = eL + (p.elbowL - eL) * env; p.elbowR = eR + (p.elbowR - eR) * env;
                mo.dx = dx0 + (mo.dx - dx0) * env; mo.dy = dy0 + (mo.dy - dy0) * env; mo.rot = r0 + (mo.rot - r0) * env;
                mo.sx = sx0 + (mo.sx - sx0) * env; mo.sy = sy0 + (mo.sy - sy0) * env;
            }
        }
    }

    private void applyAct(Film.Actor a, Pose p, Film.Act act, float t, float u, float s) {
        {
            switch (act.type) {
                case Film.G_TALK:
                    // a picture speaking in a close-up keeps its body calm (lip-sync protocol): a small, slow hand
                    if (art != null && art.sprites.containsKey(a.c.id) && art.sprites.get(a.c.id).rig != null) {
                        p.armR = Math.max(p.armR, 12 + (float) Math.sin(u * 1.3f) * 4);
                    } else {
                        p.armR = Math.max(p.armR, 35 + (float) Math.sin(u * 3.1f) * 20); p.elbowR = 45;
                    }
                    break;
                case Film.G_CLAP:
                    p.armL = 60; p.armR = 60; p.elbowL = 70 + s * 20; p.elbowR = 70 + s * 20;
                    mo.dy -= bump(u * 10) * 4;
                    break;
                case Film.G_LAUGH:
                    p.emotion = p.mouth > 0.05f ? p.emotion : Pose.LAUGH;
                    if (p.emotion == Pose.NEUTRAL) p.emotion = Pose.LAUGH;
                    mo.dy -= bump(u * 14) * 6;
                    mo.rot += (float) Math.sin(u * 7) * 3;
                    p.armL = 30; p.armR = 30; p.elbowL = 80; p.elbowR = 80;
                    break;
                case Film.G_SWORD:
                    p.armR = 100 + s * 60; p.elbowR = 10;
                    mo.dx += (float) Math.sin(u * 5) * 18 * p.facing;
                    mo.rot += s * 4 * p.facing;
                    break;
                case Film.G_BLOCK:
                    p.armR = 120 + s * 15; p.elbowR = 30;
                    mo.dx -= (float) Math.sin(u * 5) * 10 * p.facing;
                    break;
                case Film.G_WAND:
                    p.armR = 120 + s * 35; p.elbowR = 10; p.glowWand = true;
                    mo.rot += s * 2;
                    break;
                case Film.G_POINT:
                    p.armR = 95; p.elbowR = 0;
                    mo.rot += 3 * p.facing;
                    break;
                case Film.G_SALUTE:
                    p.armR = 150; p.elbowR = 100;
                    mo.rot -= Math.min(1, u * 3) * 4 * p.facing;
                    mo.sy *= 1.01f;
                    break;
                case Film.G_HUG: case Film.G_HOLD_HAND:
                    p.armL = 70; p.armR = 70; p.elbowL = 60; p.elbowR = 60;
                    mo.rot += 5 * p.facing;
                    p.emotion = Pose.HAPPY;
                    break;
                case Film.G_CRY:
                    p.tears = true; p.emotion = Pose.SAD;
                    mo.dy += bump(u * 10) * 2;
                    break;
                case Film.G_TREMBLE:
                    mo.dx += (float) Math.sin(t * 70) * 2.2f;
                    p.emotion = p.emotion == Pose.NEUTRAL ? Pose.SCARED : p.emotion;
                    break;
                case Film.G_CRUSH:
                    p.armR = 80; p.elbowR = 60; p.fist = true;
                    mo.rot += s * 2;
                    break;
                case Film.G_DANCE:
                    mo.rot += (float) Math.sin(u * 6) * 8;
                    mo.dy -= bump(u * 6) * 12;
                    p.armL = 140 + s * 20; p.armR = 140 - s * 20;
                    p.emotion = Pose.EVIL;
                    break;
                case Film.G_PAT:
                    p.armR = 110; p.elbowR = 40 + s * 15;
                    p.emotion = Pose.HAPPY;
                    break;
                case Film.G_GIVE: case Film.G_OFFER:
                    p.armR = 75; p.elbowR = 10;
                    mo.rot += 2 * p.facing;
                    break;
                case Film.G_RUB_EYES:
                    p.armL = 150; p.armR = 150; p.elbowL = 120 + s * 20; p.elbowR = 120 - s * 20;
                    p.eyesClosed = true; p.emotion = Pose.PAIN;
                    mo.dx += (float) Math.sin(t * 40) * 3;
                    mo.rot += (float) Math.sin(u * 4) * 5;
                    break;
                case Film.G_FLAIL:
                    p.armL = 120 + s * 50; p.armR = 120 - s * 50;
                    mo.rot += (float) Math.sin(u * 9) * 7;
                    mo.dx += (float) Math.sin(u * 7) * 10;
                    p.emotion = Pose.PAIN;
                    break;
                case Film.G_PUSH:
                    p.armL = 90; p.armR = 90; p.elbowL = 10; p.elbowR = 10;
                    mo.rot += 8 * p.facing;
                    break;
                case Film.G_PULL_ROPE:
                    p.armL = 165; p.armR = 165; p.elbowL = 10; p.elbowR = 10;
                    p.carrying = a.look.kind == Look.MONSTER;
                    if (a.look.kind != Look.MONSTER) mo.dy += bump(u * 10) * 10;
                    break;
                case Film.G_TWIRL:
                    p.armR = 140; p.elbowR = 120 + s * 12;
                    p.twirl = true;
                    p.emotion = Pose.PROUD;
                    break;
                case Film.G_HOLD_HEAD:
                    p.armL = 160; p.armR = 160; p.elbowL = 110; p.elbowR = 110;
                    p.emotion = Pose.SCARED;
                    mo.dx += (float) Math.sin(t * 30) * 2;
                    break;
                case Film.G_REACH:
                    p.armR = 100; p.armL = 80; p.elbowR = 0; p.elbowL = 0;
                    break;
                case Film.G_FIST:
                    p.armR = 40; p.elbowR = 90; p.fist = true; p.emotion = Pose.DETERMINED;
                    mo.sy *= 1.015f;
                    break;
                case Film.G_SCRATCH:
                    p.armL = 120 + s * 30; p.armR = 120 - s * 30;
                    mo.dx += (float) Math.sin(t * 25) * 4;
                    p.emotion = Pose.ANGRY;
                    break;
                case Film.G_JUMP: {
                    // a real jump: crouch, take off, fly on a parabola under gravity, land and squash
                    float d = act.t1 - act.t0, crouch = Math.min(0.15f, d * 0.2f), air = d - crouch - 0.12f;
                    // squash and stretch (Disney): the crouch squashes to 80 %, the take-off stretches to 120 %, the landing squashes again
                    if (u < crouch) { float k2 = u / crouch; mo.sy *= 1 - 0.2f * k2; mo.sx *= 1 + 0.12f * k2; }
                    else if (u < crouch + air) {
                        float a2 = u - crouch, peak = 80, g2 = 8 * peak / (air * air), v0 = g2 * air / 2;
                        mo.dy -= v0 * a2 - 0.5f * g2 * a2 * a2;
                        float rise = Math.max(0, 1 - a2 / (air * 0.35f));
                        mo.sy *= 1.04f + 0.16f * rise; mo.sx *= 1 - 0.08f * rise;
                        p.armL = 150; p.armR = 150;
                    } else { mo.sy *= 1 - 0.1f * (1 - (u - crouch - air) / 0.12f); }
                    break;
                }
                case Film.G_BOW: {
                    float k2 = (float) Math.sin(Math.PI * Math.min(1, u / (act.t1 - act.t0)));
                    p.tilt += 18 * k2; p.nod += 0.8f * k2; p.headTilt += 4 * k2;
                    p.armL = 55; p.armR = 55; p.elbowL = 115; p.elbowR = 115;     // hands together: namaste
                    break;
                }
                case Film.G_WAVE:
                    p.wave = 1; p.armR = 150 + (float) Math.sin(u * 12) * 20; p.elbowR = 30;
                    if (p.emotion == Pose.NEUTRAL) p.emotion = Pose.HAPPY;
                    break;
                case Film.G_NOD:
                    p.nod += (float) Math.sin(u * 10) * 0.9f;
                    break;
                case Film.G_LOOK_AWAY: {
                    // looks away (uncomfortable, hurt), then back (§11)
                    float k2 = (float) Math.sin(Math.PI * Math.min(1, u / Math.max(0.3f, act.t1 - act.t0)));
                    p.headTilt -= 9 * k2 * (p.facing < 0 ? -1 : 1);
                    p.nod += 0.45f * k2;
                    break;
                }
                case Film.G_TURN: {
                    float k2 = Math.min(1, u / (act.t1 - act.t0));
                    mo.sx *= Math.max(0.15f, Math.abs((float) Math.cos(Math.PI * k2)));
                    break;
                }
                case Film.G_LISTEN: {
                    // thought before action (handbook ch. 6): the eyes go first, the head turns a little toward the
                    // sound and holds, the arms settle, nothing else moves — then the action
                    float k2 = Rig.smooth(0, 0.22f, u) * Rig.smooth(0, 0.25f, act.t1 - act.t0 - u);
                    p.headTilt += 6 * k2 * (p.facing < 0 ? -1 : 1);
                    p.nod -= 0.18f * k2;
                    p.armL = p.armL + (8 - p.armL) * 0.5f * k2; p.armR = p.armR + (8 - p.armR) * 0.5f * k2;
                    break;
                }
                case Film.G_HEAD_SCRATCH: {
                    // the comic beat (Russo / Gunn): a puzzled scratch of the head — the arm comes up, the fingers
                    // wiggle, the head tilts into the hand, a small shrug
                    float k2 = (float) Math.sin(Math.PI * Math.min(1, u / Math.max(0.3f, act.t1 - act.t0)));
                    p.armR = Math.max(p.armR, 150 * k2);
                    p.elbowR = 120 * k2 + (float) Math.sin(u * 22) * 8 * k2;
                    p.headTilt += 7 * k2 * (p.facing < 0 ? -1 : 1);
                    mo.rot += 1.5f * k2;
                    mo.dy -= 2 * k2;
                    if (p.emotion == Pose.NEUTRAL) p.emotion = Pose.SURPRISED;
                    break;
                }
                case Film.G_WEIGHT_SHIFT: {
                    mo.dx += (float) Math.sin(u * 1.5f) * 1.2f;
                    mo.rot += (float) Math.sin(u * 1.5f) * 0.25f;
                    break;
                }
                case Film.G_SHIELD_EYES: {
                    // blinded: the arm comes up over the eyes, the head turns away and the body leans back
                    p.armR = Math.max(p.armR, 155); p.elbowR = 125;
                    p.headTilt -= 10 * (p.facing < 0 ? -1 : 1);
                    mo.rot -= 4 * p.facing;
                    mo.dx -= 6 * p.facing;
                    p.eyesClosed = true;
                    break;
                }
                case Film.G_PULL: {
                    // a hard pull: both arms forward, then the lean back and the yank
                    float k2 = Math.min(1, u / Math.max(0.3f, act.t1 - act.t0));
                    float yank = k2 < 0.5f ? 0 : (float) Math.sin((k2 - 0.5f) * 2 * Math.PI);
                    p.armR = 70 + 20 * k2; p.armL = 65 + 20 * k2; p.elbowR = 20 + 40 * yank; p.elbowL = 20 + 40 * yank;
                    mo.rot -= (6 + 8 * yank) * p.facing;
                    mo.dx -= (10 + 18 * yank) * p.facing;
                    p.emotion = Pose.DETERMINED;
                    break;
                }
                case Film.G_SHAKE_HEAD:
                    p.headTilt = (float) Math.sin(u * 9) * 10;
                    mo.rot += (float) Math.sin(u * 9) * 3;
                    break;
                case Film.G_CLUTCH:
                    p.armL = 40; p.armR = 40; p.elbowL = 110; p.elbowR = 110;
                    mo.rot += (float) Math.sin(u * 2) * 2;
                    break;
                case Film.G_WALK_PLACE:
                    p.walk = t * 9; p.walkAmt = 0.8f;
                    mo.dy -= bump(t * 9) * 5;
                    break;
                case Film.G_PLAY: case Film.G_BOUNCE:
                    mo.dy -= bump(u * 6) * 14;
                    mo.rot += (float) Math.sin(u * 3) * 4;
                    p.emotion = Pose.HAPPY;
                    p.armL = 60 + s * 30; p.armR = 60 - s * 30;
                    break;
                case Film.G_MIRROR:
                    p.armR = 115; p.elbowR = 0; p.holdR = Pose.I_MIRROR;
                    p.emotion = Pose.DETERMINED;
                    break;
                case Film.G_SPLASH: case Film.G_THROW:
                    p.armR = 100 + s * 30;
                    break;
                case Film.G_WIGGLE:
                    mo.rot += (float) Math.sin(u * 8) * 9;
                    mo.dx += (float) Math.sin(u * 8) * 6;
                    break;
                case Film.G_COUGH:
                    mo.rot += bump(u * 6) * 6 * p.facing;
                    mo.dy += bump(u * 6) * 4;
                    p.armR = 120; p.elbowR = 120;
                    break;
                case Film.G_LOOK_UP:
                    p.headTilt = -12;
                    mo.rot -= 3 * p.facing;
                    break;
                case Film.G_WHISPER:
                    mo.rot += 6 * p.facing;
                    p.armR = 130; p.elbowR = 120;
                    break;
                case Film.G_PROUD:
                    mo.sx *= 1.03f; mo.sy *= 1.02f;
                    mo.rot -= 3 * p.facing;
                    p.emotion = Pose.PROUD;
                    break;
                case Film.G_ROAR:
                    mo.sx *= 1 + 0.04f * bump(u * 3);
                    mo.sy *= 1 + 0.04f * bump(u * 3);
                    mo.dx += (float) Math.sin(t * 50) * 3;
                    p.armL = 110; p.armR = 110; p.fist = true;
                    p.emotion = Pose.ANGRY;
                    break;
                case Film.G_STEP_BACK:
                    p.emotion = Pose.SCARED;
                    mo.rot -= 4 * p.facing;
                    break;
                default:
            }
        }
    }

    // ================================================================== picture sprites

    private final Rig.Frame rf = new Rig.Frame();
    /** Screen pixels per stage unit for the character being drawn (previews: 1). */
    private float pxPerUnit = 1f;
    private final Rig.State rs = new Rig.State(), rsTmp = new Rig.State();
    private final Pose pose2 = new Pose();
    private final float[] hand = new float[2];

    /** 0 standing .. 1 seated: sitting down and getting up each take about 0.7 s. */
    static float sitAmount(Film.Actor a, float t) {
        Film.Key cur = null, prev = null;
        float runStart = 0;
        boolean sitting = false;
        for (Film.Key k : a.keys) {
            if (k.t > t) break;
            boolean s2 = k.body == Pose.SIT;
            if (cur == null || s2 != sitting) runStart = k.t;
            prev = cur;
            cur = k;
            sitting = s2;
        }
        if (cur == null) return 0;
        float u = Math.min(1, (t - runStart) / 0.7f);
        u = u * u * (3 - 2 * u);
        if (sitting) return runStart <= a.keys.get(0).t ? 1 : u;    // already seated when the scene starts
        boolean wasSitting = false;
        for (Film.Key k : a.keys) { if (k.t >= runStart) break; wasSitting = k.body == Pose.SIT; }
        return wasSitting ? 1 - u : 0;
    }

    /** The seat of the last sitting key up to t. */
    static int seatOf(Film.Actor a, float t) {
        int seat = -1;
        for (Film.Key k : a.keys) { if (k.t > t) break; if (k.body == Pose.SIT) seat = k.seat; }
        return seat;
    }

    /**
     * A seat (feet line at 0,0; h = the sitter's standing height). front = only the parts in front of a seated
     * person (cushion edge and armrests), drawn over them so the folded legs read as sitting.
     */
    private void drawSeat(Gfx g, int seat, float h, boolean front) {
        float top = -h * 0.3f, w = h * 0.62f;
        switch (seat) {
            case Film.SEAT_THRONE: {
                if (!front) {
                    // carved golden back with red velvet, jewels and finials
                    float bt = -h * 1.02f;
                    g.begin(); g.moveTo(-w * 0.52f, top); g.lineTo(-w * 0.52f, bt + h * 0.12f); g.quadTo(0, bt - h * 0.1f, w * 0.52f, bt + h * 0.12f); g.lineTo(w * 0.52f, top); g.close();
                    g.linear(-w * 0.5f, 0, w * 0.5f, 0, 0xFFB8860B, 0xFFF6D365); g.fillPath();
                    g.begin(); g.moveTo(-w * 0.4f, top); g.lineTo(-w * 0.4f, bt + h * 0.16f); g.quadTo(0, bt - h * 0.02f, w * 0.4f, bt + h * 0.16f); g.lineTo(w * 0.4f, top); g.close();
                    g.linear(0, bt, 0, top, 0xFF7F0000, 0xFFC62828); g.fillPath();
                    g.color(0x22FFFFFF); g.oval(-w * 0.15f, bt + h * 0.3f, w * 0.08f, h * 0.25f);
                    g.color(0xFF1B5E20); g.oval(0, bt + h * 0.02f, h * 0.03f, h * 0.03f);
                    g.color(0xFFFFE082); g.oval(-w * 0.52f, bt + h * 0.1f, h * 0.03f, h * 0.03f); g.oval(w * 0.52f, bt + h * 0.1f, h * 0.03f, h * 0.03f);
                    // legs
                    g.linear(0, top, 0, 0, 0xFFC9A227, 0xFF8B6914);
                    g.rect(-w * 0.5f, top, h * 0.06f, -top); g.rect(w * 0.5f - h * 0.06f, top, h * 0.06f, -top);
                    // seat cushion
                    g.linear(0, top - h * 0.04f, 0, top + h * 0.04f, 0xFFD32F2F, 0xFF8E0000);
                    g.roundRect(-w * 0.5f, top - h * 0.035f, w, h * 0.07f, h * 0.02f);
                }
                // armrests and the cushion's gold-trimmed front edge
                g.linear(0, top - h * 0.14f, 0, top + h * 0.06f, 0xFFF6D365, 0xFFB8860B);
                g.roundRect(-w * 0.6f, top - h * 0.14f, w * 0.14f, h * 0.2f, h * 0.025f);
                g.roundRect(w * 0.46f, top - h * 0.14f, w * 0.14f, h * 0.2f, h * 0.025f);
                g.color(0xFFFFE082); g.oval(-w * 0.53f, top - h * 0.15f, h * 0.025f, h * 0.025f); g.oval(w * 0.53f, top - h * 0.15f, h * 0.025f, h * 0.025f);
                g.linear(0, top, 0, top + h * 0.08f, 0xFFC62828, 0xFF7F0000);
                g.roundRect(-w * 0.46f, top + h * 0.0f, w * 0.92f, h * 0.075f, h * 0.02f);
                g.color(0xFFE0B040); g.rect(-w * 0.46f, top + h * 0.065f, w * 0.92f, h * 0.012f);
                break;
            }
            case Film.SEAT_STOOL:
                if (!front) {
                    g.color(0xFF6D4C41);
                    g.rect(-w * 0.36f, top, h * 0.035f, -top); g.rect(w * 0.36f - h * 0.035f, top, h * 0.035f, -top);
                }
                g.linear(0, top, 0, top + h * 0.06f, 0xFFA1887F, 0xFF6D4C41);
                g.roundRect(-w * 0.42f, top, w * 0.84f, h * 0.055f, h * 0.02f);
                break;
            case Film.SEAT_ROCK:
                if (!front) { g.color(0xFF7E7E74); g.oval(0, top * 0.45f, w * 0.55f, -top * 0.6f); }
                g.color(0xFF9A9A8E); g.oval(0, top * 0.25f, w * 0.5f, -top * 0.3f);
                break;
            default:
        }
    }

    /** Draws one character in a given pose at (0,0) = feet (previews and checks). */
    public void drawPosed(Gfx g, Story.CharacterDef c, Pose p, float h) {
        Art.Sprite sp = art == null ? null : art.sprites.get(c.id);
        mo.dx = 0; mo.dy = 0; mo.rot = 0; mo.sx = 1; mo.sy = 1;
        pxPerUnit = 1f;
        if (sp != null) drawSprite(g, sp, c.look, p, h, 0, null);
        else Puppet.draw(g, c.look, p, h);
    }

    /** Arm angle of the cartoon pose (0 down, 90 sideways, 170 up) as an outward swing a picture can show. */
    static float armSwing(float a) {
        return Math.max(-6, Math.min(26, (a - 8) * 0.3f));
    }

    /** The face for an emotion. */
    static void faceFor(int emotion, Rig.State f, float w) {
        switch (emotion) {
            case Pose.HAPPY: f.smile += 0.7f * w; f.squint += 0.25f * w; break;
            case Pose.LAUGH: f.smile += 1f * w; f.squint += 0.8f * w; break;
            case Pose.SAD: f.frown += 0.8f * w; f.innerUp += 0.9f * w; break;
            case Pose.ANGRY: f.anger += 1f * w; f.frown += 0.2f * w; break;
            case Pose.SCARED: f.innerUp += 0.8f * w; f.wide += 0.9f * w; f.frown += 0.3f * w; break;
            case Pose.SURPRISED: f.browUp += 1f * w; f.wide += 1f * w; break;
            case Pose.EVIL: f.smile += 0.6f * w; f.anger += 0.55f * w; break;
            case Pose.DETERMINED: f.anger += 0.45f * w; break;
            case Pose.PROUD: f.smile += 0.4f * w; f.browUp += 0.2f * w; break;
            case Pose.CURIOUS: f.browUpR += 0.8f * w; f.wide += 0.2f * w; break;
            case Pose.PAIN: f.frown += 0.6f * w; f.squint += 0.9f * w; f.anger += 0.3f * w; break;
            case Pose.DIZZY: f.squint += 0.4f * w; break;
            // the director's manual (2.6): suspicion = asymmetrical brows, narrowed gaze; relief = tension gone, softened gaze
            case Pose.SUSPICIOUS: f.anger += 0.35f * w; f.squint += 0.6f * w; f.browUpR += 0.5f * w; break;
            case Pose.RELIEVED: f.smile += 0.45f * w; f.squint += 0.15f * w; f.innerUp += 0.2f * w; break;
            default: f.smile += 0.1f * w;
        }
    }

    private final Rig.State rsLag = new Rig.State();

    /** The body language of one feeling, weighted (for blending over time). */
    static void bodyFor(int emotion, Rig.State st, float w, float t) {
        switch (emotion) {
            case Pose.SAD: st.nod += 0.9f * w; st.armL -= 2 * w; st.armR -= 2 * w; st.lean += 1.5f * w; break;
            case Pose.ANGRY: st.lean += 3 * w; st.nod += 0.3f * w; st.armL += 4 * w; st.armR += 4 * w; break;
            case Pose.SCARED: st.lean -= 4 * w; st.armL -= 3 * w; st.armR -= 3 * w; st.headRot += (float) Math.sin(t * 17) * 0.12f * w; break;
            case Pose.SURPRISED: st.nod -= 0.6f * w; st.armL += 6 * w; st.armR += 6 * w; st.lean -= 2 * w; break;
            case Pose.HAPPY: st.headRot += 3 * w; break;
            case Pose.LAUGH: st.nod -= 0.7f * w; st.headRot += (float) Math.sin(t * 3.5f) * 1.8f * w; st.armL += (float) Math.sin(t * 7) * 2 * w; st.armR += (float) Math.sin(t * 7) * 2 * w; break;
            case Pose.PROUD: st.nod -= 0.5f * w; st.lean -= 2 * w; st.breathe += w; break;
            case Pose.DETERMINED: st.nod += 0.2f * w; st.lean += 1.5f * w; break;
            case Pose.CURIOUS: st.headRot += 6 * w; break;
            case Pose.PAIN: st.nod += 0.5f * w; st.lean += 3 * w; break;
            case Pose.DIZZY: st.headRot += (float) Math.sin(t * 3) * 8 * w; break;
            case Pose.EVIL: st.nod += 0.3f * w; st.headRot -= 3 * w; break;
            case Pose.SUSPICIOUS: st.headRot += 5 * w; st.nod += 0.2f * w; st.lean -= 1 * w; break;                 // the head angled, held back
            case Pose.RELIEVED: st.nod += 0.3f * w; st.lean -= 1.5f * w; st.breathe += 1.2f * w; st.armL -= 2 * w; st.armR -= 2 * w; break;   // the posture released, a breath out
            default:
        }
    }

    /** The emotion an actor shows at time t (its key, its line, its gesture), without drawing anything. */
    private int emotionAt(Film.Actor a, float t) {
        Film.Key k = a.stateAt(t);
        Pose q = pose2;
        q.reset();
        q.emotion = k.emotion;
        Film.Speak spk = speakingAt(a, t);
        if (spk != null && spk.emotion != Pose.NEUTRAL) q.emotion = spk.emotion;
        float dx = mo.dx, dy = mo.dy, r = mo.rot, sx = mo.sx, sy = mo.sy;
        applyActs(a, q, t);
        mo.dx = dx; mo.dy = dy; mo.rot = r; mo.sx = sx; mo.sy = sy;
        return q.emotion;
    }

    /** Bones and face of a picture character for this frame, from the same pose the cartoon puppets use. */
    private Rig.State rigState(Pose p, Film.Actor a) {
        Rig.State st = rs;
        st.reset();
        float t = p.time;
        st.time = t;
        st.wind = p.wind * (p.facing < 0 ? -1 : 1);     // the picture is mirrored when facing left
        // every character moves in its own way (§26): lively ones bigger and quicker, calm ones smaller
        float en = a != null ? a.look.energy : 1f, poise = a != null ? a.look.poise : 0f;
        st.armL = armSwing(8 + (p.armL - 8) * en);
        st.armR = armSwing(8 + (p.armR - 8) * en);
        st.headRot = p.headTilt * 0.8f + (float) Math.sin(t * 0.7f * en + p.seed) * 0.6f * en;
        st.lean = p.tilt;
        st.breathe = (float) Math.sin(t * 2.1f + p.seed);
        if (p.mouth > 0.02f) {
            // a speaker's head stays almost still: a slow, small accent of the phrase, never a shake with every syllable
            st.headRot += (float) Math.sin(t * 1.3f + p.seed) * 0.45f * en;
            st.nod += (float) Math.sin(t * 1.7f + p.seed) * 0.08f;
        }
        if (p.walkAmt > 0) {
            float sw = (float) Math.sin(p.walk);
            st.legLLift = Math.max(0, sw) * 0.14f * p.walkAmt;
            st.legRLift = Math.max(0, -sw) * 0.14f * p.walkAmt;
            st.legLAng = sw * 4 * p.walkAmt;
            st.legRAng = sw * 4 * p.walkAmt;
            st.headRot += sw * 1.2f;
        }
        st.nod += p.nod - 0.25f * poise;
        st.lean -= 1.2f * poise;
        st.walkAmt = p.walkAmt;
        // the lips: the jaw opens with the voice, the corners follow the sound (wide "ee", round "oo")
        float open = p.mouth;
        if (open < 0.06f && p.emotion == Pose.SURPRISED) open = 0.3f;
        if (open < 0.06f && p.emotion == Pose.LAUGH) open = 0.32f + 0.12f * (float) Math.sin(t * 9);
        st.jaw = open > 0.04f ? open : 0;
        st.lipWide = Math.max(0, Math.min(1, (p.mouthWide - 0.5f) * 2.2f));
        st.lipRound = Math.max(0, Math.min(1, (0.5f - p.mouthWide) * 2.2f));
        if (p.wave > 0) st.armR = 20 + 7 * (float) Math.sin(t * 12);
        st.twirl = p.twirl;
        st.sit = p.sit;
        if (p.sit > 0) st.lean -= 2 * p.sit;
        switch (p.body) {
            case Pose.SIT: break;
            case Pose.KNEEL: st.legScale = 0.7f; st.lean += 4; break;
            case Pose.CROUCH: st.legScale = 0.75f; st.lean += 9; st.nod += 0.4f; break;
            default:
        }
        // body language of the feeling, blended over the last half second (a body has weight: it settles into a
        // new feeling instead of snapping), and hair and cloth lag a quarter second behind the head (follow-through)
        if (a != null) {
            int n = 8;
            float r0 = st.headRot, l0 = st.lean;
            for (int i = 0; i < n; i++) bodyFor(emotionAt(a, t - i * 0.06f), st, 1f / n, t);
            float rNow = st.headRot - r0, lNow = st.lean - l0;
            Rig.State lag = rsLag;
            lag.reset();
            for (int i = 0; i < n; i++) bodyFor(emotionAt(a, t - 0.25f - i * 0.06f), lag, 1f / n, t - 0.25f);
            st.follow = rNow - lag.headRot;
            st.followLean = lNow - lag.lean;
        } else {
            bodyFor(p.emotion, st, 1f, t);
        }
        // the face: an average over the last 0.3 s, so expressions melt into each other
        // (a function of time only, so frames drawn by different threads agree)
        if (a != null) {
            int samples = 6;
            for (int i = 0; i < samples; i++) faceFor(emotionAt(a, t - i * 0.06f), st, 1f / samples);
        } else {
            faceFor(p.emotion, st, 1f);
        }
        return st;
    }

    /**
     * An animal's tail, jaw, ears and legs from its feeling and voice: a happy dog wags fast, a sad one lets
     * its tail hang, an angry or frightened one lays its ears back; the jaw opens with the words.
     */
    static void animalState(Rig.State st, Pose p) {
        float t = p.time, seed = p.seed;
        float amp = 7, freq = 2.2f, droop = 0, ears = 0;
        switch (p.emotion) {
            case Pose.HAPPY: case Pose.LAUGH: amp = 24; freq = 11; break;
            case Pose.PROUD: amp = 10; freq = 3; droop = -12; break;
            case Pose.CURIOUS: amp = 12; freq = 4; droop = -8; break;
            case Pose.SURPRISED: amp = 4; freq = 6; droop = -14; break;
            case Pose.SAD: amp = 2; freq = 1.2f; droop = 26; ears = 0.5f; break;
            case Pose.SCARED: amp = 2; freq = 16; droop = 36; ears = 1; break;
            case Pose.ANGRY: case Pose.EVIL: amp = 4; freq = 14; droop = -16; ears = 1; break;
            case Pose.PAIN: amp = 2; freq = 1; droop = 20; ears = 0.7f; break;
            case Pose.DETERMINED: amp = 4; freq = 3; droop = -6; ears = 0.3f; break;
            case Pose.SUSPICIOUS: amp = 3; freq = 5; droop = -4; ears = 0.6f; break;
            case Pose.RELIEVED: amp = 14; freq = 6; droop = 4; break;
            default:
        }
        if (p.walkAmt > 0) amp = Math.max(amp, 9);
        st.tail = droop + amp * (float) Math.sin(t * freq + seed);     // + hangs down, - held up
        st.jaw = Math.min(1, p.mouth * 1.3f + (p.emotion == Pose.ANGRY && p.mouth > 0.05f ? 0.25f : 0));
        if (p.emotion == Pose.LAUGH) st.jaw = Math.max(st.jaw, 0.35f + 0.2f * (float) Math.sin(t * 9));
        // an ear flick now and then, more when listening
        float flick = (float) Math.sin(t * 0.83f + seed * 3.1f);
        st.ear = flick > 0.9f ? (float) Math.sin(t * 34) : 0;
        if (p.emotion == Pose.CURIOUS || p.emotion == Pose.SURPRISED) st.ear = -0.8f + 0.2f * (float) Math.sin(t * 9);
        st.earBack = ears;
        st.walkPhase = p.walk;
        st.walkAmt = p.walkAmt;
        // an animal lies or sits by folding its legs under it
        st.sit = p.body == Pose.LIE ? 1 : p.body == Pose.SIT || p.body == Pose.KNEEL ? 0.6f : Math.min(1, p.sit);
        if (p.body == Pose.LIE) { st.walkAmt = 0; st.nod += 0.4f; }
        // breathing shows more on a resting animal; panting when happy
        if (p.emotion == Pose.HAPPY && p.mouth < 0.05f) st.breathe = (float) Math.sin(t * 9 + seed);
    }

    /**
     * The picture a character is drawn with in this moment: the front picture (the identity) — or, when the
     * views made from it exist, the side view while walking (facing the way of the walk), the three-quarter
     * view in a two-shot while turned to the other character. Never while speaking (lip-sync needs the face),
     * never lying, sitting or carried.
     */
    private Art.Sprite viewOf(Art.Sprite sp, Film.Actor a, Film.Seg s, Pose p, Film.Key k, boolean speaking, float t) {
        if (sp.views == null || speaking || k.body != Pose.STAND || k.anchor != Film.A_GROUND || p.sit > 0) return sp;
        if (p.walkAmt > 0.35f && sp.view(1) != null) return sp.view(1);
        Film.Shot sh = shotAt(t);
        if (sh != null && sh.type == ShotPlanner.TWO_SHOT && sp.view(0) != null && p.walkAmt <= 0.35f) {
            // turned toward the other character of the two-shot: the three-quarter view
            for (Film.Actor o : s.actors) {
                if (o == a || !o.stateAt(t).visible || o.stateAt(t).anchor != Film.A_GROUND) continue;
                float dx = Director.xAt(o, t) - Director.xAt(a, t);
                if (Math.abs(dx) < 40 || Math.abs(dx) > 900) continue;
                if (Math.signum(dx) == Math.signum(k.facing)) return sp.view(0);
            }
        }
        return sp;
    }

    private int lastShot;

    /** The shot playing at t (the shots are in order; the last one found is tried first). */
    Film.Shot shotAt(float t) {
        if (film == null || film.shots.isEmpty()) return null;
        int n = film.shots.size();
        int i = Math.max(0, Math.min(n - 1, lastShot));
        Film.Shot sh = film.shots.get(i);
        if (!(t >= sh.t && t < sh.t + sh.dur)) {
            for (i = 0; i < n; i++) { sh = film.shots.get(i); if (t >= sh.t && t < sh.t + sh.dur) break; }
            if (i >= n) return null;
            lastShot = i;
        }
        return sh;
    }

    /**
     * The over-the-shoulder reverse: the speaker's shoulder and the back of their head in the foreground at the
     * edge of the frame, on the side the listener faces, large and soft (out of the depth of field), so the
     * reaction is seen from the speaker's place in the conversation (the scene maker guide, ch. 7).
     */
    private void overShoulder(Gfx g, Film.Seg s, float t) {
        Film.Shot sh = shotAt(t);
        if (sh == null || sh.ots.length() == 0) return;
        Film.Actor fg = null, to = null;
        for (Film.Actor a : s.actors) { if (a.c.shown().equals(sh.ots)) fg = a; if (a.c.shown().equals(sh.subject)) to = a; }
        if (fg == null) return;
        Art.Sprite sp = art.sprites.get(fg.c.id);
        Art.Sprite back = sp == null ? null : sp.view(2);
        if (back == null) return;
        float side = to != null ? to.stateAt(t).facing : -fg.stateAt(t).facing;
        final float h = vh * 1.45f, w = h * back.w / Math.max(1, back.h);
        final float cx = side > 0 ? vw - w * 0.34f : w * 0.34f, bottom = vh * 1.12f;
        final Object img = back.img;
        final float fw = w;
        g.layerLow("ots:" + fg.c.id + ":" + (side > 0 ? "R" : "L"), vw, vh, 0.35f, new Gfx.Painter() {
            public void paint(Gfx gg) {
                gg.image(img, cx - fw / 2, bottom - h, fw, h);
            }
        });
    }

    private void drawSprite(Gfx g, Art.Sprite sp, Look look, Pose p, float h, float rot, Film.Actor actor) {
        float scale = h / sp.h;
        float w = sp.w * scale;
        Rig rig = sp.rig;
        boolean beast = rig != null && rig.animal;
        boolean bare = p.noHeadwear && sp.bareImg != null;          // the turban / cap has been taken off
        // every picture moves through its mesh, also lying down or hanging upside down (the whole picture is
        // turned; the body still breathes, the face still speaks and changes expression)
        boolean rigged = rig != null && (!beast || p.body != Pose.HANG) && !(p.noHeadwear && sp.turbanY > 0 && !bare);
        Rig.State st = rigged ? rigState(p, actor) : null;
        if (rigged && beast) animalState(st, p);
        g.save();
        if (p.body == Pose.LIE && !beast) {
            g.translate(0, -w * 0.32f);
            g.rotate(p.facing > 0 ? -86 : 86);
            g.translate(0, h * 0.5f);
        } else if (p.body == Pose.HANG) {
            g.rotate(180);
        }
        if (rot != 0) g.rotate(rot);
        float sy = mo.sy, sx = mo.sx;
        if (!rigged && p.body == Pose.SIT) sy *= 0.8f;
        else if (!rigged && p.body == Pose.KNEEL) sy *= 0.74f;
        if (p.walkAmt > 0) sy *= 1 + (float) Math.sin(p.walk * 2) * 0.012f * p.walkAmt;
        float mirror = p.facing < 0 ? -1 : 1;
        g.scale(sx * mirror, sy);
        // shadow
        if (p.body != Pose.LIE && p.body != Pose.HANG) {
            // a soft contact shadow (wide and faint, dark only right under the feet), so the character stands on
            // the floor of the picture instead of floating over it
            for (int i = 0; i < 5; i++) {
                float k = 1 - i * 0.17f;
                g.color(i < 4 ? 0x12000000 : 0x2A000000);
                g.oval(0, -h * 0.002f, w * 0.5f * k, h * 0.032f * k);
            }
        }
        float left = -w / 2, top = -h;
        if (p.noHeadwear && sp.turbanY > 0 && !bare) {
            float cut = sp.turbanY;
            float fx = left + sp.mouthX * w;
            float fw = Math.max(sp.eyeRX - sp.eyeLX, 0.1f) * w * 1.3f;
            float fy = top + cut * h;
            // bald head first, the face picture is drawn over its lower half
            g.color(Puppet.shade(sp.skin, 0.92f));
            g.oval(fx, fy + fw * 0.15f, fw * 1.02f, fw * 0.72f);
            g.color(sp.skin);
            g.oval(fx, fy + fw * 0.12f, fw * 0.98f, fw * 0.68f);
            g.color(Puppet.alpha(0xFFFFFFFF, 0.35f));
            g.oval(fx - fw * 0.3f, fy - fw * 0.3f, fw * 0.3f, fw * 0.1f);
            // the face column from the cut line down; the sides only from the shoulders down — a turban's tail, a
            // feather or a cap's brim beside the bare head would otherwise still show (v22)
            float eyeYf = (sp.eyeLY + sp.eyeRY) / 2f;
            float neck = Math.max(cut, Math.min(1f, sp.mouthY + Math.max(0.02f, sp.mouthY - eyeYf) * 0.9f));
            float colL = Math.max(left, fx - fw * 0.75f), colR = Math.min(left + w, fx + fw * 0.75f);
            g.imageRect(sp.img, (colL - left) / w * sp.w, cut * sp.h, (colR - colL) / w * sp.w, sp.h * (neck - cut), colL, top + cut * h, colR - colL, h * (neck - cut));
            g.imageRect(sp.img, 0, neck * sp.h, sp.w, sp.h * (1 - neck), left, top + neck * h, w, h * (1 - neck));
        } else if (rigged) {
            // sitting / kneeling: the legs fold, so the picture comes down to keep the feet on the ground
            float rise = rig.feetRise(st, h);
            if (rise > 0) g.translate(0, rise);
            rig.bodyMesh(rf, st, left, top, w, h, h * pxPerUnit);
            g.imageMesh(bare ? sp.bareImg : sp.img, rf.cols, rf.rows, rf.body);
            if (p.wet > 0.02f && sp.wetImg != null && !bare) { g.save(); g.setAlpha(Math.min(0.85f, p.wet)); g.imageMesh(sp.wetImg, rf.cols, rf.rows, rf.body); g.restore(); }
            Object faceLayer = bare ? rig.faceBareImg : rig.faceImg;
            if (rig.face && faceLayer != null) {
                rig.faceMesh(rf, st);
                g.imageMesh(faceLayer, rf.fcols, rf.frows, rf.face);
                if (p.wet > 0.02f && rig.faceWetImg != null && !bare) { g.save(); g.setAlpha(Math.min(0.85f, p.wet)); g.imageMesh(rig.faceWetImg, rf.fcols, rf.frows, rf.face); g.restore(); }
            }
            // rim light: the outline catches the place's key light (warm, from the sun's side; softer indoors and
            // at night), so the character belongs to the picture instead of floating on it
            Film.Seg ls = curSeg;
            if (sp.rimL != null && sp.rimR != null && !bare && ls != null) {
                boolean fromLeft = !(ls.tod == Sets.EVENING || ls.tod == Sets.NIGHT);
                float k = outdoor(ls.set) ? (ls.tod == Sets.NIGHT ? 0.22f : ls.tod == Sets.EVENING || ls.tod == Sets.MORNING ? 0.45f : 0.32f) : 0.24f;
                k /= 1 + 0.3f * Math.max(0, camZ - 1);     // closer in, the edge is larger on screen: keep it subtle
                Object rim = fromLeft == (p.facing >= 0) ? sp.rimL : sp.rimR;
                g.save();
                g.setAlpha(k);
                g.imageMesh(rim, rf.cols, rf.rows, rf.body);
                g.restore();
            }
            // the eyes, mouth and tears below are drawn in the head's own position
            Rig.applyHead(rf, g);
        } else {
            g.image(sp.img, left, top, w, h);
            if (p.wet > 0.02f && sp.wetImg != null) { g.save(); g.setAlpha(Math.min(0.85f, p.wet)); g.image(sp.wetImg, left, top, w, h); g.restore(); }
        }
        if (rigged && rig.animal && st.jaw > 0.12f) {
            // the open jaw: a dark mouth between the upper and the lowered jaw, a tongue inside
            float hx = left + rig.jawX * w, hy = top + rig.jawY * h, tx = left + rig.jawTipX * w;
            double ja = Math.toRadians(rig.headSide * 16 * st.jaw);
            float lx = hx + (float) Math.cos(ja) * (tx - hx), ly = hy + (float) Math.sin(ja) * (tx - hx);
            g.color(0xF0300C0C);
            g.begin(); g.moveTo(hx, hy); g.lineTo(tx, hy); g.lineTo(lx, ly); g.close(); g.fillPath();
            if (st.jaw > 0.3f) {
                g.color(0xE0D9636B);
                g.oval(hx + (lx - hx) * 0.62f, hy + (ly - hy) * 0.55f, Math.abs(tx - hx) * 0.2f, Math.abs(ly - hy) * 0.16f);
            }
            g.color(0xF0F5F2EA);    // a tooth or two at the front
            g.oval(tx - rig.headSide * Math.abs(tx - hx) * 0.1f, hy + Math.abs(ly - hy) * 0.08f, Math.abs(tx - hx) * 0.05f, Math.abs(ly - hy) * 0.09f);
        }
        if (sp.faceKnown && !(rigged && rig.animal && !rig.eyesOnHead)) {
            float ex1 = left + sp.eyeLX * w, ey1 = top + sp.eyeLY * h;
            float ex2 = left + sp.eyeRX * w, ey2 = top + sp.eyeRY * h;
            float er = sp.eyeR * w;
            // blinking / closed eyes
            if (p.blink > 0.5f || p.eyesClosed) {
                for (int i = 0; i < 2; i++) {
                    float ex = i == 0 ? ex1 : ex2, ey = i == 0 ? ey1 : ey2;
                    g.color(sp.lid);
                    g.oval(ex, ey, er * 1.45f, er * 1.25f);
                    g.color(0xFF2A1A12);
                    g.begin();
                    g.moveTo(ex - er * 1.3f, ey + er * 0.1f);
                    g.quadTo(ex, ey + er * 0.75f, ex + er * 1.3f, ey + er * 0.1f);
                    g.strokePath(Math.max(1.5f, er * 0.18f));
                }
            }
            if (p.redFace) {
                g.color(0x55FF2A2A);
                g.oval((ex1 + ex2) / 2, (ey1 + top + sp.mouthY * h) / 2, Math.abs(ex2 - ex1) * 1.1f, Math.abs(top + sp.mouthY * h - ey1) * 1.1f + er);
            }
            if (p.tears) {
                g.color(0xCC64B5F6);
                float fall = (p.time * 50) % (h * 0.08f);
                g.oval(ex1, ey1 + er * 1.4f + fall, er * 0.22f, er * 0.32f);
                g.oval(ex2, ey2 + er * 1.4f + (fall + h * 0.03f) % (h * 0.08f), er * 0.22f, er * 0.32f);
            }
            if (p.sweat) {
                // a drop of sweat at the temple sliding down: a teardrop (round below, pointed above) with a glint
                float dx = ex2 + er * 2.2f, dy = ey2 - er * 1.6f + (p.time * 15) % (er * 2), dr = er * 0.22f;
                g.color(0xB081D4FA);
                g.oval(dx, dy + dr * 0.4f, dr, dr * 1.1f);
                g.oval(dx, dy - dr * 0.35f, dr * 0.45f, dr * 0.9f);
                g.color(0x90FFFFFF);
                g.oval(dx - dr * 0.35f, dy + dr * 0.1f, dr * 0.22f, dr * 0.3f);
            }
            // lip-sync mouth (open in surprise or laughter even when not speaking)
            float m = p.mouth;
            if (rigged && m < 0.06f && p.emotion == Pose.SURPRISED) m = 0.28f;
            if (rigged && m < 0.06f && p.emotion == Pose.LAUGH) m = 0.32f + 0.12f * (float) Math.sin(p.time * 9);
            if (m > 0.06f && rigged && !rig.animal && rig.face && rig.faceImg != null && st.jaw > 0.02f && sp.faceKnown) {
                // the fine face mesh has parted the real lips: only the inside of the mouth shows between them
                float mx = left + sp.mouthX * w, my = top + sp.mouthY * h, hw = sp.mouthHW * w;
                float gap = Rig.jawDrop(hw) * st.jaw;
                // only the dark inside between the parted real lips, and only once they are clearly apart: never a
                // painted mouth shape of its own (it reads as a second pair of lips), no teeth bar
                if (gap > hw * 0.10f) {
                    float ow = hw * (0.62f + 0.18f * st.lipWide - 0.28f * st.lipRound), cy = my + gap * 0.5f;
                    float oh = Math.max(0.5f, gap * 0.40f);
                    g.color(0xB0200608);
                    g.oval(mx, cy, ow, oh);
                }
            }
            if (p.wearsTurban && !drawRealHat(g, sp, p, left, top, w, h)) {
                float tx = left + sp.mouthX * w;
                drawTurbanAt(g, tx, top + h * 0.035f, w * 0.3f, p);
            }
        } else if (p.wearsTurban) {
            drawTurbanAt(g, 0, top + h * 0.04f, w * 0.3f, p);
        }
        g.restore();
        // held items (not mirrored text-wise, drawn upright near the hands)
        if (p.holdR != Pose.I_NONE && p.body != Pose.LIE) {
            float hx = p.facing * w * 0.36f, hy = -h * 0.45f;
            if (rigged) {
                rig.handAt(rf, st, 1, hand);
                hx = hand[0] * (p.facing < 0 ? -1 : 1) * mo.sx;
                hy = hand[1] * mo.sy + rig.feetRise(st, h);
            }
            if (p.holdR == Pose.I_WOOD_SWORD || p.holdR == Pose.I_SWORD) {
                g.save();
                g.translate(hx, hy);
                // swinging in the fight; otherwise held low and still at the side, the blade pointing down and forward
                g.rotate(p.swing ? p.facing * (40 + (float) Math.sin(p.time * 7) * 25) : p.facing * (150 + (float) Math.sin(p.time * 1.3f) * 2));
                g.color(0xFF5D4037); g.line(0, 0, 0, 16, 7);
                g.color(0xFFE5B530); g.line(-10, 0, 10, 0, 5);
                g.color(p.holdR == Pose.I_SWORD ? 0xFFE0E0E0 : 0xFFC08A55);
                g.line(0, 0, 0, -h * 0.32f, 8);
                g.restore();
            } else {
                drawItem(g, p.holdR, hx, hy, h, p);
            }
        }
    }

    /**
     * The real turban / cap of its owner (cut from the owner's own picture) on this character's head, sized by
     * the two faces' eye distances and set just as high above the eyes as it sat on its owner.
     */
    private boolean drawRealHat(Gfx g, Art.Sprite sp, Pose p, float left, float top, float w, float h) {
        if (p.turbanOwner == null || art == null || !sp.faceKnown) return false;
        Art.Sprite o = art.sprites.get(p.turbanOwner);
        if (o == null || o.hatImg == null || o.hatW < 2) return false;
        float d = Math.abs(sp.eyeRX - sp.eyeLX) * w;
        if (d < 2) return false;
        // a head a little wider than its eye distance suggests (animals' eyes sit close) gets a bigger hat
        float k = d * 1.08f;
        float ex = left + (sp.eyeLX + sp.eyeRX) / 2 * w, ey = top + (sp.eyeLY + sp.eyeRY) / 2 * h;
        float x0 = ex + o.hatX0 * k, x1 = ex + o.hatX1 * k, y0 = ey + o.hatY0 * k, y1 = ey + o.hatY1 * k;
        g.imageRect(o.hatImg, 0, 0, o.hatW, o.hatH, x0, y0, x1 - x0, y1 - y0);
        return true;
    }

    private void drawTurbanAt(Gfx g, float x, float y, float r, Pose p) {
        Puppet.drawTurbanShape(g, x, y, r, p.turbanColor, p.turbanBand, false);
    }

    private void drawItem(Gfx g, int item, float x, float y, float h, Pose p) {
        float s = h / 400f;
        g.save();
        g.translate(x, y);
        g.scale(s * 1.6f, s * 1.6f);
        switch (item) {
            case Pose.I_RIBBON:
                g.color(0xFFC62828);
                g.begin(); g.moveTo(0, -6); g.quadTo(14, 10 + (float) Math.sin(p.time * 4) * 4, 4, 30); g.strokePath(6);
                g.begin(); g.moveTo(0, -6); g.quadTo(-12, 8, -6, 26); g.strokePath(6);
                break;
            case Pose.I_BANANA:
                g.color(0xFFFDD835);
                g.begin(); g.moveTo(-6, -18); g.quadTo(16, -4, -2, 16); g.quadTo(5, -2, -6, -18); g.close(); g.fillPath();
                g.color(0xFF6D4C41); g.oval(-6, -18, 2.5f, 2.5f);
                break;
            case Pose.I_MIRROR:
                g.color(0xFF8D6E63); g.line(0, 6, 0, 22, 5);
                g.color(0xFFE5B530); g.oval(0, -8, 16, 16);
                g.color(0xFFCFE8F7); g.oval(0, -8, 12, 12);
                g.color(0xFFFFFFFF); g.oval(-4, -12, 4, 4);
                break;
            case Pose.I_BASKET:
                g.color(0xFFA1887F); g.roundRect(-24, -6, 48, 22, 8);
                g.color(0xFF6D4C41); g.strokeOval(0, -6, 22, 14, 3);
                int[] cols = {0xFFFFB300, 0xFFE91E63, 0xFF7CB342, 0xFFFF7043};
                for (int i = 0; i < 4; i++) { g.color(cols[i]); g.oval(-15 + i * 10, -8, 7, 7); }
                break;
            case Pose.I_FLOWER:
                g.color(0xFF6D4C41); g.line(0, 0, 0, 14, 3);
                g.color(0xFF8D6E63); g.oval(0, -4, 7, 7);
                break;
            case Pose.I_BOTTLE:
                g.color(0xFF6D4C41); g.roundRect(-7, -16, 14, 24, 5);
                g.color(0xFF90CAF9); g.rect(-4, -20, 8, 5);
                break;
            default:
        }
        g.restore();
    }

    // ================================================================== effects

    private float actorScreenX(Film.Actor a, float t) { return a == null ? 640 : Director.xAt(a, t); }

    private void drawFxLayer(Gfx g, Film.Seg s, float t, boolean behind) {
        for (Film.Fx f : s.fx) {
            if (t < f.t0 || t >= f.t1) continue;
            float u = t - f.t0;
            switch (f.type) {
                case Film.FX_MAGIC_FLOWER: if (behind) magicFlower(g, f, t); break;
                case Film.FX_BLOOM: if (behind) bloom(g, s, f, t); break;
                case Film.FX_RIBBON: if (behind) ribbon(g, f.x, f.y, f.color); break;
                case Film.FX_BELL: if (behind) bell(g, f, t); break;
                case Film.FX_BUTTERFLY: if (!behind) butterfly(g, s, f, t); break;
                case Film.FX_SMOKE: if (!behind) smoke(g, s, f, t); break;
                case Film.FX_BEAM: if (!behind) beam(g, s, f, t); break;
                case Film.FX_NET: if (!behind) net(g, s, f, t); break;
                case Film.FX_CAGE: if (!behind) cage(g, s, f, t); break;
                case Film.FX_SPARKLE: if (!behind) {
                    float x = f.a != null ? actorScreenX(f.a, t) + f.a.stateAt(t).facing * 60 : f.x;
                    sparkles(g, t, x - 120, f.y - 120, 240, 240, 14, f.color == 0 ? 0xFFFFF59D : f.color);
                } break;
                case Film.FX_SPLASH: if (!behind) splash(g, f, u); break;
                case Film.FX_SEAT: if (behind) {
                    float hh = f.a != null ? actorHeight(f.a.look, f.a.c, art, s) : 400;
                    g.save(); g.translate(f.x, s.ground); drawSeat(g, f.kind, hh, false); drawSeat(g, f.kind, hh, true); g.restore();
                } break;
                case Film.FX_STONE: if (!behind) {
                    // the stone's arc from the hand, then the splash and the rings of waves (or a puff of dust)
                    float flight = f.t2 - f.t0;
                    if (u < flight) {
                        float hx = f.a != null ? Director.xAt(f.a, f.t0) + f.a.stateAt(f.t0).facing * 40 : f.x - 300;
                        float hy = s.ground - (f.a != null ? actorHeight(f.a.look, f.a.c, art, s) * 0.6f : 200);
                        Nature.stone(g, hx, hy, f.x, f.y, u, flight);
                    } else if (f.color == 1) Nature.splashRipples(g, f.x, f.y, u - flight, 1);
                    else if (u - flight < 0.8f) { g.color(Puppet.alpha(0xFFBCAAA4, 0.6f * (1 - (u - flight) / 0.8f))); g.oval(f.x, f.y - 6, 18 + (u - flight) * 30, 8); }
                } break;
                case Film.FX_WATER_HIT: if (!behind) Nature.splashRipples(g, f.x, f.y, u, Math.max(1, f.color)); break;
                case Film.FX_FALL: if (!behind) Nature.flyingObject(g, f.x, s.ground - 430, f.a != null ? 25 : -15, 0, s.ground + 8, u, f.kind, 1.4f); break;
                case Film.FX_THROW: if (!behind) {
                    float hx = f.a != null ? Director.xAt(f.a, f.t0) + f.a.stateAt(f.t0).facing * 40 : 300;
                    float hy = s.ground - (f.a != null ? actorHeight(f.a.look, f.a.c, art, s) * 0.6f : 200);
                    float tx = f.b != null ? Director.xAt(f.b, f.t0 + 0.9f) : f.x, ty = f.b != null ? s.ground - actorHeight(f.b.look, f.b.c, art, s) * 0.55f : f.y;
                    float T = 0.9f, vx = (tx - hx) / T, vy = (ty - hy - 0.5f * 1500 * T * T) / T;
                    if (f.b != null && u > T) break;          // caught
                    Nature.flyingObject(g, hx, hy, vx, vy, s.ground + 8, u, f.kind, 1.3f);
                } break;
                case Film.FX_LEAVES: if (!behind) leaves(g, f, u); break;
                case Film.FX_GLOW_AREA: if (behind) {
                    // lamps or flowers lighting up one after another across the stage, then pulsing softly
                    int col = f.color == 0 ? 0xFFFFF176 : f.color;
                    for (int i = 0; i < 7; i++) {
                        float on = Rig.smooth(i * 0.12f, i * 0.12f + 0.3f, u);
                        if (on <= 0) continue;
                        float pulse = 0.75f + 0.25f * (float) Math.sin(t * 2.5 + i), x = 100 + i * 180, y = s.ground - 40 - (i % 2) * 30;
                        g.radial(x, y, 95, Puppet.alpha(col, 0.55f * on * pulse), Puppet.alpha(col, 0f));
                        g.oval(x, y, 95, 95);
                        g.color(Puppet.alpha(0xFFFFFFFF, 0.8f * on)); g.oval(x, y, 7, 7);
                    }
                } break;
                case Film.FX_TWINKLE: if (!behind) sparkles(g, t * 1.5f, f.x - 260, f.y - 140, 520, 220, 18, f.color == 0 ? 0xFF80DEEA : f.color); break;
                case Film.FX_DRONE: if (!behind) drone(g, s, f, t); break;
                case Film.FX_HEARTS: if (!behind && f.a != null) {
                    float ha = actorHeight(f.a.look, f.a.c, art, s), x0 = actorScreenX(f.a, t), y0 = s.ground - ha * 1.02f;
                    for (int i = 0; i < 5; i++) {
                        float k = (u * 0.6f + i * 0.2f) % 1f;
                        float x = x0 + (i - 2) * 22 + (float) Math.sin(t * 3 + i) * 8, y = y0 - k * 120, r = 7 + 4 * (float) Math.sin(t * 6 + i);
                        g.color(Puppet.alpha(0xFFFF4081, 1 - k));
                        g.oval(x - r * 0.5f, y - r * 0.3f, r * 0.6f, r * 0.6f); g.oval(x + r * 0.5f, y - r * 0.3f, r * 0.6f, r * 0.6f);
                        g.begin(); g.moveTo(x - r, y - r * 0.2f); g.lineTo(x + r, y - r * 0.2f); g.lineTo(x, y + r); g.close(); g.fillPath();
                    }
                } break;
                case Film.FX_NOTIFY: if (!behind && f.a != null) {
                    // a glowing card pops up above the head with a little "!" and bounces once
                    float ha = actorHeight(f.a.look, f.a.c, art, s), x = actorScreenX(f.a, t) + f.a.stateAt(t).facing * 40;
                    float k = Rig.smooth(0, 0.25f, u) * Rig.smooth(0, 0.3f, f.t1 - t), bounce = (float) Math.sin(Math.min(1, u / 0.5f) * Math.PI) * 14;
                    float y = s.ground - ha * 1.15f - bounce, w = 150 * k, h = 56 * k;
                    g.radial(x, y, 110, Puppet.alpha(0xFF40C4FF, 0.3f * k), 0x0040C4FF); g.oval(x, y, 110, 110);
                    g.color(Puppet.alpha(0xFFF5FBFF, k)); g.roundRect(x - w / 2, y - h / 2, w, h, 10);
                    g.color(Puppet.alpha(0xFF2979FF, k)); g.roundRect(x - w / 2 + 8, y - h / 2 + 10, h - 20, h - 20, 6);
                    g.color(Puppet.alpha(0xFFFFFFFF, k)); g.rect(x - w / 2 + 8 + (h - 20) / 2 - 2, y - h / 2 + 15, 4, 16); g.oval(x - w / 2 + 8 + (h - 20) / 2, y + h / 2 - 16, 4, 4);
                    g.color(Puppet.alpha(0xFF90A4AE, k)); g.rect(x - w / 2 + h, y - 8, w * 0.45f, 5); g.rect(x - w / 2 + h, y + 4, w * 0.3f, 5);
                } break;
                case Film.FX_DATA: if (!behind) {
                    // bits of light leave the place in a stream (the data stolen), bending away and upwards
                    for (int i = 0; i < 26; i++) {
                        float k = (u * 0.45f + i * 0.0385f) % 1f;
                        float x = f.x + k * 900 * (f.kind == 0 ? 1 : -1), y = f.y - k * 260 + (float) Math.sin(k * 9 + i) * 18 - i * 3;
                        g.color(Puppet.alpha(i % 3 == 0 ? 0xFF00E5FF : 0xFF76FF03, (1 - k) * 0.9f));
                        g.rect(x, y, 7, 7);
                    }
                } break;
                case Film.FX_SPARKS: if (!behind) {
                    float x = f.a != null ? actorScreenX(f.a, t) + f.a.stateAt(t).facing * 60 : f.x, y = f.y;
                    long sd = (long) (t * 60);
                    for (int i = 0; i < 10; i++) {
                        sd = sd * 6364136223846793005L + 1442695040888963407L;
                        float ang = ((sd >>> 33) % 360) * 0.01745f, len = 14 + ((sd >>> 13) % 30);
                        g.color(Puppet.alpha(i % 2 == 0 ? 0xFFFFF59D : 0xFF80D8FF, 0.9f * (1 - u / (f.t1 - f.t0))));
                        g.line(x, y, x + (float) Math.cos(ang) * len, y + (float) Math.sin(ang) * len, 2);
                    }
                    g.radial(x, y, 60, Puppet.alpha(0xFFB3E5FC, 0.5f * (1 - u / (f.t1 - f.t0))), 0x00B3E5FC); g.oval(x, y, 60, 60);
                } break;
                case Film.FX_STEAM: if (!behind) {
                    for (int i = 0; i < 6; i++) {
                        float k = (u * 0.5f + i * 0.17f) % 1f;
                        g.color(Puppet.alpha(0xFFECEFF1, 0.35f * (1 - k)));
                        g.oval(f.x + (float) Math.sin(k * 5 + i) * 25, f.y - k * 160, 18 + k * 40, 14 + k * 30);
                    }
                } break;
                case Film.FX_SHADOW_PASS: if (behind) {
                    // Gunn: the shadow of something unseen sweeps over the ground of a funny scene, slow in, slow out
                    float k = Math.min(1, u / (f.t1 - f.t0)), e = k * k * (3 - 2 * k);
                    float x = f.x - e * (f.x + 760);
                    g.color(Puppet.alpha(0xFF000000, 0.4f * (float) Math.sin(Math.PI * k)));
                    g.oval(x, s.ground - 14, 300, 46);
                    g.color(Puppet.alpha(0xFF000000, 0.22f * (float) Math.sin(Math.PI * k)));
                    g.oval(x + 120, s.ground - 40, 180, 30);
                } break;
                case Film.FX_GREEN_GLOW: if (!behind) {
                    float k = (float) Math.sin(Math.min(1, u / (f.t1 - f.t0)) * Math.PI);
                    g.radial(f.x, f.y, 260, Puppet.alpha(0xFF76FF03, 0.7f * k), 0x0076FF03);
                    g.oval(f.x, f.y, 260, 260);
                } break;
                case Film.FX_HIDE_ROCK: if (!behind) hideRock(g, f, s); break;
                case Film.FX_DUST: if (!behind) {
                    float k = u / (f.t1 - f.t0);
                    g.color(Puppet.alpha(0xFFBCAAA4, 0.6f * (1 - k)));
                    for (int i = 0; i < 5; i++) g.oval(f.x + (i - 2) * 18 * (1 + k), f.y - 10 - k * 20, 14 + k * 20, 10 + k * 10);
                } break;
                case Film.FX_LADDOO_GLOW: if (!behind) {
                    for (Film.Actor a : s.actors) {
                        if (a.stateAt(t).holdR == Pose.I_BASKET) {
                            float h = actorHeight(a.look, a.c, art, s);
                            float x = actorScreenX(a, t) + a.stateAt(t).facing * h * 0.15f;
                            sparkles(g, t * 2, x - 60, s.ground - h * 0.6f, 120, 80, 10, 0xFFFFEB3B);
                        }
                    }
                } break;
                default:
            }
        }
    }

    private void drawScreenFx(Gfx g, Film.Seg s, float t) {
        for (Film.Fx f : s.fx) {
            if (t < f.t0 || t >= f.t1) continue;
            float u = t - f.t0, d = f.t1 - f.t0;
            if (f.type == Film.FX_LIGHTNING) {
                Nature.flash(g, vw, vh, u);
            } else if (f.type == Film.FX_FLASH) {
                float k = 1 - u / d;
                g.color(Puppet.alpha(f.color == 0 ? 0xFFFFFFFF : f.color, 0.55f * k * k));
                g.rect(0, 0, vw, vh);
            } else if (f.type == Film.FX_FLICKER) {
                float fl = Sets.flicker(t * 1.3f + 7);
                g.color(Puppet.alpha(0xFF000000, 0.35f * (1 - fl)));
                g.rect(0, 0, vw, vh);
            } else if (f.type == Film.FX_LIGHTS_OFF) {
                // the lights go out: darkness comes down in half a second and stays (a little blue remains to see by)
                float k = Rig.smooth(0, 0.5f, u) * Rig.smooth(0, 0.6f, f.t1 - t);
                g.color(Puppet.alpha(0xFF040810, 0.62f * k));
                g.rect(0, 0, vw, vh);
            } else if (f.type == Film.FX_GLITCH) {
                // bands of the picture slip sideways and blocks of static appear for a moment
                long sd = (long) (t * 30) * 977;
                for (int i = 0; i < 6; i++) {
                    sd = sd * 6364136223846793005L + 1442695040888963407L;
                    float y = ((sd >>> 33) % 100) / 100f * vh, h = 6 + ((sd >>> 13) % 24), x = ((sd >>> 40) % 100) / 100f * vw * 0.6f;
                    g.color(Puppet.alpha(i % 2 == 0 ? 0xFF00E5FF : 0xFFFF4081, 0.35f));
                    g.rect(x, y, vw * 0.3f + ((sd >>> 20) % 300), h);
                    g.color(Puppet.alpha(0xFFFFFFFF, 0.2f));
                    g.rect(x + 20, y + h * 0.3f, vw * 0.15f, h * 0.3f);
                }
                g.color(Puppet.alpha(0xFF000000, 0.12f));
                g.rect(0, 0, vw, vh);
            } else if (f.type == Film.FX_SHOT && f.pic != null) {
                float a = Math.min(1, Math.min(u / 0.4f, (f.t1 - t) / 0.4f));
                g.save();
                g.setAlpha(Math.max(0, a));
                if (f.pic.object) {
                    // an insert of the important object: the place darkens and the thing itself comes up in the
                    // middle, a little larger as it settles, with a soft shadow under it
                    g.color(Puppet.alpha(0xFF000000, 0.5f));
                    g.rect(0, 0, vw, vh);
                    float grow = 0.92f + 0.08f * Math.min(1, u / 0.5f);
                    float ih = vh * 0.6f * grow, iw = ih * f.pic.w / Math.max(1, f.pic.h);
                    if (iw > vw * 0.7f) { iw = vw * 0.7f; ih = iw * f.pic.h / Math.max(1, f.pic.w); }
                    float x = vw / 2 - iw / 2, y = vh * 0.5f - ih / 2 - 6 * (u / d);
                    g.color(Puppet.alpha(0xFF000000, 0.3f));
                    g.oval(vw / 2, y + ih + 10, iw * 0.42f, ih * 0.045f);
                    g.image(f.pic.img, x, y, iw, ih);
                } else cover(g, f.pic, 1.0f + 0.08f * (u / d), -20 * (u / d), 0);
                g.restore();
            }
        }
    }

    private void magicFlower(Gfx g, Film.Fx f, float t) {
        float x = f.x, y = f.y;
        if (t < f.t2) {
            // dry, drooping bud
            g.color(0xFF6D5A3A);
            g.begin(); g.moveTo(x, y); g.quadTo(x + 8, y - 50, x + 22, y - 62); g.strokePath(4);
            g.color(0xFF8D6E4A);
            g.oval(x + 24, y - 60, 9, 13);
            g.color(0xFF7A6B44);
            g.oval(x - 12, y - 24, 12, 5);
            return;
        }
        float g1 = Math.min(1, (t - f.t2) / 0.7f);
        float over = g1 < 1 ? 1 + 0.25f * (float) Math.sin(g1 * Math.PI) : 1;
        float r = 46 * g1 * over;
        g.radial(x, y - 80, r * 3.2f, 0x99B3E5FC, 0x0040C4FF);
        g.oval(x, y - 80, r * 3.2f, r * 3.2f);
        g.color(0xFF2E7D32);
        g.line(x, y, x, y - 80, 6);
        g.oval(x - 16, y - 30, 16, 6);
        g.oval(x + 16, y - 46, 16, 6);
        for (int i = 0; i < 8; i++) {
            g.save();
            g.translate(x, y - 80);
            g.rotate(i * 45 + t * 10);
            g.color(i % 2 == 0 ? 0xFF29B6F6 : 0xFF4FC3F7);
            g.oval(0, -r * 0.6f, r * 0.32f, r * 0.62f);
            g.restore();
        }
        g.color(0xFFFFF59D);
        g.oval(x, y - 80, r * 0.28f, r * 0.28f);
        sparkles(g, t, x - 100, y - 200, 200, 160, 10, 0xFFE1F5FE);
    }

    private void bloom(Gfx g, Film.Seg s, Film.Fx f, float t) {
        int n = f.color == 1 ? 60 : 28;
        int[] cols = {0xFF42A5F5, 0xFFEC407A, 0xFFFFCA28, 0xFFAB47BC, 0xFFFF7043, 0xFF26C6DA, 0xFFF48FB1};
        for (int i = 0; i < n; i++) {
            float st = f.t0 + (i % 12) * 0.16f + (i / 12) * 0.05f;
            if (t < st) continue;
            float k = Math.min(1, (t - st) / 0.35f);
            float sc = k < 1 ? k * (1 + 0.4f * (float) Math.sin(k * Math.PI)) : 1;
            float x = 40 + ((i * 7349) % 1000) / 1000f * 1200;
            float y = s.ground - 30 + ((i * 3571) % 1000) / 1000f * 70;
            float r = (9 + (i % 4) * 3) * sc;
            int c = cols[i % cols.length];
            g.color(0xFF388E3C);
            g.line(x, y + r, x, y + r * 2.2f, 2);
            for (int pI = 0; pI < 5; pI++) {
                double a = pI * Math.PI * 2 / 5 + i;
                g.color(c);
                g.oval(x + (float) Math.cos(a) * r * 0.55f, y + (float) Math.sin(a) * r * 0.55f, r * 0.5f, r * 0.5f);
            }
            g.color(0xFFFFF59D);
            g.oval(x, y, r * 0.3f, r * 0.3f);
            if (k < 1) { g.color(Puppet.alpha(0xFFFFFFFF, 1 - k)); g.oval(x, y, r * 2 * (1 - k) + 2, r * 2 * (1 - k) + 2); }
        }
    }

    private void ribbon(Gfx g, float x, float y, int c) {
        g.color(c == 0 ? 0xFFC62828 : c);
        g.begin();
        g.moveTo(x - 40, y); g.cubicTo(x - 20, y - 18, x, y + 12, x + 20, y - 4); g.cubicTo(x + 30, y - 10, x + 40, y + 2, x + 48, y - 2);
        g.strokePath(7);
    }

    private void bell(Gfx g, Film.Fx f, float t) {
        float u = t - f.t0;
        float sw = (float) Math.sin(u * 5) * 18 * Math.max(0, 1 - u / (f.t1 - f.t0));
        g.save();
        g.translate(f.x, f.y - 140);
        g.color(0xFF5D4037);
        g.line(-120, 0, 120, 0, 12);
        g.rotate(sw);
        g.color(0xFF8D6E63);
        g.line(0, 0, 0, 40, 4);
        g.color(0xFFB08A3E);
        g.begin(); g.moveTo(-18, 40); g.quadTo(-22, 100, -55, 130); g.lineTo(55, 130); g.quadTo(22, 100, 18, 40); g.close(); g.fillPath();
        g.color(0xFFD4AF5A);
        g.rect(-50, 122, 100, 10);
        g.color(0xFF6D4C41);
        g.oval(0 + sw, 140, 10, 10);
        g.color(0xFFC8A951);
        g.line(0, 140, 0, 330, 3);
        g.restore();
        for (int i = 0; i < 3; i++) {
            float k = ((u * 1.2f + i * 0.33f) % 1f);
            g.color(Puppet.alpha(0xFFFFE082, 0.5f * (1 - k)));
            g.strokeOval(f.x, f.y - 60, 80 + k * 300, 50 + k * 190, 4);
        }
    }

    private void butterfly(Gfx g, Film.Seg s, Film.Fx f, float t) {
        float x, y;
        if (t < f.t2 && f.a != null) {
            Film.Key k = f.a.stateAt(t);
            float ax = Director.xAt(f.a, t);
            x = ax + 140 * (k.facing) + (float) Math.sin(t * 2.3f) * 60;
            y = s.ground - 250 + (float) Math.sin(t * 3.1f) * 40;
        } else if (t < f.t2 + 1.2f && f.a != null) {
            float ax = Director.xAt(f.a, f.t2);
            float sx = ax + 140, sy = s.ground - 250;
            float u = (t - f.t2) / 1.2f;
            x = sx + (f.x + 22 - sx) * u;
            y = sy + (f.y - 60 - sy) * u - (float) Math.sin(u * Math.PI) * 60;
        } else { x = f.x + 22; y = f.y - 66; }
        boolean resting = t >= f.t2 + 1.2f;
        float flap = resting ? 0.4f + 0.3f * (float) Math.sin(t * 3) : bump(t * 18);
        g.save();
        g.translate(x, y);
        float wing = 18 * (0.25f + flap);
        g.color(0xFFFF8F00); g.oval(-wing * 0.6f, -6, wing * 0.65f, 12); g.oval(wing * 0.6f, -6, wing * 0.65f, 12);
        g.color(0xFF7E57C2); g.oval(-wing * 0.45f, 8, wing * 0.45f, 9); g.oval(wing * 0.45f, 8, wing * 0.45f, 9);
        g.color(0xFF3E2723); g.roundRect(-2, -10, 4, 22, 2);
        g.line(0, -10, -6, -18, 1.5f); g.line(0, -10, 6, -18, 1.5f);
        g.restore();
    }

    private void smoke(Gfx g, Film.Seg s, Film.Fx f, float t) {
        float u = (t - f.t0) / (f.t1 - f.t0);
        float x = f.x, y = f.y;
        int c = f.color == 0 ? 0xFF9E9E9E : f.color;
        for (int i = 0; i < 12; i++) {
            double a = i * 2.39996;
            float r = (40 + (i % 4) * 22) * (0.4f + u * 1.4f);
            float px = x + (float) Math.cos(a) * u * 150, py = y + (float) Math.sin(a) * u * 90 - u * 60;
            g.color(Puppet.alpha(Puppet.mix(c, 0xFFFFFFFF, (i % 3) * 0.15f), 0.75f * (1 - u)));
            g.oval(px, py, r, r * 0.8f);
        }
    }

    /** A small drone: a body with four rotors, a blinking light, flying in a lazy figure near its actor. */
    private void drone(Gfx g, Film.Seg s, Film.Fx f, float t) {
        float u = t - f.t0;
        float cx = f.a != null ? actorScreenX(f.a, t) : f.x, cy = s.ground - (f.a != null ? actorHeight(f.a.look, f.a.c, art, s) * 1.25f : 300);
        float x = cx + (float) Math.sin(u * 0.9) * 170 + (float) Math.sin(u * 2.3) * 30, y = cy - 40 + (float) Math.cos(u * 1.3) * 45;
        float k = Rig.smooth(0, 0.4f, u) * Rig.smooth(0, 0.5f, f.t1 - t);
        g.save();
        g.setAlpha(k);
        g.color(0xFF263238); g.roundRect(x - 16, y - 6, 32, 12, 5);
        g.color(0xFF455A64); g.line(x - 26, y - 8, x + 26, y - 8, 3);
        g.color(Puppet.alpha(0xFF90A4AE, 0.55f));
        float spin = (t * 40) % 1f;
        g.oval(x - 26, y - 10, 16, 3 + 2 * spin); g.oval(x + 26, y - 10, 16, 3 + 2 * (1 - spin));
        g.color(((int) (t * 5)) % 2 == 0 ? 0xFFFF1744 : 0xFF00E676); g.oval(x, y + 2, 3, 3);
        g.color(0xFF80DEEA); g.oval(x + 10, y, 4, 3);
        g.restore();
    }

    private void beam(Gfx g, Film.Seg s, Film.Fx f, float t) {
        float u = t - f.t0;
        if (f.a == null) return;
        float ha = actorHeight(f.a.look, f.a.c, art, s);
        float ax = Director.xAt(f.a, t) + f.a.stateAt(t).facing * ha * 0.36f, ay = s.ground - ha * 0.45f;
        float bx, by;
        if (f.b != null) {
            float hb = actorHeight(f.b.look, f.b.c, art, s);
            bx = Director.xAt(f.b, t); by = s.ground - hb * 0.82f;
        } else { bx = ax + 600 * f.a.stateAt(t).facing; by = ay - 80; }
        float k = Math.min(1, u / 0.3f);
        float ex = ax + (bx - ax) * k, ey = ay + (by - ay) * k;
        float dx = ex - ax, dy = ey - ay, len = (float) Math.sqrt(dx * dx + dy * dy) + 1;
        float nx = -dy / len, ny = dx / len;
        float flick = 0.85f + 0.15f * (float) Math.sin(t * 30);
        g.color(Puppet.alpha(0xFFFFF8E1, 0.55f * flick));
        g.begin(); g.moveTo(ax + nx * 6, ay + ny * 6); g.lineTo(ex + nx * 34, ey + ny * 34); g.lineTo(ex - nx * 34, ey - ny * 34); g.lineTo(ax - nx * 6, ay - ny * 6); g.close(); g.fillPath();
        g.color(Puppet.alpha(0xFFFFFFFF, 0.8f * flick));
        g.begin(); g.moveTo(ax + nx * 2, ay + ny * 2); g.lineTo(ex + nx * 10, ey + ny * 10); g.lineTo(ex - nx * 10, ey - ny * 10); g.lineTo(ax - nx * 2, ay - ny * 2); g.close(); g.fillPath();
        if (k >= 1) {
            g.radial(ex, ey, 90, Puppet.alpha(0xFFFFFFFF, 0.9f), 0x00FFFFFF);
            g.oval(ex, ey, 90, 90);
        }
    }

    private void net(Gfx g, Film.Seg s, Film.Fx f, float t) {
        if (f.a == null) return;
        float u = Math.min(1, (t - f.t0) / 0.45f);
        float h = actorHeight(f.a.look, f.a.c, art, s);
        float x = Director.xAt(f.a, t), top = s.ground - h * 1.05f - (1 - u) * 300;
        float w = h * 0.55f;
        g.color(0xCC8D6E63);
        for (int i = 0; i <= 8; i++) {
            float fx = x - w + i * w / 4;
            g.line(fx, top, x - w + (fx - (x - w)) * 0.9f + w * 0.05f, s.ground, 3);
        }
        for (int j = 0; j <= 8; j++) {
            float yy = top + (s.ground - top) * j / 8f;
            float ww = w * (0.6f + 0.4f * (float) Math.sin(Math.PI * j / 8f));
            g.line(x - ww, yy, x + ww, yy, 3);
        }
    }

    private void cage(Gfx g, Film.Seg s, Film.Fx f, float t) {
        float x = f.x, y = s.ground + 6;
        float w = 230, h = 170;
        boolean open = t > f.t2;
        g.color(0xFF37474F);
        g.rect(x - w / 2, y - h, w, 10);
        g.rect(x - w / 2, y - 6, w, 8);
        g.color(0xFF546E7A);
        for (int i = 0; i <= 8; i++) {
            float bx = x - w / 2 + i * w / 8;
            if (open && i <= 3) {
                float k = Math.min(1, (t - f.t2) / 0.6f);
                bx = x - w / 2 + (i * w / 8) * (1 - k) - k * 40;
            }
            g.line(bx, y - h + 6, bx, y - 4, 5);
        }
        g.color(0xFF8D6E63);
        g.rect(x + w / 2 - 30, y - h / 2 - 8, 18, 16);
    }

    private void splash(Gfx g, Film.Fx f, float u) {
        float d = f.t1 - f.t0, k = u / d;
        g.color(Puppet.alpha(0xFF81D4FA, 1 - k));
        for (int i = 0; i < 14; i++) {
            double a = -Math.PI * (0.15 + 0.7 * i / 13.0);
            float r = 30 + k * 110;
            g.oval(f.x + (float) Math.cos(a) * r, f.y + (float) Math.sin(a) * r + k * k * 60, 5, 7);
        }
    }

    private void leaves(Gfx g, Film.Fx f, float u) {
        for (int i = 0; i < 10; i++) {
            float x = f.x - 120 + i * 26 + (float) Math.sin(u * 3 + i) * 20;
            float y = f.y - 60 + u * 140 + i * 9;
            g.save();
            g.translate(x, y);
            g.rotate(u * 200 + i * 40);
            g.color(i % 2 == 0 ? 0xFF558B2F : 0xFF7CB342);
            g.oval(0, 0, 9, 4);
            g.restore();
        }
    }

    private void hideRock(Gfx g, Film.Fx f, Film.Seg s) {
        float x = f.x, y = s.ground + 20;
        g.color(0xFF1E2421);
        g.begin();
        g.moveTo(x - 270, y + 20); g.lineTo(x - 240, y - 190); g.lineTo(x - 120, y - 245); g.lineTo(x + 10, y - 215);
        g.lineTo(x + 140, y - 250); g.lineTo(x + 250, y - 170); g.lineTo(x + 280, y + 20); g.close();
        g.fillPath();
        g.color(0xFF2C3530);
        g.begin(); g.moveTo(x - 200, y - 170); g.lineTo(x - 120, y - 225); g.lineTo(x - 40, y - 185); g.close(); g.fillPath();
        g.color(0xFF3A4540);
        g.begin(); g.moveTo(x + 60, y - 200); g.lineTo(x + 140, y - 240); g.lineTo(x + 200, y - 180); g.close(); g.fillPath();
    }

    // ================================================================== subtitles

    private final List<String> wrapBuf = new ArrayList<String>();

    private void drawSubs(Gfx g, Film.Seg s, float t) {
        for (Film.Sub sb : s.subs) {
            if (t < sb.t0 || t >= sb.t1) continue;
            float size = 30;
            String who = sb.who.length() > 0 ? sb.who + ": " : "";
            List<String> lines = wrap(g, who + Txt.withoutParens(sb.text), size, vw - 70);
            int per = 2;
            int chunks = (lines.size() + per - 1) / per;
            int ci = Math.min(chunks - 1, (int) ((t - sb.t0) / (sb.t1 - sb.t0) * chunks));
            int from = ci * per, to = Math.min(lines.size(), from + per);
            float boxH = (to - from) * 42 + 22;
            float maxW = 0;
            for (int i = from; i < to; i++) maxW = Math.max(maxW, g.textWidth(lines.get(i), size, true));
            g.color(0x99000000);
            g.roundRect(vw / 2 - maxW / 2 - 24, vh - 24 - boxH, maxW + 48, boxH, 14);
            for (int i = from; i < to; i++) {
                float y = vh - 24 - boxH + 42 + (i - from) * 42 - 6;
                String l = lines.get(i);
                if (i == 0 && who.length() > 0 && l.startsWith(who)) {
                    float ww = g.textWidth(l, size, true);
                    float x0 = vw / 2 - ww / 2;
                    g.color(0xFFFFD54F);
                    g.text(who, x0, y, size, true, 0);
                    g.color(0xFFFFFFFF);
                    g.text(l.substring(who.length()), x0 + g.textWidth(who, size, true), y, size, true, 0);
                } else {
                    g.color(0xFFFFFFFF);
                    g.text(l, vw / 2, y, size, true, 1);
                }
            }
        }
    }

    private List<String> wrap(Gfx g, String text, float size, float maxW) {
        wrapBuf.clear();
        String[] words = text.split(" ");
        StringBuilder cur = new StringBuilder();
        for (String w : words) {
            String trial = cur.length() == 0 ? w : cur + " " + w;
            if (g.textWidth(trial, size, true) > maxW && cur.length() > 0) {
                wrapBuf.add(cur.toString());
                cur.setLength(0);
                cur.append(w);
            } else {
                cur.setLength(0);
                cur.append(trial);
            }
        }
        if (cur.length() > 0) wrapBuf.add(cur.toString());
        return new ArrayList<String>(wrapBuf);
    }
}
