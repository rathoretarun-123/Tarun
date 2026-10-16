# The AI Animation Director handbook — trained into the director (v17)

The "AI Animation Director — Professional Technical Direction Handbook" (v1.0, October 2026) is the third guide
the director follows, beside the Technical Director protocol (`docs/technical-director.md`, the hard constraints)
and the Pixar-Lead protocol (`docs/pixar-lead.md`, the look and the story engine). Its text as given is stored in
the app (`app/src/main/assets/ai_animation_director_handbook.md`, ⚙ Settings → 📜 The director's protocols) and its
rules are code in `core/Handbook.java` and the classes it touches.

## Where the handbook and the protocols disagree — what the studio does

The handbook is an operating guide with options; the Technical Director protocol is a set of hard constraints.
Where they contradict, the hard constraint wins and the handbook's intent is carried out inside it; where the
protocols are silent, the handbook wins. Nothing contradictory is left in the code.

| Topic | Handbook | Protocol | The studio does |
|---|---|---|---|
| Camera movement | motivated dolly, pan, tracking, crane, orbit; a static frame lets acting carry the scene | P3: the camera is always static; shake only in the edit | every shot is a locked tripod; a motivated move becomes a cut (a dolly-in is a cut-in closer, a dolly-out a cut wider) |
| Shot duration | vary the durations; a realization may need several seconds | P6: about 3 s, never over 4 s | no shot over 4 s; within that, durations follow the beat (a reaction 1.6 s, a line by its words, a ma pause 1.8 s); a long realization is held over two still shots |
| Dialogue coverage | sustained two-shots can beat shot-reverse-shot | §8: lip-sync only in a front close-up of at most six words | while words are spoken the protocol wins; two-shots open and close a dialogue and carry its silent beats |
| Exaggeration | avoid exaggerated expressions without justification | Pixar-Lead: exaggeration 1.5, squash 80 %, stretch 120 % | exaggeration kept for the peaks and the comic beats; subtle elsewhere |
| Lighting plan | key, fill, rim, practical, ambient, atmospheric | Pixar-Lead: two lights only (key + bounce) | two directional sources; the rim is the key's edge; practicals only where the story shows them; ambient = sky and ground; atmospheric = fog |
| Lens | 24–35 broad, 35–50 general, 50–85 portraits, 85+ details | a focal length per format | the handbook's ranges by shot size; the format's own lens for close-ups (`Handbook.focalFor`) |
| Prompt structure | project, character, environment, intention, blocking, performance, cinematography, lighting, motion | C4: six fields in a fixed order | the six fields first, in order; then PROJECT, INTENTION, BLOCKING, PERFORMANCE, CONTINUITY, END STATE |
| Negative constraints | targeted, never indiscriminate | a NEGATIVE line | one targeted list of the recurring defects both name |
| Dutch angle | use sparingly | (silent) | at most one per scene, on a villain's peak only |
| Reference images | references and review are essential | C1 | the lock sheet is the reference of every shot; an earlier frame with an error never overrides it |

## What each chapter does in the app

| Chapter | In the app |
|---|---|
| 1 Mandate, five principles | the principles head the handbook summary; "restraint" and "sequence over isolated beauty" are the locked camera and the quality check |
| 2 Screenplay analysis | `Handbook.sceneObjective`: every scene's narrative objective, each character's goal (their first line), the obstacle, the emotional change (first manner → last manner), the visual idea (place and hour), the exit condition — in the production file, derived from the script |
| 3 Character bible, identity lock | one picture per character all film; Character Lock Sheets before any shot (v16); the master sheet (front, three-quarter, side, back) and the expression sheet from Studio 3D (`Doll3D.masterSheet`, 📐 in the Studio) |
| 4 Art direction, colour script | the Pixar-Lead colour script per act; the environment bible is the Location Lock Plates and Set3D |
| 5 Cinematography | the five questions answered for every shot in the shot list; the shot-size vocabulary (`Handbook.purposeOf`); angle meanings; the lens by shot size; Dutch angle at most once per scene; moves are cuts |
| 6 Acting | thought before action: a sentence with a stimulus (a sound, "अचानक", "hears", "notices"…) gets a 0.55 s beat first — the head turns toward it, the body holds (`G_LISTEN`) — then the action; the gaze written for every shot (`Handbook.gaze`) |
| 7 Lighting, materials, finishing | the two-light model with the rim; Studio 3D's materials (skin with subsurface warmth, hair highlights, cloth, metal, stone, glass eyes); the filmic finish with restrained bloom |
| 8 Shot-generation pipeline | the reference hierarchy = lock sheets; the prompt architecture appended after the six fields; targeted negative constraints |
| 9 Storyboard, animatic | Human QC's first frames are the storyboard; durations vary by beat (the quality check counts the distinct lengths) |
| 10 Hindi voice, lip-sync | `Handbook.voiceIdentity` per character in the production file (age, tonal quality, pace, accent and diction, emotional range, pronunciation of the name); Devanagari dialogue as written; mouth shapes from the audio itself (no English assumptions); listeners' mouths at rest |
| 11 Editing, sound, music | reactions get their beat; jump cuts removed; sounds from the script's words and the floor's material; music ducks under lines and rests in ma pauses |
| 12 Continuity | `Handbook.ledger`: per scene the hour and key light, every character's start and end position, facing, costume state, props, wetness, and the dialogue axis (180-degree principle) — in the film's quality check |
| 13 Quality assurance | `Handbook.score`: the four gates and ten scores from the checks the studio ran (validation, lip-sync words, motion, the frame-by-frame check's boiling/shake/floating); critical failures named whatever the average |
| 14 Failure diagnosis | the protocol's error correction and Human QC's fixes (calmer, closer, wider, listener, no cut) |
| 15 Production management | every shot has a stable ID (`KAHANI_SC04_SH007_V001`); the previous film is kept as `film_previous.mp4` (never overwrite the only approved version) |
| 16 Delivery checklist | `Handbook.delivery`: the fifteen boxes answered by what was done, in the film's quality check |
| Appendix A master prompt | the handbook's words in the summary shown in the app |
| Appendix B production order | mapped onto the pipeline (script analysis → lock sheets → plates → shot table → first frames → shots → QC → edit → export) |

Everything the handbook adds is reported in two places: the film's quality check (📄 Descriptions → "THE LAST FILM
MADE", also `qc.txt` with the film) and the production file (the scene objectives, the voice identities and the
twelve-field prompts).
