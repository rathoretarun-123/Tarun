package com.tarun.kahani.app;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaPlayer;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.io.InputStream;
import java.util.List;

/**
 * The library chooser: shows 4 items at a time with "next 4", plus the other ways to get one
 * (phone, camera, record, online search, AI, or let the studio decide).
 */
final class Picker {
    interface Listener {
        void picked(Library.Item it);
        void action(String action);   // phone | camera | record | online | ai | auto | phoneVoice
    }

    private final Activity a;
    private final Library lib;
    private static MediaPlayer player;

    Picker(Activity a, Library lib) { this.a = a; this.lib = lib; }

    /** Plays items that are not files (the studio's built-in voices are spoken on demand). */
    interface Player { void play(Library.Item it); }
    Player presetPlayer;
    /** Built-in items shown after the user's own (e.g. studio voices). */
    final java.util.List<Library.Item> extra = new java.util.ArrayList<Library.Item>();
    /** Items that suit what is being chosen; shown first with a ★. */
    final java.util.Set<String> suggested = new java.util.HashSet<String>();
    /** How well each item fits (higher first among the ★ items) and a short reason shown under it. */
    final java.util.Map<String, Float> fit = new java.util.HashMap<String, Float>();
    final java.util.Map<String, String> note = new java.util.HashMap<String, String>();

