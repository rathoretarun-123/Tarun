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

    @Test
    public void dashboardEditorCastAndFaceScreensOpenWithoutCrashing() throws Exception {
        ActivityController<MainActivity> ac = Robolectric.buildActivity(MainActivity.class).setup();
        MainActivity a = ac.get();
        View root = a.getWindow().getDecorView();
        List<String> home = texts(root, new ArrayList<String>());
        System.out.println("HOME: " + home);
        assertTrue(home.toString().contains("नई फ़िल्म बनाएँ"));

        // new empty film -> editor -> back
        find(root, "नई फ़िल्म बनाएँ").performClick();
        idle();
        List<String> ed = texts(root, new ArrayList<String>());
        System.out.println("EDITOR: " + ed);
        assertTrue(ed.toString().contains("1. कहानी"));
        a.onBackPressed();
        idle();

        // sample project -> editor -> director's check -> face screen
        Project p = sampleProject();
        set(a, "project", p);
        call(a, "showEditor", new Class<?>[0]);
        idle();
        assertTrue(texts(root, new ArrayList<String>()).toString().contains("शुरुआत का चित्र"));
        find(root, "निर्देशक से जाँच").performClick();
        idle();
        List<String> cast = texts(root, new ArrayList<String>());
        System.out.println("CAST: " + cast);
        assertTrue(cast.toString().contains("वानुषा"));
        assertTrue(cast.toString().contains("निर्देशक की योजना"));
        call(a, "showFace", new Class<?>[]{String.class}, "वृंदा");
        Thread.sleep(3000);
        idle();
        System.out.println("FACE: " + texts(root, new ArrayList<String>()));
        a.onBackPressed();
        idle();
        // progress screen with no job and player with no film must not crash
        call(a, "showPlayer", new Class<?>[0]);
        idle();
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
        p.setSetting("quality", "360");
        com.tarun.kahani.app.FilmJob job = new com.tarun.kahani.app.FilmJob(RuntimeEnvironment.getApplication(), p);
        long t0 = System.currentTimeMillis();
        job.run();
        System.out.println("JOB: done=" + job.done + " failed=" + job.failed + " err=" + job.error + " warn=" + job.warning
                + " stage=" + job.stage + " secs=" + job.filmSeconds + " took=" + (System.currentTimeMillis() - t0) + "ms");
        assertTrue("job failed: " + job.error, job.done);
        assertTrue(p.film().exists());
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
