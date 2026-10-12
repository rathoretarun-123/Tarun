package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;

import java.nio.file.*;
import java.util.*;

/**
 * v46: the user's ten सुंदरपुर गाँव scripts (tools/testdata/v46stories/s01..s10), each directed and checked for what the
 * script says happens — on the screen, not only in the words: the cow stuck in the mud and pulled out by everyone, the
 * monkey's mango, everyone sitting under the peepal tree, the cow brought in out of the rain, the lost calf found,
 * the race, the fair's flags and the rangoli, the heavy water pot and the help with it, the cap snatched and given
 * back, the road cleaned.
 *
 * Run: java -cp ... com.tarun.kahani.jvm.Sundar46Check tools/testdata/v46stories
 */
public class Sundar46Check {

    static Film.Actor who(Film.Seg s, String name) {
        for (Film.Actor a : s.actors) if (a.c.displayName.equals(name)) return a;
        return null;
    }

    static boolean act(Film.Actor a, int type) {
        if (a != null) for (Film.Act x : a.acts) if (x.type == type) return true;
        return false;
    }

    static boolean holds(Film.Actor a, int item) {
        if (a != null) for (Film.Key k : a.keys) if (k.holdR == item || k.holdL == item) return true;
        return false;
    }

    static Film.Fx fx(Film.Seg s, int type) {
        for (Film.Fx f : s.fx) if (f.type == type) return f;
        return null;
    }

    static int count(Film.Seg s, int type) {
        int n = 0;
        for (Film.Fx f : s.fx) if (f.type == type) n++;
        return n;
    }

