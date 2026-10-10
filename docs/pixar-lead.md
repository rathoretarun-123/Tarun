# Pixar-Lead AI Director protocol v4.0 — hardcoded (v14)

`PIXAR_LEAD = StorySpine + Braintrust + Deakins2Light + 12Principles + SafeZoneResizing[format_spec] + AntiStretchValidation`

The protocol exactly as given (both the v4.0 text and the "PIXAR IS THE BOSS — simple English" rules) is stored in
the app as `app/src/main/assets/pixar_lead_protocol.md` and can be read in ⚙ Settings → 📜 The director's protocols.
It is hardcoded in `app/src/main/java/com/tarun/kahani/core/PixarLead.java` (the formats and their safe zones, the
story rules, the Braintrust, the Deakins key light and colour script, the Disney principle tags, the enhancers, the
first-frame checks, the thumbnail and poster prompts) and enforced by the director, the renderer, the app and the
descriptions file. It sits on top of the Technical Director protocol (`docs/technical-director.md`), which keeps
every rule it had (3-second static shots, lip-sync close-ups of at most six words, motion under 15 %, one action per
shot, the validation layer).

## How each rule is enforced

| Rule | Enforced by |
|---|---|
| **Story engine** R4 spine | `PixarLead.spine(story)`: "Once upon a time / Every day / One day / Because of that ×2 / Until finally" from the script's scenes; the hero is the character with the most lines; the turning points (trouble, climax) are found from the text. Written at the top of the descriptions and kept in `film.spine`. |
| R4 acts → colour script | `PixarLead.acts(story)` gives every scene an act (1 setup, 2 turn, 3 escalation, 4 climax, 5 resolution); `Seg.act` drives `PixarLead.colorScript` (contrast rises towards the climax, warmth returns at the end). |
| R5 at most 4 characters / 60 s | The Braintrust counts speaking characters in every 60 s window and reports the crowded ones (the writer's call; the director keeps every shot on one or two of them). |
| R13 opinions | Braintrust Q2 checks every 5 shots that a speaker has an emotion or a word of will ("नहीं", "ज़रूर", "must", "never", "!"…). |
| R1, R7, R19 | Listed in the descriptions' STORY SPINE section (they are rules for the writer; the director cannot change the script). |
| **Braintrust** every 5 shots | `PixarLead.braintrust(film, story)`: Q1 story clear without dialogue, Q2 strong opinion, Q3 a ma pause, Q4 costume culturally accurate (real garment names: लहंगा, साड़ी, अचकन, पगड़ी…). Suggestions only — AUTHORITY: the director decides. In the quality check and the descriptions. |
| **Deakins: MAX 2 LIGHTS** | `Renderer.light()`: one key light (side, colour and strength from `PixarLead.keyLight(set, tod)`) and one bounce (soft, from the ground, in the colour of the background picture's lower part `Backdrop.avgLow` or the painted floor). The old third "soft" lamp is gone; the rim light is the key's own edge. Every LIGHTING field reads "Key … | Bounce … — two lights only". |
| Colour script per act / place | `PixarLead.colorScript`: garden warm yellow, cave green with orange accents, night blue; the background dictates the colour; `FilmLook` applies it (warmth, green tint, saturation, contrast, bloom), blended across cuts. |
| **Disney 12 principles** | Already in the motion (slow in / slow out, anticipation, follow-through, arcs, squash & stretch on bounces, timing by weight); the tags `anticipation, follow-through, slow in slow out, arcs, squash and stretch, secondary action` go into every video prompt (`PRINCIPLES:` line, checked by the validation layer). New: **secondary action** while idle (a slow weight shift, an occasional glance), **timing by weight** (`Director.weightSpeed`: a monkey moves faster, a giant or a heavy character slower). |
| **Nolan** real sounds, cross-cutting | Steps by the floor's material (`SFX_STEPS_HARD` on stone, marble, cave floors; running steps for runs); long lines cut between the speaker and the listener. |
| **Miyazaki** ma | `Director.maPause`: after two fast beats, a 1.8 s still medium-wide shot on the hero with only the wind (never dropped as a jump cut). Counted in the quality check. |
| **Narsimha** real garments | Braintrust Q4 and a note on every lock sheet whose costume has no real garment name. |
| **Russo** one comic beat per scene, hero's colours | `Director.comicBeat`: once per part (never in a sad part) a character who can carry a joke (naughty, clumsy, a monkey…) scratches their head and shrugs (`G_HEAD_SCRATCH`). The hero's palette (from the look) is the thumbnail's and poster's colour. |
| **Gunn** counterpoint | One joke in a scary scene (the comic beat); one scary shadow in a funny scene when the story has a villain (`FX_SHADOW_PASS`: a shadow sweeps over the ground). |
| **Spider-Verse** on twos | ⚙ Settings → *Animate on twos*: `PixarLead.stepFps` — experts 24, learners 12 (children, "सीख"), rebels 8 (monkeys, "नटखट"); the renderer holds each pose for 2–3 frames while the camera, the place and the lip-sync stay smooth. Off by default. The descriptions note each character's rate. |
| **FORMAT_SPECS** | `PixarLead.FORMATS`: 16:9 1920×1080, 9:16 1080×1920, 1:1 1080×1080, 4:5 1080×1350, 2.39:1 1920×804, each with its safe zones (16:9 eyes on the top third, 20 % headroom, 10 % sides; 9:16 face in the middle 60 %, 15 % top / bottom / sides; 1:1 70 % centre), lens (35 mm vertical, 85 mm landscape) and character scale lock. Chosen once in the dialog before making the film. |
| RULE_RESIZE_1 never stretch | Pictures are always drawn in proportion; prompts are cleaned of "stretch to / resize to / crop to / outpaint"; a stretched picture coming back from AI is refused. |
| RULE_RESIZE_2 ratio as a parameter | `shape.aspect_ratio` and `size` are parameters in every prompt; never written as words (`TechnicalDirector.clean`). |
| RULE_RESIZE_3 safe zones | `Director.safeFrames` (side margins, headroom, caption zone per format), `faceCam` (eyes on the format's eye line, head never cut). The QC stills show the safe zones drawn over every first frame. |
| RULE_RESIZE_4 no blurry upscale | AI plates are asked for at the format's native size (1920×1080…) — `TechnicalDirector.sizeFor(ar, outputHeight)`. |
| RULE_RESIZE_5 letterbox / pillarbox | A picture of another shape is shown whole over a soft enlarged copy of itself, never cut. |
| RULE_RESIZE_6 character scale lock | `Renderer.actorHeight` = format scale (0.6 / 0.5 / 0.65 / 0.6) × 720 × the character's height. |
| RULE_RESIZE_7 first-frame checks | `Director.validateShots`: the subject's head inside the frame, the feet of a full shot in the bottom 85–98 % with the shadow; failures reframe the shot (counted in the quality check). The AI picture's real width and height are verified (≤ 5 % off) and a wrong one is made again. |
| RULE_RESIZE_8 thumbnail / poster | `Director.stills()` + `Renderer.renderSeg`: the thumbnail (1280×720, the hero's face 60 %, solid palette colour, bottom 15 % free for text) and the poster (1080×1920, full body, 40 % empty at the top) are made separately after every film (`thumbnail.jpg`, `poster.jpg`, buttons on the player screen). |
| Prompt builder | `TechnicalDirector.IMAGE_TEMPLATE` / `VIDEO_TEMPLATE`: FORMAT line, lens, STYLE and NEGATIVE lines, `resume_from_snapshot_id`, `contains_speech`, duration, fps and PRINCIPLES tags. |
| FINAL VALIDATION | The validation layer checks every shot and every prompt (aspect as a parameter, face not stretched, head and feet inside, static background, the principle tags); what fails is corrected first. |

## Refined and extended (v15)

The "PIXAR IS THE BOSS — refined and extended" text is stored verbatim in the same asset. What it added to the code:
rules 2, 3, 6, 12, 15 and 16 (`PixarLead.STORY_RULES`), Braintrust Q5 (the face is the same as the lock sheet — always
true here, one picture per character), Disney's numbers (`SQUASH` 0.8, `STRETCH` 1.2 on jumps, `FRAMES_HEAVY` 24 /
`FRAMES_LIGHT` 6, `EXAGGERATION` 1.5), the scared run on twos, the hand-drawn wobble of drawn characters, music as a
character (a heartbeat under tense parts, the dhol under celebrations), the cinema format's 20 % headroom, and the
eight-point final checklist (`PixarLead.CHECKLIST`) answered at the end of every descriptions file.

## Test stories

`tools/testdata/machhuare_ka_beta.txt` (Hindi: a fisherman's son, a boat at sea, a storm, a tiger, diyas at night) and
`tools/testdata/the_lost_kite.txt` (English: a rooftop, a banyan tree, rain, a candle) and
`tools/testdata/neo_mumbai.txt` (a 21st-century free-form script: a coder girl, a robo-dog, an AI witch, a rooftop neon
garden, a mall basement, sounds written as words) are run by the Robolectric tests next to the bundled sample story:
the protocol holds for any script, not only the sample.
