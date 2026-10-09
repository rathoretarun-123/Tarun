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

<!-- MATRIX TABLE -->

## The Robolectric suite (15 tests, real Android drawing)

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

<!-- ROBOLECTRIC RESULT -->

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
