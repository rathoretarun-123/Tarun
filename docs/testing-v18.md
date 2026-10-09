# Testing v18 — five stories, every option

What was tested before the v18 APK was built, how, and what it found. The numbers come from the desktop harness
(`tools/jvm`, the same `core/` code the phone runs, drawn by AWT instead of the Android canvas) and from the
Robolectric suite (`tools/robotest`, the app's own screens and Android drawing).

## The five stories

| Story | Language | Cast | Pictures |
|---|---|---|---|
| The Two Princesses of Ratnagarh (the sample) | Hindi | 11 characters (two princesses, a friend, a monkey, the king and queen, four guards, a witch, a demon) | 15 character pictures, 11 real back views, 10 backgrounds, 15 full-screen inserts, title and end pages |
| Mithu aur Billi ki Diwali (new for v18) | Hinglish | Dadi, a parrot, a cat, a boy, the village watchman | none — every character is a drawn puppet on the desktop and a 3D doll on the phone |
| The fisherman | Hindi | a fisherman, his daughter, a storm at sea | none |
| The kite | Hindi | two children, a kite, a rooftop | none |
| Neo-Mumbai | Hinglish, modern | a girl, a robot dog, a hacker, a mall, a rooftop | none |

## The options

Every switch the app offers was exercised across the runs: the five delivery formats (16:9, 9:16, 1:1, 4:5,
2.39:1), subtitles on and off, animation on twos, the speed (0.85, 1, 1.2), the brightness edit, Human QC
(the final check's calming), Studio 3D (dolls, places and views as proposals, accepted in the Robolectric job),
the views (over-the-shoulder reverses, side views, three-quarter two-shots) and the library's automatic use.

## The desktop matrix

Each run makes the whole film plan, synthesizes every voice, runs the final check on every shot (pipeline step 6,
every shot played frame by frame at check size), renders the first 40–90 seconds at 24 fps and meters the
finished frames as they are written (step 8). "Boiling" is two frames in a row differing by more than 14 per
cell on average; "shake" is more than 50 % of the frame changing between two frames; "floating" is a standing
character's feet drawn above the ground line.

| Run | Story | Format | Options | Shots checked | Boiling / shake (step 6) | Calmed / still lively | Floating (feet above the ground line) | Gates | Step 8 (the finished film) | Render speed (width) |
|---|---|---|---|---|---|---|---|---|---|---|
| sample_169 (rc=0) | The Two Princesses of Ratnagarh (sample, 15 pictures + 11 real back views + 15 inserts) | 16:9 | defaults | 315 of 316 shots | 0 / 0 | 0 / 0 | 0 of 15179 | 4/4 pass | boiling 0 shots, camera shake 0 shots | 9.4 fps at 640 px |
| story5_169 (rc=0) | Mithu aur Billi ki Diwali (Hinglish, 5 characters, no pictures) | 16:9 | defaults | 47 of 47 shots | 0 / 0 | 0 / 0 | 0 of 2340 | 4/4 pass | boiling 0 shots, camera shake 0 shots | 40.1 fps at 640 px |
| story2_169 (rc=0) | The fisherman (story 2) | 16:9 | animation on twos | 75 of 75 shots | 0 / 0 | 0 / 0 | 0 of 2820 | 4/4 pass | boiling 0 shots, camera shake 0 shots | 40.8 fps at 640 px |
| story3_11 (rc=0) | The kite (story 3) | 1:1 | speed 1.2 | 45 of 45 shots | 0 / 0 | 0 / 0 | 0 of 1968 | 4/4 pass | boiling 0 shots, camera shake 0 shots | 41.2 fps at 480 px |
| story4_239 (rc=0) | Neo-Mumbai (story 4) | 2.39:1 | speed 0.85 | 114 of 114 shots | 0 / 0 | 0 / 0 | 0 of 3940 | 4/4 pass | boiling 0 shots, camera shake 0 shots | 48.1 fps at 720 px |
| sample_916 (rc=0) | The Two Princesses of Ratnagarh (sample) | 9:16 | subtitles, brightness +0.1 | 323 of 326 shots | 0 / 0 | 0 / 0 | 0 of 15288 | 4/4 pass | boiling 0 shots, camera shake 0 shots | 5.7 fps at 360 px |
| story5_45 (rc=0) | Mithu aur Billi ki Diwali | 4:5 | subtitles | 47 of 47 shots | 0 / 0 | 0 / 0 | 0 of 2004 | 4/4 pass | boiling 0 shots, camera shake 0 shots | 33.5 fps at 480 px |


Every run passes all four approval gates with no boiling, no shake and no floating, in the shots checked before the
film and in the finished frames. The sample at 9:16 had 8 boiling close-ups in the first run (a speaker's body
bobbing with a sword swing while the face filled the narrow frame); the close-speaker rule fixed them (0 in the
run above). Render speed is the desktop harness at the width shown (the phone draws at its own size with the
pixel mesh; see "Faster drawing" in Settings).

## The Robolectric suite (16 tests, real Android drawing)

1. `hinglishStoryIsReadAndShownInUsersSpelling` — a Hinglish script is read, the lines are shown as typed.
2. `loginDashboardStoryStudioLibrarySettingsOpenWithoutCrashing` — every screen opens; the home screen is English
   only; the library is "tarunkahani Library".
3. `directorPlacesPicturesFromLibrary` — library pictures are matched to characters and places by name, words and look.
4. `directorFindsVoicesThatFitTheDescription` — saved voices are matched to the voices the script describes.
5. `libraryIsUsedWithoutAnyButton` — the library fills a story's missing pictures and voices by itself.
6. `userSoundsAreUsedWhereTheStoryDescribesThem` — uploaded sounds play where the story mentions them.
7. `filmJobFinishesEndToEnd` — the whole film job on the phone's code path, with Studio 3D proposals decided once.
8. `framesRenderThroughAndroidCanvas` — frames of every kind draw through the Android canvas.
9. `finalQcPlaysEveryShotAndLocksEveryCharacterAndPlace` — lock sheets and plates, the final check of 40 shots
   (no floating, nothing still lively), the meter of step 8.
10. `studio3dBuildsCharactersAndPlacesAndTheHandbookIsWired` — dolls and places from descriptions, the handbook's rules.
11. `bigMeshDrawsThroughTheAndroidCanvas` — a pixel-level mesh above 65 535 vertices draws banded, pixel-exact.
12. `viewsFromThePictureProposalsAlbumAndOverTheShoulder` — the sample's real back view stays the back; the
    three-quarter and side views are proposed from the picture, scored, accepted, loaded with their rigs; the
    dolls for the characters without a picture are proposed; a rejection is remembered; the library keeps everything.
13. `pixarLeadFormatsSpineBraintrustAndStillPages` — formats, safe zones, the story spine, the Braintrust notes.
14. `anyScriptIsStagedByTheProtocol` — a generic script is staged by the technical protocol.
15. `modernFreeFormScriptIsReadStagedAndCued` — a free-form modern script with its cues.
16. `freeGithubSourcesGuidesAndDescriptionDetails` (v19) — the free model catalogue chooses by the description's words
    and keeps the props it names; the free demos' requests are built from a demo's own description and their event
    stream is read (no network); the glTF reader; the four bundled training documents; the description's braid,
    ribbon and wings add geometry behind a figure made from a picture.

Result with the final v18 code: **15 of 15 pass**; with the v19 code **16 of 16 pass** (see the end of this page) (`gradle cleanTest test --offline`, Robolectric with native Android
graphics). The longest are the end-to-end film job (86 s) and the final check of 40 shots (54 s); the views test
takes 13 s (the three-quarter and side views of Vanusha from her picture in 4.3 s).

## What the testing found, and what was done

| Found | Where | Done |
|---|---|---|
| Two cuts within 0.02 s of each other (a planned cut and the lip-sync cut at a line's start): one frame of the first flashed, and the same planned shot was listed twice | the sample, 1:13.7 | `Director.qualityCheck` drops a cut followed within 0.3 s by another (the opening cut of a part stays); `shotsFromCuts` matches a planned shot to its nearest cut, once |
| Lip-sync close-ups on 9:16 boiling at 15–19 per cell: the speaker's body bobbed with a gesture (a sword swing, a clap) while the face filled the narrow frame | the sample at 9:16, 8 shots | a speaker seen close (zoom ≥ 2.5) keeps the body still — gestures move the arms, not the body (`Renderer`); measured again: 5.5 per cell |
| An insert picture's own cuts (into the full-screen still and back) counted as "66 % of the frame moved" | the Android job, shot 002 | every insert is a shot of its own with its two cuts in the shot list (`Director`); the check treats those two cuts as cuts (`FinalQc.special`) |
| The approval card called 8 shots a critical temporal defect although the check had already calmed them | the sample at 9:16 | the card scores what is left after the fix (`FinalQc.Result.boilingLeft/shakingLeft`) |
| A drop of sweat drawn as a flat blue dot in close-ups | the sample, scene 2 | a teardrop with a glint |
| The home screen's library button no longer said "Library" | the screens test | "📚 tarunkahani Library" |
| The views test expected three made views; the sample now gives the real back | the views test | the studio makes only the missing views; the real back is never replaced |

## Pixar-class, honestly

The films pass every gate of the protocol the app carries (story, performance, visual and technical, continuity),
with no boiling, shake or floating in the finished frames, lip-sync measured against the voice, two-light
shading with contact shadows, the filmic finish, and the user's own pictures kept intact in every shot. They are
still pictures bent through meshes and drawn puppets, not a 3D-animated feature: see "Honest limits" in the README.

With the v20 code (the director's manual: a 17th test `directorsManualIsTrainedRecordsAndAnimatic`, and `filmJobFinishesEndToEnd` extended with the animatic, the audio and export checks and Gate 3): see `docs/directors-manual.md` for the run.
