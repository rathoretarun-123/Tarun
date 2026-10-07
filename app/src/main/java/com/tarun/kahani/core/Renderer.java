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
    private final Pose pose = new Pose();

    public Renderer(Film film, Art art) {
        this.film = film;
        this.art = art == null ? new Art() : art;
    }

    /** On-screen height (stage pixels) of a character standing on the floor. */
    public static float actorHeight(Look l, Story.CharacterDef c, Art art, Film.Seg seg) {
        boolean sprite = art != null && c != null && art.sprites.containsKey(c.id);
        float base = sprite ? 400f : 345f;
        float h = l.height;
        if (sprite && l.kind == Look.MONKEY) h = 0.42f;
        return base * h;
    }

    // ================================================================== frame

    public void render(Gfx g, float t) {
        g.save();
        g.scale(g.width() / W, g.height() / H);
        Film.Seg s = film.segAt(t);
        g.color(0xFF000000);
        g.rect(0, 0, W, H);
        if (s != null) {
            switch (s.type) {
                case Film.S_TITLE: drawTitle(g, s, t); break;
                case Film.S_CARD: drawCard(g, s, t); break;
                case Film.S_END: drawEnd(g, s, t); break;
                default: drawScene(g, s, t);
            }
            float a = 1f;
            if (s.fadeIn > 0) a = Math.min(a, (t - s.t0) / s.fadeIn);
            if (s.fadeOut > 0) a = Math.min(a, (s.t1 - t) / s.fadeOut);
            if (a < 1f) { g.color(Puppet.alpha(0xFF000000, 1f - Math.max(0, a))); g.rect(0, 0, W, H); }
        }
        g.restore();
    }

    // ================================================================== title, cards, end

    private void cover(Gfx g, Art.Backdrop b, float zoom, float panX, float panY) {
        float sx = b.x0 * b.w, sy = b.y0 * b.h, sw = (b.x1 - b.x0) * b.w, sh = (b.y1 - b.y0) * b.h;
        g.save();
        g.translate(W / 2 + panX, H / 2 + panY);
        g.scale(zoom, zoom);
        g.imageRect(b.img, sx, sy, sw, sh, -W / 2, -H / 2, W, H);
        g.restore();
    }

    /** Portrait pictures: blurred-looking dark fill behind, full picture in the middle. */
    private void contain(Gfx g, Art.Backdrop b, float zoom) {
        float ar = b.w / (float) b.h;
        if (ar >= 1.5f) { cover(g, b, zoom, 0, 0); return; }
        g.save();
        g.translate(W / 2, H / 2);
        g.scale(1.6f, 1.6f);
        g.setAlpha(0.55f);
        g.image(b.img, -W / 2, -W / 2 / ar, W, W / ar);
        g.restore();
        g.color(0x88000000);
        g.rect(0, 0, W, H);
        float h = H * zoom, w = h * ar;
        g.image(b.img, (W - w) / 2, (H - h) / 2, w, h);
    }

    private void drawTitle(Gfx g, Film.Seg s, float t) {
        float u = (t - s.t0) / Math.max(0.1f, s.t1 - s.t0);
        boolean text = art.titleText || s.backdrop == null;
        if (s.backdrop != null) {
            contain(g, s.backdrop, 1.0f + 0.05f * u);
        } else {
            g.save();
            g.translate(W / 2, H / 2); g.scale(1 + 0.05f * u, 1 + 0.05f * u); g.translate(-W / 2, -H / 2);
            g.layer("set:title", W, H, new Gfx.Painter() { public void paint(Gfx gg) { Sets.paintStatic(gg, Sets.GARDEN, Sets.MORNING); } });
            Sets.paintLive(g, Sets.GARDEN, Sets.MORNING, t);
            heroesLineup(g, t, 2, 640, Sets.GROUND + 20, 1.0f);
            g.restore();
        }
        sparkles(g, t, 0, 0, W, H, 26, 0xFFFFE082);
        if (text) {
            float a = Math.min(1, Math.max(0, (t - s.t0 - 0.6f) / 1.2f));
            g.save();
            g.setAlpha(a);
            g.linear(0, 40, 0, 240, 0xAA000000, 0x00000000);
            g.rect(0, 0, W, 260);
            bigText(g, s.text1, W / 2, 150, fitSize(g, s.text1, 78, 1180), 0xFFFFD54F, 0xFF5D2E00);
            if (s.text2.length() > 0) bigText(g, s.text2, W / 2, 210, 34, 0xFFFFFFFF, 0xFF000000);
            g.restore();
        }
    }

    private void drawCard(Gfx g, Film.Seg s, float t) {
        float u = (t - s.t0) / Math.max(0.1f, s.t1 - s.t0);
        if (s.backdrop != null) cover(g, s.backdrop, 1.08f + 0.04f * u, 0, 0);
        else {
            final int set = s.set, tod = s.tod;
            g.save();
            g.translate(W / 2, H / 2); g.scale(1.08f + 0.04f * u, 1.08f + 0.04f * u); g.translate(-W / 2, -H / 2);
            g.layer("set:" + set + ":" + tod, W, H, new Gfx.Painter() { public void paint(Gfx gg) { Sets.paintStatic(gg, set, tod); } });
            g.restore();
        }
        g.color(0xB0000000);
        g.rect(0, 0, W, H);
        float a = Math.min(1, (t - s.t0) / 0.5f);
        g.save();
        g.setAlpha(a);
        // ornament lines
        g.color(0xFFE5B530);
        float lw = 280 * Math.min(1, (t - s.t0) / 0.8f);
        g.line(W / 2 - 40 - lw, 300, W / 2 - 40, 300, 3);
        g.line(W / 2 + 40, 300, W / 2 + 40 + lw, 300, 3);
        g.oval(W / 2, 300, 7, 7);
        bigText(g, s.text1, W / 2, 400, 92, 0xFFFFD54F, 0xFF4E2600);
        if (s.text2.length() > 0) bigText(g, s.text2, W / 2, 478, fitSize(g, s.text2, 46, 1150), 0xFFFFFFFF, 0xFF000000);
        g.color(0xFFE5B530);
        g.line(W / 2 - 40 - lw, 520, W / 2 + 40 + lw, 520, 2);
        g.restore();
    }

    private void drawEnd(Gfx g, Film.Seg s, float t) {
        float u = (t - s.t0) / Math.max(0.1f, s.t1 - s.t0);
        boolean text = art.endText || s.backdrop == null;
        if (s.backdrop != null) contain(g, s.backdrop, 1.0f + 0.06f * u);
        else {
            g.save();
            g.translate(W / 2, H / 2); g.scale(1 + 0.04f * u, 1 + 0.04f * u); g.translate(-W / 2, -H / 2);
            g.layer("set:end", W, H, new Gfx.Painter() { public void paint(Gfx gg) { Sets.paintStatic(gg, Sets.CELEBRATION, Sets.EVENING); } });
            Sets.paintLive(g, Sets.CELEBRATION, Sets.EVENING, t);
            heroesLineup(g, t, 6, 640, Sets.GROUND + 30, 0.85f);
            g.restore();
        }
        sparkles(g, t, 0, 0, W, H, 40, 0xFFFFE082);
        if (text) {
            float a = Math.min(1, Math.max(0, (t - s.t0 - 0.8f) / 1.2f));
            float sc = 0.8f + 0.2f * a;
            g.save();
            g.setAlpha(a);
            g.color(0x77000000);
            g.roundRect(W / 2 - 300, 70, 600, 170, 30);
            g.translate(W / 2, 200);
            g.scale(sc, sc);
            bigText(g, s.text1, 0, 0, 120, 0xFFFFD54F, 0xFF4E2600);
            g.restore();
        }
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
            if (sp != null) drawSprite(g, sp, c.look, pose, h, 0);
            else Puppet.draw(g, c.look, pose, h);
            g.restore();
        }
    }

    // ================================================================== scene

    private float camX, camY, camZ;

    private void camera(Film.Seg s, float t) {
        Film.Cam cur = null, prev = null;
        for (Film.Cam c : s.cams) { if (c.t <= t) { prev = cur; cur = c; } else break; }
        if (cur == null) { camX = 640; camY = 360; camZ = 1; }
        else if (cur.ease > 0 && prev != null && t < cur.t + cur.ease) {
            float u = (t - cur.t) / cur.ease;
            u = u * u * (3 - 2 * u);
            camX = prev.cx + (cur.cx - prev.cx) * u;
            camY = prev.cy + (cur.cy - prev.cy) * u;
            camZ = prev.zoom + (cur.zoom - prev.zoom) * u;
        } else { camX = cur.cx; camY = cur.cy; camZ = cur.zoom; }
        if (camZ < 1) camZ = 1;
        float hw = W / 2 / camZ, hh = H / 2 / camZ;
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

    private void drawScene(Gfx g, final Film.Seg s, float t) {
        camera(s, t);
        g.save();
        g.translate(W / 2, H / 2);
        g.scale(camZ, camZ);
        g.translate(-camX, -camY);
        if (s.backdrop != null) {
            final Art.Backdrop b = s.backdrop;
            g.layer("bd:" + System.identityHashCode(b), W, H, new Gfx.Painter() {
                public void paint(Gfx gg) {
                    gg.imageRect(b.img, b.x0 * b.w, b.y0 * b.h, (b.x1 - b.x0) * b.w, (b.y1 - b.y0) * b.h, 0, 0, W, H);
                }
            });
            if (s.tod == Sets.EVENING) { g.color(0x40FF7043); g.rect(0, 0, W, H); g.color(0x30301060); g.rect(0, 0, W, H); }
            if (s.tod == Sets.NIGHT && s.set != Sets.FOREST) { g.color(0x50101C3A); g.rect(0, 0, W, H); }
            if (s.festive) Sets.celebrationLights(g, t);
        } else {
            final int set = s.set, tod = s.tod;
            g.layer("set:" + set + ":" + tod, W, H, new Gfx.Painter() { public void paint(Gfx gg) { Sets.paintStatic(gg, set, tod); } });
            Sets.paintLive(g, set, tod, t);
        }
        // tree branch for monkeys
        for (Film.Actor a : s.actors) {
            Film.Key k = a.stateAt(t);
            if (k.visible && k.anchor == Film.A_BRANCH) { branch(g, s, k.x); break; }
        }
        drawFxLayer(g, s, t, true);
        List<Film.Actor> list = new ArrayList<Film.Actor>(s.actors);
        final float tt = t;
        Collections.sort(list, new Comparator<Film.Actor>() {
            public int compare(Film.Actor a, Film.Actor b) {
                Film.Key ka = a.stateAt(tt), kb = b.stateAt(tt);
                int la = layerOf(ka), lb = layerOf(kb);
                if (la != lb) return la - lb;
                return a.order - b.order;
            }
        });
        for (Film.Actor a : list) drawActor(g, s, a, t);
        drawFxLayer(g, s, t, false);
        if (s.backdrop == null) Sets.paintFront(g, s.set, s.tod);
        g.restore();
        if (s.backdrop == null) {
            int tint = Sets.tint(s.set, s.tod);
            if (tint != 0) { g.color(tint); g.rect(0, 0, W, H); }
        }
        drawScreenFx(g, s, t);
        if (film.subtitles) drawSubs(g, s, t);
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
                    return 0.35f + 0.35f * (float) Math.abs(Math.sin((t - sp.t0) * 11));
                }
                int i = (int) ((t - l.start) * 100);
                if (i < 0 || i >= l.env.length) return 0;
                return l.env[i];
            }
        }
        return 0;
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
        Art.Sprite sp = art.sprites.get(a.c.id);
        float h = actorHeight(a.look, a.c, art, s);
        float x = Director.xAt(a, t);
        float y = s.ground;
        float scale = 1f;
        if (k.depth > 0) { y -= 26; scale = 0.88f; }

        // ---- pose
        Pose p = pose;
        p.reset();
        p.time = t;
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
        p.blink = blink(t, a.order);
        p.turbanColor = 0xFF2F5DB5;
        p.turbanBand = 0xFFC62828;
        for (Film.Actor o : s.actors) if (o != a && o.look.headwear == Look.HW_TURBAN && o.stateAt(t).noHeadwear) { p.turbanColor = o.look.headColor; p.turbanBand = o.look.headBand; }
        Film.Speak spk = speakingAt(a, t);
        if (spk != null) {
            p.mouth = mouthAt(a, t);
            if (spk.emotion != Pose.NEUTRAL) p.emotion = spk.emotion;
            if (p.emotion == Pose.SAD) p.tears = true;
        }
        Film.Key mv = moving(a, t);
        mo.dx = 0; mo.dy = 0; mo.rot = 0; mo.sx = 1; mo.sy = 1;
        if (mv != null) {
            float speed = mv.run ? 15f : 9f;
            p.walk = t * speed;
            p.walkAmt = 1f;
            p.facing = mv.facing;
            mo.dy = -(float) Math.abs(Math.sin(t * speed)) * (mv.run ? 16 : 7);
            mo.rot = (float) Math.sin(t * speed) * (mv.run ? 4 : 2.5f);
            p.armL = 25 + (float) Math.sin(t * speed) * 25;
            p.armR = 25 - (float) Math.sin(t * speed) * 25;
        } else {
            // idle breathing
            float br = (float) Math.sin(t * 2.1f + a.order);
            mo.sy = 1 + br * 0.006f;
            p.bob = br * 1.2f;
        }
        applyActs(a, p, t);
        if (spk != null) {
            // talking: small nods with the voice
            float m = p.mouth;
            mo.rot += (float) Math.sin(t * 5.3f + a.order) * 1.2f * (0.3f + m);
            mo.dy -= m * 3;
            if (p.armR < 30 && a.look.kind != Look.MONKEY) { p.armR = 30 + (float) Math.sin(t * 2.7f) * 15; p.elbowR = 40; }
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
                    if (mv2 != null) y -= (float) Math.abs(Math.sin(t * 9)) * 8;
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
        g.translate(x + mo.dx, y + mo.dy);
        if (scale != 1f) g.scale(scale, scale);
        if (k.netted) mo.sy *= 0.97f;
        if (sp != null) drawSprite(g, sp, a.look, p, h, mo.rot);
        else {
            if (mo.rot != 0) g.rotate(mo.rot * 0.5f);
            Puppet.draw(g, a.look, p, h);
        }
        g.restore();
    }

    /** Converts gestures into arm angles (cartoon puppets) and body motion (picture sprites). */
    private void applyActs(Film.Actor a, Pose p, float t) {
        for (Film.Act act : a.acts) {
            if (t < act.t0 || t >= act.t1) continue;
            float u = t - act.t0;
            float s = (float) Math.sin(u * 10);
            switch (act.type) {
                case Film.G_TALK:
                    p.armR = Math.max(p.armR, 35 + (float) Math.sin(u * 3.1f) * 20); p.elbowR = 45;
                    break;
                case Film.G_CLAP:
                    p.armL = 60; p.armR = 60; p.elbowL = 70 + s * 20; p.elbowR = 70 + s * 20;
                    mo.dy -= Math.abs(s) * 4;
                    break;
                case Film.G_LAUGH:
                    p.emotion = p.mouth > 0.05f ? p.emotion : Pose.LAUGH;
                    if (p.emotion == Pose.NEUTRAL) p.emotion = Pose.LAUGH;
                    mo.dy -= Math.abs((float) Math.sin(u * 14)) * 6;
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
                    mo.dy += Math.abs(s) * 2;
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
                    mo.dy -= Math.abs((float) Math.sin(u * 6)) * 12;
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
                    if (a.look.kind != Look.MONSTER) mo.dy += Math.abs(s) * 10;
                    break;
                case Film.G_TWIRL:
                    p.armR = 140; p.elbowR = 120 + s * 12;
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
                    float d = act.t1 - act.t0;
                    float v = Math.min(1, u / d);
                    mo.dy -= (float) Math.sin(v * Math.PI) * 70;
                    p.armL = 150; p.armR = 150;
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
                    mo.dy -= Math.abs((float) Math.sin(t * 9)) * 5;
                    break;
                case Film.G_PLAY: case Film.G_BOUNCE:
                    mo.dy -= Math.abs((float) Math.sin(u * 6)) * 14;
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
                    mo.rot += (float) Math.abs(Math.sin(u * 6)) * 6 * p.facing;
                    mo.dy += Math.abs((float) Math.sin(u * 6)) * 4;
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
                    mo.sx *= 1 + 0.04f * (float) Math.abs(Math.sin(u * 3));
                    mo.sy *= 1 + 0.04f * (float) Math.abs(Math.sin(u * 3));
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

    private void drawSprite(Gfx g, Art.Sprite sp, Look look, Pose p, float h, float rot) {
        float scale = h / sp.h;
        float w = sp.w * scale;
        g.save();
        if (p.body == Pose.LIE) {
            g.translate(0, -w * 0.32f);
            g.rotate(p.facing > 0 ? -86 : 86);
            g.translate(0, h * 0.5f);
        } else if (p.body == Pose.HANG) {
            g.rotate(180);
        }
        if (rot != 0) g.rotate(rot);
        float sy = mo.sy, sx = mo.sx;
        if (p.body == Pose.SIT) sy *= 0.8f;
        else if (p.body == Pose.KNEEL) sy *= 0.74f;
        if (p.walkAmt > 0) sy *= 1 + (float) Math.sin(p.walk * 2) * 0.012f * p.walkAmt;
        float mirror = p.facing < 0 ? -1 : 1;
        g.scale(sx * mirror, sy);
        // shadow
        if (p.body != Pose.LIE && p.body != Pose.HANG) { g.color(0x40000000); g.oval(0, 0, w * 0.42f, h * 0.025f); }
        float left = -w / 2, top = -h;
        if (p.noHeadwear && sp.turbanY > 0) {
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
            g.imageRect(sp.img, 0, cut * sp.h, sp.w, sp.h * (1 - cut), left, top + cut * h, w, h * (1 - cut));
        } else {
            g.image(sp.img, left, top, w, h);
        }
        if (sp.faceKnown) {
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
                g.color(0xCC81D4FA);
                g.oval(ex2 + er * 2.2f, ey2 - er * 1.6f + (p.time * 15) % (er * 2), er * 0.25f, er * 0.38f);
            }
            // lip-sync mouth
            float m = p.mouth;
            if (m > 0.06f) {
                float mx = left + sp.mouthX * w, my = top + sp.mouthY * h;
                float hw = sp.mouthHW * w;
                float oh = hw * (0.18f + 0.62f * m);
                float ow = hw * (0.95f - 0.15f * m);
                g.color(sp.lip);
                g.oval(mx, my + oh * 0.35f, ow + hw * 0.1f, oh + hw * 0.1f);
                g.color(0xFF3B0E0E);
                g.oval(mx, my + oh * 0.35f, ow, oh);
                g.color(0xFFF5F2EA);
                g.roundRect(mx - ow * 0.7f, my + oh * 0.35f - oh * 0.92f, ow * 1.4f, oh * 0.38f, oh * 0.15f);
                if (m > 0.3f) { g.color(0xFFD9636B); g.oval(mx, my + oh * 0.35f + oh * 0.55f, ow * 0.55f, oh * 0.32f); }
            }
            if (p.wearsTurban) {
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
            if (p.holdR == Pose.I_WOOD_SWORD || p.holdR == Pose.I_SWORD) {
                g.save();
                g.translate(hx, hy);
                g.rotate(p.facing * (-40 + (float) Math.sin(p.time * 10) * 35));
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
                case Film.FX_LEAVES: if (!behind) leaves(g, f, u); break;
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
            if (f.type == Film.FX_FLASH) {
                float k = 1 - u / d;
                g.color(Puppet.alpha(f.color == 0 ? 0xFFFFFFFF : f.color, 0.55f * k * k));
                g.rect(0, 0, W, H);
            } else if (f.type == Film.FX_SHOT && f.pic != null) {
                float a = Math.min(1, Math.min(u / 0.4f, (f.t1 - t) / 0.4f));
                g.save();
                g.setAlpha(Math.max(0, a));
                cover(g, f.pic, 1.0f + 0.08f * (u / d), -20 * (u / d), 0);
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
        float flap = resting ? 0.4f + 0.3f * (float) Math.sin(t * 3) : (float) Math.abs(Math.sin(t * 18));
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
            List<String> lines = wrap(g, who + Txt.withoutParens(sb.text), size, 1100);
            int per = 2;
            int chunks = (lines.size() + per - 1) / per;
            int ci = Math.min(chunks - 1, (int) ((t - sb.t0) / (sb.t1 - sb.t0) * chunks));
            int from = ci * per, to = Math.min(lines.size(), from + per);
            float boxH = (to - from) * 42 + 22;
            float maxW = 0;
            for (int i = from; i < to; i++) maxW = Math.max(maxW, g.textWidth(lines.get(i), size, true));
            g.color(0x99000000);
            g.roundRect(W / 2 - maxW / 2 - 24, H - 24 - boxH, maxW + 48, boxH, 14);
            for (int i = from; i < to; i++) {
                float y = H - 24 - boxH + 42 + (i - from) * 42 - 6;
                String l = lines.get(i);
                if (i == 0 && who.length() > 0 && l.startsWith(who)) {
                    float ww = g.textWidth(l, size, true);
                    float x0 = W / 2 - ww / 2;
                    g.color(0xFFFFD54F);
                    g.text(who, x0, y, size, true, 0);
                    g.color(0xFFFFFFFF);
                    g.text(l.substring(who.length()), x0 + g.textWidth(who, size, true), y, size, true, 0);
                } else {
                    g.color(0xFFFFFFFF);
                    g.text(l, W / 2, y, size, true, 1);
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
