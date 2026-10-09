package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import javax.imageio.ImageIO;

/**
 * Renders every character of a script with Studio 3D (and every place, when asked) into PNG files, for checking by
 * eye. Usage: Studio3Sheet script.txt outDir [places]
 */
public class Studio3Sheet {
    public static void main(String[] a) throws Exception {
        Story st = ScriptParser.parse(new String(Files.readAllBytes(Paths.get(a[0])), "UTF-8"));
        File out = new File(a[1]);
        out.mkdirs();
        long t0 = System.currentTimeMillis();
        int i = 0;
        for (Story.CharacterDef c : st.cast()) {
            long t = System.currentTimeMillis();
            Doll3D.Result r = Doll3D.make(c.look, 768, i);
            BufferedImage img = new BufferedImage(r.w, r.h, BufferedImage.TYPE_INT_ARGB);
            img.setRGB(0, 0, r.w, r.h, r.px, 0, r.w);
            ImageIO.write(img, "png", new File(out, String.format("char_%02d.png", i)));
            Look lk = c.look;
            System.out.printf("   look kind %d outfit %d female %b hair %d headwear %d species %d robot %b%n", lk.kind, lk.outfit, lk.female, lk.hair, lk.headwear, lk.species, lk.robot);
            System.out.printf("%2d %-16s %4dx%-4d eyes (%.2f,%.2f) (%.2f,%.2f) mouth (%.2f,%.2f) hw %.3f eyeR %.3f turban %.2f  %d ms%n", i, c.shown(), r.w, r.h,
                    r.eyeLX, r.eyeLY, r.eyeRX, r.eyeRY, r.mouthX, r.mouthY, r.mouthHW, r.eyeR, r.turbanY, System.currentTimeMillis() - t);
            i++;
        }
        if (a.length > 2 && a[2].startsWith("views")) {
            // the master sheet (front, three-quarter, side, back) and the expressions of one character: views:<index>
            int idx = Integer.parseInt(a[2].substring(6));
            Story.CharacterDef c = st.cast().get(idx);
            Doll3D.Result r = Doll3D.masterSheet(c.look, 640, idx);
            BufferedImage img = new BufferedImage(r.w, r.h, BufferedImage.TYPE_INT_RGB);
            img.setRGB(0, 0, r.w, r.h, r.px, 0, r.w);
            ImageIO.write(img, "png", new File(out, String.format("views_%02d.png", idx)));
            int[] emos = {Pose.NEUTRAL, Pose.HAPPY, Pose.SAD, Pose.ANGRY, Pose.SURPRISED};
            for (int e = 0; e < emos.length; e++) {
                Doll3D.Result v = Doll3D.make(c.look, 480, idx, 0, emos[e]);
                BufferedImage im = new BufferedImage(v.w, v.h, BufferedImage.TYPE_INT_ARGB);
                im.setRGB(0, 0, v.w, v.h, v.px, 0, v.w);
                ImageIO.write(im, "png", new File(out, String.format("expr_%02d_%d.png", idx, e)));
            }
            System.out.println("views of " + c.shown() + " -> " + out);
            return;
        }
        if (a.length > 2) {
            int n = 0;
            for (Story.Scene sc : st.scenes) {
                String where = sc.setting.length() > 0 ? sc.setting : sc.title;
                int set = Sets.detect(sc.title + " " + where);
                int tod = Sets.detectTime(sc.title + " " + where, Sets.DAY);
                long t = System.currentTimeMillis();
                Set3D.Result r = Set3D.make(set, tod, 1280, 720, n);
                BufferedImage img = new BufferedImage(r.w, r.h, BufferedImage.TYPE_INT_RGB);
                img.setRGB(0, 0, r.w, r.h, r.px, 0, r.w);
                ImageIO.write(img, "png", new File(out, String.format("place_%02d.png", n)));
                System.out.printf("place %d %-22s set %d tod %d ground %.2f  %d ms%n", n, Sets.label(set), set, tod, r.ground, System.currentTimeMillis() - t);
                n++;
            }
        }
        System.out.println("done in " + (System.currentTimeMillis() - t0) + " ms -> " + out);
    }
}
