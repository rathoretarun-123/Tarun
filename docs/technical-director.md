# Technical Director protocol — how the app applies it

The director works as a Technical Director by default (`Director.Options.technical = true`). Story is staged
for stability: many short, static, well-grounded shots.

| Protocol rule | What the app does |
|---|---|
| C1 Character lock | Every character keeps one picture (your upload, library pick or studio design) for the whole film. The **Character Lock Sheets** in the descriptions file copy each costume **verbatim** from the script and give the height in feet and a lock-sheet image prompt. |
| C2 First-frame protocol | The shot table gives, for every shot, a **first-frame image prompt** and a **video prompt that uses that image as input** (for making clips in other apps). |
| C3 Aspect ratio lock | The platform (16:9, 9:16, 1:1) is chosen once and drives the render size; prompts say `shape.aspect_ratio = FINAL_AR` and never contain ratio or crop words. |
| C4 Six mandatory fields | Every shot in the table has CHARACTERS, PLACEMENT, ACTION, GROUNDING, LIGHTING, CAMERA in that order. |
| C5 / P1 Motion limit, one action | Shots are about 3 s and never longer than ~4.5 s (`limitShotLength`); each shot's action is one action under 10% of the frame. |
| P2 No long dialogue in a far shot | A line of more than six words always goes to a front-facing close-up. |
| P3 Static camera | `lockCameras()`: every planned camera move becomes the framing of a static shot; changes of view are clean cuts; the renderer's "living camera" drift is off. |
| P4 Forbidden words | Prompts are cleaned of running/flying/spinning/zoom/pan/shaky/handheld/ratio words. |
| P5 Hands | Prompts ask for "hands with 5 fingers, anatomically correct" or keep hands out of close-ups. |
| P6 Clip length | The table says how many 3 s clips each shot needs. |
| §5 Anti-shake | Prompts include "static background, background locked, smooth motion, no morphing, temporal coherence", anticipation and follow-through, cloth with weight. In the app's own film the camera never shakes unless the story asks (earthquake, thunder). |
| §6 Placement and scale | Placement is written in layers: foreground (0–1 m, by place), midground left/centre/right third (who stands where in that frame), background (10–100 m), depth, occlusion, ground. Every character's height is given in feet. Feet stand on the ground line with a contact shadow. |
| §7 Safe zone | Close-ups put the face about 40% from the top (20% headroom) with look room, inside the centre 60%. |
| §8 Lip-sync | In the table, dialogue is split into close-ups of at most six words with `contains_speech = true`; non-close-up speech is marked silent (lip-sync in post). In the app, the lips move on the fine face mesh. |
| §10 Validation | Every shot carries the validation checklist. |
| §11 Pipeline order, §12 Error correction, Final law | Written at the top and bottom of the descriptions file. |
