package com.tarun.kahani.core;

/**
 * The "Phone-Local AI 3D Animated Film Creator" guide (v1.0, 9 October 2026), trained into the studio and
 * hardcoded. The guide as given is bundled in the app's assets (phone_local_film_creator_guide.md); this class
 * holds its tables, the map from each of its sections to the code that applies it, and the places where it
 * disagrees with the documents already in force (and what the studio does there). Nothing trained before is
 * removed: only contradictions are resolved.
 */
public final class PhoneGuide {
    private PhoneGuide() {}

    public static final String TITLE = "PHONE-LOCAL AI 3D ANIMATED FILM CREATOR — implementation guide (v1.0, 9 October 2026)";

    /** §2: the three operating modes, labelled honestly. */
    public static final String[][] MODES = {
            {"Offline core", "import references, edit the story, build the shot list, animate the pictures and dolls, assemble, render, save and export — everything the studio does without a key or a network"},
            {"Offline AI assist", "the cut-out, the face finder, the picture traits (PicSense), the figure model, the voice matching — all on the phone"},
            {"Optional online enhancement", "natural voices, AI pictures and cues, free 3D demos and models — opt-in in Settings, never required to make a film"}};

    /** §7.1: route selection by what the user gave. */
    public static final String[][] ROUTES = {
            {"One portrait", "the picture itself, rigged as a 2.5D figure (head, arms, legs, face mesh); the figure model for its views", "a flat cut-out with parallax"},
            {"Several angles (up to ten)", "the real angles saved as the character's views (front, three-quarter, side, back) and used shot by shot", "the figure model fills the missing angles"},
            {"Full-body front and side", "the figure model fitted to the real side picture", "the 2.5D figure"},
            {"Existing 3D file", "a GLB / glTF read by the studio (skins, animations, props), posed and rendered", "the studio's doll"},
            {"Location photos (up to ten)", "the main picture as the plate with its floor line; the reverse angle for reverse shots", "the painted set with 2.5D depth"},
            {"Small object / prop (up to ten)", "the object's picture as an insert of the thing itself; held props from the description", "a drawn prop"}};

    /** §5.3: the reference record. */
    public static final String[] REFERENCE_RECORD = {"reference_id (CHAR_001 / LOC_001 / PROP_…)", "type", "display_name", "source_images with view labels", "approved_traits", "unknown_traits", "3D_asset_id (doll / model / views)", "confidence per identity, geometry, texture, view coverage", "revision"};

    /** §8.3: the shot card fields (all written in every shot's record). */
    public static final String[] SHOT_CARD = {"shot_id / scene_id", "story_source", "purpose", "cast / location / props (IDs)", "framing", "blocking", "camera", "action / emotion", "dialogue / audio", "continuity constraints", "validation status"};

    /** §9.2: the camera rules. */
    public static final String[] CAMERA_RULES = {
            "Establish geography before fast cutting; keep screen direction unless the axis is crossed with a clear transition",
            "Slow moves only for emotional beats — in force: the locked tripod, so moves are cuts",
            "Faces visible during key dialogue; props never over eyes or mouths",
            "Depth of field only where it hides nothing the story needs",
            "One lens language; no wide-angle distortion on close-up faces",
            "Safe areas for vertical video, captions and platform UI"};

    /** §10.2: the lip-sync acceptance criteria. */
    public static final String[] LIPSYNC = {
            "The mouth opens and closes with the syllables, never at a constant rate",
            "The jaw is plausible and never stretches the face",
            "Mouth shapes blend without popping; teeth and tongue never through the lips",
            "The speaker's identity and voice stay the same between scenes",
            "Subtitles match the approved script, no timing tags"};

    /** §11.1: the render stages. */
    public static final String[] RENDER_STAGES = {"Storyboard animatic", "Low-resolution scene preview", "Shot approval render", "Final sequence render", "Composition", "Encode and verify", "Playback verification", "Project packaging"};

