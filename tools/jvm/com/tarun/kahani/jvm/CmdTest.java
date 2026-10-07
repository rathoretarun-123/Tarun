package com.tarun.kahani.jvm;

import com.tarun.kahani.core.*;
import java.util.*;

/** Prints how after-preview commands are understood. */
public class CmdTest {
    public static void main(String[] a) {
        List<String> names = Arrays.asList("वृंदा", "वानुषा", "खान राक्षस", "आंटी चुड़ैल", "राजू बंदर", "रतनलाल");
        String[] tests = {
            "the background music is too loud",
            "I can't hear Vrinda properly",
            "make Khan sound deeper and a bit slower",
            "the film drags, speed it up",
            "make it black and white",
            "the video is too big to upload on whatsapp",
            "turn the birds down a little and add captions",
            "make the colours more vivid",
            "make the music lower and increase brightness",
            "video is too dark",
            "make it darker",
            "brightness कम करो",
            "background music थोड़ा कम करो और वृंदा की आवाज़ तेज़ करो",
            "brightness बढ़ाओ",
            "वीडियो में अंधेरा बहुत है",
            "file size कम करो",
            "make the file smaller",
            "खान की आवाज़ और मोटी करो",
            "subtitles लगाओ",
            "subtitles हटा दो",
            "instagram के लिए बनाओ",
            "increase voice level of Raju by 50%",
            "राजू बंदर की आवाज़ 50% बढ़ाओ",
            "पृष्ठभूमि की आवाज़ बंद करो",
            "रंग थोड़ा गर्म करो",
            "music mute",
            "720p में बनाओ",
            "वानुषा की आवाज़ पतली करो, संगीत बढ़ाओ",
            "please make it more cinematic and dramatic"
        };
        for (String t : tests) {
            CommandParser.Result r = CommandParser.parse(t, names);
            Edits e = new Edits();
            StringBuilder sb = new StringBuilder();
            for (Map<String, Object> c : r.commands) sb.append(Json.write(c)).append(" => ").append(e.apply(c)).append("; ");
            System.out.println(t + "\n    " + sb + (r.unknown.isEmpty() ? "" : " UNKNOWN=" + r.unknown));
        }
    }
}
