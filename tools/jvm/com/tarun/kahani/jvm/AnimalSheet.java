package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import javax.imageio.ImageIO;

/**
 * Animal pictures with their rig: tail, ears, jaw (lip-sync), head and legs in many feelings, for checking by eye.
 * Each animal is first drawn as a cartoon on a white page and saved as a picture, then loaded back like a
 * user's photo (cut out, rigged). args: workDir out.png
 */
public class AnimalSheet {
    public static void main(String[] a) throws Exception {
        String dir = a[0];
        new File(dir).mkdirs();
        String[][] beasts = {{"शेरू", "शेरू एक बड़ा शेर है, सुनहरी अयाल वाला।"}, {"मोती", "मोती एक भूरा कुत्ता है।"},
                {"गौरी", "गौरी एक सफ़ेद गाय है।"}, {"बादल", "बादल एक काला घोड़ा है।"}};
        String[] cols = {"neutral", "happy", "sad", "angry", "scared", "curious", "talk A", "lie down", "walk A", "walk B", "nod"};
        MakeFilm.AwtLoader L = new MakeFilm.AwtLoader(dir);
        Art art = new Art();
        Story.CharacterDef[] cs = new Story.CharacterDef[beasts.length];
        for (int i = 0; i < beasts.length; i++) {
            Story.CharacterDef c = new Story.CharacterDef();
            c.id = "a" + i; c.displayName = beasts[i][0]; c.fullName = beasts[i][0]; c.description = beasts[i][1];
            c.look = LookDesigner.design(c, null);
            cs[i] = c;
            // the "photo": the cartoon on a white page
            BufferedImage ph = new BufferedImage(700, 520, BufferedImage.TYPE_INT_RGB);
            Graphics2D pg = ph.createGraphics();
            pg.setColor(Color.WHITE); pg.fillRect(0, 0, 700, 520);
            AwtGfx g = new AwtGfx(ph);
            Pose p = new Pose(); p.reset(); p.facing = 1; p.time = 0.4f;
            g.save(); g.translate(350, 470); Puppet.draw(g, c.look, p, 330); g.restore();
            ImageIO.write(ph, "png", new File(dir, "animal" + i + ".png"));
            Art.Sprite sp = Art.makeSprite(L, "animal" + i + ".png", 1100, true);
            sp.rig = Rig.build(sp.pixelsForSampling, sp, c.look, L);
            Rig r = sp.rig;
            System.out.println(c.displayName + " kind=" + c.look.kind + " species=" + c.look.species + " size=" + sp.w + "x" + sp.h + " face=" + sp.faceKnown
                    + (r == null ? " rig=none" : String.format(" animal=%b head=%d tail=%.2f,%.2f legTop=%.2f headX=%.2f-%.2f headY=%.2f-%.2f neck=%.2f jaw=%.2f,%.2f",
                    r.animal, r.headSide, r.tailX, r.tailY, r.legTop, r.headX0, r.headX1, r.headTop, r.headBottom, r.neckX, r.jawX, r.jawY) + " hasTail=" + r.hasTail + " eyesOnHead=" + r.eyesOnHead + " mouthOnHead=" + r.mouthOnHead));
            art.sprites.put(c.id, sp);
        }
        Renderer rd = new Renderer(null, art);
        int cw = 300, ch = 280;
        BufferedImage img = new BufferedImage(cw * cols.length, ch * beasts.length, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(new Color(0xDDE8D8)); g2.fillRect(0, 0, img.getWidth(), img.getHeight());
        AwtGfx g = new AwtGfx(img);
        Pose p = new Pose();
        for (int row = 0; row < beasts.length; row++) {
            for (int col = 0; col < cols.length; col++) {
                p.reset();
                p.time = 2.0f; p.seed = 3; p.facing = 1;
                switch (col) {
                    case 1: p.emotion = Pose.HAPPY; break;
                    case 2: p.emotion = Pose.SAD; break;
                    case 3: p.emotion = Pose.ANGRY; break;
                    case 4: p.emotion = Pose.SCARED; break;
                    case 5: p.emotion = Pose.CURIOUS; break;
                    case 6: p.mouth = 0.9f; break;
                    case 7: p.body = Pose.LIE; break;
                    case 8: p.walk = (float) (Math.PI / 2); p.walkAmt = 1; break;
                    case 9: p.walk = (float) (-Math.PI / 2); p.walkAmt = 1; break;
                    case 10: p.nod = 1; break;
                    default:
                }
                g.save();
                g.translate(col * cw + cw / 2f, row * ch + ch - 20);
                rd.drawPosed(g, cs[row], p, ch - 70);
                g.restore();
                if (row == 0) { g2.setColor(Color.DARK_GRAY); g2.setFont(new Font("SansSerif", Font.BOLD, 18)); g2.drawString(cols[col], col * cw + 10, 22); }
            }
        }
        ImageIO.write(img, "png", new File(a[1]));
    }
}
