package com.tarun.kahani.app;

import android.Manifest;
import android.accounts.AccountManager;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.MediaScannerConnection;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.speech.tts.TextToSpeech;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.MediaController;
import android.widget.ProgressBar;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.VideoView;

import com.tarun.kahani.core.Art;
import com.tarun.kahani.core.Bible;
import com.tarun.kahani.core.Cloud;
import com.tarun.kahani.core.CommandParser;
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Edits;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.Puppet;
import com.tarun.kahani.core.ScriptAI;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Sets;
import com.tarun.kahani.core.SoundLib;
import com.tarun.kahani.core.Story;
import com.tarun.kahani.core.Toon;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * The whole app: sign-in, a simple dashboard, story, studio (pictures, voices, places, sounds, production file),
 * background progress, preview with a command box, library and settings.
 */
public class MainActivity extends Activity {

    static final int REQ_LOGIN = 11, REQ_SCRIPT = 12, REQ_IMAGE = 13, REQ_CAMERA = 14, REQ_AUDIO = 15, REQ_BULK = 16,
            REQ_SAVE_TEXT = 17, REQ_RESTORE = 18, REQ_PERMS = 20, REQ_PERM_GALLERY = 21, REQ_PERM_ONE = 22, REQ_LIB_MANY = 23;
    static final int S_HOME = 0, S_STORY = 1, S_STUDIO = 2, S_FACE = 3, S_PROGRESS = 4, S_PLAYER = 5, S_LIBRARY = 6,
            S_SETTINGS = 7, S_LINES = 8, S_LOGIN = 9, S_QC = 10;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private Project project;
    private int screen;
    private EditText scriptBox;
    private Story castStory;
    private Library library;
    /** What an incoming picture/sound is for: char:NAME, scene:N, title, end, voice:NAME, amb:N, lib:pic|voice|sound */
    private String target;
    private File cameraFile;
    private String pendingText, pendingTextName;
    private Voices previewVoices;
    private AudioIO.Recorder recorder;
    private File recordingFile;       // what the line recorder is writing (deleted if left unfinished)
    private boolean previewBusy;
    private Runnable afterPermission;
    private boolean askedPerms;
    private String libTab = Library.PIC;

