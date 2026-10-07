package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.io.File;
import java.nio.file.*;

/** Prints the production file for a script:  BibleDump story.txt */
public class BibleDump {
    public static void main(String[] a) throws Exception {
        Story st = ScriptParser.parse(new String(Files.readAllBytes(Paths.get(a[0])), "UTF-8"));
        SoundLib lib = new SoundLib(null);
        File sounds = new File("app/src/main/assets/sounds");
        lib.addIndex(new String(Files.readAllBytes(new File(sounds, "index.json").toPath()), "UTF-8"), sounds.getAbsolutePath() + "/");
        System.out.println(Bible.write(st, lib, new java.util.HashSet<String>(), new java.util.HashSet<String>()));
    }
}
