# Testing v38 — the animated director's craft guide, then the user's review

## 1. The guide

The user gave an additional guide for the AI director ("AI Adult Animated Director — Comprehensive Guide, Pixar / Disney
Style") and asked for it to be trained into the director.

Most of that guide is sexual content: a vocabulary of sex acts and body parts, scene recipes for sex, masturbation, nude
bathing and undressing, ejaculation, choosing and compositing pictures by nudity, and adult sounds. **None of it is trained.**
The studio makes family films; it does not recognise, draw, voice or score any of it. The guide is not bundled in the app.

Its film craft is trained and hardcoded in `core/FilmCraft.java` (15 rules, each naming the code that applies it, shown in the
protocols screen, reported in every shot list):

| Guide section (craft only) | Where it lives |
|---|---|
| §1.2 the script as a production brief | already the director's practice (ShotPlanner arc, shot list) — recorded as a rule |
| §2 appealing design, cinematic light (key, fill, rim), colour and atmosphere | `FilmCraft.beatLook` via `FilmLook.at`: richer at the peak, warmer when tender (new); key/fill/rim already in Renderer/Art |
| §2 "exaggerated but believable performance", §3.4 readable expression states | `FilmCraft.flush`: cheeks flush with anger, pride, laughter and effort, by the beat's intensity (new) |
| §2 storytelling camera | already ShotPlanner (establishing wides, push-ins while tension rises, closer at the peak) |
| §4 best picture per beat (feeling first, then body and angle) | already `Casting` (v27) — recorded as a rule |
| §8 missing picture: ask the user, never invent | `FilmCraft.pictureWants`: "PICTURES THAT WOULD HELP" per character in the shot list (new) |
| §7 quality and continuity checklist | `FilmCraft.check`: speaker on the stage and facing us, breathing room after peak lines, touches only where written (new) |
| §8 consent and agency as written | touches (hug, held hand, peck) only from the script's words — counted by the checklist, tested |
| §9 delivery with scene markers | `FilmCraft.editList`; FilmJob saves `edit_list.txt`; the film screen downloads it (new). Subtitles: "add subtitles" after the film, as before |

## 2. The user's review of the first v38 build, and the fixes

*"New lips are seen when Vanusha is laughing, the picture of the guard is not ok. Subtitles can be added post production by
prompt. Please debug and test thoroughly."*

**New lips (वानुषा).** Frames at 0:39.6–0:44 of the sample, cropped to her face: a dark mouth bar painted under her real smile
in every frame, laughing or speaking. Cause: her picture already shows an open, laughing mouth with teeth, and the mouth point
(from the sample's cast) sat on the skin under her nose, about 0.6 mouth-widths above the real opening; the face mesh opened
"the mouth" there and the renderer painted a dark opening inside it. Fix (`Art.mouthFromPicture`, `Renderer`): teeth
(light and much less coloured than the skin beside them) found just below or around the point move the point onto the real
mouth before the face mesh is built; a mouth showing teeth or its dark inside is marked open and never painted open again (its
lips move half as far with the voice); a laugh or surprise without words no longer opens a picture's mouth at all. Measured on
the sample: वानुषा 334 tooth pixels → point moved, open; two other characters marked open by their dark mouths; the others
closed as before. Frames re-checked: her own smile, her own lips moving, nothing painted under them.

**Blinks on pictures.** The same crops showed flat skin-coloured discs over her eyes during a blink. Now an almond-shaped lid
with a soft edge into the skin and lashes along its lower edge.

**The guard (रतनलाल).** When the monkey takes his turban, his face showed torn white patches, half an eyebrow and a ghost of his
moustache curl. Cause: `clearHeadwearLeftovers` removed *everything that was not skin* beside the face (narrower than his face)
from the top of the picture down to the neck — the eyebrow's end, the moustache curl and shaded cheek and ear went, and the
palace wall showed through the holes. Fix: only the turban's own colours go (its main colours, not their average), above the
eyes; below them only its cool or vivid dyed colours (a flap or a band end, never skin); anything left floating beside the
head, joined neither to the face nor to the body, goes too. Measured: of 6,968 picture pixels beside his face between the eyes
and the mouth, 1,457 were cut before, 64 after. Also: the sharp face layer no longer includes a hand raised to the face, and
such a hand keeps the head steady with it (the face layer moved with the head and the side of the head with the arm).

**Over-the-shoulder shots (found in the 64-frame sweep).** In every over-the-shoulder reverse the shoulder character was also
drawn on the stage beside the listener — the same person twice, the face half hidden by their own back. Now they are only the
foreground shoulder, which shows the back of the head and the shoulder (it showed the waist) and never reaches past 40% of the
frame (a witch's wide hat crossed the middle). 12 of the sample's 48 such shots checked by eye.

**Subtitles.** They were already off unless asked for ("add subtitles" in the box under the film); the separate `.srt` file the
first build saved is dropped. The edit list with scene markers stays.

## 3. The user: "Not able to upload any picture for any character, object or place in the studio"

Reproduced with pictures handed over exactly as the phone does (`onActivityResult` with a content address), drawn pictures
saved by a phone's gallery (which writes the phone's make and model into them) and a real camera photo (exposure and aperture
in its Exif directory):

| Upload | Before | After |
|---|---|---|
| angles · character · drawn, saved by a phone | **refused** ("looks like a photo of a real person") | taken |
| angles · character · drawn PNG | taken | taken |
| angles · thing · drawn, saved by a phone | taken | taken |
| angles · place · drawn, saved by a phone | taken | taken |
| angles · place · camera photo | taken | taken |
| angles · character · camera photo | refused | refused (v37 rule) → **taken** as an animated avatar after §3b |
| one picture · character · drawn, saved by a phone | **refused** | taken |
| one picture · place · drawn, saved by a phone | taken, but turned into the "animated style" as a photo | taken as artwork |

Cause: `Library.cameraPhoto` (v37) counted the make or the model as camera data. Now only the exposure, the aperture, the ISO or
the focal length count. Library pictures v37 marked as photos for that reason are read once again and offered again (tested).
The v37 test's "camera photo" carried only a make; it now carries a real exposure and aperture.

### Then: "The library keeps just the last one of 60 pictures; the picker opens a pop-up telling to upload pictures with no option to upload"

* **The pop-up.** `anglesFor` built its `AlertDialog` with both `setMessage` and `setItems`; Android shows only the message when both
  are set, so the four choices (Gallery, Files, the tarunkahani library, the internet / AI) never appeared on a phone — only
  "Cancel". It is the Studio's upload for characters, places and things, the missing-pictures card's and the make-film dialog's.
  Now one view: the explanation and a button per choice. A scan of every dialog in the app found no other with both.
* **The library.** `addMany` (Home → add to the library) saved each picture with no kind; v37's camera check (the make and the
  model) refused every drawn picture a phone had saved, and the refusal was counted as "could not be opened" — only a picture
  without them was kept. With the camera fix all are kept; a real refused photo is now named as such in the message.
* Test `v38LibraryKeepsEveryPictureAndTheAnglesPopUpHasChoices`: ten such pictures handed over as the phone hands them over
  (`onActivityResult`, ten addresses at once) — 0 → 10 in the library; the angles pop-up for a character, a place and a thing
  shows the Gallery, Files and library buttons.

### 3b. Then: "you can again implement the check and also allow using camera … not able to upload multiple pictures, pictures of objects and pictures at the film-making place"

* The v37 restriction is removed: the camera is back in every picture menu (`Ui` pickers, the angles pop-up's "📷 Camera — one at a
  time" button), a camera photo of a person is taken as a character and turned into the animated avatar, costume photos are
  accepted (`SheetSaver`, `SituationsGuide` as in v36), the AI refusal and `Library.PhotoRefused` are gone.
* The angles pop-up's buttons: Photos / gallery (Android's photo picker, up to 10), Files (the document picker, several at once),
  Camera, the tarunkahani library, and for characters and places the internet / AI chooser.
* Test `v38SeveralPicturesThroughTheButtonsForEveryKind`: for a character, a place and a thing, from the Studio and from the
  make-film dialog, three pictures handed over at once through Gallery and through Files — all three saved as that one's views
  every time. Test `v38PhotosAndTheCameraAllowedAgain`: a camera photo as a character is taken; the menus offer the camera.

## 3c. Then: "It is taking तीनो as a character and asking for a picture — take only the characters of the character list"

Reproduced: `तीनो (एक साथ): "…"` made a character named "तीनो" (only "तीनों", with the dot, was known as a group word,
and only in stage directions); "वृंदा और कृपा:" was read as वृंदा doing something called "और कृपा"; "राजकुमार:" matched
"राजकुमारी" as a part of the word.

Fixed (`ScriptParser.resolveSpeakers`, `groupOf`, `looseResolve`, `byRole`, `Txt.norm`, `Director.dialogue`):

| Speaker | Before | After |
|---|---|---|
| तीनो / तीनों / All three | a new character "तीनो", picture asked | the three: named together in the scene first, never the one spoken to |
| दोनो / दोनों | a new character | the two |
| सब / सभी / सभी बच्चे | a new character | everyone in the scene but the one spoken to |
| वृंदा और कृपा / वानुषा, वृंदा और कृपा | the first name only | all of them |
| वृन्दा (spelling) | a new character | वृंदा |
| सिपाही, गार्ड, माँ (a role) | a new character | the listed guard / mother |
| a name not in the list | a new character, picture asked | a voice off the stage, no picture asked |
| a script with no character list | — | names still become characters (nothing else to go by); group words never |

A group line: all of them on the stage, facing us, their lips moving with the same words, one medium-wide frame on the
group, the subtitle naming them all (`Beat.chorus`, `Director.chorusLines`). Checked on a family story (पापा, मम्मी,
सिया, परी): "तीनो: वाह! धन्यवाद मम्मी!" → पापा, सिया, परी; "दोनो: प्लीज़ पापा!" → सिया, परी; "सब: हुर्रे!" → all four.
All 25 test stories (the sample, four test scripts, twenty soak stories): every speaker exactly as before.

Rendered (no pictures, the studio's own drawing) and found two more: the living room was a garden (`Sets.detect` knew
"living room" only in English letters), and "पापा, सिया और परी सोफ़े पर बैठे हैं" seated only पापा
(`Director.joinedPlural`: names joined with a plural verb apply to all of them). After: a room, all three on the sofa,
asserted in the test. Places of all 25 test stories unchanged; postures (every character every half second) changed in two places, both now right: "कान्हा और बापू थके हुए रेत पर बैठ जाते हैं" (बापू sits too) and "तारा और इनाया bean bags पर बैठी हैं" (इनाया sits too).

## 3d. The user's pictures: "save in library and check upload and splitting"

36 pictures (kept, phone-sized, in `tools/testdata/sheets38`): 19 character sheets on a transparent background, 10 place
sheets of six views, 4 scenes and title cards. Split at the phone's 1200 px: every one right except सिया's staggered
five-view sheet (2 instead of 5) — no empty row or column separates staggered figures, and the touching-figure fallback
only took side-by-side or stacked pairs. `Angles.erodeSplit(…, free)`: separate figure-sized parts in any layout are
figures. The 60 earlier test sheets: identical. Test `v38UserSheetsSplitSavedAndGroupLinesSpokenByTheCast`: the
splitting, all 36 added to the library at once, three of मम्मी's sheets and two place sheets uploaded as angles.

## 4. Thorough checks

* 64 frames across the whole 12-minute sample film (every 11.3 s), plus frames around every fix, checked by eye.
* The butterfly line: "(तितली उड़कर एक सूखी, मुरझाई हुई कली पर बैठ जाती है।)" no longer makes वानुषा sit (`Director.creatureSits`); checked
  on every test story — only that sentence changes.
* Sample checklist: readable speakers 36 of 36; breathing room after 23 of 23 peak lines; ten scene markers named after the scenes.
* Known and unchanged: खान राक्षस has only a back picture in the sample, so the studio draws him; the shot list asks for his picture.

## 5. Tests

New: `v38CraftGuideTrainedAndReported`, `v38EditListAndSubtitlesByInstruction`, `v38BeatLookAndFlush`,
`v38PicturesKeepTheirOwnMouthAndFace`, `v38UploadsFromThePhoneReachTheStudio`, `v38LibraryKeepsEveryPictureAndTheAnglesPopUpHasChoices`, `v38SeveralPicturesThroughTheButtonsForEveryKind`, `v38PhotosAndTheCameraAllowedAgain` — all pass, with `manyHeadedCharactersKeepTheirHeads`,
`directorUsesTheRightPictureForEachShot` and `framesRenderThroughAndroidCanvas` (7 of 7).

Full suite on the final code: **65 tests, 0 failures** (43 min; `twentyStoriesSoak` is opt-in and was not run — its twenty
stories parse, place and pose exactly as before, checked with the dumps above). Two older tests were updated for the
pictures pop-up's buttons (they pressed the first item of a list); the user's 36 pictures go through
`v38UserSheetsSplitSavedAndGroupLinesSpokenByTheCast`.
