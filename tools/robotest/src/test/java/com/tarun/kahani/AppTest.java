package com.tarun.kahani;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.tarun.kahani.app.AndroidGfx;
import com.tarun.kahani.app.MainActivity;
import com.tarun.kahani.app.Project;
import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Doll3D;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.FinalQc;
import com.tarun.kahani.core.Handbook;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.Nature;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.Rig;
import com.tarun.kahani.core.Set3D;
import com.tarun.kahani.core.Sets;
import com.tarun.kahani.core.Gfx;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.TechnicalDirector;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.android.controller.ActivityController;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34, manifest = Config.NONE)
public class AppTest {

    static final File ASSETS = new File(System.getProperty("kahani.assets"));
    static final File OUT = new File(System.getProperty("kahani.out"));

    /** Makes a project folder with the sample story and pictures (like the "उदाहरण" button). */
    /** Every test starts with a fresh library: the shared one would carry the pictures of an earlier test into the next. */
    @org.junit.Before
    public void freshLibrary() throws Exception {
        java.lang.reflect.Field f = com.tarun.kahani.app.Library.class.getDeclaredField("shared");
        f.setAccessible(true);
        f.set(null, null);
    }

    static Project sampleProject() throws Exception {
        Project p = Project.create(RuntimeEnvironment.getApplication());
        Files.copy(new File(ASSETS, "sample_story.txt").toPath(), new File(p.dir, "script.txt").toPath());
        for (File f : new File(ASSETS, "sample").listFiles()) Files.copy(f.toPath(), new File(p.dir, f.getName()).toPath());
        return p;
    }

    static void idle() { shadowOf(Looper.getMainLooper()).idle(); }

