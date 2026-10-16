package com.tarun.kahani.app;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.provider.OpenableColumns;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * Shares files from the app's "shared" folder with other apps: the camera writes a photo here, and the production
 * file / film can be sent to other apps. Only files inside that one folder are reachable.
 */
public final class FilesProvider extends ContentProvider {
    public static final String AUTH = "com.tarun.kahani.files";

    public static File sharedDir(Context c) {
        File d = new File(c.getCacheDir(), "shared");
        d.mkdirs();
        return d;
    }

    public static Uri uriFor(File f) { return Uri.parse("content://" + AUTH + "/" + Uri.encode(f.getName())); }

    private File fileFor(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.contains("/") || name.contains("..")) throw new FileNotFoundException();
        return new File(sharedDir(getContext()), name);
    }

    public boolean onCreate() { return true; }

    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        File f = fileFor(uri);
        int m = mode.contains("w") ? ParcelFileDescriptor.MODE_READ_WRITE | ParcelFileDescriptor.MODE_CREATE
                | (mode.contains("t") ? ParcelFileDescriptor.MODE_TRUNCATE : 0) : ParcelFileDescriptor.MODE_READ_ONLY;
        return ParcelFileDescriptor.open(f, m);
    }

    public Cursor query(Uri uri, String[] projection, String sel, String[] args, String sort) {
        File f;
        try { f = fileFor(uri); } catch (FileNotFoundException e) { return null; }
        MatrixCursor c = new MatrixCursor(new String[]{OpenableColumns.DISPLAY_NAME, OpenableColumns.SIZE});
        c.addRow(new Object[]{f.getName(), f.length()});
        return c;
    }

    public String getType(Uri uri) {
        String n = uri.getLastPathSegment() == null ? "" : uri.getLastPathSegment().toLowerCase();
        if (n.endsWith(".jpg")) return "image/jpeg";
        if (n.endsWith(".png")) return "image/png";
        if (n.endsWith(".mp4")) return "video/mp4";
        if (n.endsWith(".txt")) return "text/plain";
        if (n.endsWith(".wav")) return "audio/wav";
        return "application/octet-stream";
    }

    public Uri insert(Uri uri, ContentValues v) { return null; }
    public int delete(Uri uri, String s, String[] a) { return 0; }
    public int update(Uri uri, ContentValues v, String s, String[] a) { return 0; }
}
