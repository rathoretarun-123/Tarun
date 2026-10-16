package com.tarun.kahani.jvm;

import com.tarun.kahani.core.Cutout;
import com.tarun.kahani.core.Doll3D;
import com.tarun.kahani.core.Figure3D;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;

/**
 * Renders the views of a character from its own picture (Figure3D): FigureSheet assetsDir outDir [name…].
 * Reads cast.txt for the face points; writes <name>_045.png, _090.png, _180.png and a sheet per character.
 */
public final class FigureSheet {
    public static void main(String[] args) throws Exception {
        File assets = new File(args[0]), out = new File(args[1]);
        out.mkdirs();
        Story story = ScriptParser.parse(new String(java.nio.file.Files.readAllBytes(new File(assets.getParentFile(), "sample_story.txt").toPath()), "UTF-8"));
        String cast = new String(java.nio.file.Files.readAllBytes(new File(assets, "cast.txt").toPath()), "UTF-8");
        for (String line : cast.split("\n")) {
            String[] f = line.trim().split("\\|");
            if (f.length < 3 || !f[0].equals("char")) continue;
            if (args.length > 2) {
                boolean want = false;
                for (int i = 2; i < args.length; i++) if (f[1].contains(args[i]) || f[2].contains(args[i])) want = true;
                if (!want) continue;
            }
            Story.CharacterDef c = ScriptParser.resolve(story, f[1]);
            Look look = c != null && c.look != null ? c.look : new Look();
            BufferedImage bi = ImageIO.read(new File(assets, f[2]));
            int w = bi.getWidth(), h = bi.getHeight();
            int[] px = bi.getRGB(0, 0, w, h, null, 0, w);
            long t0 = System.currentTimeMillis();
            Cutout.Result r = Cutout.process(px, w, h, look.kind == Look.ANIMAL || look.kind == Look.BIRD);
            float[] face = null;
            if (f.length >= 11) {
                face = new float[9];
                for (int i = 0; i < 8; i++) face[i] = Float.parseFloat(f[3 + i].trim());
                face[8] = f.length >= 12 ? Float.parseFloat(f[11].trim()) : 0;
            }
            Figure3D.Model m = Figure3D.build(r.px, r.w, r.h, face, look);
            long t1 = System.currentTimeMillis();
            String base = f[2].replaceAll("\\.[a-z]+$", "");
            Doll3D.Result[] views = Figure3D.views(m, 900);
            long t2 = System.currentTimeMillis();
            int sheetW = r.w * 900 / r.h + 40, sheetH = 940;
            for (Doll3D.Result v : views) sheetW += v.w + 40;
            BufferedImage sheet = new BufferedImage(sheetW, sheetH, BufferedImage.TYPE_INT_ARGB);
            java.awt.Graphics2D g = sheet.createGraphics();
            g.setColor(new java.awt.Color(0xE4EAF0));
            g.fillRect(0, 0, sheetW, sheetH);
            int x = 20;
            BufferedImage orig = new BufferedImage(r.w, r.h, BufferedImage.TYPE_INT_ARGB);
            orig.setRGB(0, 0, r.w, r.h, r.px, 0, r.w);
            int ow = r.w * 900 / r.h;
            g.drawImage(orig, x, 20, ow, 900, null);
            x += ow + 40;
            for (int i = 0; i < views.length; i++) {
                Doll3D.Result v = views[i];
                BufferedImage vi = new BufferedImage(v.w, v.h, BufferedImage.TYPE_INT_ARGB);
                vi.setRGB(0, 0, v.w, v.h, v.px, 0, v.w);
                ImageIO.write(vi, "png", new File(out, String.format("%s_%03d.png", base, (int) Figure3D.VIEW_ANGLES[i])));
                g.drawImage(vi, x, 20 + (900 - v.h), null);
                if (v.faceKnown) {
                    g.setColor(java.awt.Color.RED);
                    g.fillOval(x + Math.round(v.eyeLX * v.w) - 4, 20 + (900 - v.h) + Math.round(v.eyeLY * v.h) - 4, 8, 8);
                    g.fillOval(x + Math.round(v.eyeRX * v.w) - 4, 20 + (900 - v.h) + Math.round(v.eyeRY * v.h) - 4, 8, 8);
                    g.fillOval(x + Math.round(v.mouthX * v.w) - 4, 20 + (900 - v.h) + Math.round(v.mouthY * v.h) - 4, 8, 8);
                }
                x += v.w + 40;
            }
            g.dispose();
            ImageIO.write(sheet, "png", new File(out, base + "_views.png"));
            System.out.printf("%s: cut-out %dx%d, runs %d, face %s, legs %s, model %d ms, three views %d ms%n", f[1], r.w, r.h, m.runs(), m.faceKnown, m.legs, t1 - t0, t2 - t1);
        }
    }
}