    static List<String> texts(View v, List<String> out) {
        if (v instanceof TextView) out.add(((TextView) v).getText().toString());
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) texts(((ViewGroup) v).getChildAt(i), out);
        return out;
    }

    static View find(View v, String startsWith) {
        if (v instanceof TextView && ((TextView) v).getText().toString().contains(startsWith)) return v;
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) {
            View r = find(((ViewGroup) v).getChildAt(i), startsWith);
            if (r != null) return r;
        }
        return null;
    }

    static void call(Object o, String m, Class<?>[] types, Object... args) throws Exception {
        Method mm = o.getClass().getDeclaredMethod(m, types);
        mm.setAccessible(true);
        mm.invoke(o, args);
    }

    static void set(Object o, String f, Object v) throws Exception {
        Field ff = o.getClass().getDeclaredField(f);
        ff.setAccessible(true);
        ff.set(o, v);
    }

    static boolean hasDevanagari(String t) {
        for (int i = 0; i < t.length(); i++) if (t.charAt(i) >= 0x0900 && t.charAt(i) <= 0x097F && "समाप्त".indexOf(t.charAt(i)) < 0) return true;
        return false;
    }

    @Test
    public void hinglishStoryIsReadAndShownInUsersSpelling() throws Exception {
        String hs = "Characters:\n1. Meena (8 saal): chhoti ladki, gulabi frock\n2. Raju Bandar: shaitaan bandar\n\nMeena aur Jadui Aam\n"
                + "Scene 1: Bagiche mein\n(Place: Gaon ka bagicha. Subah ka samay.)\n"
                + "Meena (hanste hue): \"Raju! Mera ribbon wapas do, abhi ke abhi!\"\nRaju Bandar (shaitani se): \"Pehle mujhe pakad ke dikhao!\"\n";
        Story st = ScriptParser.parse(hs);
        System.out.println("HINGLISH: title=" + st.title + " chars=" + st.characters + " labels=" + st.characters.get(0).shown() + "," + st.characters.get(1).shown());
        assertTrue(st.hinglish);
        assertTrue(st.hindi);
        assertTrue(st.title.equals("Meena aur Jadui Aam"));
        assertTrue(st.dialogueCount() == 2);
        assertTrue(st.characters.get(0).shown().equals("Meena"));
        assertTrue(st.characters.get(1).displayName.equals("Raju Bandar"));
        Director d = new Director(st, new Director.Options());
        Film f = d.prepare();
        System.out.println("HINGLISH LINE: speak=" + f.lines.get(0).text + " | shown=" + f.lines.get(0).shown);
        assertTrue(f.lines.get(0).text.contains("मेरा"));
        assertTrue(f.lines.get(0).shown.contains("Mera ribbon wapas do"));

        // one story mixing Hindi, English and Hinglish dialogue: each line keeps its own language
        String mix = "पात्र:\n1. वृंदा (11 वर्ष): राजकुमारी\n2. Mr. Brown (40 years): an English teacher\nदृश्य 1: महल\n(स्थान: महल का बगीचा)\n"
                + "वृंदा (मुस्कुराकर): \"नमस्ते! आप कहाँ से आए हैं?\"\nMr. Brown (smiling): \"Good morning, Princess! I have come from London.\"\n"
                + "वृंदा (हैरानी से): \"Wow! Aap sach mein London se aaye ho?\"\n";
        Story ms = ScriptParser.parse(mix);
        Film mf = new Director(ms, new Director.Options()).prepare();
        assertTrue(mf.lines.get(0).hindi && !mf.lines.get(1).hindi && mf.lines.get(2).hindi);
        assertTrue(mf.lines.get(0).shown.equals("नमस्ते! आप कहाँ से आए हैं?"));
        assertTrue(mf.lines.get(2).text.contains("London") && mf.lines.get(2).text.contains("आप"));
    }

    static String dialogText() {
        android.app.Dialog d = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        if (d == null || d.getWindow() == null) return "";
        return texts(d.getWindow().getDecorView(), new ArrayList<String>()).toString();
    }

    @Test
    public void loginDashboardStoryStudioLibrarySettingsOpenWithoutCrashing() throws Exception {
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        View root = a.getWindow().getDecorView();
        List<String> login = texts(root, new ArrayList<String>());
        System.out.println("LOGIN: " + login);
        assertTrue(login.toString().contains("Sign in with Gmail"));
        find(root, "Not now").performClick();
        idle();
        List<String> home = texts(root, new ArrayList<String>());
        System.out.println("HOME: " + home);
        assertTrue(home.toString().contains("New film"));
        assertTrue(home.toString().contains("Library"));
        assertTrue("the home screen's library upload button (v21)", home.toString().contains("Add pictures, voices or sounds to the library"));

        for (String t : home) assertTrue("Hindi left in the home screen: " + t, !hasDevanagari(t) || t.contains("Ratnagarh") || t.contains("रत्नगढ़"));
        // new empty film -> story screen (clear button, platforms) -> back
        find(root, "New film").performClick();
        idle();
        List<String> ed = texts(root, new ArrayList<String>());
        System.out.println("STORY: " + ed);
        for (String t : ed) assertTrue("Hindi left in the story screen: " + t, !hasDevanagari(t));
        assertTrue(ed.toString().contains("1. Story"));
        assertTrue(ed.toString().contains("Clear"));
        assertTrue(ed.toString().contains("Make film") && ed.toString().contains("Descriptions") && ed.toString().contains("Open studio"));
        a.onBackPressed();
        idle();

        // sample project -> story -> studio
        Project p = sampleProject();
        set(a, "project", p);
        call(a, "showStory", new Class<?>[0]);
        idle();
        find(root, "Open studio").performClick();
        idle();
        List<String> st = texts(root, new ArrayList<String>());
        System.out.println("STUDIO: " + st);
        assertTrue(st.toString().contains("वानुषा"));
        assertTrue(st.toString().contains("Descriptions for other apps"));
        assertTrue(st.toString().contains("Find pictures in my library"));
        assertTrue(st.toString().contains("Still missing"));
        assertTrue(st.toString().contains("Director's plan"));

        // picture chooser shows 4 at a time with "next 4"
        call(a, "choosePicture", new Class<?>[]{String.class, String.class}, "char:वृंदा", "वृंदा");
        idle();
        String pick = dialogText();
        System.out.println("PICKER: " + pick);
        assertTrue(pick.contains("Next 4"));
        assertTrue(pick.contains("camera"));
        org.robolectric.shadows.ShadowDialog.getLatestDialog().dismiss();

        // voice chooser
        Story story = ScriptParser.parse(p.read("script.txt"));
        call(a, "chooseVoice", new Class<?>[]{String.class, Story.class, Story.CharacterDef.class}, "वृंदा", story, story.characters.get(0));
        idle();
        String vp = dialogText();
        System.out.println("VOICE PICKER: " + vp);
        assertTrue(vp.contains("Record"));
        org.robolectric.shadows.ShadowDialog.getLatestDialog().dismiss();

        // record-each-line screen
        call(a, "showLines", new Class<?>[0]);
        idle();
        assertTrue(texts(root, new ArrayList<String>()).toString().contains("Record"));
        call(a, "showFace", new Class<?>[]{String.class}, "वृंदा");
        Thread.sleep(3000);
        idle();
        a.onBackPressed();
        idle();

        // library tabs and settings
        call(a, "showLibrary", new Class<?>[0]);
        idle();
        List<String> lib = texts(root, new ArrayList<String>());
        System.out.println("LIBRARY: " + lib.subList(0, Math.min(14, lib.size())));
        assertTrue(lib.toString().contains("From phone"));
        set(a, "libTab", "sound");
        call(a, "showLibrary", new Class<?>[0]);
        idle();
        assertTrue(texts(root, new ArrayList<String>()).toString().contains("Sounds"));
        call(a, "showSettings", new Class<?>[0]);
        idle();
        List<String> se = texts(root, new ArrayList<String>());
        assertTrue(se.toString().contains("Gemini"));
        assertTrue(se.toString().contains("Log out") || se.toString().contains("Sign in"));

        // player + natural-language command box (film file faked)
        set(a, "project", p);
        Files.write(p.film().toPath(), new byte[100]);
        call(a, "showPlayer", new Class<?>[0]);
        idle();
        TextView status = new TextView(a);
        android.widget.EditText box = new android.widget.EditText(a);
        call(a, "applyCommand", new Class<?>[]{String.class, TextView.class, android.widget.EditText.class},
                "the music is too loud, I can't hear Vrinda and please add subtitles", status, box);
        idle();
        System.out.println("COMMAND: " + status.getText());
        String edits = p.read("edits.json");
        System.out.println("EDITS: " + edits);
        assertTrue(edits.contains("\"subtitles\":true"));
        assertTrue(edits.contains("वृंदा"));
        ac.pause().stop().destroy();
    }

    /** The director places uploaded pictures (no names given) and they land in the film's cast list. */
    @Test
    public void directorPlacesPicturesFromLibrary() throws Exception {
        RuntimeEnvironment.getApplication().getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        Project p = Project.create(RuntimeEnvironment.getApplication());
        Files.copy(new File(ASSETS, "sample_story.txt").toPath(), new File(p.dir, "script.txt").toPath());
        set(a, "project", p);
        Field libF = MainActivity.class.getDeclaredField("library");
        libF.setAccessible(true);
        com.tarun.kahani.app.Library lib = (com.tarun.kahani.app.Library) libF.get(a);
        Class<?> pc = Class.forName("com.tarun.kahani.app.MainActivity$Placement");
        java.lang.reflect.Constructor<?> ctor = pc.getDeclaredConstructor();
        ctor.setAccessible(true);
        List<Object> ps = new ArrayList<Object>();
        String[] files = {"char_vrinda.jpg", "char_vanusha.jpg", "bg_forest.jpg", "char_kripa.jpg"};
        for (String f : files) {
            byte[] data = Files.readAllBytes(new File(ASSETS, "sample/" + f).toPath());
            com.tarun.kahani.app.Library.Item it = lib.addBytes("pic", "", "photo" + ps.size(), "", data, ".jpg", "test");
            assertTrue("picture not analysed: " + it.meta, it.meta.contains("hue="));
            Object pl = ctor.newInstance();
            Field fi = pc.getDeclaredField("item"); fi.setAccessible(true); fi.set(pl, it);
            Field fn = pc.getDeclaredField("fileName"); fn.setAccessible(true); fn.set(pl, "IMG_" + ps.size() + ".jpg");
            ps.add(pl);
        }
        Story st = ScriptParser.parse(p.read("script.txt"));
        set(a, "castStory", st);
        call(a, "identify", new Class<?>[]{List.class, Story.class, boolean.class, boolean.class}, ps, st, false, false);
        Field tf = pc.getDeclaredField("target"); tf.setAccessible(true);
        Field lf = pc.getDeclaredField("label"); lf.setAccessible(true);
        Field of = pc.getDeclaredField("options"); of.setAccessible(true);
        int placed = 0;
        for (Object pl : ps) {
            System.out.println("PLACED: " + tf.get(pl) + " (" + lf.get(pl) + ") options=" + ((List<?>) of.get(pl)).size());
            if (tf.get(pl) != null) placed++;
            assertTrue(((List<?>) of.get(pl)).size() > 3);
        }
        assertTrue(placed >= 3);
        call(a, "applyPlacements", new Class<?>[]{List.class}, ps);
        for (int i = 0; i < 50 && p.read("cast.txt").split("\n").length < placed; i++) { Thread.sleep(100); idle(); }
        System.out.println("CAST: " + p.read("cast.txt"));
        assertTrue(p.read("cast.txt").contains("char|"));
        ac.pause().stop().destroy();
    }

    /** Saved voices (no names given) are matched to characters by the voice the script describes. */
    @Test
    public void directorFindsVoicesThatFitTheDescription() throws Exception {
        RuntimeEnvironment.getApplication().getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        Project p = Project.create(RuntimeEnvironment.getApplication());
        String script = "पात्र (Characters):\n"
                + "1. राजा विक्रम (70 वर्ष):\n * चेहरा: सफ़ेद दाढ़ी, बड़ी मूँछें, सिर पर पगड़ी।\n * आवाज़: भारी, गहरी और धीमी आवाज़।\n"
                + "2. मीना (8 वर्ष):\n * चेहरा: गोल चेहरा, दो चोटियाँ।\n * आवाज़: चंचल, जल्दी-जल्दी बोलती है।\n"
                + "3. रानी सुमन:\n * चेहरा: सौम्य, बड़ी लाल बिंदी, साड़ी।\n * आवाज़: मीठी, कोमल और सुरीली आवाज़।\n\n"
                + "दृश्य 1: महल\n(स्थान: राजमहल का दरबार)\n"
                + "राजा विक्रम: \"आज हम एक नई यात्रा पर चलेंगे।\"\nमीना: \"मैं भी चलूँगी!\"\nरानी सुमन: \"ध्यान से जाना, बेटी।\"\n";
        Files.write(new File(p.dir, "script.txt").toPath(), script.getBytes("UTF-8"));
        set(a, "project", p);
        Field libF = MainActivity.class.getDeclaredField("library");
        libF.setAccessible(true);
        com.tarun.kahani.app.Library lib = (com.tarun.kahani.app.Library) libF.get(a);
        java.util.Map<String, String> idOf = new java.util.HashMap<String, String>();
        int n = 0;
        for (String f : new String[]{"man", "deep_slow", "woman", "child"}) {
            byte[] data = Files.readAllBytes(new File("voices/" + f + ".wav").toPath());
            com.tarun.kahani.app.Library.Item it = lib.addBytes("voice", "", "recording " + (++n), "", data, ".wav", "test");
            assertTrue("voice not measured: " + it.meta, it.meta.contains("vf="));
            System.out.println("VOICE " + f + ": " + it.tags + "  [" + it.meta + "]");
            idOf.put(it.id, f);
        }
        call(a, "voiceMatches", new Class<?>[]{boolean.class}, false);
        idle();
        android.app.AlertDialog d = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("no voice suggestions shown", d);
        d.getButton(android.app.AlertDialog.BUTTON_POSITIVE).performClick();
        idle();
        Story st = ScriptParser.parse(script);
        java.util.Map<String, String> got = new java.util.HashMap<String, String>();
        for (Story.CharacterDef c : st.characters) {
            String id = p.setting("vsample." + c.displayName, "");
            got.put(c.fullName == null ? c.displayName : c.fullName, idOf.get(id));
            System.out.println("CAST VOICE: " + c.displayName + " -> " + idOf.get(id));
        }
        assertTrue(got.toString(), "deep_slow".equals(got.get("राजा विक्रम")));
        assertTrue(got.toString(), "child".equals(got.get("मीना")));
        assertTrue(got.toString(), "woman".equals(got.get("रानी सुमन")));
        ac.pause().stop().destroy();
    }

    /**
     * No button: before the film the director takes what clearly fits from the library (a picture named after a
     * character or place, a voice saved under a character's name), and pictures from older stories join the
     * library once, without duplicates.
     */
    @Test
    public void libraryIsUsedWithoutAnyButton() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        com.tarun.kahani.app.Library lib = com.tarun.kahani.app.Library.get(ctx);
        Project p = Project.create(ctx);
        Files.copy(new File(ASSETS, "sample_story.txt").toPath(), new File(p.dir, "script.txt").toPath());
        Story st = ScriptParser.parse(p.read("script.txt"));
        lib.addBytes("pic", "", "वृंदा", "", Files.readAllBytes(new File(ASSETS, "sample/char_vrinda.jpg").toPath()), ".jpg", "test");
        lib.addBytes("pic", "", "रत्नगढ़ का महल और बगीचा", "", Files.readAllBytes(new File(ASSETS, "sample/bg_garden.jpg").toPath()), ".jpg", "test");
        com.tarun.kahani.app.Library.Item v = lib.addBytes("voice", "", "राजा तरुण", "", Files.readAllBytes(new File("voices/deep_slow.wav").toPath()), ".wav", "test");
        Class<?> al = Class.forName("com.tarun.kahani.app.AutoLibrary");
        Method fill = al.getDeclaredMethod("fill", android.content.Context.class, Project.class, Story.class);
        fill.setAccessible(true);
        String notes = (String) fill.invoke(null, ctx, p, st);
        System.out.println("AUTO: " + notes + "\nCAST: " + p.read("cast.txt"));
        String cast = p.read("cast.txt");
        assertTrue(cast, cast.contains("char|वृंदा|"));
        assertTrue(cast, cast.contains("scene|1|"));
        assertTrue("voice not given: " + p.setting("vsample.राजा तरुण", ""), v.id.equals(p.setting("vsample.राजा तरुण", "")));
        // nothing is taken twice: a second look finds nothing new
        String again = (String) fill.invoke(null, ctx, p, st);
        assertTrue(again, again.length() == 0);

        // an older story with its own picture: adopted once; the library's own pictures (now in p) are not re-added
        Project old = Project.create(ctx);
        Files.write(new File(old.dir, "script.txt").toPath(), "पात्र:\n1. मोती (कुत्ता): भूरा, प्यारा कुत्ता।\n\nदृश्य 1: घर\nमोती: \"भौं!\"\n".getBytes("UTF-8"));
        // a picture no other test has: random coloured blocks
        Bitmap bm = Bitmap.createBitmap(300, 400, Bitmap.Config.ARGB_8888);
        android.graphics.Canvas cv = new android.graphics.Canvas(bm);
        java.util.Random rr = new java.util.Random(System.nanoTime());
        android.graphics.Paint pt = new android.graphics.Paint();
        for (int i = 0; i < 12; i++) { pt.setColor(0xFF000000 | rr.nextInt(0xFFFFFF)); cv.drawRect(rr.nextInt(300), rr.nextInt(400), rr.nextInt(300), rr.nextInt(400), pt); }
        FileOutputStream fo = new FileOutputStream(new File(old.dir, "char_1.jpg"));
        bm.compress(Bitmap.CompressFormat.JPEG, 90, fo);
        fo.close();
        old.write("cast.txt", "char|मोती|char_1.jpg\n");
        Method adopt = al.getDeclaredMethod("adoptOldStories", android.content.Context.class, com.tarun.kahani.app.Library.class);
        adopt.setAccessible(true);
        int before = lib.find("pic", null, null).size();
        adopt.invoke(null, ctx, lib);
        int after = lib.find("pic", null, null).size();
        boolean named = false;
        for (com.tarun.kahani.app.Library.Item it : lib.find("pic", null, null)) if (it.name.equals("मोती") && it.tags.contains("कुत्ता")) named = true;
        System.out.println("ADOPTED: " + (after - before) + " named=" + named);
        assertTrue("old story's picture should be adopted exactly once (got " + (after - before) + ")", after - before == 1);
        assertTrue(named);
        adopt.invoke(null, ctx, lib);
        assertTrue(lib.find("pic", null, null).size() == after);

        // v25: the director learns from the user — a picture the user said is वृंदा (even under a camera file name) is
        // placed by that word in a new story; the one the user said is not वृंदा is never offered for her again;
        // a camera-named picture with nothing said about it is never guessed on its look alone
        Method cam = al.getDeclaredMethod("cameraName", String.class);
        cam.setAccessible(true);
        assertTrue((Boolean) cam.invoke(null, "IMG_20240912_123456"));
        assertTrue((Boolean) cam.invoke(null, "PXL_20250101_1"));
        assertTrue((Boolean) cam.invoke(null, "Screenshot_2025-01-01"));
        assertTrue(!(Boolean) cam.invoke(null, "वृंदा"));
        assertTrue(!(Boolean) cam.invoke(null, "Ratanlal"));
        Method lk = al.getDeclaredMethod("labelKey", String.class);
        lk.setAccessible(true);
        String key = (String) lk.invoke(null, "वृंदा");
        com.tarun.kahani.app.Library.Item said = lib.addBytes("pic", "", "", "", Files.readAllBytes(new File(ASSETS, "sample/char_vrinda.jpg").toPath()), ".jpg", "phone");
        said.setMeta("is:" + key, "1");
        for (com.tarun.kahani.app.Library.Item it : lib.find("pic", null, null)) if (it.name.equals("वृंदा")) it.setMeta("not:" + key, "1");
        lib.save();
        Project p2 = Project.create(ctx);
        Files.copy(new File(ASSETS, "sample_story.txt").toPath(), new File(p2.dir, "script.txt").toPath());
        String n2 = (String) fill.invoke(null, ctx, p2, st);
        System.out.println("LEARNT: " + n2);
        assertTrue("the picture the user said is वृंदा should be hers: " + p2.setting("auto.pic.char:वृंदा", "") + " vs " + said.id, said.id.equals(p2.setting("auto.pic.char:वृंदा", "")));
    }

    static byte[] wav16(float[] x, int sr) { return com.tarun.kahani.core.Wav.encode16(x, sr); }

    /** The user's own character and place sheets (tools/testdata/sheets): 60 sheets of 10 angles, poses or views each. */
    static File[] userSheets() {
        File dir = new File(ASSETS, "../../../../tools/testdata/sheets");
        File[] fs = dir.listFiles();
        if (fs == null) return new File[0];
        java.util.Arrays.sort(fs);
        return fs;
    }

    /** The upload's closing toast (✅ … or Could not …) has shown: the background work and its done() ran. */
    static boolean uploadDone() {
        String t = org.robolectric.shadows.ShadowToast.getTextOfLatestToast();
        return t != null && (t.startsWith("✅") || t.startsWith("Could not") || t.startsWith("No picture") || t.startsWith("These pictures"));
    }

    static int[] pixelsOf(File f, int[] wh) {
        Bitmap b = android.graphics.BitmapFactory.decodeFile(f.getAbsolutePath());
        assertNotNull(f.getName(), b);
        wh[0] = b.getWidth(); wh[1] = b.getHeight();
        int[] px = new int[wh[0] * wh[1]];
        b.getPixels(px, 0, wh[0], 0, 0, wh[0], wh[1]);
        return px;
    }

    /**
     * v26: every one of the user's 60 sheets (characters in angles, poses and emotions; places in views) splits
     * into its figures or panels — ten on most, eleven or twelve where the generator put six on a row — every
     * piece a whole figure: none under 30% of the median area, none overlapping another.
     */
    @Test
    public void userSheetsSplitIntoTheirFigures() throws Exception {
        File[] sheets = userSheets();
        assertTrue("no sheets at " + new File(ASSETS, "../../../../tools/testdata/sheets"), sheets.length >= 40);
        int total = 0, figures = 0;
        StringBuilder report = new StringBuilder();
        for (File f : sheets) {
            if (!f.getName().endsWith(".jpg")) continue;
            int[] wh = new int[2];
            int[] px = pixelsOf(f, wh);
            boolean place = f.getName().matches("sheet(3[4-9]|40|41|42|43)\\.jpg");
            List<com.tarun.kahani.core.Angles.Piece> all = com.tarun.kahani.core.Angles.split(px, wh[0], wh[1], !place);
            List<com.tarun.kahani.core.Angles.Piece> figs = com.tarun.kahani.core.Angles.figures(all, wh[0], wh[1]);
            List<Long> areas = new ArrayList<Long>();
            for (com.tarun.kahani.core.Angles.Piece pc : figs) areas.add((long) pc.w * pc.h);
            java.util.Collections.sort(areas);
            long median = areas.isEmpty() ? 0 : areas.get(areas.size() / 2);
            int overlaps = 0;
            for (int i = 0; i < figs.size(); i++) for (int j = i + 1; j < figs.size(); j++) {
                com.tarun.kahani.core.Angles.Piece a = figs.get(i), b = figs.get(j);
                int ix = Math.max(0, Math.min(a.x0 + a.w, b.x0 + b.w) - Math.max(a.x0, b.x0)), iy = Math.max(0, Math.min(a.y0 + a.h, b.y0 + b.h) - Math.max(a.y0, b.y0));
                if ((long) ix * iy > 0.3f * Math.min((long) a.w * a.h, (long) b.w * b.h)) overlaps++;
            }
            report.append(f.getName()).append('=').append(figs.size()).append(' ');
            total++;
            figures += figs.size();
            assertTrue(f.getName() + ": " + figs.size() + " figures", figs.size() >= 10 && figs.size() <= 12);
            assertTrue(f.getName() + ": a piece under 30% of the median (a part of a figure)", areas.isEmpty() || areas.get(0) >= 0.3f * median);
            assertTrue(f.getName() + ": " + overlaps + " overlapping pieces", overlaps == 0);
        }
        System.out.println("SHEETS: " + total + " sheets, " + figures + " figures, none cut — " + report);
    }

    /**
     * v26: a sheet given for a character becomes its front picture, its real views and up to 100 library pictures;
     * a thing named by the user gets its insert; a sheet of a place's views becomes the wide view, the reverse
     * angle and the rest in the library — all through the app's own upload path (saveAngles), no drawn view made.
     */
    @Test
    public void sheetsUploadedForCharacterThingAndPlaceAreSplitAndSaved() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        File[] sheets = userSheets();
        assertTrue(sheets.length >= 40);
        File girl = new File(sheets[0].getParentFile(), "sheet02.jpg"), cave = new File(sheets[0].getParentFile(), "sheet39.jpg"), monkey = new File(sheets[0].getParentFile(), "sheet01.jpg");
        Project p = Project.create(ctx);
        Files.copy(new File(ASSETS, "sample_story.txt").toPath(), new File(p.dir, "script.txt").toPath());
        Story st = ScriptParser.parse(p.read("script.txt"));
        Story.CharacterDef girlDef = null;
        for (Story.CharacterDef c : st.cast()) if (c.look != null && c.look.kind == com.tarun.kahani.core.Look.GIRL) { girlDef = c; break; }
        if (girlDef == null) girlDef = st.cast().get(0);
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        java.lang.reflect.Field pf = MainActivity.class.getDeclaredField("project");
        pf.setAccessible(true);
        pf.set(a, p);
        Method keyFor = MainActivity.class.getDeclaredMethod("keyFor", Story.CharacterDef.class);
        keyFor.setAccessible(true);
        String key = (String) keyFor.invoke(a, girlDef);
        Method save = MainActivity.class.getDeclaredMethod("saveAngles", String.class, List.class);
        save.setAccessible(true);
        java.lang.reflect.Field lf = MainActivity.class.getDeclaredField("library");
        lf.setAccessible(true);
        com.tarun.kahani.app.Library lib = (com.tarun.kahani.app.Library) lf.get(a);     // the activity's own instance (what the upload writes to)
        int libBefore = lib.find("pic", null, null).size();
        // 1. the character: one sheet of ten
        List<byte[]> one = new ArrayList<byte[]>();
        one.add(Files.readAllBytes(girl.toPath()));
        save.invoke(a, "angles:char:" + key + ":" + girlDef.shown(), one);
        for (int i = 0; i < 1200 && !uploadDone(); i++) { idle(); Thread.sleep(100); }
        idle();
        String cast = p.read("cast.txt");
        System.out.println("SHEET UPLOAD cast:\n" + cast + "\nSHEET TOAST: " + org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
        assertTrue("real angles not noted", "1".equals(p.setting("realangles." + key, "")));
        assertTrue("no front picture: " + cast, cast.contains("char|" + key + "|"));
        assertTrue("no view line: " + cast, cast.contains("view|" + key + "|"));
        int ofGirl = 0;
        for (com.tarun.kahani.app.Library.Item it : lib.find("pic", null, null)) if (girlDef.shown().equals(it.meta("ofName")) || it.name.equals(girlDef.shown())) ofGirl++;
        assertTrue("library pictures of the character: " + ofGirl, ofGirl >= 8);
        // no drawn view is ever made for her now
        assertTrue("1".equals(p.setting("realangles." + key, "")));
        // 2. a thing named by the user: its insert from one figure of the monkey sheet
        int[] wh = new int[2];
        int[] px = pixelsOf(monkey, wh);
        List<com.tarun.kahani.core.Angles.Piece> figs = com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px, wh[0], wh[1]), wh[0], wh[1]);
        assertTrue(figs.size() >= 9);
        com.tarun.kahani.core.Angles.Piece pc = figs.get(0);
        Bitmap pb = Bitmap.createBitmap(pc.px, pc.w, pc.h, Bitmap.Config.ARGB_8888);
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        pb.compress(Bitmap.CompressFormat.PNG, 100, bo);
        List<byte[]> thing = new ArrayList<byte[]>();
        thing.add(bo.toByteArray());
        save.invoke(a, "angles:obj:पगड़ी:पगड़ी", thing);
        org.robolectric.shadows.ShadowToast.reset();
        for (int i = 0; i < 600 && !uploadDone(); i++) { idle(); Thread.sleep(100); }
        idle();
        assertTrue("thing not saved: " + p.read("cast.txt"), p.read("cast.txt").contains("shot||पगड़ी|"));
        // 3. a place: a sheet of ten views of the cave for part 1 — the wide view, the reverse angle, the rest in the library
        List<byte[]> place = new ArrayList<byte[]>();
        place.add(Files.readAllBytes(cave.toPath()));
        save.invoke(a, "angles:scene:1:गुफा", place);
        org.robolectric.shadows.ShadowToast.reset();
        for (int i = 0; i < 1200 && !uploadDone(); i++) { idle(); Thread.sleep(100); }
        idle();
        System.out.println("SHEET TOAST 3: " + org.robolectric.shadows.ShadowToast.getTextOfLatestToast());
        assertTrue("wide view missing: " + p.read("cast.txt"), p.manifestLine("scene", "1") != null);
        assertTrue("reverse angle missing: " + p.read("cast.txt"), p.manifestLine("scene", "1r") != null);
        int ofCave = 0;
        for (com.tarun.kahani.app.Library.Item it : lib.find("pic", null, null)) if ("गुफा".equals(it.meta("ofName")) || it.name.equals("गुफा")) ofCave++;
        System.out.println("SHEET UPLOAD: character pictures " + ofGirl + ", cave pictures " + ofCave + ", library " + libBefore + " → " + lib.find("pic", null, null).size());
        assertTrue("library pictures of the cave: " + ofCave, ofCave >= 8);
        ac.pause().stop().destroy();
    }

    /**
     * v27: what the director reads of each figure of a sheet — its angle, its pose (standing, walking, running,
     * sitting, lying, waving…) and its feeling — on the user's own sheets: the sure cases hold, and the reading
     * never floods a sheet with one feeling (a wrong feeling would cast a wrong picture; the user corrects the rest).
     */
    @Test
    public void poseSenseReadsTheUsersSheets() throws Exception {
        File[] sheets = userSheets();
        assertTrue(sheets.length >= 40);
        File dir = sheets[0].getParentFile();
        String[] names = {"sheet02.jpg", "sheet04.jpg", "sheet09.jpg", "sheet01.jpg", "sheet15.jpg"};
        boolean[] beast = {false, false, true, true, false};
        java.util.Map<String, List<com.tarun.kahani.core.PoseSense.Tag>> read = new java.util.HashMap<String, List<com.tarun.kahani.core.PoseSense.Tag>>();
        for (int n = 0; n < names.length; n++) {
            int[] wh = new int[2];
            int[] px = pixelsOf(new File(dir, names[n]), wh);
            List<com.tarun.kahani.core.Angles.Piece> figs = com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px, wh[0], wh[1]), wh[0], wh[1]);
            assertTrue(names[n] + ": " + figs.size(), figs.size() >= 10);
            int standH = 0;
            List<com.tarun.kahani.core.Cutout.Result> rs = new ArrayList<com.tarun.kahani.core.Cutout.Result>();
            for (com.tarun.kahani.core.Angles.Piece pc : figs) {
                com.tarun.kahani.core.Cutout.Result r = com.tarun.kahani.core.Cutout.process(pc.px.clone(), pc.w, pc.h, beast[n]);
                rs.add(r);
                if (r.w < r.h * 1.25f) standH = Math.max(standH, r.h);
            }
            List<com.tarun.kahani.core.PoseSense.Tag> tags = new ArrayList<com.tarun.kahani.core.PoseSense.Tag>();
            StringBuilder sb = new StringBuilder(names[n] + ":");
            int laughing = 0, sad = 0;
            for (com.tarun.kahani.core.Cutout.Result r : rs) {
                com.tarun.kahani.core.PoseSense.Tag t = com.tarun.kahani.core.PoseSense.tag(r, standH, beast[n]);
                tags.add(t);
                sb.append(" [").append(t).append(t.m != null ? String.format(java.util.Locale.US, " hR=%.2f low=%.2f feet=%.2f", t.m.hRatio, t.m.lowMass, t.m.feet) : "").append("]");
                if (t.emotion == com.tarun.kahani.core.PoseSense.LAUGH) laughing++;
                if (t.emotion == com.tarun.kahani.core.PoseSense.SAD) sad++;
            }
            System.out.println("POSES " + sb);
            assertTrue(names[n] + " floods with laughing: " + laughing, laughing <= figs.size() / 4);
            assertTrue(names[n] + " floods with sad: " + sad, sad <= figs.size() / 4);
            read.put(names[n], tags);
        }
        // the sure cases: the girl's second figure stands sideways; the twelfth figure of sheet 4 sits (ninth);
        // the sleeping bull (sheet 9, third) lies; the monkey's sixth figure runs; sheet 15's tenth runs sideways
        com.tarun.kahani.core.PoseSense.Tag t = read.get("sheet02.jpg").get(1);
        assertTrue("sheet02 #2: " + t, t.angle == com.tarun.kahani.core.Angles.SIDE && t.pose == com.tarun.kahani.core.PoseSense.STAND);
        t = read.get("sheet04.jpg").get(8);
        assertTrue("sheet04 #9: " + t, t.pose == com.tarun.kahani.core.PoseSense.SIT);
        t = read.get("sheet09.jpg").get(2);
        assertTrue("sheet09 #3: " + t, t.pose == com.tarun.kahani.core.PoseSense.LIE);
        t = read.get("sheet01.jpg").get(5);
        assertTrue("sheet01 #6: " + t, t.pose == com.tarun.kahani.core.PoseSense.RUN && t.angle == com.tarun.kahani.core.Angles.SIDE);
        t = read.get("sheet15.jpg").get(9);
        assertTrue("sheet15 #10 (the girl sitting cross-legged, chin on her hand): " + t, t.pose == com.tarun.kahani.core.PoseSense.SIT);
        t = read.get("sheet15.jpg").get(4);
        assertTrue("sheet15 #5 (waving): " + t, t.pose == com.tarun.kahani.core.PoseSense.WAVE || t.pose == com.tarun.kahani.core.PoseSense.ARMS_UP || t.pose == com.tarun.kahani.core.PoseSense.STAND);
        // every figure of every sheet is tagged standing-front at least somewhere, and more than one angle is seen
        for (String nm : names) {
            java.util.Set<Float> angles = new java.util.HashSet<Float>();
            for (com.tarun.kahani.core.PoseSense.Tag tg : read.get(nm)) angles.add(tg.angle);
            assertTrue(nm + " angles seen: " + angles, angles.size() >= 3);
        }
    }

    /**
     * v27: the director casts the right one of the user's own pictures for each shot — the sideways walking
     * picture for the entrance, the crying picture for the crying line, the laughing picture for the laugh, the
     * sitting picture once she sits, the back picture behind the shoulder — and writes it in the shot list; the
     * renderer then draws that picture as it is for the whole shot.
     */
    @Test
    public void directorUsesTheRightPictureForEachShot() throws Exception {
        Project p = sampleProject();
        String sample = p.read("script.txt");
        String head = sample.substring(0, sample.indexOf("दृश्य 1:"));
        String script = head
                + "दृश्य 1: बगीचे में सुबह\n"
                + "(स्थान: रत्नगढ़ का महल और बगीचा। सुबह का समय है। वानुषा फूलों के पास खड़ी है।)\n"
                + "(वृंदा धीरे-धीरे चलती हुई बगीचे में आती है।)\n"
                + "वृंदा (रोते हुए): \"मेरी तलवार खो गई है, मुझे वह कहीं नहीं मिल रही।\"\n"
                + "वानुषा (हँसते हुए): \"दीदी, वह तो यहाँ पेड़ के नीचे पड़ी है!\"\n"
                + "(वृंदा पेड़ के नीचे बैठ जाती है।)\n"
                + "वृंदा (धीरे से): \"अब मैं यहीं बैठकर थोड़ा आराम करूँगी।\"\n"
                + "वानुषा (प्यार से): \"ठीक है दीदी, मैं तितलियों के पीछे जाती हूँ।\"\n";
        p.write("script.txt", script);
        Story story = ScriptParser.parse(script);
        Story.CharacterDef vrinda = null, vanusha = null;
        for (Story.CharacterDef c : story.cast()) { if (c.displayName.contains("वृंदा")) vrinda = c; if (c.displayName.contains("वानुषा")) vanusha = c; }
        assertNotNull(vrinda); assertNotNull(vanusha);
        String key = (String) s3d("keyFor", p, story, vrinda);
        // her pose pictures: five figures of the girl's sheet, tagged (as the user would after the reading)
        File[] sheets = userSheets();
        int[] wh = new int[2];
        int[] px = pixelsOf(new File(sheets[0].getParentFile(), "sheet02.jpg"), wh);
        List<com.tarun.kahani.core.Angles.Piece> figs = com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px, wh[0], wh[1]), wh[0], wh[1]);
        assertTrue(figs.size() >= 10);
        int[] use = {0, 1, 5, 8, 9, 2};
        float[] angle = {com.tarun.kahani.core.Angles.FRONT, com.tarun.kahani.core.Angles.SIDE, com.tarun.kahani.core.Angles.BACK, com.tarun.kahani.core.Angles.THREE_QUARTER, com.tarun.kahani.core.Angles.FRONT, com.tarun.kahani.core.Angles.SIDE};
        int[] pose = {com.tarun.kahani.core.PoseSense.STAND, com.tarun.kahani.core.PoseSense.WALK, com.tarun.kahani.core.PoseSense.STAND, com.tarun.kahani.core.PoseSense.STAND, com.tarun.kahani.core.PoseSense.SIT, com.tarun.kahani.core.PoseSense.STAND};
        int[] emo = {com.tarun.kahani.core.PoseSense.SAD, com.tarun.kahani.core.PoseSense.NEUTRAL, com.tarun.kahani.core.PoseSense.NEUTRAL, com.tarun.kahani.core.PoseSense.LAUGH, com.tarun.kahani.core.PoseSense.NEUTRAL, com.tarun.kahani.core.PoseSense.NEUTRAL};
        StringBuilder cast = new StringBuilder(p.read("cast.txt"));
        for (int i = 0; i < use.length; i++) {
            com.tarun.kahani.core.Angles.Piece pc = figs.get(use[i]);
            Bitmap pb = Bitmap.createBitmap(pc.px, pc.w, pc.h, Bitmap.Config.ARGB_8888);
            java.io.FileOutputStream fo = new java.io.FileOutputStream(new File(p.dir, "pose_" + i + ".png"));
            pb.compress(Bitmap.CompressFormat.PNG, 100, fo);
            fo.close();
            com.tarun.kahani.core.Cutout.Result r = com.tarun.kahani.core.Cutout.process(pc.px.clone(), pc.w, pc.h, false);
            String face = r.faceFound && Math.abs(angle[i]) < 46
                    ? String.format(java.util.Locale.US, "%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f", r.mouthX, r.mouthY, r.mouthW / 2f, r.eyeLX, r.eyeY, r.eyeRX, r.eyeY, r.eyeR)
                    : "0|0|0|0|0|0|0|0";
            if (i == 0) assertTrue("the crying front picture has a face to speak with", r.faceFound);
            cast.append("pose|").append(key).append("|pose_").append(i).append(".png|").append((int) angle[i]).append("|").append(pose[i]).append("|").append(emo[i])
                    .append("|").append(pose[i] == com.tarun.kahani.core.PoseSense.SIT ? "0.72" : "1.0").append("|").append(face).append("\n");
        }
        p.write("cast.txt", cast.toString());
        Art art = Art.fromManifest(p.read("cast.txt"), story, p.loader());
        Art.Sprite sp = art.sprites.get(vrinda.id);
        assertNotNull(sp);
        assertTrue("six pose pictures loaded: " + (sp.poses == null ? 0 : sp.poses.size()), sp.poses != null && sp.poses.size() == 6);
        Director d = new Director(story, new Director.Options());
        Film film = d.prepare();
        film = d.direct(art);
        assertTrue("the shot list names the pictures used", film.shotList.contains("PICTURES USED"));
        Film.Actor her = null;
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors) if (a.c == vrinda && her == null) her = a;
        assertNotNull(her);
        // the crying line and the laugh, by their times
        Film.Speak cry = null, sit = null;
        for (Film.Speak sk : her.speaks) { if (sk.emotion == com.tarun.kahani.core.Pose.SAD && cry == null) cry = sk; else if (sk.emotion != com.tarun.kahani.core.Pose.SAD) sit = sk; }
        assertNotNull("the crying line", cry);
        assertNotNull("the sitting line", sit);
        int walkShots = 0, cryShots = 0, sitShots = 0, backShots = 0, realShots = 0;
        StringBuilder log = new StringBuilder();
        for (Film.Shot sh : film.shots) {
            Integer idx = sh.pictures.get(vrinda.id);
            log.append(String.format(java.util.Locale.US, "%5.1f %-6s %s%n", sh.t, idx == null ? "-" : String.valueOf(idx), sh.view));
            if (idx == null) continue;
            if (idx >= 0) realShots++;
            Film.Key k = her.stateAt(sh.t + Math.max(0.05f, Math.min(sh.dur * 0.5f, 1.2f)));
            boolean moving = false;
            float mid = sh.t + Math.max(0.05f, Math.min(sh.dur * 0.5f, 1.2f));
            for (Film.Key kk : her.keys) if (kk.moveDur > 0 && mid >= kk.t && mid < kk.t + kk.moveDur) moving = true;
            if (sh.ots.length() > 0 && sh.ots.equals(vrinda.shown())) { assertTrue("behind her shoulder: her back picture, got " + idx, idx == 2); backShots++; }
            else if (k.body == com.tarun.kahani.core.Pose.SIT) { assertTrue("sitting: her sitting picture, got " + idx, idx == 4); sitShots++; }
            else if (moving && k.body == com.tarun.kahani.core.Pose.STAND) {
                assertTrue("walking: her sideways walking picture, got " + idx, idx == 1);
                int[] cyc = sh.cycles.get(vrinda.id);
                assertTrue("a step cycle of her two sideways pictures: " + java.util.Arrays.toString(cyc), cyc != null && cyc.length == 2 && cyc[0] == 1 && cyc[1] == 5);
                walkShots++;
            }
            else if (cry.t0 < sh.t + sh.dur && cry.t1 > sh.t && k.body == com.tarun.kahani.core.Pose.STAND) { assertTrue("crying: her crying picture, got " + idx, idx == 0); cryShots++; }
        }
        System.out.println("CASTING of " + vrinda.shown() + ":\n" + log);
        System.out.println("CASTING: walking " + walkShots + ", crying " + cryShots + ", sitting " + sitShots + ", back " + backShots + ", real pictures in " + realShots + " of " + film.shots.size() + " shots");
        assertTrue("a walking shot", walkShots >= 1);
        assertTrue("a crying shot", cryShots >= 1);
        assertTrue("a sitting shot", sitShots >= 1);
        assertTrue("real pictures used in most of her shots: " + realShots, realShots >= 3);
        assertTrue("the shot list says which picture: " + film.shotList, film.shotList.contains("your picture 1 (front, standing, sad)") && film.shotList.contains("your picture 5 (front, sitting, neutral)"));
        // the renderer draws a frame of the crying shot with the real picture, without error
        Film.Shot crying = null;
        for (Film.Shot sh : film.shots) { Integer idx = sh.pictures.get(vrinda.id); if (idx != null && idx == 0) { crying = sh; break; } }
        assertNotNull(crying);
        Bitmap bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(bmp, 4);
        Renderer rd = new Renderer(film, art);
        // every shot renders (the walk with its step cycle at two phases, the back, the seat, the laugh)
        long t0r = System.currentTimeMillis();
        int rendered = 0;
        for (Film.Shot sh : film.shots) { rd.render(g, sh.t + Math.min(0.4f, sh.dur * 0.5f)); rendered++; if (sh.cycles.containsKey(vrinda.id)) { rd.render(g, sh.t + Math.min(0.4f, sh.dur * 0.5f) + 0.36f); rendered++; } }
        System.out.println("CASTING: " + rendered + " frames of " + film.shots.size() + " shots rendered in " + (System.currentTimeMillis() - t0r) + " ms");
        rd.render(g, crying.t + 0.5f);
        int[] fpx = new int[1280 * 720];
        bmp.getPixels(fpx, 0, 1280, 0, 0, 1280, 720);
        long sum = 0;
        for (int i = 0; i < fpx.length; i += 97) sum += (fpx[i] >> 8) & 255;
        assertTrue("the crying shot's frame is not blank", sum > 0);
        OUT.mkdirs();
        FileOutputStream fo = new FileOutputStream(new File(OUT, "casting_crying.png"));
        bmp.compress(Bitmap.CompressFormat.PNG, 100, fo);
        fo.close();
    }

    /**
     * v29: a sheet of several figures is split whichever way it reaches a character — placed by the director's own
     * library placement (a library picture named as the character), or chosen through the character's picture
     * picker — never saved whole as the front picture.
     */
    @Test
    public void sheetChosenAnywhereIsSplit() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        File[] sheets = userSheets();
        File girl = new File(sheets[0].getParentFile(), "sheet02.jpg"), girl2 = new File(sheets[0].getParentFile(), "sheet15.jpg");
        byte[] sheet = Files.readAllBytes(girl.toPath());
        Class<?> ss = Class.forName("com.tarun.kahani.app.SheetSaver");
        Method isSheet = ss.getDeclaredMethod("isSheet", byte[].class, boolean.class);
        isSheet.setAccessible(true);
        assertTrue("a sheet is a sheet", (Boolean) isSheet.invoke(null, sheet, false));
        assertTrue("a single picture is not", !(Boolean) isSheet.invoke(null, Files.readAllBytes(new File(ASSETS, "sample/char_vrinda.jpg").toPath()), false));
        // 1. the director's own placement from the library: the sheet named as the character
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        java.lang.reflect.Field lf = MainActivity.class.getDeclaredField("library");
        lf.setAccessible(true);
        com.tarun.kahani.app.Library lib = (com.tarun.kahani.app.Library) lf.get(a);
        Project p = Project.create(ctx);
        Files.copy(new File(ASSETS, "sample_story.txt").toPath(), new File(p.dir, "script.txt").toPath());
        Story st = ScriptParser.parse(p.read("script.txt"));
        Story.CharacterDef vrinda = null;
        for (Story.CharacterDef c : st.cast()) if (c.displayName.contains("वृंदा")) vrinda = c;
        assertNotNull(vrinda);
        Method keyFor = MainActivity.class.getDeclaredMethod("keyFor", Story.CharacterDef.class);
        keyFor.setAccessible(true);
        java.lang.reflect.Field pf = MainActivity.class.getDeclaredField("project");
        pf.setAccessible(true);
        pf.set(a, p);
        String key = (String) keyFor.invoke(a, vrinda);
        lib.addBytes("pic", "", vrinda.displayName, "", sheet, ".jpg", "test");
        Class<?> al = Class.forName("com.tarun.kahani.app.AutoLibrary");
        Method fill = al.getDeclaredMethod("fill", android.content.Context.class, Project.class, Story.class);
        fill.setAccessible(true);
        String notes = (String) fill.invoke(null, ctx, p, st);
        String cast = p.read("cast.txt");
        System.out.println("SHEET AUTO: " + notes + "\nCAST: " + cast);
        int poses = 0;
        for (String l : cast.split("\n")) if (l.startsWith("pose|" + key + "|")) poses++;
        assertTrue("the director split the sheet it placed: " + poses + " pose pictures", poses >= 8);
        assertTrue("a front picture from the sheet", cast.contains("char|" + key + "|"));
        String front = p.manifestLine("char", key).split("\\|")[2];
        Bitmap fb = android.graphics.BitmapFactory.decodeFile(p.file(front).getAbsolutePath());
        assertTrue("the front is one figure, not the sheet: " + fb.getWidth() + "x" + fb.getHeight(), fb.getHeight() > fb.getWidth());
        // 2. the picker: a second sheet chosen for the character through "Picture: …"
        com.tarun.kahani.app.Library.Item it2 = lib.addBytes("pic", "", "sheet two", "", Files.readAllBytes(girl2.toPath()), ".jpg", "test");
        java.lang.reflect.Field tf = MainActivity.class.getDeclaredField("target");
        tf.setAccessible(true);
        tf.set(a, "char:" + key);
        Method use = MainActivity.class.getDeclaredMethod("usePicture", com.tarun.kahani.app.Library.Item.class);
        use.setAccessible(true);
        org.robolectric.shadows.ShadowToast.reset();
        use.invoke(a, it2);
        for (int i = 0; i < 1200 && !uploadDone(); i++) { idle(); Thread.sleep(100); }
        idle();
        cast = p.read("cast.txt");
        int poses2 = 0;
        for (String l : cast.split("\n")) if (l.startsWith("pose|" + key + "|")) poses2++;
        System.out.println("SHEET PICKER toast: " + org.robolectric.shadows.ShadowToast.getTextOfLatestToast() + " — pose pictures " + poses + " → " + poses2);
        assertTrue("the picker split the second sheet too: " + poses2, poses2 >= poses + 8);
        front = p.manifestLine("char", key).split("\\|")[2];
        fb = android.graphics.BitmapFactory.decodeFile(p.file(front).getAbsolutePath());
        assertTrue("the front is still one figure: " + fb.getWidth() + "x" + fb.getHeight(), fb.getHeight() > fb.getWidth());
        ac.pause().stop().destroy();
    }

    /**
     * v30: the "AI Animated Film Director — Reference-Based Training & Production Guide" is trained in: the twelve
     * principles and the sections mapped to code, the scene brief at the head of every scene of the shot list, the
     * weighted quality score with the critical-defect override at its end, the guide on the protocols screen.
     */
    @Test
    public void directorTrainingGuideIsTrainedAndScored() throws Exception {
        assertTrue(com.tarun.kahani.core.DirectorTraining.TWELVE_MAP.length == 12);
        for (int i = 0; i < 12; i++) {
            String name = com.tarun.kahani.core.DirectorTraining.TWELVE_MAP[i][0].toLowerCase(java.util.Locale.US);
            assertTrue(name + " vs " + com.tarun.kahani.core.PixarLead.TWELVE[i], name.equals(com.tarun.kahani.core.PixarLead.TWELVE[i]));
            assertTrue("principle mapped to code: " + name, com.tarun.kahani.core.DirectorTraining.TWELVE_MAP[i][1].length() > 30);
        }
        assertTrue(com.tarun.kahani.core.DirectorTraining.SECTIONS.length == 15);
        assertTrue(com.tarun.kahani.core.DirectorTraining.SUMMARY.contains("cannot do"));
        File guide = new File(ASSETS, "director_reference_training_guide.md");
        assertTrue("the guide is bundled", guide.exists());
        String g = new String(Files.readAllBytes(guide.toPath()), "UTF-8");
        assertTrue(g.contains("Twelve Principles") && g.contains("Quality-Control Scoring Engine") && g.contains("Master System Prompt"));
        // the sample film: a scene brief per scene, the score card at the end
        Project p = sampleProject();
        Story story = ScriptParser.parse(p.read("script.txt"));
        Art art = Art.fromManifest(p.read("cast.txt"), story, p.loader());
        Director d = new Director(story, new Director.Options());
        Film film = d.prepare();
        film = d.direct(art);
        int briefs = 0;
        for (String l : film.shotList.split("\n")) if (l.startsWith("SCENE BRIEF")) briefs++;
        assertTrue("a brief per scene: " + briefs + " of " + story.scenes.size(), briefs >= Math.min(3, story.scenes.size()) && briefs <= story.scenes.size() + 2);
        assertTrue(film.shotList.contains("  Subjects: ") && film.shotList.contains("  Camera: ") && film.shotList.contains("  Constraints: "));
        String card = com.tarun.kahani.core.DirectorTraining.scoreCard(film, art, story, null);
        System.out.println("TRAINING SCORE:\n" + card);
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("QC SCORE[^:]*: (\\d+)/100").matcher(card);
        assertTrue("a score", m.find());
        int sc = Integer.parseInt(m.group(1));
        assertTrue("score 0..100: " + sc, sc >= 0 && sc <= 100);
        int cats = 0;
        for (String c : com.tarun.kahani.core.SceneMaker.CATEGORIES) if (card.contains(c)) cats++;
        assertTrue("six categories", cats == 6);
        assertTrue("the override line", card.contains("Critical-defect override: "));
        assertTrue("no character of the sample is missing from its scene", com.tarun.kahani.core.DirectorTraining.missingCharacters(film) == 0);
        assertTrue("the shot list ends with the score", film.shotList.contains("QC SCORE (training guide §12"));
        // the protocols screen shows the guide
        RuntimeEnvironment.getApplication().getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        Method sp = MainActivity.class.getDeclaredMethod("showProtocol");
        sp.setAccessible(true);
        sp.invoke(a);
        idle();
        android.app.Dialog dlg = org.robolectric.shadows.ShadowDialog.getLatestDialog();
        assertNotNull("the protocols dialog", dlg);
        String all = android.text.TextUtils.join("\n", texts(dlg.getWindow().getDecorView(), new ArrayList<String>()));
        System.out.println("PROTOCOLS DIALOG: " + all.length() + " chars; training summary shown: " + all.contains("Reference-Based Training") + "; guide text shown: " + all.contains("Twelve Principles"));
        assertTrue("the training summary on the protocols screen", all.contains("Reference-Based Training") && all.contains("What a phone cannot do"));
        ac.pause().stop().destroy();
    }

    /**
     * v31: a sheet added to the library itself (Home, the library screen, the bulk picker) is split at once: the
     * library holds its figures — the front named as the sheet, the best of each angle a view, every figure with
     * its reading — and the director takes them all when it places that character; a place sheet becomes its
     * panels (wide view, reverse angle, the rest).
     */
    @Test
    public void sheetAddedToTheLibraryIsSplitAndFollowsTheCharacter() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        File[] sheets = userSheets();
        byte[] girl = Files.readAllBytes(new File(sheets[0].getParentFile(), "sheet02.jpg").toPath());
        byte[] cave = Files.readAllBytes(new File(sheets[0].getParentFile(), "sheet39.jpg").toPath());
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        java.lang.reflect.Field lf = MainActivity.class.getDeclaredField("library");
        lf.setAccessible(true);
        com.tarun.kahani.app.Library lib = (com.tarun.kahani.app.Library) lf.get(a);
        Class<?> ss = Class.forName("com.tarun.kahani.app.SheetSaver");
        Method save = ss.getDeclaredMethod("saveToLibrary", com.tarun.kahani.app.Library.class, String.class, String.class, byte[].class, String.class);
        save.setAccessible(true);
        // 1. the girl's sheet, kind left to the picture: figures
        List<com.tarun.kahani.app.Library.Item> fam = (List<com.tarun.kahani.app.Library.Item>) save.invoke(null, lib, "IMG_20261009", "", girl, "test");
        assertTrue("split into figures: " + fam.size(), fam.size() >= 10);
        com.tarun.kahani.app.Library.Item main = fam.get(0);
        assertTrue("the main is the front, a person: " + main.kind + " " + main.name, "person".equals(main.kind) && "1".equals(main.meta("sheetMain")));
        int children = 0, tagged = 0, views = 0;
        for (com.tarun.kahani.app.Library.Item it : lib.find("pic", null, null)) {
            if (main.id.equals(it.meta("sheet"))) children++;
            if (it.meta("posetag") != null && (main.id.equals(it.meta("sheet")) || it == main)) tagged++;
            if (main.id.equals(it.meta("sheet")) && "view".equals(it.kind) && it.name.contains(" view)")) views++;
        }
        System.out.println("LIBRARY SHEET: " + fam.size() + " items, " + children + " children, " + tagged + " with readings, " + views + " views; main " + main.name);
        assertTrue("the figures belong to the main: " + children, children >= 9);
        assertTrue("readings kept: " + tagged, tagged >= 8);
        assertTrue("views among them: " + views, views >= 2);
        // the user names it: the whole family follows the name
        Method rename = ss.getDeclaredMethod("rename", com.tarun.kahani.app.Library.class, com.tarun.kahani.app.Library.Item.class, String.class, String.class, String.class);
        rename.setAccessible(true);
        String old = main.name;
        main.name = "वृंदा"; main.kind = "person";
        rename.invoke(null, lib, main, old, "वृंदा", "person");
        lib.save();
        int named = 0;
        for (com.tarun.kahani.app.Library.Item it : lib.find("pic", null, null)) if ("वृंदा".equals(it.meta("ofName")) && it.name.startsWith("वृंदा (")) named++;
        assertTrue("renamed family: " + named, named >= 9);
        // 2. the director places her from the library: the front, the views and the pose pictures follow
        Project p = Project.create(ctx);
        Files.copy(new File(ASSETS, "sample_story.txt").toPath(), new File(p.dir, "script.txt").toPath());
        Story st = ScriptParser.parse(p.read("script.txt"));
        Story.CharacterDef vrinda = null;
        for (Story.CharacterDef c : st.cast()) if (c.displayName.contains("वृंदा")) vrinda = c;
        assertNotNull(vrinda);
        java.lang.reflect.Field pf = MainActivity.class.getDeclaredField("project");
        pf.setAccessible(true);
        pf.set(a, p);
        Method keyFor = MainActivity.class.getDeclaredMethod("keyFor", Story.CharacterDef.class);
        keyFor.setAccessible(true);
        String key = (String) keyFor.invoke(a, vrinda);
        Class<?> al = Class.forName("com.tarun.kahani.app.AutoLibrary");
        Method fill = al.getDeclaredMethod("fill", android.content.Context.class, Project.class, Story.class);
        fill.setAccessible(true);
        String notes = (String) fill.invoke(null, ctx, p, st);
        String cast = p.read("cast.txt");
        int poses = 0, viewLines = 0;
        for (String l : cast.split("\n")) { if (l.startsWith("pose|" + key + "|")) poses++; if (l.startsWith("view|" + key + "|")) viewLines++; }
        System.out.println("LIBRARY SHEET → film: " + notes + "\npose lines " + poses + ", view lines " + viewLines);
        assertTrue("her front from the library", cast.contains("char|" + key + "|"));
        String front = p.manifestLine("char", key).split("\\|")[2];
        Bitmap fb = android.graphics.BitmapFactory.decodeFile(p.file(front).getAbsolutePath());
        assertTrue("the front is one figure: " + fb.getWidth() + "x" + fb.getHeight(), fb.getHeight() > fb.getWidth());
        assertTrue("her pose pictures followed: " + poses, poses >= 8);
        assertTrue("her views followed: " + viewLines, viewLines >= 1);
        // 3. a place sheet, kind left to the picture: panels
        List<com.tarun.kahani.app.Library.Item> place = (List<com.tarun.kahani.app.Library.Item>) save.invoke(null, lib, "गुफा", "", cave, "test");
        assertTrue("split into panels: " + place.size(), place.size() >= 8);
        assertTrue("the wide view is a place: " + place.get(0).kind, "place".equals(place.get(0).kind) && "गुफा".equals(place.get(0).name));
        assertTrue("the reverse angle", "reverse".equals(place.get(1).meta("view")) && place.get(1).name.contains("reverse"));
        Bitmap pb = android.graphics.BitmapFactory.decodeStream(lib.open(place.get(0)));
        // a whole panel (the sheet's panels are portrait, five to a row): picture content in every corner, not the sheet's white
        int dark = 0;
        for (int cy = 0; cy < 2; cy++) for (int cx = 0; cx < 2; cx++) {
            int c = pb.getPixel(cx == 0 ? pb.getWidth() / 10 : pb.getWidth() * 9 / 10, cy == 0 ? pb.getHeight() / 10 : pb.getHeight() * 9 / 10);
            if (((c >> 16) & 255) + ((c >> 8) & 255) + (c & 255) < 690) dark++;
        }
        assertTrue("a whole panel with content in its corners (" + dark + "/4), " + pb.getWidth() + "x" + pb.getHeight(), dark >= 3 && pb.getWidth() > 200);
        ac.pause().stop().destroy();
    }

    /**
     * v31: three simple stories (Hindi, English, Hinglish) made end to end the way the phone makes them — the user's
     * sheets added to the library and split, the director placing them by name, the film job with its animatic,
     * the shot check and the final QC — and the result measured: shots, pictures used, the QC score, missing
     * characters, frames rendered for a look. The numbers go to build/frames/three_stories.txt.
     */
    @Test
    public void threeSimpleStoriesEndToEnd() throws Exception {
        org.robolectric.shadows.ShadowMediaCodec.CodecConfig.Codec copy = new org.robolectric.shadows.ShadowMediaCodec.CodecConfig.Codec() {
            public void process(java.nio.ByteBuffer in, java.nio.ByteBuffer out) {
                int n = Math.min(in.remaining(), out.remaining());
                for (int i = 0; i < Math.min(n, 64); i++) out.put(in.get());
                in.position(in.limit());
            }
        };
        org.robolectric.shadows.ShadowMediaCodec.addEncoder("video/avc", new org.robolectric.shadows.ShadowMediaCodec.CodecConfig(1280 * 720 * 2, 4096, copy));
        org.robolectric.shadows.ShadowMediaCodec.addEncoder("audio/mp4a-latm", new org.robolectric.shadows.ShadowMediaCodec.CodecConfig(16384, 4096, copy));
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        File[] sheets = userSheets();
        File dir = sheets[0].getParentFile();
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        java.lang.reflect.Field lf = MainActivity.class.getDeclaredField("library");
        lf.setAccessible(true);
        com.tarun.kahani.app.Library lib = (com.tarun.kahani.app.Library) lf.get(a);
        Class<?> ss = Class.forName("com.tarun.kahani.app.SheetSaver");
        Method save = ss.getDeclaredMethod("saveToLibrary", com.tarun.kahani.app.Library.class, String.class, String.class, byte[].class, String.class);
        save.setAccessible(true);
        Class<?> al = Class.forName("com.tarun.kahani.app.AutoLibrary");
        Method fill = al.getDeclaredMethod("fill", android.content.Context.class, Project.class, Story.class);
        fill.setAccessible(true);
        // the library, as the user would fill it: sheets named as the story names them, a garden from the sample
        String[][] sheetFor = {{"मीना", "sheet15.jpg", "person"}, {"राजू", "sheet01.jpg", "person"}, {"Asha", "sheet04.jpg", "person"}, {"Khan", "sheet11.jpg", "person"},
                {"Vrinda", "sheet02.jpg", "person"}, {"Bull", "sheet09.jpg", "person"}, {"गुफा", "sheet39.jpg", "place"}, {"the cave", "sheet34.jpg", "place"}, {"gufa", "sheet36.jpg", "place"}};
        StringBuilder rep = new StringBuilder("THREE SIMPLE STORIES — v31 end to end (Robolectric, 360p)\n\n");
        for (String[] sf : sheetFor) {
            List<?> fam = (List<?>) save.invoke(null, lib, sf[0], sf[2], Files.readAllBytes(new File(dir, sf[1]).toPath()), "test");
            rep.append(String.format(java.util.Locale.US, "library: %-9s ← %s split into %d pictures%n", sf[0], sf[1], fam.size()));
            assertTrue(sf[0] + " split: " + fam.size(), fam.size() >= 8);
        }
        lib.addBytes("pic", "place", "महल का बगीचा", "", Files.readAllBytes(new File(ASSETS, "sample/bg_garden.jpg").toPath()), ".jpg", "test");
        String[] titles = {"मीना और राजू की तितली (Hindi)", "Asha and the Beast (English)", "Vrinda aur Bull (Hinglish)"};
        String[] scripts = {
                "पात्र:\n1. मीना (8 वर्ष): लड़की, सफ़ेद-नीला कुर्ता, दो चोटियाँ, कंधे पर झोला\n2. राजू बंदर: लाल-भूरे बालों वाला छोटा बंदर, लाल बंडी\n"
                + "स्थान:\n1. महल का बगीचा: फूलों और तितलियों वाला बड़ा बगीचा\n2. गुफा: पहाड़ की गुफा, दरवाज़े पर पीतल का घंटा\n"
                + "मीना और राजू की तितली\n"
                + "दृश्य 1: बगीचे में सुबह\n(स्थान: महल का बगीचा। सुबह का समय है। मीना फूलों के पास खड़ी है।)\n"
                + "(राजू बंदर दौड़ता हुआ आता है।)\n"
                + "राजू (हँसते हुए): \"मीना, मेरे साथ गुफा चलो!\"\n"
                + "मीना (रोते हुए): \"मुझे गुफा से डर लगता है।\"\n"
                + "राजू (प्यार से): \"डरो मत, मैं तुम्हारे साथ हूँ।\"\n"
                + "(मीना मुस्कुराती है और दोनों चल पड़ते हैं।)\n"
                + "दृश्य 2: गुफा के दरवाज़े पर\n(स्थान: गुफा। दोपहर। मीना और राजू गुफा के दरवाज़े पर आते हैं।)\n"
                + "मीना (हैरानी से): \"देखो, कितना बड़ा घंटा!\"\n"
                + "(राजू घंटा बजाता है। \"टन-टन\" की आवाज़ गूँजती है।)\n"
                + "राजू (खिलखिलाकर): \"अब गुफा का दरवाज़ा खुलेगा!\"\n"
                + "(मीना पत्थर पर बैठ जाती है और हँसती है।)\n",
                "Characters:\n1. Asha (10 years): a brave girl, turquoise lehenga-choli, a long braid, a sword on her belt\n2. Khan: a huge black-furred horned monster (राक्षस) with chains and bones\n"
                + "Places:\n1. the cave: a dark cave with green crystals and bones\n"
                + "Asha and the Beast\n"
                + "Scene 1: At the cave\n(Place: the cave. Evening. Asha walks into the cave with her sword.)\n"
                + "(Khan comes out of the dark, roaring.)\n"
                + "Khan (angrily): \"Who dares to enter my cave?\"\n"
                + "Asha (bravely): \"I am Asha. Give back the village bell!\"\n"
                + "(Khan laughs and raises his arms.)\n"
                + "Khan (laughing): \"Take it, if you can!\"\n"
                + "(Asha runs at him and points her sword. Khan sits down, surprised.)\n"
                + "Asha (kindly): \"Let us be friends instead.\"\n"
                + "Khan (happily): \"Friends. I like that.\"\n",
                "Characters:\n1. Vrinda (11 saal): bahadur ladki, firozi ghaghra-choli, lambi choti\n2. Bull: ek bada kala bull (बैल), seengon wala\n"
                + "Places:\n1. gufa: pahad ki gufa, darwaze par ghanta\n"
                + "Vrinda aur Bull\n"
                + "Scene 1: gufa ke bahar\n(Sthan: gufa. Subah. Vrinda gufa ke bahar khadi hai.)\n"
                + "(Bull daudta hua aata hai.)\n"
                + "Bull (gusse se): \"Meri gufa se door raho!\"\n"
                + "Vrinda (haste hue): \"Main sirf ghanta dekhne aayi hoon.\"\n"
                + "(Bull ruk jata hai aur baith jata hai.)\n"
                + "Bull (udaas hokar): \"Mujhe koi dost nahi hai.\"\n"
                + "Vrinda (pyar se): \"Main tumhari dost banungi.\"\n"
                + "(Dono haste hain. Ghanta \"tan-tan\" bajta hai.)\n"};
        OUT.mkdirs();
        int storiesDone = 0;
        List<String> problems = new ArrayList<String>();
        final Project[] lastProject = {null};
        for (int si = 0; si < scripts.length; si++) {
            long t0 = System.currentTimeMillis();
            Project p = Project.create(ctx);
            p.write("script.txt", scripts[si]);
            p.write("edits.json", "{\"height\":360,\"aspect\":\"16:9\",\"brightness\":0.2,\"music\":0.7}");
            Story st = ScriptParser.parse(scripts[si]);
            rep.append("\n=== Story ").append(si + 1).append(": ").append(titles[si]).append('\n');
            rep.append("cast read: ");
            for (Story.CharacterDef c : st.cast()) rep.append(c.shown()).append(" (kind ").append(c.look == null ? "?" : String.valueOf(c.look.kind)).append(c.look != null && c.look.kind == com.tarun.kahani.core.Look.MONSTER ? " monster" : c.look != null && c.look.kind == com.tarun.kahani.core.Look.ANIMAL ? " animal" : c.look != null && c.look.kind == com.tarun.kahani.core.Look.MONKEY ? " monkey" : c.look != null && c.look.kind == com.tarun.kahani.core.Look.GIRL ? " girl" : "").append(") ");
            rep.append("; scenes: ").append(st.scenes.size()).append('\n');
            if (!(st.cast().size() >= 2)) problems.add("story " + (si + 1) + ": fewer than two characters read");
            String notes = (String) fill.invoke(null, ctx, p, st);
            rep.append("director's placement: ").append(notes.replace("\n", " | ")).append('\n');
            String cast = p.read("cast.txt");
            int chars = 0, poses = 0, scenes = 0;
            for (String l : cast.split("\n")) { if (l.startsWith("char|")) chars++; if (l.startsWith("pose|")) poses++; if (l.startsWith("scene|")) scenes++; }
            rep.append(String.format(java.util.Locale.US, "cast.txt: %d character pictures, %d pose pictures, %d place pictures%n", chars, poses, scenes));
            if (!(chars >= 2)) problems.add("story " + (si + 1) + ": not both characters placed from the library");
            if (!(poses >= 10)) problems.add("story " + (si + 1) + ": pose pictures did not follow (" + poses + ")");
            // the film job, as on the phone: proposals accepted, the shot check approved
            final com.tarun.kahani.app.FilmJob job = new com.tarun.kahani.app.FilmJob(ctx, p);
            Thread runner = new Thread(new Runnable() { public void run() { job.run(); } });
            runner.start();
            long wait = System.currentTimeMillis();
            boolean asked = false;
            while (!job.qcWaiting && runner.isAlive() && System.currentTimeMillis() - wait < 900000) {
                if (job.proposalsWaiting && !asked) { s3d("decideAll", p, null, ctx, true); asked = true; }
                Thread.sleep(50);
            }
            assertTrue("story " + (si + 1) + " did not reach the shot check: " + job.error, job.qcWaiting);
            int stills = 0;
            for (Integer i : job.qcShotIndex) if (i >= 0) stills++;
            job.approve();
            runner.join(900000);
            rep.append(String.format(java.util.Locale.US, "film job: done=%b error=%s seconds=%.1f stills checked=%d proposals asked=%b took=%d s%n", job.done, job.error, job.filmSeconds, stills, asked, (System.currentTimeMillis() - t0) / 1000));
            assertTrue("story " + (si + 1) + " failed: " + job.error, job.done);
            assertTrue(p.film().exists());
            // the shot list: pictures used, the score
            String qc = p.read("qc.txt");
            int shotsN = 0, used = 0, real = 0, cycles = 0;
            for (String l : qc.split("\n")) {
                if (l.startsWith("SHOT ID:")) shotsN++;
                if (l.startsWith("PICTURES USED:")) { used++; if (l.contains("your picture")) real++; if (l.contains("stepping through")) cycles++; }
            }
            java.util.Map<String, Integer> usedLines = new java.util.TreeMap<String, Integer>();
            for (String l : qc.split("\n")) if (l.startsWith("PICTURES USED:")) { Integer c = usedLines.get(l); usedLines.put(l, c == null ? 1 : c + 1); }
            for (java.util.Map.Entry<String, Integer> e : usedLines.entrySet()) rep.append("  ").append(e.getValue()).append("× ").append(e.getKey()).append('\n');
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("QC SCORE[^:]*: (\\d+)/100 — ([^\n]*)").matcher(qc);
            String score = "?", status = "?";
            int scoreN = -1;
            while (m.find()) { score = m.group(1); status = m.group(2); scoreN = Integer.parseInt(score); }
            java.util.regex.Matcher miss = java.util.regex.Pattern.compile("Critical-defect override: ([^\n]*)").matcher(qc);
            String crit = "?";
            while (miss.find()) crit = miss.group(1);
            rep.append(String.format(java.util.Locale.US, "shots: %d; shots with a PICTURES USED line: %d, of them drawn from the user's own pictures: %d, walking through step pictures: %d%n", shotsN, used, real, cycles));
            rep.append("QC SCORE: ").append(score).append("/100 — ").append(status).append("; override: ").append(crit).append('\n');
            System.out.println("STORY " + (si + 1) + " so far:\n" + rep);
            Files.write(new File(OUT, "three_stories.txt").toPath(), rep.toString().getBytes("UTF-8"));
            if (!(shotsN >= 6)) problems.add("story " + (si + 1) + ": only " + shotsN + " shots");
            if (!(real >= 2)) problems.add("story " + (si + 1) + ": the user's pictures drawn in only " + real + " shots");
            if (!(scoreN >= 70)) problems.add("story " + (si + 1) + ": score " + score);
            if (!crit.startsWith("none")) problems.add("story " + (si + 1) + ": critical defect — " + crit);
            // frames for a look: the director's shots rendered at 1280x720
            Art art = Art.fromManifest(p.read("cast.txt"), st, p.loader());
            Director d = new Director(st, new Director.Options());
            Film film = d.prepare();
            film = d.direct(art);
            Bitmap bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
            AndroidGfx g = new AndroidGfx(bmp, 4);
            Renderer rd = new Renderer(film, art);
            int step = Math.max(1, film.shots.size() / 6), fn = 0;
            for (int i = 0; i < film.shots.size() && fn < 8; i += step) {
                Film.Shot sh = film.shots.get(i);
                rd.render(g, sh.t + Math.min(0.6f, sh.dur * 0.5f));
                FileOutputStream fo = new FileOutputStream(new File(OUT, String.format(java.util.Locale.US, "story%d_shot%02d.png", si + 1, i + 1)));
                bmp.compress(Bitmap.CompressFormat.PNG, 100, fo);
                fo.close();
                fn++;
                Integer idx = null;
                for (Film.Seg sg : film.segs) for (Film.Actor ac2 : sg.actors) if (sh.pictures.get(ac2.c.id) != null && idx == null) idx = sh.pictures.get(ac2.c.id);
                rep.append(String.format(java.util.Locale.US, "  frame story%d_shot%02d.png at %.1f s: %s — %s%n", si + 1, i + 1, sh.t, com.tarun.kahani.core.ShotPlanner.SIZE_NAME[Math.max(0, Math.min(6, sh.size))], sh.view.length() > 0 ? sh.view : "drawn"));
            }
            storiesDone++;
            lastProject[0] = p;
        }
        // v33: an instruction after the film — made again with the pictures and approvals kept (no proposals, no shot check)
        {
            long t0 = System.currentTimeMillis();
            Project p = lastProject[0];
            String castBefore = p.read("cast.txt");
            com.tarun.kahani.core.Edits ed = com.tarun.kahani.core.Edits.fromJson(p.read("edits.json"));
            for (java.util.Map<String, Object> c : com.tarun.kahani.core.CommandParser.parse("make Vrinda bigger, scene 1 brighter, shorter shots, music softer", java.util.Arrays.asList("Vrinda", "Bull")).commands) ed.apply(c);
            p.write("edits.json", ed.toJson());
            final com.tarun.kahani.app.FilmJob job = new com.tarun.kahani.app.FilmJob(ctx, p);
            Thread runner = new Thread(new Runnable() { public void run() { job.run(); } });
            runner.start();
            boolean stopped = false;
            long wait = System.currentTimeMillis();
            while (runner.isAlive() && System.currentTimeMillis() - wait < 900000) {
                if (job.qcWaiting || job.proposalsWaiting) { stopped = true; if (job.proposalsWaiting) s3d("decideAll", p, null, ctx, true); if (job.qcWaiting) job.approve(); }
                Thread.sleep(50);
            }
            runner.join(900000);
            rep.append(String.format(java.util.Locale.US, "%nremake with an instruction: remake=%b done=%b stopped for a decision=%b pictures unchanged=%b took=%d s%n", job.remake, job.done, stopped, castBefore.equals(p.read("cast.txt")), (System.currentTimeMillis() - t0) / 1000));
            if (!job.remake) problems.add("remake: not recognised as a remake");
            if (!job.done) problems.add("remake: failed " + job.error);
            if (stopped) problems.add("remake: stopped for proposals or the shot check again");
            if (!castBefore.equals(p.read("cast.txt"))) problems.add("remake: the pictures changed");
            if (!p.read("qc.txt").contains("HUMAN QC: a remake")) problems.add("remake: not noted in the shot list");
        }
        rep.append("\nstories finished: ").append(storiesDone).append(" of 3\n");
        for (String pr : problems) rep.append("PROBLEM: ").append(pr).append('\n');
        Files.write(new File(OUT, "three_stories.txt").toPath(), rep.toString().getBytes("UTF-8"));
        System.out.println(rep);
        ac.pause().stop().destroy();
        assertTrue("problems: " + problems, problems.isEmpty() && storiesDone == 3);
    }

    /** The first view whose text starts with the prefix (the n-th such view when skip > 0), or null. */
    static View byText(View v, String prefix, int[] skip) {
        if (v instanceof TextView && ((TextView) v).getText().toString().startsWith(prefix)) { if (skip[0] <= 0) return v; skip[0]--; }
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { View r = byText(((ViewGroup) v).getChildAt(i), prefix, skip); if (r != null) return r; }
        return null;
    }

    /** Chooses the first item of the latest list dialog (the "Photos / gallery" line of the upload dialog). */
    static void firstDialogItem() {
        android.app.AlertDialog dlg = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("a dialog", dlg);
        assertNotNull("a list dialog", dlg.getListView());
        dlg.getListView().performItemClick(dlg.getListView(), 0, 0);
        idle();
    }

    /** The picker intent the app started last, which must be one that opens pictures. */
    static String pickerStarted(MainActivity a) {
        Intent i = shadowOf(a).getNextStartedActivity();
        assertNotNull("a picker was opened", i);
        String act = i.getAction() == null ? "" : i.getAction();
        if (Intent.ACTION_CHOOSER.equals(act)) { Intent in = i.getParcelableExtra(Intent.EXTRA_INTENT); act = in != null && in.getAction() != null ? in.getAction() : act; }
        assertTrue("a picture picker: " + act, act.contains("PICK_IMAGES") || act.equals(Intent.ACTION_OPEN_DOCUMENT) || act.equals(Intent.ACTION_GET_CONTENT) || act.equals(Intent.ACTION_PICK));
        return act;
    }

    /**
     * v33: pictures can be given everywhere, several at once — the Studio cards of a character, a place and a thing,
     * the story screen's missing-pictures card with the director's added scenes, the make-film popup's "Pictures
     * first", the progress screen's rows — every button opens the multi-picture picker.
     */
    @Test
    public void uploadButtonsOpenThePickerEverywhere() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        Project p = sampleProject();
        Story story = ScriptParser.parse(p.read("script.txt"));
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        set(a, "project", p);
        StringBuilder rep = new StringBuilder();
        // 1. the Studio: a character's one upload button, then a place's
        call(a, "showStudio", new Class<?>[0]);
        idle();
        View root = a.findViewById(android.R.id.content);
        View b = byText(root, "📷 Pictures (up to 10)", new int[]{0});
        assertNotNull("the character's upload button", b);
        b.performClick(); idle();
        firstDialogItem();
        rep.append("studio character: ").append(pickerStarted(a)).append('\n');
        int chars = story.cast().size();
        View pb = byText(root, "📷 Pictures (up to 10)", new int[]{chars});
        assertNotNull("the place's upload button", pb);
        pb.performClick(); idle();
        firstDialogItem();
        rep.append("studio place: ").append(pickerStarted(a)).append('\n');
        // a thing, named on the spot
        View tb = byText(root, "➕ Another thing of the story", new int[]{0});
        if (tb == null) tb = byText(root, "➕ A thing of the story", new int[]{0});
        assertNotNull("the thing button", tb);
        tb.performClick(); idle();
        android.app.AlertDialog nd = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(nd);
        EditText q = (EditText) byTextClass(nd.getWindow().getDecorView(), EditText.class);
        assertNotNull("the name box", q);
        q.setText("पगड़ी");
        nd.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick(); idle();
        firstDialogItem();
        rep.append("studio thing: ").append(pickerStarted(a)).append('\n');
        // 2. the story screen: the director's plan — a missing picture or an added scene, and a thing
        call(a, "showStory", new Class<?>[0]);
        idle();
        root = a.findViewById(android.R.id.content);
        View mb = byText(root, "📷 ", new int[]{0});
        View xb = byText(root, "🎬 ", new int[]{0});
        if (xb == null) xb = byText(root, "✅ ", new int[]{0});
        assertTrue("a plan row to upload for (missing: " + (mb != null) + ", added scene: " + (xb != null) + ")", mb != null || xb != null);
        (xb != null ? xb : mb).performClick(); idle();
        firstDialogItem();
        rep.append("story plan row: ").append(pickerStarted(a)).append('\n');
        // 3. the make-film popup: "Pictures first"
        call(a, "makeFilm", new Class<?>[0]);
        idle();
        android.app.AlertDialog mk = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("the make-film dialog", mk);
        Button neutral = mk.getButton(android.content.DialogInterface.BUTTON_NEUTRAL);
        assertNotNull("Pictures first", neutral);
        neutral.performClick(); idle();
        android.app.AlertDialog list = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(list);
        if (list.getListView() != null && list.getListView().getCount() > 0) {
            list.getListView().performItemClick(list.getListView(), 0, 0); idle();
            android.app.AlertDialog nxt = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
            if (nxt != null && nxt.getListView() != null && nxt != list) { nxt.getListView().performItemClick(nxt.getListView(), 0, 0); idle(); }
            else if (nxt != null && byTextClass(nxt.getWindow().getDecorView(), EditText.class) != null) {
                ((EditText) byTextClass(nxt.getWindow().getDecorView(), EditText.class)).setText("तलवार");
                nxt.getButton(android.content.DialogInterface.BUTTON_POSITIVE).performClick(); idle();
                firstDialogItem();
            }
            rep.append("popup pictures first: ").append(pickerStarted(a)).append('\n');
        } else rep.append("popup pictures first: no list (nothing missing)\n");
        // 4. the progress screen's rows ("Pictures in this film")
        LinearLayout card = new LinearLayout(a);
        call(a, "picturesCard", new Class<?>[]{LinearLayout.class, Story.class}, card, story);
        idle();
        View add = byText(card, "📷 ", new int[]{0});
        assertNotNull("a row's add/more button", add);
        add.performClick(); idle();
        firstDialogItem();
        rep.append("progress row: ").append(pickerStarted(a)).append('\n');
        System.out.println("UPLOAD BUTTONS:\n" + rep);
        ac.pause().stop().destroy();
    }

    static View byTextClass(View v, Class<?> k) {
        if (k.isInstance(v)) return v;
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) { View r = byTextClass(((ViewGroup) v).getChildAt(i), k); if (r != null) return r; }
        return null;
    }

    /**
     * v33: the post-production box understands plain instructions (size of a character, the light of a part, the
     * length of shots, music, voices, brightness, the shape…) without the internet, and keeps them in the edits.
     */
    @Test
    public void postProductionInstructionsAreUnderstood() throws Exception {
        List<String> names = java.util.Arrays.asList("वृंदा", "वानुषा", "राजू", "खान राक्षस", "Asha", "Khan", "Vrinda");
        String[][] asks = {
                {"make the monkey bigger", "size"}, {"Khan is too small", "size"}, {"make Asha a little smaller", "size"}, {"make Vrinda much bigger", "size"},
                {"scene 2 brighter", "scene_brightness"}, {"the cave is too dark", "scene_brightness"}, {"part 1 darker", "scene_brightness"}, {"make the garden scene much brighter", "scene_brightness"},
                {"shorter shots", "shot_length"}, {"more cuts", "shot_length"}, {"the cuts are too fast", "shot_length"}, {"longer shots please", "shot_length"},
                {"music softer", "music"}, {"the music is too loud", "music"}, {"remove the music", "music"}, {"make all voices louder", "voices"},
                {"Khan's voice deeper", "voice_pitch"}, {"Vrinda speaks slower", "voice_rate"}, {"brighter", "brightness"}, {"warmer colours", "warmth"},
                {"make it vertical for instagram", "aspect"}, {"720p", "resolution"}, {"subtitles on", "subtitles"}, {"faster pace", "speed"},
                {"black and white", "saturation"}, {"less contrast", "contrast"}, {"the background sounds are too loud", "ambience"}, {"reset everything", "reset"}};
        StringBuilder rep = new StringBuilder();
        int ok = 0;
        com.tarun.kahani.core.Edits ed = new com.tarun.kahani.core.Edits();
        for (String[] ask : asks) {
            com.tarun.kahani.core.CommandParser.Result r = com.tarun.kahani.core.CommandParser.parse(ask[0], names);
            String ops = "";
            for (java.util.Map<String, Object> c : r.commands) ops += (ops.length() > 0 ? "," : "") + c.get("op");
            boolean good = r.unknown.isEmpty() && ops.contains(ask[1]);
            if (good) ok++;
            rep.append(good ? "  ✔ " : "  ✖ ").append(ask[0]).append(" → ").append(ops).append(r.unknown.isEmpty() ? "" : " (not understood: " + r.unknown + ")").append('\n');
            for (java.util.Map<String, Object> c : r.commands) ed.apply(c);
        }
        System.out.println("POST-PRODUCTION:\n" + rep + ok + " of " + asks.length + " understood");
        assertTrue("every instruction understood: " + ok + " of " + asks.length + "\n" + rep, ok == asks.length);
        // the edits keep them, and survive the file
        com.tarun.kahani.core.Edits ed2 = new com.tarun.kahani.core.Edits();
        for (String[] ask : new String[][]{{"make the monkey bigger"}, {"scene 2 brighter"}, {"shorter shots"}, {"music softer"}})
            for (java.util.Map<String, Object> c : com.tarun.kahani.core.CommandParser.parse(ask[0], names).commands) ed2.apply(c);
        com.tarun.kahani.core.Edits back = com.tarun.kahani.core.Edits.fromJson(ed2.toJson());
        assertTrue("the monkey's size kept: " + back.charScale, back.charScale.get("राजू") != null && back.charScale.get("राजू") > 1.1f);
        assertTrue("scene 2's light kept: " + back.sceneBright, back.sceneBright.get("2") != null && back.sceneBright.get("2") > 0.1f);
        assertTrue("shorter shots kept: " + back.shotLength, back.shotLength < 0.9f);
        assertTrue("music kept: " + back.music, back.music < 0.9f);
    }

    /**
     * v33: twenty stories, short and long, in three languages, with the user's sheets — made end to end at 240p.
     * Run only when asked (-Dkahani.soak=1): it takes the better part of an hour. The report goes to
     * build/frames/soak.txt.
     */
    @Test
    public void twentyStoriesSoak() throws Exception {
        org.junit.Assume.assumeTrue("the soak runs with -Dkahani.soak=1", "1".equals(System.getProperty("kahani.soak")));
        org.robolectric.shadows.ShadowMediaCodec.CodecConfig.Codec copy = new org.robolectric.shadows.ShadowMediaCodec.CodecConfig.Codec() {
            public void process(java.nio.ByteBuffer in, java.nio.ByteBuffer out) {
                int n = Math.min(in.remaining(), out.remaining());
                for (int i = 0; i < Math.min(n, 64); i++) out.put(in.get());
                in.position(in.limit());
            }
        };
        org.robolectric.shadows.ShadowMediaCodec.addEncoder("video/avc", new org.robolectric.shadows.ShadowMediaCodec.CodecConfig(1280 * 720 * 2, 4096, copy));
        org.robolectric.shadows.ShadowMediaCodec.addEncoder("audio/mp4a-latm", new org.robolectric.shadows.ShadowMediaCodec.CodecConfig(16384, 4096, copy));
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        File[] sheets = userSheets();
        File dir = sheets[0].getParentFile();
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        java.lang.reflect.Field lf = MainActivity.class.getDeclaredField("library");
        lf.setAccessible(true);
        com.tarun.kahani.app.Library lib = (com.tarun.kahani.app.Library) lf.get(a);
        Class<?> ss = Class.forName("com.tarun.kahani.app.SheetSaver");
        Method save = ss.getDeclaredMethod("saveToLibrary", com.tarun.kahani.app.Library.class, String.class, String.class, byte[].class, String.class);
        save.setAccessible(true);
        Class<?> al = Class.forName("com.tarun.kahani.app.AutoLibrary");
        Method fill = al.getDeclaredMethod("fill", android.content.Context.class, Project.class, Story.class);
        fill.setAccessible(true);
        // the library: the user's sheets under the names the stories use (an upload per character and place)
        String[][] sheetFor = {{"मीना", "sheet15.jpg", "person"}, {"राजू", "sheet01.jpg", "person"}, {"Asha", "sheet04.jpg", "person"}, {"Khan", "sheet11.jpg", "person"},
                {"Vrinda", "sheet02.jpg", "person"}, {"Bull", "sheet09.jpg", "person"}, {"गुफा", "sheet39.jpg", "place"}, {"the cave", "sheet34.jpg", "place"}, {"gufa", "sheet36.jpg", "place"}};
        for (String[] sf : sheetFor) save.invoke(null, lib, sf[0], sf[2], Files.readAllBytes(new File(dir, sf[1]).toPath()), "soak");
        lib.addBytes("pic", "place", "महल का बगीचा", "", Files.readAllBytes(new File(ASSETS, "sample/bg_garden.jpg").toPath()), ".jpg", "soak");
        lib.addBytes("pic", "place", "the garden", "", Files.readAllBytes(new File(ASSETS, "sample/bg_garden.jpg").toPath()), ".jpg", "soak");
        lib.addBytes("pic", "place", "bagicha", "", Files.readAllBytes(new File(ASSETS, "sample/bg_garden.jpg").toPath()), ".jpg", "soak");
        String[] scripts = SoakStories.all();
        StringBuilder rep = new StringBuilder("TWENTY STORIES — soak (Robolectric, 240p)\n");
        List<String> problems = new ArrayList<String>();
        OUT.mkdirs();
        long tAll = System.currentTimeMillis();
        for (int si = 0; si < scripts.length; si++) {
            long t0 = System.currentTimeMillis();
            Project p = Project.create(ctx);
            p.write("script.txt", scripts[si]);
            p.write("edits.json", "{\"height\":240,\"aspect\":\"" + (si % 3 == 1 ? "9:16" : "16:9") + "\",\"brightness\":0.1,\"music\":0.7}");
            Story st = ScriptParser.parse(scripts[si]);
            String title = scripts[si].split("\n")[0];
            rep.append(String.format(java.util.Locale.US, "%n=== %2d. %s — %d lines, %d scenes, cast %d%n", si + 1, SoakStories.title(si), scripts[si].split("\n").length, st.scenes.size(), st.cast().size()));
            if (st.cast().size() < 1) { problems.add((si + 1) + ": no character read"); continue; }
            try { fill.invoke(null, ctx, p, st); } catch (Exception e) { problems.add((si + 1) + ": placement failed: " + e); }
            String cast = p.read("cast.txt");
            int chars = 0, poses = 0;
            for (String l : cast.split("\n")) { if (l.startsWith("char|")) chars++; if (l.startsWith("pose|")) poses++; }
            rep.append(String.format(java.util.Locale.US, "placed: %d character pictures, %d pose pictures%n", chars, poses));
            final com.tarun.kahani.app.FilmJob job = new com.tarun.kahani.app.FilmJob(ctx, p);
            Thread runner = new Thread(new Runnable() { public void run() { job.run(); } });
            runner.start();
            long wait = System.currentTimeMillis();
            boolean asked = false;
            while (!job.qcWaiting && runner.isAlive() && System.currentTimeMillis() - wait < 900000) {
                if (job.proposalsWaiting && !asked) { s3d("decideAll", p, null, ctx, true); asked = true; }
                Thread.sleep(50);
            }
            if (job.qcWaiting) job.approve();
            runner.join(900000);
            String qc = job.done ? p.read("qc.txt") : "";
            int shotsN = 0, real = 0;
            for (String l : qc.split("\n")) { if (l.startsWith("SHOT ID:")) shotsN++; if (l.startsWith("PICTURES USED:") && l.contains("your picture")) real++; }
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("QC SCORE[^:]*: (\\d+)/100").matcher(qc);
            int score = -1;
            while (m.find()) score = Integer.parseInt(m.group(1));
            java.util.regex.Matcher miss = java.util.regex.Pattern.compile("Critical-defect override: ([^\n]*)").matcher(qc);
            String crit = "?";
            while (miss.find()) crit = miss.group(1);
            rep.append(String.format(java.util.Locale.US, "film: done=%b error=%s seconds=%.0f shots=%d real-picture shots=%d score=%d critical=%s took=%d s%n",
                    job.done, job.error, job.filmSeconds, shotsN, real, score, crit.length() > 40 ? crit.substring(0, 40) : crit, (System.currentTimeMillis() - t0) / 1000));
            if (!job.done) problems.add((si + 1) + ": failed: " + job.error);
            else {
                if (shotsN < 4) problems.add((si + 1) + ": only " + shotsN + " shots");
                if (score >= 0 && score < 70) problems.add((si + 1) + ": score " + score);
                if (!crit.startsWith("none")) problems.add((si + 1) + ": critical — " + crit);
                if (poses > 0 && real == 0) problems.add((si + 1) + ": pose pictures present but none used");
            }
            Files.write(new File(OUT, "soak.txt").toPath(), rep.toString().getBytes("UTF-8"));
        }
        rep.append(String.format(java.util.Locale.US, "%nall %d stories in %d min; problems: %d%n", scripts.length, (System.currentTimeMillis() - tAll) / 60000, problems.size()));
        for (String pr : problems) rep.append("PROBLEM: ").append(pr).append('\n');
        Files.write(new File(OUT, "soak.txt").toPath(), rep.toString().getBytes("UTF-8"));
        System.out.println(rep);
        ac.pause().stop().destroy();
        assertTrue("problems: " + problems, problems.isEmpty());
    }

    /** The user's own sounds: recognised offline, matched in Hindi and English, used as backgrounds and effects. */
    @Test
    public void userSoundsAreUsedWhereTheStoryDescribesThem() throws Exception {
        RuntimeEnvironment.getApplication().getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        com.tarun.kahani.app.Library lib = com.tarun.kahani.app.Library.get(RuntimeEnvironment.getApplication());
        int sr = 32000;
        java.util.Random rnd = new java.util.Random(5);
        // 12 s of steady bright noise, like rain, saved with a meaningless phone file name
        float[] rain = new float[sr * 12];
        float prev = 0;
        for (int i = 0; i < rain.length; i++) { float n = rnd.nextFloat() * 2 - 1; rain[i] = (n - prev) * 0.3f; prev = n; }
        com.tarun.kahani.app.Library.Item r = lib.addBytes("sound", "amb", "AUD-20250101-WA0003", "", wav16(rain, sr), ".wav", "test");
        System.out.println("RAIN: kind=" + r.kind + " meta=" + r.meta);
        assertTrue(r.meta, "amb".equals(r.kind));
        assertTrue(r.meta, r.meta("sl").contains("rain") || r.meta("sl").contains("water"));
        // a short effect the user called "horse"
        float[] hooves = new float[sr * 2];
        for (int k = 0; k < 8; k++) for (int i = 0; i < 900; i++) hooves[k * sr / 4 + i] = (float) Math.sin(i * 0.2) * (1 - i / 900f) * 0.8f;
        com.tarun.kahani.app.Library.Item h = lib.addBytes("sound", "amb", "horse", "horse galloping", wav16(hooves, sr), ".wav", "test");
        System.out.println("HORSE: kind=" + h.kind + " meta=" + h.meta);
        assertTrue(h.meta, "sfx".equals(h.kind));
        String script = "पात्र:\n1. राजू (10 वर्ष): गाँव का लड़का\n\nदृश्य 1: गाँव\n(स्थान: गाँव, बारिश वाली रात)\n"
                + "घोड़ा दौड़ता हुआ आया।\nराजू: \"अरे! यह घोड़ा कहाँ से आया?\"\n";
        Story st = ScriptParser.parse(script);
        com.tarun.kahani.core.SoundLib sl = lib.soundLib();
        Director.Options o = new Director.Options();
        o.sounds = sl;
        Director d = new Director(st, o);
        d.prepare();
        Film f = d.direct(new Art());
        boolean horse = false;
        for (Film.Sfx x : f.sfx) if (x.type == Film.SFX_USER && h.path.equals(x.file)) horse = true;
        for (String n : f.notes) if (n.contains("🔊")) System.out.println("NOTE: " + n);
        assertTrue("the horse sound is not used", horse);
        boolean rainUsed = false;
        for (Film.Amb a : f.ambience) {
            com.tarun.kahani.core.SoundLib.Entry e = sl.best(a.words, "amb", null);
            System.out.println("AMBIENCE for \"" + a.words + "\" -> " + (e == null ? "-" : e.title));
            if (e != null && e.path.equals(r.path)) rainUsed = true;
        }
        assertTrue("the rain sound is not used for the rainy place", rainUsed);
    }

    /** Whole background job with a short script: no TTS available, fake hardware encoders. Must finish, not hang. */
    @Test
    public void filmJobFinishesEndToEnd() throws Exception {
        org.robolectric.shadows.ShadowMediaCodec.CodecConfig.Codec copy = new org.robolectric.shadows.ShadowMediaCodec.CodecConfig.Codec() {
            public void process(java.nio.ByteBuffer in, java.nio.ByteBuffer out) {
                int n = Math.min(in.remaining(), out.remaining());
                for (int i = 0; i < Math.min(n, 64); i++) out.put(in.get());
                in.position(in.limit());
            }
        };
        org.robolectric.shadows.ShadowMediaCodec.addEncoder("video/avc", new org.robolectric.shadows.ShadowMediaCodec.CodecConfig(1280 * 720 * 2, 4096, copy));
        org.robolectric.shadows.ShadowMediaCodec.addEncoder("audio/mp4a-latm", new org.robolectric.shadows.ShadowMediaCodec.CodecConfig(16384, 4096, copy));
        Project p = sampleProject();
        String script = "पात्र:\n1. मीना (8 वर्ष): लड़की, गुलाबी फ्रॉक, दो चोटियाँ\n2. राजू बंदर: लाल बंडी\nछोटी सी कहानी\n"
                + "दृश्य 1: बगीचे में\n(स्थान: महल का बगीचा। सुबह। मीना तितली के पीछे दौड़ रही है।)\n"
                + "मीना (हँसते हुए): \"राजू, देखो तितली!\"\n(राजू बंदर पेड़ से छलाँग मार कर आता है।)\n";
        p.write("script.txt", script);
        p.write("edits.json", "{\"height\":360,\"aspect\":\"9:16\",\"brightness\":0.2,\"music\":0.7}");
        // the user recorded Meena's line in their own voice: it must be used (and drive the lip sync)
        String line = com.tarun.kahani.core.Txt.forSpeech("राजू, देखो तितली!");
        File lines = new File(p.dir, "lines");
        lines.mkdirs();
        float[] tone = new float[32000];
        for (int i = 0; i < tone.length; i++) tone[i] = (float) (0.3 * Math.sin(i * 0.05) * (0.5 + 0.5 * Math.sin(i * 0.0007)));
        java.lang.reflect.Method h = com.tarun.kahani.app.FilmJob.class.getDeclaredMethod("hash", String.class);
        h.setAccessible(true);
        com.tarun.kahani.app.AudioIO.writeWav(new File(lines, h.invoke(null, "मीना|" + line) + ".wav"), tone, 32000);
        RuntimeEnvironment.getApplication().getSharedPreferences("kahani", 0).edit().putString("online", "0").commit();
        final com.tarun.kahani.app.FilmJob job = new com.tarun.kahani.app.FilmJob(RuntimeEnvironment.getApplication(), p);
        long t0 = System.currentTimeMillis();
        // Human QC: the job stops at the first frames of every shot and waits; the user fixes two shots and approves
        Thread runner = new Thread(new Runnable() { public void run() { job.run(); } });
        runner.start();
        long wait = System.currentTimeMillis();
        // the director asks first: the dolls the studio made for the two characters without a picture are proposals
        // until the user decides (here: use them all)
        boolean asked = false;
        while (!job.qcWaiting && runner.isAlive() && System.currentTimeMillis() - wait < 600000) {
            if (job.proposalsWaiting && !asked) {
                java.util.List<?> props = (java.util.List<?>) s3d("proposals", p);
                assertTrue("a doll proposed for the character without a picture, with its views", props.size() >= 2);
                s3d("decideAll", p, null, RuntimeEnvironment.getApplication(), true);
                asked = true;
            }
            Thread.sleep(50);
        }
        assertTrue("the job did not stop for the shot check: " + job.error, job.qcWaiting);
        // the director's manual (3.7): the animatic is the first card of the check, made with the real mix before the film
        boolean animatic = false;
        for (String[] it : job.qcItems) if (it[2].equals("animatic")) animatic = true;
        assertTrue("the animatic card", animatic);
        assertTrue("animatic.mp4 " + p.file("animatic.mp4").length(), p.file("animatic.mp4").length() > 1000);
        assertTrue("the director asked before using the 3D dolls", asked);
        assertTrue("the accepted doll is the character's picture", p.manifestLine("char", "मीना") != null);
        int shots = 0;
        for (Integer i : job.qcShotIndex) if (i >= 0) shots++;
        System.out.println("QC: " + job.qcItems.size() + " stills (" + shots + " shots)");
        assertTrue("no first frames to check", shots >= 2);
        for (String[] it : job.qcItems) assertTrue("missing still " + it[0], new File(it[0]).length() > 500);
        job.qcFixes.put(0, Director.FIX_CALM);
        job.qcFixes.put(1, Director.FIX_WIDER);
        job.approve();
        runner.join(600000);
        System.out.println("JOB: done=" + job.done + " failed=" + job.failed + " err=" + job.error + " warn=" + job.warning
                + " stage=" + job.stage + " secs=" + job.filmSeconds + " took=" + (System.currentTimeMillis() - t0) + "ms");
        assertTrue("job failed: " + job.error, job.done);
        assertTrue(p.film().exists());
        assertTrue("recorded line not used: " + job.voicedLines, job.voicedLines == 1);
        // the manual's records and checks are kept with the film
        String qc = p.read("qc.txt");
        for (String must : new String[]{"PROJECT ASSET INVENTORY", "FACIAL IDENTITY SPECIFICATION", "AUDIO CHECK", "EXPORT CHECK", "THREE-LEVEL REVIEW",
                "Gate 3 — Animatic: passed", "QA CHECKLIST", "ANIMATIC (the director's manual 3.7): made before the film"})
            assertTrue("qc.txt lacks: " + must, qc.contains(must));
    }

    @Test
    public void framesRenderThroughAndroidCanvas() throws Exception {
        Project p = sampleProject();
        Story story = ScriptParser.parse(p.read("script.txt"));
        long t0 = System.currentTimeMillis();
        Art art = Art.fromManifest(p.read("cast.txt"), story, p.loader());
        System.out.println("art loaded in " + (System.currentTimeMillis() - t0) + "ms sprites=" + art.sprites.size());
        assertTrue(art.sprites.size() >= 9);
        Director d = new Director(story, new Director.Options());
        Film film = d.prepare();
        film = d.direct(art);
        Bitmap bmp = Bitmap.createBitmap(1280, 720, Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(bmp, 4);
        Renderer r = new Renderer(film, art);
        OUT.mkdirs();
        float[] times = {4f, 9.5f, 20f, 26f, 45f, 60f, 75f, 95f, 120f, 150f, 175f, 200f, film.duration - 3f};
        long tr = System.currentTimeMillis();
        int[] px = new int[1280 * 720];
        for (float t : times) {
            r.render(g, t);
            bmp.getPixels(px, 0, 1280, 0, 0, 1280, 720);
            FileOutputStream o = new FileOutputStream(new File(OUT, String.format("android_%06.1f.png", t)));
            bmp.compress(Bitmap.CompressFormat.PNG, 100, o);
            o.close();
        }
        System.out.println("rendered " + times.length + " frames in " + (System.currentTimeMillis() - tr) + "ms");
        // the frame must not be blank
        int distinct = 0, last = 0;
        for (int i = 0; i < px.length; i += 997) if (px[i] != last) { distinct++; last = px[i]; }
        assertTrue("frame looks blank", distinct > 50);
        g.release();
    }

    /** A part rendered on its own through the Android canvas is not blank. */
    private static boolean rendersSomething(Film film, Art art, Film.Seg s, int w, int h) {
        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(bmp, 4);
        try {
            new Renderer(film, art).renderSeg(g, s, 0.5f);
            int[] px = new int[w * h];
            bmp.getPixels(px, 0, w, 0, 0, w, h);
            int distinct = 0, last = 0;
            for (int i = 0; i < px.length; i += 97) if (px[i] != last) { distinct++; last = px[i]; }
            return distinct > 40;
        } finally {
            g.release();
            bmp.recycle();
        }
    }

    /**
     * v16 — the Technical Director pipeline's steps 1, 2, 6 and 8 are done by the studio itself: a Character Lock
     * Sheet for every character and a Location Lock Plate (no characters) for every place render as pictures; every
     * shot is played frame by frame at check size and compared (no boiling, no shake, no floating feet); the finished
     * film is metered as it is written; the lip-sync face fill (8.3) is measured, validated and corrected; the
     * objective length is reported.
     */
    @Test
    public void finalQcPlaysEveryShotAndLocksEveryCharacterAndPlace() throws Exception {
        Project p = sampleProject();
        Story story = ScriptParser.parse(p.read("script.txt"));
        Art art = Art.fromManifest(p.read("cast.txt"), story, p.loader());
        Director d = new Director(story, new Director.Options());
        Film film = d.prepare();
        film = d.direct(art);
        assertTrue(film.shotList.contains("Lip-sync framing (8.3)"));
        assertTrue(film.shotList.contains("Objective (small films of 30-90 s)"));
        // steps 1-2: a lock sheet for every character of the cast (pictures and drawn puppets alike), a plate per place
        int sheets = 0;
        for (Story.CharacterDef c : story.cast()) {
            int[] sz = d.lockSheetSize(c);
            Film.Seg s = d.lockSheet(c, sz[0] / (float) sz[1]);
            assertTrue(s.actors.size() == 1 && s.actors.get(0).c == c);
            assertTrue("lock sheet blank: " + c.shown(), rendersSomething(film, art, s, sz[0] / 2, sz[1] / 2));
            sheets++;
        }
        assertTrue("sheets " + sheets, sheets >= 6);
        List<Object[]> plates = d.locationPlates();
        assertTrue("plates " + plates.size(), plates.size() >= 3);
        for (Object[] pl : plates) assertTrue(((Film.Seg) pl[1]).actors.isEmpty());
        assertTrue(rendersSomething(film, art, (Film.Seg) plates.get(0)[1], 320, 180));
        // step 6: the first 40 shots played frame by frame at check size
        final int w = 256, h = 144;
        final Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        final AndroidGfx g = new AndroidGfx(bmp, 4);
        final int[] px = new int[w * h];
        final int[] done = {0};
        long t0 = System.currentTimeMillis();
        FinalQc.Result r = FinalQc.check(film, art, new FinalQc.Surface() {
            public Gfx gfx() { return g; }
            public int[] pixels() { bmp.getPixels(px, 0, w, 0, 0, w, h); return px; }
            public int width() { return w; }
            public int height() { return h; }
        }, new FinalQc.Progress() {
            public void at(int i, int total) { done[0] = i; }
            public boolean cancelled() { return done[0] >= 40; }
        });
        System.out.println("final QC of 40 shots in " + (System.currentTimeMillis() - t0) + "ms\n" + r.text());
        if (r.stillLively > 0 || r.boiling > 0) {
            // what the check saw: the frames of the first flagged shot, for a look (build/qc_dump)
            java.util.regex.Matcher m = java.util.regex.Pattern.compile("shot (\\d+) at").matcher(r.notes.get(0));
            if (m.find()) {
                Film.Shot sh = film.shots.get(Integer.parseInt(m.group(1)) - 1);
                Renderer ren = new Renderer(film, art);
                File dd = new File("build/qc_dump");
                dd.mkdirs();
                for (float u : new float[]{0.05f, 0.5f, 0.95f}) for (int i = 0; i < 4; i++) {
                    float t = sh.t + sh.dur * u + i / 24f;
                    ren.render(g, t);
                    java.io.FileOutputStream o = new java.io.FileOutputStream(new File(dd, String.format(java.util.Locale.US, "s%03d_%07.3f.png", Integer.parseInt(m.group(1)), t)));
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, o);
                    o.close();
                }
                System.out.println("QC DUMP shot " + m.group(1) + " t=" + sh.t + " dur=" + sh.dur + " size=" + sh.size + " | " + sh.purpose + " | " + sh.action + " | " + sh.subject);
                for (Film.Seg sg : film.segs) if (sg.type == Film.S_SCENE && sh.t >= sg.t0 && sh.t < sg.t1) {
                    for (Film.Fx f : sg.fx) if (f.t1 >= sh.t - 1 && f.t0 <= sh.t + sh.dur + 1) System.out.println("  fx type=" + f.type + " " + f.t0 + "-" + f.t1);
                    for (Film.Cam c : sg.cams) if (c.t >= sh.t - 1 && c.t <= sh.t + sh.dur + 1) System.out.println("  cam t=" + c.t + " zoom=" + c.zoom + " ease=" + c.ease);
                }
                for (Film.Weather wt : film.weather) if (wt.t1 >= sh.t - 1 && wt.t0 <= sh.t + sh.dur + 1) System.out.println("  weather type=" + wt.type + " " + wt.t0 + "-" + wt.t1);
            }
        }
        assertTrue("checked " + r.checked, r.checked >= 30);
        assertTrue("feet " + r.feetChecked, r.feetChecked > 100);
        assertTrue("floating " + r.floating, r.floating == 0);
        assertTrue("still lively " + r.stillLively + " " + r.notes, r.stillLively == 0);
        assertTrue(r.text().contains("Step 6") && r.text().contains("Floating: 0 of"));
        // step 8: the meter fed frames in a row of one steady shot
        Film.Shot steady = null;
        for (Film.Shot sh : film.shots) { Film.Seg sg = film.segAt(sh.t + 0.01f); if (sg != null && sh.t > sg.t0 + 1.5f && sh.t + sh.dur < sg.t1 - 1.5f && sh.dur >= 1.5f) { steady = sh; break; } }
        assertNotNull(steady);
        FinalQc.Meter m = new FinalQc.Meter(film, w, h, 24);
        Renderer ren = new Renderer(film, art);
        int f0 = (int) Math.ceil((steady.t + 0.2f) * 24);
        for (int f = f0; f < f0 + 8; f++) {
            ren.render(g, f / 24f);
            bmp.getPixels(px, 0, w, 0, 0, w, h);
            m.frame(f, px);
        }
        g.release();
        bmp.recycle();
        assertTrue("meter pairs " + m.pairs, m.frames == 8 && m.pairs >= 5);
        assertTrue(m.report().contains("Step 8") && m.boilingShots() == 0 && m.shakingShots() == 0);
        // 8.3: the validator flags a lip-sync face smaller than its picture and frame allow, and the correction reframes it
        TechnicalDirector.Shot v = new TechnicalDirector.Shot();
        v.speech = true; v.closeUp = true; v.words = 4; v.face = 0.40f; v.faceWant = 0.72f;
        v.placement = "Foreground 0-1 m | Midground left third 2-4 m | Background 10-100 m";
        v.grounding = TechnicalDirector.GROUND;
        v.prompt = "CHARACTERS: PLACEMENT: ACTION: GROUNDING: LIGHTING: CAMERA: " + TechnicalDirector.STABLE;
        assertTrue(TechnicalDirector.validate(v).toString().contains("lip-sync face fills 40%"));
        assertTrue(TechnicalDirector.correct(v).isEmpty());
        assertTrue(v.fixes.toString().contains("framed on the face"));
        // a 9:16 frame cannot hold a 65% face inside its centre 60%: the most it allows is not a fault
        TechnicalDirector.Shot narrow = new TechnicalDirector.Shot();
        narrow.speech = true; narrow.closeUp = true; narrow.words = 3; narrow.face = 0.40f; narrow.faceWant = 0.40f;
        narrow.placement = v.placement; narrow.grounding = v.grounding; narrow.prompt = v.prompt;
        assertTrue(TechnicalDirector.validate(narrow).isEmpty());
        // place names are whole words: a "small living room" is a room, not a mall's basement; a "proof" is no roof
        assertTrue(com.tarun.kahani.core.Sets.detect("Grandpa's small living room at night, a candle on the table") == com.tarun.kahani.core.Sets.ROOM);
        assertTrue(com.tarun.kahani.core.Sets.detect("the dark basement of the closed mall") == com.tarun.kahani.core.Sets.BASEMENT);
        assertTrue(com.tarun.kahani.core.Sets.detect("the rooftop of Sky-Line Tower") == com.tarun.kahani.core.Sets.ROOFTOP);
    }

    /**
     * v17 — Studio 3D builds a character and a place on the phone (Android bitmaps, no service): the character comes
     * with its eye and mouth points, turns round for the master sheet and changes expression; the place comes with
     * its floor line; the pictures go into a story and are read back by the art loader with the face known exactly.
     * The handbook's helpers: the stimulus words, the shot ID, the lens, the scorecard. Every mesh is pixel level.
     */
    @Test
    public void studio3dBuildsCharactersAndPlacesAndTheHandbookIsWired() throws Exception {
        assertTrue(Rig.CELL_PX == 1f && Nature.CELL_PX == 1f);
        Project p = sampleProject();
        Story story = ScriptParser.parse(p.read("script.txt"));
        // a person, an animal, a bird and a robot in 3D
        int made = 0;
        for (Story.CharacterDef c : story.cast()) {
            Doll3D.Result r = Doll3D.make(c.look, 360, made);
            assertTrue(c.shown() + " size", r.w > 60 && r.h > 60);
            int opaque = 0;
            for (int i = 0; i < r.px.length; i += 7) if ((r.px[i] >>> 24) > 200) opaque++;
            assertTrue(c.shown() + " drawn", opaque > r.px.length / 7 / 8);
            assertTrue(c.shown() + " face points", r.mouthY > r.eyeLY && r.eyeLX < r.eyeRX && r.eyeR > 0.005f && r.mouthHW > 0.005f);
            if (++made >= 6) break;
        }
        Look bird = new Look(); bird.kind = Look.BIRD; bird.species = Look.SP_PEACOCK; bird.furColor = 0xFF1E88E5;
        assertTrue(Doll3D.make(bird, 300, 1).sideView);
        Look robot = new Look(); robot.kind = Look.ANIMAL; robot.species = Look.SP_DOG; robot.robot = true; robot.furColor = 0xFFB71C1C;
        assertTrue(Doll3D.make(robot, 300, 2).w > 100);
        // the master sheet and the expressions
        Story.CharacterDef hero = story.cast().get(0);
        Doll3D.Result sheet = Doll3D.masterSheet(hero.look, 240, 0);
        assertTrue("sheet " + sheet.w + "x" + sheet.h, sheet.w > sheet.h * 2);
        Doll3D.Result happy = Doll3D.make(hero.look, 300, 0, 0, Pose.HAPPY), back = Doll3D.make(hero.look, 300, 0, 180, Pose.NEUTRAL);
        assertTrue(happy.faceKnown && !back.faceKnown);
        // a place at night with its floor line
        Set3D.Result place = Set3D.make(Sets.GARDEN, Sets.NIGHT, 320, 180, 3);
        assertTrue(place.ground > 0.55f && place.ground < 0.9f);
        int distinct = 0, last = 0;
        for (int i = 0; i < place.px.length; i += 53) if (place.px[i] != last) { distinct++; last = place.px[i]; }
        assertTrue("place drawn", distinct > 60);
        // saved into a story through the phone's picture path, the face points travel with the picture
        Story.CharacterDef c0 = story.cast().get(0);
        String file = (String) s3d("makeCharacter", p, story, c0, null, RuntimeEnvironment.getApplication(), null, false, null, false);
        assertTrue(p.has(file));
        String line = p.manifestLine("char", c0.displayName);
        assertNotNull(line);
        assertTrue(line, line.split("\\|").length >= 11);
        Art art = Art.fromManifest(p.read("cast.txt"), story, p.loader());
        Art.Sprite sp = art.sprites.get(c0.id);
        assertNotNull(sp);
        assertTrue("face known from the 3D picture", sp.faceKnown);
        // the handbook's helpers
        assertTrue(Handbook.stimulus("अचानक एक आवाज़ आई।") && Handbook.stimulus("She hears a knock.") && !Handbook.stimulus("वह बगीचे में चलती है।"));
        assertTrue(Handbook.shotId(4, 7, 3).equals("KAHANI_SC04_SH007_V003"));
        assertTrue(Handbook.focalFor(com.tarun.kahani.core.ShotPlanner.XWIDE, "16:9").equals("28mm") && Handbook.focalFor(com.tarun.kahani.core.ShotPlanner.CU, "16:9").equals("85mm"));
        Director d = new Director(story, new Director.Options());
        Film film = d.prepare();
        film = d.direct(art);
        assertNotNull(film.stats);
        assertTrue(film.shotList.contains("SHOT ID: KAHANI_SC") && film.shotList.contains("FIVE QUESTIONS") && film.shotList.contains("CONTINUITY LEDGER"));
        Handbook.Card card = Handbook.score(film, film.stats, 0, 0, 0);
        assertTrue(card.text().contains("Gate 1") && card.score[7] == 5 && card.gate[2]);
        assertTrue(Handbook.delivery(card, film.stats).contains("FINAL DELIVERY CHECKLIST"));
        // the handbook text and its summary are in the app
        assertTrue(Handbook.SUMMARY.contains("WHERE THE HANDBOOK AND THE PROTOCOLS DISAGREE"));
        assertTrue(new String(Files.readAllBytes(new File(ASSETS, "ai_animation_director_handbook.md").toPath()), "UTF-8").contains("Four approval gates"));
    }

    /** The platform's mesh drawing with more vertices than a 16-bit index can count: what Skia does with it. */
    @Test
    public void bigMeshDrawsThroughTheAndroidCanvas() throws Exception {
        // Canvas.drawBitmapMesh indexes vertices with 16 bits: past 65536 vertices a single draw leaves the far rows
        // undrawn, so AndroidGfx draws a fine mesh in bands — every size must come out complete
        for (int cells : new int[]{100, 255, 256, 300, 500, 1024}) {
            Bitmap src = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888);
            src.eraseColor(0xFFFF0000);
            Bitmap dst = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888);
            dst.eraseColor(0xFF0000FF);
            AndroidGfx g = new AndroidGfx(dst, 4);
            float[] v = new float[(cells + 1) * (cells + 1) * 2];
            int k = 0;
            for (int j = 0; j <= cells; j++) for (int i = 0; i <= cells; i++) { v[k++] = 200f * i / cells; v[k++] = 200f * j / cells; }
            g.imageMesh(src, cells, cells, v);
            int blue = 0;
            StringBuilder where = new StringBuilder();
            for (int y = 0; y < 200; y += 3) for (int x = 0; x < 200; x += 3) if ((dst.getPixel(x, y) & 0xFFFFFF) != 0xFF0000) { blue++; if (where.length() < 200) where.append(x).append(',').append(y).append(' '); }
            System.out.println("mesh " + cells + "x" + cells + " (" + ((cells + 1) * (cells + 1)) + " vertices): " + blue + " undrawn sample points " + where);
            g.release();
            assertTrue("mesh of " + cells + " cells did not draw fully", blue == 0);
        }
        // the banded drawing gives the same picture as a single draw: a wavy mesh over a detailed picture, drawn
        // once directly (under the limit) and once in many bands (the limit lowered), opaque and translucent
        int W = 240, Hh = 240, cells = 200;
        Bitmap src = Bitmap.createBitmap(300, 220, Bitmap.Config.ARGB_8888);
        int[] sp = new int[300 * 220];
        for (int y = 0; y < 220; y++) for (int x = 0; x < 300; x++) {
            int r = (x * 255 / 299), gg = (y * 255 / 219), b = ((x / 7 + y / 5) & 1) == 0 ? 40 : 220;
            int a = (x > 20 && x < 280) ? 255 : 90;        // soft edges like a cut-out character
            sp[y * 300 + x] = (a << 24) | (r << 16) | (gg << 8) | b;
        }
        src.setPixels(sp, 0, 300, 0, 0, 300, 220);
        float[] v = new float[(cells + 1) * (cells + 1) * 2];
        int k = 0;
        for (int j = 0; j <= cells; j++) for (int i = 0; i <= cells; i++) {
            v[k++] = 10 + 220f * i / cells + (float) Math.sin(j * 0.11) * 6;
            v[k++] = 10 + 220f * j / cells + (float) Math.cos(i * 0.09) * 5;
        }
        for (float alpha : new float[]{1f, 0.6f}) {
            int[] one = null;
            int worst = 0, worstAt = -1;
            for (int pass = 0; pass < 2; pass++) {
                Bitmap dst = Bitmap.createBitmap(W, Hh, Bitmap.Config.ARGB_8888);
                dst.eraseColor(0xFF203040);
                AndroidGfx g = new AndroidGfx(dst, 4);
                int saved = AndroidGfx.MESH_VERTS_PER_DRAW;
                AndroidGfx.MESH_VERTS_PER_DRAW = pass == 0 ? 1 << 20 : 4000;   // one draw, then about ten bands
                try { g.save(); g.setAlpha(alpha); g.imageMesh(src, cells, cells, v); g.restore(); }
                finally { AndroidGfx.MESH_VERTS_PER_DRAW = saved; }
                int[] px = new int[W * Hh];
                dst.getPixels(px, 0, W, 0, 0, W, Hh);
                g.release();
                if (pass == 0) { one = px; continue; }
                for (int i = 0; i < px.length; i++) {
                    int a = one[i], b = px[i];
                    int d = Math.max(Math.abs(((a >> 16) & 255) - ((b >> 16) & 255)), Math.max(Math.abs(((a >> 8) & 255) - ((b >> 8) & 255)), Math.abs((a & 255) - (b & 255))));
                    if (d > worst) { worst = d; worstAt = i; }
                }
            }
            System.out.println("banded vs single draw at alpha " + alpha + ": worst channel difference " + worst + " at " + (worstAt % W) + "," + (worstAt / W));
            assertTrue("the banded mesh differs from a single draw by " + worst + " at alpha " + alpha, worst <= 2);
        }
    }

    /** Calls a package-private Studio3DArt helper by reflection (the test lives in another package): by name and number of arguments. */
    private static Object s3d(String method, Object... args) throws Exception {
        return call("com.tarun.kahani.app.Studio3DArt", method, args);
    }

    private static Object call(String cls, String method, Object... args) throws Exception {
        Class<?> k = Class.forName(cls);
        for (Method m : k.getDeclaredMethods()) {
            if (!m.getName().equals(method) || m.getParameterTypes().length != args.length) continue;
            m.setAccessible(true);
            try { return m.invoke(null, args); }
            catch (java.lang.reflect.InvocationTargetException e) { throw e.getCause() instanceof Exception ? (Exception) e.getCause() : e; }
        }
        throw new NoSuchMethodException(cls + "." + method + "/" + args.length);
    }

    /**
     * v18: the views of a character made from its own picture (three-quarter, side, back) — the back of the head
     * shows hair, not the face; the proposals wait for the user's decision and a rejected one is deleted; the
     * dolls of characters without a picture are proposed with their views; the director plans over-the-shoulder
     * reverses once the back view exists and the renderer draws them; a GLB model is read and rendered; the
     * library keeps every picture in the app's own folder (tarunkahani), never the photos library; the scene maker's rubric scores every proposal.
     */
    @Test
    public void viewsFromThePictureProposalsAlbumAndOverTheShoulder() throws Exception {
        Project p = sampleProject();
        Story story = ScriptParser.parse(p.read("script.txt"));
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        Story.CharacterDef vanusha = null;
        for (Story.CharacterDef c : story.cast()) if (c.displayName.contains("वानुषा")) vanusha = c;
        assertNotNull(vanusha);
        String key = (String) s3d("keyFor", p, story, vanusha);
        String front = (String) s3d("charFile", p, story, vanusha);
        assertNotNull(front);
        // ---- the sample gives her a real back view (the user's own picture): it is the back, and stays the back
        String[] own = (String[]) s3d("viewFiles", p, key);
        assertTrue("the real back view of the sample", own[2] != null && own[2].contains("_back") && own[0] == null && own[1] == null);
        // ---- the other views, as proposals (the director asks): only the three-quarter and the side are made
        long t0 = System.currentTimeMillis();
        int made = (Integer) s3d("makeViews", p, story, vanusha, null, ctx, null, true, "", null, null, false);
        System.out.println("views of " + vanusha.shown() + " made in " + (System.currentTimeMillis() - t0) + " ms");
        assertTrue("two views proposed beside the real back: " + made, made == 2);
        java.util.List<String[]> props = (java.util.List<String[]>) s3d("viewProposals", p, key);
        assertTrue(props.size() == 2);
        for (String[] f : props) assertTrue("scored: " + f[f.length - 1], f[f.length - 1].startsWith("Score"));
        String[] before = (String[]) s3d("viewFiles", p, key);
        assertTrue("nothing used before the decision", before[0] == null && before[1] == null && own[2].equals(before[2]));
        // the back view: as tall as the picture, its head of hair (no skin where the face would be)
        String backFile = own[2], sideFile = null;
        for (String[] f : props) { assertTrue("the real back is never replaced", !f[3].trim().equals("180")); if (f[3].trim().equals("-90")) sideFile = f[4]; }
        assertNotNull(sideFile);
        Bitmap back = android.graphics.BitmapFactory.decodeFile(p.file(backFile).getAbsolutePath());
        Bitmap side = android.graphics.BitmapFactory.decodeFile(p.file(sideFile).getAbsolutePath());
        Bitmap fr = android.graphics.BitmapFactory.decodeFile(p.file(front).getAbsolutePath());
        assertTrue("back view tall", back.getHeight() > 900);
        assertTrue("side view narrower than the front", side.getWidth() / (float) side.getHeight() < fr.getWidth() / (float) fr.getHeight());
        int skin = 0, n = 0;
        for (int y = (int) (back.getHeight() * 0.08f); y < back.getHeight() * 0.16f; y += 3) for (int x = (int) (back.getWidth() * 0.4f); x < back.getWidth() * 0.6f; x += 3) {
            int c = back.getPixel(x, y);
            if ((c >>> 24) < 200) continue;
            n++;
            if (com.tarun.kahani.core.Cutout.isSkin(c)) skin++;
        }
        assertTrue("the back of the head is hair, not a face (" + skin + "/" + n + ")", n > 20 && skin < n * 0.15f);
        // ---- accept the views: manifest lines, the sprite's views with their own rigs
        for (String[] f : props) s3d("accept", p, null, ctx, f);
        assertTrue((Boolean) s3d("hasViews", p, key));
        assertTrue(((java.util.List<?>) s3d("viewProposals", p, key)).isEmpty());
        Art art = Art.fromManifest(p.read("cast.txt"), story, p.loader());
        Art.Sprite sp = art.sprites.get(vanusha.id);
        assertNotNull(sp.views);
        assertTrue("three views loaded", sp.view(0) != null && sp.view(1) != null && sp.view(2) != null);
        assertTrue("the side view has a rig", sp.view(1).rig != null);
        assertTrue("the three-quarter view keeps the face points", sp.view(0).faceKnown);
        // ---- the dolls of the characters without a picture (the monster, the witch) are proposed with their views
        int dolls = (Integer) s3d("makeMissing", p, story, new com.tarun.kahani.core.Edits(), null, ctx, null, true, null, null, false);
        java.util.List<String[]> all = (java.util.List<String[]>) s3d("proposals", p);
        int chars = 0;
        String[] rejectMe = null;
        for (String[] f : all) if (f[1].equals("char")) { chars++; if (rejectMe == null) rejectMe = f; }
        assertTrue("a doll proposed for the monster (the only one without a picture): " + dolls + "/" + chars, dolls >= 1 && chars >= 1);
        String rejectedFile = rejectMe[3];
        s3d("reject", p, rejectMe);
        assertTrue("a rejected proposal is deleted", !p.has(rejectedFile));
        assertTrue("remembered as rejected", (Boolean) s3d("rejected", p, "char", rejectMe[2]));
        assertTrue("no picture after a rejection", p.manifestLine("char", rejectMe[2]) == null);
        s3d("decideAll", p, null, ctx, true);
        assertTrue(((java.util.List<?>) s3d("proposals", p)).isEmpty());
        // ---- the director: an over-the-shoulder reverse once the back view exists, drawn by the renderer
        art = Art.fromManifest(p.read("cast.txt"), story, p.loader());
        Director d = new Director(story, new Director.Options());
        Film film = d.prepare();
        film = d.direct(art);
        Film.Shot ots = null;
        for (Film.Shot sh : film.shots) if (sh.ots.length() > 0 && sh.type == com.tarun.kahani.core.ShotPlanner.OTS) { ots = sh; break; }
        assertNotNull("an over-the-shoulder reverse was planned", ots);
        assertTrue(film.shotList.contains("OVER THE SHOULDER") && film.shotList.contains("PICTURES USED"));
        Bitmap bmp = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(bmp, 4);
        new Renderer(film, art).render(g, ots.t + 0.1f);
        int[] px = new int[640 * 360];
        bmp.getPixels(px, 0, 640, 0, 0, 640, 360);
        int distinct = 0, last = 0;
        for (int i = 0; i < px.length; i += 331) if (px[i] != last) { distinct++; last = px[i]; }
        assertTrue("the reverse shot is drawn", distinct > 50);
        OUT.mkdirs();
        FileOutputStream o = new FileOutputStream(new File(OUT, "ots.png"));
        bmp.compress(Bitmap.CompressFormat.PNG, 100, o);
        o.close();
        g.release();
        // ---- the scene maker's rubric and the character bible
        int[] ratings = com.tarun.kahani.core.SceneMaker.ratings(true, true, true, true, 1f, true);
        int sc = com.tarun.kahani.core.SceneMaker.score(ratings);
        assertTrue("a view from the picture scores for approval: " + sc, sc >= 85 && com.tarun.kahani.core.SceneMaker.status(sc, "").equals("review for approval"));
        assertTrue(com.tarun.kahani.core.SceneMaker.score(com.tarun.kahani.core.SceneMaker.ratings(false, false, false, false, 0f, false)) < 70);
        assertTrue(com.tarun.kahani.core.SceneMaker.status(95, "the face differs").startsWith("rejected"));
        String bibles = (String) s3d("bibles", p, story);
        assertTrue(bibles.contains("CHARACTER BIBLE CHAR_001") && bibles.contains("views accepted"));
        // ---- a GLB model (a textured cube written here) is read and rendered from the side
        byte[] glb = tinyGlb();
        com.tarun.kahani.core.Glb.Model model = com.tarun.kahani.core.Glb.load(glb, new com.tarun.kahani.core.Glb.ImageDecoder() {
            public int[] decode(byte[] bytes) {
                Bitmap b = android.graphics.BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
                int[] out = new int[b.getWidth() * b.getHeight() + 2];
                out[0] = b.getWidth(); out[1] = b.getHeight();
                b.getPixels(out, 2, b.getWidth(), 0, 0, b.getWidth(), b.getHeight());
                return out;
            }
        });
        assertTrue(model.note, model.triangles == 12 && model.pictures == 1);
        Doll3D.Result cube = com.tarun.kahani.core.Glb.render(model, 200, -45);
        int opaque = 0;
        for (int i = 0; i < cube.px.length; i++) if ((cube.px[i] >>> 24) > 200) opaque++;
        assertTrue("the cube is drawn", opaque > cube.px.length / 6);
        java.util.Map<String, Object> body = com.tarun.kahani.core.ImageTo3D.meshyBody(new byte[]{1, 2, 3}, "a king");
        assertTrue(((String) body.get("image_url")).startsWith("data:image/png;base64,AQID") && ((String) body.get("texture_prompt")).contains("a king"));
        // ---- the library: a picture added to the app's own library lands in the app's private folder, nowhere else
        com.tarun.kahani.app.Library lib = com.tarun.kahani.app.Library.get(ctx);
        com.tarun.kahani.app.Library.Item it = lib.addBytes(com.tarun.kahani.app.Library.PIC, "person", "test", "", Files.readAllBytes(p.file(front).toPath()), ".jpg", "test");
        assertNotNull(it);
        assertTrue("kept in the app's own library folder: " + it.path, it.path.startsWith(ctx.getFilesDir().getAbsolutePath()));
        assertTrue("never offered to the photos library", it.meta("album") == null || it.meta("album").length() == 0);
    }

    /** A GLB with one textured cube: 8 vertices, 12 triangles, a 2x2 PNG. */
    @Test
    public void freeGithubSourcesGuidesAndDescriptionDetails() throws Exception {
        // ---- the free model catalogue: words of a description choose a model, the kind keeps it honest, props follow the words
        com.tarun.kahani.core.Look man = new com.tarun.kahani.core.Look();
        man.kind = com.tarun.kahani.core.Look.MAN;
        java.util.List<com.tarun.kahani.core.FreeModels.Entry> knights = com.tarun.kahani.core.FreeModels.matches("रत्नगढ़ का बहादुर सैनिक, हाथ में तलवार और ढाल", man);
        assertTrue("a soldier gets the knight", !knights.isEmpty() && knights.get(0).name.startsWith("Knight"));
        assertTrue("CC0", knights.get(0).free() && knights.get(0).url.startsWith("https://raw.githubusercontent.com/"));
        String[] props = com.tarun.kahani.core.FreeModels.propsFor(knights.get(0), man, "हाथ में तलवार और ढाल");
        boolean sword = false, shield = false;
        for (String pr : props) { if (pr.toLowerCase().contains("sword")) sword = true; if (pr.toLowerCase().contains("shield")) shield = true; }
        assertTrue("the sword and a shield are kept: " + java.util.Arrays.toString(props), sword && shield);
        com.tarun.kahani.core.Look girl = new com.tarun.kahani.core.Look();
        girl.kind = com.tarun.kahani.core.Look.GIRL;
        assertTrue("a girl described as a knight stays a girl", com.tarun.kahani.core.FreeModels.matches("knight", girl).isEmpty());
        assertTrue("nothing without a fitting word", com.tarun.kahani.core.FreeModels.matches("एक दयालु दादी", man).isEmpty());
        for (com.tarun.kahani.core.FreeModels.Entry e : com.tarun.kahani.core.FreeModels.ALL) assertTrue(e.name + " has a licence", e.licence.length() > 3 && e.credit.length() > 5);
        assertTrue(com.tarun.kahani.core.FreeModels.SOURCES.contains("CC0") && com.tarun.kahani.core.FreeModels.SOURCES.contains("TripoSR"));
        // ---- the free image-to-3D demos: the request is built from the demo's own description, the reply read from its event stream (no network)
        String info = "{\"named_endpoints\":{\"/generate\":{\"parameters\":[{\"parameter_name\":\"mc_resolution\",\"component\":\"Slider\",\"parameter_default\":256},"
                + "{\"parameter_name\":\"image\",\"component\":\"Image\",\"python_type\":{\"type\":\"filepath\"}},{\"parameter_name\":\"caption\",\"component\":\"Textbox\",\"parameter_default\":\"\"}]}}}";
        java.util.List<Object> data = com.tarun.kahani.core.ImageTo3D.stepInputs(com.tarun.kahani.core.Json.parseLoose(info), "generate",
                fileData("/tmp/gradio/x/picture.png"), "a brave knight");
        assertTrue("three inputs in the demo's order", data.size() == 3);
        assertTrue("the default is kept", ((Number) data.get(0)).intValue() == 256);
        assertTrue("the picture goes to the image input", data.get(1) instanceof java.util.Map);
        assertTrue("the description goes to the caption", String.valueOf(data.get(2)).contains("knight"));
        java.util.List<Object> unknown = com.tarun.kahani.core.ImageTo3D.stepInputs(null, "preprocess", fileData("p"), "x");
        assertTrue("without a description of the step the picture alone is sent", unknown.size() == 1);
        java.util.List<Object> done = com.tarun.kahani.core.ImageTo3D.complete("event: heartbeat\ndata: null\n\nevent: complete\ndata: [{\"path\":\"/tmp/gradio/a/model.obj\"},{\"path\":\"/tmp/gradio/a/model.glb\",\"url\":\"https://x.hf.space/gradio_api/file=/tmp/gradio/a/model.glb\"}]\n\n", "generate");
        assertTrue("the GLB is found among the outputs", com.tarun.kahani.core.ImageTo3D.glbUrl(done, "https://x.hf.space").endsWith("model.glb"));
        boolean failed = false;
        try { com.tarun.kahani.core.ImageTo3D.complete("event: error\ndata: \"GPU quota exceeded\"\n", "generate"); } catch (java.io.IOException e) { failed = e.getMessage().contains("quota"); }
        assertTrue("an error event is a failure", failed);
        assertTrue(com.tarun.kahani.core.ImageTo3D.SPACES.length >= 3 && com.tarun.kahani.core.ImageTo3D.SPACES[0].base.endsWith(".hf.space"));
        // ---- the glTF reader poses a rigged model from its idle animation and keeps a prop only when asked for
        com.tarun.kahani.core.Glb.Model plainModel = com.tarun.kahani.core.Glb.load(tinyGlb(), null);
        assertTrue(plainModel.triangles == 12 && plainModel.joints == 0);
        // ---- the training documents are bundled
        for (String a : new String[]{"image_to_3d_plain_guide.md", "director_training_guide.md", "ai_3d_scene_maker_guide.md", "ai_animation_director_handbook.md"}) {
            String text = new String(Files.readAllBytes(new File(ASSETS, a).toPath()), "UTF-8");
            assertTrue(a + " is bundled", text.length() > 1000);
        }
        assertTrue("the recorded guide maps its rules to the code", new String(Files.readAllBytes(new File(ASSETS, "image_to_3d_plain_guide.md").toPath()), "UTF-8").contains("Figure3D.details"));
        // ---- the description's details on the back of a figure made from a picture: a braid adds geometry, a plain look does not
        Project p = sampleProject();
        Story story = ScriptParser.parse(p.read("script.txt"));
        Story.CharacterDef vanusha = null;
        for (Story.CharacterDef c : story.cast()) if (c.displayName.contains("वानुषा")) vanusha = c;
        String front = (String) s3d("charFile", p, story, vanusha);
        int[] d = p.loader().decode(front, 900);
        int[] px = new int[d[0] * d[1]];
        System.arraycopy(d, 2, px, 0, px.length);
        com.tarun.kahani.core.Cutout.Result cut = com.tarun.kahani.core.Cutout.process(px, d[0], d[1], false);
        com.tarun.kahani.core.Look plain = new com.tarun.kahani.core.Look();
        plain.kind = com.tarun.kahani.core.Look.GIRL; plain.female = true; plain.hair = com.tarun.kahani.core.Look.H_SHORT;
        com.tarun.kahani.core.Look braided = new com.tarun.kahani.core.Look();
        braided.kind = com.tarun.kahani.core.Look.GIRL; braided.female = true; braided.hair = com.tarun.kahani.core.Look.H_BRAID; braided.ribbon1 = 0xFFC62828; braided.wings = true;
        System.setProperty("kahani.force3d", "1");
        try {
            com.tarun.kahani.core.Figure3D.Model a = com.tarun.kahani.core.Figure3D.build(cut.px, cut.w, cut.h, null, plain);
            com.tarun.kahani.core.Figure3D.Model b = com.tarun.kahani.core.Figure3D.build(cut.px, cut.w, cut.h, null, braided);
            com.tarun.kahani.core.Studio3D.Scene sa = new com.tarun.kahani.core.Studio3D.Scene(), sb = new com.tarun.kahani.core.Studio3D.Scene();
            call("com.tarun.kahani.core.Figure3D", "mesh", sa, a, 0f);
            call("com.tarun.kahani.core.Figure3D", "mesh", sb, b, 0f);
            int ta = triangles(sa.mesh), tb = triangles(sb.mesh);
            assertTrue("the braid, its ribbon and the wings add geometry behind the figure: " + ta + " -> " + tb, tb > ta + 200);
            assertTrue("the hair colour is read from the picture", b.hairColor != 0);
        } finally {
            System.clearProperty("kahani.force3d");
        }
    }

    private static java.util.Map<String, Object> fileData(String path) {
        java.util.Map<String, Object> m = new java.util.HashMap<String, Object>();
        m.put("path", path);
        return m;
    }

    private static int triangles(Object mesh) throws Exception {
        java.lang.reflect.Field f = mesh.getClass().getDeclaredField("nt");
        f.setAccessible(true);
        return f.getInt(mesh);
    }

    private static byte[] tinyGlb() throws Exception {
        float[] pos = {-1,0,-1, 1,0,-1, 1,2,-1, -1,2,-1, -1,0,1, 1,0,1, 1,2,1, -1,2,1};
        float[] uv = {0,0, 1,0, 1,1, 0,1, 0,0, 1,0, 1,1, 0,1};
        short[] idx = {0,2,1, 0,3,2, 4,5,6, 4,6,7, 0,1,5, 0,5,4, 1,2,6, 1,6,5, 2,3,7, 2,7,6, 3,0,4, 3,4,7};
        Bitmap tex = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        tex.setPixel(0, 0, 0xFFFF0000); tex.setPixel(1, 0, 0xFF00FF00); tex.setPixel(0, 1, 0xFF0000FF); tex.setPixel(1, 1, 0xFFFFFF00);
        java.io.ByteArrayOutputStream po = new java.io.ByteArrayOutputStream();
        tex.compress(Bitmap.CompressFormat.PNG, 100, po);
        byte[] png = po.toByteArray();
        java.nio.ByteBuffer bin = java.nio.ByteBuffer.allocate(pos.length * 4 + uv.length * 4 + idx.length * 2 + 4 + png.length + 8).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        for (float f : pos) bin.putFloat(f);
        int uvOff = bin.position();
        for (float f : uv) bin.putFloat(f);
        int idxOff = bin.position();
        for (short i : idx) bin.putShort(i);
        while (bin.position() % 4 != 0) bin.put((byte) 0);
        int pngOff = bin.position();
        bin.put(png);
        while (bin.position() % 4 != 0) bin.put((byte) 0);
        byte[] binBytes = java.util.Arrays.copyOf(bin.array(), bin.position());
        String json = "{\"asset\":{\"version\":\"2.0\"},\"scene\":0,\"scenes\":[{\"nodes\":[0]}],\"nodes\":[{\"mesh\":0}],"
                + "\"meshes\":[{\"primitives\":[{\"attributes\":{\"POSITION\":0,\"TEXCOORD_0\":1},\"indices\":2,\"material\":0}]}],"
                + "\"materials\":[{\"pbrMetallicRoughness\":{\"baseColorTexture\":{\"index\":0},\"metallicFactor\":0}}],"
                + "\"textures\":[{\"source\":0}],\"images\":[{\"bufferView\":3,\"mimeType\":\"image/png\"}],"
                + "\"accessors\":[{\"bufferView\":0,\"componentType\":5126,\"count\":8,\"type\":\"VEC3\"},{\"bufferView\":1,\"componentType\":5126,\"count\":8,\"type\":\"VEC2\"},"
                + "{\"bufferView\":2,\"componentType\":5123,\"count\":36,\"type\":\"SCALAR\"}],"
                + "\"bufferViews\":[{\"buffer\":0,\"byteOffset\":0,\"byteLength\":" + (pos.length * 4) + "},{\"buffer\":0,\"byteOffset\":" + uvOff + ",\"byteLength\":" + (uv.length * 4) + "},"
                + "{\"buffer\":0,\"byteOffset\":" + idxOff + ",\"byteLength\":" + (idx.length * 2) + "},{\"buffer\":0,\"byteOffset\":" + pngOff + ",\"byteLength\":" + png.length + "}],"
                + "\"buffers\":[{\"byteLength\":" + binBytes.length + "}]}";
        byte[] jb = json.getBytes("UTF-8");
        int jpad = (4 - jb.length % 4) % 4;
        java.nio.ByteBuffer out = java.nio.ByteBuffer.allocate(12 + 8 + jb.length + jpad + 8 + binBytes.length).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        out.putInt(0x46546C67).putInt(2).putInt(out.capacity());
        out.putInt(jb.length + jpad).putInt(0x4E4F534A).put(jb);
        for (int i = 0; i < jpad; i++) out.put((byte) 0x20);
        out.putInt(binBytes.length).putInt(0x004E4942).put(binBytes);
        return out.array();
    }

    /**
     * Pixar-Lead v4.0: the five delivery formats keep their exact shape, the hardcoded safe zones and scale lock are
     * there, the command box understands the new formats, the story spine and the Braintrust are in every film and
     * the descriptions package, and the thumbnail and poster are made natively in their own formats.
     */
    /**
     * The AI Director's Production Manual (v2.0), trained and hardcoded — its own final acceptance test: a short story
     * with two recurring characters, two locations, dialogue, physical interaction, a loud sound, a look and an
     * emotional change; the records it asks for; the facial identity specification and the inventory of the sample.
     */
    @Test
    public void directorsManualIsTrainedRecordsAndAnimatic() throws Exception {
        String manual = new String(Files.readAllBytes(new File(ASSETS, "ai_film_maker_directors_manual.md").toPath()), "UTF-8");
        assertTrue(manual.contains("Gate 5") && manual.contains("Final acceptance test") && manual.contains("Step 2.3"));
        assertTrue(com.tarun.kahani.core.DirectorsManual.SUMMARY.contains("Gate 5 — Film") && com.tarun.kahani.core.DirectorsManual.ENFORCEMENT.length >= 25
                && com.tarun.kahani.core.DirectorsManual.PRECEDENCE.length >= 6 && com.tarun.kahani.core.DirectorsManual.QA.length == 11);
        // 2.6: suspicion and relief read from the manners
        assertTrue(Director.emotionOf("शक से", "", null) == Pose.SUSPICIOUS);
        assertTrue(Director.emotionOf("राहत की साँस लेकर", "", null) == Pose.RELIEVED);
        assertTrue(Director.emotionOf("relieved", "", null) == Pose.RELIEVED);
        assertTrue(com.tarun.kahani.core.DirectorsManual.loud("अचानक ज़ोर का धमाका होता है।") && !com.tarun.kahani.core.DirectorsManual.loud("मीना मुस्कुराती है।"));
        assertTrue(com.tarun.kahani.core.DirectorsManual.looksAt("मीना दरवाज़े की ओर देखती है।") && !com.tarun.kahani.core.DirectorsManual.looksAt("मीना दौड़ती है।"));
        // the acceptance story (Part VII)
        File f = new File(ASSETS, "../../../../tools/testdata/manual_test.txt");
        Story story = ScriptParser.parse(new String(Files.readAllBytes(f.toPath()), "UTF-8"));
        assertTrue("cast " + story.cast().size() + " scenes " + story.scenes.size(), story.cast().size() == 2 && story.scenes.size() == 2);
        Director.Options opt = new Director.Options();
        opt.aspect = "16:9";
        Director d = new Director(story, opt);
        d.prepare();
        Film film = d.direct(new Art());
        String q = film.shotList;
        int pov = 0, loud = 0, estab = 0;
        for (Film.Shot sh : film.shots) {
            assertTrue("shot over 4 s: " + sh.dur, sh.dur <= 4.05f);
            if (sh.type == com.tarun.kahani.core.ShotPlanner.POV) pov++;
            if (sh.purpose.startsWith("Reaction to the sound")) loud++;
            if (sh.stage == com.tarun.kahani.core.ShotPlanner.ESTABLISH && sh.purpose.startsWith("Establish")) estab++;
        }
        assertTrue("a point-of-view shot for the look: " + pov, pov >= 1);
        assertTrue("a reaction to the loud sound: " + loud, loud >= 1);
        assertTrue("an establishing shot per place: " + estab, estab >= 2);
        for (String must : new String[]{"ASSETS: CHAR_001", "LOC_001", "LOC_002", "TRANSITION IN: establishing cut", "TRANSITION IN: reaction cut", "STATE AT START:", "STATE AT END:",
                "VOICE / MUSIC: VOICE_001", "NARRATIVE BEAT SHEET", "SCENE RECORDS", "SC_01", "SC_02", "SCENE-COVERAGE REPORT", "PROP LEDGER", "LOCATION RECORDS",
                "One brow down", "Tension gone", "Director's manual (v2.0)", "Point of view: what मीना sees", "CONTINUITY LEDGER"})
            assertTrue("the shot list lacks: " + must, q.contains(must));
        // 2.3 and 1.1: the facial identity specification and the inventory of the sample cast (real pictures, real back views)
        Project p = sampleProject();
        Story sample = ScriptParser.parse(p.read("script.txt"));
        String specs = (String) s3d("facialSpecs", p, sample, true);
        for (String must : new String[]{"FACIAL IDENTITY SPECIFICATION", "Character ID: CHAR_001", "read from the picture", "IDENTITY CONSTRAINTS", "Uncertain features", "Human review status"})
            assertTrue("specs lack: " + must, specs.contains(must));
        @SuppressWarnings("unchecked")
        List<String[]> inv = (List<String[]>) s3d("inventory", p, sample, null);
        int chars = 0, views = 0;
        for (String[] r : inv) {
            if (r[1].equals("character")) { chars++; assertTrue(r[0] + " " + r[4], !r[4].equals("MISSING FILE")); }
            if (r[1].equals("view")) { views++; assertTrue(r[0] + " " + r[4], r[4].equals("inspected")); }
        }
        assertTrue("characters " + chars + " views " + views, chars == sample.cast().size() && views >= 11);
        String invText = com.tarun.kahani.core.DirectorsManual.inventory(inv);
        assertTrue(invText.contains("PROJECT ASSET INVENTORY") && invText.contains("CHAR_001_V2") && invText.contains("VOICE_001"));
        System.out.println("MANUAL: shots " + film.shots.size() + " pov " + pov + " loud " + loud + " establishing " + estab + "; inventory rows " + inv.size());
    }

    /**
     * The Phone-Local AI 3D Animated Film Creator guide (v21): bundled and hardcoded; the angle of real fronts and
     * backs read from the face; a sheet of two figures split; the establishing bridge; the reverse angle behind the
     * reverse shots; the dip between scenes (never two scenes over each other); the reference-conditioned doll look.
     */
    @Test
    public void phoneGuideAnglesSplitBridgeAndReverse() throws Exception {
        String guide = new String(Files.readAllBytes(new File(ASSETS, "phone_local_film_creator_guide.md").toPath()), "UTF-8");
        assertTrue(guide.contains("Appendix C") && guide.contains("Pixar-level"));
        assertTrue(com.tarun.kahani.core.PhoneGuide.SUMMARY.contains("Offline core") && com.tarun.kahani.core.PhoneGuide.PRECEDENCE.length >= 6
                && com.tarun.kahani.core.PhoneGuide.ENFORCEMENT.length >= 15 && com.tarun.kahani.core.PhoneGuide.FAILURES.length == 10);
        // the angle of real pictures, read from the face
        String[][] pics = {{"char_vrinda.jpg", "front"}, {"char_vrinda_back.jpg", "back"}, {"char_raju.jpg", "front"}, {"char_raju_back.jpg", "back"}, {"char_kripa.jpg", "front"}, {"char_kripa_back.jpg", "back"}};
        int[][] px = new int[2][];
        int[] ws = new int[2], hs = new int[2];
        for (int i = 0; i < pics.length; i++) {
            Bitmap b = android.graphics.BitmapFactory.decodeFile(new File(ASSETS, "sample/" + pics[i][0]).getAbsolutePath());
            assertNotNull(pics[i][0], b);
            int w = b.getWidth(), h = b.getHeight();
            int[] p = new int[w * h];
            b.getPixels(p, 0, w, 0, 0, w, h);
            com.tarun.kahani.core.Cutout.Result r = com.tarun.kahani.core.Cutout.process(p.clone(), w, h, false);
            String got = com.tarun.kahani.core.Angles.name(com.tarun.kahani.core.Angles.guess(r));
            assertTrue(pics[i][0] + " read as " + got, got.equals(pics[i][1]));
            if (i < 2) { px[i] = p; ws[i] = w; hs[i] = h; }
        }
        // a sheet of two figures side by side becomes two pictures, left to right
        int sh = Math.max(hs[0], hs[1]), sw = ws[0] + ws[1] + 40;
        int[] sheet = new int[sw * sh];
        java.util.Arrays.fill(sheet, 0xFFFFFFFF);
        for (int k = 0; k < 2; k++) for (int y = 0; y < hs[k]; y++) for (int x = 0; x < ws[k]; x++) {
            int c = px[k][y * ws[k] + x];
            if ((c >>> 24) > 100) sheet[y * sw + x + (k == 0 ? 0 : ws[0] + 40)] = c | 0xFF000000;
        }
        List<com.tarun.kahani.core.Angles.Piece> pieces = com.tarun.kahani.core.Angles.split(sheet, sw, sh);
        assertTrue("pieces " + pieces.size(), pieces.size() == 2 && pieces.get(0).x0 < pieces.get(1).x0 && pieces.get(1).w > 100);
        float[] assigned = com.tarun.kahani.core.Angles.assign(new float[]{0, 0, 180, 0, 0});
        assertTrue(assigned[0] == 0 && assigned[1] == -45 && assigned[2] == 180 && assigned[3] == -90 && Float.isNaN(assigned[4]));
        // the sample with a reverse angle of scene 1: bridges, the reverse backdrop and reverse cams, the dip
        Project p = sampleProject();
        p.write("cast.txt", p.read("cast.txt") + "\nscene|1r|bg_practice_ground.jpg\n");
        Story story = ScriptParser.parse(p.read("script.txt"));
        Art art = Art.fromManifest(p.read("cast.txt"), story, p.loader());
        Director.Options opt = new Director.Options();
        opt.aspect = "16:9";
        opt.sceneCards = false;
        Director d = new Director(story, opt);
        d.prepare();
        Film film = d.direct(art);
        int bridges = 0;
        for (String n : film.notes) if (n.startsWith("Bridge before")) bridges++;
        assertTrue("bridges " + bridges, bridges >= 1);
        Film.Seg first = null;
        for (Film.Seg sg : film.segs) if (sg.type == Film.S_SCENE) { first = sg; break; }
        assertNotNull(first);
        assertTrue("the reverse angle of scene 1", first.backdropReverse != null);
        int reverse = 0;
        for (Film.Seg sg : film.segs) for (Film.Cam c : sg.cams) if (c.reverse) reverse++;
        assertTrue("reverse cams " + reverse, reverse >= 3);
        assertTrue(film.shotList.contains("Phone-local guide (v1.0)"));
        Film.Seg dip = null;
        for (int i = 1; i < film.segs.size(); i++) if (film.segs.get(i).type == Film.S_SCENE && film.segs.get(i - 1).type == Film.S_SCENE && film.segs.get(i).transition == 0) { dip = film.segs.get(i); break; }
        if (dip != null) {
            Bitmap bmp = Bitmap.createBitmap(320, 180, Bitmap.Config.ARGB_8888);
            AndroidGfx g = new AndroidGfx(bmp, 4);
            new Renderer(film, art).render(g, dip.t0 + 0.28f);
            int[] fp = new int[320 * 180];
            bmp.getPixels(fp, 0, 320, 0, 0, 320, 180);
            double lum = 0;
            for (int c : fp) lum += 0.299 * ((c >> 16) & 255) + 0.587 * ((c >> 8) & 255) + 0.114 * (c & 255);
            lum /= fp.length;
            assertTrue("the dip frame is dark, not two scenes blended: " + lum, lum < 45);
            g.release();
        }
        // the doll takes its colours from the nearest uploaded picture (the sample's library pictures)
        Story.CharacterDef c0 = story.cast().get(0);
        String[] note = {""};
        com.tarun.kahani.app.Library lib = com.tarun.kahani.app.Library.get(RuntimeEnvironment.getApplication());
        byte[] vr = Files.readAllBytes(new File(ASSETS, "sample/char_vrinda.jpg").toPath());
        com.tarun.kahani.app.Library.Item ref = lib.addBytes(com.tarun.kahani.app.Library.PIC, "person", c0.displayName, "", vr, ".jpg", "test");
        lib.analysePicture(ref);
        Look rl = (Look) call("com.tarun.kahani.app.Studio3DArt", "referenceLook", c0.look, c0, lib, note);
        assertNotNull(rl);
        assertTrue("reference note: " + note[0], note[0].contains("reference"));
        // v23: a character without a picture is made from the best-fitting library picture, recoloured to the description
        int[] cloth = new int[40 * 40];
        java.util.Arrays.fill(cloth, 0xFFD02020);                       // red cloth
        int[] rec = (int[]) call("com.tarun.kahani.app.Studio3DArt", "recolour", cloth, 40, 40, 0xFF2040D0, 0xFFE8C04A);
        float[] hv = com.tarun.kahani.core.PicSense.hsv(rec[800]);
        assertTrue("the red cloth turned blue: hue " + hv[0], hv[0] > 200 && hv[0] < 260);
        String made = (String) call("com.tarun.kahani.app.Studio3DArt", "referencePicture", p, story, c0, lib, RuntimeEnvironment.getApplication(), null, false);
        assertTrue("a picture made from the user's picture", made != null && p.has(made) && p.file(made).length() > 1000);
        assertTrue(p.setting("credit3d." + c0.displayName, "").contains("recoloured"));
        // v24: a place without a picture is made from the user's own place picture that fits the scene's words best
        byte[] gd = Files.readAllBytes(new File(ASSETS, "sample/bg_garden.jpg").toPath());
        com.tarun.kahani.app.Library.Item place = lib.addBytes(com.tarun.kahani.app.Library.PIC, "place", "महल का बगीचा", "garden बगीचा", gd, ".jpg", "test");
        lib.analysePicture(place);
        Story.Scene sc1 = story.scenes.get(0);
        String madePlace = (String) call("com.tarun.kahani.app.Studio3DArt", "referencePlace", p, sc1, sc1.setting, com.tarun.kahani.core.Sets.NIGHT, lib, RuntimeEnvironment.getApplication(), null, false);
        assertTrue("a place made from the user's picture: " + madePlace, madePlace != null && p.file(madePlace).length() > 1000);
        assertTrue(p.read("cast.txt").contains("scene|1|" + madePlace) || p.read("cast.txt").contains("|" + madePlace));
        System.out.println("PHONE GUIDE: bridges " + bridges + " reverse cams " + reverse + " pieces " + pieces.size() + " " + note[0] + " made " + made + " place " + madePlace);
    }

    @Test
    public void pixarLeadFormatsSpineBraintrustAndStillPages() throws Exception {
        // formats: the output size always has the format's exact shape (never a stretched frame)
        String[] ars = {"16:9", "9:16", "1:1", "4:5", "2.39:1"};
        for (String ar : ars) {
            com.tarun.kahani.core.Edits e = new com.tarun.kahani.core.Edits();
            e.aspect = ar; e.height = 1080;
            int[] sz = e.size();
            float want = com.tarun.kahani.core.PixarLead.spec(ar).ratio, got = sz[0] / (float) sz[1];
            assertTrue(ar + " came out as " + sz[0] + "x" + sz[1], Math.abs(got / want - 1) < 0.02f);
            assertTrue(ar + " width must be a multiple of 16 or 2", sz[0] % 2 == 0 && sz[1] % 2 == 0);
        }
        assertTrue(com.tarun.kahani.core.PixarLead.spec("9:16").charScale == 0.5f && com.tarun.kahani.core.PixarLead.spec("16:9").charScale == 0.6f
                && com.tarun.kahani.core.PixarLead.spec("1:1").charScale == 0.65f);
        assertTrue(com.tarun.kahani.core.PixarLead.spec("9:16").top == 0.15f && com.tarun.kahani.core.PixarLead.spec("16:9").top == 0.20f);
        int[] plate = com.tarun.kahani.core.TechnicalDirector.sizeFor("16:9", 1080);
        assertTrue("native plate size", plate[0] == 1920 && plate[1] == 1080);
        plate = com.tarun.kahani.core.TechnicalDirector.sizeFor("2.39:1", 720);
        assertTrue("cinema plate " + plate[0] + "x" + plate[1], plate[0] == 1920 && plate[1] == 804);
        // the command box
        com.tarun.kahani.core.CommandParser.Result cr = com.tarun.kahani.core.CommandParser.parse("make it for the cinema screen", new ArrayList<String>());
        assertTrue("cinema command: " + cr.commands, cr.commands.size() == 1 && "2.39:1".equals(cr.commands.get(0).get("value")));
        cr = com.tarun.kahani.core.CommandParser.parse("instagram portrait please", new ArrayList<String>());
        assertTrue("4:5 command: " + cr.commands, cr.commands.size() == 1 && "4:5".equals(cr.commands.get(0).get("value")));
        // the film: spine, acts, Braintrust, the protocol's counters in the quality check
        Project p = sampleProject();
        Story story = ScriptParser.parse(p.read("script.txt"));
        Art art = Art.fromManifest(p.read("cast.txt"), story, p.loader());
        Director.Options opt = new Director.Options();
        opt.aspect = "9:16";
        Director d = new Director(story, opt);
        d.prepare();
        Film film = d.direct(art);
        assertTrue("spine", film.spine != null && film.spine.length == 6 && film.spine[0].startsWith("Once upon a time"));
        assertTrue("braintrust", film.braintrust != null && film.braintrust.windows > 10 && film.braintrust.text.contains("Q3 a ma pause"));
        assertTrue("acts", film.segs.get(1).act >= 1 && film.segs.get(film.segs.size() - 2).act == 5);
        assertTrue("scale lock", Math.abs(film.segs.get(1).charScale - 0.5f) < 1e-6f);
        assertTrue("qc lines", film.shotList.contains("Pixar-Lead protocol v4.0") && film.shotList.contains("ma pauses") && film.shotList.contains("First-frame checks"));
        for (Film.Shot sh : film.shots) assertTrue("shot over 4 s", sh.dur <= 4.05f);
        // the thumbnail (16:9) and the poster (9:16), made separately: the poster's top 35% is empty for the title
        Film.Seg[] pages = d.stills();
        assertTrue(pages.length == 2);
        int[][] sizes = {{1280, 720}, {1080, 1920}};
        OUT.mkdirs();
        for (int i = 0; i < 2; i++) {
            Bitmap bmp = Bitmap.createBitmap(sizes[i][0], sizes[i][1], Bitmap.Config.ARGB_8888);
            AndroidGfx g = new AndroidGfx(bmp, 4);
            new Renderer(film, art).renderSeg(g, pages[i], 0.5f);
            int[] px = new int[sizes[i][0] * sizes[i][1]];
            bmp.getPixels(px, 0, sizes[i][0], 0, 0, sizes[i][0], sizes[i][1]);
            FileOutputStream o = new FileOutputStream(new File(OUT, i == 0 ? "thumbnail.png" : "poster.png"));
            bmp.compress(Bitmap.CompressFormat.PNG, 100, o);
            o.close();
            int distinct = 0, last = 0;
            for (int k = 0; k < px.length; k += 997) if (px[k] != last) { distinct++; last = px[k]; }
            assertTrue("page " + i + " looks blank", distinct > 30);
            if (i == 1) {
                // nothing but the solid colour (and its soft light) in the top 35%: the title's space
                int w = sizes[i][0];
                for (int y = 0; y < sizes[i][1] * 0.35f; y += 40) for (int x = 0; x < w; x += 40) {
                    int cpx = px[y * w + x];
                    int r = (cpx >> 16) & 255, gg = (cpx >> 8) & 255, b = cpx & 255;
                    assertTrue("something in the poster's title space at " + x + "," + y, r > gg && r > b);   // the crimson palette, no skin or costume
                }
            }
            g.release();
            bmp.recycle();
        }
        // the descriptions package carries the protocol's sections
        String book = com.tarun.kahani.core.ShotBook.write(story, null, "16:9");
        for (String must : new String[]{"0. STORY SPINE", "BRAINTRUST (every 5 shots", "two lights only", "PRINCIPLES: anticipation", "FORMAT: YOUTUBE_MAIN_16_9",
                "7. THUMBNAIL AND POSTER", "size = 1920x1080", "contains_speech = true", "squash and stretch"})
            assertTrue("descriptions missing: " + must, book.contains(must));
        assertTrue("a stretch instruction slipped into a prompt", !book.contains("stretch to") && !book.contains("crop to"));
    }

    /** A different script (no pictures): a boat at sea, a storm with rain, a tiger, candles at night — every rule holds. */
    @Test
    public void anyScriptIsStagedByTheProtocol() throws Exception {
        File f = new File(ASSETS, "../../../../tools/testdata/machhuare_ka_beta.txt");
        assertTrue("test story missing: " + f, f.exists());
        Story story = ScriptParser.parse(new String(Files.readAllBytes(f.toPath()), "UTF-8"));
        assertTrue("characters: " + story.characters.size(), story.characters.size() == 6);
        Director.Options opt = new Director.Options();
        opt.aspect = "16:9";
        opt.onTwos = true;
        Director d = new Director(story, opt);
        d.prepare();
        Art art = new Art();
        Film film = d.direct(art);
        Film.Seg boat = null, night = null;
        for (Film.Seg sg : film.segs) { if (sg.inBoat && boat == null) boat = sg; if (sg.type == Film.S_SCENE && sg.scene == 3) night = sg; }
        assertTrue("the sea scene puts the characters in the boat", boat != null);
        assertTrue("night in the village", night != null && night.tod == com.tarun.kahani.core.Sets.NIGHT);
        boolean rain = false, candles = false;
        for (Film.Weather w : film.weather) { if (w.type == Film.W_RAIN || w.type == Film.W_STORM) rain = true; if (w.type == Film.W_CANDLES) candles = true; }
        assertTrue("rain", rain);
        assertTrue("diyas at night", candles);
        // on twos: the brave son and his father are experts (24), the naughty little sister steps on threes (8)
        int stepped = 0, expert = 0;
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors) { if (a.stepFps > 0 && a.stepFps < 24) stepped++; if (a.stepFps == 24) expert++; }
        assertTrue("a rebel or learner moves on twos / threes", stepped >= 1);
        assertTrue("experts move on ones", expert >= 1);
        for (Film.Shot sh : film.shots) { assertTrue(sh.dur <= 4.05f); if (sh.speech) assertTrue("words " + sh.words, sh.words <= 6); }
        assertTrue(film.shotList.contains("Miyazaki ma pauses"));
        Bitmap bmp = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(bmp, 4);
        Renderer r = new Renderer(film, art);
        r.safeZoneOverlay = true;
        int[] px = new int[640 * 360];
        float[] times = {boat.t0 + 3f, night.t0 + 4f};
        for (float t : times) {
            r.render(g, t);
            bmp.getPixels(px, 0, 640, 0, 0, 640, 360);
            int distinct = 0, last = 0;
            for (int k = 0; k < px.length; k += 331) if (px[k] != last) { distinct++; last = px[k]; }
            assertTrue("frame blank at " + t, distinct > 30);
        }
        g.release();
    }

    /**
     * A 21st-century script in free form (roles after names, manner before the colon, laughs inside the narration,
     * an AI voice, a robot dog, a rooftop and a mall basement, sounds written as words): read, staged and lit right.
     */
    @Test
    public void modernFreeFormScriptIsReadStagedAndCued() throws Exception {
        File f = new File(ASSETS, "../../../../tools/testdata/neo_mumbai.txt");
        assertTrue("test story missing: " + f, f.exists());
        Story story = ScriptParser.parse(new String(Files.readAllBytes(f.toPath()), "UTF-8"));
        assertTrue("title: " + story.title, story.title.contains("Light Thieves"));
        assertTrue("characters: " + story.characters.size(), story.characters.size() == 7 && story.cast().size() == 6);
        Story.CharacterDef tara = ScriptParser.resolve(story, "तारा"), kabir = ScriptParser.resolve(story, "कबीर"), inaya = ScriptParser.resolve(story, "इनाया"),
                vex = ScriptParser.resolve(story, "वेक्स"), algora = ScriptParser.resolve(story, "एल्गोरा"), bolt = ScriptParser.resolve(story, "बोल्ट");
        assertTrue("roles", tara.fullName.equals("तारा मल्होत्रा") && tara.role.contains("Coder"));
        assertTrue("Tara: a girl in a hoodie with a ponytail", tara.look.female && tara.look.outfit == com.tarun.kahani.core.Look.O_HOODIE && tara.look.hair == com.tarun.kahani.core.Look.H_PONYTAIL);
        assertTrue("Kabir: a boy in a t-shirt with a controller", !kabir.look.female && kabir.look.kind == com.tarun.kahani.core.Look.BOY && kabir.look.outfit == com.tarun.kahani.core.Look.O_TSHIRT
                && kabir.look.gadget == com.tarun.kahani.core.Look.GD_CONTROLLER && kabir.look.lightShoes);
        assertTrue("Inaya: a girl (from the verbs) with glasses", inaya.look.female && inaya.look.glasses == 1);
        assertTrue("Vex: a man in a suit with dark glasses, a villain, not a monster", vex.look.kind == com.tarun.kahani.core.Look.MAN && vex.look.outfit == com.tarun.kahani.core.Look.O_SUIT
                && vex.look.glasses == 2 && !vex.look.hero);
        assertTrue("Algora: a witch in a coat with a hood", algora.look.kind == com.tarun.kahani.core.Look.WITCH && algora.look.outfit == com.tarun.kahani.core.Look.O_COAT
                && algora.look.headwear == com.tarun.kahani.core.Look.HW_HOOD && algora.look.techWand);
        assertTrue("Bolt: a robot dog", bolt.look.kind == com.tarun.kahani.core.Look.ANIMAL && bolt.look.species == com.tarun.kahani.core.Look.SP_DOG && bolt.look.robot);
        boolean voice = false;
        for (Story.CharacterDef c : story.characters) if (c.voiceOnly && c.displayName.contains("AI")) voice = true;
        assertTrue("the AI voice is a voice only", voice);
        // the lines: manner before the colon, the laugh inside the narration, the off-screen voice
        int lines = 0, inayaLines = 0, kabirLaughs = 0; boolean off = false;
        for (Story.Scene sc : story.scenes) for (Story.Beat b : sc.beats) {
            if (b.type != Story.Beat.DIALOGUE) continue;
            lines++;
            if (b.speaker == inaya) inayaLines++;
            if (b.speaker == kabir && b.text.startsWith("हा हा")) kabirLaughs++;
            if (b.offScreen) off = true;
        }
        assertTrue("lines " + lines, lines >= 24);
        assertTrue("Inaya's four lines (manner before the colon)", inayaLines == 4);
        assertTrue("Kabir laughs inside the narration", kabirLaughs == 2);
        assertTrue("Algora's off-screen line", off);
        // the places: scenes without a (स्थान:) line get the named place or the last one; the sets are today's
        assertTrue(story.scenes.get(4).setting.contains("गार्डन") && story.scenes.get(6).setting.contains("मॉल"));
        assertTrue(com.tarun.kahani.core.Sets.detect(story.scenes.get(0).setting) == com.tarun.kahani.core.Sets.ROOFTOP);
        assertTrue(com.tarun.kahani.core.Sets.detect(story.scenes.get(2).setting) == com.tarun.kahani.core.Sets.BASEMENT);
        // the film: cues become effects and sounds; the voice is never on the stage
        Director.Options opt = new Director.Options();
        Director d = new Director(story, opt);
        d.prepare();
        Art art = new Art();
        Film film = d.direct(art);
        java.util.Set<Integer> fx = new java.util.HashSet<Integer>(), sfx = new java.util.HashSet<Integer>();
        for (Film.Seg sg : film.segs) { for (Film.Fx x : sg.fx) fx.add(x.type); for (Film.Actor a : sg.actors) assertTrue("a voice on the stage", !a.c.voiceOnly); }
        for (Film.Sfx x : film.sfx) sfx.add(x.type);
        for (int must : new int[]{Film.FX_GLOW_AREA, Film.FX_NOTIFY, Film.FX_LIGHTS_OFF, Film.FX_GLITCH, Film.FX_DATA, Film.FX_BEAM, Film.FX_SPARKS, Film.FX_HEARTS, Film.FX_DRONE})
            assertTrue("effect missing: " + must, fx.contains(must));
        for (int must : new int[]{Film.SFX_TYPING, Film.SFX_BEEP, Film.SFX_POWER_DOWN, Film.SFX_GLITCH, Film.SFX_SPARK, Film.SFX_HUM, Film.SFX_TRAFFIC, Film.SFX_HEARTBEAT})
            assertTrue("sound missing: " + must, sfx.contains(must));
        assertTrue("morning on the rooftop", film.segs.get(1).set == com.tarun.kahani.core.Sets.ROOFTOP && film.segs.get(1).tod == com.tarun.kahani.core.Sets.MORNING);
        assertTrue("a dark basement", film.segs.get(3).set == com.tarun.kahani.core.Sets.BASEMENT && film.segs.get(3).tod == com.tarun.kahani.core.Sets.NIGHT);
        for (Film.Shot sh : film.shots) { assertTrue(sh.dur <= 4.05f); if (sh.speech) assertTrue(sh.words <= 6); }
        // two frames through the Android canvas: the rooftop morning, the basement
        Bitmap bmp = Bitmap.createBitmap(640, 360, Bitmap.Config.ARGB_8888);
        AndroidGfx g = new AndroidGfx(bmp, 4);
        Renderer r = new Renderer(film, art);
        int[] px = new int[640 * 360];
        for (float t : new float[]{film.segs.get(1).t0 + 3f, film.segs.get(3).t0 + 4f}) {
            r.render(g, t);
            bmp.getPixels(px, 0, 640, 0, 0, 640, 360);
            int distinct = 0, last = 0;
            for (int k = 0; k < px.length; k += 331) if (px[k] != last) { distinct++; last = px[k]; }
            assertTrue("frame blank at " + t, distinct > 30);
        }
        g.release();
    }

    /**
     * v34: pictures are taken for every character, every place, every added scene and every thing of the story, from
     * every button that offers them — and the newest front, place or insert is the one the Studio shows and the film
     * uses, whichever key the button used (the Studio's cast key or the popup's display name).
     */
    @Test
    public void everyCharacterPlaceAndThingTakesPictures() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        Project p = sampleProject();
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        set(a, "project", p);
        Field lf = MainActivity.class.getDeclaredField("library");
        lf.setAccessible(true);
        com.tarun.kahani.app.Library lib = (com.tarun.kahani.app.Library) lf.get(a);
        Story st = (Story) call("com.tarun.kahani.app.FilmJob", "storyOf", p);
        set(a, "castStory", st);
        Method keyFor = MainActivity.class.getDeclaredMethod("keyFor", Story.CharacterDef.class);
        keyFor.setAccessible(true);
        Method charFile = MainActivity.class.getDeclaredMethod("charFile", Story.CharacterDef.class);
        charFile.setAccessible(true);
        Method sceneFile = MainActivity.class.getDeclaredMethod("sceneFile", Story.Scene.class);
        sceneFile.setAccessible(true);
        byte[] picA = Files.readAllBytes(new File(ASSETS, "sample/char_vrinda.jpg").toPath());
        byte[] picB = Files.readAllBytes(new File(ASSETS, "sample/char_kripa.jpg").toPath());
        byte[] place = Files.readAllBytes(new File(ASSETS, "sample/bg_forest.jpg").toPath());
        byte[] thing = Files.readAllBytes(new File(ASSETS, "sample/char_kripa_pouch.jpg").toPath());
        StringBuilder rep = new StringBuilder();
        // 1. every character: a front from the Studio's button (its cast key), then another from the popup (its display name)
        assertTrue("characters in the story", st.cast().size() >= 5);
        for (Story.CharacterDef c : st.cast()) {
            String key = (String) keyFor.invoke(a, c);
            List<byte[]> one = new ArrayList<byte[]>(); one.add(picA);
            call("com.tarun.kahani.app.SheetSaver", "save", p, lib, st, "angles:char:" + key + ":" + c.shown(), one, null);
            String first = (String) charFile.invoke(a, c);
            assertNotNull(c.shown() + ": a front after the Studio's upload", first);
            List<byte[]> two = new ArrayList<byte[]>(); two.add(picB);
            call("com.tarun.kahani.app.SheetSaver", "save", p, lib, st, "angles:char:" + c.displayName + ":" + c.shown(), two, null);
            String second = (String) charFile.invoke(a, c);
            assertNotNull(c.shown() + ": a front after the popup's upload", second);
            assertTrue(c.shown() + ": the newest front is shown (" + first + " -> " + second + ")", !second.equals(first));
            String film = (String) call("com.tarun.kahani.app.Studio3DArt", "charFile", p, st, c);
            assertTrue(c.shown() + ": the film uses the same front as the Studio (" + film + " vs " + second + ")", second.equals(film));
            int lines = 0; java.util.Set<String> keys = new java.util.HashSet<String>();
            for (String l : p.read("cast.txt").split("\n")) {
                String[] f = l.split("\\|");
                if (f.length >= 3 && f[0].equals("char") && ScriptParser.resolve(st, f[1]) == c) lines++;
                if (f.length >= 3 && (f[0].equals("pose") || f[0].equals("view")) && ScriptParser.resolve(st, f[1]) == c) keys.add(f[1]);
            }
            assertTrue(c.shown() + ": one character line, not " + lines, lines == 1);
            assertTrue(c.shown() + ": its pictures under one key, not " + keys, keys.size() <= 1);
            rep.append(c.shown()).append(": ").append(first).append(" -> ").append(second).append('\n');
        }
        // 2. every place: a new picture replaces the wide view, the earlier one stays in the library
        for (Story.Scene sc : st.scenes) {
            String before = (String) sceneFile.invoke(a, sc);
            List<byte[]> one = new ArrayList<byte[]>(); one.add(place);
            call("com.tarun.kahani.app.SheetSaver", "save", p, lib, st, "angles:scene:" + sc.number + ":" + sc.title, one, null);
            String after = (String) sceneFile.invoke(a, sc);
            assertNotNull("part " + sc.number + ": a place picture", after);
            assertTrue("part " + sc.number + ": the new picture is the place now", !after.equals(before));
            if (before != null) {
                boolean kept = false;
                for (com.tarun.kahani.app.Library.Item it : lib.find(com.tarun.kahani.app.Library.PIC, null, null)) if (it.name.endsWith("(earlier)")) kept = true;
                assertTrue("part " + sc.number + ": the earlier place picture is in the library", kept);
            }
            rep.append("part ").append(sc.number).append(": ").append(before).append(" -> ").append(after).append('\n');
        }
        // 3. every scene the director adds
        for (com.tarun.kahani.core.ScenePlan.Extra x : com.tarun.kahani.core.ScenePlan.extras(st)) {
            List<byte[]> one = new ArrayList<byte[]>(); one.add(place);
            call("com.tarun.kahani.app.SheetSaver", "save", p, lib, st, "angles:scene:" + x.key + ":" + x.label, one, null);
            assertNotNull("added scene " + x.label + ": its picture", p.manifestLine("scene", x.key));
            rep.append("added scene ").append(x.key).append(": ").append(p.manifestLine("scene", x.key)).append('\n');
        }
        // 4. every thing of the story and one named now: the newest insert replaces the earlier one
        Method things = MainActivity.class.getDeclaredMethod("things", Story.class);
        things.setAccessible(true);
        @SuppressWarnings("unchecked") List<String[]> ts = new ArrayList<String[]>((List<String[]>) things.invoke(a, st));
        ts.add(new String[]{"पगड़ी", "पगड़ी"});
        assertTrue("things in the story", ts.size() >= 2);
        for (String[] o : ts) {
            for (int k = 0; k < 2; k++) {
                List<byte[]> one = new ArrayList<byte[]>(); one.add(k == 0 ? thing : picB);
                call("com.tarun.kahani.app.SheetSaver", "save", p, lib, st, "angles:obj:" + o[0] + ":" + o[1], one, null);
            }
            int n = 0; String file = null;
            for (String l : p.read("cast.txt").split("\n")) {
                String[] f = l.split("\\|");
                if (f.length >= 5 && f[0].equals("shot") && f[1].trim().length() == 0 && f[2].trim().equals(o[0]) && f[4].trim().equals("object")) { n++; file = f[3]; }
            }
            assertTrue(o[1] + ": one insert line, not " + n, n == 1);
            assertTrue(o[1] + ": its file is in the story", p.has(file));
            rep.append("thing ").append(o[1]).append(": ").append(file).append('\n');
        }
        // 5. the Studio: one upload button per character and per place, one per thing
        call(a, "showStudio", new Class<?>[0]);
        idle();
        View root = a.findViewById(android.R.id.content);
        List<String> tx = texts(root, new ArrayList<String>());
        int up = 0, objUp = 0;
        for (String t : tx) { if (t.equals("📷 Pictures (up to 10)")) up++; if (t.equals("📷 Angles") || t.equals("📷 More angles")) objUp++; }
        assertTrue("one upload button per character and place: " + up + " for " + st.cast().size() + " + " + st.scenes.size(), up == st.cast().size() + st.scenes.size());
        assertTrue("one upload button per thing: " + objUp + " for " + ts.size(), objUp >= Math.min(20, ts.size()));
        // 6. the progress screen's card: a row for every character, place, added scene and thing
        LinearLayout card = new LinearLayout(a);
        call(a, "picturesCard", new Class<?>[]{LinearLayout.class, Story.class}, card, st);
        idle();
        List<String> rows = texts(card, new ArrayList<String>());
        int more = 0;
        for (String t : rows) if (t.equals("📷 More") || t.equals("📷 Add")) more++;
        int want = st.cast().size() + st.scenes.size() + com.tarun.kahani.core.ScenePlan.extras(st).size() + ts.size();
        assertTrue("a row with its button for everything: " + more + " of " + want, more == want);
        // 7. the make-film popup lists every character, place, added scene and thing
        call(a, "makeFilm", new Class<?>[0]);
        idle();
        android.app.AlertDialog mk = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull("the make-film dialog", mk);
        mk.getButton(android.content.DialogInterface.BUTTON_NEUTRAL).performClick(); idle();
        android.app.AlertDialog list = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
        assertNotNull(list);
        List<String> items = new ArrayList<String>();
        for (int i = 0; i < list.getListView().getCount(); i++) items.add(String.valueOf(list.getListView().getItemAtPosition(i)));
        for (Story.CharacterDef c : st.cast()) { boolean has = false; for (String it : items) if (it.startsWith(c.shown())) has = true; assertTrue("popup lists " + c.shown() + ": " + items, has); }
        for (Story.Scene sc : st.scenes) { boolean has = false; for (String it : items) if (it.startsWith("Part " + sc.number + ":")) has = true; assertTrue("popup lists part " + sc.number + ": " + items, has); }
        for (String[] o : ts) { boolean has = false; for (String it : items) if (it.startsWith("🔑 " + o[1]) || it.startsWith(o[1] + " (scene")) has = true; assertTrue("popup lists the thing " + o[1] + ": " + items, has); }
        assertTrue("popup lets a thing be named", items.get(items.size() - 1).startsWith("➕"));
        list.dismiss(); mk.dismiss();
        // 8. a character whose name reads like a voice is pictured after all when the user says so
        Project q = Project.create(ctx);
        q.write("script.txt", "रेडियो की कहानी\n\n(स्थान: एक छोटा घर। सुबह का समय।)\nमीना: \"आज रेडियो पर क्या आएगा?\"\nरेडियो की आवाज़: \"आज की ताज़ा ख़बरें!\"\nमीना: \"वाह!\"\n\nसमाप्त\n");
        Story vs = (Story) call("com.tarun.kahani.app.FilmJob", "storyOf", q);
        Story.CharacterDef radio = null;
        for (Story.CharacterDef c : vs.characters) if (c.voiceOnly) radio = c;
        assertNotNull("the radio reads as a voice only: " + vs.characters.size(), radio);
        assertTrue("a voice only is not in the cast", !vs.cast().contains(radio));
        set(a, "project", q);
        call(a, "showStudio", new Class<?>[0]);
        idle();
        View anyway = byText(a.findViewById(android.R.id.content), "📷 Picture it anyway", new int[]{0});
        assertNotNull("the voice-only row offers a picture anyway", anyway);
        anyway.performClick(); idle();
        firstDialogItem();
        rep.append("picture it anyway: ").append(pickerStarted(a)).append('\n');
        Story vs2 = (Story) call("com.tarun.kahani.app.FilmJob", "storyOf", q);
        boolean pictured = false;
        for (Story.CharacterDef c : vs2.cast()) if (c.displayName.equals(radio.displayName)) pictured = true;
        assertTrue("the radio is pictured from now on", pictured);
        System.out.println("PICTURES FOR EVERYTHING:\n" + rep);
        ac.pause().stop().destroy();
    }

    /**
     * v34: a character with more than one head (Ravana): the head count read from the name or the description, the
     * face taken from the central head of the picture (the one that speaks), the heads beside it never mistaken
     * for raised arms, the picture read as a front, and the drawn puppet given its other heads.
     */
    @Test
    public void manyHeadedCharactersKeepTheirHeads() throws Exception {
        // 1. the description
        assertTrue(com.tarun.kahani.core.LookDesigner.headsIn("रावण — दस सिर वाला राक्षस, सोने का मुकुट") == 10);
        assertTrue(com.tarun.kahani.core.LookDesigner.headsIn("लंका का राजा, दशानन") == 10);
        assertTrue(com.tarun.kahani.core.LookDesigner.headsIn("तीन सिरों वाला अजगर, हरे शल्क") == 3);
        assertTrue(com.tarun.kahani.core.LookDesigner.headsIn("a three-headed dragon with green scales") == 3);
        assertTrue(com.tarun.kahani.core.LookDesigner.headsIn("a monster with 5 heads") == 5);
        assertTrue(com.tarun.kahani.core.LookDesigner.headsIn("उसके सिर पर लाल पगड़ी है") == 1);
        assertTrue(com.tarun.kahani.core.LookDesigner.headsIn("a girl with a red ribbon in her hair, head held high") == 1);
        Story st = ScriptParser.parse("रावण और राम\n\nरावण (दस सिर वाला राक्षस, काले बाल, सोने का मुकुट, लाल आँखें)\nराम (नीला धनुषधारी राजकुमार)\n\n(स्थान: लंका का महल।)\nरावण: \"कौन है?\"\nराम: \"मैं राम हूँ।\"\n\nसमाप्त\n");
        Story.CharacterDef ravan = null;
        for (Story.CharacterDef c : st.characters) if (c.displayName.contains("रावण")) ravan = c;
        assertNotNull(ravan);
        assertTrue("Ravana has ten heads: " + ravan.look.heads, ravan.look.heads == 10);
        // 2. a picture: a body with five heads in a row (skin, dark eyes with whites, a red mouth, dark hair above)
        int w = 640, h = 960;
        int[] px = new int[w * h];
        java.util.Arrays.fill(px, 0xFFFFFFFF);
        for (int y = 330; y < 900; y++) for (int x = 200; x < 440; x++) px[y * w + x] = 0xFF2A4FA0;      // the body
        for (int y = 300; y < 340; y++) for (int x = 290; x < 350; x++) px[y * w + x] = 0xFFD9A074;      // the neck
        int[] cxs = {100, 210, 320, 430, 540};
        for (int cx : cxs) {
            int cy = 230, r = 52;
            for (int y = cy - r - 18; y <= cy + r; y++) for (int x = cx - r; x <= cx + r; x++) {
                float dx = x - cx, dy = y - cy;
                if (dx * dx + dy * dy <= r * r) px[y * w + x] = y < cy - r * 0.55f ? 0xFF1E1410 : 0xFFD9A074;    // hair above, skin below
                else if (y < cy - r * 0.3f && dx * dx + (dy + 14) * (dy + 14) <= (r + 6) * (r + 6)) px[y * w + x] = 0xFF1E1410;   // the hair's top
            }
            for (int e = -1; e <= 1; e += 2) {
                int ex = cx + e * 18, ey = cy - 8;
                for (int y = ey - 8; y <= ey + 8; y++) for (int x = ex - 11; x <= ex + 11; x++) if ((x - ex) * (x - ex) / 121f + (y - ey) * (y - ey) / 64f <= 1) px[y * w + x] = 0xFFFFFFFF;   // the white
                for (int y = ey - 5; y <= ey + 5; y++) for (int x = ex - 5; x <= ex + 5; x++) if ((x - ex) * (x - ex) + (y - ey) * (y - ey) <= 25) px[y * w + x] = 0xFF1A1008;                  // the iris
            }
            for (int y = cy + 22; y <= cy + 26; y++) for (int x = cx - 14; x <= cx + 14; x++) px[y * w + x] = 0xFF9C2A2A;    // the mouth
        }
        com.tarun.kahani.core.Cutout.Result r = com.tarun.kahani.core.Cutout.process(px, w, h, false);
        assertTrue("a face is found", r.faceFound);
        assertTrue("five heads read in the picture: " + r.heads, r.heads >= 3);
        float mx = r.mouthX * r.w + r.cropX;
        assertTrue("the face is the central head's (mouth at x=" + mx + ")", Math.abs(mx - 320) < 40);
        assertTrue("a many-headed front is a front: " + com.tarun.kahani.core.Angles.name(com.tarun.kahani.core.Angles.guess(r)),
                com.tarun.kahani.core.Angles.guess(r) == com.tarun.kahani.core.Angles.FRONT || com.tarun.kahani.core.Angles.guess(r) == com.tarun.kahani.core.Angles.THREE_QUARTER);
        // 3. the rig: no raised arm from the heads beside the face, the whole row is the head
        Art.Sprite sp = new Art.Sprite();
        sp.faceKnown = true;
        sp.eyeLX = r.eyeLX; sp.eyeRX = r.eyeRX; sp.eyeLY = r.eyeY; sp.eyeRY = r.eyeY; sp.eyeR = r.eyeR;
        sp.mouthX = r.mouthX; sp.mouthY = r.mouthY; sp.mouthHW = r.mouthW / 2;
        Rig rig = Rig.build(r, sp, ravan.look, sampleProject().loader());
        assertNotNull("a rig for the many-headed figure", rig);
        assertTrue("no raised arm read from the other heads", !rig.armUp[0] && !rig.armUp[1]);
        assertTrue("the head is the whole row: " + rig.headHalf, rig.headHalf > 0.3f);
        // 4. the drawn puppet has its heads: wider at the head than a one-headed one
        int[] wide = new int[2];
        for (int k = 0; k < 2; k++) {
            Look lk = ravan.look.copy();
            lk.heads = k == 0 ? 1 : 10;
            Bitmap b = Bitmap.createBitmap(600, 300, Bitmap.Config.ARGB_8888);
            AndroidGfx g = new AndroidGfx(b, 1);
            g.color(0xFFFFFFFF); g.rect(0, 0, 600, 300);
            Pose p = new Pose(); p.time = 1;
            g.save(); g.translate(300, 290);
            com.tarun.kahani.core.Puppet.draw(g, lk, p, 260);
            g.restore();
            int minX = 600, maxX = 0;
            for (int y = 0; y < 90; y++) for (int x = 0; x < 600; x++) if ((b.getPixel(x, y) & 0xFFFFFF) != 0xFFFFFF) { minX = Math.min(minX, x); maxX = Math.max(maxX, x); }
            wide[k] = maxX - minX;
        }
        assertTrue("ten drawn heads are wider than one: " + wide[0] + " -> " + wide[1], wide[1] > wide[0] * 2);
        System.out.println("MANY HEADS: picture heads=" + r.heads + " mouthX=" + mx + " headHalf=" + rig.headHalf + " puppet width " + wide[0] + " -> " + wide[1]);
    }

    /**
     * v34 (the emotion / activity / angle guide §11, §13): the user's sheets darkened as if photographed in a dark room
     * read like the bright ones — the same angle for most figures, a light level on every reading, a lower confidence
     * on the dark ones (so the user is asked to confirm), and never a feeling on a back or a faceless figure.
     */
    @Test
    public void darkPicturesReadLikeBrightOnes() throws Exception {
        File[] sheets = userSheets();
        File dir = sheets[0].getParentFile();
        String[] names = {"sheet02.jpg", "sheet15.jpg", "sheet01.jpg"};
        boolean[] beast = {false, false, true};
        StringBuilder rep = new StringBuilder();
        int totalFigs = 0, agree = 0, lessSure = 0;
        for (int n = 0; n < names.length; n++) {
            int[] wh = new int[2];
            int[] px = pixelsOf(new File(dir, names[n]), wh);
            List<com.tarun.kahani.core.Angles.Piece> figs = com.tarun.kahani.core.Angles.figures(com.tarun.kahani.core.Angles.split(px, wh[0], wh[1]), wh[0], wh[1]);
            assertTrue(names[n] + ": " + figs.size(), figs.size() >= 10);
            int standH = 0;
            List<com.tarun.kahani.core.Cutout.Result> bright = new ArrayList<com.tarun.kahani.core.Cutout.Result>(), dark = new ArrayList<com.tarun.kahani.core.Cutout.Result>();
            for (com.tarun.kahani.core.Angles.Piece pc : figs) {
                com.tarun.kahani.core.Cutout.Result rb = com.tarun.kahani.core.Cutout.process(pc.px.clone(), pc.w, pc.h, beast[n]);
                int[] dp = pc.px.clone();
                for (int i = 0; i < dp.length; i++) { int c = dp[i]; dp[i] = (c & 0xFF000000) | ((int) (((c >> 16) & 255) * 0.3f) << 16) | ((int) (((c >> 8) & 255) * 0.3f) << 8) | (int) ((c & 255) * 0.3f); }
                com.tarun.kahani.core.Cutout.Result rd = com.tarun.kahani.core.Cutout.process(dp, pc.w, pc.h, beast[n]);
                bright.add(rb); dark.add(rd);
                if (rb.w < rb.h * 1.25f) standH = Math.max(standH, rb.h);
            }
            int sheetAgree = 0;
            for (int i = 0; i < figs.size(); i++) {
                com.tarun.kahani.core.Cutout.Result rb = bright.get(i), rd = dark.get(i);
                // (a dark-furred monkey is "low light" even in a bright sheet — the brightened reading is what saves it)
                assertTrue(names[n] + " #" + (i + 1) + " bright light level: " + rb.lightLevel, beast[n] || rb.lightLevel <= com.tarun.kahani.core.Cutout.NORMAL);
                assertTrue(names[n] + " #" + (i + 1) + " dark light level: " + rd.lightLevel + " vs " + rb.lightLevel, rd.lightLevel >= com.tarun.kahani.core.Cutout.LOW && rd.lightLevel > rb.lightLevel);
                if (rd.faceFound) assertTrue(names[n] + " #" + (i + 1) + " read from a brightened copy", rd.pxRead != null && rd.pxRead != rd.px);
                com.tarun.kahani.core.PoseSense.Tag tb = com.tarun.kahani.core.PoseSense.tag(rb, standH, beast[n]), td = com.tarun.kahani.core.PoseSense.tag(rd, standH, beast[n]);
                totalFigs++;
                if (tb.angle == td.angle) { agree++; sheetAgree++; }
                if (td.conf < tb.conf) lessSure++;
                for (com.tarun.kahani.core.PoseSense.Tag t : new com.tarun.kahani.core.PoseSense.Tag[]{tb, td}) {
                    if (t.angle == com.tarun.kahani.core.Angles.BACK || !t.m.face)
                        assertTrue(names[n] + " #" + (i + 1) + " a feeling on a back / faceless figure: " + t, t.emotion == com.tarun.kahani.core.PoseSense.NO_FACE || t.emotion == com.tarun.kahani.core.PoseSense.ASLEEP);
                    assertTrue(t.record(), t.record().contains("light=") && t.record().contains("confidence="));
                }
                rep.append(names[n]).append(" #").append(i + 1).append(": bright ").append(tb).append(String.format(java.util.Locale.US, " (%.2f)", tb.conf)).append(" | dark ").append(td).append(String.format(java.util.Locale.US, " (%.2f, %s)", td.conf, com.tarun.kahani.core.Cutout.lightName(td.light))).append('\n');
            }
            assertTrue(names[n] + ": the dark readings agree with the bright ones on " + sheetAgree + " of " + figs.size(), sheetAgree >= figs.size() * 3 / 4);
        }
        assertTrue("dark readings are less sure: " + lessSure + " of " + totalFigs, lessSure >= totalFigs * 3 / 4);
        System.out.println("DARK vs BRIGHT (" + agree + "/" + totalFigs + " angles agree):\n" + rep);
    }

    /**
     * v34 (the 3D still picture maker manual): every prompt carries the six blocks and the negative list (after the
     * Technical Director's cleaning); the doll is drawn at least 2048 px tall where the heap allows, its face is never
     * covered by its own shoulders (adult and monster too), its eyes have catch-lights and its skin warm shadows; a
     * doll with blown whites is lit again and passes; the 3D maker's proposal carries the checklist; a library
     * picture smaller than 512 px is never the maker's reference.
     */
    @Test
    public void dollPassesTheStillPictureChecklist() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        Project p = Project.create(ctx);
        p.write("script.txt", "जंगल की कहानी\n\nभोला (40 साल का आदमी, सफ़ेद कुर्ता, बड़ी मूँछें)\nखान राक्षस (विशालकाय राक्षस, काले बाल, लाल आँखें, सींग)\n\n(स्थान: घना जंगल, सुबह।)\nभोला: \"यह कौन है?\"\nखान राक्षस: \"हा हा हा!\"\n\nसमाप्त\n");
        Story st = ScriptParser.parse(p.read("script.txt"));
        StringBuilder rep = new StringBuilder();
        // 1. the prompt builder
        for (Story.CharacterDef c : st.cast()) {
            String pr = TechnicalDirector.clean(com.tarun.kahani.core.Bible.characterPrompt(c));
            assertTrue("character prompt complete: " + pr, com.tarun.kahani.core.StillPrompt.complete(pr));
        }
        String pl = com.tarun.kahani.core.Bible.placePrompt("घना जंगल", "घना जंगल, सुबह", "16:9");
        assertTrue("place prompt complete: " + pl, com.tarun.kahani.core.StillPrompt.complete(pl));
        assertTrue("no aspect words in the prompt", !pl.contains("16:9"));
        // 2. the dolls: faces visible, catch-lights, warm skin, 2048 px
        for (Story.CharacterDef c : st.cast()) {
            com.tarun.kahani.core.Doll3D.Result r = com.tarun.kahani.core.Doll3D.make(c.look, 2900, 7, 0, com.tarun.kahani.core.Pose.NEUTRAL, null);
            com.tarun.kahani.core.StillQa.Result q = com.tarun.kahani.core.StillQa.check(r.px, r.w, r.h, new float[]{r.eyeLX, r.eyeLY, r.eyeRX, r.eyeRY, r.eyeR}, null, false, com.tarun.kahani.core.StillQa.HANDS_GEOMETRY);
            rep.append(c.shown()).append(": ").append(q.summary()).append('\n');
            assertTrue(c.shown() + " eyes with catch-lights (face not covered): " + q.summary(), q.catchLights);
            assertTrue(c.shown() + " skin glows: " + q.summary(), q.warmSkin);
            assertTrue(c.shown() + " at least 2048 px: " + r.w + "x" + r.h, Math.max(r.w, r.h) >= 2048);
            // the mouth is the face's, not the shirt's: its colour is far from the outfit's
            int mc = r.px[Math.min(r.h - 1, (int) (r.mouthY * r.h)) * r.w + (int) (r.mouthX * r.w)];
            int pc = c.look.primary;
            int d = Math.abs(((mc >> 16) & 255) - ((pc >> 16) & 255)) + Math.abs(((mc >> 8) & 255) - ((pc >> 8) & 255)) + Math.abs((mc & 255) - (pc & 255));
            assertTrue(c.shown() + " mouth not covered by the shirt (colour distance " + d + ")", d > 40 || c.look.kind == com.tarun.kahani.core.Look.MONSTER);
        }
        // 3. blown whites are lit again and pass
        Story.CharacterDef bhola = st.cast().get(0);
        com.tarun.kahani.core.Look white = bhola.look.copy();
        white.primary = 0xFFF6F6F2; white.secondary = 0xFFF0F0EC;
        com.tarun.kahani.core.Doll3D.Result w1 = com.tarun.kahani.core.Doll3D.make(white, 2900, 7, 0, com.tarun.kahani.core.Pose.NEUTRAL, null, 1f);
        com.tarun.kahani.core.Doll3D.Result w2 = com.tarun.kahani.core.Doll3D.make(white, 2900, 7, 0, com.tarun.kahani.core.Pose.NEUTRAL, null, 0.8f);
        com.tarun.kahani.core.StillQa.Result q1 = com.tarun.kahani.core.StillQa.check(w1.px, w1.w, w1.h, null, null, false, 0), q2 = com.tarun.kahani.core.StillQa.check(w2.px, w2.w, w2.h, null, null, false, 0);
        rep.append("white kurta: ").append(q1.summary()).append(" → relit: ").append(q2.summary()).append('\n');
        assertTrue("the relight takes the blown whites down: " + q1.summary() + " → " + q2.summary(), !q2.harsh && q2.score >= q1.score);
        // 4. the 3D maker's proposal carries the checklist
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        Field lf = MainActivity.class.getDeclaredField("library");
        lf.setAccessible(true);
        com.tarun.kahani.app.Library lib = (com.tarun.kahani.app.Library) lf.get(a);
        String key = (String) s3d("keyFor", p, st, bhola);
        p.setSetting("rejected3d.ref." + key, "1");
        s3d("makeCharacter", p, st, bhola, lib, ctx, null, true, null, false);
        String[] prop = (String[]) s3d("proposalFor", p, "char", key);
        assertNotNull("a doll proposed", prop);
        String verdict = prop[prop.length - 1];
        rep.append("proposal: ").append(verdict).append('\n');
        assertTrue("the proposal carries the checklist: " + verdict, verdict.startsWith("Score") && verdict.contains("Still QA") && verdict.contains("catch-lights"));
        // 5. the cleaning rule: no reference under 512 px
        Bitmap small = Bitmap.createBitmap(300, 300, Bitmap.Config.ARGB_8888), big = Bitmap.createBitmap(600, 900, Bitmap.Config.ARGB_8888);
        java.io.ByteArrayOutputStream bs = new java.io.ByteArrayOutputStream(), bb = new java.io.ByteArrayOutputStream();
        small.compress(Bitmap.CompressFormat.PNG, 100, bs); big.compress(Bitmap.CompressFormat.PNG, 100, bb);
        com.tarun.kahani.app.Library.Item is = lib.addBytes("pic", "person", "tiny", "", bs.toByteArray(), ".png", "test");
        com.tarun.kahani.app.Library.Item ib = lib.addBytes("pic", "person", "large", "", bb.toByteArray(), ".png", "test");
        assertTrue("a 300 px picture is no reference", !(Boolean) s3d("bigEnough", lib, is));
        assertTrue("a 900 px picture is", (Boolean) s3d("bigEnough", lib, ib));
        System.out.println("STILL PICTURE CHECKLIST:\n" + rep);
        ac.pause().stop().destroy();
    }

    /**
     * v34: a character with more than two arms and one who sits on an animal (Durga on her lion): the arm count and
     * the mount read from the description, the drawn puppet and the doll given their arms and the animal, the rig
     * of such a picture keeping the arms still and the legs off the walk, and the director's cut shortened to 2.2 s.
     */
    @Test
    public void manyArmedRidersAndShorterShots() throws Exception {
        // 1. the words
        Story st = ScriptParser.parse("पात्र और रूप-रंग (Characters):\n1. दुर्गा देवी (30 वर्ष):\n * चेहरा और शरीर: दिव्य तेज वाली देवी, आठ भुजाओं वाली, हर हाथ में एक अस्त्र, शेर पर सवार।\n * पहनावा: लाल साड़ी, सोने का मुकुट।\n"
                + "2. कार्तिकेय (20 वर्ष):\n * चेहरा: a six-armed god riding a peacock.\n * पहनावा: blue dhoti.\n3. रामू (10 वर्ष):\n * चेहरा: गोल चेहरा, हाथ में लाल पतंग।\n4. शेरू:\n * चेहरा और शरीर: एक बड़ा सुनहरा शेर, जंगल का राजा।\n"
                + "\nदृश्य 1: पहाड़\n(स्थान: पहाड़ की चोटी। सुबह।)\nदुर्गा देवी: \"डरो मत।\"\nकार्तिकेय: \"जय!\"\nरामू: \"वाह!\"\nशेरू: \"गर्र!\"\n\nसमाप्त\n");
        Story.CharacterDef durga = null, kartik = null, ramu = null, sheru = null;
        for (Story.CharacterDef c : st.characters) {
            if (c.displayName.contains("दुर्गा")) durga = c; else if (c.displayName.contains("कार्तिकेय")) kartik = c; else if (c.displayName.contains("रामू")) ramu = c; else if (c.displayName.contains("शेरू")) sheru = c;
        }
        assertNotNull(durga); assertNotNull(kartik); assertNotNull(ramu); assertNotNull(sheru);
        assertTrue("Durga: eight arms, on a lion, a woman: " + durga.look.arms + "/" + durga.look.mount + "/" + durga.look.kind, durga.look.arms == 8 && durga.look.mount == Look.SP_LION && durga.look.female && durga.look.kind != Look.ANIMAL);
        assertTrue("Kartikeya: six arms, on a peacock: " + kartik.look.arms + "/" + kartik.look.mount, kartik.look.arms == 6 && kartik.look.mount == Look.SP_PEACOCK);
        assertTrue("Ramu: two arms, no mount (a kite in his hand is no third arm): " + ramu.look.arms + "/" + ramu.look.mount, ramu.look.arms == 2 && ramu.look.mount < 0);
        assertTrue("Sheru is a lion, not a rider: " + sheru.look.kind + "/" + sheru.look.mount, sheru.look.kind == Look.ANIMAL && sheru.look.mount < 0);
        assertTrue(com.tarun.kahani.core.LookDesigner.armsIn("चार हाथों वाला देवता") == 4 && com.tarun.kahani.core.LookDesigner.mountIn("चूहे की सवारी") == Look.SP_MOUSE
                && com.tarun.kahani.core.LookDesigner.mountIn("mounted on a white bull") == Look.SP_COW && com.tarun.kahani.core.LookDesigner.mountIn("a girl who loves her dog") < 0);
        // 2. the drawn puppet: eight arms are wider at the shoulders than two; a rider is wider than tall at the bottom (the lion)
        int[] shoulderW = new int[2], baseW = new int[2];
        for (int k = 0; k < 2; k++) {
            Look lk = durga.look.copy();
            if (k == 0) { lk.arms = 2; lk.mount = -1; }
            Bitmap b = Bitmap.createBitmap(700, 420, Bitmap.Config.ARGB_8888);
            AndroidGfx g = new AndroidGfx(b, 1);
            g.color(0xFFFFFFFF); g.rect(0, 0, 700, 420);
            Pose p = new Pose(); p.time = 1;
            g.save(); g.translate(350, 410);
            com.tarun.kahani.core.Puppet.draw(g, lk, p, 380);
            g.restore();
            int[] px = new int[700 * 420];
            b.getPixels(px, 0, 700, 0, 0, 700, 420);
            int minS = 700, maxS = 0, minB = 700, maxB = 0;
            for (int y = 0; y < 420; y++) for (int x = 0; x < 700; x++) {
                if ((px[y * 700 + x] & 0xFFFFFF) == 0xFFFFFF) continue;
                if (y > 120 && y < 220) { minS = Math.min(minS, x); maxS = Math.max(maxS, x); }
                if (y > 330) { minB = Math.min(minB, x); maxB = Math.max(maxB, x); }
            }
            shoulderW[k] = maxS - minS; baseW[k] = maxB - minB;
        }
        assertTrue("eight arms fan wider than two: " + shoulderW[0] + " -> " + shoulderW[1], shoulderW[1] > shoulderW[0] * 1.3f);
        assertTrue("the lion under her is wider than her own feet: " + baseW[0] + " -> " + baseW[1], baseW[1] > baseW[0] * 1.5f);
        // 3. the doll: the lion under the goddess, her face high in the picture
        com.tarun.kahani.core.Doll3D.Result r = com.tarun.kahani.core.Doll3D.make(durga.look, 900, 7, 0, com.tarun.kahani.core.Pose.NEUTRAL, null);
        assertTrue("a wide doll (the lion): " + r.w + "x" + r.h, r.w > r.h * 0.8f && r.faceKnown);
        assertTrue("her face in the upper part: " + r.mouthY, r.mouthY < 0.45f);
        // 4. the rig of a rider's picture: arms still, legs off the walk cycle
        int w = 900, h = 700;
        int[] px = new int[w * h];
        java.util.Arrays.fill(px, 0xFFFFFFFF);
        for (int y = 420; y < 640; y++) for (int x = 120; x < 780; x++) { float dx = (x - 450) / 330f, dy = (y - 530) / 110f; if (dx * dx + dy * dy <= 1) px[y * w + x] = 0xFFC9963A; }   // the lion's body
        for (int y = 600; y < 690; y++) for (int x : new int[]{200, 330, 560, 690}) for (int k = -22; k <= 22; k++) px[y * w + x + k] = 0xFFB8862E;                                      // its legs
        for (int y = 250; y < 470; y++) for (int x = 370; x < 530; x++) px[y * w + x] = 0xFFC0282A;                                                                                   // her red sari
        for (int y = 290; y < 330; y++) for (int x = 250; x < 650; x++) px[y * w + x] = 0xFFD9A074;                                                                                   // her arms out
        for (int y = 110; y < 262; y++) for (int x = 380; x < 520; x++) { float dx = (x - 450) / 70f, dy = (y - 186) / 76f; if (dx * dx + dy * dy <= 1) px[y * w + x] = y < 140 ? 0xFF1E1410 : 0xFFD9A074; }   // her head
        for (int e = -1; e <= 1; e += 2) { int ex = 450 + e * 24, ey = 176; for (int y = ey - 9; y <= ey + 9; y++) for (int x = ex - 12; x <= ex + 12; x++) if ((x - ex) * (x - ex) / 144f + (y - ey) * (y - ey) / 81f <= 1) px[y * w + x] = 0xFFFFFFFF;
            for (int y = ey - 5; y <= ey + 5; y++) for (int x = ex - 5; x <= ex + 5; x++) if ((x - ex) * (x - ex) + (y - ey) * (y - ey) <= 25) px[y * w + x] = 0xFF1A1008; }
        for (int y = 214; y <= 219; y++) for (int x = 432; x <= 468; x++) px[y * w + x] = 0xFF9C2A2A;
        com.tarun.kahani.core.Cutout.Result cut = com.tarun.kahani.core.Cutout.process(px, w, h, false);
        Art.Sprite sp = new Art.Sprite();
        sp.faceKnown = cut.faceFound;
        sp.eyeLX = cut.eyeLX; sp.eyeRX = cut.eyeRX; sp.eyeLY = cut.eyeY; sp.eyeRY = cut.eyeY; sp.eyeR = cut.eyeR; sp.mouthX = cut.mouthX; sp.mouthY = cut.mouthY; sp.mouthHW = cut.mouthW / 2;
        Rig rig = Rig.build(cut, sp, durga.look, sampleProject().loader());
        if (rig != null && !rig.animal) {
            assertTrue("a rider's arms never swing", rig.armsFixed);
            assertTrue("a rider's legs never walk", !rig.legs);
        }
        // 5. shorter shots: the cut is 2.2 s and the director's shots keep under it (an establishing shot may hold longer)
        assertTrue("the cut is 2 s", Math.abs(TechnicalDirector.CUT_SECONDS_DEFAULT - 2.0f) < 0.01f);
        TechnicalDirector.CUT_SECONDS = TechnicalDirector.CUT_SECONDS_DEFAULT;
        Project p = sampleProject();
        Story story = ScriptParser.parse(p.read("script.txt"));
        Art art = Art.fromManifest(p.read("cast.txt"), story, p.loader());
        Director d = new Director(story, new Director.Options());
        d.prepare();
        Film film = d.direct(art);
        int under = 0, over = 0; float longest = 0, total = 0;
        for (Film.Shot sh : film.shots) { total += sh.dur; longest = Math.max(longest, sh.dur); if (sh.dur <= TechnicalDirector.CUT_SECONDS + 0.05f) under++; else over++; }
        System.out.println("SHOTS: " + film.shots.size() + ", mean " + (total / Math.max(1, film.shots.size())) + " s, longest " + longest + " s, over the cut " + over);
        assertTrue("most shots under the cut: " + under + " of " + film.shots.size(), under >= film.shots.size() * 0.85f);
        assertTrue("no shot over the four-second cap: " + longest, longest <= TechnicalDirector.MAX_SHOT_SECONDS + 0.05f);
        assertTrue("the mean shot is short: " + (total / film.shots.size()), total / film.shots.size() <= 2.0f);
    }

    /**
     * v34: Durga on her lion, and the lion speaking: the lion is not staged on its own while she is there — its lines
     * move the jaw of the lion in her picture (the rider's lips stay shut), framed wide enough to show it; her own
     * lines move her lips. A character who turns away is drawn from the back. The best of the uploaded pictures
     * (a standing, calm front with a face) becomes the main picture.
     */
    @Test
    public void riderAndAnimalBothSpeakAndBacksAreUsed() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        // 1. the story: Durga rides the lion; the lion speaks; a boy turns away and leaves
        Story st = ScriptParser.parse("पात्र और रूप-रंग (Characters):\n1. दुर्गा देवी (30 वर्ष):\n * चेहरा और शरीर: आठ भुजाओं वाली देवी, शेर पर सवार।\n * पहनावा: लाल साड़ी।\n"
                + "2. शेर:\n * चेहरा और शरीर: एक बड़ा सुनहरा शेर, देवी का वाहन।\n3. रामू (10 वर्ष):\n * चेहरा: गोल चेहरा।\n"
                + "\nदृश्य 1: पहाड़\n(स्थान: पहाड़ की चोटी। सुबह।)\nदुर्गा देवी: \"डरो मत, रामू।\"\nशेर: \"हम तुम्हारी रक्षा करेंगे।\"\nरामू: \"धन्यवाद माँ!\"\n(रामू पीठ फेरकर धीरे-धीरे चला जाता है।)\nदुर्गा देवी: \"जाओ, खुश रहो।\"\n\nसमाप्त\n");
        Story.CharacterDef durga = null, lion = null, ramu = null;
        for (Story.CharacterDef c : st.characters) { if (c.displayName.contains("दुर्गा")) durga = c; else if (c.displayName.equals("शेर")) lion = c; else if (c.displayName.contains("रामू")) ramu = c; }
        assertNotNull(durga); assertNotNull(lion); assertNotNull(ramu);
        assertTrue("the lion is Durga's mount: " + lion.rider + " / " + durga.mountChar, lion.rider == durga && durga.mountChar == lion);
        Director d = new Director(st, new Director.Options());
        d.prepare();
        Film film = d.direct(Art.fromManifest("", st, sampleProject().loader()));
        Film.Actor dA = null, lA = null, rA = null;
        for (Film.Seg sg : film.segs) for (Film.Actor a : sg.actors) { if (a.c == durga && dA == null) dA = a; if (a.c == lion) lA = a; if (a.c == ramu && rA == null) rA = a; }
        assertNotNull("Durga is staged", dA);
        assertTrue("the lion is not staged on its own beside her", lA == null);
        int own = 0, mount = 0;
        for (Film.Speak sk : dA.speaks) if (sk.mount) mount++; else own++;
        assertTrue("Durga speaks with her lips twice and the lion with its jaw once: " + own + "/" + mount, own == 2 && mount == 1);
        // the renderer: the lion's line moves the mount's mouth, never Durga's lips; her lines move her lips
        Renderer rd = new Renderer(film, null);
        Film.Speak lionLine = null, herLine = null;
        for (Film.Speak sk : dA.speaks) { if (sk.mount) lionLine = sk; else if (herLine == null) herLine = sk; }
        float maxMount = 0;
        Method mouthAt = Renderer.class.getDeclaredMethod("mouthAt", Film.Actor.class, float.class);
        mouthAt.setAccessible(true);
        for (float t = lionLine.t0 + 0.05f; t < lionLine.t1; t += 0.05f) maxMount = Math.max(maxMount, (Float) mouthAt.invoke(rd, dA, t));
        assertTrue("the lion's line has a mouth to move: " + maxMount, maxMount > 0.1f);
        // the shot of the lion's line shows the whole picture (not her face alone)
        Film.Shot ls = null;
        for (Film.Shot sh : film.shots) if (sh.t <= lionLine.t0 + 0.1f && sh.t + sh.dur > lionLine.t0 + 0.1f) ls = sh;
        assertNotNull("a shot for the lion's line", ls);
        assertTrue("the lion's line is framed wide: size " + ls.size, ls.size <= com.tarun.kahani.core.ShotPlanner.MEDIUM);
        // 2. the rig of a rider's picture finds the lion's jaw on the lion's side
        int w = 900, h = 700;
        int[] px = new int[w * h];
        java.util.Arrays.fill(px, 0xFFFFFFFF);
        for (int y = 420; y < 640; y++) for (int x = 120; x < 700; x++) { float dx = (x - 410) / 290f, dy = (y - 530) / 110f; if (dx * dx + dy * dy <= 1) px[y * w + x] = 0xFFC9963A; }   // the body
        for (int y = 380; y < 560; y++) for (int x = 640; x < 860; x++) { float dx = (x - 750) / 110f, dy = (y - 470) / 90f; if (dx * dx + dy * dy <= 1) px[y * w + x] = 0xFFB8862E; }    // its head, right
        for (int y = 600; y < 690; y++) for (int x : new int[]{200, 330, 500, 620}) for (int k = -22; k <= 22; k++) px[y * w + x + k] = 0xFFB8862E;
        for (int y = 250; y < 470; y++) for (int x = 340; x < 480; x++) px[y * w + x] = 0xFFC0282A;
        for (int y = 110; y < 262; y++) for (int x = 340; x < 480; x++) { float dx = (x - 410) / 70f, dy = (y - 186) / 76f; if (dx * dx + dy * dy <= 1) px[y * w + x] = y < 140 ? 0xFF1E1410 : 0xFFD9A074; }
        for (int e = -1; e <= 1; e += 2) { int ex = 410 + e * 24, ey = 176; for (int y = ey - 9; y <= ey + 9; y++) for (int x = ex - 12; x <= ex + 12; x++) if ((x - ex) * (x - ex) / 144f + (y - ey) * (y - ey) / 81f <= 1) px[y * w + x] = 0xFFFFFFFF;
            for (int y = ey - 5; y <= ey + 5; y++) for (int x = ex - 5; x <= ex + 5; x++) if ((x - ex) * (x - ex) + (y - ey) * (y - ey) <= 25) px[y * w + x] = 0xFF1A1008; }
        for (int y = 214; y <= 219; y++) for (int x = 392; x <= 428; x++) px[y * w + x] = 0xFF9C2A2A;
        com.tarun.kahani.core.Cutout.Result cut = com.tarun.kahani.core.Cutout.process(px, w, h, false);
        Art.Sprite sp = new Art.Sprite();
        sp.faceKnown = cut.faceFound;
        sp.eyeLX = cut.eyeLX; sp.eyeRX = cut.eyeRX; sp.eyeLY = cut.eyeY; sp.eyeRY = cut.eyeY; sp.eyeR = cut.eyeR; sp.mouthX = cut.mouthX; sp.mouthY = cut.mouthY; sp.mouthHW = cut.mouthW / 2;
        Rig rig = Rig.build(cut, sp, durga.look, sampleProject().loader());
        assertNotNull("a rig for the rider's picture", rig);
        if (!rig.animal) {
            assertTrue("the lion's jaw is found", rig.mountJaw);
            assertTrue("on the lion's side (right): " + rig.mjSide + " tip " + rig.mjTipX, rig.mjSide > 0 && rig.mjTipX > 0.85f);
        }
        // 3. the boy turns away and leaves: his back is to the camera, and the casting wants his back picture
        float back = -1;
        for (Film.Key k : rA.keys) if (k.backTurned) { back = k.t; break; }
        assertTrue("Ramu turns his back", back >= 0);
        Film.Seg seg = film.segAt(back + 0.3f);
        Film.Shot bs = null;
        for (Film.Shot sh : film.shots) if (sh.t <= back + 0.3f && sh.t + sh.dur > back + 0.3f) bs = sh;
        if (bs != null) assertTrue("the casting wants his back", Math.abs(com.tarun.kahani.core.Casting.want(film, seg, bs, rA, back + 0.3f).angle - com.tarun.kahani.core.Angles.BACK) < 1);
        // 4. the best of the uploads becomes the main picture: sheet 15 (the kurta girl: crying, laughing, angry, sitting…)
        Project p = sampleProject();
        Story sst = ScriptParser.parse(p.read("script.txt"));
        Story.CharacterDef vr = null;
        for (Story.CharacterDef c : sst.cast()) if (c.displayName.contains("वृंदा")) vr = c;
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        Field lf = MainActivity.class.getDeclaredField("library");
        lf.setAccessible(true);
        com.tarun.kahani.app.Library lib = (com.tarun.kahani.app.Library) lf.get(ac.get());
        String key = (String) s3d("keyFor", p, sst, vr);
        File sheet = new File(userSheets()[0].getParentFile(), "sheet15.jpg");
        List<byte[]> one = new ArrayList<byte[]>(); one.add(Files.readAllBytes(sheet.toPath()));
        call("com.tarun.kahani.app.SheetSaver", "save", p, lib, sst, "angles:char:" + key + ":" + vr.shown(), one, null);
        String mainFile = p.manifestLine("char", key).split("\\|")[2];
        byte[] mainBytes = Files.readAllBytes(p.file(mainFile).toPath());
        String[] mainTag = null;
        for (String l : p.read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length >= 6 && f[0].equals("pose") && f[1].equals(key) && p.has(f[2]) && java.util.Arrays.equals(Files.readAllBytes(p.file(f[2]).toPath()), mainBytes)) mainTag = f;
        }
        assertNotNull("the main picture is one of the sheet's figures", mainTag);
        System.out.println("MAIN PICTURE of sheet 15: angle " + mainTag[3] + " pose " + mainTag[4] + " emotion " + mainTag[5] + "; lion line framed " + ls.size + ", back at " + back);
        assertTrue("the main picture faces front: " + mainTag[3], Math.abs(Float.parseFloat(mainTag[3])) < 1);
        assertTrue("the main picture stands: " + mainTag[4], Integer.parseInt(mainTag[4]) == com.tarun.kahani.core.PoseSense.STAND);
        int em = Integer.parseInt(mainTag[5]);
        assertTrue("the main picture is calm or happy: " + em, em == com.tarun.kahani.core.PoseSense.NEUTRAL || em == com.tarun.kahani.core.PoseSense.HAPPY);
        ac.pause().stop().destroy();
    }

    /** Every visible view with a click action, in screen order. */
    static List<View> clickables(View v, List<View> out) {
        if (v.getVisibility() != View.VISIBLE) return out;
        if (v.hasOnClickListeners()) out.add(v);
        if (v instanceof ViewGroup) for (int i = 0; i < ((ViewGroup) v).getChildCount(); i++) clickables(((ViewGroup) v).getChildAt(i), out);
        return out;
    }

    static String labelOf(View v) {
        if (v instanceof TextView) return ((TextView) v).getText().toString().replace('\n', ' ');
        return v.getContentDescription() == null ? v.getClass().getSimpleName() : v.getContentDescription().toString();
    }

    /**
     * v34: every button of every screen (Home, the story, the Studio, the lines, the library, settings), tapped one by
     * one on a fresh screen, with the first choice of any list it opens tapped too: none may crash. Buttons that start
     * a whole film, a 3D build, recording, or delete things are left out (other tests run those paths).
     */
    @Test
    public void everyButtonOnEveryScreenWorks() throws Exception {
        android.content.Context ctx = RuntimeEnvironment.getApplication();
        ctx.getSharedPreferences("kahani", 0).edit().putString("online", "0").putString("skipLogin", "1").commit();
        Project p = sampleProject();
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        String[] screens = {"showHome", "showStory", "showStudio", "showLines", "showLibrary", "showSettings"};
        String[] deny = {"Make the film", "🎬 Make", "Make film", "Delete", "Clear", "✖", "Log out", "Logout", "Sign out", "Exit", "🧊", "Build all", "Record", "🎙  Record",
                "Remove", "Reset", "Forget", "Uninstall", "Make the views", "📐 Make", "Restore", "Approve", "■", "Stop"};
        StringBuilder rep = new StringBuilder();
        List<String> failures = new ArrayList<String>();
        int clicked = 0, skipped = 0;
        for (String screen : screens) {
            set(a, "project", p);
            call(a, screen, new Class<?>[0]);
            idle();
            int n = clickables(a.findViewById(android.R.id.content), new ArrayList<View>()).size();
            int here = 0;
            for (int i = 0; i < n; i++) {
                try {
                    for (android.app.Dialog dl : org.robolectric.shadows.ShadowDialog.getShownDialogs()) if (dl.isShowing()) dl.dismiss();
                    set(a, "project", p);
                    call(a, screen, new Class<?>[0]);
                    idle();
                } catch (Throwable e) { failures.add(screen + " could not be shown again: " + e); break; }
                List<View> cl = clickables(a.findViewById(android.R.id.content), new ArrayList<View>());
                if (i >= cl.size()) break;
                View v = cl.get(i);
                String label = labelOf(v);
                boolean no = false;
                for (String dny : deny) if (label.contains(dny)) no = true;
                if (no) { skipped++; continue; }
                android.app.Dialog before = org.robolectric.shadows.ShadowDialog.getLatestDialog();
                try {
                    v.performClick();
                    idle();
                    while (shadowOf(a).getNextStartedActivity() != null) { /* a picker or a share sheet opened: fine */ }
                    android.app.AlertDialog dl = org.robolectric.shadows.ShadowAlertDialog.getLatestAlertDialog();
                    if (dl != null && dl != before && dl.isShowing() && dl.getListView() != null && dl.getListView().getCount() > 0) {
                        String first = String.valueOf(dl.getListView().getItemAtPosition(0));
                        boolean bad = false;
                        for (String dny : deny) if (first.contains(dny)) bad = true;
                        if (!bad) { dl.getListView().performItemClick(dl.getListView(), 0, 0); idle(); while (shadowOf(a).getNextStartedActivity() != null) { } }
                    }
                    clicked++; here++;
                } catch (Throwable e) {
                    Throwable c = e instanceof java.lang.reflect.InvocationTargetException && e.getCause() != null ? e.getCause() : e;
                    failures.add(screen + " / \"" + label + "\": " + c);
                }
            }
            rep.append(screen).append(": ").append(here).append(" of ").append(n).append(" tapped\n");
        }
        for (android.app.Dialog dl : org.robolectric.shadows.ShadowDialog.getShownDialogs()) if (dl.isShowing()) dl.dismiss();
        System.out.println("BUTTONS: " + clicked + " tapped, " + skipped + " left out, " + failures.size() + " failed\n" + rep + (failures.isEmpty() ? "" : "FAILED:\n" + String.join("\n", failures)));
        assertTrue("buttons that crashed:\n" + String.join("\n", failures), failures.isEmpty());
        assertTrue("most buttons tapped: " + clicked, clicked >= 40);
        ac.pause().stop().destroy();
    }
}