    /** §11.2: the export presets. */
    public static final String[][] EXPORT_PRESETS = {
            {"Draft preview", "480p, fast"}, {"Standard mobile", "720p"}, {"High quality", "1080p (longer, warmer phone)"},
            {"Vertical short", "9:16 framed on its own, never a crop of a 16:9 master"}, {"Project archive", "the backup of the library and the story"}};

    /** §14: failure handling. */
    public static final String[][] FAILURES = {
            {"Ambiguous character match", "the candidates are shown, the user chooses; nothing is merged silently"},
            {"Insufficient photo angles", "the missing views are named; the figure model or the doll stands in, as a proposal"},
            {"Model unavailable / offline", "the deterministic studio does the work and says what is off"},
            {"Out of memory", "a smaller frame size is tried, state saved"},
            {"Overheating", "pause from the progress screen; a smaller quality in Settings"},
            {"Broken rig / deformation", "the frame-by-frame check calms the shot; Human QC can change it"},
            {"Continuity mismatch", "the ledger and the state lines name it; the shot is fixed in the plan"},
            {"Missing asset after reopen", "the inventory marks MISSING FILE; nothing is substituted silently"},
            {"Export interrupted", "the previous film is kept; the new one replaces it only when finished and checked"},
            {"Storage almost full", "the film is written in parts; the cache can be cleared without touching the library"}};

    /** §16: the acceptance checklist. */
    public static final String[] ACCEPTANCE = {
            "Create and reopen a project with no network", "Import and tag existing photos without modifying originals", "Stable IDs and approved reference sheets for every character and location",
            "Ambiguous identities and unseen details surfaced for review", "The same master asset reused in every shot", "The scene and shot list can be inspected and edited",
            "Scene expansion is explainable and adds no plot facts", "One rigged route and one 2.5D fallback", "Preview rendering does not lose work when interrupted",
            "Audio, dialogue and lip-sync reviewed before export", "The exported video plays on the device", "The project can be archived and restored", "Limits documented"};

    /** Where the guide and the documents already in force disagree, and what the studio does (no earlier rule removed). */
    public static final String[][] PRECEDENCE = {
            {"\"Pixar-level\"", "the guide (Appendix C): do not describe the app as Pixar-level or promise parity with a major studio; define measurable targets",
                    "the user's ask: output should be Pixar or Disney level",
                    "the guide's own boundary is adopted and said plainly in the app: every rule of every document is applied and measured (the gates, the frame-by-frame meter, the checklist); a phone bending pictures is not a render farm — the measurable targets are the QC's numbers"},
            {"Slow dolly, pan or orbit for emotional beats", "the guide §9.2", "the Technical Director's locked tripod (in force since v12)",
                    "the tripod wins: an emotional beat gets a locked close-up and a reaction cut; no camera move is drawn"},
            {"Phoneme-to-viseme timing", "the guide §10 (alignment model or manual timing)", "the voices come back as audio only",
                    "the mouth follows the voice's own envelope and its sound (round / wide) at 100 Hz; closed in silence; no drawn mouth over a found mouth (the fine face mesh parts the real lips)"},
            {"Several views reconstructed only when enough exist", "the guide §2.1 / §7.1", "the figure model makes views from one picture (v18)",
                    "real angles always win: an uploaded angle replaces the made view; the made view stands in only for a missing angle and is marked as made"},
            {"Separate dialogue, music and effects tracks", "the guide §10.1", "one mixed track in the film", "the mix ducks music under every line; the animatic and the audio check read the same mix; separate tracks are not exported"},
            {"Native UI frameworks (Compose / Flutter), ONNX / LiteRT", "the guide §3", "the app is plain Android Java without Gradle or native libraries, keyless and public",
                    "kept as is: no model runtime is added; every 'AI assist' is the studio's own deterministic code"}};

