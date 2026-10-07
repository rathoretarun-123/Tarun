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
    private static final ExecutorService IO = Executors.newSingleThreadExecutor();

    /** Copies the item's file and its description in the background (never slows the app down). */
    static void copy(final Context ctx, final Library.Item it) {
        if (it == null || it.builtIn) return;
        final Context app = ctx.getApplicationContext();
        IO.execute(new Runnable() {
            public void run() {
                try {
                    File f = new File(it.path);
                    write(app, f.getName(), new FileInputStream(f), mime(f.getName()));
                    write(app, f.getName() + ".json", new java.io.ByteArrayInputStream(Library.describe(it).getBytes("UTF-8")), "application/json");
                } catch (Throwable ignored) {
                    // the library inside the app is still complete; the backup is an extra
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
            Uri media = files.get(file);
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
