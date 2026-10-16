package com.tarun.kahani.core;

/**
 * v34: the "Pixar-Level 3D Still Picture Maker — Training Manual for Mobile" (version 1.0, October 2026), trained
 * into the studio's 3D picture maker (the doll, the place set, the pictures made from the user's own, and the
 * prompts sent to the free image service). The manual's engine is a cloud GPU running SDXL with a trained LoRA,
 * IP-Adapter and ControlNet; none of that trains or runs inside this app, so the studio keeps the manual's
 * benchmark, prompt builder, library rules and checklist and applies them in its own renderer (Studio3D, Doll3D,
 * Set3D) and in every prompt. The map says which step is applied where, and what is not done.
 */
public final class StillImageGuide {
    private StillImageGuide() {}

    public static final String TITLE = "3D STILL PICTURE MAKER — Training Manual for Mobile v1.0 (October 2026)";

    public static final String[][] STEPS = {
            {"§2 The five criteria: subsurface scattering, global illumination, appealing design (big expressive eyes, head 1:3 to 1:4), rich materials, cinematic framing",
                    "the doll's renderer: skin with subsurface scattering, a warm key, a cool fill and a rim light, sky and ground bounce, ambient occlusion and soft shadows, a filmic shoulder; eyes with catch-lights; a child's head a third of the height, an adult's about a quarter; cloth, silk, metal, hair and fur materials; the place set with depth of field and atmospheric perspective"},
            {"§3–4 Hybrid architecture: the heavy model in the cloud, the phone keeps the library, the prompt builder and the preview",
                    "the phone draws the doll and the place itself (offline, no download); the free image service is used only when the internet and free AI are on; the library and the prompts live on the phone"},
            {"§5 Mode A text-to-still / Mode B library picture + text (character consistency) / Mode C restyle",
                    "A: the doll and the set built from the description; B: the library picture that fits the description best, recoloured to it (the reference counts 75 %, the words 25 %); C: a camera photo turned into the film's cartoon look"},
            {"§6 The library: categories, a character lock, the cleaning rule (no picture under 512 px)",
                    "people, views, places and things are kept apart; the main picture is the identity lock; a library picture smaller than 512 px on its long side is never the maker's reference"},
            {"§7 The prompt builder: subject, action / emotion, the LOCKED style tag, lighting, quality, camera — and the negative list always",
                    "StillPrompt.build: every character and place prompt carries the six blocks and the negative list (blurry, deformed, bad anatomy, distorted hands, flat, plastic skin, watermark, text…)"},
            {"§9 Captions: style, description, lighting, material in every caption", "the prompt's blocks are the caption: the description, the locked style, the lighting and the materials"},
            {"§13 Performance: compress, cache, progressive preview, a battery saver", "pictures are kept at the size the phone's memory allows; a large doll is drawn without supersampling (the same work as a small one)"},
            {"§14 UX: character lock, style strength, one 'magic' button that adds the quality tags",
                    "the identity lock is the main picture; the reference strength is the 75/25 rule; the quality tags are added to every prompt without a button"},
            {"§15 The approval checklist: skin glows (not plastic), eyes with highlights, soft light with occlusion, background in depth of field, like the reference (≥ 0.75), whole hands, at least 2048 px",
                    "StillQa measures each item on every doll, set and recoloured picture and writes the result beside its ✔ Use / ✖ Reject; a doll that comes out dark, flat or harsh is lit again once before it is offered; pictures from the AI service get the same list in the shot list, with 'look at the hands'"},
            {"§18 From still to animated", "the studio animates the pictures itself: the rig, the meshes, the lip-sync and the camera"},
    };

    public static final String[][] LIMITS = {
            {"SDXL with a Pixar LoRA trained on 400 stills, IP-Adapter, InstantID, ControlNet on a cloud GPU", "no model is trained or run on the phone; the doll is built and lit by the studio's own renderer, and the free service gets the manual's prompt"},
            {"Four options to pick from", "one doll per description; ✖ Reject brings the next proposal (the free model, the reference picture or the doll)"},
            {"An upscaler to 2048+ (Real-ESRGAN, SUPIR)", "the doll is drawn at 2048 px where the phone's memory allows, never upscaled"},
            {"A hand-fix model", "a doll's hands are geometry (always whole); a picture from the service is checked by eye"},
            {"Brand words in the prompt", "left out: the words that describe the look are sent instead"},
    };

    public static final String SUMMARY;
    static {
        StringBuilder b = new StringBuilder(TITLE).append('\n');
        b.append("The manual's steps, and what the 3D maker does for each:\n");
        for (String[] p : STEPS) b.append("  • ").append(p[0]).append(" — ").append(p[1]).append('\n');
        b.append("What the manual asks for that the phone does not do:\n");
        for (String[] p : LIMITS) b.append("  • ").append(p[0]).append(" → ").append(p[1]).append('\n');
        SUMMARY = b.toString();
    }
}
