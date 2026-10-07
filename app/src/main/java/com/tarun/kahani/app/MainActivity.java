package com.tarun.kahani.app;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
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
import com.tarun.kahani.core.Director;
import com.tarun.kahani.core.Film;
import com.tarun.kahani.core.Look;
import com.tarun.kahani.core.Pose;
import com.tarun.kahani.core.Puppet;
import com.tarun.kahani.core.ScriptParser;
import com.tarun.kahani.core.Sets;
import com.tarun.kahani.core.Story;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Locale;

/** The whole app: dashboard, story editor, characters & scenes, progress, player. */
public class MainActivity extends Activity {

    private static final int PICK_TITLE = 1, PICK_END = 2, PICK_SCRIPT = 3, PICK_CHAR = 100, PICK_SCENE = 400, PERM = 9;

    private final Handler ui = new Handler(Looper.getMainLooper());
    private FrameLayout root;
    private Project project;
    private int screen;               // 0 home, 1 editor, 2 cast, 3 face, 4 progress, 5 player
    private EditText scriptBox;
    private Story castStory;
    private String pendingCharName, pendingScene;
    private Voices previewVoices;

    // ================================================================== lifecycle

    @Override
    protected void onCreate(Bundle b) {
        super.onCreate(b);
        Thread.setDefaultUncaughtExceptionHandler(new CrashLog(this, Thread.getDefaultUncaughtExceptionHandler()));
        root = new FrameLayout(this);
        root.setBackgroundColor(Ui.BG);
        setContentView(root);
        getWindow().setStatusBarColor(Ui.PRIMARY_DARK);
        if (b != null) {
            String pd = b.getString("project");
            if (pd != null && new File(pd).isDirectory()) project = new Project(new File(pd));
            pendingCharName = b.getString("pchar");
            pendingScene = b.getString("pscene");
        }
        String crash = CrashLog.last(this);
        FilmJob job = FilmJob.current;
        if (job != null && !job.done && !job.failed && !job.cancelled) { project = job.project; showProgress(); }
        else if (project != null) showEditor();
        else showHome();
        if (crash != null) {
            new AlertDialog.Builder(this).setTitle("पिछली बार ऐप बंद हो गया था")
                    .setMessage("माफ़ कीजिए। आपकी कहानी सुरक्षित है। अगर बार-बार हो तो गुणवत्ता 480p करके देखें.\n\n" + crash)
                    .setPositiveButton("ठीक है", null).show();
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle out) {
        super.onSaveInstanceState(out);
        if (project != null) out.putString("project", project.dir.getAbsolutePath());
        if (pendingCharName != null) out.putString("pchar", pendingCharName);
        if (pendingScene != null) out.putString("pscene", pendingScene);
    }

    @Override
    protected void onPause() {
        super.onPause();
        saveScript();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (previewVoices != null) previewVoices.shutdown();
        ui.removeCallbacksAndMessages(null);
    }

    @Override
    public void onBackPressed() {
        switch (screen) {
            case 1: saveScript(); showHome(); break;
            case 2: case 4: case 5: showEditor(); break;
            case 3: showCast(); break;
            default: super.onBackPressed();
        }
    }

    private void setScreen(int s, View content) {
        screen = s;
        ui.removeCallbacksAndMessages(null);
        if (s != 4) getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        root.removeAllViews();
        root.addView(content, new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
    }

    private LinearLayout page(String title, boolean back) {
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
        setScreen(screen, outer);
        return body;
    }

    private void toast(String s) { Toast.makeText(this, s, Toast.LENGTH_LONG).show(); }

    // ================================================================== home (dashboard)

    private void showHome() {
        screen = 0;
        project = null;
        LinearLayout body = page("🎬 कहानी फ़िल्म", false);
        LinearLayout hero = Ui.card(this);
        hero.addView(Ui.text(this, "अपनी कहानी से कार्टून फ़िल्म बनाइए", 18, Ui.TEXT, true));
        hero.addView(Ui.text(this, "कहानी लिखें या चिपकाएँ → (चाहें तो) चित्र जोड़ें → ‘फ़िल्म बनाएँ’ दबाएँ। आवाज़ें, संगीत, दृश्य और कैमरा ऐप खुद तैयार करता है।", 15, Ui.SUB, false));
        hero.addView(Ui.button(this, "➕  नई फ़िल्म बनाएँ", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { project = Project.create(MainActivity.this); showEditor(); }
        }));
        hero.addView(Ui.button(this, "📖  उदाहरण: रत्नगढ़ की दो राजकुमारियाँ", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { openSample(); }
        }));
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
        if (!ps.isEmpty()) {
            TextView h = Ui.title(this, "मेरी फ़िल्में");
            h.setPadding(Ui.dp(this, 16), Ui.dp(this, 10), 0, 0);
            body.addView(h);
        }
        for (final Project p : ps) {
            LinearLayout c = Ui.card(this);
            final boolean made = p.film().exists();
            c.addView(Ui.text(this, p.name(), 17, Ui.TEXT, true));
            String st = made ? "✅ फ़िल्म तैयार (" + FilmJob.fmt(Long.parseLong(p.setting("filmSeconds", "0"))) + ")" : "✏️ अधूरी";
            c.addView(Ui.text(this, st, 14, made ? Ui.GREEN : Ui.SUB, false));
            LinearLayout r = Ui.row(this);
            r.addView(Ui.small(this, "खोलें", Ui.PRIMARY, new View.OnClickListener() {
                public void onClick(View v) { project = p; showEditor(); }
            }));
            if (made) r.addView(Ui.small(this, "▶ देखें", Ui.GREEN, new View.OnClickListener() {
                public void onClick(View v) { project = p; showPlayer(); }
            }));
            r.addView(Ui.small(this, "🗑 हटाएँ", Ui.RED, new View.OnClickListener() {
                public void onClick(View v) { confirmDelete(p); }
            }));
            c.addView(r);
            body.addView(c);
        }
    }

