package com.tarun.kahani.jvm;

import com.tarun.kahani.core.Angles;
import com.tarun.kahani.core.Cutout;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/** Checks the angle guess of pictures and the splitting of a sheet: AnglesCheck pic1 [pic2…]; a sheet is made from the first two. */
public final class AnglesCheck {
    public static void main(String[] a) throws Exception {
        int[][] pxs = new int[a.length][]; int[] ws = new int[a.length], hs = new int[a.length];
        for (int i = 0; i < a.length; i++) {
            BufferedImage im = ImageIO.read(new File(a[i]));
            ws[i] = im.getWidth(); hs[i] = im.getHeight();
            pxs[i] = im.getRGB(0, 0, ws[i], hs[i], null, 0, ws[i]);
            Cutout.Result r = Cutout.process(pxs[i].clone(), ws[i], hs[i], false);
            System.out.println(new File(a[i]).getName() + ": " + Angles.debug(r) + " -> " + Angles.name(Angles.guess(r)));
        }
        if (a.length >= 2) {
            // a sheet: the first two side by side on white
            int h = Math.max(hs[0], hs[1]), w = ws[0] + ws[1] + 40;
            int[] sheet = new int[w * h];
            java.util.Arrays.fill(sheet, 0xFFFFFFFF);
            for (int k = 0; k < 2; k++) for (int y = 0; y < hs[k]; y++) for (int x = 0; x < ws[k]; x++) {
                int c = pxs[k][y * ws[k] + x];
                if ((c >>> 24) > 100) sheet[y * w + x + (k == 0 ? 0 : ws[0] + 40)] = c | 0xFF000000;
            }
            List<Angles.Piece> ps = Angles.split(sheet, w, h);
            System.out.println("sheet " + w + "x" + h + " -> " + ps.size() + " pieces");
            for (Angles.Piece p : ps) System.out.println("  piece at " + p.x0 + "," + p.y0 + " " + p.w + "x" + p.h);
        }
    }
}
