package com.tarun.kahani.jvm;

import com.tarun.kahani.core.Angles;
import com.tarun.kahani.core.Cutout;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/** Splits sheets of figures and reports the pieces (and their guessed angles): SheetCheck sheet.png [outDir] … */
public final class SheetCheck {
    public static void main(String[] a) throws Exception {
        File out = a.length > 1 && new File(a[1]).isDirectory() ? new File(a[1]) : null;
        for (String f : a) {
            if (out != null && new File(f).isDirectory()) continue;
            BufferedImage im = ImageIO.read(new File(f));
            int w = im.getWidth(), h = im.getHeight();
            int[] px = im.getRGB(0, 0, w, h, null, 0, w);
            long t0 = System.currentTimeMillis();
            List<Angles.Piece> all = Angles.split(px, w, h);
            List<Angles.Piece> figs = Angles.figures(all, w, h);
            System.out.println(new File(f).getName() + " " + w + "x" + h + ": " + all.size() + " pieces, " + figs.size() + " figures (" + (System.currentTimeMillis() - t0) + " ms)");
            int k = 0;
            for (Angles.Piece p : figs) {
                Cutout.Result r = Cutout.process(p.px.clone(), p.w, p.h, false);
                System.out.println("  " + (++k) + ": at " + p.x0 + "," + p.y0 + " " + p.w + "x" + p.h + " -> " + Angles.name(Angles.guess(r)));
                if (out != null) {
                    BufferedImage o = new BufferedImage(p.w, p.h, BufferedImage.TYPE_INT_ARGB);
                    o.setRGB(0, 0, p.w, p.h, p.px, 0, p.w);
                    ImageIO.write(o, "png", new File(out, new File(f).getName().replaceAll("\\..*$", "") + "_" + k + ".png"));
                }
            }
        }
    }
}
