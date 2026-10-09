package com.tarun.kahani.app;

import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import java.util.ArrayList;
import java.util.List;

/**
 * v29: one place where pictures of a character, a place or a thing are read, split and saved — a sheet of several
 * figures (angles, poses, expressions) into its figures, a sheet of place views into its panels — whichever way
 * the picture arrived: the "Pictures" button, the character's picture picker, a library placement, the director's
 * own placement from the library, the mouth/eye page. Every figure becomes a pose picture of the character
 * (PoseSense reading), the best of each angle its view, the rest library pictures of the same thing.
 */
final class SheetSaver {
    private SheetSaver() {}

    /** True when the picture is a sheet of several figures (or panels): it splits into two or more figure-sized pieces. */
    static boolean isSheet(byte[] data, boolean panels) {
        try {
            int[] dec = MainActivity.decodeBytes(data, 1200);
            if (dec == null) return false;
            int w = dec[0], h = dec[1];
            int[] px = new int[w * h];
            System.arraycopy(dec, 2, px, 0, px.length);
            List<com.tarun.kahani.core.Angles.Piece> parts = panels
                    ? com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px, w, h, false), w, h)
                    : com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px, w, h), w, h);
            return parts.size() >= 2;
        } catch (Throwable e) {
            return false;
        }
    }

    /** The target string of the split-and-save for a character key, a scene number or a thing. */
    static String target(String kind, String key, String shown) { return "angles:" + kind + ":" + key + ":" + shown; }

    /**
     * Reads, splits and saves the pictures for the target ("angles:char:key:shown", "angles:scene:n:name",
     * "angles:obj:key:name"); the pose files made go into newPoses (may be null). Returns the message for the user.
     */
    static Object save(Project project, Library library, Story st, String tgt, List<byte[]> datas, List<String> newPosesIn) throws Exception {
        final String[] p = tgt.split(":", 4);
        final String kind = p.length > 1 ? p[1] : "char", key = p.length > 2 ? p[2] : "", shown = p.length > 3 && p[3].length() > 0 ? p[3] : key;
        final List<String> newPoses = newPosesIn != null ? newPosesIn : new ArrayList<String>();
        Story.CharacterDef c = kind.equals("char") ? ScriptParser.resolve(st, key) : null;
        boolean beast = c != null && c.look != null && (c.look.kind == Look.ANIMAL || c.look.kind == Look.BIRD);
        // 1. every picture; a sheet of several figures split into them
        List<Object[]> pics = new ArrayList<Object[]>();     // {px, w, h, cutOut, cameraPhoto}
        int split = 0;
        int unreadable = 0, files = 0;
        for (byte[] d : datas) {
            if (++files > 10) break;                                    // v26: up to 10 pictures, each up to 10 angles
            // v28: a sheet is read at up to 2600 px wide where the heap allows, so every figure cut from it
            // (a tenth of the sheet) is sharp enough for a close-up and its face large enough to read
            int[] dec = MainActivity.decodeBytes(d, Project.bigSide());
            if (dec == null) { unreadable++; continue; }
            int w = dec[0], h = dec[1];
            int[] px = new int[w * h];
            System.arraycopy(dec, 2, px, 0, px.length);
            boolean camera = Library.cameraPhoto(d);
            List<com.tarun.kahani.core.Angles.Piece> parts = new ArrayList<com.tarun.kahani.core.Angles.Piece>();
            // a sheet of several figures (angles, poses, expressions) is split into them: the figure-sized pieces
            // only (labels and crumbs dropped), the ten largest, in reading order; a sheet of place views is split
            // into its panels (whole crops). v29: the picture itself decides (a sheet splits into two or more
            // figure-sized pieces; a photo with a real background does not), never a camera tag in the file
            try {
                parts = kind.equals("scene") ? com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px, w, h, false), w, h)
                        : com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px, w, h), w, h);
            } catch (OutOfMemoryError oom) {
                // v30: a large sheet on a phone with little heap left: read again smaller and split that
                parts = new ArrayList<com.tarun.kahani.core.Angles.Piece>();
                try {
                    int[] dec2 = MainActivity.decodeBytes(d, 1600);
                    if (dec2 != null) {
                        w = dec2[0]; h = dec2[1];
                        px = new int[w * h];
                        System.arraycopy(dec2, 2, px, 0, px.length);
                        parts = kind.equals("scene") ? com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px, w, h, false), w, h)
                                : com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px, w, h), w, h);
                    }
                } catch (Throwable e) { parts = new ArrayList<com.tarun.kahani.core.Angles.Piece>(); }
            } catch (Throwable e) { parts = new ArrayList<com.tarun.kahani.core.Angles.Piece>(); }
            if (parts.size() >= 2) camera = false;
            if (parts.size() >= 2) { split += parts.size(); for (com.tarun.kahani.core.Angles.Piece pc : parts) pics.add(new Object[]{pc.px, pc.w, pc.h, Boolean.TRUE, camera}); }
            else pics.add(new Object[]{px, w, h, Boolean.FALSE, camera});
            if (pics.size() >= 100) break;
        }
        if (pics.isEmpty()) return unreadable > 0 ? "These pictures could not be read (" + unreadable + "). Try another format (JPG or PNG) or a smaller picture" : "No picture could be read";
        if (pics.size() > 100) pics = pics.subList(0, 100);
        StringBuilder done = new StringBuilder();
        if (kind.equals("char")) {
            // 2. the angle of each figure from its face, then the front, the views, the extras
            float[] guessed = new float[pics.size()];
            byte[][] bytes = new byte[pics.size()][];
            com.tarun.kahani.core.Cutout.Result[] cuts = new com.tarun.kahani.core.Cutout.Result[pics.size()];
            com.tarun.kahani.core.PoseSense.Tag[] tags = new com.tarun.kahani.core.PoseSense.Tag[pics.size()];
            float[] hRatios = new float[pics.size()];
            int standH = 0;
            for (int i = 0; i < pics.size(); i++) {
                Object[] o = pics.get(i);
                int[] px = (int[]) o[0]; int w = (Integer) o[1], h = (Integer) o[2];
                boolean cut = (Boolean) o[3], camera = (Boolean) o[4];
                com.tarun.kahani.core.Cutout.Result r = null;
                try { r = com.tarun.kahani.core.Cutout.process(px, w, h, beast); } catch (Throwable e) { /* the picture is kept as a front */ }
                cuts[i] = r;
                guessed[i] = r == null ? com.tarun.kahani.core.Angles.FRONT : com.tarun.kahani.core.Angles.guess(r, beast);
                if (r != null && cut && r.w > 0 && r.h > 0 && r.w < r.h * 1.25f) standH = Math.max(standH, r.h);   // the tallest upright figure stands (a lying one is wider than tall)
                byte[] b = Studio3DArt.encode(px, w, h, cut);
                if (camera && !cut) { try { b = MainActivity.toonify(b, true); } catch (Exception ignored) { /* the photo itself then */ } }
                bytes[i] = b;
            }
            // v27: what each picture shows — its angle, pose and feeling, read from the figure (the user corrects the reading after)
            for (int i = 0; i < pics.size(); i++) {
                com.tarun.kahani.core.Cutout.Result r = cuts[i];
                if (r == null) continue;
                boolean cut = (Boolean) pics.get(i)[3];
                try {
                    tags[i] = com.tarun.kahani.core.PoseSense.tag(r, cut ? standH : 0, beast);
                    guessed[i] = tags[i].angle;
                    hRatios[i] = cut && standH > 0 ? Math.max(0.2f, Math.min(1.5f, r.h / (float) standH)) : 1f;
                } catch (Throwable e) { tags[i] = null; }
            }
            // v26: every slot (front, three-quarter, side, back) takes the best real picture guessed at that angle —
            // the largest one; the others of that angle go to the library as more pictures of the same thing.
            // A slot the user gave nothing for stays empty: no drawn view is ever put there.
            float[] angles = new float[pics.size()];
            java.util.Arrays.fill(angles, Float.NaN);
            float[] slotAngles = {com.tarun.kahani.core.Angles.FRONT, com.tarun.kahani.core.Angles.THREE_QUARTER, com.tarun.kahani.core.Angles.SIDE, com.tarun.kahani.core.Angles.BACK};
            for (float sa : slotAngles) {
                int best = -1; long bestArea = -1;
                for (int i = 0; i < pics.size(); i++) {
                    if (guessed[i] != sa) continue;
                    long area = (long) (Integer) pics.get(i)[1] * (Integer) pics.get(i)[2];
                    if (area > bestArea) { bestArea = area; best = i; }
                }
                if (best >= 0) angles[best] = sa;
            }
            String have = c == null ? null : Studio3DArt.charFile(project, st, c);
            boolean frontReal = "1".equals(project.setting("realview." + key + ".0", ""));
            // a drawn front (a doll, a 3D-made picture) gives way to the first real front
            boolean haveFront = false;
            for (float a : angles) if (a == com.tarun.kahani.core.Angles.FRONT) haveFront = true;
            if (have != null && haveFront && !frontReal && have.startsWith("3d_")) { project.setManifest("char", key, null); have = null; }
            // the drawn views (made from the picture) go: real angles replace them, slot by slot or entirely
            for (float sa : slotAngles) {
                if (sa == com.tarun.kahani.core.Angles.FRONT) continue;
                if (!Studio3DArt.realView(project, key, sa)) Studio3DArt.setView(project, key, sa, null);
            }
            project.setSetting("realangles." + key, "1");
            project.setSetting("rejected3d.view." + key, "1");
            String libKey = project.setting("pic.char:" + key, "");
            int posesMade = 0;
            for (int i = 0; i < pics.size(); i++) {
                float a = angles[i];
                boolean png = bytes[i].length > 8 && (bytes[i][1] & 255) == 'P';
                String ext = png ? ".png" : ".jpg";
                Library.Item made = null;
                if (!Float.isNaN(a) && a == com.tarun.kahani.core.Angles.FRONT) {
                    project.setSetting("realview." + key + ".0", "1");
                    if (have == null) {
                        String f = project.savePicture(bytes[i], "char");
                        project.setManifest("char", key, "char|" + key + "|" + f);
                        Library.Item it = library.addBytes(Library.PIC, "person", shown, "front", bytes[i], ext, "angles");
                        project.setSetting("pic.char:" + key, it.id);
                        libKey = it.id;
                        have = f;
                        made = it;
                        done.append("front picture; ");
                    } else {
                        made = library.addBytes(Library.PIC, "person", shown + " (front, another)", "front", bytes[i], ext, "angles");
                        made.setMeta("ofName", shown);
                        done.append("another front (library); ");
                    }
                } else if (!Float.isNaN(a)) {
                    String f = project.savePicture(bytes[i], "view");
                    Studio3DArt.setView(project, key, a, "view|" + key + "|" + (int) a + "|" + f);
                    project.setSetting("realview." + key + "." + (int) a, "1");
                    Library.Item it = library.addBytes(Library.PIC, "view", shown + " (" + com.tarun.kahani.core.Angles.name(a) + " view)", com.tarun.kahani.core.Angles.name(a), bytes[i], ext, "angles");
                    it.setMeta("view", String.valueOf((int) a));
                    it.setMeta("ofName", shown);
                    if (libKey.length() > 0) it.setMeta("of", libKey);
                    made = it;
                    done.append(com.tarun.kahani.core.Angles.name(a)).append(" view; ");
                } else {
                    String an = com.tarun.kahani.core.Angles.name(guessed[i]);
                    Library.Item it = library.addBytes(Library.PIC, guessed[i] == com.tarun.kahani.core.Angles.FRONT ? "person" : "view", shown + " (" + an + ", picture " + (i + 1) + ")", an, bytes[i], ext, "angles");
                    it.setMeta("ofName", shown);
                    if (guessed[i] != com.tarun.kahani.core.Angles.FRONT) it.setMeta("view", String.valueOf((int) guessed[i]));
                    if (libKey.length() > 0) it.setMeta("of", libKey);
                    made = it;
                    done.append(an).append(" ").append(i + 1).append(" (library); ");
                }
                // v27: every picture is a pose picture of the character — the director picks it for the shots
                // that need its angle, pose and feeling (the library keeps the reading for the next story)
                if (tags[i] != null) {
                    try {
                        String pf = project.savePicture(bytes[i], "pose");
                        String pl = Studio3DArt.poseLine(key, pf, tags[i], hRatios[i], cuts[i]);
                        Studio3DArt.addPose(project, key, pl);
                        newPoses.add(pf);
                        posesMade++;
                        if (made != null) {
                            made.setMeta("posefile", pf);
                            made.setMeta("posetag", pl.substring(pl.indexOf('|', pl.indexOf('|', pl.indexOf('|') + 1) + 1) + 1));
                            made.setMeta("pose", com.tarun.kahani.core.PoseSense.poseName(tags[i].pose));
                            made.setMeta("emotion", com.tarun.kahani.core.PoseSense.emotionName(tags[i].emotion));
                        }
                    } catch (Exception ignored) { /* the picture stays a view / library picture */ }
                }
            }
            if (posesMade > 0) done.append(posesMade).append(" pose pictures for the shots; ");
            Studio3DArt.dropProposals(project, Studio3DArt.P_VIEW, key, null, true);     // real angles beat made views
        } else if (kind.equals("scene")) {
            boolean haveMain = project.manifestLine("scene", key) != null, haveRev = project.manifestLine("scene", key + "r") != null;
            for (int i = 0; i < pics.size(); i++) {
                Object[] o = pics.get(i);
                byte[] b = Studio3DArt.encode((int[]) o[0], (Integer) o[1], (Integer) o[2], false);
                if ((Boolean) o[4]) { try { b = MainActivity.toonify(b, false); } catch (Exception ignored) { /* the photo itself then */ } }
                if (!haveMain) {
                    String f = project.savePicture(b, "scene");
                    project.setManifest("scene", key + "a", null); project.setManifest("scene", key + "b", null);
                    project.setManifest("scene", key, "scene|" + key + "|" + f);
                    library.addBytes(Library.PIC, "place", shown, "wide view", b, ".jpg", "angles");
                    haveMain = true;
                    done.append("the place (wide); ");
                } else if (!haveRev) {
                    String f = project.savePicture(b, "scene");
                    project.setManifest("scene", key + "r", "scene|" + key + "r|" + f);
                    Library.Item it = library.addBytes(Library.PIC, "place", shown + " (reverse angle)", "reverse angle", b, ".jpg", "angles");
                    it.setMeta("view", "reverse");
                    haveRev = true;
                    done.append("the reverse angle (behind the reverse shots); ");
                } else {
                    Library.Item it = library.addBytes(Library.PIC, "place", shown + " (angle " + (i + 1) + ")", "", b, ".jpg", "angles");
                    it.setMeta("ofName", shown);
                    done.append("angle ").append(i + 1).append(" (library); ");
                }
            }
        } else {
            // a thing: its first picture is the insert of the thing itself; every angle goes to the library
            boolean haveObj = project.read("cast.txt").contains("|" + key + "|");
            for (int i = 0; i < pics.size(); i++) {
                Object[] o = pics.get(i);
                boolean cut = (Boolean) o[3];
                byte[] b = Studio3DArt.encode((int[]) o[0], (Integer) o[1], (Integer) o[2], cut);
                if (!haveObj) {
                    String f = project.savePicture(b, "obj");
                    project.setManifest("shot", ":" + key, "shot||" + key + "|" + f + "|object");
                    haveObj = true;
                    done.append("the insert picture; ");
                } else done.append("angle ").append(i + 1).append(" (library); ");
                library.addBytes(Library.PIC, "object", shown + (i == 0 ? "" : " (angle " + (i + 1) + ")"), key.replace(',', ' '), b, cut ? ".png" : ".jpg", "angles");
            }
        }
        library.save();
        return "✅ " + shown + ": " + done + (split > 0 ? "(" + split + " figures split from a sheet) " : "") + "— all in the library";
    }
}
