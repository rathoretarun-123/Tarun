package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The "AI 3D Animated Image & Video Scene Maker" guide (reference-based training and production guide) and the
 * plain-English image-to-3D guide, as the studio's rules for every picture it makes from a reference:
 *
 * <ul>
 * <li>The governing principle: the character stays the same; the camera, expression, action and place may
 *     change. The user's picture is the master reference and the identity; every view, every shot, every
 *     insert is made from it, and a description never redraws a character who has a picture.</li>
 * <li>Reference and description together: the picture rules shape, face and colours (REFERENCE_STRENGTH),
 *     the description rules style, pose, material and props.</li>
 * <li>Canonical asset memory: a permanent character ID (CHAR_001…), the master picture, the approved views,
 *     the approved corrections (what the user accepted or rejected) — the story's manifest and the library,
 *     never a prompt alone.</li>
 * <li>Confidence labels for what was read from the references: high (set by the user or seen in several
 *     views), medium (found in one picture), low (inferred; never made permanent).</li>
 * <li>The quality-control rubric: six weighted categories, 0–5 each, a score out of 100 and its status; a
 *     critical defect fails whatever the score.</li>
 * <li>The continuity record of every shot: who is in it, with which picture, facing which way, over whose
 *     shoulder — in the shot list.</li>
 * </ul>
 */
public final class SceneMaker {
    private SceneMaker() {}

    /** How much of a character's shape, face and colours comes from the picture when a description also exists. */
    public static final float REFERENCE_STRENGTH = 0.75f;

    /** The rubric's weights (ch. 11): identity, geometry, style, lighting and composition, compliance, continuity. */
    public static final int[] WEIGHTS = {25, 20, 15, 10, 15, 15};
    public static final String[] CATEGORIES = {"Character identity", "3D geometry and anatomy", "Style and rendering", "Lighting and composition", "Prompt compliance", "Scene continuity"};

    /** Confidence of an attribute read from the references. */
    public static final String HIGH = "high", MEDIUM = "medium", LOW = "low";

    static final String[] KIND_NAMES = {"girl", "woman", "man", "boy", "witch", "monster", "monkey", "old man", "animal", "bird"};

    /** A permanent character ID from the character's order in the cast (CHAR_001…). */
    public static String id(int order) { return String.format(Locale.US, "CHAR_%03d", order + 1); }

    /** The weighted score out of 100 for six ratings 0–5. */
    public static int score(int[] ratings) {
        float sum = 0, wsum = 0;
        for (int i = 0; i < WEIGHTS.length && i < ratings.length; i++) { sum += Math.max(0, Math.min(5, ratings[i])) * WEIGHTS[i]; wsum += WEIGHTS[i]; }
        return Math.round(sum / Math.max(1, wsum) / 5f * 100);
    }

    /** The guide's status for a score (critical defects override it: see critical()). */
    public static String status(int score, String critical) {
        if (critical != null && critical.length() > 0) return "rejected: " + critical;
        if (score >= 85) return "review for approval";
        if (score >= 70) return "needs improvement";
        return "regenerate or repair";
    }

    /**
     * The ratings of a picture the studio made for a character or a place.
     * fromPicture: made from the user's own picture (a view) — the identity is the picture itself.
     * faceKnown / legs / arms: what the model found (geometry); cue: a style cue from the references was applied;
     * traits: how many of the description's traits the look carries (0..1); same: the same picture all film.
     */
    public static int[] ratings(boolean fromPicture, boolean faceKnown, boolean partsFound, boolean cue, float traits, boolean same) {
        int identity = fromPicture ? 5 : 3;                                  // a doll from words is medium confidence
        int geometry = 2 + (faceKnown ? 1 : 0) + (partsFound ? 2 : 1);
        int style = cue ? 4 : 3;
        int light = 4;                                                       // the two-light model, contact shadow, rim
        int comply = Math.round(2 + 3 * Math.max(0, Math.min(1, traits)));
        int cont = same ? 5 : 3;
        return new int[]{identity, Math.min(5, geometry), style, light, comply, cont};
    }

    /** The automatic rejection conditions (11.1) the studio can check itself; "" when none applies. */
    public static String critical(boolean rightCharacter, boolean anatomyOk, boolean requiredPresent) {
        if (!rightCharacter) return "the face differs from the canonical character";
        if (!anatomyOk) return "extra limbs or broken geometry";
        if (!requiredPresent) return "a required character or prop is missing";
        return "";
    }

    /** One line about a made picture for a proposal card and the production file. */
    public static String verdict(int[] ratings, String critical) {
        int sc = score(ratings);
        StringBuilder b = new StringBuilder();
        b.append("Score ").append(sc).append("/100 — ").append(status(sc, critical));
        b.append(" (");
        for (int i = 0; i < CATEGORIES.length; i++) b.append(i > 0 ? ", " : "").append(CATEGORIES[i].toLowerCase(Locale.US)).append(' ').append(ratings[i]);
        return b.append(')').toString();
    }

