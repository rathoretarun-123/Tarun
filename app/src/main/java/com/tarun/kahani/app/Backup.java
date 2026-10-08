package com.tarun.kahani.app;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import com.tarun.kahani.core.Json;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * Keeps a second, permanent copy of everything the user puts in the library (photos, voices, sounds) in the
 * phone's Downloads/KahaniFilm/Library folder. That copy survives closing, updating and even uninstalling the
 * app, and the library can be restored from it.
 */
final class Backup {
    private Backup() {}

    static final String FOLDER = "KahaniFilm/Library";
    /** Added to backed-up files so galleries and music players leave them alone. */
    static final String PRIVATE = ".kfbak";
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    /** Copies the item's file and its description in the background (never slows the app down). */
    static void copy(final Context ctx, final Library.Item it) {
        if (it == null || it.builtIn) return;
        final Context app = ctx.getApplicationContext();
        IO.execute(new Runnable() {
            public void run() {
                try {
                    File f = new File(it.path);
                    // kept private: saved as ".kfbak" (not a picture or song to the phone), so the photos and voices
                    // of the library never show up in the gallery or the music player
                    write(app, f.getName() + PRIVATE, new FileInputStream(f), "application/octet-stream");
                    write(app, f.getName() + ".json", new java.io.ByteArrayInputStream(Library.describe(it).getBytes("UTF-8")), "application/json");
                } catch (Throwable ignored) {
                    // the library inside the app is still complete; the backup is an extra
                }
            }
        });
    }

    /**
     * Older versions backed up the library as ordinary pictures and sounds, which galleries show. This moves
     * those copies (the app's own files only) to the private form once, in the background.
     */
    static void privatizeOld(final Context ctx) {
        if (Build.VERSION.SDK_INT < 29) return;
        final Context app = ctx.getApplicationContext();
        IO.execute(new Runnable() {
            public void run() {
                try {
                    ContentResolver cr = app.getContentResolver();
                    Uri base = MediaStore.Downloads.EXTERNAL_CONTENT_URI;
                    Cursor c = cr.query(base, new String[]{MediaStore.MediaColumns._ID, MediaStore.MediaColumns.DISPLAY_NAME, MediaStore.MediaColumns.MIME_TYPE},
                            MediaStore.MediaColumns.RELATIVE_PATH + " LIKE ?", new String[]{Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER + "%"}, null);
                    if (c == null) return;
                    List<Object[]> old = new ArrayList<Object[]>();
                    try {
                        while (c.moveToNext()) {
                            String name = c.getString(1), mime = c.getString(2);
                            if (name == null || name.endsWith(PRIVATE) || name.endsWith(".json")) continue;
                            if (mime == null || !(mime.startsWith("image/") || mime.startsWith("audio/"))) continue;
                            old.add(new Object[]{c.getLong(0), name});
                        }
                    } finally {
                        c.close();
                    }
                    for (Object[] o : old) {
                        Uri u = android.content.ContentUris.withAppendedId(base, (Long) o[0]);
                        write(app, o[1] + PRIVATE, cr.openInputStream(u), "application/octet-stream");
                        cr.delete(u, null, null);
                    }
                } catch (Throwable ignored) {
                    // not ours to move, or no access: the copies simply stay where they are
                }
            }
        });
    }

    static String mime(String n) {
        n = n.toLowerCase();
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".jpg") || n.endsWith(".jpeg")) return "image/jpeg";
        if (n.endsWith(".wav")) return "audio/wav";
        if (n.endsWith(".mp3")) return "audio/mpeg";
        if (n.endsWith(".ogg")) return "audio/ogg";
        if (n.endsWith(".m4a")) return "audio/mp4";
        return "application/octet-stream";
    }

    private static void write(Context c, String name, InputStream in, String mime) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
            v.put(MediaStore.MediaColumns.MIME_TYPE, mime);
            v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS + "/" + FOLDER);
            Uri uri = c.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
            if (uri == null) { in.close(); return; }
            OutputStream o = c.getContentResolver().openOutputStream(uri);
            Project.copy(in, o);
        } else {
            if (c.checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE) != android.content.pm.PackageManager.PERMISSION_GRANTED) {
                in.close();
                return;
            }
            File d = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS), FOLDER);
            d.mkdirs();
            try { new File(d, ".nomedia").createNewFile(); } catch (Exception ignored) {}
            Project.copy(in, new java.io.FileOutputStream(new File(d, name)));
        }
    }

    /**
     * Brings back library items from a backup folder the user picked (Downloads/KahaniFilm/Library).
     * Items already in the library are skipped. Returns how many were restored.
     */
    static int restore(Context c, Uri tree, Library lib) throws Exception {
        ContentResolver cr = c.getContentResolver();
        Uri children = DocumentsContract.buildChildDocumentsUriUsingTree(tree, DocumentsContract.getTreeDocumentId(tree));
        Map<String, Uri> files = new HashMap<String, Uri>();
        Cursor cur = cr.query(children, new String[]{DocumentsContract.Document.COLUMN_DOCUMENT_ID, DocumentsContract.Document.COLUMN_DISPLAY_NAME}, null, null, null);
        if (cur == null) return 0;
        try {
            while (cur.moveToNext()) files.put(cur.getString(1), DocumentsContract.buildDocumentUriUsingTree(tree, cur.getString(0)));
        } finally {
            cur.close();
        }
        java.util.Set<String> have = new java.util.HashSet<String>();
        for (Library.Item it : lib.items) have.add(it.id);
        int n = 0;
        List<String> names = new ArrayList<String>(files.keySet());
        for (String name : names) {
            if (!name.endsWith(".json")) continue;
            // MediaStore may add " (1)" to repeated names; the description tells the real file name
            Object d = Json.parseLoose(new String(Project.readAll(cr.openInputStream(files.get(name))), "UTF-8"));
            if (d == null) continue;
            String id = Json.str(d, "id", ""), file = Json.str(d, "file", "");
            if (id.length() == 0 || have.contains(id)) continue;
            Uri media = files.get(file + PRIVATE);
            if (media == null) media = files.get(file);          // backups made by older versions
            if (media == null) continue;
            String ext = file.contains(".") ? file.substring(file.lastIndexOf('.')) : ".bin";
            byte[] data = Project.readAll(cr.openInputStream(media));
            Library.Item it = lib.restoreItem(Json.str(d, "type", Library.PIC), id, data, ext);
            it.kind = Json.str(d, "kind", ""); it.name = Json.str(d, "name", ""); it.tags = Json.str(d, "tags", "");
            it.source = Json.str(d, "source", ""); it.meta = Json.str(d, "meta", "");
            have.add(id);
            n++;
        }
        lib.save();
        return n;
    }
}
