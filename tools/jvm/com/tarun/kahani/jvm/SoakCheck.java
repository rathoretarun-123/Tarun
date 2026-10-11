package com.tarun.kahani.jvm;
import com.tarun.kahani.core.*;
import java.io.File;
import java.nio.file.*;
import java.util.*;
/**
 * The v41 soak: directs every story of a folder (with the user's pictures when its cast is theirs) and checks what
 * must always hold: no crash, keys in time order, nothing NaN, acts that end after they start, every speaker on the
 * stage and in the picture's width while speaking, no walking while lying or seated off a vehicle, no camera flash
 * cuts or wild zooms, segments in order. SoakCheck storyDir famProj rahulProj menProj
 */
public class SoakCheck {
    static boolean together(Film.Actor a, Film.Actor b, float t) {
        for (Film.Actor x : new Film.Actor[]{a, b}) for (Film.Act ac : x.acts)
            if ((ac.type == Film.G_HUG || ac.type == Film.G_HOLD_HAND || ac.type == Film.G_RIDE || ac.type == Film.G_KISS || ac.type == Film.G_PLAYGROUND || ac.type == Film.G_GIVE)
                    && t >= ac.t0 - 1.0f && t <= ac.t1 + 1.0f) return true;
        return false;
    }
    static boolean playing(Film.Actor a, float t) { for (Film.Act x : a.acts) if (x.type == Film.G_PLAYGROUND && t >= x.t0 - 0.3f && t <= x.t1 + 0.3f) return true; return false; }
    static boolean bad(float f) { return Float.isNaN(f) || Float.isInfinite(f); }
    static int scored, shotsTotal, over6, overMotion, multi, stillWrong, corrected, reactions, povs, loud, estabHeld, estabTotal, longest, longShots;
    static float scoreSum;
    static Map<String, Integer> gatesFailed = new TreeMap<String, Integer>();
    public static void main(String[] a) throws Exception {
        File[] fs = new File(a[0]).listFiles((d, n) -> n.endsWith(".txt"));
        Arrays.sort(fs);
        Map<String, Integer> totals = new TreeMap<String, Integer>();
        int stories = 0, crashed = 0;
        float filmTime = 0;
        Map<String, Art> cache = new HashMap<String, Art>();
        for (File f : fs) {
            String script = new String(Files.readAllBytes(f.toPath()), "UTF-8");
            Map<String, Integer> issues = new TreeMap<String, Integer>();
            List<String> notes = new ArrayList<String>();
            stories++;
            try {
                Story st = ScriptParser.parse(script);
                if (st.cast().isEmpty()) { issues.put("no cast", 1); }
                if (st.scenes.isEmpty()) { issues.put("no scenes", 1); }
                String names = "";
                for (Story.CharacterDef c : st.cast()) names += c.fullName + " ";
                String proj = names.contains("तरुण") || names.contains("प्रिया") || names.contains("वृंदा") || names.contains("वनुशा") ? a[1]
                        : names.contains("राहुल") ? a[2] : names.contains("अर्जुन") || names.contains("कबीर ") || names.contains("समीर") ? a[3] : null;
                Art art = new Art();
                if (proj != null) {
                    Art.spriteSide = 600; Art.backdropSide = 800;
                    art = Art.fromManifest(new String(Files.readAllBytes(Paths.get(proj, "cast.txt")), "UTF-8"), st, new MakeFilm.AwtLoader(proj));
                    // v42: a picture folder that no longer loads (moved, cleaned away) is a failure, never a quiet doll film
                    if (art.sprites.isEmpty()) issues.merge("user pictures not loaded", 1, Integer::sum);
                }
                Director d = new Director(st, new Director.Options());
                d.prepare();
                Film film = d.direct(art);
                filmTime += film.duration;
                // the documents' measurable rules, as the director scores itself (Handbook ch. 13)
                if (film.stats != null) {
                    Handbook.Stats hs = film.stats;
                    Handbook.Card card = Handbook.score(film, hs, -1, -1, -1);
                    int sum = 0;
                    for (int v : card.score) sum += v;
                    scoreSum += sum / (float) card.score.length;
                    scored++;
                    for (int g = 0; g < card.gate.length; g++) if (!card.gate[g]) {
                        gatesFailed.merge(Handbook.GATES[g], 1, Integer::sum);
                        if (System.getenv("GATES") != null) System.out.println("GATE " + f.getName() + " " + Handbook.GATES[g] + " cast=" + st.cast().size() + " lines=" + film.lines.size() + " reactions=" + hs.reactions);
                    }
                    if (!card.critical.isEmpty()) { issues.merge("protocol critical", 1, Integer::sum); notes.addAll(card.critical); }
                    shotsTotal += hs.shots; over6 += hs.over6; overMotion += hs.overMotion; multi += hs.multi; stillWrong += hs.stillWrong;
                    corrected += hs.corrected; reactions += hs.reactions; povs += hs.pov; loud += hs.loudReactions;
                    estabHeld += hs.estabHeld; estabTotal += hs.estabTotal;
                    longest = Math.max(longest, hs.longest);
                }
                for (Film.Shot sh : film.shots) if (sh.dur > TechnicalDirector.MAX_SHOT_SECONDS + 0.05f) {
                    longShots++;
                    if (System.getenv("LONG") != null) System.out.println("LONG " + f.getName() + " t=" + sh.t + " dur=" + sh.dur + " type=" + sh.type + " size=" + sh.size + " " + sh.purpose + " | " + sh.action);
                }
                if (film.duration < 5 || film.duration > 3600 || bad(film.duration)) issues.merge("film length", 1, Integer::sum);
                if (film.shots.isEmpty()) issues.merge("no shots", 1, Integer::sum);
                float prevEnd = -1;
                for (Film.Seg sg : film.segs) {
                    if (sg.t1 < sg.t0) issues.merge("segment ends before it starts", 1, Integer::sum);
                    if (prevEnd > sg.t0 + 1.5f) issues.merge("segments overlap", 1, Integer::sum);
                    prevEnd = sg.t1;
                    for (Film.Actor ac : sg.actors) {
                        for (int i = 0; i < ac.keys.size(); i++) {
                            Film.Key k = ac.keys.get(i);
                            if (bad(k.t) || bad(k.x) || bad(k.moveDur) || bad(k.facing)) issues.merge("NaN key", 1, Integer::sum);
                            if (i > 0 && k.t < ac.keys.get(i - 1).t - 1e-3f) issues.merge("key out of order", 1, Integer::sum);
                            if (k.moveDur < 0) issues.merge("negative move", 1, Integer::sum);
                            if (k.moveDur > 0.3f && k.visible && k.body == Pose.LIE && !playing(ac, k.t) && Math.abs(k.x - Director.xAt(ac, k.t + 0.01f)) > 40) { issues.merge("moves while lying", 1, Integer::sum); if (notes.size() < 6) notes.add(ac.c.fullName + " moves lying at " + k.t); }
                            if (k.moveDur > 0.3f && k.visible && k.body == Pose.SIT && Math.abs(k.x - Director.xAt(ac, k.t + 0.01f)) > 40) {
                                boolean riding = false;
                                for (Film.Act x : ac.acts) if ((x.type == Film.G_RIDE || x.type == Film.G_PLAYGROUND) && k.t >= x.t0 - 0.5f && k.t <= x.t1 + 0.5f) riding = true;
                                if (!riding && ac.look != null && ac.look.aid != Look.AID_WHEELCHAIR) { issues.merge("walks seated", 1, Integer::sum); if (notes.size() < 6) notes.add(ac.c.fullName + " walks seated at " + k.t); }
                            }
                        }
                        // nobody stays on view beyond the stage's edges (a walk off is gone once it is out)
                        for (float tt = sg.t0 + 0.5f; tt < sg.t1 - 0.5f; tt += 1f) {
                            Film.Key kk = ac.stateAt(tt);
                            if (!kk.visible || kk.anchor != Film.A_GROUND) continue;
                            float xx = Director.xAt(ac, tt);
                            boolean moving = false;
                            for (Film.Key m : ac.keys) if (m.moveDur > 0 && tt >= m.t && tt <= m.t + m.moveDur + 0.6f) moving = true;
                            if ((xx < -150 || xx > 1430) && !moving) { issues.merge("lingers off the stage", 1, Integer::sum); if (notes.size() < 6) notes.add(String.format(Locale.US, "%s at x=%.0f t=%.1f", ac.c.fullName, xx, tt)); break; }
                        }
                        for (Film.Act x : ac.acts) {
                            if (bad(x.t0) || bad(x.t1)) issues.merge("NaN act", 1, Integer::sum);
                            if (x.t1 < x.t0) issues.merge("act ends before it starts", 1, Integer::sum);
                        }
                    }
                    Film.Cam prev = null;
                    for (Film.Cam c : sg.cams) {
                        if (bad(c.t) || bad(c.cx) || bad(c.cy) || bad(c.zoom)) issues.merge("NaN camera", 1, Integer::sum);
                        if (c.zoom < 0.9f || c.zoom > 4.6f) { issues.merge("wild zoom", 1, Integer::sum); if (notes.size() < 6) notes.add("zoom " + c.zoom + " at " + c.t); }
                        if (prev != null && c.ease == 0 && c.t - prev.t < 0.25f && c.t - prev.t > 1e-3f && Math.abs(c.zoom - prev.zoom) + Math.abs(c.cx - prev.cx) / 200 > 0.15f) {
                            issues.merge("flash cut", 1, Integer::sum); if (notes.size() < 6) notes.add(String.format(Locale.US, "flash cut %.2f→%.2f", prev.t, c.t)); }
                        prev = c;
                    }
                }
                // two people standing almost on top of each other (not hugging, holding hands, riding together) for a while
                for (Film.Seg sg : film.segs) {
                    for (int p = 0; p < sg.actors.size(); p++) for (int q = p + 1; q < sg.actors.size(); q++) {
                        Film.Actor A = sg.actors.get(p), B = sg.actors.get(q);
                        if (A.look == null || B.look == null || !A.look.isHumanoid() || !B.look.isHumanoid()) continue;
                        float run = 0;
                        for (float tt = sg.t0 + 0.25f; tt < sg.t1; tt += 0.25f) {
                            Film.Key ka = A.stateAt(tt), kb = B.stateAt(tt);
                            // (v42: one walking past in front of or behind the other — feet 10 px or more apart in
                            // depth — is no standing on each other; a stop there is)
                            float dl = Math.abs(Director.liftAt(A, tt) - Director.liftAt(B, tt));
                            boolean still = Math.abs(Director.xAt(A, tt) - Director.xAt(A, tt - 0.25f)) < 3 && Math.abs(Director.xAt(B, tt) - Director.xAt(B, tt - 0.25f)) < 3;
                            boolean close = ka.visible && kb.visible && ka.anchor == Film.A_GROUND && kb.anchor == Film.A_GROUND
                                    && Math.abs(Director.xAt(A, tt) - Director.xAt(B, tt)) < 45 && !together(A, B, tt)
                                    && Math.abs(ka.depth - kb.depth) < 0.2f && dl < 20 && (dl < 10 || still);
                            run = close ? run + 0.25f : 0;
                            if (run >= 1.5f) { issues.merge("stand on each other", 1, Integer::sum); if (notes.size() < 6) notes.add(String.format(Locale.US, "%s+%s t=%.1f", A.c.fullName, B.c.fullName, tt)); break; }
                        }
                    }
                }
                for (Film.Line l : film.lines) {
                    if (l.who == null || l.who.voiceOnly) continue;
                    Film.Seg in = null;
                    for (Film.Seg sg : film.segs) if (l.start >= sg.t0 - 1e-3f && l.start < sg.t1) in = sg;
                    if (in == null) { issues.merge("line outside the film", 1, Integer::sum); continue; }
                    Film.Actor sp = null;
                    for (Film.Actor ac : in.actors) if (ac.c == l.who) sp = ac;
                    if (sp == null) { issues.merge("speaker not in the scene", 1, Integer::sum); if (notes.size() < 6) notes.add(l.who.fullName + " not in scene at " + l.start); continue; }
                    float tm = l.start + Math.min(0.5f, l.dur * 0.5f);
                    Film.Key k = sp.stateAt(tm);
                    if (!k.visible) { issues.merge("speaks unseen", 1, Integer::sum); if (notes.size() < 6) notes.add(l.who.fullName + " unseen at " + tm); }
                    else {
                        float x = Director.xAt(sp, tm);
                        if (x < -60 || x > 1340) { issues.merge("speaks off the stage", 1, Integer::sum); if (notes.size() < 6) notes.add(l.who.fullName + " at x=" + (int) x + " t=" + tm); }
                        // the camera of that moment shows the one speaking (an over-the-shoulder or a reaction shot
                        // may hold on the listener for a part of a line: checked at the line's middle and end)
                        int seen = 0;
                        for (float u : new float[]{0.3f, 0.6f, 0.9f}) {
                            float tt = l.start + l.dur * u;
                            Film.Cam cc = null;
                            for (Film.Cam c : in.cams) if (c.t <= tt) cc = c;
                            if (cc == null) { seen++; continue; }
                            float half = 640f / cc.zoom, xs = Director.xAt(sp, tt);
                            if (xs > cc.cx - half + 20 && xs < cc.cx + half - 20) seen++;
                        }
                        if (seen == 0) { issues.merge("speaker never in the frame", 1, Integer::sum); if (notes.size() < 6) notes.add(String.format(Locale.US, "%s unframed %.1f-%.1f", l.who.fullName, l.start, l.start + l.dur)); }
                    }
                }
            } catch (Throwable e) {
                crashed++;
                issues.put("CRASH " + e, 1);
                StackTraceElement[] tr = e.getStackTrace();
                for (int i = 0; i < Math.min(4, tr.length); i++) notes.add(tr[i].toString());
            }
            for (Map.Entry<String, Integer> e : issues.entrySet()) totals.merge(e.getKey(), e.getValue(), Integer::sum);
            if (!issues.isEmpty()) System.out.println(f.getName() + ": " + issues + (notes.isEmpty() ? "" : "  " + notes));
        }
        System.out.printf(Locale.US, "SOAK %d stories, %d crashed, %.0f s of film; issues: %s%n", stories, crashed, filmTime, totals);
        System.out.printf(Locale.US, "PROTOCOL %d films scored, average %.2f / 5; gates not passed: %s; %d shots (%d over %.0f s), lip-sync shots over six words %d, "
                + "over the motion limit %d, two actions in a shot %d, corrected %d, still wrong %d, reactions %d, POV shots %d, reactions to loud sounds %d, "
                + "establishing shots held %d of %d%n", scored, scored == 0 ? 0 : scoreSum / scored, gatesFailed, shotsTotal, longShots, TechnicalDirector.MAX_SHOT_SECONDS,
                over6, overMotion, multi, corrected, stillWrong, reactions, povs, loud, estabHeld, estabTotal);
    }
}
