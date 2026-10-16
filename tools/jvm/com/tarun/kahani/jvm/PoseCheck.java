package com.tarun.kahani.jvm;

import com.tarun.kahani.core.Angles;
import com.tarun.kahani.core.Cutout;
import com.tarun.kahani.core.PoseSense;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.List;
import javax.imageio.ImageIO;

/** Splits sheets and prints the pose and emotion read from every figure, with the measures: PoseCheck [-m] sheet.jpg … */
public final class PoseCheck {
    public static void main(String[] a) throws Exception {
        boolean measures = a.length > 0 && a[0].equals("-m");
        for (String f : a) {
            if (f.startsWith("-")) continue;
            BufferedImage im = ImageIO.read(new File(f));
            int w = im.getWidth(), h = im.getHeight();
            int[] px = im.getRGB(0, 0, w, h, null, 0, w);
            boolean beast = f.contains("beast");
            List<Angles.Piece> figs = Angles.figures(Angles.split(px, w, h), w, h);
            int standH = 0;
            for (Angles.Piece p : figs) standH = Math.max(standH, p.h);
            System.out.println(new File(f).getName() + " (" + figs.size() + " figures, tallest " + standH + ")");
            int k = 0;
            for (Angles.Piece p : figs) {
                Cutout.Result r = Cutout.process(p.px.clone(), p.w, p.h, beast);
                PoseSense.Tag t = PoseSense.tag(r, standH, beast);
                System.out.println("  " + (++k) + ": " + t + (measures ? "   [" + t.m + "]" : ""));
            }
        }
    }
}
