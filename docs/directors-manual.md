# The AI Director's Production Manual (v2.0) — trained into the director and hardcoded (v20)

The user gave the studio `AI_Animated_Film_Maker_Directors_Manual.docx` ("AI Animated Film Maker — AI Director's
Production Manual, version 2.0, October 2026") with the instruction **"Train your director with this and hardcode it."**

The manual is bundled as given (converted to Markdown) in `app/src/main/assets/ai_film_maker_directors_manual.md` and
shown in ⚙ Settings → 📜 after the handbook, the scene maker guide, the image-to-3D guide and the directing map.
`core/DirectorsManual.java` holds its tables (principles, the nine stages, the five gates, the eleven QA dimensions,
the shot types, the transitions, the durations, the expression cues, the eight underdeveloped moments, the
acceptance test), the map from every step to the code that applies it (`ENFORCEMENT`), the table of the places where
it disagrees with the protocols already in force (`PRECEDENCE`), and the writers of every record it asks for.

## What the director now does differently

| Manual | What the studio does | Code |
|---|---|---|
| 1.1 Catalogue everything; a file name is never enough | **Project asset inventory** written with every film and in the 📄 production file: every character (`CHAR_001`…), view (`CHAR_001_V2` = the back), voice (`VOICE_001`), place picture, insert picture, open proposal and background sound, with its file, source (your picture / library / studio doll accepted / free model / puppet) and status — each file checked to exist | `Studio3DArt.inventory`, `DirectorsManual.inventory` |
| 1.6 Canonical records with persistent IDs | `CHAR_`, `LOC_`, `VOICE_` and `SC_` IDs in every record; **location records** (interior/exterior, reference, entrances, time-of-day and lighting variants, scenes); the **prop ledger** (owner from the description, held / put down / worn scene by scene) | `DirectorsManual.charIds/locIds/sceneId`, `locationRecords`, `propLedger` |
| 1.7 A shot-specific reference package | Every shot in the QC now carries **ASSETS** (character IDs, the picture each is drawn with, held items, the location ID and its hour) | `DirectorsManual.assets` |
| 2.1–2.4 Analyse the face in a fixed order, no invented measurements | A **facial identity specification** per character in the manual's template: face geometry from the cut-out's found eye, mouth and chin points as fractions of the picture (the method is stated in the record), skin tone and undertone read from the picture, hair and marks from the picture's traits and the description, identity constraints (locked / variable / temporary / uncertain), approval and review status. What no picture shows (iris colour, nostrils, teeth, the parting, the profile without a side picture, the back without a back picture) is listed as **uncertain**, never reported as known | `Studio3DArt.facialSpecs`, `DirectorsManual.facialSpec` |
| 2.6 Expression cues incl. suspicion and relief | Two new feelings: **SUSPICIOUS** (manner "शक से", "संदेह", "suspicious"…: asymmetrical brows, narrowed eyes, head angled) and **RELIEVED** ("राहत", "चैन की साँस", "relieved"…: tension gone, a breath out, posture released) in the face, the body, the puppet, the animals, the music and the listener's empathy | `Pose`, `Director.emotionOf/faceOf/bodyOf/lineMood/empathy`, `Renderer.faceFor/bodyFor/animalState`, `Puppet`, `Bible` |
| 3.1 Narrative beat sheet | The eight beats read from the script's own scenes through the acts and the spine (never forced: a short script says so) | `DirectorsManual.beatSheet` |
| 3.2 Underdeveloped moments | The **scene-coverage report**: the eight checks run on the staged film (characters present without an entrance, things held before they are named, decisions without a look or reaction before them, loud sounds, parts not opened wide, questions without a reaction or an answer, moves across cuts, far jumps of feeling), each marked as a genuine gap or an intentional omission | `DirectorsManual.coverage` |
| 3.2 / 3.4 A dramatic sound has a visible reaction | A loud sound in a direction (धमाका, गरज, दहाड़, चीख, explosion, thunder, crash, bang…) startles **every face on stage a beat later** (the head turns, surprised or scared by the words) and gets a **reaction shot** of the nearest face | `Director.loudReaction` |
| 3.4 Point-of-view | "X की ओर देखती है", "looks at", "stares at"… makes **two shots**: the look (a medium close-up of the looker, eyes first) and the **POV** (the other character framed medium from where the looker stands, or the place ahead of them) — a new shot type beside single, two-shot and over-the-shoulder | `Director.pointOfView`, `ShotPlanner.POV` |
| 3.3 Scenes | **Scene records** per part: ID, purpose, act, location and hour, characters (who enters), beginning and ending state, objectives, essential actions, emotional progression, what is revealed, the transition to the next scene | `DirectorsManual.sceneRecords` |
| 3.5 Shot production record | Beside the handbook's lines, every shot has **TRANSITION IN**, **STATE AT START**, **STATE AT END** (position, facing, body, what is held, the feeling, tears/sweat/wet) and **VOICE / MUSIC** (the voice ID and timing of the line, the music cue, the effects inside the shot) | `DirectorsManual.transition/state/voiceMusic` |
| 3.6 Durations | Establishing shots are **held for 3.9 s** (the top of the protocol's 4-s cap) when no one moves during them; the QC counts how many were held | `Director.direction` (the hold), the QC line |
| 3.7 Animatic | The **animatic** is made before the film: the first frame of every shot held for its length, the title and end cards, with the **real voices, music and sounds** (the mix is now made before Human QC), 320 px wide at 12 fps, saved as `animatic.mp4`. It is the first card of Human QC (tap to play), approved with the first frames — Gate 3. 🎬 Animatic on the film screen plays it again | `FilmJob.animatic`, `MainActivity.showAnimatic` |
| 3.9 Transitions | Every shot names how it is entered: establishing cut, reaction cut, cut on action, match cut, cut on the line, dissolve / fade (dips to black and white), or an ordinary cut | `DirectorsManual.transition` |
| 3.10 Three levels of review | The **three-level review** (shot, scene, film) written from the checks that ran | `DirectorsManual.review` |
| IV The five gates | **Gate 1 Story** (spine + beat sheet + cast), **Gate 2 Assets** (lock sheets, no open proposals), **Gate 3 Animatic** (approved in Human QC), **Gate 4 Shots** (validation + frame-by-frame check), **Gate 5 Film** (the export check, no missing voice) — each passed or NOT passed with the reason, at the end of every film's QC | `DirectorsManual.review` |
| V QA checklist | The **eleven dimensions** answered from the checks, including two new ones: the **audio check** of the mix (lines without a voice, the longest silence inside the scenes, clipping) and the **export check** of the file (it decodes, its duration matches the plan, the audio track is present, the picture size) | `FilmJob.audioCheck/exportCheck` |
| VI Never claim an unrendered, untested output is finished | The film's QC ends with the export check and the gates; the progress screen's "Your film is ready" comes after them | `FilmJob.run` |
| VII Final acceptance test | `tools/testdata/manual_test.txt` (two characters, two places, dialogue, a loud sound, a look, a hug, a change of feeling) is directed in the Robolectric test `directorsManualIsTrainedRecordsAndAnimatic`; the sample film's end-to-end test checks the animatic, the audio and export checks and Gate 3 | `AppTest` |

## Where the manual and the protocols disagree (and what wins)

| Topic | The manual | In force | The studio does |
|---|---|---|---|
| Shot durations | establishing 4–10 s, emotional close-up 3–8 s, dialogue 3–6 s ("planning ranges, not rigid rules") | Technical Director: no shot over 4 s; lip-sync close-ups of at most 6 words | the cap wins: establishing shots are held 3.9 s unless someone enters or moves during them (then the motion rule cuts sooner and the move is seen wide); long emotional moments are cut into locked close-ups and reactions |
| Tracking shots | follow movement when movement matters | the locked tripod; no character moves 15 % of the frame in one shot | cuts cover a walk (side views for walking); the camera never moves |
| Left and right profiles | both profiles on the master sheet | the figure model from one picture is symmetric | one side view is made, recorded as both profiles with the note; a side picture you upload wins |
| Visual embeddings | image similarity when supported | no learned network on the phone without a key | PicSense traits and the description's words; the picture is inspected, never trusted by its name |
| Phoneme-level lip-sync | phoneme timing → visemes | the voices come back as audio only | the mouth follows the voice's own envelope and sound at 100 Hz, never the music's rhythm |
| "Pixar-level" | aim for feature-animation craftsmanship; do not promise Pixar-level results from prompting alone | the user's earlier ask | the manual's own realistic target is adopted (docs/audit-v18.md §4) |
| Regenerate only the affected shot | repair one shot | one render pass from one plan | the shot is fixed in the plan (Human QC, the final QC's 80 % motion cut) and the film is made again from the fixed plan; the neighbours are unchanged by construction |

## What the records look like

The film's quality check (`qc.txt`, also in the 📄 production file) now reads, in order: the shot list with the
handbook's and the manual's lines per shot → the quality check (with the manual's counts) → the Braintrust → the
continuity ledger → the narrative beat sheet → the scene records → the scene-coverage report → the prop ledger →
the location records → the lock sheets → the asset inventory → the facial identity specification of every character
→ the audio check → Human QC and the animatic → the final QC (steps 6 and 8) → the handbook's gates and scores → the
export check → the three-level review, the five gates and the QA checklist.

## Test

Desktop (`MakeFilm` on `tools/testdata/manual_test.txt`, 16:9, 40 s): 81 shots, none over 4 s, 1 point-of-view pair,
1 reaction to the loud sound, both parts opened wide, suspicion and relief on the faces of the two lines that ask for
them, 0 boiling / 0 shaking in the frame-by-frame meter. Robolectric: `directorsManualIsTrainedRecordsAndAnimatic`
and the extended `filmJobFinishesEndToEnd` (animatic card and file, audio and export checks, Gate 3) — see the
README for the suite's result.
