package com.tarun.kahani.app;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;

/**
 * Keeps the film being made when the screen is off or the app is in the background: a foreground service with a
 * progress notification (stage, percent, time left) and a partial wake lock. No time limit.
 */
public final class FilmService extends Service {
    static final String CH = "film";
    static final int ID = 7;
    private PowerManager.WakeLock lock;
    private final Handler h = new Handler(Looper.getMainLooper());
    private boolean finished;

    public static void start(Context c) {
        Intent i = new Intent(c, FilmService.class);
        try {
            if (Build.VERSION.SDK_INT >= 26) c.startForegroundService(i);
            else c.startService(i);
        } catch (Exception ignored) {
            // the job still runs in its own thread while the app is open
        }
    }

    public IBinder onBind(Intent i) { return null; }

    public int onStartCommand(Intent intent, int flags, int startId) {
        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (Build.VERSION.SDK_INT >= 26 && nm != null && nm.getNotificationChannel(CH) == null) {
            NotificationChannel ch = new NotificationChannel(CH, "Film making", NotificationManager.IMPORTANCE_LOW);
            ch.setDescription("Progress of the film being made");
            nm.createNotificationChannel(ch);
        }
        Notification n = build("Making your film…", "", 0, false);
        try {
            if (Build.VERSION.SDK_INT >= 29) startForeground(ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC);
            else startForeground(ID, n);
        } catch (Exception e) {
            stopSelf();
            return START_NOT_STICKY;
        }
        if (lock == null) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null) {
                lock = pm.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "kahani:film");
                lock.setReferenceCounted(false);
                lock.acquire();
            }
        }
        h.removeCallbacksAndMessages(null);
        h.post(tick);
        return START_NOT_STICKY;
    }

    private final Runnable tick = new Runnable() {
        public void run() {
            FilmJob j = FilmJob.current;
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            if (j == null || j.done || j.failed || j.cancelled) {
                if (!finished && j != null && nm != null) {
                    finished = true;
                    String t = j.done ? "🎉 Your film is ready!" : j.failed ? "❌ The film could not be made" : "Stopped";
                    nm.notify(ID + 1, build(t, j.done ? "Tap to watch" : j.error, 0, true));
                }
                stopForeground(true);
                stopSelf();
                return;
            }
            if (nm != null) nm.notify(ID, build(j.stage, j.eta(), (int) (j.progress * 100), false));
            h.postDelayed(this, 2000);
        }
    };

    private Notification build(String title, String text, int pct, boolean done) {
        Intent open = new Intent(this, MainActivity.class);
        open.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP | Intent.FLAG_ACTIVITY_CLEAR_TOP);
        PendingIntent pi = PendingIntent.getActivity(this, 0, open, PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification.Builder b = Build.VERSION.SDK_INT >= 26 ? new Notification.Builder(this, CH) : new Notification.Builder(this);
        b.setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(title).setContentText(text).setContentIntent(pi)
                .setOnlyAlertOnce(true);
        if (!done) b.setOngoing(true).setProgress(100, pct, pct == 0);
        else b.setAutoCancel(true);
        return b.build();
    }

    public void onDestroy() {
        h.removeCallbacksAndMessages(null);
        if (lock != null && lock.isHeld()) lock.release();
        super.onDestroy();
    }
}