    static Bitmap thumb(Activity a, Library.Item it, int max) {
        try {
            InputStream in = it.path.startsWith("asset:") ? a.getAssets().open(it.path.substring(6)) : new java.io.FileInputStream(it.path);
            byte[] data = Project.readAll(in);
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int s = 1;
            while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= max) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, o);
            return b == null ? null : Project.upright(b, data);
        } catch (Throwable e) {
            return null;
        }
    }

    private static final java.util.concurrent.ExecutorService THUMBS = java.util.concurrent.Executors.newSingleThreadExecutor();

    /** Loads a thumbnail on a worker thread and shows it when ready (the screen never freezes). */
    static void thumbAsync(final Activity a, final ImageView iv, final Library.Item it, final int max) {
        iv.setTag(it.id);
        THUMBS.execute(new Runnable() {
            public void run() {
                final Bitmap b = thumb(a, it, max);
                a.runOnUiThread(new Runnable() {
                    public void run() { if (b != null && it.id.equals(iv.getTag())) iv.setImageBitmap(b); }
                });
            }
        });
    }

    static void play(Activity a, String path) {
        stop();
        try {
            MediaPlayer mp = new MediaPlayer();
            if (path.startsWith("asset:")) {
                AssetFileDescriptor fd = a.getAssets().openFd(path.substring(6));
                mp.setDataSource(fd.getFileDescriptor(), fd.getStartOffset(), fd.getLength());
                fd.close();
            } else mp.setDataSource(path);
            player = mp;
            if (path.startsWith("http")) {
                // internet sounds buffer in the background so the screen never freezes
                mp.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                    public void onPrepared(MediaPlayer p) { if (p == player) p.start(); }
                });
                mp.setOnErrorListener(new MediaPlayer.OnErrorListener() {
                    public boolean onError(MediaPlayer p, int what, int extra) { return true; }
                });
                mp.prepareAsync();
            } else {
                mp.prepare();
                mp.start();
            }
        } catch (Exception e) {
            android.widget.Toast.makeText(a, "Could not play", android.widget.Toast.LENGTH_SHORT).show();
        }
    }

    static void stop() {
        try { if (player != null) { player.stop(); player.release(); } } catch (Exception ignored) {}
        player = null;
        playing = null;
        if (playBtn != null) playBtn.setText("▶");
        playBtn = null;
    }

    private static String playing;
    private static android.widget.Button playBtn;

    /**
     * Play / pause / stop for one sound in a list: ▶ starts it (and stops any other), the same button then shows
     * ⏸ to pause and ▶ again to carry on; ■ stops it. When the sound ends the button goes back to ▶.
     */
    static View controls(final Activity a, final String path) {
        LinearLayout r = Ui.row(a);
        final android.widget.Button[] pb = new android.widget.Button[1];
        pb[0] = Ui.small(a, "▶", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) {
                if (path.equals(playing) && player != null) {
                    try {
                        if (player.isPlaying()) { player.pause(); pb[0].setText("▶"); }
                        else { player.start(); pb[0].setText("⏸"); }
                    } catch (Exception e) { stop(); }
                    return;
                }
                play(a, path);
                if (player == null) return;
                playing = path;
                playBtn = pb[0];
                pb[0].setText("⏸");
                final MediaPlayer mine = player;
                mine.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    public void onCompletion(MediaPlayer p) { if (p == player) { pb[0].setText("▶"); stop(); } }
                });
            }
        });
        r.addView(pb[0]);
        r.addView(Ui.small(a, "■", Ui.SUB, new View.OnClickListener() {
            public void onClick(View v) { if (path.equals(playing)) stop(); }
        }));
        return r;
    }

    /**
     * Shows the chooser. type = Library.PIC / VOICE / SOUND, query sorts best matches first,
     * actions = which extra buttons to offer.
     */
    void show(String title, final String type, final String query, final String[] actions, final Listener l) {
        final List<Library.Item> items = new java.util.ArrayList<Library.Item>(lib.find(type, null, query));
        items.addAll(extra);
        if (!suggested.isEmpty()) {
            java.util.Collections.sort(items, new java.util.Comparator<Library.Item>() {
                public int compare(Library.Item x, Library.Item y) {
                    int c = (suggested.contains(y.id) ? 1 : 0) - (suggested.contains(x.id) ? 1 : 0);
                    if (c != 0) return c;
                    Float fx = fit.get(x.id), fy = fit.get(y.id);
                    return Float.compare(fy == null ? 0 : fy, fx == null ? 0 : fx);
                }
            });
        }
        final AlertDialog[] d = new AlertDialog[1];
        final LinearLayout body = Ui.column(a);
        body.setPadding(Ui.dp(a, 12), Ui.dp(a, 6), Ui.dp(a, 12), Ui.dp(a, 6));
        final LinearLayout grid = Ui.column(a);
        final int[] page = {0};
        final TextView pageInfo = Ui.text(a, "", 13, Ui.SUB, false);
        final Runnable[] fill = new Runnable[1];
        fill[0] = new Runnable() {
            public void run() {
                grid.removeAllViews();
                int from = page[0] * 4;
                if (items.isEmpty()) grid.addView(Ui.text(a, "Nothing in the library yet — add below.", 14, Ui.SUB, false));
                LinearLayout row = null;
                for (int i = from; i < Math.min(items.size(), from + 4); i++) {
                    final Library.Item it = items.get(i);
                    if (type.equals(Library.PIC)) {
                        if ((i - from) % 2 == 0) { row = Ui.row(a); grid.addView(row); }
                        LinearLayout cell = Ui.column(a);
                        cell.setPadding(Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4), Ui.dp(a, 4));
                        ImageView iv = new ImageView(a);
                        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                        iv.setBackgroundColor(0xFFEEEEEE);
                        thumbAsync(a, iv, it, 300);
                        cell.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(a, 110)));
                        TextView t = Ui.text(a, it.label(), 12, Ui.TEXT, false);
                        t.setMaxLines(1);
                        cell.addView(t);
                        cell.setOnClickListener(new View.OnClickListener() {
                            public void onClick(View v) { d[0].dismiss(); l.picked(it); }
                        });
                        row.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    } else {
                        LinearLayout r = Ui.row(a);
                        r.setGravity(Gravity.CENTER_VERTICAL);
                        TextView t = Ui.text(a, (suggested.contains(it.id) ? "★ " : "") + (type.equals(Library.VOICE) ? "🎙 " : "🔊 ") + it.label()
                                + (it.builtIn ? "" : "  (yours)"), 14, suggested.contains(it.id) ? Ui.GREEN : Ui.TEXT, false);
                        String why = note.get(it.id);
                        if (why != null && why.length() > 0) {
                            LinearLayout tc = Ui.column(a);
                            tc.addView(t);
                            tc.addView(Ui.text(a, why, 12, Ui.SUB, false));
                            r.addView(tc, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                        } else {
                            r.addView(t, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                        }
                        if (it.path.startsWith("preset:") && presetPlayer != null) r.addView(Ui.small(a, "▶", Ui.BLUE, new View.OnClickListener() {
                            public void onClick(View v) { presetPlayer.play(it); }
                        }));
                        else r.addView(controls(a, it.path));
                        r.addView(Ui.small(a, "Choose", Ui.GREEN, new View.OnClickListener() {
                            public void onClick(View v) { stop(); d[0].dismiss(); l.picked(it); }
                        }));
                        grid.addView(r);
                    }
                }
                int pages = Math.max(1, (items.size() + 3) / 4);
                pageInfo.setText(items.isEmpty() ? "" : "Page " + (page[0] + 1) + " / " + pages + "  (total " + items.size() + ")");
            }
        };
        body.addView(grid);
        LinearLayout nav = Ui.row(a);
        nav.addView(Ui.small(a, "◀ Previous 4", Ui.SUB, new View.OnClickListener() {
            public void onClick(View v) { if (page[0] > 0) { page[0]--; fill[0].run(); } }
        }));
        nav.addView(Ui.small(a, "Next 4 ▶", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { if ((page[0] + 1) * 4 < items.size()) { page[0]++; fill[0].run(); } }
        }));
        body.addView(nav);
        body.addView(pageInfo);
        for (final String act : actions) {
            String label;
            int color = Ui.PRIMARY;
            if (act.equals("phone")) label = type.equals(Library.PIC) ? "📂 Picture from phone" : "📂 File from phone";
            else if (act.equals("camera")) label = "📷 Photo from camera";
            else if (act.equals("record")) label = "🎙 Record now";
            else if (act.equals("online")) { label = "🌐 Search free on the internet"; color = Ui.BLUE; }
            else if (act.equals("ai")) { label = "✨ Make with AI (free)"; color = Ui.BLUE; }
            else if (act.equals("phoneVoice")) { label = "🗣 Studio voice (remove sample)"; color = Ui.SUB; }
            else { label = "🎬 Let the studio choose"; color = Ui.GREEN; }
            Button b = Ui.button(a, label, color, new View.OnClickListener() {
                public void onClick(View v) { stop(); d[0].dismiss(); l.action(act); }
            });
            body.addView(b);
        }
        fill[0].run();
        ScrollView sv = new ScrollView(a);
        sv.addView(body);
        d[0] = new AlertDialog.Builder(a).setTitle(title).setView(sv).setNegativeButton("Close", null).create();
        d[0].setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            public void onDismiss(android.content.DialogInterface di) { stop(); }
        });
        d[0].show();
    }
}
