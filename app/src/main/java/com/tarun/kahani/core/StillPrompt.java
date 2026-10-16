package com.tarun.kahani.core;

/**
 * v34: the prompt builder of the "Pixar-Level 3D Still Picture Maker" training manual (§7): every prompt for a made
 * picture is built from six blocks — subject, action / emotion, the style tag (locked: always added), lighting,
 * quality and camera — and carries the negative list. Brand names are left out of the words sent to a service (a
 * service may refuse them, and the studio never claims a studio's name); the words that describe that look stay.
 * The aspect ratio is a parameter of the picture, never words in the prompt (the Technical Director's rule C3,
 * which wins over the manual's "--ar 3:4"). The words the Technical Director forbids in a prompt are not used either.
 */
public final class StillPrompt {
    private StillPrompt() {}

    /** Block 3, locked: added to every prompt. */
    public static final String STYLE = "3D animated feature film still, stylized appealing character design, big expressive eyes with catch-lights, "
            + "soft rounded shapes, adorable and expressive";
    /** Block 4. */
    public static final String LIGHTING = "cinematic three-point lighting (warm key, soft fill, rim light), soft volumetric light, global illumination with bounced light, "
            + "soft shadows with ambient occlusion";
    /** Block 5. */
    public static final String QUALITY = "subsurface scattering on skin, fabric fuzz, individual hair strands, physically based materials, ultra detailed, high resolution 3D render";
    /** Block 6 for a character and for a place. */
    public static final String CAMERA_CHARACTER = "full body, eye-level medium-long lens, centred, filmic colour grade";
    public static final String CAMERA_PLACE = "establishing view, depth of field (soft far background, sharp stage), atmospheric perspective, filmic colour grade";
    /** The negative list (§7: always used). */
    public static final String NEGATIVE = "blurry, low quality, distorted face, deformed, ugly, bad anatomy, extra limbs, distorted hands, extra fingers, 2d, flat shading, "
            + "plastic skin, watermark, text, letters, logo, body cut off at the frame";

    /** The six blocks and the negative list, in the manual's order. */
    public static String build(String subject, String actionEmotion, boolean place) {
        StringBuilder b = new StringBuilder();
        b.append(subject.trim());
        if (actionEmotion != null && actionEmotion.trim().length() > 0) b.append(" Action and feeling: ").append(actionEmotion.trim()).append('.');
        b.append(" Style: ").append(STYLE).append('.');
        b.append(" Lighting: ").append(LIGHTING).append('.');
        b.append(" Quality: ").append(QUALITY).append('.');
        b.append(" Camera: ").append(place ? CAMERA_PLACE : CAMERA_CHARACTER).append('.');
        b.append(" Avoid: ").append(NEGATIVE).append('.');
        return b.toString();
    }

    /** True when a prompt carries every block and the negative list (the builder's own check). */
    public static boolean complete(String prompt) {
        return prompt != null && prompt.contains(STYLE) && prompt.contains("Lighting: ") && prompt.contains("Quality: ") && prompt.contains("Camera: ") && prompt.contains(NEGATIVE);
    }
}
