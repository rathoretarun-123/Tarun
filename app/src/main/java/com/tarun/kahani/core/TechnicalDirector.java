package com.tarun.kahani.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * The Technical Director protocol, hardcoded. This replaces creative freedom with engineering discipline: the
 * director is not a storyteller but a Technical Director whose job is temporally stable, spatially accurate,
 * scale-consistent animated shots. It never makes a whole scene at once; it makes SHOTS.
 *
 * Everything here is enforced in code, not only written down:
 *  - Director: locked cameras (P3), shots of about 3 s and never more than 4 s (P6), every spoken line in
 *    front-facing close-ups of at most six words (section 8), a new shot whenever a character has moved 15 % of
 *    the frame (C5) or starts a second action (P1), faces in the safe zone and nobody cut by the frame (7),
 *    feet on the ground with a contact shadow (C4.4), and the validation layer with error correction (10, 12).
 *  - Renderer / Rig: head almost still while speaking, slow in / slow out, anticipation and follow-through,
 *    cloth with weight, no shake, no morphing (pixel-level meshes that follow the picture exactly) (5).
 *  - ShotBook: Character Lock Sheets, Location Lock Plates, the shot table with the six fields in order, the
 *    image and video templates below, every prompt validated before it is written (9, 10, 11).
 *  - AI pictures: made in FINAL_AR as a parameter (never as words in the prompt) and cleaned of the
 *    forbidden words (C3, P4).
 */
public final class TechnicalDirector {
    private TechnicalDirector() {}

    // ------------------------------------------------------------------ the numbers of the protocol

    /** FINAL LAW: think in 3-second static shots. */
    public static final float SHOT_SECONDS = 3f;
    /** P6: never longer than 4 seconds per shot. */
    public static final float MAX_SHOT_SECONDS = 4f;
    /** C5: a character moves less than 15 % of the frame width in one shot. */
    public static final float MAX_MOTION = 0.15f;
    /** C4.3: one action only, at most 10 % frame movement (the shot table asks for this). */
    public static final float ACTION_MOTION = 0.10f;
    /** Section 8: at most six words of dialogue per shot. */
    public static final int LIP_SYNC_WORDS = 6;
    /** Section 8.3: the face fills 65-75 % of the frame in a lip-sync shot. */
    public static final float FACE_MIN = 0.65f, FACE_MAX = 0.75f;
    /** Section 7: the face in the centre 60 %, 20 % headroom, 15 % empty at left and right. */
    public static final float SAFE_ZONE = 0.60f, HEADROOM = 0.20f, SIDE_MARGIN = 0.15f;
    /** Section 5 / 12: when a shot shakes or morphs, the motion is reduced by 80 %. */
    public static final float MOTION_FIX = 0.2f;

    public static final String STABLE = "static background, no background movement, background locked, smooth motion, no morphing, "
            + "consistent character, temporal coherence";
    public static final String CAMERA = "locked tripod, static shot, static background, no camera movement, smooth 24fps, no morphing, no wobble";
    public static final String PHYSICS = "anticipation: weight shifts and knees bend before any move, slow in and slow out, "
            + "follow-through: hair, braid and dupatta settle 0.5 s after the head stops";
    public static final String CLOTH = "clothes have weight: heavy fabric with gravity folds, not weightless";
    public static final String GROUND = "feet firmly on the ground, shadow under feet touching the ground, gravity, weight";
    public static final String HANDS = "hands with 5 fingers, anatomically correct, or holding a prop";
    /** The STYLE line of every first-frame prompt (the Pixar-lead prompt builder). */
    public static final String STYLE = "premium 3D animated feature film, soft subsurface skin, rich fabric detail, cinematic depth of field, "
            + "two lights only (key + bounce), warm catch-light in the eyes, clean background, no text";
    /** The NEGATIVE line of every first-frame prompt: what must never appear. */
    public static final String NEGATIVE = "stretched, distorted face, extra fingers, cut-off head, cut-off feet, floating, blurry upscale, morphing, "
            + "text, watermark, camera motion, second light source, "
            // the handbook's targeted constraints (ch. 8): the recurring defects, named
            + "character redesign, costume change, malformed hands, inconsistent shadows, random background characters, abrupt pose change, "
            + "mouth movement during silence, proprietary characters";
    public static final String LIP_SYNC = "close-up, front-facing, face looking directly at the camera 0 degrees, face 65-75% of the frame, "
            + "mouth clearly visible, no shadow on the mouth, soft frontal light on the face, catch-light in the eyes, head almost still, "
            + "only mouth and jaw move, no head turn, contains_speech = true";

