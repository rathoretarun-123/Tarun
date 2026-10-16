package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;
import javax.imageio.ImageIO;

/**
 * Draws picture characters with their rig in many poses and feelings, for checking by eye.
 * args: assetsDir/sample out.png [character names or numbers…]
 */
public class RigSheet {
    public static void main(String[] a) throws Exception {
        String assets = a[0];
        Story story = ScriptParser.parse(new String(Files.readAllBytes(Paths.get(assets, "..", "sample_story.txt")), "UTF-8"));
        Art art = Art.fromManifest(new String(Files.readAllBytes(Paths.get(assets, "cast.txt")), "UTF-8"), story, new MakeFilm.AwtLoader(assets));
        Renderer r = new Renderer(null, art);
        String[] cols = {"neutral", "happy", "laugh", "sad", "angry", "scared", "surprised", "talk", "talk ee", "talk oo", "point", "walk A", "walk B", "sit", "bow", "wave", "twirl"};
        int cw = 230, ch = 520;
        java.util.List<Story.CharacterDef> who = new java.util.ArrayList<Story.CharacterDef>();
        for (int i = 2; i < a.length; i++) {
            Story.CharacterDef found = a[i].matches("\\d+") ? story.characters.get(Integer.parseInt(a[i])) : ScriptParser.resolve(story, a[i]);
            if (found == null) for (Story.CharacterDef c : story.characters) if (c.displayName.contains(a[i]) || (c.fullName != null && c.fullName.contains(a[i]))) found = c;
            if (found == null) { System.out.println("not found: " + a[i] + " in " + story.characters); continue; }
            who.add(found);
        }
        BufferedImage img = new BufferedImage(cw * cols.length, ch * who.size(), BufferedImage.TYPE_INT_RGB);
        Graphics2D g2 = img.createGraphics();
        g2.setColor(new Color(0xDDE8D8)); g2.fillRect(0, 0, img.getWidth(), img.getHeight());
        AwtGfx g = new AwtGfx(img);
        Pose p = new Pose();
        for (int row = 0; row < who.size(); row++) {
            Story.CharacterDef c = who.get(row);
            Art.Sprite sp = art.sprites.get(c.id);
            Rig rig = sp == null ? null : sp.rig;
            System.out.println(c.displayName + " rig=" + (rig == null ? "none" : String.format("chin=%.2f shoulder=%.2f/%.2f hip=%.2f legs=%b gap=%.2f face=%b armEnd=%.2f armInner=%.2f",
                    rig.chinY, rig.shoulderY, rig.shoulderHalf, rig.hipY, rig.legs, rig.gapX, rig.face, rig.armEnd, rig.armInner) + " armUp=" + rig.armUp[0] + "/" + rig.armUp[1]));
            for (int col = 0; col < cols.length; col++) {
                p.reset();
                p.time = 2.0f; p.seed = 3; p.facing = 1;
                switch (col) {
                    case 1: p.emotion = Pose.HAPPY; break;
                    case 2: p.emotion = Pose.LAUGH; break;
                    case 3: p.emotion = Pose.SAD; break;
                    case 4: p.emotion = Pose.ANGRY; p.armR = 40; break;
                    case 5: p.emotion = Pose.SCARED; break;
                    case 6: p.emotion = Pose.SURPRISED; break;
                    case 7: p.mouth = 0.6f; p.armR = 45; p.time = 2.3f; break;
                    case 8: p.mouth = 0.45f; p.mouthWide = 1f; break;
                    case 9: p.mouth = 0.6f; p.mouthWide = 0f; break;
                    case 10: p.armR = 95; p.emotion = Pose.DETERMINED; break;
                    case 11: p.walk = (float) (Math.PI / 2); p.walkAmt = 1; p.armL = 50; p.armR = 0; break;
                    case 12: p.walk = (float) (-Math.PI / 2); p.walkAmt = 1; p.armL = 0; p.armR = 50; break;
                    case 13: p.body = Pose.SIT; p.sit = 1; break;
                    case 14: p.tilt = 18; p.nod = 0.8f; p.armL = 55; p.armR = 55; break;
                    case 15: p.wave = 1; p.armR = 150; p.emotion = Pose.HAPPY; break;
                    case 16: p.twirl = true; p.time = 2.45f; p.emotion = Pose.PROUD; break;
                    default:
                }
                g.save();
                g.translate(col * cw + cw / 2f, row * ch + ch - 20);
                r.drawPosed(g, c, p, ch - 60);
                g.restore();
                if (row == 0) { g2.setColor(Color.DARK_GRAY); g2.setFont(new Font("SansSerif", Font.BOLD, 18)); g2.drawString(cols[col], col * cw + 10, 22); }
            }
        }
        ImageIO.write(img, "png", new File(a[1]));
    }
}