    /** The problems found in story n's film (none: it is staged as written). */
    public static List<String> check(int n, Story st, Film film) {
        List<String> bad = new ArrayList<String>();
        Film.Seg s = null;
        for (Film.Seg g : film.segs) if (g.type == Film.S_SCENE) { s = g; break; }
        if (s == null) { bad.add("no scene"); return bad; }
        if (st.characters.size() != 7) bad.add("cast of " + st.characters.size() + ", not 7");
        for (Story.CharacterDef c : st.characters) {
            if (c.displayName.equals("चिंकी") && c.look.kind != Look.MONKEY) bad.add("चिंकी is no monkey");
            if (c.displayName.equals("गौरी") && (c.look.kind != Look.ANIMAL || c.look.species != Look.SP_COW)) bad.add("गौरी is no cow");
            if (c.displayName.equals("सीता") && !c.look.female) bad.add("सीता is not a woman");
            if (c.displayName.equals("अनाया") && c.look.kind != Look.GIRL) bad.add("अनाया is not a girl");
            if (c.displayName.equals("आरव") && c.look.kind != Look.BOY) bad.add("आरव is not a boy");
            if (c.displayName.equals("दादाजी") && c.look.kind != Look.OLD_MAN) bad.add("दादाजी is not an old man");
        }
        switch (n) {
            case 1: {
                Film.Fx mud = fx(s, Film.FX_MUD);
                if (mud == null || mud.a == null || !mud.a.c.displayName.equals("गौरी")) bad.add("गौरी not stuck in mud");
                else if (mud.t2 <= mud.t0) bad.add("गौरी never pulled out");
                int pullers = 0;
                for (Film.Actor a : s.actors) if (act(a, Film.G_PULL)) pullers++;
                if (pullers < 5) bad.add("only " + pullers + " pull (all five should)");
                if (fx(s, Film.FX_ROPE) == null) bad.add("no rope");
                Film.Actor dada = who(s, "दादाजी"), an = who(s, "अनाया");
                if (!act(an, Film.G_WAVE)) bad.add("अनाया does not call दादाजी");
                // he comes over — or, with no room beside her, turns to her where he stands
                if (dada != null && an != null) {
                    float call = -1;
                    for (Film.Act x : an.acts) if (x.type == Film.G_WAVE) call = x.t0;
                    boolean came = Math.abs(dada.keys.get(0).x - Director.xAt(dada, s.t1 - 1)) >= 40, turned = false;
                    for (Film.Key k : dada.keys) if (call >= 0 && k.t >= call && k.t <= call + 3 && Math.signum(k.facing) == Math.signum(Director.xAt(an, k.t) - Director.xAt(dada, k.t))) turned = true;
                    if (!came && !turned) bad.add("दादाजी neither comes over nor turns to अनाया");
                }
                // pulled out after the count
                Film.Line count = null;
                for (Film.Line l : film.lines) if (l.text != null && l.text.contains("तीन")) count = l;
                if (mud != null && count != null && mud.t2 < count.start + count.dur - 0.05f) bad.add("out before the count ends");
                break;
            }
            case 2: {
                Film.Actor ch = who(s, "चिंकी"), si = who(s, "सीता");
                if (!holds(ch, Pose.I_MANGO)) bad.add("चिंकी holds no mango");
                boolean ran = false;
                if (ch != null) for (Film.Key k : ch.keys) if (k.run) ran = true;
                if (!ran) bad.add("चिंकी does not run off");
                boolean gave = false;
                if (si != null) for (Film.Act x : si.acts) if (x.type == Film.G_GIVE && x.target == ch && x.item == Pose.I_MANGO) gave = true;
                if (!gave) bad.add("सीता gives चिंकी no mango");
                if (act(who(s, "रमेश"), Film.G_EAT)) bad.add("a generic line made someone eat");
                break;
            }
            case 3: {
                int sit = 0;
                for (Film.Actor a : s.actors) if (a.look.isHumanoid() && a.keys.get(0).body == Pose.SIT) sit++;
                if (sit < 4) bad.add("only " + sit + " seated under the peepal from the start");
                Film.Actor dada = who(s, "दादाजी");
                if (dada != null && dada.keys.get(0).seat != Film.SEAT_STOOL) bad.add("दादाजी not on a stool");
                break;
            }
            case 4: {
                Film.Fx shed = fx(s, Film.FX_SHED);
                if (shed == null) bad.add("no thatched shelter");
                boolean rain = false;
                for (Film.Weather w : film.weather) if (w.type == Film.W_RAIN) rain = true;
                if (!rain) bad.add("no rain");
                Film.Actor cow = who(s, "गौरी");
                if (cow == null) bad.add("गौरी not on the stage");
                else if (shed != null) {
                    if (Math.abs(cow.keys.get(0).x - shed.x) < shed.color / 2f) bad.add("गौरी starts under the roof");
                    if (Math.abs(Director.xAt(cow, s.t1 - 0.5f) - shed.x) > shed.color / 2f) bad.add("गौरी never brought under the roof");
                }
                for (Film.Actor a : s.actors) if (a.look.isHumanoid() && act(a, Film.G_COVER_HEAD) && shed != null && Math.abs(a.keys.get(0).x - shed.x) < shed.color / 2f - 30) {
                    boolean outLater = false;
                    for (Film.Act x : a.acts) if (x.type == Film.G_COVER_HEAD && Math.abs(Director.xAt(a, x.t0) - shed.x) > shed.color / 2f) outLater = true;
                    if (!outLater) bad.add(a.c.displayName + " covers the head under the roof");
                }
                Film.Actor ar = who(s, "आरव");
                if (ar == null || ar.umbrellaFrom < 0) bad.add("आरव brings no umbrella");
                break;
            }
            case 5: {
                Film.Fx calf = null;
                for (Film.Fx f : s.fx) if (f.type == Film.FX_ANIMAL && f.y == 3) calf = f;
                if (calf == null) bad.add("no calf found");
                else if (calf.b == null || !calf.b.c.displayName.equals("गौरी")) bad.add("the calf does not go to गौरी");
                int search = 0;
                for (Film.Actor a : s.actors) if (act(a, Film.G_SHIELD_EYES)) search++;
                if (search < 2) bad.add("no search of the fields");
                Film.Actor cow = who(s, "गौरी");
                boolean sad = false, happy = false;
                if (cow != null) for (Film.Key k : cow.keys) { if (k.emotion == Pose.SAD) sad = true; if (sad && k.emotion == Pose.HAPPY) happy = true; }
                if (!sad) bad.add("गौरी not sad");
                if (!happy) bad.add("गौरी not happy when the calf is found");
                break;
            }
            case 6: {
                Film.Actor ar = who(s, "आरव"), an = who(s, "अनाया"), dada = who(s, "दादाजी");
                for (Film.Actor a : new Film.Actor[]{ar, an}) {
                    boolean ran = false;
                    if (a != null) for (Film.Key k : a.keys) if (k.run && k.moveDur > 1.5f) ran = true;
                    if (!ran) bad.add((a == null ? "?" : a.c.displayName) + " does not race");
                }
                // the race ends short of दादाजी, who stands watching
                if (dada != null) for (Film.Actor a : new Film.Actor[]{ar, an}) if (a != null) for (Film.Key k : a.keys) if (k.run && k.moveDur > 1.5f) {
                    float te = k.t + k.moveDur + 0.2f;
                    if (Math.abs(Director.xAt(a, te) - Director.xAt(dada, te)) < 60) bad.add(a.c.displayName + " ends the race on दादाजी's spot");
                }
                if (!holds(ar, Pose.I_GRASS) || !holds(an, Pose.I_GRASS)) bad.add("they do not bring the grass together");
                break;
            }
            case 7: {
                Film.Fx b = fx(s, Film.FX_BUNTING);
                if (b == null || b.a == null || !b.a.c.displayName.equals("आरव")) bad.add("आरव hangs no flags");
                boolean rangoli = false;
                Film.Actor an = who(s, "अनाया");
                if (an != null) for (Film.Act x : an.acts) if (x.type == Film.G_TASK && x.item == Film.T_RANGOLI) rangoli = true;
                if (!rangoli) bad.add("अनाया makes no rangoli");
                break;
            }
            case 8: {
                Film.Actor si = who(s, "सीता"), ra = who(s, "रमेश");
                if (!holds(si, Pose.I_POT)) bad.add("सीता carries no pot");
                if (!holds(ra, Pose.I_POT)) bad.add("रमेश does not take the pot");
                if (!holds(who(s, "अनाया"), Pose.I_POT)) bad.add("अनाया brings no small pot");
                if (!holds(who(s, "आरव"), Pose.I_POT)) bad.add("आरव (\"मैं भी!\") brings no pot");
                if (si != null && si.keys.get(0).x > 0 && si.keys.size() > 1 && si.keys.get(1).x > -50) bad.add("सीता does not come from the well");
                break;
            }
            case 9: {
                Film.Actor dada = who(s, "दादाजी"), ch = who(s, "चिंकी");
                if (dada == null || dada.look.headwear != Look.HW_CAP) bad.add("दादाजी wears no cap");
                boolean off = false, on = false, mOn = false, mOff = false;
                if (dada != null) for (Film.Key k : dada.keys) { if (k.noHeadwear) off = true; else if (off) on = true; }
                if (ch != null) for (Film.Key k : ch.keys) { if (k.wearsTurban) mOn = true; else if (mOn) mOff = true; }
                if (!off || !mOn) bad.add("the cap is not snatched");
                if (!on || !mOff) bad.add("the cap is not given back");
                if (!act(who(s, "अनाया"), Film.G_WAVE)) bad.add("अनाया does not wave");
                break;
            }
            case 10: {
                int pieces = count(s, Film.FX_LITTER), cleared = 0;
                for (Film.Fx f : s.fx) if (f.type == Film.FX_LITTER && f.t1 < s.t1) cleared++;
                if (pieces < 5) bad.add("no rubbish on the road");
                if (cleared < pieces) bad.add((pieces - cleared) + " pieces of rubbish left");
                Film.Actor si = who(s, "सीता"), ra = who(s, "रमेश");
                boolean swept = false;
                if (si != null) for (Film.Act x : si.acts) if (x.type == Film.G_TASK && x.item == Film.T_SWEEP) swept = true;
                if (!swept || !holds(si, Pose.I_BROOM)) bad.add("सीता does not sweep with a broom");
                boolean picked = false;
                if (ra != null) for (Film.Key k : ra.keys) if (k.body == Pose.CROUCH) picked = true;
                if (!picked) bad.add("रमेश picks nothing up");
                break;
            }
            default:
        }
        // nobody stands on another (two people on one spot for more than a moment, after the first second)
        for (int i = 0; i < s.actors.size(); i++) for (int j = i + 1; j < s.actors.size(); j++) {
            Film.Actor a = s.actors.get(i), b = s.actors.get(j);
            if (!a.look.isHumanoid() || !b.look.isHumanoid()) continue;
            float run = 0;
            for (float t = s.t0 + 1; t < s.t1; t += 0.1f) {
                boolean both = a.stateAt(t).visible && b.stateAt(t).visible && Math.abs(Director.xAt(a, t) - Director.xAt(b, t)) < 30;
                run = both ? run + 0.1f : 0;
                if (run > 1.2f) { bad.add(a.c.displayName + " and " + b.c.displayName + " on one spot at " + String.format(Locale.US, "%.1f", t)); break; }
            }
        }
        return bad;
    }

    public static void main(String[] args) throws Exception {
        Path dir = Paths.get(args.length > 0 ? args[0] : "tools/testdata/v46stories");
        int ok = 0, n = 0;
        for (int i = 1; i <= 10; i++) {
            Path f = dir.resolve(String.format(Locale.US, "s%02d.txt", i));
            if (!Files.exists(f)) continue;
            n++;
            Story st = ScriptParser.parse(new String(Files.readAllBytes(f), "UTF-8"));
            Director d = new Director(st, new Director.Options());
            d.prepare();
            Film film = d.direct(new Art());
            List<String> bad = check(i, st, film);
            if (bad.isEmpty()) ok++;
            System.out.println((bad.isEmpty() ? "PASS " : "FAIL ") + f.getFileName() + " " + st.title + (bad.isEmpty() ? "" : "  " + bad) + "  (deeds " + d.spokenDeeds + ", village " + d.villageStaged + ")");
        }
        System.out.println("SUNDARPUR " + ok + " of " + n);
    }
}