    // ================================================================== lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Thread.setDefaultUncaughtExceptionHandler(new CrashLog(this, Thread.getDefaultUncaughtExceptionHandler()));
        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        setContentView(root);
        getWindow().setStatusBarColor(Ui.PRIMARY_DARK);
        library = Library.get(this);
        // pictures placed in older stories join the library (once), so every new story can use them
        final Context app = getApplicationContext();
        new Thread(new Runnable() {
            public void run() { try { AutoLibrary.adoptOldStories(app, library); } catch (Throwable ignored) {} }
        }, "adopt-old-stories").start();
        // library copies saved by older versions as plain photos: made private once, out of the gallery
        if (!"1".equals(Prefs.get(this, "privateBackup", "0"))) { Backup.privatizeOld(this); Prefs.put(this, "privateBackup", "1"); }
        if (b != null) {
            String pd = b.getString("project");
            if (pd != null && new File(pd).isDirectory()) project = new Project(new File(pd));
            target = b.getString("target");
            String cf = b.getString("camera");
            if (cf != null) cameraFile = new File(cf);
        }
        String crash = CrashLog.last(this);
        FilmJob job = FilmJob.current;
        if (Prefs.account(this).length() == 0 && !Prefs.skippedLogin(this)) showLogin();
        else if (job != null && !job.done && !job.failed && !job.cancelled) { project = job.project; showProgress(); }
        else if (job != null && job.done && !job.seen && project == null && job.project.dir.isDirectory()) { project = job.project; showPlayer(); }
        else if (project != null) showStory();
        else showHome();
        if (crash != null) {
            new AlertDialog.Builder(this).setTitle("The app closed unexpectedly last time")
                    .setMessage("Sorry about that. Your story is safe. If it keeps happening, try 720p.\n\n" + crash)
                    .setPositiveButton("OK", null).show();
        }
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        FilmJob j = FilmJob.current;
        if (j != null && j.done && screen != S_PLAYER) { project = j.project; showPlayer(); }
        else if (j != null && !j.done && !j.failed && !j.cancelled && screen != S_PROGRESS) { project = j.project; showProgress(); }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (project != null) out.putString("project", project.dir.getAbsolutePath());
        if (target != null) out.putString("target", target);
        if (cameraFile != null) out.putString("camera", cameraFile.getAbsolutePath());
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveScript();
        Picker.stop();
        stopRecording();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (previewVoices != null) previewVoices.shutdown();
        if (recorder != null) recorder.stop();
        ui.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
        switch (screen) {
            case S_STORY: saveScript(); showHome(); break;
            case S_STUDIO: case S_PROGRESS: case S_PLAYER: case S_QC: showStory(); break;
            case S_FACE: case S_LINES: showStudio(); break;
            case S_LIBRARY: case S_SETTINGS: if (project != null) showStory(); else showHome(); break;
            default: super.onBackPressed();
        }
    }

    /** Stops a recording that is still running (leaving the screen or the app) and throws it away. */
    private void stopRecording() {
        if (recorder == null) return;
        recorder.stop();
        recorder = null;
        if (recordingFile != null) recordingFile.delete();
        recordingFile = null;
    }

    private void setScreen(int s, View content) {
        stopRecording();
        screen = s;
        ui.removeCallbacksAndMessages(null);
        if (s != S_PROGRESS) getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root.removeAllViews();
        root.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private LinearLayout page(int s, String title, boolean back) {
        LinearLayout outer = Ui.column(this);
        LinearLayout bar = Ui.row(this);
        bar.setBackgroundColor(Ui.PRIMARY);
        bar.setPadding(Ui.dp(this, 8), Ui.dp(this, 10), Ui.dp(this, 12), Ui.dp(this, 10));
        if (back) {
            TextView arrow = Ui.text(this, "  ←  ", 22, 0xFFFFFFFF, true);
            arrow.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { onBackPressed(); } });
            bar.addView(arrow);
        }
        TextView t = Ui.text(this, title, 20, 0xFFFFFFFF, true);
        t.setMaxLines(1);
        bar.addView(t);
        outer.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        ScrollView sv = new ScrollView(this);
        sv.setFillViewport(true);
        LinearLayout body = Ui.column(this);
        body.setPadding(0, Ui.dp(this, 8), 0, Ui.dp(this, 24));
        sv.addView(body);
        outer.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        setScreen(s, outer);
        return body;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    private TextView heading(LinearLayout body, String s) {
        TextView h = Ui.title(this, s);
        h.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), 0, 0);
        body.addView(h);
        return h;
    }

    /** Runs work in the background with a spinner; done runs on the UI thread with the result (or error). */
    interface Work { Object run() throws Exception; }
    interface Done { void done(Object result, Exception error); }

    private void background(String title, final Work w, final Done d) {
        final ProgressBar pb = new ProgressBar(this);
        pb.setPadding(0, Ui.dp(this, 16), 0, Ui.dp(this, 16));
        final AlertDialog dlg = title == null ? null : new AlertDialog.Builder(this).setTitle(title).setView(pb).setCancelable(false).show();
        new Thread(new Runnable() {
            public void run() {
                Object r = null;
                Exception err = null;
                try { r = w.run(); } catch (Exception e) { err = e; } catch (Throwable t) { err = new Exception(t.toString()); }
                final Object fr = r;
                final Exception fe = err;
                ui.post(new Runnable() {
                    public void run() {
                        try { if (dlg != null) dlg.dismiss(); } catch (Exception ignored) {}
                        if (!isFinishing() && !isDestroyed()) d.done(fr, fe);
                    }
                });
            }
        }).start();
    }

    // ================================================================== sign-in & permissions

    private void showLogin() {
        LinearLayout body = page(S_LOGIN, "🎬 Kahani Film", false);
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, "Welcome!", 22, Ui.TEXT, true));
        c.addView(Ui.text(this, "Turn your story into a cartoon film for children — the studio prepares voices, music, camera and scenes for you.\n\nSign in with your Gmail (Google account) to start.", 16, Ui.SUB, false));
        c.addView(Ui.button(this, "G   Sign in with Gmail", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { login(); }
        }));
        c.addView(Ui.small(this, "Not now", Ui.SUB, new View.OnClickListener() {
            public void onClick(View v) { Prefs.put(MainActivity.this, "skipLogin", "1"); askPermissions(); showHome(); }
        }));
        body.addView(c);
    }

    private void login() {
        try {
            Intent i = AccountManager.newChooseAccountIntent(null, null, new String[]{"com.google"}, null, null, null, null);
            startActivityForResult(i, REQ_LOGIN);
        } catch (Exception e) {
            final EditText et = new EditText(this);
            et.setHint("Your Gmail address");
            et.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_CLASS_TEXT);
            new AlertDialog.Builder(this).setTitle("Enter your Gmail address").setView(et)
                    .setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) {
                            String m = et.getText().toString().trim();
                            if (m.contains("@")) { Prefs.setAccount(MainActivity.this, m); askPermissions(); showHome(); }
                        }
                    }).show();
        }
    }

    private void logout() {
        new AlertDialog.Builder(this).setTitle("Log out?")
                .setMessage("Your films and library stay safe on this phone.")
                .setPositiveButton("Yes, log out", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        Prefs.setAccount(MainActivity.this, "");
                        Prefs.put(MainActivity.this, "skipLogin", "0");
                        project = null;
                        showLogin();
                    }
                }).setNegativeButton("No", null).show();
    }

    private String[] neededPermissions() {
        List<String> p = new ArrayList<String>();
        p.add(Manifest.permission.RECORD_AUDIO);
        p.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= 33) p.add("android.permission.POST_NOTIFICATIONS");
        if (Build.VERSION.SDK_INT < 29) p.add(Manifest.permission.WRITE_EXTERNAL_STORAGE);   // backup copy in Downloads
        // pictures and sounds are picked with the system file chooser, which needs no storage permission
        List<String> missing = new ArrayList<String>();
        for (String s : p) if (checkSelfPermission(s) != PackageManager.PERMISSION_GRANTED) missing.add(s);
        return missing.toArray(new String[0]);
    }

    private void askPermissions() {
        if (askedPerms) return;
        askedPerms = true;
        String[] m = neededPermissions();
        if (m.length > 0) requestPermissions(m, REQ_PERMS);
    }

    /** Runs r when the permission is granted (asks if needed). */
    private void withPermission(String perm, String why, Runnable r) {
        if (checkSelfPermission(perm) == PackageManager.PERMISSION_GRANTED) { r.run(); return; }
        afterPermission = r;
        toast(why);
        requestPermissions(new String[]{perm}, REQ_PERM_ONE);
    }

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code == REQ_PERM_ONE) {
            Runnable r = afterPermission;
            afterPermission = null;
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED && r != null) r.run();
            else toast("Permission not given — turn it on in Settings > Apps > Kahani Film > Permissions");
        } else if (code == REQ_PERM_GALLERY) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) saveToGallery(pendingShare);
            else toast("No permission to save to the gallery");
        }
    }

    // ================================================================== home (dashboard)

    private void showHome() {
        project = null;
        LinearLayout body = page(S_HOME, "🎬 Kahani Film", false);
        String acc = Prefs.account(this);
        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 12), 0);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView who = Ui.text(this, acc.length() > 0 ? "👤 " + acc : "👤 Not signed in", 14, Ui.SUB, false);
        top.addView(who, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(Ui.small(this, acc.length() > 0 ? "Log out" : "Sign in", Ui.SUB, new View.OnClickListener() {
            public void onClick(View v) { if (Prefs.account(MainActivity.this).length() > 0) logout(); else login(); }
        }));
        body.addView(top);

        LinearLayout hero = Ui.card(this);
        hero.addView(Ui.text(this, "Make a cartoon film from your story", 18, Ui.TEXT, true));
        hero.addView(Ui.text(this, "1. Paste your story   2. (Optional) add pictures and voices   3. Tap ‘Make film’", 15, Ui.SUB, false));
        hero.addView(Ui.button(this, "➕  New film", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { project = Project.create(MainActivity.this); showStory(); }
        }));
        hero.addView(Ui.button(this, "📖  Example: The Two Princesses of Ratnagarh", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { openSample(); }
        }));
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "📚 Library", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { showLibrary(); }
        }));
        r.addView(Ui.small(this, "⚙ Settings", Ui.SUB, new View.OnClickListener() {
            public void onClick(View v) { showSettings(); }
        }));
        hero.addView(r);
        body.addView(hero);

        FilmJob job = FilmJob.current;
        if (job != null && !job.done && !job.failed && !job.cancelled) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "⏳ A film is being made: " + job.project.name(), 16, Ui.TEXT, true));
            final Project jp = job.project;
            c.addView(Ui.button(this, "See progress", Ui.GREEN, new View.OnClickListener() {
                public void onClick(View v) { project = jp; showProgress(); }
            }));
            body.addView(c);
        }

        List<Project> ps = Project.all(this);
        if (!ps.isEmpty()) heading(body, "My films");
        for (final Project p : ps) {
            LinearLayout c = Ui.card(this);
            final boolean made = p.film().exists();
            c.addView(Ui.text(this, p.name(), 17, Ui.TEXT, true));
            long secs = 0;
            try { secs = Long.parseLong(p.setting("filmSeconds", "0")); } catch (NumberFormatException ignored) {}
            c.addView(Ui.text(this, made ? "✅ Film ready (" + FilmJob.fmt(secs) + ")" : "✏️ In progress", 14, made ? Ui.GREEN : Ui.SUB, false));
            LinearLayout rr = Ui.row(this);
            rr.addView(Ui.small(this, "Open", Ui.PRIMARY, new View.OnClickListener() {
                public void onClick(View v) { project = p; showStory(); }
            }));
            if (made) rr.addView(Ui.small(this, "▶ Watch", Ui.GREEN, new View.OnClickListener() {
                public void onClick(View v) { project = p; showPlayer(); }
            }));
            rr.addView(Ui.small(this, "🗑", Ui.RED, new View.OnClickListener() {
                public void onClick(View v) { confirmDelete(p); }
            }));
            c.addView(rr);
            body.addView(c);
        }
    }

    private void openSample() {
        for (Project p : Project.all(this)) {
            if (p.has("char_vrinda.jpg") && p.read("script.txt").contains("रत्नगढ़")) { project = p; showStory(); return; }
        }
        background("Preparing the example…", new Work() {
            public Object run() throws Exception { return Project.createSample(MainActivity.this); }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("Could not open the example: " + e.getMessage()); return; }
                project = (Project) r;
                showStory();
            }
        });
    }

    private void confirmDelete(final Project p) {
        new AlertDialog.Builder(this).setTitle("Delete?").setMessage("\"" + p.name() + "\" and its film will be deleted for good.")
                .setPositiveButton("Yes, delete", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { deleteDir(p.dir); showHome(); }
                }).setNegativeButton("No", null).show();
    }

    static void deleteDir(File d) {
        File[] fs = d.listFiles();
        if (fs != null) for (File f : fs) { if (f.isDirectory()) deleteDir(f); else f.delete(); }
        d.delete();
    }

    // ================================================================== story

    private void saveScript() {
        if (project != null && scriptBox != null && screen == S_STORY) {
            String now = scriptBox.getText().toString();
            if (!now.equals(project.read("script.txt"))) {
                project.write("script.txt", now);
                project.setSetting("useAi", "0");   // the AI version was made from the old text
            }
        }
    }

    private Edits edits() { return Edits.fromJson(project.read("edits.json")); }
    private void saveEdits(Edits e) { project.write("edits.json", e.toJson()); }

    private void showStory() {
        if (project == null) { showHome(); return; }
        LinearLayout body = page(S_STORY, project.name(), true);

        LinearLayout c1 = Ui.card(this);
        c1.addView(Ui.title(this, "1. Story"));
        c1.addView(Ui.text(this, "Write it any way you like — Hindi, English or Hinglish, as a story or as a script (Name (feeling): \"dialogue\"). Descriptions of characters and places are only used for the pictures; they are never read aloud.", 13, Ui.SUB, false));
        scriptBox = new EditText(this);
        scriptBox.setText(project.read("script.txt"));
        scriptBox.setHint("Paste your story here…");
        scriptBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        scriptBox.setGravity(Gravity.TOP);
        scriptBox.setMinLines(8);
        scriptBox.setVerticalScrollBarEnabled(true);
        scriptBox.setTextSize(15);
        scriptBox.setBackground(Ui.round(0xFFFFFDF7, Ui.dp(this, 10), 0x33000000, Ui.dp(this, 1)));
        scriptBox.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));
        c1.addView(scriptBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 240)));
        // right below the story, in a light shade: clears the whole story (asks first)
        c1.addView(Ui.soft(this, "🧹  Clear the whole story", 0xFF8D6E63, 0xFFFBF3EA, new View.OnClickListener() {
            public void onClick(View v) {
                if (scriptBox.getText().length() == 0) { toast("The story box is already empty"); return; }
                new AlertDialog.Builder(MainActivity.this).setTitle("Clear the story?").setMessage("The whole story in the box will be removed.")
                        .setPositiveButton("Yes, clear", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { scriptBox.setText(""); saveScript(); }
                        }).setNegativeButton("No", null).show();
            }
        }));
        LinearLayout r1 = Ui.row(this);
        r1.addView(Ui.small(this, "📋 Paste", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(MainActivity.this);
                    scriptBox.setText(t);
                    saveScript();
                } else toast("The clipboard is empty");
            }
        }));
        r1.addView(Ui.small(this, "📂 File", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { pick("text/*", REQ_SCRIPT, false); }
        }));
        c1.addView(r1);
        boolean hasAi = project.has("script_ai.txt");
        final boolean useAi = "1".equals(project.setting("useAi", "0")) && hasAi;
        c1.addView(Ui.button(this, "🤖  Read with AI (make a script)", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { aiRead(); }
        }));
        if (hasAi) {
            CheckBox cb = new CheckBox(this);
            cb.setText("Use the script made by AI");
            cb.setChecked(useAi);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean on) { project.setSetting("useAi", on ? "1" : "0"); }
            });
            c1.addView(cb);
            c1.addView(Ui.small(this, "See the AI script", Ui.SUB, new View.OnClickListener() {
                public void onClick(View v) { showText("AI script", project.read("script_ai.txt")); }
            }));
        }
        // right after the story is read: everything described, to make it in other apps if wanted
        c1.addView(Ui.button(this, "📄  Descriptions of every character, place, object, shot, sound and voice", Ui.PRIMARY_DARK, new View.OnClickListener() {
            public void onClick(View v) {
                saveScript();
                Story st = loadStory();
                if (st == null || st.scenes.isEmpty()) { toast("Write or paste a story first"); return; }
                productionFile(st);
            }
        }));
        body.addView(c1);

        LinearLayout c2 = Ui.card(this);
        c2.addView(Ui.title(this, "2. Studio: characters, voices, places"));
        c2.addView(Ui.text(this, "The director reads your story and lists everything needed. Add pictures and voices, or let the studio choose. Download the production file here too.", 13, Ui.SUB, false));
        c2.addView(Ui.button(this, "🎭  Open studio", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { saveScript(); showStudio(); }
        }));
        body.addView(c2);

        LinearLayout c3 = Ui.card(this);
        c3.addView(Ui.title(this, "3. Where will you post it?"));
        final Edits ed = edits();
        final String[][] plats = {{"YouTube (wide 16:9)", "16:9"}, {"Instagram Reel / YouTube Shorts (tall 9:16)", "9:16"},
                {"Facebook / Instagram post (square 1:1)", "1:1"}};
        RadioGroup rg = new RadioGroup(this);
        for (int i = 0; i < plats.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(plats[i][0]);
            rb.setTextSize(15);
            rb.setId(2000 + i);
            rg.addView(rb);
            if (plats[i][1].equals(ed.aspect)) rb.setChecked(true);
        }
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup g, int id) { Edits e = edits(); e.aspect = plats[id - 2000][1]; saveEdits(e); }
        });
        c3.addView(rg);
        c3.addView(Ui.text(this, "Quality:", 14, Ui.SUB, false));
        RadioGroup qg = new RadioGroup(this);
        qg.setOrientation(RadioGroup.HORIZONTAL);
        final int[] qs = {480, 720, 1080};
        final String[] ql = {"480p (fast)", "720p HD", "1080p Full HD"};
        for (int i = 0; i < qs.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(ql[i]);
            rb.setTextSize(14);
            rb.setId(3000 + i);
            qg.addView(rb);
            if (qs[i] == ed.height) rb.setChecked(true);
        }
        qg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup g, int id) { Edits e = edits(); e.height = qs[id - 3000]; saveEdits(e); }
        });
        c3.addView(qg);
        body.addView(c3);

        LinearLayout c4 = Ui.card(this);
        c4.addView(Ui.button(this, "🎬  Make film", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { makeFilm(); }
        }));
        c4.addView(Ui.text(this, "While the film is being made you can lock the phone or use other apps — it keeps working.", 13, Ui.SUB, false));
        if (project.film().exists()) {
            c4.addView(Ui.button(this, "▶  Watch the film / make changes", Ui.PRIMARY, new View.OnClickListener() {
                public void onClick(View v) { showPlayer(); }
            }));
        }
        body.addView(c4);
    }

    private void aiRead() {
        saveScript();
        final String text = project.read("script.txt");
        if (text.trim().length() < 20) { toast("Write or paste a story first"); return; }
        if (!Prefs.online(this)) { toast("Turn on online features in Settings"); return; }
        background("AI is reading the story… (1-2 minutes)", new Work() {
            public Object run() { return ScriptAI.read(Prefs.cloud(MainActivity.this), text); }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("Could not do it: " + e.getMessage()); return; }
                ScriptAI.Result res = (ScriptAI.Result) r;
                if (res.rewritten) {
                    project.write("script_ai.txt", res.script);
                    project.setSetting("useAi", "1");
                }
                new AlertDialog.Builder(MainActivity.this).setTitle(res.rewritten ? "✅ Script ready" : "AI")
                        .setMessage(res.note + (res.rewritten ? "\n\nThe film will now be made from this script. Your original story is kept too." : ""))
                        .setPositiveButton("OK", null).show();
                showStory();
            }
        });
    }

    private void showText(String title, String text) {
        TextView tv = Ui.text(this, text, 14, Ui.TEXT, false);
        tv.setTextIsSelectable(true);
        tv.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        new AlertDialog.Builder(this).setTitle(title).setView(sv).setPositiveButton("Close", null).show();
    }

    // ================================================================== studio

    private Story loadStory() {
        Story st = ScriptParser.parse(FilmJob.scriptOf(project));
        castStory = st;
        return st;
    }

    private void showStudio() {
        if (project == null) { showHome(); return; }
        LinearLayout body = page(S_STUDIO, "🎭 Studio", true);
        final Story st;
        try {
            st = loadStory();
        } catch (Throwable e) {
            body.addView(Ui.text(this, "Could not read the story: " + e.getMessage(), 16, Ui.RED, true));
            return;
        }
        // ---- summary and what is missing
        int noPic = 0, noVoice = 0, noBg = 0;
        for (Story.CharacterDef c : st.characters) {
            if (charFile(c) == null) noPic++;
            if (library.byId(project.setting("vsample." + c.displayName, "")) == null) noVoice++;
        }
        for (Story.Scene sc : st.scenes) if (sceneFile(sc) == null) noBg++;
        // first visit: offer pictures already in the library (from earlier stories) that fit this story
        boolean offered = false;
        if (!"1".equals(project.setting("libChecked", "0")) && noPic == st.characters.size() && st.characters.size() > 0) {
            project.setSetting("libChecked", "1");
            for (Library.Item it : library.find(Library.PIC, null, null)) if (!it.builtIn) { offered = true; break; }
            if (offered) libraryMatches(true);
        }
        if (!offered && !"1".equals(project.setting("voiceChecked", "0")) && noVoice == st.characters.size() && st.characters.size() > 0) {
            // (after the pictures, or on the next visit) offer saved voices that fit the voices the script describes
            project.setSetting("voiceChecked", "1");
            boolean any = false;
            for (Library.Item it : library.find(Library.VOICE, null, null)) if (!it.builtIn) { any = true; break; }
            if (any) voiceMatches(true);
        }
        LinearLayout sum = Ui.card(this);
        sum.addView(Ui.title(this, "📜 " + st.title));
        sum.addView(Ui.text(this, "Language: " + (st.hinglish ? "Hinglish (spoken in Hindi, ends with \"समाप्त\")" : st.hindi ? "Hindi (ends with \"समाप्त\")" : "English (ends with \"The End\")")
                + "\nCharacters: " + st.characters.size() + "   •   Parts: " + st.scenes.size() + "   •   Lines: " + st.dialogueCount()
                + (st.hasNarrator ? "\nNarrator voice: yes (the story has a narrator)" : ""), 15, Ui.TEXT, false));
        String miss = "Still missing:\n• " + noPic + " character pictures  • " + noVoice + " voice samples  • " + noBg + " backgrounds"
                + "\nAnything you don't add, the studio creates with AI in 3D style when you make the film (internet), or draws itself offline.";
        sum.addView(Ui.text(this, miss, 14, noPic + noVoice + noBg == 0 ? Ui.GREEN : Ui.PRIMARY_DARK, false));
        for (String w : st.warnings) sum.addView(Ui.text(this, "⚠ " + w, 13, Ui.RED, false));
        if (st.dialogueCount() == 0) sum.addView(Ui.text(this, "⚠ No dialogue found. Tap \"Read with AI\" or write lines like — Name: \"dialogue\"", 14, Ui.RED, true));
        sum.addView(Ui.button(this, "📄  Descriptions for other apps: characters, places, objects, every shot, sounds, voices", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { productionFile(st); }
        }));
        sum.addView(Ui.button(this, "✨  Find pictures in my library for this story", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { libraryMatches(false); }
        }));
        sum.addView(Ui.button(this, "📥  Add many pictures — the director places them", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { pick("image/*", REQ_BULK, true); }
        }));
        sum.addView(Ui.button(this, "🎙  Find voices in my library for this story", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { voiceMatches(false); }
        }));
        sum.addView(Ui.button(this, "🎙  Record lines in your own voice", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { showLines(); }
        }));
        body.addView(sum);

        // ---- title and end pages
        heading(body, "Start and end");
        body.addView(pageCard("Title page", "title"));
        body.addView(pageCard(st.hindi ? "End page (समाप्त)" : "End page (The End)", "end"));

        // ---- characters
        heading(body, "Characters (pictures and voices)");
        for (Story.CharacterDef c : st.characters) body.addView(characterCard(st, c));
        if (st.hasNarrator) body.addView(narratorCard());

        // ---- parts of the story
        heading(body, "Places (backgrounds and sounds)");
        SoundLib sl = library.soundLib();
        for (Story.Scene sc : st.scenes) body.addView(sceneCard(sc, sl));

        // ---- director plan
        LinearLayout plan = Ui.card(this);
        plan.addView(Ui.title(this, "🎬 Director's plan"));
        try {
            Director.Options po = new Director.Options();
            po.sounds = sl;      // shows where your own sounds will play
            Director d = new Director(st, po);
            d.prepare();
            Film f = d.direct(new Art());
            StringBuilder sb = new StringBuilder();
            for (String n : f.notes) sb.append("• ").append(n).append('\n');
            sb.append("\nEstimated length: ").append(FilmJob.fmt((long) f.duration));
            if (f.duration > 30 * 60) sb.append("\n⚠ Longer than 30 minutes — it will take longer to make.");
            plan.addView(Ui.text(this, sb.toString(), 14, Ui.TEXT, false));
        } catch (Throwable e) {
            plan.addView(Ui.text(this, "Could not make the plan: " + e, 14, Ui.RED, false));
        }
        plan.addView(Ui.button(this, "🎬  Make film", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { makeFilm(); }
        }));
        body.addView(plan);
    }

    private String charFile(Story.CharacterDef c) {
        String line = project.manifestLine("char", c.displayName);
        if (line == null) line = findCharLine(c);
        String f = line != null && line.split("\\|").length > 2 ? line.split("\\|")[2] : null;
        return f != null && project.has(f) ? f : null;
    }

    private String sceneFile(Story.Scene sc) {
        String line = project.manifestLine("scene", String.valueOf(sc.number));
        if (line == null) line = project.manifestLine("scene", sc.number + "a");
        String f = line != null && line.split("\\|").length > 2 ? line.split("\\|")[2] : null;
        return f != null && project.has(f) ? f : null;
    }

    private LinearLayout pageCard(String title, final String kind) {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, title, 16, Ui.TEXT, true));
        String line = project.manifestLine(kind, kind);
        String file = line != null && line.split("\\|").length > 1 ? line.split("\\|")[1] : null;
        if (file != null && project.has(file)) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(thumb(project.file(file), 600));
            c.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 150)));
            final String f = file;
            CheckBox cb = new CheckBox(this);
            cb.setText(kind.equals("title") ? "Write the title on the picture" : "Write \"The End\" on the picture");
            cb.setChecked(!line.endsWith("|0"));
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean on) { project.setManifest(kind, kind, kind + "|" + f + "|" + (on ? "1" : "0")); }
            });
            c.addView(cb);
        } else {
            c.addView(Ui.text(this, "🎨 The studio will make a nice page itself (with music)", 13, Ui.SUB, false));
        }
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "🖼 Choose picture", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { choosePicture(kind, kind.equals("title") ? castStory.title : "end समाप्त"); }
        }));
        if (file != null) r.addView(Ui.small(this, "✖ Remove", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) { project.setManifest(kind, kind, null); showStudio(); }
        }));
        c.addView(r);
        return c;
    }

    private LinearLayout characterCard(final Story st, final Story.CharacterDef c) {
        LinearLayout card = Ui.card(this);
        LinearLayout top = Ui.row(this);
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        final String file = charFile(c);
        String line = project.manifestLine("char", keyFor(c));
        if (file != null) iv.setImageBitmap(thumb(project.file(file), 300));
        else iv.setImageBitmap(puppetThumb(c.look));
        top.addView(iv, new LinearLayout.LayoutParams(Ui.dp(this, 80), Ui.dp(this, 120)));
        LinearLayout info = Ui.column(this);
        info.setPadding(Ui.dp(this, 10), 0, 0, 0);
        info.addView(Ui.text(this, c.shown(), 18, Ui.TEXT, true));
        info.addView(Ui.text(this, describe(c), 13, Ui.SUB, false));
        boolean mouthSet = line != null && line.split("\\|").length >= 11;
        info.addView(Ui.text(this, file != null ? "🖼 Your picture" + (mouthSet ? " • 👄 mouth set" : " • 👄 automatic") : "🎨 Studio cartoon", 13, file != null ? Ui.GREEN : Ui.SUB, false));
        Library.Item vs = library.byId(project.setting("vsample." + c.displayName, ""));
        String vtxt = vs != null ? "🎙 Voice: your sample \"" + vs.label() + "\"" : (Prefs.geminiKey(this).length() > 20 && Prefs.aiVoices(this) ? "✨ Voice: AI (with feeling)"
                : Prefs.online(this) && Prefs.naturalVoices(this) ? "🗣 Voice: " + presetLabel(project.setting("evoice." + c.displayName, ""), "natural (best match)") : "📱 Voice: phone voice");
        // what the description asks of the voice (deep, slow…) and the effects the studio adds (raspy, trembling…)
        List<String> vw = new ArrayList<String>(com.tarun.kahani.core.VoiceMatch.want(c).words);
        vw.addAll(com.tarun.kahani.core.VoiceStyle.forCharacter(c).words);
        if (!vw.isEmpty()) vtxt += "\n🎚 From the story: " + join(vw);
        info.addView(Ui.text(this, vtxt, 13, vs != null ? Ui.GREEN : Ui.SUB, false));
        top.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(top);
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "🖼 Picture", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { choosePicture("char:" + keyFor(c), c.displayName + " " + c.description); }
        }));
        r.addView(Ui.small(this, "🎙 Voice", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { chooseVoice(c.displayName, st, c); }
        }));
        r.addView(Ui.small(this, "🔊", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { previewVoice(st, c, false); }
        }));
        if (file != null) r.addView(Ui.small(this, "👄", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { showFace(keyFor(c)); }
        }));
        if (file != null) r.addView(Ui.small(this, "✖", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) { project.setManifest("char", keyFor(c), null); project.setManifest("char", c.displayName, null); showStudio(); }
        }));
        card.addView(r);
        return card;
    }

    private LinearLayout narratorCard() {
        LinearLayout card = Ui.card(this);
        card.addView(Ui.text(this, "🎙 Narrator (voice only)", 17, Ui.TEXT, true));
        Library.Item vs = library.byId(project.setting("vsample.narrator", ""));
        card.addView(Ui.text(this, vs != null ? "Your sample: " + vs.label() : "Studio voice", 13, Ui.SUB, false));
        card.addView(Ui.small(this, "🎙 Choose voice", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { chooseVoice("narrator", castStory, null); }
        }));
        return card;
    }

    private LinearLayout sceneCard(final Story.Scene sc, SoundLib sl) {
        LinearLayout card = Ui.card(this);
        final String key = String.valueOf(sc.number);
        final String file = sceneFile(sc);
        card.addView(Ui.text(this, (castStory.hindi ? "Part " : "Part ") + sc.number + (sc.title.length() > 0 ? ": " + sc.title : ""), 16, Ui.TEXT, true));
        card.addView(Ui.text(this, "Place: " + Sets.label(Sets.detect(sc.setting)) + "   •   Lines: " + countDialogue(sc), 13, Ui.SUB, false));
        if (file != null) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(thumb(project.file(file), 500));
            card.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 120)));
        } else card.addView(Ui.text(this, "🎨 Background made by the studio", 13, Ui.SUB, false));
        Library.Item amb = library.byId(project.setting("amb." + key, ""));
        SoundLib.Entry auto = sl.best(sc.setting + " " + sc.title, "amb", null);
        card.addView(Ui.text(this, "🔊 Background sound: " + (amb != null ? amb.label() + " (your choice)"
                : auto != null ? auto.title + (auto.user ? " (yours — it fits the description)" : " (automatic)") : "Automatic"), 13, amb != null || (auto != null && auto.user) ? Ui.GREEN : Ui.SUB, false));
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "🖼 Background", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { choosePicture("scene:" + key, sc.setting); }
        }));
        r.addView(Ui.small(this, "🔊 Sound", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { chooseSound("amb:" + key, sc.setting + " " + sc.title); }
        }));
        if (file != null) r.addView(Ui.small(this, "✖", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) { project.setManifest("scene", key, null); project.setManifest("scene", key + "a", null); project.setManifest("scene", key + "b", null); showStudio(); }
        }));
        card.addView(r);
        return card;
    }

    private int countDialogue(Story.Scene sc) {
        int n = 0;
        for (Story.Beat b : sc.beats) if (b.type == Story.Beat.DIALOGUE) n++;
        return n;
    }

    /** Manifest lines may use any alias of the character (e.g. the sample uses "राजू" for "राजू बंदर"). */
    private String findCharLine(Story.CharacterDef c) {
        for (String l : project.read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length >= 3 && f[0].equals("char") && resolve(f[1]) == c) return l;
        }
        return null;
    }

    private String keyFor(Story.CharacterDef c) {
        for (String l : project.read("cast.txt").split("\n")) {
            String[] f = l.split("\\|");
            if (f.length >= 3 && f[0].equals("char") && resolve(f[1]) == c) return f[1];
        }
        return c.displayName;
    }

    private Story.CharacterDef resolve(String name) {
        if (castStory == null) return null;
        for (Story.CharacterDef c : castStory.characters) {
            if (c.displayName.equals(name)) return c;
            for (String a : c.aliases) if (a.equals(name)) return c;
        }
        return null;
    }

    private String describe(Story.CharacterDef c) {
        Look l = c.look;
        String kind;
        switch (l.kind) {
            case Look.GIRL: kind = "girl"; break;
            case Look.BOY: kind = "boy"; break;
            case Look.WOMAN: kind = "woman"; break;
            case Look.WITCH: kind = "witch"; break;
            case Look.MONSTER: kind = "monster"; break;
            case Look.MONKEY: kind = "monkey"; break;
            case Look.ANIMAL: kind = "animal"; break;
            case Look.BIRD: kind = "bird"; break;
            default: kind = "man";
        }
        return kind + (c.age > 0 ? ", " + c.age + " yrs" : "") + (l.hero ? "" : " • villain") + (c.fromScript ? "" : " • (no description found)");
    }

    static Bitmap thumb(File f, int max) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeFile(f.getAbsolutePath(), o);
            int s = 1;
            while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= max) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            return BitmapFactory.decodeFile(f.getAbsolutePath(), o);
        } catch (Throwable e) {
            return null;
        }
    }

    private Bitmap puppetThumb(Look look) {
        try {
            Bitmap b = Bitmap.createBitmap(160, 240, Bitmap.Config.ARGB_8888);
            AndroidGfx g = new AndroidGfx(b, 1);
            g.color(0xFFF3E9D8);
            g.rect(0, 0, 160, 240);
            Pose p = new Pose();
            p.emotion = Pose.HAPPY;
            p.time = 1;
            g.save();
            g.translate(80, 228);
            float H = 215 * Math.min(1f, look.height) / Math.max(0.6f, Math.min(1.45f, look.height));
            if (look.kind == Look.MONKEY) H = 150;
            Puppet.draw(g, look, p, H);
            g.restore();
            return b;
        } catch (Throwable e) {
            return null;
        }
    }

    // ================================================================== production file

    /** Tries a key with one small request; returns a short note, or throws with what went wrong. */
    private String testKey(String which) throws Exception {
        com.tarun.kahani.core.Cloud c = Prefs.cloud(this);
        String key = Prefs.get(this, which, "").trim();
        if (key.length() < 10) throw new Exception("Paste the key first");
        if (which.equals("elevenKey")) {
            java.util.List<com.tarun.kahani.core.Eleven.Voice> v = new com.tarun.kahani.core.Eleven(c, key).voices();
            return v.size() + " voices on your account";
        }
        if (which.equals("freesoundKey")) {
            c.pixabayKey = ""; c.pexelsKey = "";
            java.util.List<com.tarun.kahani.core.Cloud.Found> f = c.searchSounds("rain", 3);
            for (com.tarun.kahani.core.Cloud.Found x : f) if ("Freesound".equals(x.source)) return "found \"" + x.title + "\"";
            throw new Exception(c.lastError.length() > 0 ? c.lastError : "Freesound gave no result");
        }
        if (which.equals("pixabayKey") || which.equals("pexelsKey")) {
            if (which.equals("pixabayKey")) c.pexelsKey = ""; else c.pixabayKey = "";
            java.util.List<com.tarun.kahani.core.Cloud.Found> f = c.searchPictures("garden", 3);
            String want = which.equals("pixabayKey") ? "Pixabay" : "Pexels";
            for (com.tarun.kahani.core.Cloud.Found x : f) if (want.equals(x.source)) return "found a picture";
            throw new Exception(c.lastError.length() > 0 ? c.lastError : want + " gave no result");
        }
        return null;
    }

    private void productionFile(final Story st) {
        final Set<String> pics = new HashSet<String>(), voices = new HashSet<String>();
        for (Story.CharacterDef c : st.characters) {
            if (charFile(c) != null) pics.add(com.tarun.kahani.core.Txt.norm(c.displayName));
            if (library.byId(project.setting("vsample." + c.displayName, "")) != null) voices.add(com.tarun.kahani.core.Txt.norm(c.displayName));
        }
        final SoundLib sl = library.soundLib();
        background("Making the production file…", new Work() {
            public Object run() {
                // the production file, then the full Technical Director package (lock sheets, plates, objects,
                // sounds, voices and every shot with ready prompts)
                return Bible.write(st, sl, pics, voices) + "\n\n" + com.tarun.kahani.core.ShotBook.write(st, sl, edits().aspect);
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("Could not make it: " + e.getMessage()); return; }
                final String text = (String) r;
                final String name = safeName(st.title) + "_production.txt";
                new AlertDialog.Builder(MainActivity.this).setTitle("📄 Production file ready")
                        .setMessage("Every character (costume locked word for word), place, object, shot, sound and voice — with ready first-frame and video prompts for other apps, and the director's shot list. Make the pictures, clips, voices or sounds elsewhere and upload them here: the studio places them by name.")
                        .setPositiveButton("💾 Download", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { saveTextFile(name, text); }
                        })
                        .setNeutralButton("📤 Share", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { shareText(name, text); }
                        })
                        .setNegativeButton("👁 Read here", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { showText("Production file", text); }
                        }).show();
            }
        });
    }

    private void saveTextFile(String name, String text) {
        pendingText = text;
        pendingTextName = name;
        try {
            Intent i = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.setType("text/plain");
            i.putExtra(Intent.EXTRA_TITLE, name);
            startActivityForResult(i, REQ_SAVE_TEXT);
        } catch (Exception e) {
            saveTextToDownloads(name, text);
        }
    }

    private void saveTextToDownloads(String name, String text) {
        try {
            if (Build.VERSION.SDK_INT >= 29) {
                ContentValues v = new ContentValues();
                v.put(MediaStore.MediaColumns.DISPLAY_NAME, name);
                v.put(MediaStore.MediaColumns.MIME_TYPE, "text/plain");
                v.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
                Uri uri = getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, v);
                if (uri == null) throw new Exception("Downloads folder not available");
                OutputStream o = getContentResolver().openOutputStream(uri);
                o.write(text.getBytes("UTF-8"));
                o.close();
                toast("✅ Saved to Downloads: " + name);
            } else shareText(name, text);
        } catch (Exception e) {
            toast("Could not save: " + e.getMessage());
        }
    }

    private void shareText(String name, String text) {
        try {
            File f = new File(FilesProvider.sharedDir(this), name.replaceAll("[^\\p{L}\\p{M}\\p{N}_.-]", "_"));
            FileOutputStream o = new FileOutputStream(f);
            o.write(text.getBytes("UTF-8"));
            o.close();
            Intent s = new Intent(Intent.ACTION_SEND);
            s.setType("text/plain");
            s.putExtra(Intent.EXTRA_STREAM, FilesProvider.uriFor(f));
            s.putExtra(Intent.EXTRA_SUBJECT, name);
            s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            startActivity(Intent.createChooser(s, "Share"));
        } catch (Exception e) {
            toast("Could not share");
        }
    }

    // ================================================================== choosing pictures

    private String targetName(String t) {
        if (t.startsWith("char:")) return t.substring(5);
        if (t.startsWith("scene:")) return (castStory != null && castStory.hindi ? "Part " : "Part ") + t.substring(6);
        if (t.equals("title")) return "Title";
        if (t.equals("end")) return "End";
        return "";
    }

    private void choosePicture(final String tgt, final String query) {
        target = tgt;
        new Picker(this, library).show("Picture: " + targetName(tgt), Library.PIC, query,
                new String[]{"phone", "camera", "online", "ai", "auto"}, new Picker.Listener() {
                    public void picked(Library.Item it) { usePicture(it); }
                    public void action(String a) {
                        if (a.equals("phone")) pick("image/*", REQ_IMAGE, false);
                        else if (a.equals("camera")) camera();
                        else if (a.equals("online")) searchPictures(query);
                        else if (a.equals("ai")) aiPicture();
                        else { clearTarget(tgt); showStudio(); }
                    }
                });
    }

    private void clearTarget(String tgt) {
        if (tgt.startsWith("char:")) project.setManifest("char", tgt.substring(5), null);
        else if (tgt.startsWith("scene:")) { String k = tgt.substring(6); project.setManifest("scene", k, null); project.setManifest("scene", k + "a", null); project.setManifest("scene", k + "b", null); }
        else if (tgt.equals("title") || tgt.equals("end")) project.setManifest(tgt, tgt, null);
        else if (tgt.startsWith("voice:")) project.setSetting("vsample." + tgt.substring(6), "");
        else if (tgt.startsWith("amb:")) project.setSetting("amb." + tgt.substring(4), "");
    }

    /** Puts a library picture into the project for the current target. */
    private void usePicture(final Library.Item it) {
        final String tgt = target;
        if (tgt == null || project == null || tgt.startsWith("lib")) { if (screen == S_LIBRARY) showLibrary(); return; }
        background("Adding the picture…", new Work() {
            public Object run() throws Exception {
                InputStream in = library.open(it);
                return project.savePicture(Project.readAll(in), tgt.startsWith("char:") ? "char" : tgt.startsWith("scene:") ? "scene" : tgt);
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("Could not add the picture: " + e.getMessage()); return; }
                String f = (String) r;
                if (tgt.startsWith("char:")) {
                    String name = tgt.substring(5);
                    project.setManifest("char", name, "char|" + name + "|" + f);
                    showFace(name);
                    return;
                } else if (tgt.startsWith("scene:")) {
                    String k = tgt.substring(6);
                    project.setManifest("scene", k + "a", null);
                    project.setManifest("scene", k + "b", null);
                    project.setManifest("scene", k, "scene|" + k + "|" + f);
                } else if (tgt.equals("title") || tgt.equals("end")) {
                    project.setManifest(tgt, tgt, tgt + "|" + f + "|1");
                }
                toast("✅ Added");
                showStudio();
            }
        });
    }

    /** A picture arrived from the phone or camera: offer the cartoon (avatar) look, save to library, use it. */
    /**
     * A picture arrived (phone, camera, download). Real camera photos become animated avatars automatically;
     * for other pictures the user decides with one tap. Everything is saved in the library for later stories.
     */
    private void incomingPicture(final byte[] data, final String fileName) {
        final String tgt = target == null ? "lib:pic" : target;
        final boolean person = tgt.startsWith("char:") || tgt.equals("lib:pic");
        boolean camera = "camera".equals(fileName) || Library.cameraPhoto(data);
        if (camera) {
            toast("📷 Real photo — turning it into an animated avatar…");
            savePicture(data, fileName, tgt, true, person);
            return;
        }
        new AlertDialog.Builder(this).setTitle("Is this a real photo?")
                .setMessage(person ? "Real photos of people are turned into an animated avatar so they fit the cartoon world (the background is removed)."
                        : "Real photos of places can be turned into the animated style of the film.")
                .setPositiveButton("🎨 Make animated avatar", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { savePicture(data, fileName, tgt, true, person); }
                })
                .setNegativeButton("It's artwork — keep it", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { savePicture(data, fileName, tgt, false, person); }
                }).show();
    }

    private void savePicture(final byte[] data, final String fileName, final String tgt, final boolean toon, final boolean person) {
        background(toon ? "Making the animated avatar…" : "Saving…", new Work() {
            public Object run() throws Exception {
                byte[] bytes = toon ? toonify(data, person) : data;
                String kind = tgt.startsWith("char:") ? "person" : tgt.startsWith("scene:") ? "place" : tgt.equals("title") || tgt.equals("end") ? tgt : "";
                String name = targetName(tgt);
                if (name.length() == 0 && fileName != null) name = fileName.replaceAll("\\.[A-Za-z0-9]+$", "");
                String ext = toon && person ? ".png" : ".jpg";
                Library.Item it = library.addBytes(Library.PIC, kind, name, fileName == null ? "" : fileName, bytes, ext, toon ? "photo → avatar" : "phone");
                if (toon) { it.setMeta("avatar", "1"); library.save(); }
                return it;
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("Could not do it: " + e.getMessage()); return; }
                usePicture((Library.Item) r);
            }
        });
    }

    static byte[] toonify(byte[] data) throws Exception { return toonify(data, false); }

    static byte[] toonify(byte[] data, boolean person) throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        int s = 1;
        while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= 1200) s *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = s;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, o);
        if (b == null) throw new Exception("Could not read the picture");
        b = Project.upright(b, data);
        int w = b.getWidth(), h = b.getHeight();
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        b.recycle();
        int[] t = Toon.avatar(px, w, h, person);
        Bitmap out = Bitmap.createBitmap(t, w, h, Bitmap.Config.ARGB_8888);
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        out.compress(person ? Bitmap.CompressFormat.PNG : Bitmap.CompressFormat.JPEG, 92, bo);
        out.recycle();
        return bo.toByteArray();
    }

    private void camera() {
        withPermission(Manifest.permission.CAMERA, "Please allow the camera", new Runnable() {
            public void run() {
                try {
                    cameraFile = new File(FilesProvider.sharedDir(MainActivity.this), "cam_" + System.currentTimeMillis() + ".jpg");
                    Intent i = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                    Uri u = FilesProvider.uriFor(cameraFile);
                    i.putExtra(MediaStore.EXTRA_OUTPUT, u);
                    i.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION | Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    i.setClipData(ClipData.newRawUri("photo", u));
                    startActivityForResult(i, REQ_CAMERA);
                } catch (Exception e) {
                    toast("Could not open the camera");
                }
            }
        });
    }

    private void searchPictures(String query) {
        if (!Prefs.online(this)) { toast("Turn on online features in Settings"); return; }
        final EditText q = new EditText(this);
        String s = query == null ? "" : query;
        q.setText(s.length() > 60 ? s.substring(0, 60) : s);
        new AlertDialog.Builder(this).setTitle("🌐 Search free pictures (Openverse, Wikimedia)").setView(q)
                .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        final String text = q.getText().toString().trim();
                        background("Searching…", new Work() {
                            public Object run() throws Exception {
                                Cloud c = Prefs.cloud(MainActivity.this);
                                List<Cloud.Found> fs = c.searchPictures(text, 12);
                                List<Object[]> out = new ArrayList<Object[]>();
                                for (Cloud.Found f : fs) {
                                    try {
                                        byte[] t = c.download(f.thumb.length() > 0 ? f.thumb : f.url);
                                        Bitmap b = decodeSmall(t, 400);
                                        if (b != null) out.add(new Object[]{f, b});
                                    } catch (Throwable ignored) {}
                                }
                                if (out.isEmpty()) throw new Exception(c.lastError.length() > 0 ? c.lastError : "Nothing found");
                                return out;
                            }
                        }, new Done() {
                            @SuppressWarnings("unchecked")
                            public void done(Object r, Exception e) {
                                if (e != null) { toast("Not found: " + e.getMessage()); return; }
                                showFound((List<Object[]>) r, text);
                            }
                        });
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void showFound(final List<Object[]> found, final String query) {
        final AlertDialog[] d = new AlertDialog[1];
        final LinearLayout body = Ui.column(this);
        final LinearLayout grid = Ui.column(this);
        final int[] page = {0};
        final Runnable[] fill = new Runnable[1];
        fill[0] = new Runnable() {
            public void run() {
                grid.removeAllViews();
                LinearLayout row = null;
                for (int i = page[0] * 4; i < Math.min(found.size(), page[0] * 4 + 4); i++) {
                    final Cloud.Found f = (Cloud.Found) found.get(i)[0];
                    if ((i % 4) % 2 == 0) { row = Ui.row(MainActivity.this); grid.addView(row); }
                    ImageView iv = new ImageView(MainActivity.this);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setImageBitmap((Bitmap) found.get(i)[1]);
                    iv.setPadding(Ui.dp(MainActivity.this, 3), Ui.dp(MainActivity.this, 3), Ui.dp(MainActivity.this, 3), Ui.dp(MainActivity.this, 3));
                    iv.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) { d[0].dismiss(); downloadFound(f, query); }
                    });
                    row.addView(iv, new LinearLayout.LayoutParams(0, Ui.dp(MainActivity.this, 120), 1f));
                }
            }
        };
        body.addView(grid);
        body.addView(Ui.small(this, "Next 4 ▶", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { page[0] = (page[0] + 1) * 4 < found.size() ? page[0] + 1 : 0; fill[0].run(); }
        }));
        body.addView(Ui.text(this, "These pictures have free licences (the source is noted in your library).", 12, Ui.SUB, false));
        fill[0].run();
        d[0] = new AlertDialog.Builder(this).setTitle("Choose a picture").setView(body).setNegativeButton("Close", null).show();
    }

    private void downloadFound(final Cloud.Found f, final String query) {
        background("Downloading the picture…", new Work() {
            public Object run() throws Exception {
                byte[] b = Prefs.cloud(MainActivity.this).download(f.url);
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(b, 0, b.length, o);
                if (o.outWidth <= 0) throw new Exception("This picture could not be opened");
                return b;
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("Download failed: " + e.getMessage()); return; }
                incomingPicture((byte[]) r, f.title + " (" + f.source + " " + f.license + " " + f.creator + ")");
            }
        });
    }

    private void aiPicture() {
        if (!Prefs.online(this)) { toast("Turn on online features in Settings"); return; }
        final String tgt = target;
        String prompt;
        final boolean tall;
        if (tgt != null && tgt.startsWith("char:") && resolve(tgt.substring(5)) != null) {
            prompt = Bible.characterPrompt(resolve(tgt.substring(5)));
            tall = true;
        } else if (tgt != null && tgt.startsWith("scene:")) {
            String setting = "";
            for (Story.Scene sc : castStory.scenes) if (String.valueOf(sc.number).equals(tgt.substring(6))) setting = sc.setting;
            prompt = Bible.placePrompt(targetName(tgt), setting, edits().aspect);
            tall = false;
        } else if ("title".equals(tgt)) {
            prompt = "3D animated movie poster for a premium Indian children's film named '" + castStory.title + "', main characters together, cinematic lighting, depth of field, no text.";
            tall = false;
        } else {
            prompt = "Beautiful calm sunset landscape, cinematic 3D animated film style, volumetric light, for the ending of a children's film, no text.";
            tall = false;
        }
        final EditText et = new EditText(this);
        et.setText(prompt);
        et.setMaxLines(8);
        new AlertDialog.Builder(this).setTitle("✨ AI picture (free, no key)").setView(et)
                .setPositiveButton("Make", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { makeAiPicture(et.getText().toString(), tall); }
                }).setNegativeButton("Cancel", null).show();
    }

    private void makeAiPicture(final String prompt, final boolean tall) {
        final int seed = (int) (System.currentTimeMillis() % 100000);
        background("AI is drawing… (30-60 seconds)", new Work() {
            public Object run() throws Exception {
                // characters are made tall (native, never cropped); places in the film's shape (FINAL_AR, a parameter)
                int[] plate = com.tarun.kahani.core.TechnicalDirector.sizeFor(edits().aspect);
                return Prefs.cloud(MainActivity.this).makePicture(com.tarun.kahani.core.TechnicalDirector.clean(prompt), tall ? 768 : plate[0], tall ? 1152 : plate[1], seed);
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("Could not make the picture: " + e.getMessage()); return; }
                final byte[] b = (byte[]) r;
                ImageView iv = new ImageView(MainActivity.this);
                iv.setAdjustViewBounds(true);
                iv.setImageBitmap(decodeSmall(b, 900));
                new AlertDialog.Builder(MainActivity.this).setTitle("Do you like it?").setView(iv)
                        .setPositiveButton("✔ Use it", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                try {
                                    String tgt = target == null ? "lib:pic" : target;
                                    Library.Item it = library.addBytes(Library.PIC, tgt.startsWith("char:") ? "person" : tgt.startsWith("scene:") ? "place" : tgt,
                                            targetName(tgt), prompt.length() > 200 ? prompt.substring(0, 200) : prompt, b, ".jpg", "AI");
                                    usePicture(it);
                                } catch (Exception ex) { toast("Could not save"); }
                            }
                        })
                        .setNeutralButton("↻ Again", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { makeAiPicture(prompt, tall); }
                        }).setNegativeButton("Cancel", null).show();
            }
        });
    }

    // ================================================================== voices

    /**
     * One list of voices: the user's own recordings and the studio's built-in voices, 4 at a time, with the
     * ones that suit this character (by age, gender and kind from the script) marked ★ and shown first.
     */
    private void chooseVoice(final String name, final Story st, final Story.CharacterDef c) {
        target = "voice:" + name;
        Picker p = new Picker(this, library);
        boolean hindi = st == null || st.hindi;
        // the voice the script describes for this character ("भारी, धीमी आवाज़", "a sweet voice"…)
        com.tarun.kahani.core.VoiceMatch.Want want = c == null ? null : com.tarun.kahani.core.VoiceMatch.want(c);
        com.tarun.kahani.core.EdgeVoice.Cast best = c == null ? com.tarun.kahani.core.EdgeVoice.narrator(hindi)
                : com.tarun.kahani.core.VoiceMatch.adjust(com.tarun.kahani.core.EdgeVoice.castFor(c.look, c.age, hindi, st.characters.indexOf(c)), want);
        for (String[] pr : com.tarun.kahani.core.EdgeVoice.presets(hindi)) {
            Library.Item it = new Library.Item();
            it.id = "preset:" + pr[0];
            it.path = it.id;
            it.type = Library.VOICE;
            it.name = pr[1] + " (studio)";
            it.builtIn = true;
            p.extra.add(it);
            com.tarun.kahani.core.EdgeVoice.Cast pc = com.tarun.kahani.core.EdgeVoice.Cast.parse(pr[0]);
            if (pc != null && pc.voice.equals(best.voice) && Math.abs(pc.pitchHz - best.pitchHz) <= 12) p.suggested.add(it.id);
        }
        Library.Item bestItem = new Library.Item();
        bestItem.id = "preset:" + best.key();
        bestItem.path = bestItem.id;
        bestItem.type = Library.VOICE;
        bestItem.name = "Best match for " + (c == null ? "the narrator" : c.shown()) + " (studio)";
        bestItem.builtIn = true;
        p.extra.add(0, bestItem);
        p.suggested.add(bestItem.id);
        p.fit.put(bestItem.id, 0.8f);
        if (want != null) {
            // only what a studio voice can really change (it can't become raspy)
            List<String> made = new ArrayList<String>();
            for (String w : want.words) if (!w.startsWith("raspy") && !w.startsWith("loud")) made.add(w);
            if (!made.isEmpty()) p.note.put(bestItem.id, "Made " + join(made) + ", as the story describes");
        }
        // your own voices: measured once, then compared with the description
        for (Library.Item it : library.find(Library.VOICE, null, null)) {
            if (it.builtIn) continue;
            float[] vf = Library.voiceFeatures(it);
            if (vf == null) { p.note.put(it.id, it.meta("vf") == null ? "Still being measured…" : "No clear voice found in this recording"); continue; }
            String heard = com.tarun.kahani.core.VoiceMatch.describe(vf);
            if (want == null) { p.note.put(it.id, heard); continue; }
            float sc = com.tarun.kahani.core.VoiceMatch.score(vf, want);
            p.fit.put(it.id, sc);
            String fits = com.tarun.kahani.core.VoiceMatch.fits(vf, want);
            if (sc >= 0.72f) p.suggested.add(it.id);
            p.note.put(it.id, heard + (sc >= 0.72f && fits.length() > 0 ? "  •  fits: " + fits : ""));
        }
        final String line = sampleLine(st, c);
        p.presetPlayer = new Picker.Player() {
            public void play(final Library.Item it) { speakPreset(it.path.substring(7), line, st == null || st.hindi); }
        };
        p.show("Voice: " + (name.equals("narrator") ? "Narrator" : c != null ? c.shown() : name), Library.VOICE, name,
                new String[]{"record", "phone"}, new Picker.Listener() {
                    public void picked(Library.Item it) {
                        if (it.path.startsWith("preset:")) {
                            project.setSetting("vsample." + name, "");
                            project.setSetting("evoice." + name, it.path.substring(7));
                            project.setSetting("evoiceIdx." + name, "");
                            toast("✅ " + (c != null ? c.shown() : "Narrator") + " will speak with: " + it.label());
                        } else {
                            project.setSetting("vsample." + name, it.id);
                            toast("✅ All of " + (c != null ? c.shown() : "the narrator") + "'s lines will be made in your voice \"" + it.label() + "\"");
                            if (c != null) previewVoice(st, c, true);
                        }
                        showStudio();
                    }
                    public void action(String a) {
                        if (a.equals("record")) record(Library.VOICE, name);
                        else if (a.equals("phone")) pick("audio/*", REQ_AUDIO, false);
                    }
                });
    }

    static String join(List<String> words) {
        StringBuilder b = new StringBuilder();
        for (String w : words) { if (b.length() > 0) b.append(", "); b.append(w); }
        return b.toString();
    }

    static String presetLabel(String key, String def) {
        if (key == null || key.length() == 0) return def;
        for (String[] p : com.tarun.kahani.core.EdgeVoice.presets(true)) if (p[0].equals(key)) return p[1];
        com.tarun.kahani.core.EdgeVoice.Cast c = com.tarun.kahani.core.EdgeVoice.Cast.parse(key);
        return c == null ? def : c.voice.replace("Neural", "").replaceAll("^[a-z]{2}-[A-Z]{2}-", "") + " (custom)";
    }

    private String sampleLine(Story st, Story.CharacterDef c) {
        String sample = st != null && st.hindi ? "नमस्ते! आज हम एक नई कहानी सुनाते हैं।" : "Hello! Let me tell you a new story today.";
        if (st != null && c != null) for (Story.Scene sc : st.scenes) for (Story.Beat b : sc.beats) if (b.speaker == c) { sample = b.text; break; }
        return sample.length() > 110 ? sample.substring(0, 110) : sample;
    }

    /** Speaks a line with one of the studio's built-in voices (online natural voices) and plays it. */
    private void speakPreset(final String key, final String text, final boolean hindi) {
        if (previewBusy) { toast("Still making the last voice… please wait"); return; }
        previewBusy = true;
        toast("🔊 Making the voice…");
        background(null, new Work() {
            public Object run() throws Exception {
                com.tarun.kahani.core.EdgeVoice.Cast c = com.tarun.kahani.core.EdgeVoice.Cast.parse(key);
                boolean lineHindi = com.tarun.kahani.core.Txt.mostlyHindi(text);
                boolean presetHindi = c.voice.startsWith("hi-");
                String say = presetHindi == lineHindi ? text
                        : presetHindi ? "नमस्ते! आज हम एक नई कहानी सुनाते हैं।" : "Hello! Let me tell you a new story today.";
                byte[] mp3 = new com.tarun.kahani.core.EdgeVoice().speak(com.tarun.kahani.core.Txt.forSpeech(say), c, null);
                File f = new File(getCacheDir(), "preset_preview.mp3");
                FileOutputStream o = new FileOutputStream(f);
                o.write(mp3);
                o.close();
                return f;
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                previewBusy = false;
                if (e != null) { toast("Could not reach the studio voices (check internet): " + e.getMessage()); return; }
                Picker.play(MainActivity.this, ((File) r).getAbsolutePath());
            }
        });
    }

    /** Plays one of the character's lines in the voice the film will use (with the sample applied if any). */
    private void previewVoice(final Story st, final Story.CharacterDef c, final boolean keepVoice) {
        if (previewBusy) { toast("Still making the last voice… please wait"); return; }
        previewBusy = true;
        final Library.Item vs = library.byId(project.setting("vsample." + c.displayName, ""));
        String sample = st.hindi ? "नमस्ते, मैं " + c.displayName + " हूँ।" : "Hello, I am " + c.displayName + ".";
        for (Story.Scene sc : st.scenes) for (Story.Beat b : sc.beats) if (b.speaker == c) { sample = b.text; break; }
        if (sample.length() > 110) sample = sample.substring(0, 110);
        final String text = sample;
        background(previewVoices == null ? "Preparing the voice…" : null, new Work() {
            public Object run() throws Exception {
                boolean natural = Prefs.online(MainActivity.this) && Prefs.naturalVoices(MainActivity.this);
                if (previewVoices == null) {
                    Voices v = new Voices();
                    if (!v.init(MainActivity.this, st.hindi) && !natural) throw new Exception("No Text-to-Speech found on this phone");
                    previewVoices = v;
                }
                previewVoices.edgeOff = false;
                previewVoices.edge = natural ? new com.tarun.kahani.core.EdgeVoice() : null;
                Map<Story.CharacterDef, Voices.Cast> cast = previewVoices.castAll(st, project);
                Voices.Cast k = cast.get(c);
                if (k == null) k = Voices.defaultCast(c.look);
                int n = previewVoices.voices.size();
                int choice = 0, choices = 0;
                if (vs != null) {
                    float[] pcm = AudioIO.decode(MainActivity.this, vs.path, 60);
                    if (pcm != null) { k.sample = com.tarun.kahani.core.VoiceFx.profile(pcm, com.tarun.kahani.core.Synth.SR); k.sampleId = vs.id; }
                }
                if (natural) {
                    // natural voices: each tap on 🔊 tries the next variation and remembers it
                    List<com.tarun.kahani.core.EdgeVoice.Cast> opts = com.tarun.kahani.core.EdgeVoice.options(c.look, c.age, st.hindi, st.characters.indexOf(c));
                    com.tarun.kahani.core.VoiceMatch.Want want = com.tarun.kahani.core.VoiceMatch.want(c);
                    for (int i = 0; i < opts.size(); i++) opts.set(i, com.tarun.kahani.core.VoiceMatch.adjust(opts.get(i), want));
                    String key = "evoiceIdx." + c.displayName;
                    String cur = project.setting(key, "");
                    try { choice = cur.length() == 0 ? 0 : Integer.parseInt(cur); } catch (NumberFormatException ignored) {}
                    if (!keepVoice && vs == null && cur.length() > 0) choice = (choice + 1) % opts.size();
                    choices = opts.size();
                    project.setSetting(key, String.valueOf(choice));
                    project.setSetting("evoice." + c.displayName, opts.get(choice).key());
                    k.edge = k.sample != null ? com.tarun.kahani.core.EdgeVoice.forSample(k.sample.pitch, st.hindi) : opts.get(choice);
                } else {
                    if (!keepVoice && vs == null && n > 0 && project.setting("voice." + c.displayName, "").length() > 0) k.voice = (k.voice + 1) % n;
                    if (n > 0) project.setSetting("voice." + c.displayName, String.valueOf(k.voice < 0 ? 0 : k.voice));
                    choice = k.voice < 0 ? 0 : k.voice;
                    choices = n;
                }
                Cloud cl = Prefs.online(MainActivity.this) && Prefs.aiVoices(MainActivity.this) ? Prefs.cloud(MainActivity.this) : null;
                if (cl != null && cl.hasGemini()) {
                    String gv = project.setting("gvoice." + c.displayName, "");
                    k.gemini = gv.length() > 0 ? gv : Cloud.geminiVoiceFor(c.look, st.characters.indexOf(c));
                }
                Film.Line l = new Film.Line();
                l.text = com.tarun.kahani.core.Txt.forSpeech(text);
                l.who = c;
                l.emotion = Pose.NEUTRAL;
                File tmp = new File(getCacheDir(), "preview");
                tmp.mkdirs();
                float[] pcm = previewVoices.speak(l, k, tmp, 0, cl, null);
                if (pcm == null) throw new Exception("Could not make the voice — check Text-to-Speech settings");
                File f = new File(tmp, "preview.wav");
                AudioIO.writeWav(f, pcm, com.tarun.kahani.core.Synth.SR);
                return new Object[]{f, choice, choices, previewVoices.lastEngine, previewVoices.languageOk};
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                previewBusy = false;
                if (e != null) { toast(e.getMessage()); offerTtsInstall(); return; }
                Object[] o = (Object[]) r;
                Picker.play(MainActivity.this, ((File) o[0]).getAbsolutePath());
                String eng = (String) o[3];
                String pos = " " + ((Integer) o[1] + 1) + "/" + Math.max(1, (Integer) o[2]) + " — tap 🔊 again for another";
                toast("🔊 " + (eng.contains("sample") ? "Voice from your sample" : eng.startsWith("AI") ? "AI voice"
                        : eng.startsWith("natural") ? "Natural voice" + pos : "Phone voice" + pos));
                if (eng.startsWith("phone") && previewVoices.edge != null) toast("Natural voice service not reachable (check internet) — you heard the phone voice");
                else if (!(Boolean) o[4] && eng.startsWith("phone")) offerTtsInstall();
            }
        });
    }

    private void offerTtsInstall() {
        new AlertDialog.Builder(this).setTitle("Phone voice")
                .setMessage("If the Hindi voice is missing, download the Hindi voice in Google Text-to-speech.")
                .setPositiveButton("Download", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        try { startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)); }
                        catch (Exception e) { toast("Go to Settings > Language > Text-to-speech"); }
                    }
                }).setNegativeButton("Later", null).show();
    }

    // ================================================================== sounds

    private void chooseSound(final String tgt, final String query) {
        target = tgt;
        new Picker(this, library).show("Background sound", Library.SOUND, query, new String[]{"record", "phone", "online", "auto"},
                new Picker.Listener() {
                    public void picked(Library.Item it) { useSound(it); }
                    public void action(String a) {
                        if (a.equals("record")) record(Library.SOUND, "");
                        else if (a.equals("phone")) pick("audio/*", REQ_AUDIO, false);
                        else if (a.equals("online")) searchSounds(query);
                        else { clearTarget(tgt); showStudio(); }
                    }
                });
    }

    private void useSound(final Library.Item it) {
        // a new sound: say what it is (pre-filled with what the studio heard), so it is used in the right places
        if (Library.SOUND.equals(it.type) && !it.builtIn && !"1".equals(it.meta("kindSet"))) {
            describeSound(it, new Runnable() { public void run() { useSoundNow(it); } });
            return;
        }
        useSoundNow(it);
    }

    /** "What is this sound?": its words (English or Hindi) and whether it loops, plays once, is music or voices. */
    private void describeSound(final Library.Item it, final Runnable then) {
        LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 16), Ui.dp(this, 6), Ui.dp(this, 16), Ui.dp(this, 6));
        String heard = it.meta("ai") != null ? "AI heard: " + it.meta("ai")
                : it.meta("sl") != null && it.meta("sl").length() > 0 ? "Sounds like (a guess): " + it.meta("sl") : "";
        if (heard.length() > 0) body.addView(Ui.text(this, heard, 13, Ui.SUB, false));
        body.addView(Ui.text(this, "Words for this sound — the director plays it where the story mentions them (English or Hindi):", 14, Ui.TEXT, false));
        final EditText words = new EditText(this);
        String given = Library.meaningful(it.name + "," + it.tags);
        String guess = it.meta("ai") != null ? it.meta("ai") : it.meta("sl") == null ? "" : it.meta("sl");
        words.setText(given.length() >= 3 ? given : guess);
        words.setHint("e.g. rain, बारिश  •  horse galloping  •  people talking in a market");
        body.addView(words);
        final RadioGroup rg = new RadioGroup(this);
        final String[][] kinds = {{"amb", "Background — loops under a scene (rain, river, birds, wind)"}, {"voices", "Background voices — people talking, a crowd"},
                {"sfx", "Effect — plays once (door, thunder, horse, bell)"}, {"music", "Music"}};
        String cur = "voices".equals(it.meta("sk")) && "amb".equals(it.kind) ? "voices" : it.kind;
        for (int i = 0; i < kinds.length; i++) {
            RadioButton b = new RadioButton(this);
            b.setId(1000 + i);
            b.setText(kinds[i][1]);
            rg.addView(b);
            if (kinds[i][0].equals(cur)) rg.check(1000 + i);
        }
        if (rg.getCheckedRadioButtonId() < 0) rg.check(1000);
        body.addView(rg);
        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        new AlertDialog.Builder(this).setTitle("🔊 What is this sound?").setView(sv)
                .setPositiveButton("✔ Save", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        int k = Math.max(0, Math.min(kinds.length - 1, rg.getCheckedRadioButtonId() - 1000));
                        String kind = kinds[k][0];
                        it.kind = kind.equals("voices") ? "amb" : kind;
                        if (kind.equals("voices")) it.setMeta("sk", "voices");
                        String ws = words.getText().toString().trim().replace(';', ',');
                        if (ws.length() > 0) it.tags = ws + (kind.equals("voices") ? ", people talking, crowd" : "");
                        it.setMeta("kindSet", "1");
                        library.save();
                        then.run();
                    }
                }).setNegativeButton("Later", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { then.run(); }
                }).show();
    }

    private void useSoundNow(Library.Item it) {
        String tgt = target;
        if (tgt != null && tgt.startsWith("amb:") && project != null) {
            project.setSetting("amb." + tgt.substring(4), it.id);
            toast("✅ Background sound set");
            showStudio();
        } else if (tgt != null && tgt.startsWith("voice:") && project != null) {
            project.setSetting("vsample." + tgt.substring(6), it.id);
            toast("✅ Voice sample set — all lines will be made in this voice");
            showStudio();
        } else if (screen == S_LIBRARY) showLibrary();
    }

    private void searchSounds(String query) {
        if (!Prefs.online(this)) { toast("Turn on online features in Settings"); return; }
        final EditText q = new EditText(this);
        q.setHint("e.g. forest birds, rain, river, crowd");
        String s = query == null ? "" : query;
        q.setText(s.length() > 40 ? s.substring(0, 40) : s);
        new AlertDialog.Builder(this).setTitle("🌐 Search free sounds (English words work best)").setView(q)
                .setPositiveButton("Search", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        final String text = q.getText().toString().trim();
                        background("Searching…", new Work() {
                            public Object run() throws Exception {
                                Cloud c = Prefs.cloud(MainActivity.this);
                                List<Cloud.Found> fs = c.searchSounds(text, 12);
                                if (fs.isEmpty()) throw new Exception(c.lastError.length() > 0 ? c.lastError : "Nothing found");
                                return fs;
                            }
                        }, new Done() {
                            @SuppressWarnings("unchecked")
                            public void done(Object r, Exception e) {
                                if (e != null) { toast("Not found: " + e.getMessage()); return; }
                                showFoundSounds((List<Cloud.Found>) r, text);
                            }
                        });
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void showFoundSounds(final List<Cloud.Found> fs, final String query) {
        final AlertDialog[] d = new AlertDialog[1];
        LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 12), 0);
        for (final Cloud.Found f : fs) {
            LinearLayout r = Ui.row(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            r.addView(Ui.text(this, f.title + (f.seconds > 0 ? " (" + Math.round(f.seconds) + "s)" : ""), 13, Ui.TEXT, false),
                    new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            r.addView(Picker.controls(this, f.url));
            r.addView(Ui.small(this, "Choose", Ui.GREEN, new View.OnClickListener() {
                public void onClick(View v) { Picker.stop(); d[0].dismiss(); downloadSound(f, query); }
            }));
            body.addView(r);
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        d[0] = new AlertDialog.Builder(this).setTitle("Choose a sound").setView(sv).setNegativeButton("Close", null).create();
        d[0].setOnDismissListener(new DialogInterface.OnDismissListener() { public void onDismiss(DialogInterface di) { Picker.stop(); } });
        d[0].show();
    }

    private void downloadSound(final Cloud.Found f, final String query) {
        background("Downloading the sound…", new Work() {
            public Object run() throws Exception {
                byte[] b = Prefs.cloud(MainActivity.this).download(f.url);
                String u = f.url.toLowerCase(Locale.US);
                String ext = u.contains(".mp3") ? ".mp3" : u.contains(".ogg") ? ".ogg" : u.contains(".wav") ? ".wav" : u.contains(".flac") ? ".flac" : ".m4a";
                Library.Item it = library.addBytes(Library.SOUND, "amb", f.title, query + "," + f.title, b, ext, f.source + " " + f.license + " " + f.creator);
                if (AudioIO.decode(MainActivity.this, it.path) == null) { library.remove(it); throw new Exception("This sound does not play on this phone"); }
                return it;
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("Failed: " + e.getMessage()); return; }
                useSound((Library.Item) r);
            }
        });
    }

    // ================================================================== recording

    /** Records a voice sample or sound with the microphone and saves it to the library. */
    private void record(final String type, final String name) {
        withPermission(Manifest.permission.RECORD_AUDIO, "Please allow the microphone", new Runnable() {
            public void run() { recordDialog(type, name); }
        });
    }

    private void recordDialog(final String type, final String name) {
        final File out = new File(library.dir(), "rec_" + System.currentTimeMillis() + ".wav");
        final LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        final TextView tip = Ui.text(this, type.equals(Library.VOICE)
                ? "In a quiet place, speak clearly for 10–20 seconds (any sentences). All of " + name + "'s lines will be made in this voice."
                : "Record the sound you want close to the phone.", 14, Ui.SUB, false);
        body.addView(tip);
        final TextView time = Ui.text(this, "0:00", 28, Ui.TEXT, true);
        time.setGravity(Gravity.CENTER);
        body.addView(time);
        final ProgressBar meter = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        meter.setMax(100);
        body.addView(meter);
        final EditText nm = new EditText(this);
        nm.setHint(type.equals(Library.VOICE) ? "Name (e.g. My voice)" : "Name / words (e.g. rain, बारिश)");
        nm.setText(name);
        body.addView(nm);
        final Button btn = Ui.button(this, "⏺  Start recording", Ui.RED, null);
        body.addView(btn);
        final AlertDialog d = new AlertDialog.Builder(this).setTitle(type.equals(Library.VOICE) ? "🎙 Record a voice" : "🎙 Record a sound")
                .setView(body).setNegativeButton("Cancel", null).create();
        final Runnable tick = new Runnable() {
            public void run() {
                if (recorder == null || !recorder.isRunning()) return;
                long s = (System.currentTimeMillis() - recorder.startedAt) / 1000;
                time.setText(s / 60 + ":" + String.format(Locale.US, "%02d", s % 60));
                meter.setProgress((int) (Math.min(1f, recorder.level * 1.5f) * 100));
                ui.postDelayed(this, 120);
            }
        };
        btn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (recorder == null) {
                    recorder = new AudioIO.Recorder(out);
                    recordingFile = out;
                    recorder.start();
                    btn.setText("⏹  Stop and save");
                    ui.post(tick);
                } else {
                    AudioIO.Recorder r = recorder;
                    recorder = null;
                    recordingFile = null;
                    r.stop();
                    d.dismiss();
                    if (r.error != null || out.length() < 32000) { toast("Nothing was recorded " + (r.error == null ? "(too short)" : r.error)); out.delete(); return; }
                    final String n = nm.getText().toString().trim();
                    // saving measures the voice (pitch, speed, tone), which takes a few seconds: not on the screen's thread
                    background("Saving and measuring the voice…", new Work() {
                        public Object run() throws Exception {
                            return library.add(type, type.equals(Library.VOICE) ? "voice" : "amb", n, n, out, ".wav", "recorded");
                        }
                    }, new Done() {
                        public void done(Object r, Exception e) {
                            if (e != null) { toast("Could not save: " + e.getMessage()); return; }
                            toast("✅ Saved to library");
                            useSound((Library.Item) r);
                        }
                    });
                }
            }
        });
        d.setOnDismissListener(new DialogInterface.OnDismissListener() {
            public void onDismiss(DialogInterface di) {
                if (recorder != null) { recorder.stop(); recorder = null; recordingFile = null; out.delete(); }
            }
        });
        d.show();
    }

    // ================================================================== recording each line yourself

    private void showLines() {
        LinearLayout body = page(S_LINES, "🎙 Record lines", true);
        LinearLayout info = Ui.card(this);
        info.addView(Ui.text(this, "Record any line you want in your own (or your children's) real voice. Other lines use the sample / natural / phone voice. Lips move with your recording.", 14, Ui.SUB, false));
        body.addView(info);
        Story st = loadStory();
        Film f;
        try {
            f = new Director(st, new Director.Options()).prepare();
        } catch (Throwable e) {
            body.addView(Ui.text(this, "Could not read the story", 15, Ui.RED, true));
            return;
        }
        final File dir = new File(project.dir, "lines");
        dir.mkdirs();
        for (final Film.Line l : f.lines) {
            final String who = l.who == null ? "कथावाचक" : l.who.displayName;
            final File file = new File(dir, FilmJob.hash(who + "|" + l.text) + ".wav");
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, (l.who == null ? "Narrator" : l.who.shown()) + (l.manner.length() > 0 ? " (" + st.shown(l.manner) + ")" : ""), 15, Ui.TEXT, true));
            c.addView(Ui.text(this, l.shown, 14, Ui.SUB, false));
            LinearLayout r = Ui.row(this);
            final Button rec = Ui.small(this, file.equals(recordingFile) ? "⏹ Stop" : file.exists() ? "🎙 Redo" : "🎙 Record", Ui.RED, null);
            rec.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    withPermission(Manifest.permission.RECORD_AUDIO, "Please allow the microphone", new Runnable() {
                        public void run() {
                            if (recorder != null && file.equals(recordingFile)) {
                                // finish this line's recording
                                AudioIO.Recorder rr = recorder;
                                recorder = null;
                                recordingFile = null;
                                rr.stop();
                                toast(file.length() > 16000 ? "✅ Saved" : "Recording too short");
                                if (file.length() <= 16000) file.delete();
                                showLines();
                                return;
                            }
                            if (recorder != null) { toast("Stop the current recording with ⏹ first"); return; }
                            recordingFile = file;
                            recorder = new AudioIO.Recorder(file);
                            recorder.start();
                            rec.setText("⏹ Stop");
                        }
                    });
                }
            });
            r.addView(rec);
            if (file.exists()) {
                r.addView(Picker.controls(this, file.getAbsolutePath()));
                r.addView(Ui.small(this, "✖", Ui.SUB, new View.OnClickListener() {
                    public void onClick(View v) { file.delete(); showLines(); }
                }));
            }
            c.addView(r);
            body.addView(c);
        }
    }

    // ================================================================== picking files

    /**
     * Many files at once into the library (photos from the whole gallery, sounds, voices — mixed is fine).
     * audioAs: "voice", "sound" or "auto" (decided from the file's name and what it sounds like).
     */
    private void pickMany(String audioAs, String... mimes) {
        manyAudioAs = audioAs;
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType(mimes.length == 1 ? mimes[0] : "*/*");
            if (mimes.length > 1) i.putExtra(Intent.EXTRA_MIME_TYPES, mimes);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(i, "Choose one or many"), REQ_LIB_MANY);
            toast("Tip: long-press to select many at once");
        } catch (Exception e) {
            toast("No file picker app found");
        }
    }

    private String manyAudioAs = "auto";

    /** Adds every chosen file to the library; pictures and sounds are measured so the director can use them. */
    private void addMany(final List<Uri> uris, final String audioAs) {
        background("Adding " + uris.size() + (uris.size() == 1 ? " file" : " files") + " to your library…", new Work() {
            public Object run() throws Exception {
                int pics = 0, voices = 0, sounds = 0, bad = 0;
                for (Uri u : uris) {
                    try {
                        String name = displayName(u);
                        String mime = getContentResolver().getType(u);
                        if (mime == null) mime = "";
                        byte[] b = Project.readAll(getContentResolver().openInputStream(u));
                        String base = name.replaceAll("\\.[A-Za-z0-9]+$", "");
                        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase(Locale.US) : "";
                        boolean image = mime.startsWith("image/") || ext.matches("\\.(jpe?g|png|webp|gif|bmp|heic)");
                        if (image) {
                            library.addBytes(Library.PIC, "", base, name, b, ".jpg", "phone");
                            pics++;
                            continue;
                        }
                        if (ext.length() == 0) ext = ".m4a";
                        boolean voice = audioAs.equals("voice") || (audioAs.equals("auto") && looksLikeVoice(name));
                        Library.Item it = library.addBytes(voice ? Library.VOICE : Library.SOUND, voice ? "voice" : "amb", base, name, b, ext, "phone");
                        if (AudioIO.decode(MainActivity.this, it.path) == null) { library.remove(it); bad++; continue; }
                        if (voice) voices++; else sounds++;
                    } catch (Exception e) {
                        bad++;
                    }
                }
                library.save();
                StringBuilder m = new StringBuilder("✅ Added");
                if (pics > 0) m.append(" ").append(pics).append(pics == 1 ? " picture" : " pictures");
                if (voices > 0) m.append(pics > 0 ? "," : "").append(" ").append(voices).append(voices == 1 ? " voice" : " voices");
                if (sounds > 0) m.append(pics + voices > 0 ? "," : "").append(" ").append(sounds).append(sounds == 1 ? " sound" : " sounds");
                if (pics + voices + sounds == 0) m = new StringBuilder("Nothing could be added");
                if (bad > 0) m.append(" (").append(bad).append(" could not be opened)");
                m.append(". The director uses them by itself in every story.");
                return m.toString();
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("Could not add them: " + e.getMessage()); return; }
                toast((String) r);
                if (screen == S_LIBRARY) showLibrary();
            }
        });
    }

    /** A voice sample rather than a sound, from its file name ("voice", "आवाज़", "dialogue", "sample"…). */
    static boolean looksLikeVoice(String name) {
        return com.tarun.kahani.core.Txt.has(name.replace('_', ' ').replace('-', ' '), "voice", "vocal", "speech", "speaking", "dialogue", "narration",
                "sample", "आवाज़", "आवाज", "awaaz", "awaz", "बोल", "संवाद");
    }

    private void pick(String type, int code, boolean multiple) {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType(type);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            if (multiple) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(i, "Choose"), code);
        } catch (Exception e) {
            toast("No file picker app found");
        }
    }

    private String displayName(Uri uri) {
        try {
            Cursor c = getContentResolver().query(uri, new String[]{OpenableColumns.DISPLAY_NAME}, null, null, null);
            if (c != null) {
                try { if (c.moveToFirst()) return c.getString(0); } finally { c.close(); }
            }
        } catch (Exception ignored) {
        }
        String p = uri.getLastPathSegment();
        return p == null ? "" : p;
    }

    @Override
    protected void onActivityResult(int code, int result, final Intent data) {
        super.onActivityResult(code, result, data);
        if (code == REQ_LOGIN) {
            if (result == RESULT_OK && data != null) {
                String name = data.getStringExtra(AccountManager.KEY_ACCOUNT_NAME);
                if (name != null && name.length() > 0) {
                    Prefs.setAccount(this, name);
                    toast("✅ Signed in: " + name);
                    askPermissions();
                    showHome();
                }
            }
            return;
        }
        if (code == REQ_CAMERA) {
            if (result == RESULT_OK && cameraFile != null && cameraFile.length() > 1000) {
                try {
                    byte[] b = AudioIO.readFile(cameraFile);
                    cameraFile.delete();
                    incomingPicture(b, "camera");
                } catch (Exception e) {
                    toast("No photo received");
                }
            } else if (result == RESULT_OK && data != null && data.getExtras() != null && data.getExtras().get("data") instanceof Bitmap) {
                Bitmap bm = (Bitmap) data.getExtras().get("data");
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 95, bo);
                incomingPicture(bo.toByteArray(), "camera");
            }
            return;
        }
        if (code == REQ_RESTORE) {
            if (result == RESULT_OK && data != null && data.getData() != null) {
                final Uri tree = data.getData();
                background("Restoring your library…", new Work() {
                    public Object run() throws Exception { return Backup.restore(MainActivity.this, tree, library); }
                }, new Done() {
                    public void done(Object r, Exception e) {
                        if (e != null) { toast("Could not restore: " + e.getMessage()); return; }
                        toast("✅ " + r + " items restored to your library");
                        showLibrary();
                    }
                });
            }
            return;
        }
        if (code == REQ_SAVE_TEXT) {
            if (result == RESULT_OK && data != null && data.getData() != null && pendingText == null) {
                toast("Please tap \"Download\" again");
            }
            if (result == RESULT_OK && data != null && data.getData() != null && pendingText != null) {
                try {
                    OutputStream o = getContentResolver().openOutputStream(data.getData());
                    o.write(pendingText.getBytes("UTF-8"));
                    o.close();
                    toast("✅ Saved");
                } catch (Exception e) {
                    saveTextToDownloads(pendingTextName, pendingText);
                }
            }
            pendingText = null;
            return;
        }
        if (result != RESULT_OK || data == null) return;
        if (code == REQ_LIB_MANY) {
            List<Uri> uris = new ArrayList<Uri>();
            if (data.getClipData() != null) for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
            else if (data.getData() != null) uris.add(data.getData());
            if (!uris.isEmpty()) addMany(uris, manyAudioAs);
            return;
        }
        if (code == REQ_BULK) {
            List<Uri> uris = new ArrayList<Uri>();
            if (data.getClipData() != null) for (int i = 0; i < data.getClipData().getItemCount(); i++) uris.add(data.getClipData().getItemAt(i).getUri());
            else if (data.getData() != null) uris.add(data.getData());
            bulkPictures(uris);
            return;
        }
        if (data.getData() == null) return;
        final Uri uri = data.getData();
        try {
            if (code == REQ_SCRIPT && project != null) {
                InputStream in = getContentResolver().openInputStream(uri);
                project.write("script.txt", new String(Project.readAll(in), "UTF-8"));
                project.setSetting("useAi", "0");
                showStory();
            } else if (code == REQ_IMAGE) {
                background("Opening the picture…", new Work() {
                    public Object run() throws Exception {
                        InputStream in = getContentResolver().openInputStream(uri);
                        if (in == null) throw new Exception("Could not open the picture");
                        return new Object[]{Project.readAll(in), displayName(uri)};
                    }
                }, new Done() {
                    public void done(Object r, Exception e) {
                        if (e != null) { toast("Could not open the picture: " + e.getMessage()); return; }
                        incomingPicture((byte[]) ((Object[]) r)[0], (String) ((Object[]) r)[1]);
                    }
                });
            } else if (code == REQ_AUDIO) {
                final String name = displayName(uri);
                background("Adding the sound…", new Work() {
                    public Object run() throws Exception {
                        byte[] b = Project.readAll(getContentResolver().openInputStream(uri));
                        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase(Locale.US) : ".m4a";
                        boolean voice = target != null && (target.startsWith("voice:") || target.equals("lib:voice"));
                        String n = target != null && target.startsWith("voice:") ? target.substring(6) : name.replaceAll("\\.[A-Za-z0-9]+$", "");
                        Library.Item it = library.addBytes(voice ? Library.VOICE : Library.SOUND, voice ? "voice" : "amb", n, name, b, ext, "phone");
                        if (AudioIO.decode(MainActivity.this, it.path) == null) { library.remove(it); throw new Exception("This file does not play"); }
                        return it;
                    }
                }, new Done() {
                    public void done(Object r, Exception e) {
                        if (e != null) { toast(e.getMessage()); return; }
                        useSound((Library.Item) r);
                    }
                });
            }
        } catch (Throwable e) {
            toast("Could not open: " + e.getMessage());
        }
    }

    // ================================================================== the director places pictures

    /** One picture and where the director thinks it belongs. */
    static final class Placement {
        Library.Item item;
        String fileName = "";
        String target;              // char:KEY | place:NAME | title | end | null
        String label = "";
        float score;
        final List<String[]> options = new ArrayList<String[]>();   // {target, label}
        Bitmap thumb;
    }

    /** Everything a picture can be in this story: {target, label, description}. */
    private List<String[]> pictureTargets(Story st) {
        List<String[]> t = new ArrayList<String[]>();
        for (Story.CharacterDef c : st.characters) t.add(new String[]{"char:" + keyFor(c), c.shown(), c.description});
        for (String[] p : Bible.places(st)) t.add(new String[]{"place:" + p[0], st.shown(p[0]), p[1]});
        t.add(new String[]{"title", "Title page", "title picture / movie poster of " + st.title + ", main characters together"});
        t.add(new String[]{"end", "End page", "ending picture, calm landscape, sunset, the end"});
        return t;
    }

    /**
     * Scores every picture against every character/place: file name or library words (exact), AI vision
     * (online), and the offline colour/scene analysis; then gives each picture its best free target.
     */
    private void identify(List<Placement> ps, Story st, boolean useAi, boolean libraryWords) {
        List<String[]> targets = pictureTargets(st);
        List<String[]> cands = new ArrayList<String[]>();
        for (String[] t : targets) cands.add(new String[]{t[1], t[2]});
        List<String> labels = new ArrayList<String>();
        for (String[] t : targets) labels.add(t[1]);
        Cloud cloud = useAi && Prefs.online(this) ? Prefs.cloud(this) : null;
        boolean aiDown = false;
        float[][] score = new float[ps.size()][targets.size()];
        for (int i = 0; i < ps.size(); i++) {
            Placement p = ps.get(i);
            com.tarun.kahani.core.PicSense.Info in = library.info(p.item);
            com.tarun.kahani.core.PicSense.Traits tr = com.tarun.kahani.core.PicSense.Traits.fromMeta(p.item.meta);
            String byName = ScriptAI.matchName(p.fileName, labels);
            for (int t = 0; t < targets.size(); t++) {
                String tg = targets.get(t)[0];
                float f = 0;
                if (in != null) {
                    if (tg.startsWith("char:")) for (Story.CharacterDef c : st.characters) { if (tg.equals("char:" + keyFor(c))) f = com.tarun.kahani.core.PicSense.matchCharacter(in, tr, c); }
                    else if (tg.startsWith("place:")) f = com.tarun.kahani.core.PicSense.matchPlace(in, targets.get(t)[2]);
                }
                float s = f * 0.75f;
                if (libraryWords) s = Math.max(s, com.tarun.kahani.core.PicSense.textMatch(p.item.name + " " + p.item.tags, targets.get(t)[1], targets.get(t)[2]));
                if (byName != null && byName.equals(targets.get(t)[1])) s = 1f;
                score[i][t] = s;
            }
            if (cloud != null && !aiDown && byName == null) {
                try {
                    byte[] small = shrink(Project.readAll(library.open(p.item)), 640);
                    ScriptAI.Seen seen = ScriptAI.look(cloud, small, cands);
                    if (seen.caption.length() > 0) { p.item.tags = (p.item.tags + ", " + seen.caption).replaceAll("^, ", ""); }
                    if (seen.realPhoto) p.item.setMeta("realphoto", "1");
                    if (seen.match != null) for (int t = 0; t < targets.size(); t++) if (targets.get(t)[1].equals(seen.match)) score[i][t] = Math.max(score[i][t], 0.95f);
                } catch (Exception e) {
                    aiDown = true;   // no internet: the offline analysis decides
                }
            }
        }
        library.save();
        int[] best = com.tarun.kahani.core.PicSense.assign(score, 0.12f);
        for (int i = 0; i < ps.size(); i++) {
            Placement p = ps.get(i);
            if (best[i] >= 0) { p.target = targets.get(best[i])[0]; p.label = targets.get(best[i])[1]; p.score = score[i][best[i]]; }
            // the 4 most likely choices for "Change"
            final float[] sc = score[i];
            List<Integer> order = new ArrayList<Integer>();
            for (int t = 0; t < targets.size(); t++) order.add(t);
            java.util.Collections.sort(order, new java.util.Comparator<Integer>() {
                public int compare(Integer a, Integer b) { return Float.compare(sc[b], sc[a]); }
            });
            for (int k = 0; k < order.size(); k++) p.options.add(new String[]{targets.get(order.get(k))[0], targets.get(order.get(k))[1]});
            p.thumb = Picker.thumb(this, p.item, 160);
        }
    }

    static String confidence(float s) { return s >= 0.9f ? "sure" : s >= 0.5f ? "likely" : "guess — please check"; }

    /** Shows where each picture will go; the user can change any of them, then everything is applied at once. */
    private void reviewPlacements(final List<Placement> ps, String title) {
        final AlertDialog[] d = new AlertDialog[1];
        final LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4));
        body.addView(Ui.text(this, "The director read the story and placed your pictures. Tap \"Change\" if one is wrong.", 13, Ui.SUB, false));
        final Runnable[] fill = new Runnable[1];
        final LinearLayout list = Ui.column(this);
        body.addView(list);
        fill[0] = new Runnable() {
            public void run() {
                list.removeAllViews();
                for (final Placement p : ps) {
                    LinearLayout r = Ui.row(MainActivity.this);
                    r.setGravity(Gravity.CENTER_VERTICAL);
                    r.setPadding(0, Ui.dp(MainActivity.this, 4), 0, Ui.dp(MainActivity.this, 4));
                    ImageView iv = new ImageView(MainActivity.this);
                    iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                    iv.setImageBitmap(p.thumb);
                    r.addView(iv, new LinearLayout.LayoutParams(Ui.dp(MainActivity.this, 56), Ui.dp(MainActivity.this, 56)));
                    String txt = p.target == null ? "→ not used (kept in your library)" : "→ " + p.label + "  (" + confidence(p.score) + ")";
                    TextView tv = Ui.text(MainActivity.this, txt, 14, p.target == null ? Ui.SUB : p.score >= 0.5f ? Ui.GREEN : Ui.PRIMARY_DARK, false);
                    tv.setPadding(Ui.dp(MainActivity.this, 8), 0, 0, 0);
                    r.addView(tv, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                    r.addView(Ui.small(MainActivity.this, "Change", Ui.BLUE, new View.OnClickListener() {
                        public void onClick(View v) {
                            final String[] labels = new String[p.options.size() + 1];
                            for (int i = 0; i < p.options.size(); i++) labels[i] = p.options.get(i)[1];
                            labels[labels.length - 1] = "Don't use it here";
                            new AlertDialog.Builder(MainActivity.this).setTitle("This picture is…").setItems(labels, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dd, int which) {
                                    if (which == labels.length - 1) { p.target = null; }
                                    else {
                                        // one picture per character: free the other picture that had it
                                        for (Placement o : ps) if (o != p && p.options.get(which)[0].equals(o.target)) o.target = null;
                                        p.target = p.options.get(which)[0]; p.label = p.options.get(which)[1]; p.score = 1f;
                                    }
                                    fill[0].run();
                                }
                            }).show();
                        }
                    }));
                    list.addView(r);
                }
            }
        };
        fill[0].run();
        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        d[0] = new AlertDialog.Builder(this).setTitle(title).setView(sv)
                .setPositiveButton("✔ Apply", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int w) { applyPlacements(ps); }
                }).setNegativeButton("Cancel", null).show();
    }

    private void applyPlacements(final List<Placement> ps) {
        final Story st = castStory != null ? castStory : loadStory();
        background("Placing the pictures…", new Work() {
            public Object run() throws Exception {
                int n = 0;
                for (Placement p : ps) {
                    if (p.target == null) continue;
                    String f = project.savePicture(Project.readAll(library.open(p.item)), p.target.startsWith("char:") ? "char" : "pic");
                    if (p.target.startsWith("char:")) {
                        String key = p.target.substring(5);
                        project.setManifest("char", key, "char|" + key + "|" + f);
                    } else if (p.target.equals("title") || p.target.equals("end")) {
                        project.setManifest(p.target, p.target, p.target + "|" + f + "|1");
                    } else if (p.target.startsWith("place:")) {
                        String place = p.target.substring(6);
                        for (Story.Scene sc : st.scenes) {
                            String first = sc.setting.length() > 0 ? sc.setting : sc.title;
                            if (Bible.similar(place, first) || com.tarun.kahani.core.Txt.norm(first).contains(com.tarun.kahani.core.Txt.norm(place))
                                    || Bible.similar(Bible.firstClauseOf(first), place)) {
                                String k = String.valueOf(sc.number);
                                project.setManifest("scene", k + "a", null);
                                project.setManifest("scene", k + "b", null);
                                project.setManifest("scene", k, "scene|" + k + "|" + f);
                            }
                        }
                    }
                    n++;
                }
                return n;
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("Could not place them: " + e.getMessage()); return; }
                toast("✅ " + r + " pictures placed. Tap 👄 on characters to fine-tune the lips.");
                showStudio();
            }
        });
    }

    /** Many pictures at once: the director recognises each from the story's descriptions and places it. */
    private void bulkPictures(final List<Uri> uris) {
        final Story st = loadStory();
        background("The director is looking at " + uris.size() + " pictures…", new Work() {
            public Object run() throws Exception {
                List<Placement> ps = new ArrayList<Placement>();
                for (Uri u : uris) {
                    byte[] data;
                    try { data = Project.readAll(getContentResolver().openInputStream(u)); } catch (Exception e) { continue; }
                    Placement p = new Placement();
                    p.fileName = displayName(u);
                    p.item = library.addBytes(Library.PIC, "", p.fileName.replaceAll("\\.[A-Za-z0-9]+$", ""), p.fileName, data, ".jpg", "phone");
                    ps.add(p);
                }
                identify(ps, st, true, false);
                // real photos of people and places become animated avatars before they are used
                for (Placement p : ps) {
                    boolean real = "1".equals(p.item.meta("camera")) || "1".equals(p.item.meta("realphoto"));
                    if (!real || p.target == null) continue;
                    boolean person = p.target.startsWith("char:");
                    byte[] av = toonify(Project.readAll(library.open(p.item)), person);
                    Library.Item a = library.addBytes(Library.PIC, person ? "person" : "place", p.label, p.item.tags, av, person ? ".png" : ".jpg", "photo → avatar");
                    a.setMeta("avatar", "1");
                    p.item = a;
                    p.thumb = Picker.thumb(MainActivity.this, a, 160);
                }
                library.save();
                return ps;
            }
        }, new Done() {
            @SuppressWarnings("unchecked")
            public void done(Object r, Exception e) {
                if (e != null) { toast("Could not do it: " + e.getMessage()); return; }
                reviewPlacements((List<Placement>) r, "Where your pictures go");
            }
        });
    }

    /** Pictures saved in the library (from any story) that fit this story's characters and places. */
    private void libraryMatches(final boolean quietIfNone) {
        final Story st = loadStory();
        background(quietIfNone ? null : "Looking through your library…", new Work() {
            public Object run() throws Exception {
                List<Placement> ps = new ArrayList<Placement>();
                for (Library.Item it : library.find(Library.PIC, null, null)) {
                    if (it.builtIn) continue;
                    Placement p = new Placement();
                    p.item = it;
                    p.fileName = it.name + " " + it.tags;
                    ps.add(p);
                }
                if (ps.isEmpty()) return ps;
                identify(ps, st, false, true);
                List<Placement> keep = new ArrayList<Placement>();
                for (Placement p : ps) if (p.target != null && p.score >= 0.35f) keep.add(p);
                return keep;
            }
        }, new Done() {
            @SuppressWarnings("unchecked")
            public void done(Object r, Exception e) {
                if (e != null) { if (!quietIfNone) toast("Could not do it: " + e.getMessage()); return; }
                List<Placement> ps = (List<Placement>) r;
                if (ps.isEmpty()) { if (!quietIfNone) toast("No pictures in your library fit this story yet"); return; }
                reviewPlacements(ps, "Pictures from your library that fit this story");
            }
        });
    }

    /**
     * Voices saved in the library (recorded or added for any story) that fit this story's characters: each
     * voice is matched by its measured pitch, speed and tone to the character's age, gender and the voice the
     * script describes. Each voice goes to one character at most; the user checks and applies.
     */
    private void voiceMatches(boolean quietIfNone) {
        final Story st = loadStory();
        if (st == null) return;
        Set<String> taken = new HashSet<String>();
        List<Story.CharacterDef> chars = new ArrayList<Story.CharacterDef>();
        for (Story.CharacterDef c : st.characters) {
            Library.Item cur = library.byId(project.setting("vsample." + c.displayName, ""));
            if (cur != null) taken.add(cur.id); else chars.add(c);
        }
        final List<Library.Item> mine = new ArrayList<Library.Item>();
        List<float[]> feats = new ArrayList<float[]>();
        int unmeasured = 0;
        for (Library.Item it : library.find(Library.VOICE, null, null)) {
            if (it.builtIn || taken.contains(it.id)) continue;
            float[] vf = Library.voiceFeatures(it);
            if (vf == null) { if (it.meta("vf") == null) unmeasured++; continue; }
            mine.add(it);
            feats.add(vf);
        }
        if (mine.isEmpty() || chars.isEmpty()) {
            if (!quietIfNone) toast(chars.isEmpty() ? "Every character already has a voice sample"
                    : unmeasured > 0 ? "Your voices are still being measured — try again in a moment" : "No voices in your library yet — record or add one first");
            return;
        }
        List<com.tarun.kahani.core.VoiceMatch.Want> wants = new ArrayList<com.tarun.kahani.core.VoiceMatch.Want>();
        for (Story.CharacterDef c : chars) wants.add(com.tarun.kahani.core.VoiceMatch.want(c));
        int[] a = com.tarun.kahani.core.VoiceMatch.assign(feats, wants, 0.72f);
        final List<Story.CharacterDef> rowsC = new ArrayList<Story.CharacterDef>();
        final List<Library.Item> rowsV = new ArrayList<Library.Item>();
        final List<CheckBox> boxes = new ArrayList<CheckBox>();
        LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 12), Ui.dp(this, 4), Ui.dp(this, 12), Ui.dp(this, 4));
        body.addView(Ui.text(this, "The director compared your saved voices with the voices the story describes "
                + "(age, gender, deep / sweet / raspy, slow / fast). Untick any you don't want, tap ▶ to listen.", 13, Ui.SUB, false));
        for (int i = 0; i < chars.size(); i++) {
            if (a[i] < 0) continue;
            Story.CharacterDef c = chars.get(i);
            final Library.Item it = mine.get(a[i]);
            float sc = com.tarun.kahani.core.VoiceMatch.score(feats.get(a[i]), wants.get(i));
            String fits = com.tarun.kahani.core.VoiceMatch.fits(feats.get(a[i]), wants.get(i));
            LinearLayout r = Ui.row(this);
            r.setGravity(Gravity.CENTER_VERTICAL);
            CheckBox cb = new CheckBox(this);
            cb.setChecked(true);
            cb.setText(c.shown() + "  →  🎙 " + it.label() + "\n" + (fits.length() > 0 ? "fits: " + fits + "  •  " : "") + (sc >= 0.85f ? "good match" : "possible match"));
            cb.setTextSize(14);
            r.addView(cb, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
            r.addView(Picker.controls(this, it.path));
            body.addView(r);
            rowsC.add(c);
            rowsV.add(it);
            boxes.add(cb);
        }
        if (rowsC.isEmpty()) {
            if (!quietIfNone) toast("None of your saved voices fits the characters of this story — the studio voices will be used");
            return;
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        new AlertDialog.Builder(this).setTitle("Voices from your library that fit this story").setView(sv)
                .setPositiveButton("✔ Apply", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int w) {
                        Picker.stop();
                        int n = 0;
                        for (int i = 0; i < rowsC.size(); i++) {
                            if (!boxes.get(i).isChecked()) continue;
                            project.setSetting("vsample." + rowsC.get(i).displayName, rowsV.get(i).id);
                            n++;
                        }
                        toast("✅ " + n + (n == 1 ? " voice" : " voices") + " from your library will be used");
                        showStudio();
                    }
                }).setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface di, int w) { Picker.stop(); }
                }).show();
    }

    /** Decodes a picture no bigger than about max pixels on its long side (never the full-size photo). */
    static Bitmap decodeSmall(byte[] data, int max) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            if (o.outWidth <= 0) return null;
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

    static byte[] shrink(byte[] data, int max) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            BitmapFactory.decodeByteArray(data, 0, data.length, o);
            int s = 1;
            while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= max) s *= 2;
            o = new BitmapFactory.Options();
            o.inSampleSize = s;
            Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, o);
            java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
            b.compress(Bitmap.CompressFormat.JPEG, 80, bo);
            b.recycle();
            return bo.toByteArray();
        } catch (Throwable e) {
            return null;
        }
    }

    // ================================================================== face (mouth & eyes) setup

    private void showFace(final String charName) {
        final LinearLayout outer = Ui.column(this);
        final TextView hint = Ui.text(this, "Preparing the picture…", 17, 0xFFFFFFFF, true);
        hint.setBackgroundColor(Ui.PRIMARY);
        hint.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        outer.addView(hint);
        final FaceTapView fv = new FaceTapView(this);
        outer.addView(fv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout r = Ui.row(this);
        r.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        r.setBackgroundColor(Ui.BG);
        final Button again = Ui.small(this, "↺ Redo", Ui.RED, null);
        final Button save = Ui.small(this, "✔ Save", Ui.GREEN, null);
        final Button skip = Ui.small(this, "Automatic", Ui.SUB, null);
        r.addView(again);
        r.addView(save);
        r.addView(skip);
        outer.addView(r);
        setScreen(S_FACE, outer);
        final String[] steps = {"1/3: Tap the middle of the mouth", "2/3: Tap the left eye (as you look at it)", "3/3: Tap the right eye", "✔ Done! Tap \"Save\""};
        fv.listener = new FaceTapView.Listener() {
            public void changed(int step) { hint.setText(steps[Math.min(3, step)]); }
        };
        again.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { fv.restart(); } });
        skip.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showStudio(); } });
        String line = project.manifestLine("char", charName);
        if (line == null) { showStudio(); return; }
        final String[] f = line.split("\\|");
        new Thread(new Runnable() {
            public void run() {
                final Art.Sprite sp;
                try {
                    sp = Art.makeSprite(project.loader(), f[2], 1100);
                } catch (Throwable e) {
                    ui.post(new Runnable() { public void run() { toast("Could not open the picture"); showStudio(); } });
                    return;
                }
                ui.post(new Runnable() {
                    public void run() {
                        if (screen != S_FACE) return;
                        if (sp == null) { toast("Could not open the picture"); showStudio(); return; }
                        float[] init = null;
                        if (f.length >= 11) {
                            try {
                                init = new float[]{Float.parseFloat(f[3]), Float.parseFloat(f[4]), Float.parseFloat(f[6]), Float.parseFloat(f[7]), Float.parseFloat(f[8]), Float.parseFloat(f[9])};
                            } catch (NumberFormatException ignored) {}
                        } else if (sp.faceKnown) {
                            init = new float[]{sp.mouthX, sp.mouthY, sp.eyeLX, sp.eyeLY, sp.eyeRX, sp.eyeRY};
                        }
                        fv.set((Bitmap) sp.img, init);
                        hint.setText(fv.step >= 3 ? "Are the marks right? If not, tap \"Redo\" and tap the mouth and eyes" : steps[0]);
                    }
                });
            }
        }).start();
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (fv.step < 3) { toast("First tap the mouth and both eyes"); return; }
                float[] p = fv.pts;
                float eyeDist = Math.abs(p[4] - p[2]);
                float mouthHW = eyeDist * 0.42f, eyeR = eyeDist * 0.24f;
                String nl = String.format(Locale.US, "char|%s|%s|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%s",
                        f[1], f[2], p[0], p[1], mouthHW, p[2], p[3], p[4], p[5], eyeR, f.length >= 12 ? f[11] : "0");
                project.setManifest("char", f[1], nl);
                toast("Saved — the lips will now move with the voice");
                showStudio();
            }
        });
    }

    // ================================================================== making the film

    private void makeFilm() {
        saveScript();
        if (FilmJob.scriptOf(project).trim().length() < 10) { toast("Write or paste a story first"); return; }
        // the director asks before generating anything (FINAL_AR, which pictures AI may make, the shot check)
        askBeforeMaking(new Runnable() { public void run() { startFilm(); } });
    }

    /**
     * Before anything is generated, the director asks: where the film will be shown (its shape, decided once —
     * FINAL_AR), whether the missing pictures may be made with AI, and whether to check every shot's first frame
     * before the film is made (Human QC).
     */
    private void askBeforeMaking(final Runnable go) {
        final Edits ed = edits();
        LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 18), Ui.dp(this, 6), Ui.dp(this, 18), Ui.dp(this, 6));
        body.addView(Ui.text(this, "Where will this film be shown? (decided once for the whole film)", 15, Ui.TEXT, true));
        final android.widget.RadioGroup rg = new android.widget.RadioGroup(this);
        final String[] ars = {"16:9", "9:16", "1:1"};
        String[] labels = {"▭  YouTube / TV — landscape 16:9", "▯  Reels / Shorts / WhatsApp status — vertical 9:16", "▢  Instagram post — square 1:1"};
        for (int i = 0; i < ars.length; i++) {
            android.widget.RadioButton rb = new android.widget.RadioButton(this);
            rb.setText(labels[i]);
            rb.setId(1000 + i);
            rg.addView(rb);
            if (ars[i].equals(ed.aspect)) rb.setChecked(true);
        }
        if (rg.getCheckedRadioButtonId() == -1) rg.check(1000);
        body.addView(rg);
        // pictures still missing (the library is searched first; the rest can be made with AI)
        final CheckBox ai = new CheckBox(this);
        try {
            Story st = ScriptParser.parse(FilmJob.scriptOf(project));
            List<String[]> miss = AutoLibrary.missingTargets(project, st);
            StringBuilder m = new StringBuilder();
            int shown = 0;
            for (String[] t : miss) {
                if (t[0].startsWith("shot:")) continue;
                if (shown++ < 10) m.append(m.length() > 0 ? ", " : "").append(t[1]);
            }
            if (shown > 10) m.append(" and ").append(shown - 10).append(" more");
            if (shown > 0) {
                body.addView(Ui.text(this, "\nNo picture yet: " + m + ". The director looks in your library first.", 14, Ui.SUB, false));
                ai.setText("Make the rest with free AI in 3D animated style (made natively in the film's shape)");
                ai.setChecked(Prefs.autoArt(this) && Prefs.online(this));
                ai.setEnabled(Prefs.online(this));
                body.addView(ai);
            }
        } catch (Exception ignored) {}
        final CheckBox qc = new CheckBox(this);
        qc.setText("Show me the first frame of every shot before the film is made, so I can fix any (Human QC)");
        qc.setChecked(Prefs.humanQc(this));
        body.addView(qc);
        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        new AlertDialog.Builder(this).setTitle("🎬 Before the director makes your film").setView(sv)
                .setPositiveButton("🎬 Make the film", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        int k = rg.getCheckedRadioButtonId() - 1000;
                        Edits e = edits();
                        e.aspect = ars[Math.max(0, Math.min(2, k))];
                        saveEdits(e);
                        if (ai.getParent() != null) Prefs.put(MainActivity.this, "autoArt", ai.isChecked() ? "1" : "0");
                        Prefs.put(MainActivity.this, "humanQc", qc.isChecked() ? "1" : "0");
                        go.run();
                    }
                }).setNegativeButton("Cancel", null).show();
    }

    private void startFilm() {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED && !askedPerms) {
            askPermissions();
        }
        FilmJob j = FilmJob.start(this, project);
        if (j == null) {
            FilmJob cur = FilmJob.current;
            final Project other = cur != null ? cur.project : null;
            AlertDialog.Builder b = new AlertDialog.Builder(this).setTitle("A film is already being made")
                    .setMessage(cur != null && cur.cancelled ? "The previous film is still stopping — try again in a few seconds."
                            : "\"" + (other == null ? "" : other.name()) + "\" is being made. Make this film after it finishes, or stop it.")
                    .setPositiveButton("OK", null);
            if (other != null && !cur.cancelled) b.setNeutralButton("See its progress", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { project = other; showProgress(); }
            });
            b.show();
            return;
        }
        showProgress();
    }

    /**
     * Human QC: the director made the first frame of every shot; the user looks through them, fixes any shot with
     * the protocol's corrections, and approves. Only then is the film made.
     */
    private void showQc(final FilmJob j) {
        LinearLayout body = page(S_QC, "🎬 Check every shot", false);
        LinearLayout head = Ui.card(this);
        head.addView(Ui.text(this, "The director planned " + countShots(j) + " shots of about 3 seconds each and made the first frame of each one. "
                + "Look through them. Tap a shot to fix it (calmer, closer, wider, show the listener, or no cut there). Then tap Approve: "
                + "the film is made exactly from this plan.", 14, Ui.SUB, false));
        body.addView(head);
        final LinearLayout grid = Ui.column(this);
        body.addView(grid);
        final int per = 12;
        final int[] page = {0};
        final TextView info = Ui.text(this, "", 13, Ui.SUB, false);
        final Runnable[] fill = new Runnable[1];
        fill[0] = new Runnable() {
            public void run() {
                grid.removeAllViews();
                int from = page[0] * per, to = Math.min(j.qcItems.size(), from + per);
                LinearLayout row = null;
                for (int i = from; i < to; i++) {
                    final int item = i;
                    final String[] it = j.qcItems.get(i);
                    if ((i - from) % 2 == 0) { row = Ui.row(MainActivity.this); grid.addView(row); }
                    LinearLayout cell = Ui.column(MainActivity.this);
                    cell.setPadding(Ui.dp(MainActivity.this, 4), Ui.dp(MainActivity.this, 4), Ui.dp(MainActivity.this, 4), Ui.dp(MainActivity.this, 8));
                    ImageView iv = new ImageView(MainActivity.this);
                    iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
                    iv.setBackgroundColor(0xFF000000);
                    Bitmap b = android.graphics.BitmapFactory.decodeFile(it[0]);
                    if (b != null) iv.setImageBitmap(b);
                    cell.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(MainActivity.this, it[2].equals("char") ? 150 : 100)));
                    final int shotIdx = j.qcShotIndex.get(i);
                    Integer fx = shotIdx >= 0 ? j.qcFixes.get(shotIdx) : null;
                    TextView t = Ui.text(MainActivity.this, (fx != null && fx != Director.FIX_NONE ? "✎ " + Director.FIX_NAMES[fx].split(" —")[0] + "\n" : "") + it[1], 11,
                            fx != null && fx != Director.FIX_NONE ? Ui.BLUE : Ui.TEXT, false);
                    t.setMaxLines(4);
                    cell.addView(t);
                    if (shotIdx >= 0) cell.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            Integer cur = j.qcFixes.get(shotIdx);
                            new AlertDialog.Builder(MainActivity.this).setTitle(it[1].split("\n")[0])
                                    .setSingleChoiceItems(Director.FIX_NAMES, cur == null ? 0 : cur, new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface d, int w) {
                                            if (w == Director.FIX_NONE) j.qcFixes.remove(shotIdx); else j.qcFixes.put(shotIdx, w);
                                            d.dismiss();
                                            fill[0].run();
                                        }
                                    }).setNegativeButton("Close", null).show();
                        }
                    });
                    row.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                }
                int pages = Math.max(1, (j.qcItems.size() + per - 1) / per);
                info.setText("Page " + (page[0] + 1) + " of " + pages + "  •  " + j.qcFixes.size() + " shot(s) to fix");
            }
        };
        fill[0].run();
        LinearLayout nav = Ui.row(this);
        nav.addView(Ui.small(this, "◀ Previous", Ui.SUB, new View.OnClickListener() {
            public void onClick(View v) { if (page[0] > 0) { page[0]--; fill[0].run(); } }
        }));
        nav.addView(Ui.small(this, "Next ▶", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { if ((page[0] + 1) * per < j.qcItems.size()) { page[0]++; fill[0].run(); } }
        }));
        body.addView(nav);
        body.addView(info);
        LinearLayout c = Ui.card(this);
        c.addView(Ui.button(this, "✔  Approve and make the film", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { j.approve(); showProgress(); }
        }));
        c.addView(Ui.button(this, "■  Stop (change pictures or the story first)", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) { j.cancel(); j.approve(); showStory(); }
        }));
        body.addView(c);
    }

    /** The Technical Director protocol exactly as given, and how the app applies each part of it. */
    private void showProtocol() {
        String given;
        try { given = new String(Project.readAll(getAssets().open("technical_director_protocol.md")), "UTF-8"); }
        catch (Exception e) { given = com.tarun.kahani.core.TechnicalDirector.PROTOCOL; }
        String how = "HOW THE APP APPLIES IT\n"
                + "• Every film is made of shots of about 3 s (never over 4), each with a locked camera and one action.\n"
                + "• Every spoken line: front-facing close-ups framed on the face, at most 6 words per shot, the listener's silent reaction between; "
                + "the speaker stops walking to speak, the head stays still, only the mouth and jaw move.\n"
                + "• No character moves 15% of the frame in one shot: runs are slowed to walking pace, or the action is cut into still shots.\n"
                + "• Feet on the floor found in each place picture, with a soft contact shadow; heights in feet in every description.\n"
                + "• FINAL_AR is asked once before making; AI pictures are made in that shape; every shot is framed for it (faces in the centre 60%, "
                + "headroom, 15% empty at the sides, nobody cut in half).\n"
                + "• Pictures are bent through meshes fine to the pixel; gestures ease in and out, anticipation before moves, follow-through of hair and cloth.\n"
                + "• Human QC: the first frame of every shot is shown to you before the film is made; your fixes use the protocol's error correction.\n"
                + "• The validation layer checks every shot and every prompt; the descriptions file (📄) has the lock sheets, plates, shot table, "
                + "the image and video templates filled in for every shot, and the validation result.\n\n";
        TextView tv = Ui.text(this, how + given, 13, Ui.TEXT, false);
        tv.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        tv.setTextIsSelectable(true);
        ScrollView sv = new ScrollView(this);
        sv.addView(tv);
        new AlertDialog.Builder(this).setTitle("📜 Technical Director protocol").setView(sv).setPositiveButton("Close", null).show();
    }

    private static int countShots(FilmJob j) {
        int n = 0;
        for (Integer i : j.qcShotIndex) if (i >= 0) n++;
        return n;
    }

    private void showProgress() {
        LinearLayout body = page(S_PROGRESS, "⏳ Making your film", true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout c = Ui.card(this);
        final TextView stage = Ui.text(this, "", 17, Ui.TEXT, true);
        final TextView eta = Ui.text(this, "", 15, Ui.BLUE, true);
        final ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        final ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setBackgroundColor(0xFF000000);
        final TextView note = Ui.text(this, "You can lock the phone or use other apps — the film keeps being made (progress shows in the notification).", 14, Ui.SUB, false);
        final TextView warn = Ui.text(this, "", 14, Ui.RED, false);
        final TextView fromLib = Ui.text(this, "", 13, Ui.GREEN, false);
        c.addView(stage);
        c.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 22)));
        c.addView(eta);
        c.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 200)));
        c.addView(note);
        c.addView(fromLib);
        c.addView(warn);
        final Button stop = Ui.button(this, "■  Stop", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this).setTitle("Stop?").setMessage("The film being made will stop.")
                        .setPositiveButton("Yes, stop", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { FilmJob j = FilmJob.current; if (j != null) { j.cancel(); j.pause(false); } }
                        }).setNegativeButton("No", null).show();
            }
        });
        final Button pause = Ui.button(this, "⏸  Pause", Ui.BLUE, null);
        pause.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                FilmJob j = FilmJob.current;
                if (j == null) return;
                j.pause(!j.paused);
                pause.setText(j.paused ? "▶  Resume" : "⏸  Pause");
            }
        });
        FilmJob cur = FilmJob.current;
        if (cur != null && cur.paused) pause.setText("▶  Resume");
        c.addView(pause);
        c.addView(stop);
        body.addView(c);
        ui.post(new Runnable() {
            public void run() {
                if (screen != S_PROGRESS) return;
                FilmJob j = FilmJob.current;
                if (j == null) { showStory(); return; }
                if (j.qcWaiting) { showQc(j); return; }
                stage.setText(j.paused ? "⏸ Paused — tap Resume to carry on" : j.stage);
                eta.setText(j.paused ? "" : j.eta());
                bar.setProgress((int) (j.progress * 1000));
                if (j.preview != null) preview.setImageBitmap(j.preview);
                if (j.warning.length() > 0) warn.setText("⚠ " + j.warning);
                if (j.info.length() > 0) fromLib.setText("📚 " + j.info);
                if (j.done) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    toast("🎉 Your film is ready!");
                    project = j.project;
                    showPlayer();
                    return;
                }
                if (j.failed || j.cancelled) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    stage.setText(j.failed ? "❌ The film could not be made" : "Stopped");
                    eta.setText("");
                    if (j.failed) warn.setText(j.error);
                    pause.setVisibility(View.GONE);
                    stop.setText("← Back");
                    stop.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showStory(); } });
                    return;
                }
                ui.postDelayed(this, 500);
            }
        });
    }

    // ================================================================== preview, commands, saving

    private void showPlayer() {
        if (project == null || !project.film().exists()) { showStory(); return; }
        FilmJob fj = FilmJob.current;
        if (fj != null && fj.done && fj.project.dir.equals(project.dir)) fj.seen = true;
        LinearLayout outer = Ui.column(this);
        outer.setBackgroundColor(0xFF000000);
        FrameLayout fl = new FrameLayout(this);
        final VideoView vv = new VideoView(this);
        fl.addView(vv, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER));
        outer.addView(fl, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout panel = Ui.column(this);
        panel.setBackgroundColor(Ui.BG);
        panel.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        panel.addView(Ui.text(this, "✍ Describe a change in plain English, e.g. \"the music is too loud\" • \"make Vrinda's voice a little higher\" • \"brighter and warmer\" • \"add subtitles\" • \"smaller file for WhatsApp\" • \"make it for Instagram\"", 13, Ui.SUB, false));
        LinearLayout cmdRow = Ui.row(this);
        final EditText cmd = new EditText(this);
        cmd.setHint("Your instruction…");
        cmd.setTextSize(15);
        cmd.setSingleLine(false);
        cmd.setMaxLines(3);
        cmdRow.addView(cmd, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView status = Ui.text(this, pendingEditsNote(), 13, Ui.GREEN, false);
        cmdRow.addView(Ui.small(this, "✔ Apply", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { applyCommand(cmd.getText().toString(), status, cmd); }
        }));
        panel.addView(cmdRow);
        panel.addView(status);
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "🔁 Make again", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { vv.stopPlayback(); makeFilm(); }
        }));
        r.addView(Ui.small(this, "💾 Download", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { saveToGallery(false); }
        }));
        r.addView(Ui.small(this, "📤 Share", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { saveToGallery(true); }
        }));
        panel.addView(r);
        outer.addView(panel);
        setScreen(S_PLAYER, outer);
        MediaController mc = new MediaController(this);
        mc.setAnchorView(vv);
        vv.setMediaController(mc);
        vv.setVideoPath(project.film().getAbsolutePath());
        vv.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
            public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                toast("The film could not play here — download it and watch it in the gallery");
                return true;
            }
        });
        vv.start();
    }

    private String pendingEditsNote() {
        long made = 0;
        try { made = Long.parseLong(project.setting("madeAt", "0")); } catch (NumberFormatException ignored) {}
        File e = project.file("edits.json");
        if (e.exists() && e.lastModified() > made + 1000) return "Changes saved — tap \"🔁 Make again\" to see them (voices are reused, so it is quicker).";
        return "";
    }

    private void applyCommand(final String text, final TextView status, final EditText box) {
        if (text.trim().length() == 0) return;
        for (int i = 0; i < text.length(); i++) {
            if (com.tarun.kahani.core.Txt.isDevanagari(text.charAt(i))) {
                status.setText("Please type the change in English, e.g. \"make the background music softer\".");
                return;
            }
        }
        loadStory();
        final List<String> names = new ArrayList<String>();
        for (Story.CharacterDef c : castStory.characters) names.add(c.displayName);
        final Edits ed = edits();
        CommandParser.Result res = CommandParser.parse(text, names);
        final StringBuilder done = new StringBuilder();
        for (Map<String, Object> c : res.commands) {
            String d = ed.apply(c);
            if (d != null) done.append("✔ ").append(d).append('\n');
        }
        saveEdits(ed);
        if (res.unknown.isEmpty() || !Prefs.online(this)) {
            if (!res.unknown.isEmpty()) done.append("? Not understood: ").append(res.unknown).append('\n');
            status.setText(done + (done.length() > 0 ? "Tap \"🔁 Make again\" to see the changes." : ""));
            box.setText("");
            return;
        }
        final String rest = android.text.TextUtils.join(", ", res.unknown);
        background("AI is understanding your instruction…", new Work() {
            public Object run() throws Exception { return ScriptAI.commands(Prefs.cloud(MainActivity.this), rest, names); }
        }, new Done() {
            @SuppressWarnings("unchecked")
            public void done(Object r, Exception e) {
                Edits ed2 = edits();
                if (e == null) for (Map<String, Object> c : (List<Map<String, Object>>) r) {
                    String d = ed2.apply(c);
                    if (d != null) done.append("✔ ").append(d).append('\n');
                }
                else done.append("? Not understood: ").append(rest).append('\n');
                saveEdits(ed2);
                status.setText(done + "Tap \"🔁 Make again\" to see the changes.");
                box.setText("");
            }
        });
    }

    private boolean pendingShare;

    private void saveToGallery(final boolean thenShare) {
        if (project == null || !project.film().exists()) return;
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingShare = thenShare;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, REQ_PERM_GALLERY);
            return;
        }
        final File src = project.film();
        final String name = safeName(project.name()) + ".mp4";
        background(thenShare ? "Preparing to share…" : "Saving to the gallery…", new Work() {
            public Object run() throws Exception {
                String saved = project.setting("savedUri", "");
                if (saved.length() > 0 && project.setting("saved", "0").equals("1")) return Uri.parse(saved);
                Uri uri = copyToGallery(src, name);
                project.setSetting("savedUri", uri.toString());
                project.setSetting("saved", "1");
                return uri;
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("Could not save: " + e.getMessage()); return; }
                if (!thenShare) { toast("✅ Saved to the gallery (Movies/KahaniFilm)"); return; }
                try {
                    Intent s = new Intent(Intent.ACTION_SEND);
                    s.setType("video/mp4");
                    s.putExtra(Intent.EXTRA_STREAM, (Uri) r);
                    s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(s, "Share the film (YouTube, Instagram, Facebook…)"));
                } catch (Exception ex) {
                    toast("Could not share");
                }
            }
        });
    }

    private static String safeName(String s) {
        String n = s.replaceAll("[\\\\/:*?\"<>|]", " ").trim();
        if (n.length() == 0) n = "kahani";
        if (n.length() > 60) n = n.substring(0, 60);
        return n + "_" + (System.currentTimeMillis() / 1000 % 100000);
    }

    private Uri copyToGallery(File src, String name) throws Exception {
        if (Build.VERSION.SDK_INT >= 29) {
            ContentValues v = new ContentValues();
            v.put(MediaStore.Video.Media.DISPLAY_NAME, name);
            v.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
            v.put(MediaStore.Video.Media.RELATIVE_PATH, Environment.DIRECTORY_MOVIES + "/KahaniFilm");
            v.put(MediaStore.Video.Media.IS_PENDING, 1);
            Uri uri = getContentResolver().insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, v);
            if (uri == null) throw new Exception("Gallery not available");
            OutputStream o = getContentResolver().openOutputStream(uri);
            Project.copy(new FileInputStream(src), o);
            v.clear();
            v.put(MediaStore.Video.Media.IS_PENDING, 0);
            getContentResolver().update(uri, v, null, null);
            return uri;
        }
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "KahaniFilm");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("Could not create the folder");
        File dst = new File(dir, name);
        Project.copy(new FileInputStream(src), new FileOutputStream(dst));
        final Uri[] out = {null};
        final Object lock = new Object();
        MediaScannerConnection.scanFile(this, new String[]{dst.getAbsolutePath()}, new String[]{"video/mp4"},
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) { synchronized (lock) { out[0] = uri; lock.notifyAll(); } }
                });
        synchronized (lock) { if (out[0] == null) lock.wait(5000); }
        if (out[0] == null) throw new Exception("The gallery did not recognise the file");
        return out[0];
    }

    // ================================================================== library

    private void showLibrary() {
        target = "lib:" + libTab;
        LinearLayout body = page(S_LIBRARY, "📚 My library", true);
        LinearLayout tabs = Ui.row(this);
        tabs.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        final String[][] t = {{Library.PIC, "🖼 Pictures"}, {Library.VOICE, "🎙 Voices"}, {Library.SOUND, "🔊 Sounds"}};
        for (final String[] x : t) {
            tabs.addView(Ui.small(this, x[1], x[0].equals(libTab) ? Ui.PRIMARY : Ui.SUB, new View.OnClickListener() {
                public void onClick(View v) { libTab = x[0]; showLibrary(); }
            }));
        }
        body.addView(tabs);
        LinearLayout add = Ui.card(this);
        if (libTab.equals(Library.PIC)) {
            add.addView(Ui.text(this, "Your pictures can be used in every film. Real photos can be turned into cartoon avatars.", 13, Ui.SUB, false));
            LinearLayout r = Ui.row(this);
            r.addView(Ui.small(this, "📂 From phone", Ui.PRIMARY, new View.OnClickListener() { public void onClick(View v) { target = "lib:pic"; pickMany("auto", "image/*"); } }));
            r.addView(Ui.small(this, "📷 Camera", Ui.PRIMARY, new View.OnClickListener() { public void onClick(View v) { target = "lib:pic"; camera(); } }));
            r.addView(Ui.small(this, "🌐 Search", Ui.BLUE, new View.OnClickListener() { public void onClick(View v) { target = "lib:pic"; searchPictures(""); } }));
            add.addView(r);
        } else if (libTab.equals(Library.VOICE)) {
            add.addView(Ui.text(this, "Voice samples (10–20 seconds). Give one to a character in the studio and all their lines are made in that voice.", 13, Ui.SUB, false));
            LinearLayout r = Ui.row(this);
            r.addView(Ui.small(this, "🎙 Record", Ui.RED, new View.OnClickListener() { public void onClick(View v) { target = "lib:voice"; record(Library.VOICE, ""); } }));
            r.addView(Ui.small(this, "📂 From files", Ui.PRIMARY, new View.OnClickListener() { public void onClick(View v) { target = "lib:voice"; pickMany("voice", "audio/*"); } }));
            add.addView(r);
        } else {
            add.addView(Ui.text(this, "Nature sounds (rain, river, birds), background voices (a market, a crowd), effects (a door, thunder, a horse) and music. "
                    + "Say what each one is — in English or Hindi — and the director uses it wherever a story describes it: backgrounds under the matching places, effects at the moment they happen.", 13, Ui.SUB, false));
            LinearLayout r = Ui.row(this);
            r.addView(Ui.small(this, "🎙 Record", Ui.RED, new View.OnClickListener() { public void onClick(View v) { target = "lib:sound"; record(Library.SOUND, ""); } }));
            r.addView(Ui.small(this, "📂 Files", Ui.PRIMARY, new View.OnClickListener() { public void onClick(View v) { target = "lib:sound"; pickMany("sound", "audio/*"); } }));
            r.addView(Ui.small(this, "🌐 Search", Ui.BLUE, new View.OnClickListener() { public void onClick(View v) { target = "lib:sound"; searchSounds(""); } }));
            add.addView(r);
        }
        body.addView(add);
        LinearLayout many = Ui.card(this);
        many.addView(Ui.text(this, "Add many at once — photos from your whole gallery, sounds and voice samples together. "
                + "The director looks through the library by itself before every film and uses what fits.", 13, Ui.SUB, false));
        many.addView(Ui.small(this, "➕ Add many (photos, sounds, voices)", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { pickMany("auto", "image/*", "audio/*"); }
        }));
        body.addView(many);
        LinearLayout safe = Ui.card(this);
        safe.addView(Ui.text(this, "🔒 Everything you add stays saved on this phone, even when the app is closed. A backup copy is also kept in Downloads/KahaniFilm/Library — it stays even if the app is removed.", 13, Ui.SUB, false));
        safe.addView(Ui.small(this, "♻ Restore from backup", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) {
                try {
                    Intent i = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
                    startActivityForResult(i, REQ_RESTORE);
                    toast("Choose the folder Downloads › KahaniFilm › Library");
                } catch (Exception e) {
                    toast("No folder picker found on this phone");
                }
            }
        }));
        body.addView(safe);
        List<Library.Item> items = library.find(libTab, null, null);
        LinearLayout row = null;
        int i = 0;
        for (final Library.Item it : items) {
            if (libTab.equals(Library.PIC)) {
                if (i % 2 == 0) { row = Ui.row(this); row.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0); body.addView(row); }
                LinearLayout cell = Ui.column(this);
                cell.setPadding(Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4), Ui.dp(this, 4));
                ImageView iv = new ImageView(this);
                iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
                iv.setBackgroundColor(0xFFEEEEEE);
                Picker.thumbAsync(this, iv, it, 300);
                cell.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 120)));
                TextView tv = Ui.text(this, it.label() + (it.builtIn ? " (app)" : ""), 12, Ui.TEXT, false);
                tv.setMaxLines(1);
                cell.addView(tv);
                if (!it.builtIn) cell.setOnLongClickListener(new View.OnLongClickListener() {
                    public boolean onLongClick(View v) { confirmRemove(it); return true; }
                });
                row.addView(cell, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                i++;
            } else {
                LinearLayout r = Ui.row(this);
                r.setPadding(Ui.dp(this, 12), 0, Ui.dp(this, 8), 0);
                r.setGravity(Gravity.CENTER_VERTICAL);
                String sub = "";
                if (!it.builtIn && Library.SOUND.equals(it.type)) {
                    String ws = Library.meaningful(it.name + "," + it.tags);
                    sub = "\n" + Library.soundKindLabel(it) + (ws.length() > 0 ? " • " + ws : it.meta("sl") != null && it.meta("sl").length() > 0 ? " • sounds like " + it.meta("sl") : "");
                } else if (!it.builtIn && Library.VOICE.equals(it.type) && it.meta("vdesc") != null) {
                    sub = "\n" + it.meta("vdesc");
                }
                r.addView(Ui.text(this, it.label() + (it.builtIn ? "  (app)" : "") + sub, 14, Ui.TEXT, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                if (!it.path.startsWith("preset:")) r.addView(Picker.controls(this, it.path));
                if (!it.builtIn && Library.SOUND.equals(it.type)) r.addView(Ui.small(this, "✎", Ui.GREEN, new View.OnClickListener() {
                    public void onClick(View v) { describeSound(it, new Runnable() { public void run() { showLibrary(); } }); }
                }));
                if (!it.builtIn) r.addView(Ui.small(this, "🗑", Ui.RED, new View.OnClickListener() { public void onClick(View v) { confirmRemove(it); } }));
                body.addView(r);
            }
        }
        if (libTab.equals(Library.PIC) && !items.isEmpty())
            body.addView(Ui.text(this, "  Long-press your picture to delete it.", 12, Ui.SUB, false));
    }

    private void confirmRemove(final Library.Item it) {
        new AlertDialog.Builder(this).setTitle("Delete?").setMessage(it.label())
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { Picker.stop(); library.remove(it); showLibrary(); }
                }).setNegativeButton("No", null).show();
    }

    // ================================================================== settings

    private void showSettings() {
        LinearLayout body = page(S_SETTINGS, "⚙ Settings", true);
        LinearLayout acc = Ui.card(this);
        String a = Prefs.account(this);
        acc.addView(Ui.title(this, "Account"));
        acc.addView(Ui.text(this, a.length() > 0 ? "👤 " + a : "Not signed in", 15, Ui.TEXT, false));
        acc.addView(Ui.small(this, a.length() > 0 ? "Log out" : "Sign in with Gmail", a.length() > 0 ? Ui.RED : Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { if (Prefs.account(MainActivity.this).length() > 0) logout(); else login(); }
        }));
        body.addView(acc);

        LinearLayout ai = Ui.card(this);
        ai.addView(Ui.title(this, "🔑 Google Gemini — smarter story reading, picture recognition, AI voices"));
        ai.addView(Ui.text(this, "The app works without any key (free AI pictures and story reading). For better story reading, picture recognition and expressive AI voices, add a free Google Gemini key:\n1. Open aistudio.google.com/apikey (same Gmail)\n2. Tap \"Create API key\" and copy it\n3. Paste it here. The key stays only on this phone.", 13, Ui.SUB, false));
        final EditText key = new EditText(this);
        key.setHint("Gemini API key (AIza…)");
        key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setText(Prefs.geminiKey(this));
        ai.addView(key);
        LinearLayout kr = Ui.row(this);
        kr.addView(Ui.small(this, "💾 Save", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { Prefs.put(MainActivity.this, "geminiKey", key.getText().toString().trim()); toast("Saved"); }
        }));
        kr.addView(Ui.small(this, "🧪 Test", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) {
                Prefs.put(MainActivity.this, "geminiKey", key.getText().toString().trim());
                background("Testing…", new Work() {
                    public Object run() throws Exception { return Prefs.cloud(MainActivity.this).gemini(null, "Reply with the single word OK", false, null, null); }
                }, new Done() {
                    public void done(Object r, Exception e) { toast(e == null ? "✅ The key works" : "❌ " + e.getMessage()); }
                });
            }
        }));
        ai.addView(kr);
        CheckBox online = new CheckBox(this);
        online.setText("Online features (AI, free picture/sound search)");
        online.setChecked(Prefs.online(this));
        online.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) { Prefs.put(MainActivity.this, "online", on ? "1" : "0"); }
        });
        ai.addView(online);
        CheckBox aa = new CheckBox(this);
        aa.setText("Studio creates missing characters and backgrounds with free AI in 3D animated style (internet) — recommended");
        aa.setChecked(Prefs.autoArt(this));
        aa.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) { Prefs.put(MainActivity.this, "autoArt", on ? "1" : "0"); }
        });
        ai.addView(aa);
        CheckBox hq = new CheckBox(this);
        hq.setText("Human QC: before a film is made, show me the first frame of every shot to check and fix — recommended");
        hq.setChecked(Prefs.humanQc(this));
        hq.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) { Prefs.put(MainActivity.this, "humanQc", on ? "1" : "0"); }
        });
        ai.addView(hq);
        ai.addView(Ui.small(this, "📜 The director's protocol (hardcoded)", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { showProtocol(); }
        }));
        CheckBox nv = new CheckBox(this);
        nv.setText("Natural voices (Microsoft's free neural voices, needs internet, no key) — recommended");
        nv.setChecked(Prefs.naturalVoices(this));
        nv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) { Prefs.put(MainActivity.this, "naturalVoices", on ? "1" : "0"); }
        });
        ai.addView(nv);
        CheckBox av = new CheckBox(this);
        av.setText("AI voices (expressive; needs the key). The free limit is very small — for short films. After the limit, remaining lines use another voice.");
        av.setChecked(Prefs.aiVoices(this));
        av.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) { Prefs.put(MainActivity.this, "aiVoices", on ? "1" : "0"); }
        });
        ai.addView(av);
        body.addView(ai);

        // every tool that takes a key gets its own card, clearly labelled, with where to get the key
        LinearLayout built = Ui.card(this);
        built.addView(Ui.title(this, "✅ Built in — no key needed"));
        built.addView(Ui.text(this, "These work straight away (with internet):\n"
                + "• Natural voices — Microsoft Edge neural voices (Hindi and English)\n"
                + "• AI pictures and story reading — Pollinations\n"
                + "• Free pictures and real sound recordings — Openverse and Wikimedia Commons\n"
                + "• The phone's own voice, offline\n\n"
                + "Why no keys come inside the app: a key is a personal password tied to one account. A key built into an app "
                + "can be read by anyone who has the app (and this app's code is public), so it is misused and switched off "
                + "within days. Each key below is free; make your own in a minute and it stays only on this phone.", 13, Ui.SUB, false));
        body.addView(built);
        final String[][] tools = {
                {"elevenKey", "🔑 ElevenLabs — the most lifelike voices",
                        "Lifelike Hindi and English voices; each character gets its own voice and keeps it.\nGet the key: elevenlabs.io → sign in → your profile (bottom left) → API keys → Create. Free plan: about 10,000 characters a month.",
                        "ElevenLabs API key (sk_…)"},
                {"freesoundKey", "🔑 Freesound — real sound recordings",
                        "Thousands of real recordings (rain, rivers, crowds, animals) for the backgrounds and effects.\nGet the key: freesound.org/apiv2/apply → sign in → Create new API credentials → copy \"Client secret/Api key\".",
                        "Freesound API key"},
                {"pixabayKey", "🔑 Pixabay — pictures and illustrations",
                        "Free photos and illustrations for places, objects and title pages.\nGet the key: pixabay.com/api/docs → sign in → your key is shown on that page under \"Parameters\".",
                        "Pixabay API key"},
                {"pexelsKey", "🔑 Pexels — photos",
                        "Free high-quality photos for places and objects.\nGet the key: pexels.com/api → sign in → Your API key.",
                        "Pexels API key"},
        };
        for (final String[] tl : tools) {
            LinearLayout card = Ui.card(this);
            card.addView(Ui.title(this, tl[1]));
            card.addView(Ui.text(this, tl[2], 13, Ui.SUB, false));
            final EditText f = new EditText(this);
            f.setHint(tl[3]);
            f.setSingleLine(true);
            f.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
            f.setText(Prefs.get(this, tl[0], ""));
            card.addView(f);
            final TextView state = Ui.text(this, Prefs.get(this, tl[0], "").length() > 10 ? "Key saved on this phone" : "No key yet — the built-in free tools are used", 12,
                    Prefs.get(this, tl[0], "").length() > 10 ? Ui.GREEN : Ui.SUB, false);
            card.addView(state);
            LinearLayout r = Ui.row(this);
            r.addView(Ui.small(this, "💾 Save", Ui.GREEN, new View.OnClickListener() {
                public void onClick(View v) {
                    Prefs.put(MainActivity.this, tl[0], f.getText().toString().trim());
                    state.setText(f.getText().toString().trim().length() > 10 ? "Key saved on this phone" : "Key removed");
                    toast("Saved on this phone");
                }
            }));
            r.addView(Ui.small(this, "🧪 Test", Ui.BLUE, new View.OnClickListener() {
                public void onClick(View v) {
                    Prefs.put(MainActivity.this, tl[0], f.getText().toString().trim());
                    background("Testing…", new Work() {
                        public Object run() throws Exception { return testKey(tl[0]); }
                    }, new Done() {
                        public void done(Object res, Exception e) {
                            String m = e == null ? "✅ The key works" + (res == null ? "" : " — " + res) : "❌ " + e.getMessage();
                            state.setText(m);
                            toast(m);
                        }
                    });
                }
            }));
            r.addView(Ui.small(this, "🗑 Clear", Ui.RED, new View.OnClickListener() {
                public void onClick(View v) { f.setText(""); Prefs.put(MainActivity.this, tl[0], ""); state.setText("Key removed"); }
            }));
            card.addView(r);
            body.addView(card);
        }

        LinearLayout ph = Ui.card(this);
        ph.addView(Ui.title(this, "Phone"));
        ph.addView(Ui.small(this, "🔊 Phone voices (Text-to-Speech)", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { offerTtsInstall(); }
        }));
        ph.addView(Ui.small(this, "🔐 Give permissions", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) {
                String[] m = neededPermissions();
                if (m.length == 0) toast("✅ All permissions are given");
                else requestPermissions(m, REQ_PERMS);
            }
        }));
        body.addView(ph);
    }
}
