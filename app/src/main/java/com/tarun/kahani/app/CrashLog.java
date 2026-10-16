package com.tarun.kahani.app;

import android.content.Context;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintWriter;
import java.io.StringWriter;

/** Remembers an unexpected crash so the next start can explain it instead of failing silently. */
final class CrashLog implements Thread.UncaughtExceptionHandler {
    private final Context ctx;
    private final Thread.UncaughtExceptionHandler next;

    CrashLog(Context c, Thread.UncaughtExceptionHandler next) {
        this.ctx = c.getApplicationContext();
        this.next = next;
    }

    public void uncaughtException(Thread t, Throwable e) {
        try {
            StringWriter sw = new StringWriter();
            e.printStackTrace(new PrintWriter(sw));
            String s = sw.toString();
            if (s.length() > 1500) s = s.substring(0, 1500);
            FileOutputStream o = new FileOutputStream(new File(ctx.getFilesDir(), "crash.txt"));
            o.write(s.getBytes("UTF-8"));
            o.close();
        } catch (Throwable ignored) {
        }
        if (next != null) next.uncaughtException(t, e);
    }

    static String last(Context c) {
        File f = new File(c.getFilesDir(), "crash.txt");
        if (!f.exists()) return null;
        try {
            String s = new String(Project.readAll(new java.io.FileInputStream(f)), "UTF-8");
            f.delete();
            return s.length() > 400 ? s.substring(0, 400) : s;
        } catch (Exception e) {
            f.delete();
            return null;
        }
    }
}
