package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;

import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.Files;
import java.util.*;

/**
 * v39: what the app does before a film on the desktop — each character's front picture made a whole standing figure
 * (Art.fullLengthFront), and the angles without a whole standing picture of the user's (three-quarter, side, back)
 * made by Studio 3D's figure model from that front (Figure3D), as accepted views. Writes the project's cast.txt.
 * Usage: MadeViews projectDir
 */
public class MadeViews {
    public static void main(String[] a) throws Exception {
        File dir = new File(a[0]);
        File castF = new File(dir, "cast.txt");
        List<String> lines = new ArrayList<>(Files.readAllLines(castF.toPath()));
        MakeFilm.AwtLoader L = new MakeFilm.AwtLoader(dir.getPath());
        Map<String, List<String[]>> poses = new HashMap<>();
        for (String l : lines) {
            String[] f = l.trim().split("\\|");
            if (f.length >= 15 && f[0].equals("pose")) poses.computeIfAbsent(f[1], k -> new ArrayList<>()).add(f);
        }
        for (int li = 0; li < lines.size(); li++) {
            String[] f = lines.get(li).trim().split("\\|");
            if (f.length < 3 || !f[0].equals("char")) continue;
            String key = f[1];
            if (poses.containsKey(key)) {
                String[] better = Art.fullLengthFront(L, f, poses.get(key));
                if (better != null) { f = better; lines.set(li, String.join("|", better)); System.out.println(key + ": front ← " + better[2]); }
            }
            // the user's whole standing views
            String[] own = new String[Figure3D.VIEW_ANGLES.length];
            for (String l : lines) {
                String[] v = l.trim().split("\\|");
                if (v.length < 4 || !v[0].equals("view") || !v[1].equals(key)) continue;
                int i = Figure3D.viewIndex(Float.parseFloat(v[2].trim()));
                if (i < 0 || !new File(dir, v[3]).exists()) continue;
                int[] d = L.decode(v[3], 320);
                boolean[] fr = d == null ? null : Art.framingOf(d, -1);
                if (fr == null || (!fr[0] && !fr[1])) own[i] = v[3];
            }
            int[] d = L.decode(f[2], 1600);
            if (d == null) continue;
            int[] px = new int[d[0] * d[1]];
            System.arraycopy(d, 2, px, 0, px.length);
            Cutout.Result cut = Cutout.process(px, d[0], d[1], false);
            float[] face = null;
            if (f.length >= 11 && Float.parseFloat(f[3]) > 0) {
                face = new float[9];
                for (int i = 0; i < 8; i++) face[i] = Float.parseFloat(f[3 + i]);
            } else if (cut.faceFound) face = new float[]{cut.mouthX, cut.mouthY, cut.mouthW, cut.eyeLX, cut.eyeY, cut.eyeRX, cut.eyeY, cut.eyeR, 0};
            Figure3D.Model m = Figure3D.build(cut.px, cut.w, cut.h, face, new Look());
            Doll3D.Result[] views = Figure3D.views(m, 1100);
            for (int i = 0; i < views.length; i++) {
                if (own[i] != null || views[i] == null) continue;
                Doll3D.Result v = views[i];
                String name = "made_" + Math.abs((key + i).hashCode()) + ".png";
                BufferedImage im = new BufferedImage(v.w, v.h, BufferedImage.TYPE_INT_ARGB);
                im.setRGB(0, 0, v.w, v.h, v.px, 0, v.w);
                javax.imageio.ImageIO.write(im, "png", new File(dir, name));
                String pts = v.faceKnown ? String.format(Locale.US, "%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f", v.mouthX, v.mouthY, v.mouthHW, v.eyeLX, v.eyeLY, v.eyeRX, v.eyeRY, v.eyeR) : "0|0|0|0|0|0|0|0";
                int ang = (int) Figure3D.VIEW_ANGLES[i];
                lines.removeIf(l -> { String[] x = l.trim().split("\\|"); return x.length >= 4 && x[0].equals("view") && x[1].equals(key) && Figure3D.viewIndex(Float.parseFloat(x[2].trim())) == Figure3D.viewIndex(ang); });
                lines.add("view|" + key + "|" + ang + "|" + name + "|" + pts);
                System.out.println(key + ": made " + Figure3D.VIEW_NAMES[i] + " view " + name);
            }
        }
        Files.write(castF.toPath(), lines);
    }
}
