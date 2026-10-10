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

    /**
     * True when the pieces are whole panels (a sheet of place views): the corners of a panel's box hold picture
     * content, the corners of a figure's box hold the sheet's background (the figure never fills its corners).
     */
    static boolean panelsLike(int[] px, int w, int h, List<com.tarun.kahani.core.Angles.Piece> parts) {
        long r = 0, g = 0, b = 0, n = 0;
        for (int x = 0; x < w; x += 3) { for (int y : new int[]{0, h - 1}) { int c = px[y * w + x]; r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255; n++; } }
        for (int y = 0; y < h; y += 3) { for (int x : new int[]{0, w - 1}) { int c = px[y * w + x]; r += (c >> 16) & 255; g += (c >> 8) & 255; b += c & 255; n++; } }
        if (n == 0) return false;
        int br = (int) (r / n), bg = (int) (g / n), bb = (int) (b / n);
        int panels = 0;
        for (com.tarun.kahani.core.Angles.Piece pc : parts) {
            int inset = Math.max(2, Math.min(pc.w, pc.h) / 40), block = Math.max(3, Math.min(pc.w, pc.h) / 12);
            int contentCorners = 0;
            for (int cy = 0; cy < 2; cy++) for (int cx = 0; cx < 2; cx++) {
                int x0 = pc.x0 + (cx == 0 ? inset : pc.w - inset - block), y0 = pc.y0 + (cy == 0 ? inset : pc.h - inset - block);
                int content = 0, total = 0;
                for (int y = y0; y < y0 + block; y++) for (int x = x0; x < x0 + block; x++) {
                    if (x < 0 || y < 0 || x >= w || y >= h) continue;
                    int c = px[y * w + x];
                    int d = Math.abs(((c >> 16) & 255) - br) + Math.abs(((c >> 8) & 255) - bg) + Math.abs((c & 255) - bb);
                    total++;
                    if (d > 60) content++;
                }
                if (total > 0 && content * 10 >= total * 6) contentCorners++;
            }
            if (contentCorners >= 3) panels++;
        }
        return panels * 2 > parts.size();
    }

    /**
     * v31: a sheet added to the library — from Home, the library screen, the bulk picker — is split at once and kept
     * as its figures (or its panels for a place), so the library holds the pictures themselves, whichever way they
     * came. kind: "person", "place", "object", or "" to let the picture decide (cut-out figures = a person or thing,
     * whole panels = a place). Returns the items added, the main one (the front, or the wide view) first; empty when
     * the picture is not a sheet.
     */
    static List<Library.Item> saveToLibrary(Library library, String name, String kind, byte[] data, String source) throws Exception {
        List<Library.Item> out = new ArrayList<Library.Item>();
        int[] dec = MainActivity.decodeBytes(data, Project.bigSide());
        if (dec == null) return out;
        int w = dec[0], h = dec[1];
        int[] px = new int[w * h];
        System.arraycopy(dec, 2, px, 0, px.length);
        boolean panels = "place".equals(kind);
        List<com.tarun.kahani.core.Angles.Piece> parts;
        try {
            parts = panels ? com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px.clone(), w, h, false), w, h)
                    : com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px.clone(), w, h), w, h);
            if (!panels && kind.length() == 0 && parts.size() >= 2 && panelsLike(px, w, h, parts)) {
                panels = true;
                parts = com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px.clone(), w, h, false), w, h);
            }
        } catch (Throwable e) { return out; }
        if (parts.size() < 2) return out;
        if (parts.size() > 100) parts = parts.subList(0, 100);
        String base = name == null || name.trim().length() == 0 ? "picture" : name.trim();
        if (panels) {
            for (int i = 0; i < parts.size(); i++) {
                com.tarun.kahani.core.Angles.Piece pc = parts.get(i);
                byte[] b = Studio3DArt.encode(pc.px, pc.w, pc.h, false);
                Library.Item it = i == 0 ? library.addBytes(Library.PIC, "place", base, "wide view", b, ".jpg", source)
                        : i == 1 ? library.addBytes(Library.PIC, "place", base + " (reverse angle)", "reverse angle", b, ".jpg", source)
                        : library.addBytes(Library.PIC, "place", base + " (angle " + (i + 1) + ")", "", b, ".jpg", source);
                if (i == 0) it.setMeta("sheetMain", "1");
                else { it.setMeta("ofName", base); it.setMeta("sheet", out.get(0).id); it.setMeta("of", out.get(0).id); it.setMeta("suffix", it.name.substring(base.length())); }
                if (i == 1) it.setMeta("view", "reverse");
                out.add(it);
            }
            library.save();
            return out;
        }
        boolean thing = "object".equals(kind);
        // the reading of every figure: angle, pose, feeling (a person); the tallest upright one stands
        com.tarun.kahani.core.Cutout.Result[] cuts = new com.tarun.kahani.core.Cutout.Result[parts.size()];
        com.tarun.kahani.core.PoseSense.Tag[] tags = new com.tarun.kahani.core.PoseSense.Tag[parts.size()];
        float[] angles = new float[parts.size()], hRatios = new float[parts.size()];
        byte[][] bytes = new byte[parts.size()][];
        int standH = 0;
        for (int i = 0; i < parts.size(); i++) {
            com.tarun.kahani.core.Angles.Piece pc = parts.get(i);
            bytes[i] = Studio3DArt.encode(pc.px, pc.w, pc.h, true);
            angles[i] = com.tarun.kahani.core.Angles.FRONT;
            if (thing) continue;
            try { cuts[i] = com.tarun.kahani.core.Cutout.process(pc.px.clone(), pc.w, pc.h, false); } catch (Throwable e) { cuts[i] = null; }
            if (cuts[i] != null && cuts[i].w > 0 && cuts[i].h > 0 && cuts[i].w < cuts[i].h * 1.25f) standH = Math.max(standH, cuts[i].h);
        }
        boolean beast = false;
        for (int pass = 0; pass < 2 && !thing; pass++) {
            int little = 0, n = 0;
            for (int i = 0; i < parts.size(); i++) {
                if (cuts[i] == null) continue;
                try {
                    tags[i] = com.tarun.kahani.core.PoseSense.tag(cuts[i], standH, beast);
                    angles[i] = tags[i].angle;
                    hRatios[i] = standH > 0 ? Math.max(0.2f, Math.min(1.5f, cuts[i].h / (float) standH)) : 1f;
                    n++;
                    if (tags[i].m != null && tags[i].m.skin < 0.35f) little++;
                } catch (Throwable e) { tags[i] = null; }
            }
            // v32: a furred creature (a monster, an animal) shows little skin on most figures: read it as a beast,
            // where "no skin" is not "the back"
            if (pass == 0 && n > 0 && little * 10 >= n * 6) {
                beast = true;
                for (int i = 0; i < parts.size(); i++) {
                    try { cuts[i] = com.tarun.kahani.core.Cutout.process(parts.get(i).px.clone(), parts.get(i).w, parts.get(i).h, true); } catch (Throwable e) { cuts[i] = null; }
                }
            } else break;
        }
        // the main picture: v34 the best of the sheet's figures (a front, else a three-quarter, with a face, standing,
        // calm, sure, large); the best of each other angle its view
        int main = -1;
        long maxArea = 1;
        for (com.tarun.kahani.core.Angles.Piece pc : parts) maxArea = Math.max(maxArea, (long) pc.w * pc.h);
        float mainS = -1e9f;
        for (int i = 0; i < parts.size(); i++) {
            long area = (long) parts.get(i).w * parts.get(i).h;
            float sc = thing ? area : mainScore(cuts[i], tags[i], angles[i], area, maxArea, hRatios[i]);
            if (sc > mainS) { mainS = sc; main = i; }
        }
        int[] bestOf = new int[4];
        java.util.Arrays.fill(bestOf, -1);
        float[] slot = {com.tarun.kahani.core.Angles.FRONT, com.tarun.kahani.core.Angles.THREE_QUARTER, com.tarun.kahani.core.Angles.SIDE, com.tarun.kahani.core.Angles.BACK};
        for (int k = 1; k < 4 && !thing; k++) {
            long best = -1;
            for (int i = 0; i < parts.size(); i++) if (Math.abs(angles[i] - slot[k]) < 1 && (long) parts.get(i).w * parts.get(i).h > best) { best = (long) parts.get(i).w * parts.get(i).h; bestOf[k] = i; }
        }
        int[] order = new int[parts.size()];
        order[0] = main;
        for (int i = 0, j = 1; i < parts.size(); i++) if (i != main) order[j++] = i;
        String mainKind = thing ? "object" : "person";
        for (int oi = 0; oi < order.length; oi++) {
            int i = order[oi];
            String an = com.tarun.kahani.core.Angles.name(angles[i]);
            Library.Item it;
            if (i == main) {
                it = library.addBytes(Library.PIC, mainKind, base, thing ? "" : "front", bytes[i], ".png", source);
                it.setMeta("sheetMain", "1");
            } else {
                boolean view = false;
                for (int k = 1; k < 4; k++) if (bestOf[k] == i) view = true;
                if (thing) it = library.addBytes(Library.PIC, "object", base + " (angle " + (i + 1) + ")", "", bytes[i], ".png", source);
                else if (view) it = library.addBytes(Library.PIC, "view", base + " (" + an + " view)", an, bytes[i], ".png", source);
                else it = library.addBytes(Library.PIC, Math.abs(angles[i]) < 1 ? "person" : "view", base + " (" + an + ", picture " + (i + 1) + ")", an, bytes[i], ".png", source);
                it.setMeta("ofName", base);
                it.setMeta("sheet", out.get(0).id);
                it.setMeta("of", out.get(0).id);
                it.setMeta("suffix", it.name.substring(base.length()));
                if (!thing && Math.abs(angles[i]) >= 1) it.setMeta("view", String.valueOf((int) angles[i]));
            }
            if (tags[i] != null) {
                String face = cuts[i] != null && cuts[i].faceFound
                        ? String.format(java.util.Locale.US, "%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f", cuts[i].mouthX, cuts[i].mouthY, cuts[i].mouthW / 2f, cuts[i].eyeLX, cuts[i].eyeY, cuts[i].eyeRX, cuts[i].eyeY, cuts[i].eyeR)
                        : "0|0|0|0|0|0|0|0";
                it.setMeta("posetag", (int) tags[i].angle + "|" + tags[i].pose + "|" + tags[i].emotion + "|" + String.format(java.util.Locale.US, "%.3f", hRatios[i]) + "|" + face);
                it.setMeta("pose", com.tarun.kahani.core.PoseSense.poseName(tags[i].pose));
                it.setMeta("emotion", com.tarun.kahani.core.PoseSense.emotionName(tags[i].emotion));
            }
            out.add(it);
        }
        library.save();
        return out;
    }

    /** The user named (or re-kinded) the main picture of a split sheet: its figures follow it. */
    static void rename(Library library, Library.Item main, String oldName, String newName, String kind) {
        if (main == null || newName == null || newName.length() == 0) return;
        for (Library.Item it : library.find(Library.PIC, null, null)) {
            if (!main.id.equals(it.meta("sheet"))) continue;
            String suffix = it.meta("suffix");
            if (suffix == null) suffix = oldName != null && it.name.startsWith(oldName) ? it.name.substring(oldName.length()) : "";
            it.name = newName + suffix;
            it.setMeta("ofName", newName);
            if ("place".equals(kind)) it.kind = "place";
            else if ("object".equals(kind)) it.kind = "object";
            else if ("person".equals(kind) && "object".equals(it.kind)) it.kind = "view";
        }
    }

    /**
     * v34: how good a picture is as a character's main picture (the one on the card, the one that speaks): a front
     * first, then a three-quarter, never a back; a face found; standing; a calm or happy face; a sure reading in good
     * light; large; as tall as the standing figures.
     */
    static float mainScore(com.tarun.kahani.core.Cutout.Result r, com.tarun.kahani.core.PoseSense.Tag t, float angle, long area, long maxArea, float hRatio) {
        float a = Math.abs(angle), s = 0;
        s += a < 1 ? 4 : Math.abs(a - Math.abs(com.tarun.kahani.core.Angles.THREE_QUARTER)) < 1 ? 2 : Math.abs(a - com.tarun.kahani.core.Angles.BACK) < 1 ? -6 : 0;
        if (r != null && r.faceFound) s += 2;
        if (t != null) {
            s += t.pose == com.tarun.kahani.core.PoseSense.STAND ? 2 : t.pose == com.tarun.kahani.core.PoseSense.WALK ? 0.5f : t.pose == com.tarun.kahani.core.PoseSense.LIE ? -2 : 0;
            s += t.emotion == com.tarun.kahani.core.PoseSense.NEUTRAL ? 1.5f : t.emotion == com.tarun.kahani.core.PoseSense.HAPPY ? 1.2f : t.emotion == com.tarun.kahani.core.PoseSense.NO_FACE ? -2 : 0;
            s += 1.5f * t.conf;
            if (t.light <= com.tarun.kahani.core.Cutout.NORMAL) s += 0.5f;
        }
        s += 2f * area / Math.max(1f, maxArea);
        if (hRatio > 0) s += 1 - Math.min(1, Math.abs(1 - hRatio));
        // v39: the front picture is a whole standing figure, never a sheet's close-up of the face (as wide as it is
        // tall): everything else is sized against it, and a close-up front made giants of the others
        if (r != null && r.w > 0) {
            float aspect = r.h / (float) r.w;
            s += aspect >= 1.8f ? 3 : aspect >= 1.45f ? 1 : aspect < 1.3f ? -5 : 0;
        }
        return s;
    }

    /** The target string of the split-and-save for a character key, a scene number or a thing. */
    static String target(String kind, String key, String shown) { return "angles:" + kind + ":" + key + ":" + shown; }

    /** v34: the key the story's manifest already uses for this character (its char, view or pose lines), else the key given. */
    static String charKey(Project project, Story st, Story.CharacterDef c, String given) {
        for (String l : project.read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length >= 3 && (f[0].equals("char") || f[0].equals("view") || f[0].equals("pose")) && ScriptParser.resolve(st, f[1]) == c) return f[1];
        }
        return given;
    }

    /**
     * v34: the insert line of a thing (shot||key|file|object), replacing the earlier one of the same thing — the
     * manifest's own replace works by the scene field, which a thing's line leaves empty.
     */
    static void setObjectLine(Project project, String key, String line) {
        StringBuilder sb = new StringBuilder();
        for (String l : project.read("cast.txt").split("\n")) {
            if (l.trim().length() == 0) continue;
            String[] f = l.split("\\|");
            boolean same = f.length >= 5 && f[0].equals("shot") && f[1].trim().length() == 0 && f[2].trim().equals(key) && f[4].trim().equals("object");
            if (!same) sb.append(l).append('\n');
        }
        if (line != null) sb.append(line).append('\n');
        project.write("cast.txt", sb.toString());
    }

    /**
     * Reads, splits and saves the pictures for the target ("angles:char:key:shown", "angles:scene:n:name",
     * "angles:obj:key:name"); the pose files made go into newPoses (may be null). Returns the message for the user.
     */
    static Object save(Project project, Library library, Story st, String tgt, List<byte[]> datas, List<String> newPosesIn) throws Exception {
        final String[] p = tgt.split(":", 4);
        final String kind = p.length > 1 ? p[1] : "char";
        final String keyGiven = p.length > 2 ? p[2] : "";
        if (kind.equals("costume")) {
            // v34: a picture of a change of clothes ("key#n"): the first readable picture is that costume's picture,
            // drawn from the moment of the change (the character's own picture recoloured until now)
            for (byte[] d : datas) {
                boolean image = d != null && d.length > 8 && (((d[0] & 255) == 0xFF && (d[1] & 255) == 0xD8) || ((d[0] & 255) == 0x89 && d[1] == 'P') || (d[0] == 'R' && d[1] == 'I') || (d[4] == 'f' && d[5] == 't'));
                if (!image && MainActivity.decodeBytes(d, 512) == null) continue;
                String f = project.savePicture(d, "costume");
                project.setManifest("costume", keyGiven, "costume|" + keyGiven + "|" + f);
                Studio3DArt.dropProposals(project, Studio3DArt.P_COSTUME, keyGiven, null, true);     // the user's picture beats a made doll
                try {
                    Library.Item it = library.addBytes(Library.PIC, "person", (p.length > 3 ? p[3] : keyGiven), "front", d, d.length > 8 && (d[1] & 255) == 'P' ? ".png" : ".jpg", "costume");
                    it.setMeta("costume", keyGiven);
                } catch (Exception ignored) { /* the story has it either way */ }
                return "The picture of the new clothes is saved — the film shows it from the moment of the change";
            }
            return "This picture could not be read";
        }
        Story.CharacterDef c = kind.equals("char") ? ScriptParser.resolve(st, keyGiven) : null;
        // v34: one key per character, whichever name the button used (the Studio's cast key, the popup's display name,
        // an alias): the key of the character's existing line, so its front, views, poses and settings never split in two
        final String key = c != null ? charKey(project, st, c, keyGiven) : keyGiven;
        final String shown = p.length > 3 && p[3].length() > 0 ? p[3] : key;
        final List<String> newPoses = newPosesIn != null ? newPosesIn : new ArrayList<String>();
        boolean beast = c != null && c.look != null && (c.look.kind == Look.ANIMAL || c.look.kind == Look.BIRD || c.look.kind == Look.MONSTER || c.look.kind == Look.MONKEY);   // v32/v33: fur reads like a beast
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
                    if (c != null && c.look != null) {
                        // v34: a many-armed character's own arms are not a wave or a cheer; a rider's low, wide figure sits
                        int ps = tags[i].pose;
                        if (c.look.arms > 2 && (ps == com.tarun.kahani.core.PoseSense.ARMS_UP || ps == com.tarun.kahani.core.PoseSense.WAVE || ps == com.tarun.kahani.core.PoseSense.POINT || ps == com.tarun.kahani.core.PoseSense.FIGHT)) tags[i].pose = com.tarun.kahani.core.PoseSense.STAND;
                        if (c.look.mount >= 0 && (ps == com.tarun.kahani.core.PoseSense.LIE || ps == com.tarun.kahani.core.PoseSense.CROUCH)) tags[i].pose = com.tarun.kahani.core.PoseSense.SIT;
                    }
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
            // v34: the main picture is the best of all the pictures given (up to 100): a front (else a three-quarter)
            // with a face, standing, calm, sure, well lit and large — so the card's picture is always one of the new ones
            long maxArea = 1;
            for (Object[] o : pics) maxArea = Math.max(maxArea, (long) (Integer) o[1] * (Integer) o[2]);
            int mainI = -1; float mainS = -1e9f;
            for (int pass = 0; pass < 2 && mainI < 0; pass++) {
                for (int i = 0; i < pics.size(); i++) {
                    float want = pass == 0 ? com.tarun.kahani.core.Angles.FRONT : com.tarun.kahani.core.Angles.THREE_QUARTER;
                    if (Math.abs(Math.abs(guessed[i]) - Math.abs(want)) > 1) continue;
                    float sc = mainScore(cuts[i], tags[i], guessed[i], (long) (Integer) pics.get(i)[1] * (Integer) pics.get(i)[2], maxArea, hRatios[i]);
                    if (sc > mainS) { mainS = sc; mainI = i; }
                }
            }
            if (mainI >= 0) angles[mainI] = com.tarun.kahani.core.Angles.FRONT;
            for (float sa : slotAngles) {
                if (sa == com.tarun.kahani.core.Angles.FRONT) continue;
                int best = -1; long bestArea = -1;
                for (int i = 0; i < pics.size(); i++) {
                    if (i == mainI || guessed[i] != sa) continue;
                    long area = (long) (Integer) pics.get(i)[1] * (Integer) pics.get(i)[2];
                    if (area > bestArea) { bestArea = area; best = i; }
                }
                if (best >= 0) angles[best] = sa;
            }
            String have = c == null ? null : Studio3DArt.charFile(project, st, c);
            boolean haveFront = false;
            for (float a : angles) if (a == com.tarun.kahani.core.Angles.FRONT) haveFront = true;
            // v33: the newly given front is the character's picture from now on (what the user uploads last is what
            // they want to see); the earlier front stays in the library as a picture of the same character
            if (have != null && haveFront) {
                try {
                    byte[] old = com.tarun.kahani.app.AudioIO.readFile(project.file(have));
                    if (old != null && old.length > 0 && !"1".equals(project.setting("fromlib." + key, ""))) {
                        Library.Item prev = library.addBytes(Library.PIC, "person", shown + " (earlier front)", "front", old, have.endsWith(".png") ? ".png" : ".jpg", "angles");
                        prev.setMeta("ofName", shown);
                    }
                } catch (Exception ignored) { /* the old front is still in the story's own folder */ }
                // every line of this character goes (under any key): the new front is the one the Studio shows and the film uses
                for (String l : project.read("cast.txt").split("\n")) {
                    String[] f = l.split("\\|");
                    if (f.length >= 3 && f[0].equals("char") && ScriptParser.resolve(st, f[1]) == c) project.setManifest("char", f[1], null);
                }
                project.setManifest("char", key, null);
                have = null;
            }
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
                            made.setMeta("conf", String.format(java.util.Locale.US, "%.2f", tags[i].conf));           // v34: the guide's record
                            made.setMeta("light", com.tarun.kahani.core.Cutout.lightName(tags[i].light));
                        }
                    } catch (Exception ignored) { /* the picture stays a view / library picture */ }
                }
            }
            if (posesMade > 0) done.append(posesMade).append(" pose pictures for the shots; ");
            Studio3DArt.dropProposals(project, Studio3DArt.P_VIEW, key, null, true);     // real angles beat made views
        } else if (kind.equals("scene")) {
            // v33: new pictures of a place replace its wide view and reverse angle (the earlier ones stay in the library)
            boolean haveMain = false, haveRev = false;
            for (String old : new String[]{project.manifestLine("scene", key), project.manifestLine("scene", key + "r")}) {
                if (old == null) continue;
                String[] of = old.split("\\|");
                if (of.length < 3 || !project.has(of[2].trim())) continue;
                try {
                    byte[] ob = com.tarun.kahani.app.AudioIO.readFile(project.file(of[2].trim()));
                    if (ob != null && ob.length > 0) { Library.Item prev = library.addBytes(Library.PIC, "place", shown + " (earlier)", "", ob, ".jpg", "angles"); prev.setMeta("ofName", shown); }
                } catch (Exception ignored) { }
            }
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
            boolean haveObj = false;                                                  // v33: the first new picture is the thing's insert from now on
            for (int i = 0; i < pics.size(); i++) {
                Object[] o = pics.get(i);
                boolean cut = (Boolean) o[3];
                byte[] b = Studio3DArt.encode((int[]) o[0], (Integer) o[1], (Integer) o[2], cut);
                if (!haveObj) {
                    String f = project.savePicture(b, "obj");
                    setObjectLine(project, key, "shot||" + key + "|" + f + "|object");      // v34: replaces the earlier insert of this thing
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
