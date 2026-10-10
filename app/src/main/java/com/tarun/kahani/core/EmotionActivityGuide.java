package com.tarun.kahani.core;

/**
 * v34: the "Comprehensive Training Guide for AI-Based Animated Director — Emotion, Activity & Camera Angle
 * Intelligence, with special focus on dark image front/back disambiguation" (version 2.0, October 2026), trained
 * into the director. The guide describes a neural pipeline (a vision-transformer backbone, face and pose detectors,
 * a depth model, a 100k-image dataset and a five-phase training curriculum on a data-centre GPU). None of that
 * runs or trains inside a phone app, so the studio keeps the guide's RULES and GATES — the parts that decide what a
 * picture shows and what the film may do with it — and applies them in its own reading of every picture
 * (Cutout, Angles, PoseSense, Casting, Renderer). Where the guide names a model, the map below names the studio's
 * own measure that stands in for it.
 */
public final class EmotionActivityGuide {
    private EmotionActivityGuide() {}

    public static final String TITLE = "EMOTION, ACTIVITY & CAMERA ANGLE INTELLIGENCE — Comprehensive Training Guide v2.0 (October 2026), with the dark-image front/back fix";

    /** The guide's steps and where the studio applies each one. */
    public static final String[][] STEPS = {
            {"§2 Core failures (back read as front in the dark; a feeling on the back of a head; bending read as sitting; the angle jittering)",
                    "every picture is tagged with its light level; a back or a faceless figure is NO_FACE and never gets a feeling; bending is its own activity; a step cycle keeps one angle for the whole shot"},
            {"§3 Architecture: preprocessing → validation gate → parallel heads (emotion, activity, angle) → rule fusion → director record",
                    "Cutout (the figure and its face) → the gate in PoseSense.tag (no face or a back → NO_FACE) → the angle (Angles.guess), the activity (the silhouette) and the feeling (the face) read in parallel → the pose line in cast.txt is the record the director casts from"},
            {"§4.1 Emotion taxonomy: Happy, Sad, Angry, Fear, Surprise, Disgust, Neutral, No_Face + intensity + visibility; a back view MUST be No_Face",
                    "neutral, happy, laughing, sad, angry, surprised, thinking, asleep and NO_FACE; the film's own feelings (fear, disgust, pride, relief…) map onto these groups; the back-view rule is enforced in PoseSense.tag and again in the renderer"},
            {"§4.2 Activity taxonomy: stand, sit, walk, run, bend, turn, wave, dance, talk, hold_object",
                    "standing, walking, running, sitting, lying, crouching, arms up, waving, pointing, fighting and (v34) bending; turning is the angle; talking is the lip-sync; a held thing is the prop ledger"},
            {"§4.3 Angle taxonomy: yaw in degrees, front / side / back view classes",
                    "front (0), three-quarter (±45), side (±90), back (180) per picture, mirrored when the character faces the other way"},
            {"§5–6 Dataset and annotation: a gold set of dark back views, two annotators and a reviewer, a light-level tag on every image",
                    "the user's own sheets are the test set (the Robolectric reading tests), every pose line carries its light level, and the user is the reviewer: the review dialog after every upload"},
            {"§7.1 EXIF fix", "every picture is turned upright from its EXIF tag before anything reads it (Project.upright)"},
            {"§7.3 Low-light enhancement, dual path: the original kept, an enhanced copy read",
                    "a figure so dark that no face can be read in it as it is, is read from a brightened copy (gamma 1.8 and a contrast stretch — Cutout.enhance) while the film draws the original pixels; a picture that reads in its own light is never brightened"},
            {"§7.4 Region normalisation (gamma for the body, equalisation for the face)", "the same brightened copy serves the skin, eye and mouth readings (Angles, PoseSense read through Cutout.Result.read())"},
            {"§7.5 Confidence gating: a weak face and back cues → angle only, emotion No_Face", "PoseSense.tag: no face, or the angle read as back → NO_FACE; the casting never puts a faceless picture where a face is wanted"},
            {"§8 Fusion: the angle gates the emotion", "the emotion is read only when the angle is a front, three-quarter or side with a face; a back picture is cast only for back shots (over the shoulder)"},
            {"§11 The six-layer dark-back fix: asymmetry cues (hairline, ears), shoulder line, depth, temporal consistency, a non-learned safety gate, user feedback",
                    "hair above the face, skin in the face box, eye darkness and face width decide front against back (Angles.guess); a step keeps one angle; the gate is in PoseSense; the user confirms unsure readings in the review dialog"},
            {"§11 layer 6: if confidence < 0.65 ask the user — 'image is dark, appears to be back view, confirm?'",
                    "every reading carries a confidence (dark light, thin skin or eye margins lower it); the unsure ones are listed first in the review dialog with a red confirm note"},
            {"§13 Evaluation: dark front, dark back, bright front, bright back sets; the robustness drop; the false-emotion rate on backs",
                    "the reading test darkens the user's sheets and compares the readings with the bright ones; a back never carries a feeling in either"},
            {"§14 Integration: a structured record (view, yaw, emotion, intensity, activity, light level, confidence); face rig disabled for a back view",
                    "the pose line: angle, activity, feeling, height ratio, face points, confidence, light level; the renderer never moves the mouth or blinks on a back or faceless picture"},
            {"§15 Edge: a quick on-device gate", "the whole reading runs on the phone, offline, in the moment the pictures are added"},
            {"§16 Privacy and transparency", "pictures stay in the app's own library on the phone; a NO_FACE reading says why ('no face (back or hidden)') instead of a blank"},
    };

    /** What the guide asks for that a phone app does not do, said plainly. */
    public static final String[][] LIMITS = {
            {"A vision-transformer backbone, YOLO face and RTMPose body detectors, MiDaS depth, Zero-DCE enhancement, ONNX / TensorRT deployment",
                    "no neural model runs on the phone; the studio reads pictures with its own measures (skin, eyes, mouth, silhouette, light), the same for every phone and without a download"},
            {"A 100k-image dataset and a five-phase training curriculum on an A100", "nothing is trained; the rules are fixed in code and the user's corrections re-tag the picture at hand"},
            {"Yaw to the degree, pitch, 21 compound emotions, intensity 0..1, 17 body keypoints", "four angle classes, nine feeling groups, eleven activities; the film's intensity comes from the script's line, not the picture"},
            {"A Kalman filter over video frames", "the pictures are stills; one picture per step keeps the angle steady within a shot"},
    };

    public static final String SUMMARY;
    static {
        StringBuilder b = new StringBuilder(TITLE).append('\n');
        b.append("The guide's steps, and what the studio does for each:\n");
        for (String[] p : STEPS) b.append("  • ").append(p[0]).append(" — ").append(p[1]).append('\n');
        b.append("What the guide asks for that the phone does not do:\n");
        for (String[] p : LIMITS) b.append("  • ").append(p[0]).append(" → ").append(p[1]).append('\n');
        SUMMARY = b.toString();
    }
}
