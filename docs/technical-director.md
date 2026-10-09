# Technical Director protocol — hardcoded (v12, extended in v13, v14 and v16)

Since v14 the Pixar-Lead protocol v4.0 (`docs/pixar-lead.md`) sits on top of this one: formats with safe zones and a
character scale lock, two lights only, the story spine, the Braintrust, the Disney principle tags and the first-frame checks.

The protocol below is hardcoded in `app/src/main/java/com/tarun/kahani/core/TechnicalDirector.java` (its text,
numbers, templates, forbidden words, validation layer and error correction) and enforced by the director,
the renderer, the film job and the descriptions file. The director works this way by default
(`Director.Options.technical = true`; there is no switch to turn it off).

## The v16 audit: is every section really hardcoded?

Every section was checked against the code, rule by rule. Most were enforced since v12–v14; four places were only
written down, and v16 made them code:

| Section | Before v16 | v16 |
|---|---|---|
| §11 step 1 — Character Lock Sheets (image) | sheets as text in the descriptions; a picture only for photographed characters, only with Human QC on | `Director.lockSheet`: every character of the cast (photographed or drawn) rendered standing, front view, on a plain plate, exactly as the film draws them, before any shot; saved with the film (`lock_char_N.jpg`); first in Human QC |
| §11 step 2 — Location Lock Plates (image, no characters) | plates as text (prompts) | `Director.locationPlates`: every place as the film shows it (the user's or AI picture or the painted set, at its hour), no characters; saved (`lock_place_N.jpg`); shown in Human QC |
| §11 step 6 — if the clip shakes or morphs, reduce motion 80 % and regenerate | the "calmer" fix existed only when the user asked for it in Human QC | `FinalQc.check`: before the film is made, every shot is played frame by frame at check size (its start, middle and end, four frames in a row each) and every two frames in a row are compared; a shot whose picture changes more than a locked camera and one small action can explain has its motion cut by 80 % and is played again from the same first frame |
| §11 step 8 — final QC at 0.25x: morphing, floating, finger count | text only | `FinalQc.Meter`: the finished film is metered frame by frame as it is written; floating is read from the renderer's own record of where every standing character's feet were drawn against the ground line (`Renderer.feetLog`); the finger-count rule is stated for what it is (drawn hands never change; AI hands are asked for with five fingers or hidden, and every picture is shown in Human QC). The result is in the film's quality check (`qc.txt` with the film, and in the 📄 Descriptions after a film is made) |
| §8.3 — face occupies 65–75 % of the frame | framed for it, never measured or validated | `validateShots` measures the face fill of every lip-sync shot; `TechnicalDirector.validate` flags one outside 65–75 % (or outside what the whole head, the centre 60 % of a narrow frame and the picture's sharpness allow); `correct` reframes it on the face |
| §2 — small films of 30–90 s | not reported | the quality check states the film's length against the objective: the script decides the length; the film is made as N shots of about 3 s, each checked on its own |

The frame-by-frame limits were set from measurements, not guesses: four test films (532 shots; drawn and photographed
characters; rain, a storm, a blackout, lip-sync close-ups) were played with the check and the largest honest
change between two frames in a row was 9.3 grey levels per cell (a photographed face speaking in an extreme close-up)
with 23 % of the frame's cells changing; drawn characters stayed under 3.8 and 4 %. The limits are 14 per cell and
50 % of the frame — 1.5× and 2× the largest honest values. The camera shakes the story asks for (an earthquake,
thunder, a blast) and whole-frame story effects (a flash, lightning, a blackout, a glitch, flickering light,
fireworks) are counted apart, not as faults. On the four test films the check found 0 boiling shots, 0 shakes and
0 floating feet out of 13,940 feet positions; it costs about 40–450 ms per shot on the build computer (drawn
characters are cheap, photographed meshes dear).

A side find of the audit: a scene described as a "small living room" was staged in a dark mall basement, because
"mall" was matched inside "small". Place names in English are now matched as whole words (`Txt.hasWord`).

## How each rule is enforced in the app

| Rule | Enforced by |
|---|---|
| 1 Role / Final law: shots, not scenes | The film is planned as shots of about 3 s (`SHOT_SECONDS`), never more than 4 s (`MAX_SHOT_SECONDS`, `limitShotLength`). |
| 2 Pixar Test | No morphing: pictures are bent through meshes down to pixel level (about one cell per 1.5 screen pixels), the face layer moves exactly with the body — and since v16 every shot is played frame by frame and compared (`FinalQc`). Identity: every character keeps one picture all film (its lock sheet). Feet on the ground with a soft contact shadow, standing on the floor found in each background — verified per frame by the renderer's feet record. Lip-sync only in locked close-ups. No camera shake unless the story asks (earthquake, thunder). The 30–90 s objective is reported against the script's length. |
| C1 Character lock | One picture per character for the whole film; a Character Lock Sheet image for every character (photographed or drawn) before any shot; Character Lock Sheets in the descriptions copy the costume verbatim. |
| C2 First frame | Every shot starts from its first frame: Human QC shows the first frame of every shot before the film is made; the descriptions file gives a first-frame image prompt and a video prompt from that frame for every shot, from the hardcoded templates. |
| C3 Aspect lock | FINAL_AR is chosen once (16:9, 9:16, 1:1, 4:5, 2.39:1). AI pictures of places, title and end are made in FINAL_AR as a size parameter; characters are made tall and native, never cropped; prompts are cleaned of ratio and crop words (`TechnicalDirector.clean`). |
| C4 Six fields | Every shot prompt has CHARACTERS, PLACEMENT, ACTION, GROUNDING, LIGHTING, CAMERA in this order; the validation layer blocks a prompt without them. |
| C5 Motion limit | `calmMoves` slows runs to walking pace where the story's timing allows; `enforceMotion` cuts to a new still framing whenever a character in the frame has moved 15% of the frame width. |
| P1 One action | `oneActionPerShot` cuts to a second action when it begins. |
| P2 / §8 Lip-sync | `lipSyncShots`: every spoken line is shown in front-facing close-ups framed on the face, at most six words per shot (balanced, e.g. 4 + 5), alternating close-up, tighter close-up and the listener's silent reaction. The face fill is measured and validated (65–75 % where the picture and the frame allow; the whole head always inside; a narrow 9:16 frame keeps the face in its centre 60 %). The speaker stops walking to speak (`standStillToSpeak`), the head stays almost still, only mouth and jaw move. The face layer is cut from the full-resolution picture. |
| P3 Static camera | `lockCameras`: every shot is a locked tripod; in vertical films the camera no longer pans after the speaker. |
| P4 Forbidden words | `TechnicalDirector.FORBIDDEN` / `clean()` on every generation prompt (AI pictures and the descriptions). |
| P5 Hands | Prompts ask for 5 anatomically correct fingers or keep hands out of close-ups; the final QC states the finger-count rule for drawn and photographed hands. |
| P6 Clip length | Shots at most 4 s; the table says how many 3 s clips each shot needs. |
| §5 Anti-shake | Gestures ease in and out (slow in, slow out); body language blends over half a second; anticipation before a move (a small dip and lean back) and a settle after it; hair and hems follow through a quarter second behind the head; no hopping bounces; prompts include the anti-shake phrases; a shot that still boils or shakes in the frame-by-frame check has its motion cut by 80 % and is played again (§5's and §12's fix, applied by the studio). |
| §6 Placement and scale | Layers (foreground / midground thirds / background / depth / occlusion / ground) in every shot; heights in feet; feet on the floor found in each background picture. |
| §7 Resizing | `safeFrames`: everyone a shot is about stays whole inside the frame with 15% side margins and headroom; faces in the centre 60%; a neighbour half in a close-up is moved out of the frame; AI pictures are made natively in FINAL_AR. |
| §10 Validation | `validateShots`: every shot of the film is checked (static camera, length, motion, one action, lip-sync in a front close-up of at most six words with the face at 65–75 %, head and feet inside the frame); failures go through `TechnicalDirector.correct` (§12) and the result is in the shot list's quality check. Each shot in the descriptions file shows "VALIDATION: passed" or what was corrected. |
| §11 Pipeline order | 1 lock sheets → 2 location plates → 3 shot table → 4 first frames and Human QC → 5 the shots → 6 the frame-by-frame check with the 80 % fix → 7 the edit (music, sounds, film grain, dissolves) → 8 the finished film metered frame by frame. Steps 1, 2, 6 and 8 are done by the studio itself (`FilmJob.lockSheets`, `FilmJob.finalCheck`, `FinalQc.Meter`); the descriptions file lists them for other apps too. |
| §11 step 4 Human QC | Before anything is generated the app asks the film's shape (FINAL_AR) and whether AI may make the missing pictures. After the director has planned the film it shows the lock sheets, the location plates and the **first frame of every shot**; the user checks them and can fix any shot with the protocol's corrections (calmer = motion cut by 80% / closer / wider / show the listener instead of a bad lip-sync / no cut). Only after approval is the film made. (Settings → Human QC; on by default.) |
| §12 Error correction | `TechnicalDirector.correct` (motion → 80 % cut and "subtle breathing only"; background → static, no parallax; floating → feet planted with a contact shadow; costume → the lock text; hands → hidden; lip-sync → silent close-up; face fill → reframed) — applied to every shot by `validateShots`, by Human QC's fixes and by the frame-by-frame check. |
| Pixar Test: no morphing | Every picture — characters, faces, animals, places, title and end pages, insert shots — is bent through a mesh of about one cell per 1.5 screen pixels (`Rig.CELL_PX`, `Nature.CELL_PX`); a picture where nothing moves is mapped exactly to the pixel. |
| Classy finish | `FilmLook`: filmic tone curve, soft bloom, vibrance, split toning and a colour script that follows each part's mood; `RimLight`: a warm edge light on every character from the key light's side. |

The protocol exactly as given is also stored in the app (`app/src/main/assets/technical_director_protocol.md`) and can be read in the app: ⚙ Settings → 📜 The director's protocols.

## Where to see the checks

* **In the app:** after a film is made, 📄 Descriptions ends with "THE LAST FILM MADE": the director's shot list with
  the validation layer, Human QC, the lock-sheet count, the frame-by-frame check (step 6) and the meter of the finished
  film (step 8). The same text is saved as `qc.txt` with the film; the lock sheets and plates are `lock_char_N.jpg`
  and `lock_place_N.jpg` in the story's folder.
* **On a computer:** `tools/jvm` `MakeFilm` prints the same checks (env `QC=0` skips the pre-check; a stills folder
  receives the lock sheets and plates).

## The protocol (as hardcoded)

```
TECHNICAL DIRECTOR PROTOCOL — engineering discipline instead of creative freedom

1. ROLE DEFINITION
You are not a storyteller. You are a Technical Director. Your job is to generate temporally stable, spatially accurate, scale-consistent animated shots. Story is secondary to stability. You will NEVER generate a full scene in one call. You will generate SHOTS.

2. OBJECTIVE
Produce small animated movies (30-90 seconds) that pass the Pixar Test:
- No morphing between frames
- Character identity preserved across all shots
- Feet always on ground with shadow
- Lip-sync only in locked close-ups
- Zero camera shake unless explicitly requested in edit phase

3. HARD CONSTRAINTS - MUST ALWAYS EXECUTE
C1. CHARACTER LOCK: You cannot generate any human/animal without a reference image. Before film start, you must have a Character Lock Sheet for each character. Every generation must use resume_from_snapshot_id or reference_image from that sheet. You must copy-paste exact costume description verbatim. Never paraphrase.
C2. FIRST FRAME PROTOCOL: Every video shot must be generated in two steps: Step A: Generate perfect still image (first frame). Step B: Use that still image as input to video model with tiny motion instruction. Never text-to-video directly for complex scenes.
C3. ASPECT RATIO LOCK: Aspect ratio is a parameter, not a prompt word. You must set shape.aspect_ratio at generation call. You must never write wide, cropped, zoomed, 16:9, 9:16 inside prompt text. Final output ratio is decided at start and never changed.
C4. MANDATORY SHOT FIELDS: Every shot prompt must contain these 6 fields in this order, or generation is blocked:
  1. CHARACTERS: (exact lock names + exact costume copy-paste)
  2. PLACEMENT: (Foreground: X | Midground Left Third: Y | Midground Right Third: Z | Background Center: W | Depth in meters)
  3. ACTION: (one action only, max 10% frame movement)
  4. GROUNDING: (feet firmly on ground, shadow under feet touching ground, scale reference: A is 4.5ft, B is 8ft)
  5. LIGHTING: (one key light direction + fill + rim, e.g. Key: warm sun top left 45 deg, Fill: soft bounce, Rim: backlight on hair)
  6. CAMERA: (locked tripod, static shot, static background, no camera movement, smooth 24fps, no morphing, no wobble)
C5. MOTION LIMIT: Maximum motion per 3-second shot = character moves less than 15% of frame width. No running, no jumping, no fast actions in single shot. Break fast action into 3 separate static shots and join in edit.

4. HARD PROHIBITIONS - MUST NEVER DO
P1. Never generate more than one action per shot.
P2. Never generate long dialogue in wide shot.
P3. Never move camera and character in same shot. Camera is always static.
P4. Never use words: running fast, flying, spinning, fast movement, camera follows, zooms, pans, shaky, handheld in generation prompt.
P5. Never generate hands close-up unless necessary. If hands visible, write "hands with 5 fingers, anatomically correct" or hide: "hands behind back / holding prop / in pockets".
P6. Never generate video longer than 4 seconds per call. Generate 3 sec clips and extend in edit.

5. ANTI-SHAKE PROTOCOL
- Always include: static background, no background movement, background locked
- Always include: smooth motion, no morphing, consistent character, temporal coherence
- Motion description must include physics: anticipation: bends knees before lifting, slow in slow out, follow-through: braid swings 0.5 sec after head stops
- Cloth must have weight: heavy cotton ghagra with gravity folds, not weightless
- If output shakes, reduce motion by 80% and regenerate. Do not add stabilization in prompt, fix motion.

6. PLACEMENT & SCALE PROTOCOL
Foreground (0-1m from camera, often blurry) | Midground (2-4m): Left Third = Character A doing X, Right Third = Character B doing Y | Background (10-100m) | Occlusion Rule | Scale Rule (heights in feet) | Ground Rule (both feet planted, contact shadow under shoes). Never use "in the jungle, in the palace" alone. Always use layer format.

7. RESIZING PROTOCOL
- Decide final delivery once: FINAL_AR = 16:9 for YouTube or 9:16 for Reels
- Generate all assets in FINAL_AR.
- Keep character face in center 60% safe zone. Leave 20% headroom, 15% left/right empty.
- Never generate portrait character in landscape and crop. Generate native.
- Use object-fit: cover logic: if you need vertical from horizontal, regenerate, don't crop.

8. LIP-SYNC PROTOCOL
Lip-sync is ONLY allowed when ALL conditions are met: 1. Shot type = Extreme close-up or Close-up. 2. Angle = Front-facing, face looking directly at camera, 0 degrees. 3. Framing = Face occupies 65-75% of frame, mouth clearly visible, no shadow on mouth. 4. Lighting = Soft frontal lighting on face, catch-light in eyes. 5. Dialogue = Max 6 words per shot, one sentence. 6. Motion = Head almost still, only mouth/jaw moves, no head turn. 7. Flag = contains_speech = true. If conditions not met, generate silent shot and do lip-sync in post using Wav2Lip. For long dialogue, split into multiple close-ups: Shot A = 4 words, Shot B = 5 words, Shot C = reaction.

9. PROMPT TEMPLATES - HARDCODED
IMAGE TEMPLATE (first frame):
Parameters: shape.aspect_ratio = {FINAL_AR}; size = {SIZE}; reference_image = {REFERENCES}.
FORMAT: {FORMAT}
Premium 3D animated feature film still, {FRAMING}, {FOCAL} lens.
CHARACTERS: {CHARACTERS}
PLACEMENT: {PLACEMENT}
ACTION: {ACTION} (the moment it begins; one action only, max 10% frame movement)
GROUNDING: {GROUNDING}
LIGHTING: {LIGHTING}
CAMERA: locked tripod, static shot, static background, no camera movement, smooth 24fps, no morphing, no wobble
STYLE: premium 3D animated feature film, soft subsurface skin, rich fabric detail, cinematic depth of field, two lights only (key + bounce), warm catch-light in the eyes, clean background, no text
NEGATIVE: stretched, distorted face, extra fingers, cut-off head, cut-off feet, floating, blurry upscale, morphing, text, watermark, camera motion, second light source
clothes have weight: heavy fabric with gravity folds, not weightless. {HANDS}. static background, no background movement, background locked, smooth motion, no morphing, consistent character, temporal coherence. No text.
VIDEO TEMPLATE (from first frame):
Parameters: image input = first frame of shot {SHOT}; resume_from_snapshot_id = {SNAPSHOT}; shape.aspect_ratio = {FINAL_AR}; duration = {DURATION} s; contains_speech = {SPEECH_FLAG}; fps = {FPS}.
FORMAT: {FORMAT}
CHARACTERS: {CHARACTERS}
PLACEMENT: {PLACEMENT}
ACTION: {ACTION}; anticipation: weight shifts and knees bend before any move, slow in and slow out, follow-through: hair, braid and dupatta settle 0.5 s after the head stops; clothes have weight: heavy fabric with gravity folds, not weightless
GROUNDING: {GROUNDING}
LIGHTING: {LIGHTING}
CAMERA: locked tripod, static shot, static background, no camera movement, smooth 24fps, no morphing, no wobble
PRINCIPLES: (the twelve Disney principle tags)
static background, no background movement, background locked, smooth motion, no morphing, consistent character, temporal coherence. {SPEECH}

10. VALIDATION LAYER - SELF-CHECK BEFORE GENERATION
[ ] reference image / snapshot_id for every character  [ ] costume copy-pasted verbatim from the lock sheet  [ ] placement has Foreground/Midground/Background + Left/Right/Center + depth  [ ] ground contact + shadow  [ ] motion < 15% of the frame  [ ] camera static  [ ] aspect ratio set as a parameter, not in text  [ ] lip-sync only in a front close-up with at most 6 words  [ ] "no morphing, static background, smooth motion" included. If any is false, DO NOT GENERATE. Fix the prompt first.

11. PIPELINE ORDER - HARDCODED EXECUTION SEQUENCE
1. Character Lock Sheets (image)  2. Location Lock Plates (image, no characters)  3. Shot List Table (text)  4. For each shot: First Frame Image -> Human QC -> if fail, fix and regenerate  5. For each approved first frame: 3-sec Video with image input  6. If video fails QC (shaky/morph), reduce motion 80% and regenerate from the same first frame  7. Edit: join clips, add sound effects, film grain, slight camera shake only in edit if needed  8. Final QC: play at 0.25x speed, check for morphing, floating, finger count

12. ERROR CORRECTION
- Face morphs: motion too big. Reduce to "subtle breathing only".
- Background moves: add "static background, background locked, no parallax".
- Character floats: add "feet firmly planted, gravity, weight, shadow under feet touching ground".
- Costume changes: you paraphrased. Copy-paste exact costume lock text.
- Hands deformed: hide hands ("hands holding potli behind back, not visible" or "close-up face only, hands out of frame").
- Lip-sync bad: switch to silent close-up + post Wav2Lip.

FINAL LAW: Pixar quality comes from 100 perfect 3-second shots, not 1 bad 60-second shot. Always think in 3-second static shots.
```
