package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/** Shows the head area of a cut-out with a labelled grid (normalised coords) for calibrating landmarks. */
public class HeadGrid {
    public static void main(String[] a) throws Exception {
        float y0 = Float.parseFloat(a[2]), y1 = Float.parseFloat(a[3]);
        float x0 = a.length > 4 ? Float.parseFloat(a[4]) : 0.2f, x1 = a.length > 5 ? Float.parseFloat(a[5]) : 0.8f;
        BufferedImage im = ImageIO.read(new File(a[1]));
        int w = im.getWidth(), h = im.getHeight();
        Cutout.Result r = Cutout.process(im.getRGB(0, 0, w, h, null, 0, w), w, h);
        BufferedImage c = new BufferedImage(r.w, r.h, BufferedImage.TYPE_INT_ARGB);
        c.setRGB(0, 0, r.w, r.h, r.px, 0, r.w);
        int sx = (int) (x0 * r.w), sy = (int) (y0 * r.h), sw = (int) ((x1 - x0) * r.w), sh = (int) ((y1 - y0) * r.h);
        int outW = 700, outH = (int) (700f * sh / sw);
        BufferedImage o = new BufferedImage(outW, outH, BufferedImage.TYPE_INT_RGB);
        Graphics2D g = o.createGraphics();
        g.setColor(Color.GRAY); g.fillRect(0, 0, outW, outH);
        g.drawImage(c, 0, 0, outW, outH, sx, sy, sx + sw, sy + sh, null);
        g.setFont(new Font("SansSerif", Font.BOLD, 13));
        for (int i = 0; i <= 100; i++) {
            float v = i / 100f;
            if (v >= y0 && v <= y1 && i % 1 == 0) {
                int yy = (int) ((v - y0) / (y1 - y0) * outH);
                g.setColor(i % 5 == 0 ? new Color(255, 0, 0, 170) : new Color(255, 255, 0, 60));
                g.drawLine(0, yy, outW, yy);
                if (i % 5 == 0) g.drawString(String.format("%.2f", v), 2, yy - 2);
            }
            if (v >= x0 && v <= x1) {
                int xx = (int) ((v - x0) / (x1 - x0) * outW);
                g.setColor(i % 5 == 0 ? new Color(0, 0, 255, 170) : new Color(0, 255, 255, 60));
                g.drawLine(xx, 0, xx, outH);
                if (i % 5 == 0) g.drawString(String.format("%.2f", v), xx + 2, 14);
            }
        }
        ImageIO.write(o, "png", new File(a[0]));
    }
}
