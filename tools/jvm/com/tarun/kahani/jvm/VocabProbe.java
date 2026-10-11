package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.nio.file.*;
import java.util.*;

/**
 * Black-box vocabulary probe: each direction sentence (one per line of the file) is staged in a tiny story of two
 * characters; a sentence is "unread" when राहुल gets no act, no body change, no move, no emotion, no hold, no fx and
 * no sound beyond the baseline sentence.
 */
public class VocabProbe {
    static String sig(String sentence) {
        String s = "शीर्षक: जाँच\n\nपात्र परिचय\n1. राहुल - 10 साल, बेटा\n2. माँ - 38 साल, माँ\n\nजगह का परिचय\n1. घर का आँगन\n\nस्क्रिप्ट शुरू\n\nदृश्य 1: घर का आँगन - दिन\n\n[राहुल आँगन में खड़ा है। माँ पास में खड़ी है।]\n\nमाँ: \"राहुल!\"\n\n["
                + sentence + "]\n\nराहुल: \"हाँ माँ।\"\n";
        if (System.getenv("EN") != null)
            s = "Title: Check\n\nCharacters\n1. Rahul - 10, a boy\n2. Mother - 38, his mother\n\nPlaces\n1. The courtyard of the house\n\nScript begins\n\nScene 1: The courtyard - day\n\n[Rahul stands in the courtyard. Mother stands near him.]\n\nMother: \"Rahul!\"\n\n["
                    + sentence + "]\n\nRahul: \"Yes, mother.\"\n";
        Story st = ScriptParser.parse(s);
        Director d = new Director(st, new Director.Options());
        d.prepare();
        Film f = d.direct(new Art());
        StringBuilder b = new StringBuilder();
        for (Film.Seg sg : f.segs) {
            if (sg.type != Film.S_SCENE) continue;
            for (Film.Actor ac : sg.actors) {
                if (ac.c == null) continue;
                b.append(ac.c.fullName).append(':');
                for (Film.Act a : ac.acts) b.append("A").append(a.type).append(',');
                Film.Key prev = null;
                for (Film.Key k : ac.keys) {
                    if (prev == null || prev.body != k.body) b.append("B").append(k.body).append(',');
                    if (prev == null || Math.abs(prev.x - k.x) > 20) b.append("X,");
                    if (prev == null || prev.emotion != k.emotion) b.append("E").append(k.emotion).append(',');
                    if (prev == null || prev.holdR != k.holdR || prev.holdL != k.holdL) b.append("H").append(k.holdR).append('/').append(k.holdL).append(',');
                    if (prev == null || prev.visible != k.visible) b.append("V").append(k.visible).append(',');
                    if (prev == null || prev.eyesShut != k.eyesShut || prev.tears != k.tears || prev.sweat != k.sweat) b.append("F,");
                    prev = k;
                }
                b.append(';');
            }
            b.append("fx").append(sg.fx.size()).append(';');
        }
        b.append("sfx").append(f.sfx.size());
        return b.toString();
    }

    public static void main(String[] a) throws Exception {
        String base = sig(System.getenv("EN") != null ? "Rahul looks at Mother." : "राहुल माँ की ओर देखता है।");
        String base2 = sig(System.getenv("EN") != null ? "Rahul keeps standing." : "राहुल खड़ा रहता है।");
        int unread = 0, n = 0;
        for (String line : Files.readAllLines(Paths.get(a[0]), java.nio.charset.StandardCharsets.UTF_8)) {
            line = line.trim();
            if (line.isEmpty() || line.startsWith("#")) continue;
            n++;
            String g = sig(line);
            if (g.equals(base) || g.equals(base2)) { unread++; System.out.println("UNREAD " + line); }
            else if (System.getenv("SHOW") != null) System.out.println("read   " + line + "   " + g);
        }
        System.out.println("VOCAB " + (n - unread) + " of " + n + " sentences staged");
    }
}
