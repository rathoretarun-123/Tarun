package com.tarun.kahani.jvm;

import com.tarun.kahani.core.Doll3D;
import com.tarun.kahani.core.Glb;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/** Renders a GLB model from four sides: GlbSheet model.glb out.png */
public final class GlbSheet {
    public static void main(String[] args) throws Exception {
        byte[] glb = java.nio.file.Files.readAllBytes(new File(args[0]).toPath());
        long t0 = System.currentTimeMillis();
        Glb.Model m = Glb.load(glb, new Glb.ImageDecoder() {
            public int[] decode(byte[] bytes) {
                try {
                    BufferedImage bi = ImageIO.read(new java.io.ByteArrayInputStream(bytes));
                    if (bi == null) return null;
                    int w = bi.getWidth(), h = bi.getHeight();
                    int[] out = new int[w * h + 2];
                    out[0] = w; out[1] = h;
                    bi.getRGB(0, 0, w, h, out, 2, w);
                    return out;
                } catch (Exception e) { return null; }
            }
        });
        System.out.println("loaded: " + m.note + " wide " + m.wide + " in " + (System.currentTimeMillis() - t0) + " ms");
        float[] angles = {0, -45, -90, 180};
        Doll3D.Result[] v = new Doll3D.Result[angles.length];
        int W = 40, H = 0;
        for (int i = 0; i < angles.length; i++) { v[i] = Glb.render(m, 600, angles[i]); W += v[i].w + 40; H = Math.max(H, v[i].h); }
        System.out.println("rendered in " + (System.currentTimeMillis() - t0) + " ms");
        BufferedImage sheet = new BufferedImage(W, H + 40, BufferedImage.TYPE_INT_ARGB);
        java.awt.Graphics2D g = sheet.createGraphics();
        g.setColor(new java.awt.Color(0xE4EAF0));
        g.fillRect(0, 0, W, H + 40);
        int x = 20;
        for (Doll3D.Result r : v) {
            BufferedImage bi = new BufferedImage(r.w, r.h, BufferedImage.TYPE_INT_ARGB);
            bi.setRGB(0, 0, r.w, r.h, r.px, 0, r.w);
            g.drawImage(bi, x, 20 + H - r.h, null);
            x += r.w + 40;
        }
        g.dispose();
        ImageIO.write(sheet, "png", new File(args[1]));
    }
}
