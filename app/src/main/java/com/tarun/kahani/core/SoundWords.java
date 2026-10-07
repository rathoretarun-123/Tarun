package com.tarun.kahani.core;

import java.util.LinkedHashSet;
import java.util.Set;

/**
 * Words for the same sound in English, Hindi and Hinglish, so a sound named "rain" is found for a story that
 * says "बारिश", and a crowd recording for "बाज़ार". Used to widen the words of the user's own sounds.
 */
public final class SoundWords {
    private SoundWords() {}

    static final String[][] GROUPS = {
            {"rain", "raining", "drizzle", "monsoon", "बारिश", "वर्षा", "बरसात", "बूँदें", "baarish", "barish"},
            {"storm", "thunderstorm", "तूफ़ान", "तूफान", "आँधी", "आंधी", "toofan"},
            {"thunder", "lightning", "गरज", "बिजली", "गड़गड़ाहट", "bijli"},
            {"wind", "breeze", "हवा", "पवन", "सरसराहट", "hawa"},
            {"river", "stream", "brook", "नदी", "धारा", "nadi", "kal-kal", "कल-कल"},
            {"waterfall", "falls", "झरना", "जलप्रपात", "jharna"},
            {"water", "पानी", "paani", "pani"},
            {"sea", "ocean", "waves", "beach", "shore", "समुद्र", "सागर", "लहरें", "लहर", "किनारा", "samundar"},
            {"fire", "flames", "campfire", "bonfire", "torch", "आग", "अग्नि", "अलाव", "मशाल", "aag"},
            {"birds", "bird", "chirping", "पक्षी", "चिड़िया", "चिड़ियाँ", "पंछी", "चहचहा", "chidiya", "panchhi"},
            {"crickets", "insects", "night", "झींगुर", "रात", "jhingur", "raat"},
            {"frogs", "pond", "मेंढक", "तालाब", "mendhak"},
            {"forest", "jungle", "woods", "जंगल", "वन", "jungle"},
            {"garden", "park", "बगीचा", "बाग", "उपवन", "bagicha"},
            {"village", "farm", "field", "गाँव", "गांव", "खेत", "gaon"},
            {"market", "bazaar", "crowd", "fair", "people talking", "voices", "chatter", "बाज़ार", "बाजार", "भीड़", "मेला", "bazaar", "bheed", "mela"},
            {"palace", "court", "hall", "throne", "महल", "दरबार", "राजमहल", "mahal", "darbar"},
            {"temple", "prayer", "puja", "aarti", "मंदिर", "पूजा", "आरती", "mandir"},
            {"bell", "bells", "chime", "घंटी", "घंटा", "ghanti"},
            {"anklets", "ghungroo", "पायल", "घुंघरू", "payal"},
            {"cave", "tunnel", "गुफा", "सुरंग", "gufa"},
            {"desert", "sand", "रेगिस्तान", "रेत", "registan"},
            {"school", "classroom", "children playing", "kids", "स्कूल", "विद्यालय", "बच्चे", "school"},
            {"home", "house", "room", "kitchen", "घर", "कमरा", "रसोई", "ghar"},
            {"city", "traffic", "street", "road", "car", "horn", "शहर", "सड़क", "गाड़ी", "shehar"},
            {"train", "railway", "रेल", "ट्रेन", "रेलगाड़ी"},
            {"horse", "hooves", "galloping", "घोड़ा", "घोड़े", "टाप", "ghoda"},
            {"elephant", "हाथी", "haathi"},
            {"lion", "tiger", "roar", "शेर", "बाघ", "दहाड़", "sher"},
            {"dog", "bark", "barking", "कुत्ता", "भौंक", "kutta"},
            {"cow", "moo", "गाय", "gaay"},
            {"rooster", "cock", "crow", "dawn", "मुर्गा", "कुकड़ूँ", "murga"},
            {"monkey", "बंदर", "bandar"},
            {"snake", "hiss", "साँप", "सांप", "फुफकार", "saanp"},
            {"door", "knock", "knocking", "creak", "दरवाज़ा", "दरवाजा", "दस्तक", "खटखट", "darwaza"},
            {"footsteps", "steps", "walking", "कदम", "आहट", "kadam"},
            {"running", "ran", "दौड़", "भागा", "भागते", "daud"},
            {"sword", "swords", "clash", "fight", "तलवार", "युद्ध", "talwar"},
            {"drum", "drums", "dhol", "nagada", "ढोल", "नगाड़ा", "dhol"},
            {"flute", "बांसुरी", "बाँसुरी", "bansuri"},
            {"clock", "ticking", "घड़ी", "ghadi"},
            {"laugh", "laughing", "giggle", "हँसी", "हंसी", "hansi"},
            {"cry", "crying", "sob", "रोना", "रोने", "rona"},
            {"clap", "clapping", "applause", "ताली", "तालियाँ", "taali"},
            {"cheer", "cheering", "celebration", "festival", "जयकार", "उत्सव", "त्योहार", "jaikaar"},
            {"magic", "spell", "sparkle", "जादू", "मंत्र", "jaadu"},
            {"explosion", "blast", "boom", "धमाका", "विस्फोट", "dhamaka"},
            {"splash", "jumped into water", "छपाक", "chhapak"},
            {"heartbeat", "heart", "धड़कन", "dhadkan"},
            {"music", "song", "singing", "संगीत", "गाना", "गीत", "sangeet", "gaana"},
    };

    /** The given words plus every other word for the same sounds. */
    public static Set<String> expand(String words) {
        Set<String> out = new LinkedHashSet<String>();
        for (String w : SoundLib.splitWords(words)) out.add(w);
        String all = Txt.norm(" " + words.replace(',', ' ') + " ");
        for (String[] g : GROUPS) {
            boolean hit = false;
            for (String w : g) {
                String n = Txt.norm(w);
                // English / Hinglish words must stand alone ("rain" not in "brain"); Hindi words may carry endings
                boolean latin = n.length() > 0 && n.charAt(0) < 0x0900;
                if (latin ? all.contains(" " + n + " ") || all.contains(" " + n + "s ") : all.contains(n)) { hit = true; break; }
            }
            if (hit) for (String w : g) out.add(Txt.norm(w));
        }
        return out;
    }
}
