package com.tarun.kahani.jvm;

import com.tarun.kahani.core.Angles;
import com.tarun.kahani.core.Cutout;
import com.tarun.kahani.core.PoseSense;

import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.*;
import javax.imageio.ImageIO;

/**
 * v42: the angle reading measured against the user's sheets labelled by eye (tools/testdata/labels/figures.tsv):
 * every figure cut as the phone cuts it, read as the upload reads it (PoseSense.tag), compared with its label.
 * AnglesLabelled testdataDir [-v]
 */
public final class AnglesLabelled {
    public static void main(String[] a) throws Exception {
        File root = new File(a[0]);
        Map<String, String[]> lab = new LinkedHashMap<String, String[]>();
        for (String l : Files.readAllLines(new File(root, "labels/figures.tsv").toPath(), StandardCharsets.UTF_8)) {
            if (l.startsWith("#") || l.trim().isEmpty()) continue;
            String[] f = l.split("\t");
            lab.put(f[0], new String[]{f[1], f[2]});
        }
        Set<String> sheets = new LinkedHashSet<String>();
        for (String k : lab.keySet()) sheets.add(k.substring(0, k.indexOf('#')));
        int ok = 0, n = 0, wholeOk = 0, wholeN = 0;
        Map<String, Integer> confusion = new TreeMap<String, Integer>();
        for (String sh : sheets) {
            BufferedImage im = ImageIO.read(new File(root, sh));
            float s = Math.min(1f, 3000f / Math.max(im.getWidth(), im.getHeight()));
            int w = Math.round(im.getWidth() * s), h = Math.round(im.getHeight() * s);
            BufferedImage sm = new BufferedImage(w, h, BufferedImage.TYPE_INT_ARGB);
            Graphics2D g = sm.createGraphics();
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
            g.drawImage(im, 0, 0, w, h, null);
            g.dispose();
            int[] px = sm.getRGB(0, 0, w, h, null, 0, w);
            List<Angles.Piece> ps = Angles.figures(Angles.split(px, w, h), w, h, false);
            for (int k = 0; k < ps.size(); k++) {
                String[] want = lab.get(sh + "#" + k);
                if (want == null) continue;
                Angles.Piece p = ps.get(k);
                Cutout.Result r = Cutout.process(p.px, p.w, p.h, false);
                String got = code(PoseSense.tag(r, 0, false).angle);
                boolean right = Arrays.asList(want[0].split("/")).contains(got);
                n++; if (right) ok++;
                if (want[1].equals("W") || want[1].equals("K")) { wholeN++; if (right) wholeOk++; }
                if (!right) {
                    confusion.merge(want[0] + " read as " + got, 1, Integer::sum);
                    if (a.length > 1) System.out.println("  " + sh + "#" + k + " " + want[0] + " -> " + got + " (" + want[1] + ")");
                }
            }
        }
        System.out.printf("ANGLES %d of %d figures read right (%.0f%%), whole standing figures %d of %d%n", ok, n, 100f * ok / Math.max(1, n), wholeOk, wholeN);
        for (Map.Entry<String, Integer> e : confusion.entrySet()) System.out.println("  " + e.getKey() + ": " + e.getValue());
    }

    static String code(float a) {
        a = Math.abs(a);
        return a < 1 ? "F" : Math.abs(a - 45) < 1 ? "Q" : Math.abs(a - 90) < 1 ? "S" : "B";
    }
}
