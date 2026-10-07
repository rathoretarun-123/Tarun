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
            REQ_SAVE_TEXT = 17, REQ_PERMS = 20, REQ_PERM_GALLERY = 21, REQ_PERM_ONE = 22;
    static final int S_HOME = 0, S_STORY = 1, S_STUDIO = 2, S_FACE = 3, S_PROGRESS = 4, S_PLAYER = 5, S_LIBRARY = 6,
            S_SETTINGS = 7, S_LINES = 8, S_LOGIN = 9;

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
        library = new Library(this);
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
            new AlertDialog.Builder(this).setTitle("पिछली बार ऐप बंद हो गया था")
                    .setMessage("माफ़ कीजिए। आपकी कहानी सुरक्षित है। अगर बार-बार हो तो 720p चुनकर देखें.\n\n" + crash)
                    .setPositiveButton("ठीक है", null).show();
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
            case S_STUDIO: case S_PROGRESS: case S_PLAYER: showStory(); break;
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
        LinearLayout body = page(S_LOGIN, "🎬 कहानी फ़िल्म", false);
        LinearLayout c = Ui.card(this);
        c.addView(Ui.text(this, "स्वागत है!", 22, Ui.TEXT, true));
        c.addView(Ui.text(this, "अपनी कहानी से बच्चों की कार्टून फ़िल्म बनाइए — आवाज़ें, संगीत, कैमरा और दृश्य स्टूडियो खुद तैयार करता है।\n\nशुरू करने के लिए अपने Gmail (Google खाते) से साइन इन करें।", 16, Ui.SUB, false));
        c.addView(Ui.button(this, "G   Gmail से साइन इन करें", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { login(); }
        }));
        c.addView(Ui.small(this, "अभी नहीं", Ui.SUB, new View.OnClickListener() {
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
            et.setHint("आपका Gmail पता");
            et.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS | InputType.TYPE_CLASS_TEXT);
            new AlertDialog.Builder(this).setTitle("Gmail पता लिखें").setView(et)
                    .setPositiveButton("ठीक है", new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface d, int w) {
                            String m = et.getText().toString().trim();
                            if (m.contains("@")) { Prefs.setAccount(MainActivity.this, m); askPermissions(); showHome(); }
                        }
                    }).show();
        }
    }

    private void logout() {
        new AlertDialog.Builder(this).setTitle("लॉग आउट करें?")
                .setMessage("आपकी फ़िल्में और लाइब्रेरी इस फ़ोन में सुरक्षित रहेंगी।")
                .setPositiveButton("हाँ, लॉग आउट", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        Prefs.setAccount(MainActivity.this, "");
                        Prefs.put(MainActivity.this, "skipLogin", "0");
                        project = null;
                        showLogin();
                    }
                }).setNegativeButton("नहीं", null).show();
    }

    private String[] neededPermissions() {
        List<String> p = new ArrayList<String>();
        p.add(Manifest.permission.RECORD_AUDIO);
        p.add(Manifest.permission.CAMERA);
        if (Build.VERSION.SDK_INT >= 33) p.add("android.permission.POST_NOTIFICATIONS");
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
            else toast("अनुमति नहीं मिली — सेटिंग्स > ऐप्स > कहानी फ़िल्म > अनुमतियाँ में चालू करें");
        } else if (code == REQ_PERM_GALLERY) {
            if (res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) saveToGallery(pendingShare);
            else toast("गैलरी में सहेजने की अनुमति नहीं मिली");
        }
    }

    // ================================================================== home (dashboard)

    private void showHome() {
        project = null;
        LinearLayout body = page(S_HOME, "🎬 कहानी फ़िल्म", false);
        String acc = Prefs.account(this);
        LinearLayout top = Ui.row(this);
        top.setPadding(Ui.dp(this, 16), Ui.dp(this, 4), Ui.dp(this, 12), 0);
        top.setGravity(Gravity.CENTER_VERTICAL);
        TextView who = Ui.text(this, acc.length() > 0 ? "👤 " + acc : "👤 साइन इन नहीं", 14, Ui.SUB, false);
        top.addView(who, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        top.addView(Ui.small(this, acc.length() > 0 ? "लॉग आउट" : "साइन इन", Ui.SUB, new View.OnClickListener() {
            public void onClick(View v) { if (Prefs.account(MainActivity.this).length() > 0) logout(); else login(); }
        }));
        body.addView(top);

        LinearLayout hero = Ui.card(this);
        hero.addView(Ui.text(this, "अपनी कहानी से कार्टून फ़िल्म बनाइए", 18, Ui.TEXT, true));
        hero.addView(Ui.text(this, "1. कहानी चिपकाएँ   2. (चाहें तो) चित्र-आवाज़ दें   3. ‘फ़िल्म बनाएँ’", 15, Ui.SUB, false));
        hero.addView(Ui.button(this, "➕  नई फ़िल्म", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { project = Project.create(MainActivity.this); showStory(); }
        }));
        hero.addView(Ui.button(this, "📖  उदाहरण: रत्नगढ़ की दो राजकुमारियाँ", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { openSample(); }
        }));
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "📚 लाइब्रेरी", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { showLibrary(); }
        }));
        r.addView(Ui.small(this, "⚙ सेटिंग", Ui.SUB, new View.OnClickListener() {
            public void onClick(View v) { showSettings(); }
        }));
        hero.addView(r);
        body.addView(hero);

        FilmJob job = FilmJob.current;
        if (job != null && !job.done && !job.failed && !job.cancelled) {
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, "⏳ एक फ़िल्म बन रही है: " + job.project.name(), 16, Ui.TEXT, true));
            final Project jp = job.project;
            c.addView(Ui.button(this, "प्रगति देखें", Ui.GREEN, new View.OnClickListener() {
                public void onClick(View v) { project = jp; showProgress(); }
            }));
            body.addView(c);
        }

        List<Project> ps = Project.all(this);
        if (!ps.isEmpty()) heading(body, "मेरी फ़िल्में");
        for (final Project p : ps) {
            LinearLayout c = Ui.card(this);
            final boolean made = p.film().exists();
            c.addView(Ui.text(this, p.name(), 17, Ui.TEXT, true));
            long secs = 0;
            try { secs = Long.parseLong(p.setting("filmSeconds", "0")); } catch (NumberFormatException ignored) {}
            c.addView(Ui.text(this, made ? "✅ फ़िल्म तैयार (" + FilmJob.fmt(secs) + ")" : "✏️ अधूरी", 14, made ? Ui.GREEN : Ui.SUB, false));
            LinearLayout rr = Ui.row(this);
            rr.addView(Ui.small(this, "खोलें", Ui.PRIMARY, new View.OnClickListener() {
                public void onClick(View v) { project = p; showStory(); }
            }));
            if (made) rr.addView(Ui.small(this, "▶ देखें", Ui.GREEN, new View.OnClickListener() {
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
        background("उदाहरण तैयार हो रहा है…", new Work() {
            public Object run() throws Exception { return Project.createSample(MainActivity.this); }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("उदाहरण नहीं खुला: " + e.getMessage()); return; }
                project = (Project) r;
                showStory();
            }
        });
    }

    private void confirmDelete(final Project p) {
        new AlertDialog.Builder(this).setTitle("हटाएँ?").setMessage("\"" + p.name() + "\" और इसकी फ़िल्म हमेशा के लिए हट जाएगी।")
                .setPositiveButton("हाँ, हटाएँ", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { deleteDir(p.dir); showHome(); }
                }).setNegativeButton("नहीं", null).show();
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
        c1.addView(Ui.title(this, "1. कहानी"));
        c1.addView(Ui.text(this, "किसी भी तरह लिखें — हिंदी या English, कहानी के रूप में या पटकथा (नाम (भाव): \"संवाद\") के रूप में। पात्रों और जगहों का विवरण केवल चित्र बनाने के काम आता है, पढ़ा नहीं जाता।", 13, Ui.SUB, false));
        scriptBox = new EditText(this);
        scriptBox.setText(project.read("script.txt"));
        scriptBox.setHint("यहाँ अपनी कहानी चिपकाएँ…");
        scriptBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        scriptBox.setGravity(Gravity.TOP);
        scriptBox.setMinLines(8);
        scriptBox.setVerticalScrollBarEnabled(true);
        scriptBox.setTextSize(15);
        scriptBox.setBackground(Ui.round(0xFFFFFDF7, Ui.dp(this, 10), 0x33000000, Ui.dp(this, 1)));
        scriptBox.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));
        c1.addView(scriptBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 240)));
        LinearLayout r1 = Ui.row(this);
        r1.addView(Ui.small(this, "📋 चिपकाएँ", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) {
                ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                if (cm != null && cm.hasPrimaryClip() && cm.getPrimaryClip().getItemCount() > 0) {
                    CharSequence t = cm.getPrimaryClip().getItemAt(0).coerceToText(MainActivity.this);
                    scriptBox.setText(t);
                    saveScript();
                } else toast("क्लिपबोर्ड खाली है");
            }
        }));
        r1.addView(Ui.small(this, "📂 फ़ाइल", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { pick("text/*", REQ_SCRIPT, false); }
        }));
        r1.addView(Ui.small(this, "🧹 साफ़ करें", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) {
                if (scriptBox.getText().length() == 0) return;
                new AlertDialog.Builder(MainActivity.this).setTitle("कहानी साफ़ करें?").setMessage("चिपकाई हुई पूरी कहानी हट जाएगी।")
                        .setPositiveButton("हाँ, साफ़ करें", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { scriptBox.setText(""); saveScript(); }
                        }).setNegativeButton("नहीं", null).show();
            }
        }));
        c1.addView(r1);
        boolean hasAi = project.has("script_ai.txt");
        final boolean useAi = "1".equals(project.setting("useAi", "0")) && hasAi;
        c1.addView(Ui.button(this, "🤖  AI से कहानी पढ़वाएँ (पटकथा बनाएँ)", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { aiRead(); }
        }));
        if (hasAi) {
            CheckBox cb = new CheckBox(this);
            cb.setText("AI की बनाई पटकथा इस्तेमाल करें");
            cb.setChecked(useAi);
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean on) { project.setSetting("useAi", on ? "1" : "0"); }
            });
            c1.addView(cb);
            c1.addView(Ui.small(this, "AI की पटकथा देखें", Ui.SUB, new View.OnClickListener() {
                public void onClick(View v) { showText("AI की पटकथा", project.read("script_ai.txt")); }
            }));
        }
        body.addView(c1);

        LinearLayout c2 = Ui.card(this);
        c2.addView(Ui.title(this, "2. स्टूडियो: पात्र, आवाज़ें, जगहें"));
        c2.addView(Ui.text(this, "निर्देशक कहानी पढ़कर बताएगा क्या-क्या चाहिए। आप चित्र/आवाज़ दें, या स्टूडियो को खुद चुनने दें। यहीं से प्रोडक्शन फ़ाइल भी डाउनलोड करें।", 13, Ui.SUB, false));
        c2.addView(Ui.button(this, "🎭  स्टूडियो खोलें", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { saveScript(); showStudio(); }
        }));
        body.addView(c2);

        LinearLayout c3 = Ui.card(this);
        c3.addView(Ui.title(this, "3. कहाँ डालेंगे?"));
        final Edits ed = edits();
        final String[][] plats = {{"YouTube (चौड़ा 16:9)", "16:9"}, {"Instagram Reel / YouTube Shorts (खड़ा 9:16)", "9:16"},
                {"Facebook / Instagram पोस्ट (चौकोर 1:1)", "1:1"}};
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
        c3.addView(Ui.text(this, "गुणवत्ता:", 14, Ui.SUB, false));
        RadioGroup qg = new RadioGroup(this);
        qg.setOrientation(RadioGroup.HORIZONTAL);
        final int[] qs = {480, 720, 1080};
        final String[] ql = {"480p (तेज़)", "720p HD", "1080p Full HD"};
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
        c4.addView(Ui.button(this, "🎬  फ़िल्म बनाएँ", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { makeFilm(); }
        }));
        c4.addView(Ui.text(this, "फ़िल्म बनते समय आप फ़ोन लॉक कर सकते हैं या दूसरा ऐप खोल सकते हैं — काम चलता रहेगा।", 13, Ui.SUB, false));
        if (project.film().exists()) {
            c4.addView(Ui.button(this, "▶  बनी हुई फ़िल्म देखें / बदलाव करें", Ui.PRIMARY, new View.OnClickListener() {
                public void onClick(View v) { showPlayer(); }
            }));
        }
        body.addView(c4);
    }

    private void aiRead() {
        saveScript();
        final String text = project.read("script.txt");
        if (text.trim().length() < 20) { toast("पहले कहानी लिखें या चिपकाएँ"); return; }
        if (!Prefs.online(this)) { toast("सेटिंग में ऑनलाइन सुविधाएँ चालू करें"); return; }
        background("AI कहानी पढ़ रहा है… (1-2 मिनट)", new Work() {
            public Object run() { return ScriptAI.read(Prefs.cloud(MainActivity.this), text); }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("नहीं हो सका: " + e.getMessage()); return; }
                ScriptAI.Result res = (ScriptAI.Result) r;
                if (res.rewritten) {
                    project.write("script_ai.txt", res.script);
                    project.setSetting("useAi", "1");
                }
                new AlertDialog.Builder(MainActivity.this).setTitle(res.rewritten ? "✅ पटकथा तैयार" : "AI")
                        .setMessage(res.note + (res.rewritten ? "\n\nअब फ़िल्म इसी पटकथा से बनेगी। आपकी मूल कहानी भी सुरक्षित है।" : ""))
                        .setPositiveButton("ठीक है", null).show();
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
        new AlertDialog.Builder(this).setTitle(title).setView(sv).setPositiveButton("बंद करें", null).show();
    }

    // ================================================================== studio

    private Story loadStory() {
        Story st = ScriptParser.parse(FilmJob.scriptOf(project));
        castStory = st;
        return st;
    }

    private void showStudio() {
        if (project == null) { showHome(); return; }
        LinearLayout body = page(S_STUDIO, "🎭 स्टूडियो", true);
        final Story st;
        try {
            st = loadStory();
        } catch (Throwable e) {
            body.addView(Ui.text(this, "कहानी पढ़ी नहीं जा सकी: " + e.getMessage(), 16, Ui.RED, true));
            return;
        }
        // ---- summary and what is missing
        int noPic = 0, noVoice = 0, noBg = 0;
        for (Story.CharacterDef c : st.characters) {
            if (charFile(c) == null) noPic++;
            if (library.byId(project.setting("vsample." + c.displayName, "")) == null) noVoice++;
        }
        for (Story.Scene sc : st.scenes) if (sceneFile(sc) == null) noBg++;
        LinearLayout sum = Ui.card(this);
        sum.addView(Ui.title(this, "📜 " + st.title));
        sum.addView(Ui.text(this, "भाषा: " + (st.hindi ? "हिंदी (अंत में \"समाप्त\")" : "English (ends with \"The End\")")
                + "\nपात्र: " + st.characters.size() + "   •   भाग: " + st.scenes.size() + "   •   संवाद: " + st.dialogueCount()
                + (st.hasNarrator ? "\nकथावाचक की आवाज़: हाँ (कहानी में कथावाचक है)" : ""), 15, Ui.TEXT, false));
        String miss = "क्या बाकी है:\n• " + noPic + " पात्रों के चित्र  • " + noVoice + " पात्रों की आवाज़ के नमूने  • " + noBg + " भागों की पृष्ठभूमि"
                + "\nजो आप नहीं देंगे, स्टूडियो अपनी लाइब्रेरी से खुद बनाएगा।";
        sum.addView(Ui.text(this, miss, 14, noPic + noVoice + noBg == 0 ? Ui.GREEN : Ui.PRIMARY_DARK, false));
        for (String w : st.warnings) sum.addView(Ui.text(this, "⚠ " + w, 13, Ui.RED, false));
        if (st.dialogueCount() == 0) sum.addView(Ui.text(this, "⚠ कोई संवाद नहीं मिला। \"AI से कहानी पढ़वाएँ\" दबाएँ या ऐसे लिखें — नाम: \"संवाद\"", 14, Ui.RED, true));
        sum.addView(Ui.button(this, "📄  प्रोडक्शन फ़ाइल (पात्र, शॉट, आवाज़ें, ध्वनियाँ)", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { productionFile(st); }
        }));
        sum.addView(Ui.button(this, "📥  कई चित्र एक साथ जोड़ें (नाम से पहचान)", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { pick("image/*", REQ_BULK, true); }
        }));
        sum.addView(Ui.button(this, "🎙  संवाद अपनी आवाज़ में रिकॉर्ड करें", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { showLines(); }
        }));
        body.addView(sum);

        // ---- title and end pages
        heading(body, "शुरुआत और अंत");
        body.addView(pageCard("शीर्षक पृष्ठ", "title"));
        body.addView(pageCard(st.hindi ? "अंतिम पृष्ठ (समाप्त)" : "End page (The End)", "end"));

        // ---- characters
        heading(body, "पात्र (चित्र और आवाज़)");
        for (Story.CharacterDef c : st.characters) body.addView(characterCard(st, c));
        if (st.hasNarrator) body.addView(narratorCard());

        // ---- parts of the story
        heading(body, "जगहें (पृष्ठभूमि और आवाज़ें)");
        SoundLib sl = library.soundLib();
        for (Story.Scene sc : st.scenes) body.addView(sceneCard(sc, sl));

        // ---- director plan
        LinearLayout plan = Ui.card(this);
        plan.addView(Ui.title(this, "🎬 निर्देशक की योजना"));
        try {
            Director d = new Director(st, new Director.Options());
            d.prepare();
            Film f = d.direct(new Art());
            StringBuilder sb = new StringBuilder();
            for (String n : f.notes) sb.append("• ").append(n).append('\n');
            sb.append("\nअनुमानित लंबाई: ").append(FilmJob.fmt((long) f.duration));
            if (f.duration > 30 * 60) sb.append("\n⚠ 30 मिनट से लंबी — बनने में ज़्यादा समय लगेगा।");
            plan.addView(Ui.text(this, sb.toString(), 14, Ui.TEXT, false));
        } catch (Throwable e) {
            plan.addView(Ui.text(this, "योजना नहीं बन सकी: " + e, 14, Ui.RED, false));
        }
        plan.addView(Ui.button(this, "🎬  फ़िल्म बनाएँ", Ui.GREEN, new View.OnClickListener() {
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
            cb.setText(kind.equals("title") ? "चित्र पर शीर्षक लिखें" : "चित्र पर \"समाप्त\" लिखें");
            cb.setChecked(!line.endsWith("|0"));
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean on) { project.setManifest(kind, kind, kind + "|" + f + "|" + (on ? "1" : "0")); }
            });
            c.addView(cb);
        } else {
            c.addView(Ui.text(this, "🎨 स्टूडियो खुद सुंदर पृष्ठ बनाएगा (संगीत के साथ)", 13, Ui.SUB, false));
        }
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "🖼 चित्र चुनें", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { choosePicture(kind, kind.equals("title") ? castStory.title : "end समाप्त"); }
        }));
        if (file != null) r.addView(Ui.small(this, "✖ हटाएँ", Ui.RED, new View.OnClickListener() {
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
        info.addView(Ui.text(this, c.displayName, 18, Ui.TEXT, true));
        info.addView(Ui.text(this, describe(c), 13, Ui.SUB, false));
        boolean mouthSet = line != null && line.split("\\|").length >= 11;
        info.addView(Ui.text(this, file != null ? "🖼 आपका चित्र" + (mouthSet ? " • 👄 मुँह सेट" : " • 👄 अपने आप") : "🎨 स्टूडियो का कार्टून", 13, file != null ? Ui.GREEN : Ui.SUB, false));
        Library.Item vs = library.byId(project.setting("vsample." + c.displayName, ""));
        String vtxt = vs != null ? "🎙 आवाज़: आपका नमूना \"" + vs.label() + "\"" : (Prefs.geminiKey(this).length() > 20 && Prefs.aiVoices(this) ? "✨ आवाज़: AI (भाव के साथ)"
                : Prefs.online(this) && Prefs.naturalVoices(this) ? "🗣 आवाज़: प्राकृतिक (न्यूरल)" : "📱 आवाज़: फ़ोन की आवाज़");
        info.addView(Ui.text(this, vtxt, 13, vs != null ? Ui.GREEN : Ui.SUB, false));
        top.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(top);
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "🖼 चित्र", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { choosePicture("char:" + keyFor(c), c.displayName + " " + c.description); }
        }));
        r.addView(Ui.small(this, "🎙 आवाज़", Ui.GREEN, new View.OnClickListener() {
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
        card.addView(Ui.text(this, "🎙 कथावाचक (केवल आवाज़)", 17, Ui.TEXT, true));
        Library.Item vs = library.byId(project.setting("vsample.narrator", ""));
        card.addView(Ui.text(this, vs != null ? "आपका नमूना: " + vs.label() : "स्टूडियो की आवाज़", 13, Ui.SUB, false));
        card.addView(Ui.small(this, "🎙 आवाज़ चुनें", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { chooseVoice("narrator", castStory, null); }
        }));
        return card;
    }

    private LinearLayout sceneCard(final Story.Scene sc, SoundLib sl) {
        LinearLayout card = Ui.card(this);
        final String key = String.valueOf(sc.number);
        final String file = sceneFile(sc);
        card.addView(Ui.text(this, (castStory.hindi ? "भाग " : "Part ") + sc.number + (sc.title.length() > 0 ? ": " + sc.title : ""), 16, Ui.TEXT, true));
        card.addView(Ui.text(this, "जगह: " + Sets.name(Sets.detect(sc.setting)) + "   •   संवाद: " + countDialogue(sc), 13, Ui.SUB, false));
        if (file != null) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(thumb(project.file(file), 500));
            card.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 120)));
        } else card.addView(Ui.text(this, "🎨 स्टूडियो की बनाई पृष्ठभूमि", 13, Ui.SUB, false));
        Library.Item amb = library.byId(project.setting("amb." + key, ""));
        SoundLib.Entry auto = sl.best(sc.setting + " " + sc.title, "amb", null);
        card.addView(Ui.text(this, "🔊 पृष्ठभूमि ध्वनि: " + (amb != null ? amb.label() + " (आपकी पसंद)" : (auto != null ? auto.title + " (अपने आप)" : "अपने आप")), 13, amb != null ? Ui.GREEN : Ui.SUB, false));
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "🖼 पृष्ठभूमि", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { choosePicture("scene:" + key, sc.setting); }
        }));
        r.addView(Ui.small(this, "🔊 ध्वनि", Ui.GREEN, new View.OnClickListener() {
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
            case Look.GIRL: kind = "लड़की"; break;
            case Look.BOY: kind = "लड़का"; break;
            case Look.WOMAN: kind = "महिला"; break;
            case Look.WITCH: kind = "चुड़ैल"; break;
            case Look.MONSTER: kind = "राक्षस"; break;
            case Look.MONKEY: kind = "बंदर"; break;
            case Look.ANIMAL: kind = "जानवर"; break;
            case Look.BIRD: kind = "पक्षी"; break;
            default: kind = "पुरुष";
        }
        return kind + (c.age > 0 ? ", " + c.age + " वर्ष" : "") + (l.hero ? "" : " • खलनायक") + (c.fromScript ? "" : " • (विवरण नहीं मिला)");
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

    private void productionFile(final Story st) {
        final Set<String> pics = new HashSet<String>(), voices = new HashSet<String>();
        for (Story.CharacterDef c : st.characters) {
            if (charFile(c) != null) pics.add(com.tarun.kahani.core.Txt.norm(c.displayName));
            if (library.byId(project.setting("vsample." + c.displayName, "")) != null) voices.add(com.tarun.kahani.core.Txt.norm(c.displayName));
        }
        final SoundLib sl = library.soundLib();
        background("प्रोडक्शन फ़ाइल बन रही है…", new Work() {
            public Object run() { return Bible.write(st, sl, pics, voices); }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("नहीं बन सकी: " + e.getMessage()); return; }
                final String text = (String) r;
                final String name = safeName(st.title) + "_production.txt";
                new AlertDialog.Builder(MainActivity.this).setTitle("📄 प्रोडक्शन फ़ाइल तैयार")
                        .setMessage("इसमें हर पात्र, जगह, शॉट, आवाज़ और ध्वनि का विवरण है, और चित्र बनाने के लिए तैयार Prompt भी।")
                        .setPositiveButton("💾 डाउनलोड करें", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { saveTextFile(name, text); }
                        })
                        .setNeutralButton("📤 साझा करें", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { shareText(name, text); }
                        })
                        .setNegativeButton("👁 यहीं पढ़ें", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { showText("प्रोडक्शन फ़ाइल", text); }
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
                if (uri == null) throw new Exception("Downloads उपलब्ध नहीं");
                OutputStream o = getContentResolver().openOutputStream(uri);
                o.write(text.getBytes("UTF-8"));
                o.close();
                toast("✅ Downloads में सहेजा: " + name);
            } else shareText(name, text);
        } catch (Exception e) {
            toast("सहेजा नहीं जा सका: " + e.getMessage());
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
            startActivity(Intent.createChooser(s, "साझा करें"));
        } catch (Exception e) {
            toast("साझा नहीं हो सका");
        }
    }

    // ================================================================== choosing pictures

    private String targetName(String t) {
        if (t.startsWith("char:")) return t.substring(5);
        if (t.startsWith("scene:")) return (castStory != null && castStory.hindi ? "भाग " : "Part ") + t.substring(6);
        if (t.equals("title")) return "शीर्षक";
        if (t.equals("end")) return "समाप्त";
        return "";
    }

    private void choosePicture(final String tgt, final String query) {
        target = tgt;
        new Picker(this, library).show("चित्र: " + targetName(tgt), Library.PIC, query,
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
        background("चित्र लगाया जा रहा है…", new Work() {
            public Object run() throws Exception {
                InputStream in = library.open(it);
                return project.savePicture(Project.readAll(in), tgt.startsWith("char:") ? "char" : tgt.startsWith("scene:") ? "scene" : tgt);
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("चित्र नहीं लगा: " + e.getMessage()); return; }
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
                toast("✅ लगा दिया");
                showStudio();
            }
        });
    }

    /** A picture arrived from the phone or camera: offer the cartoon (avatar) look, save to library, use it. */
    private void incomingPicture(final byte[] data, final String fileName) {
        final String tgt = target == null ? "lib:pic" : target;
        final boolean person = tgt.startsWith("char:") || tgt.equals("lib:pic");
        DialogInterface.OnClickListener go = new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface d, final int which) {
                final boolean toon = which == DialogInterface.BUTTON_POSITIVE;
                background(toon ? "कार्टून बनाया जा रहा है…" : "सहेजा जा रहा है…", new Work() {
                    public Object run() throws Exception {
                        byte[] bytes = toon ? toonify(data) : data;
                        String kind = tgt.startsWith("char:") ? "person" : tgt.startsWith("scene:") ? "place" : tgt.equals("title") || tgt.equals("end") ? tgt : "";
                        String name = targetName(tgt);
                        if (name.length() == 0 && fileName != null) name = fileName.replaceAll("\\.[A-Za-z0-9]+$", "");
                        return library.addBytes(Library.PIC, kind, name, fileName == null ? "" : fileName, bytes, ".jpg", toon ? "photo → cartoon" : "phone");
                    }
                }, new Done() {
                    public void done(Object r, Exception e) {
                        if (e != null) { toast("नहीं हो सका: " + e.getMessage()); return; }
                        usePicture((Library.Item) r);
                    }
                });
            }
        };
        new AlertDialog.Builder(this).setTitle("चित्र कैसा रखें?")
                .setMessage(person ? "अगर यह असली फ़ोटो है तो स्टूडियो इसे कार्टून (अवतार) में बदल सकता है।" : "असली फ़ोटो को कार्टून जैसा बनाया जा सकता है।")
                .setPositiveButton("🎨 कार्टून बनाएँ", go).setNegativeButton("जैसा है वैसा", go).show();
    }

    static byte[] toonify(byte[] data) throws Exception {
        BitmapFactory.Options o = new BitmapFactory.Options();
        o.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(data, 0, data.length, o);
        int s = 1;
        while (Math.max(o.outWidth, o.outHeight) / (s * 2) >= 1200) s *= 2;
        o = new BitmapFactory.Options();
        o.inSampleSize = s;
        o.inPreferredConfig = Bitmap.Config.ARGB_8888;
        Bitmap b = BitmapFactory.decodeByteArray(data, 0, data.length, o);
        if (b == null) throw new Exception("चित्र पढ़ा नहीं जा सका");
        b = Project.upright(b, data);
        int w = b.getWidth(), h = b.getHeight();
        int[] px = new int[w * h];
        b.getPixels(px, 0, w, 0, 0, w, h);
        b.recycle();
        int[] t = Toon.apply(px, w, h);
        Bitmap out = Bitmap.createBitmap(t, w, h, Bitmap.Config.ARGB_8888);
        java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
        out.compress(Bitmap.CompressFormat.JPEG, 92, bo);
        out.recycle();
        return bo.toByteArray();
    }

    private void camera() {
        withPermission(Manifest.permission.CAMERA, "कैमरा की अनुमति दें", new Runnable() {
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
                    toast("कैमरा नहीं खुला");
                }
            }
        });
    }

    private void searchPictures(String query) {
        if (!Prefs.online(this)) { toast("सेटिंग में ऑनलाइन सुविधाएँ चालू करें"); return; }
        final EditText q = new EditText(this);
        String s = query == null ? "" : query;
        q.setText(s.length() > 60 ? s.substring(0, 60) : s);
        new AlertDialog.Builder(this).setTitle("🌐 मुफ़्त चित्र खोजें (Openverse, Wikimedia)").setView(q)
                .setPositiveButton("खोजें", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        final String text = q.getText().toString().trim();
                        background("खोज रहे हैं…", new Work() {
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
                                if (out.isEmpty()) throw new Exception(c.lastError.length() > 0 ? c.lastError : "कुछ नहीं मिला");
                                return out;
                            }
                        }, new Done() {
                            @SuppressWarnings("unchecked")
                            public void done(Object r, Exception e) {
                                if (e != null) { toast("नहीं मिला: " + e.getMessage()); return; }
                                showFound((List<Object[]>) r, text);
                            }
                        });
                    }
                }).setNegativeButton("रद्द", null).show();
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
        body.addView(Ui.small(this, "अगले 4 ▶", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { page[0] = (page[0] + 1) * 4 < found.size() ? page[0] + 1 : 0; fill[0].run(); }
        }));
        body.addView(Ui.text(this, "चित्र मुफ़्त लाइसेंस वाले हैं (स्रोत लाइब्रेरी में लिखा रहेगा)।", 12, Ui.SUB, false));
        fill[0].run();
        d[0] = new AlertDialog.Builder(this).setTitle("चित्र चुनें").setView(body).setNegativeButton("बंद", null).show();
    }

    private void downloadFound(final Cloud.Found f, final String query) {
        background("चित्र डाउनलोड हो रहा है…", new Work() {
            public Object run() throws Exception {
                byte[] b = Prefs.cloud(MainActivity.this).download(f.url);
                BitmapFactory.Options o = new BitmapFactory.Options();
                o.inJustDecodeBounds = true;
                BitmapFactory.decodeByteArray(b, 0, b.length, o);
                if (o.outWidth <= 0) throw new Exception("यह चित्र खुला नहीं");
                return b;
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("डाउनलोड नहीं हुआ: " + e.getMessage()); return; }
                incomingPicture((byte[]) r, f.title + " (" + f.source + " " + f.license + " " + f.creator + ")");
            }
        });
    }

    private void aiPicture() {
        if (!Prefs.online(this)) { toast("सेटिंग में ऑनलाइन सुविधाएँ चालू करें"); return; }
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
            prompt = "Movie poster style title picture for a children's Indian cartoon film named '" + castStory.title + "', main characters together, colourful, no text.";
            tall = false;
        } else {
            prompt = "Beautiful calm sunset landscape for the end of a children's Indian cartoon film, colourful, no text.";
            tall = false;
        }
        final EditText et = new EditText(this);
        et.setText(prompt);
        et.setMaxLines(8);
        new AlertDialog.Builder(this).setTitle("✨ AI से चित्र (मुफ़्त, बिना कुंजी)").setView(et)
                .setPositiveButton("बनाएँ", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { makeAiPicture(et.getText().toString(), tall); }
                }).setNegativeButton("रद्द", null).show();
    }

    private void makeAiPicture(final String prompt, final boolean tall) {
        final int seed = (int) (System.currentTimeMillis() % 100000);
        background("AI चित्र बना रहा है… (30-60 सेकंड)", new Work() {
            public Object run() throws Exception {
                return Prefs.cloud(MainActivity.this).makePicture(prompt, tall ? 768 : 1280, tall ? 1152 : 720, seed);
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("चित्र नहीं बना: " + e.getMessage()); return; }
                final byte[] b = (byte[]) r;
                ImageView iv = new ImageView(MainActivity.this);
                iv.setAdjustViewBounds(true);
                iv.setImageBitmap(decodeSmall(b, 900));
                new AlertDialog.Builder(MainActivity.this).setTitle("पसंद आया?").setView(iv)
                        .setPositiveButton("✔ इस्तेमाल करें", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) {
                                try {
                                    String tgt = target == null ? "lib:pic" : target;
                                    Library.Item it = library.addBytes(Library.PIC, tgt.startsWith("char:") ? "person" : tgt.startsWith("scene:") ? "place" : tgt,
                                            targetName(tgt), prompt.length() > 200 ? prompt.substring(0, 200) : prompt, b, ".jpg", "AI");
                                    usePicture(it);
                                } catch (Exception ex) { toast("सहेजा नहीं जा सका"); }
                            }
                        })
                        .setNeutralButton("↻ दोबारा", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { makeAiPicture(prompt, tall); }
                        }).setNegativeButton("रद्द", null).show();
            }
        });
    }

    // ================================================================== voices

    private void chooseVoice(final String name, final Story st, final Story.CharacterDef c) {
        target = "voice:" + name;
        new Picker(this, library).show("आवाज़: " + (name.equals("narrator") ? "कथावाचक" : name), Library.VOICE, name,
                new String[]{"record", "phone", "phoneVoice"}, new Picker.Listener() {
                    public void picked(Library.Item it) {
                        project.setSetting("vsample." + name, it.id);
                        toast("✅ " + name + " के सारे संवाद इसी आवाज़ में बनेंगे");
                        if (c != null) previewVoice(st, c, true);
                        showStudio();
                    }
                    public void action(String a) {
                        if (a.equals("record")) record(Library.VOICE, name);
                        else if (a.equals("phone")) pick("audio/*", REQ_AUDIO, false);
                        else {
                            project.setSetting("vsample." + name, "");
                            if (c != null) previewVoice(st, c, false);
                            showStudio();
                        }
                    }
                });
    }

    /** Plays one of the character's lines in the voice the film will use (with the sample applied if any). */
    private void previewVoice(final Story st, final Story.CharacterDef c, final boolean keepVoice) {
        if (previewBusy) { toast("पिछली आवाज़ बन रही है… रुकिए"); return; }
        previewBusy = true;
        final Library.Item vs = library.byId(project.setting("vsample." + c.displayName, ""));
        String sample = st.hindi ? "नमस्ते, मैं " + c.displayName + " हूँ।" : "Hello, I am " + c.displayName + ".";
        for (Story.Scene sc : st.scenes) for (Story.Beat b : sc.beats) if (b.speaker == c) { sample = b.text; break; }
        if (sample.length() > 110) sample = sample.substring(0, 110);
        final String text = sample;
        background(previewVoices == null ? "आवाज़ तैयार हो रही है…" : null, new Work() {
            public Object run() throws Exception {
                boolean natural = Prefs.online(MainActivity.this) && Prefs.naturalVoices(MainActivity.this);
                if (previewVoices == null) {
                    Voices v = new Voices();
                    if (!v.init(MainActivity.this, st.hindi) && !natural) throw new Exception("फ़ोन में Text-to-Speech नहीं मिला");
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
                if (pcm == null) throw new Exception("आवाज़ नहीं बन सकी — Text-to-Speech सेटिंग देखें");
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
                String pos = " " + ((Integer) o[1] + 1) + "/" + Math.max(1, (Integer) o[2]) + " — पसंद न आए तो फिर 🔊 दबाएँ";
                toast("🔊 " + (eng.contains("sample") ? "आपके नमूने वाली आवाज़" : eng.startsWith("AI") ? "AI आवाज़"
                        : eng.startsWith("natural") ? "प्राकृतिक आवाज़" + pos : "फ़ोन की आवाज़" + pos));
                if (eng.startsWith("phone") && previewVoices.edge != null) toast("प्राकृतिक आवाज़ की सेवा नहीं मिली (इंटरनेट देखें) — अभी फ़ोन की आवाज़ सुनाई गई");
                else if (!(Boolean) o[4] && eng.startsWith("phone")) offerTtsInstall();
            }
        });
    }

    private void offerTtsInstall() {
        new AlertDialog.Builder(this).setTitle("फ़ोन की आवाज़")
                .setMessage("अगर हिंदी आवाज़ नहीं आ रही, तो Google Text-to-speech में हिंदी आवाज़ डाउनलोड करें।")
                .setPositiveButton("डाउनलोड करें", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        try { startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)); }
                        catch (Exception e) { toast("सेटिंग्स > भाषा > Text-to-speech में जाएँ"); }
                    }
                }).setNegativeButton("बाद में", null).show();
    }

    // ================================================================== sounds

    private void chooseSound(final String tgt, final String query) {
        target = tgt;
        new Picker(this, library).show("पृष्ठभूमि ध्वनि", Library.SOUND, query, new String[]{"record", "phone", "online", "auto"},
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

    private void useSound(Library.Item it) {
        String tgt = target;
        if (tgt != null && tgt.startsWith("amb:") && project != null) {
            project.setSetting("amb." + tgt.substring(4), it.id);
            toast("✅ पृष्ठभूमि ध्वनि लगा दी");
            showStudio();
        } else if (tgt != null && tgt.startsWith("voice:") && project != null) {
            project.setSetting("vsample." + tgt.substring(6), it.id);
            toast("✅ आवाज़ का नमूना लगा दिया — सारे संवाद इसी आवाज़ में बनेंगे");
            showStudio();
        } else if (screen == S_LIBRARY) showLibrary();
    }

    private void searchSounds(String query) {
        if (!Prefs.online(this)) { toast("सेटिंग में ऑनलाइन सुविधाएँ चालू करें"); return; }
        final EditText q = new EditText(this);
        q.setHint("जैसे: forest birds, rain, river, crowd");
        String s = query == null ? "" : query;
        q.setText(s.length() > 40 ? s.substring(0, 40) : s);
        new AlertDialog.Builder(this).setTitle("🌐 मुफ़्त ध्वनि खोजें (अंग्रेज़ी शब्द बेहतर)").setView(q)
                .setPositiveButton("खोजें", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        final String text = q.getText().toString().trim();
                        background("खोज रहे हैं…", new Work() {
                            public Object run() throws Exception {
                                Cloud c = Prefs.cloud(MainActivity.this);
                                List<Cloud.Found> fs = c.searchSounds(text, 12);
                                if (fs.isEmpty()) throw new Exception(c.lastError.length() > 0 ? c.lastError : "कुछ नहीं मिला");
                                return fs;
                            }
                        }, new Done() {
                            @SuppressWarnings("unchecked")
                            public void done(Object r, Exception e) {
                                if (e != null) { toast("नहीं मिला: " + e.getMessage()); return; }
                                showFoundSounds((List<Cloud.Found>) r, text);
                            }
                        });
                    }
                }).setNegativeButton("रद्द", null).show();
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
            r.addView(Ui.small(this, "▶", Ui.BLUE, new View.OnClickListener() {
                public void onClick(View v) { Picker.play(MainActivity.this, f.url); }
            }));
            r.addView(Ui.small(this, "चुनें", Ui.GREEN, new View.OnClickListener() {
                public void onClick(View v) { Picker.stop(); d[0].dismiss(); downloadSound(f, query); }
            }));
            body.addView(r);
        }
        ScrollView sv = new ScrollView(this);
        sv.addView(body);
        d[0] = new AlertDialog.Builder(this).setTitle("ध्वनि चुनें").setView(sv).setNegativeButton("बंद", null).create();
        d[0].setOnDismissListener(new DialogInterface.OnDismissListener() { public void onDismiss(DialogInterface di) { Picker.stop(); } });
        d[0].show();
    }

    private void downloadSound(final Cloud.Found f, final String query) {
        background("ध्वनि डाउनलोड हो रही है…", new Work() {
            public Object run() throws Exception {
                byte[] b = Prefs.cloud(MainActivity.this).download(f.url);
                String u = f.url.toLowerCase(Locale.US);
                String ext = u.contains(".mp3") ? ".mp3" : u.contains(".ogg") ? ".ogg" : u.contains(".wav") ? ".wav" : u.contains(".flac") ? ".flac" : ".m4a";
                Library.Item it = library.addBytes(Library.SOUND, "amb", f.title, query + "," + f.title, b, ext, f.source + " " + f.license + " " + f.creator);
                if (AudioIO.decode(MainActivity.this, it.path) == null) { library.remove(it); throw new Exception("यह ध्वनि फ़ोन में नहीं चल सकी"); }
                return it;
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("नहीं हुआ: " + e.getMessage()); return; }
                useSound((Library.Item) r);
            }
        });
    }

    // ================================================================== recording

    /** Records a voice sample or sound with the microphone and saves it to the library. */
    private void record(final String type, final String name) {
        withPermission(Manifest.permission.RECORD_AUDIO, "माइक की अनुमति दें", new Runnable() {
            public void run() { recordDialog(type, name); }
        });
    }

    private void recordDialog(final String type, final String name) {
        final File out = new File(library.dir(), "rec_" + System.currentTimeMillis() + ".wav");
        final LinearLayout body = Ui.column(this);
        body.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), Ui.dp(this, 16), Ui.dp(this, 8));
        final TextView tip = Ui.text(this, type.equals(Library.VOICE)
                ? "शांत जगह में 10–20 सेकंड साफ़ बोलें (कोई भी वाक्य)। इसी आवाज़ में " + name + " के सारे संवाद बनेंगे।"
                : "जो ध्वनि चाहिए उसे फ़ोन के पास रिकॉर्ड करें।", 14, Ui.SUB, false);
        body.addView(tip);
        final TextView time = Ui.text(this, "0:00", 28, Ui.TEXT, true);
        time.setGravity(Gravity.CENTER);
        body.addView(time);
        final ProgressBar meter = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        meter.setMax(100);
        body.addView(meter);
        final EditText nm = new EditText(this);
        nm.setHint(type.equals(Library.VOICE) ? "नाम (जैसे: मेरी आवाज़)" : "नाम/शब्द (जैसे: बारिश, rain)");
        nm.setText(name);
        body.addView(nm);
        final Button btn = Ui.button(this, "⏺  रिकॉर्डिंग शुरू", Ui.RED, null);
        body.addView(btn);
        final AlertDialog d = new AlertDialog.Builder(this).setTitle(type.equals(Library.VOICE) ? "🎙 आवाज़ रिकॉर्ड करें" : "🎙 ध्वनि रिकॉर्ड करें")
                .setView(body).setNegativeButton("रद्द", null).create();
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
                    btn.setText("⏹  रोकें और सहेजें");
                    ui.post(tick);
                } else {
                    AudioIO.Recorder r = recorder;
                    recorder = null;
                    recordingFile = null;
                    r.stop();
                    d.dismiss();
                    if (r.error != null || out.length() < 32000) { toast("रिकॉर्डिंग नहीं हुई " + (r.error == null ? "(बहुत छोटी)" : r.error)); out.delete(); return; }
                    try {
                        String n = nm.getText().toString().trim();
                        Library.Item it = library.add(type, type.equals(Library.VOICE) ? "voice" : "amb", n, n, out, ".wav", "recorded");
                        toast("✅ लाइब्रेरी में सहेजा");
                        useSound(it);
                    } catch (Exception e) {
                        toast("सहेजा नहीं जा सका: " + e.getMessage());
                    }
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
        LinearLayout body = page(S_LINES, "🎙 संवाद रिकॉर्ड करें", true);
        LinearLayout info = Ui.card(this);
        info.addView(Ui.text(this, "जिस संवाद को आप अपनी (या बच्चों की) असली आवाज़ में चाहते हैं, उसे यहाँ रिकॉर्ड करें। बाकी संवाद नमूने/फ़ोन/AI की आवाज़ में बनेंगे। होंठ आपकी रिकॉर्डिंग के साथ हिलेंगे।", 14, Ui.SUB, false));
        body.addView(info);
        Story st = loadStory();
        Film f;
        try {
            f = new Director(st, new Director.Options()).prepare();
        } catch (Throwable e) {
            body.addView(Ui.text(this, "कहानी पढ़ी नहीं जा सकी", 15, Ui.RED, true));
            return;
        }
        final File dir = new File(project.dir, "lines");
        dir.mkdirs();
        for (final Film.Line l : f.lines) {
            final String who = l.who == null ? "कथावाचक" : l.who.displayName;
            final File file = new File(dir, FilmJob.hash(who + "|" + l.text) + ".wav");
            LinearLayout c = Ui.card(this);
            c.addView(Ui.text(this, who + (l.manner.length() > 0 ? " (" + l.manner + ")" : ""), 15, Ui.TEXT, true));
            c.addView(Ui.text(this, l.shown, 14, Ui.SUB, false));
            LinearLayout r = Ui.row(this);
            final Button rec = Ui.small(this, file.equals(recordingFile) ? "⏹ रोकें" : file.exists() ? "🎙 फिर से" : "🎙 रिकॉर्ड", Ui.RED, null);
            rec.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    withPermission(Manifest.permission.RECORD_AUDIO, "माइक की अनुमति दें", new Runnable() {
                        public void run() {
                            if (recorder != null && file.equals(recordingFile)) {
                                // finish this line's recording
                                AudioIO.Recorder rr = recorder;
                                recorder = null;
                                recordingFile = null;
                                rr.stop();
                                toast(file.length() > 16000 ? "✅ सहेजा" : "बहुत छोटी रिकॉर्डिंग");
                                if (file.length() <= 16000) file.delete();
                                showLines();
                                return;
                            }
                            if (recorder != null) { toast("पहले चल रही रिकॉर्डिंग ⏹ से रोकें"); return; }
                            recordingFile = file;
                            recorder = new AudioIO.Recorder(file);
                            recorder.start();
                            rec.setText("⏹ रोकें");
                        }
                    });
                }
            });
            r.addView(rec);
            if (file.exists()) {
                r.addView(Ui.small(this, "▶", Ui.BLUE, new View.OnClickListener() {
                    public void onClick(View v) { Picker.play(MainActivity.this, file.getAbsolutePath()); }
                }));
                r.addView(Ui.small(this, "✖", Ui.SUB, new View.OnClickListener() {
                    public void onClick(View v) { file.delete(); showLines(); }
                }));
            }
            c.addView(r);
            body.addView(c);
        }
    }

    // ================================================================== picking files

    private void pick(String type, int code, boolean multiple) {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType(type);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            if (multiple) i.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
            startActivityForResult(Intent.createChooser(i, "चुनें"), code);
        } catch (Exception e) {
            toast("फ़ाइल चुनने वाला ऐप नहीं मिला");
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
                    toast("✅ साइन इन: " + name);
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
                    toast("फ़ोटो नहीं मिली");
                }
            } else if (result == RESULT_OK && data != null && data.getExtras() != null && data.getExtras().get("data") instanceof Bitmap) {
                Bitmap bm = (Bitmap) data.getExtras().get("data");
                java.io.ByteArrayOutputStream bo = new java.io.ByteArrayOutputStream();
                bm.compress(Bitmap.CompressFormat.JPEG, 95, bo);
                incomingPicture(bo.toByteArray(), "camera");
            }
            return;
        }
        if (code == REQ_SAVE_TEXT) {
            if (result == RESULT_OK && data != null && data.getData() != null && pendingText == null) {
                toast("फिर से \"डाउनलोड करें\" दबाएँ");
            }
            if (result == RESULT_OK && data != null && data.getData() != null && pendingText != null) {
                try {
                    OutputStream o = getContentResolver().openOutputStream(data.getData());
                    o.write(pendingText.getBytes("UTF-8"));
                    o.close();
                    toast("✅ सहेजा गया");
                } catch (Exception e) {
                    saveTextToDownloads(pendingTextName, pendingText);
                }
            }
            pendingText = null;
            return;
        }
        if (result != RESULT_OK || data == null) return;
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
                background("चित्र खुल रहा है…", new Work() {
                    public Object run() throws Exception {
                        InputStream in = getContentResolver().openInputStream(uri);
                        if (in == null) throw new Exception("चित्र नहीं खुला");
                        return new Object[]{Project.readAll(in), displayName(uri)};
                    }
                }, new Done() {
                    public void done(Object r, Exception e) {
                        if (e != null) { toast("चित्र नहीं खुला: " + e.getMessage()); return; }
                        incomingPicture((byte[]) ((Object[]) r)[0], (String) ((Object[]) r)[1]);
                    }
                });
            } else if (code == REQ_AUDIO) {
                final String name = displayName(uri);
                background("ध्वनि जोड़ी जा रही है…", new Work() {
                    public Object run() throws Exception {
                        byte[] b = Project.readAll(getContentResolver().openInputStream(uri));
                        String ext = name.contains(".") ? name.substring(name.lastIndexOf('.')).toLowerCase(Locale.US) : ".m4a";
                        boolean voice = target != null && (target.startsWith("voice:") || target.equals("lib:voice"));
                        String n = target != null && target.startsWith("voice:") ? target.substring(6) : name.replaceAll("\\.[A-Za-z0-9]+$", "");
                        Library.Item it = library.addBytes(voice ? Library.VOICE : Library.SOUND, voice ? "voice" : "amb", n, name, b, ext, "phone");
                        if (AudioIO.decode(MainActivity.this, it.path) == null) { library.remove(it); throw new Exception("यह फ़ाइल चल नहीं सकी"); }
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
            toast("खुल नहीं सका: " + e.getMessage());
        }
    }

    /** Many pictures at once: each is recognised by its file name (or by AI) and put where it belongs. */
    private void bulkPictures(final List<Uri> uris) {
        loadStory();
        final Story st = castStory;
        final List<String> names = new ArrayList<String>();
        for (Story.CharacterDef c : st.characters) names.add(c.displayName);
        final List<String[]> places = Bible.places(st);
        for (String[] p : places) names.add(p[0]);
        names.add("title");
        names.add("शीर्षक");
        names.add("end");
        names.add("समाप्त");
        background("चित्र पहचाने जा रहे हैं… (" + uris.size() + ")", new Work() {
            public Object run() throws Exception {
                Cloud cloud = Prefs.online(MainActivity.this) ? Prefs.cloud(MainActivity.this) : null;
                StringBuilder rep = new StringBuilder();
                int ok = 0;
                for (Uri u : uris) {
                    String fname = displayName(u);
                    byte[] data;
                    try { data = Project.readAll(getContentResolver().openInputStream(u)); } catch (Exception e) { continue; }
                    byte[] small = cloud != null && cloud.hasGemini() ? shrink(data, 512) : null;
                    String who = ScriptAI.identify(cloud, fname, small, names);
                    library.addBytes(Library.PIC, "", who == null ? fname : who, fname, data, ".jpg", "phone");
                    if (who == null) { rep.append("• ").append(fname).append(" → पहचाना नहीं (लाइब्रेरी में सहेजा)\n"); continue; }
                    String f = project.savePicture(data, "pic");
                    Story.CharacterDef c = null;
                    for (Story.CharacterDef cd : st.characters) if (cd.displayName.equals(who)) c = cd;
                    if (c != null) project.setManifest("char", keyFor(c), "char|" + keyFor(c) + "|" + f);
                    else if (who.equals("title") || who.equals("शीर्षक")) project.setManifest("title", "title", "title|" + f + "|1");
                    else if (who.equals("end") || who.equals("समाप्त")) project.setManifest("end", "end", "end|" + f + "|1");
                    else {
                        for (Story.Scene sc : st.scenes) {
                            if (Bible.similar(who, sc.setting) || com.tarun.kahani.core.Txt.norm(sc.setting).contains(com.tarun.kahani.core.Txt.norm(who))) {
                                String k = String.valueOf(sc.number);
                                project.setManifest("scene", k + "a", null);
                                project.setManifest("scene", k + "b", null);
                                project.setManifest("scene", k, "scene|" + k + "|" + f);
                            }
                        }
                    }
                    ok++;
                    rep.append("• ").append(fname).append(" → ").append(who).append(" ✔\n");
                }
                return ok + " चित्र अपनी जगह लगे।\n\n" + rep;
            }
        }, new Done() {
            public void done(Object r, Exception e) {
                if (e != null) { toast("नहीं हो सका: " + e.getMessage()); return; }
                showText("चित्र जोड़े गए", r + "\n\nपात्रों के चित्र में 👄 दबाकर मुँह-आँख ठीक करें ताकि होंठ सही हिलें।");
                showStudio();
            }
        });
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
        final TextView hint = Ui.text(this, "चित्र तैयार हो रहा है…", 17, 0xFFFFFFFF, true);
        hint.setBackgroundColor(Ui.PRIMARY);
        hint.setPadding(Ui.dp(this, 14), Ui.dp(this, 12), Ui.dp(this, 14), Ui.dp(this, 12));
        outer.addView(hint);
        final FaceTapView fv = new FaceTapView(this);
        outer.addView(fv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout r = Ui.row(this);
        r.setPadding(Ui.dp(this, 8), Ui.dp(this, 6), Ui.dp(this, 8), Ui.dp(this, 6));
        r.setBackgroundColor(Ui.BG);
        final Button again = Ui.small(this, "↺ फिर से", Ui.RED, null);
        final Button save = Ui.small(this, "✔ सहेजें", Ui.GREEN, null);
        final Button skip = Ui.small(this, "अपने आप", Ui.SUB, null);
        r.addView(again);
        r.addView(save);
        r.addView(skip);
        outer.addView(r);
        setScreen(S_FACE, outer);
        final String[] steps = {"1/3: चित्र में मुँह के बीच टैप करें", "2/3: बाईं आँख (आपकी तरफ़ से) पर टैप करें", "3/3: दाईं आँख पर टैप करें", "✔ हो गया! \"सहेजें\" दबाएँ"};
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
                    ui.post(new Runnable() { public void run() { toast("चित्र नहीं खुला"); showStudio(); } });
                    return;
                }
                ui.post(new Runnable() {
                    public void run() {
                        if (screen != S_FACE) return;
                        if (sp == null) { toast("चित्र नहीं खुला"); showStudio(); return; }
                        float[] init = null;
                        if (f.length >= 11) {
                            try {
                                init = new float[]{Float.parseFloat(f[3]), Float.parseFloat(f[4]), Float.parseFloat(f[6]), Float.parseFloat(f[7]), Float.parseFloat(f[8]), Float.parseFloat(f[9])};
                            } catch (NumberFormatException ignored) {}
                        } else if (sp.faceKnown) {
                            init = new float[]{sp.mouthX, sp.mouthY, sp.eyeLX, sp.eyeLY, sp.eyeRX, sp.eyeRY};
                        }
                        fv.set((Bitmap) sp.img, init);
                        hint.setText(fv.step >= 3 ? "निशान ठीक हैं? नहीं तो \"फिर से\" दबाकर मुँह और आँखों पर टैप करें" : steps[0]);
                    }
                });
            }
        }).start();
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (fv.step < 3) { toast("पहले मुँह और दोनों आँखों पर टैप करें"); return; }
                float[] p = fv.pts;
                float eyeDist = Math.abs(p[4] - p[2]);
                float mouthHW = eyeDist * 0.42f, eyeR = eyeDist * 0.24f;
                String nl = String.format(Locale.US, "char|%s|%s|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%.4f|%s",
                        f[1], f[2], p[0], p[1], mouthHW, p[2], p[3], p[4], p[5], eyeR, f.length >= 12 ? f[11] : "0");
                project.setManifest("char", f[1], nl);
                toast("सहेज लिया — अब होंठ आवाज़ के साथ हिलेंगे");
                showStudio();
            }
        });
    }

    // ================================================================== making the film

    private void makeFilm() {
        saveScript();
        if (FilmJob.scriptOf(project).trim().length() < 10) { toast("पहले कहानी लिखें या चिपकाएँ"); return; }
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission("android.permission.POST_NOTIFICATIONS") != PackageManager.PERMISSION_GRANTED && !askedPerms) {
            askPermissions();
        }
        FilmJob j = FilmJob.start(this, project);
        if (j == null) {
            FilmJob cur = FilmJob.current;
            final Project other = cur != null ? cur.project : null;
            AlertDialog.Builder b = new AlertDialog.Builder(this).setTitle("एक फ़िल्म पहले से बन रही है")
                    .setMessage(cur != null && cur.cancelled ? "पिछली फ़िल्म रुक रही है — कुछ सेकंड बाद फिर दबाएँ।"
                            : "\"" + (other == null ? "" : other.name()) + "\" बन रही है। उसके पूरा होने के बाद यह फ़िल्म बनाएँ, या उसे रोक दें।")
                    .setPositiveButton("ठीक है", null);
            if (other != null && !cur.cancelled) b.setNeutralButton("उसकी प्रगति देखें", new DialogInterface.OnClickListener() {
                public void onClick(DialogInterface d, int w) { project = other; showProgress(); }
            });
            b.show();
            return;
        }
        showProgress();
    }

    private void showProgress() {
        LinearLayout body = page(S_PROGRESS, "⏳ फ़िल्म बन रही है", true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout c = Ui.card(this);
        final TextView stage = Ui.text(this, "", 17, Ui.TEXT, true);
        final TextView eta = Ui.text(this, "", 15, Ui.BLUE, true);
        final ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        final ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setBackgroundColor(0xFF000000);
        final TextView note = Ui.text(this, "आप फ़ोन लॉक कर सकते हैं या दूसरा ऐप खोल सकते हैं — फ़िल्म बनती रहेगी (सूचना में प्रगति दिखेगी)।", 14, Ui.SUB, false);
        final TextView warn = Ui.text(this, "", 14, Ui.RED, false);
        c.addView(stage);
        c.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 22)));
        c.addView(eta);
        c.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 200)));
        c.addView(note);
        c.addView(warn);
        final Button stop = Ui.button(this, "■  रोकें", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) {
                new AlertDialog.Builder(MainActivity.this).setTitle("रोकें?").setMessage("बनती हुई फ़िल्म रुक जाएगी।")
                        .setPositiveButton("हाँ, रोकें", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface d, int w) { FilmJob j = FilmJob.current; if (j != null) j.cancel(); }
                        }).setNegativeButton("नहीं", null).show();
            }
        });
        c.addView(stop);
        body.addView(c);
        ui.post(new Runnable() {
            public void run() {
                if (screen != S_PROGRESS) return;
                FilmJob j = FilmJob.current;
                if (j == null) { showStory(); return; }
                stage.setText(j.stage);
                eta.setText(j.eta());
                bar.setProgress((int) (j.progress * 1000));
                if (j.preview != null) preview.setImageBitmap(j.preview);
                if (j.warning.length() > 0) warn.setText("⚠ " + j.warning);
                if (j.done) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    toast("🎉 फ़िल्म तैयार है!");
                    project = j.project;
                    showPlayer();
                    return;
                }
                if (j.failed || j.cancelled) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    stage.setText(j.failed ? "❌ फ़िल्म नहीं बन सकी" : "रोक दिया गया");
                    eta.setText("");
                    if (j.failed) warn.setText(j.error);
                    stop.setText("← वापस");
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
        panel.addView(Ui.text(this, "✍ बदलाव लिखें (जैसे: चमक बढ़ाओ • संगीत धीमा करो • वृंदा की आवाज़ तेज़ करो • subtitles लगाओ • file size कम करो)", 13, Ui.SUB, false));
        LinearLayout cmdRow = Ui.row(this);
        final EditText cmd = new EditText(this);
        cmd.setHint("आपका आदेश…");
        cmd.setTextSize(15);
        cmd.setSingleLine(false);
        cmd.setMaxLines(3);
        cmdRow.addView(cmd, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        final TextView status = Ui.text(this, pendingEditsNote(), 13, Ui.GREEN, false);
        cmdRow.addView(Ui.small(this, "✔ लागू", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { applyCommand(cmd.getText().toString(), status, cmd); }
        }));
        panel.addView(cmdRow);
        panel.addView(status);
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "🔁 फिर बनाएँ", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { vv.stopPlayback(); makeFilm(); }
        }));
        r.addView(Ui.small(this, "💾 डाउनलोड", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { saveToGallery(false); }
        }));
        r.addView(Ui.small(this, "📤 साझा", Ui.BLUE, new View.OnClickListener() {
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
                toast("फ़िल्म चल नहीं सकी — डाउनलोड करके गैलरी में देखें");
                return true;
            }
        });
        vv.start();
    }

    private String pendingEditsNote() {
        long made = 0;
        try { made = Long.parseLong(project.setting("madeAt", "0")); } catch (NumberFormatException ignored) {}
        File e = project.file("edits.json");
        if (e.exists() && e.lastModified() > made + 1000) return "बदलाव सहेजे गए — देखने के लिए \"🔁 फिर बनाएँ\" दबाएँ (आवाज़ें दोबारा नहीं बनेंगी, जल्दी होगा)।";
        return "";
    }

    private void applyCommand(final String text, final TextView status, final EditText box) {
        if (text.trim().length() == 0) return;
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
            if (!res.unknown.isEmpty()) done.append("? समझ नहीं आया: ").append(res.unknown).append('\n');
            status.setText(done + (done.length() > 0 ? "देखने के लिए \"🔁 फिर बनाएँ\" दबाएँ।" : ""));
            box.setText("");
            return;
        }
        final String rest = android.text.TextUtils.join(", ", res.unknown);
        background("AI आदेश समझ रहा है…", new Work() {
            public Object run() throws Exception { return ScriptAI.commands(Prefs.cloud(MainActivity.this), rest, names); }
        }, new Done() {
            @SuppressWarnings("unchecked")
            public void done(Object r, Exception e) {
                Edits ed2 = edits();
                if (e == null) for (Map<String, Object> c : (List<Map<String, Object>>) r) {
                    String d = ed2.apply(c);
                    if (d != null) done.append("✔ ").append(d).append('\n');
                }
                else done.append("? समझ नहीं आया: ").append(rest).append('\n');
                saveEdits(ed2);
                status.setText(done + "देखने के लिए \"🔁 फिर बनाएँ\" दबाएँ।");
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
        background(thenShare ? "साझा करने की तैयारी…" : "गैलरी में सहेजा जा रहा है…", new Work() {
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
                if (e != null) { toast("सहेजा नहीं जा सका: " + e.getMessage()); return; }
                if (!thenShare) { toast("✅ गैलरी में सहेजा गया (Movies/KahaniFilm)"); return; }
                try {
                    Intent s = new Intent(Intent.ACTION_SEND);
                    s.setType("video/mp4");
                    s.putExtra(Intent.EXTRA_STREAM, (Uri) r);
                    s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                    startActivity(Intent.createChooser(s, "फ़िल्म साझा करें (YouTube, Instagram, Facebook…)"));
                } catch (Exception ex) {
                    toast("साझा नहीं हो सका");
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
            if (uri == null) throw new Exception("गैलरी उपलब्ध नहीं");
            OutputStream o = getContentResolver().openOutputStream(uri);
            Project.copy(new FileInputStream(src), o);
            v.clear();
            v.put(MediaStore.Video.Media.IS_PENDING, 0);
            getContentResolver().update(uri, v, null, null);
            return uri;
        }
        File dir = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MOVIES), "KahaniFilm");
        if (!dir.exists() && !dir.mkdirs()) throw new Exception("फ़ोल्डर नहीं बन सका");
        File dst = new File(dir, name);
        Project.copy(new FileInputStream(src), new FileOutputStream(dst));
        final Uri[] out = {null};
        final Object lock = new Object();
        MediaScannerConnection.scanFile(this, new String[]{dst.getAbsolutePath()}, new String[]{"video/mp4"},
                new MediaScannerConnection.OnScanCompletedListener() {
                    public void onScanCompleted(String path, Uri uri) { synchronized (lock) { out[0] = uri; lock.notifyAll(); } }
                });
        synchronized (lock) { if (out[0] == null) lock.wait(5000); }
        if (out[0] == null) throw new Exception("गैलरी ने फ़ाइल नहीं पहचानी");
        return out[0];
    }

    // ================================================================== library

    private void showLibrary() {
        target = "lib:" + libTab;
        LinearLayout body = page(S_LIBRARY, "📚 मेरी लाइब्रेरी", true);
        LinearLayout tabs = Ui.row(this);
        tabs.setPadding(Ui.dp(this, 8), 0, Ui.dp(this, 8), 0);
        final String[][] t = {{Library.PIC, "🖼 चित्र"}, {Library.VOICE, "🎙 आवाज़ें"}, {Library.SOUND, "🔊 ध्वनियाँ"}};
        for (final String[] x : t) {
            tabs.addView(Ui.small(this, x[1], x[0].equals(libTab) ? Ui.PRIMARY : Ui.SUB, new View.OnClickListener() {
                public void onClick(View v) { libTab = x[0]; showLibrary(); }
            }));
        }
        body.addView(tabs);
        LinearLayout add = Ui.card(this);
        if (libTab.equals(Library.PIC)) {
            add.addView(Ui.text(this, "आपके चित्र हर फ़िल्म में इस्तेमाल हो सकते हैं। असली फ़ोटो को कार्टून-अवतार में बदला जा सकता है।", 13, Ui.SUB, false));
            LinearLayout r = Ui.row(this);
            r.addView(Ui.small(this, "📂 फ़ोन से", Ui.PRIMARY, new View.OnClickListener() { public void onClick(View v) { target = "lib:pic"; pick("image/*", REQ_IMAGE, false); } }));
            r.addView(Ui.small(this, "📷 कैमरा", Ui.PRIMARY, new View.OnClickListener() { public void onClick(View v) { target = "lib:pic"; camera(); } }));
            r.addView(Ui.small(this, "🌐 खोजें", Ui.BLUE, new View.OnClickListener() { public void onClick(View v) { target = "lib:pic"; searchPictures(""); } }));
            add.addView(r);
        } else if (libTab.equals(Library.VOICE)) {
            add.addView(Ui.text(this, "आवाज़ के नमूने (10–20 सेकंड)। स्टूडियो में किसी पात्र को देने पर उसके सारे संवाद इसी आवाज़ में बनेंगे।", 13, Ui.SUB, false));
            LinearLayout r = Ui.row(this);
            r.addView(Ui.small(this, "🎙 रिकॉर्ड", Ui.RED, new View.OnClickListener() { public void onClick(View v) { target = "lib:voice"; record(Library.VOICE, ""); } }));
            r.addView(Ui.small(this, "📂 फ़ाइल से", Ui.PRIMARY, new View.OnClickListener() { public void onClick(View v) { target = "lib:voice"; pick("audio/*", REQ_AUDIO, false); } }));
            add.addView(r);
        } else {
            add.addView(Ui.text(this, "प्राकृतिक ध्वनियाँ, संगीत और प्रभाव। नाम/शब्द से स्टूडियो सही जगह इस्तेमाल करता है।", 13, Ui.SUB, false));
            LinearLayout r = Ui.row(this);
            r.addView(Ui.small(this, "🎙 रिकॉर्ड", Ui.RED, new View.OnClickListener() { public void onClick(View v) { target = "lib:sound"; record(Library.SOUND, ""); } }));
            r.addView(Ui.small(this, "📂 फ़ाइल", Ui.PRIMARY, new View.OnClickListener() { public void onClick(View v) { target = "lib:sound"; pick("audio/*", REQ_AUDIO, false); } }));
            r.addView(Ui.small(this, "🌐 खोजें", Ui.BLUE, new View.OnClickListener() { public void onClick(View v) { target = "lib:sound"; searchSounds(""); } }));
            add.addView(r);
        }
        body.addView(add);
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
                TextView tv = Ui.text(this, it.label() + (it.builtIn ? " (ऐप)" : ""), 12, Ui.TEXT, false);
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
                r.addView(Ui.text(this, it.label() + (it.builtIn ? "  (ऐप)" : ""), 14, Ui.TEXT, false), new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
                r.addView(Ui.small(this, "▶", Ui.BLUE, new View.OnClickListener() { public void onClick(View v) { Picker.play(MainActivity.this, it.path); } }));
                if (!it.builtIn) r.addView(Ui.small(this, "🗑", Ui.RED, new View.OnClickListener() { public void onClick(View v) { confirmRemove(it); } }));
                body.addView(r);
            }
        }
        if (libTab.equals(Library.PIC) && !items.isEmpty())
            body.addView(Ui.text(this, "  हटाने के लिए अपने चित्र को देर तक दबाएँ।", 12, Ui.SUB, false));
    }

    private void confirmRemove(final Library.Item it) {
        new AlertDialog.Builder(this).setTitle("हटाएँ?").setMessage(it.label())
                .setPositiveButton("हाँ", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { Picker.stop(); library.remove(it); showLibrary(); }
                }).setNegativeButton("नहीं", null).show();
    }

    // ================================================================== settings

    private void showSettings() {
        LinearLayout body = page(S_SETTINGS, "⚙ सेटिंग", true);
        LinearLayout acc = Ui.card(this);
        String a = Prefs.account(this);
        acc.addView(Ui.title(this, "खाता"));
        acc.addView(Ui.text(this, a.length() > 0 ? "👤 " + a : "साइन इन नहीं", 15, Ui.TEXT, false));
        acc.addView(Ui.small(this, a.length() > 0 ? "लॉग आउट" : "Gmail से साइन इन", a.length() > 0 ? Ui.RED : Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { if (Prefs.account(MainActivity.this).length() > 0) logout(); else login(); }
        }));
        body.addView(acc);

        LinearLayout ai = Ui.card(this);
        ai.addView(Ui.title(this, "मुफ़्त AI (वैकल्पिक)"));
        ai.addView(Ui.text(this, "बिना कुंजी के भी ऐप काम करता है (मुफ़्त AI चित्र और कहानी पढ़ना)। बेहतर कहानी-पढ़ाई, चित्र-पहचान और भाव वाली AI आवाज़ों के लिए Google Gemini की मुफ़्त कुंजी डालें:\n1. aistudio.google.com/apikey खोलें (उसी Gmail से)\n2. \"Create API key\" दबाएँ, कुंजी कॉपी करें\n3. यहाँ चिपकाएँ। कुंजी सिर्फ़ इस फ़ोन में रहती है।", 13, Ui.SUB, false));
        final EditText key = new EditText(this);
        key.setHint("Gemini API key (AIza…)");
        key.setSingleLine(true);
        key.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        key.setText(Prefs.geminiKey(this));
        ai.addView(key);
        LinearLayout kr = Ui.row(this);
        kr.addView(Ui.small(this, "💾 सहेजें", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { Prefs.put(MainActivity.this, "geminiKey", key.getText().toString().trim()); toast("सहेजा"); }
        }));
        kr.addView(Ui.small(this, "🧪 जाँचें", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) {
                Prefs.put(MainActivity.this, "geminiKey", key.getText().toString().trim());
                background("जाँच हो रही है…", new Work() {
                    public Object run() throws Exception { return Prefs.cloud(MainActivity.this).gemini(null, "Reply with the single word OK", false, null, null); }
                }, new Done() {
                    public void done(Object r, Exception e) { toast(e == null ? "✅ कुंजी काम कर रही है" : "❌ " + e.getMessage()); }
                });
            }
        }));
        ai.addView(kr);
        CheckBox online = new CheckBox(this);
        online.setText("ऑनलाइन सुविधाएँ (AI, मुफ़्त चित्र/ध्वनि खोज)");
        online.setChecked(Prefs.online(this));
        online.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) { Prefs.put(MainActivity.this, "online", on ? "1" : "0"); }
        });
        ai.addView(online);
        CheckBox nv = new CheckBox(this);
        nv.setText("प्राकृतिक आवाज़ें (Microsoft की मुफ़्त न्यूरल आवाज़ें, इंटरनेट चाहिए, कुंजी नहीं) — सलाह: चालू रखें");
        nv.setChecked(Prefs.naturalVoices(this));
        nv.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) { Prefs.put(MainActivity.this, "naturalVoices", on ? "1" : "0"); }
        });
        ai.addView(nv);
        CheckBox av = new CheckBox(this);
        av.setText("AI आवाज़ें (भाव के साथ; कुंजी ज़रूरी)। मुफ़्त सीमा बहुत कम है — छोटी फ़िल्मों के लिए। सीमा पूरी होने पर बाकी संवाद फ़ोन की आवाज़ में।");
        av.setChecked(Prefs.aiVoices(this));
        av.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) { Prefs.put(MainActivity.this, "aiVoices", on ? "1" : "0"); }
        });
        ai.addView(av);
        body.addView(ai);

        LinearLayout ph = Ui.card(this);
        ph.addView(Ui.title(this, "फ़ोन"));
        ph.addView(Ui.small(this, "🔊 फ़ोन की आवाज़ें (Text-to-Speech)", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { offerTtsInstall(); }
        }));
        ph.addView(Ui.small(this, "🔐 अनुमतियाँ दें", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) {
                String[] m = neededPermissions();
                if (m.length == 0) toast("✅ सारी अनुमतियाँ मिल चुकी हैं");
                else requestPermissions(m, REQ_PERMS);
            }
        }));
        body.addView(ph);
    }
}
