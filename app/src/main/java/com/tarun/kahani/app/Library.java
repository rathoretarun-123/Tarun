package com.tarun.kahani.app;

import android.content.Context;

import com.tarun.kahani.core.Json;
import com.tarun.kahani.core.SoundLib;
import com.tarun.kahani.core.Txt;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The studio library: everything the user ever uploaded, recorded, downloaded or generated (pictures, voice
 * samples, sounds), plus the pictures and sounds that come with the app. Saved in the app's private storage and
 * reusable in every film.
 */
public final class Library {
    public static final String PIC = "pic", VOICE = "voice", SOUND = "sound";

    public static final class Item {
        public String id, type, kind = "", name = "", tags = "", path = "", source = "";
        public boolean builtIn;
        public String label() { return name.length() > 0 ? name : new File(path).getName(); }
    }

    private final Context ctx;
    private final File dir, index;
    public final List<Item> items = new ArrayList<Item>();

    public Library(Context c) {
        ctx = c.getApplicationContext();
        dir = new File(ctx.getFilesDir(), "library");
        dir.mkdirs();
        index = new File(dir, "index.json");
        load();
        addBuiltIns();
    }

    public File dir() { return dir; }

    private void load() {
        if (!index.exists()) return;
        try {
            Object o = Json.parseLoose(new String(AudioIO.readFile(index), "UTF-8"));
            if (!(o instanceof List)) return;
            for (Object x : (List<?>) o) {
                Item it = new Item();
                it.id = Json.str(x, "id", ""); it.type = Json.str(x, "type", PIC); it.kind = Json.str(x, "kind", "");
                it.name = Json.str(x, "name", ""); it.tags = Json.str(x, "tags", ""); it.path = Json.str(x, "path", "");
                it.source = Json.str(x, "source", "");
                if (new File(it.path).exists()) items.add(it);
            }
        } catch (Exception ignored) {
        }
    }

    public synchronized void save() {
        List<Object> arr = new ArrayList<Object>();
        for (Item it : items) {
            if (it.builtIn) continue;
            Map<String, Object> m = new LinkedHashMap<String, Object>();
            m.put("id", it.id); m.put("type", it.type); m.put("kind", it.kind); m.put("name", it.name);
            m.put("tags", it.tags); m.put("path", it.path); m.put("source", it.source);
            arr.add(m);
        }
        try {
            File tmp = new File(dir, "index.tmp");
            FileOutputStream o = new FileOutputStream(tmp);
            o.write(Json.write(arr).getBytes("UTF-8"));
            o.close();
            tmp.renameTo(index);
        } catch (IOException ignored) {
        }
    }

    /** Pictures and sounds bundled with the app. */
    private void addBuiltIns() {
        try {
            String[] pics = ctx.getAssets().list("sample");
            if (pics != null) for (String f : pics) {
                if (!(f.endsWith(".jpg") || f.endsWith(".png"))) continue;
                Item it = new Item();
                it.id = "asset:sample/" + f; it.type = PIC; it.builtIn = true; it.path = "asset:sample/" + f; it.source = "app";
                String base = f.replaceAll("\\.(jpg|png)$", "");
                it.kind = base.startsWith("char_") ? "person" : base.startsWith("bg_") ? "place" : base.startsWith("shot_") ? "shot" : base;
                it.name = base.replace("char_", "").replace("bg_", "").replace("shot_", "").replace('_', ' ');
                it.tags = it.name;
                items.add(it);
            }
            String json = Project.readAsset(ctx.getAssets(), "sounds/index.json");
            Object o = Json.parseLoose(json);
            if (o instanceof List) for (Object x : (List<?>) o) {
                Item it = new Item();
                it.path = "asset:sounds/" + Json.str(x, "file", ""); it.id = it.path; it.type = SOUND; it.builtIn = true;
                it.kind = Json.str(x, "type", "sfx"); it.name = Json.str(x, "title", ""); it.tags = Json.str(x, "words", ""); it.source = "app";
                items.add(it);
            }
        } catch (IOException ignored) {
        }
    }

    public synchronized Item add(String type, String kind, String name, String tags, File src, String ext, String source) throws IOException {
        Item it = new Item();
        it.id = type + "_" + System.currentTimeMillis() + "_" + (int) (Math.random() * 1000);
        it.type = type; it.kind = kind == null ? "" : kind; it.name = name == null ? "" : name; it.tags = tags == null ? "" : tags;
        it.source = source == null ? "" : source;
        File sub = new File(dir, type);
        sub.mkdirs();
        File dst = new File(sub, it.id + ext);
        if (!src.renameTo(dst)) {
            Project.copy(new java.io.FileInputStream(src), new FileOutputStream(dst));
            src.delete();
        }
        it.path = dst.getAbsolutePath();
        items.add(0, it);
        save();
        return it;
    }

    public synchronized Item addBytes(String type, String kind, String name, String tags, byte[] data, String ext, String source) throws IOException {
        File tmp = new File(dir, "incoming" + ext);
        FileOutputStream o = new FileOutputStream(tmp);
        o.write(data);
        o.close();
        return add(type, kind, name, tags, tmp, ext, source);
    }

    public synchronized void remove(Item it) {
        if (it.builtIn) return;
        items.remove(it);
        new File(it.path).delete();
        save();
    }

    public synchronized void rename(Item it, String name) {
        it.name = name;
        save();
    }

    /** Items of a type (and kind, if given), best matches for the query first. */
    public synchronized List<Item> find(String type, String kind, String query) {
        List<Item> out = new ArrayList<Item>();
        final String q = query == null ? "" : Txt.norm(query);
        for (Item it : items) {
            if (!it.type.equals(type)) continue;
            if (kind != null && kind.length() > 0 && !kind.equals(it.kind)) continue;
            out.add(it);
        }
        if (q.length() > 0) {
            java.util.Collections.sort(out, new java.util.Comparator<Item>() {
                public int compare(Item a, Item b) { return score(b, q) - score(a, q); }
            });
        }
        return out;
    }

    static int score(Item it, String q) {
        String t = Txt.norm(it.name + " " + it.tags + " " + it.kind);
        int s = it.builtIn ? 0 : 1;
        for (String w : q.split("\\s+")) if (w.length() >= 2 && t.contains(w)) s += 10 + w.length();
        for (String w : t.split("[\\s,]+")) if (w.length() >= 3 && q.contains(w)) s += 5;
        return s;
    }

    public Item byId(String id) {
        if (id == null) return null;
        for (Item it : items) if (it.id.equals(id)) return it;
        return null;
    }

    /** All sounds (built-in + user's) for the mixer. User sounds get the user's tags as keywords. */
    public SoundLib soundLib() {
        SoundLib lib = new SoundLib(AudioIO.decoder(ctx));
        try { lib.addIndex(Project.readAsset(ctx.getAssets(), "sounds/index.json"), "asset:sounds/"); } catch (IOException ignored) {}
        for (Item it : items) {
            if (it.builtIn || !it.type.equals(SOUND)) continue;
            SoundLib.Entry e = new SoundLib.Entry();
            e.path = it.path;
            e.type = it.kind.length() > 0 ? it.kind : "sfx";
            e.title = it.label();
            e.words = SoundLib.splitWords(it.name + "," + it.tags);
            lib.entries.add(e);
        }
        return lib;
    }

    /** Opens a library picture as a stream (assets or files). */
    public InputStream open(Item it) throws IOException {
        if (it.path.startsWith("asset:")) return ctx.getAssets().open(it.path.substring(6));
        return new java.io.FileInputStream(it.path);
    }
}