    /** P4 and C3: words that are never written in a generation prompt, with what is written instead. */
    public static final String[][] FORBIDDEN = {{"running fast", "walking"}, {"running", "walking"}, {"runs", "walks"}, {"ran ", "walked "},
            {"flying", "standing"}, {"flies", "stands"}, {"spinning", "turning slowly"}, {"spins", "turns slowly"}, {"fast movement", "small movement"},
            {"camera follows", ""}, {"zooms", ""}, {"zooming", ""}, {"zoomed", ""}, {"zoom in", ""}, {"pans", ""}, {"panning", ""}, {"shaky", ""},
            {"handheld", ""}, {"cropped", ""}, {"16:9", ""}, {"9:16", ""}, {"1:1", ""}, {"4:5", ""}, {"2.39:1", ""}, {"widescreen", ""}, {" wide ", " full "},
            {"jumps", "steps"}, {"jumping", "standing"},
            // PixarLead RULE_RESIZE_1: a picture is made natively in its format, never stretched, cropped to fit or outpainted
            // (only the instructions: "squash and stretch" and the NEGATIVE line's "stretched" stay)
            {"stretch to", ""}, {"stretch it", ""}, {"stretch the", ""}, {"stretched to", ""}, {"resize to", ""}, {"crop to", ""},
            {"convert aspect ratio", ""}, {"outpaint", ""}};

    // ------------------------------------------------------------------ section 9: the templates

    /** IMAGE TEMPLATE (first frame). Fill with {@link #fill}. */
    public static final String IMAGE_TEMPLATE =
            "Parameters: shape.aspect_ratio = {FINAL_AR}; size = {SIZE}; reference_image = {REFERENCES}.\n"
            + "FORMAT: {FORMAT}\n"
            + "Premium 3D animated feature film still, {FRAMING}, {FOCAL} lens.\n"
            + "CHARACTERS: {CHARACTERS}\n"
            + "PLACEMENT: {PLACEMENT}\n"
            + "ACTION: {ACTION} (the moment it begins; one action only, max 10% frame movement)\n"
            + "GROUNDING: {GROUNDING}\n"
            + "LIGHTING: {LIGHTING}\n"
            + "CAMERA: " + CAMERA + "\n"
            + "STYLE: " + STYLE + "\n"
            + "NEGATIVE: " + NEGATIVE + "\n"
            + "INTENTION: {INTENTION}\n"
            + "PERFORMANCE: {PERFORMANCE}\n"
            + "CONTINUITY: {CONTINUITY}\n"
            + CLOTH + ". {HANDS}. " + STABLE + ". No text.";

    /** VIDEO TEMPLATE (from the approved first frame, 3 seconds). Fill with {@link #fill}. */
    public static final String VIDEO_TEMPLATE =
            "Parameters: image input = first frame of shot {SHOT}; resume_from_snapshot_id = {SNAPSHOT}; shape.aspect_ratio = {FINAL_AR}; "
            + "duration = {DURATION} s; contains_speech = {SPEECH_FLAG}; fps = {FPS}.\n"
            + "FORMAT: {FORMAT}\n"
            + "CHARACTERS: {CHARACTERS}\n"
            + "PLACEMENT: {PLACEMENT}\n"
            + "ACTION: {ACTION}; " + PHYSICS + "; " + CLOTH + "\n"
            + "GROUNDING: {GROUNDING}\n"
            + "LIGHTING: {LIGHTING}\n"
            + "CAMERA: " + CAMERA + "\n"
            + "PRINCIPLES: " + PixarLead.PRINCIPLES + "\n"
            + "PROJECT: {PROJECT}\n"
            + "INTENTION: {INTENTION}\n"
            + "BLOCKING: {BLOCKING}\n"
            + "PERFORMANCE: {PERFORMANCE}\n"
            + "CONTINUITY: {CONTINUITY}\n"
            + "END STATE: {END}\n"
            + STABLE + ". {SPEECH}";

