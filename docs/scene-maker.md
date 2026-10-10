# The 3D scene maker guides — trained into the picture maker (v18)

Two more guides were given for the 3D picture maker: the "AI 3D Animated Image & Video Scene Maker — Reference-Based
Training and Production Guide" (stored as `app/src/main/assets/ai_3d_scene_maker_guide.md`, shown in ⚙ Settings →
📜 The director's protocols) and a plain-English guide to building an image-to-3D app with existing services. Their
rules are code in `core/SceneMaker.java`, `core/Figure3D.java`, `core/StyleCue.java`, `core/Glb.java`,
`core/ImageTo3D.java` and `app/Studio3DArt.java`.

## The governing principle

> The character must remain the same; the camera, expression, action and environment may change.

The user's picture is the identity of the character in every shot. Nothing redraws a character who has a picture: the
views (three-quarter, side, back) are made from that very picture, and every shot — the reverse shots, the walks, the
two-shots — is drawn with the picture or a view made from it.

## What each part of the guide does in the app

| Guide | In the app |
|---|---|
| 1 System architecture (input, reference analyser, canonical asset memory, still-image engine, scene-sequence engine, QC) | input = the Studio's pictures and the script; reference analyser = `Cutout` (the figure, its eyes and mouth), `PicSense`, `StyleCue` (light side, warmth, saturation, contrast, skin tones); canonical memory = the story's manifest (`cast.txt`: the master picture, the views, the proposals, the accepted and rejected corrections) and the library; still-image engine = `Figure3D` / `Doll3D` / `Set3D` rendered by `Studio3D`; scene-sequence engine = the Director; QC = the rubric and the approval gate |
| Engineering rule: never a prompt alone as character memory | the picture itself is the reference; prompts (for the free AI picture service) are only used for a character who has no picture |
| 2 Reference-image understanding: identity, proportions, colour, surface, clothing, lighting | `Cutout` finds the face; `Figure3D.build` reads the runs of every row (head, body, arms, legs, skirt), the skull from the skin span, the hair beside the body; `StyleCue` reads the lighting side and the colour of the references |
| 2.1 Use multiple views; a front view does not reveal the back | the back is the costume going round and the top of the head coming down (`Figure3D.texPoint`); the user can upload a real back or side picture (📷 Back, 📷 Side on the character card), which wins over the made one — the studio then makes only the missing angles; eleven real back views of the sample cast are bundled and kept in the library as views |
| 2.2 Confidence labels | `SceneMaker.HIGH / MEDIUM / LOW` in the character bible: face points set by the user = high, found in the picture = medium, inferred = low (never made permanent) |
| 3 Character identity specification, unique IDs, change history | `SceneMaker.bible`: CHAR_001…, the master picture, face, hair and wardrobe confidence, the views as assets, the accepted and rejected corrections — in the production file (📄 Descriptions) |
| 3.1 Identity-preservation rules | one picture per character for the whole film (the lock sheet, v16); the views carry the picture's own pixels; a doll is made only for a character without a picture |
| 4 Stage A silhouette and proportions | the figure keeps the picture's outline exactly in front and back; its proportions are the picture's |
| 4 Stage B facial construction, features attached to the head during rotation | the face points are carried through the three-quarter view on the figure's surface (`Figure3D.render` marks) |
| 4 Stage C geometry and deformation (a consistent mesh, a rig) | the figure is one mesh per picture; every view has its own `Rig` (the body mesh, the walk, the breathing) |
| 4 Stage D materials | `Studio3D.Material.textured` keeps the picture's own light and shade (68 % unlit) and adds the scene's key, fill and rim lightly; the doll's materials are the v17 set |
| 5 Visual style specification, STYLE LOCK stored apart from identity | `StyleCue` is the project's style (from the references), applied to dolls, places and views; it never changes a character's identity |
| 6 Prompt interpretation and scene construction | the Director's shot list (the six fields, PROJECT / INTENTION / BLOCKING / PERFORMANCE / CONTINUITY / END STATE from v17), with PICTURES USED per shot |
| 7 Camera, composition, over-the-shoulder | the reaction shot of a dialogue becomes an over-the-shoulder reverse when the speaker's back view exists (`Director.overShoulder`, `Renderer.overShoulder`: the shoulder and back of the head large and soft at the frame's edge on the side the listener faces) |
| 8 Lighting and rendering | the two-light model with contact shadows (v14–v17); the views are lit from the references' side |
| 9 Consistency across video scenes, 9.1 continuity record | the handbook's ledger (v17) plus, per shot, OVER THE SHOULDER and PICTURES USED (which picture or view each character is drawn with) |
| 9.3 Recommended video workflow: a master reference or keyframe for every important shot | one picture per shot: the first frame of every shot saved as `shots/<shot id>.jpg` with the film (🎞 One picture per shot on the film screen) |
| 10 Master system prompt (A–I) | A input analysis = Cutout/StyleCue; B canonical memory = manifest + library + bible; C 3D construction = Figure3D; D art direction = StyleCue; E materials = Studio3D; F scene and camera = the Director; G continuity = views per shot and the ledger; H QA = the rubric; I iterative correction = Use / Reject with the rejection remembered |
| 11 Quality-control scoring engine (weights 25/20/15/10/15/15, 0–5, out of 100, thresholds 85/70) | `SceneMaker.ratings / score / status / verdict`; every proposal carries its score and status; a critical defect fails whatever the score (`SceneMaker.critical`) |
| 11.1 Automatic rejection conditions | the identity rule (a doll is never made for a character who has a picture); the user's Reject; the frame-by-frame final QC (v16) rejects boiling and shaking |
| 12 Training-data preparation, 12.1 training stages | no network is trained on the phone (see the limits); the "model" is the geometric and photometric figure model, plus an optional image-to-3D service with the user's key |
| 13 Implementation checklist | reference processing ✓, character consistency ✓ (IDs, bibles, multi-angle views, corrections), image generation ✓ (style, pose for the side view, camera, aspect), video production ✓ (the film), QA ✓ (rubric, Use / Reject, versions kept as `film_previous.mp4`) |
| 14 Recommended development order | followed: reference-based stills (the views) → memory → multi-shot keyframes (one picture per shot) → motion (the film) → QC → actual training only when justified (not done; explained) |

