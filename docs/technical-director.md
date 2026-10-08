# Technical Director protocol — hardcoded in v12

The protocol below is hardcoded in `app/src/main/java/com/tarun/kahani/core/TechnicalDirector.java` (its text,
numbers, templates, forbidden words, validation layer and error correction) and enforced by the director,
the renderer and the descriptions file. The director works this way by default (`Director.Options.technical = true`).

## How each rule is enforced in the app

| Rule | Enforced by |
|---|---|
| 1 Role / Final law: shots, not scenes | The film is planned as shots of about 3 s (`SHOT_SECONDS`), never more than 4 s (`MAX_SHOT_SECONDS`, `limitShotLength`). |
| 2 Pixar Test | No morphing: pictures are bent through meshes down to pixel level (about one cell per 2 screen pixels), the face layer moves exactly with the body. Identity: every character keeps one picture all film. Feet on the ground with a soft contact shadow, standing on the floor found in each background. Lip-sync only in locked close-ups. No camera shake unless the story asks (earthquake, thunder). |
| C1 Character lock | One picture per character for the whole film; Character Lock Sheets copy the costume verbatim. |
| C2 First frame | The descriptions file gives a first-frame image prompt and a video prompt from that frame for every shot, from the hardcoded templates. |
| C3 Aspect lock | FINAL_AR is chosen once (16:9, 9:16, 1:1). AI pictures of places, title and end are made in FINAL_AR as a size parameter; characters are made tall and native, never cropped; prompts are cleaned of ratio and crop words (`TechnicalDirector.clean`). |
| C4 Six fields | Every shot prompt has CHARACTERS, PLACEMENT, ACTION, GROUNDING, LIGHTING, CAMERA in this order; the validation layer blocks a prompt without them. |
| C5 Motion limit | `calmMoves` slows runs to walking pace where the story's timing allows; `enforceMotion` cuts to a new still framing whenever a character in the frame has moved 15% of the frame width. |
| P1 One action | `oneActionPerShot` cuts to a second action when it begins. |
| P2 / §8 Lip-sync | `lipSyncShots`: every spoken line is shown in front-facing close-ups framed on the face (as large as the picture stays sharp, the whole head inside), at most six words per shot (balanced, e.g. 4 + 5), alternating close-up, tighter close-up and the listener's silent reaction. The speaker stops walking to speak (`standStillToSpeak`), the head stays almost still, only mouth and jaw move. The face layer is cut from the full-resolution picture. |
| P3 Static camera | `lockCameras`: every shot is a locked tripod; in vertical films the camera no longer pans after the speaker. |
| P4 Forbidden words | `TechnicalDirector.FORBIDDEN` / `clean()` on every generation prompt (AI pictures and the descriptions). |
| P5 Hands | Prompts ask for 5 anatomically correct fingers or keep hands out of close-ups. |
| P6 Clip length | Shots at most 4 s; the table says how many 3 s clips each shot needs. |
| §5 Anti-shake | Gestures ease in and out (slow in, slow out); body language blends over half a second; anticipation before a move (a small dip and lean back) and a settle after it; hair and hems follow through a quarter second behind the head; no hopping bounces; prompts include the anti-shake phrases. |
| §6 Placement and scale | Layers (foreground / midground thirds / background / depth / occlusion / ground) in every shot; heights in feet; feet on the floor found in each background picture. |
| §7 Resizing | `safeFrames`: everyone a shot is about stays whole inside the frame with 15% side margins and headroom; faces in the centre 60%; a neighbour half in a close-up is moved out of the frame; AI pictures are made natively in FINAL_AR. |
| §10 Validation | `validateShots`: every shot of the film is checked; failures go through `TechnicalDirector.correct` (§12) and the result is in the shot list's quality check. Each shot in the descriptions file shows "VALIDATION: passed" or what was corrected. |
| §11 / §12 | Pipeline order and error correction are written in the descriptions file and applied by `TechnicalDirector.correct`. |

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
Parameters: shape.aspect_ratio = {FINAL_AR}; reference_image = {REFERENCES}.
Premium 3D animated feature film still, {FRAMING}.
CHARACTERS: {CHARACTERS}
PLACEMENT: {PLACEMENT}
ACTION: {ACTION} (the moment it begins; one action only, max 10% frame movement)
GROUNDING: {GROUNDING}
LIGHTING: {LIGHTING}
CAMERA: locked tripod, static shot, static background, no camera movement, smooth 24fps, no morphing, no wobble
clothes have weight: heavy fabric with gravity folds, not weightless. {HANDS}. static background, no background movement, background locked, smooth motion, no morphing, consistent character, temporal coherence. No text.
VIDEO TEMPLATE (from first frame):
Parameters: image input = first frame of shot {SHOT}; shape.aspect_ratio = {FINAL_AR}; duration = 3 s.
CHARACTERS: {CHARACTERS}
PLACEMENT: {PLACEMENT}
ACTION: {ACTION}; anticipation: weight shifts and knees bend before any move, slow in and slow out, follow-through: hair, braid and dupatta settle 0.5 s after the head stops; clothes have weight: heavy fabric with gravity folds, not weightless
GROUNDING: {GROUNDING}
LIGHTING: {LIGHTING}
CAMERA: locked tripod, static shot, static background, no camera movement, smooth 24fps, no morphing, no wobble
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
