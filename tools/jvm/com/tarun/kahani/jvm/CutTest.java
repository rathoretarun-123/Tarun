package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Cuts out character pictures and marks the detected mouth and eyes, for checking. */
public class CutTest {
    public static void main(String[] a) throws Exception {
        int n = a.length - 1;
        BufferedImage sheet = new BufferedImage(260 * n, 520, BufferedImage.TYPE_INT_RGB);
        Graphics2D sg = sheet.createGraphics();
        sg.setColor(new Color(0x88CC88)); sg.fillRect(0, 0, sheet.getWidth(), 520);
        for (int i = 0; i < n; i++) {
            BufferedImage im = ImageIO.read(new File(a[i + 1]));
            int w = im.getWidth(), h = im.getHeight();
            int[] px = im.getRGB(0, 0, w, h, null, 0, w);
            long t0 = System.currentTimeMillis();
            Cutout.Result r = Cutout.process(px, w, h);
            long t1 = System.currentTimeMillis();
            BufferedImage c = new BufferedImage(r.w, r.h, BufferedImage.TYPE_INT_ARGB);
            c.setRGB(0, 0, r.w, r.h, r.px, 0, r.w);
            Graphics2D g = c.createGraphics();
            g.setStroke(new BasicStroke(Math.max(3, r.w / 150)));
            g.setColor(Color.RED);
            int mw = (int) (r.mouthW * r.w);
            g.drawOval((int) (r.mouthX * r.w) - mw, (int) (r.mouthY * r.h) - mw / 3, mw * 2, mw * 2 / 3);
            g.setColor(Color.BLUE);
            int er = (int) (r.eyeR * r.w);
            g.drawOval((int) (r.eyeLX * r.w) - er, (int) (r.eyeY * r.h) - er, er * 2, er * 2);
            g.drawOval((int) (r.eyeRX * r.w) - er, (int) (r.eyeY * r.h) - er, er * 2, er * 2);
            g.setColor(Color.MAGENTA);
            g.drawLine(0, (int) (r.faceTop * r.h), r.w, (int) (r.faceTop * r.h));
            g.drawLine(0, (int) (r.chinY * r.h), r.w, (int) (r.chinY * r.h));
            float s = 500f / r.h;
            sg.drawImage(c, i * 260 + 130 - (int) (r.w * s / 2), 10, (int) (r.w * s), 500, null);
            System.out.printf("%s %dx%d face=%b mouth=(%.3f,%.3f,w%.3f) eyes=(%.3f/%.3f,%.3f) top=%.3f ft=%.3f chin=%.3f skin=%08X %dms%n",
                new File(a[i + 1]).getName(), r.w, r.h, r.faceFound, r.mouthX, r.mouthY, r.mouthW, r.eyeLX, r.eyeRX, r.eyeY, r.headTop, r.faceTop, r.chinY, r.skin, t1 - t0);
        }
        ImageIO.write(sheet, "png", new File(a[0]));
    }
}
