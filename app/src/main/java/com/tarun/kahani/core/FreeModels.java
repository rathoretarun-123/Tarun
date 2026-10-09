package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;

/**
 * Free 3D character models on GitHub that the studio can fetch (no key) and render as a character's doll when
 * the character has no picture of its own: the model is read by Glb, posed from its idle animation, graded to
 * the user's pictures' line (StyleCue) and offered as a proposal the user accepts or rejects. Every entry names
 * its licence; CC0 needs nothing, CC-BY needs the credit line the film's production file carries. The model
 * files stay on GitHub until a film needs them; a fetched model is kept with the story.
 */
public final class FreeModels {
    private FreeModels() {}

    public static final class Entry {
        public final String name, url, licence, credit, source;
        /** Words of a description (Hindi and English, lower case) this model fits. */
        public final String[] words;
        /** Props the model carries that may be kept when the description asks for them (sword, shield, staff…). */
        public final String[] props;
        /** The character kinds (Look.kind) this model may stand in for; empty = any humanoid. */
        public final int[] kinds;
        Entry(String name, String url, String licence, String credit, String source, String[] words, String[] props, int[] kinds) {
            this.name = name; this.url = url; this.licence = licence; this.credit = credit; this.source = source; this.words = words; this.props = props; this.kinds = kinds;
        }
        public boolean free() { return licence.startsWith("CC0"); }
    }

    private static final String KAYKIT = "https://raw.githubusercontent.com/KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0/main/addons/kaykit_character_pack_adventures/Characters/gltf/";
    private static final String KHRONOS = "https://raw.githubusercontent.com/KhronosGroup/glTF-Sample-Assets/main/Models/";

    /** The catalogue, in the order they are tried. */
    public static final Entry[] ALL = {
        new Entry("Knight (KayKit Adventurers)", KAYKIT + "Knight.glb", "CC0 1.0", "KayKit Character Pack: Adventurers by Kay Lousberg (CC0)", "github.com/KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0",
                new String[]{"knight", "guard", "soldier", "warrior", "सैनिक", "सिपाही", "योद्धा", "पहरेदार", "गार्ड", "सेनापति", "कवच", "रक्षक", "chowkidar", "चौकीदार"}, new String[]{"Sword", "Shield", "Badge_Shield", "Round_Shield"}, new int[]{Look.MAN, Look.BOY, Look.OLD_MAN}),
        new Entry("Mage (KayKit Adventurers)", KAYKIT + "Mage.glb", "CC0 1.0", "KayKit Character Pack: Adventurers by Kay Lousberg (CC0)", "github.com/KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0",
                new String[]{"mage", "wizard", "sorcerer", "magician", "जादूगर", "तांत्रिक", "ऋषि", "साधु", "पंडित", "ज्योतिषी", "ओझा"}, new String[]{"Staff", "Spellbook", "Wand"}, new int[]{Look.MAN, Look.OLD_MAN, Look.WITCH}),
        new Entry("Rogue (KayKit Adventurers)", KAYKIT + "Rogue.glb", "CC0 1.0", "KayKit Character Pack: Adventurers by Kay Lousberg (CC0)", "github.com/KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0",
                new String[]{"rogue", "thief", "robber", "bandit", "spy", "चोर", "डाकू", "लुटेरा", "जासूस", "ठग", "उचक्का"}, new String[]{"Dagger", "Crossbow"}, new int[]{Look.MAN, Look.BOY}),
        new Entry("Hooded rogue (KayKit Adventurers)", KAYKIT + "Rogue_Hooded.glb", "CC0 1.0", "KayKit Character Pack: Adventurers by Kay Lousberg (CC0)", "github.com/KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0",
                new String[]{"hooded", "hood", "assassin", "masked", "नकाबपोश", "नकाब", "हुड"}, new String[]{"Dagger", "Crossbow"}, new int[]{Look.MAN, Look.BOY}),
        new Entry("Barbarian (KayKit Adventurers)", KAYKIT + "Barbarian.glb", "CC0 1.0", "KayKit Character Pack: Adventurers by Kay Lousberg (CC0)", "github.com/KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0",
                new String[]{"barbarian", "giant", "brute", "ogre", "strongman", "पहलवान", "दैत्य", "राक्षस", "दानव", "असुर", "भीम", "बलवान", "लठैत"}, new String[]{"Axe", "Shield_Round_Barbarian", "Shield"}, new int[]{Look.MAN, Look.MONSTER}),
        new Entry("Fox (Khronos glTF samples)", KHRONOS + "Fox/glTF-Binary/Fox.glb", "CC BY 4.0", "Fox by PixelMannen (CC0 model), rigged by tomkranis, converted by @AsoboStudio and @scurest — CC BY 4.0", "github.com/KhronosGroup/glTF-Sample-Assets",
                new String[]{"fox", "लोमड़ी", "लोमडी"}, new String[]{}, new int[]{Look.ANIMAL}),
        new Entry("Rigged figure (Khronos glTF samples)", KHRONOS + "RiggedFigure/glTF-Binary/RiggedFigure.glb", "CC BY 4.0", "RiggedFigure by Cesium — CC BY 4.0", "github.com/KhronosGroup/glTF-Sample-Assets",
                new String[]{"figure", "robot", "mannequin", "dummy", "पुतला", "रोबोट", "यंत्रमानव"}, new String[]{}, new int[]{Look.MAN, Look.BOY}),
    };