    /** The map from the guide's sections to the code. */
    public static final String[][] ENFORCEMENT = {
            {"§1.1 Import from library, files, camera; reference library with named people, places, props", "MainActivity: the Studio cards, the tarunkahani library, the home-screen \"Add to the library\" button; up to ten angles per thing (anglesFor), from the library, the phone or the camera, at the Studio, the progress screen and the check screen"},
            {"§1.1 Never silently guess between two plausible matches", "AutoLibrary: a picture is placed only when its best fit beats the runner-up by a clear margin; otherwise the candidates are listed for the Studio"},
            {"§5.1 Keep the original immutable; derived copies", "Project.savePicture copies; the library keeps the original file; cut-outs, views and avatars are new files"},
            {"§5.2 Recommended capture: front, three-quarter, profiles, full body, reverse angle of a place, prop front/side/rear", "Angles.guess reads each uploaded picture's angle (face found and centred = front; eyes off-centre = three-quarter; one eye or a narrow figure = side; no face = back) and saves it as that view; a place's second picture is its reverse angle"},
            {"§5.2 One picture holding several angles", "Cutout.split: the cut-out's separate figures (connected components of the mask) become separate pictures of the same thing, left to right"},
            {"§6.1 Character lock, turntable, master asset", "lock sheets, the views, the facial identity specification (v20), one picture per character all film"},
            {"§7.1 Route selection", "ROUTES above — Figure3D / Doll3D / Glb / backdrops / inserts"},
            {"§7.2 Reference-conditioned dolls", "Studio3DArt.referenceLook: a doll takes its skin, hair and clothing colours from the nearest uploaded picture that fits the description (the reference is named in the proposal)"},
            {"§8.2 Add scenes responsibly", "Director: an establishing bridge when the place changes without a card; establishing shots, reactions, inserts, POV pairs; never a new plot fact"},
            {"§8.3 Shot card", "every shot's record (the handbook's lines + the manual's ASSETS / TRANSITION / STATE / VOICE-MUSIC lines)"},
            {"§9.1 Anticipation, action, follow-through, settling", "the Renderer's easing, the body-language blend, hair and cloth lag"},
            {"§9.4 Performance budget", "FilmJob.renderVideo: workers and frame slots sized from the phone's free memory; a smaller frame when the encoder refuses; the fast-mesh switch"},
            {"§10.2 Lip-sync criteria", "the mouth from the voice's envelope and sound; lip-sync only in locked close-ups; the mouth shut in silence; no painted mouth where no mouth was found"},
            {"§11.1 Render stages", "the animatic → first frames → the film → the export check (v20)"},
            {"§11.2 Presets", "480p / 720p / 1080p and the five formats with their own framing (PixarLead.FORMATS), Facebook feed and video named"},
            {"§14 Failure handling", "FAILURES above — each with what the app does"},
            {"§15 Privacy", "everything stays on the phone; keys are the user's own; nothing is uploaded unless a service is switched on; no analytics"},
            {"§16 Acceptance", "the Robolectric suite (project offline, imports, IDs, inventory, animatic, export check)"}};

    public static final String SUMMARY;
    static {
        StringBuilder b = new StringBuilder(TITLE).append(" — trained into the studio\n\nTHE THREE MODES (§2)\n");
        for (String[] m : MODES) b.append("• ").append(m[0]).append(": ").append(m[1]).append('\n');
        b.append("\nROUTES FROM PICTURES TO ASSETS (§7.1)\n");
        for (String[] r : ROUTES) b.append("• ").append(r[0]).append(" → ").append(r[1]).append(" (fallback: ").append(r[2]).append(")\n");
        b.append("\nWHERE THE GUIDE AND THE DOCUMENTS IN FORCE DISAGREE — what the studio does\n");
        for (String[] p : PRECEDENCE) b.append("• ").append(p[0]).append(": guide — ").append(p[1]).append("; in force — ").append(p[2]).append(" → ").append(p[3]).append('\n');
        b.append("\nWHERE EACH SECTION LIVES IN THE CODE\n");
        for (String[] e : ENFORCEMENT) b.append("• ").append(e[0]).append(": ").append(e[1]).append('\n');
        b.append("\nFAILURE HANDLING (§14)\n");
        for (String[] f : FAILURES) b.append("• ").append(f[0]).append(": ").append(f[1]).append('\n');
        SUMMARY = b.toString();
    }
}