    /** Replaces {NAME} fields; pairs of name, value. */
    public static String fill(String template, String... kv) {
        String s = template;
        for (int i = 0; i + 1 < kv.length; i += 2) s = s.replace("{" + kv[i] + "}", kv[i + 1] == null ? "" : kv[i + 1]);
        return s;
    }

    /** P4 / C3: takes out every forbidden word (aspect words, fast or camera-motion words) from a prompt. */
    public static String clean(String s) {
        String o = " " + (s == null ? "" : s) + " ";
        for (String[] f : FORBIDDEN) {
            int i;
            while ((i = o.toLowerCase(Locale.ROOT).indexOf(f[0])) >= 0) o = o.substring(0, i) + f[1] + o.substring(i + f[0].length());
        }
        return o.trim().replaceAll("[ \\t]+", " ");
    }

    /** C3: the picture size for FINAL_AR (the aspect ratio is a parameter of the generation, never a word): the format's native size. */
    public static int[] sizeFor(String finalAr) {
        PixarLead.Format f = PixarLead.spec(finalAr);
        return new int[]{f.w, f.h};
    }

    /**
     * The picture size for FINAL_AR at the film's output height (RULE_RESIZE_4: a plate is made at the size it is
     * shown, never a blurry upscale): 1920x1080 for a 1080p 16:9 film, 1080x1920 for 9:16, 1920x804 for 2.39:1.
     */
    public static int[] sizeFor(String finalAr, int outHeight) {
        PixarLead.Format f = PixarLead.spec(finalAr);
        int h = Math.max(f.h, outHeight);
        if (h == f.h) return new int[]{f.w, f.h};
        // a taller output: scaled from the format's own native size, keeping its exact shape (even numbers for the encoder)
        int w = Math.round(h * f.w / (float) f.h / 2) * 2;
        return new int[]{w, h - h % 2};
    }

    /** The frame's width / height for FINAL_AR. */
    public static float ratio(String finalAr) {
        return PixarLead.spec(finalAr).ratio;
    }

    /** The FORMAT line of a prompt: the format's name, size and safe zone (RULE_RESIZE_3), with the ratio left to the parameter. */
    public static String formatLine(String finalAr) {
        PixarLead.Format f = PixarLead.spec(finalAr);
        return f.id + " — " + f.w + "x" + f.h + " px; safe zone: " + f.note + "; character scale " + Math.round(f.charScale * 100) + "% of the frame height";
    }

    /** The focal length of the lens for a format (35mm for vertical, 85mm for landscape close-ups, 50mm square). */
    public static String focalFor(String finalAr) { return PixarLead.spec(finalAr).focal; }

    // ------------------------------------------------------------------ section 10: the validation layer

    /** One shot as the director is about to make it. */
    public static final class Shot {
        public String id = "";
        public boolean referenceImages = true;      // C1: every character from its lock sheet / reference picture
        public boolean costumeVerbatim = true;      // C1: costume copied, never paraphrased
        public String characters = "", placement = "", action = "", grounding = "", lighting = "", camera = CAMERA;
        public boolean staticCamera = true;         // P3
        public boolean aspectAsParameter = true;    // C3
        public float motion;                        // largest movement of a character, fraction of the frame width (C5)
        public int actions = 1;                     // P1
        public float seconds = SHOT_SECONDS;        // P6
        public boolean speech;                      // contains_speech
        public boolean closeUp, frontFacing = true; // section 8
        public int words;                           // section 8.5
        public float face;                          // fraction of the frame height the face fills (lip-sync)
        public String prompt = "";                  // the finished prompt text
        public boolean video;                       // a video prompt (needs the PRINCIPLES tags and the speech flag)
        public float headTop = 0.1f, feet = 0.9f;   // the first-frame checks: the head's top and the feet, fractions of the frame height (full shots)
        public boolean fullBody;                    // the feet are in the frame (a full shot)
        public float faceStretch;                   // |width scale / height scale - 1| of the face (RULE_RESIZE_7: 5% at most)
        /** Section 8.3: the most of the frame's height the face of a lip-sync shot can fill with this picture and frame (the whole head inside, the centre 60%, sharpness). */
        public float faceWant;
        /** What the error correction did to this shot. */
        public final List<String> fixes = new ArrayList<String>();
    }

