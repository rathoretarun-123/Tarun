package com.tarun.kahani.app;

import com.tarun.kahani.core.Cloud;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.Txt;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Free pictures of the story's important objects, fetched without any key from GitHub: Microsoft's Fluent Emoji
 * (MIT licence), rendered in 3D on a transparent background — a kite, a crown, a diya, a drum, a key, a kite…
 * The first time a direction mentions such an object, the film cuts to it for a moment (the handbook's insert:
 * "an important object, clue or physical action"). Saved into the story (and the library) like any picture.
 */
final class FreeArt {
    private FreeArt() {}

    static final String RAW = "https://raw.githubusercontent.com/microsoft/fluentui-emoji/main/assets/";
    static final String LICENCE = "Fluent Emoji 3D (Microsoft, MIT)";

    /** {the words of the script (Hindi stems and English whole words, comma separated), the Fluent emoji's folder name}. Portable things only: never the sun, a house or an animal of the cast. */
    static final String[][] OBJECTS = {
            {"पतंग,kite", "Kite"}, {"मुकुट,ताज,crown", "Crown"}, {"तलवार,sword,swords", "Crossed swords"}, {"ढाल,shield", "Shield"},
            {"चाबी,चाभी,key,keys", "Key"}, {"दीया,दीपक,दिया,diya,lamp,oil lamp", "Diya lamp"}, {"ढोल,ढोलक,drum,drums", "Drum"}, {"घंटी,घंटा,bell,bells", "Bell"},
            {"गुब्बार,balloon,balloons", "Balloon"}, {"किताब,पुस्तक,book,books", "Open book"}, {"सेब,apple,apples", "Red apple"}, {"आम ,mango,mangoes", "Mango"},
            {"केला,केले,banana,bananas", "Banana"}, {"नारियल,coconut", "Coconut"}, {"मछली,fish", "Fish"}, {"छाता,छतरी,umbrella", "Umbrella"},
            {"मोमबत्ती,candle,candles", "Candle"}, {"तोहफ़,तोहफा,उपहार,gift,present", "Wrapped gift"}, {"केक,cake", "Birthday cake"}, {"रोटी,bread,roti", "Bread"},
            {"हांडी,हाँडी,बर्तन,pot of,cooking pot", "Pot of food"}, {"धनुष,तीर,bow and arrow,arrow,arrows", "Bow and arrow"}, {"हीरा,रत्न,gem,diamond,jewel", "Gem stone"},
            {"अँगूठी,अंगूठी,ring", "Ring"}, {"सिक्क,coin,coins", "Coin"}, {"खज़ान,खजान,थैली,money bag,treasure", "Money bag"}, {"नक़्शा,नक्शा,map", "World map"},
            {"दूरबीन,telescope", "Telescope"}, {"फ़ोन,फोन,मोबाइल,phone,mobile", "Mobile phone"}, {"लैपटॉप,laptop", "Laptop"}, {"ताला,lock,padlock", "Locked"},
            {"हड्डी,bone", "Bone"}, {"पंख,feather", "Feather"}, {"गिटार,guitar", "Guitar"}, {"तुरही,trumpet", "Trumpet"}, {"वायलिन,violin", "Violin"},
            {"गेंद,फुटबॉल,ball,football", "Soccer ball"}, {"बल्ला,cricket bat,cricket", "Cricket game"}, {"टेडी,teddy", "Teddy bear"},
            {"जादू की छड़ी,छड़ी,wand", "Magic wand"}, {"क्रिस्टल,crystal ball", "Crystal ball"}, {"टोपी,hat", "Top hat"}, {"चश्मा,glasses,spectacles", "Glasses"},
            {"बस्ता,बैग,backpack,school bag,bag", "Backpack"}, {"आवर्धक,magnifying glass", "Magnifying glass tilted left"}, {"कलम,pen", "Pen"},
            {"चिट्ठी,पत्र,scroll,letter", "Scroll"}, {"लालटेन,lantern", "Red paper lantern"}, {"टॉर्च,torch,flashlight", "Flashlight"}, {"बल्ब,bulb", "Light bulb"},
            {"हेडफ़ोन,हेडफोन,headphone,headphones", "Headphone"}, {"जॉयस्टिक,joystick,controller", "Joystick"}, {"घड़ी,clock,watch", "Alarm clock"},
            {"रेतघड़ी,hourglass", "Hourglass done"}, {"मशरूम,mushroom", "Mushroom"}, {"सूरजमुखी,sunflower", "Sunflower"}, {"गुलदस्त,bouquet", "Bouquet"},
            {"रॉकेट,rocket", "Rocket"}, {"तंबू,tent", "Tent"}, {"नाव,boat,sailboat", "Sailboat"},
    };

    /** The raw URL of an emoji's 3D picture. */
    static String url(String name) {
        String file = name.toLowerCase(Locale.ROOT).replace(' ', '_') + "_3d.png";
        return RAW + name.replace(" ", "%20") + "/3D/" + file;
    }

    /** The objects the script mentions, {keys, name}, those with a picture first mentioned in a direction. */
    static List<String[]> wanted(Story story) {
        StringBuilder all = new StringBuilder(story.title).append('\n');
        for (Story.Scene sc : story.scenes) {
            all.append(sc.title).append('\n').append(sc.setting).append('\n');
            for (Story.Beat b : sc.beats) if (b.type != Story.Beat.DIALOGUE) all.append(b.text).append('\n');
        }
        String text = all.toString();
        List<String[]> out = new ArrayList<String[]>();
        for (String[] o : OBJECTS) if (mentions(text, o[0])) out.add(o);
        return out;
    }

    static boolean mentions(String text, String keys) {
        for (String k : keys.split(",")) {
            k = k.trim();
            if (k.isEmpty()) continue;
            boolean latin = k.charAt(0) < 0x0900;
            if (latin ? Txt.hasWord(text, k) : Txt.has(text, k)) return true;
        }
        return false;
    }

    /** Fetches the pictures of up to 'max' objects the story has no picture for yet. Returns how many were added. */
    static int fetchFor(Project project, Story story, Cloud cloud, Library lib, int max, List<String> notes) {
        if (cloud == null) return 0;
        String cast = project.read("cast.txt");
        int added = 0;
        for (String[] o : wanted(story)) {
            if (added >= max) break;
            if (cast.contains("|" + o[0] + "|")) continue;          // already there
            // an animal word that is a character of the story is the character, not a picture of an object
            try {
                byte[] png = cloud.download(url(o[1]));
                if (png == null || png.length < 2000 || png.length > 3 * 1024 * 1024) continue;
                String file = project.savePicture(png, "obj");
                project.setManifest("shot", ":" + o[0], "shot||" + o[0] + "|" + file + "|object");
                try { lib.addBytes(Library.PIC, "object", o[1], o[0].replace(',', ' '), png, ".png", LICENCE); } catch (Exception ignored) {}
                added++;
                notes.add("picture of the " + o[1].toLowerCase(Locale.ROOT) + " (" + LICENCE + ")");
            } catch (Exception ignored) {
                // offline or the file is gone: the story goes on without the insert
            }
        }
        return added;
    }

    /** True when the look is an animal the script's words could also name (the object list keeps no animals, so always false for now). */
    static boolean isAnimal(Look l) { return l != null && (l.kind == Look.ANIMAL || l.kind == Look.BIRD || l.kind == Look.MONKEY); }
}