## The plain-English image-to-3D guide

| Guide | In the app |
|---|---|
| Box 1 input: 1–5 photos + a description | the character's picture (the master), optional back and side pictures, the script's description |
| Box 2 make 3D: remove the background, clean, send to a 3D AI | `Cutout` (background removal, v11), `defringe` (the halo of the old background taken out), then either the studio's own figure model or — with the user's Meshy key — the image-to-3D service (`ImageTo3D.meshy`) whose GLB model `Glb` reads and `Studio3D` renders |
| Box 3 make it move: bones and an animation | every view gets its own rig; the film animates it (walk, breathing, gestures) |
| Box 4 show and export | the views on the character card (📐 Views ✓), the film as MP4; GLB / FBX export is not offered (the app makes films, not 3D files) |
| Reference 75 % / description 25 % | `SceneMaker.REFERENCE_STRENGTH`: the picture rules shape, face and colours; the description rules style, pose, material and props (`ImageTo3D.prompt`) |
| Prompt enhancement, T-pose, isolated background, negatives | the free AI picture prompt asks for an A-pose, both hands visible, nothing in front of the body, a plain background, and lists the defects to avoid (`Bible.characterPrompt`) |
| Ask for a back photo too | 📷 Back and 📷 Side on every character card with a picture |
| Queue status while 3D takes minutes | the film job's progress line (the service's percentage is shown while it works) |
| Save every generation with its prompt and reference | the library item's source names the picture it came from; the manifest keeps the file and the score |
| Payment, credits, a web stack, watermarks | not applicable to this app |

## Limits, stated plainly

* The figure model is geometric and photometric: it inflates the picture's outline into a solid and paints it with the
  picture. It cannot invent what the picture does not show (a pattern only on the back, a tail behind the body). The
  back of the head is the top of the head brought down; hair and ribbons beside the head are flat.
* No neural network is trained or shipped. The user's own key for an image-to-3D service (Meshy) is the way to a learned
  model; that path follows the service's public API and was not exercised with a live key here.
* The views are stylised, like paper figures turned in space; they are "on the same line" as the picture in colour,
  costume and proportions, not photographic renders.