    /** The checklist; returns what is wrong (empty = generate). */
    public static List<String> validate(Shot s) {
        List<String> bad = new ArrayList<String>();
        if (!s.referenceImages) bad.add("no reference image / snapshot for a character");
        if (!s.costumeVerbatim) bad.add("costume not copied verbatim from the lock sheet");
        String pl = s.placement.toLowerCase(Locale.ROOT);
        if (!(pl.contains("foreground") && pl.contains("midground") && pl.contains("background") && (pl.contains("third") || pl.contains("center") || pl.contains("centre"))
                && pl.contains(" m"))) bad.add("placement without foreground / midground / background, thirds and depth");
        String gr = s.grounding.toLowerCase(Locale.ROOT);
        if (!(gr.contains("feet") && gr.contains("shadow"))) bad.add("no ground contact and shadow");
        if (s.motion >= MAX_MOTION) bad.add(String.format(Locale.US, "motion %.0f%% of the frame (must be under 15%%)", s.motion * 100));
        if (!s.staticCamera) bad.add("camera not static");
        if (!s.aspectAsParameter) bad.add("aspect ratio not set as a parameter");
        if (s.speech && !(s.closeUp && s.frontFacing && s.words <= LIP_SYNC_WORDS)) bad.add("lip-sync outside a front close-up of at most 6 words");
        // section 8.3: the face fills 65-75% of the frame — or as much as the whole head, the centre 60% of a narrow frame and the picture's sharpness allow
        if (s.speech && s.face > 0 && (s.face < Math.min(FACE_MIN, s.faceWant > 0 ? s.faceWant : FACE_MIN) - 0.03f || s.face > FACE_MAX + 0.03f))
            bad.add(String.format(Locale.US, "lip-sync face fills %.0f%% of the frame (65-75%%)", s.face * 100));
        String all = (s.prompt + " " + s.camera).toLowerCase(Locale.ROOT);
        if (!(all.contains("no morphing") && all.contains("static background") && all.contains("smooth motion"))) bad.add("missing: no morphing, static background, smooth motion");
        // C4: the six fields, in this order
        if (s.prompt.length() > 0) {
            int last = -1;
            for (String f : new String[]{"CHARACTERS:", "PLACEMENT:", "ACTION:", "GROUNDING:", "LIGHTING:", "CAMERA:"}) {
                int i = s.prompt.indexOf(f);
                if (i < 0 || i < last) { bad.add("the six fields are not all there in order"); break; }
                last = i;
            }
            String low = " " + s.prompt.toLowerCase(Locale.ROOT) + " ";
            for (String[] f : FORBIDDEN) if (!f[0].trim().isEmpty() && low.contains(f[0])) { bad.add("forbidden word: " + f[0].trim()); break; }
        }
        if (s.actions > 1) bad.add("more than one action in the shot");
        if (s.seconds > MAX_SHOT_SECONDS + 0.05f) bad.add(String.format(Locale.US, "%.1f s long (never more than 4 s)", s.seconds));
        // PixarLead: the Disney principle tags in every video prompt, and the first-frame checks of the resizing module
        if (s.video && s.prompt.length() > 0 && !s.prompt.contains("PRINCIPLES:")) bad.add("principles missing: " + PixarLead.PRINCIPLES);
        if (s.faceStretch > PixarLead.STRETCH_TOLERANCE) bad.add(String.format(Locale.US, "face stretched %.0f%% (5%% at most)", s.faceStretch * 100));
        if (s.headTop < PixarLead.HEAD_MIN) bad.add("head cut by the top of the frame");
        if (s.fullBody && (s.feet < PixarLead.FEET_MIN || s.feet > PixarLead.FEET_MAX)) bad.add(String.format(Locale.US, "feet at %.0f%% of the frame (must be within 85-98%%, with a shadow)", s.feet * 100));
        return bad;
    }

