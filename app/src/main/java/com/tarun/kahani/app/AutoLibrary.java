package com.tarun.kahani.app;

import android.content.Context;

import com.tarun.kahani.core.Bible;
import com.tarun.kahani.core.Cloud;
import com.tarun.kahani.core.PicSense;
import com.tarun.kahani.core.ScriptAI;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.ShotBook;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.Txt;
import com.tarun.kahani.core.VoiceMatch;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * The director looks through the phone's library by itself before every film — no button needed. A character,
 * place, title or end page that has no picture yet gets the library picture that clearly fits it (same name,
 * the words of its description, or its look: dress, moustache, colours); a character without a voice sample
 * gets the saved voice that clearly matches the voice the script describes. Only confident matches are used,
 * and they are written into the story so the same picture and voice stay with the character every time.
 * Pictures placed in stories made before the library existed are taken into the library once.
 */
final class AutoLibrary {
    private AutoLibrary() {}

    static final float PICTURE_SURE = 0.66f, VOICE_SURE = 0.8f;
    /** v25: a picture without a name (a camera file name, nothing the user wrote) is placed only when its look fits this well — else the user is asked. */
    static final float LOOKS_ONLY_SURE = 0.82f;

    /** The key under which a picture remembers what the user said it is ("is:<key>") or is not ("not:<key>"), by the label's words. */
    static String labelKey(String label) {
        return Txt.norm(label == null ? "" : label).replace(';', ' ').replace('=', ' ').trim();
    }

    /** A file name a camera or a messenger gives (IMG_2024…, PXL_…, DSC…, Screenshot…, WhatsApp Image…): no name at all. */
    /** v33: two pictures that look like the same person or thing (the same colour mix and skin share). */
    static boolean alike(PicSense.Info a, PicSense.Info b) {
        if (a == null || b == null) return false;
        float dot = 0, na = 0, nb = 0;
        for (int i = 0; i < a.hue.length && i < b.hue.length; i++) { dot += a.hue[i] * b.hue[i]; na += a.hue[i] * a.hue[i]; nb += b.hue[i] * b.hue[i]; }
        float cos = na > 0 && nb > 0 ? dot / (float) Math.sqrt(na * nb) : 0;
        return cos >= 0.9f && Math.abs(a.skin - b.skin) < 0.12f && a.figure == b.figure;
    }

    static boolean cameraName(String name) {
        String n = name == null ? "" : name.trim();
        return n.length() == 0 || n.matches("(?i)^(img|pxl|dsc|dcim|image|photo|pic|screenshot|signal|whatsapp image|snapchat|camera|vid|mvimg|panorama)[-_ ]?.*")
                || n.matches("^[0-9_\\-. ]+$") || n.matches("(?i)^[0-9a-f]{8,}$");
    }

    /** Fills what is missing from the library; returns what was chosen ("" when nothing), for the job's notes. */
    static String fill(Context ctx, Project project, Story st) {
        Library lib = Library.get(ctx);
        List<String> notes = new ArrayList<String>();
        try { adoptOldStories(ctx, lib); } catch (Throwable ignored) {}
        Cloud cloud = null;
        try { if (Prefs.online(ctx)) cloud = Prefs.cloud(ctx); } catch (Throwable ignored) {}
        try { pictures(lib, project, st, notes, cloud); } catch (Throwable ignored) {}
        try { views(lib, project, st, notes); } catch (Throwable ignored) {}
        try { voices(lib, project, st, notes); } catch (Throwable ignored) {}
        StringBuilder b = new StringBuilder();
        for (String n : notes) b.append(b.length() > 0 ? ", " : "").append(n);
        return b.toString();
    }

    // ------------------------------------------------------------------ pictures

