package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import javax.imageio.ImageIO;

/** Renders every character of a script side by side, for visual checking. */
public class Lineup {
    public static void main(String[] a) throws Exception {
        Story st = ScriptParser.parse(new String(Files.readAllBytes(Paths.get(a[0])), "UTF-8"));
        int n = st.characters.size();
        BufferedImage img = new BufferedImage(170 * n, 560, BufferedImage.TYPE_INT_ARGB);
        AwtGfx g = new AwtGfx(img);
        g.color(0xFFE9F2E0); g.rect(0, 0, img.getWidth(), img.getHeight());
        Pose p = new Pose();
        for (int i = 0; i < n; i++) {
            Story.CharacterDef c = st.characters.get(i);
            p.reset();
            p.facing = 1; p.time = 1.3f; p.seed = i;
            p.emotion = a.length > 1 ? Integer.parseInt(a[1]) : Pose.HAPPY;
            p.mouth = a.length > 2 ? Float.parseFloat(a[2]) : 0;
            g.save();
            g.translate(85 + i * 170, 500);
            Puppet.draw(g, c.look, p, Puppet.heightPx(c.look, 330));
            g.restore();
            g.color(0xFF222222);
            g.text(c.displayName, 85 + i * 170, 540, 20, true, 1);
        }
        ImageIO.write(img, "png", new File(a.length > 3 ? a[3] : "/tmp/lineup.png"));
    }
}