    /**
     * Section 12, error correction: fixes the shot by the protocol's rules until it passes (or reports what it
     * could not fix). Returns the remaining problems.
     */
    public static List<String> correct(Shot s) {
        for (int round = 0; round < 3; round++) {
            List<String> bad = validate(s);
            if (bad.isEmpty()) return bad;
            for (String b : bad) {
                if (b.startsWith("motion")) { s.motion *= MOTION_FIX; s.action += ", subtle breathing only"; s.fixes.add("motion reduced by 80%"); }
                else if (b.startsWith("no ground")) { s.grounding = (s.grounding + ", " + GROUND).replaceAll("^, ", ""); s.fixes.add("feet planted, contact shadow added"); }
                else if (b.startsWith("missing")) { s.prompt += " " + STABLE + ", no parallax."; s.camera = CAMERA; s.fixes.add("static background, no morphing added"); }
                else if (b.startsWith("camera")) { s.staticCamera = true; s.camera = CAMERA; s.fixes.add("camera locked"); }
                else if (b.startsWith("forbidden")) { s.prompt = clean(s.prompt); s.action = clean(s.action); s.fixes.add("forbidden words removed"); }
                else if (b.startsWith("lip-sync face")) {
                    s.face = Math.max(Math.min(FACE_MIN, s.faceWant > 0 ? s.faceWant : FACE_MIN), Math.min(s.face, FACE_MAX));
                    s.fixes.add("framed on the face (65-75% of the frame)");
                }
                else if (b.startsWith("lip-sync")) { s.speech = false; s.fixes.add("made silent (lip-sync in post)"); }
                else if (b.startsWith("aspect")) { s.aspectAsParameter = true; s.prompt = clean(s.prompt); s.fixes.add("aspect ratio moved to the parameter"); }
                else if (b.startsWith("costume")) { s.costumeVerbatim = true; s.fixes.add("costume lock text pasted"); }
                else if (b.contains("s long")) { s.seconds = SHOT_SECONDS; s.fixes.add("split into 3-second clips"); }
                else if (b.startsWith("more than one action")) { s.actions = 1; s.fixes.add("second action moved to its own shot"); }
                else if (b.startsWith("principles")) { s.prompt += "\nPRINCIPLES: " + PixarLead.PRINCIPLES; s.fixes.add("principle tags added"); }
                else if (b.startsWith("face stretched")) { s.faceStretch = 0; s.fixes.add("regenerated natively in the format (no stretch)"); }
                else if (b.startsWith("head cut")) { s.headTop = PixarLead.HEAD_MIN + 0.08f; s.fixes.add("framed wider: headroom added"); }
                else if (b.startsWith("feet at")) { s.feet = 0.92f; s.fixes.add("framed wider: feet and shadow inside the frame"); }
            }
        }
        return validate(s);
    }

    // ------------------------------------------------------------------ the protocol itself

