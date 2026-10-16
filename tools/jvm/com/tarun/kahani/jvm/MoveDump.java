package com.tarun.kahani.jvm;
import com.tarun.kahani.core.*;
import java.nio.file.*;
/** Every move (walk/run) the director gave each character: start, length, from x to x. */
public class MoveDump {
    public static void main(String[] a) throws Exception {
        for (String f : a) {
            Story st = ScriptParser.parse(new String(Files.readAllBytes(Paths.get(f)), "UTF-8"));
            Director d = new Director(st, new Director.Options());
            d.prepare();
            Film film = d.direct(new Art());
            for (Film.Seg sg : film.segs) for (Film.Actor ac : sg.actors) {
                StringBuilder b = new StringBuilder();
                for (Film.Key k : ac.keys) if (k.moveDur > 0) b.append(String.format(java.util.Locale.US, " [%.1f+%.1f %s x%.0f]", k.t, k.moveDur, k.run ? "run" : "walk", k.x));
                System.out.println(Paths.get(f).getFileName() + " " + ac.c.fullName + ":" + b);
            }
        }
    }
}
