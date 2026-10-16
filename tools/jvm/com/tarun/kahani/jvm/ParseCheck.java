package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.nio.file.*;
import java.util.*;

/**
 * v41: reads every story of a folder and compares what the reader found with expected.tsv (written with the stories):
 * the cast, the places, the things, the scenes, and every spoken line — its words and who says it.
 * ParseCheck dir
 */
public class ParseCheck {
    public static void main(String[] a) throws Exception {
        List<String> exp = Files.readAllLines(Paths.get(a[0], "expected.tsv"), java.nio.charset.StandardCharsets.UTF_8);
        int ok = 0, bad = 0, linesOk = 0, linesAll = 0;
        for (String row : exp) {
            String[] f = row.split("\t");
            Story st = ScriptParser.parse(new String(Files.readAllBytes(Paths.get(a[0], f[0])), "UTF-8"));
            List<String> problems = new ArrayList<String>();
            List<String> cast = Arrays.asList(f[1].split(","));
            List<String> got = new ArrayList<String>();
            for (Story.CharacterDef c : st.cast()) got.add(c.fullName);
            if (!new HashSet<String>(got).equals(new HashSet<String>(cast))) problems.add("cast " + got + " (want " + cast + ")");
            if (st.places.size() != Integer.parseInt(f[2])) problems.add("places " + st.places.size() + " (want " + f[2] + ")");
            if (st.things.size() != Integer.parseInt(f[3])) problems.add("things " + st.things.size() + " (want " + f[3] + ")");
            if (st.scenes.size() != Integer.parseInt(f[4])) problems.add("scenes " + st.scenes.size() + " (want " + f[4] + ")");
            List<String[]> lines = new ArrayList<String[]>();
            for (Story.Scene sc : st.scenes) for (Story.Beat b : sc.beats) if (b.type == Story.Beat.DIALOGUE)
                lines.add(new String[]{b.speaker == null ? "?" : b.speaker.fullName, b.text});
            int want = f.length - 5;
            linesAll += want;
            if (lines.size() != want) problems.add("lines " + lines.size() + " (want " + want + ")");
            for (int i = 0; i < want; i++) {
                String[] e = f[5 + i].split("::", 2);
                String[] g = i < lines.size() ? lines.get(i) : null;
                String wantText = Txt.norm(e[1]).replaceAll("[\\s\"“”'.,!?।…]+", "");
                boolean same = g != null && g[0].equals(e[0]) && Txt.norm(g[1]).replaceAll("[\\s\"“”'.,!?।…]+", "").equals(wantText);
                if (same) linesOk++;
                else problems.add("line " + (i + 1) + ": " + (g == null ? "missing" : g[0] + " \"" + g[1] + "\"") + " (want " + e[0] + " \"" + e[1] + "\")");
            }
            if (problems.isEmpty()) ok++; else { bad++; System.out.println(f[0] + ": " + problems); }
        }
        System.out.printf("PARSE %d stories read right, %d with problems; lines %d of %d right%n", ok, bad, linesOk, linesAll);
    }
}