    /** The pictures each character of a shot is drawn with (the front picture, or a view made from it). */
    static String viewsUsed(Art art, Film film, Film.Shot sh) {
        if (art == null || film == null) return "";
        Film.Seg s = film.segAt(sh.t + 0.01f);
        if (s == null) return "";
        List<String> out = new ArrayList<String>();
        for (Film.Actor a : s.actors) {
            Film.Key k = a.stateAt(sh.t + 0.01f);
            if (!k.visible) continue;
            Art.Sprite sp = art.sprites.get(a.c.id);
            if (sp == null) { out.add(a.c.shown() + ": drawn puppet"); continue; }
            String what = "the picture";
            if (sh.ots.equals(a.c.shown()) && sp.view(2) != null) what = "the back view (foreground)";
            else if (k.moveDur > 0 && sp.view(1) != null && k.body == Pose.STAND) what = "the side view (walking)";
            else if (sh.type == ShotPlanner.TWO_SHOT && sp.view(0) != null && k.body == Pose.STAND) what = "the three-quarter view (two-shot)";
            out.add(a.c.shown() + ": " + what);
        }
        StringBuilder b = new StringBuilder();
        for (String o : out) b.append(b.length() > 0 ? "; " : "").append(o);
        return b.toString();
    }

    /** The character bible of a cast member for the production file (ch. 3), from the look, the picture and the views. */
    public static String bible(int order, Story.CharacterDef c, String masterFile, boolean faceSetByUser, String[] viewFiles, String corrections) {
        StringBuilder b = new StringBuilder();
        b.append("CHARACTER BIBLE ").append(id(order)).append(" — ").append(c.shown()).append('\n');
        b.append("  Identity: ").append(masterFile != null ? "master picture " + masterFile + " (the reference of every shot; confidence " + HIGH + ")" : "no picture: the description and the studio's doll (confidence " + MEDIUM + ")").append('\n');
        Look l = c.look != null ? c.look : new Look();
        b.append("  Face: ").append(faceSetByUser ? "eyes and mouth set by hand (" + HIGH + ")" : masterFile != null ? "eyes and mouth found in the picture (" + MEDIUM + ")" : "from the description (" + LOW + ")").append('\n');
        String desc = c.description == null ? "" : c.description.trim();
        if (desc.length() > 160) desc = desc.substring(0, 160) + "…";
        b.append("  Hair, headwear, wardrobe: ").append(masterFile != null ? "as in the picture (" + HIGH + ")" : "from the description (" + MEDIUM + ")").append(desc.length() > 0 ? " — \"" + desc + "\"" : "").append('\n');
        b.append("  Body: ").append(KIND_NAMES[Math.max(0, Math.min(KIND_NAMES.length - 1, l.kind))]).append(", height ").append(String.format(Locale.US, "%.2f", l.height)).append(" of an adult").append(masterFile != null ? " (the picture's proportions, " + HIGH + ")" : " (" + LOW + ")").append('\n');
        b.append("  Style: the project's style cue (lit from the references' side, their saturation and contrast); the picture rules shape, face and colours (").append(Math.round(REFERENCE_STRENGTH * 100)).append(" %), the description rules style, pose and props\n");
        b.append("  Variable traits: expression, gaze, pose, camera, lighting — never the identity\n");
        b.append("  Assets: ").append(masterFile != null ? "master picture" : "doll");
        if (viewFiles != null) for (int i = 0; i < viewFiles.length; i++) if (viewFiles[i] != null) b.append(", ").append(Figure3D.VIEW_NAMES[i]).append(" view ").append(viewFiles[i]);
        b.append('\n');
        b.append("  Change history: ").append(corrections == null || corrections.length() == 0 ? "no corrections yet" : corrections).append('\n');
        return b.toString();
    }

    /** The guide in short, for the protocols screen. */
    public static final String SUMMARY =
            "AI 3D ANIMATED IMAGE & VIDEO SCENE MAKER — the studio's reference rules\n"
            + "• Governing principle: the character stays the same; camera, expression, action and place may change. Your picture is the master reference "
            + "and the identity of the character in every shot; a description never redraws a character who has a picture.\n"
            + "• Reference + description: the picture rules shape, face and colours (75 %); the description rules style, pose, material and props.\n"
            + "• Canonical memory: a permanent ID (CHAR_001…), the master picture, the approved views (three-quarter, side, back — made from the picture "
            + "or uploaded by you), and your corrections (what you accepted or rejected) live in the story and the library, never in a prompt alone.\n"
            + "• Confidence: high = set by you or seen in several views; medium = found in one picture; low = inferred, never made permanent.\n"
            + "• Views: a front picture does not show the back; the studio builds the figure from the picture's outline and colours and shows the back "
            + "as the costume going round and the top of the head coming down — or uses the back and side pictures you upload.\n"
            + "• Quality control: six weighted categories (identity 25, geometry 20, style 15, lighting and composition 10, compliance 15, continuity 15), "
            + "0–5 each, a score out of 100: 85+ review for approval, 70–84 needs improvement, below 70 regenerate. A critical defect fails whatever the score.\n"
            + "• Approval: every picture the studio makes is a proposal until you accept it where pictures are chosen; rejected pictures are never used.\n"
            + "• Continuity: every shot records who is in it, with which picture, facing which way, and over whose shoulder it is seen.\n"
            + "• Optional AI 3D model service (your key, entered in Settings): the cut-out picture goes to an image-to-3D service, the model comes back "
            + "and the studio renders its views; without a key the studio's own figure model does the work on the phone.";
}
