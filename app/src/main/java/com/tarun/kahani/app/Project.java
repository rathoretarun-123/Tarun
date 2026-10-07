package com.tarun.kahani.app;

import android.content.Context;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

import com.tarun.kahani.core.Art;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

/**
 * One film project stored in its own folder:
 * script.txt, cast.txt (picture manifest), pictures, settings.txt and film.mp4.
 */
public final class Project {
    public final File dir;

    public Project(File dir) { this.dir = dir; }

    public static File root(Context c) {
        File r = new File(c.getFilesDir(), "projects");
        if (!r.exists()) r.mkdirs();
        return r;
    }

    public static List<Project> all(Context c) {
        File[] fs = root(c).listFiles();
        List<Project> out = new ArrayList<Project>();
        if (fs == null) return out;
        Arrays.sort(fs, new Comparator<File>() {
            public int compare(File a, File b) { return Long.compare(b.lastModified(), a.lastModified()); }
        });
        for (File f : fs) if (f.isDirectory()) out.add(new Project(f));
        return out;
    }

    public static Project create(Context c) {
        File d = new File(root(c), "p" + System.currentTimeMillis());
        d.mkdirs();
        return new Project(d);
    }

    /** Copies the bundled sample story and its pictures into a new project. */
    public static Project createSample(Context c) throws IOException {
        Project p = create(c);
        AssetManager am = c.getAssets();
        p.write("script.txt", readAsset(am, "sample_story.txt"));
        String[] files = am.list("sample");
        if (files != null) {
            for (String f : files) {
                InputStream in = am.open("sample/" + f);
                copy(in, new FileOutputStream(new File(p.dir, f)));
            }
        }
        p.setSetting("name", "रत्नगढ़ की दो राजकुमारियाँ");
        return p;
    }

    public String read(String name) {
        File f = new File(dir, name);
        if (!f.exists()) return "";
        try {
            return new String(readAll(new FileInputStream(f)), "UTF-8");
        } catch (IOException e) {
            return "";
        }
    }

    public void write(String name, String text) {
        try {
            OutputStream o = new FileOutputStream(new File(dir, name));
            o.write(text.getBytes("UTF-8"));
            o.close();
            dir.setLastModified(System.currentTimeMillis());
        } catch (IOException ignored) {
        }
    }

    public File file(String name) { return new File(dir, name); }
    public boolean has(String name) { return new File(dir, name).exists(); }
    public File film() { return new File(dir, "film.mp4"); }

    // -------------------------------------------------------------- settings (key=value lines)

    public String setting(String key, String def) {
        for (String l : read("settings.txt").split("\n")) {
            int i = l.indexOf('=');
            if (i > 0 && l.substring(0, i).equals(key)) return l.substring(i + 1);
        }
        return def;
    }

    public void setSetting(String key, String value) {
        StringBuilder sb = new StringBuilder();
        boolean done = false;
        for (String l : read("settings.txt").split("\n")) {
            if (l.trim().length() == 0) continue;
            int i = l.indexOf('=');
            if (i > 0 && l.substring(0, i).equals(key)) { sb.append(key).append('=').append(value).append('\n'); done = true; }
            else sb.append(l).append('\n');
        }
        if (!done) sb.append(key).append('=').append(value).append('\n');
        write("settings.txt", sb.toString());
    }

    public String name() {
        String n = setting("name", "");
        if (n.length() > 0) return n;
        String s = read("script.txt").trim();
        if (s.length() == 0) return "नई फ़िल्म";
        try {
            String t = com.tarun.kahani.core.ScriptParser.parse(s).title;
            if (t.length() > 0) return t;
        } catch (RuntimeException ignored) {
        }
        return "नई फ़िल्म";
    }

    // -------------------------------------------------------------- manifest (cast.txt)

    /** Replaces (or removes, when line == null) the manifest line whose first two fields match. */
    public void setManifest(String kind, String key, String line) {
        StringBuilder sb = new StringBuilder();
        for (String l : read("cast.txt").split("\n")) {
            if (l.trim().length() == 0) continue;
            String[] f = l.split("\\|");
            boolean match = f.length >= 2 && f[0].equals(kind) && (kind.equals("title") || kind.equals("end") || f[1].equals(key));
            if (!match) sb.append(l).append('\n');
        }
        if (line != null) sb.append(line).append('\n');
        write("cast.txt", sb.toString());
    }

