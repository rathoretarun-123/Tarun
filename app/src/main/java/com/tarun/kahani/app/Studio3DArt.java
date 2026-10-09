package com.tarun.kahani.app;

import android.graphics.Bitmap;

import com.tarun.kahani.core.Doll3D;
import com.tarun.kahani.core.Edits;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Set3D;
import com.tarun.kahani.core.Sets;
import com.tarun.kahani.core.Story;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Locale;

/**
 * Studio 3D on the phone: the director's own three-dimensional pictures for whatever has no picture yet — a
 * character built from its description (Doll3D) with its eye and mouth points known exactly, a place at its
 * hour (Set3D) with its floor line known exactly — saved into the story and the library like any other picture.
 */
final class Studio3DArt {
    private Studio3DArt() {}

    interface Progress { void at(String what); }

    /** The picture of a character, made in 3D, saved and set as its picture. Returns the file name. */
    static String makeCharacter(Project project, Story.CharacterDef c, Library lib) throws IOException {
        Look look = c.look != null ? c.look : new Look();
        Doll3D.Result r = Doll3D.make(look, 1100, Math.abs(c.displayName.hashCode()) % 1000);
        byte[] png = encode(r.px, r.w, r.h, true);
        String file = project.savePicture(png, "3d_char");
        project.setManifest("char", c.displayName, String.format(Locale.US, "char|%s|%s|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f",
                c.displayName, file, r.mouthX, r.mouthY, r.mouthHW, r.eyeLX, r.eyeLY, r.eyeRX, r.eyeRY, r.eyeR, r.turbanY));
        if (lib != null) try { lib.addBytes(Library.PIC, "person", c.shown(), c.description.length() > 200 ? c.description.substring(0, 200) : c.description, png, ".png", "3D (studio)"); } catch (Exception ignored) {}
        return file;
    }

    /** The master sheet of a character (front, three-quarter, side, back), saved next to the story. Returns the file name. */
    static String masterSheet(Project project, Story.CharacterDef c) throws IOException {
        Look look = c.look != null ? c.look : new Look();
        Doll3D.Result r = Doll3D.masterSheet(look, 640, Math.abs(c.displayName.hashCode()) % 1000);
        String name = "master_" + Math.abs(c.displayName.hashCode()) + ".jpg";
        java.io.FileOutputStream o = new java.io.FileOutputStream(project.file(name));
        o.write(encode(r.px, r.w, r.h, false));
        o.close();
        return name;
    }

    /** The picture of a place, made in 3D in the film's shape, saved and set as the scene's background. Returns the file name. */
    static String makePlace(Project project, Story story, Story.Scene sc, Edits ed, Library lib) throws IOException {
        String where = sc.setting.length() > 0 ? sc.setting : sc.title;
        int set = Sets.detect(sc.title + " " + where);
        int tod = Sets.detectTime(sc.title + " " + where, Sets.DAY);
        int[] size = ed.size();
        int w = size[0] >= size[1] ? 1280 : Math.round(1280f * size[0] / size[1]), h = size[0] >= size[1] ? Math.round(1280f * size[1] / size[0]) : 1280;
        w &= ~1; h &= ~1;
        Set3D.Result r = Set3D.make(set, tod, w, h, sc.number);
        byte[] jpg = encode(r.px, r.w, r.h, false);
        String file = project.savePicture(jpg, "3d_place");
        project.setManifest("scene", String.valueOf(sc.number), String.format(Locale.US, "scene|%d|%s|0|0|1|1|%.4f", sc.number, file, r.ground));
        if (lib != null) try { lib.addBytes(Library.PIC, "place", Sets.label(set), where.length() > 200 ? where.substring(0, 200) : where, jpg, ".jpg", "3D (studio)"); } catch (Exception ignored) {}
        return file;
    }

    /** Every character and place still without a picture gets one in 3D. Returns how many were made. */
    static int makeMissing(Project project, Story story, Edits ed, Library lib, Progress p) {
        String cast = project.read("cast.txt");
        java.util.Set<String> haveChar = new java.util.HashSet<String>(), haveScene = new java.util.HashSet<String>();
        for (String line : cast.split("\n")) {
            String[] f = line.trim().split("\\|");
            if (f.length >= 3 && f[0].equals("char")) {
                Story.CharacterDef c = ScriptParser.resolve(story, f[1]);
                if (c != null && project.has(f[2])) haveChar.add(c.id);
            } else if (f.length >= 3 && f[0].equals("scene") && project.has(f[2])) haveScene.add(f[1].replaceAll("[a-z]$", ""));
        }
        int made = 0;
        for (Story.CharacterDef c : story.cast()) {
            if (haveChar.contains(c.id)) continue;
            if (p != null) p.at("Studio 3D: " + c.shown());
            try { makeCharacter(project, c, lib); made++; } catch (Throwable e) { android.util.Log.w("Kahani", "3D character: " + e); }
        }
        for (Story.Scene sc : story.scenes) {
            if (haveScene.contains(String.valueOf(sc.number))) continue;
            if (p != null) p.at("Studio 3D: " + (sc.title.length() > 0 ? sc.title : "part " + sc.number));
            try { makePlace(project, story, sc, ed, lib); made++; } catch (Throwable e) { android.util.Log.w("Kahani", "3D place: " + e); }
        }
        return made;
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
