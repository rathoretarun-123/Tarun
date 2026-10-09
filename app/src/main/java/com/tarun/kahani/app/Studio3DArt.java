package com.tarun.kahani.app;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Paint;

import com.tarun.kahani.core.Cloud;
import com.tarun.kahani.core.Cutout;
import com.tarun.kahani.core.Doll3D;
import com.tarun.kahani.core.Edits;
import com.tarun.kahani.core.Figure3D;
import com.tarun.kahani.core.Glb;
import com.tarun.kahani.core.ImageTo3D;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.SceneMaker;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Set3D;
import com.tarun.kahani.core.Sets;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.StyleCue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Studio 3D on the phone: the director's own three-dimensional pictures.
 *
 * <ul>
 * <li>A character without a picture gets a doll built from its description (Doll3D), lit and graded on the
 *     line of the user's own pictures (StyleCue); a place without a picture gets a 3D set (Set3D).</li>
 * <li>A character with a picture gets its views — three-quarter, side in mid-stride, back — made from that
 *     very picture (Figure3D), or from a 3D model an image-to-3D service built from it (ImageTo3D + Glb) when
 *     the user has entered a key. The user can also upload a back or side picture of their own.</li>
 * <li>Every picture the studio makes is a proposal (propose| lines in the manifest, scored by the scene maker's
 *     rubric) until the user accepts it where pictures are chosen; a rejected proposal is deleted and never
 *     used. Accepted pictures go into the story's manifest and the app's own library (tarunkahani).</li>
 * </ul>
 */
final class Studio3DArt {
    private Studio3DArt() {}

    interface Progress { void at(String what); }

    static final String P_CHAR = "char", P_SCENE = "scene", P_VIEW = "view";

    // ------------------------------------------------------------------ the style of the user's pictures

    /** Reads the style cue from the pictures the story already has (characters and places). */
    static StyleCue styleCue(Project project, Story story) {
        StyleCue cue = new StyleCue();
        for (String line : project.read("cast.txt").split("\n")) {
            String[] f = line.trim().split("\\|");
            try {
                if (f.length >= 3 && f[0].equals("char") && project.has(f[2])) {
                    Story.CharacterDef c = ScriptParser.resolve(story, f[1]);
                    boolean beast = c != null && c.look != null && (c.look.kind == Look.ANIMAL || c.look.kind == Look.BIRD);
                    int[] d = project.loader().decode(f[2], 700);
                    if (d == null) continue;
                    int[] px = new int[d[0] * d[1]];
                    System.arraycopy(d, 2, px, 0, px.length);
                    Cutout.Result r = Cutout.process(px, d[0], d[1], beast);
                    cue.add(r.px, r.w, r.h, true);
                } else if (f.length >= 3 && f[0].equals("scene") && project.has(f[2])) {
                    int[] d = project.loader().decode(f[2], 700);
                    if (d == null) continue;
                    int[] px = new int[d[0] * d[1]];
                    System.arraycopy(d, 2, px, 0, px.length);
                    cue.add(px, d[0], d[1], false);
                }
            } catch (Throwable ignored) {
                // one unreadable picture never stops the cue
            }
        }
        return cue;
    }

    // ------------------------------------------------------------------ keys and manifest helpers