    public String manifestLine(String kind, String key) {
        for (String l : read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length >= 2 && f[0].equals(kind) && (kind.equals("title") || kind.equals("end") || f[1].equals(key))) return l;
        }
        return null;
    }

    /** Saves a picture chosen by the user, downscaled so it never wastes memory. */
    public String savePicture(Context c, android.net.Uri uri, String base) throws IOException {
        InputStream in = c.getContentResolver().openInputStream(uri);
        if (in == null) throw new IOException("चित्र नहीं खुला");
        return savePicture(readAll(in), base);
    }

    /** Saves picture bytes (any format Android can read) into the project, downscaled. Returns the file name. */
    public String savePicture(byte[] data, String base) throws IOException {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        if (o.outWidth <= 0) throw new IOException("यह चित्र पढ़ा नहीं जा सका");
        int sample = 1;
        while (Math.max(o.outWidth, o.outHeight) / sample > 2600) sample *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = sample;
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, o);
        if (b == null) throw new IOException("यह चित्र पढ़ा नहीं जा सका");
        b = upright(b, data);
        boolean png = b.hasAlpha();
        String name = base + "_" + (System.nanoTime() % 100000000L) + (png ? ".png" : ".jpg");
        OutputStream out = new FileOutputStream(new File(dir, name));
        b.compress(png ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 92, out);
        out.close();
        b.recycle();
        return name;
    }

    /** Phone cameras often store portrait photos sideways with a rotation tag; turn the picture upright. */
    public static Bitmap upright(Bitmap b, byte[] data) {
        int deg = 0;
        boolean flip = false;
        try {
            android.media.ExifInterface ex = new android.media.ExifInterface(new java.io.ByteArrayInputStream(data));
            switch (ex.getAttributeInt(android.media.ExifInterface.TAG_ORIENTATION, 1)) {
                case 2: flip = true; break;
                case 3: deg = 180; break;
                case 4: deg = 180; flip = true; break;
                case 5: deg = 90; flip = true; break;
                case 6: deg = 90; break;
                case 7: deg = 270; flip = true; break;
                case 8: deg = 270; break;
                default:
            }
        } catch (Throwable ignored) {
        }
        if (deg == 0 && !flip) return b;
        android.graphics.Matrix m = new android.graphics.Matrix();
        if (flip) m.postScale(-1, 1);
        m.postRotate(deg);
        try {
            Bitmap r = Bitmap.createBitmap(b, 0, 0, b.getWidth(), b.getHeight(), m, true);
            if (r != b) b.recycle();
            return r;
        } catch (OutOfMemoryError e) {
            return b;
        }
    }

    public void delete() {
        File[] fs = dir.listFiles();
        if (fs != null) for (File f : fs) f.delete();
        dir.delete();
    }

    // -------------------------------------------------------------- loading pictures

    /** Loads pictures from the project folder for the core engine. */
    public Art.Loader loader() {
        return new Art.Loader() {
            Bitmap decodeBmp(String name, int maxSide) {
                File f = new File(dir, name);
                if (!f.exists()) return null;
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeFile(f.getAbsolutePath(), o);
                if (o.outWidth <= 0) return null;
                int sample = 1;
                while (Math.max(o.outWidth, o.outHeight) / (sample * 2) >= maxSide) sample *= 2;
                o = new BitmapFactory.Options();
                o.inSampleSize = sample;
                o.inPreferredConfig = Bitmap.Config.ARGB_8888;
                Bitmap b = BitmapFactory.decodeFile(f.getAbsolutePath(), o);
                if (b == null) return null;
                float s = Math.min(1f, maxSide / (float) Math.max(b.getWidth(), b.getHeight()));
                if (s < 0.999f) {
                    Bitmap r = Bitmap.createScaledBitmap(b, Math.max(1, Math.round(b.getWidth() * s)), Math.max(1, Math.round(b.getHeight() * s)), true);
                    if (r != b) b.recycle();
                    b = r;
                }
                return b;
            }

            public int[] decode(String name, int maxSide) {
                Bitmap b = decodeBmp(name, maxSide);
                if (b == null) return null;
                int w = b.getWidth(), h = b.getHeight();
                int[] out = new int[w * h + 2];
                out[0] = w; out[1] = h;
                b.getPixels(out, 2, w, 0, 0, w, h);
                b.recycle();
                return out;
            }

            public Object create(int[] argb, int w, int h) {
                Bitmap b = Bitmap.createBitmap(argb, w, h, Bitmap.Config.ARGB_8888);
                if (!b.hasAlpha()) b.setHasAlpha(true);
                return b;
            }

            public Object load(String name, int maxSide) { return decodeBmp(name, maxSide); }
            public int width(Object img) { return ((Bitmap) img).getWidth(); }
            public int height(Object img) { return ((Bitmap) img).getHeight(); }
        };
    }

    // -------------------------------------------------------------- io helpers

    static String readAsset(AssetManager am, String name) throws IOException {
        return new String(readAll(am.open(name)), "UTF-8");
    }

    static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream b = new ByteArrayOutputStream();
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) b.write(buf, 0, n);
        in.close();
        return b.toByteArray();
    }

    static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[65536];
        int n;
        while ((n = in.read(buf)) > 0) out.write(buf, 0, n);
        in.close();
        out.close();
    }
}