    /** Every picture this story still needs: {target, label, description}. */
    static List<String[]> missingTargets(Project project, Story st) {
        Set<Story.CharacterDef> haveChar = new HashSet<Story.CharacterDef>();
        Set<String> haveScene = new HashSet<String>();
        boolean title = false, end = false;
        for (String l : project.read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length < 3) continue;
            if (f[0].equals("char")) { Story.CharacterDef c = ScriptParser.resolve(st, f[1]); if (c != null) haveChar.add(c); }
            else if (f[0].equals("scene")) haveScene.add(f[1].replaceAll("[abc]$", ""));
            else if (f[0].equals("title")) title = true;
            else if (f[0].equals("end")) end = true;
        }
        List<String[]> t = new ArrayList<String[]>();
        for (Story.CharacterDef c : st.cast()) if (!haveChar.contains(c)) t.add(new String[]{"char:" + c.displayName, c.shown(), c.description});
        for (String[] p : Bible.places(st)) {
            boolean needed = false;
            for (Story.Scene sc : st.scenes) if (placeOf(sc, p[0]) && !haveScene.contains(String.valueOf(sc.number))) needed = true;
            if (needed) t.add(new String[]{"place:" + p[0], st.shown(p[0]), p[1]});
        }
        // insert shots: an object the action of a scene is about (a mirror, a bell, a letter) can be shown in
        // close-up from a library picture of it
        Set<String> haveShot = new HashSet<String>();
        for (String l : project.read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length >= 4 && f[0].equals("shot")) for (String k : f[2].split(",")) haveShot.add(f[1] + ":" + k.trim());
        }
        Set<String> objSeen = new HashSet<String>();
        for (Story.Scene sc : st.scenes) {
            for (Story.Beat bt : sc.beats) {
                if (bt.type != Story.Beat.DIRECTION) continue;          // what is done, not what is said
                for (String o : ShotBook.OBJECTS) {
                    if (!Txt.has(bt.text, o) || haveShot.contains(sc.number + ":" + o) || !objSeen.add(sc.number + ":" + o)) continue;
                    t.add(new String[]{"shot:" + sc.number + ":" + o, o + " (scene " + sc.number + ")", "close-up insert shot of the " + o + ": " + Bible.oneLine(bt.text)});
                }
            }
        }
        if (!title) t.add(new String[]{"title", "Title page", "title picture / movie poster of " + st.title});
        if (!end) t.add(new String[]{"end", "End page", "ending picture, calm landscape, sunset, the end"});
        return t;
    }

    /** The scene is set in this place (the same test the studio uses when the user places a picture). */
    static boolean placeOf(Story.Scene sc, String place) {
        String first = sc.setting.length() > 0 ? sc.setting : sc.title;
        return Bible.similar(place, first) || Txt.norm(first).contains(Txt.norm(place)) || Bible.similar(Bible.firstClauseOf(first), place);
    }

    /** How many library pictures the AI may look at per film (each takes a few seconds online). */
    static final int VISION_LOOKS = 30;

    static void pictures(Library lib, Project project, Story st, List<String> notes, Cloud cloud) throws Exception {
        List<String[]> targets = missingTargets(project, st);
        if (targets.isEmpty()) return;
        // every name in the story: a picture named after someone (or somewhere) else is theirs, not a look-alike
        List<String> allNames = new ArrayList<String>();
        for (Story.CharacterDef c : st.characters) { allNames.add(c.displayName); allNames.addAll(c.aliases); }
        for (String[] pl : Bible.places(st)) allNames.add(pl[0]);
        Set<String> wanted = new HashSet<String>();
        for (String[] t : targets) wanted.add(t[0].startsWith("char:") ? t[0].substring(5) : t[0].startsWith("place:") ? t[0].substring(6) : t[0]);
        Set<String> used = new HashSet<String>(java.util.Arrays.asList(project.setting("auto.pics", "").split(",")));
        List<Library.Item> pics = new ArrayList<Library.Item>();
        for (Library.Item it : lib.find(Library.PIC, null, null)) {
            if (it.builtIn || used.contains(it.id) || "view".equals(it.kind)) continue;     // a back or side view is never a front picture
            String who = ScriptAI.matchName(it.name, allNames);
            if (who != null) {
                boolean free = false;
                for (Story.CharacterDef c : st.characters) if ((c.displayName.equals(who) || c.aliases.contains(who)) && wanted.contains(c.displayName)) free = true;
                if (!free && !wanted.contains(who)) continue;
            }
            pics.add(it);
            if (pics.size() >= 400) break;     // the newest 400 pictures are plenty and keep this quick
        }
        if (pics.isEmpty()) return;
        List<String> labels = new ArrayList<String>();
        for (String[] t : targets) labels.add(t[1]);
        float[][] score = new float[pics.size()][targets.size()];
        for (int i = 0; i < pics.size(); i++) {
            Library.Item it = pics.get(i);
            PicSense.Info in = lib.info(it);
            PicSense.Traits tr = PicSense.Traits.fromMeta(it.meta);
            String byName = ScriptAI.matchName(it.name, labels);
            for (int t = 0; t < targets.size(); t++) {
                String tg = targets.get(t)[0];
                float f = 0;
                if (in != null) {
                    if (tg.startsWith("char:")) {
                        Story.CharacterDef c = ScriptParser.resolve(st, tg.substring(5));
                        // a person's picture never goes to a place and the other way round
                        if (c != null && !"place".equals(it.kind)) f = PicSense.matchCharacter(in, tr, c);
                    } else if (tg.startsWith("place:") && !"person".equals(it.kind)) f = PicSense.matchPlace(in, targets.get(t)[2]);
                }
                float s = f * 0.75f;
                float byText = PicSense.textMatch(it.name + " " + it.tags, targets.get(t)[1], targets.get(t)[2]);
                s = Math.max(s, byText);
                if (byName != null && byName.equals(targets.get(t)[1])) s = 1f;
                if ((tg.equals("title") || tg.equals("end")) && byName == null) s = Math.min(s, 0.5f);   // only when named so
                if (tg.startsWith("shot:") && ("person".equals(it.kind) || "place".equals(it.kind) || "view".equals(it.kind))) s = 0;   // a character, a place or a view is not an object (v33)
                // v25: a picture with no name and no words fitting the target is placed by its look only when very sure
                if (byName == null && byText < 0.5f && cameraName(it.name) && s < LOOKS_ONLY_SURE) s = 0;
                // v25: what the user said about this picture in any story wins over every guess
                String lk = labelKey(targets.get(t)[1]);
                if ("1".equals(it.meta("is:" + lk))) s = 1f;
                if (it.meta("sheet") != null && s > 0) s *= 0.9f;                                       // v32: a figure of a split sheet follows its main picture, never replaces it
                if ("1".equals(it.meta("not:" + lk))) s = 0;
                score[i][t] = s;
            }
        }
        // online: the AI looks at the pictures the offline analysis is unsure about (unnamed photos above all) and
        // says whom or what each shows; its answer is remembered with the picture, so it is asked only once
        if (cloud != null) {
            List<String[]> cands = new ArrayList<String[]>();
            for (String[] t : targets) cands.add(new String[]{t[1], t[2]});
            int looks = 0;
            for (int i = 0; i < pics.size() && looks < VISION_LOOKS; i++) {
                Library.Item it = pics.get(i);
                float top = 0;
                for (int t = 0; t < targets.size(); t++) top = Math.max(top, score[i][t]);
                if (top >= 0.95f) continue;
                String key = "ai:" + Integer.toHexString(labels.toString().hashCode());
                String known = it.meta(key);
                if (known == null) {
                    byte[] small = shrink(Project.readAll(lib.open(it)), 640);
                    if (small == null) continue;
                    looks++;
                    try {
                        ScriptAI.Seen seen = ScriptAI.look(cloud, small, cands);
                        if (seen.caption.length() > 0 && !it.tags.contains(seen.caption)) it.tags = (it.tags + ", " + seen.caption).replaceAll("^, ", "");
                        if (seen.realPhoto) it.setMeta("realphoto", "1");
                        known = seen.match == null ? "-" : seen.match;
                        it.setMeta(key, known);
                    } catch (Exception e) {
                        break;      // offline after all: the offline analysis decides
                    }
                }
                for (int t = 0; t < targets.size(); t++) {
                    if (targets.get(t)[1].equals(known)) score[i][t] = Math.max(score[i][t], 0.95f);
                    else if (it.tags.length() > 0) score[i][t] = Math.max(score[i][t], PicSense.textMatch(it.tags, targets.get(t)[1], targets.get(t)[2]));
                }
            }
        }
        lib.save();
        // the phone guide (§1.1, §14): never silently guess when two matches are plausible — a picture that fits two
        // targets almost alike, or a target that two pictures fit almost alike, is left for the Studio and named
        StringBuilder unsure = new StringBuilder();
        for (int i = 0; i < pics.size(); i++) {
            float b1 = 0, b2 = 0; int t1 = -1;
            for (int t = 0; t < targets.size(); t++) { float v = score[i][t]; if (v > b1) { b2 = b1; b1 = v; t1 = t; } else if (v > b2) b2 = v; }
            if (b1 >= PICTURE_SURE && b1 < 0.95f && b1 - b2 < 0.12f) {
                if (unsure.length() < 200) unsure.append(unsure.length() > 0 ? "; " : "").append('"').append(pics.get(i).label()).append("\" fits ").append(labels.get(t1)).append(" and another alike");
                for (int t = 0; t < targets.size(); t++) score[i][t] = 0;
            }
        }
        for (int t = 0; t < targets.size(); t++) {
            if (!targets.get(t)[0].startsWith("char:")) continue;      // two good pictures of one place: either is right
            float b1 = 0, b2 = 0;
            for (int i = 0; i < pics.size(); i++) { float v = score[i][t]; if (v > b1) { b2 = b1; b1 = v; } else if (v > b2) b2 = v; }
            if (b1 >= PICTURE_SURE && b1 < 0.95f && b1 - b2 < 0.08f) {
                if (unsure.length() < 200) unsure.append(unsure.length() > 0 ? "; " : "").append(labels.get(t)).append(": two pictures fit alike");
                for (int i = 0; i < pics.size(); i++) score[i][t] = 0;
            }
        }
        if (unsure.length() > 0) notes.add("not placed by itself (two matches alike — choose in the Studio): " + unsure);
        int[] best = PicSense.assign(score, PICTURE_SURE);
        for (int i = 0; i < pics.size(); i++) {
            if (best[i] < 0 || score[i][best[i]] < PICTURE_SURE) continue;
            String[] t = targets.get(best[i]);
            Library.Item it = pics.get(i);
            byte[] data = Project.readAll(lib.open(it));
            if (t[0].startsWith("char:") && SheetSaver.isSheet(data, false)) {
                // v29: a sheet of the character in the library is split into its figures, never used whole
                String key = t[0].substring(5);
                List<byte[]> one = new ArrayList<byte[]>();
                one.add(data);
                try {
                    SheetSaver.save(project, lib, st, SheetSaver.target("char", key, key), one, null);
                    notes.add(key + " ← the sheet \"" + it.label() + "\" split into its figures");
                } catch (Exception ignored) {
                }
                continue;
            }
            String f = project.savePicture(data, t[0].startsWith("char:") ? "char" : "pic");
            if (t[0].startsWith("char:")) {
                String key = t[0].substring(5);
                project.setManifest("char", key, "char|" + key + "|" + f);
            } else if (t[0].startsWith("shot:")) {
                String[] sk = t[0].split(":", 3);
                project.setManifest("shot", sk[1] + ":" + sk[2], "shot|" + sk[1] + "|" + sk[2] + "|" + f);
            } else if (t[0].equals("title") || t[0].equals("end")) {
                project.setManifest(t[0], t[0], t[0] + "|" + f + "|1");
            } else {
                String place = t[0].substring(6);
                for (Story.Scene sc : st.scenes) {
                    if (!placeOf(sc, place)) continue;
                    String k = String.valueOf(sc.number);
                    if (project.manifestLine("scene", k) != null) continue;
                    project.setManifest("scene", k, "scene|" + k + "|" + f);
                }
            }
            project.setSetting("auto.pic." + t[0], it.id);
            used.add(it.id);
            StringBuilder ub = new StringBuilder();
            for (String u : used) if (u.length() > 0) ub.append(ub.length() > 0 ? "," : "").append(u);
            project.setSetting("auto.pics", ub.toString());
            notes.add(t[1] + " ← picture \"" + it.label() + "\"");
        }
    }

    static byte[] shrink(byte[] data, int max) { return MainActivity.shrink(data, max); }

    // ------------------------------------------------------------------ views

    /**
     * A character whose picture came from the library takes the back and side views kept with that picture
     * (the app's own back views, or the views the user gave that character in an earlier story), so the
     * over-the-shoulder reverses and the walks use the real views instead of made ones.
     */
    static void views(Library lib, Project project, Story st, List<String> notes) {
        for (Story.CharacterDef c : st.cast()) {
            if (Studio3DArt.charFile(project, st, c) == null) continue;
            String key = Studio3DArt.keyFor(project, st, c);
            String[] have = Studio3DArt.viewFiles(project, key);
            String from = project.setting("auto.pic.char:" + c.displayName, project.setting("pic.char:" + c.displayName, ""));
            for (Library.Item it : lib.find(Library.PIC, null, null)) {
                if (!"view".equals(it.kind)) continue;
                float angle;
                try { angle = Float.parseFloat(it.meta("view") == null ? "180" : it.meta("view")); } catch (NumberFormatException e) { continue; }
                int idx = com.tarun.kahani.core.Figure3D.viewIndex(angle);
                if (idx < 0 || have[idx] != null) continue;
                boolean mine = from.length() > 0 && from.equals(it.meta("of"));
                if (!mine) { String of = it.meta("ofName"); mine = of != null && (of.equals(c.displayName) || c.aliases.contains(of)); }
                if (!mine) continue;
                try {
                    String f = project.savePicture(Project.readAll(lib.open(it)), "view");
                    Studio3DArt.setView(project, key, angle, "view|" + key + "|" + (int) angle + "|" + f);
                    project.setSetting("rejected3d.view." + key, "0");
                    have[idx] = f;
                    notes.add(c.shown() + " ← " + (idx == 2 ? "back" : idx == 1 ? "side" : "three-quarter") + " view \"" + it.label() + "\"");
                } catch (Exception ignored) {
                }
            }
            // v27: the pose pictures kept in the library with this character (angles, poses, feelings, as read or
            // corrected in an earlier story) become its pose lines here, so the shots are cast from them again
            if (Studio3DArt.poseLines(project, key).isEmpty()) {
                int n = 0;
                for (Library.Item it : lib.find(Library.PIC, null, null)) {
                    String tag = it.meta("posetag");
                    if (tag == null || tag.split("\\|").length < 4) continue;
                    boolean mine = from.length() > 0 && (from.equals(it.meta("of")) || from.equals(it.id));
                    if (!mine) { String of = it.meta("ofName"); mine = of != null && (of.equals(c.displayName) || c.aliases.contains(of)); }
                    if (!mine) continue;
                    try {
                        String f = project.savePicture(Project.readAll(lib.open(it)), "pose");
                        Studio3DArt.addPose(project, key, "pose|" + key + "|" + f + "|" + tag);
                        n++;
                    } catch (Exception ignored) {
                    }
                    if (n >= 100) break;
                }
                if (n > 0) notes.add(c.shown() + " ← " + n + " pose pictures (angles, poses, feelings) from the library");
            }
        }
    }

    // ------------------------------------------------------------------ voices

    static void voices(Library lib, Project project, Story st, List<String> notes) {
        Set<String> taken = new HashSet<String>();
        List<Story.CharacterDef> chars = new ArrayList<Story.CharacterDef>();
        for (Story.CharacterDef c : st.characters) {
            Library.Item cur = lib.byId(project.setting("vsample." + c.displayName, ""));
            if (cur != null) taken.add(cur.id);
            else if (project.setting("evoice." + c.displayName, "").length() == 0) chars.add(c);   // the user chose a voice: keep it
        }
        if (chars.isEmpty()) return;
        List<Library.Item> mine = new ArrayList<Library.Item>();
        List<float[]> feats = new ArrayList<float[]>();
        for (Library.Item it : lib.find(Library.VOICE, null, null)) {
            if (it.builtIn || taken.contains(it.id)) continue;
            float[] vf = Library.voiceFeatures(it);
            if (vf == null) continue;
            mine.add(it);
            feats.add(vf);
        }
        if (mine.isEmpty()) return;
        // a voice saved under a character's name is that character's voice (the best-fitting name in the whole cast)
        List<String> allNames = new ArrayList<String>();
        java.util.Map<String, Story.CharacterDef> owner = new java.util.HashMap<String, Story.CharacterDef>();
        for (Story.CharacterDef c : st.characters) {
            List<String> ns = new ArrayList<String>(c.aliases);
            ns.add(0, c.displayName);
            if (c.fullName != null) ns.add(c.fullName);
            for (String n : ns) if (n != null && n.length() > 0 && !owner.containsKey(n)) { owner.put(n, c); allNames.add(n); }
        }
        for (int k = 0; k < mine.size(); k++) {
            Library.Item it = mine.get(k);
            String hit = ScriptAI.matchName(it.name, allNames);
            Story.CharacterDef c = hit == null ? null : owner.get(hit);
            if (c == null || !chars.contains(c)) continue;
            project.setSetting("vsample." + c.displayName, it.id);
            project.setSetting("auto.voice." + c.displayName, "1");
            notes.add(c.shown() + " ← voice \"" + it.label() + "\"");
            mine.remove(k); feats.remove(k); chars.remove(c); k--;
        }
        if (mine.isEmpty() || chars.isEmpty()) return;
        List<VoiceMatch.Want> wants = new ArrayList<VoiceMatch.Want>();
        for (Story.CharacterDef c : chars) wants.add(VoiceMatch.want(c));
        int[] a = VoiceMatch.assign(feats, wants, VOICE_SURE);
        for (int i = 0; i < chars.size(); i++) {
            if (a[i] < 0) continue;
            Story.CharacterDef c = chars.get(i);
            Library.Item it = mine.get(a[i]);
            // a voice named after someone else stays theirs; otherwise only a clear match
            if (it.name.trim().length() > 0 && namedForAnother(it.name, st)) continue;
            if (VoiceMatch.score(feats.get(a[i]), wants.get(i)) < VOICE_SURE) continue;
            project.setSetting("vsample." + c.displayName, it.id);
            project.setSetting("auto.voice." + c.displayName, "1");
            notes.add(c.shown() + " ← voice \"" + it.label() + "\"");
        }
    }

    /** The voice's name is the name of a character of this story (who already has a voice). */
    static boolean namedForAnother(String name, Story st) {
        List<String> all = new ArrayList<String>();
        for (Story.CharacterDef c : st.characters) { all.add(c.displayName); all.addAll(c.aliases); }
        return ScriptAI.matchName(name, all) != null;
    }

    // ------------------------------------------------------------------ older stories

    /**
     * Pictures placed in stories made before the library existed (or placed directly in a story) are copied
     * into the library once, named after the character or place and described by that story's words.
     */
    static synchronized void adoptOldStories(Context ctx, Library lib) {
        File done = new File(lib.dir(), "adopted.txt");
        Set<String> seen = new HashSet<String>();
        String old = "";
        try { if (done.exists()) old = new String(AudioIO.readFile(done), "UTF-8"); } catch (Exception ignored) {}
        for (String l : old.split("\n")) if (l.length() > 0) seen.add(l);
        StringBuilder add = new StringBuilder();
        List<Long> known = null;     // fingerprints of the library's pictures, made once when needed
        for (Project p : Project.all(ctx)) {
            String cast = p.read("cast.txt");
            if (cast.trim().length() == 0) continue;
            Story st = null;
            for (String l : cast.split("\n")) {
                String[] f = l.split("\\|");
                if (f.length < 3 || !(f[0].equals("char") || f[0].equals("scene"))) continue;
                String key = p.dir.getName() + "/" + f[2];
                if (seen.contains(key)) continue;
                File pic = p.file(f[2]);
                seen.add(key);
                add.append(key).append('\n');
                if (!pic.exists() || pic.length() < 2000) continue;
                byte[] data;
                try { data = AudioIO.readFile(pic); } catch (Exception e) { continue; }
                // a picture that came from the library (or is already in it) is not added again
                long fp = fingerprint(data);
                if (known == null) known = fingerprints(lib);
                boolean dup = false;
                for (long k : known) if (Long.bitCount(k ^ fp) <= 5) { dup = true; break; }
                if (dup || fp == 0) continue;
                known.add(fp);
                if (st == null) {
                    try { st = ScriptParser.parse(p.read("script.txt")); } catch (Throwable e) { st = new Story(); }
                }
                String name = f[1], tags = "", kind;
                if (f[0].equals("char")) {
                    kind = "person";
                    Story.CharacterDef c = ScriptParser.resolve(st, f[1]);
                    if (c != null) { name = c.displayName; tags = c.description; }
                } else {
                    kind = "place";
                    String n = f[1].replaceAll("[abc]$", "");
                    for (Story.Scene sc : st.scenes) if (String.valueOf(sc.number).equals(n)) { name = Bible.firstClauseOf(sc.setting.length() > 0 ? sc.setting : sc.title); tags = sc.setting; }
                }
                if (tags.length() > 300) tags = tags.substring(0, 300);
                try {
                    Library.Item it = lib.addBytes(Library.PIC, kind, name, tags, data, ext(f[2]), "story: " + p.name());
                    it.setMeta("adopted", "1");
                    it.setMeta("fp", Long.toHexString(fp));
                } catch (Exception ignored) {
                }
            }
        }
        if (add.length() > 0) {
            try {
                java.io.FileOutputStream o = new java.io.FileOutputStream(done, true);
                o.write(add.toString().getBytes("UTF-8"));
                o.close();
            } catch (Exception ignored) {
            }
            lib.save();
        }
    }

    /** An 8×8 brightness fingerprint: the same picture (even re-saved smaller) gives nearly the same bits. */
    static long fingerprint(byte[] data) {
        android.graphics.Bitmap b = MainActivity.decodeSmall(data, 96);
        if (b == null) return 0;
        android.graphics.Bitmap s = android.graphics.Bitmap.createScaledBitmap(b, 8, 8, true);
        int[] px = new int[64];
        s.getPixels(px, 0, 8, 0, 0, 8, 8);
        if (s != b) s.recycle();
        b.recycle();
        float[] l = new float[64];
        float mean = 0;
        for (int i = 0; i < 64; i++) { int c = px[i]; l[i] = ((c >> 16) & 255) * 0.3f + ((c >> 8) & 255) * 0.59f + (c & 255) * 0.11f; mean += l[i] / 64; }
        long bits = 0;
        for (int i = 0; i < 64; i++) if (l[i] > mean) bits |= 1L << i;
        return bits == 0 ? 1 : bits;
    }

    /** Fingerprints of every picture in the library (measured once each and kept with the picture). */
    static List<Long> fingerprints(Library lib) {
        List<Long> out = new ArrayList<Long>();
        boolean changed = false;
        for (Library.Item it : lib.find(Library.PIC, null, null)) {
            if (it.builtIn) continue;
            String m = it.meta("fp");
            if (m == null) {
                try { m = Long.toHexString(fingerprint(Project.readAll(lib.open(it)))); } catch (Exception e) { m = "0"; }
                it.setMeta("fp", m);
                changed = true;
            }
            try { long v = new java.math.BigInteger(m, 16).longValue(); if (v != 0) out.add(v); } catch (Exception ignored) {}
        }
        if (changed) lib.save();
        return out;
    }

    static String ext(String file) {
        int k = file.lastIndexOf('.');
        return k > 0 ? file.substring(k) : ".jpg";
    }
}
