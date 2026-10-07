package com.tarun.kahani;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

import android.graphics.Bitmap;
import android.os.Looper;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.tarun.kahani.app.AndroidGfx;
import com.tarun.kahani.app.MainActivity;
import com.tarun.kahani.app.Project;
import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Renderer;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Story;

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

        for (String t : home) assertTrue("Hindi left in the home screen: " + t, !hasDevanagari(t) || t.contains("Ratnagarh") || t.contains("रत्नगढ़"));
        // new empty film -> story screen (clear button, platforms) -> back
        find(root, "New film").performClick();
        idle();
        List<String> ed = texts(root, new ArrayList<String>());
        System.out.println("STORY: " + ed);
        for (String t : ed) assertTrue("Hindi left in the story screen: " + t, !hasDevanagari(t));
        assertTrue(ed.toString().contains("1. Story"));
        assertTrue(ed.toString().contains("Clear"));
        assertTrue(ed.toString().contains("Instagram Reel"));
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
        assertTrue(st.toString().contains("Production file"));
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
        com.tarun.kahani.app.FilmJob job = new com.tarun.kahani.app.FilmJob(RuntimeEnvironment.getApplication(), p);
        long t0 = System.currentTimeMillis();
        job.run();
        System.out.println("JOB: done=" + job.done + " failed=" + job.failed + " err=" + job.error + " warn=" + job.warning
                + " stage=" + job.stage + " secs=" + job.filmSeconds + " took=" + (System.currentTimeMillis() - t0) + "ms");
        assertTrue("job failed: " + job.error, job.done);
        assertTrue(p.film().exists());
        assertTrue("recorded line not used: " + job.voicedLines, job.voicedLines == 1);
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
}
