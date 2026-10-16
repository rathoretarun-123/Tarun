package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.nio.file.*;

/** Prints what the parser and look designer understood from a script. */
public class ParseDump {
    public static void main(String[] a) throws Exception {
        String s = new String(Files.readAllBytes(Paths.get(a[0])), "UTF-8");
        Story st = ScriptParser.parse(s);
        System.out.println("TITLE=" + st.title + " | SUB=" + st.subtitle + " | hindi=" + st.hindi + " word=" + st.sceneWord);
        for (Story.CharacterDef c : st.characters) {
            Look l = c.look;
            System.out.printf("%s disp=%s full=%s age=%d aliases=%s kind=%d fem=%b h=%.2f outfit=%d prim=%08X sec=%08X hair=%d head=%d hc=%08X band=%08X must=%d bindi=%08X tilak=%08X sword=%b spear=%b wand=%b hero=%b%n",
                c.id, c.displayName, c.fullName, c.age, c.aliases, l.kind, l.female, l.height, l.outfit, l.primary, l.secondary, l.hair, l.headwear, l.headColor, l.headBand, l.mustache, l.bindi, l.tilak, l.sword, l.spear, l.wand, l.hero);
        }
        for (Story.PlaceDef p : st.places) System.out.println("PLACE " + p.name + " :: " + p.description.length());
        for (Story.Scene sc : st.scenes) {
            System.out.println("== " + sc.heading + " : " + sc.title + " | setting=" + sc.setting);
            for (Story.Beat b : sc.beats) {
                if (b.type == Story.Beat.DIALOGUE) System.out.println("  D[" + b.speaker.displayName + "] (" + b.manner + ") " + b.text.substring(0, Math.min(40, b.text.length())));
                else System.out.println("  A " + b.text.substring(0, Math.min(50, b.text.length())) + "  -> " + ScriptParser.mentions(st, b.text));
            }
        }
        for (String w : st.warnings) System.out.println("WARN " + w);
    }
}