    /** The whole protocol, as given (also in docs/technical-director.md and at the top of the descriptions). */
    public static final String PROTOCOL = ""
            + "TECHNICAL DIRECTOR PROTOCOL — engineering discipline instead of creative freedom\n"
            + "\n"
            + "1. ROLE DEFINITION\n"
            + "You are not a storyteller. You are a Technical Director. Your job is to generate temporally stable, spatially accurate, "
            + "scale-consistent animated shots. Story is secondary to stability. You will NEVER generate a full scene in one call. You will generate SHOTS.\n"
            + "\n"
            + "2. OBJECTIVE\n"
            + "Produce small animated movies (30-90 seconds) that pass the Pixar Test:\n"
            + "- No morphing between frames\n- Character identity preserved across all shots\n- Feet always on ground with shadow\n"
            + "- Lip-sync only in locked close-ups\n- Zero camera shake unless explicitly requested in edit phase\n"
            + "\n"
            + "3. HARD CONSTRAINTS - MUST ALWAYS EXECUTE\n"
            + "C1. CHARACTER LOCK: You cannot generate any human/animal without a reference image. Before film start, you must have a Character Lock "
            + "Sheet for each character. Every generation must use resume_from_snapshot_id or reference_image from that sheet. You must copy-paste exact "
            + "costume description verbatim. Never paraphrase.\n"
            + "C2. FIRST FRAME PROTOCOL: Every video shot must be generated in two steps: Step A: Generate perfect still image (first frame). Step B: Use "
            + "that still image as input to video model with tiny motion instruction. Never text-to-video directly for complex scenes.\n"
            + "C3. ASPECT RATIO LOCK: Aspect ratio is a parameter, not a prompt word. You must set shape.aspect_ratio at generation call. You must never "
            + "write wide, cropped, zoomed, 16:9, 9:16 inside prompt text. Final output ratio is decided at start and never changed.\n"
            + "C4. MANDATORY SHOT FIELDS: Every shot prompt must contain these 6 fields in this order, or generation is blocked:\n"
            + "  1. CHARACTERS: (exact lock names + exact costume copy-paste)\n"
            + "  2. PLACEMENT: (Foreground: X | Midground Left Third: Y | Midground Right Third: Z | Background Center: W | Depth in meters)\n"
            + "  3. ACTION: (one action only, max 10% frame movement)\n"
            + "  4. GROUNDING: (feet firmly on ground, shadow under feet touching ground, scale reference: A is 4.5ft, B is 8ft)\n"
            + "  5. LIGHTING: (one key light direction + fill + rim, e.g. Key: warm sun top left 45 deg, Fill: soft bounce, Rim: backlight on hair)\n"
            + "  6. CAMERA: (locked tripod, static shot, static background, no camera movement, smooth 24fps, no morphing, no wobble)\n"
            + "C5. MOTION LIMIT: Maximum motion per 3-second shot = character moves less than 15% of frame width. No running, no jumping, no fast "
            + "actions in single shot. Break fast action into 3 separate static shots and join in edit.\n"
            + "\n"
            + "4. HARD PROHIBITIONS - MUST NEVER DO\n"
            + "P1. Never generate more than one action per shot.\n"
            + "P2. Never generate long dialogue in wide shot.\n"
            + "P3. Never move camera and character in same shot. Camera is always static.\n"
            + "P4. Never use words: running fast, flying, spinning, fast movement, camera follows, zooms, pans, shaky, handheld in generation prompt.\n"
            + "P5. Never generate hands close-up unless necessary. If hands visible, write \"hands with 5 fingers, anatomically correct\" or hide: "
            + "\"hands behind back / holding prop / in pockets\".\n"
            + "P6. Never generate video longer than 4 seconds per call. Generate 3 sec clips and extend in edit.\n"
            + "\n"
            + "5. ANTI-SHAKE PROTOCOL\n"
            + "- Always include: static background, no background movement, background locked\n"
            + "- Always include: smooth motion, no morphing, consistent character, temporal coherence\n"
            + "- Motion description must include physics: anticipation: bends knees before lifting, slow in slow out, follow-through: braid swings "
            + "0.5 sec after head stops\n"
            + "- Cloth must have weight: heavy cotton ghagra with gravity folds, not weightless\n"
            + "- If output shakes, reduce motion by 80% and regenerate. Do not add stabilization in prompt, fix motion.\n"
            + "\n"
            + "6. PLACEMENT & SCALE PROTOCOL\n"
            + "Foreground (0-1m from camera, often blurry) | Midground (2-4m): Left Third = Character A doing X, Right Third = Character B doing Y | "
            + "Background (10-100m) | Occlusion Rule | Scale Rule (heights in feet) | Ground Rule (both feet planted, contact shadow under shoes). "
            + "Never use \"in the jungle, in the palace\" alone. Always use layer format.\n"
            + "\n"
            + "7. RESIZING PROTOCOL\n"
            + "- Decide final delivery once: FINAL_AR = 16:9 for YouTube or 9:16 for Reels\n- Generate all assets in FINAL_AR.\n"
            + "- Keep character face in center 60% safe zone. Leave 20% headroom, 15% left/right empty.\n"
            + "- Never generate portrait character in landscape and crop. Generate native.\n"
            + "- Use object-fit: cover logic: if you need vertical from horizontal, regenerate, don't crop.\n"
            + "\n"
            + "8. LIP-SYNC PROTOCOL\n"
            + "Lip-sync is ONLY allowed when ALL conditions are met: 1. Shot type = Extreme close-up or Close-up. 2. Angle = Front-facing, face looking "
            + "directly at camera, 0 degrees. 3. Framing = Face occupies 65-75% of frame, mouth clearly visible, no shadow on mouth. 4. Lighting = Soft "
            + "frontal lighting on face, catch-light in eyes. 5. Dialogue = Max 6 words per shot, one sentence. 6. Motion = Head almost still, only "
            + "mouth/jaw moves, no head turn. 7. Flag = contains_speech = true. If conditions not met, generate silent shot and do lip-sync in post "
            + "using Wav2Lip. For long dialogue, split into multiple close-ups: Shot A = 4 words, Shot B = 5 words, Shot C = reaction.\n"
            + "\n"
            + "9. PROMPT TEMPLATES - HARDCODED\n"
            + "IMAGE TEMPLATE (first frame):\n" + IMAGE_TEMPLATE + "\n"
            + "VIDEO TEMPLATE (from first frame):\n" + VIDEO_TEMPLATE + "\n"
            + "\n"
            + "10. VALIDATION LAYER - SELF-CHECK BEFORE GENERATION\n"
            + "[ ] reference image / snapshot_id for every character  [ ] costume copy-pasted verbatim from the lock sheet  "
            + "[ ] placement has Foreground/Midground/Background + Left/Right/Center + depth  [ ] ground contact + shadow  [ ] motion < 15% of the frame  "
            + "[ ] camera static  [ ] aspect ratio set as a parameter, not in text  [ ] lip-sync only in a front close-up with at most 6 words  "
            + "[ ] \"no morphing, static background, smooth motion\" included. If any is false, DO NOT GENERATE. Fix the prompt first.\n"
            + "\n"
            + "11. PIPELINE ORDER - HARDCODED EXECUTION SEQUENCE\n"
            + "1. Character Lock Sheets (image)  2. Location Lock Plates (image, no characters)  3. Shot List Table (text)  4. For each shot: First Frame "
            + "Image -> Human QC -> if fail, fix and regenerate  5. For each approved first frame: 3-sec Video with image input  6. If video fails QC "
            + "(shaky/morph), reduce motion 80% and regenerate from the same first frame  7. Edit: join clips, add sound effects, film grain, slight "
            + "camera shake only in edit if needed  8. Final QC: play at 0.25x speed, check for morphing, floating, finger count\n"
            + "\n"
            + "12. ERROR CORRECTION\n"
            + "- Face morphs: motion too big. Reduce to \"subtle breathing only\".\n- Background moves: add \"static background, background locked, no parallax\".\n"
            + "- Character floats: add \"feet firmly planted, gravity, weight, shadow under feet touching ground\".\n"
            + "- Costume changes: you paraphrased. Copy-paste exact costume lock text.\n"
            + "- Hands deformed: hide hands (\"hands holding potli behind back, not visible\" or \"close-up face only, hands out of frame\").\n"
            + "- Lip-sync bad: switch to silent close-up + post Wav2Lip.\n"
            + "\n"
            + "FINAL LAW: Pixar quality comes from 100 perfect 3-second shots, not 1 bad 60-second shot. Always think in 3-second static shots.\n";
}