    /** The idle poses to try, by pack naming. */
    public static final String[] POSES = {"Idle", "Unarmed_Idle", "idle", "Idle_A", "Standing", "Survey", "Walk"};

    /**
     * The models that fit a character: its description's words first (a "knight" is the knight), then its kind
     * (a man without such a word gets nothing — a free model is never forced on a character it does not describe).
     */
    public static List<Entry> matches(String description, Look look) {
        List<Entry> out = new ArrayList<Entry>();
        String d = description == null ? "" : description.toLowerCase();
        for (Entry e : ALL) {
            boolean word = false;
            for (String w : e.words) if (d.contains(w)) { word = true; break; }
            if (!word) continue;
            if (look != null && e.kinds.length > 0) {
                boolean kind = false;
                for (int k : e.kinds) if (k == look.kind) { kind = true; break; }
                if (!kind && look.kind != Look.MAN) continue;      // a girl described as a knight stays a girl
            }
            out.add(e);
        }
        return out;
    }

    /** The props of a model the description asks for (a sword, a shield, a staff…), as Glb.Options.props words. */
    public static String[] propsFor(Entry e, Look look, String description) {
        List<String> out = new ArrayList<String>();
        String d = description == null ? "" : description.toLowerCase();
        boolean sword = (look != null && look.sword) || d.contains("sword") || d.contains("तलवार");
        boolean shield = (look != null && look.shield) || d.contains("shield") || d.contains("ढाल");
        boolean staff = (look != null && look.wand) || d.contains("staff") || d.contains("wand") || d.contains("छड़ी") || d.contains("लाठी") || d.contains("डंडा");
        boolean axe = (look != null && look.axe) || d.contains("axe") || d.contains("कुल्हाड़ी") || d.contains("परशु");
        boolean dagger = (look != null && look.katar) || d.contains("dagger") || d.contains("खंजर") || d.contains("कटार") || d.contains("छुरा");
        boolean book = d.contains("book") || d.contains("किताब") || d.contains("पोथी") || d.contains("ग्रंथ");
        for (String p : e.props) {
            String q = p.toLowerCase();
            if (sword && q.contains("sword")) out.add("1H_Sword");
            else if (shield && q.contains("shield") && !out.contains(p)) out.add(p);
            else if (staff && q.contains("staff")) out.add(p);
            else if (axe && q.contains("axe")) out.add("Axe_1Handed");
            else if (dagger && q.contains("dagger")) out.add(p);
            else if (book && q.contains("spellbook")) out.add("Spellbook_open");
        }
        return out.toArray(new String[0]);
    }

    /** The free sources of the 3D maker, for the protocols screen and the README: what is used, under which licence. */
    public static final String SOURCES = "FREE SOURCES OF THE 3D MAKER (GitHub, no key)\n"
            + "Models the studio can fetch and pose as dolls (Glb reads glTF 2.0 with skins and animations; the idle pose is used; props follow the description):\n"
            + "• KayKit Character Pack: Adventurers — Knight, Mage, Rogue, Hooded Rogue, Barbarian — CC0 1.0 — github.com/KayKit-Game-Assets/KayKit-Character-Pack-Adventures-1.0\n"
            + "• Khronos glTF Sample Assets — Fox (CC BY 4.0: PixelMannen, tomkranis, AsoboStudio, scurest), RiggedFigure (CC BY 4.0: Cesium) — github.com/KhronosGroup/glTF-Sample-Assets\n"
            + "Open-source image-to-3D models whose free demos the studio can call (Gradio API on Hugging Face Spaces, no key; slow, may be asleep):\n"
            + "• TripoSR — MIT — github.com/VAST-AI-Research/TripoSR\n"
            + "• InstantMesh — Apache-2.0 — github.com/TencentARC/InstantMesh\n"
            + "• Hunyuan3D-2 — Tencent Hunyuan 3D 2.0 Community License — github.com/Tencent-Hunyuan/Hunyuan3D-2\n"
            + "Read for the studio's own methods (nothing of theirs is shipped):\n"
            + "• Meta AnimatedDrawings (MIT) — a drawn character rigged from its silhouette and joints; the figure model's parts from the rows of the cut-out follow the same idea\n"
            + "• TRELLIS (MIT), LGM (MIT), DreamGaussian (MIT), Unique3D (MIT), CRM (MIT), zero123plus (Apache-2.0), threestudio (Apache-2.0), Stable Fast 3D (Stability AI Community License) — multi-view and feed-forward image-to-3D; the views the studio draws (front, three-quarter, side, back) are the views these methods predict\n"
            + "• MiDaS (MIT), Depth Anything V2 (Apache-2.0) — depth from one picture; the figure model's depth per part (round head and limbs, flatter body) stands in for a depth network on the phone\n"
            + "• Google Filament (Apache-2.0), Khronos glTF 2.0 — the renderer and the model format the studio's own small renderer and reader follow\n"
            + "• MediaPipe (Apache-2.0) — pose and face landmarks; the studio finds the face from the picture's skin and features instead (no native library)\n";

    /** One credit line per model used, for the production file (CC-BY needs it; CC0 gets thanks). */
    public static String credit(Entry e) { return e.name + " — " + e.credit + " — " + e.source; }
}