    private void openSample() {
        for (Project p : Project.all(this)) {
            if (p.has("char_vrinda.jpg") && p.read("script.txt").contains("रत्नगढ़")) { project = p; showEditor(); return; }
        }
        final ProgressBar pb = new ProgressBar(this);
        final AlertDialog d = new AlertDialog.Builder(this).setTitle("उदाहरण तैयार हो रहा है…").setView(pb).setCancelable(false).show();
        new Thread(new Runnable() {
            public void run() {
                try {
                    final Project p = Project.createSample(MainActivity.this);
                    ui.post(new Runnable() { public void run() { d.dismiss(); project = p; showEditor(); } });
                } catch (final Exception e) {
                    ui.post(new Runnable() { public void run() { d.dismiss(); toast("उदाहरण नहीं खुला: " + e.getMessage()); } });
                }
            }
        }).start();
    }

    private void confirmDelete(final Project p) {
        new AlertDialog.Builder(this).setTitle("हटाएँ?").setMessage("\"" + p.name() + "\" और इसकी फ़िल्म हमेशा के लिए हट जाएगी।")
                .setPositiveButton("हाँ, हटाएँ", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) { p.delete(); showHome(); }
                }).setNegativeButton("नहीं", null).show();
    }

    // ================================================================== editor

    private void saveScript() {
        if (project != null && scriptBox != null && screen == 1) project.write("script.txt", scriptBox.getText().toString());
    }

    private void showEditor() {
        if (project == null) { showHome(); return; }
        screen = 1;
        LinearLayout body = page(project.name(), true);

        // ---- 1. story
        LinearLayout c1 = Ui.card(this);
        c1.addView(Ui.title(this, "1. कहानी (पटकथा)"));
        c1.addView(Ui.text(this, "पात्रों और स्थानों का विवरण सिर्फ़ चित्र बनाने के लिए है — पढ़ा नहीं जाएगा। दृश्य ऐसे लिखें:\nदृश्य 1: शीर्षक\n(स्थान: …)\nनाम (भाव): \"संवाद\"", 13, Ui.SUB, false));
        scriptBox = new EditText(this);
        scriptBox.setText(project.read("script.txt"));
        scriptBox.setHint("यहाँ अपनी कहानी चिपकाएँ…");
        scriptBox.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_MULTI_LINE | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        scriptBox.setGravity(Gravity.TOP);
        scriptBox.setMinLines(8);
        scriptBox.setMaxLines(14);
        scriptBox.setVerticalScrollBarEnabled(true);
        scriptBox.setTextSize(15);
        scriptBox.setBackground(Ui.round(0xFFFFFDF7, Ui.dp(this, 10), 0x33000000, Ui.dp(this, 1)));
        scriptBox.setPadding(Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10), Ui.dp(this, 10));
        c1.addView(scriptBox, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 260)));
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
        r1.addView(Ui.small(this, "📂 फ़ाइल से", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { pick("text/*", PICK_SCRIPT); }
        }));
        r1.addView(Ui.small(this, "🧹 साफ़", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) { scriptBox.setText(""); }
        }));
        c1.addView(r1);
        body.addView(c1);

        // ---- 2/3. title & end pictures
        body.addView(pictureCard("2. शुरुआत का चित्र (शीर्षक पृष्ठ)", "title", PICK_TITLE, "titleText", "चित्र पर शीर्षक लिखें"));
        body.addView(pictureCard("3. अंत का चित्र (समाप्त)", "end", PICK_END, "endText", "चित्र पर \"समाप्त\" लिखें"));

        // ---- 4. characters & scenes
        LinearLayout c4 = Ui.card(this);
        c4.addView(Ui.title(this, "4. पात्र, आवाज़ें और दृश्य"));
        c4.addView(Ui.text(this, "निर्देशक कहानी जाँचकर बताएगा कौन-कौन पात्र हैं, कौन सा दृश्य कहाँ है। यहाँ आप पात्रों के चित्र, आवाज़ और दृश्यों की पृष्ठभूमि बदल सकते हैं।", 13, Ui.SUB, false));
        c4.addView(Ui.button(this, "🎭  निर्देशक से जाँच कराएँ", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { saveScript(); showCast(); }
        }));
        body.addView(c4);

        // ---- 5. settings
        LinearLayout c5 = Ui.card(this);
        c5.addView(Ui.title(this, "5. सेटिंग"));
        RadioGroup rg = new RadioGroup(this);
        rg.setOrientation(RadioGroup.HORIZONTAL);
        final String[] q = {"480", "720", "1080"};
        String cur = project.setting("quality", "720");
        for (int i = 0; i < q.length; i++) {
            RadioButton rb = new RadioButton(this);
            rb.setText(q[i] + "p");
            rb.setTextSize(16);
            rb.setId(1000 + i);
            rg.addView(rb);
            if (q[i].equals(cur)) rb.setChecked(true);
        }
        rg.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            public void onCheckedChanged(RadioGroup g, int id) { project.setSetting("quality", q[id - 1000]); }
        });
        c5.addView(Ui.text(this, "गुणवत्ता (720p तेज़ और सुंदर):", 14, Ui.SUB, false));
        c5.addView(rg);
        c5.addView(check("उपशीर्षक (संवाद नीचे लिखे दिखें)", "subtitles", true));
        c5.addView(check("शीर्षक पृष्ठ पर नाम बोलें", "narrateTitle", true));
        c5.addView(check("कथावाचक: दृश्य के निर्देश भी पढ़े जाएँ", "narrator", false));
        body.addView(c5);

        // ---- 6. make
        LinearLayout c6 = Ui.card(this);
        c6.addView(Ui.button(this, "🎬  फ़िल्म बनाएँ", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { makeFilm(); }
        }));
        if (project.film().exists()) {
            c6.addView(Ui.button(this, "▶  बनी हुई फ़िल्म देखें", Ui.PRIMARY, new View.OnClickListener() {
                public void onClick(View v) { showPlayer(); }
            }));
            LinearLayout r = Ui.row(this);
            r.addView(Ui.small(this, "💾 गैलरी में", Ui.PRIMARY, new View.OnClickListener() {
                public void onClick(View v) { saveToGallery(false); }
            }));
            r.addView(Ui.small(this, "📤 साझा करें", Ui.PRIMARY, new View.OnClickListener() {
                public void onClick(View v) { saveToGallery(true); }
            }));
            c6.addView(r);
        }
        body.addView(c6);
    }

    private CheckBox check(String label, final String key, boolean def) {
        CheckBox cb = new CheckBox(this);
        cb.setText(label);
        cb.setTextSize(15);
        cb.setChecked("1".equals(project.setting(key, def ? "1" : "0")));
        cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            public void onCheckedChanged(CompoundButton b, boolean on) { project.setSetting(key, on ? "1" : "0"); }
        });
        return cb;
    }

    private LinearLayout pictureCard(String title, final String kind, final int code, final String textKey, String textLabel) {
        LinearLayout c = Ui.card(this);
        c.addView(Ui.title(this, title));
        String line = project.manifestLine(kind, kind);
        String file = line != null && line.split("\\|").length > 1 ? line.split("\\|")[1] : null;
        if (file != null && project.has(file)) {
            ImageView iv = new ImageView(this);
            iv.setAdjustViewBounds(true);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(thumb(project.file(file), 600));
            c.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 170)));
            final boolean txt = !line.endsWith("|0");
            CheckBox cb = new CheckBox(this);
            cb.setText(textLabel);
            cb.setChecked(txt);
            final String f = file;
            cb.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                public void onCheckedChanged(CompoundButton b, boolean on) { project.setManifest(kind, kind, kind + "|" + f + "|" + (on ? "1" : "0")); }
            });
            c.addView(cb);
        } else {
            c.addView(Ui.text(this, "कोई चित्र नहीं — ऐप खुद सुंदर " + (kind.equals("title") ? "शीर्षक पृष्ठ" : "\"समाप्त\" पृष्ठ") + " बनाएगा (संगीत के साथ)।", 14, Ui.SUB, false));
        }
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "🖼 चित्र चुनें", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { pick("image/*", code); }
        }));
        if (file != null) r.addView(Ui.small(this, "✖ हटाएँ", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) { project.setManifest(kind, kind, null); showEditor(); }
        }));
        c.addView(r);
        return c;
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

    // ================================================================== picking files

    private void pick(String type, int code) {
        try {
            Intent i = new Intent(Intent.ACTION_GET_CONTENT);
            i.setType(type);
            i.addCategory(Intent.CATEGORY_OPENABLE);
            startActivityForResult(Intent.createChooser(i, "चुनें"), code);
        } catch (Exception e) {
            toast("फ़ाइल चुनने वाला ऐप नहीं मिला");
        }
    }

    @Override
    protected void onActivityResult(int code, int result, Intent data) {
        super.onActivityResult(code, result, data);
        if (result != RESULT_OK || data == null || data.getData() == null || project == null) return;
        final Uri uri = data.getData();
        try {
            if (code == PICK_SCRIPT) {
                InputStream in = getContentResolver().openInputStream(uri);
                String t = new String(Project.readAll(in), "UTF-8");
                project.write("script.txt", t);
                showEditor();
            } else if (code == PICK_TITLE || code == PICK_END) {
                String kind = code == PICK_TITLE ? "title" : "end";
                String f = project.savePicture(this, uri, kind);
                project.setManifest(kind, kind, kind + "|" + f + "|0");
                showEditor();
            } else if (code == PICK_CHAR && pendingCharName != null) {
                String f = project.savePicture(this, uri, "char");
                project.setManifest("char", pendingCharName, "char|" + pendingCharName + "|" + f);
                final String name = pendingCharName;
                pendingCharName = null;
                showFace(name);
            } else if (code == PICK_SCENE && pendingScene != null) {
                String f = project.savePicture(this, uri, "scene");
                project.setManifest("scene", pendingScene, "scene|" + pendingScene + "|" + f);
                pendingScene = null;
                showCast();
            }
        } catch (Throwable e) {
            toast("खुल नहीं सका: " + e.getMessage());
        }
    }

    // ================================================================== director's check: characters & scenes

    private void showCast() {
        if (project == null) { showHome(); return; }
        screen = 2;
        LinearLayout body = page("🎭 निर्देशक की जाँच", true);
        Story st;
        try {
            st = ScriptParser.parse(project.read("script.txt"));
        } catch (Throwable e) {
            body.addView(Ui.text(this, "कहानी पढ़ी नहीं जा सकी: " + e.getMessage(), 16, Ui.RED, true));
            return;
        }
        castStory = st;
        LinearLayout sum = Ui.card(this);
        sum.addView(Ui.title(this, "📜 " + st.title));
        if (st.subtitle.length() > 0) sum.addView(Ui.text(this, st.subtitle, 14, Ui.SUB, false));
        sum.addView(Ui.text(this, "भाषा: " + (st.hindi ? "हिंदी (अंत में \"समाप्त\")" : "English (ends with \"The End\")")
                + "\nपात्र: " + st.characters.size() + "   •   दृश्य: " + st.scenes.size() + "   •   संवाद: " + st.dialogueCount()
                + "\nविवरण वाले हिस्से पढ़े नहीं जाएँगे — फ़िल्म शीर्षक पृष्ठ से शुरू होगी, फिर \"" + (st.scenes.isEmpty() ? "दृश्य 1" : st.scenes.get(0).heading) + "\"।", 15, Ui.TEXT, false));
        for (String w : st.warnings) sum.addView(Ui.text(this, "⚠ " + w, 14, Ui.RED, false));
        if (st.dialogueCount() == 0) sum.addView(Ui.text(this, "⚠ कोई संवाद नहीं मिला। संवाद ऐसे लिखें — नाम: \"संवाद\"", 14, Ui.RED, true));
        body.addView(sum);

        // characters
        TextView h = Ui.title(this, "पात्र (चित्र और आवाज़)");
        h.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), 0, 0);
        body.addView(h);
        for (int i = 0; i < st.characters.size(); i++) body.addView(characterCard(st, st.characters.get(i)));

        // scenes
        TextView h2 = Ui.title(this, "दृश्य (पृष्ठभूमि)");
        h2.setPadding(Ui.dp(this, 16), Ui.dp(this, 8), 0, 0);
        body.addView(h2);
        for (Story.Scene sc : st.scenes) body.addView(sceneCard(sc));

        // director plan preview
        LinearLayout plan = Ui.card(this);
        plan.addView(Ui.title(this, "🎬 निर्देशक की योजना"));
        try {
            Director d = new Director(st, new Director.Options());
            d.prepare();
            Film f = d.direct(new Art());
            StringBuilder sb = new StringBuilder();
            for (String n : f.notes) sb.append("• ").append(n).append('\n');
            sb.append("\nअनुमानित लंबाई: ").append(FilmJob.fmt((long) f.duration));
            plan.addView(Ui.text(this, sb.toString(), 14, Ui.TEXT, false));
        } catch (Throwable e) {
            plan.addView(Ui.text(this, "योजना नहीं बन सकी: " + e, 14, Ui.RED, false));
        }
        plan.addView(Ui.button(this, "🎬  फ़िल्म बनाएँ", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { makeFilm(); }
        }));
        body.addView(plan);
    }

    private LinearLayout characterCard(final Story st, final Story.CharacterDef c) {
        LinearLayout card = Ui.card(this);
        LinearLayout top = Ui.row(this);
        ImageView iv = new ImageView(this);
        iv.setScaleType(ImageView.ScaleType.FIT_CENTER);
        String line = project.manifestLine("char", c.displayName);
        if (line == null) line = findCharLine(c);
        final String file = line != null && line.split("\\|").length > 2 ? line.split("\\|")[2] : null;
        final boolean hasPic = file != null && project.has(file);
        if (hasPic) iv.setImageBitmap(thumb(project.file(file), 300));
        else iv.setImageBitmap(puppetThumb(c.look));
        top.addView(iv, new LinearLayout.LayoutParams(Ui.dp(this, 80), Ui.dp(this, 120)));
        LinearLayout info = Ui.column(this);
        info.setPadding(Ui.dp(this, 10), 0, 0, 0);
        info.addView(Ui.text(this, c.displayName, 18, Ui.TEXT, true));
        info.addView(Ui.text(this, describe(c), 13, Ui.SUB, false));
        info.addView(Ui.text(this, hasPic ? "🖼 आपका चित्र" + (line.split("\\|").length >= 11 ? " • 👄 मुँह सेट है" : " • 👄 मुँह अपने आप") : "🎨 ऐप का कार्टून", 13, hasPic ? Ui.GREEN : Ui.SUB, false));
        top.addView(info, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));
        card.addView(top);
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "🖼 चित्र", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { pendingCharName = keyFor(c); pick("image/*", PICK_CHAR); }
        }));
        if (hasPic) r.addView(Ui.small(this, "👄 मुँह", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { showFace(keyFor(c)); }
        }));
        r.addView(Ui.small(this, "🔊 आवाज़", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { cycleVoice(st, c); }
        }));
        if (hasPic) r.addView(Ui.small(this, "✖", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) { project.setManifest("char", keyFor(c), null); project.setManifest("char", c.displayName, null); showCast(); }
        }));
        card.addView(r);
        return card;
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
            default: kind = "पुरुष";
        }
        return kind + (c.age > 0 ? ", " + c.age + " वर्ष" : "") + (l.hero ? "" : " • खलनायक") + (c.fromScript ? "" : " • (विवरण नहीं मिला)");
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

    private LinearLayout sceneCard(final Story.Scene sc) {
        LinearLayout card = Ui.card(this);
        final String key = String.valueOf(sc.number);
        String line = project.manifestLine("scene", key);
        if (line == null) line = project.manifestLine("scene", key + "a");
        final String file = line != null && line.split("\\|").length > 2 ? line.split("\\|")[2] : null;
        card.addView(Ui.text(this, sc.heading + ": " + sc.title, 17, Ui.TEXT, true));
        card.addView(Ui.text(this, "स्थान: " + Sets.name(Sets.detect(sc.setting)) + "   •   संवाद: " + countDialogue(sc), 13, Ui.SUB, false));
        if (file != null && project.has(file)) {
            ImageView iv = new ImageView(this);
            iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
            iv.setImageBitmap(thumb(project.file(file), 500));
            card.addView(iv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 130)));
        } else card.addView(Ui.text(this, "🎨 ऐप की बनाई पृष्ठभूमि", 13, Ui.SUB, false));
        LinearLayout r = Ui.row(this);
        r.addView(Ui.small(this, "🖼 पृष्ठभूमि चित्र", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { pendingScene = key; pick("image/*", PICK_SCENE); }
        }));
        if (file != null) r.addView(Ui.small(this, "✖ हटाएँ", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) { project.setManifest("scene", key, null); project.setManifest("scene", key + "a", null); project.setManifest("scene", key + "b", null); showCast(); }
        }));
        card.addView(r);
        return card;
    }

    private int countDialogue(Story.Scene sc) {
        int n = 0;
        for (Story.Beat b : sc.beats) if (b.type == Story.Beat.DIALOGUE) n++;
        return n;
    }

    private void cycleVoice(final Story st, final Story.CharacterDef c) {
        if (previewVoices == null) {
            toast("आवाज़ें लोड हो रही हैं…");
            new Thread(new Runnable() {
                public void run() {
                    final Voices v = new Voices();
                    final boolean ok = v.init(MainActivity.this, st.hindi);
                    ui.post(new Runnable() {
                        public void run() {
                            if (!ok) { toast("फ़ोन में Text-to-Speech नहीं मिला"); offerTtsInstall(); return; }
                            previewVoices = v;
                            if (!v.languageOk) offerTtsInstall();
                            cycleVoice(st, c);
                        }
                    });
                }
            }).start();
            return;
        }
        java.util.Map<Story.CharacterDef, Voices.Cast> cast = previewVoices.castAll(st, project);
        Voices.Cast k = cast.get(c);
        int n = previewVoices.voices.size();
        if (n > 0 && project.setting("voice." + c.displayName, "").length() > 0) {
            k.voice = (k.voice + 1) % n;
        }
        if (n > 0) project.setSetting("voice." + c.displayName, String.valueOf(k.voice < 0 ? 0 : k.voice));
        String sample = "नमस्ते, मैं " + c.displayName + " हूँ।";
        if (!st.hindi) sample = "Hello, I am " + c.displayName + ".";
        for (Story.Scene sc : st.scenes) for (Story.Beat b : sc.beats) if (b.speaker == c) { sample = b.text; break; }
        if (sample.length() > 90) sample = sample.substring(0, 90);
        previewVoices.preview(sample, k);
        toast("🔊 आवाज़ " + (k.voice + 1) + "/" + Math.max(1, n) + " — फिर दबाएँ तो अगली आवाज़");
    }

    private void offerTtsInstall() {
        new AlertDialog.Builder(this).setTitle("हिंदी आवाज़ चाहिए")
                .setMessage("फ़ोन में हिंदी बोलने वाली आवाज़ डाउनलोड नहीं है। Google Text-to-speech में हिंदी आवाज़ डाउनलोड करें, फिर ऐप दोबारा खोलें।")
                .setPositiveButton("डाउनलोड करें", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int w) {
                        try { startActivity(new Intent(TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA)); }
                        catch (Exception e) { toast("सेटिंग्स > भाषा > Text-to-speech में जाएँ"); }
                    }
                }).setNegativeButton("बाद में", null).show();
    }

    // ================================================================== face (mouth & eyes) setup

    private void showFace(final String charName) {
        screen = 3;
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
        r.addView(again);
        r.addView(save);
        outer.addView(r);
        setScreen(3, outer);
        final String[] steps = {"1/3: चित्र में मुँह के बीच टैप करें", "2/3: बाईं आँख (आपकी तरफ़ से) पर टैप करें", "3/3: दाईं आँख पर टैप करें", "✔ हो गया! \"सहेजें\" दबाएँ"};
        fv.listener = new FaceTapView.Listener() {
            public void changed(int step) { hint.setText(steps[Math.min(3, step)]); }
        };
        again.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { fv.restart(); } });
        String line = project.manifestLine("char", charName);
        if (line == null) { showCast(); return; }
        final String[] f = line.split("\\|");
        new Thread(new Runnable() {
            public void run() {
                final Art.Sprite sp;
                try {
                    sp = Art.makeSprite(project.loader(), f[2], 1100);
                } catch (Throwable e) {
                    ui.post(new Runnable() { public void run() { toast("चित्र नहीं खुला"); showCast(); } });
                    return;
                }
                ui.post(new Runnable() {
                    public void run() {
                        if (sp == null) { toast("चित्र नहीं खुला"); showCast(); return; }
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
                showCast();
            }
        });
    }

    // ================================================================== making the film

    private void makeFilm() {
        saveScript();
        if (project.read("script.txt").trim().length() < 10) { toast("पहले कहानी लिखें या चिपकाएँ"); return; }
        FilmJob.start(this, project);
        showProgress();
    }

    private void showProgress() {
        screen = 4;
        LinearLayout body = page("⏳ फ़िल्म बन रही है", true);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        LinearLayout c = Ui.card(this);
        final TextView stage = Ui.text(this, "", 17, Ui.TEXT, true);
        final ProgressBar bar = new ProgressBar(this, null, android.R.attr.progressBarStyleHorizontal);
        bar.setMax(1000);
        bar.setMinimumHeight(Ui.dp(this, 14));
        final ImageView preview = new ImageView(this);
        preview.setScaleType(ImageView.ScaleType.FIT_CENTER);
        preview.setBackgroundColor(0xFF000000);
        final TextView note = Ui.text(this, "ऐप खुला रखें। स्क्रीन अपने आप चालू रहेगी।", 14, Ui.SUB, false);
        final TextView warn = Ui.text(this, "", 14, Ui.RED, false);
        c.addView(stage);
        c.addView(bar, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 22)));
        c.addView(preview, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, Ui.dp(this, 200)));
        c.addView(note);
        c.addView(warn);
        final Button stop = Ui.button(this, "■  रोकें", Ui.RED, new View.OnClickListener() {
            public void onClick(View v) {
                FilmJob j = FilmJob.current;
                if (j != null) j.cancel();
            }
        });
        c.addView(stop);
        body.addView(c);
        ui.post(new Runnable() {
            public void run() {
                if (screen != 4) return;
                FilmJob j = FilmJob.current;
                if (j == null) { showEditor(); return; }
                stage.setText(j.stage);
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
                if (j.failed) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    stage.setText("❌ फ़िल्म नहीं बन सकी");
                    warn.setText(j.error);
                    stop.setText("← वापस");
                    stop.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showEditor(); } });
                    return;
                }
                if (j.cancelled) {
                    getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                    stage.setText("रोक दिया गया");
                    stop.setText("← वापस");
                    stop.setOnClickListener(new View.OnClickListener() { public void onClick(View v) { showEditor(); } });
                    return;
                }
                ui.postDelayed(this, 400);
            }
        });
    }

    // ================================================================== player & sharing

    private void showPlayer() {
        if (project == null || !project.film().exists()) { showEditor(); return; }
        screen = 5;
        LinearLayout outer = Ui.column(this);
        outer.setBackgroundColor(0xFF000000);
        FrameLayout fl = new FrameLayout(this);
        final VideoView vv = new VideoView(this);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT, Gravity.CENTER);
        fl.addView(vv, lp);
        outer.addView(fl, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        LinearLayout r = Ui.row(this);
        r.setPadding(Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6), Ui.dp(this, 6));
        r.setBackgroundColor(Ui.BG);
        r.addView(Ui.small(this, "← वापस", Ui.PRIMARY, new View.OnClickListener() {
            public void onClick(View v) { vv.stopPlayback(); showEditor(); }
        }));
        r.addView(Ui.small(this, "💾 गैलरी", Ui.GREEN, new View.OnClickListener() {
            public void onClick(View v) { saveToGallery(false); }
        }));
        r.addView(Ui.small(this, "📤 साझा", Ui.BLUE, new View.OnClickListener() {
            public void onClick(View v) { saveToGallery(true); }
        }));
        outer.addView(r);
        setScreen(5, outer);
        MediaController mc = new MediaController(this);
        mc.setAnchorView(vv);
        vv.setMediaController(mc);
        vv.setVideoPath(project.film().getAbsolutePath());
        vv.setOnErrorListener(new android.media.MediaPlayer.OnErrorListener() {
            public boolean onError(android.media.MediaPlayer mp, int what, int extra) {
                toast("फ़िल्म चल नहीं सकी — गैलरी में सहेजकर देखें");
                return true;
            }
        });
        vv.start();
    }

    private void saveToGallery(final boolean thenShare) {
        if (project == null || !project.film().exists()) return;
        if (Build.VERSION.SDK_INT < 29 && checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            pendingShare = thenShare;
            requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, PERM);
            return;
        }
        final File src = project.film();
        final String name = safeName(project.name()) + ".mp4";
        toast(thenShare ? "साझा करने की तैयारी…" : "गैलरी में सहेजा जा रहा है…");
        new Thread(new Runnable() {
            public void run() {
                Uri uri = null;
                String err = null;
                try {
                    String saved = project.setting("savedUri", "");
                    if (saved.length() > 0 && project.setting("saved", "0").equals("1")) uri = Uri.parse(saved);
                    else {
                        uri = copyToGallery(src, name);
                        project.setSetting("savedUri", uri.toString());
                        project.setSetting("saved", "1");
                    }
                } catch (Throwable e) {
                    err = e.getMessage();
                }
                final Uri fu = uri;
                final String fe = err;
                ui.post(new Runnable() {
                    public void run() {
                        if (fu == null) { toast("सहेजा नहीं जा सका: " + fe); return; }
                        if (!thenShare) { toast("✅ गैलरी में सहेजा गया (Movies/KahaniFilm)"); return; }
                        try {
                            Intent s = new Intent(Intent.ACTION_SEND);
                            s.setType("video/mp4");
                            s.putExtra(Intent.EXTRA_STREAM, fu);
                            s.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
                            startActivity(Intent.createChooser(s, "फ़िल्म साझा करें"));
                        } catch (Exception e) {
                            toast("साझा नहीं हो सका");
                        }
                    }
                });
            }
        }).start();
    }

    private boolean pendingShare;

    @Override
    public void onRequestPermissionsResult(int code, String[] perms, int[] res) {
        if (code == PERM && res.length > 0 && res[0] == PackageManager.PERMISSION_GRANTED) saveToGallery(pendingShare);
        else if (code == PERM) toast("गैलरी में सहेजने की अनुमति नहीं मिली");
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
        Project.copy(new FileInputStream(src), new java.io.FileOutputStream(dst));
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
}