    /** The manifest key of a character (the alias its lines use, else the display name). */
    static String keyFor(Project project, Story story, Story.CharacterDef c) {
        for (String l : project.read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length >= 3 && (f[0].equals("char") || f[0].equals("view") || f[0].equals("propose")) ) {
                String k = f[0].equals("propose") ? (f.length >= 4 ? f[2] : "") : f[1];
                if (k.length() > 0 && ScriptParser.resolve(story, k) == c) return k;
            }
        }
        return c.displayName;
    }

    /** The file of a character's picture, or null. */
    static String charFile(Project project, Story story, Story.CharacterDef c) {
        for (String l : project.read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length >= 3 && f[0].equals("char") && ScriptParser.resolve(story, f[1]) == c && project.has(f[2])) return f[2];
        }
        return null;
    }

    /** The manifest's face points of a character ({mouthX, mouthY, mouthHW, eyeLX, eyeLY, eyeRX, eyeRY, eyeR, turbanY}), or null. */
    static float[] facePoints(Project project, Story story, Story.CharacterDef c) {
        for (String l : project.read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length >= 11 && f[0].equals("char") && ScriptParser.resolve(story, f[1]) == c) {
                try {
                    float[] p = new float[9];
                    for (int i = 0; i < 8; i++) p[i] = Float.parseFloat(f[3 + i].trim());
                    p[8] = f.length >= 12 ? Float.parseFloat(f[11].trim()) : 0;
                    return p[0] > 0 ? p : null;
                } catch (NumberFormatException e) { return null; }
            }
        }
        return null;
    }

    /** The view files of a character by view index (Figure3D.VIEW_ANGLES), null where there is none. */
    static String[] viewFiles(Project project, String key) {
        String[] out = new String[Figure3D.VIEW_ANGLES.length];
        for (String l : project.read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length >= 4 && f[0].equals("view") && f[1].equals(key) && project.has(f[3])) {
                try { int i = Figure3D.viewIndex(Float.parseFloat(f[2].trim())); if (i >= 0) out[i] = f[3]; } catch (NumberFormatException ignored) {}
            }
        }
        return out;
    }

    static boolean hasViews(Project project, String key) { for (String f : viewFiles(project, key)) if (f != null) return true; return false; }

    /** Every view (three-quarter, side, back) is there. */
    static boolean allViews(Project project, String key) { for (String f : viewFiles(project, key)) if (f == null) return false; return true; }

    /** Replaces (or removes with line == null) the view line of a character at an angle. */
    static void setView(Project project, String key, float angle, String line) {
        StringBuilder sb = new StringBuilder();
        for (String l : project.read("cast.txt").split("\n")) {
            if (l.trim().length() == 0) continue;
            String[] f = l.split("\\|");
            boolean match = f.length >= 4 && f[0].equals("view") && f[1].equals(key) && sameAngle(f[2], angle);
            if (match) { if (project.has(f[3]) && line == null) project.file(f[3]).delete(); continue; }
            sb.append(l).append('\n');
        }
        if (line != null) sb.append(line).append('\n');
        project.write("cast.txt", sb.toString());
    }

    static void removeViews(Project project, String key) { for (float a : Figure3D.VIEW_ANGLES) setView(project, key, a, null); }

    private static boolean sameAngle(String s, float angle) {
        try { return Math.abs(Float.parseFloat(s.trim()) - angle) < 1; } catch (NumberFormatException e) { return false; }
    }

    // ------------------------------------------------------------------ proposals

    /**
     * Every proposal in the manifest: {"propose", kind, key, file, …}. kind char: key, file, 8 face points,
     * turbanY, score, verdict. kind scene: key (number), file, 0, 0, 1, 1, ground, score, verdict.
     * kind view: key, angle, file, 8 face points (0 = unknown), score, verdict.
     */
    static List<String[]> proposals(Project project) {
        List<String[]> out = new ArrayList<String[]>();
        for (String l : project.read("cast.txt").split("\n")) {
            String[] f = l.trim().split("\\|");
            if (f.length >= 4 && f[0].equals("propose")) out.add(f);
        }
        return out;
    }

    static String[] proposalFor(Project project, String kind, String key) {
        for (String[] f : proposals(project)) if (f[1].equals(kind) && f[2].equals(key)) return f;
        return null;
    }

    static List<String[]> viewProposals(Project project, String key) {
        List<String[]> out = new ArrayList<String[]>();
        for (String[] f : proposals(project)) if (f[1].equals(P_VIEW) && f[2].equals(key)) out.add(f);
        return out;
    }

    /** The file of a proposal line. */
    static String fileOf(String[] f) { return f[1].equals(P_VIEW) ? f[4] : f[3]; }

    /** The verdict (score and status) of a proposal line. */
    static String verdictOf(String[] f) { return f[f.length - 1].startsWith("Score") ? f[f.length - 1] : ""; }

    private static void addProposal(Project project, String line) {
        project.write("cast.txt", project.read("cast.txt") + (project.read("cast.txt").endsWith("\n") || project.read("cast.txt").length() == 0 ? "" : "\n") + line + "\n");
    }

    /** Drops the proposal lines that match (kind, key[, angle]) and deletes their files when delete is set. */
    static void dropProposals(Project project, String kind, String key, String angle, boolean delete) {
        StringBuilder sb = new StringBuilder();
        for (String l : project.read("cast.txt").split("\n")) {
            if (l.trim().length() == 0) continue;
            String[] f = l.trim().split("\\|");
            boolean match = f.length >= 4 && f[0].equals("propose") && f[1].equals(kind) && f[2].equals(key) && (angle == null || (f[1].equals(P_VIEW) && sameAngle(f[3], Float.parseFloat(angle))));
            if (match) { if (delete && project.has(fileOf(f))) project.file(fileOf(f)).delete(); continue; }
            sb.append(l).append('\n');
        }
        project.write("cast.txt", sb.toString());
    }

    /** Whether the user rejected a 3D proposal of this kind and key before (it is not proposed again by itself). */
    static boolean rejected(Project project, String kind, String key) { return "1".equals(project.setting("rejected3d." + kind + "." + key, "0")); }

    /**
     * The user accepts a proposal: it becomes the character's picture / the place's background / a view and goes
     * into the app's own library. A character's doll takes its proposed views along.
     */
    static void accept(Project project, Library lib, Context ctx, String[] f) {
        String kind = f[1], key = f[2];
        if (kind.equals(P_CHAR)) {
            StringBuilder line = new StringBuilder("char|" + key + "|" + f[3]);
            for (int i = 4; i < 13 && i < f.length; i++) line.append('|').append(f[i]);
            project.setManifest("char", key, line.toString());
            dropProposals(project, P_CHAR, key, null, false);
            library(lib, ctx, project, f[3], "person", key, "3D doll (studio)");
            for (String[] v : viewProposals(project, key)) accept(project, lib, ctx, v);
        } else if (kind.equals(P_SCENE)) {
            project.setManifest("scene", key + "a", null);
            project.setManifest("scene", key + "b", null);
            project.setManifest("scene", key, "scene|" + key + "|" + f[3] + "|0|0|1|1|" + (f.length > 8 ? f[8] : "0.9"));
            dropProposals(project, P_SCENE, key, null, false);
            library(lib, ctx, project, f[3], "place", "part " + key, "3D place (studio)");
        } else if (kind.equals(P_VIEW)) {
            float angle = Float.parseFloat(f[3].trim());
            StringBuilder line = new StringBuilder("view|" + key + "|" + f[3] + "|" + f[4]);
            for (int i = 5; i < 13 && i < f.length; i++) line.append('|').append(f[i]);
            setView(project, key, angle, line.toString());
            dropProposals(project, P_VIEW, key, f[3], false);
            int vi = Figure3D.viewIndex(angle);
            library(lib, ctx, project, f[4], "person", key + " — " + (vi >= 0 ? Figure3D.VIEW_NAMES[vi] : "view"), "3D view from the picture (studio)");
        }
        project.setSetting("accepted3d." + kind + "." + key, "1");
    }

    /** The user rejects a proposal: its file is deleted, it is never used, and not proposed again by itself. */
    static void reject(Project project, String[] f) {
        String kind = f[1], key = f[2];
        dropProposals(project, kind, key, kind.equals(P_VIEW) ? f[3] : null, true);
        if (kind.equals(P_CHAR)) dropProposals(project, P_VIEW, key, null, true);
        if (kind.equals(P_CHAR) && f[f.length - 1].contains("free model")) {
            // the free model is turned down: the studio's own doll is proposed the next time instead
            project.setSetting("rejected3d.model." + key, "1");
            project.setSetting("credit3d." + key, "");
            return;
        }
        project.setSetting("rejected3d." + kind + "." + key, "1");
    }

    static void decideAll(Project project, Library lib, Context ctx, boolean accept) {
        for (String[] f : proposals(project)) { if (accept) accept(project, lib, ctx, f); else reject(project, f); }
    }

    private static void library(Library lib, Context ctx, Project project, String file, String kind, String name, String source) {
        if (lib == null) return;
        try {
            byte[] data = Project.readAll(new java.io.FileInputStream(project.file(file)));
            lib.addBytes(Library.PIC, kind, name, name, data, file.endsWith(".png") ? ".png" : ".jpg", source);
        } catch (Throwable ignored) {
            // the story keeps its picture; the library copy is an extra
        }
    }

    // ------------------------------------------------------------------ making pictures

    /**
     * The picture of a character without one: a doll built from its description, on the line of the user's
     * pictures, with its three views; a proposal when ask is set, else used at once. Returns the file name.
     */
    static String makeCharacter(Project project, Story story, Story.CharacterDef c, Library lib, Context ctx, StyleCue cue, boolean ask, Cloud cloud, boolean freeModels) throws IOException {
        Look look = c.look != null ? c.look : new Look();
        int seed = Math.abs(c.displayName.hashCode()) % 1000;
        String key = keyFor(project, story, c);
        // a free model from GitHub that the description fits (a knight, a mage, a fox…) before the studio's own doll
        if (freeModels && cloud != null && !rejected(project, "model", key)) {
            try { if (freeModel(project, story, c, lib, ctx, cue, ask, cloud, null)) return proposalFor(project, P_CHAR, key) != null ? proposalFor(project, P_CHAR, key)[3] : charFile(project, story, c); }
            catch (Throwable e) { android.util.Log.w("Kahani", "free 3D model: " + e); }
        }
        // the phone guide (§7.2) and item 6: the doll takes its reference from the user's pictures — the nearest
        // uploaded picture that fits the description lends its clothing colours and hair; the style cue its light and skin
        String[] refNote = {""};
        look = referenceLook(look, c, lib, refNote);
        Doll3D.Result r = Doll3D.make(look, 1100, seed, 0, com.tarun.kahani.core.Pose.NEUTRAL, cue);
        String file = project.savePicture(encode(r.px, r.w, r.h, true), "3d_char");
        int[] ratings = SceneMaker.ratings(false, r.faceKnown, true, cue != null && cue.pictures > 0, 0.8f, true);
        String verdict = SceneMaker.verdict(ratings, refNote[0]);
        String points = String.format(Locale.US, "%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f", r.mouthX, r.mouthY, r.mouthHW, r.eyeLX, r.eyeLY, r.eyeRX, r.eyeRY, r.eyeR, r.turbanY);
        dropProposals(project, P_CHAR, key, null, true);
        dropProposals(project, P_VIEW, key, null, true);
        addProposal(project, "propose|char|" + key + "|" + file + "|" + points + "|" + SceneMaker.score(ratings) + "|" + verdict);
        // the doll's own views (a doll turns exactly)
        boolean beast = look.kind == Look.ANIMAL || look.kind == Look.BIRD;
        String[] own = viewFiles(project, key);
        if (!beast) for (int i = 0; i < Figure3D.VIEW_ANGLES.length; i++) {
            if (own[i] != null) continue;          // the user's own back or side picture is the best view there is
            Doll3D.Result v = Doll3D.make(look, 1100, seed, Figure3D.VIEW_ANGLES[i], com.tarun.kahani.core.Pose.NEUTRAL, cue);
            String vf = project.savePicture(encode(v.px, v.w, v.h, true), "view");
            addProposal(project, "propose|view|" + key + "|" + (int) Figure3D.VIEW_ANGLES[i] + "|" + vf + "|" + viewPoints(v) + "|" + SceneMaker.score(ratings) + "|" + verdict);
        }
        if (!ask) accept(project, lib, ctx, proposalFor(project, P_CHAR, key));
        return file;
    }

    private static String viewPoints(Doll3D.Result v) {
        if (!v.faceKnown) return "0|0|0|0|0|0|0|0";
        return String.format(Locale.US, "%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f", v.mouthX, v.mouthY, v.mouthHW, v.eyeLX, v.eyeLY, v.eyeRX, v.eyeRY, v.eyeR);
    }

    /**
     * A free model from GitHub whose words the description uses (FreeModels), fetched without a key, posed from
     * its idle animation with the props the description names, graded to the pictures' line and offered as the
     * character's picture with its three views — proposals like every other 3D-made picture. The model stays with
     * the story; the credit line goes into the production file. Returns false when no model fits.
     */
    static boolean freeModel(Project project, Story story, Story.CharacterDef c, Library lib, Context ctx, StyleCue cue, boolean ask, Cloud cloud, Progress p) throws IOException {
        Look look = c.look != null ? c.look : new Look();
        java.util.List<com.tarun.kahani.core.FreeModels.Entry> fits = com.tarun.kahani.core.FreeModels.matches(c.description, look);
        if (fits.isEmpty()) return false;
        String key = keyFor(project, story, c);
        com.tarun.kahani.core.FreeModels.Entry e = fits.get(0);
        if (p != null) p.at("Free 3D model from GitHub for " + c.shown() + ": " + e.name);
        byte[] glb = cloud.download(e.url);
        Glb.Options opt = new Glb.Options();
        opt.pose = com.tarun.kahani.core.FreeModels.POSES;
        opt.props = com.tarun.kahani.core.FreeModels.propsFor(e, look, c.description);
        Glb.Model m = Glb.load(glb, decoder(), null, opt);
        try {
            java.io.FileOutputStream o = new java.io.FileOutputStream(project.file("model_" + Math.abs(key.hashCode()) + ".glb"));
            o.write(glb);
            o.close();
        } catch (IOException ignored) {
            // the rendered views are what the film needs; the model file is a keepsake
        }
        Doll3D.Result front = Glb.render(m, 1100, 0);
        if (cue != null) cue.grade(front.px, front.w, front.h);
        String file = project.savePicture(encode(front.px, front.w, front.h, true), "3d_char");
        int[] ratings = SceneMaker.ratings(false, false, true, cue != null && cue.pictures > 0, 0.85f, true);
        String verdict = SceneMaker.verdict(ratings, "") + " — free model: " + e.name + " (" + e.licence + "; " + m.note + ")";
        dropProposals(project, P_CHAR, key, null, true);
        dropProposals(project, P_VIEW, key, null, true);
        addProposal(project, "propose|char|" + key + "|" + file + "|0|0|0|0|0|0|0|0|0|" + SceneMaker.score(ratings) + "|" + verdict);
        String[] own = viewFiles(project, key);
        for (int i = 0; i < Figure3D.VIEW_ANGLES.length; i++) {
            if (own[i] != null) continue;
            Doll3D.Result v = Glb.render(m, 1100, Figure3D.VIEW_ANGLES[i]);
            if (cue != null) cue.grade(v.px, v.w, v.h);
            String vf = project.savePicture(encode(v.px, v.w, v.h, true), "view");
            addProposal(project, "propose|view|" + key + "|" + (int) Figure3D.VIEW_ANGLES[i] + "|" + vf + "|0|0|0|0|0|0|0|0|" + SceneMaker.score(ratings) + "|" + verdict);
        }
        project.setSetting("credit3d." + key, com.tarun.kahani.core.FreeModels.credit(e));
        project.setSetting("model3d." + key, e.name);
        if (!ask) accept(project, lib, ctx, proposalFor(project, P_CHAR, key));
        return true;
    }

    /** Decodes a model's embedded picture with the phone's own decoder. */
    static Glb.ImageDecoder decoder() {
        return new Glb.ImageDecoder() {
            public int[] decode(byte[] bytes) {
                Bitmap b = BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                if (b == null) return null;
                int w = b.getWidth(), h = b.getHeight();
                int[] out = new int[w * h + 2];
                out[0] = w; out[1] = h;
                b.getPixels(out, 2, w, 0, 0, w, h);
                b.recycle();
                return out;
            }
        };
    }

    /** The credit lines of the free models and services this story's pictures came from, for the production file. */
    static java.util.List<String> credits(Project project, Story story) {
        java.util.List<String> out = new java.util.ArrayList<String>();
        for (Story.CharacterDef c : story.cast()) {
            String cr = project.setting("credit3d." + keyFor(project, story, c), "");
            if (cr.length() > 0 && !out.contains(c.shown() + ": " + cr)) out.add(c.shown() + ": " + cr);
        }
        return out;
    }

    /**
     * The views of a character made from its own picture: the figure model (Figure3D), or — with the user's
     * image-to-3D key — a 3D model the service builds from the picture, rendered by the studio. Proposals when
     * ask is set. Returns how many views were made (0 for an animal's side-on picture or a missing picture).
     */
    static int makeViews(Project project, Story story, Story.CharacterDef c, Library lib, Context ctx, StyleCue cue, boolean ask, String meshyKey, Cloud cloud, Progress p, boolean freeSpace) throws IOException {
        String file = charFile(project, story, c);
        if (file == null) return 0;
        Look look = c.look != null ? c.look : new Look();
        boolean beast = look.kind == Look.ANIMAL || look.kind == Look.BIRD;
        int[] d = project.loader().decode(file, 1600);
        if (d == null) return 0;
        int[] px = new int[d[0] * d[1]];
        System.arraycopy(d, 2, px, 0, px.length);
        Cutout.Result cut = Cutout.process(px, d[0], d[1], beast);
        if (beast && cut.h < cut.w * 1.25f) return 0;          // a side-on animal: the rig already mirrors it
        String key = keyFor(project, story, c);
        float[] face = facePoints(project, story, c);
        if (face == null && cut.faceFound) face = new float[]{cut.mouthX, cut.mouthY, cut.mouthW, cut.eyeLX, cut.eyeY, cut.eyeRX, cut.eyeY, cut.eyeR, 0};
        Doll3D.Result[] views = null;
        String source = "the figure model";
        boolean keyed = meshyKey != null && meshyKey.length() >= 8;
        if (cloud != null && (keyed || freeSpace)) {
            try {
                if (p != null) p.at((keyed ? "3D model service: " : "Free 3D service: ") + c.shown());
                final Progress pp = p;
                ImageTo3D.Progress ip = new ImageTo3D.Progress() {
                    public void at(String what) { if (pp != null) pp.at(what); }
                };
                byte[] png = encode(cut.px, cut.w, cut.h, true);
                // the picture rules the shape, the description goes with it (the plain-English guide's 75 / 25)
                ImageTo3D.Result r = keyed ? ImageTo3D.meshy(cloud, meshyKey, png, c.description, ip) : ImageTo3D.freeSpaces(cloud, png, c.description, ip);
                Glb.Options opt = new Glb.Options();
                opt.allProps = true;
                Glb.Model m = Glb.load(r.glb, decoder(), null, opt);
                views = new Doll3D.Result[Figure3D.VIEW_ANGLES.length];
                for (int i = 0; i < views.length; i++) views[i] = Glb.render(m, 1100, Figure3D.VIEW_ANGLES[i]);
                source = r.note + " (" + m.note + ")";
                project.setSetting("credit3d." + key, r.credit);
            } catch (Throwable e) {
                android.util.Log.w("Kahani", "image-to-3D service: " + e);
                views = null;      // the figure model below
            }
        }
        boolean fromFigure = views == null;
        Figure3D.Model model = null;
        if (fromFigure) {
            if (p != null) p.at("Views from the picture: " + c.shown());
            model = Figure3D.build(cut.px, cut.w, cut.h, face, look);
            views = Figure3D.views(model, 1100);
        }
        boolean partsOk = model == null || !model.faceKnown || model.hipRow > model.chinRow;
        int[] ratings = SceneMaker.ratings(true, face != null, partsOk, false, 1f, true);
        String verdict = SceneMaker.verdict(ratings, "") + " — made from " + source;
        dropProposals(project, P_VIEW, key, null, true);
        String[] own = viewFiles(project, key);
        int made = 0;
        for (int i = 0; i < views.length; i++) {
            Doll3D.Result v = views[i];
            if (v == null || own[i] != null) continue;      // the user's own back or side picture is the best view there is
            if (cue != null) cue.grade(v.px, v.w, v.h);
            String vf = project.savePicture(encode(v.px, v.w, v.h, true), "view");
            addProposal(project, "propose|view|" + key + "|" + (int) Figure3D.VIEW_ANGLES[i] + "|" + vf + "|" + viewPoints(v) + "|" + SceneMaker.score(ratings) + "|" + verdict);
            made++;
        }
        if (!ask) for (String[] f : viewProposals(project, key)) accept(project, lib, ctx, f);
        return made;
    }

    /** The picture of a place without one, made in 3D in the film's shape; a proposal when ask is set. Returns the file name. */
    static String makePlace(Project project, Story story, Story.Scene sc, Edits ed, Library lib, Context ctx, StyleCue cue, boolean ask) throws IOException {
        String where = sc.setting.length() > 0 ? sc.setting : sc.title;
        int set = Sets.detect(sc.title + " " + where);
        int tod = Sets.detectTime(sc.title + " " + where, Sets.DAY);
        int[] size = ed.size();
        int w = size[0] >= size[1] ? 1280 : Math.round(1280f * size[0] / size[1]), h = size[0] >= size[1] ? Math.round(1280f * size[1] / size[0]) : 1280;
        w &= ~1; h &= ~1;
        Set3D.Result r = Set3D.make(set, tod, w, h, sc.number, cue);
        String file = project.savePicture(encode(r.px, r.w, r.h, false), "3d_place");
        int[] ratings = SceneMaker.ratings(false, false, true, cue != null && cue.pictures > 0, 0.8f, true);
        String verdict = SceneMaker.verdict(ratings, "");
        String key = String.valueOf(sc.number);
        dropProposals(project, P_SCENE, key, null, true);
        addProposal(project, String.format(Locale.US, "propose|scene|%s|%s|0|0|1|1|%.4f|%d|%s", key, file, r.ground, SceneMaker.score(ratings), verdict));
        if (!ask) accept(project, lib, ctx, proposalFor(project, P_SCENE, key));
        return file;
    }

    /** Every character and place still without a picture (and not rejected before) gets a proposal. Returns how many were made. */
    static int makeMissing(Project project, Story story, Edits ed, Library lib, Context ctx, StyleCue cue, boolean ask, Progress p, Cloud cloud, boolean freeModels) {
        int made = 0;
        for (Story.CharacterDef c : story.cast()) {
            String key = keyFor(project, story, c);
            if (charFile(project, story, c) != null || proposalFor(project, P_CHAR, key) != null || rejected(project, P_CHAR, key)) continue;
            if (p != null) p.at("Studio 3D: " + c.shown());
            try { makeCharacter(project, story, c, lib, ctx, cue, ask, cloud, freeModels); made++; } catch (Throwable e) { android.util.Log.w("Kahani", "3D character: " + e); }
        }
        java.util.Set<String> haveScene = new java.util.HashSet<String>();
        for (String line : project.read("cast.txt").split("\n")) {
            String[] f = line.trim().split("\\|");
            if (f.length >= 3 && f[0].equals("scene") && project.has(f[2])) haveScene.add(f[1].replaceAll("[a-z]$", ""));
        }
        for (Story.Scene sc : story.scenes) {
            String key = String.valueOf(sc.number);
            if (haveScene.contains(key) || proposalFor(project, P_SCENE, key) != null || rejected(project, P_SCENE, key)) continue;
            if (p != null) p.at("Studio 3D: " + (sc.title.length() > 0 ? sc.title : "part " + sc.number));
            try { makePlace(project, story, sc, ed, lib, ctx, cue, ask); made++; } catch (Throwable e) { android.util.Log.w("Kahani", "3D place: " + e); }
        }
        return made;
    }

    /** Every character with a picture but no views (and no rejected or pending view proposal) gets its views. Returns how many characters. */
    static int makeAllViews(Project project, Story story, Library lib, Context ctx, StyleCue cue, boolean ask, String meshyKey, Cloud cloud, Progress p, boolean freeSpace) {
        int made = 0;
        for (Story.CharacterDef c : story.cast()) {
            String key = keyFor(project, story, c);
            if (charFile(project, story, c) == null || allViews(project, key) || !viewProposals(project, key).isEmpty() || rejected(project, P_VIEW, key)) continue;
            try { if (makeViews(project, story, c, lib, ctx, cue, ask, meshyKey, cloud, p, freeSpace) > 0) made++; } catch (Throwable e) { android.util.Log.w("Kahani", "3D views: " + e); }
        }
        return made;
    }

    /** The master sheet of a character's doll (front, three-quarter, side, back), saved next to the story. Returns the file name. */
    static String masterSheet(Project project, Story.CharacterDef c) throws IOException {
        Look look = c.look != null ? c.look : new Look();
        Doll3D.Result r = Doll3D.masterSheet(look, 640, Math.abs(c.displayName.hashCode()) % 1000);
        String name = "master_" + Math.abs(c.displayName.hashCode()) + ".jpg";
        java.io.FileOutputStream o = new java.io.FileOutputStream(project.file(name));
        o.write(encode(r.px, r.w, r.h, false));
        o.close();
        return name;
    }

    /** The picture and its views side by side on one plate (jpg next to the story), for a look. Returns the file name or null. */
    static String viewSheet(Project project, String key, String front, String[] files) throws IOException {
        List<Bitmap> bs = new ArrayList<Bitmap>();
        if (front != null && project.has(front)) bs.add(BitmapFactory.decodeFile(project.file(front).getAbsolutePath()));
        if (files != null) for (String f : files) if (f != null && project.has(f)) bs.add(BitmapFactory.decodeFile(project.file(f).getAbsolutePath()));
        bs.removeAll(java.util.Collections.singleton(null));
        if (bs.isEmpty()) return null;
        int H = 640, W = 20;
        for (Bitmap b : bs) W += Math.round(b.getWidth() * H / (float) Math.max(1, b.getHeight())) + 20;
        Bitmap plate = Bitmap.createBitmap(W, H + 40, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(plate);
        cv.drawColor(0xFFE4EAF0);
        Paint p = new Paint(Paint.FILTER_BITMAP_FLAG | Paint.ANTI_ALIAS_FLAG);
        int x = 20;
        for (Bitmap b : bs) {
            int w = Math.round(b.getWidth() * H / (float) Math.max(1, b.getHeight()));
            cv.drawBitmap(b, null, new android.graphics.Rect(x, 20, x + w, 20 + H), p);
            x += w + 20;
            b.recycle();
        }
        String name = "views_" + Math.abs(key.hashCode()) + ".jpg";
        java.io.FileOutputStream o = new java.io.FileOutputStream(project.file(name));
        plate.compress(Bitmap.CompressFormat.JPEG, 90, o);
        o.close();
        plate.recycle();
        return name;
    }

    /**
     * A doll's look conditioned on the user's own pictures (the phone guide §7.2, item 6): the library picture that
     * fits the character's description best (by its traits and worn colours, the name counting double) lends its
     * two main clothing colours and its hair colour. The description still rules kind, outfit and props.
     */
    static Look referenceLook(Look look, Story.CharacterDef c, Library lib, String[] note) {
        if (lib == null || c == null) return look;
        Library.Item best = null;
        float bestS = 0.45f;
        int seen = 0;
        try {
            for (Library.Item it : lib.find(Library.PIC, null, null)) {
                if ("view".equals(it.kind) || "place".equals(it.kind) || "object".equals(it.kind) || "1".equals(it.meta("3d"))) continue;
                if (seen++ > 200) break;
                com.tarun.kahani.core.PicSense.Info in = lib.info(it);
                if (in == null || !in.figure) continue;
                float sc = com.tarun.kahani.core.PicSense.matchCharacter(in, com.tarun.kahani.core.PicSense.Traits.fromMeta(it.meta), c);
                if (com.tarun.kahani.core.ScriptAI.matchName(it.name, java.util.Collections.singletonList(c.displayName)) != null) sc = Math.min(1f, sc + 0.3f);
                if (sc > bestS) { bestS = sc; best = it; }
            }
        } catch (Throwable e) {
            return look;
        }
        if (best == null) return look;
        com.tarun.kahani.core.PicSense.Info in = lib.info(best);
        Look out = look.copy();
        int b1 = -1, b2 = -1;
        for (int i = 0; i < 12 && i < in.hue.length; i++) {
            if (b1 < 0 || in.hue[i] > in.hue[b1]) { b2 = b1; b1 = i; }
            else if (b2 < 0 || in.hue[i] > in.hue[b2]) b2 = i;
        }
        if (b1 >= 0 && in.hue[b1] > 0.10f) out.primary = hueColour(b1);
        if (b2 >= 0 && in.hue[b2] > 0.08f) out.secondary = hueColour(b2);
        com.tarun.kahani.core.PicSense.Traits tr = com.tarun.kahani.core.PicSense.Traits.fromMeta(best.meta);
        if (tr != null && tr.greyHair > 0) out.hairColor = 0xFFBDBDBD;
        if (note != null && note.length > 0) note[0] = String.format(Locale.US, "reference: your picture \"%s\" (fit %.0f%%) lends its colours", best.label(), bestS * 100);
        return out;
    }

    /** A worn colour for a hue bin (30-degree steps): the doll's cloth in that hue. */
    static int hueColour(int bin) {
        float[] hsv = {bin * 30f, 0.62f, 0.72f};
        return android.graphics.Color.HSVToColor(hsv);
    }

    /** The character bibles of the cast (the scene maker guide, ch. 3) for the production file. */
    static String bibles(Project project, Story story) {
        StringBuilder b = new StringBuilder();
        int order = 0;
        for (Story.CharacterDef c : story.cast()) {
            String key = keyFor(project, story, c);
            String file = charFile(project, story, c);
            String corrections = "";
            if ("1".equals(project.setting("accepted3d.char." + key, "0"))) corrections += "3D doll accepted; ";
            if ("1".equals(project.setting("rejected3d.char." + key, "0"))) corrections += "3D doll rejected; ";
            if ("1".equals(project.setting("accepted3d.view." + key, "0"))) corrections += "views accepted; ";
            if ("1".equals(project.setting("rejected3d.view." + key, "0"))) corrections += "views rejected; ";
            b.append(SceneMaker.bible(order++, c, file, facePoints(project, story, c) != null, viewFiles(project, key), corrections.trim())).append('\n');
        }
        return b.toString();
    }

    /**
     * The facial identity specification of every cast member (the director's manual 2.3): the cut-out's found
     * eye, mouth and chin points, the picture's traits and the description, in the manual's template.
     */
    static String facialSpecs(Project project, Story story, boolean humanReview) {
        StringBuilder b = new StringBuilder();
        java.util.Map<Story.CharacterDef, String> ids = com.tarun.kahani.core.DirectorsManual.charIds(story);
        for (Story.CharacterDef c : story.cast()) {
            String key = keyFor(project, story, c);
            String file = charFile(project, story, c);
            Cutout.Result cut = null;
            com.tarun.kahani.core.PicSense.Traits tr = null;
            if (file != null) {
                try {
                    boolean beast = c.look != null && (c.look.kind == Look.ANIMAL || c.look.kind == Look.BIRD);
                    int[] d = project.loader().decode(file, 420);
                    if (d != null) {
                        int[] px = new int[d[0] * d[1]];
                        System.arraycopy(d, 2, px, 0, px.length);
                        cut = Cutout.process(px, d[0], d[1], beast);
                        tr = com.tarun.kahani.core.PicSense.traits(cut);
                    }
                } catch (Throwable ignored) {
                    cut = null;
                }
            }
            String approval;
            if (file == null) approval = "no picture — the studio's puppet (approve by adding a picture, or let the studio make a doll)";
            else if ("1".equals(project.setting("accepted3d.char." + key, "0"))) approval = file + " — a studio doll you accepted (✔ Use)";
            else if (project.setting("model3d." + key, "").length() > 0) approval = file + " — a free model you accepted (" + project.setting("credit3d." + key, "") + ")";
            else approval = file + " — your own picture (approved by choosing it)";
            b.append(com.tarun.kahani.core.DirectorsManual.facialSpec(ids.get(c), c, file, cut, tr, facePoints(project, story, c) != null, viewFiles(project, key), approval, humanReview)).append('\n');
        }
        return b.toString();
    }

    /**
     * The project asset inventory (the director's manual 1.1): every character, view, place, object picture,
     * voice and background sound of this story, with its persistent ID, file, source and status — each file
     * checked to exist, a picture never trusted by its name alone.
     */
    static java.util.List<String[]> inventory(Project project, Story story, Library lib) {
        java.util.List<String[]> rows = new java.util.ArrayList<String[]>();
        java.util.Map<Story.CharacterDef, String> ids = com.tarun.kahani.core.DirectorsManual.charIds(story);
        for (Story.CharacterDef c : story.cast()) {
            String key = keyFor(project, story, c);
            String file = charFile(project, story, c);
            String id = ids.get(c);
            String source;
            if (file == null) source = "no picture: the studio's puppet from the description";
            else if ("1".equals(project.setting("accepted3d.char." + key, "0"))) source = "studio doll (3D, accepted)";
            else if (project.setting("model3d." + key, "").length() > 0) source = "free model: " + project.setting("credit3d." + key, "");
            else if (project.setting("auto.pic.char:" + key, "").length() > 0 || project.setting("pic.char:" + key, "").length() > 0) source = "your library (tarunkahani)";
            else source = "your picture";
            rows.add(new String[]{id, "character", c.shown(), file == null ? "—" : file, file == null ? "puppet" : project.has(file) ? "inspected" : "MISSING FILE", source});
            String[] views = viewFiles(project, key);
            if (views != null) for (int i = 0; i < views.length; i++) if (views[i] != null)
                rows.add(new String[]{id + "_V" + i, "view", c.shown() + " (" + com.tarun.kahani.core.Figure3D.VIEW_NAMES[i] + ")", views[i], project.has(views[i]) ? "inspected" : "MISSING FILE",
                        "1".equals(project.setting("accepted3d.view." + key, "0")) ? "made from the picture, accepted" : "given by you or the library"});
            Library.Item vs = lib == null ? null : lib.byId(project.setting("vsample." + c.displayName, ""));
            rows.add(new String[]{com.tarun.kahani.core.DirectorsManual.voiceId(id), "voice", c.shown(), vs != null ? vs.name : "—", vs != null ? "your voice sample" : "studio voice (matched to the description)", ""});
        }
        int loc = 0;
        for (String l : project.read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length >= 3 && f[0].equals("scene")) {
                rows.add(new String[]{String.format(java.util.Locale.US, "LOC_PIC_%02d", ++loc), "place picture", "scene " + f[1], f[2], project.has(f[2]) ? "inspected" : "MISSING FILE",
                        "1".equals(project.setting("accepted3d.scene." + f[1], "0")) ? "studio 3D place, accepted" : "your picture or library"});
            } else if (f.length >= 4 && f[0].equals("shot")) {
                rows.add(new String[]{"INSERT_" + f[1], "insert picture", f[2], f[3], project.has(f[3]) ? "inspected" : "MISSING FILE", "a cinematic picture of this moment / the thing itself"});
            } else if (f.length >= 4 && f[0].equals("propose")) {
                rows.add(new String[]{"PROPOSAL", "proposal (3D " + f[1] + ")", f[2], f[1].equals("view") ? (f.length > 4 ? f[4] : "") : f[3], "waiting for your ✔ Use / ✖ Reject", "not used until you decide"});
            }
        }
        for (Story.Scene sc : story.scenes) {
            Library.Item amb = lib == null ? null : lib.byId(project.setting("amb." + sc.number, ""));
            if (amb != null) rows.add(new String[]{"AMB_" + sc.number, "background sound", "scene " + sc.number, amb.name, "your recording", ""});
        }
        return rows;
    }

    /** ARGB pixels to PNG (with transparency) or JPEG bytes. */
    static byte[] encode(int[] px, int w, int h, boolean png) {
        Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        b.setPixels(px, 0, w, 0, 0, w, h);
        ByteArrayOutputStream o = new ByteArrayOutputStream();
        b.compress(png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 92, o);
        b.recycle();
        return o.toByteArray();
    }
}
